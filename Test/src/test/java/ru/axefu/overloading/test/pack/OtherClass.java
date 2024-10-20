package ru.axefu.overloading.test.pack;

/**
 * Класс со статическими переменными
 *
 * @author Artem Moshkin
 * @since 16.10.2024
 */
public class OtherClass {

    public static Vector3 a = new Vector3(-10, -10, -10);

    public static Vector3 f() {
        return a;
    }
}
