package pl.floppaac.check.player;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;

public class ScaffoldCheck extends Check {

    public ScaffoldCheck(FloppaAC plugin) {
        super(plugin, "ScaffoldA", CheckType.PLAYER);
    }

    public void handle(Player player, PlayerData data, Block placed) {
        if (player.isFlying()) {
            data.airPhasePlaces = 0;
            data.bridgeJumps = 0;
            return;
        }
        Location pl = player.getLocation();
        double dx = (double) placed.getX() + 0.5 - pl.getX();
        double dz = (double) placed.getZ() + 0.5 - pl.getZ();
        double dy = (double) placed.getY() - pl.getY();
        boolean underFeet = Math.hypot(dx, dz) < 2.2 && dy <= 0.0 && dy >= -3.0;
        if (!underFeet) {
            return;
        }

        if (player.isOnGround()) {

            if (data.airPhasePlaces >= 4) {
                data.airPhasePlaces = 0;
            }
            if (data.lastBridgeJumpMs > 0L && data.bridgeJumps > 0
                    && System.currentTimeMillis() - data.lastBridgeJumpMs > 800L) {
                data.bridgeJumps = 0;
            }
            return;
        }
        if (data.airTicks < 3) {
            return;
        }

        long now = System.currentTimeMillis();

        if (!player.isSneaking()) {
            data.airPhasePlaces++;
            if (data.airPhasePlaces >= 4) {
                flag(player, data, "air-bridge x" + data.airPhasePlaces);
                data.airPhasePlaces = 0;
            }
        } else {
            data.airPhasePlaces = 0;
        }

        if (data.airTicks <= 6 && now - data.lastBridgeJumpMs < 250L) {
            data.bridgeJumps++;
            if (data.bridgeJumps >= 3) {
                flag(player, data, "jump-bridge x" + data.bridgeJumps);
                data.bridgeJumps = 0;
            }
        } else if (data.airTicks <= 6) {
            data.bridgeJumps = Math.max(0, data.bridgeJumps - 1);
        }
        if (data.airTicks <= 6) {
            data.lastBridgeJumpMs = now;
        }
    }
}
