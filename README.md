[![Java CI with Maven](https://github.com/GReD-Clermont/omero_macro-extensions/actions/workflows/maven.yml/badge.svg)](https://github.com/GReD-Clermont/omero_macro-extensions/actions/workflows/maven.yml)

# OMERO Macro Extensions

A plugin for ImageJ to provide macro extensions to access OMERO.

## How to install

1. Install
   the [OMERO.insight plugin for Fiji](https://omero-guides.readthedocs.io/en/latest/fiji/docs/installation.html) (if
   you haven't already).
2. Download the JAR file for this [library](https://github.com/GReD-Clermont/simple-omero-client/releases/latest/).
3. Download the JAR file ([for this plugin](https://github.com/GReD-Clermont/omero_macro-extensions/releases/latest/)).
4. Place these JAR files in your plugins folder.

## How to use

Extensions first have to be loaded:

```
run("OMERO Extensions");
```

### Connection:

Connecting to OMERO is done using:

```
Ext.connectToOMERO("host", 4064, "username", "password");
```

Then, switching group can be performed through:

```
Ext.switchGroup(groupId);
```

When done, you can disconnect using:

```
Ext.disconnect()
```

### Listing repository objects (projects, datasets, images, tags)

Once connected, projects, datasets, images and tags IDs can be listed, and their name retrieved:

* Projects:

```
projects = Ext.list("projects");
print(projects);
projectIds = split(projects,",");
projectName = Ext.getName("project", projectIds[0]);
print(projectName);
```

* Datasets:

```
datasets = Ext.list("datasets");
print(datasets);
datasetIds = split(datasets,",");
datasetName = Ext.getName("dataset", datasetIds[0]);
print(datasetName);
```

* Images:

```
images = Ext.list("images");
print(images);
imageIds = split(images,",");
imageName = Ext.getName("image", imageIds[0]);
print(imageName);
```

* Tags:

```
tags = Ext.list("tags");
print(tags);
projectIds = split(tags,",");
tagName = Ext.getName("tag", tagIds[0]);
print(tagName);
```

### Listing objects with a given name

It is also possible to list objects with a specific name:

* Projects:

```
projects = Ext.list("projects", "name");
```

* Datasets:

```
datasets = Ext.list("datasets", "name");
```

* Images:

```
images = Ext.list("images", "name");
```

* Tags:

```
tags = Ext.list("tags", "name");
```

### Listing objects inside a given repository object

Each object ID can then be used to retrieve the contained objects (IDs and names):

It is, for example, possible to list datasets inside a project:

```
datasets = Ext.list("datasets", "project", projectIds[0]);
print(datasets);
datasetIds = split(datasets,",");
datasetName = Ext.getName("dataset", datasetIds[0]);
print(datasetName);
```

It is also possible to list tags attached to an image:

```
tags = Ext.list("tags", "image", imageIds[0]);
print(tags);
tagIds = split(tags,",");
tagName = Ext.getName("tag", tagIds[0]);
print(tagName);
```

Similarly, a dataset ID can be used to retrieve its images:

```
images = Ext.list("images", "dataset", datasetIds[0]);
print(images);
imageIds = split(images,",");
imageName = Ext.getName("image", imageIds[0]);
print(imageName);
```

### Creating projects, datasets and tags

Projects can be created with *Ext.createProject*:

```
projectId = Ext.createProject(name, description);
```

Datasets can be created with *Ext.createDataset*:

```
datasetId = Ext.createDataset(name, description, projectId);
```

Tags can be created with *Ext.createTag*:

```
tagId = Ext.createTag(name, description);
```

### Linking objects

Objects can be linked with *Ext.link*, e.g.:

```
Ext.link("dataset", datasetId, "tag", tagId);
```

### Deleting objects

Objects can be deleted with *Ext.delete*:

```
Ext.delete("project", projectId);
```

### Opening images

Pixel intensities can be retrieved from images:

```
imageplusID = Ext.getImage(imageIds[0]);
```

ROIs from OMERO can also be added to the ROI manager (and the current image). ROIs composed of multiple shapes (eg
3D/4D) will share the same values in the "ROI" and "ROI_ID" properties in ImageJ. These can be optionnally changed with
the "property" parameter: local indices will be in "property" while OMERO IDs will be in "property + _ID". 
This is achieved through:

```
nIJROIs = Ext.getROIs(imageIds[0], property);
```

Conversely, ImageJ ROIs can also be saved to OMERO (the property is used to group ImageJ shapes into a single 3D/4D ROI
on OMERO, if the string is empty, "ROI" is used):

```
nROIS = Ext.saveROIs(imageId, property);
```

### Saving images

The current image can be saved (as a TIF) to a dataset in OMERO:

```
newImageId = Ext.importImage(datasetId);
```

### Attaching / deleting files

Files can be attached to a project/dataset/image through *Ext.addFile*:

```
fileId = Ext.addFile('image', imageId, path);
```

They can also be deleted with *Ext.deleteFile*:

```
Ext.deleteFile(fileId);
```

### Attaching a table

A table can be created/updated using the results with *Ext.addToTable*:

```
Ext.addToTable(tableName, resultsName, imageId, roiProperty);
```

If a column named ROI containing ROI IDs is present, these will be added to the table. Alternatively, if ROIs that were
saved to OMERO are in the ROI Manager and if their name appears in the labels or is in a column named ROI, they will be
added too.

The table can then be saved to a project/dataset/image through *Ext.saveTable*:

```
Ext.saveTable(tableName, 'dataset', datasetId);
```

It can then be saved to a tab-separated text file through *Ext.saveTableAsTXT*:

```
Ext.saveTableAsTXT(tableName, pathToTXT);
```

### Work as another user (sudo)

If a user has sudo rights, it is possible to do all the above as another user:

```
Ext.sudo(otherUsername);
```

To switch back to the original user, this command should be used:

```
Ext.endSudo();
```

## License

[GPLv2+](https://choosealicense.com/licenses/gpl-2.0/)
