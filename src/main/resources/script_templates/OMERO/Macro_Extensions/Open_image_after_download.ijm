// @String(label="Username") USERNAME
// @String(label="Password", style='password', persist=false) PASSWORD
// @String(label="Host", value='wss://workshop.openmicroscopy.org/omero-ws') HOST
// @Integer(label="Port", value=443) PORT
// @Integer(label="Image ID", value=2331) image_id

run("OMERO Extensions");

// Connect to OMERO
connected = Ext.connectToOMERO(HOST, PORT, USERNAME, PASSWORD);

// Download the image. This could be composed of several files
temp_path = getDir("temp") + "OMERO_download";
File.makeDirectory(temp_path);
filelist = Ext.downloadImage(image_id, temp_path);
files = split(filelist, ",");
for(i=0; i<files.length; i++) {
    run("Bio-Formats Importer", "open=" + files[i] + " autoscale color_mode=Default view=[Standard ImageJ] stack_order=Default");
}
//Delete file in directory then delete it
files = getFileList(temp_path);
for(i=0; i<files.length; i++) {
    deleted = File.delete(temp_path + File.separator + files[i]);
}
deleted = File.delete(temp_path);

Ext.disconnect();
print("Done");