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

public class NoFallCheck extends Check {

    private static final double MAX_FALL_PER_TICK = 3.92;

    private static final int PLATEAU_TICKS = 10;

    private static final int MIN_SERVER_AIR = 4;

    public NoFallCheck(FloppaAC plugin) {
        super(plugin, "NoFallA", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from, Location to) {
        double dy = to.getY() - from.getY();

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

        if (declaredGround && solidBelow) {
            data.noFallPlateauTicks = 0;
            boolean wasFalling = data.fallingAcc;
            double fell = data.fallDistAcc;
            int air = data.serverAirTicks;
            data.fallDistAcc = 0.0;
            data.fallingAcc = false;
            data.serverAirTicks = 0;
            if (!wasFalling || fell <= 3.5 || air < MIN_SERVER_AIR) {

                return;
            }
            if (isDamageReducingSurface(to)) {
                return;
            }
            scheduleLandingConfirm(player, data, fell);
            return;
        }

        data.serverAirTicks++;
        handleFall(player, data, from, to);

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

        }
    }

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
