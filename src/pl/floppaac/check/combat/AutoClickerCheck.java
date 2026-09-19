package pl.floppaac.check.combat;

import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.ClickStats;

/**
 * AutoClickerA: FloppaAC celowo NIE flaguje stabilnego CPS.
 * Utalentowany gracz moze trzymac rowne 8 do 12 CPS przez 5 s.
 * Flaga tylko przy wartosciach nieludzkich:
 * 1) ponad 20 trafien na s,
 * 2) metronom ekstremalny: 30 probek, ponad 10 CPS i stddev ponizej 2 ms,
 * 3) seria 3 podwojnych trafien ponizej 40 ms.
 */
public class AutoClickerCheck extends Check {

    public AutoClickerCheck(FloppaAC plugin) {
        super(plugin, "AutoClickerA", CheckType.COMBAT);
    }

    public void handle(Player attacker, PlayerData data) {
        long now = System.currentTimeMillis();
        if (data.lastHitTime > 0L) {
            long gap = now - data.lastHitTime;
            if (gap > 5L && gap < 2000L) {
                PlayerData.pushCapped(data.clickGaps, gap, 40);
            }
        }
        data.lastHitTime = now;
        PlayerData.pushCapped(data.hitTimes, now, 60);

        int kind = ClickStats.classify(data.clickGaps, data.hitTimes);
        if (kind == 1) {
            double cps = ClickStats.cps(data.hitTimes, 1000L);
            flag(attacker, data, String.format("cps=%.1f (limit 20)", cps));
            data.clickGaps.clear();
            data.hitTimes.clear();
        } else if (kind == 2) {
            double cps = ClickStats.cps(data.hitTimes, 1000L);
            double std = ClickStats.stddev(data.clickGaps);
            flag(attacker, data, String.format("metronom cps=%.1f std=%.1fms", cps, std));
            data.clickGaps.clear();
        } else if (kind == 3) {
            flag(attacker, data, "podwojne trafienia <40ms x3");
            data.clickGaps.clear();
        } else if (data.clickGaps.size() >= 20) {
            double cps = ClickStats.cps(data.hitTimes, 1000L);
            double std = ClickStats.stddev(data.clickGaps);
            if (cps >= 7.0 && std < 8.0) {
                suspicious(attacker, data, String.format(
                        "equal clicks cps=%.1f std=%.1fms (observation)", cps, std));
            }
        }
    }
}
