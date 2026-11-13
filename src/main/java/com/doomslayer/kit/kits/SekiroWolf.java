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
    public boolean preventDeath(SlayerEntity e) { // Resurrection: 2 charges, one more regained every 10 kills (回生)
        var d = e.getPersistentData(); int c = d.contains("w_resc") ? d.getInt("w_resc") : 2;
        if (c <= 0) return false;
        d.putInt("w_resc", c - 1);
        e.setHealth(e.getMaxHealth() * 0.5f);
        e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3));
        Fx.dustBurst(L(e), 0xffffff, 1.4f, e.position().add(0, 1, 0), 30, 0.6); Fx.ring(L(e), e.position().add(0, 0.3, 0), 2.5f, GOLD, 1.5f, 24);
        snd(e, SoundEvents.TOTEM_USE, 0.8f, 0.8f);
        if (e.getOwner() != null) e.getOwner().displayClientMessage(Component.literal("Wolf: Resurrection! (" + (c - 1) + " left)"), true);
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
            Combat.knock(a, e.position(), 0.5, 0.1); a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2)); d.putLong("w_rise", now + 80); // 下り鯉: posture damage up for 4 s
            var ad = a.getPersistentData(); float post = ad.getFloat("w_post") + (perfectWin ? 55 : 34); // 登り鯉: stronger deflect posture damage
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
        float p = d.getFloat("w_post") + add * 0.75f; // 流水
        if (p >= postCap(e)) { // Wolf's own posture breaks: staggered
            p = 30; e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4)); snd(e, SoundEvents.GLASS_BREAK, 0.8f, 0.8f);
            Fx.burst(L(e), ParticleTypes.CRIT, e.position().add(0, 2, 0), 14, 0.4, 0.2);
        }
        d.putFloat("w_post", p);
    }



    // ================= ninjutsu (忍殺忍術) & passive skills (常在効果) =================
    static float reach(SlayerEntity e) { return now(e) < e.getPersistentData().getLong("w_reach") ? 1.5f : 1f; }
    /** 命の呼吸 / 幹の息吹: every deathblow restores some health and posture and refills an emblem */
    static void deathblowBoon(SlayerEntity e) {
        e.heal(e.getMaxHealth() * 0.12f); var d = e.getPersistentData(); d.putFloat("w_post", Math.max(0, d.getFloat("w_post") - 40)); emb(e, 1);
        Fx.dustBurst(L(e), 0x90ff90, 1f, e.position().add(0, 1.2, 0), 10, 0.4);
    }
    /** 派生攻撃・追い斬り / 連ね斬り: a lunging slash straight after a prosthetic tool */
    static void followUp(SlayerEntity e, LivingEntity t) {
        if (t == null || !t.isAlive() || e.distanceTo(t) > 7) return;
        Combat.lunge(e, 0.95, 0.02); slash(e, 85, -85, 15, 0xffffff); artHit(e, 4.2f, 8, 18, 0.8, 0.15, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.1f);
    }
    /** 放ち斬り: the tool's power thrown forward off the blade */
    static void wave(SlayerEntity e, LivingEntity t, int rgb, boolean burn) {
        Vec3 d = aim(e, t, 0), m = mz(e); Combat.hitscan(e, m, d, 9, 9f, 4, rgb); Fx.beam(L(e), m, m.add(d.scale(8)), rgb, 1.1f, 0.3);
        if (burn) for (LivingEntity f : Combat.cone(e, 8, 20)) f.setSecondsOnFire(5);
        snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.7f);
    }

    // ================= progression: prayer beads, attack power, spirit emblems =================
    static int emb(SlayerEntity e) { return e.getPersistentData().getInt("w_emb"); }
    static void emb(SlayerEntity e, int d) { e.getPersistentData().putInt("w_emb", Math.max(0, Math.min(5 + Prog.get(e, "w_rosary"), emb(e) + d))); }
    static float postCap(SlayerEntity e) { return 100 + 25 * Prog.get(e, "w_rosary"); }
    /** gate an art behind spirit emblems (形代) */
    static AttackDef cost(AttackDef a, int c) { if (c <= 0) return a; var old = a.cond; return a.when((e, t) -> emb(e) >= c && old.test(e, t)).onStart((e, t, n) -> emb(e, -c)); }

    /** raise an enemy's posture; breaking it stuns it (deathblow window) */
    static void postDmg(SlayerEntity e, LivingEntity a, float add) {
        var ad = a.getPersistentData(); float post = ad.getFloat("w_post") + add; long now = now(e);
        if (post >= 100) {
            ad.putFloat("w_post", 0); ad.putLong("w_stun", now + 90);
            if (!ad.contains("w_marks")) ad.putInt("w_marks", a.getMaxHealth() >= 300 ? 3 : a.getMaxHealth() >= 150 ? 2 : 1); // bosses need several deathblows
            a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 6)); a.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 90, 2));
            Fx.burst(L(e), ParticleTypes.CRIT, a.position().add(0, a.getBbHeight(), 0), 20, 0.4, 0.3); snd(e, SoundEvents.ANVIL_PLACE, 0.8f, 1.3f);
        } else ad.putFloat("w_post", post);
    }
    /** art hit with posture damage */
    static int artHit(SlayerEntity e, double range, float dmg, float post, double kb, double up, int rgb) {
        int n = 0; for (LivingEntity f : Combat.cone(e, range * reach(e), 60)) { Combat.damage(e, f, dmg); postDmg(e, f, post * (now(e) < e.getPersistentData().getLong("w_rise") ? 1.5f : 1f)); Combat.knock(f, e.position(), kb, up); Fx.dustBurst(L(e), rgb, 1f, mid(f), 6, 0.3); n++; }
        return n;
    }

    @Override
    public void onKill(SlayerEntity e, LivingEntity v) {
        emb(e, 1); // enemies drop spirit emblems
        if (Prog.add(e, "w_kills", 1) % 10 == 0) { var dd = e.getPersistentData(); int rc = dd.contains("w_resc") ? dd.getInt("w_resc") : 2; if (rc < 2) { dd.putInt("w_resc", rc + 1); Prog.say(e, "Resurrection power restored", 0xffffff); } }
        if (Prog.strong(v)) { // mini-boss class: a prayer bead; four make a rosary -> vitality + posture
            int beads = Prog.add(e, "w_bead", 1);
            if (beads % 4 == 0) { Prog.add(e, "w_rosary", 1); Prog.growHp(e, 0.12); Prog.say(e, "Prayer Necklace complete! Vitality and posture up", 0xe0c060); }
            else Prog.say(e, "Prayer Bead " + (beads % 4) + "/4", 0xe0c060);
        }
        if (Prog.boss(v)) { // bosses leave a Memory: attack power up, and a gourd seed
            Prog.growDmg(e, 0.1f); int seeds = Prog.add(e, "w_seed", 1); emb(e, 0); Prog.add(e, "w_rosary", 0);
            Prog.say(e, "Memory gained: Attack Power up (Gourd Seed " + seeds + ")", 0xff8040);
        }
    }

    @Override public void idle(Pose p, SlayerEntity e, float age, float m) { p.rArmX = -0.5f + p.rArmX * 0.5f; p.rArmZ = 0.1f; p.rHandX = 0.4f; p.bodyX += 0.05f; p.lArmX = -0.2f + p.lArmX * 0.5f; }

    @Override
    public void passive(SlayerEntity e) { // posture recovery, emblems, water breathing, ninjutsu upkeep, healing gourd
        var d = e.getPersistentData(); if (!d.contains("w_emb")) d.putInt("w_emb", 3); float po = d.getFloat("w_post"); if (po > 0) d.putFloat("w_post", Math.max(0, po - (e.getHealth() / e.getMaxHealth() > 0.5f ? 0.2f : 0.1f)));
        long now = now(e);
        if (e.tickCount % 500 == 0) emb(e, 1); // slow emblem recovery
        if (e.isInWater()) { e.setAirSupply(e.getMaxAirSupply()); } // 水生の呼吸術: breathe underwater
        // 血煙の術: while the smoke hangs, enemies nearby lose their target
        if (now < d.getLong("w_smoke") && e.tickCount % 5 == 0) for (LivingEntity f : Combat.around(e, e.position(), 7)) { f.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false)); if (f instanceof net.minecraft.world.entity.Mob m && m.getTarget() == e) m.setTarget(null); }
        // 傀儡の術: puppets fight for us until their time runs out
        if (e.tickCount % 5 == 0) for (LivingEntity p : e.level().getEntitiesOfClass(LivingEntity.class, e.getBoundingBox().inflate(24), x -> x.getPersistentData().getLong("w_pup") > 0)) {
            long end = p.getPersistentData().getLong("w_pup");
            if (now >= end) { p.getPersistentData().putLong("w_pup", 0); Fx.dustBurst(L(e), 0xb02020, 1.4f, p.position().add(0, 1, 0), 20, 0.5); p.setHealth(0.5f); Combat.damage(e, p, 200); snd(e, SoundEvents.SOUL_ESCAPE, 0.8f, 0.6f); continue; }
            Fx.dustBurst(L(e), 0xa060e0, 1f, p.position().add(0, p.getBbHeight() * 0.8, 0), 3, 0.3);
            if (p instanceof net.minecraft.world.entity.Mob m && (m.getTarget() == null || m.getTarget() == e || !m.getTarget().isAlive())) {
                LivingEntity best = null; double bd = 400; for (LivingEntity x : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(16), x -> x != p && x != e && x.getPersistentData().getLong("w_pup") == 0 && Combat.isFoe(e, x))) { double dd = x.distanceToSqr(p); if (dd < bd) { bd = dd; best = x; } }
                m.setTarget(best);
            }
        }
        // 傷薬瓢箪: limited charges (3 + gourd seeds, max 10), refilled when the fight is over
        int cap = Math.min(10, 3 + Prog.get(e, "w_seed")); if (!d.contains("w_gourd")) d.putInt("w_gourd", cap);
        if (e.tickCount % 40 == 0) { if (Combat.around(e, e.position(), 14).isEmpty()) { int pc = d.getInt("w_peace") + 1; d.putInt("w_peace", pc); if (pc >= 5) d.putInt("w_gourd", cap); } else d.putInt("w_peace", 0); }
        if (e.tickCount % 20 == 0 && e.getHealth() < e.getMaxHealth() * 0.5f && d.getInt("w_gourd") > 0 && now >= d.getLong("w_gcd")) {
            d.putInt("w_gourd", d.getInt("w_gourd") - 1); d.putLong("w_gcd", now + 100);
            e.heal(e.getMaxHealth() * (0.3f + 0.04f * Prog.get(e, "w_seed"))); snd(e, SoundEvents.GENERIC_DRINK, 0.8f, 1f); Fx.burst(L(e), ParticleTypes.HAPPY_VILLAGER, e.position().add(0, 1.2, 0), 10, 0.4, 0.05);
        }
    }

    @Override
    protected void defineAttacks(List<AttackDef> out) {
        defineBase(out);
        for (AttackDef a : out) if (a.id.equals("combo_chain")) a.anim = KU.comboAnim(new int[] {5, 16, 27}, 36, new int[] {0, 1, 2});
        for (AttackDef a : out) if (a.id.equals("ichimonji")) a.anim = (p, t, ag) -> KU.strikePose(p, KU.remap(t, 14 / 28f), 0, 1.2f);
        for (AttackDef a : out) if (a.id.equals("ashina_cross")) a.anim = KU.comboAnim(new int[] {8, 14}, 30, new int[] {0, 1});
        for (AttackDef a : out) if (a.id.equals("sabimaru")) a.anim = KU.comboAnim(new int[] {5, 12}, 22, new int[] {0, 1});
        for (AttackDef a : out) switch (a.id) {
            case "ashina_cross" -> cost(a, 2); case "spiral_cloud_passage" -> cost(a, 1); case "dragon_flash" -> cost(a, 3); case "nightjar_slash" -> cost(a, 1);
            default -> { }
        }
        for (AttackDef a : out) switch (a.id) { // 派生攻撃: 追い斬り (shuriken, firecracker, spear), 連ね斬り (axe), 放ち斬り (flame vent)
            case "shuriken", "firecracker", "loaded_spear", "loaded_axe" -> a.onEnd((e, t, n) -> followUp(e, t));
            case "flame_vent" -> a.onEnd((e, t, n) -> { wave(e, t, FIRE, true); followUp(e, t); });
            default -> { }
        }
        for (AttackDef a : out) if (a.id.equals("kusabimaru")) a.anim = (p, t, ag) -> KU.strikePose(p, KU.remap(t, 5 / 14f), 0, 1f);
    }

    private void defineBase(List<AttackDef> out) {
        out.add(AttackDef.of("kusabimaru").range(0, 3.2f).cd(12).weight(3).dur(14).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 5) { slash(e, 70, -60, 10, 0xffffff); melee(e, reach(e) * 3.3f, 7, 0.4, 0.1, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.4f); }
                }).anim((p, t, a) -> swing(p, t, 5 / 14f, 1f)));

        // three-hit chain
        out.add(AttackDef.of("combo_chain").range(0, 3.2f).cd(55).weight(2).dur(36).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 5 || n == 16 || n == 27) {
                        boolean r = n != 16; Combat.lunge(e, 0.3, 0); slash(e, r ? 70 : -60, r ? -60 : 70, n == 27 ? 60 : 10, 0xffffff);
                        melee(e, reach(e) * 3.3f, n == 27 ? 10 : 5, n == 27 ? 0.9 : 0.2, 0.05, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.2f + n * 0.01f);
                    }
                }).anim((p, t, a) -> {
                    float s = (float) Math.sin(t * PI * 3f);
                    p.rArmX = -1.4f + 0.4f * Math.abs(s); p.rArmZ = 0.9f * s; p.lArmX = -0.5f; p.bodyY = 0.7f * s; p.headY = -0.35f * s; p.lLegX = -0.4f; p.rLegX = 0.3f; p.rootZ = -1f;
                }));

        // Ichimonji: heavy drawing slash
        out.add(AttackDef.of("ichimonji").range(0, 3.8f).cd(90).weight(2).dur(28).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 4) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 0.8f, 1.4f);
                    if (n == 14) { Combat.lunge(e, 0.8, 0); slash(e, 90, -90, 0, 0xffffff); melee(e, reach(e) * 3.8f, 16, 1.0, 0.25, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.8f); }
                }).anim((p, t, a) -> {
                    float w = win(t, .1f, .45f) * (1 - win(t, .5f, .55f));
                    p.rArmX = track(t, 0, -0.2f, .4f, -0.6f, .5f, -1.5f, .7f, -1.4f, 1, -0.2f); p.rArmZ = track(t, 0, 0.1f, .4f, 1.1f, .55f, -0.8f, 1, 0.1f);
                    p.bodyY = track(t, 0, 0, .4f, 0.9f, .55f, -0.8f, 1, 0); p.headY = -p.bodyY * 0.5f; p.rootY -= 1.5f * w; p.lLegX = -0.6f * w; p.rLegX = 0.5f * w; p.rootZ = -2f * bump(win(t, .4f, .75f));
                }));

        // Ashina Cross: spinning cross-cut
        out.add(AttackDef.of("ashina_cross").range(0, 3.6f).cd(110).weight(1).dur(30).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 8 || n == 14) { slash(e, n == 8 ? 80 : -80, n == 8 ? -80 : 80, n == 8 ? 40 : -40, 0xffffff); melee(e, reach(e) * 3.6f, 8, 0.5, 0.1, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.9f); }
                }).anim((p, t, a) -> { float s = bump(win(t, .15f, .7f)); p.rootYaw += 6.28f * win(t, .2f, .6f); p.rArmX = -1.5f * s; p.rArmZ = 1.2f * s; p.rootY -= 0.5f * s; }));

        // Mortal Draw: iaijutsu counter, lunges and cuts
        out.add(AttackDef.of("mortal_draw").range(2, 7).cd(160).weight(2).dur(26).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 1) e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 14, 4));
                    if (n == 13) { Combat.lunge(e, 1.5, 0); slash(e, 100, -100, 0, GOLD); }
                    if (n == 15) { melee(e, reach(e) * 4f, 20, 1.3, 0.3, GOLD); Fx.ring(L(e), e.position().add(0, 1, 0), 3f, GOLD, 1.6f, 24); snd(e, SoundEvents.GENERIC_EXPLODE, 0.4f, 1.8f); }
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
                    if (n == 14) { slash(e, 80, -80, 30, 0xffffff); melee(e, reach(e) * 3.6f, 10, 0.7, 0.3, 0xffffff); }
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
                    if (n == 18) { slash(e, 0, 0, -80, 0xffffff); melee(e, reach(e) * 3.8f, 14, 0.8, 0.3, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.9f); }
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


        // ================= Combat Arts (流派技) =================
        // 旋風斬り — Whirlwind Slash (unparryable spin)
        out.add(AttackDef.of("whirlwind_slash").range(0, 4f).cd(90).weight(2).dur(30).weapon("kusabimaru")
                .tick((e, t, n) -> { if (n == 8 || n == 13 || n == 18) { slash(e, 359, 0, 0, 0xffffff); artHit(e, 4f, 5, 14, 0.7, 0.2, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.8f + n * 0.02f); } })
                .anim((p, t, a) -> { float c = bump(win(t, .1f, .85f)); p.rootYaw += 6.28f * 2 * win(t, .2f, .75f); p.rArmZ = 1.5f * c; p.rArmX = -0.3f; p.lArmZ = -1.1f * c; p.rootY -= 0.7f * c; }));
        // 奥義・大忍び刺し — Great Shinobi Thrust (spirit emblems 2): skewer, then vault off the enemy
        out.add(cost(AttackDef.of("great_shinobi_thrust").range(0, 4.2f).cd(160).weight(2).dur(30).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 6) Combat.lunge(e, 0.9, 0);
                    if (n == 10 && artHit(e, 4.2, 20, 40, 0.8, 0.15, 0xffffff) > 0) { Vec3 f = e.getViewVector(1f); e.setDeltaMovement(-f.x * 0.45, 0.85, -f.z * 0.45); e.hurtMarked = true; snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1.2f); }
                }).anim((p, t, a) -> { KU.strikePose(p, KU.remap(t, 10 / 30f), 3, 1.2f); float v = win(t, .4f, .7f) * (1 - win(t, .85f, 1f)); p.rootY += 1.5f * v; p.lLegX -= 0.7f * v; }), 2));
        // 寄鷹斬り — Mist Raven: spinning dive into the enemy
        out.add(AttackDef.of("mist_raven_dive").range(3, 9).cd(110).weight(2).dur(30).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 3) { Vec3 f = e.getViewVector(1f); e.setDeltaMovement(f.x * 0.75, 0.5, f.z * 0.75); e.hurtMarked = true; }
                    if (n >= 8 && n < 20 && n % 4 == 0) { slash(e, 359, 0, 20, 0xc0c8e0); artHit(e, 3.6f, 6, 12, 0.5, 0.2, 0xc0c8e0); }
                }).anim((p, t, a) -> { float air = win(t, .08f, .25f) * (1 - win(t, .75f, .95f)); p.rootYaw += 6.28f * 2 * win(t, .2f, .7f); p.rootY += 1.6f * air; p.rArmX = lerp(p.rArmX, -1.55f, air); p.lLegX -= 0.7f * air; p.bodyX += 0.5f * air; }));
        // 奥義・纏い斬り — Empowered slash: imbues the blade with fire (spirit emblem 1)
        out.add(cost(AttackDef.of("imbue_slash").range(0, 6).cd(120).weight(2).dur(24).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n < 8) Fx.burst(L(e), ParticleTypes.FLAME, mz(e), 2, 0.2, 0.01);
                    if (n == 10) { slash(e, 90, -90, 20, FIRE); for (LivingEntity f : Combat.cone(e, 4, 70)) { Combat.damage(e, f, 11); f.setSecondsOnFire(6); postDmg(e, f, 25); } Fx.beam(L(e), mz(e), mz(e).add(e.getViewVector(1f).scale(5)), FIRE, 1.2f, 0.4); snd(e, SoundEvents.BLAZE_SHOOT, 1f, 0.8f); }
                }).anim((p, t, a) -> KU.strikePose(p, KU.remap(t, 10 / 24f), 0, 1.2f)), 1));
        // 一文字・二連 — Ichimonji Double
        out.add(AttackDef.of("ichimonji_double").range(0, 3.8f).cd(130).weight(2).dur(42).weapon("kusabimaru")
                .tick((e, t, n) -> { if (n == 3) Combat.lunge(e, 0.5, 0); if (n == 13 || n == 29) { slash(e, 90, -90, 25, 0xffffff); artHit(e, 3.8f, n == 13 ? 14 : 12, 30, 1.0, 0.3, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.8f); if (n == 29) e.getPersistentData().putFloat("w_post", Math.max(0, e.getPersistentData().getFloat("w_post") - 25)); } })
                .anim(KU.comboAnim(new int[] {13, 29}, 42, new int[] {2, 0})));
        // 拝み連拳 — Praying Strikes (elbow & palm) / 破魔の型 — Exorcism: ends with a heavy back strike
        out.add(AttackDef.of("praying_strikes").range(0, 3f).cd(60).weight(2).dur(24)
                .tick((e, t, n) -> { if (n == 5 || n == 10 || n == 15) { artHit(e, 3f, 4, 14, 0.3, 0.05, 0xffe8c0); snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 0.9f, 1.2f); } })
                .anim(KU.comboAnim(new int[] {5, 10, 15}, 24, new int[] {5, 4, 5})));
        out.add(AttackDef.of("praying_strikes_exorcism").range(0, 3.2f).cd(100).weight(2).dur(36)
                .tick((e, t, n) -> { if (n == 5 || n == 10 || n == 15) { artHit(e, 3f, 4, 14, 0.3, 0.05, 0xffe8c0); snd(e, SoundEvents.PLAYER_ATTACK_STRONG, 0.9f, 1.2f); } if (n == 27) { artHit(e, 3.4f, 12, 40, 1.4, 0.3, 0xffd070); Fx.ring(L(e), e.position().add(0, 1, 0), 2f, 0xffd070, 1.4f, 20); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.7f); } })
                .anim(KU.comboAnim(new int[] {5, 10, 15, 27}, 36, new int[] {5, 4, 5, 3})));
        // 仙峯脚 / 奥義・仙峯寺菩薩脚 — kick, cut, kick (big posture damage)
        out.add(AttackDef.of("senpo_bodhisattva_kick").range(2, 7).cd(130).weight(2).dur(34)
                .tick((e, t, n) -> {
                    if (n == 2) Combat.lunge(e, 0.9, 0.3);
                    if (n == 8 || n == 26) { artHit(e, 3.4f, n == 8 ? 7 : 9, 40, n == 26 ? 1.2 : 0.4, 0.15, 0xffe8c0); snd(e, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 1.0f); }
                    if (n == 17) { slash(e, 80, -80, 0, 0xffffff); artHit(e, 3.6f, 8, 20, 0.4, 0.1, 0xffffff); }
                }).anim((p, t, a) -> { float s = (float) Math.sin(t * PI * 3f), air = win(t, .05f, .2f) * (1 - win(t, .85f, 1f)); p.rootY += 1.2f * air; p.rLegX = -1.6f * Math.max(0, s) * air; p.lLegX = -1.6f * Math.max(0, -s) * air; p.rArmX = lerp(p.rArmX, -1.2f, air * (1 - bump(win(t, .4f, .6f)))); }));
        // 秘伝・大忍び落とし — Shinobi Great Thrust into a plunging spin slash (emblems 2)
        out.add(cost(AttackDef.of("shadowfall").range(3, 9).cd(180).weight(2).dur(40).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n == 5) Combat.lunge(e, 0.8, 0);
                    if (n == 9) artHit(e, 4f, 12, 30, 0.5, 0.2, 0xffffff);
                    if (n == 14) { e.setDeltaMovement(0, 1.0, 0); e.hurtMarked = true; }
                    if (n == 22 && t != null) { Vec3 d = t.position().subtract(e.position()).normalize(); e.setDeltaMovement(d.x * 0.7, -1.3, d.z * 0.7); e.hurtMarked = true; }
                    if (n == 28) { slash(e, 359, 0, -30, 0xffffff); artHit(e, 4f, 18, 50, 1.0, 0.4, 0xffffff); Fx.ring(L(e), e.position().add(0, 0.2, 0), 2.8f, 0xffffff, 1.5f, 22); snd(e, SoundEvents.ANVIL_LAND, 0.8f, 1.3f); }
                }).anim((p, t, a) -> { float air = win(t, .3f, .45f) * (1 - win(t, .72f, .78f)); p.rArmX = lerp(-0.4f, -1.55f, win(t, .1f, .25f)) * (1 - air) + (-3.0f * air); p.rootYaw += 6.28f * win(t, .6f, .72f); p.rootY += 1.8f * air; p.lLegX -= 0.7f * air; }), 2));
        // 奥義・不死斬り — Immortal Severance: a miasma-wreathed sweep (emblems 3)
        out.add(cost(AttackDef.of("immortal_severance").range(3, 11).cd(240).weight(1).dur(36).weapon("kusabimaru")
                .tick((e, t, n) -> {
                    if (n >= 4 && n < 14) Fx.dustBurst(L(e), 0x7a40b0, 1.2f, mz(e), 3, 0.3);
                    if (n == 16) { Vec3 d = aim(e, t, 0), m = mz(e); Combat.hitscan(e, m, d, 12, 22f, 8, 0x9a60e0); for (LivingEntity f : Combat.cone(e, 10, 40)) { f.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1)); postDmg(e, f, 35); } slash(e, 100, -100, 10, 0x9a60e0); Fx.beam(L(e), m, m.add(d.scale(11)), 0x7a40b0, 1.5f, 0.3); snd(e, SoundEvents.WITHER_SHOOT, 0.7f, 0.8f); }
                }).anim((p, t, a) -> KU.strikePose(p, KU.remap(t, 16 / 36f), 0, 1.3f)), 3));
        // 奥義・浮き舟渡り — Floating Passage: a five-cut dance (emblems 2)
        out.add(cost(AttackDef.of("floating_passage").range(0, 3.6f).cd(150).weight(2).dur(40).weapon("kusabimaru")
                .tick((e, t, n) -> { if (n == 4) Combat.lunge(e, 0.5, 0); if (n == 6 || n == 12 || n == 18 || n == 24 || n == 30) { slash(e, n % 12 == 6 ? 80 : -80, n % 12 == 6 ? -80 : 80, 15 + (n % 3) * 20, 0xffffff); artHit(e, 3.6f, n == 30 ? 9 : 5, 20, n == 30 ? 1.0 : 0.3, 0.1, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.2f + n * 0.01f); } })
                .anim(KU.comboAnim(new int[] {6, 12, 18, 24, 30}, 40, new int[] {0, 1, 0, 1, 3})), 2));
        // 秘伝・一心 — Ichishin: draw stance, then a rapid four-cut (emblems 3)
        out.add(cost(AttackDef.of("isshin").range(0, 3.8f).cd(220).weight(1).dur(44).weapon("kusabimaru")
                .tick((e, t, n) -> { if (n == 4) snd(e, SoundEvents.ARMOR_EQUIP_IRON, 1f, 0.7f); if (n == 12) Combat.lunge(e, 1.0, 0); if (n == 14 || n == 19 || n == 24 || n == 31) { slash(e, n % 10 == 4 ? 85 : -85, n % 10 == 4 ? -85 : 85, 10, 0xffe090); artHit(e, 3.8f, n == 31 ? 16 : 7, 28, n == 31 ? 1.4 : 0.4, 0.2, 0xffe090); snd(e, SoundEvents.PLAYER_ATTACK_SWEEP, 1.1f, 1.3f); } })
                .anim(KU.comboAnim(new int[] {14, 19, 24, 31}, 44, new int[] {0, 1, 0, 2})), 3));
        // 霧がらす — Mist Raven tool: a smoke burst that makes enemies lose you
        out.add(AttackDef.of("mist_raven").range(0, 8).cd(300).weight(1).dur(20).noLos()
                .tick((e, t, n) -> { if (n == 6) { Vec3 c = e.position().add(0, 1, 0); Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, c, 28, 1.2, 0.04); Fx.burst(L(e), ParticleTypes.CLOUD, c, 18, 1.0, 0.04); e.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false)); for (LivingEntity f : Combat.around(e, e.position(), 6)) { f.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0)); if (f instanceof net.minecraft.world.entity.Mob m) m.setTarget(null); } snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 0.6f); } })
                .anim((p, t, a) -> { float c = bump(win(t, .1f, .8f)); p.lArmX = -1.4f * c; p.bodyX += 0.2f * c; p.rootY -= 0.5f * c; }));


        // ================= Ninjutsu (忍殺忍術) =================
        // 傀儡の術 — Puppeteer: after the deathblow the victim rises and fights for us for 8 s (5 emblems)
        out.add(cost(AttackDef.of("kugutsu").range(0, 3.6f).cd(60).weight(9).dur(34).weapon("kusabimaru")
                .when((e, t) -> now(e) < t.getPersistentData().getLong("w_stun") && !Prog.boss(t) && emb(e) >= 5 && !Combat.around(e, t.position(), 14).stream().allMatch(x -> x == t))
                .tick((e, t, n) -> {
                    if (n == 1 && t != null) { Vec3 back = Vec3.directionFromRotation(0, t.getYRot()).scale(-1.3); e.teleportTo(t.getX() + back.x, t.getY(), t.getZ() + back.z); }
                    if (t != null) { t.setDeltaMovement(Vec3.ZERO); t.hurtMarked = true; }
                    if (n == 11 && t != null) {
                        slash(e, 90, -90, 0, 0xa060e0); Fx.ring(L(e), mid(t), 2f, 0xa060e0, 1.6f, 20); snd(e, SoundEvents.SOUL_ESCAPE, 1f, 0.5f);
                        var td = t.getPersistentData(); td.putLong("w_stun", 0); td.putLong("w_pup", now(e) + 160);
                        t.setHealth(1f); t.removeAllEffects(); t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 170, 0, false, false)); t.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 1, false, false));
                        LivingEntity best = null; double bd = 400; for (LivingEntity x : Combat.around(e, t.position(), 14)) if (x != t) { double dd = x.distanceToSqr(t); if (dd < bd) { bd = dd; best = x; } }
                        if (t instanceof net.minecraft.world.entity.Mob m) m.setTarget(best);
                    }
                }).anim((p, t, a) -> stab(p, t, 11 / 34f)), 5));
        // 血煙の術 — Bloodsmoke: a red smokescreen; enemies lose track of us (1 emblem)
        out.add(cost(AttackDef.of("bloodsmoke").range(0, 8).cd(260).weight(1).dur(20).noLos()
                .tick((e, t, n) -> { if (n == 5) { Vec3 c = e.position().add(0, 1, 0); Fx.dustBurst(L(e), 0x8a0f0f, 2.4f, c, 30, 1.6); Fx.burst(L(e), ParticleTypes.LARGE_SMOKE, c, 20, 1.4, 0.04); e.getPersistentData().putLong("w_smoke", now(e) + 100); e.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false)); snd(e, SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 0.5f); } })
                .anim((p, t, a) -> { float c = bump(win(t, .1f, .8f)); p.rArmX = -1.8f * c; p.lArmX = -1.0f * c; p.bodyX += 0.25f * c; p.rootY -= 0.6f * c; }), 1));
        // 血刀の術 — Bloodblade: a crimson aura lengthens the blade (reach x1.5, also for arts) for 12 s (1 emblem)
        out.add(cost(AttackDef.of("bloodblade").range(0, 8).cd(300).weight(1).dur(18).noLos().weapon("kusabimaru")
                .tick((e, t, n) -> { if (n == 6) { e.getPersistentData().putLong("w_reach", now(e) + 240); Fx.dustBurst(L(e), 0xc01818, 1.6f, mz(e), 14, 0.5); Fx.ring(L(e), e.position().add(0, 1, 0), 1.8f, 0xc01818, 1.4f, 18); snd(e, SoundEvents.WITHER_SHOOT, 0.5f, 1.6f); } if (n % 4 == 0) Fx.dustBurst(L(e), 0xc01818, 0.9f, mz(e).add(e.getViewVector(1f).scale(1.2)), 2, 0.2); })
                .anim((p, t, a) -> KU.strikePose(p, KU.remap(t, 6 / 18f), 0, 0.8f)), 1));
        // 忍殺 (backstab): an unaware or blinded enemy dies to a stealth deathblow
        out.add(AttackDef.of("stealth_deathblow").range(0, 3.4f).cd(40).weight(9).dur(22).weapon("kusabimaru")
                .when((e, t) -> !Prog.boss(t) && ((t instanceof net.minecraft.world.entity.Mob m && m.getTarget() == null) || t.hasEffect(MobEffects.BLINDNESS)))
                .tick((e, t, n) -> {
                    if (n == 1 && t != null) { Vec3 back = Vec3.directionFromRotation(0, t.getYRot()).scale(-1.2); e.teleportTo(t.getX() + back.x, t.getY(), t.getZ() + back.z); }
                    if (n == 9 && t != null) { Fx.dustBurst(L(e), 0xb01010, 1.4f, mid(t), 26, 0.4); slash(e, 80, -80, 0, 0xffffff); snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 0.6f); Combat.damage(e, t, t.getHealth() + 60); deathblowBoon(e); }
                }).anim((p, t, a) -> stab(p, t, 9 / 22f)));

        // ---------- Deathblow on a posture-broken enemy (kills regular foes, halves bosses) ----------
        out.add(AttackDef.of("posture_deathblow").range(0, 3.6f).cd(30).weight(8).dur(26).weapon("kusabimaru")
                .when((e, t) -> now(e) < t.getPersistentData().getLong("w_stun"))
                .tick((e, t, n) -> {
                    if (n == 1 && t != null) { Vec3 back = Vec3.directionFromRotation(0, t.getYRot()).scale(-1.3); e.teleportTo(t.getX() + back.x, t.getY(), t.getZ() + back.z); } // 崩し裏回り: step behind the broken foe
                    if (n == 2) Combat.lunge(e, 0.4, 0.02);
                    if (t != null) { t.setDeltaMovement(Vec3.ZERO); t.hurtMarked = true; }
                    if (n == 11 && t != null) {
                        Fx.dustBurst(L(e), 0xb01010, 1.6f, mid(t), 36, 0.45); slash(e, 90, -90, 0, 0xffffff); Fx.ring(L(e), mid(t), 2f, 0xff3030, 1.6f, 20);
                        snd(e, SoundEvents.PLAYER_ATTACK_CRIT, 1.3f, 0.5f); snd(e, SoundEvents.WITHER_BREAK_BLOCK, 0.4f, 1.6f);
                        var td = t.getPersistentData(); td.putLong("w_stun", 0); int marks = td.contains("w_marks") ? td.getInt("w_marks") : (t.getMaxHealth() >= 300 ? 3 : t.getMaxHealth() >= 150 ? 2 : 1);
                        if (marks <= 1) { td.remove("w_marks"); Combat.damage(e, t, t.getHealth() + 60); }
                        else { td.putInt("w_marks", marks - 1); Combat.damage(e, t, t.getMaxHealth() * (0.9f / marks)); if (e.getOwner() != null) e.getOwner().displayClientMessage(Component.literal("Deathblow! " + (marks - 1) + " more"), true); }
                        deathblowBoon(e);
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
