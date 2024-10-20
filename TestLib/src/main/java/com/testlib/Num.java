package com.testlib;

import ru.axefu.overloading.annotation.Operator;
import ru.axefu.overloading.annotation.OperatorType;

/**
 * Скомпилированный класс играет роль сторонней библиотеки использующей аннотацию Operator
 *
 * @author Artem Moshkin
 * @since 19.10.2024
 */
public class Num {

    private final double value;

    public Num(double value) {
        this.value = value;
    }

    @Operator(OperatorType.PLUS)
    public Num plus(Num other) {
        return new Num(this.value + other.value);
    }

    @Operator(OperatorType.MINUS)
    public Num minus(Num other) {
        return new Num(this.value - other.value);
    }

    @Operator(OperatorType.MULTIPLY)
    public Num multiply(Num other) {
        return new Num(this.value * other.value);
    }

    @Operator(OperatorType.DIVIDE)
    public Num divide(Num other) {
        return new Num(this.value / other.value);
    }

    @Override
    public String toString() {
        return value + "";
    }
}
