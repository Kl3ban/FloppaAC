package pl.floppaac.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.CheckManager;
import pl.floppaac.check.combat.AutoClickerCheck;
import pl.floppaac.check.combat.CriticalsCheck;
import pl.floppaac.check.combat.KillAuraBotCheck;
import pl.floppaac.check.combat.KillAuraCheck;
import pl.floppaac.check.combat.MultiActionsCheck;
import pl.floppaac.check.combat.ReachCheck;
import pl.floppaac.check.combat.VelocityCheck;
import pl.floppaac.check.movement.FakeLagCheck;
import pl.floppaac.check.player.InventoryCheck;
import pl.floppaac.data.PlayerData;

/**
 * Walka: atakujacy (reach, aura, clicker, krytyki, fakelag)
 * i ofiara (velocity). Referencje cachowane w polach.
 */
public class CombatListener implements Listener {

    private final FloppaAC plugin;
    private final FakeLagCheck fakeLag;
    private final ReachCheck reach;
    private final KillAuraCheck aura;
    private final KillAuraBotCheck botVerify;
    private final MultiActionsCheck multi;
    private final AutoClickerCheck clicker;
    private final CriticalsCheck criticals;
    private final InventoryCheck inventory;
    private final VelocityCheck velocity;

    public CombatListener(FloppaAC plugin) {
        this.plugin = plugin;
        CheckManager cm = plugin.getCheckManager();
        fakeLag = cm.get("FakeLagA", FakeLagCheck.class);
        reach = cm.get("ReachA", ReachCheck.class);
        aura = cm.get("KillAuraA", KillAuraCheck.class);
        botVerify = cm.get("KillAuraBot", KillAuraBotCheck.class);
        multi = cm.get("MultiA", MultiActionsCheck.class);
        clicker = cm.get("AutoClickerA", AutoClickerCheck.class);
        criticals = cm.get("CriticalsA", CriticalsCheck.class);
        inventory = cm.get("InventoryA", InventoryCheck.class);
        velocity = cm.get("VelocityA", VelocityCheck.class);
    }

    /**
     * Bot weryfikacyjny: anulowanie obrazen na LOWEST, zeby wlasciwy
     * skaner walki (HIGH, ignoreCancelled) bota w ogole nie widzial.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBotHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) {
            return;
        }
        if (!(e.getDamager() instanceof Player)) {
            if (botVerify.isBot(e.getEntity().getUniqueId())) {
                e.setCancelled(true);
            }
            return;
        }
        if (botVerify.handleBotHit((Player) e.getDamager(), (LivingEntity) e.getEntity())) {
            e.setCancelled(true);
        }
    }

    /** Ochrona bota przed ogniem, lawa i innymi zrodlami. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBotHurt(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity)) {
            return;
        }
        if (botVerify.isBot(e.getEntity().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) {
            return;
        }
        if (!(e.getEntity() instanceof LivingEntity)) {
            return;
        }
        Player attacker = (Player) e.getDamager();
        LivingEntity victim = (LivingEntity) e.getEntity();
        if (attacker.equals(victim)) {
            return;
        }
        // Sweep mieczem: jeden zamach, wiele eventow obszarowych bez
        // wlasnego machniecia i celowania. Skaner aury ich nie ocenia,
        // bo kazdy wtorny cel dawalby falszywa flage NoSwing i kata.
        if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return;
        }

        PlayerData data = plugin.getDataManager().get(attacker);
        data.lastAttackMs = System.currentTimeMillis();

        boolean lagDesync = fakeLag.handleAttack(attacker, data);
        if (lagDesync) {
            // Trafienie z wstrzymanego hitboxa (fakelag): anulowane na
            // miejscu. Cheater nie zadaje obrazen, ktorych server nie widzi.
            e.setCancelled(true);
        }
        // InventoryWalk: cios z otwartym ekwipunkiem - ghost trafienia
        // plus flaga InventoryC od pierwszego ciosu.
        if (inventory.handleDamage(attacker, data)) {
            e.setCancelled(true);
        }
        // Multitool: cios z podniesiona reka - ghost trafienia.
        if (multi.handleAttack(attacker, data)) {
            e.setCancelled(true);
        }
        reach.handle(attacker, data, victim);
        aura.handleAngle(attacker, data, victim);
        aura.handleNoRotation(attacker, data, victim);
        aura.handleNoSwing(attacker, data);
        aura.handleLineOfSight(attacker, data, victim);
        aura.handleTargets(attacker, data, victim);
        double err = KillAuraCheck.aimError(attacker, victim);
        aura.handleLinear(attacker, data,
                attacker.getLocation().getYaw(), attacker.getLocation().getPitch(), err);
        clicker.handle(attacker, data);
        aura.handleReactive(attacker, data, victim, System.currentTimeMillis());
        aura.handleCharge(attacker, data);
        try {
            double dist = attacker.getEyeLocation().distance(victim.getEyeLocation());
            aura.handleOrbit(attacker, data, victim, dist);
        } catch (IllegalArgumentException ex) {
            // Inne swiaty, brak dystansu do orbity.
        }
        criticals.handle(attacker, data, victim, e.getDamage());
        inventory.handleAttack(attacker, data);
        // Heurystyki dojrzaly: weryfikacja botem zamiast kary.
        try {
            botVerify.maybeVerify(attacker, data);
        } catch (Exception ex) {
            // Weryfikacja nie blokuje walki.
        }
        // Ofiara bedaca graczem: zapisz kierunek odrzutu do Velocity.
        if (victim instanceof Player) {
            Player victimPlayer = (Player) victim;
            velocity.onDamageFrom(victimPlayer,
                    plugin.getDataManager().get(victimPlayer),
                    attacker.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHurt(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) {
            return;
        }
        // Vanilla obrazenia od upadku: znacznik dla NoFall (nie dublujemy)
        // oraz reset akumulatora spadku. Osobno przed filtrem przyczyn,
        // bo FALL nie jest powodem odrzutu ani exemptionu predkosci.
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL
                || e.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
            Player fallVictim = (Player) e.getEntity();
            PlayerData fd = plugin.getDataManager().get(fallVictim);
            fd.lastFallDamageMs = System.currentTimeMillis();
            fd.fallDistAcc = 0.0;
            fd.fallingAcc = false;
            fd.noFallPlateauTicks = 0;
            return;
        }
        switch (e.getCause()) {
            case ENTITY_ATTACK:
            case ENTITY_SWEEP_ATTACK:
            case PROJECTILE:
            case ENTITY_EXPLOSION:
            case BLOCK_EXPLOSION:
            case FIRE:
            case FIRE_TICK:
            case POISON:
            case WITHER:
                break;
            default:
                return;
        }
        Player victim = (Player) e.getEntity();
        PlayerData vd = plugin.getDataManager().get(victim);
        long now = System.currentTimeMillis();
        // Wszystko co legalnie szarpie ruch (odrzut, eksplozja, strzala,
        // ogien, trucizna, wither) wspolnie zasilaja karencje velocity.
        // (W 1.7.5 FIRE/POISON/WITHER byly odsiewane wczesniej i ten
        // exempt nigdy nie odpalil - przyczyna FP zombie podpalonego.)
        switch (e.getCause()) {
            case ENTITY_ATTACK:
            case ENTITY_SWEEP_ATTACK:
            case PROJECTILE:
            case ENTITY_EXPLOSION:
            case BLOCK_EXPLOSION:
            case FIRE:
            case FIRE_TICK:
            case POISON:
            case WITHER:
                vd.kbSinceBigMs = now;
                break;
            default:
                break;
        }
        // Uzbrajamy VelocityA tylko przy przyczynach z realnym odrzutem;
        // ogien/trucizna nie nadaja pedu, wiec oczekiwanie ruchu FP-owaloby.
        switch (e.getCause()) {
            case ENTITY_ATTACK:
            case ENTITY_SWEEP_ATTACK:
            case PROJECTILE:
            case ENTITY_EXPLOSION:
            case BLOCK_EXPLOSION:
                velocity.onDamage(victim, vd);
                break;
            default:
                break;
        }
    }

    /**
     * PlayerVelocityEvent: serwer wyslal pakiet odrzutu (zombie, strzala,
     * TNT, nasz pushback). Tylko tu wiemy na 100%, ze nastepne ticki
     * maja legalnie zaburzona grawitacje - checki fly/speed biora
     * karencje z data.velocityExempt() i progiem kb w PlayerData.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent e) {
        PlayerData vd = plugin.getDataManager().get(e.getPlayer());
        long now = System.currentTimeMillis();
        vd.kbArmedMs = now;
        vd.knockbackTimeMs = now;
        Vector v = e.getVelocity();
        try {
            vd.knockbackVelocity = v.clone();
        } catch (Exception ex) {
            vd.knockbackVelocity = null;
        }
        double horiz = Math.hypot(v.getX(), v.getZ());
        double strength = Math.max(horiz, Math.abs(v.getY()) * 0.8);
        // Odbicie pionowe po odrzucie moze wygladac jak poczatek lotu:
        // immunizacja na sygnaly "brak grawitacji" skalowana sila odrzutu.
        vd.kbTicksLeft = 20 + (int) Math.min(40, strength * 40.0);
        // Odrzut moze wypchnac poza limit predkosci poziomej.
        vd.kbSinceBigMs = now;
    }
}
