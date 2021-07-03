package fr.igred.omero;


import fr.igred.omero.annotations.TableWrapper;
import fr.igred.omero.annotations.TagAnnotationWrapper;
import fr.igred.omero.exception.AccessException;
import fr.igred.omero.exception.OMEROServerError;
import fr.igred.omero.exception.ServiceException;
import fr.igred.omero.repository.DatasetWrapper;
import fr.igred.omero.repository.GenericRepositoryObjectWrapper;
import fr.igred.omero.repository.ImageWrapper;
import fr.igred.omero.repository.ProjectWrapper;
import fr.igred.omero.roi.ROIWrapper;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.macro.ExtensionDescriptor;
import ij.macro.Functions;
import ij.macro.MacroExtension;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static ij.macro.ExtensionDescriptor.newDescriptor;


public class OMEROExtension implements PlugIn, MacroExtension {

    private static final String PROJECT = "project";
    private static final String DATASET = "dataset";
    private static final String IMAGE   = "image";
    private static final String TAG     = "tag";
    private static final String INVALID = "Invalid type";

    private final ExtensionDescriptor[] extensions = {
            newDescriptor("connectToOMERO", this, ARG_STRING, ARG_NUMBER, ARG_STRING, ARG_STRING),
            newDescriptor("switchGroup", this, ARG_NUMBER),
            newDescriptor("list", this, ARG_STRING, ARG_STRING + ARG_OPTIONAL, ARG_NUMBER + ARG_OPTIONAL),
            newDescriptor("createDataset", this, ARG_STRING, ARG_STRING, ARG_NUMBER + ARG_OPTIONAL),
            newDescriptor("createProject", this, ARG_NUMBER, ARG_STRING, ARG_STRING),
            newDescriptor("createTag", this, ARG_STRING, ARG_STRING),
            newDescriptor("link", this, ARG_STRING, ARG_NUMBER, ARG_STRING, ARG_NUMBER),
            newDescriptor("addFile", this, ARG_STRING, ARG_NUMBER, ARG_STRING),
            newDescriptor("addToTable", this, ARG_STRING, ARG_STRING + ARG_OPTIONAL, ARG_NUMBER + ARG_OPTIONAL),
            newDescriptor("saveTable", this, ARG_STRING, ARG_STRING, ARG_NUMBER),
            newDescriptor("saveTableAsTXT", this, ARG_STRING, ARG_STRING),
            newDescriptor("clearTable", this, ARG_STRING),
            newDescriptor("importImage", this, ARG_NUMBER, ARG_STRING + ARG_OPTIONAL),
            newDescriptor("downloadImage", this, ARG_NUMBER, ARG_STRING),
            newDescriptor("delete", this, ARG_STRING, ARG_NUMBER),
            newDescriptor("getName", this, ARG_STRING, ARG_NUMBER),
            newDescriptor("getImage", this, ARG_NUMBER),
            newDescriptor("getROIs", this, ARG_NUMBER),
            newDescriptor("saveROIs", this, ARG_NUMBER, ARG_STRING),
            newDescriptor("sudo", this, ARG_STRING),
            newDescriptor("endSudo", this),
            newDescriptor("disconnect", this),
            };

    private final Map<String, TableWrapper> tables = new HashMap<>(1);

    private Client client = new Client();
    private Client switched;


    private static <T extends GenericObjectWrapper<?>> String listToIDs(List<T> list) {
        return list.stream()
                   .mapToLong(T::getId)
                   .mapToObj(String::valueOf)
                   .collect(Collectors.joining(","));
    }


    private static String singularType(String type) {
        String singular = type.toLowerCase();
        int    length   = singular.length();
        if (singular.charAt(length - 1) == 's') {
            singular = singular.substring(0, length - 1);
        }
        return singular;
    }


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
                default:
                    IJ.error(INVALID + ": " + type + ".");
            }
        } catch (ServiceException | AccessException e) {
            IJ.error("Could not retrieve object: " + e.getMessage());
        }
        return object;
    }


    public boolean connect(String host, int port, String username, String password) {
        boolean connected = false;
        try {
            client.connect(host, port, username, password.toCharArray());
            connected = true;
        } catch (ServiceException | ExecutionException e) {
            IJ.error("Could not connect: " + e.getMessage());
        }
        return connected;
    }


    public String downloadImage(long imageId, String path) {
        List<File> files = new ArrayList<>();
        try {
            files = client.getImage(imageId).download(client, path);
        } catch (ServiceException | AccessException | OMEROServerError e) {
            IJ.error("Could not download image: " + e.getMessage());
        }
        return files.stream().map(File::toString).collect(Collectors.joining(","));
    }


    public String importImage(long datasetId, String path) {
        if (path == null) {
            ImagePlus imp = IJ.getImage();
            path = IJ.getDir("temp") + imp.getTitle() + ".tif";
            IJ.save(imp, path);
        }
        List<Long> imageIds = new ArrayList<>();
        try {
            imageIds = client.getDataset(datasetId).importImage(client, path);
        } catch (Exception e) {
            IJ.error("Could not import image: " + e.getMessage());
        }
        try {
            Files.deleteIfExists(new File(path).toPath());
        } catch (IOException e) {
            IJ.error("Could not delete temp image: " + e.getMessage());
        }
        return imageIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }


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


    public void deleteFile(long fileId) {
        try {
            client.deleteFile(fileId);
        } catch (ServiceException | AccessException | ExecutionException | OMEROServerError e) {
            IJ.error("Could not delete file: " + e.getMessage());
        } catch (InterruptedException e) {
            IJ.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }


    public void addToTable(String tableName, String resultsName, Long imageId) {
        ResultsTable rt;
        if (tableName == null) rt = ResultsTable.getResultsTable();
        else rt = ResultsTable.getResultsTable(resultsName);

        RoiManager rm     = RoiManager.getRoiManager();
        List<Roi>  ijRois = Arrays.asList(rm.getRoisAsArray());

        TableWrapper table = tables.get(tableName);

        try {
            if (table == null) {
                table = new TableWrapper(client, rt, imageId, ijRois, ROIWrapper.IJ_PROPERTY);
                table.setName(tableName);
                tables.put(tableName, table);
            } else {
                table.addRows(client, rt, imageId, ijRois, ROIWrapper.IJ_PROPERTY);
            }
        } catch (ExecutionException | ServiceException | AccessException e) {
            IJ.error("Could not add results to table: " + e.getMessage());
        }
    }


    public void saveTableAsTXT(String tableName, String path) {
        TableWrapper  table    = tables.get(tableName);
        Object[][]    data     = table.getData();
        int           nColumns = table.getColumnCount();
        StringBuilder sb       = new StringBuilder();
        File          f        = new File(path);
        try (PrintWriter stream = new PrintWriter(f)) {
            for (int i = 0; i < nColumns; i++) {
                sb.append(table.getColumnName(i));
                if (i != (nColumns - 1)) {
                    sb.append("\t");
                }
            }
            sb.append("\n");
            for (int i = 0; i < table.getRowCount(); i++) {
                for (int j = 0; j < nColumns; j++) {
                    Object value = data[j][i];
                    sb.append(value);
                    if (i != table.getRowCount() - 1) {
                        sb.append("\t");
                    }
                }
                sb.append("\n");
            }
            stream.write(sb.toString());
        } catch (FileNotFoundException e) {
            IJ.error("Could not create table file: ", e.getMessage());
        }
    }


    public void saveTable(String name, String type, long id) {
        GenericRepositoryObjectWrapper<?> object = getRepositoryObject(type, id);
        if (object != null) {
            TableWrapper table = tables.get(name);
            if (table != null) {
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                String newName;
                if (name == null || name.equals("")) newName = timestamp + "_" + table.getName();
                else newName = timestamp + "_" + name;
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


    public String list(String type) {
        String singularType = singularType(type);

        String results = "";
        try {
            switch (singularType) {
                case PROJECT:
                    List<ProjectWrapper> projects = client.getProjects();
                    results = listToIDs(projects);
                    break;
                case DATASET:
                    List<DatasetWrapper> datasets = client.getDatasets();
                    results = listToIDs(datasets);
                    break;
                case IMAGE:
                    List<ImageWrapper> images = client.getImages();
                    results = listToIDs(images);
                    break;
                case TAG:
                    List<TagAnnotationWrapper> tags = client.getTags();
                    results = listToIDs(tags);
                    break;
                default:
                    IJ.error(INVALID + ": " + type + ". Possible values are: projects, datasets, images or tags.");
            }
        } catch (ServiceException | AccessException | OMEROServerError e) {
            IJ.error("Could not retrieve " + type + ": " + e.getMessage());
        }
        return results;
    }


    public String list(String type, String name) {
        String singularType = singularType(type);

        String results = "";
        try {
            switch (singularType) {
                case PROJECT:
                    List<ProjectWrapper> projects = client.getProjects(name);
                    results = listToIDs(projects);
                    break;
                case DATASET:
                    List<DatasetWrapper> datasets = client.getDatasets(name);
                    results = listToIDs(datasets);
                    break;
                case IMAGE:
                    List<ImageWrapper> images = client.getImages(name);
                    results = listToIDs(images);
                    break;
                case TAG:
                    List<TagAnnotationWrapper> tags = client.getTags(name);
                    results = listToIDs(tags);
                    break;
                default:
                    IJ.error(INVALID + ": " + type + ". Possible values are: projects, datasets, images or tags.");
            }
        } catch (ServiceException | AccessException | OMEROServerError e) {
            IJ.error("Could not retrieve project name: " + e.getMessage());
        }
        return results;
    }


    public String list(String type, String parent, long id) {
        String singularType   = singularType(type);
        String singularParent = singularType(parent);

        String results = "";
        try {
            switch (singularParent) {
                case PROJECT:
                    ProjectWrapper project = client.getProject(id);
                    switch (singularType) {
                        case DATASET:
                            List<DatasetWrapper> datasets = project.getDatasets();
                            results = listToIDs(datasets);
                            break;
                        case IMAGE:
                            List<ImageWrapper> images = project.getImages(client);
                            results = listToIDs(images);
                            break;
                        case TAG:
                            List<TagAnnotationWrapper> tags = project.getTags(client);
                            results = listToIDs(tags);
                            break;
                        default:
                            IJ.error(INVALID + ": " + type + ". Possible values are: datasets, images or tags.");
                    }
                    break;
                case DATASET:
                    DatasetWrapper dataset = client.getDataset(id);
                    switch (singularType) {
                        case IMAGE:
                            List<ImageWrapper> images = dataset.getImages(client);
                            results = listToIDs(images);
                            break;
                        case TAG:
                            List<TagAnnotationWrapper> tags = dataset.getTags(client);
                            results = listToIDs(tags);
                            break;
                        default:
                            IJ.error(INVALID + ": " + type + ". Possible values are: images or tags.");
                    }
                    break;
                case IMAGE:
                    if (singularType.equals(TAG)) {
                        results = listToIDs(client.getImage(id).getTags(client));
                    } else {
                        IJ.error("Invalid type: " + type + ". Only possible value is: tags.");
                    }
                    break;
                case TAG:
                    TagAnnotationWrapper tag = client.getTag(id);
                    switch (singularType) {
                        case PROJECT:
                            List<ProjectWrapper> projects = tag.getProjects(client);
                            results = listToIDs(projects);
                            break;
                        case DATASET:
                            List<DatasetWrapper> datasets = tag.getDatasets(client);
                            results = listToIDs(datasets);
                            break;
                        case IMAGE:
                            List<ImageWrapper> images = tag.getImages(client);
                            results = listToIDs(images);
                            break;
                        default:
                            IJ.error(INVALID + ": " + type + ". Possible values are: projects, datasets or images.");
                    }
                    break;
                default:
                    IJ.error(INVALID + ": " + parent + ". Possible values are: project, dataset, image or tag.");
            }
        } catch (ServiceException | AccessException | ExecutionException | OMEROServerError e) {
            IJ.error("Could not retrieve " + type + " in " + parent + ": " + e.getMessage());
        }
        return results;
    }


    public void sudo(String user) {
        switched = client;
        try {
            client = switched.sudoGetUser(user);
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error("Could not switch user: " + e.getMessage());
        }
    }


    public void endSudo() {
        if(switched != null) {
            client = switched;
            switched = null;
        } else {
            IJ.error("No sudo has been used before.");
        }
    }


    public void link(String type1, long id1, String type2, long id2) {
        String t1 = singularType(type1);
        String t2 = singularType(type2);

        Map<String, Long> map = new HashMap<>(2);
        map.put(t1, id1);
        map.put(t2, id2);

        Long datasetId = map.get(DATASET);
        Long projectId = map.get(PROJECT);
        Long imageId   = map.get(IMAGE);
        Long tagId     = map.get(TAG);

        try {
            // Link tag to repository object
            if (t1.equals(TAG) ^ t2.equals(TAG)) {
                String obj = t1.equals(TAG) ? t2 : t1;

                GenericRepositoryObjectWrapper<?> object = getRepositoryObject(obj, map.get(obj));
                if (object != null) object.addTag(client, tagId);
            } else if (datasetId == null || (projectId == null && imageId == null)) {
                IJ.error("Cannot link " + type1 + " and " + type2);
            } else { // Or link dataset to image or project
                DatasetWrapper dataset = client.getDataset(datasetId);
                if (projectId != null) {
                    client.getProject(projectId).addDataset(client, dataset);
                } else {
                    dataset.addImage(client, client.getImage(imageId));
                }
            }
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error("Could not link " + type2 + " and " + type1 + ": " + e.getMessage());
        }
    }


    public String getName(String type, long id) {
        String name = null;

        GenericObjectWrapper<?> object = getObject(type, id);
        if (object instanceof ProjectWrapper) {
            name = ((ProjectWrapper) object).getName();
        } else if (object instanceof DatasetWrapper) {
            name = ((DatasetWrapper) object).getName();
        } else if (object instanceof ImageWrapper) {
            name = ((ImageWrapper) object).getName();
        } else if (object instanceof TagAnnotationWrapper) {
            name = ((TagAnnotationWrapper) object).getName();
        }
        return name;
    }


    public ImagePlus getImage(long id) {
        ImagePlus imp = null;
        try {
            ImageWrapper image = client.getImage(id);
            imp = image.toImagePlus(client);
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error("Could not retrieve image: " + e.getMessage());
        }
        return imp;
    }


    public int getROIs(long id) {
        List<ROIWrapper> rois = new ArrayList<>();
        try {
            ImageWrapper image = client.getImage(id);
            rois = image.getROIs(client);
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error("Could not retrieve image with ROIs: " + e.getMessage());
        }

        ImagePlus imp = IJ.getImage();

        List<Roi> ijRois = ROIWrapper.toImageJ(rois);

        RoiManager rm = RoiManager.getInstance2();
        for (Roi roi : ijRois) {
            roi.setImage(imp);
            rm.addRoi(roi);
        }
        return ijRois.size();
    }


    public int saveROIs(long id, String property) {
        int result = 0;
        try {
            ImageWrapper image = client.getImage(id);

            RoiManager rm = RoiManager.getInstance2();

            List<Roi> ijRois = Arrays.asList(rm.getRoisAsArray());

            List<ROIWrapper> rois = ROIWrapper.fromImageJ(ijRois, property);
            rois.forEach(roi -> roi.setImage(image));
            for (ROIWrapper roi : rois) {
                image.saveROI(client, roi);
            }
            result = rois.size();
            rm.reset();
            this.getROIs(id);
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error("Could not save ROIs to image: " + e.getMessage());
        }
        return result;
    }


    public void disconnect() {
        if(switched != null) endSudo();
        client.disconnect();
    }


    @Override
    public void run(String arg) {
        if (!IJ.macroRunning()) {
            IJ.error("Cannot install extensions from outside a macro!");
            return;
        }
        Functions.registerExtensions(this);
    }


    public ExtensionDescriptor[] getExtensionFunctions() {
        return extensions;
    }


    public String handleExtension(String name, Object[] args) {
        long   id;
        String type;
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
                Long projectId = null;
                if (args[2] != null) projectId = ((Double) args[2]).longValue();
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
                Long imageId = null;
                if (args[2] != null) imageId = ((Double) args[2]).longValue();
                addToTable(tableName, resultsName, imageId);
                break;

            case "saveTableAsTXT":
                saveTableAsTXT((String) args[0], (String) args[1]);
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
                String type1 = (String) args[0];
                long id1 = ((Double) args[1]).longValue();
                String type2 = (String) args[2];
                long id2 = ((Double) args[3]).longValue();
                link(type1, id1, type2, id2);
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
                int nIJRois = getROIs(((Double) args[0]).longValue());
                results = String.valueOf(nIJRois);
                break;

            case "saveROIs":
                id = ((Double) args[0]).longValue();
                String property = ((String) args[1]);
                int nROIs = saveROIs(id, property);
                results = String.valueOf(nROIs);
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
