// @String(label="Username") USERNAME
// @String(label="Password", style='password', persist=false) PASSWORD
// @String(label="Host", value='wss://workshop.openmicroscopy.org/omero-ws') HOST
// @Integer(label="Port", value=443) PORT
// @Integer(label="Image ID", value=2331) image_id

run("OMERO Extensions");

//Connect to OMERO
connected = Ext.connectToOMERO(HOST, PORT, USERNAME, PASSWORD);

//Open the image
ij_id = Ext.getImage(image_id);

Ext.disconnect();