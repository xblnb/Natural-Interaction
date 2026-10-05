package com.natural_interaction;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(natural_interaction.MODID)
public final class natural_interaction {

    public static final String MODID = "financialmarket";

    public natural_interaction(IEventBus modBus) {
        if (FMLEnvironment.dist.isClient()) {
            AccessibilityClient.init(modBus);
        }
    }
}