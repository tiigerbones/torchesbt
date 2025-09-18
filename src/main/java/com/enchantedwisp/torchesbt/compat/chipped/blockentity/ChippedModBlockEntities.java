package com.enchantedwisp.torchesbt.compat.chipped.blockentity;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.registry.BurnableRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ChippedModBlockEntities {
    private static final String[] SPECIAL_LANTERNS = {
            "big", "donut", "tall", "wide"
    };

    public static BlockEntityType<SpecialLanternBlockEntity> SPECIAL_LANTERN_BLOCK_ENTITY;

    public static List<Block> getSpecialLanternBlocks() {
        List<Block> validBlocks = new ArrayList<>();

        for (String variant : SPECIAL_LANTERNS) {
            Identifier id = Identifier.of("chipped", variant + "_lantern");
            Block block = Registries.BLOCK.get(id);

            if (block != Blocks.AIR) {
                if (BurnableRegistry.isBurnableBlock(block)) {
                    validBlocks.add(block);
                } else {
                    RealisticTorchesBT.LOGGER.warn("Chipped lantern {} found but not registered as burnable", id);
                }
            } else {
                RealisticTorchesBT.LOGGER.warn("Chipped lantern {} not found in registry", id);
            }
        }

        return validBlocks;
    }

    public static void register() {
        SPECIAL_LANTERN_BLOCK_ENTITY = BlockEntityType.Builder
                .create(SpecialLanternBlockEntity::new) // no blocks yet
                .build(null);

        Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(RealisticTorchesBT.MOD_ID, "special_lantern_block_entity"),
                SPECIAL_LANTERN_BLOCK_ENTITY
        );

        RealisticTorchesBT.LOGGER.info("Registered SpecialLanternBlockEntity type (blocks will be linked later)");
    }

    public static void linkBlocks() {
        List<Block> lanternBlocks = getSpecialLanternBlocks();
        if (lanternBlocks.isEmpty()) {
            RealisticTorchesBT.LOGGER.warn("No valid Chipped SpecialLanternBlocks found");
            return;
        }
        lanternBlocks.forEach(block -> BlockEntityType.getId(SPECIAL_LANTERN_BLOCK_ENTITY));
        RealisticTorchesBT.LOGGER.info("Linked {} special lantern blocks to SpecialLanternBlockEntity", lanternBlocks.size());
    }
}