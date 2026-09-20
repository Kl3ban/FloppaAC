package pl.floppaac.check.movement;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;

public class GroundSpoofCheck extends Check {

    public GroundSpoofCheck(FloppaAC plugin) {
        super(plugin, "GroundSpoofA", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from, Location to) {
        if (data.movementExempt()) {

            return;
        }
        if (data.velocityExempt(player, System.currentTimeMillis())) {

            return;
        }
        if (MoveUtil.canFly(player) || MoveUtil.inVehicle(player)
                || MoveUtil.inLiquid(player) || MoveUtil.onClimbable(player)
                || MoveUtil.inWeb(player) || MoveUtil.inUnloadedChunk(to)) {
            return;
        }

        if (!player.isOnGround() || data.airTicks <= 3) {
            return;
        }
        double dy = to.getY() - from.getY();
        if (Math.abs(dy) < 0.05) {
            return;
        }
        double feetY = to.getY();
        double bestTop = Double.NEGATIVE_INFINITY;
        int bx = to.getBlockX();
        int bz = to.getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int down = 1; down <= 2; down++) {
                    Material m = to.getWorld().getBlockAt(
                            bx + dx, to.getBlockY() - down, bz + dz).getType();
                    if (m.isSolid()) {
                        double top = (double) (to.getBlockY() - down) + 1.0;
                        if (top > bestTop) {
                            bestTop = top;
                        }
                        break;
                    }
                }
            }
        }
        if (bestTop == Double.NEGATIVE_INFINITY) {
            return;
        }
        double gap = feetY - bestTop;
        if (gap > 0.6) {
            data.noGroundStreak++;
            if (data.noGroundStreak >= 4) {
                flag(player, data, String.format("ground %.2f below feet, dy=%.3f",
                        gap, dy));
                data.noGroundStreak = 0;
                snapToGround(player, data);
            }
        } else {
            data.noGroundStreak = 0;
        }
    }
}
