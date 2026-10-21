package com.donutclient.mixin;

import com.donutclient.DonutClient;
import com.donutclient.module.render.ZoomModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float donut$zoom(float fov) {
        ZoomModule zoom = DonutClient.getModuleManager().get(ZoomModule.class);
        return zoom != null ? zoom.apply(fov) : fov;
    }
}
