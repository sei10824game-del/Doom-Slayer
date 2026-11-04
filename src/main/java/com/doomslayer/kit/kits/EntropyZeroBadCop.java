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

/** Bad Cop, Entropy Zero: rogue Civil Protection unit. Stunstick, full Combine/resistance arsenal, ZPG and Xen boost. */
public class EntropyZeroBadCop extends SlayerKit {
    static final int EZ_ORANGE = 0xff6a1a, STUN = 0x9fd8ff, PULSE = 0x7fd4ff, XEN = 0x7dff4a, LASER = 0xff2a2a;

    static final ProjStyle SMG_GRENADE = ProjStyle.make().color(0x4f5a3a).size(0.3f).life(60).explode(3.2f).gravity(0.045).trail(ParticleTypes.SMOKE);
    static final ProjStyle FRAG = ProjStyle.make().color(0x3d4436).size(0.28f).life(36).explode(3.5f).gravity(0.05).bounce().trail(Fx.dust(0xff2020, 0.7f));
    static final ProjStyle CROSSBOW_BOLT = ProjStyle.make().color(0xffa040).size(0.18f).life(70).gravity(0.008).pierce(2).trail(ParticleTypes.SMALL_FLAME);
    static final ProjStyle RPG_ROCKET = ProjStyle.make().color(0xffa030).size(0.45f).life(110).explode(3.6f).homing(0.22f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle ENERGY_BALL = ProjStyle.make().color(0xf0f8ff).size(0.75f).life(90).pierce(8).bounce().trail(ParticleTypes.END_ROD)
            .onTick(p -> {
                if (p.age2 % 2 == 0) Fx.dustBurst((ServerLevel) p.level(), 0xb0e8ff, 1.3f, p.position(), 3, 0.25);
                if (p.age2 % 8 == 0) Fx.sound((ServerLevel) p.level(), p.position(), SoundEvents.BEACON_AMBIENT, 0.6f, 2f);
            })
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                if (hit instanceof LivingEntity le && p.getOwner() instanceof SlayerEntity o) {
                    // dissolve: big bonus damage to anything not hulking
                    if (le.getMaxHealth() <= 60) Combat.damage(o, le, le.getHealth() + 20);
                    Fx.burst(l, ParticleTypes.END_ROD, pos, 20, 0.4, 0.1);
                    Fx.dustBurst(l, 0x202020, 1.4f, pos, 14, 0.4);
                    Fx.sound(l, pos, SoundEvents.FIRECHARGE_USE, 1f, 1.8f);
                } else {
                    Fx.ring(l, pos, 1.2f, 0xd0f0ff, 1.4f, 20);
                    Fx.burst(l, ParticleTypes.FLASH, pos, 1, 0, 0);
                    Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.9f);
                }
            });

    public EntropyZeroBadCop() {
        super("entropy_zero_bad_cop", "Bad Cop", "バッドコップ", "Entropy : Zero");
        eggPrimary = 0x1f252c; eggSecondary = EZ_ORANGE;
        maxHealth = 70; speed = 0.31; armor = 10; knockbackRes = 0.35;
        scale = 1.0f; width = 0.6f; preferredRange = 8f; dmgMult = 1f;
        idleWeaponR = "smg1";
    }

    // ================= helpers (server) =================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    /** Xen boost multiplier (kit dmgMult is shared, so the per-unit buff is applied here). */
    static float B(SlayerEntity e) { return e.hasEffect(MobEffects.DAMAGE_BOOST) ? 1.6f : 1f; }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.1, 0.3, 0); }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        return Combat.dirTo(mz(e), t, spread, new Random(e.getRandom().nextLong()));
    }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static Vec3 fwd(SlayerEntity e) { return Vec3.directionFromRotation(0, e.getYRot()); }
    static void flash(SlayerEntity e, int rgb) {
        Vec3 m = mz(e);
        Fx.burst(L(e), ParticleTypes.FLAME, m, 2, 0.04, 0.02);
        Fx.dustBurst(L(e), rgb, 1f, m, 3, 0.06);
    }
    static void shell(SlayerEntity e) { Fx.dustBurst(L(e), 0xd8b040, 0.5f, Combat.muzzle(e, 0.4, 0.55, 0.1), 1, 0.03); }
    static void bullet(SlayerEntity e, LivingEntity t, double spread, float dmg, int pierce, int rgb) {
        Combat.hitscan(e, mz(e), aim(e, t, spread), 26, dmg * B(e), pierce, rgb);
    }
    static Vec3 endOf(SlayerEntity e, LivingEntity t, Vec3 dir, double range) { return t != null ? mid(t) : mz(e).add(dir.scale(range)); }
    static void zap(ServerLevel l, Vec3 a, Vec3 b, int rgb) {
        Vec3 prev = a; int segs = 7;
        for (int i = 1; i <= segs; i++) {
            Vec3 q = a.lerp(b, i / (double) segs);
            if (i < segs) q = q.add((l.random.nextDouble() - .5) * .5, (l.random.nextDouble() - .5) * .5, (l.random.nextDouble() - .5) * .5);
            Fx.beam(l, prev, q, rgb, 0.6f, 0.2); prev = q;
        }
        Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, b, 10, 0.25, 0.3);
    }
    static Vec3 tip(SlayerEntity e) { return e.position().add(0, 1.3, 0).add(fwd(e).scale(1.4)); }
    /** stunstick contact: damage, sparks, small stun (slowness) */
    static void stunHit(SlayerEntity e, LivingEntity f, float dmg, double kb) {
        Combat.damage(e, f, dmg * B(e)); Combat.knock(f, e.position(), kb, 0.15);
        f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(f), 12, 0.25, 0.3);
        Fx.dustBurst(L(e), STUN, 0.9f, mid(f), 6, 0.3);
    }
    /** pin a ZPG-held foe at a point in front of the cop */
    static void hold(SlayerEntity e, LivingEntity t, Vec3 p) {
        if (t == null || !Combat.isFoe(e, t)) return;
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true; t.fallDistance = 0;
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** shouldered two-hander, bladed stance */
    static void rifle(Pose p, float k, float pitch, float recoil) {
        float tw = 0.22f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.15f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.05f - recoil * 0.22f, k); p.lArmY = lerp(p.lArmY, 0.45f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * k; p.bodyX += (recoil * 0.12f - pitch * 0.2f) * k; p.headX -= recoil * 0.08f * k;
        p.rootZ += recoil * 0.9f * k;
    }
    /** one-handed sidearm, cop "bladed" stance: chest turned, off hand loose */
    static void sidearm(Pose p, float k, float recoil, boolean twoHand) {
        p.bodyY -= 0.3f * k; p.headY += 0.3f * k;
        p.rArmX = lerp(p.rArmX, -HALF - recoil * 0.45f, k); p.rArmY = lerp(p.rArmY, 0.3f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        if (twoHand) { p.lArmX = lerp(p.lArmX, -HALF + 0.1f - recoil * 0.35f, k); p.lArmY = lerp(p.lArmY, 0.75f, k); }
        else { p.lArmX = lerp(p.lArmX, -0.2f, k); p.lArmZ = lerp(p.lArmZ, -0.25f, k); }
        p.rHandX -= recoil * 0.4f; p.bodyX -= recoil * 0.07f; p.headX -= recoil * 0.06f; p.rootZ += recoil * 0.5f;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.26f, k);
        p.lLegZ -= 0.06f * k; p.rLegZ += 0.08f * k; p.rootY -= 0.4f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // SMG held at low ready, muzzle down, patrol posture
        p.rArmX = -0.6f + p.rArmX * 0.25f; p.rArmY = -0.2f;
        p.lArmX = -0.85f + p.lArmX * 0.25f; p.lArmY = 0.6f;
        p.rHandX = -0.3f; p.headX += 0.03f * (float) Math.sin(age * 0.05f);
    }

    @Override
    public void passive(SlayerEntity e) {
        if (e.tickCount % 4 == 0 && e.hasEffect(MobEffects.DAMAGE_BOOST))
            Fx.dustBurst(L(e), XEN, 0.8f, e.position().add(0, 1.1, 0), 2, 0.35);
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        out.add(AttackDef.of("shiv").range(0, 2.8f).cd(34).weight(2).dur(24)
                .tick((e, t, n) -> {
                    if (n == 6 || n == 11 || n == 16) {
                        if (t != null) { Combat.damage(e, t, 4); Fx.dustBurst(KU.L(e), 0x8a0303, 1.2f, KU.mid(t), 8, 0.2); }
                        KU.snd(e, SoundEvents.TRIDENT_HIT, 0.7f, 1.4f + n * 0.02f);
                    }
                })
                .anim((p, t, a) -> KU.stab(p, t, 0.35f)));
        // ---------- Stunstick: horizontal swing (right-to-left backhand-free sweep) ----------
        out.add(AttackDef.of("stunstick_horizontal").range(0, 3.3f).cd(16).weight(3).dur(16).weapon("stunstick")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.4f, 2f);
                    if (n == 6) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1, 1.2f);
                    if (n == 7) {
                        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.5f, 70, -70, 0, STUN, 0.8f, 12);
                        for (LivingEntity f : Combat.cone(e, 3.4, 60)) stunHit(e, f, 6, 0.5);
                        snd(e, SoundEvents.TRIDENT_HIT, 0.8f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -0.6f, .3f, -1.4f, .44f, -1.5f, .7f, -1.1f, 1, -0.6f);
                    p.rArmY = trackLin(t, 0, 0, .3f, 0.95f, .44f, -0.75f, .6f, -0.9f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .3f, 0.45f, .44f, 0, 1, 0);
                    p.rHandZ = track(t, 0, 0, .3f, 0.5f, .44f, -0.4f, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, -0.5f, .44f, 0.55f, .65f, 0.6f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.lArmX = track(t, 0, -0.3f, .3f, -0.9f, .44f, -0.2f, 1, -0.2f); p.lArmZ = track(t, 0, 0, .3f, -0.3f, .5f, -0.5f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.4f, .8f, -0.4f, 1, 0); p.rLegX = track(t, 0, 0, .4f, 0.35f, .8f, 0.3f, 1, 0);
                    p.rootZ = -1.4f * bump(win(t, .3f, .8f)); p.rootY = -0.5f * bump(win(t, .25f, .85f));
                    p.bodyX = 0.18f * bump(win(t, .35f, .8f));
                }));

        // ---------- Stunstick: vertical overhead strike ----------
        out.add(AttackDef.of("stunstick_vertical").range(0, 3.3f).cd(22).weight(3).dur(18).weapon("stunstick")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.4f, 1.8f);
                    if (n == 8) {
                        Fx.arc(L(e), e.position().add(0, 1.0, 0), e.getYRot(), 1.4f, 0, 0, 90, STUN, 0.8f, 1);
                        Fx.beam(L(e), e.position().add(0, 2.4, 0).add(fwd(e).scale(0.6)), tip(e).add(0, -0.6, 0), STUN, 0.8f, 0.2);
                        for (LivingEntity f : Combat.cone(e, 3.3, 35)) stunHit(e, f, 9, 0.2);
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1, 0.9f); snd(e, SoundEvents.TRIDENT_HIT, 0.8f, 1.3f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = trackLin(t, 0, -0.6f, .35f, -2.9f, .44f, -0.7f, .6f, -0.55f, 1, -0.6f);
                    p.rArmZ = track(t, 0, 0, .35f, -0.15f, .44f, 0.05f, 1, 0);
                    p.rHandX = track(t, 0, 0, .35f, -0.6f, .44f, 0.35f, .7f, 0.2f, 1, 0);
                    p.lArmX = track(t, 0, -0.2f, .35f, -1.2f, .44f, 0.2f, 1, -0.2f); p.lArmZ = track(t, 0, 0, .35f, -0.25f, 1, 0);
                    p.bodyX = track(t, 0, 0, .35f, -0.3f, .44f, 0.45f, .7f, 0.35f, 1, 0);
                    p.bodyY = track(t, 0, 0, .35f, -0.2f, .44f, 0.15f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.headX = track(t, 0, 0, .35f, -0.25f, .44f, 0.25f, 1, 0);
                    p.rootY = track(t, 0, 0, .3f, 0.5f, .44f, -1.2f, .75f, -0.9f, 1, 0); p.rootZ = track(t, .3f, 0, .44f, -1.6f, .8f, -1.2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .44f, -0.55f, .8f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .44f, 0.4f, .8f, 0.35f, 1, 0);
                }));

        // ---------- Stunstick: electric charge (hold to overcharge, discharge slam + chain arcs) ----------
        out.add(AttackDef.of("stunstick_charge").range(0, 3.6f).cd(110).weight(2).dur(34).weapon("stunstick")
                .tick((e, t, n) -> {
                    if (n < 20) {
                        Vec3 s = Combat.muzzle(e, 0.5, 0.45, 0.2 + n * 0.02);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, s, 3, 0.12, 0.08);
                        if (n % 4 == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.6f, 0.9f + n * 0.05f);
                    }
                    if (n == 22) {
                        Vec3 c = tip(e).add(0, -0.8, 0);
                        Fx.ring(L(e), c, 1.6f, STUN, 1.3f, 24); Fx.ring(L(e), c, 2.8f, 0xffffff, 1f, 30);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, c, 40, 0.8, 0.5);
                        for (LivingEntity f : Combat.around(e, c, 3.2)) { stunHit(e, f, 14, 0.9); zap(L(e), c, mid(f), STUN); }
                        snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 1, 1.6f); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponR = t < .66f ? "stunstick_hot" : "stunstick";
                    float ch = win(t, .05f, .6f) * (1 - win(t, .62f, .64f)), shake = 0.06f * ch * (float) Math.sin(a * 5.3f);
                    p.rArmX = trackLin(t, 0, -0.6f, .18f, -2.5f, .62f, -2.7f, .66f, -0.6f, .85f, -0.5f, 1, -0.6f) + shake;
                    p.rArmZ = track(t, 0, 0, .18f, 0.35f, .62f, 0.4f, .66f, 0, 1, 0) + shake;
                    p.rHandX = track(t, 0, 0, .18f, -0.4f, .62f, -0.5f, .66f, 0.5f, 1, 0);
                    p.lArmX = track(t, 0, 0, .18f, -1.3f, .62f, -1.4f, .66f, -0.2f, 1, 0); p.lArmY = track(t, 0, 0, .18f, 0.4f, .62f, 0.4f, .66f, 0, 1, 0);
                    p.bodyX = track(t, 0, 0, .62f, -0.25f, .66f, 0.6f, .85f, 0.5f, 1, 0); p.bodyZ = shake * 0.6f;
                    p.headX = track(t, 0, 0, .62f, -0.2f, .66f, 0.35f, 1, 0);
                    p.rootY = track(t, 0, 0, .55f, -0.4f, .62f, 0.6f, .66f, -1.6f, .85f, -1.4f, 1, 0);
                    p.rootZ = track(t, .6f, 0, .66f, -1.8f, .9f, -1.6f, 1, 0);
                    p.lLegX = track(t, 0, 0, .2f, -0.3f, .66f, -0.7f, .9f, -0.6f, 1, 0); p.rLegX = track(t, 0, 0, .66f, 0.5f, .9f, 0.45f, 1, 0);
                    p.lLegZ = -0.1f * ch; p.rLegZ = 0.1f * ch;
                }));

        // ---------- Shove (two-handed shove + boot, opens distance) ----------
        out.add(AttackDef.of("shove").range(0, 2.6f).cd(40).weight(2).dur(16).weapon("smg1")
                .tick((e, t, n) -> {
                    if (n == 5) {
                        for (LivingEntity f : Combat.cone(e, 2.8, 50)) {
                            Combat.damage(e, f, 3 * B(e)); Combat.knock(f, e.position(), 1.4, 0.35);
                            f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
                            Fx.burst(L(e), ParticleTypes.POOF, mid(f), 6, 0.2, 0.05);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.8f); snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.6f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    // gun pulled tight to chest then rammed forward; right boot pushes
                    p.rArmX = track(t, 0, -0.6f, .2f, -0.5f, .31f, -1.4f, .55f, -1.3f, 1, -0.6f);
                    p.lArmX = track(t, 0, -0.85f, .2f, -0.6f, .31f, -1.5f, .55f, -1.4f, 1, -0.85f);
                    p.rArmY = -0.3f; p.lArmY = 0.6f; p.rHandX = track(t, 0, -0.3f, .2f, -1.2f, .31f, -0.9f, 1, -0.3f);
                    p.bodyX = track(t, 0, 0, .2f, -0.2f, .31f, 0.35f, .6f, 0.25f, 1, 0); p.headX = -p.bodyX * 0.4f;
                    p.bodyY = track(t, 0, 0, .2f, 0.25f, .31f, -0.15f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.rLegX = track(t, 0, 0, .2f, 0.35f, .31f, -1.2f, .5f, -1.0f, .75f, 0.1f, 1, 0);
                    p.lLegX = track(t, 0, 0, .31f, 0.2f, 1, 0);
                    p.rootZ = track(t, 0, 0, .2f, 0.8f, .31f, -1.5f, .7f, -0.6f, 1, 0); p.rootY = 0.4f * bump(win(t, .25f, .5f));
                }));

        // ---------- USP Match (rapid aimed taps) ----------
        out.add(AttackDef.of("usp").range(3, 20).cd(24).weight(2).dur(20).weapon("usp")
                .tick((e, t, n) -> {
                    if (n == 4 || n == 8 || n == 12) {
                        bullet(e, t, 0.025, 3.2f, 0, 0xffe080); flash(e, 0xffc060); shell(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f), r = kick(t, 4 / 20f, .12f) + kick(t, 8 / 20f, .12f) + kick(t, 12 / 20f, .14f);
                    sidearm(p, k, r, true); stance(p, k);
                    p.headX += 0.08f * k; // sights down
                }));

        // ---------- SMG1 (full-auto spray, muzzle climb) ----------
        out.add(AttackDef.of("smg1").range(2, 18).cd(28).weight(3).dur(30).weapon("smg1")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 24 && n % 2 == 0) {
                        bullet(e, t, 0.06, 2f, 0, 0xffe080); flash(e, 0xffb050); shell(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.6f, 1.7f + e.getRandom().nextFloat() * 0.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .12f, .14f) * (1 - win(t, .8f, .82f));
                    float r = 0.35f * Math.abs((float) Math.sin(t * 30 * PI / 2)) * fire, climb = 0.12f * win(t, .14f, .8f) * fire;
                    rifle(p, k, -climb, r); stance(p, k);
                    p.rHandZ += 0.04f * (float) Math.sin(a * 4.1f) * fire; p.bodyZ += 0.025f * (float) Math.sin(a * 3.7f) * fire;
                }));

        // ---------- SMG1 underbarrel grenade ----------
        out.add(AttackDef.of("smg1_grenade").range(6, 20).cd(140).weight(2).dur(24).weapon("smg1")
                .tick((e, t, n) -> {
                    if (n == 8) {
                        Vec3 from = Combat.muzzle(e, 1.0, 0.3, -0.15);
                        double dist = t != null ? from.distanceTo(mid(t)) : 12;
                        Combat.shoot(e, SMG_GRENADE, from, aim(e, t, 0.01).add(0, 0.12 + dist * 0.02, 0).normalize(), 0.8 + dist * 0.02, 13f * B(e));
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, from, 5, 0.1, 0.03);
                        snd(e, SoundEvents.FIREWORK_ROCKET_SHOOT, 1.2f, 0.6f); snd(e, SoundEvents.DISPENSER_LAUNCH, 1, 0.8f);
                    }
                    if (n == 16) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.6f, 1.6f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 2f * kick(t, 8 / 24f, .3f), lob = win(t, .1f, .3f) * (1 - win(t, .6f, .8f));
                    rifle(p, k, -0.25f * lob, r); stance(p, k);
                    p.bodyX -= 0.12f * lob; p.headX -= 0.1f * lob; p.rootZ += r * 0.4f;
                    float reload = bump(win(t, .55f, .8f)); p.lArmX += 0.7f * reload; p.lArmZ -= 0.2f * reload; // thumbs a new round in
                }));

        // ---------- SPAS-12 (single blast + pump) ----------
        out.add(AttackDef.of("spas12").range(1, 10).cd(26).weight(3).dur(22).weapon("spas12")
                .tick((e, t, n) -> {
                    if (n == 5) {
                        for (int i = 0; i < 7; i++) bullet(e, t, 0.08, 2.6f, 0, 0xffd070);
                        flash(e, 0xffa040); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.2f, 0.85f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.3f, 1.8f);
                    }
                    if (n == 11) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.8f);
                    if (n == 14) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.9f); shell(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = kick(t, 5 / 22f, .25f), pump = bump(win(t, .48f, .68f));
                    rifle(p, k, 0, r * 1.5f); stance(p, k);
                    p.lArmX += 0.4f * pump * k; p.lArmY -= 0.1f * pump; p.rHandX -= r * 0.35f; p.bodyZ += 0.05f * pump;
                }));

        // ---------- SPAS-12 double-barrel blast (alt fire) ----------
        out.add(AttackDef.of("spas12_double").range(0, 7).cd(70).weight(2).dur(30).weapon("spas12")
                .tick((e, t, n) -> {
                    if (n == 6) {
                        for (int i = 0; i < 13; i++) bullet(e, t, 0.11, 2.6f, 0, 0xffc060);
                        for (LivingEntity f : Combat.cone(e, 4, 40)) Combat.knock(f, e.position(), 1.1, 0.3);
                        flash(e, 0xffa040); flash(e, 0xff6020);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.7f, 1.5f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.4f, 0.6f);
                        e.setDeltaMovement(e.getDeltaMovement().subtract(fwd(e).scale(0.25))); e.hurtMarked = true;
                    }
                    if (n == 14) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.6f);
                    if (n == 18) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.7f); shell(e); shell(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .18f, .92f), r = 2.4f * kick(t, 6 / 30f, .3f), pump = bump(win(t, .45f, .65f));
                    rifle(p, k, 0.05f, r); stance(p, k);
                    p.rootY += r * 0.5f; p.bodyX -= r * 0.15f; p.rHandX -= r * 0.4f;
                    p.lArmX += 0.45f * pump * k; p.bodyZ += 0.06f * pump; p.headX += 0.1f * pump;
                    p.lLegX = lerp(p.lLegX, -0.5f, win(t, .18f, .24f) * (1 - win(t, .4f, .6f))); // braced step
                }));

        // ---------- .357 Magnum (heavy single shot, big muzzle flip) ----------
        out.add(AttackDef.of("magnum_357").range(4, 26).cd(38).weight(2).dur(24).weapon("magnum")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.CROSSBOW_LOADING_MIDDLE, 0.7f, 1.6f); // hammer cock
                    if (n == 8) {
                        bullet(e, t, 0.005, 13f, 1, 0xfff0c0); flash(e, 0xffd080); flash(e, 0xff8030);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 0.7f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.35f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .88f), r = 2.4f * kick(t, 8 / 24f, .35f);
                    sidearm(p, k, r, true); stance(p, k);
                    p.rHandX -= r * 0.4f; p.headX -= r * 0.1f; p.bodyX -= r * 0.08f;
                    p.lArmX -= 0.25f * bump(win(t, .05f, .3f)); // thumb on hammer
                }));

        // ---------- AR2 Pulse Rifle ----------
        out.add(AttackDef.of("ar2").range(3, 24).cd(30).weight(3).dur(28).weapon("ar2")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 22 && n % 3 == 1) {
                        bullet(e, t, 0.035, 3.2f, 0, PULSE);
                        Fx.dustBurst(L(e), PULSE, 1f, mz(e), 3, 0.06); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mz(e), 2, 0.05, 0.05);
                        snd(e, SoundEvents.BLAZE_SHOOT, 0.45f, 1.9f); snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.5f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .13f, .14f) * (1 - win(t, .8f, .82f));
                    float r = 0.4f * Math.abs((float) Math.sin(t * 28 * PI / 3)) * fire;
                    rifle(p, k, 0, r); stance(p, k);
                    p.bodyZ += 0.02f * (float) Math.sin(a * 2.5f) * fire;
                }));

        // ---------- AR2 alt-fire: combine energy ball ----------
        out.add(AttackDef.of("ar2_energy_ball").range(4, 26).cd(220).weight(2).dur(36).weapon("ar2")
                .when((e, t) -> Combat.around(e, t.position(), 5).size() >= 2 || t.getMaxHealth() <= 60)
                .tick((e, t, n) -> {
                    if (n < 20) {
                        Fx.dustBurst(L(e), 0xe8f8ff, 1f, mz(e), 2, 0.25 - n * 0.01);
                        if (n % 5 == 0) snd(e, SoundEvents.BEACON_ACTIVATE, 0.6f, 1.3f + n * 0.03f);
                    }
                    if (n == 20) {
                        Combat.shoot(e, ENERGY_BALL, mz(e), aim(e, t, 0), 0.9, 10f * B(e));
                        Fx.burst(L(e), ParticleTypes.FLASH, mz(e), 1, 0, 0); Fx.ring(L(e), mz(e), 0.8f, 0xd0f0ff, 1.2f, 16);
                        snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.7f, 1.9f); snd(e, SoundEvents.FIRECHARGE_USE, 1, 1.4f);
                        e.setDeltaMovement(e.getDeltaMovement().subtract(fwd(e).scale(0.3))); e.hurtMarked = true;
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), ch = win(t, .08f, .55f) * (1 - win(t, .55f, .57f)), r = 2.6f * kick(t, 20 / 36f, .3f);
                    rifle(p, k, 0.08f * ch, r); stance(p, k);
                    p.bodyZ += 0.04f * ch * ch * (float) Math.sin(a * 4.8f); p.rArmZ += 0.03f * ch * (float) Math.cos(a * 5.5f);
                    p.rootY -= 0.6f * ch; p.bodyX += 0.1f * ch; p.rootZ += r * 1.1f; p.rHandX -= r * 0.3f;
                    p.lLegX = lerp(p.lLegX, -0.5f, ch);
                }));

        // ---------- Crossbow (scoped, red-hot rebar bolt) ----------
        out.add(AttackDef.of("crossbow").range(10, 44).cd(70).weight(2).dur(40).weapon("crossbow")
                .tick((e, t, n) -> {
                    if (n == 5) snd(e, SoundEvents.SPYGLASS_USE, 1, 1);
                    if (n == 16) {
                        Combat.shoot(e, CROSSBOW_BOLT, mz(e), aim(e, t, 0), 3.2, 20f * B(e));
                        snd(e, SoundEvents.CROSSBOW_SHOOT, 1.3f, 0.8f); snd(e, SoundEvents.ARROW_SHOOT, 0.8f, 0.6f);
                    }
                    if (n == 22) snd(e, SoundEvents.SPYGLASS_STOP_USING, 1, 1);
                    if (n == 26) snd(e, SoundEvents.CROSSBOW_LOADING_START, 1, 0.9f);
                    if (n == 33) snd(e, SoundEvents.CROSSBOW_LOADING_END, 1, 0.9f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .92f), zoom = win(t, .08f, .3f) * (1 - win(t, .45f, .55f)), r = 1.6f * kick(t, 16 / 40f, .22f);
                    rifle(p, k, 0, r); stance(p, k);
                    p.headX += 0.2f * zoom; p.headY -= 0.12f * zoom; p.bodyX += 0.12f * zoom; p.rootY -= 0.6f * zoom;
                    p.bodyZ += 0.01f * (float) Math.sin(a * 0.4f) * zoom;
                    // reload: left hand pulls a new bolt from the back and drops it in
                    float load = bump(win(t, .6f, .88f));
                    p.lArmX += 1.0f * load * k; p.lArmZ -= 0.35f * load; p.lArmY -= 0.3f * load; p.headX += 0.2f * load; p.bodyY -= 0.15f * load;
                }));

        // ---------- RPG (laser-guided rocket) ----------
        out.add(AttackDef.of("rpg").range(7, 34).cd(160).weight(2).dur(40).weapon("rpg")
                .when((e, t) -> t.getMaxHealth() >= 40 || Combat.around(e, t.position(), 4).size() >= 2)
                .tick((e, t, n) -> {
                    if (n >= 3 && n < 30 && t != null) Fx.beam(L(e), Combat.muzzle(e, 1.0, 0.3, 0.15), mid(t), LASER, 0.4f, 1.2);
                    if (n == 3) snd(e, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 0.6f);
                    if (n == 10) {
                        Combat.shoot(e, RPG_ROCKET, mz(e), aim(e, t, 0.01), 0.9, 16f * B(e));
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.7, 0.3, 0), 8, 0.25, 0.05); flash(e, 0xffa040);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.55f); snd(e, SoundEvents.GHAST_SHOOT, 0.4f, 1.3f);
                    }
                    if (n == 32) snd(e, SoundEvents.PISTON_EXTEND, 0.6f, 1.1f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .92f), r = 2f * kick(t, 10 / 40f, .3f), guide = win(t, .25f, .35f) * (1 - win(t, .72f, .78f));
                    rifle(p, k, -0.04f, r); stance(p, k);
                    p.rHandX -= r * 0.4f; p.bodyX -= r * 0.1f; p.headX -= r * 0.08f;
                    p.headX += 0.12f * guide; p.headY += 0.05f * (float) Math.sin(a * 0.8f) * guide; // steering the laser
                    float rel = bump(win(t, .76f, .95f)); p.lArmX += 0.9f * rel; p.lArmZ -= 0.25f * rel; p.bodyY -= 0.1f * rel;
                }));

        // ---------- Frag grenade (overarm left-hand throw) ----------
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(150).weight(2).dur(22).weapons("smg1", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.4f);
                    if (n == 5) snd(e, SoundEvents.NOTE_BLOCK_HAT.value(), 0.6f, 2f);
                    if (n == 11) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.5);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Combat.shoot(e, FRAG, from, aim(e, t, 0.02).add(0, 0.15 + dist * 0.018, 0).normalize(), 0.75 + dist * 0.025, 14f * B(e));
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "frag" : null;
                    p.rArmX = -0.7f; p.rArmY = -0.3f; p.rHandX = -0.35f;
                    p.lArmX = track(t, 0, 0, .15f, -0.6f, .4f, -2.9f, .5f, -1.2f, .7f, -0.6f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .4f, -0.35f, .5f, 0.1f, 1, 0);
                    p.bodyY = track(t, 0, 0, .4f, -0.5f, .55f, 0.45f, .75f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .4f, -0.25f, .55f, 0.35f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.4f, .55f, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, .4f, 0.3f, .55f, 0.45f, 1, 0);
                    p.rootZ = track(t, .4f, 0, .55f, -1.4f, 1, 0); p.rootY = -0.5f * bump(win(t, .4f, .8f));
                }));

        // ---------- ZPG: pull -> hold -> throw ----------
        out.add(AttackDef.of("zpg_grab_throw").range(2, 14).cd(160).weight(2).dur(46).weapon("zpg")
                .when((e, t) -> t.getMaxHealth() <= 60)
                .tick((e, t, n) -> {
                    if (t == null) return;
                    Vec3 holdP = e.position().add(fwd(e).scale(1.8 + t.getBbWidth() * 0.5)).add(0, 0.6, 0);
                    if (n < 14) { // pull
                        Vec3 cur = t.position(), next = cur.lerp(holdP, 0.18 + n * 0.03);
                        hold(e, t, next);
                        Fx.beam(L(e), mz(e), mid(t), EZ_ORANGE, 0.7f, 0.4);
                        if (n % 3 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.9f, 1.4f + n * 0.03f);
                    } else if (n < 30) { // hold, levitating & spinning a little
                        hold(e, t, holdP.add(0, 0.12 * Math.sin(n * 0.6), 0));
                        Fx.dustBurst(L(e), EZ_ORANGE, 0.9f, mid(t), 3, 0.4);
                        if (n == 14) snd(e, SoundEvents.ITEM_PICKUP, 1, 0.5f);
                        if (n % 5 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.7f, 1.9f);
                    } else if (n == 31 && Combat.isFoe(e, t)) { // launch
                        Vec3 d = fwd(e).add(0, 0.25, 0).normalize();
                        t.setDeltaMovement(d.scale(2.4)); t.hurtMarked = true;
                        Combat.damage(e, t, 10 * B(e));
                        Fx.ring(L(e), mz(e), 1f, EZ_ORANGE, 1.4f, 18); Fx.burst(L(e), ParticleTypes.FLASH, mz(e), 1, 0, 0);
                        snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.6f); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f);
                    } else if (n == 40) { // thrown body slams into others
                        Combat.blast(e, mid(t), 2.8f, 9 * B(e), 1.0f, null);
                        Fx.burst(L(e), ParticleTypes.POOF, mid(t), 14, 0.5, 0.05); snd(e, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 1, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .92f), pull = win(t, .05f, .3f) * (1 - win(t, .3f, .35f)), held = win(t, .3f, .35f) * (1 - win(t, .66f, .68f));
                    float r = 2.6f * kick(t, 31 / 46f, .3f);
                    p.weaponR = held > 0.5f ? "zpg_hold" : "zpg";
                    rifle(p, k, -0.1f * held, r);
                    stance(p, k);
                    // pull: leaning back against the drag, shoulders straining
                    p.bodyX -= 0.25f * pull; p.rootZ += 1.2f * pull; p.headX -= 0.1f * pull;
                    p.rArmZ += 0.05f * pull * (float) Math.sin(a * 6); p.bodyZ += 0.04f * pull * (float) Math.sin(a * 5);
                    // hold: gun raised, weight settled, slight sway
                    p.bodyY += 0.1f * held * (float) Math.sin(a * 0.5f); p.rootY -= 0.4f * held;
                    // throw: step in, shove gun forward
                    float th = bump(win(t, .64f, .82f));
                    p.rootZ -= 2f * th; p.bodyX += 0.35f * th; p.lLegX -= 0.35f * th; p.rArmX -= 0.3f * th; p.lArmX -= 0.3f * th;
                    p.rootZ += r * 0.6f;
                }));

        // ---------- ZPG punt (primary blast for big targets) ----------
        out.add(AttackDef.of("zpg_punt").range(0, 6).cd(90).weight(2).dur(20).weapon("zpg")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.6f);
                    if (n == 6) {
                        for (LivingEntity f : Combat.cone(e, 6, 30)) {
                            Combat.damage(e, f, 7 * B(e)); Combat.knock(f, e.position(), 2.2, 0.45);
                            Fx.dustBurst(L(e), EZ_ORANGE, 1.2f, mid(f), 10, 0.3);
                        }
                        Vec3 m = mz(e);
                        for (int i = 1; i <= 4; i++) Fx.ring(L(e), m.add(fwd(e).scale(i * 1.1)), 0.3f + i * 0.25f, EZ_ORANGE, 1f, 12 + i * 3);
                        snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.8f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1, 0.5f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 2.2f * kick(t, 6 / 20f, .35f);
                    p.weaponR = t > .25f && t < .45f ? "zpg_hold" : "zpg";
                    rifle(p, k, 0, r); stance(p, k);
                    p.rootZ += r * 0.5f; p.bodyX -= r * 0.12f; p.rHandX -= r * 0.3f;
                    p.lLegX = lerp(p.lLegX, -0.55f, win(t, .1f, .28f) * (1 - win(t, .7f, 1)));
                }));

        // ---------- Xen boost (inject, dash forward, damage up) ----------
        out.add(AttackDef.of("xen_boost").range(4, 18).cd(600).weight(2).dur(22).weapon("smg1")
                .when((e, t) -> !e.hasEffect(MobEffects.DAMAGE_BOOST) && (e.getHealth() < e.getMaxHealth() * 0.7f || t.getMaxHealth() >= 40))
                .tick((e, t, n) -> {
                    if (n == 3) { snd(e, SoundEvents.BOTTLE_EMPTY, 1, 1.4f); snd(e, SoundEvents.PLAYER_BREATH, 1, 0.6f); }
                    if (n < 10) Fx.dustBurst(L(e), XEN, 1f, e.position().add(0, 1.2, 0), 3, 0.3);
                    if (n == 10) {
                        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
                        Combat.lunge(e, 1.3, 0.12);
                        Fx.ring(L(e), e.position().add(0, 0.2, 0), 1.2f, XEN, 1.4f, 20);
                        Fx.burst(L(e), ParticleTypes.HAPPY_VILLAGER, e.position().add(0, 1, 0), 18, 0.5, 0.1);
                        snd(e, SoundEvents.PORTAL_TRIGGER, 0.5f, 2f); snd(e, SoundEvents.ENDER_DRAGON_FLAP, 1, 1.5f);
                    }
                    if (n > 10 && n < 18) Fx.dustBurst(L(e), XEN, 1.2f, e.position().add(0, 0.9, 0), 4, 0.25);
                })
                .anim((p, t, a) -> {
                    // left hand jabs the injector into the neck, head snaps, then sprint-dash
                    float inj = win(t, .05f, .2f) * (1 - win(t, .42f, .5f)), dash = win(t, .45f, .5f) * (1 - win(t, .85f, 1));
                    p.lArmX = lerp(-0.2f, -2.4f, inj); p.lArmY = lerp(0, 0.9f, inj); p.lArmZ = lerp(0, 0.4f, inj);
                    p.headZ = -0.3f * inj; p.headX = -0.35f * bump(win(t, .2f, .45f)) + 0.2f * dash;
                    p.bodyX = 0.15f * bump(win(t, .2f, .45f)) * (float) Math.sin(a * 4) + 0.5f * dash;
                    p.rArmX = lerp(-0.6f, -1.1f, dash); p.rArmY = -0.3f; p.rHandX = -0.4f;
                    p.lArmX += 0.9f * dash;
                    p.rLegX = -1.0f * dash; p.lLegX = 0.8f * dash;
                    p.rootY = -0.6f * bump(win(t, .3f, .5f)) + 0.8f * dash; p.rootZ = -1.5f * dash;
                }));
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat uni = Mat.of(0x2a3038).noise(.1f), vest = Mat.of(0x1a1d22).bevel(.25f).noise(.08f), strap = Mat.of(0x14161a);
        Mat mask = Mat.of(0x8d9396).bevel(.3f).noise(.06f), maskD = Mat.of(0x3a3e42).noise(.06f), lens = Mat.of(EZ_ORANGE).glow();
        Mat glove = Mat.of(0x101113).noise(.05f), boot = Mat.of(0x111214).bevel(.3f), metal = Mat.of(0x6a6e72).noise(.06f);
        Mat band = Mat.of(0xd8d8d8).decal((m, g, x, y, w, h, f) -> g.rect(x, y + h / 2, w, 1, EZ_ORANGE));
        Mat chest = Mat.of(0x1a1d22).bevel(.25f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            int cx = x + w / 2; // Civil Protection "lambda-less" CP badge, scratched orange
            g.rect(cx - 1, y + 1, 2, 2, 0xc9c9c9); g.px(cx - 2, y + 3, 0xc9c9c9); g.px(cx + 1, y + 3, 0xc9c9c9);
            g.px(cx, y + 4, EZ_ORANGE);
        });
        Mat hood = Mat.of(0x2a3038).decal((m, g, x, y, w, h, f) -> { if (f == Mat.Face.BACK) g.rect(x + 1, y + h - 2, w - 2, 1, 0x1a1d22); });
        // torso
        rig.box(rig.torso, uni, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4, -11.5f, -2.8f, 8, 6, 1, .2f);
        rig.box(rig.torso, vest, -4, -11.5f, 1.8f, 8, 7, 1, .2f);
        rig.box(rig.torso, strap, -4.3f, -1.5f, -2.3f, 8, 2, 4, .3f);           // duty belt
        rig.box(rig.torso, metal, -1, -1.4f, -2.9f, 2, 1, 1);                    // buckle
        rig.box(rig.torso, strap, 2.6f, -1.2f, -3.2f, 2, 2, 1);                  // pouch
        rig.box(rig.torso, strap, -4.6f, -1.2f, -3.2f, 2, 2, 1);                 // pouch
        rig.box(rig.torso, strap, -0.5f, -11.6f, -3.0f, 1, 9, 1);                // vest strap
        // head: hooded gas-mask helmet
        rig.box(rig.head, hood, -4, -8, -4, 8, 8, 8, .35f);
        rig.box(rig.head, mask, -3.5f, -7, -4.9f, 7, 6, 1);                     // faceplate
        rig.box(rig.head, lens, -3, -6, -5.4f, 2, 2, 1);                        // left eye
        rig.box(rig.head, lens, 1, -6, -5.4f, 2, 2, 1);                         // right eye
        rig.box(rig.head, maskD, -1.5f, -3.5f, -5.8f, 3, 3, 2);                 // respirator snout
        rig.box(rig.head, metal, -2.5f, -2.5f, -5.2f, 1, 2, 1);                 // filter
        rig.box(rig.head, metal, 1.5f, -2.5f, -5.2f, 1, 2, 1);
        rig.box(rig.head, mask, -4.5f, -8.7f, -3.5f, 9, 1, 7);                  // helmet crown plate
        // arms
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, uni, -2, -2, -2, 4, 12, 4);
            rig.box(arm, vest, s < 0 ? -3f : -2f, -2.6f, -2.5f, 5, 3, 5);        // shoulder guard
            rig.box(arm, glove, -2, 8, -2, 4, 4, 4, .2f);
            if (s > 0) rig.box(arm, band, -2, 2.5f, -2, 4, 2, 4, .25f);         // CP armband
            else rig.box(arm, strap, -3.2f, -1, -1, 1, 3, 2);                   // shoulder radio
        }
        // legs
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, uni, -2, 0, -2, 4, 12, 4);
            rig.box(leg, vest, -2, 4, -2.6f, 4, 3, 1);                           // knee pad
            rig.box(leg, boot, -2, 9, -2.8f, 4, 3, 5, .2f);
            if (s < 0) rig.box(leg, strap, -2.8f, 1, -1, 1, 4, 3);              // thigh holster
        }
        buildWeapons(rig, metal);
    }

    private static void buildWeapons(Rig rig, Mat metal) {
        Mat gun = Mat.of(0x2c2e30).noise(.07f), black = Mat.of(0x151515), poly = Mat.of(0x3a3a36).noise(.05f), wood = Mat.of(0x5a3a22).noise(.15f);
        Mat comb = Mat.of(0x23282e).bevel(.3f).noise(.05f), pulse = Mat.of(PULSE).glow(), stun = Mat.of(STUN).glow(), hot = Mat.of(0xffffff).glow();
        Mat orange = Mat.of(EZ_ORANGE).glow(), red = Mat.of(LASER).glow(), olive = Mat.of(0x4b5a36).noise(.1f), rebar = Mat.of(0xff8030).glow();
        Mat steel = Mat.of(0x9a9c9e).noise(.05f);
        PartDefinition w;
        w = rig.weapon(rig.rHand, "stunstick");
        rig.box(w, black, -0.5f, -0.5f, -3, 1, 1, 5); rig.box(w, metal, -1, -1, -4, 2, 2, 1);
        rig.box(w, gun, -0.5f, -0.5f, -12, 1, 1, 8); rig.box(w, stun, -0.5f, -0.5f, -14, 1, 1, 2);
        w = rig.weapon(rig.rHand, "stunstick_hot");
        rig.box(w, black, -0.5f, -0.5f, -3, 1, 1, 5); rig.box(w, metal, -1, -1, -4, 2, 2, 1);
        rig.box(w, stun, -0.5f, -0.5f, -12, 1, 1, 8); rig.box(w, hot, -1, -1, -15, 2, 2, 3);
        w = rig.weapon(rig.rHand, "usp");
        rig.box(w, gun, -1, -2.2f, -7, 2, 2, 8); rig.box(w, black, -1, 0, -1, 2, 3, 2); rig.box(w, metal, -0.5f, -1.8f, -8, 1, 1, 1);
        w = rig.weapon(rig.rHand, "smg1");
        rig.box(w, gun, -1, -3, -8, 2, 3, 9); rig.box(w, metal, -0.5f, -2.5f, -12, 1, 1, 4); rig.box(w, black, -0.5f, 0, -5, 1, 4, 2);
        rig.box(w, poly, -1, -2, -11, 2, 2, 3); rig.box(w, black, -0.5f, -2.5f, 1, 1, 1, 5); rig.box(w, metal, -0.5f, -4, -6, 1, 1, 3);
        w = rig.weapon(rig.rHand, "spas12");
        rig.box(w, gun, -1, -3, -6, 2, 3, 8); rig.box(w, black, -0.5f, -3, -15, 1, 1, 9); rig.box(w, poly, -1, -2, -12, 2, 2, 5);
        rig.box(w, black, -0.5f, 0, -1, 1, 3, 2); rig.box(w, metal, -0.5f, -3.5f, 1, 1, 1, 5);
        w = rig.weapon(rig.rHand, "magnum");
        rig.box(w, steel, -1, -2.5f, -4, 2, 2, 3); rig.box(w, steel, -0.5f, -2.2f, -10, 1, 1, 6); rig.box(w, metal, -1, -3, -10, 2, 1, 1);
        rig.box(w, wood, -1, -0.5f, -1, 2, 4, 2);
        w = rig.weapon(rig.rHand, "ar2");
        rig.box(w, comb, -1.5f, -3.5f, -8, 3, 4, 11); rig.box(w, metal, -0.5f, -2.5f, -14, 1, 1, 6); rig.box(w, pulse, -1, -4.2f, -5, 2, 1, 4);
        rig.box(w, black, -0.5f, 0, -3, 1, 3, 2); rig.box(w, comb, -1, -2, 3, 2, 3, 3); rig.box(w, pulse, 1.6f, -2.5f, -2, 1, 1, 2);
        w = rig.weapon(rig.rHand, "crossbow");
        rig.box(w, olive, -1, -2.5f, -9, 2, 2, 12); rig.box(w, metal, -6, -2.5f, -9, 12, 1, 1); rig.box(w, rebar, -0.5f, -3.2f, -12, 1, 1, 10);
        rig.box(w, black, -0.5f, -5, -5, 1, 2, 4); rig.box(w, black, -0.5f, -0.5f, -1, 1, 3, 2);
        w = rig.weapon(rig.rHand, "rpg");
        rig.box(w, olive, -1.5f, -4, -12, 3, 3, 17); rig.box(w, black, -2, -4.5f, -13, 4, 4, 1); rig.box(w, black, -0.5f, -1, -3, 1, 3, 2);
        rig.box(w, metal, 1.5f, -4, -9, 1, 2, 3); rig.box(w, red, 1.6f, -3.5f, -10, 1, 1, 1);
        w = rig.weapon(rig.rHand, "zpg");
        rig.box(w, comb, -2, -3.5f, -7, 4, 4, 9); rig.box(w, orange, -1, -2.5f, -8, 2, 2, 1);
        rig.box(w, steel, -2.5f, -4, -12, 1, 1, 5); rig.box(w, steel, 1.5f, -4, -12, 1, 1, 5); rig.box(w, steel, -0.5f, -0.5f, -12, 1, 1, 5);
        rig.box(w, black, -0.5f, 0.5f, -3, 1, 3, 2);
        w = rig.weapon(rig.rHand, "zpg_hold");
        rig.box(w, comb, -2, -3.5f, -7, 4, 4, 9); rig.box(w, orange, -1.5f, -3, -9, 3, 3, 2);
        rig.box(w, steel, -3, -4.5f, -12, 1, 1, 5); rig.box(w, steel, 2, -4.5f, -12, 1, 1, 5); rig.box(w, steel, -0.5f, 0, -12, 1, 1, 5);
        rig.box(w, orange, -0.5f, -2.5f, -13, 1, 1, 1); rig.box(w, black, -0.5f, 0.5f, -3, 1, 3, 2);
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, olive, -1, -1, -2, 2, 2, 2); rig.box(w, metal, -0.5f, -1.8f, -1.5f, 1, 1, 1); rig.box(w, red, -0.5f, -1.2f, -2.4f, 1, 1, 1);
    }
}
