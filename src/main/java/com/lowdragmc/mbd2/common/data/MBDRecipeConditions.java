package com.lowdragmc.mbd2.common.data;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.registry.MBDRegistries;
import com.lowdragmc.mbd2.common.event.MBDRegistryEvent;
import com.lowdragmc.mbd2.common.recipe.*;
import com.lowdragmc.mbd2.integration.create.CreateRotationCondition;
import com.lowdragmc.mbd2.integration.mekanism.MekanismHeatCondition;
import net.minecraftforge.fml.ModLoader;

public final class MBDRecipeConditions {

    private MBDRecipeConditions() {}

    public static void init() {
        MBDRegistries.RECIPE_CONDITIONS.unfreeze();
        MBDRegistries.RECIPE_CONDITIONS.register(BiomeCondition.INSTANCE.getType(), BiomeCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(DimensionCondition.INSTANCE.getType(), DimensionCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(PositionYCondition.INSTANCE.getType(), PositionYCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(RainingCondition.INSTANCE.getType(), RainingCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(ThunderCondition.INSTANCE.getType(), ThunderCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(MachineLevelCondition.INSTANCE.getType(), MachineLevelCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(MachineCustomDataCondition.INSTANCE.getType(), MachineCustomDataCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(BlockCondition.INSTANCE.getType(), BlockCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(DayLightCondition.INSTANCE.getType(), DayLightCondition.class);
        MBDRegistries.RECIPE_CONDITIONS.register(RedstoneSignalCondition.INSTANCE.getType(), RedstoneSignalCondition.class);
        if (MBD2.isCreateLoaded()) {
            MBDRegistries.RECIPE_CONDITIONS.register(CreateRotationCondition.INSTANCE.getType(), CreateRotationCondition.class);
        }
        if (MBD2.isMekanismLoaded()) {
            MBDRegistries.RECIPE_CONDITIONS.register(MekanismHeatCondition.INSTANCE.getType(), MekanismHeatCondition.class);
        }
        ModLoader.get().postEvent(new MBDRegistryEvent.RecipeCondition());
        MBDRegistries.RECIPE_CONDITIONS.freeze();
    }
}
