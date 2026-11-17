package com.doomslayer;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Slayer-vs-slayer damage settings. Edit doomslayer-common.toml or use /doomslayer <rivals|friendlyfire> <true|false>. */
@Mod.EventBusSubscriber(modid = DoomSlayerMod.MODID)
public final class Rules {
    private Rules() {}
    public static final ForgeConfigSpec SPEC;
    /** slayers with different owners (or unowned) fight and hurt each other */
    public static final ForgeConfigSpec.BooleanValue RIVALS;
    /** slayers with the same owner also fight and hurt each other */
    public static final ForgeConfigSpec.BooleanValue FRIENDLY_FIRE;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        RIVALS = b.comment("Slayers with different owners (or no owner) fight and damage each other").define("rivals", true);
        FRIENDLY_FIRE = b.comment("Slayers with the same owner also fight and damage each other").define("friendlyFire", false);
        SPEC = b.build();
    }

    /** CI/dev hook: DOOMSLAYER_SELFTEST=1 runs the self-test right after the server starts, then stops it */
    @SubscribeEvent
    public static void started(net.minecraftforge.event.server.ServerStartedEvent ev) {
        if (System.getenv("DOOMSLAYER_SELFTEST") == null) return;
        selftest(ev.getServer().createCommandSourceStack().withLevel(ev.getServer().overworld()));
        ev.getServer().halt(false);
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        var root = Commands.literal("doomslayer").requires(s -> s.hasPermission(2));
        for (var v : new ForgeConfigSpec.BooleanValue[] {RIVALS, FRIENDLY_FIRE}) {
            String name = v == RIVALS ? "rivals" : "friendlyfire";
            root.then(Commands.literal(name)
                    .executes(c -> { c.getSource().sendSuccess(() -> Component.literal(name + " = " + v.get()), false); return 1; })
                    .then(Commands.argument("value", BoolArgumentType.bool()).executes(c -> {
                        boolean val = BoolArgumentType.getBool(c, "value"); v.set(val);
                        c.getSource().sendSuccess(() -> Component.literal(name + " = " + val), true); return 1;
                    })));
        }
        root.then(Commands.literal("selftest").executes(c -> selftest(c.getSource())));
        e.getDispatcher().register(root);
    }

    /** Runs every attack of every kit against a dummy target, plus every animation over t=0..1, and reports exceptions / NaN poses. */
    static int selftest(net.minecraft.commands.CommandSourceStack src) {
        var l = src.getLevel(); var sp = l.getSharedSpawnPos(); int y = l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, sp.getX(), sp.getZ());
        net.minecraft.world.phys.Vec3 base = new net.minecraft.world.phys.Vec3(sp.getX() + 0.5, y, sp.getZ() + 0.5);
        java.util.List<String> fails = new java.util.ArrayList<>(); java.util.List<Object[]> heavy = new java.util.ArrayList<>(); int attacks = 0, kits = 0;
        for (var kit : com.doomslayer.kit.Kits.ALL) {
            kits++;
            com.doomslayer.entity.SlayerEntity e = DoomSlayerMod.TYPES.get(kit).get().create(l);
            net.minecraft.world.entity.monster.Zombie z = net.minecraft.world.entity.EntityType.ZOMBIE.create(l);
            e.moveTo(base.x, base.y, base.z); z.moveTo(base.x + 3.5, base.y, base.z); z.setNoAi(true);
            z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(5000); z.setHealth(5000);
            l.addFreshEntity(e); l.addFreshEntity(z);
            try { // the model must build and its 256x256 atlas must fit (an overflow would crash the client when the entity first renders)
                var mesh = new net.minecraft.client.model.geom.builders.MeshDefinition(); var rg = new com.doomslayer.client.Rig(mesh); kit.buildModel(rg);
                int rows = rg.atlas.usedRows(); System.out.println("SELFTEST ATLAS " + rows + "/256 " + kit.id);
                if (rows > 245) fails.add(kit.id + " atlas nearly full: " + rows + "/256");
                var imgs = rg.atlas.bake(); for (var im : imgs) im.close();
            } catch (Throwable ex) { fails.add(kit.id + " MODEL BUILD: " + ex); }
            try { kit.passive(e); e.tick(); } catch (Throwable ex) { fails.add(kit.id + " passive/tick: " + ex); }
            try { kit.modifyDamage(e, e.damageSources().mobAttack(z), 5f); } catch (Throwable ex) { fails.add(kit.id + " modifyDamage: " + ex); }
            try { e.setHealth(1f); boolean r = kit.preventDeath(e); e.setHealth(e.getMaxHealth()); } catch (Throwable ex) { fails.add(kit.id + " preventDeath: " + ex); }
            try { var m = new com.doomslayer.anim.Pose(); kit.idle(m, e, 10f, 0.5f); } catch (Throwable ex) { fails.add(kit.id + " idle: " + ex); }
            for (var d : kit.attacks()) {
                attacks++;
                try {
                    e.setHealth(e.getMaxHealth()); z.setHealth(z.getMaxHealth()); e.setTarget(z); e.forceStart(d, z);
                    for (var en : toList(l.getAllEntities())) if (en instanceof com.doomslayer.entity.SlayerProjectile) en.discard();
                    for (int i = 0; i < d.duration + 2; i++) e.tick();
                    int made = 0; for (var en : l.getAllEntities()) if (en instanceof com.doomslayer.entity.SlayerProjectile) made++;
                    heavy.add(new Object[] {made * 10 / (d.duration + 2), kit.id + "." + d.id + " (" + made + " in " + d.duration + " ticks)"});
                    e.forceStart(d, null); for (int i = 0; i < d.duration + 2; i++) e.tick(); // target died mid-attack
                    for (int k = 0; k <= 20; k++) {
                        var p = new com.doomslayer.anim.Pose(); d.anim.apply(p, k / 20f, 12.5f + k);
                        for (var fld : p.getClass().getFields()) if (fld.getType() == float.class) { float v = fld.getFloat(p); if (Float.isNaN(v) || Float.isInfinite(v)) { fails.add(kit.id + "." + d.id + " anim NaN in " + fld.getName() + " @t=" + k / 20f); break; } }
                    }
                } catch (Throwable ex) { fails.add(kit.id + "." + d.id + ": " + ex + " @ " + (ex.getStackTrace().length > 0 ? ex.getStackTrace()[0] : "")); }
                e.interruptAttack(); z.removeAllEffects(); e.removeAllEffects();
            }
            for (var ent : toList(l.getAllEntities()))
                if (ent instanceof com.doomslayer.entity.SlayerProjectile || ent == e || ent == z) ent.discard();
        }
        heavy.sort((a, b) -> (Integer) b[0] - (Integer) a[0]); for (int i = 0; i < Math.min(15, heavy.size()); i++) System.out.println("SELFTEST HEAVY " + heavy.get(i)[0] + " effect-entities per 10 ticks: " + heavy.get(i)[1]);
        for (String s : fails) System.out.println("SELFTEST FAIL " + s);
        System.out.println("SELFTEST DONE kits=" + kits + " attacks=" + attacks + " fails=" + fails.size());
        int n = fails.size(); src.sendSuccess(() -> Component.literal("selftest: " + n + " failures (see log)"), false);
        return n;
    }
    private static java.util.List<net.minecraft.world.entity.Entity> toList(Iterable<net.minecraft.world.entity.Entity> it) { var l = new java.util.ArrayList<net.minecraft.world.entity.Entity>(); for (var o : it) l.add(o); return l; }
}
