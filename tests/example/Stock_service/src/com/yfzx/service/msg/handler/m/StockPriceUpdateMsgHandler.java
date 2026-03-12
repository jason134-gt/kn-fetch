package com.yfzx.service.msg.handler.m;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.event.NotifyEvent;
import com.stock.common.msg.StockPriceBatchMsg;
import com.stock.common.util.DateUtil;
import com.yfzx.service.chance.ChanceCategoryService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.spider.TalkMockService;
import com.yfzx.service.trade.ShareTimerTradeService;
import com.yfzx.service.trade.TradeCenter;
import com.yz.mycore.core.log.LogManager;
import com.yz.mycore.msg.handler.IHandler;

public class StockPriceUpdateMsgHandler implements IHandler {
	public static Set<Long> _hasComputeToday_add = new HashSet<Long>();
	static Logger logger = LoggerFactory.getLogger(StockPriceUpdateMsgHandler.class);
	
	@Override
	public void handle(Object o) {
		if (o == null)
			return;		
			
		clearPreDayData();
	
		NotifyEvent e = (NotifyEvent) o;
		StockPriceBatchMsg um = (StockPriceBatchMsg) e.getMsg();
		String msgUuid = um.getUuid();
		int msgCount = um.getBody().split("~").length;
		long startTime = System.currentTimeMillis();
		long uptime = um.getTime();
		IndexService.getInstance().putRealtimeResult2CacheV2(um.getBody(),uptime);
		LogManager.info("处理实时行情msgUuid="+msgUuid+",uptime="+DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS,new Date(uptime))+"，记录"+msgCount+"条，耗时="+(System.currentTimeMillis()-startTime)+"毫秒");	
	}
	
	
	private int isTradeOpen() {
		int st0 = TradeCenter.getInstance().getTradeStatus(0);
		int st1 = TradeCenter.getInstance().getTradeStatus(1);
		if (st0 == 1 || st1 == 1)
			return 1;
		return 0;
	}
	
	/**
	 * 如果是开市状态且收到数据  清除前一天的时分图消息
	 */
	private void clearPreDayData() {
		int state = isTradeOpen();
		if(state == 1){		
			Date td = DateUtil.getDayStartTime(new Date());		
			//重启不清理 重启后可能收到realtimer重启发来的数据
			if(!_hasComputeToday_add.contains(td.getTime()) )
			{
				synchronized (_hasComputeToday_add) {
					if(!_hasComputeToday_add.contains(td.getTime())){
						
						
						long systime = System.currentTimeMillis();
//						//创建最近一天的时分图
//						ShareTimerTradeService.getInstance().initShareTimes();						
//						ShareTimerTradeService.getInstance().clearExpiredShareTimerData();
//						logger.info("行情数据处理耗时"+(System.currentTimeMillis()-systime));
						
						_hasComputeToday_add.clear();
						_hasComputeToday_add.add(td.getTime());
//						ChanceCategoryService.getInstance().cleanYesterdayFenshiChance();
//						TalkMockService.getInstance().clear();
						
						
						//处理完成需要3秒，做成异步方式
						TimerTask tt =new TimerTask() {							
							public void run() { 
								long systime = System.currentTimeMillis();
								//时分图数据处理,跟putRealtimeResult2CacheV2的冲突，取消
//								ShareTimerTradeService.getInstance().initShareTimes();			
//								logger.info("ShareTimerTradeDateInit耗时"+(System.currentTimeMillis()-systime));
//								ShareTimerTradeService.getInstance().clearExpiredShareTimerData();
//								logger.info("ShareTimerTradeDateclear耗时"+(System.currentTimeMillis()-systime));								
								ChanceCategoryService.getInstance().cleanYesterdayFenshiChance();
								TalkMockService.getInstance().clear();
								logger.info("ChanceInit耗时"+(System.currentTimeMillis()-systime));
							}
						};
						Timer timer = new Timer("ShareTimerTradeDateInit",true);							
						timer.schedule(tt,0);						
						logger.info("行情数据处理耗时"+(System.currentTimeMillis()-systime));
						
					}
				}
				
			}
		}
	}

}
