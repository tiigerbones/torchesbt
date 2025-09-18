package com.enchantedwisp.torchesbt.compat;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.compat.chipped.ChippedRegistryHandler;
import com.enchantedwisp.torchesbt.compat.chipped.blockentity.ChippedModBlockEntities;
import com.enchantedwisp.torchesbt.registry.BurnableRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
                () -> {
                    ChippedRegistryHandler.register();           // blocks/items
                    ChippedModBlockEntities.register();         // block entity type

                    // Delay burnables + block linking until server is ready
                    ServerLifecycleEvents.SERVER_STARTED.register(server -> {
                        // Snapshot counts before registering Chipped burnables
                        int beforeItems = BurnableRegistry.getBurnableItemsCount();
                        int beforeBlocks = BurnableRegistry.getBurnableBlocksCount();

                        ChippedRegistryHandler.registerBurnables();
                        ChippedModBlockEntities.linkBlocks();

                        // Calculate exactly how many were added
                        int addedItems = BurnableRegistry.getBurnableItemsCount() - beforeItems;
                        int addedBlocks = BurnableRegistry.getBurnableBlocksCount() - beforeBlocks;

                        RealisticTorchesBT.LOGGER.info(
                                "[Compat] Chipped burnables registered: {} items, {} blocks",
                                addedItems,
                                addedBlocks
                        );
                    });
                },
                "scheduled burnable + extra compat init",
                "skipping chipped compat"
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
