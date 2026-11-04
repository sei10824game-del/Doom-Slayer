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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Dead Space protagonists, one kit per game. Weapons are picked per game by id list. */
public abstract class DeadSpaceKits extends SlayerKit {
    static final int BLUE = 0x4ad8ff, ORANGE = 0xffa030, STASIS = 0x40a0ff;
    static final ProjStyle RIPPER = ProjStyle.make().color(0xc8c8c8).size(0.5f).life(50).pierce(5).bounce().trail(ParticleTypes.CRIT);
    static final ProjStyle JAVELIN = ProjStyle.make().color(0x9ad0ff).size(0.2f).life(60).pierce(2).trail(ParticleTypes.ELECTRIC_SPARK);
    static final ProjStyle MINE = ProjStyle.make().color(0xff5050).size(0.3f).life(60).gravity(0.05).explode(3.8f).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle SPIKE = ProjStyle.make().color(0x8a8a8a).size(0.22f).life(40).pierce(1).trail(ParticleTypes.CRIT);
    static final ProjStyle CUT = ProjStyle.make().color(BLUE).size(0.2f).life(30).trail(ParticleTypes.ELECTRIC_SPARK);

    final String[] weapons; final int suit, trim, glow;

    DeadSpaceKits(String id, String name, String ja, String game, int suit, int trim, int glow, double hp, String... weapons) {
        super(id, name, ja, game);
        this.weapons = weapons; this.suit = suit; this.trim = trim; this.glow = glow;
        eggPrimary = suit; eggSecondary = glow; maxHealth = hp; speed = 0.29; armor = 14; knockbackRes = 0.5;
        scale = 1.03f; preferredRange = 9f; dmgMult = 1.0f; idleWeaponR = weapons[0];
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        p.rArmX = -0.5f + p.rArmX * 0.5f; p.lArmX = -0.3f + p.lArmX * 0.5f; p.bodyX += 0.08f; p.headX += 0.05f; // wary hunch
    }

    @Override
    public void passive(SlayerEntity e) {
        // RIG: spine meter doubles as health; stasis recharge and med-pack when critical
        if (e.tickCount % 20 == 0) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false));
        if (e.tickCount % 300 == 0 && e.getHealth() < e.getMaxHealth() * 0.4f) {
            e.heal(e.getMaxHealth() * 0.4f); snd(e, SoundEvents.BREWING_STAND_BREW, 0.8f, 1.4f);
            Fx.dustBurst(L(e), glow, 1f, e.position().add(0, 1.2, 0), 14, 0.4);
        }
    }

    private static AttackDef gun(String id, String w, float min, float max, int cd, int dur, int[] shots, SoundEvent s, float pitch,
            int pellets, double spread, float dmg, int rgb, boolean rifle) {
        return AttackDef.of(id).range(min, max).cd(cd).weight(3).dur(dur).weapon(w)
                .tick((e, t, n) -> {
                    for (int sh : shots) if (n == sh) {
                        Vec3 m = mz(e);
                        for (int i = 0; i < pellets; i++) Combat.hitscan(e, m, aim(e, t, spread), 24, dmg, 0, rgb);
                        flash(e, rgb); snd(e, s, 0.8f, pitch);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), r = 0;
                    for (int sh : shots) r = Math.max(r, 1.5f * kick(t, sh / (float) dur, .22f));
                    if (rifle) shoulder(p, k, 0, r); else pistol2(p, k, 0, r);
                    stance(p, k);
                });
    }

    /** every attack Isaac & co. can have; `weapons` says which are active in this game. */
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        for (String w : weapons) switch (w) {
            case "cutter" -> {
                out.add(gun("plasma_cutter", "cutter", 3, 22, 16, 12, new int[] {4}, SoundEvents.BEACON_POWER_SELECT, 2f, 1, 0.01, 5.5f, BLUE, false));
                // limb-cutting: vertical then horizontal beam pair
                out.add(AttackDef.of("cutter_dismember").range(3, 16).cd(70).weight(2).dur(26).weapon("cutter")
                        .tick((e, t, n) -> {
                            if (n == 5 || n == 12) {
                                Vec3 d = aim(e, t, 0), m = mz(e);
                                Combat.hitscan(e, m, d, 22, 6f, 1, BLUE); Fx.beam(L(e), m, m.add(d.scale(18)), BLUE, 0.5f, 0.25);
                                if (n == 12 && t != null) Fx.sparks(L(e), mid(t), 6);
                                snd(e, SoundEvents.BEACON_POWER_SELECT, 0.9f, n == 5 ? 1.8f : 2f);
                            }
                        }).anim((p, t, a) -> {
                            float k = ready(t, .12f, .9f); pistol2(p, k, 0, 1.2f * (kick(t, 5 / 26f, .2f) + kick(t, 12 / 26f, .2f))); stance(p, k);
                            p.rArmZ += 0.5f * win(t, .25f, .45f) * k; // roll the cutter 90 degrees
                            p.rHandY = 1.4f * win(t, .25f, .45f) * (1 - win(t, .8f, 1f));
                        }));
            }
            case "pulse" -> {
                out.add(gun("pulse_rifle", "pulse", 3, 24, 40, 24, new int[] {3, 5, 7, 9, 11, 13, 15}, SoundEvents.DISPENSER_LAUNCH, 1.4f, 1, 0.06, 2.6f, 0xffd070, true));
                out.add(AttackDef.of("pulse_grenade").range(5, 22).cd(110).weight(1).dur(22).weapon("pulse")
                        .tick((e, t, n) -> { if (n == 8) { Combat.shoot(e, MINE, mz(e), aim(e, t, 0.01).add(0, 0.1, 0).normalize(), 1.1, 9f); flash(e, ORANGE); snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.9f, 0.7f); } })
                        .anim((p, t, a) -> { float k = ready(t, .15f, .85f); shoulder(p, k, 0.25f, 2f * kick(t, 8 / 22f, .3f)); stance(p, k); }));
            }
            case "rifle" -> {
                out.add(gun("assault_rifle", "rifle", 3, 26, 40, 24, new int[] {3, 5, 7, 9, 11, 13, 15, 17}, SoundEvents.DISPENSER_LAUNCH, 1.3f, 1, 0.05, 2.8f, 0xffd070, true));
                out.add(gun("rifle_shotgun", "rifle", 0, 10, 60, 24, new int[] {6}, SoundEvents.GENERIC_EXPLODE, 1.8f, 7, 0.1, 2.8f, 0xffd070, true)); // secondary fire
            }
            case "flame" -> out.add(AttackDef.of("flamethrower").range(1, 9).cd(110).weight(2).dur(50).weapon("flame")
                    .tick((e, t, n) -> {
                        if (n >= 6 && n < 44) {
                            Vec3 m = mz(e), d = aim(e, t, 0.05);
                            for (int i = 1; i <= 6; i++) Fx.burst(L(e), ParticleTypes.FLAME, m.add(d.scale(i * 1.2)), 3, 0.2 + i * 0.05, 0.02);
                            if (n % 4 == 0) for (LivingEntity f : Combat.cone(e, 8, 22)) { Combat.damage(e, f, 3); f.setSecondsOnFire(4); }
                            if (n % 6 == 0) snd(e, SoundEvents.FIRE_AMBIENT, 1f, 0.8f);
                        }
                    }).anim((p, t, a) -> { float k = ready(t, .12f, .9f); shoulder(p, k, 0.05f, 0.25f * win(t, .12f, .9f)); stance(p, k); p.rootY -= 0.4f * k; }));
            case "line" -> out.add(AttackDef.of("line_gun").range(2, 14).cd(90).weight(2).dur(30).weapon("line")
                    .tick((e, t, n) -> {
                        if (n == 8) {
                            Vec3 d = aim(e, t, 0), m = mz(e);
                            for (int i = 0; i < 3; i++) Combat.hitscan(e, m.add(0, 0.2 * (i - 1), 0), d, 16, 9f, 8, 0xff8040);
                            for (int i = 1; i <= 10; i++) Fx.dustBurst(L(e), 0xff8040, 1.2f, m.add(d.scale(i * 1.4)), 2, 0.1);
                            snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 1f, 0.7f);
                        }
                    }).anim((p, t, a) -> { float k = ready(t, .15f, .88f); shoulder(p, k, 0, 2.4f * kick(t, 8 / 30f, .3f)); stance(p, k); }));
            case "ripper" -> out.add(AttackDef.of("ripper").range(3, 16).cd(80).weight(2).dur(36).weapon("ripper")
                    .tick((e, t, n) -> {
                        if (n >= 6 && n < 12) { Fx.burst(L(e), ParticleTypes.CRIT, mz(e), 3, 0.2, 0.1); if (n % 2 == 0) snd(e, SoundEvents.GRINDSTONE_USE, 0.7f, 1.8f); }
                        if (n == 14) { Combat.shoot(e, RIPPER, mz(e), aim(e, t, 0), 1.0, 11f); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.6f); }
                    }).anim((p, t, a) -> { float k = ready(t, .15f, .85f); pistol2(p, k, 0, 1.4f * kick(t, 14 / 36f, .3f)); stance(p, k); p.rHandY += 0.3f * (float) Math.sin(a * 6f) * k * (1 - win(t, .4f, .5f)); }));
            case "force" -> out.add(AttackDef.of("force_gun").range(0, 7).cd(70).weight(2).dur(24).weapon("force")
                    .tick((e, t, n) -> {
                        if (n == 7) {
                            for (LivingEntity f : Combat.cone(e, 7, 35)) { Combat.damage(e, f, 8); Combat.knock(f, e.position(), 2.0, 0.5); }
                            Fx.ring(L(e), mz(e), 1.2f, 0xa0e0ff, 1.8f, 20); snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.7f, 1.8f);
                        }
                    }).anim((p, t, a) -> { float k = ready(t, .15f, .85f); pistol2(p, k, 0, 2.6f * kick(t, 7 / 24f, .3f)); stance(p, k); }));
            case "contact" -> out.add(AttackDef.of("contact_beam").range(4, 22).cd(180).weight(1).dur(44).weapon("contact")
                    .tick((e, t, n) -> {
                        if (n >= 4 && n < 24) { Fx.dustBurst(L(e), glow, 1f, mz(e), 3, 0.2 + (24 - n) * 0.02); if (n % 5 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.9f, 0.8f + n * 0.03f); }
                        if (n == 26) {
                            Vec3 d = aim(e, t, 0), m = mz(e);
                            Combat.hitscan(e, m, d, 40, 32f, 6, 0xffffff); Fx.beam(L(e), m, m.add(d.scale(36)), 0xffffff, 1.5f, 0.4);
                            Fx.ring(L(e), m, 1.3f, glow, 1.8f, 20); snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.9f, 1.4f);
                        }
                    }).anim((p, t, a) -> { float k = ready(t, .1f, .92f); shoulder(p, k, 0, 3f * kick(t, 26 / 44f, .3f)); stance(p, k); p.rootY -= 0.7f * k; }));
            case "javelin" -> {
                out.add(AttackDef.of("javelin_gun").range(4, 26).cd(60).weight(2).dur(24).weapon("javelin")
                        .tick((e, t, n) -> { if (n == 7) { Combat.shoot(e, JAVELIN, mz(e), aim(e, t, 0), 2.2, 14f); snd(e, SoundEvents.TRIDENT_THROW, 1f, 1.6f); } })
                        .anim((p, t, a) -> { float k = ready(t, .15f, .88f); shoulder(p, k, 0, 2f * kick(t, 7 / 24f, .3f)); stance(p, k); }));
                // electrified javelin: skewer + lightning
                out.add(AttackDef.of("javelin_shock").range(3, 14).cd(160).weight(1).dur(40).weapon("javelin")
                        .tick((e, t, n) -> {
                            if (n == 10) Combat.shoot(e, JAVELIN, mz(e), aim(e, t, 0), 2.2, 12f);
                            if (n == 20 && t != null) { Combat.blast(e, t.position(), 4f, 10, 0.3f, null); Fx.explosion(L(e), mid(t), 2f); snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.7f, 1.8f); }
                        }).anim((p, t, a) -> { float k = ready(t, .15f, .85f); shoulder(p, k, 0, 2f * kick(t, 10 / 40f, .3f)); stance(p, k); }));
            }
            case "seeker" -> out.add(AttackDef.of("seeker_rifle").range(8, 40).cd(90).weight(2).dur(30).weapon("seeker")
                    .tick((e, t, n) -> {
                        if (n >= 6 && n < 16 && t != null) Fx.beam(L(e), mz(e), mid(t), 0xff3030, 0.2f, 0.4);
                        if (n == 18) { Combat.hitscan(e, mz(e), aim(e, t, 0), 48, 20f, 2, 0xffffff); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1f, 0.7f); flash(e, 0xffffff); }
                    }).anim((p, t, a) -> { float k = ready(t, .15f, .88f); shoulder(p, k, 0.04f, 2.2f * kick(t, 18 / 30f, .3f)); stance(p, k); }));
            case "detonator" -> out.add(AttackDef.of("detonator").range(4, 16).cd(120).weight(1).dur(34).weapon("detonator")
                    .tick((e, t, n) -> {
                        if (n == 8) { for (int i = 0; i < 3; i++) Combat.shoot(e, MINE, mz(e), aim(e, t, 0.15).add(0, 0.15, 0).normalize(), 0.7 + 0.1 * i, 8f); snd(e, SoundEvents.PISTON_EXTEND, 1f, 1.5f); }
                        if (n == 26) snd(e, SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 2f);
                    }).anim((p, t, a) -> { float k = ready(t, .15f, .88f); pistol2(p, k, 0.1f, 1.5f * kick(t, 8 / 34f, .3f)); stance(p, k); }));
            default -> {}
        }
        // ---- every Engineer has these ----
        out.add(AttackDef.of("stasis").range(2, 14).cd(200).weight(1).dur(26)
                .tick((e, t, n) -> {
                    if (n == 8 && t != null) {
                        for (LivingEntity f : Combat.around(e, t.position(), 4)) { f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 140, 4)); f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 0)); }
                        Fx.ring(L(e), t.position().add(0, 1, 0), 3f, STASIS, 1.6f, 24); Fx.dustBurst(L(e), STASIS, 1.4f, t.position().add(0, 1, 0), 20, 0.8);
                        snd(e, SoundEvents.GLASS_BREAK, 0.6f, 0.6f); snd(e, SoundEvents.BEACON_ACTIVATE, 0.7f, 1.8f);
                    }
                }).anim((p, t, a) -> { float k = ready(t, .2f, .85f); p.lArmX = lerp(p.lArmX, -1.5f, k); p.lArmZ = lerp(p.lArmZ, -0.2f, k); p.lHandX = -0.6f * k; p.rArmX -= 0.2f * k; stance(p, k); }));

        out.add(AttackDef.of("kinesis_impale").range(3, 16).cd(90).weight(2).dur(30)
                .tick((e, t, n) -> {
                    if (n >= 3 && n < 12) Fx.dustBurst(L(e), BLUE, 0.9f, e.position().add(0, 1.2 + (n - 3) * 0.1, 0).add(e.getViewVector(1f).scale(1.6)), 2, 0.2);
                    if (n == 15) { Combat.shoot(e, SPIKE, mz(e).add(0, 0.5, 0), aim(e, t, 0.01), 1.9, 12f); snd(e, SoundEvents.TRIDENT_THROW, 0.9f, 1.4f); }
                }).anim((p, t, a) -> { float k = ready(t, .15f, .85f), th = bump(win(t, .4f, .65f)); p.lArmX = lerp(p.lArmX, -1.6f - 0.4f * th, k); p.lHandX = 0.5f * k; p.rArmX -= 0.1f * k; p.bodyX -= 0.25f * th; stance(p, k); }));

        out.add(AttackDef.of("stomp").range(0, 3f).cd(50).weight(3).dur(22)
                .tick((e, t, n) -> {
                    if (n == 9) {
                        for (LivingEntity f : Combat.around(e, e.position(), 3)) { Combat.damage(e, f, 8); Combat.knock(f, e.position(), 0.3, 0.1); }
                        Fx.burst(L(e), ParticleTypes.POOF, e.position().add(0, 0.1, 0), 8, 0.4, 0.05); Fx.dustBurst(L(e), 0x8a0303, 1.2f, e.position().add(0, 0.2, 0), 12, 0.4);
                        snd(e, SoundEvents.IRON_GOLEM_STEP, 1.2f, 0.6f);
                    }
                }).anim((p, t, a) -> {
                    float up = win(t, .1f, .38f) * (1 - win(t, .4f, .44f)), dn = bump(win(t, .4f, .6f));
                    p.rLegX = -1.6f * up; p.rootY += 0.8f * up - 1f * dn; p.bodyX -= 0.2f * up; p.rArmX = -0.5f * up; p.lArmX = -0.5f * up;
                }));

        out.add(AttackDef.of("melee_smash").range(0, 3f).cd(22).weight(3).dur(18)
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 0.8f);
                    if (n == 7) { slash(e, 50, -50, 20, 0xaaaaaa); melee(e, 3.1, 7, 0.9, 0.2, 0xaaaaaa); snd(e, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.6f, 1f); }
                }).anim((p, t, a) -> swing(p, t, 7 / 18f, 1f)));
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat suitM = Mat.of(suit).bevel(.3f).noise(.1f), trimM = Mat.of(trim).bevel(.25f).noise(.08f), dark = Mat.of(0x1c1e20).noise(.06f);
        Mat gl = Mat.of(glow).glow(), metal = Mat.of(0x7d8084).noise(.06f);
        Mat chest = Mat.of(suit).bevel(.3f).noise(.1f);
        rig.box(rig.torso, dark, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4.5f, -12, -3, 9, 7, 2, .3f);
        rig.box(rig.torso, trimM, -3, -5.5f, -2.6f, 6, 3, 1);
        rig.box(rig.torso, gl, 3.5f, -9, -3.2f, 1, 4, 1); // RIG holo-gauge
        rig.box(rig.torso, trimM, -4, -11.5f, 2, 8, 9, 3); // backpack
        rig.box(rig.torso, gl, -0.5f, -10.5f, 4.6f, 1, 9, 1); // spine meter
        rig.box(rig.torso, dark, -4.5f, -2.5f, -2.5f, 9, 2, 5);
        // helmet: engineer, glowing visor
        rig.box(rig.head, suitM, -4.5f, -8.5f, -4.5f, 9, 9, 9, .3f);
        rig.box(rig.head, dark, -3.5f, -6.5f, -5.1f, 7, 4, 1);
        rig.box(rig.head, gl, -3f, -5.5f, -5.4f, 6, 1, 1);
        rig.box(rig.head, trimM, -1, -9f, -3, 2, 1, 6);
        rig.box(rig.head, metal, -5f, -5f, -1.5f, 1, 3, 3);
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm, leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(arm, dark, -2, -2, -2, 4, 12, 4);
            rig.box(arm, suitM, s < 0 ? -3.5f : -2.5f, -3, -3, 6, 5, 6, .2f);
            rig.box(arm, trimM, -2, 4, -2, 4, 4, 4, .4f);
            rig.box(arm, dark, -2, 8, -2, 4, 2, 4, .2f);
            rig.box(leg, dark, -2, 0, -2, 4, 12, 4);
            rig.box(leg, suitM, -2, 1, -2, 4, 5, 4, .4f);
            rig.box(leg, trimM, -2, 6.5f, -2.7f, 4, 3, 1);
            rig.box(leg, dark, -2, 10, -3, 4, 2, 5, .3f);
        }
        rig.box(rig.rLeg, gl, 2f, 2f, -0.5f, 1, 2, 1);
        weapons(rig, metal, dark, gl);
    }

    private static void weapons(Rig rig, Mat metal, Mat dark, Mat gl) {
        Mat orange = Mat.of(ORANGE).glow(), hot = Mat.of(0xff5030).glow(), olive = Mat.of(0x4a5238).noise(.1f), steel = Mat.of(0xa0a4a8).noise(.05f);
        PartDefinition w = rig.weapon(rig.rHand, "cutter");
        rig.box(w, dark, -1.5f, -3, -7, 3, 3, 8); rig.box(w, metal, -0.5f, -2.5f, -10, 1, 1, 3); rig.box(w, gl, -2, -2.5f, -6, 4, 1, 1); rig.box(w, gl, -2, -2.5f, -9, 4, 1, 1);
        rig.box(w, dark, -1, 0, -1, 2, 3, 2);
        w = rig.weapon(rig.rHand, "pulse");
        rig.box(w, olive, -2, -4, -9, 4, 4, 11); rig.box(w, dark, -0.5f, -3, -14, 1, 1, 5); rig.box(w, orange, -2.4f, -3.5f, -6, 1, 2, 3); rig.box(w, dark, -1, 0, -2, 2, 3, 2); rig.box(w, metal, -1, -5, -6, 2, 1, 5);
        w = rig.weapon(rig.rHand, "rifle");
        rig.box(w, dark, -1.5f, -4, -9, 3, 4, 12); rig.box(w, metal, -0.5f, -3, -15, 1, 1, 6); rig.box(w, steel, -1, -5.5f, -6, 2, 2, 5); rig.box(w, dark, -1, 0, -2, 2, 3, 2); rig.box(w, dark, -1, 0, -7, 2, 3, 2);
        w = rig.weapon(rig.rHand, "flame");
        rig.box(w, dark, -2, -4, -8, 4, 4, 10); rig.box(w, metal, -1, -3.5f, -14, 2, 2, 6); rig.box(w, hot, -0.5f, -3, -15, 1, 1, 1); rig.box(w, Mat.of(0xb03020).noise(.1f), -2, -7, -5, 4, 3, 4);
        w = rig.weapon(rig.rHand, "line");
        rig.box(w, dark, -2, -4, -8, 4, 4, 10); rig.box(w, orange, -2.5f, -3.5f, -12, 5, 1, 4); rig.box(w, orange, -2.5f, -1.5f, -12, 5, 1, 4); rig.box(w, metal, -1, -3, -13, 2, 2, 2);
        w = rig.weapon(rig.rHand, "ripper");
        rig.box(w, dark, -2, -4, -7, 4, 4, 8); rig.box(w, steel, -4, -6, -11, 8, 8, 1); rig.box(w, metal, -1, -3, -10, 2, 2, 3);
        w = rig.weapon(rig.rHand, "force");
        rig.box(w, dark, -2.5f, -4, -6, 5, 5, 8); rig.box(w, Mat.of(0xa0e0ff).glow(), -3, -4.5f, -9, 6, 6, 3); rig.box(w, metal, -1, -2, -3, 2, 2, 1);
        w = rig.weapon(rig.rHand, "contact");
        rig.box(w, steel, -2, -4, -8, 4, 4, 10); rig.box(w, gl, -1.5f, -3.5f, -14, 3, 3, 6); rig.box(w, dark, -2.5f, -5, -6, 5, 2, 4);
        w = rig.weapon(rig.rHand, "javelin");
        rig.box(w, dark, -2, -4, -8, 4, 4, 10); rig.box(w, steel, -0.5f, -3, -18, 1, 1, 10); rig.box(w, gl, -2.4f, -3.5f, -6, 1, 2, 3);
        w = rig.weapon(rig.rHand, "seeker");
        rig.box(w, olive, -1.5f, -3, -8, 3, 3, 10); rig.box(w, dark, -0.5f, -2.5f, -17, 1, 1, 9); rig.box(w, dark, -1, -6, -7, 2, 3, 5); rig.box(w, hot, -0.5f, -5.5f, -8, 1, 1, 1); rig.box(w, dark, -1, 0, -1, 2, 3, 2);
        w = rig.weapon(rig.rHand, "detonator");
        rig.box(w, dark, -1.5f, -3, -6, 3, 3, 8); rig.box(w, hot, -1, -4, -5, 2, 1, 4); rig.box(w, metal, -1, -2.5f, -9, 2, 2, 3); rig.box(w, dark, -1, 0, -1, 2, 3, 2);
    }

    // ================= kits =================
    public static class Ds1Isaac extends DeadSpaceKits {
        public Ds1Isaac() { super("dead_space_isaac", "Isaac Clarke (DS1)", "アイザック・クラーク (DS1)", "Dead Space", 0x6a5a40, 0x8a8a8a, BLUE, 100,
                "cutter", "pulse", "flame", "line", "ripper", "force", "contact"); }
    }
    public static class Ds2Isaac extends DeadSpaceKits {
        public Ds2Isaac() { super("dead_space2_isaac", "Isaac Clarke (DS2)", "アイザック・クラーク (DS2)", "Dead Space 2", 0x4a4e56, 0xb83030, BLUE, 105,
                "cutter", "javelin", "seeker", "detonator", "pulse", "flame", "line", "ripper", "contact"); }
    }
    public static class Ds3Isaac extends DeadSpaceKits {
        public Ds3Isaac() { super("dead_space3_isaac", "Isaac Clarke (DS3)", "アイザック・クラーク (DS3)", "Dead Space 3", 0xd8dde0, 0x707880, ORANGE, 115,
                "rifle", "cutter", "flame", "line", "force", "contact", "javelin", "ripper"); }
    }
    public static class Ds3Carver extends DeadSpaceKits {
        public Ds3Carver() { super("dead_space3_carver", "John Carver (DS3)", "ジョン・カーヴァー (DS3)", "Dead Space 3", 0x4a5a3a, 0x2c3326, ORANGE, 125,
                "rifle", "pulse", "flame", "seeker", "detonator"); }
    }
    public static class DsExtractionMcNeill extends DeadSpaceKits {
        public DsExtractionMcNeill() { super("dead_space_extraction_mcneill", "Nathan McNeill", "ネイサン・マクニール", "Dead Space: Extraction", 0x2e3a52, 0xd0d6dc, 0x60ff90, 95,
                "cutter", "pulse", "flame", "line", "ripper", "force"); }
    }
}
