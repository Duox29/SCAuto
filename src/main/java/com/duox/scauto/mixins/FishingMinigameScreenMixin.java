package com.duox.scauto.mixins;

import com.duox.scauto.SCAutoClient;
import com.wdiscute.starcatcher.minigame.ActiveSweetSpot;
import com.wdiscute.starcatcher.minigame.FishingMinigameScreen;
import com.wdiscute.starcatcher.modifiers.minigamemodifiers.AbstractMinigameModifier;
import org.spongepowered.asm.mixin.Final;
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

    @Shadow @Final
    protected List<ActiveSweetSpot> activeSweetSpots;

    @Shadow @Final
    protected List<AbstractMinigameModifier> modifiers;

    @Shadow
    public float progress;

    @Shadow
    public int hp;

    @Shadow
    public int treasureProgress;

    @Shadow
    public abstract void inputPressed();

    @Shadow
    public abstract float getHandlePosPrecise();

    @Unique
    private int autoTickCounter = 0;

    @Unique
    private final Map<ActiveSweetSpot, Integer> autoHitCooldown = new HashMap<>();

    @Unique
    private static final int AUTO_HIT_COOLDOWN_TICKS = 5;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        autoTickCounter++;

        // Keep cooldown state clean even when autoplay is disabled.
        autoHitCooldown.keySet().removeIf(spot -> !activeSweetSpots.contains(spot));

        SCAutoClient.AutoState state = SCAutoClient.getState();

        boolean isAutoPlayEnabled =
                state != SCAutoClient.AutoState.OFF;

        boolean isTreasureEnabled =
                state == SCAutoClient.AutoState.ON_WITH_TREASURE_NO_BAD
                        || state == SCAutoClient.AutoState.ON_WITH_TREASURE_WITH_BAD;

        boolean isBadSpotEnabled =
                state == SCAutoClient.AutoState.ON_NO_TREASURE_WITH_BAD
                        || state == SCAutoClient.AutoState.ON_WITH_TREASURE_WITH_BAD;

        if (!isAutoPlayEnabled) {
            return;
        }

        /*
         * Use the exact same predicted handle position as inputPressed().
         * getHandlePosPrecise() includes hitDelay prediction.
         */
        float handleAngle = this.getHandlePosPrecise();

        float threshold = SCAutoClient.getThreshold();
        float currentRatio =
                this.hp > 0
                        ? this.progress / (float) this.hp
                        : 0.0F;

        /*
         * Treasure priority only applies while:
         * - treasure mode is enabled
         * - treasure is not already completed
         * - at least one treasure sweet spot is still actually hittable
         */
        boolean hasHittableTreasure = false;

        if (isTreasureEnabled && this.treasureProgress < 100) {
            for (ActiveSweetSpot spot : activeSweetSpots) {
                if (isTreasureSpot(spot) && isSpotPotentiallyHittable(spot)) {
                    hasHittableTreasure = true;
                    break;
                }
            }
        }

        boolean prioritizeTreasure =
                isTreasureEnabled
                        && hasHittableTreasure
                        && this.treasureProgress < 100
                        && currentRatio > threshold;

        /*
         * Mirror FishingMinigameScreen.inputPressed():
         *
         * for (ActiveSweetSpot ass : activeSweetSpots.reversed()) {
         *     if (ass.canHit
         *             && overlap(...)
         *             && modifiers.allMatch(mod -> mod.canHitSpot(...))) {
         *         ...
         *         break;
         *     }
         * }
         *
         * This matters when multiple sweet spots overlap. We decide based on
         * the exact spot the game itself would hit.
         */
        ActiveSweetSpot actualHitSpot = null;

        for (ActiveSweetSpot spot : activeSweetSpots.reversed()) {
            if (!isSpotPotentiallyHittable(spot)) {
                continue;
            }

            if (!isOverlapping(handleAngle, spot)) {
                continue;
            }

            actualHitSpot = spot;
            break;
        }

        if (actualHitSpot == null) {
            return;
        }

        boolean treasureSpot = isTreasureSpot(actualHitSpot);
        boolean badSpot = isBadSpot(actualHitSpot);

        /*
         * Do not press if the game's real target is a bad spot and the current
         * auto mode says bad spots must be skipped.
         */
        if (badSpot && !isBadSpotEnabled) {
            return;
        }

        /*
         * Do not hit treasure unless treasure mode is enabled.
         * Also stop targeting treasure once treasure progress is complete.
         */
        if (treasureSpot) {
            if (!isTreasureEnabled || this.treasureProgress >= 100) {
                return;
            }
        } else if (prioritizeTreasure) {
            /*
             * Fish progress is safely above threshold and a treasure spot is
             * still available, so wait for treasure instead of hitting normal
             * spots.
             */
            return;
        }

        /*
         * Prevent repeated inputPressed() calls against the same sweet spot on
         * consecutive ticks.
         */
        int lastHitTick =
                autoHitCooldown.getOrDefault(actualHitSpot, -1000);

        if (autoTickCounter - lastHitTick >= AUTO_HIT_COOLDOWN_TICKS) {
            this.inputPressed();
            autoHitCooldown.put(actualHitSpot, autoTickCounter);
        }
    }

    /**
     * Mirrors the non-overlap eligibility checks from
     * FishingMinigameScreen.inputPressed().
     */
    @Unique
    private boolean isSpotPotentiallyHittable(ActiveSweetSpot spot) {
        if (spot == null || !spot.canHit) {
            return false;
        }

        FishingMinigameScreen screen =
                (FishingMinigameScreen) (Object) this;

        return this.modifiers.stream()
                .allMatch(modifier -> modifier.canHitSpot(screen, spot));
    }

    @Unique
    private boolean isTreasureSpot(ActiveSweetSpot spot) {
        return getTexturePath(spot).contains("treasure");
    }

    @Unique
    private boolean isBadSpot(ActiveSweetSpot spot) {
        String path = getTexturePath(spot);

        return path.contains("tnt")
                || path.contains("wither")
                || path.contains("creeper");
    }

    /**
     * ActiveSweetSpot.texture is ScreenUtils.Image in the current API.
     * This project exposes getPath() directly on that type.
     */
    @Unique
    private String getTexturePath(ActiveSweetSpot spot) {
        if (spot == null || spot.texture == null) {
            return "";
        }

        return spot.texture.id().getPath();
    }
    /**
     * Mirrors:
     *
     * FishingMinigameScreen.doDegreesOverlapWithLeeway(
     *     getHandlePosPrecise(),
     *     ass.pos,
     *     ass.thickness / 2
     * )
     */
    @Unique
    private boolean isOverlapping(float handleAngle, ActiveSweetSpot spot) {
        int leeway = spot.thickness / 2;
        float diff = Math.abs(spot.pos - handleAngle);

        return diff < leeway
                || diff > 360.0F - leeway;
    }
}