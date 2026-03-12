package com.yz.stock.portal.model;

import java.util.ArrayList;
import java.util.List;

public class IndustryCompanyArray {
	
	private String industryCode;
	private String name;
	private String companyArray;
	private List<String> companyList;
	
	public String getIndustryCode() {
		return industryCode;
	}
	public void setIndustryCode(String industryCode) {
		this.industryCode = industryCode;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCompanyArray() {
		return companyArray;
	}
	public void setCompanyArray(String companyArray) {
		this.companyArray = companyArray;
		this.companyList = new ArrayList<String>();
		if(companyArray != null){
			String[] comArr = companyArray.split(";");
			for(String com : comArr){
				this.companyList.add(com);
			}
		}		
	}
	public List<String> getCompanyList() {
		return companyList;
	}
	public void setCompanyList(List<String> companyList) {
		this.companyList = companyList;
	}

}
