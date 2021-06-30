[![Java CI with Maven](https://github.com/GReD-Clermont/imagej_omero_extensions/actions/workflows/maven.yml/badge.svg)](https://github.com/GReD-Clermont/imagej_omero_extensions/actions/workflows/maven.yml)

# omero_extensions

A plugin for ImageJ to provide macro extensions to access OMERO.

## How to use:

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
datasetId = Ext.createDataset(projectId, name, description);
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

ROIs from OMERO can also be added to the ROI manager (and the current image):

```
nIJROIs = Ext.getROIs(imageIds[0]);
```

Conversely, ImageJ ROIs can also be saved to OMERO (the property is used to group ImageJ shapes into a single 3D/4D ROI
on OMERO):

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
Ext.addToTable(tableName, imageId, resultsName);
```
If a column named ROI containing ROI IDs is present, these will be added to the table.
Alternatively, if ROIs that were saved to OMERO are in the ROI Manager 
and if their name appears in the labels or is in a column named ROI, they will be added too.

The table can then be saved to a project/dataset/image through *Ext.saveTable* (this will clear the current table):
```
Ext.saveTable('image', imageId, tableName);

```

## License

[GPLv2+](https://choosealicense.com/licenses/gpl-2.0/)