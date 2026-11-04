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

/** Jameson Locke, Halo 5: Guardians. Fireteam Osiris lead: Spartan abilities, Artemis scan, full UNSC/Banished-era sandbox. */
public class Halo5Locke extends SlayerKit {
    static final int GOLD = 0xc9a13b, VISOR = 0xffa82a, THRUST = 0x4fb8ff, SHIELD_C = 0xffc94a, PLASMA_C = 0x3f9dff, SWORD_C = 0x6fd8ff, TRACER = 0xffe9a0;
    static final float SHIELD = 10f;

    static final ProjStyle ROCKET = ProjStyle.make().color(0xffa040).size(0.5f).life(100).explode(3.8f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle FRAG = ProjStyle.make().color(0x4a5a3a).size(0.25f).life(34).explode(3.5f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);
    /** plasma grenade: homes onto the target to fake "sticking", blue detonation */
    static final ProjStyle PLASMA_NADE = ProjStyle.make().color(PLASMA_C).size(0.3f).life(40).explode(3f).gravity(0.03).homing(0.22f)
            .trail(ParticleTypes.ELECTRIC_SPARK)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 2.5f);
                Fx.dustBurst(l, PLASMA_C, 2f, pos, 40, 1.4);
                Fx.ring(l, pos.add(0, 0.2, 0), 2.6f, 0x9fd0ff, 1.4f, 28);
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 30, 0.8, 0.3);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1.2f, 1.4f);
                Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1f, 1.8f);
            });

    public Halo5Locke() {
        super("halo5_locke", "Jameson Locke", "ジェイムソン・ロック", "Halo 5: Guardians");
        eggPrimary = 0x1c1c1f; eggSecondary = GOLD;
        maxHealth = 70; speed = 0.31; armor = 14; knockbackRes = 0.55;
        scale = 1.08f; width = 0.7f; preferredRange = 11f; dmgMult = 1.05f;
        idleWeaponR = "br";
    }

    // ================= helpers (server) =================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.2, 0.3, 0); }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        return Combat.dirTo(mz(e), t, spread, new Random(e.getRandom().nextLong()));
    }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static Vec3 fwd(SlayerEntity e) { return Vec3.directionFromRotation(0, e.getYRot()); }
    static Vec3 right(SlayerEntity e) { Vec3 f = fwd(e); return new Vec3(-f.z, 0, f.x); }
    static void flash(SlayerEntity e, int rgb) {
        Vec3 m = mz(e);
        Fx.burst(L(e), ParticleTypes.FLAME, m, 2, 0.04, 0.02);
        Fx.dustBurst(L(e), rgb, 1f, m, 3, 0.06);
    }
    static void casing(SlayerEntity e) { Fx.dustBurst(L(e), GOLD, 0.5f, Combat.muzzle(e, 0.5, 0.55, 0.05), 1, 0.05); }
    static void bullet(SlayerEntity e, LivingEntity t, double spread, double range, float dmg, int pierce) {
        Combat.hitscan(e, mz(e), aim(e, t, spread), range, dmg, pierce, TRACER);
    }
    /** thruster pack exhaust behind/below the back */
    static void exhaust(SlayerEntity e, Vec3 dir, int n) {
        Vec3 back = e.position().add(0, 1.2, 0).subtract(fwd(e).scale(0.45));
        Fx.dustBurst(L(e), THRUST, 1.1f, back, n, 0.15);
        Fx.burst(L(e), ParticleTypes.CLOUD, back.subtract(dir.scale(0.4)), n / 2 + 1, 0.12, 0.02);
    }
    static void hold(SlayerEntity e, LivingEntity t, double dist, double lift) {
        if (t == null || !Combat.isFoe(e, t)) return;
        Vec3 p = e.position().add(fwd(e).scale(dist + t.getBbWidth() * 0.5)).add(0, lift, 0);
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true;
    }

    // ================= passive: Mjolnir energy shield =================
    @Override
    public void passive(SlayerEntity e) {
        CompoundTag d = e.getPersistentData();
        if (e.hurtTime > 0) d.putInt("h5Hurt", e.tickCount);
        float abs = e.getAbsorptionAmount(), prev = d.getFloat("h5Abs");
        ServerLevel l = L(e);
        Vec3 c = e.position().add(0, 1.0, 0);
        if (prev > 0 && abs <= 0) { // shield popped
            snd(e, SoundEvents.GLASS_BREAK, 0.9f, 1.5f); snd(e, SoundEvents.BEACON_DEACTIVATE, 0.7f, 2f);
            Fx.dustBurst(l, SHIELD_C, 1.2f, c, 24, 0.5); Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, c, 16, 0.4, 0.2);
        } else if (abs > 0 && abs < prev) Fx.dustBurst(l, SHIELD_C, 0.9f, c, 6, 0.45); // flare on hit
        int since = e.tickCount - d.getInt("h5Hurt");
        if (since > 80 && abs < SHIELD) {
            if (since == 81) snd(e, SoundEvents.BEACON_ACTIVATE, 0.6f, 1.8f);
            e.setAbsorptionAmount(Math.min(SHIELD, abs + 0.5f));
            if (e.tickCount % 2 == 0) Fx.dustBurst(l, SHIELD_C, 0.7f, c, 3, 0.45);
            if (e.getAbsorptionAmount() >= SHIELD) { snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.4f); Fx.dustBurst(l, SHIELD_C, 1f, c, 14, 0.55); }
        }
        d.putFloat("h5Abs", e.getAbsorptionAmount());
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** Spartan rifle hold: stock in the shoulder, ads = smart-link cheek weld */
    static void shoulder(Pose p, float k, float pitch, float recoil, float ads) {
        float tw = 0.18f + 0.1f * ads;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.25f + 0.05f * ads, k); p.rArmY = lerp(p.rArmY, -0.1f - tw, k); p.rArmZ = lerp(p.rArmZ, 0.05f, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.1f - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.5f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * 0.8f * k; p.headX += (0.12f * ads - recoil * 0.1f) * k; p.headZ += 0.08f * ads * k;
        p.bodyX += (0.06f + recoil * 0.1f - pitch * 0.2f) * k; p.rootZ += recoil * 0.8f * k; p.rHandX -= recoil * 0.3f * k;
    }
    /** two-handed isosceles pistol grip, chest square to target */
    static void pistol(Pose p, float k, float recoil) {
        p.rArmX = lerp(p.rArmX, -HALF - recoil * 0.4f, k); p.rArmY = lerp(p.rArmY, 0.2f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + 0.05f - recoil * 0.35f, k); p.lArmY = lerp(p.lArmY, -0.35f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX -= recoil * 0.45f * k; p.headX += (0.08f - recoil * 0.12f) * k; p.bodyX += (0.1f - recoil * 0.06f) * k; p.rootZ += recoil * 0.5f * k;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.25f, k);
        p.lLegZ -= 0.05f * k; p.rLegZ += 0.07f * k; p.rootY -= 0.35f * k;
    }
    static void brace(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.35f, k); p.rLegX = lerp(p.rLegX, 0.3f, k);
        p.lLegZ -= 0.22f * k; p.rLegZ += 0.22f * k; p.rootY -= 1.0f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // BR at low ready, muzzle angled down-left, slow breathing
        float br = 0.03f * (float) Math.sin(age * 0.08f);
        p.rArmX = -0.6f + p.rArmX * 0.25f + br; p.rArmY = -0.3f;
        p.lArmX = -0.85f + p.lArmX * 0.25f + br; p.lArmY = 0.7f; p.lArmZ = 0.05f;
        p.rHandX = -0.45f; p.bodyX += 0.05f + br * 0.5f; p.headX += br;
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        out.add(AttackDef.of("thruster_pack").range(4, 18).cd(90).weight(2).dur(24)
                .tick((e, t, n) -> {
                    if (n == 2) {
                        Vec3 f = e.getViewVector(1f), left = new Vec3(f.z, 0, -f.x);
                        e.setDeltaMovement(left.x * 1.1 + f.x * 0.3, 0.12, left.z * 1.1 + f.z * 0.3); e.hurtMarked = true;
                        KU.snd(e, SoundEvents.FIRECHARGE_USE, 1.1f, 1.5f);
                    }
                    if (n > 1 && n < 8) Fx.burst(KU.L(e), ParticleTypes.FLAME, e.position().add(0, 1, 0), 4, 0.2, 0.05);
                    if (n == 12 || n == 14 || n == 16) { Combat.hitscan(e, KU.mz(e), KU.aim(e, t, 0.02), 30, 3.5f, 0, 0xffd070); KU.flash(e, 0xffd070); KU.snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, 1.4f); }
                })
                .anim((p, t, a) -> {
                    float k = KU.ready(t, .4f, .9f), side = bump(win(t, .05f, .4f));
                    p.rootRoll = -0.25f * side; p.lLegZ = -0.45f * side; p.rLegZ = 0.25f * side; p.rootY = -0.8f * side;
                    KU.shoulder(p, k, 0, 1.2f * kick(t, 14 / 24f, .2f)); KU.stance(p, k);
                }));
        // ---------- Rifle melee: twist-loaded sideways butt-stroke with the BR ----------
        out.add(AttackDef.of("rifle_melee").range(0, 3.2f).cd(22).weight(3).dur(14).weapon("br")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.ARMOR_EQUIP_NETHERITE, 0.7f, 1.3f);
                    if (n == 5) {
                        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.5f, -60, 40, 20, 0xdddddd, 0.7f, 10);
                        for (LivingEntity f : Combat.cone(e, 3.4, 55)) { Combat.damage(e, f, 9); Combat.knock(f, e.position(), 0.8, 0.25); Fx.sparks(L(e), mid(f), 8); }
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.7f); snd(e, SoundEvents.ANVIL_PLACE, 0.4f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -0.8f, .2f, -1.1f, .36f, -1.75f, .55f, -1.6f, 1, -0.6f);
                    p.rArmY = track(t, 0, -0.25f, .2f, -0.65f, .36f, 0.2f, .6f, 0.15f, 1, -0.3f);
                    p.rHandZ = track(t, 0, 0, .2f, -0.5f, .36f, 0.8f, .6f, 0.6f, 1, 0);
                    p.rHandX = track(t, 0, -0.4f, .2f, -0.8f, .36f, 0.3f, 1, -0.4f);
                    p.lArmX = track(t, 0, -0.9f, .2f, -1.25f, .36f, -1.65f, .6f, -1.45f, 1, -0.85f);
                    p.lArmY = track(t, 0, 0.6f, .2f, 0.95f, .36f, 0.15f, .6f, 0.2f, 1, 0.7f);
                    p.bodyY = track(t, 0, 0, .2f, 0.5f, .36f, -0.55f, .55f, -0.45f, 1, 0);
                    p.bodyX = track(t, 0, 0.05f, .2f, -0.1f, .36f, 0.32f, .6f, 0.25f, 1, 0.05f);
                    p.headY = -p.bodyY * 0.6f; p.headX = track(t, 0, 0, .3f, 0.15f, 1, 0);
                    p.lLegX = track(t, 0, 0, .25f, -0.55f, .8f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .36f, 0.35f, .8f, 0.3f, 1, 0);
                    p.rootZ = track(t, 0, 0, .2f, 0.5f, .36f, -2.0f, .7f, -1.6f, 1, 0); p.rootY = -0.6f * bump(win(t, .25f, .9f));
                }));

        // ---------- Assassination: knee-kick from behind, knife to the neck, shove off ----------
        out.add(AttackDef.of("assassination").range(0, 3.5f).lowHealthTarget(0.3f).weight(6).cd(80).dur(36).weapon("knife")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.5, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.8f); }
                    if (n < 30) hold(e, t, 0.95, n >= 8 ? -0.35 : 0);
                    if (n == 8 && t != null) {
                        Combat.damage(e, t, Math.max(0, t.getHealth() - 1)); Fx.sparks(L(e), t.position().add(0, 0.4, 0), 8);
                        snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f); snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.8f, 0.7f);
                    }
                    if (n == 13) snd(e, SoundEvents.TRIDENT_RETURN, 0.8f, 1.8f);
                    if (n == 20 && t != null) {
                        Fx.dustBurst(L(e), 0x7a1010, 1.2f, mid(t), 20, 0.3); Fx.burst(L(e), ParticleTypes.DAMAGE_INDICATOR, mid(t), 6, 0.3, 0.1);
                        Combat.damage(e, t, t.getHealth() + 100);
                        snd(e, SoundEvents.TRIDENT_HIT, 1.2f, 0.8f); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.7f);
                    }
                    if (n == 28 && t != null) Combat.knock(t, e.position(), 0.8, 0.2);
                })
                .anim((p, t, a) -> {
                    p.weaponR = t > .33f ? "knife" : "br";
                    // left hand grabs the shoulder and holds
                    p.lArmX = track(t, 0, -0.8f, .12f, -1.7f, .25f, -1.5f, .55f, -1.35f, .78f, -1.7f, .85f, -1.0f, 1, -0.85f);
                    p.lArmY = track(t, 0, 0.6f, .12f, 0.2f, .78f, 0.15f, 1, 0.7f);
                    // right: rifle drops, knife drawn overhead, driven down, twisted
                    p.rArmX = track(t, 0, -0.6f, .25f, -0.3f, .36f, -0.2f, .48f, -2.8f, .56f, -1.05f, .75f, -1.0f, .9f, -0.5f, 1, -0.6f);
                    p.rArmZ = track(t, 0, 0, .48f, 0.25f, .56f, -0.15f, 1, 0);
                    p.rArmY = 0.3f * (float) Math.sin(win(t, .58f, .74f) * PI * 2);
                    p.rHandX = track(t, .33f, 0, .45f, 1.3f, .74f, 1.3f, .85f, 0.2f, 1, -0.45f);
                    // right leg stamps the back of the knee
                    p.rLegX = track(t, 0, 0, .12f, 0.4f, .2f, -1.1f, .26f, -0.7f, .34f, 0.3f, .85f, 0.3f, 1, 0);
                    p.lLegX = track(t, 0, 0, .12f, -0.3f, .26f, 0.1f, .4f, -0.4f, .85f, -0.4f, 1, 0);
                    p.bodyX = track(t, 0, 0, .12f, 0.15f, .2f, -0.2f, .45f, -0.15f, .56f, 0.45f, .8f, 0.35f, 1, 0);
                    p.bodyY = track(t, 0, 0, .45f, 0.3f, .56f, -0.2f, .85f, 0.25f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.headX = track(t, 0, 0, .2f, 0.3f, .56f, 0.45f, .85f, 0.2f, 1, 0);
                    p.rootY = track(t, 0, 0, .45f, 0, .56f, -1.2f, .8f, -0.9f, 1, 0);
                    p.rootZ = track(t, 0, 0, .12f, -1.8f, .8f, -1.8f, .88f, 0.6f, 1, 0);
                }));

        // ---------- Spartan Charge: crouch, thrusters, shoulder-first tackle ----------
        out.add(AttackDef.of("spartan_charge").range(3, 10).cd(140).weight(2).dur(24).weapon("br").moving()
                .tick((e, t, n) -> {
                    CompoundTag d = e.getPersistentData();
                    if (n == 0) { d.putBoolean("h5Chg", false); snd(e, SoundEvents.ARMOR_EQUIP_NETHERITE, 1f, 0.7f); }
                    if (n == 7) { snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2f, 0.6f); snd(e, SoundEvents.ELYTRA_FLYING, 0.5f, 1.5f); }
                    if (n >= 7 && n < 16 && !d.getBoolean("h5Chg")) {
                        Combat.lunge(e, 1.1, 0.02); exhaust(e, fwd(e), 4);
                        Fx.burst(L(e), ParticleTypes.CLOUD, e.position().add(0, 0.1, 0), 2, 0.2, 0.01);
                        for (LivingEntity f : Combat.around(e, e.position().add(0, 1, 0).add(fwd(e).scale(1.1)), 1.5)) {
                            Combat.damage(e, f, 12); Combat.knock(f, e.position(), 1.9, 0.5);
                            Fx.sparks(L(e), mid(f), 14); Fx.ring(L(e), mid(f), 0.9f, SHIELD_C, 1.2f, 14);
                            snd(e, SoundEvents.ANVIL_LAND, 0.8f, 0.8f); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.3f, 0.6f);
                            d.putBoolean("h5Chg", true);
                        }
                        if (d.getBoolean("h5Chg")) e.setDeltaMovement(e.getDeltaMovement().scale(0.1));
                    }
                })
                .anim((p, t, a) -> {
                    float wind = win(t, 0, .28f) * (1 - win(t, .3f, .35f)), run = win(t, .28f, .33f) * (1 - win(t, .66f, .75f)), rec = win(t, .7f, 1);
                    float lead = smooth(win(t, .05f, .3f)) * (1 - smooth(rec));
                    // gun tucked low across the chest, left shoulder driven forward
                    p.rArmX = -0.5f * lead + p.rArmX * (1 - lead); p.rArmY = -0.6f * lead; p.rHandX = -0.6f * lead;
                    p.lArmX = lerp(p.lArmX, -0.35f, lead); p.lArmZ = lerp(p.lArmZ, 0.35f, lead); p.lArmY = 0.2f * lead;
                    p.bodyY = 0.75f * lead; p.headY = -0.6f * lead;
                    p.bodyX = 0.15f * wind + 0.5f * run * (1 - rec); p.headX = -0.3f * run;
                    p.rootY = -1.4f * wind - 0.4f * run; p.rootZ = -1.2f * run;
                    float cyc = (float) Math.sin(a * 1.7f) * run;
                    p.lLegX = -0.35f * wind + 0.95f * cyc; p.rLegX = 0.4f * wind - 0.95f * cyc;
                    p.rootY += Math.abs(cyc) * 0.5f;
                    // impact jolt
                    float hit = kick(t, .45f, .2f);
                    p.bodyX -= hit * 0.5f; p.headX -= hit * 0.3f; p.lArmX -= hit * 0.5f;
                }));

        // ---------- Ground Pound: leap, hover-charge, meteor slam ----------
        out.add(AttackDef.of("ground_pound").range(2.5f, 10).cd(220).weight(2).dur(38).weapon("br").moving()
                .tick((e, t, n) -> {
                    CompoundTag d = e.getPersistentData();
                    e.fallDistance = 0;
                    if (n == 0) { d.putBoolean("h5Gp", false); snd(e, SoundEvents.ARMOR_EQUIP_NETHERITE, 1f, 0.6f); }
                    if (n == 5) {
                        Vec3 h = t != null ? t.position().subtract(e.position()).multiply(0.06, 0, 0.06) : Vec3.ZERO;
                        e.setDeltaMovement(h.x, 0.95, h.z); e.hurtMarked = true;
                        exhaust(e, new Vec3(0, 1, 0), 10); snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.3f, 0.5f);
                    }
                    if (n >= 11 && n < 20) {
                        Vec3 v = e.getDeltaMovement(); e.setDeltaMovement(v.x * 0.5, 0.04, v.z * 0.5); e.hurtMarked = true;
                        Vec3 c = e.position().add(0, 0.2, 0); double ang = n * 0.9;
                        Fx.dustBurst(L(e), THRUST, 1f, c.add(Math.cos(ang) * 0.8, 0, Math.sin(ang) * 0.8), 3, 0.05);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, e.position().add(0, -0.3, 0), 2, 0.2, 0.05);
                        if (n % 3 == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.6f, 0.8f + (n - 11) * 0.08f);
                    }
                    if (n == 20) {
                        Vec3 h = t != null ? t.position().subtract(e.position()).multiply(0.25, 0, 0.25) : Vec3.ZERO;
                        if (h.length() > 1.2) h = h.normalize().scale(1.2);
                        e.setDeltaMovement(h.x, -2.2, h.z); e.hurtMarked = true; snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1f, 0.4f);
                    }
                    if (n >= 20 && !d.getBoolean("h5Gp") && (e.onGround() || n >= 30)) {
                        d.putBoolean("h5Gp", true);
                        Vec3 c = e.position();
                        Combat.blast(e, c.add(0, 0.5, 0), 4.5f, 16f, 1.4f, null);
                        Fx.explosion(L(e), c, 2.5f);
                        for (int i = 0; i < 3; i++) Fx.ring(L(e), c.add(0, 0.15, 0), 1.5f + i * 1.4f, i == 1 ? THRUST : 0x9a8a70, 1.4f, 26 + i * 6);
                        Fx.burst(L(e), ParticleTypes.POOF, c, 24, 1.2, 0.05);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 1.3f, 0.8f); snd(e, SoundEvents.ANVIL_LAND, 1f, 0.5f);
                    }
                })
                .anim((p, t, a) -> {
                    float crouch = bump(win(t, 0, .2f)), air = win(t, .14f, .26f) * (1 - win(t, .5f, .55f));
                    float hover = win(t, .28f, .52f) * (1 - win(t, .52f, .56f)), dive = win(t, .52f, .56f) * (1 - win(t, .58f, .62f));
                    float land = win(t, .56f, .62f) * (1 - win(t, .8f, 1));
                    // legs: crouch -> tuck knees -> stretch down -> superhero landing
                    p.lLegX = -0.4f * crouch - 1.3f * air - 0.2f * dive - 1.1f * land;
                    p.rLegX = 0.3f * crouch - 0.9f * air + 0.3f * dive + 1.0f * land;
                    p.lLegZ = -0.15f * air; p.rLegZ = 0.15f * air;
                    p.rootY = -1.5f * crouch - 2.2f * land + 1.0f * air;
                    // arms: gun slung, right fist charged back, then driven into the ground
                    p.rHandX = -0.6f;
                    p.rArmX = lerp(lerp(-0.6f, 0.6f, air), -0.4f, land) - 1.2f * dive;
                    p.rArmZ = 0.5f * air + 0.2f * land; p.rArmY = -0.3f;
                    p.lArmX = -0.5f * crouch + 0.5f * air - 0.9f * dive - 0.3f * land; p.lArmZ = -0.6f * air - 0.4f * land;
                    p.bodyX = 0.25f * crouch + 0.35f * air + 0.6f * dive + 0.65f * land; p.headX = -0.3f * air - 0.4f * land;
                    float buzz = 0.04f * hover * (float) Math.sin(a * 5.5f);
                    p.bodyZ = buzz; p.headZ = -buzz;
                    p.rootPitch = 0.15f * dive;
                }));

        // ---------- Thruster Pack: lateral boost (both directions) ----------
        for (int s = -1; s <= 1; s += 2) {
            final int side = s;
            out.add(AttackDef.of(s > 0 ? "thruster_right" : "thruster_left").range(3, 16).cd(70).weight(2).dur(12).weapon("br").moving()
                    .tick((e, t, n) -> {
                        if (n == 1) {
                            Vec3 v = right(e).scale(1.05 * side).add(fwd(e).scale(-0.15));
                            e.setDeltaMovement(v.x, 0.12, v.z); e.hurtMarked = true;
                            snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.9f, 1.5f); snd(e, SoundEvents.BLAZE_SHOOT, 0.4f, 1.8f);
                        }
                        if (n >= 1 && n < 6) exhaust(e, right(e).scale(side), 5);
                    })
                    .anim((p, t, a) -> {
                        float k = ready(t, .1f, .8f), push = win(t, 0, .15f) * (1 - win(t, .6f, 1));
                        shoulder(p, k, 0, 0, 0);
                        p.bodyZ += 0.32f * side * push; p.headZ -= 0.25f * side * push;
                        p.bodyX += 0.15f * push;
                        // trailing leg flung out, leading knee bent to absorb
                        if (side > 0) { p.lLegZ = -0.6f * push; p.rLegX = -0.4f * push; p.rLegZ = 0.15f * push; }
                        else { p.rLegZ = 0.6f * push; p.lLegX = -0.4f * push; p.lLegZ = -0.15f * push; }
                        p.rootY = -0.8f * push; p.rootRoll = 0.12f * side * push;
                    }));
        }

        // ---------- Artemis Scan: hand to helmet, pulse wave tags every hostile ----------
        out.add(AttackDef.of("scan").range(0, 40).cd(400).weight(2).dur(28).weapon("br").noLos()
                .when((e, t) -> Combat.around(e, e.position(), 32).size() >= 2)
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.BEACON_POWER_SELECT, 1f, 1.6f);
                    if (n >= 8 && n <= 20 && n % 2 == 0) {
                        float r = (n - 6) * 2.4f; Vec3 c = e.position().add(0, 0.3 + (n - 8) * 0.05, 0);
                        Fx.ring(L(e), c, r, VISOR, 1.3f, 20 + (int) (r * 5));
                    }
                    if (n == 12) {
                        snd(e, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4f, 1.2f); snd(e, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 0.6f);
                        for (LivingEntity f : Combat.around(e, e.position(), 40)) {
                            f.addEffect(new MobEffectInstance(MobEffects.GLOWING, 240, 0));
                            Fx.dustBurst(L(e), VISOR, 1.2f, f.position().add(0, f.getBbHeight() + 0.5, 0), 6, 0.1);
                        }
                    }
                })
                .anim((p, t, a) -> {
                    float up = win(t, .08f, .25f) * (1 - win(t, .8f, .95f));
                    // left fingers to the side of the helmet, head sweeps the area
                    p.lArmX = lerp(p.lArmX, -2.6f, up); p.lArmY = lerp(p.lArmY, -0.55f, up); p.lArmZ = lerp(p.lArmZ, 0.35f, up);
                    p.rArmX = -0.55f; p.rArmY = -0.35f; p.rHandX = -0.4f;
                    p.headY = track(t, 0, 0, .25f, 0.45f, .45f, 0.45f, .7f, -0.5f, .85f, -0.5f, 1, 0);
                    p.bodyY = p.headY * 0.35f; p.headX = -0.1f * up + 0.08f * bump(win(t, .28f, .45f));
                    p.bodyX = -0.06f * up;
                    p.lLegZ = -0.12f * up; p.rLegZ = 0.12f * up; p.rLegX = 0.15f * up; p.rootY = -0.3f * up;
                }));

        // ---------- Magnum: three paced, two-handed shots ----------
        out.add(AttackDef.of("magnum").range(2, 20).cd(26).weight(3).dur(24).weapon("magnum")
                .tick((e, t, n) -> {
                    if (n == 4 || n == 10 || n == 16) {
                        bullet(e, t, 0.012, 24, 5.5f, 0); flash(e, 0xffc060); casing(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 1f, 1.3f); snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.3f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .85f), r = kick(t, 4 / 24f, .15f) + kick(t, 10 / 24f, .15f) + kick(t, 16 / 24f, .15f);
                    pistol(p, k, r * 1.2f);
                    p.lLegX = lerp(p.lLegX, -0.2f, k); p.rLegX = lerp(p.rLegX, 0.2f, k); p.lLegZ -= 0.1f * k; p.rLegZ += 0.1f * k;
                    p.rootY -= 0.4f * k; p.headY -= 0.05f * k;
                }));

        // ---------- DMR: smart-link zoom, four deliberate shots ----------
        out.add(AttackDef.of("dmr").range(8, 38).cd(48).weight(3).dur(34).weapon("dmr")
                .tick((e, t, n) -> {
                    if (n == 5) snd(e, SoundEvents.SPYGLASS_USE, 1, 1.1f);
                    if (n == 12 || n == 17 || n == 22 || n == 27) {
                        bullet(e, t, 0.004, 40, 7f, 0); flash(e, 0xffc060); casing(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.9f, 1.5f);
                    }
                    if (n == 30) snd(e, SoundEvents.SPYGLASS_STOP_USING, 1, 1.1f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .88f), ads = win(t, .1f, .3f) * (1 - win(t, .82f, .92f));
                    float r = kick(t, 12 / 34f, .1f) + kick(t, 17 / 34f, .1f) + kick(t, 22 / 34f, .1f) + kick(t, 27 / 34f, .1f);
                    shoulder(p, k, 0, r * 1.1f, ads); stance(p, k);
                    p.rootY -= 0.3f * ads; p.bodyZ += 0.012f * (float) Math.sin(a * 0.35f) * ads;
                }));

        // ---------- Battle Rifle: two 3-round bursts ----------
        out.add(AttackDef.of("battle_rifle").range(4, 28).cd(30).weight(4).dur(30).weapon("br")
                .tick((e, t, n) -> {
                    if ((n >= 6 && n <= 8) || (n >= 16 && n <= 18)) {
                        bullet(e, t, 0.012, 32, 3.4f, 0); flash(e, 0xffc060);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, 1.7f - (n % 10) * 0.05f);
                        if (n == 7 || n == 17) casing(e);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .88f), ads = win(t, .08f, .2f) * (1 - win(t, .8f, .9f));
                    float r = 0.8f * (kick(t, 6 / 30f, .07f) + kick(t, 7 / 30f, .07f) + kick(t, 8 / 30f, .12f)
                            + kick(t, 16 / 30f, .07f) + kick(t, 17 / 30f, .07f) + kick(t, 18 / 30f, .12f));
                    shoulder(p, k, 0, r, ads * 0.6f); stance(p, k);
                    p.headX -= 0.06f * bump(win(t, .33f, .5f)); // reacquire between bursts
                }));

        // ---------- Assault Rifle: sustained automatic fire, muzzle climb ----------
        out.add(AttackDef.of("assault_rifle").range(2, 16).cd(36).weight(3).dur(38).weapon("ar")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 32 && n % 2 == 0) {
                        bullet(e, t, 0.05, 22, 2.1f, 0); flash(e, 0xffb050); casing(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.6f + e.getRandom().nextFloat() * 0.2f);
                    }
                    if (n == 34) snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.5f, 1.7f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), fire = win(t, .1f, .12f) * (1 - win(t, .84f, .86f));
                    float jit = 0.06f * (float) Math.sin(a * 6.1f) * fire, climb = 0.12f * win(t, .1f, .84f) * fire;
                    shoulder(p, k, climb, 0.35f * fire + jit, 0);
                    brace(p, k * 0.6f); stance(p, k * 0.4f);
                    p.rHandZ += jit * 0.5f; p.bodyZ += jit * 0.3f; p.lArmX -= climb * 0.3f;
                    p.bodyX += 0.12f * fire;
                }));

        // ---------- Sniper Rifle (SRS99): deep zoom, breath hold, piercing shot ----------
        out.add(AttackDef.of("sniper_rifle").range(12, 56).cd(90).weight(2).dur(46).weapon("sniper")
                .tick((e, t, n) -> {
                    if (n == 6) snd(e, SoundEvents.SPYGLASS_USE, 1, 0.8f);
                    if (n == 26) {
                        Vec3 m = mz(e), d = aim(e, t, 0);
                        Combat.hitscan(e, m, d, 60, 28f, 2, 0xffffff);
                        Fx.beam(L(e), m, m.add(d.scale(6)), TRACER, 1.1f, 0.25);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, m, 4, 0.1, 0.02); flash(e, 0xffd080);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.4f, 0.5f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.7f);
                    }
                    if (n == 32) { snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.7f); casing(e); }
                    if (n == 38) snd(e, SoundEvents.SPYGLASS_STOP_USING, 1, 0.8f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .9f), ads = win(t, .12f, .4f) * (1 - win(t, .78f, .9f)), r = 2.2f * kick(t, 26 / 46f, .22f);
                    float breath = (1 - win(t, .45f, .55f)) * ads;
                    shoulder(p, k, 0.02f * (float) Math.sin(a * 0.3f) * breath, r, ads); brace(p, k * 0.5f);
                    p.lLegX -= 0.2f * ads; p.rootY -= 0.4f * ads; p.bodyX += 0.08f * ads;
                    p.headX -= 0.2f * r; p.bodyX -= 0.15f * r; p.rootZ += r * 0.6f;
                    float mag = bump(win(t, .66f, .82f)); p.lArmX += 0.4f * mag * k; p.lArmY -= 0.3f * mag; // mag check
                }));

        // ---------- Rocket Launcher (SPNKr): two tubes, shoulder-fired, tube swap ----------
        out.add(AttackDef.of("rocket_launcher").range(7, 34).cd(110).weight(2).dur(42).weapon("spnkr")
                .when((e, t) -> t.getMaxHealth() >= 40 || Combat.around(e, t.position(), 4).size() >= 2)
                .tick((e, t, n) -> {
                    if (n == 10 || n == 22) {
                        Vec3 m = Combat.muzzle(e, 1.2, 0.3, 0.15);
                        Combat.shoot(e, ROCKET, m, aim(e, t, 0.01), 1.2, 16f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.7, 0.3, 0.15), 8, 0.2, 0.05); flash(e, 0xffa040);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5f, 0.5f); snd(e, SoundEvents.GHAST_SHOOT, 0.5f, 1.2f);
                    }
                    if (n == 30) snd(e, SoundEvents.PISTON_CONTRACT, 0.8f, 1.1f);
                    if (n == 36) snd(e, SoundEvents.PISTON_EXTEND, 0.8f, 1.2f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .88f), r = 1.8f * (kick(t, 10 / 42f, .25f) + kick(t, 22 / 42f, .25f));
                    shoulder(p, k, -0.08f, r, 0.5f); brace(p, k * 0.8f);
                    p.rArmZ -= 0.15f * k; p.rArmX -= 0.1f * k; // tube rides on top of the shoulder
                    p.rLegX += 0.3f * r; p.bodyX -= 0.12f * r;
                    float swap = bump(win(t, .68f, .9f)); // pull the spent tubes back, slam fresh pair
                    p.lArmX += 0.7f * swap * k; p.lArmY -= 0.4f * swap; p.headX += 0.2f * swap; p.bodyY += 0.15f * swap;
                }));

        // ---------- Gravity Hammer: overhead arch, ground-shaking slam ----------
        out.add(AttackDef.of("gravity_hammer").range(0, 4.2f).cd(70).weight(3).dur(30).weapon("hammer")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.6f);
                    if (n > 4 && n < 13) Fx.dustBurst(L(e), THRUST, 0.9f, e.position().add(0, 2.9, 0).add(fwd(e).scale(-0.3)), 2, 0.25);
                    if (n == 14) {
                        Vec3 c = e.position().add(fwd(e).scale(2.0)).add(0, 0.2, 0);
                        Combat.blast(e, c.add(0, 0.6, 0), 4.2f, 17f, 1.9f, null);
                        for (int i = 0; i < 3; i++) Fx.ring(L(e), c, 1.2f + i * 1.3f, i == 0 ? 0xbfe8ff : THRUST, 1.5f, 24 + i * 8);
                        Fx.burst(L(e), ParticleTypes.EXPLOSION, c, 2, 0.4, 0);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, c, 30, 1.2, 0.3);
                        Fx.burst(L(e), ParticleTypes.POOF, c, 20, 1.4, 0.05);
                        snd(e, SoundEvents.ANVIL_LAND, 1.4f, 0.5f); snd(e, SoundEvents.GENERIC_EXPLODE, 1f, 1.1f); snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.4f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    // arms overhead (hammer head trails behind), whip down, head bites the ground, heave back up
                    float arm = trackLin(t, 0, -0.6f, .3f, -3.0f, .4f, -3.05f, .47f, -1.0f, .7f, -0.95f, 1, -0.6f);
                    p.rArmX = arm; p.lArmX = arm + 0.15f;
                    p.rArmY = -0.15f; p.lArmY = track(t, 0, 0.6f, .3f, -0.25f, .47f, 0.35f, 1, 0.6f);
                    p.rArmZ = 0.1f; p.lArmZ = -0.05f;
                    p.rHandX = track(t, 0, -0.4f, .3f, 0.6f, .47f, -0.2f, .7f, -0.2f, 1, -0.4f);
                    p.bodyX = track(t, 0, 0, .3f, -0.4f, .4f, -0.42f, .5f, 0.6f, .7f, 0.55f, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, -0.2f, .47f, 0.1f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.headX = track(t, 0, 0, .3f, -0.35f, .5f, 0.25f, 1, 0);
                    p.rootY = track(t, 0, 0, .3f, 0.6f, .47f, -1.6f, .72f, -1.4f, 1, 0);
                    p.rootZ = track(t, 0, 0, .3f, 0.8f, .47f, -1.5f, .8f, -1.2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.1f, .47f, -0.6f, .8f, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, .47f, 0.45f, .8f, 0.4f, 1, 0);
                    p.lLegZ = -0.2f * win(t, .4f, .5f) * (1 - win(t, .8f, 1)); p.rLegZ = -p.lLegZ;
                    float shake = 0.05f * kick(t, .47f, .2f) * (float) Math.sin(a * 7);
                    p.bodyZ += shake; p.rArmY += shake;
                }));

        // ---------- Energy Sword: ignite, lunge, forehand cleave, backhand cut ----------
        out.add(AttackDef.of("energy_sword").range(0, 6.5f).cd(60).weight(3).dur(26).weapon("sword")
                .tick((e, t, n) -> {
                    if (n == 1) snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 2f);
                    if (n == 5 && t != null && e.distanceTo(t) > 2.2f) {
                        Combat.lunge(e, Math.min(1.3, (e.distanceTo(t) - 1.5) * 0.35), 0.05); exhaust(e, fwd(e), 4);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.7f, 1.6f);
                    }
                    if (n >= 8 && n <= 10) Fx.arc(L(e), e.position().add(0, 1.25, 0), e.getYRot(), 2.0f, -80, -80 + (n - 7) * 55, -10, SWORD_C, 1f, 12);
                    if (n == 10) {
                        for (LivingEntity f : Combat.cone(e, 3.6, 70)) { Combat.damage(e, f, 15); Combat.knock(f, e.position(), 0.6, 0.2); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(f), 10, 0.3, 0.2); }
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f); snd(e, SoundEvents.FIRECHARGE_USE, 0.6f, 1.8f);
                    }
                    if (n == 17) {
                        Fx.arc(L(e), e.position().add(0, 1.0, 0), e.getYRot(), 1.9f, 70, -60, 15, SWORD_C, 1f, 14);
                        for (LivingEntity f : Combat.cone(e, 3.4, 65)) { Combat.damage(e, f, 9); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(f), 6, 0.3, 0.2); }
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.1f);
                    }
                    if (n == 23) snd(e, SoundEvents.BEACON_DEACTIVATE, 0.5f, 2f);
                })
                .anim((p, t, a) -> {
                    // blade cocked out to the right, forehand sweep across the body, wrist-roll backhand return
                    p.rArmX = track(t, 0, -0.6f, .2f, -1.3f, .38f, -1.45f, .5f, -1.3f, .65f, -1.4f, .8f, -1.2f, 1, -0.6f);
                    p.rArmY = track(t, 0, 0, .2f, -0.9f, .3f, -0.95f, .4f, 0.7f, .52f, 0.75f, .65f, -0.6f, .8f, -0.55f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .2f, 0.5f, .4f, 0.1f, .65f, 0.3f, 1, 0);
                    p.rHandZ = track(t, 0, 0, .3f, -0.4f, .4f, 0.5f, .55f, 0.5f, .65f, -0.5f, 1, 0);
                    p.rHandX = track(t, 0, -0.3f, .2f, 0.2f, .65f, 0.2f, 1, -0.3f);
                    p.lArmX = track(t, 0, -0.4f, .25f, -1.2f, .4f, -0.3f, .65f, -0.9f, 1, -0.4f);
                    p.lArmZ = track(t, 0, 0, .25f, -0.3f, .4f, -0.8f, .65f, -0.3f, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, -0.55f, .4f, 0.65f, .55f, 0.6f, .65f, -0.45f, .8f, -0.35f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyX = track(t, 0, 0, .2f, 0.3f, .4f, 0.35f, 1, 0); p.headX = -0.1f * win(t, .1f, .3f) * (1 - win(t, .8f, 1));
                    p.rootZ = track(t, 0, 0, .18f, 0.6f, .3f, -2.4f, .8f, -1.8f, 1, 0);
                    p.rootY = track(t, 0, 0, .18f, -1.0f, .3f, -0.6f, .4f, -1.1f, .8f, -0.8f, 1, 0);
                    p.lLegX = track(t, 0, 0, .18f, -0.2f, .3f, -0.8f, .8f, -0.7f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.6f, .8f, 0.5f, 1, 0);
                }));

        // ---------- Frag Grenade: left-hand overhand lob ----------
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(150).weight(2).dur(20).weapons("br", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.4f);
                    if (n == 10) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.5);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, t, 0.02).add(0, 0.2 + dist * 0.02, 0).normalize();
                        Combat.shoot(e, FRAG, from, d, 0.7 + dist * 0.025, 15f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .52f ? "frag" : null;
                    p.rArmX = -0.75f; p.rArmY = -0.35f; p.rHandX = -0.4f;
                    p.lArmX = track(t, 0, -0.6f, .15f, -0.9f, .42f, -3.0f, .52f, -2.2f, .7f, -1.0f, 1, -0.85f);
                    p.lArmZ = track(t, 0, 0, .3f, -0.4f, .52f, 0.1f, 1, 0);
                    p.bodyY = track(t, 0, 0, .42f, -0.45f, .56f, 0.4f, .75f, 0.25f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .42f, -0.25f, .56f, 0.3f, 1, 0); p.headX = track(t, 0, 0, .4f, -0.25f, .6f, 0, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.4f, .56f, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, .42f, 0.3f, .56f, 0.45f, 1, 0);
                    p.rootZ = track(t, .4f, 0, .56f, -1.3f, 1, 0); p.rootY = -0.5f * bump(win(t, .4f, .85f));
                }));

        // ---------- Plasma Grenade: low sidearm whip, sticks to the target ----------
        out.add(AttackDef.of("plasma_grenade").range(4, 16).cd(170).weight(2).dur(22).weapons("br", "plasma")
                .tick((e, t, n) -> {
                    if (n < 10) Fx.dustBurst(L(e), PLASMA_C, 0.7f, Combat.muzzle(e, 0.3, -0.5, -0.2), 1, 0.08);
                    if (n == 2) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.7f, 2f);
                    if (n == 11) {
                        Vec3 from = Combat.muzzle(e, 0.7, -0.45, 0.2);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, t, 0.01).add(0, 0.08 + dist * 0.012, 0).normalize();
                        Combat.shoot(e, PLASMA_NADE, from, d, 0.9 + dist * 0.02, 18f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 1.1f); snd(e, SoundEvents.BLAZE_SHOOT, 0.4f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "plasma" : null;
                    p.rArmX = -0.7f; p.rArmY = -0.3f; p.rHandX = -0.45f; p.rArmZ = 0.15f;
                    // arm swings out wide and low, whips across at shoulder height
                    p.lArmX = track(t, 0, -0.6f, .3f, -0.4f, .5f, -1.6f, .7f, -1.3f, 1, -0.85f);
                    p.lArmZ = track(t, 0, 0, .3f, -1.1f, .5f, -0.2f, .65f, 0.3f, 1, 0);
                    p.lArmY = track(t, 0, 0.6f, .3f, 0.1f, .5f, 0.7f, 1, 0.7f);
                    p.bodyY = track(t, 0, 0, .3f, -0.6f, .5f, 0.55f, .75f, 0.35f, 1, 0); p.headY = -p.bodyY * 0.8f;
                    p.bodyZ = track(t, 0, 0, .3f, 0.12f, .5f, -0.08f, 1, 0); p.bodyX = 0.15f * bump(win(t, .35f, .8f));
                    p.lLegX = track(t, 0, 0, .25f, -0.2f, .5f, -0.6f, .8f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .5f, 0.4f, 1, 0);
                    p.rootY = track(t, 0, 0, .3f, -0.8f, .5f, 0.3f, .6f, -0.5f, 1, 0); p.rootZ = track(t, .3f, 0, .55f, -1.1f, 1, 0);
                }));
    }

    // ================= model: Mjolnir GEN2 (Hunter-pattern helmet), black with gold trim =================
    @Override
    public void buildModel(Rig rig) {
        Mat under = Mat.of(0x26282a).noise(.1f), black = Mat.of(0x1c1c1f).bevel(.3f).noise(.06f), dark = Mat.of(0x111113);
        Mat gold = Mat.of(GOLD).bevel(.35f).noise(.05f), metal = Mat.of(0x5c5e60).noise(.07f);
        Mat visor = Mat.of(VISOR).glow(), thrust = Mat.of(THRUST).glow();
        Mat trim = Mat.of(0x1c1c1f).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.TOP || f == Mat.Face.BOTTOM) return;
            g.rect(x, y + h - 1, w, 1, GOLD);
        });
        Mat chest = Mat.of(0x1c1c1f).bevel(.32f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            int cx = x + w / 2; // gold chevron trim + centre seam
            for (int i = 0; i < 4; i++) { g.px(x + 1 + i, y + 1 + i, GOLD); g.px(x + w - 2 - i, y + 1 + i, GOLD); }
            g.rect(cx - 1, y + 4, 2, h - 5, 0x2c2c30);
        });
        Mat helm = Mat.of(0x1c1c1f).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) { g.rect(x + 1, y + h / 2, w - 2, 1, GOLD); g.rect(x + w - 3, y + 2, 1, h / 2 - 2, GOLD); }
            if (f == Mat.Face.TOP) g.rect(x + w / 2 - 1, y, 2, h, GOLD);
        });
        // torso
        rig.box(rig.torso, under, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4, -12, -3, 8, 6, 2, .35f);
        rig.box(rig.torso, black, -3, -6, -2.8f, 6, 3, 1);
        rig.box(rig.torso, trim, -4.5f, -2.5f, -2.5f, 9, 2, 5);
        rig.box(rig.torso, gold, -1, -2.5f, -3.1f, 2, 2, 1);
        rig.box(rig.torso, dark, -5.5f, -2.8f, -1, 1, 2, 2);
        rig.box(rig.torso, dark, 4.5f, -2.8f, -1, 1, 2, 2);
        rig.box(rig.torso, black, -3.5f, -13, -2.5f, 7, 1, 5);
        // thruster pack
        rig.box(rig.torso, black, -3, -11, 2, 6, 8, 2);
        rig.box(rig.torso, metal, -3.5f, -10, 3.5f, 2, 5, 2);
        rig.box(rig.torso, metal, 1.5f, -10, 3.5f, 2, 5, 2);
        rig.box(rig.torso, thrust, -3, -5, 4, 1, 1, 1);
        rig.box(rig.torso, thrust, 2, -5, 4, 1, 1, 1);
        // helmet
        rig.box(rig.head, helm, -4, -8, -4, 8, 8, 8, .5f);
        rig.box(rig.head, visor, -3, -5.5f, -4.9f, 6, 2, 1);
        rig.box(rig.head, visor, -1.5f, -3.5f, -4.9f, 3, 1, 1);
        rig.box(rig.head, dark, -2, -2.5f, -5, 4, 2, 1);
        rig.box(rig.head, gold, -5, -5, -3, 1, 3, 4);
        rig.box(rig.head, gold, 4, -5, -3, 1, 3, 4);
        rig.box(rig.head, black, -1, -9, -4, 2, 1, 7);
        rig.box(rig.head, black, -3, -2, 4, 6, 2, 1);
        // arms
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, under, -2, -2, -2, 4, 12, 4);
            rig.box(arm, trim, s < 0 ? -3.5f : -2.5f, -3, -3, 6, 4, 6);
            rig.box(arm, black, -2, 1, -2, 4, 2, 4, .2f);
            rig.box(arm, black, -2, 5, -2, 4, 5, 4, .45f);
            rig.box(arm, gold, -2, 9.5f, -2, 4, 1, 4, .5f);
        }
        rig.box(rig.lArm, visor, 1.9f, 6, -1, 1, 2, 2); // tac-pad
        // legs
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, under, -2, 0, -2, 4, 12, 4);
            rig.box(leg, black, -2, 1, -2.8f, 4, 4, 1);
            rig.box(leg, black, s < 0 ? -2.8f : 1.8f, 1, -2, 1, 4, 4);
            rig.box(leg, gold, -1.5f, 5.5f, -2.9f, 3, 2, 1);
            rig.box(leg, black, -2, 7.5f, -2.6f, 4, 2, 1);
            rig.box(leg, trim, -2, 9, -3, 4, 3, 5, .25f);
        }
        buildWeapons(rig, metal, dark, black, gold);
    }

    private static void buildWeapons(Rig rig, Mat metal, Mat dark, Mat black, Mat gold) {
        Mat gun = Mat.of(0x3b3f42).noise(.08f), olive = Mat.of(0x4c5338).noise(.1f), grey = Mat.of(0x8a8d8f).noise(.06f);
        Mat lens = Mat.of(0x3fc0ff).glow(), red = Mat.of(0xff4030).glow(), counter = Mat.of(0x5fd0ff).glow();
        Mat blade = Mat.of(SWORD_C).glow(), hamBlue = Mat.of(THRUST).glow(), alien = Mat.of(0x3a2d55).noise(.1f), brute = Mat.of(0x4a3a2a).noise(.15f);
        PartDefinition w;
        w = rig.weapon(rig.rHand, "magnum");
        rig.box(w, grey, -1, -2.5f, -7, 2, 2, 8); rig.box(w, gun, -1, -0.5f, -5, 2, 1, 5); rig.box(w, black, -1, 0, -1, 2, 3, 2);
        rig.box(w, red, -0.5f, -3, -1, 1, 1, 1);
        w = rig.weapon(rig.rHand, "br");
        rig.box(w, gun, -1.5f, -3, -8, 3, 4, 11); rig.box(w, metal, -0.5f, -2, -13, 1, 1, 5); rig.box(w, black, -1, -5, -6, 2, 2, 5);
        rig.box(w, lens, -0.5f, -4.6f, -6.4f, 1, 1, 1); rig.box(w, dark, -1, 1, 0, 2, 3, 2); rig.box(w, dark, -1, 0, -3, 2, 3, 2);
        w = rig.weapon(rig.rHand, "dmr");
        rig.box(w, olive, -1.5f, -3, -9, 3, 3, 12); rig.box(w, metal, -0.5f, -2.5f, -16, 1, 1, 7); rig.box(w, black, -1, -5, -6, 2, 2, 6);
        rig.box(w, lens, -0.5f, -4.6f, -6.4f, 1, 1, 1); rig.box(w, dark, -1, 0, -5, 2, 4, 2); rig.box(w, olive, -1, -2, 3, 2, 3, 4);
        w = rig.weapon(rig.rHand, "ar");
        rig.box(w, gun, -2, -3, -7, 4, 4, 10); rig.box(w, dark, -1.5f, -2.5f, -12, 3, 3, 5); rig.box(w, counter, -1, -3.5f, -4, 2, 1, 2);
        rig.box(w, black, -1, 1, -5, 2, 4, 2); rig.box(w, gun, -1, -2, 3, 2, 3, 4);
        w = rig.weapon(rig.rHand, "sniper");
        rig.box(w, dark, -1.5f, -3, -8, 3, 3, 11); rig.box(w, metal, -0.5f, -2.5f, -22, 1, 1, 14); rig.box(w, black, -1, -5.5f, -7, 2, 2, 7);
        rig.box(w, lens, -0.5f, -5.3f, -7.4f, 1, 1, 1); rig.box(w, dark, -1, 0, -4, 2, 3, 2); rig.box(w, gun, -1, -2.5f, 3, 2, 4, 4);
        w = rig.weapon(rig.rHand, "spnkr");
        rig.box(w, olive, -3, -6, -14, 3, 3, 18); rig.box(w, olive, 0, -6, -14, 3, 3, 18); rig.box(w, black, -3.5f, -6.5f, -15, 7, 4, 1);
        rig.box(w, black, -1, -3, -2, 2, 3, 3); rig.box(w, red, 2.5f, -7, -6, 1, 1, 2);
        w = rig.weapon(rig.rHand, "hammer");
        rig.box(w, metal, -0.5f, -0.5f, -16, 1, 1, 22); rig.box(w, brute, -3, -3.5f, -21, 6, 7, 5); rig.box(w, alien, -2, -5.5f, -20, 4, 2, 3);
        rig.box(w, hamBlue, -2.5f, 3.5f, -20.5f, 5, 1, 4); rig.box(w, brute, -1, -1, 5, 2, 2, 2);
        w = rig.weapon(rig.rHand, "sword");
        rig.box(w, alien, -1, -1, -3, 2, 2, 5); rig.box(w, blade, -2.5f, -0.5f, -15, 1, 1, 12); rig.box(w, blade, 1.5f, -0.5f, -15, 1, 1, 12);
        rig.box(w, alien, -3, -1, -4, 6, 2, 1);
        w = rig.weapon(rig.rHand, "knife");
        rig.box(w, grey, -0.5f, -1, -7, 1, 2, 6); rig.box(w, black, -0.5f, -0.5f, -1, 1, 1, 2);
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, olive, -1, -1, -2, 2, 2, 2); rig.box(w, gold, -0.5f, -1.8f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "plasma");
        rig.box(w, Mat.of(PLASMA_C).glow(), -1, -1, -2, 2, 2, 2); rig.box(w, alien, -1.5f, -0.5f, -1.5f, 3, 1, 1);
    }
}
