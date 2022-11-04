package org.cuber2simple.r2dbc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.r2dbc.extra")
public class R2dbcExtraProperties {

    private String scriptName;

}
