package pl.floppaac;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import pl.floppaac.alert.AlertManager;
import pl.floppaac.check.CheckManager;
import pl.floppaac.command.FloppaCommand;
import pl.floppaac.data.DataManager;
import pl.floppaac.listener.CombatListener;
import pl.floppaac.listener.MovementListener;
import pl.floppaac.listener.PlayerListener;
import pl.floppaac.util.TpsMonitor;

public class FloppaAC extends JavaPlugin {

    public static final String VERSION = "1.8.3";

    private static FloppaAC instance;
    private DataManager dataManager;
    private CheckManager checkManager;
    private AlertManager alertManager;
    private TpsMonitor tpsMonitor;

    public static FloppaAC getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        dataManager = new DataManager();
        alertManager = new AlertManager(this);
        checkManager = new CheckManager(this);
        checkManager.registerAll();
        tpsMonitor = new TpsMonitor();

        Bukkit.getPluginManager().registerEvents(new MovementListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        FloppaCommand cmd = new FloppaCommand(this);
        if (getCommand("floppaac") != null) {
            getCommand("floppaac").setExecutor(cmd);
            getCommand("floppaac").setTabCompleter(cmd);
        }

        Bukkit.getScheduler().runTaskTimer(this, tpsMonitor, 1L, 1L);

        long decaySeconds = getConfig().getLong("vl-decay-seconds", 30L);
        if (decaySeconds > 0) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                    new Runnable() {
                        @Override
                        public void run() {
                            dataManager.decayAll();
                        }
                    }, decaySeconds * 20L, decaySeconds * 20L);
        }

        getLogger().info("FloppaAC " + VERSION + " enabled. Checks: " + checkManager.size());
    }

    @Override
    public void onDisable() {
        getLogger().info("FloppaAC disabled.");
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public AlertManager getAlertManager() {
        return alertManager;
    }

    public TpsMonitor getTpsMonitor() {
        return tpsMonitor;
    }
}
