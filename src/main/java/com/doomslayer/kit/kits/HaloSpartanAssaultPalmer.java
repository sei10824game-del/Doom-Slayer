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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/** Commander Sarah Palmer, Halo: Spartan Assault: Spartan-IV, UNSC + Covenant arsenal, armor abilities, recharging shields. */
public class HaloSpartanAssaultPalmer extends SlayerKit {
    static final int VISOR = 0xffb428, SHIELD = 0xffd84a, PLASMA_C = 0x4aa8ff, OC_GREEN = 0x8cff3a, TRACER = 0xffe28a, BLOOD = 0x6a1010;
    static final float SHIELD_MAX = 12f, OVERSHIELD = 30f;

    static final ProjStyle PLASMA_BOLT = ProjStyle.make().color(PLASMA_C).size(0.28f).life(45).trail(ParticleTypes.ELECTRIC_SPARK);
    static final ProjStyle OVERCHARGE = ProjStyle.make().color(OC_GREEN).size(0.75f).life(70).homing(0.14f).trail(ParticleTypes.HAPPY_VILLAGER)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, OC_GREEN, 1.8f, pos, 30, 0.6);
                Fx.ring(l, pos, 1.6f, OC_GREEN, 1.3f, 22);
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 20, 0.4, 0.3);
                Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1.2f, 1.6f);
                if (hit instanceof Mob m) m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3)); // EMP stun
            });
    static final ProjStyle SPNKR = ProjStyle.make().color(0xffa030).size(0.45f).life(100).explode(3.5f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle FRAG = ProjStyle.make().color(0x3f4a2a).size(0.26f).life(34).explode(3.5f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle PLASMA_NADE = ProjStyle.make().color(0x5ab8ff).size(0.34f).life(60).explode(3f).gravity(0.04).trail(ParticleTypes.ELECTRIC_SPARK)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 2.5f);
                Fx.dustBurst(l, 0x80d0ff, 2.2f, pos, 40, 1.2);
                Fx.ring(l, pos.add(0, 0.2, 0), 2.6f, 0x60b8ff, 1.6f, 30);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1.2f, 1.5f);
                Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1f, 2f);
            });

    public HaloSpartanAssaultPalmer() {
        super("halo_spartan_assault_palmer", "Sarah Palmer", "サラ・パーマー", "Halo: Spartan Assault");
        eggPrimary = 0xd8dcd8; eggSecondary = VISOR;
        maxHealth = 70; speed = 0.34; armor = 12; knockbackRes = 0.5;
        scale = 1.05f; width = 0.65f; preferredRange = 11f; dmgMult = 1.05f;
        idleWeaponR = "ma5";
    }

    // ================= helpers (server) =================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.15, 0.3, 0); }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        return Combat.dirTo(mz(e), t, spread, new Random(e.getRandom().nextLong()));
    }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static void flash(SlayerEntity e, int rgb) {
        Vec3 m = mz(e);
        Fx.burst(L(e), ParticleTypes.FLAME, m, 2, 0.04, 0.02);
        Fx.dustBurst(L(e), rgb, 1.1f, m, 3, 0.07);
    }
    static void brass(SlayerEntity e) { Fx.dustBurst(L(e), 0xc8a040, 0.5f, Combat.muzzle(e, 0.4, 0.55, 0.1), 1, 0.05); }
    static void bullet(SlayerEntity e, LivingEntity t, double spread, double range, float dmg, int pierce) {
        Combat.hitscan(e, mz(e), aim(e, t, spread), range, dmg, pierce, TRACER);
    }
    static void hold(SlayerEntity e, LivingEntity t, double dist) {
        if (t == null || !Combat.isFoe(e, t)) return;
        Vec3 f = Vec3.directionFromRotation(0, e.getYRot());
        Vec3 p = e.position().add(f.scale(dist + t.getBbWidth() * 0.5));
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true;
    }
    static Vec3 side(SlayerEntity e) { Vec3 l = e.getViewVector(1f); return new Vec3(-l.z, 0, l.x).normalize(); }
    static void shieldFx(SlayerEntity e, int n, double spread) { Fx.dustBurst(L(e), SHIELD, 1f, e.position().add(0, 1.1, 0), n, spread); }

    // ================= passive: Spartan energy shield (absorption layer that recharges) =================
    @Override
    public void passive(SlayerEntity e) {
        CompoundTag d = e.getPersistentData();
        float abs = e.getAbsorptionAmount(), now = e.getHealth() + abs;
        int delay = d.getInt("pal_delay"), os = d.getInt("pal_os");
        if (now < d.getFloat("pal_last") - 0.01f) {
            delay = 70; // took damage: recharge delay restarts
            shieldFx(e, 6, 0.4);
            if (abs <= 0 && d.getBoolean("pal_up")) { // shields popped
                d.putBoolean("pal_up", false);
                snd(e, SoundEvents.GLASS_BREAK, 0.8f, 1.6f); snd(e, SoundEvents.NOTE_BLOCK_BASS.value(), 1f, 0.5f);
                Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, e.position().add(0, 1, 0), 25, 0.5, 0.2);
            }
        } else if (delay > 0) {
            delay--;
            if (!d.getBoolean("pal_up") && e.tickCount % 10 == 0) snd(e, SoundEvents.NOTE_BLOCK_BIT.value(), 0.4f, 1.8f); // low-shield beep
        } else if (abs < SHIELD_MAX && e.tickCount % 3 == 0) {
            if (abs <= 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.4f);
            e.setAbsorptionAmount(Math.min(SHIELD_MAX, abs + 1)); d.putBoolean("pal_up", true);
            shieldFx(e, 3, 0.35);
        }
        if (os > 0) {
            os--;
            if (e.tickCount % 4 == 0) Fx.dustBurst(L(e), 0xffa0ff, 1f, e.position().add(0, 1.1, 0), 3, 0.45);
        } else if (e.getAbsorptionAmount() > SHIELD_MAX) e.setAbsorptionAmount(Math.max(SHIELD_MAX, e.getAbsorptionAmount() - 0.2f));
        d.putInt("pal_delay", delay); d.putInt("pal_os", os);
        d.putFloat("pal_last", e.getHealth() + e.getAbsorptionAmount());
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** shouldered rifle, chest bladed slightly right, cheek on stock */
    static void shoulder(Pose p, float k, float pitch, float recoil) {
        float tw = 0.22f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.12f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.06f - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.5f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * k; p.headX += 0.06f * k; p.bodyX += (recoil * 0.1f - pitch * 0.2f) * k; p.headX -= recoil * 0.07f * k;
        p.rootZ += recoil * 0.8f * k;
    }
    /** one-handed sidearm, body squared, off hand supporting */
    static void sidearm(Pose p, float k, float recoil) {
        p.rArmX = lerp(p.rArmX, -HALF - recoil * 0.4f, k); p.rArmY = lerp(p.rArmY, 0.1f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + 0.1f - recoil * 0.3f, k); p.lArmY = lerp(p.lArmY, 0.6f, k);
        p.rHandX -= recoil * 0.35f; p.bodyX -= recoil * 0.05f * k; p.rootZ += recoil * 0.4f * k;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.26f, k);
        p.lLegZ -= 0.05f * k; p.rLegZ += 0.07f * k; p.rootY -= 0.35f * k;
    }
    static void kneel(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -1.4f, k); p.rLegX = lerp(p.rLegX, 1.2f, k); p.rLegZ += 0.1f * k; p.rootY -= 4.5f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // MA5 at low ready, muzzle down-left, both hands on it
        p.rArmX = -0.6f + p.rArmX * 0.25f; p.rArmY = -0.2f;
        p.lArmX = -0.85f + p.lArmX * 0.25f; p.lArmY = 0.6f; p.lArmZ = 0.05f;
        p.rHandX = -0.3f; p.bodyX += 0.03f; p.headX += 0.02f * (float) Math.sin(age * 0.05f);
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- Rifle bash: step in, twist, drive the MA5 stock across the jaw ----------
        out.add(AttackDef.of("rifle_bash").range(0, 3.2f).cd(22).dur(16).weapon("ma5")
                .tick((e, t, n) -> {
                    if (n == 2) { Combat.lunge(e, 0.35, 0.02); snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.8f, 1.4f); }
                    if (n == 6) {
                        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.5f, -70, 40, -20, 0xdddddd, 0.8f, 10);
                        for (LivingEntity f : Combat.cone(e, 3.4, 50)) { Combat.damage(e, f, 9); Combat.knock(f, e.position(), 1.0, 0.3); Fx.sparks(L(e), mid(f), 8); }
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.1f, 0.9f); snd(e, SoundEvents.SHIELD_BLOCK, 0.8f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    // wind-up: rifle pulled back over right shoulder; strike: swing across; recover to ready
                    p.rArmX = track(t, 0, -1.0f, .3f, -2.3f, .4f, -1.4f, .6f, -1.2f, 1, -0.7f);
                    p.rArmY = track(t, 0, -0.2f, .3f, 0.4f, .4f, -0.7f, .6f, -0.6f, 1, -0.2f);
                    p.rArmZ = track(t, 0, 0, .3f, 0.5f, .4f, -0.2f, 1, 0);
                    p.lArmX = track(t, 0, -1.0f, .3f, -1.8f, .4f, -1.3f, .6f, -1.1f, 1, -0.85f);
                    p.lArmY = track(t, 0, 0.5f, .3f, 0.9f, .4f, 0.1f, 1, 0.6f);
                    p.rHandX = track(t, 0, -0.3f, .3f, 0.9f, .4f, -0.4f, 1, -0.3f);
                    p.rHandZ = track(t, .3f, 0, .4f, 0.5f, .7f, 0.3f, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, -0.55f, .4f, 0.6f, .6f, 0.45f, 1, 0); p.headY = -p.bodyY * 0.55f;
                    p.bodyX = track(t, 0, 0, .3f, -0.1f, .42f, 0.3f, 1, 0); p.headX = track(t, .35f, 0, .45f, 0.15f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, .7f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .38f, 0.45f, .7f, 0.35f, 1, 0);
                    p.rootZ = track(t, 0, 0, .3f, 0.5f, .42f, -2.2f, .7f, -1.8f, 1, 0); p.rootY = -0.6f * bump(win(t, .3f, .9f));
                    p.bodyZ = 0.06f * bump(win(t, .38f, .55f));
                }));

        // ---------- Assassination: grab from front, M11 knife under the chin, drop ----------
        out.add(AttackDef.of("assassination").range(0, 3.5f).lowHealthTarget(0.25f).weight(6).cd(80).dur(30).weapons("knife", null)
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.5, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.8f); }
                    if (n < 24) hold(e, t, 0.85);
                    if (n == 5) snd(e, SoundEvents.ARMOR_EQUIP_CHAIN, 1, 1.3f);
                    if (n == 14 && t != null) {
                        Fx.dustBurst(L(e), BLOOD, 1.3f, mid(t), 22, 0.3); Fx.sparks(L(e), mid(t), 6);
                        Combat.damage(e, t, t.getHealth() + 100);
                        snd(e, SoundEvents.TRIDENT_HIT, 1.2f, 0.8f); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1, 0.7f);
                    }
                    if (n == 22) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.8f, 1.1f);
                })
                .anim((p, t, a) -> {
                    // left hand clamps the head, right hand reverse-grip knife: draw back, drive up, twist, pull out
                    p.lArmX = track(t, 0, 0, .15f, -1.75f, .7f, -1.6f, .85f, -0.6f, 1, 0); p.lArmY = track(t, 0, 0, .15f, 0.45f, .7f, 0.4f, 1, 0);
                    p.lArmZ = track(t, .4f, 0, .48f, 0.15f, .8f, 0, 1, 0);
                    p.rArmX = track(t, 0, 0, .2f, -0.3f, .4f, 0.5f, .47f, -1.9f, .7f, -1.8f, .82f, -0.9f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .4f, 0.3f, .47f, -0.25f, .8f, -0.1f, 1, 0);
                    p.rArmY = 0.25f * (float) Math.sin(win(t, .5f, .68f) * PI * 2);
                    p.rHandX = track(t, 0, 0, .3f, 1.4f, .5f, 0.6f, .7f, 0.6f, .85f, 1.2f, 1, 0);
                    p.bodyY = track(t, 0, 0, .4f, 0.35f, .47f, -0.3f, .8f, -0.2f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.bodyX = track(t, 0, 0, .15f, 0.2f, .47f, 0.35f, .8f, 0.25f, 1, 0);
                    p.headX = track(t, 0, 0, .45f, 0.1f, .55f, 0.35f, 1, 0);
                    p.rootZ = track(t, 0, 0, .15f, -1.8f, .85f, -1.8f, 1, 0); p.rootY = track(t, .4f, 0, .5f, 0.5f, .6f, -0.6f, 1, 0);
                    p.lLegX = track(t, 0, 0, .15f, -0.5f, .85f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .15f, 0.35f, .5f, 0.5f, .85f, 0.35f, 1, 0);
                }));

        // ---------- MA5 Assault Rifle: sustained automatic fire ----------
        out.add(AttackDef.of("ma5_assault_rifle").range(2, 20).cd(28).weight(4).dur(34).weapon("ma5")
                .tick((e, t, n) -> {
                    if (n >= 4 && n < 28) {
                        bullet(e, t, 0.04 + (n - 4) * 0.002, 22, 1.7f, 0);
                        if (n % 2 == 0) { flash(e, 0xffb050); brass(e); }
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.6f, 1.6f + e.getRandom().nextFloat() * 0.2f);
                    }
                    if (n == 29) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.5f, 1.8f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .88f), fire = win(t, .1f, .12f) * (1 - win(t, .82f, .84f));
                    float r = (0.3f + 0.15f * (float) Math.sin(a * PI)) * fire;
                    shoulder(p, k, -0.04f * fire * win(t, .12f, .82f), r); stance(p, k);
                    p.rHandZ += 0.03f * (float) Math.sin(a * 3.7f) * fire; p.bodyZ += 0.015f * (float) Math.sin(a * 2.9f) * fire;
                    p.lArmX += 0.25f * bump(win(t, .84f, .98f)); // slap the mag
                }));

        // ---------- BR55 Battle Rifle: two 3-round bursts ----------
        out.add(AttackDef.of("br55_battle_rifle").range(5, 28).cd(34).weight(4).dur(30).weapon("br55")
                .tick((e, t, n) -> {
                    if (n == 6 || n == 7 || n == 8 || n == 16 || n == 17 || n == 18) {
                        bullet(e, t, 0.008, 30, 3f, 0); flash(e, 0xffc060); brass(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, 1.25f);
                    }
                    if (n == 3) snd(e, SoundEvents.SPYGLASS_USE, 0.7f, 1.2f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .88f), zoom = win(t, .08f, .2f) * (1 - win(t, .7f, .85f));
                    float r = 1.1f * (kick(t, 6 / 30f, .13f) + kick(t, 16 / 30f, .13f));
                    shoulder(p, k, 0, r); stance(p, k);
                    p.headX += 0.12f * zoom; p.headY -= 0.08f * zoom; p.bodyX += 0.06f * zoom; p.rootY -= 0.3f * zoom;
                    p.rHandX -= r * 0.25f;
                }));

        // ---------- M7 SMG: hip-spray while strafing ----------
        out.add(AttackDef.of("m7_smg").range(1, 13).cd(30).weight(3).dur(28).weapon("smg").moving()
                .tick((e, t, n) -> {
                    if (n >= 3 && n < 24) {
                        bullet(e, t, 0.08, 15, 1.3f, 0); if (n % 2 == 1) { flash(e, 0xffb050); brass(e); }
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.5f, 1.9f + e.getRandom().nextFloat() * 0.1f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .85f), fire = win(t, .1f, .12f) * (1 - win(t, .84f, .86f));
                    float jit = 0.05f * (float) Math.sin(a * 6.1f) * fire;
                    p.rArmX = lerp(p.rArmX, -1.25f - 0.15f * fire + jit, k); p.rArmY = lerp(p.rArmY, -0.1f, k);
                    p.lArmX = lerp(p.lArmX, -1.2f + jit, k); p.lArmY = lerp(p.lArmY, 0.7f, k);
                    p.rHandX = lerp(p.rHandX, -0.3f, k); p.rHandZ += jit;
                    p.bodyY += (0.25f + 0.15f * (float) Math.sin(a * 0.4f)) * k; p.headY -= 0.25f * k; // sweep across targets
                    p.bodyX += 0.1f * k; p.rootZ += 0.3f * fire; stance(p, k);
                }));

        // ---------- M45 Shotgun: blast + pump ----------
        out.add(AttackDef.of("m45_shotgun").range(0, 7).cd(32).weight(4).dur(24).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 5) {
                        for (int i = 0; i < 10; i++) bullet(e, t, 0.11, 10, 2.4f, 0);
                        for (LivingEntity f : Combat.cone(e, 4, 35)) Combat.knock(f, e.position(), 1.0, 0.3);
                        flash(e, 0xffa040); flash(e, 0xff7020);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.7f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 0.75f);
                    }
                    if (n == 12) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.7f);
                    if (n == 16) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.8f); brass(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 2f * kick(t, 5 / 24f, .28f), pump = bump(win(t, .5f, .72f));
                    shoulder(p, k, 0, r); stance(p, k);
                    p.rHandX -= r * 0.4f; p.bodyX -= r * 0.15f; p.rootY += r * 0.4f;
                    p.lArmX += 0.4f * pump * k; p.lArmY -= 0.1f * pump; p.bodyZ += 0.04f * pump; p.headX += 0.05f * pump;
                }));

        // ---------- SRS99 Sniper Rifle: drop to a knee, scope, single high-power round ----------
        out.add(AttackDef.of("srs99_sniper").range(10, 48).cd(80).weight(2).dur(40).weapon("sniper")
                .tick((e, t, n) -> {
                    if (n == 6) snd(e, SoundEvents.SPYGLASS_USE, 1, 0.9f);
                    if (n == 24) {
                        Vec3 d = aim(e, t, 0), m = mz(e);
                        Combat.hitscan(e, m, d, 50, 22f, 2, 0xfff4c0);
                        Fx.beam(L(e), m, t != null ? mid(t) : m.add(d.scale(50)), 0xffffff, 0.5f, 0.35);
                        flash(e, 0xffd080); Fx.burst(L(e), ParticleTypes.CLOUD, m, 4, 0.1, 0.02);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.4f, 0.55f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 2f);
                    }
                    if (n == 31) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.4f);
                    if (n == 34) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.5f); brass(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), knee = win(t, 0, .2f) * (1 - win(t, .85f, 1)), zoom = win(t, .15f, .35f) * (1 - win(t, .65f, .8f));
                    float r = 2.2f * kick(t, 24 / 40f, .2f), bolt = bump(win(t, .75f, .88f));
                    shoulder(p, k, 0, r); kneel(p, knee);
                    p.headX += 0.2f * zoom; p.headY -= 0.1f * zoom; p.bodyX += 0.15f * zoom;
                    p.bodyZ += 0.008f * (float) Math.sin(a * 0.35f) * zoom; // breathing
                    p.rHandX -= r * 0.4f; p.headX -= r * 0.1f;
                    p.rArmX += 0.35f * bolt; p.rArmZ += 0.2f * bolt; // work the bolt
                }));

        // ---------- M6 Magnum: three aimed shots ----------
        out.add(AttackDef.of("m6_magnum").range(3, 22).cd(30).weight(2).dur(24).weapon("magnum")
                .tick((e, t, n) -> {
                    if (n == 4 || n == 10 || n == 16) {
                        bullet(e, t, 0.012, 24, 4f, 0); flash(e, 0xffc060);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 1f, 1.1f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), r = 1.3f * (kick(t, 4 / 24f, .12f) + kick(t, 10 / 24f, .12f) + kick(t, 16 / 24f, .12f));
                    p.bodyY -= 0.3f * k; p.headY += 0.3f * k; sidearm(p, k, r);
                    p.lLegX = lerp(p.lLegX, 0.25f, k); p.rLegX = lerp(p.rLegX, -0.2f, k); p.rootY -= 0.3f * k;
                }));

        // ---------- Plasma Rifle (Covenant): rapid bolts with overheat vent ----------
        out.add(AttackDef.of("plasma_rifle").range(3, 20).cd(34).weight(3).dur(32).weapons("plasma_rifle", null)
                .tick((e, t, n) -> {
                    if (n >= 3 && n <= 21 && n % 2 == 1) {
                        Combat.shoot(e, PLASMA_BOLT, mz(e), aim(e, t, 0.035), 1.6, 2.4f);
                        Fx.dustBurst(L(e), PLASMA_C, 0.8f, mz(e), 2, 0.06);
                        snd(e, SoundEvents.BLAZE_SHOOT, 0.45f, 1.9f);
                    }
                    if (n == 23) { snd(e, SoundEvents.FIRE_EXTINGUISH, 0.8f, 1.2f); Fx.burst(L(e), ParticleTypes.CLOUD, Combat.muzzle(e, 0.6, 0.3, 0.1), 8, 0.15, 0.04); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), fire = win(t, .08f, .1f) * (1 - win(t, .66f, .68f)), vent = bump(win(t, .7f, .92f));
                    float r = 0.3f * (0.5f + 0.5f * (float) Math.sin(a * PI)) * fire;
                    // Covenant weapon held one-handed forward, off hand braced on forearm
                    sidearm(p, k, r); p.bodyY += 0.15f * k; p.headY -= 0.1f * k; stance(p, k);
                    p.rArmX += 0.4f * vent; p.rHandY += 0.6f * vent; p.headX += 0.1f * vent; // tilt to vent heat
                    p.lArmX += 0.3f * vent;
                }));

        // ---------- Plasma Pistol: overcharge (EMP homing bolt) ----------
        out.add(AttackDef.of("plasma_pistol_overcharge").range(4, 22).cd(90).weight(2).dur(34).weapon("plasma_pistol")
                .tick((e, t, n) -> {
                    if (n < 20) {
                        Fx.dustBurst(L(e), OC_GREEN, 0.7f + n * 0.03f, mz(e), 2, 0.05 + n * 0.006);
                        if (n % 4 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.8f, 1f + n * 0.04f);
                    }
                    if (n == 20) {
                        Combat.shoot(e, OVERCHARGE, mz(e), aim(e, t, 0), 1.0, 9f);
                        snd(e, SoundEvents.ILLUSIONER_CAST_SPELL, 1.2f, 1.5f); snd(e, SoundEvents.BLAZE_SHOOT, 0.8f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), ch = win(t, .05f, .58f) * (1 - win(t, .58f, .6f)), r = 1.8f * kick(t, 20 / 34f, .25f);
                    p.bodyY -= 0.3f * k; p.headY += 0.3f * k; sidearm(p, k, r);
                    p.rArmZ += 0.035f * ch * ch * (float) Math.sin(a * 5.5f); p.bodyZ += 0.02f * ch * (float) Math.sin(a * 4.1f); // charge shudder
                    p.rootY -= 0.4f * ch; p.lArmX += 0.15f * ch; stance(p, k);
                }));

        // ---------- M41 SPNKr Rocket Launcher: two tubes, kneel and fire both ----------
        out.add(AttackDef.of("m41_spnkr").range(6, 30).cd(70).weight(2).dur(36).weapon("spnkr")
                .when((e, t) -> t.getMaxHealth() >= 30 || Combat.around(e, t.position(), 4).size() >= 2)
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.PISTON_EXTEND, 0.6f, 1.3f);
                    if (n == 10 || n == 20) {
                        Combat.shoot(e, SPNKR, Combat.muzzle(e, 1.2, n == 10 ? 0.22 : 0.38, 0.15), aim(e, t, 0.015), 1.2, 14f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.7, 0.3, 0.15), 6, 0.2, 0.05); flash(e, 0xffa040);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.6f); snd(e, SoundEvents.GHAST_SHOOT, 0.4f, 1.3f);
                    }
                    if (n == 28) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.7f, 1f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), knee = win(t, .05f, .22f) * (1 - win(t, .82f, .98f));
                    float r = 1.8f * (kick(t, 10 / 36f, .2f) + kick(t, 20 / 36f, .2f));
                    shoulder(p, k, -0.05f, r); kneel(p, knee * 0.7f);
                    p.rHandX -= r * 0.4f; p.bodyX -= r * 0.12f; p.headX -= r * 0.1f;
                    p.lArmX += 0.3f * bump(win(t, .65f, .82f)); // swap tubes
                }));

        // ---------- Frag grenade: left-hand overarm throw ----------
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(150).weight(2).dur(20).weapons("ma5", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.4f);
                    if (n == 10) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.4);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Combat.shoot(e, FRAG, from, aim(e, t, 0.02).add(0, 0.15 + dist * 0.018, 0).normalize(), 0.75 + dist * 0.025, 13f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.9f);
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

        // ---------- Plasma grenade: underarm sidearm toss that sticks ----------
        out.add(AttackDef.of("plasma_grenade").range(4, 15).cd(170).weight(2).dur(22).weapons("ma5", "plasma_nade")
                .tick((e, t, n) -> {
                    if (n < 9 && n % 3 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.6f, 1.8f);
                    if (n == 11) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.45, 0.1);
                        double dist = t != null ? from.distanceTo(mid(t)) : 9;
                        Combat.shoot(e, PLASMA_NADE, from, aim(e, t, 0.01).add(0, 0.08 + dist * 0.02, 0).normalize(), 0.8 + dist * 0.025, 15f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 1.3f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "plasma_nade" : null;
                    p.rArmX = -0.6f; p.rArmY = -0.35f; p.rHandX = -0.3f;
                    // arm sweeps back low, then whips forward side-arm across the body
                    p.lArmX = track(t, 0, 0, .2f, 0.6f, .45f, 0.9f, .55f, -1.5f, .75f, -1.2f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .45f, -0.9f, .55f, 0.3f, .75f, 0.2f, 1, 0);
                    p.lArmY = track(t, .45f, 0, .55f, 0.6f, 1, 0);
                    p.bodyY = track(t, 0, 0, .45f, -0.7f, .58f, 0.55f, .8f, 0.35f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyZ = track(t, 0, 0, .45f, -0.12f, .58f, 0.08f, 1, 0);
                    p.lLegX = track(t, 0, 0, .45f, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, .45f, 0.3f, .6f, 0.4f, 1, 0);
                    p.rootY = -1.2f * bump(win(t, .2f, .85f)); p.rootZ = track(t, .45f, 0, .6f, -1.2f, 1, 0);
                }));

        // ---------- Armor Lock: slam fists down, invulnerable shell, EMP release ----------
        out.add(AttackDef.of("armor_lock").range(0, 10).cd(400).weight(4).dur(50).weapon(null).noLos()
                .when((e, t) -> e.getAbsorptionAmount() <= 0 && e.getHealth() < e.getMaxHealth() * 0.6f)
                .tick((e, t, n) -> {
                    if (n == 6) {
                        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 38, 4, false, false));
                        snd(e, SoundEvents.ANVIL_LAND, 0.9f, 0.6f); snd(e, SoundEvents.BEACON_ACTIVATE, 1, 1.4f);
                        Fx.ring(L(e), e.position().add(0, 0.1, 0), 1.5f, 0x80c0ff, 1.3f, 24);
                    }
                    if (n > 6 && n < 44) {
                        if (n % 3 == 0) Fx.dustBurst(L(e), 0x80c8ff, 1.1f, e.position().add(0, 1, 0), 8, 0.6);
                        if (n % 8 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.9f, 1.6f);
                        // deflect incoming hostile projectiles
                        for (Projectile pr : e.level().getEntitiesOfClass(Projectile.class, e.getBoundingBox().inflate(2.5),
                                pr -> !(pr.getOwner() instanceof Player) && !(pr.getOwner() instanceof SlayerEntity))) {
                            Fx.sparks(L(e), pr.position(), 6); snd(e, SoundEvents.SHIELD_BLOCK, 0.8f, 1.5f); pr.discard();
                        }
                    }
                    if (n == 44) {
                        Vec3 c = e.position().add(0, 1, 0);
                        for (LivingEntity f : Combat.around(e, c, 5)) { Combat.damage(e, f, 6); Combat.knock(f, c, 1.4, 0.45); }
                        Fx.ring(L(e), c, 2.5f, 0xa0d8ff, 1.6f, 30); Fx.ring(L(e), c, 4.5f, 0x60a0ff, 1.4f, 40);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, c, 40, 1.5, 0.3);
                        snd(e, SoundEvents.BEACON_DEACTIVATE, 1.4f, 1.2f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float lock = win(t, .08f, .14f) * (1 - win(t, .86f, .94f)), up = bump(win(t, .86f, 1));
                    p.rArmX = track(t, 0, 0, .08f, -2.6f, .13f, 0.15f, .86f, 0.15f, .92f, -1.2f, 1, 0);
                    p.lArmX = p.rArmX;
                    p.rArmZ = track(t, 0, 0, .08f, 0.3f, .13f, -0.2f, .86f, -0.2f, .93f, 0.9f, 1, 0); p.lArmZ = -p.rArmZ;
                    p.bodyX = 0.55f * lock - 0.25f * up; p.headX = 0.35f * lock - 0.3f * up;
                    p.rootY = -4f * lock + 1.2f * up; p.rootZ = track(t, 0, 0, .08f, 0.6f, .14f, 0, 1, 0);
                    p.lLegX = -0.9f * lock; p.rLegX = 0.7f * lock; p.lLegZ = -0.25f * lock; p.rLegZ = 0.25f * lock;
                    p.bodyZ = 0.012f * (float) Math.sin(a * 7f) * lock; // humming shell
                }));

        // ---------- Overshield: brace, flare up a triple-strength shield ----------
        out.add(AttackDef.of("overshield").range(0, 24).cd(600).weight(3).dur(30).weapon(null).noLos()
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.8f || Combat.around(e, e.position(), 10).size() >= 3)
                .tick((e, t, n) -> {
                    if (n < 14 && n % 3 == 0) { snd(e, SoundEvents.BEACON_POWER_SELECT, 0.7f, 0.9f + n * 0.06f); Fx.dustBurst(L(e), 0xff90ff, 1f, e.position().add(0, 0.3 + n * 0.1, 0), 6, 0.5); }
                    if (n == 15) {
                        e.setAbsorptionAmount(OVERSHIELD); e.getPersistentData().putInt("pal_os", 300); e.getPersistentData().putBoolean("pal_up", true);
                        e.getPersistentData().putFloat("pal_last", e.getHealth() + OVERSHIELD);
                        Vec3 c = e.position().add(0, 1, 0);
                        for (int i = 0; i < 3; i++) Fx.ring(L(e), c.add(0, -0.8 + i * 0.8, 0), 1.2f, 0xffa0ff, 1.4f, 22);
                        Fx.dustBurst(L(e), SHIELD, 1.6f, c, 30, 0.8); Fx.burst(L(e), ParticleTypes.END_ROD, c, 20, 0.6, 0.1);
                        snd(e, SoundEvents.TOTEM_USE, 0.6f, 1.6f); snd(e, SoundEvents.BEACON_ACTIVATE, 1.2f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float gather = win(t, 0, .45f) * (1 - win(t, .45f, .5f)), burst = bump(win(t, .48f, .8f));
                    // fists pulled to chest, shoulders hunched, then arms flung wide and chest open
                    p.rArmX = track(t, 0, 0, .4f, -1.3f, .5f, -0.6f, .8f, -0.5f, 1, 0); p.lArmX = p.rArmX;
                    p.rArmY = track(t, 0, 0, .4f, 0.8f, .5f, -0.3f, 1, 0); p.lArmY = -p.rArmY;
                    p.rArmZ = track(t, 0, 0, .4f, -0.1f, .52f, 1.2f, .8f, 1.0f, 1, 0); p.lArmZ = -p.rArmZ;
                    p.bodyX = 0.35f * gather - 0.25f * burst; p.headX = 0.3f * gather - 0.35f * burst;
                    p.rootY = -1.5f * gather + 0.8f * burst;
                    p.lLegZ = -0.2f * burst; p.rLegZ = 0.2f * burst; p.lLegX = -0.2f * gather; p.rLegX = 0.2f * gather;
                    p.bodyZ = 0.03f * gather * (float) Math.sin(a * 6f);
                }));

        // ---------- Evade: armor-boosted combat roll to the side ----------
        out.add(AttackDef.of("evade").range(0, 8).cd(140).weight(2).dur(14).weapon("ma5").moving()
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.7f || e.getAbsorptionAmount() <= 0)
                .tick((e, t, n) -> {
                    if (n == 1) {
                        Vec3 s = side(e).scale(e.getRandom().nextBoolean() ? 0.9 : -0.9);
                        e.setDeltaMovement(s.x, 0.18, s.z); e.hurtMarked = true;
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.8f, 1.6f);
                        Fx.burst(L(e), ParticleTypes.CLOUD, e.position().add(0, 0.6, 0), 8, 0.3, 0.05);
                    }
                    if (n == 10) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.7f, 1.2f);
                })
                .anim((p, t, a) -> {
                    float tuck = bump(win(t, .05f, .8f));
                    p.rootRoll = 0.6f * (float) Math.sin(win(t, .05f, .8f) * PI) ; p.rootY = -3f * tuck;
                    p.bodyX = 0.6f * tuck; p.headX = 0.3f * tuck;
                    p.rArmX = -0.9f - 0.3f * tuck; p.rArmY = -0.2f; p.lArmX = -1.1f; p.lArmY = 0.6f; p.rHandX = -0.3f;
                    p.lLegX = -1.3f * tuck; p.rLegX = -0.8f * tuck; p.lLegZ = -0.3f * tuck;
                }));

        // ---------- Active Camo: wrist tap, shimmer, vanish ----------
        out.add(AttackDef.of("active_camo").range(4, 24).cd(700).weight(1).dur(24).weapon("ma5")
                .tick((e, t, n) -> {
                    if (n == 8) snd(e, SoundEvents.UI_BUTTON_CLICK.value(), 0.8f, 1.6f);
                    if (n > 8 && n < 20) Fx.burst(L(e), ParticleTypes.REVERSE_PORTAL, e.position().add(0, 1, 0), 4, 0.35, 0.02);
                    if (n == 16) {
                        e.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, false, false));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0, false, false));
                        snd(e, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1, 1.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float tap = win(t, .1f, .3f) * (1 - win(t, .6f, .8f)), crouch = win(t, .3f, .7f) * (1 - win(t, .85f, 1));
                    // left forearm raised across chest, right fingers tap the wrist pad; then sink low
                    p.lArmX = lerp(-0.85f, -1.6f, tap); p.lArmY = lerp(0.6f, -0.7f, tap);
                    p.rArmX = lerp(-0.6f, -1.3f, tap) + 0.15f * bump(win(t, .28f, .4f)); p.rArmY = lerp(-0.2f, 0.6f, tap); p.rHandX = -0.3f;
                    p.headX = 0.35f * tap; p.headY = 0.2f * tap; p.bodyY = -0.15f * tap;
                    p.bodyX = 0.3f * crouch; p.rootY = -1.8f * crouch; p.lLegX = -0.5f * crouch; p.rLegX = 0.4f * crouch;
                }));
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat under = Mat.of(0x2b2e33).noise(.1f), plate = Mat.of(0xd6d9d6).bevel(.32f).noise(.06f), trim = Mat.of(0x55595f).noise(.08f);
        Mat accent = Mat.of(0xc8572a).bevel(.2f), metal = Mat.of(0x7a7e84).noise(.06f), black = Mat.of(0x18191b);
        Mat visor = Mat.of(VISOR).glow(), light = Mat.of(0x9fd8ff).glow();
        Mat chest = Mat.of(0xd6d9d6).bevel(.32f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            g.rect(x + 1, y + h - 2, w - 2, 1, 0x55595f); // lower plate seam
            g.rect(x + w / 2 - 1, y + 1, 2, 1, 0xc8572a); // UNSC chevron stripe
        });
        Mat helm = Mat.of(0xd6d9d6).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.TOP) g.rect(x + w / 2 - 1, y, 2, h, 0xc8572a); // crest stripe
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) g.rect(x + 1, y + h / 2, w - 2, 1, 0x55595f);
        });
        // torso: slim Spartan-IV undersuit, sculpted chest plate, abdomen segments, armor-ability pack
        rig.box(rig.torso, under, -3.5f, -12, -2, 7, 12, 4);
        rig.box(rig.torso, chest, -4, -12, -2.8f, 8, 6, 2, .2f);
        rig.box(rig.torso, trim, -3, -6, -2.5f, 6, 3, 1);
        rig.box(rig.torso, light, -0.5f, -9, -3.3f, 1, 1, 1);
        rig.box(rig.torso, black, -4, -2.5f, -2.4f, 8, 2, 5);
        rig.box(rig.torso, metal, -1, -2.4f, -2.9f, 2, 2, 1);
        rig.box(rig.torso, plate, -3.5f, -11.5f, 1.8f, 7, 7, 2);
        rig.box(rig.torso, trim, -2.5f, -10.5f, 3.6f, 5, 5, 1); // armor ability module
        rig.box(rig.torso, light, -1.5f, -9, 4.4f, 3, 1, 1);
        rig.box(rig.torso, black, 3, -3, -1, 2, 3, 2); // magnum holster
        // helmet: Halo 4 style, angular dome, wide gold visor, chin guard, side vents
        rig.box(rig.head, helm, -4, -8, -4, 8, 8, 8, .4f);
        rig.box(rig.head, visor, -3.5f, -6, -4.8f, 7, 2, 1);
        rig.box(rig.head, visor, -2.5f, -4, -4.7f, 5, 1, 1);
        rig.box(rig.head, trim, -2, -3, -4.9f, 4, 3, 1);
        rig.box(rig.head, accent, -1, -9, -2, 2, 1, 5);
        rig.box(rig.head, metal, -4.8f, -5, -2, 1, 2, 4);
        rig.box(rig.head, metal, 3.8f, -5, -2, 1, 2, 4);
        rig.box(rig.head, black, 3.6f, -9, 0, 1, 3, 1); // antenna
        // arms: layered pauldrons with orange edge, forearm guards, TacPad on left wrist
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, under, -1.5f, -2, -1.5f, 3, 12, 3);
            rig.box(arm, plate, s < 0 ? -3 : -2, -2.5f, -2.5f, 5, 4, 5, .1f);
            rig.box(arm, accent, s < 0 ? -3.2f : 1.2f, 0.5f, -2.5f, 2, 1, 5);
            rig.box(arm, trim, -2, 5, -2, 4, 4, 4, .15f);
            rig.box(arm, black, -1.5f, 9, -1.5f, 3, 2, 3);
        }
        rig.box(rig.lArm, light, -0.5f, 6, -2.6f, 1, 2, 1);
        // legs: thigh plates, knee caps, heavy boots
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, under, -2, 0, -2, 4, 12, 4);
            rig.box(leg, plate, -2, 1, -2.6f, 4, 4, 1);
            rig.box(leg, metal, -1.5f, 5.5f, -2.8f, 3, 2, 1);
            rig.box(leg, plate, -2, 9, -3, 4, 3, 5, .2f);
            rig.box(leg, accent, -2, 8.5f, -3.1f, 4, 1, 1);
        }
        buildWeapons(rig, metal, black);
    }

    private static void buildWeapons(Rig rig, Mat metal, Mat black) {
        Mat gun = Mat.of(0x3c4038).noise(.08f), olive = Mat.of(0x4c5640).noise(.1f), grey = Mat.of(0x8a8e8a).noise(.06f);
        Mat amber = Mat.of(0xffa030).glow(), cov = Mat.of(0x5a3f8a).bevel(.35f), covG = Mat.of(0x2f6a2a).bevel(.35f);
        Mat blue = Mat.of(PLASMA_C).glow(), green = Mat.of(OC_GREEN).glow(), red = Mat.of(0xff3030).glow();
        PartDefinition w;
        w = rig.weapon(rig.rHand, "ma5"); // bullpup MA5: body, shroud, carry handle, ammo counter, mag
        rig.box(w, olive, -1.5f, -3, -9, 3, 4, 13); rig.box(w, gun, -1, -2.5f, -14, 2, 2, 5);
        rig.box(w, black, -1, -5, -6, 2, 2, 6); rig.box(w, amber, -0.5f, -4, -9.6f, 1, 1, 1);
        rig.box(w, black, -1, 1, 0, 2, 4, 2);
        w = rig.weapon(rig.rHand, "br55"); // BR55: long receiver, scope, mag, barrel
        rig.box(w, gun, -1.5f, -3, -10, 3, 4, 14); rig.box(w, metal, -0.5f, -2, -16, 1, 1, 6);
        rig.box(w, black, -1, -6, -7, 2, 3, 6); rig.box(w, red, -0.5f, -5.5f, -7.4f, 1, 1, 1); rig.box(w, black, -1, 1, -2, 2, 3, 2);
        w = rig.weapon(rig.rHand, "smg"); // M7: compact boxy body, long top mag, folded stock
        rig.box(w, gun, -1, -2.5f, -7, 2, 3, 8); rig.box(w, black, -0.5f, -4.5f, -5, 1, 2, 6);
        rig.box(w, metal, -0.5f, -2, -10, 1, 1, 3); rig.box(w, black, -0.5f, 0.5f, -5, 1, 2, 2);
        w = rig.weapon(rig.rHand, "shotgun"); // M45: receiver, tube, pump, stock
        rig.box(w, gun, -1.5f, -2.5f, -7, 3, 3, 9); rig.box(w, metal, -1, -3, -16, 2, 1, 9);
        rig.box(w, olive, -1.5f, -2, -13, 3, 2, 5); rig.box(w, black, -1, -1.5f, 2, 2, 2, 5);
        w = rig.weapon(rig.rHand, "sniper"); // SRS99: very long barrel, big scope, bipod-ish muzzle brake
        rig.box(w, gun, -1.5f, -3, -8, 3, 4, 12); rig.box(w, metal, -0.5f, -2, -24, 1, 1, 16);
        rig.box(w, black, -1, -2.5f, -25, 2, 2, 2); rig.box(w, black, -1, -6, -6, 2, 3, 8); rig.box(w, blue, -0.5f, -5.5f, -6.6f, 1, 1, 1);
        rig.box(w, black, -1, 1, -3, 2, 3, 2);
        w = rig.weapon(rig.rHand, "magnum"); // M6
        rig.box(w, gun, -1, -2.5f, -6, 2, 2, 7); rig.box(w, black, -1, -0.5f, -1, 2, 3, 2); rig.box(w, red, -0.5f, -3, -1, 1, 1, 1);
        w = rig.weapon(rig.rHand, "plasma_rifle"); // Type-25: twin prongs, bulbous body
        rig.box(w, cov, -2, -3, -6, 4, 4, 7); rig.box(w, cov, -2.5f, -3.5f, -11, 1, 2, 6); rig.box(w, cov, 1.5f, -3.5f, -11, 1, 2, 6);
        rig.box(w, blue, -1, -2.5f, -8, 2, 2, 2); rig.box(w, blue, -0.5f, -3.6f, -4, 1, 1, 3);
        w = rig.weapon(rig.rHand, "plasma_pistol"); // Type-25 pistol: curved green body
        rig.box(w, covG, -1.5f, -3, -6, 3, 3, 7); rig.box(w, covG, -1, -2.5f, -8, 2, 2, 2); rig.box(w, green, -0.5f, -2, -8.6f, 1, 1, 1);
        rig.box(w, black, -0.5f, 0, -1, 1, 2, 2);
        w = rig.weapon(rig.rHand, "spnkr"); // M41: twin tubes, sight, grip
        rig.box(w, olive, -2.5f, -4, -12, 2, 3, 18); rig.box(w, olive, 0.5f, -4, -12, 2, 3, 18);
        rig.box(w, black, -1, -6, -5, 2, 2, 4); rig.box(w, metal, -2.5f, -4.5f, -13, 5, 4, 1); rig.box(w, black, -0.5f, -1, -2, 1, 3, 2);
        w = rig.weapon(rig.rHand, "knife"); // M11 combat knife
        rig.box(w, grey, -0.5f, -1, -7, 1, 2, 6); rig.box(w, black, -0.5f, -0.5f, -1, 1, 1, 2);
        w = rig.weapon(rig.lHand, "frag"); // M9 frag
        rig.box(w, olive, -1, -1, -2, 2, 2, 2); rig.box(w, metal, -0.5f, -1.8f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "plasma_nade"); // Type-1 plasma grenade
        rig.box(w, blue, -1, -1, -2, 2, 2, 2); rig.box(w, cov, -1.5f, -0.5f, -1.5f, 3, 1, 1);
    }
}
