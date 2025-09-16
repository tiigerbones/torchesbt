package com.enchantedwisp.torchesbt.compat;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.compat.chipped.ChippedRegistryHandler;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Handles registration of compatibility items and blocks for other mods.
 */
public class CompatRegistryHandler {

    private static void runIfModLoaded(
            String modId,
            Runnable action,
            String successMessage,
            String skipMessage
    ) {
        if (FabricLoader.getInstance().isModLoaded(modId)) {
            action.run();
            RealisticTorchesBT.LOGGER.info("[Compat] {} detected - {}", modId, successMessage);
        } else {
            RealisticTorchesBT.LOGGER.info("[Compat] {} not detected - {}", modId, skipMessage);
        }
    }

    public static void registerChipped() {
        runIfModLoaded(
                "chipped",
                ChippedRegistryHandler::register,
                "registered burnable compatibility",
                "skipping burnable compatibility"
        );
    }

    public static void registerChippedClient() {
        runIfModLoaded(
                "chipped",
                ChippedRegistryHandler::registerRenderLayers,
                "registered render layers",
                "skipping render layers"
        );
    }
}
