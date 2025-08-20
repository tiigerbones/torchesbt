package com.enchantedwisp.torchesbt.config;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "torchesbt")
public class RealisticTorchesBTConfig implements ConfigData {
    @Comment("Burn time for torches. Default: 40")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.BoundedDiscrete(min = 5, max = 3600)
    public int torchBurnTime = 40;

    @Comment("Burn time for lanterns. Default: 60")
    @ConfigEntry.BoundedDiscrete(min = 5, max = 3600)
    public int lanternBurnTime = 60;

    @Comment("Burn time for campfires. Default: 100")
    @ConfigEntry.BoundedDiscrete(min = 5, max = 3600)
    public int campfireBurnTime = 100;

    @Comment("If true, rain affects burnout (torches/campfires/lanterns: faster). Default: true")
    @ConfigEntry.Gui.PrefixText
    public boolean enableRainExtinguish = true;

    @Comment("Burn time multiplier for torches in rain. Default: 2.0 (2x faster)")
    public double rainTorchMultiplier = 2.0;

    @Comment("Burn time multiplier for campfires in rain. Default: 1.5 (1.5x faster)")
    public double rainCampfireMultiplier = 1.5;

    @Comment("Burn time multiplier for campfires in rain. Default: 0.8 (0.8x faster)")
    public double rainLanternMultiplier = 0.8;

    @Override
    public void validatePostLoad() {
        // Clamp burn times
        if (torchBurnTime < 60 || torchBurnTime > 3600) {
            RealisticTorchesBT.LOGGER.warn("Correcting torchBurnTime: {} to {}. Must be between 60 and 3600.", torchBurnTime, Math.max(60, Math.min(3600, torchBurnTime)));
            torchBurnTime = Math.max(60, Math.min(3600, torchBurnTime));
        }
        if (lanternBurnTime < 60 || lanternBurnTime > 3600) {
            RealisticTorchesBT.LOGGER.warn("Correcting lanternBurnTime: {} to {}. Must be between 60 and 3600.", lanternBurnTime, Math.max(60, Math.min(3600, lanternBurnTime)));
            lanternBurnTime = Math.max(60, Math.min(3600, lanternBurnTime));
        }
        if (campfireBurnTime < 60 || campfireBurnTime > 3600) {
            RealisticTorchesBT.LOGGER.warn("Correcting campfireBurnTime: {} to {}. Must be between 60 and 3600.", campfireBurnTime, Math.max(60, Math.min(3600, campfireBurnTime)));
            campfireBurnTime = Math.max(60, Math.min(3600, campfireBurnTime));
        }
        // Clamp multipliers
        if (rainTorchMultiplier < 1.0 || rainTorchMultiplier > 10.0) {
            RealisticTorchesBT.LOGGER.warn("Correcting rainTorchMultiplier: {} to {}. Must be between 1.0 and 10.0.", rainTorchMultiplier, Math.max(1.0, Math.min(10.0, rainTorchMultiplier)));
            rainTorchMultiplier = Math.max(1.0, Math.min(10.0, rainTorchMultiplier));
        }
        if (rainCampfireMultiplier < 1.0 || rainCampfireMultiplier > 10.0) {
            RealisticTorchesBT.LOGGER.warn("Correcting rainCampfireMultiplier: {} to {}. Must be between 1.0 and 10.0.", rainCampfireMultiplier, Math.max(1.0, Math.min(10.0, rainCampfireMultiplier)));
            rainCampfireMultiplier = Math.max(1.0, Math.min(10.0, rainCampfireMultiplier));
        }
        if (rainLanternMultiplier < 1.0 || rainLanternMultiplier > 10.0) {
            RealisticTorchesBT.LOGGER.warn("Correcting rainLanternMultiplier: {} to {}. Must be between 1.0 and 10.0.", rainLanternMultiplier, Math.max(1.0, Math.min(10.0, rainLanternMultiplier)));
            rainLanternMultiplier = Math.max(1.0, Math.min(10.0, rainLanternMultiplier));
        }
    }
}