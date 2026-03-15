package com.yfzx.service.msgpush.asyntask;

import com.stock.common.constants.AsyncTaskConstants;
import com.stock.common.util.StockUtil;
import com.yfzx.service.client.UserServiceClient;

public class AsyncTaskService {
	private static AsyncTaskService instance = new AsyncTaskService();
	private AsyncTaskService() {}
	public static AsyncTaskService getInstance() {
		return instance;
	}

	public void saveTopicsEs(String id, int type) {
		try {
			UserServiceClient.getInstance().saveAsyncTasks(AsyncTaskConstants.TOPICS_ASYNC_TASK_KEY, StockUtil.joinString("^", id, type));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
