// @String(label="Username") USERNAME
// @String(label="Password", style='password') PASSWORD
// @String(label="Host", value='wss://workshop.openmicroscopy.org/omero-ws') HOST
// @Integer(label="Port", value=4064) PORT

run("OMERO Extensions");

// Connect to OMERO
connected = Ext.connectToOMERO(HOST, PORT, USERNAME, PASSWORD);

//  Create a dataset
dataset_id = Ext.createDataset("test_dataset", "");

//  Create a tag
tag_id = Ext.createTag("new tag", "");

// Link the tag and dataset
Ext.link("Dataset", dataset_id, "Tag", tag_id);

print("processing done");

// Close the connection
Ext.disconnect();
