package ru.axefu.oo.annotation.processing;

import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import ru.axefu.reflection.Reflection;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.io.InputStream;
import java.util.Set;

@SuppressWarnings({"unchecked","ResultOfMethodCallIgnored"})
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class Processor extends AbstractProcessor {
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        JavacProcessingEnvironment pe = (JavacProcessingEnvironment) Reflection.unwrap(processingEnv);
        JavaCompiler compiler = JavaCompiler.instance(pe.getContext());
        try {
            ClassLoader processorClassLoader = Reflection.get(pe, "processorClassLoader");
            MultiTaskListener taskListener = Reflection.get(pe, "taskListener");
            taskListener.add(new JavacTaskListener(compiler, processorClassLoader));
        } catch (Exception e) {
            Reflection.sneakyThrow(e);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        return false;
    }

    private <T> Class<T> defineClass(String className, ClassLoader from, ClassLoader to) throws Exception {
        try {
            return (Class<T>) to.loadClass(className);
        } catch (ClassNotFoundException ignored) {}
        String path = className.replace('.', '/') + ".class";
        byte[] bytes;
        try (InputStream is = from.getResourceAsStream(path)) {
            assert is != null;
            bytes = new byte[is.available()];
            is.read(bytes);
        }
        return Reflection.invoke(
                to,
                ClassLoader.class,
                "defineClass",
                new Class[] {String.class, byte[].class, int.class, int.class},
                className, bytes, 0, bytes.length
        );
    }

    private void patch(JavaCompiler compiler, ClassLoader myLoader) {
        try {
            compiler = Reflection.get(compiler, "delegateCompiler");
            Context context = Reflection.get(compiler, "context");
            Attr attr = Attr.instance(context);
            if (attr instanceof Attributes) return;
            ClassLoader javacLoader = attr.getClass().getClassLoader();

            //Define classes in javac ClassLoader
            Class<Attr> attrClass = defineClass(CompileStages.ATTR.toString(), myLoader, javacLoader);
            Class<TransTypes> transTypesClass = defineClass(CompileStages.TRANSTYPES.toString(), myLoader, javacLoader);
            Class<Resolve> resolveClass = defineClass(CompileStages.RESOLVE.toString(), myLoader, javacLoader);
            defineClass("ru.axefu.oo.Methods", myLoader, javacLoader);
            defineClass("ru.axefu.oo.Methods$1", myLoader, javacLoader);

            //Call instance methods
            Reflection.invoke(resolveClass, "instance", context);
            attr = Reflection.invoke(attrClass, "instance", context);
            TransTypes transTypes = Reflection.invoke(transTypesClass, "instance", context);

            //Replace old Attr and TransTypes in JavaCompiler
            Reflection.set(compiler, "attr", attr);
            Reflection.set(compiler, "transTypes", transTypes);
            Reflection.set(MemberEnter.instance(context), "attr", attr);
        } catch (Exception e) {
            Reflection.sneakyThrow(e);
        }
    }

    private class JavacTaskListener implements TaskListener {
        private final JavaCompiler compiler;

        private final ClassLoader classLoader;
        boolean done = false;

        public JavacTaskListener(JavaCompiler compiler, ClassLoader classLoader) {
            this.compiler = compiler;
            this.classLoader = classLoader;
        }

        @Override
        public void started(TaskEvent taskEvent) {
            if (!done && taskEvent.getKind() == TaskEvent.Kind.ANALYZE) {
                patch(compiler, classLoader);
                done = true;
            }
        }

        @Override
        public void finished(TaskEvent taskEvent) {}
    }
}
