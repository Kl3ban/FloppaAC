package pl.floppaac.check.player;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.ReachMath;

public class DigReachCheck extends Check {

    private static final long STREAK_WINDOW_MS = 5000L;

    public DigReachCheck(FloppaAC plugin) {
        super(plugin, "DigReachA", CheckType.PLAYER);
    }

    public boolean handle(Player player, PlayerData data, Block broken) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return false;
        }
        if (player.isGliding() || player.isInsideVehicle()
                || player.isRiptiding() || player.isSleeping()) {
            return false;
        }
        if (data.movementExempt()) {
            return false;
        }
        double reach = cfgDouble("max-reach", 4.5);
        double slack = cfgDouble("slack", 1.5);
        Location eye = player.getEyeLocation();
        double dist = ReachMath.eyeToBox(eye.getX(), eye.getY(), eye.getZ(),
                broken.getX(), broken.getY(), broken.getZ());
        long now = System.currentTimeMillis();
        if (!ReachMath.isBreach(dist, reach, slack)) {
            if (now - data.digReachBreachMs > STREAK_WINDOW_MS) {
                data.digReachStreak = 0;
            }
            return false;
        }
        if (now - data.digReachBreachMs > STREAK_WINDOW_MS) {
            data.digReachStreak = 1;
        } else {
            data.digReachStreak++;
        }
        data.digReachBreachMs = now;
        String detail = String.format("%.2f > %.2f", dist, reach + slack);
        if (data.digReachStreak >= cfgInt("streak", 2)) {
            data.digReachStreak = 0;
            return flag(player, data, detail);
        }
        suspicious(player, data, detail);
        return false;
    }
}
