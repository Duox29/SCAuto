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
        ON_NO_TREASURE_NO_BAD,
        ON_WITH_TREASURE_NO_BAD,
        ON_NO_TREASURE_WITH_BAD,
        ON_WITH_TREASURE_WITH_BAD
    }

    private static AutoState currentState = AutoState.OFF;
    private static float threshold = 0.70f;

    private static boolean lastKeyState = false;
    private static boolean lastUpState = false;
    private static boolean lastDownState = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean pressed = KeyBindings.toggleKey.isDown();
        if (pressed && !lastKeyState) {
            cycleState();
            sendStateMessage(mc);
        }
        lastKeyState = pressed;

        boolean upPressed = KeyBindings.thresholdUpKey.isDown();
        if (upPressed && !lastUpState) {
            threshold = Math.min(0.95f, threshold + 0.05f);
            mc.player.displayClientMessage(Component.literal("§e[SCAuto] Threshold: §a" + (int)(threshold * 100) + "%"), true);
        }
        lastUpState = upPressed;

        boolean downPressed = KeyBindings.thresholdDownKey.isDown();
        if (downPressed && !lastDownState) {
            threshold = Math.max(0.10f, threshold - 0.05f);
            mc.player.displayClientMessage(Component.literal("§e[SCAuto] Threshold: §c" + (int)(threshold * 100) + "%"), true);
        }
        lastDownState = downPressed;
    }

    private static void cycleState() {
        currentState = switch (currentState) {
            case OFF -> AutoState.ON_NO_TREASURE_NO_BAD;
            case ON_NO_TREASURE_NO_BAD -> AutoState.ON_WITH_TREASURE_NO_BAD;
            case ON_WITH_TREASURE_NO_BAD -> AutoState.ON_NO_TREASURE_WITH_BAD;
            case ON_NO_TREASURE_WITH_BAD -> AutoState.ON_WITH_TREASURE_WITH_BAD;
            case ON_WITH_TREASURE_WITH_BAD -> AutoState.OFF;
        };
    }

    private static void sendStateMessage(Minecraft mc) {
        String message = switch (currentState) {
            case OFF -> "§cOFF";
            case ON_NO_TREASURE_NO_BAD -> "§aON §7(No Treasure, No Bad Spots)";
            case ON_WITH_TREASURE_NO_BAD -> "§aON §e(With Treasure) §7(No Bad Spots)";
            case ON_NO_TREASURE_WITH_BAD -> "§aON §7(No Treasure) §c(With Bad Spots)";
            case ON_WITH_TREASURE_WITH_BAD -> "§aON §e(With Treasure) §c(With Bad Spots)";
        };
        mc.player.displayClientMessage(Component.literal("§b[SCAuto] Mode: " + message), true);
    }

    public static AutoState getState() {
        return currentState;
    }

    public static float getThreshold() {
        return threshold;
    }
}