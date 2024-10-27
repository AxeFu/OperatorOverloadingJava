package ru.axefu.reflection;

import javax.annotation.processing.ProcessingEnvironment;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Класс с базовыми методами рефлексии
 */
@SuppressWarnings({"unchecked", "unused"})
public class Reflection {

    /**
     * Установить поле
     *
     * @param object объект, у которого есть искомое поле
     * @param name имя поля
     * @param value новое значение поля
     */
    public static <T> void set(T object, String name, Object value) throws ReflectiveOperationException {
        set(object, (Class<T>) object.getClass(), name, value);
    }

    /**
     * Установить поле
     *
     * @param object объект, у которого есть искомое поле
     * @param clazz класс объекта
     * @param name имя поля
     * @param value новое значение поля
     */
    public static <T> void set(T object, Class<T> clazz, String name, Object value) throws ReflectiveOperationException {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    /**
     * Получить значение поля
     *
     * @param object объект, у которого есть искомое поле
     * @param name имя поля
     * @return значение поля
     */
    public static <R, A> R get(A object, String name) throws ReflectiveOperationException {
        return get(object, (Class<A>) object.getClass(), name);
    }

    /**
     * Получить значение поля
     *
     * @param object объект, у которого есть искомое поле
     * @param clazz класс объекта
     * @param name имя поля
     * @return значение поля
     */
    public static <R, A> R get(A object, Class<A> clazz, String name) throws ReflectiveOperationException {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return (R) field.get(object);
    }

    /**
     * Вызвать статический метод
     *
     * @param clazz класс, в котором есть метод
     * @param name имя метода
     * @param args значение аргументов метода
     * @return результат метода
     */
    public static <R, A> R invoke(Class<A> clazz, String name, Object... args) throws ReflectiveOperationException {
        return invoke(null, clazz, name, args);
    }

    /**
     * Вызвать статический метод
     *
     * @param clazz класс, в котором есть метод
     * @param name имя метода
     * @param argTypes типы аргументов
     * @param args значение аргументов метода
     * @return результат метода
     */
    public static <R, A> R invoke(Class<A> clazz, String name, Class<?>[] argTypes, Object... args) throws ReflectiveOperationException {
        return invoke(null, clazz, name, argTypes, args);
    }

    /**
     * Вызвать метод
     *
     * @param object объект с методом
     * @param name имя метода
     * @param args аргументы метода
     * @return результат метода
     */
    public static <R, A> R invoke(A object, String name, Object... args) throws ReflectiveOperationException {
        return invoke(object, (Class<A>) object.getClass(), name, args);
    }

    /**
     * Вызвать метод
     *
     * @param object объект с методом
     * @param clazz класс объекта
     * @param name имя метода
     * @param args аргументы метода
     * @return результат метода
     */
    public static <R, A> R invoke(A object, Class<A> clazz, String name, Object... args) throws ReflectiveOperationException {
        return invoke(object, clazz, name, Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new), args);
    }

    /**
     * Вызвать метод
     *
     * @param object объект с методом
     * @param clazz класс объекта
     * @param name имя метода
     * @param argTypes типы аргументов метода
     * @param args аргументы метода
     * @return результат метода
     */
    public static <R, A> R invoke(A object, Class<A> clazz, String name, Class<?>[] argTypes, Object... args) throws ReflectiveOperationException {
        Method method = clazz.getDeclaredMethod(name, argTypes);
        method.setAccessible(true);
        return (R) method.invoke(object, args);
    }

    /**
     * Unwrap для ProcessingEnvironment во время annotation preprocessing для IntellijIDEA
     *
     * @param wrapper - ProcessingEnvironment
     * @return JavacProcessingEnvironment
     */
    public static <T> T unwrap(T wrapper) {
        T unwrapped = null;
        try {
            final Class<?> apiWrappers = wrapper.getClass().getClassLoader().loadClass("org.jetbrains.jps.javac.APIWrappers");
            final Method unwrapMethod = apiWrappers.getDeclaredMethod("unwrap", Class.class, Object.class);
            unwrapped = (T) unwrapMethod.invoke(null, ProcessingEnvironment.class, wrapper);
        }
        catch (Throwable ignored) {}
        return unwrapped != null? unwrapped : wrapper;
    }

    /**
     * Бросает проверяемое исключение без необходимости его обрабатывать
     */
    public static <T extends Throwable> void sneakyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
