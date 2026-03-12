package com.yfzx.service.util;

import com.stock.common.constants.AAAConstants;
import com.stock.common.msg.MsgConst;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.config.BaseConfiguration;
import com.yz.mycore.core.config.ConfigFactory;
/**
 * 服务公用类
 *      
 * @author：杨真 
 * @date：2014-8-15
 */
public class ServiceUtil {
	//本机的id编号？
	public static int getSerial(String sips){
		int serial = 0;
		
		String[] policya = sips.split("\\^");
		for (int i = 0; i < policya.length; i++) {
			String pid = policya[i];

			if (pid.equals(BaseConfiguration.getAppId())) {
				serial = i;
				break;
			}

		}
		return serial;
	}
	
	public static String getServerIP(String serverId){
		int index = 0;
		String ids = ConfigCenterFactory.getString("dcss.ext_cache_server_id_list", "dcss01");
		String ips = ConfigCenterFactory.getString("dcss.ext_cache_server_ip_list", "192.168.1.112:7777");
		String[] policya = ids.split("\\^");
		for (int i = 0; i < policya.length; i++) {
			String pid = policya[i];
			if (pid.equals(serverId)) {
				index = i;
				break;
			}
			
		}
		return ips.split("\\^")[index];
	}
	
	public static String getTalkMessageListKey(String k) {
		// TODO Auto-generated method stub
//		return "tkl^"+k;
		return k + "^" + MsgConst.MSG_USER_TYPE_7;
	}
	
}
