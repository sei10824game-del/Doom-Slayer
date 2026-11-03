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
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Master Chief, Halo 4: Mark VI GEN2 armor, UNSC + Promethean arsenal, energy shields, armor abilities. */
public class Halo4Chief extends SlayerKit {
    static final int OLIVE = 0x4f5c38, VISOR = 0xe8a838, SHIELD_C = 0xf0c040, THRUST = 0x7ad0ff,
            PRO = 0xff8a1a, TRACER = 0xffe090, SWORD_C = 0x7fd8ff, PLASMA_C = 0x4aa8ff, LASER = 0xff2a2a;
    static final float SHIELD_MAX = 24f;

    /** shield state per Chief: {shield, ticksSinceHit, lastHealth} */
    static final Map<SlayerEntity, float[]> SHIELDS = new WeakHashMap<>();
    /** sticky detonator charge: entity it stuck to (nullable) + world position */
    record Stick(Entity on, Vec3 pos) {}
    static final Map<SlayerEntity, Stick> STUCK = new WeakHashMap<>();
    /** live pulse grenade fields: {x, y, z, ticksLeft} */
    static final Map<SlayerEntity, double[]> PULSES = new WeakHashMap<>();

    static final ProjStyle SCATTER = ProjStyle.make().color(PRO).size(0.16f).life(16).bounce().trail(ParticleTypes.SMALL_FLAME);
    static final ProjStyle SUPPRESS = ProjStyle.make().color(0xff7010).size(0.18f).life(28).trail(ParticleTypes.SMALL_FLAME);
    static final ProjStyle FRAG = ProjStyle.make().color(0x4a5a3a).size(0.26f).life(34).explode(3.5f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle PLASMA_NADE = ProjStyle.make().color(PLASMA_C).size(0.3f).life(50).gravity(0.035).homing(0.22f).explode(3.2f)
            .trail(ParticleTypes.ELECTRIC_SPARK)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 2.5f); Fx.dustBurst(l, PLASMA_C, 2f, pos, 40, 1.4);
                Fx.ring(l, pos, 2.2f, 0x9ad0ff, 1.4f, 26);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1.3f, 1.3f); Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1f, 1.8f);
            });
    static final ProjStyle INCIN = ProjStyle.make().color(0xff6010).size(0.6f).life(80).explode(3f).trail(ParticleTypes.FLAME)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 3.5f); Fx.dustBurst(l, PRO, 2.2f, pos, 40, 1.5);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 2f, 0.9f);
                if (!(p.getOwner() instanceof SlayerEntity o)) return;
                for (int i = 0; i < 4; i++) { // cluster sub-munitions burst around the impact
                    double a = i * Math.PI / 2 + 0.4;
                    Vec3 c = pos.add(Math.cos(a) * 2.2, 0.3, Math.sin(a) * 2.2);
                    Fx.explosion(l, c, 1.6f); Fx.burst(l, ParticleTypes.FLAME, c, 14, 0.5, 0.05);
                    Combat.blast(o, c, 2.2f, 8f, 0.6f, null);
                }
            });
    static final ProjStyle STICKY = ProjStyle.make().color(0xc8ff40).size(0.22f).life(60).gravity(0.04).trail(ParticleTypes.SMOKE)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                if (p.getOwner() instanceof SlayerEntity o) STUCK.put(o, new Stick(hit, pos));
                Fx.dustBurst(l, 0xc8ff40, 1f, pos, 8, 0.15);
                Fx.sound(l, pos, SoundEvents.SLIME_BLOCK_PLACE, 1f, 1.4f);
            });
    static final ProjStyle PULSE = ProjStyle.make().color(PRO).size(0.26f).life(40).gravity(0.05).trail(ParticleTypes.SMALL_FLAME)
            .onHit((p, pos, hit) -> {
                if (p.getOwner() instanceof SlayerEntity o) PULSES.put(o, new double[]{pos.x, pos.y, pos.z, 50});
                Fx.sound((ServerLevel) p.level(), pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1f, 1.6f);
            });

    public Halo4Chief() {
        super("halo4_chief", "Master Chief", "マスターチーフ", "Halo 4");
        eggPrimary = OLIVE; eggSecondary = VISOR;
        maxHealth = 90; speed = 0.33; armor = 14; knockbackRes = 0.7;
        scale = 1.12f; width = 0.7f; preferredRange = 12f; dmgMult = 1.05f;
        idleWeaponR = "br85";
    }

    // ================= helpers (server) =================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.2, 0.32, 0); }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        return Combat.dirTo(mz(e), t, spread, new Random(e.getRandom().nextLong()));
    }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static Vec3 endOf(SlayerEntity e, LivingEntity t, Vec3 dir, double range) { return t != null ? mid(t) : mz(e).add(dir.scale(range)); }
    static void flash(SlayerEntity e, int rgb) {
        Vec3 m = mz(e);
        Fx.burst(L(e), ParticleTypes.FLAME, m, 2, 0.04, 0.02);
        Fx.dustBurst(L(e), rgb, 1.1f, m, 3, 0.07);
    }
    static void casing(SlayerEntity e) { Fx.burst(L(e), ParticleTypes.SMOKE, Combat.muzzle(e, 0.4, 0.5, 0.1), 1, 0.03, 0.03); }
    static void bullet(SlayerEntity e, LivingEntity t, double spread, double range, float dmg) {
        Combat.hitscan(e, mz(e), aim(e, t, spread), range, dmg, 0, TRACER); flash(e, 0xffb050); casing(e);
    }
    static void hold(SlayerEntity e, LivingEntity t, double dist, double lift) {
        if (t == null || !Combat.isFoe(e, t)) return;
        Vec3 f = Vec3.directionFromRotation(0, e.getYRot());
        Vec3 p = e.position().add(f.scale(dist + t.getBbWidth() * 0.5)).add(0, lift, 0);
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true;
    }
    static float[] shield(SlayerEntity e) { return SHIELDS.computeIfAbsent(e, k -> new float[]{SHIELD_MAX, 999, e.getHealth()}); }

    // ================= passive: recharging energy shield + pulse grenade fields =================
    @Override
    public void passive(SlayerEntity e) {
        float[] s = shield(e);
        float hp = e.getHealth(), drop = s[2] - hp;
        s[1]++;
        if (drop > 0.01f) {
            s[1] = 0;
            if (s[0] > 0) {
                s[0] -= drop * 3;
                e.heal(drop * 0.5f); // shield soaks part of the hit
                Fx.dustBurst(L(e), SHIELD_C, 1.2f, e.position().add(0, 1.1, 0), 10, 0.5);
                snd(e, SoundEvents.AMETHYST_BLOCK_HIT, 0.9f, 1.6f);
                if (s[0] <= 0) {
                    s[0] = 0;
                    Fx.dustBurst(L(e), SHIELD_C, 1.6f, e.position().add(0, 1.1, 0), 30, 0.7);
                    Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, e.position().add(0, 1.1, 0), 20, 0.5, 0.2);
                    snd(e, SoundEvents.GLASS_BREAK, 1.2f, 0.8f);
                }
            }
        }
        s[2] = e.getHealth();
        if (s[1] > 100 && s[0] < SHIELD_MAX) { // recharge after 5 s without damage
            if ((int) s[1] == 101) snd(e, SoundEvents.BEACON_POWER_SELECT, 1f, 1.5f);
            s[0] = Math.min(SHIELD_MAX, s[0] + 0.5f);
            if (e.tickCount % 3 == 0) Fx.ring(L(e), e.position().add(0, 0.2 + (s[0] / SHIELD_MAX) * 1.8, 0), 0.6f, SHIELD_C, 0.9f, 10);
            if (s[0] >= SHIELD_MAX) snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.2f);
        }
        if (s[0] > 0) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 1, false, false));

        double[] pg = PULSES.get(e);
        if (pg != null) {
            Vec3 c = new Vec3(pg[0], pg[1] + 0.3, pg[2]);
            float r = (float) (1 + 2.5 * (1 - pg[3] / 50));
            if (e.tickCount % 2 == 0) { Fx.ring(L(e), c, r, PRO, 1.2f, 24); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, c, 3, r * 0.4, 0.05); }
            if ((int) pg[3] % 8 == 0) {
                snd(e, SoundEvents.BEACON_AMBIENT, 0.8f, 1.8f);
                for (LivingEntity f : Combat.around(e, c, r)) Combat.damage(e, f, 3f);
            }
            if (--pg[3] <= 0) {
                Combat.blast(e, c, 3.5f, 9f, 0.8f, null);
                Fx.dustBurst(L(e), PRO, 2f, c, 40, 1.6); Fx.burst(L(e), ParticleTypes.FLASH, c, 1, 0, 0);
                snd(e, SoundEvents.GENERIC_EXPLODE, 1f, 1.7f);
                PULSES.remove(e);
            }
        }
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** shouldered rifle, bladed stance */
    static void rifle(Pose p, float k, float pitch, float recoil) {
        float tw = 0.22f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.15f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.05f - recoil * 0.22f, k); p.lArmY = lerp(p.lArmY, 0.45f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * k; p.bodyX += (recoil * 0.12f - pitch * 0.2f) * k; p.headX -= recoil * 0.08f * k;
        p.rootZ += recoil * 0.9f * k;
    }
    /** heavy weapon at the hip */
    static void hip(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -1.0f + pitch - recoil * 0.2f, k); p.rArmY = lerp(p.rArmY, -0.2f, k); p.rArmZ = lerp(p.rArmZ, 0.15f, k);
        p.lArmX = lerp(p.lArmX, -1.3f + pitch - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.7f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX = lerp(p.rHandX, -0.55f, k);
        p.bodyX += (0.12f + recoil * 0.1f) * k; p.bodyY += 0.15f * k; p.headY -= 0.15f * k; p.rootZ += recoil * 0.7f * k;
    }
    /** pistol held in both hands, arms extended close together */
    static void pistol(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.4f, k); p.rArmY = lerp(p.rArmY, 0.12f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.1f - recoil * 0.35f, k); p.lArmY = lerp(p.lArmY, 0.62f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX -= recoil * 0.35f * k; p.headX -= recoil * 0.05f * k; p.bodyX -= recoil * 0.06f * k; p.rootZ += recoil * 0.5f * k;
    }
    /** shoulder-mounted heavy launcher */
    static void shoulder(Pose p, float k, float recoil) {
        p.rArmX = lerp(p.rArmX, -HALF - 0.35f - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.3f, k); p.rArmZ = lerp(p.rArmZ, 0.35f, k);
        p.lArmX = lerp(p.lArmX, -HALF - 0.05f - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.55f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX = lerp(p.rHandX, 0.3f, k); p.headY -= 0.2f * k; p.headZ += 0.12f * k; p.bodyY += 0.2f * k;
        p.bodyX -= recoil * 0.2f * k; p.rootZ += recoil * 1.1f * k;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.32f, k); p.rLegX = lerp(p.rLegX, 0.28f, k);
        p.lLegZ -= 0.06f * k; p.rLegZ += 0.08f * k; p.rootY -= 0.4f * k;
    }
    static void wide(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.2f, k); p.rLegX = lerp(p.rLegX, 0.15f, k);
        p.lLegZ -= 0.3f * k; p.rLegZ += 0.3f * k; p.rootY -= 1.2f * k;
    }
    static void kneel(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -1.25f, k); p.rLegX = lerp(p.rLegX, 1.1f, k); p.rootY -= 3.6f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // BR85 at low ready, muzzle angled down-left, both hands on it
        p.rArmX = -0.6f + p.rArmX * 0.25f; p.rArmY = -0.3f;
        p.lArmX = -0.85f + p.lArmX * 0.25f; p.lArmY = 0.6f; p.lArmZ = 0.05f;
        p.rHandX = -0.3f; p.bodyX += 0.03f;
        p.headX += 0.02f * (float) Math.sin(age * 0.08f); // slow scan
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- Rifle melee: butt-stroke ----------
        out.add(AttackDef.of("rifle_bash").range(0, 3.2f).cd(20).dur(16).weapon("br85")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.8f, 0.8f);
                    if (n == 6) {
                        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.3f, 50, -20, -20, 0xd0d0c0, 0.7f, 8);
                        for (LivingEntity f : Combat.cone(e, 3.3, 50)) {
                            Combat.damage(e, f, 10); Combat.knock(f, e.position(), 1.0, 0.3); Fx.sparks(L(e), mid(f), 8);
                        }
                        snd(e, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.7f, 1.4f); snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -1.2f, .25f, -0.6f, .38f, -1.9f, .55f, -1.7f, 1, -1.1f);
                    p.rArmZ = track(t, 0, 0, .25f, 0.5f, .38f, -0.3f, .6f, -0.2f, 1, 0); p.rArmY = -0.2f;
                    p.lArmX = track(t, 0, -1.3f, .25f, -0.9f, .38f, -1.75f, 1, -1.2f); p.lArmY = 0.5f;
                    p.rHandX = track(t, 0, -0.3f, .25f, 0.45f, .38f, -0.95f, .6f, -0.7f, 1, -0.3f);
                    p.bodyY = track(t, 0, 0, .25f, -0.45f, .38f, 0.5f, .6f, 0.35f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyX += track(t, 0, 0, .25f, -0.1f, .38f, 0.3f, .7f, 0.2f, 1, 0);
                    p.rootZ = track(t, 0, 0, .25f, 0.8f, .38f, -2.4f, .6f, -2f, 1, 0);
                    p.rootY = -0.5f * bump(win(t, .3f, .8f));
                    p.lLegX = track(t, 0, 0, .25f, 0.1f, .38f, -0.6f, .7f, -0.5f, 1, 0);
                    p.rLegX = track(t, 0, 0, .38f, 0.45f, .7f, 0.35f, 1, 0);
                }));

        // ---------- Assassination: grab, knee, rifle-butt hammer onto the skull ----------
        out.add(AttackDef.of("assassination").range(0, 3.5f).lowHealthTarget(0.25f).weight(6).cd(80).dur(32).weapon("br85")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.45, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.7f); }
                    hold(e, t, 0.9, 0);
                    if (n == 8 && t != null) {
                        Combat.damage(e, t, Math.max(0, t.getHealth() - 1)); Fx.sparks(L(e), mid(t), 8);
                        snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f);
                    }
                    if (n == 21 && t != null) {
                        Combat.damage(e, t, t.getHealth() + 100);
                        Fx.burst(L(e), ParticleTypes.DAMAGE_INDICATOR, mid(t), 10, 0.3, 0.1); Fx.sparks(L(e), mid(t), 14);
                        snd(e, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1f, 1.2f); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.7f);
                        shield(e)[0] = SHIELD_MAX; // shields snap back after a clean kill
                        snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    // left hand grabs and pins; right holds the rifle
                    p.lArmX = track(t, 0, -0.8f, .12f, -1.6f, .55f, -1.5f, .62f, -2.7f, .66f, -1.0f, .85f, -1.0f, 1, -0.8f);
                    p.lArmY = track(t, 0, 0.5f, .12f, 0.2f, .55f, 0.2f, .62f, 0.5f, 1, 0.5f);
                    p.rArmX = track(t, 0, -0.6f, .25f, -0.8f, .55f, -1.0f, .62f, -2.8f, .66f, -0.9f, .85f, -0.9f, 1, -0.6f);
                    p.rArmZ = track(t, 0, 0, .55f, 0.2f, .62f, 0.1f, .66f, -0.1f, 1, 0);
                    p.rHandX = track(t, 0, -0.3f, .55f, -0.3f, .62f, 1.2f, .66f, 1.4f, .85f, 0.8f, 1, -0.3f); // butt-first downstroke
                    // knee strike
                    p.rLegX = track(t, 0, 0, .15f, 0.4f, .25f, -1.7f, .35f, -0.4f, .45f, 0.2f, .66f, 0.4f, 1, 0);
                    p.lLegX = track(t, 0, 0, .12f, -0.3f, .66f, -0.45f, 1, 0);
                    p.bodyX = track(t, 0, 0, .15f, -0.15f, .25f, 0.3f, .45f, 0.05f, .6f, -0.25f, .66f, 0.55f, .85f, 0.45f, 1, 0);
                    p.bodyY = track(t, 0, 0, .25f, -0.2f, .45f, 0, .6f, 0.3f, .66f, -0.1f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.headX = track(t, .55f, 0, .62f, -0.25f, .7f, 0.4f, 1, 0);
                    p.rootY = track(t, 0, 0, .2f, 0.6f, .35f, 0, .6f, 0.3f, .7f, -1.6f, .85f, -1.3f, 1, 0);
                    p.rootZ = track(t, 0, 0, .12f, -1.8f, .85f, -1.8f, 1, 0);
                }));

        // ---------- BR85 Battle Rifle: two 3-round bursts ----------
        out.add(AttackDef.of("br85_burst").range(4, 30).cd(26).weight(4).dur(26).weapon("br85")
                .tick((e, t, n) -> {
                    if ((n >= 4 && n <= 6) || (n >= 15 && n <= 17)) {
                        bullet(e, t, 0.012, 30, 3.4f);
                        if (n == 4 || n == 15) snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 1.2f, 1.25f);
                        else snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 1.35f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .88f), r = 0;
                    for (int s : new int[]{4, 5, 6, 15, 16, 17}) r += 0.7f * kick(t, s / 26f, .1f);
                    rifle(p, k, -0.03f * r, r); stance(p, k);
                    p.rHandX -= r * 0.2f; p.headX += 0.08f * k; // cheek weld
                    float reaim = bump(win(t, .3f, .55f)); p.headY += 0.06f * reaim; p.bodyY -= 0.05f * reaim; // re-acquire between bursts
                }));

        // ---------- DMR: scoped precision shots ----------
        out.add(AttackDef.of("dmr").range(8, 40).cd(40).weight(3).dur(32).weapon("dmr")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.SPYGLASS_USE, 1, 1.1f);
                    if (n == 9 || n == 17 || n == 25) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.004), 42, 7.5f, 0, TRACER); flash(e, 0xffb050); casing(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1f, 1.5f);
                    }
                    if (n == 29) snd(e, SoundEvents.SPYGLASS_STOP_USING, 1, 1.1f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), zoom = win(t, .1f, .25f) * (1 - win(t, .85f, .95f));
                    float r = 1.3f * (kick(t, 9 / 32f, .14f) + kick(t, 17 / 32f, .14f) + kick(t, 25 / 32f, .14f));
                    rifle(p, k, -0.06f * r, r); stance(p, k);
                    p.headX += 0.2f * zoom; p.headY -= 0.12f * zoom; p.bodyX += 0.1f * zoom; p.rootY -= 0.5f * zoom;
                    p.rHandX -= r * 0.25f; p.bodyZ += 0.012f * (float) Math.sin(a * 0.35f) * zoom; // breathing
                }));

        // ---------- MA5D Assault Rifle: full-auto spray with muzzle climb ----------
        out.add(AttackDef.of("assault_rifle").range(2, 18).cd(34).weight(3).dur(36).weapon("ar")
                .tick((e, t, n) -> {
                    if (n == 1) snd(e, SoundEvents.ARMOR_EQUIP_CHAIN, 0.8f, 1.2f);
                    if (n >= 5 && n <= 30) {
                        bullet(e, t, 0.05 + (n - 5) * 0.002, 22, 1.5f);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.75f, 1.55f + e.getRandom().nextFloat() * 0.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), fire = win(t, .13f, .15f) * (1 - win(t, .84f, .86f));
                    float climb = -0.18f * win(t, .14f, .5f) + 0.1f * win(t, .5f, .85f); // climbs, then dragged back down
                    float jit = 0.3f * (0.5f + 0.5f * (float) Math.sin(a * 2.8f)) * fire;
                    rifle(p, k, climb * fire, jit); stance(p, k);
                    p.rHandZ += 0.03f * (float) Math.sin(a * 3.7f) * fire; p.bodyZ += 0.02f * (float) Math.cos(a * 3.1f) * fire;
                    p.lLegX -= 0.15f * fire; p.bodyX += 0.08f * fire; // lean into it
                }));

        // ---------- M739 SAW: braced long burst ----------
        out.add(AttackDef.of("saw").range(3, 22).cd(150).weight(2).dur(64).weapon("saw")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.9f, 0.8f);
                    if (n >= 10 && n <= 56) {
                        bullet(e, t, 0.06, 26, 1.8f);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, 1.1f + e.getRandom().nextFloat() * 0.25f);
                        if (n % 3 == 0) snd(e, SoundEvents.GENERIC_EXPLODE, 0.15f, 2f);
                    }
                    if (n == 58) Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, mz(e), 6, 0.1, 0.02);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .92f), brace = win(t, .05f, .16f) * (1 - win(t, .9f, 1));
                    float fire = win(t, .15f, .16f) * (1 - win(t, .87f, .88f)), jit = 0.06f * (float) Math.sin(a * 6.3f) * fire;
                    hip(p, k, -0.2f * brace, 0.3f * fire + jit);
                    wide(p, brace); p.bodyX += 0.15f * brace; p.headX += 0.1f * brace;
                    p.rHandZ += jit; p.bodyZ += jit * 0.5f; p.rArmY += jit * 0.5f;
                    p.bodyY += 0.25f * (float) Math.sin(t * PI * 2.5f) * fire; p.headY -= 0.15f * (float) Math.sin(t * PI * 2.5f) * fire; // sweeping fire
                }));

        // ---------- Boltshot: semi-auto hardlight pistol ----------
        out.add(AttackDef.of("boltshot").range(2, 16).cd(22).weight(2).dur(22).weapon("boltshot")
                .tick((e, t, n) -> {
                    if (n == 4 || n == 10 || n == 16) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.02), 18, 4f, 0, PRO); flash(e, PRO);
                        snd(e, SoundEvents.BLAZE_SHOOT, 0.6f, 1.9f); snd(e, SoundEvents.AMETHYST_BLOCK_HIT, 0.8f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .85f);
                    float r = kick(t, 4 / 22f, .14f) + kick(t, 10 / 22f, .14f) + kick(t, 16 / 22f, .14f);
                    pistol(p, k, 0, r);
                    p.bodyY -= 0.15f * k; p.headY += 0.15f * k; // squared up, shooting from the shoulder line
                    p.lLegX = lerp(p.lLegX, -0.25f, k); p.rLegX = lerp(p.rLegX, 0.2f, k); p.rootY -= 0.3f * k;
                }));

        // ---------- Boltshot: overcharged shotgun blast ----------
        out.add(AttackDef.of("boltshot_overcharge").range(0, 6).cd(90).weight(2).dur(30).weapon("boltshot")
                .tick((e, t, n) -> {
                    if (n < 16) {
                        Vec3 m = mz(e); double rr = 0.6 - n * 0.035;
                        Fx.dustBurst(L(e), PRO, 0.9f, m, 3, Math.max(0.05, rr));
                        if (n % 3 == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.6f, 1f + n * 0.06f);
                    }
                    if (n == 17) {
                        Vec3 m = mz(e);
                        for (int i = 0; i < 9; i++) Combat.hitscan(e, m, aim(e, t, 0.12), 8, 3f, 0, PRO);
                        for (LivingEntity f : Combat.cone(e, 4, 40)) Combat.knock(f, e.position(), 1.1, 0.3);
                        Fx.dustBurst(L(e), PRO, 1.6f, m, 20, 0.4); Fx.burst(L(e), ParticleTypes.FLASH, m, 1, 0, 0);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 0.7f); snd(e, SoundEvents.AMETHYST_CLUSTER_BREAK, 1f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), ch = win(t, .05f, .5f) * (1 - win(t, .55f, .57f)), r = 2.2f * kick(t, 17 / 30f, .3f);
                    pistol(p, k, 0.2f * ch, r);
                    // charge: gun drawn in toward the chest, crouched; release: punch it out
                    p.rArmX += 0.5f * ch; p.lArmX += 0.45f * ch; p.rHandX -= 0.3f * ch;
                    p.bodyZ += 0.03f * ch * ch * (float) Math.sin(a * 5f);
                    p.rootY -= 0.9f * ch; p.bodyX += 0.15f * ch;
                    p.rootZ += track(t, 0, 0, .5f, 0.5f, .57f, -1.6f, .8f, -0.8f, 1, 0) + r * 0.6f;
                    p.lLegX = track(t, 0, 0, .5f, -0.25f, .57f, -0.6f, .85f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .57f, 0.45f, .85f, 0.35f, 1, 0);
                }));

        // ---------- Light Rifle: weapon unfolds when scoped, piercing hardlight bolt ----------
        out.add(AttackDef.of("light_rifle").range(8, 40).cd(50).weight(3).dur(32).weapon("lightrifle")
                .tick((e, t, n) -> {
                    if (n == 6) { snd(e, SoundEvents.PISTON_EXTEND, 0.7f, 1.7f); snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.8f); }
                    if (n > 8 && n < 20) Fx.dustBurst(L(e), PRO, 0.6f, mz(e), 1, 0.06);
                    if (n == 20) {
                        Vec3 d = aim(e, t, 0.002), m = mz(e);
                        Combat.hitscan(e, m, d, 44, 14f, 1, PRO);
                        Fx.beam(L(e), m, endOf(e, t, d, 44), 0xffd090, 0.7f, 0.35);
                        snd(e, SoundEvents.TRIDENT_RIPTIDE_1, 1f, 1.8f); snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 1f, 0.9f);
                    }
                    if (n == 27) snd(e, SoundEvents.PISTON_CONTRACT, 0.6f, 1.7f);
                })
                .anim((p, t, a) -> {
                    p.weaponR = t > 0.19f && t < 0.84f ? "lightrifle_open" : "lightrifle";
                    float k = ready(t, .14f, .9f), zoom = win(t, .2f, .34f) * (1 - win(t, .78f, .88f)), r = 1.5f * kick(t, 20 / 32f, .2f);
                    rifle(p, k, 0, r); stance(p, k);
                    p.rHandY += 0.35f * bump(win(t, .12f, .22f)); // twist as the frame splits open
                    p.headX += 0.22f * zoom; p.headY -= 0.12f * zoom; p.bodyX += 0.1f * zoom; p.rootY -= 0.6f * zoom;
                    p.rHandX -= r * 0.3f;
                }));

        // ---------- Binary Rifle: laser-sighted kneeling one-shot ----------
        out.add(AttackDef.of("binary_rifle").range(12, 50).cd(200).weight(2).dur(48).weapon("binary")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.WARDEN_SONIC_CHARGE, 0.5f, 1.8f);
                    if (n >= 6 && n < 36 && n % 2 == 0) {
                        Vec3 d = aim(e, t, 0), m = mz(e);
                        Fx.beam(L(e), m, endOf(e, t, d, 50), LASER, 0.35f, 0.8);
                    }
                    if (n == 36) {
                        Vec3 d = aim(e, t, 0), m = mz(e);
                        Combat.hitscan(e, m, d, 52, 34f, 2, LASER);
                        Fx.beam(L(e), m, endOf(e, t, d, 52), 0xff9090, 1.1f, 0.3);
                        Fx.burst(L(e), ParticleTypes.FLASH, m, 1, 0, 0);
                        snd(e, SoundEvents.TRIDENT_THUNDER, 0.6f, 1.9f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 0.5f);
                    }
                    if (n == 42) snd(e, SoundEvents.PISTON_CONTRACT, 0.8f, 1.2f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .92f), kn = win(t, .02f, .16f) * (1 - win(t, .86f, .98f));
                    float zoom = win(t, .12f, .25f) * (1 - win(t, .8f, .9f)), r = 2.4f * kick(t, 36 / 48f, .25f);
                    rifle(p, k, 0, r); kneel(p, kn);
                    p.headX += 0.22f * zoom; p.headY -= 0.14f * zoom; p.bodyX += 0.18f * zoom;
                    p.bodyZ += 0.01f * (float) Math.sin(a * 0.3f) * zoom; // held breath sway
                    p.rHandX -= r * 0.35f; p.bodyX -= r * 0.15f; p.rootZ += r * 0.5f;
                }));

        // ---------- Scattershot: ricocheting hardlight pellets ----------
        out.add(AttackDef.of("scattershot").range(0, 10).cd(30).weight(3).dur(26).weapon("scattershot")
                .tick((e, t, n) -> {
                    if (n == 7) {
                        Vec3 m = mz(e);
                        for (int i = 0; i < 8; i++) Combat.shoot(e, SCATTER, m, aim(e, t, 0.09), 1.4, 2.6f);
                        Fx.dustBurst(L(e), PRO, 1.4f, m, 10, 0.2); Fx.burst(L(e), ParticleTypes.FLAME, m, 4, 0.1, 0.03);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.2f, 0.8f); snd(e, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9f, 1.2f);
                    }
                    if (n == 14) snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.7f, 1.6f);
                    if (n == 19) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.7f, 1.7f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .18f, .9f), r = 2f * kick(t, 7 / 26f, .3f);
                    rifle(p, k, 0.05f, r); stance(p, k);
                    p.rHandX -= r * 0.45f; p.bodyX -= r * 0.18f; p.rootY += r * 0.3f;
                    // frame cycles: weapon rolled, left hand slaps the top shut
                    float cyc = bump(win(t, .5f, .78f));
                    p.rHandY -= 0.4f * cyc; p.lArmX -= 0.35f * cyc; p.lArmZ -= 0.15f * cyc; p.headX += 0.08f * cyc;
                }));

        // ---------- Suppressor: Promethean automatic ----------
        out.add(AttackDef.of("suppressor").range(2, 20).cd(32).weight(3).dur(36).weapon("suppressor")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.5f, 2f);
                    if (n >= 5 && n <= 29 && n % 2 == 1) {
                        Combat.shoot(e, SUPPRESS, mz(e), aim(e, t, 0.05), 1.6, 2.2f);
                        Fx.dustBurst(L(e), PRO, 0.8f, mz(e), 2, 0.05);
                        snd(e, SoundEvents.BLAZE_SHOOT, 0.45f, 1.7f + e.getRandom().nextFloat() * 0.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .13f, .15f) * (1 - win(t, .8f, .83f));
                    float r = 0.35f * (0.5f + 0.5f * (float) Math.sin(a * PI)) * fire;
                    hip(p, k, -0.25f * k, r); stance(p, k);
                    p.rHandX += 0.25f * k; // raised to waist-aim
                    p.rHandZ += 0.03f * (float) Math.sin(a * 3.3f) * fire; p.bodyZ += 0.02f * (float) Math.sin(a * 2.1f) * fire;
                    p.bodyY -= 0.2f * (float) Math.sin(t * PI * 2) * fire; p.headY += 0.12f * (float) Math.sin(t * PI * 2) * fire; // tracking sweep
                }));

        // ---------- Incineration Cannon: charged cluster bomb ----------
        out.add(AttackDef.of("incineration_cannon").range(7, 32).cd(320).weight(1).dur(44).weapon("incin")
                .when((e, t) -> t.getMaxHealth() >= 40 || Combat.around(e, t.position(), 5).size() >= 3)
                .tick((e, t, n) -> {
                    if (n < 22) {
                        Vec3 m = mz(e); double ang = n * 0.8, rr = 0.9 - n * 0.035;
                        Fx.dustBurst(L(e), PRO, 1.2f, m.add(Math.cos(ang) * rr, Math.sin(ang) * rr, 0), 2, 0.04);
                        Fx.burst(L(e), ParticleTypes.FLAME, m, 1, 0.1, 0.01);
                        if (n % 4 == 0) snd(e, SoundEvents.FIRECHARGE_USE, 0.6f, 0.6f + n * 0.04f);
                    }
                    if (n == 24) {
                        Combat.shoot(e, INCIN, mz(e), aim(e, t, 0.005), 1.0, 22f);
                        Fx.dustBurst(L(e), PRO, 2f, mz(e), 20, 0.4); Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.5, 0.35, 0.1), 8, 0.2, 0.04);
                        e.setDeltaMovement(e.getDeltaMovement().subtract(e.getViewVector(1f).scale(0.35))); e.hurtMarked = true;
                        snd(e, SoundEvents.GHAST_SHOOT, 1.2f, 0.6f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.7f, 1.4f);
                    }
                    if (n == 34) snd(e, SoundEvents.PISTON_EXTEND, 0.8f, 0.8f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .9f), ch = win(t, .05f, .52f) * (1 - win(t, .54f, .55f)), r = 3f * kick(t, 24 / 44f, .3f);
                    shoulder(p, k, r); wide(p, k * 0.7f);
                    p.bodyX += 0.1f * ch; p.headX += 0.1f * ch; p.rootY -= 0.4f * ch;
                    p.bodyZ += 0.04f * ch * ch * (float) Math.sin(a * 5.5f);
                    p.rLegX += 0.4f * r * k; p.headX -= r * 0.08f;
                    p.lArmX += 0.3f * bump(win(t, .74f, .9f)); // racks the reload lever
                }));

        // ---------- Sticky Detonator: fire charge, then remote-detonate ----------
        out.add(AttackDef.of("sticky_detonator").range(5, 22).cd(130).weight(2).dur(36).weapon("sticky")
                .onStart((e, t, n) -> STUCK.remove(e))
                .tick((e, t, n) -> {
                    if (n == 6) {
                        Vec3 from = mz(e); double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, t, 0.01).add(0, 0.05 + dist * 0.012, 0).normalize();
                        Combat.shoot(e, STICKY, from, d, 0.9 + dist * 0.02, 1f);
                        Fx.burst(L(e), ParticleTypes.SMOKE, from, 5, 0.08, 0.02);
                        snd(e, SoundEvents.CROSSBOW_SHOOT, 1f, 0.7f); snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.6f, 1.4f);
                    }
                    if (n == 18 || n == 21) snd(e, SoundEvents.NOTE_BLOCK_PLING.value(), 0.7f, 1.8f);
                    if (n == 25) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1f, 1.2f);
                    if (n == 26) {
                        Stick s = STUCK.remove(e);
                        if (s != null) {
                            Vec3 c = s.on() != null && s.on().isAlive() ? s.on().position().add(0, s.on().getBbHeight() * 0.5, 0) : s.pos();
                            Combat.blast(e, c, 4f, 18f, 1.3f, null);
                            Fx.explosion(L(e), c, 3.5f); Fx.ring(L(e), c, 2.5f, 0xffc060, 1.6f, 28);
                            Fx.sound(L(e), c, SoundEvents.GENERIC_EXPLODE, 1.8f, 0.9f);
                        }
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), r = 1.6f * kick(t, 6 / 36f, .25f);
                    float lob = -0.2f * win(t, .05f, .16f) * (1 - win(t, .3f, .45f));
                    rifle(p, k, lob, r); stance(p, k);
                    p.rHandX -= r * 0.4f;
                    // watches the charge, then thumbs the trigger: hand rolls + sharp nod
                    float press = bump(win(t, .64f, .76f));
                    p.rHandY += 0.5f * press; p.rArmX -= 0.12f * press; p.headX += 0.15f * press; p.bodyX += 0.06f * press;
                    p.headY += 0.08f * win(t, .25f, .5f) * (1 - win(t, .6f, .7f));
                }));

        // ---------- M9 Frag grenade: overhand throw ----------
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(150).weight(2).dur(22).weapons("br85", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.5f);
                    if (n == 11) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.4); double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, t, 0.02).add(0, 0.15 + dist * 0.018, 0).normalize();
                        Combat.shoot(e, FRAG, from, d, 0.75 + dist * 0.025, 14f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .52f ? "frag" : null;
                    p.rArmX = -0.7f; p.rArmY = -0.3f; p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, 0, .15f, -0.8f, .42f, 0.9f, .52f, -2.4f, .7f, -1.4f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .42f, -0.5f, .52f, -0.1f, 1, 0);
                    p.bodyY = track(t, 0, 0, .42f, -0.55f, .55f, 0.45f, .75f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .42f, -0.2f, .55f, 0.3f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.35f, .55f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .42f, 0.3f, .55f, 0.45f, 1, 0);
                    p.rootZ = track(t, .4f, 0, .55f, -1.4f, 1, 0); p.rootY = -0.5f * bump(win(t, .4f, .8f));
                }));

        // ---------- Plasma grenade: sidearm whip, homes and sticks ----------
        out.add(AttackDef.of("plasma_grenade").range(4, 16).cd(170).weight(2).dur(22).weapons("br85", "plasma")
                .tick((e, t, n) -> {
                    if (n < 9 && n % 2 == 0) Fx.dustBurst(L(e), PLASMA_C, 0.7f, Combat.muzzle(e, 0.3, -0.5, 0.1), 2, 0.08);
                    if (n == 2) snd(e, SoundEvents.BEACON_ACTIVATE, 0.6f, 2f);
                    if (n == 10) {
                        Vec3 from = Combat.muzzle(e, 0.7, -0.3, 0.2); double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, t, 0.02).add(0, 0.1 + dist * 0.014, 0).normalize();
                        Combat.shoot(e, PLASMA_NADE, from, d, 0.8 + dist * 0.022, 16f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 1.1f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .47f ? "plasma" : null;
                    p.rArmX = -0.75f; p.rArmY = -0.35f; p.rHandX = -0.3f;
                    // arm swings wide behind, then whips flat across the body
                    p.lArmX = track(t, 0, -0.3f, .3f, -1.1f, .45f, -1.5f, .62f, -1.2f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .3f, -1.3f, .45f, -0.4f, .62f, 0.3f, 1, 0);
                    p.lArmY = track(t, 0, 0, .3f, -0.6f, .45f, 0.4f, .62f, 0.8f, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, -0.6f, .47f, 0.55f, .7f, 0.4f, 1, 0); p.headY = -p.bodyY * 0.75f;
                    p.bodyZ = track(t, 0, 0, .3f, 0.1f, .47f, -0.08f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.4f, .55f, -0.45f, 1, 0); p.rLegX = track(t, 0, 0, .47f, 0.35f, 1, 0);
                    p.rootZ = track(t, .35f, 0, .5f, -1.2f, 1, 0); p.rootYaw = 0.2f * bump(win(t, .3f, .8f));
                }));

        // ---------- Pulse grenade: underhand lob, damaging hardlight field ----------
        out.add(AttackDef.of("pulse_grenade").range(4, 14).cd(220).weight(2).dur(20).weapons("br85", "pulse")
                .when((e, t) -> !PULSES.containsKey(e))
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 2f);
                    if (n == 10) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, -0.3); double dist = t != null ? from.distanceTo(mid(t)) : 8;
                        Vec3 d = aim(e, t, 0.02).add(0, 0.25 + dist * 0.02, 0).normalize();
                        Combat.shoot(e, PULSE, from, d, 0.6 + dist * 0.022, 4f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 1.3f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "pulse" : null;
                    p.rArmX = -0.7f; p.rArmY = -0.3f; p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, 0, .3f, 0.9f, .5f, -1.3f, .7f, -1.0f, 1, 0); p.lArmZ = -0.15f;
                    p.bodyX = track(t, 0, 0, .3f, 0.3f, .5f, 0.05f, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, -0.25f, .5f, 0.2f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.rootY = track(t, 0, 0, .3f, -1.4f, .55f, -0.4f, 1, 0); // dip down to bowl it
                    p.lLegX = track(t, 0, 0, .3f, -0.55f, .7f, -0.4f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.4f, 1, 0);
                }));

        // ---------- Promethean Vision: sonar pulse, reveals every foe ----------
        out.add(AttackDef.of("promethean_vision").range(0, 40).noLos().cd(600).weight(2).dur(26).weapon("br85")
                .when((e, t) -> Combat.around(e, e.position(), 32).size() >= 2)
                .tick((e, t, n) -> {
                    if (n == 6) { snd(e, SoundEvents.BEACON_ACTIVATE, 1f, 1.6f); snd(e, SoundEvents.CONDUIT_ACTIVATE, 0.6f, 1.8f); }
                    if (n >= 8 && n <= 20 && n % 2 == 0) {
                        float rr = (n - 6) * 2.4f;
                        Fx.ring(L(e), e.position().add(0, 1.2, 0), rr, VISOR, 1.4f, (int) (16 + rr * 3));
                    }
                    if (n == 12) {
                        for (LivingEntity f : Combat.around(e, e.position(), 32)) {
                            f.addEffect(new MobEffectInstance(MobEffects.GLOWING, 240, 0, false, false));
                            Fx.dustBurst(L(e), VISOR, 1f, mid(f), 6, 0.3);
                        }
                    }
                })
                .anim((p, t, a) -> {
                    // left hand to the helmet side, head cocks, chest opens as the pulse goes out
                    p.lArmX = track(t, 0, -0.8f, .2f, -2.4f, .75f, -2.3f, 1, -0.8f); p.lArmZ = track(t, 0, 0, .2f, 0.55f, .75f, 0.5f, 1, 0);
                    p.lArmY = track(t, 0, 0.5f, .2f, -0.2f, .75f, -0.2f, 1, 0.5f);
                    p.rArmX = track(t, 0, -0.6f, .25f, -0.35f, .8f, -0.35f, 1, -0.6f); p.rArmY = -0.25f; p.rHandX = -0.5f;
                    p.headZ = track(t, 0, 0, .2f, -0.2f, .4f, -0.2f, .5f, 0, 1, 0);
                    p.headX = track(t, 0, 0, .3f, 0.15f, .45f, -0.25f, .8f, -0.15f, 1, 0);
                    p.bodyX = track(t, 0, 0, .3f, 0.12f, .45f, -0.15f, .8f, -0.1f, 1, 0);
                    p.headY = 0.35f * (float) Math.sin(win(t, .45f, .85f) * PI * 2); // looks around at what lit up
                    p.rootY = track(t, 0, 0, .3f, -0.6f, .45f, 0.2f, 1, 0);
                    p.lLegX = -0.15f; p.rLegX = 0.15f;
                }));

        // ---------- Thruster Pack: lateral boost dodge ----------
        out.add(thruster("thruster_pack_left", -1));
        out.add(thruster("thruster_pack_right", 1));

        // ---------- Energy Sword: lunge and horizontal cleave ----------
        out.add(AttackDef.of("energy_sword").range(0, 8).cd(100).weight(3).dur(24).weapon("sword")
                .tick((e, t, n) -> {
                    if (n == 1) { snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.9f); Fx.dustBurst(L(e), SWORD_C, 1f, mz(e), 8, 0.2); }
                    if (n == 4 && t != null) {
                        double dist = e.distanceTo(t);
                        if (dist > 2.2) Combat.lunge(e, Math.min(1.4, dist * 0.22), 0.08);
                        snd(e, SoundEvents.ELYTRA_FLYING, 0.3f, 2f);
                    }
                    if (n >= 8 && n <= 11) Fx.arc(L(e), e.position().add(0, 1.2, 0), e.getYRot(), 1.8f, 80 - (n - 8) * 45, 80 - (n - 7) * 45, 8, SWORD_C, 1f, 10);
                    if (n == 10) {
                        for (LivingEntity f : Combat.cone(e, 3.6, 75)) {
                            Combat.damage(e, f, 20); Combat.knock(f, e.position(), 0.7, 0.25);
                            Fx.dustBurst(L(e), SWORD_C, 1f, mid(f), 10, 0.3); Fx.sparks(L(e), mid(f), 8);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f); snd(e, SoundEvents.TRIDENT_RIPTIDE_1, 0.8f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    // cock the blade back over the right shoulder, lunge, sweep right-to-left, follow through
                    p.rArmX = track(t, 0, -0.6f, .18f, -2.0f, .38f, -1.8f, .46f, -1.45f, .6f, -1.3f, 1, -0.6f);
                    p.rArmY = trackLin(t, 0, 0, .18f, -0.2f, .38f, -0.9f, .48f, 0.9f, .62f, 1.1f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .18f, 0.7f, .38f, 0.8f, .46f, 0.1f, 1, 0);
                    p.rHandX = track(t, 0, 0, .38f, 0.4f, .48f, -0.3f, 1, 0);
                    p.lArmX = track(t, 0, -0.3f, .18f, -1.2f, .4f, -1.0f, .55f, -0.4f, 1, -0.3f);
                    p.lArmZ = track(t, 0, 0, .4f, -0.6f, .55f, -0.9f, 1, 0); // counterbalance
                    p.bodyY = trackLin(t, 0, 0, .38f, -0.65f, .48f, 0.6f, .62f, 0.75f, 1, 0); p.headY = -p.bodyY * 0.55f;
                    p.bodyX = track(t, 0, 0, .18f, 0.15f, .38f, 0.3f, .5f, 0.35f, 1, 0);
                    p.rootZ = track(t, 0, 0, .15f, 0.8f, .38f, -3.2f, .6f, -2.6f, 1, 0);
                    p.rootY = track(t, 0, 0, .15f, -1.0f, .3f, 0.4f, .42f, -1.2f, .7f, -0.9f, 1, 0);
                    p.lLegX = track(t, 0, 0, .15f, -0.2f, .38f, -0.8f, .7f, -0.7f, 1, 0);
                    p.rLegX = track(t, 0, 0, .15f, 0.2f, .38f, 0.6f, .7f, 0.5f, 1, 0);
                }));
    }

    static AttackDef thruster(String id, int side) {
        return AttackDef.of(id).range(2, 14).cd(160).weight(1).dur(16).weapon("br85")
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.7f || e.distanceTo(t) < 4)
                .tick((e, t, n) -> {
                    Vec3 f = Vec3.directionFromRotation(0, e.getYRot()), r = new Vec3(-f.z, 0, f.x).scale(side);
                    if (n == 3) {
                        e.setDeltaMovement(r.scale(1.25).add(0, 0.12, 0)); e.hurtMarked = true;
                        snd(e, SoundEvents.FIRECHARGE_USE, 0.9f, 1.5f); snd(e, SoundEvents.ELYTRA_FLYING, 0.4f, 1.8f);
                    }
                    if (n >= 3 && n <= 8) {
                        Vec3 pack = e.position().add(0, 1.2, 0).subtract(f.scale(0.4)).subtract(r.scale(0.3));
                        Fx.burst(L(e), ParticleTypes.FLAME, pack, 3, 0.08, 0.02);
                        Fx.dustBurst(L(e), THRUST, 1.2f, pack, 4, 0.12);
                        Fx.burst(L(e), ParticleTypes.CLOUD, pack.subtract(r.scale(0.4)), 2, 0.1, 0.01);
                    }
                    if (n == 12) snd(e, SoundEvents.ARMOR_EQUIP_NETHERITE, 0.9f, 0.8f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), boost = win(t, .15f, .25f) * (1 - win(t, .55f, .75f));
                    float crouch = bump(win(t, 0, .25f)) + bump(win(t, .65f, 1f)) * 0.8f;
                    rifle(p, k, 0.15f, 0); // rifle pulled in tight
                    p.rootRoll = -side * 0.35f * boost; p.bodyZ += -side * 0.12f * boost;
                    p.headZ += side * 0.2f * boost; p.headY -= side * 0.15f * boost; // eyes stay on target
                    p.rootY -= 1.4f * crouch;
                    // legs tuck during the burn, trailing leg kicks out on landing
                    p.lLegX = lerp(p.lLegX, -0.7f, boost); p.rLegX = lerp(p.rLegX, 0.3f, boost);
                    p.lLegZ += -side * 0.3f * boost; p.rLegZ += -side * 0.3f * boost;
                    p.rootX = side * -1.2f * boost;
                    p.bodyX += 0.2f * crouch;
                });
    }

    // ================= model: Mjolnir Mark VI GEN2 =================
    @Override
    public void buildModel(Rig rig) {
        Mat suit = Mat.of(0x232527).noise(.1f), metal = Mat.of(0x6d7072).noise(.07f), dark = Mat.of(0x34382a).noise(.1f);
        Mat plate = Mat.of(OLIVE).bevel(.32f).noise(.08f).grad(.2f);
        Mat visor = Mat.of(VISOR).glow().decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.FRONT) { g.rectGlow(x, y, w, 1, 0xfff0b0); g.glow(x + w - 2, y + h - 1, 0xb07020); }
        });
        Mat chest = Mat.of(OLIVE).bevel(.3f).noise(.06f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            int cx = x + w / 2; // GEN2 chest: center seam and angled pec plates
            g.rect(cx, y, 1, h, 0x2e3522);
            for (int i = 0; i < Math.min(3, h); i++) { g.px(x + 1 + i, y + h - 1 - i, 0x3a4429); g.px(x + w - 2 - i, y + h - 1 - i, 0x3a4429); }
            g.rect(cx - 2, y + 1, 1, 1, 0x7c8a5a); g.rect(cx + 2, y + 1, 1, 1, 0x7c8a5a);
        });
        Mat helm = Mat.of(OLIVE).bevel(.28f).grad(.22f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) { g.rect(x + 1, y + h / 2, w - 2, 1, 0x2a3020); g.rect(x + w / 2, y + 1, 1, h / 2, 0x2a3020); }
            if (f == Mat.Face.TOP) g.rect(x + w / 2, y, 1, h, 0x3a4429);
        });
        Mat vent = Mat.of(THRUST).glow();
        // torso
        rig.box(rig.torso, suit, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4.5f, -12, -3, 9, 6, 1, .25f);
        rig.box(rig.torso, dark, -3, -6, -2.6f, 6, 3, 1);
        rig.box(rig.torso, metal, -4.5f, -3, -2.5f, 9, 2, 5);
        rig.box(rig.torso, plate, -1.5f, -1.5f, -2.9f, 3, 2, 1);
        rig.box(rig.torso, plate, -3.5f, -11, 2, 7, 8, 2);
        rig.box(rig.torso, dark, -3, -10, 3.6f, 6, 4, 1);
        rig.box(rig.torso, vent, -3, -5, 3.6f, 2, 2, 1);
        rig.box(rig.torso, vent, 1, -5, 3.6f, 2, 2, 1);
        rig.box(rig.torso, dark, -3.5f, -13, -2.5f, 7, 1, 5);
        // helmet
        rig.box(rig.head, helm, -4, -8, -4, 8, 8, 8, .4f);
        rig.box(rig.head, visor, -3, -5.5f, -4.9f, 6, 2, 1);
        rig.box(rig.head, metal, -2.5f, -3.2f, -4.8f, 5, 3, 1);
        rig.box(rig.head, plate, -1, -9, -3.5f, 2, 1, 7);
        rig.box(rig.head, dark, -4.9f, -5, -2, 1, 3, 4);
        rig.box(rig.head, dark, 3.9f, -5, -2, 1, 3, 4);
        rig.box(rig.head, metal, -1.5f, -2, 3.8f, 3, 2, 1);
        // arms: big angular GEN2 pauldrons, forearm plates, dark gloves
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, suit, -2, -2, -2, 4, 12, 4);
            rig.box(arm, plate, s < 0 ? -3.6f : -2.4f, -3.2f, -3, 6, 4, 6, .1f);
            rig.box(arm, dark, s < 0 ? -3.4f : 2.4f, 0.8f, -2, 1, 2, 4);
            rig.box(arm, metal, -2, 3, -2, 4, 1, 4, .2f);
            rig.box(arm, plate, -2, 5, -2, 4, 4, 4, .35f);
            rig.box(arm, dark, -2, 9, -2, 4, 2, 4, .2f);
        }
        // legs
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, suit, -2, 0, -2, 4, 12, 4);
            rig.box(leg, plate, -2, 0.5f, -2.8f, 4, 4, 1);
            rig.box(leg, plate, s < 0 ? -2.8f : 1.8f, 0.5f, -2, 1, 4, 4);
            rig.box(leg, metal, -1.5f, 5, -2.9f, 3, 2, 1);
            rig.box(leg, plate, -2, 8.5f, -3, 4, 4, 5, .2f);
        }
        buildWeapons(rig, metal);
    }

    private static void buildWeapons(Rig rig, Mat metal) {
        Mat gun = Mat.of(0x2e3236).noise(.08f), gunL = Mat.of(0x5a6066).noise(.06f), tan = Mat.of(0x6a6448).noise(.1f), black = Mat.of(0x161616);
        Mat pro = Mat.of(0xa89f8e).bevel(.3f).noise(.05f), proD = Mat.of(0x4a4440).noise(.08f), proG = Mat.of(PRO).glow();
        Mat cyan = Mat.of(0x40c8ff).glow(), red = Mat.of(LASER).glow(), blade = Mat.of(SWORD_C).glow(), hilt = Mat.of(0x3a3046).noise(.1f);
        PartDefinition w;
        w = rig.weapon(rig.rHand, "br85");
        rig.box(w, gun, -1.5f, -3, -8, 3, 4, 11); rig.box(w, metal, -0.5f, -2, -14, 1, 1, 6); rig.box(w, black, -1, -5, -6, 2, 2, 5);
        rig.box(w, gunL, -1, 1, -5, 2, 3, 2); rig.box(w, gun, -1, -2.5f, 3, 2, 3, 3);
        w = rig.weapon(rig.rHand, "dmr");
        rig.box(w, gun, -1.5f, -3, -9, 3, 3, 12); rig.box(w, metal, -0.5f, -2.5f, -17, 1, 1, 8); rig.box(w, black, -1, -5.5f, -7, 2, 2, 6);
        rig.box(w, gunL, -1, 0, -7, 2, 3, 2); rig.box(w, tan, -1, -3, 3, 2, 3, 4);
        w = rig.weapon(rig.rHand, "ar");
        rig.box(w, gun, -2, -3, -7, 4, 4, 10); rig.box(w, gunL, -1, -2.5f, -11, 2, 2, 4); rig.box(w, gun, -0.5f, -5, -5, 1, 2, 5);
        rig.box(w, cyan, -0.5f, -4, 1, 1, 1, 1); rig.box(w, black, -1, 1, -4, 2, 4, 2);
        w = rig.weapon(rig.rHand, "saw");
        rig.box(w, gun, -2, -3.5f, -8, 4, 5, 10); rig.box(w, metal, -1, -2.5f, -17, 2, 2, 9); rig.box(w, tan, -2.5f, 1, -5, 5, 3, 4);
        rig.box(w, black, -0.5f, -5.5f, -6, 1, 2, 4); rig.box(w, gun, -0.5f, 1, -11, 1, 3, 1);
        w = rig.weapon(rig.rHand, "boltshot");
        rig.box(w, pro, -1, -2.5f, -6, 2, 2, 7); rig.box(w, proG, -0.5f, -0.6f, -5, 1, 1, 4); rig.box(w, proD, -1, 0, -1, 2, 3, 2);
        rig.box(w, pro, -1.5f, -3, -8, 3, 1, 2);
        w = rig.weapon(rig.rHand, "lightrifle");
        rig.box(w, pro, -1.5f, -3, -10, 3, 3, 13); rig.box(w, proG, -0.5f, -3.8f, -9, 1, 1, 10); rig.box(w, proD, -1, -2, 3, 2, 3, 3);
        w = rig.weapon(rig.rHand, "lightrifle_open");
        rig.box(w, pro, -1.5f, -3, -10, 3, 3, 13); rig.box(w, pro, -3.2f, -3.5f, -11, 1, 2, 8); rig.box(w, pro, 2.2f, -3.5f, -11, 1, 2, 8);
        rig.box(w, proG, -0.5f, -4.3f, -11, 1, 1, 12); rig.box(w, proD, -1, -2, 3, 2, 3, 3);
        w = rig.weapon(rig.rHand, "binary");
        rig.box(w, proD, -1.5f, -3, -12, 3, 3, 16); rig.box(w, pro, -1.5f, -2.5f, -20, 1, 1, 8); rig.box(w, pro, 0.5f, -2.5f, -20, 1, 1, 8);
        rig.box(w, red, -0.5f, -4, -8, 1, 1, 6); rig.box(w, pro, -1, -2, 4, 2, 3, 3);
        w = rig.weapon(rig.rHand, "scattershot");
        rig.box(w, pro, -2, -3.5f, -8, 4, 5, 10); rig.box(w, proD, -2.5f, -4, -10, 5, 5, 2); rig.box(w, proG, -1, -2.5f, -10.6f, 2, 2, 1);
        rig.box(w, proD, -1, 1, -2, 2, 3, 2);
        w = rig.weapon(rig.rHand, "suppressor");
        rig.box(w, pro, -2, -3, -9, 4, 4, 11); rig.box(w, proD, -2, -3.5f, -12, 1, 2, 3); rig.box(w, proD, 1, -3.5f, -12, 1, 2, 3);
        rig.box(w, proG, -0.5f, -4, -7, 1, 1, 6); rig.box(w, proD, -1, 1, -2, 2, 3, 2);
        w = rig.weapon(rig.rHand, "incin");
        rig.box(w, proD, -2.5f, -4.5f, -12, 5, 6, 15); rig.box(w, pro, -3, -5, -14, 6, 7, 2); rig.box(w, proG, -1.5f, -3.5f, -14.6f, 3, 3, 1);
        rig.box(w, pro, -0.5f, -6.5f, -8, 1, 2, 8);
        w = rig.weapon(rig.rHand, "sticky");
        rig.box(w, tan, -1.5f, -3, -7, 3, 3, 8); rig.box(w, gun, -1, -2.5f, -12, 2, 2, 5); rig.box(w, black, -0.5f, -6, -2, 1, 3, 1);
        rig.box(w, red, -0.5f, -3.8f, -4, 1, 1, 1); rig.box(w, gun, -1, 0, -1, 2, 3, 2);
        w = rig.weapon(rig.rHand, "sword");
        rig.box(w, hilt, -1, -1, -2, 2, 2, 5); rig.box(w, hilt, -1, -2.5f, -3, 2, 5, 1);
        rig.box(w, blade, -0.5f, -2.5f, -14, 1, 1, 11); rig.box(w, blade, -0.5f, 1.5f, -14, 1, 1, 11);
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, Mat.of(0x4a5a3a).noise(.1f), -1, -1, -2, 2, 2, 2); rig.box(w, metal, -0.5f, -1.8f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "plasma");
        rig.box(w, Mat.of(PLASMA_C).glow(), -1, -1, -2, 2, 2, 2); rig.box(w, Mat.of(0x50607a), -0.5f, -1.8f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "pulse");
        rig.box(w, pro, -1, -1, -2, 2, 2, 2); rig.box(w, proG, -0.5f, -1.8f, -1.5f, 1, 1, 1);
    }
}
