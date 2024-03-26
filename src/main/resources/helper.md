To gain access to more advanced features of OMERO
from within a macro, put the following line at the
beginning of your macro:

run("OMERO Extensions");

This will enable the following macro functions:

### Handles connection (should be used first/last) ###

Ext.connectToOMERO(host, port, username, password)
> Connects to the given OMERO `host` (and `port`)
> using the provided `username` and `password`.

Ext.disconnect()
> Disconnects from the server.

### Changes the context for other commands ###

Ext.switchGroup(groupId)
> Switches to the given group on OMERO.

Ext.listForUser(username)
> Filters listings to only include data from the given user.

Ext.sudo(username)
> Runs subsequent commands as the specified user,
> if the logged-in user has sudo rights.

Ext.endSudo()
> Switches back to the logged-in user.

### Retrieves data from OMERO ###

Ext.list(type)
> Gets the IDs for objects of the given `type`,  
> separated by commas.

Ext.list(type, name);
> Gets the IDs for objects with the given `type` and `name`,  
> separated by commas.

Ext.list(type, parentType, parentId)
> Gets the IDs for objects of the given `type`
> inside a parent container with type `parentType` and ID `parentId`,  
> separated by commas.

Ext.getName(type, id)
> Gets the name of the specified object, given its `type` and `id`.

Ext.getImage(id)
> Opens the image with the given `id`.  
> Returns the image ID in ImageJ.

Ext.getROIs(imageId, toOverlay, property)
> Retrieves the ROIs for the image with the given `imageId`.
> These are added to the ROI manager by default.
>
> If `toOverlay` (optional) is true,
> they are added to the overlay instead.
>
> Moreover, shapes from 3D/4D ROIs will share the same values
> in the "ROI" and "ROI_ID" properties.  
> The properties names can be optionally changed:
> `property` will store local indices,
> while `property + "_ID"` will store OMERO IDs.
>
> Returns the number of ROIs in ImageJ.

### Saves data ###

Ext.createTag(name, description)
> Creates a new tag with the given `name` and `description`.  
> Returns the new tag ID.

Ext.createProject(name, description)
> Creates a new project with the given `name` and `description`.  
> Returns the new project ID.

Ext.createDataset(name, description, projectId)
> Creates a new dataset with the given `name` and `description`,
> inside the project with the specified `projectId`.  
> Returns the new dataset ID.

Ext.importImage(datasetId)
> Saves the current image to the dataset with the given `datasetId`.  
> Returns the new image ID.

Ext.saveROIs(imageId, property)
> Saves ImageJ ROIs to the image with the given `imageId` on OMERO.  
> The (optional) `property` is used to group ImageJ shapes
> into a single 3D/4D ROI in OMERO.  
> The default value for this (if empty or absent) is "ROI".
>
> Returns the number of 3D/4D ROIs saved to OMERO.

Ext.addFile(type, id, path)
> Attach a file on the given `path`
> to the object with the given `type` and `id` on OMERO.  
> Returns the new file ID.

Ext.link(type1, id1, type2, id2)
> Links two objects together, using their types and IDs.
> The order does not matter.
> Possible types are:
>   * Project and Dataset
>   * Dataset and Image
>   * Tag and Project, Dataset or Image

### Removes data on OMERO ###

Ext.unlink(type1, id1, type2, id2)
> Unlinks two objects, using their types and IDs (see `link`).

Ext.delete(type, id)
> Deletes the object with the given `type` and `id` from OMERO.

Ext.deleteFile(id)
> Delete the attached file with the given `id` on OMERO.

### Table functions ###

Ext.addToTable(tableName, resultsName, imageId, roiProperty)
> Creates or updates a local table using ImageJ results, where:
>   * `tableName` is the name of the table
>   * `resultsName` (optional) is the IJ results name
>   * `imageId` (optional) is the image ID on OMERO
>   * `roiProperty` (optional) is the ROI property used for grouping 2D shapes in 3D/4D ROIs.
>
> If ROIs that were saved to OMERO are in the ROI Manager
> and if their name appears in the labels
> or is in a column named "ROI", they will be added too.

Ext.saveTable(tableName, type, id)
> Saves the table with the name `tableName` to OMERO,
> and attaches it to the object with the given `type` and `id`.

Ext.saveTableAsFile(tableName, path, delimiter)
> Saves the table to a delimited text file locally (`path`).  
> The default separator is ',' but can be changed with
> the optional `delimiter`.
