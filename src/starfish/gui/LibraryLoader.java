package starfish.gui;

import vtk.vtkNativeLibrary;

import javax.swing.*;
import java.io.*;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Starfish uses the VTK library, which is natively in C++ or something and then wrapped so it can be used in a Java
 * program. There is a vtk.jar library you can load, but it needs the real C++ code - which is in a bunch of
 * .dll/.so/etc. native files. This class makes sure that Starfish has access to those native files so it can properly
 * use the VTK library.
 *
 * This class faces two problems:
 * Making sure that the files exist, and making sure that Starfish has access to them.
 *
 * If the native files don't exist, the LibraryLoader can use a copy stored in the .jar file.
 *
 * The LibraryLoader makes sure that
 */
public class LibraryLoader {


    private final static File binPath = new File("bin");

    /**
     * Makes sure Starfish has all the necessary files
     * @return true if libraries successfully loaded, false if not loaded
     */
    public static boolean tryLoad() {
        //System.setProperty("vtk.lib.dir", new File("bin").getAbsolutePath());
        return vtkNativeLibrary.LoadAllNativeLibraries();
        /*if (!binPath.exists()) {
            if (confirmWithUser()) {
                loadEnvIntoWD();
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {}
            } else {
                JOptionPane.showMessageDialog(null,
                        "Unable to load libraries. Try using headless Starfish.");
                System.exit(1);
            }
        }
        if (binPath.exists() && !binIsInPath()) {
            JOptionPane.showMessageDialog(null, "Relaunch using the \"RUN\" file " +
                    "that was created in the same directory as the JAR file", "", JOptionPane.INFORMATION_MESSAGE);
            System.exit(1);
        }

        boolean res = tryLoadVTK();

        return res;*/
    }

    private static boolean tryLoadVTK() {
        boolean res = true;
        try {
            res = vtkNativeLibrary.LoadAllNativeLibraries();
        } catch (Exception e) {
            res = false;
        }
        vtkNativeLibrary.DisableOutputWindow(null);
        return res;
    }

    private static boolean loadEnvIntoWD() {
        String resourcePath = getPlatformSpecificEnvResourceFolder();
        List<String> files = filesInResourceFolder(resourcePath);
        for (String file : files) {
            try {
                File dest = new File(file.substring(resourcePath.length()));
                if (!file.contains(".")) { // Probably a dir
                    if (!dest.exists()) {
                        Files.createDirectory(dest.toPath());
                    }
                } else {
                    InputStream source = LibraryLoader.class.getResourceAsStream("/" + file);
                    System.out.println(file);;
                    Files.copy(source, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
    //https://stackoverflow.com/a/1435649/9314431
    private static List<String> filesInResourceFolder(String folder) {
        CodeSource src = LibraryLoader.class.getProtectionDomain().getCodeSource();
        List<String> list = new ArrayList<String>();

        try {
            if (src != null) {
                URL jar = src.getLocation();
                ZipInputStream zip = new ZipInputStream(jar.openStream());
                ZipEntry ze = null;
                for (ze = zip.getNextEntry(); ze != null; ze = zip.getNextEntry()) {
                    String entryName = ze.getName();
                    if (entryName.startsWith(folder)) {
                        list.add(entryName);
                    }
                }

            }
        } catch (IOException e) {
            return null;
        }
        return list;
    }

    private static boolean confirmWithUser() {
        int choice = JOptionPane.showConfirmDialog(null,
                "Since the GUI is enabled, files will be created in this folder. Proceed?", "",
                JOptionPane.YES_NO_OPTION);
        return choice == JOptionPane.YES_OPTION;
    }

    /**
     * Checks to see if the required binary files are in the PATH environmental variable
     */
    private static boolean binIsInPath() {
        return System.getenv("PATH").contains(binPath.getAbsolutePath());
    }

    private static String getPlatformSpecificEnvResourceFolder() {
        String operSys = System.getProperty("os.name").toLowerCase();
        if (operSys.contains("win")) {
            return "env/windows";
        } else if (operSys.contains("nix") || operSys.contains("nux")
                || operSys.contains("aix")) {
            return "env/linux";
        } else if (operSys.contains("mac")) {
            return "env/macOS";
        }
        JOptionPane.showMessageDialog(null,
                "Starfish GUI does not support this operating system. Use the headless version.",
                "", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
        return null;
    }

}
