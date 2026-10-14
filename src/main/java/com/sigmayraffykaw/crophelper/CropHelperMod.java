package com.sigmayraffykaw.crophelper;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CropHelperMod implements ModInitializer {
    public static final String MOD_ID = "crop_helper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Crop Helper starter loaded. Farming automation is not implemented yet.");
    }
}
