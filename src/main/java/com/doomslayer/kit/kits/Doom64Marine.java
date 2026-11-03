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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

/** The silent Marine of DOOM 64: cold dark-green armour, the full N64 arsenal and the demon-key powered Unmaker. */
public class Doom64Marine extends SlayerKit {

    // ---- palette (cooler, darker than the 1993 marine) ----
    static final int ARMOR = 0x2c4a3c, ARMOR_DK = 0x1b2c26, SUIT = 0x26292c, METAL = 0x56606a, GUNMETAL = 0x34393f,
            LEATHER = 0x3e3326, VISOR = 0x5ad0b4, BONE = 0xcbbfa2, FLESH = 0x5a1a1e, LASER = 0xff2a2a;
    static final int[] KEY_RGB = {0xff3c9c, 0x9a48ff, 0x3ccfff}; // the three demon keys

    // ---- projectiles ----
    static final ProjStyle ROCKET64 = ProjStyle.make().color(0xffa838).size(0.45f).life(100).explode(3.8f).trail(ParticleTypes.LARGE_SMOKE)
            .onTick(p -> Fx.burst((ServerLevel) p.level(), ParticleTypes.FLAME, p.position(), 2, 0.03, 0.01));
    static final ProjStyle PLASMA64 = ProjStyle.make().color(0x4f9cff).size(0.38f).life(55).trail(ParticleTypes.ELECTRIC_SPARK)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, 0x80c0ff, 1.4f, pos, 10, 0.2); Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 8, 0.15, 0.2);
                Fx.sound(l, pos, SoundEvents.FIRE_EXTINGUISH, 0.5f, 1.8f);
            });
    static final ProjStyle BFG64 = ProjStyle.make().color(0x70ff58).size(1.9f).life(90).explode(6.5f).trail(ParticleTypes.HAPPY_VILLAGER)
            .onTick(p -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, 0x8cff6a, 2.2f, p.position(), 4, 0.6);
                if (p.age2 % 3 == 0) Fx.burst(l, ParticleTypes.GLOW, p.position(), 3, 0.5, 0.02);
            })
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 5f);
                Fx.burst(l, ParticleTypes.HAPPY_VILLAGER, pos, 70, 2.5, 0.1);
                Fx.ring(l, pos.add(0, 0.3, 0), 3f, 0x70ff58, 2.5f, 36); Fx.ring(l, pos.add(0, 0.8, 0), 5.5f, 0xb0ffa0, 2f, 48);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 2.5f, 0.55f); Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 2f, 0.6f);
                // BFG tracers: spray out from the shooter toward every visible foe
                if (p.getOwner() instanceof SlayerEntity o) {
                    Vec3 eye = o.getEyePosition(); int n = 0;
                    for (LivingEntity f : Combat.around(o, eye, 18)) {
                        if (n++ >= 12 || !o.hasLineOfSight(f)) continue;
                        Vec3 c = f.position().add(0, f.getBbHeight() * 0.5, 0);
                        Fx.beam(l, eye, c, 0x60ff40, 1.1f, 0.5); Fx.dustBurst(l, 0x9cff80, 1.6f, c, 10, 0.3);
                        Combat.damage(o, f, 9);
                    }
                }
            });

    public Doom64Marine() {
        super("doom64_marine", "Doomguy", "ドゥームガイ", "Doom 64");
        eggPrimary = ARMOR; eggSecondary = VISOR;
        maxHealth = 80; speed = 0.31; armor = 12; knockbackRes = 0.4;
        preferredRange = 8f; dmgMult = 1.1f;
        idleWeaponR = "shotgun";
    }

    // =====================================================================================================
    // attacks
    // =====================================================================================================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---- FIST: twisting right cross with a step-in ----
        out.add(AttackDef.of("fist").range(0, 2.6f).dur(14).cd(18).weight(3)
                .tick((e, t, n) -> {
                    if (n == 3) Combat.lunge(e, 0.35, 0.02);
                    if (n == 6) {
                        boolean hit = false;
                        for (LivingEntity f : Combat.cone(e, 2.8, 50)) { Combat.damage(e, f, 9); Combat.knock(f, e.position(), 0.8, 0.25); hit = true; sparkAt(e, f, 6); }
                        Combat.sound(e, hit ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_NODAMAGE, 1f, 0.7f);
                        if (hit) Combat.sound(e, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.4f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    reset(p);
                    float tw = track(t, 0, 0, .28f, .55f, .42f, -.5f, .65f, -.42f, 1, 0);
                    p.bodyY = tw;
                    p.bodyX = track(t, 0, 0, .28f, -.1f, .42f, .3f, .65f, .24f, 1, 0);
                    p.rArmX = trackLin(t, 0, -.3f, .28f, -.5f, .42f, -1.78f, .62f, -1.62f, 1, -.2f);
                    p.rArmZ = track(t, 0, 0, .28f, .4f, .42f, 0, 1, 0);
                    p.rArmY = -tw * .8f;
                    p.lArmX = track(t, 0, -1.1f, .28f, -1.35f, .42f, -.45f, .7f, -.6f, 1, -.2f);
                    p.lArmY = .5f; p.lArmZ = -.15f;
                    p.lLegX = track(t, 0, 0, .3f, -.45f, .5f, -.5f, 1, 0);
                    p.rLegX = track(t, 0, 0, .42f, .45f, .7f, .35f, 1, 0);
                    p.rootZ = track(t, 0, 0, .28f, .6f, .42f, -1.6f, .7f, -1.2f, 1, 0);
                    p.headX = .12f; p.headY = -tw * .5f;
                }));

        // ---- CHAINSAW: hip-held rev, thrust in, grinding side-to-side ----
        out.add(AttackDef.of("chainsaw").range(0, 2.4f).dur(34).cd(50).weight(2).weapon("chainsaw")
                .tick((e, t, n) -> {
                    ServerLevel l = (ServerLevel) e.level();
                    if (n == 0) Combat.sound(e, SoundEvents.BEE_LOOP_AGGRESSIVE, 1.2f, 0.5f);
                    if (n == 6) Combat.lunge(e, 0.3, 0);
                    if (n % 4 == 0) Combat.sound(e, SoundEvents.GRINDSTONE_USE, 0.9f, n < 6 ? 0.8f : 1.4f);
                    Vec3 tip = Combat.muzzle(e, 1.3, -0.2, -0.5);
                    Fx.burst(l, ParticleTypes.SMOKE, tip, 1, 0.05, 0.01);
                    if (n >= 7 && n <= 28 && n % 2 == 1) {
                        for (LivingEntity f : Combat.cone(e, 2.7, 45)) {
                            Combat.damage(e, f, 2.6f);
                            Vec3 c = f.position().add(0, f.getBbHeight() * 0.5, 0);
                            Fx.dustBurst(l, 0x7a0606, 1.2f, c, 6, 0.25); Fx.sparks(l, tip, 3);
                            Combat.knock(f, f.position().add(f.position().subtract(e.position())), 0.05, 0); // tug toward the saw
                        }
                    }
                })
                .anim((p, t, a) -> {
                    reset(p);
                    float rev = win(t, 0, .2f), thrust = track(t, 0, 0, .2f, 0, .3f, 1, .85f, 1, 1, 0);
                    float grind = thrust * (float) Math.sin(t * PI * 7) * .16f, buzz = (float) Math.sin(a * 2.7f) * .035f * (rev + thrust);
                    p.rArmX = lerp(-.8f, -1.25f, thrust) + buzz; p.rArmY = -.15f;
                    p.lArmX = lerp(-.95f, -1.35f, thrust) - buzz; p.lArmY = .65f;
                    p.rHandX = lerp(-.75f, -.35f, thrust) + buzz * 2; p.rHandY = grind * .4f;
                    p.bodyX = .12f + .22f * thrust + buzz; p.bodyY = grind - .12f;
                    p.headX = .2f * thrust; p.headY = -grind * .5f;
                    p.lLegX = -.4f * thrust - .1f; p.rLegX = .35f * thrust + .1f; p.rLegZ = .1f; p.lLegZ = -.1f;
                    p.rootZ = -1.2f * thrust; p.rootY = -.3f * thrust;
                }));

        // ---- PISTOL: one-handed, bladed stance, two measured shots ----
        out.add(AttackDef.of("pistol").range(2, 20).dur(18).cd(14).weight(3).weapon("pistol")
                .tick((e, t, n) -> {
                    if (n == 4 || n == 11) {
                        Vec3 mz = Combat.muzzle(e, 1.0, -0.4, 0.05);
                        Combat.hitscan(e, mz, aim(e, mz, t, 0.012), 24, 5, 0, 0xffe090);
                        flash(e, mz, 0xffd070, 1);
                        Combat.sound(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, 1.9f);
                    }
                })
                .anim((p, t, a) -> {
                    reset(p);
                    float up = track(t, 0, 0, .15f, 1, .82f, 1, 1, 0), k = kick(t, .22f, .25f) + kick(t, .61f, .25f);
                    p.bodyY = -.32f * up;
                    p.rArmX = lerp(-.4f, -HALF - .05f, up) - k * .45f; p.rArmY = .3f * up;
                    p.rHandX = lerp(-.9f, 0, up) - k * .7f;
                    p.lArmX = -.15f; p.lArmZ = -.18f; p.lArmY = .1f;
                    p.headX = .04f - k * .06f; p.headY = .22f * up; p.headZ = .05f * up;
                    p.bodyX = .04f - k * .1f;
                    p.rLegX = .2f * up; p.lLegX = -.25f * up; p.rLegZ = .06f;
                }));

        // ---- SHOTGUN: shoulder, heavy kick, pump ----
        out.add(AttackDef.of("shotgun").range(1.5f, 13).dur(26).cd(24).weight(4).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 6) {
                        Vec3 mz = Combat.muzzle(e, 1.2, -0.3, 0.05);
                        pellets(e, mz, t, 7, 0.065, 2.5f, 16, 0xffd890);
                        flash(e, mz, 0xffc060, 2);
                        Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.7f); Combat.gunSound(e, 0.8f);
                    }
                    if (n == 12) Combat.sound(e, SoundEvents.PISTON_CONTRACT, 0.7f, 1.6f);
                    if (n == 17) Combat.sound(e, SoundEvents.PISTON_EXTEND, 0.7f, 1.8f);
                })
                .anim((p, t, a) -> {
                    reset(p);
                    float up = track(t, 0, 0, .18f, 1, .85f, 1, 1, 0), k = kick(t, .23f, .3f);
                    float pump = track(t, 0, 0, .45f, 0, .56f, 1, .66f, 1, .74f, 0);
                    hold(p, k);
                    p.lArmX += pump * .38f; p.lArmY -= pump * .1f;
                    p.rHandX += pump * .18f; p.rArmX += pump * .1f;
                    p.headX += pump * .12f;
                    p.bodyY = -.1f - k * .15f;
                    stance(p, 1);
                    raise(p, up);
                }));

        // ---- SUPER SHOTGUN: crouched blast, break-open reload ----
        out.add(AttackDef.of("super_shotgun").range(1, 8).dur(36).cd(55).weight(3).weapon("super_shotgun")
                .tick((e, t, n) -> {
                    ServerLevel l = (ServerLevel) e.level();
                    if (n == 7) {
                        Vec3 mz = Combat.muzzle(e, 1.2, -0.3, 0.05);
                        pellets(e, mz, t, 20, 0.13, 2.2f, 12, 0xffc880);
                        flash(e, mz, 0xff9a40, 4);
                        Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 1f, 1.15f); Combat.gunSound(e, 0.6f);
                        Combat.lunge(e, -0.25, 0.05);
                    }
                    if (n > 7 && n < 14) Fx.burst(l, ParticleTypes.SMOKE, Combat.muzzle(e, 1.1, -0.3, 0.1), 1, 0.03, 0.02);
                    if (n == 14) Combat.sound(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.9f);
                    if (n == 17) Fx.dustBurst(l, 0xa02020, 0.8f, Combat.muzzle(e, 0.3, -0.3, -0.2), 2, 0.05); // spent shells
                    if (n == 21) Combat.sound(e, SoundEvents.CROSSBOW_LOADING_END, 0.8f, 1.3f);
                    if (n == 27) Combat.sound(e, SoundEvents.IRON_DOOR_CLOSE, 0.7f, 1.9f);
                })
                .anim((p, t, a) -> {
                    reset(p);
                    float up = track(t, 0, 0, .14f, 1, .9f, 1, 1, 0), k = kick(t, .19f, .3f);
                    float open = track(t, 0, 0, .36f, 0, .44f, 1, .72f, 1, .76f, -.15f, .82f, 0);
                    float load = track(t, 0, 0, .44f, 0, .52f, 1, .6f, 1, .7f, 0);
                    hold(p, k * 1.5f);
                    p.rArmX += open * .55f; p.rHandX += open * .95f;
                    p.lArmX = lerp(p.lArmX, -.25f, load); p.lArmZ = -.3f * load; p.lArmY = lerp(p.lArmY, .2f, load);
                    p.headX += open * .25f;
                    p.bodyX += .15f - k * .3f; p.bodyY = -.15f;
                    p.rootZ += k * 1.2f; p.rootY = -.4f * up;
                    stance(p, 1.4f);
                    p.rLegX += k * .3f;
                    raise(p, up);
                }));

        // ---- CHAINGUN: braced hip-fire, rattling sweep ----
        out.add(AttackDef.of("chaingun").range(3, 18).dur(32).cd(45).weight(3).weapon("chaingun")
                .tick((e, t, n) -> {
                    if (n == 2) Combat.sound(e, SoundEvents.CROSSBOW_LOADING_MIDDLE, 0.8f, 0.7f);
                    if (n >= 6 && n <= 26 && n % 2 == 0) {
                        Vec3 mz = Combat.muzzle(e, 1.3, -0.3, -0.05);
                        Combat.hitscan(e, mz, aim(e, mz, t, 0.035), 22, 2.4f, 0, 0xffd060);
                        flash(e, mz, 0xffc050, 1);
                        Fx.dustBurst((ServerLevel) e.level(), 0xc8a040, 0.6f, Combat.muzzle(e, 0.4, -0.6, -0.1), 1, 0.05); // brass
                        Combat.sound(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, 1.5f + e.getRandom().nextFloat() * 0.3f);
                    }
                })
                .anim((p, t, a) -> {
                    reset(p);
                    float up = track(t, 0, 0, .16f, 1, .86f, 1, 1, 0), fire = win(t, .18f, .2f) * (1 - win(t, .8f, .84f));
                    float rattle = fire * Math.abs((float) Math.sin(a * 2.4f)), sweep = track(t, 0, 0, .2f, .1f, .5f, -.12f, .8f, .08f, 1, 0);
                    p.rArmX = -1.25f - rattle * .08f; p.rArmY = -.12f;
                    p.lArmX = -1.15f - rattle * .06f; p.lArmY = .7f;
                    p.rHandX = -.3f - rattle * .15f; p.rHandZ = rattle * .05f;
                    p.bodyX = .14f - rattle * .05f; p.bodyY = sweep; p.headY = -sweep * .6f; p.headX = .08f;
                    p.rootZ = rattle * .3f;
                    stance(p, 1.5f);
                    raise(p, up);
                }));

        // ---- ROCKET LAUNCHER: shoulder-mounted, cheek on sight, rocked back ----
        out.add(AttackDef.of("rocket_launcher").range(5, 26).dur(30).cd(60).weight(2).weapon("rocket_launcher")
                .tick((e, t, n) -> {
                    if (n == 10) {
                        Vec3 mz = Combat.muzzle(e, 1.3, -0.35, 0.35);
                        Combat.shoot(e, ROCKET64, mz, aim(e, mz, t, 0.01), 1.05, 16);
                        flash(e, mz, 0xffa030, 3);
                        Fx.burst((ServerLevel) e.level(), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.6, -0.35, 0.35), 8, 0.2, 0.05); // backblast
                        Combat.sound(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.6f); Combat.sound(e, SoundEvents.BLAZE_SHOOT, 0.8f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    reset(p);
                    float up = track(t, 0, 0, .22f, 1, .8f, 1, 1, 0), k = kick(t, .34f, .35f);
                    p.rArmX = -HALF - .12f - k * .5f; p.rArmY = -.05f;
                    p.lArmX = -1.85f - k * .3f; p.lArmY = .45f; p.lArmZ = -.1f;
                    p.rHandX = -.12f - k * .55f;
                    p.headZ = .22f * up; p.headY = .1f * up; p.headX = .1f - k * .1f;
                    p.bodyX = .12f - k * .35f; p.bodyZ = .06f * up;
                    p.rootZ = k * 1.6f;
                    stance(p, 1.3f); p.rLegX += k * .35f;
                    raise(p, up);
                }));

        // ---- PLASMA RIFLE: steady stream, then venting ----
        out.add(AttackDef.of("plasma_rifle").range(3, 18).dur(32).cd(45).weight(3).weapon("plasma_rifle")
                .tick((e, t, n) -> {
                    ServerLevel l = (ServerLevel) e.level();
                    if (n >= 6 && n <= 24 && n % 3 == 0) {
                        Vec3 mz = Combat.muzzle(e, 1.2, -0.3, 0.05);
                        Combat.shoot(e, PLASMA64, mz, aim(e, mz, t, 0.025), 1.3, 5);
                        Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, mz, 3, 0.05, 0.05); Fx.dustBurst(l, 0x80c0ff, 1f, mz, 2, 0.05);
                        Combat.sound(e, SoundEvents.BLAZE_SHOOT, 0.6f, 1.8f);
                    }
                    if (n == 27) { Combat.sound(e, SoundEvents.LAVA_EXTINGUISH, 0.6f, 1.4f); Fx.burst(l, ParticleTypes.CLOUD, Combat.muzzle(e, 0.6, -0.3, 0.1), 5, 0.1, 0.02); }
                })
                .anim((p, t, a) -> {
                    reset(p);
                    float up = track(t, 0, 0, .16f, 1, .78f, 1, .9f, .7f, 1, 0);
                    float fire = win(t, .18f, .2f) * (1 - win(t, .76f, .78f));
                    float k = fire * Math.abs((float) Math.sin((t - .18f) / .1f * PI)) * .5f;
                    float vent = track(t, 0, 0, .8f, 0, .86f, 1, .95f, 1, 1, 0);
                    hold(p, k);
                    p.rHandZ = vent * .6f; p.rHandX += vent * .25f; p.headX += vent * .3f; p.headY = vent * .2f;
                    p.bodyY = -.08f;
                    stance(p, 1);
                    raise(p, up);
                }));

        // ---- BFG 9000: trembling charge, cataclysmic release ----
        out.add(AttackDef.of("bfg9000").range(6, 26).dur(50).cd(260).weight(2).weapon("bfg")
                .tick((e, t, n) -> {
                    ServerLevel l = (ServerLevel) e.level();
                    Vec3 mz = Combat.muzzle(e, 1.4, -0.3, -0.05);
                    if (n == 0) Combat.sound(e, SoundEvents.BEACON_POWER_SELECT, 1.5f, 0.5f);
                    if (n == 12 || n == 22) Combat.sound(e, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.2f, 0.6f + n * 0.02f);
                    if (n < 30) {
                        float c = n / 30f;
                        Fx.ring(l, mz, 2.2f * (1 - (n % 8) / 8f), 0x70ff58, 1.2f + c, 14);
                        Fx.burst(l, ParticleTypes.GLOW, mz, 1 + n / 6, 0.15 + c * 0.2, 0.01);
                    }
                    if (n == 30) {
                        Combat.shoot(e, BFG64, mz, aim(e, mz, t, 0), 0.7, 34);
                        Fx.burst(l, ParticleTypes.FLASH, mz, 1, 0, 0); Fx.dustBurst(l, 0xa0ff80, 2.5f, mz, 20, 0.4);
                        Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 1.5f, 0.5f); Combat.sound(e, SoundEvents.BEACON_DEACTIVATE, 1.5f, 1.4f);
                        Combat.lunge(e, -0.35, 0.08);
                    }
                })
                .anim((p, t, a) -> {
                    reset(p);
                    float up = track(t, 0, 0, .12f, 1, .82f, 1, 1, 0), charge = win(t, .1f, .6f) * (1 - win(t, .6f, .62f));
                    float shake = (float) Math.sin(a * 3.1f) * .05f * charge, k = kick(t, .6f, .3f);
                    p.rArmX = -1.3f + shake - k * .7f; p.rArmY = -.15f;
                    p.lArmX = -1.15f - shake - k * .6f; p.lArmY = .8f; p.lArmZ = -.1f;
                    p.rHandX = -.25f + shake - k * .5f;
                    p.bodyX = lerp(.05f, -.12f, charge) - k * .55f + shake * .5f;
                    p.headX = .25f * charge - k * .3f; p.headZ = shake;
                    p.rootY = -.5f * charge; p.rootZ = k * 2.4f; p.rootRoll = shake * .3f;
                    stance(p, 1.6f); p.rLegX += k * .55f; p.lLegX -= k * .2f;
                    raise(p, up);
                }));

        // ---- UNMAKER (1 key): single demonic laser, deliberate pulses ----
        out.add(AttackDef.of("unmaker_single").range(3, 24).dur(26).cd(40).weight(3).weapon("unmaker1")
                .tick((e, t, n) -> {
                    if (n == 7 || n == 13 || n == 19) {
                        Vec3 mz = Combat.muzzle(e, 1.3, -0.3, 0.05);
                        laser(e, mz, aim(e, mz, t, 0.004), 6, 1, 1);
                        Combat.sound(e, SoundEvents.BLAZE_SHOOT, 0.7f, 0.55f); Combat.sound(e, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 0.5f);
                    }
                })
                .anim((p, t, a) -> {
                    reset(p);
                    float up = track(t, 0, 0, .2f, 1, .84f, 1, 1, 0);
                    float k = kick(t, .27f, .18f) + kick(t, .5f, .18f) + kick(t, .73f, .18f);
                    hold(p, k * .6f);
                    p.headX += .05f; p.bodyY = -.05f;
                    stance(p, .9f);
                    raise(p, up);
                }));

        // ---- UNMAKER (2 keys): spread triple beam, two sweeping volleys ----
        out.add(AttackDef.of("unmaker_triple").range(3, 22).dur(32).cd(70).weight(2).weapon("unmaker2")
                .tick((e, t, n) -> {
                    if (n == 9 || n == 20) {
                        Vec3 mz = Combat.muzzle(e, 1.3, -0.3, 0.05);
                        for (Vec3 d : tripleDirs(e, mz, t, 11)) laser(e, mz, d, 6, 2, 2);
                        Combat.sound(e, SoundEvents.ENDER_DRAGON_SHOOT, 0.9f, 1.3f); Combat.sound(e, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    reset(p);
                    float up = track(t, 0, 0, .2f, 1, .86f, 1, 1, 0);
                    float k = kick(t, .29f, .25f) + kick(t, .63f, .25f);
                    float sweep = track(t, 0, 0, .25f, -.14f, .45f, .16f, .6f, .16f, .75f, -.05f, 1, 0);
                    hold(p, k);
                    p.bodyY = sweep; p.headY = -sweep * .4f;
                    p.rootZ += k * .6f; p.rootY = -.2f * up;
                    stance(p, 1.25f);
                    raise(p, up);
                }));

        // ---- UNMAKER (3 keys): overcharged rapid-fire triple stream; pushed back by the output ----
        out.add(AttackDef.of("unmaker_overcharge").range(3, 22).dur(42).cd(160).weight(2).weapon("unmaker3")
                .when((e, t) -> t.getMaxHealth() >= 30 || Combat.around(e, t.position(), 6).size() >= 3 || e.getHealth() < e.getMaxHealth() * 0.5f)
                .tick((e, t, n) -> {
                    ServerLevel l = (ServerLevel) e.level();
                    Vec3 mz = Combat.muzzle(e, 1.3, -0.3, 0.05);
                    if (n == 0) Combat.sound(e, SoundEvents.WARDEN_SONIC_CHARGE, 1f, 1.4f);
                    if (n < 9) { Fx.ring(l, mz, 0.9f - n * 0.09f, KEY_RGB[n % 3], 1f, 10); Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, mz, 2, 0.15, 0.05); }
                    if (n >= 9 && n <= 33 && n % 2 == 1) {
                        for (Vec3 d : tripleDirs(e, mz, t, 6)) laser(e, mz, d, 4.5f, 3, 3);
                        if (n % 4 == 1) Combat.sound(e, SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.8f);
                        Combat.sound(e, SoundEvents.BLAZE_SHOOT, 0.5f, 0.9f);
                    }
                    if (n == 35) { Combat.sound(e, SoundEvents.LAVA_EXTINGUISH, 0.8f, 0.9f); Fx.burst(l, ParticleTypes.LARGE_SMOKE, mz, 6, 0.2, 0.03); }
                })
                .anim((p, t, a) -> {
                    reset(p);
                    float up = track(t, 0, 0, .18f, 1, .88f, 1, 1, 0);
                    float fire = win(t, .2f, .23f) * (1 - win(t, .8f, .84f));
                    float buzz = (float) Math.sin(a * 2.9f) * (.03f + .05f * fire), push = track(t, 0, 0, .22f, 0, .8f, 1, 1, 0);
                    float vent = track(t, 0, 0, .82f, 0, .88f, 1, .96f, 1, 1, 0);
                    hold(p, fire * .35f + buzz);
                    p.rHandX += vent * -.5f; p.rHandZ = vent * .3f;
                    p.bodyX -= push * .15f; p.headX += .15f * fire; p.headZ = buzz;
                    p.rootZ += push * 1.6f; p.rootY = -.45f * up; p.rootRoll = buzz * .4f;
                    stance(p, 1.7f); p.rLegX += push * .25f;
                    raise(p, up);
                }));
    }

    // =====================================================================================================
    // server helpers
    // =====================================================================================================
    static Vec3 aim(SlayerEntity e, Vec3 from, LivingEntity t, double spread) {
        Random r = new Random(e.getRandom().nextLong());
        if (t == null) { Vec3 d = e.getViewVector(1f); return d.add(r.nextGaussian() * spread, r.nextGaussian() * spread, r.nextGaussian() * spread).normalize(); }
        return Combat.dirTo(from, t, spread, r);
    }

    static void pellets(SlayerEntity e, Vec3 from, LivingEntity t, int n, double spread, float dmg, double range, int rgb) {
        for (int i = 0; i < n; i++) Combat.hitscan(e, from, aim(e, from, t, spread), range, dmg, 0, rgb);
    }

    static void flash(SlayerEntity e, Vec3 mz, int rgb, int size) {
        ServerLevel l = (ServerLevel) e.level();
        Fx.burst(l, ParticleTypes.FLAME, mz, 2 + size, 0.05, 0.02);
        Fx.burst(l, ParticleTypes.SMOKE, mz, 1 + size, 0.06, 0.01);
        Fx.dustBurst(l, rgb, 0.8f + size * 0.3f, mz, 2 + size, 0.06);
    }

    static void sparkAt(SlayerEntity e, LivingEntity f, int n) {
        Fx.sparks((ServerLevel) e.level(), f.position().add(0, f.getBbHeight() * 0.6, 0), n);
    }

    static Vec3 yawRot(Vec3 d, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(d.x * c - d.z * s, d.y, d.x * s + d.z * c);
    }

    /** Main target + up to two other foes in front (the Unmaker's 3-key auto-split); missing ones become a fixed +-spread fan. */
    static List<Vec3> tripleDirs(SlayerEntity e, Vec3 mz, LivingEntity t, double spreadDeg) {
        Vec3 main = aim(e, mz, t, 0.004);
        List<Vec3> out = new ArrayList<>(); out.add(main);
        List<LivingEntity> others = Combat.cone(e, 24, 35);
        others.remove(t);
        others.sort(Comparator.comparingDouble(e::distanceToSqr));
        for (LivingEntity o : others) { if (out.size() >= 3) break; if (e.hasLineOfSight(o)) out.add(aim(e, mz, o, 0.004)); }
        if (out.size() < 2) out.add(yawRot(main, spreadDeg));
        if (out.size() < 3) out.add(yawRot(main, -spreadDeg));
        return out;
    }

    /** Unmaker laser: hitscan with pierce + layered glow core; tier escalates thickness, colour heat and sparks. */
    static void laser(SlayerEntity e, Vec3 from, Vec3 dir, float dmg, int pierce, int tier) {
        ServerLevel l = (ServerLevel) e.level();
        double range = 26;
        Combat.hitscan(e, from, dir, range, dmg, pierce, LASER);
        Vec3 end = l.clip(new ClipContext(from, from.add(dir.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getLocation();
        int core = tier == 1 ? 0xff7070 : tier == 2 ? 0xffa0a0 : 0xffe0f0;
        Fx.beam(l, from, end, core, 0.7f + tier * 0.35f, tier == 3 ? 0.45 : 0.6);
        if (tier >= 2) Fx.beam(l, from, end, KEY_RGB[tier - 1], 0.5f, 1.4);
        if (tier == 3) { Vec3 d = end.subtract(from); for (double s = 0; s < 1; s += 0.12) Fx.burst(l, ParticleTypes.END_ROD, from.add(d.scale(s)), 1, 0.05, 0.005); }
        Fx.dustBurst(l, LASER, 1f + tier * 0.3f, from, 2 + tier, 0.05);
        Fx.burst(l, ParticleTypes.LAVA, end, tier, 0.1, 0.05);
        Fx.dustBurst(l, core, 1.5f, end, 3 * tier, 0.15);
    }

    // =====================================================================================================
    // animation helpers
    // =====================================================================================================
    private static void reset(Pose p) {
        p.bodyX = p.bodyY = p.bodyZ = p.bodyOffX = p.bodyOffY = p.bodyOffZ = 0;
        p.headX = p.headY = p.headZ = 0;
        p.rArmX = p.rArmY = p.rArmZ = p.lArmX = p.lArmY = p.lArmZ = 0;
        p.rHandX = p.rHandY = p.rHandZ = p.lHandX = p.lHandY = p.lHandZ = 0;
        p.rLegX = p.rLegY = p.rLegZ = p.lLegX = p.lLegY = p.lLegZ = 0;
        p.rootX = p.rootY = p.rootZ = p.rootYaw = p.rootPitch = p.rootRoll = 0;
    }

    /** two-handed shouldered gun; rec = recoil amount (pushes muzzle up, torso and root back) */
    private static void hold(Pose p, float rec) {
        p.rArmX = -HALF - rec * .35f; p.rArmY = -.1f;
        p.lArmX = -HALF + .12f - rec * .3f; p.lArmY = .6f;
        p.rHandX = -rec * .45f;
        p.bodyX = .08f - rec * .22f; p.headX = .06f - rec * .08f; p.headZ = .06f;
        p.rootZ = rec * .8f;
    }

    /** braced legs: left foot forward, right back, spread by w */
    private static void stance(Pose p, float w) {
        p.rLegX = .22f * w; p.lLegX = -.28f * w; p.rLegZ = .07f * w; p.lLegZ = -.07f * w;
    }

    /** blend arms from the low-ready carry (r=0) to the current aimed pose (r=1) */
    private static void raise(Pose p, float r) {
        p.rArmX = lerp(-.45f, p.rArmX, r); p.lArmX = lerp(-.6f, p.lArmX, r);
        p.rHandX = lerp(-.9f, p.rHandX, r);
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float walk) {
        // grim low-ready carry, head slightly down; arms barely swing
        p.rArmX = -.45f + p.rArmX * .15f; p.rArmY = -.1f;
        p.lArmX = -.6f + p.lArmX * .15f; p.lArmY = .55f;
        p.rHandX = -.9f;
        p.headX += .1f;
    }

    // =====================================================================================================
    // model
    // =====================================================================================================
    @Override
    public void buildModel(Rig rig) {
        Mat armor = Mat.of(ARMOR).noise(.1f).bevel(.28f).decal(Doom64Marine::plates);
        Mat armorDk = Mat.of(ARMOR_DK).noise(.12f).bevel(.3f);
        Mat suit = Mat.of(SUIT).noise(.14f).bevel(.15f);
        Mat leather = Mat.of(LEATHER).noise(.18f).bevel(.2f);
        Mat metal = Mat.of(METAL).noise(.06f).bevel(.25f);
        Mat gun = Mat.of(GUNMETAL).noise(.07f).bevel(.3f);
        Mat visor = Mat.of(VISOR).glow();

        // torso
        rig.box(rig.torso, suit, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, armor, -4, -12, -2, 8, 7, 4, .6f);                 // chest plate
        rig.box(rig.torso, armorDk, -3, -5, -2.9f, 6, 2, 1);                  // abdomen plate
        rig.box(rig.torso, leather, -4, -3, -2, 8, 2, 4, .45f);               // belt
        rig.box(rig.torso, leather, -3.5f, -3.2f, -3.3f, 2, 2, 1);            // pouches
        rig.box(rig.torso, leather, 1.5f, -3.2f, -3.3f, 2, 2, 1);
        rig.box(rig.torso, armorDk, -3, -11.5f, 2.5f, 6, 7, 2);               // backpack
        rig.box(rig.torso, metal, -2, -12, 4.5f, 4, 2, 1);                    // pack vent
        // head
        rig.box(rig.head, armor, -4, -8, -4, 8, 8, 8);
        rig.box(rig.head, armorDk, -1, -8.8f, -4.4f, 2, 1, 9);                // crest ridge
        rig.box(rig.head, visor, -3, -5.5f, -4.6f, 6, 2, 1);                  // visor
        rig.box(rig.head, armorDk, -3, -3, -4.6f, 6, 3, 1);                   // jaw guard
        rig.box(rig.head, metal, -4.7f, -6, -1.5f, 1, 3, 3);                  // ear pods
        rig.box(rig.head, metal, 3.7f, -6, -1.5f, 1, 3, 3);
        // arms
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, suit, -2, -2, -2, 4, 12, 4);
            rig.box(arm, armor, s < 0 ? -3.5f : -1.5f, -3, -3, 5, 4, 6);     // pauldron
            rig.box(arm, armorDk, -2, 5, -2, 4, 5, 4, .45f);                  // gauntlet
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, suit, -2, 0, -2, 4, 12, 4);
            rig.box(leg, armor, -2, 0, -2, 4, 5, 4, .4f);                     // thigh plate
            rig.box(leg, armorDk, -1.5f, 5.5f, -2.9f, 3, 2, 1);               // knee
            rig.box(leg, armorDk, -2, 9, -3, 4, 3, 5, .3f);                   // boot
        }

        // ---- weapons (gun space: origin fist, barrel -z, y down) ----
        PartDefinition w;
        w = rig.weapon(rig.rHand, "pistol");
        rig.box(w, gun, -1, -3.5f, -7, 2, 2, 8);
        rig.box(w, leather, -1, -1.5f, -1.5f, 2, 4, 2);
        rig.box(w, metal, -.5f, -3.2f, -8, 1, 1, 1);
        rig.box(w, gun, -.5f, -.5f, -3.5f, 1, 1, 2);

        Mat wood = Mat.of(0x4a3020).noise(.2f).bevel(.2f);
        w = rig.weapon(rig.rHand, "shotgun");
        rig.box(w, gun, -1.5f, -4, -5, 3, 3, 7);
        rig.box(w, metal, -.5f, -4, -17, 1, 1, 12);
        rig.box(w, gun, -.5f, -3, -15, 1, 1, 10);
        rig.box(w, wood, -1, -3.5f, -11, 2, 2, 4);
        rig.box(w, wood, -1, -3, 2, 2, 3, 6);
        rig.box(w, wood, -1, -1, -1.5f, 2, 3, 2);

        w = rig.weapon(rig.rHand, "super_shotgun");
        rig.box(w, metal, -1.5f, -4, -15, 1, 1, 12);
        rig.box(w, metal, .5f, -4, -15, 1, 1, 12);
        rig.box(w, gun, -1.5f, -4.5f, -4, 3, 3, 5);
        rig.box(w, gun, -1.5f, -4.4f, -13, 3, 1, 1);
        rig.box(w, wood, -1, -3.5f, 1, 2, 3, 7);
        rig.box(w, wood, -1, -1.5f, -1, 2, 3, 2);

        w = rig.weapon(rig.rHand, "chaingun");
        rig.box(w, gun, -2, -5, -6, 4, 4, 8);
        for (int i = 0; i < 4; i++) rig.box(w, metal, i % 2 == 0 ? -1.5f : .5f, i < 2 ? -4.5f : -2.5f, -18, 1, 1, 12);
        rig.box(w, gun, -2, -5, -17, 4, 4, 1);
        rig.box(w, gun, -2, -5, -11, 4, 4, 1);
        rig.box(w, metal, -.5f, -7, -5, 1, 2, 5);
        rig.box(w, armorDk, -5, -4, -4, 3, 3, 4);
        rig.box(w, leather, -1, -1, -1, 2, 3, 2);

        w = rig.weapon(rig.rHand, "rocket_launcher");
        rig.box(w, armorDk, -2, -6, -14, 4, 4, 20);
        rig.box(w, gun, -2.5f, -6.5f, -15, 5, 5, 2);
        rig.box(w, gun, -2.5f, -6.5f, 5, 5, 5, 2);
        rig.box(w, Mat.of(0xff8030).glow(), -1, -5, -15.3f, 2, 2, 1);
        rig.box(w, metal, 1.5f, -8, -6, 1, 2, 3);
        rig.box(w, leather, -1, -2, -2, 2, 4, 2);
        rig.box(w, leather, -1, -2, -9, 2, 3, 2);

        Mat plasmaGlow = Mat.of(0x60b0ff).glow();
        w = rig.weapon(rig.rHand, "plasma_rifle");
        rig.box(w, Mat.of(0x4a5870).noise(.06f).bevel(.3f), -1.5f, -5, -12, 3, 4, 14);
        for (int i = 0; i < 3; i++) rig.box(w, plasmaGlow, -2, -5.5f, -11 + i * 3, 4, 5, 1);
        rig.box(w, plasmaGlow, -1, -4.5f, -14, 2, 2, 2);
        rig.box(w, metal, -.5f, -6, -10, 1, 1, 9);
        rig.box(w, gun, -1.5f, -4, 2, 3, 3, 4);
        rig.box(w, gun, -1, -1, -1, 2, 3, 2);

        Mat bfgGlow = Mat.of(0x70ff58).glow();
        w = rig.weapon(rig.rHand, "bfg");
        rig.box(w, Mat.of(0x3c4640).noise(.08f).bevel(.3f), -3.5f, -7, -12, 7, 6, 16);
        rig.box(w, bfgGlow, -2.5f, -6, -13, 5, 4, 1);
        rig.box(w, bfgGlow, -4, -6, -9, 1, 3, 8);
        rig.box(w, bfgGlow, 3, -6, -9, 1, 3, 8);
        rig.box(w, metal, -.5f, -9, -7, 1, 2, 6);
        rig.box(w, metal, -3, -8, -15, 1, 1, 3);
        rig.box(w, metal, 2, -8, -15, 1, 1, 3);
        rig.box(w, gun, -2.5f, -6, 4, 5, 4, 3);
        rig.box(w, gun, -1, -1, -2, 2, 3, 2);

        // Unmaker x3: same demonic bone/flesh frame, 1/2/3 demon-key artifacts lit, overcharge fins on the 3-key form
        Mat flesh = Mat.of(FLESH).noise(.25f).bevel(.15f), bone = Mat.of(BONE).noise(.15f).bevel(.25f);
        Mat socket = Mat.of(0x241a1a).noise(.1f), redGlow = Mat.of(LASER).glow(), hot = Mat.of(0xffb0a0).glow();
        Mat[] keys = {Mat.of(KEY_RGB[0]).glow(), Mat.of(KEY_RGB[1]).glow(), Mat.of(KEY_RGB[2]).glow()};
        for (int lvl = 1; lvl <= 3; lvl++) {
            w = rig.weapon(rig.rHand, "unmaker" + lvl);
            rig.box(w, flesh, -1.5f, -5, -10, 3, 4, 12);
            rig.box(w, bone, -.5f, -6.5f, -11, 1, 2, 11);                     // spine
            rig.box(w, bone, -2, -5.5f, -15, 1, 1, 5);                        // jaw prongs
            rig.box(w, bone, 1, -5.5f, -15, 1, 1, 5);
            rig.box(w, bone, -.5f, -2, -14, 1, 1, 4);
            rig.box(w, lvl == 3 ? hot : redGlow, -1, -4.5f, -12, 2, 2, 2);    // emitter
            rig.box(w, bone, -1, -1, -1, 2, 3, 2);                            // grip
            rig.box(w, flesh, -1, -4.5f, 2, 2, 2, 3);                         // tail vertebra
            for (int k = 0; k < 3; k++) rig.box(w, k < lvl ? keys[k] : socket, -1, -7.8f, -8 + k * 3, 2, 2, 2);
            if (lvl == 3) {
                rig.box(w, redGlow, -2.6f, -5, -7, 1, 3, 4);
                rig.box(w, redGlow, 1.6f, -5, -7, 1, 3, 4);
            }
        }
    }

    /** armour panel seams + a horizontal rib on front faces */
    private static void plates(Mat m, Mat.Img g, int x, int y, int w, int h, Mat.Face f) {
        if (f != Mat.Face.FRONT && f != Mat.Face.BACK || w < 4 || h < 4) return;
        int seam = Mat.mix(m.base, 0x000000, .45f), hi = Mat.mix(m.base, 0x9ac0b0, .25f);
        for (int i = 1; i < w - 1; i++) { g.px(x + i, y + h / 2, seam); g.px(x + i, y + h / 2 - 1, hi); }
        if (w >= 6) for (int j = 1; j < h - 1; j++) g.px(x + w / 2, y + j, seam);
    }
}
