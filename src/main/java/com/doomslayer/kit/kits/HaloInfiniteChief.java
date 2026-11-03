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
import java.util.function.Predicate;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Master Chief, Halo Infinite (2021): weathered Mjolnir Mark VII, energy shield, Grappleshot + equipment,
 * the full UNSC / Banished sandbox, grenades, rifle bash and assassination.
 */
public class HaloInfiniteChief extends SlayerKit {
    static final int VISOR = 0xffa53a, SHIELD = 0xffd84a, HOOK = 0x9fe6ff, TRACER = 0xffe6a0, BANISHED = 0xff8a2a, ELEC = 0x8fe0ff;
    static final float SHIELD_MAX = 40;

    // ---------------- projectiles ----------------
    static final ProjStyle FRAG = ProjStyle.make().color(0x47552f).size(0.28f).life(34).explode(3.5f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle PLASMA_NADE = ProjStyle.make().color(0x40a0ff).size(0.32f).life(40).explode(3.2f).gravity(0.035).homing(0.2f).trail(ParticleTypes.ELECTRIC_SPARK);
    static final ProjStyle HYDRA_ROCKET = ProjStyle.make().color(0x9fc0d0).size(0.22f).life(60).explode(1.7f).homing(0.12f).trail(ParticleTypes.SMOKE);
    static final ProjStyle CINDER = ProjStyle.make().color(BANISHED).size(0.3f).life(32).explode(2.8f).gravity(0.03).bounce().trail(ParticleTypes.FLAME);
    static final ProjStyle REFLECT = ProjStyle.make().color(ELEC).size(0.25f).life(40).trail(ParticleTypes.ELECTRIC_SPARK);
    static final ProjStyle SKEWER_SPIKE = ProjStyle.make().color(0xd0c8b8).size(0.3f).life(40).gravity(0.005).pierce(2).trail(ParticleTypes.CRIT)
            .onHit((p, pos, hit) -> {
                if (hit != null) Combat.knock(hit, pos.subtract(p.getDeltaMovement()), 1.5, 0.3);
                Fx.sparks((ServerLevel) p.level(), pos, 12);
                Fx.sound((ServerLevel) p.level(), pos, SoundEvents.TRIDENT_HIT, 1.2f, 0.6f);
            });
    static final ProjStyle RAVAGER_BLOB = ProjStyle.make().color(0xff6a20).size(0.4f).life(50).explode(1.6f).gravity(0.04).trail(ParticleTypes.FLAME)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                ProjStyle.defaultImpact(p, pos, hit);
                Fx.burst(l, ParticleTypes.FLAME, pos, 30, 1.0, 0.03); Fx.ring(l, pos.add(0, 0.1, 0), 2f, 0xff7020, 1.3f, 18);
                Fx.sound(l, pos, SoundEvents.FIRECHARGE_USE, 1f, 0.7f);
                if (p.getOwner() instanceof SlayerEntity o) for (LivingEntity x : Combat.around(o, pos, 2.4)) x.setSecondsOnFire(4);
            });
    static final ProjStyle DISRUPT_BOLT = ProjStyle.make().color(ELEC).size(0.22f).life(40).homing(0.08f).trail(ParticleTypes.ELECTRIC_SPARK)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 16, 0.3, 0.3);
                Fx.sound(l, pos, SoundEvents.BEEHIVE_WORK, 1f, 2f);
                if (hit instanceof LivingEntity le) {
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                }
            });
    static final ProjStyle SENSOR = ProjStyle.make().color(0xff3030).size(0.18f).life(40).gravity(0.03).trail(ParticleTypes.END_ROD)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.sound(l, pos, SoundEvents.AMETHYST_BLOCK_CHIME, 2f, 0.6f);
                for (int i = 1; i <= 4; i++) Fx.ring(l, pos.add(0, 0.2, 0), i * 3.4f, 0xff3030, 1.2f, 18 + i * 8);
                if (p.getOwner() instanceof SlayerEntity o) for (LivingEntity x : Combat.around(o, pos, 14)) {
                    x.addEffect(new MobEffectInstance(MobEffects.GLOWING, 220, 0));
                    Fx.dustBurst(l, 0xff3030, 1f, x.position().add(0, x.getBbHeight() + 0.3, 0), 6, 0.1);
                }
            });

    public HaloInfiniteChief() {
        super("halo_infinite_chief", "Master Chief", "マスターチーフ", "Halo Infinite");
        eggPrimary = 0x5e6b3c; eggSecondary = VISOR;
        maxHealth = 110; speed = 0.31; armor = 18; knockbackRes = 0.7;
        scale = 1.12f; width = 0.7f; height = 2.1f; preferredRange = 10f; dmgMult = 1.1f;
        idleWeaponR = "ma40";
    }

    // ================= helpers (server) =================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.2, 0.32, 0); }
    /** left wrist (Grappleshot / equipment launcher) */
    static Vec3 lw(SlayerEntity e) { return Combat.muzzle(e, 0.9, -0.35, 0.05); }
    static Vec3 aim(SlayerEntity e, Vec3 from, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        return Combat.dirTo(from, t, spread, new Random(e.getRandom().nextLong()));
    }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) { return aim(e, mz(e), t, spread); }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static Vec3 endOf(SlayerEntity e, LivingEntity t, Vec3 dir, double range) { return t != null ? mid(t) : mz(e).add(dir.scale(range)); }
    static void flash(SlayerEntity e, int rgb) {
        Vec3 m = mz(e);
        Fx.burst(L(e), ParticleTypes.FLAME, m, 2, 0.04, 0.02); Fx.dustBurst(L(e), rgb, 1f, m, 3, 0.06);
    }
    static void casing(SlayerEntity e) { Fx.burst(L(e), ParticleTypes.SMOKE, Combat.muzzle(e, 0.3, 0.55, 0.1), 1, 0.02, 0.03); }
    static void bullet(SlayerEntity e, LivingEntity t, double spread, float dmg, double range) {
        Combat.hitscan(e, mz(e), aim(e, t, spread), range, dmg, 0, TRACER); flash(e, 0xffc060); casing(e);
    }
    static void hold(SlayerEntity e, LivingEntity t, double dist, double lift) {
        if (t == null || !Combat.isFoe(e, t)) return;
        Vec3 f = Vec3.directionFromRotation(0, e.getYRot());
        Vec3 p = e.position().add(f.scale(dist + t.getBbWidth() * 0.5)).add(0, lift, 0);
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true;
    }
    static void zap(ServerLevel l, Vec3 a, Vec3 b, int rgb) {
        Vec3 prev = a; int segs = 8;
        for (int i = 1; i <= segs; i++) {
            Vec3 q = a.lerp(b, i / (double) segs);
            if (i < segs) q = q.add((l.random.nextDouble() - .5) * .6, (l.random.nextDouble() - .5) * .6, (l.random.nextDouble() - .5) * .6);
            Fx.beam(l, prev, q, rgb, 0.7f, 0.25); prev = q;
        }
        Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, b, 10, 0.25, 0.3);
    }
    /** hook cable from wrist to the point (draws the travelling part up to 'frac') */
    static void cable(SlayerEntity e, Vec3 to, double frac) {
        Vec3 a = lw(e), b = a.lerp(to, Math.min(1, frac));
        Fx.beam(L(e), a, b, 0x8a8f90, 0.45f, 0.3);
        Fx.dustBurst(L(e), HOOK, 0.9f, b, 2, 0.05);
    }
    /** hostile (non-friendly) projectiles near a point */
    static List<Projectile> enemyShots(SlayerEntity e, AABB box) {
        Predicate<Projectile> bad = x -> !(x instanceof SlayerProjectile) && !(x.getOwner() instanceof Player) && !(x.getOwner() instanceof SlayerEntity);
        return e.level().getEntitiesOfClass(Projectile.class, box, bad::test);
    }
    static void execute(SlayerEntity e, LivingEntity t) {
        if (t == null) return;
        Combat.damage(e, t, t.getHealth() + 100);
        Fx.sparks(L(e), mid(t), 14); Fx.burst(L(e), ParticleTypes.DAMAGE_INDICATOR, mid(t), 8, 0.3, 0.1);
        snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 0.7f); snd(e, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.8f, 1.2f);
    }

    // ================= passive: Mjolnir energy shield =================
    @Override
    public void passive(SlayerEntity e) {
        CompoundTag d = e.getPersistentData();
        float sh = d.contains("mcSh") ? d.getFloat("mcSh") : SHIELD_MAX;
        float hp = e.getHealth(), last = d.contains("mcHp") ? d.getFloat("mcHp") : hp;
        int delay = d.getInt("mcDelay");
        ServerLevel l = L(e);
        Vec3 c = e.position().add(0, 1.1, 0);
        if (hp < last && e.isAlive()) {
            // shield soaks the hit: give the absorbed part back (we cannot intercept damage directly)
            float absorb = Math.min(sh, last - hp);
            if (absorb > 0) {
                e.setHealth(hp + absorb); hp += absorb; sh -= absorb;
                Fx.dustBurst(l, SHIELD, 1.1f, c, 14, 0.5);
                snd(e, SoundEvents.AMETHYST_BLOCK_HIT, 0.9f, 1.6f);
                if (sh <= 0) {
                    sh = 0; Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, c, 30, 0.6, 0.2);
                    snd(e, SoundEvents.GLASS_BREAK, 1f, 1.4f); snd(e, SoundEvents.BEACON_DEACTIVATE, 1f, 1.6f);
                }
            }
            delay = 90;
        } else if (delay > 0) {
            delay--;
            if (sh <= 0 && e.tickCount % 10 == 0) snd(e, SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f, 1.8f); // low-shield alarm
        } else if (sh < SHIELD_MAX) {
            if (sh == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 1f, 1.3f);
            sh = Math.min(SHIELD_MAX, sh + 0.8f);
            if (e.tickCount % 3 == 0) Fx.ring(l, e.position().add(0, 0.2 + (e.tickCount % 18) * 0.1, 0), 0.55f, SHIELD, 0.8f, 10);
            if (sh >= SHIELD_MAX) snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.8f);
        }
        if (sh > 0 && e.tickCount % 10 == 0) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 15, 0, false, false));
        d.putFloat("mcSh", sh); d.putFloat("mcHp", hp); d.putInt("mcDelay", delay);
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** shouldered rifle: bladed, left shoulder forward, chin tucked behind the optic */
    static void rifle(Pose p, float k, float pitch, float recoil) {
        float tw = 0.22f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.12f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.08f - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.5f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * 0.9f * k; p.headX += (0.08f - recoil * 0.06f) * k;
        p.bodyX += (0.06f - recoil * 0.1f - pitch * 0.2f) * k; p.rootZ += recoil * 0.8f * k;
    }
    /** heavy Banished weapon braced at the hip */
    static void hip(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -1.0f + pitch - recoil * 0.25f, k); p.rArmY = lerp(p.rArmY, -0.2f, k); p.rArmZ = lerp(p.rArmZ, 0.12f, k);
        p.lArmX = lerp(p.lArmX, -1.25f + pitch - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.7f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX = lerp(p.rHandX, -0.55f, k);
        p.bodyX += (0.1f - recoil * 0.12f) * k; p.bodyY += 0.18f * k; p.headY -= 0.18f * k; p.rootZ += recoil * 0.8f * k;
    }
    /** two-handed isosceles pistol grip */
    static void pistol2(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.4f, k); p.rArmY = lerp(p.rArmY, 0.12f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch - recoil * 0.35f, k); p.lArmY = lerp(p.lArmY, 0.42f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX -= recoil * 0.4f; p.headX += 0.1f * k - recoil * 0.05f; p.bodyX -= recoil * 0.05f; p.rootZ += recoil * 0.4f;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.26f, k);
        p.lLegZ -= 0.06f * k; p.rLegZ += 0.08f * k; p.rootY -= 0.4f * k;
    }
    static void wide(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.22f, k); p.rLegX = lerp(p.rLegX, 0.18f, k);
        p.lLegZ -= 0.3f * k; p.rLegZ += 0.3f * k; p.rootY -= 1.2f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // MA40 at low ready, weight on the back foot like an old soldier
        p.rArmX = -0.6f + p.rArmX * 0.25f; p.rArmY = -0.25f;
        p.lArmX = -0.85f + p.lArmX * 0.25f; p.lArmY = 0.62f; p.lArmZ = 0.05f;
        p.rHandX = -0.4f; p.bodyX += 0.05f; p.bodyY += 0.06f; p.headY -= 0.05f;
        p.headX += 0.03f * (float) Math.sin(age * 0.06f) * (1 - m);
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- Rifle bash (MA40 stock swing, right to left) ----------
        out.add(AttackDef.of("rifle_melee").range(0, 3.2f).cd(22).weight(3).dur(16).weapon("ma40")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.8f, 0.8f);
                    if (n == 6) {
                        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.5f, -70, 60, 10, 0xd0d0c0, 0.7f, 10);
                        for (LivingEntity f : Combat.cone(e, 3.3, 60)) {
                            Combat.damage(e, f, 9); Combat.knock(f, e.position(), 1.0, 0.3); Fx.sparks(L(e), mid(f), 6);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.7f); snd(e, SoundEvents.ANVIL_LAND, 0.3f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float swing = track(t, 0, 0, .25f, 1f, .375f, -1f, .6f, -0.8f, 1, 0);
                    p.bodyY = 0.6f * swing; p.headY = -0.35f * swing;
                    p.rArmX = track(t, 0, -0.6f, .25f, -0.9f, .375f, -1.5f, .6f, -1.3f, 1, -0.6f);
                    p.rArmY = track(t, 0, -0.25f, .25f, 0.5f, .375f, -0.6f, .6f, -0.5f, 1, -0.25f);
                    p.rArmZ = track(t, 0, 0, .25f, 0.5f, .375f, -0.2f, 1, 0);
                    p.lArmX = track(t, 0, -0.85f, .25f, -1.2f, .375f, -1.6f, .6f, -1.4f, 1, -0.85f);
                    p.lArmY = track(t, 0, 0.62f, .25f, 0.9f, .375f, 0.1f, 1, 0.62f);
                    p.rHandY = track(t, 0, 0, .25f, -0.8f, .375f, 0.6f, 1, 0); p.rHandX = track(t, 0, -0.4f, .3f, 0.4f, .6f, 0.2f, 1, -0.4f);
                    p.bodyX = track(t, 0, 0, .25f, -0.1f, .4f, 0.3f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, .7f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.35f, .7f, 0.35f, 1, 0);
                    p.rootZ = -1.6f * bump(win(t, .2f, .8f)); p.rootY = -0.5f * bump(win(t, .3f, .9f));
                }));

        // ---------- Assassination (grab, knee to the gut, double-fist hammer to the neck) ----------
        out.add(AttackDef.of("assassination").range(0, 3.5f).lowHealthTarget(0.25f).weight(6).cd(80).dur(34).weapon("none")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.45, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.7f); }
                    hold(e, t, 0.9, 0);
                    if (n == 10 && t != null) {
                        Combat.damage(e, t, Math.max(0, t.getHealth() - 1)); Fx.sparks(L(e), mid(t), 8);
                        snd(e, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 1, 0.8f);
                    }
                    if (n == 22) { execute(e, t); snd(e, SoundEvents.ANVIL_LAND, 0.6f, 0.7f); }
                })
                .anim((p, t, a) -> {
                    // grab: left hand on the shoulder; knee rises; both hands clasped overhead then slammed down
                    p.lArmX = track(t, 0, 0, .12f, -1.6f, .45f, -1.5f, .55f, -2.9f, .65f, -0.9f, .85f, -0.8f, 1, 0);
                    p.rArmX = track(t, 0, 0, .12f, -0.6f, .3f, -0.9f, .55f, -2.9f, .65f, -0.9f, .85f, -0.8f, 1, 0);
                    p.lArmZ = track(t, .45f, 0, .55f, -0.35f, .65f, -0.15f, 1, 0); p.rArmZ = -p.lArmZ;
                    p.lArmY = track(t, 0, 0, .12f, 0.3f, .45f, 0.3f, .55f, 0, 1, 0);
                    p.rLegX = track(t, 0, 0, .18f, 0.4f, .29f, -1.6f, .4f, -0.2f, .5f, 0.3f, 1, 0);
                    p.lLegX = track(t, 0, 0, .12f, -0.3f, .29f, 0.15f, .5f, -0.4f, .85f, -0.4f, 1, 0);
                    p.bodyX = track(t, 0, 0, .12f, 0.2f, .29f, 0.35f, .45f, -0.25f, .55f, -0.3f, .65f, 0.55f, .85f, 0.45f, 1, 0);
                    p.bodyY = track(t, 0, 0, .12f, 0.25f, .3f, -0.1f, .5f, 0, 1, 0);
                    p.headX = track(t, 0, 0, .29f, 0.3f, .5f, -0.2f, .65f, 0.45f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.rootY = track(t, 0, 0, .29f, 0.6f, .4f, 0, .55f, 0.4f, .66f, -1.4f, .85f, -1f, 1, 0);
                    p.rootZ = track(t, 0, 0, .12f, -1.8f, .85f, -1.8f, 1, 0);
                }));

        // ---------- MA40 Assault Rifle (full-auto burst) ----------
        out.add(AttackDef.of("ma40").range(2, 18).cd(34).weight(4).dur(30).weapon("ma40")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 26 && n % 2 == 0) { bullet(e, t, 0.045 + n * 0.0012, 2.2f, 20); snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.7f + e.getRandom().nextFloat() * 0.15f); }
                    if (n == 28) snd(e, SoundEvents.ARMOR_EQUIP_CHAIN, 0.6f, 1.4f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .12f, .14f) * (1 - win(t, .88f, .9f));
                    float r = 0.35f * Math.abs((float) Math.sin(a * PI * 0.5f)) * fire, climb = -0.12f * win(t, .14f, .88f) * fire;
                    rifle(p, k, climb, r); stance(p, k);
                    p.rHandZ += 0.03f * (float) Math.sin(a * 2.7f) * fire; p.bodyZ += 0.02f * (float) Math.sin(a * 1.9f) * fire;
                    p.headY += 0.12f * bump(win(t, .88f, 1)); // checks the corner after the burst
                }));

        // ---------- BR75 Battle Rifle (two 3-round bursts) ----------
        out.add(AttackDef.of("br75").range(5, 28).cd(40).weight(3).dur(34).weapon("br75")
                .tick((e, t, n) -> {
                    if ((n >= 8 && n <= 10) || (n >= 20 && n <= 22)) { bullet(e, t, 0.008, 3.4f, 30); if (n == 8 || n == 20) snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.9f, 1.5f); }
                    if (n == 4) snd(e, SoundEvents.SPYGLASS_USE, 0.8f, 1.2f);
                    if (n == 28) snd(e, SoundEvents.SPYGLASS_STOP_USING, 0.8f, 1.2f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .88f), zoom = win(t, .1f, .25f) * (1 - win(t, .78f, .9f));
                    float r = 1.1f * (kick(t, 8 / 34f, .14f) + kick(t, 20 / 34f, .14f));
                    rifle(p, k, 0, r); stance(p, k);
                    p.headX += 0.15f * zoom; p.headY -= 0.08f * zoom; p.bodyX += 0.08f * zoom; p.rootY -= 0.5f * zoom;
                    p.rHandX -= r * 0.2f; p.bodyZ -= 0.03f * r;
                }));

        // ---------- VK78 Commando (heavy semi-auto) ----------
        out.add(AttackDef.of("vk78_commando").range(4, 24).cd(36).weight(3).dur(30).weapon("vk78")
                .tick((e, t, n) -> {
                    if (n >= 5 && n <= 25 && n % 5 == 0) { bullet(e, t, 0.02, 3.8f, 26); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.9f, 1.2f); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .9f), r = 0;
                    for (int i = 1; i <= 5; i++) r += kick(t, i * 5 / 30f, .12f);
                    rifle(p, k, 0.02f, r * 1.2f); stance(p, k);
                    float sway = (float) Math.sin(t * PI * 5) * k; // rocks between shots
                    p.bodyZ += 0.04f * sway; p.headZ -= 0.03f * sway; p.rHandX -= r * 0.25f;
                }));

        // ---------- MK50 Sidekick (two-handed rapid pistol) ----------
        out.add(AttackDef.of("sidekick").range(2, 16).cd(24).weight(2).dur(22).weapon("sidekick")
                .tick((e, t, n) -> {
                    if (n == 5 || n == 9 || n == 13 || n == 17) { bullet(e, t, 0.025, 2.8f, 18); snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 2f); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .18f, .85f), r = 0;
                    for (int i = 0; i < 4; i++) r += kick(t, (5 + i * 4) / 22f, .12f);
                    pistol2(p, k, 0, r);
                    p.lLegX = lerp(p.lLegX, -0.15f, k); p.rLegX = lerp(p.rLegX, 0.15f, k); p.lLegZ -= 0.12f * k; p.rLegZ += 0.12f * k;
                    p.rootY -= 0.5f * k; p.bodyX += 0.08f * k;
                }));

        // ---------- Mangler (Banished hand cannon: two heavy rounds, thumb-cock between) ----------
        out.add(AttackDef.of("mangler").range(2, 12).cd(48).weight(2).dur(30).weapon("mangler")
                .tick((e, t, n) -> {
                    if (n == 8 || n == 20) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.01), 14, 9f, 0, BANISHED); flash(e, BANISHED);
                        if (t != null) Combat.knock(t, e.position(), 0.6, 0.15);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.7f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.2f, 0.6f);
                    }
                    if (n == 14) snd(e, SoundEvents.CROSSBOW_LOADING_END, 0.8f, 0.7f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .88f), r = 2f * (kick(t, 8 / 30f, .2f) + kick(t, 20 / 30f, .2f));
                    float cock = bump(win(t, .38f, .6f));
                    p.bodyY -= 0.35f * k; p.headY += 0.3f * k;
                    p.rArmX = lerp(p.rArmX, -HALF - r * 0.45f, k); p.rArmY = lerp(p.rArmY, 0.3f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
                    p.rHandX -= r * 0.5f;
                    // left hand braces under the wrist, then reaches over to cock
                    p.lArmX = lerp(p.lArmX, -1.3f - 0.3f * cock - r * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.75f + 0.2f * cock, k);
                    p.bodyX -= r * 0.12f; p.rootZ += r * 0.6f; p.headX -= r * 0.05f;
                    p.lLegX = lerp(p.lLegX, 0.25f, k); p.rLegX = lerp(p.rLegX, -0.25f, k); p.rootY -= 0.4f * k;
                }));

        // ---------- Shock Rifle (charge, chained lightning bolt) ----------
        out.add(AttackDef.of("shock_rifle").range(6, 30).cd(60).weight(2).dur(34).weapon("shock_rifle")
                .tick((e, t, n) -> {
                    if (n < 18) { Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mz(e), 2, 0.15, 0.05); if (n % 4 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.8f, 1.2f + n * 0.04f); }
                    if (n == 19) {
                        Vec3 d = aim(e, t, 0.003), m = mz(e);
                        Combat.hitscan(e, m, d, 32, 14f, 0, ELEC);
                        Vec3 end = endOf(e, t, d, 32); zap(L(e), m, end, ELEC); zap(L(e), m, end, 0xffffff);
                        if (t != null) for (LivingEntity x : Combat.around(e, end, 5)) {
                            if (x == t) continue; zap(L(e), end, mid(x), ELEC); Combat.damage(e, x, 5);
                        }
                        snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.1f, 1.5f); snd(e, SoundEvents.TRIDENT_THUNDER, 0.5f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), ch = win(t, .05f, .55f) * (1 - win(t, .56f, .58f)), r = 1.8f * kick(t, 19 / 34f, .25f);
                    rifle(p, k, 0, r); stance(p, k);
                    float sh = 0.03f * ch * ch;
                    p.bodyZ += sh * (float) Math.sin(a * 5.3f); p.rArmY += sh * (float) Math.cos(a * 6.1f);
                    p.headX += 0.12f * ch; p.rootY -= 0.4f * ch; p.rHandX -= r * 0.35f;
                }));

        // ---------- Skewer (shoulder-fired spike) ----------
        out.add(AttackDef.of("skewer").range(6, 30).cd(90).weight(2).dur(40).weapon("skewer")
                .tick((e, t, n) -> {
                    if (n == 6) snd(e, SoundEvents.CROSSBOW_LOADING_MIDDLE, 1, 0.6f);
                    if (n == 16) {
                        Combat.shoot(e, SKEWER_SPIKE, mz(e), aim(e, t, 0.002), 2.6, 22f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.5, 0.32, 0), 5, 0.15, 0.05); flash(e, BANISHED);
                        e.setDeltaMovement(e.getDeltaMovement().subtract(e.getViewVector(1f).scale(0.25))); e.hurtMarked = true;
                        snd(e, SoundEvents.CROSSBOW_SHOOT, 1.4f, 0.5f); snd(e, SoundEvents.TRIDENT_THROW, 1.2f, 0.6f);
                    }
                    if (n == 32) snd(e, SoundEvents.CROSSBOW_LOADING_END, 1, 0.6f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .92f), r = 2.6f * kick(t, 16 / 40f, .3f);
                    rifle(p, k, -0.04f, r); wide(p, k * 0.6f);
                    p.rHandX -= r * 0.4f; p.bodyX -= r * 0.15f;
                    // reload: left hand reaches over the shoulder for a spike, then rams it in
                    float reach = bump(win(t, .55f, .82f)), ram = bump(win(t, .8f, .88f));
                    p.lArmX -= 1.1f * reach; p.lArmZ -= 0.4f * reach; p.lArmY -= 0.5f * reach; p.headY += 0.25f * reach;
                    p.lArmX += 0.4f * ram; p.bodyZ += 0.05f * ram;
                }));

        // ---------- Hydra (6 tracking micro-rockets) ----------
        out.add(AttackDef.of("hydra").range(5, 26).cd(80).weight(2).dur(36).weapon("hydra")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.PISTON_EXTEND, 0.8f, 1.6f);
                    if (n >= 8 && n <= 23 && (n - 8) % 3 == 0) {
                        Combat.shoot(e, HYDRA_ROCKET, mz(e), aim(e, t, 0.06).add(0, 0.08, 0).normalize(), 1.2, 4.5f);
                        Fx.burst(L(e), ParticleTypes.SMOKE, mz(e), 3, 0.05, 0.02);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.9f, 1.5f);
                    }
                    if (n == 28) snd(e, SoundEvents.PISTON_CONTRACT, 0.8f, 1.6f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .88f), r = 0;
                    for (int i = 0; i < 6; i++) r += kick(t, (8 + i * 3) / 36f, .07f);
                    rifle(p, k, -0.08f, r * 0.8f); stance(p, k);
                    p.rHandY += 0.25f * (float) Math.floor(win(t, .22f, .66f) * 6); // cylinder indexes each shot
                    p.bodyZ += 0.02f * r;
                }));

        // ---------- Ravager (lobbed incendiary plasma + heat vent) ----------
        out.add(AttackDef.of("ravager").range(4, 18).cd(70).weight(2).dur(34).weapon("ravager")
                .tick((e, t, n) -> {
                    if (n == 6 || n == 10 || n == 14) {
                        Vec3 d = aim(e, t, 0.04); double dist = t != null ? e.distanceTo(t) : 10;
                        Combat.shoot(e, RAVAGER_BLOB, mz(e), d.add(0, 0.12 + dist * 0.012, 0).normalize(), 0.9 + dist * 0.02, 5f);
                        flash(e, 0xff6a20); snd(e, SoundEvents.BLAZE_SHOOT, 0.9f, 0.8f);
                    }
                    if (n >= 20 && n <= 26) { Fx.burst(L(e), ParticleTypes.CLOUD, Combat.muzzle(e, 0.6, 0.45, 0.25), 2, 0.05, 0.05); if (n == 20) snd(e, SoundEvents.FIRE_EXTINGUISH, 1, 1.2f); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .9f), r = 0.9f * (kick(t, 6 / 34f, .1f) + kick(t, 10 / 34f, .1f) + kick(t, 14 / 34f, .1f));
                    float vent = bump(win(t, .55f, .8f));
                    hip(p, k, -0.3f * (1 - vent), r); stance(p, k);
                    p.rHandY += 0.6f * vent; p.headY += 0.3f * vent; p.headX += 0.1f * vent; // turns the gun to vent heat
                    p.lArmX += 0.3f * vent;
                }));

        // ---------- Cindershot (bouncing incendiary grenade + lever cycle) ----------
        out.add(AttackDef.of("cindershot").range(5, 22).cd(60).weight(2).dur(30).weapon("cindershot")
                .tick((e, t, n) -> {
                    if (n == 9) {
                        Vec3 d = aim(e, t, 0.01); double dist = t != null ? e.distanceTo(t) : 10;
                        Combat.shoot(e, CINDER, mz(e), d.add(0, 0.05 + dist * 0.01, 0).normalize(), 1.1 + dist * 0.015, 12f);
                        flash(e, BANISHED); snd(e, SoundEvents.FIRECHARGE_USE, 1.1f, 0.9f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.3f, 1.9f);
                    }
                    if (n == 18) snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.7f, 1.4f);
                    if (n == 22) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.7f, 1.5f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 1.6f * kick(t, 9 / 30f, .25f), lever = bump(win(t, .55f, .78f));
                    rifle(p, k, 0, r); stance(p, k);
                    p.rHandX += 0.5f * lever - r * 0.3f; p.lArmX += 0.45f * lever; p.lArmY -= 0.15f * lever;
                    p.headX += 0.1f * lever; p.bodyZ += 0.04f * lever;
                }));

        // ---------- Disruptor (homing electric bolts that slow) ----------
        out.add(AttackDef.of("disruptor").range(3, 18).cd(40).weight(2).dur(26).weapon("disruptor")
                .tick((e, t, n) -> {
                    if (n == 5 || n == 11 || n == 17) {
                        Combat.shoot(e, DISRUPT_BOLT, mz(e), aim(e, t, 0.03), 1.4, 3.5f);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mz(e), 5, 0.08, 0.1);
                        snd(e, SoundEvents.SHULKER_SHOOT, 0.9f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .88f), r = kick(t, 5 / 26f, .12f) + kick(t, 11 / 26f, .12f) + kick(t, 17 / 26f, .12f);
                    // one-handed, body bladed side-on, free hand at the hip
                    p.bodyY -= 0.45f * k; p.headY += 0.42f * k;
                    p.rArmX = lerp(p.rArmX, -HALF - r * 0.3f, k); p.rArmY = lerp(p.rArmY, 0.4f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
                    p.rHandX -= r * 0.25f; p.rHandY += 0.15f * r;
                    p.lArmX = lerp(p.lArmX, 0.25f, k); p.lArmZ = lerp(p.lArmZ, -0.25f, k);
                    p.lLegX = lerp(p.lLegX, 0.3f, k); p.rLegX = lerp(p.rLegX, -0.3f, k); p.rootY -= 0.3f * k; p.rootZ += r * 0.3f;
                }));

        // ---------- Energy Sword (lunge, forehand, backhand) ----------
        out.add(AttackDef.of("energy_sword").range(0, 9).cd(70).weight(2).dur(30).weapon("energy_sword")
                .tick((e, t, n) -> {
                    if (n == 1) snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 2f);
                    if (n == 6 && t != null && e.distanceTo(t) > 2.5) {
                        Vec3 d = t.position().subtract(e.position()); double len = d.length();
                        e.setDeltaMovement(d.x / len * Math.min(1.4, len * 0.3), 0.15, d.z / len * Math.min(1.4, len * 0.3)); e.hurtMarked = true;
                        snd(e, SoundEvents.ELYTRA_FLYING, 0.5f, 2f);
                    }
                    if (n == 12 || n == 20) {
                        boolean fore = n == 12;
                        Fx.arc(L(e), e.position().add(0, 1.2, 0), e.getYRot(), 2f, fore ? -80 : 80, fore ? 80 : -80, fore ? 5 : -30, 0x6fd8ff, 1.1f, 16);
                        for (LivingEntity f : Combat.cone(e, 3.6, 75)) {
                            Combat.damage(e, f, fore ? 14 : 10); Combat.knock(f, e.position(), 0.6, 0.2);
                            Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(f), 10, 0.2, 0.2);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f); snd(e, SoundEvents.BLAZE_SHOOT, 0.5f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    // anticipation: blade drawn back low-right; lunge; forehand sweep at .4; backhand rising at .67
                    float sw = trackLin(t, 0, 0, .2f, 1f, .32f, 1f, .4f, -1f, .5f, -0.9f, .6f, -1f, .67f, 0.8f, .8f, 0.6f, 1, 0);
                    p.bodyY = 0.65f * sw; p.headY = -0.4f * sw;
                    p.rArmX = track(t, 0, -0.4f, .2f, -0.6f, .4f, -1.6f, .6f, -1.3f, .67f, -2.0f, .85f, -1.4f, 1, -0.3f);
                    p.rArmY = 0.7f * sw; p.rArmZ = track(t, 0, 0, .2f, 0.8f, .4f, -0.2f, .6f, -0.3f, .67f, 0.6f, 1, 0);
                    p.rHandX = track(t, 0, 0, .2f, 0.6f, .4f, 0.2f, .67f, -0.2f, 1, 0); p.rHandY = -0.5f * sw;
                    p.lArmX = track(t, 0, 0, .2f, -1.0f, .4f, -0.3f, .67f, -0.9f, 1, 0); p.lArmZ = track(t, 0, 0, .2f, -0.6f, .4f, -0.3f, 1, 0);
                    p.bodyX = track(t, 0, 0, .2f, 0.3f, .32f, 0.45f, .45f, 0.25f, .7f, 0.15f, 1, 0);
                    p.lLegX = track(t, 0, 0, .2f, -0.7f, .4f, -0.6f, .8f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .2f, 0.6f, .4f, 0.4f, 1, 0);
                    p.rootY = track(t, 0, 0, .2f, -1.2f, .3f, 0.4f, .4f, -0.8f, .8f, -0.6f, 1, 0);
                    p.rootZ = -2f * bump(win(t, .2f, .9f));
                }));

        // ---------- Gravity Hammer (overhead smash + gravity shockwave) ----------
        out.add(AttackDef.of("gravity_hammer").range(0, 4.5f).cd(80).weight(2).dur(32).weapon("gravity_hammer")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.ARMOR_EQUIP_NETHERITE, 1, 0.6f);
                    if (n == 16) {
                        Vec3 c = e.position().add(e.getViewVector(1f).multiply(1, 0, 1).normalize().scale(1.6));
                        for (LivingEntity x : Combat.around(e, c.add(0, 0.5, 0), 4.5)) { Combat.damage(e, x, 16); Combat.knock(x, c, 1.6, 0.7); }
                        Fx.explosion(L(e), c, 2f);
                        for (int i = 1; i <= 3; i++) Fx.ring(L(e), c.add(0, 0.15, 0), i * 1.4f, 0xb070ff, 1.4f, 14 + i * 8);
                        Fx.burst(L(e), ParticleTypes.PORTAL, c, 40, 1.2, 0.6);
                        snd(e, SoundEvents.ANVIL_LAND, 1.2f, 0.5f); snd(e, SoundEvents.GENERIC_EXPLODE, 1f, 0.8f); snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.4f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    // two-handed: haul the hammer overhead, rise on the toes, crash down into a deep crouch
                    float armX = track(t, 0, -0.6f, .3f, -3.0f, .45f, -3.1f, .5f, -0.7f, .7f, -0.6f, 1, -0.5f);
                    p.rArmX = armX; p.lArmX = armX + 0.15f;
                    p.rArmY = -0.2f; p.lArmY = 0.55f; p.lArmZ = track(t, .3f, 0, .5f, 0.15f, 1, 0);
                    p.rHandX = track(t, 0, -0.3f, .3f, 0.4f, .5f, 0.3f, 1, -0.2f);
                    p.bodyX = track(t, 0, 0, .3f, -0.35f, .45f, -0.35f, .5f, 0.6f, .7f, 0.5f, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, 0.2f, .5f, -0.1f, 1, 0);
                    p.headX = track(t, 0, 0, .3f, -0.3f, .5f, 0.4f, .8f, 0.3f, 1, 0);
                    p.rootY = track(t, 0, 0, .3f, 0.8f, .45f, 1.2f, .5f, -1.8f, .72f, -1.6f, 1, 0);
                    p.rootZ = track(t, .4f, 0, .5f, -1.2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, 0.1f, .5f, -0.65f, .8f, -0.6f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.1f, .5f, 0.5f, .8f, 0.45f, 1, 0);
                    p.lLegZ = -0.15f * win(t, .45f, .5f) * (1 - win(t, .8f, 1)); p.rLegZ = -p.lLegZ;
                }));

        // ---------- Grappleshot: hook & yank (pull the foe in, then stock-bash it) ----------
        out.add(AttackDef.of("grappleshot_pull").range(4, 18).cd(90).weight(2).dur(28).weapons("ma40", "hook")
                .when((e, t) -> t.getMaxHealth() <= 40)
                .tick((e, t, n) -> {
                    if (n == 5) snd(e, SoundEvents.FISHING_BOBBER_THROW, 1.2f, 0.6f);
                    if (t == null) return;
                    if (n >= 5 && n <= 16) cable(e, mid(t), (n - 4) / 4.0);
                    if (n == 9) { snd(e, SoundEvents.CHAIN_PLACE, 1.2f, 0.8f); Fx.sparks(L(e), mid(t), 6); }
                    if (n >= 10 && n <= 16) {
                        Vec3 to = e.position().add(Vec3.directionFromRotation(0, e.getYRot()).scale(1.6));
                        Vec3 d = to.subtract(t.position()); double len = d.length();
                        if (len > 0.6 && Combat.isFoe(e, t)) {
                            if (t instanceof Mob m) m.getNavigation().stop();
                            Vec3 v = d.scale(Math.min(1.3, len * 0.45) / len); t.setDeltaMovement(v.x, Math.max(0.12, v.y), v.z); t.hurtMarked = true;
                        }
                        if (n == 10) snd(e, SoundEvents.FISHING_BOBBER_RETRIEVE, 1.2f, 0.7f);
                    }
                    if (n == 19 && e.distanceTo(t) < 3.5) {
                        Combat.damage(e, t, 8); Combat.knock(t, e.position(), 1.1, 0.3); Fx.sparks(L(e), mid(t), 8);
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .6f ? "hook" : null;
                    // left wrist aimed, right holds rifle low; latch -> violent yank back with a twist -> rifle bash
                    p.lArmX = track(t, 0, -0.8f, .15f, -1.65f, .32f, -1.6f, .4f, -0.7f, .55f, -0.4f, .7f, -1.2f, 1, -0.85f);
                    p.lArmY = track(t, 0, 0.6f, .15f, 0.15f, .32f, 0.15f, .5f, 0.4f, 1, 0.62f);
                    p.lArmZ = track(t, .32f, 0, .45f, -0.4f, .6f, 0, 1, 0);
                    p.rArmX = track(t, 0, -0.6f, .55f, -0.7f, .62f, -0.5f, .68f, -1.5f, .85f, -1.2f, 1, -0.6f);
                    p.rArmY = track(t, 0, -0.25f, .62f, 0.3f, .68f, -0.5f, 1, -0.25f);
                    p.rHandX = track(t, .55f, -0.4f, .68f, 0.3f, 1, -0.4f);
                    p.bodyY = track(t, 0, 0, .15f, -0.3f, .32f, -0.3f, .45f, 0.45f, .62f, 0.5f, .68f, -0.5f, .85f, -0.3f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyX = track(t, 0, 0, .32f, 0.05f, .45f, -0.3f, .62f, -0.1f, .7f, 0.3f, 1, 0);
                    p.rootZ = track(t, .3f, 0, .45f, 1.2f, .62f, 0.5f, .7f, -1.5f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.3f, .45f, -0.6f, .7f, -0.4f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.3f, .45f, 0.5f, .7f, 0.4f, 1, 0);
                    p.rootY = -0.6f * bump(win(t, .35f, .8f));
                }));

        // ---------- Grappleshot: zip-line strike (fly to the foe, shock-charged hook punch) ----------
        out.add(AttackDef.of("grappleshot_strike").range(5, 20).cd(110).weight(2).dur(28).weapons("ma40", "hook")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.FISHING_BOBBER_THROW, 1.2f, 0.5f);
                    if (t == null) return;
                    if (n >= 4 && n < 20) cable(e, mid(t), (n - 3) / 4.0);
                    if (n == 8) { snd(e, SoundEvents.CHAIN_PLACE, 1.2f, 0.7f); snd(e, SoundEvents.FISHING_BOBBER_RETRIEVE, 1.2f, 0.6f); }
                    boolean close = e.distanceTo(t) < 2.4;
                    if (n >= 8 && n < 18 && !close) {
                        Vec3 d = mid(t).subtract(e.position()); double len = d.length();
                        Vec3 v = d.scale(Math.min(1.5, len * 0.4) / len); e.setDeltaMovement(v.x, Math.max(0.05, v.y * 0.6 + 0.08), v.z); e.hurtMarked = true;
                        e.fallDistance = 0;
                        if (n % 2 == 0) Fx.burst(L(e), ParticleTypes.CLOUD, e.position().add(0, 1, 0), 2, 0.2, 0.01);
                    }
                    if (n == 18) {
                        if (e.distanceTo(t) < 3.6) {
                            Combat.damage(e, t, 14); Combat.knock(t, e.position(), 1.6, 0.45);
                            zap(L(e), lw(e), mid(t), HOOK); t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                            snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.3f, 0.6f); snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5f, 2f);
                        }
                        e.setDeltaMovement(e.getDeltaMovement().scale(0.2)); e.hurtMarked = true;
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .55f ? "hook" : null;
                    float fly = win(t, .28f, .36f) * (1 - win(t, .6f, .66f));
                    // fire from the wrist, get yanked forward (body flat, legs trailing), wind up and crack a left hook
                    p.lArmX = track(t, 0, -0.8f, .14f, -1.7f, .5f, -1.7f, .58f, -0.6f, .64f, -1.75f, .8f, -1.6f, 1, -0.85f);
                    p.lArmY = track(t, 0, 0.6f, .14f, 0.1f, .5f, 0.1f, .58f, 0.6f, .64f, -0.1f, 1, 0.62f);
                    p.lArmZ = track(t, .5f, 0, .58f, -0.6f, .64f, 0.1f, 1, 0);
                    p.rArmX = lerp(-0.6f, 0.4f, fly); p.rArmY = -0.25f; p.rHandX = -0.4f;
                    p.bodyY = track(t, 0, 0, .14f, -0.3f, .5f, -0.3f, .58f, 0.6f, .64f, -0.55f, .8f, -0.4f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.bodyX = 0.5f * fly + track(t, .58f, 0, .64f, 0.3f, 1, 0); p.headX = -0.4f * fly;
                    p.rootPitch = 0.35f * fly;
                    p.rLegX = lerp(p.rLegX, 0.7f, fly); p.lLegX = lerp(p.lLegX, 0.2f, fly) + track(t, .58f, 0, .64f, -0.6f, .85f, -0.5f, 1, 0);
                    p.rootY = 1.2f * fly - 0.7f * bump(win(t, .64f, .95f));
                }));

        // ---------- Thruster (lateral boost dodge, rifle stays on target) ----------
        out.add(AttackDef.of("thruster").range(0, 7).cd(70).weight(2).dur(14).weapon("ma40")
                .tick((e, t, n) -> {
                    if (n == 2) {
                        double y = Math.toRadians(e.getYRot());
                        Vec3 left = new Vec3(Math.cos(y), 0, Math.sin(y)); // facing's left
                        e.setDeltaMovement(left.x * 1.15, 0.12, left.z * 1.15); e.hurtMarked = true;
                        Vec3 back = e.position().add(0, 1.2, 0).subtract(left.scale(0.5));
                        Fx.burst(L(e), ParticleTypes.CLOUD, back, 12, 0.2, 0.05); Fx.burst(L(e), ParticleTypes.FLAME, back, 6, 0.1, 0.02);
                        snd(e, SoundEvents.FIRECHARGE_USE, 0.8f, 1.6f); snd(e, SoundEvents.ELYTRA_FLYING, 0.4f, 2f);
                    }
                    if (n > 2 && n < 8) Fx.burst(L(e), ParticleTypes.CLOUD, e.position().add(0, 1.2, 0), 1, 0.1, 0.01);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .85f), boost = bump(win(t, .1f, .7f));
                    rifle(p, k, 0, 0);
                    p.rootRoll = 0.35f * boost; p.bodyZ += 0.15f * boost; p.headZ -= 0.2f * boost; // lean into the boost
                    p.lLegZ = -0.55f * boost; p.rLegZ = 0.15f * boost; p.rLegX = 0.3f * boost; p.lLegX = -0.2f * boost;
                    p.rootY = 0.8f * boost - 0.6f * bump(win(t, .7f, 1));
                }));

        // ---------- Repulsor (palm shockwave: knocks foes away, sends shots back) ----------
        out.add(AttackDef.of("repulsor").range(0, 5).cd(140).weight(2).dur(22).weapon("ma40")
                .tick((e, t, n) -> {
                    if (n < 7) Fx.dustBurst(L(e), ELEC, 0.9f, lw(e), 3, 0.15 - n * 0.015);
                    if (n == 2) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.8f);
                    if (n == 8) {
                        Vec3 c = lw(e), f = e.getViewVector(1f);
                        for (LivingEntity x : Combat.cone(e, 6, 75)) { Combat.damage(e, x, 3); Combat.knock(x, e.position(), 2.2, 0.5); }
                        for (Projectile pr : enemyShots(e, e.getBoundingBox().inflate(6))) {
                            Vec3 v = pr.getDeltaMovement(); Vec3 back = v.lengthSqr() < 1e-4 ? f : v.scale(-1).normalize();
                            Combat.shoot(e, REFLECT, pr.position(), back, Math.max(1.2, v.length() * 1.5), 6f);
                            Fx.sparks(L(e), pr.position(), 6); pr.discard();
                        }
                        for (int i = 1; i <= 3; i++) Fx.ring(L(e), c.add(f.scale(i * 0.8)), 0.5f + i * 0.5f, ELEC, 1.2f, 12 + i * 6);
                        Fx.burst(L(e), ParticleTypes.CLOUD, c.add(f.scale(1.5)), 15, 0.6, 0.15);
                        snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.9f); snd(e, SoundEvents.ENDER_DRAGON_FLAP, 1f, 1.5f);
                    }
                })
                .anim((p, t, a) -> {
                    // rifle swung aside on the right, left palm drawn back then thrust out; body braces against the push
                    p.rArmX = -0.5f; p.rArmY = -0.4f; p.rArmZ = 0.3f; p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, -0.85f, .3f, -0.9f, .36f, -1.75f, .7f, -1.6f, 1, -0.85f);
                    p.lArmY = track(t, 0, 0.62f, .3f, -0.4f, .36f, 0.15f, 1, 0.62f);
                    p.lArmZ = track(t, 0, 0, .3f, 0.3f, .36f, 0, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, 0.5f, .36f, -0.35f, .7f, -0.25f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyX = track(t, 0, 0, .3f, -0.1f, .36f, 0.25f, .5f, -0.05f, 1, 0);
                    p.rootZ = track(t, 0, 0, .3f, 0.4f, .36f, -0.8f, .5f, 1.2f, 1, 0); // shoved back by the recoil
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, .8f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.4f, .8f, 0.4f, 1, 0);
                    p.rootY = -0.6f * bump(win(t, .25f, .9f));
                }));

        // ---------- Threat Sensor (wrist-launched dart, reveals foes) ----------
        out.add(AttackDef.of("threat_sensor").range(6, 30).cd(300).weight(1).dur(18).weapons("ma40", "sensor")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.6f);
                    if (n == 8) {
                        Vec3 d = aim(e, lw(e), t, 0.01).add(0, 0.12, 0).normalize();
                        Combat.shoot(e, SENSOR, lw(e), d, 1.4, 1f);
                        snd(e, SoundEvents.CROSSBOW_SHOOT, 0.8f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .45f ? "sensor" : null;
                    p.rArmX = -0.6f; p.rArmY = -0.3f; p.rHandX = -0.4f;
                    p.lArmX = track(t, 0, -0.85f, .3f, -1.95f, .44f, -2.0f, .5f, -1.7f, .8f, -1.75f, 1, -0.85f);
                    p.lArmY = track(t, 0, 0.62f, .3f, 0.2f, 1, 0.62f);
                    p.bodyY = track(t, 0, 0, .3f, -0.35f, .8f, -0.35f, 1, 0); p.headY = -p.bodyY * 0.8f; p.headX = -0.15f * ready(t, .3f, .8f);
                    p.bodyX = -0.08f * ready(t, .3f, .8f) + 0.05f * kick(t, .44f, .2f);
                    p.lLegX = -0.3f * ready(t, .2f, .85f); p.rLegX = 0.25f * ready(t, .2f, .85f);
                }));

        // ---------- Drop Wall (throw the emitter, fight from behind the barrier) ----------
        out.add(AttackDef.of("drop_wall").range(3, 24).cd(360).weight(3).dur(60).weapons("ma40", "wall_emitter")
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.7f)
                .tick((e, t, n) -> {
                    if (n == 6) snd(e, SoundEvents.SNOWBALL_THROW, 1, 0.7f);
                    if (n == 9) snd(e, SoundEvents.BEACON_ACTIVATE, 1.2f, 1.4f);
                    if (n < 9) return;
                    double y = Math.toRadians(e.getYRot());
                    Vec3 fwd = new Vec3(-Math.sin(y), 0, Math.cos(y)), side = new Vec3(Math.cos(y), 0, Math.sin(y));
                    Vec3 c = e.position().add(fwd.scale(1.8));
                    if (n % 2 == 0) for (int i = -3; i <= 3; i++) for (int j = 0; j < 4; j++) {
                        Vec3 q = c.add(side.scale(i * 0.5)).add(0, 0.2 + j * 0.55, 0);
                        Fx.dustBurst(L(e), j == 3 ? 0xa0f0ff : 0x40b0ff, 0.9f, q, 1, 0.05);
                    }
                    for (Projectile pr : enemyShots(e, new AABB(c, c).inflate(2.2, 2.4, 2.2).move(0, 1, 0))) {
                        Fx.sparks(L(e), pr.position(), 8); Fx.dustBurst(L(e), 0xa0f0ff, 1.2f, pr.position(), 8, 0.2);
                        snd(e, SoundEvents.SHIELD_BLOCK, 0.8f, 1.4f); pr.discard();
                    }
                    if (n % 10 == 0) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 14, 1, false, false));
                    if (n >= 20 && n <= 52 && n % 3 == 0 && t != null) { bullet(e, t, 0.05, 2.2f, 20); snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.8f); }
                    if (n == 58) snd(e, SoundEvents.BEACON_DEACTIVATE, 1f, 1.4f);
                })
                .anim((p, t, a) -> {
                    float toss = win(t, 0, .2f);
                    p.weaponL = t < .1f ? "wall_emitter" : null;
                    if (t < .25f) {
                        // underhand toss of the emitter
                        p.rArmX = -0.6f; p.rArmY = -0.3f; p.rHandX = -0.4f;
                        p.lArmX = trackLin(toss, 0, -0.5f, .5f, 0.7f, 1, -1.3f); p.lArmZ = -0.15f;
                        p.bodyX = 0.35f * bump(toss); p.bodyY = -0.25f * bump(toss); p.rootY = -1f * bump(toss);
                        p.lLegX = -0.45f * bump(toss); p.rLegX = 0.3f * bump(toss);
                    } else {
                        // crouched behind the wall, popping up to fire over it
                        float k = win(t, .25f, .33f) * (1 - win(t, .9f, 1)), fire = win(t, .33f, .36f) * (1 - win(t, .86f, .88f));
                        float r = 0.35f * Math.abs((float) Math.sin(a * PI / 3)) * fire;
                        rifle(p, k, 0.05f, r); wide(p, k);
                        p.rootY -= 0.8f * k * (1 - fire * 0.6f); p.bodyX += 0.15f * k;
                    }
                }));

        // ---------- Shroud Screen (smoke dome, foes lose track of him) ----------
        out.add(AttackDef.of("shroud_screen").range(0, 12).cd(400).weight(3).dur(24).weapons("ma40", "shroud")
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.5f)
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.2f);
                    if (n == 8) {
                        Vec3 c = e.position().add(0, 0.2, 0);
                        Fx.burst(L(e), ParticleTypes.CAMPFIRE_COSY_SMOKE, c.add(0, 1, 0), 60, 1.8, 0.02);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, c.add(0, 1, 0), 60, 2.2, 0.03);
                        for (int i = 1; i <= 4; i++) Fx.ring(L(e), c.add(0, i * 0.5, 0), 4.5f - i * 0.6f, 0x8a9aa8, 2f, 30);
                        snd(e, SoundEvents.FIRE_EXTINGUISH, 1.5f, 0.5f); snd(e, SoundEvents.SPLASH_POTION_BREAK, 1f, 0.6f);
                        e.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false));
                        for (Mob m : e.level().getEntitiesOfClass(Mob.class, e.getBoundingBox().inflate(18), x -> x.getTarget() == e)) m.setTarget(null);
                    }
                    if (n > 8 && n % 4 == 0) Fx.burst(L(e), ParticleTypes.CAMPFIRE_COSY_SMOKE, e.position().add(0, 1, 0), 10, 1.5, 0.01);
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .33f ? "shroud" : null;
                    // drop to a knee, slam the device into the ground, rise and slip back
                    float down = track(t, 0, 0, .25f, 1f, .45f, 1f, .7f, 0, 1, 0);
                    p.lArmX = track(t, 0, -0.85f, .2f, -1.4f, .33f, -0.4f, .5f, -0.3f, .75f, -0.85f, 1, -0.85f);
                    p.lArmZ = -0.2f * down;
                    p.rArmX = -0.6f + 0.2f * down; p.rArmY = -0.3f; p.rHandX = -0.4f;
                    p.bodyX = 0.55f * down; p.headX = -0.2f * down + 0.15f * bump(win(t, .5f, 1));
                    p.headY = 0.4f * (float) Math.sin(win(t, .45f, 1) * PI * 2) * (1 - win(t, .8f, 1)); // scans left/right in the smoke
                    p.rootY = -3.5f * down;
                    p.rLegX = 1.4f * down; p.lLegX = -1.1f * down;
                    p.rootZ = 1.4f * win(t, .7f, 1) * (1 - win(t, .95f, 1));
                }));

        // ---------- Frag Grenade (overhand throw) ----------
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(140).weight(2).dur(20).weapons("ma40", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.5f);
                    if (n == 10) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.4);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, from, t, 0.02).add(0, 0.15 + dist * 0.018, 0).normalize();
                        Combat.shoot(e, FRAG, from, d, 0.75 + dist * 0.025, 14f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .52f ? "frag" : null;
                    p.rArmX = -0.7f; p.rArmY = -0.3f; p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, -0.85f, .15f, -0.8f, .42f, 0.9f, .52f, -2.4f, .7f, -1.4f, 1, -0.85f);
                    p.lArmZ = track(t, 0, 0, .42f, -0.5f, .52f, -0.1f, 1, 0);
                    p.bodyY = track(t, 0, 0, .42f, -0.55f, .55f, 0.45f, .75f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .42f, -0.2f, .55f, 0.3f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.35f, .55f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .42f, 0.3f, .55f, 0.45f, 1, 0);
                    p.rootZ = track(t, .4f, 0, .55f, -1.4f, 1, 0); p.rootY = -0.5f * bump(win(t, .4f, .8f));
                }));

        // ---------- Plasma Grenade (low sidearm whip, sticks) ----------
        out.add(AttackDef.of("plasma_grenade").range(4, 16).cd(160).weight(2).dur(20).weapons("ma40", "plasma_nade")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.6f, 2f);
                    if (n == 9) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.5, 0.1);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, from, t, 0.02).add(0, 0.1 + dist * 0.014, 0).normalize();
                        Combat.shoot(e, PLASMA_NADE, from, d, 0.8 + dist * 0.025, 15f);
                        snd(e, SoundEvents.SNOWBALL_THROW, 1, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .47f ? "plasma_nade" : null;
                    p.rArmX = -0.75f; p.rArmY = -0.35f; p.rHandX = -0.35f;
                    p.lArmX = track(t, 0, -0.85f, .3f, -0.6f, .47f, -1.5f, .7f, -1.2f, 1, -0.85f);
                    p.lArmZ = track(t, 0, 0, .3f, -1.1f, .47f, 0.4f, .7f, 0.3f, 1, 0);
                    p.lArmY = track(t, 0, 0.6f, .3f, -0.6f, .47f, 0.8f, 1, 0.6f);
                    p.bodyY = track(t, 0, 0, .3f, -0.65f, .5f, 0.5f, .75f, 0.35f, 1, 0); p.headY = -p.bodyY * 0.8f;
                    p.bodyZ = track(t, 0, 0, .3f, 0.12f, .5f, -0.05f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, .7f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.35f, .7f, 0.35f, 1, 0);
                    p.rootY = -0.8f * bump(win(t, .2f, .8f)); p.rootZ = -0.8f * bump(win(t, .35f, .9f));
                }));
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        // weathered Mark VII: olive plates with scratches and chipped edges
        Mat olive = Mat.of(0x5e6b3c).noise(.16f).bevel(.28f).decal((m, g, x, y, w, h, f) -> {
            for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
                int hsh = ((x + i) * 73856093) ^ ((y + j) * 19349663) ^ 0x5bd1e995; hsh ^= hsh >>> 13;
                if ((hsh & 31) == 0) g.px(x + i, y + j, 0x9a9a88);      // bare-metal scratch
                else if ((hsh & 63) == 1) g.px(x + i, y + j, 0x3a4024); // grime
            }
        });
        Mat suit = Mat.of(0x2b2e30).noise(.1f), dark = Mat.of(0x3c4530).noise(.12f).bevel(.22f), metal = Mat.of(0x6d6f6a).noise(.08f);
        Mat visor = Mat.of(VISOR).glow().decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.FRONT) { g.rectGlow(x, y, w, 1, 0xffe0a0); g.rectGlow(x + w - 2, y + 1, 1, 1, 0xfff4d0); }
        });
        Mat chest = Mat.of(0x5e6b3c).noise(.14f).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            g.rect(x + w / 2 - 1, y + 1, 2, h - 2, 0x4a5530); // sternum ridge
            g.px(x + 1, y + h - 2, 0x9a9a88); g.px(x + w - 3, y + 2, 0x9a9a88); g.px(x + w - 2, y + 3, 0x9a9a88); // battle damage
        });
        Mat helm = Mat.of(0x5e6b3c).noise(.15f).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) { g.rect(x + 1, y + h / 2, w - 2, 1, 0x3a4024); g.px(x + 2, y + 2, 0x9a9a88); }
            if (f == Mat.Face.TOP) g.rect(x + w / 2, y + 1, 1, h - 2, 0x3a4024);
        });
        Mat vent = Mat.of(0x6fd0ff).glow(), grap = Mat.of(0x474c44).bevel(.25f), hookGlow = Mat.of(HOOK).glow();

        // torso
        rig.box(rig.torso, suit, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4.5f, -12.2f, -3.2f, 9, 6, 2, .2f);
        rig.box(rig.torso, dark, -3, -6, -2.6f, 6, 3, 1);
        rig.box(rig.torso, suit, -4.5f, -2.6f, -2.5f, 9, 2, 5, .1f);
        rig.box(rig.torso, metal, -1.5f, -2.8f, -3.1f, 3, 2, 1);
        rig.box(rig.torso, dark, -5.2f, -2.3f, -1.5f, 2, 3, 3);
        rig.box(rig.torso, dark, 3.2f, -2.3f, -1.5f, 2, 3, 3);
        rig.box(rig.torso, dark, -4, -13, -2.5f, 8, 1, 5);
        rig.box(rig.torso, olive, -3.5f, -11.5f, 2, 7, 7, 3);
        rig.box(rig.torso, vent, -2.5f, -6, 4.3f, 1, 2, 1);
        rig.box(rig.torso, vent, 1.5f, -6, 4.3f, 1, 2, 1);
        rig.box(rig.torso, dark, -1.5f, -0.6f, -2.8f, 3, 2, 1);
        // helmet (Mark VII: rounded dome, wide gold visor, ear modules, chin guard)
        rig.box(rig.head, helm, -4, -8, -4, 8, 8, 8, .4f);
        rig.box(rig.head, visor, -3.2f, -6, -4.9f, 6, 3, 1, .1f);
        rig.box(rig.head, dark, -2, -2.6f, -4.9f, 4, 2, 1);
        rig.box(rig.head, metal, -4.9f, -5.5f, -1.5f, 1, 3, 3);
        rig.box(rig.head, metal, 3.9f, -5.5f, -1.5f, 1, 3, 3);
        rig.box(rig.head, olive, -1, -8.9f, -3, 2, 1, 6);
        rig.box(rig.head, dark, -3, -2, 3.8f, 6, 2, 1);
        // arms
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, suit, -2, -2, -2, 4, 12, 4);
            rig.box(arm, olive, s < 0 ? -3.6f : -2.4f, -3.2f, -3, 6, 4, 6);
            rig.box(arm, dark, -2, 1.5f, -2, 4, 2, 4, .2f);
            rig.box(arm, olive, -2, 4.5f, -2, 4, 5, 4, .4f);
            rig.box(arm, suit, -2, 9.2f, -2, 4, 2, 4, .15f);
        }
        // Grappleshot module on the left forearm
        rig.box(rig.lArm, grap, 2.1f, 4.5f, -2, 2, 2, 4);
        rig.box(rig.lArm, hookGlow, 2.6f, 5, -2.6f, 1, 1, 1);
        // legs
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, suit, -2, 0, -2, 4, 12, 4);
            rig.box(leg, olive, -2, 1, -2.8f, 4, 5, 1, .2f);
            rig.box(leg, metal, -1.5f, 5.6f, -3, 3, 2, 1);
            rig.box(leg, olive, -2, 7, -2.4f, 4, 3, 5, .3f);
            rig.box(leg, dark, -2, 10, -3, 4, 2, 6, .1f);
        }
        buildWeapons(rig, metal);
    }

    private static void buildWeapons(Rig rig, Mat metal) {
        Mat unsc = Mat.of(0x4f574a).noise(.08f), unscD = Mat.of(0x34383a).noise(.06f), black = Mat.of(0x1c1d1e), tan = Mat.of(0x7c7560).noise(.08f);
        Mat ban = Mat.of(0x4b3a46).noise(.1f), banP = Mat.of(0x4a3c55).noise(.1f), rust = Mat.of(0x8a5a30).noise(.12f);
        Mat ammo = Mat.of(0x60c8ff).glow(), orange = Mat.of(BANISHED).glow(), cyan = Mat.of(ELEC).glow(), red = Mat.of(0xff3030).glow();
        Mat sword = Mat.of(0x6fd8ff).glow(), purple = Mat.of(0xb070ff).glow(), plasma = Mat.of(0x40a0ff).glow();
        PartDefinition w;
        w = rig.weapon(rig.rHand, "ma40");
        rig.box(w, unsc, -1.5f, -3, -7, 3, 4, 9); rig.box(w, unscD, -1, -2.5f, -12, 2, 2, 5); rig.box(w, ammo, -1, -3.6f, -2, 2, 1, 1);
        rig.box(w, black, -1, 1, -6, 2, 4, 2); rig.box(w, unscD, -1, -2.5f, 2, 2, 3, 3);
        w = rig.weapon(rig.rHand, "br75");
        rig.box(w, unscD, -1, -2.5f, -11, 2, 3, 13); rig.box(w, black, -1, -4.5f, -6, 2, 2, 4); rig.box(w, black, -1, 0.5f, -5, 2, 3, 2);
        rig.box(w, tan, -1, -3.3f, -10, 2, 1, 3); rig.box(w, unscD, -1, -2, 2, 2, 3, 3);
        w = rig.weapon(rig.rHand, "vk78");
        rig.box(w, tan, -1.5f, -3, -8, 3, 4, 10); rig.box(w, metal, -0.5f, -2, -13, 1, 1, 5); rig.box(w, black, -0.5f, -4, -7, 1, 1, 6);
        rig.box(w, black, -1, 1, -3, 2, 4, 2);
        w = rig.weapon(rig.rHand, "sidekick");
        rig.box(w, unscD, -1, -2.2f, -6, 2, 2, 6); rig.box(w, black, -1, -0.4f, -5, 2, 1, 4); rig.box(w, black, -1, 0, -1, 2, 3, 2);
        w = rig.weapon(rig.rHand, "mangler");
        rig.box(w, ban, -1.5f, -3, -7, 3, 3, 7); rig.box(w, metal, -1, -2.5f, -11, 2, 2, 4); rig.box(w, rust, -0.5f, -6, -8, 1, 3, 4);
        rig.box(w, orange, 1.5f, -2, -6, 1, 1, 4); rig.box(w, black, -1, 0, -1, 2, 3, 2);
        w = rig.weapon(rig.rHand, "shock_rifle");
        rig.box(w, ban, -1, -2.5f, -12, 2, 3, 14); rig.box(w, cyan, -1.5f, -3, -9, 3, 3, 1); rig.box(w, cyan, -1.5f, -3, -6, 3, 3, 1);
        rig.box(w, rust, -1, -2, 2, 2, 3, 3); rig.box(w, metal, -0.5f, -2, -15, 1, 1, 3);
        w = rig.weapon(rig.rHand, "skewer");
        rig.box(w, ban, -1.5f, -3.5f, -10, 3, 4, 14); rig.box(w, metal, -0.5f, -2, -16, 1, 1, 6); rig.box(w, orange, -1, -5.5f, -4, 2, 2, 3);
        rig.box(w, black, -0.5f, 0.5f, -1, 1, 3, 2); rig.box(w, rust, -2, -4, 4, 4, 5, 2);
        w = rig.weapon(rig.rHand, "hydra");
        rig.box(w, unsc, -2, -3.5f, -7, 4, 4, 8); rig.box(w, unscD, -1.5f, -3, -12, 1, 1, 5); rig.box(w, unscD, 0.5f, -3, -12, 1, 1, 5);
        rig.box(w, unscD, -0.5f, -1.5f, -12, 1, 1, 5); rig.box(w, red, -0.5f, -4.2f, -3, 1, 1, 1); rig.box(w, black, -1, 0.5f, -2, 2, 3, 2);
        w = rig.weapon(rig.rHand, "ravager");
        rig.box(w, banP, -1.5f, -3, -8, 3, 4, 10); rig.box(w, orange, -1, -5, -6, 2, 2, 5); rig.box(w, rust, -2, -2.5f, -11, 1, 1, 3);
        rig.box(w, rust, 1, -2.5f, -11, 1, 1, 3); rig.box(w, black, -1, 1, -2, 2, 3, 2);
        w = rig.weapon(rig.rHand, "cindershot");
        rig.box(w, Mat.of(0x3a3436), -1.5f, -3, -8, 3, 3, 10); rig.box(w, orange, -0.5f, -3.8f, -7, 1, 1, 5); rig.box(w, metal, -1, -2.5f, -11, 2, 2, 3);
        rig.box(w, black, -1, 0, -2, 2, 3, 2);
        w = rig.weapon(rig.rHand, "disruptor");
        rig.box(w, Mat.of(0x4a5a50), -1, -3, -6, 2, 3, 6); rig.box(w, cyan, -1, -4.2f, -4, 2, 1, 2); rig.box(w, metal, -0.5f, -2, -9, 1, 1, 3);
        rig.box(w, black, -1, 0, -1, 2, 3, 2);
        w = rig.weapon(rig.rHand, "energy_sword");
        rig.box(w, metal, -1, -1, -2, 2, 2, 4); rig.box(w, black, -1.5f, -1.5f, -3, 3, 1, 1);
        rig.box(w, sword, -1.6f, -0.8f, -13, 1, 1, 10); rig.box(w, sword, 0.6f, -0.8f, -13, 1, 1, 10);
        w = rig.weapon(rig.rHand, "gravity_hammer");
        rig.box(w, black, -0.5f, -0.5f, -12, 1, 1, 16); rig.box(w, Mat.of(0x2f2a33), -2, -4, -16, 4, 7, 4);
        rig.box(w, purple, -1, -3, -16.6f, 2, 5, 1); rig.box(w, metal, -0.5f, -6, -15, 1, 2, 2);
        // left-hand items
        w = rig.weapon(rig.lHand, "hook");
        rig.box(w, metal, -1, -1, -4, 2, 2, 2); rig.box(w, metal, -1.5f, -1, -6, 1, 1, 2); rig.box(w, metal, 0.5f, -1, -6, 1, 1, 2);
        rig.box(w, Mat.of(HOOK).glow(), -0.5f, -0.5f, -2.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, Mat.of(0x3f4a2e), -1, -1, -2, 2, 2, 2); rig.box(w, metal, -0.5f, -1.8f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "plasma_nade");
        rig.box(w, plasma, -1, -1, -2, 2, 2, 2); rig.box(w, Mat.of(0x22334a), -0.5f, -1.6f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "sensor");
        rig.box(w, black, -0.5f, -0.5f, -4, 1, 1, 3); rig.box(w, red, -0.5f, -0.5f, -5, 1, 1, 1);
        w = rig.weapon(rig.lHand, "wall_emitter");
        rig.box(w, unscD, -1, -0.5f, -3, 2, 1, 3); rig.box(w, Mat.of(0x40b0ff).glow(), -0.5f, -1.2f, -2.5f, 1, 1, 2);
        w = rig.weapon(rig.lHand, "shroud");
        rig.box(w, Mat.of(0x6a7078), -1, -1, -2, 2, 2, 2); rig.box(w, Mat.of(0xe0f0ff).glow(), -0.5f, -1.6f, -1.5f, 1, 1, 1);
    }
}
