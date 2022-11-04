package org.cuber2simple.r2dbc.script.support;

import lombok.Data;

@Data
public class ScriptEngineTemplateProperties {

    private String name;

    private boolean sharedEngine;

    private String[] preloadScripts;

    private String renderObject;

    private String renderMethod;

    private String resourceLoaderPath;

}
