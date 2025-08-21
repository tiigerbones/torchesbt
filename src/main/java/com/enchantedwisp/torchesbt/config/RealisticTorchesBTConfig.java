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

    @Comment("Burn time multiplier for torches in rain. Default: 10.0 (10x faster)")
    public double rainTorchMultiplier = 10.0;

    @Comment("Burn time multiplier for campfires in rain. Default: 8.5 (8.5x faster)")
    public double rainCampfireMultiplier = 8.5;

    @Comment("Burn time multiplier for lanterns in rain. Default: 6.5 (6.5x faster)")
    public double rainLanternMultiplier = 6.5;

    @Comment("Burn time multiplier for lanterns in water. Default: 7.5 (7.5x faster)")
    public double waterLanternMultiplier = 7.5;

    @Override
    public void validatePostLoad() {
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
        if (waterLanternMultiplier < 1.0 || waterLanternMultiplier > 10.0) {
            RealisticTorchesBT.LOGGER.warn("Correcting waterLanternMultiplier: {} to {}. Must be between 1.0 and 10.0.", waterLanternMultiplier, Math.max(1.0, Math.min(10.0, rainLanternMultiplier)));
            waterLanternMultiplier = Math.max(1.0, Math.min(10.0, waterLanternMultiplier));
        }
    }
}