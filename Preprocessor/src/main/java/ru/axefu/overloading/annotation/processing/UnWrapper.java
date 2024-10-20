package ru.axefu.overloading.annotation.processing;

import javax.annotation.processing.ProcessingEnvironment;
import java.lang.reflect.Method;

/**
 * UnWrapper
 *
 * @author Artem Moshkin
 * @since 15.10.2024
 */
class UnWrapper {
    static ProcessingEnvironment get(ProcessingEnvironment wrapper) {
        ProcessingEnvironment unwrapped = null;
        try {
            final Class<?> apiWrappers = wrapper.getClass().getClassLoader().loadClass("org.jetbrains.jps.javac.APIWrappers");
            final Method unwrapMethod = apiWrappers.getDeclaredMethod("unwrap", Class.class, Object.class);
            unwrapped = (ProcessingEnvironment) unwrapMethod.invoke(null, ProcessingEnvironment.class, wrapper);
        }
        catch (Throwable ignored) {}
        return unwrapped != null? unwrapped : wrapper;
    }
}
