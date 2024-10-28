package ru.axefu.plugin.oo;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.TypeConversionUtil;

public class Resolver {

    public static PsiType getType(PsiBinaryExpression e) {
        if (e == null || e.getROperand() == null)
            return null;
        return getType(e.getLOperand().getType(), e.getROperand().getType(), e.getOperationTokenType(), e);
    }

    public static PsiType getType(PsiType left, PsiType right, IElementType op, PsiExpression expression) {
        if (op == null || !Startup.checkProcessor()) return null;
        String methodname =  Methods.binary.get(op);
        if (methodname != null && right != null) {
            return resolveMethod(left, methodname, right, expression);
        }
        return null;
    }

    public static PsiType resolveMethod(PsiType type, String methodName, PsiType argType, PsiExpression expression) {
        if (!(type instanceof PsiClassType clazz) || methodName==null) return null;
        if (argType == null) return null;
        PsiSubstitutor subst = clazz.resolveGenerics().getSubstitutor();
        PsiClass psiClass = clazz.resolve();
        if (psiClass == null)
            return null;
        LightMethodBuilder methodSignature = new LightMethodBuilder(psiClass.getManager(), JavaLanguage.INSTANCE, methodName);
        methodSignature.addParameter("_", argType);
        PsiMethod method = psiClass.findMethodBySignature(methodSignature, true);

        if (method == null || expression.getContext() == null) return null;
        if (!PsiResolveHelper.getInstance(expression.getProject()).isAccessible(method, expression.getContext(), psiClass)) {
             return null;
        }
        return subst.substitute(method.getReturnType());
    }
}
