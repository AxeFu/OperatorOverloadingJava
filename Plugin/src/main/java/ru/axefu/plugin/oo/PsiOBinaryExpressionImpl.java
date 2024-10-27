package ru.axefu.plugin.oo;

import com.intellij.psi.PsiBinaryExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.resolve.JavaResolveCache;
import com.intellij.psi.impl.source.tree.java.PsiBinaryExpressionImpl;
import com.intellij.util.Function;

public class PsiOBinaryExpressionImpl extends PsiBinaryExpressionImpl {
    @Override
    public PsiType getType() {
        return JavaResolveCache.getInstance(getProject()).getType(
                this,
                (Function<PsiBinaryExpression, PsiType>) e -> Resolver.getType(PsiOBinaryExpressionImpl.this)
        );
    }
}
