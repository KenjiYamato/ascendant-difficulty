package ascendant.core.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflectionHelper {
    private static final ConcurrentHashMap<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();

    private static final ClassValue<ConcurrentHashMap<String, Method>> NO_ARG_METHOD_CACHE = new ClassValue<>() {
        @Override
        protected ConcurrentHashMap<String, Method> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    private static final ClassValue<ConcurrentHashMap<MethodKey, Method>> METHOD_CACHE = new ClassValue<>() {
        @Override
        protected ConcurrentHashMap<MethodKey, Method> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    private static final ClassValue<ConcurrentHashMap<MethodKey, Method>> DECLARED_METHOD_CACHE = new ClassValue<>() {
        @Override
        protected ConcurrentHashMap<MethodKey, Method> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    private static final ClassValue<ConcurrentHashMap<String, Field>> FIELD_CACHE = new ClassValue<>() {
        @Override
        protected ConcurrentHashMap<String, Field> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    private static final ClassValue<ConcurrentHashMap<String, Field>> PUBLIC_FIELD_CACHE = new ClassValue<>() {
        @Override
        protected ConcurrentHashMap<String, Field> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    private static final ClassValue<ConcurrentHashMap<ConstructorKey, Constructor<?>>> CONSTRUCTOR_CACHE = new ClassValue<>() {
        @Override
        protected ConcurrentHashMap<ConstructorKey, Constructor<?>> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    private static final ClassValue<Field[]> INSTANCE_FIELDS_CACHE = new ClassValue<>() {
        @Override
        protected Field[] computeValue(Class<?> type) {
            ArrayList<Field> out = new ArrayList<>(32);
            Class<?> x = type;
            while (x != null && x != Object.class) {
                for (Field f : x.getDeclaredFields()) {
                    int mod = f.getModifiers();
                    if ((mod & Modifier.STATIC) != 0 || f.getType().isPrimitive()) {
                        continue;
                    }
                    try {
                        f.setAccessible(true);
                    } catch (Throwable ignored) {
                    }
                    out.add(f);
                }
                x = x.getSuperclass();
            }
            return out.toArray(Field[]::new);
        }
    };

    private ReflectionHelper() {
    }

    @Nullable
    public static Class<?> resolveClass(@Nonnull String name, @Nullable ClassLoader loader) {
        try {
            Class<?> c = CLASS_CACHE.get(name);
            if (c != null) {
                return c;
            }
            ClassLoader useLoader = loader != null ? loader : ReflectionHelper.class.getClassLoader();
            c = Class.forName(name, false, useLoader);
            Class<?> prev = CLASS_CACHE.putIfAbsent(name, c);
            return prev != null ? prev : c;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nonnull
    public static Class<?> resolveClassOrThrow(@Nonnull String name, @Nullable ClassLoader loader) throws ClassNotFoundException {
        Class<?> c = CLASS_CACHE.get(name);
        if (c != null) {
            return c;
        }
        ClassLoader useLoader = loader != null ? loader : ReflectionHelper.class.getClassLoader();
        c = Class.forName(name, false, useLoader);
        Class<?> prev = CLASS_CACHE.putIfAbsent(name, c);
        return prev != null ? prev : c;
    }

    @Nullable
    public static Class<?> resolveClass(@Nonnull String name) {
        return resolveClass(name, ReflectionHelper.class.getClassLoader());
    }

    @Nullable
    public static Method getMethod(@Nonnull Class<?> owner, @Nonnull String name, Class<?>... params) {
        try {
            MethodKey key = new MethodKey(name, params);
            ConcurrentHashMap<MethodKey, Method> map = METHOD_CACHE.get(owner);
            Method m = map.get(key);
            if (m != null) {
                return m;
            }
            m = owner.getMethod(name, params);
            m.setAccessible(true);
            Method prev = map.putIfAbsent(key, m);
            return prev != null ? prev : m;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static Method getDeclaredMethod(@Nonnull Class<?> owner, @Nonnull String name, Class<?>... params) {
        try {
            MethodKey key = new MethodKey(name, params);
            ConcurrentHashMap<MethodKey, Method> map = DECLARED_METHOD_CACHE.get(owner);
            Method m = map.get(key);
            if (m != null) {
                return m;
            }
            m = owner.getDeclaredMethod(name, params);
            m.setAccessible(true);
            Method prev = map.putIfAbsent(key, m);
            return prev != null ? prev : m;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static Method getAnyMethod(@Nonnull Class<?> owner, @Nonnull String name, Class<?>... params) {
        Method m = getDeclaredMethod(owner, name, params);
        return m != null ? m : getMethod(owner, name, params);
    }

    @Nullable
    public static Method getNoArgMethod(@Nonnull Class<?> owner, @Nonnull String name) {
        try {
            ConcurrentHashMap<String, Method> map = NO_ARG_METHOD_CACHE.get(owner);
            Method m = map.get(name);
            if (m != null) {
                return m;
            }
            m = getMethod(owner, name);
            if (m == null) {
                return null;
            }
            Method prev = map.putIfAbsent(name, m);
            return prev != null ? prev : m;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static Object invokeNoArgs(@Nullable Object target, @Nonnull String method) {
        if (target == null) {
            return null;
        }
        try {
            Method m = getNoArgMethod(target.getClass(), method);
            return m == null ? null : m.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static Field getDeclaredField(@Nonnull Class<?> owner, @Nonnull String name) {
        try {
            ConcurrentHashMap<String, Field> map = FIELD_CACHE.get(owner);
            Field f = map.get(name);
            if (f != null) {
                return f;
            }
            f = owner.getDeclaredField(name);
            f.setAccessible(true);
            Field prev = map.putIfAbsent(name, f);
            return prev != null ? prev : f;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static Field getField(@Nonnull Class<?> owner, @Nonnull String name) {
        try {
            ConcurrentHashMap<String, Field> map = PUBLIC_FIELD_CACHE.get(owner);
            Field f = map.get(name);
            if (f != null) {
                return f;
            }
            f = owner.getField(name);
            f.setAccessible(true);
            Field prev = map.putIfAbsent(name, f);
            return prev != null ? prev : f;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static Constructor<?> getConstructor(@Nonnull Class<?> owner, Class<?>... params) {
        try {
            ConstructorKey key = new ConstructorKey(params);
            ConcurrentHashMap<ConstructorKey, Constructor<?>> map = CONSTRUCTOR_CACHE.get(owner);
            Constructor<?> ctor = map.get(key);
            if (ctor != null) {
                return ctor;
            }
            ctor = owner.getConstructor(params);
            ctor.setAccessible(true);
            Constructor<?> prev = map.putIfAbsent(key, ctor);
            return prev != null ? prev : ctor;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static Object getFieldValue(@Nullable Field field, @Nullable Object target) {
        if (field == null) {
            return null;
        }
        try {
            return field.get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean setField(@Nonnull Object target, @Nonnull String name, Object value) {
        Field f = getDeclaredField(target.getClass(), name);
        if (f == null) {
            return false;
        }
        try {
            f.set(target, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nonnull
    public static Field[] getInstanceFields(@Nonnull Class<?> type) {
        return INSTANCE_FIELDS_CACHE.get(type);
    }

    private record MethodKey(String name, List<Class<?>> params) {
        private MethodKey(String name, Class<?>... params) {
            this(name, params == null || params.length == 0 ? List.of() : List.of(params));
        }
    }

    private record ConstructorKey(List<Class<?>> params) {
        private ConstructorKey(Class<?>... params) {
            this(params == null || params.length == 0 ? List.of() : List.of(params));
        }
    }
}
