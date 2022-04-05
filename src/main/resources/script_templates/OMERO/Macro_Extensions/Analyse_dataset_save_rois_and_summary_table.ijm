// @String(label="Username") USERNAME
// @String(label="Password", style='password', persist=false) PASSWORD
// @String(label="Host", value='wss://workshop.openmicroscopy.org/omero-ws') HOST
// @Integer(label="Port", value=4064) PORT
// @Integer(label="Dataset ID", value=2331) dataset_id

run("OMERO Extensions");

connected = Ext.connectToOMERO(HOST, PORT, USERNAME, PASSWORD);

table_name = "Summary_from_Fiji";

setBatchMode(true);
if(connected == "true") {
    images = Ext.list("images", "dataset", dataset_id);
    image_ids = split(images, ",");
    
    for(i=0; i<image_ids.length; i++) {
	    // Open the image
        ij_id = Ext.getImage(image_ids[i]);
        ij_id = parseInt(ij_id);
        roiManager("reset");
        // Analyse the images. This section could be replaced by any other macro
        run("8-bit");
        //white might be required depending on the version of Fiji
        run("Auto Threshold", "method=MaxEntropy stack");
        run("Analyze Particles...", "size=10-Infinity pixel display clear add stack");
        run("Set Measurements...", "area mean standard modal min centroid center perimeter bounding summarize feret's median stack display redirect=None decimal=3");
        roiManager("Measure");
        // Save the ROIs back to OMERO
        nROIs = Ext.saveROIs(image_ids[i], "");
        print("creating summary results for image ID " + image_ids[i]);
        Ext.addToTable(table_name, "Results", image_ids[i]);
        print("Image " + image_ids[i] + ": " + nROIs + " ROI(s) saved.");
        roiManager("reset");
        close("Results");
        selectImage(ij_id);
        close();
    }
}
txt_file = getDir("temp") + "idr0021_merged_results.txt";
Ext.saveTableAsFile(table_name, txt_file, ",");
Ext.saveTable(table_name, "Dataset", dataset_id);
file_id = Ext.addFile("Dataset", dataset_id, txt_file);
deleted = File.delete(txt_file);
setBatchMode(false);

Ext.disconnect();
print("processing done");