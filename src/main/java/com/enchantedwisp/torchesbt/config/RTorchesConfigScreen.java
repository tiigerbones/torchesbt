package com.enchantedwisp.torchesbt.config;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class RTorchesConfigScreen {
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Realistic Torches BT Config"))
                .setSavingRunnable(RealisticTorchesBT::saveConfig);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        RealisticTorchesBTConfig config = RealisticTorchesBT.getConfig();

        // General Category
        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
        general.addEntry(entryBuilder.startLongField(Text.translatable("config.torchesbt.torchBurnTime"), config.torchBurnTime)
                .setDefaultValue(1100)
                .setMin(60)
                .setMax(3600)
                .setTooltip(Text.translatable("config.torchesbt.torchBurnTime.tooltip"))
                .setSaveConsumer(newValue -> config.torchBurnTime = Math.toIntExact(newValue))
                .build());

        general.addEntry(entryBuilder.startLongField(Text.translatable("config.torchesbt.lanternBurnTime"), config.lanternBurnTime)
                .setDefaultValue(1100)
                .setMin(60)
                .setMax(3600)
                .setTooltip(Text.translatable("config.torchesbt.lanternBurnTime.tooltip"))
                .setSaveConsumer(newValue -> config.lanternBurnTime = Math.toIntExact(newValue))
                .build());

        general.addEntry(entryBuilder.startLongField(Text.translatable("config.torchesbt.campfireBurnTime"), config.campfireBurnTime)
                .setDefaultValue(1100)
                .setMin(60)
                .setMax(3600)
                .setTooltip(Text.translatable("config.torchesbt.campfireBurnTime.tooltip"))
                .setSaveConsumer(newValue -> config.campfireBurnTime = Math.toIntExact(newValue))
                .build());

        // Environmental Category
        ConfigCategory environment = builder.getOrCreateCategory(Text.literal("Environment"));
        environment.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config.torchesbt.enableRainExtinguish"), config.enableRainExtinguish)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.torchesbt.enableRainExtinguish.tooltip"))
                .setSaveConsumer(newValue -> config.enableRainExtinguish = newValue)
                .build());

        environment.addEntry(entryBuilder.startDoubleField(Text.translatable("config.torchesbt.rainTorchMultiplier"), config.rainTorchMultiplier)
                .setDefaultValue(2.0)
                .setMin(1.0)
                .setMax(10.0)
                .setSaveConsumer(newValue -> config.rainTorchMultiplier = newValue)
                .build());

        environment.addEntry(entryBuilder.startDoubleField(Text.translatable("config.torchesbt.rainCampfireMultiplier"), config.rainCampfireMultiplier)
                .setDefaultValue(1.5)
                .setMin(1.0)
                .setMax(10.0)
                .setSaveConsumer(newValue -> config.rainCampfireMultiplier = newValue)
                .build());

        environment.addEntry(entryBuilder.startDoubleField(Text.translatable("config.torchesbt.rainLanternMultiplier"), config.rainCampfireMultiplier)
                .setDefaultValue(0.8)
                .setMin(1.0)
                .setMax(10.0)
                .setSaveConsumer(newValue -> config.rainLanternMultiplier = newValue)
                .build());
        return builder.build();
    }
}