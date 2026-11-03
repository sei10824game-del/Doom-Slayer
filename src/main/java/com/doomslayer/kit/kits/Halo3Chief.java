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

/** Master Chief, Halo 3: Mark VI MJOLNIR, energy shields, the full Halo 3 sandbox (UNSC / Covenant / Brute / Forerunner) and equipment. */
public class Halo3Chief extends SlayerKit {
    static final int GOLD = 0xf0b030, SHIELD_C = 0xffd060, PLASMA_C = 0x4aa8ff, PP_C = 0x70ff60, NEEDLE_C = 0xff50d0, LASER_C = 0xff2a20,
            SWORD_C = 0x7ad8ff, HAMMER_C = 0x6a8cff, FUEL_C = 0x60ff40, REGEN_C = 0xff4060, BUBBLE_C = 0xffe080;
    static final int SHIELD_MAX = 16;

    // ---------- projectiles ----------
    static final ProjStyle ROCKET = ProjStyle.make().color(0xffb050).size(0.45f).life(100).explode(3.5f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle PLASMA_BOLT = ProjStyle.make().color(PLASMA_C).size(0.3f).life(45);
    static final ProjStyle PP_BOLT = ProjStyle.make().color(PP_C).size(0.26f).life(45);
    static final ProjStyle PP_OVERCHARGE = ProjStyle.make().color(PP_C).size(0.85f).life(60).homing(0.3f).trail(ParticleTypes.HAPPY_VILLAGER)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, PP_C, 1.6f, pos, 30, 0.6); Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 20, 0.5, 0.3);
                Fx.sound(l, pos, SoundEvents.FIRECHARGE_USE, 1, 1.6f);
                if (hit instanceof LivingEntity le) { le.setAbsorptionAmount(0); le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2)); }
            });
    static final ProjStyle NEEDLE = ProjStyle.make().color(NEEDLE_C).size(0.16f).life(60).homing(0.22f).trail(ParticleTypes.END_ROD)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, NEEDLE_C, 0.8f, pos, 5, 0.1);
                if (!(hit instanceof LivingEntity le) || !(p.getOwner() instanceof SlayerEntity o)) return;
                CompoundTag d = le.getPersistentData();
                int n = d.getInt("h3_needles") + 1;
                if (n >= 7) { // supercombine
                    d.putInt("h3_needles", 0);
                    Combat.blast(o, pos, 2.5f, 14, 0.9f, null); Combat.damage(o, le, 10);
                    Fx.dustBurst(l, NEEDLE_C, 2f, pos, 50, 0.9); Fx.burst(l, ParticleTypes.END_ROD, pos, 30, 0.3, 0.3);
                    Fx.ring(l, pos, 1.8f, NEEDLE_C, 1.4f, 24);
                    Fx.sound(l, pos, SoundEvents.AMETHYST_BLOCK_BREAK, 1.5f, 0.6f); Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.6f);
                } else { d.putInt("h3_needles", n); Fx.sound(l, pos, SoundEvents.AMETHYST_CLUSTER_HIT, 0.7f, 1.6f); }
            });
    static final ProjStyle BRUTE_NADE = ProjStyle.make().color(0x5a4a3a).size(0.3f).life(28).gravity(0.05).bounce().explode(2.8f).trail(ParticleTypes.SMOKE);
    static final ProjStyle SPIKE = ProjStyle.make().color(0xc0a070).size(0.14f).life(30).gravity(0.012).trail(ParticleTypes.CRIT);
    static final ProjStyle SHRAPNEL = ProjStyle.make().color(0xb0a080).size(0.12f).life(10).pierce(1).trail(ParticleTypes.CRIT);
    static final ProjStyle FUEL_ROD = ProjStyle.make().color(FUEL_C).size(0.55f).life(70).gravity(0.02).explode(3.2f).trail(ParticleTypes.HAPPY_VILLAGER);
    static final ProjStyle MISSILE = ProjStyle.make().color(0xffa060).size(0.25f).life(90).homing(0.25f).explode(2.4f).trail(ParticleTypes.SMOKE);
    static final ProjStyle FRAG = ProjStyle.make().color(0x4f5f30).size(0.28f).life(34).gravity(0.05).bounce().explode(3.6f).trail(ParticleTypes.SMOKE);
    /** stuck charge: follows whatever it is attached to (aux = entity id + 1), explodes when life runs out */
    static void follow(SlayerProjectile p) {
        if (p.aux <= 0) return;
        Entity h = p.level().getEntity(p.aux - 1);
        if (h != null && h.isAlive()) p.setPos(h.getX(), h.getY() + h.getBbHeight() * 0.6, h.getZ());
    }
    static void stick(SlayerProjectile from, ProjStyle s, Vec3 pos, Entity hit, float dmg) {
        if (!(from.getOwner() instanceof SlayerEntity o)) return;
        SlayerProjectile q = Combat.shoot(o, s, pos, new Vec3(0, 1, 0), 0, dmg);
        q.aux = hit != null ? hit.getId() + 1 : 0;
    }
    static final ProjStyle PLASMA_STUCK = ProjStyle.make().color(PLASMA_C).size(0.32f).life(22).explode(3.4f).trail(ParticleTypes.ELECTRIC_SPARK)
            .onTick(p -> {
                follow(p);
                if (p.age2 % 3 == 0) Fx.dustBurst((ServerLevel) p.level(), PLASMA_C, 1.2f, p.position(), 4, 0.15);
            })
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, PLASMA_C, 2f, pos, 50, 1.1); Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 40, 1, 0.4);
                Fx.ring(l, pos, 2.5f, PLASMA_C, 1.6f, 28); Fx.burst(l, ParticleTypes.FLASH, pos, 1, 0, 0);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1.3f, 1.3f); Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1, 2f);
            });
    static final ProjStyle PLASMA_NADE = ProjStyle.make().color(PLASMA_C).size(0.3f).life(40).gravity(0.04).trail(ParticleTypes.ELECTRIC_SPARK)
            .onHit((p, pos, hit) -> { Fx.sound((ServerLevel) p.level(), pos, SoundEvents.SLIME_BLOCK_PLACE, 1, 1.4f); stick(p, PLASMA_STUCK, pos, hit, 16); });
    static final ProjStyle SPIKE_ARMED = ProjStyle.make().color(0x8a7a5a).size(0.3f).life(14).trail(ParticleTypes.CRIT)
            .onTick(p -> { follow(p); if (p.age2 % 4 == 0) Fx.dustBurst((ServerLevel) p.level(), 0xff6020, 0.8f, p.position(), 2, 0.05); })
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 1.5f); Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1, 1.5f); Fx.sound(l, pos, SoundEvents.CHAIN_BREAK, 1.2f, 0.7f);
                if (!(p.getOwner() instanceof SlayerEntity o)) return;
                Combat.blast(o, pos, 2f, 8, 0.6f, null);
                for (int i = 0; i < 16; i++) {
                    double a = i * Math.PI / 8, up = 0.15 + l.random.nextDouble() * 0.5;
                    Combat.shoot(o, SHRAPNEL, pos, new Vec3(Math.cos(a), up, Math.sin(a)), 1.2, 5);
                }
            });
    static final ProjStyle SPIKE_NADE = ProjStyle.make().color(0x7a6a50).size(0.3f).life(32).gravity(0.05).trail(ParticleTypes.CRIT)
            .onHit((p, pos, hit) -> { Fx.sound((ServerLevel) p.level(), pos, SoundEvents.TRIDENT_HIT, 1, 0.8f); stick(p, SPIKE_ARMED, pos, hit, 6); });
    static final ProjStyle FIRE_POOL = ProjStyle.make().color(0xff6010).size(0.2f).life(90).invisible()
            .onTick(p -> {
                ServerLevel l = (ServerLevel) p.level();
                if (p.age2 % 2 == 0) for (int i = 0; i < 6; i++) {
                    double a = l.random.nextDouble() * Math.PI * 2, r = l.random.nextDouble() * 2.6;
                    Fx.burst(l, ParticleTypes.FLAME, p.position().add(Math.cos(a) * r, 0.1, Math.sin(a) * r), 1, 0.05, 0.03);
                }
                if (p.age2 % 10 == 0 && p.getOwner() instanceof SlayerEntity o) {
                    Fx.sound(l, p.position(), SoundEvents.FIRE_AMBIENT, 1, 1);
                    for (LivingEntity f : Combat.around(o, p.position(), 2.8)) { f.setSecondsOnFire(4); Combat.damage(o, f, 2.5f); }
                }
            })
            .onHit((p, pos, hit) -> {});
    static final ProjStyle FIREBOMB = ProjStyle.make().color(0xff7020).size(0.3f).life(30).gravity(0.05).trail(ParticleTypes.FLAME)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.burst(l, ParticleTypes.FLAME, pos, 60, 1.4, 0.12); Fx.burst(l, ParticleTypes.LAVA, pos, 8, 0.6, 0.1);
                Fx.sound(l, pos, SoundEvents.GLASS_BREAK, 1, 0.7f); Fx.sound(l, pos, SoundEvents.FIRECHARGE_USE, 1.4f, 0.6f);
                stick(p, FIRE_POOL, pos.add(0, 0.1, 0), null, 0);
            });

    // ---------- equipment (stationary "device" projectiles) ----------
    static boolean ally(Entity x) { return x instanceof Player || x instanceof SlayerEntity; }
    static List<LivingEntity> allies(ServerLevel l, Vec3 c, double r) {
        return l.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), x -> ally(x) && x.distanceToSqr(c) <= r * r);
    }
    /** destroy hostile projectiles near c (bubble shield / cover) */
    static void eatProjectiles(ServerLevel l, Vec3 c, double r) {
        for (Projectile pr : l.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(r), x -> !(x instanceof SlayerProjectile) && !ally(x.getOwner()))) {
            Fx.dustBurst(l, BUBBLE_C, 1f, pr.position(), 6, 0.15); pr.discard();
        }
    }
    static final ProjStyle BUBBLE = ProjStyle.make().color(BUBBLE_C).size(0.1f).life(240).invisible()
            .onTick(p -> {
                ServerLevel l = (ServerLevel) p.level(); Vec3 c = p.position();
                if (p.age2 % 4 == 0) for (int i = 0; i < 5; i++) {
                    double y = i / 5.0 * 3.2, r = Math.sqrt(Math.max(0, 3.4 * 3.4 - y * y));
                    Fx.ring(l, c.add(0, y, 0), (float) r, BUBBLE_C, 0.9f, 22 - i * 3);
                }
                eatProjectiles(l, c.add(0, 1, 0), 3.8);
                if (p.age2 % 10 == 0) for (LivingEntity a : allies(l, c, 3.4)) a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 15, 2, false, false));
                if (p.age2 % 40 == 0) Fx.sound(l, c, SoundEvents.BEACON_AMBIENT, 0.8f, 1.6f);
            })
            .onHit((p, pos, hit) -> { Fx.dustBurst((ServerLevel) p.level(), BUBBLE_C, 1.5f, pos.add(0, 1.5, 0), 40, 2); Fx.sound((ServerLevel) p.level(), pos, SoundEvents.BEACON_DEACTIVATE, 1, 1.4f); });
    static final ProjStyle REGEN_FIELD = ProjStyle.make().color(REGEN_C).size(0.1f).life(200).invisible()
            .onTick(p -> {
                ServerLevel l = (ServerLevel) p.level(); Vec3 c = p.position();
                if (p.age2 % 10 == 0) {
                    float r = 0.5f + (p.age2 % 30) / 30f * 3.2f;
                    Fx.ring(l, c.add(0, 0.2, 0), r, REGEN_C, 1.2f, 26); Fx.dustBurst(l, REGEN_C, 1f, c.add(0, 0.5, 0), 4, 0.1);
                    for (LivingEntity a : allies(l, c, 3.6)) { a.heal(1.5f); Fx.burst(l, ParticleTypes.HEART, a.position().add(0, a.getBbHeight(), 0), 1, 0.2, 0); }
                    Fx.sound(l, c, SoundEvents.BEACON_POWER_SELECT, 0.5f, 1.9f);
                }
            })
            .onHit((p, pos, hit) -> {});
    static final ProjStyle DRAIN_FIELD = ProjStyle.make().color(0x80c0ff).size(0.1f).life(40).invisible()
            .onTick(p -> {
                ServerLevel l = (ServerLevel) p.level(); Vec3 c = p.position();
                if (p.age2 % 8 != 0 || !(p.getOwner() instanceof SlayerEntity o)) return;
                Fx.ring(l, c.add(0, 0.6, 0), 4.2f, 0x80c0ff, 1.3f, 36); Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, c.add(0, 0.6, 0), 30, 2, 0.2);
                Fx.sound(l, c, SoundEvents.BEACON_DEACTIVATE, 1, 0.6f);
                for (LivingEntity f : Combat.around(o, c, 4.5)) {
                    f.setAbsorptionAmount(0); Combat.damage(o, f, 4);
                    f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1)); f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                    Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, f.position().add(0, f.getBbHeight() / 2, 0), 8, 0.3, 0.2);
                }
            })
            .onHit((p, pos, hit) -> {});
    static final ProjStyle DRAIN_DEVICE = ProjStyle.make().color(0x80c0ff).size(0.3f).life(16).gravity(0.05).bounce().trail(ParticleTypes.ELECTRIC_SPARK)
            .onHit((p, pos, hit) -> { Fx.sound((ServerLevel) p.level(), pos, SoundEvents.BEACON_ACTIVATE, 1, 1.8f); stick(p, DRAIN_FIELD, pos, null, 0); });
    static final ProjStyle FLARE = ProjStyle.make().color(0xfff4c0).size(0.4f).life(26).trail(ParticleTypes.END_ROD)
            .onTick(p -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.burst(l, ParticleTypes.END_ROD, p.position(), 3, 0.3, 0.05);
                if (p.age2 % 5 == 0 && p.getOwner() instanceof SlayerEntity o) for (LivingEntity f : Combat.around(o, p.position(), 10)) {
                    f.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0)); f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                    if (f instanceof Mob m) m.getNavigation().stop();
                }
            })
            .onHit((p, pos, hit) -> { ServerLevel l = (ServerLevel) p.level(); Fx.burst(l, ParticleTypes.FLASH, pos, 2, 0.2, 0); Fx.burst(l, ParticleTypes.END_ROD, pos, 40, 1.5, 0.2); });
    static final ProjStyle TRIP_MINE = ProjStyle.make().color(0xff3020).size(0.25f).life(600).explode(4f)
            .onTick(p -> {
                ServerLevel l = (ServerLevel) p.level();
                if (p.age2 % 10 == 0) Fx.dustBurst(l, 0xff2010, 0.9f, p.position().add(0, 0.2, 0), 1, 0);
                if (p.age2 > 20 && p.getOwner() instanceof SlayerEntity o && !Combat.around(o, p.position(), 2.3).isEmpty()) {
                    Fx.sound(l, p.position(), SoundEvents.TRIPWIRE_CLICK_ON, 1, 2f); p.age2 = p.style().life;
                }
            });
    static final ProjStyle GRAV_LIFT = ProjStyle.make().color(0x60b0ff).size(0.1f).life(140).invisible()
            .onTick(p -> {
                ServerLevel l = (ServerLevel) p.level(); Vec3 c = p.position();
                if (p.age2 % 2 == 0) for (int i = 0; i < 3; i++) Fx.dustBurst(l, 0x60b0ff, 1f, c.add(0, l.random.nextDouble() * 4, 0), 1, 0.4);
                if (p.age2 % 4 == 0) Fx.ring(l, c.add(0, 0.1, 0), 0.9f, 0x80c8ff, 1f, 14);
                for (LivingEntity a : allies(l, c.add(0, 0.5, 0), 1.3)) {
                    a.setDeltaMovement(a.getDeltaMovement().x, 1.1, a.getDeltaMovement().z); a.hurtMarked = true;
                    a.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
                    Fx.sound(l, c, SoundEvents.ENDER_DRAGON_FLAP, 0.5f, 1.8f);
                }
            })
            .onHit((p, pos, hit) -> {});
    static final ProjStyle COVER = ProjStyle.make().color(0x80b8ff).size(0.1f).life(200).invisible()
            .onTick(p -> {
                ServerLevel l = (ServerLevel) p.level(); Vec3 c = p.position(), f = p.auxVec == null ? new Vec3(0, 0, 1) : p.auxVec;
                Vec3 side = new Vec3(-f.z, 0, f.x);
                if (p.age2 % 3 == 0) for (int i = -4; i <= 4; i++) for (int j = 0; j < 4; j++)
                    Fx.dustBurst(l, 0x80b8ff, 0.8f, c.add(side.scale(i * 0.35)).add(0, j * 0.5, 0), 1, 0.02);
                eatProjectiles(l, c.add(0, 1, 0), 1.8);
                if (p.age2 % 10 == 0) for (LivingEntity a : allies(l, c.subtract(f.scale(1.5)), 2.2))
                    a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 15, 1, false, false));
            })
            .onHit((p, pos, hit) -> Fx.sound((ServerLevel) p.level(), pos, SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.2f));

    public Halo3Chief() {
        super("halo3_chief", "Master Chief", "マスターチーフ", "Halo 3");
        eggPrimary = 0x56683a; eggSecondary = GOLD;
        maxHealth = 90; speed = 0.31; armor = 14; knockbackRes = 0.6;
        scale = 1.1f; width = 0.72f; preferredRange = 10f; dmgMult = 1.05f;
        idleWeaponR = "ma5c";
    }

    // ================= helpers (server) =================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.2, 0.32, 0); }
    static Vec3 mzL(SlayerEntity e) { return Combat.muzzle(e, 1.1, -0.32, 0); }
    static Vec3 aim(SlayerEntity e, Vec3 from, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        return Combat.dirTo(from, t, spread, new Random(e.getRandom().nextLong()));
    }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) { return aim(e, mz(e), t, spread); }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static Vec3 endOf(SlayerEntity e, LivingEntity t, Vec3 dir, double range) { return t != null ? mid(t) : mz(e).add(dir.scale(range)); }
    static void flash(SlayerEntity e, Vec3 m, int rgb) {
        Fx.burst(L(e), ParticleTypes.FLAME, m, 2, 0.04, 0.02); Fx.dustBurst(L(e), rgb, 1.1f, m, 3, 0.07);
    }
    static void brass(SlayerEntity e) { Fx.burst(L(e), ParticleTypes.SMOKE, Combat.muzzle(e, 0.4, 0.55, 0.1), 1, 0.03, 0.03); }
    static void bullet(SlayerEntity e, LivingEntity t, double spread, double range, float dmg, int rgb, float pitch) {
        Combat.hitscan(e, mz(e), aim(e, t, spread), range, dmg, 0, rgb); flash(e, mz(e), 0xffc060);
        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, pitch);
    }
    static void hold(SlayerEntity e, LivingEntity t, double dist, double lift) {
        if (t == null || !Combat.isFoe(e, t)) return;
        Vec3 f = Vec3.directionFromRotation(0, e.getYRot());
        Vec3 p = e.position().add(f.scale(dist + t.getBbWidth() * 0.5)).add(0, lift, 0);
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true;
    }
    static void finish(SlayerEntity e, LivingEntity t, SoundEvent s) {
        if (t == null) return;
        Fx.sparks(L(e), mid(t), 14); Fx.burst(L(e), ParticleTypes.DAMAGE_INDICATOR, mid(t), 8, 0.3, 0.1);
        Combat.damage(e, t, t.getHealth() + 100);
        snd(e, s, 1.2f, 0.8f); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.7f);
    }
    static Vec3 hand(SlayerEntity e) { return Combat.muzzle(e, 0.6, -0.4, 0.3); }
    static Vec3 lobDir(SlayerEntity e, LivingEntity t, Vec3 from, double arc) {
        double dist = t != null ? from.distanceTo(mid(t)) : 10;
        return aim(e, from, t, 0.02).add(0, arc + dist * 0.018, 0).normalize();
    }
    static double lobSpeed(LivingEntity t, Vec3 from) { return 0.7 + (t != null ? from.distanceTo(mid(t)) : 10) * 0.025; }
    static void deploy(SlayerEntity e, ProjStyle s, Vec3 at) { Combat.shoot(e, s, at, new Vec3(0, 1, 0), 0, 0); }
    static Vec3 ahead(SlayerEntity e, double d) { return e.position().add(Vec3.directionFromRotation(0, e.getYRot()).scale(d)).add(0, 0.1, 0); }
    static void buff(SlayerEntity e, net.minecraft.world.effect.MobEffect m, int ticks, int amp) { e.addEffect(new MobEffectInstance(m, ticks, amp)); }

    // ================= passive: MJOLNIR energy shield =================
    @Override
    public void passive(SlayerEntity e) {
        CompoundTag d = e.getPersistentData();
        float abs = e.getAbsorptionAmount(), total = e.getHealth() + abs;
        int delay = d.getInt("h3_delay");
        boolean up = d.getBoolean("h3_up");
        Vec3 c = e.position().add(0, e.getBbHeight() * 0.55, 0);
        if (total < d.getFloat("h3_last") - 0.01f) {
            delay = 90; d.putBoolean("h3_chg", false);
            if (up && abs <= 0) { // shields down: the pop
                Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, c, 30, 0.5, 0.3); Fx.dustBurst(L(e), SHIELD_C, 1.3f, c, 30, 0.6);
                snd(e, SoundEvents.SHIELD_BREAK, 1.2f, 1.3f); snd(e, SoundEvents.BEACON_DEACTIVATE, 0.8f, 2f);
            } else if (abs > 0) Fx.dustBurst(L(e), SHIELD_C, 1f, c, 10, 0.45); // gold shield flare
        } else if (delay > 0) {
            delay--;
            if (abs <= 0 && e.tickCount % 12 == 0) snd(e, SoundEvents.NOTE_BLOCK_BIT.value(), 0.5f, 1.9f); // low shield beeping
        } else if (abs < SHIELD_MAX) {
            if (!d.getBoolean("h3_chg")) { d.putBoolean("h3_chg", true); snd(e, SoundEvents.BEACON_POWER_SELECT, 1f, 1.7f); }
            if (e.tickCount % 2 == 0) e.setAbsorptionAmount(Math.min(SHIELD_MAX, abs + 1));
            if (e.tickCount % 3 == 0) Fx.ring(L(e), e.position().add(0, (e.tickCount % 12) / 6.0, 0), 0.6f, SHIELD_C, 0.8f, 10);
        } else if (e.tickCount % 50 == 0 && e.getHealth() < e.getMaxHealth()) e.heal(1);
        d.putInt("h3_delay", delay);
        d.putBoolean("h3_up", e.getAbsorptionAmount() > 0);
        d.putFloat("h3_last", e.getHealth() + e.getAbsorptionAmount());
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** shouldered two-hand rifle, slight bladed stance */
    static void shoulder(Pose p, float k, float pitch, float rec) {
        float tw = 0.18f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - rec * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.12f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.08f - rec * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.5f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * 0.8f * k; p.bodyX += (rec * 0.1f - pitch * 0.2f) * k; p.headX -= rec * 0.06f * k;
        p.rootZ += rec * 0.8f * k;
    }
    /** heavy weapon braced at the hip */
    static void hip(Pose p, float k, float pitch, float rec) {
        p.rArmX = lerp(p.rArmX, -1.0f + pitch - rec * 0.2f, k); p.rArmY = lerp(p.rArmY, -0.2f, k); p.rArmZ = lerp(p.rArmZ, 0.15f, k);
        p.lArmX = lerp(p.lArmX, -1.3f + pitch - rec * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.7f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX = lerp(p.rHandX, -0.55f, k);
        p.bodyX += (0.12f + rec * 0.1f) * k; p.bodyY += 0.15f * k; p.headY -= 0.15f * k; p.rootZ += rec * 0.7f * k;
    }
    /** one gun per hand, arms splayed slightly */
    static void dual(Pose p, float k, float pitch, float recR, float recL) {
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recR * 0.4f, k); p.rArmY = lerp(p.rArmY, 0.12f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch - recL * 0.4f, k); p.lArmY = lerp(p.lArmY, -0.12f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += (recL - recR) * 0.08f * k; p.bodyX += 0.08f * k; p.headX -= 0.04f * (recR + recL) * k;
        p.rootZ += (recR + recL) * 0.35f * k;
    }
    /** one-handed right pistol, left arm guarding */
    static void oneR(Pose p, float k, float pitch, float rec) {
        p.rArmX = lerp(p.rArmX, -HALF + pitch - rec * 0.35f, k); p.rArmY = lerp(p.rArmY, 0.25f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -0.35f, k); p.lArmZ = lerp(p.lArmZ, -0.25f, k);
        p.bodyY -= 0.3f * k; p.headY += 0.3f * k; p.rHandX -= rec * 0.3f; p.rootZ += rec * 0.4f * k;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.26f, k);
        p.lLegZ -= 0.06f * k; p.rLegZ += 0.08f * k; p.rootY -= 0.4f * k;
    }
    static void wide(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.2f, k); p.rLegX = lerp(p.rLegX, 0.15f, k);
        p.lLegZ -= 0.3f * k; p.rLegZ += 0.3f * k; p.rootY -= 1.2f * k;
    }
    /** right knee down */
    static void kneel(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -1.25f, k); p.rLegX = lerp(p.rLegX, 0.55f, k); p.rLegZ += 0.05f * k;
        p.rootY -= 3.2f * k; p.bodyX += 0.1f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // MA5C at low ready across the chest
        p.rArmX = -0.6f + p.rArmX * 0.25f; p.rArmY = -0.25f;
        p.lArmX = -0.85f + p.lArmX * 0.25f; p.lArmY = 0.6f; p.lArmZ = 0.05f;
        p.rHandX = -0.3f; p.bodyX += 0.04f; p.headX += 0.02f * (float) Math.sin(age * 0.05f);
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        melee(out);
        unsc(out);
        covenant(out);
        support(out);
        grenades(out);
        equipment(out);
    }

    // ---------- melee / executions ----------
    private void melee(List<AttackDef> out) {
        // Rifle melee: MA5C butt-stroke (wind back, hook stock across, follow-through)
        out.add(AttackDef.of("rifle_melee").range(0, 3.2f).cd(22).weight(4).dur(16).weapon("ma5c")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 1.4f);
                    if (n == 7) {
                        Fx.arc(L(e), e.position().add(0, 1.5, 0), e.getYRot(), 1.3f, -60, 50, -20, 0xd0d0c0, 0.8f, 10);
                        for (LivingEntity f : Combat.cone(e, 3.3, 55)) { Combat.damage(e, f, 10); Combat.knock(f, e.position(), 1.1, 0.35); Fx.sparks(L(e), mid(f), 8); }
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.7f); snd(e, SoundEvents.ANVIL_PLACE, 0.4f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -1.0f, .3f, -0.7f, .44f, -1.55f, .6f, -1.45f, 1, -0.6f);
                    p.rArmY = track(t, 0, -0.2f, .3f, 0.35f, .44f, -0.55f, .65f, -0.6f, 1, -0.25f);
                    p.lArmX = track(t, 0, -1.1f, .3f, -0.8f, .44f, -1.7f, .65f, -1.6f, 1, -0.85f);
                    p.lArmY = track(t, 0, 0.6f, .3f, 0.9f, .44f, 0.1f, 1, 0.6f);
                    p.rHandX = track(t, 0, -0.3f, .3f, 0.6f, .44f, -0.9f, .7f, -0.7f, 1, -0.3f);
                    p.rHandZ = track(t, .25f, 0, .44f, 0.5f, .7f, 0.3f, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, 0.45f, .44f, -0.6f, .65f, -0.5f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyX = track(t, 0, 0, .3f, -0.08f, .44f, 0.3f, .7f, 0.25f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.15f, .44f, -0.55f, .8f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .44f, 0.4f, .8f, 0.35f, 1, 0);
                    p.rootZ = track(t, 0, 0, .3f, 0.4f, .44f, -2.2f, .75f, -2f, 1, 0); p.rootY = -0.5f * bump(win(t, .3f, .9f));
                }));

        // Assassination (from behind): knee-kick to the back of the leg, then a hammer-fist down on the skull
        out.add(AttackDef.of("assassination").range(0, 3.5f).lowHealthTarget(0.25f).weight(6).cd(80).dur(34).weapon("none")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.45, 0.05); snd(e, SoundEvents.ARMOR_EQUIP_IRON, 1, 0.7f); }
                    hold(e, t, 0.9, n < 9 ? 0 : -0.35);
                    if (n == 9 && t != null) { Combat.damage(e, t, Math.max(0, t.getHealth() - 1)); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f); Fx.sparks(L(e), t.position().add(0, 0.4, 0), 6); }
                    if (n == 22) { finish(e, t, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR); Fx.burst(L(e), ParticleTypes.POOF, t != null ? t.position() : e.position(), 10, 0.3, 0.05); }
                })
                .anim((p, t, a) -> {
                    p.rLegX = track(t, 0, 0, .15f, 0.5f, .26f, -1.1f, .34f, -0.2f, .5f, 0.3f, 1, 0);
                    p.lLegX = track(t, 0, 0, .26f, 0.1f, .5f, -0.5f, .85f, -0.5f, 1, 0);
                    p.lArmX = track(t, 0, 0, .15f, -1.5f, .5f, -1.3f, .6f, -0.7f, 1, 0); p.lArmY = track(t, 0, 0, .15f, 0.4f, .6f, 0.2f, 1, 0);
                    p.rArmX = track(t, .3f, -0.3f, .5f, -2.9f, .62f, -2.95f, .66f, -0.4f, .85f, -0.5f, 1, 0);
                    p.rArmZ = track(t, .3f, 0, .5f, -0.25f, .66f, 0.15f, 1, 0);
                    p.bodyX = track(t, 0, 0, .26f, -0.15f, .5f, -0.25f, .66f, 0.6f, .85f, 0.5f, 1, 0);
                    p.bodyY = track(t, 0, 0, .26f, 0.2f, .5f, 0.35f, .66f, -0.15f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.headX = track(t, .5f, 0, .66f, 0.45f, 1, 0);
                    p.rootY = track(t, .5f, 0, .58f, 0.6f, .68f, -1.6f, .85f, -1.3f, 1, 0); p.rootZ = track(t, 0, 0, .15f, -1.8f, .85f, -1.8f, 1, 0);
                }));

        // Energy Sword lunge (ignite, lunge, horizontal cleave)
        out.add(AttackDef.of("energy_sword_lunge").range(0, 7).cd(150).weight(3).dur(26).weapon("sword")
                .tick((e, t, n) -> {
                    if (n == 0) { snd(e, SoundEvents.BEACON_ACTIVATE, 1, 2f); Fx.dustBurst(L(e), SWORD_C, 1.2f, mz(e), 12, 0.3); }
                    if (n == 6) { Combat.lunge(e, 1.3, 0.1); snd(e, SoundEvents.TRIDENT_RIPTIDE_1, 0.9f, 1.6f); }
                    if (n >= 6 && n < 11) Fx.dustBurst(L(e), SWORD_C, 1f, e.position().add(0, 1.2, 0), 4, 0.3);
                    if (n == 11) {
                        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.8f, -75, 75, 8, SWORD_C, 1.3f, 18);
                        for (LivingEntity f : Combat.cone(e, 3.6, 70)) { Combat.damage(e, f, 26); Combat.knock(f, e.position(), 0.8, 0.25); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(f), 12, 0.3, 0.2); }
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.3f, 0.6f); snd(e, SoundEvents.FIRECHARGE_USE, 0.6f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -0.4f, .2f, -1.0f, .42f, -1.4f, .62f, -1.3f, 1, -0.4f);
                    p.rArmY = track(t, 0, 0, .2f, -0.9f, .42f, 0.9f, .62f, 1.1f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .2f, 0.5f, .42f, 0.1f, 1, 0);
                    p.rHandY = track(t, .2f, 0, .42f, -0.4f, 1, 0);
                    p.lArmX = track(t, 0, 0, .2f, -0.9f, .42f, -0.5f, .62f, 0.4f, 1, 0); p.lArmZ = track(t, 0, 0, .2f, -0.3f, .62f, -0.6f, 1, 0);
                    p.bodyY = track(t, 0, 0, .2f, -0.6f, .42f, 0.7f, .62f, 0.8f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .2f, 0.2f, .3f, 0.45f, .62f, 0.35f, 1, 0);
                    p.lLegX = track(t, 0, 0, .2f, -0.3f, .3f, -0.9f, .62f, -0.7f, 1, 0); p.rLegX = track(t, 0, 0, .2f, 0.4f, .3f, 0.8f, .62f, 0.5f, 1, 0);
                    p.rootY = track(t, 0, 0, .2f, -1.0f, .3f, 0.6f, .42f, -1.4f, .7f, -1.0f, 1, 0); p.rootZ = track(t, .2f, 0, .3f, -3f, .62f, -2.5f, 1, 0);
                }));

        // Energy Sword assassination: blade through the back, lift, pull free
        out.add(AttackDef.of("sword_assassination").range(0, 3.5f).lowHealthTarget(0.25f).weight(5).cd(100).dur(30).weapon("sword")
                .tick((e, t, n) -> {
                    if (n == 0) snd(e, SoundEvents.BEACON_ACTIVATE, 1, 2f);
                    hold(e, t, 1.0, n > 10 && n < 22 ? 0.4 : 0);
                    if (n == 10 && t != null) { Combat.damage(e, t, Math.max(0, t.getHealth() - 1)); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(t), 20, 0.2, 0.3); snd(e, SoundEvents.TRIDENT_HIT, 1.2f, 0.8f); }
                    if (n > 10 && n < 22 && t != null) Fx.dustBurst(L(e), SWORD_C, 1f, mid(t), 3, 0.2);
                    if (n == 22) finish(e, t, SoundEvents.TRIDENT_RETURN);
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -0.3f, .2f, -0.2f, .33f, -1.7f, .7f, -1.9f, .8f, -0.9f, 1, -0.3f);
                    p.rHandX = track(t, 0, 0, .2f, 1.2f, .33f, 0.1f, .8f, 0, 1, 0);
                    p.rArmZ = track(t, 0, 0, .33f, -0.2f, .8f, 0.3f, 1, 0);
                    p.lArmX = track(t, 0, 0, .2f, -1.5f, .7f, -1.6f, .8f, -0.4f, 1, 0); p.lArmY = track(t, 0, 0, .2f, 0.3f, 1, 0);
                    p.bodyX = track(t, 0, 0, .2f, -0.2f, .33f, 0.35f, .7f, 0.05f, .8f, 0.3f, 1, 0);
                    p.bodyY = track(t, 0, 0, .2f, 0.35f, .33f, -0.15f, 1, 0); p.headY = -p.bodyY * 0.5f; p.headX = track(t, .3f, 0, .4f, 0.3f, 1, 0);
                    p.rootY = track(t, .33f, 0, .7f, 0.6f, .8f, -0.6f, 1, 0); p.rootZ = track(t, 0, 0, .2f, -1.8f, .85f, -1.8f, 1, 0);
                    p.lLegX = track(t, 0, 0, .2f, -0.45f, .85f, -0.45f, 1, 0); p.rLegX = track(t, 0, 0, .2f, 0.35f, .85f, 0.35f, 1, 0);
                }));

        // Gravity Hammer: raise overhead, hop, slam -> gravity shockwave
        out.add(AttackDef.of("gravity_hammer").range(0, 5).cd(110).weight(3).dur(32).weapon("hammer")
                .tick((e, t, n) -> {
                    if (n == 6) { snd(e, SoundEvents.BEACON_POWER_SELECT, 0.9f, 0.6f); Fx.dustBurst(L(e), HAMMER_C, 1.2f, e.position().add(0, 3, 0), 10, 0.4); }
                    if (n == 13) Combat.lunge(e, 0.5, 0.35);
                    if (n == 19) {
                        Vec3 c = ahead(e, 1.8);
                        for (int i = 0; i < 4; i++) Fx.ring(L(e), c.add(0, 0.15 + i * 0.25, 0), 1.2f + i * 1.1f, HAMMER_C, 1.4f, 24 + i * 6);
                        Fx.burst(L(e), ParticleTypes.POOF, c, 30, 1.6, 0.1); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, c, 30, 1.2, 0.4);
                        Combat.blast(e, c, 4.8f, 20, 2.2f, null);
                        snd(e, SoundEvents.ANVIL_LAND, 1.4f, 0.5f); snd(e, SoundEvents.GENERIC_EXPLODE, 1.2f, 0.8f); snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float up = win(t, .05f, .5f) * (1 - win(t, .52f, .6f));
                    p.rArmX = track(t, 0, -0.9f, .45f, -3.0f, .55f, -3.05f, .6f, -0.9f, .8f, -0.8f, 1, -0.9f);
                    p.lArmX = track(t, 0, -1.1f, .45f, -2.9f, .55f, -2.95f, .6f, -1.0f, .8f, -0.9f, 1, -1.1f);
                    p.rArmZ = track(t, 0, 0, .45f, -0.15f, .6f, 0.1f, 1, 0); p.lArmZ = -p.rArmZ; p.lArmY = 0.3f * (1 - up); p.rArmY = -0.1f;
                    p.rHandX = track(t, 0, -0.5f, .45f, 0.9f, .6f, -0.4f, .8f, -0.6f, 1, -0.5f);
                    p.bodyX = track(t, 0, 0, .45f, -0.35f, .55f, -0.4f, .6f, 0.65f, .8f, 0.55f, 1, 0);
                    p.headX = track(t, 0, 0, .45f, -0.3f, .6f, 0.3f, 1, 0);
                    p.rootY = track(t, 0, 0, .35f, -0.8f, .45f, 2.2f, .55f, 2.6f, .6f, -2.2f, .82f, -1.8f, 1, 0);
                    p.rootZ = track(t, .35f, 0, .55f, -1.5f, .6f, -2.6f, 1, 0);
                    p.lLegX = track(t, 0, 0, .35f, -0.5f, .5f, 0.2f, .6f, -0.8f, .85f, -0.7f, 1, 0);
                    p.rLegX = track(t, 0, 0, .35f, 0.4f, .5f, 0.3f, .6f, 0.6f, .85f, 0.5f, 1, 0);
                    p.lLegZ = -0.2f * win(t, .58f, .62f) * (1 - win(t, .85f, 1)); p.rLegZ = -p.lLegZ;
                    p.bodyZ = 0.05f * (float) Math.sin(a * 2) * win(t, .1f, .3f) * (1 - win(t, .4f, .45f));
                }));
    }

    // ---------- UNSC arsenal ----------
    private void unsc(List<AttackDef> out) {
        // MA5C assault rifle: sustained auto fire, muzzle climb
        out.add(AttackDef.of("ma5c").range(2, 18).cd(24).weight(5).dur(30).weapon("ma5c")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 24) { bullet(e, t, 0.045, 22, 1.7f, 0xffe080, 1.6f + e.getRandom().nextFloat() * 0.2f); if (n % 2 == 0) brass(e); }
                    if (n == 27) snd(e, SoundEvents.ARMOR_EQUIP_CHAIN, 0.6f, 1.3f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), fire = win(t, .12f, .14f) * (1 - win(t, .82f, .84f));
                    float r = 0.28f * (0.5f + 0.5f * (float) Math.sin(a * PI * 0.9f)) * fire, climb = -0.12f * win(t, .14f, .82f) * fire;
                    shoulder(p, k, climb, r); stance(p, k);
                    p.rHandZ += 0.03f * (float) Math.sin(a * 3.1f) * fire; p.bodyZ += 0.02f * (float) Math.sin(a * 2.3f) * fire;
                    p.headX += 0.08f * k;
                }));

        // BR55 battle rifle: two 3-round bursts
        out.add(AttackDef.of("br55_burst").range(5, 30).cd(26).weight(5).dur(26).weapon("br55")
                .tick((e, t, n) -> {
                    if (n == 5 || n == 6 || n == 7 || n == 15 || n == 16 || n == 17) { bullet(e, t, 0.012, 34, 4f, 0xfff0a0, 1.25f); brass(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .88f);
                    float r = 0.9f * (kick(t, 5 / 26f, .15f) + kick(t, 15 / 26f, .15f));
                    shoulder(p, k, 0, r); stance(p, k);
                    p.headX += 0.14f * k; p.headY -= 0.08f * k; p.bodyX += 0.06f * k; // cheek to the scope
                    p.rHandX -= r * 0.25f; p.lArmY += 0.04f * r;
                }));

        // Noob combo: overcharged plasma pistol (left) strips the shield, BR55 burst (right) to the head
        out.add(AttackDef.of("noob_combo").range(5, 24).cd(160).weight(2).dur(44).weapons("br55", "ppistol_l")
                .tick((e, t, n) -> {
                    if (n < 20) { if (n % 4 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.8f, 1.2f + n * 0.04f); Fx.dustBurst(L(e), PP_C, 1f, mzL(e), 2, 0.25 - n * 0.01); }
                    if (n == 20) { Combat.shoot(e, PP_OVERCHARGE, mzL(e), aim(e, mzL(e), t, 0), 1.0, 6); snd(e, SoundEvents.FIRECHARGE_USE, 1.2f, 1.5f); flash(e, mzL(e), PP_C); }
                    if (n == 30 || n == 31 || n == 32) { bullet(e, t, 0.004, 34, 6f, 0xfff0a0, 1.25f); brass(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), ch = win(t, 0, .45f) * (1 - win(t, .45f, .47f)), rp = 1.4f * kick(t, 20 / 44f, .15f), rb = 0.9f * kick(t, 30 / 44f, .15f);
                    float pp = 1 - win(t, .5f, .6f);
                    // phase 1: left arm out with the pistol, rifle low
                    p.lArmX = lerp(p.lArmX, -HALF - rp * 0.4f, k * pp); p.lArmY = lerp(p.lArmY, -0.2f, k * pp);
                    p.rArmX = lerp(p.rArmX, -0.7f, k * pp); p.rArmY = lerp(p.rArmY, -0.25f, k * pp);
                    p.bodyY += 0.3f * k * pp; p.headY -= 0.3f * k * pp; p.lHandZ += 0.04f * ch * (float) Math.sin(a * 5);
                    // phase 2: snap the BR up
                    float br = win(t, .5f, .62f) * k;
                    shoulder(p, br, 0, rb); p.headX += 0.14f * br;
                    stance(p, k); p.rootZ += rp * 0.5f;
                }));

        // Dual SMGs: alternating spray
        out.add(AttackDef.of("dual_smg").range(2, 14).cd(28).weight(4).dur(30).weapons("smg", "smg_l")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 25) {
                        boolean left = n % 2 == 1; Vec3 m = left ? mzL(e) : mz(e);
                        Combat.hitscan(e, m, aim(e, m, t, 0.07), 16, 1.3f, 0, 0xffe090); flash(e, m, 0xffc060);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.6f, 1.9f + e.getRandom().nextFloat() * 0.1f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .12f, .14f) * (1 - win(t, .84f, .86f));
                    float rr = 0.3f * Math.max(0, (float) Math.sin(a * PI)) * fire, rl = 0.3f * Math.max(0, -(float) Math.sin(a * PI)) * fire;
                    dual(p, k, -0.1f * win(t, .14f, .84f), rr, rl); stance(p, k * 0.6f);
                    p.rHandY += 0.25f * k; p.lHandY -= 0.25f * k; // canted grips
                    p.bodyZ += 0.03f * (float) Math.sin(a * 1.7f) * fire;
                }));

        // M6G magnum: three aimed single shots
        out.add(AttackDef.of("m6g_magnum").range(4, 24).cd(26).weight(3).dur(24).weapon("magnum")
                .tick((e, t, n) -> { if (n == 4 || n == 11 || n == 18) { bullet(e, t, 0.01, 26, 5.5f, 0xffe0a0, 1.05f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.2f, 2f); } })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), r = 1.3f * (kick(t, 4 / 24f, .14f) + kick(t, 11 / 24f, .14f) + kick(t, 18 / 24f, .14f));
                    oneR(p, k, 0, r);
                    p.lArmX = lerp(p.lArmX, -1.3f, k); p.lArmY = lerp(p.lArmY, 0.5f, k); // support hand under the grip
                    p.lLegX = lerp(p.lLegX, 0.25f, k); p.rLegX = lerp(p.rLegX, -0.2f, k); p.headX -= r * 0.05f;
                }));

        // M90 shotgun: blast + pump
        out.add(AttackDef.of("m90_shotgun").range(0, 7).cd(30).weight(4).dur(26).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 5) {
                        Vec3 m = mz(e); for (int i = 0; i < 12; i++) Combat.hitscan(e, m, aim(e, t, 0.11), 12, 2.6f, 0, 0xffd070);
                        flash(e, m, 0xffa040); for (LivingEntity f : Combat.cone(e, 4, 35)) Combat.knock(f, e.position(), 1.0, 0.3);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 0.75f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.7f);
                    }
                    if (n == 13) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.7f);
                    if (n == 17) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.8f); brass(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .18f, .9f), r = 1.9f * kick(t, 5 / 26f, .25f), pump = bump(win(t, .48f, .7f));
                    hip(p, k * 0.35f, 0, 0); shoulder(p, k, 0, r); stance(p, k);
                    p.lArmX += 0.4f * pump * k; p.lArmY -= 0.1f * pump; p.rHandX -= r * 0.4f; p.bodyX -= r * 0.15f;
                    p.headX += 0.06f * pump; p.bodyZ += 0.04f * pump;
                }));

        // SRS99D sniper: kneel, scope, one heavy round, ride the recoil
        out.add(AttackDef.of("srs99_sniper").range(12, 48).cd(90).weight(3).dur(40).weapon("sniper")
                .tick((e, t, n) -> {
                    if (n == 8) snd(e, SoundEvents.SPYGLASS_USE, 1, 0.9f);
                    if (n == 22) {
                        Vec3 d = aim(e, t, 0), m = mz(e);
                        Combat.hitscan(e, m, d, 50, 26f, 2, 0xffffff); Fx.beam(L(e), m, endOf(e, t, d, 50), 0xfff0d0, 0.5f, 0.35); flash(e, m, 0xffc060);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.6f, 0.55f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.3f);
                    }
                    if (n == 30) { snd(e, SoundEvents.SPYGLASS_STOP_USING, 1, 0.9f); brass(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), kn = win(t, 0, .2f) * (1 - win(t, .8f, .95f)), zoom = win(t, .15f, .45f) * (1 - win(t, .7f, .8f));
                    float r = 2.4f * kick(t, 22 / 40f, .25f);
                    shoulder(p, k, 0, r); kneel(p, kn);
                    p.headX += 0.2f * zoom; p.headY -= 0.12f * zoom; p.bodyX += 0.08f * zoom;
                    p.bodyZ += 0.01f * (float) Math.sin(a * 0.4f) * zoom; // breathing hold
                    p.rHandX -= r * 0.35f; p.bodyX -= r * 0.12f;
                }));

        // Spartan Laser: long charge (targeting beam, rising whine), then a devastating red beam
        out.add(AttackDef.of("spartan_laser").range(8, 48).cd(420).weight(2).dur(70).weapon("laser")
                .when((e, t) -> t.getMaxHealth() >= 30)
                .tick((e, t, n) -> {
                    Vec3 m = mz(e);
                    if (n == 2) snd(e, SoundEvents.BEACON_ACTIVATE, 1.2f, 1.2f);
                    if (n >= 6 && n < 52) {
                        if (n % 5 == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.7f, 0.6f + (n - 6) * 0.03f);
                        double r = 0.7 - (n - 6) * 0.013; double ang = n * 0.8;
                        Fx.dustBurst(L(e), LASER_C, 0.9f, m.add(Math.cos(ang) * r, Math.sin(ang) * r, 0), 2, 0.02);
                        if (n % 2 == 0 && t != null) Fx.beam(L(e), m, mid(t), LASER_C, 0.35f, 0.9); // targeting beam
                    }
                    if (n == 52) {
                        Vec3 d = aim(e, t, 0);
                        Combat.hitscan(e, m, d, 50, 45f, 6, LASER_C);
                        Fx.beam(L(e), m, m.add(d.scale(50)), LASER_C, 1.8f, 0.3); Fx.beam(L(e), m, m.add(d.scale(50)), 0xffd0c0, 0.8f, 0.4);
                        Vec3 end = endOf(e, t, d, 30); Combat.blast(e, end, 2.5f, 12, 1f, null); Fx.explosion(L(e), end, 2.5f);
                        e.setDeltaMovement(e.getDeltaMovement().subtract(d.x * 0.3, 0, d.z * 0.3)); e.hurtMarked = true;
                        snd(e, SoundEvents.WARDEN_SONIC_BOOM, 1.4f, 1.5f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.2f);
                    }
                    if (n > 52 && n < 60) Fx.burst(L(e), ParticleTypes.SMOKE, m, 2, 0.05, 0.02);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .08f, .93f), ch = win(t, .08f, .74f) * (1 - win(t, .74f, .75f)), r = 3f * kick(t, 52 / 70f, .25f);
                    shoulder(p, k, 0, r); wide(p, k * 0.7f);
                    p.bodyZ += 0.04f * ch * ch * (float) Math.sin(a * 6); p.rArmY += 0.03f * ch * ch * (float) Math.sin(a * 7.3f);
                    p.headX += 0.15f * ch; p.bodyX += 0.1f * ch - r * 0.15f; p.rootZ += r * 1.1f; p.rHandX -= r * 0.3f;
                    p.rootY -= 0.4f * ch;
                }));

        // M41 SPNKr rocket launcher: two rockets, shoulder-mounted
        out.add(AttackDef.of("m41_rocket").range(6, 32).cd(120).weight(2).dur(34).weapon("rocket")
                .tick((e, t, n) -> {
                    if (n == 8 || n == 18) {
                        Vec3 m = mz(e).add(0, 0.15, 0);
                        Combat.shoot(e, ROCKET, m, aim(e, m, t, 0.012), 1.3, 15f); flash(e, m, 0xffa040);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.7, 0.32, 0.15), 6, 0.2, 0.05);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.6f); snd(e, SoundEvents.GHAST_SHOOT, 0.4f, 1.5f);
                    }
                    if (n == 26) snd(e, SoundEvents.PISTON_CONTRACT, 0.6f, 1.2f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 1.8f * (kick(t, 8 / 34f, .2f) + kick(t, 18 / 34f, .2f));
                    shoulder(p, k, -0.08f, r); stance(p, k);
                    p.rArmX -= 0.25f * k; p.rArmZ -= 0.1f * k; p.rHandX += 0.25f * k; // tube up on the shoulder
                    p.headY -= 0.1f * k; p.headX += 0.05f * k; p.bodyX -= r * 0.1f; p.rootY -= r * 0.3f;
                }));
    }

    // ---------- Covenant / Brute / Forerunner arsenal ----------
    private void covenant(List<AttackDef> out) {
        // Dual Maulers: point-blank alternating blasts
        out.add(AttackDef.of("dual_mauler").range(0, 6).cd(34).weight(3).dur(22).weapons("mauler", "mauler_l")
                .tick((e, t, n) -> {
                    if (n == 5 || n == 12) {
                        boolean left = n == 12; Vec3 m = left ? mzL(e) : mz(e);
                        for (int i = 0; i < 9; i++) Combat.hitscan(e, m, aim(e, m, t, 0.12), 9, 2.4f, 0, 0xffc070);
                        flash(e, m, 0xff9040); for (LivingEntity f : Combat.cone(e, 3.5, 35)) Combat.knock(f, e.position(), 0.7, 0.2);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.1f, 0.6f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.4f);
                    }
                    if (n == 18) snd(e, SoundEvents.CHAIN_PLACE, 0.8f, 0.8f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), rr = 1.8f * kick(t, 5 / 22f, .22f), rl = 1.8f * kick(t, 12 / 22f, .22f);
                    dual(p, k, 0.08f, rr, rl); stance(p, k);
                    p.rHandX -= rr * 0.5f; p.lHandX -= rl * 0.5f; p.bodyY += (rl - rr) * 0.15f; p.headY -= (rl - rr) * 0.08f;
                }));

        // Dual Plasma Rifles: blue bolt streams
        out.add(AttackDef.of("dual_plasma_rifle").range(3, 20).cd(30).weight(4).dur(32).weapons("prifle", "prifle_l")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 26 && n % 2 == 0) {
                        boolean left = n % 4 == 2; Vec3 m = left ? mzL(e) : mz(e);
                        Combat.shoot(e, PLASMA_BOLT, m, aim(e, m, t, 0.035), 1.6, 2.8f); Fx.dustBurst(L(e), PLASMA_C, 0.9f, m, 3, 0.06);
                        snd(e, SoundEvents.BLAZE_SHOOT, 0.5f, 1.7f + e.getRandom().nextFloat() * 0.2f);
                    }
                    if (n == 28) { snd(e, SoundEvents.FIRE_EXTINGUISH, 0.6f, 1.6f); Fx.burst(L(e), ParticleTypes.CLOUD, mz(e), 3, 0.1, 0.02); } // vents
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .1f, .12f) * (1 - win(t, .82f, .84f));
                    float w = (float) Math.sin(a * PI * 0.5f), rr = 0.3f * Math.max(0, w) * fire, rl = 0.3f * Math.max(0, -w) * fire;
                    dual(p, k, -0.05f, rr, rl); stance(p, k);
                    float vent = bump(win(t, .84f, 1)); p.rArmX += 0.4f * vent; p.lArmX += 0.4f * vent; p.headX += 0.1f * vent;
                }));

        // Needler: pink homing crystals, supercombine on 7 hits
        out.add(AttackDef.of("needler").range(4, 22).cd(40).weight(3).dur(30).weapon("needler")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 24 && n % 2 == 0) {
                        Vec3 m = mz(e); Combat.shoot(e, NEEDLE, m, aim(e, m, t, 0.08).add(0, 0.05, 0), 1.1, 2f);
                        Fx.dustBurst(L(e), NEEDLE_C, 0.7f, m, 2, 0.05); snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.4f + e.getRandom().nextFloat() * 0.4f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), fire = win(t, .12f, .14f) * (1 - win(t, .8f, .82f));
                    float r = 0.22f * Math.abs((float) Math.sin(a * PI * 0.5f)) * fire;
                    hip(p, k * 0.3f, 0, 0); shoulder(p, k, -0.04f, r); stance(p, k);
                    p.rHandY += 0.05f * (float) Math.sin(a * 1.9f) * fire; p.bodyY -= 0.05f * fire; // tracking sweep
                }));

        // Brute Shot: two bouncing grenades, then the bayonet flourish
        out.add(AttackDef.of("brute_shot").range(4, 20).cd(70).weight(2).dur(30).weapon("bruteshot")
                .tick((e, t, n) -> {
                    if (n == 6 || n == 14) {
                        Vec3 m = mz(e); Combat.shoot(e, BRUTE_NADE, m, lobDir(e, t, m, 0.05), 1.0, 9f);
                        flash(e, m, 0xff8040); snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.8f); snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.9f, 0.5f);
                    }
                    if (n == 22) snd(e, SoundEvents.CHAIN_PLACE, 1, 0.6f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), r = 1.7f * (kick(t, 6 / 30f, .2f) + kick(t, 14 / 30f, .2f));
                    hip(p, k, -0.25f, r); stance(p, k);
                    p.rootY -= r * 0.4f; p.bodyX -= r * 0.1f;
                    float twirl = bump(win(t, .65f, .88f)); p.rHandY += 1.2f * twirl; p.bodyY += 0.2f * twirl; // flips the blade round
                }));

        // Spiker: hot spikes, high rate, slight arc
        out.add(AttackDef.of("spiker").range(2, 16).cd(30).weight(3).dur(24).weapon("spiker")
                .tick((e, t, n) -> {
                    if (n >= 3 && n <= 19 && n % 2 == 1) {
                        Vec3 m = mz(e); Combat.shoot(e, SPIKE, m, aim(e, m, t, 0.05).add(0, 0.04, 0), 1.5, 3f);
                        Fx.burst(L(e), ParticleTypes.SMALL_FLAME, m, 1, 0.02, 0.01); snd(e, SoundEvents.CROSSBOW_SHOOT, 0.6f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .12f, .14f) * (1 - win(t, .8f, .82f)), r = 0.35f * Math.abs((float) Math.sin(a * PI * 0.5f)) * fire;
                    oneR(p, k, -0.04f, r); p.lArmX = lerp(p.lArmX, -1.0f, k); p.lArmY = lerp(p.lArmY, 0.3f, k); stance(p, k * 0.6f);
                }));

        // Covenant Carbine: precise green bolts
        out.add(AttackDef.of("carbine").range(6, 30).cd(26).weight(3).dur(24).weapon("carbine")
                .tick((e, t, n) -> {
                    if (n == 4 || n == 9 || n == 14 || n == 19) {
                        Vec3 m = mz(e); Combat.hitscan(e, m, aim(e, m, t, 0.008), 32, 4.5f, 0, 0x60ff80);
                        Fx.dustBurst(L(e), 0x60ff80, 1f, m, 4, 0.08); snd(e, SoundEvents.BLAZE_SHOOT, 0.6f, 2f); snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.4f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .88f), r = 0.7f * (kick(t, 4 / 24f, .1f) + kick(t, 9 / 24f, .1f) + kick(t, 14 / 24f, .1f) + kick(t, 19 / 24f, .1f));
                    shoulder(p, k, 0, r); stance(p, k); p.headX += 0.12f * k; p.headY -= 0.06f * k; p.rHandX -= r * 0.2f;
                }));

        // Beam Rifle: scoped Covenant sniper, pink lance
        out.add(AttackDef.of("beam_rifle").range(12, 48).cd(80).weight(2).dur(34).weapon("beamrifle")
                .tick((e, t, n) -> {
                    if (n == 6) snd(e, SoundEvents.SPYGLASS_USE, 1, 1.2f);
                    if (n == 18) {
                        Vec3 d = aim(e, t, 0), m = mz(e);
                        Combat.hitscan(e, m, d, 50, 22f, 1, 0xff70e0); Fx.beam(L(e), m, endOf(e, t, d, 50), 0xffb0f0, 0.8f, 0.3);
                        snd(e, SoundEvents.TRIDENT_THUNDER, 0.5f, 2f); snd(e, SoundEvents.ILLUSIONER_CAST_SPELL, 1, 1.8f);
                    }
                    if (n > 18 && n < 28) Fx.burst(L(e), ParticleTypes.CLOUD, mz(e), 1, 0.05, 0.01); // overheat vent
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), zoom = win(t, .12f, .4f) * (1 - win(t, .65f, .8f)), r = 1.6f * kick(t, 18 / 34f, .2f);
                    shoulder(p, k, 0, r); stance(p, k);
                    p.headX += 0.22f * zoom; p.headY -= 0.14f * zoom; p.bodyX += 0.12f * zoom; p.rootY -= 0.7f * zoom; p.lLegX -= 0.2f * zoom;
                }));

        // Fuel Rod Gun: lobbed green rods
        out.add(AttackDef.of("fuel_rod_gun").range(6, 26).cd(110).weight(2).dur(34).weapon("fuelrod")
                .tick((e, t, n) -> {
                    if (n == 8 || n == 14 || n == 20) {
                        Vec3 m = mz(e); Combat.shoot(e, FUEL_ROD, m, lobDir(e, t, m, 0.03), 1.1, 12f);
                        Fx.dustBurst(L(e), FUEL_C, 1.3f, m, 8, 0.15); snd(e, SoundEvents.FIRECHARGE_USE, 1, 0.7f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.3f, 1.9f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 1.6f * (kick(t, 8 / 34f, .15f) + kick(t, 14 / 34f, .15f) + kick(t, 20 / 34f, .15f));
                    hip(p, k * 0.4f, 0, 0); shoulder(p, k, -0.1f, r); stance(p, k);
                    p.rHandX -= r * 0.35f; p.bodyX -= r * 0.08f; p.rootY -= r * 0.25f;
                }));

        // Plasma pistol (semi-auto taps, then a quick overcharge)
        out.add(AttackDef.of("plasma_pistol").range(3, 20).cd(40).weight(2).dur(34).weapon("ppistol")
                .tick((e, t, n) -> {
                    if (n == 3 || n == 7 || n == 11) { Vec3 m = mz(e); Combat.shoot(e, PP_BOLT, m, aim(e, m, t, 0.02), 1.5, 3f); snd(e, SoundEvents.BLAZE_SHOOT, 0.5f, 1.9f); }
                    if (n > 12 && n < 26) { Fx.dustBurst(L(e), PP_C, 1f, mz(e), 2, 0.2); if (n % 3 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.6f, 1.4f + n * 0.03f); }
                    if (n == 26) { Combat.shoot(e, PP_OVERCHARGE, mz(e), aim(e, t, 0), 1.0, 7f); snd(e, SoundEvents.FIRECHARGE_USE, 1.1f, 1.5f); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), ch = win(t, .38f, .76f) * (1 - win(t, .76f, .78f));
                    float r = 0.8f * (kick(t, 3 / 34f, .08f) + kick(t, 7 / 34f, .08f) + kick(t, 11 / 34f, .08f)) + 1.6f * kick(t, 26 / 34f, .18f);
                    oneR(p, k, 0, r); p.lArmX = lerp(p.lArmX, -1.2f, k * ch); p.lArmY = lerp(p.lArmY, 0.6f, k * ch); // braces the wrist while charging
                    p.rArmZ += 0.03f * ch * (float) Math.sin(a * 6); p.bodyX += 0.08f * ch; p.lLegX = lerp(p.lLegX, -0.3f, k);
                }));
    }

    // ---------- support weapons (heavy, slow movement) ----------
    private void support(List<AttackDef> out) {
        // AIE-486H: rip the machine-gun turret off its mount, then hose from the hip
        out.add(AttackDef.of("turret_rip").range(3, 24).cd(600).weight(1).dur(80).weapon("turret")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 1, 0.7f);
                    if (n == 12) { snd(e, SoundEvents.ANVIL_BREAK, 0.8f, 1.2f); Fx.sparks(L(e), e.position().add(0, 1, 0), 14); }
                    if (n >= 20 && n < 72) {
                        bullet(e, t, 0.06, 26, 2.1f, 0xffe080, 1.2f + e.getRandom().nextFloat() * 0.15f);
                        if (n % 2 == 0) brass(e); if (n % 4 == 0) snd(e, SoundEvents.MINECART_RIDING, 0.5f, 1.8f);
                    }
                    if (n == 72) snd(e, SoundEvents.PISTON_CONTRACT, 0.9f, 0.6f);
                })
                .anim((p, t, a) -> {
                    float rip = win(t, 0, .16f) * (1 - win(t, .16f, .24f));
                    float k = win(t, .16f, .25f) * (1 - win(t, .92f, 1)), fire = win(t, .25f, .26f) * (1 - win(t, .9f, .91f));
                    float jit = 0.05f * (float) Math.sin(a * 6.7f) * fire;
                    // rip: both hands low, haul up and back
                    p.rArmX = lerp(p.rArmX, -0.8f + 0.5f * rip, rip); p.lArmX = lerp(p.lArmX, -0.9f + 0.5f * rip, rip);
                    p.bodyX += 0.4f * rip * (1 - win(t, .1f, .16f)) - 0.25f * bump(win(t, .1f, .22f)); p.rootY -= 1.5f * rip;
                    hip(p, k, -0.1f, 0.25f * fire + jit); wide(p, Math.max(k, rip));
                    p.rHandZ += jit; p.bodyZ += jit * 0.5f; p.headX += 0.08f * k;
                }));

        // Missile Pod: lock and ripple four homing missiles
        out.add(AttackDef.of("missile_pod").range(8, 30).cd(220).weight(1).dur(40).weapon("missilepod")
                .tick((e, t, n) -> {
                    if ((n == 3 || n == 7) && t != null) { snd(e, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f); Fx.ring(L(e), mid(t), 0.8f, 0xff4020, 1f, 14); }
                    if (n == 12 || n == 16 || n == 20 || n == 24) {
                        Vec3 m = mz(e).add(0, 0.1, 0); Combat.shoot(e, MISSILE, m, aim(e, m, t, 0.12).add(0, 0.2, 0).normalize(), 0.9, 8f);
                        flash(e, m, 0xffa040); snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.1f, 0.9f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), r = 1.1f * (kick(t, 12 / 40f, .08f) + kick(t, 16 / 40f, .08f) + kick(t, 20 / 40f, .08f) + kick(t, 24 / 40f, .08f));
                    hip(p, k, -0.35f, r); wide(p, k * 0.6f);
                    p.headX -= 0.1f * k; p.bodyX -= 0.12f * k; p.rootZ += r * 0.5f;
                }));

        // Flamethrower: sweeping stream
        out.add(AttackDef.of("flamethrower").range(0, 7).cd(160).weight(2).dur(50).weapon("flamer")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.FLINTANDSTEEL_USE, 1, 0.8f);
                    if (n >= 8 && n < 42) {
                        Vec3 m = mz(e), d = e.getViewVector(1f);
                        for (int i = 1; i <= 6; i++) Fx.burst(L(e), ParticleTypes.FLAME, m.add(d.scale(i * 1.0)), 3, 0.1 + i * 0.07, 0.02);
                        if (n % 3 == 0) for (LivingEntity f : Combat.cone(e, 7, 25)) { Combat.damage(e, f, 2.2f); f.setSecondsOnFire(5); }
                        if (n % 4 == 0) snd(e, SoundEvents.FIRE_AMBIENT, 1.2f, 0.8f);
                        if (n % 6 == 0) snd(e, SoundEvents.BLAZE_SHOOT, 0.6f, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .9f), fire = win(t, .16f, .18f) * (1 - win(t, .82f, .84f));
                    float sweep = 0.3f * (float) Math.sin(a * 0.35f) * fire;
                    hip(p, k, 0.05f, 0.08f * fire); stance(p, k);
                    p.bodyY += sweep; p.headY -= sweep * 0.5f; p.rArmY += sweep * 0.3f; p.lArmY += sweep * 0.3f;
                    p.rHandZ += 0.02f * (float) Math.sin(a * 5f) * fire;
                }));

        // Sentinel Beam: continuous Forerunner beam
        out.add(AttackDef.of("sentinel_beam").range(3, 16).cd(120).weight(2).dur(44).weapon("sentinel")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.BEACON_ACTIVATE, 0.9f, 1.6f);
                    if (n >= 8 && n < 38) {
                        Vec3 m = mz(e), d = aim(e, t, 0.01);
                        Combat.hitscan(e, m, d, 18, 1.5f, 0, 0xffb030);
                        Fx.beam(L(e), m, endOf(e, t, d, 18), 0xfff060, 0.6f, 0.4);
                        if (n % 5 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.8f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), fire = win(t, .18f, .2f) * (1 - win(t, .86f, .88f));
                    shoulder(p, k, 0, 0.12f * fire); stance(p, k);
                    float wob = 0.04f * (float) Math.sin(a * 4.4f) * fire; p.rArmY += wob; p.lArmY += wob; p.bodyZ += wob * 0.5f;
                    p.headX += 0.08f * fire;
                }));
    }

    // ---------- grenades (left hand, weapon kept in the right) ----------
    private void grenades(List<AttackDef> out) {
        // Frag: classic overhand, bounces
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(140).weight(3).dur(20).weapons("ma5c", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.4f);
                    if (n == 10) { Vec3 h = hand(e); Combat.shoot(e, FRAG, h, lobDir(e, t, h, 0.15), lobSpeed(t, h), 15f); snd(e, SoundEvents.WITCH_THROW, 1, 0.8f); }
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

        // Plasma: prime (glow), flat fast side-arm whip, sticks
        out.add(AttackDef.of("plasma_grenade").range(4, 16).cd(150).weight(3).dur(22).weapons("ma5c", "pnade")
                .tick((e, t, n) -> {
                    if (n < 8 && n % 2 == 0) { Fx.dustBurst(L(e), PLASMA_C, 1f, hand(e), 3, 0.1); }
                    if (n == 2) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.6f, 2f);
                    if (n == 11) { Vec3 h = hand(e); Combat.shoot(e, PLASMA_NADE, h, lobDir(e, t, h, 0.08), lobSpeed(t, h) + 0.15, 2f); snd(e, SoundEvents.WITCH_THROW, 1, 1.1f); }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "pnade" : null;
                    p.rArmX = -0.8f; p.rArmY = -0.3f; p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, 0, .2f, -1.2f, .4f, -1.7f, .5f, -1.4f, .7f, -0.8f, 1, 0);
                    p.lArmY = track(t, 0, 0, .2f, 0.2f, .4f, 1.1f, .5f, -0.7f, .7f, -0.5f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .4f, -0.6f, .5f, -0.3f, 1, 0);
                    p.lHandY = track(t, .1f, 0, .3f, 0.6f, .4f, 0.6f, .5f, -0.4f, 1, 0); // wrist cocked while primed
                    p.bodyY = track(t, 0, 0, .4f, -0.7f, .5f, 0.6f, .75f, 0.4f, 1, 0); p.headY = -p.bodyY * 0.8f;
                    p.bodyZ = track(t, .35f, 0, .45f, 0.1f, .55f, -0.08f, 1, 0);
                    p.lLegX = track(t, 0, 0, .4f, -0.4f, .55f, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, .4f, 0.25f, .55f, 0.45f, 1, 0);
                    p.rootZ = track(t, .4f, 0, .55f, -1.2f, 1, 0); p.rootY = -0.6f * bump(win(t, .3f, .8f));
                }));

        // Spike: heavy lob with a big wind-up, sticks, then sprays shrapnel
        out.add(AttackDef.of("spike_grenade").range(5, 18).cd(160).weight(2).dur(24).weapons("ma5c", "spikenade")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.CHAIN_PLACE, 1, 1.4f);
                    if (n == 12) { Vec3 h = hand(e).add(0, 0.2, 0); Combat.shoot(e, SPIKE_NADE, h, lobDir(e, t, h, 0.2), lobSpeed(t, h), 3f); snd(e, SoundEvents.WITCH_THROW, 1, 0.6f); }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "spikenade" : null;
                    p.rArmX = -0.6f; p.rArmY = -0.35f; p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, 0, .2f, -0.3f, .42f, 1.1f, .5f, -2.8f, .7f, -1.8f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .42f, -0.35f, .5f, 0.15f, 1, 0);
                    p.bodyX = track(t, 0, 0, .42f, -0.35f, .52f, 0.45f, .8f, 0.35f, 1, 0);
                    p.bodyY = track(t, 0, 0, .42f, -0.35f, .52f, 0.25f, 1, 0); p.headY = -p.bodyY * 0.5f; p.headX = -p.bodyX * 0.4f;
                    p.lLegX = track(t, 0, 0, .25f, -0.6f, .52f, -0.7f, 1, 0); p.rLegX = track(t, 0, 0, .42f, 0.4f, .52f, 0.6f, 1, 0);
                    p.rootY = track(t, 0, 0, .42f, -1.0f, .52f, 0.4f, .8f, -0.4f, 1, 0); p.rootZ = track(t, .42f, 0, .55f, -1.8f, 1, 0);
                }));

        // Firebomb: underhand toss, shatters into a fire pool
        out.add(AttackDef.of("firebomb_grenade").range(4, 14).cd(170).weight(2).dur(22).weapons("ma5c", "firebomb")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.FLINTANDSTEEL_USE, 1, 1.2f);
                    if (n < 10) Fx.burst(L(e), ParticleTypes.SMALL_FLAME, hand(e), 1, 0.03, 0.01);
                    if (n == 10) { Vec3 h = hand(e).add(0, -0.4, 0); Combat.shoot(e, FIREBOMB, h, lobDir(e, t, h, 0.3), lobSpeed(t, h) - 0.05, 3f); snd(e, SoundEvents.WITCH_THROW, 1, 0.9f); }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .47f ? "firebomb" : null;
                    p.rArmX = -0.7f; p.rArmY = -0.3f; p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, 0, .3f, 0.8f, .47f, -1.6f, .65f, -1.9f, 1, 0); // underhand: back then up-forward
                    p.lArmZ = track(t, 0, 0, .3f, -0.2f, .47f, 0, 1, 0);
                    p.bodyX = track(t, 0, 0, .3f, 0.35f, .47f, -0.1f, .7f, -0.15f, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, -0.3f, .5f, 0.2f, 1, 0); p.headY = -p.bodyY * 0.6f; p.headX = track(t, .4f, 0, .6f, -0.25f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, .6f, -0.4f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.35f, 1, 0);
                    p.rootY = track(t, 0, 0, .3f, -1.4f, .5f, 0.2f, 1, 0); p.rootZ = track(t, .3f, 0, .5f, -1f, 1, 0);
                }));
    }

    // ---------- equipment ----------
    private void equipment(List<AttackDef> out) {
        // Bubble Shield: slam the device down, golden dome blocks projectiles
        out.add(AttackDef.of("bubble_shield").range(0, 30).cd(700).weight(3).dur(20).weapons("ma5c", "equip")
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.6f || e.getAbsorptionAmount() <= 0)
                .tick((e, t, n) -> {
                    if (n == 9) { deploy(e, BUBBLE, e.position()); snd(e, SoundEvents.BEACON_ACTIVATE, 1.2f, 1.5f); snd(e, SoundEvents.ANVIL_PLACE, 0.4f, 1.8f); Fx.burst(L(e), ParticleTypes.FLASH, e.position().add(0, 1, 0), 1, 0, 0); }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .45f ? "equip" : null;
                    p.rArmX = -0.7f; p.rArmY = -0.3f;
                    p.lArmX = track(t, 0, 0, .25f, -1.8f, .45f, -0.6f, .7f, -0.6f, 1, 0); p.lArmZ = track(t, 0, 0, .25f, -0.3f, .45f, -0.1f, 1, 0);
                    p.bodyX = track(t, 0, 0, .25f, -0.1f, .45f, 0.55f, .75f, 0.45f, 1, 0); p.headX = track(t, .3f, 0, .45f, 0.35f, 1, 0);
                    kneel(p, win(t, .25f, .45f) * (1 - win(t, .8f, 1)));
                }));

        // Power Drain: overhand toss, EMP pulses strip shields and sap
        out.add(AttackDef.of("power_drain").range(3, 14).cd(600).weight(2).dur(20).weapons("ma5c", "equip")
                .when((e, t) -> Combat.around(e, t.position(), 4.5).size() >= 2 || t.getAbsorptionAmount() > 0)
                .tick((e, t, n) -> { if (n == 9) { Vec3 h = hand(e); Combat.shoot(e, DRAIN_DEVICE, h, lobDir(e, t, h, 0.2), lobSpeed(t, h) - 0.1, 0); snd(e, SoundEvents.WITCH_THROW, 1, 1.3f); } })
                .anim((p, t, a) -> {
                    p.weaponL = t < .45f ? "equip" : null;
                    p.rArmX = -0.75f; p.rArmY = -0.3f; p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, 0, .2f, -1.0f, .35f, -0.2f, .45f, -2.0f, .65f, -1.3f, 1, 0); p.lArmY = track(t, 0, 0, .35f, 0.4f, .5f, -0.2f, 1, 0);
                    p.bodyY = track(t, 0, 0, .35f, -0.4f, .5f, 0.35f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.lLegX = track(t, 0, 0, .35f, -0.35f, .5f, -0.45f, 1, 0); p.rLegX = track(t, 0, 0, .5f, 0.35f, 1, 0);
                    p.rootZ = track(t, .35f, 0, .5f, -1.0f, 1, 0);
                }));

        // Regenerator: plant it, healing pulses for allies
        out.add(AttackDef.of("regenerator").range(0, 30).cd(800).weight(3).dur(22).weapons("ma5c", "equip")
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.5f)
                .tick((e, t, n) -> { if (n == 11) { deploy(e, REGEN_FIELD, e.position()); snd(e, SoundEvents.BEACON_ACTIVATE, 1, 1.9f); } })
                .anim((p, t, a) -> {
                    float kn = win(t, .1f, .4f) * (1 - win(t, .75f, 1));
                    p.weaponL = t < .5f ? "equip" : null;
                    p.rArmX = lerp(-0.6f, -0.2f, kn); p.rArmZ = 0.25f * kn; // rifle swung aside
                    p.lArmX = track(t, 0, 0, .3f, -0.9f, .5f, -0.35f, .75f, -0.4f, 1, 0); p.lArmZ = -0.15f * kn;
                    p.bodyX = 0.35f * kn; p.headX = 0.3f * kn - 0.25f * bump(win(t, .55f, .9f)); p.headY = 0.2f * bump(win(t, .55f, .9f)); // scans around
                    kneel(p, kn);
                }));

        // Flare: pop it up and forward, shield the visor
        out.add(AttackDef.of("flare").range(3, 12).cd(600).weight(2).dur(20).weapons("ma5c", "equip")
                .when((e, t) -> Combat.around(e, e.position(), 10).size() >= 2)
                .tick((e, t, n) -> { if (n == 7) { Vec3 h = hand(e).add(0, 0.4, 0); Combat.shoot(e, FLARE, h, aim(e, h, t, 0).add(0, 0.5, 0).normalize(), 0.35, 0); snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1, 1.6f); } })
                .anim((p, t, a) -> {
                    p.weaponL = t < .35f ? "equip" : null;
                    p.rArmX = -0.7f; p.rArmY = -0.3f;
                    p.lArmX = track(t, 0, 0, .2f, -0.6f, .35f, -2.6f, .5f, -2.0f, .62f, -1.9f, 1, 0); // toss, then forearm over the visor
                    p.lArmY = track(t, .35f, 0, .5f, 0.9f, .85f, 0.9f, 1, 0); p.lArmZ = track(t, 0, 0, .35f, -0.2f, .5f, 0, 1, 0);
                    p.headX = track(t, 0, 0, .35f, -0.4f, .5f, 0.3f, .85f, 0.3f, 1, 0); p.headY = track(t, .4f, 0, .55f, 0.3f, .85f, 0.3f, 1, 0);
                    p.bodyY = track(t, .35f, 0, .5f, 0.25f, 1, 0); p.bodyX = track(t, .35f, 0, .5f, 0.15f, 1, 0);
                    p.lLegX = track(t, 0, 0, .45f, 0.35f, .85f, 0.35f, 1, 0); p.rootZ = track(t, .4f, 0, .55f, 0.8f, 1, 0);
                }));

        // Trip Mine: kneel and plant forward
        out.add(AttackDef.of("trip_mine").range(4, 16).cd(700).weight(2).dur(26).weapons("ma5c", "equip")
                .tick((e, t, n) -> { if (n == 13) { deploy(e, TRIP_MINE, ahead(e, 1.4)); snd(e, SoundEvents.TRIPWIRE_ATTACH, 1, 1.2f); snd(e, SoundEvents.STONE_PLACE, 1, 0.8f); } })
                .anim((p, t, a) -> {
                    float kn = win(t, .1f, .35f) * (1 - win(t, .75f, 1));
                    p.weaponL = t < .5f ? "equip" : null;
                    p.rArmX = lerp(-0.6f, -1.0f, kn); p.rArmY = -0.3f;
                    p.lArmX = track(t, 0, 0, .3f, -0.6f, .5f, -1.0f, .6f, -0.9f, 1, 0); p.lArmY = 0.2f * kn;
                    p.bodyX = 0.55f * kn; p.headX = 0.35f * kn; kneel(p, kn);
                    p.rootZ = -0.8f * bump(win(t, .3f, .6f));
                }));

        // Grav Lift: drop it, jump on it
        out.add(AttackDef.of("grav_lift").range(6, 22).cd(800).weight(1).dur(22).weapons("ma5c", "equip")
                .tick((e, t, n) -> {
                    if (n == 5) { deploy(e, GRAV_LIFT, e.position()); snd(e, SoundEvents.BEACON_ACTIVATE, 1, 1.4f); }
                    if (n == 8) { Combat.lunge(e, 0.25, 1.0); buff(e, MobEffects.SLOW_FALLING, 50, 0); }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .25f ? "equip" : null;
                    float air = win(t, .35f, .45f) * (1 - win(t, .85f, 1));
                    p.rArmX = lerp(-0.6f, -1.2f, air); p.lArmX = track(t, 0, 0, .2f, -0.5f, .35f, 0.4f, .5f, -0.6f, .85f, -0.6f, 1, 0);
                    p.lArmZ = -0.5f * air; p.rArmZ = 0.3f * air;
                    p.rootY = track(t, 0, 0, .3f, -1.4f, .38f, 0, 1, 0);
                    p.bodyX = track(t, 0, 0, .3f, 0.3f, .4f, -0.1f, .85f, 0.1f, 1, 0);
                    p.lLegX = -0.6f * air; p.rLegX = 0.3f * air; p.headX = -0.2f * air;
                }));

        // Deployable Cover: plant a shield wall in front and brace behind it
        out.add(AttackDef.of("deployable_cover").range(6, 26).cd(700).weight(2).dur(22).weapons("ma5c", "equip")
                .when((e, t) -> e.getAbsorptionAmount() < SHIELD_MAX / 2f)
                .tick((e, t, n) -> {
                    if (n == 10) {
                        SlayerProjectile c = Combat.shoot(e, COVER, ahead(e, 1.6), new Vec3(0, 1, 0), 0, 0);
                        c.auxVec = Vec3.directionFromRotation(0, e.getYRot());
                        snd(e, SoundEvents.PISTON_EXTEND, 1, 0.7f); snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.2f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .45f ? "equip" : null;
                    float br = win(t, .45f, .6f) * (1 - win(t, .85f, 1));
                    p.lArmX = track(t, 0, 0, .25f, -1.4f, .45f, -0.9f, 1, 0); p.lArmY = track(t, 0, 0, .25f, 0.3f, 1, 0);
                    p.rArmX = -0.7f; p.bodyX = 0.4f * win(t, .2f, .45f) * (1 - win(t, .85f, 1)); p.headX = 0.2f * br;
                    stance(p, br); p.rootY -= 1.2f * br;
                }));

        // Invincibility: crush the canister, golden-white surge
        out.add(AttackDef.of("invincibility").range(0, 30).cd(1200).weight(2).dur(24).weapons("ma5c", "equip")
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.35f)
                .tick((e, t, n) -> {
                    if (n == 12) {
                        buff(e, MobEffects.DAMAGE_RESISTANCE, 160, 4); buff(e, MobEffects.GLOWING, 160, 0);
                        Fx.ring(L(e), e.position().add(0, 0.2, 0), 2.5f, 0xfff4c0, 1.5f, 30); Fx.burst(L(e), ParticleTypes.TOTEM_OF_UNDYING, e.position().add(0, 1, 0), 40, 0.5, 0.4);
                        snd(e, SoundEvents.TOTEM_USE, 0.8f, 1.4f); snd(e, SoundEvents.BEACON_POWER_SELECT, 1, 1.2f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "equip" : null;
                    float surge = win(t, .5f, .6f) * (1 - win(t, .85f, 1));
                    p.lArmX = track(t, 0, 0, .3f, -1.2f, .5f, -1.0f, .6f, -0.3f, 1, 0); p.lArmY = track(t, 0, 0, .3f, 0.6f, .5f, 0.7f, .6f, 0, 1, 0);
                    p.rArmX = lerp(-0.7f, -0.3f, surge); p.rArmZ = 0.5f * surge; p.lArmZ = -0.5f * surge;
                    p.bodyX = track(t, 0, 0, .45f, 0.25f, .6f, -0.25f, 1, 0); p.headX = track(t, 0, 0, .45f, 0.3f, .6f, -0.35f, 1, 0);
                    wide(p, surge); p.rootY += 0.6f * surge;
                }));

        // Overshield: pick it up, triple shields
        out.add(AttackDef.of("overshield").range(0, 30).cd(1100).weight(2).dur(26).weapons("ma5c", "equip")
                .when((e, t) -> e.getAbsorptionAmount() < SHIELD_MAX / 2f)
                .tick((e, t, n) -> {
                    if (n == 14) {
                        e.setAbsorptionAmount(SHIELD_MAX * 2.5f); e.getPersistentData().putInt("h3_delay", 0);
                        Fx.dustBurst(L(e), 0x60ff60, 1.4f, e.position().add(0, 1, 0), 40, 0.7); Fx.ring(L(e), e.position().add(0, 1, 0), 1.2f, 0x60ff60, 1.2f, 24);
                        snd(e, SoundEvents.BEACON_ACTIVATE, 1.2f, 1.6f); snd(e, SoundEvents.ARMOR_EQUIP_DIAMOND, 1, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float kn = win(t, .05f, .35f) * (1 - win(t, .55f, .75f)), charge = win(t, .55f, .65f) * (1 - win(t, .9f, 1));
                    p.weaponL = t > .2f && t < .55f ? "equip" : null;
                    p.lArmX = track(t, 0, 0, .3f, -0.8f, .5f, -0.4f, .55f, -1.2f, .65f, -0.2f, 1, 0); p.rArmX = -0.7f;
                    p.bodyX = 0.5f * kn - 0.2f * charge; p.headX = 0.3f * kn - 0.3f * charge;
                    p.lArmZ = -0.6f * charge; p.rArmZ = 0.4f * charge; kneel(p, kn);
                    p.bodyZ = 0.05f * (float) Math.sin(a * 3) * charge;
                }));

        // Active Camouflage: power-up shimmer
        out.add(AttackDef.of("active_camo").range(4, 30).cd(1200).weight(1).dur(20).weapons("ma5c", "equip")
                .when((e, t) -> Combat.around(e, e.position(), 12).size() >= 3)
                .tick((e, t, n) -> {
                    if (n == 10) {
                        buff(e, MobEffects.INVISIBILITY, 240, 0);
                        Fx.burst(L(e), ParticleTypes.REVERSE_PORTAL, e.position().add(0, 1, 0), 40, 0.4, 0.05);
                        snd(e, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1, 1.2f); snd(e, SoundEvents.ENDERMAN_TELEPORT, 0.5f, 1.8f);
                        if (t instanceof Mob m && m.getTarget() == e) m.setTarget(null);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "equip" : null;
                    float sh = win(t, .5f, .55f) * (1 - win(t, .9f, 1));
                    p.lArmX = track(t, 0, 0, .3f, -1.3f, .5f, -1.1f, .6f, -0.5f, 1, 0); p.lArmY = track(t, 0, 0, .3f, 0.7f, .6f, 0, 1, 0);
                    p.rArmX = -0.75f; p.headX = track(t, 0, 0, .3f, 0.35f, .55f, 0, 1, 0);
                    p.bodyZ = 0.05f * (float) Math.sin(a * 4) * sh; p.headY = 0.1f * (float) Math.sin(a * 2.5f) * sh;
                    stance(p, ready(t, .3f, .9f)); p.rootY -= 0.6f * sh;
                }));
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat armor = Mat.of(0x56683a).bevel(.3f).noise(.08f), suit = Mat.of(0x2b2d2f).noise(.12f), dark = Mat.of(0x1f2420).noise(.1f);
        Mat metal = Mat.of(0x7c8084).noise(.06f), black = Mat.of(0x161616);
        Mat chest = Mat.of(0x56683a).bevel(.32f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            int cx = x + w / 2; // Mark VI chest: centre seam, two pectoral plate lines, small vents
            g.rect(cx, y, 1, h, 0x3a4826);
            g.rect(x + 1, y + h / 2, w / 2 - 2, 1, 0x3a4826); g.rect(cx + 2, y + h / 2, w / 2 - 2, 1, 0x3a4826);
            g.rect(x + 1, y + h - 2, 2, 1, 0x222222); g.rect(x + w - 3, y + h - 2, 2, 1, 0x222222);
        });
        Mat helm = Mat.of(0x56683a).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) { g.rect(x + 1, y + h / 2, w - 3, 1, 0x34401f); g.rect(x + w - 3, y + 2, 1, h - 3, 0x34401f); }
            if (f == Mat.Face.TOP) g.rect(x + w / 2 - 1, y, 2, h, 0x48582f);
        });
        Mat visor = Mat.of(GOLD).glow().decal((m, g, x, y, w, h, f) -> {
            g.rectGlow(x, y, w, 1, 0xfff0b0); // specular highlight band
            if (w > 2) g.rectGlow(x + 1, y + h - 1, w - 2, 1, 0xc07818);
        });
        Mat lights = Mat.of(0x9fffd0).glow();

        // torso
        rig.box(rig.torso, suit, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4.5f, -12.2f, -3, 9, 6, 2, .2f);
        rig.box(rig.torso, armor, -3, -6, -2.7f, 6, 3, 1);
        rig.box(rig.torso, dark, -4.5f, -2.5f, -2.5f, 9, 2, 5);
        rig.box(rig.torso, metal, -1.5f, -2.5f, -3.1f, 3, 2, 1);
        rig.box(rig.torso, armor, -4, -12, 2, 8, 9, 2); // back plate
        rig.box(rig.torso, metal, -3, -10.5f, 3.5f, 6, 5, 1); // power pack
        rig.box(rig.torso, lights, -2.5f, -5f, 3.8f, 1, 1, 1);
        rig.box(rig.torso, dark, -3.5f, -13, -2.5f, 7, 1, 5); // collar
        rig.box(rig.torso, metal, -2.5f, -1.5f, -2.9f, 1, 3, 1); // thigh holster clips
        // helmet (Mark VI: rounded dome, wide wrap visor, chin guard, brow ridge)
        rig.box(rig.head, helm, -4, -8, -4, 8, 8, 8, .45f);
        rig.box(rig.head, visor, -3.5f, -5.6f, -4.9f, 7, 3, 1);
        rig.box(rig.head, visor, -4.9f, -5.3f, -4.2f, 1, 2, 2);
        rig.box(rig.head, visor, 3.9f, -5.3f, -4.2f, 1, 2, 2);
        rig.box(rig.head, armor, -4, -6.6f, -4.95f, 8, 1, 1); // brow
        rig.box(rig.head, dark, -2, -2.6f, -4.9f, 4, 2, 1); // chin/mouth guard
        rig.box(rig.head, armor, -1, -8.9f, -3.5f, 2, 1, 7); // crest
        rig.box(rig.head, metal, -4.9f, -4, -0.5f, 1, 3, 3);
        rig.box(rig.head, metal, 3.9f, -4, -0.5f, 1, 3, 3);
        // arms
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, suit, -2, -2, -2, 4, 12, 4);
            rig.box(arm, armor, s < 0 ? -3.5f : -2.5f, -3, -3, 6, 4, 6, .15f); // shoulder pauldron
            rig.box(arm, armor, s < 0 ? -3.2f : 2.2f, -1, -2.5f, 1, 3, 5); // pauldron skirt
            rig.box(arm, armor, -2, 4.5f, -2, 4, 5, 4, .35f); // gauntlet
            rig.box(arm, metal, -1, 6, -2.8f, 2, 2, 1);
        }
        // legs
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, suit, -2, 0, -2, 4, 12, 4);
            rig.box(leg, armor, -2, 0.5f, -2.8f, 4, 5, 1); // thigh plate
            rig.box(leg, armor, s < 0 ? -2.8f : 1.8f, 0.5f, -2, 1, 4, 4); // hip plate
            rig.box(leg, metal, -1.5f, 5.5f, -2.9f, 3, 2, 1); // knee
            rig.box(leg, armor, -2, 9, -3, 4, 3, 5, .25f); // boot
        }
        buildWeapons(rig, metal, dark, black);
    }

    private static void buildWeapons(Rig rig, Mat metal, Mat dark, Mat black) {
        Mat gun = Mat.of(0x3c4038).noise(.08f), odg = Mat.of(0x4a5236).noise(.08f), cyanG = Mat.of(0x60e0ff).glow();
        Mat cov = Mat.of(0x5a3a7a).bevel(.35f), covT = Mat.of(0x3a6a5a).bevel(.35f), brute = Mat.of(0x5a4636).noise(.15f), white = Mat.of(0xd0d4d8);
        Mat blue = Mat.of(PLASMA_C).glow(), green = Mat.of(FUEL_C).glow(), pink = Mat.of(NEEDLE_C).glow(), red = Mat.of(LASER_C).glow();
        Mat sword = Mat.of(SWORD_C).glow(), hammerG = Mat.of(HAMMER_C).glow(), orange = Mat.of(0xff9020).glow(), equip = Mat.of(0x34383c).bevel(.3f);
        PartDefinition w;
        w = rig.weapon(rig.rHand, "ma5c");
        rig.box(w, odg, -1.5f, -3.5f, -8, 3, 4, 10); rig.box(w, gun, -1, -3, -13, 2, 2, 5); rig.box(w, black, -1, 0, -6, 2, 4, 2);
        rig.box(w, black, -1, -2.5f, 2, 2, 3, 4); rig.box(w, cyanG, -0.5f, -4.2f, -3, 1, 1, 1); rig.box(w, gun, -0.5f, -4.5f, -7, 1, 1, 4);
        w = rig.weapon(rig.rHand, "br55");
        rig.box(w, odg, -1, -3, -10, 2, 3, 12); rig.box(w, black, -1, -5.2f, -6, 2, 2, 5); rig.box(w, gun, -0.5f, -2.5f, -14, 1, 1, 4);
        rig.box(w, black, -1, 0, -3, 2, 3, 2); rig.box(w, gun, -1, -2.5f, 2, 2, 3, 3);
        w = rig.weapon(rig.rHand, "smg");
        rig.box(w, gun, -1, -3, -5, 2, 3, 6); rig.box(w, black, -0.5f, 0, -4, 1, 4, 1); rig.box(w, metal, -0.5f, -2.5f, -8, 1, 1, 3);
        w = rig.weapon(rig.lHand, "smg_l");
        rig.box(w, gun, -1, -3, -5, 2, 3, 6); rig.box(w, black, -0.5f, 0, -4, 1, 4, 1); rig.box(w, metal, -0.5f, -2.5f, -8, 1, 1, 3);
        w = rig.weapon(rig.rHand, "magnum");
        rig.box(w, metal, -1, -2.5f, -6, 2, 2, 6); rig.box(w, black, -1, -0.5f, -1, 2, 3, 2); rig.box(w, black, -0.5f, -3.4f, -4, 1, 1, 2);
        w = rig.weapon(rig.rHand, "shotgun");
        rig.box(w, gun, -1.5f, -3, -9, 3, 3, 10); rig.box(w, dark, -1, -1.5f, -14, 2, 2, 7); rig.box(w, metal, -1, -3, -15, 2, 1, 6);
        rig.box(w, black, -1, -2, 1, 2, 3, 4);
        w = rig.weapon(rig.rHand, "sniper");
        rig.box(w, gun, -1, -3, -10, 2, 3, 13); rig.box(w, metal, -0.5f, -2.5f, -22, 1, 1, 12); rig.box(w, black, -1, -5.5f, -7, 2, 2, 6);
        rig.box(w, gun, -1, -2.5f, 3, 2, 4, 4); rig.box(w, black, -1, 0, -4, 2, 3, 2);
        w = rig.weapon(rig.rHand, "laser");
        rig.box(w, white, -2, -4.5f, -12, 4, 5, 14); rig.box(w, red, -0.5f, -5.2f, -11, 1, 1, 10); rig.box(w, red, -1.5f, -4, -12.6f, 3, 3, 1);
        rig.box(w, gun, -1, -0.5f, -2, 2, 3, 2); rig.box(w, gun, -1, -6.5f, -6, 2, 2, 3);
        w = rig.weapon(rig.rHand, "rocket");
        rig.box(w, odg, -2.5f, -5, -12, 3, 3, 16); rig.box(w, odg, 0.5f, -5, -12, 3, 3, 16); rig.box(w, black, -0.5f, -2, -3, 1, 3, 2);
        rig.box(w, gun, -2, -7, -6, 2, 2, 4);
        w = rig.weapon(rig.rHand, "hammer");
        rig.box(w, dark, -0.5f, -0.5f, -16, 1, 1, 22); rig.box(w, brute, -3, -3, -20, 6, 5, 4); rig.box(w, hammerG, -2, -2.5f, -20.8f, 4, 4, 1);
        rig.box(w, metal, -1, 2, -19, 2, 3, 2); rig.box(w, brute, -1, -1, 5, 2, 2, 2);
        w = rig.weapon(rig.rHand, "sword");
        rig.box(w, cov, -1, -1, -2, 2, 2, 4); rig.box(w, sword, -1.6f, -1.5f, -13, 1, 1, 11); rig.box(w, sword, 0.6f, -1.5f, -13, 1, 1, 11);
        rig.box(w, sword, -0.5f, -1.5f, -15, 1, 1, 3);
        w = rig.weapon(rig.rHand, "mauler");
        rig.box(w, brute, -1.5f, -3.5f, -6, 3, 4, 6); rig.box(w, metal, -1, -3, -9, 2, 2, 3); rig.box(w, metal, -0.5f, 0.5f, -8, 1, 2, 6);
        w = rig.weapon(rig.lHand, "mauler_l");
        rig.box(w, brute, -1.5f, -3.5f, -6, 3, 4, 6); rig.box(w, metal, -1, -3, -9, 2, 2, 3); rig.box(w, metal, -0.5f, 0.5f, -8, 1, 2, 6);
        w = rig.weapon(rig.rHand, "prifle");
        rig.box(w, cov, -1.5f, -3, -7, 3, 3, 8); rig.box(w, cov, -1.5f, -3, -10, 1, 2, 3); rig.box(w, cov, 0.5f, -3, -10, 1, 2, 3); rig.box(w, blue, -0.5f, -3.6f, -5, 1, 1, 3);
        w = rig.weapon(rig.lHand, "prifle_l");
        rig.box(w, cov, -1.5f, -3, -7, 3, 3, 8); rig.box(w, cov, -1.5f, -3, -10, 1, 2, 3); rig.box(w, cov, 0.5f, -3, -10, 1, 2, 3); rig.box(w, blue, -0.5f, -3.6f, -5, 1, 1, 3);
        w = rig.weapon(rig.rHand, "ppistol");
        rig.box(w, covT, -1, -2.5f, -5, 2, 3, 5); rig.box(w, green, -0.5f, -2, -6, 1, 1, 1); rig.box(w, covT, -1, 0.5f, -2, 2, 2, 2);
        w = rig.weapon(rig.lHand, "ppistol_l");
        rig.box(w, covT, -1, -2.5f, -5, 2, 3, 5); rig.box(w, green, -0.5f, -2, -6, 1, 1, 1); rig.box(w, covT, -1, 0.5f, -2, 2, 2, 2);
        w = rig.weapon(rig.rHand, "needler");
        rig.box(w, cov, -1.5f, -3, -7, 3, 3, 8); rig.box(w, pink, -1, -4.5f, -6, 1, 2, 1); rig.box(w, pink, 0, -4.5f, -4, 1, 2, 1); rig.box(w, pink, -1, -4.5f, -2, 1, 2, 1);
        w = rig.weapon(rig.rHand, "bruteshot");
        rig.box(w, brute, -1.5f, -3.5f, -9, 3, 4, 11); rig.box(w, metal, -0.5f, 0.5f, -12, 1, 2, 7); rig.box(w, dark, -1.5f, -2, -4, 3, 3, 3);
        w = rig.weapon(rig.rHand, "spiker");
        rig.box(w, brute, -1, -3, -6, 2, 3, 7); rig.box(w, metal, -0.5f, -2.5f, -10, 1, 1, 4); rig.box(w, metal, -0.5f, 0, -7, 1, 1, 5); rig.box(w, orange, -0.5f, -3.6f, -4, 1, 1, 2);
        w = rig.weapon(rig.rHand, "carbine");
        rig.box(w, covT, -1, -3, -11, 2, 3, 14); rig.box(w, green, -0.5f, -3.6f, -8, 1, 1, 6); rig.box(w, covT, -1, 0, -3, 2, 2, 2);
        w = rig.weapon(rig.rHand, "beamrifle");
        rig.box(w, white, -1, -3, -14, 2, 2, 17); rig.box(w, pink, -0.5f, -3.6f, -12, 1, 1, 8); rig.box(w, cov, -1, -1, -2, 2, 3, 3);
        w = rig.weapon(rig.rHand, "fuelrod");
        rig.box(w, cov, -2, -4, -10, 4, 4, 12); rig.box(w, green, -1, -4.6f, -8, 2, 1, 6); rig.box(w, green, -1.5f, -3.5f, -10.6f, 3, 3, 1);
        w = rig.weapon(rig.rHand, "turret");
        rig.box(w, gun, -2, -4, -8, 4, 4, 10); rig.box(w, metal, -1, -3, -17, 2, 2, 9); rig.box(w, dark, -1.5f, -6, -6, 3, 2, 3); rig.box(w, black, -1, 0, -5, 2, 3, 3);
        w = rig.weapon(rig.rHand, "missilepod");
        rig.box(w, odg, -2.5f, -5, -7, 5, 5, 8); rig.box(w, black, -2, -4.5f, -7.4f, 4, 4, 1); rig.box(w, gun, -1, 0, -3, 2, 3, 2);
        w = rig.weapon(rig.rHand, "flamer");
        rig.box(w, gun, -1.5f, -3, -6, 3, 3, 8); rig.box(w, metal, -0.5f, -2.5f, -13, 1, 1, 7); rig.box(w, Mat.of(0x8a2a1a), -1.5f, 0, -5, 3, 3, 4); rig.box(w, orange, -0.5f, -2.5f, -13.5f, 1, 1, 1);
        w = rig.weapon(rig.rHand, "sentinel");
        rig.box(w, white, -1.5f, -3.5f, -10, 3, 3, 12); rig.box(w, orange, -0.5f, -4.2f, -9, 1, 1, 6); rig.box(w, metal, -1, -3, -12, 2, 2, 2);
        // grenades / equipment (left hand)
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, Mat.of(0x4f5f30), -1, -1, -2, 2, 2, 2); rig.box(w, metal, -0.5f, -1.8f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "pnade");
        rig.box(w, blue, -1, -1, -2, 2, 2, 2); rig.box(w, cov, -1.5f, -0.5f, -1.5f, 3, 1, 1);
        w = rig.weapon(rig.lHand, "spikenade");
        rig.box(w, brute, -1, -1, -2, 2, 2, 2); rig.box(w, metal, -0.5f, -2.5f, -1.5f, 1, 2, 1);
        w = rig.weapon(rig.lHand, "firebomb");
        rig.box(w, Mat.of(0x6a3a2a), -1, -1.5f, -2, 2, 3, 2); rig.box(w, orange, -0.5f, -2.3f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "equip");
        rig.box(w, equip, -1.5f, -2, -2.5f, 3, 3, 3); rig.box(w, Mat.of(0xffe080).glow(), -0.5f, -2.6f, -1.5f, 1, 1, 1);
    }
}
