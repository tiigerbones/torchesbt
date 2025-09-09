package com.enchantedwisp.torchesbt.util;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.config.RealisticTorchesBTConfig;
import me.shedaniel.autoconfig.AutoConfig;
import org.slf4j.Logger;

public class ConfigCache {
    private static final Logger LOGGER = RealisticTorchesBT.LOGGER;
    private static long torchBurnTime; // ticks
    private static long lanternBurnTime;
    private static long campfireBurnTime;
    private static boolean enableRainExtinguish;
    private static double rainTorchMultiplier;
    private static double rainCampfireMultiplier;
    private static double rainLanternMultiplier;
    private static double waterLanternMultiplier;
    private static boolean enableDynamicLights;

    public static void initialize() {
        RealisticTorchesBTConfig config = AutoConfig.getConfigHolder(RealisticTorchesBTConfig.class).getConfig();
        torchBurnTime = config.torchBurnTime * 20L;
        lanternBurnTime = config.lanternBurnTime * 20L;
        campfireBurnTime = config.campfireBurnTime * 20L;
        enableRainExtinguish = config.enableRainExtinguish;
        rainTorchMultiplier = config.rainTorchMultiplier;
        rainCampfireMultiplier = config.rainCampfireMultiplier;
        rainLanternMultiplier = config.rainLanternMultiplier;
        waterLanternMultiplier = config.waterLanternMultiplier;
        enableDynamicLights = config.enableDynamicLights;

        LOGGER.info(
            "Loaded config into cache: torchBurnTime={} ticks, enableRainExtinguish={}, dynamicLights={}",
            torchBurnTime,
            enableRainExtinguish,
            enableDynamicLights
        );

        LOGGER.info(
            "Dynamic lighting support: {} (config)",
            enableDynamicLights ? "Enabled" : "Disabled"
        );
    }

    // Getters...
    public static long getTorchBurnTime() { return torchBurnTime; }
    public static long getLanternBurnTime() { return lanternBurnTime; }
    public static long getCampfireBurnTime() { return campfireBurnTime; }
    public static boolean isRainExtinguishEnabled() { return enableRainExtinguish; }
    public static double getRainTorchMultiplier() { return rainTorchMultiplier; }
    public static double getRainCampfireMultiplier() { return rainCampfireMultiplier; }
    public static double getRainLanternMultiplier() { return rainLanternMultiplier; }
    public static double getWaterLanternMultiplier() { return waterLanternMultiplier; }
    public static boolean isDynamicLightsEnabled() { return enableDynamicLights; }
}