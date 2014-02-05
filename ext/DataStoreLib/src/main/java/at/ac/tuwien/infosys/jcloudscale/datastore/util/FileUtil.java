/*
   Copyright 2013 Rene Nowak 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package at.ac.tuwien.infosys.jcloudscale.datastore.util;

import at.ac.tuwien.infosys.jcloudscale.datastore.api.DatastoreException;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.DatastoreProperties;
import at.ac.tuwien.infosys.jcloudscale.datastore.configuration.PropertyLoaderImpl;
import org.apache.commons.io.FileUtils;
import sun.misc.BASE64Encoder;

import java.io.*;

public abstract class FileUtil {

    /**
     * Reads a file's content
     *
     * @param file file to read
     * @return content of the file
     * @throws IOException
     */
    public static String readFileContent(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String thisLine;
        while ((thisLine = bufferedReader.readLine()) != null) {
            stringBuilder.append(thisLine);
        }
        return stringBuilder.toString();
    }

    /**
     * Check if two files have the same content
     *
     * @param file1 first file
     * @param file2 second file
     * @return true if files have same content false otherwise
     * @throws IOException
     */
    public static boolean haveSameContent(File file1, File file2) throws IOException {
        String file1Content = readFileContent(file1);
        String file2Content = readFileContent(file2);
        return (file1Content == null || file2Content == null) ? null : file1Content.equals(file2Content);
    }

    /**
     * Check whether a given class is a file
     *
     * @param clazz given class
     * @return true if is file, false otherwise
     */
    public static boolean isFile(Class clazz) {
        return (clazz == null) ? false : clazz.equals(File.class);
    }

    /**
     * Get the file extension for a given file
     *
     * @param file given file
     * @return the extension of the file
     */
    public static String getFileExtension(File file) {
        int i = file.getName().lastIndexOf('.');
        if (i > 0) {
            return file.getName().substring(i+1);
        }
        return null;
    }

    /**
     * Get the path for temp files
     *
     * @return the path
     */
    public static String getTempPath() {
        PropertyLoaderImpl propertyLoader = new PropertyLoaderImpl(DatastoreProperties.DEFAULT_PROPERTIES_FILE);
        return propertyLoader.load(DatastoreProperties.TEMP_FILE_PATH);
    }

    /**
     * Create a temp file with the given name
     *
     * @param fileName given file name
     * @return the temp file with the given name
     */
    public static File createTempFile(String fileName) {
        String filePath = getTempPath();
        String absoluteFilePath = filePath + fileName;
        return new File(absoluteFilePath);
    }

    /**
     * Convert the content of the given file to a single string
     *
     * @param file given file
     * @return single string representing the file content
     */
    public static String toString(File file) {
        return encodeFile(file);
    }

    /**
     * Base64 encode the content of the given file
     *
     * @param file given file
     * @return the base64 encoded file content
     */
    private static String encodeFile(File file) {
        try {
            byte[] fileData = FileUtils.readFileToByteArray(file);
            return encodeDataBase64(fileData);
        } catch (IOException e) {
            throw new DatastoreException("Error reading file " + file.getName() + ":" + e.getMessage());
        }
    }

    /**
     * Base64 encode the given byte array
     *
     * @param data the given byte array
     * @return the base64 encoded byte array
     */
    private static String encodeDataBase64(byte[] data) {
        BASE64Encoder base64Encoder = new BASE64Encoder();
        return base64Encoder.encode(data);
    }
}
