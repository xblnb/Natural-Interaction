package com.naturalinteraction;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(naturalinteraction.MODID)
public final class naturalinteraction {

    public static final String MODID = "financialmarket";

    public naturalinteraction(IEventBus modBus) {
        if (FMLEnvironment.dist.isClient()) {
            AccessibilityClient.init(modBus);
        }
    }
}