package com.doomslayer.entity;

import com.doomslayer.Rules;
import com.doomslayer.kit.AttackDef;
import com.doomslayer.kit.SlayerKit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Player-aligned slayer. Never hurts / is hurt by players or other slayers. Hunts Enemy mobs, follows owner. */
public class SlayerEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> ATK = SynchedEntityData.defineId(SlayerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(SlayerEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> STAY = SynchedEntityData.defineId(SlayerEntity.class, EntityDataSerializers.BOOLEAN);

    public final SlayerKit kit;
    private int[] cds;
    private int serverTick, gcd, clientAnim, lastAtk = -1;

    public SlayerEntity(EntityType<? extends SlayerEntity> type, Level level, SlayerKit kit) {
        super(type, level);
        this.kit = kit; this.cds = new int[kit.attacks().size()];
        this.xpReward = 0; setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attrs(SlayerKit k) {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, k.maxHealth).add(Attributes.MOVEMENT_SPEED, k.speed)
                .add(Attributes.ARMOR, k.armor).add(Attributes.KNOCKBACK_RESISTANCE, k.knockbackRes)
                .add(Attributes.FOLLOW_RANGE, k.followRange).add(Attributes.ATTACK_DAMAGE, 6);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ATK, -1); entityData.define(OWNER, Optional.empty()); entityData.define(STAY, false);
    }

    // ---------- attack state (synced) ----------
    @Nullable public AttackDef currentAttack() { int i = entityData.get(ATK); return i < 0 ? null : kit.attacks().get(i); }
    /** client: ticks since the current attack began */
    public int animTick() { return level().isClientSide ? clientAnim : serverTick; }
    public int attackTicks() { return serverTick; }
    @Nullable public LivingEntity atkTarget;
    private int lastGuardFx = -100, lastHitFx = -100, airTicks;

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            int a = entityData.get(ATK);
            if (a != lastAtk) { clientAnim = 0; lastAtk = a; } else if (a >= 0) clientAnim++;
            return;
        }
        for (int i = 0; i < cds.length; i++) if (cds[i] > 0) cds[i]--;
        if (gcd > 0) gcd--;
        kit.passive(this);
        if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
            var res = getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);
            if (res != null && res.getAmplifier() >= 3 && tickCount % 7 == 0) com.doomslayer.fx.Fx.bodyShield(sl, position(), kit.eggSecondary, getBbHeight() * 1.08f, getBbWidth() * 1.1f); // invulnerable / guard stance shimmer
            double sp = getDeltaMovement().horizontalDistance();
            if (onGround()) {
                if (airTicks > 8) { // landing thud
                    com.doomslayer.fx.Fx.ring(sl, position().add(0, 0.05, 0), 1.2f * kit.scale, 0xd8d0c0, 1.2f, 16);
                    com.doomslayer.fx.Fx.burst(sl, net.minecraft.core.particles.ParticleTypes.CLOUD, position().add(0, 0.1, 0), 6, 0.35, 0.02);
                }
                airTicks = 0;
                if (sp > 0.1 && sp <= 0.55 && tickCount % 7 == 0) com.doomslayer.fx.Fx.burst(sl, net.minecraft.core.particles.ParticleTypes.CLOUD, position().add(0, 0.08, 0), 1, 0.08, 0.005); // footstep dust
            } else airTicks++;
            if (sp > 0.55 && tickCount % 2 == 0) com.doomslayer.fx.Fx.burst(sl, net.minecraft.core.particles.ParticleTypes.CLOUD, position().add(0, 0.15, 0), 2, 0.15, 0.01); // dash / dodge dust
        }
        AttackDef d = currentAttack();
        if (d != null) {
            LivingEntity t = atkTarget != null && atkTarget.isAlive() ? atkTarget : getTarget();
            if (t != null && d.faceTarget) {
                getLookControl().setLookAt(t, 60, 60);
                double dx = t.getX() - getX(), dz = t.getZ() - getZ();
                float yaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90f;
                setYRot(yaw); yBodyRot = yaw; yHeadRot = yaw;
            }
            if (!d.canMove) getNavigation().stop();
            d.tick.run(this, t, serverTick);
            com.doomslayer.kit.kits.MeleeFix.trail(this, d, serverTick);
            if (++serverTick >= d.duration) endAttack(d, t);
        }
    }

    private void endAttack(AttackDef d, @Nullable LivingEntity t) {
        d.end.run(this, t, serverTick);
        entityData.set(ATK, -1); serverTick = 0; gcd = 6;
        cds[d.index] = d.cooldown;
    }
    public void interruptAttack() { AttackDef d = currentAttack(); if (d != null) { d.end.run(this, atkTarget, serverTick); entityData.set(ATK, -1); serverTick = 0; cds[d.index] = d.cooldown; } }

    public boolean isAttacking() { return entityData.get(ATK) >= 0; }
    public boolean tryStart(AttackDef d, LivingEntity t) {
        if (isAttacking() || gcd > 0 || cds[d.index] > 0) return false;
        atkTarget = t; serverTick = 0; entityData.set(ATK, d.index);
        d.start.run(this, t, 0);
        return true;
    }

    /** weighted random choice among attacks usable right now */
    @Nullable public AttackDef pickAttack(LivingEntity t) {
        double dist = distanceTo(t);
        List<AttackDef> ok = new ArrayList<>(); int total = 0;
        for (AttackDef d : kit.attacks()) {
            if (cds[d.index] > 0 || dist < d.minRange || dist > d.maxRange) continue;
            if (t.getHealth() / t.getMaxHealth() > d.maxHealthFrac) continue;
            if (d.needsLos && !hasLineOfSight(t)) continue;
            if (!d.cond.test(this, t)) continue;
            ok.add(d); total += d.weight;
        }
        if (ok.isEmpty()) return null;
        int r = random.nextInt(total);
        for (AttackDef d : ok) { r -= d.weight; if (r < 0) return d; }
        return ok.get(0);
    }

    // ---------- ownership / faction ----------
    @Nullable public UUID getOwnerId() { return entityData.get(OWNER).orElse(null); }
    public void setOwner(@Nullable UUID id) { entityData.set(OWNER, Optional.ofNullable(id)); }
    @Nullable public Player getOwner() { UUID id = getOwnerId(); return id == null ? null : level().getPlayerByUUID(id); }
    public boolean isStaying() { return entityData.get(STAY); }

    /** slayer-vs-slayer hostility, controlled by Rules (rivals / friendlyFire) */
    public static boolean rival(SlayerEntity a, SlayerEntity b) {
        if (a == b) return false;
        boolean same = a.getOwnerId() != null && a.getOwnerId().equals(b.getOwnerId());
        return same ? Rules.FRIENDLY_FIRE.get() : Rules.RIVALS.get();
    }

    @Override public boolean isAlliedTo(Entity o) { return o instanceof Player || (o instanceof SlayerEntity s && !rival(this, s)) || super.isAlliedTo(o); }
    @Override public boolean canAttack(LivingEntity t) { return t != null && !(t instanceof Player) && (!(t instanceof SlayerEntity s) || rival(this, s)) && super.canAttack(t); }

    @Override
    public void die(DamageSource src) {
        if (!level().isClientSide && kit.preventDeath(this)) return;
        super.die(src);
        if (level() instanceof net.minecraft.server.level.ServerLevel sl) { // death burst
            net.minecraft.world.phys.Vec3 c = position().add(0, getBbHeight() * 0.55, 0);
            com.doomslayer.fx.Fx.flash(sl, c, kit.eggSecondary, 1.1f); com.doomslayer.fx.Fx.ring(sl, position().add(0, 0.1, 0), 2.6f, kit.eggSecondary, 1.5f, 24);
            com.doomslayer.fx.Fx.burst(sl, net.minecraft.core.particles.ParticleTypes.CRIT, c, 16, 0.25, 0.4);
            com.doomslayer.fx.Fx.dustBurst(sl, kit.eggPrimary, 1.6f, c, 18, 0.5); com.doomslayer.fx.Fx.burst(sl, net.minecraft.core.particles.ParticleTypes.SMOKE, c, 8, 0.4, 0.02);
        }
    }

    /** custom death: collapse (20 ticks, animated client-side), lie still, then dissolve into rising light instead of the vanilla poof */
    @Override
    protected void tickDeath() {
        ++deathTime;
        if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
            if (deathTime >= 20 && deathTime % 2 == 0) {
                net.minecraft.world.phys.Vec3 f = net.minecraft.world.phys.Vec3.directionFromRotation(0, yBodyRot).scale(getBbHeight() * 0.5 * random.nextDouble());
                net.minecraft.world.phys.Vec3 c = position().add(f).add(0, 0.25, 0);
                com.doomslayer.fx.Fx.burst(sl, net.minecraft.core.particles.ParticleTypes.END_ROD, c, 3, 0.35, 0.02); com.doomslayer.fx.Fx.dustBurst(sl, kit.eggSecondary, 1.3f, c, 4, 0.4);
            }
            if (deathTime >= 32 && !isRemoved()) {
                com.doomslayer.fx.Fx.flash(sl, position().add(0, 0.4, 0), kit.eggSecondary, 0.9f); com.doomslayer.fx.Fx.burst(sl, net.minecraft.core.particles.ParticleTypes.SMOKE, position().add(0, 0.3, 0), 10, 0.5, 0.02);
                remove(RemovalReason.KILLED);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource src, float amt) {
        Entity s = src.getEntity();
        if (s instanceof SlayerEntity o && !rival(this, o)) return false; // no friendly fire between same-owner slayers
        if (!level().isClientSide) {
            float before = amt; amt = kit.modifyDamage(this, src, amt);
            var res = getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);
            boolean guarded = amt < before || (res != null && res.getAmplifier() >= 2), halo = kit.getClass().getSimpleName().startsWith("Halo");
            if ((guarded || halo) && tickCount - lastGuardFx >= 3 && level() instanceof net.minecraft.server.level.ServerLevel sl) { // defence visuals
                lastGuardFx = tickCount;
                Entity from = src.getDirectEntity() != null ? src.getDirectEntity() : src.getEntity();
                net.minecraft.world.phys.Vec3 dir = from != null ? from.position().subtract(position()).multiply(1, 0, 1) : getViewVector(1f).multiply(1, 0, 1);
                dir = dir.lengthSqr() < 1e-4 ? getViewVector(1f) : dir.normalize();
                if (guarded) com.doomslayer.fx.Fx.shield(sl, position().add(0, getBbHeight() * 0.6, 0).add(dir.scale(0.75)), dir, kit.eggSecondary, 0.95f * kit.scale);
                if (halo || (res != null && res.getAmplifier() >= 3)) com.doomslayer.fx.Fx.bodyShield(sl, position(), kit.eggSecondary, getBbHeight() * 1.08f, getBbWidth() * 1.1f);
            }
            if (amt <= 0) return false;
        }
        boolean r = super.hurt(src, amt);
        if (r && tickCount - lastHitFx >= 2 && level() instanceof net.minecraft.server.level.ServerLevel sl) { // hit reaction: sparks + armour chips in the kit colour
            lastHitFx = tickCount;
            Entity from = src.getDirectEntity() != null ? src.getDirectEntity() : src.getEntity();
            net.minecraft.world.phys.Vec3 d = from != null ? from.position().subtract(position()).multiply(1, 0, 1) : net.minecraft.world.phys.Vec3.ZERO;
            d = d.lengthSqr() < 1e-4 ? net.minecraft.world.phys.Vec3.ZERO : d.normalize().scale(0.4);
            net.minecraft.world.phys.Vec3 hp = position().add(0, getBbHeight() * (0.45 + random.nextDouble() * 0.3), 0).add(d);
            com.doomslayer.fx.Fx.flash(sl, hp, 0xfff0d0, 0.4f + Math.min(0.5f, amt * 0.03f));
            com.doomslayer.fx.Fx.burst(sl, net.minecraft.core.particles.ParticleTypes.CRIT, hp, 6, 0.1, 0.3);
            com.doomslayer.fx.Fx.dustBurst(sl, kit.eggPrimary, 1.2f, hp, 5, 0.2);
        }
        if (r && s instanceof SlayerEntity o && getTarget() == null) setTarget(o); // retaliate
        return r;
    }

    @Override
    protected InteractionResult mobInteract(Player p, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || level().isClientSide) return InteractionResult.sidedSuccess(level().isClientSide);
        if (getOwnerId() == null) { setOwner(p.getUUID()); p.displayClientMessage(Component.literal(kit.name + " joins you."), true); }
        else if (p.getUUID().equals(getOwnerId()) && p.isShiftKeyDown()) {
            entityData.set(STAY, !isStaying());
            p.displayClientMessage(Component.literal(kit.name + (isStaying() ? ": hold position" : ": follow")), true);
        }
        else if (p.getUUID().equals(getOwnerId())) { // manual retarget: next-nearest hostile around the owner
            LivingEntity cur = getTarget(), best = null; double bd = Double.MAX_VALUE;
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(20), x -> x != cur && isHostile(this, x))) {
                double d = e.distanceToSqr(p); if (d < bd) { bd = d; best = e; }
            }
            setTarget(best); if (isAttacking()) interruptAttack();
            p.displayClientMessage(Component.literal(kit.name + (best == null ? ": no other target" : ": target -> " + best.getName().getString())), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** AI retarget: drop a far-away target when something closer is threatening us (group fights) */
    private void retarget() {
        LivingEntity cur = getTarget(); if (isAttacking()) return;
        double curD = cur == null ? Double.MAX_VALUE : distanceTo(cur);
        LivingEntity hurter = getLastHurtByMob(), best = null; double bd = Double.MAX_VALUE;
        if (hurter != null && hurter != cur && isHostile(this, hurter) && tickCount - getLastHurtByMobTimestamp() < 100 && distanceTo(hurter) + 3 < curD) best = hurter;
        else for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5), x -> x != cur && isHostile(this, x))) {
            double d = distanceTo(e); if (d < bd && d + 3 < curD && curD > 6) { bd = d; best = e; }
        }
        if (best != null) setTarget(best);
    }

    @Override public Component getTypeName() { return Component.literal(kit.name + " (" + kit.game + ")"); }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean isPushable() { return super.isPushable(); }

    @Override public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t); if (getOwnerId() != null) t.putUUID("Owner", getOwnerId()); t.putBoolean("Stay", isStaying());
    }
    @Override public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t); if (t.hasUUID("Owner")) setOwner(t.getUUID("Owner")); entityData.set(STAY, t.getBoolean("Stay"));
    }

    // ---------- goals ----------
    public static boolean isHostile(SlayerEntity self, LivingEntity e) {
        return e.isAlive() && (e instanceof SlayerEntity o ? rival(self, o) : e instanceof Enemy);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CombatGoal());
        goalSelector.addGoal(5, new FollowGoal());
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override public boolean canUse() { return !isStaying() && getOwnerId() == null && super.canUse(); }
        });
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new DefendOwnerGoal());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 5, true, false, e -> isHostile(this, e)));
    }

    class CombatGoal extends Goal {
        CombatGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() { LivingEntity t = getTarget(); return t != null && t.isAlive(); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public void tick() {
            if (tickCount % 10 == 0) retarget();
            LivingEntity t = getTarget(); if (t == null) return;
            if (!isAttacking()) {
                getLookControl().setLookAt(t, 30, 30);
                AttackDef d = pickAttack(t);
                double dist = distanceTo(t);
                if (d != null && tryStart(d, t)) return;
                if (isStaying() && dist > 3) { getNavigation().stop(); return; }
                boolean ranged = kit.attacks().stream().anyMatch(a -> a.maxRange > 6);
                if (ranged && dist < kit.preferredRange * 0.6 && kit.attacks().stream().noneMatch(a -> a.maxRange <= 4 && cds[a.index] == 0)) {
                    var away = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosAway(SlayerEntity.this, 8, 4, t.position());
                    if (away != null) getNavigation().moveTo(away.x, away.y, away.z, 1.15);
                } else if (dist > Math.min(kit.preferredRange, 2.5) && !(ranged && dist <= kit.preferredRange && hasLineOfSight(t))) {
                    getNavigation().moveTo(t, 1.2);
                } else getNavigation().stop();
            }
        }
        @Override public void stop() { getNavigation().stop(); }
    }

    class FollowGoal extends Goal {
        FollowGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() { Player o = getOwner(); return o != null && !isStaying() && distanceToSqr(o) > 64; }
        @Override public boolean canContinueToUse() { Player o = getOwner(); return o != null && !isStaying() && distanceToSqr(o) > 16 && getTarget() == null; }
        @Override public void tick() {
            Player o = getOwner(); if (o == null) return;
            if (distanceToSqr(o) > 1024 && o.onGround()) { teleportTo(o.getX(), o.getY(), o.getZ()); getNavigation().stop(); return; }
            getNavigation().moveTo(o, 1.15);
        }
    }

    class DefendOwnerGoal extends net.minecraft.world.entity.ai.goal.target.TargetGoal {
        DefendOwnerGoal() { super(SlayerEntity.this, false); }
        @Override public boolean canUse() {
            Player o = getOwner(); if (o == null) return false;
            LivingEntity t = o.getLastHurtByMob(); if (t == null || t instanceof Player || t instanceof SlayerEntity) t = o.getLastHurtMob();
            if (t == null || t instanceof Player || t instanceof SlayerEntity || !t.isAlive() || !isHostile(SlayerEntity.this, t)) return false;
            this.target = t; return true;
        }
        private LivingEntity target;
        @Override public void start() { mob.setTarget(target); super.start(); }
    }
}
