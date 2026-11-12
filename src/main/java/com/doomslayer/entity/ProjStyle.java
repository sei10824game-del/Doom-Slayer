package com.doomslayer.entity;

import com.doomslayer.fx.Fx;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Projectile look + behaviour. Create custom ones with ProjStyle.make(...) in a kit's static init; they sync by registration order. */
public class ProjStyle {
    public interface Tick { void run(SlayerProjectile p); }
    public interface Hit { void run(SlayerProjectile p, Vec3 pos, Entity hitEntity); }
    public static final List<ProjStyle> ALL = new ArrayList<>();
    public final int id;
    public int rgb = 0xffffff; public float size = 0.4f;
    public double gravity = 0, drag = 1.0;
    public int life = 80, pierce = 0;
    public float explodeRadius = 0, explodeKb = 0.6f; public boolean bounce; public float homing;
    public ParticleOptions trail;
    public boolean glow = true, visible = true;
    public enum Shape { AUTO, ORB, BOLT, DART, MISSILE, GRENADE, DISC, BEAM, FLASH, BLAST, RING, SLASH, PLASMA, NEEDLE, BFG, FLAME, HEART }
    /** static field name (set at startup by reflection); lets the renderer pick a fitting look per weapon */
    public String name = "";
    private static boolean has(String n, String... k) { for (String x : k) if (n.contains(x)) return true; return false; }
    /** visual-only entity (tracer, beam segment, flash, blast, ring): never collides or damages */
    public boolean visual;
    public ProjStyle visual() { visual = true; return this; }
    public Shape shape = Shape.AUTO;
    public Tick onTick = p -> {};
    public Hit onHit = null; // null -> default impact (sparks / explosion)

    private ProjStyle() { id = ALL.size(); ALL.add(this); }
    public static ProjStyle make() { return new ProjStyle(); }
    public ProjStyle color(int c) { rgb = c; trail = Fx.dust(c, 1f); return this; }
    public ProjStyle size(float s) { size = s; return this; }
    public ProjStyle gravity(double g) { gravity = g; return this; }
    public ProjStyle life(int l) { life = l; return this; }
    public ProjStyle pierce(int n) { pierce = n; return this; }
    public ProjStyle explode(float r) { explodeRadius = r; return this; }
    public ProjStyle trail(ParticleOptions o) { trail = o; return this; }
    public ProjStyle homing(float h) { homing = h; return this; }
    public ProjStyle bounce() { bounce = true; return this; }
    public ProjStyle shape(Shape sh) { shape = sh; return this; }
    /** explicit shape, or one guessed from the style's behaviour */
    public Shape resolved() {
        if (shape != Shape.AUTO) return shape;
        String n = name.toUpperCase();
        boolean warm = ((rgb >> 16) & 255) > 200 && ((rgb >> 8) & 255) < 170 && (rgb & 255) < 100;
        if (!n.isEmpty()) {
            if (has(n, "BFG")) return Shape.BFG;
            if (has(n, "NEEDLE")) return Shape.NEEDLE;
            if (has(n, "NADE", "FRAG", "BOMB", "GL_ROUND", "STICKY", "STUCK", "CHAINBALL", "MINE")) return Shape.GRENADE;
            if (has(n, "ROCKET", "SPNKR", "MISSILE", "MICRO", "HYDRA", "LOCKON", "BARRAGE", "RRTS")) return Shape.MISSILE;
            if (has(n, "PLASMA", "PP_", "PR_", "OVERCHARGE", "ACCEL", "PARRY_BOLT", "PULSE", "ELEC", "LASER")) return Shape.PLASMA;
            if (has(n, "INCIN", "DRAGONFIRE", "FIREBALL", "FIREBOMB") && warm) return Shape.FLAME;
            if (has(n, "SAW", "RIPPER", "SHURIKEN")) return Shape.DISC;
            if (has(n, "HEART")) return Shape.HEART;
            if (has(n, "SPIKE", "KNIFE", "SHARPDART", "SPEAR", "JAVELIN", "SHARD", "SHRAPNEL", "ARROW", "PULV_NO")) return Shape.DART;
        }
        if (explodeRadius > 0 && homing > 0) return Shape.MISSILE;
        if (explodeRadius > 0 && gravity > 0) return Shape.GRENADE;
        if (explodeRadius > 0) return size <= 0.5f ? Shape.MISSILE : Shape.ORB;
        if (pierce >= 3 && bounce && size >= 0.4f) return Shape.DISC;
        if (homing > 0) return Shape.ORB;
        if (size <= 0.3f && (pierce > 0 || gravity > 0)) return Shape.DART;
        if (size <= 0.35f) return Shape.BOLT;
        return Shape.ORB;
    }
    public ProjStyle invisible() { visible = false; return this; }
    public ProjStyle onTick(Tick t) { onTick = t; return this; }
    public ProjStyle onHit(Hit h) { onHit = h; return this; }

    // ---- presets ----
    public static final ProjStyle BULLET = make().color(0xffe080).size(0.12f).life(30).trail(ParticleTypes.CRIT);
    public static final ProjStyle PLASMA = make().color(0x55b8ff).size(0.45f).life(60).trail(ParticleTypes.ELECTRIC_SPARK);
    public static final ProjStyle ROCKET = make().color(0xffa030).size(0.5f).life(100).explode(3.5f).trail(ParticleTypes.SMOKE);
    public static final ProjStyle BFG = make().color(0x55ff55).size(1.6f).life(80).explode(6f).trail(ParticleTypes.HAPPY_VILLAGER);
    public static final ProjStyle FIREBALL = make().color(0xff5a10).size(0.6f).life(70).explode(2f).trail(ParticleTypes.FLAME);
    public static final ProjStyle GRENADE = make().color(0x66aa44).size(0.3f).life(50).explode(3.5f).gravity(0.04).bounce().trail(ParticleTypes.SMOKE);
    public static final ProjStyle LASER = make().color(0xff2020).size(0.3f).life(40).pierce(2).trail(ParticleTypes.END_ROD);
    public static final ProjStyle ARROW = make().color(0xdddddd).size(0.25f).life(80).gravity(0.02).trail(ParticleTypes.CRIT);
    public static final ProjStyle ICE = make().color(0x99e8ff).size(0.5f).life(60).explode(3f).trail(ParticleTypes.SNOWFLAKE);

    // ---- visual-only effect carriers (spawned by Fx / Combat) ----
    public static final ProjStyle VIS_TRACER = make().color(0xffe080).size(0.1f).shape(Shape.BOLT).visual();
    public static final ProjStyle VIS_BEAM = make().color(0xffffff).size(0.2f).shape(Shape.BEAM).visual();
    public static final ProjStyle VIS_FLASH = make().color(0xffe080).size(0.5f).shape(Shape.FLASH).visual();
    public static final ProjStyle VIS_BLAST = make().color(0xff8020).size(2f).shape(Shape.BLAST).visual();
    public static final ProjStyle VIS_SLASH = make().color(0xffffff).size(0.5f).shape(Shape.SLASH).visual();
    public static final ProjStyle VIS_RING = make().color(0xffffff).size(2f).shape(Shape.RING).visual();

    /** give every static ProjStyle field of the kit classes (and their superclasses) its field name */
    public static void nameAll(Iterable<Class<?>> classes) {
        for (Class<?> c0 : classes) for (Class<?> c = c0; c != null && c != Object.class; c = c.getSuperclass())
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (f.getType() != ProjStyle.class || !java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                try { f.setAccessible(true); ProjStyle p = (ProjStyle) f.get(null); if (p != null && p.name.isEmpty()) p.name = f.getName(); } catch (Exception ignored) {}
            }
    }

    public static void defaultImpact(SlayerProjectile p, Vec3 pos, Entity hit) {
        ServerLevel l = (ServerLevel) p.level();
        ProjStyle s = p.style();
        if (s.explodeRadius > 0) {
            Fx.explosion(l, pos, s.explodeRadius);
            Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1f, 1f);
        } else {
            Fx.flash(l, pos, s.rgb, Math.max(0.4f, s.size * 1.6f));
        }
    }
}
