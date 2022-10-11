package org.cuber2simple.r2dbc.repository.support;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachingExpressionParser implements ExpressionParser {
    private final ExpressionParser delegate;
    private final Map<String, Expression> cache = new ConcurrentHashMap<>();

    CachingExpressionParser(ExpressionParser delegate) {
        this.delegate = delegate;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.expression.ExpressionParser#parseExpression(java.lang.String)
     */
    @Override
    public Expression parseExpression(String expressionString) throws ParseException {
        return cache.computeIfAbsent(expressionString, delegate::parseExpression);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.expression.ExpressionParser#parseExpression(java.lang.String, org.springframework.expression.ParserContext)
     */
    @Override
    public Expression parseExpression(String expressionString, ParserContext context) throws ParseException {
        throw new UnsupportedOperationException("Parsing using ParserContext is not supported");
    }
}
