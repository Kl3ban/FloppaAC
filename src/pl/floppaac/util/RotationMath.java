package pl.floppaac.util;

import java.util.Collection;

/**
 * Matematyka rotacji dla GCD (KillAuraJ).
 * Legalny klient kwantyzuje ruch myszy wzgledem sensywnosci:
 * kazda delta yaw/pitch jest wielokrotnoscia jednego dzielnika.
 * Aim z assistem / silent aim generuje delty poza siatka.
 *
 * Fixed-point 2^24 = rozdzielczosc mantysy float32, wiec modulo
 * jest dokladne dla legalnych delt i odporne na float error.
 *
 * Dwa sposoby wyznaczania dzielnika:
 *  - gcdOf: klasyczne NWD calego okna (szybkie, ale 1-2 LSB bledu
 *    float w deltach zwala NWD do 1, ktory nigdy nie przechodzi
 *    progu MIN_DIVISOR - wowczas wraca 0 i okno jest nieocenione),
 *  - bestDivisor: glosowanie parami (odporne na rounding). Kandydatow
 *    bierzemy z NWD par delt, punktujemy pokryciem siatki. Legalny
 *    gracz: pokrycie pelne. Aim off-grid: najlepszy kandydat
 *    (sensywnosc) pokrywa wiekszosc, reszta delt lezy poza siatka.
 */
public final class RotationMath {

    /** Prog istotnej delty w stopniach (mikroruchy gubia kwantyzacje). */
    public static final double MIN_DELTA = 0.005;

    /** Tolerancja rezyduum modulo: brzeg okna (2%) = on-grid (float). */
    private static final double EDGE = 0.02;

    /** Dolna granica dzielnika: 0.0005 stopnia (ekstremalna senska). */
    private static final long MIN_DIVISOR = Math.round(0.0005 * 16777216.0);

    /** Gorna granica dzielnika: 10 stopni (nierzeczywista senska). */
    private static final long MAX_DIVISOR = Math.round(10.0 * 16777216.0);

    /** Fixed-point scale: 2^24. */
    public static final double FIXED_SCALE = 16777216.0;

    private RotationMath() {
    }

    /** Normalizacja kata do [-180, 180] (wrap przy pelnym obrocie). */
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

    /** Delta w stopniach do fixed-point (mnozenie przez 2^24). */
    public static long toFixed(double delta) {
        return Math.round(delta * FIXED_SCALE);
    }

    /** NWD dwoch liczb calkowitych (Euklides), wynik nieujemny. */
    public static long gcd(long a, long b) {
        while (b != 0L) {
            long t = a % b;
            a = b;
            b = t;
        }
        return Math.abs(a);
    }

    /**
     * Klasyczne NWD okna delt (fixed-point). Delty ponizej MIN_DELTA
     * pomijane. Zwraca 0 gdy okno za male albo NWD spadl ponizej
     * MIN_DIVISOR (float rounding) - wowczas okazuje sie nieocenialne
     * klasycznie i nalezy uzyc bestDivisor.
     */
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

    /** On-grid w fixed-point: rezyduum modulo przy brzegu (2%) = grid. */
    public static boolean onGridFixed(long f, long divisor) {
        long rem = Math.abs(f) % divisor;
        double ratio = (double) rem / (double) divisor;
        return ratio < EDGE || ratio > 1.0 - EDGE;
    }

    /** On-grid dla delty w stopniach przy zadanym dzielniku. */
    public static boolean onGrid(double delta, long divisor) {
        if (divisor <= 0L) {
            return true;
        }
        return onGridFixed(toFixed(delta), divisor);
    }

    /**
     * Liczba delt off-grid w oknie przy zadanym dzielniku. Okno musi
     * miec co najmniej 4 istotne delty, inaczej 0 (za malo danych =
     * brak sygnalu, nie wykrycie).
     */
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

    /**
     * Najlepszy dzielnik siatki z okna delt (glosowanie kandydatow).
     * Kandydaci: NWD kazdej pary delt oraz minDelta/k (k=1..8) -
     * legalne delty maja wspolna siatke sensywnosci, wiec najmniejsza
     * delta podzielona przez n da siatke z dokladnoscia do LSB (błąd
     * float32 ~1-2 LSB jest o rzad wielkosci mniejszy od brzegu 2%).
     * Punktacja: ile delt lezy na siatce kandydata; wczesne wyjscie
     * przy pelnym pokryciu. Zwraca 0 gdy okno < 4 istotnych delt albo
     * zadna siatka nie pokrywa wiecej niz polowe okna.
     */
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
