package ru.axefu.oo.annotation.processing;

enum CompileStages {
    ATTR("com.sun.tools.javac.comp.Attributes"),
    TRANSTYPES("com.sun.tools.javac.comp.TranslateTypes"),
    RESOLVE("com.sun.tools.javac.comp.Resolves");

    private final String path;

    CompileStages(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return path;
    }
}