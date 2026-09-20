package pl.floppaac.check.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;

public class AntiWebCheck extends Check {

    public AntiWebCheck(FloppaAC plugin) {
        super(plugin, "WebA", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from,
                       double horizontal) {
        if (data.movementExempt()) {

            return;
        }
        if (data.velocityExempt(player, System.currentTimeMillis())) {
            data.webStreak = 0;
            return;
        }
        if (MoveUtil.canFly(player) || MoveUtil.inVehicle(player)) {
            data.webStreak = 0;
            return;
        }
        if (!MoveUtil.inWeb(player)) {
            data.webStreak = 0;
            return;
        }
        int ping = pl.floppaac.util.PingUtil.getPing(player, data);
        double leniency = pl.floppaac.util.PingUtil.leniency(
                plugin.getConfig(), ping);

        double maxH = cfgDouble("max-in-web", 0.15) * (1.0 + leniency);
        double maxFall = cfgDouble("max-fall-in-web", 0.32) * (1.0 + leniency);

        boolean fastH = horizontal > maxH && horizontal > 0.05;
        boolean fastFall = data.lastDeltaY < -maxFall;
        if (!fastH && !fastFall) {
            data.webStreak = Math.max(0, data.webStreak - 1);
            return;
        }

        data.webStreak++;
        String details = fastH
                ? String.format("web dist=%.3f max=%.3f ping=%d", horizontal, maxH, ping)
                : String.format("web spadek dy=%.2f max=%.2f ping=%d",
                        data.lastDeltaY, maxFall, ping);

        perTickPushback(player, data, from);
        if (data.webStreak >= 3) {
            flag(player, data, details);
            data.webStreak = 0;
        }
    }
}
