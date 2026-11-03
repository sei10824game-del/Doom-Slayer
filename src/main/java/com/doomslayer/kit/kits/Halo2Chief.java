package com.doomslayer.kit.kits;

import static com.doomslayer.anim.Keys.*;

import com.doomslayer.anim.Pose;
import com.doomslayer.client.Mat;
import com.doomslayer.client.Rig;
import com.doomslayer.entity.ProjStyle;
import com.doomslayer.entity.SlayerEntity;
import com.doomslayer.entity.SlayerProjectile;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Master Chief, Halo 2: Mark VI MJOLNIR, recharging energy shield, UNSC + Covenant arsenal, dual wielding, energy sword. */
public class Halo2Chief extends SlayerKit {
    static final int VISOR = 0xf0b030, SHIELD_C = 0xffd040, PP_G = 0x80ff40, PR_B = 0x5aa8ff, BPR_R = 0xff3a2a, NEEDLE_C = 0xff5ad8,
            BEAM_C = 0xff70c8, CARB_G = 0x40ff70, SPIKE_C = 0xff8a30, SENT_Y = 0xffe060, SWORD_C = 0x78d8ff, NADE_B = 0x50b8ff, BLOOD = 0x8a1010;
    static final float SHIELD = 12f;

    static final ProjStyle PP_BOLT = ProjStyle.make().color(PP_G).size(0.28f).life(45);
    static final ProjStyle PP_OVER = ProjStyle.make().color(PP_G).size(0.8f).life(70).homing(0.22f).trail(ParticleTypes.HAPPY_VILLAGER)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, PP_G, 2f, pos, 30, 0.6); Fx.ring(l, pos, 1.5f, PP_G, 1.4f, 20);
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 20, 0.4, 0.3);
                Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1.2f, 1.6f);
                if (hit instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3)); // EMP stun
            });
    static final ProjStyle PR_BOLT = ProjStyle.make().color(PR_B).size(0.24f).life(45).trail(ParticleTypes.ELECTRIC_SPARK);
    static final ProjStyle BPR_BOLT = ProjStyle.make().color(BPR_R).size(0.24f).life(40);
    static final ProjStyle NEEDLE = ProjStyle.make().color(NEEDLE_C).size(0.14f).life(60).homing(0.3f)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, NEEDLE_C, 0.8f, pos, 5, 0.1);
                if (!(hit instanceof LivingEntity le) || !(p.getOwner() instanceof SlayerEntity o)) return;
                CompoundTag d = le.getPersistentData();
                int n = le.tickCount - d.getInt("h2c_needleT") > 60 ? 1 : d.getInt("h2c_needles") + 1;
                d.putInt("h2c_needleT", le.tickCount);
                if (n >= 7) { // supercombine
                    n = 0;
                    Fx.explosion(l, pos, 2.5f); Fx.dustBurst(l, NEEDLE_C, 2f, pos, 40, 0.8); Fx.ring(l, pos, 2f, NEEDLE_C, 1.5f, 24);
                    Combat.blast(o, pos, 3f, 14f, 1f, null);
                    Fx.sound(l, pos, SoundEvents.AMETHYST_BLOCK_BREAK, 1.5f, 0.7f); Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1f, 1.4f);
                }
                d.putInt("h2c_needles", n);
            });
    static final ProjStyle CARBINE = ProjStyle.make().color(CARB_G).size(0.16f).life(30);
    static final ProjStyle SPIKE = ProjStyle.make().color(SPIKE_C).size(0.14f).life(40).gravity(0.015).trail(ParticleTypes.CRIT);
    static final ProjStyle BRUTE_GREN = ProjStyle.make().color(0x8a7a9a).size(0.3f).life(26).gravity(0.045).bounce().explode(2.8f).trail(ParticleTypes.SMOKE);
    static final ProjStyle FRAG = ProjStyle.make().color(0x5b6b3f).size(0.26f).life(36).gravity(0.05).bounce().explode(3.5f).trail(ParticleTypes.SMOKE);
    static final ProjStyle ROCKET = ProjStyle.make().color(0xffa040).size(0.45f).life(90).explode(3.8f).homing(0.1f).trail(ParticleTypes.LARGE_SMOKE);
    /** a plasma grenade stuck to a foe: follows it (aux = entity id) and blows when its fuse runs out */
    static final ProjStyle PLASMA_FUSE = ProjStyle.make().color(NADE_B).size(0.3f).life(24).pierce(99)
            .onTick(p -> {
                Entity t = p.level().getEntity(p.aux);
                if (t != null && t.isAlive()) p.setPos(t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ());
                p.setDeltaMovement(Vec3.ZERO);
                if (p.age2 % 2 == 0) Fx.dustBurst((ServerLevel) p.level(), NADE_B, 1.2f, p.position(), 3, 0.15);
                if (p.age2 % 6 == 0) Fx.sound((ServerLevel) p.level(), p.position(), SoundEvents.NOTE_BLOCK_HAT.value(), 0.6f, 1.6f + p.age2 * 0.02f);
            })
            .onHit((p, pos, hit) -> { if (hit == null) plasmaBlow(p, pos); });
    static final ProjStyle PLASMA_NADE = ProjStyle.make().color(NADE_B).size(0.3f).life(40).gravity(0.05).trail(ParticleTypes.ELECTRIC_SPARK)
            .onHit((p, pos, hit) -> {
                if (hit instanceof LivingEntity && p.getOwner() instanceof SlayerEntity o) {
                    SlayerProjectile f = Combat.shoot(o, PLASMA_FUSE, pos, new Vec3(0, 1, 0), 0.0001, 0);
                    f.aux = hit.getId();
                    Fx.sound((ServerLevel) p.level(), pos, SoundEvents.HONEY_BLOCK_PLACE, 1.2f, 1.4f);
                } else plasmaBlow(p, pos);
            });

    static void plasmaBlow(SlayerProjectile p, Vec3 pos) {
        ServerLevel l = (ServerLevel) p.level();
        if (p.getOwner() instanceof SlayerEntity o) Combat.blast(o, pos, 3.5f, 16f, 1.1f, null);
        Fx.explosion(l, pos, 2.5f); Fx.dustBurst(l, NADE_B, 2f, pos, 40, 0.9); Fx.ring(l, pos, 2.2f, 0x9ad8ff, 1.5f, 24);
        Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 20, 0.6, 0.3);
        Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1.2f, 1.5f); Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1f, 2f);
    }

    public Halo2Chief() {
        super("halo2_chief", "Master Chief (Mark VI)", "マスターチーフ (Mark VI)", "Halo 2");
        eggPrimary = 0x56633a; eggSecondary = VISOR;
        maxHealth = 70; speed = 0.31; armor = 14; knockbackRes = 0.7;
        scale = 1.12f; width = 0.7f; preferredRange = 10f; dmgMult = 1.05f;
        idleWeaponR = "br";
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
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) { return aim(e, mz(e), t, spread); }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static void flash(SlayerEntity e, Vec3 m, int rgb) {
        Fx.burst(L(e), ParticleTypes.FLAME, m, 2, 0.04, 0.02);
        Fx.dustBurst(L(e), rgb, 1.1f, m, 4, 0.08);
    }
    static void flash(SlayerEntity e, int rgb) { flash(e, mz(e), rgb); }
    static void shell(SlayerEntity e) { Fx.burst(L(e), ParticleTypes.SMOKE, Combat.muzzle(e, 0.4, 0.5, 0.1), 1, 0.03, 0.03); }
    static void bullet(SlayerEntity e, LivingEntity t, double spread, double range, float dmg, int pierce, int rgb) {
        Combat.hitscan(e, mz(e), aim(e, t, spread), range, dmg, pierce, rgb);
    }
    static Vec3 endOf(SlayerEntity e, LivingEntity t, Vec3 dir, double range) { return t != null ? mid(t) : mz(e).add(dir.scale(range)); }
    static void hold(SlayerEntity e, LivingEntity t, double dist, double lift) {
        if (t == null || !Combat.isFoe(e, t)) return;
        Vec3 f = Vec3.directionFromRotation(0, e.getYRot());
        Vec3 p = e.position().add(f.scale(dist + t.getBbWidth() * 0.5)).add(0, lift, 0);
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true;
    }
    static void slash(SlayerEntity e, double range, double half, float dmg, double kb) {
        for (LivingEntity f : Combat.cone(e, range, half)) {
            Combat.damage(e, f, dmg); Combat.knock(f, e.position(), kb, 0.25);
            Fx.dustBurst(L(e), BLOOD, 1.2f, mid(f), 8, 0.25); Fx.sparks(L(e), mid(f), 5);
        }
    }
    static void lob(SlayerEntity e, LivingEntity t, ProjStyle s, float dmg, double up) {
        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.4);
        double dist = t != null ? from.distanceTo(mid(t)) : 10;
        Vec3 d = aim(e, from, t, 0.02).add(0, up + dist * 0.018, 0).normalize();
        Combat.shoot(e, s, from, d, 0.75 + dist * 0.025, dmg);
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    static float kicks(float t, int dur, float len, int... ns) { float s = 0; for (int n : ns) s += kick(t, n / (float) dur, len); return s; }
    /** two-handed shouldered rifle, Chief's squared stance (only a slight blade) */
    static void rifle(Pose p, float k, float pitch, float recoil) {
        float tw = 0.18f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.12f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.05f - recoil * 0.22f, k); p.lArmY = lerp(p.lArmY, 0.45f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * k; p.bodyX += (recoil * 0.12f - pitch * 0.2f + 0.06f) * k; p.headX -= recoil * 0.08f * k;
        p.rootZ += recoil * 0.9f * k;
    }
    /** one-handed weapon (H2 magnum, SMG, plasma pistol/rifle, needler): right arm out, left arm guarding */
    static void oneHand(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.35f, k); p.rArmY = lerp(p.rArmY, 0.2f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -0.45f, k); p.lArmY = lerp(p.lArmY, 0.35f, k); p.lArmZ = lerp(p.lArmZ, -0.15f, k);
        p.bodyY -= 0.3f * k; p.headY += 0.3f * k; p.bodyX += 0.05f * k;
        p.rHandX -= recoil * 0.3f; p.rootZ += recoil * 0.5f * k;
    }
    /** dual wield: both arms forward, independent recoil */
    static void dual(Pose p, float k, float pitch, float rR, float rL) {
        p.rArmX = lerp(p.rArmX, -HALF + pitch - rR * 0.35f, k); p.rArmY = lerp(p.rArmY, -0.08f, k); p.rArmZ = lerp(p.rArmZ, 0.05f, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch - rL * 0.35f, k); p.lArmY = lerp(p.lArmY, 0.08f, k); p.lArmZ = lerp(p.lArmZ, -0.05f, k);
        p.rHandX -= rR * 0.3f; p.lHandX -= rL * 0.3f;
        p.bodyX += 0.1f * k; p.bodyY += (rL - rR) * 0.06f; p.headX -= 0.05f * k; p.rootZ += (rR + rL) * 0.4f * k;
    }
    /** heavy weapon at the hip (brute shot, sentinel beam) */
    static void hip(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -1.0f + pitch - recoil * 0.2f, k); p.rArmY = lerp(p.rArmY, -0.2f, k); p.rArmZ = lerp(p.rArmZ, 0.15f, k);
        p.lArmX = lerp(p.lArmX, -1.3f + pitch - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.7f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX = lerp(p.rHandX, -0.55f, k);
        p.bodyX += (0.12f + recoil * 0.1f) * k; p.bodyY += 0.15f * k; p.headY -= 0.15f * k; p.rootZ += recoil * 0.7f * k;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.25f, k);
        p.lLegZ -= 0.06f * k; p.rLegZ += 0.08f * k; p.rootY -= 0.4f * k;
    }
    static void wide(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.2f, k); p.rLegX = lerp(p.rLegX, 0.15f, k);
        p.lLegZ -= 0.3f * k; p.rLegZ += 0.3f * k; p.rootY -= 1.2f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // battle rifle at low ready across the chest
        p.rArmX = -0.6f + p.rArmX * 0.25f; p.rArmY = -0.25f;
        p.lArmX = -0.8f + p.lArmX * 0.25f; p.lArmY = 0.6f; p.lArmZ = 0.05f;
        p.rHandX = -0.3f; p.bodyX += 0.04f;
    }

    /** MJOLNIR energy shield: absorption pool that flares when hit, pops, and recharges after 5 s without damage. */
    @Override
    public void passive(SlayerEntity e) {
        CompoundTag d = e.getPersistentData();
        float sh = e.getAbsorptionAmount();
        Vec3 c = e.position().add(0, e.getBbHeight() * 0.55, 0);
        if (d.contains("h2c_tot") && e.getHealth() + sh < d.getFloat("h2c_tot") - 0.01f) {
            d.putInt("h2c_hit", e.tickCount);
            if (sh > 0) Fx.dustBurst(L(e), SHIELD_C, 1f, c, 8, 0.45);
            else if (d.getFloat("h2c_sh") > 0) {
                Fx.dustBurst(L(e), SHIELD_C, 1.4f, c, 30, 0.6); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, c, 16, 0.4, 0.2);
                snd(e, SoundEvents.SHIELD_BREAK, 1f, 1.3f);
            }
        }
        int since = e.tickCount - d.getInt("h2c_hit");
        if (since >= 100 && sh < SHIELD) {
            if (since == 100) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.8f);
            sh = Math.min(SHIELD, sh + 0.4f); e.setAbsorptionAmount(sh);
            if (e.tickCount % 3 == 0) Fx.ring(L(e), e.position().add(0, 0.2 + (e.tickCount % 12) * 0.15, 0), 0.6f, SHIELD_C, 0.8f, 10);
        }
        d.putFloat("h2c_tot", e.getHealth() + sh); d.putFloat("h2c_sh", sh);
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- Rifle bash (BR55 butt-stroke) ----------
        out.add(AttackDef.of("rifle_bash").range(0, 3.2f).cd(20).weight(3).dur(14).weapon("br")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 0.7f);
                    if (n == 6) {
                        for (LivingEntity f : Combat.cone(e, 3.4, 50)) {
                            Combat.damage(e, f, 9); Combat.knock(f, e.position(), 1.0, 0.3); Fx.sparks(L(e), mid(f), 6);
                        }
                        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.3f, -50, 40, -10, 0xd0d0c0, 0.7f, 9);
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.7f); snd(e, SoundEvents.ANVIL_LAND, 0.25f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    p.bodyY = track(t, 0, 0, .3f, -0.55f, .45f, 0.6f, .7f, 0.45f, 1, 0);
                    p.bodyX = track(t, 0, 0.05f, .3f, -0.1f, .45f, 0.3f, .7f, 0.2f, 1, 0.05f);
                    p.rArmX = track(t, 0, -1.2f, .3f, -0.9f, .45f, -1.75f, .7f, -1.6f, 1, -1.2f);
                    p.rArmY = track(t, 0, -0.2f, .3f, -0.6f, .45f, 0.35f, .7f, 0.3f, 1, -0.2f);
                    p.lArmX = track(t, 0, -1.3f, .3f, -1.0f, .45f, -1.8f, .7f, -1.6f, 1, -1.3f);
                    p.lArmY = track(t, 0, 0.5f, .45f, 0.9f, 1, 0.5f);
                    p.rHandX = track(t, 0, 0, .3f, 1.0f, .45f, 1.3f, .75f, 1.1f, 1, 0); // barrel tips up: stock leads
                    p.headY = -p.bodyY * 0.5f; p.headX = 0.15f * bump(win(t, .3f, .8f));
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, .75f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .45f, 0.4f, .75f, 0.35f, 1, 0);
                    p.rootZ = -2.5f * bump(win(t, .25f, .85f)); p.rootY -= 0.5f * bump(win(t, .2f, .8f));
                }));

        // ---------- Punch (unarmed: left jab, right hook) ----------
        out.add(AttackDef.of("punch").range(0, 3f).cd(18).weight(1).dur(18).weapon("none")
                .tick((e, t, n) -> {
                    if (n == 4 && t != null) { Combat.damage(e, t, 3); snd(e, SoundEvents.PLAYER_ATTACK_WEAK, 1, 1.1f); }
                    if (n == 10) {
                        for (LivingEntity f : Combat.cone(e, 3.2, 55)) { Combat.damage(e, f, 8); Combat.knock(f, e.position(), 0.9, 0.3); Fx.sparks(L(e), mid(f), 6); }
                        Fx.arc(L(e), e.position().add(0, 1.4, 0), e.getYRot(), 1.3f, 70, -20, 10, 0xffffff, 0.7f, 9);
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.lArmX = track(t, 0, -0.9f, .1f, -0.7f, .22f, -1.65f, .35f, -0.9f, 1, -0.9f); p.lArmY = 0.45f;
                    p.rArmX = track(t, 0, -0.9f, .35f, -0.4f, .55f, -1.55f, .75f, -1.4f, 1, -0.7f);
                    p.rArmZ = track(t, 0, 0.1f, .35f, 0.7f, .55f, -0.2f, .75f, -0.1f, 1, 0.1f);
                    p.rArmY = track(t, 0, 0, .35f, 0.2f, .55f, -0.55f, 1, 0);
                    p.bodyY = track(t, 0, 0, .12f, -0.2f, .22f, 0.35f, .4f, 0.5f, .55f, -0.65f, .75f, -0.45f, 1, 0);
                    p.bodyX = track(t, 0, 0, .55f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.lLegX = track(t, 0, 0, .2f, -0.4f, .8f, -0.4f, 1, 0); p.rLegX = track(t, 0, 0, .55f, 0.45f, .8f, 0.3f, 1, 0);
                    p.rootZ = -1.2f * bump(win(t, .1f, .35f)) - 2f * bump(win(t, .4f, .85f));
                    p.rootYaw = -0.12f * bump(win(t, .45f, .8f));
                }));

        // ---------- Assassination (low-health foe: grab, two stock blows to the back of the head) ----------
        out.add(AttackDef.of("assassination").range(0, 3.5f).lowHealthTarget(0.3f).weight(6).cd(80).dur(34).weapon("br")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.5, 0.05); snd(e, SoundEvents.ARMOR_EQUIP_IRON, 1, 0.6f); }
                    hold(e, t, 0.9, 0);
                    if (n == 12 && t != null) {
                        Combat.damage(e, t, Math.max(0, t.getHealth() - 1)); Fx.dustBurst(L(e), BLOOD, 1.3f, mid(t), 12, 0.3);
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.6f); snd(e, SoundEvents.ANVIL_LAND, 0.3f, 1.4f);
                    }
                    if (n == 24 && t != null) {
                        Fx.dustBurst(L(e), BLOOD, 1.5f, mid(t), 26, 0.4); Fx.sparks(L(e), mid(t), 10);
                        Combat.damage(e, t, t.getHealth() + 100);
                        snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 0.6f); snd(e, SoundEvents.BONE_BLOCK_BREAK, 1.2f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    // left hand clamps the victim's shoulder, right brings the BR stock down twice
                    p.lArmX = track(t, 0, -0.8f, .12f, -1.55f, .85f, -1.45f, 1, -0.8f); p.lArmY = track(t, 0, 0.5f, .12f, 0.25f, 1, 0.5f);
                    p.lArmZ = 0.05f * (float) Math.sin(a * 2.7f) * win(t, .1f, .85f);
                    p.rArmX = track(t, 0, -1.0f, .25f, -2.85f, .35f, -1.1f, .45f, -1.2f, .6f, -2.95f, .71f, -0.9f, .85f, -1.0f, 1, -0.9f);
                    p.rArmZ = track(t, 0, 0, .25f, -0.3f, .35f, 0.15f, .6f, -0.35f, .71f, 0.2f, 1, 0);
                    p.rHandX = track(t, 0, 0, .25f, 1.4f, .35f, 0.9f, .6f, 1.5f, .71f, 0.9f, 1, 0);
                    p.bodyX = track(t, 0, 0, .25f, -0.25f, .35f, 0.45f, .5f, 0.2f, .6f, -0.3f, .71f, 0.6f, .85f, 0.45f, 1, 0);
                    p.bodyY = track(t, 0, 0, .25f, -0.2f, .35f, 0.1f, .6f, -0.25f, .71f, 0.15f, 1, 0);
                    p.headX = track(t, 0, 0, .3f, 0.3f, .85f, 0.35f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.rootY = track(t, 0, 0, .25f, 0.5f, .35f, -0.6f, .6f, 0.6f, .71f, -1.3f, .9f, -0.8f, 1, 0);
                    p.rootZ = track(t, 0, 0, .12f, -2f, .9f, -2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .12f, -0.5f, .9f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .12f, 0.4f, .9f, 0.4f, 1, 0);
                }));

        // ---------- M6D Magnum (three precise, heavy shots) ----------
        out.add(AttackDef.of("m6d_magnum").range(3, 26).cd(28).weight(2).dur(26).weapon("magnum")
                .tick((e, t, n) -> {
                    if (n == 5 || n == 11 || n == 17) {
                        bullet(e, t, 0.006, 28, 6f, 0, 0xffe8a0); flash(e, 0xffc060); shell(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 1.1f, 1.2f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.15f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f), r = kicks(t, 26, .16f, 5, 11, 17);
                    oneHand(p, k, 0, r * 1.3f);
                    p.rHandX -= r * 0.3f; p.rArmZ -= 0.08f * k; // canted wrist, snaps up on each shot
                    p.headX += 0.1f * k; p.headY -= 0.1f * k; p.bodyX -= r * 0.05f;
                    p.lLegX = lerp(p.lLegX, 0.3f, k); p.rLegX = lerp(p.rLegX, -0.2f, k); p.rootY -= 0.3f * k;
                }));

        // ---------- MA5B Assault Rifle (full-auto spray, muzzle climb) ----------
        out.add(AttackDef.of("ma5b_assault_rifle").range(2, 18).cd(34).weight(3).dur(34).weapon("ma5b")
                .tick((e, t, n) -> {
                    if (n >= 4 && n < 28) {
                        bullet(e, t, 0.06, 20, 1.3f, 0, 0xffe080);
                        if (n % 2 == 0) { flash(e, 0xffb050); shell(e); }
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.6f, 1.6f + e.getRandom().nextFloat() * 0.25f);
                    }
                    if (n == 30) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.5f, 1.8f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), fire = win(t, .1f, .13f) * (1 - win(t, .8f, .83f));
                    float climb = 0.2f * win(t, .12f, .8f) * (1 - win(t, .8f, .9f));
                    float jit = 0.05f * (float) Math.sin(a * 6.1f) * fire;
                    rifle(p, k, -climb, 0.3f * fire + jit); stance(p, k);
                    p.bodyZ += jit * 0.5f; p.rHandZ += jit; p.headX += 0.06f * fire;
                    p.lArmX += 0.25f * bump(win(t, .84f, 1)); // slaps the mag at the end
                }));

        // ---------- BR55 Battle Rifle (three 3-round bursts) ----------
        out.add(AttackDef.of("br55_battle_rifle").range(4, 30).cd(30).weight(4).dur(32).weapon("br")
                .tick((e, t, n) -> {
                    if ((n >= 4 && n <= 6) || (n >= 13 && n <= 15) || (n >= 22 && n <= 24)) {
                        bullet(e, t, 0.008, 32, 2.6f, 0, 0xfff0b0); flash(e, 0xffc060);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, 1.4f + (n % 3) * 0.08f);
                        if (n % 9 == 4) shell(e);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), r = kicks(t, 32, .2f, 5, 14, 23);
                    rifle(p, k, 0, r * 1.1f); stance(p, k);
                    p.headX += 0.14f * k; p.headY -= 0.08f * k; // cheek weld on the scope
                    p.bodyY += 0.04f * (float) Math.sin(t * PI * 3) * k; // re-acquire between bursts
                    p.rHandX -= r * 0.2f;
                }));

        // ---------- M7 SMG (one-handed, wild climb) ----------
        out.add(AttackDef.of("m7_smg").range(1.5f, 14).cd(30).weight(2).dur(26).weapon("smg")
                .tick((e, t, n) -> {
                    if (n >= 3 && n < 21) {
                        bullet(e, t, 0.07, 16, 1.0f, 0, 0xffe080);
                        if (n % 2 == 1) flash(e, 0xffb050);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.5f, 1.9f + e.getRandom().nextFloat() * 0.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .88f), fire = win(t, .1f, .12f) * (1 - win(t, .8f, .82f));
                    float jit = 0.07f * (float) Math.sin(a * 7.3f) * fire;
                    oneHand(p, k, -0.25f * win(t, .12f, .8f) * fire, 0.3f * fire);
                    p.rArmZ += jit; p.rHandY += jit; p.bodyZ += jit * 0.3f;
                    p.lArmX = lerp(p.lArmX, -0.9f, fire); p.lArmZ -= 0.2f * fire; // left hand out for balance
                    stance(p, k);
                }));

        // ---------- Dual SMGs (alternating streams) ----------
        out.add(AttackDef.of("dual_smg").range(1.5f, 14).cd(45).weight(3).dur(30).weapons("smg", "smg")
                .tick((e, t, n) -> {
                    if (n >= 3 && n < 25) {
                        boolean right = n % 2 == 0; Vec3 m = right ? mz(e) : mzL(e);
                        Combat.hitscan(e, m, aim(e, m, t, 0.075), 16, 1.0f, 0, 0xffe080); flash(e, m, 0xffb050);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.5f, right ? 1.9f : 1.75f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .88f), fire = win(t, .1f, .12f) * (1 - win(t, .83f, .85f));
                    float rR = 0.3f * fire * (0.5f + 0.5f * (float) Math.sin(a * PI)), rL = 0.3f * fire * (0.5f - 0.5f * (float) Math.sin(a * PI));
                    dual(p, k, -0.2f * win(t, .1f, .8f) * fire, rR, rL);
                    p.rArmY -= 0.15f * (float) Math.sin(a * 0.8f) * fire; p.lArmY += 0.15f * (float) Math.sin(a * 0.8f) * fire; // hosing
                    wide(p, k * 0.6f); p.headX += 0.05f * fire;
                }));

        // ---------- M90 Shotgun ----------
        out.add(AttackDef.of("m90_shotgun").range(0, 8).cd(26).weight(3).dur(24).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 6) {
                        Vec3 m = mz(e);
                        for (int i = 0; i < 15; i++) Combat.hitscan(e, m, aim(e, t, 0.09), 14, 1.7f, 0, 0xffd070);
                        flash(e, 0xffa040); for (LivingEntity f : Combat.cone(e, 4, 35)) Combat.knock(f, e.position(), 1.0, 0.3);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.4f, 0.75f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.7f);
                    }
                    if (n == 13) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.7f);
                    if (n == 17) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.8f); shell(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 2f * kick(t, 6 / 24f, .3f), pump = bump(win(t, .52f, .74f));
                    rifle(p, k, -0.05f, r); stance(p, k);
                    p.rHandX -= r * 0.4f; p.bodyX -= r * 0.15f; p.rootY += r * 0.3f;
                    p.rLegX += 0.25f * r; // rear foot takes the blast
                    p.lArmX += 0.45f * pump * k; p.lArmY -= 0.1f * pump; p.bodyZ -= 0.05f * pump; p.headY += 0.1f * pump;
                }));

        // ---------- S2 AM Sniper Rifle (kneel, scope, two shots) ----------
        out.add(AttackDef.of("s2am_sniper").range(10, 48).cd(70).weight(2).dur(40).weapon("sniper")
                .tick((e, t, n) -> {
                    if (n == 6) snd(e, SoundEvents.SPYGLASS_USE, 1, 0.9f);
                    if (n == 16 || n == 30) {
                        Vec3 d = aim(e, t, 0.002), m = mz(e);
                        Combat.hitscan(e, m, d, 50, 16f, 2, 0xffffff); Fx.beam(L(e), m, endOf(e, t, d, 50), 0xe0e0ff, 0.4f, 0.4);
                        flash(e, 0xffd080); shell(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 0.6f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.3f, 1.5f);
                    }
                    if (n == 36) snd(e, SoundEvents.SPYGLASS_STOP_USING, 1, 0.9f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), kn = win(t, .02f, .2f) * (1 - win(t, .85f, 1)), r = 2f * kicks(t, 40, .18f, 16, 30);
                    rifle(p, k, 0, r);
                    p.rootY -= 2.6f * kn; p.lLegX = lerp(p.lLegX, -1.0f, kn); p.rLegX = lerp(p.rLegX, 0.7f, kn); // drop to a knee
                    p.headX += 0.22f * kn; p.headY -= 0.12f * kn; p.bodyX += 0.12f * kn;
                    p.bodyZ += 0.012f * (float) Math.sin(a * 0.35f) * kn; // breathing
                    p.rHandX -= r * 0.4f; p.headX -= r * 0.15f;
                }));

        // ---------- M19 SSM Rocket Launcher (shoulder, lock-on homing, two rockets) ----------
        out.add(AttackDef.of("m19_rocket_launcher").range(7, 32).cd(110).weight(2).dur(40).weapon("rocket")
                .tick((e, t, n) -> {
                    if (n == 3 && t != null) { Fx.ring(L(e), mid(t), 0.9f, 0xff3020, 1f, 16); snd(e, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.6f); }
                    if (n == 10 || n == 26) {
                        Combat.shoot(e, ROCKET, mz(e), aim(e, t, 0.01), 1.1, 15f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.7, 0.32, 0.1), 8, 0.25, 0.05); flash(e, 0xffa040);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.6f); snd(e, SoundEvents.GHAST_SHOOT, 0.4f, 1.3f);
                    }
                    if (n == 34) snd(e, SoundEvents.PISTON_EXTEND, 0.6f, 1.1f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), r = 2.4f * kicks(t, 40, .3f, 10, 26);
                    rifle(p, k, -0.04f, r); wide(p, k * 0.7f);
                    p.rArmY -= 0.1f * k; p.headZ -= 0.18f * k; p.headY -= 0.1f * k; // head tilted to the sight
                    p.bodyX -= r * 0.2f; p.rHandX -= r * 0.35f; p.rootZ += r * 0.5f;
                    p.lArmX += 0.3f * bump(win(t, .78f, .95f)); // racks the next tube
                }));

        // ---------- M9 Frag Grenade (left-hand overhand) ----------
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(140).weight(2).dur(22).weapons("br", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.4f);
                    if (n == 11) { lob(e, t, FRAG, 3f, 0.15); snd(e, SoundEvents.WITCH_THROW, 1, 0.8f); }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "frag" : null;
                    p.rArmX = -0.7f; p.rArmY = -0.3f; p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, 0, .15f, -0.8f, .4f, 0.9f, .5f, -2.4f, .7f, -1.4f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .4f, -0.5f, .5f, -0.1f, 1, 0);
                    p.bodyY = track(t, 0, 0, .4f, -0.55f, .55f, 0.45f, .75f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .4f, -0.2f, .55f, 0.3f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.35f, .55f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .4f, 0.3f, .55f, 0.45f, 1, 0);
                    p.rootZ = track(t, .4f, 0, .55f, -1.4f, 1, 0); p.rootY = -0.5f * bump(win(t, .4f, .8f));
                }));

        // ---------- Plasma Grenade (primed glow, wide sidearm sling, sticks to foes) ----------
        out.add(AttackDef.of("plasma_grenade").range(5, 20).cd(150).weight(2).dur(26).weapons("br", "pnade")
                .tick((e, t, n) -> {
                    if (n < 12) Fx.dustBurst(L(e), NADE_B, 0.8f, Combat.muzzle(e, 0.2, -0.5, 0.1), 2, 0.1);
                    if (n == 4) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 2f);
                    if (n == 13) { lob(e, t, PLASMA_NADE, 2f, 0.22); snd(e, SoundEvents.WITCH_THROW, 1, 1.1f); }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "pnade" : null;
                    p.rArmX = -0.55f; p.rArmY = -0.35f; p.rArmZ = 0.15f; p.rHandX = -0.35f;
                    p.lArmX = track(t, 0, 0, .2f, -0.4f, .42f, 0.4f, .5f, -1.9f, .62f, -2.2f, .8f, -1.0f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .2f, -0.6f, .42f, -1.2f, .5f, -0.5f, .62f, 0.2f, 1, 0);
                    p.lArmY = track(t, 0, 0, .42f, -0.3f, .62f, 0.5f, 1, 0);
                    p.bodyY = track(t, 0, 0, .42f, -0.75f, .58f, 0.6f, .8f, 0.35f, 1, 0); p.headY = -p.bodyY * 0.75f;
                    p.bodyZ = track(t, 0, 0, .42f, 0.12f, .58f, -0.1f, 1, 0);
                    p.bodyX = track(t, 0, 0, .42f, -0.15f, .58f, 0.35f, 1, 0);
                    p.lLegX = track(t, 0, 0, .35f, -0.2f, .5f, -0.6f, .85f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .42f, 0.2f, .58f, 0.55f, 1, 0);
                    p.rootY = track(t, .4f, 0, .5f, 0.8f, .62f, -0.6f, 1, 0); p.rootZ = track(t, .42f, 0, .6f, -1.8f, 1, 0); // hop into the throw
                }));

        // ---------- Plasma Pistol (semi-auto taps) ----------
        out.add(AttackDef.of("plasma_pistol").range(2, 20).cd(26).weight(2).dur(24).weapon("pp")
                .tick((e, t, n) -> {
                    if (n == 4 || n == 8 || n == 12 || n == 16) {
                        Combat.shoot(e, PP_BOLT, mz(e), aim(e, t, 0.02), 1.5, 2.6f);
                        Fx.dustBurst(L(e), PP_G, 1f, mz(e), 3, 0.06); snd(e, SoundEvents.BLAZE_SHOOT, 0.5f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), r = kicks(t, 24, .1f, 4, 8, 12, 16);
                    oneHand(p, k, 0.04f, r);
                    p.rHandY = 0.25f * k; // covenant grip, canted
                    p.lArmX = lerp(p.lArmX, -0.7f, k); p.lArmY = lerp(p.lArmY, 0.55f, k);
                    stance(p, k); p.headX += 0.06f * k;
                }));

        // ---------- Plasma Pistol Overcharge (charge, homing EMP blast) ----------
        out.add(AttackDef.of("plasma_pistol_overcharge").range(4, 22).cd(140).weight(2).dur(38).weapon("pp")
                .tick((e, t, n) -> {
                    if (n < 24) {
                        Fx.dustBurst(L(e), PP_G, 0.6f + n * 0.04f, mz(e), 2, 0.25 - n * 0.008);
                        if (n % 4 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.8f, 0.9f + n * 0.05f);
                    }
                    if (n == 26) {
                        Combat.shoot(e, PP_OVER, mz(e), aim(e, t, 0), 1.1, 9f);
                        Fx.dustBurst(L(e), PP_G, 1.8f, mz(e), 16, 0.3); snd(e, SoundEvents.BLAZE_SHOOT, 1.2f, 0.8f);
                        snd(e, SoundEvents.FIRE_EXTINGUISH, 0.6f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), ch = win(t, .05f, .65f) * (1 - win(t, .66f, .68f)), r = 2f * kick(t, 26 / 38f, .25f);
                    oneHand(p, k, -0.04f, r);
                    // left hand clamps the right wrist while the pistol fights back
                    p.lArmX = lerp(p.lArmX, -1.35f, ch); p.lArmY = lerp(p.lArmY, 0.6f, ch); p.lArmZ = lerp(p.lArmZ, 0, ch);
                    float sh = 0.05f * ch * ch;
                    p.rArmZ += sh * (float) Math.sin(a * 5.3f); p.rArmY += sh * (float) Math.cos(a * 4.7f); p.bodyZ += sh * 0.5f * (float) Math.sin(a * 4.1f);
                    p.bodyY += 0.2f * ch; p.headY -= 0.15f * ch; p.rootY -= 0.6f * ch; stance(p, k);
                    p.bodyX -= r * 0.1f; p.rootZ += r * 0.6f;
                }));

        // ---------- Plasma Rifle (one-handed stream, overheat vent) ----------
        out.add(AttackDef.of("plasma_rifle").range(3, 20).cd(32).weight(2).dur(30).weapon("pr")
                .tick((e, t, n) -> {
                    if (n >= 3 && n <= 21 && n % 2 == 1) {
                        Combat.shoot(e, PR_BOLT, mz(e), aim(e, t, 0.03), 1.7, 2.3f);
                        Fx.dustBurst(L(e), PR_B, 0.9f, mz(e), 2, 0.06); snd(e, SoundEvents.BLAZE_SHOOT, 0.45f, 1.9f);
                    }
                    if (n == 23) { snd(e, SoundEvents.FIRE_EXTINGUISH, 0.8f, 1.2f); Fx.burst(L(e), ParticleTypes.CLOUD, mz(e), 6, 0.15, 0.03); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .92f), fire = win(t, .08f, .1f) * (1 - win(t, .72f, .75f)), vent = bump(win(t, .76f, 1));
                    oneHand(p, k, 0.3f * vent, 0.3f * fire * (0.5f + 0.5f * (float) Math.sin(a * PI)));
                    p.rHandY += 1.2f * vent; p.rArmY += 0.3f * vent; p.headY += 0.2f * vent; // turns the vents away
                    p.rHandZ += 0.03f * (float) Math.sin(a * 3.3f) * fire; stance(p, k);
                }));

        // ---------- Dual Plasma Rifles ----------
        out.add(AttackDef.of("dual_plasma_rifles").range(3, 20).cd(48).weight(3).dur(34).weapons("pr", "pr")
                .tick((e, t, n) -> {
                    if (n >= 3 && n <= 25) {
                        boolean right = n % 2 == 0; Vec3 m = right ? mz(e) : mzL(e);
                        Combat.shoot(e, PR_BOLT, m, aim(e, m, t, 0.035), 1.7, 2.2f);
                        Fx.dustBurst(L(e), PR_B, 0.9f, m, 2, 0.06); snd(e, SoundEvents.BLAZE_SHOOT, 0.4f, right ? 1.95f : 1.8f);
                    }
                    if (n == 27) { snd(e, SoundEvents.FIRE_EXTINGUISH, 1f, 1.1f); Fx.burst(L(e), ParticleTypes.CLOUD, mz(e), 5, 0.15, 0.03); Fx.burst(L(e), ParticleTypes.CLOUD, mzL(e), 5, 0.15, 0.03); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .92f), fire = win(t, .08f, .1f) * (1 - win(t, .74f, .77f)), vent = bump(win(t, .78f, 1));
                    float s = (float) Math.sin(a * PI);
                    dual(p, k, 0.25f * vent, 0.3f * fire * (0.5f + 0.5f * s), 0.3f * fire * (0.5f - 0.5f * s));
                    p.rArmZ += 0.5f * vent; p.lArmZ -= 0.5f * vent; p.rHandY += vent; p.lHandY -= vent; // arms flare out to vent
                    p.headX -= 0.15f * vent; stance(p, k);
                }));

        // ---------- Brute Plasma Rifle (faster, hotter, red) ----------
        out.add(AttackDef.of("brute_plasma_rifle").range(3, 18).cd(34).weight(2).dur(26).weapon("bpr")
                .tick((e, t, n) -> {
                    if (n >= 3 && n < 20) {
                        Combat.shoot(e, BPR_BOLT, mz(e), aim(e, t, 0.045), 1.8, 1.7f);
                        Fx.dustBurst(L(e), BPR_R, 0.9f, mz(e), 2, 0.06); snd(e, SoundEvents.BLAZE_SHOOT, 0.4f, 1.6f + (n % 3) * 0.1f);
                    }
                    if (n == 21) { snd(e, SoundEvents.FIRE_EXTINGUISH, 0.8f, 0.9f); Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, mz(e), 4, 0.1, 0.03); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), fire = win(t, .1f, .12f) * (1 - win(t, .75f, .78f));
                    float jit = 0.06f * (float) Math.sin(a * 6.7f) * fire;
                    oneHand(p, k, 0, 0.25f * fire + jit);
                    p.bodyX += 0.15f * fire; p.headX += 0.1f * fire; p.rArmZ += jit; p.bodyZ += jit * 0.4f; // aggressive lean
                    p.lArmX = lerp(p.lArmX, -0.2f, fire); p.lArmZ -= 0.3f * fire;
                    stance(p, k); p.rootZ -= 0.5f * fire;
                }));

        // ---------- Needler (homing needles, supercombine at 7) ----------
        out.add(AttackDef.of("needler").range(3, 18).cd(40).weight(3).dur(30).weapon("needler")
                .tick((e, t, n) -> {
                    if (n >= 3 && n <= 23 && n % 2 == 1) {
                        Combat.shoot(e, NEEDLE, mz(e), aim(e, t, 0.06), 1.2, 1.6f);
                        Fx.dustBurst(L(e), NEEDLE_C, 0.7f, mz(e), 2, 0.05); snd(e, SoundEvents.AMETHYST_BLOCK_HIT, 0.8f, 1.6f + (n % 4) * 0.1f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), fire = win(t, .1f, .12f) * (1 - win(t, .78f, .8f));
                    oneHand(p, k, -0.05f * k, 0.18f * fire * Math.abs((float) Math.sin(a * PI * 0.5f)));
                    p.rHandY += 0.3f * k; p.rArmY -= 0.1f * (float) Math.sin(a * 0.6f) * fire; // sweeping the spray
                    p.lArmX = lerp(p.lArmX, -1.1f, k); p.lArmY = lerp(p.lArmY, 0.8f, k); // left forearm across the chest
                    p.headX += 0.05f * k; stance(p, k);
                }));

        // ---------- Beam Rifle (scoped particle beam) ----------
        out.add(AttackDef.of("beam_rifle").range(10, 48).cd(70).weight(2).dur(36).weapon("beam")
                .tick((e, t, n) -> {
                    if (n == 5) snd(e, SoundEvents.SPYGLASS_USE, 1, 1.2f);
                    if (n == 14 || n == 28) {
                        Vec3 d = aim(e, t, 0.001), m = mz(e);
                        Combat.hitscan(e, m, d, 50, 18f, 1, BEAM_C);
                        Fx.beam(L(e), m, endOf(e, t, d, 50), 0xffc0e8, 0.7f, 0.3);
                        snd(e, SoundEvents.TRIDENT_RIPTIDE_1, 1f, 1.8f); snd(e, SoundEvents.BEACON_DEACTIVATE, 0.7f, 2f);
                    }
                    if (n == 32) { snd(e, SoundEvents.FIRE_EXTINGUISH, 0.5f, 1.6f); Fx.dustBurst(L(e), BEAM_C, 0.7f, mz(e), 6, 0.2); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), zoom = win(t, .1f, .3f) * (1 - win(t, .82f, .92f)), r = 1.4f * kicks(t, 36, .15f, 14, 28);
                    rifle(p, k, 0, r); stance(p, k);
                    p.headX += 0.2f * zoom; p.headY -= 0.12f * zoom; p.bodyX += 0.1f * zoom; p.rootY -= 0.7f * zoom;
                    p.rArmY += 0.04f * (float) Math.sin(a * 0.5f) * zoom; // tracking
                }));

        // ---------- Brute Shot (bouncing grenade volley) ----------
        out.add(AttackDef.of("brute_shot").range(5, 22).cd(70).weight(2).dur(36).weapon("bruteshot")
                .tick((e, t, n) -> {
                    if (n == 6 || n == 12 || n == 18 || n == 24) {
                        Vec3 d = aim(e, t, 0.03).add(0, 0.12, 0).normalize();
                        Combat.shoot(e, BRUTE_GREN, mz(e), d, 1.0, 7f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, mz(e), 3, 0.1, 0.03); flash(e, 0xff9040);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.7f); snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.5f, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), r = 1.6f * kicks(t, 36, .14f, 6, 12, 18, 24);
                    hip(p, k, 0.05f, r); wide(p, k * 0.6f);
                    p.rootY += r * 0.4f; p.headX += r * 0.1f; p.bodyZ += 0.05f * r;
                }));

        // ---------- Brute Shot bayonet (rising blade cut) ----------
        out.add(AttackDef.of("brute_shot_blade").range(0, 3.4f).cd(30).weight(2).dur(18).weapon("bruteshot")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1, 0.6f);
                    if (n == 8) {
                        slash(e, 3.6, 60, 12, 0.8);
                        Fx.arc(L(e), e.position().add(0, 1.1, 0), e.getYRot(), 1.6f, -60, 60, 50, 0xc0c0d0, 0.9f, 12);
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -0.9f, .3f, -0.2f, .45f, -2.1f, .7f, -2.2f, 1, -0.9f);
                    p.lArmX = track(t, 0, -1.2f, .3f, -0.5f, .45f, -2.0f, .7f, -2.0f, 1, -1.2f); p.lArmY = 0.6f;
                    p.rArmZ = track(t, 0, 0, .3f, 0.5f, .45f, -0.2f, 1, 0); p.rHandX = track(t, 0, -0.4f, .3f, -0.9f, .45f, 0.4f, 1, -0.4f);
                    p.bodyY = track(t, 0, 0, .3f, 0.5f, .45f, -0.5f, .7f, -0.35f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.bodyX = track(t, 0, 0, .3f, 0.35f, .45f, -0.15f, 1, 0);
                    p.rootY = track(t, 0, 0, .3f, -1.2f, .45f, 0.5f, .7f, 0, 1, 0); p.rootZ = -1.8f * bump(win(t, .25f, .8f));
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, .8f, -0.45f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.35f, .8f, 0.3f, 1, 0);
                }));

        // ---------- Spiker (arcing hot spikes) ----------
        out.add(AttackDef.of("spiker").range(2, 16).cd(32).weight(2).dur(28).weapon("spiker")
                .tick((e, t, n) -> {
                    if (n >= 3 && n <= 21 && n % 2 == 1) {
                        Vec3 d = aim(e, t, 0.04).add(0, 0.03, 0).normalize();
                        Combat.shoot(e, SPIKE, mz(e), d, 1.6, 2.6f);
                        Fx.burst(L(e), ParticleTypes.SMALL_FLAME, mz(e), 2, 0.04, 0.02);
                        snd(e, SoundEvents.ARROW_SHOOT, 0.7f, 1.5f); snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.2f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), fire = win(t, .1f, .12f) * (1 - win(t, .76f, .78f));
                    float r = 0.35f * fire * Math.abs((float) Math.sin(a * PI * 0.5f));
                    oneHand(p, k, -0.06f, r);
                    p.rHandY -= 0.35f * k; p.rArmZ -= 0.1f * k; // spiker canted inward
                    p.lArmX = lerp(p.lArmX, -0.25f, k); p.lArmZ = lerp(p.lArmZ, -0.45f, k);
                    p.bodyZ += 0.03f * (float) Math.sin(a * 1.8f) * fire; stance(p, k);
                }));

        // ---------- Energy Sword lunge (crouch, leap, diagonal cut) ----------
        out.add(AttackDef.of("energy_sword_lunge").range(3, 9).cd(90).weight(3).dur(22).weapon("sword")
                .tick((e, t, n) -> {
                    if (n == 1) snd(e, SoundEvents.BEACON_ACTIVATE, 0.6f, 2f);
                    if (n == 4) {
                        if (t != null) {
                            Vec3 d = t.position().subtract(e.position()); d = new Vec3(d.x, 0, d.z);
                            double dist = Math.max(0, d.length() - 1.2);
                            if (d.lengthSqr() > 1e-4) { d = d.normalize().scale(Math.min(1.6, 0.25 + dist * 0.22)); e.setDeltaMovement(d.x, 0.15, d.z); e.hurtMarked = true; }
                        } else Combat.lunge(e, 1.0, 0.15);
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1, 1.4f);
                    }
                    if (n == 10) {
                        slash(e, 3.8, 70, 30, 1.2);
                        Fx.arc(L(e), e.position().add(0, 1.2, 0), e.getYRot(), 1.7f, 60, -70, -35, SWORD_C, 1.1f, 16);
                        snd(e, SoundEvents.TRIDENT_RIPTIDE_2, 1, 1.6f); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1, 1.2f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -0.6f, .18f, -2.7f, .3f, -2.9f, .46f, -0.8f, .7f, -0.5f, 1, -0.6f);
                    p.rArmZ = track(t, 0, 0.1f, .18f, -0.3f, .3f, -0.4f, .46f, 0.6f, .7f, 0.5f, 1, 0.1f);
                    p.rHandX = track(t, 0, 0, .18f, 0.6f, .46f, -0.4f, 1, 0);
                    p.lArmX = track(t, 0, -0.4f, .3f, -1.4f, .46f, -0.2f, 1, -0.4f); p.lArmZ = track(t, 0, 0, .3f, -0.3f, .46f, -0.8f, 1, 0);
                    p.bodyX = track(t, 0, 0, .15f, 0.45f, .3f, 0.1f, .46f, 0.65f, .7f, 0.5f, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, -0.4f, .46f, 0.5f, .7f, 0.4f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.headX = 0.25f * bump(win(t, .1f, .9f));
                    p.rootY = track(t, 0, 0, .15f, -2.0f, .25f, 1.6f, .4f, 0.8f, .5f, -1.2f, .8f, -0.8f, 1, 0); // crouch, leap, land
                    p.rootZ = track(t, 0, 0, .15f, 1f, .4f, -3f, .7f, -2.5f, 1, 0);
                    p.lLegX = track(t, 0, 0, .15f, -0.6f, .3f, -0.9f, .46f, -0.7f, .85f, -0.5f, 1, 0);
                    p.rLegX = track(t, 0, 0, .15f, 0.5f, .3f, 0.8f, .46f, 0.6f, .85f, 0.4f, 1, 0);
                }));

        // ---------- Energy Sword close combo (forehand sweep + backhand) ----------
        out.add(AttackDef.of("energy_sword_combo").range(0, 3.2f).cd(40).weight(3).dur(22).weapon("sword")
                .tick((e, t, n) -> {
                    if (n == 7) {
                        slash(e, 3.6, 80, 20, 0.9);
                        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.6f, 80, -80, 5, SWORD_C, 1f, 16);
                        snd(e, SoundEvents.TRIDENT_RIPTIDE_1, 1, 1.7f);
                    }
                    if (n == 14) {
                        slash(e, 3.4, 70, 12, 0.8);
                        Fx.arc(L(e), e.position().add(0, 1.0, 0), e.getYRot(), 1.5f, -70, 70, 20, 0xb0f0ff, 0.9f, 14);
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.1f, 1.5f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -0.7f, .2f, -1.6f, .32f, -1.5f, .5f, -1.3f, .64f, -1.5f, .8f, -1.2f, 1, -0.7f);
                    p.rArmY = track(t, 0, 0, .2f, 0.7f, .32f, -0.8f, .5f, -1.0f, .64f, 0.6f, .8f, 0.5f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .2f, 0.6f, .32f, -0.1f, .5f, -0.2f, .64f, 0.4f, 1, 0);
                    p.rHandY = track(t, 0, 0, .2f, -0.6f, .5f, 0.8f, .64f, -0.4f, 1, 0); // blade edge leads both ways
                    p.bodyY = track(t, 0, 0, .2f, -0.6f, .32f, 0.65f, .5f, 0.7f, .64f, -0.5f, .8f, -0.4f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.bodyX = track(t, 0, 0, .32f, 0.3f, .64f, 0.25f, 1, 0);
                    p.lArmX = track(t, 0, -0.4f, .32f, -0.9f, .64f, -0.3f, 1, -0.4f); p.lArmZ = track(t, 0, 0, .32f, -0.4f, .64f, -0.6f, 1, 0);
                    p.lLegX = track(t, 0, 0, .2f, -0.4f, .64f, -0.55f, .9f, -0.4f, 1, 0); p.rLegX = track(t, 0, 0, .32f, 0.4f, .9f, 0.35f, 1, 0);
                    p.rootZ = -1.5f * bump(win(t, .15f, .45f)) - 1.5f * bump(win(t, .5f, .85f));
                    p.rootYaw = track(t, 0, 0, .32f, 0.15f, .64f, -0.15f, 1, 0); p.rootY -= 0.6f * bump(win(t, .1f, .9f));
                }));

        // ---------- Covenant Carbine (rapid green rounds) ----------
        out.add(AttackDef.of("covenant_carbine").range(5, 30).cd(30).weight(2).dur(30).weapon("carbine")
                .tick((e, t, n) -> {
                    if (n == 4 || n == 9 || n == 14 || n == 19 || n == 24) {
                        Combat.shoot(e, CARBINE, mz(e), aim(e, t, 0.006), 3.0, 4.2f);
                        Fx.dustBurst(L(e), CARB_G, 1f, mz(e), 3, 0.06); snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 2f);
                        snd(e, SoundEvents.SLIME_SQUISH_SMALL, 0.6f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), r = kicks(t, 30, .1f, 4, 9, 14, 19, 24);
                    rifle(p, k, 0, r * 1.2f); stance(p, k);
                    p.headX += 0.12f * k; p.headY -= 0.1f * k;
                    p.rHandY += 0.06f * r; p.bodyZ -= 0.02f * r;
                }));

        // ---------- Sentinel Beam (continuous yellow beam, sweeping) ----------
        out.add(AttackDef.of("sentinel_beam").range(2, 16).cd(70).weight(2).dur(40).weapon("sentinel")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.BEACON_ACTIVATE, 0.9f, 1.8f);
                    if (n >= 6 && n < 34) {
                        Vec3 d = aim(e, t, 0.01), m = mz(e);
                        Combat.hitscan(e, m, d, 16, 1.3f, 0, SENT_Y);
                        Fx.beam(L(e), m, endOf(e, t, d, 16), 0xfff2a0, 0.6f, 0.35);
                        if (t != null) Fx.burst(L(e), ParticleTypes.SMALL_FLAME, mid(t), 1, 0.15, 0.02);
                        if (n % 5 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.9f, 1.6f);
                    }
                    if (n == 34) snd(e, SoundEvents.BEACON_DEACTIVATE, 0.9f, 1.6f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), fire = win(t, .14f, .16f) * (1 - win(t, .84f, .86f));
                    hip(p, k, 0.15f, 0.1f * fire);
                    float sweep = 0.12f * (float) Math.sin(a * 0.45f) * fire;
                    p.bodyY += sweep; p.headY -= sweep * 0.3f; p.rArmY += sweep * 0.5f; p.lArmY += sweep * 0.5f;
                    p.rHandZ += 0.02f * (float) Math.sin(a * 5f) * fire; // hum
                    wide(p, k * 0.5f); p.headX += 0.08f * fire;
                }));
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat armor = Mat.of(0x56633a).bevel(.3f).noise(.08f), armorD = Mat.of(0x434d2c).bevel(.25f).noise(.1f);
        Mat under = Mat.of(0x1e201c).noise(.12f), metal = Mat.of(0x6e716a).noise(.06f), dark = Mat.of(0x2c2f28).noise(.08f);
        Mat visor = Mat.of(VISOR).glow().decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            g.rectGlow(x, y, w, 1, 0xa05a10);            // dark upper band
            g.rectGlow(x + 1, y + h - 1, 2, 1, 0xfff0b0); // glint
        });
        Mat chest = Mat.of(0x56633a).bevel(.3f).noise(.06f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            int cx = x + w / 2;
            g.rect(cx, y, 1, h, 0x3a4326);                 // sternum seam
            g.rect(x + 1, y + h - 2, w - 2, 1, 0x3e4729);  // lower plate edge
            g.rect(x + 1, y + 1, 2, 1, 0x6e7c4a); g.rect(x + w - 3, y + 1, 2, 1, 0x6e7c4a);
        });
        Mat helm = Mat.of(0x56633a).bevel(.32f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) { g.rect(x + 2, y + 3, w - 4, 1, 0x343c22); g.rect(x + w / 2, y + 3, 1, h - 4, 0x343c22); }
            if (f == Mat.Face.TOP) g.rect(x + w / 2 - 1, y, 2, h, 0x3e4729);
        });
        // torso
        rig.box(rig.torso, under, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4.5f, -12, -3, 9, 6, 1, .2f);
        rig.box(rig.torso, armorD, -3.5f, -6, -2.6f, 7, 3, 1);
        rig.box(rig.torso, dark, -3, -13, -2.5f, 6, 1, 5);              // collar
        rig.box(rig.torso, metal, -4.5f, -2.5f, -2.5f, 9, 2, 5);        // belt
        rig.box(rig.torso, armorD, -1.5f, -1, -3, 3, 2, 1);             // codpiece
        rig.box(rig.torso, armorD, -3.5f, -11.5f, 2, 7, 8, 2);          // power pack
        rig.box(rig.torso, metal, -2.5f, -10, 4, 5, 2, 1);              // pack vents
        rig.box(rig.torso, Mat.of(SHIELD_C).glow(), -0.5f, -7, 4.1f, 1, 2, 1); // shield emitter light
        // helmet
        rig.box(rig.head, helm, -4, -8, -4, 8, 8, 8, .35f);
        rig.box(rig.head, visor, -3, -6, -4.7f, 6, 3, 1);
        rig.box(rig.head, dark, -2.5f, -3, -4.7f, 5, 2, 1);              // chin / breather
        rig.box(rig.head, metal, -4.8f, -6, -2, 1, 3, 4);                // ear pieces
        rig.box(rig.head, metal, 3.8f, -6, -2, 1, 3, 4);
        rig.box(rig.head, armorD, -1, -8.7f, -3, 2, 1, 6);               // crest
        rig.box(rig.head, dark, -3, -4, 3.6f, 6, 3, 1);                  // neck seal
        rig.box(rig.head, Mat.of(0xfff4c0).glow(), 3.6f, -7.5f, -3.2f, 1, 1, 1); // helmet lamp
        // arms
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, under, -2, -2, -2, 4, 12, 4);
            rig.box(arm, armor, s < 0 ? -3.5f : -2.5f, -3, -3, 6, 4, 6, .15f); // pauldron
            rig.box(arm, armorD, s < 0 ? -3f : 2f, 1, -2, 1, 2, 4);             // pauldron skirt
            rig.box(arm, metal, -1, 2.5f, -2.6f, 2, 2, 1);                     // elbow
            rig.box(arm, armor, -2, 4.5f, -2, 4, 5, 4, .3f);                   // forearm guard
            rig.box(arm, dark, -2, 9.5f, -2, 4, 1, 4, .35f);                   // glove cuff
        }
        // legs
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, under, -2, 0, -2, 4, 12, 4);
            rig.box(leg, armor, -2, 0.5f, -2.7f, 4, 5, 1);                     // thigh plate
            rig.box(leg, armorD, s < 0 ? -2.7f : 1.7f, 1, -1.5f, 1, 4, 3);     // outer thigh
            rig.box(leg, metal, -1.5f, 5.5f, -2.9f, 3, 2, 1);                  // knee
            rig.box(leg, armor, -2, 7, -2, 4, 3, 4, .25f);                     // shin
            rig.box(leg, dark, -2, 10, -3, 4, 2, 5, .2f);                      // boot
        }
        buildWeapons(rig, metal, dark);
    }

    private static void buildWeapons(Rig rig, Mat metal, Mat dark) {
        Mat unsc = Mat.of(0x3c3f3a).noise(.08f), unscL = Mat.of(0x5d6158).noise(.06f), black = Mat.of(0x161616);
        Mat cyan = Mat.of(0x60d0ff).glow(), red = Mat.of(0xff4030).glow();
        Mat cov = Mat.of(0x5a3a8a).bevel(.4f).noise(.05f), covT = Mat.of(0x3a8a8a).bevel(.4f).noise(.05f);
        Mat brute = Mat.of(0x4a4038).noise(.12f), bruteR = Mat.of(0x6a1e1a).bevel(.3f), forerunner = Mat.of(0xc8c8c0).bevel(.3f);
        PartDefinition w;
        // --- UNSC ---
        w = rig.weapon(rig.rHand, "br");
        rig.box(w, unsc, -1, -2.5f, -9, 2, 3, 11); rig.box(w, black, -0.5f, -4.5f, -6, 1, 2, 5);
        rig.box(w, metal, -0.5f, -2, -12, 1, 1, 3); rig.box(w, unscL, -1, 0.5f, -4, 2, 2, 2);
        w = rig.weapon(rig.rHand, "ma5b");
        rig.box(w, unsc, -1.5f, -3, -8, 3, 4, 10); rig.box(w, metal, -0.5f, -2, -12, 1, 1, 4);
        rig.box(w, cyan, -1, -3.6f, -3, 2, 1, 2); rig.box(w, black, -1, 1, -6, 2, 3, 2);
        w = rig.weapon(rig.rHand, "magnum");
        rig.box(w, unscL, -0.5f, -2, -5, 1, 2, 6); rig.box(w, black, -0.5f, 0, -1, 1, 3, 2); rig.box(w, black, -0.5f, -3, -3, 1, 1, 2);
        for (PartDefinition hand : new PartDefinition[]{rig.rHand, rig.lHand}) {
            w = rig.weapon(hand, "smg");
            rig.box(w, unsc, -1, -2, -6, 2, 3, 7); rig.box(w, black, -0.5f, 1, -5, 1, 3, 1); rig.box(w, metal, -0.5f, -1.5f, -8, 1, 1, 2);
        }
        w = rig.weapon(rig.rHand, "shotgun");
        rig.box(w, unsc, -1.5f, -2.5f, -7, 3, 3, 9); rig.box(w, metal, -1, -2.5f, -14, 2, 1, 7);
        rig.box(w, Mat.of(0x4a3a2a).noise(.15f), -1, -1.5f, -12, 2, 2, 4);
        w = rig.weapon(rig.rHand, "sniper");
        rig.box(w, unsc, -1, -2.5f, -8, 2, 3, 10); rig.box(w, metal, -0.5f, -2, -17, 1, 1, 9);
        rig.box(w, black, -0.5f, -4.5f, -6, 1, 2, 5); rig.box(w, unscL, -1, -2, 2, 2, 3, 3);
        w = rig.weapon(rig.rHand, "rocket");
        rig.box(w, Mat.of(0x4a4f40).noise(.08f), -1.5f, -4, -11, 3, 4, 14); rig.box(w, black, -2, -4.5f, -12, 4, 5, 1);
        rig.box(w, red, -0.5f, -5, -5, 1, 1, 2); rig.box(w, black, -0.5f, 0, -1, 1, 2, 2);
        // --- Covenant ---
        w = rig.weapon(rig.rHand, "pp");
        rig.box(w, covT, -1.5f, -2.5f, -5, 3, 3, 5); rig.box(w, Mat.of(PP_G).glow(), -1, -2, -6.5f, 2, 1, 2); rig.box(w, dark, -0.5f, 0.5f, -2, 1, 2, 2);
        for (PartDefinition hand : new PartDefinition[]{rig.rHand, rig.lHand}) {
            w = rig.weapon(hand, "pr");
            rig.box(w, cov, -1.5f, -2.5f, -7, 3, 3, 8); rig.box(w, cov, -2, -2, -10, 1, 2, 4); rig.box(w, cov, 1, -2, -10, 1, 2, 4);
            rig.box(w, Mat.of(PR_B).glow(), -0.5f, -3, -5, 1, 1, 3);
        }
        w = rig.weapon(rig.rHand, "bpr");
        rig.box(w, bruteR, -1.5f, -2.5f, -7, 3, 3, 8); rig.box(w, brute, -2, -2, -10, 4, 2, 3);
        rig.box(w, Mat.of(BPR_R).glow(), -0.5f, -3, -5, 1, 1, 3); rig.box(w, brute, -0.5f, -4, -4, 1, 1, 2);
        w = rig.weapon(rig.rHand, "needler");
        rig.box(w, Mat.of(0x6a2a6a).bevel(.4f), -1, -2, -6, 2, 3, 7); rig.box(w, Mat.of(NEEDLE_C).glow(), -1, -4, -5, 2, 2, 4);
        rig.box(w, dark, -0.5f, -1.5f, -8, 1, 2, 2);
        w = rig.weapon(rig.rHand, "beam");
        rig.box(w, cov, -1, -2, -13, 2, 2, 15); rig.box(w, Mat.of(BEAM_C).glow(), -0.5f, -3.5f, -5, 1, 1, 4);
        rig.box(w, cov, -1.5f, -2.5f, -15, 1, 1, 3); rig.box(w, cov, 0.5f, -2.5f, -15, 1, 1, 3);
        w = rig.weapon(rig.rHand, "carbine");
        rig.box(w, Mat.of(0x2a3a2a).bevel(.3f), -1, -2.5f, -9, 2, 3, 11); rig.box(w, Mat.of(CARB_G).glow(), -0.5f, -3.3f, -7, 1, 1, 6);
        rig.box(w, dark, -0.5f, -2, -12, 1, 1, 3);
        w = rig.weapon(rig.rHand, "sword");
        rig.box(w, metal, -1, -1, -2, 2, 2, 4); rig.box(w, Mat.of(SWORD_C).glow(), -1.5f, -0.5f, -13, 1, 1, 11);
        rig.box(w, Mat.of(SWORD_C).glow(), 0.5f, -0.5f, -13, 1, 1, 11);
        // --- Brute ---
        w = rig.weapon(rig.rHand, "bruteshot");
        rig.box(w, brute, -1.5f, -3, -8, 3, 4, 10); rig.box(w, metal, -0.5f, 1, -11, 1, 2, 8); rig.box(w, brute, -2, -2, -4, 4, 3, 3);
        w = rig.weapon(rig.rHand, "spiker");
        rig.box(w, brute, -1, -2, -6, 2, 3, 7); rig.box(w, metal, -0.5f, 1, -8, 1, 2, 8); rig.box(w, Mat.of(SPIKE_C).glow(), -1.5f, -2.5f, -9, 3, 1, 3);
        // --- Forerunner ---
        w = rig.weapon(rig.rHand, "sentinel");
        rig.box(w, forerunner, -1.5f, -2.5f, -10, 3, 3, 12); rig.box(w, Mat.of(SENT_Y).glow(), -1, -2, -12, 2, 2, 2);
        rig.box(w, metal, -2, -3, -4, 4, 4, 4);
        // --- grenades (left hand) ---
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, Mat.of(0x4a5a3a), -1, -1, -2, 2, 2, 2); rig.box(w, metal, -0.5f, -1.8f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "pnade");
        rig.box(w, Mat.of(NADE_B).glow(), -1, -1, -2, 2, 2, 2); rig.box(w, cov, -1.5f, -0.5f, -1.5f, 3, 1, 1);
    }
}
