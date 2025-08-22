package com.enchantedwisp.torchesbt.burn;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.blockentity.LanternBlockEntity;
import com.enchantedwisp.torchesbt.blockentity.TorchBlockEntity;
import com.enchantedwisp.torchesbt.mixinaccess.ICampfireBurnAccessor;
import com.enchantedwisp.torchesbt.registry.RegistryHandler;
import com.enchantedwisp.torchesbt.registry.blocks.UnlitWallTorchBlock;
import com.enchantedwisp.torchesbt.util.ConfigCache;
import com.enchantedwisp.torchesbt.util.JsonLoader;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.ItemEntity;
import org.slf4j.Logger;

/**
 * Handles reignition and fueling of burnable blocks and items using igniters and fuels defined in JSON.
 */
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
                if (world.getFluidState(pos).isIn(FluidTags.WATER)) return ActionResult.PASS;
                // --- Torch blocks ---
                if (state.getBlock() == RegistryHandler.UNLIT_TORCH_BLOCK || state.getBlock() == RegistryHandler.UNLIT_WALL_TORCH_BLOCK) {
                    BlockState newState = (state.getBlock() == RegistryHandler.UNLIT_TORCH_BLOCK ? Blocks.TORCH : Blocks.WALL_TORCH).getDefaultState();
                    if (state.getBlock() == RegistryHandler.UNLIT_WALL_TORCH_BLOCK) {
                        newState = newState.with(WallTorchBlock.FACING, state.get(WallTorchBlock.FACING));
                    }
                    if (newState.canPlaceAt(world, pos)) {
                        world.setBlockState(pos, newState, 3);
                        BlockEntity entity = world.getBlockEntity(pos);
                        if (entity instanceof Burnable burnable) {
                            burnable.setRemainingBurnTime(burnable.getMaxBurnTime());
                        }
                        world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        consumeIgniter(stack, player, hand);
                        LOGGER.debug("Ignited {} at {} by player {} using {}", state.getBlock(), pos, player.getName().getString(), stack.getItem());
                        return ActionResult.SUCCESS;
                    }
                }
                // --- Lantern block ---
                else if (state.getBlock() == RegistryHandler.UNLIT_LANTERN_BLOCK) {
                    BlockState newState = Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, state.get(LanternBlock.HANGING));
                    if (newState.canPlaceAt(world, pos)) {
                        world.setBlockState(pos, newState, 3);
                        BlockEntity entity = world.getBlockEntity(pos);
                        if (entity instanceof Burnable burnable) {
                            burnable.setRemainingBurnTime(burnable.getMaxBurnTime());
                        }
                        world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        consumeIgniter(stack, player, hand);
                        LOGGER.debug("Ignited {} at {} by player {} using {}", state.getBlock(), pos, player.getName().getString(), stack.getItem());
                        return ActionResult.SUCCESS;
                    }
                }
                // --- Campfire block ---
                else if (state.getBlock() == Blocks.CAMPFIRE && !state.get(CampfireBlock.LIT)) {
                    BlockState newState = state.with(CampfireBlock.LIT, true);
                    if (newState.canPlaceAt(world, pos)) {
                        world.setBlockState(pos, newState, 3);
                        BlockEntity entity = world.getBlockEntity(pos);
                        if (entity instanceof ICampfireBurnAccessor accessor) {
                            accessor.torchesbt_setBurnTime(ConfigCache.getCampfireBurnTime());
                        }
                        world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE,
                                SoundCategory.BLOCKS, 1.0F, 1.0F);
                        consumeIgniter(stack, player, hand);
                        LOGGER.debug("Ignited campfire at {} by player {} using {}",
                                pos, player.getName().getString(), stack.getItem());
                        return ActionResult.SUCCESS;
                    }
                }
            }
            // -------------------------------
            // FUELS
            // -------------------------------
            else if (JsonLoader.CAMPFIRE_FUELS.containsKey(itemId)
                    && state.getBlock() == Blocks.CAMPFIRE
                    && state.get(CampfireBlock.LIT)) {
                BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof ICampfireBurnAccessor accessor) {
                    long currentBurnTime = accessor.torchesbt_getBurnTime();
                    long added = JsonLoader.CAMPFIRE_FUELS.get(itemId) * 20L; // convert seconds to ticks
                    long newBurnTime = currentBurnTime + added;

                    accessor.torchesbt_setBurnTime(newBurnTime);

                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE,
                            SoundCategory.BLOCKS, 1.0F, 1.0F);
                    consumeFuel(stack, player, hand);

                    LOGGER.debug("Fueled campfire at {} by player {} using {}, burn time: {} -> {}",
                            pos, player.getName().getString(), stack.getItem(), currentBurnTime, newBurnTime);
                    return ActionResult.SUCCESS;
                }
            }

            else if (JsonLoader.LANTERN_FUELS.containsKey(itemId) && state.getBlock() == Blocks.LANTERN) {
                BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof Burnable burnable) {
                    long currentBurnTime = burnable.getRemainingBurnTime();
                    long newBurnTime = currentBurnTime + JsonLoader.LANTERN_FUELS.get(itemId) * 20L;
                    burnable.setRemainingBurnTime(newBurnTime);
                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    consumeFuel(stack, player, hand);
                    LOGGER.debug("Fueled lantern at {} by player {} using {}, burn time: {} -> {}", pos, player.getName().getString(), stack.getItem(), currentBurnTime, newBurnTime);
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
            Identifier offHandItemId = Registries.ITEM.getId(offHandStack.getItem());

            // Check for unlit torch in main hand and igniter in off-hand
            if (stack.getItem() == RegistryHandler.UNLIT_TORCH && JsonLoader.IGNITERS.containsKey(offHandItemId)) {
                if (player.isSubmergedIn(FluidTags.WATER)) {
                    return TypedActionResult.fail(stack);
                }
                int stackCount = stack.getCount();
                ItemStack newStack = new ItemStack(Items.TORCH, 1);
                BurnTimeUtils.initializeBurnTime(newStack);
                player.setStackInHand(hand, newStack);
                consumeIgniter(offHandStack, player, hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
                player.playSound(SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);

                // Handle remaining unlit torches
                if (stackCount > 1) {
                    ItemStack remainingStack = new ItemStack(RegistryHandler.UNLIT_TORCH, stackCount - 1);
                    if (!player.getInventory().insertStack(remainingStack)) {
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
            // Check for unlit lantern in main hand and igniter in off-hand
            else if (stack.getItem() == RegistryHandler.UNLIT_LANTERN && JsonLoader.IGNITERS.containsKey(offHandItemId)) {
                if (player.isSubmergedIn(FluidTags.WATER)) {
                    return TypedActionResult.fail(stack);
                }
                int stackCount = stack.getCount();
                ItemStack newStack = new ItemStack(Items.LANTERN, 1);
                BurnTimeUtils.initializeBurnTime(newStack);
                player.setStackInHand(hand, newStack);
                consumeIgniter(offHandStack, player, hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
                player.playSound(SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);

                // Handle remaining unlit lanterns
                if (stackCount > 1) {
                    ItemStack remainingStack = new ItemStack(RegistryHandler.UNLIT_LANTERN, stackCount - 1);
                    if (!player.getInventory().insertStack(remainingStack)) {
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