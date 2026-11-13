package com.doomslayer.entity;

import com.doomslayer.DoomSlayerMod;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Routes "a slayer killed something" to the kit's progression hook. */
@Mod.EventBusSubscriber(modid = DoomSlayerMod.MODID)
public final class KillEvents {
    private KillEvents() {}
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent ev) {
        if (ev.getEntity().level().isClientSide || ev.getEntity() instanceof SlayerEntity) return;
        if (ev.getSource().getEntity() instanceof SlayerEntity s && s.isAlive()) s.kit.onKill(s, ev.getEntity());
    }
}
