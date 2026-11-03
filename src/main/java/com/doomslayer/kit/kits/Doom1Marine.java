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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Doomguy, Knee-Deep in the Dead: green armor, brown hair, no helmet, the full 1993 arsenal. */
public class Doom1Marine extends SlayerKit {

    // ---- projectiles ----
    static final ProjStyle BFG_BALL = ProjStyle.make().color(0x50ff50).size(1.5f).life(90).explode(4.5f)
            .trail(ParticleTypes.HAPPY_VILLAGER).onHit(Doom1Marine::bfgBurst);

    public Doom1Marine() {
        super("doom1_marine", "Doomguy", "ドゥームガイ", "Doom (1993)");
        maxHealth = 100; armor = 8; speed = 0.32; knockbackRes = 0.4;
        preferredRange = 8f;
        eggPrimary = 0x3f8a2e; eggSecondary = 0x5a3a20;
        idleWeaponR = "pistol";
    }

    // =====================================================================================
    // attacks
    // =====================================================================================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // FIST: right jab / left cross combo
        out.add(AttackDef.of("fist").range(0, 2.8f).cd(14).dur(16).weight(3)
                .tick((e, t, n) -> {
                    if (n == 2 || n == 9) Combat.sound(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.4f, 1.7f);
                    if (n == 5 || n == 12) punch(e, t, 4f, 0.35, false);
                })
                .anim((p, t, a) -> {
                    float j1 = bump(win(t, .2f, .45f)), j2 = bump(win(t, .64f, .88f));
                    p.rArmX = track(t, 0, -1.0f, .14f, -0.55f, .31f, -1.68f, .42f, -1.5f, .55f, -1.0f, 1, -1.0f);
                    p.rArmY = track(t, 0, -0.35f, .14f, -0.2f, .31f, 0.18f, .5f, -0.3f, 1, -0.35f);
                    p.rArmZ = track(t, 0, 0.1f, .14f, 0.35f, .31f, 0, .5f, 0.1f, 1, 0.1f);
                    p.lArmX = track(t, 0, -1.1f, .5f, -1.1f, .6f, -0.55f, .75f, -1.68f, .86f, -1.5f, 1, -1.1f);
                    p.lArmY = track(t, 0, 0.35f, .6f, 0.2f, .75f, -0.18f, .9f, 0.3f, 1, 0.35f);
                    p.lArmZ = track(t, 0, -0.1f, .6f, -0.35f, .75f, 0, .9f, -0.1f, 1, -0.1f);
                    p.bodyY = track(t, 0, 0, .14f, 0.4f, .31f, -0.45f, .5f, -0.35f, .6f, -0.4f, .75f, 0.45f, .88f, 0.25f, 1, 0);
                    p.bodyX = 0.12f + 0.12f * (j1 + j2);
                    p.rootZ = -1.4f * (j1 + j2); p.rootY = -0.4f;
                    p.lLegX = -0.35f - 0.15f * j1; p.rLegX = 0.3f + 0.2f * j2;
                    p.rHandX = -0.25f * j1; p.lHandX = -0.25f * j2;
                    p.headX = 0.15f; p.headY = -p.bodyY * 0.8f;
                }));

        // BERSERK: pack-fuelled haymaker that sends things flying
        out.add(AttackDef.of("berserk_punch").range(0, 3f).cd(90).dur(20).weight(2).weapons("berserk", "berserk")
                .onStart((e, t, n) -> {
                    e.heal(e.getMaxHealth() * 0.25f);
                    ServerLevel l = lv(e);
                    Fx.dustBurst(l, 0xd01010, 1.6f, e.position().add(0, 1, 0), 30, 0.6);
                    Combat.sound(e, SoundEvents.PLAYER_LEVELUP, 0.6f, 0.6f);
                })
                .tick((e, t, n) -> {
                    if (n == 3 || n == 6) Fx.ring(lv(e), e.position().add(0, 0.1, 0), 0.8f + n * 0.1f, 0xd02020, 1.2f, 16);
                    if (n == 9) Combat.lunge(e, 0.55, 0.05);
                    if (n == 11) punch(e, t, 24f, 1.8, true);
                })
                .anim((p, t, a) -> {
                    float w = win(t, 0, .45f), s = win(t, .45f, .55f), r = win(t, .75f, 1);
                    float shake = (float) Math.sin(a * 3.1f) * 0.04f * w * (1 - s);
                    p.rArmX = track(t, 0, -0.8f, .45f, 0.7f, .55f, -1.75f, .75f, -1.65f, 1, -0.8f) + shake;
                    p.rArmY = track(t, 0, -0.2f, .45f, 0.3f, .55f, 0.2f, 1, -0.2f);
                    p.rArmZ = track(t, 0, 0.1f, .45f, 0.5f, .55f, 0, 1, 0.1f);
                    p.lArmX = track(t, 0, -1f, .45f, -1.4f, .55f, 0.5f, .75f, 0.3f, 1, -1f);
                    p.lArmY = track(t, 0, 0.3f, .45f, 0.5f, .55f, 0, 1, 0.3f);
                    p.lArmZ = track(t, 0, -0.1f, .45f, -0.2f, .55f, -0.5f, 1, -0.1f);
                    p.bodyY = track(t, 0, 0, .45f, 0.85f, .55f, -0.75f, .75f, -0.6f, 1, 0) + shake;
                    p.bodyX = track(t, 0, 0.1f, .45f, -0.15f, .55f, 0.4f, .75f, 0.35f, 1, 0.1f);
                    p.bodyZ = track(t, 0, 0, .45f, -0.12f, .55f, 0.1f, 1, 0);
                    p.rootY = track(t, 0, -0.3f, .45f, -1.6f, .52f, 0.6f, .62f, -0.5f, 1, -0.3f);
                    p.rootZ = trackLin(t, 0, 0, .45f, 1.0f, .55f, -3.5f, .8f, -3f, 1, 0);
                    p.lLegX = track(t, 0, -0.3f, .45f, -0.6f, .55f, -0.8f, .8f, -0.6f, 1, -0.3f);
                    p.rLegX = track(t, 0, 0.2f, .45f, 0.35f, .55f, 0.75f, .8f, 0.6f, 1, 0.2f);
                    p.rHandX = -0.3f * s * (1 - r);
                    p.headX = track(t, 0, 0.1f, .45f, 0.3f, .55f, 0.05f, 1, 0.1f); p.headY = -p.bodyY * 0.8f;
                }));

        // CHAINSAW: pull-start, rev, then saw through while being dragged into the target
        out.add(AttackDef.of("chainsaw").range(0, 3f).cd(50).dur(32).weight(2).weapon("chainsaw")
                .tick((e, t, n) -> {
                    if (n == 1) Combat.sound(e, SoundEvents.UI_STONECUTTER_TAKE_RESULT, 0.8f, 0.5f);
                    if (n == 3 || n == 5) Combat.sound(e, SoundEvents.UI_STONECUTTER_TAKE_RESULT, 0.9f, 0.6f + n * 0.05f);
                    if (n >= 6 && n <= 27) {
                        if (n % 3 == 0) Combat.sound(e, SoundEvents.UI_STONECUTTER_TAKE_RESULT, 1f, 0.9f + e.getRandom().nextFloat() * 0.3f);
                        if (n % 4 == 0) Combat.lunge(e, 0.22, 0);
                        if (n % 2 == 0 && near(e, t, 3.4)) {
                            Combat.damage(e, t, 2.5f);
                            t.setDeltaMovement(t.getDeltaMovement().multiply(0.2, 1, 0.2));
                            Vec3 c = mid(t); ServerLevel l = lv(e);
                            Fx.sparks(l, c, 3); Fx.dustBurst(l, 0x8a0a0a, 1.2f, c, 6, 0.25);
                        }
                    }
                })
                .anim((p, t, a) -> {
                    float rev = win(t, 0, .2f), cut = win(t, .2f, .85f), out2 = win(t, .85f, 1);
                    float on = smooth(rev) * (1 - smooth(out2));
                    float vib = (float) Math.sin(a * 4.7f) * 0.045f * (0.4f + on), vib2 = (float) Math.cos(a * 5.9f) * 0.035f * (0.4f + on);
                    float saw = (float) Math.sin(a * 0.8f) * 0.18f * smooth(cut) * (1 - out2);
                    float cord = bump(win(t, .03f, .15f));
                    p.rArmX = lerp(-0.5f, -1.05f, on) + vib + saw;
                    p.rArmY = lerp(-0.1f, 0.25f, on); p.rArmZ = 0.1f + vib2;
                    p.lArmX = lerp(-0.4f, -1.2f, on) + vib * 0.8f + saw + 0.6f * cord;
                    p.lArmY = lerp(0.2f, 0.6f, on); p.lArmZ = -0.05f - vib2 - 0.4f * cord;
                    p.bodyX = lerp(0.05f, 0.35f, on) + saw * 0.3f;
                    p.bodyY = (float) Math.sin(a * 0.4f) * 0.15f * smooth(cut) + 0.15f * cord;
                    p.bodyOffX = vib * 3f * on;
                    p.rootY = -0.7f * on; p.rootZ = -1.5f * smooth(cut) * (1 - out2);
                    p.lLegX = -0.4f * on; p.rLegX = 0.35f * on; p.rLegZ = 0.08f * on; p.lLegZ = -0.08f * on;
                    level(p); p.rHandX += 0.15f; p.rHandY = vib2 * 2f;
                    p.headX = 0.2f * on; p.headY = -p.bodyY * 0.7f;
                    p.weaponR = ((int) (a * (t < .2f ? 0.5f : 1f)) & 1) == 0 ? "chainsaw" : "chainsaw_b";
                }));

        // PISTOL: one-handed, wrist-flip recoil
        out.add(AttackDef.of("pistol").range(2, 22).cd(12).dur(14).weight(2).weapon("pistol")
                .tick((e, t, n) -> {
                    if (n == 4) { bullet(e, t, 0.02, 24, 5f, 0xffe28a); flash(e, 2); Combat.gunSound(e, 1.7f); }
                })
                .anim((p, t, a) -> {
                    float up = smooth(win(t, 0, .25f)) * (1 - smooth(win(t, .8f, 1))), k = kick(t, .29f, .35f);
                    p.rArmX = lerp(-0.5f, -HALF - 0.05f, up) - k * 0.35f;
                    p.rArmY = 0.05f * up; p.rArmZ = 0.05f;
                    p.bodyY = -0.3f * up + k * 0.08f; p.bodyX = 0.04f - k * 0.1f;
                    p.lArmX = 0.15f; p.lArmY = 0; p.lArmZ = -0.05f - 0.2f * up;
                    p.lLegX = -0.25f * up; p.rLegX = 0.2f * up;
                    p.rootZ = k * 0.6f;
                    level(p); p.rHandX -= k * 0.45f;
                    p.headX = 0.05f * up - k * 0.06f; p.headY = -p.bodyY * 0.9f;
                    p.weaponR = t > .28f && t < .38f ? "pistol_f" : "pistol";
                }));

        // SHOTGUN: blast, then pump
        out.add(AttackDef.of("shotgun").range(1.5f, 14).cd(28).dur(24).weight(3).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 3) {
                        for (int i = 0; i < 7; i++) bullet(e, t, 0.075, 16, 3.2f, 0xffd070);
                        flash(e, 6); Combat.gunSound(e, 0.8f); Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.9f);
                    }
                    if (n == 10) Combat.sound(e, SoundEvents.PISTON_CONTRACT, 0.8f, 1.6f);
                    if (n == 12) Fx.dustBurst(lv(e), 0xb02020, 0.8f, Combat.muzzle(e, 0.3, 0.45, 0.1), 2, 0.05);
                    if (n == 14) Combat.sound(e, SoundEvents.PISTON_EXTEND, 0.8f, 1.7f);
                })
                .anim((p, t, a) -> {
                    float aim = smooth(win(t, 0, .1f)) * (1 - smooth(win(t, .82f, 1)));
                    float k = kick(t, .125f, .3f), pump = win(t, .38f, .66f), pb = bump(pump);
                    aim(p, -0.03f, k * 1.5f, aim);
                    p.lArmX += 0.45f * pb; p.lArmY -= 0.15f * pb;
                    p.bodyX = 0.12f * aim - 0.25f * k; p.bodyY = -0.18f * aim + 0.1f * pb; p.bodyZ = 0.06f * pb;
                    p.rootZ = 1.4f * k; p.rootY = -0.5f * aim;
                    p.lLegX = -0.4f * aim; p.rLegX = 0.3f * aim + 0.15f * k;
                    level(p); p.rHandX -= 0.35f * k; p.rHandY = 0.35f * pb;
                    p.headX = 0.1f * aim - 0.15f * k; p.headY = -p.bodyY * 0.8f;
                    p.weaponR = t > .12f && t < .2f ? "shotgun_f" : pump > .2f && pump < .8f ? "shotgun_p" : "shotgun";
                }));

        // CHAINGUN: spin-up, 12-round burst with alternating recoil, spin-down
        out.add(AttackDef.of("chaingun").range(3, 20).cd(50).dur(34).weight(2).weapon("chaingun")
                .tick((e, t, n) -> {
                    if (n == 0 || n == 3 || n == 6) Combat.sound(e, SoundEvents.PISTON_EXTEND, 0.6f, 0.6f + n * 0.08f);
                    if (n >= 9 && n <= 31 && (n - 9) % 2 == 0) {
                        bullet(e, t, 0.035, 20, 3f, 0xffe080); flash(e, 2);
                        Combat.gunSound(e, 1.3f + e.getRandom().nextFloat() * 0.1f);
                    }
                    if (n == 32) Combat.sound(e, SoundEvents.PISTON_CONTRACT, 0.6f, 0.7f);
                })
                .anim((p, t, a) -> {
                    float f = t * 34f, aim = smooth(win(t, 0, .15f)) * (1 - smooth(win(t, .9f, 1)));
                    boolean firing = f >= 9 && f < 32.5f;
                    int n = (int) ((f - 9) / 2); float sub = (f - 9) / 2 - n;
                    float r = firing ? (1 - sub) * (1 - sub) : 0, side = (n & 1) == 0 ? 1 : -1;
                    aim(p, -0.08f, r * 0.5f, aim);
                    float rumble = firing ? (float) Math.sin(a * 5f) * 0.02f : 0;
                    p.rArmX += rumble; p.lArmX += rumble;
                    p.rArmZ += side * 0.05f * r; p.lArmZ += side * 0.05f * r;
                    p.bodyY = side * 0.07f * r - 0.1f * aim; p.bodyX = 0.18f * aim - 0.08f * r;
                    p.rootX = side * 0.25f * r; p.rootY = -0.6f * aim; p.rootZ = 0.4f * r + (firing ? 0.6f * win(f, 9, 32) : 0);
                    p.lLegX = -0.35f * aim; p.rLegX = 0.35f * aim; p.lLegZ = -0.06f * aim; p.rLegZ = 0.06f * aim;
                    level(p); p.rHandX -= 0.12f * r; p.rHandZ = side * 0.06f * r;
                    p.headX = 0.08f * aim; p.headY = -p.bodyY * 0.8f;
                    float spin = f < 9 ? f * f / 12f : f < 32 ? 6.75f + (f - 9) : 29.75f + (f - 32) * 0.5f;
                    p.weaponR = firing ? (sub < .5f ? "chaingun_f" : "chaingun") : (((int) spin & 1) == 1 ? "chaingun_s" : "chaingun");
                }));

        // ROCKET LAUNCHER: brace, fire, get shoved back by the kick
        out.add(AttackDef.of("rocket_launcher").range(5, 30).cd(70).dur(28).weight(2).weapon("rocket")
                .tick((e, t, n) -> {
                    if (n == 4) Combat.sound(e, SoundEvents.ARMOR_EQUIP_IRON, 1f, 0.8f);
                    if (n == 10) {
                        Vec3 m = Combat.muzzle(e, 1.2, 0.3, 0.1);
                        Combat.shoot(e, ProjStyle.ROCKET, m, aimDir(e, t, m, 0.01), 1.25, 20f);
                        Combat.sound(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5f, 0.5f);
                        Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.6f);
                        Vec3 b = Combat.muzzle(e, -0.9, 0.3, 0.1); ServerLevel l = lv(e);
                        Fx.burst(l, ParticleTypes.LARGE_SMOKE, b, 10, 0.2, 0.05); Fx.burst(l, ParticleTypes.FLAME, b, 6, 0.15, 0.04);
                        flash(e, 4);
                    }
                })
                .anim((p, t, a) -> {
                    float aim = smooth(win(t, 0, .3f)) * (1 - smooth(win(t, .8f, 1))), k = kick(t, .357f, .5f);
                    aim(p, 0.04f, k * 1.3f, aim);
                    p.rArmX += 0.25f * bump(win(t, 0, .3f)); // heft swing-up
                    p.bodyX = 0.1f * aim - 0.35f * k; p.bodyY = -0.12f * aim;
                    p.rootY = -0.9f * aim + 0.3f * k; p.rootZ = 2.6f * k; p.rootPitch = -0.08f * k;
                    p.lLegX = -0.45f * aim - 0.1f * k; p.rLegX = 0.35f * aim + 0.25f * k;
                    level(p); p.rHandX -= 0.3f * k;
                    p.headX = 0.12f * aim - 0.2f * k; p.headY = -p.bodyY * 0.8f; p.headZ = 0.1f * aim;
                    p.weaponR = t > .35f && t < .46f ? "rocket_f" : "rocket";
                }));

        // PLASMA RIFLE: humming stream of 9 bolts, then vent
        out.add(AttackDef.of("plasma_rifle").range(3, 24).cd(60).dur(26).weight(2).weapon("plasma")
                .tick((e, t, n) -> {
                    if (n == 1) Combat.sound(e, SoundEvents.BEACON_AMBIENT, 0.6f, 2f);
                    if (n >= 4 && n <= 20 && (n - 4) % 2 == 0) {
                        Vec3 m = Combat.muzzle(e, 1.1, 0.3, 0.05);
                        Combat.shoot(e, ProjStyle.PLASMA, m, aimDir(e, t, m, 0.03), 1.7, 5f);
                        Combat.sound(e, SoundEvents.SHULKER_SHOOT, 0.7f, 1.9f);
                        Fx.burst(lv(e), ParticleTypes.ELECTRIC_SPARK, m, 3, 0.05, 0.05);
                    }
                    if (n == 22) {
                        Fx.burst(lv(e), ParticleTypes.CLOUD, Combat.muzzle(e, 0.6, 0.3, 0.2), 6, 0.1, 0.02);
                        Combat.sound(e, SoundEvents.FIRE_EXTINGUISH, 0.6f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float f = t * 26f, aim = smooth(win(t, 0, .14f)) * (1 - smooth(win(t, .85f, 1)));
                    boolean firing = f >= 4 && f < 21;
                    int n = (int) ((f - 4) / 2); float sub = (f - 4) / 2 - n, r = firing ? (1 - sub) * (1 - sub) : 0;
                    aim(p, -0.02f, 0.3f * r, aim);
                    float hum = (float) Math.sin(a * 6.3f) * 0.015f * aim, vent = bump(win(t, .75f, .98f));
                    p.rArmX += hum - 0.2f * vent; p.lArmX += hum + 0.3f * vent;
                    p.bodyX = 0.1f * aim; p.bodyY = -0.1f * aim + ((n & 1) == 0 ? 0.03f : -0.03f) * r;
                    p.rootY = -0.4f * aim; p.rootZ = 0.3f * r;
                    p.lLegX = -0.3f * aim; p.rLegX = 0.25f * aim;
                    level(p); p.rHandX -= 0.1f * r + 0.6f * vent;
                    p.headX = 0.08f * aim - 0.1f * vent; p.headY = -p.bodyY * 0.8f;
                    p.weaponR = firing && sub < .5f ? "plasma_f" : "plasma";
                }));

        // BFG 9000: long charge, green ball, tracer spray from the marine on detonation
        out.add(AttackDef.of("bfg9000").range(6, 30).cd(240).dur(52).weight(1).weapon("bfg")
                .when((e, t) -> t.getMaxHealth() >= 40 || Combat.around(e, t.position(), 6).size() >= 3 || e.getRandom().nextInt(5) == 0)
                .tick((e, t, n) -> {
                    ServerLevel l = lv(e);
                    Vec3 m = Combat.muzzle(e, 1.3, 0.3, 0.1);
                    if (n == 0) Combat.sound(e, SoundEvents.BEACON_ACTIVATE, 1.2f, 1.4f);
                    if (n == 16) Combat.sound(e, SoundEvents.BEACON_POWER_SELECT, 1.2f, 1.8f);
                    if (n < 32 && n % 2 == 0) {
                        float c = n / 32f;
                        Fx.ring(l, m, 1.6f * (1 - c) + 0.25f, 0x60ff60, 1f, 12);
                        Fx.burst(l, ParticleTypes.HAPPY_VILLAGER, m, 2, 0.2, 0);
                    }
                    if (n == 32) {
                        Combat.shoot(e, BFG_BALL, m, aimDir(e, t, m, 0), 0.85, 35f);
                        Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.5f);
                        Combat.sound(e, SoundEvents.BEACON_DEACTIVATE, 1.5f, 1.6f);
                        Fx.dustBurst(l, 0x50ff50, 2f, m, 30, 0.4);
                    }
                })
                .anim((p, t, a) -> {
                    float c = win(t, 0, .6f), aim = smooth(win(t, 0, .2f)) * (1 - smooth(win(t, .85f, 1)));
                    float k = kick(t, .615f, .35f), stag = bump(win(t, .66f, .85f));
                    float jit = t < .615f ? (float) Math.sin(a * 7.3f) * 0.05f * c * c : 0;
                    aim(p, 0.02f + jit, k * 1.8f, aim);
                    p.lArmX += jit * 0.8f;
                    p.bodyX = 0.22f * aim * c - 0.45f * k; p.bodyY = -0.1f * aim + jit;
                    p.rootY = -1.3f * smooth(c) * aim + 0.6f * k; p.rootZ = 3.5f * k + 1.5f * stag; p.rootPitch = -0.12f * k;
                    p.lLegX = -0.5f * aim; p.rLegX = 0.4f * aim + 0.5f * k + 0.3f * stag; p.lLegZ = -0.1f * aim; p.rLegZ = 0.1f * aim;
                    level(p); p.rHandX -= 0.4f * k;
                    p.headX = 0.15f * aim * c - 0.25f * k; p.headY = -p.bodyY * 0.8f;
                    boolean glow = t < .615f ? t > .05f && (c > .75f || Math.sin(a * (0.8f + c * 3f)) > 0.3f * (1 - c)) : t < .64f;
                    p.weaponR = glow ? "bfg_c" : "bfg";
                }));
    }

    // ---- server helpers ----
    private static ServerLevel lv(SlayerEntity e) { return (ServerLevel) e.level(); }
    private static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    private static boolean near(SlayerEntity e, LivingEntity t, double r) { return t != null && t.isAlive() && e.distanceTo(t) <= r; }
    private static Vec3 aimDir(SlayerEntity e, LivingEntity t, Vec3 from, double spread) {
        return t != null && t.isAlive() ? Combat.dirTo(from, t, spread, new Random(e.getRandom().nextLong())) : e.getViewVector(1f);
    }
    private static void bullet(SlayerEntity e, LivingEntity t, double spread, double range, float dmg, int rgb) {
        Vec3 m = Combat.muzzle(e, 1.0, 0.3, 0.05);
        Combat.hitscan(e, m, aimDir(e, t, m, spread), range, dmg, 0, rgb);
    }
    private static void flash(SlayerEntity e, int n) {
        Vec3 m = Combat.muzzle(e, 1.1, 0.3, 0.05); ServerLevel l = lv(e);
        Fx.burst(l, ParticleTypes.FLAME, m, n, 0.05, 0.02); Fx.burst(l, ParticleTypes.SMOKE, m, n, 0.06, 0.02);
    }
    private static void punch(SlayerEntity e, LivingEntity t, float dmg, double kb, boolean heavy) {
        if (!near(e, t, heavy ? 3.6 : 3.2)) return;
        Combat.damage(e, t, dmg);
        Combat.knock(t, e.position(), kb, heavy ? 0.5 : 0.12);
        Vec3 c = mid(t); ServerLevel l = lv(e);
        Fx.sparks(l, c, heavy ? 12 : 4);
        Fx.dustBurst(l, 0x8a0000, heavy ? 1.6f : 1f, c, heavy ? 20 : 4, heavy ? 0.4 : 0.2);
        SoundEvent s = heavy ? SoundEvents.PLAYER_ATTACK_KNOCKBACK : SoundEvents.PLAYER_ATTACK_STRONG;
        Combat.sound(e, s, 1f, heavy ? 0.7f : 1.1f);
        if (heavy) {
            Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.7f);
            Fx.burst(l, ParticleTypes.SWEEP_ATTACK, c, 1, 0, 0);
        }
    }
    /** BFG ball detonation: green blast + tracer rays from the marine to every foe in a 90 degree cone he can see. */
    private static void bfgBurst(SlayerProjectile p, Vec3 pos, Entity hit) {
        ServerLevel l = (ServerLevel) p.level();
        Fx.burst(l, ParticleTypes.EXPLOSION, pos, 3, 0.8, 0);
        Fx.dustBurst(l, 0x50ff50, 2.5f, pos, 80, 1.6);
        Fx.burst(l, ParticleTypes.HAPPY_VILLAGER, pos, 40, 2, 0.1);
        for (int i = 0; i < 3; i++) Fx.ring(l, pos, 1.5f + i * 1.5f, 0x80ff80, 1.5f, 24 + i * 8);
        Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 2f, 0.6f);
        Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 2f, 0.5f);
        if (!(p.getOwner() instanceof SlayerEntity o)) return;
        Vec3 eye = o.getEyePosition();
        for (LivingEntity v : Combat.cone(o, 32, 45)) {
            if (!o.hasLineOfSight(v)) continue;
            Vec3 c = v.position().add(0, v.getBbHeight() * 0.5, 0);
            Combat.damage(o, v, 18 + o.getRandom().nextInt(13));
            Fx.beam(l, eye, c, 0x60ff60, 1.2f, 0.5);
            Fx.dustBurst(l, 0x50ff50, 1.8f, c, 20, 0.5);
            Fx.burst(l, ParticleTypes.HAPPY_VILLAGER, c, 8, 0.4, 0);
        }
    }

    // ---- anim helpers ----
    /** keep the right-hand weapon level with the horizon regardless of arm/torso pitch */
    private static void level(Pose p) { p.rHandX = -(HALF + p.rArmX + p.bodyX); }
    /** two-handed aim blended from a low ready (w=0) to full aim (w=1) */
    private static void aim(Pose p, float pitch, float recoil, float w) {
        p.aim2(pitch, recoil);
        p.rArmX = lerp(-0.45f, p.rArmX, w); p.lArmX = lerp(-0.25f, p.lArmX, w);
        p.rArmY *= w; p.lArmY *= w;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float limbAmt) {
        float still = 1 - limbAmt;
        p.rArmX += -0.45f * still; p.rArmY -= 0.1f * still;
        p.rHandX = -(HALF + p.rArmX) * 0.6f;
        p.bodyX += (float) Math.sin(age * 0.08f) * 0.01f;
    }

    // =====================================================================================
    // model
    // =====================================================================================
    static final int SKIN = 0xd29a72, HAIR = 0x5a3a20, HAIR_D = 0x402814;

    @Override
    public void buildModel(Rig r) {
        Mat suit = Mat.of(0x4a4630).noise(.1f);
        Mat armor = Mat.of(0x3f8a2e).noise(.12f).bevel(.28f);
        Mat armorD = Mat.of(0x2c6420).noise(.1f).bevel(.25f);
        Mat chest = Mat.of(0x3f8a2e).noise(.12f).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            for (int j = 1; j < h - 1; j++) g.px(x + w / 2, y + j, 0x2a5c1e);               // centre ridge
            for (int i = 1; i < w - 1; i++) g.px(x + i, y + h - 3, 0x2a5c1e);               // lower seam
        });
        Mat glove = Mat.of(0x5a3e26).noise(.1f);
        Mat boot = Mat.of(0x3a2a1c).noise(.08f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.TOP || f == Mat.Face.BOTTOM) return;
            for (int i = 0; i < w; i++) g.px(x + i, y + h - 1, 0x141010);                    // sole
        });
        Mat belt = Mat.of(0x5a3a1e).noise(.1f), beltD = Mat.of(0x47301a).noise(.1f);
        Mat gold = Mat.of(0xc8a040).noise(.05f).bevel(.3f);
        Mat skinD = Mat.of(0xb88260).noise(.05f);
        Mat hair = Mat.of(HAIR).noise(.2f).bevel(.1f);
        Mat head = Mat.of(SKIN).noise(.05f).bevel(.08f).decal(Doom1Marine::face);

        // torso
        r.box(r.torso, suit, -4, -12, -2, 8, 12, 4);
        r.box(r.torso, chest, -4, -12, -2, 8, 7, 4, 0.45f);                 // chest armor wrap
        r.box(r.torso, armorD, -3, -5, -2.6f, 6, 3, 1);                     // abdomen plate
        r.box(r.torso, armorD, -3.5f, -11, 2.3f, 7, 6, 1);                  // back plate
        r.box(r.torso, armorD, -2.5f, -12.7f, -2, 5, 1, 4);                 // collar
        r.box(r.torso, belt, -4, -2.5f, -2, 8, 2, 4, 0.55f);
        r.box(r.torso, gold, -1, -2.6f, -3.1f, 2, 2, 1);                    // buckle
        r.box(r.torso, beltD, -5.2f, -2.5f, -1, 1, 2, 2);                   // side pouches
        r.box(r.torso, beltD, 4.2f, -2.5f, -1, 1, 2, 2);
        r.box(r.torso, beltD, -1.5f, -2.5f, 2.4f, 3, 2, 1);                 // back pouch
        // head
        r.box(r.head, head, -4, -8, -4, 8, 8, 8);
        r.box(r.head, hair, -4, -8.6f, -4, 8, 2, 8, 0.3f);                  // hair volume / fringe
        r.box(r.head, skinD, -4.6f, -5, -1, 1, 2, 1);                       // ears
        r.box(r.head, skinD, 3.6f, -5, -1, 1, 2, 1);
        // arms
        for (int s = 0; s < 2; s++) {
            PartDefinition arm = s == 0 ? r.rArm : r.lArm;
            r.box(arm, suit, -2, -2, -2, 4, 12, 4);
            r.box(arm, armor, s == 0 ? -3 : -2, -3, -3, 5, 4, 6);           // shoulder pad
            r.box(arm, armor, -2, 4, -2, 4, 3, 4, 0.35f);                   // forearm bracer
            r.box(arm, glove, -2, 7, -2, 4, 3, 4, 0.25f);
        }
        // legs
        for (int s = 0; s < 2; s++) {
            PartDefinition leg = s == 0 ? r.rLeg : r.lLeg;
            r.box(leg, suit, -2, 0, -2, 4, 12, 4);
            r.box(leg, armor, -2, 0.5f, -2, 4, 5, 4, 0.35f);                // thigh plate
            r.box(leg, armorD, -1.5f, 6, -2.7f, 3, 2, 1);                   // knee pad
            r.box(leg, boot, -2, 8, -2, 4, 4, 4, 0.4f);
            r.box(leg, boot, -2, 10, -3.4f, 4, 2, 1);                       // toe cap
        }

        // ---- weapons (gun space: origin = fist, barrel -z, y down) ----
        Mat gun = Mat.of(0x3a3c40).noise(.08f), dark = Mat.of(0x222326).noise(.06f), steel = Mat.of(0x8a8e94).noise(.06f).bevel(.2f);
        Mat wood = Mat.of(0x6a4424).noise(.18f), flashM = Mat.of(0xffd860).glow();

        // pistol
        for (int v = 0; v < 2; v++) {
            PartDefinition w = r.weapon(r.rHand, v == 0 ? "pistol" : "pistol_f");
            r.box(w, gun, -1, -4.5f, -5, 2, 2, 7);          // slide
            r.box(w, dark, -1, -2.6f, -3.5f, 2, 1, 5);      // frame
            r.box(w, dark, -0.5f, -2, -2.5f, 1, 2, 1);      // trigger guard
            r.box(w, steel, -0.5f, -4.2f, -6, 1, 1, 1);     // muzzle
            r.box(w, dark, -0.5f, -5.2f, -4.5f, 1, 1, 1);   // front sight
            r.box(w, dark, -0.5f, -5.2f, 1, 1, 1, 1);       // hammer
            if (v == 1) r.box(w, flashM, -1.5f, -5, -9, 3, 3, 3);
        }
        // shotgun (rest / fired / pumped)
        for (int v = 0; v < 3; v++) {
            PartDefinition w = r.weapon(r.rHand, v == 0 ? "shotgun" : v == 1 ? "shotgun_f" : "shotgun_p");
            r.box(w, gun, -1, -5, -2, 2, 3, 5);             // receiver
            r.box(w, dark, -0.5f, -5, -15, 1, 1, 13);       // barrel
            r.box(w, dark, -0.5f, -3.8f, -12, 1, 1, 10);    // magazine tube
            r.box(w, wood, -1, -4.4f, v == 2 ? -8 : -11, 2, 2, 4); // pump
            r.box(w, wood, -1, -4.5f, 3, 2, 3, 7);          // stock
            r.box(w, dark, -1, -5, 10, 2, 4, 1);            // butt pad
            r.box(w, steel, -0.5f, -5.8f, -14, 1, 1, 1);    // bead sight
            if (v == 1) r.box(w, flashM, -1.5f, -6, -19, 3, 3, 4);
        }
        // chaingun (two barrel phases for spin, one with flash)
        for (int v = 0; v < 3; v++) {
            PartDefinition w = r.weapon(r.rHand, v == 0 ? "chaingun" : v == 1 ? "chaingun_s" : "chaingun_f");
            r.box(w, gun, -2, -6.5f, -5, 4, 4, 7);          // housing
            r.box(w, dark, -0.5f, -8.5f, -4, 1, 1, 5);      // carry handle
            r.box(w, dark, -0.5f, -7.5f, -4, 1, 1, 1);
            r.box(w, dark, -0.5f, -7.5f, 0, 1, 1, 1);
            r.box(w, dark, -3, -6, -3, 1, 2, 3);            // side motor
            PartDefinition bar = r.pivot(w, "barrels", 0, -4.5f, -5, 0, 0, v == 0 ? 0 : PI / 6);
            for (int k = 0; k < 6; k++) {
                double ang = k * PI / 3;
                r.box(bar, steel, (float) Math.cos(ang) * 1.3f - 0.5f, (float) Math.sin(ang) * 1.3f - 0.5f, -10, 1, 1, 10);
            }
            r.box(bar, dark, -2, -2, -3, 4, 4, 1);          // rear collar
            r.box(bar, dark, -2, -2, -9, 4, 4, 1);          // front collar
            if (v == 2) r.box(bar, flashM, -2, -2, -14, 4, 4, 3);
        }
        // rocket launcher
        Mat olive = Mat.of(0x4d5a3a).noise(.1f).bevel(.22f), red = Mat.of(0xa02018).noise(.05f);
        Mat fire = Mat.of(0xff8a20).glow();
        for (int v = 0; v < 2; v++) {
            PartDefinition w = r.weapon(r.rHand, v == 0 ? "rocket" : "rocket_f");
            r.box(w, olive, -2, -8, -12, 4, 4, 16);         // tube
            r.box(w, dark, -2, -8, -13, 4, 4, 1, 0.35f);    // front ring
            r.box(w, dark, -2, -8, 4, 4, 4, 1, 0.35f);      // rear ring
            r.box(w, red, -1, -7, -13.3f, 2, 2, 1);         // warhead
            r.box(w, dark, -0.5f, -10, -5, 1, 2, 2);        // sight
            r.box(w, dark, -0.5f, -4, -1, 1, 2, 2);         // grip
            r.box(w, dark, -0.5f, -4, -8, 1, 3, 2);         // fore grip
            if (v == 1) { r.box(w, fire, -1.5f, -7.5f, 5, 3, 3, 3); r.box(w, flashM, -1.5f, -7.5f, -17, 3, 3, 3); }
        }
        // plasma rifle
        Mat pBody = Mat.of(0x4a5560).noise(.08f).bevel(.22f), blue = Mat.of(0x50a0ff).glow(), blueL = Mat.of(0xb0e4ff).glow();
        for (int v = 0; v < 2; v++) {
            PartDefinition w = r.weapon(r.rHand, v == 0 ? "plasma" : "plasma_f");
            r.box(w, pBody, -1.5f, -6, -9, 3, 3, 12);       // body
            for (int k = 0; k < 3; k++) r.box(w, blue, -2, -6.5f, -8 + k * 2, 4, 4, 1); // coils
            r.box(w, dark, -1, -5.5f, -12, 2, 2, 3);        // emitter
            r.box(w, dark, -0.5f, -7.5f, -7, 1, 1, 8);      // top rail
            r.box(w, blue, 1.5f, -5, -2, 1, 2, 3);          // energy cell
            r.box(w, pBody, -1, -5.5f, 3, 2, 3, 4);         // stock
            if (v == 1) r.box(w, blueL, -1.5f, -6, -15, 3, 3, 3);
        }
        // BFG 9000
        Mat bfgM = Mat.of(0x4a5248).noise(.1f).bevel(.25f), green = Mat.of(0x40ff50).glow(), greenL = Mat.of(0xa0ffa0).glow();
        for (int v = 0; v < 2; v++) {
            PartDefinition w = r.weapon(r.rHand, v == 0 ? "bfg" : "bfg_c");
            r.box(w, bfgM, -3, -9, -10, 6, 5, 14);          // body
            r.box(w, dark, -2.5f, -8.5f, -12, 5, 4, 2);     // emitter
            r.box(w, green, -1.5f, -7.5f, -12.5f, 3, 2, 1); // core
            r.box(w, green, -3.6f, -8, -7, 1, 3, 7);        // side vents
            r.box(w, green, 2.6f, -8, -7, 1, 3, 7);
            r.box(w, dark, -1, -10.5f, -6, 2, 1, 7);        // top handle
            r.box(w, dark, -3, -9, 4, 6, 5, 2);             // rear block
            r.box(w, dark, -1, -4, -7, 2, 3, 2);            // fore grip
            if (v == 1) r.box(w, greenL, -2, -8.5f, -17, 4, 4, 4);
        }
        // chainsaw (two chain phases)
        Mat sawRed = Mat.of(0xb8461e).noise(.1f).bevel(.25f);
        for (int v = 0; v < 2; v++) {
            final int ph = v;
            Mat chain = Mat.of(0x9a9ea4).noise(0).bevel(0).decal((m, g, x, y, w, h, f) -> {
                for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) if (((i + j + ph) & 1) == 0) g.px(x + i, y + j, 0x2a2a2a);
            });
            PartDefinition w = r.weapon(r.rHand, v == 0 ? "chainsaw" : "chainsaw_b");
            r.box(w, sawRed, -2, -6, -3, 4, 4, 6);          // motor housing
            r.box(w, dark, -0.5f, -8, -3, 1, 1, 5);         // top handle
            r.box(w, dark, -0.5f, -7, -3, 1, 1, 1);
            r.box(w, dark, -0.5f, -7, 1, 1, 1, 1);
            r.box(w, steel, -0.5f, -5, -16, 1, 2, 13);      // guide bar
            r.box(w, chain, -0.5f, -6, -16, 1, 1, 13);      // chain top
            r.box(w, chain, -0.5f, -3, -16, 1, 1, 13);      // chain bottom
            r.box(w, chain, -0.5f, -5.5f, -17, 1, 3, 1);    // tip
            r.box(w, dark, -2.5f, -5, 1, 1, 1, 2);          // pull-cord grip
            r.box(w, steel, 2, -5, -1, 1, 1, 2);            // exhaust
        }
        // berserk fists
        Mat rage = Mat.of(0xd02020).glow();
        r.box(r.weapon(r.rHand, "berserk"), rage, -2, -2, -0.5f, 4, 4, 4, 0.45f);
        r.box(r.weapon(r.lHand, "berserk"), rage, -2, -2, -0.5f, 4, 4, 4, 0.45f);
    }

    /** head texture: hair on top/back/sides, bare face with brows, blue-grey eyes, nose shadow, stern mouth. */
    private static void face(Mat m, Mat.Img g, int x, int y, int w, int h, Mat.Face f) {
        switch (f) {
            case TOP -> { for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) g.px(x + i, y + j, (i * 7 + j * 13) % 5 == 0 ? HAIR_D : HAIR); }
            case BACK -> { for (int j = 0; j < 6; j++) for (int i = 0; i < w; i++) g.px(x + i, y + j, (i + j) % 4 == 0 ? HAIR_D : HAIR); }
            case LEFT, RIGHT -> { for (int j = 0; j < 3; j++) for (int i = 0; i < w; i++) g.px(x + i, y + j, HAIR); }
            case FRONT -> {
                for (int i = 0; i < w; i++) g.px(x + i, y, HAIR);
                g.px(x, y + 1, HAIR); g.px(x + w - 1, y + 1, HAIR); g.px(x + 3, y + 1, HAIR);
                int brow = 0x3a2412, white = 0xe8e4dc, iris = 0x4a6a8a, shade = Mat.mix(SKIN, 0x000000, 0.18f);
                g.px(x + 1, y + 2, brow); g.px(x + 2, y + 2, brow); g.px(x + 5, y + 2, brow); g.px(x + 6, y + 2, brow);
                g.px(x + 1, y + 3, white); g.px(x + 2, y + 3, iris); g.px(x + 5, y + 3, iris); g.px(x + 6, y + 3, white);
                g.px(x + 3, y + 4, shade); g.px(x + 4, y + 4, shade);
                g.px(x + 3, y + 5, Mat.mix(SKIN, 0, 0.3f)); g.px(x + 4, y + 5, Mat.mix(SKIN, 0, 0.3f));
                for (int i = 2; i < 6; i++) g.px(x + i, y + 6, 0x8a4a3a);
                for (int i = 1; i < 7; i++) g.px(x + i, y + 7, Mat.mix(SKIN, HAIR, 0.2f)); // stubble jaw
            }
            default -> {}
        }
    }
}
