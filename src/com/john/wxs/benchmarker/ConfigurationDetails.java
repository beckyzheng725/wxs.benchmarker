package com.john.wxs.benchmarker;

import java.io.File;

public class ConfigurationDetails {
	private String gridName;
	private String catalogServerEndpoints;
	private String valueObjectFactoryClass;
	private boolean datastoreAccessSimulatorEnabled;
	private double datastoreMaxAccessTime;
	private String operationType;
	private String cardinality;
	private File[] keylist;
	private boolean isMultiThreaded;
	private int threadCount;
	private String mapName;
	private String key;
	private String value;

	public ConfigurationDetails() {
	}

	public String getCardinality() {
		return cardinality;
	}

	public void setCardinality(String cardinality) {
		this.cardinality = cardinality;
	}

	

	public boolean isMultiThreaded() {
		return isMultiThreaded;
	}

	public void setMultiThreaded(boolean isMultiThreaded) {
		this.isMultiThreaded = isMultiThreaded;
	}

	public String getMapName() {
		return mapName;
	}

	public void setMapName(String mapName) {
		this.mapName = mapName;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public int getThreadCount() {
		return threadCount;
	}

	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	public String getGridName() {
		return gridName;
	}

	public void setGridName(String gridName) {
		this.gridName = gridName;
	}

	public String getCatalogServerEndpoints() {
		return catalogServerEndpoints;
	}

	public void setCatalogServerEndpoints(String catalogServerEndpoints) {
		this.catalogServerEndpoints = catalogServerEndpoints;
	}

	public String getValueObjectFactoryClass() {
		return valueObjectFactoryClass;
	}

	public void setValueObjectFactoryClass(String valueObjectFactoryClass) {
		this.valueObjectFactoryClass = valueObjectFactoryClass;
	}

	public String getOperationType() {
		return operationType;
	}

	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}

	public File[] getKeylist() {
		return keylist;
	}

	public void setKeylist(File[] keylist) {
		this.keylist = keylist;
	}

	public double getDatastoreMaxAccessTime() {
		return datastoreMaxAccessTime;
	}

	public void setDatastoreMaxAccessTime(double datastoreMaxAccessTime) {
		this.datastoreMaxAccessTime = datastoreMaxAccessTime;
	}

	public boolean isDatastoreAccessSimulatorEnabled() {
		return datastoreAccessSimulatorEnabled;
	}

	public void setDatastoreAccessSimulatorEnabled(
			boolean datastoreAccessSimulatorEnabled) {
		this.datastoreAccessSimulatorEnabled = datastoreAccessSimulatorEnabled;
	}


}