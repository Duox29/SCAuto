package com.duox.scauto;


import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SCAuto.MOD_ID)
public class SCAuto {
    public static final String MOD_ID = "scauto_x";
    public SCAuto(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        modBus.addListener(this::clientSetup);
        modBus.addListener(KeyBindings::register);
        MinecraftForge.EVENT_BUS.register(SCAutoClient.class);
    }
    private void clientSetup(FMLClientSetupEvent event) {}
}