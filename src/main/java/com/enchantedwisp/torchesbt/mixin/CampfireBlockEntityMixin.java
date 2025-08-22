package com.enchantedwisp.torchesbt.mixin;

import com.enchantedwisp.torchesbt.burn.BurnTimeUtils;
import com.enchantedwisp.torchesbt.mixinaccess.ICampfireBurnAccessor;
import com.enchantedwisp.torchesbt.util.ConfigCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Adds custom burn time tracking to vanilla CampfireBlockEntity.
 */
@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin extends BlockEntity implements ICampfireBurnAccessor {
    @Unique
    private long torchesbt_burnTime = 0;

    public CampfireBlockEntityMixin(BlockPos pos, BlockState state) {
        super(BlockEntityType.CAMPFIRE, pos, state);
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void torchesbt_writeNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putLong(BurnTimeUtils.BURN_TIME_KEY, torchesbt_burnTime);
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void torchesbt_readNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(BurnTimeUtils.BURN_TIME_KEY)) {
            torchesbt_burnTime = nbt.getLong(BurnTimeUtils.BURN_TIME_KEY);
        } else if (getCachedState().get(net.minecraft.block.CampfireBlock.LIT)) {
            // Initialize burn time for lit campfires on load if NBT doesn't have it
            torchesbt_burnTime = ConfigCache.getCampfireBurnTime();
            markDirty();
        }
    }

    @Override
    public long torchesbt_getBurnTime() {
        return torchesbt_burnTime;
    }

    @Override
    public void torchesbt_setBurnTime(long time) {
        this.torchesbt_burnTime = Math.max(0, time);
        markDirty();
    }

    @Override
    public DefaultedList<ItemStack> torchesbt_getItems() {
        return ((CampfireBlockEntity) (Object) this).getItemsBeingCooked();
    }

    /**
     * Overwrites to ensure burn time is synced to the client.
     * @reason Ensure custom burn time is included in update packets.
     * @author tiigerbones
     */
    @Overwrite
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    /**
     * Overwrites to ensure burn time is synced on chunk load.
     * @reason Ensure custom burn time is included in initial chunk data.
     * @author tiigerbones
     */
    @Overwrite
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}