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

### Listing repository objects (projects, datasets, images, tags)

Once connected, projects, datasets, images and tags IDs can be listed, and their name retrieved:

* Projects:

```
Ext.list("projects", projects);
print(projects);
projectIds=split(projects,",");
Ext.getName("project", projectIds[0], projectName);
print(projectName);
```

* Datasets:

```
Ext.list("datasets", datasets);
print(datasets);
datasetIds=split(datasets,",");
Ext.getName("dataset", datasetIds[0], datasetName);
print(datasetName);
```

* Images:

```
Ext.list("images", images);
print(images);
imageIds=split(images,",");
Ext.getName("image", imageIds[0], imageName);
print(imageName);
```

* Tags:

```
Ext.list("tags", tags);
print(tags);
projectIds=split(tags,",");
Ext.getName("tag", tagIds[0], tagName);
print(tagName);
```

### Listing objects with a given name

It is also possible to list objects with a specific name:

* Projects:

```
Ext.list("projects", "name", projects);
```

* Datasets:

```
Ext.list("datasets", "name", projects);
```

* Images:

```
Ext.list("images", "name", projects);
```

* Tags:

```
Ext.list("tags", "name", projects);
```

### Listing objects inside a given repository object

Each object ID can then be used to retrieve the contained objects (IDs and names):

It is, for example, possible to list datasets inside a project:

```
Ext.list("datasets", "project", projectIds[0], datasets);
print(datasets);
datasetIds=split(datasets,",");
Ext.getName("dataset", datasetIds[0], datasetName);
print(datasetName);
```

It is also possible to list tags attached to an image:

```
Ext.list("tags", "image", imageIds[0], tags);
print(tags);
datasetIds=split(tags,",");
Ext.getName("tag", tagIds[0], tagName);
print(tagName);
```

Similarly, a dataset ID can be used to retrieve its images:

```
Ext.list("images", "dataset", datasetIds[0], images);
print(images);
imageIds=split(images,",");
Ext.getImageName(imageIds[0], imageName);
print(imageName);
```

### Images

Pixel intensities can be retrieved from images:

``````
Ext.getImage(imageIds[0]);
``````

ROIs from OMERO can also be added to the ROI manager when opening an image:

``````
Ext.getImageWithROIs(imageIds[0]);
``````

Conversely, ImageJ ROIs can also be saved to OMERO (the property is used to group ImageJ shapes into a single 3D/4D ROI
on OMERO):

```
saveROIsToImage(imageId, property);
```

## License

[GPLv2+](https://choosealicense.com/licenses/gpl-2.0/)