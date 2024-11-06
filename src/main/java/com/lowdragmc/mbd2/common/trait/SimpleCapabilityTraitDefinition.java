package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

@Getter @Setter
public abstract class SimpleCapabilityTraitDefinition extends RecipeCapabilityTraitDefinition implements IUIProviderTrait {
    @Getter @Setter
    public static class CapabilityIO {
        @Configurable(name = "config.definition.trait.capability_io.internal", tips = "config.definition.trait.capability_io.internal.tooltip")
        private IO internal = IO.BOTH;
        @Configurable(name = "config.definition.trait.capability_io.front")
        private IO frontIO = IO.BOTH;
        @Configurable(name = "config.definition.trait.capability_io.back")
        private IO backIO = IO.BOTH;
        @Configurable(name = "config.definition.trait.capability_io.left")
        private IO leftIO = IO.BOTH;
        @Configurable(name = "config.definition.trait.capability_io.right")
        private IO rightIO = IO.BOTH;
        @Configurable(name = "config.definition.trait.capability_io.top")
        private IO topIO = IO.BOTH;
        @Configurable(name = "config.definition.trait.capability_io.bottom")
        private IO bottomIO = IO.BOTH;

        public IO getIO(Direction front, @Nullable Direction side) {
            if (side == null || front.getAxis() == Direction.Axis.Y) {
                return internal;
            }
            if (side == Direction.UP) {
                return topIO;
            } else if (side == Direction.DOWN) {
                return bottomIO;
            } else if (side == front) {
                return frontIO;
            } else if (side == front.getOpposite()) {
                return backIO;
            } else if (side == front.getClockWise()) {
                return rightIO;
            } else if (side == front.getCounterClockWise()) {
                return leftIO;
            }
            return IO.NONE;
        }
    }

    @Configurable(name = "config.definition.trait.capability_io", subConfigurable = true,
            tips = {"config.definition.trait.capability_io.tooltip.0", "config.definition.trait.capability_io.tooltip.1"})
    private final CapabilityIO capabilityIO = new CapabilityIO();

    @Configurable(name = "config.definition.trait.gui_io", tips = "config.definition.trait.gui_io.tooltip")
    private IO guiIO = IO.BOTH;

    @Override
    public abstract SimpleCapabilityTrait createTrait(MBDMachine machine);

}
