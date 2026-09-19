package pl.floppaac.check;

import pl.floppaac.FloppaAC;
import pl.floppaac.check.combat.AutoClickerCheck;
import pl.floppaac.check.combat.CriticalsCheck;
import pl.floppaac.check.combat.KillAuraBotCheck;
import pl.floppaac.check.combat.KillAuraCheck;
import pl.floppaac.check.combat.KillAuraJCheck;
import pl.floppaac.check.combat.MultiActionsCheck;
import pl.floppaac.check.combat.ReachCheck;
import pl.floppaac.check.combat.VelocityCheck;
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
import pl.floppaac.check.player.FastBreakCheck;
import pl.floppaac.check.player.InventoryCheck;
import pl.floppaac.check.player.LagGuardCheck;
import pl.floppaac.check.player.NoSlowCheck;
import pl.floppaac.check.player.NukerCheck;
import pl.floppaac.check.player.ScaffoldCheck;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Rejestr wszystkich checkow FloppaAC. */
public class CheckManager {

    private final FloppaAC plugin;
    private final Map<String, Check> checks = new LinkedHashMap<String, Check>();

    public CheckManager(FloppaAC plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        add(new FlyCheck(plugin));
        add(new FlyBCheck(plugin));
        add(new FlyGlideCheck(plugin));
        add(new GroundSpoofCheck(plugin));
        add(new SpeedCheck(plugin));
        add(new SpeedBCheck(plugin));
        add(new TimerCheck(plugin));
        add(new JesusCheck(plugin));
        add(new StepCheck(plugin));
        add(new NoFallCheck(plugin));
        add(new SpiderCheck(plugin));
        add(new SprintSpoofCheck(plugin));
        add(new FakeLagCheck(plugin));
        add(new AntiWebCheck(plugin));
        add(new PhaseCheck(plugin));
        add(new ReachCheck(plugin));
        add(new KillAuraCheck(plugin));
        add(new KillAuraJCheck(plugin));
        add(new KillAuraBotCheck(plugin));
        add(new MultiActionsCheck(plugin));
        add(new pl.floppaac.check.player.AntiAutoWebCheck(plugin));
        add(new pl.floppaac.check.player.AutoTrapCheck(plugin));
        add(new AutoClickerCheck(plugin));
        add(new VelocityCheck(plugin));
        add(new CriticalsCheck(plugin));
        add(new ScaffoldCheck(plugin));
        add(new FastBreakCheck(plugin));
        add(new NukerCheck(plugin));
        add(new BadPacketsCheck(plugin));
        add(new NoSlowCheck(plugin));
        add(new InventoryCheck(plugin));
        add(new LagGuardCheck(plugin));
    }

    private void add(Check check) {
        checks.put(check.name(), check);
    }

    @SuppressWarnings("unchecked")
    public <T extends Check> T get(String name, Class<T> type) {
        return (T) checks.get(name);
    }

    public Check get(String name) {
        return checks.get(name);
    }

    public Collection<Check> all() {
        return checks.values();
    }

    public int size() {
        return checks.size();
    }
}
