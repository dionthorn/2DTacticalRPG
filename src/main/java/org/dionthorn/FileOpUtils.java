package org.dionthorn;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static java.nio.file.FileSystems.getFileSystem;

/**
 * Dedicated class for static methods related to file operations
 * Please note this class references the Run.programLogger for displaying messages.
 * If you wish to use this class in another project simply replace those lines with your own Logger or with
 * System.err.println() calls or your preferred method.
 */
public class FileOpUtils {

    /**
     * Will check to see if a file at path exists and return true or false.
     * @param targetFile the target file
     * @return returns boolean true if a file exists at path and boolean false if it doesn't.
     */
    public static boolean doesFileExist(URI targetFile) {
        boolean answer = false;
        try {
            if(targetFile.getScheme().equals("jrt")) {
                Path path = Path.of(targetFile);
                assert(Files.exists(path));
                FileSystem jrtfs = FileSystems.getFileSystem(URI.create("jrt:/"));
                return Files.exists(jrtfs.getPath(path.toString()));
            } else {
                answer = new File(targetFile.getPath()).isFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return answer;
    }

    /**
     * Returns the filenames from a directory as a string array where
     * each index is a name of a file including extensions
     * @param targetFile the target file
     * @return a string array where each index is the name of a file in the directory at path
     */
    public static String[] getFileNamesFromDirectory(URI targetFile) {
        ArrayList<String> fileNamesList = new ArrayList<>();
        String[] fileNames = new String[0];
        try {
            File[] files;
            if(Run.JRT) {
                Path path = Path.of(targetFile);
                assert(Files.exists(path));
                FileSystem jrtfs = FileSystems.getFileSystem(URI.create("jrt:/"));
                assert(Files.exists(jrtfs.getPath(path.toString())));
                try {
                    DirectoryStream<Path> stream = Files.newDirectoryStream(jrtfs.getPath(path.toString()));
                    for(Path entry: stream) {
                        fileNamesList.add(entry.getFileName().toString());
                    }
                    fileNames = fileNamesList.toArray(new String[0]);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                files = new File(targetFile.getPath()).listFiles();
            } else {
                files = new File(targetFile.getPath()).listFiles();
            }
            if(files != null) {
                if(!Run.JRT) {
                    fileNames = new String[files.length];
                    for(int i=0; i<files.length; i++) {
                        if(files[i].isFile()) {
                            fileNames[i] = files[i].getName();
                        }
                    }
                } else {
                    Run.programLogger.log(Level.INFO, "Using jrt for files");
                }
            } else {
                if(!Run.JRT) {
                    Run.programLogger.log(Level.INFO, "No Files Found In Directory " + targetFile);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileNames;
    }

    /**
     * Will process a target file as if it were lines of String using System.lineSeparator()
     * @param targetFile the target file
     * @return a string array where each index is a line from the file
     */
    public static String[] getFileLines(URI targetFile) {
        String[] toReturn = null;
        try {
            byte[] fileByteData = Files.readAllBytes(Path.of(targetFile));
            toReturn = new String(fileByteData).split(System.lineSeparator());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Run.programLogger.log(Level.INFO, "Successfully located file at: " + targetFile.getPath());
        return toReturn;
    }

    /**
     * Will either create a new file at path, or overwrite an existing one. will take each string in data and
     * write a new line per string into the file at path.
     * @param targetFile the target file
     * @param data the data where each index in data will be a new line in the file
     */
    public static void writeFileLines(URI targetFile, String[] data) {
        if(!doesFileExist(targetFile)) {
            Run.programLogger.log(Level.INFO, String.format("File: %s Doesn't Exist Creating New File!", targetFile.getPath()));
            createFile(targetFile);
        }
        File fileToWrite = new File(targetFile.getPath());
        try {
            ByteArrayOutputStream convertToBytes = new ByteArrayOutputStream();
            FileOutputStream fileWriter = new FileOutputStream(fileToWrite);
            for(String s: data) {
                s += System.lineSeparator();
                convertToBytes.write(s.getBytes());
            }
            fileWriter.write(convertToBytes.toByteArray());
            Run.programLogger.log(Level.INFO, String.format("File: %s Successfully Wrote Data", targetFile.getPath()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Will create a new File if no file at the given path exists otherwise it does nothing.
     * @param path the target path to create a new file, will do nothing if a file already exists
     */
    public static void createFile(URI path) {
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
