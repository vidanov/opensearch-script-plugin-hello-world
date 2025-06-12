package com.example;

import org.junit.Test;
import org.opensearch.common.settings.Settings;
import org.opensearch.script.ScriptContext;
import org.opensearch.script.ScriptEngine;
import org.opensearch.script.FieldScript;
import org.opensearch.script.ScoreScript;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for HelloWorldScriptPlugin with GenAI scoring
 */
public class HelloWorldScriptPluginTest {

    @Test
    public void testPluginCreation() {
        HelloWorldScriptPlugin plugin = new HelloWorldScriptPlugin();
        assertNotNull("Plugin should be created successfully", plugin);
    }

    @Test
    public void testScriptEngineCreation() {
        HelloWorldScriptPlugin plugin = new HelloWorldScriptPlugin();
        Set<ScriptContext<?>> contexts = Set.of(FieldScript.CONTEXT, ScoreScript.CONTEXT);
        
        ScriptEngine engine = plugin.getScriptEngine(Settings.EMPTY, contexts);
        
        assertNotNull("Script engine should be created", engine);
        assertEquals("Script engine type should be 'hello_world'", "hello_world", engine.getType());
    }

    @Test
    public void testSupportedContexts() {
        HelloWorldScriptPlugin.HelloWorldScriptEngine engine = new HelloWorldScriptPlugin.HelloWorldScriptEngine();
        Set<ScriptContext<?>> supportedContexts = engine.getSupportedContexts();
        
        assertTrue("Should support FIELD context", supportedContexts.contains(FieldScript.CONTEXT));
        assertTrue("Should support SCORE context", supportedContexts.contains(ScoreScript.CONTEXT));
        assertEquals("Should support exactly two contexts", 2, supportedContexts.size());
    }

    @Test
    public void testFieldScriptFactory() {
        HelloWorldScriptPlugin.HelloWorldFieldScriptFactory factory = 
            new HelloWorldScriptPlugin.HelloWorldFieldScriptFactory("hello test");
        
        assertNotNull("Field script factory should be created", factory);
    }

    @Test
    public void testGenAIScoreScriptFactory() {
        HelloWorldScriptPlugin.GenAIScoreScriptFactory factory = 
            new HelloWorldScriptPlugin.GenAIScoreScriptFactory("custom_score");
        
        assertNotNull("GenAI score script factory should be created", factory);
    }

    @Test
    public void testScriptEngineCompilation() {
        HelloWorldScriptPlugin.HelloWorldScriptEngine engine = new HelloWorldScriptPlugin.HelloWorldScriptEngine();
        
        // Test field script compilation
        try {
            Object fieldFactory = engine.compile("test", "hello", FieldScript.CONTEXT, Collections.emptyMap());
            assertNotNull("Field script should compile successfully", fieldFactory);
            assertTrue("Should return FieldScript.Factory", fieldFactory instanceof FieldScript.Factory);
        } catch (Exception e) {
            fail("Field script compilation should not throw exception: " + e.getMessage());
        }
        
        // Test score script compilation
        try {
            Object scoreFactory = engine.compile("test", "custom_score", ScoreScript.CONTEXT, Collections.emptyMap());
            assertNotNull("Score script should compile successfully", scoreFactory);
            assertTrue("Should return ScoreScript.Factory", scoreFactory instanceof ScoreScript.Factory);
        } catch (Exception e) {
            fail("Score script compilation should not throw exception: " + e.getMessage());
        }
    }

    @Test
    public void testSupportedContextsValidation() {
        HelloWorldScriptPlugin.HelloWorldScriptEngine engine = new HelloWorldScriptPlugin.HelloWorldScriptEngine();
        
        // Test that compilation works for supported contexts
        try {
            Object fieldFactory = engine.compile("test", "hello", FieldScript.CONTEXT, Collections.emptyMap());
            assertNotNull("Field script should compile", fieldFactory);
            
            Object scoreFactory = engine.compile("test", "custom_score", ScoreScript.CONTEXT, Collections.emptyMap());
            assertNotNull("Score script should compile", scoreFactory);
        } catch (Exception e) {
            fail("Supported contexts should compile without exception: " + e.getMessage());
        }
    }
}
