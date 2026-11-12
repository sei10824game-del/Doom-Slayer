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
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Siffrin (In Stars and Time): stuck in a time loop. Dying rewinds him to full health, several times. */
public class Siffrin extends SlayerKit {
    static final int LOOPS = 6, STAR = 0xffe070, PURPLE = 0xa070ff;
    static final ProjStyle BOLT = ProjStyle.make().color(PURPLE).size(0.35f).life(50).homing(0.12f).trail(ParticleTypes.ENCHANT);
    static final ProjStyle SHARD = ProjStyle.make().color(STAR).size(0.2f).life(35).pierce(2).trail(ParticleTypes.END_ROD);

    public Siffrin() {
        super("siffrin", "Siffrin", "シフリン", "In Stars and Time");
        eggPrimary = 0xf0e8f8; eggSecondary = PURPLE;
        maxHealth = 75; speed = 0.33; armor = 5; knockbackRes = 0.2;
        scale = 0.95f; preferredRange = 4f; dmgMult = 1.0f;
        idleWeaponR = "knife";
    }

    @Override
    public boolean preventDeath(SlayerEntity e) { // LOOP: rewind
        var d = e.getPersistentData();
        int used = d.getInt("ds_loop");
        if (used >= LOOPS) return false;
        d.putInt("ds_loop", used + 1);
        e.setHealth(e.getMaxHealth());
        for (LivingEntity f : Combat.around(e, e.position(), 8)) f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5));
        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4));
        Fx.ring(KU.L(e), e.position().add(0, 1, 0), 4f, STAR, 1.6f, 30); Fx.burst(KU.L(e), ParticleTypes.ENCHANT, e.position().add(0, 1, 0), 40, 1, 0.3);
        snd(e, SoundEvents.BEACON_DEACTIVATE, 1f, 0.5f); snd(e, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 0.7f);
        if (e.getOwner() != null) e.getOwner().displayClientMessage(Component.literal("Siffrin: ...Loop again. (" + (LOOPS - used - 1) + " left)"), true);
        return true;
    }

    @Override public void idle(Pose p, SlayerEntity e, float age, float m) { p.rArmX = -0.3f + p.rArmX * 0.5f; p.rArmZ = 0.1f; p.rHandX = 0.5f; p.headX += 0.05f; p.bodyX += 0.04f; }

    @Override
    protected void defineAttacks(List<AttackDef> out) {
        defineBase(out);
        for (AttackDef a : out) if (a.id.equals("knife_slash")) a.anim = (p, t, ag) -> KU.strikePose(p, KU.remap(t, 5 / 14f), 0, 1f);
    }

    private void defineBase(List<AttackDef> out) {
        out.add(AttackDef.of("knife_slash").range(0, 3f).cd(12).weight(3).dur(14).weapon("knife")
                .tick((e, t, n) -> { if (n == 5) { slash(e, 65, -55, 10, 0xffffff); melee(e, 3.1, 6, 0.4, 0.1, STAR); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.5f); } })
                .anim((p, t, a) -> swing(p, t, 5 / 14f, 1f)));
        out.add(AttackDef.of("knife_stab").range(0, 3.2f).cd(26).weight(2).dur(18).weapon("knife")
                .tick((e, t, n) -> { if (n == 2) Combat.lunge(e, 0.5, 0.02); if (n == 7) { melee(e, 3.3, 10, 0.5, 0.15, STAR); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 1.3f); } })
                .anim((p, t, a) -> stab(p, t, 7 / 18f)));

        // Time stop: all nearby foes frozen briefly, free hits
        out.add(AttackDef.of("time_stop").range(0, 8).cd(240).weight(1).dur(40).weapon("knife")
                .tick((e, t, n) -> {
                    if (n == 2) { for (LivingEntity f : Combat.around(e, e.position(), 7)) f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 7)); Fx.ring(KU.L(e), e.position().add(0, 1, 0), 5f, STAR, 1.6f, 30); snd(e, SoundEvents.BEACON_ACTIVATE, 0.9f, 0.5f); }
                    if (n >= 10 && n < 36 && n % 4 == 0) {
                        for (LivingEntity f : Combat.around(e, e.position(), 6)) { Combat.damage(e, f, 4); Fx.beam(KU.L(e), e.position().add(0, 1, 0), mid(f), STAR, 0.4f, 0.3); }
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 1.7f);
                    }
                }).anim((p, t, a) -> { float s = (float) Math.sin(t * PI * 5f); p.rArmX = -1.4f + 0.5f * Math.abs(s); p.rArmZ = 0.8f * s; p.lArmX = -1.2f; p.bodyY = 0.6f * s; }));

        // Memory bolt (homing)
        out.add(AttackDef.of("memory_bolt").range(4, 18).cd(60).weight(2).dur(20)
                .tick((e, t, n) -> { if (n == 7) { Combat.shoot(e, BOLT, mz(e), aim(e, t, 0.02), 1.0, 8f); snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.2f); } })
                .anim((p, t, a) -> { float k = KU.ready(t, .25f, .85f); p.rArmX = lerp(p.rArmX, -1.55f, k); p.lArmX = lerp(p.lArmX, -1.2f, k); p.lArmZ = lerp(p.lArmZ, -0.3f, k); p.rootZ += kick(t, 7 / 20f, .3f); }));

        // Star shards fan
        out.add(AttackDef.of("star_shards").range(3, 14).cd(90).weight(2).dur(26)
                .tick((e, t, n) -> { if (n == 9) { Vec3 d = aim(e, t, 0); for (int i = -2; i <= 2; i++) Combat.shoot(e, SHARD, mz(e), new Vec3(d.x - d.z * 0.18 * i, d.y, d.z + d.x * 0.18 * i).normalize(), 1.4, 5f); snd(e, SoundEvents.AMETHYST_BLOCK_BREAK, 1f, 1.6f); } })
                .anim((p, t, a) -> { float k = bump(win(t, .1f, .85f)); p.rArmZ = 1.2f * k; p.lArmZ = -1.2f * k; p.rArmX -= 0.8f * k; p.lArmX -= 0.8f * k; p.headX -= 0.2f * k; }));

        // Rewind step: blink back and strike
        out.add(AttackDef.of("rewind_strike").range(2, 9).cd(110).weight(2).dur(22).weapon("knife")
                .tick((e, t, n) -> {
                    if (n == 2) { Vec3 b = e.getViewVector(1f).scale(-2.5); e.setDeltaMovement(b.x * 0.5, 0.1, b.z * 0.5); e.hurtMarked = true; Fx.dustBurst(KU.L(e), PURPLE, 1.2f, e.position().add(0, 1, 0), 10, 0.4); snd(e, SoundEvents.ENDERMAN_TELEPORT, 0.7f, 1.4f); }
                    if (n == 8) { Combat.lunge(e, 1.7, 0.02); Fx.dustBurst(KU.L(e), STAR, 1f, e.position().add(0, 1, 0), 8, 0.3); }
                    if (n == 12) { slash(e, 80, -80, 0, STAR); melee(e, 3.6, 13, 0.9, 0.2, STAR); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1.2f); }
                }).anim((p, t, a) -> { float k = bump(win(t, .08f, .75f)); p.rArmX = lerp(p.rArmX, -1.5f, win(t, .4f, .55f)); p.bodyX += 0.7f * win(t, .35f, .6f) - 0.3f * win(t, .05f, .3f); p.rootY -= 1f * k; p.lLegX = -0.8f * k; p.rLegX = 0.6f * k; }));

        // Loop burst: collapse space around him
        out.add(AttackDef.of("loop_burst").range(0, 5).cd(200).weight(1).dur(34)
                .tick((e, t, n) -> {
                    if (n < 16) Fx.dustBurst(KU.L(e), PURPLE, 1.2f, e.position().add(0, 1.1, 0), 4, 2.5 - n * 0.15);
                    if (n == 18) { Combat.blast(e, e.position(), 5.5f, 14, 1.3f, e); for (int i = 1; i <= 3; i++) Fx.ring(KU.L(e), e.position().add(0, 0.3, 0), i * 1.8f, STAR, 1.5f, 28); snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.7f, 1.5f); }
                }).anim((p, t, a) -> { float k = KU.ready(t, .45f, .8f); p.rArmX = lerp(p.rArmX, -2.9f, k); p.lArmX = lerp(p.lArmX, -2.9f, k); p.rArmZ += 0.4f * k; p.lArmZ -= 0.4f * k; p.rootY -= 0.8f * k; }));
    }

    @Override
    public void buildModel(Rig rig) {
        Mat coat = Mat.of(0xf0e8f4).noise(.05f), purple = Mat.of(0x5a3a8a).noise(.08f), skin = Mat.of(0xf0d8c8).noise(.03f).bevel(.1f), hair = Mat.of(0xe8e8f4).noise(.08f);
        Mat dark = Mat.of(0x2a2438), gold = Mat.of(STAR).glow(), eye = Mat.of(0x303060);
        rig.box(rig.torso, purple, -4, -12, -2, 8, 12, 4, .1f);
        rig.box(rig.torso, coat, -4.5f, -12, -2.5f, 9, 8, 5, .2f);
        rig.box(rig.torso, gold, -0.5f, -9, -3.2f, 1, 1, 1);
        rig.box(rig.head, skin, -4, -8, -4, 8, 8, 8);
        rig.box(rig.head, hair, -4, -8, -4, 8, 3, 8, .3f);
        rig.box(rig.head, hair, -4, -8, 3, 8, 7, 1, .3f);
        rig.box(rig.head, eye, -3, -4.5f, -4.2f, 2, 2, 1); rig.box(rig.head, eye, 1, -4.5f, -4.2f, 2, 2, 1);
        rig.box(rig.head, coat, -5, -11, -5, 10, 2, 10); // wide hat brim
        rig.box(rig.head, coat, -3, -14, -3, 6, 3, 6);
        rig.box(rig.head, gold, -3, -11.5f, -5.3f, 6, 1, 1);
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm, leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(arm, coat, -2, -2, -2, 4, 9, 4, .2f);
            rig.box(arm, skin, -2, 7, -2, 4, 3, 4);
            rig.box(leg, purple, -2, 0, -2, 4, 10, 4);
            rig.box(leg, dark, -2, 9, -2, 4, 3, 4, .2f);
        }
        PartDefinition w = rig.weapon(rig.rHand, "knife");
        rig.box(w, Mat.of(0xe0e0f0).bevel(.1f), -0.5f, -2.5f, -9, 1, 2, 8); rig.box(w, dark, -1, -2, -1, 2, 2, 4);
    }
}
