package com.duox.scauto;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT)
public class KeyBindings {
    public static final String KEY_CATEGORY = "key.category.autofishing";

    public static final String KEY_TOGGLE = "key.autofishing.toggle";
    public static final String KEY_THRESHOLD_UP = "key.autofishing.threshold_up";
    public static final String KEY_THRESHOLD_DOWN = "key.autofishing.threshold_down";

    public static KeyMapping toggleKey = new KeyMapping(
            KEY_TOGGLE,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KEY_CATEGORY
    );

    public static KeyMapping thresholdUpKey = new KeyMapping(
            KEY_THRESHOLD_UP,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_EQUAL,
            KEY_CATEGORY
    );

    public static KeyMapping thresholdDownKey = new KeyMapping(
            KEY_THRESHOLD_DOWN,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_MINUS,
            KEY_CATEGORY
    );

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(toggleKey);
        event.register(thresholdUpKey);
        event.register(thresholdDownKey);
    }
}