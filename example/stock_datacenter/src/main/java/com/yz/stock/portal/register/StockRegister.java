package com.yz.stock.portal.register;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.register.IRegister;
import com.stock.common.constants.StockConstants;

public class StockRegister implements IRegister {

	Map<String,Object> _sMap = new ConcurrentHashMap<String,Object>();
	private String _prefix = "stock_";
	public <T> void register(String key, T value) {
		// TODO Auto-generated method stub
		if(key==null)
		{
			return ;
		}
		String _key = getKey(key);
		_sMap.put(_key, value);
		
	}

	private String getKey(String key) {
		// TODO Auto-generated method stub
		
		return _prefix+key;
	}

	public <T> T get(String key) {
		// TODO Auto-generated method stub
		if(key==null)
		{
			return null;
		}
		String _key = getKey(key);
		Object value = _sMap.get(_key);
		if(value==null)
		{
			return null;
		}
		return (T) value;
	}

	public <T> void remove(String key) {
		// TODO Auto-generated method stub
		if(key==null)
		{
			return ;
		}
		String _key = getKey(key);
		_sMap.remove(_key);
	}

	public void clear() {
		// TODO Auto-generated method stub
		_sMap.clear();
	}

}
