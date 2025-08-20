package com.enchantedwisp.torchesbt.mixin;

import com.enchantedwisp.torchesbt.util.BurnTimeManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "isItemBarVisible", at = @At("HEAD"), cancellable = true)
    private void isItemBarVisible(CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() == Items.TORCH || stack.getItem() == Items.LANTERN) {
            long max = BurnTimeManager.getMaxBurnTime(stack);
            long current = BurnTimeManager.getCurrentBurnTime(stack);
            cir.setReturnValue(current > 0 && current < max);
        }
    }

    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    private void getMaxDamage(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() == Items.TORCH || stack.getItem() == Items.LANTERN) {
            cir.setReturnValue((int) BurnTimeManager.getMaxBurnTime(stack));
        }
    }

    @Inject(method = "getDamage", at = @At("HEAD"), cancellable = true)
    private void getDamage(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() == Items.TORCH || stack.getItem() == Items.LANTERN) {
            long max = BurnTimeManager.getMaxBurnTime(stack);
            long current = BurnTimeManager.getCurrentBurnTime(stack);
            cir.setReturnValue((int) (max - current));
        }
    }

    @Inject(method = "getItemBarColor", at = @At("HEAD"), cancellable = true)
    private void getItemBarColor(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() == Items.TORCH || stack.getItem() == Items.LANTERN) {
            cir.setReturnValue(0xFFA500); // Orange
        }
    }

    @Inject(method = "getItemBarStep", at = @At("HEAD"), cancellable = true)
    private void getItemBarStep(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.getItem() == Items.TORCH || stack.getItem() == Items.LANTERN) {
            long max = BurnTimeManager.getMaxBurnTime(stack);
            long current = BurnTimeManager.getCurrentBurnTime(stack);
            cir.setReturnValue((int) (13.0 * current / max));
        }
    }
}