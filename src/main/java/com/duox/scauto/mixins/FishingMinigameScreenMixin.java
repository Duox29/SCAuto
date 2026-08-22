package com.duox.scauto.mixins;

import com.duox.scauto.SCAutoClient;
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

@Mixin(value = FishingMinigameScreen.class)
public abstract class FishingMinigameScreenMixin {

    @Shadow protected List<ActiveSweetSpot> activeSweetSpots;
    @Shadow public float progress;
    @Shadow public int hp;
    @Shadow public int treasureProgress;
    @Shadow public boolean treasureActive;

    @Shadow public abstract void inputPressed();
    @Shadow public abstract float getPointerPosPrecise();

    @Unique private int autoTickCounter = 0;
    @Unique private final Map<ActiveSweetSpot, Integer> autoHitCooldown = new HashMap<>();
    @Unique private static final int AUTO_HIT_COOLDOWN_TICKS = 5;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        autoTickCounter++;

        SCAutoClient.AutoState state = SCAutoClient.getState();
        boolean isAutoPlayEnabled = state != SCAutoClient.AutoState.OFF;
        boolean isTreasureEnabled = state == SCAutoClient.AutoState.ON_WITH_TREASURE_NO_BAD || state == SCAutoClient.AutoState.ON_WITH_TREASURE_WITH_BAD;
        boolean isBadSpotEnabled = state == SCAutoClient.AutoState.ON_NO_TREASURE_WITH_BAD || state == SCAutoClient.AutoState.ON_WITH_TREASURE_WITH_BAD;

        if (!isAutoPlayEnabled) return;

        float pointerAngle = this.getPointerPosPrecise();
        float threshold = SCAutoClient.getThreshold();
        float currentRatio = this.hp > 0 ? (this.progress / (float) this.hp) : 0;

        boolean hasTreasureOnScreen = false;
        for (ActiveSweetSpot spot : activeSweetSpots) {
            if (spot.texture != null && spot.texture.getPath().contains("treasure")) {
                hasTreasureOnScreen = true;
                break;
            }
        }

        boolean prioritizeTreasure = isTreasureEnabled && hasTreasureOnScreen && (this.treasureProgress < 100) && (currentRatio > threshold);

        ActiveSweetSpot targetSpot = null;

        for (ActiveSweetSpot spot : activeSweetSpots) {
            if (spot.texture == null) continue;

            String texPath = spot.texture.getPath();
            boolean isTreasure = texPath.contains("treasure");
            boolean isBadSpot = texPath.contains("tnt") || texPath.contains("wither") || texPath.contains("creeper");

            if (isBadSpot && !isBadSpotEnabled) continue;

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

        if (targetSpot != null) {
            int lastHitTick = autoHitCooldown.getOrDefault(targetSpot, -1000);
            if (autoTickCounter - lastHitTick >= AUTO_HIT_COOLDOWN_TICKS) {
                this.inputPressed();
                autoHitCooldown.put(targetSpot, autoTickCounter);
            }
        }

        autoHitCooldown.keySet().removeIf(spot -> !activeSweetSpots.contains(spot));
    }

    @Unique
    private boolean isOverlapping(float pointerAngle, ActiveSweetSpot spot) {
        int leeway = spot.thickness / 2;
        float spotPos = spot.pos;
        float diff = Math.abs(spotPos - pointerAngle);
        return diff < leeway || Math.abs(diff - 360) < leeway;
    }
}