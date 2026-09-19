package pl.floppaac.check.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;

/** StepA: wejscie na blok wyzszy niz 0.6 bez skoku. */
public class StepCheck extends Check {

    public StepCheck(FloppaAC plugin) {
        super(plugin, "StepA", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from, Location to) {
        if (data.movementExempt() || MoveUtil.canFly(player) || MoveUtil.inVehicle(player)
                || data.velocityExempt(player, System.currentTimeMillis())) {
            data.stepStreak = 0;
            return;
        }
        // Odbicie od slime daje pionowa predkosc legalnie. Miod (czt
        // podjazd po scianie miodu), babelki (nawiew pionowy) i powder
        // snow (wychodzenie) teza daja nietypowy dy.
        if (MoveUtil.onSlime(player) || MoveUtil.onHoney(player)
                || MoveUtil.inBubbleColumn(player) || MoveUtil.inPowderSnow(player)) {
            data.stepStreak = 0;
            return;
        }
        double dy = to.getY() - from.getY();
        double max = cfgDouble("max-step", 0.6) + MoveUtil.jumpPotionLevel(player) * 0.1;
        // Skok daje onGround true przez 1-2 ticki desyncu: wymagany grunt.
        if (data.groundTicks < 2) {
            data.stepStreak = 0;
            return;
        }
        if (dy > max && dy < 2.0 && player.isOnGround()) {
            data.stepStreak++;
            if (data.stepStreak >= 3) {
                flag(player, data, String.format("step=%.2f max=%.2f", dy, max));
                data.stepStreak = 0;
            }
        } else {
            data.stepStreak = Math.max(0, data.stepStreak - 1);
        }
    }
}
