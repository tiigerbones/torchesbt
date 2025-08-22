package com.enchantedwisp.torchesbt.burn;

import com.enchantedwisp.torchesbt.mixinaccess.ICampfireBurnAccessor;
import com.enchantedwisp.torchesbt.util.ConfigCache;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.Objects;

/**
 * Utility methods for handling burn times, rain checks, and item/stack operations.
 */
public class BurnTimeUtils {
    public static final String BURN_TIME_KEY = "remaining_burn";

    public static long getMaxBurnTime(ItemStack stack) {
        if (stack.getItem() == Items.TORCH) return ConfigCache.getTorchBurnTime();
        if (stack.getItem() == Items.LANTERN) return ConfigCache.getLanternBurnTime();
        return 0;
    }

    public static double getRainMultiplier(net.minecraft.item.Item item) {
        if (item == Items.TORCH) return ConfigCache.getRainTorchMultiplier();
        if (item == Items.LANTERN) return ConfigCache.getRainLanternMultiplier();
        return 1.0;
    }

    public static long getCurrentBurnTime(ItemStack stack) {
        return stack.hasNbt() && Objects.requireNonNull(stack.getNbt()).contains(BURN_TIME_KEY)
                ? stack.getNbt().getLong(BURN_TIME_KEY)
                : getMaxBurnTime(stack);
    }

    public static void setCurrentBurnTime(ItemStack stack, long burnTime) {
        stack.getOrCreateNbt().putLong(BURN_TIME_KEY, Math.min(burnTime, getMaxBurnTime(stack)));
    }

    public static long getCurrentBurnTime(BlockEntity entity) {
        if (entity instanceof Burnable burnable) {
            return burnable.getRemainingBurnTime();
        }
        if (entity instanceof ICampfireBurnAccessor accessor) {
            return accessor.torchesbt_getBurnTime();
        }
        return 0;
    }

    public static void setCurrentBurnTime(BlockEntity entity, long burnTime) {
        if (entity instanceof Burnable burnable) {
            burnable.setRemainingBurnTime(burnTime);
        } else if (entity instanceof ICampfireBurnAccessor accessor) {
            accessor.torchesbt_setBurnTime(burnTime);
            entity.markDirty();
        }
    }

    public static void initializeBurnTime(ItemStack stack) {
        setCurrentBurnTime(stack, getMaxBurnTime(stack));
    }

    public static ItemStack splitAndInitializeStack(ItemStack stack, int count) {
        ItemStack newStack = stack.copy();
        newStack.setCount(count);
        long burnTime = getCurrentBurnTime(stack);
        setCurrentBurnTime(newStack, burnTime);
        stack.decrement(count);
        return newStack;
    }

    // --- Centralized rain check ---
    public static boolean isActuallyRainingAt(World world, BlockPos pos) {
        if (world.getFluidState(pos).isIn(FluidTags.WATER)) return false; // Submersion handled separately
        Biome biome = world.getBiome(pos).value();
        Biome.Precipitation precipitation = biome.getPrecipitation(pos);
        return ConfigCache.isRainExtinguishEnabled() &&
                world.isRaining() &&
                world.isSkyVisible(pos) &&
                (precipitation == Biome.Precipitation.RAIN || precipitation == Biome.Precipitation.SNOW);
    }

    public static boolean isBurnableItem(ItemStack stack) {
        return stack.getItem() == Items.TORCH || stack.getItem() == Items.LANTERN;
    }
}