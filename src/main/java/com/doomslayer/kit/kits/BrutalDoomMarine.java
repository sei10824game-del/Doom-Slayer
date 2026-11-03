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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Doomguy as played in Brutal Doom (Sgt. Mark IV): beefy, scarred, open-faced, ultraviolent. */
public class BrutalDoomMarine extends SlayerKit {

    // ---------------- projectiles ----------------
    static final ProjStyle BD_ROCKET = ProjStyle.make().color(0xffb040).size(0.45f).life(100).explode(4f).trail(ParticleTypes.LARGE_SMOKE)
            .onTick(p -> { if (p.age2 % 2 == 0) Fx.burst((ServerLevel) p.level(), ParticleTypes.FLAME, p.position(), 2, 0.05, 0.01); });
    static final ProjStyle BD_PLASMA = ProjStyle.make().color(0x40b0ff).size(0.4f).life(50).trail(ParticleTypes.ELECTRIC_SPARK);
    static final ProjStyle FRAG = ProjStyle.make().color(0x3e5a28).size(0.25f).life(45).gravity(0.045).bounce().explode(4f).trail(ParticleTypes.SMOKE);
    static final ProjStyle BD_BFG = ProjStyle.make().color(0x50ff50).size(1.7f).life(90).explode(6f).trail(ParticleTypes.HAPPY_VILLAGER)
            .onTick(p -> Fx.dustBurst((ServerLevel) p.level(), 0x60ff60, 2f, p.position(), 4, 0.5))
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 5f); Fx.dustBurst(l, 0x50ff50, 2.5f, pos, 60, 2.5);
                Fx.ring(l, pos, 3f, 0x80ff80, 2f, 40); Fx.ring(l, pos, 5f, 0x40c040, 2f, 56);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 2f, 0.6f);
                // classic BFG spray: tracers from the shooter along its facing
                if (p.getOwner() instanceof SlayerEntity o) for (LivingEntity f : Combat.cone(o, 24, 45)) {
                    Combat.damage(o, f, 14f); Fx.beam(l, o.getEyePosition(), mid(f), 0x60ff60, 1.2f, 0.4); gore(l, mid(f), 12);
                }
            });

    public BrutalDoomMarine() {
        super("brutal_doom_marine", "Doomguy", "ドゥームガイ", "Brutal Doom (mod)");
        maxHealth = 90; speed = 0.32; armor = 14; knockbackRes = 0.5;
        scale = 1.06f; width = 0.66f; preferredRange = 8f; dmgMult = 1.15f;
        eggPrimary = 0x4f7a2e; eggSecondary = 0x9a0c0c;
        idleWeaponR = "rifle";
    }

    // ================= server helpers =================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static boolean ok(SlayerEntity e, LivingEntity t) { return t != null && Combat.isFoe(e, t); }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static Vec3 headOf(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.88, 0); }

    /** stylized gore: chunky red dust + red block bits */
    static void gore(ServerLevel l, Vec3 p, int n) {
        Fx.dustBurst(l, 0xa00c0c, 1.6f, p, n, 0.3);
        Fx.dustBurst(l, 0x560000, 1.0f, p, Math.max(1, n / 2), 0.55);
        Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()), p, Math.max(1, n / 3), 0.2, 0.15);
    }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }

    static boolean melee(SlayerEntity e, LivingEntity t, double reach, float dmg, double kb, double up, int goreN, SoundEvent s, float pitch) {
        if (!ok(e, t) || e.distanceTo(t) > reach) { snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 1.3f); return false; }
        Combat.damage(e, t, dmg); Combat.knock(t, e.position(), kb, up);
        gore(L(e), mid(t), goreN); Fx.sparks(L(e), mid(t), 5);
        snd(e, s, 1.1f, pitch);
        return true;
    }
    /** finishing blow of a fatality */
    static void execute(SlayerEntity e, LivingEntity t, Vec3 at, int goreN) {
        if (!ok(e, t)) return;
        Combat.damage(e, t, t.getHealth() + 100f);
        gore(L(e), at, goreN);
        snd(e, SoundEvents.SLIME_BLOCK_BREAK, 1.3f, 0.6f);
    }
    /** pin the victim in front of the marine during a staged fatality */
    static void hold(SlayerEntity e, LivingEntity t, double dist) {
        if (!ok(e, t)) return;
        Vec3 f = Vec3.directionFromRotation(0, e.getYRot());
        Vec3 p = e.position().add(f.scale(dist));
        t.teleportTo(p.x, t.getY(), p.z); t.setDeltaMovement(Vec3.ZERO); t.hurtMarked = true;
    }
    static void shots(SlayerEntity e, LivingEntity t, int n, double spread, float dmg, double side, int rgb) {
        if (t == null || !t.isAlive()) return;
        ServerLevel l = L(e); Vec3 m = Combat.muzzle(e, 0.95, side, 0);
        Random r = new Random(e.getRandom().nextLong()); int hits = 0;
        for (int i = 0; i < n; i++) hits += Combat.hitscan(e, m, Combat.dirTo(m, t, spread, r), 32, dmg, 0, rgb);
        Fx.burst(l, ParticleTypes.FLAME, m, 2 + n / 3, 0.05, 0.02);
        Fx.burst(l, ParticleTypes.SMOKE, m, 2, 0.05, 0.02);
        if (hits > 0) gore(l, mid(t), Math.min(30, 2 + hits * 2));
    }
    static void proj(SlayerEntity e, LivingEntity t, ProjStyle s, double side, double up, double speed, float dmg, double spread, double lob) {
        if (t == null || !t.isAlive()) return;
        Vec3 m = Combat.muzzle(e, 1.0, side, up);
        Vec3 d = Combat.dirTo(m, t, spread, new Random(e.getRandom().nextLong())).add(0, lob * m.distanceTo(t.position()), 0);
        Combat.shoot(e, s, m, d, speed, dmg);
    }
    static void shell(SlayerEntity e) { Fx.burst(L(e), Fx.dust(0xd8a830, 0.6f), Combat.muzzle(e, 0.3, 0.6, 0.1), 2, 0.05, 0); }

    // ================= anim helpers =================
    static void arms0(Pose p) {
        p.rArmX = p.rArmY = p.rArmZ = p.lArmX = p.lArmY = p.lArmZ = 0;
        p.rHandX = p.rHandY = p.rHandZ = p.lHandX = p.lHandY = p.lHandZ = 0;
    }
    /** left-foot-forward fighting stance */
    static void stance(Pose p, float k) { p.lLegX = -0.32f * k; p.rLegX = 0.3f * k; p.lLegZ = -0.07f * k; p.rLegZ = 0.07f * k; p.lLegY = p.rLegY = 0; }
    /** summed recoil kicks at the given server ticks */
    static float kicks(float t, int dur, float len, int... ticks) { float k = 0; for (int x : ticks) k += kick(t, x / (float) dur, len); return Math.min(k, 1.3f); }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float limb) {
        // rifle at low ready, heavy breathing
        float br = (float) Math.sin(age * 0.08f);
        p.rArmX = -0.55f + p.rArmX * 0.25f; p.rArmY = -0.1f; p.rArmZ = 0.05f;
        p.lArmX = -0.85f + p.lArmX * 0.2f; p.lArmY = 0.6f; p.lArmZ = 0;
        p.rHandX = -0.55f;
        p.bodyX += br * 0.025f; p.headX += br * 0.015f;
        p.rootY += br * 0.08f;
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---- FISTS: left jab, right cross ----
        out.add(AttackDef.of("fists").range(0, 2.8f).dur(18).cd(22).weight(3)
                .tick((e, tg, n) -> {
                    if (n == 4) melee(e, tg, 3.2, 4f, 0.3, 0.1, 4, SoundEvents.PLAYER_ATTACK_WEAK, 1.1f);
                    if (n == 11) melee(e, tg, 3.2, 6f, 0.7, 0.2, 8, SoundEvents.PLAYER_ATTACK_STRONG, 0.9f);
                })
                .anim((p, t, a) -> {
                    arms0(p); stance(p, 1);
                    p.weaponR = null;
                    p.lArmX = track(t, 0, -0.5f, .12f, -0.7f, .22f, -1.7f, .35f, -0.8f, 1, -0.5f);
                    p.rArmX = track(t, 0, -0.6f, .4f, -0.25f, .5f, -0.2f, .61f, -1.75f, .78f, -1.2f, 1, -0.6f);
                    p.lArmY = 0.35f; p.rArmY = -0.35f + 0.25f * bump(win(t, .5f, .7f));
                    p.lArmZ = -0.1f; p.rArmZ = 0.1f;
                    p.bodyY = track(t, 0, 0, .22f, 0.3f, .45f, 0.5f, .61f, -0.65f, .8f, -0.4f, 1, 0);
                    p.bodyX = 0.15f + 0.15f * (kick(t, .22f, .2f) + kick(t, .61f, .25f));
                    p.headX = 0.12f; p.headY = -p.bodyY * 0.6f;
                    p.rootZ = track(t, 0, 0, .22f, -1f, .45f, 0, .61f, -2f, 1, 0);
                    p.rootY = -0.5f;
                }));

        // ---- BERSERK fist combo: exaggerated wind-up, hook-hook-uppercut ----
        out.add(AttackDef.of("berserk_combo").range(0, 3f).dur(36).cd(140).weight(2)
                .tick((e, tg, n) -> {
                    ServerLevel l = L(e);
                    if (n == 0) { snd(e, SoundEvents.RAVAGER_ROAR, 0.9f, 1.4f); Fx.burst(l, ParticleTypes.ANGRY_VILLAGER, e.position().add(0, 2.1, 0), 3, 0.3, 0); }
                    if (n < 13 && n % 2 == 0) Fx.dustBurst(l, 0xff1010, 1.2f, Combat.muzzle(e, 0.2, 0, -0.2), 6, 0.5);
                    if (n == 12) Combat.lunge(e, 0.55, 0.05);
                    if (n == 14) melee(e, tg, 3.4, 11f, 0.8, 0.15, 14, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.7f);
                    if (n == 20) melee(e, tg, 3.4, 11f, 0.8, 0.15, 14, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8f);
                    if (n == 27 && melee(e, tg, 3.4, 18f, 1.2, 0.85, 26, SoundEvents.PLAYER_ATTACK_CRIT, 0.6f))
                        Fx.arc(l, e.position().add(0, 1.0, 0), e.getYRot(), 1.3f, -30, 30, 90, 0xff2020, 1.3f, 14);
                })
                .anim((p, t, a) -> {
                    arms0(p); p.weaponR = null;
                    float shake = (float) Math.sin(a * 2.3f) * 0.06f * win(t, 0, .32f) * (1 - win(t, .32f, .36f));
                    p.rArmX = track(t, 0, -0.4f, .3f, 1.0f, .39f, -1.6f, .5f, -1.2f, .56f, -0.6f, .66f, 0.7f, .75f, -2.9f, .9f, -2.5f, 1, -0.4f) + shake;
                    p.rArmZ = track(t, 0, 0.1f, .3f, 0.9f, .39f, 0.25f, .66f, 0.1f, 1, 0.1f);
                    p.rArmY = -0.6f * bump(win(t, .32f, .47f));
                    p.lArmX = track(t, 0, -0.4f, .3f, 0.8f, .45f, 0.4f, .56f, -1.6f, .66f, -1.0f, .75f, -0.3f, 1, -0.4f) - shake;
                    p.lArmZ = -track(t, 0, 0.1f, .3f, 0.8f, .5f, 0.9f, .56f, 0.25f, 1, 0.1f);
                    p.lArmY = 0.6f * bump(win(t, .5f, .62f));
                    p.bodyY = track(t, 0, 0, .3f, 0.85f, .39f, -0.75f, .5f, -0.3f, .56f, 0.75f, .66f, 0.1f, .75f, -0.35f, 1, 0);
                    p.bodyX = track(t, 0, 0, .3f, 0.5f, .39f, 0.25f, .66f, 0.45f, .75f, -0.4f, .9f, -0.2f, 1, 0);
                    p.headX = track(t, 0, 0, .3f, 0.3f, .66f, 0.2f, .75f, -0.35f, 1, 0);
                    p.headY = -p.bodyY * 0.5f;
                    p.rootY = track(t, 0, 0, .3f, -1.6f, .39f, -0.4f, .66f, -1.6f, .75f, 1.8f, .85f, 0.6f, 1, 0);
                    p.rootZ = track(t, 0, 0, .3f, 1.5f, .39f, -2f, .56f, -3f, .75f, -4f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, .39f, -0.3f, .56f, -0.55f, .75f, -0.1f, 1, 0);
                    p.rLegX = track(t, 0, 0, .3f, 0.5f, .39f, 0.2f, .56f, 0.35f, .75f, 0.1f, 1, 0);
                    p.lLegZ = -0.12f; p.rLegZ = 0.12f;
                }));

        // ---- QUICK KICK: roundhouse ----
        out.add(AttackDef.of("quick_kick").range(0, 2.8f).dur(14).cd(30).weight(2)
                .tick((e, tg, n) -> {
                    if (n == 2) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 0.8f);
                    if (n == 6 && melee(e, tg, 3.3, 8f, 1.6, 0.4, 8, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.7f))
                        Fx.arc(L(e), e.position().add(0, 0.8, 0), e.getYRot(), 1.4f, -70, 40, 15, 0xc8c8c8, 0.8f, 12);
                })
                .anim((p, t, a) -> {
                    p.rootYaw = track(t, 0, 0, .25f, 0.45f, .43f, -0.9f, .65f, -0.6f, 1, 0);
                    p.rLegX = track(t, 0, 0, .25f, -1.1f, .43f, -1.5f, .65f, -1.0f, .85f, -0.3f, 1, 0);
                    p.rLegZ = track(t, 0, 0, .25f, 0.3f, .43f, 1.05f, .65f, 0.7f, 1, 0);
                    p.lLegX = 0.1f; p.lLegZ = -0.08f;
                    p.bodyZ = track(t, 0, 0, .43f, 0.38f, .7f, 0.2f, 1, 0);
                    p.bodyX = track(t, 0, 0, .25f, 0.1f, .43f, -0.28f, 1, 0);
                    p.rArmZ = 0.9f * bump(win(t, .1f, .9f)); p.lArmZ = -1.0f * bump(win(t, .1f, .9f));
                    p.rArmX = -0.4f; p.lArmX = -0.5f; p.rArmY = p.lArmY = 0;
                    p.rHandX = -0.3f;
                    p.headY = -p.rootYaw * 0.7f; p.headZ = -p.bodyZ * 0.6f;
                    p.rootY = 0.9f * bump(win(t, .2f, .6f));
                }));

        // ---- FATALITY: kick down + curb stomp ----
        out.add(AttackDef.of("fatality_stomp").range(0, 3f).dur(30).cd(90).weight(4).lowHealthTarget(0.25f)
                .tick((e, tg, n) -> {
                    if (n == 5) melee(e, tg, 3.5, 3f, 0, 0, 6, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.6f);
                    if (n >= 5 && n < 26) hold(e, tg, 1.15);
                    if (n == 18 && ok(e, tg)) {
                        Combat.damage(e, tg, 10f); gore(L(e), headOf(tg), 22);
                        snd(e, SoundEvents.GENERIC_BIG_FALL, 1.2f, 0.7f);
                        Fx.ring(L(e), tg.position().add(0, 0.1, 0), 1.2f, 0x800000, 1.4f, 18);
                    }
                    if (n == 24 && ok(e, tg)) {
                        snd(e, SoundEvents.BONE_BLOCK_BREAK, 1.3f, 0.6f);
                        Fx.ring(L(e), tg.position().add(0, 0.1, 0), 1.8f, 0xa00c0c, 1.6f, 24);
                        execute(e, tg, headOf(tg), 45);
                    }
                })
                .anim((p, t, a) -> {
                    arms0(p); p.weaponR = null;
                    p.rLegX = track(t, 0, 0, .1f, -0.8f, .17f, -1.45f, .3f, -0.2f, .45f, -1.35f, .6f, -0.15f, .7f, -1.25f, .8f, -0.1f, 1, 0);
                    p.lLegX = track(t, 0, 0, .17f, 0.25f, .45f, 0.15f, 1, 0);
                    p.rLegZ = 0.05f; p.lLegZ = -0.05f;
                    p.rootY = track(t, 0, 0, .17f, 0.4f, .45f, 2.6f, .6f, -0.9f, .7f, 1.6f, .8f, -1.1f, 1, 0);
                    p.rootZ = track(t, 0, 0, .17f, -1.5f, .45f, -2.5f, 1, 0);
                    p.bodyX = track(t, 0, 0, .17f, -0.25f, .45f, 0.35f, .6f, 0.55f, .7f, 0.4f, .8f, 0.62f, 1, 0);
                    p.headX = 0.45f * win(t, .25f, .45f) * (1 - win(t, .85f, 1));
                    p.rArmZ = track(t, 0, 0.1f, .17f, 0.5f, .45f, 0.9f, .6f, 0.35f, .7f, 0.9f, .8f, 0.3f, 1, 0.1f);
                    p.lArmZ = -track(t, 0, 0.1f, .17f, 0.6f, .45f, 1.0f, .6f, 0.4f, .7f, 1.0f, .8f, 0.35f, 1, 0.1f);
                    p.rArmX = track(t, 0, -0.2f, .45f, -0.6f, .6f, 0.3f, 1, 0);
                    p.lArmX = track(t, 0, -0.2f, .45f, -0.7f, .6f, 0.35f, 1, 0);
                    p.bodyZ = 0.12f * kick(t, .6f, .2f) - 0.12f * kick(t, .8f, .2f);
                }));

        // ---- FATALITY: grab + neck snap ----
        out.add(AttackDef.of("fatality_neck_snap").range(0, 3f).dur(30).cd(90).weight(4).lowHealthTarget(0.25f)
                .tick((e, tg, n) -> {
                    if (n == 5 && ok(e, tg)) snd(e, SoundEvents.ARMOR_EQUIP_LEATHER, 1.2f, 0.8f);
                    if (n >= 5 && n < 24) hold(e, tg, 0.95);
                    if (n == 18 && ok(e, tg)) {
                        snd(e, SoundEvents.BONE_BLOCK_BREAK, 1.5f, 1.3f); snd(e, SoundEvents.SKELETON_HURT, 1f, 0.5f);
                        Fx.sparks(L(e), headOf(tg), 8);
                        execute(e, tg, headOf(tg), 10);
                    }
                    if (n == 24 && tg != null) Combat.knock(tg, e.position(), 0.4, 0.1);
                })
                .anim((p, t, a) -> {
                    arms0(p); p.weaponR = null; stance(p, 1.2f);
                    float grip = win(t, .15f, .28f) * (1 - win(t, .85f, 1));
                    p.rArmX = track(t, 0, -0.3f, .25f, -1.75f, .55f, -1.6f, .6f, -1.35f, .85f, -1.4f, 1, -0.3f);
                    p.lArmX = track(t, 0, -0.3f, .25f, -1.85f, .55f, -1.7f, .6f, -1.9f, .85f, -1.8f, 1, -0.3f);
                    p.rArmY = -0.5f * grip; p.lArmY = 0.5f * grip;
                    p.rArmZ = track(t, 0, 0, .5f, -0.35f, .6f, 0.55f, .85f, 0.45f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .5f, -0.3f, .6f, 0.6f, .85f, 0.5f, 1, 0);
                    p.bodyY = track(t, 0, 0, .5f, 0.4f, .6f, -0.75f, .8f, -0.6f, 1, 0);
                    p.bodyX = track(t, 0, 0, .25f, 0.2f, .45f, -0.15f, .55f, -0.18f, .6f, 0.15f, 1, 0);
                    p.rootZ = track(t, 0, 0, .2f, -2.2f, .85f, -1.5f, 1, 0);
                    p.headY = -p.bodyY * 0.4f; p.headX = 0.15f;
                    p.rootY = -0.6f * grip;
                }));

        // ---- FATALITY: chainsaw rip ----
        out.add(AttackDef.of("fatality_chainsaw_rip").range(0, 3f).dur(42).cd(110).weight(4).lowHealthTarget(0.25f).weapon("chainsaw")
                .tick((e, tg, n) -> {
                    ServerLevel l = L(e);
                    if (n < 10 && n % 3 == 0) { snd(e, SoundEvents.GRINDSTONE_USE, 1f, 0.6f + n * 0.04f); Fx.burst(l, ParticleTypes.SMOKE, Combat.muzzle(e, 0.3, 0.3, -0.3), 2, 0.05, 0.02); }
                    if (n >= 8 && n < 34) hold(e, tg, 1.35);
                    if (n == 10) Combat.lunge(e, 0.4, 0);
                    if (n >= 10 && n < 33 && n % 3 == 1 && ok(e, tg)) {
                        Combat.damage(e, tg, 2.5f); gore(l, mid(tg), 8); Fx.sparks(l, mid(tg), 3);
                        snd(e, SoundEvents.GRINDSTONE_USE, 1f, 0.9f); if (n % 6 == 1) snd(e, SoundEvents.SHEEP_SHEAR, 1f, 0.6f);
                    }
                    if (n == 33 && ok(e, tg)) {
                        snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 0.6f);
                        Fx.burst(l, Fx.dust(0xa00c0c, 2f), tg.position().add(0, tg.getBbHeight(), 0), 30, 0.15, 0);
                        execute(e, tg, mid(tg), 60);
                    }
                })
                .anim((p, t, a) -> {
                    arms0(p);
                    float grind = win(t, .25f, .3f) * (1 - win(t, .76f, .8f));
                    float pitch = track(t, 0, 0.6f, .2f, 0.35f, .26f, 0.05f, .75f, 0.15f, .84f, -1.1f, 1, 0.3f);
                    p.aim2(pitch, 0);
                    float jit = (float) Math.sin(a * 3.1f) * (0.03f + 0.07f * grind);
                    p.rArmX += jit; p.lArmX -= jit; p.rHandZ = (float) Math.sin(a * 4.3f) * 0.1f * grind;
                    p.rHandX = track(t, 0, 0.2f, .26f, 0, .84f, -0.5f, 1, 0);
                    p.bodyX = track(t, 0, 0, .2f, -0.1f, .28f, 0.35f, .75f, 0.3f, .84f, -0.35f, 1, 0) + jit * 0.5f;
                    p.bodyY = track(t, 0, 0, .26f, -0.2f, .75f, -0.15f, .84f, 0.2f, 1, 0);
                    p.rootZ = track(t, 0, 0, .26f, -3f, .8f, -2f, 1, 0);
                    p.rootY = track(t, 0, 0, .28f, -1.2f, .76f, -1.2f, .84f, 1.0f, 1, 0);
                    p.rootX = jit * 6 * grind;
                    p.lLegX = -0.45f; p.rLegX = 0.4f; p.lLegZ = -0.1f; p.rLegZ = 0.1f;
                    p.headX = 0.2f * grind - 0.2f * win(t, .78f, .86f);
                }));

        // ---- FATALITY: shotgun to the face ----
        out.add(AttackDef.of("fatality_shotgun_face").range(0, 3f).dur(30).cd(90).weight(4).lowHealthTarget(0.25f).weapon("shotgun")
                .tick((e, tg, n) -> {
                    if (n == 4 && ok(e, tg)) snd(e, SoundEvents.ARMOR_EQUIP_LEATHER, 1.2f, 0.7f);
                    if (n >= 4 && n < 18) hold(e, tg, 1.0);
                    if (n == 11) snd(e, SoundEvents.PISTON_CONTRACT, 0.8f, 1.6f);
                    if (n == 12) snd(e, SoundEvents.PISTON_EXTEND, 0.8f, 1.6f);
                    if (n == 17 && ok(e, tg)) {
                        Combat.gunSound(e, 0.7f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.7f);
                        Vec3 h = headOf(tg);
                        Fx.burst(L(e), ParticleTypes.FLAME, h, 10, 0.15, 0.05);
                        Fx.burst(L(e), Fx.dust(0xa00c0c, 1.8f), h.add(Vec3.directionFromRotation(0, e.getYRot()).scale(0.8)), 30, 0.5, 0);
                        execute(e, tg, h, 70);
                    }
                    if (n == 23 && tg != null) { Combat.knock(tg, e.position(), 1.2, 0.3); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.6f); }
                })
                .anim((p, t, a) -> {
                    arms0(p); stance(p, 1);
                    float k = kick(t, .57f, .25f);
                    p.lArmX = track(t, 0, -0.3f, .15f, -1.75f, .57f, -1.65f, .65f, -1.1f, 1, -0.3f);
                    p.lArmY = 0.3f;
                    p.rArmX = track(t, 0, -0.3f, .3f, -1.0f, .45f, -1.65f, .57f, -1.6f, 1, -0.6f) - k * 0.8f;
                    p.rArmY = -0.25f;
                    p.rHandX = track(t, 0, 0.6f, .3f, 0.3f, .45f, 0, 1, 0) - k * 0.7f;
                    p.bodyX = track(t, 0, 0, .15f, 0.2f, .5f, 0.25f, .57f, 0.25f, 1, 0) - k * 0.4f;
                    p.bodyY = track(t, 0, 0, .45f, -0.3f, .7f, -0.2f, 1, 0);
                    p.headX = -k * 0.3f; p.headY = -p.bodyY * 0.5f;
                    p.rootZ = track(t, 0, 0, .15f, -1.5f, .57f, -1.5f, 1, 0) + k * 2f;
                    p.rLegX = track(t, 0, 0.3f, .7f, 0.4f, .77f, -1.35f, .9f, -0.4f, 1, 0.3f);
                    p.bodyX -= 0.25f * bump(win(t, .7f, .92f));
                }));

        // ---- PISTOL ----
        out.add(AttackDef.of("pistol").range(2, 20).dur(20).cd(24).weight(2).weapon("pistol")
                .tick((e, tg, n) -> { if (n == 4 || n == 9 || n == 14) { shots(e, tg, 1, 0.02, 4.5f, 0.35, 0xffe080); Combat.gunSound(e, 1.7f); shell(e); } })
                .anim((p, t, a) -> {
                    arms0(p);
                    float k = kicks(t, 20, .14f, 4, 9, 14);
                    p.rArmX = lerp(-0.3f, -HALF - 0.05f, outBack(win(t, 0, .18f))) - k * 0.35f;
                    p.rArmY = -0.1f + 0.35f; // compensate bladed torso
                    p.rHandX = -k * 0.55f;
                    p.lArmX = 0.15f; p.lArmZ = -0.2f;
                    p.bodyY = -0.35f * win(t, 0, .15f); p.bodyX = -0.06f * k;
                    p.headY = 0.3f; p.headX = -0.05f * k;
                    p.lLegX = 0.25f; p.rLegX = -0.2f; p.lLegZ = -0.05f; p.rLegZ = 0.05f;
                    p.rootZ = k * 0.6f;
                }));

        // ---- PISTOLS AKIMBO (run and gun) ----
        out.add(AttackDef.of("pistols_akimbo").range(2, 18).dur(26).cd(40).weight(2).weapons("pistol", "pistol").moving()
                .tick((e, tg, n) -> {
                    if (n == 3 || n == 9 || n == 15 || n == 21) { shots(e, tg, 1, 0.03, 4f, 0.4, 0xffe080); Combat.gunSound(e, 1.75f); shell(e); }
                    if (n == 6 || n == 12 || n == 18 || n == 24) { shots(e, tg, 1, 0.03, 4f, -0.4, 0xffe080); Combat.gunSound(e, 1.65f); }
                })
                .anim((p, t, a) -> {
                    arms0(p);
                    float up = outBack(win(t, 0, .12f));
                    float kr = kicks(t, 26, .12f, 3, 9, 15, 21), kl = kicks(t, 26, .12f, 6, 12, 18, 24);
                    p.rArmX = lerp(-0.2f, -HALF, up) - kr * 0.4f; p.lArmX = lerp(-0.2f, -HALF, up) - kl * 0.4f;
                    p.rArmY = -0.05f; p.lArmY = 0.05f; p.rArmZ = -0.05f; p.lArmZ = 0.05f;
                    p.rHandX = -kr * 0.6f; p.lHandX = -kl * 0.6f;
                    p.rHandZ = 0.2f; p.lHandZ = -0.2f; // slight gangster cant
                    p.bodyY = 0.12f * (kl - kr); p.bodyX = 0.1f - 0.04f * (kl + kr);
                    p.headY = -p.bodyY * 0.8f;
                }));

        // ---- SHOTGUN (pump) ----
        out.add(AttackDef.of("shotgun").range(0, 11).dur(24).cd(28).weight(3).weapon("shotgun")
                .tick((e, tg, n) -> {
                    if (n == 4) { shots(e, tg, 7, 0.08, 3.4f, 0.3, 0xffd060); Combat.gunSound(e, 1.0f); if (ok(e, tg) && e.distanceTo(tg) < 4) Combat.knock(tg, e.position(), 0.6, 0.15); }
                    if (n == 11) { snd(e, SoundEvents.PISTON_CONTRACT, 0.8f, 1.5f); Fx.burst(L(e), Fx.dust(0xc02020, 0.8f), Combat.muzzle(e, 0.4, 0.6, 0.1), 1, 0.02, 0); }
                    if (n == 14) snd(e, SoundEvents.PISTON_EXTEND, 0.8f, 1.5f);
                })
                .anim((p, t, a) -> {
                    arms0(p); stance(p, 1);
                    float k = kick(t, .17f, .3f);
                    float pitch = track(t, 0, 0.4f, .12f, 0, .75f, 0, 1, 0.3f);
                    p.aim2(pitch, k * 1.4f);
                    float pump = track(t, 0, 0, .42f, 0, .52f, 0.5f, .64f, 0, 1, 0);
                    p.lArmX += pump; p.lArmY -= pump * 0.3f;
                    p.rHandX = -k * 0.6f; p.rHandZ = 0.15f * bump(win(t, .42f, .66f));
                    p.bodyY = -0.15f; p.headY = 0.12f;
                    p.rootZ = k * 1.6f; p.rootY = -0.5f;
                    p.headX -= k * 0.15f;
                }));

        // ---- SUPER SHOTGUN (both barrels, break-open reload) ----
        out.add(AttackDef.of("super_shotgun").range(0, 7).dur(34).cd(50).weight(3).weapon("super_shotgun")
                .tick((e, tg, n) -> {
                    if (n == 5) {
                        shots(e, tg, 14, 0.13, 3.6f, 0.3, 0xffc040); Combat.gunSound(e, 0.75f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.6f);
                        if (ok(e, tg) && e.distanceTo(tg) < 5) Combat.knock(tg, e.position(), 1.3, 0.35);
                        Vec3 f = e.getViewVector(1f); e.setDeltaMovement(-f.x * 0.35, 0.05, -f.z * 0.35); e.hurtMarked = true;
                    }
                    if (n == 14) snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 1f, 1.3f);
                    if (n == 15) Fx.burst(L(e), Fx.dust(0xc02020, 0.9f), Combat.muzzle(e, 0.5, 0.4, 0), 2, 0.05, 0);
                    if (n == 21) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 1f, 1.5f);
                    if (n == 26) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 1f, 1.2f);
                })
                .anim((p, t, a) -> {
                    arms0(p); stance(p, 1.2f);
                    float k = kick(t, .15f, .3f);
                    float open = win(t, .38f, .44f) * (1 - win(t, .74f, .8f));
                    p.aim2(track(t, 0, 0.4f, .1f, 0, .35f, 0.15f, .4f, 0.55f, .78f, 0.5f, .85f, 0.1f, 1, 0.3f), k * 2f);
                    p.rHandX = -k * 0.9f + open * 0.9f;
                    p.lArmX = lerp(p.lArmX, track(t, .4f, -1.2f, .5f, 0.2f, .6f, 0.3f, .68f, -1.3f, 1, -1.3f), open);
                    p.lArmZ = -0.3f * open * bump(win(t, .44f, .66f));
                    p.bodyX = p.bodyX - k * 0.35f + 0.15f * open; p.bodyY = -0.15f;
                    p.headX = -k * 0.3f + 0.25f * open + 0.15f * kick(t, .78f, .12f);
                    p.headY = 0.15f;
                    p.rootZ = k * 3f; p.rootY = -0.6f + k * 0.6f;
                    p.rLegX += k * 0.5f;
                }));

        // ---- MACHINE GUN (assault rifle, 3-round bursts) ----
        out.add(AttackDef.of("machine_gun").range(4, 24).dur(32).cd(36).weight(3).weapon("rifle")
                .tick((e, tg, n) -> {
                    if (n >= 4 && (n - 4) % 10 < 3) { shots(e, tg, 1, 0.035, 4f, 0.3, 0xffe080); Combat.gunSound(e, 1.5f); shell(e); }
                })
                .anim((p, t, a) -> {
                    arms0(p); stance(p, 1);
                    float k = kicks(t, 32, .08f, 4, 5, 6, 14, 15, 16, 24, 25, 26);
                    p.aim2(track(t, 0, 0.5f, .12f, 0, .85f, 0, 1, 0.4f), k * 0.6f);
                    p.rHandX = -k * 0.35f; p.rHandZ = (float) Math.sin(a * 5) * 0.04f * k;
                    p.bodyY = -0.18f; p.headY = 0.15f; p.headX += 0.1f; // cheek weld
                    p.rootZ = k * 0.7f; p.rootY = -0.4f;
                }));

        // ---- MINIGUN / CHAINGUN (spin-up, sustained fire, spin-down) ----
        out.add(AttackDef.of("minigun").range(3, 22).dur(50).cd(90).weight(2).weapon("minigun")
                .tick((e, tg, n) -> {
                    if (n < 10 && n % 3 == 0) snd(e, SoundEvents.GRINDSTONE_USE, 0.9f, 0.8f + n * 0.08f);
                    if (n >= 10 && n < 44) {
                        shots(e, tg, 1, 0.06, 2.4f, 0.25, 0xffd060);
                        if (n % 2 == 0) { Combat.gunSound(e, 1.9f); shell(e); }
                    }
                    if (n == 44) snd(e, SoundEvents.GRINDSTONE_USE, 0.9f, 0.6f);
                })
                .anim((p, t, a) -> {
                    arms0(p);
                    float n = t * 50, spin;
                    if (n < 10) spin = 0.06f * n * n;
                    else if (n < 44) spin = 6f + 1.2f * (n - 10);
                    else { float m = Math.min(n - 44, 6); spin = 46.8f + 1.2f * m - 0.1f * m * m; }
                    boolean firing = n >= 10 && n < 44;
                    float jit = firing ? (float) Math.sin(a * 6.1f) * 0.035f : 0;
                    float up = smooth(win(t, 0, .15f)) * (1 - smooth(win(t, .9f, 1)));
                    p.rArmX = lerp(-0.3f, -1.0f, up) + jit; p.rArmY = -0.05f;
                    p.lArmX = lerp(-0.3f, -1.25f, up) - jit; p.lArmY = 0.65f * up; p.lArmZ = -0.1f;
                    p.rHandX = -0.55f * up;
                    p.rHandZ = spin;
                    p.bodyX = -0.12f * up + jit; p.bodyY = firing ? (float) Math.sin((n - 10) / 34f * PI * 2) * 0.15f : 0;
                    p.headY = -p.bodyY * 0.4f; p.headX = 0.1f;
                    p.rootX = jit * 8; p.rootY = -1.1f * up; p.rootZ = firing ? 0.5f : 0;
                    p.lLegX = -0.35f; p.rLegX = 0.35f; p.lLegZ = -0.15f; p.rLegZ = 0.15f;
                }));

        // ---- ROCKET LAUNCHER ----
        out.add(AttackDef.of("rocket_launcher").range(6, 28).dur(30).cd(70).weight(2).weapon("rocket_launcher")
                .tick((e, tg, n) -> {
                    if (n == 2) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 1f, 0.8f);
                    if (n == 9) {
                        proj(e, tg, BD_ROCKET, 0.35, 0.15, 1.3, 20f, 0.01, 0);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.6f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.8f);
                        ServerLevel l = L(e); Vec3 back = Combat.muzzle(e, -1.0, 0.35, 0.15);
                        Fx.burst(l, ParticleTypes.LARGE_SMOKE, back, 10, 0.25, 0.04); Fx.burst(l, ParticleTypes.FLAME, back, 6, 0.15, 0.03);
                    }
                })
                .anim((p, t, a) -> {
                    arms0(p); stance(p, 1.3f);
                    float k = kick(t, .3f, .4f);
                    float pitch = track(t, 0, 0.6f, .22f, -0.05f, .7f, -0.05f, 1, 0.5f);
                    p.aim2(pitch, k * 1.6f);
                    p.lArmX -= 0.15f; p.lArmY = 0.35f;
                    p.rHandX = -k * 0.6f;
                    p.bodyX = 0.08f - k * 0.4f; p.bodyY = -0.25f; p.headY = 0.2f; p.headZ = -0.15f; // cheek on the tube
                    p.rootZ = k * 3.2f; p.rootY = -1f + k * 0.5f;
                    p.lLegX -= k * 0.2f;
                }));

        // ---- PLASMA RIFLE ----
        out.add(AttackDef.of("plasma_rifle").range(3, 22).dur(32).cd(55).weight(2).weapon("plasma_rifle").moving()
                .tick((e, tg, n) -> {
                    if (n >= 4 && n <= 28 && (n - 4) % 3 == 0) {
                        proj(e, tg, BD_PLASMA, 0.3, 0, 1.6, 6f, 0.03, 0);
                        snd(e, SoundEvents.SHULKER_SHOOT, 0.7f, 1.7f);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, Combat.muzzle(e, 1.0, 0.3, 0), 3, 0.05, 0.05);
                    }
                    if (n == 30) snd(e, SoundEvents.BEACON_DEACTIVATE, 0.6f, 1.6f);
                })
                .anim((p, t, a) -> {
                    arms0(p);
                    float k = kicks(t, 32, .07f, 4, 7, 10, 13, 16, 19, 22, 25, 28);
                    p.aim2(track(t, 0, 0.4f, .1f, 0, .9f, 0, 1, 0.3f), k * 0.4f);
                    p.rHandX = -k * 0.25f; p.rHandZ = (float) Math.sin(a * 0.9f) * 0.05f;
                    p.bodyY = -0.12f + (float) Math.sin(t * PI * 3) * 0.06f; p.headY = -p.bodyY * 0.7f;
                }));

        // ---- BFG 9000 ----
        out.add(AttackDef.of("bfg9000").range(8, 30).dur(44).cd(400).weight(1).weapon("bfg")
                .tick((e, tg, n) -> {
                    ServerLevel l = L(e);
                    if (n == 0) snd(e, SoundEvents.BEACON_ACTIVATE, 1.5f, 0.7f);
                    if (n < 26) {
                        Vec3 m = Combat.muzzle(e, 1.2, 0.3, 0); Random r = new Random(e.getRandom().nextLong());
                        for (int i = 0; i < 4; i++) {
                            Vec3 o = new Vec3(r.nextGaussian(), r.nextGaussian(), r.nextGaussian()).normalize().scale(1.6 - n * 0.04);
                            Fx.dustBurst(l, 0x60ff60, 1.3f, m.add(o), 1, 0);
                        }
                        Fx.burst(l, ParticleTypes.HAPPY_VILLAGER, m, 1, 0.2, 0);
                        if (n % 6 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 1.2f, 1f + n * 0.03f);
                    }
                    if (n == 26) {
                        proj(e, tg, BD_BFG, 0.3, 0, 0.75, 40f, 0, 0);
                        snd(e, SoundEvents.WARDEN_SONIC_BOOM, 1.4f, 1.3f);
                        Vec3 f = e.getViewVector(1f); e.setDeltaMovement(-f.x * 0.5, 0.15, -f.z * 0.5); e.hurtMarked = true;
                    }
                })
                .anim((p, t, a) -> {
                    arms0(p); stance(p, 1.4f);
                    float charge = win(t, .05f, .59f) * (1 - win(t, .59f, .62f));
                    float k = kick(t, .59f, .35f);
                    float shake = (float) Math.sin(a * 3.7f) * 0.06f * charge * charge;
                    p.aim2(track(t, 0, 0.5f, .15f, 0.05f, .59f, 0, 1, 0.45f), k * 2.2f);
                    p.rArmX += shake; p.lArmX -= shake;
                    p.lArmY = 0.3f; p.lArmZ = -0.15f;
                    p.rHandX = -k * 0.8f; p.rHandZ = shake * 2;
                    p.bodyX = 0.25f * charge - k * 0.55f; p.bodyY = -0.1f;
                    p.headX = 0.15f * charge - k * 0.3f;
                    p.rootX = shake * 10;
                    p.rootY = -1.4f * charge + 1.5f * bump(win(t, .59f, .75f));
                    p.rootZ = k * 4.5f;
                }));

        // ---- CHAINSAW ----
        out.add(AttackDef.of("chainsaw").range(0, 2.8f).dur(26).cd(45).weight(2).weapon("chainsaw").moving()
                .tick((e, tg, n) -> {
                    ServerLevel l = L(e);
                    if (n < 5 && n % 2 == 0) snd(e, SoundEvents.GRINDSTONE_USE, 1f, 0.7f + n * 0.05f);
                    if (n == 5) Combat.lunge(e, 0.35, 0);
                    if (n >= 5 && n < 21 && n % 2 == 1) {
                        int hit = 0;
                        for (LivingEntity f : Combat.cone(e, 3.0, 45)) { Combat.damage(e, f, 2.8f); Combat.knock(f, e.position(), 0.05, 0); gore(l, mid(f), 6); hit++; }
                        if (hit > 0) Fx.sparks(l, Combat.muzzle(e, 1.4, 0.2, -0.2), 4);
                        if (n % 4 == 1) snd(e, SoundEvents.GRINDSTONE_USE, 1f, hit > 0 ? 0.85f : 1.1f);
                        if (hit > 0 && n % 6 == 1) snd(e, SoundEvents.SHEEP_SHEAR, 1f, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    arms0(p); p.lLegX = -0.35f; p.rLegX = 0.3f;
                    float saw = win(t, .18f, .22f) * (1 - win(t, .8f, .86f));
                    p.aim2(track(t, 0, 0.6f, .2f, 0.25f, .8f, 0.25f, 1, 0.5f), 0);
                    float jit = (float) Math.sin(a * 3.3f) * (0.03f + 0.05f * saw);
                    p.rArmX += jit; p.lArmX -= jit; p.rHandZ = jit * 1.5f;
                    p.bodyY = track(t, 0, 0, .2f, 0.35f, .8f, -0.35f, 1, 0);
                    p.bodyX = 0.25f * saw + jit * 0.4f; p.headY = -p.bodyY * 0.6f; p.headX = 0.1f * saw;
                    p.rootZ = -2f * saw; p.rootY = -0.8f * saw;
                }));

        // ---- FRAG GRENADE ----
        out.add(AttackDef.of("frag_grenade").range(5, 15).dur(24).cd(120).weight(1).weapon("grenade")
                .tick((e, tg, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1f, 1.4f);
                    if (n == 13) { proj(e, tg, FRAG, 0.35, 0.4, 0.85, 16f, 0.02, 0.045); snd(e, SoundEvents.SNOWBALL_THROW, 1f, 0.5f); }
                })
                .anim((p, t, a) -> {
                    arms0(p);
                    if (t > .55f) p.weaponR = null;
                    // pin pull: left hand crosses to the grenade at chest
                    float pin = bump(win(t, .02f, .28f));
                    p.lArmX = lerp(track(t, 0, -0.2f, .3f, -0.2f, .4f, -1.5f, .55f, -1.4f, .8f, 0.2f, 1, 0), -1.2f, pin);
                    p.lArmY = 0.7f * pin;
                    p.rArmX = track(t, 0, -0.2f, .1f, -1.0f, .28f, -1.0f, .45f, -2.9f, .55f, -1.4f, .7f, -0.3f, 1, 0);
                    p.rArmY = -0.5f * pin;
                    p.rArmZ = track(t, 0, 0, .45f, 0.3f, .6f, -0.1f, 1, 0);
                    p.bodyY = track(t, 0, 0, .28f, 0, .45f, 0.6f, .55f, -0.5f, .75f, -0.4f, 1, 0);
                    p.bodyX = track(t, 0, 0, .28f, 0, .45f, -0.25f, .58f, 0.38f, .8f, 0.2f, 1, 0);
                    p.headY = -p.bodyY * 0.6f;
                    p.lLegX = track(t, 0, 0, .3f, 0, .45f, -0.45f, 1, -0.2f);
                    p.rLegX = track(t, 0, 0, .45f, 0.2f, .6f, 0.4f, 1, 0.1f);
                    p.rootZ = track(t, 0, 0, .45f, 1.2f, .6f, -1.6f, 1, 0);
                    p.rootY = -0.6f * bump(win(t, .4f, .8f));
                }));

        // ---- TAUNT: chest pound + "come get some" beckon (draws aggro) ----
        out.add(AttackDef.of("taunt").range(4, 16).dur(32).cd(320).weight(1).noLos()
                .tick((e, tg, n) -> {
                    if (n == 6 || n == 12) { snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.5f); Fx.dustBurst(L(e), 0x9a0c0c, 1f, e.position().add(0, 1.4, 0), 4, 0.2); }
                    if (n == 18) {
                        snd(e, SoundEvents.RAVAGER_ROAR, 0.7f, 1.6f);
                        for (LivingEntity f : Combat.around(e, e.position(), 16)) if (f instanceof Mob m) m.setTarget(e);
                    }
                })
                .anim((p, t, a) -> {
                    arms0(p); p.weaponR = null;
                    float pound = kick(t, 6 / 32f, .12f) + kick(t, 12 / 32f, .12f);
                    float pre = win(t, 0, .12f) * (1 - win(t, .45f, .5f));
                    p.rArmX = -1.2f * pre + 0.3f * pound * pre; p.rArmY = 0.9f * pre; p.rArmZ = -0.2f * pre;
                    float beck = win(t, .5f, .58f) * (1 - win(t, .9f, 1));
                    p.lArmX = -1.4f * beck - 0.3f * (float) Math.sin(t * PI * 10) * beck; p.lArmY = 0.2f * beck;
                    p.lHandX = 0.5f * beck;
                    p.bodyX = -0.2f * pre + 0.1f * pound - 0.15f * beck; p.headX = -0.3f * pre + 0.15f * beck;
                    p.rootY = -0.3f * pound;
                    p.lLegZ = -0.12f; p.rLegZ = 0.12f;
                }));
    }

    // ================= model =================
    static final Mat.Decal SCUFF = (m, g, x, y, w, h, f) -> {
        int s = m.seed + f.ordinal() * 977 + x * 31 + y * 17;
        for (int i = 0; i < Math.max(1, w * h / 10); i++) {
            s = s * 1103515245 + 12345; int px = (s >>> 8) % w; s = s * 1103515245 + 12345; int py = (s >>> 8) % h;
            g.px(x + Math.abs(px), y + Math.abs(py), (s & 1) == 0 ? Mat.mix(m.base, 0xd8d8b8, 0.35f) : Mat.mix(m.base, 0x101008, 0.45f));
        }
    };
    static final Mat.Decal FACE = (m, g, x, y, w, h, f) -> {
        if (f == Mat.Face.FRONT) {
            g.rect(x + 1, y + 2, 2, 1, 0x3a2410); g.rect(x + 5, y + 2, 2, 1, 0x3a2410);        // heavy brows
            g.px(x + 1, y + 3, 0xe8e0d0); g.px(x + 2, y + 3, 0x30506a);                         // eyes
            g.px(x + 5, y + 3, 0x30506a); g.px(x + 6, y + 3, 0xe8e0d0);
            g.rect(x + 3, y + 4, 2, 1, Mat.mix(m.base, 0x000000, 0.2f));                       // nose shadow
            g.rect(x + 1, y + 5, 6, 3, Mat.mix(m.base, 0x3a2a1a, 0.45f));                      // stubble jaw
            g.rect(x + 3, y + 6, 3, 1, 0x5a2620);                                               // grim mouth
            g.px(x + 6, y + 1, 0xd07070); g.px(x + 6, y + 2, 0xa04848); g.px(x + 5, y + 4, 0xa04848); g.px(x + 5, y + 5, 0xd07070); // scar
        } else if (f != Mat.Face.BOTTOM) g.rect(x, y, w, Math.min(h, 2), 0x3b2a1c); // buzz cut under helmet edge
    };
    static final Mat.Decal BUCKLE = (m, g, x, y, w, h, f) -> { if (f == Mat.Face.FRONT) g.rect(x + w / 2 - 1, y, 3, h, 0xb0a060); };
    static final Mat.Decal HAZARD = (m, g, x, y, w, h, f) -> { for (int i = 0; i < w; i++) if ((i / 2) % 2 == 0) g.px(x + i, y, 0xd0b020); };

    @Override
    public void buildModel(Rig r) {
        Mat suit = Mat.of(0x3c4a2a).noise(.1f);
        Mat armor = Mat.of(0x4f7a2e).noise(.14f).bevel(.3f).decal(SCUFF);
        Mat armorDk = Mat.of(0x33501e).noise(.12f).bevel(.3f).decal(SCUFF);
        Mat leather = Mat.of(0x5a3c20).noise(.15f);
        Mat belt = Mat.of(0x3e2814).noise(.1f).decal(BUCKLE);
        Mat skin = Mat.of(0xc08a68).noise(.06f).bevel(.08f).decal(FACE);
        Mat glove = Mat.of(0x2e2219).noise(.1f);
        Mat boot = Mat.of(0x2a2016).noise(.1f);
        Mat gunDk = Mat.of(0x26262a).noise(.06f), steel = Mat.of(0x70747a).noise(.08f), wood = Mat.of(0x6a4422).noise(.18f);

        // torso: beefy undersuit + plates
        r.box(r.torso, suit, -4, -12, -2, 8, 12, 4, 0.45f);
        r.box(r.torso, armor, -4.5f, -12.4f, -3.3f, 9, 7, 2);
        r.box(r.torso, armorDk, -3.5f, -5.5f, -2.9f, 7, 3, 1);
        r.box(r.torso, belt, -4.5f, -2.6f, -2.7f, 9, 2, 5, 0.1f);
        r.box(r.torso, leather, -4.3f, -2.3f, -3.5f, 2, 2, 1);
        r.box(r.torso, leather, 2.3f, -2.3f, -3.5f, 2, 2, 1);
        r.box(r.torso, armorDk, -4f, -12f, 2f, 8, 9, 2);
        r.box(r.torso, armorDk, -3f, -13f, -2.6f, 6, 1, 5);
        // head: open-faced, scarred, classic green helmet shell
        r.box(r.head, skin, -4, -8, -4, 8, 8, 8);
        r.box(r.head, armor, -4.5f, -9f, -4.6f, 9, 3, 9);
        r.box(r.head, armor, -4.5f, -6f, -1.5f, 9, 5, 6);
        r.box(r.head, armorDk, -1f, -9.8f, -4.2f, 2, 1, 8);
        // arms: huge pads, gauntlets, gloves
        r.box(r.rArm, suit, -2, -2, -2, 4, 12, 4, 0.35f);
        r.box(r.rArm, armor, -3.6f, -3f, -3f, 5, 4, 6);
        r.box(r.rArm, armorDk, -2.5f, 4.5f, -2.5f, 5, 4, 5);
        r.box(r.rArm, glove, -2, 8.5f, -2, 4, 3, 4, 0.4f);
        r.box(r.lArm, suit, -2, -2, -2, 4, 12, 4, 0.35f);
        r.box(r.lArm, armor, -1.4f, -3f, -3f, 5, 4, 6);
        r.box(r.lArm, armorDk, -2.5f, 4.5f, -2.5f, 5, 4, 5);
        r.box(r.lArm, glove, -2, 8.5f, -2, 4, 3, 4, 0.4f);
        // legs: knee pads, combat boots, holster
        r.box(r.rLeg, suit, -2, 0, -2, 4, 12, 4, 0.3f);
        r.box(r.rLeg, armor, -2.5f, 4f, -3f, 5, 3, 2);
        r.box(r.rLeg, boot, -2.5f, 8f, -3.5f, 5, 4, 6);
        r.box(r.rLeg, leather, -3.2f, 1f, -1.5f, 1, 4, 3);
        r.box(r.lLeg, suit, -2, 0, -2, 4, 12, 4, 0.3f);
        r.box(r.lLeg, armor, -2.5f, 4f, -3f, 5, 3, 2);
        r.box(r.lLeg, boot, -2.5f, 8f, -3.5f, 5, 4, 6);

        // ---- weapons (gun space: origin = fist, barrel -z, y down) ----
        for (var hand : List.of(r.rHand, r.lHand)) {
            var w = r.weapon(hand, "pistol");
            r.box(w, gunDk, -1f, -3.5f, -6f, 2, 2, 8);
            r.box(w, steel, -0.9f, -1.6f, -0.6f, 2, 4, 2);
        }
        var sg = r.weapon(r.rHand, "shotgun");
        r.box(sg, gunDk, -1f, -3.5f, -3f, 2, 3, 7);
        r.box(sg, steel, -0.5f, -4f, -14f, 1, 1, 11);
        r.box(sg, wood, -1f, -3.3f, -11f, 2, 2, 4);
        r.box(sg, wood, -1f, -2.5f, 4f, 2, 3, 5);

        var ssg = r.weapon(r.rHand, "super_shotgun");
        r.box(ssg, steel, -1.5f, -4f, -13f, 1, 1, 10);
        r.box(ssg, steel, 0.5f, -4f, -13f, 1, 1, 10);
        r.box(ssg, gunDk, -1.5f, -4f, -3f, 3, 3, 5);
        r.box(ssg, wood, -1.5f, -3f, -9f, 3, 1, 6);
        r.box(ssg, wood, -1f, -2.5f, 2f, 2, 3, 4);

        var rf = r.weapon(r.rHand, "rifle");
        r.box(rf, gunDk, -1f, -4f, -5f, 2, 3, 9);
        r.box(rf, steel, -0.5f, -3.5f, -10f, 1, 1, 5);
        r.box(rf, gunDk, -0.5f, -1f, -4f, 1, 4, 2);
        r.box(rf, Mat.of(0x3a3a30), -1f, -3.5f, 4f, 2, 3, 4);
        r.box(rf, steel, -0.5f, -5f, -2f, 1, 1, 3);

        var mg = r.weapon(r.rHand, "minigun");
        r.box(mg, Mat.of(0x3a3a34).noise(.1f).decal(HAZARD), -2.5f, -2.5f, -5f, 5, 5, 8);
        r.box(mg, steel, -1.5f, -1.5f, -17f, 1, 1, 12);
        r.box(mg, steel, 0.5f, -1.5f, -17f, 1, 1, 12);
        r.box(mg, steel, -1.5f, 0.5f, -17f, 1, 1, 12);
        r.box(mg, steel, 0.5f, 0.5f, -17f, 1, 1, 12);
        r.box(mg, gunDk, -2f, -2f, -15f, 4, 4, 1);
        r.box(mg, Mat.of(0x4a5a30), 2.5f, -1.5f, -3f, 3, 4, 4);

        var rl = r.weapon(r.rHand, "rocket_launcher");
        Mat olive = Mat.of(0x4a5a34).noise(.12f).decal(SCUFF);
        r.box(rl, olive, -2f, -6f, -12f, 4, 4, 18);
        r.box(rl, gunDk, -2.5f, -6.5f, -13f, 5, 5, 1);
        r.box(rl, gunDk, -2.5f, -6.5f, 6f, 5, 5, 1);
        r.box(rl, Mat.of(0xff8020).glow(), -1f, -5f, -13.5f, 2, 2, 1);
        r.box(rl, gunDk, -1f, -2.5f, -1f, 2, 3, 2);
        r.box(rl, steel, -3f, -6f, -6f, 1, 2, 2);

        var pr = r.weapon(r.rHand, "plasma_rifle");
        Mat cyan = Mat.of(0x40c8ff).glow();
        r.box(pr, Mat.of(0x3a4a5e).noise(.06f).bevel(.25f), -1.5f, -4f, -10f, 3, 3, 13);
        r.box(pr, cyan, -2f, -4.5f, -8f, 4, 1, 2);
        r.box(pr, cyan, -2f, -4.5f, -4f, 4, 1, 2);
        r.box(pr, cyan, -1f, -3.5f, -11f, 2, 2, 1);
        r.box(pr, gunDk, -1f, -1.5f, -1f, 2, 4, 2);

        var bfg = r.weapon(r.rHand, "bfg");
        Mat green = Mat.of(0x50ff50).glow();
        r.box(bfg, Mat.of(0x4a4e44).noise(.1f).bevel(.3f).decal(SCUFF), -3f, -7f, -10f, 6, 6, 14);
        r.box(bfg, green, -2f, -6f, -11f, 4, 4, 1);
        r.box(bfg, green, -3.6f, -6f, -8f, 1, 2, 8);
        r.box(bfg, green, 2.6f, -6f, -8f, 1, 2, 8);
        r.box(bfg, gunDk, -1f, -1.5f, -1f, 2, 4, 2);
        r.box(bfg, gunDk, -1f, -9f, -6f, 2, 2, 6);

        var cs = r.weapon(r.rHand, "chainsaw");
        r.box(cs, Mat.of(0xa02018).noise(.12f).decal(SCUFF), -1.5f, -3f, -2f, 3, 4, 6);
        r.box(cs, steel, -0.5f, -2.5f, -16f, 1, 2, 14, 0.15f);
        r.box(cs, gunDk, -2f, -4.2f, -4f, 4, 1, 1);
        r.box(cs, gunDk, -1f, -0.5f, -1f, 2, 2, 2);

        var gr = r.weapon(r.rHand, "grenade");
        r.box(gr, Mat.of(0x3e5a28).noise(.15f).bevel(.35f), -1f, -1.5f, -1f, 2, 2, 2, 0.2f);
        r.box(gr, steel, -0.5f, -2.6f, -0.5f, 1, 1, 1);
    }
}
