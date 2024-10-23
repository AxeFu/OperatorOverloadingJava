package ru.axefu.overloading.annotation.processing;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import ru.axefu.overloading.annotation.Operator;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.sun.tools.javac.tree.JCTree.*;

/**
 * Javac Translator
 *
 * @author Artem Moshkin
 * @since 15.10.2024
 */
class TreeTranslator extends com.sun.tools.javac.tree.TreeTranslator {
    JavacUtil util = JavacUtil.util;

    private String currentPackage;
    private final Map<String, Map<String, Type>> packages = new HashMap<>();

    private Type currentClass;
    private final Map<Type, Map<String, Type>> imports = new HashMap<>();
    private final Map<Type, Map<FieldType, Map<String, JCTree>>> classes = new HashMap<>();
    private final Map<Type, Map<Kind, Name>> overloadedOperator = new HashMap<>();

    private Scope currentScope;
    //JCTree - currentScope.key
    private final Map<JCTree, Map<FieldType, Map<String, JCTree>>> local = new HashMap<>();

    /**
     * Трансливаровать деревья предварительно разрешив типы всех переменных и методов
     */
    public void translate(JCTree[] trees) {
        List<JCTree> list = List.from(trees);
        scan(list);
        translate(list);
    }

    /**
     * Начало файла
     */
    @Override
    public void visitTopLevel(JCCompilationUnit jcCompilationUnit) {
        currentPackage = jcCompilationUnit.getPackageName().toString();
        currentScope = new Scope(jcCompilationUnit);
        super.visitTopLevel(jcCompilationUnit);
    }

    /**
     * Уходит вниз в блок класса
     */
    @Override
    public void visitClassDef(JCClassDecl jcClassDecl) {
        currentClass = jcClassDecl.type;
        down(jcClassDecl);
        super.visitClassDef(jcClassDecl);
        up();
    }

    /**
     * Уходит вниз в блок кода
     */
    @Override
    public void visitBlock(JCBlock jcBlock) {
        down(jcBlock);
        super.visitBlock(jcBlock);
        up();
    }

    /**
     * Уходит вниз в блок метода
     */
    @Override
    public void visitMethodDef(JCMethodDecl jcMethodDecl) {
        down(jcMethodDecl);
        super.visitMethodDef(jcMethodDecl);
        up();
    }

    /**
     * Разрешает тип нового класса
     */
    @Override
    public void visitNewClass(JCNewClass jcNewClass) {
        super.visitNewClass(jcNewClass);
        jcNewClass.type = jcNewClass.getIdentifier().type;
    }

    /**
     * Разрешает тип у цепочки вызовов Class.var1.var2.f();
     */
    @Override
    public void visitSelect(JCFieldAccess jcFieldAccess) {
        super.visitSelect(jcFieldAccess);
        jcFieldAccess.type = getVariable(jcFieldAccess.selected.type, jcFieldAccess.getIdentifier().toString());
    }

    /**
     * Разрешает возвращаемый тип у вызова метода
     */
    @Override
    public void visitApply(JCMethodInvocation jcMethodInvocation) {
        super.visitApply(jcMethodInvocation);
        JCTree tree = jcMethodInvocation.getMethodSelect();
        if (tree instanceof JCIdent) {
            tree.type = getMethod(((JCIdent) tree).getName() + "");
        }
        if (tree instanceof JCFieldAccess) {
            JCFieldAccess fieldAccess = (JCFieldAccess) tree;
            tree.type = getMethod(fieldAccess.selected.type, fieldAccess.getIdentifier().toString());
        }
        jcMethodInvocation.type = tree.type;
    }

    /**
     * Разрешает тип для идентификатора ClassName или имени variableName
     */
    @Override
    public void visitIdent(JCIdent jcIdent) {
        String first = jcIdent.getName().charAt(0) + "";
        if (first.toUpperCase().equals(first)) {
            jcIdent.type = imports.get(currentClass).get(jcIdent.getName() + "");
        } else {
            jcIdent.type = getVariable(jcIdent.getName() + "");
        }
        super.visitIdent(jcIdent);
    }

    /**
     * Разрешает тип для бинарной операции
     */
    @Override
    public void visitBinary(JCBinary jcBinary) {
        super.visitBinary(jcBinary);
        Type type = jcBinary.getLeftOperand().type;
        if (type == null) return;
        Map<Kind, Name> operator = getOverloadedKinds(type);
        jcBinary.type = getMethod(type, operator.get(jcBinary.getKind()).toString());
    }

    @Override
    public void visitAssignop(JCAssignOp jcAssignOp) {
        super.visitAssignop(jcAssignOp);
        Type type = jcAssignOp.getVariable().type;
        if (type == null) return;
        Kind kind = getKind(jcAssignOp.getTag().toString());
        Map<Kind, Name> operator = getOverloadedKinds(type);
        jcAssignOp.type = getMethod(type, operator.get(kind).toString());
    }

    /**
     * получить type из дерева, предварительно проверив её на null
     */
    private Type getType(JCTree tree) {
        if (tree == null) return null;
        return tree.type;
    }

    /**
     * Подняться из блока кода
     */
    private void up() {
        currentScope = currentScope.getParent();
    }

    /**
     * Опуститься в блок кода
     *
     * @param tree блок
     */
    private void down(JCTree tree) {
        currentScope = currentScope.createInner(tree);
        if (local.get(currentScope.key) == null) {
            local.put(currentScope.key, new HashMap<>());
            local.get(currentScope.key).put(FieldType.VARIABLE, new HashMap<>());
            local.get(currentScope.key).put(FieldType.METHOD, new HashMap<>());
        }
    }

    /**
     * Получить переменную, поиск начинается с локальных
     *
     * @param name имя переменной
     * @return тип переменной
     */
    private Type getVariable(String name) {
        Type result = getLocal(FieldType.VARIABLE, name);
        if (result == null) result = getVariable(currentClass, name);
        return result;
    }

    /**
     * Получить метод, поиск начинается с локальных
     *
     * @param name имя метода
     * @return возвращаемый тип метода
     */
    private Type getMethod(String name) {
        Type result = getLocal(FieldType.METHOD, name);
        if (result == null) result = getMethod(currentClass, name);
        return result;
    }

    /**
     * Получить локальное поле
     */
    private Type getLocal(FieldType fieldType, String name) {
        Scope currentScope = this.currentScope;
        JCTree result = null;
        do {
            if (currentScope.key instanceof JCCompilationUnit) break;
            result = local.get(currentScope.key).get(fieldType).get(name);
            currentScope = currentScope.getParent();
        } while (result == null);
        return getType(result);
    }

    /**
     * Получить тип переменной в классе (не локальный тип)
     *
     * @param type класс, в котором находится переменная
     * @param name имя переменной
     * @return тип переменной
     */
    private Type getVariable(Type type, String name) {
        if (type == null || name == null) return null;
        if (classes.get(type) == null) return compiledClass(type, clazz -> {
            try {
                Field field = clazz.getField(name);
                Name fieldTypeName = util.names.fromString(field.getType().toString());
                return new Type.ClassType(Type.JCNoType.noType, null, util.symtab.classes.get(fieldTypeName));
            } catch (NoSuchFieldException e) {
                return null;
            }
        });
        JCTree result = classes.get(type).get(FieldType.VARIABLE).get(name);
        return getType(result);
    }

    /**
     * Получает возвращаемый тип у метода под именем name, находящегося в классе type
     *
     * @param type класс содержащий метод
     * @param name имя метода
     * @return тип результата вызова метода name
     */
    private Type getMethod(Type type, String name) {
        if (type == null || name == null) return null;
        if (classes.get(type) == null) return compiledClass(type, clazz -> {
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(name)) {
                    Name fullTypeName = util.names.fromString(method.getReturnType().getCanonicalName());
                    return new Type.ClassType(Type.JCNoType.noType, null, util.symtab.classes.get(fullTypeName));
                }
            }
            return null;
        });
        JCTree result = classes.get(type).get(FieldType.METHOD).get(name);
        return getType(result);
    }

    /**
     * Предварительный скан перед трансляцией
     *
     * @param trees сканируемые деревья
     */
    private <T extends JCTree> void scan(List<T> trees) {
        PackageScanner packageScanner = new PackageScanner();
        packageScanner.scan(trees);
        ClassScanner scanner = new ClassScanner();
        scanner.scan(trees);
        scanner.resolveStaticImports();
    }

    /**
     * Получает все имена методов аннотированных Operator, не учитывает разное имя метода перегружающего 1 оператор
     *
     * @param type тип класса в котором есть эти методы
     * @return дерево имен методов, которые перегружают оператор
     */
    public Map<Kind, Name> getOverloadedKinds(Type type) {
        Map<Kind, Name> result = overloadedOperator.get(type);
        if (result == null) {
            result = compiledClass(type, clazz -> {
                Map<Kind, Name> map = new HashMap<>();
                for (Method method : clazz.getMethods()) {
                    Operator operator = method.getAnnotation(Operator.class);
                    if (operator != null) {
                        Kind kind = getKind(operator.value().toString());
                        if (kind != null) {
                            map.put(kind, util.names.fromString(method.getName()));
                        }
                    }
                }
                return map;
            });
        }
        return result;
    }

    /**
     * Выполнить функцию над скомпилированным классом
     */
    private <T> T compiledClass(Type type, Function<Class<?>, T> function) {
        if (type == null) return null;
        try {
            String className = type.toString();
            return function.apply(getClass().getClassLoader().loadClass(className));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Получает тип операции из строки
     *
     * @param value тип оператора в виде строки
     * @return тип оператора
     */
    public Kind getKind(String value) {
        switch (value) {
            case "PLUS_ASG":
            case "PLUS":
                return Kind.PLUS;
            case "MINUS_ASG":
            case "MINUS":
                return Kind.MINUS;
            case "MUL_ASG":
            case "MULTIPLY":
                return Kind.MULTIPLY;
            case "DIV_ASG":
            case "DIVIDE":
                return Kind.DIVIDE;
        }
        return null;
    }

    private boolean isClassScope() {
        return currentScope.key instanceof JCClassDecl;
    }

    private String getClassName(JCClassDecl jcClassDecl) {
        String prefix = isClassScope() && !currentScope.key.equals(jcClassDecl) ? ((JCClassDecl)currentScope.key).getSimpleName() + "." : "";
        return prefix + jcClassDecl.getSimpleName().toString();
    }

    /**
     * Собирает все переменные и методы в дерево classes разрешая при этом классы одного пакета и статический import
     */
    private class ClassScanner extends TreeScanner {

        private final Map<Type, Map<String, String>> classWithStaticImport = new HashMap<>();
        private final Map<String, String> staticImports = new HashMap<>();
        private final Map<String, Type> types = new HashMap<>();

        @Override
        public void visitTopLevel(JCCompilationUnit jcCompilationUnit) {
            types.clear();
            staticImports.clear();
            local.put((currentScope = new Scope(jcCompilationUnit)).key, new HashMap<>());
            local.get(currentScope.key).put(FieldType.VARIABLE, new HashMap<>());
            local.get(currentScope.key).put(FieldType.METHOD, new HashMap<>());
            currentPackage = jcCompilationUnit.getPackageName().toString();
            types.putAll(packages.get(currentPackage));
            super.visitTopLevel(jcCompilationUnit);
        }

        @Override
        public void visitImport(JCImport jcImport) {
            JCTree.JCFieldAccess jcFieldAccess = (JCTree.JCFieldAccess) jcImport.getQualifiedIdentifier();
            String packet = jcFieldAccess.getExpression().toString();
            String simpleName = jcFieldAccess.getIdentifier().toString();
            if (!jcImport.isStatic()) {
                Map<String, Type> types = packages.get(packet);
                Type type = types != null ? types.get(simpleName) : jcFieldAccess.type;
                this.types.put(simpleName, type);
            } else {
                staticImports.put(currentPackage, ((JCFieldAccess) jcFieldAccess.getExpression()).getIdentifier().toString());
            }
        }

        @Override
        public void visitClassDef(JCClassDecl jcClassDecl) {
            Type oldClass = currentClass;
            currentClass = packages.get(currentPackage).get(getClassName(jcClassDecl));
            imports.put(currentClass, new HashMap<>(types));
            classes.put(currentClass, new HashMap<>());
            classWithStaticImport.put(currentClass, new HashMap<>(staticImports));
            if (isClassScope() && !jcClassDecl.getModifiers().getFlags().contains(Modifier.STATIC)) {
                classWithStaticImport.get(currentClass).put(currentPackage, getClassName((JCClassDecl) currentScope.key));
            }
            classes.get(currentClass).put(FieldType.METHOD, new HashMap<>());
            classes.get(currentClass).put(FieldType.VARIABLE, new HashMap<>());
            classes.get(currentClass).get(FieldType.VARIABLE).put("this", jcClassDecl);
            jcClassDecl.type = currentClass;
            down(jcClassDecl);
            super.visitClassDef(jcClassDecl);
            up();
            currentClass = oldClass;
        }

        @Override
        public void visitBlock(JCBlock jcBlock) {
            down(jcBlock);
            super.visitBlock(jcBlock);
            up();
        }

        @Override
        public void visitMethodDef(JCMethodDecl jcMethodDecl) {
            jcMethodDecl.type = types.get(jcMethodDecl.getReturnType() + "");
            if (isClassScope()) {
                addMethod(jcMethodDecl.getName().toString(), jcMethodDecl);
            } else {
                addLocalMethod(jcMethodDecl.getName().toString(), jcMethodDecl);
            }
            down(jcMethodDecl);
            super.visitMethodDef(jcMethodDecl);
            up();
        }

        @Override
        public void visitVarDef(JCVariableDecl jcVariableDecl) {
            jcVariableDecl.type = types.get(jcVariableDecl.getType() + "");
            if (isClassScope()) {
                addVariable(jcVariableDecl.getName().toString(), jcVariableDecl);
            } else {
                addLocalVariable(jcVariableDecl.getName().toString(), jcVariableDecl);
            }
            super.visitVarDef(jcVariableDecl);
        }

        public void resolveStaticImports() {
            for (Type type : classWithStaticImport.keySet()) {
                Map<String, String> imports = classWithStaticImport.get(type);
                for (String pack : imports.keySet()) {
                    Type staticType = packages.get(pack).get(imports.get(pack));
                    Map<FieldType, Map<String, JCTree>> fromFields = classes.get(staticType);
                    Map<FieldType, Map<String, JCTree>> toFields = classes.get(type);
                    computeMapWithoutOverwrites(toFields.get(FieldType.VARIABLE), fromFields.get(FieldType.VARIABLE));
                    computeMapWithoutOverwrites(toFields.get(FieldType.METHOD), fromFields.get(FieldType.METHOD));
                }
            }
        }

        private void addLocalVariable(String name, JCVariableDecl tree) {
            local.get(currentScope.key).get(FieldType.VARIABLE).put(name, tree);
        }

        private void addLocalMethod(String name, JCMethodDecl tree) {
            local.get(currentScope.key).get(FieldType.METHOD).put(name, tree);
        }

        private void addVariable(String name, JCVariableDecl tree) {
            classes.get(currentClass).get(FieldType.VARIABLE).put(name, tree);
        }

        private void addMethod(String name, JCMethodDecl tree) {
            classes.get(currentClass).get(FieldType.METHOD).put(name, tree);
            overloadedOperator.computeIfAbsent(currentClass, k -> new HashMap<>());
            Kind kind = null;
            for (JCAnnotation annotation : tree.getModifiers().getAnnotations()) {
                if (!annotation.getArguments().isEmpty()) {
                    if (types.get(annotation.getAnnotationType().toString()).toString().equals(
                            Operator.class.getCanonicalName()
                    )) {
                        JCAssign assign = (JCAssign) annotation.getArguments().get(0);
                        JCFieldAccess fieldAccess = (JCFieldAccess) assign.rhs;
                        kind = getKind(fieldAccess.getIdentifier().toString());
                    }
                }
            }
            if (kind != null) {
                overloadedOperator.get(currentClass).put(kind, tree.getName());
            }
        }

        private <K, V> void computeMapWithoutOverwrites(Map<K, V> a, Map<K, V> b) {
            for (K key : b.keySet()) {
                if (!a.containsKey(key)) {
                    a.put(key, b.get(key));
                }
            }
        }
    }

    /**
     * Сканирует все новые классы и складывает их типы в дерево packages
     */
    private class PackageScanner extends TreeScanner {

        @Override
        public void visitTopLevel(JCCompilationUnit jcCompilationUnit) {
            currentPackage = jcCompilationUnit.getPackageName().toString();
            packages.computeIfAbsent(currentPackage, k -> new HashMap<>());
            currentScope = new Scope(jcCompilationUnit);
            super.visitTopLevel(jcCompilationUnit);
        }

        @Override
        public void visitClassDef(JCClassDecl jcClassDecl) {
            Type type = new Type.ClassType(Type.JCNoType.noType, null, jcClassDecl.sym);
            packages.get(currentPackage).put(getClassName(jcClassDecl), type);
            down(jcClassDecl);
            super.visitClassDef(jcClassDecl);
            up();
        }
    }

}