package com.viaden.sdk;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

class Reflection {

    static class MethodBuilder {
        @Nullable
        private final Object instance;
        @NonNull
        private final String methodName;
        @NonNull
        private final List<Class<?>> parameterClasses;
        @NonNull
        private final List<Object> parameters;
        @Nullable
        private Class<?> clazz;
        private boolean isAccessible;
        private boolean isStatic;

        MethodBuilder(@Nullable final Object instance, @NonNull final String methodName) {
            this.instance = instance;
            this.methodName = methodName;

            parameterClasses = new ArrayList<>();
            parameters = new ArrayList<>();

            clazz = (instance != null) ? instance.getClass() : null;
        }

        @NonNull
        <T> MethodBuilder addParam(@NonNull final Class<T> clazz, @NonNull final T parameter) {
            parameterClasses.add(clazz);
            parameters.add(parameter);

            return this;
        }

        @NonNull
        MethodBuilder setStatic(@Nullable final Class<?> clazz) {
            isStatic = true;
            this.clazz = clazz;
            return this;
        }

        @NonNull
        MethodBuilder setAccessible() {
            isAccessible = true;
            return this;
        }

        @NonNull
        private Method getDeclaredMethod() throws NoSuchMethodException {
            final Class<?>[] classArray = new Class<?>[parameterClasses.size()];
            final Class<?>[] parameterTypes = parameterClasses.toArray(classArray);

            Class<?> currentClass = clazz;
            while (currentClass != null) {
                try {
                    return currentClass.getDeclaredMethod(methodName, parameterTypes);
                } catch (@NonNull final NoSuchMethodException e) {
                    currentClass = currentClass.getSuperclass();
                }
            }
            throw new NoSuchMethodException();
        }

        @NonNull
        Object execute() throws Exception {
            final Method method = getDeclaredMethod();
            if (isAccessible) {
                method.setAccessible(true);
            }
            return method.invoke(isStatic ? null : this.instance, parameters.toArray());
        }
    }
}
