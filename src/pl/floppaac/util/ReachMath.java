package pl.floppaac.util;

public final class ReachMath {

    private ReachMath() {
    }

    public static double eyeToBox(double ex, double ey, double ez,
            double bx, double by, double bz) {
        double dx = gap(ex, bx, bx + 1.0);
        double dy = gap(ey, by, by + 1.0);
        double dz = gap(ez, bz, bz + 1.0);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double gap(double v, double min, double max) {
        if (v < min) {
            return min - v;
        }
        if (v > max) {
            return v - max;
        }
        return 0.0;
    }

    public static boolean isBreach(double dist, double reach, double slack) {
        return dist > reach + slack;
    }
}
