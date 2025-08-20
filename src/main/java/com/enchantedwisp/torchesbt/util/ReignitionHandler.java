package com.enchantedwisp.torchesbt.util;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.blockentity.LanternBlockEntity;
import com.enchantedwisp.torchesbt.blockentity.TorchBlockEntity;
import com.enchantedwisp.torchesbt.registry.RegistryHandler;
import com.enchantedwisp.torchesbt.registry.blocks.UnlitWallTorchBlock;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.ItemEntity;
import org.slf4j.Logger;

public class ReignitionHandler {
    private static final Logger LOGGER = RealisticTorchesBT.LOGGER;

    public static void register() {
        // -------------------------------
        // BLOCK REIGNITION & FUELS
        // -------------------------------
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Identifier itemId = Registries.ITEM.getId(stack.getItem());

            // -------------------------------
            // IGNITERS
            // -------------------------------
            if (JsonLoader.IGNITERS.containsKey(itemId)) {
                // --- Torch blocks ---
                if (state.getBlock() == RegistryHandler.UNLIT_TORCH_BLOCK || state.getBlock() == RegistryHandler.UNLIT_WALL_TORCH_BLOCK) {
                    BlockState newState = (state.getBlock() == RegistryHandler.UNLIT_TORCH_BLOCK ? Blocks.TORCH : Blocks.WALL_TORCH).getDefaultState();
                    if (state.getBlock() == RegistryHandler.UNLIT_WALL_TORCH_BLOCK) {
                        newState = newState.with(WallTorchBlock.FACING, state.get(UnlitWallTorchBlock.FACING));
                    }
                    world.setBlockState(pos, newState);
                    BlockEntity entity = world.getBlockEntity(pos);
                    if (entity instanceof TorchBlockEntity torch) {
                        torch.setRemainingBurnTime(torch.getMaxBurnTime());
                    }
                    consumeIgniter(stack, player, hand);
                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
                    LOGGER.debug("Reignited torch at {} by {}", pos, player.getName().getString());
                    return ActionResult.SUCCESS;
                }

                // --- Lantern block ---
                if (state.getBlock() == RegistryHandler.UNLIT_LANTERN_BLOCK) {
                    BlockState newState = Blocks.LANTERN.getDefaultState()
                            .with(LanternBlock.HANGING, state.get(LanternBlock.HANGING));
                    world.setBlockState(pos, newState);
                    BlockEntity entity = world.getBlockEntity(pos);
                    if (entity instanceof LanternBlockEntity lantern) {
                        lantern.setRemainingBurnTime(lantern.getMaxBurnTime());
                    }
                    consumeIgniter(stack, player, hand);
                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
                    LOGGER.debug("Reignited lantern at {} by {}", pos, player.getName().getString());
                    return ActionResult.SUCCESS;
                }

                // --- Campfire block ---
                if (state.getBlock() == Blocks.CAMPFIRE && !state.get(CampfireBlock.LIT)) {
                    world.setBlockState(pos, state.with(CampfireBlock.LIT, true));
                    BlockEntity entity = world.getBlockEntity(pos);
                    if (entity instanceof CampfireBlockEntity campfire) {
                        BurnTimeManager.setCurrentBurnTime(campfire, BurnTimeManager.getCurrentBurnTime(campfire) + JsonLoader.IGNITERS.get(itemId));
                    }
                    consumeIgniter(stack, player, hand);
                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
                    LOGGER.debug("Reignited campfire at {} by {}", pos, player.getName().getString());
                    return ActionResult.SUCCESS;
                }
            }

            // -------------------------------
            // FUELS
            // -------------------------------
            if (JsonLoader.CAMPFIRE_FUELS.containsKey(itemId) && state.getBlock() == Blocks.CAMPFIRE && state.get(CampfireBlock.LIT)) {
                BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof CampfireBlockEntity campfire) {
                    long current = BurnTimeManager.getCurrentBurnTime(campfire);
                    long addTime = JsonLoader.CAMPFIRE_FUELS.get(itemId);
                    BurnTimeManager.setCurrentBurnTime(campfire, current + addTime);
                    consumeFuel(stack, player, hand);
                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
                    LOGGER.debug("Added {} ticks to campfire at {} by {}", addTime, pos, player.getName().getString());
                    return ActionResult.SUCCESS;
                }
            }

            if (JsonLoader.LANTERN_FUELS.containsKey(itemId) && state.getBlock() == Blocks.LANTERN) {
                BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof LanternBlockEntity lantern) {
                    long current = lantern.getRemainingBurnTime();
                    long addTime = JsonLoader.LANTERN_FUELS.get(itemId);
                    lantern.setRemainingBurnTime(current + addTime);
                    consumeFuel(stack, player, hand);
                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
                    LOGGER.debug("Added {} ticks to lantern at {} by {}", addTime, pos, player.getName().getString());
                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.PASS;
        });

        // -------------------------------
        // ITEM REIGNITION
        // -------------------------------
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            ItemStack offHandStack = player.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
            Identifier offId = Registries.ITEM.getId(offHandStack.getItem());

            // Torch item reignition
            if (stack.getItem() == RegistryHandler.UNLIT_TORCH && JsonLoader.IGNITERS.containsKey(offId)) {
                int stackCount = stack.getCount();
                ItemStack newStack = new ItemStack(Items.TORCH, 1); // Ignite only one torch
                BurnTimeManager.setCurrentBurnTime(newStack, BurnTimeManager.getMaxBurnTime(newStack));
                player.setStackInHand(hand, newStack);
                consumeIgniter(offHandStack, player, hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
                player.playSound(SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);

                // Handle remaining unlit torches
                if (stackCount > 1) {
                    ItemStack remainingStack = new ItemStack(RegistryHandler.UNLIT_TORCH, stackCount - 1);
                    if (!player.getInventory().insertStack(remainingStack)) {
                        // No inventory space, drop the remaining stack
                        ItemEntity itemEntity = new ItemEntity(world, player.getX(), player.getY(), player.getZ(), remainingStack);
                        world.spawnEntity(itemEntity);
                        LOGGER.debug("Dropped {} unlit torches at {} due to full inventory for player {}", stackCount - 1, player.getPos(), player.getName().getString());
                    } else {
                        LOGGER.debug("Moved {} unlit torches to inventory for player {}", stackCount - 1, player.getName().getString());
                    }
                }

                LOGGER.debug("Converted 1 unlit torch to lit torch in player {} hand: {}", player.getName().getString(), newStack.getItem());
                return TypedActionResult.success(newStack);
            }

            // Lantern item reignition
            if (stack.getItem() == RegistryHandler.UNLIT_LANTERN && JsonLoader.IGNITERS.containsKey(offId)) {
                int stackCount = stack.getCount();
                ItemStack newStack = new ItemStack(Items.LANTERN, 1); // Ignite only one lantern
                BurnTimeManager.setCurrentBurnTime(newStack, BurnTimeManager.getMaxBurnTime(newStack));
                player.setStackInHand(hand, newStack);
                consumeIgniter(offHandStack, player, hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
                player.playSound(SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);

                // Handle remaining unlit lanterns
                if (stackCount > 1) {
                    ItemStack remainingStack = new ItemStack(RegistryHandler.UNLIT_LANTERN, stackCount - 1);
                    if (!player.getInventory().insertStack(remainingStack)) {
                        // No inventory space, drop the remaining stack
                        ItemEntity itemEntity = new ItemEntity(world, player.getX(), player.getY(), player.getZ(), remainingStack);
                        world.spawnEntity(itemEntity);
                        LOGGER.debug("Dropped {} unlit lanterns at {} due to full inventory for player {}", stackCount - 1, player.getPos(), player.getName().getString());
                    } else {
                        LOGGER.debug("Moved {} unlit lanterns to inventory for player {}", stackCount - 1, player.getName().getString());
                    }
                }

                LOGGER.debug("Converted 1 unlit lantern to lit lantern in player {} hand: {}", player.getName().getString(), newStack.getItem());
                return TypedActionResult.success(newStack);
            }

            return TypedActionResult.pass(stack);
        });
    }

    // -------------------------------
    // Helpers
    // -------------------------------
    private static void consumeIgniter(ItemStack stack, PlayerEntity player, Hand hand) {
        if (stack.isDamageable()) {
            stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
            LOGGER.debug("Damaged igniter {} in player {} hand {}: durability={}", stack.getItem(), player.getName().getString(), hand, stack.getMaxDamage() - stack.getDamage());
        } else if (!player.isCreative()) {
            stack.decrement(1);
            LOGGER.debug("Consumed igniter {} from player {} hand {}", stack.getItem(), player.getName().getString(), hand);
        }
    }

    private static void consumeFuel(ItemStack stack, PlayerEntity player, Hand hand) {
        if (!player.isCreative()) {
            stack.decrement(1);
            LOGGER.debug("Consumed fuel {} from player {} hand {}", stack.getItem(), player.getName().getString(), hand);
        }
    }
}