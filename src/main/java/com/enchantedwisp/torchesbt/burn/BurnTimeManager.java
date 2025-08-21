package com.enchantedwisp.torchesbt.burn;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.blockentity.LanternBlockEntity;
import com.enchantedwisp.torchesbt.registry.RegistryHandler;
import com.enchantedwisp.torchesbt.util.ConfigCache;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;

import java.util.List;

import static com.enchantedwisp.torchesbt.integration.DynamicLightManager.isDynamicLightingModLoaded;

/**
 * Manages burn time ticking and state changes for burnable blocks and items.
 * Uses the Burnable interface for standardized handling.
 */
public class BurnTimeManager {
    private static final Logger LOGGER = RealisticTorchesBT.LOGGER;
    public static final String BURN_TIME_KEY = "remaining_burn";
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
            if (!isBurnableItem(stack)) continue;

            // Split stack if count > 1 to keep one item in hand
            ItemStack tickStack = stack;
            if (stack.getCount() > 1) {
                tickStack = splitAndInitializeStack(stack, 1);
                player.setStackInHand(hand, tickStack);
                ItemStack remainingStack = stack.copy();
                remainingStack.setCount(stack.getCount());
                if (!player.getInventory().insertStack(remainingStack)) {
                    ItemEntity itemEntity = new ItemEntity(world, player.getX(), player.getY(), player.getZ(), remainingStack);
                    world.spawnEntity(itemEntity);
                    LOGGER.debug("Dropped {} (count={}) at {} due to full inventory for player {}", remainingStack.getItem(), remainingStack.getCount(), player.getPos(), player.getName().getString());
                } else {
                    LOGGER.debug("Moved {} (count={}) to inventory for player {}", remainingStack.getItem(), remainingStack.getCount(), player.getName().getString());
                }
            }

            // Handle torches in water (always extinguish)
            if (player.isSubmergedIn(FluidTags.WATER) && tickStack.getItem() == Items.TORCH) {
                replaceBurnableItem(player, hand, tickStack);
                LOGGER.debug("Instantly unlit {} in player {} hand {} due to water submersion", tickStack.getItem(), player.getName().getString(), hand);
                continue;
            }

            // Handle burn time ticking only with dynamic lighting
            if (isDynamicLightingModLoaded()) {
                // Check for water submersion for lanterns
                if (player.isSubmergedIn(FluidTags.WATER) && tickStack.getItem() == Items.LANTERN) {
                    long currentBurnTime = getCurrentBurnTime(tickStack);
                    if (currentBurnTime <= 0) {
                        replaceBurnableItem(player, hand, tickStack);
                        continue;
                    }
                    double multiplier = ConfigCache.getwaterLanternMultiplier();
                    long reduction = (long) Math.ceil(multiplier);
                    setCurrentBurnTime(tickStack, currentBurnTime - reduction);
                    LOGGER.debug("Ticked burn time for submerged lantern in player {} hand {}: {} -> {}", player.getName().getString(), hand, currentBurnTime, currentBurnTime - reduction);
                    continue;
                }

                long currentBurnTime = getCurrentBurnTime(tickStack);
                if (currentBurnTime <= 0) {
                    replaceBurnableItem(player, hand, tickStack);
                    continue;
                }

                double multiplier = isActuallyRainingAt(world, player.getBlockPos()) ? getRainMultiplier(tickStack.getItem()) : 1.0;
                long reduction = (long) Math.ceil(multiplier);
                setCurrentBurnTime(tickStack, currentBurnTime - reduction);

                LOGGER.debug("Ticked burn time for item {} in player {} hand {}: {} -> {}", tickStack.getItem(), player.getName().getString(), hand, currentBurnTime, currentBurnTime - reduction);
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
                if (block == Blocks.TORCH || block == Blocks.WALL_TORCH) {
                    replaceBurnableBlock(world, pos, block);
                    LOGGER.debug("Instantly unlit {} at {} due to water submersion", block, pos);
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
                        LOGGER.debug("Ticked burn time for submerged lantern at {}: {} -> {}", pos, currentBurnTime, currentBurnTime - reduction);
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
        List<ItemEntity> itemEntities = world.getEntitiesByClass(ItemEntity.class, scanBox, entity -> isBurnableItem(entity.getStack()));
        for (ItemEntity itemEntity : itemEntities) {
            ItemStack stack = itemEntity.getStack();
            FluidState fluidState = world.getFluidState(itemEntity.getBlockPos());
            if (fluidState.isIn(FluidTags.WATER)) {
                if (stack.getItem() == Items.TORCH) {
                    ItemStack newStack = new ItemStack(RegistryHandler.UNLIT_TORCH, stack.getCount());
                    itemEntity.setStack(newStack);
                    LOGGER.debug("Instantly unlit dropped {} at {} due to water submersion", stack.getItem(), itemEntity.getBlockPos());
                    continue;
                } else if (stack.getItem() == Items.LANTERN && isDynamicLightingModLoaded()) {
                    long currentBurnTime = getCurrentBurnTime(stack);
                    if (currentBurnTime <= 0) {
                        ItemStack newStack = new ItemStack(RegistryHandler.UNLIT_LANTERN, stack.getCount());
                        itemEntity.setStack(newStack);
                        LOGGER.debug("Replaced dropped {} at {} with {}", stack.getItem(), itemEntity.getBlockPos(), newStack.getItem());
                        continue;
                    }
                    double multiplier = ConfigCache.getwaterLanternMultiplier();
                    long reduction = (long) Math.ceil(multiplier);
                    setCurrentBurnTime(stack, currentBurnTime - reduction);
                    itemEntity.setStack(stack);
                    LOGGER.debug("Ticked burn time for submerged dropped lantern at {}: {} -> {}", itemEntity.getBlockPos(), currentBurnTime, currentBurnTime - reduction);
                    continue;
                }
            }

            // Only tick burn time for dropped items with dynamic lighting
            if (isDynamicLightingModLoaded()) {
                long currentBurnTime = getCurrentBurnTime(stack);
                if (currentBurnTime <= 0) {
                    ItemStack newStack = null;
                    if (stack.getItem() == Items.TORCH) {
                        newStack = new ItemStack(RegistryHandler.UNLIT_TORCH, stack.getCount());
                    } else if (stack.getItem() == Items.LANTERN) {
                        newStack = new ItemStack(RegistryHandler.UNLIT_LANTERN, stack.getCount());
                    }
                    if (newStack != null) {
                        itemEntity.setStack(newStack);
                        LOGGER.debug("Replaced dropped {} at {} with {}", stack.getItem(), itemEntity.getBlockPos(), newStack.getItem());
                    }
                    continue;
                }

                double multiplier = isActuallyRainingAt(world, itemEntity.getBlockPos()) ? getRainMultiplier(stack.getItem()) : 1.0;
                long reduction = (long) Math.ceil(multiplier);
                setCurrentBurnTime(stack, currentBurnTime - reduction);
                itemEntity.setStack(stack);
                LOGGER.debug("Ticked burn time for dropped {} at {}: {} -> {}", stack.getItem(), itemEntity.getBlockPos(), currentBurnTime, currentBurnTime - reduction);
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
        LOGGER.debug("Ticked burn time for block {} at {}: {}", block, pos, burnable.getRemainingBurnTime());
    }

    private static void tickCampfire(World world, BlockPos pos, BlockState state) {
        BlockEntity entity = world.getBlockEntity(pos);
        if (!(entity instanceof CampfireBlockEntity)) return;

        NbtCompound nbt = entity.createNbt();
        long currentBurnTime = nbt.contains(BURN_TIME_KEY) ? nbt.getLong(BURN_TIME_KEY) : ConfigCache.getCampfireBurnTime();

        if (currentBurnTime <= 0) {
            world.setBlockState(pos, state.with(CampfireBlock.LIT, false), 3);
            LOGGER.debug("Extinguished campfire at {}: burn time reached 0", pos);
            return;
        }

        double multiplier = isActuallyRainingAt(world, pos) ? ConfigCache.getRainCampfireMultiplier() : 1.0;
        long reduction = (long) Math.ceil(multiplier);
        nbt.putLong(BURN_TIME_KEY, currentBurnTime - reduction);
        entity.readNbt(nbt);
        entity.markDirty();
        LOGGER.debug("Ticked burn time for campfire at {}: {} -> {}", pos, currentBurnTime, currentBurnTime - reduction);
    }

    private static void replaceBurnableBlock(World world, BlockPos pos, Block block) {
        // Only replace lit blocks
        if (block != Blocks.TORCH && block != Blocks.WALL_TORCH && block != Blocks.LANTERN) {
            LOGGER.debug("Skipped replacement for non-burnable block {} at {}", block, pos);
            return;
        }

        BlockState newState = null;
        if (block == Blocks.TORCH) {
            newState = RegistryHandler.UNLIT_TORCH_BLOCK.getDefaultState();
        } else if (block == Blocks.WALL_TORCH) {
            newState = RegistryHandler.UNLIT_WALL_TORCH_BLOCK.getDefaultState().with(WallTorchBlock.FACING, world.getBlockState(pos).get(WallTorchBlock.FACING));
        } else if (block == Blocks.LANTERN) {
            newState = RegistryHandler.UNLIT_LANTERN_BLOCK.getDefaultState().with(LanternBlock.HANGING, world.getBlockState(pos).get(LanternBlock.HANGING));
        }

        if (newState != null && newState.canPlaceAt(world, pos)) {
            world.setBlockState(pos, newState, 3);
            LOGGER.debug("Replaced {} at {} with unlit variant", block, pos);
        } else {
            LOGGER.warn("Failed to replace {} at {} with unlit variant: newState={}", block, pos, newState == null ? "null" : newState.getBlock());
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
            LOGGER.debug("Replaced {} in player {} hand {} with {}", stack.getItem(), player.getName().getString(), hand, newStack.getItem());
        }
    }

    public static void setBurnTimeOnPlacement(World world, BlockPos pos, BlockEntity entity, ItemStack stack, long defaultBurnTime) {
        long burnTime = stack.hasNbt() && stack.getNbt().contains(BURN_TIME_KEY) ? stack.getNbt().getLong(BURN_TIME_KEY) : defaultBurnTime;
        if (entity instanceof Burnable burnable) {
            burnable.setRemainingBurnTime(burnTime);
        } else if (entity instanceof CampfireBlockEntity campfire) {
            NbtCompound nbt = campfire.createNbt();
            nbt.putLong(BURN_TIME_KEY, burnTime);
            campfire.readNbt(nbt);
            campfire.markDirty();
        }
        LOGGER.debug("Set burn time for block at {} to {}", pos, burnTime);
    }

    // --- Utilities ---
    public static long getMaxBurnTime(ItemStack stack) {
        if (stack.getItem() == Items.TORCH) return ConfigCache.getTorchBurnTime();
        if (stack.getItem() == Items.LANTERN) return ConfigCache.getLanternBurnTime();
        return 0;
    }

    private static double getRainMultiplier(net.minecraft.item.Item item) {
        if (item == Items.TORCH) return ConfigCache.getRainTorchMultiplier();
        if (item == Items.LANTERN) return ConfigCache.getRainLanternMultiplier();
        return 1.0;
    }

    public static long getCurrentBurnTime(ItemStack stack) {
        return stack.hasNbt() && stack.getNbt().contains(BURN_TIME_KEY)
                ? stack.getNbt().getLong(BURN_TIME_KEY)
                : getMaxBurnTime(stack);
    }

    public static void setCurrentBurnTime(ItemStack stack, long burnTime) {
        stack.getOrCreateNbt().putLong(BURN_TIME_KEY, Math.min(burnTime, getMaxBurnTime(stack)));
    }

    public static long getCurrentBurnTime(BlockEntity entity) {
        if (entity instanceof Burnable burnable) return burnable.getRemainingBurnTime();
        if (entity instanceof CampfireBlockEntity campfire) {
            NbtCompound nbt = campfire.createNbt();
            return nbt.contains(BURN_TIME_KEY) ? nbt.getLong(BURN_TIME_KEY) : ConfigCache.getCampfireBurnTime();
        }
        return 0;
    }

    public static void setCurrentBurnTime(BlockEntity entity, long burnTime) {
        if (entity instanceof Burnable burnable) {
            burnable.setRemainingBurnTime(burnTime);
        } else if (entity instanceof CampfireBlockEntity campfire) {
            NbtCompound nbt = campfire.createNbt();
            nbt.putLong(BURN_TIME_KEY, burnTime);
            campfire.readNbt(nbt);
            campfire.markDirty();
        }
    }

    public static void initializeBurnTime(ItemStack stack) {
        setCurrentBurnTime(stack, getMaxBurnTime(stack));
    }

    public static ItemStack splitAndInitializeStack(ItemStack stack, int count) {
        ItemStack newStack = stack.copy();
        newStack.setCount(count);
        long burnTime = getCurrentBurnTime(stack);
        setCurrentBurnTime(newStack, burnTime);
        stack.decrement(count);
        return newStack;
    }

    // --- Centralized rain check ---
    public static boolean isActuallyRainingAt(World world, BlockPos pos) {
        FluidState fluidState = world.getFluidState(pos);
        if (fluidState.isIn(FluidTags.WATER)) return false; // Submersion handled separately
        Biome biome = world.getBiome(pos).value();
        Biome.Precipitation precipitation = biome.getPrecipitation(pos);
        return ConfigCache.isRainExtinguishEnabled() &&
                world.isRaining() &&
                world.isSkyVisible(pos) &&
                (precipitation == Biome.Precipitation.RAIN || precipitation == Biome.Precipitation.SNOW);
    }

    private static boolean isBurnableItem(ItemStack stack) {
        return stack.getItem() == Items.TORCH || stack.getItem() == Items.LANTERN;
    }
}