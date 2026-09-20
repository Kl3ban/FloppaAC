package pl.floppaac.check.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;
import pl.floppaac.util.PingUtil;

public class FlyBCheck extends Check {

    public FlyBCheck(FloppaAC plugin) {
        super(plugin, "FlyB", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from, Location to,
                       double dy, double horizontal) {
        if (data.movementExempt()) {

            return;
        }

        if (data.velocityExempt(player, System.currentTimeMillis())) {

            return;
        }
        if (MoveUtil.canFly(player) || MoveUtil.inVehicle(player)
                || MoveUtil.inLiquid(player) || MoveUtil.onClimbable(player)
                || MoveUtil.inWeb(player) || MoveUtil.hasLevitation(player)
                || MoveUtil.onSlime(player) || MoveUtil.onHoney(player)
                || MoveUtil.inBubbleColumn(player) || MoveUtil.inPowderSnow(player)) {
            data.hoverTicks = 0;
            return;
        }
        if (player.isOnGround()) {
            data.hoverTicks = 0;
            return;
        }
        if (Math.abs(dy) < 0.03 && horizontal > 0.05) {
            data.hoverTicks++;
            if (data.hoverTicks >= 6) {
                int ping = PingUtil.getPing(player, data);
                flag(player, data, String.format("hover x%d dy=%.3f ping=%d",
                        data.hoverTicks, dy, ping));
                data.hoverTicks = 0;
                snapToGround(player, data);
            }
        } else {
            data.hoverTicks = Math.max(0, data.hoverTicks - 2);
        }
    }
}
