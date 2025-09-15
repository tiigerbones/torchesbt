package com.enchantedwisp.torchesbt.ignition;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.burn.BurnTimeManager;
import com.enchantedwisp.torchesbt.burn.BurnTimeUtils;
import com.enchantedwisp.torchesbt.mixinaccess.ICampfireBurnAccessor;
import com.enchantedwisp.torchesbt.registry.BurnableRegistry;
import com.enchantedwisp.torchesbt.util.JsonLoader;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * Handles ignition of unlit torches, lanterns, and campfires using defined igniter items.
 *
 * <p>Igniters are defined in {@link JsonLoader#IGNITERS}.</p>
 */
public class IgnitionHandler {
    private static final Logger LOGGER = RealisticTorchesBT.LOGGER;

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            ItemStack offHandStack = player.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);

            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            Identifier offHandItemId = Registries.ITEM.getId(offHandStack.getItem());

            // Check if the off-hand item is an igniter and the main hand item is an unlit burnable
            if (JsonLoader.IGNITERS.containsKey(offHandItemId)) {
                Item litItem = BurnableRegistry.getLitItem(stack.getItem());
                if (litItem != null) {
                    return igniteItem(world, hand, stack, offHandStack, litItem, stack.getItem(), player);
                }
            }

            // Check if the main hand item is an igniter and the off-hand item is an unlit burnable
            if (JsonLoader.IGNITERS.containsKey(itemId)) {
                Item litItem = BurnableRegistry.getLitItem(offHandStack.getItem());
                if (litItem != null) {
                    return igniteItem(world, hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND, offHandStack, stack, litItem, offHandStack.getItem(), player);
                }
            }

            return TypedActionResult.pass(stack);
        });

        // Block ignition (e.g., unlit campfire)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Block litBlock = BurnableRegistry.getLitBlock(state.getBlock());

            if (JsonLoader.IGNITERS.containsKey(itemId) && litBlock != null) {
                if (litBlock == Blocks.CAMPFIRE && !state.get(CampfireBlock.LIT)) {
                    long igniteTicks = JsonLoader.IGNITERS.get(itemId) * 20L;
                    world.setBlockState(pos, state.with(CampfireBlock.LIT, true), 3);
                    ICampfireBurnAccessor accessor = (ICampfireBurnAccessor) world.getBlockEntity(pos);
                    if (accessor != null) {
                        accessor.torchesbt_setBurnTime(igniteTicks);
                        LOGGER.debug("Ignited campfire at {} with burn time {}", pos, igniteTicks);
                    }
                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
                    ReignitionHandler.consumeIgniter(stack, player, hand);
                    return ActionResult.SUCCESS;
                } else if (litBlock != state.getBlock()) {
                    world.setBlockState(pos, litBlock.getDefaultState(), 3);
                    BurnTimeManager.setBurnTimeOnPlacement(world, pos, world.getBlockEntity(pos), stack, JsonLoader.IGNITERS.get(itemId) * 20L);
                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
                    ReignitionHandler.consumeIgniter(stack, player, hand);
                    LOGGER.debug("Ignited block {} at {} using {}", litBlock, pos, stack.getItem());
                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.PASS;
        });
    }

    private static TypedActionResult<ItemStack> igniteItem(World world, Hand hand, ItemStack stack, ItemStack igniter,
                                                          Item litItem, Item unlitItem, PlayerEntity player) {
        if (world.isClient) return TypedActionResult.pass(stack);

        Identifier igniterId = Registries.ITEM.getId(igniter.getItem());
        long igniteTicks = JsonLoader.IGNITERS.get(igniterId) * 20L;  // Get from JSON, convert to ticks

        int stackCount = stack.getCount();
        ItemStack newStack = new ItemStack(litItem, 1);
        BurnTimeUtils.setCurrentBurnTime(newStack, igniteTicks);  // Set to JSON amount instead of init (max)
        player.setStackInHand(hand, newStack);
        ReignitionHandler.consumeIgniter(igniter, player, hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
        player.playSound(SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);

        // Handle remaining unlit items
        if (stackCount > 1) {
            ItemStack remaining = new ItemStack(unlitItem, stackCount - 1);
            if (!player.getInventory().insertStack(remaining)) {
                world.spawnEntity(new net.minecraft.entity.ItemEntity(world, player.getX(), player.getY(), player.getZ(), remaining));
            }
        }
        return TypedActionResult.success(newStack);
    }
}