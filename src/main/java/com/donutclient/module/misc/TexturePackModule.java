package com.donutclient.module.misc;

import com.donutclient.module.Category;
import com.donutclient.module.Module;
import com.donutclient.module.setting.BooleanSetting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Change l'apparence de certains items, comme un pack de textures, uniquement sur ton écran.
 * Les items ne changent pas : le serveur et les autres joueurs voient toujours les vrais.
 */
public class TexturePackModule extends Module {
    private final BooleanSetting chainmailToElytra = add(new BooleanSetting("Plastron maille > Elytra", true));
    private final BooleanSetting pickleToSpawner = add(new BooleanSetting("Cornichon > Spawner", true));

    private Item chainmailChestplate, elytra, seaPickle, spawner;

    public TexturePackModule() {
        super("TexturePack", "Remplace l'apparence de certains items sur ton écran (visuel uniquement).", Category.MISC);
    }

    private static Item item(String id) {
        return Registries.ITEM.get(Identifier.of("minecraft", id));
    }

    /** Renvoie l'item à afficher à la place de {@code stack}, ou {@code stack} lui-même. */
    public ItemStack apply(ItemStack stack) {
        if (!isEnabled() || stack == null || stack.isEmpty()) return stack;
        if (elytra == null) {
            chainmailChestplate = item("chainmail_chestplate");
            elytra = item("elytra");
            seaPickle = item("sea_pickle");
            spawner = item("spawner");
        }
        if (chainmailToElytra.get() && stack.isOf(chainmailChestplate)) return new ItemStack(elytra, stack.getCount());
        if (pickleToSpawner.get() && stack.isOf(seaPickle)) return new ItemStack(spawner, stack.getCount());
        return stack;
    }
}
