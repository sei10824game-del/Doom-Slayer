package com.doomslayer.kit.kits;

import static com.doomslayer.anim.Keys.*;

import com.doomslayer.anim.Pose;
import com.doomslayer.client.Mat;
import com.doomslayer.client.Rig;
import com.doomslayer.entity.ProjStyle;
import com.doomslayer.entity.SlayerEntity;
import com.doomslayer.fx.Combat;
import com.doomslayer.fx.Fx;
import com.doomslayer.kit.AttackDef;
import com.doomslayer.kit.SlayerKit;
import java.util.List;
import java.util.Random;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Clone Cop, Entropy Zero 2: the Combine's cloned Civil Protection enforcer. Grey/teal CP armour,
 * full EZ2 arsenal, plus his Xen-touched tricks: Mirror Strike afterimage dash, Xen resonance pulse
 * and the shared-damage "Link".
 */
public class EntropyZero2CloneCop extends SlayerKit {
    static final int TEAL = 0x2ee6d0, TEAL_D = 0x138a80, XEN = 0x9a5cff, PULSE = 0x7fe8ff, BLOOD = 0x8a0303;

    static final ProjStyle SMG_GREN = ProjStyle.make().color(0x5a6060).size(0.25f).life(60).explode(3f).gravity(0.045).trail(ParticleTypes.SMOKE);
    static final ProjStyle AR2_BALL = ProjStyle.make().color(0xd8fbff).size(0.9f).life(90).pierce(6).bounce().trail(ParticleTypes.END_ROD)
            .onTick(p -> {
                if (p.age2 % 4 != 0 || !(p.getOwner() instanceof SlayerEntity o)) return;
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, PULSE, 1.4f, p.position(), 4, 0.35);
                for (LivingEntity t : Combat.around(o, p.position(), 1.6)) {
                    Combat.damage(o, t, 7); Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, t.position().add(0, 1, 0), 10, 0.3, 0.2);
                }
            })
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.ring(l, pos, 1.6f, PULSE, 1.4f, 24); Fx.burst(l, ParticleTypes.FLASH, pos, 1, 0, 0);
                Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1.2f, 1.6f);
            });
    static final ProjStyle BOLT = ProjStyle.make().color(0xffb040).size(0.18f).life(60).gravity(0.008).pierce(1).trail(ParticleTypes.CRIT);
    static final ProjStyle RPG = ProjStyle.make().color(0xff8030).size(0.45f).life(110).explode(3.5f).homing(0.12f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle FRAG = ProjStyle.make().color(0x4a5048).size(0.26f).life(36).explode(3.4f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle XEN_GREN = ProjStyle.make().color(XEN).size(0.3f).life(30).gravity(0.05).bounce().trail(ParticleTypes.PORTAL)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                if (!(p.getOwner() instanceof SlayerEntity o)) return;
                // implosion: drag everything in, crush, then pop
                for (LivingEntity t : Combat.around(o, pos, 6)) {
                    Vec3 d = pos.subtract(t.position()).normalize().scale(0.9);
                    t.setDeltaMovement(d.x, 0.35, d.z); t.hurtMarked = true;
                    Combat.damage(o, t, 11);
                }
                for (int i = 0; i < 4; i++) Fx.ring(l, pos.add(0, 0.3 + i * 0.4, 0), 4.5f - i, i % 2 == 0 ? XEN : TEAL, 1.6f, 30);
                Fx.burst(l, ParticleTypes.REVERSE_PORTAL, pos, 80, 1.5, 0.4); Fx.burst(l, ParticleTypes.PORTAL, pos, 60, 2.5, 1.2);
                Fx.sound(l, pos, SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.5f); Fx.sound(l, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 1.3f, 0.7f);
            });
    static final ProjStyle MANHACK = ProjStyle.make().color(0x70787a).size(0.4f).life(120).homing(0.35f).pierce(8).trail(ParticleTypes.CRIT)
            .onTick(p -> {
                if (p.age2 % 3 != 0 || !(p.getOwner() instanceof SlayerEntity o)) return;
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, 0xff3020, 0.7f, p.position(), 1, 0.05);
                if (p.age2 % 6 == 0) Fx.sound(l, p.position(), SoundEvents.BEE_LOOP_AGGRESSIVE, 0.6f, 2f);
                for (LivingEntity t : Combat.around(o, p.position(), 1.4)) {
                    Combat.damage(o, t, 2.5f); Fx.sparks(l, t.position().add(0, 1, 0), 4);
                    Fx.sound(l, p.position(), SoundEvents.SHEEP_SHEAR, 0.7f, 1.6f);
                }
            });

    public EntropyZero2CloneCop() {
        super("entropy_zero2_clone_cop", "Clone Cop", "クローンコップ", "Entropy Zero 2");
        eggPrimary = 0x5b6166; eggSecondary = TEAL;
        maxHealth = 90; speed = 0.33; armor = 12; knockbackRes = 0.4;
        scale = 1.0f; preferredRange = 9f; dmgMult = 1.05f;
        idleWeaponR = "smg";
    }

    // ================= helpers (server) =================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.1, 0.32, 0); }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        return Combat.dirTo(mz(e), t, spread, new Random(e.getRandom().nextLong()));
    }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static Vec3 endOf(SlayerEntity e, LivingEntity t, Vec3 dir, double r) { return t != null ? mid(t) : mz(e).add(dir.scale(r)); }
    static void flash(SlayerEntity e, int rgb) {
        Vec3 m = mz(e);
        Fx.burst(L(e), ParticleTypes.FLAME, m, 2, 0.04, 0.02); Fx.dustBurst(L(e), rgb, 1.1f, m, 4, 0.08);
    }
    static void brass(SlayerEntity e) { Fx.burst(L(e), ParticleTypes.SMOKE, Combat.muzzle(e, 0.4, 0.5, 0.1), 2, 0.03, 0.03); }
    static void shock(SlayerEntity e, LivingEntity t, int n) {
        if (t == null) return;
        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(t), n, 0.3, 0.3); Fx.dustBurst(L(e), TEAL, 1f, mid(t), n / 2, 0.3);
    }
    /** stunned target: frozen briefly */
    static void stun(LivingEntity t, int ticks) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, false));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1, false, false));
    }
    static void hold(SlayerEntity e, LivingEntity t, double dist, double lift) {
        if (t == null || !Combat.isFoe(e, t)) return;
        Vec3 f = Vec3.directionFromRotation(0, e.getYRot());
        Vec3 p = e.position().add(f.scale(dist + t.getBbWidth() * 0.5)).add(0, lift, 0);
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true;
    }
    /** teal dust silhouette left behind by Mirror Strike */
    static void ghost(ServerLevel l, Vec3 at, float yaw) {
        Vec3 side = Vec3.directionFromRotation(0, yaw + 90).scale(0.28);
        for (int i = 0; i < 9; i++) {
            double y = i * 0.2;
            Fx.dustBurst(l, i > 6 ? PULSE : TEAL, 1.1f, at.add(0, y + 0.1, 0), 1, 0.02);
            if (i < 4) { Fx.dustBurst(l, TEAL_D, 1f, at.add(side).add(0, y, 0), 1, 0.02); Fx.dustBurst(l, TEAL_D, 1f, at.subtract(side).add(0, y, 0), 1, 0.02); }
            if (i >= 4 && i < 7) { Fx.dustBurst(l, TEAL, 0.9f, at.add(side.scale(1.4)).add(0, y, 0), 1, 0.02); Fx.dustBurst(l, TEAL, 0.9f, at.subtract(side.scale(1.4)).add(0, y, 0), 1, 0.02); }
        }
        Fx.burst(l, ParticleTypes.REVERSE_PORTAL, at.add(0, 1, 0), 6, 0.2, 0.02);
    }
    static void tether(ServerLevel l, Vec3 a, Vec3 b) {
        Vec3 prev = a;
        for (int i = 1; i <= 6; i++) {
            Vec3 q = a.lerp(b, i / 6.0);
            if (i < 6) q = q.add(0, Math.sin(i * 1.3 + l.getGameTime() * 0.6) * 0.18, 0);
            Fx.beam(l, prev, q, i % 2 == 0 ? TEAL : XEN, 0.6f, 0.3); prev = q;
        }
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** shouldered long gun: bladed stance, left hand on foregrip */
    static void rifle(Pose p, float k, float pitch, float recoil) {
        float tw = 0.22f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.15f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.05f - recoil * 0.22f, k); p.lArmY = lerp(p.lArmY, 0.45f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * k; p.bodyX += (recoil * 0.12f - pitch * 0.2f) * k; p.headX -= recoil * 0.08f * k;
        p.rootZ += recoil * 0.9f * k;
    }
    /** compact weapon (SMG / pistol) held out in a Weaver grip */
    static void weaver(Pose p, float k, float recoil) {
        p.rArmX = lerp(p.rArmX, -HALF - recoil * 0.4f, k); p.rArmY = lerp(p.rArmY, -0.1f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + 0.1f - recoil * 0.3f, k); p.lArmY = lerp(p.lArmY, 0.55f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX -= recoil * 0.35f * k; p.bodyY -= 0.1f * k; p.headY += 0.1f * k; p.rootZ += recoil * 0.5f * k;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.32f, k); p.rLegX = lerp(p.rLegX, 0.28f, k);
        p.lLegZ -= 0.06f * k; p.rLegZ += 0.08f * k; p.rootY -= 0.4f * k;
    }
    static void wide(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.2f, k); p.rLegX = lerp(p.rLegX, 0.15f, k);
        p.lLegZ -= 0.3f * k; p.rLegZ += 0.3f * k; p.rootY -= 1.2f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // SMG at low ready, muzzle down, left hand under the mag; slight cop "scan" with the head
        p.rArmX = -0.6f + p.rArmX * 0.25f; p.rArmY = -0.2f;
        p.lArmX = -0.8f + p.lArmX * 0.25f; p.lArmY = 0.6f; p.lArmZ = 0.05f;
        p.rHandX = -0.3f; p.headY += 0.12f * (float) Math.sin(age * 0.03f) * (1 - m);
    }

    /** suit: slow regen + armour hardening when hurt (no damage hook -> Resistance) */
    @Override
    public void passive(SlayerEntity e) {
        if (e.tickCount % 40 == 0 && e.getHealth() < e.getMaxHealth()) e.heal(1);
        if (e.tickCount % 20 == 0 && e.getHealth() < e.getMaxHealth() * 0.35f) {
            e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1, false, false));
            Fx.dustBurst(L(e), TEAL, 0.8f, e.position().add(0, 1.2, 0), 6, 0.4);
        }
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        melee(out); guns(out); heavy(out); throwables(out); xen(out);
    }

    private void melee(List<AttackDef> out) {
        // ---------- Stunstick combo: backhand -> forehand, crackling ----------
        out.add(AttackDef.of("stunstick_combo").range(0, 3.3f).cd(20).weight(3).dur(20).weapon("stunstick")
                .tick((e, t, n) -> {
                    if (n == 1) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.8f);
                    if (n == 6 || n == 13) {
                        float yawFrom = n == 6 ? 70 : -70, yawTo = -yawFrom;
                        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.5f, yawFrom, yawTo, n == 6 ? 20 : -15, TEAL, 0.9f, 12);
                        for (LivingEntity f : Combat.cone(e, 3.4, 60)) {
                            Combat.damage(e, f, n == 6 ? 5 : 7); shock(e, f, 8); stun(f, 12);
                            Combat.knock(f, e.position(), n == 6 ? 0.3 : 0.7, 0.2);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1, 1.2f); snd(e, SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.6f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    // wind across the body (backhand), rip through, then reverse forehand
                    p.rArmX = track(t, 0, -0.8f, .2f, -1.6f, .32f, -1.3f, .5f, -2.0f, .68f, -0.9f, 1, -0.6f);
                    p.rArmY = trackLin(t, 0, 0, .2f, 0.9f, .32f, -0.7f, .5f, -0.9f, .68f, 0.6f, .85f, 0.4f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .2f, -0.4f, .32f, 0.3f, .5f, 0.5f, .68f, -0.2f, 1, 0);
                    p.rHandX = track(t, 0, 0, .5f, 0.6f, .68f, -0.4f, 1, 0);
                    p.lArmX = track(t, 0, -0.4f, .3f, -0.9f, .7f, -0.5f, 1, -0.3f); p.lArmZ = -0.25f;
                    p.bodyY = trackLin(t, 0, 0, .2f, 0.5f, .32f, -0.45f, .5f, -0.55f, .68f, 0.45f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyX = 0.15f * bump(win(t, .2f, .8f));
                    p.lLegX = track(t, 0, 0, .25f, -0.45f, .75f, -0.4f, 1, 0); p.rLegX = track(t, 0, 0, .55f, 0.4f, 1, 0);
                    p.rootZ = -1.6f * bump(win(t, .25f, .85f)); p.rootY = -0.5f * bump(win(t, .2f, .9f));
                }));

        // ---------- Stunstick overhead: charge high, slam with discharge ----------
        out.add(AttackDef.of("stunstick_overhead").range(0, 3.5f).cd(60).weight(2).dur(26).weapon("stunstick")
                .tick((e, t, n) -> {
                    if (n < 12 && n % 3 == 0) { snd(e, SoundEvents.BEACON_AMBIENT, 0.8f, 1.2f + n * 0.06f); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, e.position().add(0, 2.6, 0), 4, 0.2, 0.1); }
                    if (n == 14) {
                        Vec3 c = e.position().add(Vec3.directionFromRotation(0, e.getYRot()).scale(1.6));
                        for (LivingEntity f : Combat.around(e, c, 2.4)) { Combat.damage(e, f, 13); shock(e, f, 14); stun(f, 30); Combat.knock(f, c, 0.6, 0.5); }
                        Fx.ring(L(e), c.add(0, 0.1, 0), 2.2f, TEAL, 1.4f, 26); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, c, 40, 1, 0.5);
                        snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9f, 1.8f); snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    float crack = 0.04f * (float) Math.sin(a * 6) * win(t, .1f, .45f) * (1 - win(t, .45f, .5f));
                    p.rArmX = trackLin(t, 0, -0.6f, .4f, -3.0f, .48f, -3.05f, .56f, -0.4f, .75f, -0.5f, 1, -0.6f) + crack;
                    p.lArmX = track(t, 0, -0.4f, .4f, -2.7f, .56f, -0.6f, 1, -0.3f); p.lArmY = track(t, 0, 0, .4f, -0.4f, .56f, 0.3f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .4f, 0.15f, .56f, -0.1f, 1, 0); p.rHandX = track(t, 0, 0, .4f, 0.8f, .56f, -0.3f, 1, 0);
                    p.bodyX = trackLin(t, 0, 0, .4f, -0.35f, .56f, 0.55f, .8f, 0.45f, 1, 0); p.bodyZ = crack;
                    p.headX = track(t, 0, 0, .4f, -0.3f, .56f, 0.3f, 1, 0);
                    p.rootY = track(t, 0, 0, .4f, 0.8f, .52f, 1.4f, .58f, -1.6f, .85f, -1.2f, 1, 0);
                    p.rootZ = track(t, 0, 0, .4f, 0.5f, .58f, -2.2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .4f, 0.2f, .56f, -0.7f, .9f, -0.6f, 1, 0); p.rLegX = track(t, 0, 0, .56f, 0.55f, .9f, 0.5f, 1, 0);
                }));

        // ---------- Boot kick ----------
        out.add(AttackDef.of("kick").range(0, 2.8f).cd(35).weight(2).dur(16).weapon("smg")
                .tick((e, t, n) -> {
                    if (n == 6) {
                        for (LivingEntity f : Combat.cone(e, 3, 45)) { Combat.damage(e, f, 6); Combat.knock(f, e.position(), 1.5, 0.35); Fx.burst(L(e), ParticleTypes.POOF, mid(f), 6, 0.2, 0.05); }
                        snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.8f); snd(e, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.6f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rLegX = trackLin(t, 0, 0, .22f, 0.6f, .38f, -1.7f, .55f, -1.5f, .8f, 0.1f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, 0.2f, .8f, 0.15f, 1, 0);
                    p.bodyX = track(t, 0, 0, .22f, 0.2f, .38f, -0.35f, .6f, -0.3f, 1, 0); p.headX = -p.bodyX * 0.5f;
                    p.rArmX = track(t, 0, -0.6f, .38f, -0.2f, 1, -0.6f); p.rArmZ = track(t, 0, 0, .38f, 0.5f, 1, 0); p.rArmY = -0.2f; p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, -0.8f, .38f, -0.3f, 1, -0.8f); p.lArmZ = track(t, 0, 0, .38f, -0.6f, 1, 0);
                    p.rootZ = track(t, 0, 0, .38f, -1.2f, 1, 0); p.rootY = 0.6f * bump(win(t, .25f, .55f));
                }));

        // ---------- Execution: stun-pin, three cracks to the head, finisher pistol-whip ----------
        out.add(AttackDef.of("execution_stun_takedown").range(0, 3.5f).lowHealthTarget(0.25f).weight(6).cd(80).dur(34).weapon("stunstick")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.5, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.7f); }
                    hold(e, t, 0.9, 0);
                    if (t != null && (n == 10 || n == 15 || n == 20)) {
                        Combat.damage(e, t, Math.min(3, Math.max(0, t.getHealth() - 1))); shock(e, t, 10);
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 0.9f, 1.3f); snd(e, SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, 0.6f, 1.8f);
                    }
                    if (n == 27 && t != null) {
                        Fx.dustBurst(L(e), BLOOD, 1.3f, mid(t), 20, 0.35); shock(e, t, 30);
                        Combat.damage(e, t, t.getHealth() + 100); e.heal(5);
                        snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 1f, 2f); snd(e, SoundEvents.BONE_BLOCK_BREAK, 1.2f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    // left hand grabs collar, right arm hammers three times, then big uppercut
                    p.lArmX = track(t, 0, 0, .15f, -1.6f, .82f, -1.5f, 1, 0); p.lArmY = track(t, 0, 0, .15f, 0.35f, 1, 0);
                    float hammer = 0;
                    for (float h : new float[]{10 / 34f, 15 / 34f, 20 / 34f}) hammer += bump(win(t, h - .1f, h + .05f));
                    p.rArmX = -1.0f - 1.6f * hammer * (t < .65f ? 1 : 0) + track(t, .68f, 0, .76f, 1.1f, .82f, -1.6f, 1, 1.0f);
                    p.rArmZ = track(t, 0, 0, .2f, -0.3f, .65f, -0.3f, .8f, 0.3f, 1, 0);
                    p.rHandX = 0.6f * hammer * (t < .65f ? 1 : 0);
                    p.bodyY = track(t, 0, 0, .2f, -0.3f, .66f, -0.25f, .74f, -0.5f, .82f, 0.55f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.bodyX = 0.2f + 0.12f * hammer - 0.4f * bump(win(t, .74f, .92f));
                    p.headX = 0.25f * win(t, .1f, .25f) * (1 - win(t, .7f, .8f));
                    p.rootZ = track(t, 0, 0, .15f, -2f, .85f, -2f, 1, 0);
                    p.rootY = -0.3f * hammer + 0.8f * bump(win(t, .74f, .9f));
                    p.lLegX = track(t, 0, 0, .15f, -0.45f, .85f, -0.45f, 1, 0); p.rLegX = track(t, 0, 0, .15f, 0.35f, .85f, 0.35f, 1, 0);
                }));
    }

    private void guns(List<AttackDef> out) {
        // ---------- USP pistol: double-tap x2 ----------
        out.add(AttackDef.of("pistol").range(2, 20).cd(24).weight(2).dur(20).weapon("pistol")
                .tick((e, t, n) -> {
                    if (n == 4 || n == 6 || n == 11 || n == 13) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.025), 22, 3.2f, 0, 0xffe0a0); flash(e, 0xffc060); brass(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f);
                    float r = kick(t, 4 / 20f, .1f) + kick(t, 6 / 20f, .1f) + kick(t, 11 / 20f, .1f) + kick(t, 13 / 20f, .12f);
                    weaver(p, k, r); stance(p, k * 0.6f);
                    p.headX += 0.06f * k; p.bodyZ += 0.03f * r;
                }));

        // ---------- Pulse pistol: charge-up overcharged bolt ----------
        out.add(AttackDef.of("pulse_pistol_charged").range(4, 26).cd(70).weight(2).dur(30).weapon("pulse_pistol")
                .tick((e, t, n) -> {
                    if (n < 18) { Fx.dustBurst(L(e), PULSE, 0.7f + n * 0.03f, mz(e), 2, 0.25 - n * 0.012); if (n % 3 == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.5f, 0.9f + n * 0.06f); }
                    if (n == 18) {
                        Vec3 d = aim(e, t, 0.002), m = mz(e);
                        Combat.hitscan(e, m, d, 28, 14f, 2, PULSE);
                        Fx.beam(L(e), m, endOf(e, t, d, 28), 0xffffff, 0.8f, 0.3);
                        Combat.blast(e, endOf(e, t, d, 28), 1.8f, 4f, 0.6f, null);
                        snd(e, SoundEvents.BEACON_DEACTIVATE, 1.2f, 1.8f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.8f, 1.5f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), ch = win(t, .05f, .6f) * (1 - win(t, .6f, .62f)), r = 1.8f * kick(t, 18 / 30f, .25f);
                    weaver(p, k, r); stance(p, k);
                    // pull the pistol in close to the chest while charging, punch it out to fire
                    p.rArmX += 0.45f * ch; p.lArmX += 0.45f * ch; p.rArmY += 0.25f * ch;
                    p.bodyZ += 0.04f * ch * ch * (float) Math.sin(a * 5); p.headX += 0.1f * ch;
                    p.bodyX -= r * 0.1f;
                }));

        // ---------- SMG1 burst ----------
        out.add(AttackDef.of("smg").range(2, 18).cd(26).weight(4).dur(26).weapon("smg")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 20 && n % 2 == 0) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.06), 20, 2.1f, 0, 0xffe080); flash(e, 0xffb040); if (n % 4 == 0) brass(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.6f, 1.7f + e.getRandom().nextFloat() * 0.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .86f), fire = win(t, .14f, .16f) * (1 - win(t, .78f, .8f));
                    float r = 0.35f * Math.abs((float) Math.sin(a * PI * 0.5f)) * fire;
                    weaver(p, k, r); stance(p, k);
                    p.rHandZ += 0.04f * (float) Math.sin(a * 3.7f) * fire; p.bodyZ += 0.02f * (float) Math.sin(a * 2.9f) * fire;
                    p.rArmX -= 0.12f * win(t, .16f, .78f) * fire; // muzzle climb
                }));

        // ---------- SMG1 under-barrel grenade ----------
        out.add(AttackDef.of("smg_grenade").range(6, 22).cd(110).weight(2).dur(26).weapon("smg")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.7f, 1.6f);
                    if (n == 10) {
                        Vec3 m = Combat.muzzle(e, 1.0, 0.32, -0.12);
                        double dist = t != null ? m.distanceTo(mid(t)) : 12;
                        Combat.shoot(e, SMG_GREN, m, aim(e, t, 0.01).add(0, 0.1 + dist * 0.02, 0).normalize(), 1.0 + dist * 0.015, 12f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, m, 5, 0.1, 0.03); snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2f, 0.7f);
                    }
                    if (n == 19) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.7f, 1.6f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 1.8f * kick(t, 10 / 26f, .3f), loft = win(t, .15f, .35f) * (1 - win(t, .6f, .8f));
                    rifle(p, k, 0.3f * loft, r); stance(p, k);
                    p.bodyX -= 0.12f * loft; p.headX -= 0.15f * loft; p.rootZ += r * 0.6f;
                    float reload = bump(win(t, .62f, .82f)); p.lArmX += 0.9f * reload * k; p.lArmZ -= 0.3f * reload; p.headX += 0.2f * reload;
                }));

        // ---------- AR2 pulse rifle ----------
        out.add(AttackDef.of("ar2").range(3, 26).cd(28).weight(4).dur(26).weapon("ar2")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 20 && n % 2 == 0) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.035), 28, 2.8f, 0, PULSE);
                        Fx.dustBurst(L(e), PULSE, 1f, mz(e), 3, 0.06);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST_FAR, 0.9f, 1.4f); if (n % 4 == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.25f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .14f, .16f) * (1 - win(t, .78f, .8f));
                    float r = 0.4f * (0.5f + 0.5f * (float) Math.sin(a * PI)) * fire;
                    rifle(p, k, 0, r); stance(p, k);
                    p.rHandZ += 0.025f * (float) Math.sin(a * 3.3f) * fire;
                }));

        // ---------- AR2 secondary: combine energy ball ----------
        out.add(AttackDef.of("ar2_energy_ball").range(4, 26).cd(220).weight(2).dur(34).weapon("ar2")
                .tick((e, t, n) -> {
                    if (n < 18) { Fx.dustBurst(L(e), 0xd8fbff, 1f, mz(e), 3, 0.3 - n * 0.015); if (n % 4 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 1f, 1.4f + n * 0.04f); }
                    if (n == 18) {
                        Combat.shoot(e, AR2_BALL, mz(e), aim(e, t, 0), 0.9, 20f);
                        e.setDeltaMovement(e.getDeltaMovement().subtract(e.getViewVector(1f).scale(0.3))); e.hurtMarked = true;
                        snd(e, SoundEvents.ILLUSIONER_CAST_SPELL, 1.2f, 1.2f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1f, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), ch = win(t, .05f, .55f) * (1 - win(t, .53f, .55f)), r = 2.4f * kick(t, 18 / 34f, .3f);
                    rifle(p, k, 0.08f * ch, r); stance(p, k); wide(p, ch * 0.5f);
                    p.bodyZ += 0.05f * ch * ch * (float) Math.sin(a * 4.4f); p.rArmY += 0.03f * ch * (float) Math.cos(a * 5);
                    p.rootZ += r * 1.2f; p.bodyX -= r * 0.15f; p.rHandX -= r * 0.3f; p.headX -= r * 0.1f;
                }));

        // ---------- Shotgun ----------
        out.add(AttackDef.of("shotgun").range(0, 10).cd(28).weight(3).dur(24).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 5) {
                        Vec3 m = mz(e);
                        for (int i = 0; i < 7; i++) Combat.hitscan(e, m, aim(e, t, 0.08), 18, 2.7f, 0, 0xffd070);
                        flash(e, 0xffa040); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.2f, 0.9f);
                    }
                    if (n == 12) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.8f);
                    if (n == 16) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.9f); brass(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = kick(t, 5 / 24f, .25f), pump = bump(win(t, .48f, .72f));
                    rifle(p, k, 0, r * 1.5f); stance(p, k);
                    p.lArmX += 0.35f * pump * k; p.rHandX -= r * 0.35f; p.bodyZ += 0.05f * pump; p.headX += 0.06f * pump;
                }));

        // ---------- Shotgun secondary: double-barrel blast ----------
        out.add(AttackDef.of("shotgun_double").range(0, 6).cd(70).weight(2).dur(30).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 6) {
                        Vec3 m = mz(e);
                        for (int i = 0; i < 14; i++) Combat.hitscan(e, m, aim(e, t, 0.11), 14, 2.7f, 0, 0xffc060);
                        for (LivingEntity f : Combat.cone(e, 4, 40)) Combat.knock(f, e.position(), 1.1, 0.3);
                        flash(e, 0xffa040); flash(e, 0xff6020);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.5f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.4f, 0.6f);
                    }
                    if (n == 15) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.6f);
                    if (n == 20) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.7f, 1.7f); brass(e); brass(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .92f), r = kick(t, 6 / 30f, .3f), pump = bump(win(t, .5f, .7f));
                    rifle(p, k, 0, r * 2.4f); wide(p, k);
                    p.bodyX -= r * 0.3f; p.rootY += r * 0.6f; p.rootZ += r * 1.2f; p.headX -= r * 0.15f;
                    p.lArmX += 0.35f * pump * k; p.bodyZ += 0.05f * pump;
                }));

        // ---------- Crossbow: heated rebar ----------
        out.add(AttackDef.of("crossbow").range(8, 40).cd(60).weight(2).dur(36).weapon("crossbow")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.SPYGLASS_USE, 1, 1);
                    if (n == 14) {
                        Combat.shoot(e, BOLT, mz(e), aim(e, t, 0), 3.2, 22f);
                        snd(e, SoundEvents.CROSSBOW_SHOOT, 1.2f, 0.8f); snd(e, SoundEvents.ARROW_SHOOT, 0.8f, 0.6f);
                    }
                    if (n == 22) snd(e, SoundEvents.CROSSBOW_LOADING_START, 1, 0.9f);
                    if (n == 30) snd(e, SoundEvents.CROSSBOW_LOADING_END, 1, 0.9f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .92f), zoom = win(t, .1f, .3f) * (1 - win(t, .45f, .55f)), r = 1.4f * kick(t, 14 / 36f, .2f);
                    rifle(p, k, 0, r); stance(p, k);
                    p.headX += 0.2f * zoom; p.headY -= 0.1f * zoom; p.bodyX += 0.1f * zoom; p.rootY -= 0.5f * zoom;
                    // reload: left hand pulls a new bolt from the back and slides it in
                    float rl = win(t, .55f, .9f);
                    p.lArmX += track(rl, 0, 0, .35f, 1.6f, .45f, 1.4f, .8f, 0.1f, 1, 0) * k;
                    p.lArmZ -= 0.4f * bump(rl); p.headY += 0.2f * bump(rl); p.bodyY -= 0.15f * bump(rl);
                }));
    }

    private void heavy(List<AttackDef> out) {
        // ---------- RPG: laser-guided rocket ----------
        out.add(AttackDef.of("rpg").range(8, 34).cd(120).weight(2).dur(36).weapon("rpg")
                .when((e, t) -> t.getMaxHealth() >= 30 || Combat.around(e, t.position(), 4).size() >= 2)
                .tick((e, t, n) -> {
                    if (t != null && n < 14) Fx.dustBurst(L(e), 0xff2020, 0.7f, mid(t), 1, 0.05);
                    if (n == 14) {
                        Combat.shoot(e, RPG, mz(e), aim(e, t, 0.01), 1.0, 18f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.8, 0.32, 0), 8, 0.25, 0.06); flash(e, 0xffa040);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.55f); snd(e, SoundEvents.GHAST_SHOOT, 0.5f, 1.3f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 2.2f * kick(t, 14 / 36f, .3f);
                    // tube on the shoulder: right arm higher, head leaning into the sight
                    rifle(p, k, -0.12f, r); wide(p, k * 0.6f);
                    p.rArmZ += 0.25f * k; p.headX += 0.15f * k; p.headY -= 0.1f * k; p.headZ += 0.12f * k;
                    p.rHandX -= r * 0.4f; p.bodyX -= r * 0.15f; p.rootZ += r * 0.6f;
                    p.headY += 0.03f * (float) Math.sin(a * 0.8f) * win(t, .1f, .38f) * (1 - win(t, .38f, .4f)); // guiding laser
                }));
    }

    private void throwables(List<AttackDef> out) {
        // ---------- Frag grenade: pull pin, overhand ----------
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(160).weight(2).dur(22).weapons("smg", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.5f);
                    if (n == 11) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.4);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Combat.shoot(e, FRAG, from, aim(e, t, 0.02).add(0, 0.15 + dist * 0.018, 0).normalize(), 0.75 + dist * 0.025, 14f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "frag" : null;
                    p.rArmX = track(t, 0, -0.6f, .15f, -1.3f, .3f, -0.6f, 1, -0.6f); p.rArmY = -0.3f; p.rHandX = -0.3f; // tug the pin
                    p.lArmX = track(t, 0, 0, .15f, -1.2f, .3f, -0.6f, .45f, 0.9f, .52f, -2.4f, .7f, -1.4f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .45f, -0.5f, .52f, -0.1f, 1, 0);
                    p.bodyY = track(t, 0, 0, .45f, -0.55f, .56f, 0.45f, .75f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .45f, -0.2f, .56f, 0.3f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.35f, .56f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .45f, 0.3f, .56f, 0.45f, 1, 0);
                    p.rootZ = track(t, .42f, 0, .56f, -1.4f, 1, 0); p.rootY = -0.5f * bump(win(t, .42f, .8f));
                }));

        // ---------- Xen grenade: underhand roll, implodes ----------
        out.add(AttackDef.of("xen_grenade").range(4, 14).cd(260).weight(1).dur(26).weapons("smg", "xen_grenade")
                .when((e, t) -> Combat.around(e, t.position(), 5).size() >= 2 || t.getMaxHealth() >= 40)
                .tick((e, t, n) -> {
                    if (n < 10) Fx.burst(L(e), ParticleTypes.PORTAL, Combat.muzzle(e, 0.4, -0.4, 0), 3, 0.15, 0.2);
                    if (n == 4) snd(e, SoundEvents.ENDERMAN_AMBIENT, 0.6f, 1.6f);
                    if (n == 13) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, -0.3);
                        double dist = t != null ? from.distanceTo(mid(t)) : 8;
                        Combat.shoot(e, XEN_GREN, from, aim(e, t, 0.02).add(0, 0.05 + dist * 0.02, 0).normalize(), 0.65 + dist * 0.03, 0f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "xen_grenade" : null;
                    p.rArmX = -0.7f; p.rArmY = -0.3f; p.rHandX = -0.3f;
                    // crouch, swing the arm back low then underhand forward
                    p.lArmX = track(t, 0, 0, .2f, -0.6f, .42f, 1.0f, .52f, -1.2f, .7f, -0.9f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .2f, -0.2f, .52f, -0.05f, 1, 0);
                    p.bodyX = track(t, 0, 0, .42f, 0.45f, .55f, 0.35f, .8f, 0.1f, 1, 0); p.headX = -p.bodyX * 0.4f;
                    p.bodyY = track(t, 0, 0, .42f, -0.35f, .55f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.rootY = track(t, 0, 0, .42f, -1.8f, .6f, -1.5f, 1, 0);
                    p.lLegX = track(t, 0, 0, .4f, -0.7f, .8f, -0.6f, 1, 0); p.rLegX = track(t, 0, 0, .4f, 0.5f, .8f, 0.45f, 1, 0);
                }));

        // ---------- Manhack deploy: spin up and release ----------
        out.add(AttackDef.of("manhack_deploy").range(3, 18).cd(300).weight(1).dur(28).weapons("smg", "manhack")
                .tick((e, t, n) -> {
                    if (n < 14 && n % 2 == 0) snd(e, SoundEvents.BEE_LOOP_AGGRESSIVE, 0.6f, 1.2f + n * 0.06f);
                    if (n == 14) {
                        Vec3 from = Combat.muzzle(e, 0.8, -0.4, 0.3);
                        Combat.shoot(e, MANHACK, from, aim(e, t, 0.05).add(0, 0.15, 0).normalize(), 0.7, 5f);
                        snd(e, SoundEvents.BEE_STING, 1, 0.6f); snd(e, SoundEvents.PISTON_EXTEND, 0.7f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "manhack" : null;
                    p.rArmX = -0.6f; p.rArmY = -0.25f; p.rHandX = -0.3f;
                    float spin = win(t, .05f, .45f) * (1 - win(t, .48f, .5f));
                    p.lArmX = track(t, 0, 0, .15f, -1.4f, .45f, -1.5f, .52f, -2.0f, .7f, -1.2f, 1, 0);
                    p.lArmY = track(t, 0, 0, .15f, 0.4f, .52f, -0.2f, 1, 0);
                    p.lHandZ = 0.08f * (float) Math.sin(a * 7) * spin; // buzzing manhack
                    p.bodyY = track(t, 0, 0, .45f, -0.3f, .55f, 0.35f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.bodyZ = 0.02f * (float) Math.sin(a * 6.3f) * spin;
                    p.rootZ = track(t, .44f, 0, .56f, -1.1f, 1, 0);
                    p.lLegX = track(t, 0, 0, .45f, -0.3f, .6f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .55f, 0.35f, 1, 0);
                }));
    }

    private void xen(List<AttackDef> out) {
        // ---------- Mirror Strike: three afterimage dashes through the target, stunstick cuts ----------
        out.add(AttackDef.of("mirror_strike").range(2, 9).cd(160).weight(3).dur(34).weapon("stunstick").noLos()
                .tick((e, t, n) -> {
                    if (n == 0) { snd(e, SoundEvents.ENDERMAN_TELEPORT, 0.9f, 1.6f); Fx.burst(L(e), ParticleTypes.REVERSE_PORTAL, e.position().add(0, 1, 0), 20, 0.4, 0.1); }
                    if (t == null) return;
                    if (n == 6 || n == 14 || n == 22) {
                        ServerLevel l = L(e);
                        Vec3 from = e.position(), tp = t.position();
                        Vec3 dir = tp.subtract(from).multiply(1, 0, 1);
                        if (dir.lengthSqr() < 1e-4) dir = Vec3.directionFromRotation(0, e.getYRot());
                        dir = dir.normalize();
                        // alternate exit side so the dashes criss-cross the target
                        Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(n == 14 ? -1.2 : 1.2);
                        Vec3 to = tp.add(dir.scale(1.6 + t.getBbWidth() * 0.5)).add(side);
                        for (int i = 0; i <= 4; i++) ghost(l, from.lerp(to, i / 4.0), e.getYRot());
                        Fx.beam(l, from.add(0, 1, 0), to.add(0, 1, 0), PULSE, 0.9f, 0.25);
                        e.teleportTo(to.x, tp.y, to.z);
                        float yaw = (float) Math.toDegrees(Math.atan2(-(tp.x - to.x), tp.z - to.z));
                        e.setYRot(yaw); e.yBodyRot = yaw; e.yHeadRot = yaw;
                        Combat.damage(e, t, 8); shock(e, t, 12); stun(t, 10);
                        Fx.arc(l, tp.add(0, 1.1, 0), yaw, 1.2f, -80, 80, n == 14 ? 30 : -30, TEAL, 1f, 14);
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.5f); snd(e, SoundEvents.ENDERMAN_TELEPORT, 0.7f, 1.9f);
                    }
                    if (n == 28) {
                        // the afterimages catch up: a delayed echo hit
                        Combat.damage(e, t, 6); Fx.dustBurst(L(e), TEAL, 1.6f, mid(t), 25, 0.5); Fx.ring(L(e), mid(t), 1.3f, PULSE, 1.2f, 20);
                        snd(e, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    // each dash: crouch-load (anticipation) -> lunge with stick trailing -> slash across on arrival
                    float ph = (t * 34 - 2) / 8f, local = ph - (float) Math.floor(ph);
                    boolean active = ph >= 0 && ph < 3;
                    float dirS = ((int) Math.floor(ph)) % 2 == 0 ? 1 : -1;
                    float load = active ? win(local, 0, .45f) * (1 - win(local, .45f, .55f)) : 0;
                    float cut = active ? bump(win(local, .5f, 1)) : 0;
                    p.rArmX = -0.8f - 1.4f * cut + 0.6f * load; p.rArmY = dirS * (0.8f * load - 0.9f * cut); p.rArmZ = 0.3f * cut;
                    p.rHandX = 0.5f * load - 0.4f * cut;
                    p.lArmX = 0.7f * load - 0.4f * cut; p.lArmZ = -0.6f * load - 0.3f * cut;
                    p.bodyX = 0.5f * load + 0.3f * cut; p.bodyY = dirS * (0.5f * load - 0.55f * cut); p.headY = -p.bodyY * 0.7f; p.headX = -0.2f * load;
                    p.rootY = -1.6f * load - 0.6f * cut + 0.8f * bump(win(t, .85f, 1));
                    p.rootZ = -2.5f * cut;
                    p.lLegX = -0.8f * load - 0.5f * cut; p.rLegX = 0.7f * load + 0.6f * cut;
                    p.rootRoll = dirS * 0.12f * cut;
                    float echo = bump(win(t, .78f, .92f)); p.bodyX -= 0.2f * echo; p.headX -= 0.25f * echo; // looks up as the echo lands
                }));

        // ---------- Xen resonance pulse: gather Xen energy, slam both hands, harmonic shockwave ----------
        out.add(AttackDef.of("xen_resonance_pulse").range(0, 7).cd(240).weight(2).dur(44).weapon("none").noLos()
                .when((e, t) -> Combat.around(e, e.position(), 7).size() >= 2 || e.getHealth() < e.getMaxHealth() * 0.5f)
                .tick((e, t, n) -> {
                    ServerLevel l = L(e); Vec3 c = e.position();
                    if (n < 24) {
                        double ang = n * 0.8, r = 2.4 - n * 0.08;
                        for (int i = 0; i < 3; i++) {
                            double q = ang + i * 2.094;
                            Fx.dustBurst(l, i == 0 ? XEN : TEAL, 1.2f, c.add(Math.cos(q) * r, 0.4 + n * 0.06, Math.sin(q) * r), 2, 0.05);
                        }
                        Fx.burst(l, ParticleTypes.PORTAL, c.add(0, 1.2, 0), 4, 0.5, 0.6);
                        if (n % 4 == 0) snd(e, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 0.5f + n * 0.05f);
                    }
                    if (n == 26 || n == 31 || n == 36) {
                        int wave = (n - 26) / 5; float rad = 3.5f + wave * 1.8f;
                        for (LivingEntity f : Combat.around(e, c, rad)) {
                            Combat.damage(e, f, 9 - wave * 2); Combat.knock(f, c, 0.8 + wave * 0.2, 0.45);
                            f.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0, false, false));
                            f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false));
                            Fx.burst(l, ParticleTypes.REVERSE_PORTAL, mid(f), 10, 0.3, 0.1);
                        }
                        Fx.ring(l, c.add(0, 0.15, 0), rad, wave % 2 == 0 ? XEN : TEAL, 1.8f, 40);
                        Fx.ring(l, c.add(0, 0.6, 0), rad * 0.7f, PULSE, 1.2f, 30);
                        Fx.burst(l, ParticleTypes.SONIC_BOOM, c.add(0, 1, 0), 1, 0, 0);
                        snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.8f + wave * 0.1f, 1.4f + wave * 0.2f); snd(e, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.4f, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float gather = win(t, .05f, .55f) * (1 - win(t, .55f, .6f));
                    float slam = win(t, .55f, .6f) * (1 - win(t, .88f, 1));
                    float hover = (float) Math.sin(a * 0.5f) * gather;
                    // arms spread wide, palms up, rising -> clap down to the ground
                    float armUp = -1.6f * gather - 2.4f * bump(win(t, .5f, .58f));
                    p.rArmX = armUp + 1.2f * slam - 0.3f * slam; p.lArmX = p.rArmX;
                    p.rArmZ = 1.1f * gather - 0.2f * slam; p.lArmZ = -p.rArmZ;
                    p.rArmY = -0.3f * gather; p.lArmY = 0.3f * gather;
                    p.headX = -0.45f * gather + 0.5f * slam; p.bodyX = -0.2f * gather + 0.6f * slam;
                    p.bodyZ = 0.03f * hover;
                    p.rootY = 1.6f * gather + 0.3f * hover - 2.0f * slam;
                    float aft = bump(win(t, .62f, .88f)) * 0.08f * (float) Math.sin(a * 3); // body rings with each wave
                    p.bodyZ += aft; p.headZ = -aft;
                    p.lLegX = -0.15f * gather - 0.8f * slam; p.rLegX = 0.2f * gather + 0.9f * slam;
                    p.lLegZ = -0.2f * gather; p.rLegZ = 0.2f * gather;
                }));

        // ---------- Link: tether the target to nearby foes; every hit on it is shared across the link ----------
        out.add(AttackDef.of("link_shared_damage").range(3, 16).cd(200).weight(2).dur(46).weapon("pulse_pistol")
                .when((e, t) -> Combat.around(e, t.position(), 7).size() >= 2)
                .tick((e, t, n) -> {
                    if (t == null) return;
                    ServerLevel l = L(e);
                    List<LivingEntity> linked = Combat.around(e, t.position(), 7);
                    if (n == 6) { snd(e, SoundEvents.CONDUIT_ACTIVATE, 1.2f, 1.6f); Fx.beam(l, mz(e), mid(t), XEN, 0.9f, 0.25); }
                    if (n >= 8 && n % 2 == 0) for (LivingEntity f : linked) if (f != t) tether(l, mid(t), mid(f));
                    if (n >= 8 && n % 3 == 0) { Fx.ring(l, mid(t), 0.7f, XEN, 1f, 12); for (LivingEntity f : linked) Fx.ring(l, mid(f), 0.5f, TEAL, 0.8f, 10); }
                    if (n == 18 || n == 24 || n == 30 || n == 36) {
                        // shot the anchor; every linked foe takes the same wound
                        Vec3 d = aim(e, t, 0.005);
                        Combat.hitscan(e, mz(e), d, 20, 5f, 0, PULSE); flash(e, PULSE);
                        for (LivingEntity f : linked) if (f != t) { Combat.damage(e, f, 5f); Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, mid(f), 8, 0.3, 0.2); }
                        snd(e, SoundEvents.BEACON_DEACTIVATE, 1f, 1.9f); snd(e, SoundEvents.AMETHYST_BLOCK_HIT, 1.2f, 0.8f);
                    }
                    if (n == 42) {
                        for (LivingEntity f : linked) { Combat.damage(e, f, 4); Fx.dustBurst(l, XEN, 1.4f, mid(f), 14, 0.4); }
                        snd(e, SoundEvents.CHAIN_BREAK, 1.5f, 0.6f); snd(e, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2f, 1.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f);
                    // left hand casts the link (palm thrust, fingers clench), right hand fires the pulse pistol into the anchor
                    float cast = win(t, .05f, .15f) * (1 - win(t, .9f, 1));
                    p.lArmX = lerp(0, -1.7f, cast) + 0.05f * (float) Math.sin(a * 2.2f) * cast;
                    p.lArmY = 0.15f * cast; p.lArmZ = -0.1f * cast; p.lHandX = 0.4f * bump(win(t, .1f, .2f));
                    float r = kick(t, 18 / 46f, .1f) + kick(t, 24 / 46f, .1f) + kick(t, 30 / 46f, .1f) + kick(t, 36 / 46f, .1f);
                    float gun = win(t, .3f, .38f) * (1 - win(t, .86f, .95f));
                    p.rArmX = lerp(-0.4f, -HALF - r * 0.4f, gun); p.rArmY = -0.25f * gun; p.rHandX = -r * 0.3f * gun;
                    p.bodyY = -0.25f * cast + 0.15f * gun; p.headY = -p.bodyY * 0.6f; p.headX = 0.08f * cast;
                    p.bodyZ = 0.03f * (float) Math.sin(a * 1.7f) * cast;
                    float snap = bump(win(t, .88f, .97f)); p.lArmX += 0.9f * snap; p.bodyX = -0.2f * snap + 0.08f * cast; // yanks the link apart
                    stance(p, k); p.rootZ += r * 0.3f - 0.8f * snap;
                }));
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat cloth = Mat.of(0x4e5458).noise(.12f), plate = Mat.of(0x7c8489).bevel(.3f).noise(.08f), dark = Mat.of(0x2a2e31).noise(.1f);
        Mat teal = Mat.of(TEAL_D).bevel(.25f), glow = Mat.of(TEAL).glow(), black = Mat.of(0x18191a), metal = Mat.of(0x9aa0a4).noise(.06f);
        Mat chest = Mat.of(0x6d757a).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            int cx = x + w / 2; // teal clone-unit stripe + CP lambda-less block emblem
            g.rect(x + 1, y + 1, w - 2, 1, TEAL_D);
            g.rect(cx - 1, y + 3, 2, 3, 0x1c1f21); g.rect(cx - 2, y + 4, 4, 1, 0x1c1f21);
        });
        Mat mask = Mat.of(0x5e666b).bevel(.35f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.FRONT) { g.rect(x + 1, y + h - 3, w - 2, 1, 0x2a2e31); g.rect(x + w / 2 - 1, y + h - 2, 2, 2, 0x2a2e31); }
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) g.rect(x + 1, y + 2, w - 2, 1, TEAL_D);
        });
        Mat band = Mat.of(TEAL_D).decal((m, g, x, y, w, h, f) -> { if (f == Mat.Face.FRONT || f == Mat.Face.RIGHT) g.rect(x, y + h / 2, w, 1, 0xd8fbff); });

        // torso: grey jumpsuit, armoured chest + back radio pack, teal-trimmed belt
        rig.box(rig.torso, cloth, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4, -12, -3, 8, 7, 1, .25f);
        rig.box(rig.torso, teal, -3, -5, -2.8f, 6, 1, 1);
        rig.box(rig.torso, dark, -4.5f, -2.5f, -2.5f, 9, 2, 5);
        rig.box(rig.torso, teal, -1, -2.6f, -3.1f, 2, 2, 1);
        rig.box(rig.torso, dark, 2.5f, -2.8f, -3.2f, 2, 2, 1);
        rig.box(rig.torso, plate, -3.5f, -11.5f, 2, 7, 7, 2);
        rig.box(rig.torso, glow, -2.5f, -10.5f, 3.6f, 1, 1, 1);
        rig.box(rig.torso, black, 2.5f, -17, 2.5f, 1, 6, 1); // antenna
        // head: CP helmet + gas mask, twin teal lenses, filter canister
        rig.box(rig.head, mask, -4, -8, -4, 8, 8, 8, .4f);
        rig.box(rig.head, plate, -4.5f, -9, -4.5f, 9, 3, 9);
        rig.box(rig.head, glow, -3, -5.5f, -4.9f, 2, 2, 1);
        rig.box(rig.head, glow, 1, -5.5f, -4.9f, 2, 2, 1);
        rig.box(rig.head, dark, -1.5f, -3f, -5.5f, 3, 3, 2);
        rig.box(rig.head, metal, -4.9f, -3, -3, 1, 2, 2);
        rig.box(rig.head, teal, -1, -9.6f, -3, 2, 1, 6);
        // arms: right shoulder pauldron, left armband (CP), bracers
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, cloth, -2, -2, -2, 4, 12, 4);
            rig.box(arm, dark, -2, 6, -2, 4, 4, 4, .3f);
            rig.box(arm, black, -2, 10, -2, 4, 2, 4, .15f);
        }
        rig.box(rig.rArm, plate, -3.5f, -3, -3, 5, 4, 6);
        rig.box(rig.rArm, teal, -3.8f, -1, -3.2f, 1, 2, 6);
        rig.box(rig.lArm, band, -2, 2, -2, 4, 2, 4, .2f);
        // legs: knee pads, boots
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, cloth, -2, 0, -2, 4, 12, 4);
            rig.box(leg, plate, -2, 4, -2.8f, 4, 3, 1);
            rig.box(leg, black, -2, 9, -2.8f, 4, 3, 5, .2f);
        }
        rig.box(rig.rLeg, dark, -3, 1, -1.5f, 1, 3, 3); // thigh holster
        buildWeapons(rig, metal, dark, black, glow, teal);
    }

    private static void buildWeapons(Rig rig, Mat metal, Mat dark, Mat black, Mat glow, Mat teal) {
        Mat gun = Mat.of(0x34373a).noise(.07f), poly = Mat.of(0x232527), cmb = Mat.of(0x3c4a52).bevel(.3f), pulse = Mat.of(PULSE).glow();
        Mat xen = Mat.of(XEN).glow(), red = Mat.of(0xff2a20).glow(), olive = Mat.of(0x4a5048), wood = Mat.of(0x5a3a22).noise(.15f);
        PartDefinition w;
        w = rig.weapon(rig.rHand, "stunstick");
        rig.box(w, poly, -0.5f, -0.5f, -2, 1, 1, 4); rig.box(w, metal, -0.5f, -0.5f, -11, 1, 1, 9);
        rig.box(w, dark, -1, -1, -3, 2, 2, 1); rig.box(w, glow, -1, -1, -13, 2, 2, 2);
        w = rig.weapon(rig.rHand, "pistol");
        rig.box(w, gun, -1, -2, -6, 2, 2, 7); rig.box(w, poly, -1, 0, -1, 2, 3, 2); rig.box(w, metal, -0.5f, -2.4f, -6, 1, 1, 1);
        w = rig.weapon(rig.rHand, "pulse_pistol");
        rig.box(w, cmb, -1, -2.5f, -7, 2, 3, 8); rig.box(w, poly, -1, 0.5f, -1, 2, 3, 2);
        rig.box(w, pulse, -1.5f, -2, -5, 1, 1, 3); rig.box(w, pulse, -0.5f, -1.5f, -8, 1, 1, 1);
        w = rig.weapon(rig.rHand, "smg");
        rig.box(w, gun, -1, -2.5f, -7, 2, 3, 9); rig.box(w, metal, -0.5f, -2, -10, 1, 1, 3); rig.box(w, poly, -0.5f, 0.5f, -5, 1, 4, 1);
        rig.box(w, poly, -1, -3.5f, -4, 2, 1, 3); rig.box(w, dark, -0.5f, -1.5f, 2, 1, 1, 4); rig.box(w, olive, -1, -0.5f, -9, 2, 2, 3);
        w = rig.weapon(rig.rHand, "ar2");
        rig.box(w, cmb, -1.5f, -3, -9, 3, 4, 12); rig.box(w, poly, -1, -2, -15, 2, 2, 6); rig.box(w, pulse, -2, -2.5f, -6, 1, 2, 4);
        rig.box(w, pulse, -0.5f, -4, -3, 1, 1, 3); rig.box(w, dark, -1, 1, -4, 2, 3, 2); rig.box(w, cmb, -1, -1, 3, 2, 3, 4);
        rig.box(w, glow, 1, -2, -8, 1, 1, 2);
        w = rig.weapon(rig.rHand, "shotgun");
        rig.box(w, gun, -1.5f, -2.5f, -6, 3, 3, 7); rig.box(w, metal, -1, -3, -15, 2, 1, 10);
        rig.box(w, poly, -1.5f, -2, -12, 3, 2, 5); rig.box(w, wood, -1, -1.5f, 1, 2, 2, 5); rig.box(w, metal, -1, -1.5f, -15, 2, 1, 3);
        w = rig.weapon(rig.rHand, "crossbow");
        rig.box(w, gun, -1, -2.5f, -11, 2, 3, 14); rig.box(w, dark, -6, -3, -10, 12, 1, 1); rig.box(w, metal, -0.5f, -4, -16, 1, 1, 11);
        rig.box(w, Mat.of(0xff9a30).glow(), -0.5f, -4, -17, 1, 1, 1); rig.box(w, poly, -1, -5, -5, 2, 2, 4); rig.box(w, poly, -0.5f, 0, -1, 1, 3, 2);
        w = rig.weapon(rig.rHand, "rpg");
        rig.box(w, olive, -2, -5, -12, 4, 4, 18); rig.box(w, dark, -2.5f, -5.5f, -13, 5, 5, 1); rig.box(w, poly, -0.5f, -1, -2, 1, 3, 2);
        rig.box(w, black, 2, -6, -5, 2, 2, 3); rig.box(w, red, 2.5f, -5.5f, -6, 1, 1, 1);
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, olive, -1, -1, -2, 2, 3, 2); rig.box(w, metal, -0.5f, -2, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "xen_grenade");
        rig.box(w, xen, -1, -1, -2, 2, 2, 2); rig.box(w, teal, -1.5f, -0.5f, -2.5f, 3, 1, 3);
        w = rig.weapon(rig.lHand, "manhack");
        rig.box(w, metal, -2.5f, -1, -4, 5, 1, 5); rig.box(w, dark, -1, -2, -2.5f, 2, 3, 2); rig.box(w, red, -0.5f, -2.2f, -3.6f, 1, 1, 1);
    }
}
