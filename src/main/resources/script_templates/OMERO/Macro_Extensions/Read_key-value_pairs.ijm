// @String(label="Username") USERNAME
// @String(label="Password", style='password', persist=false) PASSWORD
// @String(label="Host", value='wss://workshop.openmicroscopy.org/omero-ws') HOST
// @Integer(label="Port", value=443) PORT
// @Integer(label="Dataset ID", value=2331) dataset_id

run("OMERO Extensions");

connected = Ext.connectToOMERO(HOST, PORT, USERNAME, PASSWORD);

if(connected == "true") {
	// Read all key value pairs from a dataset
	kvs_dataset = Ext.getKeyValuePairs("dataset", dataset_id);
	
	// Default separator is a TAB
	kvs_dataset = split(kvs_dataset, "\t"); 
	// ! If some cells are empty, split will ignore the double separators
	// -> add a space between them first with replace(kvs_dataset, "\t\t", "\t \t");
	
	for(j=0; j<kvs_dataset.length; j=j+2){
		// Every even index is the key, every odd index is the value
		print(kvs_dataset[j] + ": " + kvs_dataset[j+1]);
	}
	
    images = Ext.list("images", "dataset", dataset_id);
    image_ids = split(images, ",");
    for(i=0; i<image_ids.length; i++) {
	    // Read from each image the value associated to the "condition" key 
		condition_image = Ext.getValue("image", image_ids[i], "condition");
		print(condition_image);
    }
}

Ext.disconnect();
print("processing done");