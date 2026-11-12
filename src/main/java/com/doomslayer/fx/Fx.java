package com.doomslayer.fx;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Server-side visual helpers (all go through ServerLevel#sendParticles so clients see them). */
public final class Fx {
    private Fx() {}
    public static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
    }
    public static void sound(ServerLevel l, Vec3 p, SoundEvent s, float vol, float pitch) { l.playSound(null, p.x, p.y, p.z, s, SoundSource.HOSTILE, vol, pitch); }
    /** Particle bursts are re-drawn by the custom renderer (glow puffs, smoke, spark streaks); only exotic particle types stay vanilla. */
    public static void burst(ServerLevel l, ParticleOptions o, Vec3 p, int n, double spread, double speed) {
        if (o == ParticleTypes.EXPLOSION || o == ParticleTypes.EXPLOSION_EMITTER) { explosion(l, p, o == ParticleTypes.EXPLOSION ? 1.5f : 3f); return; }
        int[] m = map(o);
        if (m == null) { l.sendParticles(o, p.x, p.y, p.z, n, spread, spread, spread, speed); return; }
        int kind = m[0], rgb = m[1];
        float size = kind == 1 ? 0.55f : kind == 2 ? 0.09f : 0.3f; int life = kind == 1 ? 24 : kind == 2 ? 9 : 11;
        if (o instanceof DustParticleOptions d) { rgb = ((int) (d.getColor().x * 255) << 16) | ((int) (d.getColor().y * 255) << 8) | (int) (d.getColor().z * 255); size = 0.1f + d.getScale() * 0.13f; }
        int cnt = Math.max(1, Math.min(n, 28)); if (n > 28) size *= (float) Math.sqrt(n / 28.0);
        com.doomslayer.entity.ProjStyle st = kind == 1 ? com.doomslayer.entity.ProjStyle.VIS_PUFF_SMOKE : kind == 2 ? com.doomslayer.entity.ProjStyle.VIS_PUFF_SPARK : com.doomslayer.entity.ProjStyle.VIS_PUFF_GLOW;
        com.doomslayer.entity.SlayerProjectile.visual(l, st, p, Vec3.ZERO, rgb, size, life, new Vec3(speed, cnt, 0), (float) Math.max(spread, 0.05));
    }
    /** {kind (0 glow, 1 smoke, 2 spark), rgb} for the particle types we re-render; null = leave to vanilla */
    private static int[] map(ParticleOptions o) {
        if (o instanceof DustParticleOptions) return new int[] {0, 0xffffff};
        if (o == ParticleTypes.FLAME || o == ParticleTypes.SMALL_FLAME) return new int[] {0, 0xff8a20};
        if (o == ParticleTypes.SOUL_FIRE_FLAME) return new int[] {0, 0x40c8ff};
        if (o == ParticleTypes.LAVA) return new int[] {0, 0xff6a10};
        if (o == ParticleTypes.END_ROD || o == ParticleTypes.GLOW) return new int[] {0, 0xf0f4ff};
        if (o == ParticleTypes.ELECTRIC_SPARK) return new int[] {2, 0xa0e0ff};
        if (o == ParticleTypes.CRIT) return new int[] {2, 0xffe8a0};
        if (o == ParticleTypes.DAMAGE_INDICATOR) return new int[] {2, 0xc01818};
        if (o == ParticleTypes.HAPPY_VILLAGER) return new int[] {0, 0x60ff60};
        if (o == ParticleTypes.ENCHANT || o == ParticleTypes.WITCH || o == ParticleTypes.DRAGON_BREATH) return new int[] {0, 0xb070ff};
        if (o == ParticleTypes.PORTAL || o == ParticleTypes.REVERSE_PORTAL) return new int[] {0, 0xa050ff};
        if (o == ParticleTypes.HEART) return new int[] {0, 0xff6080};
        if (o == ParticleTypes.SNOWFLAKE) return new int[] {0, 0xe0f4ff};
        if (o == ParticleTypes.ITEM_SLIME) return new int[] {0, 0x70e050};
        if (o == ParticleTypes.SOUL || o == ParticleTypes.SCULK_SOUL) return new int[] {0, 0x80d8ff};
        if (o == ParticleTypes.SMOKE) return new int[] {1, 0x555555};
        if (o == ParticleTypes.LARGE_SMOKE || o == ParticleTypes.CAMPFIRE_COSY_SMOKE) return new int[] {1, 0x444444};
        if (o == ParticleTypes.CLOUD || o == ParticleTypes.POOF) return new int[] {1, 0xdddddd};
        if (o == ParticleTypes.SQUID_INK) return new int[] {1, 0x181818};
        return null;
    }
    public static void dustBurst(ServerLevel l, int rgb, float size, Vec3 p, int n, double spread) { burst(l, dust(rgb, size), p, n, spread, 0); }

    /** straight energy beam segment (custom-rendered, fades in ~3 ticks) */
    public static void beam(ServerLevel l, Vec3 a, Vec3 b, int rgb, float size, double step) {
        if (a.distanceToSqr(b) < 1e-4 || a.distanceToSqr(b) > 90 * 90) return;
        com.doomslayer.entity.SlayerProjectile.visual(l, com.doomslayer.entity.ProjStyle.VIS_BEAM, a, Vec3.ZERO, rgb, 0.04f + size * 0.1f, 3, b);
    }
    /**
     * Swing arc (the "slash"): a glowing crescent on an arc of radius r around center, plane tilted by tiltDeg around the facing axis,
     * sweeping from fromDeg to toDeg (0 = straight ahead, + = to the viewer's left). Custom-rendered sweep that fades out.
     */
    public static void arc(ServerLevel l, Vec3 center, float yawDeg, float r, float fromDeg, float toDeg, float tiltDeg, int rgb, float size, int pts) {
        com.doomslayer.entity.SlayerProjectile.visual(l, com.doomslayer.entity.ProjStyle.VIS_SLASH, center, new Vec3(tiltDeg, 0, 0), rgb, Math.max(0.3f, size) * 0.5f + r * 0.0f, 6, new Vec3(yawDeg, fromDeg, toDeg), r);
    }
    /** muzzle / impact flash */
    public static void flash(ServerLevel l, Vec3 p, int rgb, float size) {
        com.doomslayer.entity.SlayerProjectile.visual(l, com.doomslayer.entity.ProjStyle.VIS_FLASH, p, Vec3.ZERO, rgb, size, 4, null);
    }
    /** expanding shock ring on the ground (custom-rendered) */
    public static void ring(ServerLevel l, Vec3 c, float r, int rgb, float size, int pts) {
        com.doomslayer.entity.SlayerProjectile.visual(l, com.doomslayer.entity.ProjStyle.VIS_RING, c, Vec3.ZERO, rgb, r, 10, null);
    }
    public static void sparks(ServerLevel l, Vec3 p, int n) { flash(l, p, 0xffe8a0, 0.3f + Math.min(n, 12) * 0.03f); }
    public static void explosion(ServerLevel l, Vec3 p, float size) {
        com.doomslayer.entity.SlayerProjectile.visual(l, com.doomslayer.entity.ProjStyle.VIS_BLAST, p, Vec3.ZERO, 0xff8020, Math.max(1f, size), 12, null);
    }
}
