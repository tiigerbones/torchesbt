package com.enchantedwisp.torchesbt.registry;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.util.ConfigCache;
import com.enchantedwisp.torchesbt.util.JsonLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import org.slf4j.Logger;
import net.fabricmc.loader.api.FabricLoader;

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
     * Enum to represent the type of fuel used by a burnable block.
     */
    public enum FuelType {
        CAMPFIRE_FUELS(JsonLoader.CAMPFIRE_FUELS),
        LANTERN_FUELS(JsonLoader.LANTERN_FUELS),
        TORCH_FUELS(JsonLoader.TORCH_FUELS);

        /** Map of fuel items to their burn times. */
        private final Map<Identifier, Integer> fuelMap;

        /**
         * Constructs a FuelType with the specified fuel map.
         *
         * @param fuelMap The map of fuel items to their burn times.
         */
        FuelType(Map<Identifier, Integer> fuelMap) {
            this.fuelMap = fuelMap;
        }

        /**
         * Retrieves the fuel map associated with this fuel type.
         *
         * @return The map of fuel items to their burn times.
         */
        public Map<Identifier, Integer> getFuelMap() {
            return fuelMap;
        }
    }

    /**
     * Record representing a burnable item with its properties.
     *
     * @param litItem The item in its lit state.
     * @param unlitItem The item in its unlit state.
     * @param burnTime The duration the item burns for (in ticks).
     * @param rainMultiplier The multiplier applied to burn time in rain.
     */
    public record BurnableItemEntry(Item litItem, Item unlitItem, long burnTime, double rainMultiplier) {}

    /**
     * Record representing a burnable block with its properties and fuel type.
     *
     * @param litBlock The block in its lit state.
     * @param unlitBlock The block in its unlit state.
     * @param burnTime The duration the block burns for (in ticks).
     * @param rainMultiplier The multiplier applied to burn time in rain.
     * @param hasBlockEntity Whether the block has a block entity.
     * @param fuelType The type of fuel used by the block.
     */
    public record BurnableBlockEntry(Block litBlock, Block unlitBlock, long burnTime, double rainMultiplier, boolean hasBlockEntity, FuelType fuelType) {}

    public static int getBurnableItemsCount() {
        return BURNABLE_ITEMS.size();
    }

    public static int getBurnableBlocksCount() {
        return BURNABLE_BLOCKS.size();
    }

    /**
     * Registers a burnable item with its properties.
     *
     * @param litItem The item in its lit state.
     * @param unlitItem The item in its unlit state.
     * @param burnTime The duration the item burns for (in ticks).
     * @param rainMultiplier The multiplier applied to burn time in rain.
     */
    public static void registerBurnableItem(Item litItem, Item unlitItem, long burnTime, double rainMultiplier) {
        BURNABLE_ITEMS.put(litItem, new BurnableItemEntry(litItem, unlitItem, burnTime, rainMultiplier));
        LOGGER.debug("Registered burnable item: {} (unlit: {})", Registries.ITEM.getId(litItem), Registries.ITEM.getId(unlitItem));
    }

    /**
     * Registers a burnable block with its properties and fuel type.
     *
     * @param litBlock The block in its lit state.
     * @param unlitBlock The block in its unlit state.
     * @param burnTime The duration the block burns for (in ticks).
     * @param rainMultiplier The multiplier applied to burn time in rain.
     * @param hasBlockEntity Whether the block has a block entity.
     * @param fuelType The type of fuel used by the block.
     */
    public static void registerBurnableBlock(Block litBlock, Block unlitBlock, long burnTime, double rainMultiplier, boolean hasBlockEntity, FuelType fuelType) {
        BURNABLE_BLOCKS.put(litBlock, new BurnableBlockEntry(litBlock, unlitBlock, burnTime, rainMultiplier, hasBlockEntity, fuelType));
        LOGGER.debug("Registered burnable block: {} (unlit: {}, fuelType: {})", Registries.BLOCK.getId(litBlock), Registries.BLOCK.getId(unlitBlock), fuelType);
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

    public static FuelType getFuelType(Block block) {
        BurnableBlockEntry entry = BURNABLE_BLOCKS.get(block);
        return entry != null ? entry.fuelType() : null;
    }
}