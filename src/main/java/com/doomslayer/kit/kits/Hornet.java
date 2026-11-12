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

/** Hornet (Hollow Knight: Silksong): Needle, silk skills, thread. */
public class Hornet extends SlayerKit {
    static final int SILK = 0xffe0e8, RED = 0xc01830;
    static final ProjStyle NEEDLE = ProjStyle.make().color(0xe0e0e8).size(0.2f).life(30).pierce(2).trail(ParticleTypes.CRIT);
    static final ProjStyle SHARPDART = ProjStyle.make().color(RED).size(0.2f).life(30).pierce(1).trail(ParticleTypes.CRIT);
    static final ProjStyle RUNE = ProjStyle.make().color(0xff5060).size(0.7f).life(28).pierce(6).trail(ParticleTypes.DAMAGE_INDICATOR);

    public Hornet() {
        super("hornet", "Hornet", "ホーネット", "Hollow Knight: Silksong");
        eggPrimary = RED; eggSecondary = 0x201020;
        maxHealth = 70; speed = 0.4; armor = 5; knockbackRes = 0.2;
        scale = 0.95f; preferredRange = 4f; dmgMult = 1.15f;
        idleWeaponR = "needle";
    }

    @Override public void idle(Pose p, SlayerEntity e, float age, float m) { p.rArmX = -0.4f + p.rArmX * 0.5f; p.rArmZ = 0.15f; p.rHandX = 0.5f; p.bodyX += 0.04f; }

    /** SILK 0..9: +1 per needle hit, spent by silk skills and Bind */
    static int silk(SlayerEntity e) { return e.getPersistentData().getInt("hn_silk"); }
    static void silk(SlayerEntity e, int add) { if (add != 0) e.getPersistentData().putInt("hn_silk", Math.max(0, Math.min(9, silk(e) + add))); }

    @Override
    public void passive(SlayerEntity e) { // Bind: 9 silk -> heal
        if (e.tickCount % 60 == 0 && e.getHealth() < e.getMaxHealth() * 0.6f && silk(e) >= 9) {
            silk(e, -9); e.heal(e.getMaxHealth() * 0.3f); Fx.dustBurst(KU.L(e), SILK, 1f, e.position().add(0, 1, 0), 14, 0.5); snd(e, SoundEvents.WOOL_PLACE, 0.8f, 1.6f);
        }
    }

    @Override
    protected void defineAttacks(List<AttackDef> out) {
        defineBase(out);
        for (AttackDef a : out) if (a.id.equals("cross_stitch")) a.anim = KU.comboAnim(new int[] {8, 13}, 26, new int[] {0, 1});
        for (AttackDef a : out) if (a.id.equals("needle_slash")) a.anim = (p, t, ag) -> KU.strikePose(p, KU.remap(t, 4 / 12f), 0, 1f);
    }

    private void defineBase(List<AttackDef> out) {
        defineAll(out);
        java.util.Map<String, Integer> cost = java.util.Map.of("silkspear", 4, "sharpdart", 4, "thread_storm", 6, "cross_stitch", 5, "rune_rage", 6);
        for (AttackDef a : out) { Integer c = cost.get(a.id); if (c != null) { a.when((e, t) -> silk(e) >= c); a.onStart((e, t, n) -> silk(e, -c)); } }
    }

    private void defineAll(List<AttackDef> out) {
        out.add(AttackDef.of("needle_slash").range(0, 3.4f).cd(12).weight(3).dur(12).weapon("needle")
                .tick((e, t, n) -> { if (n == 4) { slash(e, 70, -60, 10, 0xffffff); silk(e, melee(e, 3.5, 6, 0.4, 0.1, RED)); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.8f); } })
                .anim((p, t, a) -> swing(p, t, 4 / 12f, 1f)));

        // Needle throw (tethered; goes out and returns)
        out.add(AttackDef.of("needle_throw").range(4, 16).cd(50).weight(2).dur(18).weapon("needle")
                .tick((e, t, n) -> { if (n == 6) { Combat.shoot(e, NEEDLE, mz(e), aim(e, t, 0), 1.8, 7f); snd(e, SoundEvents.TRIDENT_THROW, 0.8f, 2f); } if (n == 6 && t != null) Fx.beam(KU.L(e), mz(e), mid(t), SILK, 0.2f, 0.4); })
                .anim((p, t, a) -> throwAnim(p, t)));

        // Silkspear: thrust forward
        out.add(AttackDef.of("silkspear").range(2, 10).cd(80).weight(2).dur(22).weapon("needle")
                .tick((e, t, n) -> {
                    if (n == 7) {
                        Vec3 d = aim(e, t, 0), m = mz(e); Combat.hitscan(e, m, d, 10, 12f, 4, RED); Combat.lunge(e, 0.5, 0);
                        for (int i = 1; i <= 8; i++) Fx.dustBurst(KU.L(e), RED, 1f, m.add(d.scale(i * 1.1)), 2, 0.1);
                        snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1.6f);
                    }
                }).anim((p, t, a) -> stab(p, t, 7 / 22f)));

        // Sharpdart: dash-stab
        out.add(AttackDef.of("sharpdart").range(3, 12).cd(80).weight(2).dur(18).weapon("needle")
                .tick((e, t, n) -> {
                    if (n == 3) { Combat.lunge(e, 1.9, 0.02); snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.5f, 2f); }
                    if (n >= 3 && n < 9) Fx.dustBurst(KU.L(e), RED, 1f, e.position().add(0, 1, 0), 3, 0.2);
                    if (n == 8) silk(e, melee(e, 3.6, 12, 0.8, 0.15, RED));
                }).anim((p, t, a) -> { float k = bump(win(t, .1f, .8f)); p.rArmX = -1.55f * k; p.bodyX += 0.9f * k; p.lLegX = -1f * k; p.rLegX = 0.8f * k; p.rootY -= 1.4f * k; p.rootZ = -2f * win(t, .3f, .5f); }));

        // Thread Storm: spinning silk blades around Hornet
        out.add(AttackDef.of("thread_storm").range(0, 4f).cd(130).weight(1).dur(40).weapon("needle")
                .tick((e, t, n) -> {
                    if (n >= 6 && n < 34) {
                        double a = n * 0.9; Fx.beam(KU.L(e), e.position().add(0, 1.2, 0), e.position().add(Math.cos(a) * 3.2, 1.2, Math.sin(a) * 3.2), SILK, 0.3f, 0.3);
                        if (n % 4 == 0) { for (LivingEntity f : Combat.around(e, e.position(), 3.4)) { Combat.damage(e, f, 4); Fx.sparks(KU.L(e), mid(f), 3); } snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.8f); }
                    }
                }).anim((p, t, a) -> { p.rootYaw += 6.28f * 4 * win(t, .1f, .85f); p.rArmZ = 1.5f * bump(win(t, .1f, .85f)); p.rArmX = -0.3f; p.lArmZ = -1.5f * bump(win(t, .1f, .85f)); }));

        // Cross Stitch: X cut
        out.add(AttackDef.of("cross_stitch").range(0, 3.6f).cd(100).weight(2).dur(26).weapon("needle")
                .tick((e, t, n) -> { if (n == 8 || n == 13) { slash(e, n == 8 ? 80 : -80, n == 8 ? -80 : 80, n == 8 ? 45 : -45, SILK); silk(e, melee(e, 3.6, 8, 0.4, 0.15, SILK)); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.2f); } })
                .anim((p, t, a) -> { float s = (float) Math.sin(t * PI * 2f); p.rArmX = -1.5f; p.rArmZ = 1.0f * s; p.bodyY = 0.7f * s; p.rootZ = -1f * bump(win(t, .2f, .8f)); }));

        // Rune Rage: silk-wrap burst of red energy
        out.add(AttackDef.of("rune_rage").range(3, 14).cd(150).weight(1).dur(34)
                .tick((e, t, n) -> {
                    if (n < 16) Fx.dustBurst(KU.L(e), RED, 1.2f, e.position().add(0, 1.2, 0), 3, 0.5 - n * 0.02);
                    if (n == 18) { for (int i = -1; i <= 1; i++) Combat.shoot(e, RUNE, mz(e), aim(e, t, 0).add(-aim(e, t, 0).z * 0.2 * i, 0, aim(e, t, 0).x * 0.2 * i).normalize(), 1.3, 8f); snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.7f, 1.8f); }
                }).anim((p, t, a) -> { float k = KU.ready(t, .4f, .85f); p.rArmZ = 1.6f * k; p.lArmZ = -1.6f * k; p.rArmX -= 0.6f * k; p.lArmX -= 0.6f * k; p.rootY -= 0.6f * k; }));

        // Harpoon: grapple dash to the target
        out.add(AttackDef.of("harpoon").range(5, 18).cd(110).weight(2).dur(24).weapon("needle")
                .tick((e, t, n) -> {
                    if (n >= 2 && n < 8 && t != null) Fx.beam(KU.L(e), e.position().add(0, 1.3, 0), mid(t), SILK, 0.25f, 0.3);
                    if (n == 6 && t != null) { Vec3 d = t.position().subtract(e.position()).normalize(); e.setDeltaMovement(d.x * 1.6, 0.3, d.z * 1.6); e.hurtMarked = true; }
                    if (n == 13) { slash(e, 80, -80, 25, 0xffffff); silk(e, melee(e, 3.6, 10, 0.7, 0.25, 0xffffff)); }
                }).anim((p, t, a) -> { float k = bump(win(t, .05f, .45f)); p.lArmX = -2.2f * k; p.bodyX += 0.4f * win(t, .25f, .6f) * (1 - win(t, .6f, .8f)); p.rArmX = lerp(p.rArmX, -1.4f, win(t, .5f, .6f)); }));
    }

    @Override
    public void buildModel(Rig rig) {
        Mat red = Mat.of(RED).noise(.1f), dark = Mat.of(0x2a1830).bevel(.2f), shell = Mat.of(0xf4ecec).bevel(.25f), black = Mat.of(0x0a0a0e), silver = Mat.of(0xc8c8d0).bevel(.1f);
        rig.box(rig.torso, dark, -3.5f, -12, -2, 7, 12, 4);
        rig.box(rig.torso, red, -4, -11, -2.5f, 8, 9, 5, .3f); // cloak
        rig.box(rig.torso, red, -4, -3, -2.5f, 8, 6, 5, .1f); // skirt
        rig.box(rig.head, shell, -4, -8, -4, 8, 8, 8, .15f);
        rig.box(rig.head, black, -3, -5.5f, -4.5f, 2, 3, 1); rig.box(rig.head, black, 1, -5.5f, -4.5f, 2, 3, 1);
        rig.box(rig.head, shell, -4.5f, -13, -2, 2, 6, 2); rig.box(rig.head, shell, 2.5f, -13, -2, 2, 6, 2); // long horns
        rig.box(rig.head, red, -1, -4, -4.5f, 2, 1, 1);
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm, leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(arm, dark, -1.5f, -2, -1.5f, 3, 10, 3);
            rig.box(arm, red, -2, 6, -2, 4, 2, 4, .1f);
            rig.box(leg, dark, -1.5f, 0, -1.5f, 3, 12, 3);
            rig.box(leg, black, -2, 9, -2, 4, 3, 4, .1f);
        }
        PartDefinition w = rig.weapon(rig.rHand, "needle");
        rig.box(w, silver, -0.4f, -2.5f, -19, 1, 1, 17); rig.box(w, Mat.of(0xf0f0f4), -1, -3, -3, 2, 2, 2); rig.box(w, red, -0.5f, -2.5f, -1, 1, 1, 4);
    }
}
