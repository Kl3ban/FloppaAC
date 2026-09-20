package pl.floppaac.check.movement;

import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.FakeLagMath;
import pl.floppaac.util.MoveUtil;
import pl.floppaac.util.PingUtil;

public class FakeLagCheck extends Check {

    private static final long STREAK_EXPIRY_MS = 60000L;

    public FakeLagCheck(FloppaAC plugin) {
        super(plugin, "FakeLagA", CheckType.MOVEMENT);
    }

    private boolean flagLag(String vlName, Player player, PlayerData data, String details) {
        data.lastFakeLagMs = System.currentTimeMillis();
        return flagAs(vlName, player, data, details);
    }

    private void suspLag(Player player, PlayerData data, String details) {
        data.lastFakeLagMs = System.currentTimeMillis();
        suspicious(player, data, details);
    }

    public void handleMove(Player player, PlayerData data, long gapMs) {
        if (data.movementExempt()) {

            return;
        }
        if (MoveUtil.canFly(player) || MoveUtil.inVehicle(player)) {
            data.chokeStreak = 0;
            data.shortChokeStreak = 0;
            return;
        }
        long now = System.currentTimeMillis();
        int ping = PingUtil.getPing(player, data);
        double tps = plugin.getTpsMonitor().getTps();
        if (ping > 220 || tps < 18.5) {
            data.chokeStreak = 0;
            data.shortChokeStreak = 0;
            return;
        }

        data.lastRawGapMs = gapMs;
        data.lastRawGapAt = now;

        if (gapMs > 2000L) {
            data.chokeStreak = 0;
            data.shortChokeStreak = 0;
            data.lastMoveGapMs = 0L;
        }

        if (now - data.lastChokeMs > STREAK_EXPIRY_MS) {
            data.chokeStreak = 0;
        }
        if (now - data.lastShortChokeMs > STREAK_EXPIRY_MS) {
            data.shortChokeStreak = 0;
        }

        long gapThreshold = (long) cfgInt("min-gap-ms", 300);
        int burstNeeded = cfgInt("min-burst", 6);

        double skip = 0.0;
        boolean skipKnown = false;
        if (data.lastLoc != null && data.lastLoc.getWorld() != null
                && player.getWorld().equals(data.lastLoc.getWorld())) {
            skip = Math.hypot(
                    player.getX() - data.lastLoc.getX(),
                    player.getZ() - data.lastLoc.getZ());
            skipKnown = true;
        }
        boolean clientFreeze = skipKnown
                && FakeLagMath.isClientFreeze(skip, gapMs);

        if (gapMs >= 180L && clientFreeze) {

            data.chokeStreak = 0;
            data.shortChokeStreak = 0;
            data.lastMoveGapMs = 0L;
            data.lastFreezeMs = now;
            return;
        }

        if (gapMs >= gapThreshold) {
            data.lastChokeMs = now;
            if (skipKnown && skip < 0.05 && data.lastHorizontalDist < 0.05) {

                data.lastMoveGapMs = 0L;
            } else {
                data.lastMoveGapMs = gapMs;
                data.chokeStreak++;
                if (data.chokeStreak == 1) {
                    suspLag(player, data, String.format(
                            "move gap %dms ping=%d (observation)", gapMs, ping));
                }
            }
        } else if (gapMs >= 180L) {

            if (!(skipKnown && skip < 0.05 && data.lastHorizontalDist < 0.05)) {
                data.lastShortChokeMs = now;
            }
        }

        int burst = FakeLagMath.burstInWindow(data.moveTimes, now, 200L);
        data.lastBurstSize = burst;

        boolean freezeAsylum = now - data.lastFreezeMs < 2500L;

        if (!freezeAsylum && data.lastMoveGapMs >= gapThreshold && burst >= burstNeeded) {
            if (data.chokeStreak >= cfgInt("episodes", 2)) {
                flagLag("FakeLagA", player, data, String.format(
                        "choke gap=%dms burst=%d/200ms ping=%d tps=%.1f",
                        data.lastMoveGapMs, burst, ping, tps));
                data.chokeStreak = 0;
                data.lastMoveGapMs = 0L;
            } else {
                suspLag(player, data, String.format(
                        "burst after gap %d/200ms (episode %d)", burst, data.chokeStreak));
            }
        } else if (!freezeAsylum && FakeLagMath.isShortChoke(gapMs, burst, ping)) {

            data.shortChokeStreak++;
            int need = cfgInt("short-episodes", 5);
            if (data.shortChokeStreak >= need) {
                flagLag("FakeLagA", player, data, String.format(
                        "krotki choke gap=%dms burst=%d/200ms x%d ping=%d",
                        gapMs, burst, data.shortChokeStreak, ping));
                data.shortChokeStreak = 0;
            } else if (data.shortChokeStreak == 2) {
                suspLag(player, data, String.format(
                        "short gaps %dms (episode %d)", gapMs, data.shortChokeStreak));
            }
        }

        long median = FakeLagMath.medianInterval(data.moveTimes, now, 1000L);
        double driftPct = 0.0;
        if (median > 0L && median < 50L) {
            driftPct = (1.0 - ((double) median / 50.0)) * 100.0;
        }
        int movesLastSec = FakeLagMath.burstInWindow(data.moveTimes, now, 1000L);

        boolean recoveryAsylum = freezeAsylum
                || now - data.lastChokeMs < 2500L
                || now - data.lastShortChokeMs < 2500L
                || (data.lastRawGapMs >= 100L && now - data.lastRawGapAt < 2500L);
        if (!recoveryAsylum && FakeLagMath.isDesyncJitter(driftPct, median, movesLastSec, data.pingEma)) {
            if (now - data.lastDesyncFlagMs > 8000L) {
                data.lastDesyncFlagMs = now;
                flagLag("FakeLagC", player, data, String.format(
                        "timing-desync median=%dms moves/s=%d ping=%d drift=%.0f%%",
                        median, movesLastSec, ping, driftPct));
            } else {
                suspLag(player, data, String.format(
                        "timing-desync median=%dms drift=%.0f%%", median, driftPct));
            }
        }
    }

    public boolean handleAttack(Player player, PlayerData data) {
        if (data.movementExempt() || MoveUtil.canFly(player) || MoveUtil.inVehicle(player)) {
            return false;
        }
        long now = System.currentTimeMillis();
        long sinceMove = now - data.lastMoveMs;
        if (sinceMove < 0L) {
            return false;
        }
        int ping = PingUtil.getPing(player, data);
        double tps = plugin.getTpsMonitor().getTps();
        if (ping > 220 || tps < 18.5) {
            return false;
        }
        long attackGap = (long) cfgInt("attack-gap-ms", 250);
        if (FakeLagMath.isAttackDuringGap(sinceMove, ping) && sinceMove >= attackGap) {

            if (data.lastHorizontalDist < 0.03) {
                suspLag(player, data, String.format(
                        "attack %dms after move (camping?)", sinceMove));
                return false;
            }

            int recentMoves = FakeLagMath.burstInWindow(data.moveTimes, now, 2000L);
            if (recentMoves >= 6) {
                return flagLag("FakeLagB", player, data, String.format(
                        "attack in move gap %dms ping=%d tps=%.1f",
                        sinceMove, ping, tps));
            }
            suspLag(player, data, String.format(
                    "attack %dms after move (camping?)", sinceMove));
        }
        return false;
    }
}
