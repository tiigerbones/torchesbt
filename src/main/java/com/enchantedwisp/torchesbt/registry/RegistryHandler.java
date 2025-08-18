package com.enchantedwisp.torchesbt.registry;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.registry.blocks.UnlitLanternBlock;
import com.enchantedwisp.torchesbt.registry.blocks.UnlitTorchBlock;
import com.enchantedwisp.torchesbt.registry.blocks.UnlitWallTorchBlock;
import com.enchantedwisp.torchesbt.registry.items.UnlitCampFireItem;
import com.enchantedwisp.torchesbt.registry.items.UnlitLanternItem;
import com.enchantedwisp.torchesbt.registry.items.UnlitTorchItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static com.enchantedwisp.torchesbt.RealisticTorchesBT.MOD_ID;

public class RegistryHandler {
    public static final Block UNLIT_TORCH_BLOCK = new UnlitTorchBlock();
    public static final Block UNLIT_WALL_TORCH_BLOCK = new UnlitWallTorchBlock();
    public static final Block UNLIT_LANTERN_BLOCK = new UnlitLanternBlock();
    public static final Item UNLIT_TORCH = new UnlitTorchItem((UnlitTorchBlock) UNLIT_TORCH_BLOCK, new Item.Settings());
    public static final Item UNLIT_LANTERN = new UnlitLanternItem(UNLIT_LANTERN_BLOCK, new Item.Settings());
    public static final Item UNLIT_CAMPFIRE = new UnlitCampFireItem(new Item.Settings());

    public static void register() {
        registerItemGroups();
        RealisticTorchesBT.LOGGER.info("Registering blocks, items, and block entities for The Murk");

        Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "unlit_torch"), UNLIT_TORCH_BLOCK);
        Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "unlit_wall_torch"), UNLIT_WALL_TORCH_BLOCK);
        Registry.register(Registries.BLOCK, Identifier.of(MOD_ID, "unlit_lantern"), UNLIT_LANTERN_BLOCK);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "unlit_torch"), UNLIT_TORCH);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "unlit_lantern"), UNLIT_LANTERN);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "unlit_campfire"), UNLIT_CAMPFIRE);
    }

    private static void registerItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(net.minecraft.item.ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(UNLIT_TORCH);
            entries.add(UNLIT_LANTERN);
            entries.add(UNLIT_CAMPFIRE);
        });
    }
}