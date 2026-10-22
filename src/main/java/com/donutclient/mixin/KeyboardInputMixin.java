package com.donutclient.mixin;

import com.donutclient.DonutClient;
import com.donutclient.module.render.FreecamModule;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    /** En freecam, les touches déplacent la caméra : le joueur, lui, ne bouge plus. */
    @Inject(method = "tick", at = @At("TAIL"))
    private void donut$freecam(CallbackInfo ci) {
        if (DonutClient.getModuleManager() == null) return;
        FreecamModule freecam = DonutClient.getModuleManager().get(FreecamModule.class);
        if (freecam == null || !freecam.isActive()) return;
        this.playerInput = PlayerInput.DEFAULT;
        this.movementVector = Vec2f.ZERO;
    }
}
