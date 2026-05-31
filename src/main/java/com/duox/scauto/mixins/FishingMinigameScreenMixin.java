package com.duox.scauto.mixins;

import com.duox.scauto.SCAutoClient;
import com.wdiscute.starcatcher.minigame.ActiveSweetSpot;
import com.wdiscute.starcatcher.minigame.FishingMinigameScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = FishingMinigameScreen.class, remap = false)
public abstract class FishingMinigameScreenMixin {

    @Shadow protected List<ActiveSweetSpot> activeSweetSpots;
    @Shadow public float pointerPos;
    @Shadow public float pointerSpeed;
    @Shadow public int currentRotation;
    @Shadow public float partial;
    @Shadow public float hitDelay;

    // We no longer shadow gracePeriod because we want to ignore it and strike immediately
    @Shadow public float progress;
    @Shadow public int hp;
    @Shadow public int treasureProgress;

    @Shadow public abstract void inputPressed();

    @Unique private int autoTickCounter = 0;
    @Unique private final Map<ActiveSweetSpot, Integer> autoHitCooldown = new HashMap<>();
    @Unique private static final int AUTO_HIT_COOLDOWN_TICKS = 5;

    // Prevents log spam by only logging when a treasure newly appears
    @Unique private boolean hasLoggedTreasure = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        autoTickCounter++;

        // 1. LINK TO YOUR CUSTOM STATE MANAGER
        boolean isAutoPlayEnabled = SCAutoClient.getState() != SCAutoClient.AutoState.OFF;
        boolean isTreasureEnabled = SCAutoClient.getState() == SCAutoClient.AutoState.ON_WITH_TREASURE;

        // If AutoPlay is off, do absolutely nothing.
        if (!isAutoPlayEnabled) return;

        float pointerAngle = getPointerPosPrecise();
        float threshold = 0.70f;
        float currentRatio = this.hp > 0 ? (this.progress / (float) this.hp) : 0;

        // 2. CHECK IF TREASURE IS ACTIVELY ON SCREEN
        boolean hasTreasureOnScreen = false;
        for (ActiveSweetSpot spot : activeSweetSpots) {
            if (spot.texture != null && spot.texture.getPath().contains("treasure")) {
                hasTreasureOnScreen = true;
                break;
            }
        }

        // 3. CONSOLE LOGGING FOR DEBUGGING
        if (hasTreasureOnScreen && !hasLoggedTreasure) {
            System.out.println("[SCAuto] Treasure detected on screen!");
            hasLoggedTreasure = true;
        } else if (!hasTreasureOnScreen) {
            hasLoggedTreasure = false;
        }

        // Decide if we need to stall normal hits for the treasure
        boolean prioritizeTreasure = isTreasureEnabled && hasTreasureOnScreen && (this.treasureProgress < 100) && (currentRatio > threshold);

        ActiveSweetSpot targetSpot = null;

        for (ActiveSweetSpot spot : activeSweetSpots) {
            if (spot.texture == null) continue;

            String texPath = spot.texture.getPath();
            boolean isTreasure = texPath.contains("treasure");
            boolean isBadSpot = texPath.contains("tnt") || texPath.contains("wither") || texPath.contains("creeper");

            // Ignore Traps
            if (isBadSpot) continue;

            if (isOverlapping(pointerAngle, spot)) {
                if (!isTreasureEnabled) {
                    if (!isTreasure) {
                        targetSpot = spot;
                        break;
                    }
                } else {
                    if (isTreasure) {
                        if (this.treasureProgress < 100) {
                            targetSpot = spot;
                            break;
                        }
                    } else {
                        if (!prioritizeTreasure) {
                            targetSpot = spot;
                            break;
                        }
                    }
                }
            }
        }

        // 4. EXECUTE HIT
        if (targetSpot != null) {
            int lastHitTick = autoHitCooldown.getOrDefault(targetSpot, -1000);
            if (autoTickCounter - lastHitTick >= AUTO_HIT_COOLDOWN_TICKS) {
                if (targetSpot.texture != null && targetSpot.texture.getPath().contains("treasure")) {
                    System.out.println("[SCAuto] Striking Treasure Spot!");
                    sendFeedback("§6[SCAuto] Hit Treasure!");
                }

                this.inputPressed();
                autoHitCooldown.put(targetSpot, autoTickCounter);
            }
        }

        autoHitCooldown.keySet().removeIf(spot -> !activeSweetSpots.contains(spot));
    }

    @Unique
    private void sendFeedback(String message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal(message), true);
        }
    }

    @Unique
    private float getPointerPosPrecise() {
        float precise = this.pointerPos + (this.pointerSpeed * this.partial) * this.currentRotation;
        precise += this.hitDelay * this.pointerSpeed * this.currentRotation;
        return precise;
    }

    @Unique
    private boolean isOverlapping(float pointerAngle, ActiveSweetSpot spot) {
        int leeway = spot.thickness / 2;
        float spotPos = spot.pos;
        float diff = Math.abs(spotPos - pointerAngle);
        return diff < leeway || Math.abs(diff - 360) < leeway;
    }
}