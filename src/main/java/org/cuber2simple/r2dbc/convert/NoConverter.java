package org.cuber2simple.r2dbc.convert;

import org.springframework.core.convert.converter.Converter;

/**
 * 空的converter
 */
public class NoConverter implements Converter<Integer, Integer> {
    @Override
    public Integer convert(Integer source) {
        return null;
    }
}
