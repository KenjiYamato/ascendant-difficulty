package ascendant.core.integration.elitemobs;

import ascendant.core.util.ReflectionHelper;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

public final class OptionalComponentAccess {

    private OptionalComponentAccess() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    public static ComponentType<EntityStore, Component<EntityStore>> getComponentTypeFromStaticField(
            String className,
            String fieldName
    ) {
        try {
            Class<?> c = ReflectionHelper.resolveClass(className, OptionalComponentAccess.class.getClassLoader());
            if (c == null) {
                return null;
            }
            Object v = ReflectionHelper.getFieldValue(ReflectionHelper.getField(c, fieldName), null);
            if (!(v instanceof ComponentType<?, ?>)) {
                return null;
            }
            return (ComponentType) v;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static Component<EntityStore> getComponent(
            Store<EntityStore> store,
            Ref<EntityStore> ref,
            ComponentType<EntityStore, Component<EntityStore>> type
    ) {
        if (store == null || ref == null || type == null) {
            return null;
        }
        return store.getComponent(ref, type);
    }

    public static void addComponent(
            CommandBuffer<EntityStore> commandBuffer,
            Ref<EntityStore> ref,
            ComponentType<EntityStore, Component<EntityStore>> type,
            Component<EntityStore> component
    ) {
        if (commandBuffer == null || ref == null || type == null || component == null) {
            return;
        }
        commandBuffer.addComponent(ref, type, component);
    }
}
