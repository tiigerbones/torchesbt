package com.enchantedwisp.torchesbt.client.particle;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import org.jetbrains.annotations.Nullable;

public class CandleSmokeParticle extends SpriteBillboardParticle {
    protected CandleSmokeParticle(ClientWorld world, double x, double y, double z,
                                  double vx, double vy, double vz, SpriteProvider spriteProvider) {
        super(world, x, y, z, vx, vy, vz);

        // Custom tweaks for sparse/dying smoke
        this.scale = 0.03f; // smaller than vanilla smoke
        this.maxAge = 30 + this.random.nextInt(20);
        this.gravityStrength = 0.0f;
        this.velocityX *= 0.02;
        this.velocityY *= 0.02;
        this.velocityZ *= 0.02;
        // Randomize grayscale color between dark gray (0.1) and light gray (0.4)
        float grayValue = 0.3F + this.random.nextFloat() * 0.4F;
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
            return new CandleSmokeParticle(world, x, y, z, vx, vy, vz, spriteProvider);
        }
    }
}
