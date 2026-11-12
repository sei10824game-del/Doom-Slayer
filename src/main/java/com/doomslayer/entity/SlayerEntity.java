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
    }

    @Override
    public boolean hurt(DamageSource src, float amt) {
        Entity s = src.getEntity();
        if (s instanceof SlayerEntity o && !rival(this, o)) return false; // no friendly fire between same-owner slayers
        if (!level().isClientSide) { amt = kit.modifyDamage(this, src, amt); if (amt <= 0) return false; }
        boolean r = super.hurt(src, amt);
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
