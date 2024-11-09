package ru.axefu.plugin.oo;

import com.intellij.codeInsight.daemon.impl.HighlightVisitor;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightVisitorImpl;
import com.intellij.openapi.extensions.impl.ExtensionComponentAdapter;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiBinaryExpression;
import com.intellij.psi.impl.source.BasicJavaElementType;
import com.intellij.psi.impl.source.tree.JavaElementType;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.axefu.reflection.Reflection;

import java.util.List;
import java.util.function.Supplier;

public class Startup implements ProjectActivity {

    private static Project project;

    public static boolean checkProcessor() {
        if (project == null) return false;
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
        return facade.findPackage("ru.axefu.oo") != null;
    }

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        Startup.project = project;
        try {
            Reflection.set(
                    (BasicJavaElementType.JavaCompositeElementType) JavaElementType.BINARY_EXPRESSION,
                    BasicJavaElementType.JavaCompositeElementType.class,
                    "myConstructor",
                    (Supplier<PsiBinaryExpression>) PsiOBinaryExpressionImpl::new
            );

            ExtensionPointImpl<HighlightVisitor> ep =
                    (ExtensionPointImpl<HighlightVisitor>) project.getExtensionArea().getExtensionPoint(HighlightVisitor.EP_HIGHLIGHT_VISITOR);
            List<ExtensionComponentAdapter> adapters = Reflection.get(ep, ExtensionPointImpl.class, "adapters");
            for (ExtensionComponentAdapter adapter : adapters) {
                if (HighlightVisitorImpl.class.getName().equals(adapter.getAssignableToClassName())) {
                    Reflection.set(adapter, ExtensionComponentAdapter.class, "implementationClassOrName", BinaryHighlightVisitor.class);
                    break;
                }
            }
        } catch (Exception e) {
            Reflection.sneakyThrow(e);
        }
        return null;
    }
}
