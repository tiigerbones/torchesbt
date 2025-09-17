package com.enchantedwisp.torchesbt.compat.chipped.blockentity;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.registry.BurnableRegistry;
import earth.terrarium.chipped.common.blocks.SpecialLanternBlock;
import net.fabricmc.loader.api.FabricLoader;
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

    public static BlockEntityType<SpecialLanternBlockEntity> SPECIAL_LANTERN_BLOCK_ENTITY; // Non-final, initialized in register()

    public static List<Block> getSpecialLanternBlocks() {
        List<Block> validBlocks = new ArrayList<>();

        for (String variant : SPECIAL_LANTERNS) {
            Identifier id = Identifier.of("chipped", variant + "_lantern"); // Try variant + "_iron_lantern" if this fails
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
            List<Block> lanternBlocks = getSpecialLanternBlocks();
            if (lanternBlocks.isEmpty()) {
                RealisticTorchesBT.LOGGER.error("No valid Chipped SpecialLanternBlocks found — skipping BlockEntityType registration!");
                return;
            }
            SPECIAL_LANTERN_BLOCK_ENTITY = BlockEntityType.Builder.create(SpecialLanternBlockEntity::new, lanternBlocks.toArray(new Block[0])).build(null);
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(RealisticTorchesBT.MOD_ID, "special_lantern_block_entity"),
                    SPECIAL_LANTERN_BLOCK_ENTITY
            );
            RealisticTorchesBT.LOGGER.info("Registered SpecialLanternBlockEntity for Chipped mod");
        }
    }