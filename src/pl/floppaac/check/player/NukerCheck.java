package pl.floppaac.check.player;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;

/**
 * NukerA: 3 bloki nie instant w 100 ms.
 * Wlasna kolejka nukeTimes, niezalezna od FastBreak
 * (FastBreak czysci swoja kolejke przy fladze).
 */
public class NukerCheck extends Check {

    public NukerCheck(FloppaAC plugin) {
        super(plugin, "NukerA", CheckType.PLAYER);
    }

    public void handle(Player player, PlayerData data, Block broken) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (FastBreakCheck.isInstant(broken.getType())) {
            return;
        }
        long now = System.currentTimeMillis();
        PlayerData.pushCapped(data.nukeTimes, now, 30);
        int in100 = 0;
        for (long t : data.nukeTimes) {
            if (now - t <= 100L) {
                in100++;
            }
        }
        if (in100 >= 3) {
            flag(player, data, in100 + " w 100ms");
            data.nukeTimes.clear();
        }
    }
}
