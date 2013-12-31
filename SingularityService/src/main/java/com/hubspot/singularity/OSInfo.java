package com.hubspot.singularity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

public class OSInfo {

  private static HashMap<String, String> archMapping = new HashMap<String, String>();

  public static final String X86 = "x86";
  public static final String X86_64 = "x86_64";
  public static final String IA64_32 = "ia64_32";
  public static final String IA64 = "ia64";
  public static final String PPC = "ppc";

  static {
    // x86 mappings
    archMapping.put(X86, X86);
    archMapping.put("i386", X86);
    archMapping.put("i486", X86);
    archMapping.put("i586", X86);
    archMapping.put("i686", X86);
    archMapping.put("pentium", X86);

    // x86_64 mappings
    archMapping.put(X86_64, X86_64);
    archMapping.put("amd64", X86_64);
    archMapping.put("em64t", X86_64);
    archMapping.put("universal", X86_64); // Needed for openjdk7 in Mac

    // Itenium 64-bit mappings
    archMapping.put(IA64, IA64);
    archMapping.put("ia64w", IA64);

    // Itenium 32-bit mappings, usually an HP-UX construct
    archMapping.put(IA64_32, IA64_32);
    archMapping.put("ia64n", IA64_32);

    // PowerPC mappings
    archMapping.put(PPC, PPC);
    archMapping.put("power", PPC);
    archMapping.put("powerpc", PPC);
    archMapping.put("power_pc", PPC);
    archMapping.put("power_rs", PPC);

    // TODO: PowerPC 64bit mappings
  }


  public static void main(String[] args) {
    if (args.length >= 1) {
      if ("--os".equals(args[0])) {
        System.out.print(getOSName());
        return;
      } else if ("--arch".equals(args[0])) {
        System.out.print(getArchName());
        return;
      }
    }

    System.out.print(getNativeLibFolderPathForCurrentOS());
  }

  public static String getNativeLibFolderPathForCurrentOS() {
    return getOSName() + "/" + getArchName();
  }

  public static String getOSName() {
    return translateOSNameToFolderName(System.getProperty("os.name"));
  }

  public static String getArchName() {
    // if running Linux on ARM, need to determine ABI of JVM
    String osArch = System.getProperty("os.arch");
    if (osArch.startsWith("arm") && System.getProperty("os.name").contains("Linux")) {
      String javaHome = System.getProperty("java.home");
      try {
        // determine if first JVM found uses ARM hard-float ABI
        String[] cmdarray = {"/bin/sh", "-c", "find '" + javaHome +
                "' -name 'libjvm.so' | head -1 | xargs readelf -A | " +
                "grep 'Tag_ABI_VFP_args: VFP registers'"};
        int exitCode = Runtime.getRuntime().exec(cmdarray).waitFor();
        if (exitCode == 0)
          return "armhf";
      } catch (IOException e) {
        // ignored: fall back to "arm" arch (soft-float ABI)
      } catch (InterruptedException e) {
        // ignored: fall back to "arm" arch (soft-float ABI)
      }
    } else {
      String lc = osArch.toLowerCase(Locale.US);
      if (archMapping.containsKey(lc))
        return archMapping.get(lc);
    }
    return translateArchNameToFolderName(osArch);
  }

  static String translateOSNameToFolderName(String osName) {
    if (osName.contains("Windows")) {
      return "Windows";
    } else if (osName.contains("Mac")) {
      return "Mac";
    } else if (osName.contains("Linux")) {
      return "Linux";
    } else {
      return osName.replaceAll("\\W", "");
    }
  }

  static String translateArchNameToFolderName(String archName) {
    return archName.replaceAll("\\W", "");
  }
}