package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.jvm.ByteCodes;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;

import static com.sun.tools.javac.tree.JCTree.*;

public class TranslateTypes extends TransTypes {
    private final TreeMaker make;

    protected TranslateTypes(Context context) {
        super(context);
        make = TreeMaker.instance(context);
    }

    public static TranslateTypes instance(Context context) {
        TransTypes instance = context.get(transTypesKey);
        if (instance instanceof TranslateTypes) return (TranslateTypes) instance;
        context.put(transTypesKey, (TransTypes) null);
        return new TranslateTypes(context);
    }

    @Override
    public void visitBinary(JCBinary tree) {
        if (tree.operator instanceof Symbol.OperatorSymbol) {
            Symbol.OperatorSymbol operator = (Symbol.OperatorSymbol) tree.operator;
            if (operator.opcode == ByteCodes.error + 1) {
                result = translateOverloaded(tree.lhs, operator, tree.rhs);
                result = translate(result);
                return;
            }
        }
        super.visitBinary(tree);
    }

    @Override
    public void visitAssignop(JCAssignOp tree) {
        if (tree.operator instanceof Symbol.OperatorSymbol) {
            Symbol.OperatorSymbol operator = (Symbol.OperatorSymbol) tree.operator;
            if (operator.opcode == ByteCodes.error + 1) {
                result = make.Assign(tree.lhs, translateOverloaded(tree.lhs, operator, tree.rhs));
                result = translate(result);
                return;
            }
        }
        super.visitAssignop(tree);
    }

    public JCExpression translateOverloaded(JCExpression lhs, Symbol.OperatorSymbol operator, JCExpression rhs) {
        Symbol.MethodSymbol ms = (Symbol.MethodSymbol) operator.owner;
        JCTree.JCFieldAccess meth = make.Select(lhs, ms.name);
        meth.type = ms.type;
        meth.sym = ms;
        return make.Apply(null, meth, List.of(rhs)).setType(((Type.MethodType) ms.type).restype);
    }
}
