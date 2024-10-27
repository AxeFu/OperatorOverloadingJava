package ru.axefu.plugin.oo;

import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.tree.java.PsiBinaryExpressionImpl;

public class PsiOBinaryExpressionImpl extends PsiBinaryExpressionImpl {
    @Override
    public PsiType getType() {
        PsiType type = Resolver.getType(PsiOBinaryExpressionImpl.this);
        if (type == null) {
            return PsiOBinaryExpressionImpl.super.getType();
        }
        return type;
    }
}
