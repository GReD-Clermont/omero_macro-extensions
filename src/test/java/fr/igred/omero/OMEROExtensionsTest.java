package fr.igred.omero;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;


class OMEROExtensionsTest {

    private OMEROExtensions ext;


    @BeforeEach
    public void setUp() {
        ext = new OMEROExtensions();
        Object[] args = {"omero", 4064d, "testUser", "password"};
        ext.handleExtension("connectToOMERO", args);
    }


    @AfterEach
    public void tearDown() {
        ext.handleExtension("disconnect", null);
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"list;projects;1,2,3",
                                         "list;project;1,2,3",
                                         "list;datasets;1,2,3",
                                         "list;dataset;1,2,3",
                                         "list;images;1,2,3,4",
                                         "list;image;1,2,3,4",
                                         "list;tags;1,2,3",
                                         "list;tag;1,2,3",})
    void testListAll(String extension, String type, String output) {
        Object[] args = {type, new String[1]};
        ext.handleExtension(extension, args);
        assertEquals(output, ((String[]) args[args.length - 1])[0]);
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"list;projects;TestProject;2,3",
                                         "list;project;TestProject;2,3",
                                         "list;datasets;TestDatasetImport;2",
                                         "list;dataset;TestDatasetImport;2",
                                         "list;images;image1.fake;1,2,4",
                                         "list;image;image1.fake;1,2,4",
                                         "list;tags;tag2;2",
                                         "list;tag;tag2;2",})
    void testListByName(String extension, String type, String name, String output) {
        Object[] args = {type, name, new String[1]};
        ext.handleExtension(extension, args);
        assertEquals(output, ((String[]) args[args.length - 1])[0]);
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"list;datasets;project;2.0;1,2",
                                         "list;dataset;projects;2.0;1,2",
                                         "list;images;dataset;1.0;1,2,3",
                                         "list;image;datasets;1.0;1,2,3",})
    void testListFrom(String extension, String type, String parent, double id, String output) {
        Object[] args = {type, parent, id, new String[1]};
        ext.handleExtension(extension, args);
        assertEquals(output, ((String[]) args[args.length - 1])[0]);
    }


    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {"getName;project;2.0;TestProject",
                                         "getName;projects;2.0;TestProject",
                                         "getName;dataset;1.0;TestDataset",
                                         "getName;datasets;1.0;TestDataset",
                                         "getName;images;1.0;image1.fake",
                                         "getName;image;1.0;image1.fake",
                                         "getName;tags;1.0;tag1",
                                         "getName;tag;1.0;tag1",})
    void testGetName(String extension, String type, double id, String output) {
        Object[] args = {type, id, new String[1]};
        ext.handleExtension(extension, args);
        assertEquals(output, ((String[]) args[args.length - 1])[0]);
    }

}
