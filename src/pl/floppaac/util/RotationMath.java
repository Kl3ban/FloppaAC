package pl.floppaac.util;

import java.util.Collection;

public final class RotationMath {

    public static final double MIN_DELTA = 0.005;

    private static final double EDGE = 0.02;

    private static final long MIN_DIVISOR = Math.round(0.0005 * 16777216.0);

    private static final long MAX_DIVISOR = Math.round(10.0 * 16777216.0);

    public static final double FIXED_SCALE = 16777216.0;

    private RotationMath() {
    }

    public static double wrapDegrees(double d) {
        d = d % 360.0;
        if (d >= 180.0) {
            d -= 360.0;
        }
        if (d < -180.0) {
            d += 360.0;
        }
        return d;
    }

    public static long toFixed(double delta) {
        return Math.round(delta * FIXED_SCALE);
    }

    public static long gcd(long a, long b) {
        while (b != 0L) {
            long t = a % b;
            a = b;
            b = t;
        }
        return Math.abs(a);
    }

    public static long gcdOf(Collection<Double> deltas) {
        long g = 0L;
        int used = 0;
        for (Double d : deltas) {
            if (d == null) {
                continue;
            }
            double a = Math.abs(d.doubleValue());
            if (a < MIN_DELTA) {
                continue;
            }
            used++;
            g = gcd(g, toFixed(a));
            if (g <= 0L) {
                return 0L;
            }
        }
        if (used < 3 || g < MIN_DIVISOR || g > MAX_DIVISOR) {
            return 0L;
        }
        return g;
    }

    public static boolean onGridFixed(long f, long divisor) {
        long rem = Math.abs(f) % divisor;
        double ratio = (double) rem / (double) divisor;
        return ratio < EDGE || ratio > 1.0 - EDGE;
    }

    public static boolean onGrid(double delta, long divisor) {
        if (divisor <= 0L) {
            return true;
        }
        return onGridFixed(toFixed(delta), divisor);
    }

    public static int countOffGrid(Collection<Double> deltas, long divisor) {
        if (divisor <= 0L) {
            return 0;
        }
        int off = 0;
        int used = 0;
        for (Double d : deltas) {
            if (d == null) {
                continue;
            }
            double a = Math.abs(d.doubleValue());
            if (a < MIN_DELTA) {
                continue;
            }
            used++;
            if (!onGridFixed(toFixed(a), divisor)) {
                off++;
            }
        }
        return used >= 4 ? off : 0;
    }

    public static long bestDivisor(Collection<Double> deltas) {
        long[] fixed = new long[32];
        int n = 0;
        long minF = Long.MAX_VALUE;
        for (Double d : deltas) {
            if (d == null) {
                continue;
            }
            double a = Math.abs(d.doubleValue());
            if (a < MIN_DELTA) {
                continue;
            }
            fixed[n] = toFixed(a);
            if (fixed[n] < minF) {
                minF = fixed[n];
            }
            n++;
            if (n >= 32) {
                break;
            }
        }
        if (n < 4) {
            return 0L;
        }
        long best = 0L;
        int bestCover = 0;
        for (int c = 0; c < n + 8; c++) {
            long cand;
            if (c < n) {
                cand = fixed[c];
            } else {
                int k = c - n + 1;
                if (minF == Long.MAX_VALUE || minF / k < MIN_DIVISOR) {
                    continue;
                }
                cand = minF / k;
            }
            if (cand < MIN_DIVISOR || cand > MAX_DIVISOR) {
                continue;
            }
            int cover = 0;
            for (int i = 0; i < n; i++) {
                if (onGridFixed(fixed[i], cand)) {
                    cover++;
                }
            }
            if (cover > bestCover) {
                bestCover = cover;
                best = cand;
                if (cover == n) {
                    return best;
                }
            }
        }
        return bestCover > n / 2 ? best : 0L;
    }
}
