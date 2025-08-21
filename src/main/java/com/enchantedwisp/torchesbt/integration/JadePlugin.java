package com.enchantedwisp.torchesbt.integration;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.burn.Burnable;
import com.enchantedwisp.torchesbt.burn.BurnTimeManager;
import com.enchantedwisp.torchesbt.util.ConfigCache;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

/**
 * Integration with Jade to display burn time tooltips for burnable blocks.
 */
public class JadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new IBlockComponentProvider() {

            @Override
            public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                // Only show for burnable blocks
                if (accessor.getBlock() == Blocks.TORCH ||
                        accessor.getBlock() == Blocks.WALL_TORCH ||
                        accessor.getBlock() == Blocks.LANTERN ||
                        (accessor.getBlock() == Blocks.CAMPFIRE &&
                                accessor.getBlockState().get(net.minecraft.block.CampfireBlock.LIT))) {

                    long burnTime = BurnTimeManager.getCurrentBurnTime(accessor.getBlockEntity());
                    long maxBurnTime = getMaxBurnTime(accessor.getBlock(), accessor.getBlockEntity());

                    if (burnTime > 0 && maxBurnTime > 0) {
                        tooltip.add(Text.literal("Burn Time: " + (burnTime / 20) + "/" + (maxBurnTime / 20)));
                    }
                }
            }

            private long getMaxBurnTime(net.minecraft.block.Block block, BlockEntity entity) {
                if (entity instanceof Burnable burnable) {
                    return burnable.getMaxBurnTime();
                } else if (block == Blocks.CAMPFIRE) {
                    return ConfigCache.getCampfireBurnTime();
                }
                return 0;
            }

            @Override
            public Identifier getUid() {
                return new Identifier(RealisticTorchesBT.MOD_ID, "burn_time");
            }

        }, net.minecraft.block.Block.class);
    }
}