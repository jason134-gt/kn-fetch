package com.yfzx.service.hfunction;

import java.util.List;

import com.stock.common.model.IndexMessage;

/**
 * 函数统一调用接口
 *      
 * @author：杨真 
 * @date：2014年10月14日
 */
public interface IFService {

	public Double doInvoke(IndexMessage req,
			List<String> vls);
}
