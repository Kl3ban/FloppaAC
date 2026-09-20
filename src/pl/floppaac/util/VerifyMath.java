package pl.floppaac.util;

public final class VerifyMath {

    public static final int SUSPICION_VL = 2;

    public static final long ATTACK_FRESH_MS = 5000L;

    public static final long VERIFY_COOLDOWN_MS = 60000L;

    public static final int HITS_TO_CONFIRM = 2;

    public static final int DURATION_TICKS = 300;

    public static final int ORBIT_STEP_TICKS = 5;

    public static final double ORBIT_RADIUS = 2.6;

    private VerifyMath() {
    }

    public static boolean shouldStart(int auraVl, long sinceAttackMs, long sinceVerifyMs) {
        if (auraVl < SUSPICION_VL) {
            return false;
        }
        if (sinceAttackMs < 0L || sinceAttackMs > ATTACK_FRESH_MS) {
            return false;
        }
        return sinceVerifyMs > VERIFY_COOLDOWN_MS;
    }

    public static boolean isConfirmed(int hits) {
        return hits >= HITS_TO_CONFIRM;
    }
}
