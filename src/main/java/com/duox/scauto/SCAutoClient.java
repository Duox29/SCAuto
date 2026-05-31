package com.duox.scauto;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class SCAutoClient {
    public enum AutoState {
        OFF,
        ON_NO_TREASURE,
        ON_WITH_TREASURE
    }

    private static AutoState currentState = AutoState.OFF;
    private static boolean lastKeyState = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Toggle state on key press (edge triggered)
        boolean pressed = KeyBindings.toggleKey.isDown();
        if (pressed && !lastKeyState) {
            cycleState();
            sendStateMessage(mc);
        }
        lastKeyState = pressed;
    }

    private static void cycleState() {
        currentState = switch (currentState) {
            case OFF -> AutoState.ON_NO_TREASURE;
            case ON_NO_TREASURE -> AutoState.ON_WITH_TREASURE;
            case ON_WITH_TREASURE -> AutoState.OFF;
        };
    }

    private static void sendStateMessage(Minecraft mc) {
        String message = switch (currentState) {
            case OFF -> "OFF";
            case ON_NO_TREASURE -> "ON (no treasure)";
            case ON_WITH_TREASURE -> "ON (with treasure)";
        };
        mc.player.displayClientMessage(Component.literal(message), true);
    }

    public static AutoState getState() {
        return currentState;
    }
}