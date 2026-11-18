package com.doomslayer.kit.kits;

import com.doomslayer.entity.SlayerEntity;
import com.doomslayer.kit.AttackDef;
import com.doomslayer.kit.SlayerKit;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;

/**
 * Original resource systems, bolted onto the attack lists from one place: Zelda magic / arrows / bombs / weapon durability (BotW, TotK),
 * Penitent fervour, Katana ZERO chronos battery, Dead Space stasis charges, Doom 3 soul-cube charge. Values live in the entity's NBT.
 */
public final class ResourceFix {
    private ResourceFix() {}
    /** {max, initial, regenEveryTicks (0 = none), regenAmount} */
    private static final Map<String, int[]> RES = Map.ofEntries(
            ent("magic", 32, 32, 30, 1), ent("arrows", 30, 30, 40, 1), ent("bombs", 10, 10, 200, 1),
            ent("fervour", 100, 30, 0, 0), ent("chron", 10, 10, 80, 1), ent("stasis", 3, 3, 400, 1), ent("cube", 5, 0, 0, 0),
            ent("bullets", 200, 200, 25, 5), ent("shells", 20, 20, 160, 1), ent("rockets", 10, 10, 300, 1), ent("cells", 250, 250, 40, 10),
            ent("shield", 100, 100, 0, 0), ent("battery", 100, 100, 60, 1), ent("zonai", 12, 12, 60, 1), ent("fuel", 3, 2, 600, 1), ent("juice", 20, 20, 40, 1), ent("valor", 100, 0, 0, 0), ent("echo", 6, 6, 40, 1), ent("senergy", 100, 100, 30, 1), ent("musou", 100, 0, 0, 0), ent("plasma", 100, 100, 10, 4), ent("stamina", 100, 100, 10, 6), ent("tools", 6, 6, 600, 1));
    private static Map.Entry<String, int[]> ent(String k, int max, int init, int every, int amt) { return Map.entry(k, new int[] {max, init, every, amt}); }
    private static final Map<String, Object[]> ZELDA = Map.ofEntries(
            e("fire_rod", "magic", 5), e("ice_rod", "magic", 5), e("cane_somaria", "magic", 8), e("bombos", "magic", 16), e("ether", "magic", 16), e("quake", "magic", 16),
            e("din_fire", "magic", 12), e("farore_wind", "magic", 10), e("nayru_love", "magic", 16), e("thunder_spell", "magic", 12), e("fire_spell", "magic", 6), e("life_spell", "magic", 12),
            e("magic_cape", "magic", 12), e("tornado_rod", "magic", 5), e("sand_rod", "magic", 5), e("water_rod", "magic", 5), e("urbosa_fury", "magic", 20), e("revali_gale", "magic", 10),
            e("bow_arrow", "arrows", 1), e("bow_fire", "arrows", 2), e("bow_ice", "arrows", 2), e("bow_shock", "arrows", 2), e("bow_multi", "arrows", 5), e("bow_light", "arrows", 4),
            e("bow_ancient", "arrows", 4), e("bow_bomb", "arrows", 3), e("crossbow_burst", "arrows", 4), e("slingshot", "arrows", 1),
            e("bomb", "bombs", 1), e("remote_bomb", "bombs", 1), e("bombchu", "bombs", 1), e("cannon", "bombs", 2));
    private static Map.Entry<String, Object[]> e(String id, String res, int amt) { return Map.entry(id, new Object[] {res, amt}); }

    public static int get(SlayerEntity e, String res) { var d = e.getPersistentData(); return d.contains("rs_" + res) ? d.getInt("rs_" + res) : RES.getOrDefault(res, new int[] {99, 99, 0, 0})[1]; }
    public static void add(SlayerEntity e, String res, int n) { int[] r = RES.getOrDefault(res, new int[] {99, 99, 0, 0}); e.getPersistentData().putInt("rs_" + res, Math.max(0, Math.min(r[0], get(e, res) + n))); }
    private static int dur(SlayerEntity e, String w) { var d = e.getPersistentData(); return d.contains("rs_dur_" + w) ? d.getInt("rs_dur_" + w) : 30; }

    private static AttackDef gate(AttackDef a, String res, int amt, String say) {
        var old = a.cond; var os = a.start;
        return a.when((e, t) -> get(e, res) >= amt && old.test(e, t)).onStart((e, t, n) -> { os.run(e, t, n); add(e, res, -amt); if (say != null && get(e, res) == 0) Prog.say(e, say, 0xffa040); });
    }
    private static String weaponOf(String id) { return id.startsWith("spear") ? "spear" : id.startsWith("greatsword") ? "greatsword" : id.startsWith("axe") ? "axe" : id.startsWith("club") ? "club" : null; }

    /** Chainsaw (Doom 2016 / Eternal) instantly kills anything but bosses; Berserk power-up one-shots non-bosses in melee */
    public static float outgoing(SlayerEntity e, LivingEntity v, float amt) {
        var cur = e.currentAttack(); if (cur == null || Prog.boss(v)) return amt; String n = e.kit.getClass().getSimpleName();
        if (cur.id.startsWith("chainsaw") && (n.equals("Doom2016Slayer") || n.equals("DoomEternalSlayer"))) return Math.max(amt, v.getHealth() + 5);
        if (cur.maxRange <= 4.5f && e.level().getGameTime() < e.getPersistentData().getLong("ds_berserk")) return Math.max(amt, v.getHealth() + 5);
        return amt;
    }

    public static void apply(SlayerKit kit, List<AttackDef> list) {
        String n = kit.getClass().getSimpleName();
        if (isHalo(n)) for (AttackDef a : list) { // Covenant energy weapons run on an internal battery / overheat: heavy use locks them out until they cool
            String id = a.id; if (id.contains("plasma") && !id.contains("grenade") || id.contains("needler") || id.contains("repeater") || id.contains("beam_rifle")) gate(a, "plasma", id.contains("dual") ? 40 : id.contains("overcharge") ? 50 : 25, null);
        }
        if (n.equals("SkywardSword")) for (AttackDef a : list) switch (a.id) { // stamina gauge: dashing, jumping and spinning drain it; empty = slowed and vulnerable
            case "dash_attack", "jump_slash", "spin_attack", "clawshot" -> { gate(a, "stamina", 25, null); var os = a.start; a.onStart((e, t, k) -> { os.run(e, t, k); if (get(e, "stamina") <= 0) { e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 1)); Prog.say(e, "Out of stamina", 0xf0d060); } }); }
            default -> { }
        }
        if (n.equals("EchoesZelda")) for (AttackDef a : list) switch (a.id) { // echoes cost triangles; the Swordfighter form runs on its own energy
            case "echo_boulder", "echo_tornado", "echo_sword_moblin", "echo_fire_wizzrobe", "echo_ice_wizzrobe" -> gate(a, "echo", 2, "No more echo triangles");
            case "echo_fire_keese", "echo_octorok", "magic_shot" -> gate(a, "echo", 1, null);
            case "swordfighter_form" -> gate(a, "senergy", 60, null);
            case "sword_combo", "dash_attack" -> gate(a, "senergy", 8, "Swordfighter energy spent - collect orbs (kill foes)");
            default -> { }
        }
        if (n.equals("HwLink") || n.equals("HwZelda") || n.equals("HwImpa") || n.equals("HwLana") || n.equals("HwLinkle") || n.equals("AocLink") || n.equals("AocZelda"))
            for (AttackDef a : list) if (a.id.equals("musou")) gate(a, "musou", 80, "Musou gauge empty"); // Warriors: the Special gauge fills in battle
        if (n.equals("Halo4Chief")) { // armour abilities: Hardlight Shield, Autosentry
            list.add(gate(AttackDef.of("hardlight_shield").range(0, 20).cd(260).weight(2).dur(40).noLos()
                    .tick((e, t, k) -> { if (k == 3) { KU.parryWindow(e, 40); e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 40, 2, false, false)); com.doomslayer.fx.Fx.sound((net.minecraft.server.level.ServerLevel) e.level(), e.position(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 1f, 1.8f); }
                        if (k >= 3 && k < 40 && k % 6 == 0) com.doomslayer.fx.Fx.shield((net.minecraft.server.level.ServerLevel) e.level(), e.position().add(0, 1.2, 0).add(e.getViewVector(1f).scale(1.1)), e.getViewVector(1f), 0xff9a30, 1.2f); })
                    .anim((p, t, a) -> { float c = com.doomslayer.anim.Keys.bump(com.doomslayer.anim.Keys.win(t, .05f, .95f)); p.lArmX = -1.4f * c; p.rArmX = -0.8f * c; p.rootY -= 0.3f * c; }), "senergy", 0, null));
            list.add(AttackDef.of("autosentry").range(4, 24).cd(500).weight(1).dur(70)
                    .tick((e, t, k) -> {
                        var d = e.getPersistentData();
                        if (k == 6) { net.minecraft.world.phys.Vec3 f = e.getViewVector(1f); d.putDouble("rs_sx", e.getX() + f.x * 2); d.putDouble("rs_sy", e.getY() + 0.6); d.putDouble("rs_sz", e.getZ() + f.z * 2); com.doomslayer.fx.Fx.flash((net.minecraft.server.level.ServerLevel) e.level(), new net.minecraft.world.phys.Vec3(d.getDouble("rs_sx"), d.getDouble("rs_sy"), d.getDouble("rs_sz")), 0xffa040, 0.8f); }
                        if (k >= 10 && k % 5 == 0) { net.minecraft.world.phys.Vec3 sp = new net.minecraft.world.phys.Vec3(d.getDouble("rs_sx"), d.getDouble("rs_sy"), d.getDouble("rs_sz")); LivingEntity best = null; double bd = 400; for (LivingEntity x : com.doomslayer.fx.Combat.around(e, sp, 20)) { double dd = x.distanceToSqr(sp.x, sp.y, sp.z); if (dd < bd) { bd = dd; best = x; } }
                            if (best != null) { net.minecraft.world.phys.Vec3 dir = best.position().add(0, best.getBbHeight() * 0.6, 0).subtract(sp).normalize(); com.doomslayer.fx.Combat.hitscan(e, sp, dir, 22, 5f, 0, 0xffc060); } }
                    }).anim((p, t, a) -> { float c = com.doomslayer.anim.Keys.bump(com.doomslayer.anim.Keys.win(t, .05f, .25f)); p.rArmX = -0.6f * c; p.bodyX += 0.4f * c; p.rootY -= 0.8f * c; }));
        }
        if (n.equals("HalfLife2Gordon")) list.add(AttackDef.of("gravgun_catch").range(0, 30).noLos().cd(70).weight(6).dur(22).weapon("gravgun") // catch an incoming shot and fling it back
                .when((e, t) -> !e.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, e.getBoundingBox().inflate(8), p -> !(p instanceof com.doomslayer.entity.SlayerProjectile)).isEmpty())
                .tick((e, t, k) -> { if (k >= 4 && k < 14) for (var p : e.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class, e.getBoundingBox().inflate(8), q -> !(q instanceof com.doomslayer.entity.SlayerProjectile))) {
                        net.minecraft.world.phys.Vec3 dir = t != null ? t.position().add(0, t.getBbHeight() * 0.6, 0).subtract(p.position()).normalize() : e.getViewVector(1f); p.setDeltaMovement(dir.scale(Math.max(1.4, p.getDeltaMovement().length() * 1.5))); p.setOwner(e); p.hurtMarked = true;
                        com.doomslayer.fx.Fx.beam((net.minecraft.server.level.ServerLevel) e.level(), e.position().add(0, 1.3, 0), p.position(), 0xffa53a, 0.8f, 0.3); com.doomslayer.fx.Fx.flash((net.minecraft.server.level.ServerLevel) e.level(), p.position(), 0xffa53a, 0.8f); } })
                .anim((p, t, a) -> { float c = com.doomslayer.anim.Keys.bump(com.doomslayer.anim.Keys.win(t, .1f, .9f)); p.rArmX = -1.3f * c; p.lArmX = -1.5f * c; p.bodyX += 0.1f * c; }));
        if (n.equals("HwLink")) { /* weapons: context-based, Link also fights with the Magic Rod and Hammer */ }
        if (n.equals("HaloInfiniteChief")) { // Valor: earned by kills, spent on a UNSC call-in (orbital strike) from the nearest FOB
            list.add(gate(AttackDef.of("valor_orbital_strike").range(6, 26).cd(300).weight(1).dur(44).weapon("ma40")
                    .tick((e, t, k) -> {
                        if (k == 4 && t != null) { Vec3Holder.pos(e, t); }
                        if (k >= 6 && k < 30) { var p = Vec3Holder.pos(e, null); if (p != null) { com.doomslayer.fx.Fx.beam((net.minecraft.server.level.ServerLevel) e.level(), p.add(0, 18, 0), p, 0x60c8ff, 0.5f, 0.4); com.doomslayer.fx.Fx.ring((net.minecraft.server.level.ServerLevel) e.level(), p.add(0, 0.1, 0), 3.5f, 0x60c8ff, 1.4f, 26); } }
                        if (k == 32) { var p = Vec3Holder.pos(e, null); if (p != null) { var l = (net.minecraft.server.level.ServerLevel) e.level(); com.doomslayer.fx.Fx.beam(l, p.add(0, 30, 0), p, 0xffffff, 2.2f, 0.4); com.doomslayer.fx.Fx.explosion(l, p.add(0, 1, 0), 3.4f); for (LivingEntity f : com.doomslayer.fx.Combat.around(e, p, 5.5)) { com.doomslayer.fx.Combat.damage(e, f, 40); com.doomslayer.fx.Combat.knock(f, p, 1.6, 0.6); } com.doomslayer.fx.Fx.sound(l, p, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, 1.4f, 0.6f); } }
                    }).anim((p, t, a) -> { float c = com.doomslayer.anim.Keys.bump(com.doomslayer.anim.Keys.win(t, .1f, .7f)); p.rArmX = -2.4f * c; p.headX -= 0.4f * c; p.bodyX -= 0.15f * c; }), "valor", 60, "Valor spent"));
        }
        if (n.equals("HaloInfiniteChief") || n.contains("Odst") || n.startsWith("Halo")) { /* shield handled in absorb() */ }
        for (AttackDef a : list) {
            if (kit instanceof ZeldaKits) {
                Object[] c = ZELDA.get(a.id); if (c != null) gate(a, (String) c[0], (Integer) c[1], null);
                String w = weaponOf(a.id);
                if (w != null && (n.equals("BotW") || n.equals("TotK"))) { // BotW weapon durability: weapons break
                    var old = a.cond; var os = a.start; String key = "rs_dur_" + w;
                    a.when((e, t) -> dur(e, w) > 0 && old.test(e, t));
                    a.onStart((e, t, k) -> { os.run(e, t, k); int d = dur(e, w) - 1; e.getPersistentData().putInt(key, d); if (d == 0) Prog.say(e, "Your " + w + " broke!", 0xff8040); else if (d == 5) Prog.say(e, "Your " + w + " is about to break", 0xffd060); });
                }
            }
            if (isDoom(n)) { // Doom: ammo pools per weapon class
                String id = a.id;
                if (id.contains("shotgun") || id.equals("super_shotgun") || id.contains("scatter")) gate(a, "shells", id.contains("super") || id.contains("double") ? 2 : 1, null);
                else if (id.contains("rocket") || id.contains("micro") || id.contains("lock_on") || id.contains("missile") || id.contains("spnkr") || id.contains("remote_det")) gate(a, "rockets", 1, null);
                else if (id.contains("bfg")) gate(a, "cells", 100, null);
                else if (id.contains("plasma") || id.contains("heat") || id.contains("microwave") || id.contains("gauss") || id.contains("ballista") || id.contains("precision") || id.contains("vortex") || id.contains("static") || id.contains("unmaker") || id.contains("accelerator") || id.contains("cycler") || id.contains("pulverizer")) gate(a, "cells", 25, null);
                else if (id.contains("chaingun") || id.contains("minigun") || id.contains("machine") || id.contains("assault") || id.contains("full_auto") || id.contains("pistol") || id.contains("turret") || id.contains("har_") || id.contains("scope") || id.equals("pistols_akimbo") || id.contains("heavy_mg")) gate(a, "bullets", 25, null);
            }
            if (n.equals("TotK")) switch (a.id) { // Zonai energy cells power the gadgets
                case "recall", "zonai_beam", "zonai_flame", "rocket_dash" -> gate(a, "zonai", 3, "Zonai energy depleted"); case "ultrahand_throw", "ascend" -> gate(a, "zonai", 2, null); case "fuse_slam" -> gate(a, "zonai", 1, null);
                default -> { }
            }
            if ((n.equals("Doom2016Slayer") || n.equals("DoomEternalSlayer") || n.equals("Doom2005Grimm") || n.equals("BrutalDoomMarine")) && a.id.startsWith("chainsaw")) gate(a, "fuel", 1, null); // chainsaw fuel
            if (n.equals("Omori")) switch (a.id) { // JUICE pays for skills
                case "trip", "stare" -> gate(a, "juice", 3, null); case "sad_poem", "heart", "angry_strike" -> gate(a, "juice", 4, null); case "black_space_hands", "final_strike" -> gate(a, "juice", 7, null); case "happy_song" -> gate(a, "juice", 5, null); case "sad_cry" -> gate(a, "juice", 2, null);
                default -> { }
            }
            if (n.equals("Hornet") && (a.id.equals("tri_pin") || a.id.equals("straight_pin") || a.id.equals("caltrops") || a.id.equals("flintslate"))) gate(a, "tools", 1, "Tools spent - they refill on kills and rest"); // red tools have limited charges
            if (n.startsWith("Blasphemous")) switch (a.id) {
                case "debla_of_the_lights", "taranto_chant" -> gate(a, "fervour", 40, null); case "lorquiana", "tiento_thorns", "blood_miracle" -> gate(a, "fervour", 30, null); case "verdiales_parry" -> gate(a, "fervour", 20, null);
                default -> { }
            }
            if (n.equals("ZeroKatana") && a.id.equals("chronos_flurry")) gate(a, "chron", 5, null);
            if ((n.startsWith("Ds") || n.startsWith("DeadSpace")) && a.id.equals("stasis")) gate(a, "stasis", 1, null);
            if (n.startsWith("Doom") && a.id.equals("soul_cube") && (n.equals("Doom3Marine") || n.equals("DoomRpgMarine") || n.equals("Doom3RoeMarine"))) gate(a, "cube", 5, null);
        }
    }

    static boolean isDoom(String n) { return n.startsWith("Doom") || n.startsWith("Brutal") || n.startsWith("DarkAges"); }
    static boolean isHalo(String n) { return n.startsWith("Halo"); }
    static boolean isHev(String n) { return n.startsWith("HalfLife") || n.startsWith("Entropy"); }

    /** Halo energy shield / HEV battery: soaks damage before health. Returns the damage that still reaches health. */
    public static float absorb(SlayerEntity e, float amt) {
        String n = e.kit.getClass().getSimpleName();
        if (isHalo(n)) {
            int sh = get(e, "shield"); int max = shieldMax(e); if (max <= 0) return amt;
            var d = e.getPersistentData(); if (!d.contains("rs_shield")) sh = max; d.putLong("rs_hitAt", e.level().getGameTime());
            float soak = Math.min(sh, amt); d.putInt("rs_shield", (int) (sh - soak));
            if (sh > 0 && sh - soak <= 0 && e.level() instanceof net.minecraft.server.level.ServerLevel l) { com.doomslayer.fx.Fx.flash(l, e.position().add(0, e.getBbHeight() * 0.6, 0), 0x80c8ff, 1.2f); com.doomslayer.fx.Fx.sound(l, e.position(), net.minecraft.sounds.SoundEvents.GLASS_BREAK, 1f, 1.4f); }
            return amt - soak;
        }
        if (isHev(n)) {
            int bat = get(e, "battery"); float soak = Math.min(bat / 2f, amt * 0.6f); add(e, "battery", -(int) (soak * 2)); return amt - soak;
        }
        return amt;
    }
    /** Spartans: shield = 70% of max health; ODSTs have a recharging stamina instead (40%, slower) */
    static int shieldMax(SlayerEntity e) { return (int) (e.getMaxHealth() * (e.kit.getClass().getSimpleName().contains("Odst") ? 0.4 : 0.7)); }
    /** true while the energy shield still holds something (for the shimmer effect) */
    public static boolean shieldUp(SlayerEntity e) { return isHalo(e.kit.getClass().getSimpleName()) && get(e, "shield") > 0; }

    /** per-tick regeneration (server) */
    public static void tick(SlayerEntity e) {
        String n = e.kit.getClass().getSimpleName(); int t = e.tickCount;
        if (n.equals("TotK")) regen(e, "zonai", t);
        if (e.kit instanceof ZeldaKits) { regen(e, "magic", t); regen(e, "arrows", t); regen(e, "bombs", t); }
        else if (n.equals("ZeroKatana")) regen(e, "chron", t);
        else if (n.equals("SkywardSword")) regen(e, "stamina", t);
        else if (n.equals("EchoesZelda")) { regen(e, "echo", t); regen(e, "senergy", t); }
        else if (n.equals("Omori")) { regen(e, "juice", t); if (e.getHealth() / e.getMaxHealth() < 0.3f && t % 40 == 20) add(e, "juice", 1); } // SAD recovers JUICE faster
        else if (n.equals("Hornet")) regen(e, "tools", t);
        else if (n.startsWith("Ds") || n.startsWith("DeadSpace")) regen(e, "stasis", t);
        else if (isDoom(n)) { regen(e, "fuel", t); regen(e, "bullets", t); regen(e, "shells", t); regen(e, "rockets", t); regen(e, "cells", t); }
        else if (isHev(n)) regen(e, "battery", t);
        if (isHalo(n)) regen(e, "plasma", t);
        if (isHalo(n)) { // shield recharge: starts 5 s after the last hit, refills in ~3 s
            var d = e.getPersistentData(); int max = shieldMax(e); int sh = get(e, "shield"); long delay = n.contains("Odst") ? 200 : 100;
            if (!d.contains("rs_shield")) d.putInt("rs_shield", max);
            else if (sh < max && e.level().getGameTime() - d.getLong("rs_hitAt") > delay) { d.putInt("rs_shield", Math.min(max, sh + Math.max(1, max / (n.contains("Odst") ? 120 : 60)))); if (sh == 0 && e.level() instanceof net.minecraft.server.level.ServerLevel l) com.doomslayer.fx.Fx.sound(l, e.position(), net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.8f); }
        }
    }
    private static void regen(SlayerEntity e, String res, int t) { int[] r = RES.get(res); if (r[2] > 0 && t % r[2] == 0) add(e, res, r[3]); }

    /** a slayer landed a hit: fervour for the Penitent */
    public static void hit(SlayerEntity e) { String hn = e.kit.getClass().getSimpleName(); if (hn.startsWith("Hw") || hn.startsWith("Aoc")) add(e, "musou", 1); if (e.kit.getClass().getSimpleName().startsWith("Blasphemous")) add(e, "fervour", Prog.get(e, "bl_bead") >= 3 ? 5 : 3); }

    /** a slayer killed something: refills */
    public static void kill(SlayerKit kit, SlayerEntity e, LivingEntity v) {
        String n = kit.getClass().getSimpleName();
        if (kit instanceof ZeldaKits) { add(e, "magic", 4); add(e, "arrows", 3); add(e, "bombs", 1); if (Prog.strong(v)) for (String w : new String[] {"spear", "greatsword", "axe", "club"}) e.getPersistentData().putInt("rs_dur_" + w, Math.min(30, dur(e, w) + 8)); }
        if (n.startsWith("Blasphemous")) add(e, "fervour", 8);
        if (n.equals("HaloInfiniteChief")) { add(e, "valor", Prog.strong(v) ? 25 : 6); }
        if (isDoom(n)) { boolean big = Prog.strong(v); add(e, "bullets", big ? 40 : 15); add(e, "shells", big ? 4 : 1); add(e, "rockets", big ? 2 : 0); add(e, "cells", big ? 60 : 15); }
        if (isHev(n)) add(e, "battery", 10);
        if (n.equals("TotK") && Prog.strong(v)) add(e, "zonai", 2);
        if (n.equals("EchoesZelda")) { add(e, "senergy", 20); add(e, "echo", 1); }
        if (n.startsWith("Hw") || n.startsWith("Aoc")) add(e, "musou", 4);
        if (n.equals("ZeroKatana")) add(e, "chron", 1);
        if (n.equals("Omori")) add(e, "juice", 3);
        if (n.equals("Hornet")) add(e, "tools", 1);
        if (n.equals("Doom3Marine") || n.equals("DoomRpgMarine") || n.equals("Doom3RoeMarine")) { add(e, "cube", 1); if (get(e, "cube") == 5) Prog.say(e, "Soul Cube charged", 0xff4a2a); }
    }
}

/** remembers where a call-in was aimed */
final class Vec3Holder {
    static net.minecraft.world.phys.Vec3 pos(SlayerEntity e, LivingEntity t) {
        var d = e.getPersistentData();
        if (t != null) { d.putDouble("rs_cx", t.getX()); d.putDouble("rs_cy", t.getY()); d.putDouble("rs_cz", t.getZ()); d.putBoolean("rs_c", true); }
        return d.getBoolean("rs_c") ? new net.minecraft.world.phys.Vec3(d.getDouble("rs_cx"), d.getDouble("rs_cy"), d.getDouble("rs_cz")) : null;
    }
}
