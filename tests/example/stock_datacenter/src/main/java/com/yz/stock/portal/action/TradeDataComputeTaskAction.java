package com.yz.stock.portal.action;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.CompileMode;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.TaskMsg;
import com.stock.common.model.USubject;
import com.stock.common.util.DateUtil;
import com.stock.common.util.NetUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.cache.DataLoadTimeMng;
import com.yfzx.service.cache.ExtCacheService;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.USubjectService;
import com.yz.stock.common.BaseAction;
import com.yz.stock.portal.manager.ComputeIndexManager;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.task.RuleTimerTask;
import com.yz.stock.portal.task.TaskEnter;
import com.yz.stock.portal.task.TaskSchedule;

public class TradeDataComputeTaskAction extends BaseAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9171952708481009032L;
	private TaskMsg tmsg = new TaskMsg();
	Logger log = LoggerFactory.getLogger(this.getClass());
	RuleTimerTask rtt = RuleTimerTask.getInstance();
	static boolean isloadData = true;

	public TaskMsg getTmsg() {
		return tmsg;
	}

	public void setTmsg(TaskMsg tmsg) {
		this.tmsg = tmsg;
	}

	

	private void initCompile(String useOldData) {
		if (!StringUtil.isEmpty(useOldData))
			CompileMode.setUseCacheExtData(Boolean.valueOf(useOldData));

	}

	

	/**
	 * 以公司分页计算
	 * 
	 * @return
	 */
	@Action(value = "/trade/computeAllIndexByPageCompany_all")
	public String computeAllIndexByPageCompany_all() {
		String ret = ERROR;
		try {
			if (isloadData) {
				ComputeIndexManager.getInstance().computeInit();
			}
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			int page = NetUtil.getParameterInt(getHttpServletRequest(), "page",
					-1);
			if (page < 0) {
				System.out.println("please set page!page=" + page);
				return ERROR;
			}
			initCompile(useOldData);
			int isadd = NetUtil.getParameterInt(getHttpServletRequest(), "isadd",
					1);
			Date stime = NetUtil.getParameterDate(getHttpServletRequest(), "stime",
					StockUtil.getNextTime(StockUtil.getApproPeriod(new Date()), -36));
			if(isadd==0)
			{
				CompileMode.isAddCompute = false;
				stime = DateUtil.format("1990-01-01");
			}
			else
			{
				CompileMode.isAddCompute = true;
			}
			int utype = NetUtil.getParameterInt(getHttpServletRequest(), "utype",
					0);
			
			List<USubject> usl = null;
			if(utype==0)
				usl = USubjectService.getInstance().getUSubjectListAStock();
			if(utype==1)
				usl = USubjectService.getInstance().getUSubjectListHStock();
			if(utype==2)
				usl = USubjectService.getInstance().getUSubjectListZStock();
//			RuleTimerTask.getInstance().setComputeCompanyList(CompanyService.getInstance().getCompanyList());
			computeCompanyDataByPage(useOldData,page,true,true,usl,stime,new Date());

			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}
	static Object lock = new Object();
	public void computeCompanyDataByPage(String useOldData, int page,boolean isasyn,boolean istradeindex,List<USubject> usl,Date stime,Date etime) {
		initCompile(useOldData);
		TaskEnter.getInstance().computeCompanyDataByPage( useOldData,  page, isasyn, istradeindex, usl, stime, etime);
		
		
	}

	public void computeCompanyDataByPage_notAysn(String useOldData, int page,List<USubject> usl,Date stime,Date etime) {
		initCompile(useOldData);
		CRuleService.getInstance().updateIndexRuleOfCompute();
		TaskEnter.aip.set(page);
		while(true)
		{
			//超过一分钟，认为此次任务完成，开始计算下一页
//			long settime = ConfigCenterFactory.getLong("stock_dc.compute_next_page_expTime", 60000l);//一分钟
//			if(Calendar.getInstance().getTimeInMillis()-StockFactory.expTime>settime)
//			{
				StockFactory.expTime = Calendar.getInstance().getTimeInMillis();
				int size = usl.size();
				if(TaskEnter.aip.get()*RuleTimerTask.pagesize<size)
				{
					ComputeIndexManager.computeIndustryRelatIndex = false;
					try {
						System.out
								.println("["+new Date()+"]**************************************cur page:"
										+ TaskEnter.aip.get() + "**************************");
						synchronized (lock) {
							log.info("========================execute computeAllIndexByPageCompany task start of ....=========================");
							RuleTimerTask.getInstance().computeAllIndexByPageCompany(TaskEnter.aip.get(),false,true,usl,stime,etime);
						}

					} catch (Exception e) {
						log.error("execute computeNewIndexByCompany task failed!", e);
					}
					TaskEnter.aip.incrementAndGet();
				}
				else
				{
					break;
				}
					
//			}
			try {
				Thread.sleep(10000);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		
	}


	/**
	 * 以公司分页计算
	 * 
	 * @return
	 */
	@Action(value = "/trade/computeHKBaseIndexByPageCompany_all")
	public String computeHKBaseIndexByPageCompany_all() {
		String ret = ERROR;
		try {
			if (isloadData) {
				ComputeIndexManager.getInstance().computeInit();
			}
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			int page = NetUtil.getParameterInt(getHttpServletRequest(), "page",
					-1);
			if (page < 0) {
				System.out.println("please set page!page=" + page);
				return ERROR;
			}
			initCompile(useOldData);
			int isadd = NetUtil.getParameterInt(getHttpServletRequest(), "isadd",
					1);
			Date stime = NetUtil.getParameterDate(getHttpServletRequest(), "stime",
					StockUtil.getNextTime(StockUtil.getApproPeriod(new Date()), -36));
			if(isadd==0)
			{
				CompileMode.isAddCompute = false;
				stime = DateUtil.format("1990-01-01");
			}
			else
			{
				CompileMode.isAddCompute = true;
			}
			
			List<USubject> usl  = USubjectService.getInstance().getUSubjectListHStock();
			initCompile(useOldData);
			TaskEnter.aip.set(page);
			while(true)
			{
				//超过一分钟，认为此次任务完成，开始计算下一页
//				long settime = ConfigCenterFactory.getLong("stock_dc.compute_next_page_expTime", 60000l);//一分钟
//				if(Calendar.getInstance().getTimeInMillis()-StockFactory.expTime>settime)
				if(!TaskSchedule.hasRunningTask())
				{
					StockFactory.expTime = Calendar.getInstance().getTimeInMillis();
					int size = usl.size();
					if(TaskEnter.aip.get()*RuleTimerTask.pagesize<size)
					{
						ComputeIndexManager.computeIndustryRelatIndex = false;
						try {
							System.out
									.println("["+new Date()+"]**************************************cur page:"
											+ TaskEnter.aip.get() + "**************************");
							synchronized (lock) {
								log.info("========================execute computeAllIndexByPageCompany task start of ....=========================");
								RuleTimerTask rtt = RuleTimerTask.getInstance();
								rtt.computeHKBaseIndexByPageCompany(TaskEnter.aip.get(),usl,stime,new Date());
							}

						} catch (Exception e) {
							log.error("execute computeNewIndexByCompany task failed!", e);
						}
						TaskEnter.aip.incrementAndGet();
					}
					else
					{
						break;
					}
						
				}
				try {
					Thread.sleep(10000);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}
	

	static boolean isfirst = true;
	static String ctag="";
	
	public void clearAllIndExtCache() {
		for (int i = 0; i < StockConstants.INDUSTRY_BASE_INDEX_CACHE_NUM; i++) {
			try {
				ExtCacheService.getInstance().clear(
						StockConstants.UEXT_CACHE_PREFIX 
								+ i);
			} catch (Exception e) {
				log.error("failed!", e);
			}
		}
	}

}
