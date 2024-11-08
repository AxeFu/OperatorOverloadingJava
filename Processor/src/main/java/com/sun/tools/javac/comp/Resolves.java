package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.jvm.ByteCodes;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import ru.axefu.oo.Methods;

import static com.sun.tools.javac.code.Kinds.ABSENT_MTH;

@SuppressWarnings("unused")
public class Resolves extends Resolve {
    protected Resolves(Context context) {
        super(context);
    }

    public static Resolves instance(Context context) {
        Resolve instance = context.get(resolveKey);
        if (instance instanceof Resolves) return (Resolves) instance;
        context.put(resolveKey, (Resolves)null);
        return new Resolves(context);
    }

    private final Symbol methodNotFound = new SymbolNotFoundError(ABSENT_MTH);

    private Symbol findOperatorMethod(Env<AttrContext> env, Name name, List<Type> args, List<Type> typeArgTypes) {
        String methodName = args.tail.isEmpty() ? null : Methods.binary.get(name.toString());
        if (methodName == null) return methodNotFound;
        return findMethod(env, args.head, names.fromString(methodName), args.tail, typeArgTypes, true, false, false);
    }

    int count;
    @Override
    Symbol findMethod(final Env<AttrContext> env,final Type site,final Name name,final List<Type> argTypes,final List<Type> typeArgTypes,
                      boolean allowBoxing, boolean useVarargs, boolean operator) {
        Symbol bestSoFar = super.findMethod(env, site, name, argTypes, typeArgTypes, allowBoxing, useVarargs, operator);
        boolean tryOverload = (argTypes.tail != null && argTypes.tail.head != null && argTypes.tail.head.getTag() == TypeTag.ERROR);
        if ((tryOverload || (bestSoFar.kind >= Kinds.ERR)) && operator) {
            Symbol method = findOperatorMethod(env, name, argTypes, typeArgTypes);
            if (method.kind == Kinds.MTH) {
                bestSoFar = new Symbol.OperatorSymbol(method.name, method.type, ByteCodes.error + 1, method);
            }
        }
        return bestSoFar;
    }
}
