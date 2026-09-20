package pl.floppaac.util;

public class TpsMonitor implements Runnable {

    private long lastTime = -1L;
    private double tps = 20.0;

    @Override
    public void run() {
        long now = System.nanoTime();
        if (lastTime != -1L) {
            long delta = now - lastTime;
            if (delta > 0) {
                double instant = 1000000000.0 / (double) delta;
                tps = tps * 0.9 + Math.min(instant, 20.0) * 0.1;
                if (tps > 20.0) {
                    tps = 20.0;
                }
            }
        }
        lastTime = now;
    }

    public double getTps() {
        if (lastTime != -1L && System.nanoTime() - lastTime > 1000000000L) {
            return 0.0;
        }
        return tps;
    }

    public void feedDeltaNanos(long delta) {
        if (delta > 0) {
            double instant = 1000000000.0 / (double) delta;
            tps = tps * 0.9 + Math.min(instant, 20.0) * 0.1;
        }
    }
}
