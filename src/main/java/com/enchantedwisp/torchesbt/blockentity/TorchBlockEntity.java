package com.enchantedwisp.torchesbt.blockentity;

import com.enchantedwisp.torchesbt.burn.BurnTimeManager;
import com.enchantedwisp.torchesbt.burn.BurnTimeUtils;
import com.enchantedwisp.torchesbt.burn.Burnable;
import com.enchantedwisp.torchesbt.util.ConfigCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Block entity for torches and wall torches, tracking their burn time.
 * Implements Burnable for standardized burn time management.
 */
public class TorchBlockEntity extends BlockEntity implements Burnable {
    private static final String REMAINING_KEY = "remaining_burn";
    private long remainingBurnTime;

    public TorchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TORCH_BLOCK_ENTITY, pos, state);
        this.remainingBurnTime = getMaxBurnTime();
    }

    @Override
    public long getMaxBurnTime() {
        return ConfigCache.getTorchBurnTime();
    }

    @Override
    public long getRemainingBurnTime() {
        return remainingBurnTime;
    }

    @Override
    public void setRemainingBurnTime(long time) {
        this.remainingBurnTime = Math.max(0, Math.min(time, getMaxBurnTime()));
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    public void tickBurn(World world, boolean isBlock) {
        if (remainingBurnTime <= 0) return;

        double multiplier = isBlock && BurnTimeUtils.isActuallyRainingAt(world, pos) ? getRainMultiplier() : 1.0;
        long reduction = (long) Math.ceil(multiplier);
        setRemainingBurnTime(remainingBurnTime - reduction);
    }

    @Override
    public double getRainMultiplier() {
        return ConfigCache.getRainTorchMultiplier();
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        remainingBurnTime = nbt.getLong(REMAINING_KEY);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong(REMAINING_KEY, remainingBurnTime);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}