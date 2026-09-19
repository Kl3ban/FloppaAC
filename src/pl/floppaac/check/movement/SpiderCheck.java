package pl.floppaac.check.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;

/**
 * SpiderA: wspinanie sie po scianie bez drabiny. Przeprojektowane:
 * dwa niezalezne sygnaly, oba wymagaja ciaglego wznoszenia przy
 * pelnej swobodzie ruchu (bez tarczy, bez kucania, bez drabiny,
 * bez wody). Legalny gracz przy scianie ZAWSZE opada, wiec kazdy
 * ciagly wznos to cheat - pytanie tylko o czas okna.
 * SpiderWall: wznoszenie przy blokujacej scianie (dotyk pudelka
 * w 6 kierunkach), 12 tickow z rzedu (0,6 s). Kucanie/tarcza zeruja:
 * wolne pietrzenie przy scianie to legalna mechanika.
 * SpiderAir: wznoszenie w przestrzeni otwartej, 8 tickow.
 * Niezaleznie od poziomej predkosci (Jetpack i wieza maja predkosc).
 */
public class SpiderCheck extends Check {

    public SpiderCheck(FloppaAC plugin) {
        super(plugin, "SpiderA", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from, Location to) {
        if (data.movementExempt() || MoveUtil.canFly(player) || MoveUtil.inVehicle(player)
                || MoveUtil.onClimbable(player) || MoveUtil.inLiquid(player)
                || MoveUtil.hasLevitation(player) || MoveUtil.onSlime(player)
                || MoveUtil.inBubbleColumn(player)
                || data.velocityExempt(player, System.currentTimeMillis())) {
            data.spiderWallStreak = 0;
            data.spiderAirStreak = 0;
            return;
        }
        double dy = to.getY() - from.getY();
        if (dy <= 0.05 || player.isOnGround()) {
            data.spiderWallStreak = Math.max(0, data.spiderWallStreak - 1);
            data.spiderAirStreak = Math.max(0, data.spiderAirStreak - 1);
            return;
        }
        // Wolne pietrzenie przy scianie z tarcza/kucaniem to legalna
        // mechanika vanilla (skok przy blokujacej tarczy). Zeruje serie.
        if (player.isSneaking() || isBlocking(player)) {
            data.spiderWallStreak = 0;
            data.spiderAirStreak = 0;
            return;
        }
        double horizontal = Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ());
        boolean nearWall = wallAhead(player);
        if (nearWall && horizontal < 0.12) {
            data.spiderWallStreak++;
            if (data.spiderWallStreak >= 12) {
                flag(player, data, String.format("sciana dy=%.2f x%d",
                        dy, data.spiderWallStreak));
                data.spiderWallStreak = 0;
            }
        } else {
            data.spiderWallStreak = 0;
        }
        if (!nearWall && horizontal < 0.04) {
            data.spiderAirStreak++;
            if (data.spiderAirStreak >= 8) {
                flag(player, data, String.format("powietrze dy=%.2f x%d",
                        dy, data.spiderAirStreak));
                data.spiderAirStreak = 0;
            }
        } else {
            data.spiderAirStreak = 0;
        }
    }

    /** Czy przed graczem stoi blokujacy blok (pudelko pelne, 6 kierunkow). */
    private boolean wallAhead(Player player) {
        org.bukkit.Location base = player.getLocation();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        for (int[] d : dirs) {
            for (int up = 0; up <= 1; up++) {
                org.bukkit.block.Block b = base.getWorld().getBlockAt(
                        base.getBlockX() + d[0], base.getBlockY() + up, base.getBlockZ() + d[1]);
                if (b.getType().isSolid()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isBlocking(Player player) {
        try {
            return player.isBlocking();
        } catch (NoSuchMethodError e) {
            return false;
        }
    }
}
