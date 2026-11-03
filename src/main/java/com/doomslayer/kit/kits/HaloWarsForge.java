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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Sgt. John Forge, Halo Wars: UNSC Marine sergeant of the Spirit of Fire.
 * MA5B / M6 / rifle butt / frag + plasma grenades / Spartan Laser, plus commander call-ins
 * (rocket barrage, carpet bomb, MAC blast). Passive: armour shield recharge (absorption).
 */
public class HaloWarsForge extends SlayerKit {
    static final int AMMO = 0x40d0ff, LASER = 0xff2a1a, PLASMA_B = 0x3a8cff, SMOKE_RED = 0xff3030, SHIELD = 0xffd040;

    static final ProjStyle FRAG = ProjStyle.make().color(0x4c5a32).size(0.26f).life(34).explode(3.5f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle PLASMA_NADE = ProjStyle.make().color(PLASMA_B).size(0.32f).life(40).gravity(0.04).trail(ParticleTypes.ELECTRIC_SPARK)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                if (p.getOwner() instanceof SlayerEntity o) {
                    if (hit instanceof LivingEntity h && Combat.isFoe(o, h)) Combat.damage(o, h, 10); // stuck: full charge into the target
                    Combat.blast(o, pos, 3.5f, 11f, 0.9f, null);
                }
                Fx.dustBurst(l, PLASMA_B, 2f, pos, 40, 1.4);
                Fx.ring(l, pos, 2.2f, 0x80c0ff, 1.4f, 26);
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 30, 0.8, 0.3);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1.2f, 1.5f);
                Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1.0f, 1.6f);
            });
    static final ProjStyle BARRAGE_ROCKET = ProjStyle.make().color(0xffa040).size(0.45f).life(60).explode(3.2f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle BOMB = ProjStyle.make().color(0x606860).size(0.5f).life(70).gravity(0.06).explode(4f).trail(ParticleTypes.SMOKE)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                if (p.getOwner() instanceof SlayerEntity o) Combat.blast(o, pos, 4.5f, 16f, 1.2f, null);
                Fx.explosion(l, pos, 4f);
                Fx.burst(l, ParticleTypes.FLAME, pos, 30, 1.5, 0.1);
                Fx.ring(l, pos.add(0, 0.2, 0), 3.5f, 0x7a6a50, 1.4f, 28);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 2f, 0.7f);
            });

    public HaloWarsForge() {
        super("halo_wars_forge", "Sgt. John Forge", "ジョン・フォージ軍曹", "Halo Wars");
        eggPrimary = 0x4a5a34; eggSecondary = SHIELD;
        maxHealth = 80; speed = 0.31; armor = 12; knockbackRes = 0.4;
        scale = 1.02f; preferredRange = 11f; dmgMult = 1f;
        idleWeaponR = "ma5b";
    }

    // ================= passive: shield recharge =================
    @Override
    public void passive(SlayerEntity e) {
        // Marine-grade energy shield emulated with absorption: recharges after 4 s without being hit.
        float max = 10f, cur = e.getAbsorptionAmount();
        if (cur >= max || e.tickCount - e.getLastHurtByMobTimestamp() < 80 || e.hurtTime > 0 || e.tickCount % 4 != 0) return;
        if (cur <= 0) { snd(e, SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.8f); Fx.ring(L(e), e.position().add(0, 1, 0), 0.8f, SHIELD, 1f, 18); }
        e.setAbsorptionAmount(Math.min(max, cur + 1f));
        Fx.dustBurst(L(e), SHIELD, 0.7f, e.position().add(0, 1.1, 0), 2, 0.4);
        if (cur + 1f >= max) snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.4f);
    }

    // ================= helpers (server) =================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.15, 0.3, 0); }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        return Combat.dirTo(mz(e), t, spread, new Random(e.getRandom().nextLong()));
    }
    static Vec3 endOf(SlayerEntity e, LivingEntity t, Vec3 d, double r) { return t != null ? mid(t) : mz(e).add(d.scale(r)); }
    static void flash(SlayerEntity e, int rgb) {
        Fx.burst(L(e), ParticleTypes.FLAME, mz(e), 2, 0.04, 0.02);
        Fx.dustBurst(L(e), rgb, 1f, mz(e), 3, 0.06);
    }
    static void brass(SlayerEntity e) { Fx.burst(L(e), ParticleTypes.SMOKE, Combat.muzzle(e, 0.3, 0.55, 0.1), 1, 0.02, 0.02); }
    /** where a call-in lands: on the target, else ahead of Forge */
    static Vec3 zone(SlayerEntity e, LivingEntity t) {
        return t != null ? t.position() : e.position().add(Vec3.directionFromRotation(0, e.getYRot()).scale(10));
    }
    static void radio(SlayerEntity e, int n) {
        if (n % 3 == 0) snd(e, SoundEvents.NOTE_BLOCK_BIT.value(), 0.5f, 0.8f + e.getRandom().nextFloat() * 0.6f);
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** MA5B shouldered: square Marine stance, left hand on the shroud, cheek on the stock */
    static void shoulder(Pose p, float k, float pitch, float recoil) {
        float tw = 0.18f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.1f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.08f - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.5f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * k; p.headX += 0.08f * k;
        p.bodyX += (recoil * 0.1f - pitch * 0.2f) * k; p.rootZ += recoil * 0.7f * k;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.3f, k);
        p.lLegZ -= 0.08f * k; p.rLegZ += 0.08f * k; p.rootY -= 0.35f * k;
    }
    static void kneel(Pose p, float k) {
        p.rLegX = lerp(p.rLegX, 1.3f, k); p.lLegX = lerp(p.lLegX, -1.3f, k); p.rootY -= 4.5f * k; p.bodyX += 0.1f * k;
    }
    /** left hand at the helmet mic / handset, head tilted into it */
    static void talk(Pose p, float k, float age) {
        p.lArmX = lerp(p.lArmX, -2.4f, k); p.lArmY = lerp(p.lArmY, -0.5f, k); p.lArmZ = lerp(p.lArmZ, -0.15f, k);
        p.headZ += 0.18f * k; p.headX += 0.06f * k * (float) Math.sin(age * 0.6f);
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // rifle at low ready, both hands on it
        p.rArmX = -0.6f + p.rArmX * 0.25f; p.rArmY = -0.2f;
        p.lArmX = -0.85f + p.lArmX * 0.25f; p.lArmY = 0.6f; p.lArmZ = 0.05f;
        p.rHandX = -0.4f; p.bodyX += 0.03f; p.headX += 0.02f * (float) Math.sin(age * 0.08f);
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- MA5B: controlled bursts from the shoulder ----------
        out.add(AttackDef.of("ma5b_burst").range(2, 20).cd(24).weight(4).dur(30).weapon("ma5b")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.4f, 1.6f);
                    if ((n >= 6 && n <= 11) || (n >= 16 && n <= 21)) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.04), 24, 2.4f, 0, 0xffe080);
                        flash(e, 0xffb040); if (n % 2 == 0) brass(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 1.7f + e.getRandom().nextFloat() * 0.2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .88f);
                    float fire = win(t, .2f, .21f) * (1 - win(t, .37f, .38f)) + win(t, .53f, .54f) * (1 - win(t, .7f, .71f));
                    float r = (0.25f + 0.2f * (float) Math.abs(Math.sin(a * 2.2f))) * fire;
                    shoulder(p, k, -0.03f * win(t, .38f, .53f), r); stance(p, k);
                    p.rHandZ += 0.03f * (float) Math.sin(a * 3.7f) * fire; p.bodyZ += 0.015f * (float) Math.sin(a * 2.9f) * fire;
                    p.headX -= 0.05f * fire; // flinch on full auto
                }));

        // ---------- MA5B: suppressing fire (kneel, long spray sweeping across the group) ----------
        out.add(AttackDef.of("ma5b_suppressing_fire").range(4, 22).cd(90).weight(2).dur(54).weapon("ma5b")
                .when((e, t) -> Combat.around(e, t.position(), 5).size() >= 2 || t.getMaxHealth() >= 40)
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.ARMOR_EQUIP_CHAIN, 0.8f, 0.8f);
                    if (n >= 12 && n < 46) {
                        Vec3 d = aim(e, t, 0.06);
                        double sw = Math.sin((n - 12) * 0.25) * 0.18; // sweep left/right
                        d = new Vec3(d.x * Math.cos(sw) - d.z * Math.sin(sw), d.y, d.x * Math.sin(sw) + d.z * Math.cos(sw));
                        Combat.hitscan(e, mz(e), d, 24, 1.8f, 0, 0xffe080);
                        flash(e, 0xffb040); if (n % 2 == 0) brass(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.6f + e.getRandom().nextFloat() * 0.3f);
                    }
                    if (n == 47) { snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.6f, 1.6f); }
                    if (n == 50) { snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.7f, 1.7f); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .92f), kn = win(t, .05f, .2f) * (1 - win(t, .88f, 1));
                    float fire = win(t, .22f, .23f) * (1 - win(t, .85f, .86f));
                    float sweep = (float) Math.sin((t * 54 - 12) * 0.25f) * 0.18f * fire;
                    shoulder(p, k, 0, (0.3f + 0.15f * (float) Math.sin(a * 2.7f)) * fire);
                    kneel(p, kn);
                    p.bodyY += sweep; p.headY += sweep * 0.6f; p.rootYaw += sweep * 0.3f;
                    p.rHandZ += 0.035f * (float) Math.sin(a * 4.1f) * fire;
                    // mag swap at the end: rifle tips, left hand drops to the vest and slams it back
                    float mag = bump(win(t, .86f, .95f));
                    p.rHandY += 0.5f * mag; p.lArmX += 1.0f * mag; p.lArmY -= 0.3f * mag; p.headX += 0.25f * mag;
                }));

        // ---------- M6 sidearm: one-handed aimed shots, off-hand bracing ----------
        out.add(AttackDef.of("m6_pistol").range(2, 18).cd(28).weight(3).dur(24).weapon("m6")
                .tick((e, t, n) -> {
                    if (n == 5 || n == 11 || n == 17) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.015), 22, 5f, 0, 0xfff0a0); flash(e, 0xffc060);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 0.8f, 1.6f); brass(e);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f);
                    float r = kick(t, 5 / 24f, .14f) + kick(t, 11 / 24f, .14f) + kick(t, 17 / 24f, .14f);
                    // bladed pistol stance: chest turned, right arm locked out, left fist braced under
                    p.bodyY -= 0.4f * k; p.headY += 0.4f * k;
                    p.rArmX = lerp(p.rArmX, -HALF - r * 0.5f, k); p.rArmY = lerp(p.rArmY, 0.4f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
                    p.lArmX = lerp(p.lArmX, -1.2f, k); p.lArmY = lerp(p.lArmY, 0.9f, k); p.lArmZ = lerp(p.lArmZ, 0.1f, k);
                    p.rHandX -= r * 0.45f; p.bodyX -= r * 0.05f; p.headX -= r * 0.05f; p.rootZ += r * 0.3f;
                    p.lLegX = lerp(p.lLegX, 0.3f, k); p.rLegX = lerp(p.rLegX, -0.25f, k); p.rootY -= 0.3f * k;
                }));

        // ---------- Rifle butt (MA5B stock to the face) ----------
        out.add(AttackDef.of("rifle_butt").range(0, 3.2f).cd(22).weight(3).dur(16).weapon("ma5b")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.6f, 1.2f);
                    if (n == 6) {
                        Fx.arc(L(e), e.position().add(0, 1.5, 0), e.getYRot(), 1.3f, -50, 40, -20, 0xd0d0c0, 0.7f, 9);
                        for (LivingEntity f : Combat.cone(e, 3.4, 50)) {
                            Combat.damage(e, f, 9); Combat.knock(f, e.position(), 1.0, 0.35); Fx.sparks(L(e), mid(f), 6);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.8f); snd(e, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.4f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    // wind-up: rifle pulled to the right shoulder, torso coiled; strike: stock driven forward/left
                    p.rArmX = track(t, 0, -0.6f, .3f, -1.2f, .4f, -1.9f, .6f, -1.6f, 1, -0.6f);
                    p.rArmY = track(t, 0, -0.2f, .3f, -0.6f, .4f, 0.4f, .65f, 0.3f, 1, -0.2f);
                    p.rArmZ = track(t, 0, 0, .3f, 0.4f, .4f, -0.2f, 1, 0);
                    p.rHandX = track(t, 0, -0.4f, .3f, 0.6f, .4f, 2.4f, .65f, 2.2f, 1, -0.4f); // flip so the stock leads
                    p.lArmX = track(t, 0, -0.85f, .3f, -1.3f, .4f, -1.0f, 1, -0.85f); p.lArmY = track(t, 0, 0.6f, .4f, 0.2f, 1, 0.6f);
                    p.bodyY = track(t, 0, 0, .3f, -0.45f, .42f, 0.5f, .65f, 0.35f, 1, 0);
                    p.bodyX = track(t, 0, 0, .3f, -0.1f, .42f, 0.3f, 1, 0);
                    p.headY = -p.bodyY * 0.6f; p.headX = track(t, .38f, 0, .45f, 0.15f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, .75f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .42f, 0.45f, .75f, 0.35f, 1, 0);
                    p.rootZ = -2.0f * bump(win(t, .3f, .85f)); p.rootY -= 0.5f * bump(win(t, .3f, .85f));
                }));

        // ---------- Execution: rifle-butt to the knees, M6 point blank ----------
        out.add(AttackDef.of("execution_m6").range(0, 3.5f).lowHealthTarget(0.25f).weight(6).cd(80).dur(32).weapons("m6", "knife")
                .tick((e, t, n) -> {
                    if (t != null && Combat.isFoe(e, t)) {
                        Vec3 f = Vec3.directionFromRotation(0, e.getYRot()), q = e.position().add(f.scale(1.1 + t.getBbWidth() * 0.5));
                        if (t instanceof Mob m) m.getNavigation().stop();
                        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(q.x, q.y, q.z); t.hurtMarked = true;
                    }
                    if (n == 0) { Combat.lunge(e, 0.45, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.8f); }
                    if (n == 8 && t != null) {
                        Combat.damage(e, t, Math.max(0, t.getHealth() - 1)); Fx.sparks(L(e), mid(t), 8);
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.1f, 0.7f); snd(e, SoundEvents.TRIDENT_HIT, 0.8f, 1.2f);
                    }
                    if (n == 21 && t != null) {
                        Combat.damage(e, t, t.getHealth() + 50);
                        Fx.burst(L(e), ParticleTypes.FLAME, mid(t), 6, 0.1, 0.05); Fx.burst(L(e), ParticleTypes.DAMAGE_INDICATOR, mid(t), 10, 0.3, 0.1);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 1.2f); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1, 0.7f);
                        e.heal(4);
                    }
                })
                .anim((p, t, a) -> {
                    // 1: left-hand knife slash to drop them; 2: step in, M6 jammed down; 3: shot, recoil, stand
                    p.lArmX = track(t, 0, 0, .12f, -2.4f, .25f, -0.7f, .4f, -0.9f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .12f, -0.5f, .25f, 0.5f, .5f, 0.2f, 1, 0);
                    p.lHandX = track(t, 0, 0, .12f, 0.8f, .25f, -0.4f, 1, 0);
                    float r = 2f * kick(t, 21 / 32f, .2f);
                    p.rArmX = track(t, 0, -0.3f, .3f, -0.4f, .5f, -1.1f, .66f, -1.0f, .9f, -0.8f, 1, -0.3f) - r * 0.4f;
                    p.rArmY = track(t, 0, 0, .5f, 0.25f, 1, 0); p.rHandX = -r * 0.4f;
                    p.bodyY = track(t, 0, 0, .12f, 0.35f, .25f, -0.4f, .5f, 0, 1, 0);
                    p.bodyX = track(t, 0, 0, .25f, 0.3f, .5f, 0.45f, .66f, 0.4f, .85f, 0.2f, 1, 0) - r * 0.1f;
                    p.headX = track(t, 0, 0, .3f, 0.2f, .55f, 0.5f, .8f, 0.4f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.rootY = track(t, .3f, 0, .5f, -1.4f, .85f, -1.2f, 1, 0);
                    p.rootZ = track(t, 0, 0, .12f, -1.2f, .3f, -1.5f, .5f, -2f, 1, 0) + r * 0.4f;
                    p.lLegX = track(t, 0, 0, .25f, -0.5f, .5f, -0.8f, .85f, -0.8f, 1, 0);
                    p.rLegX = track(t, 0, 0, .25f, 0.3f, .5f, 0.6f, .85f, 0.6f, 1, 0);
                    p.weaponL = t < .45f ? "knife" : null;
                }));

        // ---------- M9 frag grenade (overhand lob, rifle tucked) ----------
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(150).weight(2).dur(22).weapons("ma5b", "frag")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.4f);
                    if (n == 11) {
                        Vec3 from = Combat.muzzle(e, 0.5, -0.4, 0.5);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, t, 0.02).add(0, 0.15 + dist * 0.018, 0).normalize();
                        Combat.shoot(e, FRAG, from, d, 0.75 + dist * 0.025, 13f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "frag" : null;
                    p.rArmX = -0.5f; p.rArmY = -0.3f; p.rArmZ = 0.2f; p.rHandX = -0.6f; // rifle swung low to the side
                    p.lArmX = track(t, 0, 0, .18f, -0.6f, .4f, 0.8f, .5f, -2.5f, .7f, -1.5f, 1, 0); // pin pull, cock back, release
                    p.lArmZ = track(t, 0, 0, .18f, 0.3f, .4f, -0.5f, .5f, -0.1f, 1, 0);
                    p.headX = track(t, 0, 0, .3f, -0.25f, .55f, 0.1f, 1, 0); // look up the arc
                    p.bodyY = track(t, 0, 0, .4f, -0.6f, .55f, 0.45f, .75f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .4f, -0.25f, .55f, 0.35f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.35f, .55f, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, .4f, 0.3f, .55f, 0.5f, 1, 0);
                    p.rootZ = track(t, .4f, 0, .55f, -1.3f, 1, 0); p.rootY = -0.5f * bump(win(t, .4f, .85f));
                }));

        // ---------- Plasma grenade (scavenged Covenant: armed glow, side-arm sticky throw) ----------
        out.add(AttackDef.of("plasma_grenade").range(4, 16).cd(170).weight(2).dur(24).weapons("ma5b", "plasma_nade")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.BEACON_ACTIVATE, 0.6f, 1.9f);
                    if (n > 4 && n < 12) Fx.dustBurst(L(e), PLASMA_B, 0.8f, Combat.muzzle(e, 0.2, -0.45, 0.3), 2, 0.1);
                    if (n == 12) {
                        Vec3 from = Combat.muzzle(e, 0.5, -0.4, 0.3);
                        double dist = t != null ? from.distanceTo(mid(t)) : 8;
                        Vec3 d = aim(e, t, 0.01).add(0, 0.08 + dist * 0.015, 0).normalize();
                        Combat.shoot(e, PLASMA_NADE, from, d, 0.85 + dist * 0.02, 0f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 1.3f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .52f ? "plasma_nade" : null;
                    p.rArmX = -0.55f; p.rArmY = -0.25f; p.rHandX = -0.5f;
                    // arm held out to the side while it primes (fizzing), then a flat sidearm whip
                    float prime = win(t, .1f, .25f) * (1 - win(t, .4f, .45f));
                    p.lArmX = track(t, 0, 0, .25f, -1.0f, .42f, -0.9f, .52f, -1.6f, .75f, -1.1f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .25f, -1.1f, .42f, -1.3f, .52f, 0.4f, .75f, 0.3f, 1, 0);
                    p.lArmY = 0.05f * (float) Math.sin(a * 5f) * prime;
                    p.bodyY = track(t, 0, 0, .42f, -0.7f, .55f, 0.5f, .8f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyZ = track(t, 0, 0, .42f, 0.12f, .55f, -0.08f, 1, 0);
                    p.lLegX = track(t, 0, 0, .35f, -0.4f, .6f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .45f, 0.35f, .6f, 0.4f, 1, 0);
                    p.rootZ = track(t, .42f, 0, .58f, -1.1f, 1, 0); p.rootY = -0.6f * bump(win(t, .3f, .9f));
                }));

        // ---------- Spartan Laser (shouldered, long red charge, piercing beam) ----------
        out.add(AttackDef.of("spartan_laser").range(8, 42).cd(220).weight(2).dur(52).weapon("splaser")
                .when((e, t) -> t.getMaxHealth() >= 30 || Combat.around(e, t.position(), 5).size() >= 3)
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.PISTON_EXTEND, 0.8f, 0.7f);
                    if (n >= 10 && n < 36) {
                        if (n % 4 == 2) snd(e, SoundEvents.BEACON_AMBIENT, 1f, 0.8f + (n - 10) * 0.05f);
                        Fx.dustBurst(L(e), LASER, 0.6f + (n - 10) * 0.02f, mz(e), 2, 0.05 + (36 - n) * 0.01);
                        if (t != null && n % 3 == 0) Fx.beam(L(e), mz(e), mid(t), 0xff6050, 0.35f, 0.8); // aiming dot
                    }
                    if (n == 36) {
                        Vec3 d = aim(e, t, 0), m = mz(e);
                        Combat.hitscan(e, m, d, 46, 32f, 6, LASER);
                        Fx.beam(L(e), m, m.add(d.scale(46)), LASER, 1.6f, 0.3);
                        Fx.beam(L(e), m, m.add(d.scale(46)), 0xffd0c0, 0.7f, 0.5);
                        Vec3 end = endOf(e, t, d, 46);
                        Fx.burst(L(e), ParticleTypes.FLAME, end, 20, 0.4, 0.08); Fx.explosion(L(e), end, 1.5f);
                        snd(e, SoundEvents.WARDEN_SONIC_BOOM, 1.2f, 1.6f); snd(e, SoundEvents.BEACON_DEACTIVATE, 1.2f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), ch = win(t, .2f, .69f) * (1 - win(t, .69f, .7f)), r = 2.6f * kick(t, 36 / 52f, .25f);
                    // heavy tube on the right shoulder: arms raised higher, head canted to the sight
                    shoulder(p, k, 0.08f, r); stance(p, k);
                    p.rArmZ += 0.12f * k; p.headZ += 0.15f * k; p.headX += 0.1f * ch;
                    p.bodyZ += 0.03f * ch * ch * (float) Math.sin(a * 5.5f); p.rArmY += 0.02f * ch * (float) Math.sin(a * 6.3f);
                    p.lLegX -= 0.15f * ch; p.rootY -= 0.4f * ch; // braces as the charge builds
                    p.rHandX -= r * 0.3f; p.bodyX -= r * 0.15f; p.rootZ += r * 1.2f; p.headX -= r * 0.1f;
                }));

        // ---------- Commander: rocket barrage (radio call, rockets rain on the target area) ----------
        out.add(AttackDef.of("rocket_barrage").range(8, 34).cd(360).weight(2).dur(60).weapons("ma5b", "radio")
                .when((e, t) -> Combat.around(e, t.position(), 6).size() >= 2 || t.getMaxHealth() >= 50)
                .tick((e, t, n) -> {
                    if (n < 20) radio(e, n);
                    if (n == 6 && t != null) { Fx.ring(L(e), t.position().add(0, 0.1, 0), 4f, SMOKE_RED, 1.3f, 30); snd(e, SoundEvents.NOTE_BLOCK_PLING.value(), 1, 1.5f); }
                    if (n == 18) snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.5f, 0.5f);
                    if (n >= 24 && n <= 50 && n % 3 == 0) {
                        Vec3 c = zone(e, t), r = new Vec3(e.getRandom().nextGaussian() * 2.5, 0, e.getRandom().nextGaussian() * 2.5);
                        Vec3 from = c.add(r).add(-3, 18, -3), to = c.add(r);
                        Combat.shoot(e, BARRAGE_ROCKET, from, to.subtract(from).normalize(), 1.8, 10f);
                        Fx.sound(L(e), from, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .92f), call = win(t, .05f, .15f) * (1 - win(t, .35f, .42f));
                    // rifle hangs in the right hand; left at the handset; then points at the zone and watches the sky
                    p.rArmX = lerp(p.rArmX, -0.45f, k); p.rArmY = lerp(p.rArmY, -0.2f, k); p.rHandX = -0.5f * k;
                    talk(p, call, a);
                    float point = win(t, .4f, .5f) * (1 - win(t, .85f, .95f));
                    p.lArmX = lerp(p.lArmX, -1.75f, point); p.lArmY = lerp(p.lArmY, 0.2f, point); p.lArmZ = lerp(p.lArmZ, 0, point);
                    p.headX += -0.35f * win(t, .4f, .55f) * (1 - win(t, .9f, 1)) + 0.03f * (float) Math.sin(a * 1.7f) * point;
                    p.bodyX -= 0.08f * point; p.bodyY += 0.15f * call - 0.1f * point; p.headY -= 0.1f * call;
                    stance(p, k);
                    p.rootY -= 0.4f * bump(win(t, .38f, .5f)); // ducks as they fly over
                }));

        // ---------- Commander: carpet bomb (throws a smoke marker, bombers sweep the line) ----------
        out.add(AttackDef.of("carpet_bomb").range(8, 30).cd(460).weight(2).dur(64).weapons("ma5b", "flare")
                .when((e, t) -> Combat.around(e, t.position(), 7).size() >= 3 || t.getMaxHealth() >= 80)
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.FLINTANDSTEEL_USE, 1, 1);
                    if (n > 4 && n < 14) Fx.dustBurst(L(e), SMOKE_RED, 1.2f, Combat.muzzle(e, 0.2, -0.45, 0.4), 2, 0.1);
                    if (n == 14) snd(e, SoundEvents.WITCH_THROW, 1, 0.7f);
                    if (n >= 14 && n < 40 && n % 2 == 0) {
                        Vec3 c = zone(e, t);
                        Fx.dustBurst(L(e), SMOKE_RED, 2f, c.add(0, 0.3 + (n - 14) * 0.08, 0), 6, 0.3);
                        Fx.burst(L(e), ParticleTypes.CAMPFIRE_COSY_SMOKE, c.add(0, 0.3, 0), 1, 0.1, 0.02);
                    }
                    if (n == 22) { radio(e, 0); snd(e, SoundEvents.NOTE_BLOCK_PLING.value(), 1, 1.2f); }
                    if (n == 30) snd(e, SoundEvents.ELYTRA_FLYING, 2f, 0.6f);
                    if (n >= 34 && n <= 52 && n % 2 == 0) {
                        // bomb line runs from behind the marker through it along Forge's facing
                        Vec3 f = Vec3.directionFromRotation(0, e.getYRot());
                        Vec3 c = zone(e, t).add(f.scale((n - 43) * 1.1));
                        Vec3 from = c.add(e.getRandom().nextGaussian() * 0.8, 20, e.getRandom().nextGaussian() * 0.8);
                        Combat.shoot(e, BOMB, from, new Vec3(f.x * 0.15, -1, f.z * 0.15).normalize(), 1.2, 0f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .92f);
                    p.weaponL = t < .22f ? "flare" : "radio";
                    p.rArmX = lerp(p.rArmX, -0.45f, k); p.rArmY = lerp(p.rArmY, -0.2f, k); p.rHandX = -0.5f * k;
                    // underhand toss of the smoke
                    float toss = win(t, 0, .25f) * (1 - win(t, .3f, .35f));
                    p.lArmX = track(t, 0, 0, .12f, 0.7f, .22f, -1.3f, .3f, -0.4f, 1, 0) * toss;
                    p.bodyX += 0.25f * bump(win(t, .1f, .3f)) * toss; p.lLegX = lerp(p.lLegX, -0.55f, bump(win(t, .05f, .35f)));
                    // radio call
                    talk(p, win(t, .3f, .38f) * (1 - win(t, .5f, .55f)), a);
                    // wave the bombers in, then brace/crouch away from the blasts
                    float wave = win(t, .5f, .55f) * (1 - win(t, .7f, .75f));
                    p.lArmX = lerp(p.lArmX, -2.8f, wave); p.lArmZ = lerp(p.lArmZ, -0.3f + 0.4f * (float) Math.sin(a * 0.9f), wave);
                    p.headX -= 0.4f * wave;
                    float brace = win(t, .72f, .8f) * (1 - win(t, .92f, 1));
                    p.bodyX += 0.3f * brace; p.headX += 0.2f * brace; p.lArmX = lerp(p.lArmX, -1.2f, brace); p.rootY -= 1.8f * brace;
                    p.lLegX = lerp(p.lLegX, -0.6f, brace); p.rLegX = lerp(p.rLegX, 0.6f, brace);
                    p.bodyZ += 0.04f * (float) Math.sin(a * 3f) * brace; // ground shake
                }));

        // ---------- Commander: MAC blast (designator painted target, orbital slug from Spirit of Fire) ----------
        out.add(AttackDef.of("mac_blast").range(10, 36).cd(900).weight(1).dur(70).weapon("designator")
                .when((e, t) -> t.getMaxHealth() >= 100 || Combat.around(e, t.position(), 6).size() >= 4)
                .tick((e, t, n) -> {
                    if (n < 16) radio(e, n);
                    if (n >= 16 && n < 50 && t != null) {
                        Fx.beam(L(e), mz(e), mid(t), 0xff4040, 0.4f, 0.7);
                        if (n % 5 == 0) snd(e, SoundEvents.NOTE_BLOCK_PLING.value(), 0.7f, 1.0f + (n - 16) * 0.03f);
                    }
                    if (n == 40) snd(e, SoundEvents.WARDEN_SONIC_CHARGE, 1.5f, 0.6f);
                    if (n == 50) {
                        Vec3 c = zone(e, t);
                        Fx.beam(L(e), c.add(0, 40, 0), c, 0xd0e8ff, 2f, 0.5);
                        Fx.beam(L(e), c.add(0, 40, 0), c, 0xffffff, 1f, 0.8);
                        Combat.blast(e, c, 6f, 40f, 2f, null);
                        Fx.explosion(L(e), c, 6f);
                        for (int i = 0; i < 4; i++) Fx.ring(L(e), c.add(0, 0.2 + i * 0.4, 0), 2 + i * 1.6f, 0xb0d0ff, 1.6f, 32);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, c, 50, 2.5, 0.1);
                        Fx.sound(L(e), c, SoundEvents.GENERIC_EXPLODE, 3f, 0.5f); Fx.sound(L(e), c, SoundEvents.LIGHTNING_BOLT_THUNDER, 2f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .92f), call = win(t, 0, .08f) * (1 - win(t, .2f, .25f));
                    float paint = win(t, .22f, .3f) * (1 - win(t, .72f, .78f));
                    float impact = kick(t, 50 / 70f, .25f);
                    // designator one-handed while radioing, then two-handed on the target
                    p.rArmX = lerp(p.rArmX, -HALF, k); p.rArmY = lerp(p.rArmY, -0.1f, k);
                    talk(p, call, a);
                    p.lArmX = lerp(p.lArmX, -1.4f, paint); p.lArmY = lerp(p.lArmY, 0.55f, paint); p.lArmZ = lerp(p.lArmZ, 0, paint);
                    p.headX += 0.12f * paint; p.bodyX += 0.08f * paint;
                    kneel(p, paint);
                    p.rArmZ += 0.015f * (float) Math.sin(a * 3.1f) * paint; // hand tremor holding the laser steady
                    // shockwave: thrown back on the knee, arm over the visor
                    p.bodyX -= 0.4f * impact; p.headX -= 0.3f * impact; p.lArmX -= 0.8f * impact; p.rootZ += 2f * impact;
                }));
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat fat = Mat.of(0x4f5a3a).noise(.15f);                       // olive BDU fatigues
        Mat plate = Mat.of(0x3e4a2c).bevel(.32f).noise(.09f);          // M52B body armour
        Mat dark = Mat.of(0x23281c).noise(.1f), webbing = Mat.of(0x5c5236).noise(.14f);
        Mat metal = Mat.of(0x6a6c68).noise(.06f), black = Mat.of(0x161616), skin = Mat.of(0xb07a58).noise(.1f);
        Mat visor = Mat.of(SHIELD).glow(), green = Mat.of(0x60ff80).glow();
        Mat chest = Mat.of(0x3e4a2c).bevel(.32f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            g.rect(x + 1, y + 1, 3, 2, 0xd8d8c8);                       // name tape
            g.rect(x + w - 3, y + 1, 2, 1, 0x2a3020); g.rect(x + w - 3, y + 3, 2, 1, 0x2a3020); // plate seams
        });
        Mat sarge = Mat.of(0x3e4a2c).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.LEFT && f != Mat.Face.RIGHT) return;
            int cx = x + w / 2;                                         // sergeant chevrons
            for (int r = 0; r < 3; r++) for (int i = 0; i < 3; i++) { g.px(cx - 1 - i, y + 1 + i + r * 2, 0xe0c060); g.px(cx + i, y + 1 + i + r * 2, 0xe0c060); }
        });
        Mat helm = Mat.of(0x48553a).bevel(.3f).noise(.1f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) g.rect(x + 1, y + h - 2, w - 2, 1, 0x2a3020);
            if (f == Mat.Face.TOP) g.rect(x + w / 2 - 1, y + 1, 2, h - 2, 0x2f3826);
        });

        // torso: fatigues, armour plate, webbing, belt, radio pack
        rig.box(rig.torso, fat, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4, -12, -3, 8, 7, 1, .25f);
        rig.box(rig.torso, plate, -3.5f, -5, -2.8f, 7, 2, 1);
        rig.box(rig.torso, webbing, -4.5f, -2.5f, -2.5f, 9, 2, 5);
        rig.box(rig.torso, webbing, -3.5f, -4, -3.8f, 2, 2, 1);   // mag pouches
        rig.box(rig.torso, webbing, 1.5f, -4, -3.8f, 2, 2, 1);
        rig.box(rig.torso, metal, -0.5f, -2.3f, -3, 1, 1, 1);     // buckle
        rig.box(rig.torso, plate, -4, -11.5f, 2, 8, 9, 1);        // back plate
        rig.box(rig.torso, dark, -3, -11, 3, 6, 7, 2);            // field radio
        rig.box(rig.torso, green, 1, -10, 5, 1, 1, 1);            // radio status light
        rig.box(rig.torso, black, 2, -18, 3.5f, 1, 8, 1);         // antenna
        // head: bare stubbled jaw under the CH252 helmet, amber HUD eyepiece
        rig.box(rig.head, skin, -4, -8, -4, 8, 8, 8);
        rig.box(rig.head, Mat.of(0x4a3a30).noise(.3f), -3.5f, -2.2f, -4.3f, 7, 2, 1); // stubble
        rig.box(rig.head, helm, -4.5f, -9, -4.5f, 9, 5, 9, .2f);
        rig.box(rig.head, helm, -4.5f, -4, 0, 9, 2, 5);           // neck guard / ear flaps
        rig.box(rig.head, dark, -4.5f, -9.6f, -5.2f, 9, 1, 2);    // brim
        rig.box(rig.head, visor, -3.5f, -5.6f, -5, 3, 2, 1);      // HUD eyepiece (left eye)
        rig.box(rig.head, black, 0.5f, -5.6f, -4.8f, 3, 1, 1);    // goggle strap
        rig.box(rig.head, black, 3.6f, -6, -2, 1, 3, 2);          // headset
        rig.box(rig.head, black, 3.6f, -3, -4, 1, 1, 3);          // boom mic
        // arms: shoulder pads (chevrons), rolled sleeves, forearm guards, gloves
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, fat, -2, -2, -2, 4, 12, 4);
            rig.box(arm, sarge, s < 0 ? -3f : -2f, -2.8f, -2.5f, 5, 4, 5);
            rig.box(arm, skin, -2, 4, -2, 4, 2, 4, .05f);
            rig.box(arm, plate, -2, 6, -2, 4, 3, 4, .3f);
            rig.box(arm, black, -2, 9, -2, 4, 2, 4, .2f);
        }
        rig.box(rig.lArm, dark, 1.6f, 6.5f, -1, 1, 2, 2);         // wrist TACPAD
        rig.box(rig.lArm, green, 2.4f, 7, -0.5f, 1, 1, 1);
        // legs: cargo pockets, knee pads, combat boots
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, fat, -2, 0, -2, 4, 12, 4);
            rig.box(leg, webbing, s < 0 ? -2.8f : 1.8f, 2, -1.5f, 1, 3, 3);
            rig.box(leg, plate, -2, 5, -2.8f, 4, 3, 1, .1f);
            rig.box(leg, black, -2, 9.5f, -3, 4, 3, 5, .2f);
        }
        rig.box(rig.rLeg, dark, -2.6f, 1, -1.5f, 1, 4, 2);       // holster
        buildWeapons(rig, metal, dark, black);
    }

    private static void buildWeapons(Rig rig, Mat metal, Mat dark, Mat black) {
        Mat gun = Mat.of(0x2e3230).noise(.07f), gunG = Mat.of(0x404a3a).noise(.08f), steel = Mat.of(0x9a9c98).noise(.05f);
        Mat cyan = Mat.of(AMMO).glow(), red = Mat.of(LASER).glow(), blue = Mat.of(PLASMA_B).glow(), flare = Mat.of(SMOKE_RED).glow();
        PartDefinition w;
        // MA5B assault rifle: bullpup body, shroud, carry handle, digital ammo counter, mag, stock
        w = rig.weapon(rig.rHand, "ma5b");
        rig.box(w, gunG, -1.5f, -3, -9, 3, 4, 13);
        rig.box(w, gun, -1, -2.5f, -14, 2, 2, 5);
        rig.box(w, black, -0.5f, -2, -15.5f, 1, 1, 2);
        rig.box(w, gun, -1, -4.5f, -6, 2, 2, 6);
        rig.box(w, cyan, -0.5f, -4.2f, -6.3f, 1, 1, 1);
        rig.box(w, black, -1, 1, -1, 2, 4, 2);
        rig.box(w, dark, -1, 0, 2, 2, 3, 4);
        // M6 pistol: slide, smart-link scope, grip
        w = rig.weapon(rig.rHand, "m6");
        rig.box(w, steel, -1, -2.5f, -7, 2, 2, 8);
        rig.box(w, black, -0.5f, -3.5f, -4, 1, 1, 3);
        rig.box(w, gun, -1, -0.5f, -1, 2, 3, 2);
        // Spartan Laser: long tube, battery pack, lens, sight
        w = rig.weapon(rig.rHand, "splaser");
        rig.box(w, Mat.of(0x5a6050).noise(.06f), -2, -4, -16, 4, 4, 20);
        rig.box(w, red, -1.5f, -3.5f, -17, 3, 3, 1);
        rig.box(w, dark, -2.5f, -1, -4, 5, 3, 5);
        rig.box(w, black, -0.5f, -6, -8, 1, 2, 4);
        rig.box(w, red, -2.6f, -3, -12, 1, 1, 6);
        rig.box(w, black, -0.5f, 0, -1, 1, 3, 2);
        // laser target designator
        w = rig.weapon(rig.rHand, "designator");
        rig.box(w, dark, -1.5f, -3, -8, 3, 3, 9);
        rig.box(w, red, -1, -2.5f, -9, 2, 2, 1);
        rig.box(w, black, -0.5f, 0, -2, 1, 3, 2);
        // off-hand items
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, Mat.of(0x4c5a32), -1, -1, -2, 2, 2, 2); rig.box(w, metal, -0.5f, -1.8f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "plasma_nade");
        rig.box(w, blue, -1, -1, -2.5f, 2, 2, 2); rig.box(w, Mat.of(0x5a3a8a), -1.5f, -0.5f, -2, 3, 1, 1);
        w = rig.weapon(rig.lHand, "radio");
        rig.box(w, black, -1, -1, -3, 2, 2, 4); rig.box(w, metal, -0.5f, -2.5f, -2, 1, 2, 1);
        w = rig.weapon(rig.lHand, "flare");
        rig.box(w, flare, -0.5f, -1, -3, 1, 1, 3); rig.box(w, dark, -0.5f, -1, 0, 1, 1, 1);
        w = rig.weapon(rig.lHand, "knife");
        rig.box(w, steel, -0.5f, -1, -6, 1, 2, 5); rig.box(w, black, -0.5f, -0.5f, -1, 1, 1, 2);
    }
}
