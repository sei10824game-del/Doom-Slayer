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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Master Chief, Halo: Combat Evolved. MJOLNIR Mark V, recharging shields, the full CE sandbox. */
public class HaloCeChief extends SlayerKit {
    static final int SHIELD_C = 0xffc040, VISOR = 0xe8a634, PP_GREEN = 0x70ff50, PR_BLUE = 0x5aa8ff, NEEDLE = 0xff5ad2, SWORD_C = 0x8cc8ff;
    static final float SHIELD_MAX = 16f;
    static final int SHIELD_DELAY = 70, PNADE_LIFE = 80;

    static final ProjStyle ROCKET_CE = ProjStyle.make().color(0xffa040).size(0.5f).life(100).explode(4f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle FRAG_CE = ProjStyle.make().color(0x4b5a32).size(0.26f).life(40).explode(3.5f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle PP_BOLT = ProjStyle.make().color(PP_GREEN).size(0.3f).life(45).trail(ParticleTypes.HAPPY_VILLAGER);
    static final ProjStyle PR_BOLT = ProjStyle.make().color(PR_BLUE).size(0.3f).life(45).trail(ParticleTypes.ELECTRIC_SPARK);
    /** plasma pistol overcharge: big homing bolt, EMP burst */
    static final ProjStyle PP_OVERCHARGE = ProjStyle.make().color(PP_GREEN).size(0.75f).life(70).homing(0.25f).explode(1.6f).trail(ParticleTypes.GLOW)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, PP_GREEN, 1.8f, pos, 30, 0.6);
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 25, 0.5, 0.4);
                Fx.ring(l, pos, 1.4f, PP_GREEN, 1.2f, 18);
                Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1.2f, 1.8f);
            });
    /** Needler: homing pink shards; 7 in the same target -> supercombine detonation */
    static final ProjStyle NEEDLE_P = ProjStyle.make().color(NEEDLE).size(0.18f).life(55).homing(0.18f).trail(ParticleTypes.END_ROD)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, NEEDLE, 0.9f, pos, 5, 0.12);
                if (!(hit instanceof LivingEntity v) || !(p.getOwner() instanceof SlayerEntity o)) { Fx.sound(l, pos, SoundEvents.AMETHYST_BLOCK_HIT, 0.6f, 1.6f); return; }
                CompoundTag d = v.getPersistentData();
                long now = l.getGameTime();
                int c = now - d.getLong("hceNdlT") > 60 ? 1 : d.getInt("hceNdl") + 1;
                d.putLong("hceNdlT", now);
                Fx.sound(l, pos, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.2f + c * 0.1f);
                if (c >= 7) {
                    c = 0;
                    Vec3 m = v.position().add(0, v.getBbHeight() * 0.6, 0);
                    Fx.dustBurst(l, NEEDLE, 2.2f, m, 50, 0.9);
                    Fx.burst(l, ParticleTypes.END_ROD, m, 30, 0.4, 0.35);
                    Fx.ring(l, m, 2f, NEEDLE, 1.5f, 24);
                    Combat.blast(o, m, 2.8f, 16f, 0.9f, null);
                    Fx.sound(l, m, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6f, 0.6f);
                    Fx.sound(l, m, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.5f);
                }
                d.putInt("hceNdl", c);
            });
    /** plasma grenade: sticks to the first foe it passes, then cooks off */
    static final ProjStyle PLASMA_NADE = ProjStyle.make().color(0x50b0ff).size(0.32f).life(PNADE_LIFE).explode(3.2f).gravity(0.035).trail(ParticleTypes.ELECTRIC_SPARK)
            .onTick(p -> {
                if (!(p.getOwner() instanceof SlayerEntity o)) return;
                ServerLevel l = (ServerLevel) p.level();
                if (p.aux > 0) {
                    Entity v = l.getEntity(p.aux);
                    if (v != null && v.isAlive() && p.auxVec != null) { Vec3 at = v.position().add(p.auxVec); p.setPos(at.x, at.y, at.z); }
                    p.setDeltaMovement(Vec3.ZERO);
                    Fx.dustBurst(l, 0x80d0ff, 1.1f, p.position(), 3, 0.1);
                    if (p.age2 % 4 == 0) Fx.sound(l, p.position(), SoundEvents.FIRE_EXTINGUISH, 0.4f, 1.8f);
                    return;
                }
                Vec3 next = p.position().add(p.getDeltaMovement());
                for (LivingEntity t : Combat.around(o, next, 1.3)) {
                    Vec3 c = t.position().add(0, t.getBbHeight() * 0.6, 0), out = p.position().subtract(c);
                    out = new Vec3(out.x, 0, out.z);
                    out = out.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : out.normalize();
                    p.aux = t.getId();
                    p.auxVec = new Vec3(0, t.getBbHeight() * 0.6, 0).add(out.scale(t.getBbWidth() * 0.5 + 0.15));
                    p.age2 = Math.max(p.age2, PNADE_LIFE - 24);
                    Fx.sound(l, p.position(), SoundEvents.SLIME_ATTACK, 1f, 1.4f);
                    Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, p.position(), 12, 0.2, 0.2);
                    break;
                }
            })
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 3f);
                Fx.dustBurst(l, 0x60c0ff, 2f, pos, 40, 1.2);
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 30, 0.8, 0.5);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1.2f, 1.3f);
                Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1f, 2f);
            });

    public HaloCeChief() {
        super("halo_ce_chief", "Master Chief", "マスターチーフ", "Halo: Combat Evolved");
        eggPrimary = 0x56653a; eggSecondary = VISOR;
        maxHealth = 75; speed = 0.31; armor = 14; knockbackRes = 0.55;
        scale = 1.1f; width = 0.68f; preferredRange = 10f;
        idleWeaponR = "ar";
    }

    // ================= helpers (server) =================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.2, 0.3, 0); }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        return Combat.dirTo(mz(e), t, spread, new Random(e.getRandom().nextLong()));
    }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static void flash(SlayerEntity e, int rgb) {
        Vec3 m = mz(e);
        Fx.burst(L(e), ParticleTypes.FLAME, m, 2, 0.04, 0.02);
        Fx.dustBurst(L(e), rgb, 1.1f, m, 3, 0.07);
    }
    static void brass(SlayerEntity e) { Fx.dustBurst(L(e), 0xc8a040, 0.6f, Combat.muzzle(e, 0.3, 0.55, 0.1), 1, 0.05); }
    static void hold(SlayerEntity e, LivingEntity t, double dist) {
        if (t == null || !Combat.isFoe(e, t)) return;
        Vec3 f = Vec3.directionFromRotation(0, e.getYRot());
        Vec3 p = e.position().add(f.scale(dist + t.getBbWidth() * 0.5));
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true;
    }
    static void vent(SlayerEntity e, int rgb) {
        Vec3 m = Combat.muzzle(e, 0.8, 0.45, 0.1);
        Fx.burst(L(e), ParticleTypes.CLOUD, m, 2, 0.08, 0.03);
        Fx.dustBurst(L(e), rgb, 0.8f, m, 2, 0.12);
    }
    static void lob(SlayerEntity e, LivingEntity t, ProjStyle s, float dmg) {
        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.45);
        double dist = t != null ? from.distanceTo(mid(t)) : 10;
        Vec3 d = aim(e, t, 0.02).add(0, 0.12 + dist * 0.02, 0).normalize();
        Combat.shoot(e, s, from, d, 0.7 + dist * 0.028, dmg);
    }

    /** MJOLNIR energy shield: absorption that recharges after SHIELD_DELAY ticks without damage. */
    @Override
    public void passive(SlayerEntity e) {
        CompoundTag d = e.getPersistentData();
        float abs = e.getAbsorptionAmount(), total = e.getHealth() + abs;
        int quiet = d.getInt("hceQuiet");
        boolean up = d.getBoolean("hceUp");
        Vec3 c = e.position().add(0, e.getBbHeight() * 0.55, 0);
        if (total < d.getFloat("hcePrev") - 0.01f) {
            quiet = 0;
            if (up && abs <= 0) { // shield collapses
                up = false;
                Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, c, 25, 0.4, 0.3);
                Fx.dustBurst(L(e), SHIELD_C, 1.4f, c, 20, 0.5);
                snd(e, SoundEvents.BEACON_DEACTIVATE, 1.2f, 1.6f);
            } else if (abs > 0) { // flare
                Fx.dustBurst(L(e), SHIELD_C, 1.2f, c, 12, 0.45);
                snd(e, SoundEvents.AMETHYST_BLOCK_HIT, 0.8f, 0.6f);
            }
        } else quiet++;
        if (quiet >= SHIELD_DELAY && abs < SHIELD_MAX) {
            if (quiet == SHIELD_DELAY) snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.7f);
            abs = Math.min(SHIELD_MAX, abs + 0.4f);
            e.setAbsorptionAmount(abs);
            if (e.tickCount % 3 == 0) Fx.dustBurst(L(e), SHIELD_C, 0.8f, c, 4, 0.45);
            if (abs >= SHIELD_MAX) { up = true; snd(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.9f); }
        }
        if (abs > 0) up = true;
        d.putInt("hceQuiet", Math.min(quiet, 10000));
        d.putBoolean("hceUp", up);
        d.putFloat("hcePrev", e.getHealth() + e.getAbsorptionAmount());
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** two-handed shouldered long gun: squared chest, cheek on the stock, elbows tucked (Chief stance) */
    static void shoulder(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -1.45f + pitch - recoil * 0.35f, k); p.rArmY = lerp(p.rArmY, -0.28f, k); p.rArmZ = lerp(p.rArmZ, 0.06f, k);
        p.lArmX = lerp(p.lArmX, -1.52f + pitch - recoil * 0.25f, k); p.lArmY = lerp(p.lArmY, 0.55f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += 0.18f * k; p.headY -= 0.14f * k; p.headX += (0.06f - recoil * 0.06f) * k;
        p.bodyX += (0.06f + recoil * 0.1f) * k; p.rootZ += recoil * 0.8f * k; p.rHandX -= recoil * 0.2f * k;
    }
    /** one-handed sidearm, bladed stance, off hand down and back */
    static void oneHand(Pose p, float k, float pitch, float recoil, float blade) {
        p.bodyY -= blade * k; p.headY += blade * 0.9f * k;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.45f, k); p.rArmY = lerp(p.rArmY, blade, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, 0.2f, k); p.lArmZ = lerp(p.lArmZ, -0.18f, k);
        p.rHandX -= recoil * 0.35f * k; p.bodyX -= recoil * 0.05f * k; p.rootZ += recoil * 0.4f * k;
    }
    static void plant(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.25f, k);
        p.lLegZ -= 0.05f * k; p.rLegZ += 0.07f * k; p.rootY -= 0.4f * k;
    }
    static void brace(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.35f, k); p.rLegX = lerp(p.rLegX, 0.35f, k);
        p.lLegZ -= 0.22f * k; p.rLegZ += 0.22f * k; p.rootY -= 1.1f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // MA5B at low ready across the chest
        p.rArmX = -0.62f + p.rArmX * 0.25f; p.rArmY = -0.3f;
        p.lArmX = -0.88f + p.lArmX * 0.25f; p.lArmY = 0.62f; p.lArmZ = 0.04f;
        p.rHandX = -0.25f; p.bodyX += 0.03f + 0.015f * (float) Math.sin(age * 0.08f);
        p.headX += 0.02f * (float) Math.sin(age * 0.08f + 1);
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- Melee: MA5B rifle-butt bash ----------
        out.add(AttackDef.of("rifle_butt").range(0, 3.2f).cd(20).weight(3).dur(14).weapon("ar")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.3f);
                    if (n == 6) {
                        for (LivingEntity f : Combat.cone(e, 3.4, 55)) {
                            Combat.damage(e, f, 9); Combat.knock(f, e.position(), 1.0, 0.3); Fx.sparks(L(e), mid(f), 8);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.7f); snd(e, SoundEvents.ANVIL_LAND, 0.3f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -0.62f, .25f, -0.3f, .43f, -1.75f, .62f, -1.55f, 1, -0.62f);
                    p.rArmY = track(t, 0, -0.3f, .25f, 0.25f, .43f, -0.5f, 1, -0.3f);
                    p.lArmX = track(t, 0, -0.88f, .25f, -0.5f, .43f, -1.95f, .62f, -1.7f, 1, -0.88f); p.lArmY = 0.6f;
                    p.rHandX = track(t, 0, -0.25f, .25f, 0.6f, .43f, 1.4f, .65f, 1.2f, 1, -0.25f);
                    p.bodyY = track(t, 0, 0, .25f, -0.5f, .43f, 0.45f, .65f, 0.35f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyX = track(t, 0, 0, .25f, -0.12f, .43f, 0.32f, .7f, 0.2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, .8f, -0.45f, 1, 0); p.rLegX = track(t, 0, 0, .43f, 0.4f, .8f, 0.3f, 1, 0);
                    p.rootZ = -2.2f * bump(win(t, .25f, .85f)); p.rootY = -0.5f * bump(win(t, .2f, .9f));
                }));

        // ---------- Melee: overhand hammer punch ----------
        out.add(AttackDef.of("hammer_punch").range(0, 3).cd(22).weight(2).dur(16).weapon("none")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.6f, 0.7f);
                    if (n == 8) {
                        for (LivingEntity f : Combat.cone(e, 3.2, 50)) {
                            Combat.damage(e, f, 8); Combat.knock(f, e.position(), 0.6, -0.1); Fx.sparks(L(e), mid(f), 6);
                        }
                        Fx.burst(L(e), ParticleTypes.POOF, Combat.muzzle(e, 1.4, 0, -0.3), 5, 0.2, 0.02);
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, 0, .3f, -2.85f, .42f, -2.9f, .5f, -1.0f, .7f, -0.9f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .3f, 0.35f, .5f, -0.1f, 1, 0);
                    p.lArmX = track(t, 0, 0, .2f, -1.1f, .6f, -0.9f, 1, 0); p.lArmZ = track(t, 0, 0, .2f, -0.35f, 1, 0); p.lArmY = 0.3f * win(t, 0, .2f) * (1 - win(t, .7f, 1));
                    p.bodyX = track(t, 0, 0, .3f, -0.25f, .42f, -0.25f, .52f, 0.5f, .75f, 0.4f, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, 0.3f, .5f, -0.2f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.headX = track(t, 0, 0, .3f, -0.2f, .5f, 0.25f, 1, 0);
                    p.lLegX = track(t, 0, 0, .35f, -0.55f, .8f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .45f, 0.35f, .8f, 0.3f, 1, 0);
                    p.rootY = track(t, 0, 0, .3f, 0.5f, .5f, -1.2f, .8f, -0.8f, 1, 0); p.rootZ = -1.8f * bump(win(t, .3f, .9f));
                }));

        // ---------- Execution: grab + double rifle-butt beatdown ----------
        out.add(AttackDef.of("beatdown_execution").range(0, 3.5f).lowHealthTarget(0.3f).weight(6).cd(80).dur(30).weapon("ar")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.45, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.7f); }
                    hold(e, t, 0.9);
                    if (n == 10 && t != null) {
                        Combat.damage(e, t, Math.max(0, t.getHealth() - 1)); Fx.sparks(L(e), mid(t), 10);
                        snd(e, SoundEvents.ANVIL_LAND, 0.5f, 1.3f); snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1, 0.8f);
                    }
                    if (n == 19 && t != null) {
                        Combat.damage(e, t, t.getHealth() + 100);
                        Fx.sparks(L(e), mid(t), 18); Fx.burst(L(e), ParticleTypes.POOF, t.position(), 10, 0.3, 0.05);
                        snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 0.6f); snd(e, SoundEvents.ANVIL_LAND, 0.7f, 0.9f);
                    }
                })
                .anim((p, t, a) -> {
                    p.lArmX = track(t, 0, -0.88f, .1f, -1.65f, .55f, -1.5f, .62f, -0.4f, 1, -0.88f); p.lArmY = track(t, 0, 0.6f, .1f, 0.25f, .62f, 0.2f, 1, 0.6f);
                    p.rArmX = track(t, 0, -0.62f, .22f, -2.9f, .33f, -1.3f, .45f, -2.95f, .63f, -0.9f, .8f, -0.8f, 1, -0.62f);
                    p.rArmZ = track(t, 0, 0, .22f, -0.2f, .63f, 0.15f, 1, 0);
                    p.rHandX = track(t, 0, -0.25f, .22f, 1.5f, .45f, 1.6f, .63f, 1.3f, .85f, 1.0f, 1, -0.25f);
                    p.bodyX = track(t, 0, 0, .22f, -0.3f, .33f, 0.35f, .45f, -0.35f, .63f, 0.55f, .8f, 0.45f, 1, 0);
                    p.bodyY = track(t, 0, 0, .1f, -0.2f, .45f, -0.25f, .63f, 0.15f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.headX = track(t, .3f, 0, .63f, 0.35f, 1, 0);
                    p.rootY = track(t, 0, 0, .22f, 0.6f, .33f, -0.6f, .45f, 0.8f, .63f, -1.5f, .85f, -1.0f, 1, 0);
                    p.rootZ = track(t, 0, 0, .1f, -1.8f, .85f, -1.8f, 1, 0);
                    p.lLegX = track(t, 0, 0, .1f, -0.5f, .85f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .1f, 0.35f, .63f, 0.5f, .85f, 0.4f, 1, 0);
                }));

        // ---------- M6D pistol: three precise zoomed shots ----------
        out.add(AttackDef.of("m6d_pistol").range(3, 26).cd(28).weight(3).dur(22).weapon("pistol")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.SPYGLASS_USE, 0.6f, 1.4f);
                    if (n == 4 || n == 9 || n == 14) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.006), 30, 7f, 0, 0xffe8a0); flash(e, 0xffc060); brass(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 1.1f, 1.2f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.15f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f), zoom = win(t, .08f, .18f) * (1 - win(t, .7f, .85f));
                    float r = kick(t, 4 / 22f, .14f) + kick(t, 9 / 22f, .14f) + kick(t, 14 / 22f, .14f);
                    oneHand(p, k, -0.04f, r, 0.42f);
                    p.headX += 0.1f * zoom; p.headZ += 0.08f * zoom;
                    p.lLegX = lerp(p.lLegX, 0.25f, k); p.rLegX = lerp(p.rLegX, -0.25f, k); p.rootY -= 0.3f * k;
                }));

        // ---------- MA5B assault rifle: full-auto burst with bloom + mag check ----------
        out.add(AttackDef.of("ma5b_full_auto").range(2, 20).cd(34).weight(4).dur(34).weapon("ar")
                .tick((e, t, n) -> {
                    if (n >= 5 && n <= 26) {
                        double spread = 0.03 + (n - 5) * 0.004;
                        Combat.hitscan(e, mz(e), aim(e, t, spread), 22, 1.5f, 0, 0xffe080);
                        if (n % 2 == 0) { flash(e, 0xffb050); brass(e); }
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.6f + e.getRandom().nextFloat() * 0.2f);
                    }
                    if (n == 29) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.5f, 1.8f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .92f), fire = win(t, .13f, .16f) * (1 - win(t, .77f, .8f));
                    float jit = 0.05f * (float) Math.sin(a * 7.3f) * fire, climb = 0.14f * win(t, .15f, .77f) * fire;
                    shoulder(p, k, -climb, 0.25f * fire + jit); plant(p, k);
                    p.bodyZ += jit * 0.35f; p.rHandZ += jit * 0.5f; p.headX += 0.05f * fire;
                    float chk = bump(win(t, .8f, .96f)); // tap the magazine, glance at the ammo counter
                    p.lArmX += 0.7f * chk; p.lArmY -= 0.25f * chk; p.headX += 0.2f * chk; p.rHandY += 0.35f * chk;
                }));

        // ---------- M90 shotgun: blast + pump ----------
        out.add(AttackDef.of("m90_shotgun").range(0, 9).cd(26).weight(4).dur(24).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 6) {
                        Vec3 m = mz(e);
                        for (int i = 0; i < 12; i++) Combat.hitscan(e, m, aim(e, t, 0.11), 16, 2.4f, 0, 0xffd070);
                        for (LivingEntity f : Combat.cone(e, 3.5, 35)) Combat.knock(f, e.position(), 1.0, 0.3);
                        flash(e, 0xffa040); flash(e, 0xff7020);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.6f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 0.8f);
                    }
                    if (n == 13) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.7f);
                    if (n == 17) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.8f); Fx.dustBurst(L(e), 0xc02020, 0.7f, Combat.muzzle(e, 0.4, 0.55, 0.1), 1, 0.05); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = kick(t, 6 / 24f, .3f), pump = bump(win(t, .52f, .74f));
                    shoulder(p, k, 0.04f, r * 1.7f); plant(p, k);
                    p.lArmX += 0.42f * pump * k; p.lArmY -= 0.12f * pump; p.bodyZ += 0.05f * pump;
                    p.bodyX -= r * 0.22f; p.rootY += r * 0.5f; p.headX -= r * 0.12f; p.rHandX -= r * 0.3f;
                }));

        // ---------- S2 AM sniper rifle: kneel, scope, two piercing shots ----------
        out.add(AttackDef.of("s2am_sniper").range(10, 48).cd(70).weight(2).dur(40).weapon("sniper")
                .tick((e, t, n) -> {
                    if (n == 5) snd(e, SoundEvents.SPYGLASS_USE, 1, 0.9f);
                    if (n == 16 || n == 30) {
                        Vec3 m = mz(e), d = aim(e, t, 0.002);
                        Combat.hitscan(e, m, d, 50, 16f, 2, 0xd0e8ff);
                        Fx.beam(L(e), m, t != null ? mid(t) : m.add(d.scale(50)), 0xffffff, 0.6f, 0.35);
                        flash(e, 0xffd080); brass(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.4f, 0.7f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.8f);
                    }
                    if (n == 34) snd(e, SoundEvents.SPYGLASS_STOP_USING, 1, 0.9f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), zoom = win(t, .1f, .3f) * (1 - win(t, .85f, .95f));
                    float r = 1.6f * (kick(t, 16 / 40f, .15f) + kick(t, 30 / 40f, .15f));
                    shoulder(p, k, 0, r);
                    p.lLegX = lerp(p.lLegX, -0.75f, zoom); p.rLegX = lerp(p.rLegX, 0.8f, zoom); p.rootY -= 3f * zoom; // kneel
                    p.headX += 0.22f * zoom; p.headY -= 0.1f * zoom; p.bodyX += 0.14f * zoom;
                    p.bodyZ += 0.012f * (float) Math.sin(a * 0.35f) * zoom; // breathing sway
                    p.rHandX -= r * 0.25f;
                }));

        // ---------- M19 SSM rocket launcher: two shoulder-fired rockets ----------
        out.add(AttackDef.of("m19_rocket").range(7, 32).cd(140).weight(2).dur(42).weapon("rocket")
                .when((e, t) -> t.getMaxHealth() >= 30 || Combat.around(e, t.position(), 4).size() >= 2)
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.PISTON_EXTEND, 0.7f, 1.2f);
                    if (n == 12 || n == 26) {
                        Combat.shoot(e, ROCKET_CE, Combat.muzzle(e, 1.3, 0.35, 0.25), aim(e, t, 0.01), 1.1, 18f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.6, 0.35, 0.25), 8, 0.2, 0.05); // backblast
                        flash(e, 0xffa040);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.6f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.6f);
                    }
                    if (n == 34) snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.7f, 1.2f);
                    if (n == 38) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.7f, 1.2f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 2.4f * (kick(t, 12 / 42f, .16f) + kick(t, 26 / 42f, .16f));
                    float aimK = k * (1 - win(t, .72f, .8f) + win(t, .92f, 1) * 0);
                    p.rArmX = lerp(p.rArmX, -1.85f - r * 0.3f, aimK); p.rArmY = lerp(p.rArmY, -0.1f, aimK); p.rArmZ = lerp(p.rArmZ, 0.1f, aimK);
                    p.lArmX = lerp(p.lArmX, -1.6f - r * 0.2f, aimK); p.lArmY = lerp(p.lArmY, 0.6f, aimK);
                    p.rHandX += (0.3f - r * 0.2f) * aimK;
                    p.headY -= 0.22f * aimK; p.headZ += 0.12f * aimK; p.bodyY += 0.2f * aimK;
                    p.rootZ += r * 1.5f; p.bodyX -= r * 0.25f; brace(p, k);
                    float rl = bump(win(t, .74f, .96f)); // drop to reload the twin tubes
                    p.rArmX = lerp(p.rArmX, -0.9f, rl); p.rHandX += 0.9f * rl;
                    p.lArmX = lerp(p.lArmX, -0.4f, rl); p.lArmZ -= 0.3f * rl; p.headX += 0.35f * rl; p.bodyX += 0.25f * rl;
                }));

        // ---------- M9 HE-DP frag grenade ----------
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(150).weight(2).dur(20).weapons("ar", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.4f);
                    if (n == 10) { lob(e, t, FRAG_CE, 15f); snd(e, SoundEvents.WITCH_THROW, 1, 0.8f); }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .52f ? "frag" : null;
                    p.rArmX = track(t, 0, -0.62f, .12f, -1.0f, .22f, -0.5f, 1, -0.62f); p.rArmY = track(t, 0, -0.3f, .12f, 0.3f, .22f, -0.3f, 1, -0.3f);
                    p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, -0.88f, .12f, -1.0f, .42f, 0.95f, .52f, -2.5f, .7f, -1.3f, 1, -0.88f);
                    p.lArmZ = track(t, 0, 0, .42f, -0.55f, .52f, -0.1f, 1, 0); p.lArmY = track(t, 0, 0.6f, .12f, 0.5f, .42f, 0, .7f, 0.3f, 1, 0.6f);
                    p.bodyY = track(t, 0, 0, .42f, -0.6f, .55f, 0.5f, .75f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .42f, -0.22f, .55f, 0.32f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.35f, .55f, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, .42f, 0.3f, .55f, 0.45f, 1, 0);
                    p.rootZ = track(t, .4f, 0, .55f, -1.5f, 1, 0); p.rootY = -0.5f * bump(win(t, .4f, .8f));
                }));

        // ---------- Plasma grenade (sticky) ----------
        out.add(AttackDef.of("plasma_grenade").range(4, 16).cd(170).weight(2).dur(24).weapons("ar", "pnade")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.7f, 2f);
                    if (n > 4 && n < 12) Fx.dustBurst(L(e), 0x60c0ff, 0.8f, Combat.muzzle(e, 0.3, -0.45, 0.3), 2, 0.08);
                    if (n == 12) { lob(e, t, PLASMA_NADE, 18f); snd(e, SoundEvents.WITCH_THROW, 1, 1.1f); snd(e, SoundEvents.FIRE_EXTINGUISH, 0.5f, 1.6f); }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .52f ? "pnade" : null;
                    p.rArmX = track(t, 0, -0.62f, .4f, -0.4f, .6f, -0.9f, 1, -0.62f); p.rArmY = -0.3f; p.rHandX = -0.3f;
                    // low back sweep -> high release, crow-hop forward
                    p.lArmX = track(t, 0, -0.88f, .15f, 0.4f, .38f, 1.1f, .5f, -2.7f, .68f, -1.6f, 1, -0.88f);
                    p.lArmZ = track(t, 0, 0, .15f, -0.25f, .38f, -0.7f, .5f, 0.05f, 1, 0); p.lArmY = track(t, 0, 0.6f, .2f, 0, .68f, 0.25f, 1, 0.6f);
                    p.lHandX = track(t, .15f, 0, .38f, 0.6f, .52f, -0.4f, 1, 0);
                    p.bodyY = track(t, 0, 0, .38f, -0.75f, .52f, 0.6f, .75f, 0.35f, 1, 0); p.headY = -p.bodyY * 0.75f;
                    p.bodyX = track(t, 0, 0, .3f, -0.3f, .52f, 0.4f, .8f, 0.15f, 1, 0);
                    p.rootY = track(t, 0, 0, .25f, -0.6f, .38f, 1.0f, .5f, -0.4f, .7f, -0.8f, 1, 0);
                    p.rootZ = track(t, 0, 0, .3f, 1.0f, .52f, -2.2f, .8f, -1.6f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, 0.3f, .45f, -0.7f, .8f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .3f, -0.3f, .45f, 0.5f, .8f, 0.4f, 1, 0);
                }));

        // ---------- Plasma pistol: rapid semi-auto bolts ----------
        out.add(AttackDef.of("plasma_pistol").range(3, 20).cd(26).weight(2).dur(20).weapon("ppistol")
                .tick((e, t, n) -> {
                    if (n == 3 || n == 6 || n == 9 || n == 12) {
                        Combat.shoot(e, PP_BOLT, mz(e), aim(e, t, 0.03), 1.4, 3.2f);
                        Fx.dustBurst(L(e), PP_GREEN, 0.9f, mz(e), 3, 0.06);
                        snd(e, SoundEvents.BLAZE_SHOOT, 0.45f, 2f);
                    }
                    if (n == 15) vent(e, PP_GREEN);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .85f);
                    float r = kick(t, 3 / 20f, .1f) + kick(t, 6 / 20f, .1f) + kick(t, 9 / 20f, .1f) + kick(t, 12 / 20f, .1f);
                    oneHand(p, k, 0.12f, r * 0.8f, 0.25f);
                    p.rHandY += 0.45f * k; // canted alien grip
                    p.lArmX = lerp(p.lArmX, -0.5f, k); p.lArmZ -= 0.15f * k; // off hand up as a guard
                    plant(p, k);
                }));

        // ---------- Plasma pistol: overcharge (charge, homing EMP bolt, overheat) ----------
        out.add(AttackDef.of("plasma_pistol_overcharge").range(4, 22).cd(110).weight(2).dur(44).weapon("ppistol")
                .when((e, t) -> t.getMaxHealth() >= 20)
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.BEACON_AMBIENT, 1.2f, 1.6f);
                    if (n > 2 && n < 28) {
                        Fx.dustBurst(L(e), PP_GREEN, 0.6f + n * 0.04f, mz(e), 2, 0.05 + n * 0.004);
                        if (n % 5 == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.5f, 1.0f + n * 0.03f);
                    }
                    if (n == 28) {
                        Combat.shoot(e, PP_OVERCHARGE, mz(e), aim(e, t, 0), 0.9, 12f);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mz(e), 12, 0.15, 0.2);
                        snd(e, SoundEvents.BLAZE_SHOOT, 1.3f, 0.7f); snd(e, SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.5f);
                    }
                    if (n >= 31 && n <= 40 && n % 2 == 1) { vent(e, PP_GREEN); if (n == 31) snd(e, SoundEvents.FIRE_EXTINGUISH, 0.8f, 1.2f); }
                })
                .anim((p, t, a) -> {
                    p.weaponR = t > .05f && t < .65f ? "ppistol_oc" : "ppistol";
                    float k = ready(t, .1f, .92f), ch = win(t, .05f, .63f) * (1 - win(t, .63f, .65f)), r = 2.4f * kick(t, 28 / 44f, .18f);
                    float vt = win(t, .7f, .76f) * (1 - win(t, .92f, 1));
                    oneHand(p, k, -0.05f - 0.6f * vt, r, 0.15f * (1 - vt));
                    p.lArmX = lerp(p.lArmX, -1.35f, ch); p.lArmY = lerp(p.lArmY, 0.75f, ch); // brace the wrist while charging
                    float buzz = 0.04f * ch * ch * (float) Math.sin(a * 6.1f);
                    p.rArmY += buzz; p.bodyZ += buzz * 0.5f; p.rootY -= 0.7f * ch; p.headX += 0.08f * ch;
                    p.rootZ += r * 0.8f; p.bodyX -= r * 0.1f;
                    p.rHandZ += 0.15f * vt * (float) Math.sin(a * 3f); p.rHandY += 0.6f * vt; p.headX += 0.3f * vt; p.headY += 0.2f * vt; // inspect the hot emitter
                    plant(p, k);
                }));

        // ---------- Plasma rifle: sustained bolts then overheat vent ----------
        out.add(AttackDef.of("plasma_rifle").range(3, 20).cd(40).weight(3).dur(38).weapon("prifle")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 24 && n % 2 == 0) {
                        Combat.shoot(e, PR_BOLT, mz(e), aim(e, t, 0.04), 1.6, 2.8f);
                        Fx.dustBurst(L(e), PR_BLUE, 0.9f, mz(e), 2, 0.06);
                        snd(e, SoundEvents.BLAZE_SHOOT, 0.45f, 1.8f + e.getRandom().nextFloat() * 0.2f);
                    }
                    if (n == 26) snd(e, SoundEvents.FIRE_EXTINGUISH, 1f, 0.9f);
                    if (n >= 26 && n <= 35) vent(e, 0xff8040);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .92f), fire = win(t, .1f, .12f) * (1 - win(t, .64f, .66f));
                    float vt = win(t, .68f, .74f) * (1 - win(t, .94f, 1));
                    p.weaponR = t > .68f && t < .95f ? "prifle_vent" : "prifle";
                    float jit = 0.035f * (float) Math.sin(a * 5.4f) * fire;
                    p.rArmX = lerp(p.rArmX, -1.35f - 0.12f * fire - jit, k) + 0.5f * vt; p.rArmY = lerp(p.rArmY, 0.1f, k); p.rArmZ = lerp(p.rArmZ, 0.05f, k);
                    p.lArmX = lerp(p.lArmX, -0.7f, k); p.lArmZ = lerp(p.lArmZ, -0.25f, k) - 0.3f * vt; p.lArmY = lerp(p.lArmY, 0.2f, k);
                    p.rHandY += 0.7f * vt; p.rHandZ += jit; p.bodyZ += jit * 0.4f;
                    p.bodyY -= 0.15f * k; p.headY += 0.12f * k; p.headX += 0.3f * vt; p.bodyX -= 0.12f * vt; // lean away from the heat
                    p.rootZ += 0.25f * fire; plant(p, k);
                }));

        // ---------- Needler: homing crystal spray, supercombine on 7 ----------
        out.add(AttackDef.of("needler").range(3, 18).cd(36).weight(3).dur(30).weapon("needler")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 22 && n % 2 == 0) {
                        Combat.shoot(e, NEEDLE_P, mz(e), aim(e, t, 0.08).add(0, 0.06, 0), 1.0, 1.6f);
                        Fx.dustBurst(L(e), NEEDLE, 0.7f, mz(e), 2, 0.05);
                        snd(e, SoundEvents.AMETHYST_BLOCK_STEP, 0.8f, 1.6f + e.getRandom().nextFloat() * 0.3f);
                    }
                    if (n == 25) snd(e, SoundEvents.AMETHYST_CLUSTER_PLACE, 0.8f, 1.3f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), fire = win(t, .13f, .15f) * (1 - win(t, .73f, .76f));
                    float r = 0.35f * Math.abs((float) Math.sin(t * 30 * PI / 2)) * fire, sweep = 0.12f * (float) Math.sin(a * 0.9f) * fire;
                    p.rArmX = lerp(p.rArmX, -1.45f - r * 0.4f, k); p.rArmY = lerp(p.rArmY, 0.0f, k) + sweep; p.rArmZ = lerp(p.rArmZ, 0, k);
                    p.lArmX = lerp(p.lArmX, -0.6f, k); p.lArmZ = lerp(p.lArmZ, -0.3f, k);
                    p.bodyY += sweep * 0.8f; p.headY -= sweep * 0.4f; p.bodyX += 0.08f * k; p.rHandX -= r * 0.3f;
                    p.rootZ += r * 0.4f; plant(p, k);
                    float reload = bump(win(t, .78f, .97f)); // slam a fresh crystal block in
                    p.lArmX -= 0.8f * reload; p.lArmY += 0.6f * reload; p.headX += 0.2f * reload;
                }));

        // ---------- Energy sword (wrested from an Elite): pick up, ignite, lunge, sweep ----------
        out.add(AttackDef.of("energy_sword_stolen").range(2, 9).cd(400).weight(2).dur(40).weapon("sword")
                .when((e, t) -> t.getMaxHealth() >= 20)
                .tick((e, t, n) -> {
                    if (n == 6) snd(e, SoundEvents.ARMOR_EQUIP_GENERIC, 1, 0.8f);
                    if (n == 13) {
                        snd(e, SoundEvents.BLAZE_SHOOT, 0.9f, 0.6f); snd(e, SoundEvents.BEACON_ACTIVATE, 1f, 1.8f);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, Combat.muzzle(e, 0.6, 0.35, 0), 10, 0.2, 0.2);
                    }
                    if (n > 13 && n < 32) Fx.dustBurst(L(e), SWORD_C, 0.9f, Combat.muzzle(e, 0.9, 0.35, 0.1), 2, 0.12);
                    if (n == 17) { Combat.lunge(e, 1.15, 0.15); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1, 0.6f); }
                    if (n == 21) {
                        Fx.arc(L(e), e.position().add(0, 1.2, 0), e.getYRot(), 2f, 80, -80, 5, SWORD_C, 1.2f, 18);
                        for (LivingEntity f : Combat.cone(e, 3.9, 70)) {
                            Combat.damage(e, f, 24); Combat.knock(f, e.position(), 0.7, 0.25);
                            Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(f), 14, 0.25, 0.3);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.3f, 0.8f); snd(e, SoundEvents.BLAZE_HURT, 0.6f, 1.6f);
                    }
                    if (n == 34) snd(e, SoundEvents.BEACON_DEACTIVATE, 0.7f, 1.8f);
                })
                .anim((p, t, a) -> {
                    p.weaponR = t < .2f ? null : t < .33f || t > .88f ? "sword_off" : "sword";
                    p.bodyX = track(t, 0, 0, .15f, 0.8f, .28f, 0.75f, .36f, 0.05f, .45f, 0.35f, .55f, 0.45f, .75f, 0.2f, 1, 0);
                    p.rootY = track(t, 0, 0, .15f, -3f, .28f, -3f, .36f, 0, .45f, -1f, .55f, -1.6f, .8f, -1f, 1, 0);
                    p.rArmX = track(t, 0, 0, .15f, -0.95f, .28f, -0.9f, .36f, -1.6f, .45f, -1.45f, .53f, -1.3f, .7f, -1.1f, 1, 0);
                    p.rArmY = track(t, 0, 0, .36f, 0.1f, .45f, -0.95f, .53f, 0.85f, .7f, 0.7f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .36f, 0, .45f, 0.6f, .53f, -0.2f, 1, 0);
                    p.rHandX = track(t, .28f, 0, .36f, -0.7f, .45f, 0.2f, .9f, 0.1f, 1, 0);
                    p.bodyY = track(t, 0, 0, .36f, 0, .45f, -0.6f, .53f, 0.65f, .7f, 0.5f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.headX = track(t, 0, 0, .15f, 0.4f, .3f, 0.3f, .36f, 0.15f, 1, 0);
                    p.lArmX = track(t, 0, 0, .15f, -0.3f, .45f, -0.85f, .53f, -0.4f, 1, 0); p.lArmZ = -0.45f * bump(win(t, .36f, .8f));
                    p.lLegX = track(t, 0, 0, .15f, -0.65f, .28f, -0.65f, .4f, -0.2f, .45f, -0.9f, .75f, -0.8f, 1, 0);
                    p.rLegX = track(t, 0, 0, .15f, 0.45f, .28f, 0.45f, .45f, 0.7f, .75f, 0.5f, 1, 0);
                    p.rootZ = track(t, .36f, 0, .45f, -2.6f, .75f, -2f, 1, 0);
                }));

        // ---------- Health pack (CE has no health regen) ----------
        out.add(AttackDef.of("health_pack").range(0, 48).noLos().cd(400).weight(5).dur(26).weapons("ar", "medkit")
                .when((e, t) -> e.getHealth() / e.getMaxHealth() < 0.45f)
                .tick((e, t, n) -> {
                    if (n == 7) snd(e, SoundEvents.ARMOR_EQUIP_LEATHER, 1, 1.2f);
                    if (n == 14) {
                        e.heal((float) (e.getMaxHealth() * 0.45));
                        Vec3 c = e.position().add(0, 1.1, 0);
                        Fx.burst(L(e), ParticleTypes.HEART, c, 5, 0.4, 0.05);
                        Fx.dustBurst(L(e), 0xffffff, 1f, c, 14, 0.4);
                        snd(e, SoundEvents.BREWING_STAND_BREW, 1, 1.4f); snd(e, SoundEvents.EXPERIENCE_ORB_PICKUP, 1, 0.9f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t > .22f && t < .6f ? "medkit" : null;
                    p.rArmX = track(t, 0, -0.62f, .2f, -0.3f, .8f, -0.35f, 1, -0.62f); p.rArmY = -0.3f; p.rArmZ = 0.2f * win(t, 0, .2f) * (1 - win(t, .8f, 1));
                    p.lArmX = track(t, 0, -0.88f, .25f, -0.6f, .4f, -1.4f, .55f, -1.2f, .7f, -0.5f, 1, -0.88f);
                    p.lArmY = track(t, 0, 0.6f, .25f, 0.1f, .4f, 0.85f, .55f, 0.9f, 1, 0.6f);
                    p.bodyX = track(t, 0, 0, .25f, 0.7f, .4f, 0.15f, .55f, 0.2f, .7f, -0.1f, 1, 0);
                    p.headX = track(t, 0, 0, .25f, 0.45f, .45f, 0.5f, .7f, -0.15f, 1, 0);
                    p.rootY = track(t, 0, 0, .25f, -2.5f, .4f, -0.4f, .7f, 0, 1, 0);
                    p.lLegX = track(t, 0, 0, .25f, -0.6f, .45f, -0.2f, 1, 0); p.rLegX = track(t, 0, 0, .25f, 0.5f, .45f, 0.15f, 1, 0);
                    p.bodyZ = 0.05f * bump(win(t, .52f, .62f)); p.rootZ = 0.6f * bump(win(t, .5f, .65f)); // stim jolt
                }));
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat armor = Mat.of(0x56653a).bevel(.3f).noise(.1f), armorD = Mat.of(0x434f2d).bevel(.25f).noise(.1f);
        Mat suit = Mat.of(0x2c2d2b).noise(.12f), metal = Mat.of(0x6e716b).noise(.07f), dark = Mat.of(0x1c1d1c);
        Mat visor = Mat.of(VISOR).glow().decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            g.rectGlow(x + 1, y, Math.max(1, w / 3), 1, 0xfff0b0); // specular streak
        });
        Mat chest = Mat.of(0x56653a).bevel(.32f).noise(.08f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            g.rect(x + w / 2, y, 1, h, 0x3a4426);          // sternum seam
            g.rect(x + 1, y + h - 1, w - 2, 1, 0x3a4426);    // lower edge
            g.rect(x + 1, y + 1, 2, 1, 0x6f7f4c); g.rect(x + w - 3, y + 1, 2, 1, 0x6f7f4c);
        });
        Mat helm = Mat.of(0x56653a).bevel(.34f).noise(.08f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) { g.rect(x + 1, y + h / 2, w - 2, 1, 0x3a4426); g.rect(x + w / 2, y + 2, 1, h - 3, 0x3a4426); }
            if (f == Mat.Face.TOP) g.rect(x + w / 2, y, 1, h, 0x6f7f4c);
        });
        // torso: undersuit, segmented Mark V chest, belt, power pack
        rig.box(rig.torso, suit, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4.5f, -12.3f, -3, 9, 6, 2, .2f);
        rig.box(rig.torso, armorD, -3, -6.2f, -2.7f, 6, 3, 1);
        rig.box(rig.torso, metal, -1, -6.4f, -3.2f, 2, 1, 1);
        rig.box(rig.torso, dark, -4.5f, -3, -2.5f, 9, 2, 5);
        rig.box(rig.torso, metal, -3.5f, -2.8f, -2.9f, 2, 2, 1);
        rig.box(rig.torso, metal, 1.5f, -2.8f, -2.9f, 2, 2, 1);
        rig.box(rig.torso, armorD, -4, -13, -2.5f, 8, 1, 5);
        rig.box(rig.torso, armor, -3.5f, -11.5f, 2, 7, 8, 2, .15f);
        rig.box(rig.torso, metal, -3, -10, 4, 2, 6, 1);
        rig.box(rig.torso, metal, 1, -10, 4, 2, 6, 1);
        rig.box(rig.torso, dark, -1, -9, 4.2f, 2, 4, 1);
        // Mark V helmet: rounded dome, wide gold visor, chin guard, side breathers, antenna
        rig.box(rig.head, helm, -4, -8, -4, 8, 8, 8, .5f);
        rig.box(rig.head, visor, -3.5f, -6, -5, 7, 3, 1);
        rig.box(rig.head, Mat.of(0xa0701c).glow(), -2.5f, -3, -4.9f, 5, 1, 1);
        rig.box(rig.head, armorD, -2, -2, -4.8f, 4, 2, 1);
        rig.box(rig.head, armorD, -1, -9, -3.5f, 2, 1, 7);
        rig.box(rig.head, metal, -5, -4.5f, -1.5f, 1, 3, 3);
        rig.box(rig.head, metal, 4, -4.5f, -1.5f, 1, 3, 3);
        rig.box(rig.head, dark, 3.2f, -11, 2, 1, 4, 1);
        // arms: big rounded Mark V pauldrons, bracers
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, suit, -2, -2, -2, 4, 12, 4);
            rig.box(arm, armor, s < 0 ? -3.5f : -2.5f, -3, -3, 6, 4, 6, .25f);
            rig.box(arm, armorD, s < 0 ? -3.2f : 1.2f, 1, -2, 2, 2, 4);
            rig.box(arm, dark, -2, 4, -2, 4, 1, 4, .2f);
            rig.box(arm, armor, -2, 5, -2, 4, 5, 4, .35f);
        }
        // legs: thigh plates, knee caps, armoured boots
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, suit, -2, 0, -2, 4, 12, 4);
            rig.box(leg, armor, -2, 0.5f, -2, 4, 4, 4, .3f);
            rig.box(leg, metal, -1.5f, 5, -2.9f, 3, 2, 1);
            rig.box(leg, armor, -2, 8, -2, 4, 4, 4, .35f);
            rig.box(leg, dark, -2, 11, -3, 4, 1, 1, .2f);
        }
        buildWeapons(rig, metal, dark);
    }

    private static void buildWeapons(Rig rig, Mat metal, Mat dark) {
        Mat gun = Mat.of(0x3b4036).noise(.08f), steel = Mat.of(0x8d8f8a).noise(.06f), olive = Mat.of(0x4a5530).noise(.1f);
        Mat cyan = Mat.of(0x50c8ff).glow(), red = Mat.of(0xff4030).glow();
        Mat cov = Mat.of(0x55408a).bevel(.4f).noise(.05f), covD = Mat.of(0x352a5a).bevel(.3f), covPink = Mat.of(0x7a2a70).bevel(.4f).noise(.05f);
        Mat ppG = Mat.of(PP_GREEN).glow(), prB = Mat.of(PR_BLUE).glow(), heat = Mat.of(0xff8030).glow(), ndl = Mat.of(NEEDLE).glow(), blade = Mat.of(SWORD_C).glow();
        PartDefinition w;
        // MA5B: bullpup, ammo counter, under-barrel light
        w = rig.weapon(rig.rHand, "ar");
        rig.box(w, gun, -1.5f, -3, -8, 3, 4, 11); rig.box(w, gun, -1, -2.5f, -14, 2, 2, 6); rig.box(w, dark, -0.5f, -2, -15, 1, 1, 1);
        rig.box(w, dark, -1, 0, -1, 2, 3, 2); rig.box(w, dark, -1, 1, 1, 2, 3, 2); rig.box(w, cyan, -0.5f, -3.8f, -6, 1, 1, 2);
        rig.box(w, metal, -0.5f, -1, -12, 1, 1, 3); rig.box(w, gun, -1.5f, -2.5f, 3, 3, 3, 3);
        // M6D: silver slab with smart-link scope
        w = rig.weapon(rig.rHand, "pistol");
        rig.box(w, steel, -1, -2.5f, -7, 2, 2, 7); rig.box(w, dark, -1, -0.5f, -6, 2, 1, 5); rig.box(w, dark, -1, 0, -1, 2, 4, 2);
        rig.box(w, dark, -0.5f, -3.5f, -4, 1, 1, 3); rig.box(w, red, -0.5f, -3.5f, -4.6f, 1, 1, 1);
        // M90: pump shotgun with tube magazine
        w = rig.weapon(rig.rHand, "shotgun");
        rig.box(w, gun, -1.5f, -3, -6, 3, 3, 8); rig.box(w, metal, -0.5f, -3, -16, 1, 1, 10); rig.box(w, metal, -0.5f, -2, -15, 1, 1, 9);
        rig.box(w, dark, -1, -2.5f, -12, 2, 2, 4); rig.box(w, dark, -1, 0, -1, 2, 3, 2); rig.box(w, gun, -1, -2.5f, 2, 2, 3, 5);
        // S2 AM: long barrel, big scope, magazine
        w = rig.weapon(rig.rHand, "sniper");
        rig.box(w, gun, -1.5f, -3, -7, 3, 3, 10); rig.box(w, metal, -0.5f, -2.5f, -22, 1, 1, 15); rig.box(w, dark, -1, -2.8f, -23, 2, 2, 1);
        rig.box(w, dark, -1, -5, -6, 2, 2, 7); rig.box(w, cyan, -0.5f, -4.5f, -6.6f, 1, 1, 1);
        rig.box(w, dark, -1, 0, -4, 2, 3, 2); rig.box(w, dark, -1, 0, -1, 2, 3, 2); rig.box(w, gun, -1, -2.5f, 3, 2, 3, 5);
        // M19 SSM: twin-tube shoulder launcher
        w = rig.weapon(rig.rHand, "rocket");
        rig.box(w, olive, -3, -5, -12, 3, 3, 18); rig.box(w, olive, 0, -5, -12, 3, 3, 18);
        rig.box(w, dark, -3, -5, -12.6f, 6, 3, 1); rig.box(w, dark, -0.5f, 0, -2, 1, 3, 2); rig.box(w, metal, -1, -2, -4, 2, 2, 5);
        rig.box(w, dark, -0.5f, -6.5f, -4, 1, 1, 3); rig.box(w, red, -0.5f, -6.5f, -4.6f, 1, 1, 1);
        // Covenant plasma pistol (normal + overcharged emitter)
        for (String n : new String[] {"ppistol", "ppistol_oc"}) {
            w = rig.weapon(rig.rHand, n);
            rig.box(w, cov, -1.5f, -3, -6, 3, 3, 7); rig.box(w, covD, -0.5f, -4, -5, 1, 1, 5); rig.box(w, covD, -1, 0, -2, 2, 2, 2);
            if (n.equals("ppistol")) rig.box(w, ppG, -1, -2.5f, -7, 2, 2, 1);
            else { rig.box(w, ppG, -1.5f, -3, -9, 3, 3, 3, .3f); rig.box(w, ppG, -0.5f, -4.2f, -4, 1, 1, 2); }
        }
        // Covenant plasma rifle (normal + venting)
        for (String n : new String[] {"prifle", "prifle_vent"}) {
            w = rig.weapon(rig.rHand, n);
            rig.box(w, cov, -1.5f, -3, -6, 3, 3, 9); rig.box(w, covD, -2.5f, -3, -11, 1, 2, 6); rig.box(w, covD, 1.5f, -3, -11, 1, 2, 6);
            rig.box(w, prB, -0.5f, -2.5f, -9, 1, 1, 2); rig.box(w, covD, -1, 0, -1, 2, 2, 2);
            if (n.equals("prifle_vent")) { rig.box(w, heat, -2.6f, -3.5f, -5, 1, 2, 4); rig.box(w, heat, 1.6f, -3.5f, -5, 1, 2, 4); }
        }
        // Needler: pink crystal spines
        w = rig.weapon(rig.rHand, "needler");
        rig.box(w, covPink, -1.5f, -3, -8, 3, 3, 9); rig.box(w, covD, -1, -2.5f, -9, 2, 2, 1); rig.box(w, covD, -1, 0, -1, 2, 2, 2);
        rig.box(w, ndl, -1, -5, -6, 1, 2, 1); rig.box(w, ndl, 0, -5.5f, -4, 1, 3, 1); rig.box(w, ndl, -1, -5, -2, 1, 2, 1);
        // Energy sword: hilt (unlit) + forked plasma blades
        for (String n : new String[] {"sword", "sword_off"}) {
            w = rig.weapon(rig.rHand, n);
            rig.box(w, covD, -1, -1, -3, 2, 2, 4); rig.box(w, cov, -1.5f, -1.5f, -4, 3, 3, 1);
            if (n.equals("sword")) { rig.box(w, blade, -1.5f, -1, -14, 1, 1, 10); rig.box(w, blade, 0.5f, -1, -14, 1, 1, 10); }
        }
        // left hand: frag, plasma grenade, health pack
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, olive, -1, -1, -2, 2, 2, 2, .2f); rig.box(w, metal, -0.5f, -1.8f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "pnade");
        rig.box(w, Mat.of(0x60c0ff).glow(), -1, -1.2f, -2.2f, 2, 2, 2, .3f); rig.box(w, covD, -1.5f, -0.5f, -1.5f, 3, 1, 1);
        w = rig.weapon(rig.lHand, "medkit");
        rig.box(w, Mat.of(0xe8e8e2).bevel(.3f), -1.5f, -1.5f, -2.5f, 3, 2, 3); rig.box(w, Mat.of(0xe08020), -1.5f, -1.7f, -1.5f, 3, 1, 1);
    }
}
