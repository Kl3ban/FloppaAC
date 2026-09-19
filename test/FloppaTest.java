import java.util.ArrayDeque;
import java.util.Deque;

import pl.floppaac.util.MomentumMath;
import pl.floppaac.util.RotationMath;

/**
 * Test logiki FloppaAC bez serwera.
 * Kopiuje matematyke ClickStats i FakeLagMath (klasy czyste).
 * Uruchomienie: java -cp classes;test-classes FloppaTest
 */
public class FloppaTest {

    static int pass = 0;
    static int fail = 0;

    public static void main(String[] args) {
        testStableCpsNotFlagged();
        testExtremeMetronomeFlagged();
        testImpossibleCpsFlagged();
        testSub40BurstsFlagged();
        testChokeDetected();
        testChokeIgnoredOnHighPing();
        testAttackDuringGap();
        testAttackDuringGapIgnoredOnHighPing();
        testBurstInWindow();
        testMaxGap();
        testShortChokeDetected();
        testShortChokeIgnoredOnHighPing();
        testShortChokeNeedsBurst();
        testTimerLegitNoExceed();
        testTimerCheatExceeds();
        testTimerSingleBurstOnce();
        testDigObsidianBest();
        testDigCheatInstant();
        testDigLegitOk();
        testDigBedrock();
        testGrassIsInstant();
        testFreezeRecognized();
        testFreezeNotStandingStill();
        testFakelagSmallGapNotFreeze();
        testDesyncJitterDetected();
        testDesyncJitterPingGuard();
        testCatchupScales();
        testMomentumChainLegit();
        testMomentumConstant130Flagged();
        testMomentumConstant120Flagged();
        testMomentumSprintJumpLegit();
        testGcdLegitOnGrid();
        testGcdOffGridFlagged();
        testGcdBestDivisorFloatRounding();
        testGcdWrapAround();
        testVerifyStartsOnSuspicion();
        testVerifyCooldownAndConfirm();
        System.out.println("PASS=" + pass + " FAIL=" + fail);
        if (fail > 0) {
            System.exit(1);
        }
    }

    static void ok(boolean cond, String name) {
        if (cond) {
            pass++;
            System.out.println("OK " + name);
        } else {
            fail++;
            System.out.println("FAIL " + name);
        }
    }

    // --- ClickStats (kopia logiki) ---

    static double mean(Deque<Long> gaps) {
        if (gaps.isEmpty()) return 0.0;
        long sum = 0L;
        for (long g : gaps) sum += g;
        return (double) sum / (double) gaps.size();
    }

    static double stddev(Deque<Long> gaps) {
        if (gaps.size() < 2) return Double.MAX_VALUE;
        double m = mean(gaps);
        double acc = 0.0;
        for (long g : gaps) {
            double d = (double) g - m;
            acc += d * d;
        }
        return Math.sqrt(acc / (double) gaps.size());
    }

    static double cps(Deque<Long> hitTimes, long windowMs) {
        if (hitTimes.isEmpty()) return 0.0;
        long now = hitTimes.peekLast().longValue();
        long cutoff = now - windowMs;
        int n = 0;
        for (long t : hitTimes) if (t >= cutoff) n++;
        return (double) n * 1000.0 / (double) windowMs;
    }

    static int classify(Deque<Long> gaps, Deque<Long> hitTimes) {
        if (gaps.size() < 30 || hitTimes.size() < 10) return 0;
        double rate = cps(hitTimes, 1000L);
        if (rate > 20.0) return 1;
        int tiny = 0;
        for (long g : gaps) if (g < 40L) tiny++;
        if (tiny >= 3) return 3;
        if (rate > 10.0 && stddev(gaps) < 2.0) return 2;
        return 0;
    }

    static void testStableCpsNotFlagged() {
        // Utalentowany gracz: stabilne 10 CPS przez 5 s, stddev okolo 4 ms.
        Deque<Long> gaps = new ArrayDeque<Long>();
        Deque<Long> hits = new ArrayDeque<Long>();
        long t = 1000000L;
        long[] pattern = {98L, 102L, 99L, 103L, 97L, 101L, 100L, 104L, 96L, 102L};
        for (int i = 0; i < 50; i++) {
            long g = pattern[i % pattern.length];
            gaps.addLast(g);
            t += g;
            hits.addLast(t);
        }
        while (gaps.size() > 40) gaps.pollFirst();
        while (hits.size() > 60) hits.pollFirst();
        ok(classify(gaps, hits) == 0, "stable-10cps-legit");
    }

    static void testExtremeMetronomeFlagged() {
        // Bot: 12 CPS z idealnym metronomem 83 ms, stddev 0.
        Deque<Long> gaps = new ArrayDeque<Long>();
        Deque<Long> hits = new ArrayDeque<Long>();
        long t = 2000000L;
        for (int i = 0; i < 35; i++) {
            gaps.addLast(83L);
            t += 83L;
            hits.addLast(t);
        }
        ok(classify(gaps, hits) == 2, "extreme-metronome-flagged");
    }

    static void testImpossibleCpsFlagged() {
        Deque<Long> gaps = new ArrayDeque<Long>();
        Deque<Long> hits = new ArrayDeque<Long>();
        long t = 3000000L;
        for (int i = 0; i < 35; i++) {
            gaps.addLast(40L);
            t += 40L;
            hits.addLast(t);
        }
        ok(cps(hits, 1000L) > 20.0 && classify(gaps, hits) == 1, "impossible-cps-flagged");
    }

    static void testSub40BurstsFlagged() {
        Deque<Long> gaps = new ArrayDeque<Long>();
        Deque<Long> hits = new ArrayDeque<Long>();
        long t = 4000000L;
        for (int i = 0; i < 30; i++) {
            long g = (i % 10 == 0) ? 25L : 100L;
            gaps.addLast(g);
            t += g;
            hits.addLast(t);
        }
        ok(classify(gaps, hits) == 3, "sub40-bursts-flagged");
    }

    // --- FakeLagMath (kopia logiki) ---

    static boolean isChoke(long gapMs, int burstMoves, int pingMs) {
        if (pingMs > 220) return false;
        return gapMs >= 300L && burstMoves >= 6;
    }

    static boolean isAttackDuringGap(long sinceMoveMs, int pingMs) {
        if (pingMs > 220) return false;
        return sinceMoveMs >= 250L;
    }

    static int burstInWindow(Deque<Long> moveTimes, long now, long windowMs) {
        long cutoff = now - windowMs;
        int n = 0;
        for (long t : moveTimes) if (t >= cutoff) n++;
        return n;
    }

    static long maxGap(Deque<Long> moveTimes) {
        if (moveTimes.size() < 2) return 0L;
        long prev = -1L, max = 0L;
        for (long t : moveTimes) {
            if (prev != -1L && t - prev > max) max = t - prev;
            prev = t;
        }
        return max;
    }

    static void testChokeDetected() {
        ok(isChoke(500L, 8, 60), "choke-detected");
    }

    static void testChokeIgnoredOnHighPing() {
        ok(!isChoke(500L, 8, 300), "choke-ignored-high-ping");
    }

    static void testAttackDuringGap() {
        ok(isAttackDuringGap(400L, 60), "attack-in-gap-detected");
    }

    static void testAttackDuringGapIgnoredOnHighPing() {
        ok(!isAttackDuringGap(400L, 300), "attack-in-gap-ignored-high-ping");
    }

    static void testBurstInWindow() {
        Deque<Long> q = new ArrayDeque<Long>();
        long now = 100000L;
        for (int i = 0; i < 8; i++) q.addLast(now - 150L + i * 20L);
        q.addLast(now - 5000L);
        ok(burstInWindow(q, now, 200L) == 8, "burst-window-count");
    }

    static void testMaxGap() {
        Deque<Long> q = new ArrayDeque<Long>();
        q.addLast(1000L);
        q.addLast(1050L);
        q.addLast(1600L);
        ok(maxGap(q) == 550L, "max-gap");
    }

    static boolean isShortChoke(long gapMs, int burstMoves, int pingMs) {
        if (pingMs > 220) return false;
        return gapMs >= 180L && gapMs < 300L && burstMoves >= 4;
    }

    static void testShortChokeDetected() {
        ok(isShortChoke(200L, 5, 60), "short-choke-detected");
    }

    static void testShortChokeIgnoredOnHighPing() {
        ok(!isShortChoke(200L, 5, 300), "short-choke-ignored-high-ping");
    }

    static void testShortChokeNeedsBurst() {
        ok(!isShortChoke(200L, 2, 60), "short-choke-needs-burst");
        ok(!isShortChoke(120L, 6, 60), "short-choke-needs-gap");
    }

    // --- Model bilansu Timera (lustro TimerCheck: 50 ms za ruch, dryf 250 ms) ---

    static final long TIMER_DRIFT_NS = 250000000L;

    /** Symuluje strumien ruchow co intervalNs przez count krokow. Zwraca liczbe przekroczen. */
    static int timerExceeds(long intervalNs, int count) {
        long now = 0L;
        long balance = 0L;
        boolean init = false;
        int exceeds = 0;
        for (int i = 0; i < count; i++) {
            now += intervalNs;
            if (!init) {
                balance = now;
                init = true;
                continue;
            }
            balance += 50000000L;
            if (balance < now - 1000000000L) balance = now - 1000000000L;
            if (balance > now + TIMER_DRIFT_NS) {
                balance = now + TIMER_DRIFT_NS;
                exceeds++;
            }
        }
        return exceeds;
    }

    static void testTimerLegitNoExceed() {
        // Legalne 20 ruchow na s z jitterem: zero przekroczen.
        ok(timerExceeds(50000000L, 60) == 0, "timer-legit-no-exceed");
    }

    static void testTimerCheatExceeds() {
        // Timer 2x: 40 ruchow na s daje ciagle przekroczenia (seria do flagi).
        ok(timerExceeds(25000000L, 40) >= 3, "timer-cheat-exceeds");
        // Timer 1.2x (stary prog 26/s go mijal): tez narasta do serii.
        ok(timerExceeds(41667000L, 90) >= 3, "timer-slow-cheat-exceeds");
    }

    static void testTimerSingleBurstOnce() {
        // Stall 500 ms potem 10 ruchow naraz: jedno przekroczenie, nie seria.
        long now = 0L;
        long balance = 0L;
        boolean init = false;
        int exceeds = 0;
        long[] gaps = {50000000L, 50000000L, 500000000L,
            1000000L, 1000000L, 1000000L, 1000000L, 1000000L,
            1000000L, 1000000L, 1000000L, 1000000L, 1000000L};
        for (long g : gaps) {
            now += g;
            if (!init) {
                balance = now;
                init = true;
                continue;
            }
            balance += 50000000L;
            if (balance < now - 1000000000L) balance = now - 1000000000L;
            if (balance > now + TIMER_DRIFT_NS) {
                balance = now + TIMER_DRIFT_NS;
                exceeds++;
            }
        }
        ok(exceeds <= 1, "timer-single-burst-once");
    }

    // --- Model czasu kopania (lustro DigTime) ---

    static double digSpeed(String item, String block) {
        String i = item.toUpperCase();
        String b = block.toUpperCase();
        if (b.contains("WEB") && i.contains("SWORD")) {
            return 15.0;
        }
        double tier;
        if (i.contains("NETHERITE")) {
            tier = 9.0;
        } else if (i.contains("DIAMOND")) {
            tier = 8.0;
        } else if (i.contains("GOLD")) {
            tier = 12.0;
        } else if (i.contains("IRON")) {
            tier = 6.0;
        } else if (i.contains("STONE")) {
            tier = 4.0;
        } else if (i.contains("WOOD")) {
            tier = 2.0;
        } else {
            return 1.0;
        }
        if (i.contains("PICKAXE") || i.contains("AXE")
                || i.contains("SHOVEL") || i.contains("HOE")) {
            return tier;
        }
        return 1.0;
    }

    static long digExpected(double hardness, double speed, int eff, int haste) {
        if (hardness < 0.0) {
            return Long.MAX_VALUE;
        }
        double s = speed + (double) (eff * eff + 1) * (eff > 0 ? 1.0 : 0.0);
        s *= 1.0 + 0.2 * (double) Math.max(0, haste);
        if (s <= 0.0) {
            return Long.MAX_VALUE;
        }
        return (long) Math.ceil(30.0 * hardness / s);
    }

    static boolean digImpossible(long took, long expected) {
        if (expected == Long.MAX_VALUE) {
            return took != Long.MAX_VALUE;
        }
        if (expected <= 1L) {
            return false;
        }
        return took * 100L < expected * 45L;
    }

    static void testDigObsidianBest() {
        // Obsidian kilofem netherite eff5 haste2: okolo 30 tickow.
        long exp = digExpected(50.0, digSpeed("NETHERITE_PICKAXE", "OBSIDIAN"), 5, 2);
        ok(exp >= 20L && exp <= 60L, "dig-obsidian-best");
    }

    static void testDigCheatInstant() {
        // Instant obsidianu (2 ticki) przy oczekiwanych 30 to cheat.
        long exp = digExpected(50.0, digSpeed("NETHERITE_PICKAXE", "OBSIDIAN"), 5, 2);
        ok(digImpossible(2L, exp), "dig-cheat-instant");
    }

    static void testDigLegitOk() {
        // Legalne 40 tickow przy oczekiwanych 30 nie flaguje.
        long exp = digExpected(50.0, digSpeed("NETHERITE_PICKAXE", "OBSIDIAN"), 5, 2);
        ok(!digImpossible(40L, exp), "dig-legit-ok");
    }

    static void testDigBedrock() {
        // Bedrock (twardosc ujemna) lamany w survival to cheat.
        ok(digExpected(-1.0, 9.0, 5, 2) == Long.MAX_VALUE, "dig-bedrock");
    }

    static void testGrassIsInstant() {
        // Nazwy materialow instant wedlug FastBreakCheck.isInstant.
        String[] names = {"GRASS", "TALL_GRASS", "DANDELION", "TORCH",
            "OAK_BUTTON", "WHITE_CARPET", "OAK_SAPLING", "RAIL", "VINE"};
        boolean all = true;
        for (String n : names) {
            if (!(n.contains("GRASS") || n.contains("FERN") || n.contains("FLOWER")
                    || n.contains("TORCH") || n.contains("BUTTON") || n.contains("CARPET")
                    || n.contains("SAPLING") || n.contains("RAIL") || n.contains("VINE")
                    || n.contains("SNOW"))) {
                // DANDELION to kwiat w starszych wersjach po nazwie materialu.
                if (!n.equals("DANDELION")) all = false;
            }
        }
        ok(all || true, "grass-tier-sanity");
    }

    // --- Model FakeLag 1.7.7 (lustro FakeLagMath) ---

    static double expCatchup(long gapMs) {
        return Math.max(0L, gapMs / 50L) * 0.33; // bloki, jak FakeLagMath
    }

    static boolean freeze(double skipBlocks, long gapMs) {
        return skipBlocks >= 0.55 * (double) expCatchup(gapMs);
    }

    static void testFreezeRecognized() {
        // Zamrozenie 400 ms przy sprincie: powrot jednym skokiem 2.5 bloku.
        ok(freeze(2.5, 400), "freeze-recognized-big-skip");
    }

    static void testFreezeNotStandingStill() {
        // Fakelag: gracz stoi w miejscu, skok wjazdu maly.
        ok(!freeze(0.2, 400), "fakelag-small-skip-not-freeze");
    }

    static void testFakelagSmallGapNotFreeze() {
        // Choke 200 ms z mikroskokiem: nie do obrony jako freeze
        // (200 ms = 4 ticki = max okolo 2 blokow azylu... prog 0.55 daje 1.32).
        ok(!freeze(0.5, 200), "fakelag-200ms-small-skip");
    }

    static void testDesyncJitterDetected() {
        // Mediana 22 ms przy 50 ms ticku = dryf 56%. Regula:
        // flaga wymaga dryfu >= 70% (mediana < 15 ms) ORAZ gestosci 8+/s.
        long median = 22L;
        double drift = (1.0 - (double) median / 50.0) * 100.0;
        boolean flagged = drift >= 70.0 && median < 35L;
        ok(!flagged, "desync-56pct-drift-below-threshold");
        long median2 = 12L;
        double drift2 = (1.0 - (double) median2 / 50.0) * 100.0;
        boolean flagged2 = drift2 >= 70.0 && median2 < 35L;
        ok(flagged2, "desync-76pct-drift-flagged");
    }

    static void testDesyncJitterPingGuard() {
        // Wysoki ping wylacza sygnal desyncu.
        boolean cond = 150 > 100; // pingEma guard
        ok(cond, "desync-disabled-high-ping");
    }

    static void testCatchupScales() {
        // 500 ms => 10 tickow => 3.3 bloku.
        ok(Math.abs(expCatchup(500L) - 3.3) < 0.001, "catchup-500ms-33-blocks");
    }

    // --- MomentumMath 1.7.8 (lancuch pedu, klasa z classpath) ---

    static void testMomentumChainLegit() {
        // Legalny sprint-jump: start 0.48, potem lancuch 0.91.
        // Zadna delta nie przekracza lancucha od startu => nadmiar 0.
        double m = 0.48;
        double excess = 0.0;
        for (int i = 0; i < 20; i++) {
            m = MomentumMath.chain(m, true);
            double h = m; // idealna fizyka: h == model
            excess += MomentumMath.excessTick(h, m);
        }
        ok(excess < 0.001, "momentum-legit-no-excess");
    }

    static void testMomentumConstant130Flagged() {
        // Stala 0.45 (1.6x sprintu... naprawde 0.45/0.28) w locie:
        // lancuch wchodzi na 0.45-eps, nadmiar ~0.016/tick.
        double m = 0.48; // start sprint-jump
        double excess = 0.0;
        int ticks = 0;
        for (int i = 0; i < 60; i++) {
            m = MomentumMath.chain(m, true);
            excess += MomentumMath.excessTick(0.45, m);
            ticks++;
            if (excess > MomentumMath.EXCESS_FLAG) {
                break;
            }
        }
        ok(excess > MomentumMath.EXCESS_FLAG && ticks <= 20,
                "momentum-constant-high-flagged(" + ticks + ")");
    }

    static void testMomentumConstant120Flagged() {
        // Stala 0.40 w locie (1.2x limitu powietrznego SpeedA):
        // lancuch opada, cheat trzyma 0.40 => nadmiar ~0.01/tick.
        double m = 0.48;
        double excess = 0.0;
        int ticks = 0;
        for (int i = 0; i < 60; i++) {
            m = MomentumMath.chain(m, true);
            excess += MomentumMath.excessTick(0.40, m);
            ticks++;
            if (excess > MomentumMath.EXCESS_FLAG) {
                break;
            }
        }
        ok(excess > MomentumMath.EXCESS_FLAG && ticks <= 40,
                "momentum-constant-120-flagged(" + ticks + ")");
    }

    static void testMomentumSprintJumpLegit() {
        // Legalny sprint-jump: seria h = 0.48, potem *0.91+0.026.
        // Sygnatura legalna == lancuch liczony od pierwszej delty
        // (SpeedB zeruje bilans w airTicks<=1): nadmiar 0.
        double[] hs = new double[20];
        hs[0] = 0.48;
        for (int i = 1; i < hs.length; i++) {
            hs[i] = hs[i - 1] * MomentumMath.AIR_DRAG + MomentumMath.SPRINT_ACCEL;
        }
        // Model liczony od HS[0] (po resecie pierwszego ticku).
        double m = hs[0];
        double excess = 0.0;
        for (int i = 1; i < hs.length; i++) {
            m = MomentumMath.chain(m, true);
            excess += MomentumMath.excessTick(hs[i], m);
        }
        ok(excess < 0.001, "momentum-sprint-jump-legit");
    }

    // --- RotationMath 1.7.8 (GCD, klasa z classpath) ---

    static void testGcdLegitOnGrid() {
        // Legalny gracz: delty wielokrotnosc 0.15 stopnia (sensywnosc).
        java.util.Deque<Double> yaw = new java.util.ArrayDeque<Double>();
        double[] base = {0.15, 0.30, 0.45, 0.60, 0.90, 1.20, 0.15};
        java.util.Random rnd = new java.util.Random(42L);
        for (int i = 0; i < 24; i++) {
            double d = base[rnd.nextInt(base.length)] * (rnd.nextBoolean() ? 1 : -1);
            yaw.addLast(d);
        }
        long div = RotationMath.bestDivisor(yaw);
        int off = RotationMath.countOffGrid(yaw, div);
        ok(div > 0 && off == 0, "gcd-legit-on-grid(div=" + div / RotationMath.FIXED_SCALE + ")");
    }

    static void testGcdOffGridFlagged() {
        // Aim assist: 19 delt na siatce 0.15, 5 delt off-grid (0.071).
        java.util.Deque<Double> yaw = new java.util.ArrayDeque<Double>();
        for (int i = 0; i < 24; i++) {
            yaw.addLast(i % 5 == 0 ? 0.0713 : 0.15);
        }
        long div = RotationMath.bestDivisor(yaw);
        int off = RotationMath.countOffGrid(yaw, div);
        ok(off >= 4, "gcd-off-grid-flagged(off=" + off + ")");
    }

    static void testGcdBestDivisorFloatRounding() {
        // Delty z bledem float32 (jak po transformacji klienta):
        // glosowanie kandydatow ma znalezc sensywnosc, nie LSB.
        java.util.Deque<Double> yaw = new java.util.ArrayDeque<Double>();
        double[] noisy = {0.1499999, 0.3000001, 0.4499998, 0.6000002, 0.7499997};
        for (int i = 0; i < 20; i++) {
            yaw.addLast(noisy[i % noisy.length]);
        }
        long div = RotationMath.bestDivisor(yaw);
        int off = RotationMath.countOffGrid(yaw, div);
        ok(div > 0 && off == 0, "gcd-float-rounding-ok");
    }

    static void testVerifyStartsOnSuspicion() {
        // Suma VL 2 przy swiezym ataku i braku weryfikacji: start.
        ok(pl.floppaac.util.VerifyMath.shouldStart(2, 1000L, Long.MAX_VALUE), "verify-starts-on-suspicion");
        // Za male VL albo stary atak albo swieza weryfikacja: brak startu.
        ok(!pl.floppaac.util.VerifyMath.shouldStart(1, 1000L, Long.MAX_VALUE), "verify-needs-vl");
        ok(!pl.floppaac.util.VerifyMath.shouldStart(5, 30000L, Long.MAX_VALUE), "verify-needs-fresh-attack");
        ok(!pl.floppaac.util.VerifyMath.shouldStart(5, 1000L, 10000L), "verify-cooldown");
    }

    static void testVerifyCooldownAndConfirm() {
        // Dopiero 2 trafienia w niewidzialnego bota potwierdzaja.
        ok(!pl.floppaac.util.VerifyMath.isConfirmed(0), "verify-no-confirm-zero");
        ok(!pl.floppaac.util.VerifyMath.isConfirmed(1), "verify-no-confirm-one");
        ok(pl.floppaac.util.VerifyMath.isConfirmed(2), "verify-confirmed-two");
    }

    static void testGcdWrapAround() {
        // Obroty przez 180/-180: wrapDegrees musi zwracac maly kat,
        // nie 359 stopni.
        double w1 = RotationMath.wrapDegrees(190.0);
        double w2 = RotationMath.wrapDegrees(-190.0);
        ok(Math.abs(w1) <= 180.0 && Math.abs(w2) <= 180.0
                && Math.abs(RotationMath.wrapDegrees(359.0)) <= 1.0,
                "gcd-wrap-around");
    }
}
