package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.jvm.ByteCodes;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.*;

import static com.sun.tools.javac.code.TypeTag.NONE;

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

    @Override
    Type check(JCTree tree, Type found, int ownkind, ResultInfo resultInfo) {
        Type old = tree.type;
        Type res = old;
        if (tree instanceof JCTree.JCBinary) res = checkOverloadBinary((JCTree.JCBinary) tree);
        if (tree instanceof JCTree.JCAssignOp) res = checkAssignOpBinary((JCTree.JCAssignOp) tree);
        if (res != old) return res;
        return super.check(tree, found, ownkind, resultInfo);
    }

    private boolean referenceIsRhs;
    @Override
    public void visitReference(JCTree.JCMemberReference that) {
        if (pt().isErroneous() || (pt().hasTag(NONE) && pt() != Type.recoveryType)) {
            if (pt().hasTag(NONE) && referenceIsRhs) {
                //is lambda and is inside binary/assignOp
                referenceIsRhs = false;
                return;
            }
        }
        super.visitReference(that);
    }

    @Override
    public void visitBinary(JCTree.JCBinary tree) {
        referenceIsRhs = tree.rhs instanceof JCTree.JCMemberReference;
        super.visitBinary(tree);
        referenceIsRhs = false;
    }

    @Override
    public void visitAssignop(JCTree.JCAssignOp tree) {
        referenceIsRhs = tree.rhs instanceof JCTree.JCMemberReference;
        super.visitAssignop(tree);
        referenceIsRhs = false;
    }

    private Type checkAssignOpBinary(JCTree.JCAssignOp tree) {
        Symbol.OperatorSymbol operator = (Symbol.OperatorSymbol) tree.operator;
        if (operator.opcode == ByteCodes.error + 1) {
            JCTree.JCAssign jcAssign = make.Assign(tree.lhs, translateOverloadedBinary(tree.lhs, operator, tree.rhs));
            visitAssign(jcAssign);
            tree.type = jcAssign.type;
        }
        return tree.type;
    }

    private Type checkOverloadBinary(JCTree.JCBinary tree) {
        Symbol.OperatorSymbol operator = (Symbol.OperatorSymbol) tree.operator;
        if (operator.opcode == ByteCodes.error + 1) {
            JCTree method = translateOverloadedBinary(tree.lhs, operator, tree.rhs);
            visitApply((JCTree.JCMethodInvocation) method);
            tree.type = method.type;
        }
        return tree.type;
    }

    JCTree.JCExpression translateOverloadedBinary(
            JCTree.JCExpression lhs,
            Symbol.OperatorSymbol operator,
            JCTree.JCExpression rhs
    ) {
        Symbol.MethodSymbol ms = (Symbol.MethodSymbol) operator.owner;
        JCTree.JCFieldAccess meth = make.Select(lhs, ms.name);
        meth.type = ms.type;
        meth.sym = ms;
        return make.Apply(null, meth, List.of(rhs)).setType(((Type.MethodType) ms.type).restype);
    }
}
