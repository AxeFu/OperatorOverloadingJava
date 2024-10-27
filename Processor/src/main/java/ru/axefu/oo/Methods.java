package ru.axefu.oo;

import java.util.HashMap;
import java.util.Map;

public interface Methods {
    Map<String, String> binary = new HashMap<String, String>() {{
        put("+", "add");
        put("-", "subtract");
        put("*", "multiply");
        put("/", "divide");
    }};
}