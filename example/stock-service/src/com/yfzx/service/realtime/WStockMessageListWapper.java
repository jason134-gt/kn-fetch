package com.yfzx.service.realtime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.trade.StockTrade;

public class WStockMessageListWapper implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6433358233048089645L;
	//private static Object _q = new Object();
	Logger log = LoggerFactory.getLogger(WStockMessageListWapper.class);
	List<StockTrade> _q;
	String key;	

	public WStockMessageListWapper() {
		_q = new ArrayList<StockTrade>(1700);
	}

	/**
	 * 包装后的List,
	 * 删除等操作不影响内部
	 */
	public List<StockTrade> getMessageList() {
		if (_q == null) {
			_q = new ArrayList<StockTrade>(1700);
		}
		return Lists.newArrayList(_q);
	}
	
	/**
	 * 删除部分消息
	 */
	public void removeAll(List<StockTrade> tml){
		_q.removeAll(tml);
	}
	
	/**
	 * 清空 内部的List<StockTrade>
	 */
	public void clear(){
		_q.clear();
	}

	@Override
	public String toString() {
		return "WStockMessageListWapper [ _q=" + _q + ", key="
				+ key + ", ";
	}

	public WStockMessageListWapper(String key) {
		this.key = key;
	}

	public void put(StockTrade item) {		
		long itemTime = item.getUptime();
		// 清理过期的数据
		synchronized (_q) {
			StockTrade finded = null;
			int size = _q.size();
			int index = _q.size();
			// 数据本身是小的在前面，大的在后面
			for (int i = size-1; i >=0; i--) {
				StockTrade tm = _q.get(i);				
				if ( itemTime == tm.getUptime() ) {
					finded = tm;
					break;
				}else if(itemTime > tm.getUptime()){
					index = i;
					break;
				}
			}
			if (finded != null) {
				
			}else{
				_q.add(index, item);
			}			
		}
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}


	/**
	 * @param ftype 0=向前 1=向后
	 * @param time 时间点
	 * @param limit 去除的数目
	 * @return
	 */
	public List<StockTrade> getStockTradeList(int ftype, long time, int limit) {
		List<StockTrade> el = new ArrayList<StockTrade>();
		List<StockTrade> tel = null;
		if (_q.size() == 0)
			return null;
		synchronized (_q) {
			int index = _q.size();
			// 定位,查找开始取数的位置
			for (int i = 0; i < _q.size(); i++) {
				StockTrade e = _q.get(i);
				if (e != null) {
					if (e.getUptime()<= time) {
						if (ftype == 0) {
								index = i;
						} else {
							if(index>_q.size())
							{
								index=_q.size()-1;
							}
							else
								index = i;
						}

						break;
					}
				}
			}
			// 往前
			if (ftype == 0) {
				tel = _q.subList(0, index);
				if (tel.size() > 0) {
					int start = index - limit;
					if (start < 0)
						start = 0;
					//el = tel.subList(start, tel.size() - 1);
					el = Lists.newArrayList(tel.subList(start, tel.size()));
				}

			}

			// 往后
			if (ftype == 1) {
				if(time==0){
					index = 0;
				}
				tel = _q.subList(index, _q.size());
				if (tel.size() > 0) {
					int end = limit;
					if (end > tel.size())
						end = tel.size();
					//el = tel.subList(0, end);
					el = Lists.newArrayList(tel.subList(0, end));
				}

			}

		}
		return el;
	}

	/**
	 * 取最新消息列表
	 * 
	 * @param etime：截止时间
	 * @param maxcount:最大条数
	 * @param 
	 * @return
	 */
	public List<StockTrade> getStockTradeList(long etime, int maxcount) {
		List<StockTrade> el = new ArrayList<StockTrade>();
		if (_q.size() == 0)
			return null;
		synchronized (_q) {
			int index = 0;
			// 定位,查找开始取数的位置
			for (int i = 0; i < _q.size(); i++) {
				StockTrade e = _q.get(i);
				if (e != null) {
					if (e.getUptime() < etime||i>=maxcount) {
						index = i;
						break;
					}
				}
			}
			if(index!=0)
				el = Lists.newArrayList(_q.subList(0, index));
		}
		return el;
	}	
	
	public List<StockTrade> getStockTradeList(int start, int limit) {
		int size = _q.size();
		int end = start + limit;
		if (start > size)
			return null;
		if (end > size)
			end = size;
		return new ArrayList<StockTrade>(_q.subList(start, end));
	}

	public int getActiveSize() {
		if (_q == null) {
			return 0;
		} else {
			return _q.size();
		}
	}
}