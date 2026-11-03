package com.doomslayer;

import com.doomslayer.client.KitClient;
import com.doomslayer.client.ProjectileRenderer;
import com.doomslayer.client.SlayerRenderer;
import com.doomslayer.entity.SlayerEntity;
import com.doomslayer.entity.SlayerProjectile;
import com.doomslayer.kit.Kits;
import com.doomslayer.kit.SlayerKit;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(DoomSlayerMod.MODID)
public class DoomSlayerMod {
    public static final String MODID = "doomslayer";
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final Map<SlayerKit, RegistryObject<EntityType<SlayerEntity>>> TYPES = new LinkedHashMap<>();
    public static final RegistryObject<EntityType<SlayerProjectile>> PROJECTILE = ENTITIES.register("projectile",
            () -> EntityType.Builder.<SlayerProjectile>of(SlayerProjectile::new, MobCategory.MISC).sized(0.3f, 0.3f).clientTrackingRange(8).updateInterval(1).build("projectile"));

    static {
        for (SlayerKit k : Kits.ALL) {
            RegistryObject<EntityType<SlayerEntity>> t = ENTITIES.register(k.id, () -> EntityType.Builder.<SlayerEntity>of((ty, l) -> new SlayerEntity(ty, l, k), MobCategory.CREATURE)
                    .sized(k.width * k.scale, k.height * k.scale).clientTrackingRange(10).build(k.id));
            TYPES.put(k, t);
            ITEMS.register(k.id + "_spawn_egg", () -> new ForgeSpawnEggItem(t, k.eggPrimary, k.eggSecondary, new Item.Properties()) {
                @Override public Component getName(ItemStack s) { return Component.literal(k.name + " (" + k.game + ") Spawn Egg"); }
            });
        }
        TABS.register("slayers", () -> CreativeModeTab.builder().title(Component.literal("Doom Slayer"))
                .icon(() -> new ItemStack(Items.NETHERITE_SWORD))
                .displayItems((p, out) -> ITEMS.getEntries().forEach(i -> out.accept(i.get()))).build());
    }

    public DoomSlayerMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus); ITEMS.register(bus); TABS.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> TYPES.forEach((k, t) -> e.put(t.get(), SlayerEntity.attrs(k).build())));
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Client {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
            for (SlayerKit k : Kits.ALL) e.registerLayerDefinition(KitClient.layerOf(k), () -> KitClient.get(k).layer);
        }
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            TYPES.forEach((k, t) -> e.registerEntityRenderer(t.get(), ctx -> new SlayerRenderer(ctx, k)));
            e.registerEntityRenderer(PROJECTILE.get(), ProjectileRenderer::new);
        }
    }
}
