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


import fr.igred.omero.Client;
import fr.igred.omero.annotations.TableWrapper;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


@ExtendWith(TestResultLogger.class)
class OMEROExtensionTest {

    private static final Object[] NULL_ARRAY = {null, null, null, null};

    private static final String HOSTNAME = "omero";
    private static final double PORT     = 4064;
    private static final String USERNAME = "testUser";
    private static final String PASSWORD = "password";

    private OMEROMacroExtension ext;


    @BeforeEach
    public void setUp() {
        ext = new OMEROMacroExtension();
        Object[] args = {HOSTNAME, PORT, USERNAME, PASSWORD};
        ext.handleExtension("connectToOMERO", args);
    }


    @AfterEach
    public void tearDown() {
        ext.handleExtension("disconnect", NULL_ARRAY);
    }


    @Test
    void testSwitchGroup() {
        final double target  = 4;
        final double initial = 3;
        Object[]     args    = {target};
        Object[]     args2   = {initial};
        String       result  = ext.handleExtension("switchGroup", args);
        String       result2 = ext.handleExtension("switchGroup", args2);
        assertEquals(target, Double.parseDouble(result));
        assertEquals(initial, Double.parseDouble(result2));
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {USERNAME + ";2",
                                         "all;-1",
                                         "'';-1",
                                         ";-1",})
    void testListForUser(String user, double output) {
        Object[] args    = {user};
        String   result  = ext.handleExtension("listForUser", args);
        Object[] args2   = {"projects", null, null};
        String   result2 = ext.handleExtension("list", args2);
        assertEquals(output, Double.parseDouble(result));
        assertEquals("1,2", result2);
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"list;projects;1,2",
                                         "list;project;1,2",
                                         "list;datasets;1,2,3",
                                         "list;dataset;1,2,3",
                                         "list;images;1,2,3,4,5,6,7,8,9,10",
                                         "list;image;1,2,3,4,5,6,7,8,9,10",
                                         "list;screens;1,2",
                                         "list;plates;1,2,3",
                                         "list;wells;1,2,3,4,5",
                                         "list;tags;1,2,3",
                                         "list;tag;1,2,3",})
    void testListAll(String extension, String type, String output) {
        Object[] args   = {type, null, null};
        String   result = ext.handleExtension(extension, args);
        assertEquals(output, result, String.format("\"%s\" failed for: %s", extension, type));
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"list;projects;TestProject;1,2",
                                         "list;project;TestProject;1,2",
                                         "list;datasets;TestDatasetImport;2",
                                         "list;dataset;TestDatasetImport;2",
                                         "list;images;image1.fake;1,2,4",
                                         "list;image;image1.fake;1,2,4",
                                         "list;screen;TestScreen;1",
                                         "list;plate;Plate Name 0;1,2",
                                         "list;tags;tag2;2",
                                         "list;tag;tag2;2",})
    void testListByName(String extension, String type, String name, String output) {
        Object[] args   = {type, name, null};
        String   result = ext.handleExtension(extension, args);
        assertEquals(output, result, String.format("\"%s\" failed for: %s,%s", extension, type, name));
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"list;datasets;project;1.0;1,2",
                                         "list;dataset;projects;1.0;1,2",
                                         "list;images;dataset;1.0;1,2,3",
                                         "list;image;datasets;1.0;1,2,3",
                                         "list;tags;image;1.0;1,2",
                                         "list;tag;images;1.0;1,2",
                                         "list;projects;tag;1.0;2",
                                         "list;project;tags;1.0;2",
                                         "list;datasets;tag;1.0;3",
                                         "list;dataset;tags;1.0;3",
                                         "list;images;project;1.0;1,2,3",
                                         "list;image;projects;1.0;1,2,3",
                                         "list;images;tag;1.0;1,2,4",
                                         "list;image;tags;1.0;1,2,4",
                                         "list;screens;tag;1.0;1",
                                         "list;plates;tag;1.0;1",
                                         "list;wells;tag;1.0;1",
                                         "list;tags;screen;1.0;1",
                                         "list;tags;plate;1.0;1",
                                         "list;tags;well;1.0;1",
                                         "list;plates;screen;2.0;2,3",
                                         "list;wells;screen;2.0;2,3,4,5",
                                         "list;images;screen;1.0;5,6",
                                         "list;wells;plate;2.0;2,3",
                                         "list;images;plate;2.0;7,8",
                                         "list;images;well;1.0;5,6",})
    void testListFrom(String extension, String type, String parent, double id, String output) {
        Object[] args   = {type, parent, id};
        String   result = ext.handleExtension(extension, args);

        String sortedIds = Arrays.stream(result.split(",")).map(Long::parseLong).sorted()
                                 .map(String::valueOf)
                                 .collect(Collectors.joining(","));

        assertEquals(output, sortedIds, String.format("\"%s\" failed for: %s,%s,%f", extension, type, parent, id));
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"getName;project;1.0;TestProject",
                                         "getName;projects;1.0;TestProject",
                                         "getName;dataset;1.0;TestDataset",
                                         "getName;datasets;1.0;TestDataset",
                                         "getName;images;1.0;image1.fake",
                                         "getName;image;1.0;image1.fake",
                                         "getName;screen;1.0;TestScreen",
                                         "getName;plate;2.0;Plate Name 0",
                                         "getName;well;1.0;Well A-1",
                                         "getName;tag;1.0;tag1",})
    void testGetName(String extension, String type, double id, String output) {
        Object[] args   = {type, id};
        String   result = ext.handleExtension(extension, args);
        assertEquals(output, result, String.format("\"%s\" failed for: %s,%f", extension, type, id));
    }


    @Test
    void testCreateProject() {
        Object[] args   = {"toDelete", "toBeDeleted"};
        String   result = ext.handleExtension("createProject", args);
        double   id     = Double.parseDouble(result);
        Object[] args2  = {"project", id};
        ext.handleExtension("delete", args2);
        assertNotEquals("", result);
    }


    @ParameterizedTest
    @NullSource
    @ValueSource(doubles = {1.0})
    void testCreateDataset(Double projectId) {
        Object[] args   = {"toDelete", "toBeDeleted", projectId};
        String   result = ext.handleExtension("createDataset", args);
        double   id     = Double.parseDouble(result);
        Object[] args2  = {"dataset", id};
        ext.handleExtension("delete", args2);
        assertNotEquals("", result);
    }


    @Test
    void testCreateAndLinkTag() {
        final double projectId = 2;
        Object[]     args      = {"Project tag", "tag attached to a project"};
        String       result    = ext.handleExtension("createTag", args);
        Double       id        = Double.parseDouble(result);
        Object[]     args2     = {"tag", id, "project", projectId};
        ext.handleExtension("link", args2);
        Object[] args3 = {"tag", id};
        ext.handleExtension("delete", args3);
        assertNotNull(id);
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"tag;1.0;project;2.0",
                                         "tag;1.0;dataset;3.0",
                                         "tag;1.0;screen;1.0",
                                         "tag;1.0;plate;1.0",
                                         "tag;1.0;well;1.0",
                                         "dataset;3.0;project;2.0",
                                         "image;1.0;dataset;1.0",})
    void testUnlinkThenLink(String type1, double id1, String type2, double id2) {
        Object[] listArgs = {type1, type2, id2};
        Object[] args     = {type1, id1, type2, id2};

        String res  = ext.handleExtension("list", listArgs);
        int    size = res.isEmpty() ? 0 : res.split(",").length;

        ext.handleExtension("unlink", args);
        String res2  = ext.handleExtension("list", listArgs);
        int    size2 = res2.isEmpty() ? 0 : res2.split(",").length;

        ext.handleExtension("link", args);
        String res3  = ext.handleExtension("list", listArgs);
        int    size3 = res3.isEmpty() ? 0 : res3.split(",").length;

        assertEquals(size - 1, size2, String.format("Unlinking failed for: %s,%f,%s,%f", type1, id1, type2, id2));
        assertEquals(size, size3, String.format("Linking failed for: %s,%f,%s,%f", type1, id1, type2, id2));
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"project;1.0;test1.txt",
                                         "projects;1.0;test2.txt",
                                         "dataset;1.0;test3.txt",
                                         "datasets;1.0;test4.txt",
                                         "images;1.0;test5.txt",
                                         "image;1.0;test6.txt",})
    void testAddAndDeleteFile(String type, double id, String filename) throws Exception {
        final int size = 2 * 262144 + 20;

        File file = new File(filename);
        if (!file.createNewFile())
            System.err.println("\"" + file.getCanonicalPath() + "\" could not be created.");

        byte[] array = new byte[size];
        new SecureRandom().nextBytes(array);
        String generatedString = new String(array, StandardCharsets.UTF_8);
        try (PrintStream out = new PrintStream(Files.newOutputStream(Paths.get(filename)),
                                               false,
                                               StandardCharsets.UTF_8.name())) {
            out.print(generatedString);
        }

        Object[] args   = {type, id, filename};
        String   result = ext.handleExtension("addFile", args);
        assertNotEquals(-1L, Long.parseLong(result));

        if (!file.delete())
            System.err.println("\"" + file.getCanonicalPath() + "\" could not be deleted.");

        double   fileId = Double.parseDouble(result);
        Object[] args2  = {fileId};
        ext.handleExtension("deleteFile", args2);
    }


    @Test
    void testListWellsByName() {
        Object[] args   = {"wells", "Well A-1", null};
        String   result = ext.handleExtension("list", args);

        Long[] ids = Arrays.stream(result.split(",")).map(Long::parseLong).sorted().toArray(Long[]::new);
        assertEquals(3, ids.length);
        assertEquals(1L, ids[0]);
    }


    @Test
    void testGetImage() {
        final int size = 512;
        ImagePlus imp  = ext.getImage(1L);
        assertEquals(size, imp.getWidth());
        assertEquals(size, imp.getHeight());
    }


    @Test
    void testSaveAndGetROIs() {
        ImagePlus imp     = ext.getImage(1L);
        Overlay   overlay = new Overlay();
        Roi       roi     = new Roi(25, 30, 70, 50);
        roi.setImage(imp);
        overlay.add(roi);
        imp.setOverlay(overlay);
        int savedROIs = ext.saveROIs(imp, 1L, "");
        overlay.clear();
        int loadedROIs = ext.getROIs(imp, 1L, true, "");
        ext.removeROIs(1L);
        int clearedROIs = ext.getROIs(imp, 1L, true, "");


        assertEquals(1, savedROIs);
        assertEquals(1, loadedROIs);
        assertEquals(1, imp.getOverlay().size());
        assertEquals(0, clearedROIs);
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"image;1;null;testKey1\ttestValue1\ttestKey2\t20",
                                         "image;3;' ';testKey1 testValue1 testKey2 20",
                                         "image;2;&&;testKey1&&testValue2&&testKey2&&30",
                                         "image;4;'';''"}, nullValues = {"null"})
    void testGetKeyValuePairs(String type, Double id, String separator, String output) {
        Object[] args   = {type, id, separator};
        String   result = ext.handleExtension("getKeyValuePairs", args);
        assertEquals(output, result);
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"image;1;testKey1;null;testValue1",
                                         "image;3;testKey2;null;20",
                                         "image;2;testKey2;null;30",
                                         "image;2;notExist;default;default"}, nullValues = {"null"})
    void testGetValue(String type, Double id, String key, String defaultValue, String output) {
        Object[] args   = {type, id, key, defaultValue};
        String   result = ext.handleExtension("getValue", args);
        assertEquals(output, result);
    }


    @Test
    void testImportImage() throws IOException {
        String path = "8bit-unsigned&pixelType=uint8&sizeZ=3&sizeC=5&sizeT=7&sizeX=512&sizeY=512.fake";
        File   f    = new File("." + File.separator + path);
        if (!f.createNewFile()) {
            System.err.println("\"" + f.getCanonicalPath() + "\" could not be created.");
            fail();
        }

        Object[] args1   = {2.0d, path};
        String   listIds = ext.handleExtension("importImage", args1);
        long[]   ids     = Arrays.stream(listIds.split(",")).mapToLong(Long::parseLong).toArray();
        assertNotNull(ids);
        assertEquals(1, ids.length);

        Object[] args2 = {"Image", (double) ids[0]};
        ext.handleExtension("delete", args2);

        Object[] args3    = {"image", "dataset", 2.0D};
        String   listIds2 = ext.handleExtension("list", args3);
        assertEquals("", listIds2);
        Files.deleteIfExists(f.toPath());
    }


    @Test
    void testDownloadImage() throws IOException {
        Object[]   args    = {1.0d, "."};
        String     results = ext.handleExtension("downloadImage", args);
        String[]   paths   = results.split(",");
        List<File> files   = Arrays.stream(paths).map(File::new).collect(Collectors.toList());
        assertEquals(2, paths.length);
        assertTrue(files.get(0).exists());
        assertTrue(files.get(1).exists());
        Files.deleteIfExists(files.get(0).toPath());
        Files.deleteIfExists(files.get(1).toPath());
    }


    @Test
    void testSudo() {
        ext.handleExtension("disconnect", NULL_ARRAY);

        Object[] args = {HOSTNAME, PORT, "root", "omero"};
        ext.handleExtension("connectToOMERO", args);

        Object[] args2 = {USERNAME};
        ext.handleExtension("sudo", args2);

        Object[] args3 = {"Project tag", "tag attached to a project"};

        String result = ext.handleExtension("createTag", args3);
        double id     = Double.parseDouble(result);

        Object[] args4 = {"tag", id, "project", 2.0};

        ext.handleExtension("link", args4);
        Object[] args5 = {"tag", id};

        ext.handleExtension("delete", args5);

        ext.handleExtension("endSudo", NULL_ARRAY);
        assertNotEquals("", result);
    }


    @Test
    void testTable() throws Exception {
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

        Object[] args2 = {"test_table", "dataset", 1.0d};
        ext.handleExtension("saveTable", args2);

        File     textFile = new File("test.txt");
        Object[] args3    = {"test_table", textFile.getCanonicalPath(), null};
        ext.handleExtension("saveTableAsFile", args3);

        NumberFormat formatter = NumberFormat.getInstance();
        formatter.setMaximumFractionDigits(4);
        String s1 = formatter.format(size1);
        String s2 = formatter.format(size2);

        String line1 = "\"Image\"\t\"Label\"\t\"Size\"";
        String line2 = String.format("\"%d\"\t\"%s\"\t\"%s\"", imageId, label1, s1);
        String line3 = String.format("\"%d\"\t\"%s\"\t\"%s\"", imageId, label2, s2);

        List<String> expected = Arrays.asList(line1, line2, line3);

        List<String> actual = Files.readAllLines(textFile.toPath());
        assertEquals(expected, actual);
        Files.deleteIfExists(textFile.toPath());

        Client client = new Client();
        client.connect(HOSTNAME, (int) PORT, USERNAME, PASSWORD.toCharArray());
        List<TableWrapper> tables = client.getDataset(1L).getTables(client);
        client.disconnect();
        assertEquals(1, tables.size());
        assertEquals(2, tables.get(0).getRowCount());
        assertEquals(size1, tables.get(0).getData(0, 2));
        assertEquals(size2, tables.get(0).getData(1, 2));
    }

}
