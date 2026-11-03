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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** The Doom Slayer, DOOM (2016): Praetor Suit, full 2016 arsenal + staged glory kills. */
public class Doom2016Slayer extends SlayerKit {
    static final int BLOOD = 0x8a0303, HEALTH = 0x3aa0ff, GREEN = 0x66ff44, PLASMA_C = 0x40c8ff, RED = 0xff3020;

    static final ProjStyle PLASMA_BOLT = ProjStyle.make().color(PLASMA_C).size(0.35f).life(50).trail(ParticleTypes.ELECTRIC_SPARK);
    static final ProjStyle MICRO_MISSILE = ProjStyle.make().color(0xffb050).size(0.2f).life(45).explode(1.3f).homing(0.08f).trail(ParticleTypes.SMOKE);
    static final ProjStyle ROCKET16 = ProjStyle.make().color(0xff8020).size(0.5f).life(100).explode(3.5f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle LOCK_ROCKET = ProjStyle.make().color(0xff5020).size(0.42f).life(100).explode(3f).homing(0.3f).trail(ParticleTypes.SMOKE);
    static final ProjStyle FRAG = ProjStyle.make().color(0x5b6b2f).size(0.28f).life(32).explode(3.5f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle BFG_BALL = ProjStyle.make().color(0x50ff50).size(1.8f).life(90).explode(6.5f).trail(ParticleTypes.HAPPY_VILLAGER)
            .onTick(p -> {
                if (p.age2 % 3 != 0 || !(p.getOwner() instanceof SlayerEntity o)) return;
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, 0x50ff50, 1.6f, p.position(), 6, 0.6);
                for (LivingEntity t : Combat.around(o, p.position(), 8)) {
                    Fx.beam(l, p.position(), t.position().add(0, t.getBbHeight() / 2, 0), 0x60ff60, 0.9f, 0.4);
                    Combat.damage(o, t, 4);
                }
            })
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 5f);
                Fx.dustBurst(l, 0x50ff50, 2.5f, pos, 60, 3);
                for (int i = 0; i < 4; i++) Fx.ring(l, pos.add(0, 0.2 + i * 0.5, 0), 2 + i * 1.3f, 0x60ff60, 1.5f, 28);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 2.5f, 0.6f);
            });

    public Doom2016Slayer() {
        super("doom2016_slayer", "Doom Slayer", "ドゥームスレイヤー", "DOOM (2016)");
        eggPrimary = 0x3f4a28; eggSecondary = GREEN;
        maxHealth = 100; speed = 0.32; armor = 16; knockbackRes = 0.6;
        scale = 1.08f; width = 0.7f; preferredRange = 9f; dmgMult = 1.1f;
        idleWeaponR = "shotgun";
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
    static void flash(SlayerEntity e, int rgb) {
        Vec3 m = mz(e);
        Fx.burst(L(e), ParticleTypes.FLAME, m, 3, 0.05, 0.02);
        Fx.dustBurst(L(e), rgb, 1.2f, m, 4, 0.08);
    }
    static void shell(SlayerEntity e) { Fx.burst(L(e), ParticleTypes.SMOKE, Combat.muzzle(e, 0.4, 0.5, 0.1), 2, 0.03, 0.03); }
    static void pellets(SlayerEntity e, LivingEntity t, int n, double spread, float dmg, int rgb) {
        Vec3 m = mz(e);
        for (int i = 0; i < n; i++) Combat.hitscan(e, m, aim(e, t, spread), 22, dmg, 0, rgb);
    }
    /** pin a glory-kill / chainsaw victim in front of the slayer */
    static void hold(SlayerEntity e, LivingEntity t, double dist, double lift) {
        if (t == null || !Combat.isFoe(e, t)) return;
        Vec3 f = Vec3.directionFromRotation(0, e.getYRot());
        Vec3 p = e.position().add(f.scale(dist + t.getBbWidth() * 0.5)).add(0, lift, 0);
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true;
    }
    static void gore(SlayerEntity e, LivingEntity t, int n) {
        if (t == null) return;
        Fx.dustBurst(L(e), BLOOD, 1.4f, mid(t), n, 0.35);
        Fx.burst(L(e), ParticleTypes.DAMAGE_INDICATOR, mid(t), n / 3, 0.3, 0.1);
    }
    /** finishing blow: kills, sprays gore, drops "health" (heals the slayer) */
    static void execute(SlayerEntity e, LivingEntity t, SoundEvent s) {
        if (t == null) return;
        gore(e, t, 30);
        Combat.damage(e, t, t.getHealth() + 100);
        snd(e, s, 1.2f, 0.8f); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.7f);
        e.heal(6);
        Fx.dustBurst(L(e), HEALTH, 1.2f, t.position().add(0, 0.6, 0), 14, 0.5);
    }
    static void zap(ServerLevel l, Vec3 a, Vec3 b, int rgb) {
        Vec3 prev = a; int segs = 9;
        for (int i = 1; i <= segs; i++) {
            Vec3 q = a.lerp(b, i / (double) segs);
            if (i < segs) q = q.add((l.random.nextDouble() - .5) * .7, (l.random.nextDouble() - .5) * .7, (l.random.nextDouble() - .5) * .7);
            Fx.beam(l, prev, q, rgb, 0.8f, 0.25); prev = q;
        }
        Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, b, 14, 0.3, 0.4);
    }
    static Vec3 endOf(SlayerEntity e, LivingEntity t, Vec3 dir, double range) { return t != null ? mid(t) : mz(e).add(dir.scale(range)); }

    // ================= helpers (anim) =================
    /** blend weight: raise over [0,in], lower over [out,1] */
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** two-handed shouldered weapon, bladed stance (chest turned right, left shoulder forward) */
    static void rifle(Pose p, float k, float pitch, float recoil) {
        float tw = 0.25f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.15f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.05f - recoil * 0.22f, k); p.lArmY = lerp(p.lArmY, 0.45f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * k; p.bodyX += (recoil * 0.12f - pitch * 0.2f) * k; p.headX -= recoil * 0.08f * k;
        p.rootZ += recoil * 0.9f * k;
    }
    /** heavy weapon held at the hip (chaingun, BFG, chainsaw) */
    static void hip(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -1.0f + pitch - recoil * 0.2f, k); p.rArmY = lerp(p.rArmY, -0.2f, k); p.rArmZ = lerp(p.rArmZ, 0.15f, k);
        p.lArmX = lerp(p.lArmX, -1.3f + pitch - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.7f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX = lerp(p.rHandX, -0.55f, k);
        p.bodyX += (0.12f + recoil * 0.1f) * k; p.bodyY += 0.15f * k; p.headY -= 0.15f * k; p.rootZ += recoil * 0.7f * k;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.32f, k); p.rLegX = lerp(p.rLegX, 0.28f, k);
        p.lLegZ -= 0.06f * k; p.rLegZ += 0.08f * k; p.rootY -= 0.4f * k;
    }
    static void wide(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.2f, k); p.rLegX = lerp(p.rLegX, 0.15f, k);
        p.lLegZ -= 0.3f * k; p.rLegZ += 0.3f * k; p.rootY -= 1.2f * k;
    }
    static void noLegs(Pose p) { p.rLegX = 0; p.lLegX = 0; }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // shotgun carried low across the body, both hands on it
        p.rArmX = -0.55f + p.rArmX * 0.25f; p.rArmY = -0.25f;
        p.lArmX = -0.75f + p.lArmX * 0.25f; p.lArmY = 0.65f; p.lArmZ = 0.05f;
        p.rHandX = -0.35f; p.bodyX += 0.04f;
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- melee: punch (left jab -> right haymaker) ----------
        out.add(AttackDef.of("punch").range(0, 3.2f).cd(18).dur(16).weapon("none")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.PLAYER_ATTACK_WEAK, 1, 1.2f);
                    if (n == 4 && t != null) Combat.damage(e, t, 3);
                    if (n == 8) {
                        Fx.arc(L(e), e.position().add(0, 1.4, 0), e.getYRot(), 1.4f, 70, -30, 15, 0xffffff, 0.8f, 10);
                        for (LivingEntity f : Combat.cone(e, 3.4, 55)) {
                            Combat.damage(e, f, 8); Combat.knock(f, e.position(), 0.9, 0.3); Fx.sparks(L(e), mid(f), 6);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.lArmX = track(t, 0, -0.9f, .15f, -0.6f, .25f, -1.6f, .38f, -0.9f, 1, -0.9f); p.lArmY = 0.5f; p.lArmZ = -0.1f;
                    p.rArmX = track(t, 0, -0.8f, .3f, 0.35f, .5f, -1.7f, .7f, -1.5f, 1, -0.6f);
                    p.rArmZ = track(t, 0, 0.1f, .3f, 0.55f, .5f, -0.05f, 1, 0.1f);
                    p.rArmY = track(t, 0, 0, .5f, -0.35f, 1, 0);
                    p.bodyY = track(t, 0, 0, .25f, -0.25f, .38f, 0.55f, .52f, -0.55f, .75f, -0.4f, 1, 0);
                    p.bodyX += track(t, 0, 0, .5f, 0.28f, 1, 0);
                    p.headY -= p.bodyY * 0.6f;
                    p.lLegX = track(t, 0, 0, .3f, -0.45f, .8f, -0.45f, 1, 0); p.rLegX = track(t, 0, 0, .5f, 0.4f, .8f, 0.3f, 1, 0);
                    p.rootZ = -2.2f * bump(win(t, .3f, .8f)); p.rootY -= 0.6f * bump(win(t, .3f, .9f));
                }));

        // ---------- GLORY KILL 1: rip-and-tear (rip the arm off, beat them with it) ----------
        out.add(AttackDef.of("glory_rip_and_tear").range(0, 3.5f).lowHealthTarget(0.25f).weight(6).cd(60).dur(36).weapon("none")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.5, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.7f); }
                    hold(e, t, 1.0, 0);
                    if (n == 16 && t != null) {
                        gore(e, t, 18); snd(e, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 0.9f, 1.3f); snd(e, SoundEvents.SLIME_SQUISH, 1.2f, 0.6f);
                        Combat.damage(e, t, Math.max(0, t.getHealth() - 1));
                    }
                    if (n == 27) execute(e, t, SoundEvents.BONE_BLOCK_BREAK);
                })
                .anim((p, t, a) -> {
                    p.weaponR = t > .44f ? "limb" : null;
                    p.lArmX = track(t, 0, 0, .12f, -1.7f, .44f, -1.5f, .5f, -0.6f, 1, 0); p.lArmY = track(t, 0, 0, .12f, 0.3f, .5f, 0, 1, 0);
                    p.rArmX = track(t, 0, 0, .15f, -1.4f, .42f, -1.3f, .48f, 0.6f, .62f, -2.9f, .74f, -2.9f, .8f, -0.7f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .2f, -0.35f, .42f, -0.35f, .5f, 0.4f, .7f, 0.2f, 1, 0);
                    p.rHandX = track(t, .5f, 0, .7f, -0.6f, .8f, 0.3f, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, -0.35f, .42f, -0.35f, .5f, 0.75f, .7f, 0.3f, .8f, -0.2f, 1, 0);
                    p.bodyX = track(t, 0, 0, .12f, 0.25f, .42f, 0.25f, .5f, -0.2f, .72f, -0.25f, .8f, 0.5f, 1, 0);
                    p.headY = -p.bodyY * 0.5f; p.headX += track(t, .7f, 0, .8f, 0.35f, 1, 0);
                    p.rootZ = track(t, 0, 0, .12f, -2.5f, .45f, -2.5f, .5f, -1f, 1, 0);
                    p.rootY = track(t, .72f, 0, .8f, -1.2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .12f, -0.5f, .8f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .48f, 0.5f, .8f, 0.4f, 1, 0);
                    p.bodyZ = 0.06f * (float) Math.sin(a * 2.5f) * win(t, .3f, .44f);
                }));

        // ---------- GLORY KILL 2: head crush (lift by the skull, squeeze) ----------
        out.add(AttackDef.of("glory_head_crush").range(0, 3.5f).lowHealthTarget(0.25f).weight(6).cd(60).dur(32).weapon("none")
                .tick((e, t, n) -> {
                    if (n == 0) snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.6f);
                    hold(e, t, 0.9, n > 5 && n < 23 ? 0.25 + 0.04 * Math.min(n - 5, 10) : 0);
                    if (n > 12 && n < 22 && n % 3 == 0) { gore(e, t, 3); snd(e, SoundEvents.SKELETON_HURT, 0.6f, 0.5f); }
                    if (n == 22) { execute(e, t, SoundEvents.SLIME_BLOCK_BREAK); snd(e, SoundEvents.BONE_BLOCK_BREAK, 1.3f, 0.6f); }
                })
                .anim((p, t, a) -> {
                    float sq = win(t, .45f, .69f);
                    p.rArmX = track(t, 0, 0, .2f, -2.0f, .7f, -2.1f, .85f, -1.3f, 1, 0); p.lArmX = p.rArmX;
                    p.rArmZ = track(t, 0, 0, .2f, 0.4f, .45f, -0.05f, .69f, -0.45f, .8f, -0.2f, 1, 0); p.lArmZ = -p.rArmZ;
                    p.bodyX = track(t, 0, 0, .2f, -0.2f, .6f, -0.25f, .72f, 0.25f, 1, 0);
                    p.bodyZ = 0.07f * (float) Math.sin(a * 3.1f) * sq; p.rArmY = 0.06f * (float) Math.sin(a * 2.9f) * sq; p.lArmY = -p.rArmY;
                    p.headX = track(t, 0, 0, .2f, -0.35f, .7f, -0.35f, .8f, 0.15f, 1, 0); p.headY = 0;
                    p.rootY = track(t, 0, 0, .25f, 0.6f, .69f, 0.6f, .75f, -0.8f, 1, 0);
                    p.lLegX = track(t, 0, 0, .15f, -0.35f, .8f, -0.35f, 1, 0); p.rLegX = track(t, 0, 0, .15f, 0.3f, .8f, 0.3f, 1, 0);
                    p.lLegZ = -0.12f * sq; p.rLegZ = 0.12f * sq;
                }));

        // ---------- GLORY KILL 3: neck snap ----------
        out.add(AttackDef.of("glory_neck_snap").range(0, 3.5f).lowHealthTarget(0.25f).weight(6).cd(60).dur(26).weapon("none")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.45, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.8f); }
                    hold(e, t, 0.8, 0);
                    if (n == 17) { execute(e, t, SoundEvents.BONE_BLOCK_BREAK); snd(e, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 1, 1.4f); }
                })
                .anim((p, t, a) -> {
                    p.lArmX = track(t, 0, 0, .2f, -1.55f, .8f, -1.5f, 1, 0); p.lArmY = track(t, 0, 0, .2f, 0.55f, .8f, 0.5f, 1, 0);
                    p.rArmX = track(t, 0, 0, .2f, -1.75f, .8f, -1.6f, 1, 0); p.rArmY = track(t, 0, 0, .2f, -0.45f, .8f, -0.4f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .5f, -0.2f, .6f, -0.25f, .67f, 0.45f, .85f, 0.3f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .5f, -0.25f, .6f, -0.3f, .67f, 0.4f, .85f, 0.25f, 1, 0);
                    p.bodyY = trackLin(t, 0, 0, .2f, 0, .55f, -0.4f, .62f, -0.45f, .67f, 0.65f, .8f, 0.55f, 1, 0);
                    p.bodyX = track(t, 0, 0, .2f, 0.3f, .67f, 0.35f, 1, 0);
                    p.headY = -p.bodyY * 0.3f; p.headX = 0.3f * win(t, .1f, .3f) * (1 - win(t, .85f, 1));
                    p.rootYaw = 0.25f * bump(win(t, .6f, .9f));
                    p.rootZ = track(t, 0, 0, .2f, -2f, .85f, -2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .2f, -0.4f, .85f, -0.4f, 1, 0); p.rLegX = track(t, 0, 0, .2f, 0.35f, .85f, 0.35f, 1, 0);
                }));

        // ---------- GLORY KILL 4: kick down + stomp ----------
        out.add(AttackDef.of("glory_stomp").range(0, 3.5f).lowHealthTarget(0.25f).weight(6).cd(60).dur(34).weapon("none")
                .tick((e, t, n) -> {
                    hold(e, t, n < 8 ? 1.1 : 1.4, 0);
                    if (n == 8 && t != null) { Combat.damage(e, t, Math.max(0, t.getHealth() - 1)); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f); Fx.sparks(L(e), mid(t), 8); }
                    if (n == 24 && t != null) {
                        execute(e, t, SoundEvents.SLIME_BLOCK_BREAK); snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.6f);
                        Fx.ring(L(e), t.position().add(0, 0.1, 0), 1.4f, 0x5a4a3a, 1.2f, 20); Fx.burst(L(e), ParticleTypes.POOF, t.position(), 12, 0.4, 0.05);
                    }
                })
                .anim((p, t, a) -> {
                    p.rLegX = track(t, 0, 0, .12f, 0.45f, .24f, -1.5f, .34f, -0.2f, .45f, 0, .58f, -1.75f, .66f, -1.75f, .7f, 0.1f, .85f, 0.1f, 1, 0);
                    p.lLegX = track(t, 0, 0, .24f, 0.15f, .45f, 0, .7f, 0.15f, 1, 0);
                    p.bodyX = track(t, 0, 0, .12f, 0.1f, .24f, -0.3f, .34f, 0, .58f, 0.1f, .7f, 0.5f, .85f, 0.45f, 1, 0);
                    p.rootY = track(t, 0, 0, .45f, 0, .6f, 1.8f, .7f, -1.2f, .85f, -0.8f, 1, 0);
                    p.rootZ = track(t, 0, 0, .24f, 0.8f, .45f, 0, .7f, -1.5f, 1, 0);
                    p.rArmX = track(t, 0, 0, .24f, -0.6f, .58f, -0.9f, .7f, 0.3f, 1, 0); p.rArmZ = track(t, 0, 0.1f, .24f, 0.7f, .7f, 0.5f, 1, 0.1f);
                    p.lArmX = track(t, 0, 0, .24f, -0.5f, .58f, -0.9f, .7f, 0.3f, 1, 0); p.lArmZ = track(t, 0, -0.1f, .24f, -0.7f, .7f, -0.5f, 1, -0.1f);
                    p.headX = track(t, 0, 0, .55f, 0.2f, .7f, 0.6f, 1, 0); p.headY = 0;
                    p.bodyY = track(t, 0, 0, .24f, 0.25f, .45f, 0, 1, 0);
                }));

        // ---------- GLORY KILL 5: combat knife through the skull ----------
        out.add(AttackDef.of("glory_knife_skull").range(0, 3.5f).lowHealthTarget(0.25f).weight(6).cd(60).dur(28).weapon("knife")
                .tick((e, t, n) -> {
                    if (n == 0) snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.9f);
                    hold(e, t, 0.9, 0);
                    if (n == 6) snd(e, SoundEvents.TRIDENT_RETURN, 0.8f, 1.6f);
                    if (n == 15) execute(e, t, SoundEvents.TRIDENT_HIT);
                    if (n == 22 && t != null) { gore(e, t, 10); snd(e, SoundEvents.SLIME_SQUISH, 1, 0.8f); }
                })
                .anim((p, t, a) -> {
                    p.lArmX = track(t, 0, 0, .15f, -1.6f, .8f, -1.5f, 1, 0); p.lArmY = track(t, 0, 0, .15f, 0.35f, 1, 0);
                    p.rArmX = track(t, 0, 0, .15f, -0.6f, .4f, -2.95f, .52f, -1.05f, .7f, -1.0f, .8f, -1.9f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .4f, 0.25f, .52f, -0.2f, .8f, 0, 1, 0);
                    p.rArmY = 0.35f * (float) Math.sin(win(t, .55f, .72f) * PI * 2);
                    p.rHandX = track(t, 0, 0, .3f, 1.3f, .7f, 1.3f, .85f, 0.3f, 1, 0);
                    p.bodyX = track(t, 0, 0, .4f, -0.18f, .52f, 0.4f, .8f, 0.3f, 1, 0);
                    p.bodyY = track(t, 0, 0, .4f, 0.3f, .52f, -0.2f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.headX = track(t, .45f, 0, .55f, 0.3f, 1, 0);
                    p.rootY = track(t, .45f, 0, .52f, -1.0f, .8f, -0.6f, 1, 0); p.rootZ = track(t, 0, 0, .15f, -1.6f, .85f, -1.6f, 1, 0);
                    p.lLegX = track(t, 0, 0, .15f, -0.45f, .85f, -0.45f, 1, 0); p.rLegX = track(t, 0, 0, .15f, 0.35f, .85f, 0.35f, 1, 0);
                }));

        // ---------- Chainsaw (rev, lunge, grind, rip upward) ----------
        out.add(AttackDef.of("chainsaw").range(0, 4).cd(300).weight(3).dur(42).weapon("chainsaw")
                .when((e, t) -> t.getMaxHealth() <= 60 || t.getHealth() <= 30)
                .tick((e, t, n) -> {
                    if (n < 12 && n % 3 == 0) { snd(e, SoundEvents.MINECART_RIDING, 0.8f, 1.6f + n * 0.03f); Fx.burst(L(e), ParticleTypes.SMOKE, mz(e), 3, 0.1, 0.02); }
                    if (n == 12) { Combat.lunge(e, 0.7, 0.1); snd(e, SoundEvents.SHEEP_SHEAR, 1.2f, 0.5f); }
                    if (n >= 14 && n < 34) {
                        hold(e, t, 1.0, 0);
                        if (n % 2 == 0 && t != null) {
                            Combat.damage(e, t, Math.min(2.5f, Math.max(0, t.getHealth() - 1)));
                            gore(e, t, 6); Fx.sparks(L(e), mid(t), 3);
                            snd(e, SoundEvents.SHEEP_SHEAR, 0.9f, 0.6f + e.getRandom().nextFloat() * 0.3f);
                        }
                        if (n % 4 == 0) snd(e, SoundEvents.MINECART_RIDING, 1f, 2f);
                    }
                    if (n == 34) { execute(e, t, SoundEvents.SLIME_BLOCK_BREAK); e.heal(4); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), saw = win(t, .33f, .8f) * (1 - win(t, .8f, .82f));
                    float pitch = 0.3f + 0.15f * (float) Math.sin(a * 1.3f) * saw - 0.9f * bump(win(t, .8f, .92f));
                    hip(p, k, pitch, 0);
                    float vib = 0.035f * (float) Math.sin(a * 5.3f) * k;
                    p.rHandZ += vib; p.bodyZ += vib * 0.5f; p.rArmY += vib;
                    p.rootZ += track(t, 0, 0, .28f, 1f, .33f, -3f, .8f, -2.5f, 1, 0);
                    p.bodyX += 0.25f * saw - 0.25f * bump(win(t, .8f, .95f)); p.headX += 0.2f * saw;
                    stance(p, k);
                    p.lLegX = lerp(p.lLegX, -0.6f, win(t, .28f, .33f) * (1 - win(t, .85f, 1)));
                }));

        // ---------- Pistol (3 shots + charged final shot) ----------
        out.add(AttackDef.of("pistol").range(3, 24).cd(30).dur(22).weapon("pistol")
                .tick((e, t, n) -> {
                    if (n == 3 || n == 7) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.02), 24, 3.5f, 0, PLASMA_C); flash(e, PLASMA_C);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 1.9f);
                    }
                    if (n > 8 && n < 15) Fx.dustBurst(L(e), PLASMA_C, 0.8f, mz(e), 2, 0.1);
                    if (n == 12) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.6f, 2f);
                    if (n == 15) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.005), 28, 8f, 1, 0x80e0ff); flash(e, 0x80e0ff);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .85f), r = kick(t, 3 / 22f, .12f) + kick(t, 7 / 22f, .12f) + 1.6f * kick(t, 15 / 22f, .2f);
                    p.bodyY -= 0.35f * k; p.headY += 0.35f * k;
                    p.rArmX = lerp(p.rArmX, -HALF - r * 0.35f, k); p.rArmY = lerp(p.rArmY, 0.35f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
                    p.lArmX = lerp(p.lArmX, -0.25f, k); p.lArmZ = lerp(p.lArmZ, -0.3f, k);
                    p.rHandX -= r * 0.3f; p.bodyX -= r * 0.06f; p.rootZ += r * 0.5f;
                    p.lLegX = lerp(p.lLegX, 0.25f, k); p.rLegX = lerp(p.rLegX, -0.2f, k);
                }));

        // ---------- Combat Shotgun (pump) ----------
        out.add(AttackDef.of("shotgun").range(1.5f, 11).cd(26).weight(3).dur(22).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 5) {
                        pellets(e, t, 8, 0.07, 2.6f, 0xffd070); flash(e, 0xffa040);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.2f, 0.9f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.3f, 1.8f);
                    }
                    if (n == 11) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.8f);
                    if (n == 15) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.9f); shell(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = kick(t, 5 / 22f, .25f), pump = bump(win(t, .48f, .72f));
                    rifle(p, k, 0, r * 1.4f); stance(p, k);
                    p.lArmX += 0.35f * pump * k; p.lArmY -= 0.1f * pump; p.rHandX -= r * 0.35f;
                    p.bodyZ += 0.05f * pump; p.headX += 0.05f * pump;
                }));

        // ---------- Combat Shotgun: Tactical / Charged burst (3 rounds) ----------
        out.add(AttackDef.of("shotgun_tactical_burst").range(2, 12).cd(70).weight(2).dur(30).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n < 9 && n % 2 == 0) { Fx.dustBurst(L(e), 0xffb040, 0.9f, mz(e), 3, 0.15); snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 0.5f, 1.2f + n * 0.1f); }
                    if (n == 9 || n == 12 || n == 15) {
                        pellets(e, t, 7, 0.06, 2.3f, 0xffd070); flash(e, 0xffa040); shell(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.1f, 1.1f);
                    }
                    if (n == 22) snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.8f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f);
                    float r = kick(t, 9 / 30f, .1f) + kick(t, 12 / 30f, .1f) + kick(t, 15 / 30f, .14f);
                    rifle(p, k, -0.03f * win(t, 0, .3f), r * 1.2f); stance(p, k);
                    p.headX += 0.12f * win(t, .05f, .28f) * (1 - win(t, .6f, .8f)); // cheek on stock while charging
                    p.bodyZ += 0.04f * (float) Math.sin(a * 2f) * (1 - win(t, .3f, .35f)) * k;
                    float pump = bump(win(t, .65f, .82f)); p.lArmX += 0.35f * pump * k;
                    p.rootZ += r * 0.4f;
                }));

        // ---------- Super Shotgun (double blast, break-open reload) ----------
        out.add(AttackDef.of("super_shotgun").range(0, 8).cd(40).weight(3).dur(32).weapon("ssg")
                .tick((e, t, n) -> {
                    if (n == 5) {
                        pellets(e, t, 14, 0.1, 2.8f, 0xffc060); flash(e, 0xffa040); flash(e, 0xff6020);
                        for (LivingEntity f : Combat.cone(e, 4, 40)) Combat.knock(f, e.position(), 1.2, 0.35);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.5f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.4f, 0.6f);
                    }
                    if (n == 13) { snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.8f, 1.4f); shell(e); shell(e); }
                    if (n == 19) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.8f, 1.5f);
                    if (n == 24) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.9f, 1.3f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .92f), r = kick(t, 5 / 32f, .3f), brk = win(t, .38f, .45f) * (1 - win(t, .72f, .77f));
                    rifle(p, k, 0.35f * brk, r * 2.2f); stance(p, k);
                    p.rHandX += 0.9f * brk - r * 0.6f; p.bodyX -= r * 0.25f; p.rootY += r * 0.6f;
                    // left hand drops to belt for shells, then slaps them in
                    float load = bump(win(t, .48f, .7f));
                    p.lArmX += 1.0f * load * k; p.lArmZ -= 0.3f * load; p.headX += 0.25f * brk;
                    p.rArmZ += 0.15f * bump(win(t, .72f, .8f));
                }));

        // ---------- Heavy Assault Rifle: Tactical Scope (zoom, 3 precise shots) ----------
        out.add(AttackDef.of("har_tactical_scope").range(7, 32).cd(55).weight(2).dur(32).weapon("har")
                .tick((e, t, n) -> {
                    if (n == 6) snd(e, SoundEvents.SPYGLASS_USE, 1, 1);
                    if (n == 13 || n == 16 || n == 19) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.004), 34, 7.5f, 0, 0xffe080); flash(e, 0xffc060);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 1.1f, 1.3f);
                    }
                    if (n == 24) snd(e, SoundEvents.SPYGLASS_STOP_USING, 1, 1);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .88f), zoom = win(t, .15f, .35f) * (1 - win(t, .72f, .82f));
                    float r = kick(t, 13 / 32f, .08f) + kick(t, 16 / 32f, .08f) + kick(t, 19 / 32f, .08f);
                    rifle(p, k, 0, r); stance(p, k);
                    p.headX += 0.2f * zoom; p.headY -= 0.1f * zoom; p.bodyX += 0.12f * zoom; p.rootY -= 0.6f * zoom; // crouch into scope
                    p.rArmX -= 0.05f * zoom; p.lArmX -= 0.05f * zoom;
                }));

        // ---------- Heavy Assault Rifle: Micro Missiles ----------
        out.add(AttackDef.of("har_micro_missiles").range(4, 26).cd(80).weight(2).dur(30).weapon("har")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.PISTON_EXTEND, 0.7f, 1.8f);
                    if (n >= 6 && n <= 16 && n % 2 == 0) {
                        Vec3 m = Combat.muzzle(e, 1.0, 0.5, 0.15);
                        Combat.shoot(e, MICRO_MISSILE, m, aim(e, t, 0.05), 1.3, 3f);
                        Fx.burst(L(e), ParticleTypes.SMOKE, m, 3, 0.05, 0.02);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.9f, 1.6f);
                    }
                    if (n == 22) snd(e, SoundEvents.PISTON_CONTRACT, 0.7f, 1.8f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .88f), fire = win(t, .2f, .55f);
                    float r = 0.5f * Math.abs((float) Math.sin(t * 30 * PI / 2)) * fire * (1 - win(t, .55f, .6f));
                    rifle(p, k, -0.06f, r); stance(p, k);
                    p.bodyZ += 0.03f * (float) Math.sin(a * 4) * fire * (1 - win(t, .55f, .6f));
                    p.lArmY += 0.1f * bump(win(t, .05f, .2f)); // flick the attachment open
                }));

        // ---------- Plasma Rifle (rapid bolts) ----------
        out.add(AttackDef.of("plasma_rifle").range(3, 22).cd(30).weight(3).dur(26).weapon("plasma")
                .tick((e, t, n) -> {
                    if (n >= 3 && n <= 21 && n % 2 == 1) {
                        Combat.shoot(e, PLASMA_BOLT, mz(e), aim(e, t, 0.03), 1.7, 2.6f);
                        Fx.dustBurst(L(e), PLASMA_C, 0.9f, mz(e), 3, 0.06);
                        snd(e, SoundEvents.BLAZE_SHOOT, 0.5f, 1.9f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .1f, .12f) * (1 - win(t, .82f, .84f));
                    float r = 0.35f * (0.5f + 0.5f * (float) Math.sin(a * PI)) * fire;
                    rifle(p, k, 0, r); stance(p, k);
                    p.rHandZ += 0.03f * (float) Math.sin(a * 3.3f) * fire;
                }));

        // ---------- Plasma Rifle: Heat Blast (charge, then large blast) ----------
        out.add(AttackDef.of("plasma_heat_blast").range(0, 7).cd(120).weight(2).dur(36).weapon("plasma")
                .tick((e, t, n) -> {
                    if (n < 22) {
                        if (n % 3 == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.6f, 0.8f + n * 0.05f);
                        Fx.burst(L(e), ParticleTypes.FLAME, mz(e), 2, 0.2 - n * 0.008, 0.01);
                        Fx.dustBurst(L(e), 0xff7020, 1f, mz(e), 2, 0.25);
                    }
                    if (n == 23) {
                        Vec3 f = e.getViewVector(1f), c = e.position().add(0, 1, 0).add(f.scale(2.5));
                        for (LivingEntity x : Combat.cone(e, 7, 60)) { Combat.damage(e, x, 12); Combat.knock(x, e.position(), 1.4, 0.4); x.setSecondsOnFire(3); }
                        Combat.blast(e, c, 3.5f, 6, 1.0f, null);
                        Fx.explosion(L(e), c, 3f); Fx.ring(L(e), c, 2.5f, 0xff9030, 2f, 30); Fx.ring(L(e), c, 4f, 0xff5010, 1.6f, 36);
                        Fx.burst(L(e), ParticleTypes.FLAME, c, 60, 1.6, 0.15);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 1.5f, 1.2f); snd(e, SoundEvents.FIRECHARGE_USE, 1.5f, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), ch = win(t, .1f, .62f), r = 2.4f * kick(t, 23 / 36f, .3f);
                    rifle(p, k, 0.12f * ch * (1 - win(t, .6f, .64f)), r); stance(p, k);
                    float shake = 0.05f * ch * ch * (1 - win(t, .63f, .64f));
                    p.bodyZ += shake * (float) Math.sin(a * 4.5f); p.rArmZ += shake * (float) Math.cos(a * 5.1f);
                    p.rootY -= 0.8f * ch * (1 - win(t, .63f, .75f)); p.bodyX += 0.15f * ch - r * 0.15f; p.rootZ += r * 0.8f;
                }));

        // ---------- Rocket Launcher ----------
        out.add(AttackDef.of("rocket_launcher").range(6, 30).cd(45).weight(2).dur(24).weapon("rocket")
                .tick((e, t, n) -> {
                    if (n == 6) {
                        Combat.shoot(e, ROCKET16, mz(e), aim(e, t, 0.01), 1.3, 14f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.6, 0.32, 0), 6, 0.2, 0.05); flash(e, 0xffa040);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.6f); snd(e, SoundEvents.GHAST_SHOOT, 0.5f, 1.4f);
                    }
                    if (n == 16) snd(e, SoundEvents.PISTON_EXTEND, 0.6f, 1.2f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 2f * kick(t, 6 / 24f, .35f);
                    rifle(p, k, -0.05f, r); stance(p, k);
                    p.rHandX -= r * 0.5f; p.bodyX -= r * 0.12f; p.headX -= r * 0.1f;
                    p.lArmX += 0.25f * bump(win(t, .6f, .8f)); // racks next rocket
                }));

        // ---------- Rocket Launcher: Lock-On Burst ----------
        out.add(AttackDef.of("rocket_lock_on_burst").range(8, 32).cd(140).weight(2).dur(42).weapon("rocket")
                .tick((e, t, n) -> {
                    if ((n == 4 || n == 10 || n == 16) && t != null) {
                        snd(e, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f + n * 0.03f);
                        Fx.ring(L(e), mid(t), 0.9f, RED, 1f, 16); Fx.ring(L(e), mid(t).add(0, 0.3, 0), 0.6f, RED, 1f, 12);
                    }
                    if (n == 19) snd(e, SoundEvents.NOTE_BLOCK_PLING.value(), 1, 2f);
                    if (n == 22 || n == 26 || n == 30) {
                        Vec3 d = aim(e, t, 0.15).add(0, 0.25, 0).normalize();
                        Combat.shoot(e, LOCK_ROCKET, mz(e), d, 0.9, 10f);
                        flash(e, 0xffa040); snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), lock = win(t, .05f, .45f) * (1 - win(t, .5f, .55f));
                    float r = 1.5f * (kick(t, 22 / 42f, .1f) + kick(t, 26 / 42f, .1f) + kick(t, 30 / 42f, .12f));
                    rifle(p, k, -0.18f * win(t, .45f, .52f), r); stance(p, k);
                    p.headX += 0.15f * lock; p.bodyX += 0.08f * lock; p.rHandX -= r * 0.4f;
                    p.headY += 0.04f * (float) Math.sin(a * 0.9f) * lock; // tracking the target
                }));

        // ---------- Static Cannon (charge by moving, lightning discharge) ----------
        out.add(AttackDef.of("static_cannon").range(4, 24).cd(90).weight(2).dur(34).weapon("static")
                .tick((e, t, n) -> {
                    if (n < 22) {
                        Vec3 c = e.position().add(0, 1, 0); double ang = n * 0.7;
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, c.add(Math.cos(ang) * 0.8, n * 0.03, Math.sin(ang) * 0.8), 2, 0.05, 0.05);
                        Fx.dustBurst(L(e), 0x80d0ff, 0.8f, mz(e), 1, 0.12);
                        if (n % 4 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.8f, 1f + n * 0.04f);
                    }
                    if (n == 24) {
                        Vec3 d = aim(e, t, 0.003), m = mz(e);
                        Combat.hitscan(e, m, d, 28, 17f, 1, 0xa0e8ff);
                        zap(L(e), m, endOf(e, t, d, 28), 0xa0e8ff); zap(L(e), m, endOf(e, t, d, 28), 0xffffff);
                        snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2f, 1.4f); snd(e, SoundEvents.TRIDENT_THUNDER, 0.5f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), ch = win(t, 0, .65f) * (1 - win(t, .68f, .7f)), r = 1.6f * kick(t, 24 / 34f, .25f);
                    rifle(p, k, 0, r); stance(p, k * (1 - ch));
                    // stepping in place to build static charge
                    float step = (float) Math.sin(a * 0.9f) * ch;
                    p.rLegX = lerp(p.rLegX, 0.5f * step, ch); p.lLegX = lerp(p.lLegX, -0.5f * step, ch);
                    p.rootY += Math.abs(step) * 0.6f; p.bodyZ += 0.04f * step;
                    p.rootZ += r * 0.6f;
                }));

        // ---------- Gauss Cannon: Siege Mode (brace, long charge, devastating piercing beam) ----------
        out.add(AttackDef.of("gauss_siege_mode").range(6, 40).cd(200).weight(2).dur(52).weapon("gauss")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.PISTON_EXTEND, 1, 0.6f);
                    if (n >= 14 && n < 40) {
                        if (n % 5 == 0) snd(e, SoundEvents.BEACON_ACTIVATE, 0.7f, 0.8f + (n - 14) * 0.04f);
                        Vec3 m = mz(e); double r = 0.6 - (n - 14) * 0.02;
                        Fx.dustBurst(L(e), 0x40a0ff, 1f, m, 4, Math.max(0.05, r));
                        Fx.burst(L(e), ParticleTypes.END_ROD, m, 1, 0.05, 0.01);
                    }
                    if (n == 40) {
                        Vec3 d = aim(e, t, 0), m = mz(e);
                        Combat.hitscan(e, m, d, 44, 30f, 6, 0x60c0ff);
                        Fx.beam(L(e), m, m.add(d.scale(44)), 0xffffff, 1.6f, 0.4);
                        Vec3 end = endOf(e, t, d, 30);
                        Combat.blast(e, end, 3f, 10f, 1.2f, null); Fx.explosion(L(e), end, 3f);
                        Fx.ring(L(e), m, 1.2f, 0x60c0ff, 1.5f, 20);
                        e.setDeltaMovement(e.getDeltaMovement().subtract(d.x * 0.5, 0, d.z * 0.5)); e.hurtMarked = true;
                        snd(e, SoundEvents.WARDEN_SONIC_BOOM, 1.4f, 1.2f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.7f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .92f), brace = win(t, .03f, .25f) * (1 - win(t, .88f, 1));
                    float ch = win(t, .27f, .77f) * (1 - win(t, .77f, .78f)), r = 3f * kick(t, 40 / 52f, .2f);
                    rifle(p, k, 0, r); wide(p, brace);
                    p.bodyX += 0.2f * brace; p.headX += 0.15f * brace;
                    p.bodyZ += 0.04f * ch * ch * (float) Math.sin(a * 6); p.rArmY += 0.03f * ch * (float) Math.sin(a * 7);
                    p.rootZ += r * 1.4f; p.bodyX -= r * 0.15f; p.rHandX -= r * 0.25f;
                }));

        // ---------- Gauss Cannon: Precision Bolt (scoped charged shot) ----------
        out.add(AttackDef.of("gauss_precision_bolt").range(8, 40).cd(80).weight(2).dur(30).weapon("gauss")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.SPYGLASS_USE, 1, 0.8f);
                    if (n > 5 && n < 16) { Fx.dustBurst(L(e), 0x40a0ff, 0.8f, mz(e), 2, 0.1); if (n % 3 == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.5f, 1.5f + n * 0.03f); }
                    if (n == 17) {
                        Vec3 d = aim(e, t, 0), m = mz(e);
                        Combat.hitscan(e, m, d, 44, 20f, 2, 0x80d0ff); Fx.beam(L(e), m, endOf(e, t, d, 44), 0xffffff, 0.9f, 0.5);
                        snd(e, SoundEvents.TRIDENT_THUNDER, 0.6f, 2f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1f, 0.5f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), zoom = win(t, .12f, .3f) * (1 - win(t, .7f, .85f)), r = 2f * kick(t, 17 / 30f, .22f);
                    rifle(p, k, 0, r); stance(p, k);
                    p.headX += 0.2f * zoom; p.headY -= 0.12f * zoom; p.bodyX += 0.1f * zoom; p.rootY -= 0.5f * zoom;
                    p.rHandX -= r * 0.3f;
                }));

        // ---------- Vortex Rifle (scoped charge, piercing red beam) ----------
        out.add(AttackDef.of("vortex_rifle").range(10, 48).cd(90).weight(2).dur(38).weapon("vortex")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.WARDEN_SONIC_CHARGE, 0.6f, 1.6f);
                    if (n > 4 && n < 26) Fx.dustBurst(L(e), RED, 0.7f, mz(e), 1, 0.06);
                    if (n == 27) {
                        Vec3 d = aim(e, t, 0), m = mz(e);
                        Combat.hitscan(e, m, d, 50, 24f, 3, RED);
                        Fx.beam(L(e), m, endOf(e, t, d, 50), 0xff9080, 0.6f, 0.3);
                        snd(e, SoundEvents.TRIDENT_RIPTIDE_1, 1.2f, 1.5f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), zoom = win(t, .1f, .3f) * (1 - win(t, .75f, .85f)), r = 1.6f * kick(t, 27 / 38f, .2f);
                    rifle(p, k, 0, r); stance(p, k);
                    p.headX += 0.22f * zoom; p.headY -= 0.12f * zoom; p.bodyX += 0.15f * zoom; p.rootY -= 1.0f * zoom;
                    p.lLegX -= 0.15f * zoom; // kneel-ish
                    p.bodyZ += 0.01f * (float) Math.sin(a * 0.4f) * zoom; // breathing sway
                }));

        // ---------- Chaingun: Mobile Turret (spin-up, deploy, sustained fire) ----------
        out.add(AttackDef.of("chaingun_mobile_turret").range(4, 24).cd(130).weight(2).dur(72).weapon("chaingun")
                .tick((e, t, n) -> {
                    if (n < 16 && n % 3 == 0) snd(e, SoundEvents.MINECART_RIDING, 0.7f, 0.6f + n * 0.08f);
                    if (n == 10) snd(e, SoundEvents.PISTON_EXTEND, 0.9f, 0.8f);
                    if (n >= 16 && n < 64) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.05), 26, 1.7f, 0, 0xffe080);
                        if (n % 2 == 0) { flash(e, 0xffa040); shell(e); }
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.5f + e.getRandom().nextFloat() * 0.3f);
                    }
                    if (n == 64) snd(e, SoundEvents.PISTON_CONTRACT, 0.9f, 0.7f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .92f), dep = win(t, .08f, .2f) * (1 - win(t, .9f, 1)), fire = win(t, .22f, .23f) * (1 - win(t, .88f, .89f));
                    float jit = 0.04f * (float) Math.sin(a * 6.7f) * fire;
                    hip(p, k, -0.25f * dep, 0.25f * fire + jit);
                    wide(p, dep); p.bodyX += 0.12f * dep; p.rHandZ += jit; p.bodyZ += jit * 0.4f;
                    p.headX += 0.1f * dep; p.rootZ += 0.15f * fire;
                }));

        // ---------- BFG 9000 ----------
        out.add(AttackDef.of("bfg_9000").range(8, 36).cd(500).weight(1).dur(52).weapon("bfg")
                .when((e, t) -> t.getMaxHealth() >= 80 || Combat.around(e, t.position(), 6).size() >= 3)
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.BEACON_ACTIVATE, 1.4f, 0.6f);
                    if (n < 32) {
                        Vec3 m = mz(e); double ang = n * 0.6, r = 1.2 - n * 0.03;
                        Fx.dustBurst(L(e), 0x50ff50, 1.4f, m.add(Math.cos(ang) * r, Math.sin(ang) * r, 0), 3, 0.05);
                        Fx.burst(L(e), ParticleTypes.HAPPY_VILLAGER, m, 2, 0.3, 0.02);
                        if (n % 6 == 0) snd(e, SoundEvents.PORTAL_AMBIENT, 0.8f, 1.2f + n * 0.02f);
                    }
                    if (n == 32) {
                        Combat.shoot(e, BFG_BALL, mz(e), aim(e, t, 0), 0.6, 45f);
                        Fx.dustBurst(L(e), 0x80ff80, 2f, mz(e), 25, 0.6);
                        e.setDeltaMovement(e.getDeltaMovement().subtract(e.getViewVector(1f).scale(0.4))); e.hurtMarked = true;
                        snd(e, SoundEvents.ENDER_DRAGON_SHOOT, 1.6f, 0.6f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), ch = win(t, .05f, .6f) * (1 - win(t, .61f, .62f)), r = 3f * kick(t, 32 / 52f, .3f);
                    hip(p, k, -0.35f * k, r); wide(p, k * 0.8f);
                    p.bodyX -= 0.12f * ch; p.bodyZ += 0.05f * ch * ch * (float) Math.sin(a * 5);
                    p.headX -= 0.1f * ch; p.rootY += 0.5f * ch; p.rootZ += r * 1.2f; p.rHandX -= r * 0.3f;
                }));

        // ---------- Frag Grenade (left-hand lob) ----------
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(160).weight(2).dur(20).weapons("shotgun", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.5f);
                    if (n == 10) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.4);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
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
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat suit = Mat.of(0x3b4526).noise(.12f), plate = Mat.of(0x55653a).bevel(.3f).noise(.1f), dark = Mat.of(0x252a1c).noise(.1f);
        Mat metal = Mat.of(0x6c6c66).noise(.07f), black = Mat.of(0x1a1a1a), glow = Mat.of(GREEN).glow();
        Mat chest = Mat.of(0x55653a).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            int cx = x + w / 2; // white Praetor emblem: a chevron with a central bar
            for (int i = 0; i < 3; i++) { g.px(cx - 1 - i, y + 1 + i, 0xeeeeee); g.px(cx + i, y + 1 + i, 0xeeeeee); }
            g.rect(cx - 1, y + 1, 2, 4, 0xf4f4f4);
        });
        Mat helm = Mat.of(0x55653a).bevel(.28f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) g.rect(x + 1, y + h / 2, w - 2, 1, 0x2a3020);
        });
        // torso
        rig.box(rig.torso, suit, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4, -12, -3, 8, 6, 1, .3f);
        rig.box(rig.torso, glow, -1, -6, -3.6f, 2, 1, 1);
        rig.box(rig.torso, plate, -3, -5.5f, -2.7f, 6, 3, 1);
        rig.box(rig.torso, dark, -4.5f, -2.5f, -2.5f, 9, 2, 5);
        rig.box(rig.torso, metal, -1, -2.5f, -3.1f, 2, 2, 1);
        rig.box(rig.torso, plate, -3.5f, -11, 2, 7, 8, 2);
        rig.box(rig.torso, glow, -0.5f, -9, 3.8f, 1, 3, 1);
        rig.box(rig.torso, dark, -4, -13, -2.5f, 8, 1, 5);
        // head / helmet
        rig.box(rig.head, helm, -4, -8, -4, 8, 8, 8, .45f);
        rig.box(rig.head, glow, -3, -5.5f, -4.9f, 6, 2, 1);
        rig.box(rig.head, dark, -2, -3, -4.9f, 4, 3, 1);
        rig.box(rig.head, plate, -1, -9, -3, 2, 1, 6);
        rig.box(rig.head, metal, -4.9f, -5, -1.5f, 1, 3, 3);
        rig.box(rig.head, metal, 3.9f, -5, -1.5f, 1, 3, 3);
        // arms
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, suit, -2, -2, -2, 4, 12, 4);
            rig.box(arm, plate, s < 0 ? -3.5f : -2.5f, -3, -3, 6, 4, 6);
            rig.box(arm, dark, -2, 2, -2, 4, 2, 4, .25f);
            rig.box(arm, plate, -2, 5, -2, 4, 5, 4, .4f);
            rig.box(arm, glow, -0.5f, 6, -2.7f, 1, 2, 1);
        }
        // legs
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, suit, -2, 0, -2, 4, 12, 4);
            rig.box(leg, plate, -2, 1, -2.7f, 4, 4, 1);
            rig.box(leg, metal, -1.5f, 5.5f, -2.9f, 3, 2, 1);
            rig.box(leg, dark, -2, 9, -3, 4, 3, 5, .25f);
        }
        buildWeapons(rig, metal, dark, black, glow);
    }

    private static void buildWeapons(Rig rig, Mat metal, Mat dark, Mat black, Mat glow) {
        Mat gun = Mat.of(0x3a3c3e).noise(.08f), gunL = Mat.of(0x8a8c88).noise(.06f), wood = Mat.of(0x5a3a22).noise(.15f);
        Mat cyan = Mat.of(PLASMA_C).glow(), red = Mat.of(RED).glow(), blue = Mat.of(0x60b0ff).glow(), orange = Mat.of(0xb3401a);
        Mat flesh = Mat.of(0x7a3b2b).noise(.25f);
        PartDefinition w;
        w = rig.weapon(rig.rHand, "pistol");
        rig.box(w, gun, -1, -2, -7, 2, 2, 7); rig.box(w, black, -1, 0, -1, 2, 3, 2); rig.box(w, cyan, -0.5f, -2.6f, -5, 1, 1, 3);
        w = rig.weapon(rig.rHand, "shotgun");
        rig.box(w, gun, -1.5f, -2.5f, -6, 3, 3, 8); rig.box(w, metal, -1, -3, -15, 2, 1, 9);
        rig.box(w, dark, -1.5f, -2, -12, 3, 2, 5); rig.box(w, black, -1, -1.5f, 2, 2, 2, 5);
        w = rig.weapon(rig.rHand, "ssg");
        rig.box(w, black, -1.5f, -2.5f, -16, 3, 2, 11); rig.box(w, metal, -1.5f, -3, -5, 3, 3, 5); rig.box(w, wood, -1, -1.5f, 0, 2, 3, 6);
        w = rig.weapon(rig.rHand, "har");
        rig.box(w, gun, -1.5f, -3, -9, 3, 4, 11); rig.box(w, metal, -0.5f, -2, -15, 1, 1, 6); rig.box(w, black, -1, -5, -6, 2, 2, 5);
        rig.box(w, red, -0.5f, -4.5f, -6.4f, 1, 1, 1); rig.box(w, dark, -1, 1, -6, 2, 3, 2); rig.box(w, metal, 1.5f, -3, -8, 2, 2, 4);
        w = rig.weapon(rig.rHand, "plasma");
        rig.box(w, Mat.of(0x3a4450), -2, -3, -8, 4, 4, 9); rig.box(w, cyan, -2.6f, -2.5f, -6, 1, 2, 4); rig.box(w, cyan, 1.6f, -2.5f, -6, 1, 2, 4);
        rig.box(w, metal, -1, -2, -12, 2, 2, 4);
        w = rig.weapon(rig.rHand, "rocket");
        rig.box(w, dark, -1.5f, -3.5f, -13, 3, 3, 16); rig.box(w, metal, -2, -4, -14, 4, 4, 1);
        rig.box(w, black, -0.5f, 0, -1, 1, 3, 2); rig.box(w, red, -0.5f, -5, -6, 1, 1, 3);
        w = rig.weapon(rig.rHand, "gauss");
        rig.box(w, gunL, -2, -3, -10, 4, 4, 12); rig.box(w, blue, -1, -4, -11, 2, 1, 9);
        rig.box(w, metal, -1, -2, -15, 2, 2, 5); rig.box(w, gun, -1, -2, 2, 2, 3, 4);
        w = rig.weapon(rig.rHand, "static");
        rig.box(w, gun, -1.5f, -3, -8, 3, 3, 10); rig.box(w, cyan, -2.5f, -4, -6, 5, 5, 1); rig.box(w, cyan, -2.5f, -4, -3, 5, 5, 1);
        rig.box(w, metal, -0.5f, -2, -11, 1, 1, 3);
        w = rig.weapon(rig.rHand, "vortex");
        rig.box(w, gun, -1, -3, -14, 2, 3, 16); rig.box(w, black, -1, -5, -6, 2, 2, 5);
        rig.box(w, red, -1.6f, -2.5f, -12, 1, 1, 8); rig.box(w, dark, -1, -2, 2, 2, 3, 4);
        w = rig.weapon(rig.rHand, "chaingun");
        rig.box(w, gun, -2.5f, -4, -5, 5, 5, 7); rig.box(w, metal, -1.5f, -3, -15, 3, 3, 10); rig.box(w, dark, -2, -3.5f, -16, 4, 4, 1);
        rig.box(w, black, -0.5f, -6, -4, 1, 2, 4); rig.box(w, dark, -2, 1, -4, 4, 3, 3);
        w = rig.weapon(rig.rHand, "bfg");
        rig.box(w, dark, -3, -4, -9, 6, 5, 11); rig.box(w, glow, -1.5f, -5, -7, 3, 1, 5); rig.box(w, glow, -2.5f, -3.5f, -10, 4, 4, 1);
        rig.box(w, black, -0.5f, -7, -5, 1, 2, 5); rig.box(w, metal, 3, -3, -6, 1, 3, 4); rig.box(w, metal, -4, -3, -6, 1, 3, 4);
        w = rig.weapon(rig.rHand, "chainsaw");
        rig.box(w, orange, -2, -3, -4, 4, 4, 6); rig.box(w, gunL, -0.5f, -2.5f, -17, 1, 2, 13);
        rig.box(w, black, -1.5f, -3.5f, -5, 3, 1, 2); rig.box(w, black, -0.5f, -5, -3, 1, 2, 4);
        w = rig.weapon(rig.rHand, "knife");
        rig.box(w, gunL, -0.5f, -1, -7, 1, 2, 6); rig.box(w, black, -0.5f, -0.5f, -1, 1, 1, 2);
        w = rig.weapon(rig.rHand, "limb");
        rig.box(w, flesh, -1, -1, -9, 2, 2, 9); rig.box(w, Mat.of(0xd8c8a0), -1.5f, -1.5f, -10.5f, 3, 3, 2);
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, Mat.of(0x4a5a2a), -1, -1, -2, 2, 2, 2); rig.box(w, glow, -0.5f, -1.8f, -1.5f, 1, 1, 1);
    }
}
