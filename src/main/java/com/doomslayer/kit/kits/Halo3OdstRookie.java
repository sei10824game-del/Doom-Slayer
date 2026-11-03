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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** The Rookie, Halo 3: ODST: night-ops ODST with VISR, suppressed M7S / M6C SOCOM, full UNSC + Covenant pickups. */
public class Halo3OdstRookie extends SlayerKit {
    static final int VISR = 0xffb020, VISR_FOE = 0xff3a20, PLASMA_C = 0x40e070, PLASMA_B = 0x50a0ff, NEEDLE = 0xff50d0, BLOOD = 0x6a0a0a;
    static final float SHIELD = 10f;

    static final ProjStyle SPNKR = ProjStyle.make().color(0xffa040).size(0.45f).life(100).explode(3.5f).trail(ParticleTypes.LARGE_SMOKE);
    static final ProjStyle FRAG = ProjStyle.make().color(0x4a5a3a).size(0.26f).life(34).explode(3.5f).gravity(0.05).bounce().trail(ParticleTypes.SMOKE);
    static final ProjStyle PLASMA_BOLT = ProjStyle.make().color(PLASMA_B).size(0.3f).life(45).trail(ParticleTypes.ELECTRIC_SPARK);
    static final ProjStyle PP_BOLT = ProjStyle.make().color(PLASMA_C).size(0.25f).life(40).trail(ParticleTypes.HAPPY_VILLAGER);
    static final ProjStyle NEEDLE_P = ProjStyle.make().color(NEEDLE).size(0.15f).life(50).homing(0.18f).trail(ParticleTypes.WITCH);
    static final ProjStyle OVERCHARGE = ProjStyle.make().color(PLASMA_C).size(0.75f).life(70).homing(0.22f).trail(ParticleTypes.HAPPY_VILLAGER)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Fx.dustBurst(l, PLASMA_C, 1.8f, pos, 30, 0.6);
                Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, pos, 25, 0.5, 0.3);
                Fx.ring(l, pos, 1.6f, PLASMA_C, 1.3f, 20);
                Fx.sound(l, pos, SoundEvents.BEACON_DEACTIVATE, 1.2f, 1.8f);
                if (p.getOwner() instanceof SlayerEntity o) {
                    Combat.blast(o, pos, 2.2f, 9f, 0.4f, null);
                    for (LivingEntity t : Combat.around(o, pos, 2.5)) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3)); // EMP stun
                }
            });
    static final ProjStyle PLASMA_NADE = ProjStyle.make().color(PLASMA_B).size(0.3f).life(30).gravity(0.04).homing(0.12f).trail(ParticleTypes.SOUL_FIRE_FLAME)
            .onHit((p, pos, hit) -> {
                ServerLevel l = (ServerLevel) p.level();
                Vec3 c = hit != null ? hit.position().add(0, hit.getBbHeight() * 0.5, 0) : pos; // sticks to whoever it hits
                Fx.explosion(l, c, 2.5f);
                Fx.dustBurst(l, PLASMA_B, 2.2f, c, 40, 1.2);
                Fx.ring(l, c, 2.4f, 0x80c0ff, 1.6f, 28); Fx.ring(l, c.add(0, 0.4, 0), 1.5f, 0xffffff, 1.2f, 20);
                Fx.burst(l, ParticleTypes.SOUL_FIRE_FLAME, c, 30, 0.8, 0.12);
                Fx.sound(l, c, SoundEvents.GENERIC_EXPLODE, 1.6f, 1.3f); Fx.sound(l, c, SoundEvents.BEACON_DEACTIVATE, 1f, 2f);
                if (p.getOwner() instanceof SlayerEntity o) {
                    if (hit instanceof LivingEntity le && Combat.isFoe(o, le)) Combat.damage(o, le, 14);
                    Combat.blast(o, c, 3.2f, 10f, 0.9f, null);
                }
            });

    public Halo3OdstRookie() {
        super("halo3_odst_rookie", "The Rookie", "ルーキー", "Halo 3: ODST");
        eggPrimary = 0x24282e; eggSecondary = VISR;
        maxHealth = 70; speed = 0.31; armor = 9; knockbackRes = 0.3;
        scale = 1.0f; width = 0.6f; preferredRange = 10f; dmgMult = 1f;
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
    static Vec3 endOf(SlayerEntity e, LivingEntity t, Vec3 dir, double range) { return t != null ? mid(t) : mz(e).add(dir.scale(range)); }
    static void flash(SlayerEntity e, int rgb) {
        Vec3 m = mz(e);
        Fx.burst(L(e), ParticleTypes.FLAME, m, 2, 0.04, 0.02);
        Fx.dustBurst(L(e), rgb, 1.1f, m, 3, 0.06);
    }
    /** suppressed shot: no flame, a puff of smoke and a dull cough */
    static void suppressed(SlayerEntity e, float pitch) {
        Fx.burst(L(e), ParticleTypes.SMOKE, mz(e), 1, 0.02, 0.01);
        snd(e, SoundEvents.CROSSBOW_SHOOT, 0.45f, pitch); snd(e, SoundEvents.ARROW_HIT, 0.25f, 1.9f);
    }
    static void shell(SlayerEntity e) { Fx.burst(L(e), ParticleTypes.SMOKE, Combat.muzzle(e, 0.35, 0.5, 0.1), 1, 0.03, 0.03); }
    static void hold(SlayerEntity e, LivingEntity t, double dist) {
        if (t == null || !Combat.isFoe(e, t)) return;
        Vec3 f = Vec3.directionFromRotation(0, e.getYRot());
        Vec3 p = e.position().add(f.scale(dist + t.getBbWidth() * 0.5));
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(Vec3.ZERO); t.teleportTo(p.x, p.y, p.z); t.hurtMarked = true;
    }
    /** VISR outline: amber box-corner brackets around a target */
    static void outline(ServerLevel l, LivingEntity t, int rgb) {
        double w = t.getBbWidth() * 0.6 + 0.1, h = t.getBbHeight();
        Vec3 b = t.position();
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            Vec3 c = b.add(sx * w, 0, sz * w);
            Fx.beam(l, c, c.add(0, h, 0), rgb, 0.6f, 0.35);
        }
        Fx.ring(l, b.add(0, h + 0.15, 0), (float) w, rgb, 0.7f, 10);
    }

    // ================= helpers (anim) =================
    static float ready(float t, float in, float out) { return win(t, 0, in) * (1 - win(t, out, 1)); }
    /** shouldered two-hander; recoil pushes the body back and climbs the muzzle */
    static void rifle(Pose p, float k, float pitch, float recoil) {
        float tw = 0.22f;
        p.rArmX = lerp(p.rArmX, -HALF + pitch - recoil * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.12f - tw, k); p.rArmZ = lerp(p.rArmZ, 0, k);
        p.lArmX = lerp(p.lArmX, -HALF + pitch + 0.06f - recoil * 0.22f, k); p.lArmY = lerp(p.lArmY, 0.48f - tw, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyY += tw * k; p.headY -= tw * k; p.bodyX += (recoil * 0.1f - pitch * 0.2f) * k; p.headX -= recoil * 0.07f * k;
        p.rHandX -= recoil * 0.3f * k; p.rootZ += recoil * 0.8f * k;
    }
    /** compact SMG hold: stock tucked, elbows in, slightly hunched (night-ops CQB) */
    static void cqb(Pose p, float k, float recoil) {
        p.rArmX = lerp(p.rArmX, -1.35f - recoil * 0.25f, k); p.rArmY = lerp(p.rArmY, -0.3f, k); p.rArmZ = lerp(p.rArmZ, 0.05f, k);
        p.lArmX = lerp(p.lArmX, -1.4f - recoil * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.55f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
        p.bodyX += 0.14f * k; p.bodyY += 0.18f * k; p.headY -= 0.18f * k; p.headX -= 0.06f * k;
        p.rHandX -= recoil * 0.25f * k; p.rootZ += recoil * 0.5f * k;
    }
    /** shoulder-mounted launcher: everything rides higher, head tilted to the sight */
    static void shoulder(Pose p, float k, float recoil) {
        p.rArmX = lerp(p.rArmX, -1.75f - recoil * 0.15f, k); p.rArmY = lerp(p.rArmY, -0.05f, k); p.rArmZ = lerp(p.rArmZ, 0.25f, k);
        p.lArmX = lerp(p.lArmX, -1.6f - recoil * 0.1f, k); p.lArmY = lerp(p.lArmY, 0.4f, k);
        p.rHandX += 0.25f * k; p.headZ -= 0.15f * k; p.headY -= 0.15f * k; p.bodyY += 0.15f * k;
        p.bodyX -= recoil * 0.2f * k; p.rootZ += recoil * 1.2f * k;
    }
    static void stance(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -0.3f, k); p.rLegX = lerp(p.rLegX, 0.26f, k);
        p.lLegZ -= 0.06f * k; p.rLegZ += 0.08f * k; p.rootY -= 0.4f * k;
    }
    /** right-knee-down kneel (sniper) */
    static void kneel(Pose p, float k) {
        p.lLegX = lerp(p.lLegX, -1.2f, k); p.rLegX = lerp(p.rLegX, 1.0f, k);
        p.rootY -= 3.5f * k; p.bodyX += 0.1f * k;
    }
    /** bring the off hand toward the helmet's VISR toggle */
    static void touchHelmet(Pose p, float k) {
        p.lArmX = lerp(p.lArmX, -2.55f, k); p.lArmY = lerp(p.lArmY, 0.2f, k); p.lArmZ = lerp(p.lArmZ, 0.55f, k);
        p.headZ += 0.12f * k; p.headY += 0.1f * k;
    }

    @Override
    public void idle(Pose p, SlayerEntity e, float age, float m) {
        // M7S at low ready, muzzle down, finger off the trigger
        p.rArmX = -0.75f + p.rArmX * 0.2f; p.rArmY = -0.3f;
        p.lArmX = -0.95f + p.lArmX * 0.2f; p.lArmY = 0.6f; p.lArmZ = 0.05f;
        p.rHandX = 0.35f; p.bodyX += 0.05f;
        p.headY += 0.12f * (float) Math.sin(age * 0.03f) * (1 - m); // scanning the dark
    }

    /** stamina/shield: absorption hearts recharge after not being hit; Resistance while shields are up */
    @Override
    public void passive(SlayerEntity e) {
        ServerLevel l = L(e);
        float abs = e.getAbsorptionAmount();
        boolean calm = e.hurtTime == 0 && e.tickCount - e.getLastHurtByMobTimestamp() > 80;
        if (calm && abs < SHIELD && e.tickCount % 4 == 0) {
            if (abs <= 0.01f) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.7f, 1.6f);
            float n = Math.min(SHIELD, abs + 0.5f);
            e.setAbsorptionAmount(n);
            Fx.dustBurst(l, VISR, 0.6f, e.position().add(0, 1.1, 0), 3, 0.35);
            if (n >= SHIELD) { snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.4f); Fx.ring(l, e.position().add(0, 1, 0), 0.8f, VISR, 1f, 16); }
        }
        if (abs > 0 && e.tickCount % 20 == 0) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, 0, false, false));
        if (e.hurtTime == 9 && abs <= 0) { Fx.burst(l, ParticleTypes.ELECTRIC_SPARK, e.position().add(0, 1.2, 0), 8, 0.3, 0.2); snd(e, SoundEvents.GLASS_BREAK, 0.4f, 1.8f); }
        if (e.tickCount % 30 == 0) Fx.dustBurst(l, VISR, 0.4f, e.position().add(0, e.getBbHeight() * 0.9, 0).add(e.getViewVector(1f).scale(0.3)), 1, 0.02);
    }

    // ================= attacks =================
    @Override
    protected void defineAttacks(List<AttackDef> out) {
        // ---------- Rifle bash: step in, butt-stroke the M7S across the jaw ----------
        out.add(AttackDef.of("rifle_bash").range(0, 3.2f).cd(18).dur(16).weapon("smg")
                .tick((e, t, n) -> {
                    if (n == 2) Combat.lunge(e, 0.35, 0.02);
                    if (n == 6) {
                        Fx.arc(L(e), e.position().add(0, 1.4, 0), e.getYRot(), 1.3f, -60, 40, -20, 0xd0d0d0, 0.7f, 9);
                        for (LivingEntity f : Combat.cone(e, 3.3, 55)) { Combat.damage(e, f, 9); Combat.knock(f, e.position(), 0.8, 0.25); Fx.sparks(L(e), mid(f), 6); }
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.1f, 0.9f); snd(e, SoundEvents.SHIELD_BLOCK, 0.6f, 1.2f);
                    }
                })
                .anim((p, t, a) -> {
                    // gun cocks back over the right shoulder, then the stock drives forward-left
                    p.rArmX = track(t, 0, -1.3f, .3f, -2.3f, .4f, -1.2f, .7f, -1.1f, 1, -0.8f);
                    p.rArmZ = track(t, 0, 0, .3f, 0.4f, .4f, -0.45f, .7f, -0.3f, 1, 0);
                    p.rArmY = track(t, 0, -0.3f, .3f, 0.2f, .4f, -0.7f, 1, -0.3f);
                    p.rHandX = track(t, 0, 0, .3f, 1.1f, .4f, -0.4f, .75f, -0.2f, 1, 0.3f);
                    p.lArmX = track(t, 0, -1.4f, .3f, -1.9f, .4f, -1.2f, 1, -0.9f); p.lArmY = track(t, 0, 0.5f, .4f, 0.1f, 1, 0.5f);
                    p.bodyY = track(t, 0, 0, .3f, -0.45f, .42f, 0.5f, .7f, 0.35f, 1, 0); p.headY = -p.bodyY * 0.6f;
                    p.bodyX = track(t, 0, 0, .3f, -0.1f, .42f, 0.3f, 1, 0); p.headX = track(t, .38f, 0, .45f, 0.15f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.5f, .8f, -0.45f, 1, 0); p.rLegX = track(t, 0, 0, .42f, 0.45f, .8f, 0.3f, 1, 0);
                    p.rootZ = -1.8f * bump(win(t, .25f, .8f)); p.rootY -= 0.5f * bump(win(t, .3f, .9f));
                }));

        // ---------- Assassination: grab from behind, two stock blows, drop them ----------
        out.add(AttackDef.of("assassination").range(0, 3.5f).lowHealthTarget(0.3f).weight(6).cd(80).dur(32).weapon("smg")
                .tick((e, t, n) -> {
                    if (n == 0) { Combat.lunge(e, 0.45, 0.05); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.8f); }
                    hold(e, t, 0.75);
                    if ((n == 12 || n == 20) && t != null) {
                        Combat.damage(e, t, Math.max(0, Math.min(6, t.getHealth() - 1)));
                        Fx.dustBurst(L(e), BLOOD, 1.2f, mid(t), 8, 0.25); Fx.sparks(L(e), mid(t), 5);
                        snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 1.1f, 0.7f + n * 0.01f);
                    }
                    if (n == 26 && t != null) {
                        Fx.dustBurst(L(e), BLOOD, 1.4f, mid(t), 20, 0.35);
                        Combat.damage(e, t, t.getHealth() + 100);
                        snd(e, SoundEvents.BONE_BLOCK_BREAK, 1.2f, 0.7f); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.7f);
                    }
                })
                .anim((p, t, a) -> {
                    // left arm hooks the neck and holds; right arm hammers the stock down twice
                    p.lArmX = track(t, 0, -0.9f, .15f, -1.75f, .85f, -1.6f, 1, -0.9f); p.lArmY = track(t, 0, 0.5f, .15f, 0.75f, .85f, 0.7f, 1, 0.5f);
                    p.lArmZ = track(t, 0, 0, .15f, 0.3f, .85f, 0.3f, 1, 0);
                    p.rArmX = track(t, 0, -1.3f, .25f, -2.8f, .37f, -1.0f, .5f, -2.8f, .62f, -1.0f, .72f, -2.9f, .81f, -0.6f, 1, -0.8f);
                    p.rArmZ = track(t, 0, 0, .25f, 0.35f, .81f, 0.1f, 1, 0);
                    p.rHandX = track(t, 0, 0, .25f, 1.4f, .37f, 0.2f, .5f, 1.4f, .62f, 0.2f, .72f, 1.5f, .81f, 0, 1, 0.3f);
                    p.bodyX = track(t, 0, 0, .15f, 0.2f, .25f, -0.15f, .37f, 0.35f, .5f, -0.1f, .62f, 0.35f, .72f, -0.15f, .81f, 0.55f, 1, 0);
                    p.bodyY = track(t, 0, 0, .15f, -0.25f, .81f, 0.2f, 1, 0); p.headY = -p.bodyY * 0.5f;
                    p.headX = track(t, .7f, 0, .81f, 0.4f, 1, 0);
                    p.rootZ = track(t, 0, 0, .15f, -2.2f, .85f, -2f, 1, 0); p.rootY = track(t, .72f, 0, .81f, -1.2f, 1, 0);
                    p.lLegX = track(t, 0, 0, .15f, -0.45f, .85f, -0.45f, 1, 0); p.rLegX = track(t, 0, 0, .15f, 0.4f, .85f, 0.4f, 1, 0);
                }));

        // ---------- M7S suppressed SMG (sustained spray, can strafe) ----------
        out.add(AttackDef.of("m7s_suppressed_smg").range(1.5f, 15).cd(24).weight(5).dur(28).weapon("smg").moving()
                .tick((e, t, n) -> {
                    if (n >= 4 && n <= 22 && n % 2 == 0) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.07), 18, 1.7f, 0, 0xa0a0a0);
                        suppressed(e, 1.6f + e.getRandom().nextFloat() * 0.3f);
                        if (n % 4 == 0) shell(e);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .88f), fire = win(t, .13f, .15f) * (1 - win(t, .8f, .82f));
                    float r = 0.3f * Math.abs((float) Math.sin(a * PI * 0.5f)) * fire;
                    cqb(p, k, r); stance(p, k * 0.6f);
                    p.rHandZ += 0.04f * (float) Math.sin(a * 2.7f) * fire; // muzzle walk
                    p.bodyZ += 0.02f * (float) Math.sin(a * 1.3f) * fire;
                }));

        // ---------- M6C/SOCOM suppressed pistol: thumb the scope, two placed head shots ----------
        out.add(AttackDef.of("m6c_socom_pistol").range(4, 28).cd(36).weight(3).dur(30).weapon("pistol")
                .tick((e, t, n) -> {
                    if (n == 5) snd(e, SoundEvents.SPYGLASS_USE, 0.8f, 1.3f);
                    if (n == 13 || n == 21) {
                        Vec3 d = aim(e, t, 0.006);
                        if (t != null) d = mid(t).add(0, t.getBbHeight() * 0.3, 0).subtract(mz(e)).normalize(); // aim for the head
                        Combat.hitscan(e, mz(e), d, 30, 7f, 0, 0xc8c8c8); suppressed(e, 1.2f); shell(e);
                        if (t != null) Fx.burst(L(e), ParticleTypes.CRIT, t.getEyePosition(), 6, 0.1, 0.2);
                    }
                    if (n == 26) snd(e, SoundEvents.SPYGLASS_STOP_USING, 0.8f, 1.3f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .88f), zoom = win(t, .12f, .35f) * (1 - win(t, .82f, .92f));
                    float r = kick(t, 13 / 30f, .12f) + kick(t, 21 / 30f, .12f);
                    // weaver stance: right arm locked out, left cups the grip, chin down to the scope
                    p.rArmX = lerp(p.rArmX, -HALF - r * 0.4f, k); p.rArmY = lerp(p.rArmY, 0.1f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
                    p.lArmX = lerp(p.lArmX, -1.45f - r * 0.3f, k); p.lArmY = lerp(p.lArmY, 0.62f, k); p.lArmZ = lerp(p.lArmZ, 0, k);
                    p.rHandX -= r * 0.45f; p.bodyY -= 0.15f * k; p.headY += 0.08f * k;
                    p.headX += 0.18f * zoom; p.headZ -= 0.08f * zoom; p.bodyX += 0.1f * zoom - r * 0.05f; p.rootY -= 0.5f * zoom;
                    p.rootZ += r * 0.4f;
                    p.lLegX = lerp(p.lLegX, -0.25f, k); p.rLegX = lerp(p.rLegX, 0.3f, k);
                }));

        // ---------- VISR: tap the helmet, light up every hostile in amber/red outlines ----------
        out.add(AttackDef.of("visr_mode").range(3, 40).cd(400).weight(2).dur(28).weapon("smg").noLos()
                .tick((e, t, n) -> {
                    if (n == 6) { snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.8f); snd(e, SoundEvents.UI_BUTTON_CLICK.value(), 0.6f, 1.6f); }
                    if (n == 9) {
                        Fx.dustBurst(L(e), VISR, 1.2f, e.getEyePosition().add(e.getViewVector(1f).scale(0.35)), 14, 0.15);
                        for (int i = 0; i < 3; i++) Fx.ring(L(e), e.position().add(0, 0.2 + i * 0.6, 0), 2.5f + i * 3.5f, VISR, 1f, 24 + i * 10);
                        for (LivingEntity f : Combat.around(e, e.position(), 36)) {
                            f.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
                            f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0)); // exposed: nowhere to hide in the dark
                            outline(L(e), f, VISR_FOE);
                        }
                        e.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
                    }
                    if (n == 18 && t != null) outline(L(e), t, VISR);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), tap = win(t, .08f, .25f) * (1 - win(t, .38f, .55f));
                    float press = bump(win(t, .26f, .34f));
                    // rifle drops to a one-handed low carry while the left hand goes to the helmet
                    p.rArmX = lerp(p.rArmX, -0.9f, k); p.rArmY = lerp(p.rArmY, -0.25f, k); p.rHandX += 0.3f * k;
                    touchHelmet(p, tap); p.lArmX += 0.15f * press; p.headZ += 0.08f * press;
                    // then a slow head/torso sweep across the battlefield
                    float sweep = win(t, .45f, .95f);
                    p.headY += 0.55f * (float) Math.sin(sweep * PI * 2) * k; p.bodyY += 0.2f * (float) Math.sin(sweep * PI * 2) * k;
                    p.headX += 0.08f * k;
                    p.lLegX = lerp(p.lLegX, -0.15f, k); p.rLegX = lerp(p.rLegX, 0.1f, k); p.rootY -= 0.3f * k;
                }));

        // ---------- MA5C assault rifle (full-auto, muzzle climb, pulls it back down) ----------
        out.add(AttackDef.of("ma5c_assault_rifle").range(3, 18).cd(32).weight(4).dur(34).weapon("ma5c")
                .tick((e, t, n) -> {
                    if (n >= 6 && n <= 26) {
                        Combat.hitscan(e, mz(e), aim(e, t, 0.05 + (n - 6) * 0.003), 22, 1.4f, 0, 0xffe090);
                        if (n % 2 == 0) { flash(e, 0xffb050); shell(e); }
                        snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.75f, 1.35f + e.getRandom().nextFloat() * 0.2f);
                    }
                    if (n == 29) snd(e, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.5f, 1.8f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .9f), fire = win(t, .17f, .18f) * (1 - win(t, .77f, .78f));
                    float climb = 0.18f * win(t, .18f, .77f) * fire;
                    float r = 0.28f * (0.5f + 0.5f * (float) Math.sin(a * PI)) * fire;
                    rifle(p, k, -climb, r); stance(p, k);
                    p.rHandZ += 0.035f * (float) Math.sin(a * 3.1f) * fire; p.bodyZ += 0.02f * (float) Math.sin(a * 2.3f) * fire;
                    p.lArmX += 0.25f * bump(win(t, .8f, .95f)); // slaps the mag
                }));

        // ---------- M90 shotgun (bark + long pump) ----------
        out.add(AttackDef.of("m90_shotgun").range(0, 7).cd(28).weight(4).dur(26).weapon("m90")
                .tick((e, t, n) -> {
                    if (n == 6) {
                        Vec3 m = mz(e);
                        for (int i = 0; i < 10; i++) Combat.hitscan(e, m, aim(e, t, 0.1), 12, 2.4f, 0, 0xffd070);
                        for (LivingEntity f : Combat.cone(e, 3.5, 35)) Combat.knock(f, e.position(), 0.9, 0.3);
                        flash(e, 0xffa040); flash(e, 0xff7020);
                        snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.7f); snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.3f, 0.75f);
                    }
                    if (n == 12) snd(e, SoundEvents.IRON_DOOR_OPEN, 0.6f, 1.6f);
                    if (n == 17) { snd(e, SoundEvents.IRON_DOOR_CLOSE, 0.6f, 1.7f); shell(e); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .18f, .9f), r = 1.8f * kick(t, 6 / 26f, .25f), pump = bump(win(t, .45f, .7f));
                    rifle(p, k, 0, r); stance(p, k);
                    p.lArmX += 0.4f * pump * k; p.lArmY -= 0.1f * pump; p.bodyZ += 0.05f * pump; p.headX += 0.06f * pump;
                    p.rootY += r * 0.4f; p.bodyX -= r * 0.1f;
                }));

        // ---------- SRS99 sniper rifle: drop to a knee, scope in, one round through ----------
        out.add(AttackDef.of("srs99_sniper").range(12, 48).cd(70).weight(3).dur(40).weapon("srs99")
                .tick((e, t, n) -> {
                    if (n == 6) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.6f, 0.8f);
                    if (n == 12) snd(e, SoundEvents.SPYGLASS_USE, 1f, 0.9f);
                    if (n == 24) {
                        Vec3 d = aim(e, t, 0), m = mz(e);
                        Combat.hitscan(e, m, d, 52, 22f, 2, 0xfff0c0);
                        Fx.beam(L(e), m, endOf(e, t, d, 52), 0xffffff, 0.5f, 0.4);
                        flash(e, 0xffc060); Fx.burst(L(e), ParticleTypes.POOF, m, 4, 0.1, 0.05);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.4f, 0.55f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.9f);
                    }
                    if (n == 30) { shell(e); snd(e, SoundEvents.IRON_TRAPDOOR_OPEN, 0.5f, 1.9f); }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .2f, .9f), kn = win(t, 0, .25f) * (1 - win(t, .85f, 1)), zoom = win(t, .25f, .4f) * (1 - win(t, .78f, .86f));
                    float r = 2.4f * kick(t, 24 / 40f, .2f);
                    rifle(p, k, 0, r); kneel(p, kn);
                    p.headX += 0.22f * zoom; p.headY -= 0.14f * zoom; p.bodyX += 0.12f * zoom;
                    p.bodyZ += 0.012f * (float) Math.sin(a * 0.35f) * zoom; // breath hold
                    p.rHandX -= r * 0.2f; p.bodyX -= r * 0.12f;
                }));

        // ---------- M41 SPNKr: two rockets off the shoulder ----------
        out.add(AttackDef.of("m41_spnkr_rockets").range(7, 30).cd(120).weight(2).dur(36).weapon("spnkr")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.PISTON_EXTEND, 0.7f, 0.9f);
                    if (n == 10 || n == 20) {
                        Vec3 m = Combat.muzzle(e, 1.1, 0.35, 0.25);
                        Combat.shoot(e, SPNKR, m, aim(e, t, 0.02), 1.2, 14f);
                        Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, Combat.muzzle(e, -0.7, 0.35, 0.25), 8, 0.2, 0.05); flash(e, 0xffa040);
                        snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.4f, 0.55f); snd(e, SoundEvents.GHAST_SHOOT, 0.4f, 1.3f);
                    }
                    if (n == 28) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.7f, 0.7f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .18f, .9f), r = 1.7f * (kick(t, 10 / 36f, .2f) + kick(t, 20 / 36f, .2f));
                    shoulder(p, k, r); stance(p, k);
                    p.rootY -= 0.3f * k; p.rLegX += 0.15f * k; // brace the back foot
                    p.lArmX += 0.3f * bump(win(t, .75f, .92f)); // checks the tube
                }));

        // ---------- Plasma pistol: rapid taps ----------
        out.add(AttackDef.of("plasma_pistol").range(3, 16).cd(26).weight(2).dur(22).weapon("plasma_pistol")
                .tick((e, t, n) -> {
                    if (n == 4 || n == 8 || n == 12 || n == 16) {
                        Combat.shoot(e, PP_BOLT, mz(e), aim(e, t, 0.04), 1.4, 2.8f);
                        Fx.dustBurst(L(e), PLASMA_C, 0.8f, mz(e), 2, 0.05); snd(e, SoundEvents.BLAZE_SHOOT, 0.4f, 2f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .15f, .85f);
                    float r = kick(t, 4 / 22f, .08f) + kick(t, 8 / 22f, .08f) + kick(t, 12 / 22f, .08f) + kick(t, 16 / 22f, .08f);
                    // one-handed, bladed: off arm tucked to the chest
                    p.bodyY -= 0.35f * k; p.headY += 0.35f * k;
                    p.rArmX = lerp(p.rArmX, -HALF - r * 0.3f, k); p.rArmY = lerp(p.rArmY, 0.35f, k); p.rArmZ = lerp(p.rArmZ, 0, k);
                    p.lArmX = lerp(p.lArmX, -0.5f, k); p.lArmY = lerp(p.lArmY, 0.5f, k); p.lArmZ = lerp(p.lArmZ, 0.1f, k);
                    p.rHandX -= r * 0.25f; p.rootZ += r * 0.3f;
                    p.lLegX = lerp(p.lLegX, 0.25f, k); p.rLegX = lerp(p.rLegX, -0.2f, k);
                }));

        // ---------- Plasma pistol overcharge: hold the trigger, homing EMP bolt ----------
        out.add(AttackDef.of("plasma_pistol_overcharge").range(4, 22).cd(140).weight(2).dur(36).weapon("plasma_pistol")
                .tick((e, t, n) -> {
                    if (n < 22) {
                        Vec3 m = mz(e); double ang = n * 0.8, r = 0.35 - n * 0.012;
                        Fx.dustBurst(L(e), PLASMA_C, 1f, m.add(Math.cos(ang) * r, Math.sin(ang) * r, 0), 2, 0.03);
                        if (n % 4 == 0) snd(e, SoundEvents.BEACON_AMBIENT, 0.7f, 1.2f + n * 0.04f);
                    }
                    if (n == 23) {
                        Combat.shoot(e, OVERCHARGE, mz(e), aim(e, t, 0), 0.9, 10f);
                        Fx.dustBurst(L(e), PLASMA_C, 1.6f, mz(e), 12, 0.2);
                        snd(e, SoundEvents.BLAZE_SHOOT, 1.2f, 0.7f); snd(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 0.6f);
                    }
                    if (n == 27) Fx.burst(L(e), ParticleTypes.CLOUD, mz(e), 6, 0.1, 0.03); // vent heat
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), ch = win(t, .05f, .62f) * (1 - win(t, .63f, .64f)), r = 2f * kick(t, 23 / 36f, .25f);
                    // two-hand braced, gun shaking harder as it charges
                    p.rArmX = lerp(p.rArmX, -HALF - r * 0.35f, k); p.rArmY = lerp(p.rArmY, 0.05f, k);
                    p.lArmX = lerp(p.lArmX, -1.45f - r * 0.25f, k); p.lArmY = lerp(p.lArmY, 0.6f, k);
                    float sh = 0.05f * ch * ch;
                    p.rHandZ += sh * (float) Math.sin(a * 5.3f); p.bodyZ += sh * 0.5f * (float) Math.sin(a * 4.1f);
                    p.bodyX += 0.1f * ch - r * 0.12f; p.headX += 0.1f * ch; p.rootY -= 0.6f * ch; p.rootZ += r * 0.6f; p.rHandX -= r * 0.3f;
                    stance(p, k);
                }));

        // ---------- Plasma rifle: twin-trigger stream, then the vents flare ----------
        out.add(AttackDef.of("plasma_rifle").range(3, 18).cd(36).weight(3).dur(32).weapon("plasma_rifle")
                .tick((e, t, n) -> {
                    if (n >= 3 && n <= 21 && n % 2 == 1) {
                        Combat.shoot(e, PLASMA_BOLT, mz(e), aim(e, t, 0.035), 1.6, 2.6f);
                        Fx.dustBurst(L(e), PLASMA_B, 0.8f, mz(e), 2, 0.05); snd(e, SoundEvents.BLAZE_SHOOT, 0.45f, 1.7f);
                    }
                    if (n == 23) snd(e, SoundEvents.FIRE_EXTINGUISH, 0.8f, 1.2f);
                    if (n > 23 && n < 30) Fx.burst(L(e), ParticleTypes.CLOUD, Combat.muzzle(e, 0.7, 0.3, 0.1), 2, 0.08, 0.03);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .9f), fire = win(t, .09f, .1f) * (1 - win(t, .66f, .68f));
                    float r = 0.3f * (0.5f + 0.5f * (float) Math.sin(a * PI)) * fire, vent = win(t, .7f, .75f) * (1 - win(t, .9f, 1));
                    // Covenant grip is held lower and flatter; overheat: tilts the gun away and turns the face
                    p.rArmX = lerp(p.rArmX, -1.3f - r * 0.3f, k); p.rArmY = lerp(p.rArmY, -0.2f, k);
                    p.lArmX = lerp(p.lArmX, -1.35f - r * 0.2f, k); p.lArmY = lerp(p.lArmY, 0.55f, k);
                    p.bodyY += 0.2f * k; p.headY -= 0.2f * k; p.rootZ += r * 0.5f;
                    p.rHandY += 0.5f * vent; p.headY += 0.45f * vent; p.bodyX -= 0.08f * vent; p.lArmX += 0.3f * vent;
                    stance(p, k);
                }));

        // ---------- Needler: fan of homing crystals that supercombine ----------
        out.add(AttackDef.of("needler").range(4, 20).cd(60).weight(2).dur(30).weapon("needler")
                .tick((e, t, n) -> {
                    if (n >= 5 && n <= 20 && n % 2 == 1) {
                        Combat.shoot(e, NEEDLE_P, mz(e), aim(e, t, 0.12).add(0, 0.05, 0).normalize(), 1.1, 2.2f);
                        snd(e, SoundEvents.AMETHYST_BLOCK_HIT, 0.6f, 1.8f);
                    }
                    if (n == 26 && t != null && t.isAlive() && e.distanceTo(t) < 22) { // supercombine
                        Vec3 c = mid(t);
                        Fx.dustBurst(L(e), NEEDLE, 1.8f, c, 30, 0.5); Fx.burst(L(e), ParticleTypes.WITCH, c, 20, 0.4, 0.2);
                        Combat.blast(e, c, 2.2f, 8f, 0.5f, null);
                        snd(e, SoundEvents.AMETHYST_BLOCK_BREAK, 1.4f, 0.8f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.8f);
                    }
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .9f), fire = win(t, .15f, .17f) * (1 - win(t, .68f, .7f));
                    float r = 0.22f * Math.abs((float) Math.sin(a * PI * 0.5f)) * fire;
                    rifle(p, k, -0.05f, r); stance(p, k);
                    p.rHandZ += 0.06f * (float) Math.sin(a * 0.9f) * fire; // sprays in a fan
                    p.bodyY += 0.06f * (float) Math.sin(a * 0.9f) * fire;
                    p.headX += 0.1f * bump(win(t, .8f, .95f)); // watches the pop
                }));

        // ---------- M9 frag grenade (left-hand overarm) ----------
        out.add(AttackDef.of("m9_frag_grenade").range(5, 18).cd(160).weight(2).dur(22).weapons("smg", "frag")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.TRIPWIRE_CLICK_ON, 1, 1.4f);
                    if (n == 11) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.4);
                        double dist = t != null ? from.distanceTo(mid(t)) : 10;
                        Vec3 d = aim(e, t, 0.02).add(0, 0.15 + dist * 0.018, 0).normalize();
                        Combat.shoot(e, FRAG, from, d, 0.75 + dist * 0.025, 13f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 0.8f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "frag" : null;
                    p.rArmX = -0.8f; p.rArmY = -0.3f; p.rHandX = 0.3f;
                    p.lArmX = track(t, 0, 0, .18f, -0.8f, .42f, 0.9f, .5f, -2.5f, .7f, -1.4f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .42f, -0.5f, .5f, -0.1f, 1, 0);
                    p.bodyY = track(t, 0, 0, .42f, -0.55f, .55f, 0.45f, .75f, 0.3f, 1, 0); p.headY = -p.bodyY * 0.7f;
                    p.bodyX = track(t, 0, 0, .42f, -0.2f, .55f, 0.3f, 1, 0);
                    p.lLegX = track(t, 0, 0, .3f, -0.35f, .55f, -0.5f, 1, 0); p.rLegX = track(t, 0, 0, .42f, 0.3f, .55f, 0.45f, 1, 0);
                    p.rootZ = track(t, .4f, 0, .55f, -1.3f, 1, 0); p.rootY = -0.5f * bump(win(t, .4f, .8f));
                }));

        // ---------- Plasma grenade: prime (glow), underhand side-arm stick ----------
        out.add(AttackDef.of("plasma_grenade_stick").range(4, 15).cd(200).weight(2).dur(26).weapons("smg", "plasma_nade")
                .tick((e, t, n) -> {
                    if (n == 3) snd(e, SoundEvents.BEACON_POWER_SELECT, 0.8f, 2f);
                    if (n < 12) Fx.dustBurst(L(e), PLASMA_B, 0.7f, Combat.muzzle(e, 0.3, -0.45, -0.1), 1, 0.05);
                    if (n == 13) {
                        Vec3 from = Combat.muzzle(e, 0.6, -0.4, 0.2);
                        double dist = t != null ? from.distanceTo(mid(t)) : 9;
                        Vec3 d = aim(e, t, 0.01).add(0, 0.08 + dist * 0.015, 0).normalize();
                        Combat.shoot(e, PLASMA_NADE, from, d, 0.8 + dist * 0.025, 4f);
                        snd(e, SoundEvents.WITCH_THROW, 1, 1.1f); snd(e, SoundEvents.SOUL_ESCAPE, 1, 1.5f);
                    }
                })
                .anim((p, t, a) -> {
                    p.weaponL = t < .5f ? "plasma_nade" : null;
                    p.rArmX = -0.8f; p.rArmY = -0.3f; p.rHandX = 0.3f;
                    // side-arm: wind low and back, whip across the body
                    p.lArmX = track(t, 0, 0, .2f, -0.6f, .45f, 0.5f, .52f, -1.6f, .7f, -1.1f, 1, 0);
                    p.lArmZ = track(t, 0, 0, .2f, -0.2f, .45f, -0.9f, .52f, 0.5f, .75f, 0.3f, 1, 0);
                    p.lArmY = track(t, .45f, 0, .52f, 0.6f, 1, 0);
                    p.bodyY = track(t, 0, 0, .45f, -0.6f, .55f, 0.5f, .8f, 0.25f, 1, 0); p.headY = -p.bodyY * 0.75f;
                    p.bodyX = track(t, 0, 0, .45f, 0.15f, .55f, 0.25f, 1, 0); p.bodyZ = track(t, 0, 0, .45f, 0.12f, .55f, -0.08f, 1, 0);
                    p.lLegX = track(t, 0, 0, .45f, -0.45f, .6f, -0.55f, 1, 0); p.rLegX = track(t, 0, 0, .45f, 0.35f, .6f, 0.4f, 1, 0);
                    p.rootY = -0.9f * bump(win(t, .2f, .8f)); p.rootZ = track(t, .45f, 0, .55f, -1f, 1, 0);
                }));
    }

    // ================= model =================
    @Override
    public void buildModel(Rig rig) {
        Mat under = Mat.of(0x1c1e22).noise(.14f), plate = Mat.of(0x2f343c).bevel(.32f).noise(.09f), dark = Mat.of(0x15171a).noise(.08f);
        Mat grey = Mat.of(0x55585c).noise(.1f), webbing = Mat.of(0x3a3a30).noise(.18f), visor = Mat.of(VISR).glow();
        Mat chest = Mat.of(0x2f343c).bevel(.3f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.FRONT) return;
            g.rect(x + 1, y + h - 3, w - 2, 1, 0x1a1c20); // magazine pouch seams
            g.rect(x + w / 2, y + 1, 1, h - 4, 0x1a1c20);
        });
        Mat pad = Mat.of(0x2f343c).bevel(.35f).decal((m, g, x, y, w, h, f) -> {
            if (f != Mat.Face.RIGHT && f != Mat.Face.LEFT) return; // ODST "feet first into hell" winged-skull, abstracted to a white chevron
            int cx = x + w / 2, cy = y + h / 2;
            g.px(cx, cy - 1, 0xd8d8d8); g.px(cx - 1, cy, 0xd8d8d8); g.px(cx + 1, cy, 0xd8d8d8); g.px(cx - 2, cy + 1, 0xd8d8d8); g.px(cx + 2, cy + 1, 0xd8d8d8);
        });
        Mat helm = Mat.of(0x2b3037).bevel(.4f).noise(.07f).decal((m, g, x, y, w, h, f) -> {
            if (f == Mat.Face.LEFT || f == Mat.Face.RIGHT) g.rect(x + 1, y + h - 3, w - 2, 1, 0x101215);
        });
        // torso: undersuit, chest rig, belt with pouches, ammo pack
        rig.box(rig.torso, under, -4, -12, -2, 8, 12, 4);
        rig.box(rig.torso, chest, -4, -12, -3, 8, 7, 1, .3f);
        rig.box(rig.torso, webbing, -3.5f, -5, -2.9f, 7, 2, 1);
        rig.box(rig.torso, dark, -4.5f, -2.5f, -2.5f, 9, 2, 5);
        rig.box(rig.torso, webbing, 2.5f, -3, -3.2f, 2, 2, 1);
        rig.box(rig.torso, webbing, -4.5f, -3, -3.2f, 2, 2, 1);
        rig.box(rig.torso, plate, -3.5f, -11.5f, 2, 7, 8, 3);
        rig.box(rig.torso, grey, -3, -10.5f, 4.5f, 2, 5, 1);
        rig.box(rig.torso, grey, 1, -10.5f, 4.5f, 2, 5, 1);
        rig.box(rig.torso, dark, -4, -13, -2.5f, 8, 1, 5);
        // helmet: rounded shell, wide wrap visor, rebreather chin, VISR lamp and antenna
        rig.box(rig.head, helm, -4, -8, -4, 8, 8, 8, .5f);
        rig.box(rig.head, visor, -3.5f, -6, -4.9f, 7, 2, 1);
        rig.box(rig.head, visor, -4.8f, -5.5f, -4, 1, 1, 2);
        rig.box(rig.head, dark, -2.5f, -3.5f, -5, 5, 3, 1);
        rig.box(rig.head, grey, -1.5f, -2.5f, -5.6f, 3, 2, 1);
        rig.box(rig.head, plate, -1, -9, -3, 2, 1, 6);
        rig.box(rig.head, grey, 3.8f, -6, -1, 1, 3, 3);
        rig.box(rig.head, dark, 4.2f, -11, 1, 1, 5, 1);
        // arms: big rounded pauldron with emblem, forearm guard
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm;
            rig.box(arm, under, -2, -2, -2, 4, 12, 4);
            rig.box(arm, pad, s < 0 ? -3.5f : -2.5f, -3, -3, 6, 5, 6);
            rig.box(arm, dark, -2, 3, -2, 4, 1, 4, .25f);
            rig.box(arm, plate, -2, 5, -2, 4, 5, 4, .35f);
        }
        // legs: thigh plates with cargo pouches, knee pads, heavy boots
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(leg, under, -2, 0, -2, 4, 12, 4);
            rig.box(leg, plate, -2, 0.5f, -2.7f, 4, 4, 1);
            rig.box(leg, webbing, s < 0 ? -3 : 2, 2, -1, 1, 3, 2);
            rig.box(leg, grey, -1.5f, 5.5f, -3, 3, 2, 1);
            rig.box(leg, dark, -2, 9, -3, 4, 3, 5, .25f);
        }
        buildWeapons(rig, grey, dark);
    }

    private static void buildWeapons(Rig rig, Mat grey, Mat dark) {
        Mat gun = Mat.of(0x2c2e30).noise(.08f), gunL = Mat.of(0x6c7070).noise(.06f), olive = Mat.of(0x4a5038).noise(.1f);
        Mat black = Mat.of(0x121212), ammo = Mat.of(0x40a0ff).glow(), dot = Mat.of(0xff3020).glow();
        Mat cov = Mat.of(0x6a4a9a).bevel(.4f), covG = Mat.of(0x4a5a8a).bevel(.4f), green = Mat.of(PLASMA_C).glow(), blue = Mat.of(PLASMA_B).glow();
        Mat pink = Mat.of(NEEDLE).glow(), needBody = Mat.of(0x5a3a6a).bevel(.4f);
        PartDefinition w;
        // M7S: compact body, long suppressor, folding stock, top sight
        w = rig.weapon(rig.rHand, "smg");
        rig.box(w, gun, -1, -2.5f, -6, 2, 3, 7); rig.box(w, black, -1, -2, -13, 2, 2, 7); rig.box(w, black, -1, 0, -2, 2, 3, 2);
        rig.box(w, gunL, -0.5f, -2, 1, 1, 1, 4); rig.box(w, dark, -0.5f, -4, -4, 1, 1, 3); rig.box(w, dot, -0.5f, -4.4f, -4.5f, 1, 1, 1);
        rig.box(w, gun, -0.5f, 0.5f, -5, 1, 3, 1);
        // M6C/SOCOM: slide, suppressor, KFA-2 sight
        w = rig.weapon(rig.rHand, "pistol");
        rig.box(w, gun, -1, -2, -6, 2, 2, 6); rig.box(w, black, -1, 0, -1, 2, 3, 2); rig.box(w, black, -0.5f, -1.8f, -11, 1, 1, 5);
        rig.box(w, dark, -0.5f, -3.2f, -4, 1, 1, 3); rig.box(w, dot, -0.5f, -3.6f, -4.4f, 1, 1, 1);
        // MA5C: bulky bullpup, ammo counter, shroud
        w = rig.weapon(rig.rHand, "ma5c");
        rig.box(w, olive, -1.5f, -3, -10, 3, 4, 12); rig.box(w, gun, -1, -2.5f, -14, 2, 2, 4); rig.box(w, ammo, -0.5f, -3.4f, -2, 1, 1, 2);
        rig.box(w, black, -1, 1, -6, 2, 3, 2); rig.box(w, dark, -0.5f, -4, -9, 1, 1, 6); rig.box(w, black, -1, 1, -1, 2, 3, 2);
        // M90: long tube, pump, stock
        w = rig.weapon(rig.rHand, "m90");
        rig.box(w, gun, -1.5f, -2.5f, -6, 3, 3, 8); rig.box(w, grey, -1, -3, -16, 2, 1, 10); rig.box(w, black, -1, -1.5f, -13, 2, 2, 6);
        rig.box(w, dark, -1, -1, 2, 2, 2, 4);
        // SRS99: very long barrel, scope, mag, bipod-less stock
        w = rig.weapon(rig.rHand, "srs99");
        rig.box(w, olive, -1, -2.5f, -9, 2, 3, 12); rig.box(w, gun, -0.5f, -2, -21, 1, 1, 12); rig.box(w, black, -1, -5, -7, 2, 2, 7);
        rig.box(w, ammo, -0.5f, -5.3f, -7.4f, 1, 1, 1); rig.box(w, black, -1, 0.5f, -3, 2, 3, 2); rig.box(w, dark, -1.5f, -2.5f, -22, 3, 2, 1);
        // M41 SPNKr: twin tubes, grip, sight block
        w = rig.weapon(rig.rHand, "spnkr");
        rig.box(w, olive, -2, -4, -14, 2, 3, 18); rig.box(w, olive, 0, -4, -14, 2, 3, 18); rig.box(w, dark, -2.5f, -4.5f, -15, 5, 4, 1);
        rig.box(w, black, -0.5f, -1, -2, 1, 3, 2); rig.box(w, dark, 2, -5, -6, 1, 2, 3); rig.box(w, dot, 2, -5.6f, -6, 1, 1, 1);
        // Covenant plasma pistol: purple shell, green emitter
        w = rig.weapon(rig.rHand, "plasma_pistol");
        rig.box(w, cov, -1.5f, -2.5f, -6, 3, 3, 7); rig.box(w, green, -1, -2, -6.6f, 2, 2, 1); rig.box(w, cov, -1, 0.5f, -1, 2, 2, 2);
        rig.box(w, green, -0.5f, -3, -3, 1, 1, 2);
        // plasma rifle: forked prongs, blue vents
        w = rig.weapon(rig.rHand, "plasma_rifle");
        rig.box(w, covG, -2, -3, -7, 4, 4, 8); rig.box(w, covG, -2, -3, -11, 1, 3, 4); rig.box(w, covG, 1, -3, -11, 1, 3, 4);
        rig.box(w, blue, -1, -2.5f, -9, 2, 2, 1); rig.box(w, blue, -2.5f, -2, -4, 1, 2, 3); rig.box(w, blue, 1.5f, -2, -4, 1, 2, 3);
        // needler: hump body, pink crystal row
        w = rig.weapon(rig.rHand, "needler");
        rig.box(w, needBody, -1.5f, -3, -8, 3, 4, 9); rig.box(w, pink, -1, -4.5f, -7, 2, 2, 1); rig.box(w, pink, -1, -5, -5, 2, 2, 1);
        rig.box(w, pink, -1, -4.5f, -3, 2, 2, 1); rig.box(w, needBody, -0.5f, 1, -1, 1, 3, 2);
        // grenades (left hand)
        w = rig.weapon(rig.lHand, "frag");
        rig.box(w, olive, -1, -1, -2, 2, 2, 2); rig.box(w, grey, -0.5f, -1.8f, -1.5f, 1, 1, 1);
        w = rig.weapon(rig.lHand, "plasma_nade");
        rig.box(w, blue, -1, -1, -2, 2, 2, 2); rig.box(w, covG, -1.5f, -0.5f, -1.5f, 3, 1, 1);
    }
}
