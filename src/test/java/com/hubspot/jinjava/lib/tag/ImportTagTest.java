package com.hubspot.jinjava.lib.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Throwables;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.Context;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.lib.fn.MacroFunction;
import com.hubspot.jinjava.loader.ResourceLocator;


public class ImportTagTest {

  private Context context;
  private JinjavaInterpreter interpreter;
  
  @Before
  public void setup() {
    Jinjava jinjava = new Jinjava();
    jinjava.setResourceLocator(new ResourceLocator() {
      @Override
      public String getString(String fullName, Charset encoding,
          JinjavaInterpreter interpreter) throws IOException {
        return Resources.toString(
            Resources.getResource(String.format("tags/macrotag/%s", fullName)), Charsets.UTF_8);
      }
    });
    
    context = new Context();
    interpreter = new JinjavaInterpreter(jinjava, context, jinjava.getGlobalConfig());
    JinjavaInterpreter.pushCurrent(interpreter);
    
    context.put("padding", 42);
  }
  
  @After
  public void cleanup() {
    JinjavaInterpreter.popCurrent();
  }
  
  @Test
  public void importedContextExposesVars() {
    assertThat(fixture("import")).contains("wrap-padding: padding-left:42px;padding-right:42px");
  }
  
  @Test
  public void importedContextExposesMacros() {
    assertThat(fixture("import")).contains("<td height=\"42\">");
    MacroFunction fn = (MacroFunction) interpreter.resolveObject("pegasus.spacer", -1);
    assertThat(fn.getName()).isEqualTo("spacer");
    assertThat(fn.getArguments()).containsExactly("orientation", "size");
    assertThat(fn.getDefaults()).contains(entry("orientation", "h"), entry("size", 42));
  }
  
  @Test
  public void importedContextDoesntExposePrivateMacros() {
    fixture("import");
    assertThat(context.get("_private")).isNull();
  }
  
  private String fixture(String name) {
    try {
      return interpreter.renderString(Resources.toString(
              Resources.getResource(String.format("tags/macrotag/%s.jinja", name)), Charsets.UTF_8));
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

}