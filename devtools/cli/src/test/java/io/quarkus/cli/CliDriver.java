package io.quarkus.cli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Assertions;

import picocli.CommandLine;

public class CliDriver {
    // Enable to dump full output
    private static final boolean printOutput = true;

    static final PrintStream stdout = System.out;
    static final PrintStream stderr = System.err;

    public static Result executeArbitraryCommand(String... args) throws Exception {
        System.out.println("$ " + String.join(" ", args));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream outPs = new PrintStream(out);
        System.setOut(outPs);

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream errPs = new PrintStream(err);
        System.setErr(errPs);

        Result result = new Result();
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

            Process p = pb.start();
            p.waitFor();
            outPs.flush();
            errPs.flush();
        } finally {
            System.setOut(stdout);
            System.setErr(stderr);
        }
        result.stdout = out.toString();
        result.stderr = err.toString();
        return result;
    }

    public static Result execute(String... args) throws Exception {
        String newArgs[] = Arrays.copyOf(args, args.length + 1);
        newArgs[args.length] = "--cli-test";

        System.out.println("$ quarkus " + String.join(" ", newArgs));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream outPs = new PrintStream(out);
        System.setOut(outPs);

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream errPs = new PrintStream(err);
        System.setErr(errPs);

        Result result = new Result();
        QuarkusCli cli = new QuarkusCli();
        try {
            result.exitCode = cli.run(newArgs);
            outPs.flush();
            errPs.flush();
        } finally {
            System.setOut(stdout);
            System.setErr(stderr);
        }
        result.stdout = out.toString();
        result.stderr = err.toString();
        return result;
    }

    public static void println(String msg) {
        if (printOutput) {
            System.out.println(msg);
        }
    }

    public static class Result {
        int exitCode;
        String stdout;
        String stderr;

        public void echoSystemOut() {
            if (CliDriver.printOutput) {
                System.out.println(stdout);
                System.out.println();
            }
        }

        public void echoSystemErr() {
            if (CliDriver.printOutput) {
                System.out.println(stderr);
                System.out.println();
            }
        }

        @Override
        public String toString() {
            return "result: {\n  exitCode: {" + exitCode
                    + "},\n  system_err: {" + stderr
                    + "},\n  system_out: {" + stdout + "}\n}";
        }
    }

    public static void deleteDir(Path path) throws Exception {
        if (!path.toFile().exists()) {
            return;
        }

        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);

        Assertions.assertFalse(path.toFile().exists());
    }

    public static String readFileAsString(Path path) throws Exception {
        return new String(Files.readAllBytes(path));
    }

    public static void valdiateGeneratedSourcePackage(Path project, String name) {
        Path packagePath = project.resolve("src/main/java/" + name);
        Assertions.assertTrue(packagePath.toFile().exists(),
                "Package directory should exist: " + packagePath.toAbsolutePath().toString());
        Assertions.assertTrue(packagePath.toFile().isDirectory(),
                "Package directory should be a directory: " + packagePath.toAbsolutePath().toString());
    }

    public static Result invokeValidateExtensionList() throws Exception {
        Result result = execute("extension", "list", "-e", "-B", "--verbose");

        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        return result;
    }

    public static Result invokeExtensionAddQute(Path file) throws Exception {
        // add the qute extension
        Result result = execute("extension", "add", "qute", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);

        // list all extensions, make sure qute is present
        result = invokeValidateExtensionList();
        Assertions.assertTrue(result.stdout.contains("quarkus-qute"),
                "Expected quarkus-qute to be in the list of extensions. Result:\n" + result);

        String content = readFileAsString(file);
        Assertions.assertTrue(content.contains("quarkus-qute"),
                "quarkus-qute should be listed as a dependency. Result:\n" + content);

        return result;
    }

    public static Result invokeExtensionRemoveQute(Path file) throws Exception {
        // remove the qute extension
        Result result = execute("extension", "remove", "qute", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);

        // list all extensions, make sure qute is present
        result = invokeValidateExtensionList();
        Assertions.assertFalse(result.stdout.contains("quarkus-qute"),
                "Expected quarkus-qute to be missing from the list of extensions. Result:\n" + result);

        String content = readFileAsString(file);
        Assertions.assertFalse(content.contains("quarkus-qute"),
                "quarkus-qute should not be listed as a dependency. Result:\n" + content);

        return result;
    }

    public static Result invokeExtensionAddMultiple(Path file) throws Exception {
        // add the qute extension
        Result result = execute("extension", "add", "amazon-lambda-http", "jackson", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);

        // list all extensions, make sure all are present
        result = invokeValidateExtensionList();
        Assertions.assertTrue(result.stdout.contains("quarkus-qute"),
                "Expected quarkus-qute to be in the list of extensions. Result:\n" + result);
        Assertions.assertTrue(result.stdout.contains("quarkus-amazon-lambda-http"),
                "Expected quarkus-amazon-lambda-http to be in the list of extensions. Result:\n" + result);
        Assertions.assertTrue(result.stdout.contains("quarkus-jackson"),
                "Expected quarkus-jackson to be in the list of extensions. Result:\n" + result);

        String content = CliDriver.readFileAsString(file);
        Assertions.assertTrue(content.contains("quarkus-qute"),
                "quarkus-qute should still be listed as a dependency. Result:\n" + content);
        Assertions.assertTrue(content.contains("quarkus-amazon-lambda-http"),
                "quarkus-amazon-lambda-http should be listed as a dependency. Result:\n" + content);
        Assertions.assertTrue(content.contains("quarkus-jackson"),
                "quarkus-jackson should be listed as a dependency. Result:\n" + content);

        return result;
    }

    public static Result invokeExtensionRemoveMultiple(Path file) throws Exception {
        // add the qute extension
        Result result = execute("extension", "remove", "amazon-lambda-http", "jackson", "-e", "-B", "--verbose");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);

        // list all extensions, make sure all are present
        result = invokeValidateExtensionList();
        Assertions.assertFalse(result.stdout.contains("quarkus-qute"),
                "quarkus-qute should not be in the list of extensions. Result:\n" + result);
        Assertions.assertFalse(result.stdout.contains("quarkus-amazon-lambda-http"),
                "quarkus-amazon-lambda-http should not be in the list of extensions. Result:\n" + result);
        Assertions.assertFalse(result.stdout.contains("quarkus-jackson"),
                "quarkus-jackson should not be in the list of extensions. Result:\n" + result);

        String content = CliDriver.readFileAsString(file);
        Assertions.assertFalse(content.contains("quarkus-qute"),
                "quarkus-qute should not be listed as a dependency. Result:\n" + content);
        Assertions.assertFalse(content.contains("quarkus-amazon-lambda-http"),
                "quarkus-amazon-lambda-http should not be listed as a dependency. Result:\n" + content);
        Assertions.assertFalse(content.contains("quarkus-jackson"),
                "quarkus-jackson should not be listed as a dependency. Result:\n" + content);

        return result;
    }

    public static Result invokeExtensionListInstallable() throws Exception {
        Result result = CliDriver.execute("extension", "list", "-e", "-B", "--verbose", "-i");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        Assertions.assertTrue(result.stdout.contains("quarkus-hibernate-orm"),
                "quarkus-hibernate-orm should be listed as an installable extension. Found:\n" + result);
        Assertions.assertFalse(result.stdout.contains("quarkus-qute"),
                "quarkus-qute should not be listed as an installable extension. Found:\n" + result);

        return result;
    }

    public static Result invokeExtensionListInstallableSearch() throws Exception {
        Result result = CliDriver.execute("extension", "list", "-e", "-B", "--verbose", "-i", "--search=vertx-*");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);

        Assertions.assertTrue(result.stdout.contains("quarkus-vertx-web"),
                "quarkus-vertx-web should be returned in search result. Found:\n" + result);
        Assertions.assertFalse(result.stdout.contains("quarkus-vertx-http"),
                "quarkus-vertx-http should not be returned in search result (already installed). Found:\n" + result);

        return result;
    }

    public static void invokeExtensionListFormatting() throws Exception {
        Result result = CliDriver.execute("extension", "list", "-e", "-B", "--verbose", "-i", "--concise");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        Assertions.assertTrue(result.stdout.contains("quarkus-vertx-web"),
                "quarkus-vertx-web should be returned in result. Found:\n" + result);
        Assertions.assertTrue(result.stdout.contains("Reactive Routes"),
                "'Reactive Routes' descriptive name should be returned in results. Found:\n" + result);

        result = CliDriver.execute("extension", "list", "-e", "-B", "--verbose", "-i", "--full");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        // TODO

        result = CliDriver.execute("extension", "list", "-e", "-B", "--verbose", "-i", "--origins");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        // TODO

        // Two different output options can not be specified together
        result = CliDriver.execute("extension", "list", "-e", "-B", "--verbose", "-i", "--origins", "--name");
        Assertions.assertEquals(CommandLine.ExitCode.USAGE, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        // TODO
    }

    public static Result invokeExtensionAddRedundantQute() throws Exception {
        Result result = execute("extension", "add", "-e", "-B", "--verbose", "qute");
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        return result;
    }

    public static Result invokeExtensionRemoveNonexistent() throws Exception {
        Result result = execute("extension", "remove", "-e", "-B", "--verbose", "nonexistent");
        System.out.println(result);
        return result;
    }

    public static Result invokeValidateBuild(Path project) throws Exception {
        Result result = execute("build", "-e", "-B", "--dryrun"); // "build" is the default, skip ansi output
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);
        System.out.println(result.stdout);

        result = execute("build", "-e", "-B", "--clean"); // "build" is the default, skip ansi output
        Assertions.assertEquals(CommandLine.ExitCode.OK, result.exitCode,
                "Expected OK return code. Result:\n" + result);

        return result;
    }

    public static void validateApplicationProperties(Path project, List<String> configs) throws Exception {
        Path properties = project.resolve("src/main/resources/application.properties");
        Assertions.assertTrue(properties.toFile().exists(),
                "application.properties should exist: " + properties.toAbsolutePath().toString());
        String propertiesFile = CliDriver.readFileAsString(properties);
        configs.forEach(conf -> Assertions.assertTrue(propertiesFile.contains(conf),
                "Properties file should contain " + conf + ". Found:\n" + propertiesFile));
    }
}
