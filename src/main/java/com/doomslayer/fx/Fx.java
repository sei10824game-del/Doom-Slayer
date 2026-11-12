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
    public static void burst(ServerLevel l, ParticleOptions o, Vec3 p, int n, double spread, double speed) { l.sendParticles(o, p.x, p.y, p.z, n, spread, spread, spread, speed); }
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
