package com.yfzx.service.spider;

import java.util.Comparator;

public class ComparatorTagBean implements Comparator<TagBean>{

	@Override
	public int compare(TagBean o1, TagBean o2) {
		//112上遇到很怪的问题 NoSuchMethodError o1.getNum()
		try{
			int num1 = o1.getNum();
			int num2 = o2.getNum();
			int compareResult = num1-num2;
			//从大到小排序
			if(compareResult <0){
				return 1;
			}else if(compareResult >0){
				return -1;
			}else{//相等时候，暂时不排序
				return 1;
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return 1;
	}

}
