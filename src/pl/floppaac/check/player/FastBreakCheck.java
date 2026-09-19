package pl.floppaac.check.player;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.DigTime;
import pl.floppaac.util.MoveUtil;

/**
 * FastBreakA: kopanie szybciej niz pozwala narzedzie.
 * Bloki instant (trawa, kwiaty, pochodnie, dywany, sadzonki,
 * szyny, pnacza) maja osobny wysoki limit i nigdy nie sa
 * weryfikowane czasem. Reszta ma limit tempa.
 * Historia: incydent z flagowaniem trawy jest tu niemozliwy,
 * bo tier INSTANT jest odfiltrowany przed liczeniem.
 */
public class FastBreakCheck extends Check {

    public FastBreakCheck(FloppaAC plugin) {
        super(plugin, "FastBreakA", CheckType.PLAYER);
    }

    public void handle(Player player, PlayerData data, Block broken) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (isInstant(broken.getType())) {
            return;
        }
        handleInstantHard(player, data, broken);
        long now = System.currentTimeMillis();
        PlayerData.pushCapped(data.breakTimes, now, 30);

        long cutoff = now - 1000L;
        int n = 0;
        for (long t : data.breakTimes) {
            if (t >= cutoff) {
                n++;
            }
        }
        if (n > cfgInt("max-blocks-per-second", 4)) {
            flag(player, data, n + " blocks/s");
            data.breakTimes.clear();
        }
    }

    /** Start kopania (BlockDamageEvent). */
    public void onDigStart(Player player, PlayerData data, Block block) {
        if (player.getGameMode() == GameMode.CREATIVE || isInstant(block.getType())) {
            return;
        }
        data.digStartMs = System.currentTimeMillis();
        data.digX = block.getX();
        data.digY = block.getY();
        data.digZ = block.getZ();
        try {
            data.digHardness = block.getType().getHardness();
        } catch (NoSuchMethodError e) {
            data.digHardness = 0.0f;
        }
    }

    /**
     * FastBreakB: kopanie szybsze niz pozwala dolne oszacowanie vanilla.
     * Model liczy narzedzie, Efficiency, Haste, powietrze i wode.
     * Zawsze zaklada wlasciwe narzedzie, wiec legalne kopanie jest
     * wolniejsze albo rowne. Flaga ponizej 45 procent w serii 3.
     * Niezniszczalne (bedrock) lamane w survival to natychmiastowa flaga.
     */
    private void handleInstantHard(Player player, PlayerData data, Block broken) {
        if (data.digStartMs == 0L) {
            return;
        }
        if (broken.getX() != data.digX || broken.getY() != data.digY
                || broken.getZ() != data.digZ) {
            data.fastBreakBStreak = 0;
            return;
        }
        long tookMs = System.currentTimeMillis() - data.digStartMs;
        data.digStartMs = 0L;
        double hardness = data.digHardness;
        if (hardness < 0.0) {
            flagAs("FastBreakB", player, data, broken.getType().name() + " unbreakable");
            return;
        }
        long tookTicks = Math.max(1L, tookMs / 50L);
        long expected = DigTime.expectedTicks(hardness,
                DigTime.toolSpeed(heldName(player), broken.getType().name()),
                efficiency(player), haste(player),
                !player.isOnGround(), MoveUtil.inLiquid(player), hasAqua(player));
        if (DigTime.isImpossible(tookTicks, expected)) {
            data.fastBreakBStreak++;
            if (data.fastBreakBStreak >= 3) {
                flagAs("FastBreakB", player, data, String.format(
                        "%s %d/%d tickow x%d", broken.getType().name(),
                        tookTicks, expected, data.fastBreakBStreak));
                data.fastBreakBStreak = 0;
            }
        } else {
            data.fastBreakBStreak = Math.max(0, data.fastBreakBStreak - 1);
        }
    }

    private String heldName(Player player) {
        try {
            if (player.getInventory().getItemInMainHand() != null) {
                return player.getInventory().getItemInMainHand().getType().name();
            }
        } catch (NoSuchMethodError e) {
            return "HAND";
        }
        return "HAND";
    }

    private int efficiency(Player player) {
        try {
            org.bukkit.inventory.ItemStack hand =
                    player.getInventory().getItemInMainHand();
            if (hand != null) {
                org.bukkit.enchantments.Enchantment eff =
                        org.bukkit.enchantments.Enchantment.getByName("EFFICIENCY");
                if (eff == null) {
                    eff = org.bukkit.enchantments.Enchantment.getByName("DIG_SPEED");
                }
                if (eff != null) {
                    return hand.getEnchantmentLevel(eff);
                }
            }
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            return 0;
        }
        return 0;
    }

    private int haste(Player player) {
        try {
            org.bukkit.potion.PotionEffectType fast =
                    org.bukkit.potion.PotionEffectType.getByName("FAST_DIGGING");
            if (fast == null) {
                fast = org.bukkit.potion.PotionEffectType.getByName("HASTE");
            }
            if (fast != null && player.hasPotionEffect(fast)
                    && player.getPotionEffect(fast) != null) {
                return player.getPotionEffect(fast).getAmplifier() + 1;
            }
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            return 0;
        }
        return 0;
    }

    private boolean hasAqua(Player player) {
        try {
            org.bukkit.inventory.ItemStack helm = player.getInventory().getHelmet();
            if (helm != null) {
                org.bukkit.enchantments.Enchantment aqua =
                        org.bukkit.enchantments.Enchantment.getByName("WATER_WORKER");
                if (aqua != null) {
                    return helm.getEnchantmentLevel(aqua) > 0;
                }
            }
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            return false;
        }
        return false;
    }

    /** Bloki lamane jednym uderzeniem. Nigdy nie flaguja. */
    public static boolean isInstant(Material m) {
        String n = m.name();
        return n.contains("GRASS")
            || n.contains("FERN")
            || n.contains("FLOWER")
            || n.contains("TORCH")
            || n.contains("BUTTON")
            || n.contains("CARPET")
            || n.contains("SAPLING")
            || n.contains("RAIL")
            || n.contains("VINE")
            || n.contains("SNOW")
            || n.contains("DEAD_BUSH")
            || n.contains("TULIP")
            || n.contains("DAISY")
            || n.equals("RED_MUSHROOM")
            || n.equals("BROWN_MUSHROOM")
            || n.equals("SUGAR_CANE")
            || n.equals("LADDER")
            || n.equals("LEVER")
            || n.equals("REDSTONE_WIRE")
            || n.equals("STRING")
            || n.equals("TRIPWIRE")
            || n.equals("FIRE")
            || n.equals("WHEAT")
            || n.equals("CARROTS")
            || n.equals("POTATOES")
            || n.equals("BEETROOTS")
            || n.equals("NETHER_WART")
            || n.contains("CANDLE")
            || n.contains("PETALS");
    }
}
