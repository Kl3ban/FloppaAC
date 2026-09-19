package pl.floppaac.check.player;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;

/**
 * InventoryA: ChestStealer, ponad 12 klikow na s w ekwipunku.
 * InventoryB: akcje walki i sprintu z otwartym GUI.
 * Legalny klient zamyka GUI przed atakiem (Close Window idzie pierwsze),
 * a sprint z otwartym GUI jest niemozliwy. Atak z otwartym GUI w serii 2
 * oraz sprint z GUI w serii 5 to sygnaly.
 */
public class InventoryCheck extends Check {

    public InventoryCheck(FloppaAC plugin) {
        super(plugin, "InventoryA", CheckType.PLAYER);
    }

    public void handle(Player player, PlayerData data) {
        long now = System.currentTimeMillis();
        PlayerData.pushCapped(data.invClickTimes, now, 30);
        long cutoff = now - 1000L;
        int n = 0;
        for (long t : data.invClickTimes) {
            if (t >= cutoff) {
                n++;
            }
        }
        if (n > cfgInt("max-clicks-per-second", 12)) {
            flag(player, data, n + " clicks/s");
            data.invClickTimes.clear();
        }
    }

    public void handleAttack(Player attacker, PlayerData data) {
        if (!data.invOpen) {
            data.invAtkStreak = 0;
            return;
        }
        if (attacker.getGameMode() == GameMode.CREATIVE
                || attacker.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        data.invAtkStreak++;
        if (data.invAtkStreak >= 2) {
            flagAs("InventoryB", attacker, data, "atak z GUI x" + data.invAtkStreak);
            data.invAtkStreak = 0;
        }
    }

    /**
     * InventoryC: obrazenia z otwartego ekwipunku (InventoryWalk).
     * Legalny klient zamyka GUI przed atakiem, wiec kazde trafienie
     * z otwartym GUI to pakietowy multiaction. Flaga od pierwszego
     * ciosu, listener anulowuje obrazenia (ghost trafienia).
     * @return true gdy trafienie poszlo z otwartego GUI
     */
    public boolean handleDamage(Player attacker, PlayerData data) {
        if (!data.invOpen) {
            return false;
        }
        if (attacker.getGameMode() == GameMode.CREATIVE
                || attacker.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        flagAs("InventoryC", attacker, data, "obrazenia z otwartym GUI");
        return true;
    }

    public void handleMove(Player player, PlayerData data, Location from, Location to,
                           double horizontal) {
        if (!data.invOpen) {
            data.invMoveStreak = 0;
            return;
        }
        // Obrot kamery z otwartym GUI (jak InventoryMove u Hawka).
        // W vanilla mysz obsluguje kursor, kamera stoi.
        boolean rotated = from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch();
        if (rotated && !data.movementExempt()) {
            data.invMoveStreak++;
            if (data.invMoveStreak >= 3) {
                flagAs("InventoryB", player, data, "obrot z GUI x" + data.invMoveStreak);
                data.invMoveStreak = 0;
            }
            return;
        }
        if (player.isSprinting() && horizontal > 0.25) {
            data.invMoveStreak++;
            if (data.invMoveStreak >= 5) {
                flagAs("InventoryB", player, data, "sprint z GUI x" + data.invMoveStreak);
                data.invMoveStreak = 0;
            }
        } else if (!rotated) {
            data.invMoveStreak = Math.max(0, data.invMoveStreak - 1);
        }
    }
}
