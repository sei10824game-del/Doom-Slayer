package com.doomslayer.entity;

import com.doomslayer.fx.Combat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;

public class SlayerProjectile extends Projectile {
    private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(SlayerProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TINT = SynchedEntityData.defineId(SlayerProjectile.class, EntityDataSerializers.INT), LIFE = SynchedEntityData.defineId(SlayerProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RAD = SynchedEntityData.defineId(SlayerProjectile.class, EntityDataSerializers.FLOAT);
    public float radius() { return entityData.get(RAD); }
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(SlayerProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<org.joml.Vector3f> END = SynchedEntityData.defineId(SlayerProjectile.class, EntityDataSerializers.VECTOR3);
    public int tint() { return entityData.get(TINT); } public float sizeOv() { return entityData.get(SIZE); } public int life() { return entityData.get(LIFE); }
    public Vec3 end() { var v = entityData.get(END); return new Vec3(v.x, v.y, v.z); }
    /** spawn a purely visual effect entity (custom-rendered tracer / beam / flash / blast / ring) */
    public static void visual(net.minecraft.server.level.ServerLevel l, ProjStyle st, Vec3 pos, Vec3 vel, int tint, float size, int life, Vec3 end) { visual(l, st, pos, vel, tint, size, life, end, 1f); }
    public static void visual(net.minecraft.server.level.ServerLevel l, ProjStyle st, Vec3 pos, Vec3 vel, int tint, float size, int life, Vec3 end, float radius) {
        SlayerProjectile p = new SlayerProjectile(com.doomslayer.DoomSlayerMod.PROJECTILE.get(), l);
        p.setStyle(st); p.entityData.set(TINT, tint); p.entityData.set(SIZE, size); p.entityData.set(LIFE, life); p.entityData.set(RAD, radius);
        if (end != null) p.entityData.set(END, new org.joml.Vector3f((float) end.x, (float) end.y, (float) end.z));
        p.setPos(pos); p.setDeltaMovement(vel); p.setNoGravity(true); p.noPhysics = true;
        l.addFreshEntity(p);
    }
    public float damage = 6; public int age2, pierceLeft;
    private final java.util.Set<Integer> hitIds = new java.util.HashSet<>();
    /** free scratch values for ProjStyle hooks */
    public int aux; public Vec3 auxVec;
    /** CLIENT: recent positions for the ribbon trail (oldest first) */
    public final java.util.ArrayList<Vec3> hist = new java.util.ArrayList<>();

    public SlayerProjectile(EntityType<? extends SlayerProjectile> t, Level l) { super(t, l); noPhysics = false; }

    public ProjStyle style() { return ProjStyle.ALL.get(Math.min(entityData.get(STYLE), ProjStyle.ALL.size() - 1)); }
    public void setStyle(ProjStyle s) { entityData.set(STYLE, s.id); pierceLeft = s.pierce; }
    @Override protected void defineSynchedData() { entityData.define(STYLE, 0); entityData.define(TINT, 0); entityData.define(SIZE, 0f); entityData.define(RAD, 1f); entityData.define(LIFE, 4); entityData.define(END, new org.joml.Vector3f()); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putInt("S", entityData.get(STYLE)); }
    @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); entityData.set(STYLE, t.getInt("S")); }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!(e instanceof LivingEntity le) || !(getOwner() instanceof SlayerEntity o)) return false;
        return !hitIds.contains(e.getId()) && Combat.isFoe(o, le) && super.canHitEntity(e);
    }

    @Override
    public void tick() {
        super.tick();
        ProjStyle s = style();
        if (s.visual) { // effect carrier: just drift, record trail, expire
            if (level().isClientSide) { hist.add(position()); if (hist.size() > 14) hist.remove(0); }
            else if (++age2 >= entityData.get(LIFE)) { discard(); return; }
            if (s.resolved() == ProjStyle.Shape.BOLT) setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);
            return;
        }
        Vec3 mv = getDeltaMovement();
        if (!level().isClientSide && s.homing > 0 && getOwner() instanceof SlayerEntity o) {
            LivingEntity best = null; double bd = 144;
            for (LivingEntity t : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(12), x -> Combat.isFoe(o, x))) {
                double d = t.distanceToSqr(this); if (d < bd) { bd = d; best = t; }
            }
            if (best != null) {
                Vec3 want = best.position().add(0, best.getBbHeight() * 0.5, 0).subtract(position()).normalize().scale(mv.length());
                mv = mv.scale(1 - s.homing).add(want.scale(s.homing));
            }
        }
        mv = mv.scale(s.drag).add(0, -s.gravity, 0);
        setDeltaMovement(mv);
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (!level().isClientSide) {
            if (hit.getType() == HitResult.Type.ENTITY) {
                Entity e = ((EntityHitResult) hit).getEntity(); hitIds.add(e.getId());
                if (getOwner() instanceof SlayerEntity o && e instanceof LivingEntity le) Combat.damage(o, le, damage);
                impact(hit.getLocation(), e);
                if (pierceLeft-- > 0) { /* continue */ } else { discard(); return; }
            } else if (hit.getType() == HitResult.Type.BLOCK) {
                if (s.bounce) {
                    BlockHitResult b = (BlockHitResult) hit;
                    Vec3 n = new Vec3(b.getDirection().getStepX(), b.getDirection().getStepY(), b.getDirection().getStepZ());
                    mv = mv.subtract(n.scale(1.7 * mv.dot(n))).scale(0.6); setDeltaMovement(mv);
                    setPos(hit.getLocation().add(n.scale(0.1)));
                    if (mv.lengthSqr() < 0.002) { impact(position(), null); discard(); return; }
                } else { impact(hit.getLocation(), null); discard(); return; }
            }
            s.onTick.run(this);
            if (isRemoved()) return;
            if (++age2 >= s.life) { impact(position(), null); discard(); return; }
        } else {
            hist.add(position()); if (hist.size() > 14) hist.remove(0);
        }
        setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);
    }

    private void impact(Vec3 pos, Entity hit) {
        ProjStyle s = style();
        if (s.onHit != null) s.onHit.run(this, pos, hit); else ProjStyle.defaultImpact(this, pos, hit);
        if (s.explodeRadius > 0 && getOwner() instanceof SlayerEntity o && level() instanceof ServerLevel)
            Combat.blast(o, pos, s.explodeRadius, damage, s.explodeKb, hit);
    }
}
