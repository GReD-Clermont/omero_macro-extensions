// @String(label="Username") USERNAME
// @String(label="Password", style='password', persist=false) PASSWORD
// @String(label="Host", value='wss://workshop.openmicroscopy.org/omero-ws') HOST
// @Integer(label="Port", value=4064) PORT

path_to_file = File.openDialog("Choose a File");

run("OMERO Extensions");

// Connect to OMERO
connected = Ext.connectToOMERO(HOST, PORT, USERNAME, PASSWORD);

// Create a Dataset
dataset_id = Ext.createDataset("Image Imported via Fiji", "");

// Import the selected image to OMERO
print("importing...");
Ext.importImage(dataset_id, path_to_file);

Ext.disconnect();
print("Done");