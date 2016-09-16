package org.alfresco.solr;

import org.apache.commons.io.FileUtils;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.CoreContainer;

import java.io.*;

/**
 * Methods taken from AlfrescoCoreAdminHandler that deal with I/O resources
 */
public class HandlerOfResources {

    public static void copyDirectory(File srcDir, File destDir, boolean preserveFileDate) throws IOException
    {
        FileUtils.copyDirectory(srcDir,destDir,preserveFileDate);
    }

    public static void copyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException
    {
        FileUtils.copyFile(srcFile,destFile,preserveFileDate);
    }

    public void deleteDirectory(File directory) throws IOException
    {
        FileUtils.deleteDirectory(directory);
    }

    /**
     * Opens an InputStream
     * @param solrHome
     * @param resource
     * @return InputStream
     */
    public static InputStream openResource(String solrHome, String resource)
    {
        InputStream is = null;
        try
        {
            File f0 = new File(resource);
            File f = f0;
            if (!f.isAbsolute())
            {
                // try $CWD/$configDir/$resource
                String path = solrHome;
                path = path.endsWith("/") ? path : path + "/";
                f = new File(path + resource);
            }
            if (f.isFile() && f.canRead())
            {
                return new FileInputStream(f);
            }
            else if (f != f0)
            { // no success with $CWD/$configDir/$resource
                if (f0.isFile() && f0.canRead()) return new FileInputStream(f0);
            }
            // delegate to the class loader (looking into $INSTANCE_DIR/lib jars)
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error opening " + resource, e);
        }
        if (is == null) { throw new RuntimeException("Can't find resource '" + resource + "' in classpath or '"
                + solrHome + "', cwd=" + System.getProperty("user.dir")); }
        return is;
    }

    /**
     * Safely gets a boolean from SolrParams
     * @param params
     * @param paramName
     * @return boolean
     */
    public static boolean getSafeBoolean(SolrParams params, String paramName)
    {
        boolean paramValue = false;
        if (params.get(paramName) != null)
        {
            paramValue = Boolean.valueOf(params.get(paramName));
        }
        return paramValue;
    }

    /**
     * Safely gets a Long from SolrParams
     * @param params
     * @param paramName
     * @return Long
     */
    public static Long getSafeLong(SolrParams params, String paramName)
    {
        Long paramValue = null;
        if (params.get(paramName) != null)
        {
            paramValue = Long.valueOf(params.get(paramName));
        }
        return paramValue;
    }
}
