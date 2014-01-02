package com.hubspot.singularity;

import org.apache.mesos.MesosNativeLibrary;

import java.io.*;
import java.net.URL;
import java.util.UUID;

public class MesosNativeLoader {
  public static final String KEY_SINGULARITY_MESOS_TEMPDIR = "singularity.mesos.tempdir";
  public static final String KEY_SINGULARITY_MESOS_USE_SYSTEMLIB = "singularity.mesos.use.systemlib";

  private static File nativeLibFile = null;

  static void cleanUpExtractedNativeLib() {
    if (nativeLibFile != null && nativeLibFile.exists())
      nativeLibFile.delete();
  }

  /**
   * Load a native library of mesos
   */
  private static void loadNativeLibrary() {

    nativeLibFile = findNativeLibrary();
    if (nativeLibFile != null) {
      System.out.println(nativeLibFile.getAbsolutePath());
    }
  }


  private static boolean contentsEquals(InputStream in1, InputStream in2) throws IOException {
    if (!(in1 instanceof BufferedInputStream)) {
      in1 = new BufferedInputStream(in1);
    }
    if (!(in2 instanceof BufferedInputStream)) {
      in2 = new BufferedInputStream(in2);
    }

    int ch = in1.read();
    while (ch != -1) {
      int ch2 = in2.read();
      if (ch != ch2)
        return false;
      ch = in1.read();
    }
    int ch2 = in2.read();
    return ch2 == -1;
  }

  /**
   * Extract the specified library file to the target folder
   *
   * @param libFolderForCurrentOS
   * @param libraryFileName
   * @param targetFolder
   * @return
   */
  private static File extractLibraryFile(String libFolderForCurrentOS, String libraryFileName, String targetFolder) {
    String nativeLibraryFilePath = libFolderForCurrentOS + "/" + libraryFileName;

    // Attach UUID to the native library file to ensure multiple class loaders can read the libmesos multiple times.
    String uuid = UUID.randomUUID().toString();
    String extractedLibFileName = String.format("mesos-%s-%s", uuid, libraryFileName);
    File extractedLibFile = new File(targetFolder, extractedLibFileName);

    try {
      // Extract a native library file into the target directory
      InputStream reader = MesosNativeLoader.class.getResourceAsStream(nativeLibraryFilePath);
      FileOutputStream writer = new FileOutputStream(extractedLibFile);
      try {
        byte[] buffer = new byte[8192];
        int bytesRead = 0;
        while ((bytesRead = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, bytesRead);
        }
      } finally {
        // Delete the extracted lib file on JVM exit.
        extractedLibFile.deleteOnExit();

        if (writer != null)
          writer.close();
        if (reader != null)
          reader.close();
      }

      // Set executable (x) flag to enable Java to load the native library
      extractedLibFile.setReadable(true);
      extractedLibFile.setWritable(true, true);
      extractedLibFile.setExecutable(true);


      // Check whether the contents are properly copied from the resource folder
      {
        InputStream nativeIn = MesosNativeLoader.class.getResourceAsStream(nativeLibraryFilePath);
        InputStream extractedLibIn = new FileInputStream(extractedLibFile);
        try {
          if (!contentsEquals(nativeIn, extractedLibIn))
            throw new RuntimeException(String.format("Failed to write a native library file at %s", extractedLibFile));
        } finally {
          if (nativeIn != null)
            nativeIn.close();
          if (extractedLibIn != null)
            extractedLibIn.close();
        }
      }

      return new File(targetFolder, extractedLibFileName);
    } catch (IOException e) {
      e.printStackTrace(System.err);
      return null;
    }
  }

  static File findNativeLibrary() {

    boolean useSystemLib = Boolean.parseBoolean(System.getProperty(KEY_SINGULARITY_MESOS_USE_SYSTEMLIB, "false"));
    if (useSystemLib)
      return null; // Use a pre-installed libmesos

    // Try to load the library from the path set in MESOS_NATIVE_LIBRARY
    String mesosNativeLibraryFullPath = System.getenv("MESOS_NATIVE_LIBRARY");
    if (mesosNativeLibraryFullPath != null) {
      File nativeLib = new File(mesosNativeLibraryFullPath);
      if (nativeLib.exists()) {
        return nativeLib;
      }
    }

    String mesosNativeLibraryName = System.mapLibraryName("mesos");

    // Load an OS-dependent native library inside a jar file
    String mesosNativeLibraryPath = "/native/" + OSInfo.getNativeLibFolderPathForCurrentOS();
    boolean hasNativeLib = hasResource(mesosNativeLibraryPath + "/" + mesosNativeLibraryName);
    if (!hasNativeLib) {
      if (OSInfo.getOSName().equals("Mac")) {
        // Fix for openjdk7 for Mac
        String altName = "libmesos.jnilib";
        if (hasResource(mesosNativeLibraryPath + "/" + altName)) {
          mesosNativeLibraryName = altName;
          hasNativeLib = true;
        }
      }
    }

    if (!hasNativeLib) {
      throw new RuntimeException(String.format("no native library is found for os.name=%s and os.arch=%s", OSInfo.getOSName(), OSInfo.getArchName()));
    } else {
      URL url = MesosNativeLoader.class.getResource(mesosNativeLibraryPath + "/" + mesosNativeLibraryName);
      if (url != null && url.getFile() != null) {
        File localNativeLib = new File(url.getFile());
        if (localNativeLib.exists()) {
          return localNativeLib;
        }
      }
    }

    String tempFolder = new File(System.getProperty(KEY_SINGULARITY_MESOS_TEMPDIR, System.getProperty("java.io.tmpdir"))).getAbsolutePath();

    return extractLibraryFile(mesosNativeLibraryPath, mesosNativeLibraryName, tempFolder);
  }

  private static boolean hasResource(String path) {
    return MesosNativeLoader.class.getResource(path) != null;
  }

  public static void load() {
    File library = findNativeLibrary();
    if(library != null) {
      System.out.println(String.format("Loading Mesos Library found at: %s", library.getAbsolutePath()));
      MesosNativeLibrary.load(library.getAbsolutePath());
    }
  }
}
