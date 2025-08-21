package com.enchantedwisp.torchesbt.mixin;

import com.enchantedwisp.torchesbt.burn.BurnTimeManager;
import com.enchantedwisp.torchesbt.mixinaccess.ICampfireBurnAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.CampfireBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        nbt.putLong(BurnTimeManager.BURN_TIME_KEY, torchesbt_burnTime);
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void torchesbt_readNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains(BurnTimeManager.BURN_TIME_KEY)) {
            torchesbt_burnTime = nbt.getLong(BurnTimeManager.BURN_TIME_KEY);
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
}

