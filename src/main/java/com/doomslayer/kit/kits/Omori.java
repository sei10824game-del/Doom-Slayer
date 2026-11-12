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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** OMORI (OMORI): the monochrome boy with the kitchen knife. Survives exactly one lethal blow at 1 HP. */
public class Omori extends SlayerKit {
    static final ProjStyle KNIFE = ProjStyle.make().color(0xe8e8e8).size(0.2f).life(40).pierce(1).trail(ParticleTypes.CRIT);
    static final ProjStyle TEAR = ProjStyle.make().color(0x6aa0ff).size(0.22f).life(60).gravity(0.02).trail(ParticleTypes.DRIPPING_WATER);
    static final ProjStyle HEART = ProjStyle.make().color(0xff4060).size(0.3f).life(70).homing(0.18f).explode(2.2f).trail(ParticleTypes.HEART);

    public Omori() {
        super("omori", "OMORI", "オモリ", "OMORI");
        eggPrimary = 0xf2f2f2; eggSecondary = 0x111111;
        maxHealth = 70; speed = 0.32; armor = 6; knockbackRes = 0.2;
        scale = 0.95f; preferredRange = 4f; dmgMult = 1.0f;
        idleWeaponR = "knife";
    }

    @Override
    public boolean preventDeath(SlayerEntity e) {
        var d = e.getPersistentData();
        if (d.getBoolean("ds_omori_used")) return false;
        d.putBoolean("ds_omori_used", true);
        e.setHealth(1f);
        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4));
        Fx.ring(L(e), e.position().add(0, 1, 0), 2f, 0xffffff, 1.6f, 24); Fx.burst(L(e), ParticleTypes.END_ROD, e.position().add(0, 1, 0), 24, 0.5, 0.1);
        snd(e, SoundEvents.TOTEM_USE, 0.8f, 0.6f);
        if (e.getOwner() != null) e.getOwner().displayClientMessage(net.minecraft.network.chat.Component.literal("OMORI: ...I'm still here. (1 HP)"), true);
        return true;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        p.rArmX = -0.2f + p.rArmX * 0.5f; p.rArmZ = 0.1f; p.rHandX = 0.4f; p.headX += 0.1f; p.bodyX += 0.05f; // listless slouch
    }

    @Override
    public void passive(SlayerEntity e) {
        // EMOTIONS: angry when hurt (strength), sad when critical (resistance), happy otherwise (slow regen)
        if (e.tickCount % 40 != 0) return;
        float f = e.getHealth() / e.getMaxHealth();
        if (f < 0.3f) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, false));
        else if (f < 0.65f) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, false, false));
        else e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false));
    }

    @Override
    protected void defineAttacks(List<AttackDef> out) {
        defineBase(out);
        for (AttackDef a : out) if (a.id.equals("final_strike")) a.anim = (p, t, ag) -> KU.strikePose(p, KU.remap(t, 20 / 34f), 2, 1.2f);
    }

    private void defineBase(List<AttackDef> out) {
        out.add(AttackDef.of("attack").range(0, 3f).cd(14).weight(3).dur(14).weapon("knife")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);
                    if (n == 5) { slash(e, 60, -50, 15, 0xffffff); melee(e, 3.1, 6, 0.4, 0.1, 0xffffff); }
                }).anim((p, t, a) -> swing(p, t, 5 / 14f, 1f)));

        out.add(AttackDef.of("stab").range(0, 3.2f).cd(26).weight(2).dur(18).weapon("knife")
                .tick((e, t, n) -> {
                    if (n == 2) Combat.lunge(e, 0.55, 0.02);
                    if (n == 7) { melee(e, 3.3, 10, 0.5, 0.15, 0x000000); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 1.3f); }
                }).anim((p, t, a) -> stab(p, t, 7 / 18f)));

        out.add(AttackDef.of("knife_throw").range(4, 16).cd(50).weight(2).dur(18).weapon("knife")
                .tick((e, t, n) -> {
                    if (n == 6) { Combat.shoot(e, KNIFE, mz(e), aim(e, t, 0.02), 1.7, 7f); snd(e, SoundEvents.TRIDENT_THROW, 0.8f, 1.8f); }
                }).anim((p, t, a) -> throwAnim(p, t)));

        // Trip: drop the knife and sweep leg, knockdown
        out.add(AttackDef.of("trip").range(0, 3f).cd(60).weight(1).dur(20)
                .tick((e, t, n) -> {
                    if (n == 6) for (LivingEntity f : Combat.cone(e, 2.8, 80)) {
                        Combat.damage(e, f, 4); Combat.knock(f, e.position(), 0.9, 0.35);
                        f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    }
                }).anim((p, t, a) -> {
                    float s = bump(win(t, .15f, .7f));
                    p.rootY -= 3f * s; p.rLegX = -1.4f * s; p.lLegX = 0.3f * s; p.bodyX += 0.4f * s; p.rArmZ = 0.9f * s; p.lArmZ = -0.9f * s;
                }));

        // Final Strike: the heavy, stilled lunge with a monochrome flash
        out.add(AttackDef.of("final_strike").range(0, 3.6f).cd(120).weight(1).dur(34).weapon("knife").lowHealthTarget(0.5f)
                .tick((e, t, n) -> {
                    if (n == 8) snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 0.5f);
                    if (n >= 8 && n < 20) Fx.dustBurst(L(e), n % 2 == 0 ? 0xffffff : 0x000000, 1.2f, e.position().add(0, 1, 0), 4, 0.5);
                    if (n == 20) {
                        Combat.lunge(e, 0.7, 0.05);
                        melee(e, 3.6, 22, 1.2, 0.4, 0xffffff);
                        Fx.ring(L(e), e.position().add(0, 0.2, 0), 3f, 0xffffff, 1.6f, 24); snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.6f);
                    }
                }).anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -0.2f, .3f, -2.9f, .55f, -2.9f, .62f, -0.7f, .8f, -0.9f, 1, -0.2f);
                    p.lArmX = track(t, 0, 0, .4f, -2.5f, .62f, -0.5f, 1, 0);
                    p.bodyX = track(t, 0, 0, .55f, -0.3f, .62f, 0.6f, .85f, 0.3f, 1, 0); p.headX = -p.bodyX * 0.5f;
                    p.rootY = track(t, 0, 0, .55f, 0.6f, .62f, -1.5f, 1, 0); p.rootZ = -2.5f * bump(win(t, .55f, .85f));
                }));

        // Stare: Omori's blank gaze drains the enemy's will
        out.add(AttackDef.of("stare").range(2, 12).cd(160).weight(1).dur(30)
                .tick((e, t, n) -> {
                    if (n == 10 && t != null) {
                        for (LivingEntity f : Combat.around(e, t.position(), 4)) {
                            f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1)); f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                            if (f instanceof Mob m) m.setTarget(null);
                        }
                        Fx.ring(L(e), t.position().add(0, 1, 0), 2.5f, 0x202020, 1.6f, 24);
                        snd(e, SoundEvents.WARDEN_HEARTBEAT, 1f, 0.6f);
                    }
                }).anim((p, t, a) -> { float k = bump(win(t, .1f, .9f)); p.headX += 0.25f * k; p.rArmZ += 0.1f * k; p.lArmZ -= 0.1f * k; }));

        // Sad tears (rain of sorrow)
        out.add(AttackDef.of("sad_poem").range(4, 18).cd(110).weight(2).dur(34)
                .tick((e, t, n) -> {
                    if (n >= 8 && n < 28 && n % 4 == 0)
                        Combat.shoot(e, TEAR, e.position().add(0, 2.4, 0), aim(e, t, 0.2).add(0, 0.1, 0).normalize(), 0.9, 4f);
                    if (n == 8) snd(e, SoundEvents.AMBIENT_UNDERWATER_ENTER, 0.8f, 0.7f);
                }).anim((p, t, a) -> { float k = bump(win(t, .1f, .9f)); p.rArmX = -2.6f * k; p.lArmX = -2.6f * k; p.headX -= 0.4f * k; p.bodyX -= 0.15f * k; }));

        // Hands of Black Space: grab and crush
        out.add(AttackDef.of("black_space_hands").range(2, 12).cd(180).weight(1).dur(40)
                .tick((e, t, n) -> {
                    if (t != null && n >= 6 && n < 30) {
                        Vec3 b = t.position();
                        if (n % 3 == 0) { Fx.dustBurst(L(e), 0x000000, 1.6f, b.add(0, 0.5, 0), 10, 0.4); Fx.burst(L(e), ParticleTypes.SQUID_INK, b.add(0, 1, 0), 4, 0.3, 0.02); }
                        t.setDeltaMovement(t.getDeltaMovement().multiply(0.2, 1, 0.2));
                    }
                    if (n == 30 && t != null) { Combat.damage(e, t, 16); snd(e, SoundEvents.WITHER_HURT, 0.6f, 0.5f); Fx.explosion(L(e), t.position().add(0, 1, 0), 1.5f); }
                }).anim((p, t, a) -> { float k = bump(win(t, .1f, .85f)); p.rArmX = -1.3f * k; p.lArmX = -1.3f * k; p.rArmZ = 0.4f * k; p.lArmZ = -0.4f * k; p.bodyX += 0.2f * k; }));

        // Heart: a homing heart (the friends' gift)
        out.add(AttackDef.of("heart").range(4, 20).cd(90).weight(1).dur(24)
                .tick((e, t, n) -> { if (n == 9) { Combat.shoot(e, HEART, mz(e), aim(e, t, 0.02), 0.8, 9f); snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.4f); } })
                .anim((p, t, a) -> throwAnim(p, t)));
    }

    @Override
    public void buildModel(Rig rig) {
        Mat white = Mat.of(0xf4f4f4).noise(.04f), black = Mat.of(0x161616).noise(.04f), skin = Mat.of(0xe8e0d8).noise(.03f).bevel(.1f);
        Mat hair = Mat.of(0x0b0b0b).noise(.06f), eye = Mat.of(0x050505), gray = Mat.of(0x6a6a6a).noise(.05f);
        Mat shirt = Mat.of(0xf4f4f4).decal((m, g, x, y, w, h, f) -> { if (f == Mat.Face.FRONT) g.rect(x, y + h - 2, w, 2, 0x161616); });
        rig.box(rig.torso, shirt, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, black, -4, -3, -2, 8, 3, 4, .1f);
        rig.box(rig.head, skin, -4, -8, -4, 8, 8, 8);
        rig.box(rig.head, hair, -4, -8, -4, 8, 3, 8, .3f);
        rig.box(rig.head, hair, -4, -8, 3, 8, 5, 1, .3f);
        rig.box(rig.head, eye, -3, -4.5f, -4.2f, 2, 2, 1);
        rig.box(rig.head, eye, 1, -4.5f, -4.2f, 2, 2, 1);
        rig.box(rig.head, gray, -1, -1.5f, -4.2f, 2, 1, 1);
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm, leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(arm, white, -2, -2, -2, 4, 6, 4);
            rig.box(arm, skin, -2, 4, -2, 4, 6, 4);
            rig.box(leg, black, -2, 0, -2, 4, 9, 4);
            rig.box(leg, white, -2, 9, -2, 4, 3, 4, .1f);
        }
        PartDefinition w = rig.weapon(rig.rHand, "knife");
        rig.box(w, Mat.of(0xdcdcdc).bevel(.1f), -0.5f, -2.5f, -9, 1, 2, 8);
        rig.box(w, black, -1, -2, -1, 2, 2, 4);
    }
}
