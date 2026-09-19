package pl.floppaac.check.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;

/**
 * NoSlowA: pelna predkosc podczas blokowania, jedzenia i celowania.
 * Vanilla tarcza w gore albo jedzenie to okolo 60 procent predkosci.
 * Prog liczony z modelu SpeedCheck (mikstura i lod), wymagana seria
 * 4 tickow, bo desync isHandRaised przy pingu 60 daje 1-2 ticki.
 */
public class NoSlowCheck extends Check {

    public NoSlowCheck(FloppaAC plugin) {
        super(plugin, "NoSlowA", CheckType.PLAYER);
    }

    public void handle(Player player, PlayerData data, Location from, Location to,
                       double horizontal) {
        if (data.movementExempt() || MoveUtil.canFly(player) || MoveUtil.inVehicle(player)
                || MoveUtil.inLiquid(player) || MoveUtil.onIce(player)
                || data.velocityExempt(player, System.currentTimeMillis())) {
            data.noSlowStreak = 0;
            return;
        }
        boolean slowed;
        try {
            slowed = player.isBlocking() || player.isHandRaised();
        } catch (NoSuchMethodError e) {
            return;
        }
        if (!slowed) {
            data.noSlowStreak = 0;
            return;
        }
        double max = MoveUtil.maxHorizontal(player, 0.2155, 0.2806, 0.06) * 0.65;
        if (player.isOnGround() && horizontal > max && horizontal > 0.1) {
            data.noSlowStreak++;
            if (data.noSlowStreak >= 4) {
                flag(player, data, String.format("dist=%.3f max=%.3f blok=%b",
                        horizontal, max, player.isBlocking()));
                data.noSlowStreak = 0;
                // Flaga = cofniecie: noslowdown traci nadmierne ticki.
                perTickPushback(player, data, from);
            }
        } else {
            data.noSlowStreak = Math.max(0, data.noSlowStreak - 1);
            if (slowed && horizontal > max - 0.03) {
                suspicious(player, data, String.format("szybko ze spowolnieniem %.3f", horizontal));
            }
        }
    }
}
