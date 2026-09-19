package pl.floppaac.util;

/**
 * Model pedu poziomego vanilla (SpeedB / late-air decay).
 * Powietrze: h *= 0.91 na tick plus przyrost 0.02 (sprint 0.026).
 * Sprint-jump doklada 0.2 w kierunku patrzenia w pierwszym ticku,
 * wiec start ~0.48 i potem tylko wygasanie tarcia.
 *
 * Kluczowy model 1.7.8: NIE porownujemy h z capem per-tick (eps 0.04
 * przepuszcza stala predkosc 1.3x, bo nadmiar 0.016 < eps). Zamiast
 * tego prowadzimy LANCUCH modelu m (m = m*0.91 + accel) od zmierzonego
 * startu i akumulujemy nadmiar suma(max(0, h - m)). Legalny gracz nigdy
 * nie przekracza lancucha (nierownosc trojkata dla skretu), wiec suma
 * ~0. Stala 1.2x laczy 0.2 nadmiaru w ~9 tickach, 1.3x w ~5.
 */
public final class MomentumMath {

    public static final double AIR_DRAG = 0.91;
    public static final double AIR_ACCEL = 0.02;
    public static final double SPRINT_ACCEL = 0.026;
    public static final double SPRINT_JUMP_BOOST = 0.2;

    /** Suma nadmiaru nad lancuch, po ktorej SpeedB flaguje. */
    public static final double EXCESS_FLAG = 0.2;

    /** Ticki w powietrzu, zanim oceniamy (boost sprint-jump zyje wczesnie). */
    public static final int LATE_AIR_TICKS = 8;

    private MomentumMath() {
    }

    /** Nastepny punkt lancuchu modelu z poprzedniego. */
    public static double chain(double prevM, boolean sprinting) {
        double accel = sprinting ? SPRINT_ACCEL : AIR_ACCEL;
        return prevM * AIR_DRAG + accel;
    }

    /** Nadmiar pojedynczego ticku nad lancuch (0 gdy w modelu). */
    public static double excessTick(double h, double m) {
        double e = h - m;
        return e > 0.0 ? e : 0.0;
    }

    /** Maksymalny h na tick w powietrzu przy zadanym poprzednim h. */
    public static double airCap(double prevH, boolean sprinting) {
        return chain(prevH, sprinting);
    }

    /**
     * Czy h miesci sie w pedzie (late-air decay) z tolerancja eps.
     * eps ~0.04 trzyma float error, pochylnie i mikroskok z bloku.
     */
    public static boolean withinMomentum(double prevH, double h,
                                         boolean sprinting, double eps) {
        return h <= chain(prevH, sprinting) + eps;
    }

    /** Teoretyczny ped sprint-jump w ticku t (diagnostyka / progi). */
    public static double sprintJumpCap(int tickInAir) {
        double v = MoveUtil.SPRINT_SPEED + SPRINT_JUMP_BOOST;
        for (int i = 0; i < tickInAir; i++) {
            v *= AIR_DRAG;
        }
        return v;
    }
}
