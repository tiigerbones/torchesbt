package com.enchantedwisp.torchesbt.util;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.blockentity.LanternBlockEntity;
import com.enchantedwisp.torchesbt.blockentity.TorchBlockEntity;
import com.enchantedwisp.torchesbt.registry.RegistryHandler;
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

import static com.enchantedwisp.torchesbt.util.DynamicLightManager.isDynamicLightingModLoaded;

public class BurnTimeManager {
    private static final Logger LOGGER = RealisticTorchesBT.LOGGER;
    public static final String BURN_TIME_KEY = "remaining_burn";
    private static final int ITEM_UPDATE_INTERVAL = 10; // every 0.5s
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
        World world = player.getWorld();
        if (world.isClient) return;

        for (Hand hand : Hand.values()) {
            ItemStack stack = player.getStackInHand(hand);
            if (isBurnableItem(stack)) processBurnableItem(stack, world, player, hand);
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

        if (isDynamicLightingModLoaded()) {
            for (ItemEntity itemEntity : world.getEntitiesByClass(ItemEntity.class, box,
                    e -> isBurnableItem(e.getStack()))) {

                ItemStack stack = itemEntity.getStack();
                long before = getCurrentBurnTime(stack);

                processBurnableItem(stack, world, null, null);

                if (before > 0 && getCurrentBurnTime(stack) == 0) {
                    ItemStack newStack = (stack.getItem() == Items.TORCH ? RegistryHandler.UNLIT_TORCH : RegistryHandler.UNLIT_LANTERN).getDefaultStack();
                    newStack.setCount(stack.getCount());
                    itemEntity.setStack(newStack);
                } else {
                    itemEntity.setStack(stack);
                }
            }
        }
    }

    // --- Burnable checks ---
    private static boolean isBurnableItem(ItemStack stack) {
        return stack.getItem() == Items.TORCH || stack.getItem() == Items.LANTERN;
    }

    // --- Item ticking ---
    private static void processBurnableItem(ItemStack stack, World world, PlayerEntity player, Hand hand) {
        if (!isBurnableItem(stack)) return;

        long max = getMaxBurnTime(stack);
        long current = getCurrentBurnTime(stack);

        // Clamp if config decreased
        if (current > max) current = max;

        if (current > 0) {
            current--;
            stack.getOrCreateNbt().putLong(BURN_TIME_KEY, current);
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

        if (ConfigCache.isRainExtinguishEnabled() && world.isRaining() && world.isSkyVisible(pos)) {
            if (current > 0) torch.setRemainingBurnTime(current - (long) Math.ceil(ConfigCache.getRainTorchMultiplier()));
            if (torch.getRemainingBurnTime() <= 0) BurnableLightUtil.convertToUnlit(world, pos, state);
        } else {
            if (current > 0) torch.setRemainingBurnTime(current - 1);
            if (torch.getRemainingBurnTime() <= 0) BurnableLightUtil.convertToUnlit(world, pos, state);
        }
    }

    private static void processLanternBurnTime(LanternBlockEntity lantern, World world, BlockPos pos, BlockState state) {
        long max = lantern.getMaxBurnTime();
        long current = lantern.getRemainingBurnTime();

        if (current > max) lantern.setRemainingBurnTime(max);

        if (ConfigCache.isRainExtinguishEnabled() && world.isRaining() && world.isSkyVisible(pos)) {
            if (current > 0) lantern.setRemainingBurnTime(current - (long) Math.ceil(ConfigCache.getRainLanternMultiplier()));
            if (lantern.getRemainingBurnTime() <= 0) BurnableLightUtil.convertToUnlit(world, pos, state);
        } else {
            if (current > 0) lantern.setRemainingBurnTime(current - 1);
            if (lantern.getRemainingBurnTime() <= 0) BurnableLightUtil.convertToUnlit(world, pos, state);
        }
    }

    private static void processCampfireBurnTime(CampfireBlockEntity campfire, World world, BlockPos pos, BlockState state) {
        NbtCompound nbt = campfire.createNbt();
        long max = ConfigCache.getCampfireBurnTime();
        long current = nbt.contains(BURN_TIME_KEY) ? nbt.getLong(BURN_TIME_KEY) : max;

        if (current > max) current = max;
        nbt.putLong(BURN_TIME_KEY, current);
        campfire.readNbt(nbt);
        campfire.markDirty();

        if (current > 0) {
            long decrement = (ConfigCache.isRainExtinguishEnabled() && world.isRaining() && world.isSkyVisible(pos))
                    ? (long) Math.ceil(ConfigCache.getRainCampfireMultiplier()) : 1;
            current -= decrement;
            nbt.putLong(BURN_TIME_KEY, current);
            campfire.readNbt(nbt);
            campfire.markDirty();
        }

        if (current <= 0) {
            world.setBlockState(pos, state.with(CampfireBlock.LIT, false), Block.NOTIFY_ALL);
            nbt.remove(BURN_TIME_KEY);
            campfire.readNbt(nbt);
            campfire.markDirty();
        }
    }

    // --- Utilities for ItemStacks ---
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

    // --- Utilities for BlockEntities ---
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

    /** Initialize a new ItemStack’s burn time to max */
    public static void initializeBurnTime(ItemStack stack) {
        setCurrentBurnTime(stack, getMaxBurnTime(stack));
    }
}
