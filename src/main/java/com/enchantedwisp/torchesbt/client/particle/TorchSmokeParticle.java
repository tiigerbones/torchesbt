package com.enchantedwisp.torchesbt.client.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class TorchSmokeParticle extends SpriteBillboardParticle {

    protected TorchSmokeParticle(ClientWorld world, double x, double y, double z,
                                 double vx, double vy, double vz, SpriteProvider spriteProvider) {
        super(world, x, y, z, vx, vy, vz);

        // Occasional skip: 1 in 2 chance to vanish immediately
        if (world.random.nextInt(2) == 0) {
            this.markDead();
            return;
        }

        // Custom tweaks for sparse/dying smoke
        this.scale = 0.05f;
        this.maxAge = 30 + this.random.nextInt(20); // dies out quicker
        this.gravityStrength = 0.0f;
        this.velocityX *= 0.02;
        this.velocityY *= 0.02;
        this.velocityZ *= 0.02;

        // Randomize grayscale color between dark gray (0.1) and light gray (0.4)
        float grayValue = 0.1F + this.random.nextFloat() * 0.3F;
        this.setColor(grayValue, grayValue, grayValue);

        this.alpha = 0.8f;
        this.setSpriteForAge(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();
        this.alpha = ((float) this.maxAge - this.age) / (float) this.maxAge; // fade over lifetime
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Nullable
        @Override
        public Particle createParticle(DefaultParticleType type, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new TorchSmokeParticle(world, x, y, z, vx, vy, vz, spriteProvider);
        }
    }
}
