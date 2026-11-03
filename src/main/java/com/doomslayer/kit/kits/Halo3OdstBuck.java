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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Gunnery Sgt. Edward Buck, Halo 3: ODST. Silenced SMG / MA5C / M6C SOCOM / M90 / Brute Shot, VISR, Spartan-grade haymaker. */
public class Halo3OdstBuck extends SlayerKit {
    static final int VISOR = 0x3aa8ff, TRACER = 0xffe8a0, PLASMA_C = 0x4aa0ff, BLOOD = 0x6a1010, SHIELD = 0x8fd0ff;
    static final float SHIELD_MAX = 8f;

    static final ProjStyle FRAG = ProjStyle.make().color(0x4c5a3a).size(0.26f).life(34).explode(3.4f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle BRUTE_NADE = ProjStyle.make().color(0x9a6a30).size(0.3f).life(40).explode(2.6f).gravity(0.045).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle PLASMA_NADE = ProjStyle.make().color(PLASMA_C).size(0.32f).life(40).gravity(0.04).homing(0.12f).trail(ParticleTypes.ELECTRIC_SPARK)
            .onHit((p, pos, hit) -> {
                if (!(p.getOwner() instanceof SlayerEntity o)) return;
                ServerLevel l = (ServerLevel) p.level();
                // stuck: the victim carries the charge for a beat, then it goes off
                if (hit instanceof LivingEntity v) Combat.damage(o, v, 8);
                Combat.blast(o, pos, 3.2f, 13f, 0.8f, null);
                Fx.explosion(l, pos, 2.2f);
                Fx.dustBurst(l, PLASMA_C, 2f, pos, 40, 1.4);
                Fx.ring(l, pos.add(0, 0.2, 0), 2.6f, 0x9ad8ff, 1.4f, 28);
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 30, 0.8, 0.3);
                Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1.3f, 1.5f);
                Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1.2f, 1.8f);
            });

    public Halo3OdstBuck() {
        super("halo3_odst_buck", "Buck", "バック", "Halo 3: ODST");
        eggPrimary = 0x2e3338; eggSecondary = VISOR;
        maxHealth = 70; speed = 0.31; armor = 12; knockbackRes = 0.4;
        scale = 1.0f; width = 0.65f; preferredRange = 8f; dmgMult = 1f;
        idleWeaponR = "smg";
    }

    // ================= helpers (server) =================
    static ServerLevel L(SlayerEntity e) { return (ServerLevel) e.level(); }
    static void snd(SlayerEntity e, SoundEvent s, float v, float p) { Combat.sound(e, s, v, p); }
    static Vec3 mz(SlayerEntity e) { return Combat.muzzle(e, 1.1, 0.3, 0); }
    static Vec3 aim(SlayerEntity e, LivingEntity t, double spread) {
        if (t == null) return e.getViewVector(1f);
        return Combat.dirTo(mz(e), t, spread, new Random(e.getRandom().nextLong()));
    }
    static Vec3 mid(LivingEntity t) { return t.position().add(0, t.getBbHeight() * 0.6, 0); }
    /** suppressor: almost no flash, a puff of gas */
    static void puff(SlayerEntity e) { Fx.burst(L(e), ParticleTypes.SMOKE, mz(e), 1, 0.02, 0.01); Fx.dustBurst(L(e), 0xc8c0a8, 0.6f, mz(e), 2, 0.04); }
    static void flash(SlayerEntity e) { Fx.burst(L(e), ParticleTypes.FLAME, mz(e), 3, 0.05, 0.02); Fx.dustBurst(L(e), 0xffb050, 1.1f, mz(e), 3, 0.08); }
    static void brass(SlayerEntity e) { Fx.dustBurst(L(e), 0xd8b040, 0.5f, Combat.muzzle(e, 0.3, 0.55, 0.15), 1, 0.05); }
    static void hold(SlayerEntity e, LivingEntity t, double dist, double lift) {
        if (t == null || !Combat.isFoe(e, t)) return;
        Vec3 f = Vec3.directionFromRotation(0, e.getYRot());
        Vec3 p = e.position().add(f.scale(dist + t.getBbWidth() * 0.5)).add(0, lift, 0);
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true;
    }
    static void lob(SlayerEntity e, LivingEntity t, ProjStyle s, Vec3 from, double base, float dmg) {
        double dist = t != null ? from.distanceTo(mid(t)) : 10;
        Vec3 d = aim(e, t, 0.03).add(0, 0.12 + dist * 0.02, 0).normalize();
        Combat.shoot(e, s, from, d, base + dist * 0.025, dmg);
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** shouldered long gun, bladed stance */
    static void rifle(Pose p, float k, float pitch, float recoil) {
        float tw = 0.28f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.12f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.08f - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.5f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * k; p.bodyX += (recoil * 0.1f - pitch * 0.2f) * k; p.headX -= recoil * 0.07f * k;
        p.rootZ += recoil * 0.8f * k;
    }
    /** heavy launcher braced at the hip/shoulder line */
    static void heavy(Pose p, float k, float pitch, float recoil) {
        p.rArmX = lerp(p.rArmX, -1.2f + pitch - recoil * 0.25f, k); p.rArmY = lerp(p.rArmY, -0.25f, k); p.rArmZ = lerp(p.rArmZ, 0.1f, k);
        p.lArmX = lerp(p.lArmX, -1.4f + pitch - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.6f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.rHandX = lerp(p.rHandX, -0.35f, k);
        p.bodyX += (0.1f + recoil * 0.12f) * k; p.bodyY += 0.2f * k; p.headY -= 0.2f * k; p.rootZ += recoil * 0.9f * k;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.26f, k);
        p.lLegZ -= 0.05f * k; p.rLegZ += 0.08f * k; p.rootY -= 0.45f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // SMG at low ready, muzzle down-left, both hands on it, slight soldierly hunch
        p.rArmX = -0.6f + p.rArmX * 0.25f; p.rArmY = -0.3f;
        p.lArmX = -0.8f + p.lArmX * 0.25f; p.lArmY = 0.6f; p.lArmZ = 0.05f;
        p.rHandX = -0.3f; p.bodyX += 0.05f; p.headX += 0.02f * (float) Math.sin(age * 0.05f);
    }

    /** Shield (stamina) regen: absorption = shield, recharges 5 s after the last hit. */
    @Override
    public void passive(SlayerEntity e) {
        CompoundTag d = e.getPersistentData();
        float sh = e.getAbsorptionAmount();
        if (e.hurtTime == 10) {
            d.putInt("odstHit", e.tickCount);
            if (sh <= 0 && d.getFloat("odstSh") > 0) { // shield popped
                snd(e, SoundEvents.SHIELD_BREAK, 0.9f, 1.4f);
                Fx.dustBurst(L(e), SHIELD, 1.2f, e.position().add(0, 1, 0), 20, 0.5);
            }
        }
        if (e.tickCount - d.getInt("odstHit") > 100 && sh < SHIELD_MAX) {
            if (sh <= 0) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.6f);
            sh = Math.min(SHIELD_MAX, sh + 0.25f);
            e.setAbsorptionAmount(sh);
            if (e.tickCount % 4 == 0) Fx.dustBurst(L(e), SHIELD, 0.8f, e.position().add(0, 1, 0), 4, 0.45);
            if (sh >= SHIELD_MAX) snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
        }
        d.putFloat("odstSh", sh);
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- M7S suppressed SMG: long spray, muzzle climbs, controlled back down ----------
        out.add(AttackDef.of("m7s_smg").range(1.5f, 16).cd(28).weight(4).dur(32).weapon("smg")
                .tick((e, t, n) -> {
                    if (n == 2) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.5f, 1.6f);
                    if (n >= 5 && n <= 24) {
                        double climb = (n - 5) * 0.004;
                        Combat.hitscan(e, mz(e), aim(e, t, 0.05 + climb).add(0, climb, 0).normalize(), 20, 1.3f, 0, TRACER);
                        puff(e); if (n % 2 == 0) brass(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.35f, 2f);
                        if (n % 3 == 0) snd(e, SoundEvents.TRIPWIRE_CLICK_OFF, 0.4f, 2f);
                    }
                    if (n == 28) snd(e, SoundEvents.ARMOR_EQUIP_CHAIN, 0.5f, 1.5f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .88f), fire = win(t, .15f, .17f) * (1 - win(t, .76f, .78f));
                    float climb = -0.22f * win(t, .16f, .77f) * fire;
                    float r = 0.3f * (0.5f + 0.5f * (float) Math.sin(a * 3.3f)) * fire;
                    rifle(p, k, climb, r); stance(p, k);
                    p.rHandZ += 0.04f * (float) Math.sin(a * 4.1f) * fire; p.rHandX -= r * 0.25f;
                    p.headX += 0.12f * k; // cheek down behind the smart-link
                    p.bodyZ += 0.025f * (float) Math.sin(a * 3.7f) * fire;
                    // after the burst: tilt the gun to eye the ammo, left hand slides back
                    float chk = bump(win(t, .78f, 1));
                    p.rHandY += 0.6f * chk; p.headY -= 0.15f * chk; p.lArmX += 0.25f * chk;
                }));

        // ---------- MA5C assault rifle: walked bursts with mag-tap ----------
        out.add(AttackDef.of("ma5c_rifle").range(3, 22).cd(32).weight(3).dur(36).weapon("ar")
                .tick((e, t, n) -> {
                    if ((n >= 6 && n <= 12) || (n >= 17 && n <= 23)) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.045), 24, 2f, 0, TRACER);
                        flash(e); if (n % 2 == 0) brass(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, 1.45f + e.getRandom().nextFloat() * 0.15f);
                    }
                    if (n == 29) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.5f, 1.9f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .9f);
                    float f1 = win(t, 6 / 36f, 6.5f / 36f) * (1 - win(t, 12 / 36f, 13 / 36f));
                    float f2 = win(t, 17 / 36f, 17.5f / 36f) * (1 - win(t, 23 / 36f, 24 / 36f));
                    float fire = f1 + f2, r = 0.45f * Math.abs((float) Math.sin(a * 2.6f)) * fire;
                    rifle(p, k, -0.06f * fire, r); stance(p, k);
                    // shift aim between bursts: torso swings a little, weight rolls to the front foot
                    p.bodyY += 0.12f * bump(win(t, .35f, .5f)); p.headY -= 0.08f * bump(win(t, .35f, .5f));
                    p.lLegX -= 0.12f * fire; p.rootZ -= 0.4f * fire;
                    float tap = bump(win(t, .74f, .86f)); // slap the mag
                    p.lArmX += 0.55f * tap * k; p.lArmY -= 0.25f * tap; p.headX += 0.15f * tap; p.rHandY -= 0.3f * tap;
                }));

        // ---------- M6C/SOCOM: scoped, two-handed precision triple ----------
        out.add(AttackDef.of("m6c_socom").range(5, 30).cd(30).weight(3).dur(32).weapon("pistol")
                .tick((e, t, n) -> {
                    if (n == 5) snd(e, SoundEvents.SPYGLASS_USE, 0.8f, 1.3f);
                    if (n == 12 || n == 18 || n == 24) {
                        float dmg = t != null && t.getHealth() < t.getMaxHealth() * 0.4f ? 9f : 6f; // headshot finisher
                        Combat.hitscan(e, mz(e), aim(e, t, 0.006), 32, dmg, 0, 0xfff0c0);
                        puff(e); brass(e);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.45f, 1.7f); snd(e, SoundEvents.ARROW_SHOOT, 0.6f, 0.6f);
                    }
                    if (n == 28) snd(e, SoundEvents.SPYGLASS_STOP_USING, 0.8f, 1.3f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .88f), zoom = win(t, .12f, .3f) * (1 - win(t, .82f, .92f));
                    float r = kick(t, 12 / 32f, .12f) + kick(t, 18 / 32f, .12f) + kick(t, 24 / 32f, .14f);
                    // Weaver grip: both arms in, right elbow locked, left pulling back
                    p.rArmX = lerp(p.rArmX, -HALF - r * 0.45f, k); p.rArmY = lerp(p.rArmY, 0.1f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
                    p.lArmX = lerp(p.lArmX, -HALF + 0.1f - r * 0.4f, k); p.lArmY = lerp(p.lArmY, 0.55f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
                    p.rHandX -= r * 0.5f; // wrist flip
                    p.bodyY -= 0.12f * k; p.headY += 0.12f * k;
                    p.headX += 0.2f * zoom; p.bodyX += 0.1f * zoom - r * 0.06f; p.rootY -= 0.5f * zoom;
                    p.lLegX = lerp(p.lLegX, -0.25f, k); p.rLegX = lerp(p.rLegX, 0.3f, k); p.rLegZ += 0.06f * k;
                    p.rootZ += r * 0.35f;
                }));

        // ---------- M90 CAWS shotgun: brutal close blast + pump ----------
        out.add(AttackDef.of("m90_shotgun").range(0, 8).cd(30).weight(3).dur(26).weapon("shotgun")
                .tick((e, t, n) -> {
                    if (n == 7) {
                        Vec3 m = mz(e);
                        for (int i = 0; i < 12; i++) Combat.hitscan(e, m, aim(e, t, 0.09), 14, 2.1f, 0, 0xffd080);
                        for (LivingEntity f : Combat.cone(e, 4, 35)) Combat.knock(f, e.position(), 0.9, 0.25);
                        flash(e); flash(e); Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, m, 3, 0.1, 0.02);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 0.7f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.35f, 1.7f);
                    }
                    if (n == 14) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.6f);
                    if (n == 18) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.7f); Fx.dustBurst(L(e), 0xb02020, 0.7f, Combat.muzzle(e, 0.4, 0.5, 0.1), 2, 0.05); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), r = 2f * kick(t, 7 / 26f, .28f), pump = bump(win(t, .52f, .74f));
                    rifle(p, k, 0.04f * (1 - win(t, 0, .27f)), r); stance(p, k);
                    p.rHandX -= r * 0.4f; p.bodyX -= r * 0.18f; p.rootY += r * 0.4f; p.headX -= r * 0.1f;
                    p.rLegX += 0.2f * r; // back foot absorbs the kick
                    p.lArmX += 0.45f * pump * k; p.lArmY -= 0.12f * pump; p.bodyZ += 0.05f * pump; p.headX += 0.06f * pump;
                }));

        // ---------- Brute Shot: three lobbed bouncing grenades, then crack the drum ----------
        out.add(AttackDef.of("brute_shot").range(5, 24).cd(70).weight(2).dur(44).weapon("bruteshot")
                .tick((e, t, n) -> {
                    if (n == 8 || n == 16 || n == 24) {
                        lob(e, t, BRUTE_NADE, mz(e), 0.75, 9f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, mz(e), 4, 0.15, 0.04); flash(e);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.9f); snd(e, SoundEvents.CROSSBOW_SHOOT, 1.2f, 0.5f);
                    }
                    if (n == 31) snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.8f, 0.8f);
                    if (n == 37) snd(e, SoundEvents.CROSSBOW_LOADING_END, 1f, 0.6f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .14f, .92f);
                    float r = 1.6f * (kick(t, 8 / 44f, .12f) + kick(t, 16 / 44f, .12f) + kick(t, 24 / 44f, .14f));
                    heavy(p, k, -0.2f * win(t, .05f, .18f) * (1 - win(t, .62f, .7f)), r); stance(p, k);
                    p.rHandX -= r * 0.4f; p.bodyX -= r * 0.15f; p.headX -= r * 0.08f; p.rootY += r * 0.3f;
                    // reload: drop the muzzle, left hand slams a fresh drum home
                    float rel = win(t, .66f, .72f) * (1 - win(t, .86f, .92f)), slam = bump(win(t, .76f, .86f));
                    p.rHandX += 0.6f * rel; p.headX += 0.25f * rel; p.bodyX += 0.1f * rel;
                    p.lArmX += 0.8f * slam; p.lArmZ -= 0.25f * slam; p.bodyZ -= 0.06f * slam;
                }));

        // ---------- Brute Shot bayonet: low-to-high gutting slash ----------
        out.add(AttackDef.of("brute_shot_blade").range(0, 3.6f).cd(40).weight(2).dur(22).weapon("bruteshot")
                .tick((e, t, n) -> {
                    if (n == 4) { Combat.lunge(e, 0.55, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.7f); }
                    if (n == 9) {
                        Fx.arc(L(e), e.position().add(0, 1.1, 0), e.getYRot(), 1.6f, -60, 40, -70, 0xd8d8d8, 0.9f, 12);
                        for (LivingEntity f : Combat.cone(e, 3.6, 60)) {
                            Combat.damage(e, f, 11); Combat.knock(f, e.position(), 0.6, 0.55);
                            Fx.dustBurst(L(e), BLOOD, 1.2f, mid(f), 10, 0.3); Fx.sparks(L(e), mid(f), 6);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.2f, 0.7f); snd(e, SoundEvents.TRIDENT_HIT, 0.8f, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    // blade dropped low and back -> rising slash across -> blade high, overextended -> recover
                    p.rArmX = track(t, 0, -0.6f, .3f, -0.15f, .45f, -2.3f, .62f, -2.5f, 1, -0.6f);
                    p.lArmX = track(t, 0, -0.8f, .3f, -0.35f, .45f, -2.1f, .62f, -2.3f, 1, -0.8f);
                    p.rArmY = track(t, 0, -0.3f, .3f, -0.55f, .45f, 0.2f, 1, -0.3f);
                    p.lArmY = track(t, 0, 0.6f, .3f, 0.3f, .45f, 0.6f, 1, 0.6f);
                    p.rHandX = track(t, 0, -0.3f, .3f, 0.4f, .45f, -0.6f, .7f, -0.4f, 1, -0.3f);
                    p.bodyY = track(t, 0, 0, .3f, -0.5f, .45f, 0.45f, .62f, 0.55f, 1, 0); p.headY = -p.bodyY * 0.55f;
                    p.bodyX = track(t, 0, 0, .3f, 0.35f, .45f, -0.2f, .62f, -0.25f, 1, 0);
                    p.headX = track(t, 0, 0, .3f, 0.2f, .5f, -0.2f, 1, 0);
                    p.rootY = track(t, 0, 0, .3f, -1.4f, .45f, 0.4f, .62f, 0.2f, 1, 0);
                    p.rootZ = track(t, 0, 0, .18f, 0.6f, .45f, -2.6f, .7f, -2.2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, .7f, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.35f, .45f, 0.55f, 1, 0);
                }));

        // ---------- Spartan-grade haymaker (bare-fist melee) ----------
        out.add(AttackDef.of("odst_haymaker").range(0, 3.2f).cd(22).weight(3).dur(20).weapon("none")
                .tick((e, t, n) -> {
                    if (n == 5) { Combat.lunge(e, 0.6, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 1.4f); }
                    if (n == 8) {
                        Fx.arc(L(e), e.position().add(0, 1.45, 0), e.getYRot(), 1.3f, 80, -20, 10, 0xffffff, 0.8f, 10);
                        for (LivingEntity f : Combat.cone(e, 3.4, 45)) {
                            Combat.damage(e, f, 12); Combat.knock(f, e.position(), 1.5, 0.45);
                            Fx.sparks(L(e), mid(f), 10); Fx.burst(L(e), ParticleTypes.POOF, mid(f), 4, 0.15, 0.05);
                        }
                        snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.3f, 0.6f); snd(e, SoundEvents.IRON_GOLEM_ATTACK, 0.8f, 1.1f);
                    }
                })
                .anim((p, t, a) -> {
                    // wind: torso coils right, fist cocked by the ear, guard hand out -> unload -> overrotate -> reset
                    p.rArmX = track(t, 0, -0.4f, .3f, -1.1f, .4f, -1.75f, .55f, -1.6f, 1, -0.4f);
                    p.rArmZ = track(t, 0, 0.1f, .3f, 0.9f, .4f, -0.1f, .55f, -0.35f, 1, 0.1f);
                    p.rArmY = track(t, 0, 0, .3f, 0.3f, .4f, -0.4f, 1, 0);
                    p.lArmX = track(t, 0, -0.5f, .3f, -1.3f, .4f, -0.3f, .6f, -0.2f, 1, -0.5f);
                    p.lArmZ = track(t, 0, -0.1f, .3f, -0.2f, .4f, -0.6f, 1, -0.1f);
                    p.bodyY = track(t, 0, 0, .3f, -0.7f, .4f, 0.65f, .55f, 0.75f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .3f, -0.1f, .42f, 0.35f, .6f, 0.3f, 1, 0); p.headX = -p.bodyX * 0.4f;
                    p.bodyZ = track(t, 0, 0, .3f, 0.08f, .42f, -0.1f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, .7f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .3f, 0.2f, .42f, 0.6f, .7f, 0.45f, 1, 0);
                    p.rootZ = track(t, 0, 0, .3f, 0.6f, .42f, -2.8f, .65f, -2.4f, 1, 0);
                    p.rootY = track(t, 0, 0, .3f, -0.6f, .42f, -1.0f, 1, 0);
                    p.rootYaw = track(t, 0, 0, .3f, -0.12f, .42f, 0.15f, 1, 0);
                }));

        // ---------- Rifle bash: swing the MA5C stock through the target ----------
        out.add(AttackDef.of("rifle_bash").range(0, 3).cd(18).weight(3).dur(16).weapon("ar")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 1.6f);
                    if (n == 6) {
                        for (LivingEntity f : Combat.cone(e, 3.2, 50)) {
                            Combat.damage(e, f, 8); Combat.knock(f, e.position(), 1.1, 0.3); Fx.sparks(L(e), mid(f), 7);
                        }
                        snd(e, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.7f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    p.rArmX = track(t, 0, -1.2f, .25f, -1.0f, .4f, -1.6f, .6f, -1.5f, 1, -1.2f);
                    p.lArmX = track(t, 0, -1.4f, .25f, -1.1f, .4f, -1.7f, .6f, -1.6f, 1, -1.4f);
                    p.rArmY = track(t, 0, -0.3f, .25f, -0.75f, .4f, 0.45f, .6f, 0.55f, 1, -0.3f);
                    p.lArmY = track(t, 0, 0.5f, .25f, 0.1f, .4f, 0.95f, 1, 0.5f);
                    p.rHandZ = track(t, 0, 0, .25f, 0.5f, .4f, -1.0f, .6f, -1.1f, 1, 0); // stock leads the swing
                    p.bodyY = track(t, 0, 0, .25f, -0.55f, .4f, 0.6f, .6f, 0.7f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyX = track(t, 0, 0, .25f, -0.1f, .4f, 0.3f, 1, 0);
                    p.lLegX = track(t, 0, 0, .25f, -0.4f, .7f, -0.4f, 1, 0); p.rLegX = track(t, 0, 0, .4f, 0.45f, .7f, 0.3f, 1, 0);
                    p.rootZ = track(t, 0, 0, .25f, 0.5f, .45f, -2.2f, 1, 0); p.rootY = -0.5f * bump(win(t, .2f, .8f));
                }));

        // ---------- Assassination (from behind): knee kick, hammer-fist, silenced pistol finish ----------
        out.add(AttackDef.of("odst_assassination").range(0, 3.5f).lowHealthTarget(0.3f).weight(6).cd(80).dur(40).weapon("none")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.5, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.8f); }
                    hold(e, t, 1.0, n >= 7 ? -0.35 : 0);
                    if (n == 6 && t != null) { Combat.damage(e, t, Math.max(0, t.getHealth() * 0.3f)); snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.7f); Fx.sparks(L(e), t.position().add(0, 0.4, 0), 6); }
                    if (n == 17 && t != null) {
                        Combat.damage(e, t, Math.max(0, t.getHealth() - 1)); snd(e, SoundEvents.ANVIL_LAND, 0.6f, 1.6f);
                        Fx.dustBurst(L(e), BLOOD, 1.3f, mid(t), 10, 0.3);
                    }
                    if (n == 24) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.7f, 1.5f);
                    if (n == 29 && t != null) {
                        Fx.dustBurst(L(e), BLOOD, 1.5f, mid(t), 26, 0.35);
                        Combat.damage(e, t, t.getHealth() + 100);
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.6f, 1.6f); snd(e, SoundEvents.ARROW_HIT, 1f, 0.6f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponR = t > .58f ? "pistol" : null;
                    // 1) kick behind the knee
                    p.rLegX = track(t, 0, 0, .08f, 0.5f, .15f, -1.2f, .22f, -0.2f, .3f, 0.3f, .85f, 0.3f, 1, 0);
                    p.lLegX = track(t, 0, 0, .15f, 0.1f, .3f, -0.45f, .85f, -0.45f, 1, 0);
                    p.lArmX = track(t, 0, 0, .1f, -1.6f, .3f, -1.4f, .45f, -2.6f, .7f, -1.2f, .9f, -1.1f, 1, 0); // grabs the shoulder
                    // 2) two-handed hammer fist to the back of the skull
                    p.rArmX = track(t, 0, 0, .15f, -0.6f, .3f, -2.9f, .42f, -0.9f, .58f, -0.9f, .66f, -HALF, .9f, -HALF, 1, 0);
                    p.rArmZ = track(t, .3f, 0, .42f, 0.2f, .58f, 0, 1, 0);
                    p.lArmZ = track(t, .3f, 0, .42f, -0.2f, .58f, 0, 1, 0);
                    p.bodyX = track(t, 0, 0, .15f, -0.25f, .3f, -0.2f, .43f, 0.55f, .58f, 0.4f, .72f, 0.35f, 1, 0);
                    p.headX = track(t, .3f, 0, .43f, 0.4f, .72f, 0.45f, 1, 0);
                    p.rootY = track(t, .3f, 0, .43f, -1.4f, .72f, -1.2f, 1, 0);
                    // 3) draw the M6C, point blank, recoil
                    float r = 1.4f * kick(t, 29 / 40f, .14f);
                    p.rHandX = -r * 0.5f; p.rArmX -= r * 0.3f; p.bodyY = 0.1f * win(t, .6f, .7f) * (1 - win(t, .9f, 1));
                    p.headY = -p.bodyY * 0.5f;
                    p.rootZ = track(t, 0, 0, .1f, -1.8f, .9f, -1.8f, 1, 0) + r * 0.4f;
                }));

        // ---------- M9 frag grenade (overhand lob, SMG kept in the right hand) ----------
        out.add(AttackDef.of("frag_grenade").range(5, 18).cd(150).weight(2).dur(22).weapons("smg", "frag")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.4f);
                    if (n == 11) { lob(e, t, FRAG, Combat.muzzle(e, 0.6, -0.4, 0.45), 0.75, 14f); snd(e, SoundEvents.WITCH_THROW, 1, 0.8f); }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .52f ? "frag" : null;
                    p.rArmX = -0.65f; p.rArmY = -0.35f; p.rHandX = -0.3f;
                    p.lArmX = track(t, 0, 0, .15f, -0.6f, .42f, 0.9f, .52f, -2.5f, .72f, -1.3f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .42f, -0.55f, .52f, -0.1f, 1, 0);
                    p.bodyY = track(t, 0, 0, .42f, -0.6f, .55f, 0.5f, .78f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .42f, -0.25f, .55f, 0.35f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.35f, .55f, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, .42f, 0.3f, .55f, 0.5f, 1, 0);
                    p.rootZ = track(t, .4f, 0, .55f, -1.5f, 1, 0); p.rootY = -0.5f * bump(win(t, .4f, .8f));
                }));

        // ---------- Plasma grenade (prime it glowing, then a whipping sidearm throw; sticks) ----------
        out.add(AttackDef.of("plasma_grenade").range(4, 16).cd(170).weight(2).dur(26).weapons("smg", "plasma_nade")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.BEACON_ACTIVATE, 0.7f, 2f);
                    if (n > 3 && n < 13) Fx.dustBurst(L(e), PLASMA_C, 0.8f, Combat.muzzle(e, 0.3, -0.45, 0.2), 2, 0.08);
                    if (n == 14) { lob(e, t, PLASMA_NADE, Combat.muzzle(e, 0.6, -0.4, 0.3), 0.85, 6f); snd(e, SoundEvents.WITCH_THROW, 1, 1.3f); }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .55f ? "plasma_nade" : null;
                    p.rArmX = -0.6f; p.rArmY = -0.3f; p.rHandX = -0.35f;
                    // prime: bring the nade in front of the visor, then wind out to the side
                    p.lArmX = track(t, 0, 0, .14f, -1.4f, .38f, -1.3f, .48f, -1.1f, .56f, -1.5f, .75f, -0.8f, 1, 0);
                    p.lArmY = track(t, 0, 0, .14f, -0.4f, .38f, -0.3f, .48f, 1.1f, .56f, -0.6f, .75f, -0.4f, 1, 0);
                    p.lArmZ = track(t, .38f, 0, .48f, -0.9f, .56f, 0.2f, 1, 0);
                    p.headX = track(t, 0, 0, .14f, 0.25f, .38f, 0.25f, .5f, 0, 1, 0);
                    p.bodyY = track(t, 0, 0, .38f, 0.05f, .48f, 0.55f, .58f, -0.5f, .78f, -0.35f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyZ = track(t, .38f, 0, .48f, -0.1f, .58f, 0.08f, 1, 0);
                    p.bodyX = 0.03f * (float) Math.sin(a * 3) * win(t, .14f, .38f) * (1 - win(t, .38f, .4f)); // jittery hand on a live charge
                    p.lLegX = track(t, .38f, 0, .48f, -0.2f, .6f, -0.5f, 1, 0); p.rLegX = track(t, .38f, 0, .6f, 0.4f, 1, 0);
                    p.rootZ = track(t, .45f, 0, .6f, -1.2f, 1, 0); p.rootYaw = track(t, .38f, 0, .48f, 0.15f, .6f, -0.15f, 1, 0);
                }));

        // ---------- VISR: tap the helmet, sweep the area, outline every hostile ----------
        out.add(AttackDef.of("visr_scan").range(0, 40).noLos().cd(400).weight(2).dur(26).weapon("smg")
                .when((e, t) -> Combat.around(e, e.position(), 28).size() >= 2)
                .tick((e, t, n) -> {
                    if (n == 5) { snd(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 2f); Fx.dustBurst(L(e), VISOR, 1f, e.position().add(0, 1.7, 0), 10, 0.2); }
                    if (n == 10) {
                        for (LivingEntity f : Combat.around(e, e.position(), 28)) {
                            f.addEffect(new MobEffectInstance(MobEffects.GLOWING, 240, 0));
                            Fx.ring(L(e), f.position().add(0, 0.1, 0), 0.8f, 0xff4040, 0.9f, 12);
                        }
                        Fx.ring(L(e), e.position().add(0, 0.2, 0), 4f, VISOR, 1f, 40); Fx.ring(L(e), e.position().add(0, 0.2, 0), 8f, VISOR, 1f, 60);
                        snd(e, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.6f);
                    }
                })
                .anim((p, t, a) -> {
                    float tap = win(t, .05f, .2f) * (1 - win(t, .35f, .5f));
                    p.rArmX = -0.6f; p.rArmY = -0.3f; p.rHandX = -0.3f;
                    p.lArmX = lerp(0, -2.6f, tap); p.lArmZ = lerp(0, 0.4f, tap); p.lArmY = lerp(0, 0.3f, tap); // two fingers to the helmet
                    p.headZ = -0.15f * tap;
                    // sweep left -> right
                    float sw = (float) Math.sin(win(t, .3f, .9f) * PI * 2) * win(t, .25f, .35f) * (1 - win(t, .85f, .95f));
                    p.headY = 0.6f * sw; p.bodyY = 0.25f * sw; p.headX = 0.1f * win(t, .3f, .4f) * (1 - win(t, .85f, .95f));
                    p.lLegX = -0.1f * sw; p.rLegX = 0.1f * sw; p.rootY = -0.3f * win(t, .25f, .35f) * (1 - win(t, .9f, 1));
                }));

        // ---------- Health pack: crouch, slap a med-kit to the chest ----------
        out.add(AttackDef.of("health_pack").range(0, 40).noLos().noFace().cd(360).weight(5).dur(30).weapons("none", "medkit")
                .when((e, t) -> e.getHealth() < e.getMaxHealth() * 0.45f)
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.ARMOR_EQUIP_LEATHER, 1f, 1.2f);
                    if (n == 15) {
                        e.heal(14);
                        Fx.dustBurst(L(e), 0xffffff, 1f, e.position().add(0, 1.1, 0), 14, 0.35);
                        Fx.dustBurst(L(e), 0xe03030, 1f, e.position().add(0, 1.1, 0), 8, 0.3);
                        snd(e, SoundEvents.PLAYER_LEVELUP, 0.5f, 1.8f); snd(e, SoundEvents.BREWING_STAND_BREW, 0.8f, 1.4f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f), slap = bump(win(t, .4f, .58f));
                    p.lArmX = track(t, 0, 0, .15f, 0.4f, .35f, -0.9f, .5f, -1.3f, .75f, -1.1f, 1, 0);
                    p.lArmY = track(t, 0, 0, .35f, 0.7f, .5f, 0.9f, 1, 0);
                    p.rArmX = track(t, 0, 0, .3f, -0.4f, .5f, -1.0f, .75f, -0.9f, 1, 0); p.rArmY = track(t, 0, 0, .4f, -0.6f, 1, 0);
                    p.bodyX = 0.3f * k + 0.15f * slap; p.headX = 0.35f * k;
                    p.rootY = -1.8f * k; p.lLegX = -0.6f * k; p.rLegX = 0.7f * k; p.rLegZ = 0.1f * k; // half-kneel
                    p.bodyZ = -0.06f * slap; p.headZ = 0.04f * (float) Math.sin(a * 0.5f) * k; // catching breath
                }));
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat under = Mat.of(0x1e2124).noise(.1f), plate = Mat.of(0x3b4148).bevel(.32f).noise(.08f), dark = Mat.of(0x272b30).noise(.08f);
        Mat steel = Mat.of(0x5c6168).noise(.06f), pouch = Mat.of(0x3a3a30).noise(.14f), visor = Mat.of(VISOR).glow();
        Mat skin = Mat.of(0xb98a6a);
        Mat chest = Mat.of(0x3b4148).bevel(.3f).noise(.07f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            int cx = x + w / 2; // white stencil: downward drop-pod arrow
            for (int i = 0; i < 3; i++) { g.px(cx - 2 + i, y + 1 + i, 0xd8d8d8); g.px(cx + 1 - i, y + 1 + i, 0xd8d8d8); }
            g.rect(x + 1, y + h - 2, w - 2, 1, 0x23272b);
        });
        Mat sarge = Mat.of(0x3b4148).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.RIGHT && f != Mat.Face.LEFT) return;
            for (int i = 0; i < 3; i++) g.rect(x + 1, y + 1 + i * 2, w - 2, 1, 0xd8b030); // gunnery sergeant chevrons
        });
        Mat helm = Mat.of(0x353a40).bevel(.3f).noise(.07f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) { g.rect(x + 1, y + h / 2, w - 2, 1, 0x1a1d20); g.px(x + w - 2, y + 2, VISOR); }
        });
        // torso
        rig.box(rig.torso, under, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4, -12, -3, 8, 6, 1, .3f);
        rig.box(rig.torso, plate, -3.5f, -6, -2.8f, 7, 2, 1);
        for (int i = 0; i < 3; i++) rig.box(rig.torso, pouch, -3.5f + i * 2.5f, -4, -3.4f, 2, 2, 1); // ammo pouches
        rig.box(rig.torso, dark, -4.5f, -2, -2.5f, 9, 2, 5);            // belt
        rig.box(rig.torso, steel, -1, -2, -3, 2, 2, 1);                  // buckle
        rig.box(rig.torso, pouch, 3.2f, -2.5f, -1.5f, 2, 3, 3);          // hip pouch
        rig.box(rig.torso, dark, -4, -13, -2.5f, 8, 1, 5);               // collar
        rig.box(rig.torso, plate, -3.5f, -11.5f, 2, 7, 9, 2);            // back plate / pack
        rig.box(rig.torso, dark, -3, -10, 4, 6, 5, 1);                   // radio pack (squad leader)
        rig.box(rig.torso, steel, 2, -16, 3.5f, 1, 6, 1);                // whip antenna
        // helmet: rounded ODST shell, wide blue visor, rebreather jaw, rear guard
        rig.box(rig.head, helm, -4, -8, -4, 8, 8, 8, .5f);
        rig.box(rig.head, visor, -3.5f, -6, -5, 7, 2, 1);
        rig.box(rig.head, dark, -4, -6.5f, -5.2f, 8, 1, 1);              // visor brow
        rig.box(rig.head, dark, -2.5f, -3.5f, -5.1f, 5, 3, 1);           // rebreather
        rig.box(rig.head, steel, -4.9f, -4, -3, 1, 2, 3);
        rig.box(rig.head, steel, 3.9f, -4, -3, 1, 2, 3);
        rig.box(rig.head, plate, -1, -9, -3.5f, 2, 1, 7);                // ridge
        rig.box(rig.head, plate, -3.5f, -2, 3.5f, 7, 2, 1);              // neck guard
        rig.box(rig.head, skin, -1, -0.5f, -2, 2, 1, 2);                 // chin peeks out (Buck's stubble)
        // arms: big ODST pauldrons, right one carries the sergeant chevrons
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, under, -2, -2, -2, 4, 12, 4);
            rig.box(arm, s < 0 ? sarge : plate, s < 0 ? -3.5f : -2.5f, -3, -3, 6, 5, 6);
            rig.box(arm, plate, -2, 4, -2, 4, 5, 4, .35f);               // forearm guard
            rig.box(arm, dark, -2, 9, -2, 4, 1, 4, .2f);                 // glove cuff
        }
        rig.box(rig.lArm, visor, -0.5f, 5, -2.6f, 1, 1, 1);              // wrist display
        // legs
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, under, -2, 0, -2, 4, 12, 4);
            rig.box(leg, plate, -2, 0.5f, -2.7f, 4, 4, 1);               // thigh plate
            rig.box(leg, steel, -1.5f, 5, -3, 3, 2, 1);                  // knee pad
            rig.box(leg, dark, -2, 9, -3, 4, 3, 5, .25f);                // boot
        }
        rig.box(rig.rLeg, pouch, -3.2f, 2, -1, 1, 3, 2);                 // thigh holster
        buildWeapons(rig, steel, dark);
    }

    private static void buildWeapons(Rig rig, Mat steel, Mat dark) {
        Mat gun = Mat.of(0x2e3134).noise(.07f), olive = Mat.of(0x4e5a3a).noise(.08f), black = Mat.of(0x161616);
        Mat red = Mat.of(0xff3030).glow(), blue = Mat.of(VISOR).glow(), bronze = Mat.of(0x6e5532).noise(.14f), blade = Mat.of(0xb8bcc0).bevel(.4f);
        PartDefinition w;
        w = rig.weapon(rig.rHand, "smg");     // M7S: receiver, long suppressor, folding stock, smart-link sight
        rig.box(w, gun, -1.5f, -3, -7, 3, 3, 8); rig.box(w, black, -1, -2.5f, -14, 2, 2, 7);
        rig.box(w, black, -1, 0, -1, 2, 3, 2); rig.box(w, gun, -0.5f, 0, -6, 1, 3, 1);
        rig.box(w, steel, -0.5f, -2.5f, 1, 1, 1, 5); rig.box(w, dark, -1, -4.5f, -5, 2, 1, 3); rig.box(w, red, -0.5f, -4.8f, -5.5f, 1, 1, 1);
        w = rig.weapon(rig.rHand, "ar");      // MA5C: bullpup body, shroud, mag, ammo counter, stock
        rig.box(w, olive, -1.5f, -3.5f, -9, 3, 4, 11); rig.box(w, gun, -1.5f, -3, -14, 3, 3, 5); rig.box(w, black, -0.5f, -2, -16, 1, 1, 2);
        rig.box(w, black, -1, 0.5f, -5, 2, 4, 2); rig.box(w, blue, -1, -4.2f, -3, 2, 1, 1); rig.box(w, olive, -1, -3, 2, 2, 3, 4);
        rig.box(w, black, -1, 0, -1, 2, 3, 2);
        w = rig.weapon(rig.rHand, "pistol");  // M6C/SOCOM: slide, suppressor, scope
        rig.box(w, gun, -1, -2.5f, -7, 2, 2, 7); rig.box(w, black, -1, -2.5f, -11, 2, 2, 4);
        rig.box(w, black, -1, -0.5f, -1, 2, 3, 2); rig.box(w, dark, -0.5f, -4, -5, 1, 1, 3); rig.box(w, red, -0.5f, -4, -5.8f, 1, 1, 1);
        w = rig.weapon(rig.rHand, "shotgun"); // M90: twin tubes, pump, receiver, stock
        rig.box(w, gun, -1.5f, -3, -6, 3, 3, 7); rig.box(w, black, -1, -3, -16, 2, 1, 10); rig.box(w, steel, -1, -2, -14, 2, 1, 8);
        rig.box(w, dark, -1.5f, -2.3f, -12, 3, 2, 4); rig.box(w, gun, -1, -2, 1, 2, 3, 5); rig.box(w, black, -1, 0, -1, 2, 3, 2);
        w = rig.weapon(rig.rHand, "bruteshot"); // Covenant-Brute launcher: drum, spiked top, under-barrel blade
        rig.box(w, bronze, -2, -4, -9, 4, 4, 11); rig.box(w, dark, -1.5f, -3.5f, -14, 3, 3, 5);
        rig.box(w, dark, -2.5f, -2, -6, 5, 3, 3); rig.box(w, blade, -0.5f, 0, -17, 1, 3, 10);
        rig.box(w, steel, -0.5f, -5.5f, -8, 1, 2, 2); rig.box(w, steel, -0.5f, -5.5f, -4, 1, 2, 2); rig.box(w, black, -1, 0, -1, 2, 3, 2);
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, olive, -1, -1, -2, 2, 2, 2); rig.box(w, steel, -0.5f, -1.8f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "plasma_nade");
        rig.box(w, blue, -1, -1, -2, 2, 2, 2); rig.box(w, Mat.of(0x3a4a8a), -1.5f, -0.5f, -2.5f, 3, 1, 3);
        w = rig.weapon(rig.lHand, "medkit");
        rig.box(w, Mat.of(0xe8e8e0).bevel(.3f), -1.5f, -1.5f, -3, 3, 3, 4); rig.box(w, red, -0.5f, -1.8f, -2, 1, 1, 2);
    }
}
