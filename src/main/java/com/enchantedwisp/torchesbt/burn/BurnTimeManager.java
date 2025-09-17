package com.enchantedwisp.torchesbt.burn;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.mixinaccess.ICampfireBurnAccessor;
import com.enchantedwisp.torchesbt.registry.BurnableRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;

import static com.enchantedwisp.torchesbt.ignition.IgnitionHandler.copyProperties;
import static com.enchantedwisp.torchesbt.integration.DynamicLightManager.isDynamicLightsEnabled;

/**
 * Handles burn time ticking for player-held items, nearby dropped items, and burnable blocks.
 * Only ticks items when Dynamic Lights is enabled.
 */
public class BurnTimeManager {
    private static final Logger LOGGER = RealisticTorchesBT.LOGGER;
    private static final int ITEM_UPDATE_INTERVAL = 5;
    private static final int SCAN_RADIUS = 32;
    private static final int SCAN_VERTICAL = 8;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % ITEM_UPDATE_INTERVAL != 0) return;

            for (PlayerEntity player : server.getPlayerManager().getPlayerList()) {
                processPlayerItems(player);
                processNearbyBurnables(player);
            }
        });

        LOGGER.info("Registered burn time tick handler");
    }

    // --- Tick player-held items ---
    private static void processPlayerItems(PlayerEntity player) {
        if (player.getWorld().isClient) return;

        for (Hand hand : Hand.values()) {
            ItemStack stack = player.getStackInHand(hand);
            if (!BurnableRegistry.isBurnableItem(stack.getItem())) continue;

            if (!isDynamicLightsEnabled()) continue;

            long burnTime = BurnTimeUtils.getCurrentBurnTime(stack);
            if (burnTime <= 0) {
                extinguishPlayerItem(player, hand, stack);
                continue;
            }

            double multiplier = BurnTimeUtils.isActuallyRainingAt(player.getWorld(), player.getBlockPos())
                    ? BurnableRegistry.getRainMultiplier(stack.getItem())
                    : 1.0;
            burnTime -= (long) Math.ceil(multiplier);
            BurnTimeUtils.setCurrentBurnTime(stack, burnTime);

            if (burnTime <= 0) {
                extinguishPlayerItem(player, hand, stack);
            }
        }
    }

    private static void extinguishPlayerItem(PlayerEntity player, Hand hand, ItemStack stack) {
        ItemStack unlit = new ItemStack(Objects.requireNonNull(BurnableRegistry.getUnlitItem(stack.getItem())), stack.getCount());
        player.setStackInHand(hand, unlit);

    }

    // --- Tick nearby burnables ---
    private static void processNearbyBurnables(PlayerEntity player) {
        World world = player.getWorld();
        if (world.isClient) return;

        BlockPos pos = player.getBlockPos();
        Box scanBox = new Box(pos).expand(SCAN_RADIUS, SCAN_VERTICAL, SCAN_RADIUS);

        // Blocks
        for (BlockPos blockPos : BlockPos.iterate(pos.add(-SCAN_RADIUS, -SCAN_VERTICAL, -SCAN_RADIUS),
                pos.add(SCAN_RADIUS, SCAN_VERTICAL, SCAN_RADIUS))) {
            BlockState state = world.getBlockState(blockPos);
            Block block = state.getBlock();
            BlockEntity entity = world.getBlockEntity(blockPos);

            if (entity instanceof Burnable burnable) {
                tickBurnableBlock(world, blockPos, state, block, burnable);
            } else if (entity instanceof ICampfireBurnAccessor campfire) {
                tickCampfire(world, blockPos, state, campfire);
            }
        }

        // Dropped items
        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, scanBox, e -> BurnableRegistry.isBurnableItem(e.getStack().getItem()));
        for (ItemEntity itemEntity : items) {
            if (!isDynamicLightsEnabled()) continue;

            ItemStack stack = itemEntity.getStack();
            long burnTime = BurnTimeUtils.getCurrentBurnTime(stack);
            if (burnTime <= 0) {
                ItemStack unlit = new ItemStack(Objects.requireNonNull(BurnableRegistry.getUnlitItem(stack.getItem())), stack.getCount());
                itemEntity.setStack(unlit);
                continue;
            }

            double multiplier = BurnTimeUtils.isActuallyRainingAt(world, itemEntity.getBlockPos())
                    ? BurnableRegistry.getRainMultiplier(stack.getItem())
                    : 1.0;
            BurnTimeUtils.setCurrentBurnTime(stack, burnTime - (long) Math.ceil(multiplier));
            itemEntity.setStack(stack);
        }
    }

    private static void tickBurnableBlock(World world, BlockPos pos, BlockState state, Block block, Burnable burnable) {
        long burnTime = burnable.getRemainingBurnTime();
        if (burnTime <= 0) {
            Block unlit = BurnableRegistry.getUnlitBlock(state.getBlock());
            if (unlit != null && unlit != state.getBlock()) {
                BlockState newState = copyProperties(state, unlit.getDefaultState());
                world.setBlockState(pos, newState, 3);
                LOGGER.debug("Extinguished {} at {}", unlit, pos);
            }
            return;
        }
        burnable.tickBurn(world, true);
    }

    private static void tickCampfire(World world, BlockPos pos, BlockState state, ICampfireBurnAccessor campfire) {
        long burnTime = campfire.torchesbt_getBurnTime();

        // Only extinguish if burnTime is 0 or less AND the campfire is currently lit
        if (burnTime <= 0 && state.get(CampfireBlock.LIT)) {
            world.setBlockState(pos, state.with(CampfireBlock.LIT, false), 3);
            return;
        }

        // Only tick burn time if the campfire is still lit
        if (burnTime > 0) {
            double multiplier = BurnTimeUtils.isActuallyRainingAt(world, pos)
                    ? BurnableRegistry.getRainMultiplier(Blocks.CAMPFIRE)
                    : 1.0;
            campfire.torchesbt_setBurnTime(burnTime - (long) Math.ceil(multiplier));
            // Sync to client for real-time Jade tooltip updates
            if (!world.isClient) {
                world.updateListeners(pos, state, state, 3);
            }
        }
    }

    public static void setBurnTimeOnPlacement(World world, BlockPos pos, BlockEntity entity, ItemStack stack, long defaultBurnTime) {
        long burnTime = BurnTimeUtils.getCurrentBurnTime(stack);
        if (burnTime <= 0) burnTime = defaultBurnTime;

        if (entity instanceof Burnable burnable) {
            burnable.setRemainingBurnTime(burnTime);
        } else if (entity instanceof ICampfireBurnAccessor campfire) {
            campfire.torchesbt_setBurnTime(burnTime);
        }
    }
}