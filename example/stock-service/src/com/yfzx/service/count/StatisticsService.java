package com.yfzx.service.count;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.stock.common.event.NotifyEvent;
import com.stock.common.model.snn.EConst;
import com.stock.common.util.StockUtil;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.mycore.msg.ClientEventCenter;
import com.yz.mycore.msg.message.IMessage;

/**
 * 统计信息服务类
 *      
 * @author：杨真 
 * @date：2014-7-15
 */
public class StatisticsService {

	private static StatisticsService instance = new StatisticsService();
	private StatisticsService()
	{
		
	}
	public static StatisticsService getInstance()
	{
		return instance;
	}
	/**
	 * 统计数据不需要很准确，所以不需要考虑并发
	 * @param key
	 * @param itemName
	 * @param add
	 */
	public void putStatisticsItem2Cache(String key,String itemName,int add)
	{
		Map<String,AtomicInteger> iem = LCEnter.getInstance().get(key, StockUtil.getStatisticsCacheName(key));
		if(iem==null)
		{
			iem = new ConcurrentHashMap<String,AtomicInteger>();
			LCEnter.getInstance().put(key,iem, StockUtil.getStatisticsCacheName(key));
		}
		AtomicInteger ai  = iem.get(itemName);
		if(ai==null)
		{
			ai = new AtomicInteger();
			iem.put(itemName, ai);
		}
		ai.addAndGet(add);
	}
	public void setStatisticsItem(String key,String itemName,int newValue)
	{
		Map<String,AtomicInteger> iem = LCEnter.getInstance().get(key, StockUtil.getStatisticsCacheName(key));
		if(iem==null)
		{
			iem = new ConcurrentHashMap<String,AtomicInteger>();
			LCEnter.getInstance().put(key,iem, StockUtil.getStatisticsCacheName(key));
		}
		AtomicInteger ai  = iem.get(itemName);
		if(ai==null)
		{
			ai = new AtomicInteger();
			iem.put(itemName, ai);
		}
		ai.set(newValue);
	}
	
	public int getStatisticsItemValue(String key,String itemName)
	{
		Map<String,AtomicInteger> iem = LCEnter.getInstance().get(key, StockUtil.getStatisticsCacheName(key));
		if(iem!=null)
		{
			AtomicInteger ai  = iem.get(itemName);
			if(ai!=null)
				return ai.get();
		}
		
		return 0;
	}
	
	public void notifyTheEvent(IMessage im)
	{
		NotifyEvent ne = new NotifyEvent();
		ne.setHType(EConst.EVENT_4);
		ne.setMsg(im);
		ClientEventCenter.getInstance().putEvent(ne);
	}
}
