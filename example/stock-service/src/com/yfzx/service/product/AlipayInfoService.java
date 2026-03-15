package com.yfzx.service.product;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.product.AliPayInfo;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.agent.DBAgent;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.manager.DAFFactory;

public class AlipayInfoService {
	private final static Logger logger = LoggerFactory.getLogger(OrderService.class);
	private static AlipayInfoService instance = new AlipayInfoService();
	static DBAgent dbAgent = DBAgent.getInstance();
	
	private static final String ALIPAYINFO_BASE_NS = "com.yz.stock.portal.dao.AliPayInfoDao";
	
	private AlipayInfoService() {}
	
	public static AlipayInfoService getInstance() {
		return instance;
	}
	
	public Long insert(AliPayInfo aliPayInfo) {
		RequestMessage req = DAFFactory.buildRequest(ALIPAYINFO_BASE_NS + ".insert", aliPayInfo, StockConstants.common);
		ResponseMessage rm = dbAgent.createRecord(req);
		String returnCode = rm.getRetrunCode();
		if(BaseCodes.SUCCESS.equals(returnCode)) {
			AliPayInfo o = (AliPayInfo)rm.getResult();
			return o.getId();
		}
		return null;
	}
	
	public AliPayInfo getAliPayInfo(long id) {
		RequestMessage req = DAFFactory.buildRequest(ALIPAYINFO_BASE_NS + ".selectByPrimaryKey", id, StockConstants.common);
		ResponseMessage res = dbAgent.queryForObject(req);
		return (AliPayInfo)res.getResult();
	}	
	
	/**
	 * 
	 * @param uid
	 * @param alipayInfo 可以传值null
	 * @param fetchSize
	 * @param offset
	 * @return
	 */
	public List<AliPayInfo> getAliPayInfoPaging(long uid, AliPayInfo alipayInfo, int fetchSize, int offset) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("fetchSize", fetchSize);
		params.put("offset", offset);
		params.putAll(convertToMap(alipayInfo));
		
		RequestMessage req = DAFFactory.buildRequest(ALIPAYINFO_BASE_NS+"."+"listPaging", params, StockConstants.common);
		ResponseMessage rm = dbAgent.queryForList(req);
		List<AliPayInfo> list = (List<AliPayInfo>)rm.getResult();
		return list;
	}
	
	public static Map convertToMap(Object obj) {
		Map map = new HashMap();
		if(obj != null) {
			Class c = obj.getClass();
			Method m[] = c.getDeclaredMethods();
			try {
				for (int i = 0; i < m.length; i++) {
					String methodName = m[i].getName();
					if (methodName.indexOf("get")==0) {
						Object val = m[i].invoke(obj, new Object[0]);
						if((! "getDataType".equals(methodName) && !("getKey").equals(methodName)) 
								&& (val != null || "".equals(val))) {
							 map.put(m[i].getName().substring(3).toLowerCase(), val);
						}
					}
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	    return map;
	}
	
}
