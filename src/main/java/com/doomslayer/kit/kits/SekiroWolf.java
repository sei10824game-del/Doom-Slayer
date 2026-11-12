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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;
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


    // ================= Posture / deflect system =================
    static long now(SlayerEntity e) { return e.level().getGameTime(); }

    /** Deflect: perfect window negates the hit, builds the enemy's posture and bats back projectiles; guard cuts damage but builds Wolf's posture. */
    @Override
    public float modifyDamage(SlayerEntity e, DamageSource src, float amt) {
        var d = e.getPersistentData(); long now = now(e);
        Entity direct = src.getDirectEntity(), att = src.getEntity();
        boolean perfectWin = now < d.getLong("w_parry"), guard = now < d.getLong("w_guard"), umbrella = now < d.getLong("w_umb");
        if (direct instanceof Projectile pr && !(pr instanceof com.doomslayer.entity.SlayerProjectile) && (perfectWin || umbrella || guard)) {
            pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.3)); pr.setOwner(e); pr.hurtMarked = true; // deflect it back
            Fx.sparks(L(e), pr.position(), 10); snd(e, SoundEvents.ANVIL_LAND, 0.7f, 2f);
            return 0;
        }
        if (!(att instanceof LivingEntity a) || direct != att) return amt; // melee only below
        if (umbrella) { guardPosture(e, d, amt * 0.3f); snd(e, SoundEvents.SHIELD_BLOCK, 1f, 1.2f); return amt * 0.05f; }
        boolean auto = now >= d.getLong("w_cd") && e.getRandom().nextFloat() < 0.35f; // reflexive deflect
        if (perfectWin || auto) {
            d.putLong("w_cd", now + 14);
            Vec3 c = e.position().add(0, 1.2, 0).add(e.getViewVector(1f).scale(0.8));
            Fx.sparks(L(e), c, 18); Fx.dustBurst(L(e), 0xffd070, 1.2f, c, 10, 0.3); Fx.ring(L(e), c, 1.1f, 0xffe090, 1.4f, 14);
            snd(e, SoundEvents.ANVIL_LAND, 0.9f, 1.9f); snd(e, SoundEvents.IRON_GOLEM_HURT, 0.7f, 1.8f); // clang
            Combat.knock(a, e.position(), 0.5, 0.1); a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
            var ad = a.getPersistentData(); float post = ad.getFloat("w_post") + (perfectWin ? 45 : 28);
            if (post >= 100) { // posture broken: stunned, deathblow available
                ad.putFloat("w_post", 0); ad.putLong("w_stun", now + 90);
                a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 6)); a.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 2));
                Fx.burst(L(e), ParticleTypes.CRIT, a.position().add(0, a.getBbHeight(), 0), 20, 0.4, 0.3); snd(e, SoundEvents.ANVIL_PLACE, 0.8f, 1.3f);
                if (e.getOwner() != null) e.getOwner().displayClientMessage(Component.literal("Posture broken!"), true);
            } else ad.putFloat("w_post", post);
            return 0;
        }
        if (guard) { guardPosture(e, d, amt * 1.5f); snd(e, SoundEvents.SHIELD_BLOCK, 0.9f, 1.4f); Fx.sparks(L(e), e.position().add(0, 1.2, 0), 6); return amt * 0.15f; }
        guardPosture(e, d, amt * 0.8f);
        return amt;
    }

    private static void guardPosture(SlayerEntity e, net.minecraft.nbt.CompoundTag d, float add) {
        float p = d.getFloat("w_post") + add;
        if (p >= 100) { // Wolf's own posture breaks: staggered
            p = 30; e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4)); snd(e, SoundEvents.GLASS_BREAK, 0.8f, 0.8f);
            Fx.burst(L(e), ParticleTypes.CRIT, e.position().add(0, 2, 0), 14, 0.4, 0.2);
        }
        d.putFloat("w_post", p);
    }

    @Override public void idle(Pose p, SlayerEntity e, float age, float m) { p.rArmX = -0.5f + p.rArmX * 0.5f; p.rArmZ = 0.1f; p.rHandX = 0.4f; p.bodyX += 0.05f; p.lArmX = -0.2f + p.lArmX * 0.5f; }

    @Override
    public void passive(SlayerEntity e) { // Healing Gourd + posture recovery
        var d = e.getPersistentData(); float po = d.getFloat("w_post"); if (po > 0) d.putFloat("w_post", Math.max(0, po - (e.getHealth() / e.getMaxHealth() > 0.5f ? 0.2f : 0.1f)));
        if (e.tickCount % 400 == 0 && e.getHealth() < e.getMaxHealth() * 0.5f) {
            e.heal(e.getMaxHealth() * 0.3f); snd(e, SoundEvents.GENERIC_DRINK, 0.8f, 1f); Fx.burst(L(e), ParticleTypes.HAPPY_VILLAGER, e.position().add(0, 1.2, 0), 10, 0.4, 0.05);
        }
    }

    @Override
    protected void defineAttacks(List<AttackDef> out) {
        defineBase(out);
        for (AttackDef a : out) if (a.id.equals("combo_chain")) a.anim = KU.comboAnim(new int[] {5, 16, 27}, 36, new int[] {0, 1, 2});
        for (AttackDef a : out) if (a.id.equals("ichimonji")) a.anim = (p, t, ag) -> KU.strikePose(p, KU.remap(t, 14 / 28f), 0, 1.2f);
        for (AttackDef a : out) if (a.id.equals("ashina_cross")) a.anim = KU.comboAnim(new int[] {8, 14}, 30, new int[] {0, 1});
        for (AttackDef a : out) if (a.id.equals("sabimaru")) a.anim = KU.comboAnim(new int[] {5, 12}, 22, new int[] {0, 1});
        for (AttackDef a : out) if (a.id.equals("kusabimaru")) a.anim = (p, t, ag) -> KU.strikePose(p, KU.remap(t, 5 / 14f), 0, 1f);
    }

    private void defineBase(List<AttackDef> out) {
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


        // ---------- Deflect (hold the blade across the body; the 14-tick window is a perfect parry) ----------
        out.add(AttackDef.of("deflect").range(0, 7).cd(45).weight(3).dur(26).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    var d = e.getPersistentData();
                    if (n == 2) { d.putLong("w_parry", now(e) + 14); d.putLong("w_guard", now(e) + 26); snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.7f, 1.8f); }
                    if (n == 3 || n == 10) Fx.dustBurst(L(e), 0xffffff, 0.8f, e.position().add(0, 1.3, 0).add(e.getViewVector(1f).scale(0.8)), 3, 0.2);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .12f, .85f), sh = 0.03f * (float) Math.sin(a * 9f) * k;
                    // blade vertical across the chest, off-hand steadying the spine, knees bent, tiny shudder on contact
                    p.rArmX = lerp(p.rArmX, -1.25f, k); p.rArmZ = lerp(p.rArmZ, -0.25f, k) + sh; p.rArmY = lerp(p.rArmY, 0.35f, k); p.rHandX = lerp(p.rHandX, -1.2f, k);
                    p.lArmX = lerp(p.lArmX, -1.0f, k); p.lArmZ = lerp(p.lArmZ, 0.25f, k); p.lArmY = lerp(p.lArmY, 0.5f, k);
                    p.bodyY += 0.25f * k; p.headY -= 0.2f * k; p.bodyX += 0.1f * k; stance(p, k); p.rootY -= 0.5f * k;
                    p.rootZ += 0.8f * bump(win(t, .12f, .35f)); // slight recoil when braced
                }));

        // ---------- Loaded Umbrella: blocks everything frontal for a short time ----------
        out.add(AttackDef.of("loaded_umbrella").range(0, 10).cd(160).weight(1).dur(52).weapon("umbrella")
                .tick((e, t, n) -> {
                    if (n == 2) { e.getPersistentData().putLong("w_umb", now(e) + 44); snd(e, SoundEvents.WOOL_PLACE, 1f, 1.8f); }
                    if (n >= 3 && n < 46 && n % 8 == 0) Fx.ring(L(e), e.position().add(0, 1.3, 0).add(e.getViewVector(1f).scale(0.9)), 0.9f, 0xd8c8a0, 1.2f, 12);
                    if (n == 48) snd(e, SoundEvents.WOOL_BREAK, 0.8f, 1.2f);
                })
                .anim((p, t, a) -> {
                    float k = ready(t, .1f, .88f);
                    p.rArmX = lerp(p.rArmX, -1.5f, k); p.rArmZ = lerp(p.rArmZ, -0.1f, k); p.lArmX = lerp(p.lArmX, -1.2f, k); p.lArmZ = lerp(p.lArmZ, 0.3f, k);
                    stance(p, k); p.rootY -= 0.6f * k; p.bodyX += 0.15f * k; p.headX += 0.1f * k;
                }));

        // ---------- Sabimaru: poisoned slashes ----------
        out.add(AttackDef.of("sabimaru").range(0, 3.3f).cd(40).weight(2).dur(22).weapon("sabimaru")
                .tick((e, t, n) -> {
                    if (n == 5 || n == 12) {
                        boolean r = n == 5; slash(e, r ? 70 : -60, r ? -60 : 70, 10, 0x60c040);
                        for (LivingEntity f : Combat.cone(e, 3.3, 55)) { Combat.damage(e, f, 4.5f); f.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1)); Fx.dustBurst(L(e), 0x60c040, 1f, mid(f), 8, 0.3); }
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.5f);
                    }
                }).anim((p, t, a) -> {
                    float s = (float) Math.sin(t * PI * 2f);
                    p.rArmX = -1.4f + 0.4f * Math.abs(s); p.rArmZ = 0.9f * s; p.lArmX = -0.4f; p.bodyY = 0.7f * s; p.headY = -0.35f * s; p.lLegX = -0.4f; p.rLegX = 0.3f;
                }));

        // ---------- Spiral Cloud Passage: leaping spin ----------
        out.add(AttackDef.of("spiral_cloud_passage").range(2, 8).cd(130).weight(2).dur(34).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 3) { Vec3 f = e.getViewVector(1f); e.setDeltaMovement(f.x * 0.6, 0.65, f.z * 0.6); e.hurtMarked = true; snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8f, 1.6f); }
                    if (n >= 9 && n < 26 && n % 4 == 1) {
                        slash(e, 359, 0, 0, 0xffffff);
                        for (LivingEntity f : Combat.around(e, e.position().add(0, 1, 0), 3.4)) { Combat.damage(e, f, 5); Combat.knock(f, e.position(), 0.5, 0.2); Fx.sparks(L(e), mid(f), 4); }
                        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.3f);
                    }
                }).anim((p, t, a) -> {
                    float air = win(t, .08f, .25f) * (1 - win(t, .8f, 1f));
                    p.rootYaw += 6.28f * 3 * win(t, .22f, .75f); p.rootY += 2f * air; p.lLegX = -1.1f * air; p.rLegX = 0.4f * air; p.rArmX = lerp(-0.2f, -1.55f, air); p.rArmZ = 1.2f * air; p.lArmZ = -0.8f * air;
                }));

        // ---------- Dragon Flash: lightning-fast draw, a long piercing line ----------
        out.add(AttackDef.of("dragon_flash").range(4, 16).cd(200).weight(1).dur(30).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n >= 3 && n < 12) Fx.burst(L(e), ParticleTypes.ELECTRIC_SPARK, e.position().add(0, 1.2, 0), 3, 0.4, 0.2);
                    if (n == 14) {
                        Vec3 d = aim(e, t, 0), m = mz(e); Combat.hitscan(e, m, d, 18, 16f, 8, 0xc0e0ff);
                        Vec3 prev = m; for (int i = 1; i <= 9; i++) { Vec3 q = m.add(d.scale(i * 2)).add((e.getRandom().nextDouble() - .5) * .5, (e.getRandom().nextDouble() - .5) * .5, (e.getRandom().nextDouble() - .5) * .5); Fx.beam(L(e), prev, q, i % 2 == 0 ? 0xffffff : 0xa0d0ff, 0.9f, 0.25); prev = q; }
                        snd(e, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5f, 1.6f); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1.4f);
                    }
                }).anim((p, t, a) -> {
                    float w = win(t, .1f, .4f) * (1 - win(t, .5f, .55f));
                    p.rArmX = track(t, 0, -0.2f, .4f, -0.4f, .47f, -1.55f, .7f, -1.5f, 1, -0.2f); p.rArmZ = track(t, 0, 0.1f, .4f, -0.6f, .47f, 0, 1, 0.1f);
                    p.bodyY = track(t, 0, 0, .4f, 0.9f, .47f, -0.5f, 1, 0); p.rootY -= 1.6f * w; p.lLegX = -0.7f * w; p.rLegX = 0.55f * w; p.rootZ = -1f * bump(win(t, .45f, .8f));
                }));

        // ---------- Nightjar Slash: leap high, come down cutting ----------
        out.add(AttackDef.of("nightjar_slash").range(3, 9).cd(120).weight(2).dur(30).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 3) { Vec3 f = e.getViewVector(1f); e.setDeltaMovement(f.x * 0.35, 0.95, f.z * 0.35); e.hurtMarked = true; snd(e, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.5f, 1.8f); }
                    if (n == 13 && t != null) { Vec3 d = t.position().subtract(e.position()).normalize(); e.setDeltaMovement(d.x * 0.9, -1.1, d.z * 0.9); e.hurtMarked = true; }
                    if (n == 18) { slash(e, 0, 0, -80, 0xffffff); melee(e, 3.8, 14, 0.8, 0.3, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.9f); }
                }).anim((p, t, a) -> {
                    float up = win(t, .08f, .3f) * (1 - win(t, .4f, .5f)), dn = bump(win(t, .42f, .75f));
                    p.rArmX = lerp(p.rArmX, -3.0f, up) - 0.2f * dn; p.lLegX = -0.8f * up; p.rLegX = 0.5f * up; p.bodyX += 0.2f * up + 0.5f * dn; p.rootY += 1.5f * up;
                }));

        // ---------- Senpou Leaping Kicks ----------
        out.add(AttackDef.of("leaping_kicks").range(2, 7).cd(100).weight(2).dur(30)
                .tick((e, t, n) -> {
                    if (n == 2) Combat.lunge(e, 0.9, 0.3);
                    if (n == 7 || n == 14 || n == 21) {
                        for (LivingEntity f : Combat.cone(e, 3.3, 50)) { Combat.damage(e, f, n == 21 ? 7 : 4); Combat.knock(f, e.position(), n == 21 ? 1.1 : 0.2, 0.1); Fx.sparks(L(e), mid(f), 5); }
                        snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 1.0f + n * 0.01f);
                    }
                    if (n == 21) { Vec3 f = e.getViewVector(1f); e.setDeltaMovement(-f.x * 0.5, 0.35, -f.z * 0.5); e.hurtMarked = true; }
                }).anim((p, t, a) -> {
                    float air = win(t, .05f, .2f) * (1 - win(t, .8f, 1f)), s = (float) Math.sin(t * PI * 3f);
                    p.rootY += 1.2f * air; p.bodyX -= 0.4f * air; p.rLegX = -1.5f * Math.max(0, s) * air; p.lLegX = -1.5f * Math.max(0, -s) * air; p.rArmX = -0.8f * air; p.lArmX = -0.8f * air;
                }));

        // ---------- Deathblow on a posture-broken enemy (kills regular foes, halves bosses) ----------
        out.add(AttackDef.of("posture_deathblow").range(0, 3.6f).cd(30).weight(8).dur(26).weapon("kusabimaru")
                .when((e, t) -> now(e) < t.getPersistentData().getLong("w_stun"))
                .tick((e, t, n) -> {
                    if (n == 2) Combat.lunge(e, 0.9, 0.02);
                    if (t != null) { t.setDeltaMovement(Vec3.ZERO); t.hurtMarked = true; }
                    if (n == 11 && t != null) {
                        Fx.dustBurst(L(e), 0xb01010, 1.6f, mid(t), 36, 0.45); slash(e, 90, -90, 0, 0xffffff); Fx.ring(L(e), mid(t), 2f, 0xff3030, 1.6f, 20);
                        snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1.3f, 0.5f); snd(e, SoundEvents.WITHER_BREAK_BLOCK, 0.4f, 1.6f);
                        t.getPersistentData().putLong("w_stun", 0);
                        Combat.damage(e, t, t.getMaxHealth() <= 120 ? t.getHealth() + 60 : t.getMaxHealth() * 0.5f);
                    }
                }).anim((p, t, a) -> stab(p, t, 11 / 26f)));

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
        PartDefinition w = rig.weapon(rig.rHand, "sabimaru");
        rig.box(w, Mat.of(0x6a8a60).bevel(.1f), -0.4f, -2.5f, -17, 1, 1, 15); rig.box(w, Mat.of(0x203020), -1.5f, -3, -3, 3, 2, 1); rig.box(w, Mat.of(0x40a030).glow(), -0.5f, -2.5f, -2, 1, 1, 5);
        w = rig.weapon(rig.rHand, "umbrella");
        rig.box(w, Mat.of(0x5a3a22), -0.5f, -2.5f, -14, 1, 1, 16); rig.box(w, Mat.of(0xd8c8a0).noise(.1f), -6, -4, -18, 12, 1, 10); rig.box(w, Mat.of(0xb02020), -4, -5, -17, 8, 1, 8); rig.box(w, Mat.of(0x303030), -1, -5.5f, -14, 2, 1, 2);
        w = rig.weapon(rig.rHand, "kusabimaru");
        rig.box(w, steel, -0.4f, -2.5f, -17, 1, 1, 15); rig.box(w, Mat.of(0x303030), -1.5f, -3, -3, 3, 2, 1); rig.box(w, red, -0.5f, -2.5f, -2, 1, 1, 5);
    }
}
