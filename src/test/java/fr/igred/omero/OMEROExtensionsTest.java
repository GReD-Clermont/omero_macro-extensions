package fr.igred.omero;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


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
        Object[] args   = {2.0d, "toDelete", "toBeDeleted"};
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

}
