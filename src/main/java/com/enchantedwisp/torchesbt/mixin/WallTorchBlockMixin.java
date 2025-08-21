package com.enchantedwisp.torchesbt.mixin;

import com.enchantedwisp.torchesbt.burn.BurnTimeManager;
import com.enchantedwisp.torchesbt.blockentity.TorchBlockEntity;
import com.enchantedwisp.torchesbt.util.ConfigCache;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin to add block entity support and burn time handling for WallTorchBlock.
 */
@Mixin(WallTorchBlock.class)
public abstract class WallTorchBlockMixin extends Block implements BlockEntityProvider {
    public WallTorchBlockMixin(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TorchBlockEntity(pos, state);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof TorchBlockEntity) {
            BurnTimeManager.setBurnTimeOnPlacement(world, pos, entity, itemStack, ConfigCache.getTorchBurnTime());
        }
    }
}