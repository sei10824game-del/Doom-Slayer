package com.doomslayer.anim;

/** Animation math. t is progress 0..1 of the attack. */
public final class Keys {
    private Keys() {}
    public static final float PI = (float) Math.PI, HALF = PI / 2, DEG = PI / 180f;
    public static float clamp01(float t) { return t < 0 ? 0 : Math.min(t, 1); }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    public static float smooth(float t) { t = clamp01(t); return t * t * (3 - 2 * t); }
    public static float inQuad(float t) { t = clamp01(t); return t * t; }
    public static float outQuad(float t) { t = clamp01(t); return 1 - (1 - t) * (1 - t); }
    public static float outCubic(float t) { t = clamp01(t); float u = 1 - t; return 1 - u * u * u; }
    public static float inCubic(float t) { t = clamp01(t); return t * t * t; }
    public static float outBack(float t) { t = clamp01(t); float c = 1.70158f, u = t - 1; return 1 + (c + 1) * u * u * u + c * u * u; }
    /** 0..1..0 bump */
    public static float bump(float t) { return (float) Math.sin(clamp01(t) * PI); }
    /** progress of t inside window [a,b], clamped 0..1 */
    public static float win(float t, float a, float b) { return clamp01((t - a) / (b - a)); }
    /**
     * Keyframe track: pairs of (time,value), times ascending, smoothstep between keys.
     * track(t, 0,0, .3f,1.2f, .6f,1.2f, 1,0)
     */
    public static float track(float t, float... kv) {
        if (t <= kv[0]) return kv[1];
        for (int i = 2; i < kv.length; i += 2) {
            if (t <= kv[i]) {
                float t0 = kv[i - 2], t1 = kv[i];
                return lerp(kv[i - 1], kv[i + 1], smooth((t - t0) / (t1 - t0)));
            }
        }
        return kv[kv.length - 1];
    }
    /** same as track but linear between keys (snappy hits) */
    public static float trackLin(float t, float... kv) {
        if (t <= kv[0]) return kv[1];
        for (int i = 2; i < kv.length; i += 2) {
            if (t <= kv[i]) return lerp(kv[i - 1], kv[i + 1], (t - kv[i - 2]) / (kv[i] - kv[i - 2]));
        }
        return kv[kv.length - 1];
    }
    /** damped recoil kick starting at time t0 (decays over len) */
    public static float kick(float t, float t0, float len) {
        float u = (t - t0) / len;
        return u < 0 || u > 1 ? 0 : (1 - u) * (1 - u) * (u < 0.15f ? u / 0.15f : 1);
    }
}
