package com.donutclient.mixin;

import com.donutclient.DonutClient;
import com.donutclient.module.misc.TexturePackModule;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemModelManager.class)
public class ItemModelManagerMixin {
    /** Tous les rendus d'items (inventaire, main, items au sol...) passent par ces méthodes. */
    @ModifyVariable(method = {"update", "clearAndUpdate", "updateForLivingEntity", "updateForNonLivingEntity"},
            at = @At("HEAD"), argsOnly = true)
    private ItemStack donut$texturePack(ItemStack stack) {
        if (DonutClient.getModuleManager() == null) return stack;
        TexturePackModule texturePack = DonutClient.getModuleManager().get(TexturePackModule.class);
        return texturePack != null ? texturePack.apply(stack) : stack;
    }
}
