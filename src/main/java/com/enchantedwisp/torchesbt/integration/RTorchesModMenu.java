package com.enchantedwisp.torchesbt.integration;

import com.enchantedwisp.torchesbt.config.RTorchesConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class RTorchesModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return RTorchesConfigScreen::createConfigScreen;
    }
}