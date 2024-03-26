// @String(label="Username") USERNAME
// @String(label="Password", style='password', persist=false) PASSWORD
// @String(label="Host", value='wss://workshop.openmicroscopy.org/omero-ws') HOST
// @Integer(label="Port", value=443) PORT
// @Integer(label="Image ID", value=2331) image_id

run("OMERO Extensions");

//Connect to OMERO
connected = Ext.connectToOMERO(HOST, PORT, USERNAME, PASSWORD);

//Need an image in Fiji to load ROIs
newImage("tmp_load", "8-bit color-mode", 1, 1, 1, 1, 1);

ij_id = Ext.getROIs(image_id);
roiManager("list");
for(i=0; i<RoiManager.size; i++){
	name = getResultString("Name", i);
	x1 = getResult("X", i);
	x2 = getResult("Width", i) + x1 - 1;
	y1 = getResult("Y", i);
	y2 = getResult("Height", i) + y1 - 1;
	bounds = String.format("x:%.0f:%.0f y:%.0f:%.0f", x1,x2,y1,y2);
	
	c = getResult("C", i);
	z = getResult("Z", i);
	t = getResult("T", i);
	if(c>0) bounds = bounds + " c:"+(c-1);
	if(z>0) bounds = bounds + " z:"+(z-1);
	if(t>0) bounds = bounds + " t:"+(t-1);
	print(bounds);
	Ext.getImage(image_id, bounds);
	rename(getTitle() + "["+name+"]");
}
close("tmp_load");
Ext.disconnect();
