package org.cuber2simple.r2dbc.conf;


import groovy.util.GroovyScriptEngine;
import org.cuber2simple.r2dbc.config.ScriptEngineTemplateProperties;
import org.cuber2simple.r2dbc.repository.support.ScriptEngineTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class R2dbcExtraConf {

    public static final String GROOVY_RENDER_OBJECT =
                    "class Evaluator{\n" +
                    "  def engine = new groovy.text.SimpleTemplateEngine()\n" +
                    "  def cache = new HashMap();\n" +
                    "  def nvl(obj, string){\n" +
                    "    if(obj){\n" +
                    "      string\n" +
                    "    }else{\n" +
                    "      ''\n" +
                    "    }\n" +
                    "  }\n" +
                    "  \n" +
                    "  def eval(Object...objects){\n" +
                    "    objects[1]._this = this\n" +
                    "    objects[1]._application = objects[2]\n" +
                    "    def closureWithOneArg = { temp-> engine.createTemplate(temp) }\n" +
                    "    def templateExpression = cache.computeIfAbsent(objects[0], closureWithOneArg)\n" +
                    "    templateExpression.make(objects[1])\n" +
                    "  }\n" +
                    "}\n" +
                    "def evaluator =  new Evaluator()";

    private static final String GROOVY_RENDER_METHOD = "eval";

    @Bean
    @ConditionalOnMissingBean(ScriptEngineTemplate.class)
    @ConditionalOnClass(GroovyScriptEngine.class)
    public ScriptEngineTemplate scriptEngineTemplate(ApplicationContext applicationContext) {
        ScriptEngineTemplateProperties scriptEngineTemplateProperties = new ScriptEngineTemplateProperties();
        scriptEngineTemplateProperties.setSharedEngine(true);
        scriptEngineTemplateProperties.setName("groovy");
        scriptEngineTemplateProperties.setRenderObject(GROOVY_RENDER_OBJECT);
        scriptEngineTemplateProperties.setRenderMethod(GROOVY_RENDER_METHOD);
        return new ScriptEngineTemplate(scriptEngineTemplateProperties, applicationContext);
    }
}
