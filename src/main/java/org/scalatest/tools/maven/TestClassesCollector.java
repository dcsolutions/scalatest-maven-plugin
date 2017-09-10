package org.scalatest.tools.maven;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TestClassesCollector {
  
  private final String path;
  
  public TestClassesCollector(String path) {
    this.path = path;
  }
  
  private List<String> collect( String path ) {
    return collect(path, new ArrayList<String>());
  }
  private List<String> collect(String path, List<String> collected) {
    File root = new File( path );
    File[] list = root.listFiles();
    if (list == null) return collected;
    for ( File f : list ) {
      if ( f.isDirectory() ) {
        collected = collect( f.getAbsolutePath(), collected );
      }
      else {
        collected.add(f.getAbsoluteFile().getAbsolutePath());
      }
    }
    return collected;
  }
  
  public List<String> testClasses() {
    ArrayList<String> classes = new ArrayList<String>();
    Iterator<String> paths = collect(path).iterator();
    while (paths.hasNext()) {
      String classPath = paths.next();
      if (classPath.indexOf("$") > -1) {
        classPath = classPath.substring(0, classPath.indexOf("$"));
      }
      String classToAdd = classPath
              .replaceAll(path, "")
              .replaceAll(".class", "")
              .replace(File.separatorChar, '.');
      if (!classes.contains(classToAdd)) {
        classes.add(classToAdd);
      }
    }
    Collections.sort(classes);
    return classes;
  }
  
  /**
   * Passes a fully qualified class name to a tester class and checks if the class is an instance of scalatest Suite.
   * @param log mojo logger
   * @param baseDir project's base directory
   * @param classPath classpath to use
   * @param testOutputDirectory URL to the compiled test classes directory, usually target/test-classes
   * @param cls a fully qualified class name
   * @return true if the class is a test suite, false otherwise
   */
  public boolean isClassATestSuite(Log log, String baseDir, String classPath, String testOutputDirectory, String cls)
  throws MojoFailureException {
    
    // we can't load classes from the classpath in here, maven will be upset and fail with etrn imports errors
    // and we can't reference scalatest from here as it isn't on the maven classpath
    // so we need to fork again but whatever...
    
    final Commandline cli = new Commandline();
    cli.setWorkingDirectory(baseDir);
    cli.setExecutable("java");
    cli.addEnvironment("CLASSPATH", classPath);
    cli.createArg().setValue(String.format("-Dbasedir=%s", baseDir));
    cli.createArg().setValue("com.diehl.scalatest.forkTools.IsClassATestSuite");
    cli.createArg().setValue(testOutputDirectory);
    cli.createArg().setValue(cls);
    final StreamConsumer streamConsumer = new StreamConsumer() {
      public void consumeLine(final String line) {
        System.out.println(line);
      }
    };
    try {
      final int result = CommandLineUtils.executeCommandLine(cli, streamConsumer, streamConsumer);
      return result == 0;
    } catch (Exception ex) {
      throw new MojoFailureException("Exception while running suite verifier.", ex);
    }
  }
  
}
