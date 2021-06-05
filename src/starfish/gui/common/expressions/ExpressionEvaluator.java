package starfish.gui.common.expressions;

import java.util.Map;

public class ExpressionEvaluator {

    private static final Map<String, Float> CONSTANTS = Map.of(
            "e", (float) Math.E,
            "pi", (float) Math.PI, "π", (float) Math.PI);

    public float evaluate(String expression, Map<String, Float> vars) {
        return evaluate(new CompiledExpression(expression), vars);
    }
    public Float evaluate(CompiledExpression expression, Map<String, Float> vars) {
        return null;
    }

}
