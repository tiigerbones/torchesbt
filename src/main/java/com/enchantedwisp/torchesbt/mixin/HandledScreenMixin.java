package com.enchantedwisp.torchesbt.mixin;

import com.enchantedwisp.torchesbt.api.FuelTypeAPI;
import com.enchantedwisp.torchesbt.network.ItemFuelPacket;
import com.enchantedwisp.torchesbt.core.BurnableRegistry;
import com.enchantedwisp.torchesbt.util.ConfigCache;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to detect left-clicks on screen handler slots containing burnable items
 * when the cursor holds a valid fuel item, sending a refueling packet to the server.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> implements ScreenHandlerProvider<T> {

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V",
            at = @At("HEAD"), cancellable = true)
    private void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType, CallbackInfo ci) {
        // Check if inventory refueling is enabled
        if (!ConfigCache.allowInventoryRefueling()) {
            return; // Do nothing if refueling is disabled
        }

        if (slot == null || !slot.hasStack() || actionType != SlotActionType.PICKUP || button != 0) {
            return; // Only handle left-clicks (button 0) with PICKUP action
        }

        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        ItemStack cursorStack = screen.getScreenHandler().getCursorStack();
        if (cursorStack.isEmpty()) {
            return;
        }

        // Check if cursor holds a valid fuel item
        Identifier fuelItemId = Registries.ITEM.getId(cursorStack.getItem());
        boolean isValidFuel = false;
        for (Identifier fuelTypeId : FuelTypeAPI.getFuelTypeIds()) {
            FuelTypeAPI.FuelType fuelType = FuelTypeAPI.getFuelType(fuelTypeId);
            if (fuelType != null && fuelType.getFuelMap().containsKey(fuelItemId)) {
                isValidFuel = true;
                break;
            }
        }
        if (!isValidFuel) {
            return;
        }

        // Check if the slot contains a burnable item
        ItemStack targetStack = slot.getStack();
        if (!BurnableRegistry.isBurnableItem(targetStack.getItem())) {
            return;
        }

        // Use the handler slot ID (works for inventory, trinkets, etc.)
        int handlerSlotId = slot.id;

        // Send packet to server
        ItemFuelPacket packet = new ItemFuelPacket(handlerSlotId, cursorStack);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        packet.write(buf);

        ClientPlayNetworking.send(ItemFuelPacket.ID, buf);
        ci.cancel(); // Prevent default pickup behavior
    }
}