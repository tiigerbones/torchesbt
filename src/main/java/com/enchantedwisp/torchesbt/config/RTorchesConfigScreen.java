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
        // Environmental Category
        ConfigCategory environment = builder.getOrCreateCategory(Text.literal("Environment"));
        environment.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config.torchesbt.enableRainExtinguish"), config.enableRainExtinguish)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.torchesbt.enableRainExtinguish.tooltip"))
                .setSaveConsumer(newValue -> config.enableRainExtinguish = newValue)
                .build());

        environment.addEntry(entryBuilder.startDoubleField(Text.translatable("config.torchesbt.rainTorchMultiplier"), config.rainTorchMultiplier)
                .setDefaultValue(10.0)
                .setMin(1.0)
                .setMax(10.0)
                .setSaveConsumer(newValue -> config.rainTorchMultiplier = newValue)
                .build());

        environment.addEntry(entryBuilder.startDoubleField(Text.translatable("config.torchesbt.rainCampfireMultiplier"), config.rainCampfireMultiplier)
                .setDefaultValue(8.5)
                .setMin(1.0)
                .setMax(10.0)
                .setSaveConsumer(newValue -> config.rainCampfireMultiplier = newValue)
                .build());

        environment.addEntry(entryBuilder.startDoubleField(Text.translatable("config.torchesbt.rainLanternMultiplier"), config.rainCampfireMultiplier)
                .setDefaultValue(6.5)
                .setMin(1.0)
                .setMax(10.0)
                .setSaveConsumer(newValue -> config.rainLanternMultiplier = newValue)
                .build());

        environment.addEntry(entryBuilder.startDoubleField(Text.translatable("config.torchesbt.waterLanternMultiplier"), config.waterLanternMultiplier)
                .setDefaultValue(6.5)
                .setMin(1.0)
                .setMax(10.0)
                .setSaveConsumer(newValue -> config.waterLanternMultiplier = newValue)
                .build());
        return builder.build();
    }
}