package com.yfzx.service.spider;

import java.util.ArrayList;
import java.util.List;

/**
 * 用于TalkMockService对公司和用户平均分配的比较对象
 * @author Administrator
 *
 */
public class UidCompany {
	
	private List<String> companyList = new ArrayList<String>();
	private long uid;
	@SuppressWarnings("unused")
	private UidCompany(){
		
	}
	
	public UidCompany(long uid){
		this.uid = uid;		
	}
	
	public long getUid(){
		return this.uid;
	}
	
	/**
	 * 用于排序
	 * @return
	 */
	public int getSize(){
		if(companyList != null){
			return companyList.size();
		}else{
			return 0;
		}
	}
	
	public List<String> getList(){
		return new ArrayList<String>(companyList);
	}
	
	public void add(String code){
		for(String str : companyList){
			if(str.equals(code)){
				return;
			}
		}
		
		companyList.add(code);
	}
	
	public void clear(){
		companyList.clear();
	}

}
