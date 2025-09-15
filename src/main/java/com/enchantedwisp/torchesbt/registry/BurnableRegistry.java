package com.enchantedwisp.torchesbt.registry;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.util.ConfigCache;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized registry for managing burnable items and blocks, their lit/unlit states, and properties.
 */
public class BurnableRegistry {
    private static final Logger LOGGER = RealisticTorchesBT.LOGGER;
    private static final Map<Item, BurnableItemEntry> BURNABLE_ITEMS = new HashMap<>();
    private static final Map<Block, BurnableBlockEntry> BURNABLE_BLOCKS = new HashMap<>();

    /**
     * Represents a burnable item with its properties.
     */
    public record BurnableItemEntry(Item litItem, Item unlitItem, long burnTime, double rainMultiplier) {}

    /**
     * Represents a burnable block with its properties.
     */
    public record BurnableBlockEntry(Block litBlock, Block unlitBlock, long burnTime, double rainMultiplier, boolean hasBlockEntity) {}

    public static void register() {
        // Register default burnable items and blocks
        registerBurnableItem(
                Items.TORCH,
                RegistryHandler.UNLIT_TORCH,
                ConfigCache.getTorchBurnTime(),
                ConfigCache.getRainTorchMultiplier()
        );
        registerBurnableItem(
                Items.LANTERN,
                RegistryHandler.UNLIT_LANTERN,
                ConfigCache.getLanternBurnTime(),
                ConfigCache.getRainLanternMultiplier()
        );
        registerBurnableBlock(
                Blocks.TORCH,
                RegistryHandler.UNLIT_TORCH_BLOCK,
                ConfigCache.getTorchBurnTime(),
                ConfigCache.getRainTorchMultiplier(),
                true
        );
        registerBurnableBlock(
                Blocks.WALL_TORCH,
                RegistryHandler.UNLIT_WALL_TORCH_BLOCK,
                ConfigCache.getTorchBurnTime(),
                ConfigCache.getRainTorchMultiplier(),
                true
        );
        registerBurnableBlock(
                Blocks.LANTERN,
                RegistryHandler.UNLIT_LANTERN_BLOCK,
                ConfigCache.getLanternBurnTime(),
                ConfigCache.getRainLanternMultiplier(),
                true
        );
        registerBurnableBlock(
                Blocks.CAMPFIRE,
                Blocks.CAMPFIRE, // Unlit campfire uses same block with LIT=false
                ConfigCache.getCampfireBurnTime(),
                ConfigCache.getRainCampfireMultiplier(),
                true
        );
        LOGGER.info("Registered {} burnable items and {} burnable blocks", BURNABLE_ITEMS.size(), BURNABLE_BLOCKS.size());
    }

    private static void registerBurnableItem(Item litItem, Item unlitItem, long burnTime, double rainMultiplier) {
        BURNABLE_ITEMS.put(litItem, new BurnableItemEntry(litItem, unlitItem, burnTime, rainMultiplier));
        LOGGER.debug("Registered burnable item: {} (unlit: {})", Registries.ITEM.getId(litItem), Registries.ITEM.getId(unlitItem));
    }

    private static void registerBurnableBlock(Block litBlock, Block unlitBlock, long burnTime, double rainMultiplier, boolean hasBlockEntity) {
        BURNABLE_BLOCKS.put(litBlock, new BurnableBlockEntry(litBlock, unlitBlock, burnTime, rainMultiplier, hasBlockEntity));
        LOGGER.debug("Registered burnable block: {} (unlit: {})", Registries.BLOCK.getId(litBlock), Registries.BLOCK.getId(unlitBlock));
    }

    public static boolean isBurnableItem(Item item) {
        return BURNABLE_ITEMS.containsKey(item);
    }

    public static boolean isBurnableBlock(Block block) {
        return BURNABLE_BLOCKS.containsKey(block);
    }

    public static Item getUnlitItem(Item litItem) {
        BurnableItemEntry entry = BURNABLE_ITEMS.get(litItem);
        return entry != null ? entry.unlitItem() : null;
    }

    public static Item getLitItem(Item unlitItem) {
        return BURNABLE_ITEMS.values().stream()
                .filter(entry -> entry.unlitItem() == unlitItem)
                .map(BurnableItemEntry::litItem)
                .findFirst()
                .orElse(null);
    }

    public static Block getUnlitBlock(Block litBlock) {
        BurnableBlockEntry entry = BURNABLE_BLOCKS.get(litBlock);
        return entry != null ? entry.unlitBlock() : null;
    }

    public static Block getLitBlock(Block unlitBlock) {
        return BURNABLE_BLOCKS.values().stream()
                .filter(entry -> entry.unlitBlock() == unlitBlock)
                .map(BurnableBlockEntry::litBlock)
                .findFirst()
                .orElse(null);
    }

    public static long getBurnTime(Item item) {
        BurnableItemEntry entry = BURNABLE_ITEMS.get(item);
        return entry != null ? entry.burnTime() : 0;
    }

    public static long getBurnTime(Block block) {
        BurnableBlockEntry entry = BURNABLE_BLOCKS.get(block);
        return entry != null ? entry.burnTime() : 0;
    }

    public static double getRainMultiplier(Item item) {
        BurnableItemEntry entry = BURNABLE_ITEMS.get(item);
        return entry != null ? entry.rainMultiplier() : 1.0;
    }

    public static double getRainMultiplier(Block block) {
        BurnableBlockEntry entry = BURNABLE_BLOCKS.get(block);
        return entry != null ? entry.rainMultiplier() : 1.0;
    }

    public static boolean hasBlockEntity(Block block) {
        BurnableBlockEntry entry = BURNABLE_BLOCKS.get(block);
        return entry != null && entry.hasBlockEntity();
    }
}