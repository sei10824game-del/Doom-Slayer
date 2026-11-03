package com.doomslayer.kit.kits;

import static com.doomslayer.anim.Keys.*;
import static com.doomslayer.kit.kits.Doom2016Slayer.L;
import static com.doomslayer.kit.kits.Doom2016Slayer.hold;
import static com.doomslayer.kit.kits.Doom2016Slayer.mid;
import static com.doomslayer.kit.kits.Doom2016Slayer.ready;
import static com.doomslayer.kit.kits.Doom2016Slayer.snd;
import static com.doomslayer.kit.kits.Doom2016Slayer.stance;
import static com.doomslayer.kit.kits.Doom2016Slayer.wide;

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
import net.minecraft.server.level.ServerLevel;
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

/** Noble Six (SPARTAN-B312), Halo: Reach: Mark V[B] MJOLNIR, full Reach sandbox, armor abilities, shields, assassination. */
public class HaloReachNobleSix extends SlayerKit {
    static final int VISOR = 0xf0a028, SHIELD = 0xf2d040, PLASMA_C = 0x40a0ff, NEEDLE = 0xff50d0, FOCUS = 0xffa040, SWORD = 0x70c8ff;

    static final ProjStyle SPNKR = ProjStyle.make().color(0xffa040).size(0.45f).life(100).explode(3.5f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle GL_ROUND = ProjStyle.make().color(0x5a6a40).size(0.28f).life(40).explode(3f).gravity(0.045).bounce().trail(ParticleTypes.SMOKE)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 2.5f); Fx.ring(l, pos, 2.5f, 0x80c0ff, 1.3f, 26); // EMP pulse
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 25, 1.2, 0.3);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1f, 1.2f);
                if (p.getOwner() instanceof SlayerEntity o)
                    for (LivingEntity x : Combat.around(o, pos, 3.5)) x.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3));
            });
    static final ProjStyle NEEDLE_ROUND = ProjStyle.make().color(NEEDLE).size(0.14f).life(40).homing(0.06f).trail(ParticleTypes.END_ROD)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, NEEDLE, 0.8f, pos, 5, 0.1);
                if (!(hit instanceof LivingEntity le) || !(p.getOwner() instanceof SlayerEntity o)) return;
                int c = le.getPersistentData().getInt("ns_needles") + 1;
                if (c >= 3) { // supercombine
                    le.getPersistentData().putInt("ns_needles", 0);
                    Vec3 m = le.position().add(0, le.getBbHeight() * 0.5, 0);
                    Combat.blast(o, m, 2.8f, 12f, 0.9f, null);
                    Fx.dustBurst(l, NEEDLE, 2f, m, 40, 0.9); Fx.ring(l, m, 2f, 0xff80e0, 1.4f, 24);
                    Fx.sound(l, m, SoundEvents.AMETHYST_BLOCK_BREAK, 1.5f, 0.6f); Fx.sound(l, m, SoundEvents.GENERIC_EXPLODE, 0.7f, 1.6f);
                } else le.getPersistentData().putInt("ns_needles", c);
            });
    static final ProjStyle PLASMA_SHOT = ProjStyle.make().color(PLASMA_C).size(0.25f).life(45).trail(ParticleTypes.ELECTRIC_SPARK);
    static final ProjStyle FRAG = ProjStyle.make().color(0x4a5a3a).size(0.26f).life(36).explode(3.5f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle PLASMA_NADE = ProjStyle.make().color(PLASMA_C).size(0.35f).life(45).explode(3f).gravity(0.035).homing(0.12f).trail(ParticleTypes.ELECTRIC_SPARK)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 2.5f); Fx.dustBurst(l, PLASMA_C, 2f, pos, 40, 1.2); Fx.ring(l, pos, 2.8f, 0x80d0ff, 1.5f, 30);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1.2f, 1.4f); Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1f, 1.6f);
            });

    public HaloReachNobleSix() {
        super("halo_reach_noble_six", "Noble Six", "ノーブル・シックス", "Halo: Reach");
        eggPrimary = 0x5c666b; eggSecondary = VISOR;
        maxHealth = 90; speed = 0.33; armor = 14; knockbackRes = 0.5;
        scale = 1.1f; width = 0.7f; preferredRange = 11f; dmgMult = 1.05f;
        idleWeaponR = "ma37";
    }

    // ================= helpers (server) =================
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.25, 0.3, 0); }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        return Combat.dirTo(mz(e), t, spread, new Random(e.getRandom().nextLong()));
    }
    static void flash(SlayerEntity e, int rgb) {
        Vec3 m = mz(e);
        Fx.burst(L(e), ParticleTypes.FLAME, m, 2, 0.04, 0.02); Fx.dustBurst(L(e), rgb, 1.1f, m, 3, 0.07);
    }
    static void casing(SlayerEntity e) { Fx.burst(L(e), ParticleTypes.SMOKE, Combat.muzzle(e, 0.45, 0.55, 0.1), 1, 0.03, 0.03); }
    static void bullet(SlayerEntity e, LivingEntity t, double spread, float dmg, int pierce, int rgb, double range) {
        Combat.hitscan(e, mz(e), aim(e, t, spread), range, dmg, pierce, rgb); flash(e, 0xffc060); casing(e);
    }
    static void effect(LivingEntity x, net.minecraft.world.effect.MobEffect m, int ticks, int amp) { x.addEffect(new MobEffectInstance(m, ticks, amp, false, false)); }
    /** remove hostile projectiles near c (shield / armor lock emulation) */
    static void eatProjectiles(SlayerEntity e, Vec3 c, double r) {
        for (Projectile pr : e.level().getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(r))) {
            Entity o = pr.getOwner();
            if (pr instanceof SlayerProjectile || o instanceof Player || o instanceof SlayerEntity) continue;
            Fx.sparks(L(e), pr.position(), 6); pr.discard();
        }
    }
    static void thrust(SlayerEntity e) {
        Vec3 b = Combat.muzzle(e, -0.55, 0, -0.5);
        Fx.burst(L(e), ParticleTypes.FLAME, b, 4, 0.08, 0.02); Fx.burst(L(e), ParticleTypes.SMOKE, b.add(0, -0.4, 0), 2, 0.1, 0.02);
    }

    // ================= helpers (anim) =================
    /** Spartan shoulder carry: rifle shouldered, head down on the sights, slight forward lean. */
    static void shoulder(Pose p, float k, float pitch, float recoil) {
        float tw = 0.2f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.1f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.08f - recoil * 0.25f, k); p.lArmY = lerp(p.lArmY, 0.5f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * k; p.headX += 0.08f * k;
        p.bodyX += (0.08f + recoil * 0.12f - pitch * 0.2f) * k; p.headX -= recoil * 0.08f * k;
        p.rootZ += recoil * 0.8f * k;
    }
    /** heavy launcher on the right shoulder (SPNKr / GL) */
    static void launcher(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.2f, k); p.rArmY = lerp(p.rArmY, -0.05f, k); p.rArmZ = lerp(p.rArmZ, 0.15f, k);
        p.lArmX = lerp(p.lArmX, -1.3f + pitch - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.55f, k); p.lArmZ = 0;
        p.bodyY += 0.3f * k; p.headY -= 0.25f * k; p.headZ -= 0.12f * k;
        p.bodyX += (-pitch * 0.25f - recoil * 0.15f) * k; p.rootZ += recoil * 1.2f * k;
    }
    /** bladed left-hand throw (grenades / emitters), right hand keeps rifle low */
    static void throwL(Pose p, float t, float hold, float rel) {
        p.rArmX = track(t, 0, -0.6f, rel, -0.5f, 1, -0.6f); p.rArmY = -0.3f; p.rHandX = -0.3f;
        p.lArmX = track(t, 0, 0, hold * 0.4f, -0.9f, hold, 0.9f, rel, -2.5f, rel + .2f, -1.3f, 1, 0);
        p.lArmZ = track(t, 0, 0, hold, -0.55f, rel, -0.1f, 1, 0);
        p.bodyY = track(t, 0, 0, hold, -0.6f, rel + .03f, 0.5f, rel + .25f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.75f;
        p.bodyX = track(t, 0, 0, hold, -0.22f, rel + .03f, 0.32f, 1, 0);
        p.lLegX = track(t, 0, 0, hold * 0.7f, -0.4f, rel, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, hold, 0.35f, rel, 0.5f, 1, 0);
        p.rootZ = track(t, hold * 0.9f, 0, rel + .05f, -1.5f, 1, 0); p.rootY = -0.6f * bump(win(t, hold, 1));
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // MA37 at low ready, muzzle down, both hands
        float br = 0.03f * (float) Math.sin(age * 0.08f);
        p.rArmX = -0.6f + p.rArmX * 0.25f + br; p.rArmY = -0.3f;
        p.lArmX = -0.85f + p.lArmX * 0.25f + br; p.lArmY = 0.6f; p.lArmZ = 0.05f;
        p.rHandX = -0.4f; p.bodyX += 0.05f; p.headX += 0.04f + br * 0.5f;
    }

    /** SHIELDS: absorption layer that recharges after ~5 s without taking damage (Reach style, with the recharge chirp). */
    @Override
    public void passive(SlayerEntity e) {
        int since = e.tickCount - e.getLastHurtByMobTimestamp();
        float sh = e.getAbsorptionAmount();
        if (e.hurtTime > 0 || since < 100 || sh >= 12 || e.tickCount % 4 != 0) return;
        if (sh == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.8f);
        e.setAbsorptionAmount(Math.min(12, sh + 1));
        Fx.dustBurst(L(e), SHIELD, 0.7f, e.position().add(0, 1.1, 0), 4, 0.45);
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- Rifle melee: MA37 butt-stroke ----------
        out.add(AttackDef.of("rifle_melee").range(0, 3.2f).cd(22).weight(3).dur(16).weapon("ma37")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.3f);
                    if (n == 6) {
                        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.3f, 60, -40, -20, 0xd0d0d0, 0.7f, 8);
                        for (LivingEntity f : Combat.cone(e, 3.3, 50)) {
                            Combat.damage(e, f, 9); Combat.knock(f, e.position(), 1.0, 0.3); Fx.sparks(L(e), mid(f), 8);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.7f); snd(e, SoundEvents.ANVIL_LAND, 0.25f, 1.7f);
                    }
                })
                .anim((p, t, a) -> {
                    // pull rifle back across chest, then drive the stock forward-up, recover
                    p.rArmX = track(t, 0, -0.6f, .25f, -0.3f, .4f, -1.7f, .6f, -1.6f, 1, -0.6f);
                    p.rArmY = track(t, 0, -0.3f, .25f, 0.2f, .4f, -0.4f, 1, -0.3f);
                    p.lArmX = track(t, 0, -0.85f, .25f, -0.5f, .4f, -1.9f, .6f, -1.8f, 1, -0.85f); p.lArmY = track(t, 0, 0.6f, .4f, 0.3f, 1, 0.6f);
                    p.rHandX = track(t, 0, -0.4f, .25f, 0.6f, .4f, -1.2f, .6f, -1.0f, 1, -0.4f);
                    p.rHandZ = track(t, .25f, 0, .4f, 0.4f, 1, 0);
                    p.bodyY = trackLin(t, 0, 0, .25f, 0.45f, .4f, -0.5f, .65f, -0.35f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyX = track(t, 0, 0.05f, .25f, -0.1f, .42f, 0.35f, .7f, 0.25f, 1, 0.05f);
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, .75f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .4f, 0.4f, .75f, 0.35f, 1, 0);
                    p.rootZ = -2f * bump(win(t, .25f, .85f)); p.rootY = -0.5f * bump(win(t, .3f, .9f));
                }));

        // ---------- Assassination: knife in the back, wrestle down ----------
        out.add(AttackDef.of("assassination").range(0, 3.5f).lowHealthTarget(0.3f).weight(6).cd(80).dur(34).weapon("knife")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.45, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.8f); }
                    hold(e, t, 0.85, 0);
                    if (n == 5) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 1f, 1.4f);
                    if (n == 14 && t != null) { snd(e, SoundEvents.TRIDENT_HIT, 1.2f, 0.9f); Fx.sparks(L(e), mid(t), 10); Fx.dustBurst(L(e), 0x502010, 1.2f, mid(t), 12, 0.3); }
                    if (n == 22 && t != null) {
                        Combat.damage(e, t, t.getHealth() + 100);
                        snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 0.6f); snd(e, SoundEvents.ARMOR_EQUIP_NETHERITE, 1f, 0.7f);
                        Fx.burst(L(e), ParticleTypes.POOF, t.position(), 10, 0.4, 0.05);
                    }
                })
                .anim((p, t, a) -> {
                    // grab with left arm around the neck, overhead knife plunge, push them down, rise
                    p.lArmX = track(t, 0, 0, .12f, -1.7f, .7f, -1.4f, .8f, -0.4f, 1, 0); p.lArmY = track(t, 0, 0, .12f, -0.4f, .7f, -0.3f, 1, 0);
                    p.rArmX = track(t, 0, 0, .15f, -0.5f, .35f, -3.0f, .43f, -1.2f, .62f, -0.9f, .75f, -1.4f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .35f, 0.3f, .43f, -0.15f, 1, 0);
                    p.rHandX = track(t, 0, 0, .3f, 1.4f, .7f, 1.4f, .85f, 0.2f, 1, 0);
                    p.bodyX = track(t, 0, 0, .35f, -0.2f, .43f, 0.45f, .65f, 0.7f, .8f, 0.3f, 1, 0);
                    p.bodyY = track(t, 0, 0, .12f, 0.3f, .43f, -0.15f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.headX = track(t, .4f, 0, .5f, 0.4f, .8f, 0.4f, 1, 0);
                    p.rootY = track(t, .4f, 0, .5f, -1.2f, .7f, -2.0f, .85f, -1.0f, 1, 0); p.rootZ = track(t, 0, 0, .12f, -1.8f, .85f, -1.8f, 1, 0);
                    p.lLegX = track(t, 0, 0, .12f, -0.5f, .6f, -1.1f, .85f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .43f, 0.4f, .65f, 0.9f, .85f, 0.3f, 1, 0);
                }));

        // ---------- MA37 Assault Rifle (sustained auto) ----------
        out.add(AttackDef.of("ma37").range(2, 20).cd(26).weight(4).dur(28).weapon("ma37")
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 22 && n % 2 == 0) {
                        bullet(e, t, 0.045, 2.1f, 0, 0xffe090, 22);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.7f + e.getRandom().nextFloat() * 0.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .13f, .15f) * (1 - win(t, .8f, .82f));
                    float r = 0.4f * (0.5f + 0.5f * (float) Math.sin(a * PI)) * fire;
                    shoulder(p, k, 0, r); stance(p, k);
                    p.rHandZ += 0.04f * (float) Math.sin(a * 2.7f) * fire; p.bodyZ += 0.02f * (float) Math.sin(a * 1.9f) * fire;
                    p.headX += 0.05f * fire; // tucked on the sights
                }));

        // ---------- M392 DMR (paced precision shots) ----------
        out.add(AttackDef.of("dmr").range(6, 30).cd(34).weight(4).dur(30).weapon("dmr")
                .tick((e, t, n) -> {
                    if (n == 7 || n == 14 || n == 21) {
                        bullet(e, t, 0.006, 6.5f, 0, 0xfff0b0, 32);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.9f, 1.5f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .88f), zoom = win(t, .1f, .22f) * (1 - win(t, .78f, .88f));
                    float r = 1.1f * (kick(t, 7 / 30f, .12f) + kick(t, 14 / 30f, .12f) + kick(t, 21 / 30f, .12f));
                    shoulder(p, k, 0, r); stance(p, k);
                    p.headX += 0.15f * zoom; p.bodyX += 0.08f * zoom; p.rootY -= 0.3f * zoom; p.rHandX -= r * 0.3f;
                }));

        // ---------- M6G Magnum (one-handed, heavy kick) ----------
        out.add(AttackDef.of("m6g_magnum").range(2, 22).cd(30).weight(3).dur(26).weapon("m6g")
                .tick((e, t, n) -> {
                    if (n == 6 || n == 15) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.01), 24, 7f, 0, 0xffe0a0); flash(e, 0xffb050); casing(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1f, 1.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f), r = 1.4f * (kick(t, 6 / 26f, .2f) + kick(t, 15 / 26f, .2f));
                    p.bodyY -= 0.4f * k; p.headY += 0.38f * k; // side-on stance
                    p.rArmX = lerp(p.rArmX, -HALF - r * 0.45f, k); p.rArmY = lerp(p.rArmY, 0.38f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
                    p.lArmX = lerp(p.lArmX, -0.35f, k); p.lArmZ = lerp(p.lArmZ, -0.25f, k); p.lArmY = lerp(p.lArmY, 0.2f, k);
                    p.rHandX -= r * 0.45f; p.bodyX -= r * 0.08f; p.headX -= r * 0.05f; p.rootZ += r * 0.5f;
                    p.lLegX = lerp(p.lLegX, 0.3f, k); p.rLegX = lerp(p.rLegX, -0.25f, k); p.rootY -= 0.3f * k;
                }));

        // ---------- M45 Shotgun (pump) ----------
        out.add(AttackDef.of("m45_shotgun").range(0, 9).cd(30).weight(3).dur(24).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 5) {
                        Vec3 m = mz(e);
                        for (int i = 0; i < 12; i++) Combat.hitscan(e, m, aim(e, t, 0.09), 14, 2.4f, 0, 0xffd070);
                        for (LivingEntity f : Combat.cone(e, 3.5, 40)) Combat.knock(f, e.position(), 0.9, 0.3);
                        flash(e, 0xffa040); flash(e, 0xff7020);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.7f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 0.8f);
                    }
                    if (n == 12) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.7f);
                    if (n == 16) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.8f); casing(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 1.8f * kick(t, 5 / 24f, .28f), pump = bump(win(t, .48f, .72f));
                    shoulder(p, k, 0.1f * r, r); stance(p, k);
                    p.lArmX += 0.4f * pump * k; p.lArmY -= 0.12f * pump; p.rHandX -= r * 0.45f + 0.1f * pump;
                    p.bodyZ += 0.06f * pump; p.headX += 0.06f * pump; p.rootY += 0.3f * r;
                }));

        // ---------- SRS99 Sniper Rifle (scope, two piercing rounds) ----------
        out.add(AttackDef.of("srs99_sniper").range(12, 48).cd(70).weight(3).dur(40).weapon("sniper")
                .tick((e, t, n) -> {
                    if (n == 6) snd(e, SoundEvents.SPYGLASS_USE, 1, 0.9f);
                    if (n == 16 || n == 30) {
                        Vec3 m = mz(e), d = aim(e, t, 0);
                        Combat.hitscan(e, m, d, 50, 18f, 2, 0xffffff); flash(e, 0xffd080);
                        Fx.burst(L(e), ParticleTypes.CLOUD, m, 4, 0.1, 0.02);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.6f, 0.5f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 2f);
                    }
                    if (n == 20) { snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.6f, 1.6f); casing(e); }
                    if (n == 24) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.6f, 1.7f);
                    if (n == 34) snd(e, SoundEvents.SPYGLASS_STOP_USING, 1, 0.9f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .9f), zoom = win(t, .1f, .3f) * (1 - win(t, .82f, .92f));
                    float r = 2.2f * (kick(t, 16 / 40f, .2f) + kick(t, 30 / 40f, .2f)), bolt = bump(win(t, .48f, .62f));
                    shoulder(p, k, 0, r); wide(p, zoom * 0.6f);
                    p.headX += 0.22f * zoom; p.headY -= 0.1f * zoom; p.bodyX += 0.15f * zoom; p.rootY -= 0.5f * zoom;
                    p.rHandX -= r * 0.35f; p.rArmX += 0.5f * bolt; p.rArmZ += 0.2f * bolt; p.rHandY += 0.4f * bolt; // working the bolt
                    p.headY += 0.03f * (float) Math.sin(a * 0.4f) * zoom;
                }));

        // ---------- M41 SPNKr Rocket Launcher (twin tubes, two rockets) ----------
        out.add(AttackDef.of("spnkr_rocket").range(7, 34).cd(80).weight(2).dur(34).weapon("spnkr")
                .tick((e, t, n) -> {
                    if (n == 8 || n == 18) {
                        Combat.shoot(e, SPNKR, Combat.muzzle(e, 1.3, 0.3, n == 8 ? 0.12 : -0.05), aim(e, t, 0.015), 1.2, 15f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.7, 0.3, 0.1), 6, 0.2, 0.05); flash(e, 0xffa040);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.55f); snd(e, SoundEvents.GHAST_SHOOT, 0.4f, 1.3f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 2.2f * (kick(t, 8 / 34f, .25f) + kick(t, 18 / 34f, .25f));
                    launcher(p, k, -0.04f, r); stance(p, k);
                    p.rHandX -= r * 0.35f; p.headX -= r * 0.08f; p.rootY += r * 0.3f;
                    p.lArmX += 0.3f * bump(win(t, .7f, .9f)); // pats the tube
                }));

        // ---------- M319 Grenade Launcher (bouncing EMP round) ----------
        out.add(AttackDef.of("m319_grenade_launcher").range(5, 24).cd(60).weight(2).dur(30).weapon("gl")
                .tick((e, t, n) -> {
                    if (n == 8) {
                        double dist = t != null ? e.distanceTo(t) : 10;
                        Vec3 d = aim(e, t, 0.02).add(0, 0.12 + dist * 0.02, 0).normalize();
                        Combat.shoot(e, GL_ROUND, mz(e), d, 0.85 + dist * 0.02, 12f);
                        Fx.burst(L(e), ParticleTypes.POOF, mz(e), 6, 0.1, 0.03);
                        snd(e, SoundEvents.FIRECHARGE_USE, 1.2f, 0.7f); snd(e, SoundEvents.CROSSBOW_SHOOT, 1f, 0.6f);
                    }
                    if (n == 16) snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.7f, 1.3f);
                    if (n == 22) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.8f, 1.2f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 1.6f * kick(t, 8 / 30f, .25f), brk = bump(win(t, .5f, .78f));
                    launcher(p, k, -0.12f * k + 0.5f * brk, r); stance(p, k);
                    p.rHandX += 0.8f * brk - r * 0.3f; p.headX += 0.2f * brk; // break-open single shot reload
                    p.lArmX += 0.8f * bump(win(t, .55f, .72f)); p.lArmZ -= 0.25f * bump(win(t, .55f, .72f));
                }));

        // ---------- Needle Rifle (precision needles -> supercombine) ----------
        out.add(AttackDef.of("needle_rifle").range(5, 28).cd(34).weight(3).dur(30).weapon("needle")
                .tick((e, t, n) -> {
                    if (n == 6 || n == 11 || n == 16 || n == 21) {
                        Combat.shoot(e, NEEDLE_ROUND, mz(e), aim(e, t, 0.008), 1.9, 3.5f);
                        Fx.dustBurst(L(e), NEEDLE, 0.9f, mz(e), 4, 0.06);
                        snd(e, SoundEvents.AMETHYST_BLOCK_HIT, 1.2f, 1.6f); snd(e, SoundEvents.BLAZE_SHOOT, 0.3f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .88f);
                    float r = 0.8f * (kick(t, 6 / 30f, .1f) + kick(t, 11 / 30f, .1f) + kick(t, 16 / 30f, .1f) + kick(t, 21 / 30f, .1f));
                    shoulder(p, k, 0, r); stance(p, k);
                    p.headX += 0.12f * k; p.rHandZ += 0.05f * r;
                }));

        // ---------- Plasma Repeater (spray, then heat vent) ----------
        out.add(AttackDef.of("plasma_repeater").range(3, 20).cd(40).weight(3).dur(36).weapon("repeater")
                .tick((e, t, n) -> {
                    if (n >= 3 && n <= 22) {
                        if (n % 2 == 1 || n < 10) {
                            Combat.shoot(e, PLASMA_SHOT, mz(e), aim(e, t, 0.02 + n * 0.002), 1.6, 1.8f);
                            Fx.dustBurst(L(e), PLASMA_C, 0.8f, mz(e), 2, 0.05);
                            snd(e, SoundEvents.BLAZE_SHOOT, 0.4f, 1.7f + n * 0.01f);
                        }
                    }
                    if (n == 24) snd(e, SoundEvents.FIRE_EXTINGUISH, 1f, 1.2f);
                    if (n > 24 && n < 32) Fx.burst(L(e), ParticleTypes.CLOUD, Combat.muzzle(e, 0.7, 0.45, 0.2), 2, 0.08, 0.04);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .92f), fire = win(t, .08f, .1f) * (1 - win(t, .62f, .64f)), vent = bump(win(t, .64f, .92f));
                    float r = 0.35f * (0.5f + 0.5f * (float) Math.sin(a * 2.2f)) * fire;
                    // held at the waist like a Covenant weapon
                    p.rArmX = lerp(p.rArmX, -1.25f - r * 0.3f + 0.5f * vent, k); p.rArmY = lerp(p.rArmY, -0.15f, k);
                    p.lArmX = lerp(p.lArmX, -1.35f - r * 0.2f + 0.6f * vent, k); p.lArmY = lerp(p.lArmY, 0.6f, k);
                    p.rHandX -= 0.3f * k + r * 0.3f; p.rHandY += 0.7f * vent; // tilts to vent
                    p.bodyX += 0.12f * k + 0.1f * fire; p.bodyY += 0.12f * k; p.headY -= 0.12f * k + 0.3f * vent; p.headX += 0.15f * vent;
                    p.bodyZ += 0.04f * (float) Math.sin(a * 3.1f) * fire; p.rootZ += r * 0.5f;
                    stance(p, k);
                }));

        // ---------- Focus Rifle (continuous beam) ----------
        out.add(AttackDef.of("focus_rifle").range(8, 36).cd(80).weight(2).dur(38).weapon("focus")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.BEACON_ACTIVATE, 0.9f, 1.6f);
                    if (n >= 8 && n <= 30) {
                        Vec3 m = mz(e), d = aim(e, t, 0.002);
                        if (n % 2 == 0) Combat.hitscan(e, m, d, 38, 2.2f, 0, FOCUS);
                        else Fx.beam(L(e), m, t != null ? mid(t) : m.add(d.scale(38)), 0xffd080, 0.6f, 0.5);
                        if (t != null) { Fx.burst(L(e), ParticleTypes.FLAME, mid(t), 1, 0.1, 0.01); if (n % 6 == 0) t.setSecondsOnFire(1); }
                        if (n % 5 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 1f, 1.8f);
                    }
                    if (n == 31) { snd(e, SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.6f); Fx.burst(L(e), ParticleTypes.CLOUD, mz(e), 6, 0.1, 0.03); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), beam = win(t, .2f, .22f) * (1 - win(t, .8f, .82f)), zoom = win(t, .05f, .2f) * (1 - win(t, .85f, .95f));
                    shoulder(p, k, 0, 0.15f * beam); wide(p, 0.5f * k);
                    p.headX += 0.2f * zoom; p.bodyX += 0.12f * zoom; p.rootY -= 0.4f * zoom;
                    float tr = 0.03f * (float) Math.sin(a * 0.7f) * beam; // sweep while tracking
                    p.bodyY += tr; p.rArmY += tr; p.lArmY += tr;
                    p.rHandZ += 0.02f * (float) Math.sin(a * 6f) * beam;
                }));

        // ---------- Energy Sword (lunge slash, picked up from an Elite) ----------
        out.add(AttackDef.of("energy_sword").range(0, 6).cd(90).weight(3).dur(22).weapon("sword").noLos()
                .tick((e, t, n) -> {
                    if (n == 1) snd(e, SoundEvents.BEACON_ACTIVATE, 0.7f, 2f);
                    if (n == 6) { Combat.lunge(e, t != null && e.distanceTo(t) > 3 ? 1.1 : 0.5, 0.12); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f); }
                    if (n == 10) {
                        Fx.arc(L(e), e.position().add(0, 1.2, 0), e.getYRot(), 2.2f, 80, -80, 25, SWORD, 1.4f, 18);
                        Fx.arc(L(e), e.position().add(0, 1.2, 0), e.getYRot(), 1.8f, 80, -80, 25, 0xffffff, 0.7f, 14);
                        for (LivingEntity f : Combat.cone(e, 3.8, 70)) {
                            Combat.damage(e, f, 22); Combat.knock(f, e.position(), 0.8, 0.25);
                            Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(f), 14, 0.3, 0.3);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.3f, 0.6f); snd(e, SoundEvents.TRIDENT_RIPTIDE_1, 1f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    // blade back over the right shoulder, explosive lunge, flat left-to-right cut, settle
                    p.rArmX = track(t, 0, -0.5f, .25f, -2.2f, .38f, -2.3f, .48f, -1.3f, .7f, -1.1f, 1, -0.5f);
                    p.rArmY = trackLin(t, 0, 0, .3f, 0.9f, .45f, 0.9f, .55f, -0.9f, .75f, -0.8f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .3f, 0.5f, .55f, -0.2f, 1, 0);
                    p.rHandX = track(t, 0, 0, .3f, -0.4f, .55f, 0.2f, 1, 0); p.rHandZ = track(t, .4f, 0.4f, .6f, -0.4f, 1, 0);
                    p.lArmX = track(t, 0, 0, .3f, -1.2f, .5f, -0.4f, 1, 0); p.lArmZ = track(t, 0, 0, .3f, -0.2f, .5f, -0.7f, 1, 0);
                    p.bodyY = trackLin(t, 0, 0, .3f, -0.6f, .45f, -0.6f, .55f, 0.7f, .75f, 0.55f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyX = track(t, 0, 0, .25f, 0.2f, .45f, 0.35f, .6f, 0.45f, 1, 0);
                    p.lLegX = track(t, 0, 0, .25f, -0.3f, .45f, -0.9f, .75f, -0.7f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.3f, .45f, 0.8f, .75f, 0.5f, 1, 0);
                    p.rootZ = track(t, 0, 0, .25f, 0.6f, .45f, -3.2f, .7f, -2.5f, 1, 0); p.rootY = track(t, 0, 0, .25f, -0.8f, .45f, 0.6f, .6f, -1.0f, 1, 0);
                }));

        // ---------- M9 Frag Grenade ----------
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(140).weight(2).dur(20).weapons("ma37", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.4f);
                    if (n == 10) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.4);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Combat.shoot(e, FRAG, from, aim(e, t, 0.02).add(0, 0.15 + dist * 0.018, 0).normalize(), 0.75 + dist * 0.025, 14f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.8f);
                    }
                })
                .anim((p, t, a) -> { p.weaponL = t < .52f ? "frag" : null; throwL(p, t, .42f, .52f); }));

        // ---------- Type-1 Plasma Grenade (homing "stick") ----------
        out.add(AttackDef.of("plasma_grenade").range(4, 16).cd(160).weight(2).dur(22).weapons("ma37", "pnade")
                .tick((e, t, n) -> {
                    if (n >= 2 && n < 11) Fx.dustBurst(L(e), PLASMA_C, 0.7f, Combat.muzzle(e, 0.3, -0.5, 0.3), 2, 0.08);
                    if (n == 2) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 2f);
                    if (n == 11) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.4);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Combat.shoot(e, PLASMA_NADE, from, aim(e, t, 0.01).add(0, 0.1 + dist * 0.015, 0).normalize(), 0.8 + dist * 0.02, 16f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 1.1f);
                    }
                })
                .anim((p, t, a) -> { p.weaponL = t < .5f ? "pnade" : null; throwL(p, t, .4f, .5f); p.headX += 0.1f * bump(win(t, .1f, .4f)); }));

        // ---------- Sprint (close distance, end with a running rifle strike) ----------
        out.add(AttackDef.of("sprint").range(7, 18).cd(140).weight(2).dur(24).weapon("ma37").moving()
                .tick((e, t, n) -> {
                    if (t == null) return;
                    if (n < 16 && e.distanceTo(t) > 2.2) {
                        Vec3 d = t.position().subtract(e.position()); d = new Vec3(d.x, 0, d.z).normalize().scale(0.55);
                        e.setDeltaMovement(d.x, e.getDeltaMovement().y, d.z); e.hurtMarked = true;
                        if (n % 4 == 0) { snd(e, SoundEvents.NETHERITE_BLOCK_STEP, 0.8f, 0.8f); Fx.burst(L(e), ParticleTypes.POOF, e.position(), 2, 0.1, 0.01); }
                    }
                    if (n == 18 && e.distanceTo(t) < 3.5) {
                        Combat.damage(e, t, 10); Combat.knock(t, e.position(), 1.4, 0.35); Fx.sparks(L(e), mid(t), 10);
                        snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    float run = win(t, 0, .1f) * (1 - win(t, .65f, .72f)), sw = (float) Math.sin(a * 1.4f) * run;
                    // rifle pulled to the chest, deep lean, big arm pumping legs
                    p.rArmX = -0.9f + 0.3f * sw; p.lArmX = -1.1f + 0.3f * sw; p.rArmY = -0.4f; p.lArmY = 0.7f; p.rHandX = -0.7f * run;
                    p.rLegX = 1.0f * sw; p.lLegX = -1.0f * sw;
                    p.bodyX += 0.4f * run; p.headX -= 0.25f * run; p.rootY += Math.abs(sw) * 0.8f; p.bodyY += 0.08f * sw;
                    float hit = win(t, .7f, .78f) * (1 - win(t, .85f, 1));
                    p.rArmX = lerp(p.rArmX, -1.8f, hit); p.lArmX = lerp(p.lArmX, -2.0f, hit); p.rHandX = lerp(p.rHandX, -1.1f, hit);
                    p.bodyY -= 0.4f * hit; p.bodyX += 0.3f * hit; p.lLegX = lerp(p.lLegX, -0.6f, hit); p.rootZ -= 2f * hit;
                }));

        // ---------- Jetpack (rise, hover, fire the DMR down on them) ----------
        out.add(AttackDef.of("jetpack").range(6, 24).cd(220).weight(2).dur(40).weapon("dmr")
                .tick((e, t, n) -> {
                    if (n < 14) { e.setDeltaMovement(e.getDeltaMovement().x * 0.8, 0.32, e.getDeltaMovement().z * 0.8); e.hurtMarked = true; thrust(e); }
                    else if (n < 34) { e.setDeltaMovement(e.getDeltaMovement().x * 0.5, 0.03, e.getDeltaMovement().z * 0.5); e.hurtMarked = true; if (n % 2 == 0) thrust(e); }
                    if (n % 4 == 0 && n < 34) snd(e, SoundEvents.FIRECHARGE_USE, 0.5f, 0.5f + n * 0.01f);
                    if (n == 0) effect(e, MobEffects.SLOW_FALLING, 90, 0);
                    if ((n == 18 || n == 24 || n == 30) && t != null) {
                        bullet(e, t, 0.008, 6.5f, 0, 0xfff0b0, 34); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.9f, 1.5f);
                    }
                })
                .anim((p, t, a) -> {
                    float lift = win(t, 0, .12f) * (1 - win(t, .88f, 1)), aimK = win(t, .32f, .42f) * (1 - win(t, .85f, .95f));
                    float r = 1.1f * (kick(t, 18 / 40f, .1f) + kick(t, 24 / 40f, .1f) + kick(t, 30 / 40f, .1f));
                    // legs trail and dangle, arms brace out during the boost, then aim down
                    p.rLegX = lerp(p.rLegX, 0.25f + 0.08f * (float) Math.sin(a * 0.5f), lift); p.lLegX = lerp(p.lLegX, 0.1f - 0.08f * (float) Math.sin(a * 0.5f), lift);
                    p.rLegZ += 0.1f * lift; p.lLegZ -= 0.1f * lift;
                    p.rArmX = lerp(p.rArmX, -0.4f, lift); p.lArmX = lerp(p.lArmX, -0.3f, lift); p.rArmZ += 0.4f * lift * (1 - aimK); p.lArmZ -= 0.4f * lift * (1 - aimK);
                    shoulder(p, aimK, 0.45f, r);
                    p.bodyX += 0.15f * lift + 0.15f * aimK; p.headX += 0.3f * aimK; p.rootPitch += 0.1f * aimK;
                }));

        // ---------- Armor Lock (invulnerable crouch, EMP release) ----------
        out.add(AttackDef.of("armor_lock").range(0, 12).cd(400).weight(5).dur(44).weapon("ma37").noLos()
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.55f)
                .tick((e, t, n) -> {
                    if (n == 0) { effect(e, MobEffects.DAMAGE_RESISTANCE, 44, 4); snd(e, SoundEvents.ANVIL_LAND, 1f, 0.5f); snd(e, SoundEvents.BEACON_ACTIVATE, 1, 1.2f); }
                    Vec3 c = e.position().add(0, 1, 0);
                    if (n < 36) {
                        e.setDeltaMovement(0, e.getDeltaMovement().y, 0); eatProjectiles(e, c, 3);
                        if (n % 3 == 0) Fx.ring(L(e), e.position().add(0, 0.2 + (n % 9) * 0.25, 0), 1.1f, 0x60b0ff, 1f, 16);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, c, 2, 0.6, 0.05);
                        for (LivingEntity x : Combat.around(e, c, 2.2)) if (x instanceof Mob) Combat.knock(x, e.position(), 0.3, 0.05);
                    }
                    if (n == 36) {
                        for (LivingEntity x : Combat.around(e, c, 4.5)) { Combat.damage(e, x, 6); Combat.knock(x, e.position(), 1.6, 0.45); effect(x, MobEffects.MOVEMENT_SLOWDOWN, 40, 3); }
                        Fx.ring(L(e), c, 2.5f, 0x80c8ff, 1.5f, 30); Fx.ring(L(e), c, 4.2f, 0x60a0ff, 1.3f, 40);
                        Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, c, 50, 2, 0.4);
                        snd(e, SoundEvents.BEACON_DEACTIVATE, 1.4f, 1f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float lock = win(t, 0, .12f) * (1 - win(t, .8f, .84f)), rel = bump(win(t, .8f, 1));
                    // slam a fist into the ground, knee down, head tucked; release throws arms wide
                    p.rArmX = lerp(-0.6f, -0.35f, lock); p.rArmZ = 0.2f * lock - 1.2f * rel; p.rHandX = -0.4f;
                    p.lArmX = lerp(0, -0.15f, lock) - 0.2f * rel; p.lArmZ = -0.35f * lock + 1.2f * rel;
                    p.bodyX = 0.55f * lock - 0.25f * rel; p.headX = 0.4f * lock - 0.3f * rel;
                    p.rLegX = -1.2f * lock; p.lLegX = 1.1f * lock; p.rootY = -4.0f * lock + 1.0f * rel;
                    p.bodyZ = 0.015f * (float) Math.sin(a * 6f) * lock;
                }));

        // ---------- Drop Shield (throw the emitter, heal inside the bubble) ----------
        out.add(AttackDef.of("drop_shield").range(0, 16).cd(500).weight(5).dur(56).weapons("ma37", "emitter").noLos()
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.5f)
                .tick((e, t, n) -> {
                    Vec3 c = e.position().add(0, 0.9, 0);
                    if (n == 8) { snd(e, SoundEvents.BEACON_ACTIVATE, 1.2f, 1.4f); effect(e, MobEffects.REGENERATION, 60, 2); effect(e, MobEffects.DAMAGE_RESISTANCE, 50, 2); }
                    if (n >= 8) {
                        eatProjectiles(e, c, 3.5);
                        if (n % 2 == 0) for (int i = 0; i < 3; i++) Fx.ring(L(e), e.position().add(0, 0.2 + i * 1.0, 0), (float) Math.sqrt(9 - (i - 1) * (i - 1) * 2.0), 0x80c0ff, 0.9f, 22);
                        if (n % 10 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.8f, 1.6f);
                        Fx.burst(L(e), ParticleTypes.HAPPY_VILLAGER, c, 1, 0.5, 0.01);
                    }
                    if (n == 55) snd(e, SoundEvents.BEACON_DEACTIVATE, 1, 1.4f);
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .14f ? "emitter" : null;
                    float down = win(t, .14f, .22f);
                    // underhand toss to the feet, then crouch inside the bubble with rifle up
                    p.lArmX = track(t, 0, 0, .08f, 0.6f, .14f, -0.9f, .22f, -0.4f, 1, -0.4f) * (1 - down) + down * lerp(-0.4f, -1.4f, win(t, .22f, .3f));
                    p.bodyX = track(t, 0, 0, .14f, 0.45f, .3f, 0.2f, 1, 0.1f); p.headX = track(t, 0, 0, .14f, 0.35f, .3f, 0.05f, 1, 0);
                    p.rootY = track(t, 0, 0, .14f, -1.0f, .3f, -1.8f, .92f, -1.8f, 1, 0);
                    p.rLegX = -0.5f * win(t, .1f, .3f); p.lLegX = 0.4f * win(t, .1f, .3f);
                    shoulder(p, win(t, .22f, .32f) * (1 - win(t, .9f, 1)), 0, 0);
                    p.headY += 0.25f * (float) Math.sin(a * 0.15f) * win(t, .3f, .9f); // scanning
                }));

        // ---------- Active Camo (cloak, shimmer, reposition) ----------
        out.add(AttackDef.of("active_camo").range(4, 24).cd(600).weight(2).dur(20).weapon("dmr").noLos()
                .when((e, t) -> !e.hasEffect(MobEffects.INVISIBILITY))
                .tick((e, t, n) -> {
                    if (n == 4) { snd(e, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1, 1.3f); snd(e, SoundEvents.BEACON_POWER_SELECT, 0.6f, 0.6f); }
                    if (n >= 4 && n < 16) Fx.burst(L(e), ParticleTypes.REVERSE_PORTAL, e.position().add(0, 0.3 + (n - 4) * 0.12, 0), 6, 0.35, 0.02);
                    if (n == 14) { effect(e, MobEffects.INVISIBILITY, 160, 0); effect(e, MobEffects.MOVEMENT_SPEED, 160, 0); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .85f);
                    // touches the wrist panel, then sinks into a crouched stalk
                    p.lArmX = track(t, 0, 0, .2f, -1.3f, .55f, -1.3f, .75f, -0.8f, 1, -0.8f); p.lArmY = track(t, 0, 0, .2f, -0.7f, .55f, -0.7f, 1, 0.5f);
                    p.rArmX = track(t, 0, -0.6f, .2f, -1.2f, .55f, -1.2f, 1, -0.9f); p.rArmY = track(t, 0, -0.3f, .3f, 0.5f, .6f, 0.2f, 1, -0.2f);
                    p.headX = track(t, 0, 0, .2f, 0.45f, .55f, 0.45f, .8f, 0.1f, 1, 0.1f); p.headY = track(t, 0, 0, .2f, 0.3f, .55f, 0.3f, 1, 0);
                    p.bodyX += 0.2f * k; p.rootY -= 1.2f * win(t, .55f, .8f) * k;
                    p.rLegX = lerp(p.rLegX, 0.35f, win(t, .55f, .8f)); p.lLegX = lerp(p.lLegX, -0.45f, win(t, .55f, .8f));
                }));

        // ---------- Hologram (decoy runs at the enemy, they chase it) ----------
        out.add(AttackDef.of("hologram").range(6, 26).cd(360).weight(2).dur(40).weapon("ma37")
                .tick((e, t, n) -> {
                    if (n == 5) snd(e, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.2f, 1.2f);
                    if (n < 6 || t == null) return;
                    Vec3 s = e.position(), d = t.position().subtract(s);
                    Vec3 h = s.add(d.scale(Math.min(1, (n - 6) / 26.0) * 0.85));
                    for (int i = 0; i < 4; i++) Fx.dustBurst(L(e), i == 3 ? VISOR : 0x8ab8e0, 1.1f, h.add(0, 0.25 + i * 0.5, 0), 3, 0.15);
                    if (n % 4 == 0) Fx.burst(L(e), ParticleTypes.END_ROD, h.add(0, 1, 0), 2, 0.2, 0.01);
                    if (n % 5 == 0) for (LivingEntity x : Combat.around(e, h, 8)) if (x instanceof Mob m) m.getNavigation().moveTo(h.x, h.y, h.z, 1.1);
                    if (n == 39) { Fx.burst(L(e), ParticleTypes.POOF, h.add(0, 1, 0), 12, 0.3, 0.02); snd(e, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8f, 0.7f); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), point = bump(win(t, .02f, .25f));
                    // left arm sweeps out to project the decoy, then hold rifle and watch it run
                    p.lArmX = lerp(p.lArmX, -1.5f, point); p.lArmY = lerp(p.lArmY, -0.3f, point); p.bodyY -= 0.25f * point; p.headY += 0.2f * point;
                    shoulder(p, k * win(t, .25f, .35f), 0, 0); stance(p, k);
                    p.headX += 0.1f * k; p.rootY -= 0.4f * k;
                }));
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat suit = Mat.of(0x2b2f31).noise(.12f), plate = Mat.of(0x5c666b).bevel(.28f).noise(.08f), dark = Mat.of(0x3a4246).noise(.1f);
        Mat olive = Mat.of(0x4f5640).bevel(.25f).noise(.1f), metal = Mat.of(0x7e8488).noise(.06f), visor = Mat.of(VISOR).glow();
        Mat amber = Mat.of(0xffc060).glow(), pouch = Mat.of(0x3e4232).noise(.18f);
        Mat chest = Mat.of(0x5c666b).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return; // Mark V chest: two horizontal grenade pouches + centre ridge
            g.rect(x + 1, y + h - 2, w / 2 - 1, 1, 0x3a4246); g.rect(x + w / 2 + 1, y + h - 2, w / 2 - 2, 1, 0x3a4246);
            g.rect(x + w / 2, y, 1, h - 1, 0x484f52);
        });
        Mat helm = Mat.of(0x5c666b).bevel(.25f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) { g.rect(x + 1, y + h / 2, w - 2, 1, 0x30363a); g.px(x + w / 2, y + h / 2 - 1, 0x30363a); }
            if (f == Mat.Face.TOP) g.rect(x + w / 2, y, 1, h, 0x484f52);
        });
        Mat noble = Mat.of(0x3a4246).decal((m, g, x, y, w, h, f) -> { // shoulder Noble Team stripe
            if (f == Mat.Face.RIGHT || f == Mat.Face.LEFT || f == Mat.Face.FRONT) g.rect(x, y + 1, w, 1, 0xb0b8bc);
        });
        // torso
        rig.box(rig.torso, suit, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4.5f, -12, -3, 9, 6, 2, .2f);
        rig.box(rig.torso, pouch, -3.5f, -6.2f, -3.3f, 7, 2, 1);
        rig.box(rig.torso, olive, -3, -4, -2.6f, 6, 2, 1);
        rig.box(rig.torso, dark, -4.5f, -2.3f, -2.5f, 9, 2, 5); // belt
        rig.box(rig.torso, metal, 2.5f, -2, -3.1f, 2, 2, 1); // knife sheath clip
        rig.box(rig.torso, plate, -4, -12, 1.8f, 8, 6, 2); // armour ability pack
        rig.box(rig.torso, dark, -3, -11, 3.6f, 2, 5, 2); // thrusters
        rig.box(rig.torso, dark, 1, -11, 3.6f, 2, 5, 2);
        rig.box(rig.torso, amber, -0.5f, -10, 3.8f, 1, 2, 1);
        // head: Mark V[B] helmet, wraparound gold visor, side ear pods, UA antenna
        rig.box(rig.head, helm, -4, -8, -4, 8, 8, 8, .4f);
        rig.box(rig.head, visor, -3.5f, -6, -4.85f, 7, 3, 1);
        rig.box(rig.head, visor, -4.85f, -5.5f, -3.2f, 1, 2, 2);
        rig.box(rig.head, visor, 3.85f, -5.5f, -3.2f, 1, 2, 2);
        rig.box(rig.head, dark, -2, -2.8f, -4.9f, 4, 2, 1); // breather
        rig.box(rig.head, plate, -1, -9, -4, 2, 1, 7); // crest
        rig.box(rig.head, dark, -5, -4.5f, -1, 1, 3, 3);
        rig.box(rig.head, dark, 4, -4.5f, -1, 1, 3, 3);
        rig.box(rig.head, metal, 4.2f, -11, 1, 1, 6, 1); // antenna
        // arms
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, suit, -2, -2, -2, 4, 12, 4);
            rig.box(arm, s < 0 ? noble : plate, s < 0 ? -3.5f : -2.5f, -3, -2.8f, 6, 4, 5);
            rig.box(arm, olive, -2, 4, -2, 4, 5, 4, .35f);
            rig.box(arm, dark, -2, 9, -2, 4, 2, 4, .2f);
        }
        rig.box(rig.lArm, amber, 1.8f, 5, -1, 1, 2, 2); // wrist display (ability panel)
        // legs
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, suit, -2, 0, -2, 4, 12, 4);
            rig.box(leg, plate, -2, 0.5f, -2.6f, 4, 4, 1, .1f);
            rig.box(leg, olive, -1.5f, 5, -2.9f, 3, 2, 1); // knee pad
            rig.box(leg, dark, -2, 9, -3, 4, 3, 5, .2f);
        }
        rig.box(rig.rLeg, pouch, -3, 1.5f, -1, 1, 3, 2); // thigh holster
        buildWeapons(rig, metal, dark);
    }

    private static void buildWeapons(Rig rig, Mat metal, Mat dark) {
        Mat unsc = Mat.of(0x3c4034).noise(.08f), gun = Mat.of(0x2c2e30).noise(.06f), grey = Mat.of(0x6e7270).noise(.05f);
        Mat cov = Mat.of(0x5a3a8a).bevel(.3f).noise(.05f), covG = Mat.of(0x3c6a4a).bevel(.3f), pink = Mat.of(NEEDLE).glow();
        Mat blue = Mat.of(PLASMA_C).glow(), orange = Mat.of(FOCUS).glow(), red = Mat.of(0xff3020).glow(), green = Mat.of(0x60ff60).glow();
        Mat swordC = Mat.of(SWORD).glow(), hilt = Mat.of(0x404858).bevel(.3f);
        PartDefinition w;
        w = rig.weapon(rig.rHand, "ma37"); // bullpup-ish body, ammo counter, shroud
        rig.box(w, unsc, -1.5f, -3, -9, 3, 3, 12); rig.box(w, gun, -1, -2.5f, -13, 2, 2, 4); rig.box(w, gun, -1, 0, -6, 2, 3, 2);
        rig.box(w, Mat.of(0x60c0ff).glow(), -0.5f, -3.5f, -2, 1, 1, 1);
        w = rig.weapon(rig.rHand, "dmr"); // long receiver + scope
        rig.box(w, unsc, -1, -2.5f, -10, 2, 3, 13); rig.box(w, gun, -0.5f, -2, -15, 1, 1, 5); rig.box(w, gun, -1, -5, -6, 2, 2, 5);
        rig.box(w, gun, -1, 0.5f, -5, 2, 3, 2);
        w = rig.weapon(rig.rHand, "m6g");
        rig.box(w, grey, -1, -2, -7, 2, 2, 7); rig.box(w, gun, -1, 0, -1, 2, 3, 2); rig.box(w, gun, -0.5f, -2.6f, -4, 1, 1, 2);
        w = rig.weapon(rig.rHand, "shotgun");
        rig.box(w, gun, -1.5f, -2.5f, -6, 3, 3, 9); rig.box(w, grey, -1, -2.5f, -15, 2, 1, 9); rig.box(w, unsc, -1.5f, -1.5f, -12, 3, 2, 5);
        w = rig.weapon(rig.rHand, "sniper"); // long barrel, big scope, muzzle brake
        rig.box(w, gun, -1, -2.5f, -8, 2, 3, 12); rig.box(w, grey, -0.5f, -2, -20, 1, 1, 12); rig.box(w, gun, -1, -2.5f, -21, 2, 2, 1);
        rig.box(w, unsc, -1, -5, -6, 2, 2, 6);
        w = rig.weapon(rig.rHand, "spnkr"); // twin tubes
        rig.box(w, unsc, -2, -4.5f, -12, 2, 3, 16); rig.box(w, unsc, 0, -4.5f, -12, 2, 3, 16); rig.box(w, gun, -0.5f, -1.5f, -3, 1, 2, 3);
        rig.box(w, red, -0.5f, -5.5f, -6, 1, 1, 2);
        w = rig.weapon(rig.rHand, "gl"); // stubby wide barrel
        rig.box(w, unsc, -1.5f, -3, -6, 3, 3, 9); rig.box(w, gun, -2, -3.5f, -12, 4, 4, 6); rig.box(w, gun, -0.5f, 0, -2, 1, 3, 2);
        w = rig.weapon(rig.rHand, "needle"); // purple Covenant body with pink crystals
        rig.box(w, cov, -1.5f, -3, -11, 3, 3, 14); rig.box(w, pink, -1, -4.2f, -9, 2, 1, 5); rig.box(w, cov, -1, -2.5f, -14, 2, 2, 3);
        w = rig.weapon(rig.rHand, "repeater"); // bulbous green, blue vents
        rig.box(w, covG, -2, -3, -9, 4, 4, 10); rig.box(w, blue, -2.5f, -2.5f, -6, 1, 2, 4); rig.box(w, blue, 1.5f, -2.5f, -6, 1, 2, 4);
        rig.box(w, covG, -1, -2, -12, 2, 2, 3);
        w = rig.weapon(rig.rHand, "focus"); // long spine with orange emitter
        rig.box(w, cov, -1, -3, -13, 2, 3, 16); rig.box(w, orange, -0.5f, -2.5f, -15, 1, 1, 2); rig.box(w, cov, -1, -5, -6, 2, 2, 4);
        w = rig.weapon(rig.rHand, "sword"); // hilt + two plasma blades
        rig.box(w, hilt, -1, -1, -2, 2, 2, 4); rig.box(w, swordC, -1.5f, -0.5f, -14, 1, 1, 12); rig.box(w, swordC, 0.5f, -0.5f, -14, 1, 1, 12);
        w = rig.weapon(rig.rHand, "knife");
        rig.box(w, grey, -0.5f, -1, -7, 1, 2, 6); rig.box(w, gun, -0.5f, -0.5f, -1, 1, 1, 2);
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, unsc, -1, -1, -2, 2, 2, 2); rig.box(w, red, -0.5f, -1.8f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "pnade");
        rig.box(w, blue, -1, -1, -2, 2, 2, 2); rig.box(w, hilt, -1.5f, -0.5f, -1.5f, 3, 1, 1);
        w = rig.weapon(rig.lHand, "emitter");
        rig.box(w, metal, -1, -1, -2, 2, 2, 3); rig.box(w, green, -0.5f, -1.8f, -1, 1, 1, 1);
    }
}
