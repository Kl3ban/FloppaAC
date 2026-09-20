package pl.floppaac.util;

public final class MomentumMath {

    public static final double AIR_DRAG = 0.91;
    public static final double AIR_ACCEL = 0.02;
    public static final double SPRINT_ACCEL = 0.026;
    public static final double SPRINT_JUMP_BOOST = 0.2;

    public static final double EXCESS_FLAG = 0.2;

    public static final int LATE_AIR_TICKS = 8;

    private MomentumMath() {
    }

    public static double chain(double prevM, boolean sprinting) {
        double accel = sprinting ? SPRINT_ACCEL : AIR_ACCEL;
        return prevM * AIR_DRAG + accel;
    }

    public static double excessTick(double h, double m) {
        double e = h - m;
        return e > 0.0 ? e : 0.0;
    }

    public static double airCap(double prevH, boolean sprinting) {
        return chain(prevH, sprinting);
    }

    public static boolean withinMomentum(double prevH, double h,
                                         boolean sprinting, double eps) {
        return h <= chain(prevH, sprinting) + eps;
    }

    public static double sprintJumpCap(int tickInAir) {
        double v = MoveUtil.SPRINT_SPEED + SPRINT_JUMP_BOOST;
        for (int i = 0; i < tickInAir; i++) {
            v *= AIR_DRAG;
        }
        return v;
    }
}
