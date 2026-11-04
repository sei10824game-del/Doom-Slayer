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

/** The Penitent One, one kit per game. */
public abstract class BlasphemousKits extends SlayerKit {
    static final int BLOOD = 0xa01020, GOLD = 0xf0c060, HOLY = 0xfff0b0;
    static final ProjStyle LIGHT = ProjStyle.make().color(HOLY).size(0.5f).life(26).pierce(5).trail(ParticleTypes.END_ROD);
    static final ProjStyle BLOODWAVE = ProjStyle.make().color(BLOOD).size(0.7f).life(24).pierce(4).trail(ParticleTypes.DAMAGE_INDICATOR);

    BlasphemousKits(String id, String name, String ja, String game, double hp) {
        super(id, name, ja, game);
        eggPrimary = 0x201814; eggSecondary = BLOOD;
        maxHealth = hp; speed = 0.3; armor = 9; knockbackRes = 0.5;
        scale = 1.03f; preferredRange = 3.5f; dmgMult = 1.1f;
    }

    @Override public void idle(Pose p, SlayerEntity e, float age, float m) { p.rArmX = -0.35f + p.rArmX * 0.5f; p.rArmZ = 0.12f; p.rHandX = 0.6f; p.headX += 0.12f; p.bodyX += 0.08f; }

    @Override
    public void passive(SlayerEntity e) { // Bile Flask
        if (e.tickCount % 300 == 0 && e.getHealth() < e.getMaxHealth() * 0.45f) {
            e.heal(e.getMaxHealth() * 0.35f); snd(e, SoundEvents.GENERIC_DRINK, 0.9f, 0.7f);
            Fx.dustBurst(KU.L(e), GOLD, 1f, e.position().add(0, 1.2, 0), 14, 0.4);
        }
    }

    /** shared: finishing move on a weakened enemy */
    static AttackDef execution(String weapon) {
        return AttackDef.of("execution").range(0, 3.4f).lowHealthTarget(0.3f).cd(40).weight(5).dur(28).weapon(weapon)
                .tick((e, t, n) -> {
                    if (n == 2) Combat.lunge(e, 0.8, 0);
                    if (n == 14 && t != null) {
                        Fx.dustBurst(KU.L(e), BLOOD, 1.6f, mid(t), 36, 0.45); slash(e, 90, -90, 30, BLOOD); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 0.5f);
                        Combat.damage(e, t, t.getHealth() + 60);
                    }
                }).anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -0.3f, .3f, -3.0f, .5f, -3.0f, .55f, -0.5f, .8f, -0.7f, 1, -0.3f); p.lArmX = track(t, 0, 0, .3f, -2.8f, .5f, -2.8f, .55f, -0.4f, 1, 0);
                    p.bodyX = track(t, 0, 0, .45f, -0.3f, .55f, 0.7f, .8f, 0.4f, 1, 0); p.headX = -p.bodyX * 0.4f; p.rootY = track(t, 0, 0, .5f, 0.4f, .55f, -1.8f, 1, 0); p.rootZ = -2f * bump(win(t, .4f, .8f));
                });
    }

    // ================= model (shared, per-game accents) =================
    @Override
    public void buildModel(Rig rig) {
        Mat cloth = Mat.of(0x1c1816).noise(.12f), armor = Mat.of(0x3a342e).bevel(.25f).noise(.1f), bone = Mat.of(0xd8cdb8).noise(.08f), blood = Mat.of(BLOOD).noise(.1f);
        Mat gold = Mat.of(GOLD).bevel(.2f), dark = Mat.of(0x0a0808), gem = Mat.of(HOLY).glow();
        rig.box(rig.torso, cloth, -4, -12, -2, 8, 12, 4, .1f);
        rig.box(rig.torso, armor, -4.3f, -12, -2.4f, 9, 6, 5, .2f);
        rig.box(rig.torso, blood, -4, -4, -2.5f, 8, 5, 5, .1f); // tattered skirt
        rig.box(rig.torso, gold, -1, -10, -3.5f, 2, 2, 1);
        rig.box(rig.head, dark, -4, -8, -4, 8, 8, 8);
        rig.box(rig.head, bone, -4.5f, -9, -4.5f, 9, 4, 9, .1f); // capirote base
        rig.box(rig.head, bone, -3, -13, -3, 6, 4, 6);
        rig.box(rig.head, bone, -2, -17, -2, 4, 4, 4);
        rig.box(rig.head, bone, -1, -20, -1, 2, 3, 2);
        rig.box(rig.head, gem, -3, -5, -4.4f, 6, 1, 1); // eyes in the dark face
        rig.box(rig.head, gold, -1.5f, -5, -4.7f, 3, 1, 1);
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm, leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(arm, cloth, -2, -2, -2, 4, 10, 4, .1f);
            rig.box(arm, armor, -2.5f, -3, -2.5f, 5, 4, 5, .1f);
            rig.box(arm, dark, -2, 7, -2, 4, 3, 4, .2f);
            rig.box(leg, cloth, -2, 0, -2, 4, 11, 4);
            rig.box(leg, armor, -2.2f, 8, -2.2f, 4, 4, 4, .3f);
        }
        weaponModels(rig);
    }

    abstract void weaponModels(Rig rig);

    // ================= Blasphemous 1 =================
    public static class Blasphemous1 extends BlasphemousKits {
        public Blasphemous1() { super("blasphemous_penitent", "The Penitent One", "悔悛者", "Blasphemous", 105); idleWeaponR = "mea_culpa"; }

        @Override
        protected void defineAttacks(List<AttackDef> out) {
            out.add(AttackDef.of("mea_culpa_combo").range(0, 3.4f).cd(16).weight(3).dur(18).weapon("mea_culpa")
                    .tick((e, t, n) -> { if (n == 6) { slash(e, 80, -70, 15, BLOOD); melee(e, 3.5, 8, 0.6, 0.1, BLOOD); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.9f); } })
                    .anim((p, t, a) -> swing(p, t, 6 / 18f, 1.2f)));
            out.add(AttackDef.of("upward_slash").range(0, 3.4f).cd(40).weight(2).dur(20).weapon("mea_culpa")
                    .tick((e, t, n) -> { if (n == 7) { slash(e, 0, 0, -80, BLOOD); for (LivingEntity f : Combat.cone(e, 3.4, 60)) { Combat.damage(e, f, 10); f.setDeltaMovement(0, 0.7, 0); f.hurtMarked = true; } e.setDeltaMovement(0, 0.35, 0); e.hurtMarked = true; snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.8f); } })
                    .anim((p, t, a) -> { float k = win(t, .1f, .4f) * (1 - win(t, .55f, .8f)); p.rArmX = lerp(p.rArmX, -3.0f, k); p.bodyX -= 0.3f * k; p.rootY -= 1f * win(t, 0, .3f) * (1 - win(t, .3f, .4f)); }));
            // Plunge from above
            out.add(AttackDef.of("plunge").range(0, 6).cd(120).weight(1).dur(30).weapon("mea_culpa")
                    .tick((e, t, n) -> {
                        if (n == 2) { e.setDeltaMovement(0, 0.85, 0); e.hurtMarked = true; }
                        if (n == 12) { e.setDeltaMovement(0, -1.7, 0); e.hurtMarked = true; }
                        if (n == 18) { Combat.blast(e, e.position(), 4.5f, 14, 1.0f, e); Fx.ring(KU.L(e), e.position().add(0, 0.2, 0), 3.5f, BLOOD, 1.6f, 26); snd(e, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.2f); }
                    }).anim((p, t, a) -> { float k = bump(win(t, .1f, .6f)); p.rArmX = -2.9f * k; p.lArmX = -2.9f * k; p.lLegX = 0.3f * k; p.bodyX -= 0.2f * k; }));
            // Dash slash
            out.add(AttackDef.of("slide_slash").range(3, 10).cd(80).weight(2).dur(20).weapon("mea_culpa")
                    .tick((e, t, n) -> { if (n == 3) Combat.lunge(e, 1.5, 0); if (n == 8) { slash(e, 90, -90, 0, BLOOD); melee(e, 3.6, 10, 0.9, 0.15, BLOOD); } })
                    .anim((p, t, a) -> { float k = bump(win(t, .1f, .8f)); p.rArmX = -1.5f * k; p.bodyX += 0.9f * k; p.rootY -= 1.8f * k; p.lLegX = -1.2f * k; p.rLegX = 0.5f * k; }));
            // Prayers
            out.add(AttackDef.of("debla_of_the_lights").range(3, 16).cd(150).weight(1).dur(34)
                    .tick((e, t, n) -> {
                        if (n < 14) Fx.dustBurst(KU.L(e), HOLY, 1f, e.position().add(0, 2.2, 0), 3, 0.4);
                        if (n == 16) { Vec3 m = mz(e), d = aim(e, t, 0); Combat.hitscan(e, m, d, 20, 18f, 6, HOLY); Fx.beam(KU.L(e), m, m.add(d.scale(18)), HOLY, 1.2f, 0.4); snd(e, SoundEvents.BEACON_POWER_SELECT, 1f, 1.4f); }
                    }).anim((p, t, a) -> { float k = KU.ready(t, .4f, .85f); p.rArmX = lerp(p.rArmX, -2.6f, k); p.lArmX = lerp(p.lArmX, -2.6f, k); p.headX -= 0.4f * k; }));
            out.add(AttackDef.of("lorquiana").range(2, 10).cd(140).weight(1).dur(30)
                    .tick((e, t, n) -> { if (n == 12) { Combat.shoot(e, BLOODWAVE, mz(e), aim(e, t, 0), 1.2, 12f); Fx.ring(KU.L(e), e.position().add(0, 0.3, 0), 2.5f, BLOOD, 1.5f, 20); snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.6f); } })
                    .anim((p, t, a) -> { float k = KU.ready(t, .35f, .8f); p.rArmX = lerp(p.rArmX, -1.55f, k); p.lArmX = lerp(p.lArmX, -1.55f, k); p.rootY -= 0.8f * k; p.rootZ += 1.5f * kick(t, 12 / 30f, .3f); }));
            out.add(AttackDef.of("verdiales_parry").range(0, 5).cd(160).weight(1).dur(20)
                    .tick((e, t, n) -> { if (n == 1) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 36, 4)); if (n == 6) { Combat.blast(e, e.position(), 3.5f, 6, 1.0f, e); Fx.ring(KU.L(e), e.position().add(0, 1, 0), 2.5f, GOLD, 1.5f, 22); snd(e, SoundEvents.ANVIL_LAND, 0.6f, 1.8f); } })
                    .anim((p, t, a) -> { float k = bump(win(t, .1f, .8f)); p.rArmX = -1.0f * k; p.rArmZ = 0.6f * k; p.bodyX -= 0.15f * k; p.lArmX = -0.6f * k; }));
            out.add(execution("mea_culpa"));
        }

        @Override
        void weaponModels(Rig rig) {
            PartDefinition w = rig.weapon(rig.rHand, "mea_culpa");
            rig.box(w, Mat.of(0x2a2020).bevel(.1f), -1, -3, -17, 2, 2, 15); rig.box(w, Mat.of(BLOOD).glow(), -0.5f, -2.5f, -17, 1, 1, 15);
            rig.box(w, Mat.of(GOLD), -3, -3.5f, -3, 6, 2, 1); rig.box(w, Mat.of(0x2a1a14), -0.5f, -3, -2, 1, 1, 5);
        }
    }

    // ================= Blasphemous 2 =================
    public static class Blasphemous2 extends BlasphemousKits {
        public Blasphemous2() { super("blasphemous2_penitent", "The Penitent One (II)", "悔悛者 (II)", "Blasphemous 2", 115); idleWeaponR = "ruego"; }

        @Override
        protected void defineAttacks(List<AttackDef> out) {
            // Ruego al Alba: sword
            out.add(AttackDef.of("ruego_combo").range(0, 3.3f).cd(14).weight(3).dur(16).weapon("ruego")
                    .tick((e, t, n) -> { if (n == 5) { slash(e, 75, -65, 10, HOLY); melee(e, 3.4, 7, 0.5, 0.1, HOLY); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.3f); } })
                    .anim((p, t, a) -> swing(p, t, 5 / 16f, 1f)));
            out.add(AttackDef.of("ruego_dawn_wave").range(3, 14).cd(100).weight(2).dur(26).weapon("ruego")
                    .tick((e, t, n) -> { if (n == 10) { Combat.shoot(e, LIGHT, mz(e), aim(e, t, 0), 1.4, 11f); slash(e, 90, -90, 40, HOLY); snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.8f); } })
                    .anim((p, t, a) -> swing(p, t, 10 / 26f, 1.3f)));
            // Veredicto: censer flail
            out.add(AttackDef.of("veredicto_swing").range(0, 4.2f).cd(40).weight(2).dur(30).weapon("veredicto")
                    .tick((e, t, n) -> { if (n >= 8 && n < 24 && n % 5 == 0) { slash(e, 359, 0, 0, BLOOD); melee(e, 4.2, 7, 0.8, 0.1, BLOOD); snd(e, SoundEvents.CHAIN_BREAK, 0.8f, 0.7f); } })
                    .anim((p, t, a) -> { p.rootYaw += 6.28f * 2 * win(t, .2f, .8f); p.rArmZ = 1.4f * bump(win(t, .1f, .9f)); p.rArmX = -0.5f; p.rootY -= 0.4f * bump(win(t, .1f, .9f)); }));
            out.add(AttackDef.of("veredicto_slam").range(0, 4f).cd(110).weight(1).dur(36).weapon("veredicto")
                    .tick((e, t, n) -> {
                        if (n < 16) Fx.burst(KU.L(e), ParticleTypes.FLAME, e.position().add(0, 2.2, 0), 2, 0.3, 0.02);
                        if (n == 20) { Combat.blast(e, e.position().add(e.getViewVector(1f).scale(2)), 4f, 20, 1.5f, e); Fx.explosion(KU.L(e), e.position().add(e.getViewVector(1f).scale(2.2)).add(0, 0.2, 0), 2.5f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.0f); }
                    }).anim((p, t, a) -> {
                        p.rArmX = track(t, 0, -0.3f, .45f, -3.0f, .58f, -0.6f, .8f, -0.7f, 1, -0.3f); p.lArmX = track(t, 0, 0, .45f, -2.8f, .58f, -0.5f, 1, 0);
                        p.bodyX = track(t, 0, 0, .45f, -0.35f, .58f, 0.7f, 1, 0); p.rootY = track(t, 0, 0, .5f, 0.5f, .58f, -1.5f, 1, 0); p.rootZ = -2f * bump(win(t, .45f, .8f));
                    }));
            // Sarmiento & Centella: dual rapiers
            out.add(AttackDef.of("rapier_flurry").range(0, 3.4f).cd(50).weight(3).dur(30).weapons("sarmiento", "centella")
                    .tick((e, t, n) -> { if (n >= 4 && n < 26 && n % 3 == 0) { melee(e, 3.4, 3.5f, 0.15, 0.05, 0xffffff); Fx.sparks(KU.L(e), e.position().add(e.getViewVector(1f).scale(2)).add(0, 1.2, 0), 3); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.9f); } })
                    .anim((p, t, a) -> { float s = (float) Math.sin(t * PI * 6f); p.rArmX = -1.55f + 0.2f * s; p.lArmX = -1.55f - 0.2f * s; p.rArmZ = 0.2f * s; p.lArmZ = -0.2f * s; p.bodyY = 0.2f * s; p.rootZ = -1f; p.lLegX = -0.4f; p.rLegX = 0.3f; }));
            out.add(AttackDef.of("rapier_lunge").range(3, 10).cd(70).weight(2).dur(20).weapons("sarmiento", "centella")
                    .tick((e, t, n) -> { if (n == 4) Combat.lunge(e, 1.7, 0.02); if (n == 8) { melee(e, 3.8, 12, 0.9, 0.15, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1.4f); } })
                    .anim((p, t, a) -> { float k = bump(win(t, .1f, .8f)); p.rArmX = -1.55f * k; p.lArmX = -1.3f * k; p.bodyX += 0.7f * k; p.rootY -= 1.4f * k; p.lLegX = -1f * k; p.rLegX = 0.8f * k; }));
            // Prayer-like: blood miracle
            out.add(AttackDef.of("blood_miracle").range(2, 10).cd(160).weight(1).dur(34)
                    .tick((e, t, n) -> {
                        if (n >= 8 && n < 26 && n % 6 == 0 && t != null) { Vec3 b = t.position(); Fx.dustBurst(KU.L(e), BLOOD, 1.5f, b.add(0, 0.3, 0), 14, 0.6); for (LivingEntity f : Combat.around(e, b, 2.5)) Combat.damage(e, f, 5); snd(e, SoundEvents.WITHER_HURT, 0.5f, 0.8f); }
                    }).anim((p, t, a) -> { float k = KU.ready(t, .25f, .85f); p.rArmX = lerp(p.rArmX, -2.4f, k); p.lArmX = lerp(p.lArmX, -2.4f, k); p.headX -= 0.3f * k; }));
            out.add(execution("ruego"));
        }

        @Override
        void weaponModels(Rig rig) {
            PartDefinition w = rig.weapon(rig.rHand, "ruego");
            rig.box(w, Mat.of(0xe8e0d0).bevel(.1f), -1, -3, -18, 2, 2, 16); rig.box(w, Mat.of(HOLY).glow(), -0.5f, -2.5f, -18, 1, 1, 14); rig.box(w, Mat.of(GOLD), -3, -3.5f, -3, 6, 2, 1);
            w = rig.weapon(rig.rHand, "veredicto");
            rig.box(w, Mat.of(0x2a2020), -0.5f, -2, -9, 1, 1, 10); rig.box(w, Mat.of(0x3a3430).bevel(.3f), -2.5f, -4.5f, -15, 5, 5, 5); rig.box(w, Mat.of(0xff6020).glow(), -1, -3, -16, 2, 2, 1);
            w = rig.weapon(rig.rHand, "sarmiento");
            rig.box(w, Mat.of(0xd8d8e0).bevel(.1f), -0.4f, -2.5f, -17, 1, 1, 15); rig.box(w, Mat.of(GOLD), -2, -3, -3, 4, 2, 1);
            w = rig.weapon(rig.lHand, "centella");
            rig.box(w, Mat.of(0xd8d8e0).bevel(.1f), -0.4f, -2.5f, -17, 1, 1, 15); rig.box(w, Mat.of(BLOOD), -2, -3, -3, 4, 2, 1);
        }
    }
}
