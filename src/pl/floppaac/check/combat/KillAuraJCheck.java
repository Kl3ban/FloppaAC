package pl.floppaac.check.combat;

import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;
import pl.floppaac.util.RotationMath;

/**
 * KillAuraJ: GCD rotacji (off-grid aim).
 * Legalny klient kwantyzuje delty wzgledem sensywnosci: kazda delta
 * jest wielokrotnoscia jednego dzielnika (GCD okna). Aim boty /
 * silent aim z interpolacja generuja delty poza siatka.
 *
 * Proby: GCD z okna 20 delt (fixed-point 2^24, odporne na float),
 * potem licznik delt off-grid. Flaga dopiero gdy okno ma >= 4 delt
 * off-grid (legalne szumy to 1-2) i przy serii ocen >= 3 w oknie
 * 5 s od ataku (kontekst combat), throttle 5 s. Gliding/pojazd/
 * levitacja wyklaczone (obroty nie od myszy).
 */
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
        // Najpierw glosowanie parami (odporne na float-rounding);
        // klasyczne NWD jako fallback dla stabilnych okien.
        long divisor = RotationMath.bestDivisor(yaw);
        if (divisor <= 0L) {
            divisor = RotationMath.gcdOf(yaw);
            if (divisor <= 0L) {
                return;
            }
        }
        int offYaw = RotationMath.countOffGrid(yaw, divisor);
        int offPitch = RotationMath.countOffGrid(data.gcdPitchDeltas, divisor);
        // Sygnal wymaga wyraznej populacji off-grid. Legalne flicki
        // z akceleracja myszy daja 6-9 delt off-grid na okno, wiec prog
        // to 10 (potwierdzone FP 11+20, 5+1 i 7+2 na legalnej grze).
        // Heurystyka tylko sygnalizuje do bota, nie karze.
        if (offYaw + offPitch < 10) {
            data.gcdOffStreak = 0;
            return;
        }
        // Kontekst combat: GCD oceniamy tylko gdy gracz niedawno atakowal.
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
