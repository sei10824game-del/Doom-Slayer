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
    private static final Map<String, int[]> RES = Map.of(
            "magic", new int[] {32, 32, 30, 1}, "arrows", new int[] {30, 30, 40, 1}, "bombs", new int[] {10, 10, 200, 1},
            "fervour", new int[] {100, 30, 0, 0}, "chron", new int[] {10, 10, 80, 1}, "stasis", new int[] {3, 3, 400, 1}, "cube", new int[] {5, 0, 0, 0});
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

    public static void apply(SlayerKit kit, List<AttackDef> list) {
        String n = kit.getClass().getSimpleName();
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
            if (n.startsWith("Blasphemous")) switch (a.id) {
                case "debla_of_the_lights", "taranto_chant" -> gate(a, "fervour", 40, null); case "lorquiana", "tiento_thorns", "blood_miracle" -> gate(a, "fervour", 30, null); case "verdiales_parry" -> gate(a, "fervour", 20, null);
                default -> { }
            }
            if (n.equals("ZeroKatana") && a.id.equals("chronos_flurry")) gate(a, "chron", 5, null);
            if ((n.startsWith("Ds") || n.startsWith("DeadSpace")) && a.id.equals("stasis")) gate(a, "stasis", 1, null);
            if (n.startsWith("Doom") && a.id.equals("soul_cube") && (n.equals("Doom3Marine") || n.equals("DoomRpgMarine") || n.equals("Doom3RoeMarine"))) gate(a, "cube", 5, null);
        }
    }

    /** per-tick regeneration (server) */
    public static void tick(SlayerEntity e) {
        String n = e.kit.getClass().getSimpleName(); int t = e.tickCount;
        if (e.kit instanceof ZeldaKits) { regen(e, "magic", t); regen(e, "arrows", t); regen(e, "bombs", t); }
        else if (n.equals("ZeroKatana")) regen(e, "chron", t);
        else if (n.startsWith("Ds") || n.startsWith("DeadSpace")) regen(e, "stasis", t);
    }
    private static void regen(SlayerEntity e, String res, int t) { int[] r = RES.get(res); if (r[2] > 0 && t % r[2] == 0) add(e, res, r[3]); }

    /** a slayer landed a hit: fervour for the Penitent */
    public static void hit(SlayerEntity e) { if (e.kit.getClass().getSimpleName().startsWith("Blasphemous")) add(e, "fervour", 3); }

    /** a slayer killed something: refills */
    public static void kill(SlayerKit kit, SlayerEntity e, LivingEntity v) {
        String n = kit.getClass().getSimpleName();
        if (kit instanceof ZeldaKits) { add(e, "magic", 4); add(e, "arrows", 3); add(e, "bombs", 1); if (Prog.strong(v)) for (String w : new String[] {"spear", "greatsword", "axe", "club"}) e.getPersistentData().putInt("rs_dur_" + w, Math.min(30, dur(e, w) + 8)); }
        if (n.startsWith("Blasphemous")) add(e, "fervour", 8);
        if (n.equals("ZeroKatana")) add(e, "chron", 1);
        if (n.equals("Doom3Marine") || n.equals("DoomRpgMarine") || n.equals("Doom3RoeMarine")) { add(e, "cube", 1); if (get(e, "cube") == 5) Prog.say(e, "Soul Cube charged", 0xff4a2a); }
    }
}
