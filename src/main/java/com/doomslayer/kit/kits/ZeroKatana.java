package com.doomslayer.kit.kits;

import static com.doomslayer.anim.Keys.*;
import static com.doomslayer.kit.kits.KU.*;

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
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Zero (Katana ZERO): the Dragon. Chronos slow-motion, dash slashes, thrown objects. */
public class ZeroKatana extends SlayerKit {
    static final int NEON = 0x40e8ff, PINK = 0xff3090;
    static final ProjStyle BOTTLE = ProjStyle.make().color(0x60b060).size(0.25f).life(40).gravity(0.03).trail(ParticleTypes.CRIT);
    static final ProjStyle KNIFE = ProjStyle.make().color(0xd0d0d0).size(0.2f).life(40).pierce(1).trail(ParticleTypes.CRIT);

    public ZeroKatana() {
        super("katana_zero", "Zero", "ゼロ", "Katana ZERO");
        eggPrimary = 0x202428; eggSecondary = NEON;
        maxHealth = 55; speed = 0.4; armor = 2; knockbackRes = 0.1;
        scale = 0.98f; preferredRange = 3f; dmgMult = 1.2f;
        idleWeaponR = "katana";
    }

    @Override public void idle(Pose p, SlayerEntity e, float age, float m) { p.rArmX = -0.2f + p.rArmX * 0.5f; p.rArmZ = 0.1f; p.rHandX = 0.8f; p.bodyX += 0.06f; }

    @Override
    public void passive(SlayerEntity e) {
        // Chronos drug: periodic slow-motion field around Zero when enemies are near
        if (e.tickCount % 200 == 0 && !Combat.around(e, e.position(), 8).isEmpty()) {
            for (LivingEntity f : Combat.around(e, e.position(), 8)) f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false));
            Fx.ring(L(e), e.position().add(0, 1, 0), 3f, NEON, 1.4f, 24); snd(e, SoundEvents.BEACON_DEACTIVATE, 0.6f, 1.8f);
        }
    }

    @Override
    protected void defineAttacks(List<AttackDef> out) {
        out.add(AttackDef.of("slash").range(0, 3.2f).cd(12).weight(3).dur(12).weapon("katana")
                .tick((e, t, n) -> {
                    if (n == 2) Combat.lunge(e, 0.5, 0);
                    if (n == 4) { slash(e, 80, -70, 5, NEON); melee(e, 3.3, 8, 0.5, 0.1, NEON); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.7f); }
                }).anim((p, t, a) -> swing(p, t, 4 / 12f, 1.1f)));

        // dash through the target, cutting everything on the line
        out.add(AttackDef.of("dash_slash").range(3, 12).cd(60).weight(3).dur(16).weapon("katana")
                .tick((e, t, n) -> {
                    if (n == 2) { Combat.lunge(e, 1.9, 0.02); snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.6f, 1.8f); }
                    if (n >= 2 && n < 8) { Fx.burst(L(e), ParticleTypes.END_ROD, e.position().add(0, 1, 0), 3, 0.15, 0.01); Fx.dustBurst(L(e), NEON, 1f, e.position().add(0, 1, 0), 3, 0.2); }
                    if (n == 6) { slash(e, 90, -90, 0, NEON); melee(e, 3.6, 12, 0.9, 0.2, NEON); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1.4f); }
                }).anim((p, t, a) -> {
                    float k = bump(win(t, .1f, .8f));
                    p.rArmX = -1.5f * k; p.rArmZ = -1.0f * k; p.lArmX = 0.8f * k; p.bodyX += 0.9f * k; p.headX -= 0.5f * k; p.rootY -= 1.5f * k;
                    p.lLegX = -1.1f * k; p.rLegX = 1.0f * k; p.rootZ = -2f * k;
                }));

        // rising slash (launcher)
        out.add(AttackDef.of("uppercut").range(0, 3f).cd(50).weight(2).dur(18).weapon("katana")
                .tick((e, t, n) -> {
                    if (n == 5) {
                        slash(e, 0, 0, -75, NEON);
                        for (LivingEntity f : Combat.cone(e, 3.2, 60)) { Combat.damage(e, f, 9); f.setDeltaMovement(0, 0.8, 0); f.hurtMarked = true; }
                        e.setDeltaMovement(0, 0.45, 0); e.hurtMarked = true; snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.2f);
                    }
                }).anim((p, t, a) -> {
                    float k = win(t, .1f, .35f) * (1 - win(t, .5f, .8f));
                    p.rArmX = lerp(p.rArmX, -3.0f, k); p.rArmZ = 0.3f * k; p.bodyX -= 0.3f * k; p.rootY -= 1.2f * win(t, 0, .25f) * (1 - win(t, .25f, .35f));
                }));

        // throw a bottle / knife pickup
        out.add(AttackDef.of("throw_bottle").range(4, 16).cd(60).weight(2).dur(18)
                .tick((e, t, n) -> { if (n == 6) { Combat.shoot(e, BOTTLE, mz(e), aim(e, t, 0.02).add(0, 0.05, 0).normalize(), 1.4, 6f); snd(e, SoundEvents.SNOWBALL_THROW, 1f, 1.2f); } })
                .anim((p, t, a) -> throwAnim(p, t)));
        out.add(AttackDef.of("throw_knife").range(4, 18).cd(70).weight(1).dur(16)
                .tick((e, t, n) -> { if (n == 5) { Combat.shoot(e, KNIFE, mz(e), aim(e, t, 0), 2.0, 9f); snd(e, SoundEvents.TRIDENT_THROW, 0.8f, 1.9f); } })
                .anim((p, t, a) -> throwAnim(p, t)));

        // Chronos: bullet-time burst, freezes foes then fan of cuts
        out.add(AttackDef.of("chronos_flurry").range(0, 6).cd(260).weight(1).dur(44).weapon("katana")
                .tick((e, t, n) -> {
                    if (n == 1) {
                        for (LivingEntity f : Combat.around(e, e.position(), 7)) f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6));
                        Fx.ring(L(e), e.position().add(0, 1, 0), 4f, PINK, 1.6f, 28); snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 0.5f);
                    }
                    if (n >= 8 && n < 40 && n % 4 == 0) {
                        for (LivingEntity f : Combat.around(e, e.position(), 6)) { Combat.damage(e, f, 4.5f); Fx.sparks(L(e), mid(f), 4); Fx.beam(L(e), e.position().add(0, 1, 0), mid(f), n % 8 == 0 ? NEON : PINK, 0.5f, 0.3); }
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.6f + n * 0.01f);
                    }
                }).anim((p, t, a) -> {
                    float s = (float) Math.sin(t * PI * 8f);
                    p.rArmX = -1.5f + 0.6f * Math.abs(s); p.rArmZ = 1.2f * s; p.lArmX = -0.8f; p.bodyY = 0.8f * s; p.headY = -0.4f * s; p.lLegX = -0.5f; p.rLegX = 0.4f; p.rootY -= 0.6f;
                }));

        // deflect: swing to bat away; emulate with brief resistance + shockwave
        out.add(AttackDef.of("deflect").range(0, 6).cd(100).weight(1).dur(14).weapon("katana")
                .tick((e, t, n) -> {
                    if (n == 1) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 24, 4));
                    if (n == 4) { slash(e, 60, -60, 20, 0xffffff); Combat.blast(e, e.position(), 3.5f, 5, 1f, e); Fx.ring(L(e), e.position().add(0, 1.2, 0), 2f, 0xffffff, 1.5f, 20); snd(e, SoundEvents.ANVIL_LAND, 0.5f, 2f); }
                }).anim((p, t, a) -> swing(p, t, 4 / 14f, 0.6f)));
    }

    @Override
    public void buildModel(Rig rig) {
        Mat robe = Mat.of(0x2a2f38).noise(.1f), under = Mat.of(0x15171c), skin = Mat.of(0xd8b090).noise(.04f).bevel(.1f), hair = Mat.of(0x0d0d10).noise(.06f);
        Mat red = Mat.of(0xa02030).noise(.1f), glow = Mat.of(NEON).glow(), steel = Mat.of(0xe0e4ea).bevel(.1f);
        rig.box(rig.torso, robe, -4, -12, -2, 8, 12, 4, .1f);
        rig.box(rig.torso, red, -4, -4, -2.4f, 8, 2, 5); // sash
        rig.box(rig.torso, under, -1, -12, -2.4f, 2, 8, 1);
        rig.box(rig.head, skin, -4, -8, -4, 8, 8, 8);
        rig.box(rig.head, hair, -4, -8, -4, 8, 3, 8, .3f);
        rig.box(rig.head, hair, -4, -8, 3, 8, 8, 1, .3f); // long black hair
        rig.box(rig.head, under, -3, -4.5f, -4.2f, 6, 1, 1);
        rig.box(rig.head, glow, -2, -4.5f, -4.4f, 1, 1, 1);
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm, leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(arm, robe, -2, -2, -2, 4, 9, 4, .1f);
            rig.box(arm, skin, -2, 7, -2, 4, 3, 4);
            rig.box(leg, under, -2, 0, -2, 4, 10, 4);
            rig.box(leg, robe, -2, 9, -2, 4, 3, 4, .2f);
        }
        PartDefinition w = rig.weapon(rig.rHand, "katana");
        rig.box(w, steel, -0.4f, -2.5f, -18, 1, 1, 16); rig.box(w, Mat.of(0xc8a030), -1.5f, -3, -3, 3, 2, 1); rig.box(w, red, -0.5f, -2.5f, -2, 1, 1, 5);
    }
}
