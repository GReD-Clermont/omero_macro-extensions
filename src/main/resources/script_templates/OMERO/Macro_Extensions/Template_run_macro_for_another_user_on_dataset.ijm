// @String(label="Username") USERNAME
// @String(label="Password", style='password', persist=false) PASSWORD
// @String(label="Host", value='wss://workshop.openmicroscopy.org/omero-ws') HOST
// @Integer(label="Port", value=443) PORT
// @Integer(label="Dataset ID", value=2331) dataset_id
// @String(label="Target User's name", value="") target_user

run("OMERO Extensions");

// Connect to OMERO
connected = Ext.connectToOMERO(HOST, PORT, USERNAME, PASSWORD);

//if target_user ~= None:
// Switch context to target user and open omeroImage as ImagePlus object
if(target_user != "") Ext.sudo(target_user);

// get all images in an omero dataset
images = Ext.list("images", "dataset", dataset_id);
image_ids = split(images, ",");

// Loop through each image
for(i=0; i<image_ids.length; i++) {
    // Open the image
    ij_id = Ext.getImage(image_ids[i]);
    ij_id = parseInt(ij_id);
    
    // Initialize the ROI manager
    run("ROI Manager...");
    // load the OMERO ROIs linked to a given image and add them to the manager
    count = Ext.getROIs(image_ids[i]);
    // Run Macro:
    // run("Enhance Contrast...", "saturated=0.3")
    // or
    // runMacro("/path/to/Macrofile")
    // Close the various components
    selectWindow("ROI Manager");
    run("Close");
    selectImage(ij_id);
    close();
}
// Close the connection
Ext.disconnect();
print("processing done");