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

### Repository objects (projects, datasets, images)

Once connected, projects IDs can be listed, and their name retrieved:

```
Ext.listProjects(projects);
print(projects);
projectIds=split(projects,",");
Ext.getProjectName(projectIds[0], projectName);
print(projectName);
```

Each project ID can then be used to retrieve the contained datasets (IDs and names):

```
Ext.listDatasetsInProject(projectIds[0], datasets);
print(datasets);
datasetIds=split(datasets,",");
Ext.getDatasetName(datasetIds[0], datasetName);
print(datasetName);
```

Similarly, each dataset ID can be used to retrieve its images (IDs, names, pixels and ROIs):

```
Ext.listImagesInDataset(datasetIds[0], images);
print(images);
imageIds=split(images,",");
Ext.getImageName(imageIds[0], imageName);
print(imageName);
Ext.getImage(imageIds[0]);
Ext.getImageWithROIs(imageIds[0]);
```

ImageJ ROIs can also be saved to OMERO (the property is used to group ImageJ shapes into a single 3D/4D ROI on OMERO):
```
Ext.saveROIsToImage(imageId, property);
```

## License
[GPLv2+](https://choosealicense.com/licenses/gpl-2.0/)
