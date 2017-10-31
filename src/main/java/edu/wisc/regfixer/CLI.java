package edu.wisc.regfixer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeoutException;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import edu.wisc.regfixer.enumerate.Benchmark;
import edu.wisc.regfixer.enumerate.Job;
import edu.wisc.regfixer.util.ReportStream;
import edu.wisc.regfixer.util.Ansi;

public class CLI {
  private static class ArgsRoot {
    @Parameter(names={"--help", "-h"})
    private boolean help = false;

    @Parameter(names={"--version", "-v"})
    private boolean version = false;

    @Parameter
    private List<String> catchall = new ArrayList<>();
  }

  @Parameters(separators="=")
  private static class ArgsServe {
    @Parameter(names="--port")
    private Integer port = null;

    @Parameter(names="--limit")
    private Integer limit = null;

    @Parameter(names="--open")
    private String open = null;

    @Parameter(names="--debug")
    private boolean debug = false;

    @Parameter
    private List<String> catchall = new ArrayList<>();
  }

  @Parameters(separators="=")
  private static class ArgsFix {
    @Parameter(names="--color")
    private boolean color = false;

    @Parameter(names="--limit")
    private Integer limit = null;

    @Parameter(names="--print-class-tree")
    private boolean printClassTree = false;

    @Parameter(names="--print-formula")
    private boolean printFormula = false;

    @Parameter(names="--print-model")
    private boolean printModel = false;

    @Parameter
    private List<String> files = new ArrayList<>();
  }

  @Parameters(separators="=")
  private static class ArgsTest {
  }

  public static void main (String[] argv) {
    ArgsRoot root = new ArgsRoot();
    ArgsServe serve = new ArgsServe();
    ArgsFix fix = new ArgsFix();
    ArgsTest test = new ArgsTest();

    JCommander cli = JCommander.newBuilder()
      .programName("regfixer")
      .addObject(root)
      .addCommand("serve", serve)
      .addCommand("fix", fix)
      .addCommand("test", test)
      .build();

    cli.parse(argv);

    if (root.help) {
      System.exit(handleHelp());
    }

    if (root.version) {
      System.exit(handleVersion());
    }

    if (cli.getParsedCommand() == null) {
      System.exit(handleHelp());
    }

    if (cli.getParsedCommand().equals("serve")) {
      if (handleServe(serve) == 0) {
        return;
      } else {
        System.exit(1);
      }
    }

    if (cli.getParsedCommand().equals("fix")) {
      System.exit(handleFix(fix));
    }

    if (cli.getParsedCommand().equals("test")) {
      System.exit(handleTest(test));
    }
  }

  private static int handleHelp () {
    System.err.print(
        "\n  Usage: regfixer [options]"
      + "\n         regfixer [command] [command options]"
      + "\n"
      + "\n"
      + "\n  Options:"
      + "\n    -h, --help     output usage information"
      + "\n    -v, --version  output the version number"
      + "\n"
      + "\n"
      + "\n  Commands:"
      + "\n    serve [options]"
      + "\n      --port <number>"
      + "\n          The port to listen for incoming connections. If this flag is not"
      + "\n          specified the PORT environment variable will be used instead. If"
      + "\n          neither the flag nor the variable are set, an error will occur."
      + "\n      --limit <number>"
      + "\n          The maximum number of unsuccessful enumeration cycles that occur"
      + "\n          before a TimeoutException is thrown and the job aborts without a"
      + "\n          final result. Default value is 1000."
      + "\n      --open <file> (not implemented)"
      + "\n          Read a benchmark file and use its contents as the initial values"
      + "\n          in the web-app. If this flag is not set, the web-app will launch"
      + "\n          from a clean state."
      + "\n      --debug"
      + "\n          When this flag is set, each repair request will produce a report"
      + "\n          that will be written to a file describing the steps used to find"
      + "\n          the repair. These reports will be written to disk with filenames"
      + "\n          in the format: 'debug_TIMESTAMP_HASH.txt' where TIMESTAMP is the"
      + "\n          UNIX timestamp (in milliseconds) and HASH is the first part of a"
      + "\n          SHA1 hash computed from formatting the repair requests inputs as"
      + "\n          a benchmark file."
      + "\n    fix [options] <file>"
      + "\n      --color"
      + "\n          If set, output will include ANSI color codes."
      + "\n      --limit <number>"
      + "\n          The maximum number of unsuccessful enumeration cycles that occur"
      + "\n          before a TimeoutException is thrown and the job aborts without a"
      + "\n          final result. Default value is 1000."
      + "\n      --print-class-tree"
      + "\n          For all SAT formulae, print the character class trees that track"
      + "\n          how frequently each character class is used per hole."
      + "\n      --print-formula"
      + "\n          For all SAT formulae, print the formula represented as a string."
      + "\n      --print-model"
      + "\n          For all SAT satisfiable formulae, print the model mapping all of"
      + "\n          formula's variables to their computed values."
      + "\n      <file>"
      + "\n          Read a benchmark file and use its contents to compute the repair"
      + "\n          of its regular expression. If this argument is not set, an error"
      + "\n          will occur. A description of the simple benchmark file format is"
      + "\n          included below."
      + "\n"
      + "\n"
      + "\n  Benchmark File Format:"
      + "\n    A benchmark file has 3 sections separated by dividers:"
      + "\n"
      + "\n    Regular Expression"
      + "\n      The first line of the file contains a single regular expression."
      + "\n"
      + "\n    Divider"
      + "\n      A single line that only contains 3 dashes: '---'"
      + "\n"
      + "\n    Set of 0+ Positive Matches"
      + "\n      Each positive match is contained on a separate line with the format:"
      + "\n      '(START_INDEX:END_INDEX)' where START_INDEX is the leftmost index of"
      + "\n      the match (inclusive), END_INDEX is the rightmost index (exclusive)."
      + "\n      Both indices are counted relative to the entire corpus."
      + "\n"
      + "\n    Divider"
      + "\n      A single line that only contains 3 dashes: '---'"
      + "\n"
      + "\n    Corpus"
      + "\n      All characters found after the line with the second divider are part"
      + "\n      of the corpus. The corpus has no restrictions on which characters it"
      + "\n      can contain or whitespace formatting. It is recommended that LF line"
      + "\n      separators be used for consistency and easier index counting."
      + "\n"
      + "\n"
    );

    return 0;
  }

  private static int handleVersion () {
    System.err.println("1.0.0");
    return 0;
  }

  private static int handleServe (ArgsServe args) {
    if (args.port == null) {
      if (System.getenv("PORT") == null) {
        System.err.println("no port given");
        return 1;
      } else {
        try {
          args.port = Integer.valueOf(System.getenv("PORT"));
        } catch (NumberFormatException ex) {
          System.err.println("no port given");
          return 1;
        }
      }
    }

    if (args.limit == null || args.limit < 1) {
      args.limit = 1000;
    }

    Server.start(args.port, args.limit, args.debug);
    return 0;
  }

  private static int handleFix (ArgsFix args) {
    if (args.files.size() > 1) {
      System.err.println("too many arguments");
      return 1;
    } else if (args.files.size() == 0) {
      System.err.println("no file given");
      return 1;
    }

    Job job = null;

    Config config = new Config();
    config.setBool("print-class-tree", args.printClassTree);
    config.setBool("print-formula", args.printFormula);
    config.setBool("print-model", args.printModel);

    try {
      job = Benchmark.readFromFile(args.files.get(0));
    } catch (IOException ex) {
      System.err.println("unable to read file");
      return 1;
    }

    ReportStream report = new ReportStream(System.out, true, args.color);

    if (args.limit == null || args.limit <= 1) {
      args.limit = 1000;
    }

    try {
      RegFixer.fix(job, report, args.limit, config);
    } catch (TimeoutException ex) {
      System.out.println("TIMEOUT EXCEPTION");
      return 1;
    }

    return 0;
  }

  private static int handleTest (ArgsTest args) {
    File testingDir = new File("tests");
    File[] testingFiles = testingDir.listFiles();

    Arrays.sort(testingFiles, new Comparator<File>() {
      @Override
      public int compare (File a, File b) {
        return a.getName().compareTo(b.getName());
      }
    });

    if (testingFiles == null) {
      System.err.println("cannot read directory ./tests");
      return 1;
    }

    for (int i = 0; i < testingFiles.length; i++) {
      if (testingFiles[i].isFile() && testingFiles[i].getName().matches("test_\\w+\\.txt")) {
        Job job = null;

        try {
          job = Benchmark.readFromFile(testingFiles[i].getPath());
        } catch (IOException ex) {
          System.err.println("unable to read file: " + testingFiles[i].getPath());
        }

        try {
          String solution = RegFixer.fix(job);

          String expectedPath = "./tests/expect_" + testingFiles[i].getName().substring(5);
          String expected = null;
          try {
            byte[] encoded = Files.readAllBytes(Paths.get(expectedPath));
            expected = new String(encoded, StandardCharsets.UTF_8);
          } catch (Exception ex) {}

          if (expected == null) {
            System.out.println(Ansi.Green.sprintf("  ✓ %-32s %s", testingFiles[i].getName(), solution));
          } else {
            System.out.println(Ansi.Green.sprintf("  ✓ %-32s %-32s %s", testingFiles[i].getName(), solution, expected));
          }
        } catch (TimeoutException ex) {
          System.out.println(Ansi.Red.sprintf("  ✗ %-32s %s", testingFiles[i].getName(), "test timed out"));
        }
      }
    }

    return 0;
  }
}
