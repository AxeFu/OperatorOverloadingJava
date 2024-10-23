package ru.axefu.plugin.overloading;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightVisitorImpl;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BinaryLocalInspectionTool extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new ReferenceExpressionVisitor(holder);
    }

    private static class ReferenceExpressionVisitor extends HighlightVisitorImpl {

        public ReferenceExpressionVisitor(ProblemsHolder holder) {
            prepareToRunAsInspection(new HighlightInfoHolder(holder.getFile()) {
                @Override
                public boolean add(@Nullable HighlightInfo info) {
                    if (super.add(info)) {
                        if (info != null && info.getSeverity() == HighlightSeverity.ERROR) {
                            final int startOffset = info.getStartOffset();
                            final PsiElement element = holder.getFile().findElementAt(startOffset);
                            if (element != null) {
                                holder.registerProblem(element, info.getDescription());
                            }
                        }
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean hasErrorResults() {
                    return false;
                }
            });
        }

        @Override
        public void visitVariable(@NotNull PsiVariable variable) {
            if (variable.getReference() instanceof PsiReferenceExpression referenceExpression) {
                visitReferenceExpression(referenceExpression);
            }
        }

        @Override
        public void visitReturnStatement(@NotNull PsiReturnStatement statement) {
            super.visitStatement(statement);
        }

        @Override
        public void visitPolyadicExpression(@NotNull PsiPolyadicExpression expression) {
        }

        @Override
        public void visitAssignmentExpression(@NotNull PsiAssignmentExpression assignment) {
            super.visitExpression(assignment);
        }
    }
}
