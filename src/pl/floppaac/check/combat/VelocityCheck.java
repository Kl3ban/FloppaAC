package pl.floppaac.check.combat;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import pl.floppaac.FloppaAC;
import pl.floppaac.check.Check;
import pl.floppaac.check.CheckType;
import pl.floppaac.data.PlayerData;
import pl.floppaac.util.MoveUtil;
import pl.floppaac.util.PingUtil;

/**
 * VelocityA: brak odrzutu po trafieniu (anti-knockback).
 *
 * Serwer wysyla pakiet velocity (PlayerVelocityEvent) i zapisuje wektor.
 * Po odczekaniu (5 + ping/50 tickow) ofiara musi przemiescic sie
 * w kierunku odrzutu. Ocena jest kierunkowa (rzut na kierunek velocity),
 * wiec celowy sprint w bok nie maskuje cancela, a legalny odrzut
 * (silniejszy od sprintu) zawsze daje dodatni rzut.
 *
 * Poprawki 1.8.0:
 * - onDamage nie nadpisuje kierunku z onDamageFrom (podwojny arm
 *   w tym samym ticku kasowal knockbackFrom i test sciany),
 * - brak warunku isOnGround (cancel w powietrzu tez lapany),
 * - ocena pionu dla odrzutow w gore (eksplozje, punch),
 * - fallback na calkowity ruch gdy brak wektora velocity.
 * Zwolnienia: woda, pajeczyna, drabina, pojazd, lot oraz sciana
 * za plecami ofiary. Seria 2 potwierdzen.
 */
public class VelocityCheck extends Check {

    public VelocityCheck(FloppaAC plugin) {
        super(plugin, "VelocityA", CheckType.COMBAT);
    }

    public void onDamage(Player victim, PlayerData data) {
        // onAttack uzbraja wczesniej z kierunkiem. Drugi arm z onHurt
        // w tym samym ticku nie moze skasowac knockbackFrom.
        if (data.awaitingVelocity && data.knockbackFrom != null) {
            arm(data, victim);
            return;
        }
        data.knockbackLoc = victim.getLocation().clone();
        data.knockbackFrom = null;
        arm(data, victim);
    }

    public void onDamageFrom(Player victim, PlayerData data, Location attackerLoc) {
        data.knockbackLoc = victim.getLocation().clone();
        data.knockbackFrom = attackerLoc == null ? null : attackerLoc.clone();
        arm(data, victim);
    }

    private void arm(PlayerData data, Player victim) {
        int ping = PingUtil.getPing(victim, data);
        data.knockbackWaitTicks = 5 + Math.max(0, ping) / 50;
        data.awaitingVelocity = true;
    }

    public void handle(Player player, PlayerData data) {
        if (!data.awaitingVelocity) {
            return;
        }
        if (data.knockbackWaitTicks > 0) {
            data.knockbackWaitTicks--;
            return;
        }
        data.awaitingVelocity = false;
        if (data.knockbackLoc == null) {
            data.velocityStreak = 0;
            data.knockbackVelocity = null;
            return;
        }
        Location cur;
        try {
            cur = player.getLocation();
        } catch (Exception e) {
            data.velocityStreak = 0;
            data.knockbackVelocity = null;
            return;
        }
        if (data.knockbackLoc.getWorld() == null || cur.getWorld() == null
                || !data.knockbackLoc.getWorld().equals(cur.getWorld())) {
            data.velocityStreak = 0;
            data.knockbackVelocity = null;
            return;
        }
        if (MoveUtil.inLiquid(player) || MoveUtil.inWeb(player)
                || MoveUtil.onClimbable(player) || MoveUtil.inVehicle(player)
                || player.isGliding() || player.isRiptiding()) {
            data.velocityStreak = 0;
            data.knockbackVelocity = null;
            return;
        }
        // Sciana za plecami: odrzut legalnie zerowy.
        if (data.knockbackFrom != null && hasWallBehind(player, data)) {
            data.velocityStreak = 0;
            data.knockbackVelocity = null;
            return;
        }
        double dx = cur.getX() - data.knockbackLoc.getX();
        double dz = cur.getZ() - data.knockbackLoc.getZ();
        double dy = cur.getY() - data.knockbackLoc.getY();
        double moved = Math.hypot(dx, dz);

        boolean cancelled = false;
        String detailKind = "ruch";

        Vector kv = data.knockbackVelocity;
        long ageMs = System.currentTimeMillis() - data.knockbackTimeMs;
        if (kv != null && ageMs >= 0L && ageMs < 2000L) {
            double expH = Math.hypot(kv.getX(), kv.getZ());
            double expY = kv.getY();
            if (expH > 0.12) {
                double len = Math.max(0.000001, expH);
                double dirX = kv.getX() / len;
                double dirZ = kv.getZ() / len;
                double dot = dx * dirX + dz * dirZ;
                double threshold = Math.min(0.25, expH * 0.5);
                if (threshold < 0.10) {
                    threshold = 0.10;
                }
                detailKind = String.format("kier=%.3f min=%.3f", dot, threshold);
                // Rzut ponizej progu przy realnym wektorze to cancel.
                // Celowy ruch w bok nie zeruje rzutu (ortogonalny),
                // tylko ruch dokladnie przeciwny, ktorego sprint
                // nie jest w stanie dac (odrzut silniejszy od sprintu).
                if (dot < threshold) {
                    cancelled = true;
                }
            } else if (expY > 0.25) {
                // Odrzut glownie w gore: ofiara musi zyskac wysokosc.
                detailKind = String.format("pion=%.3f exp=%.3f", dy, expY);
                if (dy < 0.05 && !player.isOnGround()) {
                    cancelled = true;
                } else if (dy < -0.15) {
                    cancelled = true;
                }
            } else {
                double minRatio = cfgDouble("min-ratio", 0.2);
                double limit = Math.max(0.10, 0.12 * minRatio * 5.0);
                detailKind = String.format("kb=%.3f", moved);
                if (moved < limit) {
                    cancelled = true;
                }
            }
        } else {
            double minRatio = cfgDouble("min-ratio", 0.2);
            double limit = Math.max(0.10, 0.12 * minRatio * 5.0);
            detailKind = String.format("kb=%.3f", moved);
            if (moved < limit) {
                cancelled = true;
            }
        }
        data.knockbackVelocity = null;
        if (cancelled) {
            data.velocityStreak++;
            if (data.velocityStreak >= 2) {
                flag(player, data, String.format("%s x%d",
                        detailKind, data.velocityStreak));
                data.velocityStreak = 0;
            }
        } else {
            data.velocityStreak = 0;
        }
    }

    /** Czy blok na drodze odrzutu (metr od ofiary) jest solidny. */
    private boolean hasWallBehind(Player player, PlayerData data) {
        try {
            Location v = player.getLocation();
            double dx = v.getX() - data.knockbackFrom.getX();
            double dz = v.getZ() - data.knockbackFrom.getZ();
            double len = Math.hypot(dx, dz);
            if (len < 0.2) {
                return false;
            }
            dx /= len;
            dz /= len;
            Location probe = v.clone();
            probe.setX(probe.getX() + dx * 1.0);
            probe.setZ(probe.getZ() + dz * 1.0);
            Material feet = probe.getBlock().getType();
            probe.setY(probe.getY() + 1.0);
            Material head = probe.getBlock().getType();
            return feet.isSolid() || head.isSolid();
        } catch (Exception e) {
            return false;
        }
    }
}
