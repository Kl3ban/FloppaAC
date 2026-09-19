package pl.floppaac.check.movement;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;

/**
 * PhaseA: przejscie przez pelny blok.
 * Oba konce ruchu w srodku solidnego bloku i dystans powyzej
 * 0.3 bloku. Wymagana seria 2 zeby krawedzie chunkow nie flagowaly.
 */
public class PhaseCheck extends Check {

    public PhaseCheck(FloppaAC plugin) {
        super(plugin, "PhaseA", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data, Location from, Location to) {
        if (data.movementExempt()) {
            data.phaseStreak = 0;
            return;
        }
        if (data.velocityExempt(player, System.currentTimeMillis())) {
            data.phaseStreak = 0;
            return;
        }
        boolean fromSolid = from.getBlock().getType().isSolid()
                && from.getBlock().getType().isOccluding();
        boolean toSolid = to.getBlock().getType().isSolid()
                && to.getBlock().getType().isOccluding();
        double dist = from.distance(to);
        if (fromSolid && toSolid && dist > 0.3) {
            phaseHit(player, data, to.getBlock().getType().name(), dist);
            return;
        }
        // Przelot powietrze-powietrze przez solidny srodek (NcpClip).
        // Sprint daje max okolo 0.4 na tick, wiec prog 0.5 jest bezpieczny.
        if (!fromSolid && !toSolid && dist > 0.5) {
            Location mid = from.clone().add(to.clone().subtract(from).multiply(0.5));
            if (mid.getBlock().getType().isSolid()
                    && mid.getBlock().getType().isOccluding()) {
                phaseHit(player, data, mid.getBlock().getType().name(), dist);
                return;
            }
        }
        data.phaseStreak = 0;
    }

    private void phaseHit(Player player, PlayerData data, String block, double dist) {
        data.phaseStreak++;
        if (data.phaseStreak >= 2) {
            flag(player, data, String.format("w bloku %s dist=%.2f",
                    block, dist));
            data.phaseStreak = 0;
        }
    }
}
