package com.naturalinteraction.LEA.capture;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

final class LeaReflect {

    static final Class<?>[] NO_TYPES = new Class<?>[0];
    static final Object[] NO_ARGS = new Object[0];

    private LeaReflect() {
    }

    static Class<?> find(String className) {
        try {
            return Class.forName(className, false, LeaReflect.class.getClassLoader());
        } catch (Throwable t) {
            return null;
        }
    }

    static Object construct(Class<?> owner, Class<?>[] types, Object[] args) throws Exception {
        Constructor<?> constructor = owner.getConstructor(types);
        constructor.setAccessible(true);
        return constructor.newInstance(args);
    }

    static Object call(Object target, String name, Class<?>[] types, Object[] args) throws Exception {
        Method method = target.getClass().getMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    static Object callQuietly(Object target, String name, Class<?>[] types, Object[] args) {
        try {
            return call(target, name, types, args);
        } catch (Throwable t) {
            return null;
        }
    }

    static Object callStatic(Class<?> owner, String name, Class<?>[] types, Object[] args) throws Exception {
        Method method = owner.getMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    static Object field(Class<?> owner, String name) {
        try {
            Field field = owner.getField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    static String describe(Throwable throwable) {
        if (throwable == null) {
            return "unknown error";
        }
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isEmpty()) {
            return cause.getClass().getSimpleName();
        }
        return cause.getClass().getSimpleName() + ": " + message;
    }
}
