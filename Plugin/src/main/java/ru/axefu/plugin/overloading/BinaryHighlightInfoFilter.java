package ru.axefu.plugin.overloading;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightUtilHook;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Фильтер подсветки ошибок
 *
 * @author Artem Moshkin
 * @since 13.10.2024
 */
public class BinaryHighlightInfoFilter implements HighlightInfoFilter {

    @Override
    public boolean accept(@NotNull HighlightInfo highlightInfo, @Nullable PsiFile file) {
        if (file == null || (highlightInfo.getSeverity() != HighlightSeverity.ERROR)) return true;
        PsiElement element = getPsiElement(highlightInfo, file, false);
        if (element == null) {
            element = getPsiElement(highlightInfo, file, true);
        }
        if (element == null) return true;
        if (element instanceof PsiPolyadicExpression expression) {
            return HighlightUtilHook.isPolyadicOperatorApplicable(expression) == null;
        }
        if (element instanceof PsiDeclarationStatement expression) {
            if (expression.getDeclaredElements().length == 1) {
                element = expression.getDeclaredElements()[0];
                if (element instanceof PsiVariable variable) {
                    PsiType require = variable.getType();
                    PsiType current = HighlightUtilHook.getType(variable.getInitializer());
                    if (current != null) {
                        if (HighlightUtilHook.isAssignability(require, current, null)) {
                            return false;
                        }
                    }
                }
            }
        }
        if (element instanceof PsiAssignmentExpression expression) {
            boolean result = !HighlightUtilHook.isAssignmentCompatibleTypes(expression);
            if (!result && !HighlightUtilHook.isAssignmentOperatorApplicable(expression)) result = true;
            if (!result) {
                HighlightUtilHook.checkReferenceExpression((PsiReferenceExpression) expression.getLExpression(), file);
                return false;
            }
            return true;
        }
        if (element instanceof PsiReturnStatement expression) {
            PsiMethod method = findMethodInParent(expression);
            if (method != null) {
                PsiType returnType = method.getReturnType();
                if (expression.getReturnValue() != null) {
                    PsiType valueType = HighlightUtilHook.getType(expression.getReturnValue());
                    if (valueType != null) {
                        if (HighlightUtilHook.isAssignability(returnType, valueType, null)) {
                            return false;
                        }
                    }
                }
            }
        }
        System.out.println(element.getClass() + " " + highlightInfo);
        return true;
    }

    private PsiMethod findMethodInParent(PsiReturnStatement expression) {
        PsiElement result = expression.getParent();
        while (!(result instanceof PsiMethod)) {
            result = result.getParent();
            if (result == null) return null;
        }
        return (PsiMethod) result;
    }

    private PsiElement getPsiElement(HighlightInfo highlightInfo, PsiElement file, boolean skipStartOffsetCheck) {
        if (file == null) return null;
        int startOffset = highlightInfo.startOffset;
        int endOffset = highlightInfo.endOffset;
        LinkedList<PsiElement> list = new LinkedList<>();
        list.add(file);
        while (!list.isEmpty()) {
            for (PsiElement child : list.getFirst().getChildren()) {
                TextRange range = child.getTextRange();
                if (range.getEndOffset() == endOffset && (skipStartOffsetCheck || range.getStartOffset() == startOffset)) {
                    return child;
                }
                if (child.getChildren().length != 0) {
                    list.add(child);
                }
            }
            list.removeFirst();
        }
        return null;
    }
}
