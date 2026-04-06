package com.overworldlabs.plots.bridge.mixin;

import com.hypixel.hytale.protocol.MovementStates;
import java.lang.reflect.Field;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "com.hypixel.hytale.builtin.mounts.MountSystems$HandleMountInput")
public abstract class MountHandleInputFlyingMixin {

    @ModifyArg(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/hypixel/hytale/server/core/entity/movement/MovementStatesComponent;setMovementStates(Lcom/hypixel/hytale/protocol/MovementStates;)V"
            ),
            index = 0,
            require = 0
    )
    private MovementStates plots$forceFlyingWhileMounted(MovementStates states) {
        if (states != null && plots$isHorseMountContext()) {
            boolean jumpInput = plots$readBooleanHint("jump", "ascend", "up", "space");
            boolean sneakInput = plots$readBooleanHint("sneak", "crouch", "descend", "down");
            states.flying = true;
            states.onGround = false;
            if (jumpInput) {
                states.jumping = true;
                states.idle = false;
                states.horizontalIdle = false;
            }
            if (sneakInput) {
                states.crouching = true;
                states.idle = false;
            }
        }
        return states;
    }

    private boolean plots$isHorseMountContext() {
        Class<?> type = this.getClass();
        while (type != null && type != Object.class) {
            Field[] fields = type.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value;
                try {
                    value = field.get(this);
                } catch (IllegalAccessException ignored) {
                    continue;
                }
                if (value == null) {
                    continue;
                }

                if (value instanceof Enum<?>) {
                    String enumName = ((Enum<?>) value).name();
                    if (enumName != null && enumName.toLowerCase().contains("horse")) {
                        return true;
                    }
                }

                String fieldName = field.getName();
                String text = String.valueOf(value);
                if (fieldName != null && fieldName.toLowerCase().contains("mount")
                        && text.toLowerCase().contains("horse")) {
                    return true;
                }
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private boolean plots$readBooleanHint(String... hints) {
        Class<?> type = this.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (!(fieldType == boolean.class || fieldType == Boolean.class)) {
                    continue;
                }
                String fieldName = field.getName();
                if (fieldName == null) {
                    continue;
                }
                String lower = fieldName.toLowerCase();
                boolean nameMatch = false;
                for (String hint : hints) {
                    if (lower.contains(hint)) {
                        nameMatch = true;
                        break;
                    }
                }
                if (!nameMatch) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(this);
                    if (value instanceof Boolean b && b) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return false;
    }
}
