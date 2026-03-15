package com.yfzx.service.mobile;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.stock.common.constants.MobileConst;
import com.stock.common.util.StringUtil;
import com.yz.configcenter.ConfigCenterFactory;

public class MobileAgentService {
	private static MobileAgentService instance=new MobileAgentService();
	
	public static MobileAgentService getIntance(){
		return instance;
	}
	
	/**
	 *判断当前的请求类型
	 *0为其他请求
	 *1为android
	 *2为iphone
	 * */
	public int getRequestType(HttpServletRequest request){
		String userAgent=request.getHeader(MobileConst. m_request_userAgent);
		if(StringUtils.isEmpty(userAgent)){//兼容旧版本的android客户端.
			userAgent=request.getHeader(MobileConst.m_request_Android_Old_userAagent);
		}
		if(!StringUtil.isEmpty(userAgent)){
			String[] mobileFlagArr=ConfigCenterFactory.getString("stock_zjs.mobile_http_head_flag", "android;iphone").split(";");
			for(int i=mobileFlagArr.length-1;i>=0;i--){
				String mobileFlag=mobileFlagArr[i];
				if(mobileFlag.equalsIgnoreCase(userAgent)){
					return i+1;
				}
			}
		}
		return 0;
	}
}
