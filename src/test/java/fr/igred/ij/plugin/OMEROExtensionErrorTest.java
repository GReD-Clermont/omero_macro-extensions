/*
 *  Copyright (C) 2021-2023 GReD
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package fr.igred.ij.plugin;


import ij.measure.ResultsTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


@ExtendWith(TestResultLogger.class)
class OMEROExtensionErrorTest {

    private static final Object[] NULL_ARRAY = {null, null, null, null};

    private final ByteArrayOutputStream outContent  = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent  = new ByteArrayOutputStream();
    private final PrintStream           originalOut = System.out;
    private final PrintStream           originalErr = System.err;

    private OMEROMacroExtension ext;


    @BeforeEach
    public void setUp() {
        final double port = 4064;
        ext = new OMEROMacroExtension();
        Object[] args = {"omero", port, "testUser", "password"};
        ext.handleExtension("connectToOMERO", args);
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }


    @AfterEach
    public void tearDown() {
        ext.handleExtension("disconnect", NULL_ARRAY);
        System.setOut(originalOut);
        System.setErr(originalErr);
    }


    @Test
    void testRun() throws IOException {
        ext.run("");
        String expected = String.format("The macro extensions are designed to be used within a macro.%n" +
                                        "Instructions on doing so will be printed to the Log window.%n");
        try (InputStream is = ext.getClass().getResourceAsStream("/helper.md")) {
            if (is != null) {
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                byte[]                buffer = new byte[2 ^ 10];
                int                   length = is.read(buffer);
                while (length != -1) {
                    result.write(buffer, 0, length);
                    length = is.read(buffer);
                }
                expected += result.toString("UTF-8");
            }
        }
        assertEquals(expected.trim(), outContent.toString().trim());
    }


    @Test
    void testNoSuchMethod() {
        ext.handleExtension("hello", NULL_ARRAY);
        String expected = "No such method: hello";
        assertEquals(expected, outContent.toString().trim());
    }


    @Test
    void testConnectionError() {
        final double port = 4064;
        ext.handleExtension("disconnect", NULL_ARRAY);
        Object[] args = {"omero", port, "omero", "password"};
        ext.handleExtension("connectToOMERO", args);
        String expected = "Could not connect: Cannot connect to OMERO";
        assertEquals(expected, outContent.toString().trim());
    }


    @Test
    void testDownloadImageError() {
        Object[] args = {-1.0d, "."};
        ext.handleExtension("downloadImage", args);
        String expected = "Could not download image: Image -1 doesn't exist in this context";
        assertEquals(expected, outContent.toString().trim());
    }


    @Test
    void testImportImageError() throws IOException {
        String path = "8bit-unsigned&pixelType=uint8&sizeZ=3&sizeC=5&sizeT=7&sizeX=512&sizeY=512.fake";
        File   f    = new File("." + File.separator + path);
        if (!f.createNewFile()) {
            System.err.println("\"" + f.getCanonicalPath() + "\" could not be created.");
            fail();
        }

        Object[] args1 = {-1.0d, path};
        ext.handleExtension("importImage", args1);
        String expected = "Could not import image: Dataset -1 doesn't exist in this context";
        assertEquals(expected, outContent.toString().trim());
        Files.deleteIfExists(f.toPath());
    }


    @Test
    void testGetImageError() {
        Object[] args = {-1.0d, null};
        ext.handleExtension("getImage", args);
        String expected = "Could not retrieve image: Image -1 doesn't exist in this context";
        assertEquals(expected, outContent.toString().trim());
    }


    @Test
    void testGetImageFromROIError() {
        Object[] args = {1.0d, "-1"};
        ext.handleExtension("getImage", args);
        String expected = "Could not retrieve image: ROI not found: -1";
        assertEquals(expected, outContent.toString().trim());
    }


    @Test
    void testListForUserError() {
        Object[] args = {"hello"};
        ext.handleExtension("listForUser", args);
        String expected = "Could not retrieve user: hello";
        assertEquals(expected, outContent.toString().trim());
    }


    @Test
    void testSudoError() {
        Object[] args = {"roger"};
        ext.handleExtension("sudo", args);
        String expected = "Could not switch user: User not found: roger";
        assertEquals(expected, outContent.toString().trim());
    }


    @Test
    void testEndSudoError() {
        ext.handleExtension("endSudo", NULL_ARRAY);
        String expected = "No sudo has been used before.";
        assertEquals(expected, outContent.toString().trim());
    }


    @Test
    void testListInvalidArgs() {
        final double datasetId = 2;
        Object[]     args      = {"dataset", null, datasetId};
        ext.handleExtension("list", args);
        String expected = "Second argument should not be null.";
        assertEquals(expected, outContent.toString().trim());
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"tag;hello;2",
                                         "hello;project;2",
                                         "hello;dataset;2",
                                         "hello;image;2",
                                         "hello;screen;2",
                                         "hello;plate;2",
                                         "hello;well;2",
                                         "hello;tag;2",
                                         "hello;kv-pair;2",
                                         "hello;TestDatasetImport;",
                                         "hello;;",})
    void testListInvalidType(String type1, String type2, Double id) {
        Object[] args   = {type1, type2, id};
        String   output = ext.handleExtension("list", args);
        String   error  = outContent.toString().trim();
        assertTrue(output.isEmpty());
        assertTrue(error.startsWith("Invalid type: hello. "));
    }


    @ParameterizedTest
    @ValueSource(strings = {"link", "unlink"})
    @Disabled("Methods no longer try to link or unlink invalid types currently")
    void testLinkUnlinkInvalidType(String function) {
        Object[] args = {"tag", 1.0, "hello", 1.0};
        ext.handleExtension(function, args);
        String expected = "Invalid type: hello.";
        assertEquals(expected, outContent.toString().trim());
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"link;hello;1.0;world;1.0",
                                         "unlink;hello;1.0;world;1.0",
                                         "link;image;image",
                                         "link;image;project",
                                         "link;image;screen",
                                         "link;tag;kv-pair"})
    void testCannotLinkOrUnlink(String function, String type1, String type2) {
        Object[] args = {type1, 1.0, type2, 1.0};
        ext.handleExtension(function, args);
        String expected = String.format("Cannot %s %s and %s", function, type1, type2);
        assertEquals(expected, outContent.toString().trim());
    }

    @Test
    void testKeyNotExist() {
        final String key = "notExist";
        final double imageId = 2;
        Object[]     args      = {"image", imageId, key, null};
        ext.handleExtension("getValue", args);
        String expected = "Could not retrieve value: Key \"" + key + "\" not found";
        assertEquals(expected, outContent.toString().trim());
    }


    @Test
    void testClearTable() throws Exception {
        long   imageId = 1L;
        String label1  = "test";
        String label2  = "test2";
        double size1   = 25.023579d;
        double size2   = 50.0d;

        ResultsTable rt1 = new ResultsTable();
        rt1.incrementCounter();
        rt1.setLabel(label1, 0);
        rt1.setValue("Size", 0, size1);

        ResultsTable rt2 = new ResultsTable();
        rt2.incrementCounter();
        rt2.setLabel(label2, 0);
        rt2.setValue("Size", 0, size2);

        ext.addToTable("test_table", rt1, imageId, new ArrayList<>(0), null);
        ext.addToTable("test_table", rt2, imageId, new ArrayList<>(0), null);

        Object[] args2 = {"test_table"};
        ext.handleExtension("clearTable", args2);

        File     textFile = new File("test.txt");
        Object[] args3    = {"test_table", textFile.getCanonicalPath(), null};
        ext.handleExtension("saveTableAsFile", args3);

        String expected = "Table does not exist: test_table";
        assertEquals(expected, outContent.toString().trim());
    }

}
