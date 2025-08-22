package com.enchantedwisp.torchesbt.ignition;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.burn.Burnable;
import com.enchantedwisp.torchesbt.mixinaccess.ICampfireBurnAccessor;
import com.enchantedwisp.torchesbt.util.JsonLoader;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;

/**
 * Handles fueling of campfires and lanterns using defined fuel items.
 *
 * <p>Fuels are defined in {@link JsonLoader#CAMPFIRE_FUELS} and
 * {@link JsonLoader#LANTERN_FUELS}.</p>
 */
public class FuelHandler {
    private static final Logger LOGGER = RealisticTorchesBT.LOGGER;

    /**
     * Registers fueling logic for campfires and lanterns.
     * Triggered when a player right-clicks with a fuel item.
     */
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            BlockPos pos = hitResult.getBlockPos();
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            BlockEntity entity = world.getBlockEntity(pos);

            // Campfire fueling
            if (JsonLoader.CAMPFIRE_FUELS.containsKey(itemId)
                    && world.getBlockState(pos).getBlock() == Blocks.CAMPFIRE
                    && world.getBlockState(pos).get(CampfireBlock.LIT)) {
                if (entity instanceof ICampfireBurnAccessor accessor) {
                    long current = accessor.torchesbt_getBurnTime();
                    long added = JsonLoader.CAMPFIRE_FUELS.get(itemId) * 20L;
                    accessor.torchesbt_setBurnTime(current + added);

                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
                    ReignitionHandler.consumeFuel(stack, player, hand);
                    LOGGER.debug("Fueled campfire at {} by {} using {}, {} -> {}", pos, player.getName().getString(), stack.getItem(), current, current + added);
                    return ActionResult.SUCCESS;
                }
            }

            // Lantern fueling
            if (JsonLoader.LANTERN_FUELS.containsKey(itemId)
                    && world.getBlockState(pos).getBlock() == Blocks.LANTERN) {
                if (entity instanceof Burnable burnable) {
                    long current = burnable.getRemainingBurnTime();
                    long added = JsonLoader.LANTERN_FUELS.get(itemId) * 20L;
                    burnable.setRemainingBurnTime(current + added);

                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
                    ReignitionHandler.consumeFuel(stack, player, hand);
                    LOGGER.debug("Fueled lantern at {} by {} using {}, {} -> {}", pos, player.getName().getString(), stack.getItem(), current, current + added);
                    return ActionResult.SUCCESS;
                }
            }

            // Lantern fueling
            if (JsonLoader.TORCH_FUELS.containsKey(itemId)
                    && (world.getBlockState(pos).getBlock() == Blocks.TORCH || world.getBlockState(pos).getBlock() == Blocks.WALL_TORCH)) {
                if (entity instanceof Burnable burnable) {
                    long current = burnable.getRemainingBurnTime();
                    long added = JsonLoader.TORCH_FUELS.get(itemId) * 20L;
                    burnable.setRemainingBurnTime(current + added);

                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
                    ReignitionHandler.consumeFuel(stack, player, hand);
                    LOGGER.debug("Fueled lantern at {} by {} using {}, {} -> {}", pos, player.getName().getString(), stack.getItem(), current, current + added);
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
    }
}
