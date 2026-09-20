package pl.floppaac.util;

import java.util.Deque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FakeLagMath {

    private FakeLagMath() {
    }

    public static boolean isChoke(long gapMs, int burstMoves, int pingMs) {
        if (pingMs > 220) {
            return false;
        }
        return gapMs >= 300L && burstMoves >= 6;
    }

    public static boolean isAttackDuringGap(long sinceMoveMs, int pingMs) {
        if (pingMs > 220) {
            return false;
        }
        return sinceMoveMs >= 250L;
    }

    public static int burstInWindow(Deque<Long> moveTimes, long now, long windowMs) {
        long cutoff = now - windowMs;
        int n = 0;
        for (long t : moveTimes) {
            if (t >= cutoff) {
                n++;
            }
        }
        return n;
    }

    public static long maxGap(Deque<Long> moveTimes) {
        if (moveTimes.size() < 2) {
            return 0L;
        }
        long prev = -1L;
        long max = 0L;
        for (long t : moveTimes) {
            if (prev != -1L) {
                long gap = t - prev;
                if (gap > max) {
                    max = gap;
                }
            }
            prev = t;
        }
        return max;
    }

    public static double expectedCatchupBlocks(long gapMs) {
        long ticks = Math.max(0L, gapMs / 50L);
        return ticks * 0.33;
    }

    public static boolean isClientFreeze(double skipBlocks, long gapMs) {
        return skipBlocks >= 0.55 * expectedCatchupBlocks(gapMs);
    }

    public static boolean isShortChoke(long gapMs, int burstMoves, int pingMs) {
        if (pingMs > 220) {
            return false;
        }
        return gapMs >= 180L && gapMs < 300L && burstMoves >= 4;
    }

    public static long medianInterval(Deque<Long> moveTimes, long now, long windowMs) {
        long cutoff = now - windowMs;
        List<Long> gaps = new ArrayList<Long>();
        long prev = -1L;
        for (long t : moveTimes) {
            if (t >= cutoff && prev != -1L && t >= prev) {
                gaps.add(Long.valueOf(t - prev));
            }
            if (t >= cutoff) {
                prev = t;
            }
        }
        if (gaps.size() < 4) {
            return -1L;
        }
        Collections.sort(gaps);
        int mid = gaps.size() / 2;
        if ((gaps.size() & 1) == 1) {
            return gaps.get(mid).longValue();
        }
        return (gaps.get(mid - 1).longValue() + gaps.get(mid).longValue()) / 2L;
    }

    public static boolean isDesyncJitter(double tickDriftPct, long medianIntervalMs,
                                         int movesInLastSecond, int pingEmaMs) {
        if (pingEmaMs > 80 || movesInLastSecond < 10 || medianIntervalMs < 0L) {
            return false;
        }
        return tickDriftPct >= 75.0 && medianIntervalMs < 30L;
    }
}
