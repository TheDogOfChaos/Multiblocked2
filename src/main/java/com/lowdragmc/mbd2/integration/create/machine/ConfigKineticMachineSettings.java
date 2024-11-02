package com.lowdragmc.mbd2.integration.create.machine;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.core.Direction;

@Getter
@Builder
@Accessors(fluent = true)
public class ConfigKineticMachineSettings implements IConfigurable, IPersistedSerializable {
    public enum RotationFacing {
        FRONT,
        BACK,
        LEFT,
        RIGHT,
        UP,
        DOWN;

        public Direction getDirection(Direction frontFacing) {
            if (frontFacing.getAxis() == Direction.Axis.Y) {
                return switch (this) {
                    case FRONT -> frontFacing;
                    case BACK -> frontFacing.getOpposite();
                    case LEFT -> Direction.WEST;
                    case RIGHT -> Direction.EAST;
                    case UP -> Direction.NORTH;
                    case DOWN -> Direction.SOUTH;
                };
            }
            return switch (this) {
                case FRONT -> frontFacing;
                case BACK -> frontFacing.getOpposite();
                case LEFT -> frontFacing.getClockWise();
                case RIGHT -> frontFacing.getCounterClockWise();
                case UP -> Direction.UP;
                case DOWN -> Direction.DOWN;
            };
        }
    }

    @Configurable(name = "config.kinetic_machine.is_generator", tips = "config.kinetic_machine.is_generator.tooltip")
    @Builder.Default
    public boolean isGenerator = false;
    @Configurable(name = "config.kinetic_machine.torque", tips = {"config.kinetic_machine.torque.tooltip.0",
            "config.kinetic_machine.torque.tooltip.1"})
    @Builder.Default
    @NumberRange(range = {0, Float.MAX_VALUE})
    public float torque = 4;
    @Configurable(name = "config.kinetic_machine.front_rotation", tips = "config.kinetic_machine.front_rotation.tooltip")
    @Builder.Default
    public RotationFacing frontRotation = RotationFacing.FRONT;
    @Configurable(name = "config.kinetic_machine.has_back_rotation", tips = {"config.kinetic_machine.has_back_rotation.tooltip.0",
            "config.kinetic_machine.has_back_rotation.tooltip.1"})
    @Builder.Default
    public boolean hasBackRotation = true;
    @Configurable(name = "config.kinetic_machine.max_rpm", tips = {
            "config.kinetic_machine.max_rpm.tooltip.0",
            "config.kinetic_machine.max_rpm.tooltip.1"
    })
    @Builder.Default
    @NumberRange(range = {0, Integer.MAX_VALUE})
    public int maxRPM = 256;
    @Configurable(name = "config.kinetic_machine.use_flywheel", tips = "config.kinetic_machine.use_flywheel.tooltip")
    @Builder.Default
    public boolean useFlywheel = true;

    public enum RenderLayer {
        SOLID,
        CUTOUT,
        TRANSPARENT,
    }

    @Configurable(name = "config.kinetic_machine.flywheel_render_layer", tips = "config.kinetic_machine.flywheel_render_layer.tooltip")
    @Builder.Default
    public RenderLayer renderLayer = RenderLayer.SOLID;

    /**
     * Get the rotation facing based on the machine front facing;
     */
    public Direction getRotationFacing(Direction frontFacing) {
        return frontRotation.getDirection(frontFacing);
    }


    /**
     * Has shaft towards the given face.
     */
    public boolean hasShaftTowards(Direction towardFace, Direction rotationFacing) {
        if (towardFace.getAxis() == rotationFacing.getAxis()) {
            if (!hasBackRotation) {
                return towardFace == rotationFacing;
            }
            return true;
        }
        return false;
    }

    public float getCapacity() {
        return isGenerator ? Math.max(torque, Float.MIN_VALUE) : 0;
    }

    public float getImpact() {
        return isGenerator ? 0 : torque;
    }
}
