package pl.floppaac.check.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MomentumMath;
import pl.floppaac.util.MoveUtil;
import pl.floppaac.util.PingUtil;

/**
 * SpeedB: ped poziomy w poznych tickach powietrza (late-air decay).
 * Model vanilla: m = m * 0.91 + accel (sprint 0.026).
 *
 * Kluczowy model 1.7.8: NIE porownujemy h z capem per-tick (eps 0.04
 * przepuszcza stala predkosc 1.3x, bo nadmiar 0.016 < eps). Zamiast
 * tego prowadzimy lancuch modelu od zmierzonego startu fazy
 * powietrznej i akumulujemy nadmiar suma(max(0, h - m)). Legalny
 * gracz nigdy nie przekracza lancucha (nierownosc trojkata dla
 * skretu), wiec suma ~0.
 *
 * Uczciwe liczby (lancuch podaza za cheatem, wiec stala predkosc
 * daje staly przyrost nadmiaru na tick): 0.40 (1.2x) -> 0.01/tick
 * = 20 tickow; 0.45 -> 14; 0.50 -> 11; 0.60 -> 7. Czyli cheat
 * "1.3x w locie" lapanie po ok. 1 s utrzymania, przyspieszajacy
 * fly (h rosnaca) juz po kilku tickach.
 *
 * Cap liczony od ZMIERZONEJ poprzedniej delty, wiec lod, woda,
 * pistony i inne modyfikatory startu nie psuja modelu - oceniamy
 * kontynuacje pedu, nie absolutna predkosc.
 *
 * Zero-FP: ticki pojedyncze (luka 20-80 ms), od 8. ticka powietrza,
 * z exemptionami rzadkich mechanik (slime, miod, babelki, pistony,
 * powder snow). Wczesne ticki pomija boost sprint-jump (0.2).
 */
public class SpeedBCheck extends Check {

    public SpeedBCheck(FloppaAC plugin) {
        super(plugin, "SpeedB", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from, Location to,
                       double horizontal) {
        boolean airborne = data.airTicks > 0 && !player.isOnGround();

        if (!airborne) {
            // Nowa faza ruchu: lancuch od zmierzonej delty, nadmiar od zera.
            data.speedPrevH = horizontal;
            data.speedExcessSum = 0.0;
            data.speedBStreak = 0;
            return;
        }

        double prevH = data.speedPrevH;
        data.speedPrevH = horizontal;

        // Pierwszy tick powietrza: boost sprint-jump (0.2) i starty z
        // lodu/pistonu wchodza do BAZY lancucha (zmierzona delta), nie
        // do nadmiaru. Bez tego kazdy sprint-jump mialby ok. 0.199
        // nadmiaru w pierwszym ticku = FP przy kazdym skoku.
        if (data.airTicks <= 1) {
            data.speedExcessSum = 0.0;
            return;
        }
        // Piston pcha poziomo bez PlayerVelocityEvent: tick z pchneciem
        // pomijamy w akumulacji (impuls staje sie nowa baza lancucha).
        if (MoveUtil.nearPiston(to)) {
            data.speedExcessSum = 0.0;
            return;
        }

        // Exemptiony PRZED akumulacja: duzy knockback legalnie wyrzuca
        // poza lancuch pedu. Gdyby nadmiar rosl w trakcie karencji,
        // flaga padalaby tuz po jej koncu z bilansu nabitego legalnym
        // odrzutem (FP po mocnym KB). Karencja nie zeruje serii flag,
        // ale zeruje bilans nadmiaru.
        if (data.movementExempt()
                || data.velocityExempt(player, System.currentTimeMillis())) {
            data.speedExcessSum = 0.0;
            return;
        }
        if (MoveUtil.canFly(player) || MoveUtil.inVehicle(player)
                || MoveUtil.inLiquid(player) || MoveUtil.onClimbable(player)
                || MoveUtil.inWeb(player) || MoveUtil.hasLevitation(player)) {
            data.speedExcessSum = 0.0;
            data.speedBStreak = 0;
            return;
        }
        if (MoveUtil.onSlime(player) || MoveUtil.onHoney(player)
                || MoveUtil.inPowderSnow(player) || MoveUtil.inBubbleColumn(player)) {
            data.speedExcessSum = 0.0;
            data.speedBStreak = 0;
            return;
        }

        // Lancuch i nadmiar tylko na wiarygodnej fazie bez exemptionow.
        if (prevH > 0.0 && data.lastMoveGapMs >= 20L && data.lastMoveGapMs <= 80L) {
            double m = MomentumMath.chain(prevH, player.isSprinting());
            data.speedExcessSum += MomentumMath.excessTick(horizontal, m);
        } else {
            // Sklejone ticki lub dziura w strumieniu: model per-tick
            // nieczytelny. Faza niewiarygodna - reset bilansu.
            data.speedExcessSum = 0.0;
        }
        if (data.airTicks < MomentumMath.LATE_AIR_TICKS) {
            return;
        }
        if (horizontal <= 0.1) {
            return;
        }

        int ping = PingUtil.getPing(player, data);
        double excess = data.speedExcessSum * PingUtil.leniency(plugin.getConfig(), ping);

        if (excess > MomentumMath.EXCESS_FLAG) {
            data.speedBStreak++;
            if (data.speedBStreak >= cfgInt("streak", 2)) {
                flag(player, data, String.format(
                        "momentum nadmiar=%.3f dist=%.3f prev=%.3f air=%d ping=%d",
                        data.speedExcessSum, horizontal, prevH, data.airTicks, ping));
                data.speedBStreak = 0;
                data.speedExcessSum = 0.0;
                perTickPushback(player, data, from);
            } else {
                suspicious(player, data, String.format(
                        "ped nad lancuchem nadmiar=%.3f (air=%d)",
                        data.speedExcessSum, data.airTicks));
            }
        } else if (data.speedExcessSum > MomentumMath.EXCESS_FLAG * 0.5) {
            suspicious(player, data, String.format(
                    "ped nad lancuchem (obserwacja) nadmiar=%.3f air=%d",
                    data.speedExcessSum, data.airTicks));
        } else {
            data.speedBStreak = Math.max(0, data.speedBStreak - 1);
        }
    }
}
