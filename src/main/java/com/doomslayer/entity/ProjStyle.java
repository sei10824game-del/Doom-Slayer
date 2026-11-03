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

    static void defaultImpact(SlayerProjectile p, Vec3 pos, Entity hit) {
        ServerLevel l = (ServerLevel) p.level();
        ProjStyle s = p.style();
        if (s.explodeRadius > 0) {
            Fx.explosion(l, pos, s.explodeRadius);
            Fx.sound(l, pos, SoundEvents.GENERIC_EXPLODE, 1f, 1f);
        } else {
            Fx.sparks(l, pos, 6); Fx.dustBurst(l, s.rgb, 1f, pos, 6, 0.15);
        }
    }
}
