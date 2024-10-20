package ru.axefu.overloading.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация, позволяет перегружать бинарные и унарные операторы
 *
 * @author Artem Moshkin
 * @since 15.10.2024
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Operator {
    OperatorType value();
}
