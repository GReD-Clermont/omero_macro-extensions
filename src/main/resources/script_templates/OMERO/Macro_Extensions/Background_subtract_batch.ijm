// @String(label="Username") USERNAME
// @String(label="Password", style='password', persist=false) PASSWORD
// @String(label="Host", value='wss://workshop.openmicroscopy.org/omero-ws') HOST
// @Integer(label="Port", value=443) PORT
// @Integer(label="Dataset ID", value=2331) dataset_id

run("OMERO Extensions");

// Connect to OMERO
connected = Ext.connectToOMERO(HOST, PORT, USERNAME, PASSWORD);

// Retrieve the images contained in the specified dataset
images = Ext.list("images", "dataset", dataset_id);
image_ids = split(images, ",");

//Create a dataset to store the newly created images will be added
name = "script_editor_output_from_dataset_" + dataset_id
new_dataset_id = Ext.createDataset(name, "");

// Loop through each image
for(i=0; i<image_ids.length; i++) {
	print(image_ids[i]);
	// Open the image
	ij_id = Ext.getImage(image_ids[i]);
	ij_id = parseInt(ij_id);
	run("Enhance Contrast...", "saturated=0.3");
	run("Subtract Background...", "rolling=50 stack");

    // Save modified image as OME-TIFF using Bio-Formats
    name = getTitle();
	name = replace(name, " ","");
	temp_path = getDir("temp") + name + ".ome.tiff";
	run("Bio-Formats Exporter", "save=" + temp_path + " compression=Uncompressed");
	close();
	
    // Upload the generated OME-TIFF to OMERO
    print("uploading..."):
    Ext.importImage(new_dataset_id, temp_path);
	// delete the local OME-TIFF image
	File.delete(temp_path);
	print("imported");
}

print("Done");
// Close the connection
Ext.disconnect();
