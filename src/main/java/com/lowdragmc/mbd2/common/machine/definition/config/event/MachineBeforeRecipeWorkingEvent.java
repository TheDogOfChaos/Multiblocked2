package com.lowdragmc.mbd2.common.machine.definition.config.event;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.machine.definition.config.event.graphprocess.GraphParameterGet;
import lombok.Getter;
import net.minecraftforge.eventbus.api.Cancelable;

import java.util.Map;
import java.util.Optional;

@Getter
@Cancelable
@LDLRegister(name = "MachineBeforeRecipeWorkingEvent", group = "MachineEvent")
public class MachineBeforeRecipeWorkingEvent extends MachineEvent {
    @GraphParameterGet
    public final MBDRecipe recipe;

    public MachineBeforeRecipeWorkingEvent(MBDMachine machine, MBDRecipe recipe) {
        super(machine);
        this.recipe = recipe;
    }

    @Override
    public void bindParameters(Map<String, ExposedParameter> exposedParameters) {
        super.bindParameters(exposedParameters);
        Optional.ofNullable(exposedParameters.get("recipe")).ifPresent(p -> p.setValue(recipe));
    }
}