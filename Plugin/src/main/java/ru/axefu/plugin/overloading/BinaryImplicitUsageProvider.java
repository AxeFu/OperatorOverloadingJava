package ru.axefu.plugin.overloading;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;

/**
 * Удаляет предупреждение что метод с аннотацией Operator не используется
 *
 * @author Artem Moshkin
 * @since 20.10.2024
 */
public class BinaryImplicitUsageProvider implements ImplicitUsageProvider {

    private static final String OPERATOR = "ru.axefu.overloading.annotation.Operator";

    @Override
    public boolean isImplicitUsage(@NotNull PsiElement element) {
        if (element instanceof PsiMethod) {
            return AnnotationUtil.isAnnotated((PsiModifierListOwner) element, OPERATOR,0);
        }
        return false;
    }

    @Override
    public boolean isImplicitRead(@NotNull PsiElement element) {
        return false;
    }

    @Override
    public boolean isImplicitWrite(@NotNull PsiElement element) {
        return false;
    }
}
