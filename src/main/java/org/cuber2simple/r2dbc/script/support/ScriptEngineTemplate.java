package org.cuber2simple.r2dbc.script.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.cuber2simple.r2dbc.script.support.ScriptEngineTemplateProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.data.relational.repository.query.RelationalParameterAccessor;
import org.springframework.data.repository.query.Parameter;
import org.springframework.scripting.support.StandardScriptEvalException;
import org.springframework.scripting.support.StandardScriptUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.script.*;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ScriptEngineTemplate {

    private ScriptEngineTemplateProperties scriptEngineTemplateProperties;

    private ApplicationContext applicationContext;

    private ScriptEngineManager scriptEngineManager;

    private ScriptEngine scriptEngine;

    private String[] resourceLoaderPaths;

    private Object renderObject;

    private static final ConcurrentHashMap<String, CompiledScript> COMPILED_SCRIPT_CACHE = new ConcurrentHashMap<>();

    public ScriptEngineTemplate(ScriptEngineTemplateProperties scriptEngineTemplateProperties, ApplicationContext applicationContext) {
        this.scriptEngineTemplateProperties = scriptEngineTemplateProperties;
        this.applicationContext = applicationContext;
        //设置加载路径
        setResourceLoaderPaths(scriptEngineTemplateProperties.getResourceLoaderPath());
        //初始化引擎
        initEngine();
        if (StringUtils.hasText(this.scriptEngineTemplateProperties.getRenderObject())) {
            ScriptContext scriptContext = new SimpleScriptContext();
            ScriptEngine engine = createEngine();
            renderObject = eval(engine, this.scriptEngineTemplateProperties.getRenderObject(), scriptContext);
        }

    }

    private void setResourceLoaderPaths(String resourceLoaderPath) {
        String[] paths = StringUtils.commaDelimitedListToStringArray(resourceLoaderPath);
        this.resourceLoaderPaths = new String[paths.length + 1];
        this.resourceLoaderPaths[0] = "";
        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];
            if (!path.endsWith("/") && !path.endsWith(":")) {
                path = path + "/";
            }
            this.resourceLoaderPaths[i + 1] = path;
        }
    }

    private Object eval(ScriptEngine scriptEngine, String script, ScriptContext scriptContext) {
        Object result = null;
        try {
            CompiledScript compiledScript = COMPILED_SCRIPT_CACHE.get(script);
            if (Objects.nonNull(compiledScript)) {
                result = compiledScript.eval(scriptContext);
            } else {
                result = scriptEngine.eval(script, scriptContext);
            }
        } catch (Exception e) {
            log.error("执行脚本出错");
        }
        return result;
    }

    /**
     * 预编译script
     *
     * @param script
     */
    public void precompiled(String script) {
        if (Objects.nonNull(scriptEngine) && !StringUtils.hasText(this.scriptEngineTemplateProperties.getRenderMethod())) {
            try {
                COMPILED_SCRIPT_CACHE.computeIfAbsent(script, (sc) -> {
                    try {
                        return ((Compilable) scriptEngine).compile(script);
                    } catch (ScriptException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                log.error("编译脚本失败", e);
            }
        }
    }

    private void initEngine() {
        if (this.scriptEngineTemplateProperties.isSharedEngine()) {
            this.scriptEngine = createEngine();
        }
    }

    private ScriptEngine createEngine() {
        ScriptEngineManager scriptEngineManager = this.scriptEngineManager;
        if (scriptEngineManager == null) {
            scriptEngineManager = new ScriptEngineManager(applicationContext.getClassLoader());
            this.scriptEngineManager = scriptEngineManager;
        }

        ScriptEngine engine = StandardScriptUtils.retrieveEngineByName(scriptEngineManager, scriptEngineTemplateProperties.getName());
        loadScripts(engine);
        return engine;
    }


    protected void loadScripts(ScriptEngine engine) {
        if (ArrayUtils.isNotEmpty(scriptEngineTemplateProperties.getPreloadScripts())) {
            for (String script : this.scriptEngineTemplateProperties.getPreloadScripts()) {
                Resource resource = getResource(script);
                if (resource == null) {
                    throw new IllegalStateException("Script resource [" + script + "] not found");
                }
                try {
                    engine.eval(new InputStreamReader(resource.getInputStream()));
                } catch (Throwable ex) {
                    throw new IllegalStateException("Failed to evaluate script [" + script + "]", ex);
                }
            }
        }
    }

    protected Resource getResource(String location) {
        if (this.resourceLoaderPaths != null) {
            for (String path : this.resourceLoaderPaths) {
                Resource resource = applicationContext.getResource(path + location);
                if (resource.exists()) {
                    return resource;
                }
            }
        }
        return null;
    }

    private ScriptEngine getEngine() {
        if (!this.scriptEngineTemplateProperties.isSharedEngine()) {
            return createEngine();
        } else {
            return this.scriptEngine;
        }
    }

    public Mono<String> eval(String sql, RelationalParameterAccessor accessor) {
        return Mono.create(monoSink -> monoSink.success(getEngine()))
                .cast(ScriptEngine.class).map(scriptEngine1 -> {
                    try {
                        Object html = null;
                        ScriptContext scriptContext = of(accessor);
                        Map<String, Object> map = toMap(accessor);
                        if (this.scriptEngineTemplateProperties.getRenderMethod() == null) {
                            html = eval(scriptEngine1, sql, scriptContext);
                        } else if (renderObject != null) {
                            html = ((Invocable) scriptEngine1).invokeMethod(renderObject, this.scriptEngineTemplateProperties.getRenderMethod(), sql, map, applicationContext);
                        } else {
                            html = ((Invocable) scriptEngine1).invokeFunction(this.scriptEngineTemplateProperties.getRenderMethod(), sql, map, applicationContext);
                        }
                        return String.valueOf(html);
                    } catch (ScriptException ex) {
                        throw new IllegalStateException("Failed to render script template", new StandardScriptEvalException(ex));
                    } catch (Exception ex) {
                        throw new IllegalStateException("Failed to render script template", ex);
                    }

                });
    }

    private Map<String, Object> toMap(RelationalParameterAccessor accessor) {
        Map<String, Object> stringObjectMap = new HashMap<>();
        Object[] values = accessor.getValues();
        for (Parameter bindableParameter : accessor.getBindableParameters()) {
            Optional<String> name = bindableParameter.getName();
            if (name.isPresent()) {
                stringObjectMap.put(name.get(), values[bindableParameter.getIndex()]);
            }
        }
        return stringObjectMap;
    }

    private ScriptContext of(RelationalParameterAccessor accessor) {
        ScriptContext scriptContext = new SimpleScriptContext();
        Object[] values = accessor.getValues();
        scriptContext.setAttribute("_application", this.applicationContext, ScriptContext.GLOBAL_SCOPE);
        for (Parameter bindableParameter : accessor.getBindableParameters()) {
            Optional<String> name = bindableParameter.getName();
            if (name.isPresent()) {
                scriptContext.setAttribute(name.get(), values[bindableParameter.getIndex()], ScriptContext.GLOBAL_SCOPE);
            }
        }
        return scriptContext;
    }


}
