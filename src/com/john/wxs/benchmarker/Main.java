package com.john.wxs.benchmarker;


import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.ibm.websphere.objectgrid.ClientClusterContext;
import com.ibm.websphere.objectgrid.ObjectGrid;
import com.ibm.websphere.objectgrid.ObjectGridManagerFactory;

public class Main {
	

	private static final String VERSION = "0.1";
	private boolean _isDebugMode = false;
	private ConfigurationDetailsManager _cfgManager;
	private ObjectGrid _objectGrid;
	private ClientClusterContext _ccc;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Main m = new Main();
		m.showLaunchSequence();
		m.inLoadCommandLineArgs(args);
		m.inLoadConfigData();
		m.initObjectGrid();
		m.doOperation();
		

	}
	
	private void initObjectGrid(){
		try {
			_ccc = ObjectGridManagerFactory.getObjectGridManager().connect(_cfgManager.getDetails().getCatalogServerEndpoints(), null, null);
			_objectGrid = ObjectGridManagerFactory.getObjectGridManager().getObjectGrid(_ccc, _cfgManager.getDetails().getGridName());
		} catch (Throwable e) {
			e("Error: error initing grid : " + e.getLocalizedMessage() + " -- Cause: " + e.getCause());
			if (_isDebugMode){
				e.printStackTrace();
			}
		}
		
	}


	
	private void doOperation() {
		if (_cfgManager.getDetails().isMultiThreaded()){
			if (_isDebugMode){
				o("Starting op " + _cfgManager.getDetails().getOperationType() + " in multi-threaded mode. ");
			}
			//do it multithreaded
			ThreadableOperation tOp = new ThreadableOperation(_cfgManager, _objectGrid, _isDebugMode);
			for (int i=0;i<_cfgManager.getDetails().getThreadCount(); i++){
				Thread t = new Thread(tOp, "BenchMarker-Operation-Thread-" + i);
				t.start();
				
			}
		} else {
			if (_isDebugMode){
				o("Starting op " + _cfgManager.getDetails().getOperationType() + " in single-threaded mode. ");
			}
			Thread.currentThread().setName("BenchMarker-Operation-Thread-0");
			ThreadableOperation tOp = new ThreadableOperation(_cfgManager, _objectGrid, _isDebugMode);
			tOp.run();
			
		}
		
		
	}

	private void showLaunchSequence() {
		o("Sample benchmarking tool for IBM WebSphere eXtreme Scale version " + VERSION);
		o("Created and developed by John Pape, 2010");
		o("This sample application is not supported by IBM. ");
		o("----------------------------------------------------------------------");
		
	}

	
	
	

	private void inLoadConfigData() {
		
		ResourceBundle config = null;
		try {
			//get the config data from the property file colocated with the application JAR
			config = ResourceBundle.getBundle("benchmarker", Locale.getDefault());
			o("Loaded config data (from property file)");
		} catch (MissingResourceException mre){
			//try once more using the default classpath location
			o("No config file was provided at runtime, defaulting to internal config data.");
			try {
				config = ResourceBundle.getBundle("com.john.wxs.benchmarker.benchmarker", Locale.getDefault());
			} catch (MissingResourceException mre2){
				e("Error: Unable to find any config file.");
			}
			
		}
			 
		Enumeration<String> e = config.getKeys();
		if (_isDebugMode){
			o("Dumping config file contents");
			o("--------------------------");
		}
		
		while(e.hasMoreElements()){
			String x = e.nextElement();
			if (config.getString(x).length() == 0){
				e("Error: Found unset configuration values, this is not allowed. [" + x + "]");
				System.exit(1);
			}
			if (_isDebugMode){
				o("CONFIG:[" + x + "]:" + config.getString(x));
			}
			
		}
		if (_isDebugMode){
			o("--------------------------");
		}
		
		_cfgManager = new ConfigurationDetailsManager(config);
		
		
		
	}

	private void inLoadCommandLineArgs(String[] args) {
		List<String> list = Arrays.asList(args);
		if (list.contains("debug") || list.contains("DEBUG")){
			_isDebugMode = true;
		}
		
		if (_isDebugMode){
			o("Dumping command line args:");
			o("--------------------------");
			for (int i=0;i<args.length;i++){
				this.o("ARG" + i + " :" + args[i]);
			}
			o("--------------------------");
		}
		
		
		
	}
	
	
	
	private void o(String message){
		o(message, true);
	}
	
	private void o(String message, boolean newLine){
		if (newLine){
			System.out.println(message);
		} else {
			System.out.print(message);
		}
	}
	
	private void e(String error){
		System.err.println(error);
	}

}

class ElapsedTimeCalculator {
	public static double calcTime(long start, long end){
		return (end-start)/1000000.0;
		
	}
}
