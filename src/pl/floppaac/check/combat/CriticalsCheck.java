package pl.floppaac.check.combat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;

public class CriticalsCheck extends Check {

    public CriticalsCheck(FloppaAC plugin) {
        super(plugin, "CriticalsA", CheckType.COMBAT);
    }

    public void handle(Player attacker, PlayerData data, LivingEntity victim, double damage) {
        if (data.movementExempt()
                || data.velocityExempt(attacker, System.currentTimeMillis())) {
            data.critStreak = 0;
            return;
        }

        if (data.invOpen) {
            data.critStreak = 0;
            return;
        }
        if (MoveUtil.canFly(attacker) || MoveUtil.inVehicle(attacker)
                || MoveUtil.inLiquid(attacker) || MoveUtil.onClimbable(attacker)
                || MoveUtil.hasLevitation(attacker)) {
            data.critStreak = 0;
            return;
        }

        if (attacker.isOnGround()) {
            data.critStreak = 0;
            return;
        }

        if (attacker.getFallDistance() >= 0.3f) {
            data.critStreak = 0;
            return;
        }

        double dy = data.lastDeltaY;
        if (Math.abs(dy) < 0.08 && data.airTicks >= 2 && data.airTicks <= 10) {
            data.critStreak++;
            if (data.critStreak >= 4) {
                flag(attacker, data, String.format(
                        "packet-crit x%d fall=%.2f dy=%.3f",
                        data.critStreak, attacker.getFallDistance(), dy));
                data.critStreak = 0;
            } else if (data.critStreak == 2) {
                suspicious(attacker, data, "crit without fall (observation)");
            }
        } else {
            data.critStreak = Math.max(0, data.critStreak - 1);
        }
    }
}
