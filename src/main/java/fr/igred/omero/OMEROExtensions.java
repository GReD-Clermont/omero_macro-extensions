package fr.igred.omero;


import fr.igred.omero.exception.AccessException;
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


public class OMEROExtensions implements PlugIn, MacroExtension {

    private static final Client client = new Client();

    private final ExtensionDescriptor[] extensions = {
            ExtensionDescriptor.newDescriptor("connectToOMERO", this, ARG_STRING, ARG_NUMBER, ARG_STRING, ARG_STRING),
            ExtensionDescriptor.newDescriptor("switchGroup", this, ARG_NUMBER),
            ExtensionDescriptor.newDescriptor("listProjects", this, ARG_OUTPUT + ARG_STRING),
            ExtensionDescriptor.newDescriptor("getProjectName", this, ARG_NUMBER, ARG_OUTPUT + ARG_STRING),
            ExtensionDescriptor.newDescriptor("listDatasetsInProject", this, ARG_NUMBER, ARG_OUTPUT + ARG_STRING),
            ExtensionDescriptor.newDescriptor("getDatasetName", this, ARG_NUMBER, ARG_OUTPUT + ARG_STRING),
            ExtensionDescriptor.newDescriptor("listImagesInDataset", this, ARG_NUMBER, ARG_OUTPUT + ARG_STRING),
            ExtensionDescriptor.newDescriptor("getImageName", this, ARG_NUMBER, ARG_OUTPUT + ARG_STRING),
            ExtensionDescriptor.newDescriptor("getImage", this, ARG_NUMBER),
            ExtensionDescriptor.newDescriptor("getImageWithROIs", this, ARG_NUMBER),
            ExtensionDescriptor.newDescriptor("saveROIsToImage", this, ARG_NUMBER, ARG_STRING),
            ExtensionDescriptor.newDescriptor("disconnect", this),
            };


    private static void connectToOMERO(String host, int port, String username, String password) {
        try {
            client.connect(host, port, username, password);
        } catch (ServiceException | ExecutionException e) {
            IJ.log("Could not connect: " + e.getMessage());
        }
    }


    private static String listProjects() {
        StringBuilder ids = new StringBuilder();
        try {
            List<ProjectWrapper> projects = client.getProjects();
            for (ProjectWrapper project : projects) {
                if (!ids.toString().equals("")) {
                    ids.append(",");
                }
                ids.append(project.getId());
            }
        } catch (ServiceException | AccessException e) {
            IJ.log("Could not retrieve projects: " + e.getMessage());
        }
        return ids.toString();
    }


    private static String getProjectName(long id) {
        String name = "";
        try {
            ProjectWrapper project = client.getProject(id);
            name = project.getName();
        } catch (ServiceException | AccessException e) {
            IJ.log("Could not retrieve project name: " + e.getMessage());
        }
        return name;
    }


    private static String listDatasetsInProject(long id) {
        String list = "";
        try {
            ProjectWrapper       project  = client.getProject(id);
            List<DatasetWrapper> datasets = project.getDatasets();
            StringBuilder        ids      = new StringBuilder();
            for (DatasetWrapper dataset : datasets) {
                ids.append(dataset.getId()).append(",");
            }
            list = ids.substring(0, ids.length() - 1);
        } catch (ServiceException | AccessException e) {
            IJ.log("Could not retrieve datasets: " + e.getMessage());
        }
        return list;
    }


    private static String getDatasetName(long id) {
        String name = "";
        try {
            DatasetWrapper dataset = client.getDataset(id);
            name = dataset.getName();
        } catch (ServiceException | AccessException e) {
            IJ.log("Could not retrieve dataset name: " + e.getMessage());
        }
        return name;
    }


    private static String listImagesInDataset(long id) {
        String list = "";
        try {
            DatasetWrapper     dataset = client.getDataset(id);
            List<ImageWrapper> images  = dataset.getImages(client);
            StringBuilder      ids     = new StringBuilder();
            for (ImageWrapper image : images) {
                ids.append(image.getId()).append(",");
            }
            list = ids.substring(0, ids.length() - 1);
        } catch (ServiceException | AccessException e) {
            IJ.log("Could not retrieve images: " + e.getMessage());
        }
        return list;
    }


    private static String getImageName(long id) {
        String name = "";
        try {
            ImageWrapper image = client.getImage(id);
            name = image.getName();
        } catch (ServiceException | AccessException e) {
            IJ.log("Could not retrieve image name: " + e.getMessage());
        }
        return name;
    }


    private static void getImage(long id) {
        try {
            ImageWrapper image = client.getImage(id);
            ImagePlus    imp   = image.toImagePlus(client);
            imp.show();
        } catch (ServiceException | AccessException | ExecutionException e) {
            IJ.log("Could not retrieve image: " + e.getMessage());
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
            IJ.log("Could not retrieve image with ROIs: " + e.getMessage());
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
            IJ.log("Could not save ROIs to image: " + e.getMessage());
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
        long id;
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

            case "listProjects":
                ((String[]) args[0])[0] = listProjects();
                break;

            case "getProjectName":
                id = ((Double) args[0]).longValue();
                ((String[]) args[1])[0] = getProjectName(id);
                break;

            case "listDatasetsInProject":
                id = ((Double) args[0]).longValue();
                ((String[]) args[1])[0] = listDatasetsInProject(id);
                break;

            case "getDatasetName":
                id = ((Double) args[0]).longValue();
                ((String[]) args[1])[0] = getDatasetName(id);
                break;

            case "listImagesInDataset":
                id = ((Double) args[0]).longValue();
                ((String[]) args[1])[0] = listImagesInDataset(id);
                break;

            case "getImageName":
                id = ((Double) args[0]).longValue();
                ((String[]) args[1])[0] = getImageName(id);
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
                IJ.log("No such method: " + name);
        }

        return null;
    }

}
