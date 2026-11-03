package com.doomslayer.fx;

import com.doomslayer.DoomSlayerMod;
import com.doomslayer.entity.ProjStyle;
import com.doomslayer.entity.SlayerEntity;
import com.doomslayer.entity.SlayerProjectile;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Server-side combat toolbox for attack ticks. Everything here only ever hurts "foes" (hostile mobs, never players/slayers/pets/villagers). */
public final class Combat {
    private Combat() {}

    public static boolean isFoe(SlayerEntity s, LivingEntity t) {
        if (t == null || !t.isAlive() || t instanceof Player || t instanceof SlayerEntity) return false;
        if (t instanceof TamableAnimal ta && ta.isTame()) return false;
        if (t == s.getTarget() || t == s.atkTarget || t instanceof Enemy) return true;
        return t instanceof Mob m && (m.getTarget() instanceof Player || m.getTarget() instanceof SlayerEntity);
    }

    public static boolean damage(SlayerEntity s, LivingEntity t, float amount) {
        if (!isFoe(s, t)) return false;
        t.invulnerableTime = 0;
        return t.hurt(s.damageSources().mobAttack(s), amount * s.kit.dmgMult);
    }
    public static void knock(Entity t, Vec3 from, double strength, double up) {
        Vec3 d = t.position().subtract(from); d = new Vec3(d.x, 0, d.z);
        d = d.lengthSqr() < 1e-4 ? Vec3.ZERO : d.normalize();
        t.setDeltaMovement(t.getDeltaMovement().add(d.x * strength, up, d.z * strength)); t.hurtMarked = true;
    }
    public static List<LivingEntity> foes(SlayerEntity s, AABB box) {
        return s.level().getEntitiesOfClass(LivingEntity.class, box, e -> isFoe(s, e));
    }
    public static List<LivingEntity> around(SlayerEntity s, Vec3 c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : foes(s, new AABB(c, c).inflate(r))) if (e.position().add(0, e.getBbHeight() / 2, 0).distanceTo(c) <= r + e.getBbWidth() / 2) out.add(e);
        return out;
    }
    /** foes inside a forward cone (halfAngleDeg) up to range, measured from the slayer's eyes along its facing. */
    public static List<LivingEntity> cone(SlayerEntity s, double range, double halfAngleDeg) {
        Vec3 eye = s.getEyePosition(), look = s.getViewVector(1f); List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : foes(s, s.getBoundingBox().inflate(range))) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
            double d = to.length(); if (d > range + e.getBbWidth() / 2 || d < 1e-3) continue;
            if (Math.toDegrees(Math.acos(Mth.clamp(to.normalize().dot(look), -1, 1))) <= halfAngleDeg) out.add(e);
        }
        return out;
    }
    /** Explosion with falloff, knockback, particles + sound but NO block damage and no harm to non-foes. */
    public static void blast(SlayerEntity s, Vec3 c, float radius, float dmg, float kb, @Nullable Entity skip) {
        for (LivingEntity e : around(s, c, radius)) {
            if (e == skip) continue;
            double f = 1 - Math.min(1, e.position().add(0, e.getBbHeight() / 2, 0).distanceTo(c) / (radius + 0.01));
            damage(s, e, (float) (dmg * (0.4 + 0.6 * f))); knock(e, c.add(0, -0.3, 0), kb * (0.4 + f), 0.25 + 0.3 * f);
        }
    }

    // ---- aiming / shooting ----
    /** point in front of the slayer at the gun: fwd along facing, side (+ = right), up */
    public static Vec3 muzzle(SlayerEntity s, double fwd, double side, double up) {
        Vec3 look = s.getViewVector(1f), right = look.cross(new Vec3(0, 1, 0)).normalize();
        return s.getEyePosition().add(look.scale(fwd)).add(right.scale(side)).add(0, up - 0.2, 0);
    }
    public static Vec3 dirTo(Vec3 from, LivingEntity t, double spread, java.util.Random r) {
        Vec3 d = t.position().add(0, t.getBbHeight() * 0.55, 0).subtract(from).normalize();
        return d.add(r.nextGaussian() * spread, r.nextGaussian() * spread, r.nextGaussian() * spread).normalize();
    }
    public static Vec3 dirTo(SlayerEntity s, LivingEntity t, double spread) { return dirTo(muzzle(s, 0.8, -0.3, 0), t, spread, new java.util.Random(s.getRandom().nextLong())); }

    /** Instant bullet. Hits the first foe along the ray (pierce = how many extra foes). Draws a tracer. Returns number of foes hit. */
    public static int hitscan(SlayerEntity s, Vec3 from, Vec3 dir, double range, float dmg, int pierce, int tracerRgb) {
        ServerLevel l = (ServerLevel) s.level();
        Vec3 end = from.add(dir.scale(range));
        BlockHitResult bh = l.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, s));
        if (bh.getType() != HitResult.Type.MISS) end = bh.getLocation();
        int hits = 0; Vec3 cur = from; Entity ignore = null;
        for (int i = 0; i <= pierce; i++) {
            final Entity ig = ignore; final Vec3 curF = cur;
            EntityHitResult eh = ProjectileUtil.getEntityHitResult(l, s, curF, end, new AABB(curF, end).inflate(1), e -> e instanceof LivingEntity le && e != ig && isFoe(s, le));
            if (eh == null) break;
            damage(s, (LivingEntity) eh.getEntity(), dmg); hits++;
            Fx.sparks(l, eh.getLocation(), 4); ignore = eh.getEntity(); cur = eh.getLocation();
        }
        Fx.beam(l, from, end, tracerRgb, 0.5f, 0.6);
        if (hits == 0 && bh.getType() != HitResult.Type.MISS) Fx.burst(l, ParticleTypes.SMOKE, end, 3, 0.05, 0.01);
        return hits;
    }
    public static SlayerProjectile shoot(SlayerEntity s, ProjStyle style, Vec3 from, Vec3 dir, double speed, float dmg) {
        SlayerProjectile p = new SlayerProjectile(DoomSlayerMod.PROJECTILE.get(), s.level());
        p.setOwner(s); p.setStyle(style); p.damage = dmg;
        p.setPos(from); p.setDeltaMovement(dir.normalize().scale(speed));
        s.level().addFreshEntity(p);
        return p;
    }
    public static void sound(SlayerEntity s, net.minecraft.sounds.SoundEvent ev, float vol, float pitch) { Fx.sound((ServerLevel) s.level(), s.position(), ev, vol, pitch); }
    /** dash/lunge along facing */
    public static void lunge(SlayerEntity s, double speed, double up) {
        Vec3 f = s.getViewVector(1f); s.setDeltaMovement(f.x * speed, up, f.z * speed); s.hurtMarked = true;
    }
    public static void gunSound(SlayerEntity s, float pitch) { sound(s, SoundEvents.FIREWORK_ROCKET_BLAST, 1.2f, pitch); }
}
