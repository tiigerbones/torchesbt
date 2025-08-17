package com.enchantedwisp.torchesbt.registry;

import com.enchantedwisp.torchesbt.RealisticTorchesBT;
import com.enchantedwisp.torchesbt.client.particle.CandleSmokeParticle;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class Particles {
    public static final DefaultParticleType CANDLE_SMOKE = FabricParticleTypes.simple(false);
    public static final DefaultParticleType TORCH_SMOKE = FabricParticleTypes.simple(false);

    public static void register() {
        Registry.register(
                Registries.PARTICLE_TYPE,
                new Identifier(RealisticTorchesBT.MOD_ID, "candle_smoke"),
                CANDLE_SMOKE
        );

        Registry.register(
                Registries.PARTICLE_TYPE,
                new Identifier(RealisticTorchesBT.MOD_ID, "torch_smoke"),
                TORCH_SMOKE
        );
    }

    public static void registerClient() {
        ParticleFactoryRegistry.getInstance().register(CANDLE_SMOKE, CandleSmokeParticle.Factory::new);
    }
}
