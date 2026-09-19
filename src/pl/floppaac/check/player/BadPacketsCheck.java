package pl.floppaac.check.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;

/**
 * BadPacketsA: pakiety ktorych vanilla klient nigdy nie wysyla.
 * Niemozliwy pitch, ruch po smierci, flaga lotu bez uprawnien.
 */
public class BadPacketsCheck extends Check {

    public BadPacketsCheck(FloppaAC plugin) {
        super(plugin, "BadPacketsA", CheckType.PLAYER);
    }

    public void handleMove(Player player, PlayerData data, Location to) {
        float pitch = to.getPitch();
        if (pitch < -90.5f || pitch > 90.5f) {
            flag(player, data, "pitch=" + pitch);
            return;
        }
        if (player.isDead()) {
            flag(player, data, "moved-while-dead");
            return;
        }
        if (player.isFlying() && !player.getAllowFlight()) {
            flag(player, data, "flying-no-permission");
        }
    }
}
