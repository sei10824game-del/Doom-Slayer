package com.doomslayer.kit.kits;

import static com.doomslayer.anim.Keys.*;
import static com.doomslayer.kit.kits.KU.*;

import com.doomslayer.anim.Anim;
import com.doomslayer.anim.Pose;
import com.doomslayer.client.Mat;
import com.doomslayer.client.Rig;
import com.doomslayer.entity.ProjStyle;
import com.doomslayer.entity.SlayerEntity;
import com.doomslayer.fx.Combat;
import com.doomslayer.fx.Fx;
import com.doomslayer.kit.AttackDef;
import com.doomslayer.kit.SlayerKit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Every Zelda protagonist (main series, spin-offs, Hyrule Warriors / Age of Calamity). One shared attack library; each kit picks its
 * attack ids and a {@link Look}. Weapon models are generated from whatever weapons the chosen attacks use.
 */
public abstract class ZeldaKits extends SlayerKit {
    static final int FIRE = 0xff7a20, ICE = 0x9fe8ff, LIGHT = 0xfff0a0, ELEC = 0x80d8ff, WIND = 0xc8f0c8, SHEIK = 0x40c8ff, GOLD = 0xf0c840, ANCIENT = 0x30e0ff;

    /** appearance knobs */
    public static class Look {
        int tunic = 0x2f9a3a, under = 0xe8e0c0, hat = -1, hair = 0xc8a050, skin = 0xf0c8a0, belt = 0x7a4a22, boots = 0x6a4a2a, blade = 0xdce4f0, hilt = 0x2a50b0, shield = 0x2a50b0, trim = 0xd0d0d0;
        boolean dress, wolf, bigHead, longHair, noShield, hatless, bandana, ears = true;
        Look tunic(int c) { tunic = c; return this; } Look under(int c) { under = c; return this; } Look hat(int c) { hat = c; return this; }
        Look hair(int c) { hair = c; return this; } Look skin(int c) { skin = c; return this; } Look blade(int c, int h) { blade = c; hilt = h; return this; }
        Look shield(int c, int t) { shield = c; trim = t; return this; } Look dress() { dress = true; longHair = true; hatless = true; noShield = true; return this; }
        Look wolf() { wolf = true; noShield = true; hatless = true; return this; } Look big() { bigHead = true; return this; } Look noShield() { noShield = true; return this; }
        Look hatless() { hatless = true; return this; } Look longHair() { longHair = true; return this; } Look bandana() { bandana = true; hatless = true; return this; }
        Look boots(int c) { boots = c; return this; }
    }

    // ---------- projectile styles ----------
    static ProjStyle shot(int rgb, float size, int life, int pierce, boolean explode, double gravity, java.util.function.BiConsumer<net.minecraft.world.entity.LivingEntity, SlayerEntity> fx) {
        ProjStyle s = ProjStyle.make().color(rgb).size(size).life(life).pierce(pierce).gravity(gravity);
        if (explode) s.explode(3.4f);
        s.onHit((p, pos, hit) -> {
            if (fx != null && hit instanceof LivingEntity le && p.getOwner() instanceof SlayerEntity o) fx.accept(le, o);
            ProjStyle.defaultImpact(p, pos, hit);
        });
        return s;
    }
    static final ProjStyle ARROW_P = ProjStyle.make().color(0xe0d0a0).size(0.22f).life(60).gravity(0.012).pierce(1).shape(ProjStyle.Shape.DART);
    static final ProjStyle ARROW_FIRE = shot(FIRE, 0.25f, 60, 1, false, 0.012, (t, o) -> t.setSecondsOnFire(5)).shape(ProjStyle.Shape.DART);
    static final ProjStyle ARROW_ICE = shot(ICE, 0.25f, 60, 1, false, 0.012, (t, o) -> t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4))).shape(ProjStyle.Shape.DART);
    static final ProjStyle ARROW_SHOCK = shot(ELEC, 0.25f, 60, 2, false, 0.01, (t, o) -> Combat.damage(o, t, 5)).shape(ProjStyle.Shape.DART);
    static final ProjStyle ARROW_LIGHT = shot(LIGHT, 0.3f, 50, 6, false, 0, null).shape(ProjStyle.Shape.BOLT);
    static final ProjStyle ARROW_ANCIENT = shot(ANCIENT, 0.3f, 60, 5, false, 0, null).shape(ProjStyle.Shape.BOLT);
    static final ProjStyle ARROW_BOMB = ProjStyle.make().color(0xd05030).size(0.3f).life(60).gravity(0.014).explode(3f).shape(ProjStyle.Shape.DART);
    static final ProjStyle SEED = ProjStyle.make().color(0xb08040).size(0.15f).life(30).gravity(0.02).shape(ProjStyle.Shape.BOLT);
    static final ProjStyle BEAM = shot(0xc8f0ff, 0.5f, 26, 3, false, 0, null).shape(ProjStyle.Shape.BOLT);
    static final ProjStyle BEAM_BIG = shot(LIGHT, 0.8f, 28, 6, false, 0, null).shape(ProjStyle.Shape.BOLT);
    static final ProjStyle BOOMER = ProjStyle.make().color(0xc08a40).size(0.45f).life(26).pierce(3).homing(0.2f).shape(ProjStyle.Shape.DISC);
    static final ProjStyle BOMB = ProjStyle.make().color(0x303040).size(0.35f).life(32).gravity(0.05).explode(3.6f).bounce().shape(ProjStyle.Shape.GRENADE);
    static final ProjStyle BOMBCHU = ProjStyle.make().color(0xd04040).size(0.3f).life(50).gravity(0.04).homing(0.25f).explode(3f).shape(ProjStyle.Shape.MISSILE);
    static final ProjStyle FIREBALL = shot(FIRE, 0.5f, 50, 0, true, 0.01, (t, o) -> t.setSecondsOnFire(6)).shape(ProjStyle.Shape.ORB);
    static final ProjStyle ICEBALL = shot(ICE, 0.5f, 50, 0, false, 0, (t, o) -> { t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 6)); Combat.damage(o, t, 4); }).shape(ProjStyle.Shape.ORB);
    static final ProjStyle MAGIC = shot(0xb070ff, 0.5f, 50, 1, true, 0, null).shape(ProjStyle.Shape.ORB);
    static final ProjStyle HOMING = ProjStyle.make().color(LIGHT).size(0.3f).life(60).homing(0.2f).shape(ProjStyle.Shape.ORB);
    static final ProjStyle BOULDER = ProjStyle.make().color(0x8a8a80).size(0.9f).life(30).gravity(0.08).explode(3.2f).shape(ProjStyle.Shape.GRENADE);
    static final ProjStyle ZONAI_BOX = ProjStyle.make().color(0xa08050).size(0.8f).life(30).gravity(0.04).explode(3f).shape(ProjStyle.Shape.GRENADE);
    static final ProjStyle SOMARIA = ProjStyle.make().color(0xff6040).size(0.5f).life(40).pierce(2).explode(2f).shape(ProjStyle.Shape.GRENADE);
    static final ProjStyle COCCO = ProjStyle.make().color(0xfff8f0).size(0.35f).life(60).homing(0.3f).explode(1.8f).shape(ProjStyle.Shape.ORB);

    // ---------- kit data ----------
    final String[] atk; final Look look; final boolean fairy; final int fairies;

    ZeldaKits(String id, String name, String ja, String game, Look look, double hp, double speed, float scale, int fairies, String... atk) {
        super(id, name, ja, game);
        this.look = look; this.atk = atk; this.fairies = fairies; this.fairy = fairies > 0;
        eggPrimary = look.dress || look.wolf ? look.tunic : (look.hat >= 0 ? look.hat : look.tunic); eggSecondary = look.hair == 0 ? look.tunic : look.under;
        maxHealth = hp; this.speed = speed; armor = 6; knockbackRes = 0.2; this.scale = scale; preferredRange = 6f; dmgMult = 1f;
        idleWeaponR = "sword"; idleWeaponL = look.noShield ? null : "shield";
    }

    @Override public boolean preventDeath(SlayerEntity e) { // bottled fairy / Mipha's Grace
        var d = e.getPersistentData(); int used = d.getInt("zl_fairy");
        if (used >= fairies) return false;
        d.putInt("zl_fairy", used + 1); e.setHealth(e.getMaxHealth() * 0.6f);
        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4));
        Fx.dustBurst(L(e), 0xffb0e0, 1.2f, e.position().add(0, 1, 0), 30, 0.6); Fx.ring(L(e), e.position().add(0, 1, 0), 2f, 0xffd0f0, 1.5f, 22);
        snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.8f); snd(e, SoundEvents.TOTEM_USE, 0.6f, 1.6f);
        if (e.getOwner() != null) e.getOwner().displayClientMessage(Component.literal(name + ": a fairy heals you! (" + (fairies - used - 1) + " left)"), true);
        return true;
    }
    /** Shield: perfect-parry window first; otherwise hits from the front are blocked (projectiles fully, melee mostly). */
    @Override
    public float modifyDamage(SlayerEntity e, net.minecraft.world.damagesource.DamageSource src, float amt) {
        float r = KU.defend(e, src, amt);
        if (r <= 0 || look.noShield) return r;
        net.minecraft.world.entity.Entity from = src.getDirectEntity() != null ? src.getDirectEntity() : src.getEntity();
        if (from == null || src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)) return r;
        Vec3 to = from.position().subtract(e.position()).multiply(1, 0, 1);
        if (to.lengthSqr() < 1e-4 || to.normalize().dot(e.getViewVector(1f).multiply(1, 0, 1).normalize()) < 0.25) return r; // must come from the front
        float chance = e.isAttacking() ? 0.4f : 0.8f;
        if (e.getRandom().nextFloat() > chance) return r;
        Vec3 c = e.position().add(0, 1.2, 0).add(e.getViewVector(1f).scale(0.8));
        Fx.sparks(L(e), c, 10); Fx.dustBurst(L(e), look.trim, 0.9f, c, 6, 0.2); snd(e, SoundEvents.SHIELD_BLOCK, 1f, 1f + 0.2f * e.getRandom().nextFloat());
        if (from instanceof net.minecraft.world.entity.projectile.Projectile) return 0;
        if (from instanceof LivingEntity a) Combat.knock(a, e.position(), 0.35, 0.05);
        return r * 0.15f;
    }
    @Override public void idle(Pose p, SlayerEntity e, float age, float m) {
        p.rArmX = -0.55f + p.rArmX * 0.5f; p.rArmZ = 0.2f; p.rHandX = 0.7f; p.bodyX += 0.05f; p.bodyY += 0.2f; p.headY -= 0.1f;
        if (!look.noShield) { p.lArmX = -1.15f; p.lArmY = 0.5f; p.lArmZ = 0.3f; p.lHandX = -0.2f; p.bodyY += 0.15f; } // shield held up in front of the chest
    }
    @Override public void passive(SlayerEntity e) { if (e.tickCount % 100 == 0 && e.getHealth() < e.getMaxHealth() && Combat.around(e, e.position(), 8).isEmpty()) e.heal(3); }

    @Override
    protected void defineAttacks(List<AttackDef> out) { for (String id : atk) out.add(build(id)); }

    // ---------- anim kinds ----------
    enum K { SLASH, COMBO, STAB, OVER, SPIN, CAST, BOW, THROW, SLAM, JUMP, DASH, GUARD }
    static Anim anim(K k, float h, int n) {
        return (p, t, a) -> {
            switch (k) {
                case SLASH -> swing(p, t, h, 1f);
                case COMBO -> { float s = (float) Math.sin(t * PI * n); p.rArmX = -1.4f + 0.4f * Math.abs(s); p.rArmZ = 0.9f * s; p.lArmX = -0.5f; p.bodyY = 0.7f * s; p.headY = -0.35f * s; p.lLegX = -0.4f; p.rLegX = 0.3f; p.rootZ = -1f; }
                case STAB -> stab(p, t, h);
                case OVER -> { p.rArmX = track(t, 0, -0.2f, h * .6f, -3.0f, h, -0.5f, h + .25f, -0.8f, 1, -0.2f); p.lArmX = track(t, 0, 0, h * .6f, -2.0f, h, -0.3f, 1, 0);
                    p.bodyX = track(t, 0, 0, h * .6f, -0.3f, h, 0.6f, 1, 0); p.rootY = track(t, 0, 0, h * .6f, 0.4f, h, -1.5f, 1, 0); p.rootZ = -1.8f * bump(win(t, h * .5f, h + .3f)); }
                case SPIN -> { p.rootYaw += 6.28f * win(t, .15f, .8f); p.rArmZ = 1.5f * bump(win(t, .1f, .85f)); p.rArmX = -0.3f; p.lArmZ = -1.1f * bump(win(t, .1f, .85f)); p.rootY -= 0.6f * bump(win(t, .1f, .85f)); }
                case CAST -> { float c = ready(t, .3f, .85f); p.rArmX = lerp(p.rArmX, -1.7f, c); p.lArmX = lerp(p.lArmX, -1.7f, c); p.headX -= 0.2f * c; p.rootZ += 1.2f * kick(t, h, .3f); }
                case BOW -> { float c = ready(t, .15f, .88f); shoulder(p, c, 0, 1.3f * kick(t, h, .25f)); stance(p, c); p.lArmX -= 0.1f * c; }
                case THROW -> throwAnim(p, t);
                case SLAM -> { p.rArmX = track(t, 0, -0.2f, h * .6f, -2.9f, h, -0.4f, 1, -0.2f); p.lArmX = track(t, 0, 0, h * .6f, -2.9f, h, -0.4f, 1, 0); p.bodyX = track(t, 0, 0, h * .6f, -0.4f, h, 0.8f, 1, 0); p.rootY = track(t, 0, 0, h * .6f, 0.5f, h, -2f, 1, 0); }
                case JUMP -> { float up = win(t, .08f, .3f) * (1 - win(t, .38f, .5f)), dn = bump(win(t, .4f, .75f)); p.rArmX = lerp(p.rArmX, -3.0f, up) + 2.2f * dn; p.lLegX = -0.8f * up; p.rLegX = 0.5f * up; p.rootY += 1.6f * up; p.bodyX += 0.6f * dn; }
                case DASH -> { float c = bump(win(t, .1f, .8f)); p.rArmX = -1.5f * c; p.bodyX += 0.9f * c; p.rootY -= 1.5f * c; p.lLegX = -1.1f * c; p.rLegX = 0.9f * c; }
                case GUARD -> { float c = ready(t, .12f, .85f); p.rArmX = lerp(p.rArmX, -1.0f, c); p.rArmZ = lerp(p.rArmZ, 0.3f, c); p.lArmX = lerp(p.lArmX, -1.35f, c); p.lArmZ = lerp(p.lArmZ, 0.4f, c); stance(p, c); p.rootY -= 0.4f * c; }
            }
        };
    }

    // ---------- attack factories ----------
    AttackDef base(String id, String w, double min, double max, int cd, int dur, int weight) {
        return AttackDef.of(id).range((float) min, (float) max).cd(cd).dur(dur).weight(weight).weapons(w, look.noShield || w == null ? null : "shield");
    }
    AttackDef melee(String id, double range, int cd, int dur, int[] hits, float dmg, double kb, double up, int rgb, K k, float frame, int weight) {
        return base(id, "sword", 0, range, cd, dur, weight).tick((e, t, n) -> {
            for (int i = 0; i < hits.length; i++) if (n == hits[i]) {
                boolean r = i % 2 == 0; slash(e, r ? 80 : -70, r ? -70 : 80, k == K.OVER ? 60 : (k == K.STAB ? 0 : 10 + 12 * i), rgb); KU.melee(e, range + 0.1, dmg, kb, up, rgb); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 1.3f + 0.05f * i);
                if (i == 0 && k == K.DASH) Combat.lunge(e, 1.0, 0);
                Fx.burst(L(e), ParticleTypes.CRIT, e.position().add(e.getViewVector(1f).scale(2)).add(0, 1.2, 0), 6, 0.3, 0.25);
            }
            for (int h : hits) if (n == h - 3) Combat.lunge(e, k == K.STAB ? 0.55 : 0.32, 0); // step into the blow
        }).anim(k == K.DASH ? anim(k, frame, 1) : KU.comboAnim(hits, dur, k == K.OVER ? new int[] {2} : k == K.STAB ? new int[] {3} : new int[] {0, 1, 0, 3}));
    }
    AttackDef proj(String id, String w, double min, double max, int cd, int dur, int fire, int count, double spread, ProjStyle st, double speed, float dmg, K k, net.minecraft.sounds.SoundEvent s, float pitch) {
        return base(id, w, min, max, cd, dur, 3).tick((e, t, n) -> {
            if (n == fire) {
                Vec3 d = aim(e, t, 0);
                for (int i = 0; i < count; i++) { double off = count == 1 ? 0 : (i - (count - 1) / 2.0) * spread; Combat.shoot(e, st, mz(e), new Vec3(d.x - d.z * off, d.y, d.z + d.x * off).normalize(), speed, dmg); }
                snd(e, s, 0.9f, pitch);
            }
        }).anim(anim(k, fire / (float) dur, 1));
    }
    AttackDef aoe(String id, String w, double min, double max, int cd, int dur, int fire, double radius, float dmg, double kb, int rgb, K k, java.util.function.BiConsumer<SlayerEntity, LivingEntity> fx, boolean atTarget) {
        return base(id, w, min, max, cd, dur, 2).tick((e, t, n) -> {
            if (n == fire) {
                Vec3 c = atTarget && t != null ? t.position() : e.position();
                for (LivingEntity f : Combat.around(e, c, radius)) { Combat.damage(e, f, dmg); Combat.knock(f, c, kb, 0.25); if (fx != null) fx.accept(e, f); }
                Fx.ring(L(e), c.add(0, 0.3, 0), (float) radius * 0.5f, rgb, 1.6f, 24); Fx.ring(L(e), c.add(0, 0.3, 0), (float) radius, rgb, 1.5f, 32);
                Fx.dustBurst(L(e), rgb, 1.4f, c.add(0, 0.6, 0), 20, radius * 0.4); snd(e, SoundEvents.GENERIC_EXPLODE, 0.5f, 1.4f);
            }
        }).anim(anim(k, fire / (float) dur, 1));
    }
    AttackDef buff(String id, int cd, int dur, int ticks, int rgb, K k) {
        return base(id, "sword", 0, 12, cd, dur, 1).noLos().when((e, t) -> true).tick((e, t, n) -> {
            if (n == 2) { e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ticks, 4, false, true)); Fx.ring(L(e), e.position().add(0, 1, 0), 2.5f, rgb, 1.6f, 26); snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 1.6f); }
            if (n % 4 == 0) Fx.dustBurst(L(e), rgb, 1f, e.position().add(0, 1.1, 0), 5, 0.6);
        }).anim(anim(k, .5f, 1));
    }
    private static final java.util.Random R = new java.util.Random();

    AttackDef build(String id) {
        switch (id) {
            // ---- sword work ----
            case "sword_slash": return melee(id, 3.1, 12, 14, new int[] {5}, 6, 0.4, 0.1, 0xffffff, K.SLASH, 5 / 14f, 3);
            case "sword_combo": return melee(id, 3.2, 40, 26, new int[] {5, 12, 19}, 5, 0.3, 0.1, 0xffffff, K.COMBO, 0, 3);
            case "sword_stab": return melee(id, 3.4, 26, 18, new int[] {7}, 9, 0.6, 0.15, 0xffffff, K.STAB, 7 / 18f, 2);
            case "overhead": return melee(id, 3.3, 40, 22, new int[] {9}, 10, 0.8, 0.3, 0xffffff, K.OVER, 9 / 22f, 2);
            case "rock_breaker": return melee(id, 3.6, 90, 30, new int[] {14}, 16, 1.0, 0.4, 0xc0a070, K.OVER, 14 / 30f, 1);
            case "dash_attack": return melee(id, 3.6, 60, 20, new int[] {8}, 9, 0.8, 0.15, 0xffffff, K.DASH, 0, 2).onStart((e, t, n) -> Combat.lunge(e, 1.0, 0.02));
            case "greatsword_smash": return melee(id, 3.8, 70, 30, new int[] {14}, 15, 1.2, 0.4, 0xffffff, K.SLAM, 14 / 30f, 2);
            case "spear_thrust": return melee(id, 4.4, 24, 20, new int[] {8}, 8, 0.7, 0.1, 0xffffff, K.STAB, 8 / 20f, 3);
            case "rapier_flurry": return melee(id, 3.4, 50, 34, new int[] {5, 9, 13, 17, 21, 25}, 3.2f, 0.1, 0.05, 0xffffff, K.COMBO, 0, 3);
            case "wolf_bite": return melee(id, 2.8, 30, 22, new int[] {4, 9, 14}, 4.5f, 0.3, 0.1, 0xff4040, K.COMBO, 0, 3);
            case "mortal_draw": return melee(id, 4, 160, 26, new int[] {14}, 20, 1.3, 0.3, 0xffd070, K.DASH, 0, 1).onStart((e, t, n) -> Combat.lunge(e, 0.2, 0));
            case "ending_blow": return melee(id, 3.4, 40, 26, new int[] {13}, 22, 0.5, 0.1, 0xff4040, K.STAB, 13 / 26f, 6).lowHealthTarget(0.4f);
            case "shield_bash": return base(id, "sword", 0, 3.2, 60, 18, 2).tick((e, t, n) -> {
                if (n == 6) for (LivingEntity f : Combat.cone(e, 3.2, 55)) { Combat.damage(e, f, 4); Combat.knock(f, e.position(), 1.1, 0.2); f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 5)); Fx.sparks(L(e), mid(f), 6); snd(e, SoundEvents.SHIELD_BLOCK, 1f, 0.7f); }
            }).anim((p, t, a) -> { float c = bump(win(t, .1f, .7f)); p.lArmX = -1.5f * c; p.lArmZ = 0.2f * c; p.bodyX += 0.5f * c; p.rootZ = -1.6f * c; });
            case "jump_slash": return base(id, "sword", 2, 7, 70, 24, 2).tick((e, t, n) -> {
                if (n == 2) { Vec3 f = e.getViewVector(1f); e.setDeltaMovement(f.x * 0.5, 0.6, f.z * 0.5); e.hurtMarked = true; }
                if (n == 12) { slash(e, 0, 0, -80, 0xffffff); KU.melee(e, 3.6, 11, 0.8, 0.3, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1f); }
            }).anim(anim(K.JUMP, 0, 1));
            case "helm_splitter": case "downthrust": return base(id, "sword", 3, 8, 120, 28, 1).tick((e, t, n) -> {
                if (n == 2) { Vec3 f = e.getViewVector(1f); e.setDeltaMovement(f.x * 0.3, 0.95, f.z * 0.3); e.hurtMarked = true; }
                if (n == 12 && t != null) { Vec3 d = t.position().subtract(e.position()).normalize(); e.setDeltaMovement(d.x * 0.8, -1.2, d.z * 0.8); e.hurtMarked = true; }
                if (n == 18) { KU.melee(e, 3.8, 16, 0.8, 0.4, 0xffffff); Fx.ring(L(e), e.position().add(0, 0.2, 0), 2.5f, 0xffffff, 1.5f, 20); snd(e, SoundEvents.ANVIL_LAND, 0.8f, 1.4f); }
            }).anim(anim(K.JUMP, 0, 1));
            case "upthrust": return base(id, "sword", 0, 3.2, 50, 18, 2).tick((e, t, n) -> {
                if (n == 6) { slash(e, 0, 0, -80, 0xffffff); for (LivingEntity f : Combat.cone(e, 3.4, 60)) { Combat.damage(e, f, 9); f.setDeltaMovement(0, 0.8, 0); f.hurtMarked = true; } e.setDeltaMovement(0, 0.4, 0); e.hurtMarked = true; }
            }).anim((p, t, a) -> { float c = win(t, .1f, .4f) * (1 - win(t, .55f, .8f)); p.rArmX = lerp(p.rArmX, -3.0f, c); p.bodyX -= 0.3f * c; });
            case "spin_attack": case "great_spin": {
                boolean big = id.equals("great_spin");
                return base(id, "sword", 0, big ? 4.2 : 3.6, big ? 130 : 80, 28, 3).tick((e, t, n) -> {
                    if (n < 10) Fx.dustBurst(L(e), 0xffffff, 0.8f, e.position().add(0, 1.2, 0), 2, 0.4);
                    if (n == 12 || (big && n == 16)) { slash(e, 359, 0, 0, 0xffffff); for (LivingEntity f : Combat.around(e, e.position().add(0, 1, 0), big ? 4.2 : 3.6)) { Combat.damage(e, f, big ? 12 : 8); Combat.knock(f, e.position(), 0.9, 0.25); Fx.sparks(L(e), mid(f), 5); } snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.9f); }
                }).anim((p, t, a) -> { float c = win(t, .05f, .35f) * (1 - win(t, .8f, 1f)); p.rootYaw += 6.28f * (big ? 2 : 1) * win(t, .35f, .8f); p.rArmZ = 1.4f * c; p.lArmZ = -1.1f * c; p.rArmX = -0.3f; p.rootY -= 0.6f * c; });
            }
            case "hammer_slam": return aoe(id, "hammer", 0, 5, 100, 32, 18, 4.2, 14, 1.4, 0xc0a070, K.SLAM, (e, f) -> f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3)), false);
            case "iron_stomp": return aoe(id, "sword", 0, 4, 80, 22, 10, 3.5, 8, 0.8, 0x808080, K.SLAM, (e, f) -> f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5)), false);
            case "ball_chain": return aoe(id, "ballchain", 0, 5, 120, 36, 20, 4.8, 13, 1.3, 0x909090, K.SPIN, null, false);
            case "four_formation": return base(id, "sword", 0, 6, 120, 32, 2).tick((e, t, n) -> {
                if (n == 8 || n == 13 || n == 18 || n == 23) { int i = (n - 8) / 5; float yaw = e.getYRot() + 90 * i; Fx.arc(L(e), e.position().add(0, 1.3, 0), yaw, 1.8f, 80, -80, 20, new int[] {0x40d040, 0xe04040, 0x4060e0, 0xb040e0}[i], 0.8f, 12);
                    for (LivingEntity f : Combat.around(e, e.position(), 4)) { Combat.damage(e, f, 5); Fx.sparks(L(e), mid(f), 3); } snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.1f + 0.1f * i); }
            }).anim(anim(K.SPIN, 0, 1));
            case "doppelganger": return base(id, "sword", 0, 8, 150, 30, 2).tick((e, t, n) -> {
                if (n == 6 || n == 12 || n == 18) { for (LivingEntity f : Combat.around(e, e.position(), 6)) { Combat.damage(e, f, 5); Fx.beam(L(e), e.position().add(0, 1, 0), mid(f), 0x60e060, 0.5f, 0.3); } Fx.dustBurst(L(e), 0x60e060, 1.2f, e.position().add(0, 1, 0), 10, 0.6); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.5f); }
            }).anim(anim(K.SPIN, 0, 1));
            // ---- defence / movement ----
            case "guard_parry": case "perfect_guard": return base(id, "sword", 0, 7, 50, 24, 3).noLos().tick((e, t, n) -> {
                if (n == 2) { KU.parryWindow(e, 12); snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.8f, 1.6f); }
                if (n == 3 && t != null && e.distanceTo(t) < 4) { /* riposte handled by next attack */ }
            }).anim(anim(K.GUARD, 0, 1));
            case "dodge": case "sidehop": case "backflip": return base(id, "sword", 0, 9, 70, 14, 2).noLos().noFace().tick((e, t, n) -> {
                if (n == 1) { e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 4, false, false)); Vec3 f = e.getViewVector(1f); boolean back = id.equals("backflip"); double s = back ? -0.9 : 0.9; Vec3 d = back ? f : new Vec3(f.z, 0, -f.x); e.setDeltaMovement(d.x * s, back ? 0.35 : 0.08, d.z * s); e.hurtMarked = true; snd(e, SoundEvents.ARMOR_EQUIP_LEATHER, 0.8f, 1.4f); }
                if (n < 8) Fx.burst(L(e), ParticleTypes.CLOUD, e.position().add(0, 0.2, 0), 2, 0.2, 0.01);
            }).anim((p, t, a) -> { float c = bump(win(t, 0, 1)); p.rootRoll += 0.5f * c; p.rootY -= 1.2f * c; p.lLegX = -0.8f * c; p.rLegX = 0.6f * c; });
            case "flurry_rush": return base(id, "sword", 0, 6, 220, 40, 2).tick((e, t, n) -> {
                if (n == 1) { for (LivingEntity f : Combat.around(e, e.position(), 6)) f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 6)); Fx.ring(L(e), e.position().add(0, 1, 0), 4f, 0xa0e0ff, 1.6f, 26); snd(e, SoundEvents.BEACON_ACTIVATE, 0.8f, 0.5f); }
                if (n >= 6 && n < 36 && n % 5 == 1) { slash(e, n % 10 == 6 ? 70 : -70, n % 10 == 6 ? -70 : 70, 15, 0xa0e0ff); for (LivingEntity f : Combat.around(e, e.position(), 4)) { Combat.damage(e, f, 4.5f); Fx.sparks(L(e), mid(f), 4); } snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.7f); }
            }).anim(anim(K.COMBO, 0, 6));
            case "nayru_love": return buff(id, 600, 50, 120, 0x4080ff, K.CAST);
            case "daruk_protection": return buff(id, 500, 40, 90, 0xd06020, K.GUARD);
            case "magic_cape": return buff(id, 450, 40, 80, 0xc040c0, K.CAST).onStart((e, t, n) -> e.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false)));
            case "wall_merge": return base(id, "sword", 2, 8, 120, 24, 2).tick((e, t, n) -> {
                if (n == 1) { e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 14, 4, false, false)); e.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 14, 0, false, false)); Combat.lunge(e, 1.3, 0); }
                if (n == 14) { KU.melee(e, 3.6, 10, 0.7, 0.15, 0xc0a0ff); Fx.dustBurst(L(e), 0xc0a0ff, 1.2f, e.position().add(0, 1, 0), 12, 0.4); }
            }).anim(anim(K.DASH, 0, 1));
            // ---- ranged weapons ----
            case "bow_arrow": return proj(id, "bow", 5, 30, 40, 22, 9, 1, 0, ARROW_P, 2.2, 7, K.BOW, SoundEvents.ARROW_SHOOT, 1.1f);
            case "bow_fire": return proj(id, "bow", 5, 28, 60, 22, 9, 1, 0, ARROW_FIRE, 2.2, 7, K.BOW, SoundEvents.ARROW_SHOOT, 0.9f);
            case "bow_ice": return proj(id, "bow", 5, 28, 60, 22, 9, 1, 0, ARROW_ICE, 2.2, 6, K.BOW, SoundEvents.ARROW_SHOOT, 1.3f);
            case "bow_shock": return proj(id, "bow", 5, 28, 70, 22, 9, 1, 0, ARROW_SHOCK, 2.2, 6, K.BOW, SoundEvents.ARROW_SHOOT, 1.5f);
            case "bow_light": return proj(id, "bow", 6, 36, 120, 26, 12, 1, 0, ARROW_LIGHT, 2.8, 22, K.BOW, SoundEvents.BEACON_POWER_SELECT, 1.8f);
            case "bow_ancient": return proj(id, "bow", 6, 36, 140, 26, 12, 1, 0, ARROW_ANCIENT, 3.0, 28, K.BOW, SoundEvents.BEACON_POWER_SELECT, 2f);
            case "bow_multi": return proj(id, "bow", 4, 24, 90, 24, 10, 5, 0.12, ARROW_P, 2.0, 5, K.BOW, SoundEvents.ARROW_SHOOT, 1f);
            case "bow_bomb": return proj(id, "bow", 5, 26, 100, 24, 10, 1, 0, ARROW_BOMB, 1.9, 9, K.BOW, SoundEvents.ARROW_SHOOT, 0.7f);
            case "slingshot": return proj(id, "slingshot", 3, 20, 36, 16, 6, 3, 0.07, SEED, 1.7, 3, K.BOW, SoundEvents.SNOWBALL_THROW, 1.5f);
            case "crossbow_burst": return base(id, "crossbow", 3, 24, 60, 30, 3).tick((e, t, n) -> { if (n >= 5 && n < 26 && n % 4 == 1) { Combat.shoot(e, ARROW_P, mz(e), aim(e, t, 0.04), 2.2, 4f); snd(e, SoundEvents.CROSSBOW_SHOOT, 0.8f, 1.4f); } }).anim(anim(K.BOW, 8 / 30f, 1));
            case "boomerang": case "gale_boomerang": return proj(id, "boomerang", 3, 18, 60, 18, 6, 1, 0, BOOMER, 1.4, id.startsWith("gale") ? 8 : 5, K.THROW, SoundEvents.TRIDENT_THROW, 1.4f);
            case "bomb": case "remote_bomb": return proj(id, "bomb", 4, 18, 90, 20, 8, 1, 0, BOMB, 0.85, 11, K.THROW, SoundEvents.SNOWBALL_THROW, 0.7f);
            case "bombchu": return proj(id, "bomb", 5, 20, 120, 20, 8, 1, 0, BOMBCHU, 0.6, 9, K.THROW, SoundEvents.SNOWBALL_THROW, 1.2f);
            case "cannon": return proj(id, "bomb", 6, 24, 130, 24, 12, 1, 0, BOMB, 1.4, 12, K.BOW, SoundEvents.GENERIC_EXPLODE, 1.4f);
            case "fire_rod": case "candle_fire": return proj(id, "rod", 3, 18, 45, 20, 7, 1, 0, FIREBALL, 1.1, 8, K.CAST, SoundEvents.BLAZE_SHOOT, 1f);
            case "ice_rod": return proj(id, "rod", 3, 18, 55, 20, 7, 1, 0, ICEBALL, 1.1, 5, K.CAST, SoundEvents.SNOW_GOLEM_SHOOT, 0.8f);
            case "fire_spell": return proj(id, "sword", 3, 16, 60, 20, 7, 1, 0, FIREBALL, 1.0, 9, K.CAST, SoundEvents.BLAZE_SHOOT, 0.8f);
            case "cane_somaria": return proj(id, "rod", 3, 14, 70, 22, 8, 1, 0, SOMARIA, 0.9, 7, K.CAST, SoundEvents.AMETHYST_BLOCK_BREAK, 0.7f);
            case "magic_shot": case "echo_fire_keese": case "echo_octorok": return proj(id, "wand", 3, 18, 60, 24, 8, 3, 0.25, HOMING, 0.9, 5, K.CAST, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f);
            case "sword_beam": return proj(id, "sword", 4, 22, 60, 20, 7, 1, 0, BEAM, 1.6, 8, K.SLASH, SoundEvents.PLAYER_ATTACK_SWEEP, 1.6f);
            case "master_beam": return proj(id, "sword", 4, 24, 100, 22, 8, 1, 0, BEAM_BIG, 1.8, 12, K.SLASH, SoundEvents.BEACON_POWER_SELECT, 1.9f).when((e, t) -> e.getHealth() >= e.getMaxHealth() * 0.9f);
            case "skyward_strike": return base(id, "sword", 4, 24, 110, 34, 2).tick((e, t, n) -> {
                if (n < 18) Fx.dustBurst(L(e), LIGHT, 1f, e.position().add(0, 2.4, 0), 3, 0.2);
                if (n == 18) { Combat.shoot(e, BEAM_BIG, mz(e), aim(e, t, 0), 1.9, 15f); Fx.ring(L(e), mz(e), 1.2f, LIGHT, 1.6f, 22); snd(e, SoundEvents.BEACON_POWER_SELECT, 1f, 1.7f); }
            }).anim((p, t, a) -> { float c = win(t, .05f, .5f) * (1 - win(t, .55f, .6f)); p.rArmX = lerp(p.rArmX, -3.0f, c) + 2f * kick(t, 18 / 34f, .2f); p.bodyX -= 0.2f * c; });
            case "midna_blast": return proj(id, "none", 4, 22, 140, 28, 12, 1, 0, MAGIC, 1.1, 14, K.CAST, SoundEvents.ENDER_DRAGON_SHOOT, 1.4f);
            case "boulder_echo": case "echo_boulder": return base(id, "wand", 4, 18, 120, 30, 2).tick((e, t, n) -> {
                if (n == 10 && t != null) { Combat.shoot(e, BOULDER, t.position().add(0, 7, 0), new Vec3(0, -1, 0), 1.2, 12f); Fx.ring(L(e), t.position().add(0, 0.2, 0), 2f, 0x909080, 1.4f, 18); snd(e, SoundEvents.STONE_PLACE, 1f, 0.8f); }
            }).anim(anim(K.CAST, 10 / 30f, 1));
            case "ultrahand_throw": return base(id, "slate", 3, 18, 100, 28, 2).tick((e, t, n) -> {
                if (n >= 2 && n < 12) Fx.beam(L(e), mz(e), mz(e).add(e.getViewVector(1f).scale(2.5)).add(0, 1.2, 0), 0xa0ff80, 0.4f, 0.4);
                if (n == 14) { Combat.shoot(e, ZONAI_BOX, mz(e).add(e.getViewVector(1f).scale(2.2)).add(0, 1, 0), aim(e, t, 0.01), 1.3, 18f); snd(e, SoundEvents.ANVIL_PLACE, 0.7f, 1.3f); }
            }).anim(anim(K.THROW, 0, 1));
            case "magnesis": return base(id, "slate", 3, 18, 100, 26, 2).tick((e, t, n) -> {
                if (n >= 2 && n < 14) Fx.beam(L(e), mz(e), mz(e).add(e.getViewVector(1f).scale(2)).add(0, 1.4, 0), 0xff4040, 0.4f, 0.4);
                if (n == 14) { Combat.shoot(e, ZONAI_BOX, mz(e).add(e.getViewVector(1f).scale(2)).add(0, 1.4, 0), aim(e, t, 0.01), 1.6, 20f); snd(e, SoundEvents.ANVIL_LAND, 0.8f, 1.2f); }
            }).anim(anim(K.THROW, 0, 1));
            case "zonai_beam": case "guardian_beam": return base(id, "slate", 5, 28, 120, 26, 2).tick((e, t, n) -> {
                if (n >= 4 && n < 14 && t != null) Fx.beam(L(e), mz(e), mid(t), 0xff3030, 0.2f, 0.4);
                if (n == 16) { Vec3 d = aim(e, t, 0), m = mz(e); Combat.hitscan(e, m, d, 30, 15f, 3, 0xff4040); Fx.beam(L(e), m, m.add(d.scale(24)), id.startsWith("guard") ? 0xff3030 : 0x60ffa0, 0.9f, 0.3); snd(e, SoundEvents.BEACON_POWER_SELECT, 1f, 1.2f); }
            }).anim(anim(K.BOW, 16 / 26f, 1));
            case "zonai_flame": return base(id, "slate", 1, 8, 110, 40, 2).tick((e, t, n) -> {
                if (n >= 6 && n < 34) { Vec3 m = mz(e), d = aim(e, t, 0.05); for (int i = 1; i <= 5; i++) Fx.burst(L(e), ParticleTypes.FLAME, m.add(d.scale(i * 1.2)), 3, 0.25, 0.02); if (n % 4 == 0) for (LivingEntity f : Combat.cone(e, 7, 24)) { Combat.damage(e, f, 3.5f); f.setSecondsOnFire(5); } if (n % 6 == 0) snd(e, SoundEvents.FIRE_AMBIENT, 1f, 0.8f); }
            }).anim(anim(K.BOW, 0.5f, 1));
            case "rocket_dash": return base(id, "sword", 3, 14, 100, 22, 2).tick((e, t, n) -> {
                if (n == 3) { Combat.lunge(e, 1.8, 0.05); snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.8f, 1f); }
                if (n >= 3 && n < 10) Fx.burst(L(e), ParticleTypes.FLAME, e.position().add(0, 0.6, 0), 4, 0.2, 0.02);
                if (n == 9) KU.melee(e, 3.8, 14, 1.2, 0.3, FIRE);
            }).anim(anim(K.DASH, 0, 1));
            // ---- magic ----
            case "din_fire": return aoe(id, "none", 0, 8, 260, 30, 14, 5.5, 13, 1.1, FIRE, K.CAST, (e, f) -> f.setSecondsOnFire(6), false);
            case "farore_wind": return base(id, "none", 6, 20, 240, 26, 1).noLos().tick((e, t, n) -> {
                if (n == 6) { Fx.dustBurst(L(e), 0x40e060, 1.4f, e.position().add(0, 1, 0), 24, 0.5); snd(e, SoundEvents.ENDERMAN_TELEPORT, 1f, 1.5f); if (t != null) { Vec3 d = t.position().subtract(e.position()).normalize(); e.teleportTo(t.getX() - d.x * 2.2, t.getY(), t.getZ() - d.z * 2.2); Fx.dustBurst(L(e), 0x40e060, 1.4f, e.position().add(0, 1, 0), 24, 0.5); } }
                if (n == 14) KU.melee(e, 3.4, 9, 0.6, 0.2, 0x40e060);
            }).anim(anim(K.CAST, .3f, 1));
            case "bombos": return aoe(id, "rod", 3, 14, 300, 34, 18, 6, 15, 1, FIRE, K.CAST, (e, f) -> f.setSecondsOnFire(6), true);
            case "ether": return base(id, "rod", 3, 16, 300, 34, 1).tick((e, t, n) -> {
                if (n == 14) for (LivingEntity f : Combat.around(e, e.position(), 12)) { Combat.damage(e, f, 12); f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4)); Fx.beam(L(e), f.position().add(0, 8, 0), mid(f), 0xffffff, 1f, 0.5); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(f), 14, 0.4, 0.3); snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6f, 1.6f); }
            }).anim(anim(K.CAST, 14 / 34f, 1));
            case "quake": return aoe(id, "rod", 0, 8, 300, 34, 20, 7, 14, 1.3, 0xa08050, K.SLAM, null, false);
            case "thunder_spell": case "urbosa_fury": case "sage_riju": return base(id, "none", 2, 14, id.equals("thunder_spell") ? 180 : 500, 36, 1).tick((e, t, n) -> {
                if (n >= 10 && n < 30 && n % 5 == 0) for (LivingEntity f : Combat.around(e, e.position(), 11)) { Combat.damage(e, f, 8); Fx.beam(L(e), f.position().add(0, 8, 0), mid(f), 0xffffa0, 1f, 0.4); Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, mid(f), 8, 0.3, 0.2); snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5f, 1.7f); }
            }).anim(anim(K.CAST, 20 / 36f, 1));
            case "rod_of_seasons": return base(id, "rod", 3, 14, 200, 30, 2).tick((e, t, n) -> {
                if (n == 10) { int s = R.nextInt(4); int[] col = {0xff90c0, 0xffa030, 0xd08040, 0x90e0ff}; Vec3 c = t != null ? t.position() : e.position();
                    for (LivingEntity f : Combat.around(e, c, 5)) { Combat.damage(e, f, 4); if (s == 0) f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 6)); if (s == 1) f.setSecondsOnFire(5); if (s == 2) Combat.knock(f, c, 1.4, 0.4); if (s == 3) f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 7)); }
                    if (s == 0) e.heal(5); Fx.ring(L(e), c.add(0, 0.3, 0), 4f, col[s], 1.6f, 28); Fx.dustBurst(L(e), col[s], 1.4f, c.add(0, 1, 0), 24, 1.5); snd(e, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.2f); }
            }).anim(anim(K.CAST, 10 / 30f, 1));
            case "tornado_rod": case "gust_bellows": case "whirlwind": case "sage_tulin": case "echo_tornado": return aoe(id, "rod", 0, 8, 100, 28, 12, 5, 4, 2.2, WIND, K.CAST, null, false);
            case "sand_rod": case "sand_wand": return aoe(id, "rod", 2, 12, 100, 26, 10, 4, 3, 0.3, 0xe0c880, K.CAST, (e, f) -> f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 5)), true);
            case "water_rod": case "sage_sidon": return aoe(id, "rod", 3, 12, 100, 24, 10, 3.8, 6, 1.5, 0x60a0ff, K.CAST, null, true);
            case "fire_gloves": return aoe(id, "none", 0, 7, 100, 24, 10, 4, 9, 0.7, FIRE, K.CAST, (e, f) -> f.setSecondsOnFire(5), false);
            case "stasis": case "phantom_hourglass": case "minish_shrink": return base(id, "slate", 2, 16, 220, 22, 2).tick((e, t, n) -> {
                if (n == 8 && t != null) { for (LivingEntity f : Combat.around(e, t.position(), id.equals("phantom_hourglass") ? 6 : 2.5)) { f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 9)); f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 2)); f.addEffect(new MobEffectInstance(MobEffects.GLOWING, 90, 0)); Fx.ring(L(e), f.position().add(0, 1, 0), 1.6f, 0xffd040, 1.5f, 20); } snd(e, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 0.6f); }
            }).anim(anim(K.CAST, .35f, 1));
            case "cryonis": return base(id, "slate", 3, 14, 140, 24, 2).tick((e, t, n) -> {
                if (n == 10 && t != null) { Combat.damage(e, t, 9); t.setDeltaMovement(0, 1.1, 0); t.hurtMarked = true; for (int i = 0; i < 4; i++) Fx.burst(L(e), ParticleTypes.SNOWFLAKE, t.position().add(0, i * 0.6, 0), 8, 0.4, 0.05); Fx.dustBurst(L(e), ICE, 1.4f, t.position().add(0, 1, 0), 20, 0.5); snd(e, SoundEvents.GLASS_BREAK, 1f, 0.6f); }
            }).anim(anim(K.CAST, 10 / 24f, 1));
            case "recall": return base(id, "slate", 3, 16, 200, 34, 1).tick((e, t, n) -> {
                if (n == 2 && t != null) e.getPersistentData().putDouble("zl_rx", t.getX()); if (n == 2 && t != null) { e.getPersistentData().putDouble("zl_ry", t.getY()); e.getPersistentData().putDouble("zl_rz", t.getZ()); }
                if (n == 24 && t != null) { var d = e.getPersistentData(); t.teleportTo(d.getDouble("zl_rx"), d.getDouble("zl_ry"), d.getDouble("zl_rz")); Combat.damage(e, t, 8); Fx.dustBurst(L(e), 0x70ffb0, 1.4f, t.position().add(0, 1, 0), 24, 0.5); snd(e, SoundEvents.ENDERMAN_TELEPORT, 1f, 0.6f); }
            }).anim(anim(K.CAST, .5f, 1));
            case "ascend": return base(id, "sword", 4, 12, 150, 30, 1).tick((e, t, n) -> {
                if (n == 3) { e.setDeltaMovement(0, 1.1, 0); e.hurtMarked = true; Fx.burst(L(e), ParticleTypes.END_ROD, e.position(), 16, 0.3, 0.2); }
                if (n == 15 && t != null) { Vec3 d = t.position().subtract(e.position()).normalize(); e.setDeltaMovement(d.x * 0.8, -1.0, d.z * 0.8); e.hurtMarked = true; }
                if (n == 20) { KU.melee(e, 3.8, 15, 0.8, 0.3, 0xffffff); Fx.ring(L(e), e.position().add(0, 0.2, 0), 2.5f, 0xa0ffc0, 1.5f, 22); }
            }).anim(anim(K.JUMP, 0, 1));
            case "fuse_slam": return melee(id, 3.8, 120, 32, new int[] {16}, 18, 1.1, 0.4, FIRE, K.OVER, 16 / 32f, 1).onStart((e, t, n) -> snd(e, SoundEvents.ANVIL_PLACE, 0.7f, 1.6f));
            // ---- hooks / utility ----
            case "hookshot": case "whip": case "switch_hook": return base(id, "hookshot", 4, 16, 90, 22, 2).tick((e, t, n) -> {
                if (n >= 2 && n < 8 && t != null) Fx.beam(L(e), mz(e), mid(t), 0xb0b0b0, 0.3f, 0.3);
                if (n == 7 && t != null) { Combat.damage(e, t, 3); Vec3 d = e.position().subtract(t.position()).normalize(); t.setDeltaMovement(d.x * 1.3, 0.25, d.z * 1.3); t.hurtMarked = true; snd(e, SoundEvents.CHAIN_BREAK, 0.9f, 1.4f); }
                if (n == 13) KU.melee(e, 3.4, 6, 0.4, 0.1, 0xffffff);
            }).anim(anim(K.THROW, 0, 1));
            case "clawshot": case "harpoon": return base(id, "hookshot", 5, 20, 110, 24, 2).tick((e, t, n) -> {
                if (n >= 2 && n < 8 && t != null) Fx.beam(L(e), mz(e), mid(t), 0xb0b0b0, 0.3f, 0.3);
                if (n == 6 && t != null) { Vec3 d = t.position().subtract(e.position()).normalize(); e.setDeltaMovement(d.x * 1.6, 0.3, d.z * 1.6); e.hurtMarked = true; }
                if (n == 14) KU.melee(e, 3.6, 10, 0.7, 0.25, 0xffffff);
            }).anim(anim(K.DASH, 0, 1));
            case "gust_jar": return base(id, "jar", 1, 7, 100, 36, 2).tick((e, t, n) -> {
                if (n >= 4 && n < 28) for (LivingEntity f : Combat.cone(e, 7, 30)) { Vec3 d = e.position().subtract(f.position()).normalize(); f.setDeltaMovement(f.getDeltaMovement().add(d.scale(0.12))); f.hurtMarked = true; if (n % 6 == 0) Combat.damage(e, f, 2); }
                if (n % 4 == 0) Fx.burst(L(e), ParticleTypes.CLOUD, mz(e).add(e.getViewVector(1f).scale(2)), 3, 0.4, 0.02);
            }).anim(anim(K.BOW, .5f, 1));
            case "beetle": return proj(id, "none", 4, 22, 70, 22, 8, 1, 0, HOMING, 1.0, 6, K.THROW, SoundEvents.BEE_LOOP_AGGRESSIVE, 1.5f);
            case "pegasus_dash": return melee(id, 3.6, 70, 22, new int[] {10}, 8, 1.0, 0.2, 0xffffff, K.DASH, 0, 2).onStart((e, t, n) -> { Combat.lunge(e, 1.2, 0.02); });
            // ---- Majora's masks ----
            case "deku_bubble": return proj(id, "none", 3, 14, 60, 22, 7, 3, 0.3, HOMING, 0.7, 4, K.CAST, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.4f);
            case "deku_spin": return aoe(id, "none", 0, 5, 90, 26, 12, 3.6, 7, 0.9, 0x70c050, K.SPIN, null, false);
            case "goron_roll": return melee(id, 3.6, 100, 30, new int[] {8, 14, 20}, 7, 1.0, 0.25, 0xc08040, K.DASH, 0, 2).onStart((e, t, n) -> Combat.lunge(e, 1.6, 0));
            case "goron_pound": return aoe(id, "none", 0, 5, 110, 28, 14, 4.8, 16, 1.4, 0xc08040, K.SLAM, (e, f) -> f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3)), false);
            case "zora_fins": return proj(id, "none", 3, 16, 60, 20, 6, 2, 0.2, BOOMER, 1.4, 5, K.THROW, SoundEvents.TRIDENT_THROW, 1.8f);
            case "zora_barrier": return base(id, "none", 0, 5, 160, 46, 2).tick((e, t, n) -> {
                if (n >= 4 && n < 42) { double a = n * 0.7; Fx.beam(L(e), e.position().add(0, 1, 0), e.position().add(Math.cos(a) * 3, 1.2, Math.sin(a) * 3), 0x80d0ff, 0.5f, 0.3); if (n % 4 == 0) for (LivingEntity f : Combat.around(e, e.position(), 3.4)) { Combat.damage(e, f, 3); f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2)); } if (n % 8 == 0) snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.3f, 2f); }
            }).anim(anim(K.SPIN, 0, 1));
            case "fierce_beam": return proj(id, "sword", 4, 30, 140, 30, 12, 3, 0.16, BEAM_BIG, 2f, 14, K.SLASH, SoundEvents.BEACON_POWER_SELECT, 1.2f);
            // ---- wolf Link ----
            case "wolf_pounce": return base(id, "none", 2, 9, 90, 24, 2).tick((e, t, n) -> {
                if (n == 3) { Combat.lunge(e, 1.3, 0.35); snd(e, SoundEvents.WOLF_GROWL, 1f, 1f); }
                if (n == 11) for (LivingEntity f : Combat.cone(e, 3.4, 60)) { Combat.damage(e, f, 10); f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4)); Fx.dustBurst(L(e), 0xa01010, 1f, mid(f), 8, 0.3); }
            }).anim(anim(K.JUMP, 0, 1));
            case "howl": return base(id, "none", 0, 14, 400, 40, 1).noLos().tick((e, t, n) -> {
                if (n == 6) { snd(e, SoundEvents.WOLF_HOWL, 1.4f, 0.7f); for (LivingEntity f : Combat.around(e, e.position(), 8)) f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1)); e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1)); e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1)); Fx.ring(L(e), e.position().add(0, 1, 0), 5f, 0x80c0ff, 1.5f, 30); }
            }).anim((p, t, a) -> { float c = bump(win(t, .1f, .9f)); p.headX -= 0.8f * c; p.bodyX -= 0.3f * c; });
            case "midna_wolf_dash": return melee(id, 3.6, 70, 22, new int[] {10}, 9, 1, 0.2, 0xc0a0ff, K.DASH, 0, 2).onStart((e, t, n) -> Combat.lunge(e, 1.5, 0));
            // ---- Warriors specials ----
            case "musou": return base(id, "sword", 0, 8, 360, 50, 1).tick((e, t, n) -> {
                if (n >= 8 && n < 42 && n % 4 == 0) { for (LivingEntity f : Combat.around(e, e.position(), 7)) { Combat.damage(e, f, 5); Combat.knock(f, e.position(), 0.5, 0.3); } Fx.ring(L(e), e.position().add(0, 0.5 + (n % 8) * 0.1, 0), 2.5f + (n - 8) * 0.12f, GOLD, 1.5f, 26); slash(e, n % 8 == 0 ? 80 : -80, n % 8 == 0 ? -80 : 80, 30, GOLD); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.1f + n * 0.01f); }
                if (n == 44) { for (LivingEntity f : Combat.around(e, e.position(), 8)) { Combat.damage(e, f, 14); Combat.knock(f, e.position(), 1.4, 0.5); } Fx.explosion(L(e), e.position().add(0, 1, 0), 3f); snd(e, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.2f); }
            }).anim(anim(K.SPIN, 0, 1));
            case "great_fairy": return base(id, "none", 0, 12, 400, 40, 1).noLos().tick((e, t, n) -> {
                if (n == 10) { e.heal(e.getMaxHealth() * 0.3f); Fx.dustBurst(L(e), 0xffb0e0, 1.3f, e.position().add(0, 1.2, 0), 30, 0.7); snd(e, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.5f); }
                if (n == 24) { Vec3 d = aim(e, t, 0); Combat.hitscan(e, mz(e), d, 20, 14f, 6, 0xffb0e0); Fx.beam(L(e), mz(e), mz(e).add(d.scale(18)), 0xffd0f0, 1.1f, 0.3); }
            }).anim(anim(K.CAST, .6f, 1));
            case "clone_strikes": return base(id, "sword", 0, 8, 150, 34, 2).tick((e, t, n) -> {
                if (n == 6 || n == 11 || n == 16 || n == 21) { for (LivingEntity f : Combat.around(e, e.position(), 5)) { Combat.damage(e, f, 6); Fx.sparks(L(e), mid(f), 5); Fx.dustBurst(L(e), 0x202020, 1f, f.position().add(Math.cos(n) * 1.2, 0.8, Math.sin(n) * 1.2), 6, 0.2); } snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.5f); }
            }).anim(anim(K.COMBO, 0, 5));
            case "shuriken_volley": return proj(id, "none", 4, 20, 70, 20, 7, 5, 0.1, ARROW_P, 2.0, 4, K.THROW, SoundEvents.TRIDENT_THROW, 2f);
            case "cucco_swarm": return proj(id, "none", 3, 20, 130, 26, 9, 6, 0.3, COCCO, 0.7, 4, K.CAST, SoundEvents.CHICKEN_AMBIENT, 1.2f);
            case "book_fire": return proj(id, "book", 3, 20, 70, 22, 8, 1, 0, FIREBALL, 1.1, 9, K.CAST, SoundEvents.BLAZE_SHOOT, 1f);
            case "book_light": return base(id, "book", 3, 16, 160, 30, 2).tick((e, t, n) -> { if (n == 14) for (LivingEntity f : Combat.around(e, e.position(), 9)) { Combat.damage(e, f, 10); Fx.beam(L(e), f.position().add(0, 8, 0), mid(f), 0xffffff, 1f, 0.4); snd(e, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5f, 1.8f); } }).anim(anim(K.CAST, 14 / 30f, 1));
            case "ancient_blast": return aoe(id, "slate", 4, 18, 200, 30, 18, 4.5, 16, 1, ANCIENT, K.CAST, null, true);
            case "bind": return base(id, "wand", 3, 12, 140, 30, 2).tick((e, t, n) -> {
                if (t != null && n >= 4 && n < 24) { Fx.beam(L(e), mz(e), mid(t), 0xffe080, 0.4f, 0.3); Vec3 p = e.position().add(e.getViewVector(1f).scale(2.5)); Vec3 d = p.subtract(t.position()); t.setDeltaMovement(d.scale(0.2)); t.hurtMarked = true; if (n % 6 == 0) Combat.damage(e, t, 2); }
                if (n == 24 && t != null) { Vec3 f = e.getViewVector(1f); t.setDeltaMovement(f.x * 2, 0.3, f.z * 2); t.hurtMarked = true; Combat.damage(e, t, 9); }
            }).anim(anim(K.CAST, .8f, 1));
            case "phantom_charge": return melee(id, 3.8, 90, 28, new int[] {12}, 14, 1.4, 0.3, 0xc0c0d0, K.DASH, 0, 2).onStart((e, t, n) -> Combat.lunge(e, 1.2, 0));
            case "light_burst": return aoe(id, "none", 0, 7, 200, 28, 12, 5, 11, 1.2, LIGHT, K.CAST, null, false);
        }
        throw new IllegalArgumentException("unknown zelda attack " + id);
    }

    // ---------- model ----------
    @Override
    public void buildModel(Rig rig) {
        Look l = look;
        Mat skin = Mat.of(l.skin).noise(.04f).bevel(.1f), tunic = Mat.of(l.tunic).noise(.1f).bevel(.12f), under = Mat.of(l.under).noise(.08f), hair = Mat.of(l.hair).noise(.12f);
        Mat belt = Mat.of(l.belt).noise(.1f), buckle = Mat.of(GOLD).bevel(.2f), boots = Mat.of(l.boots).noise(.1f), dark = Mat.of(0x201810), eye = Mat.of(0x2060c0).glow(), fur = Mat.of(l.wolf ? 0x6a6a72 : l.tunic).noise(.15f);
        if (l.wolf) { // Wolf Link: grey fur, snout, ears, tail
            Mat f2 = Mat.of(0x4a4a52).noise(.15f), belly = Mat.of(0xb8b8c0).noise(.12f), red = Mat.of(0xe0a020).glow();
            rig.box(rig.torso, fur, -4, -12, -2, 8, 12, 4, .1f); rig.box(rig.torso, belly, -2, -10, -2.6f, 4, 8, 1); rig.box(rig.torso, f2, -3, -13, -2, 6, 3, 4, .2f);
            rig.box(rig.torso, f2, -1, -4, 2, 2, 2, 6); rig.box(rig.torso, f2, -2, -3, 6, 4, 3, 3); // tail
            rig.box(rig.head, fur, -4, -8, -4, 8, 8, 8, .1f); rig.box(rig.head, belly, -2.5f, -4, -9, 5, 4, 5); rig.box(rig.head, dark, -1, -4.5f, -9.6f, 2, 2, 1);
            rig.box(rig.head, f2, -4, -12, -1, 3, 4, 2); rig.box(rig.head, f2, 1, -12, -1, 3, 4, 2); rig.box(rig.head, red, -3.5f, -6, -4.5f, 2, 1, 1); rig.box(rig.head, red, 1.5f, -6, -4.5f, 2, 1, 1);
            for (int s = -1; s <= 1; s += 2) { PartDefinition arm = s < 0 ? rig.rArm : rig.lArm, leg = s < 0 ? rig.rLeg : rig.lLeg; rig.box(arm, fur, -2, -2, -2, 4, 12, 4, .1f); rig.box(leg, fur, -2, 0, -2, 4, 12, 4, .1f); rig.box(leg, f2, -2, 9, -2.5f, 4, 3, 5, .1f); }
            mounts(rig); return;
        }
        // torso / outfit
        if (l.dress) {
            Mat gown = Mat.of(l.tunic).noise(.06f).bevel(.15f);
            rig.box(rig.torso, gown, -4, -12, -2, 8, 12, 4, .15f); rig.box(rig.torso, under, -4.2f, -12, -2.4f, 8, 4, 5, .1f); rig.box(rig.torso, gown, -5, -4, -3, 10, 6, 6); rig.box(rig.torso, under, -5.2f, 1, -3.2f, 10, 1, 6, .1f);
            rig.box(rig.torso, buckle, -1, -9, -2.6f, 2, 2, 1); rig.box(rig.torso, belt, -4.5f, -4.5f, -2.5f, 9, 1, 5);
        } else {
            rig.box(rig.torso, under, -4, -12, -2, 8, 12, 4); rig.box(rig.torso, tunic, -4.2f, -12, -2.2f, 8, 11, 4, .25f); rig.box(rig.torso, belt, -4.5f, -4, -2.5f, 9, 2, 5); rig.box(rig.torso, buckle, -1, -4, -3.1f, 2, 2, 1);
            rig.box(rig.torso, Mat.of(l.shield == 0 ? 0x704020 : 0x704020), -1, -12, 2.3f, 2, 8, 2); // sheathed sword on back
        }
        // head
        rig.box(rig.head, skin, -4, -8, -4, 8, 8, 8);
        if (l.bigHead) rig.box(rig.head, skin, -5, -9, -5, 10, 10, 10, .1f);
        rig.box(rig.head, hair, -4, -8.4f, -4, 8, 3, 8, .3f); rig.box(rig.head, hair, -4, -8, 3, 8, 8, 1, .3f);
        if (l.longHair) { rig.box(rig.head, hair, -4, -8, 3.2f, 8, 16, 1); rig.box(rig.head, hair, -5, -6, -2, 1, 10, 4); rig.box(rig.head, hair, 4, -6, -2, 1, 10, 4); }
        rig.box(rig.head, eye, -3, -4.5f, -4.2f, 2, 2, 1); rig.box(rig.head, eye, 1, -4.5f, -4.2f, 2, 2, 1);
        if (l.ears) { rig.box(rig.head, skin, -6, -5, -1, 2, 1, 2); rig.box(rig.head, skin, 4, -5, -1, 2, 1, 2); } // pointed hylian ears
        if (l.hat >= 0 && !l.hatless) { // pointed cap
            Mat hat = Mat.of(l.hat).noise(.1f).bevel(.15f);
            rig.box(rig.head, hat, -4.5f, -10, -4.5f, 9, 3, 9); rig.box(rig.head, hat, -3.5f, -13, -3, 7, 3, 7); rig.box(rig.head, hat, -2.5f, -16, -1.5f, 5, 3, 5); rig.box(rig.head, hat, -1.5f, -18, 0, 3, 3, 4); rig.box(rig.head, hat, -1, -17, 3, 2, 4, 3); rig.box(rig.head, buckle, -0.5f, -17.5f, 6, 1, 1, 1);
        }
        if (l.bandana) rig.box(rig.head, Mat.of(0xc03040).noise(.1f), -4, -8, -4, 8, 3, 8, .4f);
        if (l.dress) { rig.box(rig.head, buckle, -3, -9.5f, -4.3f, 6, 1, 1); rig.box(rig.head, Mat.of(0x4060e0).glow(), -0.5f, -10.5f, -4.5f, 1, 2, 1); } // tiara + gem
        for (int s = -1; s <= 1; s += 2) {
            PartDefinition arm = s < 0 ? rig.rArm : rig.lArm, leg = s < 0 ? rig.rLeg : rig.lLeg;
            rig.box(arm, l.dress ? tunic : under, -2, -2, -2, 4, 8, 4, .05f); rig.box(arm, skin, -2, 6, -2, 4, 4, 4); rig.box(arm, belt, -2.2f, 5, -2.2f, 4, 2, 4, .3f);
            rig.box(leg, l.dress ? Mat.of(l.under) : under, -2, 0, -2, 4, 8, 4); rig.box(leg, boots, -2, 7, -2, 4, 5, 4, .3f);
        }
        mounts(rig);
    }

    private void mounts(Rig rig) {
        Set<String> need = new LinkedHashSet<>(); need.add("sword");
        for (AttackDef a : attacks()) { if (a.weaponR != null) need.add(a.weaponR); if (a.weaponL != null) need.add(a.weaponL); }
        Mat steel = Mat.of(look.blade).bevel(.1f), hilt = Mat.of(look.hilt).bevel(.2f), gold = Mat.of(GOLD).bevel(.2f), wood = Mat.of(0x6a4a2a).noise(.15f), dark = Mat.of(0x303038).noise(.08f), metal = Mat.of(0x8a8c90).noise(.06f);
        for (String n : need) {
            PartDefinition w;
            switch (n) {
                case "sword" -> { w = rig.weapon(rig.rHand, "sword"); rig.box(w, steel, -0.5f, -2.5f, -17, 1, 2, 15); rig.box(w, Mat.of(look.blade == 0xdce4f0 ? 0xa8c8f0 : look.blade).glow(), -0.3f, -2.2f, -17, 1, 1, 14); rig.box(w, gold, -2.5f, -3, -3, 5, 2, 1); rig.box(w, hilt, -0.5f, -2.5f, -2, 1, 2, 5); }
                case "shield" -> { w = rig.weapon(rig.lHand, "shield"); rig.box(w, Mat.of(look.shield).bevel(.2f), 2, -5, -4, 1, 10, 8); rig.box(w, Mat.of(look.trim).bevel(.2f), 3, -6, -4.5f, 1, 12, 1); rig.box(w, gold, 3, -2, -1, 1, 3, 3); rig.box(w, Mat.of(look.trim), 3, -3, -5, 1, 1, 10); }
                case "bow" -> { w = rig.weapon(rig.rHand, "bow"); rig.box(w, wood, -0.5f, -9, -2, 1, 18, 1); rig.box(w, wood, -0.5f, -10, -3, 1, 2, 1); rig.box(w, wood, -0.5f, 8, -3, 1, 2, 1); rig.box(w, Mat.of(0xe0e0d0), -0.2f, -9, -3, 1, 18, 1); }
                case "slingshot" -> { w = rig.weapon(rig.rHand, "slingshot"); rig.box(w, wood, -0.5f, -2, -4, 1, 2, 5); rig.box(w, wood, -2.5f, -5, -6, 1, 4, 1); rig.box(w, wood, 1.5f, -5, -6, 1, 4, 1); rig.box(w, Mat.of(0xa04030), -1.5f, -5, -6, 3, 1, 1); }
                case "bomb" -> { w = rig.weapon(rig.rHand, "bomb"); rig.box(w, dark, -2, -2.5f, -4, 4, 4, 4, .2f); rig.box(w, Mat.of(FIRE).glow(), -0.5f, -4, -3, 1, 2, 1); }
                case "boomerang" -> { w = rig.weapon(rig.rHand, "boomerang"); rig.box(w, Mat.of(0xc08a40).bevel(.2f), -3, -2, -6, 6, 1, 2); rig.box(w, Mat.of(0xc08a40).bevel(.2f), -3, -2, -8, 2, 1, 2); rig.box(w, Mat.of(0xc08a40).bevel(.2f), 1, -2, -8, 2, 1, 2); }
                case "hookshot" -> { w = rig.weapon(rig.rHand, "hookshot"); rig.box(w, metal, -1, -3, -8, 2, 2, 9); rig.box(w, Mat.of(0xc0c0c8), -1.5f, -3.5f, -12, 3, 3, 4); rig.box(w, dark, -1, 0, -1, 2, 3, 2); }
                case "hammer" -> { w = rig.weapon(rig.rHand, "hammer"); rig.box(w, wood, -0.5f, -2.5f, -14, 1, 1, 14); rig.box(w, metal, -3, -5, -19, 6, 6, 6, .1f); rig.box(w, Mat.of(GOLD), -3.2f, -3, -17, 6, 1, 1); }
                case "rod" -> { w = rig.weapon(rig.rHand, "rod"); rig.box(w, Mat.of(0xb0a080).bevel(.1f), -0.5f, -2.5f, -12, 1, 1, 12); rig.box(w, Mat.of(0xff6030).glow(), -1, -3, -14, 2, 2, 2); }
                case "wand" -> { w = rig.weapon(rig.rHand, "wand"); rig.box(w, Mat.of(0xe0d0a0).bevel(.1f), -0.5f, -2.5f, -12, 1, 1, 12); rig.box(w, Mat.of(0x60c0ff).glow(), -1.5f, -3.5f, -15, 3, 3, 3); }
                case "book" -> { w = rig.weapon(rig.rHand, "book"); rig.box(w, Mat.of(0x6a2a8a).bevel(.15f), -3, -4, -4, 6, 1, 8); rig.box(w, Mat.of(GOLD), -0.5f, -4.5f, -1, 1, 1, 2); }
                case "crossbow" -> { w = rig.weapon(rig.rHand, "crossbow"); rig.box(w, wood, -1, -2.5f, -8, 2, 2, 10); rig.box(w, metal, -5, -3, -8, 10, 1, 1); rig.box(w, dark, -1, 0, -1, 2, 3, 2); }
                case "slate" -> { w = rig.weapon(rig.rHand, "slate"); rig.box(w, Mat.of(0x303840).bevel(.15f), -3, -2, -6, 6, 1, 8); rig.box(w, Mat.of(SHEIK).glow(), -2, -2.6f, -5, 4, 1, 6); rig.box(w, Mat.of(0xe0a020).glow(), -0.5f, -3, -2, 1, 1, 1); }
                case "jar" -> { w = rig.weapon(rig.rHand, "jar"); rig.box(w, Mat.of(0x70b050).bevel(.2f), -2.5f, -4, -6, 5, 5, 5); rig.box(w, metal, -1, -3, -10, 2, 2, 4); rig.box(w, Mat.of(0x806030), -1.5f, -6, -5, 3, 2, 3); }
                case "ballchain" -> { w = rig.weapon(rig.rHand, "ballchain"); rig.box(w, metal, -0.5f, -2.5f, -9, 1, 1, 8); rig.box(w, Mat.of(0x505058).bevel(.2f), -3, -5, -14, 6, 6, 6, .2f); }
                case "none" -> { /* no weapon */ }
                default -> { /* shared names handled above */ }
            }
        }
    }

    // ================= kits =================
    static Look link() { return new Look().hat(0x2f9a3a); }
    public static class LoZ extends ZeldaKits { public LoZ() { super("zelda_loz_link", "Link (The Legend of Zelda)", "リンク (ゼルダの伝説)", "The Legend of Zelda", link().tunic(0x40a040).shield(0xa03030, 0xe0e0e0), 90, 0.32, 1f, 1,
            "sword_slash", "sword_beam", "sword_stab", "boomerang", "bow_arrow", "bomb", "candle_fire", "shield_bash", "dodge"); } }
    public static class ZeldaII extends ZeldaKits { public ZeldaII() { super("zelda2_link", "Link (Zelda II)", "リンク (リンクの冒険)", "Zelda II: The Adventure of Link", link().tunic(0x4aa04a).shield(0xc06020, 0xe0e0e0), 95, 0.32, 1f, 1,
            "sword_combo", "downthrust", "upthrust", "fire_spell", "thunder_spell", "sword_beam", "guard_parry", "jump_slash"); } }
    public static class ALttP extends ZeldaKits { public ALttP() { super("zelda_alttp_link", "Link (A Link to the Past)", "リンク (神々のトライフォース)", "A Link to the Past", link().tunic(0x3aa03a).hair(0xd05080).shield(0x3050c0, 0xe0e0e0), 100, 0.33, 1f, 2,
            "sword_combo", "spin_attack", "sword_beam", "bow_arrow", "bomb", "boomerang", "hookshot", "hammer_slam", "fire_rod", "ice_rod", "bombos", "ether", "quake", "cane_somaria", "magic_cape", "guard_parry", "dash_attack"); } }
    public static class LinksAwakening extends ZeldaKits { public LinksAwakening() { super("zelda_la_link", "Link (Link's Awakening)", "リンク (夢をみる島)", "Link's Awakening", link().tunic(0x3aa03a).shield(0x3050c0, 0xe0e0e0), 100, 0.33, 0.95f, 1,
            "sword_combo", "spin_attack", "jump_slash", "pegasus_dash", "bow_arrow", "bomb", "boomerang", "hookshot", "fire_rod", "shield_bash", "guard_parry", "dodge"); } }
    public static class OoTAdult extends ZeldaKits { public OoTAdult() { super("zelda_oot_link", "Link (Ocarina of Time, Adult)", "リンク (時のオカリナ・大人)", "Ocarina of Time", link().tunic(0x2f9a3a).shield(0xc03030, 0xe0e0e0), 110, 0.33, 1.05f, 2,
            "sword_combo", "spin_attack", "great_spin", "jump_slash", "ending_blow", "bow_arrow", "bow_fire", "bow_ice", "bow_light", "hookshot", "hammer_slam", "bomb", "bombchu", "din_fire", "farore_wind", "nayru_love", "iron_stomp", "guard_parry", "backflip", "sidehop"); } }
    public static class OoTChild extends ZeldaKits { public OoTChild() { super("zelda_oot_young_link", "Young Link (Ocarina of Time)", "こどもリンク (時のオカリナ)", "Ocarina of Time", link().tunic(0x2f9a3a).shield(0xc03030, 0xe0e0e0), 70, 0.31, 0.7f, 1,
            "sword_combo", "spin_attack", "jump_slash", "slingshot", "boomerang", "bomb", "bombchu", "shield_bash", "guard_parry", "backflip", "sidehop"); } }
    public static class MajoraMask extends ZeldaKits { public MajoraMask() { super("zelda_mm_link", "Link (Majora's Mask)", "リンク (ムジュラの仮面)", "Majora's Mask", link().tunic(0x2f9a3a).shield(0x3050c0, 0xe0e0e0), 100, 0.33, 0.9f, 2,
            "sword_combo", "spin_attack", "jump_slash", "bow_arrow", "bomb", "deku_bubble", "deku_spin", "goron_roll", "goron_pound", "zora_fins", "zora_barrier", "fierce_beam", "guard_parry", "backflip"); } }
    public static class Oracle extends ZeldaKits { public Oracle() { super("zelda_oracle_link", "Link (Oracle of Seasons/Ages)", "リンク (ふしぎの木の実)", "Oracle of Seasons / Ages", link().tunic(0x35a035).shield(0x3050c0, 0xe0e0e0), 100, 0.33, 0.95f, 1,
            "sword_combo", "spin_attack", "rod_of_seasons", "switch_hook", "cane_somaria", "bomb", "boomerang", "bow_arrow", "pegasus_dash", "shield_bash", "guard_parry"); } }
    public static class FourSwords extends ZeldaKits { public FourSwords() { super("zelda_fs_link", "Link (Four Swords)", "リンク (4つの剣)", "The Four Swords", link().tunic(0x40c040).shield(0x3050c0, 0xe0e0e0), 95, 0.34, 0.85f, 1,
            "sword_combo", "spin_attack", "four_formation", "doppelganger", "sword_beam", "bomb", "bow_arrow", "fire_rod", "gust_jar", "dash_attack"); } }
    public static class WindWaker extends ZeldaKits { public WindWaker() { super("zelda_ww_link", "Toon Link (The Wind Waker)", "トゥーンリンク (風のタクト)", "The Wind Waker", link().tunic(0x4ab040).big().shield(0xc04040, 0xe0e0e0), 100, 0.34, 0.9f, 1,
            "sword_combo", "spin_attack", "jump_slash", "ending_blow", "dodge", "backflip", "guard_parry", "bow_arrow", "boomerang", "hookshot", "bomb", "hammer_slam", "tornado_rod", "bow_light", "shield_bash"); } }
    public static class MinishCap extends ZeldaKits { public MinishCap() { super("zelda_mc_link", "Link (The Minish Cap)", "リンク (ふしぎのぼうし)", "The Minish Cap", link().tunic(0x45b045).big().shield(0x3050c0, 0xe0e0e0), 95, 0.33, 0.85f, 1,
            "sword_combo", "spin_attack", "great_spin", "rock_breaker", "dash_attack", "gust_jar", "minish_shrink", "bomb", "bow_arrow", "boomerang", "sword_beam", "guard_parry"); } }
    public static class TwilightPrincess extends ZeldaKits { public TwilightPrincess() { super("zelda_tp_link", "Link (Twilight Princess)", "リンク (トワイライトプリンセス)", "Twilight Princess", link().tunic(0x3a9a3a).hair(0x8a6030).shield(0x3050c0, 0xe0e0e0), 110, 0.33, 1.05f, 2,
            "sword_combo", "spin_attack", "great_spin", "ending_blow", "helm_splitter", "mortal_draw", "shield_bash", "guard_parry", "bow_arrow", "gale_boomerang", "clawshot", "bomb", "ball_chain", "iron_stomp", "dash_attack", "backflip", "sidehop"); } }
    public static class WolfLink extends ZeldaKits { public WolfLink() { super("zelda_tp_wolf_link", "Wolf Link (Twilight Princess)", "ウルフリンク (トワイライトプリンセス)", "Twilight Princess", new Look().wolf().tunic(0x6a6a72), 100, 0.4, 1.0f, 1,
            "wolf_bite", "wolf_pounce", "midna_wolf_dash", "midna_blast", "howl", "dodge"); } }
    public static class PhantomHourglass extends ZeldaKits { public PhantomHourglass() { super("zelda_ph_link", "Link (Phantom Hourglass)", "リンク (夢幻の砂時計)", "Phantom Hourglass", link().tunic(0x40a840).big().shield(0xc04040, 0xe0e0e0), 90, 0.33, 0.9f, 1,
            "sword_combo", "spin_attack", "dash_attack", "phantom_hourglass", "boomerang", "bomb", "bombchu", "cannon", "hookshot", "fire_rod", "bow_arrow", "shield_bash"); } }
    public static class SpiritTracksLink extends ZeldaKits { public SpiritTracksLink() { super("zelda_st_link", "Link (Spirit Tracks)", "リンク (大地の汽笛)", "Spirit Tracks", link().tunic(0x40a840).big().shield(0xc04040, 0xe0e0e0), 90, 0.33, 0.9f, 1,
            "sword_combo", "spin_attack", "dash_attack", "whirlwind", "whip", "sand_wand", "boomerang", "bomb", "bow_arrow", "shield_bash"); } }
    public static class SpiritTracksZelda extends ZeldaKits { public SpiritTracksZelda() { super("zelda_st_zelda", "Zelda (Spirit Tracks)", "ゼルダ (大地の汽笛)", "Spirit Tracks", new Look().dress().tunic(0xf0d0e0).hair(0x9a5a2a).under(0xffffff), 80, 0.3, 0.95f, 1,
            "sword_combo", "phantom_charge", "light_burst", "magic_shot", "bind", "dodge"); } }
    public static class SkywardSword extends ZeldaKits { public SkywardSword() { super("zelda_ss_link", "Link (Skyward Sword)", "リンク (スカイウォードソード)", "Skyward Sword", link().tunic(0x3a9a3a).hair(0xe0c060).blade(0xe8f0ff, 0x3060c0).shield(0xc0c0d0, 0xf0f0f0), 105, 0.34, 1f, 1,
            "sword_combo", "spin_attack", "skyward_strike", "ending_blow", "jump_slash", "shield_bash", "guard_parry", "gust_bellows", "beetle", "whip", "clawshot", "bow_arrow", "slingshot", "bomb", "dash_attack"); } }
    public static class ALBW extends ZeldaKits { public ALBW() { super("zelda_albw_link", "Link (A Link Between Worlds)", "リンク (神々のトライフォース2)", "A Link Between Worlds", link().tunic(0x3aa03a).hair(0xd0a060).shield(0x3050c0, 0xe0e0e0), 100, 0.33, 1f, 1,
            "sword_combo", "spin_attack", "sword_beam", "wall_merge", "tornado_rod", "fire_rod", "ice_rod", "sand_rod", "hammer_slam", "bow_arrow", "bomb", "hookshot", "guard_parry"); } }
    public static class TriForceHeroes extends ZeldaKits { public TriForceHeroes() { super("zelda_tfh_link", "Link (Tri Force Heroes)", "リンク (トライフォース英雄伝説)", "Tri Force Heroes", link().tunic(0x40c040).shield(0x3050c0, 0xe0e0e0), 85, 0.34, 0.85f, 1,
            "sword_combo", "spin_attack", "sword_beam", "fire_gloves", "water_rod", "gust_jar", "clawshot", "bomb", "boomerang", "bow_arrow", "hammer_slam", "doppelganger"); } }
    public static class BotW extends ZeldaKits { public BotW() { super("zelda_botw_link", "Link (Breath of the Wild)", "リンク (ブレス オブ ザ ワイルド)", "Breath of the Wild", new Look().tunic(0x4a70c0).under(0xe0d8c0).hair(0xd0b060).hat(0x4a70c0).hatless().blade(0xe8f0ff, 0x3060c0).shield(0x3050c0, 0xe0e0e0), 120, 0.35, 1.0f, 3,
            "sword_combo", "spear_thrust", "greatsword_smash", "master_beam", "perfect_guard", "dodge", "backflip", "flurry_rush", "bow_arrow", "bow_fire", "bow_ice", "bow_shock", "bow_multi", "bow_ancient", "bomb", "stasis", "magnesis", "cryonis", "daruk_protection", "urbosa_fury", "dash_attack", "sword_stab"); } }
    public static class TotK extends ZeldaKits { public TotK() { super("zelda_totk_link", "Link (Tears of the Kingdom)", "リンク (ティアーズ オブ ザ キングダム)", "Tears of the Kingdom", new Look().tunic(0x3f78c0).under(0xe0d8c0).hair(0xc8b060).hat(0x3f78c0).hatless().blade(0xe8f0ff, 0x3060c0).shield(0x3050c0, 0xe0e0e0), 125, 0.35, 1.0f, 3,
            "sword_combo", "spear_thrust", "greatsword_smash", "fuse_slam", "master_beam", "perfect_guard", "dodge", "flurry_rush", "bow_arrow", "bow_fire", "bow_ice", "bow_shock", "bow_multi", "bow_bomb", "ultrahand_throw", "recall", "ascend", "zonai_beam", "zonai_flame", "rocket_dash", "sage_tulin", "sage_riju", "sage_sidon", "bomb"); } }
    public static class EchoesZelda extends ZeldaKits { public EchoesZelda() { super("zelda_eow_zelda", "Zelda (Echoes of Wisdom)", "ゼルダ (知恵のかりもの)", "Echoes of Wisdom", new Look().dress().tunic(0x6a4a90).hair(0xc8a050).under(0xf0e0d0), 85, 0.31, 0.98f, 2,
            "sword_combo", "dash_attack", "echo_boulder", "echo_fire_keese", "echo_tornado", "bind", "light_burst", "magic_shot", "dodge", "echo_octorok"); } }
    public static class HwLink extends ZeldaKits { public HwLink() { super("zelda_hw_link", "Link (Hyrule Warriors)", "リンク (ゼルダ無双)", "Hyrule Warriors", link().tunic(0x3aa03a).shield(0x3050c0, 0xe0e0e0), 140, 0.34, 1f, 1,
            "sword_combo", "spin_attack", "sword_beam", "jump_slash", "boomerang", "bomb", "bow_light", "hookshot", "great_fairy", "musou", "guard_parry", "dodge"); } }
    public static class HwZelda extends ZeldaKits { public HwZelda() { super("zelda_hw_zelda", "Zelda (Hyrule Warriors)", "ゼルダ (ゼルダ無双)", "Hyrule Warriors", new Look().dress().tunic(0xe8e0f0).hair(0xc09040).under(0xd0a0e0), 110, 0.33, 0.98f, 1,
            "rapier_flurry", "bow_light", "din_fire", "nayru_love", "farore_wind", "light_burst", "magic_shot", "musou", "dodge"); } }
    public static class HwImpa extends ZeldaKits { public HwImpa() { super("zelda_hw_impa", "Impa (Hyrule Warriors)", "インパ (ゼルダ無双)", "Hyrule Warriors", new Look().tunic(0xa02030).hair(0xe8e8f0).under(0x302020).skin(0xe8b890).longHair().hatless().noShield().blade(0xd0d0d8, 0xa02030), 150, 0.32, 1.05f, 1,
            "greatsword_smash", "sword_combo", "spin_attack", "clone_strikes", "shuriken_volley", "dash_attack", "musou", "dodge"); } }
    public static class HwLana extends ZeldaKits { public HwLana() { super("zelda_hw_lana", "Lana (Hyrule Warriors)", "ルナ (ゼルダ無双)", "Hyrule Warriors", new Look().dress().tunic(0x4a3a8a).hair(0x6a40a8).under(0x302060), 105, 0.33, 0.96f, 1,
            "spear_thrust", "book_fire", "book_light", "cucco_swarm", "tornado_rod", "magic_shot", "musou", "dodge"); } }
    public static class HwLinkle extends ZeldaKits { public HwLinkle() { super("zelda_hw_linkle", "Linkle (Hyrule Warriors)", "リンクル (ゼルダ無双)", "Hyrule Warriors", new Look().tunic(0x3a9a3a).hair(0x6a3a1a).under(0xe0d0b0).noShield().hatless(), 110, 0.35, 0.95f, 1,
            "crossbow_burst", "bow_bomb", "sword_combo", "spin_attack", "dash_attack", "bomb", "musou", "dodge", "backflip"); } }
    public static class AocLink extends ZeldaKits { public AocLink() { super("zelda_aoc_link", "Link (Age of Calamity)", "リンク (ゼルダ無双 厄災の黙示録)", "Age of Calamity", new Look().tunic(0x4a70c0).under(0xe0d8c0).hair(0xd0b060).hatless().blade(0xe8f0ff, 0x3060c0).shield(0x3050c0, 0xe0e0e0), 140, 0.35, 1f, 2,
            "sword_combo", "spear_thrust", "greatsword_smash", "master_beam", "perfect_guard", "flurry_rush", "dodge", "bow_ancient", "stasis", "magnesis", "cryonis", "bomb", "ancient_blast", "musou"); } }
    public static class AocZelda extends ZeldaKits { public AocZelda() { super("zelda_aoc_zelda", "Zelda (Age of Calamity)", "ゼルダ (ゼルダ無双 厄災の黙示録)", "Age of Calamity", new Look().dress().tunic(0x3a6aa0).hair(0xc8a050).under(0xe0e8f0), 110, 0.33, 0.98f, 2,
            "guardian_beam", "ancient_blast", "stasis", "cryonis", "magnesis", "bomb", "zonai_beam", "light_burst", "dodge", "musou"); } }
}
