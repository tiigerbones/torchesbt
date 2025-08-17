package com.enchantedwisp.torchesbt.registry.blocks;

import com.enchantedwisp.torchesbt.registry.Particles;
import net.minecraft.block.Blocks;
import net.minecraft.block.TorchBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.BlockSoundGroup;

public class UnlitTorchBlock extends TorchBlock {
    public UnlitTorchBlock() {
        super(Settings.copy(Blocks.TORCH)
                        .luminance(state -> 0) // No light emission
                        .sounds(BlockSoundGroup.WOOD),
                Particles.TORCH_SMOKE
        );
    }



    @Override
    public String getTranslationKey() {
        return "block.torchesbt.unlit_torch";
    }
}
