package com.duox.scauto.mixins;

import com.wdiscute.starcatcher.minigame.ActiveSweetSpot;
import com.wdiscute.starcatcher.minigame.FishingMinigameScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(FishingMinigameScreen.class)
public abstract class FishingMinigameScreenMixin {

    @Shadow(remap = false)
    private List<ActiveSweetSpot> activeSweetSpots;

    @Shadow(remap = false)
    private float pointerPos;

    @Shadow(remap = false)
    private float pointerSpeed;

    @Shadow(remap = false)
    private int currentRotation;

    @Shadow(remap = false)
    private float partial;

    @Shadow(remap = false)
    private float hitDelay;

    @Shadow(remap = false)
    private int gracePeriod;

    // Cooldown tracking: for each sweet spot, last tick when auto-hit was triggered
    @Unique
    private final Map<ActiveSweetSpot, Integer> autoHitCooldown = new HashMap<>();

    @Unique
    private static final int AUTO_HIT_COOLDOWN_TICKS = 5; // prevent hitting the same spot too fast

    /**
     * Injects at the end of tick() to check for pointer-sweetspot overlap and auto-hit.
     */
    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void onTick(CallbackInfo ci) {
        FishingMinigameScreen screen = (FishingMinigameScreen) (Object) this;

        // Don't auto-hit during grace period
        if (screen.gracePeriod > 0) return;

        // Get precise pointer angle including hit delay
        float pointerAngle = getPointerPosPrecise(screen);

        // Check all sweet spots
        for (ActiveSweetSpot spot : activeSweetSpots) {
            if (isOverlapping(pointerAngle, spot)) {
                // Cooldown check: only hit if enough ticks passed since last auto-hit on this spot
                int lastHitTick = autoHitCooldown.getOrDefault(spot, -1000);
                if (screen.tickCount - lastHitTick >= AUTO_HIT_COOLDOWN_TICKS) {
                    // Trigger the hit
                    screen.inputPressed();
                    autoHitCooldown.put(spot, screen.tickCount);
                    break; // only one hit per tick
                }
            }
        }

        // Clean up cooldown map for spots that are no longer active (optional)
        autoHitCooldown.keySet().removeIf(spot -> !activeSweetSpots.contains(spot));
    }

    @Unique
    private float getPointerPosPrecise(FishingMinigameScreen screen) {
        float precise = screen.pointerPos + (screen.pointerSpeed * screen.partial) * screen.currentRotation;
        precise += screen.hitDelay * screen.pointerSpeed * screen.currentRotation;
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