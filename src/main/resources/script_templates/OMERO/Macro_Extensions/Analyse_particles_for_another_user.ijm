// @String(label="Username") USERNAME
// @String(label="Password", style='password') PASSWORD
// @String(label="Host", value='wss://workshop.openmicroscopy.org/omero-ws') HOST
// @Integer(label="Port", value=4064) PORT
// @Integer(label="Dataset ID", value=2331) dataset_id
// @String(label="Target User's name", value="") target_user

run("OMERO Extensions");

connected = Ext.connectToOMERO(HOST, PORT, USERNAME, PASSWORD);

//if target_user ~= None:
// Switch context to target user and open omeroImage as ImagePlus object
if(target_user != "") Ext.sudo(target_user);

setBatchMode(true);
images = Ext.list("images", "dataset", dataset_id);
image_ids = split(images, ",");

for(i=0; i<image_ids.length; i++) {
    ij_id = Ext.getImage(image_ids[i]);
    ij_id = parseInt(ij_id);
    roiManager("reset");
    // Some analysis which creates ROI's and Results Table
    run("8-bit");
    //white might be required depending on the version of Fiji
    run("Auto Threshold", "method=MaxEntropy stack");
    run("Analyze Particles...", "size=10-Infinity pixel display clear add stack");
    run("Set Measurements...", "area mean standard modal min centroid center perimeter bounding summarize feret's median stack display redirect=None decimal=3");
    roiManager("Measure");
    nROIs = Ext.saveROIs(image_ids[i], "");
    print("Image " + image_ids[i] + ": " + nROIs + " ROI(s) saved.");
    // Close the various components
    roiManager("reset");
    close("ROI Manager");
    close("Results");
    selectImage(ij_id);
    close();
}
setBatchMode(false);
Ext.endSudo();
Ext.disconnect();
print("processing done");