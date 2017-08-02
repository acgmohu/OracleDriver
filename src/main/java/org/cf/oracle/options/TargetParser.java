package org.cf.oracle.options;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.cf.oracle.FileUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class TargetParser {

    private static InvocationTarget buildTarget(Gson gson, String className, String methodName, String... args)
                    throws ClassNotFoundException, NoSuchMethodException, SecurityException {
        return buildTarget(gson, "", className, methodName, args);
    }

    private static InvocationTarget buildTarget(Gson gson, String id, String className, String methodName,
                    String... args) throws ClassNotFoundException, NoSuchMethodException, SecurityException {
        Class<?>[] parameterTypes = new Class[args.length];
        Object[] methodArguments = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            String[] parts = args[i].split(":", 2);
            parameterTypes[i] = smaliToJavaClass(parts[0]);
            if (parts.length == 1) {
                methodArguments[i] = null;
            } else {
                String jsonValue = parts[1];
                if (parameterTypes[i] == String.class) {
                    try {
                        // Normalizing strings to byte[] avoids escaping ruby, bash, adb shell, and java
                        byte[] stringBytes = (byte[]) gson.fromJson(jsonValue, Class.forName("[B"));
                        methodArguments[i] = new String(stringBytes);
                    } catch (JsonSyntaxException ex) {
                        // Possibly not using byte array format for string (good luck)
                        methodArguments[i] = jsonValue;
                    }
                } else if (parameterTypes[i] == char.class) {
                    try {
                        int ii = Integer.valueOf(jsonValue);
                        char c = (char)ii;
                        methodArguments[i] = c;
                    } catch (JsonSyntaxException ex) {
                        System.out.println(ex.getLocalizedMessage());
                        methodArguments[i] = jsonValue;
                    }
                } else if (parameterTypes[i] == char[].class) {
                    int[] bytes = (int[]) gson.fromJson(jsonValue, Class.forName("[I"));
                    char[] chars = new char[bytes.length];
                    for (int j=0; j<bytes.length; j++) {
                        char c = (char)bytes[j];
                        chars[j] = c;
                    }
                    methodArguments[i] = chars;
                } else {
//                    System.out.println("Parsing: " + jsonValue + " as " + parameterTypes[i]);
                    methodArguments[i] = gson.fromJson(jsonValue, parameterTypes[i]);
                }
            }
        }
        Class<?> methodClass = Class.forName(className);
        Method method = methodClass.getDeclaredMethod(methodName, parameterTypes);

        return new InvocationTarget(id, args, methodArguments, method);
    }

    private static List<InvocationTarget> loadTargetsFromFile(Gson gson, String fileName) throws IOException {
        String targetJson = FileUtils.readFile(fileName);
        JsonArray targetItems = new JsonParser().parse(targetJson).getAsJsonArray();
        // JsonArray targetItems = json.getAsJsonArray();
        List<InvocationTarget> targets = new LinkedList<>();
        for (JsonElement element : targetItems) {
            JsonObject targetItem = element.getAsJsonObject();
            String id = targetItem.get("id").getAsString();
            String className = targetItem.get("className").getAsString();
            String methodName = targetItem.get("methodName").getAsString();
            JsonArray argumentsJson = targetItem.get("arguments").getAsJsonArray();
            String[] arguments = new String[argumentsJson.size()];
            for (int i = 0; i < arguments.length; i++) {
                arguments[i] = argumentsJson.get(i).getAsString();
            }

            InvocationTarget target;
            try {
                target = buildTarget(gson, id, className, methodName, arguments);
            } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
                System.out.println("Could not build target: " + className + ";->" + methodName);
                e.printStackTrace();
                continue;
            }

            targets.add(target);

        }


        return targets;
    }

    private static Class<?> smaliToJavaClass(String className) throws ClassNotFoundException {
        switch (className) {
            case "I":
                return int.class;
            case "V":
                return void.class;
            case "Z":
                return boolean.class;
            case "B":
                return byte.class;
            case "S":
                return short.class;
            case "J":
                return long.class;
            case "C":
                return char.class;
            case "F":
                return float.class;
            case "D":
                return double.class;
            default:
                return Class.forName(className);
        }
    }

    public static List<InvocationTarget> parse(String[] args, Gson gson) throws ClassNotFoundException,
                    NoSuchMethodException, SecurityException, IOException {
        if (args[0].startsWith("@")) {
            String fileName = args[0].substring(1);

            return loadTargetsFromFile(gson, fileName);
        } else {
            InvocationTarget target = buildTarget(gson, args[0], args[1], Arrays.copyOfRange(args, 2, args.length));
            List<InvocationTarget> targets = new LinkedList<>();
            targets.add(target);

            return targets;
        }
    }

}
