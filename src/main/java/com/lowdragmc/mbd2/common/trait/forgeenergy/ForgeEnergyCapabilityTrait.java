package com.lowdragmc.mbd2.common.trait.forgeenergy;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.api.capability.recipe.IRecipeHandlerTrait;
import com.lowdragmc.mbd2.api.recipe.MBDRecipe;
import com.lowdragmc.mbd2.common.capability.recipe.ForgeEnergyRecipeCapability;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.mbd2.common.trait.RecipeHandlerTrait;
import com.lowdragmc.mbd2.common.trait.SimpleCapabilityTrait;
import lombok.Getter;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ForgeEnergyCapabilityTrait extends SimpleCapabilityTrait<IEnergyStorage> {
    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ForgeEnergyCapabilityTrait.class);
    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Persisted
    @DescSynced
    public final CopiableEnergyStorage storage;
    private final ForgeEnergyRecipeHandler recipeHandler = new ForgeEnergyRecipeHandler();

    public ForgeEnergyCapabilityTrait(MBDMachine machine, ForgeEnergyCapabilityTraitDefinition definition) {
        super(machine, definition);
        storage = createStorages();
        storage.setOnContentsChanged(this::notifyListeners);
    }

    @Override
    public ForgeEnergyCapabilityTraitDefinition getDefinition() {
        return (ForgeEnergyCapabilityTraitDefinition) super.getDefinition();
    }

    @Override
    public void onLoadingTraitInPreview() {
        storage.receiveEnergy(getDefinition().getCapacity() / 2, false);
    }

    protected CopiableEnergyStorage createStorages() {
        return new CopiableEnergyStorage(getDefinition().getCapacity());
    }

    @Override
    public IEnergyStorage getCapContent(IO capbilityIO) {
        return new EnergyStorageWrapper(this.storage, capbilityIO, getDefinition().getMaxReceive(), getDefinition().getMaxExtract());
    }

    @Override
    public IEnergyStorage mergeContents(List<IEnergyStorage> contents) {
        return new EnergyStorageList(contents.toArray(new IEnergyStorage[0]));
    }

    @Override
    public List<IRecipeHandlerTrait<?>> getRecipeHandlerTraits() {
        return List.of(recipeHandler);
    }

    public class ForgeEnergyRecipeHandler extends RecipeHandlerTrait<Integer> {
        protected ForgeEnergyRecipeHandler() {
            super(ForgeEnergyCapabilityTrait.this, ForgeEnergyRecipeCapability.CAP);
        }

        @Override
        public List<Integer> handleRecipeInner(IO io, MBDRecipe recipe, List<Integer> left, @Nullable String slotName, boolean simulate) {
            if (io != getHandlerIO()) return left;
            int required = left.stream().reduce(0, Integer::sum);
            var capability = simulate ? storage.copy() : storage;
            if (io == IO.IN) {
                var extracted = capability.extractEnergy(required, simulate);
                required -= extracted;
            } else {
                var received = capability.receiveEnergy(required, simulate);
                required -= received;
            }
            return required > 0 ? List.of(required) : null;
        }
    }
}
