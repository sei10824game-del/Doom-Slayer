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
        e.getDispatcher().register(root);
    }
}
