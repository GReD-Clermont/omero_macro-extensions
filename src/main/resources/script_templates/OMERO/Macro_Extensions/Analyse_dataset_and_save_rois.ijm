// @String(label="Username") USERNAME
// @String(label="Password", style='password') PASSWORD
// @String(label="Host", value='wss://workshop.openmicroscopy.org/omero-ws') HOST
// @Integer(label="Port", value=4064) PORT
// @Integer(label="Dataset ID", value=2331) dataset_id

run("OMERO Extensions");

connected = Ext.connectToOMERO(HOST, PORT, USERNAME, PASSWORD);

setBatchMode(true);
if(connected == "true") {
    images = Ext.list("images", "dataset", dataset_id);
    image_ids = split(images, ",");
    
    for(i=0; i<image_ids.length; i++) {
        ij_id = Ext.getImage(image_ids[i]);
        ij_id = parseInt(ij_id);
        roiManager("reset");
        run("8-bit");
        run("Auto Threshold", "method=MaxEntropy stack");
        run("Analyze Particles...", "size=10-Infinity pixel display clear add stack");
        run("Set Measurements...", "area mean standard modal min centroid center perimeter bounding summarize feret's median stack display redirect=None decimal=3");
        roiManager("Measure");
        nROIs = Ext.saveROIs(image_ids[i], "");
        print("Image " + image_ids[i] + ": " + nROIs + " ROI(s) saved.");
        roiManager("reset");
        close("Results");
        selectImage(ij_id);
        close();
    }
}
setBatchMode(false);

Ext.disconnect();
print("processing done");