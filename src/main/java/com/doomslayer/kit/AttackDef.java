package com.doomslayer.kit;

import com.doomslayer.anim.Anim;
import com.doomslayer.entity.SlayerEntity;
import net.minecraft.world.entity.LivingEntity;

/** One attack of a kit: server logic (tick) + client animation (anim) + selection rules. */
public class AttackDef {
    public interface Tick { void run(SlayerEntity e, LivingEntity target, int t); }
    public interface Cond { boolean test(SlayerEntity e, LivingEntity target); }

    public final String id;
    public int index;
    public float minRange = 0, maxRange = 3;
    public int cooldown = 20, duration = 20, weight = 1;
    public boolean needsLos = true, faceTarget = true, canMove = false;
    /** if > 0 the entity only starts this attack when health fraction <= this (execution/glory kill style) */
    public float maxHealthFrac = 2f;
    public Cond cond = (e, t) -> true;
    public Tick tick = (e, t, n) -> {};
    public Tick start = (e, t, n) -> {};
    public Tick end = (e, t, n) -> {};
    public Anim anim = (p, t, a) -> {};
    public String weaponR, weaponL;

    public AttackDef(String id) { this.id = id; }
    public static AttackDef of(String id) { return new AttackDef(id); }
    public AttackDef range(float min, float max) { minRange = min; maxRange = max; if (max <= 4) needsLos = false; return this; }
    public AttackDef cd(int ticks) { cooldown = ticks; return this; }
    public AttackDef dur(int ticks) { duration = ticks; return this; }
    public AttackDef weight(int w) { weight = w; return this; }
    public AttackDef weapons(String r, String l) { weaponR = r; weaponL = l; return this; }
    public AttackDef weapon(String r) { weaponR = r; return this; }
    public AttackDef when(Cond c) { cond = c; return this; }
    public AttackDef lowHealthTarget(float frac) { maxHealthFrac = frac; return this; }
    public AttackDef moving() { canMove = true; return this; }
    public AttackDef noLos() { needsLos = false; return this; }
    public AttackDef noFace() { faceTarget = false; return this; }
    /** runs every server tick; t = ticks since start (0..duration-1) */
    public AttackDef tick(Tick x) { tick = x; return this; }
    public AttackDef onStart(Tick x) { start = x; return this; }
    public AttackDef onEnd(Tick x) { end = x; return this; }
    public AttackDef anim(Anim a) { anim = a; return this; }
}
