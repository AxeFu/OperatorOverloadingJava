package ru.axefu.plugin.oo;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.tree.IElementType;

public class Resolver {

    public static PsiType getType(PsiBinaryExpression e) {
        if (e == null || e.getROperand() == null)
            return null;
        return getType(e.getLOperand().getType(), e.getROperand().getType(), e.getOperationTokenType());
    }

    public static PsiType getType(PsiType left, PsiType right, IElementType op) {
        if (op == null || !Startup.checkProcessor()) return null;
        String methodname =  Methods.binary.get(op);
        if (methodname != null && right != null) {
            return resolveMethod(left, methodname, right);
        }
        return null;
    }

    public static PsiType resolveMethod(PsiType type, String methodName, PsiType argType) {
        if (!(type instanceof PsiClassType clazz) || methodName==null) return null;
        if (argType == null) return null;
        PsiSubstitutor subst = clazz.resolveGenerics().getSubstitutor();
        PsiClass psiClass = clazz.resolve();
        if (psiClass == null)
            return null;
        LightMethodBuilder methodSignature = new LightMethodBuilder(psiClass.getManager(), JavaLanguage.INSTANCE, methodName);
        methodSignature.addParameter("_", argType);
        PsiMethod method = psiClass.findMethodBySignature(methodSignature, true);
        return method==null ? null : subst.substitute(method.getReturnType());
    }
}
