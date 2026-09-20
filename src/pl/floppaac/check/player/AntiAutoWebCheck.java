package pl.floppaac.check.player;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;

public class AntiAutoWebCheck extends Check {

    public AntiAutoWebCheck(FloppaAC plugin) {
        super(plugin, "WebB", CheckType.PLAYER);
    }

    public void handle(Player player, PlayerData data, Block placed) {
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE
                || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return;
        }
        if (placed.getType() != Material.COBWEB) {
            return;
        }
        boolean onEntity = false;
        try {
            for (Entity e : player.getWorld().getNearbyEntities(
                    placed.getLocation().add(0.5, 0.5, 0.5), 0.9, 0.9, 0.9)) {
                if (e instanceof LivingEntity && !e.equals(player)) {
                    onEntity = true;
                    break;
                }
            }
        } catch (NoSuchMethodError err) {
            return;
        }
        if (!onEntity) {
            return;
        }
        long now = System.currentTimeMillis();
        java.util.ArrayDeque<Long> q = data.webOnPlayerTimes;
        while (!q.isEmpty() && now - q.peekFirst() > 3000L) {
            q.pollFirst();
        }
        q.addLast(now);
        if (q.size() >= 4) {
            flag(player, data, "web on entity x" + q.size() + "/3s");
            q.clear();
        }
    }
}
