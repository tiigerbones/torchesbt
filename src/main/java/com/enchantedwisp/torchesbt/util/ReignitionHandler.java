package com.enchantedwisp.torchesbt.util;

import com.enchantedwisp.torchesbt.registry.RegistryHandler;
import com.enchantedwisp.torchesbt.registry.blocks.UnlitWallTorchBlock;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReignitionHandler {
    private static final Logger LOGGER = LogManager.getLogger("torchesbt");

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

                // --- Torch blocks (igniters only) ---
                if (state.getBlock() == RegistryHandler.UNLIT_TORCH_BLOCK || state.getBlock() == RegistryHandler.UNLIT_WALL_TORCH_BLOCK) {
                    BlockState newState = (state.getBlock() == RegistryHandler.UNLIT_TORCH_BLOCK ? Blocks.TORCH : Blocks.WALL_TORCH).getDefaultState();
                    if (state.getBlock() == RegistryHandler.UNLIT_WALL_TORCH_BLOCK) {
                        newState = newState.with(WallTorchBlock.FACING, state.get(UnlitWallTorchBlock.FACING));
                    }
                    if (world.getBlockEntity(pos) != null) world.removeBlockEntity(pos);
                    world.setBlockState(pos, newState);
                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    consumeIgniter(stack, player, hand);
                    return ActionResult.CONSUME;
                }

                // --- Lantern blocks ---
                if (state.getBlock() == RegistryHandler.UNLIT_LANTERN_BLOCK) {
                    BlockState newState = Blocks.LANTERN.getDefaultState()
                            .with(LanternBlock.HANGING, state.get(LanternBlock.HANGING));
                    if (world.getBlockEntity(pos) != null) world.removeBlockEntity(pos);
                    world.setBlockState(pos, newState);
                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    consumeIgniter(stack, player, hand);
                    return ActionResult.CONSUME;
                }

                // --- Campfires (unlit only) ---
                if (state.getBlock() == Blocks.CAMPFIRE && !state.get(CampfireBlock.LIT)) {
                    BlockState newState = state.with(CampfireBlock.LIT, true);
                    if (world.getBlockEntity(pos) != null) world.removeBlockEntity(pos);
                    world.setBlockState(pos, newState);
                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    consumeIgniter(stack, player, hand);
                    return ActionResult.CONSUME;
                }
            }

            // -------------------------------
            // FUELS (consume on right-click, sizzle sound)
            // -------------------------------

            // Lanterns
            if (state.getBlock() == Blocks.LANTERN && JsonLoader.LANTERN_FUELS.containsKey(itemId)) {
                int extraTime = JsonLoader.LANTERN_FUELS.get(itemId);
                consumeFuel(stack, player, hand); // consume immediately
                world.playSound(null, pos, SoundEvents.BLOCK_LANTERN_HIT, SoundCategory.BLOCKS, 1.0F, 1.0F); // sizzle sound
                LOGGER.debug("Added {} ticks of fuel to lantern at {} using {}", extraTime, pos, itemId);
                return ActionResult.SUCCESS;
            }

            // Lit campfires
            if (state.getBlock() == Blocks.CAMPFIRE && state.get(CampfireBlock.LIT) && JsonLoader.CAMPFIRE_FUELS.containsKey(itemId)) {
                int extraTime = JsonLoader.CAMPFIRE_FUELS.get(itemId);
                consumeFuel(stack, player, hand); // consume immediately
                world.playSound(null, pos, SoundEvents.BLOCK_LAVA_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F); // sizzle sound
                LOGGER.debug("Added {} ticks of fuel to campfire at {} using {}", extraTime, pos, itemId);
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });

        // -------------------------------
        // ITEM REIGNITION (inventory items)
        // -------------------------------
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            ItemStack offHandStack = player.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);

            Identifier stackId = Registries.ITEM.getId(stack.getItem());
            Identifier offId = Registries.ITEM.getId(offHandStack.getItem());

            // Torch item reignition
            if (stack.getItem() == RegistryHandler.UNLIT_TORCH && JsonLoader.IGNITERS.containsKey(offId)) {
                ItemStack newStack = new ItemStack(Blocks.TORCH.asItem(), stack.getCount());
                player.setStackInHand(hand, newStack);
                consumeIgniter(offHandStack, player, hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
                player.playSound(SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
                LOGGER.debug("Converted unlit torch to lit torch in player {} hand: {}", player.getName().getString(), newStack.getItem());
                return TypedActionResult.success(newStack);
            }

            // Lantern item reignition
            if (stack.getItem() == RegistryHandler.UNLIT_LANTERN && JsonLoader.IGNITERS.containsKey(offId)) {
                ItemStack newStack = new ItemStack(Blocks.LANTERN.asItem(), stack.getCount());
                player.setStackInHand(hand, newStack);
                consumeIgniter(offHandStack, player, hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
                player.playSound(SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
                LOGGER.debug("Converted unlit lantern to lit lantern in player {} hand: {}", player.getName().getString(), newStack.getItem());
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
