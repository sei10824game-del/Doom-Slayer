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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * The Marine of DOOM II: Hell on Earth. Back on Earth, battle-worn, meaner: weathered olive armour with scratches and
 * blood, red-orange visor, heavier pads, wide aggressive stance. Signature weapon (idle) is the Super Shotgun.
 */
public class Doom2Marine extends SlayerKit {
    private static final Random R = new Random();

    // ---------------- projectiles ----------------
    static final ProjStyle D2_ROCKET = ProjStyle.make().color(0xffa040).size(0.42f).life(100).explode(3.8f)
            .trail(ParticleTypes.LARGE_SMOKE)
            .onTick(p -> Fx.burst((ServerLevel) p.level(), ParticleTypes.FLAME, p.position(), 2, 0.05, 0.01));
    static final ProjStyle D2_PLASMA = ProjStyle.make().color(0x3a8cff).size(0.34f).life(50).trail(ParticleTypes.ELECTRIC_SPARK)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, 0x60b0ff, 1.2f, pos, 10, 0.2);
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 8, 0.2, 0.2);
            });
    static final ProjStyle D2_BFG = ProjStyle.make().color(0x50ff40).size(1.4f).life(80).explode(5.5f).trail(ParticleTypes.HAPPY_VILLAGER)
            .onTick(p -> Fx.dustBurst((ServerLevel) p.level(), 0x60ff50, 2f, p.position(), 3, 0.5))
            .onHit(Doom2Marine::bfgBurst);

    public Doom2Marine() {
        super("doom2_marine", "Doom Marine (Doom II)", "ドゥームマリーン（DOOM II）", "Doom II: Hell on Earth");
        eggPrimary = 0x3a5228; eggSecondary = 0x8a1a10;
        maxHealth = 72; armor = 12; speed = 0.31; knockbackRes = 0.45; dmgMult = 1.1f; preferredRange = 7f;
        idleWeaponR = "ssg";
    }

    // =====================================================================================
    // attacks
    // =====================================================================================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---- Fist: guard up, twist, right cross ----
        out.add(AttackDef.of("fist").range(0, 2.8f).dur(12).cd(14).weight(1)
                .tick((e, tg, n) -> {
                    if (n != 5) return;
                    for (LivingEntity f : Combat.cone(e, 2.8, 60)) { Combat.damage(e, f, 5f); Combat.knock(f, e.position(), 0.5, 0.15); }
                    Combat.sound(e, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.8f);
                    Fx.sparks(lvl(e), Combat.muzzle(e, 1.2, 0.2, 0), 4);
                })
                .anim((p, t, age) -> {
                    float wind = track(t, 0, 0, .3f, 1, .45f, 0), hit = track(t, .3f, 0, .42f, 1, .65f, 1, 1, 0);
                    p.rArmX = -0.4f + 0.5f * wind - 1.25f * hit; p.rArmY = -0.3f * wind + 0.25f * hit; p.rArmZ = 0.2f * wind;
                    p.lArmX = -1.15f + 0.3f * hit; p.lArmY = 0.55f; p.lArmZ = -0.1f;
                    p.bodyY = 0.45f * wind - 0.55f * hit; p.bodyX = 0.1f + 0.15f * hit; p.headY = -p.bodyY * 0.6f;
                    p.lLegX = -0.15f - 0.35f * hit; p.rLegX = 0.1f + 0.25f * hit; p.lLegZ = -0.08f; p.rLegZ = 0.08f;
                    p.rootZ = -1.5f * hit; p.rootY = -0.4f;
                }));

        // ---- Chainsaw: rev, plunge in and grind, rip out ----
        out.add(AttackDef.of("chainsaw").range(0, 3f).dur(30).cd(50).weight(2).weapon("chainsaw").moving()
                .tick((e, tg, n) -> {
                    if (n % 4 == 0) Combat.sound(e, SoundEvents.UI_STONECUTTER_TAKE_RESULT, 1f, 0.55f + R.nextFloat() * 0.15f);
                    if (n == 6) Combat.lunge(e, 0.35, 0);
                    if (n >= 6 && n <= 26 && n % 3 == 0) {
                        ServerLevel l = lvl(e); Vec3 tip = Combat.muzzle(e, 1.4, 0.2, -0.3);
                        Fx.sparks(l, tip, 3);
                        for (LivingEntity f : Combat.cone(e, 3.0, 50)) {
                            Combat.damage(e, f, 3f); f.setDeltaMovement(f.getDeltaMovement().scale(0.3));
                            Fx.dustBurst(l, 0x7a0000, 1.4f, f.position().add(0, f.getBbHeight() * 0.5, 0), 8, 0.25);
                        }
                    }
                })
                .anim((p, t, age) -> {
                    float rev = win(t, 0, .2f), plunge = track(t, 0, 0, .2f, 0, .3f, 1, .85f, 1, 1, 0);
                    float jit = (float) Math.sin(age * 2.7f) * 0.04f * rev;
                    p.rArmX = -0.9f - 0.4f * plunge + jit; p.rArmY = -0.2f;
                    p.lArmX = -0.8f - 0.4f * plunge + jit; p.lArmY = 0.5f;
                    p.rHandX = -HALF - p.rArmX + 0.12f * plunge + jit;
                    p.bodyX = 0.25f + 0.2f * plunge; p.bodyY = (float) Math.sin(age * 0.9f) * 0.09f * plunge;
                    p.headX = 0.2f; p.headY = -p.bodyY;
                    p.rootZ = -2f * plunge; p.rootY = -1f;
                    p.lLegX = -0.45f; p.rLegX = 0.35f; p.rLegZ = 0.12f; p.lLegZ = -0.12f;
                }));

        // ---- Pistol: bladed one-handed double tap ----
        out.add(AttackDef.of("pistol").range(3, 20).dur(18).cd(20).weight(1).weapon("pistol")
                .tick((e, tg, n) -> {
                    if (tg == null || (n != 4 && n != 11)) return;
                    Vec3 m = Combat.muzzle(e, 0.9, 0.35, 0);
                    Combat.hitscan(e, m, Combat.dirTo(m, tg, 0.02, R), 24, 4.5f, 0, 0xffe080);
                    flash(e, m, 2); Combat.gunSound(e, 1.6f);
                })
                .anim((p, t, age) -> {
                    float raise = track(t, 0, 0, .18f, 1, .85f, 1, 1, 0), rec = kick(t, .22f, .25f) + kick(t, .61f, .25f);
                    p.rArmX = lerp(-0.3f, -1.62f, raise) - 0.45f * rec; p.rArmY = -0.1f; p.rHandX = -0.5f * rec;
                    p.lArmX = 0.2f; p.lArmZ = -0.5f; p.lArmY = -0.3f;
                    p.bodyY = -0.5f * raise; p.headY = 0.45f * raise; p.bodyX = 0.05f - 0.06f * rec;
                    p.rLegX = 0.25f; p.lLegX = -0.2f; p.lLegZ = -0.1f; p.rootZ = 0.5f * rec;
                }));

        // ---- Shotgun: fire, pump ----
        out.add(AttackDef.of("shotgun").range(3, 14).dur(26).cd(30).weight(2).weapon("shotgun")
                .tick((e, tg, n) -> {
                    if (n == 11) { Combat.sound(e, SoundEvents.ARMOR_EQUIP_IRON, 1f, 1.6f); eject(e, 1); }
                    if (tg == null || n != 5) return;
                    Vec3 m = Combat.muzzle(e, 1.0, 0.3, 0);
                    for (int i = 0; i < 7; i++) Combat.hitscan(e, m, Combat.dirTo(m, tg, 0.06, R), 18, 2.8f, 0, 0xffd070);
                    flash(e, m, 4); Combat.gunSound(e, 0.9f);
                })
                .anim((p, t, age) -> {
                    float raise = track(t, 0, 0, .12f, 1, .9f, 1, 1, 0), rec = kick(t, .19f, .3f);
                    float pump = track(t, .38f, 0, .5f, 1, .62f, 1, .72f, 0);
                    p.rArmX = lerp(-0.45f, -HALF - rec * 0.3f, raise); p.rArmY = lerp(0, -0.15f, raise);
                    p.lArmX = lerp(-0.7f, -1.45f - rec * 0.2f, raise) + 0.45f * pump; p.lArmY = lerp(0.5f, 0.45f, raise); p.lArmZ = -0.1f * pump;
                    p.rHandX = -0.25f * pump - 0.6f * rec;
                    p.bodyX = 0.08f - 0.18f * rec; p.bodyY = 0.1f * pump; p.headX = 0.1f * pump;
                    p.rootZ = 1.2f * rec; p.rootY = -0.5f; p.lLegX = -0.3f; p.rLegX = 0.25f;
                }));

        // ---- SUPER SHOTGUN: both barrels, break open, eject, load two shells, snap shut ----
        out.add(AttackDef.of("super_shotgun").range(0, 9).dur(50).cd(50).weight(3).weapon("ssg")
                .tick((e, tg, n) -> {
                    ServerLevel l = lvl(e);
                    switch (n) {
                        case 6 -> {
                            if (tg == null) return;
                            Vec3 m = Combat.muzzle(e, 1.1, 0.3, 0);
                            for (int i = 0; i < 20; i++) Combat.hitscan(e, m, Combat.dirTo(m, tg, 0.13, R), 14, 2.3f, 0, 0xffc060);
                            for (LivingEntity f : Combat.cone(e, 5, 30)) Combat.knock(f, e.position(), 1.1, 0.3);
                            flash(e, m, 8); Fx.burst(l, ParticleTypes.LARGE_SMOKE, m, 6, 0.2, 0.02);
                            Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.5f); Combat.gunSound(e, 0.55f);
                        }
                        case 14 -> Combat.sound(e, SoundEvents.IRON_TRAPDOOR_OPEN, 1f, 1.4f);
                        case 16 -> eject(e, 2);
                        case 22 -> Combat.sound(e, SoundEvents.ARMOR_EQUIP_GENERIC, 1f, 1.2f);
                        case 30 -> Combat.sound(e, SoundEvents.ARMOR_EQUIP_IRON, 1f, 1.5f);
                        case 38 -> Combat.sound(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 1f, 1.8f);
                        default -> {}
                    }
                })
                .anim((p, t, age) -> {
                    float rec = kick(t, .12f, .22f);
                    float aim = track(t, 0, 0, .1f, 1, .24f, 1, .32f, 0, .74f, 0, .82f, 1, .95f, 1, 1, .6f);
                    float open = track(t, .24f, 0, .32f, 1, .74f, 1, .78f, 0);
                    float reach = track(t, .36f, 0, .44f, 1, .5f, 1, .6f, 0), load = track(t, .52f, 0, .62f, 1, .72f, 1, .78f, 0);
                    float flick = bump(win(t, .74f, .84f));
                    p.weaponR = open > 0.5f ? "ssg_open" : "ssg";
                    p.weaponL = t > .44f && t < .7f ? "shells" : null;
                    p.rArmX = lerp(-0.85f, -HALF, aim) - rec * 0.55f - 0.25f * flick; p.rArmY = lerp(-0.35f, -0.15f, aim);
                    p.rHandX = 0.3f * open - 0.9f * rec - 0.45f * flick;
                    float lx = lerp(-1.45f, -0.6f, 1 - aim); lx = lerp(lx, 0.3f, reach); lx = lerp(lx, -1.05f, load);
                    float ly = lerp(0.45f, 0.05f, reach); ly = lerp(ly, 0.75f, load);
                    p.lArmX = lx; p.lArmY = ly; p.lArmZ = -0.25f * reach;
                    p.bodyX = 0.1f - 0.35f * rec + 0.12f * (1 - aim); p.bodyY = 0.25f * reach - 0.15f * load;
                    p.headX = 0.35f * (1 - aim) - 0.1f * rec; p.headY = -0.2f * reach;
                    p.rootZ = 2.5f * rec; p.rootY = -0.6f - 0.4f * rec;
                    p.lLegX = -0.4f; p.rLegX = 0.35f + 0.2f * rec; p.lLegZ = -0.1f; p.rLegZ = 0.1f;
                }));

        // ---- Chaingun: braced hip fire, sweeping, spinning barrels ----
        out.add(AttackDef.of("chaingun").range(4, 18).dur(40).cd(45).weight(2).weapon("chaingun")
                .tick((e, tg, n) -> {
                    if (n == 0) Combat.sound(e, SoundEvents.MINECART_RIDING, 0.6f, 1.8f);
                    if (tg == null || n < 6 || n > 36 || n % 2 != 0) return;
                    Vec3 m = Combat.muzzle(e, 1.1, 0.3, -0.15);
                    Combat.hitscan(e, m, Combat.dirTo(m, tg, 0.045, R), 22, 2.2f, 0, 0xffe090);
                    flash(e, m, 1); Combat.sound(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.7f + R.nextFloat() * 0.2f);
                    Fx.dustBurst(lvl(e), 0xc8a040, 0.6f, Combat.muzzle(e, 0.4, 0.6, -0.2), 1, 0.05);
                })
                .anim((p, t, age) -> {
                    float fire = win(t, .12f, .18f) * (1 - win(t, .9f, .96f)), raise = track(t, 0, 0, .12f, 1, .92f, 1, 1, 0);
                    float jit = (float) Math.sin(age * 5f) * 0.05f * fire;
                    if (fire > 0 && ((int) (age * 1.5f)) % 2 == 1) p.weaponR = "chaingun_b";
                    p.rArmX = lerp(-0.4f, -1.25f, raise) + jit; p.rArmY = -0.1f;
                    p.lArmX = lerp(-0.5f, -1.1f, raise) + jit; p.lArmY = 0.55f;
                    p.rHandX = (-HALF - p.rArmX) * raise;
                    p.bodyX = -0.12f * fire; p.bodyY = (float) Math.sin(age * 0.25f) * 0.12f * fire;
                    p.headX = 0.05f; p.headY = -p.bodyY;
                    p.rootZ = 0.6f * fire + jit * 4; p.rootY = -1f;
                    p.lLegX = -0.35f; p.rLegX = 0.35f; p.rLegZ = 0.15f; p.lLegZ = -0.15f;
                }));

        // ---- Rocket Launcher: drop to a knee, shoulder it, heavy recoil ----
        out.add(AttackDef.of("rocket_launcher").range(5, 24).dur(30).cd(50).weight(2).weapon("rocket")
                .tick((e, tg, n) -> {
                    if (tg == null || n != 10) return;
                    ServerLevel l = lvl(e);
                    Vec3 m = Combat.muzzle(e, 1.0, 0.3, 0.25);
                    Combat.shoot(e, D2_ROCKET, m, Combat.dirTo(m, tg, 0.01, R), 1.3, 16f);
                    Vec3 back = Combat.muzzle(e, -0.8, 0.3, 0.3);
                    Fx.burst(l, ParticleTypes.LARGE_SMOKE, back, 8, 0.2, 0.05); Fx.burst(l, ParticleTypes.FLAME, back, 6, 0.15, 0.05);
                    Combat.sound(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5f, 0.7f);
                })
                .anim((p, t, age) -> {
                    float raise = track(t, 0, 0, .25f, 1, .8f, 1, 1, 0), kneel = track(t, .1f, 0, .28f, 1, .8f, 1, 1, 0), rec = kick(t, .33f, .35f);
                    p.rArmX = lerp(-0.4f, -1.75f, raise); p.rHandX = (-HALF - p.rArmX) * raise - 0.6f * rec; p.rArmY = -0.1f;
                    p.lArmX = lerp(-0.3f, -1.4f, raise); p.lArmY = 0.55f * raise;
                    p.rLegX = 1.0f * kneel; p.lLegX = -0.7f * kneel; p.rootY = -3f * kneel;
                    p.bodyX = 0.15f - 0.4f * rec; p.bodyY = -0.2f * raise; p.headY = 0.2f * raise; p.headX = -0.2f * rec;
                    p.rootZ = 2.5f * rec;
                }));

        // ---- Plasma Rifle: strafing stream of blue bolts ----
        out.add(AttackDef.of("plasma_rifle").range(4, 20).dur(34).cd(40).weight(2).weapon("plasma").moving()
                .tick((e, tg, n) -> {
                    e.getMoveControl().strafe(0, ((e.tickCount / 34) % 2 == 0) ? 0.5f : -0.5f);
                    if (tg == null || n < 6 || n > 27 || n % 3 != 0) return;
                    Vec3 m = Combat.muzzle(e, 1.0, 0.3, 0);
                    Combat.shoot(e, D2_PLASMA, m, Combat.dirTo(m, tg, 0.03, R), 1.6, 3.5f);
                    Fx.burst(lvl(e), ParticleTypes.ELECTRIC_SPARK, m, 3, 0.05, 0.1);
                    Combat.sound(e, SoundEvents.BLAZE_SHOOT, 0.6f, 1.8f);
                })
                .anim((p, t, age) -> {
                    float raise = track(t, 0, 0, .15f, 1, .88f, 1, 1, 0), ft = t * 34f;
                    float rec = ft >= 6 && ft < 30 ? kick((ft - 6) % 3f / 3f, 0, 1f) : 0;
                    float step = (float) Math.sin(age * 0.6f) * 0.3f * raise;
                    p.rArmX = lerp(-0.45f, -HALF, raise) - 0.18f * rec; p.rArmY = -0.12f; p.rHandX = -0.25f * rec;
                    p.lArmX = lerp(-0.7f, -1.45f, raise); p.lArmY = 0.45f;
                    p.bodyX = 0.05f - 0.06f * rec; p.rootRoll = step * 0.1f;
                    p.rLegZ = step; p.lLegZ = step; p.rLegX = 0.1f; p.lLegX = -0.1f; p.rootY = -0.3f;
                }));

        // ---- BFG 9000: charge, fire green ball, tracer spray on impact ----
        out.add(AttackDef.of("bfg9000").range(6, 28).dur(60).cd(260).weight(1).weapon("bfg")
                .tick((e, tg, n) -> {
                    ServerLevel l = lvl(e);
                    if (n == 0) Combat.sound(e, SoundEvents.BEACON_ACTIVATE, 1.5f, 1.4f);
                    if (n < 30) {
                        Vec3 m = Combat.muzzle(e, 1.1, 0.3, -0.2); double r = 1.6 * (1 - n / 30.0);
                        for (int i = 0; i < 4; i++) {
                            double a = R.nextDouble() * Math.PI * 2, b = R.nextDouble() * Math.PI - Math.PI / 2;
                            Fx.dustBurst(l, 0x50ff40, 1.5f, m.add(Math.cos(a) * Math.cos(b) * r, Math.sin(b) * r, Math.sin(a) * Math.cos(b) * r), 1, 0);
                        }
                    }
                    if (n == 30 && tg != null) {
                        Vec3 m = Combat.muzzle(e, 1.2, 0.3, -0.2);
                        Combat.shoot(e, D2_BFG, m, Combat.dirTo(m, tg, 0.0, R), 0.9, 40f);
                        Fx.dustBurst(l, 0x80ff60, 2.5f, m, 20, 0.4);
                        Combat.sound(e, SoundEvents.WARDEN_SONIC_BOOM, 1.2f, 1.3f);
                    }
                })
                .anim((p, t, age) -> {
                    float charge = t < .5f ? win(t, .05f, .5f) : 0, rec = kick(t, .5f, .4f), hold = track(t, 0, 0, .12f, 1, .9f, 1, 1, 0);
                    float sh = charge * (float) Math.sin(age * 4f) * 0.04f;
                    p.rArmX = lerp(-0.4f, -1.05f, hold) + sh - 0.6f * rec; p.rArmY = -0.15f;
                    p.lArmX = lerp(-0.4f, -1.0f, hold) + sh - 0.5f * rec; p.lArmY = 0.6f;
                    p.rHandX = (-HALF - p.rArmX) * hold - 0.7f * rec;
                    p.bodyX = 0.2f * charge - 0.45f * rec; p.headX = 0.1f * charge - 0.3f * rec;
                    p.rootY = -1.6f * charge + 1f * rec; p.rootZ = 4f * rec + sh * 10;
                    p.lLegX = -0.45f; p.rLegX = 0.45f + 0.3f * rec; p.rLegZ = 0.15f; p.lLegZ = -0.15f;
                }));

        // ---- Megasphere + Berserk: grab power, roar, crouch, gibbing uppercut ----
        out.add(AttackDef.of("berserk_punch").range(0, 3.2f).dur(28).cd(200).weight(1).weapons("knuckle", "knuckle")
                .onStart((e, tg, n) -> {
                    ServerLevel l = lvl(e);
                    e.heal(e.getMaxHealth());
                    e.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 3));
                    e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
                    Vec3 c = e.position().add(0, 1, 0);
                    for (int i = 0; i < 3; i++) Fx.ring(l, c.add(0, i * 0.4 - 0.4, 0), 1.2f, 0x6080ff, 1.5f, 20);
                    Fx.dustBurst(l, 0xd01010, 2f, c, 25, 0.6);
                    Combat.sound(e, SoundEvents.PLAYER_LEVELUP, 1f, 0.6f); Combat.sound(e, SoundEvents.RAVAGER_ROAR, 0.8f, 1.3f);
                })
                .tick((e, tg, n) -> {
                    ServerLevel l = lvl(e);
                    if (n < 12 && n % 2 == 0) Fx.dustBurst(l, 0xff1010, 1.2f, e.position().add(0, 1.2, 0), 4, 0.4);
                    if (n == 12) Combat.lunge(e, 0.6, 0.15);
                    if (n != 15) return;
                    for (LivingEntity f : Combat.cone(e, 3.2, 70)) {
                        Combat.damage(e, f, 20f); Combat.knock(f, e.position(), 2.0, 0.6);
                        Fx.dustBurst(l, 0x7a0000, 2f, f.position().add(0, f.getBbHeight() * 0.6, 0), 24, 0.4);
                    }
                    Fx.sparks(l, Combat.muzzle(e, 1.3, 0.2, 0.2), 10);
                    Combat.sound(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f);
                    Combat.sound(e, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 0.9f, 1.2f);
                })
                .anim((p, t, age) -> {
                    float flex = track(t, 0, 0, .12f, 1, .3f, 1, .38f, 0), cock = track(t, .3f, 0, .46f, 1, .54f, 0), up = track(t, .46f, 0, .56f, 1, .75f, 1, 1, 0);
                    p.rArmX = -0.3f * flex + 0.7f * cock - 2.6f * up; p.rArmZ = 0.9f * flex; p.rArmY = 0.2f * up;
                    p.lArmX = -0.3f * flex - 0.9f * cock + 0.6f * up; p.lArmZ = -0.9f * flex - 0.3f * up; p.lArmY = 0.4f * cock;
                    p.headX = -0.4f * flex + 0.2f * cock; p.bodyX = -0.15f * flex + 0.25f * cock - 0.25f * up;
                    p.bodyY = 0.6f * cock - 0.55f * up; p.headY = -p.bodyY * 0.6f;
                    p.rootY = 0.4f * flex - 1.5f * cock + 1.2f * up; p.rootZ = -3f * up;
                    p.lLegX = -0.3f * cock - 0.5f * up; p.rLegX = 0.2f * cock + 0.4f * up; p.rLegZ = 0.1f; p.lLegZ = -0.1f;
                }));

        // ---- Invulnerability sphere: crouch, burst of red light, damage-immune pulse ----
        out.add(AttackDef.of("invulnerability").range(0, 16).dur(30).cd(400).weight(1).noLos()
                .when((e, tg) -> e.getHealth() < e.getMaxHealth() * 0.6f || Combat.around(e, e.position(), 6).size() >= 3)
                .onStart((e, tg, n) -> {
                    e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 4));
                    e.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 120, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
                    Combat.sound(e, SoundEvents.BEACON_POWER_SELECT, 1.2f, 0.5f); Combat.sound(e, SoundEvents.TOTEM_USE, 0.5f, 1.4f);
                })
                .tick((e, tg, n) -> {
                    ServerLevel l = lvl(e); Vec3 c = e.position().add(0, 1, 0);
                    if (n < 10) {
                        double r = 2.5 * (1 - n / 10.0);
                        for (int i = 0; i < 6; i++) { double a = R.nextDouble() * Math.PI * 2; Fx.dustBurst(l, 0xff2010, 1.5f, c.add(Math.cos(a) * r, R.nextDouble() - 0.5, Math.sin(a) * r), 1, 0); }
                    }
                    if (n == 10) {
                        for (LivingEntity f : Combat.around(e, c, 5)) { Combat.damage(e, f, 5f); Combat.knock(f, c, 1.4, 0.4); }
                        Fx.dustBurst(l, 0xff0000, 3f, c, 40, 0.8);
                        Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 0.6f, 0.7f);
                    }
                    if (n >= 10 && n <= 16) Fx.ring(l, e.position().add(0, 0.2, 0), 1f + (n - 10) * 0.7f, 0xff2010, 2f, 32);
                })
                .anim((p, t, age) -> {
                    float gather = track(t, 0, 0, .25f, 1, .33f, 0), burst = track(t, .28f, 0, .36f, 1, .7f, 1, 1, 0);
                    p.rArmX = -1.2f * gather - 0.6f * burst; p.rArmY = 0.7f * gather; p.rArmZ = 1.4f * burst;
                    p.lArmX = -1.2f * gather - 0.6f * burst; p.lArmY = -0.7f * gather; p.lArmZ = -1.4f * burst;
                    p.bodyX = 0.35f * gather - 0.3f * burst; p.headX = 0.3f * gather - 0.45f * burst;
                    p.rootY = -1.5f * gather + 1.5f * burst;
                    p.rLegZ = 0.1f * gather + 0.3f * burst; p.lLegZ = -0.1f * gather - 0.3f * burst; p.rLegX = 0.2f * gather; p.lLegX = -0.2f * gather;
                }));
    }

    // ---------------- server helpers ----------------
    private static ServerLevel lvl(SlayerEntity e) { return (ServerLevel) e.level(); }
    private static void flash(SlayerEntity e, Vec3 m, int n) {
        ServerLevel l = lvl(e);
        Fx.burst(l, ParticleTypes.FLAME, m, n, 0.06, 0.02); Fx.dustBurst(l, 0xfff0a0, 1.2f, m, n, 0.08);
    }
    private static void eject(SlayerEntity e, int shells) {
        ServerLevel l = lvl(e); Vec3 p = Combat.muzzle(e, 0.3, 0.5, 0.1);
        Fx.dustBurst(l, 0xa02020, 0.8f, p, shells * 2, 0.1); Fx.burst(l, ParticleTypes.SMOKE, p, 3, 0.05, 0.01);
    }
    /** BFG 9000: green explosion + the famous tracer spray from the shooter along its facing. */
    private static void bfgBurst(SlayerProjectile p, Vec3 pos, Entity hit) {
        ServerLevel l = (ServerLevel) p.level();
        Fx.explosion(l, pos, 2f); Fx.dustBurst(l, 0x50ff40, 3f, pos, 40, 1.5);
        Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1.5f, 0.7f);
        if (!(p.getOwner() instanceof SlayerEntity o) || !o.isAlive()) return;
        for (LivingEntity f : Combat.cone(o, 20, 45)) {
            Combat.damage(o, f, 9f);
            Vec3 c = f.position().add(0, f.getBbHeight() * 0.5, 0);
            Fx.beam(l, o.getEyePosition(), c, 0x50ff40, 1f, 0.6); Fx.dustBurst(l, 0x80ff60, 2f, c, 8, 0.3);
        }
    }

    @Override
    public void passive(SlayerEntity e) {
        MobEffectInstance r = e.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (r != null && r.getAmplifier() >= 4 && e.tickCount % 2 == 0)
            Fx.dustBurst(lvl(e), 0xff1a10, 1.3f, e.position().add(0, e.getBbHeight() * 0.5, 0), 5, 0.4);
    }

    // =====================================================================================
    // client: idle + model
    // =====================================================================================
    @Override
    public void idle(Pose p, SlayerEntity e, float age, float limbAmt) {
        float still = 1 - limbAmt;
        // low-ready with the super shotgun, chin down, wide planted stance
        p.rArmX += -0.45f; p.rHandX = -0.35f;
        p.lArmX = lerp(p.lArmX, -0.75f, still); p.lArmY = 0.5f * still;
        p.headX += 0.12f; p.bodyX += 0.05f;
        p.rLegZ += 0.09f * still; p.lLegZ -= 0.09f * still; p.lLegX -= 0.12f * still; p.rLegX += 0.08f * still;
    }

    private static int h(int x, int y, int s) {
        int k = x * 73856093 ^ y * 19349663 ^ s * 83492791; k ^= k >>> 13; k *= 0x5bd1e995; k ^= k >>> 15; return k & 0xffff;
    }
    /** battle damage: scratches (light), scorch (dark), blood spatter */
    private static Mat.Decal worn(float blood) {
        return (m, g, x, y, w, hh, f) -> {
            for (int j = 0; j < hh; j++) for (int i = 0; i < w; i++) {
                int v = h(x + i, y + j, m.seed);
                if (v < 2400) g.px(x + i, y + j, Mat.mix(m.base, 0xb8b49a, 0.45f));
                else if (v > 65535 - (int) (blood * 65535)) g.px(x + i, y + j, (v & 1) == 0 ? 0x5a0a08 : 0x3a0605);
                else if (v > 30000 && v < 31500) g.px(x + i, y + j, Mat.mix(m.base, 0x101008, 0.6f));
            }
            if (w >= 5 && hh >= 4) { // a diagonal gouge
                int sx = h(x, y, 7) % (w - 3), sy = h(y, x, 9) % (hh - 2);
                for (int k = 0; k < 3; k++) g.px(x + sx + k, y + sy + k / 2, Mat.mix(m.base, 0xd0ccb0, 0.5f));
            }
        };
    }

    @Override
    public void buildModel(Rig rig) {
        Mat armor = Mat.of(0x3d5a2a).noise(0.2f).bevel(0.22f).decal(worn(0.03f));
        Mat armorDk = Mat.of(0x2b3f1e).noise(0.18f).bevel(0.3f).decal(worn(0.04f));
        Mat legs = Mat.of(0x34482a).noise(0.16f).decal(worn(0.02f));
        Mat leather = Mat.of(0x4a3420).noise(0.15f);
        Mat boot = Mat.of(0x2a1f14).noise(0.12f).bevel(0.3f);
        Mat skin = Mat.of(0xa87a5c).noise(0.1f).decal(worn(0.06f));
        Mat face = Mat.of(0x2a2c2e).noise(0.1f);
        Mat visor = Mat.of(0xff4a18).glow();
        Mat edge = Mat.of(0x6b6a52).noise(0.2f);

        // body
        rig.box(rig.torso, armor, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, armorDk, -4.5f, -11.5f, -2.6f, 9, 6, 1);       // chest plate
        rig.box(rig.torso, leather, -4.5f, -2.5f, -2.5f, 9, 2, 5);        // belt
        rig.box(rig.torso, leather, -4, -3.2f, -3.3f, 2, 2, 1);            // pouches
        rig.box(rig.torso, leather, 2, -3.2f, -3.3f, 2, 2, 1);
        rig.box(rig.torso, armorDk, -3, -11, 2, 6, 7, 2);                  // backpack
        rig.box(rig.torso, armorDk, -3, -13, -2.5f, 6, 2, 5);              // collar
        // head: helmet, red-orange visor, mouth guard, crest, ear pieces
        rig.box(rig.head, armor, -4, -8, -4, 8, 8, 8);
        rig.box(rig.head, visor, -3, -5.5f, -4.6f, 6, 2, 1);
        rig.box(rig.head, face, -3, -3.5f, -4.5f, 6, 3, 1);
        rig.box(rig.head, armorDk, -1, -8.8f, -4.2f, 2, 1, 8);
        rig.box(rig.head, armorDk, -4.6f, -5, -1.5f, 1, 3, 3);
        rig.box(rig.head, armorDk, 3.6f, -5, -1.5f, 1, 3, 3);
        // arms: heavy pads with worn rim, bracers, bare scarred fists
        for (int s = 0; s < 2; s++) {
            PartDefinition arm = s == 0 ? rig.rArm : rig.lArm; float o = s == 0 ? -1.2f : 0.2f;
            rig.box(arm, armor, -2, -2, -2, 4, 12, 4);
            rig.box(arm, armorDk, -2.5f + o, -2.8f, -2.5f, 5, 3, 5);
            rig.box(arm, edge, -2.7f + o, 0.2f, -2.7f, 5, 1, 5, 0.2f);
            rig.box(arm, leather, -2.5f, 4, -2.5f, 5, 3, 5);
            rig.box(arm, skin, -2, 8, -2, 4, 2, 4, 0.25f);
        }
        // legs: knee pads, boots, thigh pouch
        for (int s = 0; s < 2; s++) {
            PartDefinition leg = s == 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, legs, -2, 0, -2, 4, 12, 4);
            rig.box(leg, armorDk, -2.5f, 4, -2.7f, 5, 3, 1);
            rig.box(leg, boot, -2.5f, 9, -2.8f, 5, 3, 5);
        }
        rig.box(rig.rLeg, leather, -3.2f, 1, -1.5f, 1, 3, 3);

        // ---------------- weapons (GUN SPACE: x right, y down, barrel -z) ----------------
        Mat metal = Mat.of(0x5c5f63).noise(0.1f), gun = Mat.of(0x2d2f33).noise(0.08f), dark = Mat.of(0x18191b), wood = Mat.of(0x5b3a1f).noise(0.2f);
        Mat orange = Mat.of(0xa04818).noise(0.15f).decal(worn(0.05f));
        Mat plasmaGlow = Mat.of(0x40a0ff).glow(), bfgGlow = Mat.of(0x50ff40).glow(), red = Mat.of(0xa82018), brass = Mat.of(0xc09030);

        PartDefinition w = rig.weapon(rig.rHand, "pistol");
        rig.box(w, dark, -1, -1, -1, 2, 4, 2);
        rig.box(w, metal, -1, -3, -6, 2, 2, 8);
        rig.box(w, dark, -0.5f, -2.5f, -7.5f, 1, 1, 2);

        w = rig.weapon(rig.rHand, "shotgun");
        rig.box(w, wood, -1, -2, 1, 2, 3, 6);
        rig.box(w, gun, -1.5f, -3, -4, 3, 3, 5);
        rig.box(w, dark, -0.5f, -3, -15, 1, 1, 11);
        rig.box(w, dark, -0.5f, -2, -13, 1, 1, 9);
        rig.box(w, wood, -1, -2.5f, -11, 2, 2, 4);

        superShotgun(rig, "ssg", 0f, wood, metal, dark);
        superShotgun(rig, "ssg_open", 0.65f, wood, metal, dark);
        w = rig.weapon(rig.lHand, "shells");
        rig.box(w, red, -1.6f, -1, -1.5f, 1, 1, 3);
        rig.box(w, red, 0.4f, -1, -1.5f, 1, 1, 3);
        rig.box(w, brass, -1.8f, -1.2f, 1.5f, 3, 1, 1);

        chaingun(rig, "chaingun", false, gun, dark, metal, brass);
        chaingun(rig, "chaingun_b", true, gun, dark, metal, brass);

        w = rig.weapon(rig.rHand, "rocket");
        rig.box(w, armorDk, -2, -6, -14, 4, 4, 20);
        rig.box(w, dark, -1, -2, -1, 2, 3, 2);
        rig.box(w, dark, -0.5f, -7.5f, -10, 1, 2, 1);
        rig.box(w, red, -1.5f, -5.5f, -15, 3, 3, 1);
        rig.box(w, dark, -2.5f, -6.5f, 5, 5, 5, 1);
        rig.box(w, gun, 2, -5.5f, -6, 1, 3, 6);

        w = rig.weapon(rig.rHand, "plasma");
        rig.box(w, metal, -2, -4, -8, 4, 3, 10);
        rig.box(w, plasmaGlow, -1.5f, -4.6f, -7, 3, 1, 6);
        rig.box(w, dark, -1, -3.5f, -12, 2, 2, 4);
        rig.box(w, plasmaGlow, -1.5f, -4, -12.8f, 3, 3, 1);
        rig.box(w, dark, -1, -1, -1, 2, 4, 2);
        rig.box(w, plasmaGlow, 2, -3.5f, -5, 1, 2, 4);

        w = rig.weapon(rig.rHand, "bfg");
        rig.box(w, Mat.of(0x3d4a3a).noise(0.12f).decal(worn(0.02f)), -3.5f, -6, -10, 7, 6, 14);
        rig.box(w, bfgGlow, -2.5f, -6.8f, -7, 5, 1, 8);
        rig.box(w, dark, -2.5f, -5.5f, -12, 5, 5, 2);
        rig.box(w, bfgGlow, -1.5f, -4.5f, -12.6f, 3, 3, 1);
        rig.box(w, dark, -1, -1, -1, 2, 4, 2);
        rig.box(w, gun, 3.5f, -5, -8, 1, 4, 10);
        rig.box(w, gun, -4.5f, -5, -8, 1, 4, 10);

        w = rig.weapon(rig.rHand, "chainsaw");
        rig.box(w, orange, -2, -4, -3, 4, 5, 7);
        rig.box(w, dark, -0.5f, -6, -2, 1, 2, 4);
        rig.box(w, Mat.of(0x5c5f63).noise(0.1f).decal((m, g, x, y, ww, hh, f) -> { for (int i = 0; i < ww; i += 2) { g.px(x + i, y, 0x202020); g.px(x + i, y + hh - 1, 0x202020); } }),
                -0.5f, -3.5f, -14, 1, 3, 11);
        rig.box(w, dark, 2, -3, -2, 1, 3, 4);

        for (int s = 0; s < 2; s++) {
            w = rig.weapon(s == 0 ? rig.rHand : rig.lHand, "knuckle");
            rig.box(w, Mat.of(0xff2010).glow(), -2.2f, -1, -2.7f, 4, 2, 1, 0.1f);
        }
    }

    private static void superShotgun(Rig rig, String name, float breakAngle, Mat wood, Mat metal, Mat dark) {
        PartDefinition w = rig.weapon(rig.rHand, name);
        rig.box(w, wood, -1.5f, -2, 1, 3, 3, 6);
        rig.box(w, metal, -2, -3.5f, -3, 4, 3, 4);
        PartDefinition hinge = rig.pivot(w, "hinge", 0, -2, -3, breakAngle, 0, 0);
        rig.box(hinge, dark, -2, -1.6f, -11, 2, 2, 11);
        rig.box(hinge, dark, 0, -1.6f, -11, 2, 2, 11);
        rig.box(hinge, wood, -1.5f, 0.3f, -8, 3, 1, 5);
    }

    private static void chaingun(Rig rig, String name, boolean rotated, Mat gun, Mat dark, Mat metal, Mat brass) {
        PartDefinition w = rig.weapon(rig.rHand, name);
        rig.box(w, dark, -1, -1, -1, 2, 4, 2);
        rig.box(w, gun, -2, -4, -6, 4, 4, 8);
        float[] off = rotated ? new float[]{-0.5f, -4.5f, 1, -3, -0.5f, -1.5f, -2, -3} : new float[]{-1.5f, -4, 0.5f, -4, -1.5f, -2, 0.5f, -2};
        for (int i = 0; i < 8; i += 2) rig.box(w, dark, off[i], off[i + 1], -16, 1, 1, 10);
        rig.box(w, metal, -2.5f, -5, -14, 5, 5, 1);
        rig.box(w, metal, -0.5f, -6, -5, 1, 2, 4);
        rig.box(w, brass, -3, -2, -4, 1, 3, 3);
    }
}
