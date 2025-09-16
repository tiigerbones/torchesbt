package com.enchantedwisp.torchesbt.mixin;

import com.enchantedwisp.torchesbt.registry.BurnableRegistry;
import earth.terrarium.chipped.Chipped;
import com.enchantedwisp.torchesbt.compat.chipped.blockentity.ChippedModBlockEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chipped.class)
public class ChippedInitializerMixin {
    @Inject(method = "init", at = @At("TAIL"), remap = false) // remap=false if Chipped doesn't use mappings
    private static void injectRegistration(CallbackInfo ci) {
        BurnableRegistry.registerChippedBurnables(); // Register burnables first
        ChippedModBlockEntities.register();
    }
}