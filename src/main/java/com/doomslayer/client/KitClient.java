package com.doomslayer.client;

import com.doomslayer.DoomSlayerMod;
import com.doomslayer.kit.SlayerKit;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Memoises the baked rig (layer definition + procedural textures) per kit. */
public final class KitClient {
    public static final class Baked {
        public LayerDefinition layer; public Rig rig; public ResourceLocation tex, glowTex;
    }
    private static final Map<SlayerKit, Baked> CACHE = new HashMap<>();
    public static ModelLayerLocation layerOf(SlayerKit k) { return new ModelLayerLocation(new ResourceLocation(DoomSlayerMod.MODID, k.id), "main"); }

    public static synchronized Baked get(SlayerKit k) {
        return CACHE.computeIfAbsent(k, kit -> {
            Baked b = new Baked();
            MeshDefinition mesh = new MeshDefinition();
            b.rig = new Rig(mesh);
            kit.buildModel(b.rig);
            b.layer = LayerDefinition.create(mesh, Atlas.SIZE, Atlas.SIZE);
            return b;
        });
    }
    /** must run on render thread */
    public static void ensureTextures(SlayerKit k) {
        Baked b = get(k);
        if (b.tex != null) return;
        NativeImage[] imgs = b.rig.atlas.bake();
        b.tex = Minecraft.getInstance().getTextureManager().register("doomslayer_" + k.id, new DynamicTexture(imgs[0]));
        b.glowTex = Minecraft.getInstance().getTextureManager().register("doomslayer_" + k.id + "_g", new DynamicTexture(imgs[1]));
    }
}
