package ru.axefu.overloading.annotation.processing;

import com.sun.tools.javac.tree.JCTree;

/**
 * Дерево для записи локальных переменных
 *
 * @author Artem Moshkin
 * @since 18.10.2024
 */
class Scope {

    private final Scope parent;
    public final JCTree key;
    public final int level;

    public Scope(JCTree key) {
        this(key, null);
    }

    private Scope(JCTree key, Scope parent) {
        this.key = key;
        this.parent = parent;
        this.level = parent == null ? 0 : parent.level + 1;
    }

    public Scope createInner(JCTree key) {
        return new Scope(key, this);
    }

    public Scope getParent() {
        return parent;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return key.equals(obj);
    }

    @Override
    public String toString() {
        return key.toString();
    }
}
