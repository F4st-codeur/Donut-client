package com.donutclient.util;

import com.donutclient.DonutClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Accès au schéma chargé dans Litematica, par réflexion : Litematica reste optionnel,
 * le client fonctionne sans lui.
 */
public final class LitematicaBridge {
    /** Zone (bornes incluses) d'une sous-région d'un placement Litematica. */
    public record Region(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public long volume() {
            return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }
    }

    private static Boolean available;
    private static Method getSchematicWorld, getPlacementManager, getAllPlacements, isEnabled, getSubRegionBoxes, getPos1, getPos2;
    private static Object placementEnabled;

    private LitematicaBridge() {
    }

    public static boolean isAvailable() {
        if (available == null) init();
        return available;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void init() {
        available = false;
        if (!FabricLoader.getInstance().isModLoaded("litematica")) return;
        try {
            getSchematicWorld = Class.forName("fi.dy.masa.litematica.world.SchematicWorldHandler").getMethod("getSchematicWorld");
            getPlacementManager = Class.forName("fi.dy.masa.litematica.data.DataManager").getMethod("getSchematicPlacementManager");
            getAllPlacements = Class.forName("fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager").getMethod("getAllSchematicsPlacements");
            Class<?> requiredEnabled = Class.forName("fi.dy.masa.litematica.schematic.placement.SubRegionPlacement$RequiredEnabled");
            placementEnabled = Enum.valueOf((Class<Enum>) requiredEnabled, "PLACEMENT_ENABLED");
            Class<?> placement = Class.forName("fi.dy.masa.litematica.schematic.placement.SchematicPlacement");
            isEnabled = placement.getMethod("isEnabled");
            getSubRegionBoxes = placement.getMethod("getSubRegionBoxes", requiredEnabled);
            Class<?> box = Class.forName("fi.dy.masa.litematica.selection.Box");
            getPos1 = box.getMethod("getPos1");
            getPos2 = box.getMethod("getPos2");
            available = true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            DonutClient.LOGGER.warn("Litematica trouvé mais son API a changé, intégration désactivée", e);
        }
    }

    /** Monde « fantôme » de Litematica contenant les blocs du schéma, ou {@code null}. */
    public static World getSchematicWorld() {
        if (!isAvailable()) return null;
        try {
            return (World) getSchematicWorld.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** Zones de tous les placements actifs. */
    public static List<Region> getRegions() {
        List<Region> regions = new ArrayList<>();
        if (!isAvailable()) return regions;
        try {
            Object manager = getPlacementManager.invoke(null);
            for (Object placement : (List<?>) getAllPlacements.invoke(manager)) {
                if (!(Boolean) isEnabled.invoke(placement)) continue;
                for (Object box : ((Map<?, ?>) getSubRegionBoxes.invoke(placement, placementEnabled)).values()) {
                    BlockPos a = (BlockPos) getPos1.invoke(box);
                    BlockPos b = (BlockPos) getPos2.invoke(box);
                    if (a == null || b == null) continue;
                    regions.add(new Region(
                            Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                            Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ())));
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            DonutClient.LOGGER.warn("Impossible de lire les placements Litematica", e);
        }
        return regions;
    }
}
