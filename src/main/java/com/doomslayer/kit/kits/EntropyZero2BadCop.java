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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/**
 * Bad Cop, Entropy : Zero 2. The EZ1 Metrocop arsenal, now Xen-augmented: green/purple veins burning through a
 * blacked-out uniform, Xen growth on the left shoulder, a three-hit augmented stunstick with a charged slam,
 * catching enemy fire and hurling it back as a combine ball, resin grenades and the Xen injection overload.
 */
public class EntropyZero2BadCop extends SlayerKit {
    static final int XG = 0x6cff3a, XP = 0xb44aff, AR2C = 0x7fe8ff, CBALL = 0xe8f8ff, BLOOD = 0x8a0303, AMBER = 0xffc060;

    // ---------- projectiles ----------
    static final ProjStyle SMG_GREN = ProjStyle.make().color(0x59604a).size(0.26f).life(60).explode(3.2f).gravity(0.06).trail(ParticleTypes.SMOKE);
    static final ProjStyle FRAG = ProjStyle.make().color(0x3c4a30).size(0.28f).life(36).explode(3.6f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle BOLT = ProjStyle.make().color(0xff7a30).size(0.16f).life(60).pierce(2).gravity(0.004).trail(ParticleTypes.CRIT);
    static final ProjStyle RPG = ProjStyle.make().color(0xffa040).size(0.45f).life(120).explode(3.6f).homing(0.22f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle COMBINE_BALL = ball(CBALL, AR2C, 0f);
    static final ProjStyle XEN_BALL = ball(XP, XG, 0.12f);
    static final ProjStyle RESIN = ProjStyle.make().color(XP).size(0.3f).life(40).gravity(0.05).trail(ParticleTypes.ITEM_SLIME)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, XP, 1.8f, pos, 40, 1.4); Fx.dustBurst(l, XG, 1.4f, pos, 30, 1.6);
                Fx.burst(l, ParticleTypes.ITEM_SLIME, pos, 30, 1.2, 0.1);
                Fx.ring(l, pos.add(0, 0.15, 0), 2.5f, XG, 1.4f, 26); Fx.ring(l, pos.add(0, 0.15, 0), 3.5f, XP, 1.2f, 32);
                Fx.sound(l, pos, SoundEvents.SLIME_BLOCK_BREAK, 1.4f, 0.6f); Fx.sound(l, pos, SoundEvents.CHORUS_FRUIT_TELEPORT, 1f, 0.6f);
                if (!(p.getOwner() instanceof SlayerEntity o)) return;
                for (LivingEntity t : Combat.around(o, pos, 3.5)) {
                    Combat.damage(o, t, 7);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 4));
                    t.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                }
            });

    /** dark-energy sphere: bounces, pierces, vaporises whatever it touches */
    static ProjStyle ball(int core, int halo, float homing) {
        return ProjStyle.make().color(core).size(0.75f).life(100).pierce(8).bounce().homing(homing).trail(ParticleTypes.END_ROD)
                .onTick(p -> {
                    if (!(p.getOwner() instanceof SlayerEntity o)) return;
                    ServerLevel l = (ServerLevel) p.level();
                    Fx.dustBurst(l, halo, 1.3f, p.position(), 3, 0.35);
                    if (p.age2 % 2 != 0) return;
                    for (LivingEntity t : Combat.around(o, p.position(), 1.3)) {
                        Combat.damage(o, t, 16); Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, t.position().add(0, t.getBbHeight() / 2, 0), 14, 0.4, 0.3);
                        Fx.sound(l, p.position(), SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.8f);
                    }
                })
                .onHit((p, pos, hit) -> {
                    ServerLevel l = (ServerLevel) p.level();
                    Fx.dustBurst(l, core, 2f, pos, 30, 0.8); Fx.ring(l, pos, 1.8f, halo, 1.4f, 22);
                    Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 20, 0.5, 0.4);
                    Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1.2f, 1.4f);
                });
    }

    public EntropyZero2BadCop() {
        super("entropy_zero2_bad_cop", "Bad Cop (EZ2)", "バッドコップ (EZ2)", "Entropy : Zero 2");
        eggPrimary = 0x22262c; eggSecondary = XG;
        maxHealth = 90; speed = 0.31; armor = 13; knockbackRes = 0.45;
        scale = 1.02f; preferredRange = 8f; dmgMult = 1.05f;
        idleWeaponR = "stunstick";
    }

    // ================= server helpers =================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.1, 0.3, 0); }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) {
        return t == null ? e.getViewVector(1f) : Combat.dirTo(mz(e), t, spread, new Random(e.getRandom().nextLong()));
    }
    static Vec3 endOf(SlayerEntity e, LivingEntity t, Vec3 d, double r) { return t != null ? mid(t) : mz(e).add(d.scale(r)); }
    static void flash(SlayerEntity e, int rgb) {
        Fx.burst(L(e), ParticleTypes.FLAME, mz(e), 2, 0.04, 0.02); Fx.dustBurst(L(e), rgb, 1.1f, mz(e), 4, 0.08);
    }
    static void shell(SlayerEntity e) { Fx.burst(L(e), ParticleTypes.SMOKE, Combat.muzzle(e, 0.4, 0.5, 0.1), 2, 0.03, 0.03); }
    static void shoot(SlayerEntity e, LivingEntity t, double spread, float dmg, int rgb) {
        Combat.hitscan(e, mz(e), aim(e, t, spread), 26, dmg, 0, rgb);
    }
    /** Xen-augmented stunstick contact: damage + arcing discharge */
    static void shock(SlayerEntity e, LivingEntity t, float dmg, double kb) {
        if (t == null) return;
        Combat.damage(e, t, dmg); Combat.knock(t, e.position(), kb, 0.15);
        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(t), 14, 0.3, 0.3);
        Fx.dustBurst(L(e), XP, 1.1f, mid(t), 6, 0.3); Fx.dustBurst(L(e), XG, 0.9f, mid(t), 4, 0.3);
        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 1.1f); snd(e, SoundEvents.BEE_STING, 0.6f, 0.6f);
    }
    static void swingFx(SlayerEntity e, float from, float to, float tilt) {
        Vec3 c = e.position().add(0, 1.3, 0);
        Fx.arc(L(e), c, e.getYRot(), 1.6f, from, to, tilt, XP, 0.9f, 14);
        Fx.arc(L(e), c, e.getYRot(), 1.45f, from, to, tilt, XG, 0.6f, 10);
        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.3f);
    }
    static void hold(SlayerEntity e, LivingEntity t, double dist, double lift) {
        if (t == null || !Combat.isFoe(e, t)) return;
        Vec3 p = e.position().add(Vec3.directionFromRotation(0, e.getYRot()).scale(dist + t.getBbWidth() * 0.5)).add(0, lift, 0);
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true;
    }
    static void veins(SlayerEntity e, int n) {
        Vec3 c = e.position().add(0, 1.1, 0);
        Fx.dustBurst(L(e), XG, 0.7f, c, n, 0.35); Fx.dustBurst(L(e), XP, 0.7f, c, n, 0.35);
    }
    /** hostile projectiles (arrows, fireballs...) flying near the cop */
    static List<Projectile> incoming(SlayerEntity e, double r) {
        return e.level().getEntitiesOfClass(Projectile.class, e.getBoundingBox().inflate(r),
                p -> !(p instanceof SlayerProjectile) && p.getOwner() instanceof LivingEntity o && Combat.isFoe(e, o));
    }

    @Override
    public void passive(SlayerEntity e) {
        if (e.tickCount % 25 == 0) veins(e, 2);
        // Xen augmentation slowly knits wounds outside of combat
        if (e.tickCount % 50 == 0 && e.getTarget() == null && e.getHealth() < e.getMaxHealth()) e.heal(1);
    }

    // ================= anim helpers =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** Metrocop rifle carry: stock high on the shoulder, head tucked down behind the sights, elbows in */
    static void shoulder(Pose p, float k, float pitch, float recoil) {
        float tw = 0.3f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.1f - tw, k); p.rArmZ = lerp(p.rArmZ, -0.05f, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.08f - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.5f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * 0.85f * k; p.headX += 0.12f * k - recoil * 0.1f * k;
        p.bodyX += (0.06f + recoil * 0.12f) * k; p.rootZ += recoil * 0.8f * k;
    }
    /** one-handed sidearm, off hand drawn to the chest, body bladed */
    static void sidearm(Pose p, float k, float pitch, float recoil) {
        p.bodyY -= 0.4f * k; p.headY += 0.4f * k;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.4f, k); p.rArmY = lerp(p.rArmY, 0.4f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -0.7f, k); p.lArmY = lerp(p.lArmY, 0.6f, k); p.lArmZ = lerp(p.lArmZ, 0.1f, k);
        p.rHandX -= recoil * 0.35f; p.bodyX -= recoil * 0.05f; p.rootZ += recoil * 0.4f;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.3f, k);
        p.lLegZ -= 0.05f * k; p.rLegZ += 0.07f * k; p.rootY -= 0.35f * k;
    }
    static void crouch(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.75f, k); p.rLegX = lerp(p.rLegX, 0.9f, k); p.rootY -= 2.2f * k; p.bodyX += 0.2f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // stunstick held low and ready, shoulders hunched; Xen twitch every few seconds
        p.rArmX = -0.35f + p.rArmX * 0.4f; p.rArmZ = 0.12f; p.rHandX = -0.5f;
        p.lArmX = -0.15f + p.lArmX * 0.5f; p.lArmZ = -0.12f;
        p.bodyX += 0.06f; p.headX += 0.05f;
        float tw = (float) Math.pow(Math.max(0, Math.sin(age * 0.07f)), 40);
        p.headZ += 0.25f * tw * (float) Math.sin(age * 2.3f); p.lArmZ -= 0.2f * tw; p.lArmX -= 0.2f * tw;
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- Augmented stunstick: 3-hit combo (forehand, backhand, overhead) ----------
        out.add(AttackDef.of("stunstick_combo").range(0, 3.3f).cd(22).weight(3).dur(34).weapon("stunstick")
                .tick((e, t, n) -> {
                    if (n == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.5f, 1.8f);
                    if (n == 6) { swingFx(e, -80, 80, 10); shock(e, t, 5, 0.3); }
                    if (n == 15) { swingFx(e, 80, -80, -10); shock(e, t, 5, 0.3); }
                    if (n == 26) {
                        swingFx(e, -80, 70, 90); shock(e, t, 9, 0.9);
                        if (t != null) Fx.ring(L(e), t.position().add(0, 0.1, 0), 1.2f, XP, 1.1f, 16);
                        snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.4f, 1.8f);
                    }
                    if (n == 6 || n == 15) Combat.lunge(e, 0.25, 0);
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -0.5f, .1f, -1.2f, .18f, -1.35f, .3f, -1.3f, .44f, -1.4f, .56f, -2.75f, .66f, -2.95f, .76f, -0.45f, .88f, -0.5f, 1, -0.4f);
                    p.rArmY = track(t, 0, 0, .1f, -0.9f, .18f, 0.8f, .3f, 0.9f, .44f, -0.8f, .56f, -0.2f, .76f, 0.1f, 1, 0);
                    p.rArmZ = track(t, 0, 0.1f, .1f, 0.6f, .18f, 0, .3f, -0.2f, .44f, 0.5f, .56f, 0.2f, .76f, 0, 1, 0.1f);
                    p.rHandX = track(t, 0, 0, .16f, -0.3f, .2f, 0.2f, .42f, -0.3f, .46f, 0.2f, .74f, 0.6f, .78f, -0.2f, 1, 0);
                    p.bodyY = track(t, 0, 0, .1f, -0.6f, .18f, 0.55f, .3f, 0.6f, .44f, -0.5f, .56f, -0.1f, .76f, 0.05f, 1, 0);
                    p.bodyX = track(t, 0, 0, .18f, 0.15f, .3f, 0.05f, .44f, 0.15f, .6f, -0.25f, .76f, 0.45f, .9f, 0.3f, 1, 0);
                    p.headY = -p.bodyY * 0.6f; p.headX = 0.2f * bump(win(t, .6f, .92f)) - 0.15f * bump(win(t, .5f, .66f));
                    p.lArmX = track(t, 0, -0.4f, .18f, -0.2f, .44f, -0.9f, .6f, -0.6f, .76f, 0.2f, 1, -0.4f);
                    p.lArmZ = track(t, 0, -0.2f, .18f, -0.6f, .44f, -0.1f, 1, -0.2f);
                    p.lLegX = track(t, 0, 0, .12f, -0.4f, .3f, -0.3f, .44f, -0.5f, .7f, -0.6f, .8f, -0.7f, 1, 0);
                    p.rLegX = track(t, 0, 0, .18f, 0.35f, .44f, 0.3f, .76f, 0.5f, 1, 0);
                    p.rootZ = -1.2f * bump(win(t, .08f, .3f)) - 1.0f * bump(win(t, .34f, .55f)) - 2.0f * bump(win(t, .6f, .92f));
                    p.rootY = -0.7f * bump(win(t, .66f, .95f)) + 0.4f * bump(win(t, .5f, .66f));
                }));

        // ---------- Augmented stunstick: charged ground slam ----------
        out.add(AttackDef.of("stunstick_charged_slam").range(0, 4).cd(120).weight(2).dur(40).weapon("stunstick")
                .tick((e, t, n) -> {
                    if (n < 22) {
                        Vec3 tip = Combat.muzzle(e, -0.2, 0.2, 1.0);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, tip, 3, 0.25, 0.1);
                        Fx.dustBurst(L(e), n % 2 == 0 ? XP : XG, 1.2f, tip, 3, 0.3 - n * 0.01);
                        if (n % 4 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.9f, 0.9f + n * 0.05f);
                    }
                    if (n == 22) { Combat.lunge(e, 0.55, 0.42); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.7f); }
                    if (n == 28) {
                        Vec3 c = e.position().add(Vec3.directionFromRotation(0, e.getYRot()).scale(1.4)).add(0, 0.2, 0);
                        Combat.blast(e, c, 4.2f, 14, 1.3f, null);
                        for (LivingEntity f : Combat.around(e, c, 4.2)) {
                            f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                            Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(f), 12, 0.4, 0.3);
                        }
                        for (int i = 0; i < 3; i++) Fx.ring(L(e), c, 1.2f + i * 1.3f, i % 2 == 0 ? XP : XG, 1.5f, 20 + i * 8);
                        Fx.burst(L(e), ParticleTypes.POOF, c, 20, 0.8, 0.05); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, c, 40, 1.5, 0.4);
                        snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2f, 1.2f); snd(e, SoundEvents.ANVIL_LAND, 0.8f, 0.5f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), ch = win(t, .05f, .55f) * (1 - win(t, .55f, .56f));
                    p.rArmX = track(t, 0, -0.4f, .15f, -2.4f, .55f, -3.0f, .66f, -3.1f, .7f, -0.3f, .85f, -0.2f, 1, -0.4f);
                    p.lArmX = track(t, 0, -0.3f, .15f, -2.2f, .55f, -2.9f, .66f, -3.0f, .7f, -0.5f, .85f, -0.4f, 1, -0.3f);
                    p.rArmY = -0.3f * k; p.lArmY = 0.4f * k; p.rArmZ = 0; p.lArmZ = 0;
                    p.rHandX = track(t, 0, 0, .55f, -0.4f, .7f, 0.5f, 1, 0);
                    p.bodyX = track(t, 0, 0, .15f, -0.25f, .55f, -0.35f, .66f, -0.4f, .7f, 0.7f, .85f, 0.6f, 1, 0);
                    p.bodyZ = 0.05f * ch * ch * (float) Math.sin(a * 5);
                    p.headX = track(t, 0, 0, .15f, -0.35f, .66f, -0.3f, .7f, 0.45f, 1, 0);
                    p.rootY = track(t, 0, 0, .15f, -1.0f, .5f, -1.4f, .6f, 2.2f, .66f, 2.6f, .7f, -2.0f, .85f, -1.6f, 1, 0);
                    p.rootZ = track(t, .5f, 0, .66f, -2f, .85f, -2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .15f, -0.5f, .55f, -0.5f, .62f, -0.9f, .7f, -0.9f, .85f, -0.8f, 1, 0);
                    p.rLegX = track(t, 0, 0, .15f, 0.4f, .55f, 0.4f, .62f, 0.6f, .7f, 0.9f, .85f, 0.8f, 1, 0);
                    p.lLegZ = -0.15f * ch; p.rLegZ = 0.15f * ch;
                }));

        // ---------- Execution: throat grab + stunstick rammed into the chest ----------
        out.add(AttackDef.of("stunstick_execution").range(0, 3.5f).lowHealthTarget(0.25f).weight(6).cd(70).dur(38).weapon("stunstick")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.45, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.8f); }
                    hold(e, t, 0.9, n > 4 && n < 30 ? 0.3 : 0);
                    if (n == 10 && t != null) { snd(e, SoundEvents.TRIDENT_HIT, 1, 0.7f); Fx.dustBurst(L(e), BLOOD, 1.3f, mid(t), 10, 0.25); }
                    if (n > 10 && n < 29 && n % 2 == 0 && t != null) {
                        Combat.damage(e, t, Math.min(1.5f, Math.max(0, t.getHealth() - 1)));
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(t), 10, 0.35, 0.3); Fx.dustBurst(L(e), n % 4 == 0 ? XP : XG, 1f, mid(t), 5, 0.4);
                        snd(e, SoundEvents.BEE_STING, 0.7f, 0.5f + n * 0.02f);
                    }
                    if (n == 30 && t != null) {
                        Fx.dustBurst(L(e), BLOOD, 1.5f, mid(t), 25, 0.4); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(t), 30, 0.5, 0.5);
                        Combat.damage(e, t, t.getHealth() + 100); Combat.knock(t, e.position(), 1.2, 0.3);
                        snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 1f, 1.5f); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1, 0.7f);
                        e.heal(5); veins(e, 10);
                    }
                })
                .anim((p, t, a) -> {
                    float zap = win(t, .28f, .3f) * (1 - win(t, .76f, .78f));
                    p.lArmX = track(t, 0, 0, .12f, -1.7f, .76f, -1.8f, .82f, -1.2f, 1, 0); p.lArmY = track(t, 0, 0, .12f, 0.3f, .76f, 0.3f, 1, 0);
                    p.lArmZ = track(t, .76f, 0, .82f, 0.3f, 1, 0);
                    p.rArmX = track(t, 0, 0, .12f, 0.3f, .2f, 0.6f, .27f, -1.3f, .76f, -1.25f, .82f, 0.4f, 1, 0);
                    p.rArmY = track(t, 0, 0, .27f, -0.2f, 1, 0); p.rArmZ = track(t, 0, 0, .2f, 0.4f, .27f, 0, 1, 0);
                    p.rHandX = track(t, 0, 0, .2f, -1.0f, .27f, 0.2f, .76f, 0.2f, 1, 0);
                    p.rArmY += 0.07f * (float) Math.sin(a * 3.1f) * zap; p.bodyZ = 0.04f * (float) Math.sin(a * 2.7f) * zap;
                    p.bodyY = track(t, 0, 0, .2f, -0.45f, .27f, 0.25f, .76f, 0.2f, .82f, -0.2f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.bodyX = track(t, 0, 0, .27f, 0.3f, .76f, 0.25f, .82f, -0.1f, 1, 0);
                    p.headX = track(t, 0, 0, .3f, -0.15f, .76f, -0.15f, .82f, 0.25f, 1, 0); p.headZ = 0.1f * zap * (float) Math.sin(a * 1.7f);
                    p.rootZ = track(t, 0, 0, .12f, -2f, .27f, -2.4f, .82f, -2.2f, 1, 0); p.rootY = track(t, .76f, 0, .82f, -0.6f, 1, 0);
                    p.lLegX = track(t, 0, 0, .12f, -0.5f, .85f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .12f, 0.4f, .85f, 0.4f, 1, 0);
                }));

        // ---------- Shab: improvised shiv, three fast stabs while the off hand pins the target ----------
        out.add(AttackDef.of("shab_shiv").range(0, 2.8f).cd(34).weight(2).dur(24).weapon("shiv")
                .tick((e, t, n) -> {
                    if (n == 6 || n == 11 || n == 16) {
                        if (t != null) { Combat.damage(e, t, 4); Fx.dustBurst(L(e), BLOOD, 1.2f, mid(t), 8, 0.2); }
                        snd(e, SoundEvents.TRIDENT_HIT, 0.7f, 1.4f + n * 0.02f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f), s1 = bump(win(t, .14f, .3f)), s2 = bump(win(t, .36f, .5f)), s3 = bump(win(t, .56f, .74f));
                    float stab = Math.max(s1, Math.max(s2, s3));
                    p.lArmX = lerp(p.lArmX, -1.5f, k); p.lArmY = lerp(p.lArmY, 0.3f, k); p.lArmZ = lerp(p.lArmZ, -0.1f, k);
                    p.rArmX = lerp(p.rArmX, -0.6f - 0.9f * stab, k); p.rArmY = lerp(p.rArmY, 0.1f - 0.2f * stab, k); p.rArmZ = lerp(p.rArmZ, 0.35f * (1 - stab), k);
                    p.rHandX = 0.8f * k - 0.6f * stab;
                    p.bodyY = k * (-0.35f + 0.55f * stab); p.headY = -p.bodyY * 0.6f; p.bodyX = k * (0.15f + 0.15f * stab);
                    p.rootZ = -0.9f * stab - 1f * k; p.rootY = -0.3f * k;
                    p.lLegX = lerp(p.lLegX, -0.45f, k); p.rLegX = lerp(p.rLegX, 0.35f + 0.1f * stab, k);
                }));

        // ---------- Boot kick ----------
        out.add(AttackDef.of("boot_kick").range(0, 2.8f).cd(50).weight(1).dur(20).weapon("stunstick")
                .tick((e, t, n) -> {
                    if (n == 8 && t != null) {
                        Combat.damage(e, t, 6); Combat.knock(t, e.position(), 1.4, 0.35); Fx.sparks(L(e), mid(t), 6);
                        snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rLegX = track(t, 0, 0, .25f, 0.5f, .32f, 0.9f, .42f, -1.6f, .6f, -1.4f, .8f, 0.1f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, 0.15f, .8f, 0.15f, 1, 0);
                    p.bodyX = track(t, 0, 0, .3f, 0.15f, .42f, -0.4f, .62f, -0.3f, 1, 0);
                    p.rootZ = track(t, 0, 0, .3f, 0.6f, .42f, -0.8f, 1, 0); p.rootY = -0.5f * bump(win(t, .3f, .7f));
                    p.rArmX = track(t, 0, -0.35f, .42f, 0.5f, .7f, 0.3f, 1, -0.35f); p.rArmZ = track(t, 0, 0.1f, .42f, 0.5f, 1, 0.1f);
                    p.lArmX = track(t, 0, 0, .42f, -0.9f, .7f, -0.6f, 1, 0); p.lArmZ = track(t, 0, 0, .42f, -0.4f, 1, 0);
                    p.headX = 0.15f * bump(win(t, .35f, .8f));
                }));

        // ---------- USP Match: three aimed shots ----------
        out.add(AttackDef.of("usp_pistol").range(3, 20).cd(22).weight(3).dur(20).weapon("usp")
                .tick((e, t, n) -> {
                    if (n == 3 || n == 8 || n == 13) { shoot(e, t, 0.025, 4, AMBER); flash(e, AMBER); shell(e); snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, 1.8f); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .85f), r = kick(t, 3 / 20f, .12f) + kick(t, 8 / 20f, .12f) + kick(t, 13 / 20f, .14f);
                    sidearm(p, k, 0, r); stance(p, k);
                    p.headX += 0.1f * k;
                }));

        // ---------- SMG1: automatic burst ----------
        out.add(AttackDef.of("smg1").range(3, 18).cd(28).weight(3).dur(28).weapon("smg1")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 20 && n % 2 == 0) { shoot(e, t, 0.05, 2.2f, AMBER); flash(e, AMBER); if (n % 4 == 0) shell(e); snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.6f, 1.9f + e.getRandom().nextFloat() * 0.2f); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .13f, .15f) * (1 - win(t, .72f, .74f));
                    float r = 0.3f * Math.abs((float) Math.sin(a * 1.6f)) * fire;
                    shoulder(p, k, 0.05f * (float) Math.sin(a * 0.8f) * fire, r); stance(p, k);
                    p.rHandZ += 0.04f * (float) Math.sin(a * 3.7f) * fire; p.bodyZ += 0.02f * (float) Math.sin(a * 2.9f) * fire;
                }));

        // ---------- SMG1 alt: underbarrel grenade ----------
        out.add(AttackDef.of("smg1_grenade").range(6, 18).cd(140).weight(2).dur(26).weapon("smg1")
                .tick((e, t, n) -> {
                    if (n == 10) {
                        double dist = t != null ? mz(e).distanceTo(mid(t)) : 10;
                        Combat.shoot(e, SMG_GREN, mz(e), aim(e, t, 0.01).add(0, 0.1 + dist * 0.02, 0).normalize(), 0.9 + dist * 0.02, 12);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, mz(e), 4, 0.1, 0.03);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1, 0.7f); snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.6f, 1.5f);
                    }
                    if (n == 18) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.5f, 1.8f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 1.8f * kick(t, 10 / 26f, .3f), tilt = win(t, .1f, .35f) * (1 - win(t, .6f, .8f));
                    shoulder(p, k, -0.3f * tilt, r); stance(p, k);
                    p.lArmX += 0.15f * bump(win(t, .2f, .36f)); p.bodyX -= 0.1f * tilt; p.headX -= 0.1f * tilt;
                    p.lArmX += 0.4f * bump(win(t, .62f, .85f)); p.lArmZ -= 0.2f * bump(win(t, .62f, .85f)); // slide the next round in
                }));

        // ---------- SPAS-12: pump shot ----------
        out.add(AttackDef.of("spas12").range(1.5f, 10).cd(26).weight(3).dur(22).weapon("spas12")
                .tick((e, t, n) -> {
                    if (n == 5) {
                        for (int i = 0; i < 7; i++) shoot(e, t, 0.08, 2.4f, AMBER);
                        flash(e, 0xffa040); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.2f, 0.9f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.3f, 1.9f);
                    }
                    if (n == 11) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.7f);
                    if (n == 15) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.8f); shell(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = kick(t, 5 / 22f, .25f), pump = bump(win(t, .48f, .72f));
                    shoulder(p, k, 0, r * 1.5f); stance(p, k);
                    p.lArmX += 0.4f * pump * k; p.rHandX -= r * 0.4f; p.bodyZ += 0.05f * pump; p.headX += 0.05f * pump;
                }));

        // ---------- SPAS-12 alt: double-shell blast ----------
        out.add(AttackDef.of("spas12_double").range(0, 7).cd(60).weight(2).dur(30).weapon("spas12")
                .tick((e, t, n) -> {
                    if (n == 6) {
                        for (int i = 0; i < 14; i++) shoot(e, t, 0.11, 2.6f, AMBER);
                        for (LivingEntity f : Combat.cone(e, 4, 40)) Combat.knock(f, e.position(), 1.1, 0.3);
                        flash(e, 0xffa040); flash(e, 0xff6020);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.5f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.4f, 0.6f);
                    }
                    if (n == 14 || n == 20) { snd(e, n == 14 ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, 0.7f, 1.6f); shell(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .92f), r = kick(t, 6 / 30f, .3f), pump = bump(win(t, .42f, .58f)) + bump(win(t, .6f, .76f));
                    shoulder(p, k, 0, r * 2.4f); stance(p, k);
                    p.rootY += r * 0.6f; p.bodyX -= r * 0.25f; p.rHandX -= r * 0.6f;
                    p.lArmX += 0.4f * pump * k; p.lLegX -= 0.15f * r;
                }));

        // ---------- .357 Magnum: thumb the hammer, one heavy shot ----------
        out.add(AttackDef.of("magnum_357").range(6, 28).cd(40).weight(2).dur(26).weapon("magnum")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 0.8f);
                    if (n == 11) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.005), 32, 14, 1, 0xfff0c0); flash(e, 0xffd080);
                        Fx.burst(L(e), ParticleTypes.SMOKE, mz(e), 5, 0.1, 0.02);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 0.8f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.3f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .88f), r = 2.4f * kick(t, 11 / 26f, .35f), cock = bump(win(t, .1f, .25f));
                    // two-handed grip, weaver stance
                    p.rArmX = lerp(p.rArmX, -HALF - r * 0.55f, k); p.rArmY = lerp(p.rArmY, 0.15f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
                    p.lArmX = lerp(p.lArmX, -HALF + 0.1f - r * 0.5f, k); p.lArmY = lerp(p.lArmY, 0.6f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
                    p.rHandY += 0.3f * cock; p.rHandX -= r * 0.5f;
                    p.bodyY -= 0.15f * k; p.headY += 0.1f * k; p.headX += 0.12f * k - r * 0.12f; p.bodyX -= r * 0.15f;
                    p.rootZ += r * 0.7f; stance(p, k);
                }));

        // ---------- AR2 pulse rifle ----------
        out.add(AttackDef.of("ar2").range(4, 26).cd(30).weight(3).dur(30).weapon("ar2")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 22 && n % 2 == 0) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.03), 28, 3.2f, 0, AR2C); Fx.dustBurst(L(e), AR2C, 1f, mz(e), 3, 0.06);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.6f, 1.2f); if (n % 4 == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.25f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .13f, .15f) * (1 - win(t, .75f, .77f));
                    float r = 0.4f * (0.5f + 0.5f * (float) Math.sin(a * PI)) * fire;
                    shoulder(p, k, 0, r); stance(p, k);
                    p.rHandZ += 0.03f * (float) Math.sin(a * 3.1f) * fire;
                }));

        // ---------- AR2 alt: dark energy ball ----------
        out.add(AttackDef.of("ar2_energy_ball").range(4, 24).cd(180).weight(2).dur(36).weapon("ar2")
                .tick((e, t, n) -> {
                    if (n < 20) {
                        Fx.dustBurst(L(e), n % 2 == 0 ? CBALL : AR2C, 1.2f, mz(e), 3, 0.35 - n * 0.015);
                        if (n % 5 == 0) snd(e, SoundEvents.BEACON_ACTIVATE, 0.6f, 1f + n * 0.05f);
                    }
                    if (n == 20) {
                        Combat.shoot(e, COMBINE_BALL, mz(e), aim(e, t, 0), 0.9, 24);
                        Fx.dustBurst(L(e), CBALL, 2f, mz(e), 20, 0.4);
                        e.setDeltaMovement(e.getDeltaMovement().subtract(e.getViewVector(1f).scale(0.3))); e.hurtMarked = true;
                        snd(e, SoundEvents.ENDER_DRAGON_SHOOT, 1f, 1.6f); snd(e, SoundEvents.BEACON_DEACTIVATE, 1, 1.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), ch = win(t, .05f, .55f) * (1 - win(t, .55f, .56f)), r = 2.4f * kick(t, 20 / 36f, .3f);
                    shoulder(p, k, 0, r); stance(p, k);
                    p.bodyZ += 0.04f * ch * ch * (float) Math.sin(a * 4.5f); p.rArmZ += 0.03f * ch * (float) Math.cos(a * 5f);
                    p.rootY -= 0.6f * ch; p.rootZ += r * 0.9f; p.bodyX -= r * 0.15f; p.rHandX -= r * 0.4f;
                }));

        // ---------- Crossbow: scoped heated rebar ----------
        out.add(AttackDef.of("crossbow").range(10, 40).cd(60).weight(2).dur(36).weapon("crossbow")
                .tick((e, t, n) -> {
                    if (n == 5) snd(e, SoundEvents.SPYGLASS_USE, 1, 1);
                    if (n == 16) {
                        Combat.shoot(e, BOLT, mz(e), aim(e, t, 0.002), 3.2, 22);
                        snd(e, SoundEvents.CROSSBOW_SHOOT, 1.3f, 0.7f); snd(e, SoundEvents.ARROW_SHOOT, 1, 0.6f);
                    }
                    if (n == 24) snd(e, SoundEvents.CROSSBOW_LOADING_MIDDLE, 1, 0.9f);
                    if (n == 30) snd(e, SoundEvents.CROSSBOW_LOADING_END, 1, 0.9f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), zoom = win(t, .1f, .3f) * (1 - win(t, .5f, .6f)), r = 1.2f * kick(t, 16 / 36f, .2f);
                    float reload = bump(win(t, .6f, .9f));
                    shoulder(p, k, 0.3f * reload, r); stance(p, k);
                    p.headX += 0.2f * zoom; p.headY -= 0.1f * zoom; p.bodyX += 0.12f * zoom; p.rootY -= 0.8f * zoom; p.lLegX -= 0.15f * zoom;
                    p.lArmX += 0.6f * reload; p.lArmZ -= 0.4f * reload; p.headX += 0.2f * reload; // seat a fresh bolt
                }));

        // ---------- RPG: laser-guided rocket ----------
        out.add(AttackDef.of("rpg").range(8, 36).cd(90).weight(2).dur(34).weapon("rpg")
                .tick((e, t, n) -> {
                    if (n >= 3 && n < 12 && t != null) Fx.beam(L(e), Combat.muzzle(e, 1.2, 0.35, 0.15), mid(t), 0xff2020, 0.5f, 1.2);
                    if (n == 6) snd(e, SoundEvents.NOTE_BLOCK_PLING.value(), 0.7f, 2f);
                    if (n == 12) {
                        Combat.shoot(e, RPG, mz(e), aim(e, t, 0.01), 1.1, 16);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.6, 0.32, 0), 8, 0.25, 0.05); flash(e, 0xffa040);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.5f); snd(e, SoundEvents.GHAST_SHOOT, 0.5f, 1.3f);
                    }
                    if (n == 24) snd(e, SoundEvents.PISTON_EXTEND, 0.6f, 1.2f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 2f * kick(t, 12 / 34f, .35f);
                    shoulder(p, k, -0.05f, r); stance(p, k);
                    p.rArmZ -= 0.1f * k; p.headY -= 0.1f * k; p.headX += 0.05f * k; // eye to the sight, tube over the shoulder
                    p.rHandX -= r * 0.4f; p.bodyX -= r * 0.15f;
                    p.lArmX += 0.5f * bump(win(t, .65f, .88f)); p.lArmZ -= 0.3f * bump(win(t, .65f, .88f));
                }));

        // ---------- ZPG: pull in, then punt with a zero-point blast ----------
        out.add(AttackDef.of("zpg").range(2, 12).cd(140).weight(2).dur(30).weapon("zpg")
                .tick((e, t, n) -> {
                    if (n < 14) {
                        Vec3 m = mz(e);
                        for (LivingEntity f : Combat.cone(e, 12, 25)) {
                            Vec3 d = m.subtract(f.position()).normalize().scale(0.12);
                            f.setDeltaMovement(f.getDeltaMovement().add(d)); f.hurtMarked = true;
                            if (n % 3 == 0) Fx.beam(L(e), mid(f), m, 0xffb050, 0.6f, 0.6);
                        }
                        if (n % 3 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.8f, 1.6f);
                    }
                    if (n == 15) {
                        for (LivingEntity f : Combat.cone(e, 8, 35)) { Combat.damage(e, f, 10); Combat.knock(f, e.position(), 2.4, 0.6); Fx.sparks(L(e), mid(f), 8); }
                        Vec3 m = mz(e), f = e.getViewVector(1f);
                        for (int i = 1; i <= 4; i++) Fx.ring(L(e), m.add(f.scale(i * 1.4)), 0.4f + i * 0.35f, 0xffc060, 1.2f, 14);
                        snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.9f, 1.6f); snd(e, SoundEvents.ANVIL_LAND, 0.5f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), pull = win(t, .05f, .45f) * (1 - win(t, .48f, .5f)), r = 2.6f * kick(t, 15 / 30f, .35f);
                    // hip carried, braced for the shove
                    p.rArmX = lerp(p.rArmX, -1.1f - r * 0.2f, k); p.rArmY = lerp(p.rArmY, -0.2f, k); p.rArmZ = lerp(p.rArmZ, 0.1f, k);
                    p.lArmX = lerp(p.lArmX, -1.35f - r * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.65f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
                    p.rHandX = lerp(p.rHandX, -0.45f, k);
                    p.bodyX += (0.1f - 0.15f * pull + 0.2f * r) * k; p.bodyY += 0.15f * k; p.headY -= 0.15f * k;
                    p.rootZ += 1.0f * pull - 1.4f * r; p.bodyZ += 0.03f * pull * (float) Math.sin(a * 6);
                    p.lLegX = lerp(p.lLegX, -0.5f, k); p.rLegX = lerp(p.rLegX, 0.45f, k); p.rootY -= 0.6f * k;
                }));

        // ---------- Frag grenade (overhand) ----------
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(160).weight(2).dur(22).weapons("stunstick", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.5f);
                    if (n == 11) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.5);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Combat.shoot(e, FRAG, from, aim(e, t, 0.02).add(0, 0.15 + dist * 0.018, 0).normalize(), 0.75 + dist * 0.025, 14);
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "frag" : null;
                    p.rArmX = track(t, 0, -0.35f, .4f, -0.1f, .6f, -0.6f, 1, -0.35f); p.rArmZ = 0.3f;
                    p.lArmX = track(t, 0, 0, .15f, -0.8f, .4f, 0.9f, .5f, -2.4f, .7f, -1.4f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .4f, -0.5f, .5f, -0.1f, 1, 0);
                    p.lHandX = track(t, 0, 0, .1f, 0.4f, .2f, 0, 1, 0); // pull the pin with the thumb
                    p.bodyY = track(t, 0, 0, .4f, -0.55f, .55f, 0.45f, .75f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .4f, -0.2f, .55f, 0.3f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.35f, .55f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .4f, 0.3f, .55f, 0.45f, 1, 0);
                    p.rootZ = track(t, .4f, 0, .55f, -1.4f, 1, 0); p.rootY = -0.5f * bump(win(t, .4f, .8f));
                }));

        // ---------- Resin grenade (underarm lob of Xen goo) ----------
        out.add(AttackDef.of("resin_grenade").range(4, 16).cd(200).weight(2).dur(24).weapons("stunstick", "resin")
                .tick((e, t, n) -> {
                    if (n < 10) Fx.dustBurst(L(e), n % 2 == 0 ? XP : XG, 0.8f, Combat.muzzle(e, 0.3, -0.45, -0.5), 2, 0.1);
                    if (n == 4) snd(e, SoundEvents.SLIME_SQUISH, 1, 0.6f);
                    if (n == 12) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, -0.1);
                        double dist = t != null ? from.distanceTo(mid(t)) : 9;
                        Combat.shoot(e, RESIN, from, aim(e, t, 0.02).add(0, 0.25 + dist * 0.02, 0).normalize(), 0.6 + dist * 0.025, 4);
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.6f); snd(e, SoundEvents.SLIME_JUMP, 0.8f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .52f ? "resin" : null;
                    p.lArmX = track(t, 0, 0, .2f, 0.6f, .4f, 0.9f, .52f, -1.3f, .7f, -1.0f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .4f, -0.2f, 1, 0);
                    p.rArmX = track(t, 0, -0.35f, .4f, -0.2f, .6f, -0.5f, 1, -0.35f); p.rArmZ = track(t, 0, 0.1f, .5f, 0.4f, 1, 0.1f);
                    p.bodyX = track(t, 0, 0, .4f, 0.35f, .55f, -0.1f, 1, 0);
                    p.bodyY = track(t, 0, 0, .4f, -0.3f, .55f, 0.2f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.headX = track(t, 0, 0, .4f, -0.2f, .6f, -0.1f, 1, 0);
                    p.rootY = track(t, 0, 0, .4f, -1.6f, .6f, -0.4f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.6f, .6f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.5f, .6f, 0.35f, 1, 0);
                }));

        // ---------- Combine ball catch: snatch enemy fire out of the air, hurl it back as a Xen-charged ball ----------
        out.add(AttackDef.of("combine_ball_catch").range(0, 24).cd(100).weight(5).dur(30).weapons("stunstick", "cball")
                .when((e, t) -> !incoming(e, 8).isEmpty())
                .tick((e, t, n) -> {
                    Vec3 hand = Combat.muzzle(e, 0.7, -0.4, 0.2);
                    if (n >= 2 && n <= 10) {
                        for (Projectile pr : incoming(e, 9)) {
                            Fx.beam(L(e), pr.position(), hand, XP, 0.7f, 0.4);
                            Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, pr.position(), 6, 0.2, 0.2);
                            pr.discard();
                            snd(e, SoundEvents.SHIELD_BLOCK, 1, 1.4f);
                        }
                        Fx.dustBurst(L(e), n % 2 == 0 ? XP : XG, 1.2f, hand, 4, 0.2);
                    }
                    if (n == 8) snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.7f);
                    if (n > 10 && n < 18) Fx.dustBurst(L(e), CBALL, 1f, hand, 2, 0.15);
                    if (n == 18) {
                        Vec3 d = t != null ? Combat.dirTo(hand, t, 0, new Random(e.getRandom().nextLong())) : e.getViewVector(1f);
                        Combat.shoot(e, XEN_BALL, hand, d, 1.0, 22);
                        snd(e, SoundEvents.ENDER_DRAGON_SHOOT, 0.9f, 1.7f); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t > .26f && t < .62f ? "cball" : null;
                    // palm thrust out to catch, recoil into chest, then a pitcher's throw
                    p.lArmX = track(t, 0, 0, .1f, -1.65f, .3f, -1.6f, .38f, -0.6f, .5f, 0.7f, .6f, -2.4f, .75f, -1.3f, 1, 0);
                    p.lArmY = track(t, 0, 0, .1f, 0.1f, .38f, 0.6f, .5f, 0, 1, 0);
                    p.lArmZ = track(t, 0, 0, .38f, 0, .5f, -0.6f, .6f, -0.1f, 1, 0);
                    p.lHandX = track(t, 0, 0, .1f, -0.8f, .3f, -0.8f, .4f, 0, 1, 0);
                    p.rArmX = track(t, 0, -0.35f, .1f, -0.1f, .5f, 0.2f, .62f, -0.7f, 1, -0.35f); p.rArmZ = track(t, 0, 0.1f, .5f, 0.5f, 1, 0.1f);
                    p.bodyY = track(t, 0, 0, .1f, 0.2f, .3f, 0.2f, .5f, -0.6f, .62f, 0.5f, .8f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyX = track(t, 0, 0, .1f, -0.1f, .35f, -0.2f, .5f, -0.25f, .62f, 0.35f, 1, 0);
                    p.headX = track(t, 0, 0, .1f, -0.1f, .5f, 0, .62f, 0.2f, 1, 0);
                    p.rootZ = track(t, 0, 0, .3f, 0.8f, .5f, 1f, .62f, -1.6f, .8f, -1.2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .1f, -0.3f, .5f, -0.1f, .62f, -0.6f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.3f, .62f, 0.5f, 1, 0);
                    p.bodyZ = 0.04f * (float) Math.sin(a * 4) * win(t, .1f, .3f) * (1 - win(t, .3f, .35f));
                }));

        // ---------- Xen boost: veins flare, combat stimulant surge ----------
        out.add(AttackDef.of("xen_boost").range(0, 30).cd(600).weight(4).dur(30).noLos().weapon(null)
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.6f)
                .tick((e, t, n) -> {
                    if (n < 24) veins(e, 3);
                    if (n == 4) snd(e, SoundEvents.ZOMBIE_VILLAGER_CURE, 0.6f, 1.6f);
                    if (n == 16) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 1));
                        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 240, 1));
                        e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
                        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 0));
                        Vec3 c = e.position().add(0, 0.2, 0);
                        Fx.ring(L(e), c, 1.5f, XG, 1.4f, 22); Fx.ring(L(e), c.add(0, 0.6, 0), 1.1f, XP, 1.4f, 18);
                        snd(e, SoundEvents.RAVAGER_ROAR, 0.8f, 1.5f); snd(e, SoundEvents.BEACON_POWER_SELECT, 1, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f), flex = win(t, .45f, .55f) * (1 - win(t, .8f, .95f)), shake = win(t, .1f, .5f) * (1 - win(t, .5f, .55f));
                    // hunch over gripping the left arm as the growth pulses... then rear back and flex
                    p.bodyX = 0.5f * k * (1 - flex) - 0.25f * flex; p.headX = 0.4f * k * (1 - flex) - 0.45f * flex;
                    p.rArmX = lerp(-0.9f, -0.3f, flex) * k; p.rArmY = lerp(0.7f, 0, flex) * k; p.rArmZ = 0.9f * flex;
                    p.lArmX = lerp(-0.5f, -0.3f, flex) * k; p.lArmZ = -0.9f * flex - 0.1f;
                    p.rHandX = -0.9f * flex;
                    float s = 0.06f * shake * (float) Math.sin(a * 5.3f);
                    p.bodyZ = s; p.headZ = -s * 2; p.lArmY = s * 2;
                    p.rootY = -1.4f * k * (1 - flex) + 0.4f * flex;
                    p.lLegX = -0.4f * k; p.rLegX = 0.4f * k; p.lLegZ = -0.18f * flex; p.rLegZ = 0.18f * flex;
                }));

        // ---------- Xen injection: syringe to the neck, convulse, release a Xen overload nova ----------
        out.add(AttackDef.of("xen_injection_blast").range(0, 8).cd(900).weight(8).dur(46).noLos().weapon("syringe")
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.35f)
                .tick((e, t, n) -> {
                    if (n == 8) { snd(e, SoundEvents.BOTTLE_EMPTY, 1, 0.8f); snd(e, SoundEvents.TRIDENT_HIT, 0.6f, 1.8f); }
                    if (n > 10 && n < 34) {
                        veins(e, 4); if (n % 4 == 0) snd(e, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 0.5f, 0.6f + n * 0.03f);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, e.position().add(0, 1, 0), 3, 0.5, 0.2);
                    }
                    if (n == 34) {
                        Vec3 c = e.position().add(0, 1, 0);
                        Combat.blast(e, c, 6.5f, 18, 1.6f, null);
                        for (LivingEntity f : Combat.around(e, c, 6.5)) {
                            f.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1)); f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
                            Fx.beam(L(e), c, mid(f), XP, 0.9f, 0.3);
                        }
                        Fx.explosion(L(e), c, 3f);
                        for (int i = 0; i < 4; i++) Fx.ring(L(e), c.add(0, -0.8 + i * 0.5, 0), 2 + i * 1.3f, i % 2 == 0 ? XG : XP, 1.8f, 30);
                        Fx.dustBurst(L(e), XG, 2.2f, c, 50, 3); Fx.dustBurst(L(e), XP, 2.2f, c, 50, 3);
                        e.heal(25); e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
                        snd(e, SoundEvents.GENERIC_EXPLODE, 1.4f, 0.7f); snd(e, SoundEvents.RAVAGER_ROAR, 1.2f, 1.1f); snd(e, SoundEvents.CHORUS_FRUIT_TELEPORT, 1.4f, 0.5f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponR = t < .3f ? "syringe" : null;
                    float conv = win(t, .24f, .3f) * (1 - win(t, .7f, .74f)), nova = bump(win(t, .72f, .9f));
                    // syringe raised and stabbed into the side of the neck
                    p.rArmX = track(t, 0, 0, .1f, -1.8f, .17f, -2.5f, .26f, -2.5f, .32f, -0.5f, .72f, -0.5f, .78f, -1.9f, .9f, -1.7f, 1, 0);
                    p.rArmY = track(t, 0, 0, .1f, 0.5f, .17f, 0.9f, .26f, 0.9f, .32f, 0, 1, 0);
                    p.rArmZ = track(t, .7f, 0, .78f, 1.3f, .9f, 1.1f, 1, 0);
                    p.rHandX = track(t, 0, 0, .1f, 0.8f, .26f, 0.8f, .32f, 0, 1, 0);
                    p.headZ = track(t, 0, 0, .12f, -0.35f, .26f, -0.35f, .32f, 0, 1, 0);
                    // convulse on one knee, gripping the dirt
                    p.lArmX = track(t, 0, 0, .3f, -0.3f, .72f, -0.4f, .78f, -1.9f, .9f, -1.7f, 1, 0);
                    p.lArmZ = track(t, .7f, -0.1f, .78f, -1.3f, .9f, -1.1f, 1, 0);
                    p.bodyX = track(t, 0, 0, .26f, 0.1f, .34f, 0.6f, .72f, 0.55f, .78f, -0.35f, .9f, -0.3f, 1, 0);
                    p.headX = track(t, .26f, 0, .34f, 0.5f, .72f, 0.45f, .78f, -0.5f, 1, 0);
                    float jit = conv * (0.07f * (float) Math.sin(a * 5.7f) + 0.05f * (float) Math.sin(a * 9.1f));
                    p.bodyZ = jit; p.headY = jit * 3; p.lArmY = jit * 2; p.rArmY += jit * 2;
                    p.rootY = track(t, 0, 0, .26f, 0, .34f, -4f, .72f, -4f, .78f, 0.8f, .9f, 0.5f, 1, 0);
                    p.rLegX = track(t, 0, 0, .26f, 0, .34f, 1.3f, .72f, 1.3f, .78f, 0, 1, 0);
                    p.lLegX = track(t, 0, 0, .26f, 0, .34f, -1.2f, .72f, -1.2f, .78f, 0, 1, 0);
                    p.lLegZ = -0.25f * nova; p.rLegZ = 0.25f * nova;
                }));
    }

    // ================= model =================
    /** glowing Xen veins: jagged branching lines across a face */
    static Mat.Decal veinDecal(int seed, Mat.Face... faces) {
        return (m, g, x, y, w, h, f) -> {
            boolean ok = false; for (Mat.Face q : faces) ok |= q == f;
            if (!ok || w < 3 || h < 3) return;
            Random r = new Random(seed * 31L + f.ordinal());
            for (int v = 0; v < 2; v++) {
                int col = v == 0 ? XG : XP, px = x + r.nextInt(w);
                for (int j = 0; j < h; j++) {
                    g.glow(px, y + j, col);
                    px += r.nextInt(3) - 1; px = Math.max(x, Math.min(x + w - 1, px));
                    if (r.nextInt(5) == 0) g.glow(Math.max(x, Math.min(x + w - 1, px + (r.nextBoolean() ? 1 : -1))), y + j, Mat.mix(col, 0xffffff, 0.3f));
                }
            }
        };
    }

    @Override
    public void buildModel(Rig rig) {
        Mat uni = Mat.of(0x23272e).noise(.1f), uniV = Mat.of(0x23272e).noise(.1f).decal(veinDecal(1, Mat.Face.FRONT, Mat.Face.BACK));
        Mat vest = Mat.of(0x3a414b).bevel(.3f).noise(.08f).decal(veinDecal(2, Mat.Face.FRONT));
        Mat plate = Mat.of(0x4a525c).bevel(.28f), black = Mat.of(0x141618), strap = Mat.of(0x2c2a26).noise(.12f);
        Mat mask = Mat.of(0x8b929b).bevel(.3f).noise(.06f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.FRONT) { g.rect(x + 1, y + h - 2, w - 2, 1, 0x50565e); g.glow(x + 1, y + 1, XG); }
        });
        Mat lens = Mat.of(XG).glow(), lensP = Mat.of(XP).glow(), veinG = Mat.of(XG).glow(), veinP = Mat.of(XP).glow();
        Mat growth = Mat.of(0x5a2a6e).noise(.3f).bevel(.35f).decal(veinDecal(3, Mat.Face.FRONT, Mat.Face.TOP, Mat.Face.LEFT));
        Mat band = Mat.of(0xd8d8d0), skinX = Mat.of(0x4a3a50).noise(.2f).decal(veinDecal(4, Mat.Face.FRONT, Mat.Face.LEFT, Mat.Face.RIGHT));

        // torso: dark uniform, combine vest, chest Xen wound
        rig.box(rig.torso, uniV, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, vest, -4, -12, -2.8f, 8, 7, 1, .2f);
        rig.box(rig.torso, plate, -4, -11, 2, 8, 7, 1);
        rig.box(rig.torso, veinG, 1, -9, -3.5f, 2, 2, 1);
        rig.box(rig.torso, veinP, 2, -8, -3.6f, 1, 3, 1);
        rig.box(rig.torso, strap, -4.5f, -2.5f, -2.5f, 9, 2, 5);
        rig.box(rig.torso, black, -3.5f, -2.8f, -3.3f, 2, 2, 1);
        rig.box(rig.torso, Mat.of(XG).glow(), 2, -2.8f, -3.4f, 1, 2, 1); // resin grenade on the belt
        rig.box(rig.torso, strap, -1, -13, -2.5f, 2, 1, 5);
        rig.box(rig.torso, black, -2, -10, 3, 4, 5, 2); // radio pack
        rig.box(rig.torso, lensP, -1, -9, 4.6f, 1, 1, 1);

        // head: metrocop mask with cracked Xen-lit lenses, hood
        rig.box(rig.head, uni, -4, -8, -4, 8, 8, 8, .3f);
        rig.box(rig.head, mask, -3.5f, -7, -4.6f, 7, 7, 2);
        rig.box(rig.head, lens, -3, -5.5f, -5.1f, 2, 2, 1);
        rig.box(rig.head, lensP, 1, -5.5f, -5.1f, 2, 2, 1);
        rig.box(rig.head, black, -1.5f, -2.5f, -6, 3, 3, 2); // respirator
        rig.box(rig.head, plate, -4.6f, -4, -1, 1, 2, 2);
        rig.box(rig.head, plate, 3.6f, -4, -1, 1, 2, 2);
        rig.box(rig.head, veinG, 3.7f, -7, -3, 1, 4, 1); // vein crawling up the left temple

        // right arm: armored sleeve, glove
        rig.box(rig.rArm, uni, -2, -2, -2, 4, 12, 4);
        rig.box(rig.rArm, plate, -3, -2.5f, -2.5f, 4, 4, 5);
        rig.box(rig.rArm, black, -2, 7, -2, 4, 3, 4, .2f);
        rig.box(rig.rArm, band, -2, 1.5f, -2, 4, 1, 4, .15f);
        // left arm: Xen-mutated, growth over the shoulder, veined forearm
        rig.box(rig.lArm, skinX, -2, -2, -2, 4, 12, 4);
        rig.box(rig.lArm, growth, -1.5f, -4, -3, 5, 4, 6);
        rig.box(rig.lArm, growth, 1.5f, -5, -1, 3, 3, 3);
        rig.box(rig.lArm, veinP, 0, -5.5f, -2, 2, 1, 2);
        rig.box(rig.lArm, veinG, 2.2f, 2, -1, 1, 4, 1);
        rig.box(rig.lArm, black, -2, 7, -2, 4, 3, 4, .2f);
        rig.box(rig.lArm, veinG, -0.5f, 8, -2.6f, 1, 2, 1);

        // legs
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, uni, -2, 0, -2, 4, 12, 4);
            rig.box(leg, plate, -2, 3, -2.7f, 4, 3, 1);
            rig.box(leg, black, -2, 9, -2.8f, 4, 3, 5, .2f);
        }
        rig.box(rig.lLeg, veinP, 1.6f, 1, -1, 1, 5, 1);
        rig.box(rig.rLeg, strap, -2.7f, 1, -1.5f, 1, 3, 3); // thigh holster

        buildWeapons(rig, black, plate);
    }

    private static void buildWeapons(Rig rig, Mat black, Mat plate) {
        Mat gun = Mat.of(0x34373b).noise(.08f), gunL = Mat.of(0x8a8c88).noise(.06f), wood = Mat.of(0x5a3a22).noise(.15f);
        Mat cmb = Mat.of(0x2e3a48).bevel(.3f), cyan = Mat.of(AR2C).glow(), red = Mat.of(0xff2020).glow(), orange = Mat.of(0xff8030).glow();
        Mat xg = Mat.of(XG).glow(), xp = Mat.of(XP).glow(), cloth = Mat.of(0x6a5a40).noise(.2f), olive = Mat.of(0x4a5530);
        PartDefinition w;
        w = rig.weapon(rig.rHand, "stunstick");
        rig.box(w, black, -0.5f, -0.5f, -3, 1, 1, 5, .2f); rig.box(w, gun, -0.5f, -0.5f, -14, 1, 1, 11);
        rig.box(w, xg, -1, -1, -10, 2, 2, 1); rig.box(w, xg, -1, -1, -7, 2, 2, 1); rig.box(w, xp, -1, -1, -17, 2, 2, 3);
        w = rig.weapon(rig.rHand, "shiv");
        rig.box(w, gunL, -0.5f, -1, -7, 1, 2, 6); rig.box(w, cloth, -0.5f, -0.5f, -1, 1, 1, 3, .15f);
        w = rig.weapon(rig.rHand, "usp");
        rig.box(w, gun, -1, -2.5f, -7, 2, 2, 8); rig.box(w, black, -1, -0.5f, -1, 2, 3, 2); rig.box(w, gunL, -0.5f, -2, -8, 1, 1, 1);
        w = rig.weapon(rig.rHand, "smg1");
        rig.box(w, gun, -1.5f, -3, -7, 3, 3, 8); rig.box(w, gunL, -0.5f, -2, -11, 1, 1, 4); rig.box(w, black, -1, 0, -10, 2, 2, 4);
        rig.box(w, black, -0.5f, 0, -5, 1, 4, 2); rig.box(w, gun, -1, -2.5f, 1, 2, 2, 4); rig.box(w, black, -0.5f, -4, -5, 1, 1, 3);
        w = rig.weapon(rig.rHand, "spas12");
        rig.box(w, gun, -1.5f, -2.5f, -6, 3, 3, 8); rig.box(w, gunL, -1, -3, -16, 2, 1, 10); rig.box(w, black, -1.5f, -2, -13, 3, 2, 5);
        rig.box(w, gun, -1, -1.5f, 2, 2, 2, 5); rig.box(w, black, -0.5f, 0.5f, -3, 1, 3, 2);
        w = rig.weapon(rig.rHand, "magnum");
        rig.box(w, gunL, -1.5f, -2.5f, -3, 3, 3, 3); rig.box(w, gunL, -1, -2.5f, -9, 2, 2, 6); rig.box(w, wood, -1, 0, -1, 2, 3, 2);
        rig.box(w, black, -0.5f, -3.5f, -1, 1, 1, 1);
        w = rig.weapon(rig.rHand, "ar2");
        rig.box(w, cmb, -2, -3.5f, -10, 4, 4, 13); rig.box(w, gun, -1, -2.5f, -16, 2, 2, 6); rig.box(w, cyan, -1, -4.5f, -5, 2, 1, 4);
        rig.box(w, cyan, -2.5f, -2.5f, -2, 1, 2, 2); rig.box(w, black, -1, 0.5f, -4, 2, 3, 2);
        w = rig.weapon(rig.rHand, "crossbow");
        rig.box(w, wood, -1, -2, -9, 2, 2, 13); rig.box(w, gun, -6, -2.5f, -11, 12, 1, 2); rig.box(w, orange, -0.5f, -3, -17, 1, 1, 11);
        rig.box(w, black, -1, -5, -6, 2, 2, 5); rig.box(w, black, -0.5f, 0, -3, 1, 3, 2);
        w = rig.weapon(rig.rHand, "rpg");
        rig.box(w, olive, -1.5f, -3.5f, -12, 3, 3, 17); rig.box(w, gun, -2, -4, -13, 4, 4, 1); rig.box(w, red, -0.5f, -5, -10, 1, 1, 2);
        rig.box(w, black, -0.5f, 0, -1, 1, 3, 2); rig.box(w, black, -1, -5.5f, -4, 2, 2, 3);
        w = rig.weapon(rig.rHand, "zpg");
        rig.box(w, plate, -2.5f, -4, -8, 5, 5, 10); rig.box(w, gunL, -3, -3, -13, 1, 1, 5); rig.box(w, gunL, 2, -3, -13, 1, 1, 5);
        rig.box(w, gunL, -0.5f, -5, -13, 1, 1, 5); rig.box(w, orange, -1, -2.5f, -10, 2, 2, 2); rig.box(w, black, -1, 1, -3, 2, 3, 2);
        w = rig.weapon(rig.rHand, "syringe");
        rig.box(w, xg, -0.5f, -0.5f, -4, 1, 1, 4); rig.box(w, gunL, -0.5f, -0.5f, 0, 1, 1, 2); rig.box(w, gunL, -0.5f, -0.5f, -6, 1, 1, 2, -0.3f);
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, Mat.of(0x3c4a30), -1, -1, -2, 2, 2, 2); rig.box(w, red, -0.5f, -1.8f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "resin");
        rig.box(w, xp, -1, -1, -2, 2, 2, 2, .2f); rig.box(w, xg, -0.5f, -1.9f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "cball");
        rig.box(w, Mat.of(CBALL).glow(), -1.5f, -1.5f, -3, 3, 3, 3); rig.box(w, xp, -1, -1, -3.4f, 2, 2, 2, .4f);
    }
}
