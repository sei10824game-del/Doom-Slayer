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
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The Doom Slayer, DOOM Eternal: Praetor Suit (Eternal pattern), left-shoulder launcher (Frag / Ice Bomb / Flame Belch),
 * wrist Blood Punch + Doom Blade, Crucible on the back, full Eternal arsenal with every weapon mod.
 */
public class DoomEternalSlayer extends SlayerKit {

    // ---------------- projectile styles (static: registration order syncs ids) ----------------
    static final ProjStyle FRAG = ProjStyle.make().color(0x9aa860).size(0.28f).gravity(0.045).bounce().life(32).explode(3.6f).trail(ParticleTypes.SMOKE);
    static final ProjStyle ICE_BOMB = ProjStyle.make().color(0x9fe6ff).size(0.42f).gravity(0.04).life(34).explode(3.5f).trail(ParticleTypes.SNOWFLAKE)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.burst(l, ParticleTypes.SNOWFLAKE, pos, 60, 1.6, 0.08);
                Fx.burst(l, ParticleTypes.ITEM_SNOWBALL, pos, 30, 1.2, 0.2);
                Fx.ring(l, pos.add(0, 0.2, 0), 4.5f, 0xbff4ff, 1.6f, 36);
                Fx.sound(l, pos, SoundEvents.GLASS_BREAK, 1.4f, 0.7f);
                Fx.sound(l, pos, SoundEvents.POWDER_SNOW_BREAK, 1.4f, 0.6f);
                if (p.getOwner() instanceof SlayerEntity o) for (LivingEntity f : Combat.around(o, pos, 4.8)) {
                    f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 6));
                    f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                    f.setTicksFrozen(Math.max(f.getTicksFrozen(), 340));
                    Fx.dustBurst(l, 0xd8faff, 1.4f, f.position().add(0, f.getBbHeight() / 2, 0), 14, f.getBbWidth() * 0.6);
                }
            });
    /** sticky bomb glued to a demon: follows it, blinks, then pops */
    static final ProjStyle STUCK = ProjStyle.make().color(0xff5a10).size(0.24f).life(22).explode(2.8f)
            .onTick(p -> {
                Entity t = p.level().getEntity(p.aux);
                if (t != null && t.isAlive()) p.setPos(t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ());
                p.setDeltaMovement(Vec3.ZERO);
                if (p.age2 % 5 == 0) {
                    Fx.dustBurst((ServerLevel) p.level(), 0xff2000, 1.3f, p.position(), 3, 0.05);
                    Fx.sound((ServerLevel) p.level(), p.position(), SoundEvents.STONE_BUTTON_CLICK_ON, 0.6f, 2f);
                }
            });
    static final ProjStyle STICKY = ProjStyle.make().color(0xffa040).size(0.2f).life(30).gravity(0.012).trail(ParticleTypes.SMALL_FLAME)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                if (!(p.getOwner() instanceof SlayerEntity o)) return;
                if (hit instanceof LivingEntity) {
                    SlayerProjectile s = Combat.shoot(o, STUCK, pos, new Vec3(0, 1, 0), 0, 12f);
                    s.aux = hit.getId();
                    Fx.sound(l, pos, SoundEvents.SLIME_SQUISH, 1f, 1.4f);
                } else {
                    Fx.explosion(l, pos, 1.6f); Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.5f);
                    Combat.blast(o, pos, 2.6f, 9f, 0.6f, null);
                }
            });
    static final ProjStyle MICRO = ProjStyle.make().color(0xffcc66).size(0.2f).life(40).homing(0.18f).explode(1.7f).trail(ParticleTypes.SMOKE);
    static final ProjStyle LOCKON = ProjStyle.make().color(0xff7030).size(0.34f).life(70).homing(0.3f).explode(2.6f).trail(ParticleTypes.LARGE_SMOKE);
    /** rocket with remote detonation: blows when a demon is within reach of the warhead */
    static final ProjStyle REMOTE = ProjStyle.make().color(0xffa030).size(0.5f).life(60).explode(4.6f).trail(ParticleTypes.FLAME)
            .onTick(p -> {
                if (p.age2 > 3 && p.getOwner() instanceof SlayerEntity o && !Combat.around(o, p.position(), 2.8).isEmpty()) {
                    Fx.sound((ServerLevel) p.level(), p.position(), SoundEvents.STONE_BUTTON_CLICK_ON, 1.5f, 1.8f);
                    Fx.burst((ServerLevel) p.level(), ParticleTypes.FLASH, p.position(), 1, 0, 0);
                    p.age2 = 100000;
                }
            });
    static final ProjStyle BOLT = ProjStyle.make().color(0xff7a20).size(0.3f).life(30).explode(2.6f).trail(ParticleTypes.FLAME);
    static final ProjStyle BFG_ORB = ProjStyle.make().color(0x50ff50).size(1.8f).life(70).explode(7f).trail(ParticleTypes.HAPPY_VILLAGER)
            .onTick(p -> {
                if (p.age2 % 3 != 0 || !(p.getOwner() instanceof SlayerEntity o)) return;
                ServerLevel l = (ServerLevel) p.level();
                for (LivingEntity f : Combat.around(o, p.position(), 8)) {
                    Fx.beam(l, p.position(), f.position().add(0, f.getBbHeight() * 0.5, 0), 0x70ff70, 1.3f, 0.35);
                    Combat.damage(o, f, 2.5f);
                }
                Fx.burst(l, ParticleTypes.GLOW, p.position(), 6, 0.8, 0.05);
                if (p.age2 % 9 == 0) Fx.sound(l, p.position(), SoundEvents.BEACON_AMBIENT, 1.5f, 1.6f);
            });

    private static final String FUEL = "dese_fuel";

    public DoomEternalSlayer() {
        super("doom_eternal_slayer", "Doom Slayer", "ドゥームスレイヤー", "DOOM Eternal");
        eggPrimary = 0x46552e; eggSecondary = 0xffb020;
        maxHealth = 100; speed = 0.34; armor = 16; knockbackRes = 0.7; followRange = 40;
        preferredRange = 7f; dmgMult = 1.1f;
        idleWeaponR = "super_shotgun";
    }

    // =====================================================================================
    // helpers (server)
    // =====================================================================================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static boolean ok(LivingEntity t) { return t != null && t.isAlive(); }
    static Vec3 flat(Vec3 v) { v = new Vec3(v.x, 0, v.z); return v.lengthSqr() < 1e-6 ? Vec3.ZERO : v.normalize(); }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.55, 0); }
    static Vec3 gun(SlayerEntity e) { return Combat.muzzle(e, 0.95, -0.3, 0); }
    /** muzzle of the left-shoulder launcher */
    static Vec3 shoulder(SlayerEntity e) { return Combat.muzzle(e, 0.4, -0.45, 0.25); }
    static Random rng(SlayerEntity e) { return new Random(e.getRandom().nextLong()); }

    static void pellets(SlayerEntity e, LivingEntity t, int n, double spread, float dmg, double range, int rgb) {
        if (!ok(t)) return;
        Vec3 from = gun(e); Random r = rng(e);
        for (int i = 0; i < n; i++) Combat.hitscan(e, from, Combat.dirTo(from, t, spread, r), range, dmg, 0, rgb);
        Fx.burst(L(e), ParticleTypes.FLAME, from, 5, 0.08, 0.06);
        Fx.burst(L(e), ParticleTypes.SMOKE, from, 4, 0.1, 0.02);
    }
    static SlayerProjectile fire(SlayerEntity e, LivingEntity t, ProjStyle s, Vec3 from, double speed, float dmg, double spread, double lift) {
        Vec3 d = ok(t) ? Combat.dirTo(from, t, spread, rng(e)) : e.getViewVector(1f);
        return Combat.shoot(e, s, from, d.add(0, lift, 0), speed, dmg);
    }
    static void blood(SlayerEntity e, Vec3 at, int n) {
        Fx.burst(L(e), new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()), at, n, 0.3, 0.15);
        Fx.dustBurst(L(e), 0x8a0000, 1.5f, at, n / 2, 0.35);
    }
    /** glory-kill snap: teleport in front of the staggered demon */
    static void snap(SlayerEntity e, LivingEntity t, double gap) {
        Vec3 d = flat(e.position().subtract(t.position()));
        if (d == Vec3.ZERO) d = new Vec3(0, 0, 1);
        double r = gap + t.getBbWidth() / 2;
        e.teleportTo(t.getX() + d.x * r, t.getY(), t.getZ() + d.z * r);
        Fx.burst(L(e), ParticleTypes.CLOUD, e.position().add(0, 0.5, 0), 8, 0.3, 0.05);
    }
    /** keep a staggered demon locked in place (effects expire on their own) */
    static void restrain(SlayerEntity e, LivingEntity t) {
        if (!ok(t) || !Combat.isFoe(e, t)) return;
        t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0); t.hurtMarked = true;
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 9, false, false));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 6, 9, false, false));
        if (t instanceof Mob m) m.getNavigation().stop();
    }
    static void execute(SlayerEntity e, LivingEntity t, float heal) {
        if (!ok(t)) return;
        Vec3 at = mid(t);
        Combat.damage(e, t, t.getMaxHealth() * 2 + 40);
        blood(e, at, 40);
        Combat.sound(e, SoundEvents.PLAYER_ATTACK_CRIT, 1.3f, 0.6f);
        Combat.sound(e, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 0.9f, 0.7f);
        e.heal(heal);
        Fx.dustBurst(L(e), 0x40ff60, 1.2f, e.position().add(0, 1, 0), 12, 0.5); // health drop
    }
    static int fuel(SlayerEntity e) { CompoundTag d = e.getPersistentData(); return d.contains(FUEL) ? d.getInt(FUEL) : 3; }
    static int fuelCost(LivingEntity t) { return t.getMaxHealth() <= 30 ? 1 : t.getMaxHealth() <= 80 ? 2 : 3; }

    // =====================================================================================
    // helpers (client animation)
    // =====================================================================================
    /** blend in over [0,in], out over [out,1] */
    static float env(float t, float in, float out) { return t < in ? smooth(t / in) : t > out ? 1 - smooth((t - out) / (1 - out)) : 1; }
    /** two-handed long gun: right on grip, left on foregrip. pitch < 0 aims higher. */
    static void hold(Pose p, float w, float pitch, float rec) {
        p.rArmX = lerp(p.rArmX, -HALF + pitch - rec * 0.35f, w); p.rArmY = lerp(p.rArmY, -0.12f, w); p.rArmZ = lerp(p.rArmZ, 0, w);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.08f - rec * 0.25f, w); p.lArmY = lerp(p.lArmY, 0.55f, w); p.lArmZ = lerp(p.lArmZ, 0, w);
        p.bodyX += rec * 0.14f * w; p.headX -= rec * 0.08f * w; p.rootZ += rec * 0.8f * w;
    }
    /** braced shooting stance: feet apart, knees bent, torso turned slightly. */
    static void brace(Pose p, float w) {
        p.rLegX = lerp(p.rLegX, 0.32f, w); p.lLegX = lerp(p.lLegX, -0.38f, w);
        p.rLegZ += 0.1f * w; p.lLegZ -= 0.1f * w; p.rootY -= 0.9f * w; p.bodyY += 0.12f * w; p.headY -= 0.1f * w;
    }
    /** lunging step forward with the left leg */
    static void step(Pose p, float s) {
        p.lLegX -= 0.7f * s; p.rLegX += 0.5f * s; p.rootZ -= 2.5f * s; p.rootY -= 1.2f * s; p.bodyX += 0.2f * s;
    }

    // =====================================================================================
    // attacks
    // =====================================================================================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------------- mobility ----------------
        out.add(AttackDef.of("dash").range(4, 14).cd(50).dur(8).weight(2)
                .tick((e, t, n) -> {
                    if (n == 0 && ok(t)) {
                        Vec3 f = flat(t.position().subtract(e.position()));
                        Vec3 side = new Vec3(-f.z, 0, f.x).scale(e.getRandom().nextBoolean() ? 1 : -1);
                        Vec3 d = f.scale(0.8).add(side.scale(0.75)).normalize();
                        e.setDeltaMovement(d.x * 1.5, 0.12, d.z * 1.5); e.hurtMarked = true;
                        Combat.sound(e, SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 1.9f);
                        Combat.sound(e, SoundEvents.FIRECHARGE_USE, 0.5f, 1.8f);
                    }
                    if (n < 5) {
                        Fx.burst(L(e), ParticleTypes.CLOUD, e.position().add(0, 0.6, 0), 4, 0.25, 0.02);
                        Fx.dustBurst(L(e), 0x6ab4ff, 1.1f, e.position().add(0, 1.1, 0), 3, 0.3);
                    }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.12f, 0.7f);
                    p.rootPitch += 0.35f * w; p.bodyX += 0.2f * w; p.rootY += bump(t) * 1.6f; p.rootRoll += 0.18f * w;
                    p.rArmX = lerp(p.rArmX, 0.9f, w); p.lArmX = lerp(p.lArmX, 0.75f, w); p.rArmZ += 0.35f * w; p.lArmZ -= 0.35f * w;
                    p.rLegX = lerp(p.rLegX, -0.9f, w); p.lLegX = lerp(p.lLegX, 0.8f, w); p.headX -= 0.3f * w;
                }));

        // ---------------- glory kills (staggered demon < 25%) ----------------
        // 1) grab, knee, rip the arm off, beat it with its own arm, blood-fist finish
        out.add(AttackDef.of("glory_kill_rip").range(0, 6).cd(60).dur(36).weight(5).lowHealthTarget(0.25f)
                .tick((e, t, n) -> {
                    if (!ok(t)) return;
                    if (n == 0) { snap(e, t, 0.9); Combat.sound(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f); }
                    restrain(e, t);
                    if (n == 8) { Combat.damage(e, t, 2); Combat.sound(e, SoundEvents.IRON_GOLEM_ATTACK, 1f, 0.8f); blood(e, mid(t), 10); }
                    if (n == 16) { Combat.damage(e, t, 2); Combat.sound(e, SoundEvents.ITEM_BREAK, 1.2f, 0.5f); Combat.sound(e, SoundEvents.SLIME_SQUISH, 1.2f, 0.6f); blood(e, mid(t).add(0, 0.3, 0), 25); }
                    if (n == 24) { Combat.damage(e, t, 2); Combat.sound(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.7f); blood(e, mid(t).add(0, 0.4, 0), 15); }
                    if (n == 30) { execute(e, t, 8); Combat.knock(t, e.position(), 1.2, 0.3); }
                })
                .anim((p, t, a) -> {
                    float g = track(t, 0, 0, 0.1f, 1, 0.3f, 1, 0.4f, 0);
                    float knee = track(t, 0.12f, 0, 0.22f, 1, 0.3f, 0);
                    float rip = track(t, 0.32f, 0, 0.44f, 1, 0.52f, 0);
                    float raise = track(t, 0.48f, 0, 0.58f, 1, 0.67f, 0);
                    float smash = trackLin(t, 0.58f, 0, 0.67f, 1, 0.74f, 0);
                    float fin = trackLin(t, 0.74f, 0, 0.83f, 1, 0.95f, 0);
                    float ready = track(t, 0.7f, 0, 0.76f, 1, 0.83f, 0);
                    p.weaponR = null;
                    p.lArmX = -1.55f * g - 0.7f * rip + 0.6f * ready - 1.75f * fin; p.lArmY = 0.2f * g + 0.3f * rip; p.lArmZ = -0.15f * g;
                    p.rArmX = -1.1f * g - 1.3f * rip - 2.9f * raise - 0.9f * smash; p.rArmY = -0.3f * rip; p.rArmZ = 0.2f * raise;
                    p.rLegX = -1.3f * knee + 0.3f * (rip + fin); p.lLegX = 0.15f * knee - 0.5f * fin - 0.2f * smash;
                    p.rootY += 1.2f * knee - 1f * smash - 0.8f * fin;
                    p.bodyX = 0.25f * g - 0.35f * rip - 0.2f * raise + 0.55f * smash + 0.35f * fin;
                    p.bodyY = -0.7f * rip + 0.4f * raise - 0.3f * smash - 0.6f * fin;
                    p.headX += 0.3f * (g + smash) - 0.2f * rip; p.rootZ -= 2.5f * fin;
                }));
        // 2) leap onto the demon and stomp its skull flat
        out.add(AttackDef.of("glory_kill_stomp").range(0, 6).cd(60).dur(30).weight(4).lowHealthTarget(0.25f)
                .tick((e, t, n) -> {
                    if (!ok(t)) return;
                    if (n == 0) { snap(e, t, 1.1); Combat.sound(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.7f); }
                    restrain(e, t);
                    if (n == 3) { Combat.damage(e, t, 1); Combat.knock(t, e.position(), 0, -0.3); Combat.sound(e, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.6f); }
                    if (n == 7) { e.setDeltaMovement(0, 0.55, 0); e.hurtMarked = true; }
                    if (n == 16) {
                        Vec3 at = t.position().add(0, t.getBbHeight(), 0);
                        execute(e, t, 8);
                        Fx.burst(L(e), ParticleTypes.EXPLOSION, at, 2, 0.2, 0);
                        Fx.ring(L(e), t.position().add(0, 0.1, 0), 1.8f, 0x8a0000, 1.6f, 20);
                        Combat.sound(e, SoundEvents.ANVIL_LAND, 0.8f, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float kick = track(t, 0, 0, 0.1f, 1, 0.18f, 0);
                    float crouch = track(t, 0.15f, 0, 0.23f, 1, 0.27f, 0);
                    float air = track(t, 0.25f, 0, 0.35f, 1, 0.5f, 1, 0.53f, 0);
                    float stomp = trackLin(t, 0.48f, 0, 0.53f, 1, 0.75f, 1, 1, 0);
                    p.weaponR = null;
                    p.rLegX = -1.2f * kick - 0.6f * crouch - 1.4f * air + 0.25f * stomp;
                    p.lLegX = 0.3f * kick + 0.3f * crouch + 0.6f * air - 0.1f * stomp;
                    p.rootY += -2f * crouch + 3f * air - 2f * stomp;
                    p.rArmX = -0.8f * air + 0.4f * stomp - 0.3f * kick; p.lArmX = -1.0f * air + 0.5f * stomp;
                    p.rArmZ += 0.9f * air + 0.5f * stomp; p.lArmZ -= 0.9f * air + 0.5f * stomp;
                    p.bodyX = 0.15f * kick + 0.4f * crouch - 0.2f * air + 0.45f * stomp;
                    p.headX += 0.5f * stomp + 0.2f * crouch; p.rootPitch += 0.1f * stomp;
                }));
        // 3) Doom Blade (left wrist): two stabs and an uppercut that lifts the demon off its feet
        out.add(AttackDef.of("glory_kill_doom_blade").range(0, 6).cd(60).dur(30).weight(4).lowHealthTarget(0.25f)
                .weapons(null, "doom_blade")
                .tick((e, t, n) -> {
                    if (!ok(t)) return;
                    if (n == 0) { snap(e, t, 0.8); Combat.sound(e, SoundEvents.ARMOR_EQUIP_IRON, 1.2f, 0.5f); }
                    if (n < 20) restrain(e, t);
                    if (n == 6 || n == 12) { Combat.damage(e, t, 2); Combat.sound(e, SoundEvents.TRIDENT_HIT, 1.2f, 0.7f); blood(e, mid(t), 18); Fx.sparks(L(e), mid(t), 6); }
                    if (n == 20) {
                        Combat.knock(t, e.position(), 0.2, 0.75);
                        Combat.sound(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
                        Fx.arc(L(e), e.position().add(0, 0.4, 0), e.getYRot(), 1.6f, -90, 60, 90, 0xff3010, 1.2f, 16);
                    }
                    if (n == 23) execute(e, t, 8);
                })
                .anim((p, t, a) -> {
                    float s1 = trackLin(t, 0.08f, 0, 0.2f, 1, 0.28f, 0);
                    float s2 = trackLin(t, 0.3f, 0, 0.4f, 1, 0.48f, 0);
                    float low = track(t, 0.48f, 0, 0.6f, 1, 0.67f, 0);
                    float up = trackLin(t, 0.6f, 0, 0.68f, 1, 0.85f, 1, 1, 0);
                    p.weaponR = null;
                    p.lArmX = -1.0f + 0.4f * low - 0.7f * (s1 + s2) - 2.1f * up; p.lArmY = 0.3f - 0.3f * (s1 + s2);
                    p.rArmX = -1.3f + 0.6f * (s1 + s2) + 0.6f * up; p.rArmY = -0.4f;
                    p.lHandX = 0.4f * low;
                    p.bodyY = -0.45f * (s1 + s2) + 0.3f * low - 0.5f * up;
                    p.bodyX = 0.2f + 0.25f * (s1 + s2) + 0.35f * low - 0.5f * up;
                    step(p, 0.6f * (s1 + s2));
                    p.rootY += -1.6f * low + 1.5f * up; p.rLegX += 0.4f * low; p.lLegX -= 0.6f * low;
                    p.headX -= 0.4f * up;
                }));

        // ---------------- melee / abilities ----------------
        out.add(AttackDef.of("blood_punch").range(0, 3.5f).cd(90).dur(18).weight(3)
                .tick((e, t, n) -> {
                    Vec3 fist = Combat.muzzle(e, 0.9, -0.2, -0.1);
                    if (n < 8) Fx.dustBurst(L(e), 0xff2010, 1.2f, fist, 2, 0.15);
                    if (n == 2) Combat.sound(e, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.9f, 1.6f);
                    if (n == 9) {
                        Vec3 c = Combat.muzzle(e, 1.8, 0, -0.4);
                        Combat.blast(e, c, 3.6f, 16f, 1.6f, null);
                        Fx.burst(L(e), ParticleTypes.EXPLOSION, c, 2, 0.3, 0);
                        Fx.burst(L(e), ParticleTypes.FLAME, c, 25, 0.6, 0.12);
                        Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 1.2f, 1.5f);
                        Combat.sound(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.5f, 0.5f);
                    }
                    if (n >= 9 && n <= 13) Fx.ring(L(e), Combat.muzzle(e, 1.8, 0, -1.1), 0.8f + (n - 9) * 0.8f, 0xff3010, 1.5f, 18 + (n - 9) * 6);
                })
                .anim((p, t, a) -> {
                    float wind = track(t, 0, 0, 0.36f, 1, 0.44f, 1, 0.5f, 0);
                    float hit = trackLin(t, 0.42f, 0, 0.5f, 1, 0.7f, 1, 1, 0);
                    p.weaponR = null;
                    p.lArmX = lerp(lerp(p.lArmX, 0.8f, wind), -HALF - 0.15f, hit); p.lArmZ -= 0.35f * wind; p.lArmY = 0.15f * hit;
                    p.rArmX = lerp(p.rArmX, -0.7f, Math.max(wind, hit)); p.rArmZ += 0.3f * wind;
                    p.bodyY += 0.65f * wind - 0.55f * hit; p.bodyX += -0.12f * wind + 0.35f * hit;
                    p.rootY -= 1.3f * wind; step(p, hit); p.headX += 0.15f * hit;
                    p.rootY += kick(t, 0.5f, 0.3f) * 0.8f;
                }));
        out.add(AttackDef.of("chainsaw").range(0, 3.5f).cd(140).dur(32).weight(4)
                .when((e, t) -> t.getMaxHealth() <= 150 && fuel(e) >= fuelCost(t))
                .weapon("chainsaw")
                .onStart((e, t, n) -> { if (ok(t)) e.getPersistentData().putInt(FUEL, fuel(e) - fuelCost(t)); })
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.sound(e, SoundEvents.MINECART_RIDING, 1.4f, 1.8f); if (ok(t)) snap(e, t, 0.9); }
                    if (!ok(t)) return;
                    restrain(e, t);
                    if (n >= 6 && n <= 24) {
                        Fx.sparks(L(e), mid(t), 3); blood(e, mid(t), 6);
                        if (n % 3 == 0) Combat.damage(e, t, 1f);
                        if (n % 4 == 0) Combat.sound(e, SoundEvents.GRINDSTONE_USE, 1.2f, 1.5f);
                    }
                    if (n == 26) {
                        execute(e, t, 2);
                        Fx.burst(L(e), ParticleTypes.CRIT, e.position().add(0, 1, 0), 20, 0.6, 0.3); // ammo spray
                        Combat.sound(e, SoundEvents.ITEM_PICKUP, 1.2f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float rev = env(t, 0.15f, 0.85f);
                    float push = track(t, 0.15f, 0, 0.25f, 1, 0.8f, 1, 0.86f, 0);
                    float rip = trackLin(t, 0.78f, 0, 0.84f, 1, 1, 0);
                    float shake = (float) Math.sin(a * 3.1f) * 0.05f * push;
                    hold(p, rev, 0.35f - 0.3f * push - 1.0f * rip + shake, 0);
                    brace(p, rev); step(p, 0.6f * push);
                    p.bodyX += 0.25f * push; p.bodyY += shake * 2 - 0.4f * rip; p.headX += 0.2f * push;
                    p.rHandZ = shake;
                }));
        out.add(AttackDef.of("flame_belch").range(0, 5).cd(160).dur(24).weight(2)
                .tick((e, t, n) -> {
                    if (n == 3) { Combat.sound(e, SoundEvents.BLAZE_SHOOT, 1.2f, 0.7f); Combat.sound(e, SoundEvents.FIRECHARGE_USE, 1f, 0.6f); }
                    if (n >= 4 && n <= 19) {
                        ServerLevel l = L(e); Vec3 from = shoulder(e), look = e.getViewVector(1f); Random r = rng(e);
                        for (int i = 0; i < 10; i++) {
                            Vec3 d = look.add(r.nextGaussian() * 0.22, r.nextGaussian() * 0.12, r.nextGaussian() * 0.22).normalize();
                            l.sendParticles(i % 3 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, from.x, from.y, from.z, 0, d.x, d.y, d.z, 0.35 + r.nextDouble() * 0.2);
                        }
                        if (n % 3 == 0) for (LivingEntity f : Combat.cone(e, 5.5, 32)) { f.setSecondsOnFire(7); Combat.damage(e, f, 1.5f); }
                        if (n % 5 == 0) Combat.sound(e, SoundEvents.FIRE_AMBIENT, 1.5f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.15f, 0.82f);
                    float jet = (float) Math.sin(a * 2.3f) * 0.03f * win(t, 0.16f, 0.8f);
                    // drop the left shoulder forward toward the target, arms braced back
                    p.bodyY = lerp(p.bodyY, -0.5f, w); p.bodyZ += 0.18f * w; p.bodyX += 0.28f * w + jet;
                    p.lArmX = lerp(p.lArmX, -0.5f, w); p.lArmZ -= 0.45f * w;
                    p.rArmX = lerp(p.rArmX, -0.9f, w); p.rArmY = lerp(p.rArmY, 0.5f, w);
                    brace(p, w); p.headY += 0.35f * w; p.headX += 0.1f * w; p.rootZ += 0.6f * w;
                }));
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(100).dur(14).weight(2)
                .tick((e, t, n) -> {
                    if (n == 6) {
                        fire(e, t, FRAG, shoulder(e), 1.05, 6f, 0.03, 0.22);
                        Combat.sound(e, SoundEvents.DISPENSER_LAUNCH, 1.2f, 0.7f);
                        Fx.burst(L(e), ParticleTypes.SMOKE, shoulder(e), 6, 0.1, 0.03);
                    }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.2f, 0.75f), k = kick(t, 0.43f, 0.4f);
                    p.bodyY = lerp(p.bodyY, -0.35f, w); p.bodyX -= 0.12f * w - 0.25f * k;
                    p.headY += 0.25f * w; p.headX -= 0.2f * w;
                    hold(p, w * 0.6f, 0.5f, 0); p.lArmZ -= 0.25f * w + 0.4f * k;
                    p.rootZ += 1.5f * k; p.rootY -= 0.6f * k; p.lLegX -= 0.25f * w; p.rLegX += 0.2f * w;
                }));
        out.add(AttackDef.of("ice_bomb").range(4, 16).cd(220).dur(14).weight(2)
                .tick((e, t, n) -> {
                    if (n < 6) Fx.burst(L(e), ParticleTypes.SNOWFLAKE, shoulder(e), 2, 0.08, 0.01);
                    if (n == 6) {
                        fire(e, t, ICE_BOMB, shoulder(e), 1.0, 3f, 0.02, 0.18);
                        Combat.sound(e, SoundEvents.SNOW_GOLEM_SHOOT, 1.4f, 0.6f);
                        Combat.sound(e, SoundEvents.DISPENSER_LAUNCH, 1f, 1.3f);
                    }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.2f, 0.75f), k = kick(t, 0.43f, 0.45f);
                    p.bodyY = lerp(p.bodyY, -0.45f, w); p.bodyZ += 0.1f * w; p.bodyX += 0.3f * k;
                    p.headY += 0.3f * w; p.headX -= 0.15f * w;
                    p.lArmX = lerp(p.lArmX, -0.3f, w); p.lArmZ -= 0.5f * w + 0.3f * k;
                    hold(p, w * 0.4f, 0.6f, 0); p.rArmY -= 0.3f * w;
                    brace(p, w); p.rootZ += 1.8f * k;
                }));

        // ---------------- Crucible ----------------
        out.add(AttackDef.of("crucible_combo").range(0, 3.8f).cd(110).dur(30).weight(3).weapon("crucible")
                .tick((e, t, n) -> {
                    ServerLevel l = L(e); Vec3 c = e.position().add(0, 1.1, 0); float yaw = e.getYRot();
                    if (n == 0) Combat.sound(e, SoundEvents.BEACON_ACTIVATE, 1f, 2f);
                    if (n == 5 || n == 6) Fx.arc(l, c, yaw, 2.6f, n == 5 ? 80 : 0, n == 5 ? 0 : -80, -15, 0xff2a10, 1.5f, 14);
                    if (n == 12 || n == 13) Fx.arc(l, c, yaw, 2.6f, n == 12 ? -80 : 0, n == 12 ? 0 : 80, 35, 0xff4a20, 1.5f, 14);
                    if (n >= 19 && n <= 22) Fx.arc(l, c.add(0, -0.1, 0), yaw, 2.9f, (n - 19) * 90 - 180, (n - 18) * 90 - 180, 0, 0xff2010, 1.7f, 16);
                    if (n == 6 || n == 13 || n == 21) {
                        for (LivingEntity f : n == 21 ? Combat.around(e, c, 3.6) : Combat.cone(e, 4, 75)) {
                            Combat.damage(e, f, n == 21 ? 16f : 12f); Combat.knock(f, e.position(), 0.5, 0.15); blood(e, mid(f), 8);
                            f.setSecondsOnFire(2);
                        }
                        Combat.sound(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, n == 21 ? 0.6f : 0.9f);
                        Combat.sound(e, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 0.5f);
                    }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.06f, 0.88f);
                    // slash 1: right-to-left flat
                    float w1 = track(t, 0, 0, 0.13f, 1, 0.2f, 0);
                    float s1 = trackLin(t, 0.13f, 0, 0.21f, 1, 0.3f, 0);
                    // slash 2: rising left-to-right diagonal
                    float w2 = track(t, 0.26f, 0, 0.35f, 1, 0.42f, 0);
                    float s2 = trackLin(t, 0.36f, 0, 0.44f, 1, 0.55f, 0);
                    // slash 3: full spin
                    float w3 = track(t, 0.52f, 0, 0.6f, 1, 0.64f, 0);
                    float spin = smooth(win(t, 0.6f, 0.76f));
                    float s3 = track(t, 0.6f, 0, 0.66f, 1, 0.8f, 1, 0.92f, 0);
                    p.rArmX = lerp(p.rArmX, -HALF + 0.2f * w2 - 0.5f * s2 - 0.3f * w1, w);
                    p.rArmY = 1.0f * w1 - 1.0f * s1 - 0.9f * w2 + 0.9f * s2 + 0.9f * w3 - 0.4f * s3;
                    p.rArmZ = 0.3f * s3; p.rHandZ = HALF * (s1 + w1) - 0.6f * (w2 + s2) + HALF * s3;
                    p.lArmX = lerp(p.lArmX, -0.6f, w); p.lArmZ -= 0.4f * (s1 + s3);
                    p.bodyY = 0.55f * w1 - 0.6f * s1 - 0.5f * w2 + 0.55f * s2 + 0.6f * w3;
                    p.bodyX += 0.25f * (s1 + s2) - 0.1f * w2 + 0.2f * s3;
                    p.rootYaw = -spin * 2 * PI;
                    step(p, 0.7f * s1 + 0.5f * s2 + 0.4f * s3);
                    p.rootY -= 0.8f * w2 + 0.8f * w3; p.headX += 0.1f * s3;
                }));
        // overhead vertical cleave: slayer below half health goes for big damage
        out.add(AttackDef.of("crucible_overhead").range(0, 4.5f).cd(200).dur(28).weight(4).weapon("crucible")
                .when((e, t) -> e.getHealth() <= e.getMaxHealth() * 0.5f)
                .tick((e, t, n) -> {
                    ServerLevel l = L(e); Vec3 c = e.position().add(0, 1.3, 0); float yaw = e.getYRot();
                    if (n == 0) { Combat.sound(e, SoundEvents.BEACON_ACTIVATE, 1.2f, 1.6f); Combat.sound(e, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 0.5f); }
                    if (n < 12) Fx.dustBurst(l, 0xff2010, 1.2f, c.add(0, 1.4, 0), 3, 0.2);
                    if (n >= 13 && n <= 16) {
                        float from = -100 + (n - 13) * 50, to = from + 55;
                        Fx.arc(l, c, yaw, 3.6f, from, to, 90, 0xff2a10, 2.2f, 14);
                        Fx.arc(l, c, yaw, 2.8f, from, to, 90, 0xffa080, 1.4f, 12);
                    }
                    if (n == 15) {
                        Vec3 ground = Combat.muzzle(e, 2.6, 0, -1.5);
                        for (LivingEntity f : Combat.cone(e, 5, 35)) { Combat.damage(e, f, 45f); blood(e, mid(f), 30); f.setSecondsOnFire(4); }
                        Fx.burst(l, ParticleTypes.EXPLOSION, ground, 3, 0.4, 0);
                        Fx.burst(l, ParticleTypes.LAVA, ground, 12, 0.5, 0.2);
                        Fx.ring(l, ground.add(0, 0.2, 0), 1.6f, 0xff3010, 1.6f, 20);
                        Combat.sound(e, SoundEvents.TRIDENT_THUNDER, 1f, 1.4f);
                        Combat.sound(e, SoundEvents.ANVIL_LAND, 0.8f, 0.5f);
                    }
                })
                .anim((p, t, a) -> {
                    float raise = track(t, 0, 0, 0.4f, 1, 0.48f, 1, 0.55f, 0);
                    float cut = trackLin(t, 0.47f, 0, 0.55f, 1, 0.8f, 1, 1, 0);
                    float hum = (float) Math.sin(a * 1.4f) * 0.04f * raise;
                    p.rArmX = -PI - 0.35f * raise * raise + hum;
                    p.rArmX = lerp(p.rArmX, -0.55f, cut);
                    p.lArmX = lerp(-PI - 0.25f, -0.7f, cut) * Math.max(raise, cut); p.lArmY = 0.4f * raise - 0.2f * cut;
                    p.rArmY = -0.3f * raise; p.rArmZ = 0;
                    p.bodyX = -0.35f * raise + 0.7f * cut; p.bodyY = 0.2f * raise;
                    p.headX = -0.3f * raise + 0.35f * cut;
                    p.rootY += 0.8f * raise - 2.2f * cut; step(p, cut);
                    p.rLegX += 0.3f * raise; p.rootPitch += 0.08f * cut;
                    p.rootY += kick(t, 0.55f, 0.2f) * -0.8f;
                }));

        // ---------------- Combat Shotgun ----------------
        out.add(AttackDef.of("combat_shotgun").range(1.5f, 10).cd(20).dur(16).weight(3).weapon("combat_shotgun")
                .tick((e, t, n) -> { if (n == 5) { pellets(e, t, 8, 0.06, 2.2f, 24, 0xffd070); Combat.gunSound(e, 0.8f); } })
                .anim((p, t, a) -> {
                    float w = env(t, 0.2f, 0.8f), k = kick(t, 0.31f, 0.35f), pump = track(t, 0.55f, 0, 0.65f, 1, 0.75f, 0);
                    hold(p, w, 0, k); brace(p, w); p.lArmX += 0.35f * pump; p.lArmY -= 0.1f * pump; p.rHandX += 0.1f * pump;
                    p.bodyY -= 0.08f * k; p.rootY -= 0.3f * k;
                }));
        out.add(AttackDef.of("full_auto").range(2, 12).cd(80).dur(24).weight(2).weapon("combat_shotgun")
                .tick((e, t, n) -> { if (n >= 4 && n <= 20 && n % 4 == 0) { pellets(e, t, 5, 0.08, 1.7f, 22, 0xffd070); Combat.gunSound(e, 1.1f); } })
                .anim((p, t, a) -> {
                    float w = env(t, 0.12f, 0.9f), k = 0;
                    for (int i = 0; i < 5; i++) k += kick(t, (4 + i * 4) / 24f, 0.14f);
                    hold(p, w, 0.05f, k); brace(p, w); p.bodyX += 0.1f * w; p.rootY -= 0.4f * w;
                    p.bodyZ += (float) Math.sin(a * 2) * 0.03f * k;
                }));
        out.add(AttackDef.of("sticky_bomb").range(4, 16).cd(90).dur(18).weight(2).weapon("combat_shotgun")
                .tick((e, t, n) -> {
                    if (n == 2) Combat.sound(e, SoundEvents.CROSSBOW_LOADING_END, 1f, 1.4f);
                    if (n == 7) { fire(e, t, STICKY, gun(e), 1.5, 3f, 0.01, 0.04); Combat.sound(e, SoundEvents.DISPENSER_LAUNCH, 1.2f, 1.2f); Fx.burst(L(e), ParticleTypes.SMOKE, gun(e), 4, 0.05, 0.02); }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.25f, 0.8f), k = kick(t, 0.39f, 0.4f);
                    hold(p, w, -0.08f, k * 1.3f); brace(p, w); p.rHandZ += 0.15f * w; p.headZ += 0.1f * w;
                    p.lArmX -= 0.15f * win(t, 0.05f, 0.25f) * (1 - win(t, 0.3f, 0.4f));
                }));

        // ---------------- Super Shotgun + Meat Hook ----------------
        out.add(AttackDef.of("super_shotgun").range(0, 6).cd(30).dur(22).weight(4).weapon("super_shotgun")
                .tick((e, t, n) -> {
                    if (n == 6) {
                        pellets(e, t, 16, 0.1, 2.1f, 16, 0xffb060);
                        Combat.gunSound(e, 0.6f); Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.8f);
                        if (ok(t) && e.distanceTo(t) < 3) Combat.knock(t, e.position(), 1.1, 0.25);
                    }
                    if (n == 13) Combat.sound(e, SoundEvents.PISTON_CONTRACT, 0.8f, 1.8f);
                    if (n == 18) Combat.sound(e, SoundEvents.PISTON_EXTEND, 0.8f, 2f);
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.15f, 0.88f), k = kick(t, 0.27f, 0.3f);
                    float brk = track(t, 0.5f, 0, 0.6f, 1, 0.78f, 1, 0.86f, 0);
                    hold(p, w, 0.15f * brk, k * 1.6f); brace(p, w);
                    p.rHandX += 0.6f * brk; p.lArmX += 0.5f * brk; p.lArmY -= 0.35f * brk;
                    p.rootZ += 1.2f * k; p.bodyX -= 0.1f * k; p.headX += 0.2f * brk;
                }));
        out.add(AttackDef.of("meat_hook").range(5, 18).cd(110).dur(30).weight(4).weapon("super_shotgun")
                .tick((e, t, n) -> {
                    if (!ok(t)) return;
                    ServerLevel l = L(e);
                    if (n == 3) { Combat.sound(e, SoundEvents.FISHING_BOBBER_THROW, 1.4f, 0.6f); Combat.sound(e, SoundEvents.CHAIN_HIT, 1.2f, 0.8f); }
                    if (n >= 3 && n <= 14) {
                        Vec3 to = mid(t);
                        Fx.beam(l, gun(e), to, 0xffb030, 0.8f, 0.3);
                        Fx.burst(l, ParticleTypes.SMALL_FLAME, to, 2, 0.15, 0.01);
                        t.setSecondsOnFire(2);
                        if (n >= 4 && e.distanceTo(t) > 2.2) {
                            Vec3 d = to.subtract(e.position()).normalize();
                            e.setDeltaMovement(d.x * 1.15, Math.max(0.15, d.y * 0.6 + 0.1), d.z * 1.15); e.hurtMarked = true; e.fallDistance = 0;
                            Fx.burst(l, ParticleTypes.CLOUD, e.position().add(0, 0.8, 0), 2, 0.2, 0.01);
                        } else if (n >= 4) e.setDeltaMovement(e.getDeltaMovement().scale(0.2));
                    }
                    if (n == 16) {
                        pellets(e, t, 22, 0.08, 2.4f, 10, 0xffb060);
                        Combat.gunSound(e, 0.5f); Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.6f);
                        Combat.knock(t, e.position(), 1.4, 0.35); blood(e, mid(t), 20);
                    }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.06f, 0.88f);
                    float throwK = kick(t, 0.1f, 0.12f);
                    float fly = track(t, 0.13f, 0, 0.2f, 1, 0.45f, 1, 0.5f, 0);
                    float land = track(t, 0.45f, 0, 0.5f, 1, 0.6f, 1, 0.7f, 0);
                    float k = kick(t, 0.53f, 0.3f);
                    float brk = track(t, 0.68f, 0, 0.75f, 1, 0.88f, 1, 0.95f, 0);
                    hold(p, w, -0.25f * throwK - 0.1f * fly + 0.2f * brk, k * 1.8f + throwK);
                    p.rootPitch += 0.55f * fly; p.rootY += 2.5f * fly;
                    p.rLegX = lerp(p.rLegX, 0.9f, fly); p.lLegX = lerp(p.lLegX, 0.2f, fly); p.lLegX -= 0.5f * fly;
                    brace(p, land); p.rootY -= 1.2f * land;
                    p.rHandX += 0.6f * brk; p.lArmX += 0.5f * brk; p.lArmY -= 0.35f * brk;
                    p.rootZ += 2f * k; p.headX -= 0.2f * fly;
                }));

        // ---------------- Heavy Cannon ----------------
        out.add(AttackDef.of("heavy_cannon").range(5, 28).cd(25).dur(16).weight(3).weapon("heavy_cannon")
                .tick((e, t, n) -> {
                    if (ok(t) && (n == 4 || n == 7 || n == 10)) { Vec3 f = gun(e); Combat.hitscan(e, f, Combat.dirTo(f, t, 0.02, rng(e)), 32, 4.5f, 0, 0xffe080); Combat.gunSound(e, 1.3f); Fx.burst(L(e), ParticleTypes.SMOKE, f, 2, 0.05, 0.01); }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.2f, 0.82f);
                    float k = kick(t, 4 / 16f, 0.12f) + kick(t, 7 / 16f, 0.12f) + kick(t, 10 / 16f, 0.12f);
                    hold(p, w, -0.02f, k); brace(p, w); p.headY -= 0.1f * w; p.bodyY += 0.05f * k;
                }));
        out.add(AttackDef.of("precision_bolt").range(10, 40).cd(90).dur(30).weight(2).weapon("heavy_cannon")
                .tick((e, t, n) -> {
                    if (!ok(t)) return;
                    if (n == 2) Combat.sound(e, SoundEvents.SPYGLASS_USE, 1.4f, 1f);
                    if (n > 4 && n < 18) Fx.dustBurst(L(e), 0xff2020, 0.8f, t.getEyePosition(), 1, 0.02);
                    if (n == 18) {
                        Vec3 f = gun(e);
                        Combat.hitscan(e, f, t.getEyePosition().subtract(f).normalize(), 48, 24f, 1, 0xffc040);
                        Combat.sound(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.6f, 0.6f); Combat.gunSound(e, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.15f, 0.85f), scope = track(t, 0.05f, 0, 0.25f, 1, 0.62f, 1, 0.75f, 0), k = kick(t, 0.6f, 0.3f);
                    hold(p, w, -0.05f, k * 1.5f); brace(p, w);
                    p.headZ += 0.25f * scope; p.headX += 0.12f * scope; p.headY -= 0.15f * scope; p.bodyY += 0.15f * scope;
                    p.rArmY -= 0.15f * scope; p.rootY -= 0.6f * scope;
                }));
        out.add(AttackDef.of("micro_missiles").range(6, 24).cd(100).dur(26).weight(2).weapon("heavy_cannon")
                .tick((e, t, n) -> {
                    if (n == 1) Combat.sound(e, SoundEvents.CROSSBOW_LOADING_MIDDLE, 1f, 1.8f);
                    if (n >= 6 && n <= 21 && n % 3 == 0) { fire(e, t, MICRO, gun(e), 1.3, 4f, 0.08, 0.03); Combat.sound(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1f, 1.6f); }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.15f, 0.88f), k = 0;
                    for (int i = 0; i < 6; i++) k += kick(t, (6 + i * 3) / 26f, 0.1f);
                    hold(p, w, -0.15f, k * 0.6f); brace(p, w);
                    p.bodyY += (float) Math.sin(t * PI * 3) * 0.15f * w; p.rootY -= 0.4f * w;
                }));

        // ---------------- Plasma Rifle ----------------
        out.add(AttackDef.of("plasma_rifle").range(4, 20).cd(35).dur(26).weight(3).weapon("plasma_rifle")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 22 && n % 2 == 0) { fire(e, t, ProjStyle.PLASMA, gun(e), 1.7, 3f, 0.04, 0); Combat.sound(e, SoundEvents.ILLUSIONER_CAST_SPELL, 0.5f, 1.9f); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, gun(e), 2, 0.05, 0.05); }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.12f, 0.88f), fireW = win(t, 0.15f, 0.85f) * (1 - win(t, 0.85f, 0.9f));
                    float k = Math.abs((float) Math.sin(t * 26 * PI / 2)) * 0.35f * fireW;
                    hold(p, w, 0, k); brace(p, w); p.bodyZ += (float) Math.sin(a * 1.7f) * 0.02f * fireW;
                }));
        out.add(AttackDef.of("heat_blast").range(0, 5).cd(150).dur(24).weight(2).weapon("plasma_rifle")
                .tick((e, t, n) -> {
                    if (n < 13) { Fx.dustBurst(L(e), 0x60c8ff, 1.0f, gun(e), 3, 0.2); if (n % 4 == 0) Combat.sound(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.2f + n * 0.05f); }
                    if (n == 14) {
                        Vec3 c = Combat.muzzle(e, 2.4, 0, -0.3);
                        Combat.blast(e, c, 4.6f, 14f, 1.4f, null);
                        for (LivingEntity f : Combat.around(e, c, 4.6)) f.setSecondsOnFire(3);
                        Fx.burst(L(e), ParticleTypes.SOUL_FIRE_FLAME, c, 40, 1.2, 0.15);
                        Fx.ring(L(e), c, 2.5f, 0x60c8ff, 1.8f, 28); Fx.ring(L(e), c, 4.2f, 0x2080ff, 1.6f, 40);
                        Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 1f, 1.3f); Combat.sound(e, SoundEvents.FIRECHARGE_USE, 1f, 0.5f);
                    }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.1f, 0.85f), charge = win(t, 0, 0.55f) * (1 - win(t, 0.58f, 0.65f)), k = kick(t, 0.58f, 0.35f);
                    float shake = (float) Math.sin(a * 4) * 0.04f * charge;
                    hold(p, w, 0.15f - 0.15f * charge + shake, k * 2f); brace(p, w);
                    p.rootY -= 1.2f * charge; p.bodyX += 0.15f * charge; p.rootZ += 2f * k; p.headX += 0.1f * charge;
                }));
        out.add(AttackDef.of("microwave_beam").range(3, 16).cd(160).dur(40).weight(2).weapon("plasma_rifle")
                .tick((e, t, n) -> {
                    if (!ok(t)) return;
                    if (n == 4) Combat.sound(e, SoundEvents.WARDEN_SONIC_CHARGE, 0.8f, 1.8f);
                    if (n >= 6 && n <= 36 && n % 2 == 0) {
                        Vec3 f = gun(e), to = mid(t);
                        Fx.beam(L(e), f, to, 0x9ad8ff, 0.9f, 0.3);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, to, 4, t.getBbWidth() * 0.4, 0.05);
                        Combat.damage(e, t, 1.8f);
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 4, false, false));
                        if (n % 6 == 0) Combat.sound(e, SoundEvents.BEACON_AMBIENT, 1f, 2f);
                        if (t.isDeadOrDying()) { // demon pops
                            Fx.explosion(L(e), to, 2.5f); blood(e, to, 30);
                            Combat.blast(e, to, 3.5f, 12f, 1f, t);
                            Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 1.2f, 1.2f);
                        }
                    }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.12f, 0.92f), beam = win(t, 0.15f, 0.25f) * (1 - win(t, 0.9f, 0.95f));
                    float jitter = (float) Math.sin(a * 5) * 0.03f * beam;
                    hold(p, w, jitter, 0.2f * beam); brace(p, w);
                    p.rootY -= 0.6f * beam; p.bodyX -= 0.08f * beam; p.lArmX -= 0.08f * beam; p.headX += 0.05f * beam;
                }));

        // ---------------- Rocket Launcher ----------------
        out.add(AttackDef.of("rocket_launcher").range(7, 30).cd(45).dur(18).weight(3).weapon("rocket_launcher")
                .tick((e, t, n) -> { if (n == 6) { fire(e, t, ProjStyle.ROCKET, gun(e), 1.4, 16f, 0.01, 0); Combat.sound(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5f, 0.5f); Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, gun(e), 6, 0.15, 0.03); } })
                .anim((p, t, a) -> {
                    float w = env(t, 0.2f, 0.8f), k = kick(t, 0.33f, 0.45f);
                    hold(p, w, -0.05f, k * 1.6f); brace(p, w); p.rootZ += 2f * k; p.rootY -= 0.6f * k; p.headY -= 0.15f * w;
                }));
        out.add(AttackDef.of("lock_on_burst").range(8, 30).cd(160).dur(34).weight(2).weapon("rocket_launcher")
                .tick((e, t, n) -> {
                    if (!ok(t)) return;
                    if (n < 16 && n % 4 == 0) { Combat.sound(e, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.6f + n * 0.03f); Fx.ring(L(e), mid(t), t.getBbWidth() + 0.4f - n * 0.02f, 0xff2020, 1f, 14); }
                    if (n >= 18 && n <= 30 && n % 3 == 0) { fire(e, t, LOCKON, gun(e), 1.0, 7f, 0.12, 0.08); Combat.sound(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2f, 0.9f); }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.1f, 0.9f), lock = win(t, 0.05f, 0.45f), k = 0;
                    for (int i = 0; i < 5; i++) k += kick(t, (18 + i * 3) / 34f, 0.08f);
                    hold(p, w, -0.08f, k); brace(p, w);
                    p.headZ += 0.15f * lock * (1 - win(t, 0.5f, 0.55f)); p.rootY -= 0.5f * w; p.bodyY += 0.1f * k;
                }));
        out.add(AttackDef.of("remote_detonation").range(8, 30).cd(90).dur(22).weight(2).weapon("rocket_launcher")
                .tick((e, t, n) -> {
                    if (n == 6) { fire(e, t, REMOTE, gun(e), 1.25, 10f, 0.01, 0); Combat.sound(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5f, 0.6f); Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, gun(e), 6, 0.15, 0.03); }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.2f, 0.85f), k = kick(t, 0.27f, 0.4f), click = kick(t, 0.6f, 0.15f);
                    hold(p, w, -0.05f, k * 1.6f); brace(p, w);
                    p.rHandZ += 0.2f * click; p.headX -= 0.1f * win(t, 0.3f, 0.6f); p.rootZ += 2f * k;
                }));

        // ---------------- Ballista ----------------
        out.add(AttackDef.of("ballista").range(8, 36).cd(55).dur(20).weight(2).weapon("ballista")
                .tick((e, t, n) -> {
                    if (n < 8) Fx.dustBurst(L(e), 0xff8a20, 0.8f, gun(e), 2, 0.1);
                    if (n == 8) { fire(e, t, BOLT, gun(e), 3.2, 14f, 0.005, 0); Combat.sound(e, SoundEvents.CROSSBOW_SHOOT, 1.5f, 0.5f); Combat.gunSound(e, 0.9f); }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.2f, 0.8f), k = kick(t, 0.4f, 0.4f);
                    hold(p, w, -0.04f, k * 1.4f); brace(p, w); p.headZ += 0.12f * w; p.rootZ += 1.5f * k;
                }));
        out.add(AttackDef.of("destroyer_blade").range(4, 22).cd(220).dur(40).weight(2).weapon("ballista")
                .tick((e, t, n) -> {
                    ServerLevel l = L(e);
                    if (n < 24) {
                        Fx.arc(l, gun(e), e.getYRot(), 0.3f + n * 0.04f, -90, 90, 0, 0xff8020, 0.8f, 8);
                        if (n % 6 == 0) Combat.sound(e, SoundEvents.BEACON_POWER_SELECT, 0.9f, 0.8f + n * 0.04f);
                    }
                    if (n == 26) { Combat.sound(e, SoundEvents.WARDEN_SONIC_BOOM, 1.2f, 1.5f); Combat.sound(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.5f); }
                    if (n >= 26 && n <= 31) {
                        Vec3 base = e.position(), f = flat(e.getViewVector(1f)), right = new Vec3(-f.z, 0, f.x);
                        double d0 = (n - 26) * 3.6, d1 = d0 + 3.6;
                        for (double dd : new double[]{d1 - 0.6, d1}) {
                            Vec3 c = base.add(f.scale(dd)).add(0, 1.1, 0);
                            Fx.beam(l, c.subtract(right.scale(3.5)), c.add(right.scale(3.5)), 0xff7a10, 1.6f, 0.25);
                        }
                        for (LivingEntity v : Combat.foes(e, e.getBoundingBox().inflate(23, 3, 23))) {
                            Vec3 rel = v.position().subtract(base); double fw = rel.dot(f), lat = Math.abs(rel.dot(right));
                            if (fw > d0 && fw <= d1 && lat < 3.5 + v.getBbWidth() / 2 && Math.abs(rel.y) < 3) {
                                Combat.damage(e, v, 30f); Combat.knock(v, base, 0.8, 0.2); blood(e, mid(v), 15); v.setSecondsOnFire(3);
                            }
                        }
                    }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.1f, 0.88f), charge = win(t, 0, 0.6f) * (1 - win(t, 0.62f, 0.66f));
                    float k = kick(t, 0.65f, 0.35f), sweep = track(t, 0.62f, 0, 0.7f, 1, 0.8f, 0);
                    float shake = (float) Math.sin(a * 3.5f) * 0.035f * charge;
                    hold(p, w, shake, k * 2f); brace(p, w);
                    p.bodyY += 0.35f * charge - 0.5f * sweep; p.rootY -= 1.2f * charge; p.rHandZ += HALF * charge;
                    p.rootZ += 2.5f * k;
                }));

        // ---------------- Gauss Cannon (precision shot) ----------------
        out.add(AttackDef.of("gauss_precision_shot").range(10, 40).cd(150).dur(34).weight(2).weapon("gauss")
                .tick((e, t, n) -> {
                    if (n < 22) {
                        Vec3 g = gun(e); Random r = rng(e);
                        Vec3 o = g.add(r.nextGaussian() * 0.6, r.nextGaussian() * 0.6, r.nextGaussian() * 0.6);
                        L(e).sendParticles(ParticleTypes.ELECTRIC_SPARK, o.x, o.y, o.z, 0, g.x - o.x, g.y - o.y, g.z - o.z, 0.25);
                        if (n % 5 == 0) Combat.sound(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.2f + n * 0.03f);
                    }
                    if (n == 24 && ok(t)) {
                        Vec3 f = gun(e), d = t.getEyePosition().subtract(f).normalize();
                        Combat.hitscan(e, f, d, 60, 28f, 6, 0x60b8ff);
                        Fx.beam(L(e), f, f.add(d.scale(40)), 0xd8f0ff, 1.6f, 0.5);
                        Combat.sound(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.4f, 1.6f);
                        Combat.sound(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.4f, 0.5f);
                        Vec3 back = e.getViewVector(1f).scale(-0.6); e.setDeltaMovement(back.x, 0.1, back.z); e.hurtMarked = true;
                    }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.1f, 0.88f), charge = win(t, 0, 0.68f) * (1 - win(t, 0.7f, 0.72f)), k = kick(t, 0.7f, 0.3f);
                    float hum = (float) Math.sin(a * 3) * 0.025f * charge;
                    hold(p, w, hum - 0.02f, k * 2.4f); brace(p, w);
                    p.headZ += 0.2f * charge; p.rootY -= 0.9f * charge; p.rootZ += 3f * k; p.bodyX -= 0.15f * k;
                }));

        // ---------------- Chaingun ----------------
        out.add(AttackDef.of("chaingun").range(4, 22).cd(50).dur(34).weight(3).weapon("chaingun")
                .tick((e, t, n) -> {
                    if (n < 10 && n % 3 == 0) Combat.sound(e, SoundEvents.PISTON_EXTEND, 0.5f, 1.2f + n * 0.08f);
                    if (n >= 10 && n <= 32 && ok(t)) {
                        Vec3 f = gun(e); Combat.hitscan(e, f, Combat.dirTo(f, t, 0.045, rng(e)), 30, 1.7f, 0, 0xffe080);
                        if (n % 2 == 0) Combat.gunSound(e, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.12f, 0.9f), fireW = win(t, 0.28f, 0.32f) * (1 - win(t, 0.94f, 0.98f));
                    float jit = (float) Math.sin(a * 6) * 0.04f * fireW;
                    hold(p, w, 0.25f + jit, 0.25f * fireW); brace(p, w);
                    p.bodyX -= 0.12f * w; p.rootY -= 0.4f * w; p.rHandZ += jit; p.bodyZ += jit * 0.5f;
                }));
        out.add(AttackDef.of("mobile_turret").range(6, 24).cd(200).dur(50).weight(2).weapon("chaingun")
                .tick((e, t, n) -> {
                    if (n == 2) { Combat.sound(e, SoundEvents.PISTON_EXTEND, 1f, 0.6f); Combat.sound(e, SoundEvents.ARMOR_EQUIP_NETHERITE, 1f, 0.8f); }
                    if (n >= 12 && n <= 46 && ok(t)) {
                        Vec3 f = gun(e); Random r = rng(e);
                        for (int i = 0; i < 3; i++) Combat.hitscan(e, f, Combat.dirTo(f, t, 0.06, r), 30, 1.4f, 0, i == 0 ? 0xffe080 : 0xffb050);
                        Combat.gunSound(e, 1.6f + (n % 3) * 0.1f);
                        Fx.burst(L(e), ParticleTypes.SMOKE, f, 2, 0.1, 0.02);
                    }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.2f, 0.92f), fireW = win(t, 0.22f, 0.26f) * (1 - win(t, 0.92f, 0.95f));
                    float jit = (float) Math.sin(a * 7) * 0.05f * fireW;
                    hold(p, w, 0.4f + jit, 0.3f * fireW);
                    p.rLegX = lerp(p.rLegX, 0.5f, w); p.lLegX = lerp(p.lLegX, -0.5f, w); p.rLegZ += 0.25f * w; p.lLegZ -= 0.25f * w;
                    p.rootY -= 2.2f * w; p.bodyX -= 0.25f * w; p.headX += 0.15f * w; p.rootRoll += jit * 0.3f;
                    p.lArmY += 0.2f * w;
                }));
        out.add(AttackDef.of("energy_shield").range(2, 8).cd(240).dur(36).weight(2).weapon("chaingun")
                .tick((e, t, n) -> {
                    ServerLevel l = L(e);
                    if (n == 0) { Combat.sound(e, SoundEvents.BEACON_ACTIVATE, 1.2f, 1.6f); Combat.sound(e, SoundEvents.SHIELD_BLOCK, 1f, 0.6f); }
                    if (n < 24) {
                        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 4, 3, false, false));
                        if (n % 2 == 0) for (double h : new double[]{0.4, 1.0, 1.6, 2.1}) Fx.arc(l, e.position().add(0, h, 0), e.getYRot(), 1.5f, -70, 70, 0, 0x60c0ff, 1.1f, 10);
                        Vec3 f = flat(e.getViewVector(1f));
                        for (Projectile pr : e.level().getEntitiesOfClass(Projectile.class, e.getBoundingBox().inflate(4),
                                pr -> !(pr instanceof SlayerProjectile) && pr.getOwner() instanceof LivingEntity le && Combat.isFoe(e, le))) {
                            if (flat(pr.position().subtract(e.position())).dot(f) > 0) {
                                Fx.sparks(l, pr.position(), 6); Fx.sound(l, pr.position(), SoundEvents.SHIELD_BLOCK, 0.8f, 1.4f); pr.discard();
                            }
                        }
                    }
                    if (n == 24) {
                        Combat.lunge(e, 1.3, 0.1); Combat.sound(e, SoundEvents.ENDER_DRAGON_FLAP, 1f, 1.5f);
                        for (LivingEntity v : Combat.cone(e, 4.5, 50)) { Combat.damage(e, v, 10f); Combat.knock(v, e.position(), 1.6, 0.35); }
                    }
                    if (n >= 24 && n < 30) Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, Combat.muzzle(e, 1.2, 0, 0), 6, 0.6, 0.05);
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.08f, 0.85f), guard = win(t, 0, 0.1f) * (1 - win(t, 0.62f, 0.67f));
                    float dash = track(t, 0.64f, 0, 0.7f, 1, 0.82f, 1, 0.9f, 0);
                    hold(p, w, 0.6f - 0.3f * dash, 0);
                    brace(p, guard); p.bodyX += 0.2f * guard + 0.35f * dash; p.headX += 0.1f * guard;
                    p.rootPitch += 0.3f * dash; p.rLegX -= 0.8f * dash; p.lLegX += 0.6f * dash; p.rootZ -= 3f * dash;
                }));

        // ---------------- BFG 9000 ----------------
        out.add(AttackDef.of("bfg_9000").range(10, 30).cd(600).dur(40).weight(2).weapon("bfg")
                .when((e, t) -> Combat.around(e, t.position(), 7).size() >= 2 || t.getMaxHealth() >= 60)
                .tick((e, t, n) -> {
                    ServerLevel l = L(e);
                    if (n < 20) {
                        Fx.burst(l, ParticleTypes.HAPPY_VILLAGER, gun(e), 3, 0.3, 0.05);
                        Fx.dustBurst(l, 0x50ff50, 1.4f, gun(e), 3, 0.25);
                        if (n % 5 == 0) Combat.sound(e, SoundEvents.BEACON_POWER_SELECT, 1.2f, 0.5f + n * 0.04f);
                    }
                    if (n == 22) {
                        fire(e, t, BFG_ORB, gun(e), 0.75, 34f, 0, 0);
                        Combat.sound(e, SoundEvents.WARDEN_SONIC_BOOM, 1.6f, 0.6f);
                        Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 1.2f, 0.5f);
                        Vec3 back = e.getViewVector(1f).scale(-0.5); e.setDeltaMovement(back.x, 0.1, back.z); e.hurtMarked = true;
                    }
                })
                .anim((p, t, a) -> {
                    float w = env(t, 0.1f, 0.85f), charge = win(t, 0, 0.53f) * (1 - win(t, 0.55f, 0.58f)), k = kick(t, 0.55f, 0.4f);
                    float shake = (float) Math.sin(a * 4) * 0.03f * charge;
                    hold(p, w, 0.12f + shake, k * 2.6f);
                    p.lArmY -= 0.2f * w; // supporting the big frame from below
                    brace(p, w); p.rootY -= 1.4f * charge; p.bodyX += 0.1f * charge - 0.25f * k; p.rootZ += 3.5f * k; p.headX -= 0.15f * k;
                }));
    }

    // =====================================================================================
    // passive: chainsaw fuel regen, stagger flash on low-health target
    // =====================================================================================
    @Override
    public void passive(SlayerEntity e) {
        if (e.tickCount % 200 == 0 && fuel(e) < 3) e.getPersistentData().putInt(FUEL, fuel(e) + 1);
        LivingEntity t = e.getTarget();
        if (e.tickCount % 4 == 0 && ok(t) && t.getHealth() <= t.getMaxHealth() * 0.25f && Combat.isFoe(e, t))
            Fx.dustBurst(L(e), (e.tickCount / 4) % 2 == 0 ? 0xff8a00 : 0x3080ff, 1.3f, mid(t), 4, t.getBbWidth() * 0.5);
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float limbAmt) {
        // super shotgun held at low ready, slight forward hunch
        p.rArmX = p.rArmX * 0.4f - 0.55f; p.rArmY = -0.1f;
        p.lArmX = p.lArmX * 0.4f - 0.45f; p.lArmY = 0.4f;
        p.bodyX += 0.05f; p.headX -= 0.03f;
    }

    // =====================================================================================
    // model (Praetor Suit, DOOM Eternal pattern)
    // =====================================================================================
    @Override
    public void buildModel(Rig rig) {
        Mat suit = Mat.of(0x4a5a30).noise(0.1f).bevel(0.22f);
        Mat plate = Mat.of(0x5c6c3a).noise(0.12f).bevel(0.3f)
                .decal((m, g, x, y, w, h, f) -> { if (f == Mat.Face.FRONT && h > 3) for (int i = 0; i < w; i++) g.px(x + i, y + h / 2, 0x2c3420); });
        Mat under = Mat.of(0x2b2c2a).noise(0.1f);
        Mat metal = Mat.of(0x60656a).noise(0.08f).bevel(0.25f);
        Mat dark = Mat.of(0x34383c).noise(0.06f);
        Mat helm = Mat.of(0x55663a).noise(0.08f).bevel(0.25f)
                .decal((m, g, x, y, w, h, f) -> {
                    if (f == Mat.Face.FRONT) { g.rect(x + 1, y + 2, w - 2, 3, 0x1e2018); for (int i = 0; i < w; i++) g.px(x + i, y + h - 1, 0x3a4628); }
                    if (f == Mat.Face.TOP) for (int j = 0; j < h; j++) g.px(x + w / 2, y + j, 0x7a8a50);
                });
        Mat visor = Mat.of(0xffb020).glow();
        Mat red = Mat.of(0xff2a10).glow();
        Mat orange = Mat.of(0xff8a20).glow();
        Mat blue = Mat.of(0x50c0ff).glow();
        Mat green = Mat.of(0x50ff50).glow();
        Mat leather = Mat.of(0x4a3826).noise(0.15f);
        Mat wood = Mat.of(0x5a3a20).noise(0.18f);

        rig.humanoid(under, suit, helm);
        // ---- head ----
        rig.box(rig.head, visor, -3, -5.5f, -4.4f, 6, 2, 1);
        rig.box(rig.head, metal, -2.5f, -3, -4.6f, 5, 2, 1);          // breather
        rig.box(rig.head, plate, -1, -8.7f, -4.2f, 2, 1, 8);           // crest ridge
        rig.box(rig.head, plate, -4.5f, -6, -2.5f, 1, 4, 4);           // ear plates
        rig.box(rig.head, plate, 3.5f, -6, -2.5f, 1, 4, 4);
        // ---- torso ----
        rig.box(rig.torso, plate, -4.5f, -12, -3, 9, 6, 1);            // chest plate
        rig.box(rig.torso, plate, -3.5f, -5.5f, -2.6f, 7, 3, 1);       // abdomen
        rig.box(rig.torso, metal, -3, -13, -2.5f, 6, 1, 5);            // collar
        rig.box(rig.torso, red, -0.5f, -9.5f, -3.3f, 1, 1, 1);         // argent core light
        rig.box(rig.torso, leather, -4.5f, -2.2f, -2.5f, 9, 2, 5);     // belt
        rig.box(rig.torso, leather, -4, -2.6f, -3.3f, 2, 2, 1);        // pouches
        rig.box(rig.torso, leather, 2, -2.6f, -3.3f, 2, 2, 1);
        rig.box(rig.torso, dark, -3, -11, 2, 6, 6, 2);                 // backpack
        // Crucible in its sheath on the back, hilt over the right shoulder
        PartDefinition sheath = rig.pivot(rig.torso, "sheath", 0, -6, 4, 0, 0, -0.55f);
        rig.box(sheath, dark, -1, -6, 0, 2, 12, 1);
        rig.box(sheath, metal, -1, -10, -0.5f, 2, 4, 2);
        rig.box(sheath, red, -0.5f, -11, 0, 1, 1, 1);
        // left-shoulder launcher (frag / ice bomb / flame belch)
        rig.box(rig.torso, dark, 4.5f, -14.5f, -1.5f, 3, 2, 3);
        rig.box(rig.torso, metal, 5, -15.5f, -5.5f, 2, 2, 6);
        rig.box(rig.torso, orange, 5, -15.5f, -6.3f, 2, 2, 1);
        rig.box(rig.torso, metal, 7.2f, -14, -4, 1, 1, 3);             // flamethrower nozzle
        // ---- arms ----
        for (PartDefinition arm : new PartDefinition[]{rig.rArm, rig.lArm}) {
            boolean r = arm == rig.rArm;
            rig.box(arm, plate, r ? -3.5f : -1.5f, -3, -2.5f, 5, 4, 5);   // shoulder pad
            rig.box(arm, suit, r ? -3 : -1, -3.6f, -2, 4, 1, 4);        // pad top layer
            rig.box(arm, metal, -2.5f, 4.5f, -2.5f, 5, 4, 5);           // gauntlet
        }
        rig.box(rig.lArm, red, -2.5f, 8.5f, -2.6f, 5, 1, 1);            // blood punch knuckles
        rig.box(rig.lArm, metal, 2.3f, 3.5f, -1, 1, 4, 2);              // doom blade housing
        // ---- legs ----
        for (PartDefinition leg : new PartDefinition[]{rig.rLeg, rig.lLeg}) {
            rig.box(leg, plate, -2.3f, 1, -2.6f, 4, 4, 1);              // thigh plate
            rig.box(leg, metal, -2, 5.5f, -3, 4, 2, 1);                 // knee pad
            rig.box(leg, dark, -2.5f, 9, -3, 5, 3, 6);                  // boot
        }

        // ================= weapons (gun space: fist origin, barrel -z, y down) =================
        PartDefinition w;
        w = rig.weapon(rig.rHand, "super_shotgun");
        rig.box(w, dark, -2, -3, -15, 2, 2, 11);
        rig.box(w, dark, 0, -3, -15, 2, 2, 11);
        rig.box(w, metal, -2, -3.5f, -4, 4, 3, 5);
        rig.box(w, wood, -1.5f, -2, 1, 3, 3, 5);
        rig.box(w, metal, -1, -1, -15, 2, 1, 3);                        // meat hook housing
        rig.box(w, orange, -0.5f, -0.2f, -16, 1, 1, 2);                 // hook claw

        w = rig.weapon(rig.rHand, "combat_shotgun");
        rig.box(w, dark, -1.5f, -3, -7, 3, 3, 9);
        rig.box(w, metal, -1, -3.5f, -14, 2, 2, 7);
        rig.box(w, leather, -1.5f, -1.2f, -12, 3, 2, 4);                // pump
        rig.box(w, dark, -1, -2.5f, 2, 2, 3, 4);
        rig.box(w, orange, 1.5f, -3, -10, 1, 1, 3);                     // sticky bomb module

        w = rig.weapon(rig.rHand, "heavy_cannon");
        rig.box(w, metal, -1.5f, -3.5f, -8, 3, 4, 10);
        rig.box(w, dark, -1, -3, -15, 2, 2, 7);
        rig.box(w, dark, -1, -5.5f, -6, 2, 2, 5);                       // scope
        rig.box(w, red, -0.5f, -5, -6.3f, 1, 1, 1);
        rig.box(w, dark, -1, 0.5f, -5, 2, 3, 2);                        // magazine

        w = rig.weapon(rig.rHand, "plasma_rifle");
        rig.box(w, dark, -2, -3.5f, -9, 4, 4, 10);
        rig.box(w, blue, -2.6f, -3, -7, 1, 2, 5);
        rig.box(w, blue, 1.6f, -3, -7, 1, 2, 5);
        rig.box(w, metal, -1.5f, -3, -12, 3, 3, 3);

        w = rig.weapon(rig.rHand, "rocket_launcher");
        rig.box(w, metal, -2, -4.5f, -12, 4, 4, 15);
        rig.box(w, dark, -2.5f, -5, -13, 5, 5, 1);
        rig.box(w, orange, -1, -3.5f, -13.3f, 2, 2, 1);
        rig.box(w, dark, -0.5f, -0.5f, -3, 1, 3, 2);                    // grip

        w = rig.weapon(rig.rHand, "ballista");
        rig.box(w, metal, -1.5f, -3.5f, -10, 3, 3, 13);
        rig.box(w, dark, -3, -3, -14, 1, 2, 6);
        rig.box(w, dark, 2, -3, -14, 1, 2, 6);
        rig.box(w, orange, -0.5f, -3, -13, 1, 1, 3);

        w = rig.weapon(rig.rHand, "gauss");
        rig.box(w, metal, -1.5f, -3.5f, -8, 3, 4, 11);
        rig.box(w, dark, -1, -3, -16, 2, 2, 8);
        rig.box(w, blue, -2, -4, -12, 4, 4, 1);
        rig.box(w, blue, -2, -4, -15, 4, 4, 1);

        w = rig.weapon(rig.rHand, "chaingun");
        rig.box(w, dark, -2, -4, -16, 4, 4, 10);
        rig.box(w, metal, -2.5f, -4.5f, -6, 5, 5, 8);
        rig.box(w, dark, -0.5f, -6.5f, -4, 1, 2, 4);                    // carry handle

        w = rig.weapon(rig.rHand, "bfg");
        rig.box(w, metal, -3, -5, -10, 6, 6, 12);
        rig.box(w, green, -2, -4, -12, 4, 3, 2);
        rig.box(w, dark, -1.5f, -3.5f, -14, 3, 3, 2);

        w = rig.weapon(rig.rHand, "chainsaw");
        rig.box(w, Mat.of(0xb02a10).noise(0.1f), -1.5f, -3, -4, 3, 4, 6);
        rig.box(w, metal, -0.5f, -2, -14, 1, 3, 10);
        rig.box(w, dark, -0.6f, -2.3f, -14.3f, 1, 1, 10);              // chain

        w = rig.weapon(rig.rHand, "crucible");
        rig.box(w, metal, -1, -1, -1, 2, 2, 4);
        rig.box(w, dark, -1.5f, -1.5f, -2, 3, 3, 1);
        rig.box(w, red, -0.5f, -1, -18, 1, 2, 16);

        w = rig.weapon(rig.lHand, "doom_blade");
        rig.box(w, red, -0.5f, -2.5f, -9, 1, 1, 9);
    }
}
