package pl.floppaac.check.player;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.OreStats;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class XrayCheck extends Check {

    private final Map<UUID, Deque<Long>> windows = new HashMap<UUID, Deque<Long>>();
    private final Map<UUID, Integer> losStreak = new HashMap<UUID, Integer>();

    public XrayCheck(FloppaAC plugin) {
        super(plugin, "XrayA", CheckType.PLAYER);
    }

    public void handle(Player player, PlayerData data, Block broken) {
        if (player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        UUID id = player.getUniqueId();
        Deque<Long> window = windows.get(id);
        if (window == null) {
            window = new ArrayDeque<Long>();
            windows.put(id, window);
        }
        String n = broken.getType().name();
        boolean ore = OreStats.isValuableOre(n);
        boolean waste = OreStats.isWaste(n);
        if (!ore && !waste) {
            return;
        }
        boolean los = hadLineOfSight(player, broken);
        int streak = 0;
        if (!los) {
            streak = losStreak.containsKey(id) ? losStreak.get(id) + 1 : 1;
            losStreak.put(id, streak);
        } else {
            losStreak.put(id, 0);
        }
        OreStats.push(window, ore ? OreStats.ORE : OreStats.WASTE,
                cfgInt("window", 30));
        if (ore && !los && OreStats.xrayLosFlag(streak, cfgInt("los-streak", 4))) {
            flag(player, data, n + " no-los x" + streak);
            losStreak.put(id, 0);
        }
        if (OreStats.xrayRatioFlag(window,
                cfgInt("window", 30),
                cfgInt("min-ores", 8),
                cfgDouble("max-waste-per-ore", 1.2))) {
            int w = 0;
            int o = 0;
            for (long v : window) {
                if (v == OreStats.ORE) {
                    o++;
                } else if (v == OreStats.WASTE) {
                    w++;
                }
            }
            flag(player, data, String.format("ratio %.2f waste %d ore %d",
                    w / (double) Math.max(1, o), w, o));
            window.clear();
            losStreak.put(id, 0);
        }
    }

    private boolean hadLineOfSight(Player player, Block block) {
        try {
            org.bukkit.util.Vector target = block.getLocation()
                    .add(0.5, 0.5, 0.5).toVector();
            org.bukkit.util.Vector dir = target.subtract(
                    player.getEyeLocation().toVector());
            double dist = dir.length();
            if (dist < 0.1) {
                return true;
            }
            RayTraceResult r = player.getWorld().rayTraceBlocks(
                    player.getEyeLocation(), dir, Math.min(dist + 0.5, 16.0),
                    FluidCollisionMode.NEVER, true);
            return r != null && r.getHitBlock() != null
                    && r.getHitBlock().equals(block);
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            return true;
        }
    }

    public void reset(UUID id) {
        losStreak.put(id, 0);
    }

    public void remove(UUID id) {
        windows.remove(id);
        losStreak.remove(id);
    }
}
