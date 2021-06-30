package fr.igred.omero;


import fr.igred.omero.annotations.TableWrapper;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


class OMEROExtensionTest {

    private OMEROExtension ext;


    @BeforeEach
    public void setUp() {
        ext = new OMEROExtension();
        Object[] args = {"omero", 4064d, "testUser", "password"};
        ext.handleExtension("connectToOMERO", args);
    }


    @AfterEach
    public void tearDown() {
        ext.handleExtension("disconnect", null);
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"list;projects;1,2",
                                         "list;project;1,2",
                                         "list;datasets;1,2,3",
                                         "list;dataset;1,2,3",
                                         "list;images;1,2,3,4",
                                         "list;image;1,2,3,4",
                                         "list;tags;1,2,3",
                                         "list;tag;1,2,3",})
    void testListAll(String extension, String type, String output) {
        Object[] args   = {type, null, null};
        String   result = ext.handleExtension(extension, args);
        assertEquals(output, result);
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"list;projects;TestProject;1,2",
                                         "list;project;TestProject;1,2",
                                         "list;datasets;TestDatasetImport;2",
                                         "list;dataset;TestDatasetImport;2",
                                         "list;images;image1.fake;1,2,4",
                                         "list;image;image1.fake;1,2,4",
                                         "list;tags;tag2;2",
                                         "list;tag;tag2;2",})
    void testListByName(String extension, String type, String name, String output) {
        Object[] args   = {type, name, null};
        String   result = ext.handleExtension(extension, args);
        assertEquals(output, result);
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
                                         "list;images;tag;1.0;1,2,4",
                                         "list;image;tags;1.0;1,2,4",})
    void testListFrom(String extension, String type, String parent, double id, String output) {
        Object[] args   = {type, parent, id};
        String   result = ext.handleExtension(extension, args);
        assertEquals(output, result);
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"getName;project;1.0;TestProject",
                                         "getName;projects;1.0;TestProject",
                                         "getName;dataset;1.0;TestDataset",
                                         "getName;datasets;1.0;TestDataset",
                                         "getName;images;1.0;image1.fake",
                                         "getName;image;1.0;image1.fake",
                                         "getName;tags;1.0;tag1",
                                         "getName;tag;1.0;tag1",})
    void testGetName(String extension, String type, double id, String output) {
        Object[] args   = {type, id};
        String   result = ext.handleExtension(extension, args);
        assertEquals(output, result);
    }


    @Test
    void testCreateProject() {
        Object[] args   = {"toDelete", "toBeDeleted"};
        String   result = ext.handleExtension("createProject", args);
        Double   id     = Double.parseDouble(result);
        Object[] args2  = {"project", id};
        ext.handleExtension("delete", args2);
        assertNotNull(id);
    }


    @Test
    void testCreateDataset() {
        Object[] args   = {"toDelete", "toBeDeleted", null};
        String   result = ext.handleExtension("createDataset", args);
        Double   id     = Double.parseDouble(result);
        Object[] args2  = {"dataset", id};
        ext.handleExtension("delete", args2);
        assertNotNull(id);
    }


    @Test
    void testCreateAndLinkTag() {
        Object[] args   = {"Project tag", "tag attached to a project"};
        String   result = ext.handleExtension("createTag", args);
        Double   id     = Double.parseDouble(result);
        Object[] args2  = {"tag", id, "project", 2.0};
        ext.handleExtension("link", args2);
        Object[] args3 = {"tag", id};
        ext.handleExtension("delete", args3);
        assertNotNull(id);
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"project;1.0;./test1.txt",
                                         "projects;1.0;./test2.txt",
                                         "dataset;1.0;./test3.txt",
                                         "datasets;1.0;./test4.txt",
                                         "images;1.0;./test5.txt",
                                         "image;1.0;./test6.txt",})
    void testAddAndDeleteFile(String type, double id, String filename) throws Exception {
        File file = new File(filename);
        if (!file.createNewFile())
            System.err.println("\"" + file.getCanonicalPath() + "\" could not be created.");

        byte[] array = new byte[2 * 262144 + 20];
        new Random().nextBytes(array);
        String generatedString = new String(array, StandardCharsets.UTF_8);
        try (PrintStream out = new PrintStream(new FileOutputStream(filename))) {
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
    void testGetImage() {
        ImagePlus imp = ext.getImage(1L);
        assertEquals(512, imp.getWidth());
        assertEquals(512, imp.getHeight());
    }


    @Test
    void testImportImage() throws IOException {
        String path = "./8bit-unsigned&pixelType=uint8&sizeZ=3&sizeC=5&sizeT=7&sizeX=512&sizeY=512.fake";
        File   f    = new File(path);
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

        Object[] args3 = {"image", "dataset", 2.0D};
        listIds = ext.handleExtension("list", args3);
        assertEquals("", listIds);
    }


    @Test
    @Disabled("Requires X11")
    void testTable() throws Exception {
        ResultsTable rt = new ResultsTable();
        rt.incrementCounter();
        rt.setLabel("test", 0);
        rt.setValue("Size", 0, 25.0);

        Object[] args = {"test_table", "test", 1.0d};
        ext.handleExtension("addToTable", args);

        Object[] args2 = {"test_table", "dataset", 1.0d};
        ext.handleExtension("saveTable", args2);

        Client client = new Client();
        client.connect("omero", 4064, "testUser", "password".toCharArray());
        List<TableWrapper> tables = client.getImage(1L).getTables(client);
        client.disconnect();
        assertEquals(1, tables.size());
        assertEquals(1, tables.get(0).getRowCount());
    }

}
