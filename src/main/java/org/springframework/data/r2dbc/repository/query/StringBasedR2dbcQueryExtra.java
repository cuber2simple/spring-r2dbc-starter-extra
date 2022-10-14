package org.springframework.data.r2dbc.repository.query;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.dialect.BindTargetBinder;
import org.springframework.data.r2dbc.mapping.SettableValue;
import org.springframework.data.relational.repository.query.RelationalParameterAccessor;
import org.springframework.data.repository.query.ReactiveQueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.expression.ExpressionParser;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.binding.BindTarget;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StringBasedR2dbcQueryExtra extends StringBasedR2dbcQuery {

    private final ExpressionQuery expressionQuery;
    private final ExpressionEvaluatingParameterBinder binder;
    private final ExpressionParser expressionParser;
    private final ReactiveQueryMethodEvaluationContextProvider evaluationContextProvider;
    private final ExpressionDependencies expressionDependencies;
    private final ReactiveDataAccessStrategy dataAccessStrategy;

    public StringBasedR2dbcQueryExtra(String query, R2dbcQueryMethod method, R2dbcEntityOperations entityOperations, R2dbcConverter converter, ReactiveDataAccessStrategy dataAccessStrategy, ExpressionParser expressionParser, ReactiveQueryMethodEvaluationContextProvider evaluationContextProvider) {
        super(query, method, entityOperations, converter, dataAccessStrategy, expressionParser, evaluationContextProvider);
        this.expressionParser = expressionParser;
        this.evaluationContextProvider = evaluationContextProvider;

        Assert.hasText(query, "Query must not be empty");

        this.dataAccessStrategy = dataAccessStrategy;
        this.expressionQuery = ExpressionQuery.create(query);
        this.binder = new ExpressionEvaluatingParameterBinder(expressionQuery, dataAccessStrategy);
        this.expressionDependencies = createExpressionDependencies();
    }

    private ExpressionDependencies createExpressionDependencies() {

        if (expressionQuery.getBindings().isEmpty()) {
            return ExpressionDependencies.none();
        }

        List<ExpressionDependencies> dependencies = new ArrayList<>();

        for (ExpressionQuery.ParameterBinding binding : expressionQuery.getBindings()) {
            dependencies.add(ExpressionDependencies.discover(expressionParser.parseExpression(binding.getExpression())));
        }

        return ExpressionDependencies.merged(dependencies);
    }

    @Override
    protected Mono<PreparedOperation<?>> createQuery(RelationalParameterAccessor accessor) {
        return getSpelEvaluator(accessor).map(evaluator -> new ExpandedQuery(accessor, evaluator));
    }

    @Override
    Class<?> resolveResultType(ResultProcessor resultProcessor) {

        Class<?> returnedType = resultProcessor.getReturnedType().getReturnedType();
        return !returnedType.isInterface() ? returnedType : super.resolveResultType(resultProcessor);
    }

    private Mono<R2dbcSpELExpressionEvaluator> getSpelEvaluator(RelationalParameterAccessor accessor) {

        return evaluationContextProvider
                .getEvaluationContextLater(getQueryMethod().getParameters(), accessor.getValues(), expressionDependencies)
                .<R2dbcSpELExpressionEvaluator>map(
                        context -> new DefaultR2dbcSpELExpressionEvaluator(expressionParser, context))
                .defaultIfEmpty(DefaultR2dbcSpELExpressionEvaluator.unsupported());
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [").append(expressionQuery.getQuery());
        sb.append(']');
        return sb.toString();
    }

    private class ExpandedQuery implements PreparedOperation<String> {

        private final BindTargetRecorder recordedBindings;

        private final PreparedOperation<?> expanded;

        private final Map<String, Parameter> remainderByName;

        private final Map<Integer, Parameter> remainderByIndex;

        public ExpandedQuery(RelationalParameterAccessor accessor, R2dbcSpELExpressionEvaluator evaluator) {

            this.recordedBindings = new BindTargetRecorder();
            binder.bind(recordedBindings, accessor, evaluator);

            remainderByName = new LinkedHashMap<>(recordedBindings.byName);
            remainderByIndex = new LinkedHashMap<>(recordedBindings.byIndex);
            expanded = dataAccessStrategy.processNamedParameters(expressionQuery.getQuery(), (index, name) -> {

                if (recordedBindings.byName.containsKey(name)) {
                    remainderByName.remove(name);
                    return SettableValue.fromParameter(recordedBindings.byName.get(name));
                }

                if (recordedBindings.byIndex.containsKey(index)) {
                    remainderByIndex.remove(index);
                    return SettableValue.fromParameter(recordedBindings.byIndex.get(index));
                }

                return null;
            });
        }

        @Override
        public String getSource() {
            return expressionQuery.getQuery();
        }

        @Override
        public void bindTo(BindTarget target) {
            expanded.bindTo(target);
        }

        @Override
        public String toQuery() {
            return expanded.toQuery();
        }

        @Override
        public String toString() {
            return String.format("Original: [%s], Expanded: [%s]", expressionQuery.getQuery(), expanded.toQuery());
        }
    }

    private static class BindTargetRecorder implements BindTarget {

        final Map<Integer, Parameter> byIndex = new LinkedHashMap<>();

        final Map<String, Parameter> byName = new LinkedHashMap<>();

        @Override
        public void bind(String identifier, Object value) {
            byName.put(identifier, toParameter(value));
        }

        @NotNull
        private Parameter toParameter(Object value) {

            if (value instanceof SettableValue) {
                return ((SettableValue) value).toParameter();
            }

            return value instanceof Parameter ? (Parameter) value : Parameter.from(value);
        }

        @Override
        public void bind(int index, Object value) {
            byIndex.put(index, toParameter(value));
        }

        @Override
        public void bindNull(String identifier, Class<?> type) {
            byName.put(identifier, Parameter.empty(type));
        }

        @Override
        public void bindNull(int index, Class<?> type) {
            byIndex.put(index, Parameter.empty(type));
        }
    }
}
