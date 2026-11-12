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

/** Chara (Undertale): Real Knife, DETERMINATION. Refuses to die: revives with full HP up to REVIVES times. */
public class Chara extends SlayerKit {
    static final int REVIVES = 20, RED = 0xff2020;
    static final ProjStyle SOUL = ProjStyle.make().color(RED).size(0.22f).life(60).homing(0.12f).trail(ParticleTypes.CRIT);
    static final ProjStyle KNIFE = ProjStyle.make().color(0xe0e0e0).size(0.2f).life(40).pierce(2).trail(ParticleTypes.CRIT);
    static final ProjStyle SLASH_WAVE = ProjStyle.make().color(RED).size(0.7f).life(24).pierce(4).trail(ParticleTypes.DAMAGE_INDICATOR);

    public Chara() {
        super("chara", "Chara", "チャラ", "Undertale");
        eggPrimary = 0x4a9a3a; eggSecondary = 0xff2020;
        maxHealth = 92; speed = 0.34; armor = 4; knockbackRes = 0.2;
        scale = 0.95f; preferredRange = 3.5f; dmgMult = 1.1f;
        idleWeaponR = "real_knife";
    }

    @Override
    public boolean preventDeath(SlayerEntity e) {
        var d = e.getPersistentData();
        int used = d.getInt("ds_chara_rev");
        if (used >= REVIVES) return false;
        d.putInt("ds_chara_rev", used + 1);
        e.setHealth(e.getMaxHealth());
        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4));
        Vec3 c = e.position().add(0, 1, 0);
        Fx.dustBurst(L(e), RED, 1.6f, c, 30, 0.6); Fx.ring(L(e), c, 2.5f, RED, 1.6f, 24);
        snd(e, SoundEvents.TOTEM_USE, 0.7f, 0.5f); snd(e, SoundEvents.WITHER_SPAWN, 0.3f, 1.8f);
        if (e.getOwner() != null) e.getOwner().displayClientMessage(Component.literal("Chara: You cannot give up just yet... (" + (REVIVES - used - 1) + " left)"), true);
        return true;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        p.rArmX = -0.3f + p.rArmX * 0.5f; p.rArmZ = 0.12f; p.rHandX = 0.45f; p.headX += 0.06f;
    }

    @Override
    public void passive(SlayerEntity e) {
        if (e.tickCount % 60 == 0) e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false));
    }

    @Override
    protected void defineAttacks(List<AttackDef> out) {
        defineBase(out);
        for (AttackDef a : out) if (a.id.equals("flurry")) a.anim = KU.comboAnim(new int[] {4, 10, 16, 22}, 30, new int[] {0, 1, 0, 1});
        for (AttackDef a : out) if (a.id.equals("slash")) a.anim = (p, t, ag) -> KU.strikePose(p, KU.remap(t, 5 / 14f), 0, 1f);
        for (AttackDef a : out) if (a.id.equals("crescent")) a.anim = (p, t, ag) -> KU.strikePose(p, KU.remap(t, 8 / 24f), 0, 1.2f);
    }

    private void defineBase(List<AttackDef> out) {
        out.add(AttackDef.of("slash").range(0, 3f).cd(12).weight(3).dur(14).weapon("real_knife")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.6f);
                    if (n == 5) { slash(e, 70, -60, 10, RED); melee(e, 3.1, 7, 0.4, 0.1, RED); }
                }).anim((p, t, a) -> swing(p, t, 5 / 14f, 1f)));

        out.add(AttackDef.of("flurry").range(0, 3f).cd(60).weight(2).dur(30).weapon("real_knife")
                .tick((e, t, n) -> {
                    if (n == 4 || n == 10 || n == 16 || n == 22) {
                        boolean r = n % 12 == 4; slash(e, r ? 70 : -60, r ? -60 : 70, r ? 10 : -20, RED);
                        melee(e, 3.1, 4.5f, 0.2, 0.05, RED); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, 1.2f + n * 0.02f);
                    }
                }).anim((p, t, a) -> {
                    float s = (float) Math.sin(t * PI * 4f);
                    p.rArmX = -1.4f + 0.5f * Math.abs(s); p.rArmZ = 0.9f * s; p.lArmX = -1.0f; p.lArmZ = -0.8f * s;
                    p.bodyY = 0.7f * s; p.headY = -0.35f * s; p.rootZ = -1f; p.lLegX = -0.4f; p.rLegX = 0.3f;
                }));

        out.add(AttackDef.of("lunge_stab").range(2, 7).cd(70).weight(2).dur(20).weapon("real_knife")
                .tick((e, t, n) -> {
                    if (n == 2) { Combat.lunge(e, 1.1, 0.03); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8f, 1.4f); }
                    if (n == 8) { melee(e, 3.4, 11, 0.7, 0.15, RED); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1f); }
                }).anim((p, t, a) -> stab(p, t, 8 / 20f)));

        out.add(AttackDef.of("knife_throw").range(4, 16).cd(45).weight(2).dur(18).weapon("real_knife")
                .tick((e, t, n) -> { if (n == 6) { Combat.shoot(e, KNIFE, mz(e), aim(e, t, 0.02), 1.7, 7f); snd(e, SoundEvents.TRIDENT_THROW, 0.8f, 1.9f); } })
                .anim((p, t, a) -> throwAnim(p, t)));

        // Slash wave: crescent of red energy
        out.add(AttackDef.of("crescent").range(3, 14).cd(80).weight(2).dur(24).weapon("real_knife")
                .tick((e, t, n) -> {
                    if (n == 8) {
                        Combat.shoot(e, SLASH_WAVE, mz(e), aim(e, t, 0), 1.2, 9f); slash(e, 80, -80, 60, RED);
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
                    }
                }).anim((p, t, a) -> swing(p, t, 8 / 24f, 1.2f)));

        // DETERMINATION: ring of soul bullets
        out.add(AttackDef.of("determination").range(3, 14).cd(130).weight(1).dur(40)
                .tick((e, t, n) -> {
                    if (n >= 8 && n < 28 && n % 4 == 0) {
                        for (int i = 0; i < 8; i++) {
                            double a = i * Math.PI / 4 + n * 0.2;
                            Combat.shoot(e, SOUL, e.position().add(0, 1.3, 0), new Vec3(Math.cos(a), 0.02, Math.sin(a)), 0.7, 4f);
                        }
                        snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 0.6f);
                    }
                }).anim((p, t, a) -> { float k = bump(win(t, .1f, .85f)); p.rArmZ = 1.5f * k; p.lArmZ = -1.5f * k; p.headX -= 0.4f * k; p.rootY += 0.4f * k; }));

        // Dust: reset shockwave
        out.add(AttackDef.of("reset").range(0, 5).cd(220).weight(1).dur(36)
                .tick((e, t, n) -> {
                    if (n == 14) {
                        Vec3 c = e.position().add(0, 0.2, 0);
                        for (int i = 1; i <= 3; i++) Fx.ring(L(e), c, i * 1.7f, RED, 1.6f, 28);
                        Combat.blast(e, e.position(), 6f, 14, 1.4f, e); snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.7f, 1.4f);
                    }
                }).anim((p, t, a) -> {
                    float k = win(t, 0, .35f) * (1 - win(t, .45f, .6f));
                    p.rArmX = -2.9f * k; p.lArmX = -2.9f * k; p.bodyX -= 0.3f * k; p.rootY -= 1f * k + 1.5f * bump(win(t, .35f, .55f)); p.lLegX = -0.4f; p.rLegX = 0.4f;
                }));

        // Execution (genocide route): sever a nearly-dead enemy
        out.add(AttackDef.of("genocide").range(0, 3.4f).lowHealthTarget(0.3f).cd(40).weight(5).dur(24).weapon("real_knife")
                .tick((e, t, n) -> {
                    if (n == 2) Combat.lunge(e, 0.9, 0.02);
                    if (n == 9 && t != null) {
                        Fx.dustBurst(L(e), RED, 1.4f, mid(t), 24, 0.4); slash(e, 90, -90, 40, RED);
                        snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.6f);
                        Combat.damage(e, t, t.getHealth() + 50);
                    }
                }).anim((p, t, a) -> stab(p, t, 9 / 24f)));
    }

    @Override
    public void buildModel(Rig rig) {
        Mat skin = Mat.of(0xf0d4b8).noise(.03f).bevel(.1f), hair = Mat.of(0x5a3a22).noise(.1f), eye = Mat.of(RED).glow();
        Mat green = Mat.of(0x4a9a3a).noise(.06f), stripe = Mat.of(0xe8d838), brown = Mat.of(0x6a4a30).noise(.08f), dark = Mat.of(0x303030);
        Mat sweater = Mat.of(0x4a9a3a).noise(.06f).decal((m, g, x, y, w, h, f) -> { if (f == Mat.Face.FRONT || f == Mat.Face.BACK) g.rect(x, y + h / 2 - 1, w, 2, 0xe8d838); });
        rig.box(rig.torso, sweater, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, brown, -4, -3, -2, 8, 3, 4, .1f);
        rig.box(rig.head, skin, -4, -8, -4, 8, 8, 8);
        rig.box(rig.head, hair, -4, -8, -4, 8, 3, 8, .3f);
        rig.box(rig.head, hair, -4, -8, 3, 8, 5, 1, .3f);
        rig.box(rig.head, hair, -4.3f, -8, -4.4f, 2, 4, 1);
        rig.box(rig.head, eye, -3, -4.5f, -4.2f, 2, 1, 1);
        rig.box(rig.head, eye, 1, -4.5f, -4.2f, 2, 1, 1);
        rig.box(rig.head, dark, -1.5f, -1.5f, -4.2f, 3, 1, 1);
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm, leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(arm, green, -2, -2, -2, 4, 8, 4);
            rig.box(arm, skin, -2, 6, -2, 4, 4, 4);
            rig.box(leg, brown, -2, 0, -2, 4, 10, 4);
            rig.box(leg, dark, -2, 10, -2.5f, 4, 2, 5, .1f);
        }
        rig.box(rig.torso, Mat.of(0xe8d838).glow(), -1.5f, -11.5f, -2.3f, 3, 1, 1); // locket glint
        PartDefinition w = rig.weapon(rig.rHand, "real_knife");
        rig.box(w, Mat.of(0xe8e8e8).bevel(.1f), -0.5f, -2.5f, -10, 1, 2, 9);
        rig.box(w, Mat.of(0x8a1a1a), -1, -2, -1, 2, 2, 4);
    }
}
