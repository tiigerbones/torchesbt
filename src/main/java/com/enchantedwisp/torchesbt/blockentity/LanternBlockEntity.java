package com.enchantedwisp.torchesbt.blockentity;

import com.enchantedwisp.torchesbt.util.ConfigCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

public class LanternBlockEntity extends BlockEntity {
    private static final String REMAINING_KEY = "remaining_burn";
    private long remainingBurnTime; // only remaining, not max

    public LanternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LANTERN_BLOCK_ENTITY, pos, state);
        this.remainingBurnTime = getMaxBurnTime(); // initialize full
    }

    /** Returns the max burn time from config (dynamic). */
    public long getMaxBurnTime() {
        return ConfigCache.getLanternBurnTime();
    }

    /** Returns remaining burn time. */
    public long getRemainingBurnTime() {
        return remainingBurnTime;
    }

    /** Set remaining burn time (clamped). */
    public void setRemainingBurnTime(long time) {
        this.remainingBurnTime = Math.max(0, Math.min(time, getMaxBurnTime()));
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    /** Reduce remaining time by 1 tick. */
    public void tickBurn() {
        if (remainingBurnTime > 0) {
            setRemainingBurnTime(remainingBurnTime - 1);
        }
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
