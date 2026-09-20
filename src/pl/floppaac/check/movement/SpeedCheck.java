package pl.floppaac.check.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;
import pl.floppaac.util.PingUtil;

public class SpeedCheck extends Check {

    public SpeedCheck(FloppaAC plugin) {
        super(plugin, "SpeedA", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from, Location to,
                       double horizontal) {
        if (data.movementExempt()) {

            return;
        }
        if (MoveUtil.canFly(player) || MoveUtil.inVehicle(player)
                || MoveUtil.inLiquid(player) || player.isGliding() || player.isRiptiding()) {
            data.speedStreak = 0;
            return;
        }

        long now = System.currentTimeMillis();

        if (data.velocityExempt(player, now)) {
            return;
        }

        long sinceKb = now - data.kbSinceBigMs;
        if (sinceKb < 1500L) {
            return;
        }

        double baseWalk = cfgDouble("base-walk", 0.2155);
        double baseSprint = cfgDouble("base-sprint", 0.2806);
        double potionBonus = cfgDouble("potion-bonus", 0.06);

        double max = MoveUtil.maxHorizontal(player, baseWalk, baseSprint, potionBonus);
        double dy = to.getY() - from.getY();
        boolean airborne = !player.isOnGround() && Math.abs(dy) > 0.005;
        if (airborne) {
            max *= 1.6;
        } else {
            max *= 1.12;
        }

        if (!airborne && now - data.lastLandMs < 500L) {
            max *= 1.25;
        }

        if (dy < -0.15) {
            max += 0.25;
        }
        int ping = PingUtil.getPing(player, data);
        max = MoveUtil.applyLeniency(max, PingUtil.leniency(plugin.getConfig(), ping));

        if (sinceKb < 3500L) {
            max += 0.15;
        }

        if (horizontal > max && horizontal > 0.05) {
            data.speedStreak++;
            int streakLimit = cfgInt("streak", 6);
            if (data.speedStreak >= streakLimit) {
                flag(player, data, String.format("dist=%.3f max=%.3f ping=%d sprint=%b",
                        horizontal, max, ping, player.isSprinting()));
                data.speedStreak = 0;
                perTickPushback(player, data, from);
            } else if (data.speedStreak == 4) {
                suspicious(player, data, String.format("speed narasta %.3f vs %.3f",
                        horizontal, max));
            }
        } else {
            data.speedStreak = Math.max(0, data.speedStreak - 1);
        }
    }
}
