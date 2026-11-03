package com.doomslayer.kit.kits;

import static com.doomslayer.anim.Keys.*;
import static com.doomslayer.kit.kits.Doom2016Slayer.L;
import static com.doomslayer.kit.kits.Doom2016Slayer.aim;
import static com.doomslayer.kit.kits.Doom2016Slayer.hip;
import static com.doomslayer.kit.kits.Doom2016Slayer.hold;
import static com.doomslayer.kit.kits.Doom2016Slayer.mid;
import static com.doomslayer.kit.kits.Doom2016Slayer.mz;
import static com.doomslayer.kit.kits.Doom2016Slayer.ready;
import static com.doomslayer.kit.kits.Doom2016Slayer.rifle;
import static com.doomslayer.kit.kits.Doom2016Slayer.snd;
import static com.doomslayer.kit.kits.Doom2016Slayer.stance;
import static com.doomslayer.kit.kits.Doom2016Slayer.wide;

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
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** The Arbiter (Thel 'Vadam), Halo 3: Sangheili in silver Arbiter harness, full Covenant + Brute arsenal, energy sword, active camo, shields. */
public class Halo3Arbiter extends SlayerKit {
    static final int SWORD_C = 0x66d8ff, PLASMA_C = 0x5a8cff, CARBINE_C = 0x6dff4a, NEEDLE_C = 0xff4fd8, FUEL_C = 0x7cff3a,
            BEAM_C = 0xd070ff, NADE_C = 0x4fb4ff, SHIELD_C = 0xffd060, BLUE_BLOOD = 0x3050c8;
    static final float SHIELD = 12f;

    /** needles stuck per target -> supercombine at 7 (ponytail: weak map keyed by entity, fine for a handful of targets) */
    static final Map<Entity, Integer> NEEDLES = new WeakHashMap<>();

    static final ProjStyle PLASMA_BOLT = ProjStyle.make().color(PLASMA_C).size(0.3f).life(45).trail(ParticleTypes.ELECTRIC_SPARK);
    static final ProjStyle CARBINE_ROUND = ProjStyle.make().color(CARBINE_C).size(0.16f).life(30).trail(ParticleTypes.HAPPY_VILLAGER);
    static final ProjStyle NEEDLE = ProjStyle.make().color(NEEDLE_C).size(0.14f).life(60).homing(0.22f).trail(ParticleTypes.END_ROD)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, NEEDLE_C, 0.8f, pos, 4, 0.1);
                if (!(hit instanceof LivingEntity le) || !(p.getOwner() instanceof SlayerEntity o)) return;
                int n = NEEDLES.merge(le, 1, Integer::sum);
                if (n < 7) return;
                NEEDLES.remove(le); // supercombine
                Combat.blast(o, pos, 2.8f, 16f, 0.9f, null);
                Fx.dustBurst(l, NEEDLE_C, 1.8f, pos, 40, 1.2); Fx.ring(l, pos, 2f, NEEDLE_C, 1.4f, 26);
                Fx.burst(l, ParticleTypes.END_ROD, pos, 20, 0.4, 0.3);
                Fx.sound(l, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, 2f, 0.6f); Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 0.9f, 1.6f);
            });
    static final ProjStyle FUEL_ROD = ProjStyle.make().color(FUEL_C).size(0.45f).life(70).explode(3.2f).gravity(0.025).trail(ParticleTypes.HAPPY_VILLAGER);
    static final ProjStyle BRUTE_NADE = ProjStyle.make().color(0x8a5a3a).size(0.3f).life(28).explode(2.6f).gravity(0.04).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle SPIKE = ProjStyle.make().color(0xff8a3a).size(0.15f).life(35).gravity(0.012).pierce(1).trail(ParticleTypes.CRIT);
    static final ProjStyle FRAG = ProjStyle.make().color(0x56603a).size(0.26f).life(34).explode(3.4f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle PLASMA_NADE = ProjStyle.make().color(NADE_C).size(0.3f).life(40).explode(3.6f).gravity(0.045).trail(ParticleTypes.ELECTRIC_SPARK)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                if (hit != null) Fx.sound(l, pos, SoundEvents.SLIME_ATTACK, 1.2f, 1.6f); // "stuck!"
                Fx.explosion(l, pos, 3.6f);
                Fx.dustBurst(l, NADE_C, 2f, pos, 40, 1.4); Fx.ring(l, pos, 2.6f, NADE_C, 1.6f, 30);
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 30, 0.8, 0.4);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1.3f, 1.3f);
            });

    public Halo3Arbiter() {
        super("halo3_arbiter", "The Arbiter", "アービター", "Halo 3");
        eggPrimary = 0x8c9096; eggSecondary = SWORD_C;
        maxHealth = 90; speed = 0.34; armor = 14; knockbackRes = 0.5;
        scale = 1.14f; width = 0.7f; height = 2.1f; preferredRange = 8f; dmgMult = 1.05f;
        idleWeaponR = "carbine";
    }

    // ================= helpers =================
    static void bolt(SlayerEntity e, LivingEntity t, ProjStyle s, double speed, float dmg, double spread) {
        Combat.shoot(e, s, mz(e), aim(e, t, spread), speed, dmg);
    }
    static void slash(SlayerEntity e, float from, float to, float tilt, double reach, double half, float dmg) {
        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), (float) reach * 0.8f, from, to, tilt, SWORD_C, 1.1f, 14);
        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), (float) reach * 0.65f, from, to, tilt, 0xffffff, 0.6f, 10);
        for (LivingEntity f : Combat.cone(e, reach, half)) {
            Combat.damage(e, f, dmg); Combat.knock(f, e.position(), 0.5, 0.2);
            Fx.dustBurst(L(e), SWORD_C, 1f, mid(f), 8, 0.3); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(f), 6, 0.2, 0.2);
            snd(e, SoundEvents.TRIDENT_HIT, 1f, 1.4f);
        }
        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.1f, 1.3f);
    }
    static void flash(SlayerEntity e, int rgb) { Fx.dustBurst(L(e), rgb, 1.2f, mz(e), 5, 0.08); }

    // ================= anim helpers =================
    /** one-handed pistol-grip (plasma rifle, needler, spiker): weapon arm forward, off hand loose by chest */
    static void oneHand(Pose p, float k, float recoil) {
        p.rArmX = lerp(p.rArmX, -HALF - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, 0.1f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -0.6f, k); p.lArmY = lerp(p.lArmY, 0.3f, k); p.lArmZ = lerp(p.lArmZ, -0.25f, k);
        p.bodyY -= 0.2f * k; p.headY += 0.2f * k; p.bodyX += 0.08f * k - recoil * 0.05f; p.rootZ += recoil * 0.5f * k;
        p.rHandX -= recoil * 0.25f;
    }
    /** Sangheili crouched prowl: knees bent, chest low and forward, head up */
    static void prowl(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.45f, k); p.rLegX = lerp(p.rLegX, 0.35f, k);
        p.lLegZ -= 0.1f * k; p.rLegZ += 0.1f * k; p.rootY -= 1.4f * k; p.bodyX += 0.3f * k; p.headX -= 0.25f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // carbine held at the low ready, slight Sangheili hunch
        p.rArmX = -0.5f + p.rArmX * 0.25f; p.rArmY = -0.2f;
        p.lArmX = -0.8f + p.lArmX * 0.25f; p.lArmY = 0.6f; p.lArmZ = 0.05f;
        p.rHandX = -0.3f; p.bodyX += 0.12f; p.headX -= 0.1f;
        p.headZ += 0.03f * (float) Math.sin(age * 0.07f); // mandibles twitch / head tilt
    }

    /** Halo shields: after 5 s without mob damage, absorption recharges to SHIELD with a gold shimmer */
    @Override
    public void passive(SlayerEntity e) {
        if (e.tickCount - e.getLastHurtByMobTimestamp() < 100 || e.tickCount % 5 != 0) return;
        float a = e.getAbsorptionAmount();
        if (a >= SHIELD) return;
        if (a == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.6f); // recharge start
        e.setAbsorptionAmount(Math.min(SHIELD, a + 1.5f));
        Fx.dustBurst(L(e), SHIELD_C, 0.9f, e.position().add(0, 1.1, 0), 6, 0.5);
        if (e.getAbsorptionAmount() >= SHIELD) snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.4f);
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- Energy Sword: lunge (crouch, rocket forward, horizontal cleave) ----------
        out.add(AttackDef.of("sword_lunge").range(3, 9).cd(70).weight(3).dur(24).weapon("sword")
                .tick((e, t, n) -> {
                    if (n == 1) snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.8f);
                    if (n == 6) {
                        if (t != null) { Vec3 d = mid(t).subtract(e.position()).multiply(1, 0, 1).normalize(); e.setDeltaMovement(d.scale(1.25).add(0, 0.12, 0)); e.hurtMarked = true; }
                        snd(e, SoundEvents.TRIDENT_RIPTIDE_1, 1f, 1.5f);
                    }
                    if (n > 6 && n < 11) Fx.dustBurst(L(e), SWORD_C, 1f, e.position().add(0, 1, 0), 4, 0.3);
                    if (n == 11) slash(e, 70, -70, 5, 3.6, 70, 22f);
                })
                .anim((p, t, a) -> {
                    float crouch = win(t, 0, .22f) * (1 - win(t, .25f, .35f)), fly = win(t, .25f, .3f) * (1 - win(t, .5f, .6f));
                    p.rArmX = track(t, 0, -0.4f, .22f, -1.0f, .42f, -1.3f, .5f, -1.4f, .7f, -1.2f, 1, -0.4f);
                    p.rArmZ = track(t, 0, 0, .22f, 0.9f, .42f, 1.1f, .5f, -0.5f, .7f, -0.6f, 1, 0);
                    p.rHandY = HALF * win(t, .1f, .3f) * (1 - win(t, .8f, 1)); // blade flat for the cleave
                    p.lArmX = track(t, 0, 0, .22f, -0.9f, .45f, -1.3f, .55f, 0.4f, .8f, 0.3f, 1, 0); p.lArmZ = track(t, 0, 0, .45f, -0.3f, .6f, -0.7f, 1, 0);
                    p.bodyY = track(t, 0, 0, .22f, -0.55f, .42f, -0.6f, .5f, 0.8f, .7f, 0.6f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = 0.35f * crouch + 0.45f * fly + 0.15f * win(t, .5f, .55f) * (1 - win(t, .8f, 1));
                    p.rootY = -1.8f * crouch + 0.6f * fly - 0.8f * bump(win(t, .55f, .9f));
                    p.lLegX = -0.6f * crouch - 0.8f * fly - 0.5f * bump(win(t, .5f, .9f)); p.rLegX = 0.5f * crouch + 0.9f * fly + 0.4f * bump(win(t, .5f, .9f));
                    p.rootZ = -1.5f * fly;
                }));

        // ---------- Energy Sword: three-hit combo (forehand, backhand, overhead) ----------
        out.add(AttackDef.of("sword_combo").range(0, 3.6f).cd(30).weight(4).dur(34).weapon("sword")
                .tick((e, t, n) -> {
                    if (n == 6) slash(e, 60, -60, 10, 3.4, 60, 10f);
                    if (n == 15) slash(e, -60, 60, -15, 3.4, 60, 10f);
                    if (n == 26) {
                        slash(e, -15, 15, 85, 3.2, 35, 15f);
                        Fx.arc(L(e), e.position().add(0, 1.2, 0), e.getYRot(), 2.4f, -70, 70, 90, SWORD_C, 1.2f, 16);
                        if (t != null) Fx.ring(L(e), t.position().add(0, 0.1, 0), 1.2f, SWORD_C, 1f, 16);
                        snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    // forehand R->L (0-.25), backhand L->R (.3-.5), overhead (.55-.8)
                    p.rArmX = track(t, 0, -0.5f, .1f, -1.2f, .2f, -1.4f, .3f, -1.3f, .44f, -1.4f, .6f, -2.9f, .76f, -0.6f, .86f, -0.5f, 1, -0.4f);
                    p.rArmZ = track(t, 0, 0, .1f, 1.0f, .2f, -0.6f, .3f, -0.8f, .44f, 0.9f, .6f, 0.1f, .76f, 0, 1, 0);
                    p.rHandY = HALF * (win(t, .05f, .1f) * (1 - win(t, .5f, .56f)));
                    p.rHandX = track(t, .55f, 0, .6f, 0.6f, .76f, -0.3f, 1, 0);
                    p.bodyY = track(t, 0, 0, .1f, -0.55f, .2f, 0.6f, .3f, 0.65f, .44f, -0.6f, .55f, -0.2f, .76f, 0, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyX = track(t, 0, 0.1f, .2f, 0.25f, .44f, 0.25f, .6f, -0.25f, .76f, 0.5f, .9f, 0.35f, 1, 0.1f);
                    p.lArmX = track(t, 0, -0.3f, .2f, 0.3f, .44f, -0.9f, .6f, -0.6f, .76f, 0.3f, 1, -0.2f); p.lArmZ = track(t, 0, -0.2f, .2f, -0.6f, .44f, -0.1f, .76f, -0.5f, 1, -0.2f);
                    p.lLegX = track(t, 0, 0, .2f, -0.4f, .3f, -0.2f, .44f, -0.5f, .76f, -0.6f, 1, 0);
                    p.rLegX = track(t, 0, 0, .2f, 0.3f, .44f, 0.4f, .76f, 0.5f, 1, 0);
                    p.rootZ = -0.8f * bump(win(t, .1f, .3f)) - 0.8f * bump(win(t, .35f, .55f)) - 1.6f * bump(win(t, .6f, .9f));
                    p.rootY = 0.5f * bump(win(t, .55f, .66f)) - 1.2f * bump(win(t, .7f, .95f)); // hop into the overhead
                }));

        // ---------- Sword assassination (execution: grab, run through, lift, kick off) ----------
        out.add(AttackDef.of("sword_assassination").range(0, 3.5f).lowHealthTarget(0.3f).weight(6).cd(80).dur(36).weapon("sword")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.45, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.7f); }
                    hold(e, t, 0.9, n > 12 && n < 26 ? 0.05 * Math.min(n - 12, 8) : 0);
                    if (n == 12 && t != null) {
                        Combat.damage(e, t, Math.max(0, t.getHealth() - 1));
                        Fx.dustBurst(L(e), SWORD_C, 1.3f, mid(t), 20, 0.3); Fx.dustBurst(L(e), BLUE_BLOOD, 1.2f, mid(t), 10, 0.3);
                        snd(e, SoundEvents.TRIDENT_HIT, 1.2f, 0.7f); snd(e, SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.8f);
                    }
                    if (n > 12 && n < 26 && n % 3 == 0 && t != null) Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(t), 4, 0.2, 0.1);
                    if (n == 27 && t != null) {
                        Combat.damage(e, t, t.getHealth() + 100); Combat.knock(t, e.position(), 1.2, 0.3);
                        Fx.dustBurst(L(e), SWORD_C, 1.5f, mid(t), 25, 0.5);
                        snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 0.6f); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float lift = win(t, .33f, .55f) * (1 - win(t, .7f, .78f));
                    p.lArmX = track(t, 0, 0, .15f, -1.7f, .7f, -1.6f, .78f, -0.4f, 1, 0); p.lArmY = track(t, 0, 0, .15f, 0.4f, .7f, 0.3f, 1, 0); // grab the shoulder
                    p.rArmX = track(t, 0, 0, .15f, 0.5f, .33f, -1.4f, .55f, -2.0f, .72f, -2.0f, .78f, -1.0f, .9f, -0.8f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .15f, 0.5f, .33f, 0, .72f, 0, .78f, 0.8f, 1, 0);
                    p.rHandX = track(t, 0, 0, .33f, 0, .55f, -0.35f * lift, 1, 0);
                    p.bodyY = track(t, 0, 0, .15f, -0.5f, .33f, 0.3f, .72f, 0.2f, .8f, -0.4f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.bodyX = track(t, 0, 0, .15f, 0.15f, .33f, 0.4f, .55f, -0.15f, .72f, -0.2f, .8f, 0.2f, 1, 0);
                    p.headX = track(t, .3f, 0, .4f, 0.25f, .72f, -0.15f, 1, 0);
                    p.rootZ = track(t, 0, 0, .15f, -2.2f, .78f, -2f, .85f, 0.5f, 1, 0); p.rootY = -0.8f * bump(win(t, .2f, .45f)) + 0.4f * lift;
                    p.lLegX = track(t, 0, 0, .15f, -0.5f, .78f, -0.4f, 1, 0);
                    p.rLegX = track(t, 0, 0, .15f, 0.35f, .72f, 0.35f, .78f, -1.4f, .88f, -0.3f, 1, 0); // kick the body off the blade
                }));

        // ---------- Rifle bash: carbine butt-stroke (Halo melee) ----------
        out.add(AttackDef.of("rifle_bash").range(0, 3).cd(22).weight(3).dur(16).weapon("carbine")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.PLAYER_ATTACK_NODAMAGE, 1, 0.8f);
                    if (n == 6) {
                        for (LivingEntity f : Combat.cone(e, 3.2, 50)) { Combat.damage(e, f, 9); Combat.knock(f, e.position(), 0.9, 0.3); Fx.sparks(L(e), mid(f), 6); }
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.7f); snd(e, SoundEvents.SHIELD_BLOCK, 0.7f, 1.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .85f);
                    rifle(p, k, 0, 0);
                    // swing the stock: rifle rotates muzzle-up, both arms drive across
                    p.rHandX += track(t, 0, 0, .2f, 0.9f, .4f, -0.4f, .7f, -0.2f, 1, 0);
                    p.rArmX += track(t, 0, 0, .2f, 0.5f, .4f, -0.6f, .7f, -0.3f, 1, 0); p.lArmX += track(t, 0, 0, .2f, 0.4f, .4f, -0.5f, 1, 0);
                    p.bodyY += track(t, 0, 0, .2f, -0.45f, .4f, 0.5f, .7f, 0.3f, 1, 0); p.headY -= p.bodyY * 0.4f;
                    p.bodyX += track(t, 0, 0, .2f, -0.1f, .4f, 0.35f, 1, 0);
                    p.rootZ += -2f * bump(win(t, .2f, .7f)); p.rootY -= 0.5f * bump(win(t, .3f, .8f));
                    p.lLegX = track(t, 0, 0, .25f, -0.55f, .7f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .4f, 0.4f, .7f, 0.3f, 1, 0);
                }));

        // ---------- Plasma Rifle (dual-wielded, alternating, overheat vent) ----------
        out.add(AttackDef.of("plasma_rifle").range(2, 20).cd(34).weight(3).dur(34).weapons("plasma", "plasma_l")
                .tick((e, t, n) -> {
                    if (n >= 3 && n <= 23 && n % 2 == 1) {
                        boolean left = (n / 2) % 2 == 0;
                        Vec3 m = Combat.muzzle(e, 1.2, left ? -0.32 : 0.32, 0);
                        Combat.shoot(e, PLASMA_BOLT, m, t != null ? Combat.dirTo(m, t, 0.03, new java.util.Random(e.getRandom().nextLong())) : e.getViewVector(1f), 1.6, 2.4f);
                        Fx.dustBurst(L(e), PLASMA_C, 0.9f, m, 3, 0.06);
                        snd(e, SoundEvents.BLAZE_SHOOT, 0.45f, 1.8f + e.getRandom().nextFloat() * 0.2f);
                    }
                    if (n == 25) snd(e, SoundEvents.FIRE_EXTINGUISH, 0.9f, 1.3f); // overheat
                    if (n > 25 && n < 31) Fx.burst(L(e), ParticleTypes.CLOUD, mz(e), 2, 0.1, 0.02);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), fire = win(t, .08f, .1f) * (1 - win(t, .7f, .72f)), heat = bump(win(t, .72f, .95f));
                    float rr = 0.5f * Math.max(0, (float) Math.sin(a * PI)) * fire, rl = 0.5f * Math.max(0, -(float) Math.sin(a * PI)) * fire;
                    p.rArmX = lerp(p.rArmX, -HALF - rr * 0.3f + heat * 0.5f, k); p.rArmY = lerp(p.rArmY, 0.15f, k); p.rArmZ = lerp(p.rArmZ, 0.1f + heat * 0.3f, k);
                    p.lArmX = lerp(p.lArmX, -HALF - rl * 0.3f + heat * 0.5f, k); p.lArmY = lerp(p.lArmY, -0.15f, k); p.lArmZ = lerp(p.lArmZ, -0.1f - heat * 0.3f, k);
                    p.rHandX -= rr * 0.25f + heat * 0.4f; p.lHandX -= rl * 0.25f + heat * 0.4f;
                    p.bodyY += 0.12f * (rr - rl); p.rootZ += (rr + rl) * 0.4f; p.headX -= 0.15f * heat;
                    stance(p, k);
                }));

        // ---------- Covenant Carbine (four precise radioactive rounds) ----------
        out.add(AttackDef.of("carbine").range(5, 30).cd(26).weight(4).dur(26).weapon("carbine")
                .tick((e, t, n) -> {
                    if (n == 5 || n == 9 || n == 13 || n == 17) {
                        bolt(e, t, CARBINE_ROUND, 2.6, 5f, 0.008); flash(e, CARBINE_C);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, 1.7f); snd(e, SoundEvents.SLIME_SQUISH_SMALL, 0.5f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .18f, .88f), r = 0;
                    for (int i = 0; i < 4; i++) r += kick(t, (5 + 4 * i) / 26f, .12f);
                    rifle(p, k, 0, r); stance(p, k);
                    p.headX += 0.1f * k; p.rHandX -= r * 0.3f; p.bodyX -= r * 0.04f;
                }));

        // ---------- Needler (homing pink stream, supercombine) ----------
        out.add(AttackDef.of("needler").range(3, 18).cd(40).weight(3).dur(30).weapon("needler")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f, 1.5f);
                    if (n >= 4 && n <= 24 && n % 2 == 0) {
                        bolt(e, t, NEEDLE, 1.1, 1.6f, 0.12); Fx.dustBurst(L(e), NEEDLE_C, 0.7f, mz(e), 2, 0.05);
                        snd(e, SoundEvents.AMETHYST_BLOCK_HIT, 0.7f, 1.6f + e.getRandom().nextFloat() * 0.3f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .12f, .14f) * (1 - win(t, .8f, .82f));
                    float r = 0.3f * (0.5f + 0.5f * (float) Math.sin(a * PI)) * fire;
                    oneHand(p, k, r); stance(p, k);
                    p.rArmY += 0.12f * (float) Math.sin(a * 0.5f) * fire; p.bodyY += 0.08f * (float) Math.sin(a * 0.5f) * fire; // sprays across
                    p.rHandZ += 0.04f * (float) Math.sin(a * 3.1f) * fire;
                }));

        // ---------- Fuel Rod Gun (arcing green rods) ----------
        out.add(AttackDef.of("fuel_rod").range(6, 26).cd(70).weight(2).dur(32).weapon("fuelrod")
                .tick((e, t, n) -> {
                    if (n == 6 || n == 13 || n == 20) {
                        Vec3 m = mz(e); double dist = t != null ? m.distanceTo(mid(t)) : 12;
                        Combat.shoot(e, FUEL_ROD, m, aim(e, t, 0.02).add(0, dist * 0.012, 0).normalize(), 1.1, 12f);
                        Fx.dustBurst(L(e), FUEL_C, 1.4f, m, 10, 0.2); Fx.burst(L(e), ParticleTypes.SMOKE, Combat.muzzle(e, -0.5, 0.32, 0.1), 4, 0.1, 0.03);
                        snd(e, SoundEvents.GHAST_SHOOT, 0.7f, 1.5f); snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1f, 0.8f);
                    }
                    if (n == 26) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.8f, 0.8f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), r = 1.6f * (kick(t, 6 / 32f, .15f) + kick(t, 13 / 32f, .15f) + kick(t, 20 / 32f, .15f));
                    hip(p, k, -0.15f, r * 0.6f); wide(p, k * 0.7f);
                    p.rHandX -= r * 0.4f; p.bodyX -= r * 0.1f; p.headX -= r * 0.06f; p.rootZ += r * 0.6f;
                    p.lArmX += 0.3f * bump(win(t, .75f, .95f)); // slap in a fresh rod
                }));

        // ---------- Beam Rifle (scoped, pink-violet hyper-velocity beam) ----------
        out.add(AttackDef.of("beam_rifle").range(12, 48).cd(70).weight(2).dur(34).weapon("beam")
                .tick((e, t, n) -> {
                    if (n == 5) snd(e, SoundEvents.SPYGLASS_USE, 1, 1.2f);
                    if (n == 18 || n == 26) {
                        Vec3 d = aim(e, t, 0.001), m = mz(e);
                        Combat.hitscan(e, m, d, 50, 20f, 1, BEAM_C);
                        Fx.beam(L(e), m, t != null ? mid(t) : m.add(d.scale(50)), 0xffb0ff, 0.7f, 0.3);
                        snd(e, SoundEvents.TRIDENT_RIPTIDE_2, 1f, 1.8f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.8f, 1.4f);
                    }
                    if (n > 26 && n < 31) Fx.burst(L(e), ParticleTypes.CLOUD, mz(e), 1, 0.05, 0.01); // vent
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), zoom = win(t, .1f, .3f) * (1 - win(t, .8f, .9f));
                    float r = 1.4f * (kick(t, 18 / 34f, .15f) + kick(t, 26 / 34f, .15f));
                    rifle(p, k, 0, r); stance(p, k);
                    p.headX += 0.25f * zoom; p.headY -= 0.12f * zoom; p.bodyX += 0.15f * zoom; p.rootY -= 1.3f * zoom; // kneel into the scope
                    p.lLegX -= 0.3f * zoom; p.rLegX += 0.5f * zoom;
                    p.bodyZ += 0.01f * (float) Math.sin(a * 0.4f) * zoom; p.rHandX -= r * 0.3f;
                }));

        // ---------- Brute Shot (four bouncing grenades) ----------
        out.add(AttackDef.of("brute_shot").range(5, 22).cd(60).weight(2).dur(30).weapon("bruteshot")
                .tick((e, t, n) -> {
                    if (n == 5 || n == 10 || n == 15 || n == 20) {
                        Vec3 m = mz(e); double dist = t != null ? m.distanceTo(mid(t)) : 10;
                        Combat.shoot(e, BRUTE_NADE, m, aim(e, t, 0.04).add(0, 0.08 + dist * 0.015, 0).normalize(), 1.0, 8f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, m, 3, 0.1, 0.02); Fx.burst(L(e), ParticleTypes.FLAME, m, 3, 0.05, 0.02);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.9f); snd(e, SoundEvents.PISTON_EXTEND, 0.5f, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .9f), r = 0;
                    for (int i = 0; i < 4; i++) r += 1.4f * kick(t, (5 + 5 * i) / 30f, .14f);
                    rifle(p, k, -0.12f, r); stance(p, k);
                    p.rHandX -= r * 0.45f; p.bodyX -= r * 0.12f; p.rootZ += r * 0.4f; p.headX -= r * 0.08f;
                }));

        // ---------- Brute Shot bayonet (rising blade slash) ----------
        out.add(AttackDef.of("brute_shot_blade").range(0, 3.4f).cd(40).weight(2).dur(22).weapon("bruteshot")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.PLAYER_ATTACK_NODAMAGE, 1, 0.6f);
                    if (n == 9) {
                        Fx.arc(L(e), e.position().add(0, 1.0, 0), e.getYRot(), 2.4f, -50, 50, 70, 0xcccccc, 1f, 14);
                        for (LivingEntity f : Combat.cone(e, 3.6, 55)) { Combat.damage(e, f, 13); Combat.knock(f, e.position(), 0.6, 0.5); Fx.dustBurst(L(e), BLUE_BLOOD, 1f, mid(f), 8, 0.3); }
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f); snd(e, SoundEvents.ANVIL_LAND, 0.4f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .85f);
                    hip(p, k, 0, 0);
                    p.rArmX += track(t, 0, 0, .3f, 0.7f, .45f, -1.0f, .7f, -0.8f, 1, 0); p.lArmX += track(t, 0, 0, .3f, 0.6f, .45f, -0.9f, 1, 0);
                    p.rHandX += track(t, 0, 0, .3f, 0.5f, .45f, -0.5f, 1, 0);
                    p.bodyY += track(t, 0, 0, .3f, -0.45f, .45f, 0.35f, 1, 0); p.headY -= p.bodyY * 0.4f;
                    p.bodyX += track(t, 0, 0, .3f, 0.35f, .45f, -0.25f, .7f, -0.1f, 1, 0);
                    p.rootY += -1.2f * bump(win(t, .1f, .4f)) + 0.4f * bump(win(t, .4f, .6f));
                    p.rootZ += -1.6f * bump(win(t, .3f, .8f));
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, .8f, -0.4f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.4f, .8f, 0.35f, 1, 0);
                }));

        // ---------- Spiker (rapid heated spikes, arcing slightly) ----------
        out.add(AttackDef.of("spiker").range(2, 16).cd(34).weight(3).dur(28).weapon("spiker")
                .tick((e, t, n) -> {
                    if (n >= 3 && n <= 21 && n % 2 == 1) {
                        bolt(e, t, SPIKE, 1.7, 2.4f, 0.05); Fx.burst(L(e), ParticleTypes.FLAME, mz(e), 1, 0.03, 0.01);
                        snd(e, SoundEvents.ARROW_SHOOT, 0.7f, 1.6f + e.getRandom().nextFloat() * 0.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .88f), fire = win(t, .1f, .12f) * (1 - win(t, .76f, .78f));
                    float r = 0.4f * Math.abs((float) Math.sin(a * PI * 0.5f)) * fire;
                    oneHand(p, k, r); prowl(p, k * 0.6f);
                    p.lArmX = lerp(p.lArmX, -0.9f, k); p.lArmZ = lerp(p.lArmZ, -0.5f, k); // off-hand balancing, Sangheili style
                    p.rHandZ += 0.04f * (float) Math.sin(a * 2.7f) * fire;
                }));

        // ---------- Gravity Hammer (wind-up overhead, ground slam shockwave) ----------
        out.add(AttackDef.of("gravity_hammer").range(0, 4).cd(90).weight(2).dur(34).weapon("hammer")
                .tick((e, t, n) -> {
                    if (n == 6) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 0.5f);
                    if (n == 16) {
                        Vec3 c = e.position().add(e.getViewVector(1f).multiply(1, 0, 1).normalize().scale(1.8)).add(0, 0.2, 0);
                        for (LivingEntity f : Combat.around(e, c, 4.5)) { Combat.damage(e, f, 18); Combat.knock(f, c, 1.8, 0.7); }
                        Fx.explosion(L(e), c, 2f);
                        for (int i = 0; i < 3; i++) Fx.ring(L(e), c, 1.2f + i * 1.3f, 0x7ab0ff, 1.5f, 24 + i * 8);
                        Fx.burst(L(e), ParticleTypes.POOF, c, 20, 1.2, 0.1); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, c, 20, 0.8, 0.4);
                        snd(e, SoundEvents.ANVIL_LAND, 1.2f, 0.5f); snd(e, SoundEvents.GENERIC_EXPLODE, 1.2f, 0.7f); snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.4f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float slam = 16 / 34f;
                    float armX = track(t, 0, -0.6f, .3f, -3.0f, .4f, -3.1f, slam, -0.9f, .7f, -0.8f, 1, -0.5f);
                    p.rArmX = armX; p.lArmX = armX - 0.1f; p.rArmY = -0.3f; p.lArmY = 0.5f; p.rArmZ = 0.1f; p.lArmZ = -0.1f;
                    p.rHandX = track(t, 0, -0.4f, .3f, 0.6f, slam, -0.5f, 1, -0.3f);
                    p.bodyX = track(t, 0, 0, .3f, -0.35f, .4f, -0.4f, slam, 0.6f, .7f, 0.55f, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, -0.3f, slam, 0.15f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.headX = track(t, 0, 0, .3f, -0.3f, slam, 0.2f, 1, 0);
                    p.rootY = track(t, 0, 0, .25f, -0.6f, .38f, 1.4f, slam, -1.8f, .75f, -1.5f, 1, 0); // hop up, crash down
                    p.rootZ = track(t, 0, 0, .3f, 0.6f, slam, -1.5f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.2f, slam, -0.7f, .8f, -0.6f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.2f, slam, 0.6f, .8f, 0.5f, 1, 0);
                    p.lLegZ = -0.2f * win(t, .4f, slam) * (1 - win(t, .8f, 1)); p.rLegZ = -p.lLegZ;
                    p.bodyZ = 0.04f * (float) Math.sin(a * 6) * kick(t, slam, .2f) * 4; // impact shudder
                }));

        // ---------- Plasma Grenade (sticky, overhand) ----------
        out.add(AttackDef.of("plasma_grenade").range(4, 16).cd(150).weight(2).dur(20).weapons("carbine", "pnade")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.BEACON_ACTIVATE, 0.6f, 2f);
                    if (n == 10) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.5); double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Combat.shoot(e, PLASMA_NADE, from, aim(e, t, 0.01).add(0, 0.12 + dist * 0.02, 0).normalize(), 0.75 + dist * 0.025, 16f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 1.1f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .52f ? "pnade" : null;
                    p.rArmX = -0.6f; p.rArmY = -0.3f; p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, 0, .2f, -2.8f, .42f, -3.0f, .52f, -1.2f, .7f, -0.6f, 1, 0); // overhand
                    p.lArmZ = track(t, 0, 0, .2f, -0.3f, .52f, 0, 1, 0);
                    p.bodyY = track(t, 0, 0, .42f, -0.5f, .55f, 0.4f, .8f, 0.2f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyX = track(t, 0, 0, .42f, -0.3f, .58f, 0.4f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.4f, .55f, -0.6f, 1, 0); p.rLegX = track(t, 0, 0, .42f, 0.3f, .55f, 0.5f, 1, 0);
                    p.rootZ = track(t, .4f, 0, .58f, -1.5f, 1, 0); p.rootY = -0.5f * bump(win(t, .4f, .8f));
                }));

        // ---------- Frag Grenade (sidearm skip throw) ----------
        out.add(AttackDef.of("frag_grenade").range(4, 16).cd(150).weight(2).dur(20).weapons("carbine", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.4f);
                    if (n == 9) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.3); double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Combat.shoot(e, FRAG, from, aim(e, t, 0.02).add(0, 0.1 + dist * 0.015, 0).normalize(), 0.8 + dist * 0.025, 13f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .47f ? "frag" : null;
                    p.rArmX = -0.7f; p.rArmY = -0.3f; p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, 0, .2f, -0.6f, .4f, 0.4f, .47f, -1.6f, .7f, -1.2f, 1, 0); // low sidearm
                    p.lArmZ = track(t, 0, 0, .4f, -1.0f, .47f, 0.2f, .7f, 0.3f, 1, 0);
                    p.bodyY = track(t, 0, 0, .4f, -0.6f, .5f, 0.5f, .75f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyZ = track(t, 0, 0, .4f, -0.15f, .5f, 0.05f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.4f, .5f, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, .4f, 0.35f, .5f, 0.45f, 1, 0);
                    p.rootY = -0.9f * bump(win(t, .25f, .7f)); p.rootZ = track(t, .35f, 0, .5f, -1.2f, 1, 0);
                }));

        // ---------- Active Camo (equipment: shimmer out, invisibility + damage reduction) ----------
        out.add(AttackDef.of("active_camo").range(0, 30).cd(600).weight(2).dur(26).weapon("sword").noLos()
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.6f || Combat.around(e, e.position(), 10).size() >= 3)
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1f, 1.2f);
                    if (n > 4 && n < 18) Fx.dustBurst(L(e), 0xb0e0ff, 0.8f, e.position().add(0, 0.2 + (n - 4) * 0.13, 0), 8, 0.4);
                    if (n == 16) {
                        e.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, false, false));
                        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1, false, false));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0, false, false));
                        Fx.burst(L(e), ParticleTypes.REVERSE_PORTAL, e.position().add(0, 1, 0), 30, 0.4, 0.05);
                        snd(e, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1f, 1f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f), press = bump(win(t, .2f, .55f));
                    // right fist to chest, left hand presses the gauntlet control, then low crouch as he fades
                    p.rArmX = lerp(p.rArmX, -1.2f, k); p.rArmY = lerp(p.rArmY, 0.6f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
                    p.lArmX = lerp(p.lArmX, -1.0f - 0.2f * press, k); p.lArmY = lerp(p.lArmY, -0.7f, k);
                    p.headX += 0.35f * press; p.headY -= 0.25f * press; p.bodyY -= 0.15f * press;
                    float fade = win(t, .55f, .75f) * (1 - win(t, .9f, 1));
                    prowl(p, fade); p.bodyZ += 0.03f * (float) Math.sin(a * 3) * fade;
                }));
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat suit = Mat.of(0x2a2a30).noise(.12f), silver = Mat.of(0x9a9ea6).bevel(.32f).noise(.06f), steel = Mat.of(0x6a6e78).bevel(.25f).noise(.08f);
        Mat skin = Mat.of(0x5a4e44).noise(.22f), dark = Mat.of(0x1c1c22), gold = Mat.of(0xb08a3a).bevel(.3f).noise(.05f);
        Mat eye = Mat.of(0xffb040).glow(), glowB = Mat.of(SWORD_C).glow();
        Mat chest = Mat.of(0x9a9ea6).bevel(.32f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return; // central ridge + Covenant glyph
            int cx = x + w / 2;
            g.rect(cx - 1, y, 2, h, 0x6a6e78);
            for (int i = 0; i < 3; i++) { g.px(cx - 3 + i, y + 2 + i, 0xc8a050); g.px(cx + 2 - i, y + 2 + i, 0xc8a050); }
        });
        Mat helm = Mat.of(0x9a9ea6).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) { g.rect(x + 1, y + h - 2, w - 2, 1, 0x4a4e58); g.rect(x + 2, y + 1, w - 4, 1, 0xb08a3a); }
        });
        // torso: broad Sangheili chest, gold-trimmed collar, hip skirt plates, back spine
        rig.box(rig.torso, suit, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -5, -12, -3.5f, 10, 7, 3, .2f);
        rig.box(rig.torso, gold, -3, -13, -3, 6, 1, 6);
        rig.box(rig.torso, steel, -4, -5, -2.8f, 8, 3, 1);
        rig.box(rig.torso, dark, -4.5f, -2, -2.5f, 9, 2, 5);
        rig.box(rig.torso, gold, -1, -2, -3, 2, 2, 1);
        rig.box(rig.torso, steel, -4, 0, -3, 3, 4, 1); rig.box(rig.torso, steel, 1, 0, -3, 3, 4, 1); // tassets
        rig.box(rig.torso, silver, -4, -12, 2, 8, 8, 2);
        rig.box(rig.torso, glowB, -0.5f, -10, 3.6f, 1, 4, 1);
        // head: long forward-swept helmet, crest, split mandibles, glowing eyes
        rig.box(rig.head, skin, -2, -3, -2, 4, 3, 4);           // neck
        rig.box(rig.head, helm, -3, -8, -7, 6, 5, 9, .2f);       // skull/helmet, juts forward
        rig.box(rig.head, silver, -1, -10, -6, 2, 2, 9);         // crest
        rig.box(rig.head, gold, -0.5f, -11, 0, 1, 2, 4);         // crest spine back
        rig.box(rig.head, steel, -2.5f, -9, -9, 5, 3, 3);        // brow/visor plate
        rig.box(rig.head, eye, -2.6f, -6.5f, -7.4f, 1, 1, 1); rig.box(rig.head, eye, 1.6f, -6.5f, -7.4f, 1, 1, 1);
        rig.box(rig.head, skin, -2.5f, -3.5f, -8, 2, 2, 5); rig.box(rig.head, skin, 0.5f, -3.5f, -8, 2, 2, 5); // mandibles
        rig.box(rig.head, dark, -1, -3, -6, 2, 1, 3);
        // arms: big pauldrons, long forearms with gauntlets
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, suit, -2, -2, -2, 4, 12, 4);
            rig.box(arm, silver, s < 0 ? -4 : -2, -4, -3.5f, 6, 5, 7, .2f);
            rig.box(arm, gold, s < 0 ? -4.5f : 3.5f, -3, -2.5f, 1, 3, 5);
            rig.box(arm, steel, -2, 5, -2, 4, 5, 4, .35f);
            rig.box(arm, s < 0 ? glowB : dark, -0.5f, 6, -2.8f, 1, 2, 1);
        }
        // legs: digitigrade (thigh forward, shin swept back, splayed hoof-boot)
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, suit, -2, 0, -2.5f, 4, 6, 4);
            rig.box(leg, silver, -2, 0.5f, -3.2f, 4, 5, 1);
            PartDefinition shin = rig.pivot(leg, s < 0 ? "rShin" : "lShin", 0, 5.5f, -0.5f, -0.35f, 0, 0);
            rig.box(shin, suit, -1.5f, 0, -1.5f, 3, 5, 3);
            rig.box(shin, steel, -1.5f, 0.5f, 1.3f, 3, 4, 1);
            rig.box(leg, dark, -2, 10, -4, 4, 2, 6, .1f);
            rig.box(leg, steel, -1.5f, 10.5f, -5, 3, 1, 1);
        }
        buildWeapons(rig);
    }

    private static void buildWeapons(Rig rig) {
        Mat cov = Mat.of(0x6a5aa8).bevel(.35f).noise(.05f), covDk = Mat.of(0x3a3060).bevel(.3f), grip = Mat.of(0x222228);
        Mat carb = Mat.of(0x2f5a46).bevel(.35f), carbG = Mat.of(CARBINE_C).glow();
        Mat needl = Mat.of(0x8a3a8a).bevel(.35f), needG = Mat.of(NEEDLE_C).glow();
        Mat fuel = Mat.of(0x5a6a4a).bevel(.3f), fuelG = Mat.of(FUEL_C).glow();
        Mat beam = Mat.of(0x7a6e9a).bevel(.3f), beamG = Mat.of(BEAM_C).glow();
        Mat steel = Mat.of(0x6a6e78).bevel(.25f), iron = Mat.of(0x4a4440).noise(.15f), rust = Mat.of(0x7a4a2a).noise(.18f), blade = Mat.of(0xb8bcc4).bevel(.2f);
        Mat swordG = Mat.of(SWORD_C).glow(), plasG = Mat.of(PLASMA_C).glow(), hamG = Mat.of(0x7ab0ff).glow(), nadeG = Mat.of(NADE_C).glow();
        PartDefinition w;
        // energy sword: hilt + forked twin blades
        w = rig.weapon(rig.rHand, "sword");
        rig.box(w, grip, -0.5f, -1, -1, 1, 3, 3); rig.box(w, steel, -1.5f, -2, -2, 3, 5, 1);
        rig.box(w, swordG, -0.5f, -3, -14, 1, 1, 12); rig.box(w, swordG, -0.5f, 2, -13, 1, 1, 11); rig.box(w, swordG, -0.5f, -2, -4, 1, 4, 2);
        // plasma rifle (pair for dual wield)
        for (int s = 0; s < 2; s++) {
            w = rig.weapon(s == 0 ? rig.rHand : rig.lHand, s == 0 ? "plasma" : "plasma_l");
            rig.box(w, cov, -1.5f, -2, -7, 3, 3, 8); rig.box(w, covDk, -1, -3, -10, 2, 1, 5); rig.box(w, covDk, -1, 1, -10, 2, 1, 4);
            rig.box(w, plasG, -0.5f, -1.5f, -9, 1, 1, 2); rig.box(w, grip, -0.5f, 1, -1, 1, 3, 2);
        }
        w = rig.weapon(rig.rHand, "carbine");
        rig.box(w, carb, -1, -2, -10, 2, 3, 14); rig.box(w, carb, -0.5f, -1.5f, -15, 1, 1, 5); rig.box(w, carbG, -0.5f, -3.2f, -8, 1, 1, 5);
        rig.box(w, carbG, 0.6f, -1.5f, -9, 1, 1, 6); rig.box(w, grip, -0.5f, 1, -2, 1, 3, 2);
        w = rig.weapon(rig.rHand, "needler");
        rig.box(w, needl, -1.5f, -2, -7, 3, 3, 8); rig.box(w, needG, -1, -4, -6, 1, 2, 1); rig.box(w, needG, 0, -4.5f, -4, 1, 3, 1);
        rig.box(w, needG, -1, -4, -2, 1, 2, 1); rig.box(w, grip, -0.5f, 1, -1, 1, 3, 2);
        w = rig.weapon(rig.rHand, "fuelrod");
        rig.box(w, fuel, -2, -3, -12, 4, 4, 16); rig.box(w, fuelG, -2.5f, -3.5f, -13, 5, 5, 1); rig.box(w, fuelG, 2, -2, -8, 1, 2, 6);
        rig.box(w, grip, -0.5f, 1, -2, 1, 3, 2); rig.box(w, iron, -1.5f, -2.5f, 4, 3, 3, 2);
        w = rig.weapon(rig.rHand, "beam");
        rig.box(w, beam, -1, -2, -14, 2, 2, 18); rig.box(w, beam, -1, -3, -19, 2, 1, 5); rig.box(w, beam, -1, 0, -19, 2, 1, 5);
        rig.box(w, beamG, -0.5f, -3.5f, -8, 1, 1, 6); rig.box(w, grip, -0.5f, 0, -1, 1, 3, 2);
        w = rig.weapon(rig.rHand, "bruteshot");
        rig.box(w, iron, -2, -3, -9, 4, 4, 12); rig.box(w, rust, -2.5f, -1, -5, 5, 3, 3); rig.box(w, iron, -1, -2.5f, -13, 2, 2, 4);
        rig.box(w, blade, -0.5f, 1, -14, 1, 3, 9); rig.box(w, grip, -0.5f, 1, -1, 1, 3, 2);
        w = rig.weapon(rig.rHand, "spiker");
        rig.box(w, iron, -1, -2, -7, 2, 3, 9); rig.box(w, rust, -1.8f, -1.5f, -11, 1, 1, 6); rig.box(w, rust, 0.8f, -1.5f, -11, 1, 1, 6);
        rig.box(w, blade, -0.5f, -3.5f, -5, 1, 2, 4); rig.box(w, grip, -0.5f, 1, -1, 1, 3, 2);
        w = rig.weapon(rig.rHand, "hammer");
        rig.box(w, grip, -0.5f, -0.5f, -12, 1, 1, 18); rig.box(w, iron, -3, -3, -18, 6, 6, 6); rig.box(w, hamG, -2, -2, -18.6f, 4, 4, 1);
        rig.box(w, blade, -0.5f, -6, -16, 1, 3, 2); rig.box(w, rust, -1, -1, 5, 2, 2, 2);
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, Mat.of(0x56603a), -1, -1, -2, 2, 2, 2); rig.box(w, grip, -0.5f, -1.8f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "pnade");
        rig.box(w, nadeG, -1, -1, -2, 2, 2, 2); rig.box(w, covDk, -1.5f, -0.5f, -1.5f, 3, 1, 1);
    }
}
