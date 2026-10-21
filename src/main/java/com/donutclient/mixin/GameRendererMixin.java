package com.donutclient.mixin;

import com.donutclient.DonutClient;
import com.donutclient.module.render.ZoomModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void donut$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        ZoomModule zoom = DonutClient.getModuleManager().get(ZoomModule.class);
        if (zoom != null && zoom.isZooming()) cir.setReturnValue(zoom.apply(cir.getReturnValue()));
    }
}
