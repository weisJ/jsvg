package com.github.weisj.jsvg.renderer;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;

/**
 * TODO Refactor test suite into testFixtures to allow different implementations to use the files more easily
 */
public class FXTestSVGFiles {

    private static final String TEST_PATH = System.getenv("JAVAFX_TEST_SVG_PATH");

    public static File getTestSVGDirectory(){
        if(TEST_PATH != null){
            return new File(TEST_PATH);
        }
        // Fallback
        String srcDir = System.getProperty("user.dir");
        String testDir = srcDir + "/jsvg/src/test/resources";
        return new File(testDir);
    }

    public static List<String> findTestSVGFiles() {
        File dir = getTestSVGDirectory();
        if(dir.exists() && dir.isDirectory()) {
            return FileUtils.listFiles(dir, new String[] {"svg"}, true).stream().map(File::getAbsolutePath).toList();
        }
        return List.of();
    }
}
