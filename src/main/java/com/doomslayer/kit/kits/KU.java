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
    /** horizontal sweep strike with one arm (hit at frame h of 0..1) */
    static void swing(Pose p, float t, float h, float wind) {
        p.rArmX = track(t, 0, -0.2f, h * .6f, -2.2f * wind, h, -1.2f, h + .2f, -1.4f, 1, -0.2f);
        p.rArmZ = track(t, 0, 0.1f, h * .6f, 0.6f * wind, h, -0.6f, h + .2f, -0.7f, 1, 0.1f);
        p.rHandX = track(t, 0, 0.3f, h * .6f, 0.7f, h, -0.3f, 1, 0.3f);
        p.bodyY = track(t, 0, 0, h * .6f, 0.5f * wind, h, -0.55f, h + .2f, -0.6f, 1, 0); p.headY = -p.bodyY * 0.6f;
        p.lArmX = track(t, 0, 0, h * .6f, -0.8f, 1, 0);
        p.lLegX = track(t, 0, 0, h, -0.4f, 1, 0); p.rLegX = track(t, 0, 0, h, 0.3f, 1, 0);
        p.rootZ = -1.6f * bump(win(t, h * .5f, h + .3f));
    }
    static void stab(Pose p, float t, float h) {
        p.rArmX = track(t, 0, -0.2f, h * .7f, -0.5f, h, -1.55f, h + .2f, -1.5f, 1, -0.2f);
        p.rArmY = track(t, 0, 0, h * .7f, 0.5f, h, 0, 1, 0);
        p.bodyY = track(t, 0, 0, h * .7f, 0.6f, h, -0.3f, 1, 0); p.headY = -p.bodyY * 0.5f;
        p.bodyX = track(t, 0, 0, h * .7f, -0.15f, h, 0.4f, 1, 0);
        p.lLegX = track(t, 0, 0, h, -0.6f, 1, 0); p.rLegX = track(t, 0, 0, h, 0.5f, 1, 0);
        p.rootZ = -2f * bump(win(t, h * .6f, h + .3f));
    }
}
