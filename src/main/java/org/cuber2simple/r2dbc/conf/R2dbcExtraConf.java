package org.cuber2simple.r2dbc.conf;

import org.cuber2simple.r2dbc.components.ExtraQueryFunc;
import org.cuber2simple.r2dbc.repository.support.R2dbcRepositoryFactoryBeanExtra;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(repositoryBaseClass = R2dbcRepositoryFactoryBeanExtra.class)
public class R2dbcExtraConf {

    @Bean("queryFunc")
    public ExtraQueryFunc extraQueryFunc() {
        return new ExtraQueryFunc();
    }
}
