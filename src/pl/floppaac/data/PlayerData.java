package pl.floppaac.data;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wszystkie dane o graczu w jednym miejscu.
 * Zero alokacji na ticku poza kolejkami o stalym limicie.
 * Pola FakeLag opisuja synchronizacje pakietow ruchu i ataku.
 */
public class PlayerData {

    public final UUID uuid;

    /** check-name albo vl-name -> violation level */
    public final Map<String, Integer> vl = new ConcurrentHashMap<String, Integer>();
    /** vl-name -> ostatni czas flagi w ms (anty-spam) */
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
    /** Seria wznoszenia przy scianie (SpiderA). */
    public int spiderWallStreak;
    /** Seria wznoszenia w otwartym powietrzu (SpiderA). */
    public int spiderAirStreak;
    /** Seria deklaracji gruntu w locie (NoFallA no-ground). */
    public int noGroundStreak;
    public int stepStreak;
    public int webStreak;
    public int phaseStreak;
    /** Seria potwierdzonych brakow odrzutu z rzedu. */
    public int velocityStreak;
    public double lastDeltaY;
    public double lastHorizontalDist;

    // --- exemptiony (timestampy ms) ---
    public long joinTime;
    public long lastTeleport;
    public long lastRespawn;
    public long lastVehicleExit;

    // --- combat ---
    /** czasy trafien w ms do statystyk CPS */
    public final Deque<Long> hitTimes = new ArrayDeque<Long>();
    /** trafione encje w oknie 1 s (AuraB: multi-target) */
    public final Deque<TargetHit> targetHits = new ArrayDeque<TargetHit>();
    /** odstepy miedzy trafieniami w ms do AutoClickera */
    public final Deque<Long> clickGaps = new ArrayDeque<Long>();
    public long lastHitTime;
    /** Przerwa od poprzedniego DOWOLNEGO pakietu ruchu w ms. */
    public long anyGapMs;
    /** Ostatnie machniecie reka w ms (PlayerAnimationEvent). 0 to brak bazy. */
    public long lastSwingMs;
    /** Ciosy bez pary machniecia z rzedu (KillAuraE). */
    public int noSwingStrikes;
    /** Ostatnie wykrycie FakeLag w ms (LagGuard ustepuje). */
    public long lastFakeLagMs;
    /** Ostatnia bezpieczna pozycja naziemna (poza webem i woda). */
    public Location lastSafeLoc;
    /** Ostatnie 20 ciosow: 1 gdy pelny cooldown, 0 gdy niepelny. */
    public final Deque<Long> chargeHits = new ArrayDeque<Long>();
    /** Ostatnie dystanse trafien do wykrywania orbity. */
    public final Deque<Double> hitDists = new ArrayDeque<Double>();
    /** Seria atakow z otwartym GUI. */
    public int invAtkStreak;
    /** Seria sprintu z otwartym GUI. */
    public int invMoveStreak;
    /** Czy GUI gracza jest otwarte. */
    public boolean invOpen;

    // --- rotacje do wykrywania aury liniowej ---
    /** Ostatnie delty yaw w stopniach (wartosci absolutne). */
    public final Deque<Double> yawDeltas = new ArrayDeque<Double>();
    /** Ostatnie delty pitch w stopniach (wartosci absolutne). */
    public final Deque<Double> pitchDeltas = new ArrayDeque<Double>();
    public float lastYaw;
    public float lastPitch;
    public boolean hasLastRotation;
    public int linearHits;
    public int snapStreak;
    /** Czas ostatniego snap kata w ms (okno serii). */
    public long lastSnapMs;
    // --- silent aim: rotacja atakujacego i pozycja ofiary miedzy ciosami ---
    /** Id ostatnio trafionej encji. */
    public int lastVictimId = Integer.MIN_VALUE;
    /** Pozycja ostatnio trafionej encji. */
    public Location lastVictimPos;
    public float lastAtkYaw;
    public float lastAtkPitch;
    public boolean hasLastAtkRot;
    /** Seria ciosow bez rotacji w ruszajacy sie cel. */
    public int noRotStreak;
    /** Seria podejrzanych krytykow z rzedu (packet-crits). */
    public int critStreak;
    /** Seria przekroczen zasiegu w oknie 2 s. */
    public int reachStreak;
    /** Czas ostatniego przekroczenia zasiegu w ms. */
    public long lastReachMs;
    /** Seria NoSlow z rzedu. */
    public int noSlowStreak;
    /** Seria duzych wzniosow Fly z rzedu. */
    public int flyBigStreak;
    /** Seria sprintu do tylu z rzedu. */
    public int sprintStreak;
    /** Seria sprintu przy glodzie. */
    public int hungerStreak;
    /** Seria startow sprintu ze slepota. */
    public int blindStreak;
    /** Seria sprintu w locie na elytrze. */
    public int glideSprintStreak;
    /** Seria sprintu pod woda. */
    public int waterSprintStreak;
    /** Seria ciosow z podniesiona reka (multitool). */
    public int multiAStreak;
    /** Seria machniec z podniesiona reka (multitool). */
    public int multiBStreak;
    /** Seria ciosow bez linii wzroku (KillAuraH: aura przez sciane). */
    public int losStreak;
    /** Historia probek yaw (czas w [1]) dla KillAuraI. */
    public final java.util.Deque<float[]> yawHist =
            new java.util.LinkedList<float[]>();
    /** Czas poprzedniego trafienia (KillAuraI). */
    public long lastAuraHitMs;
    /** Probki [blad katowy, sredni obrot przed ciosiem] dla KillAuraI. */
    public final java.util.Deque<double[]> auraIPairs =
            new java.util.LinkedList<double[]>();
    /** Ostatnie wyrzucenie przedmiotu w ms. */
    public long lastDropMs;
    /** Postawione bloki pod soba w jednej fazie powietrza (scaffold). */
    public int airPhasePlaces;
    /** Skoki-budowanie pod soba z rzedu (jump-bridge). */
    public int bridgeJumps;
    /** Ostatni skok budowania w ms. */
    public long lastBridgeJumpMs;
    /** Znaczniki czasu webow postawionych NA graczach (AutoWeb). */
    public final java.util.ArrayDeque<Long> webOnPlayerTimes = new java.util.ArrayDeque<>();
    /** Znaczniki czasu blokow obudowy wokol graczy (AutoTrap). */
    public final java.util.ArrayDeque<Long> boxOnVictimTimes = new java.util.ArrayDeque<>();
    /** Seria slizgu w dol z rzedu. */
    public int glideStreak;
            /** Seria klikania niewidocznej sciany (GhostHand). */
    public int placeFaceStreak;
    /** Seria stawiania przez sciane (GhostHand). */
    public int placeWallStreak;
    /** Seria stawiania w powietrzu (AirPlace). */
    public int airPlaceStreak;
    /** Krotka karencja po setbacku w ms (zamiast pelnego exemptu). */
    public long setbackGraceMs;

    // --- 1.7.8: ped poziomy (SpeedB) i GCD rotacji (KillAuraJ) ---
    /** Pozioma predkosc poprzedniego ticku w powietrzu (SpeedB). */
    public double speedPrevH;
    /** Skumulowany nadmiar nad lancuch pędu w tej fazie powietrznej (SpeedB). */
    public double speedExcessSum;
    /** Seria przekroczen pedu w poznych tickach powietrza (SpeedB). */
    public int speedBStreak;
    /** Delty yaw z kazdego pakietu ruchu (KillAuraJ GCD), limit 24. */
    public final Deque<Double> gcdYawDeltas = new ArrayDeque<Double>();
    /** Delty pitch z kazdego pakietu ruchu (KillAuraJ GCD), limit 24. */
    public final Deque<Double> gcdPitchDeltas = new ArrayDeque<Double>();
    /** Poprzedni yaw do delt GCD (nie rusza lastYaw uzywanych przez KillAuraI). */
    public float gcdPrevYaw;
    /** Poprzedni pitch do delt GCD. */
    public float gcdPrevPitch;
    /** Czy mamy pare poprzednich rotacji dla GCD. */
    public boolean gcdHasPrev;
    /** Seria ocen off-grid z rzedu (KillAuraJ). */
    public int gcdOffStreak;
    /** Ostatnia flaga GCD w ms (throttle ocen). */
    public long lastGcdFlagMs;

    /**
     * Pelny reset pedu i okna GCD: teleport, smierc, zmiana gamemode,
     * wyjscie z pojazdu - nowa faza ruchu, stare delty nie moga
     * dowodzic aim-asysty ani pedu.
     */
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

    // --- velocity ---
    public Vector knockbackVelocity;
    public Location knockbackLoc;
    /** Pozycja atakujacego w chwili ciosu (kierunek odrzutu). */
    public Location knockbackFrom;
    public int knockbackWaitTicks;
    public boolean awaitingVelocity;
    /** Czas wyslania pakietu velocity przez serwer (ms). */
    public long knockbackTimeMs;
    /** Ostatnie vanilla obrazenia od upadku (ms) - NoFall nie dubluje. */
    public long lastFallDamageMs;
    /** Ostatnio zaobserwowana podniesiona reka (ms) - multitool flick. */
    public long lastRaiseSeenMs;
    /** Ostatnio zaobserwowana opuszczona reka (ms) - multitool flick. */
    public long lastUnraiseSeenMs;

    // --- exemptiony na knockback / sila zewnetrzna (Speed FP) ---
    /** Czas ostatniego zakonczenia skoku (ladowanie) w ms. */
    public long lastLandMs;

    // --- Knockback (PlayerVelocityEvent): shared exemption for movement checks ---
    /** Timestamp ostatniego pakietu velocity od serwera (ms). */
    public long kbArmedMs;
    /** Pozostale ticki immunizacji na sygnaly "no gravity/recoil". */
    public int kbTicksLeft;
    /** Timestamp ostatniego odrzutu/impulsu wplywajacego na predkosc pozioma. */
    public long kbSinceBigMs;

    // --- nofall ---
    public double fallDistAcc;
    public boolean fallingAcc;
    /** Ruchy z rzedu poza serwerowym gruntem (schody nie licza). */
    public int serverAirTicks;
    /** Ostatnia weryfikacja botem KillAura (ms). */
    public long lastBotVerifyMs;
    /** Tiki "standing in air" nad gruntem (NoFall No-Ground). */
    public int noFallPlateauTicks;
    public long lastGroundTickMs;
    /** Surowa przerwa miedzy ruchami pozycyjnymi w ms (takze <180). */
    public long lastRawGapMs;
    /** Kiedy zmierzono ostatnia surowa przerwe (recovery-asyl C). */
    public long lastRawGapAt;
    
    // --- scaffold / break / inventory ---
    public final Deque<Long> placeTimes = new ArrayDeque<Long>();
    public final Deque<Long> breakTimes = new ArrayDeque<Long>();
    /** Start kopania (BlockDamageEvent) do FastBreakB. */
    public long digStartMs;
    public int digX;
    public int digY;
    public int digZ;
    public float digHardness;
    /** Seria instant-lamania twardych blokow. */
    public int fastBreakBStreak;
    /** Osobna kolejka Nukera (FastBreak czysci wlasna przy fladze). */
    public final Deque<Long> nukeTimes = new ArrayDeque<Long>();
    public final Deque<Long> invClickTimes = new ArrayDeque<Long>();
    public long lastPlaceTime;
    public long lastBreakTime;
    public int scaffoldAirPlaces;

    // --- timer: bilans 50 ms za ruch (model Grima) ---
    public long timerBalanceNs;
    public long timerLastExceedNs;
    public int timerStreak;

    // --- FakeLag: synchronizacja ruchu i ataku ---
    /** Znaczniki czasu ruchow ze zmiana pozycji (ms), limit 120. */
    public final Deque<Long> moveTimes = new ArrayDeque<Long>();
    /** Ostatni ruch ze zmiana pozycji w ms. */
    public long lastMoveMs;
    /** Ostatni dowolny pakiet ruchu (takze sam obrot glowy) w ms. */
    public long lastAnyPacketMs;
    /** Ostatnia przerwa w ruchu pozycyjnym w ms. */
    public long lastMoveGapMs;
    /** Rozmiar ostatniego burstu po przerwie. */
    public int lastBurstSize;
    /** Seria epizodow choke potwierdzonych z rzedu. */
    public int chokeStreak;
    /** Czas ostatniego epizodu choke w ms (wygasanie serii). */
    public long lastChokeMs;
    /** Seria krotkich luk (MoonLight 200 ms) z rzedu. */
    public int shortChokeStreak;
    /** Ostatnie zamarzniecie klienta (legalny lag FPS) w ms: azyl dla epizodow. */
    public long lastFreezeMs;
    /** Ostatnia flaga desyncu czasow w ms (throttle 8 s). */
    public long lastDesyncFlagMs;
    /** Czas ostatniej krotkiej luki w ms. */
    public long lastShortChokeMs;
    /** Ostatni atak w ms (do FakeLagB). */
    public long lastAttackMs;
    /** Licznik luk wlaczonych do LagGuard w oknie 60 s. */
    public final Deque<Long> bigGaps = new ArrayDeque<Long>();
    /** Ostatnie odstepy miedzy ruchami pozycyjnymi (ms), limit 40. */
    public final Deque<Long> moveGaps = new ArrayDeque<Long>();

    // --- verbose i diagnostyka ---
    public long suspiciousCount;
    public long flagCount;

    // --- misc ---
    public int pingCache;
    public long pingCacheTime;
    /** Wyglaszona srednia pingu (EMA 0.3) do oceny stabilnosci lacza. */
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

    /** Czy gracz jest swiezo po evencie ktory tlumaczy dziwny ruch. */
    public boolean movementExempt() {
        long now = System.currentTimeMillis();
        return now - joinTime < 3000L
            || now - lastTeleport < 1000L
            || now - lastRespawn < 3000L
            || now - lastVehicleExit < 1500L
            || now < setbackGraceMs;
    }

    /**
     * Immunizacja ruchu po knockbacku (zombie, strzala, TNT, łuk, uderzenie).
     * Grawitacja jest legalnie zaburzona przez ticki odbicia i lotu po
     * odrzucie, wiec checki fly/glidelike nie oceniaja tych tickow.
     */
    public boolean velocityExempt(org.bukkit.entity.Player p, long now) {
        return kbTicksLeft > 0 || now - kbSinceBigMs < 1200L;
    }

    /** Wrzuc timestamp do kolejki z limitem rozmiaru. */
    public static void pushCapped(Deque<Long> q, long value, int cap) {
        q.addLast(Long.valueOf(value));
        while (q.size() > cap) {
            q.pollFirst();
        }
    }

    /** Wrzuc wartosc double do kolejki z limitem. */
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
