package pl.floppaac.check.combat;

import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;
import pl.floppaac.util.RotationMath;

public class KillAuraJCheck extends Check {

    public KillAuraJCheck(FloppaAC plugin) {
        super(plugin, "KillAuraJ", CheckType.COMBAT);
    }

    public void handle(Player player, PlayerData data) {
        if (MoveUtil.canFly(player) || MoveUtil.inVehicle(player)
                || data.movementExempt() || data.invOpen
                || data.velocityExempt(player, System.currentTimeMillis())) {
            data.gcdOffStreak = 0;
            return;
        }
        java.util.Deque<Double> yaw = data.gcdYawDeltas;
        if (yaw.size() < 20) {
            return;
        }

        long divisor = RotationMath.bestDivisor(yaw);
        if (divisor <= 0L) {
            divisor = RotationMath.gcdOf(yaw);
            if (divisor <= 0L) {
                return;
            }
        }
        int offYaw = RotationMath.countOffGrid(yaw, divisor);
        int offPitch = RotationMath.countOffGrid(data.gcdPitchDeltas, divisor);

        if (offYaw + offPitch < 10) {
            data.gcdOffStreak = 0;
            return;
        }

        long now = System.currentTimeMillis();
        if (data.lastAttackMs <= 0L || now - data.lastAttackMs > 5000L) {
            return;
        }
        if (now - data.lastGcdFlagMs < 8000L) {
            return;
        }
        data.gcdOffStreak++;
        if (data.gcdOffStreak >= 4) {
            data.lastGcdFlagMs = now;
            signalAs("KillAuraJ", player, data, String.format(
                    "gcd div=%.4f off yaw=%d/%d pitch=%d/%d",
                    divisor / 16777216.0, offYaw, yaw.size(),
                    offPitch, data.gcdPitchDeltas.size()));
            data.gcdOffStreak = 0;
        } else {
            suspicious(player, data, String.format(
                    "gcd off-grid yaw=%d pitch=%d div=%.4f",
                    offYaw, offPitch, divisor / 16777216.0));
        }
    }
}
