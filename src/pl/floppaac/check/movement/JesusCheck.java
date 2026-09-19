package pl.floppaac.check.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;

/** JesusA: chodzenie po wodzie bez plywania. */
public class JesusCheck extends Check {

    public JesusCheck(FloppaAC plugin) {
        super(plugin, "JesusA", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location to, double horizontal) {
        if (data.movementExempt() || MoveUtil.canFly(player) || MoveUtil.inVehicle(player)
                || data.velocityExempt(player, System.currentTimeMillis())) {
            // velocityExempt: odrzut wpuszcza pod wode i podnosi nad fleke.
            data.jesusStreak = 0;
            return;
        }
        try {
            if (player.isSwimming()) {
                data.jesusStreak = 0;
                return;
            }
        } catch (NoSuchMethodError e) {
            // bardzo stara wersja bez plywania
        }
        if (!MoveUtil.inLiquid(player) && to.getBlock().getType().name().contains("WATER")) {
            if (Math.abs(to.getY() - Math.floor(to.getY())) < 0.15 && horizontal > 0.1) {
                data.jesusStreak++;
                if (data.jesusStreak >= 6) {
                    flag(player, data, "poziom na wodzie");
                    data.jesusStreak = 0;
                }
                return;
            }
        }
        data.jesusStreak = Math.max(0, data.jesusStreak - 1);
    }
}
