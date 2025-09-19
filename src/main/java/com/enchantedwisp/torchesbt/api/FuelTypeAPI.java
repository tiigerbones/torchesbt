package com.enchantedwisp.torchesbt.api;

import net.minecraft.item.Item;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * API for managing custom fuel types for burnable blocks or items.
 * Allows registering new fuel types and associating items or tags with burn times.
 */
public class FuelTypeAPI {
    public static final String MOD_ID = "torchesbt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Map<Identifier, FuelType> FUEL_TYPES = new HashMap<>();

    /**
     * Represents a fuel type with a unique ID and associated fuel map.
     */
    public static class FuelType {
        private final Identifier id;
        private final Map<Identifier, Integer> fuelMap;

        public FuelType(Identifier id) {
            this.id = id;
            this.fuelMap = new HashMap<>();
        }

        public Identifier getId() {
            return id;
        }

        public Map<Identifier, Integer> getFuelMap() {
            return fuelMap;
        }
    }

    /**
     * Registers a new fuel type with a unique identifier.
     *
     * @param id the unique identifier for the fuel type (e.g., "yourmod:custom_lamp")
     * @return the registered FuelType instance
     */
    public static FuelType registerFuelType(Identifier id) {
        if (FUEL_TYPES.containsKey(id)) {
            LOGGER.warn("Fuel type {} already registered, returning existing instance", id);
            return FUEL_TYPES.get(id);
        }
        FuelType fuelType = new FuelType(id);
        FUEL_TYPES.put(id, fuelType);
        LOGGER.info("Registered fuel type {}", id);
        return fuelType;
    }

    /**
     * Adds an item as a fuel for a specific fuel type with a burn time (in seconds).
     *
     * @param fuelType   the fuel type to add the item to
     * @param item       the item to use as fuel
     * @param burnTimeSeconds the burn time in seconds (converted to ticks internally)
     */
    public static void addFuelItem(FuelType fuelType, Item item, int burnTimeSeconds) {
        Identifier itemId = net.minecraft.registry.Registries.ITEM.getId(item);
        if (fuelType.getFuelMap().containsKey(itemId)) {
            LOGGER.warn("Item {} already registered as fuel for {}, overwriting", itemId, fuelType.getId());
        }
        fuelType.getFuelMap().put(itemId, burnTimeSeconds);
        LOGGER.debug("Added fuel item {} for {} with {} seconds", itemId, fuelType.getId(), burnTimeSeconds);
    }

    /**
     * Adds an item tag as a fuel for a specific fuel type with a burn time (in seconds).
     *
     * @param fuelType   the fuel type to add the tag to
     * @param tag        the item tag to use as fuel
     * @param burnTimeSeconds the burn time in seconds (converted to ticks internally)
     */
    public static void addFuelTag(FuelType fuelType, TagKey<Item> tag, int burnTimeSeconds) {
        var entryList = net.minecraft.registry.Registries.ITEM.getEntryList(tag);
        if (entryList.isPresent()) {
            entryList.get().forEach(entry -> {
                Identifier entryId = net.minecraft.registry.Registries.ITEM.getId(entry.value());
                if (fuelType.getFuelMap().containsKey(entryId)) {
                    LOGGER.warn("Item {} from tag {} already registered for {}, overwriting", entryId, tag.id(), fuelType.getId());
                }
                fuelType.getFuelMap().put(entryId, burnTimeSeconds);
                LOGGER.debug("Added fuel item {} from tag {} for {} with {} seconds", entryId, tag.id(), fuelType.getId(), burnTimeSeconds);
            });
        } else {
            LOGGER.warn("Tag {} is empty or invalid for fuel type {}", tag.id(), fuelType.getId());
        }
    }

    /**
     * Gets the fuel type by its identifier.
     *
     * @param id the fuel type identifier
     * @return the FuelType instance, or null if not found
     */
    public static FuelType getFuelType(Identifier id) {
        return FUEL_TYPES.get(id);
    }

    /**
     * Gets the fuel burn time for an item in a specific fuel type (in ticks).
     *
     * @param fuelType the fuel type
     * @param itemId   the item identifier
     * @return the burn time in ticks, or 0 if not a valid fuel
     */
    public static long getFuelBurnTime(FuelType fuelType, Identifier itemId) {
        Integer seconds = fuelType.getFuelMap().get(itemId);
        return seconds != null ? seconds * 20L : 0;
    }

    /**
     * Gets all registered fuel type identifiers.
     *
     * @return set of registered fuel type IDs
     */
    public static Iterable<Identifier> getFuelTypeIds() {
        return FUEL_TYPES.keySet();
    }

    /**
     * Clears all registered fuel types.
     * Useful for datapack/resource reloads.
     */
    public static void clear() {
        for (FuelType type : FUEL_TYPES.values()) {
            type.getFuelMap().clear();
        }
        LOGGER.info("Cleared all fuel type contents");
    }
}
