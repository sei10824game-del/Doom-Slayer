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
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** The UAC Marine of DOOM 3 (2004): heavy, deliberate, flashlight-lit, Soul Cube. */
public class Doom3Marine extends SlayerKit {
    private static final String SOULS = "d3marine_souls";

    // ---------------- projectiles ----------------
    static final ProjStyle GRENADE = ProjStyle.make().color(0x56603a).size(0.28f).life(60).explode(3.5f).gravity(0.05).bounce()
            .trail(ParticleTypes.SMOKE)
            .onTick(p -> { if (p.age2 % 15 == 0) Fx.sound((ServerLevel) p.level(), p.position(), SoundEvents.LEVER_CLICK, 0.6f, 2f); });

    static final ProjStyle BFG = ProjStyle.make().color(0x66ff66).size(1.4f).life(90).explode(6f).trail(ParticleTypes.HAPPY_VILLAGER)
            .onTick(p -> {
                // BFG tracer rays: tendrils lash every foe near the ball
                if (p.age2 % 3 != 0 || !(p.getOwner() instanceof SlayerEntity o)) return;
                ServerLevel l = (ServerLevel) p.level();
                for (LivingEntity f : Combat.around(o, p.position(), 7)) {
                    Fx.beam(l, p.position(), f.position().add(0, f.getBbHeight() * 0.5, 0), 0x7dff7d, 0.8f, 0.35);
                    Combat.damage(o, f, 3f);
                }
            })
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 6f);
                Fx.dustBurst(l, 0x66ff66, 2f, pos, 40, 2.5);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 2f, 0.6f);
                if (p.getOwner() instanceof SlayerEntity o) for (LivingEntity f : Combat.around(o, pos, 10))
                    Fx.beam(l, pos, f.position().add(0, f.getBbHeight() * 0.5, 0), 0x99ff99, 1f, 0.3);
            });

    /** Soul Cube: spectral skull that homes in, drains the foe and pours the life back into the marine. */
    static final ProjStyle SOUL = ProjStyle.make().color(0xff6a2a).size(0.7f).life(140).homing(0.3f).trail(ParticleTypes.SOUL_FIRE_FLAME)
            .onTick(p -> Fx.burst((ServerLevel) p.level(), ParticleTypes.SCULK_SOUL, p.position(), 1, 0.15, 0.01))
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.burst(l, ParticleTypes.SOUL, pos, 25, 0.5, 0.05);
                Fx.sound(l, pos, SoundEvents.SOUL_ESCAPE, 2f, 0.6f);
                if (hit != null && p.getOwner() instanceof SlayerEntity o) {
                    o.heal(p.damage * 0.75f);
                    Fx.beam(l, pos, o.position().add(0, 1.2, 0), 0xff5522, 1.2f, 0.3);
                    Fx.burst(l, ParticleTypes.HEART, o.position().add(0, 2, 0), 4, 0.4, 0);
                }
            });

    public Doom3Marine() {
        super("doom3_marine", "Doom Marine (UAC Marine)", "ドゥームマリーン（UAC海兵隊員）", "Doom 3");
        maxHealth = 100; speed = 0.25; armor = 12; knockbackRes = 0.5;
        preferredRange = 9; dmgMult = 1f;
        eggPrimary = 0x55636e; eggSecondary = 0xfff1c0;
        idleWeaponR = "machinegun";
    }

    // ---------------- server helpers ----------------
    static ServerLevel lvl(SlayerEntity e) { return (ServerLevel) e.level(); }
    static Vec3 aimDir(SlayerEntity e, LivingEntity t, double spread) {
        return t != null ? Combat.dirTo(e, t, spread) : e.getViewVector(1f);
    }
    static Vec3 gun(SlayerEntity e) { return Combat.muzzle(e, 0.8, -0.3, 0); }
    static void fire(SlayerEntity e, LivingEntity t, double spread, float dmg, int tracer) {
        Combat.hitscan(e, gun(e), aimDir(e, t, spread), 32, dmg, 0, tracer);
        Fx.burst(lvl(e), ParticleTypes.SMOKE, gun(e), 2, 0.05, 0.01);
    }
    static void punch(SlayerEntity e, float range, float dmg, double kb, double up) {
        for (LivingEntity f : Combat.cone(e, range, 55)) {
            Combat.damage(e, f, dmg); Combat.knock(f, e.position(), kb, up);
            Fx.sparks(lvl(e), f.position().add(0, f.getBbHeight() * 0.6, 0), 4);
        }
    }
    static int souls(SlayerEntity e) { return e.getPersistentData().getInt(SOULS); }

    @Override
    public void passive(SlayerEntity e) {
        // count kills for the Soul Cube
        LivingEntity t = e.atkTarget;
        if (t != null && !t.isAlive() && t.getLastHurtByMob() == e) {
            e.getPersistentData().putInt(SOULS, souls(e) + 1); e.atkTarget = null;
        }
        // shoulder/gun flashlight cone in the dark or in combat
        if (e.tickCount % 3 != 0) return;
        ServerLevel l = lvl(e);
        if (e.getTarget() == null && l.getMaxLocalRawBrightness(e.blockPosition()) > 7) return;
        Vec3 look = e.getViewVector(1f), o = Combat.muzzle(e, 0.4, 0.35, 0.15);
        for (int i = 0; i < 3; i++) {
            double d = 1.2 + e.getRandom().nextDouble() * 5, s = d * 0.14;
            Vec3 q = o.add(look.scale(d));
            l.sendParticles(Fx.dust(0xfff4c8, 0.5f), q.x, q.y, q.z, 1, s, s, s, 0);
        }
    }

    // ---------------- anim helpers ----------------
    /** blend from the current (idle/locomotion) arms to a two-handed aim; heavy staggered stance. */
    static void aim(Pose p, float raise, float pitch, float recoil) {
        float rx = p.rArmX, ry = p.rArmY, rz = p.rArmZ, lx = p.lArmX, ly = p.lArmY, lz = p.lArmZ;
        p.aim2(pitch, recoil * raise);
        p.rArmX = lerp(rx, p.rArmX, raise); p.rArmY = lerp(ry, p.rArmY, raise); p.rArmZ = lerp(rz, p.rArmZ, raise);
        p.lArmX = lerp(lx, p.lArmX, raise); p.lArmY = lerp(ly, p.lArmY, raise); p.lArmZ = lerp(lz, p.lArmZ, raise);
        p.rHandX = -recoil * 0.45f;
        p.rLegX = lerp(p.rLegX, 0.3f, raise); p.lLegX = lerp(p.lLegX, -0.3f, raise);
        p.rootY -= 0.3f * raise; p.headX += 0.12f * raise; p.headY -= 0.1f * raise;
        p.rootZ += recoil * 0.4f;
    }
    static float raise(float t, float up, float down) { return smooth(win(t, 0, up)) * (1 - smooth(win(t, down, 1))); }

    // ---------------- attacks ----------------
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // FISTS: alternating jab (right) / cross (left)
        out.add(AttackDef.of("fists").range(0, 2.8f).cd(16).dur(14).weapon("none").tick((e, t, n) -> {
            if (n == 4 || n == 9) { punch(e, 2.8f, 6, 0.3, 0.1); Combat.sound(e, SoundEvents.PLAYER_ATTACK_STRONG, 1f, n == 4 ? 1.1f : 0.9f); }
        }).anim((p, t, a) -> {
            p.rArmX = track(t, 0, -0.7f, 0.18f, -0.5f, 0.29f, -1.7f, 0.45f, -0.7f, 1, -0.6f);
            p.rArmY = track(t, 0, 0, 0.29f, 0.3f, 0.5f, 0);
            p.lArmX = track(t, 0, -1.0f, 0.5f, -0.8f, 0.64f, -1.75f, 0.82f, -1.0f, 1, -0.7f);
            p.lArmY = track(t, 0, 0, 0.64f, -0.3f, 0.85f, 0);
            p.bodyY = track(t, 0, 0, 0.18f, 0.3f, 0.29f, -0.45f, 0.5f, -0.1f, 0.64f, 0.5f, 0.85f, 0.1f, 1, 0);
            p.bodyX = 0.12f + 0.1f * (bump(win(t, 0.2f, 0.4f)) + bump(win(t, 0.55f, 0.75f)));
            p.headX += 0.15f; p.headY = -p.bodyY * 0.6f;
            p.lLegX = -0.35f; p.rLegX = 0.3f; p.rootY = -0.3f - 0.15f * bump(win(t, 0.2f, 0.75f));
            p.rootZ = -0.6f * (bump(win(t, 0.2f, 0.4f)) + bump(win(t, 0.55f, 0.75f)));
        }));

        // COLD PUNCH: the knockout haymaker, leaves the foe dazed
        out.add(AttackDef.of("cold_punch").range(0, 2.8f).cd(60).dur(20).weapon("none").tick((e, t, n) -> {
            if (n == 4) Combat.sound(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 0.6f);
            if (n == 11) {
                for (LivingEntity f : Combat.cone(e, 3f, 50)) {
                    Combat.damage(e, f, 14); Combat.knock(f, e.position(), 1.2, 0.45);
                    f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                    f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                    Fx.burst(lvl(e), ParticleTypes.CRIT, f.getEyePosition(), 12, 0.3, 0.3);
                }
                Combat.sound(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.7f);
                Combat.sound(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.8f);
            }
        }).anim((p, t, a) -> {
            p.bodyY = track(t, 0, 0, 0.45f, 0.85f, 0.55f, -0.7f, 0.75f, -0.6f, 1, 0);
            p.bodyX = track(t, 0, 0.05f, 0.45f, -0.15f, 0.55f, 0.35f, 0.75f, 0.3f, 1, 0.05f);
            p.rArmX = trackLin(t, 0, -0.6f, 0.45f, 0.3f, 0.55f, -2.0f, 0.75f, -1.8f, 1, -0.5f);
            p.rArmZ = track(t, 0, 0.1f, 0.45f, 0.7f, 0.55f, 0.05f, 1, 0.1f);
            p.lArmX = track(t, 0, -1.0f, 0.45f, -1.3f, 0.55f, -0.4f, 1, -0.8f);
            p.lArmZ = track(t, 0, 0, 0.55f, -0.4f, 1, 0);
            p.rootY = track(t, 0, 0, 0.45f, -0.7f, 0.55f, 0.4f, 0.75f, 0, 1, 0);
            p.rootZ = track(t, 0, 0, 0.45f, 0.5f, 0.55f, -1.5f, 0.8f, -1.2f, 1, 0);
            p.rLegX = track(t, 0, 0.2f, 0.45f, 0.55f, 0.55f, 0.1f, 1, 0.2f);
            p.lLegX = track(t, 0, -0.2f, 0.45f, -0.5f, 0.6f, -0.3f, 1, -0.1f);
            p.headY = -p.bodyY * 0.7f; p.headX += 0.1f;
        }));

        // FLASHLIGHT BASH: overhead clubbing swing with the heavy UAC torch
        out.add(AttackDef.of("flashlight_bash").range(0, 3f).cd(40).dur(16).weapon("flashlight").tick((e, t, n) -> {
            if (n == 3) Combat.sound(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 1.3f);
            if (n == 8) {
                punch(e, 3f, 9, 0.6, 0.2);
                Vec3 c = Combat.muzzle(e, 1.4, 0, 0.2);
                Fx.dustBurst(lvl(e), 0xfff4c8, 1.2f, c, 10, 0.4);
                Combat.sound(e, SoundEvents.LANTERN_HIT, 1.2f, 0.8f);
                Combat.sound(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 1f);
            }
        }).anim((p, t, a) -> {
            p.rArmX = trackLin(t, 0, -0.6f, 0.35f, -3.0f, 0.5f, -0.8f, 0.7f, -0.6f, 1, -0.6f);
            p.rHandX = track(t, 0, 0, 0.35f, -0.7f, 0.5f, 0.5f, 1, 0);
            p.rArmZ = track(t, 0, 0, 0.35f, 0.2f, 0.5f, -0.1f, 1, 0);
            p.lArmX = track(t, 0, -0.5f, 0.35f, -1.1f, 0.6f, -0.6f, 1, -0.4f);
            p.bodyX = track(t, 0, 0, 0.35f, -0.25f, 0.5f, 0.45f, 0.7f, 0.35f, 1, 0);
            p.bodyY = track(t, 0, 0, 0.35f, 0.3f, 0.5f, -0.2f, 1, 0);
            p.rootZ = -1.4f * bump(win(t, 0.3f, 0.8f));
            p.rootY = -0.5f * bump(win(t, 0.35f, 0.9f));
            p.lLegX = track(t, 0, 0, 0.45f, -0.5f, 0.8f, -0.4f, 1, 0);
            p.rLegX = track(t, 0, 0, 0.45f, 0.35f, 1, 0);
            p.headX += 0.2f * bump(win(t, 0.3f, 0.8f));
        }));

        // CHAINSAW: rev, lunge in and grind
        out.add(AttackDef.of("chainsaw").range(0, 3.5f).cd(80).dur(30).weapon("chainsaw").moving().tick((e, t, n) -> {
            if (n == 0) Combat.sound(e, SoundEvents.MINECART_RIDING, 1f, 1.8f);
            if (n == 4) Combat.lunge(e, 0.5, 0.05);
            if (n >= 6 && n <= 26 && n % 3 == 0) {
                for (LivingEntity f : Combat.cone(e, 2.6, 45)) {
                    Combat.damage(e, f, 4); Combat.knock(f, e.position(), 0.05, 0);
                    Fx.dustBurst(lvl(e), 0x8a0a0a, 1f, f.position().add(0, f.getBbHeight() * 0.5, 0), 6, 0.25);
                }
                Fx.sparks(lvl(e), Combat.muzzle(e, 1.5, -0.2, -0.3), 3);
                Combat.sound(e, SoundEvents.SHEEP_SHEAR, 0.8f, 0.6f + e.getRandom().nextFloat() * 0.3f);
            }
        }).anim((p, t, a) -> {
            float r = raise(t, 0.12f, 0.88f), grind = win(t, 0.2f, 0.87f) > 0 && t < 0.87f ? 1 : 0;
            aim(p, r, 0.45f, 0);
            float buzz = (float) Math.sin(a * 3.1f) * 0.05f * r;
            p.rArmX += buzz; p.lArmX -= buzz;
            p.lArmX += track(t, 0, 0, 0.05f, 0.5f, 0.12f, 0);   // rev cord yank
            p.bodyY = grind * (float) Math.sin(t * PI * 7) * 0.28f;
            p.bodyX = 0.15f * r + buzz * 0.5f;
            p.rootZ = -1.8f * bump(win(t, 0.1f, 0.6f));
            p.rLegX = 0.45f * r; p.lLegX = -0.45f * r; p.rootY = -0.45f * r;
            p.headY = -p.bodyY * 0.5f;
        }));

        // PISTOL: 3 aimed shots
        out.add(AttackDef.of("pistol").range(2, 18).cd(25).dur(18).weapon("pistol").tick((e, t, n) -> {
            if (n == 3 || n == 9 || n == 15) { fire(e, t, 0.02, 5, 0xffe9a0); Combat.gunSound(e, 1.7f); }
        }).anim((p, t, a) -> {
            float k = kick(t, 0.17f, 0.2f) + kick(t, 0.5f, 0.2f) + kick(t, 0.83f, 0.16f);
            aim(p, raise(t, 0.12f, 0.92f), 0, k * 0.8f);
            p.headZ = 0.1f; p.bodyY = -0.15f;
        }));

        // SHOTGUN: boom, then pump
        out.add(AttackDef.of("shotgun").range(1.5f, 10).cd(35).dur(24).weapon("shotgun").tick((e, t, n) -> {
            if (n == 4) {
                Vec3 m = gun(e);
                for (int i = 0; i < 13; i++) Combat.hitscan(e, m, aimDir(e, t, 0.09), 14, 2.2f, 0, 0xffd080);
                Fx.burst(lvl(e), ParticleTypes.FLAME, m, 6, 0.1, 0.05); Fx.burst(lvl(e), ParticleTypes.SMOKE, m, 6, 0.1, 0.02);
                Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 0.7f, 1.8f);
            }
            if (n == 10) Combat.sound(e, SoundEvents.PISTON_CONTRACT, 0.8f, 1.6f);
            if (n == 15) Combat.sound(e, SoundEvents.PISTON_EXTEND, 0.8f, 1.6f);
        }).anim((p, t, a) -> {
            float k = kick(t, 0.17f, 0.25f);
            aim(p, raise(t, 0.12f, 0.9f), 0.05f, k * 1.6f);
            float pump = track(t, 0.4f, 0, 0.55f, 1, 0.7f, 0);
            p.lArmX += pump * 0.5f; p.lArmY -= pump * 0.15f;
            p.rHandZ = pump * 0.35f; p.rHandX -= pump * 0.25f;
            p.bodyX -= k * 0.3f; p.rootZ += k * 1.0f; p.headX -= k * 0.2f;
            p.bodyY = pump * 0.12f; p.rootY -= pump * 0.15f;
        }));

        // MACHINE GUN: controlled burst, then mag check / slap
        out.add(AttackDef.of("machinegun").range(3, 22).cd(40).dur(22).weapon("machinegun").tick((e, t, n) -> {
            if (n >= 2 && n <= 16 && n % 2 == 0) { fire(e, t, 0.045, 3.5f, 0xffe080); Combat.gunSound(e, 1.4f + e.getRandom().nextFloat() * 0.1f); }
            if (n == 18) Combat.sound(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.6f, 1.6f);
            if (n == 20) Combat.sound(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.7f, 1.4f);
        }).anim((p, t, a) -> {
            float firing = t > 0.09f && t < 0.77f ? 1 : 0;
            float k = firing * 0.45f * Math.abs((float) Math.sin(t * 22 * PI / 2));
            aim(p, raise(t, 0.08f, 0.95f), 0, k);
            float rel = bump(win(t, 0.78f, 1f));
            p.lArmX += rel * 0.9f; p.lArmZ -= rel * -0.3f; p.lArmY -= rel * 0.3f;
            p.rHandZ = rel * 0.55f; p.headX += rel * 0.3f; p.headY += rel * 0.2f;
            p.bodyX += firing * 0.08f; p.rLegX += firing * 0.1f;
        }));

        // CHAINGUN: spin-up, braced hip-fire stream, wind-down
        out.add(AttackDef.of("chaingun").range(4, 24).cd(90).dur(40).weapon("chaingun").tick((e, t, n) -> {
            if (n == 0) Combat.sound(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.8f);
            if (n >= 10 && n <= 36) {
                fire(e, t, 0.06, 3f, 0xfff0a0);
                if (n % 2 == 0) Combat.gunSound(e, 1.9f);
                Fx.burst(lvl(e), ParticleTypes.CRIT, gun(e), 1, 0.05, 0.1);
            }
            if (n == 37) Combat.sound(e, SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.8f);
        }).anim((p, t, a) -> {
            float r = raise(t, 0.2f, 0.92f), fire = t > 0.25f && t < 0.9f ? 1 : 0;
            float shake = fire * (float) Math.sin(a * 4.3f) * 0.05f;
            aim(p, r, 0.35f, fire * 0.25f + shake);
            p.rLegX = 0.5f * r; p.lLegX = -0.5f * r; p.rLegZ = 0.15f * r; p.lLegZ = -0.15f * r;
            p.rootY -= 0.35f * r; p.bodyX = -0.12f * fire + 0.2f * bump(win(t, 0, 0.25f));
            p.bodyY = shake * 0.8f; p.headX += 0.25f * bump(win(t, 0, 0.25f));
            p.rootZ += 0.15f * fire;
        }));

        // GRENADE: pull pin, overhand lob with the off hand (bounces, fuse)
        out.add(AttackDef.of("grenade").range(4, 16).cd(70).dur(18).weapons(null, "grenade").tick((e, t, n) -> {
            if (n == 4) Combat.sound(e, SoundEvents.CHAIN_BREAK, 0.8f, 1.8f);
            if (n == 10) {
                Vec3 from = Combat.muzzle(e, 0.6, 0.4, 0.6), d = aimDir(e, t, 0.03);
                double dist = t != null ? e.distanceTo(t) : 8;
                Combat.shoot(e, GRENADE, from, d.add(0, 0.15 + dist * 0.025, 0), 0.75 + dist * 0.02, 16);
                Combat.sound(e, SoundEvents.EGG_THROW, 1f, 0.5f);
            }
        }).anim((p, t, a) -> {
            p.lArmX = trackLin(t, 0, -0.5f, 0.22f, -1.0f, 0.42f, -3.4f, 0.56f, -1.3f, 0.75f, -0.5f, 1, -0.4f);
            p.lArmZ = track(t, 0, 0, 0.42f, -0.3f, 0.6f, 0.1f, 1, 0);
            p.rArmX = track(t, 0, -0.5f, 0.22f, -1.2f, 0.32f, -0.4f, 1, -0.5f); // right hand yanks the pin
            p.rArmY = track(t, 0, 0, 0.22f, 0.5f, 0.32f, 0);
            p.bodyY = track(t, 0, 0, 0.42f, -0.55f, 0.58f, 0.5f, 0.8f, 0.2f, 1, 0);
            p.bodyX = track(t, 0, 0, 0.42f, -0.2f, 0.58f, 0.35f, 1, 0);
            p.lLegX = track(t, 0, 0, 0.42f, -0.2f, 0.56f, -0.55f, 0.85f, -0.3f, 1, 0);
            p.rLegX = track(t, 0, 0, 0.42f, 0.4f, 0.6f, 0.2f, 1, 0);
            p.rootPitch = track(t, 0, 0, 0.42f, -0.05f, 0.58f, 0.08f, 1, 0);
            p.headY = -p.bodyY * 0.6f;
        }));

        // ROCKET LAUNCHER: shouldered shot, heavy kick, breech reload
        out.add(AttackDef.of("rocket").range(6, 30).cd(70).dur(24).weapon("rocket").tick((e, t, n) -> {
            if (n == 8) {
                Vec3 m = Combat.muzzle(e, 1.0, -0.3, 0.1);
                Combat.shoot(e, ProjStyle.ROCKET, m, aimDir(e, t, 0.01), 0.9, 18);
                Fx.burst(lvl(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.6, -0.3, 0.1), 8, 0.2, 0.02);
                Combat.sound(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5f, 0.6f);
            }
            if (n == 17) Combat.sound(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.5f);
        }).anim((p, t, a) -> {
            float k = kick(t, 0.33f, 0.35f);
            aim(p, raise(t, 0.2f, 0.92f), -0.05f, k * 1.4f);
            p.headZ = 0.18f; p.bodyY = -0.15f; p.bodyX -= k * 0.4f; p.rootZ += k * 1.2f; p.rLegX += k * 0.3f;
            float rel = bump(win(t, 0.6f, 0.9f));
            p.lArmX += rel * 0.6f; p.lArmY -= rel * 0.3f; p.rHandX -= rel * 0.2f; p.headX += rel * 0.15f;
        }));

        // PLASMA GUN: rapid bolts, then heat vent
        out.add(AttackDef.of("plasma").range(3, 20).cd(50).dur(20).weapon("plasma").tick((e, t, n) -> {
            if (n >= 3 && n <= 15 && n % 3 == 0) {
                Combat.shoot(e, ProjStyle.PLASMA, gun(e), aimDir(e, t, 0.03), 1.6, 6);
                Combat.sound(e, SoundEvents.SHULKER_SHOOT, 0.8f, 1.6f);
            }
            if (n == 17) { Combat.sound(e, SoundEvents.FIRE_EXTINGUISH, 0.5f, 1.4f); Fx.burst(lvl(e), ParticleTypes.CLOUD, gun(e), 4, 0.1, 0.02); }
        }).anim((p, t, a) -> {
            float k = 0;
            for (int i = 3; i <= 15; i += 3) k += kick(t, i / 20f, 0.12f);
            aim(p, raise(t, 0.12f, 0.93f), 0, k * 0.4f);
            float vent = bump(win(t, 0.8f, 1f));
            p.rHandZ = vent * 0.5f + (float) Math.sin(a * 2) * 0.02f; p.rHandX -= vent * 0.3f;
            p.headY += vent * 0.15f; p.bodyY = -0.1f;
        }));

        // BFG 9000: long charge, then the ball with tracer rays
        out.add(AttackDef.of("bfg").range(8, 32).cd(240).dur(40).weapon("bfg").tick((e, t, n) -> {
            if (n == 0) Combat.sound(e, SoundEvents.BEACON_ACTIVATE, 1.5f, 0.6f);
            if (n < 26 && n % 2 == 0) Fx.dustBurst(lvl(e), 0x66ff66, 1f + n / 20f, gun(e), 2 + n / 4, 0.1 + n * 0.01);
            if (n == 26) {
                Combat.shoot(e, BFG, Combat.muzzle(e, 1.2, -0.3, 0), aimDir(e, t, 0), 0.55, 40);
                Combat.sound(e, SoundEvents.WITHER_SHOOT, 1.5f, 0.6f);
                Fx.burst(lvl(e), ParticleTypes.HAPPY_VILLAGER, gun(e), 20, 0.4, 0.1);
            }
        }).anim((p, t, a) -> {
            float ch = win(t, 0, 0.62f), k = kick(t, 0.65f, 0.3f), r = raise(t, 0.15f, 0.92f);
            float shake = ch * (t < 0.65f ? 1 : 0) * (float) Math.sin(a * 5) * 0.06f;
            aim(p, r, 0.1f - ch * 0.05f + shake, k * 2f);
            p.rLegX = 0.55f * r; p.lLegX = -0.45f * r; p.rootY -= 0.4f * ch * r;
            p.bodyX = 0.2f * ch * (t < 0.65f ? 1 : 0) - 0.5f * k; p.bodyY = shake;
            p.headX += 0.2f * ch; p.rootZ += 2f * k;
        }));

        // SOUL CUBE: needs 5 souls (or desperate), charge, release homing skull that drains a foe
        out.add(AttackDef.of("soul_cube").range(3, 24).cd(200).dur(36).weight(3).weapon("soulcube")
                .when((e, t) -> souls(e) >= 5 || e.getHealth() < e.getMaxHealth() * 0.4f).tick((e, t, n) -> {
            Vec3 c = Combat.muzzle(e, 0.6, 0, -0.1);
            if (n == 0) Combat.sound(e, SoundEvents.SOUL_ESCAPE, 1.5f, 0.5f);
            if (n < 20 && n % 2 == 0) { Fx.burst(lvl(e), ParticleTypes.SOUL, c, 3, 0.6, 0.01); Fx.dustBurst(lvl(e), 0xff6a2a, 0.8f, c, 2, 0.15); }
            if (n == 22) {
                Combat.shoot(e, SOUL, c, aimDir(e, t, 0), 0.7, 40);
                Combat.sound(e, SoundEvents.WITHER_AMBIENT, 1f, 1.6f);
                e.getPersistentData().putInt(SOULS, 0);
            }
        }).anim((p, t, a) -> {
            float ch = smooth(win(t, 0, 0.55f)), go = smooth(win(t, 0.55f, 0.65f)), back = smooth(win(t, 0.8f, 1f));
            float tremble = ch * (1 - go) * (float) Math.sin(a * 6) * 0.04f;
            float x = lerp(lerp(-0.8f, -1.5f, ch), -2.1f, go);
            x = lerp(x, -0.6f, back);
            p.rArmX = x + tremble; p.lArmX = x - tremble; p.rArmY = -0.3f; p.lArmY = 0.3f;
            p.rArmZ = 0.3f * go * (1 - back); p.lArmZ = -0.3f * go * (1 - back);
            p.bodyX = lerp(-0.15f * ch, 0.3f, go) * (1 - back);
            p.headX += lerp(0.3f * ch, -0.1f, go) * (1 - back);
            p.rootY = (0.3f * ch - 0.3f * go) * (1 - back);
            p.lLegX = -0.35f * go * (1 - back); p.rLegX = 0.3f * go * (1 - back);
        }));
    }

    // ---------------- look ----------------
    @Override
    public void idle(Pose p, SlayerEntity e, float age, float limbAmt) {
        // low-ready carry: rifle angled down across the body, heavy breathing
        float br = (float) Math.sin(age * 0.08f) * 0.03f;
        p.rArmX = p.rArmX * 0.3f - 0.65f + br; p.rArmY = -0.1f;
        p.lArmX = p.lArmX * 0.3f - 0.85f + br; p.lArmY = 0.55f; p.lArmZ = 0.05f;
        p.rHandX = 0.35f; p.bodyX += 0.04f + br * 0.5f; p.headX += br;
    }

    @Override
    public void buildModel(Rig rig) {
        Mat armor = Mat.of(0x55636e).noise(0.1f).bevel(0.28f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.FRONT && w >= 8 && h >= 6) { g.rect(x + 1, y + 1, 3, 1, 0xc9a227); g.rect(x + w - 3, y + h - 2, 2, 1, 0x2b3035); }
        });
        Mat armorLt = Mat.of(0x6f7e8a).noise(0.08f).bevel(0.25f);
        Mat suit = Mat.of(0x2c3237).noise(0.15f);
        Mat helm = Mat.of(0x5f6d78).noise(0.08f).bevel(0.22f);
        Mat face = Mat.of(0x3a3f44);
        Mat visor = Mat.of(0x7fd0ff).glow();
        Mat lamp = Mat.of(0xfff1c0).glow();
        Mat metal = Mat.of(0x3b3f43).noise(0.1f);
        Mat gunmetal = Mat.of(0x24272a).noise(0.06f);
        Mat boot = Mat.of(0x1e2225).noise(0.1f);
        Mat belt = Mat.of(0x3e372c).noise(0.12f);

        rig.humanoid(suit, suit, face);
        // torso armour
        rig.box(rig.torso, armor, -4.5f, -12.2f, -3, 9, 7, 6);
        rig.box(rig.torso, armorLt, -4, -5.2f, -2.6f, 8, 3, 5);
        rig.box(rig.torso, belt, -4.5f, -2.2f, -2.6f, 9, 2, 5);
        rig.box(rig.torso, metal, -3, -11, 3, 6, 8, 2);             // backpack / O2 pack
        rig.box(rig.torso, suit, -3, -13, -2.5f, 6, 1, 5);          // neck collar
        // helmet
        rig.box(rig.head, helm, -4.5f, -8.6f, -4.5f, 9, 4, 9);
        rig.box(rig.head, visor, -3.5f, -5.4f, -4.7f, 7, 2, 1);
        rig.box(rig.head, metal, -2, -3, -5, 4, 3, 1);              // respirator
        rig.box(rig.head, helm, -5, -6, -2, 1, 4, 4);
        rig.box(rig.head, helm, 4, -6, -2, 1, 4, 4);
        // arms
        for (PartDefinition arm : new PartDefinition[]{rig.rArm, rig.lArm}) {
            boolean r = arm == rig.rArm;
            rig.box(arm, armor, r ? -3.5f : -1.5f, -3, -2.5f, 5, 5, 5);
            rig.box(arm, armorLt, -2.5f, 5, -2.5f, 5, 4, 5);
        }
        rig.box(rig.lArm, metal, 1.5f, -4.5f, -3.5f, 2, 2, 3);     // shoulder lamp housing
        rig.box(rig.lArm, lamp, 1.5f, -4.5f, -4.5f, 2, 2, 1);      // lens
        // legs
        for (PartDefinition leg : new PartDefinition[]{rig.rLeg, rig.lLeg}) {
            rig.box(leg, armor, -2.5f, 1, -2.5f, 5, 4, 5);
            rig.box(leg, armorLt, -2, 5.5f, -3, 4, 2, 1);
            rig.box(leg, boot, -2.5f, 9, -3, 5, 3, 6);
        }

        // ---- weapons (gun space: origin fist, barrel -z, y down) ----
        PartDefinition w = rig.weapon(rig.rHand, "pistol");
        rig.box(w, gunmetal, -1, -1, -2, 2, 3, 2);
        rig.box(w, metal, -1, -3, -6, 2, 2, 7);

        w = rig.weapon(rig.rHand, "shotgun");
        rig.box(w, gunmetal, -1, -1, -1, 2, 3, 2);
        rig.box(w, metal, -1.5f, -3.5f, -5, 3, 3, 7);
        rig.box(w, gunmetal, -0.5f, -3.5f, -15, 1, 1, 10);
        rig.box(w, gunmetal, -0.5f, -2.5f, -13, 1, 1, 8);
        rig.box(w, Mat.of(0x4a3a2a), -1, -3, -11, 2, 2, 4);         // pump
        rig.box(w, gunmetal, -1, -3, 2, 2, 3, 5);

        w = rig.weapon(rig.rHand, "machinegun");
        rig.box(w, armor, -1.5f, -4, -7, 3, 4, 10);
        rig.box(w, gunmetal, -1, -3.5f, -13, 2, 2, 6);
        rig.box(w, gunmetal, -1, 0, -5, 2, 4, 2);
        rig.box(w, gunmetal, -1, 0, -1, 2, 3, 2);
        rig.box(w, gunmetal, -1, -3.5f, 3, 2, 3, 4);
        rig.box(w, metal, -1, -1.5f, -11, 2, 2, 3);                 // under-barrel flashlight
        rig.box(w, lamp, -1, -1.5f, -12, 2, 2, 1);

        w = rig.weapon(rig.rHand, "chaingun");
        rig.box(w, metal, -2.5f, -5, -6, 5, 5, 9);
        rig.box(w, gunmetal, -1.5f, -4, -16, 3, 3, 10);
        rig.box(w, metal, -2, -4.5f, -17, 4, 4, 1);
        rig.box(w, gunmetal, -0.5f, -7, -4, 1, 2, 5);
        rig.box(w, gunmetal, -1, 0, -1, 2, 3, 2);
        rig.box(w, armor, 2.5f, -4, -4, 2, 4, 4);

        w = rig.weapon(rig.rHand, "rocket");
        rig.box(w, armor, -1.5f, -5, -12, 3, 3, 17);
        rig.box(w, metal, -2, -5.5f, -14, 4, 4, 2);
        rig.box(w, metal, -2, -5.5f, 5, 4, 4, 2);
        rig.box(w, Mat.of(0xff8a30).glow(), -1, -4.5f, -14.2f, 2, 2, 1);
        rig.box(w, gunmetal, -1, -2, -1, 2, 4, 2);
        rig.box(w, gunmetal, -2.5f, -6.5f, -4, 1, 2, 3);

        Mat plasmaGlow = Mat.of(0x55c8ff).glow();
        w = rig.weapon(rig.rHand, "plasma");
        rig.box(w, Mat.of(0x5a6670).bevel(0.3f), -1.5f, -4, -9, 3, 4, 11);
        rig.box(w, metal, -1, -3.5f, -12, 2, 2, 3);
        rig.box(w, plasmaGlow, -2, -3.5f, -8, 1, 2, 5);
        rig.box(w, plasmaGlow, 1, -3.5f, -8, 1, 2, 5);
        rig.box(w, plasmaGlow, -1, -5, -3, 2, 1, 3);
        rig.box(w, gunmetal, -1, 0, -1, 2, 3, 2);

        w = rig.weapon(rig.rHand, "chainsaw");
        rig.box(w, Mat.of(0x8a6a2a).noise(0.15f), -2, -4, -4, 4, 4, 6);  // engine
        rig.box(w, gunmetal, -0.5f, -1, -1, 1, 2, 3);
        rig.box(w, Mat.of(0x6a6e72).decal((m, g, x, y, ww, h, f) -> { for (int i = 0; i < ww; i += 2) g.px(x + i, y, 0x202020); }),
                -0.5f, -3.5f, -17, 1, 3, 13);
        rig.box(w, gunmetal, -2.5f, -5, -5, 5, 1, 1);                   // front handle bar

        Mat bfgGlow = Mat.of(0x66ff66).glow();
        w = rig.weapon(rig.rHand, "bfg");
        rig.box(w, Mat.of(0x4b5560).bevel(0.3f), -2.5f, -6, -10, 5, 6, 14);
        rig.box(w, bfgGlow, -1.5f, -7, -7, 3, 1, 6);
        rig.box(w, metal, -2, -5.5f, -13, 4, 4, 3);
        rig.box(w, bfgGlow, -1.5f, -5, -14, 3, 3, 1);
        rig.box(w, gunmetal, -3, -6.5f, -15, 1, 1, 5);
        rig.box(w, gunmetal, 2, -6.5f, -15, 1, 1, 5);
        rig.box(w, gunmetal, -1, 0, -1, 2, 3, 2);

        w = rig.weapon(rig.rHand, "flashlight");
        rig.box(w, metal, -1, -2, -8, 2, 2, 9);
        rig.box(w, gunmetal, -1.5f, -2.5f, -10, 3, 3, 2);
        rig.box(w, lamp, -1, -2, -11, 2, 2, 1);

        w = rig.weapon(rig.rHand, "soulcube");
        rig.box(w, Mat.of(0x6a4a3a).noise(0.2f).decal((m, g, x, y, ww, h, f) -> {
            g.rectGlow(x + 1, y + h / 2, ww - 2, 1, 0xff7a30);
            g.rectGlow(x + ww / 2, y + 1, 1, h - 2, 0xff7a30);
        }), -2.5f, -6, -5, 5, 5, 5);

        w = rig.weapon(rig.lHand, "grenade");
        rig.box(w, Mat.of(0x4a5a30), -1, -1, -1, 2, 3, 2);
        rig.box(w, metal, -0.5f, -2, -0.5f, 1, 1, 1);
    }
}
