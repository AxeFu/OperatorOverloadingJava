package ru.axefu.plugin.oo;

import com.intellij.lang.java.JavaLanguage;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.TypeConversionUtil;

public class Resolver {

    public static final PsiType noType = TypeConversionUtil.NULL_TYPE;

    public static PsiType getType(PsiBinaryExpression e) {
        if (e == null || e.getROperand() == null)
            return noType;
        return getType(e.getLOperand().getType(), e.getROperand().getType(), e.getOperationTokenType());
    }

    public static PsiType getType(PsiType left, PsiType right, IElementType op) {
        if (op == null || !Startup.checkProcessor()) return noType;
        String methodname =  Methods.binary.get(op);
        if (methodname != null && right != null) {
            return resolveMethod(left, methodname, right);
        }
        return noType;
    }

    public static PsiType resolveMethod(PsiType type, String methodName, PsiType argType) {
        if (!(type instanceof PsiClassType clazz) || methodName==null) return noType;
        if (argType == null) return noType;
        PsiSubstitutor subst = clazz.resolveGenerics().getSubstitutor();
        PsiClass psiClass = clazz.resolve();
        if (psiClass == null)
            return noType;
        LightMethodBuilder methodSignature = new LightMethodBuilder(psiClass.getManager(), JavaLanguage.INSTANCE, methodName);
        methodSignature.addParameter("_", argType);
        PsiMethod method = psiClass.findMethodBySignature(methodSignature, true);
        return method==null ? noType : subst.substitute(method.getReturnType());
    }
}
