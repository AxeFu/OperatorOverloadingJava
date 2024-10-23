package ru.axefu.overloading.annotation.processing;

import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.Messager;

/**
 * Служебный класс для процессора
 *
 * @author Artem Moshkin
 * @since 19.10.2024
 */
class JavacUtil {
    static JavacUtil util;
    public final TreeMaker maker;
    public final Symtab symtab;
    public final Types types;
    public final Names names;

    public static void init(Context context) {
        util = new JavacUtil(context);
    }

    private JavacUtil(Context context) {
        this.maker = TreeMaker.instance(context);
        this.symtab = Symtab.instance(context);
        this.types = Types.instance(context);
        this.names = Names.instance(context);
    }

}
