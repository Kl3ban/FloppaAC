package pl.floppaac.data;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {

    public final UUID uuid;

    public final Map<String, Integer> vl = new ConcurrentHashMap<String, Integer>();

    public final Map<String, Long> lastFlag = new ConcurrentHashMap<String, Long>();

    public Location lastLoc;
    public Location lastGroundLoc;
    public Location setbackLoc;

    public int airTicks;
    public int groundTicks;
    public int hoverTicks;
    public int riseStreak;
    public int speedStreak;
    public int jesusStreak;

    public int spiderWallStreak;

    public int spiderAirStreak;

    public int noGroundStreak;
    public int stepStreak;
    public int webStreak;
    public int phaseStreak;

    public int velocityStreak;
    public double lastDeltaY;
    public double lastHorizontalDist;

    public long joinTime;
    public long lastTeleport;
    public long lastRespawn;
    public long lastVehicleExit;

    public final Deque<Long> hitTimes = new ArrayDeque<Long>();

    public final Deque<TargetHit> targetHits = new ArrayDeque<TargetHit>();

    public final Deque<Long> clickGaps = new ArrayDeque<Long>();
    public long lastHitTime;

    public long anyGapMs;

    public long lastSwingMs;

    public int noSwingStrikes;

    public long lastFakeLagMs;

    public Location lastSafeLoc;

    public final Deque<Long> chargeHits = new ArrayDeque<Long>();

    public final Deque<Double> hitDists = new ArrayDeque<Double>();

    public int invAtkStreak;

    public int invMoveStreak;

    public boolean invOpen;

    public final Deque<Double> yawDeltas = new ArrayDeque<Double>();

    public final Deque<Double> pitchDeltas = new ArrayDeque<Double>();
    public float lastYaw;
    public float lastPitch;
    public boolean hasLastRotation;
    public int linearHits;
    public int snapStreak;

    public long lastSnapMs;

    public int lastVictimId = Integer.MIN_VALUE;

    public Location lastVictimPos;
    public float lastAtkYaw;
    public float lastAtkPitch;
    public boolean hasLastAtkRot;

    public int noRotStreak;

    public int critStreak;

    public int reachStreak;

    public long lastReachMs;

    public int noSlowStreak;

    public int flyBigStreak;

    public int sprintStreak;

    public int hungerStreak;

    public int blindStreak;

    public int glideSprintStreak;

    public int waterSprintStreak;

    public int multiAStreak;

    public int multiBStreak;

    public int losStreak;

    public final java.util.Deque<float[]> yawHist =
            new java.util.LinkedList<float[]>();

    public long lastAuraHitMs;

    public final java.util.Deque<double[]> auraIPairs =
            new java.util.LinkedList<double[]>();

    public long lastDropMs;

    public int airPhasePlaces;

    public int bridgeJumps;

    public long lastBridgeJumpMs;

    public final java.util.ArrayDeque<Long> webOnPlayerTimes = new java.util.ArrayDeque<>();

    public final java.util.ArrayDeque<Long> boxOnVictimTimes = new java.util.ArrayDeque<>();

    public int glideStreak;

    public int placeFaceStreak;

    public int placeWallStreak;

    public int airPlaceStreak;

    public long setbackGraceMs;

    public double speedPrevH;

    public double speedExcessSum;

    public int speedBStreak;

    public final Deque<Double> gcdYawDeltas = new ArrayDeque<Double>();

    public final Deque<Double> gcdPitchDeltas = new ArrayDeque<Double>();

    public float gcdPrevYaw;

    public float gcdPrevPitch;

    public boolean gcdHasPrev;

    public int gcdOffStreak;

    public long lastGcdFlagMs;

    public void resetMomentumGcd() {
        speedPrevH = 0.0;
        speedBStreak = 0;
        gcdYawDeltas.clear();
        gcdPitchDeltas.clear();
        gcdPrevYaw = 0.0f;
        gcdPrevPitch = 0.0f;
        gcdHasPrev = false;
        gcdOffStreak = 0;
    }

    public Vector knockbackVelocity;
    public Location knockbackLoc;

    public Location knockbackFrom;
    public int knockbackWaitTicks;
    public boolean awaitingVelocity;

    public long knockbackTimeMs;

    public long lastFallDamageMs;

    public long lastRaiseSeenMs;

    public long lastUnraiseSeenMs;

    public long lastLandMs;

    public long kbArmedMs;

    public int kbTicksLeft;

    public long kbSinceBigMs;

    public double fallDistAcc;
    public boolean fallingAcc;

    public int serverAirTicks;

    public long lastBotVerifyMs;

    public int noFallPlateauTicks;
    public long lastGroundTickMs;

    public long lastRawGapMs;

    public long lastRawGapAt;

    public final Deque<Long> placeTimes = new ArrayDeque<Long>();
    public final Deque<Long> breakTimes = new ArrayDeque<Long>();

    public long digStartMs;
    public int digX;
    public int digY;
    public int digZ;
    public float digHardness;

    public int fastBreakBStreak;

    public final Deque<Long> nukeTimes = new ArrayDeque<Long>();
    public final Deque<Long> invClickTimes = new ArrayDeque<Long>();
    public long lastPlaceTime;
    public long lastBreakTime;
    public int scaffoldAirPlaces;

    public long timerBalanceNs;
    public long timerLastExceedNs;
    public int timerStreak;

    public final Deque<Long> moveTimes = new ArrayDeque<Long>();

    public long lastMoveMs;

    public long lastAnyPacketMs;

    public long lastMoveGapMs;

    public int lastBurstSize;

    public int chokeStreak;

    public long lastChokeMs;

    public int shortChokeStreak;

    public long lastFreezeMs;

    public long lastDesyncFlagMs;

    public long lastShortChokeMs;

    public long lastAttackMs;

    public final Deque<Long> bigGaps = new ArrayDeque<Long>();

    public final Deque<Long> moveGaps = new ArrayDeque<Long>();

    public long suspiciousCount;
    public long flagCount;

    public int pingCache;
    public long pingCacheTime;

    public int pingEma;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        long now = System.currentTimeMillis();
        this.joinTime = now;
        this.lastMoveMs = now;
        this.lastAnyPacketMs = now;
    }

    public int getVl(String check) {
        Integer v = vl.get(check);
        return v == null ? 0 : v.intValue();
    }

    public void addVl(String check) {
        vl.put(check, getVl(check) + 1);
        flagCount++;
    }

    public void decay() {
        for (Map.Entry<String, Integer> e : vl.entrySet()) {
            if (e.getValue().intValue() > 0) {
                e.setValue(Integer.valueOf(e.getValue().intValue() - 1));
            }
        }
    }

    public boolean movementExempt() {
        long now = System.currentTimeMillis();
        return now - joinTime < 3000L
            || now - lastTeleport < 1000L
            || now - lastRespawn < 3000L
            || now - lastVehicleExit < 1500L
            || now < setbackGraceMs;
    }

    public boolean velocityExempt(org.bukkit.entity.Player p, long now) {
        return kbTicksLeft > 0 || now - kbSinceBigMs < 1200L;
    }

    public static void pushCapped(Deque<Long> q, long value, int cap) {
        q.addLast(Long.valueOf(value));
        while (q.size() > cap) {
            q.pollFirst();
        }
    }

    public static void pushCappedD(Deque<Double> q, double value, int cap) {
        q.addLast(Double.valueOf(value));
        while (q.size() > cap) {
            q.pollFirst();
        }
    }

    public static final class TargetHit {
        public final int entityId;
        public final long time;
        public TargetHit(int entityId, long time) {
            this.entityId = entityId;
            this.time = time;
        }
    }
}
