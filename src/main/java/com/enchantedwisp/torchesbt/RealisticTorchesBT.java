package com.enchantedwisp.torchesbt;

import com.enchantedwisp.torchesbt.blockentity.ModBlockEntities;
import com.enchantedwisp.torchesbt.burn.BurnTimeManager;
import com.enchantedwisp.torchesbt.compat.CompatRegistryHandler;
import com.enchantedwisp.torchesbt.compat.CompatResourceLoader;
import com.enchantedwisp.torchesbt.ignition.ReignitionHandler;
import com.enchantedwisp.torchesbt.config.RealisticTorchesBTConfig;
import com.enchantedwisp.torchesbt.registry.BurnableRegistry;
import com.enchantedwisp.torchesbt.registry.Particles;
import com.enchantedwisp.torchesbt.registry.RegistryHandler;
import com.enchantedwisp.torchesbt.util.*;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealisticTorchesBT implements ModInitializer {
    public static final String MOD_ID = "torchesbt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static ConfigHolder<RealisticTorchesBTConfig> configHolder;
    private static RealisticTorchesBTConfig config;

    public static RealisticTorchesBTConfig getConfig() {
        return config;
    }

    public static void saveConfig() {
        try {
            configHolder.save();
        } catch (Exception e) {
            LOGGER.error("Failed to save RealisticTorchesBTConfig to config/torchesbt.json5", e);
        }
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Realistic Torches BT");

        // Register config
        try {
            configHolder = AutoConfig.register(RealisticTorchesBTConfig.class, JanksonConfigSerializer::new);
            config = configHolder.getConfig();
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize RealisticTorchesBTConfig from config/torchesbt.json5, falling back to defaults", e);
            config = new RealisticTorchesBTConfig();
            configHolder = AutoConfig.getConfigHolder(RealisticTorchesBTConfig.class);
        }

        // Initialize ConfigCache
        ConfigCache.initialize();

        // Register items
        RegistryHandler.register();
        ModBlockEntities.register();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            RegistryHandler.registerBurnables();
            BurnableRegistry.snapshotCounts("Vanilla");
            BurnableRegistry.logSource("Vanilla", LOGGER);
        });

        // Compat Items
        CompatRegistryHandler.registerChipped();
        CompatResourceLoader.loadChippedResources();


        // Register Particle
        Particles.register();

        // Burntime
        ReignitionHandler.register();
        BurnTimeManager.register();

        // Json
        JsonLoader.register();
    }
}