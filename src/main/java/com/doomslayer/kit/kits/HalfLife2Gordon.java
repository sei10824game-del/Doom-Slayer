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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Gordon Freeman, Half-Life 2: HEV Mark V, crowbar, Zero-Point Energy Field Manipulator and the full City 17 arsenal. */
public class HalfLife2Gordon extends SlayerKit {
    static final int HEV = 0xe0761c, GRAV = 0xffa53a, GRAV_BLUE = 0x9fe0ff, PULSE = 0x9fd8ff, LASER = 0xff2a1a, BAIT = 0xd8c050;

    // ---------- projectiles ----------
    static final ProjStyle SMG_GRENADE = ProjStyle.make().color(0x4d5a3a).size(0.25f).life(60).gravity(0.045).explode(3.2f).trail(ParticleTypes.SMOKE);
    static final ProjStyle REBAR = ProjStyle.make().color(0xff9a40).size(0.16f).life(60).pierce(2).trail(ParticleTypes.SMALL_FLAME);
    static final ProjStyle RPG_ROCKET = ProjStyle.make().color(0xffa040).size(0.45f).life(110).explode(3.8f).homing(0.22f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle FRAG = ProjStyle.make().color(0x3e4a2c).size(0.26f).life(55).gravity(0.05).explode(3.8f).bounce().trail(ParticleTypes.SMOKE)
            .onTick(p -> { if (p.age2 % 8 == 0) Fx.dustBurst((ServerLevel) p.level(), 0xff2010, 1.3f, p.position(), 2, 0.02); });
    static final ProjStyle SAWBLADE = ProjStyle.make().color(0xc0c0c0).size(0.45f).life(50).pierce(4).trail(ParticleTypes.CRIT)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.sparks(l, pos, 10); Fx.dustBurst(l, 0x8a0303, 1.2f, pos, 8, 0.25);
                Fx.sound(l, pos, SoundEvents.ANVIL_LAND, 0.5f, 1.9f);
            });
    static final ProjStyle BARREL = ProjStyle.make().color(0xb02020).size(0.7f).life(60).gravity(0.025).explode(4f).trail(ParticleTypes.SMOKE)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 4f); Fx.burst(l, ParticleTypes.FLAME, pos, 40, 1.2, 0.08);
                Fx.burst(l, ParticleTypes.LARGE_SMOKE, pos, 14, 1.0, 0.03);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1.6f, 0.8f);
            });
    static final ProjStyle COMBINE_BALL = ProjStyle.make().color(0xeaf6ff).size(0.6f).life(80).pierce(5).bounce().trail(ParticleTypes.END_ROD)
            .onTick(p -> { if (p.age2 % 2 == 0) Fx.dustBurst((ServerLevel) p.level(), PULSE, 1.4f, p.position(), 3, 0.2); })
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.burst(l, ParticleTypes.END_ROD, pos, 16, 0.3, 0.15); Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 12, 0.3, 0.3);
                Fx.sound(l, pos, SoundEvents.SHULKER_BULLET_HIT, 1f, 0.6f);
                if (!(p.getOwner() instanceof SlayerEntity o)) return;
                if (hit instanceof LivingEntity le && Combat.isFoe(o, le) && le.getHealth() <= 30) dissolve(o, le);
                if (hit == null) {
                    Fx.ring(l, pos, 2f, PULSE, 1.6f, 26); Combat.blast(o, pos, 2.5f, 8, 0.8f, null);
                    Fx.sound(l, pos, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 2f);
                }
            });
    static final ProjStyle BUGBAIT = ProjStyle.make().color(BAIT).size(0.28f).life(60).gravity(0.045).trail(ParticleTypes.ITEM_SLIME)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, BAIT, 1.8f, pos, 40, 1.4); Fx.burst(l, ParticleTypes.ITEM_SLIME, pos, 20, 0.6, 0.1);
                Fx.ring(l, pos.add(0, 0.2, 0), 3.5f, 0xb89a30, 1.4f, 30);
                Fx.sound(l, pos, SoundEvents.SLIME_SQUISH, 1.4f, 0.6f); Fx.sound(l, pos, SoundEvents.SILVERFISH_AMBIENT, 1.5f, 0.5f);
                if (!(p.getOwner() instanceof SlayerEntity o)) return;
                List<LivingEntity> fs = Combat.around(o, pos, 7);
                for (int i = 0; i < fs.size(); i++) {
                    LivingEntity f = fs.get(i);
                    f.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                    f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    Combat.damage(o, f, 3); // antlion swarm bites
                    if (f instanceof Mob m && fs.size() > 1) m.setTarget(fs.get((i + 1) % fs.size())); // pheromones turn them on each other
                }
            });

    public HalfLife2Gordon() {
        super("half_life2_gordon", "Gordon Freeman", "ゴードン・フリーマン", "Half-Life 2");
        eggPrimary = HEV; eggSecondary = 0x2b2b2d;
        maxHealth = 100; speed = 0.33; armor = 12; knockbackRes = 0.4;
        scale = 1.0f; preferredRange = 9f; dmgMult = 1.0f;
        idleWeaponR = "crowbar";
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
    static Vec3 endOf(SlayerEntity e, LivingEntity t, Vec3 dir, double range) { return t != null ? mid(t) : mz(e).add(dir.scale(range)); }
    static void flash(SlayerEntity e, int rgb) {
        Vec3 m = mz(e);
        Fx.burst(L(e), ParticleTypes.FLAME, m, 2, 0.04, 0.02); Fx.dustBurst(L(e), rgb, 1.1f, m, 4, 0.07);
    }
    static void shell(SlayerEntity e) { Fx.burst(L(e), ParticleTypes.SMOKE, Combat.muzzle(e, 0.4, 0.5, 0.1), 2, 0.03, 0.03); }
    static void pellets(SlayerEntity e, LivingEntity t, int n, double spread, float dmg) {
        Vec3 m = mz(e);
        for (int i = 0; i < n; i++) Combat.hitscan(e, m, aim(e, t, spread), 20, dmg, 0, 0xffd070);
    }
    static Vec3 holdPoint(SlayerEntity e, double dist, double lift) {
        Vec3 f = e.getViewVector(1f);
        return e.position().add(f.x * dist, 0.6 + lift, f.z * dist);
    }
    /** gravity-gun tractor: drag the target toward the hold point, snapping once close. returns true when held */
    static boolean tractor(SlayerEntity e, LivingEntity t, double dist, double lift, double pull) {
        if (t == null || !Combat.isFoe(e, t)) return false;
        Vec3 p = holdPoint(e, dist + t.getBbWidth() * 0.5, lift), d = p.subtract(t.position());
        if (t instanceof Mob m) m.getNavigation().stop();
        t.fallDistance = 0;
        if (d.length() < 0.8) { t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true; return true; }
        t.setDeltaMovement(d.normalize().scale(Math.min(pull, d.length() * 0.4))); t.hurtMarked = true;
        return false;
    }
    static void zap(ServerLevel l, Vec3 a, Vec3 b, int rgb) {
        Vec3 prev = a; int segs = 7;
        for (int i = 1; i <= segs; i++) {
            Vec3 q = a.lerp(b, i / (double) segs);
            if (i < segs) q = q.add((l.random.nextDouble() - .5) * .5, (l.random.nextDouble() - .5) * .5, (l.random.nextDouble() - .5) * .5);
            Fx.beam(l, prev, q, rgb, 0.7f, 0.25); prev = q;
        }
    }
    /** Combine energy / supercharged gravity gun dissolve: kills, white-hot disintegration */
    static void dissolve(SlayerEntity e, LivingEntity t) {
        ServerLevel l = L(e); Vec3 c = mid(t);
        Fx.burst(l, ParticleTypes.END_ROD, c, 30, 0.4, 0.12); Fx.dustBurst(l, 0xffffff, 1.6f, c, 24, 0.5);
        Fx.dustBurst(l, PULSE, 1.2f, c, 20, 0.7); Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, c, 20, 0.5, 0.3);
        t.setDeltaMovement(0, 0.25, 0); t.hurtMarked = true;
        Combat.damage(e, t, t.getHealth() + 100);
        Fx.sound(l, c, SoundEvents.BEACON_DEACTIVATE, 1.4f, 1.6f); Fx.sound(l, c, SoundEvents.FIRE_EXTINGUISH, 1f, 0.6f);
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** shouldered long gun, chest turned right */
    static void shoulder(Pose p, float k, float pitch, float recoil) {
        float tw = 0.22f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.15f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.05f - recoil * 0.22f, k); p.lArmY = lerp(p.lArmY, 0.45f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX = lerp(p.rHandX, -recoil * 0.3f, k);
        p.bodyY += tw * k; p.headY -= tw * k; p.bodyX += (recoil * 0.12f - pitch * 0.2f) * k; p.headX -= recoil * 0.08f * k;
        p.rootZ += recoil * 0.9f * k;
    }
    /** two-handed isosceles pistol grip */
    static void pistol2(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.45f, k); p.rArmY = lerp(p.rArmY, -0.1f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.08f - recoil * 0.4f, k); p.lArmY = lerp(p.lArmY, 0.42f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX = lerp(p.rHandX, -recoil * 0.45f, k);
        p.bodyX -= recoil * 0.06f * k; p.headX -= recoil * 0.06f * k; p.rootZ += recoil * 0.5f * k;
    }
    /** gravity gun cradled at chest height, left hand under the emitter */
    static void grav(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -1.15f + pitch - recoil * 0.25f, k); p.rArmY = lerp(p.rArmY, -0.2f, k); p.rArmZ = lerp(p.rArmZ, 0.12f, k);
        p.lArmX = lerp(p.lArmX, -1.35f + pitch - recoil * 0.25f, k); p.lArmY = lerp(p.lArmY, 0.65f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX = lerp(p.rHandX, -0.4f, k);
        p.bodyX += (0.1f + recoil * 0.1f) * k; p.bodyY += 0.12f * k; p.headY -= 0.12f * k; p.rootZ += recoil * 0.8f * k;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.26f, k);
        p.lLegZ -= 0.06f * k; p.rLegZ += 0.08f * k; p.rootY -= 0.4f * k;
    }
    static void wide(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.2f, k); p.rLegX = lerp(p.rLegX, 0.15f, k);
        p.lLegZ -= 0.28f * k; p.rLegZ += 0.28f * k; p.rootY -= 1.1f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // crowbar hanging loose in the right hand, tip angled forward, relaxed scientist slouch
        p.rArmX = -0.25f + p.rArmX * 0.5f; p.rArmZ = 0.12f; p.rHandX = 0.55f;
        p.headX += 0.04f; p.bodyX += 0.03f;
    }

    @Override
    public void passive(SlayerEntity e) {
        // HEV Mark V: suit armor (damage resistance) + automatic medical system when critical
        if (e.tickCount % 20 == 0) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false));
        if (e.tickCount % 240 == 0 && e.getHealth() < e.getMaxHealth() * 0.35f) {
            e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, false));
            snd(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.8f); snd(e, SoundEvents.NOTE_BLOCK_BIT.value(), 0.8f, 1.2f);
            Fx.dustBurst(L(e), HEV, 1f, e.position().add(0, 1.1, 0), 12, 0.4);
        }
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- Crowbar: forehand chop -> backhand return ----------
        out.add(AttackDef.of("crowbar").range(0, 3.2f).cd(16).weight(3).dur(18).weapon("crowbar")
                .tick((e, t, n) -> {
                    if (n == 3 || n == 10) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.4f);
                    if (n == 5 || n == 12) {
                        boolean fore = n == 5;
                        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.5f, fore ? 70 : -60, fore ? -50 : 60, fore ? 25 : -10, 0xb01818, 0.7f, 10);
                        for (LivingEntity f : Combat.cone(e, 3.3, 50)) {
                            Combat.damage(e, f, fore ? 6 : 5); Combat.knock(f, e.position(), 0.5, 0.15); Fx.sparks(L(e), mid(f), 5);
                            snd(e, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 1.6f);
                        }
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, 0, .18f, -2.5f, .28f, -0.9f, .42f, -1.25f, .55f, -1.5f, .67f, -1.15f, .85f, -0.6f, 1, -0.2f);
                    p.rArmZ = track(t, 0, 0.1f, .18f, 0.5f, .28f, -0.45f, .5f, -0.6f, .67f, 0.7f, .85f, 0.4f, 1, 0.12f);
                    p.rArmY = track(t, 0, 0, .18f, 0.2f, .28f, -0.4f, .5f, -0.5f, .67f, 0.3f, 1, 0);
                    p.rHandX = track(t, 0, 0.5f, .18f, 0.7f, .28f, -0.4f, .5f, 0.3f, .67f, -0.3f, 1, 0.55f);
                    p.lArmX = track(t, 0, 0, .18f, -0.9f, .28f, -0.3f, .5f, -0.8f, .67f, -0.4f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .18f, -0.35f, .5f, -0.2f, .67f, -0.5f, 1, 0);
                    p.bodyY = track(t, 0, 0, .18f, 0.5f, .28f, -0.55f, .5f, -0.6f, .67f, 0.45f, .85f, 0.2f, 1, 0);
                    p.bodyX = track(t, 0, 0, .18f, -0.1f, .28f, 0.3f, .5f, 0.12f, .67f, 0.25f, 1, 0);
                    p.headY -= p.bodyY * 0.6f;
                    p.lLegX = track(t, 0, 0, .25f, -0.45f, .6f, -0.2f, .75f, -0.4f, 1, 0);
                    p.rLegX = track(t, 0, 0, .28f, 0.35f, .55f, 0.1f, .7f, 0.3f, 1, 0);
                    p.rootZ = -1.5f * bump(win(t, .15f, .45f)) - 1f * bump(win(t, .5f, .85f));
                    p.rootY -= 0.5f * bump(win(t, .2f, .4f)) + 0.4f * bump(win(t, .58f, .78f));
                }));

        // ---------- Crowbar execution: grab the collar, overhead two-hand smash, wrench free ----------
        out.add(AttackDef.of("crowbar_execution").range(0, 3.4f).lowHealthTarget(0.3f).weight(5).cd(70).dur(30).weapon("crowbar")
                .tick((e, t, n) -> {
                    if (n == 1) { Combat.lunge(e, 0.45, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.8f); }
                    if (t != null && n < 20 && Combat.isFoe(e, t)) {
                        Vec3 f = Vec3.directionFromRotation(0, e.getYRot()), q = e.position().add(f.scale(1.0 + t.getBbWidth() * 0.5));
                        if (t instanceof Mob m) m.getNavigation().stop();
                        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(q.x, q.y, q.z); t.hurtMarked = true;
                    }
                    if (n == 13) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1, 0.7f);
                    if (n == 17 && t != null) {
                        Fx.dustBurst(L(e), 0x8a0303, 1.4f, mid(t), 26, 0.35); Fx.sparks(L(e), mid(t), 10);
                        snd(e, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 1.2f, 0.9f); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.7f);
                        Combat.damage(e, t, t.getHealth() + 60);
                    }
                    if (n == 23) snd(e, SoundEvents.CHAIN_BREAK, 0.8f, 0.8f);
                })
                .anim((p, t, a) -> {
                    p.lArmX = track(t, 0, 0, .15f, -1.6f, .45f, -1.4f, .55f, -0.4f, 1, 0); p.lArmY = track(t, 0, 0, .15f, 0.3f, .5f, 0, 1, 0);
                    p.lArmZ = track(t, .45f, 0, .57f, -0.5f, 1, 0);
                    p.rArmX = track(t, 0, -0.25f, .2f, -1.8f, .48f, -3.0f, .57f, -0.55f, .7f, -0.7f, .82f, -1.2f, 1, -0.2f);
                    p.rArmZ = track(t, 0, 0.1f, .48f, -0.15f, .57f, -0.1f, .82f, 0.5f, 1, 0.12f);
                    p.rHandX = track(t, 0, 0.55f, .48f, 1.1f, .57f, -0.3f, .7f, -0.2f, .82f, 0.8f, 1, 0.55f);
                    p.bodyX = track(t, 0, 0, .15f, 0.15f, .48f, -0.32f, .57f, 0.55f, .72f, 0.45f, .85f, 0.1f, 1, 0);
                    p.bodyY = track(t, 0, 0, .48f, 0.2f, .57f, -0.15f, .82f, 0.45f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.headX = track(t, 0, 0, .48f, -0.25f, .57f, 0.35f, .8f, 0.2f, 1, 0);
                    p.rootY = track(t, 0, 0, .4f, 0.5f, .5f, 0.6f, .57f, -1.5f, .75f, -1.2f, 1, 0);
                    p.rootZ = track(t, 0, 0, .15f, -1.8f, .57f, -2.2f, .85f, -1.5f, 1, 0);
                    p.lLegX = track(t, 0, 0, .15f, -0.5f, .57f, -0.65f, .85f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .15f, 0.3f, .57f, 0.5f, .85f, 0.3f, 1, 0);
                    p.bodyZ = 0.06f * bump(win(t, .78f, .92f)) * (float) Math.sin(a * 2.5f); // yank the crowbar loose
                }));

        // ---------- HEV sprint into a slide tackle ----------
        out.add(AttackDef.of("sprint_slide").range(4, 13).cd(120).weight(2).dur(26).weapon("crowbar")
                .tick((e, t, n) -> {
                    if (n == 1 || n == 6) { Combat.lunge(e, 0.85, 0.02); snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.6f, 1.4f); }
                    if (n < 11 && n % 3 == 0) Fx.burst(L(e), ParticleTypes.CLOUD, e.position().add(0, 0.1, 0), 2, 0.1, 0.01);
                    if (n == 11) { Combat.lunge(e, 1.1, 0); snd(e, SoundEvents.GRINDSTONE_USE, 1f, 1.2f); }
                    if (n >= 11 && n < 22) Fx.burst(L(e), ParticleTypes.POOF, e.position().add(0, 0.1, 0), 3, 0.2, 0.02);
                    if (n == 15) for (LivingEntity f : Combat.cone(e, 2.8, 60)) {
                        Combat.damage(e, f, 7); Combat.knock(f, e.position(), 0.6, 0.65); Fx.sparks(L(e), mid(f), 6);
                        snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float run = 1 - win(t, .38f, .46f), slide = win(t, .4f, .48f) * (1 - win(t, .82f, .95f));
                    float s = (float) Math.sin(a * 1.7f) * run;
                    p.rLegX = 1.1f * s; p.lLegX = -1.1f * s;
                    p.rArmX = -0.9f * s - 0.4f * run; p.lArmX = 0.9f * s - 0.4f * run; p.rHandX = 0.2f;
                    p.bodyX = 0.4f * run; p.headX = -0.25f * run; p.rootY = Math.abs(s) * 0.7f;
                    // slide: drop low, lean back, lead leg out, trailing leg tucked, arms out for balance
                    p.rootY -= 4.2f * slide; p.bodyX -= 0.55f * slide; p.headX += 0.45f * slide;
                    p.lLegX = lerp(p.lLegX, track(t, .4f, -1.1f, .58f, -1.55f, 1, -1.2f), slide);
                    p.rLegX = lerp(p.rLegX, -0.3f, slide); p.rLegZ += 0.35f * slide;
                    p.lArmX = lerp(p.lArmX, 0.5f, slide); p.lArmZ -= 0.8f * slide;
                    p.rArmX = lerp(p.rArmX, -1.1f, slide); p.rArmZ += 0.4f * slide; p.rHandX = lerp(p.rHandX, -0.3f, slide);
                    p.bodyY = 0.2f * slide; p.rootZ = 1.5f * slide;
                }));

        // ---------- Gravity Gun: primary punt ----------
        out.add(AttackDef.of("gravgun_punt").range(0, 5).cd(26).weight(3).dur(16).weapon("gravgun")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.5f, 1.9f);
                    if (n == 6) {
                        Vec3 m = mz(e);
                        for (LivingEntity f : Combat.cone(e, 5.5, 45)) {
                            Combat.damage(e, f, 6); Combat.knock(f, e.position(), 1.9, 0.45);
                            Fx.beam(L(e), m, mid(f), GRAV, 1f, 0.3);
                        }
                        Fx.ring(L(e), m, 0.8f, GRAV, 1.4f, 16); Fx.dustBurst(L(e), GRAV, 1.3f, m, 14, 0.3);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.9f); snd(e, SoundEvents.IRON_GOLEM_ATTACK, 1f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f), r = 2.2f * kick(t, 6 / 16f, .3f);
                    float thrust = track(t, 0, 0, .3f, 0.35f, .38f, -0.25f, 1, 0); // pull back, then shove forward
                    grav(p, k, 0, r); stance(p, k);
                    p.rArmX -= thrust * 0.5f; p.lArmX -= thrust * 0.5f; p.bodyX -= thrust * 0.3f;
                    p.rootZ += thrust * 2f; p.headX -= r * 0.08f;
                    p.lLegX -= 0.25f * bump(win(t, .3f, .7f));
                }));

        // ---------- Gravity Gun: pull, hold, launch ----------
        out.add(AttackDef.of("gravgun_pull_launch").range(2, 13).cd(90).weight(3).dur(40).weapon("gravgun")
                .when((e, t) -> t.getBbWidth() <= 1.0f && t.getMaxHealth() <= 40)
                .tick((e, t, n) -> {
                    if (n == 1) snd(e, SoundEvents.BEACON_ACTIVATE, 0.9f, 1.7f);
                    if (n >= 2 && n < 28) {
                        tractor(e, t, 2.0, n < 16 ? 0.2 : 0.5, 1.0);
                        if (t != null && n % 2 == 0) Fx.beam(L(e), mz(e), mid(t), GRAV, 0.8f, 0.35);
                        if (n % 5 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.8f, 1.8f);
                    }
                    if (n == 28 && t != null && Combat.isFoe(e, t)) {
                        Vec3 f = e.getViewVector(1f);
                        Combat.damage(e, t, 14);
                        t.setDeltaMovement(f.x * 2.4, 0.35, f.z * 2.4); t.hurtMarked = true;
                        Fx.ring(L(e), mz(e), 1f, GRAV, 1.6f, 20); Fx.dustBurst(L(e), GRAV, 1.4f, mz(e), 18, 0.35);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.8f); snd(e, SoundEvents.IRON_GOLEM_ATTACK, 1.2f, 1.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), pull = win(t, .05f, .4f) * (1 - win(t, .4f, .45f)), hold = win(t, .4f, .45f) * (1 - win(t, .68f, .7f));
                    float r = 2.6f * kick(t, 28 / 40f, .25f);
                    grav(p, k, -0.1f * hold, r); stance(p, k);
                    // pull: lean back against the load, gun drawn toward the chest, tremor
                    float jit = 0.04f * (float) Math.sin(a * 5.3f) * pull;
                    p.bodyX -= 0.25f * pull; p.rArmX += 0.3f * pull + jit; p.lArmX += 0.3f * pull - jit; p.rootZ += 1.6f * pull;
                    p.lLegX -= 0.2f * pull; p.rLegX += 0.25f * pull;
                    // hold: steady, chin down, slow bob
                    p.headX += 0.12f * hold; p.bodyZ += 0.02f * (float) Math.sin(a * 0.6f) * hold;
                    // launch: lunge into it
                    float push = bump(win(t, .66f, .85f));
                    p.bodyX += 0.3f * push; p.rootZ -= 2f * push; p.lLegX -= 0.3f * push;
                }));

        // ---------- Gravity Gun: rip up a sawblade and fling it ----------
        out.add(AttackDef.of("gravgun_sawblade").range(3, 16).cd(70).weight(2).dur(30).weapon("gravgun")
                .tick((e, t, n) -> {
                    Vec3 ground = Combat.muzzle(e, 2.0, 0.3, -1.3);
                    if (n == 2) snd(e, SoundEvents.BEACON_ACTIVATE, 0.7f, 1.9f);
                    if (n >= 2 && n < 10) { Fx.beam(L(e), mz(e), ground, GRAV, 0.7f, 0.35); Fx.burst(L(e), ParticleTypes.CRIT, ground, 2, 0.2, 0.05); }
                    if (n == 10) snd(e, SoundEvents.ANVIL_PLACE, 0.5f, 1.8f);
                    if (n > 10 && n < 18) Fx.dustBurst(L(e), GRAV, 0.9f, mz(e), 2, 0.25);
                    if (n == 18) {
                        Combat.shoot(e, SAWBLADE, mz(e), aim(e, t, 0.01), 1.9, 14f);
                        Fx.ring(L(e), mz(e), 0.8f, GRAV, 1.3f, 16);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 2f); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponR = t > .33f && t < .62f ? "gravgun_saw" : "gravgun";
                    float k = ready(t, .1f, .9f), reach = bump(win(t, .02f, .38f)), r = 2.2f * kick(t, 18 / 30f, .25f);
                    grav(p, k, 0.55f * reach, r); stance(p, k);
                    p.bodyX += 0.4f * reach; p.headX += 0.45f * reach; p.rootY -= 1.4f * reach; // stoop toward the floor
                    p.lLegX -= 0.25f * reach;
                    float lift = win(t, .33f, .55f) * (1 - win(t, .6f, .62f));
                    p.bodyY -= 0.25f * lift; p.headY += 0.25f * lift; // wind the body back with the blade held
                    p.bodyY += 0.35f * bump(win(t, .58f, .8f)); p.rootZ -= 1.5f * bump(win(t, .58f, .8f));
                }));

        // ---------- Gravity Gun: explosive barrel ----------
        out.add(AttackDef.of("gravgun_barrel").range(5, 18).cd(110).weight(2).dur(34).weapon("gravgun")
                .tick((e, t, n) -> {
                    Vec3 src = Combat.muzzle(e, 3.5, -1.2, -1.0);
                    if (n == 2) snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.6f);
                    if (n >= 2 && n < 12) { Fx.beam(L(e), mz(e), src, GRAV, 0.8f, 0.35); Fx.burst(L(e), ParticleTypes.SMOKE, src, 2, 0.3, 0.02); }
                    if (n == 12) snd(e, SoundEvents.ANVIL_PLACE, 0.7f, 0.8f);
                    if (n == 21) {
                        Vec3 d = aim(e, t, 0.01).add(0, 0.08, 0).normalize();
                        Combat.shoot(e, BARREL, mz(e).add(e.getViewVector(1f).scale(0.6)), d, 1.35, 10f);
                        Fx.ring(L(e), mz(e), 1f, GRAV, 1.5f, 20);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.8f); snd(e, SoundEvents.IRON_GOLEM_ATTACK, 1.1f, 0.9f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponR = t > .35f && t < .64f ? "gravgun_barrel" : "gravgun";
                    float k = ready(t, .12f, .9f), r = 2.8f * kick(t, 21 / 34f, .28f);
                    float sweep = track(t, 0, 0, .15f, -0.5f, .35f, 0.2f, .6f, 0, 1, 0); // aim off to the side, drag it across
                    grav(p, k, 0.15f * bump(win(t, .1f, .35f)), r); wide(p, k * 0.7f);
                    p.bodyY += sweep; p.headY -= sweep * 0.5f;
                    float heavy = win(t, .35f, .45f) * (1 - win(t, .6f, .62f));
                    p.rArmX += 0.15f * heavy; p.lArmX += 0.15f * heavy; p.bodyX -= 0.15f * heavy; p.rootY -= 0.6f * heavy;
                    p.bodyZ += 0.025f * (float) Math.sin(a * 3f) * heavy;
                    p.rootZ -= 1.8f * bump(win(t, .6f, .82f)); p.bodyX += 0.3f * bump(win(t, .6f, .82f));
                }));

        // ---------- Supercharged Gravity Gun (Citadel): seize anything, electrocute, dissolve ----------
        out.add(AttackDef.of("gravgun_supercharged").range(2, 15).cd(360).weight(2).dur(48).weapon("gravgun_blue")
                .when((e, t) -> t.getMaxHealth() <= 150 && (t.getMaxHealth() > 40 || e.getHealth() < e.getMaxHealth() * 0.6f))
                .tick((e, t, n) -> {
                    if (n == 1) { snd(e, SoundEvents.BEACON_ACTIVATE, 1.2f, 1.2f); snd(e, SoundEvents.CONDUIT_ACTIVATE, 0.8f, 1.6f); }
                    if (n < 34) Fx.dustBurst(L(e), GRAV_BLUE, 0.9f, mz(e), 2, 0.2);
                    if (n >= 3 && n < 34) {
                        tractor(e, t, 2.2, n < 14 ? 0.3 : 0.9, 1.4);
                        if (t != null && n >= 14) {
                            zap(L(e), mz(e), mid(t), n % 2 == 0 ? 0xffffff : GRAV_BLUE);
                            if (n % 3 == 0) { Combat.damage(e, t, Math.min(2.5f, Math.max(0, t.getHealth() - 1))); snd(e, SoundEvents.BEE_STING, 0.4f, 0.5f); }
                            if (n % 6 == 0) snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.4f, 1.9f);
                        }
                    }
                    if (n == 34 && t != null && Combat.isFoe(e, t)) {
                        Vec3 f = e.getViewVector(1f);
                        Fx.ring(L(e), mz(e), 1.2f, GRAV_BLUE, 1.8f, 24);
                        snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.8f, 1.8f);
                        if (t.getHealth() <= 40) dissolve(e, t);
                        else { Combat.damage(e, t, 26); t.setDeltaMovement(f.x * 2.8, 0.4, f.z * 2.8); t.hurtMarked = true; }
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .08f, .92f), seize = win(t, .05f, .28f), cook = win(t, .28f, .32f) * (1 - win(t, .7f, .72f));
                    float r = 3f * kick(t, 34 / 48f, .25f);
                    grav(p, k, -0.25f * cook, r); wide(p, k * 0.8f);
                    float shake = 0.05f * cook * (float) Math.sin(a * 6.1f);
                    p.rArmZ += shake; p.lArmZ -= shake; p.bodyZ += shake * 0.6f; p.headZ += shake * 0.4f;
                    p.bodyX -= 0.2f * seize * (1 - cook) + 0.1f * cook; p.headX -= 0.2f * cook; // look up at the lifted victim
                    p.rootZ += 1.2f * seize * (1 - win(t, .7f, .75f));
                    float fling = bump(win(t, .7f, .88f));
                    p.bodyX += 0.4f * fling; p.rootZ -= 2.2f * fling; p.lLegX -= 0.35f * fling; p.rootY += 0.5f * fling;
                }));

        // ---------- USP Match: four quick taps ----------
        out.add(AttackDef.of("usp_pistol").range(2, 20).cd(22).weight(3).dur(20).weapon("usp")
                .tick((e, t, n) -> {
                    if (n == 3 || n == 6 || n == 9 || n == 12) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.025), 22, 3.5f, 0, 0xffe080); flash(e, 0xffb050); shell(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 2f);
                    }
                    if (n == 16) snd(e, SoundEvents.STONE_BUTTON_CLICK_ON, 0.5f, 1.4f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .85f);
                    float r = kick(t, 3 / 20f, .1f) + kick(t, 6 / 20f, .1f) + kick(t, 9 / 20f, .1f) + kick(t, 12 / 20f, .12f);
                    pistol2(p, k, 0, r); stance(p, k * 0.6f);
                    p.bodyY -= 0.1f * k; p.headY += 0.1f * k; p.headX += 0.06f * k; // sight picture
                    p.bodyZ += 0.02f * r;
                }));

        // ---------- .357 Magnum: cock, one heavy shot, muzzle flip, revolver spin ----------
        out.add(AttackDef.of("magnum_357").range(4, 26).cd(50).weight(2).dur(36).weapon("magnum")
                .tick((e, t, n) -> {
                    if (n == 5) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1f, 0.9f);
                    if (n == 10) {
                        Vec3 d = aim(e, t, 0.004), m = mz(e);
                        Combat.hitscan(e, m, d, 30, 17f, 1, 0xfff0b0); flash(e, 0xffc060); flash(e, 0xff8020);
                        if (t != null) Combat.knock(t, e.position(), 0.6, 0.15);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 0.7f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.45f, 1.9f);
                    }
                    if (n == 24) snd(e, SoundEvents.CROSSBOW_LOADING_MIDDLE, 0.6f, 1.8f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .9f), aimK = 1 - win(t, .5f, .62f), r = 3f * kick(t, 10 / 36f, .3f);
                    pistol2(p, k * aimK, 0, r);
                    p.rHandY += 0.25f * bump(win(t, .08f, .2f)); // thumb rolls the hammer back
                    p.headX -= 0.12f * r * 0.3f; p.bodyX -= 0.08f * r; p.rootZ += 0.4f * r;
                    // spin: left hand drops, gun twirls on the trigger finger, settles low
                    float sp = smooth(win(t, .6f, .86f));
                    p.rArmX = lerp(p.rArmX, -0.9f, (1 - aimK) * k); p.rArmY = lerp(p.rArmY, -0.3f, (1 - aimK) * k);
                    p.rHandX = lerp(p.rHandX, -2 * PI * sp, 1 - aimK);
                    p.lArmX = lerp(p.lArmX, -0.2f, 1 - aimK); p.lArmY = lerp(p.lArmY, 0, 1 - aimK);
                    p.headX += 0.25f * bump(win(t, .58f, .9f)); p.bodyY -= 0.15f * bump(win(t, .58f, .9f));
                    stance(p, k * 0.7f);
                }));

        // ---------- SMG1: full auto, muzzle climb ----------
        out.add(AttackDef.of("smg1").range(2, 18).cd(28).weight(3).dur(26).weapon("smg1")
                .tick((e, t, n) -> {
                    if (n >= 4 && n < 20) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.06), 20, 1.6f, 0, 0xffe080);
                        if (n % 2 == 0) { flash(e, 0xffb050); shell(e); }
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.5f, 1.8f + e.getRandom().nextFloat() * 0.3f);
                    }
                    if (n == 22) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.4f, 1.9f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), fire = win(t, .14f, .16f) * (1 - win(t, .76f, .78f));
                    float r = (0.3f + 0.15f * (float) Math.sin(a * 3.1f)) * fire;
                    shoulder(p, k, -0.15f * win(t, .15f, .75f) * fire, r); stance(p, k);
                    p.bodyZ += 0.03f * (float) Math.sin(a * 2.7f) * fire; p.rHandZ += 0.03f * (float) Math.sin(a * 3.9f) * fire;
                    p.lArmX += 0.1f * fire * (float) Math.sin(a * 2.3f); // fighting the climb with the support hand
                }));

        // ---------- SMG1 alt: underbarrel grenade + reload ----------
        out.add(AttackDef.of("smg1_grenade").range(5, 20).cd(140).weight(2).dur(32).weapon("smg1")
                .tick((e, t, n) -> {
                    if (n == 8) {
                        Vec3 from = mz(e);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, t, 0.02).add(0, 0.12 + dist * 0.02, 0).normalize();
                        Combat.shoot(e, SMG_GRENADE, from, d, 0.9 + dist * 0.025, 12f);
                        Fx.burst(L(e), ParticleTypes.SMOKE, from, 6, 0.1, 0.03);
                        snd(e, SoundEvents.FIRECHARGE_USE, 0.9f, 1.4f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.3f, 2f);
                    }
                    if (n == 17) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.7f, 1.6f);
                    if (n == 24) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.7f, 1.5f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), r = 2f * kick(t, 8 / 32f, .25f), lob = win(t, .05f, .22f) * (1 - win(t, .4f, .5f));
                    shoulder(p, k, -0.3f * lob, r); stance(p, k);
                    p.bodyX -= 0.12f * lob; p.headX -= 0.1f * lob;
                    // reload: left hand to belt, then thumb the round into the launcher
                    float belt = bump(win(t, .45f, .62f)), load = bump(win(t, .62f, .8f));
                    p.lArmX += 1.1f * belt * k + 0.25f * load * k; p.lArmZ -= 0.35f * belt;
                    p.headX += 0.2f * (belt + load); p.headY -= 0.15f * belt;
                    p.rArmX += 0.2f * (belt + load); // lower the gun to look at it
                }));

        // ---------- SPAS-12: single blast, pump ----------
        out.add(AttackDef.of("spas12").range(0, 9).cd(26).weight(3).dur(22).weapon("spas12")
                .tick((e, t, n) -> {
                    if (n == 5) {
                        pellets(e, t, 7, 0.07, 2.5f); flash(e, 0xffa040);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.2f, 0.9f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.3f, 1.8f);
                    }
                    if (n == 11) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.9f);
                    if (n == 15) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 2f); shell(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 1.6f * kick(t, 5 / 22f, .25f), pump = bump(win(t, .48f, .72f));
                    shoulder(p, k, 0, r); stance(p, k);
                    p.lArmX += 0.35f * pump * k; p.lArmY -= 0.08f * pump;
                    p.bodyZ += 0.05f * pump; p.bodyY -= 0.08f * pump; p.headX += 0.05f * pump;
                    p.rootY += 0.3f * r;
                }));

        // ---------- SPAS-12 alt: both shells at once, double pump ----------
        out.add(AttackDef.of("spas12_double").range(0, 7).cd(55).weight(2).dur(34).weapon("spas12")
                .tick((e, t, n) -> {
                    if (n == 6) {
                        pellets(e, t, 13, 0.1, 2.6f); flash(e, 0xffa040); flash(e, 0xff6020);
                        for (LivingEntity f : Combat.cone(e, 4.5, 40)) Combat.knock(f, e.position(), 1.2, 0.35);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.5f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.4f, 0.6f);
                    }
                    if (n == 15 || n == 23) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.8f);
                    if (n == 18 || n == 26) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.9f); shell(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .92f), r = 2.6f * kick(t, 6 / 34f, .3f);
                    shoulder(p, k, 0, r); wide(p, k * 0.6f); stance(p, k * 0.5f);
                    p.bodyX -= r * 0.15f; p.rootY += r * 0.5f; p.headX -= r * 0.1f;
                    float pump = bump(win(t, .42f, .56f)) + bump(win(t, .66f, .8f));
                    p.lArmX += 0.38f * pump * k; p.bodyZ += 0.05f * pump; p.bodyY -= 0.08f * pump;
                }));

        // ---------- Overwatch AR2: pulse rifle bursts ----------
        out.add(AttackDef.of("ar2").range(3, 24).cd(30).weight(3).dur(30).weapon("ar2")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 24 && n % 2 == 0) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.035), 26, 2.8f, 0, PULSE);
                        Fx.dustBurst(L(e), PULSE, 1.1f, mz(e), 4, 0.07); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mz(e), 2, 0.05, 0.05);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.6f, 1.1f); snd(e, SoundEvents.SHULKER_SHOOT, 0.25f, 1.9f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), fire = win(t, .12f, .14f) * (1 - win(t, .8f, .82f));
                    float r = 0.4f * (0.5f + 0.5f * (float) Math.sin(a * PI)) * fire;
                    shoulder(p, k, 0, r); stance(p, k);
                    p.headX += 0.06f * k; p.rHandZ += 0.02f * (float) Math.sin(a * 3.3f) * fire;
                }));

        // ---------- AR2 alt: dark energy (Combine) ball ----------
        out.add(AttackDef.of("ar2_combine_ball").range(4, 22).cd(200).weight(2).dur(42).weapon("ar2")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.BEACON_ACTIVATE, 0.9f, 1.8f);
                    if (n < 20) {
                        Vec3 m = mz(e); double ang = n * 0.8, rr = 0.7 - n * 0.03;
                        Fx.dustBurst(L(e), n % 2 == 0 ? 0xffffff : PULSE, 1f, m.add(Math.cos(ang) * rr, Math.sin(ang) * rr, 0), 2, 0.03);
                        if (n % 4 == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.5f, 1.2f + n * 0.04f);
                    }
                    if (n == 21) {
                        Combat.shoot(e, COMBINE_BALL, mz(e), aim(e, t, 0.005), 1.1, 12f);
                        Fx.ring(L(e), mz(e), 1f, PULSE, 1.6f, 20);
                        e.setDeltaMovement(e.getDeltaMovement().subtract(e.getViewVector(1f).scale(0.3))); e.hurtMarked = true;
                        snd(e, SoundEvents.ILLUSIONER_CAST_SPELL, 1.2f, 1.6f); snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.4f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), ch = win(t, .05f, .48f) * (1 - win(t, .49f, .5f)), r = 2.8f * kick(t, 21 / 42f, .3f);
                    shoulder(p, k, 0.05f * ch, r); stance(p, k); wide(p, ch * 0.5f);
                    float sh = 0.04f * ch * ch * (float) Math.sin(a * 5.5f);
                    p.bodyZ += sh; p.rArmZ += sh; p.lArmZ -= sh;
                    p.bodyX += 0.12f * ch - r * 0.15f; p.headX += 0.1f * ch;
                    p.lArmX += 0.3f * bump(win(t, .7f, .85f)); p.lArmZ -= 0.25f * bump(win(t, .7f, .85f)); // slap the cell
                }));

        // ---------- Crossbow: scope in, superheated rebar, redraw ----------
        out.add(AttackDef.of("crossbow").range(8, 40).cd(80).weight(2).dur(44).weapon("crossbow")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.SPYGLASS_USE, 1f, 1f);
                    if (n < 18) Fx.burst(L(e), ParticleTypes.SMALL_FLAME, mz(e), 1, 0.03, 0.005); // rebar glowing hot
                    if (n == 18) {
                        Combat.shoot(e, REBAR, mz(e), aim(e, t, 0.002), 3.3, 19f);
                        Fx.burst(L(e), ParticleTypes.CLOUD, mz(e), 3, 0.05, 0.02);
                        snd(e, SoundEvents.CROSSBOW_SHOOT, 1.4f, 0.7f); snd(e, SoundEvents.ARROW_HIT, 0.8f, 0.6f);
                    }
                    if (n == 24) snd(e, SoundEvents.SPYGLASS_STOP_USING, 1f, 1f);
                    if (n == 27) snd(e, SoundEvents.CROSSBOW_LOADING_START, 1f, 0.8f);
                    if (n == 32) snd(e, SoundEvents.CROSSBOW_LOADING_MIDDLE, 1f, 0.8f);
                    if (n == 38) snd(e, SoundEvents.CROSSBOW_LOADING_END, 1f, 0.8f);
                })
                .anim((p, t, a) -> {
                    p.weaponR = t > .41f && t < .84f ? "crossbow_empty" : "crossbow";
                    float k = ready(t, .15f, .92f), zoom = win(t, .07f, .3f) * (1 - win(t, .52f, .6f)), r = 1.8f * kick(t, 18 / 44f, .18f);
                    shoulder(p, k, 0, r); stance(p, k);
                    p.headX += 0.2f * zoom; p.headY -= 0.12f * zoom; p.bodyX += 0.12f * zoom; p.rootY -= 1.0f * zoom; p.lLegX -= 0.15f * zoom;
                    p.bodyZ += 0.012f * (float) Math.sin(a * 0.4f) * zoom; // breath hold sway
                    // reload: stock down, left hand pulls string back then seats a rebar
                    float rel = win(t, .58f, .66f) * (1 - win(t, .88f, .95f));
                    p.rArmX += 0.6f * rel; p.rHandX += 0.4f * rel;
                    p.lArmX = lerp(p.lArmX, track(t, .6f, -1.4f, .72f, -0.7f, .8f, -1.3f, .88f, -1.2f, 1, -1.2f), rel);
                    p.lArmY = lerp(p.lArmY, 0.35f, rel); p.headX += 0.25f * rel; p.bodyX += 0.15f * rel;
                }));

        // ---------- RPG: shoulder, laser-guided rocket (keeps painting the target) ----------
        out.add(AttackDef.of("rpg").range(7, 34).cd(120).weight(2).dur(40).weapon("rpg")
                .when((e, t) -> t.getMaxHealth() >= 30 || Combat.around(e, t.position(), 5).size() >= 2)
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.8f, 0.9f);
                    if (t != null && n >= 6 && n < 36 && n % 2 == 0) {
                        Fx.beam(L(e), mz(e), mid(t), LASER, 0.35f, 0.6); Fx.dustBurst(L(e), LASER, 1f, mid(t), 2, 0.05);
                    }
                    if (n == 6) snd(e, SoundEvents.NOTE_BLOCK_PLING.value(), 0.6f, 2f);
                    if (n == 11) {
                        Combat.shoot(e, RPG_ROCKET, mz(e), aim(e, t, 0.01).add(0, 0.05, 0).normalize(), 0.9, 16f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.7, 0.35, 0.15), 8, 0.2, 0.05); flash(e, 0xffa040);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.5f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .92f), r = 2.4f * kick(t, 11 / 40f, .3f), guide = win(t, .3f, .4f) * (1 - win(t, .88f, .95f));
                    shoulder(p, k, -0.12f, r); stance(p, k);
                    p.rArmX -= 0.25f * k; p.rHandX += 0.25f * k; // tube up on the shoulder
                    p.headY -= 0.1f * k; p.headX += 0.1f * k; p.headZ += 0.12f * k; // cheek against the tube
                    p.bodyX -= r * 0.15f; p.rootZ += r * 0.6f;
                    p.headY += 0.04f * (float) Math.sin(a * 0.8f) * guide; p.bodyY += 0.03f * (float) Math.sin(a * 0.8f) * guide; // steering the laser
                }));

        // ---------- Frag grenade: pull pin, overhand throw ----------
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(160).weight(2).dur(22).weapon("frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1f, 1.5f);
                    if (n == 5) snd(e, SoundEvents.NOTE_BLOCK_HAT.value(), 0.6f, 1.6f);
                    if (n == 11) {
                        Vec3 from = Combat.muzzle(e, 0.6, 0.4, 0.5);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, t, 0.02).add(0, 0.15 + dist * 0.018, 0).normalize();
                        Combat.shoot(e, FRAG, from, d, 0.75 + dist * 0.025, 14f);
                        snd(e, SoundEvents.WITCH_THROW, 1f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponR = t < .52f ? "frag" : null;
                    p.rArmX = track(t, 0, -0.3f, .18f, -0.9f, .42f, 0.9f, .52f, -2.5f, .7f, -1.2f, 1, -0.2f);
                    p.rArmZ = track(t, 0, 0.1f, .42f, 0.5f, .52f, 0.05f, 1, 0.12f);
                    p.rHandX = track(t, 0, 0, .42f, 0.6f, .52f, -0.4f, 1, 0);
                    p.lArmX = track(t, 0, 0, .1f, -0.9f, .2f, -0.9f, .3f, -1.6f, .52f, -1.5f, .7f, -0.3f, 1, 0); // pin pull, then point at target
                    p.lArmY = track(t, 0, 0, .1f, -0.4f, .25f, 0.15f, 1, 0);
                    p.bodyY = track(t, 0, 0, .42f, 0.55f, .55f, -0.45f, .75f, -0.3f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .42f, -0.22f, .55f, 0.3f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.4f, .55f, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, .42f, 0.3f, .55f, 0.45f, 1, 0);
                    p.rootZ = track(t, .4f, 0, .55f, -1.4f, 1, 0); p.rootY = -0.5f * bump(win(t, .4f, .8f));
                }));

        // ---------- Pheropod (bug bait): squeeze, underhand lob -> antlions turn on the enemy ----------
        out.add(AttackDef.of("bugbait").range(5, 16).cd(400).weight(2).dur(28).weapon("bugbait")
                .when((e, t) -> Combat.around(e, t.position(), 7).size() >= 2)
                .tick((e, t, n) -> {
                    if (n == 3 || n == 7) { snd(e, SoundEvents.SLIME_SQUISH, 0.9f, 1.3f); Fx.dustBurst(L(e), BAIT, 0.9f, Combat.muzzle(e, 0.4, 0.3, -0.1), 6, 0.15); }
                    if (n == 9) snd(e, SoundEvents.SILVERFISH_AMBIENT, 1f, 0.6f);
                    if (n == 16) {
                        Vec3 from = Combat.muzzle(e, 0.6, 0.4, -0.2);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, t, 0.02).add(0, 0.25 + dist * 0.015, 0).normalize();
                        Combat.shoot(e, BUGBAIT, from, d, 0.7 + dist * 0.025, 2f);
                        snd(e, SoundEvents.SNOWBALL_THROW, 1f, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponR = t < .58f ? "bugbait" : null;
                    float sq = win(t, .05f, .15f) * (1 - win(t, .35f, .4f));
                    // squeeze it in front of the chest, both hands, head down to look
                    p.rArmX = track(t, 0, -0.3f, .12f, -1.1f, .35f, -1.0f, .48f, 0.9f, .58f, -1.9f, .78f, -1.4f, 1, -0.2f);
                    p.rArmY = track(t, 0, 0, .12f, 0.4f, .35f, 0.4f, .45f, 0, 1, 0);
                    p.rHandX = 0.12f * (float) Math.sin(a * 3f) * sq + track(t, .4f, 0, .5f, 0.5f, .6f, -0.5f, 1, 0);
                    p.lArmX = track(t, 0, 0, .12f, -1.1f, .35f, -1.0f, .45f, -0.4f, .6f, -0.8f, 1, 0);
                    p.lArmY = track(t, 0, 0, .12f, -0.45f, .35f, -0.45f, .45f, 0, 1, 0);
                    p.headX += 0.35f * sq; p.bodyX += 0.1f * sq;
                    // underhand lob: dip and rise
                    p.bodyY = track(t, .35f, 0, .48f, 0.35f, .6f, -0.3f, 1, 0); p.headY -= p.bodyY * 0.6f;
                    p.bodyX += track(t, .35f, 0, .48f, 0.3f, .62f, -0.15f, 1, 0);
                    p.rootY = track(t, .35f, 0, .48f, -1.3f, .62f, 0.3f, 1, 0);
                    p.lLegX = track(t, .35f, 0, .5f, -0.5f, .8f, -0.4f, 1, 0); p.rLegX = track(t, .35f, 0, .5f, 0.4f, 1, 0);
                }));
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat under = Mat.of(0x2b2b2d).noise(.1f), hev = Mat.of(HEV).bevel(.32f).noise(.08f), hevD = Mat.of(0xb35a12).bevel(.25f).noise(.1f);
        Mat rubber = Mat.of(0x1c1c1e).noise(.06f), metal = Mat.of(0x7d7f80).noise(.06f), amber = Mat.of(0xffb030).glow();
        Mat skin = Mat.of(0xd8ab86).noise(.05f).bevel(.1f), hair = Mat.of(0x3b2616).noise(.2f), lens = Mat.of(0xcfe8ff).glow();
        Mat chest = Mat.of(HEV).bevel(.32f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            int cx = x + w / 2, cy = y + h / 2; // black lambda inside a dark ring
            for (int i = -2; i <= 2; i++) { g.px(cx + i, cy - 3, 0x202020); g.px(cx + i, cy + 2, 0x202020); g.px(cx - 3, cy + i, 0x202020); g.px(cx + 2, cy + i, 0x202020); }
            for (int i = 0; i < 4; i++) g.px(cx - 1 + i / 2, cy - 2 + i, 0x101010);
            g.px(cx - 1, cy + 1, 0x101010); g.px(cx - 2, cy + 1, 0x101010);
        });
        // torso: dark undersuit, orange HEV chest plate + abdomen bands, power pack on the back, collar ring
        rig.box(rig.torso, under, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4, -12, -3, 8, 6, 1, .3f);
        rig.box(rig.torso, hevD, -3, -5.5f, -2.6f, 6, 2, 1);
        rig.box(rig.torso, amber, 2, -10, -3.7f, 1, 1, 1);
        rig.box(rig.torso, rubber, -4.5f, -2.5f, -2.5f, 9, 2, 5);
        rig.box(rig.torso, metal, -1, -2.5f, -3.1f, 2, 2, 1);
        rig.box(rig.torso, hev, -3.5f, -11.5f, 2, 7, 7, 2);
        rig.box(rig.torso, rubber, -3, -4.5f, 2.2f, 6, 2, 2);
        rig.box(rig.torso, metal, -3.4f, -11, 4, 1, 6, 1);
        rig.box(rig.torso, metal, 2.4f, -11, 4, 1, 6, 1);
        rig.box(rig.torso, rubber, -3, -13, -3, 6, 1, 6);
        // head: Gordon himself (no helmet in HL2) — hair, beard, glowing glasses
        rig.box(rig.head, skin, -4, -8, -4, 8, 8, 8);
        rig.box(rig.head, hair, -4, -9, -4, 8, 1, 8, .2f);
        rig.box(rig.head, hair, -4, -8, 3.2f, 8, 5, 1);
        rig.box(rig.head, hair, -3.5f, -3, -4.6f, 7, 3, 1);
        rig.box(rig.head, lens, -3, -5.5f, -4.7f, 2, 1, 1);
        rig.box(rig.head, lens, 1, -5.5f, -4.7f, 2, 1, 1);
        rig.box(rig.head, rubber, -1, -5.5f, -4.6f, 2, 1, 1);
        // arms: orange pauldron, dark sleeve, orange vambrace, black glove
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, under, -2, -2, -2, 4, 12, 4);
            rig.box(arm, hev, s < 0 ? -3f : -2f, -2.5f, -2.5f, 5, 4, 5);
            rig.box(arm, hevD, -2, 4, -2, 4, 4, 4, .3f);
            rig.box(arm, rubber, -2, 8, -2, 4, 2, 4, .2f);
        }
        rig.box(rig.lArm, amber, -0.5f, 5, -2.8f, 1, 1, 1); // HEV wrist readout
        // legs: orange thigh plate, rubber knee, orange shin, black boot
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, under, -2, 0, -2, 4, 12, 4);
            rig.box(leg, hev, -2, 1, -2.6f, 4, 4, 1);
            rig.box(leg, rubber, -1.5f, 5, -2.8f, 3, 2, 1);
            rig.box(leg, hevD, -2, 7, -2.5f, 4, 3, 1);
            rig.box(leg, rubber, -2, 10, -3, 4, 2, 5, .2f);
        }
        buildWeapons(rig, metal, rubber);
    }

    private static void gravgun(Rig rig, String name, Mat core) {
        Mat shell = Mat.of(0xe8892a).bevel(.3f), dark = Mat.of(0x2e2e30).noise(.08f);
        PartDefinition w = rig.weapon(rig.rHand, name);
        rig.box(w, dark, -2, -3, -6, 4, 4, 8);
        rig.box(w, shell, -2.5f, -4.5f, -5, 5, 2, 6);
        rig.box(w, core, -1, -2.5f, -7, 2, 2, 1);
        rig.box(w, dark, -2.5f, -3, -11, 1, 1, 5);
        rig.box(w, dark, 1.5f, -3, -11, 1, 1, 5);
        rig.box(w, dark, -0.5f, -5, -11, 1, 1, 5);
        rig.box(w, Mat.of(0x1a1a1a), -1, 1, -1, 2, 3, 2);
        if (name.equals("gravgun_saw")) rig.box(w, Mat.of(0xb8b8b8).bevel(.1f), -3, -6, -16, 6, 6, 1);
        if (name.equals("gravgun_barrel")) {
            rig.box(w, Mat.of(0xa82020).noise(.12f), -3, -8, -21, 6, 8, 6);
            rig.box(w, Mat.of(0xe0d040), -3, -6, -21, 6, 1, 6, .1f);
        }
    }

    private static void buildWeapons(Rig rig, Mat metal, Mat rubber) {
        Mat gun = Mat.of(0x37393b).noise(.08f), steel = Mat.of(0x9a9ca0).noise(.05f), black = Mat.of(0x161616), wood = Mat.of(0x5a3a22).noise(.15f);
        Mat red = Mat.of(0xa31d1d).noise(.1f), pulse = Mat.of(PULSE).glow(), hot = Mat.of(0xff8a30).glow(), laser = Mat.of(LASER).glow();
        PartDefinition w;
        // crowbar (red shaft, hooked claw)
        w = rig.weapon(rig.rHand, "crowbar");
        rig.box(w, red, -0.5f, -0.5f, -15, 1, 1, 17);
        rig.box(w, red, -0.5f, -2.5f, -16, 1, 3, 1);
        rig.box(w, metal, -0.5f, -3, -15, 1, 1, 1);
        rig.box(w, metal, -0.5f, 0, 2, 1, 1, 1);
        // gravity gun (normal + supercharged + held props)
        gravgun(rig, "gravgun", Mat.of(GRAV).glow());
        gravgun(rig, "gravgun_blue", Mat.of(GRAV_BLUE).glow());
        gravgun(rig, "gravgun_saw", Mat.of(GRAV).glow());
        gravgun(rig, "gravgun_barrel", Mat.of(GRAV).glow());
        // USP Match
        w = rig.weapon(rig.rHand, "usp");
        rig.box(w, gun, -1, -2.5f, -6, 2, 1, 6); rig.box(w, black, -1, -1.5f, -5, 2, 1, 5); rig.box(w, black, -1, -0.5f, -1.5f, 2, 3, 2);
        // .357 Magnum
        w = rig.weapon(rig.rHand, "magnum");
        rig.box(w, steel, -0.5f, -2.5f, -9, 1, 1, 6); rig.box(w, steel, -1.5f, -3, -3, 3, 3, 3); rig.box(w, steel, -1, -2, -4, 2, 2, 1);
        rig.box(w, wood, -1, -0.5f, -1, 2, 4, 2); rig.box(w, black, -0.5f, -3.5f, 0, 1, 1, 1);
        // SMG1 (MP7 + grenade launcher)
        w = rig.weapon(rig.rHand, "smg1");
        rig.box(w, gun, -1.5f, -3, -7, 3, 3, 8); rig.box(w, black, -0.5f, -2.5f, -10, 1, 1, 3); rig.box(w, black, -1, 0, -5, 2, 4, 1);
        rig.box(w, metal, -1, -0.5f, -9, 2, 2, 3); rig.box(w, black, -0.5f, -2, 1, 1, 1, 4); rig.box(w, black, -1, 0, -1, 2, 3, 2);
        // SPAS-12
        w = rig.weapon(rig.rHand, "spas12");
        rig.box(w, black, -1.5f, -3, -6, 3, 3, 8); rig.box(w, metal, -1, -3.5f, -16, 2, 1, 10); rig.box(w, gun, -1.5f, -2.5f, -13, 3, 2, 6);
        rig.box(w, metal, -0.5f, -4, 2, 1, 1, 5); rig.box(w, black, -1, 0, -1, 2, 3, 2);
        // Overwatch Standard Issue Pulse Rifle (AR2)
        w = rig.weapon(rig.rHand, "ar2");
        rig.box(w, Mat.of(0x2a3038).bevel(.3f), -2, -3.5f, -8, 4, 4, 10); rig.box(w, black, -0.5f, -2.5f, -14, 1, 1, 6);
        rig.box(w, pulse, -0.5f, -4.5f, -7, 1, 1, 5); rig.box(w, pulse, -2.6f, -3, -3, 1, 2, 2); rig.box(w, gun, -1.5f, -3, 2, 3, 3, 3);
        rig.box(w, black, -1, 0.5f, -2, 2, 3, 2);
        // Crossbow (loaded with glowing rebar / empty)
        for (int i = 0; i < 2; i++) {
            w = rig.weapon(rig.rHand, i == 0 ? "crossbow" : "crossbow_empty");
            rig.box(w, gun, -1, -2.5f, -9, 2, 2, 12); rig.box(w, metal, -5, -3, -9, 10, 1, 1); rig.box(w, black, -1, -5, -5, 2, 2, 4);
            rig.box(w, Mat.of(0xc8a020), -1.5f, -2, 0, 3, 2, 2); rig.box(w, black, -1, -0.5f, -1, 2, 3, 2);
            if (i == 0) rig.box(w, hot, -0.5f, -3.5f, -15, 1, 1, 10);
        }
        // RPG (laser-guided)
        w = rig.weapon(rig.rHand, "rpg");
        rig.box(w, Mat.of(0x3b4a3a).noise(.1f), -1.5f, -4.5f, -13, 3, 3, 19); rig.box(w, black, -2, -5, -14, 4, 4, 1);
        rig.box(w, black, -0.5f, -6.5f, -4, 1, 2, 2); rig.box(w, laser, -0.5f, -5.5f, -13, 1, 1, 1); rig.box(w, black, -1, -1.5f, -2, 2, 4, 2);
        // throwables
        w = rig.weapon(rig.rHand, "frag");
        rig.box(w, Mat.of(0x3e4a2c), -1, -1, -2, 2, 3, 2); rig.box(w, laser, -0.5f, -2, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.rHand, "bugbait");
        rig.box(w, Mat.of(BAIT).noise(.25f).bevel(.4f), -1.5f, -1.5f, -2.5f, 3, 3, 3); rig.box(w, Mat.of(0xff9a30).glow(), -0.5f, -2.2f, -1.5f, 1, 1, 1);
    }
}
