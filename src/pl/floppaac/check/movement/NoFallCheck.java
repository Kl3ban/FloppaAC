package pl.floppaac.check.movement;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;

/**
 * NoFallA - fall distance liczone WYLACZNIE serwerowo z pozycji.
 * Klient nie ma tu nic do gadania: isOnGround() i getFallDistance()
 * pochodza z deklaracji klienta, wiec NoFall pakietowy je falszuje.
 *
 * Mechanizm 1.8.1:
 *  - grunt to JEDNOCZESNIE grunt deklarowany i serwerowy
 *    (hasSolidBelow). Ruch po schodach i plytach to grunt co tick,
 *    wiec akumulator nie rosnie i nie ma flag za schodzenie.
 *    Licznik serverAirTicks rozroznia schody (0-1) od upadku (wiele).
 *  - ladowanie po realnym locie (fell powyzej 3.5, powietrza
 *    co najmniej 4 ticki) nie zadaje obrazen od razu. Najpierw
 *    szybka sciezka (vanilla juz policzyla), a w razie watpliwosci
 *    potwierdzenie 3 ticki pozniej po znaczniku lastFallDamageMs.
 *    Pierwszy tick ladowania nie dubluje obrazen vanilla.
 *  - tryb No-Ground (onGround=false zawsze): detektor PLATEAU liczy
 *    ticki stania nad serwerowym gruntem bez opadania. Rozliczenie
 *    nie patrzy na claimed, tylko na brak swiezych obrazen FALL.
 *
 * Zwolnienia: kreatywny/spectator, elytra, woda, pajeczyna, drabina,
 * slime, slow falling, levitation, pojazd, canFly, unloaded chunk,
 * knockback, bloki tlumiace (snop, miod, snieg).
 */
public class NoFallCheck extends Check {

    /** Terminal velocity vanilla w blokach na tick. */
    private static final double MAX_FALL_PER_TICK = 3.92;

    /** Tickow stania nad gruntem do flagi (No-Ground). */
    private static final int PLATEAU_TICKS = 10;

    /** Minimalne serwerowe powietrze przed rozliczeniem ladowania. */
    private static final int MIN_SERVER_AIR = 4;

    public NoFallCheck(FloppaAC plugin) {
        super(plugin, "NoFallA", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from, Location to) {
        double dy = to.getY() - from.getY();

        // Exemptiony najpierw, zeby elytra, odrzut czy teleport
        // nie nakrecaly akumulatora.
        if (data.movementExempt()
                || player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR
                || player.isGliding()
                || MoveUtil.canFly(player) || MoveUtil.inVehicle(player)
                || MoveUtil.inLiquid(player) || MoveUtil.onClimbable(player)
                || MoveUtil.inWeb(player) || MoveUtil.onSlime(player)
                || MoveUtil.onHoney(player) || MoveUtil.inPowderSnow(player)
                || MoveUtil.inBubbleColumn(player)
                || MoveUtil.hasSlowFalling(player)
                || MoveUtil.hasLevitation(player)
                || MoveUtil.inUnloadedChunk(to)
                || data.velocityExempt(player, System.currentTimeMillis())) {
            data.fallDistAcc = 0.0;
            data.fallingAcc = false;
            data.noFallPlateauTicks = 0;
            data.serverAirTicks = 0;
            return;
        }

        boolean declaredGround = false;
        try {
            declaredGround = player.isOnGround();
        } catch (Exception e) {
            declaredGround = false;
        }
        boolean solidBelow = false;
        try {
            solidBelow = MoveUtil.hasSolidBelow(to);
        } catch (Exception e) {
            solidBelow = false;
        }

        // Grunt tylko gdy klient i serwer zgodni. Schody, plyty
        // i plaski teren zeruja akumulator co tick.
        if (declaredGround && solidBelow) {
            data.noFallPlateauTicks = 0;
            boolean wasFalling = data.fallingAcc;
            double fell = data.fallDistAcc;
            int air = data.serverAirTicks;
            data.fallDistAcc = 0.0;
            data.fallingAcc = false;
            data.serverAirTicks = 0;
            if (!wasFalling || fell <= 3.5 || air < MIN_SERVER_AIR) {
                // Chodzenie, schody, krotki zeskok.
                return;
            }
            if (isDamageReducingSurface(to)) {
                return;
            }
            scheduleLandingConfirm(player, data, fell);
            return;
        }

        // Faza powietrzna (w tym grunt=true w powietrzu z trybu Packet).
        data.serverAirTicks++;
        handleFall(player, data, from, to);
        // Tryb No-Ground: klient deklaruje onGround=false nawet na
        // ziemi, wiec faza ladowania nigdy nie nastepuje i vanilla
        // nie rozlicza upadku. Sygnatura: serwer widzi solidny grunt
        // pod stopami, gracz nie opada, akumulator trzyma spadek.
        if (data.fallDistAcc > 3.5 && solidBelow && !declaredGround
                && dy > -0.05) {
            data.noFallPlateauTicks++;
            if (data.noFallPlateauTicks >= PLATEAU_TICKS) {
                double fellP = data.fallDistAcc;
                data.fallDistAcc = 0.0;
                data.fallingAcc = false;
                data.noFallPlateauTicks = 0;
                long now = System.currentTimeMillis();
                boolean vanillaDone = now - data.lastFallDamageMs < 1500L;
                if (!isDamageReducingSurface(to)
                        && !vanillaDone
                        && player.getGameMode() != GameMode.CREATIVE
                        && player.getGameMode() != GameMode.SPECTATOR) {
                    double dmgP = fellP - 3.0;
                    if (dmgP > 0.0) {
                        player.setFallDistance((float) Math.max(0.0, dmgP));
                        player.damage(dmgP);
                        player.setFallDistance(0.0f);
                    }
                }
                float claimedP = 0.0f;
                try {
                    claimedP = player.getFallDistance();
                } catch (Exception e) {
                    claimedP = 0.0f;
                }
                flag(player, data, String.format(
                        "no-ground: fall=%.1f claimed=%.1f (%d ticks above ground)",
                        fellP, claimedP, PLATEAU_TICKS));
            }
        } else if (!solidBelow || dy <= -0.05) {
            data.noFallPlateauTicks = 0;
        }
    }

    /**
     * Rozliczenie ladowania z potwierdzeniem. Szybka sciezka odpuszcza,
     * gdy vanilla juz policzyla (znacznik FALL albo claimed). W razie
     * watpliwosci zadanie synchroniczne 3 ticki pozniej sprawdza
     * znacznik ponownie i dopiero wtedy zadaje obrazenia z flagą.
     * Dzieki temu legalny upadek nigdy nie dostaje podwojnych obrazen.
     */
    private void scheduleLandingConfirm(final Player player,
                                        final PlayerData data,
                                        final double fell) {
        long now = System.currentTimeMillis();
        float claimed = 0.0f;
        try {
            claimed = player.getFallDistance();
        } catch (Exception e) {
            claimed = 0.0f;
        }
        if (now - data.lastFallDamageMs < 1500L || claimed >= fell * 0.4) {
            // Vanilla sama policzyla obrazenia z tego upadku.
            return;
        }
        final java.util.UUID uuid = player.getUniqueId();
        final float claimedF = claimed;
        try {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null || !p.isOnline() || p.isDead()) {
                        return;
                    }
                    PlayerData d = plugin.getDataManager().get(p);
                    // Vanilla zdazyla w miedzyczasie.
                    if (System.currentTimeMillis() - d.lastFallDamageMs < 1500L) {
                        return;
                    }
                    if (d.movementExempt()
                            || p.getGameMode() == GameMode.CREATIVE
                            || p.getGameMode() == GameMode.SPECTATOR
                            || p.isGliding()
                            || MoveUtil.canFly(p) || MoveUtil.inVehicle(p)
                            || MoveUtil.inLiquid(p) || MoveUtil.onClimbable(p)
                            || MoveUtil.inWeb(p) || MoveUtil.onSlime(p)
                            || MoveUtil.onHoney(p) || MoveUtil.inPowderSnow(p)
                            || MoveUtil.inBubbleColumn(p)
                            || MoveUtil.hasSlowFalling(p)
                            || MoveUtil.hasLevitation(p)
                            || MoveUtil.inUnloadedChunk(p.getLocation())
                            || d.velocityExempt(p, System.currentTimeMillis())) {
                        return;
                    }
                    if (isDamageReducingSurface(p.getLocation())) {
                        return;
                    }
                    // NoFall wyciszyl obrazenia: zadajemy pelne vanilla
                    // obrazenia z serwerowego pomiaru wysokosci spadku.
                    double dmg = fell - 3.0;
                    if (dmg > 0.0) {
                        p.setFallDistance((float) Math.max(0.0, dmg));
                        p.damage(dmg);
                        p.setFallDistance(0.0f);
                    }
                    flag(p, d, String.format(
                            "nofall off: fall=%.1f claimed=%.1f",
                            fell, claimedF));
                }
            }, 3L);
        } catch (Exception e) {
            // Brak potwierdzenia nie blokuje gry.
        }
    }

    /** Sumowanie drogi spadku w powietrzu wylacznie z pozycji. */
    private void handleFall(Player player, PlayerData data, Location from, Location to) {
        double dy = to.getY() - from.getY();
        double fallTick = Math.max(0.0, -dy);
        if (fallTick > MAX_FALL_PER_TICK) {
            fallTick = MAX_FALL_PER_TICK;
        }
        if (fallTick > 0.0 || data.fallingAcc) {
            data.fallDistAcc += fallTick;
            data.fallingAcc = true;
        }
    }

    /** Bloki, na ktorych vanilla legalnie tlumi obrazenia od upadku. */
    private static boolean isDamageReducingSurface(Location loc) {
        Material m = loc.getWorld().getBlockAt(
                loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ()).getType();
        String n = m.name();
        return n.equals("HAY_BLOCK") || n.equals("HONEY_BLOCK")
                || n.contains("POWDER_SNOW") || n.endsWith("_BED")
                || n.contains("TWISTING_VINES") || n.contains("CAVE_VINES")
                || n.equals("SWEET_BERRY_BUSH") || n.equals("SLIME_BLOCK");
    }
}
