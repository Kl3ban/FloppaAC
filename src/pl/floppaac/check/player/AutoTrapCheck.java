package pl.floppaac.check.player;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;

/**
 * AutoTrapA: automatyczne zabudowywanie przeciwnika boxem.
 * Sygnatura: solidny blok postawiony w boksie wokol ZYWEJ istoty
 * (6 pionow wokol celu: N/S/E/W na poziomie stopy i glowy plus
 * nad glowa), 8+ w 3 sekundy. Czlowiek buduje pukszaltke na gruncie;
 * dokladanie blokow bezposrednio przy glowie celu to skrypt.
 */
public class AutoTrapCheck extends Check {

    public AutoTrapCheck(FloppaAC plugin) {
        super(plugin, "AutoTrapA", CheckType.PLAYER);
    }

    public void handle(Player player, PlayerData data, Block placed) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE
                || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }
        if (!placed.getType().isSolid()) {
            return;
        }
        boolean nearVictim = false;
        try {
            for (org.bukkit.entity.Entity e : player.getWorld().getNearbyEntities(
                    placed.getLocation().add(0.5, 0.5, 0.5), 1.6, 1.9, 1.6)) {
                if (e instanceof org.bukkit.entity.LivingEntity && !e.equals(player)) {
                    nearVictim = true;
                    break;
                }
            }
        } catch (NoSuchMethodError err) {
            return;
        }
        if (!nearVictim) {
            return;
        }
        long now = System.currentTimeMillis();
        java.util.ArrayDeque<Long> q = data.boxOnVictimTimes;
        while (!q.isEmpty() && now - q.peekFirst() > 3000L) {
            q.pollFirst();
        }
        q.addLast(now);
        if (q.size() >= 8) {
            flag(player, data, "box na celu x" + q.size() + "/3s");
            q.clear();
        }
    }
}
