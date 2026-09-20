package pl.floppaac.check.movement;

import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;

public class TimerCheck extends Check {

    private static final long MAX_BEHIND_NS = 1000000000L;

    private static final long STREAK_WINDOW_NS = 2000000000L;

    public TimerCheck(FloppaAC plugin) {
        super(plugin, "TimerA", CheckType.MOVEMENT);
    }

    public void handle(Player player, PlayerData data) {
        long now = System.nanoTime();
        if (data.movementExempt() || MoveUtil.inVehicle(player)) {
            data.timerBalanceNs = 0L;
            data.timerStreak = 0;
            return;
        }
        if (data.timerBalanceNs == 0L) {
            data.timerBalanceNs = now;
            return;
        }
        data.timerBalanceNs += 50000000L;
        if (data.timerBalanceNs < now - MAX_BEHIND_NS) {
            data.timerBalanceNs = now - MAX_BEHIND_NS;
        }
        long drift = (long) cfgInt("drift-ms", 250) * 1000000L;
        if (data.timerBalanceNs > now + drift) {
            data.timerBalanceNs = now + drift;
            if (now - data.timerLastExceedNs > STREAK_WINDOW_NS) {
                data.timerStreak = 0;
            }
            data.timerLastExceedNs = now;
            data.timerStreak++;
            if (data.timerStreak >= 3) {
                flag(player, data, "balance x" + data.timerStreak);
                data.timerStreak = 0;
            } else if (data.timerStreak == 2) {
                suspicious(player, data, "timer drifting (observation)");
            }
        }
    }

    public void handleIdleGap(PlayerData data, long gapMs) {

        if (gapMs > 5000L) {
            data.timerBalanceNs = 0L;
            data.timerStreak = 0;
        }
    }
}
