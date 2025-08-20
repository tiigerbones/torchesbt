package com.enchantedwisp.torchesbt.burn;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.blockentity.LanternBlockEntity;
import com.enchantedwisp.torchesbt.blockentity.TorchBlockEntity;
import com.enchantedwisp.torchesbt.registry.RegistryHandler;
import com.enchantedwisp.torchesbt.util.ConfigCache;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.slf4j.Logger;

import static com.enchantedwisp.torchesbt.integration.DynamicLightManager.isDynamicLightingModLoaded;

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
                if (isDynamicLightingModLoaded()) processPlayerBurnTimes(player);
                processNearbyBurnables(player);
            }
        });

        LOGGER.info("Registered burn time tick handler");
    }

    // --- Player-held items ---
    private static void processPlayerBurnTimes(PlayerEntity player) {
        if (!isDynamicLightingModLoaded()) return;

        World world = player.getWorld();
        if (world.isClient) return;

        for (Hand hand : Hand.values()) {
            ItemStack stack = player.getStackInHand(hand);
            if (!isBurnableItem(stack)) continue;

            if (stack.hasNbt() && stack.getNbt().contains(BURN_TIME_KEY)) {
                processBurnableItem(stack, world, player, hand, player.getBlockPos());
            } else {
                splitAndInitializeStack(player, hand, stack);
            }
        }
    }

    // Split stack to initialize one item with burn-time
    private static void splitAndInitializeStack(PlayerEntity player, Hand hand, ItemStack stack) {
        if (stack.getCount() > 1) {
            ItemStack burningItem = stack.copyWithCount(1);
            initializeBurnTime(burningItem);
            stack.decrement(1);
            player.setStackInHand(hand, burningItem);

            if (!player.getInventory().insertStack(stack)) {
                player.dropItem(stack, false);
            }
        } else {
            initializeBurnTime(stack);
            player.setStackInHand(hand, stack);
        }
    }

    // --- Nearby blocks + dropped items ---
    private static void processNearbyBurnables(PlayerEntity player) {
        World world = player.getWorld();
        if (world.isClient) return;

        Box box = new Box(player.getBlockPos()).expand(SCAN_RADIUS, SCAN_VERTICAL, SCAN_RADIUS);

        for (BlockPos pos : BlockPos.iterate(
                (int) box.minX, (int) box.minY, (int) box.minZ,
                (int) box.maxX, (int) box.maxY, (int) box.maxZ)) {

            BlockState state = world.getBlockState(pos);
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof TorchBlockEntity torch) {
                processTorchBurnTime(torch, world, pos, state);
            } else if (blockEntity instanceof LanternBlockEntity lantern) {
                processLanternBurnTime(lantern, world, pos, state);
            } else if (blockEntity instanceof CampfireBlockEntity campfire && state.get(CampfireBlock.LIT)) {
                processCampfireBurnTime(campfire, world, pos, state);
            }
        }

        // --- Process dropped items ---
        if (isDynamicLightingModLoaded()) {
            for (ItemEntity itemEntity : world.getEntitiesByClass(ItemEntity.class, box,
                    e -> isBurnableItem(e.getStack()))) {

                ItemStack stack = itemEntity.getStack();

                if (stack.hasNbt() && stack.getNbt().contains(BURN_TIME_KEY)) {
                    // Tick the burnable item
                    processBurnableItem(stack, world, null, null, itemEntity.getBlockPos());

                    // Convert to unlit if burn time reached 0
                    if (getCurrentBurnTime(stack) <= 0) {
                        ItemStack newStack = (stack.getItem() == Items.TORCH ? RegistryHandler.UNLIT_TORCH : RegistryHandler.UNLIT_LANTERN).getDefaultStack();
                        newStack.setCount(stack.getCount());
                        itemEntity.setStack(newStack);
                    } else if (getCurrentBurnTime(stack) != stack.getNbt().getLong(BURN_TIME_KEY)) {
                        itemEntity.setStack(stack);
                    }
                } else {
                    // Stack has no burn-time yet, split to initialize
                    if (stack.getCount() > 1) {
                        ItemStack burningItem = stack.copyWithCount(1);
                        initializeBurnTime(burningItem);
                        stack.decrement(1);

                        itemEntity.setStack(burningItem);

                        // Drop remaining stack as a new entity
                        ItemEntity remainder = new ItemEntity(world, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), stack);
                        world.spawnEntity(remainder);
                    } else {
                        // Only one item in stack: initialize burn time
                        initializeBurnTime(stack);
                        itemEntity.setStack(stack);
                    }
                }
            }
        }
    }

    // --- Burnable checks ---
    private static boolean isBurnableItem(ItemStack stack) {
        return stack.getItem() == Items.TORCH || stack.getItem() == Items.LANTERN;
    }

    // --- Item ticking ---
    private static void processBurnableItem(ItemStack stack, World world, PlayerEntity player, Hand hand, BlockPos pos) {
        if (!isBurnableItem(stack)) return;

        long max = getMaxBurnTime(stack);
        long current = getCurrentBurnTime(stack);

        if (current > max) current = max;

        if (current > 0) {
            long decrement = 1;

            BlockPos checkPos = pos != null ? pos : (player != null ? player.getBlockPos() : null);

            if (checkPos != null && isActuallyRainingAt(world, checkPos)) {
                if (stack.getItem() == Items.TORCH) decrement = (long) Math.ceil(ConfigCache.getRainTorchMultiplier());
                if (stack.getItem() == Items.LANTERN) decrement = (long) Math.ceil(ConfigCache.getRainLanternMultiplier());
            }

            long newBurn = Math.max(current - decrement, 0);
            if (newBurn != current) {
                stack.getOrCreateNbt().putLong(BURN_TIME_KEY, newBurn);
            }
        }

        if (current <= 0) {
            ItemStack newStack = (stack.getItem() == Items.TORCH ? RegistryHandler.UNLIT_TORCH : RegistryHandler.UNLIT_LANTERN).getDefaultStack();
            newStack.setCount(stack.getCount());

            if (player != null && hand != null) {
                player.setStackInHand(hand, newStack);
            }
        }
    }

    // --- Block ticking ---
    private static void processTorchBurnTime(TorchBlockEntity torch, World world, BlockPos pos, BlockState state) {
        long max = torch.getMaxBurnTime();
        long current = torch.getRemainingBurnTime();

        if (current > max) torch.setRemainingBurnTime(max);

        long decrement = isActuallyRainingAt(world, pos) ? (long) Math.ceil(ConfigCache.getRainTorchMultiplier()) : 1;

        long newBurn = Math.max(current - decrement, 0);
        if (newBurn != current) {
            torch.setRemainingBurnTime(newBurn);
            if (newBurn <= 0) BurnableLightUtil.convertToUnlit(world, pos, state);
        }
    }

    private static void processLanternBurnTime(LanternBlockEntity lantern, World world, BlockPos pos, BlockState state) {
        long max = lantern.getMaxBurnTime();
        long current = lantern.getRemainingBurnTime();

        if (current > max) lantern.setRemainingBurnTime(max);

        long decrement = isActuallyRainingAt(world, pos) ? (long) Math.ceil(ConfigCache.getRainLanternMultiplier()) : 1;

        long newBurn = Math.max(current - decrement, 0);
        if (newBurn != current) {
            lantern.setRemainingBurnTime(newBurn);
            if (newBurn <= 0) BurnableLightUtil.convertToUnlit(world, pos, state);
        }
    }

    private static void processCampfireBurnTime(CampfireBlockEntity campfire, World world, BlockPos pos, BlockState state) {
        NbtCompound nbt = campfire.createNbt();
        long max = ConfigCache.getCampfireBurnTime();
        long current = nbt.contains(BURN_TIME_KEY) ? nbt.getLong(BURN_TIME_KEY) : max;

        if (current > max) current = max;

        long decrement = isActuallyRainingAt(world, pos) ? (long) Math.ceil(ConfigCache.getRainCampfireMultiplier()) : 1;

        long newBurn = Math.max(current - decrement, 0);
        if (newBurn != current) {
            nbt.putLong(BURN_TIME_KEY, newBurn);
            campfire.readNbt(nbt);
            campfire.markDirty();
        }

        if (newBurn <= 0) {
            world.setBlockState(pos, state.with(CampfireBlock.LIT, false), Block.NOTIFY_ALL);
            nbt.remove(BURN_TIME_KEY);
            campfire.readNbt(nbt);
            campfire.markDirty();
        }
    }

    // --- Utilities ---
    public static long getMaxBurnTime(ItemStack stack) {
        if (stack.getItem() == Items.TORCH) return ConfigCache.getTorchBurnTime();
        if (stack.getItem() == Items.LANTERN) return ConfigCache.getLanternBurnTime();
        return 0;
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
        if (entity instanceof TorchBlockEntity torch) return torch.getRemainingBurnTime();
        if (entity instanceof LanternBlockEntity lantern) return lantern.getRemainingBurnTime();
        if (entity instanceof CampfireBlockEntity campfire) {
            NbtCompound nbt = campfire.createNbt();
            return nbt.contains(BURN_TIME_KEY) ? nbt.getLong(BURN_TIME_KEY) : ConfigCache.getCampfireBurnTime();
        }
        return 0;
    }

    public static void setCurrentBurnTime(BlockEntity entity, long burnTime) {
        if (entity instanceof TorchBlockEntity torch) {
            torch.setRemainingBurnTime(burnTime);
        } else if (entity instanceof LanternBlockEntity lantern) {
            lantern.setRemainingBurnTime(burnTime);
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

    // --- Centralized rain check ---
    public static boolean isActuallyRainingAt(World world, BlockPos pos) {
        return ConfigCache.isRainExtinguishEnabled() && world.isRaining() && world.isSkyVisible(pos) && world.hasRain(pos);
    }
}
