package com.lowdragmc.mbd2.integration.mekanism.trait.heat;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextTextureWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ITrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTraitDefinition;
import com.lowdragmc.mbd2.integration.mekanism.MekanismHeatRecipeCapability;
import com.lowdragmc.mbd2.utils.WidgetUtils;
import lombok.Getter;
import lombok.Setter;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.network.chat.Component;

@Setter
@Getter
@LDLRegister(name = "mek_heat_container", group = "trait", modID = "mekanism")
public class MekHeatCapabilityTraitDefinition extends SimpleCapabilityTraitDefinition {

    @Configurable(name = "config.definition.trait.mek_heat_container.capacity")
    @NumberRange(range = {1, Double.MAX_VALUE})
    private double capacity = 5000d;
    @Configurable(name = "config.definition.trait.mek_heat_container.min_heat_display",
            tips = {
                    "config.definition.trait.mek_heat_container.min_heat_display.tooltip.0",
                    "config.definition.trait.mek_heat_container.min_heat_display.tooltip.1"
            })
    @NumberRange(range = {-Double.MAX_VALUE, Double.MAX_VALUE})
    private double minHeatDisplay = 0;
    @Configurable(name = "config.definition.trait.mek_heat_container.inverse_conduction",
            tips = "config.definition.trait.mek_heat_container.inverse_conduction.tooltip")
    @NumberRange(range = {1d, Double.MAX_VALUE})
    private double inverseConduction = 1d;

    @Override
    public SimpleCapabilityTrait createTrait(MBDMachine machine) {
        return new MekHeatCapabilityTrait(machine, this);
    }

    @Override
    public IGuiTexture getIcon() {
        return new ItemStackTexture(MekanismBlocks.RESISTIVE_HEATER.getItemStack());
    }

    @Override
    public void createTraitUITemplate(WidgetGroup ui) {
        var prefix = uiPrefixName();
        var energyBar = new ProgressWidget(ProgressWidget.JEIProgress, 0, 0, 100, 15, new ProgressTexture(
                IGuiTexture.EMPTY, MekanismHeatRecipeCapability.HUD_BAR
        ));
        energyBar.setBackground(MekanismHeatRecipeCapability.HUD_BACKGROUND);
        energyBar.setId(prefix);
        var energyBarText = new TextTextureWidget(5, 3, 90, 10)
                .setText("0 heat")
                .textureStyle(texture -> texture.setDropShadow(true));
        energyBarText.setId(prefix + "_text");
        ui.addWidget(energyBar);
        ui.addWidget(energyBarText);
    }

    @Override
    public void initTraitUI(ITrait trait, WidgetGroup group) {
        if (trait instanceof MekHeatCapabilityTrait heatTrait) {
            var prefix = uiPrefixName();
            var range = capacity - minHeatDisplay;
            WidgetUtils.widgetByIdForEach(group, "^%s$".formatted(prefix), ProgressWidget.class, energyBar -> {
                energyBar.setProgressSupplier(() -> Math.max(heatTrait.container.getTotalTemperature() - minHeatDisplay, 0) / range);
                energyBar.setDynamicHoverTips(value -> LocalizationUtils.format(
                        "config.definition.trait.mek_heat_container.ui_container_hover",
                        Math.round(range * value), range));
            });
            WidgetUtils.widgetByIdForEach(group, "^%s_text$".formatted(prefix), TextTextureWidget.class, energyBarText -> {
                energyBarText.setText(() -> Component.literal(Math.round(heatTrait.container.getTotalTemperature()) + " heat"));
            });
        }
    }
}
