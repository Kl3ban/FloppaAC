package pl.floppaac.check.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;
import pl.floppaac.util.PingUtil;

public class FlyCheck extends Check {

    public FlyCheck(FloppaAC plugin) {
        super(plugin, "FlyA", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from, Location to) {
        if (data.movementExempt()) {

            return;
        }

        if (data.velocityExempt(player, System.currentTimeMillis())) {

            return;
        }
        if (MoveUtil.canFly(player) || MoveUtil.inVehicle(player)) {
            data.riseStreak = 0;
            return;
        }

        if (MoveUtil.hasLevitation(player)) {
            data.riseStreak = 0;
            data.flyBigStreak = 0;
            return;
        }
        if (MoveUtil.inLiquid(player) || MoveUtil.onClimbable(player)) {
            data.riseStreak = 0;
            return;
        }

        if (MoveUtil.onSlime(player) || MoveUtil.onHoney(player)
                || MoveUtil.inBubbleColumn(player) || MoveUtil.inPowderSnow(player)) {
            data.riseStreak = 0;
            data.flyBigStreak = 0;
            return;
        }

        double dy = to.getY() - from.getY();
        if (dy <= 0.02 || player.isOnGround()) {
            if (dy <= 0.02) {
                data.riseStreak = 0;
            }
            return;
        }

        int ping = PingUtil.getPing(player, data);
        double leniency = PingUtil.leniency(plugin.getConfig(), ping);

        double maxRise = MoveUtil.JUMP_VELOCITY
                + MoveUtil.jumpPotionLevel(player) * 0.12 + 0.05;
        maxRise = MoveUtil.applyLeniency(maxRise, leniency);

        if (dy > maxRise && data.airTicks > 2) {
            data.flyBigStreak++;
            if (data.flyBigStreak >= 2) {
                flag(player, data, String.format("dy=%.3f max=%.3f ping=%d x%d",
                        dy, maxRise, ping, data.flyBigStreak));
                data.flyBigStreak = 0;
                snapToGround(player, data);
            }
            data.riseStreak = 0;
            return;
        }
        data.flyBigStreak = 0;

        data.riseStreak++;
        if (data.riseStreak >= 6) {
            flag(player, data, String.format("rise x%d dy=%.3f ping=%d",
                    data.riseStreak, dy, ping));
            data.riseStreak = 0;

            snapToGround(player, data);
        }
    }
}
