package com.microsoft.seeds.fsmexecutor.models.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WriteToFile {
    public static String FILE_PATH = "C:\\Users\\kchourasia\\Desktop\\fsmexecutor\\test_out.txt";
    public static void write(String str){
        Path path
                = Paths.get(FILE_PATH);

        byte[] arr = str.getBytes();

        // Try block to check for exceptions
        try {
            // Now calling Files.writeString() method
            // with path , content & standard charsets
            Files.write(path, arr);
        }
        // Catch block to handle the exception
        catch (IOException ex) {
            // Print messqage exception occurred as
            // invalid. directory local path is passed
            System.out.print("Invalid Path");
            throw new RuntimeException(ex);
        }
    }
}
