package com.enchantedwisp.torchesbt.util;

import com.enchantedwisp.torchesbt.registry.RegistryHandler;
import com.enchantedwisp.torchesbt.registry.blocks.UnlitWallTorchBlock;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReignitionHandler {
    private static final Logger LOGGER = LogManager.getLogger("torchesbt");

    public static void register() {
        // BLOCK REIGNITION
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);

            if (stack.getItem() == Items.FLINT_AND_STEEL) {
                // Torch blocks
                if (state.getBlock() == RegistryHandler.UNLIT_TORCH_BLOCK || state.getBlock() == RegistryHandler.UNLIT_WALL_TORCH_BLOCK) {
                    BlockState newState = (state.getBlock() == RegistryHandler.UNLIT_TORCH_BLOCK ? Blocks.TORCH : Blocks.WALL_TORCH).getDefaultState();
                    if (state.getBlock() == RegistryHandler.UNLIT_WALL_TORCH_BLOCK) {
                        newState = newState.with(WallTorchBlock.FACING, state.get(UnlitWallTorchBlock.FACING));
                    }
                    world.setBlockState(pos, newState);
                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);

                    damageFlintAndSteel(stack, player, hand); // 🔥 now takes damage


                    return ActionResult.SUCCESS;
                }

                // Lantern blocks
                else if (state.getBlock() == RegistryHandler.UNLIT_LANTERN_BLOCK) {
                    BlockState newState = Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, state.get(LanternBlock.HANGING));
                    world.setBlockState(pos, newState);
                    world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);

                    damageFlintAndSteel(stack, player, hand); // 🔥 now takes damage


                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });


        // ITEM REIGNITION
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            ItemStack offHandStack = player.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);

            // Torch item reignition
            if (stack.getItem() == RegistryHandler.UNLIT_TORCH && offHandStack.getItem() == Items.FLINT_AND_STEEL) {
                ItemStack newStack = new ItemStack(Items.TORCH, stack.getCount());
                player.setStackInHand(hand, newStack);
                damageFlintAndSteel(offHandStack, player, hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
                player.playSound(SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
                LOGGER.debug("Converted unlit torch to lit torch in player {} hand: {}", player.getName().getString(), newStack.getItem());
                return TypedActionResult.success(newStack);
            }

            // Lantern item reignition
            else if (stack.getItem() == RegistryHandler.UNLIT_LANTERN && offHandStack.getItem() == Items.FLINT_AND_STEEL) {
                ItemStack newStack = new ItemStack(Items.LANTERN, stack.getCount());
                player.setStackInHand(hand, newStack);
                damageFlintAndSteel(offHandStack, player, hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
                player.playSound(SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
                LOGGER.debug("Converted unlit lantern to lit lantern in player {} hand: {}", player.getName().getString(), newStack.getItem());
                return TypedActionResult.success(newStack);
            }
            return TypedActionResult.pass(stack);
        });
    }

    private static void damageFlintAndSteel(ItemStack stack, PlayerEntity player, Hand hand) {
        if (stack.isDamageable()) {
            stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
            LOGGER.debug("Damaged flint and steel in player {} hand {}: durability={}", player.getName().getString(), hand, stack.getMaxDamage() - stack.getDamage());
        }
    }
}
