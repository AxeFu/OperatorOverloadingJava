package ru.axefu.overloading.annotation.processing;

/**
 * Типы полей
 *
 * @author Artem Moshkin
 * @since 19.10.2024
 */
enum FieldType {
    METHOD("method"),
    VARIABLE("variable");

    private final String value;
    FieldType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
