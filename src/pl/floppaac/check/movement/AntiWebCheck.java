package pl.floppaac.check.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;

/**
 * WebA (AntiWeb) - przepisany per-tick, bez serii.
 * Vanilla w pajeczynie tnie ruch poziomy do ~0.05/tick i pionowy
 * do ~0.3/tick. Każdy tick powyżej limitu jest NATYCHMIAST cofany
 * (per-tick pushback), a flaga pada po 3 szybkich tickach.
 * Nie ma tu serii 5 ani jednego darmowego przejscia: postep cheata
 * jest kasowany na biezaco.
 * Limity z zapasem na lag (x1.6 przy pingu >150 ms).
 */
public class AntiWebCheck extends Check {

    public AntiWebCheck(FloppaAC plugin) {
        super(plugin, "WebA", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from,
                       double horizontal) {
        if (data.movementExempt()) {
            // Karencja po naszym pushbacku NIE zeruje serii.
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
        // Cofnij postep z TEGO ticku od razu, zanim cokolwiek zaliczy.
        perTickPushback(player, data, from);
        if (data.webStreak >= 3) {
            flag(player, data, details);
            data.webStreak = 0;
        }
    }
}
