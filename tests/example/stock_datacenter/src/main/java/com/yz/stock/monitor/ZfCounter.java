package com.yz.stock.monitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.stock.common.constants.StockConstants;
import com.stock.common.util.DateUtil;
import com.stock.common.util.SMathUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.realtime.RealtimeDataItem;
import com.yz.configcenter.ConfigCenterFactory;

public class ZfCounter {

	List<RealtimeDataItem> _success = new ArrayList<RealtimeDataItem>();
	static ZfCounter instance = new ZfCounter();
	Map<String,Integer> _ztm = new ConcurrentHashMap<String,Integer>();
	public ZfCounter() {

	}

	public static ZfCounter getInstance() {
		return instance;
	}

	public void add(RealtimeDataItem item) {
		_success.add(item);
	}

	public void clear() {
		_success.clear();
		_ztm.clear();
	}

	public void print() {
		double add = 0;
		double reduce = 0;
		double sumzf = 0.0;
		StringBuilder xdsb = new StringBuilder();
		StringBuilder szsb = new StringBuilder();
		StringBuilder ztsb = new StringBuilder();
		Double low = ConfigCenterFactory.getDouble("wstock.ZfCounter_zflow",
				-0.1);
		Double high = ConfigCenterFactory.getDouble("wstock.ZfCounter_zfhigh",
				0.0);
		for (RealtimeDataItem item : _success) {
			// 收盘价
			Double s = IndexValueAgent.getIndexValue(item.getUidentify(),
					StockConstants.INDEX_CODE_TRADE_S,
					DateUtil.getDayStartTime(item.getTime()));
			Double zdf = IndexValueAgent.getIndexValue(item.getUidentify(),
					StockConstants.INDEX_CODE_TRADE_ZDF,
					DateUtil.getDayStartTime(item.getTime()));
			if(s==null||item.getC()==0)
			{
				System.out.println("s is null"+item);
				continue;
			}
			Double zf = (s - item.getC()) / item.getC();
			if (zf > 0) {
				add++;
			} else {
				reduce++;
			}
			String type ;
			if (zf > low && zf < high) {
				xdsb.append(item.getUidentify());
				xdsb.append(";");
				type= "xdsb";
			} else {
				// 涨幅大于9%的单独拉出来
				if (zdf != null && zdf > 9.0) {
					ztsb.append(item.getUidentify());
					ztsb.append(";");
					type= "ztsb";
				} else {
					szsb.append(item.getUidentify());
					szsb.append(";");
					type= "szsb";
				}

			}
			System.out.println("type="+type+",uidentify=" + item.getUidentify()
					+ ",checkPrice=" + item.getC() + ",s="
					+ SMathUtil.getDouble(s, 2) + ",zf="
					+ SMathUtil.getDouble(zf * 100, 3) + ",zdf="
					+ SMathUtil.getDouble(zdf, 3) );
			sumzf += zf;
		}

		double xratio = add / (add + reduce) * 100;// 胜算
		double avgzf = sumzf / (add + reduce) * 100;// 平均涨幅
		System.out.println("avgzf=" + avgzf + "%,xratio=" + xratio + "%,add="
				+ add + ",reduce=" + reduce);
		System.out.println("xdsb:low=" + low + ",high=" + high + "|"
				+ xdsb.toString());
		System.out.println("szsb:" + szsb.toString());
		System.out.println("ztsb:" + ztsb.toString());
		
		Iterator<String>  iter = _ztm.keySet().iterator();
		while(iter.hasNext())
		{
			String k = iter.next();
			Integer seconds = _ztm.get(k);
			if(seconds!=0)
				System.out.println("uidentify="+k+",seconds="+seconds);
		}
	}
	
	
	public void addZTM(String uidentify,Double zf,Integer time)
	{
		if(zf>9.9)
		{
			Integer t = _ztm.get(uidentify);
			if(t==null)
			{
				_ztm.put(uidentify, time);
			}
		}
	}

	public Integer getZTSeconds(String uidentify) {
		// TODO Auto-generated method stub
		return _ztm.get(uidentify);
	}
}
