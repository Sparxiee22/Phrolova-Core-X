package com.phrolova.corex;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RootShell {
    public static String run(String command) {
        String output = runWith(new String[]{"su", "-mm", "-c", command});
        if (!output.startsWith("ERROR:") && !output.contains("invalid option") && !output.contains("Unknown option")) {
            return output;
        }
        return runWith(new String[]{"su", "-c", command});
    }

    private static String runWith(String[] command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) output.append(line).append("\n");
            while ((line = error.readLine()) != null) output.append(line).append("\n");
            process.waitFor();
        } catch (Exception e) {
            output.append("ERROR: ").append(e.getMessage());
        }
        return output.toString().trim();
    }

    public static boolean hasRoot() {
        String result = run("id");
        return result.contains("uid=0");
    }
}
