package pl.floppaac.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.CheckManager;
import pl.floppaac.check.combat.MultiActionsCheck;
import pl.floppaac.check.player.FastBreakCheck;
import pl.floppaac.check.player.InventoryCheck;
import pl.floppaac.check.player.NukerCheck;
import pl.floppaac.check.movement.SprintSpoofCheck;
import pl.floppaac.check.player.ScaffoldCheck;
import pl.floppaac.data.PlayerData;

/** Exemptiony oraz checki blokow i inventory. */
public class PlayerListener implements Listener {

    private final FloppaAC plugin;
    private final ScaffoldCheck scaffold;
    private final SprintSpoofCheck sprint;
    private final FastBreakCheck fastBreak;
    private final NukerCheck nuker;
    private final MultiActionsCheck multi;
    private final pl.floppaac.check.player.AntiAutoWebCheck webTrap;
    private final pl.floppaac.check.player.AutoTrapCheck autoTrap;
    private final InventoryCheck inventory;
    private final pl.floppaac.check.combat.KillAuraBotCheck botVerify;

    public PlayerListener(FloppaAC plugin) {
        this.plugin = plugin;
        CheckManager cm = plugin.getCheckManager();
        scaffold = cm.get("ScaffoldA", ScaffoldCheck.class);
        sprint = cm.get("SprintA", SprintSpoofCheck.class);
        fastBreak = cm.get("FastBreakA", FastBreakCheck.class);
        nuker = cm.get("NukerA", NukerCheck.class);
        multi = cm.get("MultiA", MultiActionsCheck.class);
        webTrap = cm.get("WebB", pl.floppaac.check.player.AntiAutoWebCheck.class);
        autoTrap = cm.get("AutoTrapA", pl.floppaac.check.player.AutoTrapCheck.class);
        inventory = cm.get("InventoryA", InventoryCheck.class);
        botVerify = cm.get("KillAuraBot", pl.floppaac.check.combat.KillAuraBotCheck.class);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerData data = plugin.getDataManager().get(p);
        long now = System.currentTimeMillis();
        data.joinTime = now;
        data.lastMoveMs = now;
        data.lastAnyPacketMs = now;
        data.moveTimes.clear();
        data.moveGaps.clear();
        data.chokeStreak = 0;
        if (p.getName() != null && p.hasPermission("floppaac.staff")) {
            plugin.getAlertManager().info(p, "FloppaAC aktywny. Tryb testowy: kicki zamiast banow.");
            plugin.getAlertManager().info(p, "Komendy: /floppaac verbose, /floppaac status, /floppaac vl.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        try {
            botVerify.cancel(e.getPlayer().getUniqueId());
        } catch (Exception ex) {
            // Ignorowane.
        }
        plugin.getDataManager().remove(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        PlayerData data = plugin.getDataManager().get(e.getPlayer());
        data.lastTeleport = System.currentTimeMillis();
        data.knockbackVelocity = null;
        data.awaitingVelocity = false;
        data.lastRaiseSeenMs = 0L;
        data.lastUnraiseSeenMs = 0L;
        data.airTicks = 0;
        data.hoverTicks = 0;
        data.riseStreak = 0;
        data.speedStreak = 0;
        data.chokeStreak = 0;
        data.fallDistAcc = 0.0;
        data.fallingAcc = false;
        data.kbTicksLeft = 0;
        data.moveTimes.clear();
        data.setbackLoc = e.getTo() != null ? e.getTo().clone() : null;
        data.noFallPlateauTicks = 0;
        data.serverAirTicks = 0;
        data.resetMomentumGcd();
        try {
            botVerify.cancel(e.getPlayer().getUniqueId());
        } catch (Exception ex) {
            // Ignorowane.
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        PlayerData data = plugin.getDataManager().get(e.getPlayer());
        data.lastRespawn = System.currentTimeMillis();
        data.fallDistAcc = 0.0;
        data.fallingAcc = false;
        data.noFallPlateauTicks = 0;
        data.serverAirTicks = 0;
        try {
            botVerify.cancel(e.getPlayer().getUniqueId());
        } catch (Exception ex) {
            // Ignorowane.
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        // Smierc resetuje faze spadku i stany ruchu: respawn i powrot
        // z ekranu smierci to legalna przerwa, nie epizod FakeLag.
        PlayerData data = plugin.getDataManager().get(e.getEntity());
        data.fallDistAcc = 0.0;
        data.fallingAcc = false;
        data.kbTicksLeft = 0;
        data.airTicks = 0;
        data.hoverTicks = 0;
        data.chokeStreak = 0;
        data.shortChokeStreak = 0;
        data.awaitingVelocity = false;
        data.knockbackVelocity = null;
        data.noFallPlateauTicks = 0;
        data.serverAirTicks = 0;
        data.lastRaiseSeenMs = 0L;
        data.lastUnraiseSeenMs = 0L;
        data.resetMomentumGcd();
        try {
            botVerify.cancel(e.getEntity().getUniqueId());
        } catch (Exception ex) {
            // Ignorowane.
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent e) {
        if (e.getExited() instanceof Player) {
            plugin.getDataManager().get((Player) e.getExited()).lastVehicleExit =
                    System.currentTimeMillis();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent e) {
        PlayerData data = plugin.getDataManager().get(e.getPlayer());
        long now = System.currentTimeMillis();
        data.lastSwingMs = now;
        // Machniecie tuz po ciosie to para atak-machniecie (kolejnosc
        // w burście FakeLag i opoznienie pakietu przy wyzszym pingu),
        // nie aura bez machniec.
        if (now - data.lastAttackMs < 750L) {
            data.noSwingStrikes = 0;
        }
        multi.handleSwing(e.getPlayer(), data);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        plugin.getDataManager().get(e.getPlayer()).lastDropMs = System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGuiOpen(InventoryOpenEvent e) {
        if (e.getPlayer() instanceof Player) {
            PlayerData data = plugin.getDataManager().get((Player) e.getPlayer());
            data.invOpen = true;
            data.invAtkStreak = 0;
            data.invMoveStreak = 0;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGuiClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player) {
            PlayerData data = plugin.getDataManager().get((Player) e.getPlayer());
            data.invOpen = false;
            data.invAtkStreak = 0;
            data.invMoveStreak = 0;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSprintToggle(PlayerToggleSprintEvent e) {
        if (!e.isSprinting()) {
            return;
        }
        Player player = e.getPlayer();
        sprint.handleSprintStart(player, plugin.getDataManager().get(player));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        PlayerData data = plugin.getDataManager().get(player);
        scaffold.handle(player, data, e.getBlockPlaced());
        webTrap.handle(player, data, e.getBlockPlaced());
        autoTrap.handle(player, data, e.getBlockPlaced());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGamemodeChange(PlayerGameModeChangeEvent e) {
        // Zmiana trybu (creative <-> survival) bez teleportu potrafi
        // zostawic leciacy akumulator spadku albo seria wznoszenia.
        PlayerData data = plugin.getDataManager().get(e.getPlayer());
        data.fallDistAcc = 0.0;
        data.fallingAcc = false;
        data.airTicks = 0;
        data.hoverTicks = 0;
        data.riseStreak = 0;
        data.flyBigStreak = 0;
        data.glideStreak = 0;
        data.speedStreak = 0;
        data.lastTeleport = System.currentTimeMillis();
        data.noFallPlateauTicks = 0;
        data.serverAirTicks = 0;
        data.resetMomentumGcd();
        try {
            botVerify.cancel(e.getPlayer().getUniqueId());
        } catch (Exception ex) {
            // Ignorowane.
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDigStart(BlockDamageEvent e) {
        Player player = e.getPlayer();
        fastBreak.onDigStart(player, plugin.getDataManager().get(player), e.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        PlayerData data = plugin.getDataManager().get(player);
        fastBreak.handle(player, data, e.getBlock());
        nuker.handle(player, data, e.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) e.getWhoClicked();
        inventory.handle(player, plugin.getDataManager().get(player));
    }
}
