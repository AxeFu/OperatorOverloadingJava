package ru.axefu.overloading.test.pack;

import com.testlib.Num;
import ru.axefu.overloading.annotation.Operator;
import ru.axefu.overloading.annotation.OperatorType;

/**
 * Вектор с перегруженными операторами
 *
 * @author Artem Moshkin
 * @since 15.10.2024
 */
public class Vector3 {

    public final double x, y, z;

    public Vector3() {
        this(0,0,0);
    }

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Operator(OperatorType.PLUS)
    public Vector3 plus(Vector3 other) {
        return new Vector3(this.x + other.x, this.y + other.y, this.z + other.z);
    }

    @Operator(OperatorType.PLUS)
    public Num plus(Num other) {
        return new Num(x) + other;
    }

    @Operator(OperatorType.MINUS)
    public Vector3 minus(Vector3 other) {
        return new Vector3(this.x - other.x, this.y - other.y, this.z - other.z);
    }

    @Operator(OperatorType.MULTIPLY)
    public Vector3 multiply(double value) {
        return new Vector3(this.x * value, this.y * value, this.z * value);
    }

    @Operator(OperatorType.MULTIPLY)
    public Vector3 multiply(Vector3 other) {
        return new Vector3(
            this.y * other.z - other.y * this.z,
            this.x * other.z - other.x * this.z,
            this.x * other.y - other.x * this.y
        );
    }

    @Operator(OperatorType.DIVIDE)
    public Vector3 divide(float value) {
        return new Vector3(this.x / value, this.y / value, this.z / value);
    }

    @Override
    public String toString() {
        return "x: " + this.x + ", y: " + this.y + ", z: " + this.z;
    }
}
