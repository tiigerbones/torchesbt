package com.enchantedwisp.torchesbt.blockentity;

import com.enchantedwisp.torchesbt.util.ConfigCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

public class TorchBlockEntity extends BlockEntity {
    private static final String REMAINING_KEY = "remaining_burn";
    private long remainingBurnTime; // only store how much time is left

    public TorchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TORCH_BLOCK_ENTITY, pos, state);
        this.remainingBurnTime = ConfigCache.getTorchBurnTime(); // initialize with full time
    }

    /** Current remaining burn time */
    public long getRemainingBurnTime() {
        return remainingBurnTime;
    }

    /** Max burn time always comes from config */
    public long getMaxBurnTime() {
        return ConfigCache.getTorchBurnTime();
    }

    /** Set remaining burn time */
    public void setRemainingBurnTime(long time) {
        this.remainingBurnTime = Math.max(0, time);
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.remainingBurnTime = nbt.getLong(REMAINING_KEY);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong(REMAINING_KEY, this.remainingBurnTime);
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
