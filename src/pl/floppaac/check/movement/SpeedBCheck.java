package pl.floppaac.check.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MomentumMath;
import pl.floppaac.util.MoveUtil;
import pl.floppaac.util.PingUtil;

public class SpeedBCheck extends Check {

    public SpeedBCheck(FloppaAC plugin) {
        super(plugin, "SpeedB", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from, Location to,
                       double horizontal) {
        boolean airborne = data.airTicks > 0 && !player.isOnGround();

        if (!airborne) {

            data.speedPrevH = horizontal;
            data.speedExcessSum = 0.0;
            data.speedBStreak = 0;
            return;
        }

        double prevH = data.speedPrevH;
        data.speedPrevH = horizontal;

        if (data.airTicks <= 1) {
            data.speedExcessSum = 0.0;
            return;
        }

        if (MoveUtil.nearPiston(to)) {
            data.speedExcessSum = 0.0;
            return;
        }

        if (data.movementExempt()
                || data.velocityExempt(player, System.currentTimeMillis())) {
            data.speedExcessSum = 0.0;
            return;
        }
        if (MoveUtil.canFly(player) || MoveUtil.inVehicle(player)
                || MoveUtil.inLiquid(player) || MoveUtil.onClimbable(player)
                || MoveUtil.inWeb(player) || MoveUtil.hasLevitation(player)) {
            data.speedExcessSum = 0.0;
            data.speedBStreak = 0;
            return;
        }
        if (MoveUtil.onSlime(player) || MoveUtil.onHoney(player)
                || MoveUtil.inPowderSnow(player) || MoveUtil.inBubbleColumn(player)) {
            data.speedExcessSum = 0.0;
            data.speedBStreak = 0;
            return;
        }

        if (prevH > 0.0 && data.lastMoveGapMs >= 20L && data.lastMoveGapMs <= 80L) {
            double m = MomentumMath.chain(prevH, player.isSprinting());
            data.speedExcessSum += MomentumMath.excessTick(horizontal, m);
        } else {

            data.speedExcessSum = 0.0;
        }
        if (data.airTicks < MomentumMath.LATE_AIR_TICKS) {
            return;
        }
        if (horizontal <= 0.1) {
            return;
        }

        int ping = PingUtil.getPing(player, data);
        double excess = data.speedExcessSum * PingUtil.leniency(plugin.getConfig(), ping);

        if (excess > MomentumMath.EXCESS_FLAG) {
            data.speedBStreak++;
            if (data.speedBStreak >= cfgInt("streak", 2)) {
                flag(player, data, String.format(
                        "momentum nadmiar=%.3f dist=%.3f prev=%.3f air=%d ping=%d",
                        data.speedExcessSum, horizontal, prevH, data.airTicks, ping));
                data.speedBStreak = 0;
                data.speedExcessSum = 0.0;
                perTickPushback(player, data, from);
            } else {
                suspicious(player, data, String.format(
                        "momentum above chain excess=%.3f (air=%d)",
                        data.speedExcessSum, data.airTicks));
            }
        } else if (data.speedExcessSum > MomentumMath.EXCESS_FLAG * 0.5) {
            suspicious(player, data, String.format(
                    "momentum above chain (observation) excess=%.3f air=%d",
                    data.speedExcessSum, data.airTicks));
        } else {
            data.speedBStreak = Math.max(0, data.speedBStreak - 1);
        }
    }
}
