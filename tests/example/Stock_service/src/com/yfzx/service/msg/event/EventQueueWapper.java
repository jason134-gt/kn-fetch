package com.yfzx.service.msg.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yz.mycore.msg.event.IEvent;
/**
 * 消息组包装类，带过期时间
 *      
 * @author：杨真 
 * @date：2014-7-26
 */
public class EventQueueWapper implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7824414181229037973L;
	Logger log = LoggerFactory.getLogger(EventQueueWapper.class);
	Queue<IEvent> _q;
	String key;
	int qsize = 60;
	long experied = 300000l;//-1为永不过期
//	static {
//		experied = ConfigCenterFactory.getLong("stock_dc.EventWapper_timeout",
//				300000l);
//	}

	public EventQueueWapper() {
		_q = new ArrayBlockingQueue<IEvent>(qsize);
		experied=300000l;
	}

	public EventQueueWapper(int qsize,long experied) {
		this.qsize = qsize;
		_q = new ArrayBlockingQueue<IEvent>(qsize);
		this.experied = experied;
	}

	public Queue<IEvent> getQueue() {
		if (_q == null) {
			_q = new ArrayBlockingQueue<IEvent>(qsize);
		}
		return _q;

	}

	public EventQueueWapper(String key) {
		this.key = key;
	}

	public void put(IEvent item) {
		//清理过期的数据
		clear();
		synchronized(this)
		{
			//如果没有放入，就先把头部移除一个，再放
			if(!_q.offer(item))
			{
				
					_q.poll();
					_q.offer(item);
			}
		}
	}

	// 放入时按时间先后排好序的
	public void clear() {
		try {
			if(experied<0)
				return;
			while (true) {
				if (_q.size() > 0) {
					IEvent e = _q.peek();
					if (e != null) {
						if (System.currentTimeMillis() - e.getTime() > experied) {
							_q.poll();
						} else
							break;
					} else
						break;
				} else
					break;

			}
		} catch (Exception e) {
			log.error("clearExperiedData failed", e);
		}
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public List<IEvent> getEventList()
	{
		List<IEvent> el = new ArrayList<IEvent>();
		if(_q.size()==0)
			return null;
		Iterator<IEvent> iter = _q.iterator();
		while(iter.hasNext())
		{
			IEvent e = iter.next();
			if(e!=null)
				el.add(e);
		}
		return el;
	}
	
	public Object[] getEventArray()
	{
		return _q.toArray();
	}
}
