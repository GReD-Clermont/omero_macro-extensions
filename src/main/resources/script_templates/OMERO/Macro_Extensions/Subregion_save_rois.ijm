// @String(label="Username") USERNAME
// @String(label="Password", style='password', persist=false) PASSWORD
// @String(label="Host", value='wss://workshop.openmicroscopy.org/omero-ws') HOST
// @Integer(label="Port", value=443) PORT
// @Integer(label="Image ID", value=2331) image_id

run("OMERO Extensions");

//Connect to OMERO
connected = Ext.connectToOMERO(HOST, PORT, USERNAME, PASSWORD);

// Possible syntax for subregion (same for each coordinate):
// "x:200:400"  select in X from 200 to 400, [incl, excl[
// "x:200:"     select in X from 200  (alias of "x:200:-1")
// "x::400"     select in X up to 400 (alias of "x:0:400")
// "z:10"       select the slice 10
// "z:10 x:200:400" combine them in any order
x = newArray(50, 150);
y = newArray(100, -1);
c = newArray(1, 2);
z = newArray(2, 4);
t = newArray(5, 10);

// Putting it all together
bounds = String.format("x:%.0f:%.0f y:%.0f:%.0f c:%.0f:%.0f z:%.0f:%.0f t:%.0f:%.0f", x[0], x[1], y[0], y[1], c[0], c[1], z[0], z[1], t[0], t[1]);

ij_id = Ext.getImage(image_id, bounds);

// ROI creation and assigning it to a specific slice
makeRectangle(8, 8, 4, 4);
roiManager("add");
roiManager("select", 0);
RoiManager.setPosition(2); // Assign to the second slice in Fiji

save_rois(x[0], y[0], c[0], z[0], t[0]); // Calls Ext.saveROIs(image_id) after shifting the ROIs
Ext.disconnect();

function save_rois(dx, dy, dc, dz, dt){
	// ROIs needs to be translated and assigned to the correct image slice on OMERO
	n = roiManager("count");
	for (i=0; i<n; i++) {
	  roiManager("select", i);
	  Roi.getPosition(channel, slice, frame); // If a previous channel/slice/frame is set to ROI, use that as starting point before shifting

	  if (channel>0) channel = dc + channel; // offset 'channel' with the 0-indexed value 'dc'
	  if (slice>0) slice = dz + slice;
	  if (frame>0) frame = dt + frame;
	  
	  RoiManager.setPosition(channel, slice, frame);
	  RoiManager.translate(dx, dy);
	}
	Ext.saveROIs(image_id);
}
