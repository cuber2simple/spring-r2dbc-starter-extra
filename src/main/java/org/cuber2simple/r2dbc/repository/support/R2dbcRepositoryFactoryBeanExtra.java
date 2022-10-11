package org.cuber2simple.r2dbc.repository.support;

import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.r2dbc.core.DatabaseClient;

import java.io.Serializable;

public class R2dbcRepositoryFactoryBeanExtra<T extends Repository<S, ID>, S, ID extends Serializable> extends R2dbcRepositoryFactoryBean<T, S, ID> {
    /**
     * Creates a new {@link R2dbcRepositoryFactoryBean} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    public R2dbcRepositoryFactoryBeanExtra(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport getFactoryInstance(DatabaseClient client, ReactiveDataAccessStrategy dataAccessStrategy) {
        return new R2dbcRepositoryFactoryExtra(client, dataAccessStrategy);
    }

    @Override
    protected RepositoryFactorySupport getFactoryInstance(R2dbcEntityOperations operations) {
        return new R2dbcRepositoryFactoryExtra(operations);
    }
}
