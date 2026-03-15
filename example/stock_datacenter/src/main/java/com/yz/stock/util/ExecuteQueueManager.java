package com.yz.stock.util;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.yz.common.vo.Pair;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.core.config.ConfigFactory;
import com.yz.mycore.msg.disruptor.MyDisruptor;
import com.yz.mycore.msg.disruptor.MycoreConsumeQueueHandler;
import com.yz.mycore.msg.handler.HandleUtil;
import com.yz.mycore.msg.handler.IHandler;
import com.yz.stock.portal.excel.IDataHandler;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.model.BatchQueueEntity;
import com.yz.stock.portal.service.Companyasset0290DataService;
import com.yz.stock.portal.service.Companycash0292DataService;
import com.yz.stock.portal.service.Companyprofile0291DataService;
import com.yz.stock.portal.service.HkTradeUpdateService;
import com.yz.stock.portal.service.IndustryDCService;
import com.yz.stock.portal.service.index.IndexDCService;

public class ExecuteQueueManager {

	static Logger logger = LoggerFactory.getLogger(ExecuteQueueManager.class);
	static ExecuteQueueManager instance = new ExecuteQueueManager();
	static ArrayBlockingQueue<BatchQueueEntity> _uQueue = new ArrayBlockingQueue<BatchQueueEntity>(
			1000);
	static ArrayBlockingQueue<BatchQueueEntity> _iQueue = new ArrayBlockingQueue<BatchQueueEntity>(
			1000);
	public static Map<Integer, IDataHandler> _hm = new ConcurrentHashMap<Integer, IDataHandler>();
	static AtomicInteger ai = new AtomicInteger();
	static ArrayBlockingQueue<HandlerFuture> _doQueue = new ArrayBlockingQueue<HandlerFuture>(
			100);
	static Long lastestIHandleTime = Calendar.getInstance().getTimeInMillis();// 最后一次处理时间
	static Long lastestUHandleTime = Calendar.getInstance().getTimeInMillis();// 最后一次处理时间
	
	static int hInterval = 30 * 1000;// 30秒
	static int batchISize = 1000;// 批量插入大小
	static int batchUSize = 1000;// 批量更新大小
	static int batchWriteThreadSize = 10;
	static int remainDBPoolSize = 10;//数据库连接池最小连接数
	static long batchWriteThreadSleep = 100l;
	public static int opencomputelog = 0;
	static MyDisruptor mydisruptor ;
	static Map<Object, Map<Integer, Pair<Long,List<BatchQueueEntity>>>> _typemaper = new ConcurrentHashMap<Object, Map<Integer, Pair<Long,List<BatchQueueEntity>>>>();
	static Long lastestHandleTime = Calendar.getInstance().getTimeInMillis();// 最后一次处理时间

	static HandleUtil<BatchQueueEntity> hu = new HandleUtil<BatchQueueEntity>();
	static {
		
		_hm.put(StockConstants.TYPE_ASSET0290,
				Companyasset0290DataService.getInstance());
		_hm.put(StockConstants.TYPE_PROFILE0291,
				Companyprofile0291DataService.getInstance());
		_hm.put(StockConstants.TYPE_CASH0292,
				Companycash0292DataService.getInstance());

		_hm.put(StockConstants.TYPE_EXTINDEX, IndexDCService.getInstance());
		_hm.put(Integer.valueOf(StockConstants.I_EXT_INDEX_TYPE), IndustryDCService.getInstance());
		
		
		_hm.put(Integer.valueOf(StockConstants.TYPE_Update_TRADE0001), HkTradeUpdateService.getInstance());
		
		Configuration c = ConfigFactory.getConfiguration();

//		batchWriteThreadSize = c.getInt("stock.thread.batchWriteThreadSize");
		batchWriteThreadSleep = c.getInt("stock.thread.batchWriteThreadSleep");
		
		batchWriteThreadSize = ConfigCenterFactory.getInt("stock_dc.batchWriteThreadSize", 5);
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh(){

			@Override
			public void refresh() {
				batchWriteThreadSize = ConfigCenterFactory.getInt("stock_dc.batchWriteThreadSize", 5);
				opencomputelog = ConfigCenterFactory.getInt("stock_dc.opencomputelog", 1);
			}
		});
		Thread doT = new Thread(new Runnable() {

			@SuppressWarnings("rawtypes")
			public void run() {
				Map<Future, HandlerFuture> m = new ConcurrentHashMap<Future, HandlerFuture>();
				ExecutorService bes = StockFactory.getBatchDBThreadPool();
				while (true) {
					try {
						// 每次取几个任务，运行完一个放入一个(运行完后删除任务）,再取
						HandlerFuture hf = _doQueue.poll();

						// Future ff = es.submit(hf);

						logger.debug("doQueueBatchWriteDbThread taskqueue current size : "
								+ _doQueue.size()
								+ ",not complete task size : "
								+ m.keySet().size());

						if (hf == null) {
							checkCompleteTask(m);
							Thread.sleep(1000l);
							//减少此日志打印
							if(Calendar.getInstance().getTimeInMillis()%10==0)
							   logger.debug("doQueueBatchWriteDbThread _doQueue is empty ,sleep 1000 ...noTaskComplents size :"+m.keySet().size());
							continue;
						}
						Future f = null;
						//检查数据库连接池是否有足够的连接数
//						DbConnectionBroker dbpool = StockFactory.getMyConPool();
//						int freeCount = dbpool.getSize() - dbpool.getUseCount();
//						while(freeCount<remainDBPoolSize)
//						{
//							logger.info("doQueueBatchWriteDbThread sleep 1000, db connection pool connection less,dbpool Size : "+dbpool.getSize()+" ; dbpool useCount : "+dbpool.getUseCount());
//							Thread.sleep(1000l);
//							freeCount = dbpool.getSize() - dbpool.getUseCount();
//							
//						}
						f = bes.submit(hf);

						if (f != null)
							m.put(f, hf);
						// 如果提交的任务数据大于系统（myTaskSize)设定的数，则先执行完已提交的任务，再进行下一轮任务
						if (m.keySet().size() > batchWriteThreadSize) {
							boolean noTaskComplents = true;
							while (noTaskComplents) {
								Set<Future> oSet = m.keySet();
								for (Future tf : oSet) {
									try {
										if (tf.isDone()) {
											Object tt = m.get(tf);
											logger.debug("doQueueBatchWriteDbThread this task is complete ! task : "
													+ tt);

											m.remove(tf);

											noTaskComplents = false;
										}
									} catch (Exception e) {
										logger.error(
												"doQueueBatchWriteDbThread wait future failed!",
												e);
									}
								}
								if (noTaskComplents) {
									if(Calendar.getInstance().getTimeInMillis()%10==0)
										logger.debug("doQueueBatchWriteDbThread noTaskComplents sleep 1000 ... taskqueue current size : "
												+ _doQueue.size()
												+ ",not complete task size : "
												+ m.keySet().size());
									Thread.sleep(1l);
								}
							}

							//在上一批任务都已执行完的情况下，清除上一轮的所有任务。
							// m.clear();
						}

						Thread.sleep(1);//每个流程结束，休息一下。
					} catch (Exception e) {
						logger.error("ExecuteQueueManager", e);
					}
				}

			}

			@SuppressWarnings("rawtypes")
			private void checkCompleteTask(Map<Future, HandlerFuture> m) {
				// TODO Auto-generated method stub
				Set<Future> oSet = m.keySet();
				for (Future tf : oSet) {
					try {
						if (tf.isDone()) {
							Object tt = m.get(tf);
							logger.debug("doQueueBatchWriteDbThread this task is complete ! task : "
									+ tt);
							m.remove(tf);
						}
					} catch (Exception e) {
						logger.error(
								" doQueueBatchWriteDbThread wait future failed!",
								e);
					}
				}
			}

		});
		doT.setName("doQueueBatchWriteDbThread");
		doT.start();

		mydisruptor = new MyDisruptor(new MycoreConsumeQueueHandler(new IHandler(){

			@Override
			public void handle(Object ee) {
				try {
					
					if(ee==null)
						return;
					BatchQueueEntity be = (BatchQueueEntity) ee;
					Map<Integer, Pair<Long,List<BatchQueueEntity>>> m = _typemaper.get(be
							.getType());
					if (m == null) {
						synchronized (_typemaper) {
							m = _typemaper.get(be.getType());
							if (m == null) {
								m = new ConcurrentHashMap<Integer, Pair<Long,List<BatchQueueEntity>>>();
								_typemaper.put(be.getType(), m);
							}
						}
					}
					if (m != null) {
						{
							hu.combineEvent(m, ai, batchISize, be);
						}
					}

				} catch (Exception e) {
					logger.error("ExecuteQueueManager batch handle failed!", e);
				}
				
			}
			
			
			
		}, 1, 1024*32,30000l,"ExecuteQueueManagerDisruptor"));
		
		
		Thread bt = new Thread(new Runnable() {

			public void run() {
				while (true) {
//					lastestHandleTime = System.currentTimeMillis();
					Iterator<Object> iter = _typemaper.keySet().iterator();
					while (iter.hasNext()) {
						Map<Integer, Pair<Long,List<BatchQueueEntity>>> m = _typemaper
								.get(iter.next());
						hu.batchHandle(m, ai, lastestHandleTime, batchISize,
								new IHandler() {

									public void handle(Object h) {
										if (h == null)
											return;
										List<BatchQueueEntity> sl = (List<BatchQueueEntity>) h;
										if (sl != null && sl.size() > 0) {
											BatchQueueEntity be = sl.get(0);
											// 更新
											IDataHandler hd = getHanlder(be
													.getType());
											HandlerFuture hf = new HandlerFuture(
													StockConstants.OPT_INSERT,
													sl, hd);
											ExecuteQueueManager.put2Doqueue(hf);
										}

									}

									private IDataHandler getHanlder(Integer type) {
										// TODO Auto-generated method stub
										return ExecuteQueueManager._hm
												.get(type);
									}
								});
					}

				}

			}

		});
		bt.setName("BatchComposeTask_flushThread");
		bt.start();
		
		// 组装批量数据线程
//		Thread t = new Thread(new BatchComposeRun(_iQueue, batchISize, ai));
//		t.setName("ExecuteQueueManager_thread1");
//		t.start();
//		
//		t = new Thread(new BatchComposeRun(_iQueue, batchISize, ai));
//		t.setName("ExecuteQueueManager_thread2");
//		t.start();
//		t = new Thread(new BatchComposeRun(_iQueue, batchISize, ai));
//		t.setName("ExecuteQueueManager_thread3");
//		t.start();
	}

	ExecuteQueueManager() {

	}

	public static ExecuteQueueManager getInstance() {
		return instance;
	}

	public static void add2UQueue(BatchQueueEntity e) {
		try {
			_uQueue.put(e);
		} catch (Exception e2) {
			// TODO: handle exception
		}
	}

	public static void add2IQueue(BatchQueueEntity e) {
		try {
//			_iQueue.put(e);
			mydisruptor.disruptor(e);
		} catch (Exception e2) {
			// TODO: handle exception
		}
	}

	public static void put2Doqueue(HandlerFuture e)
	{
		try {
			_doQueue.put(e);
		} catch (Exception e2) {
			// TODO: handle exception
		}
	}
	
}

