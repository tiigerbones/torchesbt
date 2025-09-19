package com.enchantedwisp.torchesbt.ignition;
import com.enchantedwisp.torchesbt.api.FuelTypeAPI;
import com.enchantedwisp.torchesbt.registry.CustomFuelTypes;
import com.enchantedwisp.torchesbt.registry.DefaultFuelTypes;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import static com.enchantedwisp.torchesbt.util.JsonLoader.IGNITERS;
import static com.enchantedwisp.torchesbt.util.JsonLoader.loadJsonFiles;

/**
 * Central class for registering and reloading all fuel types (default + custom).
 */
public class FuelTypeLoader {
    private static final Logger LOGGER = com.enchantedwisp.torchesbt.RealisticTorchesBT.LOGGER;

    private static void reset() {
        IGNITERS.clear();
        FuelTypeAPI.clear();
        LOGGER.debug("Reset fuel and igniter registries");
    }

    public static void register() {
        // Load fuels when the server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ResourceManager manager = server.getResourceManager();
            LOGGER.info("Server starting, loading fuels and igniters");
            reset();

            // Load igniters
            loadJsonFiles(manager, "ignite", IGNITERS, "ignite_amount");

            // Load default fuel types
            loadJsonFiles(manager, "fuel/torch", DefaultFuelTypes.TORCH.getFuelMap(), "add_time");
            loadJsonFiles(manager, "fuel/lantern", DefaultFuelTypes.LANTERN.getFuelMap(), "add_time");
            loadJsonFiles(manager, "fuel/campfire", DefaultFuelTypes.CAMPFIRE.getFuelMap(), "add_time");

            // Load custom fuels
            CustomFuelTypes.load(manager);

            LOGGER.info("Finished loading all fuels and igniters");
        });

        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
                .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                    private final Identifier RELOAD_ID = new Identifier("torchesbt", "json_loader");

                    @Override
                    public Identifier getFabricId() {
                        return RELOAD_ID;
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        reset();
                        LOGGER.info("Cleared fuels and igniters on reload");
                    }
                });

        LOGGER.info("Registered fuel type loader");
    }

    private FuelTypeLoader() {
    }
}