package pl.floppaac.check.combat;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;

public class MultiActionsCheck extends Check {

    private static final long RAISED_WINDOW_MS = 250L;

    public MultiActionsCheck(FloppaAC plugin) {
        super(plugin, "MultiA", CheckType.COMBAT);
    }

    public void handleMove(Player player, PlayerData data) {
        long now = System.currentTimeMillis();
        boolean raised;
        try {
            raised = player.isHandRaised();
        } catch (NoSuchMethodError e) {
            return;
        }
        if (raised) {
            data.lastRaiseSeenMs = now;
        } else {
            data.lastUnraiseSeenMs = now;
        }
    }

    public boolean handleAttack(Player attacker, PlayerData data) {
        if (data.movementExempt()) {
            data.multiAStreak = 0;
            return false;
        }

        if (data.invOpen) {
            data.multiAStreak = 0;
            return false;
        }
        if (attacker.getGameMode() == GameMode.CREATIVE
                || attacker.getGameMode() == GameMode.SPECTATOR) {
            data.multiAStreak = 0;
            return false;
        }
        boolean raisedNow;
        try {
            raisedNow = attacker.isHandRaised();
        } catch (NoSuchMethodError e) {
            return false;
        }
        long now = System.currentTimeMillis();
        boolean continuousRaised = now - data.lastRaiseSeenMs < RAISED_WINDOW_MS
                && data.lastRaiseSeenMs >= data.lastUnraiseSeenMs;
        boolean detected = raisedNow || continuousRaised;

        if (detected && !raisedNow && data.lastUnraiseSeenMs > data.lastRaiseSeenMs) {
            detected = false;
        }
        if (detected) {
            data.multiAStreak++;
            if (data.multiAStreak >= 3) {
                flagAs("MultiA", attacker, data, "hit with raised hand x"
                        + data.multiAStreak);
                data.multiAStreak = 0;
            } else {
                suspicious(attacker, data, "hit with raised hand ("
                        + data.multiAStreak + "/3, ghost)");
            }

            return true;
        }
        data.multiAStreak = 0;
        return false;
    }

    public void handleSwing(Player player, PlayerData data) {
        if (data.movementExempt()) {
            data.multiBStreak = 0;
            return;
        }
        if (data.invOpen) {
            data.multiBStreak = 0;
            return;
        }
        if (System.currentTimeMillis() - data.lastDropMs < 500L) {
            return;
        }
        boolean raised;
        try {
            raised = player.isHandRaised();
        } catch (NoSuchMethodError e) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean continuousRaised = now - data.lastRaiseSeenMs < RAISED_WINDOW_MS
                && data.lastRaiseSeenMs >= data.lastUnraiseSeenMs;
        if (raised || continuousRaised) {
            data.multiBStreak++;
            if (data.multiBStreak >= 3) {
                flagAs("MultiB", player, data, "swing with raised hand x"
                        + data.multiBStreak);
                data.multiBStreak = 0;
            }
        } else {
            data.multiBStreak = 0;
        }
    }
}
