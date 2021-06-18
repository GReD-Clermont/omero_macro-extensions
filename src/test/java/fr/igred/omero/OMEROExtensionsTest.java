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
    @CsvSource(delimiter = ';', value = {"listProjects;-1.0;1,2,3",
                                         "getProjectName;2.0;TestProject",
                                         "listDatasetsInProject;2.0;1,2",
                                         "getDatasetName;1.0;TestDataset",
                                         "listImagesInDataset;1.0;1,2,3",
                                         "getImageName;1.0;image1.fake",})
    void testTextExtensions(String extension, double id, String output) {
        Object[] args = {id, new String[1]};
        if (id < 0) args[0] = args[1];
        ext.handleExtension(extension, args);
        assertEquals(output, ((String[]) args[args.length - 1])[0]);
    }

}
