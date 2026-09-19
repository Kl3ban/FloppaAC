package pl.floppaac.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.CheckManager;
import pl.floppaac.check.combat.VelocityCheck;
import pl.floppaac.check.combat.KillAuraJCheck;
import pl.floppaac.check.combat.MultiActionsCheck;
import pl.floppaac.check.movement.AntiWebCheck;
import pl.floppaac.check.movement.FakeLagCheck;
import pl.floppaac.check.movement.FlyBCheck;
import pl.floppaac.check.movement.FlyCheck;
import pl.floppaac.check.movement.FlyGlideCheck;
import pl.floppaac.check.movement.GroundSpoofCheck;
import pl.floppaac.check.movement.JesusCheck;
import pl.floppaac.check.movement.NoFallCheck;
import pl.floppaac.check.movement.PhaseCheck;
import pl.floppaac.check.movement.SpeedBCheck;
import pl.floppaac.check.movement.SpeedCheck;
import pl.floppaac.check.movement.SpiderCheck;
import pl.floppaac.check.movement.SprintSpoofCheck;
import pl.floppaac.check.movement.StepCheck;
import pl.floppaac.check.movement.TimerCheck;
import pl.floppaac.check.player.BadPacketsCheck;
import pl.floppaac.check.player.InventoryCheck;
import pl.floppaac.check.player.LagGuardCheck;
import pl.floppaac.check.player.NoSlowCheck;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;
import pl.floppaac.util.RotationMath;

/**
 * Centralny ruch gracza. Raz liczymy delty i ksiegowosc FakeLag,
 * potem odpalamy wszystkie checki movement.
 * Referencje checkow cachowane w polach (zero lookupow na event).
 */
public class MovementListener implements Listener {

    private final FloppaAC plugin;

    private final TimerCheck timer;
    private final LagGuardCheck lagGuard;
    private final FakeLagCheck fakeLag;
    private final FlyCheck fly;
    private final FlyBCheck flyB;
    private final FlyGlideCheck flyC;
    private final GroundSpoofCheck groundSpoof;
    private final SpeedCheck speed;
    private final JesusCheck jesus;
    private final StepCheck step;
    private final NoFallCheck noFall;
    private final SpiderCheck spider;
    private final SprintSpoofCheck sprint;
    private final AntiWebCheck web;
    private final PhaseCheck phase;
    private final BadPacketsCheck badPackets;
    private final NoSlowCheck noSlow;
    private final InventoryCheck inventory;
    private final VelocityCheck velocity;
    private final SpeedBCheck speedB;
    private final KillAuraJCheck killAuraJ;
    private final MultiActionsCheck multi;

    public MovementListener(FloppaAC plugin) {
        this.plugin = plugin;
        CheckManager cm = plugin.getCheckManager();
        timer = cm.get("TimerA", TimerCheck.class);
        lagGuard = cm.get("LagGuard", LagGuardCheck.class);
        fakeLag = cm.get("FakeLagA", FakeLagCheck.class);
        fly = cm.get("FlyA", FlyCheck.class);
        flyB = cm.get("FlyB", FlyBCheck.class);
        flyC = cm.get("FlyC", FlyGlideCheck.class);
        groundSpoof = cm.get("GroundSpoofA", GroundSpoofCheck.class);
        speed = cm.get("SpeedA", SpeedCheck.class);
        jesus = cm.get("JesusA", JesusCheck.class);
        step = cm.get("StepA", StepCheck.class);
        noFall = cm.get("NoFallA", NoFallCheck.class);
        spider = cm.get("SpiderA", SpiderCheck.class);
        sprint = cm.get("SprintA", SprintSpoofCheck.class);
        web = cm.get("WebA", AntiWebCheck.class);
        phase = cm.get("PhaseA", PhaseCheck.class);
        badPackets = cm.get("BadPacketsA", BadPacketsCheck.class);
        noSlow = cm.get("NoSlowA", NoSlowCheck.class);
        inventory = cm.get("InventoryA", InventoryCheck.class);
        velocity = cm.get("VelocityA", VelocityCheck.class);
        speedB = cm.get("SpeedB", SpeedBCheck.class);
        killAuraJ = cm.get("KillAuraJ", KillAuraJCheck.class);
        multi = cm.get("MultiA", MultiActionsCheck.class);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) {
            return;
        }
        PlayerData data = plugin.getDataManager().get(player);
        long now = System.currentTimeMillis();

        boolean positionChanged = from.getX() != to.getX()
                || from.getY() != to.getY()
                || from.getZ() != to.getZ();

        // Kazdy pakiet ruchu, takze sam obrot glowy.
        data.anyGapMs = now - data.lastAnyPacketMs;
        data.lastAnyPacketMs = now;
        // Multitool: probkowanie reki na kazdym pakiecie (flick).
        try {
            multi.handleMove(player, data);
        } catch (Exception ex) {
            // Brak probki nie blokuje ruchu.
        }

        // --- KillAuraJ: probkowanie delt rotacji (kazdy pakiet ruchu,
        // takze sam obrot glowy bez zmiany pozycji) ---
        if (data.gcdHasPrev) {
            double dyaw = RotationMath.wrapDegrees(to.getYaw() - data.gcdPrevYaw);
            double dpitch = RotationMath.wrapDegrees(to.getPitch() - data.gcdPrevPitch);
            if (Math.abs(dyaw) >= RotationMath.MIN_DELTA) {
                PlayerData.pushCappedD(data.gcdYawDeltas, dyaw, 24);
            }
            if (Math.abs(dpitch) >= RotationMath.MIN_DELTA) {
                PlayerData.pushCappedD(data.gcdPitchDeltas, dpitch, 24);
            }
        }
        data.gcdPrevYaw = to.getYaw();
        data.gcdPrevPitch = to.getPitch();
        data.gcdHasPrev = true;

        if (!positionChanged) {
            // Sam obrot glowy to tez pakiet z flaga gruntu (jak u Grima).
            // NoFall i GroundSpoof oceniaja deklarowany grunt, BadPackets pitch.
            noFall.handle(player, data, from, to);
            groundSpoof.handle(player, data, from, to);
            badPackets.handleMove(player, data, to);
            return;
        }

        // --- ksiegowosc FakeLag i Timer ---
        long gap = now - data.lastMoveMs;
        data.lastMoveMs = now;
        PlayerData.pushCapped(data.moveTimes, now, 120);
        PlayerData.pushCapped(data.moveGaps, gap, 40);
        timer.handleIdleGap(data, gap);
        // LagGuard patrzy na przerwe w DOWOLNYCH pakietach (takze obrot),
        // zeby stanie i rozgladanie nie liczylo sie jako luka lacza.
        lagGuard.handleGap(player, data, data.anyGapMs);

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double dy = to.getY() - from.getY();
        double horizontal = Math.hypot(dx, dz);

        if (player.isOnGround()) {
            data.groundTicks++;
            if (data.airTicks >= 3) {
                // Ladowanie po skoku: ped z lotu rozladowuje sie jeszcze
                // przez kilka tickow (SpeedA ma luzniejszy limit).
                data.lastLandMs = now;
            }
            data.airTicks = 0;
            if (!data.movementExempt() && !MoveUtil.inVehicle(player)) {
                data.setbackLoc = from.clone();
                data.lastGroundLoc = from.clone();
                // Bezpieczny grunt poza webem i woda: cel cofania.
                if (!MoveUtil.inWeb(player) && !MoveUtil.inLiquid(player)) {
                    data.lastSafeLoc = from.clone();
                }
            }
        } else {
            data.airTicks++;
            data.groundTicks = 0;
        }
        data.lastDeltaY = dy;
        data.lastHorizontalDist = horizontal;

        // Pakiety ruchu = zywy gracz: ticki immunizacji na knockback maleja.
        if (data.kbTicksLeft > 0) {
            data.kbTicksLeft--;
        }

        // --- checki ---
        timer.handle(player, data);
        fakeLag.handleMove(player, data, gap);
        fly.handle(player, data, from, to);
        flyB.handle(player, data, from, to, dy, horizontal);
        flyC.handle(player, data, dy, horizontal);
        groundSpoof.handle(player, data, from, to);
        speed.handle(player, data, from, to, horizontal);
        speedB.handle(player, data, from, to, horizontal);
        killAuraJ.handle(player, data);
        jesus.handle(player, data, to, horizontal);
        step.handle(player, data, from, to);
        noFall.handle(player, data, from, to);
        spider.handle(player, data, from, to);
        sprint.handle(player, data, from, to, horizontal);
        sprint.handleStates(player, data);
        web.handle(player, data, from, horizontal);
        phase.handle(player, data, from, to);
        badPackets.handleMove(player, data, to);
        noSlow.handle(player, data, from, to, horizontal);
        inventory.handleMove(player, data, from, to, horizontal);
        velocity.handle(player, data);

        data.lastLoc = to.clone();
    }
}
