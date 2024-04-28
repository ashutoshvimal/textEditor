package org.texteditor;

import java.io.IOException;

public class RawTerminal {
    public static void main(String[] args) {
        try {
            // Command to open a new terminal window and execute the "dir" command
            String command = "gnome-terminal -- bash -c 'dir; read -p \"Press Enter to exit...\"'";

            // Create ProcessBuilder
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("bash", "-c", command);

            // Start the process
            Process process = builder.start();

            // Wait for the process to complete
            int exitCode = process.waitFor();
            System.out.println("Exited with error code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
