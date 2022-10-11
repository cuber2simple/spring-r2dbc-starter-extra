package org.cuber2simple.r2dbc.components;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Objects;

public class ExtraQueryFunc {

    public String nvl(Object obj, String condition) {
        String result = StringUtils.EMPTY;
        if (Objects.nonNull(obj)) {
            result = condition;
            if (obj instanceof String && StringUtils.isEmpty((String) obj)) {
                result = StringUtils.EMPTY;
            }
            if (obj instanceof Collection<?> && CollectionUtils.isEmpty((Collection<?>) obj)) {
                result = StringUtils.EMPTY;
            }
        }
        return result;
    }

}
