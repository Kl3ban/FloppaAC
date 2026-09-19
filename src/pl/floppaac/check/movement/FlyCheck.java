package pl.floppaac.check.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;
import pl.floppaac.util.PingUtil;

/**
 * FlyA: pionowa predkosc. Skok vanilla max 0.42 plus jump boost.
 * Potem Y zawsze spada (grawitacja 0.08 na tick).
 */
public class FlyCheck extends Check {

    public FlyCheck(FloppaAC plugin) {
        super(plugin, "FlyA", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from, Location to) {
        if (data.movementExempt()) {
            // Karencja po naszym pushbacku NIE zeruje serii.
            return;
        }

        if (data.velocityExempt(player, System.currentTimeMillis())) {
            // Odrzut (zombie, strzala, TNT): grawitacja legalnie zaburzona
            // przez ticki odbicia - nie oceniamy ich.
            return;
        }
        if (MoveUtil.canFly(player) || MoveUtil.inVehicle(player)) {
            data.riseStreak = 0;
            return;
        }
        // Lewitacja Shulkera unosi legalnie.
        if (MoveUtil.hasLevitation(player)) {
            data.riseStreak = 0;
            data.flyBigStreak = 0;
            return;
        }
        if (MoveUtil.inLiquid(player) || MoveUtil.onClimbable(player)) {
            data.riseStreak = 0;
            return;
        }
        // 1.7.8: rzadkie mechaniki pionowe - odbicie slime, ciag
        // miodu, nawiew babelkow, toniecie w powder snow.
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
        // Przy pingu 60 dwa ticki sklejone w jeden event daja dy 0.8,
        // wiec pojedynczy duzy wznios wymaga potwierdzenia.
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
            // Lot przerwany od razu, nie po setbacku.
            snapToGround(player, data);
        }
    }
}
