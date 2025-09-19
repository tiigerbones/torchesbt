package com.enchantedwisp.torchesbt.util;

import com.enchantedwisp.torchesbt.api.FuelTypeAPI;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads JSON-based configuration for igniters and fuels.
 * Supports custom fuel types via FuelTypeAPI.
 */
public class JsonLoader {
    public static final Map<Identifier, Integer> IGNITERS = new HashMap<>();
    private static final Logger LOGGER = com.enchantedwisp.torchesbt.RealisticTorchesBT.LOGGER;
    private static final Gson GSON = new Gson();

    public static void register() {

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ResourceManager manager = server.getResourceManager();
            LOGGER.info("Server starting, preloading fuels and igniters");

            // Clear all fuel maps
            FuelTypeAPI.clear();
            JsonLoader.IGNITERS.clear();

            // Register default fuel types
            FuelTypeAPI.FuelType torchFuel = FuelTypeAPI.registerFuelType(new Identifier("torchesbt", "torch"));
            FuelTypeAPI.FuelType lanternFuel = FuelTypeAPI.registerFuelType(new Identifier("torchesbt", "lantern"));
            FuelTypeAPI.FuelType campfireFuel = FuelTypeAPI.registerFuelType(new Identifier("torchesbt", "campfire"));

            // Load JSONs into default fuel types
            loadJsonFiles(manager, "ignite", IGNITERS, "ignite_amount");
            loadJsonFiles(manager, "fuel/torch", torchFuel.getFuelMap(), "add_time");
            loadJsonFiles(manager, "fuel/lantern", lanternFuel.getFuelMap(), "add_time");
            loadJsonFiles(manager, "fuel/campfire", campfireFuel.getFuelMap(), "add_time");

            // Load custom fuel types dynamically
            for (Identifier fuelTypeId : FuelTypeAPI.getFuelTypeIds()) {
                if (!fuelTypeId.getNamespace().equals("torchesbt")) {
                    loadJsonFiles(manager,
                            "fuel/" + fuelTypeId.getPath(),
                            FuelTypeAPI.getFuelType(fuelTypeId).getFuelMap(),
                            "add_time");
                }
            }

            LOGGER.info("All fuels and igniters loaded on server start");
        });

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            private final Identifier RELOAD_ID = new Identifier("torchesbt", "json_loader");

            @Override
            public Identifier getFabricId() {
                return RELOAD_ID;
            }

            @Override
            public void reload(ResourceManager manager) {
                // Clear existing entries
                IGNITERS.clear();
                FuelTypeAPI.clear();

                // Register default fuel types
                FuelTypeAPI.FuelType torchFuel = FuelTypeAPI.registerFuelType(new Identifier("torchesbt", "torch"));
                FuelTypeAPI.FuelType lanternFuel = FuelTypeAPI.registerFuelType(new Identifier("torchesbt", "lantern"));
                FuelTypeAPI.FuelType campfireFuel = FuelTypeAPI.registerFuelType(new Identifier("torchesbt", "campfire"));

                // Load JSONs
                loadJsonFiles(manager, "ignite", IGNITERS, "ignite_amount");
                loadJsonFiles(manager, "fuel/torch", torchFuel.getFuelMap(), "add_time");
                loadJsonFiles(manager, "fuel/lantern", lanternFuel.getFuelMap(), "add_time");
                loadJsonFiles(manager, "fuel/campfire", campfireFuel.getFuelMap(), "add_time");

                // Load custom fuel types
                for (Identifier fuelTypeId : FuelTypeAPI.getFuelTypeIds()) {
                    if (!fuelTypeId.getNamespace().equals("torchesbt")) {
                        loadJsonFiles(manager,
                                "fuel/" + fuelTypeId.getPath(),
                                FuelTypeAPI.getFuelType(fuelTypeId).getFuelMap(),
                                "add_time");
                    }
                }
                LOGGER.info("Reloaded igniters and fuels from JSONs");
            }
        });
        LOGGER.info("Registered JSON loader reload listener");
    }

    private static void loadJsonFiles(ResourceManager manager, String folder, Map<Identifier, Integer> targetMap, String valueKey) {
        for (Identifier id : manager.findResources(folder, path -> path.getPath().endsWith(".json")).keySet()) {
            try {
                Resource resource = manager.getResourceOrThrow(id);
                try (InputStream inputStream = resource.getInputStream();
                     InputStreamReader reader = new InputStreamReader(inputStream)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    JsonArray items = json.getAsJsonArray("items");
                    int value = json.get(valueKey).getAsInt();

                    if (items == null || value <= 0) {
                        LOGGER.warn("Skipping invalid JSON at {}: Missing or invalid items/{}", id, valueKey);
                        continue;
                    }

                    LOGGER.debug("Processing JSON at {} with {} items and {}={}", id, items.size(), valueKey, value);
                    for (JsonElement itemElement : items) {
                        String itemString = itemElement.getAsString();
                        boolean isTag = itemString.startsWith("#");
                        String parsedString = isTag ? itemString.substring(1) : itemString;
                        Identifier itemId = Identifier.tryParse(parsedString);

                        if (itemId == null) {
                            LOGGER.warn("Invalid identifier in JSON at {}: {}", id, itemString);
                            continue;
                        }

                        if (isTag) {
                            TagKey<net.minecraft.item.Item> tag = TagKey.of(RegistryKeys.ITEM, itemId);
                            var entryList = Registries.ITEM.getEntryList(tag);
                            if (entryList.isPresent()) {
                                entryList.get().forEach(entry -> {
                                    Identifier entryId = Registries.ITEM.getId(entry.value());
                                    putIfAbsent(entryId, value, targetMap, folder);
                                    LOGGER.debug("Added tag item {} from {} with {}={}", entryId, itemId, valueKey, value);
                                });
                            } else {
                                LOGGER.warn("Tag {} in JSON at {} is empty or invalid", itemId, id);
                            }
                        } else {
                            if (Registries.ITEM.containsId(itemId)) {
                                Identifier entryId = Registries.ITEM.getId(Registries.ITEM.get(itemId));
                                putIfAbsent(entryId, value, targetMap, folder);
                                LOGGER.debug("Added item {} with {}={}", entryId, valueKey, value);
                            } else {
                                LOGGER.warn("Unknown item {} in JSON at {}", itemId, id);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load JSON at {}: {}", id, e.getMessage());
            }
        }
        LOGGER.info("Loaded {} entries from {} JSONs", targetMap.size(), folder);
    }

    private static void putIfAbsent(Identifier entryId, int value, Map<Identifier, Integer> targetMap, String folder) {
        if (!targetMap.containsKey(entryId)) {
            targetMap.put(entryId, value);
        } else {
            LOGGER.warn("Duplicate item {} in {} JSON, keeping first value", entryId, folder);
        }
    }
}