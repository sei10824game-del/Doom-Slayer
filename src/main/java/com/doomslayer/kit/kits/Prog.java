package com.doomslayer.kit.kits;

import com.doomslayer.entity.SlayerEntity;
import com.doomslayer.fx.Fx;
import com.doomslayer.kit.SlayerKit;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Shared progression helpers: tough kills drop beads / shards / nodes; boss kills raise attack power. Values persist in the entity's NBT. */
public final class Prog {
    private Prog() {}
    /** "strong enemy" (mini-boss class) and "boss" thresholds, by max health */
    static boolean strong(LivingEntity v) { return v.getMaxHealth() >= 50 || v.getType() == EntityType.WITHER || v.getType() == EntityType.WARDEN; }
    static boolean boss(LivingEntity v) { return v.getMaxHealth() >= 150 || v.getType() == EntityType.WITHER || v.getType() == EntityType.ENDER_DRAGON || v.getType() == EntityType.WARDEN || v.getType() == EntityType.ELDER_GUARDIAN; }
    static int get(SlayerEntity e, String k) { return e.getPersistentData().getInt(k); }
    static int add(SlayerEntity e, String k, int n) { int v = get(e, k) + n; e.getPersistentData().putInt(k, v); return v; }
    /** permanent max-health growth (+frac of current max), healing by the same amount */
    static void growHp(SlayerEntity e, double frac) {
        var a = e.getAttribute(Attributes.MAX_HEALTH); if (a == null) return;
        double old = a.getBaseValue(); a.setBaseValue(old * (1 + frac)); e.heal((float) (old * frac));
    }
    static void growDmg(SlayerEntity e, float add) { e.getPersistentData().putFloat("ds_dmgx", e.dmgScale() + add); }
    static void say(SlayerEntity e, String msg, int rgb) {
        if (e.getOwner() != null) e.getOwner().displayClientMessage(Component.literal(e.kit.name + ": " + msg), true);
        if (e.level() instanceof net.minecraft.server.level.ServerLevel l) { Fx.aura(l, e.position(), rgb, e.getBbHeight() * 1.1f, e.getBbWidth(), 26); Fx.flash(l, e.position().add(0, e.getBbHeight() * 0.7, 0), rgb, 0.8f); Fx.sound(l, e.position(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.6f, 1.4f); }
    }
    /** pieces (shards / fragments / beads): every 'per' pieces grants a permanent HP boost */
    static boolean piece(SlayerEntity e, String key, int per, double hpFrac, String what, int rgb) {
        int n = add(e, key, 1);
        if (n % per == 0) { growHp(e, hpFrac); say(e, what + " complete! Max HP up", rgb); return true; }
        say(e, what + " " + (n % per) + "/" + per, rgb); return false;
    }

    /** default per-family progression; kits with their own rules override SlayerKit#onKill */
    public static void familyKill(SlayerKit k, SlayerEntity e, LivingEntity v) {
        String n = k.getClass().getSimpleName();
        if (n.startsWith("Doom") || n.startsWith("Brutal") || n.startsWith("DarkAges")) { // Praetor suit / Argent upgrades from tough demons
            if (strong(v) && add(e, "pg_pts", 1) % 3 == 0) { growHp(e, 0.08); growDmg(e, 0.05f); say(e, "Praetor upgrade! (HP +8%, damage +5%)", 0xff6020); }
        } else if (n.startsWith("Ds") || n.startsWith("DeadSpace")) { // power nodes
            if (strong(v) && add(e, "pg_pts", 1) % 4 == 0) { growHp(e, 0.07); growDmg(e, 0.06f); say(e, "Power node used (suit + weapon upgrade)", 0x4ad8ff); }
        } else if (n.startsWith("Blasphemous")) { // flask / sword hearts
            if (strong(v) && add(e, "pg_pts", 1) % 3 == 0) { growHp(e, 0.07); say(e, "Flask empowered", 0xa01020); }
            if (boss(v)) { growDmg(e, 0.08f); say(e, "Mea Culpa heart: damage up", 0xf0c060); }
        } else if (n.startsWith("Halo")) { // commendations: shield/armour proficiency
            if (strong(v) && add(e, "pg_pts", 1) % 5 == 0) { growDmg(e, 0.04f); growHp(e, 0.05); say(e, "Commendation earned", 0x60c8ff); }
        }
    }
}
