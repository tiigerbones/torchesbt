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
            String displayName,
            Runnable action,
            String successMessage,
            String skipMessage
    ) {
        if (FabricLoader.getInstance().isModLoaded(modId)) {
            action.run();
            RealisticTorchesBT.LOGGER.info("[Compat] {} detected - {}", displayName, successMessage);
        }
    }

    public static void registerChipped() {
        runIfModLoaded(
                "chipped",
                "Chipped",
                ChippedRegistryHandler::register,
                "Registered compat",
                "Skipping compat"
        );
    }

    public static void registerChippedClient() {
        runIfModLoaded(
                "chipped",
                "Chipped",
                ChippedRegistryHandler::registerRenderLayers,
                "Registered clientside compat",
                "Skipping clientside compat"
        );
    }
}
