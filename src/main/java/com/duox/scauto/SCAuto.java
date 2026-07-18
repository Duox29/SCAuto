package com.duox.scauto;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(SCAuto.MOD_ID)
public class SCAuto {
    public static final String MOD_ID = "scauto_x";
    public SCAuto(IEventBus modBus) {
        modBus.addListener(this::clientSetup);
    }
    private void clientSetup(FMLClientSetupEvent event) {}
}