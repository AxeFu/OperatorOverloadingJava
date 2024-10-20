package ru.axefu.plugin.overloading;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Фильтер подсветки ошибок
 *
 * @author Artem Moshkin
 * @since 13.10.2024
 */
public class BinaryHighlightErrorFilter implements HighlightInfoFilter {

    private PsiType incompatibleCheckType = null;
    private int checkTypePosition;

    @Override
    public boolean accept(@NotNull HighlightInfo highlightInfo, @Nullable PsiFile file) {
        if (file == null) return true;
        PsiElement element = getPsiElement(highlightInfo, file);
        //Дичайший костыль, но делать по другому пока лень
        if ((element instanceof PsiAssignmentExpression) ||
                (element instanceof PsiReturnStatement) &&
                incompatibleCheckType != null &&
                checkTypePosition > highlightInfo.getStartOffset()) {

            String foundType = incompatibleCheckType.getCanonicalText();
            incompatibleCheckType = null;
            checkTypePosition = -1;
            String searchMsg = "required: '";
            String description = highlightInfo.getDescription();
            if (description == null) {
                return true;
            }
            int startIndex = description.indexOf(searchMsg);
            if (startIndex == -1) {
                return true;
            }
            startIndex += searchMsg.length();
            String requiredType = highlightInfo.getDescription().substring(startIndex,
                    Math.min(startIndex + foundType.length(), highlightInfo.getDescription().length())
            );
            if (requiredType.equals(foundType)) {
                return false;
            }
        }
        //Тут всё ок, костыля нет, просто проверка в лоб
        if (element instanceof PsiPolyadicExpression expression) {
            if ((incompatibleCheckType = getType(expression)) != null) {
                checkTypePosition = expression.getTextRange().getStartOffset();
                return false;
            }
        }
        return true;
    }

    //Та самая проверка в лоб
    private PsiType getType(PsiPolyadicExpression expression) {
        PsiExpression[] operands = expression.getOperands();
        IElementType operationSign = expression.getOperationTokenType();
        PsiType lType = operands[0].getType();
        for (int i = 1; i < operands.length; i++) {
            if (lType == null) return null;
            Map<IElementType, Map<PsiType, PsiType>> operators = operators((PsiClassType) lType);
            PsiType rType = operands[i].getType();
            if (operands[i] instanceof PsiPolyadicExpression operand) {
                rType = getType(operand);
            }
            if (operators.get(operationSign) == null) return null;
            lType = operators.get(operationSign).get(rType);
            if (rType instanceof PsiPrimitiveType primitiveType) {
                switch (primitiveType.getName()) {
                    case "char": if (lType == null) lType = operators.get(operationSign).get(PsiTypes.intType());
                    case "int": if (lType == null) lType = operators.get(operationSign).get(PsiTypes.longType());
                    case "long": if (lType == null) lType = operators.get(operationSign).get(PsiTypes.floatType());
                    case "float": if (lType == null) lType = operators.get(operationSign).get(PsiTypes.doubleType());
                }
            }
            if (lType == null) return null;
        }
        return lType;
    }

    private Map<IElementType, Map<PsiType, PsiType>> operators(PsiClassType operand) {
        Map<IElementType, Map<PsiType, PsiType>> result = new HashMap<>();
        PsiClass lClazz = operand.resolve();
        if (lClazz == null) return result;
        PsiMethod[] psiMethod = lClazz.getMethods();
        for (PsiMethod method : psiMethod) {
            PsiElement[] modifiers = method.getModifierList().getChildren();
            PsiParameterList parameterList = method.getParameterList();
            if (parameterList.isEmpty() || parameterList.getParametersCount() != 1) continue;
            PsiParameter parameter = parameterList.getParameter(0);
            if (parameter == null) continue;
            PsiType argumentType = parameter.getType();
            for (PsiElement modifier : modifiers) {
                if (modifier instanceof PsiAnnotation annotation) {
                    if (Objects.equals(annotation.getQualifiedName(), "ru.axefu.overloading.annotation.Operator")) {
                        PsiAnnotationParameterList annotationParameterList = annotation.getParameterList();
                        PsiNameValuePair[] psiNameValuePair = annotationParameterList.getAttributes();
                        if (psiNameValuePair.length == 0) continue;
                        PsiAnnotationMemberValue memberValue = psiNameValuePair[0].getDetachedValue();
                        if (memberValue == null) continue;
                        PsiReference typeReference = memberValue.getReference();
                        if (typeReference == null) continue;
                        PsiField attributeField = (PsiField) typeReference.resolve();
                        if (attributeField == null) continue;
                        switch (attributeField.getName()) {
                            case "PLUS": put(result, JavaTokenType.PLUS, argumentType, method.getReturnType()); break;
                            case "MINUS": put(result, JavaTokenType.MINUS, argumentType, method.getReturnType()); break;
                            case "MULTIPLY": put(result, JavaTokenType.ASTERISK, argumentType, method.getReturnType()); break;
                            case "DIVIDE": put(result, JavaTokenType.DIV, argumentType, method.getReturnType()); break;
                        }
                    }
                }
            }
        }
        return result;
    }

    private void put(
            Map<IElementType, Map<PsiType, PsiType>> map,
            IElementType operatorType,
            PsiType argumentType,
            PsiType resultType
    ) {
        Map<PsiType, PsiType> nameType = map.computeIfAbsent(operatorType, k -> new HashMap<>());
        nameType.put(argumentType, resultType);
    }

    private PsiElement getPsiElement(HighlightInfo highlightInfo, PsiElement file) {
        int startOffset = highlightInfo.startOffset;
        int endOffset = highlightInfo.endOffset;
        LinkedList<PsiElement> list = new LinkedList<>();
        list.add(file);
        while (!list.isEmpty()) {
            for (PsiElement child : list.getFirst().getChildren()) {
                TextRange range = child.getTextRange();
                if (range.getStartOffset() == startOffset && range.getEndOffset() == endOffset) {
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
