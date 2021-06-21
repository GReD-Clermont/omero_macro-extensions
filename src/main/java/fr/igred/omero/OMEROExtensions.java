package fr.igred.omero;


import fr.igred.omero.annotations.TagAnnotationWrapper;
import fr.igred.omero.exception.AccessException;
import fr.igred.omero.exception.OMEROServerError;
import fr.igred.omero.exception.ServiceException;
import fr.igred.omero.repository.DatasetWrapper;
import fr.igred.omero.repository.ImageWrapper;
import fr.igred.omero.repository.ProjectWrapper;
import fr.igred.omero.roi.ROIWrapper;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.macro.ExtensionDescriptor;
import ij.macro.Functions;
import ij.macro.MacroExtension;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


public class OMEROExtensions implements PlugIn, MacroExtension {

    private static final Client client = new Client();


    private static final String PROJECT  = "project";
    private static final String PROJECTS = PROJECT + "s";
    private static final String DATASET  = "dataset";
    private static final String DATASETS = DATASET + "s";
    private static final String IMAGE    = "image";
    private static final String IMAGES   = IMAGE + "s";
    private static final String TAG      = "tag";
    private static final String TAGS     = TAG + "s";

    private static final String INVALID = "Invalid type";

    private final ExtensionDescriptor[] extensions = {
            ExtensionDescriptor.newDescriptor("connectToOMERO", this, ARG_STRING, ARG_NUMBER, ARG_STRING, ARG_STRING),
            ExtensionDescriptor.newDescriptor("switchGroup", this, ARG_NUMBER),
            ExtensionDescriptor.newDescriptor("list", this, ARG_STRING, ARG_STRING + ARG_OPTIONAL, ARG_NUMBER + ARG_OPTIONAL, ARG_OUTPUT + ARG_STRING),
            ExtensionDescriptor.newDescriptor("getName", this, ARG_STRING, ARG_NUMBER, ARG_OUTPUT + ARG_STRING),
            ExtensionDescriptor.newDescriptor("getImage", this, ARG_NUMBER),
            ExtensionDescriptor.newDescriptor("getImageWithROIs", this, ARG_NUMBER),
            ExtensionDescriptor.newDescriptor("saveROIsToImage", this, ARG_NUMBER, ARG_STRING),
            ExtensionDescriptor.newDescriptor("disconnect", this),
            };


    private static <T extends GenericObjectWrapper<?>> String listToIDs(List<T> list) {
        return list.stream()
                   .mapToLong(T::getId)
                   .mapToObj(String::valueOf)
                   .collect(Collectors.joining(","));
    }


    private static void connectToOMERO(String host, int port, String username, String password) {
        try {
            client.connect(host, port, username, password);
        } catch (ServiceException | ExecutionException e) {
            IJ.error("Could not connect: " + e.getMessage());
        }
    }


    private static String list(String type) {
        String results = "";
        try {
            switch (type.toLowerCase()) {
                case PROJECT:
                case PROJECTS:
                    List<ProjectWrapper> projects = client.getProjects();
                    results = listToIDs(projects);
                    break;
                case DATASET:
                case DATASETS:
                    List<DatasetWrapper> datasets = client.getDatasets();
                    results = listToIDs(datasets);
                    break;
                case IMAGE:
                case IMAGES:
                    List<ImageWrapper> images = client.getImages();
                    results = listToIDs(images);
                    break;
                case TAG:
                case TAGS:
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


    private static String list(String type, String name) {
        String results = "";
        try {
            switch (type.toLowerCase()) {
                case PROJECTS:
                case PROJECT:
                    List<ProjectWrapper> projects = client.getProjects(name);
                    results = listToIDs(projects);
                    break;
                case DATASET:
                case DATASETS:
                    List<DatasetWrapper> datasets = client.getDatasets(name);
                    results = listToIDs(datasets);
                    break;
                case IMAGE:
                case IMAGES:
                    List<ImageWrapper> images = client.getImages(name);
                    results = listToIDs(images);
                    break;
                case TAG:
                case TAGS:
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


    private static String list(String type, String parent, long id) {
        String results = "";
        try {
            switch (parent.toLowerCase()) {
                case PROJECTS:
                case PROJECT:
                    ProjectWrapper project = client.getProject(id);
                    switch (type.toLowerCase()) {
                        case DATASET:
                        case DATASETS:
                            List<DatasetWrapper> datasets = project.getDatasets();
                            results = listToIDs(datasets);
                            break;
                        case IMAGE:
                        case IMAGES:
                            List<ImageWrapper> images = project.getImages(client);
                            results = listToIDs(images);
                            break;
                        case TAG:
                        case TAGS:
                            List<TagAnnotationWrapper> tags = project.getTags(client);
                            results = listToIDs(tags);
                            break;
                        default:
                            IJ.error(INVALID + ": " + type + ". Possible values are: datasets, images or tags.");
                    }
                    break;
                case DATASETS:
                case DATASET:
                    DatasetWrapper dataset = client.getDataset(id);
                    switch (type.toLowerCase()) {
                        case IMAGE:
                        case IMAGES:
                            List<ImageWrapper> images = dataset.getImages(client);
                            results = listToIDs(images);
                            break;
                        case TAG:
                        case TAGS:
                            List<TagAnnotationWrapper> tags = dataset.getTags(client);
                            results = listToIDs(tags);
                            break;
                        default:
                            IJ.error(INVALID + ": " + type + ". Possible values are: images or tags.");
                    }
                    break;
                case IMAGES:
                case IMAGE:
                    if (type.equals(TAGS)) results = listToIDs(client.getImage(id).getTags(client));
                    else IJ.error("Invalid type: " + type + ". Only possible value is: tags.");
                    break;
                default:
                    IJ.error(INVALID + ": " + parent + ". Possible values are: project, dataset or image.");
            }
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error("Could not retrieve " + type + " in " + parent + ": " + e.getMessage());
        }
        return results;
    }


    private static String getName(String type, long id) {
        String name = "";
        try {
            switch (type.toLowerCase()) {
                case PROJECTS:
                case PROJECT:
                    ProjectWrapper project = client.getProject(id);
                    name = project.getName();
                    break;
                case DATASETS:
                case DATASET:
                    DatasetWrapper dataset = client.getDataset(id);
                    name = dataset.getName();
                    break;
                case IMAGES:
                case IMAGE:
                    ImageWrapper image = client.getImage(id);
                    name = image.getName();
                    break;
                case TAGS:
                case TAG:
                    TagAnnotationWrapper tag = client.getTag(id);
                    name = tag.getName();
                    break;
                default:
                    IJ.error(INVALID + ": " + type + ". Possible values are: project, dataset, image or tag.");
            }
        } catch (ServiceException | AccessException | OMEROServerError e) {
            IJ.error("Could not retrieve project name: " + e.getMessage());
        }
        return name;
    }


    private static void getImage(long id) {
        try {
            ImageWrapper image = client.getImage(id);
            ImagePlus    imp   = image.toImagePlus(client);
            imp.show();
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error("Could not retrieve image: " + e.getMessage());
        }
    }


    private static void getImageWithROIs(long id) {
        try {
            ImageWrapper image = client.getImage(id);

            ImagePlus imp = image.toImagePlus(client);
            imp.show();

            List<ROIWrapper> rois = image.getROIs(client);

            List<Roi> ijRois = ROIWrapper.toImageJ(rois);

            int index = 0;
            for (ROIWrapper roi : rois) {
                List<Roi> shapes = roi.toImageJ();
                for (Roi r : shapes) {
                    r.setProperty("INDEX", String.valueOf(index));
                    if (rois.size() < 255) {
                        r.setGroup(index);
                    }
                }
                ijRois.addAll(shapes);
                index++;
            }
            RoiManager rm = RoiManager.getRoiManager();
            for (Roi roi : ijRois) {
                roi.setImage(imp);
                rm.addRoi(roi);
            }
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error("Could not retrieve image with ROIs: " + e.getMessage());
        }
    }


    private static void saveROIsToImage(long id, String property) {
        try {
            ImageWrapper image = client.getImage(id);

            RoiManager rm = RoiManager.getRoiManager();

            List<Roi> ijRois = Arrays.asList(rm.getRoisAsArray());

            List<ROIWrapper> rois = ROIWrapper.fromImageJ(ijRois, property);
            rois.forEach(roi -> roi.setImage(image));
            for (ROIWrapper roi : rois) {
                image.saveROI(client, roi);
            }
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.error("Could not save ROIs to image: " + e.getMessage());
        }
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
        String type;
        long   id;
        switch (name) {
            case "connectToOMERO":
                String host = ((String) args[0]);
                int port = ((Double) args[1]).intValue();
                String username = ((String) args[2]);
                String password = ((String) args[3]);
                connectToOMERO(host, port, username, password);
                break;

            case "switchGroup":
                long groupId = ((Double) args[0]).longValue();
                client.switchGroup(groupId);
                break;

            case "list":
                type = (String) args[0];
                switch(args.length) {
                    case 2:
                        ((String[]) args[1])[0] = list(type);
                        break;
                    case 3:
                        ((String[]) args[2])[0] = list(type, (String) args[1]);
                        break;
                    case 4:
                        String parentType = (String) args[1];
                        id = ((Double) args[2]).longValue();
                        ((String[]) args[3])[0] = list(type, parentType, id);
                        break;
                    default:
                        IJ.error("Wrong number of parameters.");
                }
                break;

            case "getName":
                type = (String) args[0];
                id = ((Double) args[1]).longValue();
                ((String[]) args[2])[0] = getName(type, id);
                break;

            case "getImage":
                getImage(((Double) args[0]).longValue());
                break;

            case "getImageWithROIs":
                getImageWithROIs(((Double) args[0]).longValue());
                break;

            case "saveROIsToImage":
                long imageId = ((Double) args[0]).longValue();
                String property = ((String) args[1]);
                saveROIsToImage(imageId, property);
                break;

            case "disconnect":
                client.disconnect();
                break;

            default:
                IJ.error("No such method: " + name);
        }

        return null;
    }

}
