package com.doomslayer.kit.kits;

import static com.doomslayer.anim.Keys.*;

import com.doomslayer.anim.Pose;
import com.doomslayer.client.Mat;
import com.doomslayer.client.Rig;
import com.doomslayer.entity.ProjStyle;
import com.doomslayer.fx.Combat;
import com.doomslayer.fx.Fx;
import com.doomslayer.entity.SlayerEntity;
import com.doomslayer.kit.AttackDef;
import com.doomslayer.kit.SlayerKit;
import java.util.List;
import java.util.Random;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Gordon Freeman, Half-Life (1998): HEV Mark IV, crowbar and the full Black Mesa arsenal. */
public class HalfLifeGordon extends SlayerKit {
    static final int HEV = 0xd9731a, GAUSS = 0xffc050, EGON = 0x60c8ff, HIVE = 0xffd040, SNARK = 0xc8b060;

    static final ProjStyle GLAUNCH = ProjStyle.make().color(0x4d5a3a).size(0.25f).life(60).gravity(0.045).explode(3.2f).trail(ParticleTypes.SMOKE);
    static final ProjStyle BOLT = ProjStyle.make().color(0xff9a40).size(0.16f).life(60).pierce(1).trail(ParticleTypes.SMALL_FLAME);
    static final ProjStyle ROCKET = ProjStyle.make().color(0xffa040).size(0.45f).life(110).explode(3.8f).homing(0.22f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle HORNET = ProjStyle.make().color(HIVE).size(0.14f).life(70).homing(0.35f).trail(ParticleTypes.CRIT);
    static final ProjStyle FRAG = ProjStyle.make().color(0x3e4a2c).size(0.26f).life(55).gravity(0.05).explode(3.8f).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle SATCHEL = ProjStyle.make().color(0x6a5a3a).size(0.3f).life(40).gravity(0.05).explode(4.6f).trail(ParticleTypes.SMOKE);
    static final ProjStyle MINE = ProjStyle.make().color(0x555555).size(0.2f).life(30).gravity(0.06).explode(4.2f);
    static final ProjStyle SQUEAK = ProjStyle.make().color(SNARK).size(0.3f).life(80).gravity(0.04).homing(0.2f).bounce().explode(1.8f).trail(ParticleTypes.ITEM_SLIME);

    public HalfLifeGordon() {
        super("half_life_gordon", "Gordon Freeman (HL1)", "ゴードン・フリーマン(HL1)", "Half-Life");
        eggPrimary = HEV; eggSecondary = 0x303030;
        maxHealth = 100; speed = 0.33; armor = 10; knockbackRes = 0.3;
        scale = 1.0f; preferredRange = 9f; dmgMult = 1.0f;
        idleWeaponR = "crowbar";
    }

    // ---------- server helpers ----------
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.15, 0.3, 0); }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        return Combat.dirTo(mz(e), t, spread, new Random(e.getRandom().nextLong()));
    }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static void flash(SlayerEntity e) {
        Vec3 m = mz(e);
        Fx.burst(L(e), ParticleTypes.FLAME, m, 2, 0.04, 0.02); Fx.burst(L(e), ParticleTypes.SMOKE, m, 2, 0.04, 0.02);
    }
    static void pellets(SlayerEntity e, LivingEntity t, int n, double spread, float dmg) {
        Vec3 m = mz(e);
        for (int i = 0; i < n; i++) Combat.hitscan(e, m, aim(e, t, spread), 20, dmg, 0, 0xffd070);
    }
    static void slash(SlayerEntity e, float from, float to, float tilt) {
        Fx.arc(L(e), e.position().add(0, 1.3, 0), e.getYRot(), 1.5f, from, to, tilt, 0xc8c8c8, 0.6f, 9);
    }
    static void melee(SlayerEntity e, float dmg, double kb, double up) {
        for (LivingEntity f : Combat.cone(e, 3.3, 55)) {
            Combat.damage(e, f, dmg); Combat.knock(f, e.position(), kb, up); Fx.sparks(L(e), mid(f), 5);
            snd(e, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 1.5f);
        }
    }

    // ---------- anim helpers ----------
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    static void shoulder(Pose p, float k, float pitch, float recoil) {
        float tw = 0.22f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.15f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.05f - recoil * 0.22f, k); p.lArmY = lerp(p.lArmY, 0.45f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * k; p.bodyX += (recoil * 0.12f - pitch * 0.2f) * k; p.rootZ += recoil * 0.9f * k;
    }
    static void pistol2(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.45f, k); p.rArmY = lerp(p.rArmY, -0.1f, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.08f - recoil * 0.4f, k); p.lArmY = lerp(p.lArmY, 0.42f, k);
        p.rHandX = lerp(p.rHandX, -recoil * 0.45f, k); p.bodyX -= recoil * 0.06f * k; p.rootZ += recoil * 0.5f * k;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.26f, k); p.lLegZ -= 0.06f * k; p.rLegZ += 0.08f * k; p.rootY -= 0.4f * k;
    }
    static void throwAnim(Pose p, float t) {
        p.rArmX = track(t, 0, -0.2f, .25f, -2.6f, .5f, -0.4f, .7f, -1.4f, 1, -0.2f);
        p.rArmZ = track(t, 0, 0.1f, .25f, 0.5f, .5f, -0.2f, 1, 0.1f);
        p.lArmX = track(t, 0, 0, .25f, -1f, .5f, 0.3f, 1, 0);
        p.bodyY = track(t, 0, 0, .25f, 0.5f, .5f, -0.5f, .8f, -0.2f, 1, 0); p.headY = -p.bodyY * 0.5f;
        p.bodyX = track(t, 0, 0, .25f, -0.2f, .5f, 0.35f, 1, 0);
        p.rLegX = track(t, 0, 0, .25f, 0.3f, .5f, -0.4f, 1, 0); p.lLegX = track(t, 0, 0, .25f, -0.3f, .5f, 0.4f, 1, 0);
    }
    static void crouchPlace(Pose p, float t) {
        float d = bump(win(t, .1f, .9f));
        p.rootY -= 4f * d; p.bodyX += 0.7f * d; p.headX -= 0.4f * d; p.lLegX -= 1.2f * d; p.rLegX -= 0.9f * d;
        p.rArmX = lerp(p.rArmX, -0.9f, d); p.lArmX = lerp(p.lArmX, -0.7f, d); p.rootZ -= 1.5f * d;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        p.rArmX = -0.25f + p.rArmX * 0.5f; p.rArmZ = 0.12f; p.rHandX = 0.55f;
        p.headX += 0.04f; p.bodyX += 0.03f;
    }

    @Override
    public void passive(SlayerEntity e) {
        // HEV Mark IV: suit resistance + morphine/medical auto-injection when critical
        if (e.tickCount % 20 == 0) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false));
        if (e.tickCount % 240 == 0 && e.getHealth() < e.getMaxHealth() * 0.35f) {
            e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, false));
            snd(e, SoundEvents.NOTE_BLOCK_BIT.value(), 0.8f, 1.2f);
        }
    }

    /** compact hitscan/shot weapon definition: shots at given ticks, recoil kick on each */
    private static AttackDef gun(String id, String weapon, float min, float max, int cd, int dur, int[] shots, SoundEvent s, float pitch,
            int pellets, double spread, float dmg, boolean twoHand) {
        return AttackDef.of(id).range(min, max).cd(cd).weight(3).dur(dur).weapon(weapon)
                .tick((e, t, n) -> {
                    for (int sh : shots) if (n == sh) {
                        pellets(e, t, pellets, spread, dmg); flash(e); snd(e, s, 0.9f, pitch);
                        Combat.knock(e, e.position().add(e.getViewVector(1f)), 0.05, 0);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), r = 0;
                    for (int sh : shots) r = Math.max(r, 1.6f * kick(t, sh / (float) dur, .22f));
                    if (twoHand) pistol2(p, k, 0, r); else shoulder(p, k, 0, r);
                    stance(p, k);
                });
    }

    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- Crowbar: side swing ----------
        out.add(AttackDef.of("crowbar_side").range(0, 3.2f).cd(14).weight(3).dur(16).weapon("crowbar")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.4f);
                    if (n == 5) { slash(e, 70, -50, 25); melee(e, 6, 0.5, 0.15); }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, 0, .2f, -2.2f, .35f, -1.1f, .55f, -1.4f, 1, -0.2f);
                    p.rArmZ = track(t, 0, 0.1f, .2f, 0.6f, .35f, -0.5f, .6f, -0.7f, 1, 0.12f);
                    p.rHandX = track(t, 0, 0.5f, .2f, 0.7f, .35f, -0.4f, 1, 0.55f);
                    p.lArmX = track(t, 0, 0, .2f, -0.8f, 1, 0);
                    p.bodyY = track(t, 0, 0, .2f, 0.5f, .35f, -0.55f, .6f, -0.6f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.lLegX = track(t, 0, 0, .25f, -0.4f, 1, 0); p.rLegX = track(t, 0, 0, .25f, 0.3f, 1, 0);
                    p.rootZ = -1.6f * bump(win(t, .15f, .5f));
                }));

        // ---------- Crowbar: overhead chop ----------
        out.add(AttackDef.of("crowbar_overhead").range(0, 3.2f).cd(24).weight(2).dur(20).weapon("crowbar")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 0.9f);
                    if (n == 8) { slash(e, 0, 0, -80); melee(e, 9, 0.6, 0.3); }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -0.2f, .25f, -3.0f, .4f, -1.1f, .6f, -0.9f, 1, -0.2f);
                    p.lArmX = track(t, 0, 0, .25f, -2.6f, .4f, -1.0f, .6f, -0.8f, 1, 0);
                    p.rHandX = track(t, 0, 0.55f, .25f, 1.0f, .4f, -0.3f, 1, 0.55f);
                    p.bodyX = track(t, 0, 0, .25f, -0.3f, .4f, 0.55f, .7f, 0.35f, 1, 0); p.headX = -p.bodyX * 0.5f;
                    p.rootY = track(t, 0, 0, .25f, 0.5f, .4f, -1.4f, .7f, -0.8f, 1, 0);
                    p.lLegX = track(t, 0, 0, .4f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .4f, 0.3f, 1, 0);
                    p.rootZ = -1.8f * bump(win(t, .25f, .6f));
                }));

        // ---------- Crowbar: charged smash (wind up, then heavy blow) ----------
        out.add(AttackDef.of("crowbar_charged").range(0, 3.4f).cd(70).weight(1).dur(34).weapon("crowbar")
                .tick((e, t, n) -> {
                    if (n == 6) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.8f, 0.8f);
                    if (n == 20) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.1f, 0.6f);
                    if (n == 23) {
                        slash(e, 80, -70, 30); melee(e, 14, 1.4, 0.45);
                        Fx.ring(L(e), e.position().add(0, 0.2, 0), 2.2f, 0xaaaaaa, 1.5f, 14);
                        snd(e, SoundEvents.ANVIL_LAND, 0.6f, 1.5f);
                    }
                })
                .anim((p, t, a) -> {
                    float w = win(t, .1f, .55f) * (1 - win(t, .6f, .64f)), sh = 0.03f * (float) Math.sin(a * 7f) * w;
                    p.rArmX = track(t, 0, -0.2f, .5f, -2.6f, .68f, -1.0f, .85f, -0.9f, 1, -0.2f) + sh;
                    p.rArmZ = track(t, 0, 0.1f, .5f, 0.9f, .68f, -0.7f, 1, 0.12f);
                    p.lArmX = track(t, 0, 0, .5f, -1.4f, .68f, -0.6f, 1, 0); p.rHandX = track(t, 0, 0.55f, .5f, 0.9f, .68f, -0.5f, 1, 0.55f);
                    p.bodyY = track(t, 0, 0, .55f, 0.9f, .68f, -0.8f, .9f, -0.3f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.bodyX = track(t, 0, 0, .55f, -0.25f, .68f, 0.4f, 1, 0);
                    p.rootY = track(t, 0, 0, .55f, -1f, .68f, -0.4f, 1, 0); p.rootZ = -2f * bump(win(t, .5f, .8f));
                    p.lLegX = -0.5f * w; p.rLegX = 0.35f * w;
                }));

        // ---------- 9mm Glock ----------
        out.add(gun("glock", "glock", 3, 20, 14, 10, new int[] {3}, SoundEvents.DISPENSER_LAUNCH, 1.9f, 1, 0.02, 4.5f, true));
        out.add(gun("glock_burst", "glock", 3, 20, 40, 18, new int[] {3, 7, 11}, SoundEvents.DISPENSER_LAUNCH, 1.8f, 1, 0.05, 3.5f, true));

        // ---------- .357 Python ----------
        out.add(gun("magnum", "magnum", 3, 28, 36, 20, new int[] {5}, SoundEvents.GENERIC_EXPLODE, 1.9f, 1, 0.005, 14f, true));

        // ---------- MP5 + AR grenade ----------
        out.add(gun("mp5", "mp5", 3, 22, 40, 22, new int[] {3, 5, 7, 9, 11, 13, 15}, SoundEvents.DISPENSER_LAUNCH, 1.5f, 1, 0.07, 3f, false));
        out.add(AttackDef.of("mp5_grenade").range(5, 22).cd(100).weight(2).dur(22).weapon("mp5")
                .tick((e, t, n) -> {
                    if (n == 8) {
                        Vec3 d = aim(e, t, 0.01).add(0, 0.12, 0).normalize();
                        Combat.shoot(e, GLAUNCH, mz(e), d, 1.3, 8f); flash(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1f, 0.7f); snd(e, SoundEvents.PISTON_EXTEND, 0.7f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f), r = 2.2f * kick(t, 8 / 22f, .3f);
                    shoulder(p, k, 0.25f, r); stance(p, k);
                }));

        // ---------- SPAS-12 / shotgun: single + double barrel ----------
        out.add(AttackDef.of("shotgun").range(0, 14).cd(34).weight(3).dur(26).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 5) { pellets(e, t, 6, 0.09, 3.2f); flash(e); snd(e, SoundEvents.GENERIC_EXPLODE, 0.6f, 1.8f); }
                    if (n == 16) { snd(e, SoundEvents.PISTON_CONTRACT, 0.8f, 1.5f); snd(e, SoundEvents.PISTON_EXTEND, 0.8f, 1.7f); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), r = 2.4f * kick(t, 5 / 26f, .25f);
                    shoulder(p, k, 0, r); stance(p, k);
                    p.lArmX += 0.3f * bump(win(t, .55f, .8f)); // pump
                }));
        out.add(AttackDef.of("shotgun_double").range(0, 10).cd(70).weight(2).dur(30).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 6) {
                        pellets(e, t, 12, 0.12, 3.2f); flash(e); flash(e);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 1f, 1.5f); snd(e, SoundEvents.IRON_GOLEM_ATTACK, 0.8f, 1.4f);
                        Combat.knock(e, e.position().add(e.getViewVector(1f)), 0.4, 0.1);
                    }
                    if (n == 20) snd(e, SoundEvents.PISTON_EXTEND, 0.8f, 1.5f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), r = 3.4f * kick(t, 6 / 30f, .3f);
                    shoulder(p, k, 0, r); stance(p, k); p.rootY -= 0.5f * k; p.bodyX -= 0.2f * r;
                }));

        // ---------- Crossbow ----------
        out.add(AttackDef.of("crossbow").range(6, 30).cd(60).weight(2).dur(26).weapon("crossbow")
                .tick((e, t, n) -> {
                    if (n == 7) {
                        Combat.shoot(e, BOLT, mz(e), aim(e, t, 0.0), 2.4, 15f);
                        snd(e, SoundEvents.CROSSBOW_SHOOT, 1f, 0.9f);
                    }
                    if (n == 14) snd(e, SoundEvents.CROSSBOW_LOADING_MIDDLE, 0.8f, 1f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), r = 1.4f * kick(t, 7 / 26f, .25f);
                    shoulder(p, k, 0.05f, r); stance(p, k); p.lArmX += 0.3f * bump(win(t, .5f, .8f));
                }));

        // ---------- RPG (laser-guided) ----------
        out.add(AttackDef.of("rpg").range(6, 32).cd(130).weight(2).dur(34).weapon("rpg")
                .tick((e, t, n) -> {
                    if (n >= 6 && n < 18 && n % 2 == 0 && t != null) Fx.beam(L(e), mz(e), mid(t), 0xff2a1a, 0.3f, 0.2);
                    if (n == 18) {
                        Combat.shoot(e, ROCKET, mz(e), aim(e, t, 0.0), 1.1, 12f); flash(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.6f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .88f), r = 3f * kick(t, 18 / 34f, .3f);
                    shoulder(p, k, 0.1f, r); wideLegs(p, k);
                }));

        // ---------- Gauss: quick shot ----------
        out.add(AttackDef.of("gauss").range(4, 30).cd(40).weight(2).dur(18).weapon("tau")
                .tick((e, t, n) -> {
                    if (n == 5) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.0), 40, 9f, 0, GAUSS);
                        if (t != null) Fx.beam(L(e), mz(e), mid(t), GAUSS, 0.6f, 0.25);
                        snd(e, SoundEvents.BEACON_POWER_SELECT, 1f, 1.6f); snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.3f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f), r = 1.6f * kick(t, 5 / 18f, .3f);
                    pistol2(p, k, 0, r); stance(p, k);
                }));

        // ---------- Gauss: charged piercing blast with recoil kick-back ----------
        out.add(AttackDef.of("gauss_charged").range(5, 36).cd(160).weight(1).dur(48).weapon("tau")
                .tick((e, t, n) -> {
                    if (n >= 4 && n < 32) {
                        Fx.dustBurst(L(e), GAUSS, 0.9f, mz(e), 2, 0.15);
                        if (n % 6 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.8f, 0.8f + n / 40f);
                    }
                    if (n == 32) {
                        Vec3 d = aim(e, t, 0.0); Vec3 m = mz(e);
                        Combat.hitscan(e, m, d, 50, 34f, 6, GAUSS);
                        Fx.beam(L(e), m, m.add(d.scale(40)), 0xffffff, 1.4f, 0.5);
                        Fx.ring(L(e), m, 1.2f, GAUSS, 1.8f, 18);
                        snd(e, SoundEvents.WARDEN_SONIC_BOOM, 0.9f, 1.8f);
                        e.setDeltaMovement(d.x * -0.9, 0.25, d.z * -0.9); e.hurtMarked = true; // blast recoil
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .92f), c = win(t, .08f, .65f) * (1 - win(t, .67f, .7f)), r = 3.6f * kick(t, 32 / 48f, .3f);
                    pistol2(p, k, 0, r); stance(p, k); p.rootY -= 0.8f * c;
                    p.rArmZ += 0.03f * (float) Math.sin(a * 6f) * c; p.bodyZ += 0.03f * (float) Math.sin(a * 6.3f) * c;
                    p.bodyX -= 0.5f * kick(t, 32 / 48f, .3f); p.rLegX += 0.4f * r * 0.3f;
                }));

        // ---------- Egon: continuous beam ----------
        out.add(AttackDef.of("egon").range(3, 14).cd(110).weight(2).dur(60).weapon("egon")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.BEACON_ACTIVATE, 1f, 1.6f);
                    if (n >= 6 && n < 54) {
                        Vec3 d = aim(e, t, 0.0), m = mz(e);
                        Vec3 end = t != null ? mid(t) : m.add(d.scale(10));
                        Fx.beam(L(e), m, end, n % 2 == 0 ? EGON : 0xffffff, 0.7f, 0.3);
                        if (n % 3 == 0) {
                            Combat.hitscan(e, m, d, 14, 4.5f, 2, EGON);
                            Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, end, 4, 0.3, 0.2);
                        }
                        if (n % 8 == 0) snd(e, SoundEvents.CONDUIT_AMBIENT_SHORT, 0.8f, 1.7f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .92f), f = win(t, .1f, .9f), jit = 0.03f * (float) Math.sin(a * 8f) * f;
                    shoulder(p, k, 0, 0.3f * f); stance(p, k); p.rootY -= 0.5f * k; p.rArmZ += jit; p.lArmZ -= jit; p.headX += 0.1f * f;
                }));

        // ---------- Hivehand: homing hornets ----------
        out.add(AttackDef.of("hivehand").range(3, 20).cd(60).weight(2).dur(30).weapon("hivehand")
                .tick((e, t, n) -> {
                    if (n >= 4 && n < 24 && n % 4 == 0) {
                        Combat.shoot(e, HORNET, mz(e), aim(e, t, 0.1), 1.0, 3.5f);
                        snd(e, SoundEvents.BEE_LOOP_AGGRESSIVE, 0.6f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .88f), tw = 0.15f * (float) Math.sin(a * 5f);
                    pistol2(p, k, 0.05f, 0.4f * bump(win(t, .2f, .8f))); stance(p, k); p.rHandY += tw * k;
                }));

        // ---------- Hand grenade ----------
        out.add(AttackDef.of("grenade").range(5, 18).cd(110).weight(2).dur(24).weapon("grenade")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.FLINTANDSTEEL_USE, 0.7f, 1.4f);
                    if (n == 12) {
                        Vec3 d = aim(e, t, 0.02).add(0, 0.22, 0).normalize();
                        Combat.shoot(e, FRAG, mz(e), d, 0.9, 11f);
                        snd(e, SoundEvents.SNOWBALL_THROW, 1f, 0.6f);
                    }
                })
                .anim((p, t, a) -> throwAnim(p, t)));

        // ---------- Satchel charge: toss, then remote detonate ----------
        out.add(AttackDef.of("satchel").range(5, 14).cd(160).weight(1).dur(36).weapon("satchel")
                .tick((e, t, n) -> {
                    if (n == 10) {
                        Vec3 d = aim(e, t, 0.02).add(0, 0.18, 0).normalize();
                        Combat.shoot(e, SATCHEL, mz(e), d, 0.75, 22f); snd(e, SoundEvents.SNOWBALL_THROW, 1f, 0.5f);
                    }
                    if (n == 26) { snd(e, SoundEvents.UI_BUTTON_CLICK.value(), 1f, 1.5f); snd(e, SoundEvents.NOTE_BLOCK_PLING.value(), 0.8f, 2f); }
                })
                .anim((p, t, a) -> {
                    throwAnim(p, Math.min(1f, t * 1.6f));
                    float det = win(t, .62f, .72f) * (1 - win(t, .85f, 1f));
                    p.lArmX = lerp(p.lArmX, -1.1f, det); p.lArmZ = lerp(p.lArmZ, -0.2f, det); p.lHandX = lerp(p.lHandX, -0.6f, det * bump(win(t, .65f, .8f)));
                }));

        // ---------- Trip mine ----------
        out.add(AttackDef.of("tripmine").range(3, 10).cd(200).weight(1).dur(40).weapon("tripmine")
                .tick((e, t, n) -> {
                    if (n == 14) {
                        Vec3 f = e.getViewVector(1f), pos = e.position().add(f.x * 1.6, 0.9, f.z * 1.6);
                        Fx.burst(L(e), ParticleTypes.CRIT, pos, 6, 0.1, 0.05); snd(e, SoundEvents.TRIPWIRE_ATTACH, 1f, 1.2f);
                    }
                    if (n >= 20 && n < 34) {
                        Vec3 f = e.getViewVector(1f); Vec3 a = e.position().add(f.x * 1.6, 0.9, f.z * 1.6);
                        Fx.beam(L(e), a, a.add(f.x * 3.5, 0, f.z * 3.5), 0xff2a1a, 0.15f, 0.1);
                    }
                    if (n == 34 && t != null && t.position().distanceTo(e.position()) < 7) {
                        Vec3 f = e.getViewVector(1f), pos = e.position().add(f.x * 3.2, 0.9, f.z * 3.2);
                        Fx.explosion(L(e), pos, 3.5f); Combat.blast(e, pos, 4f, 14, 1f, null);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 1.2f, 0.9f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponR = t < .4f ? "tripmine" : "glock"; // plant, then back away with the pistol out
                    crouchPlace(p, t);
                }));

        // ---------- Snarks ----------
        out.add(AttackDef.of("snark").range(4, 20).cd(90).weight(2).dur(28).weapon("snark")
                .tick((e, t, n) -> {
                    if (n == 10) {
                        for (int i = 0; i < 3; i++) {
                            Vec3 d = aim(e, t, 0.2).add(0, 0.2, 0).normalize();
                            Combat.shoot(e, SQUEAK, mz(e), d, 0.6 + 0.1 * i, 5f);
                        }
                        snd(e, SoundEvents.SILVERFISH_AMBIENT, 1.2f, 1.6f); snd(e, SoundEvents.SNOWBALL_THROW, 0.8f, 0.8f);
                    }
                })
                .anim((p, t, a) -> throwAnim(p, t)));

        // ---------- Long jump module ----------
        out.add(AttackDef.of("long_jump").range(4, 16).cd(120).weight(2).dur(30).weapon("crowbar")
                .tick((e, t, n) -> {
                    if (n == 6) { Combat.lunge(e, 1.35, 0.55); snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.9f, 1.4f); }
                    if (n > 6 && n < 18) Fx.burst(L(e), ParticleTypes.FLAME, e.position().add(0, 0.1, 0), 3, 0.15, 0.02);
                    if (n == 20) {
                        for (LivingEntity f : Combat.cone(e, 3.3, 70)) {
                            Combat.damage(e, f, 8); Combat.knock(f, e.position(), 0.8, 0.4); Fx.sparks(L(e), mid(f), 6);
                        }
                        slash(e, 60, -40, 20); snd(e, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.8f, 1.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float crouch = bump(win(t, 0, .25f)), fly = win(t, .2f, .35f) * (1 - win(t, .65f, .8f)), strike = bump(win(t, .6f, .8f));
                    p.rootY -= 3f * crouch; p.lLegX -= 1f * crouch; p.rLegX -= 1f * crouch; p.bodyX += 0.5f * crouch;
                    p.bodyX += 0.5f * fly; p.lLegX += 0.8f * fly; p.rLegX -= 0.6f * fly; p.lArmX = -0.5f * fly; p.rArmX = -1.1f * fly;
                    p.rArmX = lerp(p.rArmX, -2.4f, strike); p.rHandX = 0.5f; p.bodyY += 0.5f * strike;
                }));
    }

    private static void wideLegs(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.2f, k); p.rLegX = lerp(p.rLegX, 0.15f, k); p.lLegZ -= 0.28f * k; p.rLegZ += 0.28f * k; p.rootY -= 1.1f * k;
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat under = Mat.of(0x2b2b2d).noise(.1f), hev = Mat.of(HEV).bevel(.32f).noise(.08f), hevD = Mat.of(0x9a4f10).bevel(.25f).noise(.1f);
        Mat rubber = Mat.of(0x1c1c1e).noise(.06f), metal = Mat.of(0x7d7f80).noise(.06f), amber = Mat.of(0xffb030).glow();
        Mat skin = Mat.of(0xd8ab86).noise(.05f).bevel(.1f), hair = Mat.of(0x3b2616).noise(.2f), glass = Mat.of(0xcfe8ff).glow();
        // torso: orange HEV Mark IV vest with lambda patch, back pack
        rig.box(rig.torso, under, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, hev, -4, -12, -3, 8, 6, 1, .3f);
        rig.box(rig.torso, hevD, -3, -5.5f, -2.6f, 6, 2, 1);
        rig.box(rig.torso, amber, 2, -10, -3.7f, 1, 1, 1);
        rig.box(rig.torso, rubber, -4.5f, -2.5f, -2.5f, 9, 2, 5);
        rig.box(rig.torso, hev, -3.5f, -11.5f, 2, 7, 7, 2);
        rig.box(rig.torso, metal, -3.4f, -11, 4, 1, 6, 1);
        rig.box(rig.torso, metal, 2.4f, -11, 4, 1, 6, 1);
        rig.box(rig.torso, rubber, -3, -13, -3, 6, 1, 6);
        // head: bearded Gordon with glasses
        rig.box(rig.head, skin, -4, -8, -4, 8, 8, 8);
        rig.box(rig.head, hair, -4, -9, -4, 8, 1, 8, .2f);
        rig.box(rig.head, hair, -4, -8, 3.2f, 8, 5, 1);
        rig.box(rig.head, hair, -3.5f, -3, -4.6f, 7, 3, 1);
        rig.box(rig.head, glass, -3, -5.5f, -4.7f, 2, 1, 1);
        rig.box(rig.head, glass, 1, -5.5f, -4.7f, 2, 1, 1);
        rig.box(rig.head, rubber, -1, -5.5f, -4.6f, 2, 1, 1);
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, under, -2, -2, -2, 4, 12, 4);
            rig.box(arm, hev, s < 0 ? -3f : -2f, -2.5f, -2.5f, 5, 4, 5);
            rig.box(arm, hevD, -2, 4, -2, 4, 4, 4, .3f);
            rig.box(arm, rubber, -2, 8, -2, 4, 2, 4, .2f);
        }
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, under, -2, 0, -2, 4, 12, 4);
            rig.box(leg, hev, -2, 1, -2.6f, 4, 4, 1);
            rig.box(leg, rubber, -1.5f, 5, -2.8f, 3, 2, 1);
            rig.box(leg, hevD, -2, 7, -2.5f, 4, 3, 1);
            rig.box(leg, rubber, -2, 10, -3, 4, 2, 5, .2f);
        }
        weapons(rig, metal, rubber);
    }

    private static void weapons(Rig rig, Mat metal, Mat rubber) {
        Mat gun = Mat.of(0x37393b).noise(.08f), steel = Mat.of(0x9a9ca0).noise(.05f), black = Mat.of(0x161616), wood = Mat.of(0x5a3a22).noise(.15f);
        Mat red = Mat.of(0xa31d1d).noise(.1f), hot = Mat.of(0xff8a30).glow(), gauss = Mat.of(GAUSS).glow(), egon = Mat.of(EGON).glow(), hive = Mat.of(HIVE).glow();
        PartDefinition w = rig.weapon(rig.rHand, "crowbar");
        rig.box(w, red, -0.5f, -0.5f, -15, 1, 1, 17); rig.box(w, red, -0.5f, -2.5f, -16, 1, 3, 1);
        rig.box(w, metal, -0.5f, -3, -15, 1, 1, 1); rig.box(w, metal, -0.5f, 0, 2, 1, 1, 1);
        w = rig.weapon(rig.rHand, "glock");
        rig.box(w, gun, -1, -2.5f, -6, 2, 1, 6); rig.box(w, black, -1, -1.5f, -5, 2, 1, 5); rig.box(w, black, -1, -0.5f, -1.5f, 2, 3, 2);
        w = rig.weapon(rig.rHand, "magnum");
        rig.box(w, steel, -0.5f, -2.5f, -9, 1, 1, 6); rig.box(w, steel, -1.5f, -3, -3, 3, 3, 3); rig.box(w, wood, -1, -0.5f, -1, 2, 4, 2);
        w = rig.weapon(rig.rHand, "mp5");
        rig.box(w, gun, -1.5f, -3, -7, 3, 3, 8); rig.box(w, black, -0.5f, -2.5f, -10, 1, 1, 3); rig.box(w, black, -1, 0, -5, 2, 4, 1);
        rig.box(w, metal, -1, -0.5f, -9, 2, 2, 3); rig.box(w, black, -0.5f, -2, 1, 1, 1, 4);
        w = rig.weapon(rig.rHand, "shotgun");
        rig.box(w, wood, -1.5f, -3, -6, 3, 3, 8); rig.box(w, metal, -1, -3.5f, -16, 2, 1, 10); rig.box(w, gun, -1.5f, -2.5f, -13, 3, 2, 6);
        rig.box(w, black, -1, 0, -1, 2, 3, 2);
        w = rig.weapon(rig.rHand, "crossbow");
        rig.box(w, gun, -1, -2.5f, -9, 2, 2, 12); rig.box(w, metal, -5, -3, -9, 10, 1, 1); rig.box(w, black, -1, -5, -5, 2, 2, 4);
        rig.box(w, hot, -0.5f, -3.5f, -15, 1, 1, 10); rig.box(w, black, -1, -0.5f, -1, 2, 3, 2);
        w = rig.weapon(rig.rHand, "rpg");
        rig.box(w, Mat.of(0x3b4a3a).noise(.1f), -1.5f, -4.5f, -13, 3, 3, 19); rig.box(w, black, -2, -5, -14, 4, 4, 1);
        rig.box(w, black, -0.5f, -6.5f, -4, 1, 2, 2); rig.box(w, Mat.of(0xff2a1a).glow(), -0.5f, -5.5f, -13, 1, 1, 1); rig.box(w, black, -1, -1.5f, -2, 2, 4, 2);
        w = rig.weapon(rig.rHand, "tau");
        rig.box(w, steel, -2, -4, -8, 4, 4, 9); rig.box(w, gauss, -1, -3, -11, 2, 2, 3); rig.box(w, gauss, -2.6f, -3.5f, -6, 1, 3, 3);
        rig.box(w, gauss, 1.6f, -3.5f, -6, 1, 3, 3); rig.box(w, black, -1, 0, -2, 2, 3, 2);
        w = rig.weapon(rig.rHand, "egon");
        rig.box(w, gun, -2.5f, -4.5f, -8, 5, 5, 10); rig.box(w, steel, -1.5f, -3.5f, -16, 3, 3, 8); rig.box(w, egon, -1, -3, -18, 2, 2, 2);
        rig.box(w, egon, -3, -3, -5, 1, 2, 2); rig.box(w, black, -1, 0, -2, 2, 3, 2);
        w = rig.weapon(rig.rHand, "hivehand");
        rig.box(w, Mat.of(0x4a5a3a).noise(.2f).bevel(.4f), -2, -3.5f, -7, 4, 4, 8); rig.box(w, hive, -1, -3, -9, 2, 2, 2); rig.box(w, black, -1, 0, -2, 2, 3, 2);
        w = rig.weapon(rig.rHand, "grenade");
        rig.box(w, Mat.of(0x3e4a2c), -1, -1, -2, 2, 3, 2); rig.box(w, Mat.of(0xff2a1a).glow(), -0.5f, -2, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.rHand, "satchel");
        rig.box(w, Mat.of(0x6a5a3a).noise(.15f), -2, -2, -3, 4, 4, 5); rig.box(w, Mat.of(0xff2a1a).glow(), -0.5f, -3, -1, 1, 1, 1);
        w = rig.weapon(rig.rHand, "tripmine");
        rig.box(w, Mat.of(0x555a50), -2, -1.5f, -3, 4, 2, 3); rig.box(w, Mat.of(0xff2a1a).glow(), -0.5f, -2.2f, -3.4f, 1, 1, 1);
        w = rig.weapon(rig.rHand, "snark");
        rig.box(w, Mat.of(SNARK).noise(.25f).bevel(.4f), -1.5f, -1.5f, -3, 3, 3, 4); rig.box(w, black, -1, -2.2f, -3.4f, 1, 1, 1);
    }
}
