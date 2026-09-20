package pl.floppaac.util;

import java.util.Deque;

public final class ClickStats {

    private ClickStats() {
    }

    public static double mean(Deque<Long> gaps) {
        if (gaps.isEmpty()) {
            return 0.0;
        }
        long sum = 0L;
        for (long g : gaps) {
            sum += g;
        }
        return (double) sum / (double) gaps.size();
    }

    public static double stddev(Deque<Long> gaps) {
        if (gaps.size() < 2) {
            return Double.MAX_VALUE;
        }
        double m = mean(gaps);
        double acc = 0.0;
        for (long g : gaps) {
            double d = (double) g - m;
            acc += d * d;
        }
        return Math.sqrt(acc / (double) gaps.size());
    }

    public static double cps(Deque<Long> hitTimes, long windowMs) {
        if (hitTimes.isEmpty()) {
            return 0.0;
        }
        long now = hitTimes.peekLast().longValue();
        long cutoff = now - windowMs;
        int n = 0;
        for (long t : hitTimes) {
            if (t >= cutoff) {
                n++;
            }
        }
        return (double) n * 1000.0 / (double) windowMs;
    }

    public static int classify(Deque<Long> gaps, Deque<Long> hitTimes) {
        if (gaps.size() < 30 || hitTimes.size() < 10) {
            return 0;
        }
        double rate = cps(hitTimes, 1000L);
        if (rate > 20.0) {
            return 1;
        }
        int tiny = 0;
        for (long g : gaps) {
            if (g < 40L) {
                tiny++;
            }
        }
        if (tiny >= 3) {
            return 3;
        }
        if (rate > 10.0 && stddev(gaps) < 2.0) {
            return 2;
        }
        return 0;
    }
}
