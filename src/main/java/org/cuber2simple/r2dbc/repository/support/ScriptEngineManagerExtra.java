package org.cuber2simple.r2dbc.repository.support;


import lombok.extern.slf4j.Slf4j;
import org.cuber2simple.r2dbc.annotation.DynamicQuery;
import org.springframework.data.relational.repository.query.RelationalParameterAccessor;
import org.springframework.data.repository.query.Parameter;

import javax.script.*;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class ScriptEngineManagerExtra {

    private static final ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();

    private static final ConcurrentHashMap<String, ScriptEngine> SCRIPT_ENGINE_CACHE = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<DynamicQuery, CompiledScript> COMPILED_SCRIPT_CACHE = new ConcurrentHashMap<>();

    private static ScriptEngine fetch(String lang) {
        return SCRIPT_ENGINE_CACHE.computeIfAbsent(lang, (shortName) -> SCRIPT_ENGINE_MANAGER.getEngineByName(shortName));
    }


    private static CompiledScript fetch(String lang, String script) {
        ScriptEngine scriptEngine = fetch(lang);
        CompiledScript compiled = null;
        try {
            compiled = ((Compilable) scriptEngine).compile(script);
        } catch (Exception e) {
            log.error("编译脚本失败", e);
        }
        return compiled;
    }

    public static CompiledScript fetch(DynamicQuery dynamicQuery) {
        return COMPILED_SCRIPT_CACHE.computeIfAbsent(dynamicQuery, (query) -> fetch(query.lang(), query.value()));
    }

    public static String cleanQuery(CompiledScript compiledScript, RelationalParameterAccessor accessor) {
        try {
            return (String) compiledScript.eval(of(accessor));
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    private static ScriptContext of(RelationalParameterAccessor accessor) {
        ScriptContext scriptContext = new SimpleScriptContext();
        Object[] values = accessor.getValues();
        for (Parameter bindableParameter : accessor.getBindableParameters()) {
            Optional<String> name = bindableParameter.getName();
            if (name.isPresent()) {
                scriptContext.setAttribute(name.get(), values[bindableParameter.getIndex()], ScriptContext.ENGINE_SCOPE);
            }
        }
        return scriptContext;
    }


}
