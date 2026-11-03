package com.doomslayer.kit.kits;

import static com.doomslayer.anim.Keys.*;

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
import java.util.Random;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Stan Blazkowicz, the UAC marine of Doom RPG (id Software / Fountainhead, 2005 mobile).
 * Blue-grey UAC armour, helmet lamp. Turn-based dungeon crawler arsenal: fists, axe, chainsaw, pistol, shotgun,
 * super shotgun, chaingun, rocket launcher, plasma gun, BFG 9000, soul cube, frag grenade, medkit.
 */
public class DoomRpgMarine extends SlayerKit {
    private static final Random R = new Random();

    /** Soul Cube: homing cube that rips the soul out of its target and gives the health to the marine. */
    static final ProjStyle SOUL_CUBE = ProjStyle.make().color(0xff4a2a).size(0.55f).life(80).homing(0.3f)
            .trail(ParticleTypes.SOUL_FIRE_FLAME).onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.burst(l, ParticleTypes.SOUL, pos, 20, 0.35, 0.04);
                Fx.dustBurst(l, 0xff3020, 1.4f, pos, 14, 0.3);
                if (hit != null && p.getOwner() instanceof SlayerEntity o) {
                    o.heal(p.damage * 0.6f);
                    Fx.beam(l, pos, o.position().add(0, 1.1, 0), 0x50e0ff, 0.9f, 0.35);
                    Fx.burst(l, ParticleTypes.HEART, o.position().add(0, 2.1, 0), 5, 0.4, 0);
                    Fx.sound(l, o.position(), SoundEvents.SOUL_ESCAPE, 1.4f, 0.7f);
                }
            });
    /** BFG 9000 ball: huge green explosion with double shock ring. */
    static final ProjStyle BFG_BALL = ProjStyle.make().color(0x60ff40).size(1.8f).life(90).explode(6.5f)
            .trail(ParticleTypes.HAPPY_VILLAGER).onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 4f);
                Fx.ring(l, pos, 3f, 0x80ff60, 2f, 40);
                Fx.ring(l, pos.add(0, 0.4, 0), 5.5f, 0x40ff30, 2f, 64);
                Fx.burst(l, ParticleTypes.HAPPY_VILLAGER, pos, 40, 2.5, 0.1);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 2.5f, 0.55f);
            });

    public DoomRpgMarine() {
        super("doom_rpg_marine", "Stan Blazkowicz", "スタン・ブラスコヴィッチ", "Doom RPG (2005 mobile)");
        eggPrimary = 0x5a6e80; eggSecondary = 0x2c3540;
        maxHealth = 70; speed = 0.29; armor = 12; preferredRange = 8f;
        idleWeaponR = "pistol";
    }

    // ------------------------------------------------------------------ server helpers
    private static ServerLevel sl(SlayerEntity e) { return (ServerLevel) e.level(); }
    private static Vec3 gun(SlayerEntity e, double fwd) { return Combat.muzzle(e, fwd, 0.3, 0.05); }
    private static void snd(SlayerEntity e, SoundEvent s, float vol, float pitch) { Combat.sound(e, s, vol, pitch); }
    private static void flash(SlayerEntity e, Vec3 at, int n) {
        Fx.burst(sl(e), ParticleTypes.FLAME, at, n, 0.05, 0.01);
        Fx.burst(sl(e), ParticleTypes.SMOKE, at, n, 0.06, 0.02);
    }
    /** hitscan pellets from the right-hand muzzle */
    private static void fire(SlayerEntity e, LivingEntity t, int pellets, double spread, float dmg, double range, int rgb) {
        if (t == null) return;
        Vec3 from = gun(e, 0.9);
        for (int i = 0; i < pellets; i++) Combat.hitscan(e, from, Combat.dirTo(from, t, spread, R), range, dmg, 0, rgb);
        flash(e, from, 3 + pellets / 2);
    }
    /** forward cone melee; the current target always counts if within reach */
    private static List<LivingEntity> melee(SlayerEntity e, LivingEntity t, double range, double half, float dmg, double kb, double up) {
        List<LivingEntity> hits = Combat.cone(e, range, half);
        if (t != null && Combat.isFoe(e, t) && e.distanceTo(t) <= range + 0.5 && !hits.contains(t)) hits.add(t);
        for (LivingEntity f : hits) if (Combat.damage(e, f, dmg)) {
            Combat.knock(f, e.position(), kb, up);
            Fx.sparks(sl(e), f.position().add(0, f.getBbHeight() * 0.6, 0), 6);
        }
        return hits;
    }
    private static void axeArc(SlayerEntity e, float from, float to, float tilt) {
        Fx.arc(sl(e), e.position().add(0, 1.15, 0), e.getYRot(), 2.2f, from, to, tilt, 0xcfd6dd, 1.3f, 16);
        Fx.arc(sl(e), e.position().add(0, 1.15, 0), e.getYRot(), 1.8f, from, to, tilt, 0x8a96a2, 0.9f, 12);
    }

    // ------------------------------------------------------------------ attacks
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // FISTS: left jab (tick 5) into a heavy right cross (tick 12)
        out.add(AttackDef.of("fists").range(0, 2.6f).cd(16).dur(18).weight(2)
                .tick((e, t, n) -> {
                    if (n == 5) { melee(e, t, 2.6, 55, 4f, 0.3, 0.1); snd(e, SoundEvents.PLAYER_ATTACK_WEAK, 1f, 1.1f); }
                    if (n == 12) { melee(e, t, 2.7, 55, 6.5f, 0.8, 0.2); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.9f); }
                })
                .anim((p, t, age) -> {
                    float s = track(t, 0, 0, .15f, 1, .85f, 1, 1, 0);
                    p.bodyY = track(t, 0, 0, .18f, -.15f, .28f, .35f, .42f, .2f, .56f, .3f, .67f, -.55f, .8f, -.45f, 1, 0);
                    p.bodyX = .15f * s + .12f * kick(t, .67f, .3f);
                    p.rArmX = track(t, 0, 0, .15f, -1.25f, .56f, -1.05f, .67f, -1.8f, .8f, -1.6f, 1, 0);
                    p.rArmY = track(t, 0, 0, .15f, .1f, .56f, .15f, .67f, -.25f, 1, 0);
                    p.lArmX = track(t, 0, 0, .15f, -1.3f, .22f, -1.15f, .28f, -1.8f, .4f, -1.25f, .67f, -1.0f, .85f, -1.2f, 1, 0);
                    p.lArmY = track(t, 0, 0, .15f, -.1f, .28f, .25f, .4f, 0, 1, 0);
                    p.rArmZ = 0; p.lArmZ = 0;
                    p.rLegX = .3f * s; p.lLegX = -.35f * s;
                    p.rootY = -.5f * s;
                    p.rootZ = -.8f * kick(t, .25f, .25f) - 1.4f * kick(t, .64f, .3f);
                    p.rootYaw = p.bodyY * .2f;
                    p.headY -= p.bodyY * .7f; p.headX += .12f * s;
                }));

        // AXE: weighty diagonal overhead chop (tick 13) then a rising horizontal backhand (tick 25)
        out.add(AttackDef.of("axe").range(0, 3.2f).cd(30).dur(36).weight(2).weapon("axe")
                .tick((e, t, n) -> {
                    if (n == 9) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.6f);
                    if (n >= 10 && n <= 13) axeArc(e, -75, -75 + 150 * (n - 9) / 4f, 55);
                    if (n == 13) {
                        for (LivingEntity f : melee(e, t, 3.3, 70, 12f, 0.9, 0.35))
                            Fx.burst(sl(e), ParticleTypes.DAMAGE_INDICATOR, f.position().add(0, 1, 0), 6, 0.2, 0.1);
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.7f); snd(e, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.6f, 1.3f);
                    }
                    if (n == 21) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.75f);
                    if (n >= 22 && n <= 25) axeArc(e, 80, 80 - 160 * (n - 21) / 4f, -12);
                    if (n == 25) { melee(e, t, 3.3, 80, 9f, 1.2, 0.2); snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.85f); }
                })
                .anim((p, t, age) -> {
                    p.rArmX = track(t, 0, 0, .22f, -2.9f, .28f, -2.95f, .36f, -.5f, .44f, -.3f, .58f, -1.3f, .69f, -1.5f, .78f, -1.4f, 1, 0);
                    p.rArmY = track(t, 0, 0, .28f, .2f, .36f, -.1f, .5f, -.9f, .58f, -1.0f, .69f, .9f, .8f, .8f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .28f, .25f, .36f, 0, .69f, .4f, 1, 0);
                    p.lArmX = p.rArmX * .92f + .1f; p.lArmY = p.rArmY * .8f + .45f; p.lArmZ = -.05f;
                    p.rHandX = track(t, 0, 0, .28f, -.7f, .36f, .5f, .46f, .4f, .58f, -.3f, .69f, .2f, 1, 0);
                    p.rHandZ = track(t, 0, 0, .5f, 0, .58f, 1.2f, .69f, 1.4f, .8f, 1.2f, 1, 0);
                    p.bodyY = track(t, 0, 0, .28f, .5f, .36f, -.3f, .44f, -.35f, .58f, -.7f, .69f, .7f, .8f, .6f, 1, 0);
                    p.bodyX = track(t, 0, 0, .28f, -.3f, .36f, .55f, .44f, .6f, .58f, .15f, .69f, .25f, .8f, .2f, 1, 0);
                    p.rootY = track(t, 0, 0, .28f, .4f, .36f, -1.4f, .46f, -1.3f, .6f, -.6f, .69f, -.9f, .8f, -.7f, 1, 0);
                    p.rootZ = track(t, 0, 0, .28f, 1.0f, .36f, -1.8f, .46f, -1.8f, .69f, -2.2f, 1, 0);
                    p.rootYaw = p.bodyY * .25f;
                    p.lLegX = track(t, 0, 0, .28f, .1f, .36f, -.6f, .69f, -.5f, 1, 0);
                    p.rLegX = track(t, 0, 0, .28f, -.05f, .36f, .5f, .69f, .45f, 1, 0);
                    p.rLegZ = .1f; p.lLegZ = -.1f;
                    p.headX += -p.bodyX * .5f; p.headY -= p.bodyY * .6f;
                }));

        // CHAINSAW: rev, lunge in, grind (ticks 9-27), rip upward (tick 29)
        out.add(AttackDef.of("chainsaw").range(0, 2.8f).cd(40).dur(34).weapon("chainsaw")
                .tick((e, t, n) -> {
                    if (n == 0) snd(e, SoundEvents.GRINDSTONE_USE, 1f, 0.6f);
                    if (n == 8) Combat.lunge(e, 0.55, 0.05);
                    if (n >= 9 && n <= 27 && n % 2 == 1) {
                        for (LivingEntity f : melee(e, t, 2.8, 50, 2.2f, 0.05, 0))
                            Fx.dustBurst(sl(e), 0x8a0000, 1f, f.position().add(0, f.getBbHeight() * 0.5, 0), 6, 0.2);
                        Fx.burst(sl(e), ParticleTypes.SMOKE, gun(e, 0.6), 2, 0.05, 0.01);
                    }
                    if (n >= 9 && n <= 27 && n % 4 == 1) snd(e, SoundEvents.GRINDSTONE_USE, 1f, 1.4f);
                    if (n == 29) { melee(e, t, 3, 60, 6f, 0.4, 0.5); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.3f); }
                })
                .anim((p, t, age) -> {
                    float r = track(t, 0, 0, .12f, 1, .9f, 1, 1, 0);
                    float g = win(t, .26f, .32f) * (1 - win(t, .8f, .86f));
                    float jt = (float) Math.sin(age * 4.3f) * .05f * (g + .3f * r);
                    p.rArmX = track(t, 0, 0, .15f, -.7f, .24f, -.55f, .3f, -1.35f, .8f, -1.25f, .87f, -1.95f, .93f, -1.75f, 1, 0) + jt;
                    p.rArmY = -.15f * r; p.rArmZ = 0;
                    p.lArmX = p.rArmX + .15f; p.lArmY = .6f * r; p.lArmZ = 0;
                    p.rHandX = track(t, 0, 0, .15f, .3f, .3f, 0, .8f, .1f, .87f, -.5f, 1, 0) + jt * .6f;
                    p.bodyX = track(t, 0, 0, .15f, -.1f, .3f, .45f, .8f, .45f, .87f, .1f, 1, 0) + jt * .5f;
                    p.bodyY = track(t, 0, 0, .15f, .4f, .3f, -.1f, .8f, -.1f, 1, 0) + jt * .4f;
                    p.rootZ = track(t, 0, 0, .2f, .6f, .28f, -2.2f, .8f, -2.4f, 1, 0);
                    p.rootY = track(t, 0, 0, .2f, -.3f, .3f, -1f, .8f, -1f, .87f, .3f, 1, 0);
                    p.rLegX = .55f * r; p.lLegX = -.5f * r; p.rLegZ = .12f * r; p.lLegZ = -.12f * r;
                    p.headX += .2f * g - .2f * bump(win(t, .8f, .95f)); p.headY -= p.bodyY * .6f;
                }));

        // PISTOL: two aimed shots, one-handed with support hand (ticks 4, 10)
        out.add(AttackDef.of("pistol").range(2.5f, 18).cd(16).dur(16).weight(3).weapon("pistol")
                .tick((e, t, n) -> {
                    if (n == 4 || n == 10) { fire(e, t, 1, 0.02, 5f, 22, 0xffe080); Combat.gunSound(e, 1.7f); }
                })
                .anim((p, t, age) -> {
                    float raise = track(t, 0, 0, .18f, 1, .85f, 1, 1, 0);
                    float rec = kick(t, .25f, .22f) + kick(t, .625f, .22f);
                    p.rArmX = -1.57f * raise - .3f * rec; p.rArmY = .2f * raise; p.rArmZ = 0;
                    p.lArmX = -1.4f * raise - .2f * rec; p.lArmY = .8f * raise; p.lArmZ = 0;
                    p.rHandX = -.5f * rec;
                    p.bodyY = -.25f * raise; p.bodyX = .05f * raise - .08f * rec;
                    p.headY -= p.bodyY * .8f; p.headZ = .1f * raise;
                    p.rLegX = .3f * raise; p.lLegX = -.25f * raise;
                    p.rootY = -.25f * raise; p.rootZ = .25f * rec;
                }));

        // SHOTGUN: one blast (tick 6) and a pump (ticks 11-17)
        out.add(AttackDef.of("shotgun").range(1.5f, 11).cd(26).dur(24).weight(3).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 6) { fire(e, t, 7, 0.07, 2.6f, 16, 0xffd070); snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 1.5f, 0.7f); }
                    if (n == 11) snd(e, SoundEvents.PISTON_CONTRACT, 0.6f, 1.6f);
                    if (n == 16) snd(e, SoundEvents.PISTON_EXTEND, 0.6f, 1.7f);
                })
                .anim((p, t, age) -> {
                    float raise = track(t, 0, 0, .2f, 1, .88f, 1, 1, 0);
                    float rec = kick(t, .25f, .3f);
                    float pump = bump(win(t, .42f, .72f));
                    p.rArmX = -1.5f * raise - .45f * rec; p.rArmY = -.1f * raise; p.rArmZ = 0;
                    p.lArmX = -1.45f * raise - .35f * rec + .35f * pump; p.lArmY = .5f * raise; p.lArmZ = -.1f * pump;
                    p.rHandX = -.6f * rec + .1f * pump;
                    p.bodyX = .1f * raise - .3f * rec; p.bodyY = .2f * raise + .1f * pump;
                    p.rootZ = 1.4f * rec; p.rootY = -.3f * raise;
                    p.headX += -.15f * rec; p.headY -= p.bodyY * .7f; p.headZ = -.08f * raise;
                    p.rLegX = .4f * raise; p.lLegX = -.3f * raise;
                }));

        // SUPER SHOTGUN: double-barrel blast (tick 7), break open, load two shells, snap shut
        out.add(AttackDef.of("super_shotgun").range(0.5f, 8).cd(40).dur(34).weight(2).weapon("ssg")
                .tick((e, t, n) -> {
                    if (n == 7) {
                        fire(e, t, 14, 0.11, 2.4f, 12, 0xffc060);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 2f, 0.5f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.8f);
                    }
                    if (n == 15) snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.8f, 1.5f);
                    if (n == 23) snd(e, SoundEvents.LEVER_CLICK, 0.8f, 1.8f);
                    if (n == 27) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.9f, 1.4f);
                })
                .anim((p, t, age) -> {
                    float raise = track(t, 0, 0, .18f, 1, .9f, 1, 1, 0);
                    float rec = kick(t, .206f, .35f);
                    float open = track(t, 0, 0, .4f, 0, .47f, 1, .76f, 1, .8f, 0, 1, 0);
                    float reach = track(t, 0, 0, .47f, 0, .55f, 1, .62f, 1, .7f, 0, 1, 0);
                    float load = bump(win(t, .66f, .78f));
                    p.rArmX = -1.5f * raise - .7f * rec + .5f * open; p.rArmY = -.1f * raise; p.rArmZ = 0;
                    p.rHandX = -.8f * rec + .9f * open - .3f * kick(t, .8f, .12f);
                    p.lArmX = (-1.4f * raise - .5f * rec) * (1 - reach) + .25f * reach - .9f * load;
                    p.lArmY = .5f * raise * (1 - reach) + .5f * load; p.lArmZ = -.25f * reach;
                    p.bodyX = .12f * raise - .45f * rec + .15f * open; p.bodyY = .2f * raise;
                    p.rootZ = 2.2f * rec; p.rootY = -.35f * raise + .4f * rec;
                    p.headX += -.25f * rec + .3f * open; p.headY -= p.bodyY * .7f;
                    p.rLegX = .45f * raise; p.lLegX = -.35f * raise - .3f * rec;
                }));

        // CHAINGUN: spin-up, hip-fired stream (ticks 8-32 every 2), spin-down
        out.add(AttackDef.of("chaingun").range(3, 16).cd(50).dur(40).weight(2).weapon("chaingun")
                .tick((e, t, n) -> {
                    if (n == 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.6f, 2f);
                    if (n >= 8 && n <= 32 && n % 2 == 0) {
                        fire(e, t, 1, 0.05, 2.2f, 20, 0xffe890);
                        Fx.burst(sl(e), ParticleTypes.CRIT, gun(e, 0.3), 1, 0.05, 0.15); // brass
                        if (n % 4 == 0) Combat.gunSound(e, 1.9f);
                    }
                })
                .anim((p, t, age) -> {
                    float raise = track(t, 0, 0, .15f, 1, .88f, 1, 1, 0);
                    float fire = win(t, .18f, .22f) * (1 - win(t, .8f, .84f));
                    float sh = (float) Math.sin(age * 5.5f) * .04f * fire;
                    p.rArmX = -1.25f * raise - .08f * fire + sh; p.rArmY = -.05f; p.rArmZ = 0;
                    p.lArmX = -1.05f * raise + sh; p.lArmY = .65f * raise; p.lArmZ = 0;
                    p.rHandX = -.3f * raise + sh * 1.5f;
                    p.bodyX = .2f * raise + sh; p.bodyY = .15f * raise; p.bodyZ = sh * .5f;
                    p.rootZ = .4f * fire + sh * 3; p.rootY = -.6f * raise;
                    p.rLegX = .5f * raise; p.lLegX = -.4f * raise; p.rLegZ = .15f * raise; p.lLegZ = -.15f * raise;
                    p.headX += sh; p.headY -= p.bodyY * .7f;
                }));

        // ROCKET LAUNCHER: shoulder, brace, launch (tick 10) with backblast
        out.add(AttackDef.of("rocket_launcher").range(5, 24).cd(60).dur(30).weight(2).weapon("rocket")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.CROSSBOW_LOADING_END, 0.8f, 0.6f);
                    if (n == 10 && t != null) {
                        Vec3 from = Combat.muzzle(e, 1.1, 0.35, 0.25);
                        Combat.shoot(e, ProjStyle.ROCKET, from, Combat.dirTo(from, t, 0.01, R), 1.3, 16f);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.6f, 0.6f);
                        flash(e, from, 6);
                        Vec3 back = Combat.muzzle(e, -1.2, 0.35, 0.25);
                        Fx.burst(sl(e), ParticleTypes.LARGE_SMOKE, back, 10, 0.25, 0.05);
                        Fx.burst(sl(e), ParticleTypes.FLAME, back, 6, 0.15, 0.04);
                    }
                })
                .anim((p, t, age) -> {
                    float raise = track(t, 0, 0, .22f, 1, .85f, 1, 1, 0);
                    float rec = kick(t, .333f, .45f);
                    p.rArmX = -1.6f * raise - .55f * rec; p.rArmY = -.1f * raise; p.rArmZ = .15f * raise;
                    p.lArmX = -1.55f * raise - .45f * rec; p.lArmY = .55f * raise; p.lArmZ = 0;
                    p.rHandX = -.4f * rec;
                    p.bodyX = .15f * raise - .45f * rec; p.bodyY = .3f * raise; p.bodyZ = -.08f * raise;
                    p.headY -= .3f * raise; p.headZ = .2f * raise; p.headX += -.2f * rec;
                    p.rootZ = 2.5f * rec; p.rootY = track(t, 0, 0, .28f, -.8f, .4f, .3f, .6f, -.4f, 1, 0);
                    p.rootPitch = -.08f * rec;
                    p.rLegX = .55f * raise; p.lLegX = -.45f * raise - .3f * rec;
                }));

        // PLASMA GUN: five-bolt burst with sweeping aim (ticks 6,9,12,15,18)
        out.add(AttackDef.of("plasma_gun").range(3, 20).cd(32).dur(26).weight(2).weapon("plasma")
                .tick((e, t, n) -> {
                    if (n >= 6 && n <= 18 && (n - 6) % 3 == 0 && t != null) {
                        Vec3 from = gun(e, 1.1);
                        Combat.shoot(e, ProjStyle.PLASMA, from, Combat.dirTo(from, t, 0.04, R), 1.6, 4.5f);
                        snd(e, SoundEvents.SHULKER_SHOOT, 0.8f, 1.8f);
                        Fx.burst(sl(e), ParticleTypes.ELECTRIC_SPARK, from, 4, 0.08, 0.05);
                    }
                })
                .anim((p, t, age) -> {
                    float raise = track(t, 0, 0, .18f, 1, .82f, 1, 1, 0);
                    float rec = 0;
                    for (int k = 0; k < 5; k++) rec += kick(t, (6 + 3 * k) / 26f, .1f);
                    p.rArmX = -1.55f * raise - .2f * rec; p.rArmY = -.12f * raise; p.rArmZ = 0;
                    p.lArmX = -1.5f * raise - .15f * rec; p.lArmY = .5f * raise; p.lArmZ = 0;
                    p.rHandX = -.25f * rec;
                    p.bodyX = .08f * raise - .08f * rec;
                    p.bodyY = .15f * raise + .08f * (float) Math.sin(age * 0.8f) * win(t, .2f, .75f);
                    p.rootZ = .4f * rec; p.rootY = -.3f * raise;
                    p.rLegX = .35f * raise; p.lLegX = -.3f * raise;
                    p.headY -= p.bodyY * .8f; p.headZ = -.1f * raise;
                }));

        // BFG 9000: charge (ticks 0-29, green energy converging), fire (tick 30) + tracer sprays
        out.add(AttackDef.of("bfg9000").range(6, 24).cd(400).dur(50).weapon("bfg")
                .tick((e, t, n) -> {
                    ServerLevel l = sl(e);
                    if (n == 0) snd(e, SoundEvents.BEACON_ACTIVATE, 1.5f, 1.4f);
                    if (n < 30) {
                        Vec3 m = gun(e, 1.3);
                        double rad = 1.4 * (1 - n / 30.0);
                        for (int k = 0; k < 3; k++) {
                            double a = n * 0.7 + k * 2.094;
                            l.sendParticles(Fx.dust(0x70ff50, 1.2f), m.x + Math.cos(a) * rad, m.y + Math.sin(a) * rad, m.z + Math.sin(a + 1) * rad, 1, 0, 0, 0, 0);
                        }
                        if (n == 20) snd(e, SoundEvents.BEACON_POWER_SELECT, 1.5f, 0.6f);
                    }
                    if (n == 30 && t != null) {
                        Vec3 from = gun(e, 1.4);
                        Combat.shoot(e, BFG_BALL, from, Combat.dirTo(from, t, 0, R), 0.8, 30f);
                        for (LivingEntity f : Combat.cone(e, 18, 30)) {
                            Fx.beam(l, from, f.position().add(0, f.getBbHeight() * 0.5, 0), 0x50ff30, 1.1f, 0.5);
                            Combat.damage(e, f, 8f);
                        }
                        snd(e, SoundEvents.WITHER_SHOOT, 1.6f, 0.5f); snd(e, SoundEvents.GENERIC_EXPLODE, 1f, 1.6f);
                        Fx.burst(l, ParticleTypes.HAPPY_VILLAGER, from, 20, 0.4, 0.1);
                    }
                })
                .anim((p, t, age) -> {
                    float raise = track(t, 0, 0, .16f, 1, .86f, 1, 1, 0);
                    float ch = win(t, .1f, .6f) * (1 - win(t, .6f, .62f));
                    float sh = (float) Math.sin(age * 6.3f) * .045f * ch * ch;
                    float rec = kick(t, .6f, .35f);
                    p.rArmX = -1.35f * raise - .1f * ch - .6f * rec + sh; p.rArmY = -.1f; p.rArmZ = 0;
                    p.lArmX = -1.25f * raise - .1f * ch - .5f * rec + sh; p.lArmY = .7f * raise; p.lArmZ = 0;
                    p.rHandX = -.15f * raise - .4f * rec;
                    p.bodyX = .3f * ch - .6f * rec + sh; p.bodyY = .15f * raise; p.bodyZ = sh;
                    p.rootY = -1.2f * ch + bump(win(t, .6f, .75f)) - .3f * raise;
                    p.rootZ = 3.2f * track(t, 0, 0, .6f, 0, .72f, 1, .9f, .6f, 1, 0);
                    p.rootPitch = -.12f * rec;
                    p.rLegX = .6f * raise; p.lLegX = -.5f * raise; p.rLegZ = .15f * raise; p.lLegZ = -.15f * raise;
                    p.headX += .15f * ch - .35f * rec; p.headY -= p.bodyY * .7f;
                }));

        // SOUL CUBE: raise the cube overhead, souls swirl, hurl it (tick 20); it drains the foe and heals the marine
        out.add(AttackDef.of("soul_cube").range(2, 20).cd(500).dur(40).weapons(null, "soulcube")
                .tick((e, t, n) -> {
                    if (n < 20) {
                        Vec3 h = Combat.muzzle(e, 0.6, -0.35, 0.3 + n * 0.04);
                        Fx.burst(sl(e), ParticleTypes.SOUL, h, 2, 0.25, 0.02);
                        Fx.dustBurst(sl(e), 0xff3a20, 0.8f, h, 2, 0.2);
                    }
                    if (n == 5 || n == 13) snd(e, SoundEvents.SOUL_ESCAPE, 1.2f, 0.6f);
                    if (n == 20 && t != null) {
                        Vec3 from = Combat.muzzle(e, 0.8, -0.35, 0.6);
                        Combat.shoot(e, SOUL_CUBE, from, Combat.dirTo(from, t, 0, R), 0.9, 18f);
                        snd(e, SoundEvents.EVOKER_CAST_SPELL, 1.2f, 0.7f);
                    }
                })
                .anim((p, t, age) -> {
                    p.weaponL = t < .52f ? "soulcube" : null;
                    p.lArmX = track(t, 0, 0, .25f, -1.3f, .42f, -2.7f, .47f, -2.8f, .52f, -1.6f, .62f, -1.7f, 1, 0);
                    p.lArmY = track(t, 0, 0, .25f, .4f, .42f, .15f, .52f, .25f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .42f, -.1f, 1, 0);
                    p.lHandX = track(t, 0, 0, .42f, .4f, .52f, -.3f, 1, 0) + (float) Math.sin(age * .9f) * .08f * win(t, .25f, .45f);
                    p.rArmX = track(t, 0, 0, .25f, -.4f, .42f, -.2f, .52f, .3f, .7f, .2f, 1, 0);
                    p.rArmY = 0; p.rArmZ = track(t, 0, 0, .25f, .3f, .42f, .6f, .52f, .4f, 1, 0);
                    p.bodyX = track(t, 0, 0, .25f, .05f, .42f, -.3f, .47f, -.32f, .52f, .35f, .65f, .25f, 1, 0);
                    p.bodyY = track(t, 0, 0, .25f, -.1f, .42f, -.25f, .52f, .3f, 1, 0);
                    p.headX += track(t, 0, 0, .42f, -.45f, .52f, .15f, 1, 0); p.headY -= p.bodyY * .6f;
                    p.rootY = track(t, 0, 0, .42f, .6f, .52f, -.8f, .7f, -.5f, 1, 0);
                    p.rootZ = track(t, 0, 0, .42f, .5f, .53f, -1.5f, .75f, -1.2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .42f, .05f, .52f, -.55f, .8f, -.4f, 1, 0);
                    p.rLegX = track(t, 0, 0, .52f, .45f, .8f, .35f, 1, 0);
                }));

        // FRAG GRENADE: pull pin with off hand, point, overhand lob (tick 13)
        out.add(AttackDef.of("grenade").range(4, 14).cd(90).dur(24).weapon("grenade")
                .tick((e, t, n) -> {
                    if (n == 5) snd(e, SoundEvents.TRIPWIRE_CLICK_OFF, 1f, 1.5f);
                    if (n == 13 && t != null) {
                        Vec3 from = Combat.muzzle(e, 0.6, 0.3, 0.5);
                        Vec3 d = t.position().subtract(from);
                        double h = Math.max(1, Math.hypot(d.x, d.z)), vh = 0.7, T = h / vh;
                        double vy = (d.y + 0.5 * ProjStyle.GRENADE.gravity * T * T) / T;
                        Vec3 v = new Vec3(d.x / h * vh, vy, d.z / h * vh);
                        Combat.shoot(e, ProjStyle.GRENADE, from, v, v.length(), 12f);
                        snd(e, SoundEvents.WITCH_THROW, 1f, 0.8f);
                    }
                })
                .anim((p, t, age) -> {
                    p.weaponR = t < .56f ? "grenade" : null;
                    p.rArmX = track(t, 0, 0, .12f, -1.0f, .3f, -1.1f, .48f, -2.75f, .54f, -.7f, .66f, -.3f, 1, 0);
                    p.rArmY = track(t, 0, 0, .12f, .4f, .3f, .3f, .48f, .1f, .54f, -.35f, 1, 0);
                    p.rArmZ = track(t, 0, 0, .3f, 0, .48f, .35f, .54f, 0, 1, 0);
                    p.lArmX = track(t, 0, 0, .12f, -1.05f, .2f, -1.1f, .3f, -.6f, .4f, -1.55f, .5f, -1.55f, .6f, -.4f, 1, 0);
                    p.lArmY = track(t, 0, 0, .12f, .55f, .2f, .6f, .3f, 0, .4f, .1f, .6f, -.1f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .25f, -.35f, .35f, 0, .6f, -.3f, 1, 0);
                    p.rHandX = track(t, 0, 0, .48f, -.6f, .54f, .5f, 1, 0);
                    p.bodyY = track(t, 0, 0, .3f, .1f, .48f, .55f, .54f, -.4f, .66f, -.5f, 1, 0);
                    p.bodyX = track(t, 0, 0, .3f, .05f, .48f, -.2f, .54f, .35f, .66f, .4f, 1, 0);
                    p.rootZ = track(t, 0, 0, .48f, .7f, .54f, -1.6f, .7f, -1.6f, 1, 0);
                    p.rootY = track(t, 0, 0, .48f, .2f, .56f, -.7f, 1, 0);
                    p.lLegX = track(t, 0, 0, .48f, -.1f, .54f, -.6f, .75f, -.5f, 1, 0);
                    p.rLegX = track(t, 0, 0, .48f, .25f, .54f, .55f, .75f, .4f, 1, 0);
                    p.headY -= p.bodyY * .8f;
                }));

        // MEDKIT: kneel, pull kit from belt, open, apply (heal at tick 26), toss it, stand
        out.add(AttackDef.of("medkit").range(0, 40).noLos().cd(400).dur(44).weight(6).weapons(null, "medkit").noFace()
                .when((e, t) -> e.getHealth() / e.getMaxHealth() < 0.5f)
                .tick((e, t, n) -> {
                    if (n == 5) snd(e, SoundEvents.BUNDLE_REMOVE_ONE, 1f, 0.8f);
                    if (n == 15) snd(e, SoundEvents.BOOK_PAGE_TURN, 1f, 0.7f);
                    if (n == 26) {
                        e.heal(e.getMaxHealth() * 0.35f);
                        ServerLevel l = sl(e); Vec3 c = e.position().add(0, 1, 0);
                        Fx.burst(l, ParticleTypes.HEART, c.add(0, 0.8, 0), 6, 0.4, 0);
                        Fx.burst(l, ParticleTypes.HAPPY_VILLAGER, c, 14, 0.5, 0.05);
                        Fx.ring(l, e.position().add(0, 0.1, 0), 1.2f, 0xff4040, 1.2f, 24);
                        snd(e, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.5f); snd(e, SoundEvents.GENERIC_DRINK, 0.8f, 1.2f);
                    }
                    if (n == 33) snd(e, SoundEvents.ITEM_PICKUP, 0.8f, 0.6f);
                })
                .anim((p, t, age) -> {
                    float kneel = track(t, 0, 0, .18f, 1, .82f, 1, .95f, 0, 1, 0);
                    float breath = (float) Math.sin(age * .5f) * .05f * win(t, .5f, .75f);
                    p.weaponL = t > .2f && t < .78f ? "medkit" : null;
                    p.rootY = -4f * kneel + .3f * bump(win(t, .82f, 1f));
                    p.rLegX = 1.0f * kneel; p.lLegX = -1.1f * kneel; p.rLegZ = .05f * kneel; p.lLegZ = 0;
                    p.lArmX = track(t, 0, 0, .1f, .1f, .2f, .35f, .32f, -1.1f, .45f, -1.15f, .55f, -.6f, .68f, -.6f, .75f, -1.3f, .82f, -.6f, 1, 0);
                    p.lArmY = track(t, 0, 0, .2f, 0, .32f, .45f, .55f, .9f, .68f, .9f, .75f, .2f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .2f, -.35f, .32f, 0, .68f, 0, .76f, -.7f, .85f, -.2f, 1, 0);
                    p.lHandX = track(t, 0, 0, .32f, .6f, .55f, .2f, 1, 0);
                    p.rArmX = track(t, 0, 0, .3f, -.2f, .4f, -1.05f, .47f, -1.0f, .55f, -.75f, .68f, -.75f, .78f, -.2f, 1, 0);
                    p.rArmY = track(t, 0, 0, .4f, -.45f, .55f, -.65f, .68f, -.6f, .78f, 0, 1, 0);
                    p.rArmZ = track(t, 0, 0, .78f, .2f, 1, 0);
                    p.bodyX = track(t, 0, 0, .18f, .35f, .32f, .25f, .55f, .45f, .68f, .4f, .82f, .1f, .92f, -.15f, 1, 0) + breath;
                    p.bodyY = track(t, 0, 0, .2f, .2f, .32f, 0, 1, 0);
                    p.headY *= 1 - kneel * .7f;
                    p.headY += track(t, 0, 0, .2f, -.3f, .32f, 0, 1, 0);
                    p.headX += track(t, 0, 0, .2f, .45f, .3f, .3f, .55f, .5f, .68f, .15f, .75f, -.2f, .92f, -.15f, 1, 0);
                }));
    }

    /** standing still: pistol at low ready, sweeping the helmet lamp around the dark corridor */
    @Override
    public void idle(Pose p, SlayerEntity e, float age, float limbAmt) {
        float still = 1 - limbAmt;
        p.rArmX += -.35f * still; p.rArmY += .1f * still;
        p.rHandX += -.2f * still;
        p.headY += (float) Math.sin(age * .035f) * .25f * still;
    }

    // ------------------------------------------------------------------ model
    @Override
    public void buildModel(Rig rig) {
        Mat armor = Mat.of(0x5a6e80).noise(.1f).bevel(.25f);
        Mat armorDk = Mat.of(0x3c4a58).noise(.1f).bevel(.25f);
        Mat chest = Mat.of(0x5f7486).noise(.08f).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            g.rect(x, y + h / 2, w, 1, 0x2c3540);           // plate seam
            g.rect(x + 1, y + 1, 3, 1, 0xd8d8d0);             // UAC tag
            g.glow(x + w - 2, y + 1, 0x40ff60);               // status light
        });
        Mat under = Mat.of(0x34363a).noise(.14f);
        Mat belt = Mat.of(0x4a3f30).noise(.12f);
        Mat boot = Mat.of(0x2a2a2c).noise(.1f);
        Mat glove = Mat.of(0x2e3034).noise(.1f);
        Mat helm = Mat.of(0x627688).noise(.08f).bevel(.28f);
        Mat visor = Mat.of(0x8fe0ff).glow();
        Mat lamp = Mat.of(0xfff2b8).glow();
        Mat metal = Mat.of(0x8a8e94).noise(.06f);
        Mat gunM = Mat.of(0x3a3c40).noise(.06f);
        Mat gunDk = Mat.of(0x232427).noise(.06f);
        Mat wood = Mat.of(0x6b4a2a).noise(.18f);
        Mat olive = Mat.of(0x4f5a34).noise(.1f);
        Mat steel = Mat.of(0xb8bcc0).noise(.05f).bevel(.1f);
        Mat orange = Mat.of(0xd06a10).noise(.1f);
        Mat chain = Mat.of(0x55585c).decal((m, g, x, y, w, h, f) -> {
            for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) if (((i + j) & 1) == 0) g.px(x + i, y + j, 0x1c1c1e);
        });
        Mat plasmaBody = Mat.of(0x4a5560).noise(.06f);
        Mat plasmaGlow = Mat.of(0x55b8ff).glow();
        Mat bfgBody = Mat.of(0x5a6048).noise(.08f);
        Mat bfgGlow = Mat.of(0x66ff55).glow();
        Mat rocketTip = Mat.of(0xc02a20).glow();
        Mat soul = Mat.of(0x8a3a2a).noise(.2f).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            g.glow(x + 1, y + 1, 0xff5a20); g.glow(x + w - 2, y + 1, 0xff5a20);
            g.rect(x + 1, y + h - 2, w - 2, 1, 0x2a0a08);
        });
        Mat medkit = Mat.of(0xe8e8e0).noise(.04f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT && f != Mat.Face.BACK) return;
            g.rect(x + w / 2 - 1, y, 2, h, 0xc01818);
            g.rect(x + 1, y + h / 2 - 1, w - 2, 2, 0xc01818);
        });

        // torso
        rig.box(rig.torso, under, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4.5f, -12.3f, -2.8f, 9, 7, 1);
        rig.box(rig.torso, armor, -4.5f, -12.3f, 1.8f, 9, 7, 1);
        rig.box(rig.torso, armorDk, -3.5f, -5, -2.6f, 7, 2, 1);
        rig.box(rig.torso, armorDk, -3, -13, -2.5f, 6, 1, 5);
        rig.box(rig.torso, belt, -4, -3, -2, 8, 2, 4, .35f);
        rig.box(rig.torso, metal, -1, -3, -2.7f, 2, 2, 1);
        rig.box(rig.torso, belt, 2.5f, -3.3f, -3, 2, 2, 1);
        rig.box(rig.torso, belt, -4.5f, -3.3f, -3, 2, 2, 1);
        rig.box(rig.torso, armorDk, -3, -11, 2.3f, 6, 7, 2);
        // head: UAC helmet, visor, lamp
        rig.box(rig.head, helm, -4, -8, -4, 8, 8, 8);
        rig.box(rig.head, visor, -3, -5.5f, -4.6f, 6, 2, 1);
        rig.box(rig.head, armorDk, -3, -3, -4.8f, 6, 3, 1);
        rig.box(rig.head, armorDk, -1, -9, -4.5f, 2, 2, 9);
        rig.box(rig.head, armorDk, -4.8f, -5, -1.5f, 1, 3, 3);
        rig.box(rig.head, metal, 4, -6, -3, 2, 2, 4);
        rig.box(rig.head, lamp, 4, -6, -3.6f, 2, 2, 1);
        // arms
        for (PartDefinition arm : new PartDefinition[]{rig.rArm, rig.lArm}) {
            boolean r = arm == rig.rArm;
            rig.box(arm, under, -2, -2, -2, 4, 12, 4);
            rig.box(arm, armor, r ? -3.2f : -1.8f, -3.2f, -2.8f, 5, 4, 6);
            rig.box(arm, armorDk, -2, 4, -2, 4, 3, 4, .3f);
            rig.box(arm, glove, -2, 8, -2, 4, 3, 4, .35f);
        }
        // legs
        for (PartDefinition leg : new PartDefinition[]{rig.rLeg, rig.lLeg}) {
            rig.box(leg, under, -2, 0, -2, 4, 12, 4);
            rig.box(leg, armor, -2, 1, -2.6f, 4, 4, 1);
            rig.box(leg, armorDk, -2, 5, -2.8f, 4, 2, 1);
            rig.box(leg, armor, -2, 7, -2.6f, 4, 2, 1);
            rig.box(leg, boot, -2, 9, -2, 4, 3, 4, .35f);
            rig.box(leg, boot, -2, 10, -3.2f, 4, 2, 1, .2f);
        }

        // ---- weapons (gun space: origin = fist, barrel -z, y down)
        PartDefinition w;
        w = rig.weapon(rig.rHand, "pistol");
        rig.box(w, gunM, -1, -4, -6, 2, 2, 7);
        rig.box(w, gunDk, -1, -2, -5, 2, 1, 5);
        rig.box(w, gunDk, -1, -2, 0, 2, 5, 2);
        rig.box(w, metal, -.5f, -5, -5.5f, 1, 1, 1);

        w = rig.weapon(rig.rHand, "shotgun");
        rig.box(w, gunM, -.5f, -4, -14, 1, 1, 13);
        rig.box(w, gunDk, -.5f, -3, -12, 1, 1, 9);
        rig.box(w, gunM, -1, -4, -3, 2, 3, 4);
        rig.box(w, wood, -1, -3.3f, -10, 2, 2, 4);
        rig.box(w, wood, -1, -3, 1, 2, 3, 6);
        rig.box(w, wood, -1, -1, 0, 2, 3, 2);

        w = rig.weapon(rig.rHand, "ssg");
        rig.box(w, gunM, -1.5f, -4, -12, 1, 1, 10);
        rig.box(w, gunM, .5f, -4, -12, 1, 1, 10);
        rig.box(w, metal, -.5f, -4.6f, -12, 1, 1, 10);
        rig.box(w, wood, -1.5f, -3, -8, 3, 1, 4);
        rig.box(w, gunDk, -1.5f, -4, -2, 3, 3, 3);
        rig.box(w, wood, -1, -3, 1, 2, 3, 5);

        w = rig.weapon(rig.rHand, "chaingun");
        rig.box(w, gunM, -1.5f, -5.5f, -14, 1, 1, 9);
        rig.box(w, gunM, .5f, -5.5f, -14, 1, 1, 9);
        rig.box(w, gunM, -1.5f, -3.5f, -14, 1, 1, 9);
        rig.box(w, gunM, .5f, -3.5f, -14, 1, 1, 9);
        rig.box(w, metal, -2, -6, -12, 4, 4, 1);
        rig.box(w, metal, -2, -6, -8, 4, 4, 1);
        rig.box(w, gunDk, -2.5f, -6.5f, -5, 5, 5, 7);
        rig.box(w, gunDk, -.5f, -8.5f, -4, 1, 2, 4);
        rig.box(w, olive, 2.5f, -4, -3, 2, 3, 3);
        rig.box(w, gunDk, -1, -1.5f, -1, 2, 3, 2);

        w = rig.weapon(rig.rHand, "rocket");
        rig.box(w, olive, -2, -7, -12, 4, 4, 20);
        rig.box(w, gunDk, -2.5f, -7.5f, -13, 5, 5, 1);
        rig.box(w, gunDk, -2.5f, -7.5f, 7, 5, 5, 1);
        rig.box(w, rocketTip, -1, -6, -12.5f, 2, 2, 1);
        rig.box(w, metal, -3.5f, -6.5f, -4, 1, 2, 3);
        rig.box(w, gunDk, -1, -3, -2, 2, 4, 2);

        w = rig.weapon(rig.rHand, "plasma");
        rig.box(w, plasmaBody, -2, -6, -10, 4, 4, 12);
        for (int z : new int[]{-8, -5, -2}) rig.box(w, plasmaGlow, -2, -5.5f, z, 4, 3, 1, .3f);
        rig.box(w, plasmaGlow, -1, -5, -11, 2, 2, 1);
        rig.box(w, gunDk, -.5f, -8, -6, 1, 2, 4);
        rig.box(w, gunDk, -1, -2, -1, 2, 3, 2);

        w = rig.weapon(rig.rHand, "bfg");
        rig.box(w, bfgBody, -3, -9, -12, 6, 7, 16);
        rig.box(w, bfgGlow, -3, -7, -8, 6, 3, 6, .3f);
        rig.box(w, gunDk, -3, -9.5f, -15, 2, 2, 3);
        rig.box(w, gunDk, 1, -9.5f, -15, 2, 2, 3);
        rig.box(w, gunDk, -1, -3.5f, -15, 2, 2, 3);
        rig.box(w, bfgGlow, -1, -7, -14, 2, 2, 2);
        rig.box(w, gunDk, -1, -11, -6, 2, 2, 6);
        rig.box(w, gunDk, -1, -2, -1, 2, 3, 2);

        w = rig.weapon(rig.rHand, "chainsaw");
        rig.box(w, orange, -2, -5, -2, 4, 5, 7);
        rig.box(w, gunDk, -2.5f, -4, 1, 5, 3, 3);
        rig.box(w, gunDk, -.5f, -8, -1, 1, 3, 4);
        rig.box(w, steel, -.5f, -4, -15, 1, 3, 13);
        rig.box(w, chain, -.5f, -4.5f, -15.5f, 1, 4, 14, .1f);
        rig.box(w, gunDk, -1, -1, 0, 2, 3, 2);

        w = rig.weapon(rig.rHand, "axe");
        rig.box(w, wood, -.5f, -15, -1, 1, 18, 1);
        rig.box(w, steel, -.5f, -16, -6, 1, 4, 5);
        rig.box(w, metal, -.5f, -17, -7, 1, 6, 1);
        rig.box(w, armorDk, -.5f, -15, 0, 1, 2, 2);
        rig.box(w, belt, -1, -13, -1.5f, 2, 3, 2);

        w = rig.weapon(rig.rHand, "grenade");
        rig.box(w, olive, -1, -2, -2, 2, 3, 2);
        rig.box(w, metal, -.5f, -3, -1.5f, 1, 1, 1);
        rig.box(w, metal, .8f, -3, -1.5f, 1, 2, 1);

        w = rig.weapon(rig.lHand, "soulcube");
        rig.box(w, soul, -2.5f, -5, -6, 5, 5, 5);

        w = rig.weapon(rig.lHand, "medkit");
        rig.box(w, medkit, -3, -4, -3, 6, 4, 2);
        rig.box(w, gunDk, -1, -5, -2.5f, 2, 1, 1);
    }
}
