package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.jvm.ByteCodes;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;

public class TranslateTypes extends TransTypes {
    private final TreeMaker make;
    private final Attributes attr;

    protected TranslateTypes(Context context) {
        super(context);
        attr = Attributes.instance(context);
        make = TreeMaker.instance(context);
    }

    @SuppressWarnings("unused")
    public static TranslateTypes instance(Context context) {
        TransTypes instance = context.get(transTypesKey);
        if (instance instanceof TranslateTypes) return (TranslateTypes) instance;
        context.put(transTypesKey, (TransTypes) null);
        return new TranslateTypes(context);
    }

    @Override
    public void visitBinary(JCTree.JCBinary tree) {
        if (tree.operator instanceof Symbol.OperatorSymbol) {
            Symbol.OperatorSymbol operator = (Symbol.OperatorSymbol) tree.operator;
            if (operator.opcode == ByteCodes.error + 1) {
                result = attr.translateOverloadedBinary(tree.lhs, operator, tree.rhs);
                result = translate(result);
                return;
            }
        }
        super.visitBinary(tree);
    }

    @Override
    public void visitAssignop(JCTree.JCAssignOp tree) {
        if (tree.operator instanceof Symbol.OperatorSymbol) {
            Symbol.OperatorSymbol operator = (Symbol.OperatorSymbol) tree.operator;
            if (operator.opcode == ByteCodes.error + 1) {
                result = make.Assign(tree.lhs, attr.translateOverloadedBinary(tree.lhs, operator, tree.rhs));
                result = translate(result);
                return;
            }
        }
        super.visitAssignop(tree);
    }
}
