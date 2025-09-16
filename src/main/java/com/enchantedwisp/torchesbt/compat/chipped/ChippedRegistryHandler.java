package com.enchantedwisp.torchesbt.compat.chipped;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.compat.chipped.block.ChippedUnlitLanternBlock;
import com.enchantedwisp.torchesbt.compat.chipped.block.ChippedUnlitTorchBlock;
import com.enchantedwisp.torchesbt.compat.chipped.block.ChippedUnlitWallTorchBlock;
import com.enchantedwisp.torchesbt.compat.chipped.block.SpecialUnlitLanternBlock;
import com.enchantedwisp.torchesbt.compat.chipped.item.ChippedUnlitLanternItem;
import com.enchantedwisp.torchesbt.compat.chipped.item.ChippedUnlitTorchItem;
import com.enchantedwisp.torchesbt.compat.chipped.item.ChippedSpecialUnlitLanternItem;
import com.enchantedwisp.torchesbt.registry.BurnableRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.shape.VoxelShape;

import java.util.ArrayList;
import java.util.List;

import static com.enchantedwisp.torchesbt.RealisticTorchesBT.MOD_ID;

/**
 * Handles registration of Chipped mod's torches and lanterns.
 */
public class ChippedRegistryHandler {
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

    private static final List<Block> UNLIT_LANTERN_BLOCKS = new ArrayList<>();
    private static final List<Item> UNLIT_LANTERN_ITEMS = new ArrayList<>();
    private static final List<Block> UNLIT_TORCH_BLOCKS = new ArrayList<>();
    private static final List<Item> UNLIT_TORCH_ITEMS = new ArrayList<>();

    public static void register() {
        // Register Chipped lanterns (items and blocks)
        for (String variant : CHIPPED_LANTERNS) {
            registerLantern(variant, false);
        }

        // Register Chipped special lanterns (items and blocks)
        for (String variant : SPECIAL_LANTERNS) {
            registerLantern(variant, true);
        }

        // Register Chipped torches (items and blocks)
        for (String variant : CHIPPED_TORCHES) {
            registerTorch(variant);
        }

        // Add items to item group
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            UNLIT_LANTERN_ITEMS.forEach(entries::add);
            UNLIT_TORCH_ITEMS.forEach(entries::add);
        });

        // Register Chipped burnables after blocks and items are registered
        BurnableRegistry.registerChippedBurnables();

        RealisticTorchesBT.LOGGER.info("Registered {} Chipped unlit items and blocks", UNLIT_LANTERN_ITEMS.size() + UNLIT_TORCH_ITEMS.size() * 2);
    }

    private static void registerLantern(String variant, boolean isSpecial) {
        Block unlitBlock;

        if (isSpecial) {
            // Pick correct shapes per variant
            VoxelShape eastShape;
            VoxelShape northShape;

            switch (variant) {
                case "tall" -> {
                    eastShape = Block.createCuboidShape(5, 0, 5, 11, 15, 11);
                    northShape = eastShape;
                }
                case "big", "wide" -> {
                    eastShape = Block.createCuboidShape(1, 1, 1, 15, 15, 15);
                    northShape = eastShape;
                }
                default -> {
                    eastShape = SpecialUnlitLanternBlock.DEFAULT_EAST_SHAPE;
                    northShape = SpecialUnlitLanternBlock.DEFAULT_NORTH_SHAPE;
                }
            }

            unlitBlock = new SpecialUnlitLanternBlock(
                    Block.Settings.copy(Blocks.LANTERN),
                    eastShape,
                    northShape
            );
        } else {
            unlitBlock = new ChippedUnlitLanternBlock();
        }

        Identifier unlitBlockId = Identifier.of(MOD_ID, "chipped/unlit_" + variant + "_lantern");
        Registry.register(Registries.BLOCK, unlitBlockId, unlitBlock);
        UNLIT_LANTERN_BLOCKS.add(unlitBlock);

        // Register lantern item
        Item unlitItem = isSpecial
                ? new ChippedSpecialUnlitLanternItem(unlitBlock, new Item.Settings())
                : new ChippedUnlitLanternItem(unlitBlock, new Item.Settings());
        Identifier unlitItemId = Identifier.of(MOD_ID, "chipped/unlit_" + variant + "_lantern");
        Registry.register(Registries.ITEM, unlitItemId, unlitItem);
        UNLIT_LANTERN_ITEMS.add(unlitItem);

        RealisticTorchesBT.LOGGER.debug(
                "Registered lantern: {} (unlit block: {}, unlit item: {})",
                variant, unlitBlockId, unlitItemId
        );
    }

    private static void registerTorch(String variant) {
        // Register torch block
        ChippedUnlitTorchBlock unlitTorchBlock = new ChippedUnlitTorchBlock(variant);
        Identifier unlitTorchBlockId = Identifier.of(MOD_ID, "chipped/unlit_" + variant + "_torch");
        Registry.register(Registries.BLOCK, unlitTorchBlockId, unlitTorchBlock);
        UNLIT_TORCH_BLOCKS.add(unlitTorchBlock);

        // Register wall torch block
        Block unlitWallTorchBlock = new ChippedUnlitWallTorchBlock(variant);
        Identifier unlitWallTorchBlockId = Identifier.of(MOD_ID, "chipped/unlit_" + variant + "_wall_torch");
        Registry.register(Registries.BLOCK, unlitWallTorchBlockId, unlitWallTorchBlock);
        UNLIT_TORCH_BLOCKS.add(unlitWallTorchBlock);

        // Register torch item
        Item unlitItem = new ChippedUnlitTorchItem(unlitTorchBlock, new Item.Settings(), variant);
        Identifier unlitItemId = Identifier.of(MOD_ID, "chipped/unlit_" + variant + "_torch");
        Registry.register(Registries.ITEM, unlitItemId, unlitItem);
        UNLIT_TORCH_ITEMS.add(unlitItem);

        RealisticTorchesBT.LOGGER.debug(
                "Registered torch: {} (unlit torch: {}, unlit wall torch: {}, unlit item: {})",
                variant, unlitTorchBlockId, unlitWallTorchBlockId, unlitItemId
        );
    }

    @Environment(EnvType.CLIENT)
    public static void registerRenderLayers() {
        for (Block lantern : UNLIT_LANTERN_BLOCKS) {
            BlockRenderLayerMap.INSTANCE.putBlock(lantern, RenderLayer.getCutout());
        }
        for (Block torch : UNLIT_TORCH_BLOCKS) {
            BlockRenderLayerMap.INSTANCE.putBlock(torch, RenderLayer.getCutout());
        }
    }
}