package com.yfzx.service.msg.handler.m;

import java.util.HashSet;
import java.util.Set;

import com.stock.common.event.NotifyEvent;
import com.stock.common.model.Tagrule;
import com.stock.common.msg.TradeAlarmMsg;
import com.stock.common.util.StockUtil;
import com.yfzx.service.db.TagruleService;
import com.yfzx.service.trade.TradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.msg.handler.IHandler;

public class TradeMsgType1Handler implements IHandler {

	public static Set<Long> _hasComputeToday_add = new HashSet<Long>();
	Long lastPersitanceTime = System.currentTimeMillis();
	@Override
	public void handle(Object o) {
//		if (o == null)
//			return;
//		NotifyEvent e = (NotifyEvent) o;
//		TradeAlarmMsg um = (TradeAlarmMsg) e.getMsg();
////		String nkey = um.getSourceid() + "^" + um.getMsgType();
////		UserEventService.getInstance().putEvent2Queue(nkey, e);
//
////		clearPreDayChances();
////		// 放入有机可趁
////		TradeService.getInstance().add2ChanceWaper(um);
//
//		int size1 = ConfigCenterFactory.getInt("stock_zjs.chanceSize1", 300);
//		int size2 = ConfigCenterFactory.getInt("stock_zjs.chanceSize2", 500);
//		int size3 = ConfigCenterFactory.getInt("stock_zjs.chanceSize3", 1000);
//		int size4 = ConfigCenterFactory.getInt("stock_zjs.chanceSize4", 2000);
//
//		Tagrule tr = TagruleService.getInstance().getTagruleByIdFromCache(
//				um.getEventid());
//		if(tr==null)
//			return ;
//		String name = tr.getTagDesc();
//		String firstKey = TradeService.getInstance().getFirstByType(um.getSourceid());
//		String key = StockUtil.joinString("_", firstKey,"js","dx",tr.getId());
//		//只放一次
//		um.putAttr("mn", name);
//		String cname = TradeService.getInstance().getCategoryName("_", firstKey,"js","dx")+"_"+name;
//		TradeService.getInstance().add2ChanceWaper(key,cname,um,size1,true, null,1);
//
//		key = StockUtil.joinString("_", firstKey,"js","dx");
//		cname = TradeService.getInstance().getCategoryName("_", firstKey,"js","dx");
//		TradeService.getInstance().add2ChanceWaper(key,cname,um,size2,false, null,2);
//
//		key = StockUtil.joinString("_", firstKey,"js");
//		cname = TradeService.getInstance().getCategoryName("_", firstKey,"js");
//		TradeService.getInstance().add2ChanceWaper(key,cname,um,size3,false,null,3);
//
//		key = firstKey;
//		cname = TradeService.getInstance().getNameByType(key);
//		TradeService.getInstance().add2ChanceWaper(key,cname,um,size4,false, null,4);

//		persistenceChances();
	}

	private void persistenceChances() {
		long chancePersitanceTimeInterval = ConfigCenterFactory.getLong("stock_zjs.chancePersitanceTimeInterval", 900000l);
		//间隔一段时间持久化一次
		if(System.currentTimeMillis() - lastPersitanceTime>chancePersitanceTimeInterval)
		{
			synchronized (lastPersitanceTime) {
				if(System.currentTimeMillis() - lastPersitanceTime>chancePersitanceTimeInterval)
				{
//					TradeService.getInstance().flushChances2Disk();
					lastPersitanceTime = System.currentTimeMillis();
				}
			}
		}

	}

	/**
	 * 清除前一天的机会消息
	 */
//	private void clearPreDayChances() {
//		Date td = DateUtil.getDayStartTime(new Date());
//		if(!_hasComputeToday_add.contains(td.getTime()))
//		{
//			synchronized (_hasComputeToday_add) {
//				if(!_hasComputeToday_add.contains(td.getTime()))
//				{
//					String msgtypes = ConfigCenterFactory.getString("snn.chance_stock_need_clear_msg_type", "100,101");
//					for(String msgtype:msgtypes.split(","))
//					{
//						TradeService.getInstance().clearChanceWapper(Integer.valueOf(msgtype));
//					}
//					_hasComputeToday_add.clear();
//					_hasComputeToday_add.add(td.getTime());
//				}
//			}
//
//		}
//
//	}

}
