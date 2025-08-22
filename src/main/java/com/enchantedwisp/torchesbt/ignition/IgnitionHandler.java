package com.enchantedwisp.torchesbt.ignition;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.burn.BurnTimeUtils;
import com.enchantedwisp.torchesbt.burn.Burnable;
import com.enchantedwisp.torchesbt.mixinaccess.ICampfireBurnAccessor;
import com.enchantedwisp.torchesbt.registry.RegistryHandler;
import com.enchantedwisp.torchesbt.util.ConfigCache;
import com.enchantedwisp.torchesbt.util.JsonLoader;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;

/**
 * Handles reignition (lighting) of torches, lanterns, and campfires,
 * as well as handheld unlit items using defined igniters.
 *
 * <p>Igniters are defined in {@link JsonLoader#IGNITERS}.
 * This class listens to {@link UseBlockCallback} and {@link UseItemCallback}
 * to apply ignition behavior.</p>
 */
public class IgnitionHandler {
    private static final Logger LOGGER = RealisticTorchesBT.LOGGER;

    /**
     * Registers block and item ignition event handlers.
     * <ul>
     *     <li>Right-clicking unlit torches/lanterns/campfires with an igniter lights them.</li>
     *     <li>Holding an unlit torch/lantern in one hand and an igniter in the other lights it.</li>
     * </ul>
     */
    public static void register() {
        registerBlockIgnition();
        registerItemIgnition();
    }

    /**
     * Registers logic for igniting blocks (torches, lanterns, campfires).
     */
    private static void registerBlockIgnition() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Identifier itemId = Registries.ITEM.getId(stack.getItem());

            if (!JsonLoader.IGNITERS.containsKey(itemId)) return ActionResult.PASS;
            if (world.getFluidState(pos).isIn(FluidTags.WATER)) return ActionResult.PASS;

            // Torch ignition
            if (state.getBlock() == RegistryHandler.UNLIT_TORCH_BLOCK || state.getBlock() == RegistryHandler.UNLIT_WALL_TORCH_BLOCK) {
                return igniteTorchBlock(world, pos, state, stack, player, hand);
            }
            // Lantern ignition
            else if (state.getBlock() == RegistryHandler.UNLIT_LANTERN_BLOCK) {
                return igniteLanternBlock(world, pos, state, stack, player, hand);
            }
            // Campfire ignition
            else if (state.getBlock() == Blocks.CAMPFIRE && !state.get(CampfireBlock.LIT)) {
                return igniteCampfireBlock(world, pos, state, stack, player, hand);
            }
            return ActionResult.PASS;
        });
    }

    /**
     * Registers logic for igniting items in-hand (torches and lanterns).
     */
    private static void registerItemIgnition() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));

            ItemStack stack = player.getStackInHand(hand);
            ItemStack offHandStack = player.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);
            Identifier offHandItemId = Registries.ITEM.getId(offHandStack.getItem());

            if (!JsonLoader.IGNITERS.containsKey(offHandItemId)) return TypedActionResult.pass(stack);
            if (player.isSubmergedIn(FluidTags.WATER)) return TypedActionResult.fail(stack);

            // Torch in hand
            if (stack.getItem() == RegistryHandler.UNLIT_TORCH) {
                return igniteHeldItem(player, world, hand, stack, offHandStack, Items.TORCH, RegistryHandler.UNLIT_TORCH);
            }
            // Lantern in hand
            else if (stack.getItem() == RegistryHandler.UNLIT_LANTERN) {
                return igniteHeldItem(player, world, hand, stack, offHandStack, Items.LANTERN, RegistryHandler.UNLIT_LANTERN);
            }
            return TypedActionResult.pass(stack);
        });
    }

    // -------------------------------
    // Block ignition helpers
    // -------------------------------

    private static ActionResult igniteTorchBlock(net.minecraft.world.World world, BlockPos pos, BlockState state,
                                                 ItemStack stack, PlayerEntity player, Hand hand) {
        Identifier itemId = Registries.ITEM.getId(stack.getItem());  // Get the igniter's ID to look up in JSON
        long igniteTicks = JsonLoader.IGNITERS.get(itemId) * 20L;  // Fetch ignite_amount from JSON and convert seconds to ticks

        BlockState newState;
        if (state.getBlock() == RegistryHandler.UNLIT_TORCH_BLOCK) {
            newState = Blocks.TORCH.getDefaultState();
        } else {
            newState = Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, state.get(WallTorchBlock.FACING));
        }

        if (newState.canPlaceAt(world, pos)) {
            world.setBlockState(pos, newState, 3);
            if (world.getBlockEntity(pos) instanceof Burnable burnable) {
                burnable.setRemainingBurnTime(igniteTicks);  // Set to JSON amount instead of max
            }
            world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
            ReignitionHandler.consumeIgniter(stack, player, hand);
            LOGGER.debug("Ignited torch at {} by {} using {}, set to {} ticks", pos, player.getName().getString(), stack.getItem(), igniteTicks);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    private static ActionResult igniteLanternBlock(net.minecraft.world.World world, BlockPos pos, BlockState state,
                                                   ItemStack stack, PlayerEntity player, Hand hand) {
        Identifier itemId = Registries.ITEM.getId(stack.getItem());  // Already have this from caller
        long igniteTicks = JsonLoader.IGNITERS.get(itemId) * 20L;  // Get from JSON, convert seconds to ticks

        BlockState newState = Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, state.get(LanternBlock.HANGING));
        if (newState.canPlaceAt(world, pos)) {
            world.setBlockState(pos, newState, 3);
            if (world.getBlockEntity(pos) instanceof Burnable burnable) {
                burnable.setRemainingBurnTime(igniteTicks);  // Use JSON amount instead of max
            }
            world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
            ReignitionHandler.consumeIgniter(stack, player, hand);
            LOGGER.debug("Ignited lantern at {} by {} using {}, set to {} ticks", pos, player.getName().getString(), stack.getItem(), igniteTicks);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    private static ActionResult igniteCampfireBlock(net.minecraft.world.World world, BlockPos pos, BlockState state,
                                                    ItemStack stack, PlayerEntity player, Hand hand) {
        Identifier itemId = Registries.ITEM.getId(stack.getItem());  // Already have this from caller
        long igniteTicks = JsonLoader.IGNITERS.get(itemId) * 20L;  // Get from JSON, convert seconds to ticks

        BlockState newState = state.with(CampfireBlock.LIT, true);
        world.setBlockState(pos, newState, 3);
        if (world.getBlockEntity(pos) instanceof ICampfireBurnAccessor accessor) {
            accessor.torchesbt_setBurnTime(igniteTicks);  // Use JSON amount instead of config max
        }
        world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1, 1);
        ReignitionHandler.consumeIgniter(stack, player, hand);
        LOGGER.debug("Ignited campfire at {} by {} using {}, set to {} ticks", pos, player.getName().getString(), stack.getItem(), igniteTicks);
        return ActionResult.SUCCESS;
    }

    // -------------------------------
    // Handheld item ignition helper
    // -------------------------------

    /**
     * Converts an unlit handheld item into its lit version using an igniter.
     *
     * @param player   The player performing the action
     * @param world    The world
     * @param hand     The hand holding the unlit item
     * @param stack    The unlit stack
     * @param igniter  The igniter stack
     * @param litItem  The lit item type
     * @param unlitItem The unlit item type
     * @return Result containing the lit stack
     */
    private static TypedActionResult<ItemStack> igniteHeldItem(PlayerEntity player, net.minecraft.world.World world,
                                                               Hand hand, ItemStack stack, ItemStack igniter,
                                                               net.minecraft.item.Item litItem, net.minecraft.item.Item unlitItem) {
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
                world.spawnEntity(new ItemEntity(world, player.getX(), player.getY(), player.getZ(), remaining));
            }
        }
        return TypedActionResult.success(newStack);
    }
}
