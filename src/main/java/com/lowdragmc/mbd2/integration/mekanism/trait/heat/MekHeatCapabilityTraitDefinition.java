package com.lowdragmc.mbd2.integration.mekanism.trait.heat;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.mbd2.api.capability.recipe.RecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.integration.mekanism.MekanismHeatRecipeCapability;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import lombok.Getter;
import lombok.Setter;
import mekanism.api.heat.IHeatHandler;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismBlocks;
import net.minecraftforge.common.capabilities.Capability;

@LDLRegister(name = "mek_heat_container", group = "trait", modID = "mekanism")
public class MekHeatCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition<IHeatHandler, Double> {
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.mek_heat_container.capacity")
    @NumberRange(range = {1, Double.MAX_VALUE})
    private double capacity = 5000d;
    @Getter
    @Setter
    @Configurable(name = "config.definition.trait.mek_heat_container.inverse_conduction",
            tips = "config.definition.trait.mek_heat_container.inverse_conduction.tooltip")
    @NumberRange(range = {1d, Double.MAX_VALUE})
    private double inverseConduction = 1d;

    @Override
    public SimpleCapabilityTrait<IHeatHandler, Double> createTrait(MBDMachine machine) {
        return new MekHeatCapabilityTrait(machine, this);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(MekanismBlocks.RESISTIVE_HEATER.getItemStack());
    }

    @Override
    public RecipeCapability<Double> getRecipeCapability() {
        return MekanismHeatRecipeCapability.CAP;
    }

    @Override
    public Capability<IHeatHandler> getCapability() {
        return Capabilities.HEAT_HANDLER;
    }

    @Override
    public void createTraitUITemplate(WidgetGroup ui) {
        var prefix = uiPrefixName();
        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 15, new ProgressTexture(
                IGuiTexture.EMPTY, MekanismHeatRecipeCapability.HUD_BAR
        ));
        energyBar.setBackground(MekanismHeatRecipeCapability.HUD_BACKGROUND);
        energyBar.setOverlay(new TextTexture("0/0 heat"));
        energyBar.setId(prefix);
        ui.addWidget(energyBar);
    }

    @Override
    public void initTraitUI(ITrait trait, WidgetGroup group) {
        if (trait instanceof MekHeatCapabilityTrait heatTrait) {
            var prefix = uiPrefixName();
            WidgetUtils.widgetByIdForEach(group, "^%s$".formatted(prefix), ProgressWidget.class, energyBar -> {
                energyBar.setProgressSupplier(() -> Math.max(heatTrait.container.getTotalTemperature(), 0) / heatTrait.container.getTotalHeatCapacity());
                if (energyBar.getOverlay() instanceof TextTexture textTexture) {
                    textTexture.updateText(heatTrait.container.getTotalTemperature() + "/" + heatTrait.container.getTotalHeatCapacity() + " heat");
                }
                energyBar.setDynamicHoverTips(value -> LocalizationUtils.format(
                        "config.definition.trait.gtm_energy_container.ui_container_hover",
                        heatTrait.container.getTotalTemperature(), heatTrait.container.getTotalHeatCapacity()));
            });
        }
    }
}
