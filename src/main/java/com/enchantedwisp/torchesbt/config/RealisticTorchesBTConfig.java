package com.enchantedwisp.torchesbt.config;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "torchesbt")
public class RealisticTorchesBTConfig implements ConfigData {
    @Comment("Burn time for torches in seconds. Default: 300 (5 minutes)")
    @ConfigEntry.Gui.PrefixText
    @ConfigEntry.BoundedDiscrete(min = 60, max = 8800)
    public int torchBurnTime = 1100;

    @Comment("Burn time for lanterns in seconds. Default: 600 (10 minutes)")
    @ConfigEntry.BoundedDiscrete(min = 60, max = 8800)
    public int lanternBurnTime = 1100;

    @Comment("Chance (in percent) for a torch to break when its burn time reaches 0 (0-100). Default: 50")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int torchBreakChance = 50;

    @Override
    public void validatePostLoad() {
        // Validate torch burn time
        if (torchBurnTime < 60 || torchBurnTime > 3600) {
            RealisticTorchesBT.LOGGER.warn("Correcting torchBurnTime: {} to {}. Must be between 60 and 3600.", torchBurnTime, Math.max(60, Math.min(3600, torchBurnTime)));
            torchBurnTime = Math.max(60, Math.min(3600, torchBurnTime));
        }

        // Validate lantern burn time
        if (lanternBurnTime < 60 || lanternBurnTime > 3600) {
            RealisticTorchesBT.LOGGER.warn("Correcting lanternBurnTime: {} to {}. Must be between 60 and 3600.", lanternBurnTime, Math.max(60, Math.min(3600, lanternBurnTime)));
            lanternBurnTime = Math.max(60, Math.min(3600, lanternBurnTime));
        }

        // Validate torch break chance
        if (torchBreakChance < 0 || torchBreakChance > 100) {
            RealisticTorchesBT.LOGGER.warn("Correcting torchBreakChance: {} to {}. Must be between 0 and 100.", torchBreakChance, Math.max(0, Math.min(100, torchBreakChance)));
            torchBreakChance = Math.max(0, Math.min(100, torchBreakChance));
        }
    }
}