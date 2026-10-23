package com.donutclient.module.world;

import com.donutclient.mixin.ClientPlayerInteractionManagerAccessor;
import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.module.setting.BooleanSetting;
import com.donutclient.module.setting.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/**
 * Mine plus vite : ajoute de la progression de minage à chaque tick et supprime le délai de
 * 5 ticks entre deux blocs. En multijoueur, le serveur refuse les blocs cassés beaucoup trop vite :
 * une vitesse autour de 130 % reste généralement acceptée.
 */
public class FastMineModule extends Module {
    private final NumberSetting speed = add(new NumberSetting("Vitesse (%)", 130, 100, 300, 10));
    private final BooleanSetting noCooldown = add(new BooleanSetting("Sans délai", true));

    public FastMineModule() {
        super("FastMine", "Mine plus vite (vitesse réglable, sans délai entre les blocs).", Category.WORLD);
    }

    @Override
    public String getInfo() {
        return speed.get() + "%";
    }

    @Override
    public void onTick() {
        if (mc.interactionManager == null || mc.world == null) return;
        ClientPlayerInteractionManagerAccessor manager = (ClientPlayerInteractionManagerAccessor) mc.interactionManager;

        if (noCooldown.get() && manager.donut$getBreakingCooldown() > 0) manager.donut$setBreakingCooldown(0);

        if (!manager.donut$isBreakingBlock() || speed.get() <= 100) return;
        BlockPos pos = manager.donut$getBreakingPos();
        BlockState state = mc.world.getBlockState(pos);
        if (state.isAir()) return;
        // Le jeu a déjà ajouté 100 % de la progression ce tick : on ajoute le surplus.
        float delta = state.calcBlockBreakingDelta(mc.player, mc.world, pos);
        float extra = delta * (speed.get() - 100) / 100f;
        manager.donut$setBreakingProgress(Math.min(1f, manager.donut$getBreakingProgress() + extra));
    }
}
