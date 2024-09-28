package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.lowdraglib.syncdata.managed.MultiManagedStorage;
import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.blockentity.IMachineBlockEntity;
import com.lowdragmc.mbd2.api.capability.MBDCapabilities;
import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticEffectHandler;
import com.simibubi.create.foundation.utility.Lang;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public class MBDKineticMachineBlockEntity extends KineticBlockEntity implements IMachineBlockEntity {
    @Getter
    public final MultiManagedStorage rootStorage = new MultiManagedStorage();
    @Getter
    private final long offset = MBD2.RND.nextLong();
    @Getter
    private IMachine metaMachine;
    public float workingSpeed;
    public boolean reActivateSource;
    public CreateKineticMachineDefinition definition;
    public MBDKineticMachineBlockEntity(CreateKineticMachineDefinition definition, BlockEntityType<?> type, BlockPos pos, BlockState blockState, Function<IMachineBlockEntity, IMachine> machineFactory) {
        super(type, pos, blockState);
        this.setMachine(machineFactory.apply(this));
        this.definition = definition;
    }

    public void setMachine(IMachine newMachine) {
        if (metaMachine == newMachine) return;
        if (metaMachine != null && level != null && !level.isClientSide) {
            metaMachine.onUnload();
        }
        if (metaMachine instanceof MBDMachine machine) {
            machine.detach();
        }
        metaMachine = newMachine;
        if (newMachine instanceof MBDMachine machine) {
            machine.getAdditionalTraits().add(CreateStressTrait.DEFINITION.createTrait(machine));
            machine.initCapabilitiesProxy();
        }
    }


    @Override
    public void invalidate() {
        super.invalidate();
        metaMachine.onUnload();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        getMetaMachine().onLoad();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == MBDCapabilities.CAPABILITY_MACHINE) {
            return MBDCapabilities.CAPABILITY_MACHINE.orEmpty(cap, LazyOptional.of(this::getMetaMachine));
        }
        if (metaMachine instanceof MBDMachine machine) {
            return machine.getCapability(cap, side);
        }
        return super.getCapability(cap, side);
    }

    @Override
    public AABB getRenderBoundingBox() {
        if (metaMachine instanceof MBDMachine machine) {
            var value = machine.getRenderBoundingBox();
            if (value != null) {
                return value;
            }
        }
        return super.getRenderBoundingBox();
    }

    //////////////////////////////////////
    // ********* Create *********//
    //////////////////////////////////////

    public KineticEffectHandler getEffects() {
        return effects;
    }

    /**
     * Get the max stress generated by this machine.
     * if the machine is not a generator, it will return 0
     */
    public float scheduleWorking(float stress, boolean simulate) {
        if (definition.kineticMachineSettings.isGenerator) {
            var capacity = definition.kineticMachineSettings.getCapacity();
            float speed = Math.min(definition.kineticMachineSettings.maxRPM, stress / capacity);
            if (!simulate) {
                workingSpeed = speed;
                updateGeneratedRotation();
            }
            return speed * capacity;
        }
        return 0;
    }

    public void scheduleWorking(float su) {
        scheduleWorking(su, false);
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (!level.isClientSide && !definition.kineticMachineSettings.isGenerator) {
            if (speed > definition.kineticMachineSettings.maxRPM) {
                // over speed
                this.level.destroyBlock(this.worldPosition, true);
            }
        }
    }

    public void stopWorking() {
        if (definition.kineticMachineSettings.isGenerator && getGeneratedSpeed() != 0) {
            workingSpeed = 0;
            updateGeneratedRotation();
        }
    }

    @Override
    public float getGeneratedSpeed() {
        return workingSpeed;
    }

    protected void notifyStressCapacityChange(float capacity) {
        this.getOrCreateNetwork().updateCapacityFor(this, capacity);
    }

    public void removeSource() {
        if (definition.kineticMachineSettings.isGenerator && this.hasSource() && this.isSource()) {
            this.reActivateSource = true;
        }
        super.removeSource();
    }

    public void setSource(BlockPos source) {
        super.setSource(source);
        if (!definition.kineticMachineSettings.isGenerator) return;
        if (this.level.getBlockEntity(source) instanceof KineticBlockEntity sourceTe) {
            if (this.reActivateSource && Math.abs(sourceTe.getSpeed()) >= Math.abs(this.getGeneratedSpeed())) {
                this.reActivateSource = false;
            }
        }
    }

    public void tick() {
        super.tick();
        if (definition.kineticMachineSettings.isGenerator && this.reActivateSource) {
            this.updateGeneratedRotation();
            this.reActivateSource = false;
        }
    }

    @Override
    public float calculateStressApplied() {
        float impact = definition.kineticMachineSettings.getImpact();
        this.lastStressApplied = impact;
        return impact;
    }

    @Override
    public float calculateAddedStressCapacity() {
        float capacity = definition.kineticMachineSettings.getCapacity();
        this.lastCapacityProvided = capacity;
        return capacity;
    }

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        float stressBase = this.calculateAddedStressCapacity();
        if (stressBase != 0.0F && IRotate.StressImpact.isEnabled()) {
            Lang.translate("gui.goggles.generator_stats").forGoggles(tooltip);
            Lang.translate("tooltip.capacityProvided").style(ChatFormatting.GRAY).forGoggles(tooltip);
            float speed = this.getTheoreticalSpeed();
            if (speed != this.getGeneratedSpeed() && speed != 0.0F) {
                stressBase *= this.getGeneratedSpeed() / speed;
            }

            speed = Math.abs(speed);
            float stressTotal = stressBase * speed;
            Lang.number(stressTotal).translate("generic.unit.stress").style(ChatFormatting.AQUA).space()
                    .add(Lang.translate("gui.goggles.at_current_speed").style(ChatFormatting.DARK_GRAY))
                    .forGoggles(tooltip, 1);
            added = true;
        }

        return added;
    }

    public void updateGeneratedRotation() {
        if (!definition.kineticMachineSettings.isGenerator) return;
        float speed = this.getGeneratedSpeed();
        float prevSpeed = this.speed;
        if (!this.level.isClientSide) {
            if (prevSpeed != speed) {
                if (!this.hasSource()) {
                    IRotate.SpeedLevel levelBefore = IRotate.SpeedLevel.of(this.speed);
                    IRotate.SpeedLevel levelafter = IRotate.SpeedLevel.of(speed);
                    if (levelBefore != levelafter) {
                        this.effects.queueRotationIndicators();
                    }
                }

                this.applyNewSpeed(prevSpeed, speed);
            }

            if (this.hasNetwork() && speed != 0.0F) {
                KineticNetwork network = this.getOrCreateNetwork();
                this.notifyStressCapacityChange(this.calculateAddedStressCapacity());
                this.getOrCreateNetwork().updateStressFor(this, this.calculateStressApplied());
                network.updateStress();
            }

            this.onSpeedChanged(prevSpeed);
            this.sendData();
        }
    }

    public void applyNewSpeed(float prevSpeed, float speed) {
        if (speed == 0.0F) {
            if (this.hasSource()) {
                this.notifyStressCapacityChange(0.0F);
                this.getOrCreateNetwork().updateStressFor(this, this.calculateStressApplied());
            } else {
                this.detachKinetics();
                this.setSpeed(0.0F);
                this.setNetwork(null);
            }
        } else if (prevSpeed == 0.0F) {
            this.setSpeed(speed);
            this.setNetwork(this.createNetworkId());
            this.attachKinetics();
        } else if (this.hasSource()) {
            if (Math.abs(prevSpeed) >= Math.abs(speed)) {
                if (Math.signum(prevSpeed) != Math.signum(speed)) {
                    this.level.destroyBlock(this.worldPosition, true);
                }
            } else {
                this.detachKinetics();
                this.setSpeed(speed);
                this.source = null;
                this.setNetwork(this.createNetworkId());
                this.attachKinetics();
            }
        } else {
            this.detachKinetics();
            this.setSpeed(speed);
            this.attachKinetics();
        }
    }

    public Long createNetworkId() {
        return this.worldPosition.asLong();
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putFloat("workingSpeed", workingSpeed);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        workingSpeed = compound.contains("workingSpeed") ? compound.getFloat("workingSpeed") : 0;
    }

}
