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

/**
 * Master Chief, Halo 5: Guardians (Mjolnir Mark VI GEN2, Blue Team leader).
 * Spartan Abilities (Charge, Ground Pound, Clamber, Slide, Thruster, Stabilize, Smart-link), recharging energy shield,
 * full UNSC + Forerunner/Covenant sandbox, rifle bash, energy sword and staged assassinations.
 */
public class Halo5Chief extends SlayerKit {
    static final int SHIELD_C = 0xffd040, THRUST_C = 0x9fd8ff, VISOR = 0xffb428, PLASMA_C = 0x4aa8ff, SWORD_C = 0x7fe6ff,
            FORE_C = 0xff9a30, RAIL_C = 0x8ad0ff, TRACER = 0xffe9a0;
    static final float SHIELD_MAX = 10f;

    static final ProjStyle SPNKR = ProjStyle.make().color(0xffa040).size(0.45f).life(100).explode(3.5f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle HYDRA = ProjStyle.make().color(0xff7a30).size(0.22f).life(70).explode(1.6f).homing(0.22f).trail(ParticleTypes.SMOKE);
    static final ProjStyle FRAG = ProjStyle.make().color(0x56603a).size(0.26f).life(34).explode(3.6f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle PLASMA_NADE = ProjStyle.make().color(PLASMA_C).size(0.3f).life(40).explode(3.2f).gravity(0.035).homing(0.12f)
            .trail(ParticleTypes.ELECTRIC_SPARK)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.burst(l, ParticleTypes.EXPLOSION, pos, 1, 0, 0);
                Fx.dustBurst(l, PLASMA_C, 2f, pos, 40, 1.4);
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 30, 0.8, 0.3);
                Fx.ring(l, pos, 2.2f, 0x9fd0ff, 1.4f, 26); Fx.ring(l, pos.add(0, 0.4, 0), 3.2f, PLASMA_C, 1.2f, 30);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1.2f, 1.5f); Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1.2f, 1.8f);
            });

    public Halo5Chief() {
        super("halo5_chief", "Master Chief (Halo 5)", "マスターチーフ (Halo 5)", "Halo 5: Guardians");
        eggPrimary = 0x4b5833; eggSecondary = VISOR;
        maxHealth = 90; speed = 0.33; armor = 14; knockbackRes = 0.7;
        scale = 1.12f; width = 0.7f; preferredRange = 11f; dmgMult = 1.05f;
        idleWeaponR = "br75";
    }

    // ================= helpers (server) =================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.25, 0.28, 0.02); }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static Vec3 head(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.88, 0); }
    static Vec3 fwd(SlayerEntity e) { return Vec3.directionFromRotation(0, e.getYRot()); }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        return Combat.dirTo(mz(e), t, spread, new Random(e.getRandom().nextLong()));
    }
    /** precision aim at the head (Smart-link / DMR headshots) */
    static Vec3 aimHead(SlayerEntity e, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        Random r = new Random(e.getRandom().nextLong());
        Vec3 d = head(t).subtract(mz(e)).normalize();
        return d.add(r.nextGaussian() * spread, r.nextGaussian() * spread, r.nextGaussian() * spread).normalize();
    }
    static Vec3 endOf(SlayerEntity e, LivingEntity t, Vec3 dir, double range) { return t != null ? mid(t) : mz(e).add(dir.scale(range)); }
    static void flash(SlayerEntity e, int rgb) {
        Vec3 m = mz(e);
        Fx.burst(L(e), ParticleTypes.FLAME, m, 2, 0.04, 0.02);
        Fx.dustBurst(L(e), rgb, 1.1f, m, 3, 0.07);
    }
    static void brass(SlayerEntity e) { Fx.dustBurst(L(e), 0xc8a040, 0.5f, Combat.muzzle(e, 0.45, 0.55, 0.1), 1, 0.04); }
    static void bullet(SlayerEntity e, LivingEntity t, double spread, float dmg, int pierce, double range) {
        Combat.hitscan(e, mz(e), aim(e, t, spread), range, dmg, pierce, TRACER);
    }
    /** thruster pack exhaust (two nozzles on the back) */
    static void exhaust(SlayerEntity e, Vec3 dir, int n) {
        Vec3 back = fwd(e).scale(-0.45), right = new Vec3(-fwd(e).z, 0, fwd(e).x).scale(0.18), base = e.position().add(0, 1.15, 0).add(back);
        for (int s = -1; s <= 1; s += 2) {
            Vec3 q = base.add(right.scale(s));
            Fx.dustBurst(L(e), THRUST_C, 1.1f, q, n, 0.06);
            L(e).sendParticles(ParticleTypes.CLOUD, q.x, q.y, q.z, n / 2 + 1, dir.x * 0.1, dir.y * 0.1, dir.z * 0.1, 0.05);
        }
    }
    static CompoundTag D(SlayerEntity e) { return e.getPersistentData(); }
    /** pin an assassination victim in front of the Chief */
    static void hold(SlayerEntity e, LivingEntity t, double dist, double lift) {
        if (t == null || !Combat.isFoe(e, t)) return;
        Vec3 p = e.position().add(fwd(e).scale(dist + t.getBbWidth() * 0.5)).add(0, lift, 0);
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true;
    }
    static void wound(SlayerEntity e, LivingEntity t, float leave) {
        if (t == null) return;
        Combat.damage(e, t, Math.max(0, t.getHealth() - leave));
        Fx.sparks(L(e), mid(t), 8); Fx.burst(L(e), ParticleTypes.DAMAGE_INDICATOR, mid(t), 4, 0.3, 0.1);
    }
    static void assassinate(SlayerEntity e, LivingEntity t, SoundEvent s) {
        if (t == null) return;
        Combat.damage(e, t, t.getHealth() + 100);
        Fx.sparks(L(e), mid(t), 14); Fx.burst(L(e), ParticleTypes.DAMAGE_INDICATOR, mid(t), 10, 0.35, 0.1);
        snd(e, s, 1.2f, 0.8f); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.6f);
    }
    static void jump(SlayerEntity e, double up, double ahead) { Vec3 f = fwd(e); e.setDeltaMovement(f.x * ahead, up, f.z * ahead); e.hurtMarked = true; }
    static void hover(SlayerEntity e, boolean on) { e.setNoGravity(on); if (on) { e.setDeltaMovement(Vec3.ZERO); e.hurtMarked = true; } }

    // ================= passive: recharging energy shield =================
    @Override
    public void passive(SlayerEntity e) {
        if (e.level().isClientSide) return;
        CompoundTag d = D(e);
        float abs = e.getAbsorptionAmount(), last = d.getFloat("h5Shield");
        int now = e.tickCount;
        if (abs < last - 0.01f) {
            d.putInt("h5Hit", now);
            Fx.dustBurst(L(e), SHIELD_C, 1.2f, e.position().add(0, 1.1, 0), 10, 0.5); // shield flare
            if (abs <= 0.01f) { // pop
                Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, e.position().add(0, 1.1, 0), 24, 0.5, 0.3);
                snd(e, SoundEvents.SHIELD_BREAK, 1f, 1.4f); snd(e, SoundEvents.GLASS_BREAK, 0.6f, 1.8f);
            }
        }
        int quiet = now - Math.max(d.getInt("h5Hit"), e.getLastHurtByMobTimestamp());
        if (quiet > 100 && abs < SHIELD_MAX) {
            if (!d.getBoolean("h5Charging")) { d.putBoolean("h5Charging", true); snd(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.6f); }
            abs = Math.min(SHIELD_MAX, abs + 0.25f); e.setAbsorptionAmount(abs);
            if (now % 3 == 0) {
                double a = now * 0.5;
                Fx.dustBurst(L(e), SHIELD_C, 0.9f, e.position().add(Math.cos(a) * 0.5, 0.2 + (abs / SHIELD_MAX) * 1.8, Math.sin(a) * 0.5), 2, 0.05);
            }
            if (abs >= SHIELD_MAX) snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.5f);
        } else d.putBoolean("h5Charging", false);
        if (abs <= 0.01f && quiet < 100 && now % 12 == 0) snd(e, SoundEvents.NOTE_BLOCK_BIT.value(), 0.5f, 2f); // low-shield alarm
        if (abs > 0.01f && now % 20 == 0) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false));
        d.putFloat("h5Shield", abs);
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** rifle shouldered square to the target (Halo stance: chest nearly square, slight right cant) */
    static void shoulder(Pose p, float k, float pitch, float recoil) {
        float tw = 0.15f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.28f, k); p.rArmY = lerp(p.rArmY, -0.1f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.08f - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.5f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * k; p.bodyX += (recoil * 0.1f - pitch * 0.2f + 0.05f) * k; p.headX -= recoil * 0.07f * k;
        p.rootZ += recoil * 0.8f * k;
    }
    /** Smart-link: head drops onto the optic, elbows tuck in */
    static void smartlink(Pose p, float z) {
        p.headX += 0.2f * z; p.headY -= 0.1f * z; p.headZ += 0.08f * z; p.bodyX += 0.1f * z;
        p.rArmY += 0.08f * z; p.lArmY -= 0.06f * z; p.rArmX -= 0.04f * z; p.lArmX -= 0.06f * z;
    }
    /** heavy weapon held low at the hip */
    static void hip(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -1.05f + pitch - recoil * 0.2f, k); p.rArmY = lerp(p.rArmY, -0.2f, k); p.rArmZ = lerp(p.rArmZ, 0.12f, k);
        p.lArmX = lerp(p.lArmX, -1.25f + pitch - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.65f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX = lerp(p.rHandX, -0.5f, k);
        p.bodyX += (0.1f + recoil * 0.1f) * k; p.bodyY += 0.12f * k; p.headY -= 0.12f * k; p.rootZ += recoil * 0.6f * k;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.26f, k);
        p.lLegZ -= 0.07f * k; p.rLegZ += 0.08f * k; p.rootY -= 0.45f * k;
    }
    static void brace(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.25f, k); p.rLegX = lerp(p.rLegX, 0.2f, k);
        p.lLegZ -= 0.28f * k; p.rLegZ += 0.28f * k; p.rootY -= 1.1f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // BR75 at low ready, muzzle angled down-left, both hands on it
        p.rArmX = -0.6f + p.rArmX * 0.25f; p.rArmY = -0.2f;
        p.lArmX = -0.85f + p.lArmX * 0.25f; p.lArmY = 0.6f; p.lArmZ = 0.05f;
        p.rHandX = -0.3f; p.bodyX += 0.05f;
        p.bodyX += 0.015f * (float) Math.sin(age * 0.08f); p.headY += 0.04f * (float) Math.sin(age * 0.021f);
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- Melee: rifle bash (butt-stroke with the BR75) ----------
        out.add(AttackDef.of("rifle_bash").range(0, 3.2f).cd(20).dur(16).weapon("br75")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.9f, 0.8f);
                    if (n == 6) {
                        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.5f, -60, 40, -20, 0xd0d0d0, 0.8f, 10);
                        for (LivingEntity f : Combat.cone(e, 3.3, 50)) {
                            Combat.damage(e, f, 10); Combat.knock(f, e.position(), 1.1, 0.3); Fx.sparks(L(e), mid(f), 8);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.7f); snd(e, SoundEvents.ANVIL_LAND, 0.35f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    // wind up: rifle drawn back across the right hip -> butt swung forward-left -> follow through -> reset
                    p.rArmX = track(t, 0, -0.6f, .3f, -0.2f, .42f, -1.7f, .6f, -1.5f, 1, -0.6f);
                    p.rArmY = track(t, 0, -0.2f, .3f, -0.5f, .42f, 0.35f, .62f, 0.45f, 1, -0.2f);
                    p.rArmZ = track(t, 0, 0, .3f, 0.35f, .42f, -0.2f, 1, 0);
                    p.lArmX = track(t, 0, -0.85f, .3f, -0.5f, .42f, -1.5f, .62f, -1.3f, 1, -0.85f);
                    p.lArmY = track(t, 0, 0.6f, .42f, 0.9f, 1, 0.6f);
                    p.rHandX = track(t, 0, -0.3f, .3f, 0.4f, .42f, -0.9f, .7f, -0.7f, 1, -0.3f);
                    p.bodyY = track(t, 0, 0, .3f, -0.45f, .42f, 0.5f, .62f, 0.55f, 1, 0);
                    p.bodyX = track(t, 0, 0.05f, .3f, -0.08f, .45f, 0.32f, .7f, 0.25f, 1, 0.05f);
                    p.headY = -p.bodyY * 0.6f; p.headX = 0.15f * bump(win(t, .35f, .7f));
                    p.lLegX = track(t, 0, 0, .3f, -0.15f, .42f, -0.55f, .8f, -0.5f, 1, 0);
                    p.rLegX = track(t, 0, 0, .3f, 0.2f, .45f, 0.45f, .8f, 0.35f, 1, 0);
                    p.rootZ = track(t, 0, 0, .3f, 0.6f, .45f, -2.4f, .75f, -2f, 1, 0); p.rootY = -0.7f * bump(win(t, .3f, .85f));
                }));

        // ---------- Assassination 1: grab, knee to the gut, neck snap over the shoulder ----------
        out.add(AttackDef.of("assassination_knee_snap").range(0, 3.5f).lowHealthTarget(0.25f).weight(6).cd(70).dur(36).weapon("none")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.45, 0.05); snd(e, SoundEvents.ARMOR_EQUIP_NETHERITE, 1, 0.7f); }
                    hold(e, t, 0.85, n >= 10 && n < 16 ? 0.15 : 0);
                    if (n == 11) { wound(e, t, 1); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.3f, 0.6f); }
                    if (n == 25) { assassinate(e, t, SoundEvents.BONE_BLOCK_BREAK); snd(e, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 0.8f, 1.5f); }
                })
                .anim((p, t, a) -> {
                    p.lArmX = track(t, 0, 0, .12f, -1.7f, .4f, -1.4f, .62f, -1.6f, .75f, -0.8f, 1, 0); p.lArmY = track(t, 0, 0, .12f, 0.4f, 1, 0);
                    p.rArmX = track(t, 0, 0, .12f, -1.6f, .4f, -1.2f, .6f, -1.8f, .7f, -1.7f, 1, 0); p.rArmY = track(t, 0, 0, .12f, -0.4f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .58f, -0.15f, .7f, 0.5f, .85f, 0.3f, 1, 0); p.lArmZ = track(t, 0, 0, .58f, -0.25f, .7f, 0.35f, 1, 0);
                    // knee: right leg drives up, body crunches over it
                    p.rLegX = track(t, 0, 0, .12f, 0.35f, .22f, 0.5f, .31f, -1.55f, .4f, -1.2f, .5f, 0.2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .12f, -0.3f, .31f, 0.1f, .5f, -0.35f, .85f, -0.35f, 1, 0);
                    p.bodyX = track(t, 0, 0, .12f, 0.15f, .31f, 0.45f, .5f, 0.15f, .7f, 0.25f, 1, 0);
                    p.rootY = track(t, 0, 0, .22f, -0.3f, .31f, 0.6f, .45f, 0, 1, 0);
                    // snap: violent torso twist
                    p.bodyY = trackLin(t, 0, 0, .55f, -0.35f, .63f, -0.42f, .7f, 0.6f, .85f, 0.5f, 1, 0);
                    p.headY = -p.bodyY * 0.4f; p.headX = 0.25f * win(t, .1f, .3f) * (1 - win(t, .85f, 1));
                    p.rootYaw = 0.2f * bump(win(t, .62f, .9f)); p.rootZ = track(t, 0, 0, .12f, -2f, .85f, -2f, 1, 0);
                }));

        // ---------- Assassination 2: lift by the throat, body-slam into the ground ----------
        out.add(AttackDef.of("assassination_choke_slam").range(0, 3.5f).lowHealthTarget(0.25f).weight(6).cd(70).dur(38).weapon("none")
                .tick((e, t, n) -> {
                    if (n == 0) snd(e, SoundEvents.ARMOR_EQUIP_NETHERITE, 1, 0.6f);
                    hold(e, t, 0.95, n < 6 ? 0 : n < 22 ? Math.min(1.1, (n - 6) * 0.1) : n < 25 ? 0.2 : 0);
                    if (n > 8 && n < 22 && n % 4 == 0 && t != null) { snd(e, SoundEvents.SKELETON_HURT, 0.5f, 0.6f); Fx.sparks(L(e), head(t), 2); }
                    if (n == 25 && t != null) {
                        assassinate(e, t, SoundEvents.ANVIL_LAND);
                        Fx.ring(L(e), t.position().add(0, 0.1, 0), 1.6f, 0x6a5a48, 1.3f, 22); Fx.burst(L(e), ParticleTypes.POOF, t.position(), 14, 0.5, 0.05);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    float lift = win(t, .15f, .55f) * (1 - win(t, .58f, .66f));
                    p.rArmX = track(t, 0, 0, .15f, -1.6f, .55f, -2.75f, .6f, -2.85f, .66f, -0.5f, .8f, -0.5f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .15f, -0.1f, .55f, 0.1f, 1, 0); p.rArmY = 0.05f * (float) Math.sin(a * 2.7f) * lift;
                    p.lArmX = track(t, 0, 0, .2f, -0.5f, .55f, -0.4f, .62f, -1.2f, .7f, 0.2f, 1, 0); p.lArmZ = track(t, 0, 0, .55f, -0.45f, .7f, -0.6f, 1, 0);
                    p.bodyX = track(t, 0, 0, .15f, 0.1f, .55f, -0.25f, .6f, -0.3f, .68f, 0.65f, .82f, 0.55f, 1, 0);
                    p.bodyY = track(t, 0, 0, .55f, -0.2f, .66f, 0.25f, 1, 0); p.bodyZ = 0.05f * (float) Math.sin(a * 2.3f) * lift;
                    p.headX = track(t, 0, 0, .15f, -0.25f, .55f, -0.45f, .66f, 0.4f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.rootY = track(t, 0, 0, .55f, 0.5f, .6f, 0.8f, .68f, -2.4f, .85f, -2.0f, 1, 0);
                    p.rootZ = track(t, 0, 0, .6f, -0.5f, .68f, -2.2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .15f, -0.35f, .6f, -0.3f, .68f, -1.2f, .85f, -1.1f, 1, 0);
                    p.rLegX = track(t, 0, 0, .15f, 0.3f, .6f, 0.3f, .68f, 1.1f, .85f, 1.0f, 1, 0);
                }));

        // ---------- Assassination 3: energy sword impale + lift + rip out ----------
        out.add(AttackDef.of("assassination_sword_impale").range(0, 3.5f).lowHealthTarget(0.3f).weight(5).cd(80).dur(32).weapon("sword")
                .tick((e, t, n) -> {
                    if (n == 0) { snd(e, SoundEvents.BEACON_ACTIVATE, 1, 1.8f); Combat.lunge(e, 0.4, 0.03); }
                    hold(e, t, 1.1, n >= 10 && n < 22 ? 0.4 : 0);
                    if (n == 10 && t != null) {
                        wound(e, t, 1); Fx.dustBurst(L(e), SWORD_C, 1.3f, mid(t), 16, 0.3);
                        snd(e, SoundEvents.TRIDENT_HIT, 1.2f, 0.7f); snd(e, SoundEvents.FIRECHARGE_USE, 0.8f, 1.6f);
                    }
                    if (n > 10 && n < 22 && t != null && n % 2 == 0) Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(t), 3, 0.2, 0.1);
                    if (n == 22 && t != null) {
                        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.6f, -10, 80, 70, SWORD_C, 1f, 14);
                        assassinate(e, t, SoundEvents.TRIDENT_RETURN);
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -0.5f, .2f, -0.2f, .31f, -1.6f, .45f, -1.9f, .66f, -2.05f, .75f, -0.8f, 1, 0);
                    p.rArmY = track(t, 0, 0, .2f, -0.4f, .31f, 0.1f, .66f, 0, .75f, 0.5f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .66f, 0, .75f, 0.6f, 1, 0);
                    p.rHandX = track(t, 0, 0, .2f, 0.3f, .31f, 0, .66f, -0.35f, .75f, 0.2f, 1, 0);
                    p.lArmX = track(t, 0, 0, .2f, -1.3f, .31f, -0.6f, .66f, -0.7f, 1, 0); p.lArmZ = track(t, 0, 0, .2f, -0.2f, .31f, -0.6f, 1, 0);
                    p.bodyY = track(t, 0, 0, .2f, -0.45f, .31f, 0.15f, .66f, 0.1f, .75f, 0.6f, 1, 0);
                    p.bodyX = track(t, 0, 0, .2f, -0.05f, .31f, 0.35f, .45f, 0.1f, .66f, -0.1f, .75f, 0.3f, 1, 0);
                    p.headY = -p.bodyY * 0.5f; p.headX = track(t, .3f, 0, .45f, -0.2f, .66f, -0.3f, .8f, 0.2f, 1, 0);
                    p.rootZ = track(t, 0, 0, .2f, 0.6f, .31f, -2.6f, .75f, -2.2f, 1, 0);
                    p.rootY = track(t, 0, 0, .2f, -0.4f, .31f, -1.0f, .5f, 0.2f, .66f, 0.3f, .75f, -0.6f, 1, 0);
                    p.lLegX = track(t, 0, 0, .2f, -0.2f, .31f, -0.7f, .85f, -0.6f, 1, 0); p.rLegX = track(t, 0, 0, .31f, 0.55f, .85f, 0.45f, 1, 0);
                }));

        // ---------- Spartan Charge (thruster-assisted shoulder tackle) ----------
        out.add(AttackDef.of("spartan_charge").range(3, 9).cd(110).weight(3).dur(26).weapon("br75")
                .onStart((e, t, n) -> D(e).putBoolean("h5Chg", false))
                .tick((e, t, n) -> {
                    if (n < 7 && n % 2 == 0) { exhaust(e, Vec3.ZERO, 2); snd(e, SoundEvents.BLAZE_BURN, 0.6f, 1.6f + n * 0.05f); }
                    if (n == 7) { Combat.lunge(e, 1.45, 0.08); snd(e, SoundEvents.FIRECHARGE_USE, 1.2f, 0.7f); snd(e, SoundEvents.ELYTRA_FLYING, 0.5f, 1.8f); }
                    if (n >= 7 && n <= 15 && !D(e).getBoolean("h5Chg")) {
                        exhaust(e, fwd(e).scale(-1), 3);
                        List<LivingEntity> hit = Combat.around(e, e.position().add(0, 1, 0).add(fwd(e).scale(1.0)), 1.5);
                        if (!hit.isEmpty()) {
                            D(e).putBoolean("h5Chg", true);
                            for (LivingEntity f : hit) { Combat.damage(e, f, 15); Combat.knock(f, e.position(), 1.8, 0.45); Fx.sparks(L(e), mid(f), 12); }
                            Fx.ring(L(e), e.position().add(fwd(e).scale(1.2)).add(0, 1, 0), 1.2f, THRUST_C, 1.3f, 18);
                            snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.4f, 0.5f); snd(e, SoundEvents.ANVIL_LAND, 0.5f, 0.8f);
                            e.setDeltaMovement(e.getDeltaMovement().scale(0.2)); e.hurtMarked = true;
                        }
                    }
                })
                .anim((p, t, a) -> {
                    float wind = win(t, 0, .26f) * (1 - win(t, .27f, .32f)), run = win(t, .26f, .3f) * (1 - win(t, .62f, .75f));
                    // wind-up: drop low, left shoulder leads, rifle pulled to the chest
                    p.rArmX = track(t, 0, -0.6f, .26f, -0.9f, .6f, -0.9f, 1, -0.6f); p.rArmY = -0.3f; p.rHandX = -0.6f;
                    p.lArmX = track(t, 0, -0.85f, .26f, -0.2f, .45f, -0.3f, .6f, -0.6f, 1, -0.85f);
                    p.lArmZ = track(t, 0, 0, .26f, -0.35f, .6f, -0.3f, 1, 0); p.lArmY = track(t, 0, 0.6f, .26f, 0.1f, 1, 0.6f);
                    p.bodyY = track(t, 0, 0, .26f, -0.55f, .6f, -0.5f, 1, 0); p.headY = -p.bodyY * 0.8f;
                    p.bodyX = 0.45f * wind + 0.6f * run; p.headX = -0.35f * run - 0.1f * wind;
                    p.rootY = -1.2f * wind - 0.6f * run;
                    float stride = (float) Math.sin(a * 1.4f) * run;
                    p.rLegX = lerp(0.35f * wind, 1.0f * stride, run); p.lLegX = lerp(-0.45f * wind, -1.0f * stride, run);
                    p.rootZ = track(t, 0, 0, .26f, 0.8f, .32f, -1.8f, .6f, -1.6f, 1, 0);
                }));

        // ---------- Ground Pound (leap, hover-charge, meteor slam) ----------
        out.add(AttackDef.of("ground_pound").range(0, 8).cd(160).weight(2).dur(36).weapon("none").noLos()
                .when((e, t) -> Combat.around(e, t.position(), 4).size() >= 2 || t.getMaxHealth() >= 40)
                .onStart((e, t, n) -> D(e).putBoolean("h5Gp", false))
                .onEnd((e, t, n) -> hover(e, false))
                .tick((e, t, n) -> {
                    if (n == 0) { jump(e, 0.95, 0.3); exhaust(e, new Vec3(0, -1, 0), 4); snd(e, SoundEvents.FIRECHARGE_USE, 1, 1.4f); }
                    if (n == 6) { hover(e, true); snd(e, SoundEvents.BEACON_ACTIVATE, 1, 1.4f); }
                    if (n > 6 && n < 16) {
                        hover(e, true); exhaust(e, new Vec3(0, -1, 0), 2);
                        Fx.ring(L(e), e.position().add(0, -0.2, 0), 1.6f - (n - 6) * 0.13f, THRUST_C, 1f, 14);
                        if (n % 3 == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.7f, 1f + (n - 6) * 0.08f);
                    }
                    if (n == 16) {
                        hover(e, false);
                        Vec3 to = t != null ? t.position().subtract(e.position()) : fwd(e);
                        Vec3 h = new Vec3(to.x, 0, to.z); h = h.lengthSqr() > 1e-4 ? h.normalize().scale(Math.min(0.8, h.length() * 0.3)) : Vec3.ZERO;
                        e.setDeltaMovement(h.x, -2.4, h.z); e.hurtMarked = true;
                        snd(e, SoundEvents.ELYTRA_FLYING, 0.6f, 2f);
                    }
                    if (n >= 17 && !D(e).getBoolean("h5Gp") && (e.onGround() || n >= 24)) {
                        D(e).putBoolean("h5Gp", true);
                        Vec3 c = e.position().add(0, 0.3, 0);
                        Combat.blast(e, c, 5f, 17f, 1.4f, null);
                        Fx.explosion(L(e), c, 2.5f);
                        for (int i = 0; i < 3; i++) Fx.ring(L(e), c.add(0, i * 0.15, 0), 1.5f + i * 1.4f, i == 1 ? THRUST_C : 0x8a7a60, 1.6f, 26 + i * 6);
                        Fx.burst(L(e), ParticleTypes.POOF, c, 30, 1.6, 0.08);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 1.5f, 0.8f); snd(e, SoundEvents.ANVIL_LAND, 1f, 0.5f);
                    }
                })
                .anim((p, t, a) -> {
                    float air = win(t, .15f, .2f) * (1 - win(t, .44f, .5f)), dive = win(t, .44f, .5f) * (1 - win(t, .55f, .58f));
                    float land = win(t, .55f, .6f) * (1 - win(t, .82f, 1));
                    // take-off crouch -> knees tucked hover, fist cocked high -> fist drives down -> one-knee landing
                    p.rArmX = track(t, 0, 0, .15f, -0.5f, .2f, -2.9f, .44f, -2.95f, .55f, -0.4f, .82f, -0.5f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .2f, -0.3f, .44f, -0.3f, .55f, 0.1f, 1, 0);
                    p.lArmX = track(t, 0, 0, .15f, 0.4f, .2f, -0.4f, .44f, -0.5f, .55f, 0.6f, .82f, 0.5f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .2f, -0.9f, .44f, -0.9f, .55f, -0.6f, .82f, -0.5f, 1, 0);
                    p.bodyX = track(t, 0, 0, .12f, 0.35f, .2f, 0.3f, .44f, 0.25f, .55f, 0.75f, .82f, 0.6f, 1, 0);
                    p.bodyY = 0.15f * air;
                    p.headX = track(t, 0, 0, .2f, 0.3f, .44f, 0.4f, .55f, -0.2f, .82f, -0.25f, 1, 0);
                    p.rLegX = track(t, 0, 0, .12f, -0.4f, .2f, -1.3f, .44f, -1.2f, .5f, -0.2f, .58f, 1.0f, .82f, 0.9f, 1, 0);
                    p.lLegX = track(t, 0, 0, .12f, -0.4f, .2f, -0.6f, .44f, -0.6f, .5f, 0.1f, .58f, -1.3f, .82f, -1.2f, 1, 0);
                    p.rootY = track(t, 0, 0, .12f, -1.4f, .18f, 0.2f, .5f, 0.2f, .58f, -3.2f, .82f, -2.8f, 1, 0);
                    p.bodyZ = 0.04f * (float) Math.sin(a * 3.7f) * air - 0.08f * dive + 0.05f * land;
                }));

        // ---------- Thruster dodge (lateral burst) + BR return fire ----------
        out.add(AttackDef.of("thruster_dodge").range(4, 18).cd(90).weight(2).dur(26).weapon("br75")
                .tick((e, t, n) -> {
                    if (n == 2) {
                        Vec3 f = fwd(e), left = new Vec3(f.z, 0, -f.x);
                        e.setDeltaMovement(left.x * 1.1, 0.12, left.z * 1.1); e.hurtMarked = true;
                        exhaust(e, left.scale(-1), 6); snd(e, SoundEvents.FIRECHARGE_USE, 1.1f, 1.5f); snd(e, SoundEvents.ELYTRA_FLYING, 0.4f, 2f);
                    }
                    if (n > 2 && n < 7) exhaust(e, Vec3.ZERO, 1);
                    if (n == 14 || n == 15 || n == 16) { bullet(e, t, 0.012, 3.3f, 0, 30); flash(e, TRACER); brass(e); if (n == 14) snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 1f, 1.4f); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .45f, .9f), side = bump(win(t, .05f, .45f)), r = kick(t, 14 / 26f, .2f);
                    // rifle swings to the hip while thrusting, body rolls into the dash, legs splay
                    p.rArmX = lerp(-0.7f, p.rArmX, 0); p.rArmY = -0.2f; p.lArmX = -0.9f; p.lArmY = 0.6f; p.rHandX = -0.4f;
                    p.rootRoll = -0.25f * side; p.bodyZ = -0.12f * side; p.headZ = 0.15f * side;
                    p.lArmZ = -0.4f * side; p.rArmZ = 0.1f * side;
                    p.lLegZ = -0.45f * side; p.rLegZ = 0.25f * side; p.rootY = -0.8f * side;
                    shoulder(p, k, 0, r * 1.3f); stance(p, k);
                }));

        // ---------- Clamber (vault up a ledge toward a higher target, then fire) ----------
        out.add(AttackDef.of("clamber_vault").range(0, 14).cd(70).weight(4).dur(28).weapon("br75").noLos()
                .when((e, t) -> t.getY() > e.getY() + 1.4)
                .tick((e, t, n) -> {
                    if (n == 3) { jump(e, 0.85, 0.35); exhaust(e, new Vec3(0, -1, 0), 3); snd(e, SoundEvents.ARMOR_EQUIP_IRON, 1, 0.7f); }
                    if (n == 9) { jump(e, 0.55, 0.3); snd(e, SoundEvents.STONE_STEP, 1.2f, 0.7f); Fx.burst(L(e), ParticleTypes.POOF, e.position().add(fwd(e).scale(0.6)).add(0, 1.8, 0), 6, 0.2, 0.02); }
                    if (n == 19 || n == 20 || n == 21) { bullet(e, t, 0.01, 3.3f, 0, 30); flash(e, TRACER); brass(e); if (n == 19) snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 1f, 1.4f); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .55f, .9f), r = kick(t, 19 / 28f, .18f);
                    // reach up -> haul up onto the ledge (arms press down, knees tuck) -> rise to aim
                    p.rArmX = track(t, 0, -0.6f, .1f, -0.9f, .2f, -2.9f, .32f, -2.6f, .42f, -0.3f, .55f, -0.8f, 1, -0.8f);
                    p.lArmX = track(t, 0, -0.85f, .1f, -1f, .2f, -2.9f, .32f, -2.6f, .42f, -0.3f, .55f, -0.9f, 1, -0.9f);
                    p.rArmZ = track(t, 0, 0, .2f, 0.2f, .42f, 0.3f, .55f, 0, 1, 0); p.lArmZ = -p.rArmZ;
                    p.rHandX = track(t, 0, -0.3f, .2f, 0.8f, .42f, 0.6f, .55f, -0.3f, 1, -0.3f);
                    p.bodyX = track(t, 0, 0, .1f, 0.2f, .2f, -0.15f, .32f, 0.1f, .42f, 0.55f, .55f, 0.1f, 1, 0);
                    p.headX = track(t, 0, 0, .1f, -0.4f, .32f, -0.35f, .45f, 0.1f, 1, 0);
                    p.rLegX = track(t, 0, 0, .1f, 0.3f, .2f, 0.4f, .32f, -1.0f, .42f, -1.4f, .55f, 0, 1, 0);
                    p.lLegX = track(t, 0, 0, .1f, -0.3f, .2f, 0.2f, .32f, -0.4f, .42f, -1.1f, .55f, 0, 1, 0);
                    p.rootY = track(t, 0, 0, .1f, -1.2f, .2f, 0.5f, .42f, 0.8f, .55f, -0.4f, 1, 0);
                    shoulder(p, k, 0.05f, r * 1.3f); stance(p, k);
                }));

        // ---------- Slide (sprint-slide into close range, SMG spray) ----------
        out.add(AttackDef.of("slide_smg").range(3, 9).cd(80).weight(2).dur(26).weapon("smg")
                .tick((e, t, n) -> {
                    if (n == 3) { Combat.lunge(e, 1.0, 0.02); snd(e, SoundEvents.GRAVEL_STEP, 1.2f, 0.6f); }
                    if (n > 3 && n < 15) {
                        Fx.burst(L(e), ParticleTypes.POOF, e.position().add(0, 0.1, 0), 2, 0.2, 0.01);
                        Fx.sparks(L(e), e.position().add(fwd(e).scale(0.4)), 1);
                        if (n % 3 == 0) snd(e, SoundEvents.GRINDSTONE_USE, 0.4f, 1.6f);
                    }
                    if (n >= 6 && n <= 20) { bullet(e, t, 0.07, 1.4f, 0, 16); if (n % 2 == 0) { flash(e, TRACER); brass(e); } snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.5f, 2f); }
                })
                .anim((p, t, a) -> {
                    float sl = win(t, .1f, .2f) * (1 - win(t, .6f, .8f)), k = ready(t, .2f, .88f);
                    float jit = 0.05f * (float) Math.sin(a * 6.1f) * win(t, .23f, .25f) * (1 - win(t, .77f, .8f));
                    hip(p, k, -0.15f, jit + 0.1f * k);
                    p.rootPitch = -0.35f * sl; p.bodyX += 0.4f * sl; p.headX += 0.25f * sl; // torso stays upright, legs out front
                    p.lLegX = lerp(p.lLegX, -1.35f, sl); p.rLegX = lerp(p.rLegX, -0.5f, sl); p.rLegZ += 0.25f * sl;
                    p.rootY -= 4.2f * sl; p.rootZ -= 1.2f * sl;
                    p.lArmZ -= 0.2f * sl; p.rHandZ += jit; p.bodyZ += jit * 0.5f;
                }));

        // ---------- BR75 battle rifle: hipfire double burst ----------
        out.add(AttackDef.of("br75_burst").range(4, 26).cd(24).weight(4).dur(22).weapon("br75")
                .tick((e, t, n) -> {
                    if (n == 5 || n == 13) snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 1.1f, 1.35f);
                    if ((n >= 5 && n <= 7) || (n >= 13 && n <= 15)) { bullet(e, t, 0.015, 3.2f, 0, 30); flash(e, TRACER); brass(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .18f, .88f), r = 1.2f * (kick(t, 5 / 22f, .16f) + kick(t, 13 / 22f, .16f));
                    shoulder(p, k, 0, r); stance(p, k);
                    p.rHandX -= r * 0.25f; p.bodyZ += 0.02f * r;
                }));

        // ---------- BR75 Smart-link (aimed down sights, precise headshot bursts) ----------
        out.add(AttackDef.of("br75_smartlink").range(10, 36).cd(60).weight(3).dur(36).weapon("br75")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.SPYGLASS_USE, 1, 1.2f);
                    if (n == 9 && t != null) { Fx.ring(L(e), head(t), 0.5f, 0x60c0ff, 0.7f, 12); snd(e, SoundEvents.NOTE_BLOCK_HAT.value(), 0.6f, 1.8f); }
                    if ((n >= 13 && n <= 15) || (n >= 22 && n <= 24)) {
                        Combat.hitscan(e, mz(e), aimHead(e, t, 0.003), 40, 4.6f, 0, TRACER); flash(e, TRACER); brass(e);
                        if (n == 13 || n == 22) snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 1.1f, 1.3f);
                    }
                    if (n == 29) snd(e, SoundEvents.SPYGLASS_STOP_USING, 1, 1.2f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), z = win(t, .1f, .3f) * (1 - win(t, .78f, .9f));
                    float r = 0.8f * (kick(t, 13 / 36f, .12f) + kick(t, 22 / 36f, .12f));
                    shoulder(p, k, 0, r); stance(p, k); smartlink(p, z);
                    p.rootY -= 0.4f * z; p.bodyZ += 0.008f * (float) Math.sin(a * 0.35f) * z;
                }));

        // ---------- DMR (semi-auto marksman, two measured shots) ----------
        out.add(AttackDef.of("dmr").range(9, 38).cd(40).weight(3).dur(28).weapon("dmr")
                .tick((e, t, n) -> {
                    if (n == 8 || n == 19) {
                        Combat.hitscan(e, mz(e), aimHead(e, t, 0.004), 42, 8.5f, 0, TRACER); flash(e, TRACER); brass(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.9f, 1.5f); snd(e, SoundEvents.CROSSBOW_SHOOT, 0.6f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .88f), z = win(t, .15f, .28f) * (1 - win(t, .8f, .9f));
                    float r = 1.5f * (kick(t, 8 / 28f, .2f) + kick(t, 19 / 28f, .2f));
                    shoulder(p, k, 0, r); stance(p, k); smartlink(p, z);
                    p.rHandX -= r * 0.35f; p.headX -= r * 0.05f;
                }));

        // ---------- DMR + Stabilize (thrust up, hover mid-air while aiming) ----------
        out.add(AttackDef.of("dmr_stabilize").range(8, 34).cd(180).weight(2).dur(42).weapon("dmr")
                .when((e, t) -> e.onGround())
                .onEnd((e, t, n) -> hover(e, false))
                .tick((e, t, n) -> {
                    if (n == 2) { jump(e, 0.8, 0); exhaust(e, new Vec3(0, -1, 0), 5); snd(e, SoundEvents.FIRECHARGE_USE, 1, 1.5f); }
                    if (n >= 9 && n < 36) {
                        hover(e, true);
                        if (n % 2 == 0) exhaust(e, new Vec3(0, -1, 0), 1);
                        if (n % 6 == 0) snd(e, SoundEvents.ELYTRA_FLYING, 0.25f, 2f);
                    }
                    if (n == 15 || n == 23 || n == 31) {
                        Combat.hitscan(e, mz(e), aimHead(e, t, 0.003), 42, 8.5f, 0, TRACER); flash(e, TRACER); brass(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.9f, 1.5f);
                    }
                    if (n == 36) { hover(e, false); snd(e, SoundEvents.BEACON_DEACTIVATE, 0.5f, 1.8f); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), air = win(t, .05f, .2f) * (1 - win(t, .85f, .95f));
                    float r = 1.4f * (kick(t, 15 / 42f, .14f) + kick(t, 23 / 42f, .14f) + kick(t, 31 / 42f, .14f));
                    shoulder(p, k, 0.1f * air, r); smartlink(p, win(t, .15f, .3f) * (1 - win(t, .85f, .95f)));
                    // legs hang loose, slight bob from the thrusters, pitching down toward the target
                    float bob = (float) Math.sin(a * 0.6f) * air;
                    p.rLegX = lerp(p.rLegX, 0.3f + 0.08f * bob, air); p.lLegX = lerp(p.lLegX, -0.25f - 0.08f * bob, air);
                    p.rLegZ += 0.12f * air; p.lLegZ -= 0.1f * air;
                    p.rootY += 0.3f * bob; p.headX += 0.15f * air; p.bodyX += 0.1f * air;
                    p.rootY -= 1.2f * bump(win(t, 0, .1f)) + 1.0f * bump(win(t, .9f, 1));
                }));

        // ---------- MA5D Assault Rifle (sustained auto fire) ----------
        out.add(AttackDef.of("assault_rifle").range(3, 20).cd(34).weight(3).dur(36).weapon("ar")
                .tick((e, t, n) -> {
                    if (n >= 5 && n <= 30) {
                        bullet(e, t, 0.045, 1.5f, 0, 24);
                        if (n % 2 == 0) { flash(e, TRACER); brass(e); }
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.6f + e.getRandom().nextFloat() * 0.25f);
                    }
                    if (n == 32) snd(e, SoundEvents.ARMOR_EQUIP_CHAIN, 0.8f, 1.2f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .9f), fire = win(t, .13f, .15f) * (1 - win(t, .84f, .86f));
                    float jit = (0.18f + 0.12f * (float) Math.sin(a * 2.9f)) * fire;
                    shoulder(p, k, 0, jit); stance(p, k);
                    p.rHandZ += 0.03f * (float) Math.sin(a * 5.3f) * fire; p.bodyZ += 0.015f * (float) Math.sin(a * 4.1f) * fire;
                    p.headX += 0.06f * fire;
                    p.lArmX += 0.3f * bump(win(t, .87f, 1)); // checks the ammo counter
                }));

        // ---------- M395 SMG (close-range bullet hose) ----------
        out.add(AttackDef.of("smg").range(1.5f, 12).cd(30).weight(3).dur(28).weapon("smg")
                .tick((e, t, n) -> {
                    if (n >= 3 && n <= 23) {
                        bullet(e, t, 0.07, 1.15f, 0, 16); bullet(e, t, 0.08, 0.6f, 0, 14);
                        if (n % 2 == 1) { flash(e, TRACER); brass(e); }
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.55f, 1.9f + e.getRandom().nextFloat() * 0.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .88f), fire = win(t, .1f, .12f) * (1 - win(t, .82f, .84f));
                    float jit = 0.06f * (float) Math.sin(a * 7.3f) * fire;
                    hip(p, k, -0.35f, 0.15f * fire + jit); stance(p, k);
                    p.bodyY -= 0.12f * k; p.headY += 0.1f * k;  // tucked compact
                    p.rArmZ += jit; p.lArmZ -= jit; p.rHandX += jit * 2;
                    p.rootZ -= 0.4f * fire;  // walks it in
                }));

        // ---------- M45D Tactical Shotgun ----------
        out.add(AttackDef.of("shotgun").range(0, 8).cd(30).weight(3).dur(26).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 6) {
                        for (int i = 0; i < 10; i++) bullet(e, t, 0.09, 2.6f, 0, 12);
                        flash(e, 0xffa040); flash(e, 0xffd080);
                        for (LivingEntity f : Combat.cone(e, 4, 35)) Combat.knock(f, e.position(), 1.0, 0.3);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 0.75f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.35f, 1.7f);
                    }
                    if (n == 13) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.7f);
                    if (n == 17) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.8f); brass(e); brass(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 2f * kick(t, 6 / 26f, .3f), pump = bump(win(t, .48f, .7f));
                    shoulder(p, k, 0.05f, r); stance(p, k);
                    p.lArmX += 0.4f * pump * k; p.lArmY -= 0.12f * pump; p.rHandX -= r * 0.4f;
                    p.bodyX -= r * 0.15f; p.rootY += r * 0.3f; p.bodyZ += 0.05f * pump; p.headX += 0.06f * pump;
                }));

        // ---------- M6H2 Magnum (sidearm, one-handed) ----------
        out.add(AttackDef.of("magnum").range(3, 22).cd(26).weight(2).dur(20).weapon("magnum")
                .tick((e, t, n) -> {
                    if (n == 5 || n == 12) {
                        Combat.hitscan(e, mz(e), aimHead(e, t, 0.008), 26, 5.5f, 0, TRACER); flash(e, TRACER);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.9f, 1.7f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f), r = 1.5f * (kick(t, 5 / 20f, .2f) + kick(t, 12 / 20f, .2f));
                    p.bodyY -= 0.4f * k; p.headY += 0.4f * k;
                    p.rArmX = lerp(p.rArmX, -HALF - r * 0.4f, k); p.rArmY = lerp(p.rArmY, 0.4f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
                    p.lArmX = lerp(p.lArmX, -0.3f, k); p.lArmZ = lerp(p.lArmZ, -0.35f, k); // free arm out for balance
                    p.rHandX -= r * 0.45f; p.bodyX -= r * 0.05f; p.rootZ += r * 0.4f; p.headX -= r * 0.04f;
                    p.lLegX = lerp(p.lLegX, 0.3f, k); p.rLegX = lerp(p.rLegX, -0.25f, k); p.rootY -= 0.3f * k;
                }));

        // ---------- M41 SPNKr rocket launcher (twin tube, both rockets) ----------
        out.add(AttackDef.of("spnkr_rocket").range(7, 32).cd(70).weight(2).dur(34).weapon("rocket")
                .tick((e, t, n) -> {
                    if (n == 8 || n == 20) {
                        Vec3 m = Combat.muzzle(e, 1.3, n == 8 ? 0.2 : 0.36, 0.1);
                        Combat.shoot(e, SPNKR, m, aim(e, t, 0.012), 1.3, 14f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.7, 0.3, 0.1), 8, 0.25, 0.05);
                        Fx.burst(L(e), ParticleTypes.FLAME, m, 4, 0.08, 0.04);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.6f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.3f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 2.2f * (kick(t, 8 / 34f, .3f) + kick(t, 20 / 34f, .3f));
                    // launcher rides high on the right shoulder, head tilted to the sight
                    shoulder(p, k, -0.12f, r); stance(p, k);
                    p.rArmX -= 0.1f * k; p.rArmZ += 0.1f * k; p.headZ -= 0.12f * k; p.headY += 0.08f * k;
                    p.rHandX -= r * 0.4f; p.bodyX -= r * 0.12f; p.rootZ += r * 0.4f;
                }));

        // ---------- Hydra multi-lock (paint target, salvo of six homing micro-rockets) ----------
        out.add(AttackDef.of("hydra").range(6, 30).cd(90).weight(2).dur(36).weapon("hydra")
                .tick((e, t, n) -> {
                    if ((n == 3 || n == 7 || n == 11) && t != null) {
                        Fx.ring(L(e), mid(t), 0.8f, 0xff3030, 1f, 14); Fx.ring(L(e), mid(t).add(0, 0.3, 0), 0.5f, 0xff3030, 1f, 10);
                        snd(e, SoundEvents.NOTE_BLOCK_PLING.value(), 0.6f, 1.4f + n * 0.04f);
                    }
                    if (n >= 14 && n <= 24 && n % 2 == 0) {
                        Vec3 m = Combat.muzzle(e, 1.1, 0.28 + ((n / 2) % 2) * 0.1, 0.05 + ((n / 2) % 3) * 0.05);
                        Vec3 d = aim(e, t, 0.12).add(0, 0.18, 0).normalize();
                        Combat.shoot(e, HYDRA, m, d, 0.95, 4.5f);
                        Fx.burst(L(e), ParticleTypes.SMOKE, m, 3, 0.05, 0.02);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.9f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .9f), lock = win(t, .05f, .35f) * (1 - win(t, .38f, .42f));
                    float fire = win(t, .38f, .4f) * (1 - win(t, .68f, .7f));
                    float r = 0.6f * Math.abs((float) Math.sin(t * 36 * PI / 2)) * fire;
                    shoulder(p, k, -0.06f, r); stance(p, k);
                    p.headX += 0.12f * lock; p.headY += 0.05f * (float) Math.sin(a * 0.8f) * lock; // sweeping to paint
                    p.bodyY += 0.06f * (float) Math.sin(a * 0.8f) * lock;
                    p.bodyZ += 0.03f * (float) Math.sin(a * 3) * fire;
                }));

        // ---------- Scattershot (Forerunner hard-light slugs that ricochet) ----------
        out.add(AttackDef.of("scattershot").range(0, 9).cd(40).weight(2).dur(26).weapon("scattershot")
                .tick((e, t, n) -> {
                    if (n < 6 && n % 2 == 0) { Fx.dustBurst(L(e), FORE_C, 0.8f, mz(e), 3, 0.15); snd(e, SoundEvents.BEACON_POWER_SELECT, 0.4f, 1.8f + n * 0.05f); }
                    if (n == 6) {
                        Vec3 m = mz(e);
                        for (int i = 0; i < 9; i++) Combat.hitscan(e, m, aim(e, t, 0.08), 12, 2.4f, 0, FORE_C);
                        // ricochets off the first victim into nearby foes
                        if (t != null) for (LivingEntity f : Combat.around(e, mid(t), 5)) {
                            if (f == t) continue;
                            Fx.beam(L(e), mid(t), mid(f), FORE_C, 0.6f, 0.4); Combat.damage(e, f, 3f); Fx.sparks(L(e), mid(f), 4);
                        }
                        Fx.dustBurst(L(e), FORE_C, 1.4f, m, 12, 0.2);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.2f, 1.2f); snd(e, SoundEvents.AMETHYST_BLOCK_BREAK, 1f, 0.7f);
                    }
                    if (n == 15) snd(e, SoundEvents.BEACON_DEACTIVATE, 0.5f, 1.6f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .18f, .9f), r = 2f * kick(t, 6 / 26f, .3f), vent = bump(win(t, .5f, .75f));
                    shoulder(p, k, 0, r); stance(p, k);
                    p.rHandX -= r * 0.4f; p.bodyX -= r * 0.12f;
                    p.rHandY += 0.35f * vent; p.lArmX += 0.15f * vent; p.headY += 0.1f * vent; // rolls the weapon to vent the hard-light core
                }));

        // ---------- ARC-920 Railgun (charge, heavy piercing slug) ----------
        out.add(AttackDef.of("railgun").range(8, 40).cd(110).weight(2).dur(38).weapon("railgun")
                .tick((e, t, n) -> {
                    if (n >= 4 && n < 22) {
                        Fx.dustBurst(L(e), RAIL_C, 0.8f, mz(e), 2, 0.12 - (n - 4) * 0.005);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mz(e), 1, 0.1, 0.05);
                        if (n % 4 == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.6f, 0.8f + (n - 4) * 0.06f);
                    }
                    if (n == 22) {
                        Vec3 d = aim(e, t, 0.002), m = mz(e);
                        Combat.hitscan(e, m, d, 44, 24f, 3, RAIL_C);
                        Fx.beam(L(e), m, endOf(e, t, d, 44), 0xffffff, 0.9f, 0.35);
                        Vec3 end = endOf(e, t, d, 30);
                        Combat.blast(e, end, 2.5f, 8f, 0.8f, t); Fx.ring(L(e), end, 1.5f, RAIL_C, 1.2f, 18);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, end, 20, 0.5, 0.3);
                        snd(e, SoundEvents.TRIDENT_THUNDER, 0.6f, 1.7f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 0.5f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .9f), ch = win(t, .1f, .58f) * (1 - win(t, .58f, .59f)), r = 2.8f * kick(t, 22 / 38f, .3f);
                    shoulder(p, k, 0, r); brace(p, k * 0.7f); smartlink(p, ch);
                    p.bodyZ += 0.04f * ch * ch * (float) Math.sin(a * 5.5f); p.rArmY += 0.03f * ch * (float) Math.sin(a * 6.3f);
                    p.rootZ += r * 1.1f; p.bodyX -= r * 0.15f; p.rHandX -= r * 0.3f; p.headX -= r * 0.06f;
                }));

        // ---------- Energy Sword: lunge (thrust-closing dash + cleave) ----------
        out.add(AttackDef.of("sword_lunge").range(3, 8).cd(60).weight(3).dur(24).weapon("sword")
                .tick((e, t, n) -> {
                    if (n == 0) snd(e, SoundEvents.BEACON_ACTIVATE, 1, 1.9f);
                    if (n == 6) { Combat.lunge(e, 1.3, 0.05); snd(e, SoundEvents.FIRECHARGE_USE, 1, 1.5f); }
                    if (n > 6 && n < 11) Fx.dustBurst(L(e), SWORD_C, 0.9f, Combat.muzzle(e, 1.4, 0.3, 0), 3, 0.1);
                    if (n == 11) {
                        Fx.arc(L(e), e.position().add(0, 1.2, 0), e.getYRot(), 2f, -70, 70, -15, SWORD_C, 1.2f, 18);
                        for (LivingEntity f : Combat.cone(e, 3.8, 65)) {
                            Combat.damage(e, f, 19); Combat.knock(f, e.position(), 0.7, 0.2);
                            Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(f), 10, 0.3, 0.2); f.setSecondsOnFire(2);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f); snd(e, SoundEvents.TRIDENT_RIPTIDE_1, 0.7f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    // blade cocked back over the right shoulder -> dash low -> wide horizontal cleave -> settle
                    p.rArmX = track(t, 0, -0.5f, .25f, -2.0f, .46f, -1.6f, .62f, -1.4f, 1, -0.5f);
                    p.rArmY = track(t, 0, 0, .25f, -0.9f, .38f, -0.9f, .46f, 0.9f, .62f, 1.0f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .25f, 0.5f, .46f, -0.2f, 1, 0);
                    p.rHandX = track(t, 0, 0, .25f, 0.5f, .46f, -0.2f, 1, 0); p.rHandZ = track(t, 0, 0, .3f, 0.5f, .46f, -0.4f, 1, 0);
                    p.lArmX = track(t, 0, 0, .25f, -1.1f, .46f, -0.3f, .62f, 0.3f, 1, 0); p.lArmZ = track(t, 0, 0, .25f, -0.2f, .46f, -0.6f, 1, 0);
                    p.bodyY = track(t, 0, 0, .25f, -0.6f, .4f, -0.6f, .46f, 0.6f, .62f, 0.65f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .25f, 0.15f, .4f, 0.45f, .55f, 0.3f, 1, 0);
                    p.rootY = track(t, 0, 0, .25f, -0.9f, .4f, -1.6f, .55f, -1.2f, 1, 0);
                    p.rootZ = track(t, 0, 0, .25f, 0.6f, .4f, -2.8f, .65f, -2.2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .25f, -0.3f, .4f, -0.9f, .7f, -0.6f, 1, 0); p.rLegX = track(t, 0, 0, .25f, 0.3f, .4f, 0.7f, .7f, 0.5f, 1, 0);
                }));

        // ---------- Energy Sword: two-hit combo (forehand + rising backhand) ----------
        out.add(AttackDef.of("sword_combo").range(0, 3.6f).cd(34).weight(3).dur(28).weapon("sword")
                .tick((e, t, n) -> {
                    if (n == 0) snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 2f);
                    if (n == 7 || n == 17) {
                        boolean back = n == 17;
                        Fx.arc(L(e), e.position().add(0, back ? 1.0 : 1.4, 0), e.getYRot(), 1.8f, back ? 60 : -70, back ? -70 : 60, back ? 35 : -25, SWORD_C, 1.1f, 16);
                        for (LivingEntity f : Combat.cone(e, 3.6, 70)) {
                            Combat.damage(e, f, back ? 12 : 10); Combat.knock(f, e.position(), back ? 0.8 : 0.4, back ? 0.45 : 0.1);
                            Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(f), 8, 0.3, 0.2);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.1f, back ? 0.9f : 1.1f); snd(e, SoundEvents.FIRECHARGE_USE, 0.4f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    // forehand right->left (hit .25), recoil coil, backhand rising left->right (hit .61)
                    p.rArmX = track(t, 0, -0.5f, .16f, -1.9f, .26f, -1.3f, .4f, -1.0f, .52f, -0.6f, .62f, -2.3f, .78f, -2.4f, 1, -0.5f);
                    p.rArmY = trackLin(t, 0, 0, .16f, -1.0f, .26f, 0.9f, .4f, 1.0f, .52f, 1.1f, .62f, -0.8f, .78f, -0.9f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .16f, 0.4f, .26f, -0.2f, .52f, -0.3f, .62f, 0.5f, 1, 0);
                    p.rHandZ = track(t, 0, 0, .16f, 0.6f, .26f, -0.6f, .52f, -0.7f, .62f, 0.6f, 1, 0);
                    p.lArmX = track(t, 0, 0, .16f, -0.9f, .26f, -0.2f, .52f, -0.9f, .62f, 0.2f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .26f, -0.5f, .52f, -0.2f, .62f, -0.6f, 1, 0);
                    p.bodyY = trackLin(t, 0, 0, .16f, -0.55f, .26f, 0.6f, .4f, 0.6f, .52f, 0.7f, .62f, -0.6f, .78f, -0.55f, 1, 0);
                    p.headY = -p.bodyY * 0.65f;
                    p.bodyX = track(t, 0, 0, .26f, 0.3f, .52f, 0.15f, .62f, -0.1f, .8f, 0, 1, 0);
                    p.rootZ = track(t, 0, 0, .26f, -1.6f, .52f, -1.0f, .62f, -2.2f, 1, 0);
                    p.rootY = -0.8f * bump(win(t, .1f, .45f)) - 0.4f * bump(win(t, .5f, .9f));
                    p.lLegX = track(t, 0, 0, .26f, -0.5f, .5f, -0.2f, .62f, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, .26f, 0.4f, .5f, 0.2f, .62f, 0.45f, 1, 0);
                }));

        // ---------- M9 frag grenade (left-hand overhand throw) ----------
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(150).weight(2).dur(22).weapons("br75", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.4f);
                    if (n == 11) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.4);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, t, 0.02).add(0, 0.15 + dist * 0.018, 0).normalize();
                        Combat.shoot(e, FRAG, from, d, 0.75 + dist * 0.025, 13f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .52f ? "frag" : null;
                    p.rArmX = -0.75f; p.rArmY = -0.3f; p.rHandX = -0.35f; // rifle tucked to the right
                    p.lArmX = track(t, 0, 0, .15f, -0.8f, .42f, -2.9f, .52f, -1.6f, .7f, -0.9f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .15f, -0.3f, .42f, -0.35f, .52f, 0, 1, 0);
                    p.bodyY = track(t, 0, 0, .42f, -0.5f, .55f, 0.4f, .75f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .42f, -0.25f, .55f, 0.35f, 1, 0); p.headX = track(t, 0, 0, .42f, -0.2f, .6f, 0.1f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.35f, .55f, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, .42f, 0.3f, .55f, 0.45f, 1, 0);
                    p.rootZ = track(t, .4f, 0, .55f, -1.4f, 1, 0); p.rootY = -0.5f * bump(win(t, .4f, .8f));
                }));

        // ---------- Plasma grenade (sticky, side-arm flick) ----------
        out.add(AttackDef.of("plasma_grenade").range(4, 16).cd(170).weight(2).dur(22).weapons("br75", "plasma_nade")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.9f);
                    if (n < 10) Fx.dustBurst(L(e), PLASMA_C, 0.7f, Combat.muzzle(e, 0.3, -0.45, 0.1), 1, 0.05);
                    if (n == 10) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.2);
                        double dist = t != null ? from.distanceTo(mid(t)) : 9;
                        Vec3 d = aim(e, t, 0.01).add(0, 0.08 + dist * 0.016, 0).normalize();
                        Combat.shoot(e, PLASMA_NADE, from, d, 0.8 + dist * 0.025, 15f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 1.2f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .47f ? "plasma_nade" : null;
                    p.rArmX = -0.75f; p.rArmY = -0.3f; p.rHandX = -0.35f;
                    // low side-arm wind-up behind the hip, whipped across the body
                    p.lArmX = track(t, 0, 0, .2f, 0.6f, .38f, 0.9f, .47f, -1.4f, .65f, -1.1f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .2f, -0.6f, .38f, -0.8f, .47f, -0.2f, .65f, 0.4f, 1, 0);
                    p.lArmY = track(t, 0, 0, .38f, 0.3f, .5f, -0.3f, 1, 0);
                    p.bodyY = track(t, 0, 0, .38f, -0.6f, .5f, 0.45f, .75f, 0.35f, 1, 0); p.headY = -p.bodyY * 0.8f;
                    p.bodyX = track(t, 0, 0, .38f, 0.05f, .5f, 0.3f, 1, 0); p.bodyZ = track(t, 0, 0, .38f, 0.12f, .5f, -0.1f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.3f, .5f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .38f, 0.25f, .5f, 0.4f, 1, 0);
                    p.rootZ = track(t, .35f, 0.4f, .5f, -1.2f, 1, 0); p.rootY = -0.6f * bump(win(t, .3f, .8f));
                }));
    }

    // ================= model: Mjolnir Mark VI GEN2 =================
    @Override
    public void buildModel(Rig rig) {
        Mat under = Mat.of(0x2a2d31).noise(.1f), olive = Mat.of(0x4e5b35).bevel(.32f).noise(.08f);
        Mat oliveD = Mat.of(0x3e4a2a).bevel(.25f).noise(.1f), steel = Mat.of(0x6f7479).noise(.06f).bevel(.25f), black = Mat.of(0x18191b);
        Mat thrust = Mat.of(THRUST_C).glow();
        Mat chest = Mat.of(0x4e5b35).bevel(.32f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            // GEN2 chest: angular centre ridge + two pectoral seams
            int cx = x + w / 2;
            g.rect(cx, y + 1, 1, h - 2, 0x3a4528);
            for (int i = 0; i < Math.min(3, w / 2 - 1); i++) { g.px(x + 1 + i, y + 2 + i, 0x3a4528); g.px(x + w - 2 - i, y + 2 + i, 0x3a4528); }
            g.rect(cx - 1, y + h - 2, 3, 1, 0x7a8086);
        });
        Mat helm = Mat.of(0x4e5b35).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) { g.rect(x + 1, y + h / 2, w - 2, 1, 0x30381f); g.rect(x + w / 2, y + 1, 1, h - 2, 0x30381f); }
            if (f == Mat.Face.TOP) g.rect(x + w / 2 - 1, y, 2, h, 0x3a4528);
        });
        Mat visor = Mat.of(VISOR).glow().decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            g.rectGlow(x, y, w, h, 0xe08a18);           // amber base
            g.rectGlow(x + 1, y, Math.max(1, w / 3), 1, 0xffe9a8); // specular streak
        });
        // ----- torso -----
        rig.box(rig.torso, under, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4.5f, -12, -3, 9, 7, 1, .3f);
        rig.box(rig.torso, oliveD, -3, -5, -2.7f, 6, 3, 1);           // abdomen plate
        rig.box(rig.torso, steel, -4.5f, -2.2f, -2.6f, 9, 2, 5);      // belt
        rig.box(rig.torso, black, -1, -1.9f, -3.2f, 2, 1, 1);         // buckle
        rig.box(rig.torso, oliveD, -4, -13, -2.6f, 8, 1, 5);          // collar
        rig.box(rig.torso, steel, -2, -13.5f, -1.5f, 4, 1, 3);        // neck seal
        // thruster pack
        rig.box(rig.torso, olive, -3.5f, -11.5f, 2, 7, 7, 2);
        rig.box(rig.torso, steel, -3, -4.5f, 2.5f, 6, 2, 2);
        rig.box(rig.torso, thrust, -3, -3, 3, 2, 1, 1);
        rig.box(rig.torso, thrust, 1, -3, 3, 2, 1, 1);
        rig.box(rig.torso, oliveD, -4.5f, -11, 2.2f, 1, 6, 1);
        rig.box(rig.torso, oliveD, 3.5f, -11, 2.2f, 1, 6, 1);
        // ----- helmet (GEN2: broad faceplate, deep chin guard, swept brow) -----
        rig.box(rig.head, helm, -4, -8, -4, 8, 8, 8, .45f);
        rig.box(rig.head, visor, -3.5f, -6, -5, 7, 3, 1);
        rig.box(rig.head, oliveD, -3, -3, -5.1f, 6, 3, 1);       // chin guard
        rig.box(rig.head, steel, -1, -2.5f, -5.6f, 2, 2, 1);     // breather
        rig.box(rig.head, olive, -4, -8.6f, -5, 8, 2, 2);        // brow
        rig.box(rig.head, oliveD, -1, -9, -3, 2, 1, 7);          // crest
        rig.box(rig.head, steel, -5, -5.5f, -2, 1, 3, 4);        // ear pods
        rig.box(rig.head, steel, 4, -5.5f, -2, 1, 3, 4);
        rig.box(rig.head, oliveD, -3.5f, -7, 3.6f, 7, 6, 1);     // rear plate
        // ----- arms: oversized layered pauldrons + gauntlets -----
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            float ox = s < 0 ? -3.6f : -2.4f;
            rig.box(arm, under, -2, -2, -2, 4, 12, 4);
            rig.box(arm, olive, ox, -3.2f, -3, 6, 4, 6);
            rig.box(arm, oliveD, ox + (s < 0 ? -0.4f : 0.4f), -1, -2.8f, 6, 2, 6, .1f);
            rig.box(arm, steel, -2, 3, -2, 4, 1, 4, .3f);           // elbow
            rig.box(arm, olive, -2, 5, -2, 4, 5, 4, .4f);           // gauntlet
            rig.box(arm, steel, s < 0 ? -2.8f : 1.8f, 6, -1, 1, 3, 2); // forearm module
        }
        // ----- legs -----
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, under, -2, 0, -2, 4, 12, 4);
            rig.box(leg, olive, -2.2f, 0.5f, -2.8f, 4, 5, 1, .2f);   // thigh plate
            rig.box(leg, oliveD, s < 0 ? -2.9f : 1.9f, 0.5f, -1.5f, 1, 4, 3); // hip plate
            rig.box(leg, steel, -1.5f, 5.5f, -3, 3, 2, 1);          // knee
            rig.box(leg, olive, -2, 7, -2, 4, 3, 4, .3f);           // shin
            rig.box(leg, oliveD, -2, 10, -3, 4, 2, 5, .25f);        // boot
        }
        buildWeapons(rig, steel, black, under);
    }

    private static void buildWeapons(Rig rig, Mat steel, Mat black, Mat under) {
        Mat gun = Mat.of(0x34383c).noise(.07f), gunL = Mat.of(0x7c8288).noise(.06f), unscOd = Mat.of(0x4a503c).noise(.07f);
        Mat fore = Mat.of(0xc9c6bb).bevel(.3f), foreO = Mat.of(FORE_C).glow(), cyan = Mat.of(0x60c8ff).glow(), red = Mat.of(0xff3a2a).glow();
        Mat sword = Mat.of(SWORD_C).glow(), hilt = Mat.of(0x3b3550).noise(.05f), plasma = Mat.of(PLASMA_C).glow(), lens = Mat.of(0x2f80d0).glow();
        PartDefinition w;
        // BR75: bullpup, top optic, long handguard
        w = rig.weapon(rig.rHand, "br75");
        rig.box(w, gun, -1.5f, -3, -8, 3, 4, 11); rig.box(w, steel, -0.5f, -2.2f, -13, 1, 1, 5);
        rig.box(w, black, -1, -5, -5, 2, 2, 5); rig.box(w, lens, -0.5f, -4.6f, -5.4f, 1, 1, 1);
        rig.box(w, unscOd, -1, 1, 0, 2, 3, 2); rig.box(w, black, -0.5f, 0, -1, 1, 3, 1);
        // DMR: long receiver, big scope, skeleton stock
        w = rig.weapon(rig.rHand, "dmr");
        rig.box(w, unscOd, -1.5f, -3, -10, 3, 3, 12); rig.box(w, steel, -0.5f, -2, -16, 1, 1, 6);
        rig.box(w, black, -1, -5, -7, 2, 2, 6); rig.box(w, lens, -0.5f, -4.6f, -7.4f, 1, 1, 1);
        rig.box(w, gun, -1, 0, -5, 2, 3, 2); rig.box(w, gun, -1, -2.5f, 2, 2, 2, 4);
        // MA5D assault rifle: chunky body, front mag, ammo counter
        w = rig.weapon(rig.rHand, "ar");
        rig.box(w, unscOd, -2, -3.5f, -9, 4, 5, 10); rig.box(w, gun, -1, -2.5f, -12, 2, 2, 3);
        rig.box(w, black, -1, 1.5f, -6, 2, 4, 2); rig.box(w, cyan, -0.5f, -4.4f, -3, 1, 1, 2);
        rig.box(w, gun, -1, -4.5f, -8, 2, 1, 4); rig.box(w, gun, -1, -2, 1, 2, 3, 3);
        // M395 SMG: compact, vertical mag, fore-grip
        w = rig.weapon(rig.rHand, "smg");
        rig.box(w, gun, -1, -3, -7, 2, 3, 8); rig.box(w, black, -0.5f, 0, -4, 1, 4, 1);
        rig.box(w, steel, -0.5f, -2, -9, 1, 1, 2); rig.box(w, black, -0.5f, 0, -7, 1, 2, 1);
        // M45D shotgun
        w = rig.weapon(rig.rHand, "shotgun");
        rig.box(w, gun, -1.5f, -3, -6, 3, 3, 9); rig.box(w, steel, -0.5f, -3, -14, 1, 1, 8);
        rig.box(w, black, -1, -2, -12, 2, 2, 4); rig.box(w, unscOd, -1, -2, 3, 2, 3, 4);
        // M6H2 magnum
        w = rig.weapon(rig.rHand, "magnum");
        rig.box(w, gunL, -1, -2.5f, -7, 2, 2, 7); rig.box(w, black, -1, -0.5f, -1, 2, 3, 2); rig.box(w, gun, -0.5f, -3, -3, 1, 1, 2);
        // M41 SPNKr: twin tubes, central sight, two grips
        w = rig.weapon(rig.rHand, "rocket");
        rig.box(w, unscOd, -2.5f, -4, -13, 2, 2, 17); rig.box(w, unscOd, 0.5f, -4, -13, 2, 2, 17);
        rig.box(w, gun, -2.5f, -4.5f, -14, 5, 3, 1); rig.box(w, black, -0.5f, -6, -4, 1, 2, 3);
        rig.box(w, black, -0.5f, -2, -1, 1, 3, 2); rig.box(w, red, -0.5f, -6.4f, -3.4f, 1, 1, 1);
        // Hydra: boxy pod launcher
        w = rig.weapon(rig.rHand, "hydra");
        rig.box(w, gun, -2, -4.5f, -9, 4, 5, 11); rig.box(w, Mat.of(0xc07020).noise(.08f), -1.5f, -4, -10, 3, 4, 1);
        rig.box(w, black, -1, 0.5f, -2, 2, 3, 2); rig.box(w, red, -0.5f, -5.4f, -4, 1, 1, 2);
        // Scattershot: Forerunner forked prongs, hard-light core
        w = rig.weapon(rig.rHand, "scattershot");
        rig.box(w, fore, -1.5f, -3.5f, -8, 3, 4, 10); rig.box(w, foreO, -0.5f, -3, -9, 1, 2, 7);
        rig.box(w, fore, -2.5f, -3, -13, 1, 2, 6); rig.box(w, fore, 1.5f, -3, -13, 1, 2, 6);
        rig.box(w, under, -0.5f, 0.5f, -1, 1, 2, 2);
        // ARC-920 railgun: long rail, coil rings
        w = rig.weapon(rig.rHand, "railgun");
        rig.box(w, unscOd, -1.5f, -3, -10, 3, 3, 14); rig.box(w, gunL, -1, -2.5f, -17, 2, 2, 7);
        rig.box(w, cyan, -2, -3.5f, -14, 4, 4, 1); rig.box(w, cyan, -2, -3.5f, -11, 4, 4, 1);
        rig.box(w, black, -0.5f, 0, -1, 1, 3, 2); rig.box(w, black, -0.5f, -5, -6, 1, 2, 3);
        // Type-1 energy sword: hilt + guard + twin hooked blades
        w = rig.weapon(rig.rHand, "sword");
        rig.box(w, hilt, -0.5f, -1, -2, 1, 2, 4); rig.box(w, hilt, -2.5f, -1, -3, 5, 2, 1);
        rig.box(w, sword, -2.5f, -0.5f, -14, 1, 1, 11); rig.box(w, sword, 1.5f, -0.5f, -14, 1, 1, 11);
        rig.box(w, sword, -1.5f, -0.5f, -15, 1, 1, 2); rig.box(w, sword, 0.5f, -0.5f, -15, 1, 1, 2);
        // grenades (left hand)
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, unscOd, -1, -1, -2, 2, 2, 2); rig.box(w, cyan, -0.5f, -1.8f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "plasma_nade");
        rig.box(w, plasma, -1, -1, -2, 2, 2, 2); rig.box(w, hilt, -1.5f, -0.5f, -1.5f, 3, 1, 1);
    }
}
