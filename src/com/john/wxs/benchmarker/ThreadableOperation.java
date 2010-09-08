package com.john.wxs.benchmarker;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ibm.websphere.objectgrid.NoActiveTransactionException;
import com.ibm.websphere.objectgrid.ObjectGrid;
import com.ibm.websphere.objectgrid.ObjectGridException;
import com.ibm.websphere.objectgrid.ObjectMap;
import com.ibm.websphere.objectgrid.Session;
import com.ibm.websphere.objectgrid.TransactionAlreadyActiveException;
import com.ibm.websphere.objectgrid.TransactionException;
import com.ibm.websphere.objectgrid.UndefinedMapException;
import com.ibm.websphere.objectgrid.plugins.TransactionCallbackException;
import com.john.wxs.benchmarker.api.KVPair;
import com.john.wxs.benchmarker.api.Objectifiable;

public class ThreadableOperation implements Runnable{
	
	private boolean _isDebugMode;
	ConfigurationDetailsManager _cfgManager = null;
	private ObjectGrid _objectGrid;
	private static final int THREAD_NAME_IGNORE_CHARS = 29;
	private double _avgTimeMillis = 0;
	private double _avgTimeIterations = 0;
	
	
	
	
	public ThreadableOperation(ConfigurationDetailsManager mgr, ObjectGrid og, boolean isDebugMode){
		_cfgManager = mgr;
		_objectGrid = og;
		_isDebugMode = isDebugMode;
		
	}
	
	private void determineOperation() {
		switch (new Integer(_cfgManager.getDetails().getOperationType())){
		case 1: doSimplePut(); break;
		case 2: doSimpleGet(); break;
		case 3: doSimpleUpdate(); break;
		case 4: doSimpleDelete(); break;
		case 5: doSideCacheGet(); break;
		default: doUnknownOp(); break;
		
		}
		
		o(Thread.currentThread().getName() + " -- has ended it's work. Avg op time [" + _avgTimeMillis / _avgTimeIterations + " ms]");
		
		
		
	}
	
	private void doUnknownOp() {
		o("Unknown operation type. Check the configuration.");
		System.exit(1);
		
	}

	private void doSideCacheGet() {
		Session session = null;
		ObjectMap map = null;
		long startTime = 0;
		long endTime = 0;
		if (_cfgManager.getDetails().getCardinality().equalsIgnoreCase("1")){
			try {
				session = _objectGrid.getSession();
				startTime = System.nanoTime();
				map = session.getMap(_cfgManager.getDetails().getMapName());
				session.begin();
				Object ary =  map.get(_cfgManager.getDetails().getKey());
				if (ary == null){
					if (_isDebugMode){
						o("Cache miss, simulating a data fetch and insert into the grid..");
					}
					
					//do some data access delay mechanism here
					//code goes here <--------------------------------
					if (_cfgManager.getDetails().isDatastoreAccessSimulatorEnabled()){
						simulateDataAccess();
					}
					
					//end delay mechanism
					
					
					///TODO Need to consider using an option for using near cache in this scenario
					
					
					map.put(_cfgManager.getDetails().getKey(), _cfgManager.getDetails().getValue());
					session.commit();
					//do this line below to fetch the data from the grid. if we just used the value we
					//already had in the code above we could end up in a condition where he benchmarker
					//reports to have fetched data from the grid even though the PUT was not committed successfully
					ary = map.get(_cfgManager.getDetails().getKey());
					
				} 
				endTime = System.nanoTime();
				o("Op Time [" + Thread.currentThread().getName() +"](SIDECACHE): " + ElapsedTimeCalculator.calcTime(startTime, endTime) + " ms [" + _cfgManager.getDetails().getKey() + "] {" + getSerializedSize(ary) + " bytes}");
				_avgTimeMillis += ElapsedTimeCalculator.calcTime(startTime, endTime);
				_avgTimeIterations = 1;
			} catch (TransactionAlreadyActiveException e) {
				e.printStackTrace();
			} catch (TransactionCallbackException e) {
				e.printStackTrace();
			} catch (TransactionException e) {
				e.printStackTrace();
			} catch (UndefinedMapException e) {
				e.printStackTrace();
			} catch (NoActiveTransactionException e) {
				e.printStackTrace();
			} catch (ObjectGridException e) {
				e.printStackTrace();
			}
			
		} else if (_cfgManager.getDetails().getCardinality().equalsIgnoreCase("n")){
			//read keylist and determine how many times to run			
			 Map<Object, Object> m = null;
				try {
					m = consumeKeyListFile(_cfgManager.getDetails().getKeylist()[new Integer(Thread.currentThread().getName().substring(THREAD_NAME_IGNORE_CHARS))]);
				} catch (ArrayIndexOutOfBoundsException e1) {
					e("Error: Possible mismatch between the number of keylist files configured and the desired number of threads configured.\n Please recheck the configuration. ");
					if (_isDebugMode){
						e1.printStackTrace();
					}
					
				}
			 Object key = null;
			 Object value = null;
			 Iterator<Object> i= m.keySet().iterator();
			 while (i.hasNext()){
			 	 try {
						session = _objectGrid.getSession();
						key = i.next();
						value = m.get(key);
						startTime = System.nanoTime();
						map = session.getMap(_cfgManager.getDetails().getMapName());
						
						Object ary = map.get(key);
						if (ary == null){
							//do some data access delay mechanism here
							if (_cfgManager.getDetails().isDatastoreAccessSimulatorEnabled()){
								simulateDataAccess();
							}
							
							session.begin();
							map.put(key, value);
							session.commit();
							ary = map.get(key);
						}
						endTime = System.nanoTime();
						o("Op Time [" + Thread.currentThread().getName() +"](GET): " + ElapsedTimeCalculator.calcTime(startTime, endTime) + " ms [" + key + "] {" + getSerializedSize(ary) + " bytes}");
						_avgTimeMillis += ElapsedTimeCalculator.calcTime(startTime, endTime);
						_avgTimeIterations++; 
					} catch (TransactionAlreadyActiveException e) {
						e.printStackTrace();
					} catch (TransactionCallbackException e) {
						e.printStackTrace();
					} catch (TransactionException e) {
						e.printStackTrace();
					} catch (UndefinedMapException e) {
						e.printStackTrace();
					} catch (NoActiveTransactionException e) {
						e.printStackTrace();
					} catch (ObjectGridException e) {
						e.printStackTrace();
					}
			 }
		}
		
	}

	private void doSimplePut() {
		Session session = null;
		ObjectMap map = null;
		long startTime = 0;
		long endTime = 0;
		if (_cfgManager.getDetails().getCardinality().equalsIgnoreCase("1")){
			try {
				session = _objectGrid.getSession();
				startTime = System.nanoTime();
				session.begin();
				map = session.getMap(_cfgManager.getDetails().getMapName());
				map.put(_cfgManager.getDetails().getKey(), _cfgManager.getDetails().getValue());
				session.commit();
				endTime = System.nanoTime();
				o("Op Time [" + Thread.currentThread().getName() +"](PUT): " + ElapsedTimeCalculator.calcTime(startTime, endTime) + " ms [" + _cfgManager.getDetails().getKey() + "] {" + getSerializedSize(_cfgManager.getDetails().getValue()) + " bytes}");
				_avgTimeMillis += ElapsedTimeCalculator.calcTime(startTime, endTime);
				_avgTimeIterations++; 
			} catch (TransactionAlreadyActiveException e) {
				e.printStackTrace();
			} catch (TransactionCallbackException e) {
				e.printStackTrace();
			} catch (TransactionException e) {
				e.printStackTrace();
			} catch (UndefinedMapException e) {
				e.printStackTrace();
			} catch (NoActiveTransactionException e) {
				e.printStackTrace();
			} catch (ObjectGridException e) {
				e.printStackTrace();
			}
			
		} else if (_cfgManager.getDetails().getCardinality().equalsIgnoreCase("n")){
			//read keylist and determine how many times to run
			 Map<Object, Object> m = null;
			try {
				m = consumeKeyListFile(_cfgManager.getDetails().getKeylist()[new Integer(Thread.currentThread().getName().substring(THREAD_NAME_IGNORE_CHARS))]);
			} catch (ArrayIndexOutOfBoundsException e1) {
				e("Error: Possible mismatch between the number of keylist files configured and the desired number of threads configured.\n Please recheck the configuration. ");
				if (_isDebugMode){
					e1.printStackTrace();
				}
				
			}
			 Object key = null;
			 String mapName = _cfgManager.getDetails().getMapName();
			 Iterator<Object> i= m.keySet().iterator();
			 while (i.hasNext()){
			 	 try {
						session = _objectGrid.getSession();
						startTime = System.nanoTime();
						session.begin();
						map = session.getMap(mapName);
						key = i.next();
						map.put(key, m.get(key));
						session.commit();
						endTime = System.nanoTime();
						o("Op Time [" + Thread.currentThread().getName() +"](PUT): " + ElapsedTimeCalculator.calcTime(startTime, endTime) + " ms [" + key + "] {" + getSerializedSize(m.get(key)) + " bytes}");
						_avgTimeMillis += ElapsedTimeCalculator.calcTime(startTime, endTime);
						_avgTimeIterations++; 
					} catch (TransactionAlreadyActiveException e) {
						e.printStackTrace();
					} catch (TransactionCallbackException e) {
						e.printStackTrace();
					} catch (TransactionException e) {
						e.printStackTrace();
					} catch (UndefinedMapException e) {
						e.printStackTrace();
					} catch (NoActiveTransactionException e) {
						e.printStackTrace();
					} catch (ObjectGridException e) {
						e.printStackTrace();
					}
			 }
		}
		
	}
	
	private void doSimpleGet() {
		Session session = null;
		ObjectMap map = null;
		long startTime = 0;
		long endTime = 0;
		if (_cfgManager.getDetails().getCardinality().equalsIgnoreCase("1")){
			try {
				session = _objectGrid.getSession();
				startTime = System.nanoTime();
				map = session.getMap(_cfgManager.getDetails().getMapName());
				Object ary =  map.get(_cfgManager.getDetails().getKey());
				
				endTime = System.nanoTime();
				o("Op Time [" + Thread.currentThread().getName() +"](GET): " + ElapsedTimeCalculator.calcTime(startTime, endTime) + " ms [" + _cfgManager.getDetails().getKey() + "] {" + getSerializedSize(ary) + " bytes}");
				_avgTimeMillis += ElapsedTimeCalculator.calcTime(startTime, endTime);
				_avgTimeIterations = 1;
			} catch (TransactionAlreadyActiveException e) {
				e.printStackTrace();
			} catch (TransactionCallbackException e) {
				e.printStackTrace();
			} catch (TransactionException e) {
				e.printStackTrace();
			} catch (UndefinedMapException e) {
				e.printStackTrace();
			} catch (NoActiveTransactionException e) {
				e.printStackTrace();
			} catch (ObjectGridException e) {
				e.printStackTrace();
			}
			
		} else if (_cfgManager.getDetails().getCardinality().equalsIgnoreCase("n")){
			//read keylist and determine how many times to run			
			 Map<Object, Object> m = null;
				try {
					m = consumeKeyListFile(_cfgManager.getDetails().getKeylist()[new Integer(Thread.currentThread().getName().substring(THREAD_NAME_IGNORE_CHARS))]);
				} catch (ArrayIndexOutOfBoundsException e1) {
					e("Error: Possible mismatch between the number of keylist files configured and the desired number of threads configured.\n Please recheck the configuration. ");
					if (_isDebugMode){
						e1.printStackTrace();
					}
					
				}
			 Object key = null;
			 Iterator<Object> i= m.keySet().iterator();
			 
			 while (i.hasNext()){
			 	 try {
			 		 	session = _objectGrid.getSession();
						startTime = System.nanoTime();
						map = session.getMap(_cfgManager.getDetails().getMapName());
						key = i.next();
						Object ary = map.get(key);
						endTime = System.nanoTime();
						o("Op Time [" + Thread.currentThread().getName() +"](GET): " + ElapsedTimeCalculator.calcTime(startTime, endTime) + " ms [" + key + "] {" + getSerializedSize(ary) + " bytes}");
						_avgTimeMillis += ElapsedTimeCalculator.calcTime(startTime, endTime);
						_avgTimeIterations++; 
					} catch (TransactionAlreadyActiveException e) {
						e.printStackTrace();
					} catch (TransactionCallbackException e) {
						e.printStackTrace();
					} catch (TransactionException e) {
						e.printStackTrace();
					} catch (UndefinedMapException e) {
						e.printStackTrace();
					} catch (NoActiveTransactionException e) {
						e.printStackTrace();
					} catch (ObjectGridException e) {
						e.printStackTrace();
					}
			 }
		}
	}
	
	private void doSimpleDelete() {
		Session session = null;
		ObjectMap map = null;
		long startTime = 0;
		long endTime = 0;
		if (_cfgManager.getDetails().getCardinality().equalsIgnoreCase("1")){
			try {
				session = _objectGrid.getSession();
				startTime = System.nanoTime();
				session.begin();
				map = session.getMap(_cfgManager.getDetails().getMapName());
				Object ary =  map.remove(_cfgManager.getDetails().getKey());
				session.commit();
				endTime = System.nanoTime();
				o("Op Time [" + Thread.currentThread().getName() +"](DELETE): " + ElapsedTimeCalculator.calcTime(startTime, endTime) + " ms [" + _cfgManager.getDetails().getKey() + "] {" + getSerializedSize(ary) + " bytes}");
				_avgTimeMillis += ElapsedTimeCalculator.calcTime(startTime, endTime);
				_avgTimeIterations++; 
			} catch (TransactionAlreadyActiveException e) {
				e.printStackTrace();
			} catch (TransactionCallbackException e) {
				e.printStackTrace();
			} catch (TransactionException e) {
				e.printStackTrace();
			} catch (UndefinedMapException e) {
				e.printStackTrace();
			} catch (NoActiveTransactionException e) {
				e.printStackTrace();
			} catch (ObjectGridException e) {
				e.printStackTrace();
			}
			
		} else if (_cfgManager.getDetails().getCardinality().equalsIgnoreCase("n")){
			//read keylist and determine how many times to run			
			 Map<Object, Object> m = null;
				try {
					m = consumeKeyListFile(_cfgManager.getDetails().getKeylist()[new Integer(Thread.currentThread().getName().substring(THREAD_NAME_IGNORE_CHARS))]);
				} catch (ArrayIndexOutOfBoundsException e1) {
					e("Error: Possible mismatch between the number of keylist files configured and the desired number of threads configured.\n Please recheck the configuration. ");
					if (_isDebugMode){
						e1.printStackTrace();
					}
					
				}
			 Object key = null;
			 Iterator<Object> i= m.keySet().iterator();
			 while (i.hasNext()){
			 	 try {
						session = _objectGrid.getSession();
						startTime = System.nanoTime();
						session.begin();
						map = session.getMap(_cfgManager.getDetails().getMapName());
						key = i.next();
						Object ary = map.remove(key);
						session.commit();
						endTime = System.nanoTime();
						o("Op Time [" + Thread.currentThread().getName() +"](DELETE): " + ElapsedTimeCalculator.calcTime(startTime, endTime) + " ms [" + key + "] {" + getSerializedSize(ary) + " bytes}");
						_avgTimeMillis += ElapsedTimeCalculator.calcTime(startTime, endTime);
						_avgTimeIterations++; 
					} catch (TransactionAlreadyActiveException e) {
						e.printStackTrace();
					} catch (TransactionCallbackException e) {
						e.printStackTrace();
					} catch (TransactionException e) {
						e.printStackTrace();
					} catch (UndefinedMapException e) {
						e.printStackTrace();
					} catch (NoActiveTransactionException e) {
						e.printStackTrace();
					} catch (ObjectGridException e) {
						e.printStackTrace();
					}
			 }
		}
		
		
	}
	
	private void doSimpleUpdate() {
		Session session = null;
		ObjectMap map = null;
		long startTime = 0;
		long endTime = 0;
		if (_cfgManager.getDetails().getCardinality().equalsIgnoreCase("1")){
			try {
				session = _objectGrid.getSession();
				startTime = System.nanoTime();
				session.begin();
				map = session.getMap(_cfgManager.getDetails().getMapName());
				map.update(_cfgManager.getDetails().getKey(), _cfgManager.getDetails().getValue());
				session.commit();
				endTime = System.nanoTime();
				o("Op Time [" + Thread.currentThread().getName() +"](UPDATE): " + ElapsedTimeCalculator.calcTime(startTime, endTime) + " ms [" + _cfgManager.getDetails().getKey() + "] {" + getSerializedSize(_cfgManager.getDetails().getValue()) + " bytes}");
				_avgTimeMillis += ElapsedTimeCalculator.calcTime(startTime, endTime);
				_avgTimeIterations++; 
			} catch (TransactionAlreadyActiveException e) {
				e.printStackTrace();
			} catch (TransactionCallbackException e) {
				e.printStackTrace();
			} catch (TransactionException e) {
				e.printStackTrace();
			} catch (UndefinedMapException e) {
				e.printStackTrace();
			} catch (NoActiveTransactionException e) {
				e.printStackTrace();
			} catch (ObjectGridException e) {
				e.printStackTrace();
			}
			
		} else if (_cfgManager.getDetails().getCardinality().equalsIgnoreCase("n")){
			//read keylist and determine how many times to run			
			 Map<Object, Object> m = null;
				try {
					m = consumeKeyListFile(_cfgManager.getDetails().getKeylist()[new Integer(Thread.currentThread().getName().substring(THREAD_NAME_IGNORE_CHARS))]);
				} catch (ArrayIndexOutOfBoundsException e1) {
					e("Error: Possible mismatch between the number of keylist files configured and the desired number of threads configured.\n Please recheck the configuration. ");
					if (_isDebugMode){
						e1.printStackTrace();
					}
					
				}
			 Object key = null;
			 Iterator<Object> i= m.keySet().iterator();
			 while (i.hasNext()){
			 	 try {
						session = _objectGrid.getSession();
						startTime = System.nanoTime();
						session.begin();
						map = session.getMap(_cfgManager.getDetails().getMapName());
						key = i.next();
						map.update(key, m.get(key));
						session.commit();
						endTime = System.nanoTime();
						o("Op Time [" + Thread.currentThread().getName() +"](UPDATE): " + ElapsedTimeCalculator.calcTime(startTime, endTime) + " ms [" + key + "] {" + getSerializedSize(m.get(key)) + " bytes}");
						_avgTimeMillis += ElapsedTimeCalculator.calcTime(startTime, endTime);
						_avgTimeIterations++; 
					} catch (TransactionAlreadyActiveException e) {
						e.printStackTrace();
					} catch (TransactionCallbackException e) {
						e.printStackTrace();
					} catch (TransactionException e) {
						e.printStackTrace();
					} catch (UndefinedMapException e) {
						e.printStackTrace();
					} catch (NoActiveTransactionException e) {
						e.printStackTrace();
					} catch (ObjectGridException e) {
						e.printStackTrace();
					}
			 }
		}
		
	}
	
	private Map<Object, Object> consumeKeyListFile(File file){
		Map<Object, Object> map = null;
		
		
		Objectifiable obj = ValueObjectFactory.getInstance(file).getObjectifier(_cfgManager.getDetails().getValueObjectFactoryClass());
		
		KVPair kvp = null;
		if (file.exists()){
			map = new HashMap<Object, Object>();
			
			try {
				FileReader fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				String line = null;
				while ((line = br.readLine()) != null){
					kvp = obj.objectify(line);
					map.put(kvp.getKey(), kvp.getValue());
					
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		} else {
			e("Error: The specified keyFile does not exist: " + file);
		}
		return map;
		
		
	}
	
	private long getSerializedSize(Object o){
        ObjectOutputStream oos = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            oos.flush();
            oos.close();
            return baos.toByteArray().length;
        } catch (IOException ex) {
            if (_isDebugMode){
            	e("Error: Problem serializing the object -- " + ex.getLocalizedMessage() + " -- " + ex.getCause());
            }
            return 0L;
        } finally {
            try {
                oos.close();
            } catch (IOException ex) {
            	e("Error: Problem serializing the object -- " + ex.getLocalizedMessage() + " -- " + ex.getCause());
            }
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
	
	private void simulateDataAccess(){
		try {
			Double d = (Math.random() * _cfgManager.getDetails().getDatastoreMaxAccessTime() * 1000);
			Thread.sleep(d.longValue());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	


	public void run() {
		determineOperation();
	}

}
