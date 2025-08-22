package com.enchantedwisp.torchesbt.mixin;

import com.enchantedwisp.torchesbt.burn.BurnTimeManager;
import com.enchantedwisp.torchesbt.burn.BurnTimeUtils;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "appendTooltip", at = @At("TAIL"))
    private void appendBurnTimeTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context, CallbackInfo ci) {
        if (stack.getItem() == Items.TORCH || stack.getItem() == Items.LANTERN) {
            long current = BurnTimeUtils.getCurrentBurnTime(stack);
            long max = BurnTimeUtils.getMaxBurnTime(stack);
            tooltip.add(Text.literal("Burn Time: " + (current / 20) + "/" + (max / 20)));
        }
    }
}