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


import fr.igred.ij.macro.OMEROMacroFunctions;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.macro.ExtensionDescriptor;
import ij.macro.Functions;
import ij.macro.MacroExtension;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static ij.macro.ExtensionDescriptor.newDescriptor;


public class OMEROMacroExtension extends OMEROMacroFunctions implements PlugIn, MacroExtension {

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
                results = String.valueOf(switchGroup(groupId));
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
                clearTable(tableName);
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
