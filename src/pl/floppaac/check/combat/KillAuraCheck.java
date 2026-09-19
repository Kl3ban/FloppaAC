package pl.floppaac.check.combat;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.ClickStats;
import pl.floppaac.util.PingUtil;

import java.util.HashSet;

/**
 * KillAura: trzy sygnaly.
 * AuraA: trafienie bez patrzenia na cel (kat powyzej 55 stopni).
 * AuraB: multi-target, wiecej niz 6 roznych encji na s.
 * AuraC: aura liniowa (LiquidBounce Linear): rotacja idzie stalymi
 * malymi krokami do celu z niska wariancja i wysoka celnoscia.
 * Legalne flicki maja duza wariancje i pudluja, wiec ich nie lapie.
 */
public class KillAuraCheck extends Check {

    public KillAuraCheck(FloppaAC plugin) {
        super(plugin, "KillAuraA", CheckType.COMBAT);
    }

    public void handleAngle(Player attacker, PlayerData data, LivingEntity victim) {
        if (data.movementExempt() || data.invOpen
                || data.velocityExempt(attacker, System.currentTimeMillis())) {
            data.snapStreak = 0;
            return;
        }
        Location eye = attacker.getEyeLocation();
        Vector look = eye.getDirection();
        look.setY(0.0);
        if (look.lengthSquared() < 0.000001) {
            return;
        }
        look = look.normalize();
        Vector toTarget = victim.getLocation().toVector().subtract(eye.toVector());
        toTarget.setY(0.0);
        double flatDist = toTarget.length();
        if (flatDist < 1.2 || flatDist > 6.0) {
            return;
        }
        toTarget = toTarget.normalize();
        double angle = Math.toDegrees(Math.acos(
                Math.max(-1.0, Math.min(1.0, look.dot(toTarget)))));

        double maxAngle = cfgDouble("max-angle", 55.0);
        int ping = PingUtil.getPing(attacker, data);
        if (ping > 100) {
            maxAngle += 10.0;
        }
        long now = System.currentTimeMillis();
        if (now - data.lastSnapMs > 10000L) {
            data.snapStreak = 0;
        }
        if (angle > maxAngle) {
            data.lastSnapMs = now;
            data.snapStreak++;
            if (data.snapStreak >= 5) {
                signalAs("KillAuraA", attacker, data, String.format(
                        "angle=%.0f dist=%.1f x%d", angle, flatDist, data.snapStreak));
                data.snapStreak = 0;
            } else {
                suspicious(attacker, data, String.format("snap %.0f (%d/5)",
                        angle, data.snapStreak));
            }
        } else {
            data.snapStreak = Math.max(0, data.snapStreak - 1);
            if (angle > maxAngle - 15.0) {
                suspicious(attacker, data, String.format("borderline angle %.0f", angle));
            }
        }
    }

    public void handleTargets(Player attacker, PlayerData data, LivingEntity victim) {
        if (data.movementExempt() || data.invOpen) {
            return;
        }
        long now = System.currentTimeMillis();
        data.targetHits.addLast(new PlayerData.TargetHit(victim.getEntityId(), now));
        while (data.targetHits.size() > 30) {
            data.targetHits.pollFirst();
        }
        while (!data.targetHits.isEmpty()
                && now - data.targetHits.peekFirst().time > 1000L) {
            data.targetHits.pollFirst();
        }
        HashSet<Integer> ids = new HashSet<Integer>();
        for (PlayerData.TargetHit h : data.targetHits) {
            ids.add(Integer.valueOf(h.entityId));
        }
        int maxTargets = cfgInt("max-targets-per-second", 6);
        if (ids.size() > maxTargets && data.targetHits.size() >= ids.size()) {
            signalAs("KillAuraB", attacker, data, "targets=" + ids.size() + "/s");
        }
    }

    /**
     * Aura liniowa. Wywolywane przy trafieniu z aktualnym yaw i pitch.
     * @param aimError blad celowania w stopniach (0 idealnie w cel)
     */
    public void handleLinear(Player attacker, PlayerData data,
                             float yaw, float pitch, double aimError) {
        if (data.movementExempt() || data.invOpen
                || data.velocityExempt(attacker, System.currentTimeMillis())) {
            data.linearHits = 0;
            return;
        }
        if (data.hasLastRotation) {
            double dyaw = Math.abs(yawDelta(data.lastYaw, yaw));
            double dpitch = Math.abs((double) pitch - (double) data.lastPitch);
            PlayerData.pushCappedD(data.yawDeltas, dyaw, 10);
            PlayerData.pushCappedD(data.pitchDeltas, dpitch, 10);
        }
        data.lastYaw = yaw;
        data.lastPitch = pitch;
        data.hasLastRotation = true;

        if (data.yawDeltas.size() < 8) {
            return;
        }
        double meanYaw = mean(data.yawDeltas);
        double stdYaw = stddev(data.yawDeltas, meanYaw);
        double meanPitch = mean(data.pitchDeltas);
        if (meanYaw > 1.5 && meanYaw < 12.0 && stdYaw < 2.5 && meanPitch < 6.0 && aimError < 5.0) {
            data.linearHits++;
            if (data.linearHits >= 5) {
                signalAs("KillAuraC", attacker, data, String.format(
                        "linear mean=%.1f std=%.1f err=%.1f",
                        meanYaw, stdYaw, aimError));
                data.linearHits = 0;
            }
        } else {
            data.linearHits = Math.max(0, data.linearHits - 1);
        }
    }

    /**
     * KillAuraD: cios bez rotacji w ruszajacy sie cel (silent aim).
     * Cel przesuniety o ponad 0.5 bloku wymaga korekty celowania.
     * Trzy takie ciosy z rzedu przy dystansie powyzej 2 bloki to sygnal.
     */
    public void handleNoRotation(Player attacker, PlayerData data, LivingEntity victim) {
        if (data.movementExempt() || data.invOpen
                || data.velocityExempt(attacker, System.currentTimeMillis())) {
            data.noRotStreak = 0;
            return;
        }
        Location eye = attacker.getEyeLocation();
        double flatDist = Math.hypot(
                victim.getLocation().getX() - eye.getX(),
                victim.getLocation().getZ() - eye.getZ());
        float yaw = attacker.getLocation().getYaw();
        float pitch = attacker.getLocation().getPitch();
        boolean sameVictim = data.hasLastAtkRot
                && victim.getEntityId() == data.lastVictimId
                && data.lastVictimPos != null
                && data.lastVictimPos.getWorld() != null
                && victim.getWorld().equals(data.lastVictimPos.getWorld());
        double victimMoved = 0.0;
        if (sameVictim) {
            victimMoved = victim.getLocation().distance(data.lastVictimPos);
        }
        double rotD = 0.0;
        if (data.hasLastAtkRot) {
            rotD = yawDelta(data.lastAtkYaw, yaw)
                    + Math.abs((double) pitch - (double) data.lastAtkPitch);
        }
        data.lastVictimId = victim.getEntityId();
        data.lastVictimPos = victim.getLocation().clone();
        data.lastAtkYaw = yaw;
        data.lastAtkPitch = pitch;
        data.hasLastAtkRot = true;

        if (!sameVictim) {
            return;
        }
        // Teleport ofiary to nie strafe.
        if (victimMoved > 8.0) {
            data.noRotStreak = 0;
            return;
        }
        if (flatDist > 2.0 && victimMoved > 0.8 && rotD < 1.0) {
            data.noRotStreak++;
            if (data.noRotStreak >= 4) {
                signalAs("KillAuraD", attacker, data, String.format(
                        "aim %.2f rot %.2f x%d", victimMoved, rotD, data.noRotStreak));
                data.noRotStreak = 0;
            }
        } else {
            data.noRotStreak = Math.max(0, data.noRotStreak - 1);
        }
    }

    /**
     * KillAuraE: cios bez machniecia reka.
     * Legalny klient zawsze wysyla ArmSwing z atakiem. Atak, po ktorym
     * przez sekunde nie bylo zadnego machniecia, to pakietowa aura.
     * Bez bazy (gracz nigdy nie machnal) check wstrzymuje sie.
     */
    public void handleNoSwing(Player attacker, PlayerData data) {
        if (data.movementExempt() || data.invOpen) {
            data.noSwingStrikes = 0;
            return;
        }
        if (data.lastSwingMs == 0L) {
            return;
        }
        long sinceSwing = System.currentTimeMillis() - data.lastSwingMs;
        if (sinceSwing > 1000L) {
            // Pojedynczy cios bez pary to czesto kolejnosc w burście
            // (atak przed machnieciem). Flaga dopiero w serii 4.
            // Pare atak-machniecie w 750 ms zeruje licznik w onSwing.
            data.noSwingStrikes++;
            if (data.noSwingStrikes >= 4) {
                signalAs("KillAuraE", attacker, data, "attack " + sinceSwing
                        + "ms after swing x" + data.noSwingStrikes);
                data.noSwingStrikes = 0;
            }
        } else {
            data.noSwingStrikes = 0;
        }
    }

    /**
     * KillAuraF: krawedz cooldownu.
     * Aura bije dokladnie w momencie pelnego naladowania (1.9+):
     * 18 z 20 ciosow na pelnym cooldownzie z metronomowa regularnoscia
     * odstepow to nie czlowiek. Ludzie mieszaja ciosy pelne i slabe.
     */
    public void handleCharge(Player attacker, PlayerData data) {
        if (data.movementExempt() || data.invOpen) {
            return;
        }
        float charge;
        try {
            charge = attacker.getAttackCooldown();
        } catch (NoSuchMethodError e) {
            return;
        }
        PlayerData.pushCapped(data.chargeHits, charge >= 0.95f ? 1L : 0L, 20);
        if (data.chargeHits.size() < 20) {
            return;
        }
        long full = 0L;
        for (Long v : data.chargeHits) {
            full += v.longValue();
        }
        if (full < 18L) {
            return;
        }
        double sd = ClickStats.stddev(data.clickGaps);
        if (sd < 30.0) {
            signalAs("KillAuraF", attacker, data, String.format(
                    "pelne=%d/20 sd=%.1fms", full, sd));
            data.chargeHits.clear();
        }
    }

    /**
     * KillAuraG: orbita.
     * Aura trzyma ofiare w waskim pasku maksymalnego zasiegu.
     * 8 trafien z rzedu w pasmie 0.4 bloku w ruchu to nie czlowiek,
     * ludzie dystans ciagna i gubia.
     */
    public void handleOrbit(Player attacker, PlayerData data,
                            LivingEntity victim, double dist) {
        if (data.movementExempt() || data.invOpen
                || data.velocityExempt(attacker, System.currentTimeMillis())) {
            data.hitDists.clear();
            return;
        }
        if (data.lastHorizontalDist < 0.05) {
            return;
        }
        PlayerData.pushCappedD(data.hitDists, dist, 12);
        if (data.hitDists.size() < 10) {
            return;
        }
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (Double d : data.hitDists) {
            double v = d.doubleValue();
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
        }
        if (max - min < 0.3) {
            signalAs("KillAuraG", attacker, data, String.format(
                    "pasmo %.2f-%.2f", min, max));
            data.hitDists.clear();
        }
    }

    /**
     * KillAuraH: cios bez linii wzroku (aura przez sciane).
     * Promien z oczu atakujacego musi trafic w ofiare. Trafienia
     * przez pelna sciane to pakietowa aura. Seria 3.
     */
    public void handleLineOfSight(Player attacker, PlayerData data, LivingEntity victim) {
        if (data.movementExempt() || data.invOpen
                || data.velocityExempt(attacker, System.currentTimeMillis())) {
            data.losStreak = 0;
            return;
        }
        boolean clear;
        try {
            clear = attacker.hasLineOfSight(victim);
        } catch (NoSuchMethodError e) {
            return;
        }
        if (clear) {
            data.losStreak = 0;
            return;
        }
        data.losStreak++;
        if (data.losStreak >= 4) {
            signalAs("KillAuraH", attacker, data,
                    "no line of sight x" + data.losStreak);
            data.losStreak = 0;
        }
    }

    /**
     * KillAuraI: konwergencja rotacji (smooth aim). Matematyczny
     * wygladzacz zbliza sie do celu wykladniczo: stosunek obrotu
     * do pozostalego bledu jest trafienie w trafienie staly.
     * Czlowiek mierzy bledem i predkoscia niezaleznie - rozrzut
     * ogromny. Wymaga realnego bledu (aura dogania cel) i realnego
     * obrotu, wiec stanie na celowniku nie flaguje.
     */
    public void handleReactive(Player attacker, PlayerData data,
                               LivingEntity victim, long now) {
        if (data.movementExempt() || data.invOpen
                || data.velocityExempt(attacker, System.currentTimeMillis())) {
            data.auraIPairs.clear();
            return;
        }
        double err = aimError(attacker, victim);
        float prevYaw = Float.NaN;
        double deltaSum = 0.0;
        int samples = 0;
        for (float[] p : data.yawHist) {
            if (now - (long) p[1] > 400L) {
                continue;
            }
            if (!Float.isNaN(prevYaw)) {
                float d = Math.abs(p[0] - prevYaw) % 360.0f;
                if (d > 180.0f) {
                    d = 360.0f - d;
                }
                deltaSum += d;
                samples++;
            }
            prevYaw = p[0];
        }
        data.yawHist.addLast(new float[]{attacker.getLocation().getYaw(), (float) now});
        while (data.yawHist.size() > 24) {
            data.yawHist.pollFirst();
        }
        if (samples < 2) {
            return;
        }
        double preMean = deltaSum / samples;
        data.auraIPairs.addLast(new double[]{err, preMean});
        while (data.auraIPairs.size() > 10) {
            data.auraIPairs.pollFirst();
        }
        if (data.auraIPairs.size() < 8) {
            return;
        }
        int withErr = 0;
        double sumR = 0.0;
        double sumR2 = 0.0;
        double sumMean = 0.0;
        int n = 0;
        for (double[] pair : data.auraIPairs) {
            if (pair[0] >= 4.0) {
                withErr++;
            }
            double r = pair[1] / Math.max(pair[0], 0.5);
            sumR += r;
            sumR2 += r * r;
            sumMean += pair[1];
            n++;
        }
        if (withErr < 5) {
            return;
        }
        double meanR = sumR / n;
        double varR = Math.max(0.0, sumR2 / n - meanR * meanR);
        double cv = Math.sqrt(varR) / Math.max(meanR, 0.0001);
        double meanPre = sumMean / n;
        if (cv < 0.3 && meanR >= 0.25 && meanR <= 10.0 && meanPre >= 4.0) {
            signalAs("KillAuraI", attacker, data, String.format(
                    "konwergencja cv=%.2f k=%.2f obrot=%.1f", cv, meanR, meanPre));
            data.auraIPairs.clear();
        }
    }

    private static float yawDelta(float a, float b) {
        float d = Math.abs(a - b) % 360.0f;
        if (d > 180.0f) {
            d = 360.0f - d;
        }
        return d;
    }

    private static double mean(java.util.Deque<Double> q) {
        double s = 0.0;
        for (double v : q) {
            s += v;
        }
        return s / (double) q.size();
    }

    private static double stddev(java.util.Deque<Double> q, double mean) {
        double acc = 0.0;
        for (double v : q) {
            double d = v - mean;
            acc += d * d;
        }
        return Math.sqrt(acc / (double) q.size());
    }

    /** Blad celowania w stopniach miedzy wzrokiem a srodkiem celu. */
    public static double aimError(Player attacker, LivingEntity victim) {
        Location eye = attacker.getEyeLocation();
        Vector look = eye.getDirection();
        if (look.lengthSquared() < 0.000001) {
            return 90.0;
        }
        look = look.normalize();
        Location center = victim.getLocation().clone();
        center.setY(center.getY() + 1.0);
        Vector to = center.toVector().subtract(eye.toVector());
        if (to.lengthSquared() < 0.000001) {
            return 90.0;
        }
        to = to.normalize();
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, look.dot(to)))));
    }
}
