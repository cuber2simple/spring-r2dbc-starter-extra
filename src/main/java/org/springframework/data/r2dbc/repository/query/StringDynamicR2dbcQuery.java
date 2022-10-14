package org.springframework.data.r2dbc.repository.query;

import org.cuber2simple.r2dbc.annotation.DynamicQuery;
import org.cuber2simple.r2dbc.repository.support.ScriptEngineTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.relational.repository.query.RelationalParameterAccessor;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.ReactiveQueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.PreparedOperation;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;

public class StringDynamicR2dbcQuery extends AbstractR2dbcQuery {
    private ExpressionParser expressionParser;
    private ReactiveQueryMethodEvaluationContextProvider evaluationContextProvider;

    private ReactiveDataAccessStrategy dataAccessStrategy;

    private DynamicQuery dynamicQuery;

    private R2dbcEntityOperations entityOperations;

    private R2dbcConverter converter;

    private ScriptEngineTemplate scriptEngineTemplate;

    private String dynamicSql;

    private ApplicationContext applicationContext;


    private static final ConcurrentHashMap<String, StringBasedR2dbcQuery> STRING_BASED_R_2_DBC_QUERY_CACHE = new ConcurrentHashMap<>();

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
    public StringDynamicR2dbcQuery(DynamicQuery dynamicQuery, R2dbcQueryMethod method, R2dbcEntityOperations entityOperations,
                                   R2dbcConverter converter, ReactiveDataAccessStrategy dataAccessStrategy, ExpressionParser expressionParser,
                                   ReactiveQueryMethodEvaluationContextProvider evaluationContextProvider, ApplicationContext applicationContext) {
        super(method, entityOperations, converter);
        this.entityOperations = entityOperations;
        this.converter = converter;
        this.expressionParser = expressionParser;
        this.evaluationContextProvider = evaluationContextProvider;
        this.dynamicQuery = dynamicQuery;
        this.dataAccessStrategy = dataAccessStrategy;
        this.applicationContext = applicationContext;
        this.scriptEngineTemplate = applicationContext.getBean(ScriptEngineTemplate.class);
        this.dynamicSql = dynamicQuery.value();
        scriptEngineTemplate.precompiled(dynamicSql);
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
        return scriptEngineTemplate.eval(dynamicSql, accessor)
                .map(sql -> STRING_BASED_R_2_DBC_QUERY_CACHE.computeIfAbsent(sql, (the) -> create(the)))
                .flatMap(stringBasedR2dbcQuery -> stringBasedR2dbcQuery.createQuery(accessor));
    }


    private StringBasedR2dbcQuery create(String cleanSql) {
        return new StringBasedR2dbcQuery(cleanSql, this.getQueryMethod(), this.entityOperations, this.converter,
                this.dataAccessStrategy,
                this.expressionParser, this.evaluationContextProvider);
    }

    @Override
    Class<?> resolveResultType(ResultProcessor resultProcessor) {
        Class<?> returnedType = resultProcessor.getReturnedType().getReturnedType();
        return !returnedType.isInterface() ? returnedType : super.resolveResultType(resultProcessor);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [").append(dynamicQuery.value());
        sb.append(']');
        return sb.toString();
    }
}
