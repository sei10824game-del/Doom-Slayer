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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** The Arbiter (Thel 'Vadam), Halo 2: Sangheili in Arbiter armour, Covenant arsenal, energy sword, active camouflage. */
public class Halo2Arbiter extends SlayerKit {
    static final int SWORD_C = 0x6ad8ff, PLASMA_C = 0x5aa8ff, OVER_C = 0x60ff70, NEEDLE_C = 0xff5ad0, CARB_C = 0x8aff4a,
            BEAM_C = 0xff7ab0, ROD_C = 0x7cff4a, SHIELD_C = 0xb0d8ff, PURPLE_BLOOD = 0x5a2a8a;

    static final ProjStyle PLASMA_BOLT = ProjStyle.make().color(PLASMA_C).size(0.3f).life(45).trail(ParticleTypes.ELECTRIC_SPARK);
    static final ProjStyle OVERCHARGE = ProjStyle.make().color(OVER_C).size(0.75f).life(70).homing(0.22f).trail(ParticleTypes.HAPPY_VILLAGER)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, OVER_C, 1.8f, pos, 30, 0.7);
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 20, 0.5, 0.3);
                Fx.ring(l, pos, 1.4f, OVER_C, 1.2f, 18);
                if (hit instanceof Mob m) m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3)); // EMP stun
                Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1f, 1.6f);
            });
    static final ProjStyle NEEDLE = ProjStyle.make().color(NEEDLE_C).size(0.16f).life(50).homing(0.18f).trail(ParticleTypes.END_ROD)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, NEEDLE_C, 0.8f, pos, 4, 0.1);
                if (!(hit instanceof LivingEntity t) || !(p.getOwner() instanceof SlayerEntity o)) return;
                // needles stay lodged; seven of them detonate together (supercombine)
                CompoundTag d = t.getPersistentData();
                int n = d.getInt("h2a_needles") + 1;
                if (n >= 7) {
                    d.putInt("h2a_needles", 0);
                    Vec3 c = t.position().add(0, t.getBbHeight() * 0.5, 0);
                    Combat.blast(o, c, 2.5f, 14f, 0.9f, null);
                    Fx.dustBurst(l, NEEDLE_C, 2f, c, 40, 0.8); Fx.ring(l, c, 1.8f, 0xffa0e8, 1.4f, 24);
                    Fx.burst(l, ParticleTypes.END_ROD, c, 25, 0.4, 0.2);
                    Fx.sound(l, c, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6f, 0.6f); Fx.sound(l, c, SoundEvents.GENERIC_EXPLODE, 0.7f, 1.6f);
                } else {
                    d.putInt("h2a_needles", n);
                    Fx.sound(l, pos, SoundEvents.AMETHYST_BLOCK_HIT, 0.8f, 1.8f);
                }
            });
    static final ProjStyle FUEL_ROD = ProjStyle.make().color(ROD_C).size(0.5f).life(80).gravity(0.012).explode(3f).trail(ParticleTypes.HAPPY_VILLAGER)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 2f);
                Fx.dustBurst(l, ROD_C, 2f, pos, 40, 1.2);
                Fx.ring(l, pos.add(0, 0.2, 0), 2.6f, ROD_C, 1.5f, 26);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1.3f, 1.3f);
            });
    static final ProjStyle PLASMA_NADE = ProjStyle.make().color(0x50a0ff).size(0.32f).life(40).homing(0.25f).explode(3.5f).trail(ParticleTypes.SOUL_FIRE_FLAME)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, 0x50a0ff, 2.2f, pos, 50, 1.4);
                Fx.burst(l, ParticleTypes.SOUL_FIRE_FLAME, pos, 30, 0.8, 0.15);
                Fx.ring(l, pos.add(0, 0.2, 0), 3f, 0x80c0ff, 1.6f, 30);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1.2f, 1.5f); Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1f, 2f);
            });
    static final ProjStyle FRAG = ProjStyle.make().color(0x4a5a2a).size(0.28f).life(34).explode(3.5f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);

    public Halo2Arbiter() {
        super("halo2_arbiter", "The Arbiter", "アービター", "Halo 2");
        eggPrimary = 0x8d8f86; eggSecondary = SWORD_C;
        maxHealth = 90; speed = 0.34; armor = 14; knockbackRes = 0.5;
        scale = 1.12f; width = 0.7f; preferredRange = 9f; dmgMult = 1.05f;
        idleWeaponR = "prifle";
    }

    // ================= helpers (server) =================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.2, 0.32, 0); }
    static Vec3 mzL(SlayerEntity e) { return Combat.muzzle(e, 1.2, -0.32, 0); }
    static Vec3 aim(SlayerEntity e, Vec3 from, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        return Combat.dirTo(from, t, spread, new Random(e.getRandom().nextLong()));
    }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static Vec3 endOf(Vec3 m, LivingEntity t, Vec3 dir, double range) { return t != null ? mid(t) : m.add(dir.scale(range)); }
    static Vec3 chest(SlayerEntity e) { return e.position().add(0, 1.4, 0); }
    static void blood(SlayerEntity e, LivingEntity t, int n) { if (t != null) Fx.dustBurst(L(e), PURPLE_BLOOD, 1.3f, mid(t), n, 0.3); }
    /** horizontal energy-sword sweep: arc trail + everything in the cone takes the hit */
    static void swordArc(SlayerEntity e, float from, float to, float tilt, float dmg, double range, double kb) {
        Fx.arc(L(e), chest(e), e.getYRot(), 1.8f, from, to, tilt, SWORD_C, 1.1f, 16);
        Fx.arc(L(e), chest(e), e.getYRot(), 1.5f, from, to, tilt, 0xffffff, 0.7f, 12);
        for (LivingEntity f : Combat.cone(e, range, 65)) {
            Combat.damage(e, f, dmg); Combat.knock(f, e.position(), kb, 0.25);
            blood(e, f, 8); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(f), 8, 0.2, 0.2);
        }
        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f); snd(e, SoundEvents.BEACON_POWER_SELECT, 0.5f, 2f);
    }
    static void hold(SlayerEntity e, LivingEntity t, double dist, double lift) {
        if (t == null || !Combat.isFoe(e, t)) return;
        Vec3 f = Vec3.directionFromRotation(0, e.getYRot());
        Vec3 p = e.position().add(f.scale(dist + t.getBbWidth() * 0.5)).add(0, lift, 0);
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true;
    }
    static void vent(SlayerEntity e, Vec3 at) {
        Fx.burst(L(e), ParticleTypes.CLOUD, at, 3, 0.08, 0.03);
        Fx.dustBurst(L(e), 0xffb060, 0.7f, at, 2, 0.08);
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** two-handed long Covenant rifle, Sangheili lean: chest forward, left shoulder leading */
    static void rifle(Pose p, float k, float pitch, float recoil) {
        float tw = 0.28f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.15f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.06f - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.5f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * k; p.bodyX += (0.1f + recoil * 0.12f - pitch * 0.2f) * k; p.headX -= (0.1f + recoil * 0.08f) * k;
        p.rootZ += recoil * 0.9f * k;
    }
    /** dual wield: both arms extended, independent recoil per hand */
    static void dual(Pose p, float k, float rr, float rl) {
        p.rArmX = lerp(p.rArmX, -HALF + 0.05f - rr * 0.4f, k); p.rArmY = lerp(p.rArmY, -0.12f, k); p.rArmZ = lerp(p.rArmZ, 0.05f, k);
        p.lArmX = lerp(p.lArmX, -HALF + 0.05f - rl * 0.4f, k); p.lArmY = lerp(p.lArmY, 0.12f, k); p.lArmZ = lerp(p.lArmZ, -0.05f, k);
        p.rHandX -= rr * 0.3f; p.lHandX -= rl * 0.3f;
        p.bodyY += (rr - rl) * 0.12f * k; p.headY -= (rr - rl) * 0.06f * k;
        p.bodyX += (0.12f - (rr + rl) * 0.05f) * k; p.rootZ += (rr + rl) * 0.4f * k;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.35f, k); p.rLegX = lerp(p.rLegX, 0.3f, k);
        p.lLegZ -= 0.08f * k; p.rLegZ += 0.1f * k; p.rootY -= 0.6f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // predatory Sangheili hunch, plasma rifle held low and forward
        float still = 1 - Math.min(1, m * 2);
        p.bodyX += 0.16f; p.headX -= 0.12f;
        p.rArmX = -0.6f + p.rArmX * 0.3f; p.rArmY = -0.1f; p.rArmZ = 0.08f; p.rHandX = -0.25f;
        p.lArmX = -0.2f + p.lArmX * 0.4f; p.lArmZ = -0.18f;
        p.headY += (float) Math.sin(age * 0.04f) * 0.3f * still;
        p.bodyZ += (float) Math.sin(age * 0.07f) * 0.02f * still;
        p.rootY -= 0.4f;
    }

    @Override
    public void passive(SlayerEntity e) {
        // Sangheili energy shield: absorbs (Resistance) while charged, flares when hit, recharges after ~4s without damage
        int last = e.getLastHurtByMobTimestamp(), since = e.tickCount - last;
        boolean charged = since > 80 || e.getHealth() > e.getMaxHealth() * 0.5f;
        if (last > 0 && since == 1 && charged) {
            Fx.dustBurst(L(e), SHIELD_C, 1.3f, e.position().add(0, 1.2, 0), 18, 0.5);
            snd(e, SoundEvents.SHIELD_BLOCK, 0.7f, 1.4f);
        }
        if (last > 0 && since == 3 && !charged) {
            Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, e.position().add(0, 1.2, 0), 20, 0.5, 0.2);
            snd(e, SoundEvents.ITEM_BREAK, 0.8f, 0.7f); // shields down
        }
        if (since >= 80 && e.getHealth() < e.getMaxHealth()) {
            if (since == 80) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.7f);
            if (e.tickCount % 4 == 0) {
                e.heal(1.5f);
                Fx.ring(L(e), e.position().add(0, 0.3 + (e.tickCount % 16) * 0.12, 0), 0.6f, SHIELD_C, 0.8f, 10);
            }
        }
        if (charged && e.tickCount % 20 == 0) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, true, false));
        if (e.isInvisible() && e.tickCount % 4 == 0) Fx.dustBurst(L(e), 0xc0c8d0, 0.5f, e.position().add(0, 1.1, 0), 2, 0.4); // camo shimmer
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- Energy Sword: three-hit combo (forehand, backhand, rising cleave) ----------
        out.add(AttackDef.of("sword_combo").range(0, 3.6f).cd(24).weight(4).dur(32).weapon("sword")
                .tick((e, t, n) -> {
                    if (n == 1) snd(e, SoundEvents.BEACON_ACTIVATE, 0.5f, 2f);
                    if (n == 7) swordArc(e, 70, -70, -10, 8, 3.6, 0.4);
                    if (n == 15) swordArc(e, -70, 70, 15, 8, 3.6, 0.4);
                    if (n == 24) { swordArc(e, 20, -20, 80, 11, 3.8, 0.9); if (t != null && Combat.isFoe(e, t)) t.setDeltaMovement(t.getDeltaMovement().add(0, 0.45, 0)); }
                })
                .anim((p, t, a) -> {
                    // right arm: wind back -> sweep across -> backhand out -> drop low -> rising cut
                    p.rArmX = track(t, 0, -0.6f, .14f, -1.5f, .24f, -1.3f, .34f, -1.2f, .48f, -1.3f, .6f, -0.3f, .76f, -2.6f, .9f, -2.4f, 1, -0.6f);
                    p.rArmY = trackLin(t, 0, 0, .14f, -0.9f, .24f, 0.9f, .34f, 0.8f, .48f, -0.9f, .6f, -0.2f, .76f, 0, 1, 0);
                    p.rArmZ = track(t, 0, 0, .14f, 0.6f, .24f, -0.3f, .48f, 0.5f, .6f, 0.1f, 1, 0);
                    p.rHandY = track(t, 0, 0, .3f, 0, .38f, PI, .6f, PI, .66f, 0, 1, 0); // flip blade for backhand
                    p.bodyY = trackLin(t, 0, 0, .14f, -0.55f, .24f, 0.6f, .34f, 0.55f, .48f, -0.5f, .6f, -0.2f, .76f, 0.15f, 1, 0);
                    p.headY = -p.bodyY * 0.6f;
                    p.bodyX = track(t, 0, 0.15f, .24f, 0.35f, .48f, 0.3f, .62f, 0.45f, .76f, -0.2f, 1, 0.15f);
                    p.lArmX = track(t, 0, -0.3f, .2f, -0.9f, .5f, -0.5f, .76f, 0.4f, 1, -0.2f); p.lArmZ = track(t, 0, -0.2f, .5f, -0.6f, 1, -0.2f);
                    p.lLegX = track(t, 0, 0, .14f, -0.5f, .48f, -0.3f, .62f, -0.7f, .8f, 0.1f, 1, 0);
                    p.rLegX = track(t, 0, 0, .24f, 0.4f, .62f, 0.55f, .8f, 0.2f, 1, 0);
                    p.rootZ = -1.8f * bump(win(t, .1f, .4f)) - 1.5f * bump(win(t, .55f, .9f));
                    p.rootY = track(t, 0, 0, .62f, -1.4f, .78f, 0.8f, 1, 0);
                }));

        // ---------- Energy Sword: lunge (sword-lock dash across the gap) ----------
        out.add(AttackDef.of("sword_lunge").range(3.5f, 10).cd(70).weight(3).dur(24).weapon("sword")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.8f);
                    if (n == 6) {
                        double d = t != null ? Math.min(2.0, 0.25 * e.distanceTo(t)) : 1.2;
                        Combat.lunge(e, d, 0.12); snd(e, SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 1.6f);
                    }
                    if (n > 6 && n < 12) Fx.dustBurst(L(e), SWORD_C, 1f, chest(e), 4, 0.3);
                    if (n == 11) swordArc(e, 80, -80, -5, 15, 4.2, 1.1);
                })
                .anim((p, t, a) -> {
                    // crouch + coil -> dash with blade cocked -> full horizontal cleave -> skid
                    float dash = win(t, .22f, .44f) * (1 - win(t, .5f, .7f));
                    p.rootY = track(t, 0, 0, .22f, -2.2f, .3f, 0.6f, .46f, -1.6f, .7f, -1.2f, 1, 0);
                    p.rootZ = track(t, 0, 0, .22f, 1.2f, .44f, -3.5f, .7f, -2f, 1, 0);
                    p.bodyX = track(t, 0, 0.1f, .22f, 0.55f, .44f, 0.45f, .6f, 0.3f, 1, 0.1f);
                    p.rArmX = track(t, 0, -0.5f, .22f, -1.1f, .44f, -1.4f, .52f, -1.5f, .7f, -1.2f, 1, -0.5f);
                    p.rArmY = trackLin(t, 0, 0, .22f, -0.4f, .44f, -1.1f, .52f, 1.0f, .7f, 0.8f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .44f, 0.7f, .52f, -0.2f, 1, 0);
                    p.bodyY = trackLin(t, 0, 0, .44f, -0.7f, .52f, 0.75f, .7f, 0.6f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.lArmX = track(t, 0, 0, .22f, -0.5f, .44f, 0.7f, .52f, -0.8f, 1, 0); p.lArmZ = -0.3f * dash - 0.2f;
                    p.lLegX = track(t, 0, 0, .22f, -0.7f, .3f, 0.5f, .44f, -0.9f, .7f, -0.7f, 1, 0);
                    p.rLegX = track(t, 0, 0, .22f, 0.6f, .3f, -0.5f, .44f, 0.7f, .7f, 0.6f, 1, 0);
                    p.headX = -0.2f * dash;
                }));

        // ---------- Sword Assassination: grab, impale through the chest, lift, kick off ----------
        out.add(AttackDef.of("sword_assassination").range(0, 3.5f).lowHealthTarget(0.3f).weight(6).cd(90).dur(36).weapon("sword")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.5, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.7f); }
                    if (n < 30) hold(e, t, 0.9, n > 13 && n < 26 ? 0.1 + 0.05 * Math.min(n - 13, 8) : 0);
                    if (n == 13 && t != null) {
                        Combat.damage(e, t, Math.max(0, t.getHealth() - 1)); blood(e, t, 18);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(t), 20, 0.3, 0.3);
                        snd(e, SoundEvents.TRIDENT_HIT, 1.2f, 0.6f); snd(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.6f);
                    }
                    if (n > 13 && n < 26 && n % 3 == 0 && t != null) { Fx.dustBurst(L(e), SWORD_C, 0.9f, mid(t), 4, 0.2); snd(e, SoundEvents.FIRE_EXTINGUISH, 0.4f, 1.8f); }
                    if (n == 28 && t != null) {
                        blood(e, t, 25); Combat.damage(e, t, t.getHealth() + 100); Combat.knock(t, e.position(), 1.0, 0.3);
                        snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 0.6f); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.5f);
                    }
                })
                .anim((p, t, a) -> {
                    float lift = win(t, .38f, .7f) * (1 - win(t, .72f, .8f));
                    // left hand grabs the shoulder, right drives the blade in from the hip and up
                    p.lArmX = track(t, 0, 0, .15f, -1.7f, .75f, -1.6f, .8f, -0.4f, 1, 0); p.lArmY = track(t, 0, 0, .15f, 0.35f, .75f, 0.3f, 1, 0);
                    p.rArmX = track(t, 0, 0, .15f, 0.5f, .3f, 0.6f, .37f, -1.35f, .7f, -2.1f, .78f, -1.9f, .86f, -0.6f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .3f, 0.4f, .37f, 0.1f, 1, 0);
                    p.rHandX = track(t, 0, 0, .3f, 0.4f, .37f, 0, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, -0.45f, .37f, 0.3f, .7f, 0.15f, .8f, -0.3f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.bodyX = track(t, 0, 0, .15f, 0.3f, .37f, 0.45f, .7f, -0.15f, .8f, 0.2f, 1, 0);
                    p.headX = track(t, 0, 0, .37f, 0.3f, .7f, -0.25f, 1, 0);
                    p.rootZ = track(t, 0, 0, .15f, -2f, .8f, -2f, 1, 0);
                    p.rootY = -0.8f * lift;
                    p.lLegX = track(t, 0, 0, .15f, -0.5f, .78f, -0.5f, .84f, 0.4f, 1, 0);
                    p.rLegX = track(t, 0, 0, .15f, 0.45f, .78f, 0.5f, .84f, -1.4f, .92f, -0.8f, 1, 0); // kick the body off the blade
                    p.bodyZ = 0.05f * (float) Math.sin(a * 3f) * lift;
                }));

        // ---------- Dual Plasma Rifles: alternating rapid fire, then overheat vent ----------
        out.add(AttackDef.of("dual_plasma_rifles").range(3, 20).cd(32).weight(4).dur(34).weapons("prifle", "prifle_l")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 22 && n % 2 == 0) {
                        boolean right = (n / 2) % 2 == 0;
                        Vec3 m = right ? mz(e) : mzL(e);
                        Combat.shoot(e, PLASMA_BOLT, m, aim(e, m, t, 0.04), 1.6, 2.6f);
                        Fx.dustBurst(L(e), PLASMA_C, 0.9f, m, 3, 0.06);
                        snd(e, SoundEvents.BLAZE_SHOOT, 0.45f, 1.9f + (right ? 0 : 0.1f));
                    }
                    if (n == 24) snd(e, SoundEvents.FIRE_EXTINGUISH, 0.9f, 1.3f);
                    if (n > 24 && n < 31) { vent(e, mz(e)); vent(e, mzL(e)); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), fire = win(t, .1f, .12f) * (1 - win(t, .66f, .68f));
                    float s = (float) Math.sin(t * 34 * PI / 4);
                    float rr = 0.4f * Math.max(0, s) * fire, rl = 0.4f * Math.max(0, -s) * fire;
                    dual(p, k, rr, rl); stance(p, k);
                    // overheat: arms drop and splay, guns tilted so the vents point skyward, head shakes
                    float heat = win(t, .68f, .76f) * (1 - win(t, .92f, 1));
                    p.rArmX += 0.7f * heat; p.lArmX += 0.7f * heat; p.rArmZ += 0.35f * heat; p.lArmZ -= 0.35f * heat;
                    p.rHandX -= 0.6f * heat; p.lHandX -= 0.6f * heat;
                    p.bodyX -= 0.15f * heat; p.headY += 0.1f * (float) Math.sin(a * 2.5f) * heat;
                }));

        // ---------- Plasma Pistol: overcharge (EMP homing bolt), plasma rifle in the off-hand ----------
        out.add(AttackDef.of("plasma_pistol_overcharge").range(4, 20).cd(110).weight(2).dur(34).weapons("ppistol", "prifle_l")
                .tick((e, t, n) -> {
                    if (n < 20) {
                        Fx.dustBurst(L(e), OVER_C, 0.6f + n * 0.04f, mz(e), 2, 0.12 - n * 0.004);
                        if (n % 4 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.9f, 1.2f + n * 0.04f);
                    }
                    if (n == 21) {
                        Combat.shoot(e, OVERCHARGE, mz(e), aim(e, mz(e), t, 0.01), 1.0, 12f);
                        Fx.dustBurst(L(e), OVER_C, 1.6f, mz(e), 14, 0.2);
                        snd(e, SoundEvents.FIRECHARGE_USE, 1.2f, 1.6f); snd(e, SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.8f);
                    }
                    if (n == 25) { Combat.shoot(e, PLASMA_BOLT, mzL(e), aim(e, mzL(e), t, 0.03), 1.6, 3f); snd(e, SoundEvents.BLAZE_SHOOT, 0.5f, 2f); }
                    if (n == 28) { Combat.shoot(e, PLASMA_BOLT, mzL(e), aim(e, mzL(e), t, 0.03), 1.6, 3f); snd(e, SoundEvents.BLAZE_SHOOT, 0.5f, 2f); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), ch = win(t, .05f, .6f) * (1 - win(t, .6f, .62f));
                    float r = 1.6f * kick(t, 21 / 34f, .2f), rl = 0.5f * (kick(t, 25 / 34f, .08f) + kick(t, 28 / 34f, .08f));
                    dual(p, k, r, rl); stance(p, k);
                    // left gun held back while the pistol charges, shaking more as it builds
                    p.lArmX += 0.6f * ch; p.lArmZ -= 0.25f * ch;
                    float sh = 0.05f * ch * ch;
                    p.rArmZ += sh * (float) Math.sin(a * 5.3f); p.bodyZ += sh * 0.5f * (float) Math.sin(a * 4.7f);
                    p.bodyY -= 0.2f * ch; p.headY += 0.15f * ch; p.headX += 0.1f * ch;
                    p.rootZ += r * 0.6f;
                }));

        // ---------- Dual Needlers: homing crystal spray, 7 needles = supercombine ----------
        out.add(AttackDef.of("dual_needlers").range(4, 18).cd(40).weight(3).dur(32).weapons("needler", "needler_l")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 24) {
                        Vec3 m = n % 2 == 0 ? mz(e) : mzL(e);
                        Vec3 d = aim(e, m, t, 0.12).add(0, 0.06, 0).normalize();
                        Combat.shoot(e, NEEDLE, m, d, 1.1, 1.6f);
                        if (n % 2 == 0) snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f, 1.8f + e.getRandom().nextFloat() * 0.3f);
                    }
                    if (n == 27) snd(e, SoundEvents.AMETHYST_CLUSTER_PLACE, 0.8f, 1.4f); // crystals regrow
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), fire = win(t, .12f, .13f) * (1 - win(t, .77f, .78f));
                    float j = (float) Math.sin(a * 6.1f) * fire;
                    dual(p, k, 0.15f * fire + 0.05f * j, 0.15f * fire - 0.05f * j);
                    stance(p, k);
                    // spraying: slow sweep of the torso across the target
                    p.bodyY += 0.18f * (float) Math.sin(t * PI * 2.5f) * fire; p.headY -= 0.1f * (float) Math.sin(t * PI * 2.5f) * fire;
                    // reload flourish: guns rolled up to inspect crystals
                    float rl = bump(win(t, .8f, 1));
                    p.rHandY += 0.8f * rl; p.lHandY -= 0.8f * rl; p.headX += 0.25f * rl; p.rArmX += 0.4f * rl; p.lArmX += 0.4f * rl;
                }));

        // ---------- Covenant Carbine: semi-automatic radioactive rounds ----------
        out.add(AttackDef.of("carbine").range(6, 30).cd(30).weight(4).dur(30).weapon("carbine")
                .tick((e, t, n) -> {
                    if (n == 6 || n == 10 || n == 14 || n == 18) {
                        Vec3 m = mz(e);
                        Combat.hitscan(e, m, aim(e, m, t, 0.012), 32, 4.5f, 0, CARB_C);
                        Fx.dustBurst(L(e), CARB_C, 1.1f, m, 5, 0.08);
                        if (t != null && e.distanceTo(t) < 32) Fx.burst(L(e), ParticleTypes.HAPPY_VILLAGER, mid(t), 3, 0.2, 0.05);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, 1.4f); snd(e, SoundEvents.AMETHYST_BLOCK_HIT, 0.6f, 0.6f);
                    }
                    if (n == 23) snd(e, SoundEvents.ARMOR_EQUIP_CHAIN, 0.8f, 1.4f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .18f, .9f);
                    float r = kick(t, 6 / 30f, .1f) + kick(t, 10 / 30f, .1f) + kick(t, 14 / 30f, .1f) + kick(t, 18 / 30f, .12f);
                    rifle(p, k, 0, r * 1.2f); stance(p, k);
                    p.rHandX -= r * 0.25f; p.headX += 0.12f * win(t, .1f, .2f) * (1 - win(t, .7f, .8f)); // cheek to scope
                    // magazine swap: left hand slaps the side of the receiver
                    float mag = bump(win(t, .72f, .9f));
                    p.lArmX += 0.45f * mag * k; p.lArmY -= 0.25f * mag; p.bodyY -= 0.1f * mag; p.headY += 0.2f * mag;
                }));

        // ---------- Rifle melee: carbine stock bash ----------
        out.add(AttackDef.of("carbine_bash").range(0, 3).cd(22).weight(3).dur(16).weapon("carbine")
                .tick((e, t, n) -> {
                    if (n == 6) {
                        for (LivingEntity f : Combat.cone(e, 3.2, 55)) {
                            Combat.damage(e, f, 7); Combat.knock(f, e.position(), 1.1, 0.3); Fx.sparks(L(e), mid(f), 8); blood(e, f, 5);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.1f, 0.7f); snd(e, SoundEvents.ANVIL_LAND, 0.3f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    // rifle swung butt-first from the right shoulder across the face
                    p.rArmX = track(t, 0, -1.4f, .2f, -1.0f, .38f, -1.9f, .6f, -1.7f, 1, -1.3f);
                    p.rArmY = trackLin(t, 0, -0.4f, .2f, -0.9f, .38f, 0.6f, .6f, 0.5f, 1, -0.3f);
                    p.rHandX = track(t, 0, 0, .2f, 0.9f, .38f, 1.4f, .7f, 1.2f, 1, 0);
                    p.lArmX = track(t, 0, -1.4f, .2f, -1.2f, .38f, -1.8f, 1, -1.2f); p.lArmY = track(t, 0, 0.3f, .38f, 0.6f, 1, 0.3f);
                    p.bodyY = trackLin(t, 0, 0.2f, .2f, -0.5f, .38f, 0.55f, .6f, 0.45f, 1, 0.1f); p.headY = -p.bodyY * 0.6f;
                    p.bodyX = track(t, 0, 0.1f, .38f, 0.4f, 1, 0.1f);
                    p.lLegX = track(t, 0, 0, .3f, -0.6f, .7f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .38f, 0.45f, 1, 0);
                    p.rootZ = -2f * bump(win(t, .25f, .85f)); p.rootY = -0.7f * bump(win(t, .25f, .9f));
                }));

        // ---------- Beam Rifle: scoped, kneeling, piercing particle beam, then overheat ----------
        out.add(AttackDef.of("beam_rifle").range(12, 48).cd(70).weight(3).dur(40).weapon("beam")
                .tick((e, t, n) -> {
                    if (n == 5) snd(e, SoundEvents.SPYGLASS_USE, 1, 1.2f);
                    if (n == 18 || n == 26) {
                        Vec3 m = mz(e), d = aim(e, m, t, 0.002);
                        Combat.hitscan(e, m, d, 50, 18f, 2, BEAM_C);
                        Fx.beam(L(e), m, endOf(m, t, d, 50), 0xffd0e8, 0.7f, 0.3);
                        if (t != null) Fx.dustBurst(L(e), BEAM_C, 1.2f, mid(t), 10, 0.2);
                        snd(e, SoundEvents.TRIDENT_RIPTIDE_1, 1.1f, 1.8f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.8f, 1.5f);
                    }
                    if (n == 31) snd(e, SoundEvents.FIRE_EXTINGUISH, 0.8f, 1.5f);
                    if (n > 31 && n < 37) vent(e, Combat.muzzle(e, 0.6, 0.35, 0.1));
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), zoom = win(t, .1f, .28f) * (1 - win(t, .74f, .86f));
                    float r = 1.4f * (kick(t, 18 / 40f, .14f) + kick(t, 26 / 40f, .14f));
                    rifle(p, k, 0, r); stance(p, k);
                    // drop to one knee behind the scope
                    p.rootY -= 2.2f * zoom; p.rLegX = lerp(p.rLegX, 0.9f, zoom); p.lLegX = lerp(p.lLegX, -1.1f, zoom);
                    p.headX += 0.22f * zoom; p.headY -= 0.12f * zoom; p.bodyX += 0.15f * zoom;
                    p.bodyZ += 0.01f * (float) Math.sin(a * 0.4f) * zoom; p.rHandX -= r * 0.3f;
                    float vent = bump(win(t, .76f, .95f)); p.rHandX -= 0.5f * vent; p.headY += 0.2f * vent;
                }));

        // ---------- Fuel Rod Gun: three lobbed radioactive rods ----------
        out.add(AttackDef.of("fuel_rod_gun").range(6, 28).cd(110).weight(2).dur(38).weapon("fuelrod")
                .when((e, t) -> t.getMaxHealth() >= 30 || Combat.around(e, t.position(), 5).size() >= 2)
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.PISTON_EXTEND, 0.8f, 0.7f);
                    if (n == 10 || n == 17 || n == 24) {
                        Vec3 m = mz(e); double dist = t != null ? m.distanceTo(mid(t)) : 12;
                        Vec3 d = aim(e, m, t, 0.03).add(0, 0.04 + dist * 0.006, 0).normalize();
                        Combat.shoot(e, FUEL_ROD, m, d, 1.1, 9f);
                        Fx.dustBurst(L(e), ROD_C, 1.4f, m, 10, 0.15); Fx.burst(L(e), ParticleTypes.SMOKE, m, 4, 0.1, 0.03);
                        snd(e, SoundEvents.GHAST_SHOOT, 0.8f, 1.4f); snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1f, 0.6f);
                    }
                    if (n == 31) snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.8f, 1.2f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f);
                    float r = 1.8f * (kick(t, 10 / 38f, .15f) + kick(t, 17 / 38f, .15f) + kick(t, 24 / 38f, .17f));
                    rifle(p, k, -0.12f, r); stance(p, k);
                    p.rootZ += r * 0.6f; p.bodyX -= r * 0.15f; p.rHandX -= r * 0.4f; p.headX -= r * 0.08f;
                    p.lLegX -= 0.15f * k; // braced against the heavy kick
                    // reload: left hand yanks the spent rod out the bottom
                    float rl = bump(win(t, .76f, .95f));
                    p.lArmX += 0.9f * rl; p.lArmZ -= 0.3f * rl; p.headX += 0.2f * rl; p.bodyY -= 0.15f * rl;
                }));

        // ---------- Plasma Grenade: sticky blue lob (left hand) ----------
        out.add(AttackDef.of("plasma_grenade").range(5, 18).cd(150).weight(2).dur(22).weapons("prifle", "pgrenade")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.BEACON_ACTIVATE, 0.7f, 2f); // primed
                    if (n == 11) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.4);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, from, t, 0.02).add(0, 0.1 + dist * 0.012, 0).normalize();
                        Combat.shoot(e, PLASMA_NADE, from, d, 0.8 + dist * 0.02, 13f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.9f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "pgrenade" : null;
                    p.rArmX = -0.8f; p.rArmY = -0.25f; p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, 0, .18f, -0.6f, .4f, 0.8f, .5f, -2.5f, .7f, -1.5f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .4f, -0.6f, .5f, -0.1f, 1, 0);
                    p.bodyY = track(t, 0, 0, .4f, -0.6f, .54f, 0.5f, .75f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0.1f, .4f, -0.25f, .54f, 0.35f, 1, 0.1f);
                    p.lLegX = track(t, 0, 0, .3f, -0.4f, .54f, -0.6f, 1, 0); p.rLegX = track(t, 0, 0, .4f, 0.35f, .54f, 0.5f, 1, 0);
                    p.rootZ = track(t, .38f, 0, .54f, -1.5f, 1, 0); p.rootY = -0.6f * bump(win(t, .35f, .8f));
                }));

        // ---------- Frag Grenade: side-arm bounce throw ----------
        out.add(AttackDef.of("frag_grenade").range(5, 16).cd(150).weight(2).dur(20).weapons("prifle", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.5f);
                    if (n == 9) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.3);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, from, t, 0.03).add(0, 0.08 + dist * 0.014, 0).normalize();
                        Combat.shoot(e, FRAG, from, d, 0.7 + dist * 0.025, 13f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    // low side-arm sling, more horizontal than the plasma lob
                    p.weaponL = t < .46f ? "frag" : null;
                    p.rArmX = -0.7f; p.rArmY = -0.3f; p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, 0, .2f, -0.3f, .38f, -0.2f, .46f, -1.5f, .7f, -1.0f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .2f, -0.9f, .38f, -1.1f, .46f, 0.2f, .7f, 0.1f, 1, 0);
                    p.lArmY = track(t, 0, 0, .38f, -0.5f, .46f, 0.6f, 1, 0);
                    p.bodyY = trackLin(t, 0, 0, .38f, -0.7f, .5f, 0.55f, .75f, 0.35f, 1, 0); p.headY = -p.bodyY * 0.8f;
                    p.bodyZ = track(t, 0, 0, .38f, 0.15f, .5f, -0.1f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .46f, 0.5f, 1, 0);
                    p.rootY = -1.0f * bump(win(t, .2f, .8f)); p.rootZ = track(t, .38f, 0, .5f, -1.2f, 1, 0);
                }));

        // ---------- Active Camouflage: wrist-module activation, goes invisible ----------
        out.add(AttackDef.of("active_camouflage").range(0, 30).cd(700).weight(3).dur(24).weapon("sword").noFace()
                .when((e, t) -> !e.isInvisible() && (e.getHealth() < e.getMaxHealth() * 0.7f || t.getMaxHealth() >= 40))
                .tick((e, t, n) -> {
                    if (n == 8) snd(e, SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 1.6f);
                    if (n == 12) {
                        e.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, false, false));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1, false, false));
                        Vec3 c = e.position().add(0, 1.1, 0);
                        for (int i = 0; i < 4; i++) Fx.ring(L(e), e.position().add(0, 0.2 + i * 0.6, 0), 0.8f, 0xd0e0ff, 0.9f, 16);
                        Fx.burst(L(e), ParticleTypes.REVERSE_PORTAL, c, 25, 0.4, 0.05);
                        snd(e, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.2f, 1.1f); snd(e, SoundEvents.BEACON_DEACTIVATE, 0.6f, 2f);
                        if (e.getTarget() instanceof Mob m && m.getTarget() == e) m.setTarget(null); // lose track of him
                    }
                    if (n > 12) Fx.dustBurst(L(e), 0xc0c8d0, 0.6f, e.position().add(0, 1.1, 0), 3, 0.5);
                })
                .anim((p, t, a) -> {
                    // left wrist raised to the chest, right hand taps the module, head bows, then low ready crouch
                    float tap = win(t, .1f, .3f) * (1 - win(t, .6f, .8f));
                    p.lArmX = track(t, 0, 0, .25f, -1.4f, .6f, -1.4f, .8f, -0.3f, 1, 0); p.lArmY = track(t, 0, 0, .25f, 0.7f, .6f, 0.7f, 1, 0);
                    p.rArmX = track(t, 0, -0.5f, .25f, -1.2f, .33f, -1.0f, .6f, -1.1f, .8f, -0.4f, 1, -0.5f);
                    p.rArmY = track(t, 0, 0, .25f, -0.6f, .6f, -0.6f, 1, 0);
                    p.rHandX = 0.3f * tap;
                    p.headX = 0.35f * tap; p.headY = 0.15f * tap;
                    p.bodyX = 0.15f + 0.15f * tap + 0.25f * win(t, .7f, 1);
                    p.bodyY = -0.15f * tap;
                    float crouch = win(t, .6f, 1);
                    p.rootY = -1.6f * crouch; p.lLegX = -0.5f * crouch; p.rLegX = 0.4f * crouch;
                }));
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat silver = Mat.of(0x9a9c94).bevel(.32f).noise(.07f), steel = Mat.of(0x6c6e68).bevel(.25f).noise(.08f);
        Mat suit = Mat.of(0x2d2836).noise(.12f), skin = Mat.of(0x5d5446).noise(.2f), teeth = Mat.of(0xd8d0b0);
        Mat glow = Mat.of(SHIELD_C).glow(), visor = Mat.of(0xffc860).glow(), cloth = Mat.of(0x3a3446).noise(.18f);
        Mat chestM = Mat.of(0x9a9c94).bevel(.32f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            int cx = x + w / 2; // Covenant chest ridge: raised central keel + swept wings
            g.rect(cx - 1, y, 2, h, 0xc4c6be);
            for (int i = 0; i < 3; i++) { g.px(cx - 2 - i, y + 1 + i, 0x6c6e68); g.px(cx + 1 + i, y + 1 + i, 0x6c6e68); }
        });
        Mat helm = Mat.of(0x9a9c94).bevel(.35f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) g.rect(x, y + h / 2, w, 1, 0x5a5c56);
            if (f == Mat.Face.TOP) g.rect(x + w / 2, y, 1, h, 0xc8cac2);
        });
        // torso: broad armoured chest, raised collar, back hump, tabard
        rig.box(rig.torso, suit, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chestM, -4.5f, -12, -3.2f, 9, 6, 2, .2f);
        rig.box(rig.torso, glow, -0.5f, -10.5f, -3.9f, 1, 3, 1);
        rig.box(rig.torso, steel, -3, -6, -2.8f, 6, 3, 1);
        rig.box(rig.torso, silver, -3.5f, -14, -2.5f, 7, 2, 5);
        rig.box(rig.torso, steel, -4, -12.5f, 1.5f, 8, 7, 3);
        rig.box(rig.torso, glow, -2.5f, -10, 4.2f, 1, 3, 1);
        rig.box(rig.torso, glow, 1.5f, -10, 4.2f, 1, 3, 1);
        rig.box(rig.torso, steel, -4.5f, -3, -2.5f, 9, 2, 5);
        rig.box(rig.torso, cloth, -2, -1, -2.7f, 4, 7, 1);
        rig.box(rig.torso, cloth, -2, -1, 1.7f, 4, 6, 1);
        // head: elongated Sangheili skull under the Arbiter helmet, crest fin, four mandibles
        rig.box(rig.head, helm, -3, -7, -5, 6, 5, 8, .2f);
        rig.box(rig.head, silver, -0.5f, -10, -4, 1, 3, 7);
        rig.box(rig.head, steel, -3.8f, -7, -1, 1, 2, 5);
        rig.box(rig.head, steel, 2.8f, -7, -1, 1, 2, 5);
        rig.box(rig.head, visor, -2.5f, -4.5f, -5.7f, 5, 1, 1);
        rig.box(rig.head, skin, -2.5f, -2, -6, 5, 2, 6);
        rig.box(rig.head, skin, -1.5f, -2, 0, 3, 3, 3);
        rig.box(rig.head, skin, -2.8f, -1.5f, -8.5f, 1, 1, 3);
        rig.box(rig.head, skin, 1.8f, -1.5f, -8.5f, 1, 1, 3);
        rig.box(rig.head, skin, -2.2f, 0, -8, 1, 1, 3);
        rig.box(rig.head, skin, 1.2f, 0, -8, 1, 1, 3);
        rig.box(rig.head, teeth, -2.8f, -1.5f, -9.3f, 1, 1, 1);
        rig.box(rig.head, teeth, 1.8f, -1.5f, -9.3f, 1, 1, 1);
        // arms: oversized pauldrons, ridged gauntlets, camo module on left wrist
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, suit, -2, -2, -2, 4, 12, 4);
            rig.box(arm, silver, s < 0 ? -4 : -3, -4, -3.5f, 7, 4, 7);
            rig.box(arm, steel, s < 0 ? -4.5f : 3.5f, -3, -2, 1, 3, 4);
            rig.box(arm, silver, -2, 5, -2, 4, 4, 4, .35f);
            rig.box(arm, skin, -1.5f, 9, -1.5f, 3, 2, 3);
            if (s > 0) rig.box(arm, glow, -0.5f, 6, -2.8f, 1, 2, 1);
        }
        // legs: digitigrade zig-zag (thigh forward, shin back, long foot)
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, suit, -2, 0, -2.5f, 4, 5, 4);
            rig.box(leg, silver, -2.2f, 0.5f, -3.4f, 4, 4, 1);
            rig.box(leg, silver, -1.8f, 5, -1, 4, 4, 4, .1f);
            rig.box(leg, steel, -1.5f, 9, 0, 3, 2, 3);
            rig.box(leg, steel, -2, 10, -4, 4, 2, 5);
        }
        buildWeapons(rig);
    }

    private static void buildWeapons(Rig rig) {
        Mat cov = Mat.of(0x6c5a8a).bevel(.4f).noise(.05f), covD = Mat.of(0x3e3250), green = Mat.of(0x3e6b48).bevel(.4f).noise(.05f);
        Mat carb = Mat.of(0x3d5f3a).bevel(.35f), needl = Mat.of(0x5b2f6f).bevel(.4f), beamM = Mat.of(0x6b7a8c).bevel(.3f);
        Mat hilt = Mat.of(0x9a9aa4).bevel(.3f), dark = Mat.of(0x222228);
        Mat blue = Mat.of(PLASMA_C).glow(), sword = Mat.of(SWORD_C).glow(), lime = Mat.of(OVER_C).glow(), pink = Mat.of(NEEDLE_C).glow();
        Mat cgl = Mat.of(CARB_C).glow(), bgl = Mat.of(BEAM_C).glow(), rod = Mat.of(ROD_C).glow(), nade = Mat.of(0x50a0ff).glow();
        PartDefinition w;
        // Energy sword: grip bar across the fist, two forked plasma blades
        w = rig.weapon(rig.rHand, "sword");
        rig.box(w, hilt, -1, -3, -1, 2, 6, 2); rig.box(w, dark, -0.5f, -4, -2, 1, 1, 2); rig.box(w, dark, -0.5f, 2, -2, 1, 1, 2);
        rig.box(w, sword, -0.5f, -4, -15, 1, 1, 13); rig.box(w, sword, -0.5f, 2, -15, 1, 1, 13);
        rig.box(w, sword, -0.5f, -3, -18, 1, 1, 3); rig.box(w, sword, -0.5f, 1, -18, 1, 1, 3);
        // Plasma rifle (both hands): pincer body with blue core
        for (int h = 0; h < 2; h++) {
            w = rig.weapon(h == 0 ? rig.rHand : rig.lHand, h == 0 ? "prifle" : "prifle_l");
            rig.box(w, cov, -1.5f, -3, -7, 3, 3, 8); rig.box(w, covD, -1, -4, -11, 2, 1, 5); rig.box(w, covD, -1, 0, -11, 2, 1, 5);
            rig.box(w, blue, -0.5f, -2, -9, 1, 2, 2); rig.box(w, covD, -1, 0, -2, 2, 2, 2);
        }
        // Plasma pistol
        w = rig.weapon(rig.rHand, "ppistol");
        rig.box(w, green, -1.5f, -3, -6, 3, 3, 6); rig.box(w, green, -2, -3.5f, -7, 4, 4, 1); rig.box(w, lime, -0.5f, -2, -7.5f, 1, 1, 1);
        rig.box(w, covD, -1, 0, -2, 2, 2, 2);
        // Needlers (both hands): purple body with pink crystal quills
        for (int h = 0; h < 2; h++) {
            w = rig.weapon(h == 0 ? rig.rHand : rig.lHand, h == 0 ? "needler" : "needler_l");
            rig.box(w, needl, -1.5f, -2.5f, -8, 3, 3, 9); rig.box(w, pink, -0.5f, -4.5f, -7, 1, 2, 1);
            rig.box(w, pink, -0.5f, -4.5f, -4.5f, 1, 2, 1); rig.box(w, pink, -0.5f, -4.5f, -2, 1, 2, 1); rig.box(w, covD, -1, 0.5f, -2, 2, 2, 2);
        }
        // Covenant carbine
        w = rig.weapon(rig.rHand, "carbine");
        rig.box(w, carb, -1, -2.5f, -14, 2, 3, 16); rig.box(w, covD, -1, -4.5f, -7, 2, 2, 4); rig.box(w, cgl, -1.6f, -2, -12, 1, 1, 8);
        rig.box(w, covD, -1, 0.5f, -5, 2, 2, 3);
        // Beam rifle: long split-barrel sniper
        w = rig.weapon(rig.rHand, "beam");
        rig.box(w, beamM, -1, -2, -18, 2, 2, 20); rig.box(w, covD, -2, -3, -10, 4, 1, 6); rig.box(w, covD, -0.5f, -4.5f, -6, 1, 2, 4);
        rig.box(w, bgl, -0.5f, -1.5f, -19, 1, 1, 1); rig.box(w, bgl, -1.5f, -1.5f, -14, 1, 1, 4);
        // Fuel rod gun
        w = rig.weapon(rig.rHand, "fuelrod");
        rig.box(w, carb, -2, -3.5f, -11, 4, 5, 14); rig.box(w, rod, -0.5f, -4.5f, -9, 1, 1, 7); rig.box(w, covD, -1.5f, -3, -14, 3, 3, 3);
        rig.box(w, rod, -1, 1.5f, -4, 2, 2, 3);
        // grenades (left hand)
        w = rig.weapon(rig.lHand, "pgrenade");
        rig.box(w, nade, -1, -1, -2, 2, 2, 2); rig.box(w, covD, -1.5f, -0.5f, -1.5f, 3, 1, 1);
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, Mat.of(0x4a5a2a), -1, -1, -2, 2, 2, 2); rig.box(w, dark, -0.5f, -1.8f, -1.5f, 1, 1, 1);
    }
}
