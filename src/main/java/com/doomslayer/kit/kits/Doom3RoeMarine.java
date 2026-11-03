package com.doomslayer.kit.kits;

import com.doomslayer.anim.Keys;
import com.doomslayer.anim.Pose;
import com.doomslayer.client.Mat;
import com.doomslayer.client.Rig;
import com.doomslayer.entity.ProjStyle;
import com.doomslayer.entity.SlayerEntity;
import com.doomslayer.entity.SlayerProjectile;
import com.doomslayer.fx.Combat;
import com.doomslayer.fx.Fx;
import com.doomslayer.kit.AttackDef;
import com.doomslayer.kit.SlayerKit;
import java.util.List;
import java.util.Random;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

/** The Bravo Team survivor of Doom 3: Resurrection of Evil: UAC armour, Grabber, Super Shotgun and the cursed Artifact on the left arm. */
public class Doom3RoeMarine extends SlayerKit {

    // ---------------- projectiles ----------------
    /** Grabber hurl: carries the grabbed foe's body along behind the bolt (entity id in p.aux). */
    static final ProjStyle GRAB_HURL = ProjStyle.make().color(0xffa030).size(0.45f).life(30).explode(2.5f).trail(ParticleTypes.ELECTRIC_SPARK)
            .onTick(p -> {
                Entity c = p.level().getEntity(p.aux);
                if (c == null || !c.isAlive()) return;
                Vec3 v = p.getDeltaMovement(), back = p.position().subtract(v.normalize().scale(1.5));
                c.setPos(back.x, back.y - c.getBbHeight() / 2, back.z);
                c.setDeltaMovement(v); c.hurtMarked = true; c.fallDistance = 0;
                Fx.dustBurst((ServerLevel) p.level(), 0xffb040, 0.8f, back, 2, 0.3);
            })
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 2.5f); Fx.sound(l, pos, SoundEvents.ANVIL_LAND, 1f, 0.6f);
                Entity c = l.getEntity(p.aux);
                if (c != null) c.setDeltaMovement(p.getDeltaMovement().scale(0.4));
            });
    /** BFG 9000 ball: zaps nearby foes with green tendrils while flying. */
    static final ProjStyle BFG_ROE = ProjStyle.make().color(0x60ff50).size(1.5f).life(70).explode(6f).trail(ParticleTypes.HAPPY_VILLAGER)
            .onTick(p -> {
                if (p.age2 % 4 != 0 || !(p.getOwner() instanceof SlayerEntity o)) return;
                for (LivingEntity le : Combat.around(o, p.position(), 6)) {
                    Combat.damage(o, le, 3f);
                    Fx.beam((ServerLevel) p.level(), p.position(), le.position().add(0, le.getBbHeight() / 2, 0), 0x80ff60, 0.8f, 0.4);
                }
            });
    /** a hostile fireball caught by the Grabber and thrown back */
    static final ProjStyle GRAB_FIREBALL = ProjStyle.make().color(0xff7020).size(0.6f).life(50).explode(2.5f).trail(ParticleTypes.FLAME);

    public Doom3RoeMarine() {
        super("doom3_roe_marine", "Bravo Team Marine", "ブラボーチームの海兵隊員", "Doom 3: Resurrection of Evil");
        eggPrimary = 0x4a5040; eggSecondary = 0xff3a1a;
        maxHealth = 70; speed = 0.3; armor = 12; preferredRange = 9f;
        idleWeaponR = "machinegun";
    }

    // ---------------- server helpers ----------------
    static ServerLevel sl(SlayerEntity e) { return (ServerLevel) e.level(); }
    static Vec3 muz(SlayerEntity e, double fwd) { return Combat.muzzle(e, fwd, 0.35, 0); }
    static Vec3 aimDir(SlayerEntity e, LivingEntity t, Vec3 from, double spread) {
        Random r = new Random(e.getRandom().nextLong());
        if (t == null) { Vec3 l = e.getViewVector(1f); return l.add(r.nextGaussian() * spread, r.nextGaussian() * spread, r.nextGaussian() * spread).normalize(); }
        return Combat.dirTo(from, t, spread, r);
    }
    static void bullets(SlayerEntity e, LivingEntity t, int n, double spread, float dmg, double range, int rgb, float pitch) {
        Vec3 m = muz(e, 1.0);
        for (int i = 0; i < n; i++) Combat.hitscan(e, m, aimDir(e, t, m, spread), range, dmg, 0, rgb);
        Fx.burst(sl(e), ParticleTypes.FLAME, m, 3, 0.05, 0.02); Fx.burst(sl(e), ParticleTypes.SMOKE, m, 2, 0.05, 0.01);
        Combat.gunSound(e, pitch);
    }
    /** melee strike: every foe in a short cone */
    static void strike(SlayerEntity e, float dmg, double range, double kb, double up, int rgb) {
        for (LivingEntity f : Combat.cone(e, range, 55)) {
            Combat.damage(e, f, dmg); Combat.knock(f, e.position(), kb, up);
            Vec3 h = f.position().add(0, f.getBbHeight() * 0.6, 0);
            Fx.sparks(sl(e), h, 5); Fx.dustBurst(sl(e), rgb, 1.2f, h, 6, 0.2);
        }
    }
    static void effect(LivingEntity le, net.minecraft.world.effect.MobEffect ef, int dur, int amp) { le.addEffect(new MobEffectInstance(ef, dur, amp)); }

    @Override public void passive(SlayerEntity e) {
        // the Artifact pulses on the left arm
        if (e.tickCount % 12 == 0) Fx.dustBurst(sl(e), 0xff3018, 0.7f, Combat.muzzle(e, 0.25, -0.4, -0.55), 2, 0.06);
    }

    // ---------------- animation helpers ----------------
    /** weight-shifted shooting stance */
    static void stance(Pose p, float k) { p.rLegX += .28f * k; p.lLegX -= .32f * k; p.rLegZ += .06f * k; p.lLegZ -= .06f * k; p.rootY -= .45f * k; }
    /** blend arms into a two-handed shoulder aim; rec = recoil (pushes back, muzzle climbs) */
    static void aim(Pose p, float up, float rec) {
        p.rArmX = Keys.lerp(p.rArmX, -1.5708f - rec * .3f, up); p.lArmX = Keys.lerp(p.lArmX, -1.45f - rec * .2f, up);
        p.rArmY = Keys.lerp(p.rArmY, -.12f, up); p.lArmY = Keys.lerp(p.lArmY, .5f, up);
        p.rArmZ *= 1 - up; p.lArmZ *= 1 - up;
        p.bodyY -= .12f * up; p.headY += .12f * up;
        p.rHandX -= rec * .5f; p.bodyX -= rec * .18f; p.headX += rec * .08f; p.rootZ += rec * .7f;
    }
    /** raise 0..rise, hold, lower from fall..1 */
    static float up(float t, float rise, float fall) { return Keys.smooth(Keys.win(t, 0, rise)) * (1 - Keys.smooth(Keys.win(t, fall, 1))); }
    static float burst(float t, float a, float b, float shots) { return t < a || t > b ? 0 : (float) Math.abs(Math.sin((t - a) / (b - a) * shots * Keys.PI)); }

    @Override public void idle(Pose p, SlayerEntity e, float age, float limb) {
        // low-ready with the machine gun, Artifact arm cradling the foregrip
        float k = 1 - limb * .6f;
        p.rArmX += -.45f * k; p.lArmX += -.75f * k; p.lArmY += .55f * k; p.rArmY -= .1f * k;
        p.rHandX -= .35f * k;
        p.headX += (float) Math.sin(age * .05f) * .03f;
    }

    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---- FISTS: left jab -> right cross ----
        out.add(AttackDef.of("fists").range(0, 2.6f).dur(14).cd(18).weight(2).weapon("none")
                .tick((e, t, n) -> {
                    if (n == 5) { strike(e, 4f, 2.8, .3, .1, 0xaa2222); Combat.sound(e, SoundEvents.PLAYER_ATTACK_WEAK, 1f, 1f); }
                    if (n == 9) { strike(e, 5f, 2.8, .6, .15, 0xaa2222); Combat.sound(e, SoundEvents.PLAYER_ATTACK_STRONG, 1f, .9f); }
                })
                .anim((p, t, a) -> {
                    float jab = Keys.trackLin(t, 0, 0, .22f, -.4f, .36f, 1, .5f, .15f, 1, 0);
                    float cross = Keys.trackLin(t, 0, 0, .46f, -.4f, .64f, 1, .8f, .25f, 1, 0);
                    p.lArmX = -1.1f - .5f * jab; p.lArmY = .25f; p.lArmZ = 0;
                    p.rArmX = -1.0f - .6f * cross; p.rArmY = -.3f + .2f * cross; p.rArmZ = 0;
                    p.bodyY = .35f * jab - .5f * cross; p.headY -= p.bodyY * .7f;
                    p.bodyX = .1f + .1f * cross; p.lLegX = -.35f; p.rLegX = .25f + .1f * cross;
                    p.rootZ = -.8f * Math.max(0, jab) - 1.4f * Math.max(0, cross); p.rootY = -.3f;
                }));

        // ---- FLASHLIGHT BASH: overhead club swing with the Doom 3 torch ----
        out.add(AttackDef.of("flashlight_bash").range(0, 2.8f).dur(16).cd(40).weapon("flashlight")
                .tick((e, t, n) -> {
                    if (n == 3) Combat.sound(e, SoundEvents.PLAYER_ATTACK_SWEEP, .8f, 1.3f);
                    if (n == 8) {
                        strike(e, 7f, 3.0, .8, .25, 0xfff4c0);
                        Fx.burst(sl(e), ParticleTypes.FLASH, Combat.muzzle(e, 1.4, .2, 0), 1, 0, 0);
                        Combat.sound(e, SoundEvents.IRON_GOLEM_ATTACK, 1f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = Keys.track(t, 0, -.3f, .3f, -2.9f, .5f, -.6f, .65f, -.45f, 1, -.3f);
                    p.rArmZ = Keys.track(t, 0, .1f, .3f, .35f, .5f, -.1f, 1, .05f);
                    p.rHandX = Keys.track(t, 0, 0, .3f, -.6f, .5f, .4f, 1, 0);
                    p.lArmX = Keys.track(t, 0, -.3f, .3f, -1.0f, .5f, .35f, 1, -.2f); p.lArmZ = -.2f;
                    p.bodyX = Keys.track(t, 0, 0, .3f, -.22f, .5f, .38f, .7f, .3f, 1, 0);
                    p.bodyY = Keys.track(t, 0, 0, .3f, .3f, .5f, -.3f, 1, 0); p.headY -= p.bodyY * .6f;
                    p.lLegX = Keys.track(t, 0, 0, .45f, -.45f, 1, -.15f); p.rLegX = Keys.track(t, 0, 0, .45f, .3f, 1, .1f);
                    p.rootZ = Keys.track(t, 0, 0, .3f, .5f, .5f, -1.6f, 1, -.3f); p.rootY = -.4f * Keys.bump(t);
                }));

        // ---- PISTOL: three one-handed shots ----
        out.add(AttackDef.of("pistol").range(2, 20).dur(18).cd(22).weapon("pistol")
                .tick((e, t, n) -> { if (n == 4 || n == 9 || n == 14) bullets(e, t, 1, .02, 4f, 24, 0xffe090, 1.7f); })
                .anim((p, t, a) -> {
                    float u = up(t, .18f, .88f);
                    float rec = Keys.kick(t, .22f, .15f) + Keys.kick(t, .5f, .15f) + Keys.kick(t, .78f, .15f);
                    p.rArmX = Keys.lerp(p.rArmX, -1.57f - .3f * rec, u); p.rArmY = -.05f * u; p.rArmZ *= 1 - u;
                    p.rHandX = -.7f * rec;
                    p.lArmX = .2f * u; p.lArmZ = -.18f * u;
                    p.bodyY = -.35f * u; p.headY += .35f * u; p.bodyX = -.08f * rec;
                    p.rLegX = -.15f * u; p.lLegX = .22f * u; p.rootZ = .4f * rec;
                }));

        // ---- SHOTGUN: blast + pump ----
        out.add(AttackDef.of("shotgun").range(1.5f, 10).dur(22).cd(30).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 6) { bullets(e, t, 8, .08, 2.5f, 14, 0xffd070, .8f); Combat.sound(e, SoundEvents.GENERIC_EXPLODE, .5f, 1.8f); }
                    if (n == 12) Combat.sound(e, SoundEvents.PISTON_CONTRACT, .7f, 1.6f);
                    if (n == 15) Combat.sound(e, SoundEvents.PISTON_EXTEND, .7f, 1.8f);
                })
                .anim((p, t, a) -> {
                    float u = up(t, .2f, .86f), rec = Keys.kick(t, .27f, .3f) * 1.4f, pump = Keys.bump(Keys.win(t, .5f, .8f));
                    aim(p, u, rec); stance(p, u);
                    p.lArmX += .38f * pump; p.lArmY -= .15f * pump; p.rHandX += .15f * pump; p.headX += .1f * pump;
                    p.rLegX += .2f * rec;
                }));

        // ---- SUPER SHOTGUN (RoE): both barrels, break open, reload ----
        out.add(AttackDef.of("super_shotgun").range(1, 8).dur(30).cd(50).weapon("supershotgun")
                .tick((e, t, n) -> {
                    if (n == 7) {
                        bullets(e, t, 16, .13, 2.6f, 11, 0xffc060, .6f);
                        Combat.sound(e, SoundEvents.GENERIC_EXPLODE, .9f, 1.3f);
                        for (LivingEntity f : Combat.cone(e, 4, 35)) Combat.knock(f, e.position(), 1.0, .3);
                        Fx.burst(sl(e), ParticleTypes.LARGE_SMOKE, muz(e, 1.4), 6, .15, .03);
                    }
                    if (n == 14) Combat.sound(e, SoundEvents.IRON_TRAPDOOR_OPEN, .8f, 1.5f);
                    if (n == 21) Combat.sound(e, SoundEvents.CROSSBOW_LOADING_END, .8f, 1.0f);
                    if (n == 25) Combat.sound(e, SoundEvents.IRON_TRAPDOOR_CLOSE, .8f, 1.6f);
                })
                .anim((p, t, a) -> {
                    float u = up(t, .2f, .92f), rec = Keys.kick(t, .23f, .35f) * 2.0f, brk = Keys.bump(Keys.win(t, .45f, .9f));
                    aim(p, u, rec); stance(p, u * 1.2f);
                    p.rootZ += rec * .8f; p.rLegX += .3f * rec; p.bodyX -= .1f * rec;
                    // break-open: barrels tip down, gun lowered to chest, left hand fetches shells from belt
                    p.rHandX += .9f * brk; p.rArmX += .55f * brk;
                    if (t > .45f) {
                        p.lArmX = Keys.track(t, .45f, p.lArmX, .55f, .25f, .66f, .25f, .78f, -1.1f, .9f, -1.4f, 1, -1.4f);
                        p.lArmZ = -.25f * Keys.bump(Keys.win(t, .45f, .75f));
                    }
                    p.headX += .3f * brk;
                }));

        // ---- MACHINE GUN: sustained burst ----
        out.add(AttackDef.of("machine_gun").range(3, 22).dur(30).cd(40).weapon("machinegun")
                .tick((e, t, n) -> { if (n >= 5 && n <= 26 && (n - 5) % 3 == 0) bullets(e, t, 1, .035, 3f, 26, 0xffe080, 1.4f); })
                .anim((p, t, a) -> {
                    float u = up(t, .15f, .9f), rec = burst(t, .17f, .88f, 8) * .45f;
                    aim(p, u, rec); stance(p, u);
                    p.bodyY += .08f * (float) Math.sin(t * 6) * u; p.rootX += .1f * (float) Math.sin(a * 3) * rec;
                }));

        // ---- CHAINGUN: spin up, hose, wind down (hip fire) ----
        out.add(AttackDef.of("chaingun").range(3, 24).dur(50).cd(80).weapon("chaingun")
                .tick((e, t, n) -> {
                    if (n < 12 && n % 3 == 0) Combat.sound(e, SoundEvents.MINECART_RIDING, .6f, .8f + n * .08f);
                    if (n >= 12 && n <= 44 && n % 2 == 0) bullets(e, t, 1, .05, 3f, 26, 0xffd060, 1.9f);
                    if (n == 45) Combat.sound(e, SoundEvents.PISTON_CONTRACT, .8f, .5f);
                })
                .anim((p, t, a) -> {
                    float u = up(t, .15f, .9f), fire = t > .24f && t < .89f ? 1 : 0, spin = Keys.win(t, 0, .24f) * (1 - Keys.win(t, .89f, 1));
                    p.rArmX = Keys.lerp(p.rArmX, -1.1f, u); p.lArmX = Keys.lerp(p.lArmX, -1.0f, u);
                    p.lArmY = .65f * u; p.rArmY = -.1f * u; p.rArmZ *= 1 - u; p.lArmZ *= 1 - u;
                    p.rHandX = -.47f * u + .05f * fire * (float) Math.sin(a * 4);
                    p.rHandY = .15f * spin * (float) Math.sin(a * 2.5f); // barrel vibration
                    p.rLegZ = .16f * u; p.lLegZ = -.16f * u; p.rLegX = .2f * u; p.lLegX = -.2f * u; p.rootY = -.6f * u;
                    p.bodyX = -.12f * u - .06f * fire; p.bodyY = .1f * (float) Math.sin(t * 7) * fire;
                    p.rootX = .15f * (float) Math.sin(a * 3.3f) * fire; p.rootZ = .4f * fire;
                }));

        // ---- ROCKET LAUNCHER: shouldered, kneel-brace, big kick ----
        out.add(AttackDef.of("rocket_launcher").range(5, 28).dur(28).cd(60).weapon("rocket")
                .tick((e, t, n) -> {
                    if (n == 10) {
                        Vec3 m = muz(e, 1.2).add(0, .15, 0);
                        Combat.shoot(e, ProjStyle.ROCKET, m, aimDir(e, t, m, .01), 1.2, 14f);
                        Combat.sound(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.2f, .6f);
                        Fx.burst(sl(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -.6, .35, .15), 8, .2, .03);
                    }
                })
                .anim((p, t, a) -> {
                    float u = up(t, .25f, .85f), rec = Keys.kick(t, .36f, .35f) * 2f;
                    p.rArmX = Keys.lerp(p.rArmX, -1.35f - .3f * rec, u); p.rArmZ = .15f * u; p.rArmY = -.2f * u;
                    p.lArmX = Keys.lerp(p.lArmX, -1.55f, u); p.lArmY = .55f * u; p.lArmZ = 0;
                    p.rHandX = -.2f * u - .6f * rec; p.headZ = .12f * u; p.headY += .1f * u;
                    p.lLegX = -.55f * u; p.rLegX = .55f * u; p.rootY = -1.1f * u;
                    p.bodyX = .12f * u - .3f * rec; p.rootZ = 2f * rec;
                }));

        // ---- PLASMA GUN: rapid bolts ----
        out.add(AttackDef.of("plasma_gun").range(3, 22).dur(26).cd(40).weapon("plasma")
                .tick((e, t, n) -> {
                    if (n >= 5 && n <= 17 && (n - 5) % 3 == 0) {
                        Vec3 m = muz(e, 1.1);
                        Combat.shoot(e, ProjStyle.PLASMA, m, aimDir(e, t, m, .03), 1.6, 4f);
                        Combat.sound(e, SoundEvents.BEACON_POWER_SELECT, .5f, 1.9f);
                    }
                })
                .anim((p, t, a) -> {
                    float u = up(t, .16f, .85f), rec = burst(t, .19f, .7f, 5) * .35f;
                    aim(p, u, rec); stance(p, u);
                    p.bodyX += .08f * u; p.lArmY += .08f * rec;
                }));

        // ---- BFG 9000: charge, shake, release ----
        out.add(AttackDef.of("bfg").range(6, 28).dur(50).cd(260).weapon("bfg")
                .tick((e, t, n) -> {
                    ServerLevel l = sl(e); Vec3 m = muz(e, 1.3);
                    if (n == 2) Combat.sound(e, SoundEvents.BEACON_ACTIVATE, 1f, 1.6f);
                    if (n < 30) { Fx.dustBurst(l, 0x60ff50, 1.2f, m, 2 + n / 6, .15 + n * .01); if (n % 5 == 0) Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, m, 4, .3, .05); }
                    if (n == 32) {
                        Combat.shoot(e, BFG_ROE, m, aimDir(e, t, m, 0), .7, 30f);
                        Combat.sound(e, SoundEvents.WARDEN_SONIC_BOOM, 1f, 1.3f);
                        Fx.burst(l, ParticleTypes.FLASH, m, 1, 0, 0);
                    }
                })
                .anim((p, t, a) -> {
                    float u = up(t, .15f, .9f), ch = Keys.win(t, .1f, .62f) * (t < .64f ? 1 : 0), rec = Keys.kick(t, .64f, .3f) * 2.5f;
                    aim(p, u, rec); stance(p, u * 1.3f);
                    p.rootX += (float) Math.sin(a * 4) * .25f * ch; p.bodyX += .12f * ch; p.headX += .1f * ch;
                    p.rArmX -= .4f * rec; p.lArmX -= .3f * rec; p.rootZ += 1.5f * rec; p.bodyX -= .25f * rec;
                    p.rLegX += .35f * rec; p.lLegX += .2f * rec;
                }));

        // ---- GRENADE: pin pull, wind-up, overhand lob ----
        out.add(AttackDef.of("grenade").range(4, 16).dur(22).cd(90).weapon("grenade")
                .tick((e, t, n) -> {
                    if (n == 3) Combat.sound(e, SoundEvents.TRIPWIRE_CLICK_ON, 1f, 1.5f);
                    if (n == 11) {
                        Vec3 m = Combat.muzzle(e, .6, .3, .5), d = aimDir(e, t, m, .02);
                        double dist = t == null ? 8 : e.distanceTo(t);
                        Combat.shoot(e, ProjStyle.GRENADE, m, d.add(0, .15 + dist * .02, 0), .55 + dist * .035, 12f);
                        Combat.sound(e, SoundEvents.SNOWBALL_THROW, 1f, .6f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponR = t < .5f ? "grenade" : "none";
                    p.rArmX = Keys.track(t, 0, -.4f, .2f, -.9f, .45f, -3.3f, .55f, -1.2f, .75f, -.5f, 1, 0);
                    p.rArmZ = Keys.track(t, 0, 0, .45f, .3f, .6f, -.1f, 1, 0);
                    p.lArmX = Keys.track(t, 0, 0, .12f, -1.2f, .2f, -1.2f, .4f, -1.5f, .55f, .4f, 1, 0);
                    p.lArmY = Keys.track(t, 0, 0, .12f, .7f, .25f, .25f, 1, 0);
                    p.bodyY = Keys.track(t, 0, 0, .45f, .45f, .58f, -.5f, 1, 0); p.headY -= p.bodyY * .8f;
                    p.bodyX = Keys.track(t, 0, 0, .45f, -.2f, .58f, .35f, 1, 0);
                    p.lLegX = Keys.track(t, 0, 0, .4f, -.45f, 1, -.1f); p.rLegX = Keys.track(t, 0, 0, .5f, .35f, 1, .05f);
                    p.rootZ = Keys.track(t, 0, 0, .45f, .6f, .6f, -1.2f, 1, 0);
                }));

        // ---- CHAINSAW: rev, plunge, grind ----
        out.add(AttackDef.of("chainsaw").range(0, 2.6f).dur(30).cd(50).weapon("chainsaw")
                .tick((e, t, n) -> {
                    if (n == 0) Combat.sound(e, SoundEvents.MINECART_RIDING, 1f, 1.6f);
                    if (n >= 6 && n <= 24 && n % 3 == 0) {
                        strike(e, 2.5f, 2.9, .05, 0, 0x9a0a0a);
                        Combat.sound(e, SoundEvents.GRINDSTONE_USE, .9f, 1.4f);
                        Fx.burst(sl(e), ParticleTypes.CRIT, muz(e, 1.5), 3, .1, .2);
                    }
                })
                .anim((p, t, a) -> {
                    float u = up(t, .15f, .88f), grind = t > .2f && t < .82f ? 1 : 0, rev = Keys.kick(t, .02f, .18f);
                    p.rArmX = Keys.lerp(p.rArmX, -1.0f, u) + .12f * grind * (float) Math.sin(a * 3);
                    p.lArmX = Keys.lerp(p.lArmX, -1.25f, u); p.lArmY = .6f * u; p.rArmY = -.15f * u; p.rArmZ *= 1 - u; p.lArmZ *= 1 - u;
                    p.rHandX = -.5f * u - .3f * rev;
                    p.bodyX = .25f * grind + .1f * rev; p.headX += .15f * grind;
                    p.lLegX = -.4f * u; p.rLegX = .35f * u; p.rootY = -.5f * u;
                    p.rootZ = -1.2f * grind; p.rootX = .2f * grind * (float) Math.sin(a * 5);
                }));

        // ---- GRABBER (Ionized Plasma Levitator): beam-pull a foe or projectile, hold, hurl ----
        out.add(AttackDef.of("grabber").range(3, 14).dur(40).cd(100).weapon("grabber")
                .onStart((e, t, n) -> {
                    Entity held = t;
                    for (Projectile pr : e.level().getEntitiesOfClass(Projectile.class, e.getBoundingBox().inflate(14),
                            x -> !(x instanceof SlayerProjectile) && x.getOwner() instanceof LivingEntity o && Combat.isFoe(e, o))) { held = pr; break; }
                    e.getPersistentData().putInt("roeGrab", held == null ? -1 : held.getId());
                    Combat.sound(e, SoundEvents.BEACON_AMBIENT, 1f, 1.8f);
                })
                .tick((e, t, n) -> {
                    ServerLevel l = sl(e); Entity held = l.getEntity(e.getPersistentData().getInt("roeGrab"));
                    if (held == null || !held.isAlive()) return;
                    Vec3 m = muz(e, 1.0), hold = Combat.muzzle(e, 2.6, .2, .1);
                    if (n >= 4 && n < 26) {
                        Fx.beam(l, m, held.position().add(0, held.getBbHeight() / 2, 0), n % 2 == 0 ? 0xffa030 : 0xffe080, .7f, .35);
                        if (n % 4 == 0) Combat.sound(e, SoundEvents.BEACON_AMBIENT, .6f, 2f);
                    }
                    if (n >= 5 && n < 15) {
                        Vec3 v = hold.subtract(held.position().add(0, held.getBbHeight() / 2, 0));
                        held.setDeltaMovement(v.scale(.35)); held.hurtMarked = true; held.fallDistance = 0;
                    } else if (n >= 15 && n < 26) {
                        held.setPos(hold.x, hold.y - held.getBbHeight() / 2, hold.z); held.setDeltaMovement(Vec3.ZERO); held.hurtMarked = true; held.fallDistance = 0;
                        Fx.dustBurst(l, 0xffb040, .9f, hold, 3, .4); if (n % 3 == 0) Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, hold, 3, .4, .05);
                    } else if (n == 26) {
                        // aim at another foe if there is one, else straight ahead
                        LivingEntity aimAt = null; double best = 400;
                        for (LivingEntity f : Combat.around(e, e.position(), 18)) {
                            double d = f.distanceToSqr(e); if (f != held && d < best) { best = d; aimAt = f; }
                        }
                        Vec3 dir = aimAt != null ? aimAt.position().add(0, aimAt.getBbHeight() / 2, 0).subtract(hold).normalize() : e.getViewVector(1f);
                        if (held instanceof LivingEntity le) {
                            Combat.damage(e, le, 4f);
                            SlayerProjectile p = Combat.shoot(e, GRAB_HURL, hold.add(dir.scale(1.6)), dir, 1.5, 14f); p.aux = held.getId();
                        } else {
                            held.discard();
                            Combat.shoot(e, GRAB_FIREBALL, hold, aimAt != null || t == null ? dir : aimDir(e, t, hold, 0), 1.6, 12f);
                        }
                        Combat.sound(e, SoundEvents.WARDEN_SONIC_BOOM, .7f, 1.8f); Fx.burst(l, ParticleTypes.SONIC_BOOM, hold, 1, 0, 0);
                    }
                })
                .anim((p, t, a) -> {
                    float u = up(t, .12f, .9f), pull = Keys.win(t, .12f, .38f) * (t < .65f ? 1 : 0), hurl = Keys.kick(t, .65f, .3f) * 1.8f;
                    aim(p, u, 0); stance(p, u);
                    float strain = pull > 0 ? 1 : 0;
                    p.rArmX += .08f * strain * (float) Math.sin(a * 2) + .15f * strain; p.lArmX += .12f * strain;
                    p.bodyX -= .15f * strain; p.rootZ += .5f * strain; p.headX -= .05f * strain;
                    p.rArmX -= .3f * hurl; p.lArmX -= .25f * hurl; p.bodyX += .3f * hurl; p.rootZ -= 1.6f * hurl;
                    p.lLegX -= .3f * hurl; p.rLegX += .2f * hurl;
                }));

        // ---- ARTIFACT: HELL TIME (slow-time aura) ----
        out.add(AttackDef.of("hell_time").range(0, 12).dur(34).cd(400).weapon("none").noLos()
                .when((e, t) -> Combat.around(e, e.position(), 10).size() >= 2 || t.getMaxHealth() >= 40)
                .tick((e, t, n) -> {
                    ServerLevel l = sl(e); Vec3 c = e.position().add(0, 1, 0);
                    if (n < 14) Fx.burst(l, ParticleTypes.REVERSE_PORTAL, Combat.muzzle(e, .3, -.4, .1 + n * .05), 4, .1, .05);
                    if (n == 2) Combat.sound(e, SoundEvents.BEACON_ACTIVATE, 1f, .5f);
                    if (n == 14) {
                        Combat.sound(e, SoundEvents.BELL_RESONATE, 1.2f, .5f); Combat.sound(e, SoundEvents.ELDER_GUARDIAN_CURSE, .5f, 1.6f);
                        for (LivingEntity f : Combat.around(e, c, 10)) { effect(f, MobEffects.MOVEMENT_SLOWDOWN, 140, 3); effect(f, MobEffects.DIG_SLOWDOWN, 140, 2); }
                        effect(e, MobEffects.MOVEMENT_SPEED, 140, 1);
                    }
                    if (n >= 14) {
                        float r = (n - 13) * .6f;
                        Fx.ring(l, c.add(0, -.9, 0), r, 0xffc040, 1.4f, 24 + n);
                        Fx.ring(l, c.add(0, .3, 0), r * .8f, 0x8a40ff, 1.0f, 18 + n);
                    }
                })
                .anim((p, t, a) -> {
                    p.lArmX = Keys.track(t, 0, 0, .25f, -1.6f, .41f, -3.0f, .8f, -3.0f, 1, 0);
                    p.lArmY = Keys.track(t, 0, 0, .25f, .9f, .41f, .1f, 1, 0); p.lArmZ = Keys.track(t, .3f, 0, .41f, -.2f, 1, 0);
                    p.rArmX = Keys.track(t, 0, 0, .41f, -.4f, 1, 0); p.rArmZ = Keys.track(t, 0, 0, .41f, .45f, .8f, .45f, 1, 0);
                    p.bodyX = Keys.track(t, 0, 0, .25f, .2f, .41f, -.25f, .8f, -.2f, 1, 0);
                    p.headX = Keys.track(t, 0, 0, .25f, .3f, .41f, -.45f, .8f, -.35f, 1, 0);
                    p.lLegZ = -.15f * Keys.bump(t); p.rLegZ = .15f * Keys.bump(t);
                    p.rootY = Keys.track(t, 0, 0, .25f, -.6f, .41f, .4f, .8f, .3f, 1, 0);
                    p.rootYaw = .15f * (float) Math.sin(a * .3f) * Keys.win(t, .41f, .8f);
                }));

        // ---- ARTIFACT: BERSERK (red frenzy triple punch) ----
        out.add(AttackDef.of("berserk").range(0, 3).dur(30).cd(300).weapon("none")
                .onStart((e, t, n) -> { effect(e, MobEffects.DAMAGE_BOOST, 200, 1); Combat.sound(e, SoundEvents.RAVAGER_ROAR, 1f, 1.2f); })
                .tick((e, t, n) -> {
                    ServerLevel l = sl(e);
                    Fx.dustBurst(l, n % 2 == 0 ? 0xff1010 : 0x900000, 1.3f, e.position().add(0, 1, 0), 3, .45);
                    if (n == 10 || n == 16) { Combat.lunge(e, .35, 0); strike(e, 7f, 3.0, .5, .15, 0xff2020); Combat.sound(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, .7f); }
                    if (n == 23) {
                        Combat.lunge(e, .3, .15); strike(e, 10f, 3.0, .9, .7, 0xff2020);
                        Combat.sound(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, .6f); Fx.burst(l, ParticleTypes.DAMAGE_INDICATOR, Combat.muzzle(e, 1.5, 0, 0), 8, .3, .1);
                    }
                })
                .anim((p, t, a) -> {
                    float roar = Keys.bump(Keys.win(t, 0, .27f)), shake = (float) Math.sin(a * 4) * .08f;
                    float h1 = Keys.trackLin(t, .25f, 0, .29f, -.4f, .33f, 1, .42f, 0);
                    float h2 = Keys.trackLin(t, .44f, 0, .49f, -.4f, .53f, 1, .62f, 0);
                    float h3 = Keys.trackLin(t, .64f, 0, .7f, -.5f, .77f, 1, .9f, .3f, 1, 0);
                    p.rArmZ = .9f * roar; p.lArmZ = -.9f * roar; p.rArmX = -.3f * roar; p.lArmX = -.3f * roar;
                    p.bodyX = -.3f * roar; p.headX = -.5f * roar; p.rootY = -.35f * roar;
                    // left hook
                    p.lArmX += -1.4f * Math.abs(h1); p.lArmY = h1 < 0 ? -.4f * -h1 : .9f * h1;
                    // right hook
                    p.rArmX += -1.4f * Math.abs(h2); p.rArmY = h2 < 0 ? .4f * -h2 : -.9f * h2;
                    // left uppercut (wind low, drive up)
                    p.lArmX += h3 < 0 ? .5f * -h3 : -2.6f * h3;
                    p.bodyY = .6f * h1 - .6f * h2 + .3f * h3; p.headY -= p.bodyY * .6f;
                    p.bodyX += h3 < 0 ? .3f * -h3 : -.3f * h3;
                    p.rootY += h3 > 0 ? 1.2f * h3 : .4f * h3;
                    p.rootZ = -1.0f * Math.max(0, h1) - 1.0f * Math.max(0, h2) - .8f * Math.max(0, h3);
                    p.lLegX = -.35f * (1 - roar); p.rLegX = .3f * (1 - roar) + .2f * Math.max(0, h3);
                    p.rootX += shake;
                }));

        // ---- ARTIFACT: INVULNERABILITY (shield pulse) ----
        out.add(AttackDef.of("invulnerability").range(0, 10).dur(28).cd(500).weapon("none").noLos()
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * .6f || Combat.around(e, e.position(), 5).size() >= 3)
                .tick((e, t, n) -> {
                    ServerLevel l = sl(e); Vec3 c = e.position().add(0, 1, 0);
                    if (n < 11) { Fx.burst(l, ParticleTypes.END_ROD, c, 3, 1.2 - n * .1, .0); if (n == 1) Combat.sound(e, SoundEvents.BEACON_POWER_SELECT, 1f, .7f); }
                    if (n == 11) {
                        effect(e, MobEffects.DAMAGE_RESISTANCE, 140, 3);
                        for (LivingEntity f : Combat.around(e, c, 6)) { Combat.damage(e, f, 3f); Combat.knock(f, e.position(), 1.7, .5); }
                        Combat.sound(e, SoundEvents.SHIELD_BLOCK, 1.2f, .6f); Combat.sound(e, SoundEvents.TOTEM_USE, .6f, 1.4f);
                        Fx.burst(l, ParticleTypes.FLASH, c, 1, 0, 0);
                    }
                    if (n >= 11 && n <= 22) {
                        float r = (n - 10) * .55f;
                        Fx.ring(l, c.add(0, -.6, 0), r, 0x60e0ff, 1.5f, 28); Fx.ring(l, c.add(0, .4, 0), r * .9f, 0xffffff, 1.0f, 20);
                    }
                })
                .anim((p, t, a) -> {
                    float g = Keys.smooth(Keys.win(t, 0, .35f)) * (t < .39f ? 1 : 0), burst = Keys.kick(t, .39f, .45f);
                    p.rArmX = -1.4f * g - .6f * burst; p.rArmY = -.9f * g; p.rArmZ = 1.2f * burst;
                    p.lArmX = -1.4f * g - .6f * burst; p.lArmY = .9f * g; p.lArmZ = -1.2f * burst;
                    p.bodyX = .4f * g - .25f * burst; p.headX = .3f * g - .3f * burst;
                    p.rootY = -1.1f * g + .5f * burst;
                    p.rLegX = .45f * g; p.lLegX = -.5f * g; p.rLegZ = .25f * burst; p.lLegZ = -.25f * burst;
                }));
    }

    // ---------------- model ----------------
    @Override
    public void buildModel(Rig rig) {
        Mat suit = Mat.of(0x34392f).noise(.1f);
        Mat armor = Mat.of(0x5c6550).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.FRONT && w >= 6 && h >= 5) { g.rect(x + 1, y + 1, w - 2, 1, 0xa8a070); g.rect(x + w / 2, y + 2, 1, 2, 0xd0c060); } // UAC trim
        });
        Mat dark = Mat.of(0x272a24).noise(.12f);
        Mat helmet = Mat.of(0x6a715d).bevel(.25f);
        Mat visor = Mat.of(0x1c1a14).noise(.02f).bevel(.1f);
        Mat lamp = Mat.of(0xfff0c0).glow();
        Mat visorGlow = Mat.of(0xffa040).glow();
        Mat art = Mat.of(0xff3a18).glow().decal((m, g, x, y, w, h, f) -> {
            for (int i = 0; i < w; i += 2) g.glow(x + i, y + (i * 7 % Math.max(1, h)), 0xffd060); // hot veins
        });
        Mat artShell = Mat.of(0x4a1a12).noise(.25f).decal((m, g, x, y, w, h, f) -> {
            for (int j = 1; j < h; j += 3) g.glow(x + (j * 5) % Math.max(1, w), y + j, 0xc02010);
        });
        Mat gun = Mat.of(0x2b2c2e).noise(.06f), metal = Mat.of(0x5a5e63).bevel(.2f), wood = Mat.of(0x5a3a20).noise(.15f);

        rig.humanoid(suit, suit, helmet);
        // torso: chest plate, ab plate, belt, backpack
        rig.box(rig.torso, armor, -4.5f, -12.3f, -2.8f, 9, 7, 1);
        rig.box(rig.torso, armor, -3, -5, -2.6f, 6, 2, 1);
        rig.box(rig.torso, dark, -4, -3, -2, 8, 1, 4, .35f);
        rig.box(rig.torso, dark, -3, -11, 2, 6, 7, 2);
        rig.box(rig.torso, lamp, -2.5f, -10, 4, 1, 1, 1);
        // helmet: visor, glow strip, chin guard, crest, shoulder lamp
        rig.box(rig.head, visor, -3.5f, -6, -4.6f, 7, 3, 1);
        rig.box(rig.head, visorGlow, -3, -5, -4.9f, 6, 1, 1);
        rig.box(rig.head, armor, -3, -2.5f, -4.7f, 6, 2, 1);
        rig.box(rig.head, armor, -1, -9, -4, 2, 2, 7);
        rig.box(rig.head, dark, -5, -6, -2, 1, 3, 3);
        // shoulders + forearm guard (right), Artifact gauntlet (left)
        rig.box(rig.rArm, armor, -3.5f, -3, -2.5f, 4, 4, 5);
        rig.box(rig.lArm, armor, -.5f, -3, -2.5f, 4, 4, 5);
        rig.box(rig.rArm, armor, -2, 4, -2, 4, 4, 4, .4f);
        rig.box(rig.lArm, artShell, -2, 4, -2, 4, 6, 4, .6f);
        rig.box(rig.lArm, art, -1, 5.5f, -3.2f, 2, 3, 1);
        rig.box(rig.lArm, art, 2.3f, 5, -1, 1, 4, 2);
        rig.box(rig.lArm, art, -1.5f, 9.6f, -2.8f, 3, 1, 1);
        // legs: knee pads, shin guards, boots, thigh pouch
        for (PartDefinition leg : new PartDefinition[]{rig.rLeg, rig.lLeg}) {
            rig.box(leg, armor, -2, 4, -2.7f, 4, 3, 1);
            rig.box(leg, dark, -2, 9, -2, 4, 3, 4, .3f);
        }
        rig.box(rig.rLeg, dark, -3, 1, -1, 1, 3, 2);

        // ---- weapons (gun space: origin = fist, barrel -z, y down) ----
        PartDefinition w;
        w = rig.weapon(rig.rHand, "pistol");
        rig.box(w, gun, -.5f, -2.5f, -5, 1, 2, 6); rig.box(w, metal, -.5f, -3.2f, -5, 1, 1, 5); rig.box(w, gun, -.5f, -1, -.5f, 1, 3, 1);

        w = rig.weapon(rig.rHand, "shotgun");
        rig.box(w, gun, -1, -3, -4, 2, 2, 6); rig.box(w, metal, -.5f, -3.6f, -12, 1, 1, 8);
        rig.box(w, wood, -1, -2.4f, -10, 2, 1, 4); rig.box(w, wood, -1, -2.2f, 2, 2, 2, 5); rig.box(w, gun, -.5f, -1, -.5f, 1, 2, 1);

        w = rig.weapon(rig.rHand, "supershotgun");
        rig.box(w, metal, -1.3f, -3.6f, -11, 1, 1, 8); rig.box(w, metal, .3f, -3.6f, -11, 1, 1, 8);
        rig.box(w, gun, -1.2f, -3.2f, -3, 2, 2, 3); rig.box(w, wood, -1, -2.6f, 0, 2, 2, 5); rig.box(w, gun, -.5f, -1, -.5f, 1, 2, 1);

        w = rig.weapon(rig.rHand, "machinegun");
        rig.box(w, metal, -1, -3.5f, -6, 2, 3, 8); rig.box(w, gun, -.5f, -3, -10, 1, 1, 4);
        rig.box(w, gun, -.5f, -.5f, -4.5f, 1, 3, 2); rig.box(w, gun, -1, -3, 2, 2, 2, 4);
        rig.box(w, Mat.of(0x40c0ff).glow(), 1, -3.2f, -1, 1, 1, 2); rig.box(w, gun, -.5f, -1, -.5f, 1, 2, 1);

        w = rig.weapon(rig.rHand, "chaingun");
        rig.box(w, metal, -2, -4, -4, 4, 4, 6); rig.box(w, gun, -2, -4, -9, 4, 4, 1);
        rig.box(w, gun, -1.5f, -3.5f, -13, 1, 1, 9); rig.box(w, gun, .5f, -3.5f, -13, 1, 1, 9);
        rig.box(w, gun, -1.5f, -1.5f, -13, 1, 1, 9); rig.box(w, gun, .5f, -1.5f, -13, 1, 1, 9);
        rig.box(w, dark, -3, -3, -2, 1, 3, 3); rig.box(w, gun, -.5f, -6, -3, 1, 2, 3);

        w = rig.weapon(rig.rHand, "rocket");
        Mat tube = Mat.of(0x4e5548).bevel(.25f);
        rig.box(w, tube, -1.5f, -5, -11, 3, 3, 13); rig.box(w, gun, -2, -5.5f, -12, 4, 4, 1);
        rig.box(w, gun, -2, -5.5f, 2, 4, 4, 2); rig.box(w, metal, 1.5f, -6, -4, 1, 1, 2); rig.box(w, gun, -.5f, -2, -.5f, 1, 2, 1);

        w = rig.weapon(rig.rHand, "plasma");
        Mat plasmaGlow = Mat.of(0x50b0ff).glow();
        rig.box(w, Mat.of(0x6c7a88).bevel(.25f), -1.5f, -4, -6, 3, 3, 8); rig.box(w, plasmaGlow, -1, -3.5f, -9, 2, 2, 3);
        rig.box(w, plasmaGlow, 1.5f, -3.5f, -4, 1, 2, 3); rig.box(w, gun, -1, -3.5f, 2, 2, 2, 3); rig.box(w, gun, -.5f, -1, -.5f, 1, 2, 1);

        w = rig.weapon(rig.rHand, "bfg");
        Mat bfgGlow = Mat.of(0x60ff50).glow();
        rig.box(w, Mat.of(0x3e4a3a).bevel(.3f), -2.5f, -5.5f, -8, 5, 5, 10); rig.box(w, bfgGlow, -1.5f, -4.5f, -10, 3, 3, 2);
        rig.box(w, bfgGlow, 2.5f, -4, -5, 1, 2, 4); rig.box(w, gun, -.5f, -7, -5, 1, 1, 4); rig.box(w, gun, -.5f, -1, -.5f, 1, 2, 1);

        w = rig.weapon(rig.rHand, "chainsaw");
        rig.box(w, Mat.of(0xb83a1a).bevel(.25f), -1.5f, -3, -3, 3, 3, 5); rig.box(w, metal, -.5f, -2.5f, -13, 1, 2, 10);
        rig.box(w, gun, -.6f, -3.1f, -13.3f, 1, 1, 10, .1f); rig.box(w, gun, -2, -5, -2, 4, 1, 1); rig.box(w, gun, -.5f, -1, -.5f, 1, 2, 1);

        w = rig.weapon(rig.rHand, "flashlight");
        rig.box(w, Mat.of(0x30353a), -1, -1, -7, 2, 2, 8); rig.box(w, gun, -1.5f, -1.5f, -9, 3, 3, 2); rig.box(w, lamp, -1, -1, -9.3f, 2, 2, 1);

        w = rig.weapon(rig.rHand, "grabber");
        Mat grabGlow = Mat.of(0xffa030).glow();
        rig.box(w, Mat.of(0x55606a).bevel(.25f), -1.5f, -4, -6, 3, 3, 8); rig.box(w, grabGlow, -1, -3.5f, -7, 2, 2, 1);
        rig.box(w, metal, -2.2f, -5, -10, 1, 1, 4); rig.box(w, metal, 1.2f, -5, -10, 1, 1, 4); rig.box(w, metal, -.5f, -2, -10, 1, 1, 4);
        rig.box(w, gun, -.5f, -1, -.5f, 1, 2, 1);

        w = rig.weapon(rig.rHand, "grenade");
        rig.box(w, Mat.of(0x4a5a30).noise(.2f), -1, -1.5f, -1, 2, 3, 2); rig.box(w, metal, -.5f, -2.5f, -.5f, 1, 1, 1);
    }
}
