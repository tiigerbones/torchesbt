package com.enchantedwisp.torchesbt.burn;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.blockentity.LanternBlockEntity;
import com.enchantedwisp.torchesbt.mixinaccess.ICampfireBurnAccessor;
import com.enchantedwisp.torchesbt.registry.RegistryHandler;
import com.enchantedwisp.torchesbt.util.ConfigCache;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.slf4j.Logger;

import java.util.List;

import static com.enchantedwisp.torchesbt.integration.DynamicLightManager.isDynamicLightingModLoaded;

/**
 * Manages burn time ticking and state changes for burnable blocks and items.
 * Uses the Burnable interface for standardized handling.
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
                processPlayerBurnTimes(player);
                processNearbyBurnables(player);
            }
        });

        LOGGER.info("Registered burn time tick handler");
    }

    // --- Player-held items ---
    private static void processPlayerBurnTimes(PlayerEntity player) {
        World world = player.getWorld();
        if (world.isClient) return;

        for (Hand hand : Hand.values()) {
            ItemStack stack = player.getStackInHand(hand);
            if (!BurnTimeUtils.isBurnableItem(stack)) continue;

            ItemStack tickStack = stack;

            // Only split stack if Dynamic Lighting mod is loaded
            if (isDynamicLightingModLoaded() && stack.getCount() > 1) {
                tickStack = BurnTimeUtils.splitAndInitializeStack(stack, 1);
                player.setStackInHand(hand, tickStack);

                ItemStack remainingStack = stack.copy();
                remainingStack.setCount(stack.getCount());
                if (!player.getInventory().insertStack(remainingStack)) {
                    ItemEntity itemEntity = new ItemEntity(world, player.getX(), player.getY(), player.getZ(), remainingStack);
                    world.spawnEntity(itemEntity);
                }
            }

            // Handle torches in water (always extinguish)
            if (player.isSubmergedIn(FluidTags.WATER) && tickStack.getItem() == Items.TORCH) {
                replaceBurnableItem(player, hand, tickStack);
                continue;
            }

            // Handle burn time ticking only with dynamic lighting
            if (isDynamicLightingModLoaded()) {
                // Lanterns in water
                if (player.isSubmergedIn(FluidTags.WATER) && tickStack.getItem() == Items.LANTERN) {
                    long currentBurnTime = BurnTimeUtils.getCurrentBurnTime(tickStack);
                    if (currentBurnTime <= 0) {
                        replaceBurnableItem(player, hand, tickStack);
                        continue;
                    }
                    double multiplier = ConfigCache.getwaterLanternMultiplier();
                    long reduction = (long) Math.ceil(multiplier);
                    BurnTimeUtils.setCurrentBurnTime(tickStack, currentBurnTime - reduction);
                    continue;
                }

                long currentBurnTime = BurnTimeUtils.getCurrentBurnTime(tickStack);
                if (currentBurnTime <= 0) {
                    replaceBurnableItem(player, hand, tickStack);
                    continue;
                }

                double multiplier = BurnTimeUtils.isActuallyRainingAt(world, player.getBlockPos())
                        ? BurnTimeUtils.getRainMultiplier(tickStack.getItem())
                        : 1.0;
                long reduction = (long) Math.ceil(multiplier);
                BurnTimeUtils.setCurrentBurnTime(tickStack, currentBurnTime - reduction);
            }
        }
    }

    // --- Nearby blocks and items ---
    private static void processNearbyBurnables(PlayerEntity player) {
        World world = player.getWorld();
        if (world.isClient) return;

        BlockPos playerPos = player.getBlockPos();
        Box scanBox = new Box(playerPos).expand(SCAN_RADIUS, SCAN_VERTICAL, SCAN_RADIUS);

        // Process blocks
        for (BlockPos pos : BlockPos.iterate(
                new BlockPos((int) scanBox.minX, (int) scanBox.minY, (int) scanBox.minZ),
                new BlockPos((int) scanBox.maxX, (int) scanBox.maxY, (int) scanBox.maxZ))) {
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            // Skip unlit blocks to prevent unnecessary processing
            if (block == RegistryHandler.UNLIT_TORCH_BLOCK ||
                    block == RegistryHandler.UNLIT_WALL_TORCH_BLOCK ||
                    block == RegistryHandler.UNLIT_LANTERN_BLOCK) {
                continue;
            }

            // Check for water submersion
            FluidState fluidState = world.getFluidState(pos);
            if (fluidState.isIn(FluidTags.WATER)) {
                if (block == Blocks.TORCH || block == Blocks.WALL_TORCH || block == Blocks.CAMPFIRE) {
                    replaceBurnableBlock(world, pos, block);
                    continue;
                } else if (block == Blocks.LANTERN) {
                    BlockEntity entity = world.getBlockEntity(pos);
                    if (entity instanceof LanternBlockEntity burnable) {
                        long currentBurnTime = burnable.getRemainingBurnTime();
                        if (currentBurnTime <= 0) {
                            replaceBurnableBlock(world, pos, block);
                            continue;
                        }
                        double multiplier = ConfigCache.getwaterLanternMultiplier();
                        long reduction = (long) Math.ceil(multiplier);
                        burnable.setRemainingBurnTime(currentBurnTime - reduction);
                        entity.markDirty();
                    }
                    continue;
                }
            }

            BlockEntity entity = world.getBlockEntity(pos);

            if (entity instanceof Burnable burnable) {
                tickBurnableBlock(world, pos, state, block, burnable);
            } else if (block == Blocks.CAMPFIRE && state.get(CampfireBlock.LIT)) {
                tickCampfire(world, pos, state);
            }
        }

        // Process dropped items
        List<ItemEntity> itemEntities = world.getEntitiesByClass(ItemEntity.class, scanBox, entity -> BurnTimeUtils.isBurnableItem(entity.getStack()));
        for (ItemEntity itemEntity : itemEntities) {
            ItemStack stack = itemEntity.getStack();
            FluidState fluidState = world.getFluidState(itemEntity.getBlockPos());
            if (fluidState.isIn(FluidTags.WATER)) {
                if (stack.getItem() == Items.TORCH) {
                    ItemStack newStack = new ItemStack(RegistryHandler.UNLIT_TORCH, stack.getCount());
                    itemEntity.setStack(newStack);
                    continue;
                } else if (stack.getItem() == Items.LANTERN && isDynamicLightingModLoaded()) {
                    long currentBurnTime = BurnTimeUtils.getCurrentBurnTime(stack);
                    if (currentBurnTime <= 0) {
                        ItemStack newStack = new ItemStack(RegistryHandler.UNLIT_LANTERN, stack.getCount());
                        itemEntity.setStack(newStack);
                        continue;
                    }
                    double multiplier = ConfigCache.getwaterLanternMultiplier();
                    long reduction = (long) Math.ceil(multiplier);
                    BurnTimeUtils.setCurrentBurnTime(stack, currentBurnTime - reduction);
                    itemEntity.setStack(stack);
                    continue;
                }
            }

            // Only tick burn time for dropped items with dynamic lighting
            if (isDynamicLightingModLoaded()) {
                long currentBurnTime = BurnTimeUtils.getCurrentBurnTime(stack);
                if (currentBurnTime <= 0) {
                    ItemStack newStack = null;
                    if (stack.getItem() == Items.TORCH) {
                        newStack = new ItemStack(RegistryHandler.UNLIT_TORCH, stack.getCount());
                    } else if (stack.getItem() == Items.LANTERN) {
                        newStack = new ItemStack(RegistryHandler.UNLIT_LANTERN, stack.getCount());
                    }
                    if (newStack != null) {
                        itemEntity.setStack(newStack);
                    }
                    continue;
                }

                double multiplier = BurnTimeUtils.isActuallyRainingAt(world, itemEntity.getBlockPos()) ? BurnTimeUtils.getRainMultiplier(stack.getItem()) : 1.0;
                long reduction = (long) Math.ceil(multiplier);
                BurnTimeUtils.setCurrentBurnTime(stack, currentBurnTime - reduction);
                itemEntity.setStack(stack);
            }
        }
    }

    private static void tickBurnableBlock(World world, BlockPos pos, BlockState state, Block block, Burnable burnable) {
        // Only process lit blocks
        if (block != Blocks.TORCH && block != Blocks.WALL_TORCH && block != Blocks.LANTERN) {
            return;
        }

        long currentBurnTime = burnable.getRemainingBurnTime();
        if (currentBurnTime <= 0) {
            replaceBurnableBlock(world, pos, block);
            return;
        }

        burnable.tickBurn(world, true);
    }

    private static void tickCampfire(World world, BlockPos pos, BlockState state) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (!(entity instanceof ICampfireBurnAccessor accessor)) return;

        long currentBurnTime = accessor.torchesbt_getBurnTime();

        if (currentBurnTime <= 0) {
            // Eject items being cooked
            for (ItemStack stack : accessor.torchesbt_getItems()) {
                if (!stack.isEmpty()) {
                    ItemEntity drop = new ItemEntity(
                            world,
                            pos.getX() + 0.5,
                            pos.getY() + 1.0,
                            pos.getZ() + 0.5,
                            stack.copy()
                    );
                    world.spawnEntity(drop);
                }
            }
            accessor.torchesbt_getItems().clear();

            // Extinguish campfire
            world.setBlockState(pos, state.with(CampfireBlock.LIT, false), 3);
            world.playSound(null, pos,
                    SoundEvents.ENTITY_GENERIC_EXTINGUISH_FIRE,
                    SoundCategory.BLOCKS,
                    1.0F, 1.0F);
            return;
        }

        // Tick down
        double multiplier = BurnTimeUtils.isActuallyRainingAt(world, pos)
                ? ConfigCache.getRainCampfireMultiplier()
                : 1.0;
        long reduction = (long) Math.ceil(multiplier);
        accessor.torchesbt_setBurnTime(currentBurnTime - reduction);
    }

    private static void replaceBurnableBlock(World world, BlockPos pos, Block block) {
        // Only replace lit blocks
        if (block != Blocks.TORCH && block != Blocks.WALL_TORCH && block != Blocks.LANTERN) {
            return;
        }

        BlockState newState = null;
        if (block == Blocks.TORCH) {
            newState = RegistryHandler.UNLIT_TORCH_BLOCK.getDefaultState();
        } else if (block == Blocks.WALL_TORCH) {
            newState = RegistryHandler.UNLIT_WALL_TORCH_BLOCK.getDefaultState().with(WallTorchBlock.FACING, world.getBlockState(pos).get(WallTorchBlock.FACING));
        } else {
            newState = RegistryHandler.UNLIT_LANTERN_BLOCK.getDefaultState().with(LanternBlock.HANGING, world.getBlockState(pos).get(LanternBlock.HANGING));
        }

        if (newState != null && newState.canPlaceAt(world, pos)) {
            world.setBlockState(pos, newState, 3);
        }
    }

    private static void replaceBurnableItem(PlayerEntity player, Hand hand, ItemStack stack) {
        ItemStack newStack = null;
        if (stack.getItem() == Items.TORCH) {
            newStack = new ItemStack(RegistryHandler.UNLIT_TORCH, 1);
        } else if (stack.getItem() == Items.LANTERN) {
            newStack = new ItemStack(RegistryHandler.UNLIT_LANTERN, 1);
        }

        if (newStack != null) {
            player.setStackInHand(hand, newStack);
        }
    }

    public static void setBurnTimeOnPlacement(World world, BlockPos pos, BlockEntity entity, ItemStack stack, long defaultBurnTime) {
        long burnTime = stack.hasNbt() && stack.getNbt() != null && stack.getNbt().contains(BurnTimeUtils.BURN_TIME_KEY)
                ? stack.getNbt().getLong(BurnTimeUtils.BURN_TIME_KEY)
                : defaultBurnTime;
        if (entity instanceof Burnable burnable) {
            burnable.setRemainingBurnTime(burnTime);
        } else if (entity instanceof ICampfireBurnAccessor accessor) {
            accessor.torchesbt_setBurnTime(burnTime);
        }
    }
}