package ru.axefu.plugin.overloading;

import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class HighlightUtil {

    public static boolean isAssignmentCompatibleTypes(@NotNull PsiAssignmentExpression assignment) {
        PsiExpression lExpr = assignment.getLExpression();
        PsiExpression rExpr = assignment.getRExpression();
        if (rExpr == null) return false;
        PsiType lType = lExpr.getType();
        PsiType rType = getType(rExpr);
        if (rType == null || lType == null) return false;

        IElementType sign = assignment.getOperationTokenType();
        if (JavaTokenType.EQ.equals(sign)) {
            return TypeConversionUtil.isAssignable(lType, rType);
        }
        else {
            // 15.26.2. Compound Assignment Operators
            IElementType opSign = TypeConversionUtil.convertEQtoOperation(sign);
            PsiType type = getType(opSign, lType, rType);
            return type == null || TypeConversionUtil.areTypesConvertible(type, lType);
        }
    }

    public static boolean isAssignmentOperatorApplicable(@NotNull PsiAssignmentExpression assignment) {
        PsiJavaToken operationSign = assignment.getOperationSign();
        IElementType eqOpSign = operationSign.getTokenType();
        IElementType opSign = TypeConversionUtil.convertEQtoOperation(eqOpSign);
        if (opSign == null) return true;
        PsiType lType = assignment.getLExpression().getType();
        PsiExpression rExpression = assignment.getRExpression();
        if (rExpression == null) return true;
        PsiType rType = getType(rExpression);
        return getType(opSign, lType, rType) != null;
    }

    public static PsiType isPolyadicOperatorApplicable(@NotNull PsiPolyadicExpression expression) {
        PsiExpression[] operands = expression.getOperands();

        PsiType lType = operands[0].getType();
        IElementType operationSign = expression.getOperationTokenType();
        for (int i = 1; i < operands.length; i++) {
            PsiExpression operand = operands[i];
            PsiType rType = operand.getType();
            if (!TypeConversionUtil.isBinaryOperatorApplicable(operationSign, lType, rType, false)) {
                return isPolyadicOverloadedOperatorApplicable(expression);
            }
            lType = TypeConversionUtil.calcTypeForBinaryExpression(lType, rType, operationSign, true);
        }
        return lType;
    }

    private static PsiType isPolyadicOverloadedOperatorApplicable(PsiPolyadicExpression expression) {
        PsiExpression[] operands = expression.getOperands();

        PsiType lType = operands[0].getType();
        IElementType operationSign = expression.getOperationTokenType();
        for (int i = 1; i < operands.length; i++) {
            PsiExpression operand = operands[i];
            PsiType rType;
            if (operand instanceof PsiBinaryExpression polyadicExpression) {
                rType = isPolyadicOverloadedOperatorApplicable(polyadicExpression);
            } else {
                rType = operand.getType();
            }
            if ((lType = getType(operationSign, lType, rType)) == null) {
                return null;
            }
        }
        return lType;
    }

    public static PsiType getType(PsiExpression expression) {
        if (expression == null) return null;
        if (expression instanceof PsiPolyadicExpression polyadicExpression) {
            PsiType resType = HighlightUtil.isPolyadicOverloadedOperatorApplicable(polyadicExpression);
            if (resType != null) {
                return resType;
            }
        }
        return expression.getType();
    }

    private static PsiType getType(IElementType operationSign, PsiType lType, PsiType rType) {
        if (lType == null || rType == null) return null;
        Map<IElementType, Map<PsiType, PsiType>> operators = operators((PsiClassType) lType);
        if (!operators.containsKey(operationSign)) return null;
        return getReturnType(operators.get(operationSign), rType);
    }


    public static PsiType getReturnType(Map<PsiType, PsiType> methods, PsiType rType) {
        PsiType returnType = methods.get(rType);
        if (rType instanceof PsiPrimitiveType primitiveType) {
            switch (primitiveType.getName()) {
                case "char":
                    if (returnType == null) returnType = methods.get(PsiTypes.intType());
                case "int":
                    if (returnType == null) returnType = methods.get(PsiTypes.charType());
                    if (returnType == null) returnType = methods.get(PsiTypes.longType());
                    if (returnType == null) returnType = methods.get(PsiTypes.floatType());
                case "long":
                case "float":
                    if (returnType == null) returnType = methods.get(PsiTypes.doubleType());
            }
        }
        return returnType;
    }

    private static Map<IElementType, Map<PsiType, PsiType>> operators(PsiClassType operand) {
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
                            case "PLUS":
                                put(result, JavaTokenType.PLUS, argumentType, method.getReturnType());
                                break;
                            case "MINUS":
                                put(result, JavaTokenType.MINUS, argumentType, method.getReturnType());
                                break;
                            case "MULTIPLY":
                                put(result, JavaTokenType.ASTERISK, argumentType, method.getReturnType());
                                break;
                            case "DIVIDE":
                                put(result, JavaTokenType.DIV, argumentType, method.getReturnType());
                                break;
                        }
                    }
                }
            }
        }
        return result;
    }

    private static void put(
            Map<IElementType, Map<PsiType, PsiType>> map,
            IElementType operatorType,
            PsiType argumentType,
            PsiType resultType
    ) {
        Map<PsiType, PsiType> nameType = map.computeIfAbsent(operatorType, k -> new HashMap<>());
        nameType.put(argumentType, resultType);
    }

}
