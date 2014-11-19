package com.hubspot.singularity.runner.base.shared;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SimpleProcessExecutor {

  public List<String> getProcessOutput(String... cmd) {
    ProcessBuilder pb = new ProcessBuilder(cmd);

    try {
      Process process = pb.start();
      process.waitFor();

      return consumeStreamAsLines(process.getInputStream());
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  private List<String> consumeStreamAsLines(InputStream is) throws IOException {
    List<String> lines = new ArrayList<>();

    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, UTF_8))) {
      String line = br.readLine();
      while (line != null) {
        lines.add(line);
        line = br.readLine();
      }
    }

    return lines;
  }

}
