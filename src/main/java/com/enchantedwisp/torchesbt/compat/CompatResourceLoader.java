package com.enchantedwisp.torchesbt.compat;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CompatResourceLoader {

    public static void loadChippedResources() {
        if (!FabricLoader.getInstance().isModLoaded("chipped")) {
            RealisticTorchesBT.LOGGER.info("[Compat] Chipped not loaded, skipping resource pack");
            return;
        }

        ModContainer container = FabricLoader.getInstance().getModContainer(RealisticTorchesBT.MOD_ID).orElse(null);
        if (container == null) {
            RealisticTorchesBT.LOGGER.warn("[Compat] Could not find mod container, skipping Chipped resource pack");
            return;
        }

        Identifier packId = new Identifier(RealisticTorchesBT.MOD_ID, "chipped_compat");

        boolean success = ResourceManagerHelper.registerBuiltinResourcePack(
                packId,
                container,
                Text.literal("Chipped Compat for RealisticTorchesBT"),
                ResourcePackActivationType.ALWAYS_ENABLED
        );

        if (success) {
            RealisticTorchesBT.LOGGER.info("[Compat] Chipped resource pack registered!");
        } else {
            RealisticTorchesBT.LOGGER.warn("[Compat] Failed to register Chipped resource pack!");
        }
    }
}
