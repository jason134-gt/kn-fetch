package com.yz.stock.portal.task;

import java.util.Date;

import com.yfzx.service.db.RankResultService;

public class computeRankTask implements Runnable {

	Integer rankcode ;
	Date time ;
	computeRankTask()
	{
		
	}
	public computeRankTask(Integer rankcode,Date time)
	{
		this.rankcode = rankcode;
		this.time = time;
	}
	public void run() {
		
		RankResultService.getInstance().computeOneRankresultAndSave(rankcode,time);

	}

}
