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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Doom Slayer of DOOM: The Dark Ages (2025). Heavy medieval plate in olive green / gunmetal with gold trim,
 * Shield Saw on the left arm, slow but brutal. Melee trio (Power Gauntlet, Flail, Dreadmace), the full gun roster,
 * parry/block/charge/throw shield moves, staged glory kills, and the rare Serrat strafing run / Atlan mech stomp.
 */
public class DarkAgesSlayer extends SlayerKit {
    static final int GREEN = 0x4c5a2e, GREEN_D = 0x33401e, GUN = 0x4a4f55, GUN_D = 0x2a2d31, GOLD = 0xc99a38,
            LEATHER = 0x3b2b1e, VISOR = 0xffb43a, PARRY = 0x6dff4a, HOT = 0xff7a1a, BLOOD = 0x8a0a0a, CHAIN = 0x8a8a8a;

    // ---------------- projectiles ----------------
    static final ProjStyle SAW = ProjStyle.make().color(0xffb040).size(0.85f).life(70).pierce(64).trail(ParticleTypes.CRIT)
            .onTick(DarkAgesSlayer::sawTick)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.sparks(l, pos, 10);
                if (hit != null) { Fx.dustBurst(l, BLOOD, 1.2f, pos, 8, 0.25); Fx.sound(l, pos, SoundEvents.GRINDSTONE_USE, 0.8f, 1.6f); }
            });
    static final ProjStyle PARRY_BOLT = ProjStyle.make().color(PARRY).size(0.55f).life(40).homing(0.25f).trail(ParticleTypes.HAPPY_VILLAGER);
    static final ProjStyle ACCEL = ProjStyle.make().color(0x9cff70).size(0.32f).life(40).trail(ParticleTypes.ELECTRIC_SPARK);
    static final ProjStyle PULV = ProjStyle.make().color(0xff4020).size(0.85f).life(60).gravity(0.012).explode(3.2f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle SHRED = ProjStyle.make().color(HOT).size(0.28f).life(30).pierce(1).trail(ParticleTypes.CRIT);
    static final ProjStyle CHAINBALL = ProjStyle.make().color(0xb0b0b0).size(0.6f).life(45).gravity(0.035).explode(2.4f).trail(ParticleTypes.CRIT);
    static final ProjStyle DRAGONFIRE = ProjStyle.make().color(0xff6a20).size(0.9f).life(60).explode(2.8f).trail(ParticleTypes.FLAME);

    public DarkAgesSlayer() {
        super("dark_ages_slayer", "Doom Slayer", "ドゥームスレイヤー", "DOOM: The Dark Ages");
        maxHealth = 130; speed = 0.25; armor = 18; knockbackRes = 0.85; followRange = 32;
        scale = 1.12f; width = 0.7f; height = 2.0f; preferredRange = 7f; dmgMult = 1.3f;
        eggPrimary = GREEN; eggSecondary = GOLD;
        idleWeaponR = "cshotgun"; idleWeaponL = "shield";
    }

    // =====================================================================================================
    // ATTACKS
    // =====================================================================================================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---- Shield Saw ----
        out.add(AttackDef.of("shield_saw_throw").range(4, 14).cd(70).dur(32).weapons("cshotgun", "shield")
                .tick(DarkAgesSlayer::tThrow).anim(DarkAgesSlayer::aThrow));
        out.add(AttackDef.of("shield_parry").range(0, 12).noLos().cd(50).dur(14).weight(3).weapons("cshotgun", "shield")
                .when((e, t) -> !incoming(e, 6).isEmpty() || e.distanceTo(t) < 3)
                .tick(DarkAgesSlayer::tParry).anim(DarkAgesSlayer::aParry));
        out.add(AttackDef.of("shield_block").range(0, 18).noLos().cd(120).dur(30).weight(2).weapons("cshotgun", "shield")
                .when((e, t) -> !incoming(e, 8).isEmpty() || t instanceof RangedAttackMob)
                .tick(DarkAgesSlayer::tBlock).anim(DarkAgesSlayer::aBlock));
        out.add(AttackDef.of("shield_charge").range(4, 14).cd(90).dur(22).weapons("cshotgun", "shield")
                .onStart((e, t, n) -> e.getPersistentData().putBoolean("das_chg", false))
                .tick(DarkAgesSlayer::tCharge).anim(DarkAgesSlayer::aCharge));
        out.add(AttackDef.of("shield_saw_grind").range(0, 3.5f).cd(45).dur(24).weapons("cshotgun", "shield_spin")
                .tick(DarkAgesSlayer::tGrind).anim(DarkAgesSlayer::aGrind));
        // ---- melee weapons ----
        out.add(AttackDef.of("dreadmace_skullcrusher_smash").range(0, 4).cd(60).dur(28).weapons("mace", "shield")
                .tick(DarkAgesSlayer::tMace).anim(DarkAgesSlayer::aMace));
        out.add(AttackDef.of("flail_spin_sweep").range(0, 4.5f).cd(55).dur(26).weapons("flail", "shield")
                .tick(DarkAgesSlayer::tFlailSweep).anim(DarkAgesSlayer::aFlailSweep));
        out.add(AttackDef.of("flail_lash").range(4, 8.5f).cd(45).dur(22).weapons("flail", "shield")
                .tick(DarkAgesSlayer::tFlailLash).anim(DarkAgesSlayer::aFlailLash));
        out.add(AttackDef.of("power_gauntlet_combo").range(0, 3).cd(40).dur(32).weapons("gauntlet", "shield")
                .tick(DarkAgesSlayer::tGauntlet).anim(DarkAgesSlayer::aGauntlet));
        // ---- guns ----
        out.add(AttackDef.of("combat_shotgun").range(2, 14).cd(28).dur(16).weapons("cshotgun", "shield")
                .tick(DarkAgesSlayer::tShotgun).anim(DarkAgesSlayer::aShotgun));
        out.add(AttackDef.of("super_shotgun").range(0, 8).cd(50).dur(26).weapons("sshotgun", "shield")
                .tick(DarkAgesSlayer::tSsg).anim(DarkAgesSlayer::aSsg));
        out.add(AttackDef.of("accelerator").range(6, 24).cd(50).dur(32).weapons("accelerator", "shield")
                .tick(DarkAgesSlayer::tAccel).anim(DarkAgesSlayer::aAccel));
        out.add(AttackDef.of("pulverizer").range(4, 20).cd(80).dur(34).weapons("pulverizer", "shield")
                .tick(DarkAgesSlayer::tPulv).anim(DarkAgesSlayer::aPulv));
        out.add(AttackDef.of("ripper_shredder").range(3, 18).cd(60).dur(36).weapons("shredder", "shield")
                .tick(DarkAgesSlayer::tShred).anim(DarkAgesSlayer::aShred));
        out.add(AttackDef.of("cycler").range(3, 16).cd(55).dur(30).weapons("cycler", "shield")
                .tick(DarkAgesSlayer::tCycler).anim(DarkAgesSlayer::aCycler));
        out.add(AttackDef.of("rocket_launcher").range(6, 26).cd(70).dur(26).weapons("rocket", "shield")
                .tick(DarkAgesSlayer::tRocket).anim(DarkAgesSlayer::aRocket));
        out.add(AttackDef.of("chainshot").range(5, 20).cd(60).dur(30).weapons("chainshot", "shield")
                .tick(DarkAgesSlayer::tChainshot).anim(DarkAgesSlayer::aChainshot));
        out.add(AttackDef.of("impaler_lance").range(10, 30).cd(70).dur(28).weapons("impaler", "shield")
                .tick(DarkAgesSlayer::tImpaler).anim(DarkAgesSlayer::aImpaler));
        // ---- glory kills (target <= 25% hp) ----
        out.add(AttackDef.of("glory_kill_shield_execution").range(0, 3.5f).lowHealthTarget(0.25f).cd(60).dur(40).weight(6)
                .weapons("gauntlet", "shield").tick(DarkAgesSlayer::tGkBash).anim(DarkAgesSlayer::aGkBash));
        out.add(AttackDef.of("glory_kill_saw_execution").range(0, 3.5f).lowHealthTarget(0.25f).cd(60).dur(44).weight(5)
                .weapons("gauntlet", "shield_spin").tick(DarkAgesSlayer::tGkSaw).anim(DarkAgesSlayer::aGkSaw));
        // ---- rare specials ----
        out.add(AttackDef.of("serrat_dragon_strafe").range(6, 30).cd(900).dur(56).weapons("cshotgun", "shield")
                .onStart(DarkAgesSlayer::sStrafe).tick(DarkAgesSlayer::tStrafe).anim(DarkAgesSlayer::aStrafe));
        out.add(AttackDef.of("atlan_mech_stomp").range(0, 14).noLos().cd(1200).dur(64).weapons("cshotgun", "shield")
                .onStart((e, t, n) -> store(e, "das_st", t.position())).tick(DarkAgesSlayer::tStomp).anim(DarkAgesSlayer::aStomp));
    }

    // =====================================================================================================
    // SERVER HELPERS
    // =====================================================================================================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 chest(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static Vec3 gunPos(SlayerEntity e) { return Combat.muzzle(e, 1.0, 0.35, 0); }
    static Vec3 shieldPos(SlayerEntity e) { return Combat.muzzle(e, 0.9, -0.4, -0.1); }
    static Random rnd(SlayerEntity e) { return new Random(e.getRandom().nextLong()); }
    static Vec3 flat(Vec3 v) { v = new Vec3(v.x, 0, v.z); return v.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : v.normalize(); }

    static void store(SlayerEntity e, String k, Vec3 v) {
        CompoundTag d = e.getPersistentData(); d.putDouble(k + "x", v.x); d.putDouble(k + "y", v.y); d.putDouble(k + "z", v.z);
    }
    static Vec3 load(SlayerEntity e, String k) {
        CompoundTag d = e.getPersistentData(); return new Vec3(d.getDouble(k + "x"), d.getDouble(k + "y"), d.getDouble(k + "z"));
    }

    static void flash(SlayerEntity e, Vec3 at) {
        ServerLevel l = L(e);
        Fx.burst(l, ParticleTypes.FLAME, at, 6, 0.08, 0.05); Fx.burst(l, ParticleTypes.SMOKE, at, 4, 0.1, 0.02);
        Fx.dustBurst(l, 0xffd070, 1.3f, at, 4, 0.05);
    }
    static void pellets(SlayerEntity e, LivingEntity tg, int n, double spread, double range, float dmg, int rgb) {
        if (tg == null) return;
        Vec3 from = gunPos(e); Random r = rnd(e);
        for (int i = 0; i < n; i++) Combat.hitscan(e, from, Combat.dirTo(from, tg, spread, r), range, dmg, 0, rgb);
        flash(e, from);
    }
    static SlayerProjectile fire(SlayerEntity e, ProjStyle s, LivingEntity tg, double speed, float dmg, double spread) {
        Vec3 from = gunPos(e);
        Vec3 dir = tg == null ? e.getViewVector(1f) : Combat.dirTo(from, tg, spread, rnd(e));
        flash(e, from);
        return Combat.shoot(e, s, from, dir, speed, dmg);
    }
    /** cone melee hit with knockback + impact fx; returns foes hit */
    static int smack(SlayerEntity e, double range, double half, float dmg, double kb, double up, int rgb) {
        int n = 0; ServerLevel l = L(e);
        for (LivingEntity v : Combat.cone(e, range, half)) {
            if (Combat.damage(e, v, dmg)) n++;
            Combat.knock(v, e.position(), kb, up);
            Vec3 c = chest(v); Fx.sparks(l, c, 6); Fx.dustBurst(l, rgb, 1.3f, c, 6, 0.2);
        }
        return n;
    }
    static void debris(ServerLevel l, Vec3 c, double r, int n) {
        BlockState st = l.getBlockState(BlockPos.containing(c.x, c.y - 0.5, c.z));
        if (st.isAir()) st = Blocks.DIRT.defaultBlockState();
        l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), c.x, c.y + 0.1, c.z, n, r, 0.1, r, 0.15);
    }
    static void freeze(LivingEntity t, int ticks) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 10, false, false));
        t.setDeltaMovement(0, t.getDeltaMovement().y, 0);
        if (t instanceof Mob m) m.getNavigation().stop();
    }
    /** enemy projectiles in flight near the slayer */
    static List<Projectile> incoming(SlayerEntity e, double r) {
        return e.level().getEntitiesOfClass(Projectile.class, e.getBoundingBox().inflate(r), p -> !(p instanceof SlayerProjectile)
                && !(p.getOwner() instanceof Player) && !(p.getOwner() instanceof SlayerEntity) && p.getDeltaMovement().lengthSqr() > 0.01);
    }
    /** destroy enemy projectiles; if reflect, send a green parry bolt back at the shooter (never at players). */
    static int deflect(SlayerEntity e, LivingEntity tg, double r, boolean reflect) {
        int n = 0; ServerLevel l = L(e);
        for (Projectile pr : incoming(e, r)) {
            Vec3 at = pr.position(); Entity sh = pr.getOwner();
            pr.discard(); n++;
            Fx.dustBurst(l, PARRY, 1.5f, at, 10, 0.2); Fx.sparks(l, at, 6);
            if (reflect) {
                LivingEntity aim = sh instanceof LivingEntity le && Combat.isFoe(e, le) ? le : tg;
                if (aim != null) Combat.shoot(e, PARRY_BOLT, at, chest(aim).subtract(at), 1.6, 12);
            }
        }
        return n;
    }

    static void sawTick(SlayerProjectile p) {
        ServerLevel l = (ServerLevel) p.level();
        Vec3 c = p.position(); double a = p.age2 * 1.4;
        for (int i = 0; i < 4; i++) {
            double q = a + i * Math.PI / 2;
            l.sendParticles(Fx.dust(i % 2 == 0 ? HOT : GOLD, 0.8f), c.x + Math.cos(q) * 0.55, c.y, c.z + Math.sin(q) * 0.55, 1, 0, 0, 0, 0);
        }
        if (p.age2 % 4 == 0) Fx.sound(l, c, SoundEvents.GRINDSTONE_USE, 0.4f, 2f);
        if (p.age2 == 12) Fx.sound(l, c, SoundEvents.TRIDENT_RETURN, 1f, 1.2f);
        if (p.age2 >= 12 && p.getOwner() instanceof SlayerEntity o) {
            Vec3 to = o.getEyePosition().subtract(0, 0.5, 0).subtract(c);
            if (to.length() < 1.4) { p.discard(); return; } // caught
            p.setDeltaMovement(to.normalize().scale(1.35));
        }
    }

    // =====================================================================================================
    // SERVER TICKS
    // =====================================================================================================
    static void tThrow(SlayerEntity e, LivingEntity tg, int t) {
        if (t == 5) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 1f, 0.7f);
        if (t == 9) {
            Vec3 from = shieldPos(e);
            Vec3 dir = tg == null ? e.getViewVector(1f) : chest(tg).subtract(from);
            Combat.shoot(e, SAW, from, dir, 1.3, 9);
            snd(e, SoundEvents.TRIDENT_THROW, 1.2f, 0.7f);
            Fx.dustBurst(L(e), HOT, 1.2f, from, 8, 0.15);
        }
        if (t == 27) { snd(e, SoundEvents.SHIELD_BLOCK, 1f, 0.8f); Fx.sparks(L(e), shieldPos(e), 6); }
    }
    static void tParry(SlayerEntity e, LivingEntity tg, int t) {
        if (t < 2 || t > 7) return;
        ServerLevel l = L(e); Vec3 sp = shieldPos(e);
        int n = deflect(e, tg, 5, true);
        if (t == 3) {
            snd(e, SoundEvents.SHIELD_BLOCK, 1.3f, 1.4f); snd(e, SoundEvents.ANVIL_LAND, 0.5f, 1.9f);
            snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.6f);
            Fx.dustBurst(l, PARRY, 2.2f, sp, 24, 0.35); Fx.burst(l, ParticleTypes.GLOW, sp, 10, 0.4, 0.05);
            Fx.arc(l, e.position().add(0, 1.2, 0), e.getYRot(), 1.4f, -70, 70, 90, PARRY, 1.6f, 22);
            for (LivingEntity v : Combat.cone(e, 3.4, 70)) {
                Combat.damage(e, v, 6); Combat.knock(v, e.position(), 1.5, 0.35); freeze(v, 30);
                Fx.dustBurst(l, PARRY, 1.4f, chest(v), 10, 0.25);
            }
        }
        if (n > 0) snd(e, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.8f);
    }
    static void tBlock(SlayerEntity e, LivingEntity tg, int t) {
        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 2, false, false));
        if (t == 2) snd(e, SoundEvents.ARMOR_EQUIP_NETHERITE, 1f, 0.6f);
        if (deflect(e, tg, 3.5, false) > 0) snd(e, SoundEvents.SHIELD_BLOCK, 1f, 1f);
        if (tg != null && t > 4 && t < 26 && e.distanceTo(tg) > 2.5) { // heavy shielded advance
            Vec3 f = flat(tg.position().subtract(e.position()));
            e.setDeltaMovement(f.x * 0.09, e.getDeltaMovement().y, f.z * 0.09); e.hurtMarked = true;
        }
        if (t % 6 == 0) Fx.dustBurst(L(e), PARRY, 0.8f, shieldPos(e), 3, 0.2);
    }
    static void tCharge(SlayerEntity e, LivingEntity tg, int t) {
        ServerLevel l = L(e); CompoundTag d = e.getPersistentData();
        if (t == 4) { snd(e, SoundEvents.RAVAGER_ROAR, 0.8f, 1.4f); snd(e, SoundEvents.ARMOR_EQUIP_NETHERITE, 1f, 0.5f); }
        if (t >= 5 && t <= 14 && !d.getBoolean("das_chg")) {
            Combat.lunge(e, 1.05, 0.02);
            Fx.dustBurst(l, PARRY, 1.2f, e.position().add(0, 1, 0), 4, 0.3); Fx.burst(l, ParticleTypes.CLOUD, e.position(), 2, 0.2, 0.01);
            if (t % 3 == 0) snd(e, SoundEvents.RAVAGER_STEP, 1f, 1.3f);
            Vec3 front = e.position().add(flat(e.getViewVector(1f)).scale(1.0)).add(0, 1, 0);
            List<LivingEntity> hit = Combat.around(e, front, 1.6);
            if (!hit.isEmpty()) {
                d.putBoolean("das_chg", true);
                for (LivingEntity v : hit) {
                    Combat.damage(e, v, 15); Combat.knock(v, e.position(), 2.4, 0.55);
                    Fx.dustBurst(l, PARRY, 2f, chest(v), 18, 0.3);
                }
                Fx.explosion(l, front, 1.2f); debris(l, e.position(), 0.8, 20);
                snd(e, SoundEvents.IRON_GOLEM_ATTACK, 1.4f, 0.7f); snd(e, SoundEvents.ANVIL_LAND, 0.8f, 0.9f);
                e.setDeltaMovement(e.getDeltaMovement().scale(0.1)); e.hurtMarked = true;
            }
        }
    }
    static void tGrind(SlayerEntity e, LivingEntity tg, int t) {
        ServerLevel l = L(e);
        if (t == 4) snd(e, SoundEvents.GRINDSTONE_USE, 1.2f, 0.8f);
        if (t >= 6 && t <= 16) {
            Fx.burst(l, ParticleTypes.CRIT, shieldPos(e), 4, 0.2, 0.3);
            Fx.dustBurst(l, HOT, 0.9f, shieldPos(e), 3, 0.25);
            if (t % 2 == 0) {
                for (LivingEntity v : Combat.cone(e, 3.2, 60)) {
                    Combat.damage(e, v, 3.2f); Fx.dustBurst(l, BLOOD, 1.1f, chest(v), 5, 0.2); Fx.sparks(l, chest(v), 4);
                }
            }
            if (t == 12) snd(e, SoundEvents.GRINDSTONE_USE, 1.2f, 1.1f);
        }
        if (t == 18) { smack(e, 3.4, 60, 6, 1.2, 0.3, HOT); snd(e, SoundEvents.SHIELD_BLOCK, 1f, 0.6f); }
    }
    static void tMace(SlayerEntity e, LivingEntity tg, int t) {
        ServerLevel l = L(e);
        if (t == 4) snd(e, SoundEvents.ARMOR_EQUIP_NETHERITE, 1f, 0.6f);
        if (t == 11) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.5f);
        Vec3 c = e.position().add(flat(e.getViewVector(1f)).scale(2.2));
        if (t == 14) {
            smack(e, 4, 55, 18, 0.6, 0.5, GOLD);
            snd(e, SoundEvents.ANVIL_LAND, 1.2f, 0.55f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.3f);
            Fx.explosion(l, c, 1.0f); debris(l, c, 1.2, 40); Fx.burst(l, ParticleTypes.CRIT, c.add(0, 0.3, 0), 20, 0.6, 0.4);
        }
        if (t >= 14 && t <= 21) {
            float r = 1 + (t - 14) * 0.75f;
            Fx.ring(l, c.add(0, 0.15, 0), r, GOLD, 1.6f, 28); debris(l, c, r * 0.6, 6);
        }
        if (t == 17) for (LivingEntity v : Combat.around(e, c, 5.5)) { Combat.damage(e, v, 6); Combat.knock(v, c, 0.9, 0.6); }
    }
    static void tFlailSweep(SlayerEntity e, LivingEntity tg, int t) {
        ServerLevel l = L(e);
        if (t == 6) snd(e, SoundEvents.CHAIN_PLACE, 1.2f, 0.7f);
        if (t >= 7 && t <= 17) {
            float from = -180 + (t - 7) * 34f;
            Fx.arc(l, e.position().add(0, 1.1, 0), e.getYRot(), 3.2f, from, from + 40, 8, CHAIN, 1.3f, 8);
            if (t % 3 == 0) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 0.7f);
        }
        if (t == 12) {
            for (LivingEntity v : Combat.around(e, e.position().add(0, 1, 0), 4.2)) {
                Combat.damage(e, v, 12); Combat.knock(v, e.position(), 1.4, 0.35); Fx.sparks(l, chest(v), 8);
            }
            snd(e, SoundEvents.CHAIN_HIT, 1.4f, 0.6f); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.7f);
        }
    }
    static void tFlailLash(SlayerEntity e, LivingEntity tg, int t) {
        if (t == 5) snd(e, SoundEvents.CHAIN_PLACE, 1f, 1.2f);
        if (t == 10 && tg != null) {
            ServerLevel l = L(e); Vec3 from = gunPos(e), dir = chest(tg).subtract(from).normalize();
            Fx.beam(l, from, from.add(dir.scale(Math.min(9, e.distanceTo(tg) + 0.5))), CHAIN, 1.0f, 0.3);
            if (Combat.hitscan(e, from, dir, 9, 13, 2, GOLD) > 0) Combat.knock(tg, e.position(), -0.8, 0.25); // yank toward slayer
            snd(e, SoundEvents.CHAIN_HIT, 1.4f, 0.8f); snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.6f);
        }
    }
    static void tGauntlet(SlayerEntity e, LivingEntity tg, int t) {
        if (t == 7) { smack(e, 3, 50, 8, 0.5, 0.1, GOLD); snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.7f); }
        if (t == 15) { smack(e, 3, 55, 7, 0.9, 0.15, PARRY); snd(e, SoundEvents.SHIELD_BLOCK, 1f, 0.7f); }
        if (t == 25) {
            smack(e, 3, 50, 13, 0.4, 0.9, GOLD);
            snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.4f, 0.5f); snd(e, SoundEvents.IRON_GOLEM_ATTACK, 1f, 0.8f);
            Fx.burst(L(e), ParticleTypes.EXPLOSION, gunPos(e), 1, 0, 0);
        }
    }
    static void tShotgun(SlayerEntity e, LivingEntity tg, int t) {
        if (t == 4) { pellets(e, tg, 8, 0.07, 16, 2.6f, 0xffd070); Combat.gunSound(e, 0.8f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.3f, 1.9f); }
        if (t == 10) snd(e, SoundEvents.PISTON_CONTRACT, 0.8f, 1.4f);
    }
    static void tSsg(SlayerEntity e, LivingEntity tg, int t) {
        if (t == 6) {
            pellets(e, tg, 16, 0.12, 10, 3.0f, 0xffc060);
            for (LivingEntity v : Combat.cone(e, 5, 30)) Combat.knock(v, e.position(), 0.9, 0.2);
            Combat.gunSound(e, 0.6f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.3f);
            Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, gunPos(e), 8, 0.2, 0.03);
        }
        if (t == 13) snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 1f, 1.3f);
        if (t == 19) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 1f, 1.5f);
    }
    static void tAccel(SlayerEntity e, LivingEntity tg, int t) {
        if (t == 2) snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 2f);
        if (t < 10) Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, gunPos(e), 3, 0.1, 0.05);
        if (t >= 10 && t <= 28 && (t - 10) % 3 == 0) { fire(e, ACCEL, tg, 2.0, 5, 0.03); snd(e, SoundEvents.BLAZE_SHOOT, 0.5f, 1.9f); }
    }
    static void tPulv(SlayerEntity e, LivingEntity tg, int t) {
        if (t == 4) snd(e, SoundEvents.GRINDSTONE_USE, 1.2f, 0.5f);
        if (t > 4 && t < 16) { Fx.burst(L(e), ParticleTypes.LAVA, gunPos(e), 1, 0.1, 0); Fx.dustBurst(L(e), 0xff4020, 1f, gunPos(e), 2, 0.15); }
        if (t == 16) {
            fire(e, PULV, tg, 1.4, 16, 0.01);
            snd(e, SoundEvents.WITHER_SHOOT, 1.2f, 0.7f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.5f);
            Combat.knock(e, gunPos(e), 0.35, 0.05);
        }
    }
    static void tShred(SlayerEntity e, LivingEntity tg, int t) {
        if (t == 4) snd(e, SoundEvents.GRINDSTONE_USE, 1f, 1.5f);
        if (t >= 6 && t <= 30 && t % 2 == 0) { fire(e, SHRED, tg, 2.2, 3, 0.05); snd(e, SoundEvents.DISPENSER_LAUNCH, 0.5f, 1.7f); }
    }
    static void tCycler(SlayerEntity e, LivingEntity tg, int t) {
        if (t == 4) snd(e, SoundEvents.BEACON_POWER_SELECT, 1f, 1.8f);
        if (tg == null || t < 6 || t > 24 || t % 2 != 0) return;
        ServerLevel l = L(e); Vec3 from = gunPos(e);
        if (Combat.hitscan(e, from, Combat.dirTo(from, tg, 0.02, rnd(e)), 16, 3, 0, 0x60e0ff) > 0) {
            for (LivingEntity v : Combat.around(e, chest(tg), 4)) {
                if (v == tg) continue;
                Combat.damage(e, v, 2); Fx.beam(l, chest(tg), chest(v), 0x9ff0ff, 0.6f, 0.3); break; // arc jumps to one neighbour
            }
            Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, chest(tg), 6, 0.3, 0.1);
        }
        Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, from, 4, 0.1, 0.05);
        if (t % 4 == 0) snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.25f, 2f);
    }
    static void tRocket(SlayerEntity e, LivingEntity tg, int t) {
        if (t == 9) {
            fire(e, ProjStyle.ROCKET, tg, 1.3, 16, 0.01);
            snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.6f);
            Vec3 back = e.position().add(0, 1.6, 0).subtract(flat(e.getViewVector(1f)).scale(1.2));
            Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, back, 10, 0.3, 0.05); Fx.burst(L(e), ParticleTypes.FLAME, back, 6, 0.2, 0.05);
        }
    }
    static void tChainshot(SlayerEntity e, LivingEntity tg, int t) {
        if (t >= 3 && t <= 11 && t % 3 == 0) snd(e, SoundEvents.CHAIN_STEP, 1f, 0.6f + t * 0.05f);
        if (t == 12) {
            fire(e, CHAINBALL, tg, 1.4, 14, 0.01);
            Vec3 from = gunPos(e); Fx.beam(L(e), from, from.add(e.getViewVector(1f).scale(4)), CHAIN, 1f, 0.25);
            snd(e, SoundEvents.CROSSBOW_SHOOT, 1.3f, 0.6f); snd(e, SoundEvents.CHAIN_BREAK, 1f, 0.8f);
        }
    }
    static void tImpaler(SlayerEntity e, LivingEntity tg, int t) {
        if (t >= 4 && t < 13) Fx.burst(L(e), ParticleTypes.END_ROD, gunPos(e), 1, 0.05, 0.01);
        if (t == 4) snd(e, SoundEvents.CROSSBOW_LOADING_MIDDLE, 1f, 0.8f);
        if (t == 13 && tg != null) {
            Vec3 from = gunPos(e);
            Combat.hitscan(e, from, Combat.dirTo(from, tg, 0.003, rnd(e)), 32, 20, 3, HOT);
            if (tg.isAlive()) tg.setSecondsOnFire(3);
            flash(e, from); snd(e, SoundEvents.CROSSBOW_SHOOT, 1.5f, 0.5f); snd(e, SoundEvents.TRIDENT_HIT, 1.2f, 0.7f);
        }
    }
    static void execute(SlayerEntity e, LivingEntity tg) {
        ServerLevel l = L(e); Vec3 c = chest(tg);
        Combat.damage(e, tg, 500);
        Fx.dustBurst(l, BLOOD, 2f, c, 40, 0.45); Fx.dustBurst(l, 0x4a0000, 1.4f, c, 20, 0.3);
        Fx.burst(l, ParticleTypes.DAMAGE_INDICATOR, c, 10, 0.3, 0.2);
        snd(e, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.2f, 0.8f); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 0.6f);
        e.heal(12); Fx.burst(l, ParticleTypes.HEART, e.position().add(0, 2, 0), 3, 0.3, 0);
    }
    static void tGkBash(SlayerEntity e, LivingEntity tg, int t) {
        if (tg == null) return;
        ServerLevel l = L(e);
        if (t == 1) { freeze(tg, 45); snd(e, SoundEvents.ARMOR_EQUIP_NETHERITE, 1f, 0.5f); }
        if (t == 8) { Combat.damage(e, tg, 2); freeze(tg, 40); Fx.dustBurst(l, PARRY, 1.6f, chest(tg), 14, 0.3); snd(e, SoundEvents.SHIELD_BLOCK, 1.2f, 0.6f); }
        if (t == 18) { Combat.damage(e, tg, 2); freeze(tg, 30); Fx.sparks(l, chest(tg), 10); Fx.dustBurst(l, BLOOD, 1.2f, chest(tg), 10, 0.2); snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.6f); }
        if (t == 30) { execute(e, tg); debris(l, tg.position(), 0.8, 30); snd(e, SoundEvents.ANVIL_LAND, 1f, 0.6f); }
    }
    static void tGkSaw(SlayerEntity e, LivingEntity tg, int t) {
        if (tg == null) return;
        ServerLevel l = L(e);
        if (t == 2) { freeze(tg, 50); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.6f); }
        if (t >= 12 && t <= 28) {
            Fx.sparks(l, chest(tg), 3); Fx.dustBurst(l, BLOOD, 1.2f, chest(tg), 4, 0.25); Fx.dustBurst(l, HOT, 0.8f, chest(tg), 2, 0.2);
            if (t % 4 == 0) { snd(e, SoundEvents.GRINDSTONE_USE, 1.2f, 0.7f + (t - 12) * 0.03f); Combat.damage(e, tg, 1); freeze(tg, 30); }
        }
        if (t == 33) { execute(e, tg); Combat.knock(tg, e.position(), 2, 0.4); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.3f, 0.5f); }
    }
    static void sStrafe(SlayerEntity e, LivingEntity tg, int t) {
        Vec3 d = flat(tg.position().subtract(e.position()));
        store(e, "das_sa", e.position().add(d.scale(2)));
        store(e, "das_sb", tg.position().add(d.scale(8)));
    }
    static void tStrafe(SlayerEntity e, LivingEntity tg, int t) {
        ServerLevel l = L(e);
        if (t == 2) snd(e, SoundEvents.ENDER_DRAGON_GROWL, 2f, 1.3f);
        if (t < 16 || t > 42) return;
        Vec3 a = load(e, "das_sa"), b = load(e, "das_sb");
        double f = (t - 16) / 26.0;
        Vec3 g = a.add(b.subtract(a).scale(f)), sky = g.add(0, 13, 0);
        // the dragon itself: dark winged silhouette + engine glow sweeping overhead
        Vec3 side = flat(b.subtract(a)).cross(new Vec3(0, 1, 0));
        for (int i = -4; i <= 4; i++) {
            Vec3 w = sky.add(side.scale(i * 0.9)).add(0, -Math.abs(i) * 0.25, 0);
            l.sendParticles(Fx.dust(0x1d2a1a, 3f), w.x, w.y, w.z, 2, 0.15, 0.1, 0.15, 0);
        }
        Fx.burst(l, ParticleTypes.DRAGON_BREATH, sky, 6, 0.4, 0.02); Fx.burst(l, ParticleTypes.FLAME, sky, 4, 0.3, 0.02);
        if (t % 6 == 0) Fx.sound(l, sky, SoundEvents.ENDER_DRAGON_FLAP, 2.5f, 0.8f);
        if (t % 2 == 0) {
            Vec3 aim = g.add(rnd(e).nextGaussian() * 1.2, 0, rnd(e).nextGaussian() * 1.2);
            Combat.shoot(e, DRAGONFIRE, sky, aim.subtract(sky), 1.6, 12);
            if (t % 4 == 0) Fx.sound(l, sky, SoundEvents.BLAZE_SHOOT, 2f, 0.6f);
        }
    }
    static void tStomp(SlayerEntity e, LivingEntity tg, int t) {
        ServerLevel l = L(e);
        if (tg != null && tg.isAlive() && t < 30) store(e, "das_st", tg.position()); // Atlan tracks until committed
        Vec3 c = load(e, "das_st");
        if (t == 2) { snd(e, SoundEvents.WARDEN_ROAR, 1.5f, 0.5f); }
        if (t >= 10 && t < 42) {
            float r = 6 - (t - 10) * 0.12f;
            Fx.ring(l, c.add(0, 0.1, 0), r, 0x101010, 2.5f, 26);
            if (t % 8 == 0) Fx.sound(l, c, SoundEvents.IRON_GOLEM_STEP, 3f, 0.4f);
        }
        if (t >= 32 && t < 42) { // mech foot descending
            double h = 14 - (t - 32) * 1.4;
            Fx.ring(l, c.add(0, h, 0), 2.4f, GUN, 3f, 18); Fx.ring(l, c.add(0, h + 0.8, 0), 2.0f, GREEN, 3f, 14);
            Fx.burst(l, ParticleTypes.FLAME, c.add(0, h + 1.5, 0), 6, 0.6, 0.05);
        }
        if (t == 42) {
            Combat.blast(e, c, 6.5f, 45, 2.2f, null);
            Fx.explosion(l, c, 4.5f); debris(l, c, 3, 80);
            Fx.burst(l, ParticleTypes.SONIC_BOOM, c.add(0, 0.5, 0), 1, 0, 0);
            snd(e, SoundEvents.ANVIL_LAND, 2f, 0.4f); Fx.sound(l, c, SoundEvents.GENERIC_EXPLODE, 3f, 0.55f);
            Fx.sound(l, c, SoundEvents.IRON_GOLEM_DEATH, 2f, 0.4f);
        }
        if (t > 42 && t <= 54) {
            float r = 1 + (t - 42) * 0.8f;
            Fx.ring(l, c.add(0, 0.2, 0), r, 0x7a6a50, 2f, 40); debris(l, c, r * 0.5, 10);
        }
    }

    // =====================================================================================================
    // ANIMATION HELPERS
    // =====================================================================================================
    /** blend weight: ramps in over [0,a], out over [b,1] */
    static float env(float t, float a, float b) { return smooth(win(t, 0, a)) * (1 - smooth(win(t, b, 1))); }
    /** wide heavy stance */
    static void brace(Pose p, float k, float d) {
        p.rootY -= 0.7f * d * k;
        p.rLegX = lerp(p.rLegX, 0.32f * d, k); p.lLegX = lerp(p.lLegX, -0.32f * d, k);
        p.rLegZ = lerp(p.rLegZ, 0.14f * d, k); p.lLegZ = lerp(p.lLegZ, -0.14f * d, k);
        p.bodyX += 0.06f * d * k;
    }
    /** shield forearm across the chest, face forward */
    static void guard(Pose p, float k) {
        p.lArmX = lerp(p.lArmX, -1.45f, k); p.lArmY = lerp(p.lArmY, 0.6f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.lHandZ = lerp(p.lHandZ, -HALF, k);
    }
    /** right hand gun raised, shield forearm braced under it */
    static void gunHold(Pose p, float k, float pitch, float rec) {
        p.rArmX = lerp(p.rArmX, -HALF + pitch - rec * 0.35f, k); p.rArmY = lerp(p.rArmY, -0.12f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -1.15f - rec * 0.15f, k); p.lArmY = lerp(p.lArmY, 0.8f, k); p.lArmZ = lerp(p.lArmZ, 0.05f, k);
        p.rHandX -= rec * 0.45f;
        p.bodyY = lerp(p.bodyY, -0.18f, k); p.bodyX += rec * 0.12f * k; p.headX -= rec * 0.06f;
        p.headY = lerp(p.headY, 0.15f, k);
    }
    static float saw(float age) { return ((int) (age * 3) & 1); }
    static String sawPart(float age) { return saw(age) == 0 ? "shield" : "shield_spin"; }
    static float frac(float x) { return x - (float) Math.floor(x); }
    /** repeating recoil for automatic fire: shots every 'every' ticks between ticks a..b of a dur-tick attack */
    static float autoKick(float t, int dur, int a, int b, int every) {
        float tk = t * dur; if (tk < a || tk > b + every) return 0;
        float u = frac((tk - a) / every); return (1 - u) * (1 - u);
    }

    // =====================================================================================================
    // ANIMATIONS (t = 0..1 over attack)
    // =====================================================================================================
    static void aThrow(Pose p, float t, float age) { // release 9/32 = .28, catch 27/32 = .84
        float k = env(t, 0.12f, 0.9f);
        brace(p, k, 0.6f);
        float tw = track(t, 0, 0, .22f, .75f, .3f, -.55f, .5f, -.35f, .84f, .1f, 1, 0);
        p.bodyY += tw; p.headY -= tw * 0.7f;
        p.bodyX += track(t, 0, 0, .22f, -.12f, .3f, .25f, .6f, .1f, 1, 0);
        p.lArmX = lerp(p.lArmX, track(t, 0, -.4f, .22f, -1.3f, .3f, -1.65f, .45f, -1.2f, .84f, -1.5f, 1, -.4f), k);
        p.lArmY = lerp(p.lArmY, track(t, 0, .3f, .22f, 1.25f, .3f, -.5f, .45f, -.3f, .84f, .5f, 1, .3f), k);
        p.lArmZ = lerp(p.lArmZ, track(t, 0, 0, .22f, .25f, .3f, -.45f, .6f, -.2f, 1, 0), k);
        p.lHandZ = track(t, 0, 0, .2f, -HALF, .3f, -HALF, .35f, 0, .8f, 0, .86f, -.6f, 1, 0);
        p.rArmX = lerp(p.rArmX, track(t, 0, 0, .22f, -.6f, .3f, .35f, .6f, -.2f, 1, 0), k);
        p.rArmZ += track(t, 0, 0, .22f, .2f, .3f, .55f, 1, 0);
        p.lLegX = lerp(p.lLegX, track(t, 0, 0, .22f, .2f, .3f, -.55f, .6f, -.4f, 1, 0), k);
        p.rLegX = lerp(p.rLegX, track(t, 0, 0, .22f, -.1f, .3f, .35f, .6f, .25f, 1, 0), k);
        p.rootZ += track(t, 0, 0, .22f, .8f, .3f, -1.5f, .6f, -1f, 1, 0);
        p.lArmX -= kick(t, .84f, .12f) * 0.5f; p.bodyX -= kick(t, .84f, .12f) * 0.1f;
        if (t > .29f && t < .84f) p.weaponL = null; // the saw is in flight
    }
    static void aParry(Pose p, float t, float age) { // flash at 3/14 = .21
        float k = env(t, 0.12f, 0.75f);
        float snap = outBack(win(t, 0, .2f));
        guard(p, k * snap);
        p.lArmX -= kick(t, .2f, .3f) * 0.35f;                 // shield punch-out
        p.lArmY -= kick(t, .2f, .3f) * 0.25f;
        p.bodyY += track(t, 0, 0, .15f, .35f, .3f, .15f, 1, 0); // lead with the left shoulder
        p.bodyX += track(t, 0, 0, .15f, -.15f, .25f, .2f, .6f, .05f, 1, 0);
        p.headX += 0.25f * k; p.headY -= 0.3f * k;           // tucked behind the rim
        p.rArmX = lerp(p.rArmX, -0.5f, k); p.rArmZ += 0.35f * k; p.rHandX += 0.4f * k;
        brace(p, k, 1f);
        p.lLegX -= 0.25f * kick(t, .18f, .4f); p.rootZ -= 1.2f * kick(t, .2f, .4f);
        p.rootY -= 0.4f * kick(t, .2f, .3f);
    }
    static void aBlock(Pose p, float t, float age) {
        float k = env(t, 0.15f, 0.85f);
        guard(p, k);
        float step = (float) Math.sin(age * 0.45f) * k;
        p.lLegX = lerp(p.lLegX, -0.35f + step * 0.3f, k); p.rLegX = lerp(p.rLegX, 0.3f - step * 0.3f, k);
        p.rLegZ = lerp(p.rLegZ, 0.12f, k); p.lLegZ = lerp(p.lLegZ, -0.12f, k);
        p.rootY -= 0.9f * k - Math.abs(step) * 0.3f;
        p.bodyX += 0.28f * k; p.bodyY += 0.3f * k; p.bodyZ += step * 0.05f;
        p.headX -= 0.1f * k; p.headY -= 0.25f * k;
        p.rArmX = lerp(p.rArmX, -0.85f, k); p.rArmY = lerp(p.rArmY, -0.3f, k); p.rHandX += 0.3f * k; // gun low-ready behind shield
        p.lArmX += (float) Math.sin(age * 1.3f) * 0.02f * k;
    }
    static void aCharge(Pose p, float t, float age) { // dash 5..14 of 22 (.23-.64)
        float k = env(t, 0.1f, 0.8f), dash = smooth(win(t, .2f, .3f)) * (1 - smooth(win(t, .62f, .75f)));
        guard(p, k);
        p.lArmY -= 0.25f * dash; p.lArmX += 0.15f * dash;
        p.bodyX += track(t, 0, 0, .2f, .3f, .3f, .55f, .62f, .55f, .75f, -.1f, 1, 0);
        p.bodyY += 0.5f * k;                                      // shoulder-first
        p.rootPitch += 0.22f * dash; p.rootY -= 0.6f * k;
        float run = (float) Math.sin(age * 1.6f);
        p.rLegX = lerp(p.rLegX, run * 1.1f * dash + .3f * (k - dash), k); p.lLegX = lerp(p.lLegX, -run * 1.1f * dash - .3f * (k - dash), k);
        p.rArmX = lerp(p.rArmX, -run * 0.6f * dash - 0.4f, k); p.rArmZ += 0.3f * k;
        p.headX -= 0.35f * dash;
        float hit = kick(t, .5f, .25f);                           // impact recoil
        p.rootZ += 1.5f * hit; p.bodyX -= 0.25f * hit; p.lArmX -= 0.3f * hit;
    }
    static void aGrind(Pose p, float t, float age) { // grind 6..16, shove 18 of 24
        float k = env(t, 0.2f, 0.85f);
        if (t > .2f && t < .75f) p.weaponL = sawPart(age); else p.weaponL = "shield";
        float shove = kick(t, .73f, .2f);
        p.lArmX = lerp(p.lArmX, -1.55f - shove * 0.25f, k); p.lArmY = lerp(p.lArmY, 0.2f, k); p.lArmZ = lerp(p.lArmZ, -0.15f, k);
        p.lHandZ = -HALF * k;
        float buzz = (float) Math.sin(age * 9) * 0.035f * smooth(win(t, .2f, .3f)) * (1 - smooth(win(t, .68f, .75f)));
        p.lArmX += buzz; p.bodyZ += buzz * 0.6f; p.headZ -= buzz;
        p.bodyY += track(t, 0, 0, .2f, .55f, .7f, .45f, .78f, .7f, 1, 0);
        p.bodyX += track(t, 0, 0, .2f, .25f, .7f, .3f, .78f, .45f, 1, 0);
        p.rArmX = lerp(p.rArmX, -0.3f, k); p.rArmZ += 0.45f * k;
        brace(p, k, 1.1f);
        p.lLegX -= 0.35f * k; p.rootZ -= 1.5f * shove;
    }
    static void aMace(Pose p, float t, float age) { // impact 14/28 = .5
        float k = env(t, 0.08f, 0.88f);
        float arm = track(t, 0, 0, .3f, -2.95f, .4f, -3.05f, .5f, -0.35f, .7f, -0.4f, 1, 0);
        p.rArmX = lerp(p.rArmX, arm, k); p.rArmY = lerp(p.rArmY, -0.2f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, arm * 0.8f, k); p.lArmY = lerp(p.lArmY, 0.55f, k); p.lArmZ = lerp(p.lArmZ, 0, k); // two-handed
        p.rHandX += track(t, 0, 0, .3f, -.6f, .45f, .3f, .5f, .2f, 1, 0);
        p.bodyX += track(t, 0, 0, .3f, -.38f, .4f, -.42f, .5f, .6f, .7f, .5f, 1, 0);
        p.bodyY += track(t, 0, 0, .3f, .2f, .5f, -.1f, 1, 0);
        p.headX += track(t, 0, 0, .3f, -.3f, .5f, .2f, 1, 0);
        p.rootY += track(t, 0, 0, .3f, .5f, .42f, .6f, .5f, -1.6f, .72f, -1.4f, 1, 0);
        p.rootZ += track(t, 0, 0, .3f, .6f, .5f, -1.5f, 1, 0);
        p.lLegX = lerp(p.lLegX, track(t, 0, 0, .3f, -.15f, .5f, -.6f, .75f, -.5f, 1, 0), k);
        p.rLegX = lerp(p.rLegX, track(t, 0, 0, .3f, .1f, .5f, .45f, .75f, .4f, 1, 0), k);
        p.rLegZ += 0.15f * k; p.lLegZ -= 0.15f * k;
        float shake = kick(t, .5f, .2f) * (float) Math.sin(age * 8);
        p.bodyZ += shake * 0.06f; p.rArmX += shake * 0.08f;
    }
    static void aFlailSweep(Pose p, float t, float age) { // spin .27-.65, hit .46
        float k = env(t, 0.12f, 0.85f);
        float spin = outQuad(win(t, .25f, .68f));
        p.rootYaw += -spin * 2 * PI + track(t, 0, 0, .25f, .5f, 1, 0) * (1 - spin);
        p.rArmX = lerp(p.rArmX, track(t, 0, 0, .25f, .3f, .3f, -1.2f, .65f, -1.1f, 1, 0), k);
        p.rArmZ = lerp(p.rArmZ, track(t, 0, 0, .25f, .3f, .32f, 1.35f, .65f, 1.2f, .85f, .2f, 1, 0), k); // arm flung out
        p.rHandX += track(t, 0, 0, .3f, -.8f, .65f, -.8f, 1, 0); // flail extended outward
        p.lArmX = lerp(p.lArmX, -0.9f, k); p.lArmY = lerp(p.lArmY, 0.7f, k); p.lArmZ = lerp(p.lArmZ, -0.35f, k);
        p.bodyZ += 0.15f * smooth(win(t, .3f, .4f)) * (1 - smooth(win(t, .6f, .75f)));
        p.bodyY += track(t, 0, 0, .25f, .6f, .35f, -.3f, .7f, -.1f, 1, 0);
        p.bodyX += 0.15f * k;
        brace(p, k, 1.2f);
        float shuffle = (float) Math.sin(spin * PI * 4) * 0.25f * k;
        p.rLegX += shuffle; p.lLegX -= shuffle;
    }
    static void aFlailLash(Pose p, float t, float age) { // crack 10/22 = .45
        float k = env(t, 0.1f, 0.85f);
        float tw = track(t, 0, 0, .3f, .6f, .45f, -.45f, .6f, -.35f, 1, 0);
        p.bodyY += tw; p.headY -= tw * 0.6f;
        p.rArmX = lerp(p.rArmX, track(t, 0, 0, .3f, -2.6f, .45f, -1.35f, .6f, -1.2f, 1, 0), k);
        p.rArmZ = lerp(p.rArmZ, track(t, 0, 0, .3f, .35f, .45f, -.1f, 1, 0), k);
        float whip = kick(t, .45f, .3f);
        p.rHandX += track(t, 0, 0, .3f, -1.1f, .45f, .4f, 1, 0) + (float) Math.sin(age * 5) * whip * 0.3f;
        p.lArmX = lerp(p.lArmX, -0.9f, k); p.lArmY = lerp(p.lArmY, 0.6f, k);
        p.lLegX = lerp(p.lLegX, track(t, 0, 0, .3f, .1f, .45f, -.55f, .7f, -.45f, 1, 0), k);
        p.rLegX = lerp(p.rLegX, track(t, 0, 0, .45f, .35f, 1, 0), k);
        p.bodyX += track(t, 0, 0, .3f, -.15f, .45f, .25f, 1, 0);
        p.rootZ += track(t, 0, 0, .3f, .6f, .45f, -1.2f, 1, 0); p.rootY -= 0.4f * k;
    }
    static void aGauntlet(Pose p, float t, float age) { // hits .22 (R straight) .47 (L shield hook) .78 (R uppercut)
        float k = env(t, 0.08f, 0.9f);
        brace(p, k, 0.9f);
        float tw = track(t, 0, 0, .14f, .35f, .22f, -.45f, .38f, .5f, .47f, .55f, .62f, .3f, .7f, .5f, .78f, -.5f, .9f, -.3f, 1, 0);
        p.bodyY += tw; p.headY -= tw * 0.7f;
        // right: chamber -> straight -> chamber -> uppercut
        p.rArmX = lerp(p.rArmX, track(t, 0, -.3f, .14f, -.6f, .22f, -1.7f, .32f, -.7f, .62f, -.4f, .7f, -.2f, .78f, -2.6f, .9f, -2.4f, 1, -.3f), k);
        p.rArmY = lerp(p.rArmY, track(t, 0, 0, .22f, .15f, .7f, -.2f, .78f, .1f, 1, 0), k);
        p.rArmZ = lerp(p.rArmZ, track(t, 0, .1f, .62f, .1f, .7f, .35f, .78f, 0, 1, .1f), k);
        // left: guard -> big shield hook across
        p.lArmX = lerp(p.lArmX, track(t, 0, -1.1f, .3f, -1.1f, .4f, -1.0f, .47f, -1.6f, .58f, -1.5f, .7f, -1.1f, 1, -.4f), k);
        p.lArmY = lerp(p.lArmY, track(t, 0, .6f, .38f, -.4f, .47f, 1.0f, .58f, .9f, .7f, .6f, 1, .3f), k);
        p.lArmZ = lerp(p.lArmZ, track(t, 0, 0, .38f, -.6f, .47f, -.1f, 1, 0), k);
        p.lHandZ = -HALF * k;
        p.bodyX += track(t, 0, 0, .22f, .2f, .47f, .25f, .7f, .35f, .78f, -.25f, 1, 0);
        p.rootY += track(t, 0, 0, .7f, -.9f, .78f, .8f, .9f, .4f, 1, 0); // drop then explode upward
        p.rootZ += track(t, 0, 0, .22f, -1f, .3f, 0, .47f, -1f, .55f, 0, .78f, -1.4f, 1, 0);
        p.lLegX += track(t, 0, 0, .22f, -.3f, .47f, -.1f, .78f, -.45f, 1, 0) * k;
        p.rLegX += track(t, .4f, 0, .47f, .3f, .6f, 0) * k;
    }
    static void aShotgun(Pose p, float t, float age) { // fire 4/16 = .25, pump .62
        float k = env(t, 0.15f, 0.88f);
        float rec = kick(t, .25f, .3f);
        gunHold(p, k, 0, rec);
        brace(p, k, 0.7f);
        float pump = bump(win(t, .5f, .78f));
        p.rHandX += pump * 0.25f; p.rArmX += pump * 0.15f; p.lArmX += pump * 0.2f; p.lArmY += pump * 0.1f; // rack it
        p.bodyY -= rec * 0.15f; p.rootZ += rec * 0.9f; p.headZ += pump * 0.05f;
    }
    static void aSsg(Pose p, float t, float age) { // blast 6/26 = .23, break .45-.75
        float k = env(t, 0.15f, 0.88f);
        float rec = kick(t, .23f, .35f);
        gunHold(p, k, 0, rec * 1.8f);
        brace(p, k, 1.2f);
        p.bodyX -= rec * 0.25f; p.rootZ += rec * 2.2f; p.rootPitch -= rec * 0.08f;
        p.lLegX -= rec * 0.3f;
        float brk = smooth(win(t, .45f, .55f)) * (1 - smooth(win(t, .7f, .78f)));
        p.rHandX += brk * 0.9f;                               // barrels drop open
        p.rArmX += brk * 0.5f; p.rArmY += brk * 0.4f;         // bring to chest
        p.lArmX = lerp(p.lArmX, -0.9f, brk); p.lArmY = lerp(p.lArmY, 1.0f, brk); // shield hand loads shells
        p.headX += brk * 0.35f; p.headY -= brk * 0.2f;
        p.rHandX -= kick(t, .73f, .15f) * 0.4f;              // snap shut
    }
    static void aAccel(Pose p, float t, float age) { // spin-up then shots every 3 from .31
        float k = env(t, 0.15f, 0.9f);
        float rec = autoKick(t, 32, 10, 28, 3) * 0.35f;
        gunHold(p, k, -0.03f, rec);
        brace(p, k, 0.8f);
        float spin = (1 - win(t, 0, .3f)) * k;
        p.rHandZ += (float) Math.sin(age * 4) * 0.04f * spin;  // charging hum
        p.headX += 0.1f * k; p.headZ += 0.08f * k;             // sighting down the rifle
        p.bodyZ += rec * 0.03f;
    }
    static void aPulv(Pose p, float t, float age) { // blast 16/34 = .47
        float k = env(t, 0.12f, 0.88f);
        float rec = kick(t, .47f, .4f) * 2.2f;
        gunHold(p, k, 0.25f, rec);                            // hip-held cannon
        p.rArmY += 0.25f * k; p.lArmX += 0.3f * k;
        brace(p, k, 1.4f);
        float grind = (float) Math.sin(age * 6) * 0.03f * smooth(win(t, .1f, .2f)) * (1 - win(t, .45f, .47f));
        p.bodyZ += grind; p.rArmX += grind;
        p.rootZ += rec * 1.4f; p.bodyX -= rec * 0.12f; p.rootPitch -= rec * 0.05f;
        p.lLegX -= rec * 0.2f; p.rLegX += rec * 0.1f;
    }
    static void aShred(Pose p, float t, float age) { // spray 6..30 of 36
        float k = env(t, 0.12f, 0.9f);
        float rec = autoKick(t, 36, 6, 30, 2) * 0.25f;
        gunHold(p, k, 0.1f, rec);
        brace(p, k, 1f);
        float sweep = (float) Math.sin(win(t, .15f, .85f) * PI * 2) * 0.25f * k; // hose across the crowd
        p.bodyY += sweep; p.headY -= sweep * 0.5f;
        float vib = (float) Math.sin(age * 11) * 0.03f * k;
        p.rArmX += vib; p.lArmX += vib; p.bodyZ += vib * 0.5f;
    }
    static void aCycler(Pose p, float t, float age) { // arcs 6..24 of 30
        float k = env(t, 0.15f, 0.9f);
        float live = smooth(win(t, .18f, .22f)) * (1 - smooth(win(t, .78f, .84f)));
        float jit = (float) Math.sin(age * 13) * 0.045f * live;
        gunHold(p, k, 0, live * 0.15f);
        brace(p, k, 0.9f);
        p.rArmX += jit; p.rArmY += jit * 0.6f; p.rHandZ += jit * 2; p.bodyZ += jit * 0.4f;
        p.headX += 0.08f * k; p.bodyX -= 0.05f * live;        // leaning back against the current
    }
    static void aRocket(Pose p, float t, float age) { // fire 9/26 = .35
        float k = env(t, 0.18f, 0.85f);
        float rec = kick(t, .35f, .35f);
        p.rArmX = lerp(p.rArmX, -2.1f - rec * 0.3f, k); p.rArmY = lerp(p.rArmY, 0.1f, k); p.rArmZ = lerp(p.rArmZ, 0.25f, k);
        p.rHandX += 0.55f * k;                                // tube over the shoulder, pointing forward
        p.lArmX = lerp(p.lArmX, -1.2f, k); p.lArmY = lerp(p.lArmY, 0.9f, k);
        p.headY = lerp(p.headY, 0.25f, k); p.headZ += 0.12f * k;
        brace(p, k, 1.1f);
        p.bodyY -= 0.2f * k; p.bodyX += rec * 0.1f - 0.05f * k; p.rootZ += rec * 1.6f;
    }
    static void aChainshot(Pose p, float t, float age) { // wind-up whirl, fire 12/30 = .4
        float k = env(t, 0.12f, 0.88f);
        float rec = kick(t, .4f, .35f) * 1.4f;
        gunHold(p, k, 0.05f, rec);
        float whirl = smooth(win(t, .08f, .15f)) * (1 - smooth(win(t, .36f, .4f)));
        p.rHandZ += (float) Math.sin(age * 2.5f) * 0.35f * whirl;  // spinning the chain drum
        p.rArmZ += (float) Math.cos(age * 2.5f) * 0.12f * whirl;
        brace(p, k, 1f);
        p.bodyY -= track(t, 0, 0, .35f, -.25f, .42f, .2f, 1, 0); p.rootZ += rec;
    }
    static void aImpaler(Pose p, float t, float age) { // fire 13/28 = .46
        float k = env(t, 0.2f, 0.88f);
        float rec = kick(t, .46f, .25f) * 1.5f;
        gunHold(p, k, -0.05f, rec);
        p.headZ += 0.2f * k; p.headX += 0.12f * k; p.headY = lerp(p.headY, 0.3f, k); // cheek to the stock
        p.rootY -= 1.0f * k; p.lLegX = lerp(p.lLegX, -0.6f, k); p.rLegX = lerp(p.rLegX, 0.6f, k); // low sniper stance
        p.bodyX += 0.12f * k; p.bodyY -= 0.1f * k;
        float breath = (float) Math.sin(age * 0.3f) * 0.015f * (1 - win(t, .35f, .45f));
        p.rArmX += breath; p.rootZ += rec * 0.8f;
    }
    static void aGkBash(Pose p, float t, float age) { // bash .2, gut punch .45, hammer-fist finish .75
        float k = env(t, 0.08f, 0.9f);
        brace(p, k, 1f);
        // stage 1: shield bash
        p.lArmX = lerp(p.lArmX, track(t, 0, -.5f, .12f, -1.0f, .2f, -1.6f, .3f, -1.4f, .4f, -.9f, .62f, -2.7f, .75f, -.5f, .9f, -.6f, 1, -.4f), k);
        p.lArmY = lerp(p.lArmY, track(t, 0, .3f, .12f, .9f, .2f, .3f, .4f, .5f, .62f, .2f, 1, .3f), k);
        p.lHandZ = -HALF * k * (1 - win(t, .5f, .6f));
        // stage 2: gauntlet gut punch; stage 3: both arms overhead -> hammer down
        p.rArmX = lerp(p.rArmX, track(t, 0, -.2f, .3f, -.4f, .38f, -.2f, .45f, -1.6f, .52f, -1.3f, .62f, -2.8f, .75f, -.6f, .9f, -.7f, 1, -.2f), k);
        p.rArmY = lerp(p.rArmY, track(t, 0, 0, .62f, -.35f, .75f, -.2f, 1, 0), k);
        p.bodyY += track(t, 0, 0, .12f, -.3f, .2f, .45f, .38f, .45f, .45f, -.45f, .55f, -.2f, .75f, 0, 1, 0);
        p.bodyX += track(t, 0, 0, .2f, .25f, .45f, .35f, .62f, -.4f, .75f, .7f, .9f, .55f, 1, 0);
        p.rootY += track(t, 0, 0, .62f, .7f, .75f, -1.8f, .9f, -1.5f, 1, 0);
        p.rootZ += track(t, 0, 0, .2f, -1.2f, .45f, -1.6f, .75f, -2.2f, 1, 0);
        p.lLegX += track(t, 0, 0, .2f, -.4f, .45f, -.3f, .75f, -.6f, 1, 0) * k;
        p.headX += track(t, 0, 0, .62f, -.3f, .75f, .35f, 1, 0);
        float shake = kick(t, .75f, .15f) * (float) Math.sin(age * 9) * 0.08f; p.bodyZ += shake;
    }
    static void aGkSaw(Pose p, float t, float age) { // grab .05-.2, saw in .27-.64, kick-off .75
        float k = env(t, 0.08f, 0.9f);
        boolean grinding = t > .25f && t < .66f;
        p.weaponL = grinding ? sawPart(age) : "shield";
        brace(p, k, 1.1f);
        // right hand grabs and holds the demon
        p.rArmX = lerp(p.rArmX, track(t, 0, 0, .12f, -1.7f, .66f, -1.6f, .72f, -.6f, 1, 0), k);
        p.rArmY = lerp(p.rArmY, track(t, 0, 0, .12f, .35f, .66f, .3f, 1, 0), k);
        p.rHandX += 0.6f * k;
        // shield saw driven in edge-first, vibrating
        float buzz = grinding ? (float) Math.sin(age * 10) * 0.05f : 0;
        p.lArmX = lerp(p.lArmX, track(t, 0, -.4f, .15f, -.9f, .25f, -1.1f, .3f, -1.55f, .64f, -1.7f, .72f, -1.0f, 1, -.4f), k) + buzz;
        p.lArmY = lerp(p.lArmY, track(t, 0, .2f, .25f, -.3f, .3f, .35f, .64f, .45f, 1, .3f), k);
        p.lHandZ = 0;
        p.bodyY += track(t, 0, 0, .25f, -.35f, .32f, .4f, .64f, .45f, .72f, 0, 1, 0) + buzz;
        p.bodyX += track(t, 0, 0, .3f, .3f, .64f, .45f, .72f, -.15f, 1, 0);
        p.headX += 0.2f * k; p.headZ += buzz;
        // kick-off with the right leg
        p.rLegX = lerp(p.rLegX, track(t, 0, .3f, .66f, .4f, .72f, .7f, .76f, -1.5f, .88f, -1.0f, 1, 0), k);
        p.rootY += track(t, 0, 0, .7f, .4f, .76f, .6f, .9f, 0, 1, 0);
        p.rootPitch -= 0.12f * kick(t, .75f, .2f);
    }
    static void aStrafe(Pose p, float t, float age) { // call it in .04-.28, pass .28-.75
        float k = env(t, 0.1f, 0.9f);
        brace(p, k, 0.8f);
        float call = smooth(win(t, .04f, .14f)) * (1 - smooth(win(t, .3f, .4f)));
        p.lArmX = lerp(p.lArmX, -2.9f, call); p.lArmZ = lerp(p.lArmZ, -0.3f, call); p.lArmY = lerp(p.lArmY, 0, call);
        p.lArmX += (float) Math.sin(age * 0.9f) * 0.1f * call;  // waving the shield overhead
        float point = smooth(win(t, .25f, .35f)) * (1 - smooth(win(t, .85f, .95f)));
        p.rArmX = lerp(p.rArmX, -1.75f, point); p.rArmY = lerp(p.rArmY, -0.1f, point); p.rHandX += 0.9f * point; // gun tilted up: "there"
        p.headX += -0.7f * call + track(t, .3f, -.5f, .75f, .1f, 1, 0) * (1 - call) * k; // eyes to the sky, then track the pass
        p.headY += track(t, .3f, .3f, .75f, -.3f, 1, 0) * k;
        p.bodyX -= 0.15f * call; p.bodyY += 0.2f * point;
        guard(p, smooth(win(t, .42f, .5f)) * (1 - smooth(win(t, .8f, .9f)))); // shield up against the blast debris
    }
    static void aStomp(Pose p, float t, float age) { // signal .03-.3, impact 42/64 = .66, rise .8-1
        float k = env(t, 0.08f, 0.92f);
        float sig = smooth(win(t, .03f, .1f)) * (1 - smooth(win(t, .3f, .38f)));
        p.rArmX = lerp(p.rArmX, -2.6f, sig); p.rArmZ = lerp(p.rArmZ, 0.4f, sig); p.rHandX += 1.2f * sig; // fist raised: summon the Atlan
        p.headX -= 0.6f * sig;
        float cover = smooth(win(t, .45f, .6f)) * (1 - smooth(win(t, .82f, .95f)));
        guard(p, cover);
        p.lArmX -= 0.6f * cover;                               // shield over the head
        float kneel = smooth(win(t, .58f, .66f)) * (1 - smooth(win(t, .82f, .96f)));
        p.rootY -= 3.2f * kneel; p.rLegX = lerp(p.rLegX, 1.2f, kneel); p.lLegX = lerp(p.lLegX, -1.3f, kneel); // one knee down
        p.bodyX += 0.4f * kneel; p.headX += 0.3f * kneel;
        p.rArmX = lerp(p.rArmX, -0.3f, kneel); p.rArmZ = lerp(p.rArmZ, 0.5f, kneel);
        float quake = kick(t, .66f, .25f) * (float) Math.sin(age * 10);
        p.rootX += quake * 0.6f; p.bodyZ += quake * 0.05f;
        brace(p, k * (1 - kneel), 1f);
    }

    // =====================================================================================================
    // IDLE
    // =====================================================================================================
    @Override
    public void idle(Pose p, SlayerEntity e, float age, float limbAmt) {
        float br = (float) Math.sin(age * 0.08f);
        p.lArmX += -0.35f; p.lArmY += 0.3f; p.lArmZ -= 0.12f;        // shield carried forward
        p.rArmZ += 0.1f; p.rArmX -= 0.15f; p.rHandX += 0.25f;          // shotgun low-ready
        p.bodyX += 0.05f + br * 0.012f; p.rootY -= 0.25f;              // heavy, hunched
        p.rLegZ += 0.05f; p.lLegZ -= 0.05f; p.headX += 0.05f;
        p.bodyZ += (float) Math.sin(age * 0.33f) * 0.05f * limbAmt;    // armoured sway when walking
        p.rootY -= limbAmt * 0.25f;                                    // heavy footfalls
    }

    // =====================================================================================================
    // MODEL
    // =====================================================================================================
    @Override
    public void buildModel(Rig rig) {
        Mat green = Mat.of(GREEN).noise(.1f).bevel(.25f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.FRONT && w >= 6 && h >= 5) { g.rect(x, y + 1, w, 1, GOLD); g.rect(x + w / 2, y + 1, 1, h - 1, Mat.mix(GOLD, GREEN, .4f)); }
        });
        Mat greenD = Mat.of(GREEN_D).noise(.12f).bevel(.2f);
        Mat gun = Mat.of(GUN).noise(.08f).bevel(.3f);
        Mat gunD = Mat.of(GUN_D).noise(.1f).bevel(.2f);
        Mat gold = Mat.of(GOLD).noise(.06f).bevel(.35f).grad(.25f);
        Mat leather = Mat.of(LEATHER).noise(.18f);
        Mat suit = Mat.of(0x2b2b27).noise(.15f).bevel(.1f);
        Mat cloth = Mat.of(0x3a3326).noise(.25f).decal((m, g, x, y, w, h, f) -> {
            for (int i = 0; i < w; i += 2) g.rect(x + i, y + h - 1 - (i % 3), 1, 1 + (i % 3), 0x1e1a14); // ragged hem
        });
        Mat visor = Mat.of(VISOR).glow();
        Mat glowG = Mat.of(PARRY).glow();
        Mat hot = Mat.of(HOT).glow();
        Mat steel = Mat.of(0x8b9198).noise(.06f).bevel(.3f);
        Mat wood = Mat.of(0x5a3d22).noise(.2f);
        Mat red = Mat.of(0xff3020).glow();
        Mat cyan = Mat.of(0x60e0ff).glow();

        // ---- torso: bulky plate ----
        rig.box(rig.torso, suit, -4, -12, -2, 8, 12, 4, 0.5f);
        rig.box(rig.torso, green, -4.5f, -12.3f, -3.2f, 9, 6, 6, 0.2f);      // chest cuirass
        rig.box(rig.torso, gunD, -3.5f, -6, -2.9f, 7, 4, 1);                 // ab plates
        rig.box(rig.torso, gold, -4.5f, -6.6f, -3.5f, 9, 1, 1);               // gold trim under the chest
        rig.box(rig.torso, leather, -4.5f, -2, -2.7f, 9, 2, 5, 0.2f);         // belt
        rig.box(rig.torso, gold, -1.5f, -2.2f, -3.3f, 3, 2, 1);               // buckle
        rig.box(rig.torso, gunD, -3.5f, -11.5f, 2.2f, 7, 7, 2);               // back plate / power pack
        rig.box(rig.torso, glowG, -2.5f, -9.5f, 4.1f, 1, 3, 1);               // pack vents
        rig.box(rig.torso, glowG, 1.5f, -9.5f, 4.1f, 1, 3, 1);
        rig.box(rig.torso, gun, -3, -13.5f, -2.5f, 6, 2, 5);                   // gorget
        rig.box(rig.torso, greenD, -4, 0, -2.9f, 3, 4, 1);                     // tassets
        rig.box(rig.torso, greenD, 1, 0, -2.9f, 3, 4, 1);
        rig.box(rig.torso, cloth, -4, -11, 4.2f, 8, 15, 1);                    // tattered cape
        // ---- head: heavy great-helm ----
        rig.box(rig.head, green, -4, -8, -4, 8, 8, 8, 0.4f);
        rig.box(rig.head, visor, -3, -5.2f, -4.6f, 6, 2, 1);                   // gold-amber visor slit
        rig.box(rig.head, gunD, -3, -3.2f, -4.7f, 6, 3, 1);                    // jaw guard
        rig.box(rig.head, gold, -0.5f, -9.2f, -4.5f, 1, 2, 8);                 // crest ridge
        rig.box(rig.head, gun, -5.2f, -6.5f, -2.5f, 1, 4, 5);                  // cheek flares
        rig.box(rig.head, gun, 4.2f, -6.5f, -2.5f, 1, 4, 5);
        rig.box(rig.head, gold, -4, -6.6f, -4.7f, 8, 1, 1);                    // brow band
        // ---- arms ----
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            float o = s * 1f;                                                 // outward offset
            rig.box(arm, suit, -2, -2, -2, 4, 12, 4, 0.35f);
            rig.box(arm, green, -3 + o, -3.8f, -3, 6, 4, 6, 0.2f);             // massive pauldron
            rig.box(arm, gold, -2.5f + o, 0, -2.6f, 5, 1, 5);                   // pauldron rim
            rig.box(arm, gun, -2.5f, 5, -2.5f, 5, 5, 5);                        // vambrace / gauntlet
            rig.box(arm, gold, -2.6f, 9.6f, -2.6f, 5, 1, 5);                    // cuff band
        }
        // ---- legs ----
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, suit, -2, 0, -2, 4, 12, 4, 0.3f);
            rig.box(leg, green, -2.5f, 0.5f, -2.9f, 5, 5, 5);                  // cuisse
            rig.box(leg, gold, -1.5f, 5.4f, -3.2f, 3, 2, 1);                    // knee cop
            rig.box(leg, gun, -2.5f, 7, -2.7f, 5, 3, 5);                        // greave
            rig.box(leg, gunD, -2.6f, 10, -3.6f, 5, 2, 6, 0.1f);                // sabaton
        }

        // ---- Shield Saw (left arm, gun space: thin in x, sits on the outer forearm) ----
        PartDefinition sh = rig.weapon(rig.lHand, "shield");
        rig.box(sh, gun, 2f, -5, -2, 1, 10, 10);
        rig.box(sh, gold, 2.6f, -2, 1, 1, 4, 4);                              // boss
        rig.box(sh, glowG, 3.2f, -1, 2, 1, 2, 2);                              // argent core
        rig.box(sh, steel, 2f, -6, 0, 1, 1, 6);                                // saw teeth (edges)
        rig.box(sh, steel, 2f, 5, 0, 1, 1, 6);
        rig.box(sh, steel, 2f, -3, -3, 1, 6, 1);
        rig.box(sh, steel, 2f, -3, 8, 1, 6, 1);
        // spinning frame: same disc rotated 45 deg with glowing hot teeth (anim alternates the two)
        PartDefinition sp = rig.pivot(rig.weapon(rig.lHand, "shield_spin"), "spin", 2.5f, 0, 3, PI / 4, 0, 0);
        rig.box(sp, gun, -0.5f, -5, -5, 1, 10, 10);
        rig.box(sp, gold, 0.1f, -2, -2, 1, 4, 4);
        rig.box(sp, hot, -0.5f, -6, -3, 1, 1, 6);
        rig.box(sp, hot, -0.5f, 5, -3, 1, 1, 6);
        rig.box(sp, hot, -0.5f, -3, -6, 1, 6, 1);
        rig.box(sp, hot, -0.5f, -3, 5, 1, 6, 1);

        // ---- right hand weapons (gun space: barrel -z, y down) ----
        PartDefinition w;
        w = rig.weapon(rig.rHand, "cshotgun");
        rig.box(w, gun, -1, -3, -3, 2, 3, 7);
        rig.box(w, gunD, -0.5f, -3.6f, -12, 1, 1, 9);
        rig.box(w, gunD, -0.5f, -2.3f, -10, 1, 1, 7);
        rig.box(w, wood, -1, -2.5f, 4, 2, 2, 4);
        rig.box(w, gold, -1.2f, -3.3f, -6, 2, 2, 1);
        w = rig.weapon(rig.rHand, "sshotgun");
        rig.box(w, gun, -1.5f, -3, -2, 3, 3, 6);
        rig.box(w, gunD, -1.5f, -3.5f, -12, 1, 2, 10);
        rig.box(w, gunD, 0.5f, -3.5f, -12, 1, 2, 10);
        rig.box(w, gold, -1.6f, -3.3f, -4, 3, 1, 2);
        rig.box(w, wood, -1, -2, 4, 2, 2, 4);
        w = rig.weapon(rig.rHand, "accelerator");
        rig.box(w, gun, -1.5f, -3.5f, -6, 3, 3, 9);
        rig.box(w, glowG, -1, -3, -9, 2, 2, 3);
        rig.box(w, gold, -2, -4.2f, -4, 4, 1, 3);
        rig.box(w, gunD, -0.5f, -0.5f, 0, 1, 3, 2);
        w = rig.weapon(rig.rHand, "pulverizer");
        rig.box(w, gun, -2.5f, -5, -4, 5, 5, 7);
        rig.box(w, gunD, -1.5f, -4, -9, 3, 3, 5);
        rig.box(w, red, -1, -3.5f, -9.4f, 2, 2, 1);
        rig.box(w, gold, -2.7f, -5.2f, -2, 5, 1, 2);
        w = rig.weapon(rig.rHand, "shredder");
        rig.box(w, gun, -1.5f, -3.5f, -5, 3, 3, 8);
        rig.box(w, hot, -0.5f, -5.5f, -9, 1, 4, 4);
        rig.box(w, gunD, -2.5f, -2, -3, 1, 3, 4);
        rig.box(w, gold, -1.6f, -3.8f, -6, 3, 1, 1);
        w = rig.weapon(rig.rHand, "cycler");
        rig.box(w, gun, -1, -3, -6, 2, 3, 9);
        rig.box(w, cyan, -1.5f, -3.5f, -5, 3, 1, 1);
        rig.box(w, cyan, -1.5f, -3.5f, -3, 3, 1, 1);
        rig.box(w, cyan, -1.5f, -3.5f, -1, 3, 1, 1);
        rig.box(w, gold, -1, -3, -9, 1, 1, 3);
        rig.box(w, gold, 0.5f, -3, -9, 1, 1, 3);
        w = rig.weapon(rig.rHand, "rocket");
        rig.box(w, green, -2, -5, -8, 4, 4, 12);
        rig.box(w, gold, -2.2f, -5.2f, -9, 4, 4, 1, 0.2f);
        rig.box(w, gunD, -0.5f, -1, -2, 1, 3, 2);
        w = rig.weapon(rig.rHand, "chainshot");
        rig.box(w, gun, -1.5f, -3.5f, -4, 3, 3, 7);
        rig.box(w, gunD, -2, -4, -9, 4, 4, 4);
        rig.box(w, steel, -0.5f, -2.5f, -6, 1, 1, 2);
        rig.box(w, gold, -1.6f, -3.7f, -1, 3, 1, 1);
        w = rig.weapon(rig.rHand, "impaler");
        rig.box(w, gun, -1, -3, -14, 2, 2, 14);
        rig.box(w, hot, -0.5f, -2.5f, -16, 1, 1, 6);
        rig.box(w, gunD, -0.5f, -4.2f, -6, 1, 1, 4);
        rig.box(w, wood, -1, -2.5f, 0, 2, 2, 4);
        // melee
        w = rig.weapon(rig.rHand, "mace");
        rig.box(w, leather, -0.5f, -0.5f, -10, 1, 1, 13);
        rig.box(w, steel, -2, -2, -15, 4, 4, 5);
        rig.box(w, gold, -3, -0.5f, -14, 6, 1, 3);
        rig.box(w, gold, -0.5f, -3, -14, 1, 6, 3);
        rig.box(w, steel, -0.5f, -0.5f, -17, 1, 1, 2);
        w = rig.weapon(rig.rHand, "flail");
        rig.box(w, leather, -0.5f, -0.5f, -6, 1, 1, 8);
        rig.box(w, steel, -0.3f, -0.3f, -10, 1, 1, 4);
        rig.box(w, gunD, -2, -2, -14, 4, 4, 4);
        rig.box(w, steel, -3, -0.5f, -12.5f, 6, 1, 1);
        rig.box(w, steel, -0.5f, -3, -12.5f, 1, 6, 1);
        rig.box(w, glowG, -1, -1, -14.4f, 2, 2, 1);
        w = rig.weapon(rig.rHand, "gauntlet");
        rig.box(w, gun, -2.8f, -2.8f, -3, 6, 6, 5);
        rig.box(w, gold, -2.5f, -3.2f, -3.6f, 5, 1, 1);
        rig.box(w, glowG, -2, -1, 2, 4, 1, 1);
    }
}
