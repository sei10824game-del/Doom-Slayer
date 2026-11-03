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

    /** straight tracer line */
    public static void beam(ServerLevel l, Vec3 a, Vec3 b, int rgb, float size, double step) {
        Vec3 d = b.subtract(a); double len = d.length(); if (len < 0.01) return;
        Vec3 u = d.scale(1 / len); ParticleOptions o = dust(rgb, size);
        for (double s = 0; s <= len; s += step) { Vec3 q = a.add(u.scale(s)); l.sendParticles(o, q.x, q.y, q.z, 1, 0, 0, 0, 0); }
    }
    /**
     * Swing arc (the "slash"): points on an arc of radius r around center, in a plane tilted by tiltDeg around the facing axis,
     * sweeping from fromDeg to toDeg (0 = straight ahead, + = to the viewer's left). Call each tick with a growing 'toDeg' to animate the trail.
     */
    public static void arc(ServerLevel l, Vec3 center, float yawDeg, float r, float fromDeg, float toDeg, float tiltDeg, int rgb, float size, int pts) {
        double yaw = Math.toRadians(yawDeg), tilt = Math.toRadians(tiltDeg);
        ParticleOptions o = dust(rgb, size);
        for (int i = 0; i <= pts; i++) {
            double a = Math.toRadians(fromDeg + (toDeg - fromDeg) * i / pts);
            double x = -Math.sin(a) * r, fwd = Math.cos(a) * r; // local: x=right-left, fwd
            double y = x * Math.sin(tilt); x *= Math.cos(tilt);
            // rotate by yaw (MC yaw 0 = +z)
            double wx = x * Math.cos(yaw) - fwd * Math.sin(yaw), wz = x * Math.sin(yaw) + fwd * Math.cos(yaw);
            l.sendParticles(o, center.x + wx, center.y + y, center.z + wz, 1, 0, 0, 0, 0);
        }
    }
    /** horizontal expanding ring */
    public static void ring(ServerLevel l, Vec3 c, float r, int rgb, float size, int pts) {
        ParticleOptions o = dust(rgb, size);
        for (int i = 0; i < pts; i++) { double a = Math.PI * 2 * i / pts; l.sendParticles(o, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0); }
    }
    public static void sparks(ServerLevel l, Vec3 p, int n) { burst(l, ParticleTypes.CRIT, p, n, 0.15, 0.25); }
    public static void explosion(ServerLevel l, Vec3 p, float size) {
        burst(l, size > 3 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, p, 1, 0, 0);
        burst(l, ParticleTypes.FLAME, p, (int) (12 * size), size * 0.25, 0.08);
        burst(l, ParticleTypes.LARGE_SMOKE, p, (int) (8 * size), size * 0.25, 0.03);
    }
}
