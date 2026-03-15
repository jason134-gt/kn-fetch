package com.yz.stock.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.yz.stock.portal.excel.IDataHandler;
import com.yz.stock.portal.model.BatchQueueEntity;

public class BatchComposeRun implements Runnable {
	Map<String, List<Object>> _m = new ConcurrentHashMap<String, List<Object>>();
	Long lastestHandleTime = Calendar.getInstance().getTimeInMillis();// 最后一次处理时间
	ArrayBlockingQueue<BatchQueueEntity> _iQueue;
	int batchISize = 1000;
	Logger logger = LoggerFactory.getLogger(BatchComposeRun.class);
	AtomicInteger ai;
	int hInterval = 30 * 1000;// 30秒
	BatchComposeRun(ArrayBlockingQueue<BatchQueueEntity> _iQueue,int batchISize,AtomicInteger ai)
	{
		this._iQueue = _iQueue;
		this.batchISize = batchISize;
		this.ai = ai;
	}
	@SuppressWarnings("rawtypes")
	public void run() {
		while (true) {
			try {
				// 0为insert,1为update,优先处理插入
				handlerQueue(StockConstants.OPT_INSERT, _iQueue,
						batchISize);
				//数据导入时用到更新队列
//				if(_uQueue.size()!=0) handlerQueue(StockConstants.OPT_UPDATE, _uQueue,
//							batchUSize);
				// 当队列中没有新数据，而map中还有部分数据的量小于batchSize，而没有运算时，走下面的逻辑，进行更新
				long now = Calendar.getInstance().getTimeInMillis();
//				long merorySize = StockUtil
//						.getFreeMerory(SUnit.million);// 剩余内存数，单位M
//				while (merorySize < 50) {
//					try {
//						logger.info("merory size <50M,gc ........... merory size :"
//								+ merorySize);
//						System.gc();
//						Thread.sleep(2000l);
//						merorySize = StockUtil
//								.getFreeMerory(SUnit.million);// 剩余内存数，单位M
//						if (merorySize > 50)
//							break;
//					} catch (Exception e) {
//						logger.error("release merory failed!", e);
//					}
//
//				}
				if ((now - lastestHandleTime > hInterval)) {
					// 处理未处理的数据
					Iterator iter = _m.keySet().iterator();
					while (iter.hasNext()) {
						try {
							logger.info("clear no handle data...,_iQueue size :"+_iQueue.size());
							String key = (String) iter.next();
							String opt = key.split("_")[0];
							String type = key.split("_")[1];
							IDataHandler h = getHanlder(Integer
									.valueOf(type));
							List<Object> ul = _m.get(key);
							HandlerFuture hf = new HandlerFuture(opt,
									ul, h);

							ExecuteQueueManager.put2Doqueue(hf);

							_m.remove(key);
						} catch (Exception e) {
							logger.error("ExecuteQueueManager", e);
						}
					}
				}
				Thread.sleep(1);//每个流程结束，休息一下。
			} catch (Exception e) {
				logger.error("ExecuteQueueManager batch handle failed!",e);
			}
		}

	}

	/**
	 * 插入每批要小，这样冲突的机率小 更新可以大批进行
	 * 
	 * @param opt
	 * @param q
	 */
	private void handlerQueue(String opt,
			ArrayBlockingQueue<BatchQueueEntity> q, int batchSize) {
		try {

			BatchQueueEntity uo = q.take();
			if (uo == null)
			{
				logger.debug("ExecuteQueueManager iqueue is empty! iqueue size : "+q.size()+" sleep 200l");
				Thread.sleep(1l);
				return;
			}
			else
			{
				ai.incrementAndGet();
				logger.debug("ExecuteQueueManager  iqueue had handle count : "+ai.incrementAndGet());
			}
				

			lastestHandleTime = Calendar.getInstance()
					.getTimeInMillis();

			String key = opt + "_" + uo.getType() + "_"
					+ uo.getTableName();
			List<Object> ul = _m.get(key);
			if (ul == null) {
				ul = new ArrayList<Object>();
				_m.put(key, ul);
			}
			ul.add(uo.getV());

			// 当数据量大于批次大小或是时间已超过了处理间隔时间（在任务结束时，可能提供到数据达不到一个指次，所以用过期时间来判断）
			if ((ul.size() > batchSize)) {
				// 更新
				IDataHandler h = getHanlder(uo.getType());
				// h.handle(opt, ul);
				HandlerFuture hf = new HandlerFuture(opt, ul, h);
				ExecuteQueueManager.put2Doqueue(hf);
				// StockFactory.submitTaskBlocking(hf);
				if(ExecuteQueueManager.opencomputelog==1)
					logger.info("ExecuteQueueManager  iqueue size : "+q.size()+" ;had handle count : "+ai.get());
				_m.remove(key);

				// // 更新处理时间
				// if (opt.equals(StockConstants.OPT_INSERT))
				// lastestIHandleTime = now;
				// if (opt.equals(StockConstants.OPT_UPDATE))
				// lastestUHandleTime = now;

			}
		} catch (Exception e) {
			logger.error("ExecuteQueueManager", e);
		}
	}

	private IDataHandler getHanlder(Integer type) {
		// TODO Auto-generated method stub
		return ExecuteQueueManager._hm.get(type);
	}

}
