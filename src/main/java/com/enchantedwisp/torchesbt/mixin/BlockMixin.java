package com.enchantedwisp.torchesbt.mixin;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.blockentity.LanternBlockEntity;
import com.enchantedwisp.torchesbt.blockentity.TorchBlockEntity;
import com.enchantedwisp.torchesbt.util.BurnTimeManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class BlockMixin {

    @Inject(method = "afterBreak",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;dropStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)V"),
            cancellable = true)
    private void modifyDropsOnBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack stack, CallbackInfo ci) {
        if (world.isClient) return;

        Block block = state.getBlock();
        if (block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.LANTERN ||
                (block == Blocks.CAMPFIRE && state.get(net.minecraft.block.CampfireBlock.LIT))) {
            if (blockEntity != null) {
                long remainingBurnTime = BurnTimeManager.getCurrentBurnTime(blockEntity);
                RealisticTorchesBT.LOGGER.debug("Block broken at {} with remaining burn time: {}", pos, remainingBurnTime);

                // Custom drop logic
                ItemStack drop = null;
                if (remainingBurnTime > 0) {
                    // Drop lit item with remaining burn time
                    if (block == Blocks.TORCH || block == Blocks.WALL_TORCH) {
                        drop = new ItemStack(Blocks.TORCH.asItem());
                    } else if (block == Blocks.LANTERN) {
                        drop = new ItemStack(Blocks.LANTERN.asItem());
                    } else if (block == Blocks.CAMPFIRE) {
                        drop = new ItemStack(Blocks.CAMPFIRE.asItem());
                    }
                    if (drop != null) {
                        BurnTimeManager.setCurrentBurnTime(drop, remainingBurnTime);
                    }
                } else {
                    // Drop unlit variant
                    if (block == Blocks.TORCH || block == Blocks.WALL_TORCH) {
                        drop = new ItemStack(com.enchantedwisp.torchesbt.registry.RegistryHandler.UNLIT_TORCH);
                    } else if (block == Blocks.LANTERN) {
                        drop = new ItemStack(com.enchantedwisp.torchesbt.registry.RegistryHandler.UNLIT_LANTERN);
                    } else if (block == Blocks.CAMPFIRE) {
                        drop = new ItemStack(com.enchantedwisp.torchesbt.registry.RegistryHandler.UNLIT_CAMPFIRE);
                    }
                }
                if (drop != null) {
                    Block.dropStack(world, pos, drop);
                    RealisticTorchesBT.LOGGER.debug("Dropped item {} at {} with burn time {}", drop.getItem(), pos, remainingBurnTime);
                }

                // Cancel default drop behavior to avoid duplicates
                ci.cancel();
            }
        }
    }
}