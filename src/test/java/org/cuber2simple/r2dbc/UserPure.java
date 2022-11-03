package org.cuber2simple.r2dbc;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserPure {

    private String name;
    private int age;
}
