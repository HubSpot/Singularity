package com.hubspot.singularity.smtp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import com.google.common.base.Throwables;
import com.google.common.io.Closeables;

public class SimpleEmailTemplate {

  private final String template;
  
  public SimpleEmailTemplate(String templateName) {
    this.template = readResourceAsString("templates/" + templateName);
  }
  
  public String render(Map<String, String> args) {
    String newTemplate = template;
    for (Entry<String, String> entry : args.entrySet()) {
      newTemplate = newTemplate.replace("{{ " + entry.getKey() + " }}", entry.getValue());
    }
    return newTemplate;
  }
  
  private final String readResourceAsString(String name) {
    InputStream inputStream = null;
    try {
      inputStream = ClassLoader.getSystemResourceAsStream(name);
      return new Scanner(inputStream).useDelimiter("\\A").next();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    } finally {
      try {
        Closeables.close(inputStream, true);
      } catch (IOException e) {}
    }
  }
  
}
