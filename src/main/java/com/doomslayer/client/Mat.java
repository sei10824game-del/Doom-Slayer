package com.doomslayer.client;

/** Procedural material painted onto the dynamic texture. Chain setters, e.g. Mat.of(0x3a5a2a).noise(.12f).bevel(.25f). */
public class Mat {
    public enum Face { TOP, BOTTOM, RIGHT, FRONT, LEFT, BACK }
    public interface Decal { void paint(Mat m, Img g, int x, int y, int w, int h, Face f); }

    public int base;
    public float noise = 0.08f, bevel = 0.18f, grad = 0.12f;
    public boolean glow;
    public Decal decal;
    public int seed;

    public static Mat of(int rgb) { Mat m = new Mat(); m.base = rgb; m.seed = rgb * 31 + 7; return m; }
    public Mat noise(float n) { noise = n; return this; }
    public Mat bevel(float b) { bevel = b; return this; }
    public Mat grad(float g) { grad = g; return this; }
    /** emissive: also drawn on the full-bright layer */
    public Mat glow() { glow = true; bevel = 0; noise = 0.04f; return this; }
    public Mat decal(Decal d) { decal = d; return this; }

    static int mul(int rgb, float k) {
        int r = Math.min(255, Math.max(0, (int) (((rgb >> 16) & 255) * k)));
        int g = Math.min(255, Math.max(0, (int) (((rgb >> 8) & 255) * k)));
        int b = Math.min(255, Math.max(0, (int) ((rgb & 255) * k)));
        return r << 16 | g << 8 | b;
    }
    public static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }
    static float hash(int x, int y, int s) {
        int h = x * 374761393 + y * 668265263 + s * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177; h ^= h >>> 16;
        return (h & 0xffff) / 65535f;
    }

    void paintFace(Img g, int x, int y, int w, int h, Face f) {
        float faceK = switch (f) { case TOP -> 1.12f; case BOTTOM -> 0.6f; case FRONT -> 1.0f; case BACK -> 0.85f; default -> 0.92f; };
        for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) {
            float k = faceK * (1 + grad * (0.5f - (h <= 1 ? 0.5f : j / (float) (h - 1))));
            k *= 1 + noise * (hash(x + i, y + j, seed) - 0.5f) * 2;
            boolean edge = i == 0 || j == 0 || i == w - 1 || j == h - 1;
            if (edge && w > 2 && h > 2) k *= 1 - bevel;
            int c = mul(base, k);
            g.px(x + i, y + j, c);
            if (glow) g.glow(x + i, y + j, c);
        }
        if (decal != null) decal.paint(this, g, x, y, w, h, f);
    }

    /** simple image holder (base + emissive), used by decals: g.px(x,y,rgb) / g.glow(x,y,rgb) */
    public static class Img {
        public final int size; public final int[] base, glow;
        public Img(int size) { this.size = size; base = new int[size * size]; glow = new int[size * size]; }
        public void px(int x, int y, int rgb) { if (x >= 0 && y >= 0 && x < size && y < size) base[y * size + x] = 0xff000000 | rgb; }
        public void glow(int x, int y, int rgb) { if (x >= 0 && y >= 0 && x < size && y < size) { glow[y * size + x] = 0xff000000 | rgb; base[y * size + x] = 0xff000000 | rgb; } }
        public void rect(int x, int y, int w, int h, int rgb) { for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) px(x + i, y + j, rgb); }
        public void rectGlow(int x, int y, int w, int h, int rgb) { for (int j = 0; j < h; j++) for (int i = 0; i < w; i++) glow(x + i, y + j, rgb); }
    }
}
