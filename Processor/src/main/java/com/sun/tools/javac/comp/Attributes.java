package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.jvm.ByteCodes;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.*;

import static com.sun.tools.javac.code.TypeTag.NONE;
import static com.sun.tools.javac.tree.JCTree.*;

public class Attributes extends Attr {
    private final TranslateTypes translator;
    private boolean referenceIsRhs;

    protected Attributes(Context context) {
        super(context);
        translator = TranslateTypes.instance(context);
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

    @Override
    public void visitReference(JCTree.JCMemberReference that) {
        if (pt().isErroneous() || (pt().hasTag(NONE) && pt() != Type.recoveryType)) {
            if (pt().hasTag(NONE) && referenceIsRhs) {
                //reference no type inside binary/assignOp
                referenceIsRhs = false;
                return;
            }
        }
        super.visitReference(that);
    }

    @Override
    public void visitLambda(JCLambda jcLambda) {
        if (pt().hasTag(TypeTag.NONE) && referenceIsRhs) {
            //lambda inside binary/assignOp
            referenceIsRhs = false;
            return;
        }
        super.visitLambda(jcLambda);
    }

    @Override
    public void visitBinary(JCTree.JCBinary tree) {
        referenceIsRhs = tree.rhs instanceof JCMemberReference || tree.rhs instanceof JCLambda;
        super.visitBinary(tree);
        referenceIsRhs = false;
    }

    @Override
    public void visitAssignop(JCTree.JCAssignOp tree) {
        referenceIsRhs = tree.rhs instanceof JCMemberReference || tree.rhs instanceof JCLambda;
        super.visitAssignop(tree);
        referenceIsRhs = false;
    }

    private Type checkAssignOpBinary(JCAssignOp tree) {
        Symbol.OperatorSymbol operator = (Symbol.OperatorSymbol) tree.operator;
        if (operator.opcode == ByteCodes.error + 1) {
            JCAssign jcAssign = make.Assign(tree.lhs, translator.translateOverloaded(tree.lhs, operator, tree.rhs));
            visitAssign(jcAssign);
            tree.type = jcAssign.type;
        }
        return tree.type;
    }

    private Type checkOverloadBinary(JCBinary tree) {
        Symbol.OperatorSymbol operator = (Symbol.OperatorSymbol) tree.operator;
        if (operator.opcode == ByteCodes.error + 1) {
            JCMethodInvocation method = translator.translateOverloaded(tree.lhs, operator, tree.rhs);
            visitApply(method);
            tree.type = method.type;
        }
        return tree.type;
    }
}
