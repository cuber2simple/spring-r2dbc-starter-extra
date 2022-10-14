package org.cuber2simple.r2dbc.repository.support;

import org.cuber2simple.r2dbc.annotation.DynamicQuery;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.repository.query.PartTreeR2dbcQuery;
import org.springframework.data.r2dbc.repository.query.R2dbcQueryMethod;
import org.springframework.data.r2dbc.repository.query.StringBasedR2dbcQuery;
import org.springframework.data.r2dbc.repository.query.StringDynamicR2dbcQuery;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.ReactiveQueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.r2dbc.core.DatabaseClient;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

public class R2dbcRepositoryFactoryExtra extends R2dbcRepositoryFactory {

    private static final SpelExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    private final DatabaseClient databaseClient;
    private final ReactiveDataAccessStrategy dataAccessStrategy;
    private final MappingContext<? extends RelationalPersistentEntity<?>, ? extends RelationalPersistentProperty> mappingContext;
    private final R2dbcConverter converter;
    private final R2dbcEntityOperations operations;
    private final ApplicationContext applicationContext;

    public R2dbcRepositoryFactoryExtra(DatabaseClient databaseClient, ReactiveDataAccessStrategy dataAccessStrategy, ApplicationContext applicationContext) {
        super(databaseClient, dataAccessStrategy);
        this.databaseClient = databaseClient;
        this.dataAccessStrategy = dataAccessStrategy;
        this.converter = dataAccessStrategy.getConverter();
        this.mappingContext = this.converter.getMappingContext();
        this.operations = new R2dbcEntityTemplate(this.databaseClient, this.dataAccessStrategy);
        this.applicationContext = applicationContext;
    }

    public R2dbcRepositoryFactoryExtra(R2dbcEntityOperations operations, ApplicationContext applicationContext) {
        super(operations);
        this.databaseClient = operations.getDatabaseClient();
        this.dataAccessStrategy = operations.getDataAccessStrategy();
        this.converter = dataAccessStrategy.getConverter();
        this.mappingContext = this.converter.getMappingContext();
        this.operations = operations;
        this.applicationContext = applicationContext;
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(QueryLookupStrategy.Key key, QueryMethodEvaluationContextProvider evaluationContextProvider) {
        return Optional.of(new R2dbcQueryLookupStrategy(this.operations,
                (ReactiveQueryMethodEvaluationContextProvider) evaluationContextProvider, this.converter,
                this.dataAccessStrategy, this.applicationContext));
    }

    private static class R2dbcQueryLookupStrategy implements QueryLookupStrategy {

        private final R2dbcEntityOperations entityOperations;
        private final ReactiveQueryMethodEvaluationContextProvider evaluationContextProvider;
        private final R2dbcConverter converter;
        private final ReactiveDataAccessStrategy dataAccessStrategy;
        private final ApplicationContext applicationContext;
        private final ExpressionParser parser = new CachingExpressionParser(EXPRESSION_PARSER);

        R2dbcQueryLookupStrategy(R2dbcEntityOperations entityOperations,
                                 ReactiveQueryMethodEvaluationContextProvider evaluationContextProvider, R2dbcConverter converter,
                                 ReactiveDataAccessStrategy dataAccessStrategy, ApplicationContext applicationContext) {
            this.entityOperations = entityOperations;
            this.evaluationContextProvider = evaluationContextProvider;
            this.converter = converter;
            this.dataAccessStrategy = dataAccessStrategy;
            this.applicationContext = applicationContext;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.projection.ProjectionFactory, org.springframework.data.repository.core.NamedQueries)
         */
        @Override
        public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
                                            NamedQueries namedQueries) {

            R2dbcQueryMethod queryMethod = new R2dbcQueryMethod(method, metadata, factory,
                    this.converter.getMappingContext());
            String namedQueryName = queryMethod.getNamedQueryName();
            DynamicQuery dynamicQuery = AnnotatedElementUtils.findMergedAnnotation(method, DynamicQuery.class);
            if (Objects.nonNull(dynamicQuery)) {
                return new StringDynamicR2dbcQuery(dynamicQuery, queryMethod, this.entityOperations, this.converter,
                        this.dataAccessStrategy,
                        parser, this.evaluationContextProvider, applicationContext);
            } else {
                if (namedQueries.hasQuery(namedQueryName)) {
                    String namedQuery = namedQueries.getQuery(namedQueryName);
                    return new StringBasedR2dbcQuery(namedQuery, queryMethod, this.entityOperations, this.converter,
                            this.dataAccessStrategy,
                            parser, this.evaluationContextProvider);
                } else if (queryMethod.hasAnnotatedQuery()) {
                    return new StringBasedR2dbcQuery(queryMethod, this.entityOperations, this.converter, this.dataAccessStrategy,
                            this.parser,
                            this.evaluationContextProvider);
                } else {
                    return new PartTreeR2dbcQuery(queryMethod, this.entityOperations, this.converter, this.dataAccessStrategy);
                }
            }
        }
    }


}
