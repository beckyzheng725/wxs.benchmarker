package com.john.wxs.benchmarker;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import com.john.wxs.benchmarker.api.KVPair;
import com.john.wxs.benchmarker.api.Objectifiable;


public class DefaultValueObjectifier implements Objectifiable {

	public KVPair objectify(String line) {
		KVPair kvp = new KVPair();
		String[] data = line.split("=");
		if (data.length >1){
			//for PUT and UPDATE ops
			kvp.setKey(data[0]);
			kvp.setValue(Arrays.asList(data[1].split(",")));
			
		} else {
			//for GET and DELETE ops
			kvp.setKey(data[0]);
			kvp.setValue(new ArrayList<String>());
		}
		return kvp;
	}

}
