package ru.axefu.plugin.overloading;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class Problems extends AbstractBaseJavaLocalInspectionTool {

    private static final Set<HighlightInfo> unregisteredProblems = new HashSet<>();
    private static final Set<PsiFile> unregisteredFiles = new HashSet<>();

    private static ProblemsHolder holder;
    public static void registerProblem(HighlightInfo info, PsiFile file) {
        if (holder == null) {
            addUnregisteredProblem(info, file);
            return;
        }
        if (info != null && info.getSeverity() == HighlightSeverity.ERROR) {
            final int startOffset = info.getStartOffset();
            final PsiElement element = file.findElementAt(startOffset);
            if (element != null) {
                holder.registerProblem(element, info.getDescription());
            }
        }
    }

    private static void registerUnregisteredProblems() {
        PsiFile[] files = unregisteredFiles.stream().toArray(PsiFile[]::new);
        int count = 0;
        for (HighlightInfo info : unregisteredProblems) {
            registerProblem(info, files[count]);
            count++;
        }
        unregisteredProblems.clear();
        unregisteredFiles.clear();
    }

    private static void addUnregisteredProblem(HighlightInfo info, PsiFile file) {
        unregisteredProblems.add(info);
        unregisteredFiles.add(file);
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        Problems.holder = holder;
        return new JavaElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                registerUnregisteredProblems();
            }
        };
    }
}
