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
import fr.igred.omero.GenericObjectWrapper;
import fr.igred.omero.annotations.TableWrapper;
import fr.igred.omero.annotations.TagAnnotationWrapper;
import fr.igred.omero.exception.AccessException;
import fr.igred.omero.exception.OMEROServerError;
import fr.igred.omero.exception.ServiceException;
import fr.igred.omero.meta.ExperimenterWrapper;
import fr.igred.omero.repository.DatasetWrapper;
import fr.igred.omero.repository.GenericRepositoryObjectWrapper;
import fr.igred.omero.repository.ImageWrapper;
import fr.igred.omero.repository.PlateWrapper;
import fr.igred.omero.repository.ProjectWrapper;
import fr.igred.omero.repository.ScreenWrapper;
import fr.igred.omero.repository.WellSampleWrapper;
import fr.igred.omero.repository.WellWrapper;
import fr.igred.omero.roi.ROIWrapper;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.macro.ExtensionDescriptor;
import ij.macro.Functions;
import ij.macro.MacroExtension;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static ij.macro.ExtensionDescriptor.newDescriptor;


public class OMEROMacroExtension implements PlugIn, MacroExtension {

    private static final String PROJECT = "project";
    private static final String DATASET = "dataset";
    private static final String IMAGE   = "image";
    private static final String SCREEN  = "screen";
    private static final String PLATE   = "plate";
    private static final String WELL    = "well";
    private static final String TAG     = "tag";
    private static final String INVALID = "Invalid type";

    private static final String ERROR_POSSIBLE_VALUES = "%s: %s. Possible values are: %s";
    private static final String ERROR_RETRIEVE_IN     = "Could not retrieve %s in %s: %s";
    public static final  char   DEFAULT_DELIMITER     = '\t';

    private final ExtensionDescriptor[] extensions = {
            newDescriptor("connectToOMERO", this, ARG_STRING, ARG_NUMBER, ARG_STRING, ARG_STRING),
            newDescriptor("switchGroup", this, ARG_NUMBER),
            newDescriptor("listForUser", this, ARG_STRING),
            newDescriptor("list", this, ARG_STRING, ARG_STRING + ARG_OPTIONAL, ARG_NUMBER + ARG_OPTIONAL),
            newDescriptor("createDataset", this, ARG_STRING, ARG_STRING, ARG_NUMBER + ARG_OPTIONAL),
            newDescriptor("createProject", this, ARG_STRING, ARG_STRING),
            newDescriptor("createTag", this, ARG_STRING, ARG_STRING),
            newDescriptor("link", this, ARG_STRING, ARG_NUMBER, ARG_STRING, ARG_NUMBER),
            newDescriptor("unlink", this, ARG_STRING, ARG_NUMBER, ARG_STRING, ARG_NUMBER),
            newDescriptor("addFile", this, ARG_STRING, ARG_NUMBER, ARG_STRING),
            newDescriptor("addToTable", this, ARG_STRING,
                          ARG_STRING + ARG_OPTIONAL, ARG_NUMBER + ARG_OPTIONAL, ARG_STRING + ARG_OPTIONAL),
            newDescriptor("saveTable", this, ARG_STRING, ARG_STRING, ARG_NUMBER),
            newDescriptor("saveTableAsFile", this, ARG_STRING, ARG_STRING, ARG_STRING + ARG_OPTIONAL),
            newDescriptor("clearTable", this, ARG_STRING),
            newDescriptor("importImage", this, ARG_NUMBER, ARG_STRING + ARG_OPTIONAL),
            newDescriptor("downloadImage", this, ARG_NUMBER, ARG_STRING),
            newDescriptor("delete", this, ARG_STRING, ARG_NUMBER),
            newDescriptor("getName", this, ARG_STRING, ARG_NUMBER),
            newDescriptor("getImage", this, ARG_NUMBER),
            newDescriptor("getROIs", this, ARG_NUMBER, ARG_NUMBER + ARG_OPTIONAL, ARG_STRING + ARG_OPTIONAL),
            newDescriptor("saveROIs", this, ARG_NUMBER, ARG_STRING + ARG_OPTIONAL),
            newDescriptor("removeROIs", this, ARG_NUMBER, ARG_STRING + ARG_OPTIONAL),
            newDescriptor("sudo", this, ARG_STRING),
            newDescriptor("endSudo", this),
            newDescriptor("disconnect", this),
            };

    private final Map<String, TableWrapper> tables = new HashMap<>(1);

    private Client client   = new Client();
    private Client switched = null;

    private ExperimenterWrapper user = null;


    /**
     * Converts a list of GenericObjectWrappers to a comma-delimited list of IDs.
     *
     * @param list The objects list.
     * @param <T>  The type of objects.
     *
     * @return A string containing the corresponding IDs, separated by commas.
     */
    private static <T extends GenericObjectWrapper<?>> String listToIDs(Collection<T> list) {
        return list.stream()
                   .mapToLong(T::getId)
                   .mapToObj(String::valueOf)
                   .collect(Collectors.joining(","));
    }


    /**
     * Makes sure the requested type is singular and lower case.
     *
     * @param type The type.
     *
     * @return The corrected type.
     */
    private static String singularType(String type) {
        String singular = type.toLowerCase(Locale.ROOT);
        int    length   = singular.length();
        //noinspection MagicCharacter
        if (singular.charAt(length - 1) == 's') {
            singular = singular.substring(0, length - 1);
        }
        return singular;
    }


    /**
     * Converts a Double to a Long.
     *
     * @param d The Double.
     *
     * @return The corresponding Long.
     */
    private static Long doubleToLong(Double d) {
        return d != null ? d.longValue() : null;
    }


    /**
     * Gets the results table with the specified name, or the active table if null.
     *
     * @param resultsName The name of the ResultsTable.
     *
     * @return The corresponding ResultsTable.
     */
    private static ResultsTable getTable(String resultsName) {
        if (resultsName == null) return ResultsTable.getResultsTable();
        else return ResultsTable.getResultsTable(resultsName);
    }


    /**
     * Filters the objects list to only keep objects from the set user.
     *
     * @param list The objects list.
     * @param <T>  The type of objects.
     *
     * @return The filtered list.
     */
    private <T extends GenericObjectWrapper<?>> List<T> filterUser(List<T> list) {
        if (user == null) return list;
        else return list.stream().filter(o -> o.getOwner().getId() == user.getId()).collect(Collectors.toList());
    }


    /**
     * Retrieves the object of the specified type with the specified ID.
     *
     * @param type The type of object.
     * @param id   The object ID.
     *
     * @return The object.
     */
    private GenericObjectWrapper<?> getObject(String type, long id) {
        String singularType = singularType(type);

        GenericObjectWrapper<?> object = null;
        if (singularType.equals(TAG)) {
            try {
                object = client.getTag(id);
            } catch (OMEROServerError | ServiceException e) {
                IJ.error("Could not retrieve tag: " + e.getMessage());
            }
        } else {
            object = getRepositoryObject(type, id);
        }
        return object;
    }


    /**
     * Retrieves the repository object of the specified type with the specified ID.
     *
     * @param type The type of object.
     * @param id   The object ID.
     *
     * @return The object.
     */
    private GenericRepositoryObjectWrapper<?> getRepositoryObject(String type, long id) {
        String singularType = singularType(type);

        GenericRepositoryObjectWrapper<?> object = null;
        try {
            switch (singularType) {
                case PROJECT:
                    object = client.getProject(id);
                    break;
                case DATASET:
                    object = client.getDataset(id);
                    break;
                case IMAGE:
                    object = client.getImage(id);
                    break;
                case SCREEN:
                    object = client.getScreen(id);
                    break;
                case PLATE:
                    object = client.getPlate(id);
                    break;
                case WELL:
                    object = client.getWell(id);
                    break;
                default:
                    IJ.error(INVALID + ": " + type + ".");
            }
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error("Could not retrieve object: " + e.getMessage());
        }
        return object;
    }


    /**
     * Lists the objects of the specified type linked to a tag.
     *
     * @param type The object type.
     * @param id   The tag id.
     *
     * @return A list of GenericObjectWrappers.
     */
    private List<? extends GenericObjectWrapper<?>> listForTag(String type, long id)
    throws ServiceException, OMEROServerError, AccessException, ExecutionException {
        String singularType = singularType(type);

        List<? extends GenericObjectWrapper<?>> objects = new ArrayList<>(0);
        TagAnnotationWrapper                    tag     = client.getTag(id);
        switch (singularType) {
            case PROJECT:
                objects = tag.getProjects(client);
                break;
            case DATASET:
                objects = tag.getDatasets(client);
                break;
            case IMAGE:
                objects = tag.getImages(client);
                break;
            case SCREEN:
                objects = tag.getScreens(client);
                break;
            case PLATE:
                objects = tag.getPlates(client);
                break;
            case WELL:
                objects = tag.getWells(client);
                break;
            default:
                String msg = String.format(ERROR_POSSIBLE_VALUES,
                                           INVALID, type,
                                           "projects, datasets, images, screens, plates or wells.");
                IJ.error(msg);
        }
        return objects;
    }


    /**
     * Lists the objects of the specified type inside a project.
     *
     * @param type The object type.
     * @param id   The project id.
     *
     * @return A list of GenericObjectWrappers.
     */
    private List<? extends GenericObjectWrapper<?>> listForProject(String type, long id)
    throws AccessException, ServiceException, ExecutionException {
        String singularType = singularType(type);

        List<? extends GenericObjectWrapper<?>> objects = new ArrayList<>(0);
        ProjectWrapper                          project = client.getProject(id);
        switch (singularType) {
            case DATASET:
                objects = project.getDatasets();
                break;
            case IMAGE:
                objects = project.getImages(client);
                break;
            case TAG:
                objects = project.getTags(client);
                break;
            default:
                IJ.error(String.format(ERROR_POSSIBLE_VALUES, INVALID, type, "datasets, images or tags."));
        }
        return objects;
    }


    /**
     * Lists the objects of the specified type inside a dataset.
     *
     * @param type The object type.
     * @param id   The dataset id.
     *
     * @return A list of GenericObjectWrappers.
     */
    private List<? extends GenericObjectWrapper<?>> listForDataset(String type, long id)
    throws AccessException, ServiceException, ExecutionException {
        String singularType = singularType(type);

        List<? extends GenericObjectWrapper<?>> objects = new ArrayList<>(0);
        DatasetWrapper                          dataset = client.getDataset(id);
        switch (singularType) {
            case IMAGE:
                objects = dataset.getImages(client);
                break;
            case TAG:
                objects = dataset.getTags(client);
                break;
            default:
                IJ.error(String.format(ERROR_POSSIBLE_VALUES, INVALID, type, "images or tags."));
        }
        return objects;
    }


    /**
     * Lists the objects of the specified type inside a screen.
     *
     * @param type The object type.
     * @param id   The screen id.
     *
     * @return A list of GenericObjectWrappers.
     */
    private List<? extends GenericObjectWrapper<?>> listForScreen(String type, long id)
    throws AccessException, ServiceException, ExecutionException {
        String singularType = singularType(type);

        List<? extends GenericObjectWrapper<?>> objects = new ArrayList<>(0);
        ScreenWrapper                           screen  = client.getScreen(id);
        List<PlateWrapper>                      plates  = screen.getPlates();
        switch (singularType) {
            case PLATE:
                objects = plates;
                break;
            case WELL:
                List<WellWrapper> wells = new ArrayList<>();
                for (PlateWrapper plate : plates) {
                    wells.addAll(plate.getWells(client));
                }
                wells.sort(Comparator.comparing(GenericObjectWrapper::getId));
                objects = wells;
                break;
            case IMAGE:
                List<ImageWrapper> images = new ArrayList<>();
                for (PlateWrapper plate : plates) {
                    for (WellWrapper well : plate.getWells(client)) {
                        images.addAll(well.getWellSamples().stream()
                                          .map(WellSampleWrapper::getImage)
                                          .collect(Collectors.toList()));
                    }
                }
                images.sort(Comparator.comparing(GenericObjectWrapper::getId));
                objects = images;
                break;
            case TAG:
                objects = screen.getTags(client);
                break;
            default:
                IJ.error(String.format(ERROR_POSSIBLE_VALUES, INVALID, type, "plates, wells, images or tags."));
        }
        return objects;
    }


    /**
     * Lists the objects of the specified type inside a plate.
     *
     * @param type The object type.
     * @param id   The plate id.
     *
     * @return A list of GenericObjectWrappers.
     */
    private List<? extends GenericObjectWrapper<?>> listForPlate(String type, long id)
    throws AccessException, ServiceException, ExecutionException {
        String singularType = singularType(type);

        List<? extends GenericObjectWrapper<?>> objects = new ArrayList<>(0);
        PlateWrapper                            plate   = client.getPlate(id);
        switch (singularType) {
            case WELL:
                objects = plate.getWells(client);
                break;
            case IMAGE:
                objects = plate.getWells(client)
                               .stream()
                               .flatMap(w -> w.getWellSamples().stream())
                               .map(WellSampleWrapper::getImage)
                               .collect(Collectors.toList());
                break;
            case TAG:
                objects = plate.getTags(client);
                break;
            default:
                IJ.error(String.format(ERROR_POSSIBLE_VALUES, INVALID, type, "wells, images or tags."));
        }
        return objects;
    }


    /**
     * Connects the client to OMERO.
     *
     * @param hostname The host name.
     * @param port     The port.
     * @param username The username.
     * @param password The password.
     *
     * @return True if connected, false otherwise.
     */
    public boolean connect(String hostname, int port, String username, String password) {
        boolean connected = false;
        try {
            client.connect(hostname, port, username, password.toCharArray());
            connected = true;
        } catch (ServiceException e) {
            IJ.error("Could not connect: " + e.getMessage());
        }
        return connected;
    }


    /**
     * Sets the user whose objects should be listed with the "list" commands.
     *
     * @param username The username. Null, empty and "all" removes the filter.
     *
     * @return The user ID if set, -1 otherwise.
     */
    public long setUser(String username) {
        long id = -1L;
        if (username != null && !username.trim().isEmpty() && !"all".equalsIgnoreCase(username)) {
            if (user != null) id = user.getId();
            ExperimenterWrapper newUser = null;
            try {
                newUser = client.getUser(username);
            } catch (ExecutionException | ServiceException | AccessException | NoSuchElementException e) {
                IJ.log("Could not retrieve user: " + username);
            }
            if (newUser != null) {
                user = newUser;
                id = user.getId();
            }
        } else {
            user = null;
        }
        return id;
    }


    /**
     * Downloads the specified image.
     *
     * @param imageId The image ID.
     * @param path    The path where the file(s) should be downloaded.
     *
     * @return The file path. If multiple files were saved, they are comma-delimited.
     */
    public String downloadImage(long imageId, String path) {
        List<File> files = new ArrayList<>(0);
        try {
            files = client.getImage(imageId).download(client, path);
        } catch (ServiceException | AccessException | OMEROServerError | ExecutionException |
                 NoSuchElementException e) {
            IJ.error("Could not download image: " + e.getMessage());
        }
        return files.stream().map(File::toString).collect(Collectors.joining(","));
    }


    /**
     * Imports the specified image file to the desired dataset.
     *
     * @param datasetId The dataset ID.
     * @param path      The path to the image file.
     *
     * @return The list of imported IDs, separated by commas.
     */
    public String importImage(long datasetId, String path) {
        String imagePath = path;
        if (path == null) {
            ImagePlus imp = IJ.getImage();
            imagePath = IJ.getDir("temp") + imp.getTitle() + ".tif";
            IJ.save(imp, imagePath);
        }
        List<Long> imageIds = new ArrayList<>(0);
        try {
            imageIds = client.getDataset(datasetId).importImage(client, imagePath);
        } catch (ServiceException | AccessException | ExecutionException | OMEROServerError | NoSuchElementException e) {
            IJ.error("Could not import image: " + e.getMessage());
        }
        if (path == null) {
            try {
                Files.deleteIfExists(new File(imagePath).toPath());
            } catch (IOException e) {
                IJ.error("Could not delete temp image: " + e.getMessage());
            }
        }
        return imageIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }


    /**
     * Adds a file to an object.
     *
     * @param type The object type.
     * @param id   The object ID.
     * @param path The path to the file.
     *
     * @return The uploaded file ID.
     */
    public long addFile(String type, long id, String path) {
        long fileId = -1;

        File file = new File(path);

        GenericRepositoryObjectWrapper<?> object = getRepositoryObject(type, id);
        if (object != null && file.isFile()) {
            try {
                fileId = object.addFile(client, file);
            } catch (ExecutionException e) {
                IJ.error("Could not add file to object: " + e.getMessage());
            } catch (InterruptedException e) {
                IJ.error(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        return fileId;
    }


    /**
     * Deletes the file with the specified ID from OMERO.
     *
     * @param id The file ID.
     */
    public void deleteFile(long id) {
        try {
            client.deleteFile(id);
        } catch (ServiceException | AccessException | ExecutionException | OMEROServerError e) {
            IJ.error("Could not delete file: " + e.getMessage());
        } catch (InterruptedException e) {
            IJ.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }


    /**
     * Adds the content of a ResultsTable (for an image) to the table with the specified name.
     *
     * @param tableName The table name.
     * @param results   The ResultsTable.
     * @param imageId   The image ID (can be null).
     * @param ijRois    The list of ImageJ ROIs.
     * @param property  The ROI property to group shapes.
     */
    public void addToTable(String tableName, ResultsTable results, Long imageId, List<? extends Roi> ijRois,
                           String property) {
        TableWrapper table = tables.get(tableName);

        if (results == null) {
            IJ.error("Results table does not exist.");
        } else {
            try {
                if (table == null) {
                    table = new TableWrapper(client, results, imageId, ijRois, property);
                    table.setName(tableName);
                    tables.put(tableName, table);
                } else {
                    table.addRows(client, results, imageId, ijRois, property);
                }
            } catch (ExecutionException | ServiceException | AccessException e) {
                IJ.error("Could not add results to table: " + e.getMessage());
            }
        }
    }


    /**
     * Saves the specified table as a file, using the specified delimiter.
     *
     * @param tableName The table name.
     * @param path      The path to the file.
     * @param delimiter The desired delimiter. If null, defaults to '\t'.
     */
    public void saveTableAsFile(String tableName, String path, CharSequence delimiter) {
        TableWrapper table = tables.get(tableName);

        char sep = delimiter == null || delimiter.length() != 1 ? DEFAULT_DELIMITER : delimiter.charAt(0);
        try {
            table.saveAs(path, sep);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            IJ.error("Could not create table file: ", e.getMessage());
        }
    }


    /**
     * Saves a table to an object on OMERO.
     *
     * @param tableName The table name.
     * @param type      The object type.
     * @param id        The object ID.
     */
    public void saveTable(String tableName, String type, long id) {
        GenericRepositoryObjectWrapper<?> object = getRepositoryObject(type, id);
        if (object != null) {
            TableWrapper table = tables.get(tableName);
            if (table != null) {
                String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.ROOT)
                                                    .format(ZonedDateTime.now());
                String newName;
                if (tableName == null || tableName.isEmpty()) newName = timestamp + "_" + table.getName();
                else newName = timestamp + "_" + tableName;
                table.setName(newName);
                try {
                    object.addTable(client, table);
                } catch (ExecutionException | ServiceException | AccessException e) {
                    IJ.error("Could not save table: " + e.getMessage());
                }
            } else {
                throw new IllegalAccessError("Table is empty!");
            }
        }
    }


    /**
     * Creates a tag on OMERO.
     *
     * @param name        The tag name.
     * @param description The tag description.
     *
     * @return The tag ID.
     */
    public long createTag(String name, String description) {
        long id = -1;
        try {
            TagAnnotationWrapper tag = new TagAnnotationWrapper(client, name, description);
            id = tag.getId();
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error("Could not create tag: " + e.getMessage());
        }
        return id;
    }


    /**
     * Creates a project on OMERO.
     *
     * @param name        The project name.
     * @param description The project description.
     *
     * @return The project ID.
     */
    public long createProject(String name, String description) {
        long id = -1;
        try {
            ProjectWrapper project = new ProjectWrapper(client, name, description);
            id = project.getId();
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error("Could not create project: " + e.getMessage());
        }
        return id;
    }


    /**
     * Creates a dataset on OMERO.
     *
     * @param name        The dataset name.
     * @param description The dataset description.
     * @param projectId   The ID of the parent project.
     *
     * @return The dataset ID.
     */
    public long createDataset(String name, String description, Long projectId) {
        long id = -1;
        try {
            DatasetWrapper dataset;
            if (projectId != null) {
                dataset = client.getProject(projectId).addDataset(client, name, description);
            } else {
                dataset = new DatasetWrapper(name, description);
                dataset.saveAndUpdate(client);
            }
            id = dataset.getId();
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error("Could not create dataset: " + e.getMessage());
        }
        return id;
    }


    /**
     * Deletes an object on OMERO.
     *
     * @param type The object type.
     * @param id   The object ID.
     */
    public void delete(String type, long id) {
        GenericObjectWrapper<?> object = getObject(type, id);
        try {
            if (object != null) client.delete(object);
        } catch (ServiceException | AccessException | ExecutionException | OMEROServerError e) {
            IJ.error("Could not delete " + type + ": " + e.getMessage());
        } catch (InterruptedException e) {
            IJ.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }


    /**
     * Lists the objects of the specified type.
     *
     * @param type The objects type.
     *
     * @return The comma-delimited list of object IDs.
     */
    public String list(String type) {
        String singularType = singularType(type);

        String results = "";
        try {
            switch (singularType) {
                case PROJECT:
                    List<ProjectWrapper> projects = client.getProjects();
                    results = listToIDs(filterUser(projects));
                    break;
                case DATASET:
                    List<DatasetWrapper> datasets = client.getDatasets();
                    results = listToIDs(filterUser(datasets));
                    break;
                case IMAGE:
                    List<ImageWrapper> images = client.getImages();
                    results = listToIDs(filterUser(images));
                    break;
                case SCREEN:
                    List<ScreenWrapper> screens = client.getScreens();
                    results = listToIDs(filterUser(screens));
                    break;
                case PLATE:
                    List<PlateWrapper> plates = client.getPlates();
                    results = listToIDs(filterUser(plates));
                    break;
                case WELL:
                    List<WellWrapper> wells = client.getWells();
                    results = listToIDs(filterUser(wells));
                    break;
                case TAG:
                    List<TagAnnotationWrapper> tags = client.getTags();
                    results = listToIDs(filterUser(tags));
                    break;
                default:
                    String msg = String.format(ERROR_POSSIBLE_VALUES, INVALID, type,
                                               "projects, datasets, images, screens, plates, wells or tags.");
                    IJ.error(msg);
            }
        } catch (ServiceException | AccessException | OMEROServerError | ExecutionException e) {
            IJ.error("Could not retrieve " + type + ": " + e.getMessage());
        }
        return results;
    }


    /**
     * Lists the objects of the specified type with the specified name.
     *
     * @param type The objects type.
     * @param name The objects name.
     *
     * @return The comma-delimited list of object IDs.
     */
    public String list(String type, String name) {
        String singularType = singularType(type);

        String results = "";
        try {
            switch (singularType) {
                case PROJECT:
                    List<ProjectWrapper> projects = client.getProjects(name);
                    results = listToIDs(filterUser(projects));
                    break;
                case DATASET:
                    List<DatasetWrapper> datasets = client.getDatasets(name);
                    results = listToIDs(filterUser(datasets));
                    break;
                case IMAGE:
                    List<ImageWrapper> images = client.getImages(name);
                    results = listToIDs(filterUser(images));
                    break;
                case SCREEN:
                    List<ScreenWrapper> screens = client.getScreens();
                    screens.removeIf(s -> !name.equals(s.getName()));
                    results = listToIDs(filterUser(screens));
                    break;
                case PLATE:
                    List<PlateWrapper> plates = client.getPlates();
                    plates.removeIf(p -> !name.equals(p.getName()));
                    results = listToIDs(filterUser(plates));
                    break;
                case WELL:
                    List<WellWrapper> wells = client.getWells();
                    wells.removeIf(w -> !name.equals(w.getName()));
                    results = listToIDs(filterUser(wells));
                    break;
                case TAG:
                    List<TagAnnotationWrapper> tags = client.getTags(name);
                    results = listToIDs(filterUser(tags));
                    break;
                default:
                    String msg = String.format(ERROR_POSSIBLE_VALUES, INVALID, type,
                                               "projects, datasets, images, screens, plates, wells or tags.");
                    IJ.error(msg);
            }
        } catch (ServiceException | AccessException | OMEROServerError | ExecutionException e) {
            IJ.error(String.format("Could not retrieve %s with name \"%s\": %s", type, name, e.getMessage()));
        }
        return results;
    }


    /**
     * Lists the objects of the specified type inside the specified container.
     *
     * @param type   The object type.
     * @param parent The type of container.
     * @param id     The container id.
     *
     * @return The comma-delimited list of object IDs.
     */
    public String list(String type, String parent, long id) {
        String singularParent = singularType(parent);
        String singularType   = singularType(type);

        Collection<? extends GenericObjectWrapper<?>> objects = new ArrayList<>(0);
        try {
            switch (singularParent) {
                case PROJECT:
                    objects = listForProject(type, id);
                    break;
                case DATASET:
                    objects = listForDataset(type, id);
                    break;
                case TAG:
                    objects = listForTag(type, id);
                    break;
                case SCREEN:
                    objects = listForScreen(type, id);
                    break;
                case PLATE:
                    objects = listForPlate(type, id);
                    break;
                case WELL:
                    WellWrapper well = client.getWell(id);
                    if (IMAGE.equals(singularType)) {
                        objects = well.getWellSamples().stream()
                                      .map(WellSampleWrapper::getImage)
                                      .collect(Collectors.toList());
                    } else if (TAG.equals(singularType)) {
                        objects = well.getTags(client);
                    } else {
                        IJ.error(String.format(ERROR_POSSIBLE_VALUES, INVALID, type, "images or tags."));
                    }
                    break;
                case IMAGE:
                    if (TAG.equals(singularType)) {
                        objects = client.getImage(id).getTags(client);
                    } else {
                        IJ.error("Invalid type: " + type + ". Only possible value is: tags.");
                    }
                    break;
                default:
                    String msg = String.format(ERROR_POSSIBLE_VALUES, INVALID, parent,
                                               "project, dataset, image, screen, plate, well or tag.");
                    IJ.error(msg);
            }
        } catch (ServiceException | AccessException | ExecutionException | OMEROServerError e) {
            IJ.error(String.format(ERROR_RETRIEVE_IN, type, parent, e.getMessage()));
        }
        return listToIDs(objects);
    }


    /**
     * Switches to another user.
     *
     * @param username The other user's name.
     */
    public void sudo(String username) {
        switched = client;
        try {
            client = switched.sudoGetUser(username);
        } catch (ServiceException | AccessException | ExecutionException | NoSuchElementException e) {
            IJ.error("Could not switch user: " + e.getMessage());
            switched = null;
        }
    }


    /**
     * Stops acting as another user.
     */
    public void endSudo() {
        if (switched != null) {
            client = switched;
            switched = null;
        } else {
            IJ.error("No sudo has been used before.");
        }
    }


    /**
     * Links two objects.
     *
     * @param type1 The first object type.
     * @param id1   The first object ID.
     * @param type2 The second object type.
     * @param id2   The second object ID.
     */
    public void link(String type1, long id1, String type2, long id2) {
        String t1 = singularType(type1);
        String t2 = singularType(type2);

        Map<String, Long> map = new HashMap<>(2);
        map.put(t1, id1);
        map.put(t2, id2);

        Long projectId = map.get(PROJECT);
        Long datasetId = map.get(DATASET);
        Long imageId   = map.get(IMAGE);
        Long tagId     = map.get(TAG);

        try {
            // Link tag to repository object
            if (t1.equals(TAG) ^ t2.equals(TAG)) {
                String obj = t1.equals(TAG) ? t2 : t1;

                GenericRepositoryObjectWrapper<?> object = getRepositoryObject(obj, map.get(obj));
                if (object != null) object.addTag(client, tagId);
            } else if (datasetId == null || (projectId == null && imageId == null)) {
                IJ.error(String.format("Cannot link %s and %s", type1, type2));
            } else { // Or link dataset to image or project
                DatasetWrapper dataset = client.getDataset(datasetId);
                if (projectId != null) {
                    client.getProject(projectId).addDataset(client, dataset);
                } else {
                    dataset.addImage(client, client.getImage(imageId));
                }
            }
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error(String.format("Cannot link %s and %s: %s", type1, type2, e.getMessage()));
        }
    }


    /**
     * Unlinks two objects.
     *
     * @param type1 The first object type.
     * @param id1   The first object ID.
     * @param type2 The second object type.
     * @param id2   The second object ID.
     */
    public void unlink(String type1, long id1, String type2, long id2) {
        String t1 = singularType(type1);
        String t2 = singularType(type2);

        Map<String, Long> map = new HashMap<>(2);
        map.put(t1, id1);
        map.put(t2, id2);

        Long projectId = map.get(PROJECT);
        Long datasetId = map.get(DATASET);
        Long imageId   = map.get(IMAGE);
        Long tagId     = map.get(TAG);

        try {
            // Unlink tag from repository object
            if (t1.equals(TAG) ^ t2.equals(TAG)) {
                String obj = t1.equals(TAG) ? t2 : t1;

                GenericRepositoryObjectWrapper<?> object = getRepositoryObject(obj, map.get(obj));
                if (object != null) object.unlink(client, client.getTag(tagId));
            } else if (datasetId == null || (projectId == null && imageId == null)) {
                IJ.error(String.format("Cannot unlink %s and %s", type1, type2));
            } else { // Or unlink dataset from image or project
                DatasetWrapper dataset = client.getDataset(datasetId);
                if (projectId != null) {
                    client.getProject(projectId).removeDataset(client, dataset);
                } else {
                    dataset.removeImage(client, client.getImage(imageId));
                }
            }
        } catch (ServiceException | AccessException | ExecutionException | OMEROServerError e) {
            IJ.error(String.format("Cannot unlink %s and %s: %s", type1, type2, e.getMessage()));
        } catch (InterruptedException e) {
            IJ.error(String.format("Cannot unlink %s and %s: %s", type1, type2, e.getMessage()));
            Thread.currentThread().interrupt();
        }
    }


    /**
     * Retrieves the name of an object.
     *
     * @param type The object type.
     * @param id   The object ID.
     *
     * @return The object name.
     */
    public String getName(String type, long id) {
        String name = null;

        GenericObjectWrapper<?> object = getObject(type, id);
        if (object instanceof GenericRepositoryObjectWrapper<?>) {
            name = ((GenericRepositoryObjectWrapper<?>) object).getName();
        } else if (object instanceof TagAnnotationWrapper) {
            name = ((TagAnnotationWrapper) object).getName();
        }
        return name;
    }


    /**
     * Opens an image.
     *
     * @param id The image ID.
     *
     * @return The image, as an {@link ImagePlus}.
     */
    public ImagePlus getImage(long id) {
        ImagePlus imp = null;
        try {
            ImageWrapper image = client.getImage(id);
            imp = image.toImagePlus(client);
        } catch (ServiceException | AccessException | ExecutionException | NoSuchElementException e) {
            IJ.error("Could not retrieve image: " + e.getMessage());
        }
        return imp;
    }


    /**
     * Retrieves the image ROIs and puts the in the ROI Manager, or the image overlay.
     *
     * @param imp       The image in ImageJ.
     * @param id        The image ID on OMERO.
     * @param toOverlay Whether to put ROIs on the overlay.
     * @param property  The ROI property to group shapes.
     *
     * @return The number of (2D) ROIs loaded in ImageJ.
     */
    public int getROIs(ImagePlus imp, long id, boolean toOverlay, String property) {
        List<ROIWrapper> rois = new ArrayList<>(0);
        try {
            ImageWrapper image = client.getImage(id);
            rois = image.getROIs(client);
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error("Could not retrieve ROIs: " + e.getMessage());
        }

        List<Roi> ijRois = ROIWrapper.toImageJ(rois, property);

        if (toOverlay) {
            Overlay overlay = imp.getOverlay();
            if (overlay == null) {
                overlay = new Overlay();
                imp.setOverlay(overlay);
            }
            for (Roi roi : ijRois) {
                roi.setImage(imp);
                overlay.add(roi);
            }
        } else {
            RoiManager rm = RoiManager.getInstance();
            if (rm == null) rm = RoiManager.getRoiManager();
            for (Roi roi : ijRois) {
                roi.setImage(imp);
                rm.addRoi(roi);
            }
        }
        return ijRois.size();
    }


    /**
     * Saves the ROIs from the ROI Manager and the image overlay to the image on OMERO.
     *
     * @param imp      The image in ImageJ.
     * @param id       The image ID on OMERO.
     * @param property The ROI property to group shapes.
     *
     * @return The number of (4D) ROIs saved on OMERO.
     */
    public int saveROIs(ImagePlus imp, long id, String property) {
        int result = 0;
        try {
            ImageWrapper image = client.getImage(id);

            Overlay overlay = imp.getOverlay();
            if (overlay != null) {
                List<Roi> ijRois = Arrays.asList(overlay.toArray());

                List<ROIWrapper> rois = ROIWrapper.fromImageJ(ijRois, property);
                rois.forEach(roi -> roi.setImage(image));
                image.saveROIs(client, rois);
                result += rois.size();
                overlay.clear();
                List<Roi> newRois = ROIWrapper.toImageJ(rois, property);
                for (Roi roi : newRois) {
                    roi.setImage(imp);
                    overlay.add(roi);
                }
            }

            RoiManager rm = RoiManager.getInstance();
            if (rm != null) {
                List<Roi> ijRois = Arrays.asList(rm.getRoisAsArray());

                List<ROIWrapper> rois = ROIWrapper.fromImageJ(ijRois, property);
                rois.forEach(roi -> roi.setImage(image));
                image.saveROIs(client, rois);
                result += rois.size();
                rm.reset();
                List<Roi> newRois = ROIWrapper.toImageJ(rois, property);
                for (Roi roi : newRois) {
                    roi.setImage(imp);
                    rm.addRoi(roi);
                }
            }
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error("Could not save ROIs to image: " + e.getMessage());
        }
        return result;
    }


    /**
     * Removes the ROIs from an image in OMERO.
     *
     * @param id The image ID on OMERO.
     *
     * @return The number of ROIs that were deleted.
     */
    public int removeROIs(long id) {

        int removed = 0;
        try {
            ImageWrapper     image = client.getImage(id);
            List<ROIWrapper> rois  = image.getROIs(client);
            client.delete(rois);
            removed = rois.size();
        } catch (ServiceException | AccessException | ExecutionException | OMEROServerError e) {
            IJ.error("Could not remove image ROIs: " + e.getMessage());
        } catch (InterruptedException e) {
            IJ.error("Could not remove image ROIs: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
        return removed;
    }


    /**
     * Disconnects from OMERO.
     */
    public void disconnect() {
        if (switched != null) endSudo();
        client.disconnect();
    }


    @Override
    public void run(String arg) {
        if (!IJ.macroRunning()) {
            IJ.showMessage("OMERO extensions for ImageJ",
                           String.format("The macro extensions are designed to be used within a macro.%n" +
                                         "Instructions on doing so will be printed to the Log window."));
            try (InputStream is = this.getClass().getResourceAsStream("/helper.md")) {
                if (is != null) {
                    ByteArrayOutputStream result = new ByteArrayOutputStream();
                    byte[]                buffer = new byte[2 ^ 10];
                    int                   length = is.read(buffer);
                    while (length != -1) {
                        result.write(buffer, 0, length);
                        length = is.read(buffer);
                    }
                    IJ.log(result.toString("UTF-8"));
                }
            } catch (IOException e) {
                IJ.error("Could not retrieve commands.");
            }
            return;
        }
        Functions.registerExtensions(this);
    }


    @Override
    public ExtensionDescriptor[] getExtensionFunctions() {
        return extensions;
    }


    @Override
    public String handleExtension(String name, Object[] args) {
        long   id;
        long   id1;
        long   id2;
        String type;
        String type1;
        String type2;
        String property;
        String tableName;
        String path;
        String results = null;
        switch (name) {
            case "connectToOMERO":
                String host = ((String) args[0]);
                int port = ((Double) args[1]).intValue();
                String username = ((String) args[2]);
                String password = ((String) args[3]);
                boolean connected = connect(host, port, username, password);
                results = String.valueOf(connected);
                break;

            case "switchGroup":
                long groupId = ((Double) args[0]).longValue();
                client.switchGroup(groupId);
                results = String.valueOf(client.getCurrentGroupId());
                break;

            case "listForUser":
                results = String.valueOf(setUser((String) args[0]));
                break;

            case "importImage":
                long datasetId = ((Double) args[0]).longValue();
                path = ((String) args[1]);
                results = importImage(datasetId, path);
                break;

            case "downloadImage":
                id = ((Double) args[0]).longValue();
                path = ((String) args[1]);
                results = downloadImage(id, path);
                break;

            case "addFile":
                type = (String) args[0];
                id = ((Double) args[1]).longValue();
                long fileId = addFile(type, id, (String) args[2]);
                results = String.valueOf(fileId);
                break;

            case "deleteFile":
                id = ((Double) args[0]).longValue();
                deleteFile(id);
                break;

            case "createDataset":
                Long projectId = doubleToLong((Double) args[2]);
                id = createDataset((String) args[0], (String) args[1], projectId);
                results = String.valueOf(id);
                break;

            case "createProject":
                id = createProject((String) args[0], (String) args[1]);
                results = String.valueOf(id);
                break;

            case "createTag":
                long tagId = createTag((String) args[0], (String) args[1]);
                results = String.valueOf(tagId);
                break;

            case "addToTable":
                tableName = (String) args[0];
                String resultsName = (String) args[1];
                Long imageId = doubleToLong((Double) args[2]);
                property = (String) args[3];

                ResultsTable rt = getTable(resultsName);
                RoiManager rm = RoiManager.getRoiManager();
                List<Roi> ijRois = Arrays.asList(rm.getRoisAsArray());

                addToTable(tableName, rt, imageId, ijRois, property);
                break;

            case "saveTableAsFile":
                tableName = (String) args[0];
                path = (String) args[1];
                CharSequence delimiter = (CharSequence) args[2];
                saveTableAsFile(tableName, path, delimiter);
                break;

            case "saveTable":
                tableName = (String) args[0];
                type = (String) args[1];
                id = ((Double) args[2]).longValue();
                saveTable(tableName, type, id);
                break;

            case "clearTable":
                tableName = (String) args[0];
                tables.remove(tableName);
                break;

            case "delete":
                type = (String) args[0];
                id = ((Double) args[1]).longValue();
                delete(type, id);
                break;

            case "list":
                type = (String) args[0];
                if (args[1] == null && args[2] == null) {
                    results = list(type);
                } else if (args[1] != null && args[2] == null) {
                    results = list(type, (String) args[1]);
                } else if (args[1] != null) {
                    String parentType = (String) args[1];
                    id = ((Double) args[2]).longValue();
                    results = list(type, parentType, id);
                } else {
                    IJ.error("Second argument should not be null.");
                }
                break;

            case "link":
                type1 = (String) args[0];
                id1 = ((Double) args[1]).longValue();
                type2 = (String) args[2];
                id2 = ((Double) args[3]).longValue();
                link(type1, id1, type2, id2);
                break;

            case "unlink":
                type1 = (String) args[0];
                id1 = ((Double) args[1]).longValue();
                type2 = (String) args[2];
                id2 = ((Double) args[3]).longValue();
                unlink(type1, id1, type2, id2);
                break;

            case "getName":
                type = (String) args[0];
                id = ((Double) args[1]).longValue();
                results = getName(type, id);
                break;

            case "getImage":
                ImagePlus imp = getImage(((Double) args[0]).longValue());
                if (imp != null) {
                    imp.show();
                    results = String.valueOf(imp.getID());
                }
                break;

            case "getROIs":
                id = ((Double) args[0]).longValue();
                Double ov = (Double) args[1];
                boolean toOverlay = ov != null && ov != 0;
                property = (String) args[2];
                int nIJRois = getROIs(IJ.getImage(), id, toOverlay, property);
                results = String.valueOf(nIJRois);
                break;

            case "saveROIs":
                id = ((Double) args[0]).longValue();
                property = (String) args[1];
                int nROIs = saveROIs(IJ.getImage(), id, property);
                results = String.valueOf(nROIs);
                break;

            case "removeROIs":
                id = ((Double) args[0]).longValue();
                int removed = removeROIs(id);
                results = String.valueOf(removed);
                break;

            case "sudo":
                sudo((String) args[0]);
                break;

            case "endSudo":
                endSudo();
                break;

            case "disconnect":
                disconnect();
                break;

            default:
                IJ.error("No such method: " + name);
        }

        return results;
    }

}
