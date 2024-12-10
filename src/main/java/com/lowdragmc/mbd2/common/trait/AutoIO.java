package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.mbd2.api.capability.recipe.IO;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;

@Getter
@Setter
public class AutoIO {
    @Configurable(name = "config.definition.trait.capability_io.front")
    @ConfigSelector(candidate = {"IN", "OUT", "NONE"})
    private IO frontIO = IO.NONE;

    @Configurable(name = "config.definition.trait.capability_io.back")
    @ConfigSelector(candidate = {"IN", "OUT", "NONE"})
    private IO backIO = IO.NONE;

    @Configurable(name = "config.definition.trait.capability_io.left")
    @ConfigSelector(candidate = {"IN", "OUT", "NONE"})
    private IO leftIO = IO.NONE;

    @Configurable(name = "config.definition.trait.capability_io.right")
    @ConfigSelector(candidate = {"IN", "OUT", "NONE"})
    private IO rightIO = IO.NONE;

    @Configurable(name = "config.definition.trait.capability_io.top")
    @ConfigSelector(candidate = {"IN", "OUT", "NONE"})
    private IO topIO = IO.NONE;

    @Configurable(name = "config.definition.trait.capability_io.bottom")
    @ConfigSelector(candidate = {"IN", "OUT", "NONE"})
    private IO bottomIO = IO.NONE;

    @Configurable(name = "config.definition.trait.auto_io.interval", tips = "config.definition.trait.auto_io.interval.tooltip")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    private int interval = 20;

    public IO getIO(Direction front, @Nullable Direction side) {
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
