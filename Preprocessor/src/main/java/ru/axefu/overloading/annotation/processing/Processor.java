package ru.axefu.overloading.annotation.processing;

import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Map;
import java.util.Set;

/**
 * Препроцессор для аннотации {@link ru.axefu.overloading.annotation.Operator}
 *
 * @author Artem Moshkin
 * @since 15.10.2024
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class Processor extends AbstractProcessor {


    private static Messager messager;
    private static long count = 0;
    public static void print(Object object) {
        messager.printMessage(Diagnostic.Kind.NOTE, "[" + count + "] " + object);
        count++;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = UnWrapper.get(processingEnv);
        messager = processingEnv.getMessager();
        JavacUtil.init(((JavacProcessingEnvironment) this.processingEnv).getContext());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        TreeTranslator translator = new BinaryTranslator();
        Trees trees = Trees.instance(processingEnv);
        translator.translate(roundEnv.getRootElements().stream().map(
                element -> (JCTree) trees.getPath(element).getCompilationUnit()
        ).toArray(JCTree[]::new));
        return false;
    }

    private static class BinaryTranslator extends TreeTranslator {
        private final TreeMaker maker = util.maker;

        @Override
        public void visitAssignop(JCTree.JCAssignOp jcAssignOp) {
            super.visitAssignop(jcAssignOp);
            Map<Tree.Kind, Name> overloaded = getOverloadedKinds(jcAssignOp.type);
            if (overloaded != null) {
                JCTree.Tag tag = getTag(getKind(jcAssignOp.getTag().toString()));
                if (tag == null) return;
                result = maker.Assign(
                        jcAssignOp.getVariable(),
                        maker.Binary(
                                tag,
                                jcAssignOp.getVariable(),
                                jcAssignOp.getExpression()
                        )
                );
                result.type = jcAssignOp.type;
                super.visitAssign((JCTree.JCAssign) result);
            }
        }

        @Override
        public void visitBinary(JCTree.JCBinary jcBinary) {
            super.visitBinary(jcBinary);
            Processor.print(jcBinary);
            Map<Tree.Kind, Name> overloaded = getOverloadedKinds(jcBinary.type);
            if (overloaded != null) {
                result = maker.Exec(maker.Apply(
                        List.nil(),
                        maker.Select(jcBinary.lhs, overloaded.get(jcBinary.getKind())),
                        List.of(jcBinary.rhs))
                ).getExpression();
                result.type = jcBinary.type;
            }
        }

        private JCTree.Tag getTag(Tree.Kind kind) {
            switch (kind) {
                case PLUS: return JCTree.Tag.PLUS;
                case MINUS: return JCTree.Tag.MINUS;
                case MULTIPLY: return JCTree.Tag.MUL;
                case DIVIDE: return JCTree.Tag.DIV;
            }
            return null;
        }
    }
}
