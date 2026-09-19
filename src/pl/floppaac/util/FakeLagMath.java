package pl.floppaac.util;

import java.util.Deque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Czysta matematyka FakeLag bez Bukkita. Testowalna bez serwera.
 *
 * FakeLag rozni sie od zwyklego laga trzema cechami:
 * A) choke: dluga luka w ruchu pozycyjnym zakonczona burstem
 *    wielu ruchow w krotkim oknie,
 * B) atak w luce: trafienie przychodzi mimo braku ruchu,
 *    a ping jest stabilny (prawdziwy lag blokuje tez ataki),
 * C) desync czasow: ruch przychodzi rownymi mikrodawkami zamiast
 *    co tick 50 ms, albo jitter ruchu przy pingu bliskim zero.
 *
 * Rozrodnienie legalnego laga FPS od fakelaga (fizyka):
 *  - zamrozenie klienta (FPS, stall) NIE generuje pakietow; po
 *    wznowieniu klient wysyla OBECNA pozycje, czyli jeden wielki
 *    skok rowny oczekiwanej drogi za cala luke,
 *  - fakelag wstrzymuje pakiety WYGENEROWANE przez dzialajacy
 *    klient; flush to wiele pakietow o normalnych deltach, skok
 *    pierwszego pakietu jest maly.
 */
public final class FakeLagMath {

    private FakeLagMath() {
    }

    /**
     * Czy luka plus burst wyglada na choke.
     * @param gapMs przerwa przed burstem w ms
     * @param burstMoves liczba ruchow pozycyjnych w 200 ms po luce
     * @param pingMs aktualny ping gracza
     */
    public static boolean isChoke(long gapMs, int burstMoves, int pingMs) {
        if (pingMs > 220) {
            return false;
        }
        return gapMs >= 300L && burstMoves >= 6;
    }

    /**
     * Czy atak w luce ruchu jest podejrzany.
     * @param sinceMoveMs czas od ostatniego ruchu pozycyjnego w ms
     * @param pingMs aktualny ping gracza
     */
    public static boolean isAttackDuringGap(long sinceMoveMs, int pingMs) {
        if (pingMs > 220) {
            return false;
        }
        return sinceMoveMs >= 250L;
    }

    /** Ile ruchow z kolejki miesci sie w oknie od znacznika now wstecz. */
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

    /** Maksymalny odstep w kolejce ruchow. */
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

    /**
     * Ile blokow gracz legalnie przebylby podczas luki gapMs.
     * Klient tickuje 20 Hz niezaleznie od FPS: zamarzniecie na
     * 300 ms odklada 6 tickow ruchu, ktore wroca jednym skokiem.
     * Sprint-jump arc daje do 0.5 na tick, wiec licze po 0.33.
     */
    public static double expectedCatchupBlocks(long gapMs) {
        long ticks = Math.max(0L, gapMs / 50L);
        return ticks * 0.33;
    }

    /**
     * Czy skok pozycji po luce wyglada na zamarzniecie klienta.
     * Skok rowny conajmniej 55 procent oczekiwanej drogi = legalny
     * lag (brak pakietow w ogole). Mniejszy = pakiety byly trzymane
     * (flush normalnych delt), co jest sygnaturem fakelaga.
     */
    public static boolean isClientFreeze(double skipBlocks, long gapMs) {
        return skipBlocks >= 0.55 * expectedCatchupBlocks(gapMs);
    }

    /**
     * Czy krotka luka plus burst wyglada na choke MoonLight (okolo 200 ms).
     * Slaby sygnal osobno, wymaga serii epizodow.
     */
    public static boolean isShortChoke(long gapMs, int burstMoves, int pingMs) {
        if (pingMs > 220) {
            return false;
        }
        return gapMs >= 180L && gapMs < 300L && burstMoves >= 4;
    }

    /** Mediana odstepow miedzy ruchami w oknie od now wstecz. */
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

    /**
     * Desync czasow dostarczania ruchu: ruch plynie rownymi
     * mikrodawkami (dryf co najmniej 75 procent ticku) przy gestosci
     * co najmniej 10 ruchow na sekunde i mediance odstepow
     * ponizej 30 ms przy zdrowym laczu (ping do 80 ms).
     * Legalny klient ma ticki rowne 50 ms - mediana rosnie.
     * Progi ostre, bo sygnal C najczesciej dawal FP na rownym laczu.
     */
    public static boolean isDesyncJitter(double tickDriftPct, long medianIntervalMs,
                                         int movesInLastSecond, int pingEmaMs) {
        if (pingEmaMs > 80 || movesInLastSecond < 10 || medianIntervalMs < 0L) {
            return false;
        }
        return tickDriftPct >= 75.0 && medianIntervalMs < 30L;
    }
}
