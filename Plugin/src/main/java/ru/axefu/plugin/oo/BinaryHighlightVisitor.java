package ru.axefu.plugin.oo;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightVisitorImpl;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
import ru.axefu.reflection.Reflection;

import java.util.List;

public class BinaryHighlightVisitor extends HighlightVisitorImpl {

    private HighlightInfoHolder holder;

    @Override
    public boolean analyze(@NotNull PsiFile file, boolean updateWholeFile, @NotNull HighlightInfoHolder holder, @NotNull Runnable highlight) {
        this.holder = holder;
        try {
            return super.analyze(file, updateWholeFile, holder, highlight);
        } finally {
            this.holder = null;
        }
    }

    @Override
    public void visitPolyadicExpression(@NotNull PsiPolyadicExpression expression) {
        super.visitPolyadicExpression(expression);
        if (isHighlighted(expression)) {
            PsiExpression[] operands = expression.getOperands();
            PsiType lType = operands[0].getType();
            IElementType operationSign = expression.getOperationTokenType();
            for (int i = 1; i < operands.length; i++) {
                PsiExpression operand = operands[i];
                PsiType rType = operand.getType();
                if (!TypeConversionUtil.isBinaryOperatorApplicable(operationSign, lType, rType, false)) {
                    PsiJavaToken token = expression.getTokenBeforeOperand(operand);
                    if (token != null) {
                        lType = Resolver.getType(lType, rType, token.getTokenType());
                    }
                } else {
                    lType = TypeConversionUtil.calcTypeForBinaryExpression(lType, rType, operationSign, true);
                }
            }
            if (lType != null) {
                removeLastHighlight();
            }
        }
    }

    @Override
    public void visitAssignmentExpression(@NotNull PsiAssignmentExpression assignment) {
        super.visitAssignmentExpression(assignment);
        if ("=".equals(assignment.getOperationSign().getText())) return;
        IElementType operationSign = TypeConversionUtil.convertEQtoOperation(assignment.getOperationTokenType());
        if (assignment.getRExpression() == null) return;
        PsiType lType = assignment.getLExpression().getType();
        PsiType rType = assignment.getRExpression().getType();
        if (lType == null || rType == null) return;
        PsiType result = Resolver.getType(lType, rType, operationSign);
        if (result == null) return;
        if (isHighlighted(assignment) && TypeConversionUtil.isAssignable(lType, result))
            removeLastHighlight();
    }

    private boolean isHighlighted(PsiElement expression) {
        if (holder.hasErrorResults()) {
            HighlightInfo hi = holder.get(holder.size() - 1);
            if (hi.getSeverity() != HighlightSeverity.ERROR) return false;
            if (expression instanceof PsiVariable variable) { // workaround for variable declaration incompatible types highlight
                PsiTypeElement typeElement = variable.getTypeElement();
                if (typeElement == null)
                    return false;
                TextRange typeElementTextRange = typeElement.getTextRange();
                TextRange variableTextRange = variable.getTextRange();
                return variableTextRange != null && typeElementTextRange != null
                        && hi.startOffset == typeElementTextRange.getStartOffset()
                        && hi.endOffset == variableTextRange.getEndOffset();
            }
            TextRange tr = expression.getTextRange();
            return hi.startOffset == tr.getStartOffset() && hi.endOffset == tr.getEndOffset();
        }
        return false;
    }

    private void removeLastHighlight() {
        try {
            List<HighlightInfo> myInfos = Reflection.get(holder, HighlightInfoHolder.class, "myInfos");
            myInfos.remove(holder.size() - 1);
            int errorCount = Reflection.get(holder, HighlightInfoHolder.class, "myErrorCount");
            Reflection.set(holder, HighlightInfoHolder.class, "myErrorCount", errorCount - 1);
        } catch (Exception e) {
            Reflection.sneakyThrow(e);
        }
    }

    @Override
    public BinaryHighlightVisitor clone() {
        return new BinaryHighlightVisitor();
    }
}
