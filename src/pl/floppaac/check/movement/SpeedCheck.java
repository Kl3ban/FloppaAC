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
 * SpeedA: predkosc pozioma. Chod 0.2158, sprint 0.2806 plus
 * speed potion i lod x1.6.
 *
 * Legalne piki, ktore NIE moga fladowac:
 *  - sprint-jump: tick odbicia ~0.35-0.48 na 1-2 ticki (leniencja
 *    powietrzna zliczana osobno, bez pushbacku),
 *  - knockback od mobow (zombie podpalony, strzala, TNT, eksplozja):
 *    karencja 1.5 s od silnego odrzutu, potem luzniejszy limit,
 *  - impulsy obrazen (ogień, trucizna, wither) wywoluja szarpniecia
 *    ruchu - karencja 1 s.
 * Pushback tylko przy potwierdzonej serii (flaga), nigdy pojedynczo.
 */
public class SpeedCheck extends Check {

    public SpeedCheck(FloppaAC plugin) {
        super(plugin, "SpeedA", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from, Location to,
                       double horizontal) {
        if (data.movementExempt()) {
            // Karencja po naszym pushbacku NIE zeruje serii.
            return;
        }
        if (MoveUtil.canFly(player) || MoveUtil.inVehicle(player)
                || MoveUtil.inLiquid(player) || player.isGliding() || player.isRiptiding()) {
            data.speedStreak = 0;
            return;
        }

        long now = System.currentTimeMillis();
        // Sila odrzutu (w tym duzy KB z knockback/punch/eksplozji):
        // pelna karencja przez ticki velocity, skalowana sila w
        // PlayerVelocityEvent. Bez tego duzy odrzut flagowal SpeedA.
        if (data.velocityExempt(player, now)) {
            return;
        }
        // Odrzut / impuls obrazen (zombie, strzala, TNT, ogien): 1.5 s
        // pelnej karencji, potem jeszcze luzniejszy prog do 3.5 s.
        long sinceKb = now - data.kbSinceBigMs;
        if (sinceKb < 1500L) {
            return;
        }

        double baseWalk = cfgDouble("base-walk", 0.2155);
        double baseSprint = cfgDouble("base-sprint", 0.2806);
        double potionBonus = cfgDouble("potion-bonus", 0.06);

        double max = MoveUtil.maxHorizontal(player, baseWalk, baseSprint, potionBonus);
        double dy = to.getY() - from.getY();
        boolean airborne = !player.isOnGround() && Math.abs(dy) > 0.005;
        if (airborne) {
            max *= 1.6;
        } else {
            max *= 1.12;
        }
        // Swieze ladowanie po skoku: ped z lotu jeszcze sie rozladowuje.
        if (!airborne && now - data.lastLandMs < 500L) {
            max *= 1.25;
        }
        // Zbieg ze wzniesienia legalnie przekracza limit plaskiego sprintu.
        if (dy < -0.15) {
            max += 0.25;
        }
        int ping = PingUtil.getPing(player, data);
        max = MoveUtil.applyLeniency(max, PingUtil.leniency(plugin.getConfig(), ping));
        // Okno po odrzucie: knockback moze wciaz dopychac.
        if (sinceKb < 3500L) {
            max += 0.15;
        }

        if (horizontal > max && horizontal > 0.05) {
            data.speedStreak++;
            int streakLimit = cfgInt("streak", 6);
            if (data.speedStreak >= streakLimit) {
                flag(player, data, String.format("dist=%.3f max=%.3f ping=%d sprint=%b",
                        horizontal, max, ping, player.isSprinting()));
                data.speedStreak = 0;
                perTickPushback(player, data, from);
            } else if (data.speedStreak == 4) {
                suspicious(player, data, String.format("speed narasta %.3f vs %.3f",
                        horizontal, max));
            }
        } else {
            data.speedStreak = Math.max(0, data.speedStreak - 1);
        }
    }
}
