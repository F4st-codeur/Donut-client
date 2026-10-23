package com.donutclient.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Accessor("currentBreakingProgress")
    float donut$getBreakingProgress();

    @Accessor("currentBreakingProgress")
    void donut$setBreakingProgress(float progress);

    @Accessor("blockBreakingCooldown")
    int donut$getBreakingCooldown();

    @Accessor("blockBreakingCooldown")
    void donut$setBreakingCooldown(int cooldown);

    @Accessor("breakingBlock")
    boolean donut$isBreakingBlock();

    @Accessor("currentBreakingPos")
    BlockPos donut$getBreakingPos();
}
