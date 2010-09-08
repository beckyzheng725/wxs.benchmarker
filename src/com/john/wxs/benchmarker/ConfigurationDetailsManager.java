package com.john.wxs.benchmarker;

import java.io.File;
import java.util.ResourceBundle;

public class ConfigurationDetailsManager {
	
	private ConfigurationDetails _details;
	
	
	public ConfigurationDetailsManager(ResourceBundle config){
		_details = new ConfigurationDetails();
		_details.setGridName(config.getString("grid.name"));
		_details.setCatalogServerEndpoints(config.getString("catalogserver.endpoints"));
		_details.setValueObjectFactoryClass(config.getString("value.object.factory.class"));
		_details.setDatastoreMaxAccessTime(new Double(config.getString("datastore.access.simulator.max.access.time")));
		_details.setDatastoreAccessSimulatorEnabled(new Boolean(config.getString("datastore.access.simulator.enabled")));
		_details.setOperationType(config.getString("operation.type"));
		_details.setCardinality(config.getString("operation." + _details.getOperationType() + ".cardinality"));
		//special handling for our multithreaded, n cardinality cases where we need multiple files (one for each thread)
		String[] strings = config.getString("operation." + _details.getOperationType() + ".keylist").split(",");
		File[] files = new File[strings.length];
		for (int i=0;i<files.length;i++){
			File f = new File(strings[i]);
			files[i] = f;
		}
		_details.setKeylist(files);
		_details.setMultiThreaded(new Boolean(config.getString("operation." + _details.getOperationType() + ".multithreaded")));
		_details.setThreadCount(new Integer(config.getString("operation." + _details.getOperationType() + ".thread.count")));
		_details.setMapName(config.getString("operation." + _details.getOperationType() + ".map.name"));
		_details.setKey(config.getString("operation." + _details.getOperationType() + ".target.key"));
		_details.setValue(config.getString("operation." + _details.getOperationType() + ".target.value"));
	}
	
	public ConfigurationDetails getDetails(){
		return _details;
	}

}
