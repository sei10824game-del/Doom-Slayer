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

/** Wolf / Sekiro (Sekiro: Shadows Die Twice): shinobi with Kusabimaru and the Shinobi Prosthetic. Resurrects once. */
public class SekiroWolf extends SlayerKit {
    static final int GOLD = 0xffc040, FIRE = 0xff6020;
    static final ProjStyle SHURIKEN = ProjStyle.make().color(0xc0c0c0).size(0.22f).life(40).pierce(1).trail(ParticleTypes.CRIT);
    static final ProjStyle SPEAR = ProjStyle.make().color(0xb0b0b0).size(0.25f).life(20).pierce(3).trail(ParticleTypes.CRIT);

    public SekiroWolf() {
        super("sekiro_wolf", "Wolf (Sekiro)", "狼 (隻狼)", "Sekiro: Shadows Die Twice");
        eggPrimary = 0x3a3a40; eggSecondary = 0xa02020;
        maxHealth = 90; speed = 0.36; armor = 6; knockbackRes = 0.3;
        scale = 1.0f; preferredRange = 4f; dmgMult = 1.1f;
        idleWeaponR = "kusabimaru";
    }

    @Override
    public boolean preventDeath(SlayerEntity e) { // Resurrection: once, with half health
        var d = e.getPersistentData();
        if (d.getBoolean("ds_wolf_res")) return false;
        d.putBoolean("ds_wolf_res", true);
        e.setHealth(e.getMaxHealth() * 0.5f);
        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3));
        Fx.dustBurst(L(e), 0xffffff, 1.4f, e.position().add(0, 1, 0), 30, 0.6); Fx.ring(L(e), e.position().add(0, 0.3, 0), 2.5f, GOLD, 1.5f, 24);
        snd(e, SoundEvents.TOTEM_USE, 0.8f, 0.8f);
        if (e.getOwner() != null) e.getOwner().displayClientMessage(Component.literal("Wolf: Resurrection!"), true);
        return true;
    }

    @Override public void idle(Pose p, SlayerEntity e, float age, float m) { p.rArmX = -0.5f + p.rArmX * 0.5f; p.rArmZ = 0.1f; p.rHandX = 0.4f; p.bodyX += 0.05f; p.lArmX = -0.2f + p.lArmX * 0.5f; }

    @Override
    public void passive(SlayerEntity e) { // Healing Gourd
        if (e.tickCount % 400 == 0 && e.getHealth() < e.getMaxHealth() * 0.5f) {
            e.heal(e.getMaxHealth() * 0.3f); snd(e, SoundEvents.GENERIC_DRINK, 0.8f, 1f); Fx.burst(L(e), ParticleTypes.HAPPY_VILLAGER, e.position().add(0, 1.2, 0), 10, 0.4, 0.05);
        }
    }

    @Override
    protected void defineAttacks(List<AttackDef> out) {
        out.add(AttackDef.of("kusabimaru").range(0, 3.2f).cd(12).weight(3).dur(14).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 5) { slash(e, 70, -60, 10, 0xffffff); melee(e, 3.3, 7, 0.4, 0.1, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.4f); }
                }).anim((p, t, a) -> swing(p, t, 5 / 14f, 1f)));

        // three-hit chain
        out.add(AttackDef.of("combo_chain").range(0, 3.2f).cd(55).weight(2).dur(36).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 5 || n == 16 || n == 27) {
                        boolean r = n != 16; Combat.lunge(e, 0.3, 0); slash(e, r ? 70 : -60, r ? -60 : 70, n == 27 ? 60 : 10, 0xffffff);
                        melee(e, 3.3, n == 27 ? 10 : 5, n == 27 ? 0.9 : 0.2, 0.05, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.2f + n * 0.01f);
                    }
                }).anim((p, t, a) -> {
                    float s = (float) Math.sin(t * PI * 3f);
                    p.rArmX = -1.4f + 0.4f * Math.abs(s); p.rArmZ = 0.9f * s; p.lArmX = -0.5f; p.bodyY = 0.7f * s; p.headY = -0.35f * s; p.lLegX = -0.4f; p.rLegX = 0.3f; p.rootZ = -1f;
                }));

        // Ichimonji: heavy drawing slash
        out.add(AttackDef.of("ichimonji").range(0, 3.8f).cd(90).weight(2).dur(28).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.8f, 1.4f);
                    if (n == 14) { Combat.lunge(e, 0.8, 0); slash(e, 90, -90, 0, 0xffffff); melee(e, 3.8, 16, 1.0, 0.25, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.8f); }
                }).anim((p, t, a) -> {
                    float w = win(t, .1f, .45f) * (1 - win(t, .5f, .55f));
                    p.rArmX = track(t, 0, -0.2f, .4f, -0.6f, .5f, -1.5f, .7f, -1.4f, 1, -0.2f); p.rArmZ = track(t, 0, 0.1f, .4f, 1.1f, .55f, -0.8f, 1, 0.1f);
                    p.bodyY = track(t, 0, 0, .4f, 0.9f, .55f, -0.8f, 1, 0); p.headY = -p.bodyY * 0.5f; p.rootY -= 1.5f * w; p.lLegX = -0.6f * w; p.rLegX = 0.5f * w; p.rootZ = -2f * bump(win(t, .4f, .75f));
                }));

        // Ashina Cross: spinning cross-cut
        out.add(AttackDef.of("ashina_cross").range(0, 3.6f).cd(110).weight(1).dur(30).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 8 || n == 14) { slash(e, n == 8 ? 80 : -80, n == 8 ? -80 : 80, n == 8 ? 40 : -40, 0xffffff); melee(e, 3.6, 8, 0.5, 0.1, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.9f); }
                }).anim((p, t, a) -> { float s = bump(win(t, .15f, .7f)); p.rootYaw += 6.28f * win(t, .2f, .6f); p.rArmX = -1.5f * s; p.rArmZ = 1.2f * s; p.rootY -= 0.5f * s; }));

        // Mortal Draw: iaijutsu counter, lunges and cuts
        out.add(AttackDef.of("mortal_draw").range(2, 7).cd(160).weight(2).dur(26).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 1) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 14, 4));
                    if (n == 13) { Combat.lunge(e, 1.5, 0); slash(e, 100, -100, 0, GOLD); }
                    if (n == 15) { melee(e, 4, 20, 1.3, 0.3, GOLD); Fx.ring(L(e), e.position().add(0, 1, 0), 3f, GOLD, 1.6f, 24); snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.8f); }
                }).anim((p, t, a) -> {
                    float h = win(t, .1f, .45f) * (1 - win(t, .5f, .55f));
                    p.rArmX = lerp(0.6f * h, -1.5f, win(t, .5f, .6f)) * 1f; p.rArmZ = -0.4f * h; p.bodyY = 0.6f * h - 1.2f * win(t, .5f, .7f) * (1 - win(t, .8f, 1f)); p.rootY -= 1.5f * h; p.lLegX = -0.5f * h; p.rLegX = 0.4f * h;
                    p.bodyX += 0.5f * win(t, .5f, .75f) * (1 - win(t, .8f, 1f));
                }));

        // Mikiri Counter: thrust-punish stomp
        out.add(AttackDef.of("mikiri_counter").range(0, 3f).cd(100).weight(2).dur(20)
                .tick((e, t, n) -> {
                    if (n == 6) {
                        for (LivingEntity f : Combat.around(e, e.position(), 2.8)) { Combat.damage(e, f, 11); f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3)); Combat.knock(f, e.position(), 0.4, 0.2); }
                        Fx.burst(L(e), ParticleTypes.POOF, e.position().add(0, 0.1, 0), 8, 0.4, 0.05); snd(e, SoundEvents.IRON_GOLEM_STEP, 1.2f, 0.6f);
                    }
                }).anim((p, t, a) -> { float s = bump(win(t, .15f, .75f)); p.rLegX = -1.7f * s; p.bodyX -= 0.4f * s; p.rootY += 0.4f * s; p.rArmX = -0.6f * s; p.lArmX = -0.6f * s; }));

        // Grappling hook: zip to the target
        out.add(AttackDef.of("grapple").range(5, 20).cd(120).weight(2).dur(24).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n >= 2 && n < 8 && t != null) Fx.beam(L(e), e.position().add(0, 1.5, 0), mid(t), 0x907050, 0.3f, 0.3);
                    if (n == 6 && t != null) { Vec3 d = t.position().subtract(e.position()).normalize(); e.setDeltaMovement(d.x * 1.5, 0.35, d.z * 1.5); e.hurtMarked = true; snd(e, SoundEvents.CROSSBOW_SHOOT, 0.8f, 0.7f); }
                    if (n == 14) { slash(e, 80, -80, 30, 0xffffff); melee(e, 3.6, 10, 0.7, 0.3, 0xffffff); }
                }).anim((p, t, a) -> { float k = bump(win(t, .05f, .45f)); p.lArmX = -2.2f * k; p.lArmZ = -0.2f * k; p.bodyX += 0.3f * win(t, .25f, .6f) * (1 - win(t, .6f, .8f)); p.rArmX = lerp(p.rArmX, -1.4f, win(t, .5f, .6f) * (1 - win(t, .8f, 1f))); }));

        // Prosthetic tools
        out.add(AttackDef.of("shuriken").range(4, 20).cd(50).weight(2).dur(16)
                .tick((e, t, n) -> { if (n == 4 || n == 8 || n == 12) { Combat.shoot(e, SHURIKEN, mz(e), aim(e, t, 0.05), 1.9, 4f); snd(e, SoundEvents.TRIDENT_THROW, 0.7f, 2f); } })
                .anim((p, t, a) -> { float k = (float) Math.sin(t * PI * 3f); p.lArmX = -1.4f; p.lArmZ = -0.1f + 0.2f * Math.abs(k); p.bodyY = 0.2f * k; }));
        out.add(AttackDef.of("firecracker").range(0, 8).cd(180).weight(1).dur(18)
                .tick((e, t, n) -> {
                    if (n == 6) {
                        Vec3 c = e.position().add(e.getViewVector(1f).scale(2)).add(0, 1.2, 0);
                        Fx.explosion(L(e), c, 1.2f); Fx.burst(L(e), ParticleTypes.FLAME, c, 20, 0.6, 0.05); snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 1.4f, 0.6f);
                        for (LivingEntity f : Combat.around(e, c, 4)) { f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3)); f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1)); Combat.damage(e, f, 2); }
                    }
                }).anim((p, t, a) -> { float k = bump(win(t, .1f, .7f)); p.lArmX = -1.6f * k; p.lArmZ = -0.5f * k; p.bodyX -= 0.2f * k; }));
        out.add(AttackDef.of("loaded_axe").range(0, 3.4f).cd(120).weight(1).dur(30)
                .tick((e, t, n) -> {
                    if (n == 14) { for (LivingEntity f : Combat.cone(e, 3.4, 50)) { Combat.damage(e, f, 15); Combat.knock(f, e.position(), 0.8, 0.2); Fx.sparks(L(e), mid(f), 8); } slash(e, 60, -60, 50, 0xaaaaaa); snd(e, SoundEvents.ANVIL_LAND, 0.8f, 1.2f); }
                }).anim((p, t, a) -> {
                    p.lArmX = track(t, 0, 0, .3f, -2.9f, .5f, -0.8f, 1, 0); p.lArmZ = track(t, 0, 0, .3f, -0.4f, .5f, 0.2f, 1, 0);
                    p.bodyX = track(t, 0, 0, .3f, -0.3f, .5f, 0.5f, 1, 0); p.rootZ = -1.5f * bump(win(t, .3f, .7f));
                }));
        out.add(AttackDef.of("loaded_spear").range(2, 9).cd(110).weight(1).dur(26)
                .tick((e, t, n) -> { if (n == 9) { Combat.lunge(e, 0.7, 0); Combat.shoot(e, SPEAR, mz(e), aim(e, t, 0), 1.5, 14f); snd(e, SoundEvents.TRIDENT_THROW, 1f, 0.9f); } })
                .anim((p, t, a) -> { float k = bump(win(t, .1f, .9f)); p.lArmX = -1.55f * k; p.bodyX += 0.3f * win(t, .35f, .55f) * (1 - win(t, .6f, 1f)); p.rootZ = -1.8f * bump(win(t, .3f, .6f)); }));
        out.add(AttackDef.of("flame_vent").range(1, 7).cd(150).weight(1).dur(40)
                .tick((e, t, n) -> {
                    if (n >= 6 && n < 30) {
                        Vec3 m = mz(e), d = aim(e, t, 0.04);
                        for (int i = 1; i <= 5; i++) Fx.burst(L(e), ParticleTypes.FLAME, m.add(d.scale(i * 1.1)), 3, 0.2 + i * 0.05, 0.02);
                        if (n % 4 == 0) for (LivingEntity f : Combat.cone(e, 6.5, 22)) { Combat.damage(e, f, 3); f.setSecondsOnFire(5); }
                        if (n % 6 == 0) snd(e, SoundEvents.FIRE_AMBIENT, 1f, 0.8f);
                    }
                }).anim((p, t, a) -> { float k = ready(t, .15f, .85f); p.lArmX = lerp(p.lArmX, -1.5f, k); p.rArmX = lerp(p.rArmX, -0.4f, k); p.bodyX += 0.1f * k; p.rootY -= 0.4f * k; }));

        // Shinobi execution (deathblow)
        out.add(AttackDef.of("deathblow").range(0, 3.4f).lowHealthTarget(0.3f).cd(40).weight(5).dur(24).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 2) Combat.lunge(e, 0.8, 0.02);
                    if (n == 10 && t != null) { Fx.dustBurst(L(e), 0xb01010, 1.5f, mid(t), 30, 0.4); slash(e, 90, -90, 0, 0xffffff); Combat.damage(e, t, t.getHealth() + 50); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 0.5f); }
                }).anim((p, t, a) -> stab(p, t, 10 / 24f)));
    }

    @Override
    public void buildModel(Rig rig) {
        Mat cloth = Mat.of(0x3a3a42).noise(.12f), dark = Mat.of(0x1c1c22).noise(.08f), skin = Mat.of(0xd8b898).noise(.04f).bevel(.1f), hair = Mat.of(0xdcdcdc).noise(.12f);
        Mat red = Mat.of(0xa02020).noise(.1f), steel = Mat.of(0xc8ccd0).bevel(.1f), prost = Mat.of(0x6a5a40).noise(.12f), gold = Mat.of(GOLD).glow();
        rig.box(rig.torso, cloth, -4, -12, -2, 8, 12, 4, .1f);
        rig.box(rig.torso, dark, -4, -6, -2.5f, 8, 3, 5);
        rig.box(rig.torso, red, -4.3f, -12, -0.5f, 1, 12, 1); rig.box(rig.torso, red, 3.3f, -12, -0.5f, 1, 12, 1); // scarf tails
        rig.box(rig.torso, dark, -3, -11, 2, 6, 8, 1); // sheathed blade pack
        rig.box(rig.head, skin, -4, -8, -4, 8, 8, 8);
        rig.box(rig.head, hair, -4, -8, -4, 8, 3, 8, .3f); // white ponytail hair
        rig.box(rig.head, hair, -2, -8, 3, 4, 9, 2, .2f);
        rig.box(rig.head, red, -4, -3, -4, 8, 3, 8, .3f); // scarf/mask
        rig.box(rig.head, Mat.of(0x101010), -3, -5, -4.2f, 6, 1, 1);
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm, leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(arm, cloth, -2, -2, -2, 4, 9, 4, .1f);
            rig.box(arm, s < 0 ? skin : prost, -2, 7, -2, 4, 3, 4, s < 0 ? 0 : .2f); // left: shinobi prosthetic
            rig.box(leg, dark, -2, 0, -2, 4, 11, 4);
            rig.box(leg, cloth, -2, 8, -2, 4, 4, 4, .2f);
        }
        rig.box(rig.lArm, gold, -1, 9, -2.5f, 2, 1, 1);
        PartDefinition w = rig.weapon(rig.rHand, "kusabimaru");
        rig.box(w, steel, -0.4f, -2.5f, -17, 1, 1, 15); rig.box(w, Mat.of(0x303030), -1.5f, -3, -3, 3, 2, 1); rig.box(w, red, -0.5f, -2.5f, -2, 1, 1, 5);
    }
}
