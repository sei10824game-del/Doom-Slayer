package com.doomslayer.client;

import com.doomslayer.DoomSlayerMod;
import com.doomslayer.entity.SlayerEntity;
import com.doomslayer.kit.AttackDef;
import com.doomslayer.kit.Kits;
import com.doomslayer.kit.SlayerKit;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dev QA hook: with DOOMSLAYER_SHOTS=<dir> (and the game opened on a save via -PquickWorld=...), spawns every kit in front of the camera and saves screenshots
 * of the idle pose and of two attacks mid-animation. Optional DOOMSLAYER_ONLY=<id,id> limits the kits.
 */
@Mod.EventBusSubscriber(modid = DoomSlayerMod.MODID, value = Dist.CLIENT)
public final class DevShots {
    private DevShots() {}
    private static final String DIR = System.getenv("DOOMSLAYER_SHOTS"), ONLY = System.getenv("DOOMSLAYER_ONLY");
    private static int world, phase, kitIdx, cnt, atkStep; private static final List<SlayerKit> KITS = new ArrayList<>();
    private static final List<AttackDef> picks = new ArrayList<>();
    private static SlayerEntity cur;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent ev) {
        if (DIR == null || ev.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        var server = mc.getSingleplayerServer();
        if (world < 140) { world++; return; }
        switch (phase) {
            case 0 -> { new File(DIR).mkdirs(); for (SlayerKit k : Kits.ALL) if (ONLY == null || List.of(ONLY.split(",")).contains(k.id)) KITS.add(k); mc.options.hideGui = true; kitIdx = 0; phase = 1; }
            case 1 -> {
                if (kitIdx >= KITS.size()) { System.out.println("DEVSHOTS DONE " + KITS.size()); System.exit(0); }
                SlayerKit kit = KITS.get(kitIdx); picks.clear(); var atks = kit.attacks(); if (System.getenv("DOOMSLAYER_NOATK") == null) picks.add(atks.get(0));
                if (System.getenv("DOOMSLAYER_NOATK") == null) for (AttackDef d : atks) if (d.maxRange > 6 && d.duration >= 16 && d != atks.get(0)) { picks.add(d); break; }
                server.execute(() -> spawn(server, kit)); cnt = 35; phase = 2;
            }
            case 2 -> {
                if (cnt <= 14 && cnt >= 2 && cnt % 3 == 0 && System.getenv("DOOMSLAYER_FXDEMO") != null) server.execute(() -> demo(server));
                if (cnt == 5) for (var en : mc.level.entitiesForRendering()) if (en instanceof com.doomslayer.entity.SlayerProjectile p) System.out.println("DEVSHOTS ENT " + KITS.get(kitIdx).id + " style=" + p.style().resolved() + " name=" + p.style().name + " size=" + p.sizeOv() + " life=" + p.life() + " age=" + p.tickCount + " at " + p.position());
                if (--cnt <= 0) { shot(mc, KITS.get(kitIdx).id + "_idle"); atkStep = 0; cnt = -1; phase = 3; } }
            case 3 -> {
                if (atkStep >= picks.size()) { server.execute(() -> { if (cur != null) cur.discard(); cur = null; }); kitIdx++; phase = 1; return; }
                AttackDef d = picks.get(atkStep);
                if (cnt == -1) { server.execute(() -> { if (cur != null) cur.forceStart(d, null); }); cnt = Math.max(4, (int) (d.duration * 0.45f)) + 3; }
                else if (--cnt == 0) { shot(mc, KITS.get(kitIdx).id + "_atk_" + d.id); atkStep++; cnt = -1; }
            }
            default -> { }
        }
    }

    private static void spawn(net.minecraft.server.MinecraftServer server, SlayerKit kit) {
        ServerLevel l = server.overworld(); var sp = l.getSharedSpawnPos(); double x = sp.getX() + 0.5, z = sp.getZ() + 0.5; double y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sp.getX(), sp.getZ());
        var bp = new net.minecraft.core.BlockPos.MutableBlockPos(); int by = (int) y; // clean studio: stone floor + pale backdrop so models read clearly
        for (int dx = -7; dx <= 7; dx++) for (int dz = -9; dz <= 6; dz++) { l.setBlockAndUpdate(bp.set(sp.getX() + dx, by - 1, sp.getZ() + dz), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState()); for (int dy = 0; dy < 12; dy++) l.setBlockAndUpdate(bp.set(sp.getX() + dx, by + dy, sp.getZ() + dz), dz == 5 ? net.minecraft.world.level.block.Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()); }
        SlayerEntity e = DoomSlayerMod.TYPES.get(kit).get().create(l); e.moveTo(x, y, z, 0f, 0f); e.setNoAi(true); e.setSilent(true); e.setInvulnerable(true); l.addFreshEntity(e); cur = e;
        var pl = server.getPlayerList().getPlayers().get(0); pl.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        double dist = 2.7 + 1.1 * kit.scale; pl.teleportTo(l, x, y + 1.6, z - dist, 0f, 10f); // looking south (+z) at the model; the model faces the camera
        e.setYRot(180f); e.yBodyRot = 180f; e.yHeadRot = 180f; l.setDayTime(6000);
    }

    /** a row of every custom effect, for visual tuning */
    private static void demo(net.minecraft.server.MinecraftServer server) {
        ServerLevel l = server.overworld(); var sp = l.getSharedSpawnPos(); double y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sp.getX(), sp.getZ()); double x0 = sp.getX() + 0.5, z = sp.getZ() + 0.5;
        var V = net.minecraft.world.phys.Vec3.ZERO; java.util.function.BiFunction<Double, Double, net.minecraft.world.phys.Vec3> P = (dx, dy) -> new net.minecraft.world.phys.Vec3(x0 + dx, y + dy, z);
        com.doomslayer.fx.Fx.ring(l, P.apply(-2.6, 0.1), 1.3f, 0xff4040, 1.6f, 24);
        com.doomslayer.fx.Fx.aura(l, P.apply(-1.7, 0.0), 0x70ffa0, 2f, 0.6f, 60);
        com.doomslayer.fx.Fx.bodyShield(l, P.apply(-0.9, 0.0), 0x80c8ff, 2f, 0.7f, 60);
        com.doomslayer.fx.Fx.shield(l, P.apply(0.9, 1.0), new net.minecraft.world.phys.Vec3(0, 0, -1), 0x80c8ff, 0.9f);
        com.doomslayer.fx.Fx.flash(l, P.apply(1.7, 1.0), 0xffe080, 0.9f);
        com.doomslayer.fx.Fx.explosion(l, P.apply(2.6, 1.0), 1.2f);
        com.doomslayer.fx.Fx.beam(l, P.apply(-2.6, 2.4), P.apply(-1.4, 2.9), 0x60c0ff, 1f, 0.3);
        com.doomslayer.fx.Fx.arc(l, P.apply(0.0, 2.4), 0f, 1.0f, 80, -80, 10, 0xffffff, 0.8f, 12);
        com.doomslayer.fx.Fx.burst(l, net.minecraft.core.particles.ParticleTypes.FLAME, P.apply(1.2, 2.4), 12, 0.3, 0.02);
        com.doomslayer.fx.Fx.burst(l, net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, P.apply(1.9, 2.4), 8, 0.3, 0.02);
        com.doomslayer.fx.Fx.burst(l, net.minecraft.core.particles.ParticleTypes.CRIT, P.apply(2.6, 2.4), 12, 0.3, 0.3);
    }

    private static void shot(Minecraft mc, String name) {
        mc.execute(() -> { try { var img = Screenshot.takeScreenshot(mc.getMainRenderTarget()); img.writeToFile(new File(DIR, name + ".png")); img.close(); } catch (Exception ex) { System.out.println("DEVSHOTS fail " + name + " " + ex); } });
    }
}
