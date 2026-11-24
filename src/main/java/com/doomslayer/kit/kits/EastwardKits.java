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

/** Eastward: John (miner-cook: frying pan, bombs, guns, meals) and Sam (Earth Born girl: psychic energy). */
public abstract class EastwardKits extends SlayerKit {
    static final int PSI = 0xff7ad0, FIRE = 0xff9a30;
    static final ProjStyle BULLET = ProjStyle.make().color(0xffe080).size(0.12f).life(30).shape(ProjStyle.Shape.BOLT);
    static final ProjStyle BOMB = ProjStyle.make().color(0x303040).size(0.35f).life(30).gravity(0.05).explode(3.6f).bounce().shape(ProjStyle.Shape.GRENADE);
    static final ProjStyle PSI_SHOT = ProjStyle.make().color(PSI).size(0.3f).life(40).pierce(1).shape(ProjStyle.Shape.PLASMA);
    static final ProjStyle PSI_BLAST = ProjStyle.make().color(PSI).size(0.8f).life(34).pierce(6).shape(ProjStyle.Shape.ORB);

    EastwardKits(String id, String name, String ja, double hp, int egg1, int egg2, String idle) {
        super(id, name, ja, "Eastward");
        eggPrimary = egg1; eggSecondary = egg2; maxHealth = hp; speed = 0.33; armor = 5; knockbackRes = 0.2; scale = 0.95f; preferredRange = 6f; idleWeaponR = idle;
    }
    @Override public float modifyDamage(SlayerEntity e, net.minecraft.world.damagesource.DamageSource src, float amt) { return KU.defend(e, src, amt); }
    @Override public void idle(Pose p, SlayerEntity e, float age, float m) { p.rArmX = -0.3f + p.rArmX * 0.5f; p.rArmZ = 0.1f; p.rHandX = 0.5f; p.headX += 0.05f; }
    @Override public void passive(SlayerEntity e) { if (e.tickCount % 140 == 0 && e.getHealth() < e.getMaxHealth() && Combat.around(e, e.position(), 8).isEmpty()) e.heal(3); }

    // ================= John =================
    public static class John extends EastwardKits {
        public John() { super("eastward_john", "John (Eastward)", "ジョン (イーストワード)", 110, 0x4a5a8a, 0xffb040, "pan"); }

        @Override
        protected void defineAttacks(List<AttackDef> out) {
            // frying pan: three-hit swing, and the Pan Deflect that bats bullets back
            out.add(AttackDef.of("pan_swing").range(0, 3f).cd(34).weight(3).dur(26).weapon("pan")
                    .tick((e, t, n) -> { if (n == 5 || n == 12 || n == 20) { boolean last = n == 20; slash(e, n == 12 ? -70 : 75, n == 12 ? 75 : -70, 10, 0xffe0a0); KU.melee(e, 3.1, last ? 9 : 5, last ? 1.0 : 0.3, 0.1, 0xffe0a0); snd(e, SoundEvents.ANVIL_PLACE, 0.5f, 1.6f); } })
                    .anim(KU.comboAnim(new int[] {5, 12, 20}, 26, new int[] {0, 1, 2})));
            out.add(AttackDef.of("pan_deflect").range(0, 8).noLos().cd(60).weight(3).dur(20).weapon("pan")
                    .tick((e, t, n) -> { if (n == 2) { KU.parryWindow(e, 14); snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.7f, 1.8f); } })
                    .anim((p, t, a) -> { float c = ready(t, .15f, .85f); p.rArmX = lerp(p.rArmX, -1.4f, c); p.rArmZ = lerp(p.rArmZ, -0.4f, c); p.lArmX = lerp(p.lArmX, -0.6f, c); stance(p, c); }));
            out.add(AttackDef.of("pistol").range(3, 22).cd(26).weight(3).dur(14).weapon("gun")
                    .tick((e, t, n) -> { if (n == 4) { Combat.hitscan(e, mz(e), aim(e, t, 0.02), 24, 4.5f, 0, 0xffe080); flash(e, 0xffe080); snd(e, SoundEvents.DISPENSER_LAUNCH, 0.8f, 1.8f); } })
                    .anim(KU.gunAnim(new int[] {4}, 14, 1, false)));
            out.add(AttackDef.of("shotgun").range(0, 11).cd(60).weight(2).dur(26).weapon("gun")
                    .tick((e, t, n) -> { if (n == 6) { for (int i = 0; i < 7; i++) Combat.hitscan(e, mz(e), aim(e, t, 0.11), 12, 2.6f, 0, 0xffd070); flash(e, 0xffa040); snd(e, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.7f); } })
                    .anim(KU.gunAnim(new int[] {6}, 26, 4, false)));
            out.add(AttackDef.of("bomb").range(4, 16).cd(90).weight(2).dur(22).weapon("bomb")
                    .tick((e, t, n) -> { if (n == 8) { Combat.shoot(e, BOMB, mz(e), aim(e, t, 0.02).add(0, 0.18, 0).normalize(), 0.9, 12f); snd(e, SoundEvents.SNOWBALL_THROW, 1f, 0.7f); } })
                    .anim((p, t, a) -> throwAnim(p, t)));
            out.add(AttackDef.of("flame_burst").range(1, 7).cd(120).weight(1).dur(36).weapon("gun")
                    .tick((e, t, n) -> { if (n >= 6 && n < 30) { Vec3 m = mz(e), d = aim(e, t, 0.05); for (int i = 1; i <= 5; i++) Fx.burst(L(e), ParticleTypes.FLAME, m.add(d.scale(i * 1.1)), 3, 0.25, 0.02); if (n % 4 == 0) for (LivingEntity f : Combat.cone(e, 6.5, 22)) { Combat.damage(e, f, 3); f.setSecondsOnFire(5); } } })
                    .anim(KU.gunAnim(new int[] {6, 30}, 36, 0, true)));
            // cooking: a hot meal heals, and warms nearby friends
            out.add(AttackDef.of("cook_meal").range(0, 12).cd(700).weight(1).dur(40).noLos().when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.65f).weapon("pan")
                    .tick((e, t, n) -> { if (n < 24 && n % 4 == 0) Fx.burst(L(e), ParticleTypes.CAMPFIRE_COSY_SMOKE, e.position().add(0, 1.5, 0), 2, 0.2, 0.02); if (n == 28) { for (LivingEntity f : e.level().getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(7), x -> x == e || x instanceof net.minecraft.world.entity.player.Player || x instanceof SlayerEntity)) { f.heal(f.getMaxHealth() * 0.4f); f.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0)); } Fx.aura(L(e), e.position(), FIRE, 2f, 0.8f, 28); snd(e, SoundEvents.GENERIC_EAT, 1f, 1f); } })
                    .anim((p, t, a) -> { float s = (float) Math.sin(t * PI * 6) * bump(win(t, .1f, .7f)); p.rArmX = -1.3f + 0.3f * s; p.lArmX = -1.0f; p.bodyX += 0.2f; p.rHandX = 0.4f * s; }));
        }

        @Override
        public void buildModel(Rig rig) {
            Mat skin = Mat.of(0xe8c09a).noise(.04f).bevel(.1f), hair = Mat.of(0x15151c).noise(.1f), vest = Mat.of(0x3a6a8a).noise(.1f), shirt = Mat.of(0xd8d0b8).noise(.08f), pants = Mat.of(0x4a3a2a).noise(.1f);
            Mat belt = Mat.of(0x6a4a22), boot = Mat.of(0x2a2220), lamp = Mat.of(0xfff0a0).glow(), eye = Mat.of(0x101018);
            rig.box(rig.torso, shirt, -4, -12, -2, 8, 12, 4); rig.box(rig.torso, vest, -4.2f, -12, -2.3f, 8, 9, 4, .2f); rig.box(rig.torso, belt, -4.4f, -3.5f, -2.4f, 9, 2, 5); rig.box(rig.torso, Mat.of(0x8a6a30), -5, -9, 2, 10, 8, 3); // backpack
            rig.box(rig.head, skin, -4, -8, -4, 8, 8, 8); rig.box(rig.head, hair, -4, -8.5f, -4, 8, 3, 8, .3f); rig.box(rig.head, hair, -4, -8, 3, 8, 8, 1, .3f);
            rig.box(rig.head, eye, -3, -4.5f, -4.2f, 2, 2, 1); rig.box(rig.head, eye, 1, -4.5f, -4.2f, 2, 2, 1);
            rig.box(rig.head, Mat.of(0xe8b030).bevel(.2f), -4.5f, -10, -4.5f, 9, 2, 9); rig.box(rig.head, lamp, -1.5f, -9.5f, -5.2f, 3, 2, 1); // miner's lamp helmet
            for (int s = -1; s <= 1; s += 2) { PartDefinition arm = s < 0 ? rig.rArm : rig.lArm, leg = s < 0 ? rig.rLeg : rig.lLeg;
                rig.box(arm, shirt, -2, -2, -2, 4, 8, 4); rig.box(arm, skin, -2, 6, -2, 4, 4, 4); rig.box(leg, pants, -2, 0, -2, 4, 9, 4); rig.box(leg, boot, -2, 8, -2, 4, 4, 4, .2f); }
            PartDefinition w = rig.weapon(rig.rHand, "pan"); rig.box(w, Mat.of(0x303038).bevel(.2f), -4, -2.5f, -13, 8, 1, 8); rig.box(w, Mat.of(0x6a4a22), -0.5f, -2.5f, -6, 1, 1, 6);
            w = rig.weapon(rig.rHand, "gun"); rig.box(w, Mat.of(0x303038), -1, -3, -8, 2, 2, 8); rig.box(w, Mat.of(0x6a4a22), -1, -1, -1, 2, 3, 2);
            w = rig.weapon(rig.rHand, "bomb"); rig.box(w, Mat.of(0x202028).bevel(.3f), -2, -2.5f, -4, 4, 4, 4, .2f); rig.box(w, Mat.of(FIRE).glow(), -0.5f, -4, -3, 1, 2, 1);
        }
    }

    // ================= Sam =================
    public static class Sam extends EastwardKits {
        public Sam() { super("eastward_sam", "Sam (Eastward)", "サム (イーストワード)", 85, 0xe8e8f4, PSI, "none"); }

        @Override
        protected void defineAttacks(List<AttackDef> out) {
            out.add(AttackDef.of("psi_slap").range(0, 2.8f).cd(26).weight(3).dur(16)
                    .tick((e, t, n) -> { if (n == 5) { KU.melee(e, 2.9, 6, 0.6, 0.15, PSI); Fx.flash(L(e), mz(e).add(e.getViewVector(1f).scale(1.5)), PSI, 0.6f); snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 0.8f, 1.4f); } })
                    .anim((p, t, a) -> KU.strikePose(p, KU.remap(t, 5 / 16f), 4, 1f)));
            out.add(AttackDef.of("psi_bolt").range(3, 20).cd(24).weight(3).dur(14)
                    .tick((e, t, n) -> { if (n == 4) { Combat.shoot(e, PSI_SHOT, mz(e), aim(e, t, 0.02), 1.5, 6f); snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.7f); } })
                    .anim((p, t, a) -> { float c = ready(t, .2f, .85f); p.rArmX = lerp(p.rArmX, -1.55f, c); p.lArmX = lerp(p.lArmX, -0.3f, c); p.rootZ += 1f * kick(t, 4 / 14f, .3f); }));
            out.add(AttackDef.of("energy_blast").range(4, 22).cd(120).weight(2).dur(44) // charged: gathers energy, then fires a piercing burst
                    .tick((e, t, n) -> { if (n < 26) Fx.dustBurst(L(e), PSI, 1f + n * 0.04f, mz(e), 3, 0.5 - n * 0.015); if (n == 28) { Combat.shoot(e, PSI_BLAST, mz(e), aim(e, t, 0), 1.6, 16f); Fx.ring(L(e), mz(e), 1.2f, PSI, 1.6f, 22); snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.7f, 1.8f); } })
                    .anim((p, t, a) -> { float c = ready(t, .15f, .85f), r = 2.4f * kick(t, 28 / 44f, .3f); p.rArmX = lerp(p.rArmX, -1.55f, c) - 0.3f * r; p.lArmX = lerp(p.lArmX, -1.55f, c) - 0.3f * r; p.rootZ += r; stance(p, c); }));
            out.add(AttackDef.of("psi_barrier").range(0, 10).cd(300).weight(1).dur(30).noLos()
                    .tick((e, t, n) -> { if (n == 4) { e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 3)); KU.parryWindow(e, 40); Fx.ring(L(e), e.position().add(0, 1, 0), 2.4f, PSI, 1.6f, 26); snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.9f); } })
                    .anim((p, t, a) -> { float c = bump(win(t, .1f, .9f)); p.rArmX = -1.0f * c; p.lArmX = -1.0f * c; p.rArmZ = 0.6f * c; p.lArmZ = -0.6f * c; p.headX -= 0.2f * c; }));
            out.add(AttackDef.of("heal_pulse").range(0, 12).cd(600).weight(1).dur(34).noLos().when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.7f)
                    .tick((e, t, n) -> { if (n == 12) { for (LivingEntity f : e.level().getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(8), x -> x == e || x instanceof net.minecraft.world.entity.player.Player || x instanceof SlayerEntity)) f.heal(f.getMaxHealth() * 0.3f); Fx.aura(L(e), e.position(), 0xa0ffc8, 2f, 0.8f, 30); snd(e, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.3f); } })
                    .anim((p, t, a) -> { float c = bump(win(t, .1f, .9f)); p.rArmX = -2.4f * c; p.lArmX = -2.4f * c; p.headX -= 0.3f * c; }));
            out.add(AttackDef.of("shockwave").range(0, 6).cd(150).weight(2).dur(26)
                    .tick((e, t, n) -> { if (n == 12) { Combat.blast(e, e.position(), 5f, 9, 1.4f, e); for (int i = 1; i <= 3; i++) Fx.ring(L(e), e.position().add(0, 0.2, 0), i * 1.7f, PSI, 1.5f, 28); snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.8f); } })
                    .anim((p, t, a) -> { float c = bump(win(t, .1f, .8f)); p.rArmX = -2.6f * c; p.lArmX = -2.6f * c; p.rootY -= 1.2f * c; p.bodyX += 0.4f * win(t, .4f, .6f) * (1 - win(t, .7f, 1f)); }));
        }

        @Override
        public void buildModel(Rig rig) {
            Mat skin = Mat.of(0xf0cfb0).noise(.04f).bevel(.1f), hair = Mat.of(0xf4f4f8).noise(.08f), dress = Mat.of(0xc03a4a).noise(.08f), white = Mat.of(0xf0f0f4).noise(.05f), boot = Mat.of(0x4a3a30), eye = Mat.of(0x2a50c0).glow(), glow = Mat.of(PSI).glow();
            rig.box(rig.torso, white, -4, -12, -2, 8, 12, 4); rig.box(rig.torso, dress, -4.2f, -12, -2.3f, 8, 8, 4, .2f); rig.box(rig.torso, dress, -5, -4, -3, 10, 5, 6); rig.box(rig.torso, glow, -1, -9, -2.7f, 2, 2, 1);
            rig.box(rig.head, skin, -4, -8, -4, 8, 8, 8); rig.box(rig.head, hair, -4, -8.5f, -4, 8, 3, 8, .3f); rig.box(rig.head, hair, -4, -8, 3, 8, 9, 1, .3f); rig.box(rig.head, hair, -5.2f, -6, -2, 1, 8, 4); rig.box(rig.head, hair, 4.2f, -6, -2, 1, 8, 4);
            rig.box(rig.head, eye, -3, -4.5f, -4.2f, 2, 2, 1); rig.box(rig.head, eye, 1, -4.5f, -4.2f, 2, 2, 1); rig.box(rig.head, glow, -0.5f, -9.2f, -4.4f, 1, 1, 1); // Earth Born mark
            for (int s = -1; s <= 1; s += 2) { PartDefinition arm = s < 0 ? rig.rArm : rig.lArm, leg = s < 0 ? rig.rLeg : rig.lLeg;
                rig.box(arm, white, -2, -2, -2, 4, 8, 4); rig.box(arm, skin, -2, 6, -2, 4, 4, 4); rig.box(leg, white, -2, 0, -2, 4, 9, 4); rig.box(leg, boot, -2, 8, -2, 4, 4, 4, .2f); }
        }
    }
}
