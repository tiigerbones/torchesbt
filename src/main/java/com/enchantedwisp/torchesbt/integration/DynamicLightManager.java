package com.enchantedwisp.torchesbt.integration;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import org.slf4j.Logger;

public class DynamicLightManager {
    private static final Logger LOGGER = RealisticTorchesBT.LOGGER;
    private static boolean isDynamicLightingModLoaded = false;

    public static void init() {
        LOGGER.info("Checking for dynamic lighting mods...");

        isDynamicLightingModLoaded = isLambDynamicLightsLoaded() || isSodiumDynamicLightsLoaded();

        LOGGER.info("Dynamic lighting support: {}", isDynamicLightingModLoaded ? "Enabled" : "Not detected");
    }

    public static boolean isDynamicLightingModLoaded() {
        return isDynamicLightingModLoaded;
    }

    private static boolean isLambDynamicLightsLoaded() {
        try {
            Class.forName("dev.lambdaurora.lambdynlights.LambDynLights");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean isSodiumDynamicLightsLoaded() {
        try {
            Class.forName("toni.sodiumdynamiclights.SodiumDynamicLights");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
