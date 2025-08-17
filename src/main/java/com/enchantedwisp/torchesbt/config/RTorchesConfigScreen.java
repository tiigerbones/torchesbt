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
                .setTitle(Text.literal("Realistc Torches BT Config"))
                .setSavingRunnable(RealisticTorchesBT::saveConfig);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        RealisticTorchesBTConfig config = RealisticTorchesBT.getConfig();

        // General Category
        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
        general.addEntry(entryBuilder.startLongField(Text.translatable("config.torchesbt.torchBurnTime"), config.torchBurnTime)
                .setDefaultValue(600)
                .setMin(0)
                .setMax(8800)
                .setTooltip(Text.translatable("config.torchesbt.torchBurnTime.tooltip"))
                .setSaveConsumer(newValue -> config.torchBurnTime = Math.toIntExact(newValue))
                .build());

        general.addEntry(entryBuilder.startLongField(Text.translatable("config.torchesbt.lanternBurnTime"), config.lanternBurnTime)
                .setDefaultValue(1100)
                .setMin(0)
                .setMax(8800)
                .setTooltip(Text.translatable("config.torchesbt.lanternBurnTime.tooltip"))
                .setSaveConsumer(newValue -> config.lanternBurnTime = Math.toIntExact(newValue))
                .build());

        general.addEntry(entryBuilder.startLongField(Text.translatable("config.torchesbt.torchBreakChance"), config.torchBreakChance)
                .setDefaultValue(50)
                .setMin(0)
                .setMax(100)
                .setTooltip(Text.translatable("config.torchesbt.torchBreakChance.tooltip"))
                .setSaveConsumer(newValue -> config.torchBreakChance = Math.toIntExact(newValue))
                .build());

        return builder.build();
    }
}