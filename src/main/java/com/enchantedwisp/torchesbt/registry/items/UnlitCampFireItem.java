package com.enchantedwisp.torchesbt.registry.items;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;

public class UnlitCampFireItem extends BlockItem {
    public UnlitCampFireItem(Settings settings) {
        super(Blocks.CAMPFIRE, settings); // ties it to vanilla campfire
    }

    @Override
    protected BlockState getPlacementState(ItemPlacementContext context) {
        // Get the normal placement state for a campfire
        BlockState state = super.getPlacementState(context);
        if (state != null) {
            // Force it to be unlit/extinguished
            state = state
                    .with(net.minecraft.block.CampfireBlock.LIT, false)
                    .with(net.minecraft.block.CampfireBlock.SIGNAL_FIRE, false);
        }
        return state;
    }
}
