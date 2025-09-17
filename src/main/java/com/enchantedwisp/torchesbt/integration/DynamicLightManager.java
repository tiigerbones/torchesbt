package com.enchantedwisp.torchesbt.integration;

import com.enchantedwisp.torchesbt.util.ConfigCache;

public class DynamicLightManager {
    public static boolean isDynamicLightsEnabled() {
        return ConfigCache.isDynamicLightsEnabled();
    }
}
