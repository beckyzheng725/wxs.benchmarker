package com.john.wxs.benchmarker;

import java.io.File;

import com.john.wxs.benchmarker.api.Objectifiable;

public class ValueObjectFactory {
	
	private static ValueObjectFactory _instance;
	private static File _file;
	
	private ValueObjectFactory(File file){
		_file = file;
	}
	
	public static ValueObjectFactory getInstance(File file){
		if (_instance == null){
			_instance = new ValueObjectFactory(file);
			return _instance;
		} else {
			if (_file == file){
				return _instance;
			} else {
				return new ValueObjectFactory(file);
			}
		}
	}
	
	public Objectifiable getObjectifier(String clazz){
		try {
			Class c = Class.forName(clazz);
			return (Objectifiable) c.newInstance();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return new DefaultValueObjectifier();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return new DefaultValueObjectifier();
		} catch (InstantiationException e) {
			e.printStackTrace();
			return new DefaultValueObjectifier();
		} 
	}
}
