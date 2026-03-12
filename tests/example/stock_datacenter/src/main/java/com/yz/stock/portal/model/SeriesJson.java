package com.yz.stock.portal.model;

import java.util.ArrayList;
import java.util.List;

public class SeriesJson {

	private String name;
	
	private List<Double> data = new ArrayList<Double>();
	
	public String getName() {
		return name;
	}



	public List<Double> getData() {
		return data;
	}



	public void setData(List<Double> data) {
		this.data = data;
	}



	public void setName(String name) {
		this.name = name;
	}




}
