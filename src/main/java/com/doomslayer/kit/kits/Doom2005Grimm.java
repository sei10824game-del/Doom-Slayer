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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Sgt. John "Reaper" Grimm, RRTS marine of the 2005 DOOM film. Black tactical kit, red-lit visor helmet, cargo vest.
 * Arsenal from the film and its first-person sequence: combat knife, fists / rifle butt, sidearm, assault rifle,
 * shotgun, heavy machine gun, chainsaw (the Hell Knight kill), rocket launcher, BFG 9000 and his C24 "Hell-infected"
 * superhuman strength (lift-and-hurl throw, ground slam) plus passive C24 regeneration.
 */
public class Doom2005Grimm extends SlayerKit {
    private static final Random RNG = new Random();
    private static final int RED = 0xff2a1a, BLOOD = 0x7a0000, TRACER = 0xffd070, BFG_GREEN = 0x55ff66;

    static final ProjStyle RRTS_ROCKET = ProjStyle.make().color(0xffa040).size(0.45f).life(90).explode(3.8f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle BFG_ORB = ProjStyle.make().color(BFG_GREEN).size(1.8f).life(90).explode(7f).trail(ParticleTypes.HAPPY_VILLAGER)
            .onTick(p -> {
                // tendrils: zap every foe near the orb
                if (p.age2 % 4 != 0 || !(p.getOwner() instanceof SlayerEntity o) || !(p.level() instanceof ServerLevel l)) return;
                for (LivingEntity f : Combat.around(o, p.position(), 5)) {
                    Fx.beam(l, p.position(), f.position().add(0, f.getBbHeight() * 0.5, 0), BFG_GREEN, 0.9f, 0.35);
                    Combat.damage(o, f, 3f);
                }
            })
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.explosion(l, pos, 6f);
                for (int r = 1; r <= 6; r += 1) Fx.ring(l, pos, r, BFG_GREEN, 1.6f, 10 + r * 6);
                Fx.burst(l, ParticleTypes.HAPPY_VILLAGER, pos, 60, 2.5, 0.2);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 2.5f, 0.6f);
                Fx.sound(l, pos, SoundEvents.WARDEN_SONIC_BOOM, 1.5f, 1.2f);
            });

    public Doom2005Grimm() {
        super("doom2005_grimm", "John 'Reaper' Grimm", "ジョン・\"リーパー\"・グリム", "Doom (2005 film)");
        maxHealth = 80; speed = 0.31; armor = 12; knockbackRes = 0.45;
        preferredRange = 9f; eggPrimary = 0x1c1d20; eggSecondary = RED;
        idleWeaponR = "rifle";
    }

    // ------------------------------------------------------------------ server helpers
    private static ServerLevel sl(SlayerEntity e) { return (ServerLevel) e.level(); }
    private static Vec3 gun(SlayerEntity e) { return Combat.muzzle(e, 1.0, 0.32, 0.05); }

    private static void flash(SlayerEntity e, Vec3 at, float size) {
        ServerLevel l = sl(e);
        Fx.dustBurst(l, 0xfff0a0, 1.2f * size, at, 3, 0.04 * size);
        Fx.burst(l, ParticleTypes.FLAME, at, 1, 0.02, 0.01);
        Fx.burst(l, ParticleTypes.SMOKE, at, 2, 0.05, 0.01);
        Fx.dustBurst(l, 0xc8a040, 0.5f, Combat.muzzle(e, 0.4, 0.6, 0.1), 1, 0.02); // ejected casing
    }
    private static void shot(SlayerEntity e, LivingEntity tg, double spread, float dmg, int pierce, double range) {
        if (tg == null) return;
        Vec3 from = gun(e);
        flash(e, from, 1f);
        Combat.hitscan(e, from, Combat.dirTo(from, tg, spread, RNG), range, dmg, pierce, TRACER);
    }
    private static void blood(ServerLevel l, LivingEntity f, int n) {
        Fx.dustBurst(l, BLOOD, 1.1f, f.position().add(0, f.getBbHeight() * 0.6, 0), n, 0.2);
    }
    /** knife arc + forward cone hit */
    private static void slash(SlayerEntity e, float from, float to, float tilt, float dmg, double kb) {
        ServerLevel l = sl(e);
        Fx.arc(l, e.position().add(0, 1.15, 0), e.getYRot(), 1.6f, from, to, tilt, 0xe8e8f0, 0.7f, 14);
        for (LivingEntity f : Combat.cone(e, 3.2, 65)) if (Combat.damage(e, f, dmg)) { Combat.knock(f, e.position(), kb, 0.1); blood(l, f, 8); }
        Combat.sound(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.35f);
    }
    /** short narrow strike (fist, stock, stab) */
    private static void strike(SlayerEntity e, double reach, float dmg, double kb, double up, SoundEvent s, float pitch) {
        ServerLevel l = sl(e);
        for (LivingEntity f : Combat.cone(e, reach, 40)) if (Combat.damage(e, f, dmg)) {
            Combat.knock(f, e.position(), kb, up); Fx.sparks(l, f.position().add(0, f.getBbHeight() * 0.6, 0), 5); blood(l, f, 4);
        }
        Combat.sound(e, s, 1f, pitch);
    }
    private static void hellAura(SlayerEntity e, int n) {
        Fx.dustBurst(sl(e), 0xaa0000, 1.3f, e.position().add(0, 1.0, 0), n, 0.5);
    }

    // ------------------------------------------------------------------ anim helpers
    /** bladed tactical stance: lead (left) leg forward, rear leg back, hips lowered, right shoulder back */
    private static void stance(Pose p, float c) {
        p.lLegX -= 0.35f * c; p.rLegX += 0.3f * c; p.rLegZ += 0.08f * c; p.lLegZ -= 0.08f * c;
        p.rootY -= 0.5f * c; p.bodyX += 0.1f * c; p.bodyY -= 0.22f * c;
    }
    /** rotate the right weapon mount so the barrel stays level whatever the arm/torso pitch */
    private static void level(Pose p, float extra) { p.rHandX = -HALF - p.rArmX - p.bodyX + extra; }
    /** aim down sights with a long gun: cheek weld, head canted to the stock */
    private static void shoulder(Pose p, float recoil) {
        p.bodyY = -0.3f; p.aim2(0, recoil); p.headY += 0.28f; p.headZ += 0.08f; p.headX += 0.06f;
        p.rArmY = -0.05f; p.lArmY = 0.55f;
        level(p, -recoil * 0.35f);
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float limbAmt) {
        // low-ready carry: muzzle down, support hand on the handguard, head sweeping sectors
        float still = 1 - limbAmt;
        p.rArmX = lerp(p.rArmX * 0.25f, -0.8f, 0.85f); p.lArmX = lerp(p.lArmX * 0.25f, -1.0f, 0.85f);
        p.rArmY = -0.1f; p.lArmY = 0.6f; p.rArmZ = 0.02f; p.lArmZ = -0.05f;
        level(p, 0.45f);
        p.bodyY = -0.12f * still;
        p.headY += (float) Math.sin(age * 0.035f) * 0.35f * still;
        p.bodyX += 0.04f * limbAmt;
    }

    @Override
    public void passive(SlayerEntity e) {
        // C24 chromosome: superhuman regeneration, faster when near death
        float frac = e.getHealth() / e.getMaxHealth();
        if (frac < 1 && e.tickCount % (frac < 0.35f ? 10 : 40) == 0) {
            e.heal(1f);
            if (frac < 0.35f) hellAura(e, 6);
        }
    }

    // ------------------------------------------------------------------ attacks
    @Override
    protected void defineAttacks(List<AttackDef> out) {

        // 1. Combat knife: forehand slash, backhand slash, lunging thrust
        out.add(AttackDef.of("knife_combo").range(0, 3).cd(40).dur(26).weapon("knife").weight(2)
                .tick((e, tg, n) -> {
                    if (n == 5) slash(e, -70, 70, -15, 6, 0.25);
                    if (n == 12) slash(e, 70, -70, 20, 6, 0.25);
                    if (n == 17) Combat.lunge(e, 0.45, 0.05);
                    if (n == 19) strike(e, 3.4, 9, 0.6, 0.1, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f);
                })
                .anim((p, k, age) -> {
                    stance(p, 0.8f);
                    p.rArmX = track(k, 0, -0.6f, .1f, -1.9f, .19f, -1.2f, .3f, -1.5f, .4f, -2.0f, .46f, -1.3f, .6f, -0.9f, .68f, -1.3f, .73f, -1.75f, .85f, -1.55f, 1, -0.6f);
                    p.rArmY = track(k, 0, 0, .1f, -0.9f, .19f, 0.8f, .3f, 0.9f, .46f, -0.8f, .6f, -0.3f, .73f, 0, 1, 0);
                    p.rArmZ = track(k, 0, 0.1f, .1f, 0.6f, .19f, -0.2f, .4f, -0.3f, .46f, 0.5f, .73f, 0.05f, 1, 0.1f);
                    p.rHandX = track(k, 0, 0, .19f, 0.3f, .46f, 0.3f, .62f, 0.5f, .73f, -0.15f, 1, 0);
                    p.rHandZ = track(k, 0, 0, .19f, 0.6f, .46f, -0.6f, .73f, 0, 1, 0);
                    p.lArmX = -1.15f + bump(win(k, .62f, .8f)) * 0.5f; p.lArmY = 0.65f; p.lArmZ = -0.2f; // guard hand by the chin
                    p.bodyY = track(k, 0, -0.2f, .1f, -0.55f, .19f, 0.5f, .3f, 0.45f, .46f, -0.45f, .6f, -0.2f, .73f, 0.2f, 1, -0.2f);
                    p.bodyX += 0.08f + kick(k, .7f, .25f) * 0.3f;
                    p.headY -= p.bodyY * 0.5f; p.headX += 0.1f;
                    p.lLegX -= track(k, 0, 0, .62f, 0.1f, .73f, 0.6f, 1, 0);
                    p.rLegX += track(k, 0, 0, .62f, 0, .73f, 0.4f, 1, 0);
                    p.rootZ = -track(k, 0, 0, .19f, 0.8f, .46f, 1.6f, .73f, 4f, 1, 0);
                }));

        // 2. Knife execution on a weakened target: grab the collar, two overhand stabs, twist out
        out.add(AttackDef.of("knife_execution").range(0, 2.8f).cd(120).dur(30).weapon("knife").lowHealthTarget(0.3f)
                .tick((e, tg, n) -> {
                    if (tg != null && n < 26) { tg.setDeltaMovement(0, tg.getDeltaMovement().y, 0); tg.hurtMarked = true; }
                    if (n == 3) Combat.sound(e, SoundEvents.ARMOR_EQUIP_LEATHER, 1f, 0.8f);
                    if (n == 10 || n == 17) strike(e, 3, 10, 0.05, 0, SoundEvents.PLAYER_ATTACK_CRIT, 0.8f);
                    if (n == 24 && tg != null) {
                        if (Combat.damage(e, tg, 14)) blood(sl(e), tg, 20);
                        Combat.knock(tg, e.position(), 0.9, 0.2);
                        Combat.sound(e, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.7f);
                    }
                })
                .anim((p, k, age) -> {
                    stance(p, 1f);
                    // left hand shoots out and grabs, pulls the victim in
                    p.lArmX = track(k, 0, -0.8f, .1f, -1.75f, .7f, -1.6f, .8f, -1.2f, 1, -0.9f);
                    p.lArmY = track(k, 0, 0.4f, .1f, 0.1f, .3f, 0.25f, 1, 0.5f);
                    // right hand: two overhand ice-pick stabs, then a twisting rip
                    p.rArmX = track(k, 0, -0.6f, .2f, -2.9f, .33f, -1.3f, .45f, -2.8f, .57f, -1.3f, .7f, -1.5f, .8f, -1.0f, 1, -0.6f);
                    p.rArmY = track(k, 0, 0, .2f, -0.3f, .33f, 0.15f, .45f, -0.3f, .57f, 0.15f, .8f, 0.6f, 1, 0);
                    p.rArmZ = 0.15f;
                    p.rHandX = track(k, 0, 0, .2f, 1.6f, .33f, 1.3f, .45f, 1.6f, .57f, 1.3f, .8f, 0.3f, 1, 0);
                    p.rHandZ = track(k, 0, 0, .7f, 0, .8f, 1.2f, 1, 0);
                    p.bodyX += track(k, 0, 0, .2f, -0.15f, .33f, 0.35f, .45f, -0.1f, .57f, 0.35f, .8f, 0.1f, 1, 0);
                    p.bodyY = track(k, 0, -0.2f, .1f, 0.25f, .8f, -0.45f, 1, -0.2f);
                    p.headX += 0.25f;
                    p.rootY -= bump(win(k, .25f, .65f)) * 0.8f;
                    p.rootZ = -1.5f * bump(win(k, 0, .9f));
                }));

        // 3. Hand-to-hand: jab, cross, hook (the brawl with Sarge)
        out.add(AttackDef.of("cqb_punches").range(0, 2.6f).cd(35).dur(20).weight(2)
                .tick((e, tg, n) -> {
                    if (n == 4) strike(e, 2.8, 4, 0.2, 0.05, SoundEvents.PLAYER_ATTACK_WEAK, 1.2f);
                    if (n == 9) strike(e, 2.8, 6, 0.4, 0.05, SoundEvents.PLAYER_ATTACK_STRONG, 1.1f);
                    if (n == 15) strike(e, 2.8, 8, 0.9, 0.25, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.9f);
                })
                .anim((p, k, age) -> {
                    p.weaponR = null; // rifle slung for the brawl
                    stance(p, 1f);
                    p.lArmX = track(k, 0, -1.3f, .12f, -0.9f, .2f, -1.65f, .3f, -1.2f, .62f, -1.3f, .75f, -1.5f, .9f, -1.2f, 1, -1.2f);
                    p.lArmY = track(k, 0, 0.5f, .2f, 0.1f, .3f, 0.5f, .62f, -0.3f, .75f, 0.9f, .9f, 0.6f, 1, 0.5f);
                    p.lArmZ = track(k, 0, -0.2f, .62f, -0.9f, .75f, -0.6f, 1, -0.2f);
                    p.rArmX = track(k, 0, -1.3f, .3f, -1.0f, .45f, -1.7f, .58f, -1.2f, 1, -1.3f);
                    p.rArmY = track(k, 0, -0.5f, .3f, -0.5f, .45f, 0.15f, .58f, -0.4f, 1, -0.5f);
                    p.rArmZ = 0.15f;
                    p.bodyY = track(k, 0, -0.2f, .2f, -0.45f, .3f, -0.3f, .45f, 0.5f, .6f, 0.3f, .75f, -0.6f, .9f, -0.3f, 1, -0.2f);
                    p.bodyX += 0.15f + kick(k, .2f, .15f) * 0.1f + kick(k, .45f, .15f) * 0.15f + kick(k, .75f, .2f) * 0.2f;
                    p.headY -= p.bodyY * 0.6f; p.headX += 0.2f;
                    p.lLegX -= track(k, 0, 0, .2f, 0.25f, .3f, 0.1f, .75f, 0.35f, 1, 0);
                    p.rootZ = -track(k, 0, 0, .2f, 0.8f, .45f, 1.5f, .75f, 2.5f, 1, 0);
                    p.rootX = Math.sin(age * 0.5f) > 0 ? 0.15f : -0.15f; // bouncing on the balls of the feet
                    p.rootY += Math.abs((float) Math.sin(age * 0.5f)) * 0.3f;
                }));

        // 4. Rifle butt stroke
        out.add(AttackDef.of("rifle_butt").range(0, 2.8f).cd(45).dur(18).weapon("rifle")
                .tick((e, tg, n) -> {
                    if (n == 8) strike(e, 3, 8, 1.3, 0.35, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 1.3f);
                })
                .anim((p, k, age) -> {
                    stance(p, 1f);
                    // chamber: rifle pulled back across the chest, then the stock is driven forward
                    p.rArmX = track(k, 0, -0.8f, .3f, -0.5f, .44f, -1.7f, .6f, -1.5f, 1, -0.8f);
                    p.rArmY = track(k, 0, -0.1f, .3f, -0.5f, .44f, 0.2f, 1, -0.1f);
                    p.lArmX = track(k, 0, -1.0f, .3f, -1.6f, .44f, -1.0f, 1, -1.0f);
                    p.lArmY = track(k, 0, 0.6f, .3f, 0.3f, .44f, 0.7f, 1, 0.6f);
                    p.rHandX = track(k, 0, 0.6f, .3f, 1.7f, .44f, 2.6f, .6f, 2.5f, 1, 0.6f); // stock swings to the front
                    p.bodyY = track(k, 0, -0.2f, .3f, -0.7f, .44f, 0.45f, .65f, 0.3f, 1, -0.2f);
                    p.bodyX += track(k, 0, 0, .3f, -0.15f, .44f, 0.4f, .7f, 0.2f, 1, 0);
                    p.headY -= p.bodyY * 0.5f;
                    p.lLegX -= track(k, 0, 0, .3f, -0.1f, .44f, 0.55f, .8f, 0.3f, 1, 0);
                    p.rLegX += track(k, 0, 0, .44f, 0.45f, 1, 0);
                    p.rootZ = -track(k, 0, 0, .3f, -0.6f, .44f, 2.4f, .8f, 1.5f, 1, 0);
                }));

        // 5. Sidearm: draw from the thigh holster, three aimed shots, two-hand isosceles
        out.add(AttackDef.of("pistol").range(3, 18).cd(35).dur(24).weapon("pistol")
                .tick((e, tg, n) -> {
                    if (n == 6 || n == 12 || n == 18) { shot(e, tg, 0.025, 5, 0, 32); Combat.gunSound(e, 1.8f); }
                    if (n == 1) Combat.sound(e, SoundEvents.ARMOR_EQUIP_IRON, 0.8f, 1.4f);
                })
                .anim((p, k, age) -> {
                    float rec = kick(k, .25f, .16f) + kick(k, .5f, .16f) + kick(k, .75f, .16f), draw = smooth(win(k, 0, .22f));
                    stance(p, 0.6f);
                    p.bodyY = lerp(-0.2f, 0, draw);
                    p.aim2(0, rec);
                    p.rArmX = lerp(0.1f, p.rArmX, draw); p.lArmX = lerp(-0.4f, p.lArmX, draw);
                    p.rArmY = lerp(0, -0.25f, draw); p.lArmY = lerp(0.3f, 0.6f, draw);
                    level(p, -rec * 0.9f); // muzzle flip
                    p.headX += 0.08f * draw; p.headY += 0.05f;
                    p.bodyX += 0.05f * draw;
                    p.rootZ = rec * 0.4f;
                }));

        // 6. Assault rifle: three controlled 3-round bursts from the shoulder, flashlight on
        out.add(AttackDef.of("assault_rifle").range(4, 26).cd(45).dur(32).weapon("rifle").weight(3)
                .tick((e, tg, n) -> {
                    if (n >= 6 && n < 30 && (n - 6) % 8 < 3) {
                        shot(e, tg, 0.035, 3.5f, 0, 40); Combat.gunSound(e, 1.3f + RNG.nextFloat() * 0.1f);
                    }
                    if (n == 0) Combat.sound(e, SoundEvents.ARMOR_EQUIP_CHAIN, 0.8f, 1.2f);
                })
                .anim((p, k, age) -> {
                    float rec = 0;
                    for (int i = 0; i < 9; i++) { float t0 = (6 + (i / 3) * 8 + i % 3) / 32f; rec += kick(k, t0, 0.07f) * 0.7f; }
                    float up = smooth(win(k, 0, .15f)), down = smooth(win(k, .9f, 1));
                    stance(p, 0.8f);
                    shoulder(p, rec);
                    p.rArmX = lerp(-0.8f, p.rArmX, up - down); p.lArmX = lerp(-1.0f, p.lArmX, up - down);
                    level(p, -rec * 0.3f + (1 - up + down) * 0.45f);
                    p.bodyX += 0.06f; p.rootZ = rec * 0.25f;
                    p.bodyY += (float) Math.sin(k * 3 * PI) * 0.05f; // sweeping between bursts
                }));

        // 7. Pump shotgun
        out.add(AttackDef.of("shotgun").range(2, 10).cd(40).dur(28).weapon("shotgun").weight(2)
                .tick((e, tg, n) -> {
                    if (n == 9 && tg != null) {
                        Vec3 from = gun(e); flash(e, from, 1.8f);
                        for (int i = 0; i < 9; i++) Combat.hitscan(e, from, Combat.dirTo(from, tg, 0.09, RNG), 16, 2.4f, 0, TRACER);
                        Combat.gunSound(e, 0.75f); Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.8f);
                    }
                    if (n == 16) Combat.sound(e, SoundEvents.PISTON_CONTRACT, 0.8f, 1.6f);
                    if (n == 19) Combat.sound(e, SoundEvents.CROSSBOW_LOADING_END, 1f, 0.9f);
                })
                .anim((p, k, age) -> {
                    float rec = kick(k, .32f, .3f) * 1.6f, pump = bump(win(k, .55f, .75f));
                    stance(p, 1f);
                    shoulder(p, rec);
                    p.lArmX += pump * 0.45f; p.lArmY -= pump * 0.1f; // rack the slide
                    p.rArmX += pump * 0.15f; p.rHandZ = -pump * 0.25f;
                    p.bodyX += rec * 0.25f - pump * 0.05f;
                    p.headX -= rec * 0.15f;
                    p.rootZ = rec * 1.2f;
                    p.rLegX += rec * 0.25f;
                    float aimIn = smooth(win(k, 0, .2f));
                    p.rArmX = lerp(-0.8f, p.rArmX, aimIn);
                }));

        // 8. Heavy machine gun (the chaingun): spin-up, sustained hip fire, spin-down
        out.add(AttackDef.of("heavy_mg").range(4, 24).cd(110).dur(46).weapon("chaingun")
                .when((e, tg) -> tg.getMaxHealth() >= 20 || Combat.around(e, tg.position(), 5).size() >= 2)
                .tick((e, tg, n) -> {
                    if (n < 10 && n % 3 == 0) Combat.sound(e, SoundEvents.GRINDSTONE_USE, 0.7f, 0.8f + n * 0.08f);
                    if (n >= 10 && n < 38) {
                        shot(e, tg, 0.06, 2.4f, 0, 36);
                        if (n % 2 == 0) Combat.gunSound(e, 1.1f + RNG.nextFloat() * 0.2f);
                        if (n % 4 == 0) Fx.burst(sl(e), ParticleTypes.SMOKE, gun(e), 3, 0.1, 0.02);
                    }
                    if (n == 38) Combat.sound(e, SoundEvents.GRINDSTONE_USE, 0.6f, 0.6f);
                })
                .anim((p, k, age) -> {
                    float fire = win(k, 10 / 46f, 11 / 46f) - win(k, 38 / 46f, 40 / 46f);
                    float shake = (float) Math.sin(age * 3.1f) * fire;
                    p.lLegX -= 0.45f; p.rLegX += 0.4f; p.rLegZ += 0.18f; p.lLegZ -= 0.18f; p.rootY -= 0.7f;
                    p.bodyX = -0.08f + fire * 0.05f + shake * 0.03f;   // lean back into the weight
                    p.bodyY = -0.15f + shake * 0.03f;
                    p.rArmX = -0.9f + shake * 0.06f; p.rArmY = -0.15f; p.rArmZ = 0.05f;
                    p.lArmX = -1.15f + shake * 0.05f; p.lArmY = 0.75f; p.lArmZ = -0.1f;
                    level(p, track(k, 0, 0.4f, .2f, 0, .85f, 0, 1, 0.4f) + shake * 0.05f);
                    p.rHandZ = (float) Math.sin(age * 0.9f) * 0.05f * (1 - fire);
                    p.rootX = shake * 0.25f; p.rootZ = fire * 0.6f + Math.abs(shake) * 0.3f;
                    p.headX += 0.05f; p.headY += shake * 0.04f;
                }));

        // 9. Chainsaw: rev, plunge, grind (the Hell Knight kill), rip upward
        out.add(AttackDef.of("chainsaw").range(0, 3.2f).cd(90).dur(40).weapon("chainsaw").weight(2)
                .tick((e, tg, n) -> {
                    ServerLevel l = sl(e);
                    if (n % 4 == 0) Combat.sound(e, SoundEvents.GRINDSTONE_USE, 0.9f, n < 8 ? 1.7f : 1.1f);
                    if (n % 3 == 0) Fx.burst(l, ParticleTypes.SMOKE, Combat.muzzle(e, 0.3, 0.5, 0.1), 2, 0.05, 0.01);
                    if (n == 8) Combat.lunge(e, 0.4, 0.05);
                    if (n >= 12 && n < 32) {
                        if (tg != null && e.distanceTo(tg) < 3.5f) { tg.setDeltaMovement(0, 0, 0); tg.hurtMarked = true; }
                        if (n % 2 == 0) for (LivingEntity f : Combat.cone(e, 3.2, 35)) if (Combat.damage(e, f, 2.5f)) {
                            Fx.sparks(l, f.position().add(0, f.getBbHeight() * 0.5, 0), 3); blood(l, f, 6);
                        }
                        if (n % 5 == 0) Combat.sound(e, SoundEvents.SHEEP_SHEAR, 1f, 0.6f);
                    }
                    if (n == 33) {
                        for (LivingEntity f : Combat.cone(e, 3.4, 40)) if (Combat.damage(e, f, 10)) { Combat.knock(f, e.position(), 0.5, 0.6); blood(l, f, 25); }
                        Combat.sound(e, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.6f);
                    }
                })
                .anim((p, k, age) -> {
                    float grind = win(k, .3f, .32f) - win(k, .8f, .82f), rev = 1 - win(k, .18f, .22f);
                    float shake = (float) Math.sin(age * 4.3f) * (0.4f * rev + grind);
                    stance(p, 1f);
                    p.rArmX = track(k, 0, -0.9f, .2f, -1.0f, .3f, -1.5f, .8f, -1.45f, .87f, -2.5f, 1, -0.9f) + shake * 0.06f;
                    p.lArmX = track(k, 0, -1.1f, .2f, -1.2f, .3f, -1.6f, .8f, -1.55f, .87f, -2.4f, 1, -1.1f) + shake * 0.06f;
                    p.rArmY = -0.1f; p.lArmY = 0.6f; p.lArmZ = -0.1f;
                    p.bodyX += track(k, 0, 0, .2f, -0.05f, .3f, 0.35f, .8f, 0.3f, .87f, -0.2f, 1, 0) + shake * 0.03f;
                    p.bodyY = track(k, 0, -0.35f, .3f, -0.1f, .8f, -0.15f, .87f, 0.15f, 1, -0.2f);
                    level(p, track(k, 0, 0.2f, .2f, 0, .3f, 0.35f, .8f, 0.35f, .87f, -0.6f, 1, 0));
                    p.lLegX -= track(k, 0, 0, .2f, 0, .3f, 0.55f, .8f, 0.5f, 1, 0);
                    p.rLegX += track(k, 0, 0, .3f, 0.35f, .8f, 0.35f, 1, 0);
                    p.rootZ = -track(k, 0, 0, .2f, 0, .3f, 2.5f, .8f, 2.5f, 1, 0);
                    p.rootX = shake * 0.12f;
                    p.headX += 0.15f + grind * 0.1f; p.headY += shake * 0.05f;
                }));

        // 10. Rocket launcher: drop to a kneel, shoulder, fire, backblast
        out.add(AttackDef.of("rocket_launcher").range(7, 32).cd(80).dur(26).weapon("rocket")
                .tick((e, tg, n) -> {
                    if (n == 4) Combat.sound(e, SoundEvents.CROSSBOW_LOADING_MIDDLE, 1f, 0.7f);
                    if (n == 12 && tg != null) {
                        Vec3 from = Combat.muzzle(e, 1.3, 0.32, 0.15);
                        Combat.shoot(e, RRTS_ROCKET, from, Combat.dirTo(from, tg, 0.01, RNG), 1.15, 14);
                        flash(e, from, 2f);
                        Vec3 back = Combat.muzzle(e, -1.2, 0.32, 0.15); // backblast
                        Fx.burst(sl(e), ParticleTypes.LARGE_SMOKE, back, 10, 0.3, 0.05);
                        Fx.burst(sl(e), ParticleTypes.FLAME, back, 6, 0.2, 0.05);
                        Combat.sound(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5f, 0.6f); Combat.gunSound(e, 0.6f);
                    }
                })
                .anim((p, k, age) -> {
                    float kneel = smooth(win(k, 0, .28f)) - smooth(win(k, .8f, 1)), rec = kick(k, .46f, .35f) * 1.5f;
                    p.rLegX += 1.3f * kneel; p.lLegX -= 0.7f * kneel; p.rootY -= 3.2f * kneel; // rear knee down
                    p.bodyY = -0.25f * kneel;
                    p.aim2(-0.05f, rec);
                    p.rArmX = lerp(-0.8f, p.rArmX, kneel); p.lArmX = lerp(-1.0f, p.lArmX, kneel);
                    p.rArmY = -0.25f; p.lArmY = 0.55f;
                    level(p, -rec * 0.4f + (1 - kneel) * 0.3f);
                    p.headY += 0.3f * kneel; p.headZ += 0.12f * kneel;
                    p.bodyX += rec * 0.3f; p.rootZ = rec * 1.5f;
                }));

        // 11. BFG 9000: charge, plant, fire the orb with tendrils, huge recoil
        out.add(AttackDef.of("bfg").range(6, 30).cd(500).dur(48).weapon("bfg")
                .when((e, tg) -> tg.getMaxHealth() >= 40 || Combat.around(e, tg.position(), 6).size() >= 3)
                .tick((e, tg, n) -> {
                    ServerLevel l = sl(e); Vec3 m = Combat.muzzle(e, 1.3, 0.32, 0.1);
                    if (n == 0) Combat.sound(e, SoundEvents.BEACON_POWER_SELECT, 1.5f, 0.6f);
                    if (n < 32) {
                        Fx.ring(l, m, 1.4f - n * 0.04f, BFG_GREEN, 0.8f, 8);
                        Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, m, 2, 0.2, 0.05);
                        if (n % 8 == 0) Combat.sound(e, SoundEvents.BEACON_ACTIVATE, 1f, 0.6f + n * 0.03f);
                    }
                    if (n == 32 && tg != null) {
                        Combat.shoot(e, BFG_ORB, m, Combat.dirTo(m, tg, 0, RNG), 0.7, 30);
                        Fx.burst(l, ParticleTypes.HAPPY_VILLAGER, m, 30, 0.5, 0.2);
                        Combat.sound(e, SoundEvents.WARDEN_SONIC_BOOM, 2f, 0.8f);
                        Combat.lunge(e, -0.6, 0.1); // recoil shoves Grimm backwards
                    }
                })
                .anim((p, k, age) -> {
                    float charge = win(k, 0, 32 / 48f), rec = kick(k, 32 / 48f, .35f) * 2f;
                    float shake = (float) Math.sin(age * 2.7f) * charge * charge * (1 - win(k, .66f, .7f));
                    p.lLegX -= 0.55f; p.rLegX += 0.5f; p.rLegZ += 0.15f; p.lLegZ -= 0.15f;
                    p.rootY -= 0.4f + charge * 0.8f * (1 - win(k, .7f, 1));
                    p.bodyX = -0.05f + charge * 0.12f + rec * 0.35f + shake * 0.03f;
                    p.bodyY = -0.2f + shake * 0.02f;
                    p.rArmX = -1.15f - rec * 0.6f + shake * 0.05f; p.lArmX = -1.35f - rec * 0.5f + shake * 0.05f;
                    p.rArmY = -0.1f; p.lArmY = 0.7f; p.lArmZ = -0.15f;
                    level(p, -rec * 0.5f);
                    p.headX += 0.15f * charge - rec * 0.2f;
                    p.rootZ = rec * 2.5f; p.rootX = shake * 0.2f;
                }));

        // 12. C24 strength: seize the enemy, hoist it overhead, hurl it ("Go to hell")
        out.add(AttackDef.of("hell_throw").range(0, 2.6f).cd(160).dur(34)
                .when((e, tg) -> tg.getBbHeight() <= 2.6f && tg.getBbWidth() <= 1.6f)
                .onStart((e, tg, n) -> { if (tg != null) tg.getPersistentData().putBoolean("grimmThrown", false); })
                .tick((e, tg, n) -> {
                    if (tg == null || !tg.isAlive()) return;
                    ServerLevel l = sl(e); Vec3 look = e.getViewVector(1f);
                    if (n == 6) { Combat.sound(e, SoundEvents.IRON_GOLEM_ATTACK, 1f, 1.3f); hellAura(e, 12); }
                    if (n >= 6 && n < 20) {
                        float lift = smooth((n - 6) / 10f);
                        tg.setPos(e.getX() + look.x * (1.0 - 0.7 * lift), e.getY() + 0.4 + lift * 2.0, e.getZ() + look.z * (1.0 - 0.7 * lift));
                        tg.setDeltaMovement(Vec3.ZERO); tg.fallDistance = 0; tg.hurtMarked = true;
                    }
                    if (n == 20) {
                        Combat.damage(e, tg, 6);
                        tg.setDeltaMovement(look.x * 1.9, 0.45, look.z * 1.9); tg.hurtMarked = true;
                        Combat.sound(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f); hellAura(e, 16);
                    }
                    if (n > 22 && (tg.horizontalCollision || tg.onGround() || n == 33) && !tg.getPersistentData().getBoolean("grimmThrown")) {
                        tg.getPersistentData().putBoolean("grimmThrown", true);
                        Combat.blast(e, tg.position().add(0, 0.5, 0), 2.6f, 10, 0.6f, null);
                        Fx.explosion(l, tg.position(), 1.5f);
                        Fx.sound(l, tg.position(), SoundEvents.ANVIL_LAND, 0.8f, 0.6f);
                        Fx.sound(l, tg.position(), SoundEvents.IRON_GOLEM_DAMAGE, 1f, 0.8f);
                    }
                })
                .anim((p, k, age) -> {
                    p.weaponR = null;
                    float grab = smooth(win(k, 0, .18f)), lift = smooth(win(k, .18f, .5f)), hurl = smooth(win(k, .52f, .62f)), rec = smooth(win(k, .7f, 1));
                    float arms = -1.5f * grab - 1.4f * lift + 2.1f * hurl + 0.8f * rec;
                    p.rArmX = arms; p.lArmX = arms;
                    p.rArmY = 0.2f * (1 - hurl); p.lArmY = -0.2f * (1 - hurl);
                    p.rArmZ = -0.1f * lift; p.lArmZ = 0.1f * lift;
                    p.bodyX += 0.25f * grab - 0.45f * lift + 0.95f * hurl - 0.75f * rec * hurl;
                    p.bodyY = 0;
                    p.headX += -0.4f * lift + 0.4f * hurl;
                    p.rootY -= 1.2f * grab * (1 - lift) + 0.8f * hurl * (1 - rec);
                    p.lLegX -= 0.4f * grab + 0.4f * hurl * (1 - rec); p.rLegX += 0.3f * grab + 0.5f * hurl * (1 - rec);
                    p.rLegZ += 0.15f * lift; p.lLegZ -= 0.15f * lift; // brace under the weight
                    p.rootZ = -1.5f * grab - 2f * hurl * (1 - rec);
                }));

        // 13. C24 strength: leap and double-fist ground slam
        out.add(AttackDef.of("hell_slam").range(0, 4.5f).cd(140).dur(30)
                .when((e, tg) -> tg.getMaxHealth() >= 30 || Combat.around(e, e.position(), 4.5).size() >= 2)
                .tick((e, tg, n) -> {
                    ServerLevel l = sl(e); e.fallDistance = 0;
                    if (n == 6) { Combat.lunge(e, 0.3, 0.75); Combat.sound(e, SoundEvents.RAVAGER_ROAR, 0.6f, 1.5f); hellAura(e, 15); }
                    if (n == 14) e.setDeltaMovement(e.getDeltaMovement().x * 0.3, -1.4, e.getDeltaMovement().z * 0.3);
                    if (n == 17) {
                        Vec3 c = e.position().add(0, 0.3, 0);
                        Combat.blast(e, c, 4.5f, 14, 1.4f, null);
                        for (int r = 1; r <= 4; r++) Fx.ring(l, c, r, 0xaa1010, 1.5f, 12 * r);
                        Fx.burst(l, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), c, 40, 1.2, 0.15);
                        Fx.burst(l, ParticleTypes.EXPLOSION, c, 3, 1, 0);
                        Combat.sound(e, SoundEvents.GENERIC_EXPLODE, 1.2f, 0.7f); Combat.sound(e, SoundEvents.ANVIL_LAND, 1f, 0.5f);
                    }
                })
                .anim((p, k, age) -> {
                    p.weaponR = null;
                    float crouch = bump(win(k, 0, .25f)), air = win(k, .2f, .27f) - win(k, .5f, .55f), slam = smooth(win(k, .5f, .57f)), rec = smooth(win(k, .7f, 1));
                    float armUp = -2.9f * (air + crouch * 0.1f);
                    p.rArmX = lerp(armUp + 0.6f * crouch, -0.5f, slam) * (1 - rec) + (-0.3f) * rec;
                    p.lArmX = p.rArmX; p.rArmZ = 0.25f * air - 0.1f * slam; p.lArmZ = -p.rArmZ;
                    p.rArmY = 0.3f * air; p.lArmY = -0.3f * air;
                    p.bodyX += 0.35f * crouch - 0.25f * air + 0.8f * slam * (1 - rec);
                    p.bodyY = 0;
                    p.headX += -0.3f * air + 0.2f * slam * (1 - rec);
                    p.rLegX += -0.8f * air + 1.3f * slam * (1 - rec); p.lLegX += -0.6f * air - 1.0f * slam * (1 - rec);
                    p.rootY -= 1.5f * crouch + 3f * slam * (1 - rec);
                    p.rootX = slam * (1 - rec) * (float) Math.sin(age * 5) * 0.2f; // impact tremor
                }));
    }

    // ------------------------------------------------------------------ model
    @Override
    public void buildModel(Rig rig) {
        Mat fabric = Mat.of(0x1c1d20).noise(0.12f);
        Mat vest = Mat.of(0x2b2d2b).noise(0.1f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.FRONT) for (int i = 0; i < w; i++) { g.px(x + i, y + 1, 0x161616); g.px(x + i, y + h - 2, 0x161616); } // MOLLE webbing
        });
        Mat plate = Mat.of(0x35383b).bevel(0.3f);
        Mat helm = Mat.of(0x222427).bevel(0.3f).noise(0.06f);
        Mat visor = Mat.of(RED).glow();
        Mat strap = Mat.of(0x121212);
        Mat pouch = Mat.of(0x303230).bevel(0.35f);
        Mat glove = Mat.of(0x161616).noise(0.05f);
        Mat boot = Mat.of(0x141312).bevel(0.3f);
        Mat skin = Mat.of(0xa87858);
        Mat patch = Mat.of(0x8a1a12).decal((m, g, x, y, w, h, f) -> g.glow(x + w / 2, y + h / 2, 0xff4020));
        Mat gunmetal = Mat.of(0x2e3033).bevel(0.25f);
        Mat steel = Mat.of(0x9a9ea4).noise(0.05f);
        Mat poly = Mat.of(0x191919);
        Mat olive = Mat.of(0x4a5236).noise(0.1f);
        Mat lamp = Mat.of(0xfff4c0).glow();
        Mat hot = Mat.of(0xff8a30).glow();
        Mat green = Mat.of(BFG_GREEN).glow();
        Mat sawRed = Mat.of(0xb8401c).bevel(0.3f);

        rig.humanoid(fabric, fabric, helm);
        // --- torso: cargo vest, pouches, belt, radio pack, chest knife
        rig.box(rig.torso, vest, -4.5f, -11.5f, -3f, 9, 8, 1);
        rig.box(rig.torso, vest, -4.5f, -11.5f, 2f, 9, 9, 1);
        rig.box(rig.torso, pouch, -4f, -5f, -4f, 2, 3, 1);
        rig.box(rig.torso, pouch, -1f, -5f, -4f, 2, 3, 1);
        rig.box(rig.torso, pouch, 2f, -5f, -4f, 2, 3, 1);
        rig.box(rig.torso, strap, -4.5f, -2f, -2.5f, 9, 2, 5);
        rig.box(rig.torso, pouch, 4f, -2.5f, -1.5f, 1, 3, 3); // dump pouch
        rig.box(rig.torso, plate, -3f, -11f, 3f, 6, 7, 2);    // radio pack
        rig.box(rig.torso, strap, 2f, -17f, 4f, 1, 6, 1);     // antenna
        rig.box(rig.torso, poly, -3.5f, -11f, -4f, 1, 4, 1);  // inverted chest knife
        rig.box(rig.torso, strap, -2f, -12.5f, -2.5f, 1, 1, 5); // shoulder straps
        rig.box(rig.torso, strap, 1f, -12.5f, -2.5f, 1, 1, 5);
        // --- head: RRTS helmet with red-lit visor band, ear cups, NVG mount, side lamp
        rig.box(rig.head, helm, -4.5f, -8.8f, -4.5f, 9, 2, 9);
        rig.box(rig.head, visor, -3.5f, -5.5f, -4.6f, 7, 2, 1);
        rig.box(rig.head, helm, -4f, -3.5f, -4.4f, 8, 1, 1); // visor lower rim
        rig.box(rig.head, skin, -2.5f, -2.5f, -4.2f, 5, 2, 1); // jaw
        rig.box(rig.head, plate, -4.9f, -5f, -2f, 1, 3, 3);
        rig.box(rig.head, plate, 3.9f, -5f, -2f, 1, 3, 3);
        rig.box(rig.head, gunmetal, -1f, -9.5f, -5f, 2, 2, 2);
        rig.box(rig.head, visor, 4.3f, -7.5f, -3f, 1, 1, 2);
        // --- arms: shoulder plates, gloves, red unit patch
        for (PartDefinition arm : new PartDefinition[]{rig.rArm, rig.lArm}) {
            rig.box(arm, plate, -2.5f, -2.6f, -2.5f, 5, 3, 5);
            rig.box(arm, glove, -2f, 7f, -2f, 4, 5, 4, 0.2f);
            rig.box(arm, strap, -2f, 3f, -2f, 4, 1, 4, 0.1f);
        }
        rig.box(rig.lArm, patch, 2.1f, 0.5f, -1f, 1, 2, 2);
        // --- legs: knee pads, boots, thigh holster, cargo pocket
        for (PartDefinition leg : new PartDefinition[]{rig.rLeg, rig.lLeg}) {
            rig.box(leg, plate, -2f, 5.5f, -2.8f, 4, 2, 1);
            rig.box(leg, boot, -2f, 9.5f, -2.8f, 4, 3, 5, 0.15f);
        }
        rig.box(rig.rLeg, poly, -3.2f, 1.5f, -1.5f, 1, 4, 3);
        rig.box(rig.lLeg, pouch, 2f, 2f, -1.5f, 1, 3, 3);

        // --- weapons (gun space: origin = fist, barrel along -z, y down)
        PartDefinition w = rig.weapon(rig.rHand, "knife");
        rig.box(w, poly, -0.5f, -0.5f, -1f, 1, 1, 3);
        rig.box(w, gunmetal, -1f, -1f, -2f, 2, 2, 1);
        rig.box(w, steel, -0.5f, -1.2f, -8f, 1, 2, 6);
        rig.box(w, steel, -0.5f, -0.7f, -9f, 1, 1, 1);

        w = rig.weapon(rig.rHand, "pistol");
        rig.box(w, gunmetal, -1f, -3f, -5f, 2, 2, 7);
        rig.box(w, poly, -1f, -1f, -4f, 2, 1, 4);
        rig.box(w, poly, -1f, -1f, -0.5f, 2, 4, 2);
        rig.box(w, steel, -0.5f, -2.5f, -6f, 1, 1, 1);
        rig.box(w, visor, -0.5f, -0.5f, -4.5f, 1, 1, 1); // laser module

        w = rig.weapon(rig.rHand, "rifle");
        rig.box(w, gunmetal, -1f, -3.5f, -5f, 2, 3, 9);
        rig.box(w, poly, -1.5f, -3.5f, -11f, 3, 3, 6);
        rig.box(w, steel, -0.5f, -3f, -15f, 1, 1, 4);
        rig.box(w, poly, -1f, -3f, 4f, 2, 2, 5);
        rig.box(w, poly, -1f, -3.5f, 9f, 2, 4, 1);
        rig.box(w, gunmetal, -1f, -0.5f, -4f, 2, 4, 2);
        rig.box(w, poly, -1f, -0.5f, -0.5f, 2, 3, 2);
        rig.box(w, gunmetal, -0.5f, -5f, -3f, 1, 2, 4);
        rig.box(w, visor, -0.5f, -5f, -3.5f, 1, 1, 1);  // red dot
        rig.box(w, steel, -2.5f, -3f, -10f, 1, 1, 3);   // flashlight
        rig.box(w, lamp, -2.5f, -3f, -11f, 1, 1, 1);

        w = rig.weapon(rig.rHand, "shotgun");
        rig.box(w, gunmetal, -1f, -3.5f, -4f, 2, 3, 7);
        rig.box(w, steel, -0.5f, -3.5f, -14f, 1, 1, 10);
        rig.box(w, gunmetal, -0.5f, -2.5f, -12f, 1, 1, 8);
        rig.box(w, poly, -1f, -2.8f, -10f, 2, 2, 4);
        rig.box(w, poly, -1f, -3f, 3f, 2, 3, 6);
        rig.box(w, poly, -1f, -1f, 0f, 2, 3, 2);

        w = rig.weapon(rig.rHand, "chaingun");
        rig.box(w, gunmetal, -2f, -4f, -6f, 4, 4, 10);
        for (int i = 0; i < 4; i++) rig.box(w, steel, i % 2 == 0 ? -1.5f : 0.5f, i < 2 ? -3.5f : -1.5f, -16f, 1, 1, 10);
        rig.box(w, gunmetal, -2f, -4f, -15f, 4, 4, 1);
        rig.box(w, gunmetal, -2f, -4f, -10f, 4, 4, 1);
        rig.box(w, hot, -1.5f, -3.5f, -16.5f, 3, 3, 1);  // glowing muzzle shroud
        rig.box(w, olive, 2f, -3f, -4f, 3, 4, 4);         // ammo can
        rig.box(w, poly, -0.5f, -6f, -4f, 1, 2, 4);       // carry handle
        rig.box(w, poly, -1f, -0.5f, -0.5f, 2, 3, 2);

        w = rig.weapon(rig.rHand, "chainsaw");
        rig.box(w, sawRed, -2f, -4f, -4f, 4, 5, 6);
        rig.box(w, poly, -0.5f, -6f, -3f, 1, 2, 4);
        rig.box(w, poly, -1f, -0.5f, -0.5f, 2, 3, 2);
        rig.box(w, steel, -0.5f, -3.5f, -16f, 1, 3, 12);
        rig.box(w, poly, -0.5f, -4.2f, -17f, 1, 1, 13);
        rig.box(w, poly, -0.5f, -1.0f, -17f, 1, 1, 13);
        rig.box(w, steel, -0.5f, -3.5f, -17f, 1, 3, 1);
        rig.box(w, hot, 2f, -3f, -1f, 1, 1, 1);           // hot exhaust

        w = rig.weapon(rig.rHand, "rocket");
        rig.box(w, olive, -1.5f, -5f, -12f, 3, 3, 18);
        rig.box(w, gunmetal, -2f, -5.5f, -13f, 4, 4, 1);
        rig.box(w, gunmetal, -2f, -5.5f, 6f, 4, 4, 1);
        rig.box(w, sawRed, -1f, -4.5f, -14f, 2, 2, 1);    // warhead nose
        rig.box(w, poly, -0.5f, -2f, -1f, 1, 3, 2);
        rig.box(w, gunmetal, -2.8f, -6f, -4f, 1, 2, 2);   // sight

        w = rig.weapon(rig.rHand, "bfg");
        rig.box(w, Mat.of(0x6e7470).bevel(0.3f), -3f, -6f, -8f, 6, 6, 12);
        rig.box(w, green, -2f, -5f, -10f, 4, 4, 2);
        rig.box(w, gunmetal, -2.5f, -5.5f, -11f, 5, 5, 1);
        rig.box(w, green, 3f, -5f, -5f, 1, 2, 6);
        rig.box(w, green, -4f, -5f, -5f, 1, 2, 6);
        rig.box(w, poly, -1f, -1f, -1f, 2, 3, 2);
        rig.box(w, gunmetal, -0.5f, -8f, -4f, 1, 2, 5);
    }
}
