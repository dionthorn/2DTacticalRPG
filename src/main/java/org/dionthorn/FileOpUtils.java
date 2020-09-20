package org.dionthorn;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * Dedicated class for static methods related to file operations
 * Please note this class references the Run.programLogger for displaying messages.
 * If you wish to use this class in another project simply replace those lines with your own Logger or with
 * System.err.println() calls or your preferred method.
 */
public class FileOpUtils {

    /**
     * Will check to see if a file at path exists and return true or false.
     * @param path the target files path
     * @return returns boolean true if a file exists at path and boolean false if it doesn't.
     */
    public static boolean doesFileExist(String path) {
        boolean answer = false;
        try {
            answer = new File(path).isFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return answer;
    }

    /**
     * Returns the filenames from a directory as a string array where
     * each index is a name of a file including extensions
     * @param path the target directory path
     * @return a string array where each index is the name of a file in the directory at path
     */
    public static String[] getFileNamesFromDirectory(String path) {
        File[] files;
        String[] fileNames = new String[0];
        try {
            files = new File(path).listFiles();
            if(files != null) {
                fileNames = new String[files.length];
                for(int i=0; i<files.length; i++) {
                    if(files[i].isFile()) {
                        fileNames[i] = files[i].getName();
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileNames;
    }

    /**
     * Will convert the file at path into a String[] where each index is a line from the file.
     * @param path the target files path
     * @return a string array where each index is the corresponding line in the target file at path
     *         will return null if it fails or file doesn't exist
     */
    public static String[] getFileLines(String path) {
        String[] toReturn = null;
        if(doesFileExist(path)) {
            File targetFile = new File(path);
            try {
                byte[] fileByteData = Files.readAllBytes(targetFile.toPath());
                toReturn = new String(fileByteData).split(System.lineSeparator());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Run.programLogger.log(Level.WARNING, String.format("File: %s Doesn't Exist!", path));
        }
        return toReturn;
    }

    /**
     * Will either create a new file at path, or overwrite an existing one. will take each string in data and
     * write a new line per string into the file at path.
     * @param path the destination to create or overwrite data
     * @param data the data where each index in data will be a new line in the file
     */
    public static void writeFileLines(String path, String[] data) {
        if(!doesFileExist(path)) {
            Run.programLogger.log(Level.INFO, String.format("File: %s Doesn't Exist Creating New File!", path));
            createFile(path);
        }
        File targetFile = new File(path);
        try {
            ByteArrayOutputStream convertToBytes = new ByteArrayOutputStream();
            FileOutputStream fileWriter = new FileOutputStream(targetFile);
            for(String s: data) {
                s += System.lineSeparator();
                convertToBytes.write(s.getBytes());
            }
            fileWriter.write(convertToBytes.toByteArray());
            Run.programLogger.log(Level.INFO, String.format("File: %s Successfully Wrote Data", path));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Will create a new File if no file at the given path exists otherwise it does nothing.
     * @param path the target path to create a new file, will do nothing if a file already exists
     */
    public static void createFile(String path) {
        File file = new File(path);
        try {
            if(file.createNewFile()) {
                Run.programLogger.log(Level.INFO, String.format("File: %s Has Been Created!", path));
            } else {
                Run.programLogger.log(Level.INFO, String.format("File: %s Already Exists!", path));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
