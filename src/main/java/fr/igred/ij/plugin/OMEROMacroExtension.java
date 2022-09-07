/*
 *  Copyright (C) 2021 GReD
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
import ij.macro.ExtensionDescriptor;
import ij.macro.Functions;
import ij.macro.MacroExtension;
import ij.plugin.PlugIn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static ij.macro.ExtensionDescriptor.newDescriptor;


public class OMEROMacroExtension extends OMEROMacroFunctions implements PlugIn, MacroExtension {

    private final ExtensionDescriptor[] extensions = buildExtensions();


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
        Number id;
        Number id1;
        Number id2;
        String type;
        String type1;
        String type2;
        String property;
        String tableName;
        String path;
        String results = null;
        switch (name) {
            case "connectToOMERO":
                String host = (String) args[0];
                Number port = (Number) args[1];
                String username = (String) args[2];
                String password = (String) args[3];
                String connected = connect(host, port, username, password);
                results = String.valueOf(connected);
                break;

            case "switchGroup":
                Number groupId = (Number) args[0];
                results = String.valueOf(switchGroup(groupId));
                break;

            case "listForUser":
                results = String.valueOf(setUser((String) args[0]));
                break;

            case "importImage":
                Number datasetId = (Number) args[0];
                path = (String) args[1];
                results = importImage(datasetId, path);
                break;

            case "downloadImage":
                id = (Number) args[0];
                path = (String) args[1];
                results = downloadImage(id, path);
                break;

            case "addFile":
                type = (String) args[0];
                id = (Number) args[1];
                Number fileId = addFile(type, id, (String) args[2]);
                results = String.valueOf(fileId);
                break;

            case "deleteFile":
                id = (Number) args[0];
                deleteFile(id);
                break;

            case "createDataset":
                Number projectId = (Number) args[2];
                id = createDataset((String) args[0], (String) args[1], projectId);
                results = String.valueOf(id);
                break;

            case "createProject":
                id = createProject((String) args[0], (String) args[1]);
                results = String.valueOf(id);
                break;

            case "createTag":
                Number tagId = createTag((String) args[0], (String) args[1]);
                results = String.valueOf(tagId);
                break;

            case "addToTable":
                tableName = (String) args[0];
                String resultsName = (String) args[1];
                Number imageId = (Number) args[2];
                property = (String) args[3];

                addToTable(tableName, resultsName, imageId, property);
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
                id = (Number) args[2];
                saveTable(tableName, type, id);
                break;

            case "clearTable":
                tableName = (String) args[0];
                clearTable(tableName);
                break;

            case "delete":
                type = (String) args[0];
                id = (Number) args[1];
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
                    id = ((Number) args[2]).longValue();
                    results = list(type, parentType, id);
                } else {
                    IJ.error("Second argument should not be null.");
                }
                break;

            case "link":
                type1 = (String) args[0];
                id1 = (Number) args[1];
                type2 = (String) args[2];
                id2 = (Number) args[3];
                link(type1, id1, type2, id2);
                break;

            case "unlink":
                type1 = (String) args[0];
                id1 = (Number) args[1];
                type2 = (String) args[2];
                id2 = (Number) args[3];
                unlink(type1, id1, type2, id2);
                break;

            case "getName":
                type = (String) args[0];
                id = (Number) args[1];
                results = getName(type, id);
                break;

            case "getImage":
                ImagePlus imp = getImage((Number) args[0]);
                if (imp != null) {
                    imp.show();
                    results = String.valueOf(imp.getID());
                }
                break;

            case "getROIs":
                id = (Number) args[0];
                Number toOverlay = (Number) args[1];
                property = (String) args[2];
                Number nIJRois = getROIs(IJ.getImage(), id, toOverlay, property);
                results = String.valueOf(nIJRois);
                break;

            case "saveROIs":
                id = (Number) args[0];
                property = (String) args[1];
                Number nROIs = saveROIs(IJ.getImage(), id, property);
                results = String.valueOf(nROIs);
                break;

            case "removeROIs":
                id = (Number) args[0];
                Number removed = removeROIs(id);
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


    private ExtensionDescriptor[] buildExtensions() {
        return new ExtensionDescriptor[]{
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
    }

}
