package com.donutclient.mixin;

import com.donutclient.DonutClient;
import com.donutclient.module.misc.FakeMoneyModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DrawContext.class)
public class DrawContextMixin {
    @ModifyVariable(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V",
            at = @At("HEAD"), argsOnly = true)
    private Text donut$fakeMoney(Text text) {
        return donut$apply(text);
    }

    @ModifyVariable(method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V",
            at = @At("HEAD"), argsOnly = true)
    private Text donut$fakeMoneyShadow(Text text) {
        return donut$apply(text);
    }

    private static Text donut$apply(Text text) {
        if (DonutClient.getModuleManager() == null) return text;
        FakeMoneyModule fakeMoney = DonutClient.getModuleManager().get(FakeMoneyModule.class);
        return fakeMoney != null ? fakeMoney.apply(text) : text;
    }
}
