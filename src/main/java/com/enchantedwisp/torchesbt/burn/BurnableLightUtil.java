package com.enchantedwisp.torchesbt.burn;

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
     * Uses the ItemStack's remaining_burn NBT if present, otherwise falls back to max burn time.
     */
    public static void setBurnTimeOnPlacement(World world, BlockPos pos, BlockEntity entity, ItemStack stack, long defaultBurnTime) {
        long burnTime = stack.hasNbt() && stack.getNbt().contains(BurnTimeManager.BURN_TIME_KEY)
                ? stack.getNbt().getLong(BurnTimeManager.BURN_TIME_KEY)
                : defaultBurnTime;

        if (entity instanceof TorchBlockEntity torch) {
            torch.setRemainingBurnTime(Math.min(burnTime, torch.getMaxBurnTime()));
        } else if (entity instanceof LanternBlockEntity lantern) {
            lantern.setRemainingBurnTime(Math.min(burnTime, lantern.getMaxBurnTime()));
        } else if (entity instanceof CampfireBlockEntity campfire) {
            BurnTimeManager.setCurrentBurnTime(campfire, burnTime);
        }
    }

    /**
     * Converts a lit block to its unlit version once burn time reaches 0.
     * Preserves properties like FACING for wall torches and HANGING for lanterns.
     */
    public static void convertToUnlit(World world, BlockPos pos, net.minecraft.block.BlockState state) {
        if (state.getBlock() instanceof net.minecraft.block.TorchBlock) {
            if (state.getBlock() instanceof net.minecraft.block.WallTorchBlock) {
                world.setBlockState(pos, RegistryHandler.UNLIT_WALL_TORCH_BLOCK.getDefaultState()
                                .with(net.minecraft.block.WallTorchBlock.FACING, state.get(net.minecraft.block.WallTorchBlock.FACING)),
                        net.minecraft.block.Block.NOTIFY_ALL);
            } else {
                world.setBlockState(pos, RegistryHandler.UNLIT_TORCH_BLOCK.getDefaultState(), net.minecraft.block.Block.NOTIFY_ALL);
            }
        } else if (state.getBlock() instanceof net.minecraft.block.LanternBlock) {
            world.setBlockState(pos, RegistryHandler.UNLIT_LANTERN_BLOCK.getDefaultState()
                            .with(net.minecraft.block.LanternBlock.HANGING, state.get(net.minecraft.block.LanternBlock.HANGING)),
                    net.minecraft.block.Block.NOTIFY_ALL);
        } else if (state.getBlock() instanceof net.minecraft.block.CampfireBlock) {
            world.setBlockState(pos, state.with(net.minecraft.block.CampfireBlock.LIT, false), net.minecraft.block.Block.NOTIFY_ALL);
        }
    }
}