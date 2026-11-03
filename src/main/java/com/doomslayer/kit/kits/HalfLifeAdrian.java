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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Cpl. Adrian Shephard, Half-Life: Opposing Force. HECU marine with PCV, gas mask, and the whole OpFor arsenal incl. Race X bioweapons. */
public class HalfLifeAdrian extends SlayerKit {
    static final int LASER = 0xff1010, SHOCK_C = 0x40d8ff, SPORE_C = 0x9cff30, PORTAL_C = 0x50ff80, BLOOD = 0x8a0303, ALIEN_BLOOD = 0xc8c020;
    static final String TM = "hla_tm", SAT = "hla_sat";

    static final ProjStyle M203 = ProjStyle.make().color(0x6a6a50).size(0.3f).life(60).explode(3.2f).gravity(0.045).trail(ParticleTypes.SMOKE);
    static final ProjStyle FRAG = ProjStyle.make().color(0x4a5530).size(0.25f).life(36).explode(3.6f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle RPG = ProjStyle.make().color(0xffa040).size(0.45f).life(140).explode(3.8f).homing(0.32f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle SHOCK = ProjStyle.make().color(SHOCK_C).size(0.3f).life(28).trail(ParticleTypes.ELECTRIC_SPARK)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 12, 0.25, 0.3); Fx.dustBurst(l, SHOCK_C, 1f, pos, 6, 0.2);
                Fx.sound(l, pos, SoundEvents.BEE_STING, 0.6f, 1.6f);
            });
    static final ProjStyle SPORE = ProjStyle.make().color(SPORE_C).size(0.35f).life(45).explode(2.8f).gravity(0.05).bounce().trail(ParticleTypes.ITEM_SLIME)
            .onHit((p, pos, hit) -> sporeBurst((ServerLevel) p.level(), pos));
    static final ProjStyle SPORE_SHOT = ProjStyle.make().color(SPORE_C).size(0.32f).life(40).explode(2.2f).trail(ParticleTypes.ITEM_SLIME)
            .onHit((p, pos, hit) -> sporeBurst((ServerLevel) p.level(), pos));
    static final ProjStyle PENGUIN = ProjStyle.make().color(0x202428).size(0.35f).life(80).explode(3.2f).gravity(0.06).homing(0.25f).bounce().trail(ParticleTypes.SNOWFLAKE);
    static final ProjStyle SNARK = ProjStyle.make().color(0xc04040).size(0.25f).life(100).pierce(3).gravity(0.06).homing(0.35f).bounce().trail(ParticleTypes.ITEM_SLIME)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, hit != null ? BLOOD : ALIEN_BLOOD, 1f, pos, 8, 0.2);
                Fx.sound(l, pos, SoundEvents.SILVERFISH_HURT, 0.8f, 1.8f);
            });
    static final ProjStyle SATCHEL = ProjStyle.make().color(0x5a4a30).size(0.32f).life(70).gravity(0.06).trail(ParticleTypes.SMOKE)
            .onHit((p, pos, hit) -> {
                if (!(p.getOwner() instanceof SlayerEntity o)) return;
                putVec(o.getPersistentData(), SAT, pos.add(0, 0.15, 0));
                Fx.sound((ServerLevel) p.level(), pos, SoundEvents.ARMOR_EQUIP_LEATHER, 1f, 0.8f);
            });
    static final ProjStyle DISPLACER = ProjStyle.make().color(PORTAL_C).size(1.1f).life(70).trail(ParticleTypes.PORTAL)
            .onTick(p -> { if (p.age2 % 2 == 0) Fx.dustBurst((ServerLevel) p.level(), PORTAL_C, 1.4f, p.position(), 5, 0.4); })
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.burst(l, ParticleTypes.REVERSE_PORTAL, pos, 60, 1.2, 0.3); Fx.dustBurst(l, PORTAL_C, 2f, pos, 40, 1.5);
                for (int i = 0; i < 3; i++) Fx.ring(l, pos.add(0, i * 0.6, 0), 1.5f + i, PORTAL_C, 1.5f, 24);
                Fx.sound(l, pos, SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.6f); Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.5f);
                if (!(p.getOwner() instanceof SlayerEntity o)) return;
                for (LivingEntity f : Combat.around(o, pos, 3.2)) {
                    Combat.damage(o, f, 14);
                    // "sent to Xen": flung through a portal to somewhere nearby
                    double a = l.random.nextDouble() * Math.PI * 2, r = 6 + l.random.nextDouble() * 4;
                    Fx.burst(l, ParticleTypes.PORTAL, f.position(), 30, 0.5, 0.5);
                    if (f.isAlive()) f.teleportTo(f.getX() + Math.cos(a) * r, f.getY() + 4, f.getZ() + Math.sin(a) * r);
                    Fx.burst(l, ParticleTypes.REVERSE_PORTAL, f.position(), 30, 0.5, 0.3);
                }
            });

    public HalfLifeAdrian() {
        super("half_life_adrian", "Adrian Shephard", "エイドリアン・シェパード", "Half-Life: Opposing Force");
        eggPrimary = 0x4b5233; eggSecondary = 0x2b2e22;
        maxHealth = 80; speed = 0.31; armor = 12; knockbackRes = 0.4;
        preferredRange = 10f; dmgMult = 1f;
        idleWeaponR = "mp5";
    }

    // ================= helpers (server) =================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.1, 0.3, 0); }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        return Combat.dirTo(mz(e), t, spread, new Random(e.getRandom().nextLong()));
    }
    static void flash(SlayerEntity e, int rgb) {
        Vec3 m = mz(e); Fx.burst(L(e), ParticleTypes.FLAME, m, 2, 0.04, 0.02); Fx.dustBurst(L(e), rgb, 1f, m, 3, 0.06);
    }
    static void brass(SlayerEntity e) { Fx.burst(L(e), ParticleTypes.SMOKE, Combat.muzzle(e, 0.4, 0.55, 0.1), 1, 0.03, 0.03); }
    static void bullet(SlayerEntity e, LivingEntity t, double spread, float dmg, int pierce, int rgb) {
        Combat.hitscan(e, mz(e), aim(e, t, spread), 30, dmg, pierce, rgb);
    }
    /** red dot laser sight painted from the muzzle onto the target */
    static void laser(SlayerEntity e, LivingEntity t) {
        if (t == null) return;
        Vec3 m = mz(e), end = mid(t);
        Fx.beam(L(e), m, end, LASER, 0.35f, 0.5);
        Fx.dustBurst(L(e), LASER, 1.3f, end, 2, 0.02);
    }
    static void blood(SlayerEntity e, LivingEntity t, int n) { if (t != null) Fx.dustBurst(L(e), BLOOD, 1.2f, mid(t), n, 0.3); }
    static void sporeBurst(ServerLevel l, Vec3 pos) {
        Fx.burst(l, ParticleTypes.ITEM_SLIME, pos, 30, 0.6, 0.15); Fx.dustBurst(l, SPORE_C, 1.6f, pos, 24, 0.9);
        Fx.burst(l, ParticleTypes.EXPLOSION, pos, 1, 0, 0);
        Fx.sound(l, pos, SoundEvents.SLIME_SQUISH, 1.4f, 0.6f); Fx.sound(l, pos, SoundEvents.PUFFER_FISH_BLOW_UP, 1f, 0.8f);
    }
    static void putVec(CompoundTag d, String k, Vec3 v) { d.putDouble(k + "x", v.x); d.putDouble(k + "y", v.y); d.putDouble(k + "z", v.z); d.putBoolean(k, true); }
    static Vec3 getVec(CompoundTag d, String k) { return new Vec3(d.getDouble(k + "x"), d.getDouble(k + "y"), d.getDouble(k + "z")); }
    static Vec3 fwd(SlayerEntity e) { return Vec3.directionFromRotation(0, e.getYRot()); }

    /** PCV: armour battery recharges as absorption; armed tripmines watch their laser. */
    @Override
    public void passive(SlayerEntity e) {
        if (e.tickCount % 80 == 0 && e.getAbsorptionAmount() < 6) e.setAbsorptionAmount(Math.min(6, e.getAbsorptionAmount() + 2));
        CompoundTag d = e.getPersistentData();
        if (!d.getBoolean(TM)) return;
        Vec3 a = getVec(d, TM), b = getVec(d, TM + "e");
        long exp = d.getLong(TM + "t");
        if (e.level().getGameTime() > exp) { d.putBoolean(TM, false); Fx.burst(L(e), ParticleTypes.SMOKE, a, 6, 0.1, 0.02); return; }
        if (e.tickCount % 3 == 0) { Fx.beam(L(e), a, b, 0x30ff40, 0.3f, 0.6); Fx.dustBurst(L(e), 0x30ff40, 0.8f, a, 1, 0.02); }
        Vec3 ab = b.subtract(a); double len2 = ab.lengthSqr();
        for (LivingEntity f : Combat.foes(e, new AABB(a, b).inflate(1.2))) {
            Vec3 c = f.position().add(0, f.getBbHeight() * 0.4, 0);
            double u = Math.max(0, Math.min(1, c.subtract(a).dot(ab) / len2));
            if (a.add(ab.scale(u)).distanceTo(c) < f.getBbWidth() * 0.5 + 0.4) {
                d.putBoolean(TM, false);
                Combat.blast(e, a, 4f, 20f, 1.2f, null); Fx.explosion(L(e), a, 3.5f);
                Fx.sound(L(e), a, SoundEvents.GENERIC_EXPLODE, 1.6f, 0.9f);
                return;
            }
        }
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** shouldered rifle, cheek welded to the stock, marine "bladed" stance */
    static void rifle(Pose p, float k, float pitch, float recoil) {
        float tw = 0.3f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.28f, k); p.rArmY = lerp(p.rArmY, -0.1f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.08f - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.5f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * 0.8f * k; p.headX += (0.12f - recoil * 0.08f) * k; p.headZ -= 0.12f * k;
        p.bodyX += (recoil * 0.1f - pitch * 0.2f) * k; p.rootZ += recoil * 0.8f * k;
    }
    /** two-hand Weaver pistol grip */
    static void weaver(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.45f, k); p.rArmY = lerp(p.rArmY, -0.05f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.1f - recoil * 0.4f, k); p.lArmY = lerp(p.lArmY, 0.6f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX -= recoil * 0.35f; p.bodyY += 0.12f * k; p.headY -= 0.1f * k; p.headX += recoil * -0.06f;
        p.bodyX += 0.06f * k - recoil * 0.05f; p.rootZ += recoil * 0.4f;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.3f, k);
        p.lLegZ -= 0.05f * k; p.rLegZ += 0.08f * k; p.rootY -= 0.35f * k;
    }
    static void wide(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.25f, k); p.rLegX = lerp(p.rLegX, 0.2f, k);
        p.lLegZ -= 0.28f * k; p.rLegZ += 0.28f * k; p.rootY -= 1.1f * k;
    }
    /** one knee down */
    static void kneel(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -1.1f, k); p.rLegX = lerp(p.rLegX, 1.0f, k); p.rLegZ += 0.1f * k;
        p.rootY -= 3.2f * k; p.bodyX += 0.08f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // MP5 at low ready, muzzle down, left hand on the handguard; slow breathing in the gas mask
        p.rArmX = -0.6f + p.rArmX * 0.25f; p.rArmY = -0.3f;
        p.lArmX = -0.85f + p.lArmX * 0.25f; p.lArmY = 0.6f; p.lArmZ = 0.05f;
        p.rHandX = -0.4f; p.bodyX += 0.05f + 0.015f * (float) Math.sin(age * 0.08f); p.headX += 0.02f * (float) Math.sin(age * 0.08f);
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- Combat knife: forehand slash -> backhand return ----------
        out.add(AttackDef.of("knife").range(0, 3.2f).cd(16).weight(2).dur(18).weapon("knife")
                .tick((e, t, n) -> {
                    if (n == 5 || n == 11) {
                        boolean back = n == 11;
                        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.3f, back ? -60 : 60, back ? 50 : -50, back ? -20 : 20, 0xdddddd, 0.6f, 10);
                        for (LivingEntity f : Combat.cone(e, 3.2, 55)) { Combat.damage(e, f, back ? 6 : 5); blood(e, f, 6); }
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, back ? 1.3f : 1.5f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -0.5f, .18f, -1.2f, .3f, -1.5f, .45f, -1.0f, .62f, -1.45f, .8f, -1.0f, 1, -0.5f);
                    p.rArmY = trackLin(t, 0, 0, .18f, -0.9f, .3f, 0.8f, .45f, 0.9f, .62f, -0.7f, .8f, -0.6f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .18f, 0.4f, .3f, -0.2f, .62f, 0.3f, 1, 0);
                    p.rHandY = track(t, 0, 0, .3f, 0.6f, .62f, -0.6f, 1, 0);
                    p.lArmX = track(t, 0, -0.4f, .3f, -0.9f, 1, -0.4f); p.lArmZ = -0.25f; p.lArmY = 0.3f;
                    p.bodyY = trackLin(t, 0, 0, .18f, -0.4f, .3f, 0.45f, .45f, 0.5f, .62f, -0.45f, .8f, -0.35f, 1, 0);
                    p.headY = -p.bodyY * 0.6f; p.bodyX = 0.18f * bump(win(t, .1f, .9f));
                    p.lLegX = track(t, 0, 0, .2f, -0.45f, .85f, -0.45f, 1, 0); p.rLegX = track(t, 0, 0, .2f, 0.35f, .85f, 0.35f, 1, 0);
                    p.rootZ = -1.4f * bump(win(t, .15f, .9f)); p.rootY = -0.5f * bump(win(t, .2f, .9f));
                }));

        // ---------- Pipe wrench: charged overhead clobber ----------
        out.add(AttackDef.of("pipe_wrench").range(0, 3.4f).cd(30).weight(2).dur(28).weapon("wrench")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.8f, 0.7f);
                    if (n == 14) {
                        Combat.lunge(e, 0.3, 0.02);
                        Fx.arc(L(e), e.position().add(0, 1.2, 0), e.getYRot(), 1.6f, 0, 0, 90, 0xaaaaaa, 0.9f, 1);
                        for (LivingEntity f : Combat.cone(e, 3.5, 50)) {
                            Combat.damage(e, f, 12); Combat.knock(f, e.position(), 0.8, 0.2); Fx.sparks(L(e), mid(f), 8); blood(e, f, 5);
                        }
                        snd(e, SoundEvents.ANVIL_LAND, 0.9f, 1.4f); snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    float hit = 14 / 28f;
                    p.rArmX = track(t, 0, -0.3f, .38f, -3.0f, hit - .04f, -3.1f, hit, -0.5f, .7f, -0.4f, 1, -0.2f);
                    p.rArmZ = track(t, 0, 0, .38f, -0.25f, hit, 0.1f, 1, 0);
                    p.rHandX = track(t, 0, 0, .38f, 0.9f, hit, -0.3f, .75f, -0.2f, 1, 0);
                    p.lArmX = track(t, 0, 0, .38f, -1.2f, hit, 0.4f, .8f, 0.2f, 1, 0); p.lArmZ = track(t, 0, 0, .38f, -0.4f, hit, -0.2f, 1, 0);
                    p.bodyX = trackLin(t, 0, 0, .38f, -0.35f, hit - .04f, -0.4f, hit, 0.5f, .75f, 0.4f, 1, 0);
                    p.bodyY = track(t, 0, 0, .38f, 0.35f, hit, -0.15f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.headX = track(t, 0, 0, .38f, -0.2f, hit, 0.3f, 1, 0);
                    p.lLegX = track(t, 0, 0, .35f, -0.2f, hit, -0.6f, .85f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, hit, 0.45f, .85f, 0.35f, 1, 0);
                    p.rootY = track(t, 0, 0, .38f, 0.4f, hit, -1.3f, .8f, -0.9f, 1, 0); p.rootZ = track(t, .35f, 0, hit, -1.8f, .85f, -1.5f, 1, 0);
                }));

        // ---------- Barnacle grapple: tongue out, reel the foe in, it bites ----------
        out.add(AttackDef.of("barnacle_grapple").range(4, 14).cd(160).weight(1).dur(30).weapon("barnacle")
                .tick((e, t, n) -> {
                    if (n == 6) snd(e, SoundEvents.FISHING_BOBBER_THROW, 1.2f, 0.6f);
                    if (n >= 6 && n <= 22 && t != null) {
                        Fx.beam(L(e), mz(e), mid(t), 0xc06080, 0.6f, 0.3);
                        if (n >= 9 && Combat.isFoe(e, t)) {
                            Vec3 pull = e.position().subtract(t.position()); double d = pull.length();
                            if (d > 1.6) { t.setDeltaMovement(pull.normalize().scale(0.55).add(0, 0.08, 0)); t.hurtMarked = true; }
                            if (t instanceof Mob m) m.getNavigation().stop();
                            if (n % 4 == 0) snd(e, SoundEvents.FISHING_BOBBER_RETRIEVE, 0.9f, 0.7f);
                        }
                    }
                    if (n == 22 && t != null) {
                        Combat.damage(e, t, 9); blood(e, t, 14);
                        snd(e, SoundEvents.SLIME_SQUISH, 1.2f, 0.6f); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), reel = win(t, .3f, .72f);
                    p.rArmX = lerp(p.rArmX, -HALF - 0.1f + 0.5f * reel - 0.4f * kick(t, .2f, .15f), k); p.rArmY = lerp(p.rArmY, 0.1f, k);
                    p.rArmZ = 0.05f * (float) Math.sin(a * 3) * reel;
                    p.lArmX = lerp(p.lArmX, -1.2f + 0.3f * reel, k); p.lArmY = 0.55f * k; // steadying the wrist
                    p.bodyX += (-0.25f * reel + 0.3f * bump(win(t, .7f, .85f))) * k; p.bodyY += 0.2f * k; p.headY -= 0.2f * k;
                    p.lLegX = lerp(p.lLegX, -0.5f + 0.2f * reel, k); p.rLegX = lerp(p.rLegX, 0.5f, k);
                    p.rootZ += (1.2f * reel - 0.8f * bump(win(t, .7f, .9f))) * k; p.rootY -= 0.6f * k;
                    p.rHandZ += 0.08f * (float) Math.sin(a * 2.2f) * k; // the barnacle squirms
                }));

        // ---------- M9 pistol: rapid one-hand double taps ----------
        out.add(AttackDef.of("m9_pistol").range(2, 20).cd(24).weight(2).dur(20).weapon("m9")
                .tick((e, t, n) -> {
                    if (n == 4 || n == 7 || n == 11 || n == 14) {
                        bullet(e, t, 0.03, 3.5f, 0, 0xffe0a0); flash(e, 0xffc060); brass(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f);
                    float r = kick(t, 4 / 20f, .1f) + kick(t, 7 / 20f, .1f) + kick(t, 11 / 20f, .1f) + kick(t, 14 / 20f, .1f);
                    p.bodyY -= 0.4f * k; p.headY += 0.4f * k; // side-on pistol stance
                    p.rArmX = lerp(p.rArmX, -HALF - r * 0.4f, k); p.rArmY = lerp(p.rArmY, 0.4f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
                    p.rHandX -= r * 0.35f;
                    p.lArmX = lerp(p.lArmX, -0.2f, k); p.lArmZ = lerp(p.lArmZ, -0.25f, k);
                    p.bodyX -= r * 0.05f; p.rootZ += r * 0.3f;
                    p.lLegX = lerp(p.lLegX, 0.25f, k); p.rLegX = lerp(p.rLegX, -0.2f, k); p.rootY -= 0.3f * k;
                }));

        // ---------- Desert Eagle: laser sight on, two heavy aimed shots ----------
        out.add(AttackDef.of("deagle_laser").range(4, 28).cd(40).weight(2).dur(32).weapon("deagle")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.STONE_BUTTON_CLICK_ON, 1f, 1.8f);
                    if (n >= 4 && n <= 26 && n % 2 == 0) laser(e, t);
                    if (n == 14 || n == 23) {
                        bullet(e, t, 0.002, 9.5f, 1, 0xffd080); flash(e, 0xffa040); brass(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.1f, 1.2f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.25f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .88f), r = 1.8f * (kick(t, 14 / 32f, .2f) + kick(t, 23 / 32f, .2f));
                    weaver(p, k, 0, r); stance(p, k);
                    float focus = win(t, .12f, .3f) * (1 - win(t, .8f, .9f));
                    p.headX += 0.1f * focus; p.headZ -= 0.1f * focus; p.bodyX += 0.05f * focus;
                    p.rArmY += 0.02f * (float) Math.sin(a * 0.7f) * focus; // tracking the dot
                }));

        // ---------- .357 Python: thumb the hammer, big muzzle flip ----------
        out.add(AttackDef.of("python_357").range(3, 24).cd(38).weight(2).dur(28).weapon("python")
                .tick((e, t, n) -> {
                    if (n == 5 || n == 16) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1f, 1.3f);
                    if (n == 9 || n == 20) {
                        bullet(e, t, 0.006, 11f, 1, 0xffe080); flash(e, 0xffb050);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 0.9f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.3f, 1.7f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .9f), r = 2.4f * (kick(t, 9 / 28f, .25f) + kick(t, 20 / 28f, .25f));
                    weaver(p, k, 0, r); stance(p, k);
                    p.rArmX -= r * 0.25f; p.lArmX -= r * 0.2f; // muzzle flip climbs the arms
                    float cock = bump(win(t, .1f, .3f)) + bump(win(t, .5f, .68f));
                    p.rHandY += 0.25f * cock; p.lArmZ -= 0.12f * cock; p.headX -= r * 0.05f; p.bodyX -= r * 0.08f;
                }));

        // ---------- MP5: controlled bursts ----------
        out.add(AttackDef.of("mp5").range(2, 22).cd(26).weight(4).dur(30).weapon("mp5")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 22 && n % 2 == 0 && (n < 11 || n > 14)) {
                        bullet(e, t, 0.045, 2.6f, 0, 0xffe080); flash(e, 0xffc060); brass(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.75f, 1.6f + e.getRandom().nextFloat() * 0.15f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .12f, .14f) * (1 - win(t, .76f, .78f)) * (t > .36f && t < .48f ? 0 : 1);
                    float r = 0.45f * (0.5f + 0.5f * (float) Math.sin(a * PI)) * fire;
                    rifle(p, k, 0, r); stance(p, k);
                    p.rHandZ += 0.03f * (float) Math.sin(a * 3.7f) * fire; p.bodyZ += 0.02f * (float) Math.sin(a * 2.9f) * fire;
                }));

        // ---------- MP5 M203: open breech, load, lob a 40mm ----------
        out.add(AttackDef.of("mp5_m203").range(6, 22).cd(110).weight(2).dur(28).weapon("mp5")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.8f, 1.6f);
                    if (n == 7) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.8f, 1.6f);
                    if (n == 12) {
                        Vec3 from = Combat.muzzle(e, 1.0, 0.3, -0.1);
                        double dist = t != null ? from.distanceTo(mid(t)) : 12;
                        Vec3 d = aim(e, t, 0.01).add(0, 0.08 + dist * 0.016, 0).normalize();
                        Combat.shoot(e, M203, from, d, 0.9 + dist * 0.02, 13f);
                        Fx.burst(L(e), ParticleTypes.SMOKE, from, 6, 0.1, 0.03);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1f, 0.5f); snd(e, SoundEvents.SNOWBALL_THROW, 1f, 0.4f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), r = 2f * kick(t, 12 / 28f, .3f);
                    float breech = bump(win(t, .08f, .3f));
                    rifle(p, k, 0.22f * win(t, .25f, .4f) * (1 - win(t, .7f, .9f)), r); stance(p, k);
                    p.lArmX += 0.5f * breech; p.lArmY -= 0.2f * breech; p.headX += 0.15f * breech; // slide the launcher tube
                    p.rootZ += r * 0.5f; p.bodyX -= r * 0.15f; p.rLegX += 0.15f * r;
                }));

        // ---------- SPAS-12: single blast + pump ----------
        out.add(AttackDef.of("shotgun").range(1.5f, 10).cd(26).weight(3).dur(22).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 5) {
                        for (int i = 0; i < 6; i++) bullet(e, t, 0.08, 2.6f, 0, 0xffd070);
                        flash(e, 0xffa040); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.2f, 0.85f);
                    }
                    if (n == 11) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.9f);
                    if (n == 15) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 2f); brass(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = kick(t, 5 / 22f, .25f), pump = bump(win(t, .48f, .72f));
                    rifle(p, k, 0, r * 1.5f); stance(p, k);
                    p.lArmX += 0.38f * pump * k; p.rHandX -= r * 0.4f; p.bodyZ += 0.04f * pump; p.headX += 0.05f * pump;
                }));

        // ---------- SPAS-12: double-barrel blast (secondary) ----------
        out.add(AttackDef.of("shotgun_double").range(0, 7).cd(50).weight(2).dur(32).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 8) {
                        for (int i = 0; i < 12; i++) bullet(e, t, 0.11, 2.6f, 0, 0xffc060);
                        for (LivingEntity f : Combat.cone(e, 4, 40)) Combat.knock(f, e.position(), 1.1, 0.3);
                        flash(e, 0xffa040); flash(e, 0xff6020);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.4f, 0.6f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.5f);
                    }
                    if (n == 15 || n == 22) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.9f);
                    if (n == 18 || n == 25) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 2f); brass(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .92f), r = 2.2f * kick(t, 8 / 32f, .3f);
                    float pump = bump(win(t, .45f, .6f)) + bump(win(t, .67f, .82f));
                    rifle(p, k, 0, r); stance(p, k);
                    p.rootY -= 0.4f * k; p.rHandX -= r * 0.5f; p.bodyX -= r * 0.18f; p.rootZ += r * 0.6f; p.lArmX += 0.38f * pump * k;
                }));

        // ---------- M249 SAW: long sustained burst, recoil shoves him back ----------
        out.add(AttackDef.of("m249_saw").range(3, 22).cd(90).weight(2).dur(46).weapon("m249")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.CHAIN_PLACE, 1f, 0.8f);
                    if (n >= 7 && n <= 37) {
                        bullet(e, t, 0.07, 2.4f, 1, 0xffe080);
                        if (n % 2 == 0) { flash(e, 0xffc060); brass(e); }
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.3f + e.getRandom().nextFloat() * 0.2f);
                        Vec3 b = fwd(e).scale(-0.03); e.setDeltaMovement(e.getDeltaMovement().add(b.x, 0, b.z)); e.hurtMarked = true;
                    }
                    if (n == 41) snd(e, SoundEvents.CHAIN_STEP, 1f, 0.8f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), fire = win(t, .14f, .16f) * (1 - win(t, .8f, .82f));
                    float r = 0.6f * (0.5f + 0.5f * (float) Math.sin(a * PI * 1.1f)) * fire;
                    // hip-fired, braced: wide feet, leaning into the gun
                    p.rArmX = lerp(p.rArmX, -1.05f - r * 0.2f, k); p.rArmY = lerp(p.rArmY, -0.2f, k); p.rArmZ = lerp(p.rArmZ, 0.15f, k);
                    p.lArmX = lerp(p.lArmX, -1.35f - r * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.7f, k);
                    p.rHandX = lerp(p.rHandX, -0.5f, k);
                    p.bodyX += (0.18f + r * 0.08f) * k; p.bodyY += 0.2f * k; p.headY -= 0.2f * k;
                    p.bodyZ += 0.04f * (float) Math.sin(a * 4.1f) * fire; p.rArmY += 0.04f * (float) Math.sin(a * 3.3f) * fire;
                    p.headX += 0.03f * (float) Math.sin(a * 5f) * fire;
                    wide(p, k); p.rootZ += r * 0.8f;
                }));

        // ---------- M40A1: kneel, scope in, one round, work the bolt ----------
        out.add(AttackDef.of("m40a1_sniper").range(10, 44).cd(70).weight(3).dur(46).weapon("m40a1")
                .tick((e, t, n) -> {
                    if (n == 8) snd(e, SoundEvents.SPYGLASS_USE, 1f, 0.9f);
                    if (n == 26) {
                        bullet(e, t, 0, 26f, 2, 0xffffff); flash(e, 0xffc060);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.5f, 0.6f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.6f);
                        if (t != null) blood(e, t, 10);
                    }
                    if (n == 32) snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.7f, 1.8f);
                    if (n == 36) { snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.7f, 1.8f); brass(e); }
                    if (n == 40) snd(e, SoundEvents.SPYGLASS_STOP_USING, 1f, 0.9f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), zoom = win(t, .15f, .4f) * (1 - win(t, .82f, .9f));
                    float r = 2f * kick(t, 26 / 46f, .2f), bolt = bump(win(t, .66f, .82f));
                    rifle(p, k, 0, r); kneel(p, k);
                    p.headX += 0.22f * zoom; p.headY -= 0.1f * zoom; p.bodyX += 0.1f * zoom;
                    p.bodyZ += 0.01f * (float) Math.sin(a * 0.5f) * zoom * (1 - win(t, .5f, .56f)); // holds breath
                    p.rArmZ += 0.5f * bolt; p.rArmX += 0.3f * bolt; p.rHandY -= 0.6f * bolt; // bolt up and back
                    p.rHandX -= r * 0.3f;
                }));

        // ---------- RPG: shoulder it, laser-paint the target, guide the rocket in ----------
        out.add(AttackDef.of("rpg_laser_guided").range(8, 36).cd(110).weight(2).dur(52).weapon("rpg")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.STONE_BUTTON_CLICK_ON, 1f, 1.5f);
                    if (n >= 4 && n <= 48 && n % 2 == 0) laser(e, t);
                    if (n == 14) {
                        Combat.shoot(e, RPG, mz(e), aim(e, t, 0.01).add(0, 0.05, 0).normalize(), 0.9, 18f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.8, 0.3, 0.1), 8, 0.25, 0.05); flash(e, 0xffa040);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.5f); snd(e, SoundEvents.GHAST_SHOOT, 0.4f, 1.3f);
                    }
                    if (n == 44) snd(e, SoundEvents.PISTON_EXTEND, 0.6f, 1.3f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), r = 2f * kick(t, 14 / 52f, .25f), guide = win(t, .3f, .4f) * (1 - win(t, .8f, .88f));
                    // tube on the right shoulder: upper arm high, elbow out
                    p.rArmX = lerp(p.rArmX, -2.3f - r * 0.15f, k); p.rArmY = lerp(p.rArmY, -0.1f, k); p.rArmZ = lerp(p.rArmZ, 0.35f, k);
                    p.rHandX = lerp(p.rHandX, 0.75f, k);
                    p.lArmX = lerp(p.lArmX, -1.6f - r * 0.1f, k); p.lArmY = lerp(p.lArmY, 0.4f, k);
                    p.headZ -= 0.2f * k; p.headY -= 0.15f * k; p.headX += 0.1f * k;
                    p.bodyY += 0.25f * k; p.bodyX -= r * 0.1f; p.rootZ += r * 0.6f;
                    p.headY += 0.04f * (float) Math.sin(a * 0.6f) * guide; p.bodyY += 0.04f * (float) Math.sin(a * 0.6f) * guide;
                    stance(p, k); p.lArmX += 0.3f * bump(win(t, .82f, .92f)); // reload slap
                }));

        // ---------- Displacer: spin up the coils, fire a portal ball ----------
        out.add(AttackDef.of("displacer").range(5, 26).cd(120).weight(2).dur(40).weapon("displacer")
                .tick((e, t, n) -> {
                    if (n < 24) {
                        double ang = n * 0.8; Vec3 c = mz(e);
                        Fx.dustBurst(L(e), PORTAL_C, 0.9f, c.add(Math.cos(ang) * 0.4, Math.sin(ang) * 0.4, 0), 2, 0.05);
                        Fx.burst(L(e), ParticleTypes.PORTAL, c, 3, 0.2, 0.2);
                        if (n % 4 == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.7f, 0.6f + n * 0.05f);
                    }
                    if (n == 24) {
                        Combat.shoot(e, DISPLACER, mz(e), aim(e, t, 0), 0.7, 0);
                        Fx.dustBurst(L(e), PORTAL_C, 2f, mz(e), 20, 0.5);
                        snd(e, SoundEvents.ENDERMAN_TELEPORT, 1.4f, 0.5f); snd(e, SoundEvents.BEACON_DEACTIVATE, 1f, 1.5f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), ch = win(t, .05f, .6f) * (1 - win(t, .6f, .61f)), r = 2.4f * kick(t, 24 / 40f, .3f);
                    p.rArmX = lerp(p.rArmX, -1.15f - r * 0.2f, k); p.rArmY = lerp(p.rArmY, -0.2f, k); p.rArmZ = lerp(p.rArmZ, 0.15f, k);
                    p.lArmX = lerp(p.lArmX, -1.4f - r * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.7f, k);
                    p.rHandX = lerp(p.rHandX, -0.4f, k);
                    float shake = 0.05f * ch * ch;
                    p.bodyZ += shake * (float) Math.sin(a * 5f); p.rArmY += shake * (float) Math.cos(a * 4.3f);
                    p.bodyX += (0.12f - 0.1f * ch) * k - r * 0.1f; p.headX -= 0.12f * ch; p.rootZ += r;
                    wide(p, k * 0.7f); p.rootY += 0.4f * ch;
                }));

        // ---------- Displacer: self-teleport when hurt (portal out, reappear at range) ----------
        out.add(AttackDef.of("displacer_teleport").range(0, 8).cd(400).weight(4).dur(30).weapon("displacer")
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.45f)
                .tick((e, t, n) -> {
                    if (n < 20) {
                        Fx.burst(L(e), ParticleTypes.PORTAL, e.position().add(0, 1, 0), 8, 0.5, 0.4);
                        if (n % 4 == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.7f, 0.8f + n * 0.06f);
                    }
                    if (n == 20) {
                        Vec3 from = e.position(), away = t != null ? from.subtract(t.position()) : fwd(e).scale(-1);
                        away = new Vec3(away.x, 0, away.z); away = away.lengthSqr() < 1e-4 ? fwd(e).scale(-1) : away.normalize();
                        Vec3 to = from.add(away.scale(11));
                        Fx.burst(L(e), ParticleTypes.REVERSE_PORTAL, from.add(0, 1, 0), 50, 0.6, 0.3); Fx.dustBurst(L(e), PORTAL_C, 2f, from.add(0, 1, 0), 30, 0.8);
                        e.teleportTo(to.x, to.y + 1, to.z); e.heal(4);
                        Fx.burst(L(e), ParticleTypes.REVERSE_PORTAL, e.position().add(0, 1, 0), 50, 0.6, 0.3); Fx.ring(L(e), e.position().add(0, 0.1, 0), 1.5f, PORTAL_C, 1.5f, 20);
                        snd(e, SoundEvents.CHORUS_FRUIT_TELEPORT, 1.5f, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), ch = win(t, .1f, .66f) * (1 - win(t, .66f, .7f));
                    // muzzle turned on himself: weapon pulled into the chest, hunched
                    p.rArmX = lerp(p.rArmX, -0.9f, k); p.rArmY = lerp(p.rArmY, 0.5f, k); p.rHandX = lerp(p.rHandX, 1.2f, k);
                    p.lArmX = lerp(p.lArmX, -1.0f, k); p.lArmY = lerp(p.lArmY, -0.3f, k);
                    p.bodyX += 0.35f * k * ch; p.headX += 0.4f * k * ch; p.rootY -= 1.2f * ch;
                    p.bodyZ += 0.05f * ch * (float) Math.sin(a * 6f);
                    p.lLegX = lerp(p.lLegX, -0.3f, ch); p.rLegX = lerp(p.rLegX, 0.3f, ch);
                    p.rootY += 1.5f * bump(win(t, .66f, .85f)); // pops out of the portal
                }));

        // ---------- Shock Roach: living arm-cannon, stream of shock bolts ----------
        out.add(AttackDef.of("shock_roach").range(3, 20).cd(34).weight(3).dur(26).weapon("roach")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.SILVERFISH_AMBIENT, 1f, 0.6f);
                    if (n >= 4 && n <= 20 && n % 2 == 0) {
                        Combat.shoot(e, SHOCK, mz(e), aim(e, t, 0.025), 1.9, 3.2f);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mz(e), 4, 0.1, 0.1);
                        snd(e, SoundEvents.GUARDIAN_ATTACK, 0.4f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .14f, .16f) * (1 - win(t, .78f, .8f));
                    float r = 0.3f * (0.5f + 0.5f * (float) Math.sin(a * PI)) * fire;
                    // right forearm thrust out, left hand clamps the wrist (the roach bucks)
                    p.rArmX = lerp(p.rArmX, -HALF - r * 0.3f, k); p.rArmY = lerp(p.rArmY, 0.1f, k);
                    p.lArmX = lerp(p.lArmX, -1.35f - r * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.75f, k);
                    p.rHandZ += 0.1f * (float) Math.sin(a * 2.6f) * k; p.rHandX += 0.06f * (float) Math.sin(a * 3.4f) * k;
                    p.bodyY += 0.15f * k; p.headY -= 0.15f * k; p.bodyX += 0.08f * k - r * 0.05f; p.rootZ += r * 0.4f;
                    stance(p, k);
                }));

        // ---------- Spore Launcher: lobbed bouncing spore ----------
        out.add(AttackDef.of("spore_launcher").range(5, 18).cd(60).weight(2).dur(26).weapon("spore")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.SLIME_SQUISH, 1f, 0.6f);
                    if (n == 11) {
                        Vec3 from = mz(e); double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, t, 0.02).add(0, 0.1 + dist * 0.017, 0).normalize();
                        Combat.shoot(e, SPORE, from, d, 0.8 + dist * 0.022, 12f);
                        Fx.burst(L(e), ParticleTypes.ITEM_SLIME, from, 8, 0.1, 0.1);
                        snd(e, SoundEvents.SLIME_JUMP, 1.3f, 0.6f); snd(e, SoundEvents.LLAMA_SPIT, 1f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .9f), squeeze = bump(win(t, .1f, .42f)), r = 1.6f * kick(t, 11 / 26f, .3f);
                    rifle(p, k, 0.25f * win(t, .1f, .4f) * (1 - win(t, .6f, .85f)), r); stance(p, k);
                    p.rHandZ += 0.12f * squeeze * (float) Math.sin(a * 3f); p.lArmX += 0.2f * squeeze; // creature gags up the spore
                    p.bodyX -= 0.08f * squeeze; p.rootY -= 0.3f * squeeze;
                }));

        // ---------- Spore Launcher: direct spit ----------
        out.add(AttackDef.of("spore_direct").range(3, 16).cd(40).weight(1).dur(18).weapon("spore")
                .tick((e, t, n) -> {
                    if (n == 6) {
                        Combat.shoot(e, SPORE_SHOT, mz(e), aim(e, t, 0.01), 1.4, 9f);
                        Fx.burst(L(e), ParticleTypes.ITEM_SLIME, mz(e), 6, 0.1, 0.1);
                        snd(e, SoundEvents.LLAMA_SPIT, 1.2f, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .85f), r = 1.4f * kick(t, 6 / 18f, .3f);
                    rifle(p, k, 0, r); stance(p, k); p.rHandX -= r * 0.3f; p.rHandZ += 0.1f * bump(win(t, .1f, .33f));
                }));

        // ---------- Penguin: underhand release of two exploding penguins ----------
        out.add(AttackDef.of("penguin").range(3, 14).cd(160).weight(1).dur(26).weapon("penguin")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.PARROT_AMBIENT, 1f, 0.6f);
                    if (n == 13 || n == 16) {
                        Vec3 from = Combat.muzzle(e, 0.8, 0.3, -0.5);
                        Combat.shoot(e, PENGUIN, from, aim(e, t, 0.15).add(0, 0.3, 0).normalize(), 0.6, 16f);
                        snd(e, SoundEvents.PARROT_HURT, 1f, 0.5f); snd(e, SoundEvents.WITCH_THROW, 0.8f, 1.2f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponR = t < .62f ? "penguin" : null;
                    float hit = 13 / 26f;
                    p.rArmX = track(t, 0, -0.6f, .3f, 0.9f, hit, -1.1f, .65f, -1.3f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .3f, 0.2f, hit, 0, 1, 0);
                    p.lArmX = track(t, 0, -0.6f, .3f, -0.4f, hit, -0.2f, 1, 0); p.lArmZ = -0.3f * bump(win(t, .2f, .8f));
                    p.bodyX = track(t, 0, 0, .3f, 0.45f, hit, 0.25f, .7f, 0.15f, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, 0.3f, hit, -0.2f, 1, 0); p.headY = -p.bodyY * 0.6f; p.headX = -p.bodyX * 0.5f;
                    p.lLegX = track(t, 0, 0, .25f, -0.6f, .85f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.4f, 1, 0);
                    p.rootY = track(t, 0, 0, .3f, -1.6f, hit, -1.0f, 1, 0); p.rootZ = track(t, .25f, 0, hit, -1f, 1, 0);
                }));

        // ---------- Hand grenade: pull pin, overhand throw ----------
        out.add(AttackDef.of("hand_grenade").range(5, 18).cd(140).weight(2).dur(24).weapons("grenade", null)
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.TRIPWIRE_CLICK_OFF, 1f, 1.6f);
                    if (n == 13) {
                        Vec3 from = Combat.muzzle(e, 0.6, 0.35, 0.4);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, t, 0.02).add(0, 0.15 + dist * 0.018, 0).normalize();
                        Combat.shoot(e, FRAG, from, d, 0.75 + dist * 0.025, 15f);
                        snd(e, SoundEvents.WITCH_THROW, 1f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponR = t < .55f ? "grenade" : null;
                    float hit = 13 / 24f;
                    p.lArmX = track(t, 0, 0, .12f, -1.2f, .25f, -1.1f, .4f, -1.0f, hit, -0.2f, .8f, -0.8f, 1, 0);
                    p.lArmY = track(t, 0, 0, .12f, -0.5f, .25f, -0.4f, .4f, 0.4f, 1, 0); // left hand yanks the pin, then points
                    p.rArmX = track(t, 0, -0.4f, .12f, -1.1f, .25f, -1.1f, .45f, -2.9f, hit, -2.4f, .7f, -0.9f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .45f, 0.35f, hit, 0, 1, 0);
                    p.bodyY = track(t, 0, 0, .45f, 0.55f, .62f, -0.45f, .85f, -0.25f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .45f, -0.25f, .62f, 0.35f, 1, 0);
                    p.lLegX = track(t, 0, 0, .4f, -0.4f, .62f, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, .45f, 0.35f, .62f, 0.45f, 1, 0);
                    p.rootZ = track(t, .4f, 0.5f, .62f, -1.3f, 1, 0); p.rootY = -0.5f * bump(win(t, .4f, .85f));
                }));

        // ---------- Satchel charge: slide it out, pull the remote, click ----------
        out.add(AttackDef.of("satchel_charge").range(4, 14).cd(240).weight(1).dur(48).weapon("satchel")
                .onStart((e, t, n) -> e.getPersistentData().putBoolean(SAT, false))
                .tick((e, t, n) -> {
                    if (n == 10) {
                        Vec3 from = Combat.muzzle(e, 0.6, 0.3, -0.4); double dist = t != null ? from.distanceTo(t.position()) : 8;
                        Vec3 d = (t != null ? t.position().add(0, 0.2, 0).subtract(from).normalize() : e.getViewVector(1f)).add(0, 0.12 + dist * 0.015, 0).normalize();
                        Combat.shoot(e, SATCHEL, from, d, 0.55 + dist * 0.03, 0);
                        snd(e, SoundEvents.WITCH_THROW, 1f, 0.6f);
                    }
                    if (n == 28) snd(e, SoundEvents.STONE_BUTTON_CLICK_OFF, 1f, 1.4f);
                    if (n == 38) {
                        snd(e, SoundEvents.STONE_BUTTON_CLICK_ON, 1f, 2f);
                        CompoundTag d = e.getPersistentData();
                        if (d.getBoolean(SAT)) {
                            Vec3 c = getVec(d, SAT); d.putBoolean(SAT, false);
                            Combat.blast(e, c, 5f, 24f, 1.4f, null); Fx.explosion(L(e), c, 4.5f);
                            Fx.sound(L(e), c, SoundEvents.GENERIC_EXPLODE, 2.2f, 0.7f);
                        }
                    }
                })
                .anim((p, t, a) -> {
                    float thr = 10 / 48f, press = 38 / 48f;
                    p.weaponR = t < thr + .04f ? "satchel" : t > .45f ? "detonator" : null;
                    // underhand bowl
                    p.rArmX = track(t, 0, -0.3f, .12f, 0.8f, thr, -1.0f, .32f, -0.4f, .45f, -1.2f, press - .04f, -1.3f, press, -1.45f, .9f, -1.3f, 1, 0);
                    p.rArmY = track(t, .32f, 0, .45f, 0.4f, .9f, 0.4f, 1, 0);
                    p.bodyX = track(t, 0, 0, .12f, 0.4f, thr, 0.2f, .32f, 0, 1, 0);
                    p.bodyY = track(t, 0, 0, .12f, 0.25f, thr, -0.15f, .4f, 0, .5f, -0.2f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.headX = track(t, .45f, 0, .55f, 0.25f, press, 0.25f, press + .04f, -0.05f, 1, 0); // glances at remote, then at the blast
                    p.lArmX = track(t, 0, 0, .12f, -0.5f, thr, 0, .45f, -0.9f, 1, 0); p.lArmZ = track(t, .45f, 0, .55f, -0.3f, 1, 0);
                    p.rHandX = 0.4f * kick(t, press, .1f); p.rArmX -= 0.15f * kick(t, press, .15f);
                    p.lLegX = track(t, 0, 0, .12f, -0.6f, .4f, -0.3f, 1, 0); p.rLegX = track(t, 0, 0, .12f, 0.4f, .4f, 0.2f, 1, 0);
                    p.rootY = track(t, 0, 0, .12f, -1.5f, .3f, -0.4f, 1, 0);
                    p.rootZ = 0.6f * kick(t, press + 0.02f, .2f); // flinch at the boom
                }));

        // ---------- Tripmine: kneel, stick it down, arm the beam ----------
        out.add(AttackDef.of("tripmine").range(5, 16).cd(300).weight(1).dur(32).weapon("tripmine")
                .when((e, t) -> !e.getPersistentData().getBoolean(TM))
                .tick((e, t, n) -> {
                    if (n == 18) {
                        Vec3 f = fwd(e), a = e.position().add(f.scale(1.4)).add(0, 0.25, 0);
                        Vec3 dir = t != null ? t.position().add(0, 0.6, 0).subtract(a).normalize() : f;
                        Vec3 b = a.add(dir.scale(t != null ? Math.min(12, a.distanceTo(t.position()) + 3) : 8));
                        CompoundTag d = e.getPersistentData();
                        putVec(d, TM, a); putVec(d, TM + "e", b); d.putLong(TM + "t", e.level().getGameTime() + 600);
                        snd(e, SoundEvents.TRIPWIRE_ATTACH, 1f, 0.8f);
                    }
                    if (n == 22) snd(e, SoundEvents.BEACON_ACTIVATE, 0.7f, 2f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .3f, .85f), plant = 18 / 32f;
                    p.weaponR = t < plant ? "tripmine" : null;
                    kneel(p, k);
                    p.bodyX += 0.55f * k; p.headX -= 0.25f * k; // looks along the ground
                    p.rArmX = track(t, 0, -0.4f, .35f, -0.9f, plant - .05f, -0.75f, plant, -0.55f, .7f, -0.6f, 1, 0);
                    p.rArmZ = track(t, .3f, 0, plant, 0.15f, 1, 0);
                    p.rHandX = 0.3f * kick(t, plant, .15f);
                    p.lArmX = track(t, 0, 0, .35f, -0.6f, .85f, -0.6f, 1, 0); p.lArmZ = -0.2f * k; // braced on the knee
                }));

        // ---------- Snarks: squeeze the nest, toss three hungry snarks ----------
        out.add(AttackDef.of("snark").range(3, 14).cd(180).weight(1).dur(28).weapons("snark", null)
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.SILVERFISH_AMBIENT, 1.2f, 1.4f);
                    if (n == 12 || n == 15 || n == 18) {
                        Vec3 from = Combat.muzzle(e, 0.8, 0.2, 0);
                        Combat.shoot(e, SNARK, from, aim(e, t, 0.25).add(0, 0.25, 0).normalize(), 0.65, 4f);
                        snd(e, SoundEvents.SILVERFISH_HURT, 1f, 1.6f + n * 0.02f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponR = t < .7f ? "snark" : null;
                    float hold = ready(t, .15f, .7f);
                    float toss = bump(win(t, .38f, .5f)) + bump(win(t, .5f, .6f)) + bump(win(t, .6f, .7f));
                    p.rArmX = lerp(p.rArmX, -1.0f - 0.5f * toss, hold); p.rArmY = lerp(p.rArmY, 0.35f, hold);
                    p.lArmX = lerp(p.lArmX, -0.95f, hold); p.lArmY = lerp(p.lArmY, -0.35f, hold); // cupping the nest
                    p.rArmZ += 0.06f * (float) Math.sin(a * 4f) * hold * (1 - win(t, .35f, .4f)); // it wriggles
                    p.bodyX += (0.15f + 0.1f * toss) * hold; p.headX += 0.2f * hold; p.bodyY += 0.12f * toss;
                    p.lLegX = lerp(p.lLegX, -0.35f, hold); p.rLegX = lerp(p.rLegX, 0.3f, hold); p.rootY -= 0.5f * hold;
                    p.rootZ -= 0.4f * toss;
                }));
    }

    // ================= model =================
    static float h(int x, int y, int s) { int q = x * 374761393 + y * 668265263 + s * 1274126177; q = (q ^ (q >>> 13)) * 1274126177; q ^= q >>> 16; return (q & 0xffff) / 65535f; }
    /** HECU woodland camo: two blotch layers over the base */
    static Mat camo(int base) {
        return Mat.of(base).noise(.1f).decal((m, g, x, y, w, hh, f) -> {
            for (int j = 0; j < hh; j++) for (int i = 0; i < w; i++) {
                float a = h((x + i) / 2, (y + j) / 2, 11), b = h((x + i) / 2 + 7, (y + j) / 3, 23);
                if (a < 0.22f) g.px(x + i, y + j, 0x2e2a1c); else if (b < 0.2f) g.px(x + i, y + j, 0x6b5a3a); else if (a > 0.86f) g.px(x + i, y + j, 0x3e5a2c);
            }
        });
    }

    @Override
    public void buildModel(Rig rig) {
        Mat fat = camo(0x55633e), vest = Mat.of(0x3e4530).noise(.12f).bevel(.25f), pouch = Mat.of(0x4a5236).noise(.12f).bevel(.3f);
        Mat helm = camo(0x4b5a36).bevel(.3f), mask = Mat.of(0x2a2c28).noise(.08f), rubber = Mat.of(0x1c1d1b).noise(.05f);
        Mat lens = Mat.of(0x9adf8a).glow(), black = Mat.of(0x181818), boot = Mat.of(0x221a14).noise(.1f).bevel(.3f), metal = Mat.of(0x6c6e6a).noise(.06f);
        Mat radio = Mat.of(0x3a3f2c).noise(.1f).bevel(.25f).decal((m, g, x, y, w, hh, f) -> {
            if (f == Mat.Face.BACK) { g.rect(x + 1, y + 1, 2, 1, 0x101010); g.glow(x + w - 2, y + 1, 0xff3020); }
        });
        Mat chest = Mat.of(0x3e4530).bevel(.25f).decal((m, g, x, y, w, hh, f) -> {
            if (f != Mat.Face.FRONT) return;
            g.rect(x + 1, y + 1, w - 2, 1, 0x2a2e20); g.rectGlow(x + w / 2, y + hh - 2, 1, 1, 0x60ff60); // PCV charge LED
        });
        // torso: fatigues, PCV vest, magazine pouches, belt, back radio + antenna
        rig.box(rig.torso, fat, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4, -11.5f, -2.5f, 8, 8, 5, .25f);
        rig.box(rig.torso, pouch, -3.5f, -6.5f, -3.6f, 3, 3, 1);
        rig.box(rig.torso, pouch, 0.5f, -6.5f, -3.6f, 3, 3, 1);
        rig.box(rig.torso, black, -4.5f, -2.5f, -2.5f, 9, 2, 5);
        rig.box(rig.torso, metal, -1, -2.3f, -2.9f, 2, 1, 1);
        rig.box(rig.torso, pouch, 2.5f, -3, -2.8f, 2, 3, 2);
        rig.box(rig.torso, radio, -3, -11, 2.4f, 6, 7, 2);
        rig.box(rig.torso, black, 2, -18, 3, 1, 7, 1);
        // head: PASGT helmet, gas mask, round green lenses, filter, hoses
        rig.box(rig.head, mask, -4, -8, -4, 8, 8, 8);
        rig.box(rig.head, helm, -4.5f, -9.5f, -4.5f, 9, 3, 9, .2f);
        rig.box(rig.head, helm, -5, -7, -4.5f, 10, 1, 9);
        rig.box(rig.head, lens, -3, -5.5f, -4.6f, 2, 2, 1);
        rig.box(rig.head, lens, 1, -5.5f, -4.6f, 2, 2, 1);
        rig.box(rig.head, rubber, -1.5f, -3, -5.5f, 3, 3, 2);
        rig.box(rig.head, metal, -1, -2.5f, -6.3f, 2, 2, 1);
        rig.box(rig.head, rubber, -4.6f, -4, -2, 1, 1, 4);
        rig.box(rig.head, rubber, 3.6f, -4, -2, 1, 1, 4);
        // arms: camo sleeves, shoulder pads, black gloves
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, fat, -2, -2, -2, 4, 12, 4);
            rig.box(arm, vest, s < 0 ? -2.8f : -2.2f, -2.5f, -2.5f, 5, 3, 5);
            rig.box(arm, black, -2, 7, -2, 4, 3, 4, .2f);
        }
        rig.box(rig.lArm, black, -2.3f, 3, -1, 1, 2, 2); // watch
        // legs: camo trousers, knee pads, thigh holster, boots
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, fat, -2, 0, -2, 4, 12, 4);
            rig.box(leg, rubber, -1.5f, 5, -2.6f, 3, 2, 1);
            rig.box(leg, boot, -2, 9, -2.5f, 4, 3, 5, .2f);
        }
        rig.box(rig.rLeg, black, -3, 1, -1, 1, 4, 3);
        buildWeapons(rig, metal, black);
    }

    private static void buildWeapons(Rig rig, Mat metal, Mat black) {
        Mat gun = Mat.of(0x2c2d2e).noise(.07f), steel = Mat.of(0x9a9c98).noise(.05f), olive = Mat.of(0x4a5236).noise(.1f);
        Mat wood = Mat.of(0x6a4528).noise(.15f), red = Mat.of(LASER).glow(), green = Mat.of(PORTAL_C).glow(), shock = Mat.of(SHOCK_C).glow();
        Mat flesh = Mat.of(0x7a4a6a).noise(.25f), spore = Mat.of(0x5a7a30).noise(.25f), sporeG = Mat.of(SPORE_C).glow(), tan = Mat.of(0x6b5a3a).noise(.12f);
        PartDefinition w;
        w = rig.weapon(rig.rHand, "knife");
        rig.box(w, steel, -0.5f, -1, -7, 1, 2, 6); rig.box(w, black, -0.5f, -0.5f, -1, 1, 1, 3); rig.box(w, metal, -1, -1, -1.5f, 2, 2, 1);
        w = rig.weapon(rig.rHand, "wrench");
        rig.box(w, Mat.of(0xa03020).noise(.12f), -0.5f, -0.5f, -11, 1, 1, 13); rig.box(w, metal, -1.5f, -1, -14, 3, 2, 3); rig.box(w, metal, -1.5f, -3, -14, 1, 2, 2);
        w = rig.weapon(rig.rHand, "m9");
        rig.box(w, gun, -1, -2, -6, 2, 2, 7); rig.box(w, black, -1, 0, -1, 2, 3, 2); rig.box(w, metal, -0.5f, -2.3f, -6.3f, 1, 1, 1);
        w = rig.weapon(rig.rHand, "deagle");
        rig.box(w, steel, -1, -2.5f, -7, 2, 3, 8); rig.box(w, black, -1, 0.5f, -1, 2, 3, 2); rig.box(w, gun, -0.5f, -0.5f, -6, 1, 1, 3); rig.box(w, red, -0.5f, -0.4f, -6.4f, 1, 1, 1);
        w = rig.weapon(rig.rHand, "python");
        rig.box(w, steel, -0.5f, -2.5f, -8, 1, 2, 6); rig.box(w, steel, -1.5f, -2.5f, -3, 3, 3, 3); rig.box(w, wood, -1, 0, -1, 2, 3, 2); rig.box(w, metal, -0.5f, -3.2f, -8, 1, 1, 5);
        w = rig.weapon(rig.rHand, "mp5");
        rig.box(w, gun, -1, -2.5f, -6, 2, 3, 9); rig.box(w, black, -1, -2, -11, 2, 2, 5); rig.box(w, metal, -0.5f, -1.5f, -13, 1, 1, 2);
        rig.box(w, black, -0.5f, 0.5f, -4, 1, 3, 1); rig.box(w, metal, -1, -1.5f, 3, 2, 1, 3); rig.box(w, olive, -1, -0.3f, -10, 2, 1, 5);
        w = rig.weapon(rig.rHand, "shotgun");
        rig.box(w, gun, -1.5f, -2.5f, -5, 3, 3, 7); rig.box(w, metal, -0.5f, -2.8f, -15, 1, 1, 10); rig.box(w, black, -1, -1.6f, -12, 2, 1, 7);
        rig.box(w, black, -1, -1.5f, 2, 2, 2, 4); rig.box(w, metal, -1.5f, -1.5f, 5.5f, 3, 1, 1);
        w = rig.weapon(rig.rHand, "m249");
        rig.box(w, gun, -1.5f, -3, -6, 3, 4, 10); rig.box(w, metal, -0.5f, -2, -15, 1, 1, 9); rig.box(w, olive, -2, 1, -5, 4, 3, 4);
        rig.box(w, black, -1, -4, -4, 2, 1, 5); rig.box(w, metal, -1.5f, -1, -14, 3, 1, 1); rig.box(w, gun, -1, -2, 4, 2, 3, 4);
        w = rig.weapon(rig.rHand, "m40a1");
        rig.box(w, olive, -1, -2, -9, 2, 2, 15); rig.box(w, metal, -0.5f, -1.8f, -19, 1, 1, 10); rig.box(w, black, -1, -4.5f, -6, 2, 2, 7);
        rig.box(w, Mat.of(0x80c0ff).glow(), -0.5f, -4.2f, -6.2f, 1, 1, 1); rig.box(w, metal, 1, -2, -2, 1, 1, 1);
        w = rig.weapon(rig.rHand, "rpg");
        rig.box(w, olive, -1.5f, -3.5f, -14, 3, 3, 22); rig.box(w, black, -2, -4, -15, 4, 4, 1); rig.box(w, black, -0.5f, 0, -2, 1, 3, 2);
        rig.box(w, metal, -2.5f, -4, -6, 1, 2, 3); rig.box(w, red, -2.6f, -3.5f, -6.3f, 1, 1, 1);
        w = rig.weapon(rig.rHand, "displacer");
        rig.box(w, gun, -2.5f, -3.5f, -7, 5, 5, 10); rig.box(w, green, -2, -3, -9, 4, 4, 1); rig.box(w, green, -2, -3, -5, 4, 4, 1);
        rig.box(w, steel, -3, -4, -11, 1, 6, 2); rig.box(w, steel, 2, -4, -11, 1, 6, 2); rig.box(w, black, -0.5f, 1.5f, -2, 1, 3, 2);
        w = rig.weapon(rig.rHand, "roach");
        rig.box(w, flesh, -1.5f, -2.5f, -6, 3, 3, 7); rig.box(w, flesh, -1, -2, -8, 2, 2, 2); rig.box(w, shock, -2, -3, -4, 1, 1, 1);
        rig.box(w, shock, 1, -3, -4, 1, 1, 1); rig.box(w, Mat.of(0x4a3048), -2, -0.5f, -5, 4, 1, 4);
        w = rig.weapon(rig.rHand, "spore");
        rig.box(w, spore, -2, -3, -7, 4, 4, 9); rig.box(w, sporeG, -1, -2, -9, 2, 2, 2); rig.box(w, spore, -0.5f, -4.5f, -3, 1, 2, 3);
        rig.box(w, tan, -1, 1, -2, 2, 2, 3);
        w = rig.weapon(rig.rHand, "barnacle");
        rig.box(w, Mat.of(0x8a6a50).noise(.2f), -1.5f, -2, -5, 3, 3, 5); rig.box(w, Mat.of(0xb04060), -1, -1.5f, -7, 2, 2, 2); rig.box(w, black, -1.5f, -0.5f, -1, 3, 1, 2);
        w = rig.weapon(rig.rHand, "penguin");
        rig.box(w, black, -1.5f, -3, -2, 3, 4, 3); rig.box(w, Mat.of(0xe8e8e8), -1, -2.5f, -2.3f, 2, 3, 1); rig.box(w, Mat.of(0xe0a020), -0.5f, -2.2f, -3, 1, 1, 1);
        rig.box(w, Mat.of(0xff3030).glow(), -1, -3.2f, -2.2f, 2, 1, 1);
        w = rig.weapon(rig.rHand, "grenade");
        rig.box(w, olive, -1, -1, -2, 2, 2, 2); rig.box(w, metal, -0.5f, -2, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.rHand, "satchel");
        rig.box(w, tan, -2, -1.5f, -3, 4, 3, 3); rig.box(w, black, -2.2f, -0.5f, -3.2f, 4, 1, 1);
        w = rig.weapon(rig.rHand, "detonator");
        rig.box(w, olive, -1, -1, -2, 2, 2, 3); rig.box(w, red, -0.5f, -1.6f, -1.5f, 1, 1, 1); rig.box(w, black, -0.3f, -3, -1, 1, 2, 1);
        w = rig.weapon(rig.rHand, "tripmine");
        rig.box(w, metal, -1.5f, -1.5f, -2, 3, 3, 2); rig.box(w, Mat.of(0x30ff40).glow(), -0.5f, -0.5f, -2.5f, 1, 1, 1);
        w = rig.weapon(rig.rHand, "snark");
        rig.box(w, Mat.of(0x5a3a20).noise(.25f), -2, -2.5f, -3, 4, 3, 4); rig.box(w, Mat.of(0xc04040), -1, -3.3f, -2.5f, 2, 1, 2); rig.box(w, Mat.of(0x60ff40).glow(), -0.5f, -2.8f, -3.2f, 1, 1, 1);
    }
}
