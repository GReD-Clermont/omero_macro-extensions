// @String(label="Username") USERNAME
// @String(label="Password", style='password', persist=false) PASSWORD
// @String(label="Host", value='wss://workshop.openmicroscopy.org/omero-ws') HOST
// @Integer(label="Port", value=4064) PORT
// @Integer(label="Image ID", value=2331) image_id

run("OMERO Extensions");
setBatchMode(true);

// Connect to OMERO
print("connecting...");
connected = Ext.connectToOMERO(HOST, PORT, USERNAME, PASSWORD);

print("opening Image...");
// Open the Image using Bio-Formats
ij_id = Ext.getImage(image_id);

// Crop the image
print("cropping...");
makeRectangle(0, 0, 200, 200);
run("Crop");

// Save modified image as OME-TIFF using Bio-Formats Exporter
name = getTitle();
name = replace(name, " ","");
temp_path = getDir("temp") + name + ".ome.tiff";
run("Bio-Formats Exporter", "save=" + temp_path + " compression=Uncompressed");
close();

// Create a Dataset
dataset_id = Ext.createDataset("Cropped Image", "");

// Import the generated OME-TIFF to OMERO
print("importing...");
Ext.importImage(dataset_id, temp_path);
// delete the local OME-TIFF image
File.delete(temp_path);
print("imported");

// Close the connection
Ext.disconnect();
print("Done");

setBatchMode(false);