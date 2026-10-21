package com.donutclient.mixin;

import com.donutclient.DonutClient;
import com.donutclient.module.render.PlayerEspModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @ModifyReturnValue(method = "hasOutline", at = @At("RETURN"))
    private boolean donut$playerGlow(boolean original, Entity entity) {
        if (original) return true;
        PlayerEspModule esp = DonutClient.getModuleManager().get(PlayerEspModule.class);
        return esp != null && esp.shouldGlow(entity);
    }
}
