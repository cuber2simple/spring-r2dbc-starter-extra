package org.springframework.data.r2dbc.repository.query;

import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.dialect.BindTargetBinder;
import org.springframework.data.r2dbc.mapping.SettableValue;
import org.springframework.data.relational.repository.query.RelationalParameterAccessor;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.ReactiveQueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.spel.ExpressionDependencies;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.binding.BindTarget;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StringDynamicR2dbcQuery extends AbstractR2dbcQuery {
    private ExpressionParser expressionParser;
    private ReactiveQueryMethodEvaluationContextProvider evaluationContextProvider;

    private String originQuery;

    private ReactiveDataAccessStrategy dataAccessStrategy;

    /**
     * Creates a new {@link StringBasedR2dbcQuery} for the given {@link StringBasedR2dbcQuery}, {@link DatabaseClient},
     * {@link SpelExpressionParser}, and {@link QueryMethodEvaluationContextProvider}.
     *
     * @param queryMethod               must not be {@literal null}.
     * @param entityOperations          must not be {@literal null}.
     * @param converter                 must not be {@literal null}.
     * @param dataAccessStrategy        must not be {@literal null}.
     * @param expressionParser          must not be {@literal null}.
     * @param evaluationContextProvider must not be {@literal null}.
     */
    public StringDynamicR2dbcQuery(R2dbcQueryMethod queryMethod, R2dbcEntityOperations entityOperations,
                                   R2dbcConverter converter, ReactiveDataAccessStrategy dataAccessStrategy, ExpressionParser expressionParser,
                                   ReactiveQueryMethodEvaluationContextProvider evaluationContextProvider) {
        this(queryMethod.getRequiredAnnotatedQuery(), queryMethod, entityOperations, converter, dataAccessStrategy,
                expressionParser, evaluationContextProvider);
    }

    /**
     * Create a new {@link StringBasedR2dbcQuery} for the given {@code query}, {@link R2dbcQueryMethod},
     * {@link DatabaseClient}, {@link SpelExpressionParser}, and {@link QueryMethodEvaluationContextProvider}.
     *
     * @param method                    must not be {@literal null}.
     * @param entityOperations          must not be {@literal null}.
     * @param converter                 must not be {@literal null}.
     * @param dataAccessStrategy        must not be {@literal null}.
     * @param expressionParser          must not be {@literal null}.
     * @param evaluationContextProvider must not be {@literal null}.
     */
    public StringDynamicR2dbcQuery(String query, R2dbcQueryMethod method, R2dbcEntityOperations entityOperations,
                                   R2dbcConverter converter, ReactiveDataAccessStrategy dataAccessStrategy, ExpressionParser expressionParser,
                                   ReactiveQueryMethodEvaluationContextProvider evaluationContextProvider) {

        super(method, entityOperations, converter);
        this.expressionParser = expressionParser;
        this.evaluationContextProvider = evaluationContextProvider;

        Assert.hasText(query, "Query must not be empty");
        this.originQuery = query;
        this.dataAccessStrategy = dataAccessStrategy;
//        this.expressionQuery = ExpressionQuery.create(query);
//        this.binder = new ExpressionEvaluatingParameterBinder(expressionQuery, dataAccessStrategy);
//        this.expressionDependencies = createExpressionDependencies();
    }

    /**
     * Creates a new {@link AbstractR2dbcQuery} from the given {@link R2dbcQueryMethod} and {@link R2dbcEntityOperations}.
     *
     * @param method           must not be {@literal null}.
     * @param entityOperations must not be {@literal null}.
     * @param converter        must not be {@literal null}.
     * @since 1.4
     */
    public StringDynamicR2dbcQuery(R2dbcQueryMethod method, R2dbcEntityOperations entityOperations, R2dbcConverter converter) {
        super(method, entityOperations, converter);
    }


    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.repository.query.AbstractR2dbcQuery#isModifyingQuery()
     */
    @Override
    protected boolean isModifyingQuery() {
        return getQueryMethod().isModifyingQuery();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.repository.query.AbstractR2dbcQuery#isCountQuery()
     */
    @Override
    protected boolean isCountQuery() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.repository.query.AbstractR2dbcQuery#isExistsQuery()
     */
    @Override
    protected boolean isExistsQuery() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.r2dbc.repository.query.AbstractR2dbcQuery#createQuery(org.springframework.data.relational.repository.query.RelationalParameterAccessor)
     */
    @Override
    protected Mono<PreparedOperation<?>> createQuery(RelationalParameterAccessor accessor) {

        return init(accessor).map(evaluator -> new StringDynamicR2dbcQuery.CleanQuery(evaluator))
                .map(cleanQuery -> new StringDynamicR2dbcQuery.ExpandedQuery(accessor, cleanQuery));
    }

    @Override
    Class<?> resolveResultType(ResultProcessor resultProcessor) {
        Class<?> returnedType = resultProcessor.getReturnedType().getReturnedType();
        return !returnedType.isInterface() ? returnedType : super.resolveResultType(resultProcessor);
    }

    private Mono<R2dbcSpELExpressionEvaluator> init(RelationalParameterAccessor accessor) {

        return evaluationContextProvider
                .getEvaluationContextLater(getQueryMethod().getParameters(), accessor.getValues())
                .<R2dbcSpELExpressionEvaluator>map(
                        context -> new DefaultR2dbcSpELExpressionEvaluator(expressionParser, context))
                .defaultIfEmpty(DefaultR2dbcSpELExpressionEvaluator.unsupported());
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [").append(originQuery);
        sb.append(']');
        return sb.toString();
    }

    private class CleanQuery {

        private String query;

        private ExpressionQuery expressionQuery;

        private ExpressionEvaluatingParameterBinder binder;

        private ExpressionDependencies expressionDependencies;

        public CleanQuery(R2dbcSpELExpressionEvaluator evaluator) {
            Parameter evaluate = evaluator.evaluate(originQuery);
            query = (String) evaluate.getValue();
            this.expressionQuery = ExpressionQuery.create(query);
            this.binder = new ExpressionEvaluatingParameterBinder(expressionQuery, dataAccessStrategy);
            this.expressionDependencies = createExpressionDependencies(expressionQuery);
        }

        public String getQuery() {
            return query;
        }

        public ExpressionQuery getExpressionQuery() {
            return expressionQuery;
        }

        public ExpressionEvaluatingParameterBinder getBinder() {
            return binder;
        }

        public ExpressionDependencies getExpressionDependencies() {
            return expressionDependencies;
        }

        private ExpressionDependencies createExpressionDependencies(ExpressionQuery query) {

            if (query.getBindings().isEmpty()) {
                return ExpressionDependencies.none();
            }

            List<ExpressionDependencies> dependencies = new ArrayList<>();

            for (ExpressionQuery.ParameterBinding binding : query.getBindings()) {
                dependencies.add(ExpressionDependencies.discover(expressionParser.parseExpression(binding.getExpression())));
            }

            return ExpressionDependencies.merged(dependencies);
        }

    }

    private class ExpandedQuery implements PreparedOperation<String> {

        private final BindTargetRecorder recordedBindings;

        private final PreparedOperation<?> expanded;

        private final Map<String, Parameter> remainderByName;

        private final Map<Integer, Parameter> remainderByIndex;

        private CleanQuery cleanQuery;

        public ExpandedQuery(RelationalParameterAccessor accessor, CleanQuery cleanQuery) {

            this.recordedBindings = new BindTargetRecorder();
            EvaluationContext evaluationContext = evaluationContextProvider.getEvaluationContext(getQueryMethod().getParameters(), accessor.getValues(), cleanQuery.expressionDependencies);
            DefaultR2dbcSpELExpressionEvaluator defaultR2dbcSpELExpressionEvaluator = new DefaultR2dbcSpELExpressionEvaluator(expressionParser, evaluationContext);
            cleanQuery.binder.bind(recordedBindings, accessor, defaultR2dbcSpELExpressionEvaluator);

            remainderByName = new LinkedHashMap<>(recordedBindings.byName);
            remainderByIndex = new LinkedHashMap<>(recordedBindings.byIndex);
            expanded = dataAccessStrategy.processNamedParameters(cleanQuery.expressionQuery.getQuery(), (index, name) -> {

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
            return cleanQuery.expressionQuery.getQuery();
        }

        @Override
        public void bindTo(BindTarget target) {

            BindTargetBinder binder = new BindTargetBinder(target);
            expanded.bindTo(target);

            remainderByName.forEach(binder::bind);
            remainderByIndex.forEach(binder::bind);
        }

        @Override
        public String toQuery() {
            return expanded.toQuery();
        }

        @Override
        public String toString() {
            return String.format("Born: [%s],Original: [%s], Expanded: [%s]", originQuery, cleanQuery.expressionQuery.getQuery(), expanded.toQuery());
        }
    }

    private static class BindTargetRecorder implements BindTarget {

        final Map<Integer, Parameter> byIndex = new LinkedHashMap<>();

        final Map<String, Parameter> byName = new LinkedHashMap<>();

        @Override
        public void bind(String identifier, Object value) {
            byName.put(identifier, toParameter(value));
        }

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
