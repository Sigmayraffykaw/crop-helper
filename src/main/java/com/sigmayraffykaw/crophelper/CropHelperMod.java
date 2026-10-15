package com.sigmayraffykaw.crophelper;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CropHelperMod implements ModInitializer {
    public static final String MOD_ID = "crop_helper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Item CROP_HELPER_CORE = registerCore();

    private static Item registerCore() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MOD_ID, "crop_helper_core");
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item item = new Item(new Item.Properties().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Crop Helper loaded. Craft and carry a Crop Helper Core to unlock its features.");
    }
}
