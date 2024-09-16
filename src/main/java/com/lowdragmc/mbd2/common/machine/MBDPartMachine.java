package com.lowdragmc.mbd2.common.machine;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeCapabilityHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.machine.IMultiController;
import com.lowdragmc.mbd2.api.machine.IMultiPart;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.api.recipe.RecipeCondition;
import com.lowdragmc.mbd2.api.recipe.RecipeLogic;
import com.lowdragmc.mbd2.api.recipe.content.ContentModifier;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import com.lowdragmc.mbd2.common.machine.definition.config.ConfigPartSettings;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

public class MBDPartMachine extends MBDMachine implements IMultiPart {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MBDPartMachine.class, MBDMachine.MANAGED_FIELD_HOLDER);
    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @DescSynced
    @RequireRerender
    protected final Set<BlockPos> controllerPositions  = new HashSet<>();
    @Getter
    @DescSynced
    @RequireRerender
    protected boolean disableRendering = false;

    public MBDPartMachine(IMachineBlockEntity machineHolder, MBDMachineDefinition definition, Object... args) {
        super(machineHolder, definition, args);
    }

    /**
     * Whether it belongs to the specified controller.
     */
    @Override
    public boolean hasController(BlockPos controllerPos) {
        return controllerPositions.contains(controllerPos);
    }

    /**
     * Whether it belongs to a formed Multiblock.
     */
    @Override
    public boolean isFormed() {
        return !controllerPositions.isEmpty();
    }

    /**
     * Get all attached controllers
     */
    @Override
    public List<IMultiController> getControllers() {
        List<IMultiController> result = new ArrayList<>();
        for (var blockPos : controllerPositions) {
            IMultiController.ofController(getLevel(), blockPos).ifPresent(result::add);
        }
        return result;
    }

    /**
     * Get all available traits for recipe logic. It is only used for controller recipe logic.
     * <br>
     * For self recipe logic, use {@link IRecipeCapabilityHolder#getCapabilitiesProxy()} to get recipe handlers.
     */
    @Override
    public List<IRecipeHandlerTrait> getRecipeHandlers() {
        return getAdditionalTraits().stream().filter(IRecipeHandlerTrait.class::isInstance).map(IRecipeHandlerTrait.class::cast)
                .toList();
    }

    /**
     * on machine invalid in the chunk.
     * <br>
     * You should call it in yourselves {@link BlockEntity#setRemoved()}.
     */
    @Override
    public void onUnload() {
        super.onUnload();
        var level = getLevel();
        for (BlockPos pos : controllerPositions) {
            if (level instanceof ServerLevel && level.isLoaded(pos)) {
                IMultiController.ofController(getLevel(), pos).ifPresent(IMultiController::onPartUnload);
            }
        }
        controllerPositions.clear();
    }

    /**
     * Called when it was added to a multiblock.
     */
    @Override
    public void removedFromController(IMultiController controller) {
        controllerPositions.remove(controller.getPos());
        checkDisabledRendering();
    }

    @Override
    public void addedToController(IMultiController controller) {
        controllerPositions.add(controller.getPos());
        checkDisabledRendering();
    }

    /**
     * check if there is any controller ask the part to disable rendering.
     */
    public void checkDisabledRendering() {
        var result = false;
        for (var controller : getControllers()) {
            if (controller instanceof MBDMultiblockMachine machine) {
                if (machine.getRenderingDisabledPositions().contains(getPos())) {
                    result = true;
                    break;
                }
            }
        }
        disableRendering = result;
    }

    /**
     * Can it be shared among multi multiblock.
     */
    @Override
    public boolean canShared() {
        return Optional.ofNullable(getDefinition().partSettings()).map(ConfigPartSettings::canShare).orElse(true);
    }

    /**
     * Called when controller recipe logic status changed
     */
    @Override
    public void notifyControllerRecipeStatusChanged(IMultiController controller, RecipeLogic.Status oldStatus, RecipeLogic.Status newStatus) {
        IMultiPart.super.notifyControllerRecipeStatusChanged(controller, oldStatus, newStatus);
        if (isFormed()) {
            switch (newStatus) {
                case WORKING -> setMachineState("working");
                case IDLE -> setMachineState("formed");
                case WAITING -> setMachineState("waiting");
                case SUSPEND -> setMachineState("suspend");
            }
        } else {
            setMachineState("base");
        }
    }

    /**
     * Override it to modify controller recipe on the fly e.g. applying overclock, change chance, etc
     * <br>
     * We will apply part recipe modifiers here. see {@link ConfigPartSettings#recipeModifiers()}.
     * @param recipe recipe from detected from MBDRecipeType
     * @param controllerRecipeLogic controller recipe logic
     * @return modified recipe.
     *         null -- this recipe is unavailable
     */
    @Override
    public MBDRecipe modifyControllerRecipe(MBDRecipe recipe, RecipeLogic controllerRecipeLogic) {
        if (getDefinition().partSettings() != null) {
            var contentModifiers = new ArrayList<ContentModifier>();
            var durationModifiers = new ArrayList<ContentModifier>();

            for (var modifier : getDefinition().partSettings().recipeModifiers()) {
                var or = new HashMap<String, List<RecipeCondition>>();
                var success = true;
                for (RecipeCondition condition : modifier.recipeConditions) {
                    if (condition.isOr()) {
                        or.computeIfAbsent(condition.getType(), type -> new ArrayList<>()).add(condition);
                    } else if (condition.test(recipe, controllerRecipeLogic) == condition.isReverse()) {
                        success = false;
                        break;
                    }
                }
                for (List<RecipeCondition> conditions : or.values()) {
                    MBDRecipe finalRecipe = recipe;
                    if (conditions.stream().allMatch(condition -> condition.test(finalRecipe, controllerRecipeLogic) == condition.isReverse())) {
                        success = false;
                        break;
                    }
                }
                if (success) {
                    contentModifiers.add(modifier.contentModifier);
                    durationModifiers.add(modifier.durationModifier);
                }
            }
            if (!contentModifiers.isEmpty()) {
                recipe = recipe.copy(contentModifiers.stream().reduce(ContentModifier::merge).orElseThrow());
                recipe.duration = durationModifiers.stream().reduce(ContentModifier::merge).orElseThrow().apply(recipe.duration).intValue();
            }
        }
        return recipe;
    }
}
