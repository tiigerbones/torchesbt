package com.enchantedwisp.torchesbt.datagen;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.registry.RegistryHandler;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.SurvivesExplosionLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ModLootTableProvider extends FabricBlockLootTableProvider {
    private static final String[] CHIPPED_LANTERNS = {
            "blue_paper", "burning_coal", "checkered_iron", "dark_blue_paper",
            "ender", "green_paper", "iron_bowl", "purple_paper",
            "red_paper", "small_green", "white_paper", "wooden_cage",
            "wrought_iron", "yellow_tube"
    };
    private static final String[] SPECIAL_LANTERNS = {
            "big", "donut", "tall", "wide"
    };
    private static final String[] CHIPPED_TORCHES = {
            "acacia", "birch", "crimson", "dark_oak",
            "glow", "iron", "jungle", "spruce", "warped"
    };

    public ModLootTableProvider(FabricDataOutput dataOutput) {
        super(dataOutput);
    }

    @Override
    public void generate() {
        // Add loot tables for default unlit blocks
        addDrop(RegistryHandler.UNLIT_TORCH_BLOCK, RegistryHandler.UNLIT_TORCH);
        addDrop(RegistryHandler.UNLIT_WALL_TORCH_BLOCK, RegistryHandler.UNLIT_TORCH);
        addDrop(RegistryHandler.UNLIT_LANTERN_BLOCK, RegistryHandler.UNLIT_LANTERN);

        // Add loot tables for Chipped unlit blocks
        for (String variant : CHIPPED_LANTERNS) {
            Block unlitBlock = Registries.BLOCK.get(Identifier.of(RealisticTorchesBT.MOD_ID, "chipped/unlit_" + variant + "_lantern"));
            Item unlitItem = Registries.ITEM.get(Identifier.of(RealisticTorchesBT.MOD_ID, "chipped/unlit_" + variant + "_lantern"));
            addDrop(unlitBlock, unlitItem);
        }
        for (String variant : SPECIAL_LANTERNS) {
            Block unlitBlock = Registries.BLOCK.get(Identifier.of(RealisticTorchesBT.MOD_ID, "chipped/unlit_" + variant + "_lantern"));
            Item unlitItem = Registries.ITEM.get(Identifier.of(RealisticTorchesBT.MOD_ID, "chipped/unlit_" + variant + "_lantern"));
            addDrop(unlitBlock, unlitItem);
        }
        for (String variant : CHIPPED_TORCHES) {
            Block unlitTorchBlock = Registries.BLOCK.get(Identifier.of(RealisticTorchesBT.MOD_ID, "chipped/unlit_" + variant + "_torch"));
            Block unlitWallTorchBlock = Registries.BLOCK.get(Identifier.of(RealisticTorchesBT.MOD_ID, "chipped/unlit_" + variant + "_wall_torch"));
            Item unlitItem = Registries.ITEM.get(Identifier.of(RealisticTorchesBT.MOD_ID, "chipped/unlit_" + variant + "_torch"));
            addDrop(unlitTorchBlock, unlitItem);
            addDrop(unlitWallTorchBlock, unlitItem);
        }
    }

    private void addDrop(Block block, Item item) {
        addDrop(block, LootTable.builder()
                .pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1)) // always 1 drop
                        .with(ItemEntry.builder(item))
                        .conditionally(SurvivesExplosionLootCondition.builder())
                )
        );
    }

}