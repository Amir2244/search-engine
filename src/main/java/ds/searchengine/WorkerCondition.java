package ds.searchengine;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

public class WorkerCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context,@NonNull AnnotatedTypeMetadata metadata) {
        return "worker".equals(context.getEnvironment().getProperty("node.role"));
    }
}
