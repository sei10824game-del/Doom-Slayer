package com.doomslayer.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.List;

/** Shelf-packed procedural texture. Rig#box allocates here; bake() paints every box with its Mat. */
public class Atlas {
    public static final int SIZE = 256;
    private record Box(Mat m, int u, int v, int w, int h, int d) {}
    private final List<Box> boxes = new ArrayList<>();
    private int cx, cy, rowH;

    /** @return {u,v} texOffs for a w*h*d cuboid */
    public int[] alloc(Mat m, int w, int h, int d) {
        int bw = 2 * (w + d), bh = d + h;
        if (cx + bw > SIZE) { cx = 0; cy += rowH; rowH = 0; }
        if (cy + bh > SIZE) throw new IllegalStateException("Atlas full (256x256): reuse parts or shrink boxes");
        boxes.add(new Box(m, cx, cy, w, h, d));
        int[] r = {cx, cy}; cx += bw; rowH = Math.max(rowH, bh);
        return r;
    }

    public NativeImage[] bake() {
        Mat.Img g = new Mat.Img(SIZE);
        for (Box b : boxes) {
            int u = b.u, v = b.v, w = b.w, h = b.h, d = b.d;
            b.m.paintFace(g, u + d, v, w, d, Mat.Face.TOP);
            b.m.paintFace(g, u + d + w, v, w, d, Mat.Face.BOTTOM);
            b.m.paintFace(g, u, v + d, d, h, Mat.Face.RIGHT);
            b.m.paintFace(g, u + d, v + d, w, h, Mat.Face.FRONT);
            b.m.paintFace(g, u + d + w, v + d, d, h, Mat.Face.LEFT);
            b.m.paintFace(g, u + 2 * d + w, v + d, w, h, Mat.Face.BACK);
        }
        NativeImage base = new NativeImage(SIZE, SIZE, true), glow = new NativeImage(SIZE, SIZE, true);
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            base.setPixelRGBA(x, y, abgr(g.base[y * SIZE + x]));
            glow.setPixelRGBA(x, y, abgr(g.glow[y * SIZE + x]));
        }
        return new NativeImage[]{base, glow};
    }
    private static int abgr(int argb) {
        return (argb & 0xff000000) | ((argb & 0xff) << 16) | (argb & 0xff00) | ((argb >> 16) & 0xff);
    }
}
