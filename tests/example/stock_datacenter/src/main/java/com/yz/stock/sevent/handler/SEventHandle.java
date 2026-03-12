package com.yz.stock.sevent.handler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.event.NotifyEvent;
import com.stock.common.model.snn.EConst;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.TradeAlarmMsg;
import com.yfzx.service.trade.TradeService;
import com.yz.common.vo.Pair;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.msg.ClientEventCenter;
import com.yz.mycore.msg.event.IEvent;
import com.yz.mycore.msg.handler.HandleUtil;
import com.yz.mycore.msg.handler.IHandler;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.sevent.event.SEvent;
/**
 * SEvent事件handle
 * 为不阻塞上级队列，此处加了批量处理，按批次进行
 *      
 * @author：杨真 
 * @date：2014-4-10
 */
public class SEventHandle implements IHandler {

	static Logger logger = LoggerFactory.getLogger(SEventHandle.class);
	private static AtomicInteger _ai = new AtomicInteger(0);//处理批次
	private static int batchsize = 20;
	static long lastHandleTimes = System.currentTimeMillis();
	private static ConcurrentHashMap<Integer,Pair<Long,List<IEvent>>> _bcache = new ConcurrentHashMap<Integer,Pair<Long,List<IEvent>>>();
	static HandleUtil<IEvent> hu = new HandleUtil<IEvent>();
	static
	{
		Thread bt = new Thread(new Runnable(){
			public void run() {
				while(true)
				{
					hu.batchHandle(_bcache,_ai,lastHandleTimes,batchsize,new IHandler(){

						public void handle(Object h) {
							if(h==null)
								return ;
							List<IEvent> sl = (List<IEvent>) h;
							batchHandle(sl);
							
						}
						
					});
				}
				
			}
			
		});
		bt.setName("SEventHandlebatchThread");
		bt.start();
		
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {

			public void refresh() {
				batchsize = ConfigCenterFactory.getInt("yas.msg_batchsize", 100);
			}

		});
	}
	/**
	 * 为不阻塞上及队列，此处加了批量处理，按批次进行
	 */
	public void handle(Object e) {
		
		if(e==null) return;
		SEvent se = (SEvent) e;
		if(se.isSave())
		{
			lastHandleTimes = System.currentTimeMillis();
			hu.combineEvent(_bcache,_ai,batchsize,se);
		}
		//生成一个通知事件，把消息通知给消息订阅者
		TradeAlarmMsg tamsg = new TradeAlarmMsg(se.getSourceid(),se.getEid(),se.getStime().getTime(),se.getEtime().getTime());
		tamsg.setMsgType(MsgConst.MSG_TRADEMSG_TYPE_0);
		TradeService.getInstance().notifyTheEventChance(tamsg);
	}
	protected static void batchHandle(List<IEvent> sl) {
		try {
			if(sl==null||sl.size()==0) return;
			SqlSession sse = StockFactory.getSqlSessionFactory().openSession();
			Connection con =  sse.getConnection();
			boolean isfailed = false;
			boolean iscomplete = false;// 用来判断，是不是在for循环中一个循环还没有结束时，连接就free了
			try {

				con.setAutoCommit(false);
				Statement stmt = con.createStatement(
						ResultSet.TYPE_SCROLL_SENSITIVE,
						ResultSet.CONCUR_READ_ONLY);
				for (int x = 0; x < sl.size(); x++) {
					SEvent v = (SEvent) sl.get(x);
					stmt.addBatch(v.toUpdateSql());
				}
				stmt.executeBatch();
				con.commit();
				iscomplete = true;
			} catch (Exception e) {
				logger.error("BatchInsert failed!", e);
				isfailed = true;

			} finally {
				try {
					if (!iscomplete)
						logger.error("is not complete,but  db connection free!");
					//回笼处理
					if (isfailed) {
						// 如果此批失败，则进行单条操作
						handlerFailure(sl);
					}
					sse.close();
				} catch (Exception e2) {
					logger.error("close db connection failed!", e2);
				}
			}
		} catch (Exception e) {
			logger.error("get db connection failed!", e);
		}
		
	}
	/**
	 * 只进行一次恢复操作
	 * @param sl
	 */
	private static void handlerFailure(List<IEvent> sl) {
		try {
			SqlSession sse = StockFactory.getSqlSessionFactory().openSession();
			Connection con =  sse.getConnection();
			boolean iscomplete = false;// 用来判断，是不是在for循环中一个循环还没有结束时，连接就free了
			try {

				con.setAutoCommit(false);
				Statement stmt = con.createStatement(
						ResultSet.TYPE_SCROLL_SENSITIVE,
						ResultSet.CONCUR_READ_ONLY);
				for (int x = 0; x < sl.size(); x++) {
					SEvent v = (SEvent) sl.get(x);
					stmt.addBatch(v.toUpdateSql());
				}
				stmt.executeBatch();
				con.commit();
				iscomplete = true;
			} catch (Exception e) {
				logger.error("BatchInsert failed!", e);

			} finally {
				try {
					if (!iscomplete)
						logger.error("is not complete,but  db connection free!");
					sse.close();
					logger.error(sl.toString());
				} catch (Exception e2) {
					logger.error("close db connection failed!", e2);
				}
			}
		} catch (Exception e) {
			logger.error("get db connection failed!", e);
		}
		
	}

}
