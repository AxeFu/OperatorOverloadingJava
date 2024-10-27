package com.sun.tools.javac.comp;

import com.sun.tools.javac.util.Context;

@SuppressWarnings("unused")
public class Attributes extends Attr {
    protected Attributes(Context context) {
        super(context);
    }

    public static Attributes instance(Context context) {
        Attr instance = context.get(attrKey);
        if (instance instanceof Attributes) return (Attributes) instance;
        context.put(attrKey, (Attr)null);
        return new Attributes(context);
    }
}
