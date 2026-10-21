package com.donutclient.mixin;

import com.donutclient.DonutClient;
import com.donutclient.module.render.PlayerEspModule;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class EntityMixin {
    @ModifyReturnValue(method = "getTeamColorValue", at = @At("RETURN"))
    private int donut$glowColor(int original) {
        PlayerEspModule esp = DonutClient.getModuleManager().get(PlayerEspModule.class);
        return esp != null && esp.shouldGlow((Entity) (Object) this) ? esp.getGlowColor() : original;
    }
}
