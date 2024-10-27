package ru.axefu.plugin.oo;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.tree.IElementType;

import java.util.HashMap;
import java.util.Map;

/**
 * Method names
 *
 * @author Artem Moshkin
 * @since 27.10.2024
 */
public interface Methods {
    Map<IElementType, String> binary = new HashMap<>() {{
        put(JavaTokenType.PLUS, "add");
        put(JavaTokenType.MINUS, "subtract");
        put(JavaTokenType.ASTERISK, "multiply");
        put(JavaTokenType.DIV, "divide");
    }};
}
