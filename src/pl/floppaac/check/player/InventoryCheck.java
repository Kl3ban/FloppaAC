package pl.floppaac.check.player;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;

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
            flagAs("InventoryB", attacker, data, "attack from GUI x" + data.invAtkStreak);
            data.invAtkStreak = 0;
        }
    }

    public boolean handleDamage(Player attacker, PlayerData data) {
        if (!data.invOpen) {
            return false;
        }
        if (attacker.getGameMode() == GameMode.CREATIVE
                || attacker.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        flagAs("InventoryC", attacker, data, "damage with open GUI");
        return true;
    }

    public void handleMove(Player player, PlayerData data, Location from, Location to,
                           double horizontal) {
        if (!data.invOpen) {
            data.invMoveStreak = 0;
            return;
        }

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
                flagAs("InventoryB", player, data, "sprint from GUI x" + data.invMoveStreak);
                data.invMoveStreak = 0;
            }
        } else if (!rotated) {
            data.invMoveStreak = Math.max(0, data.invMoveStreak - 1);
        }
    }
}
