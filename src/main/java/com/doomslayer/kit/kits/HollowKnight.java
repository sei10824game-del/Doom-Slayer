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

/** The Knight (Hollow Knight): Old Nail, Soul spells, nail arts, Focus. */
public class HollowKnight extends SlayerKit {
    static final int SOUL = 0xe8f4ff, SHADE = 0x101018;
    static final ProjStyle FIREBALL = ProjStyle.make().color(SOUL).size(0.6f).life(30).pierce(4).trail(ParticleTypes.END_ROD);
    static final ProjStyle SHADE_SOUL = ProjStyle.make().color(0xc0c8ff).size(0.9f).life(30).pierce(6).trail(ParticleTypes.END_ROD);

    public HollowKnight() {
        super("hollow_knight", "The Knight", "ナイト", "Hollow Knight");
        eggPrimary = 0x16161c; eggSecondary = SOUL;
        maxHealth = 50; speed = 0.36; armor = 4; knockbackRes = 0.1;
        scale = 0.85f; preferredRange = 3f; dmgMult = 1.1f;
        idleWeaponR = "nail";
    }

    @Override public void idle(Pose p, SlayerEntity e, float age, float m) { p.rArmX = -0.3f + p.rArmX * 0.5f; p.rArmZ = 0.1f; p.rHandX = 0.7f; p.bodyX += 0.05f; }

    @Override
    public void passive(SlayerEntity e) { // Focus: slow soul heal while safe
        if (e.tickCount % 120 == 0 && e.getHealth() < e.getMaxHealth() && Combat.around(e, e.position(), 7).isEmpty()) {
            e.heal(4); Fx.burst(KU.L(e), ParticleTypes.END_ROD, e.position().add(0, 1, 0), 10, 0.3, 0.05); snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 1.6f);
        }
    }

    @Override
    protected void defineAttacks(List<AttackDef> out) {
        out.add(AttackDef.of("nail_slash").range(0, 3.1f).cd(12).weight(3).dur(12).weapon("nail")
                .tick((e, t, n) -> { if (n == 4) { slash(e, 70, -60, 5, 0xffffff); melee(e, 3.2, 5, 0.4, 0.1, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.6f); } })
                .anim((p, t, a) -> swing(p, t, 4 / 12f, 1f)));

        out.add(AttackDef.of("up_slash").range(0, 3f).cd(30).weight(1).dur(14).weapon("nail")
                .tick((e, t, n) -> { if (n == 5) { slash(e, 0, 0, -80, 0xffffff); melee(e, 3.2, 5, 0.1, 0.5, 0xffffff); } })
                .anim((p, t, a) -> { float k = bump(win(t, .1f, .8f)); p.rArmX = -3.0f * k; p.bodyX -= 0.25f * k; p.headX -= 0.3f * k; }));

        // down slash / pogo: leap and bounce on the target
        out.add(AttackDef.of("pogo").range(1, 5).cd(70).weight(2).dur(24).weapon("nail")
                .tick((e, t, n) -> {
                    if (n == 2) { e.setDeltaMovement(e.getViewVector(1f).x * 0.4, 0.65, e.getViewVector(1f).z * 0.4); e.hurtMarked = true; }
                    if (n == 10) {
                        for (LivingEntity f : Combat.around(e, e.position(), 2.2)) { Combat.damage(e, f, 7); Fx.sparks(KU.L(e), mid(f), 6); }
                        e.setDeltaMovement(0, 0.5, 0); e.hurtMarked = true; snd(e, SoundEvents.ANVIL_LAND, 0.4f, 2f);
                    }
                }).anim((p, t, a) -> { float k = bump(win(t, .1f, .85f)); p.rArmX = 0.8f * k; p.rHandX = 1.5f * k; p.lLegX = -0.7f * k; p.rLegX = -0.4f * k; p.bodyX += 0.2f * k; }));

        // Dash Slash (nail art)
        out.add(AttackDef.of("dash_slash").range(3, 10).cd(90).weight(2).dur(22).weapon("nail")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.BEACON_ACTIVATE, 0.5f, 1.8f);
                    if (n == 10) { Combat.lunge(e, 1.6, 0.02); }
                    if (n >= 10 && n < 15) Fx.burst(KU.L(e), ParticleTypes.CLOUD, e.position().add(0, 0.5, 0), 3, 0.2, 0.02);
                    if (n == 13) { slash(e, 80, -80, 0, 0xffffff); melee(e, 3.8, 12, 1.0, 0.2, 0xffffff); }
                }).anim((p, t, a) -> {
                    float k = bump(win(t, .1f, .8f));
                    p.rArmX = lerp(p.rArmX, -1.5f, k); p.bodyX += 1f * k; p.rootY -= 1.2f * k; p.lLegX = -1f * k; p.rLegX = 0.9f * k; p.rootZ = -2f * win(t, .4f, .6f);
                }));

        // Great Slash
        out.add(AttackDef.of("great_slash").range(0, 4f).cd(110).weight(1).dur(30).weapon("nail")
                .tick((e, t, n) -> {
                    if (n < 14 && n % 4 == 0) Fx.dustBurst(KU.L(e), SOUL, 1f, e.position().add(0, 1.3, 0), 3, 0.5);
                    if (n == 16) { slash(e, 100, -100, 15, SOUL); melee(e, 4.2, 15, 1.2, 0.3, SOUL); Fx.ring(KU.L(e), e.position().add(0, 1, 0), 3f, SOUL, 1.5f, 20); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.7f); }
                }).anim((p, t, a) -> {
                    float w = win(t, .1f, .45f) * (1 - win(t, .5f, .55f));
                    p.rArmX = track(t, 0, -0.3f, .45f, -2.7f, .6f, -1.2f, 1, -0.3f); p.rArmZ = track(t, 0, 0.1f, .45f, 0.9f, .6f, -0.8f, 1, 0.1f);
                    p.bodyY = track(t, 0, 0, .45f, 0.8f, .6f, -0.8f, 1, 0); p.rootY -= 1f * w; p.rootZ = -1.5f * bump(win(t, .45f, .8f));
                }));

        // Cyclone Slash
        out.add(AttackDef.of("cyclone_slash").range(0, 3.6f).cd(120).weight(1).dur(36).weapon("nail")
                .tick((e, t, n) -> { if (n >= 6 && n < 30 && n % 4 == 0) { slash(e, 0, 359, 0, 0xffffff); melee(e, 3.6, 4, 0.2, 0.1, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 1.4f); } })
                .anim((p, t, a) -> { p.rootYaw += 6.28f * 3 * win(t, .1f, .85f); p.rArmZ = 1.5f * bump(win(t, .1f, .85f)); p.rArmX = -0.4f; }));

        out.add(AttackDef.of("vengeful_spirit").range(3, 18).cd(60).weight(2).dur(20)
                .tick((e, t, n) -> { if (n == 7) { Combat.shoot(e, FIREBALL, mz(e), aim(e, t, 0), 1.5, 8f); snd(e, SoundEvents.ALLAY_ITEM_GIVEN, 1f, 0.8f); } })
                .anim((p, t, a) -> { float k = KU.ready(t, .25f, .8f); p.rArmX = lerp(p.rArmX, -1.55f, k); p.lArmX = lerp(p.lArmX, -1.55f, k); p.rootZ += 1.2f * kick(t, 7 / 20f, .3f); }));
        out.add(AttackDef.of("shade_soul").range(3, 20).cd(130).weight(1).dur(24)
                .tick((e, t, n) -> { if (n == 9) { Combat.shoot(e, SHADE_SOUL, mz(e), aim(e, t, 0), 1.8, 16f); snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.8f); } })
                .anim((p, t, a) -> { float k = KU.ready(t, .3f, .8f); p.rArmX = lerp(p.rArmX, -1.55f, k); p.lArmX = lerp(p.lArmX, -1.55f, k); p.rootZ += 2f * kick(t, 9 / 24f, .3f); }));

        // Desolate Dive / Descending Dark
        out.add(AttackDef.of("desolate_dive").range(0, 8).cd(140).weight(1).dur(34)
                .tick((e, t, n) -> {
                    if (n == 3) { e.setDeltaMovement(0, 0.8, 0); e.hurtMarked = true; }
                    if (n == 14) { e.setDeltaMovement(0, -1.6, 0); e.hurtMarked = true; }
                    if (n == 20) {
                        Combat.blast(e, e.position(), 5f, 16, 1.2f, e); Fx.ring(KU.L(e), e.position().add(0, 0.2, 0), 4f, SOUL, 1.6f, 28);
                        Fx.burst(KU.L(e), ParticleTypes.END_ROD, e.position().add(0, 0.4, 0), 24, 1.2, 0.2); snd(e, SoundEvents.GENERIC_EXPLODE, 0.7f, 1.6f);
                    }
                }).anim((p, t, a) -> { float k = bump(win(t, .1f, .6f)); p.rArmX = -2.9f * k; p.lArmX = -2.9f * k; p.lLegX = 0.3f * k; p.bodyX -= 0.2f * k; }));

        // Howling Wraiths
        out.add(AttackDef.of("howling_wraiths").range(2, 10).cd(120).weight(1).dur(26)
                .tick((e, t, n) -> {
                    if (n == 8) {
                        Vec3 m = mz(e), d = aim(e, t, 0.01);
                        Combat.hitscan(e, m, d, 10, 9f, 6, SOUL);
                        for (int i = 1; i <= 8; i++) Fx.dustBurst(KU.L(e), SOUL, 1.5f, m.add(d.scale(i)), 4, 0.4);
                        snd(e, SoundEvents.GHAST_SCREAM, 0.6f, 1.8f);
                    }
                }).anim((p, t, a) -> { float k = KU.ready(t, .25f, .8f); p.rArmX = lerp(p.rArmX, -3.0f, k); p.lArmX = lerp(p.lArmX, -3.0f, k); p.headX -= 0.3f * k; }));

        // Shade Cloak: dash through enemies, brief invulnerability
        out.add(AttackDef.of("shade_cloak").range(3, 10).cd(150).weight(1).dur(16)
                .tick((e, t, n) -> {
                    if (n == 2) { e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 14, 4)); Combat.lunge(e, 1.9, 0); snd(e, SoundEvents.ENDERMAN_TELEPORT, 0.7f, 1.4f); }
                    if (n >= 2 && n < 10) Fx.dustBurst(KU.L(e), SHADE, 1.4f, e.position().add(0, 1, 0), 6, 0.3);
                    if (n == 8) melee(e, 3.5, 8, 0.8, 0.1, SHADE);
                }).anim((p, t, a) -> { float k = bump(win(t, .1f, .85f)); p.bodyX += 1f * k; p.rArmX = -0.5f * k; p.lArmX = -0.5f * k; p.rootY -= 1.4f * k; }));
    }

    @Override
    public void buildModel(Rig rig) {
        Mat shell = Mat.of(0x1a1a22).bevel(.3f).noise(.05f), white = Mat.of(0xf0f0f4).bevel(.2f), cloak = Mat.of(0x2a2e5a).noise(.1f), black = Mat.of(0x050508);
        rig.box(rig.torso, black, -3, -12, -2, 6, 12, 4);
        rig.box(rig.torso, cloak, -3.5f, -12, -2.5f, 7, 9, 5, .2f); // short cloak
        // big round pale head with horns
        rig.box(rig.head, white, -4, -8, -4, 8, 8, 8, .2f);
        rig.box(rig.head, black, -3, -5.5f, -4.6f, 2, 3, 1); rig.box(rig.head, black, 1, -5.5f, -4.6f, 2, 3, 1);
        rig.box(rig.head, white, -4.5f, -12, -1, 2, 5, 2); rig.box(rig.head, white, 2.5f, -12, -1, 2, 5, 2);
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm, leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(arm, black, -1.5f, -2, -1.5f, 3, 10, 3);
            rig.box(leg, black, -1.5f, 0, -1.5f, 3, 12, 3);
            rig.box(leg, shell, -2, 9, -2, 4, 3, 4, .1f);
        }
        PartDefinition w = rig.weapon(rig.rHand, "nail");
        rig.box(w, Mat.of(0xe8e8ec).bevel(.1f), -0.5f, -2.5f, -13, 1, 1, 11); rig.box(w, Mat.of(0xc8c8d0), -1.5f, -3, -3, 3, 2, 1); rig.box(w, Mat.of(0x303038), -0.5f, -2.5f, -2, 1, 1, 4);
    }
}
