package pl.floppaac.data;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Przechowuje PlayerData oraz decay VL. */
public class DataManager {

    private final Map<UUID, PlayerData> data = new ConcurrentHashMap<UUID, PlayerData>();

    public PlayerData get(Player player) {
        return data.computeIfAbsent(player.getUniqueId(), PlayerData::new);
    }

    public PlayerData get(UUID uuid) {
        return data.computeIfAbsent(uuid, PlayerData::new);
    }

    public PlayerData find(UUID uuid) {
        return data.get(uuid);
    }

    public void remove(Player player) {
        data.remove(player.getUniqueId());
    }

    public void remove(UUID uuid) {
        data.remove(uuid);
    }

    public int tracked() {
        return data.size();
    }

    public void decayAll() {
        for (PlayerData d : data.values()) {
            d.decay();
        }
    }
}
