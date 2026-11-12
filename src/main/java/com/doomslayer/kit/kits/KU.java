package com.doomslayer.kit.kits;

import static com.doomslayer.anim.Keys.*;

import com.doomslayer.anim.Pose;
import com.doomslayer.entity.SlayerEntity;
import com.doomslayer.fx.Combat;
import com.doomslayer.fx.Fx;
import java.util.Random;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Shared helpers for newer kits (Omori, Chara, Dead Space). */
final class KU {
    private KU() {}
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.15, 0.3, 0); }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) {
        return t == null ? e.getViewVector(1f) : Combat.dirTo(mz(e), t, spread, new Random(e.getRandom().nextLong()));
    }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static void flash(SlayerEntity e, int rgb) {
        Fx.burst(L(e), ParticleTypes.FLAME, mz(e), 2, 0.04, 0.02); Fx.dustBurst(L(e), rgb, 1.1f, mz(e), 4, 0.08);
    }
    static void slash(SlayerEntity e, float from, float to, float tilt, int rgb) {
        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.5f, from, to, tilt, rgb, 0.7f, 10);
    }
    static int melee(SlayerEntity e, double range, float dmg, double kb, double up, int rgb) {
        int n = 0;
        for (LivingEntity f : Combat.cone(e, range, 55)) {
            Combat.damage(e, f, dmg); Combat.knock(f, e.position(), kb, up); Fx.dustBurst(L(e), rgb, 1f, mid(f), 6, 0.3); Fx.sparks(L(e), mid(f), 4); n++;
        }
        return n;
    }
    /** open a parry window (ticks); see {@link #defend} */
    static void parryWindow(SlayerEntity e, int ticks) { e.getPersistentData().putLong("ku_parry", e.level().getGameTime() + ticks); }
    /** Parry: inside the window, melee hits are negated (attacker staggered) and vanilla projectiles are batted back at their shooter. */
    static float defend(SlayerEntity e, net.minecraft.world.damagesource.DamageSource src, float amt) {
        if (e.level().getGameTime() >= e.getPersistentData().getLong("ku_parry")) return amt;
        net.minecraft.world.entity.Entity direct = src.getDirectEntity(), att = src.getEntity();
        if (direct instanceof net.minecraft.world.entity.projectile.Projectile pr && !(pr instanceof com.doomslayer.entity.SlayerProjectile)) {
            pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.3)); pr.setOwner(e); pr.hurtMarked = true;
            Fx.sparks(L(e), pr.position(), 10); snd(e, SoundEvents.ANVIL_LAND, 0.7f, 2f); return 0;
        }
        if (att instanceof LivingEntity a && direct == att) {
            Vec3 c = e.position().add(0, 1.2, 0).add(e.getViewVector(1f).scale(0.8));
            Fx.sparks(L(e), c, 16); Fx.ring(L(e), c, 1f, 0xffe090, 1.4f, 14); snd(e, SoundEvents.ANVIL_LAND, 0.9f, 1.9f);
            Combat.knock(a, e.position(), 0.6, 0.1);
            a.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
            a.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 40, 1));
            return 0;
        }
        return amt;
    }
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** two-handed shouldered long gun */
    static void shoulder(Pose p, float k, float pitch, float recoil) {
        float tw = 0.22f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.15f - tw, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.05f - recoil * 0.22f, k); p.lArmY = lerp(p.lArmY, 0.45f - tw, k);
        p.bodyY += tw * k; p.headY -= tw * k; p.bodyX += (recoil * 0.12f - pitch * 0.2f) * k; p.rootZ += recoil * 0.9f * k;
    }
    /** pistol / one-hand-forward with support hand */
    static void pistol2(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.45f, k); p.rArmY = lerp(p.rArmY, -0.1f, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.08f - recoil * 0.4f, k); p.lArmY = lerp(p.lArmY, 0.42f, k);
        p.rHandX = lerp(p.rHandX, -recoil * 0.45f, k); p.bodyX -= recoil * 0.06f * k; p.rootZ += recoil * 0.5f * k;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.26f, k); p.lLegZ -= 0.06f * k; p.rLegZ += 0.08f * k; p.rootY -= 0.4f * k;
    }
    static void throwAnim(Pose p, float t) {
        p.rArmX = track(t, 0, -0.2f, .25f, -2.6f, .5f, -0.4f, .7f, -1.4f, 1, -0.2f);
        p.rArmZ = track(t, 0, 0.1f, .25f, 0.5f, .5f, -0.2f, 1, 0.1f);
        p.lArmX = track(t, 0, 0, .25f, -1f, .5f, 0.3f, 1, 0);
        p.bodyY = track(t, 0, 0, .25f, 0.5f, .5f, -0.5f, .8f, -0.2f, 1, 0); p.headY = -p.bodyY * 0.5f;
        p.bodyX = track(t, 0, 0, .25f, -0.2f, .5f, 0.35f, 1, 0);
        p.rLegX = track(t, 0, 0, .25f, 0.3f, .5f, -0.4f, 1, 0); p.lLegX = track(t, 0, 0, .25f, -0.3f, .5f, 0.4f, 1, 0);
    }
    /** 4-phase value curve: base -> coil (decelerating wind-up) -> strike (accelerating snap) -> follow-through (decelerating overshoot) -> base */
    static float kf(float u, float base, float coil, float strike, float follow) {
        if (u < .36f) return lerp(base, coil, outCubic(win(u, 0, .36f)));
        if (u < .47f) { float k = win(u, .36f, .47f); return lerp(coil, strike, k * k); }
        if (u < .70f) return lerp(strike, follow, outCubic(win(u, .47f, .70f)));
        return lerp(follow, base, smooth(win(u, .70f, 1f)));
    }
    /** whole-body strike. u 0..1 with the impact at ~0.47. kind: 0 forehand, 1 backhand, 2 overhead chop, 3 thrust */
    static void strikePose(Pose p, float u, int kind, float power) {
        float k = power;
        switch (kind) {
            case 0, 1 -> {
                float d = kind == 0 ? 1 : -1;
                p.rArmX = kf(u, -0.2f, -2.1f * k, -1.25f, -0.95f); p.rArmZ = kf(u, 0.1f, 1.05f * d * k, -0.95f * d, -1.15f * d); p.rArmY = kf(u, 0, 0.25f * d, -0.35f * d, -0.3f * d);
                p.rHandX = kf(u, 0.4f, 1.1f, -0.7f, -0.5f); p.rHandZ = kf(u, 0, 0.5f * d, -0.6f * d, -0.4f * d);
                p.bodyY = kf(u, 0, 0.95f * d * k, -1.0f * d, -1.1f * d); p.headY = -0.55f * p.bodyY; p.bodyX = kf(u, 0, -0.18f, 0.35f, 0.3f); p.bodyZ = kf(u, 0, -0.12f * d, 0.1f * d, 0.08f * d);
                p.lArmX = kf(u, 0, -0.6f, 0.35f, 0.2f); p.lArmZ = kf(u, 0, -0.55f * d, 0.45f * d, 0.3f * d);
                p.lLegX = kf(u, 0, 0.3f, -0.8f, -0.6f); p.rLegX = kf(u, 0, -0.35f, 0.55f, 0.45f); p.lLegZ += 0.1f * (kf(u, 0, 0, 1, 1)); p.rLegZ -= 0.1f * (kf(u, 0, 0, 1, 1));
                p.rootZ += kf(u, 0, 0.6f, -1.9f, -2.1f); p.rootY += kf(u, 0, -0.6f, -1.3f, -1.0f); p.rootRoll += kf(u, 0, -0.08f * d, 0.1f * d, 0.06f * d);
            }
            case 2 -> {
                p.rArmX = kf(u, -0.2f, -3.05f * k, -0.45f, -0.7f); p.rArmZ = kf(u, 0.1f, 0.25f, -0.1f, 0.1f); p.rHandX = kf(u, 0.4f, 1.3f, -0.5f, -0.2f);
                p.lArmX = kf(u, 0, -2.4f * k, -0.35f, -0.5f); p.lArmZ = kf(u, 0, -0.3f, 0.1f, 0);
                p.bodyX = kf(u, 0, -0.45f * k, 0.8f, 0.65f); p.headX = kf(u, 0, -0.3f, 0.2f, 0.25f); p.bodyY = kf(u, 0, 0.2f, -0.15f, -0.1f);
                p.lLegX = kf(u, 0, 0.2f, -0.9f, -0.7f); p.rLegX = kf(u, 0, -0.4f, 0.7f, 0.55f);
                p.rootY += kf(u, 0, 0.5f, -2.2f, -1.6f); p.rootZ += kf(u, 0, 0.8f, -1.6f, -1.8f);
            }
            case 4, 5 -> { // jab / cross: 4 = right fist, 5 = left fist
                boolean r = kind == 4; float d = r ? 1 : -1;
                float arm = r ? 1 : -1; // r uses rArm, l uses lArm
                float ax = kf(u, -0.3f, -0.7f, -1.58f, -1.4f), az = kf(u, 0.1f, 0.35f * d, 0.05f * d, 0.1f * d);
                if (r) { p.rArmX = ax; p.rArmZ = az; p.rArmY = kf(u, 0, 0.45f, -0.3f, -0.2f); p.lArmX = kf(u, 0, -1.2f, -1.25f, -1.1f); p.lArmZ = kf(u, 0, 0.4f, 0.35f, 0.3f); }
                else { p.lArmX = ax; p.lArmZ = az; p.lArmY = kf(u, 0, -0.45f, 0.3f, 0.2f); p.rArmX = kf(u, 0, -1.2f, -1.25f, -1.1f); p.rArmZ = kf(u, 0, -0.4f, -0.35f, -0.3f); }
                p.bodyY = kf(u, 0, 0.7f * d, -0.75f * d, -0.65f * d); p.headY = -0.4f * p.bodyY; p.bodyX = kf(u, 0, -0.1f, 0.3f, 0.25f);
                p.lLegX = kf(u, 0, 0.25f, -0.7f, -0.5f); p.rLegX = kf(u, 0, -0.3f, 0.5f, 0.4f); p.rootY += kf(u, 0, -0.5f, -1.1f, -0.9f); p.rootZ += kf(u, 0, 0.5f, -1.6f, -1.8f);
            }
            default -> { // thrust
                p.rArmX = kf(u, -0.2f, -0.55f, -1.58f, -1.5f); p.rArmY = kf(u, 0, 0.6f * k, -0.05f, 0); p.rArmZ = kf(u, 0.1f, 0.5f, 0, 0.05f); p.rHandX = kf(u, 0.4f, 0.7f, 0.0f, 0.1f);
                p.lArmX = kf(u, 0, -0.9f, 0.4f, 0.3f); p.lArmZ = kf(u, 0, -0.5f, 0.5f, 0.4f);
                p.bodyY = kf(u, 0, 0.7f * k, -0.45f, -0.4f); p.headY = -0.5f * p.bodyY; p.bodyX = kf(u, 0, -0.2f, 0.5f, 0.45f);
                p.lLegX = kf(u, 0, 0.4f, -1.0f, -0.85f); p.rLegX = kf(u, 0, -0.5f, 0.7f, 0.6f); p.rootY += kf(u, 0, -0.5f, -1.6f, -1.3f); p.rootZ += kf(u, 0, 1.0f, -2.6f, -2.8f);
            }
        }
    }
    /** one swing with impact at frame h (0..1 of the attack) */
    static void swing(Pose p, float t, float h, float wind) { strikePose(p, remap(t, h), 0, Math.min(1.2f, wind)); }
    static void stab(Pose p, float t, float h) { strikePose(p, remap(t, h), 3, 1f); }
    static float remap(float t, float h) { return t < h ? 0.47f * t / Math.max(h, 0.01f) : 0.47f + 0.53f * (t - h) / Math.max(1 - h, 0.01f); }
    /** attack window for several hits: each hit gets its own wind-up/strike/recover (alternating kinds) */
    static com.doomslayer.anim.Anim comboAnim(int[] hits, int dur, int[] kinds) {
        return (p, t, a) -> {
            float tick = t * dur;
            for (int i = 0; i < hits.length; i++) {
                float wind = Math.min(5.5f, hits[i] - (i > 0 ? hits[i - 1] + 6f : 0f)); wind = Math.max(wind, 1.5f);
                float hs = hits[i] - wind, he = hits[i] + 6f;
                if (tick >= hs && tick <= he) { float u = tick < hits[i] ? 0.47f * (tick - hs) / wind : 0.47f + 0.53f * (tick - hits[i]) / 6f; strikePose(p, u, kinds[i % kinds.length], 1f); return; }
            }
            stance(p, 0.7f); p.rArmX = -0.9f; p.rArmZ = 0.3f; p.lArmX = -0.8f; p.rHandX = 0.6f; // between hits: guard up, sword raised
        };
    }

    /** gun animation: raise -> hold -> per-shot recoil (+ muzzle climb) -> lower. style 0 auto, 1 pistol, 2 launcher, 3 precision, 4 shotgun */
    static com.doomslayer.anim.Anim gunAnim(int[] shots, int dur, int style, boolean sustained) {
        final float[] S = {0.75f, 1.5f, 2.6f, 1.9f, 2.3f};
        return (p, t, a) -> {
            float tick = t * dur; int first = shots[0], last = shots[shots.length - 1];
            float k = smooth(win(tick, 0, Math.max(3, first - 2))) * (1 - smooth(win(tick, last + 5, Math.max(last + 6, dur))));
            float r = 0, climb = 0;
            if (sustained) { float on = win(tick, first, first + 2) * (1 - win(tick, last, last + 2)); r = 0.4f * on * (1 + 0.3f * (float) Math.sin(tick * 3.1f)); climb = 0.1f * on; }
            else {
                int passed = 0;
                for (int sh : shots) { float x = tick - sh; if (x < 0) continue; passed++; r += x < 1 ? x : (float) Math.exp(-(x - 1) / 2.4); }
                climb = Math.min(0.25f, passed * 0.025f) * (1 - win(tick, last + 1, last + 9));
            }
            r *= S[style];
            float after = bump(win(tick, last + 3, last + 11)) * k;
            switch (style) {
                case 1 -> { pistol2(p, k, climb * 1.5f, r); stance(p, k); }
                case 2 -> { shoulder(p, k, 0.2f + climb, r); stance(p, k); p.lLegZ -= 0.25f * k; p.rLegZ += 0.25f * k; p.rootY -= 0.8f * k; p.lArmX += 0.45f * after; }
                case 3 -> { shoulder(p, k, 0.03f + climb * 0.3f, r); stance(p, k); p.headZ += 0.16f * k; p.bodyX += 0.1f * k; p.rArmZ += 0.18f * after; p.rHandZ += 0.6f * after; }
                case 4 -> { shoulder(p, k, 0.04f + climb, r); stance(p, k); p.lArmX += 0.4f * after; p.rootZ += 0.8f * r * 0.3f; }
                default -> { shoulder(p, k, climb, r); stance(p, k); if (r > 0.1f) p.bodyZ += 0.02f * (float) Math.sin(tick * 6f) * k; }
            }
        };
    }
}
