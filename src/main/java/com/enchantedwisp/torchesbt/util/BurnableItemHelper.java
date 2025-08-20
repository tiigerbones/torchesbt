package com.enchantedwisp.torchesbt.util;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public class BurnableItemHelper {
    private static final String REMAINING_KEY = "remaining_burn";

    /** Get remaining burn time from NBT, or default to max if missing */
    public static long getRemainingBurn(ItemStack stack, long defaultMax) {
        if (!stack.hasNbt()) {
            return defaultMax;
        }
        NbtCompound tag = stack.getNbt();
        assert tag != null;
        return tag.contains(REMAINING_KEY) ? tag.getLong(REMAINING_KEY) : defaultMax;
    }

    /** Set remaining burn time */
    public static void setRemainingBurn(ItemStack stack, long time) {
        NbtCompound tag = stack.getOrCreateNbt();
        tag.putLong(REMAINING_KEY, Math.max(0, time));
    }

    /** Initialize a fresh item with full burn time */
    public static void initBurn(ItemStack stack, long maxBurn) {
        setRemainingBurn(stack, maxBurn);
    }
}
