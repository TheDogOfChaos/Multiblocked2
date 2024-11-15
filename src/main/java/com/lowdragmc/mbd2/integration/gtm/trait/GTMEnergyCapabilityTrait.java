package com.lowdragmc.mbd2.integration.gtm.trait;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.ICapabilityProviderTrait;
import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import com.lowdragmc.mbd2.integration.gtm.GTMEnergyRecipeCapability;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public class GTMEnergyCapabilityTrait extends SimpleCapabilityTrait {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(GTMEnergyCapabilityTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final CopiableEnergyContainer container;
    private final EnergyRecipeHandler recipeHandler = new EnergyRecipeHandler();
    private final EnergyContainerCap energyContainerCap = new EnergyContainerCap();

    public GTMEnergyCapabilityTrait(MBDMachine machine, GTMEnergyCapabilityTraitDefinition definition) {
        super(machine, definition);
        container = createStorages(machine);
        container.setOnContentsChanged(this::notifyListeners);
    }

    @Override
    public GTMEnergyCapabilityTraitDefinition getDefinition() {
        return (GTMEnergyCapabilityTraitDefinition) super.getDefinition();
    }

    @Override
    public void onLoadingTraitInPreview() {
        container.setEnergyStored(getDefinition().getCapacity() / 2);
    }

    protected CopiableEnergyContainer createStorages(MBDMachine machine) {
        return new CopiableEnergyContainer(machine, getDefinition().isExplosionMachine(), getDefinition().getCapacity(),
                getDefinition().getInputAmperage(), getDefinition().getInputVoltage(),
                getDefinition().getOutputAmperage(), getDefinition().getOutputVoltage());
    }


    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    @Override
    public List<ICapabilityProviderTrait<?>> getCapabilityProviderTraits() {
        return List.of(energyContainerCap);
    }

    public class EnergyRecipeHandler extends RecipeHandlerTrait<Long> {
        protected EnergyRecipeHandler() {
            super(GTMEnergyCapabilityTrait.this, GTMEnergyRecipeCapability.CAP);
        }

        @Override
        public List<Long> handleRecipeInner(IO io, MBDRecipe recipe, List<Long> left, @Nullable String slotName, boolean simulate) {
            if (!compatibleWith(io)) return left;
            long required = left.stream().reduce(0L, Long::sum);
            var capability = simulate ? container.copy() : container;
            if (io == IO.IN) {
                var canOutput = capability.getEnergyStored();
                if (!simulate) {
                    capability.addEnergy(-Math.min(canOutput, required));
                }
                required -= canOutput;
            } else if (io == IO.OUT) {
                long canInput = capability.getEnergyCapacity() - capability.getEnergyStored();
                if (!simulate) {
                    capability.addEnergy(Math.min(canInput, required));
                }
                required -= canInput;
            }
            return required > 0 ? List.of(required) : null;
        }
    }

    public class EnergyContainerCap implements ICapabilityProviderTrait<IEnergyContainer> {
        @Override
        public IO getCapabilityIO(@Nullable Direction side) {
            return GTMEnergyCapabilityTrait.this.getCapabilityIO(side);
        }

        @Override
        public Capability<IEnergyContainer> getCapability() {
            return GTCapability.CAPABILITY_ENERGY_CONTAINER;
        }

        @Override
        public IEnergyContainer getCapContent(IO capbilityIO) {
            return new EnergyContainerWrapper(container, capbilityIO);
        }

        @Override
        public IEnergyContainer mergeContents(List<IEnergyContainer> contents) {
            return new EnergyContainerList(contents);
        }

    }
}
