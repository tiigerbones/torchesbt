package com.enchantedwisp.torchesbt.util;

import com.enchantedwisp.torchesbt.blockentity.LanternBlockEntity;
import com.enchantedwisp.torchesbt.blockentity.TorchBlockEntity;
import com.enchantedwisp.torchesbt.registry.RegistryHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BurnableLightUtil {

    /**
     * Sets the initial burn time when placing a block that has burnable behavior.
     * For torch and lantern, it sets remaining burn time directly.
     * For campfire, it sets the NBT burn time.
     */
    public static void setBurnTimeOnPlacement(World world, BlockPos pos, BlockEntity entity, ItemStack stack, long burnTime) {
        if (entity instanceof TorchBlockEntity torch) {
            torch.setRemainingBurnTime(torch.getMaxBurnTime());
        } else if (entity instanceof LanternBlockEntity lantern) {
            lantern.setRemainingBurnTime(lantern.getMaxBurnTime());
        } else if (entity instanceof CampfireBlockEntity campfire) {
            // Campfire still stores burn time in NBT
            BurnTimeManager.setCurrentBurnTime(campfire, burnTime);
        }
    }

    /**
     * Converts a lit block to its unlit version once burn time reaches 0.
     * This method is already called in tick handlers.
     */
    public static void convertToUnlit(World world, BlockPos pos, net.minecraft.block.BlockState state) {
        if (state.getBlock() instanceof net.minecraft.block.TorchBlock) {
            world.setBlockState(pos, RegistryHandler.UNLIT_TORCH_BLOCK.getDefaultState(), net.minecraft.block.Block.NOTIFY_ALL);
        } else if (state.getBlock() instanceof net.minecraft.block.LanternBlock) {
            world.setBlockState(pos, RegistryHandler.UNLIT_LANTERN_BLOCK.getDefaultState()
                    .with(net.minecraft.block.LanternBlock.HANGING, state.get(net.minecraft.block.LanternBlock.HANGING)), net.minecraft.block.Block.NOTIFY_ALL);
        } else if (state.getBlock() instanceof net.minecraft.block.CampfireBlock) {
            world.setBlockState(pos, state.with(net.minecraft.block.CampfireBlock.LIT, false), net.minecraft.block.Block.NOTIFY_ALL);
        }
    }
}
