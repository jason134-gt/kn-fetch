package com.yfzx.service.msg.handler.m;

import com.stock.common.event.NotifyEvent;
import com.stock.common.msg.TradeAlarmMsg;
import com.stock.common.util.StockUtil;
import com.yfzx.service.trade.TradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.msg.handler.IHandler;

public class TradeMsgType0Handler implements IHandler {

	@Override
	public void handle(Object o) {
//		if (o == null)
//			return;
//		NotifyEvent e = (NotifyEvent) o;
//		TradeAlarmMsg um = (TradeAlarmMsg) e.getMsg();
////		String nkey = um.getSourceid() + "^" + um.getMsgType();
////		UserEventService.getInstance().putEvent2Queue(nkey, e);
//		int size1 = ConfigCenterFactory.getInt("stock_zjs.chanceSize1", 300);
//		int size2 = ConfigCenterFactory.getInt("stock_zjs.chanceSize2", 500);
//		int size3 = ConfigCenterFactory.getInt("stock_zjs.chanceSize3", 1000);
//		int size4 = ConfigCenterFactory.getInt("stock_zjs.chanceSize4", 2000);
//		//放入有机可趁
//		String desc = um.getAttr("desc");
//		String name = TradeService.getInstance().getRealMsgDescByRealType(Integer.valueOf(desc));
//		String firstKey = TradeService.getInstance().getFirstByType(um.getSourceid());
//		String key = StockUtil.joinString("_", firstKey,"js","cdx",desc);
//		//只放一次
//		um.putAttr("mn", name);
//		String cname = TradeService.getInstance().getCategoryName("_", firstKey,"js","cdx")+"_"+name;
//		TradeService.getInstance().add2ChanceWaper(key,cname,um,size1,true, null,1);
//
//		key = StockUtil.joinString("_", firstKey,"js","cdx");
//		cname = TradeService.getInstance().getCategoryName("_", firstKey,"js","cdx");
//		TradeService.getInstance().add2ChanceWaper(key,cname,um,size2,false, null,2);
//
//		key = StockUtil.joinString("_", firstKey,"js");
//		name = TradeService.getInstance().getCategoryName("_", firstKey,"js");
//		TradeService.getInstance().add2ChanceWaper(key,cname,um,size3,false, null,3);
//
//		key = firstKey;
//		cname = TradeService.getInstance().getNameByType(key);
//		TradeService.getInstance().add2ChanceWaper(key,cname,um,size4,false, null, 4);

	}

}
