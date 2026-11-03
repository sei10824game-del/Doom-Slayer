package com.doomslayer.anim;

@FunctionalInterface
public interface Anim {
    /** @param t attack progress 0..1 (smooth, includes partial tick)  @param age entity age in ticks (for oscillation) */
    void apply(Pose p, float t, float age);
}
