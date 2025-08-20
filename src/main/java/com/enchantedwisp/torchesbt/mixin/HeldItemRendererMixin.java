package com.enchantedwisp.torchesbt.mixin;

import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Redirect(method = "updateHeldItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;areEqual(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z"))
    private boolean skipBobIfOnlyBurnTime(ItemStack stack1, ItemStack stack2) {
        if ((stack1.getItem() == Items.TORCH || stack1.getItem() == Items.LANTERN) &&
                (stack2.getItem() == Items.TORCH || stack2.getItem() == Items.LANTERN)) {
            ItemStack copy1 = stack1.copy();
            ItemStack copy2 = stack2.copy();
            copy1.removeSubNbt("remaining_burn");
            copy2.removeSubNbt("remaining_burn");
            return ItemStack.areEqual(copy1, copy2);
        }
        return ItemStack.areEqual(stack1, stack2);
    }
}