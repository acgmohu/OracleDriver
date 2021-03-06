package org.cf.oracle;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cf.oracle.options.InvocationTarget;
import org.cf.oracle.options.TargetParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Driver {

    private static final String DRIVER_DIR = "/data/local";

    private static final String OUTPUT_HEADER = "===ORACLE DRIVER OUTPUT===\n";
    private static final String EXCEPTION_LOG = DRIVER_DIR + "/od-exception.txt";
    private static final String OUTPUT_FILE = DRIVER_DIR + "/od-output.json";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static void die(String msg, Exception exception) {
        PrintWriter writer;
        try {
            writer = new PrintWriter(EXCEPTION_LOG, "UTF-8");
        } catch (Exception e) {
            return;
        }
        writer.println(msg);
        writer.println(exception);
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        writer.println(sw.toString());
        writer.close();

        // app_process, dalvikvm, and dvz don't propagate exit codes, so this doesn't matter
        System.exit(-1);
    }

    private static String invokeMethod(Method method, Object[] arguments) throws IOException, IllegalAccessException,
                    IllegalArgumentException, InvocationTargetException {
        method.setAccessible(true);
        Object returnObject = method.invoke(null, arguments);

        Class<?> returnClass = method.getReturnType();
        if (returnClass.getName().equals("Ljava.lang.Void;")) {
            // I hear an ancient voice, whispering from the Void, and it chills my lightless heart...
            return null;
        }

        String output;
        try {
            output = GSON.toJson(returnClass.cast(returnObject));
        } catch (Exception ex) {
            output = GSON.toJson(returnObject);
        }

        return output;
    }

    private static void showUsage() {
        System.out.println("Usage: export CLASSPATH=/data/local/od.zip; app_process /system/bin org.cf.oracle.Driver <class> <method> [<parameter type>:<parameter value json>]");
        System.out.println("       export CLASSPATH=/data/local/od.zip; app_process /system/bin org.cf.oracle.Driver @<json file>");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            showUsage();
            System.exit(-1);
        }

        boolean multipleTargets = args.length < 2 && args[0].startsWith("@");
        if (args.length < 1 && !multipleTargets) {
            showUsage();
            System.exit(-1);
        }

        try {
            StackSpoofer.init();
        } catch (NumberFormatException | IOException e) {
            System.out.println("Error parsing stack spoof info");
            die("Error parsing stack spoof info", e);
        }

        List<InvocationTarget> targets = null;
        try {
            targets = TargetParser.parse(args, GSON);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IOException e) {
            System.out.println("Unable to parse targets.");
            die("Unable to parse targets", e);
        }

        if (targets == null){
            System.out.println("Targets is null.");
            die("Targets is null", new Exception("None"));
            return;
        }

        String output = null;
        if (!multipleTargets) {
            InvocationTarget target = targets.get(0);
            try {
                output = invokeMethod(target.getMethod(), target.getArguments());
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | IOException e) {
                die("Error executing '" + target.getMethod() + "' with " + target.getArgumentsString(), e);
            }

            if (output != null) {
                System.out.println(OUTPUT_HEADER + output);
            }
        } else {
            // @<json file>
            Map<String, String[]> idToOutput = new HashMap<>();
            for (InvocationTarget target : targets) {
                String status;
                try {
                    output = invokeMethod(target.getMethod(), target.getArguments());
                    status = "success";
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | IOException e) {
                    e.printStackTrace();
                    continue;
//                    StringBuilder sb = new StringBuilder("Error executing '");
//                    sb.append(target.getMethod()).append("' with ").append(target.getArgumentsString()).append('\n');
//                    StringWriter sw = new StringWriter();
//                    e.printStackTrace(new PrintWriter(sw));
//                    sb.append(sw.getBuffer());
//                    output = sb.toString();
//                    status = "failure";
                }
                idToOutput.put(target.getId(), new String[] { status, output });
            }

            String json = GSON.toJson(idToOutput);
            try {
                FileUtils.writeFile(OUTPUT_FILE, json);
            } catch (FileNotFoundException | UnsupportedEncodingException e) {
                die("Unable to write output to " + OUTPUT_FILE, e);
            }

            System.out.println(OUTPUT_HEADER + "success");
        }
    }
}
