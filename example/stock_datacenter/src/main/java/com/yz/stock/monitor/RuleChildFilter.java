package com.yz.stock.monitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.yz.configcenter.ConfigCenterFactory;

public class RuleChildFilter  {
	
	Map<String, Long> _preRulecheckFilter = new ConcurrentHashMap<String, Long>();
	
	public boolean isFind(String key)
	{
		return _preRulecheckFilter.containsKey(key);
	}
	public void put(String k, Long v)
	{
		_preRulecheckFilter.put(k, v);
	}
	public void remove(String k)
	{
		_preRulecheckFilter.remove(k);
	}
	public void clear()
	{
		_preRulecheckFilter.clear();
	}
	public Long get(String k)
	{
		return _preRulecheckFilter.get(k);
	}
	@Override
	public String toString() {
		return "RuleChildFilter [_preRulecheckFilter=" + _preRulecheckFilter
				+"]";
	}
	
	
}
