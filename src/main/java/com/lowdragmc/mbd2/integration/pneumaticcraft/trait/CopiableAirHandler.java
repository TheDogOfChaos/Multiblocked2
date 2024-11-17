package com.lowdragmc.mbd2.integration.pneumaticcraft.trait;

import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.desht.pneumaticcraft.api.pressure.PressureTier;
import me.desht.pneumaticcraft.common.capabilities.MachineAirHandler;
import net.minecraft.nbt.CompoundTag;

@Getter
public class CopiableAirHandler extends MachineAirHandler implements ITagSerializable<CompoundTag>, IContentChangeAware {
    @Setter
    public Runnable onContentsChanged = () -> {};
    @Accessors(fluent = true)
    protected final float maxPressure;
    private final PressureTier tier;

    public CopiableAirHandler(PressureTier tier, int baseVolume, float maxPressure) {
        this(tier, baseVolume, 0, maxPressure);
    }

    public CopiableAirHandler(PressureTier tier, int baseVolume, float pressure, float maxPressure) {
        super(tier, baseVolume);
        this.tier = tier;
        this.maxPressure = maxPressure;
        super.setPressure(pressure);
    }

    public CopiableAirHandler copy() {
        return new CopiableAirHandler(tier, getBaseVolume(), getPressure(), maxPressure);
    }

    @Override
    public void addAir(int amount) {
        super.addAir(amount);
        if (amount != 0) {
            onContentsChanged.run();
        }
    }

    @Override
    public void setBaseVolume(int newBaseVolume) {
        if (newBaseVolume != getBaseVolume()) {
            super.setBaseVolume(newBaseVolume);
            onContentsChanged.run();
        }
    }

}
