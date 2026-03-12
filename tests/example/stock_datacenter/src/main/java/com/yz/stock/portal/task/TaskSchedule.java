package com.yz.stock.portal.task;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskSchedule {

	public static Map<Long,Map<Long,Integer>> _pcMap = new ConcurrentHashMap<Long,Map<Long,Integer>>();
	public static long _lastAccess = System.currentTimeMillis();
	/**
	 * 父子结构pid:父id,childid:子线程id
	 * pid={cid1,cid2,cid3}
	 * @param pid
	 * @param childid
	 */
	public static void put(Long pid,Long childid)
	{
		synchronized (_pcMap) {
			Map<Long,Integer> cmap = _pcMap.get(pid);
			if(cmap==null)
			{
				cmap = new ConcurrentHashMap<Long,Integer>();
				_pcMap.put(pid, cmap);
			}
			cmap.put(childid, 1);
		}
		
	}
	
	public static boolean hasRunningTask()
	{
		//进行三次确认
		for(int i=0 ;i<3;i++)
		{
			synchronized (_pcMap) {
				Map<Long,Integer> cmap = _pcMap.get(Thread.currentThread().getId());
				if(cmap!=null&&cmap.keySet().size()>0)
					return true;
			}
			try {
				Thread.sleep(11327);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		return false;
	}
	public static void remove(Long pid,Long pcid)
	{
		synchronized (_pcMap) {
			Map<Long,Integer> cmap = _pcMap.get(pid);
			if(cmap!=null)
				cmap.remove(pcid);
		}
		
	}
	
}
