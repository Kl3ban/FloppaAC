package pl.floppaac.check.combat;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.PingUtil;

public class ReachCheck extends Check {

    public ReachCheck(FloppaAC plugin) {
        super(plugin, "ReachA", CheckType.COMBAT);
    }

    public void handle(Player attacker, PlayerData data, LivingEntity victim) {
        if (data.movementExempt() || data.invOpen) {
            data.reachStreak = 0;
            return;
        }
        if (attacker.getGameMode() == GameMode.CREATIVE
                || attacker.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        Location eye = attacker.getEyeLocation();
        double dEye = eye.distance(victim.getEyeLocation());
        double dFeet = eye.distance(victim.getLocation());
        double dist = Math.min(dEye, dFeet);

        int ping = PingUtil.getPing(attacker, data);
        double max = cfgDouble("max-reach", 3.2);
        max += ping > 150 ? 0.3 : 0.15;
        max = max * (1.0 + PingUtil.leniency(plugin.getConfig(), ping));

        long now = System.currentTimeMillis();
        if (now - data.lastReachMs > 2000L) {
            data.reachStreak = 0;
        }
        if (dist > max) {
            data.lastReachMs = now;
            data.reachStreak++;
            if (data.reachStreak >= 4) {
                flag(attacker, data, String.format("dist=%.2f max=%.2f ping=%d x%d",
                        dist, max, ping, data.reachStreak));
                data.reachStreak = 0;
            } else {
                suspicious(attacker, data, String.format("reach %.2f vs %.2f (%d/4)",
                        dist, max, data.reachStreak));
            }
        } else {
            if (dist > max - 0.25) {
                suspicious(attacker, data, String.format("reach graniczny %.2f", dist));
            } else {
                data.reachStreak = Math.max(0, data.reachStreak - 1);
            }
        }
    }
}
