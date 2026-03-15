package com.yz.stock.portal.action;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.CompileMode;
import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.StockException;
import com.stock.common.model.Company;
import com.stock.common.model.Industry;
import com.stock.common.model.TaskMsg;
import com.stock.common.model.USubject;
import com.stock.common.model.company.Stock0001;
import com.stock.common.util.DateUtil;
import com.stock.common.util.NetUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.stock.common.util.TreeNode;
import com.yfzx.service.cache.ExtCacheService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.IndustryService;
import com.yfzx.service.db.Stock0001Service;
import com.yfzx.service.db.USubjectService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.stock.common.BaseAction;
import com.yz.stock.portal.cache.TUExtCacheLoadService;
import com.yz.stock.portal.manager.ComputeIndexManager;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.task.RuleTimerTask;
import com.yz.stock.portal.task.TaskEnter;

public class CompanyDataComputeTaskAction extends BaseAction {

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

	/**
	 * 计算指标
	 * 
	 * 计算某一批公司，在各报表体系下，所有的指标
	 * 
	 * @return
	 */
	@Action(value = "/task/computeIndex")
	public String computeIndex() {
		try {
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			initCompile(useOldData);
			int isadd = NetUtil.getParameterInt(getHttpServletRequest(), "isadd",
					1);
			if(isadd==0)
			{
				CompileMode.isAddCompute = false;
			}
			else
			{
				CompileMode.isAddCompute = true;
			}
			ComputeIndexManager.getInstance().computeInit();
			// 取所有报表体系相关的指标
			LCEnter lcEnter = LCEnter.getInstance();
			List<String> tsl = lcEnter.get(
					StockConstants.TABLE_SYSTEM_LIST_CACHE_KEY,
					StockConstants.MATCH_INFO_CACHE);
			if (tsl == null) {
				log.error(
						"load table system code list failed!",
						new StockException(
								Integer.valueOf(StockCodes.LOAD_TABLE_SYSTEM_CODE_ERROR),
								"load table system code list failed"));
				this.setErrorReason("load table system code list failed");
				return ERROR;
			}
			Date stime = StockUtil.getNextTime(StockUtil.getApproPeriod(new Date()), -36);
			Date etime = new Date();
			for (String tsc : tsl) {
				doComputeIndex(tsc,stime,etime);
			}
		} catch (Exception e) {
			log.error("compute index failed!", e);
			return ERROR;
		}
		return SUCCESS;
	}

	private String doComputeIndex(String tsc,Date stime,Date etime) {
		// TODO Auto-generated method stub
		String uType = tmsg.getUpdateType();
		String ret = ERROR;
		// 转化时间为临近值
		if (StringUtil.isEmpty(tmsg.getStartTime())
				|| StringUtil.isEmpty(tmsg.getEndTime())) {
			this.setErrorReason("时间不能为空!");
			return ERROR;
		}
		// String st = tmsg.getStartTime();
		// String et = tmsg.getEndTime();
		try {
			if (uType.equals(StockConstants.PART_UPDATE)) {
				String[] cCodeArray = tmsg.getCompanyInfo().split("\\|");
				for (String cCode : cCodeArray) {
					// String cCode =
					// StockUtil.getCompanyCode(tmsg.getCompanyInfo());
					if (cCode == null) {
						this.setErrorReason("公司编码不能为空!");
						return ERROR;
					}
					Company c = CompanyService.getInstance().getCompanyByCodeFromCache(
							cCode);
					rtt.computeOneCompanyAllIndex(c, tmsg.getInterval(), tsc,stime,etime);

				}

			}
			// if (uType.equals(StockConstants.ALL_UPDATE)) {
			// rtt.updateAll(st, et, tmsg.getInterval(),tsc);
			// }
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	/**
	 * 
	 * 计算指定公司的指定指标
	 * 
	 * @return
	 */
	@Action(value = "/task/computeOneIndexOfCompany")
	public String computeOneIndexOfCompany() {
		try {
			ComputeIndexManager.getInstance().computeInit();
			String indexcode = this.getHttpServletRequest().getParameter(
					"indexcode");
			String companycode = this.getHttpServletRequest().getParameter(
					"companycode");
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			initCompile(useOldData);
			int isadd = NetUtil.getParameterInt(getHttpServletRequest(), "isadd",
					1);
			if(isadd==0)
			{
				CompileMode.isAddCompute = false;
			}
			else
			{
				CompileMode.isAddCompute = true;
			}
			USubject c = USubjectService.getInstance().getUSubjectByUIdentifyFromCache(companycode);
			rtt.computeOneCompanyOneIndex(c, indexcode);

		} catch (Exception e) {
			log.error("compute index failed!", e);
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/task/computeOneIndex")
	public String computeOneIndex(){
		ComputeIndexManager.getInstance().computeInit();
		
		String indexcode = this.getHttpServletRequest().getParameter(
				"indexcode");			
		String useOldData = this.getHttpServletRequest().getParameter(
				"useOldData");
		initCompile(useOldData);
		int isadd = NetUtil.getParameterInt(getHttpServletRequest(), "isadd",
				1);
		if(isadd==0)
		{
			CompileMode.isAddCompute = false;
		}
		else
		{
			CompileMode.isAddCompute = true;
		}
		List<USubject> cList = USubjectService.getInstance().getUSubjectAHZList();
		for(USubject c : cList){
			try {
				rtt.computeOneCompanyOneIndex(c, indexcode);
				log.info("start compute company index ;companycode="+c.getUidentify()+",indexcode="+indexcode);
			} catch (Exception e) {
				log.error("compute index failed!", e);			
			}
		}
		return SUCCESS;
	}

	private void initCompile(String useOldData) {
		if (!StringUtil.isEmpty(useOldData))
			CompileMode.setUseCacheExtData(Boolean.valueOf(useOldData));
		
		List<USubject> ausl = USubjectService.getInstance().getUSubjectListAStock();
		List<USubject> husl = USubjectService.getInstance().getUSubjectListHStock();
		ausl.addAll(husl);
		for (USubject us : ausl) {
			Stock0001 s = Stock0001Service
					.getInstance()
					.getStock0001ByCompanycodeFromCache(us.getUidentify());
			if (s == null)
				continue;
			USubject cus = USubjectService.getInstance()
					.getUSubjectByUIdentifyFromCache(us.getUidentify());
			if (cus != null) {
				cus.removeAttr("mintime");
			}
		}
		

	}

	/*
	 * 增量计算----只计算数据有更新的公司
	 */
	@Action(value = "/task/computeDataUpdateCompany")
	public String computeDataUpdateCompany() {
		String ret = ERROR;
		try {
			if (isloadData) {
				ComputeIndexManager.getInstance().computeInit();
			}
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			initCompile(useOldData);
			int isadd = NetUtil.getParameterInt(getHttpServletRequest(), "isadd",
					1);
			if(isadd==0)
			{
				CompileMode.isAddCompute = false;
			}
			else
			{
				CompileMode.isAddCompute = true;
			}
			Date stime = StockUtil.getNextTime(StockUtil.getApproPeriod(new Date()), -36);
			Date etime = new Date();
			Date uptime = StockUtil.getNextTimeV3(new Date(), -2, Calendar.DATE);
			uptime = DateUtil.getDayStartTime(uptime); 
			TaskEnter.getInstance().computeDataUpdateCompany(uptime,stime,etime);
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	/**
	 * 计算指标--增量
	 * 
	 * @return
	 */
	@Action(value = "/task/computeIndexNew")
	public String computeIndexNew() {
		String ret = ERROR;
		try {
			if (isloadData) {
				ComputeIndexManager.getInstance().computeInit();
			}
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			initCompile(useOldData);
			int isadd = NetUtil.getParameterInt(getHttpServletRequest(), "isadd",
					1);
			if(isadd==0)
			{
				CompileMode.isAddCompute = false;
			}
			else
			{
				CompileMode.isAddCompute = true;
			}
			Date stime = StockUtil.getNextTime(StockUtil.getApproPeriod(new Date()), -36);
			Date etime = new Date();
			TaskEnter.getInstance().executeTaskOfAdd(false,stime,etime);
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	/**
	 * 计算指标--增量 按每个公司来顺序计算指标
	 * 
	 * @return
	 */
	@Action(value = "/task/computeIndexNewByCompany")
	public String computeNewIndexByCompany() {
		String ret = ERROR;
		try {
			if (isloadData) {
				ComputeIndexManager.getInstance().computeInit();
			}
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			initCompile(useOldData);
			Date stime = StockUtil.getNextTime(StockUtil.getApproPeriod(new Date()), -36);
			Date etime = new Date();
			TaskEnter.getInstance().computeNewIndexByCompany(false,stime,etime);
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	/**
	 * 以公司分页计算
	 * 
	 * @return
	 */
	@Action(value = "/task/computeAllIndexByPageCompany")
	public String computeAllIndexByPageCompany() {
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
			if(isadd==0)
			{
				CompileMode.isAddCompute = false;
			}
			else
			{
				CompileMode.isAddCompute = true;
			}
			Date stime = StockUtil.getNextTime(StockUtil.getApproPeriod(new Date()), -36);
			Date etime = new Date();
			List<USubject> usl = USubjectService.getInstance().getUSubjectAHZList();
			ComputeIndexManager.computeIndustryRelatIndex = false;
			TaskEnter.getInstance().computeAllIndexByPageCompany(page,true,false,usl,stime,etime);

			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	/**
	 * 以公司分页计算
	 * 
	 * @return
	 */
	@Action(value = "/task/computeAllIndexByPageCompany_all")
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
//			int utype = NetUtil.getParameterInt(getHttpServletRequest(), "utype",
//					0);
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
			
			List<USubject> usl = USubjectService.getInstance().getUSubjectListAStock();
//			if(utype==0)
//				usl = USubjectService.getInstance().getUSubjectListAStock();
//			if(utype==1)
//				usl = USubjectService.getInstance().getUSubjectListHStock();
//			if(utype==3)
//				usl = USubjectService.getInstance().getUSubjectListZStock();
//			computeCompanyDataByPage(useOldData,page,true,false,usl,stime,new Date());
			initCompile(useOldData);
			TaskEnter.getInstance().computeCompanyDataByPage(useOldData,page,true,false,usl,stime,new Date());
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	
	/**
	 * 计算指标--全量 按每个公司来顺序计算指标
	 * 
	 * @return
	 */
	@Action(value = "/task/computeAllIndexByCompany")
	public String computeAllIndexByCompany() {
		String ret = ERROR;
		try {
			if (isloadData) {
				ComputeIndexManager.getInstance().computeInit();
			}
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			int isadd = NetUtil.getParameterInt(getHttpServletRequest(), "isadd",
					1);
			if(isadd==0)
			{
				CompileMode.isAddCompute = false;
			}
			else
			{
				CompileMode.isAddCompute = true;
			}
			initCompile(useOldData);
			Date stime = StockUtil.getNextTime(StockUtil.getApproPeriod(new Date()), -36);
			Date etime = new Date();
			TaskEnter.getInstance().computeAllIndexByCompany(stime,etime);
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	/**
	 * 计算指标--全量
	 * 
	 * @return
	 */
	@Action(value = "/task/computeIndexAll")
	public String computeIndexAll() {
		String ret = ERROR;
		try {
			if (isloadData) {
				ComputeIndexManager.getInstance().computeInit();
			}
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			initCompile(useOldData);
			TaskEnter.getInstance().executeTaskOfAll();
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	@Action(value = "/task/clearNoDcSet")
	public String clearNoDcSet() {
		String ret = ERROR;
		try {
			// 清除没有数据公司集合
			RuleTimerTask.clearNoDcSet();
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	@Action(value = "/task/setLoadData")
	public String setLoadData() {
		String ret = ERROR;
		try {
			String ldata = this.getHttpServletRequest()
					.getParameter("loadData");
			if (!StringUtil.isEmpty(ldata) && "true".equals(ldata.trim())) {
				isloadData = true;
			} else {
				isloadData = true;
			}

			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	/**
	 * 计算一段时间内数据有更新的公司的指标
	 * 
	 * @return
	 */
	@Action(value = "/task/computeIndexOfCompanyDataUpdate")
	public String computeIndexOfCompanyDataUpdate() {
		String ret = ERROR;
		try {
			String puptime = this.getHttpServletRequest().getParameter("upTime");
			String sendmail = this.getHttpServletRequest().getParameter(
					"sendmail");
			Date uptime = DateUtil.format(puptime);
			// 把最新更新的数据导入正式库
			TaskEnter.getInstance().importNewData2NormalDb(uptime);
			if (isloadData) {
				ComputeIndexManager.getInstance().computeInit();
			}
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			initCompile(useOldData);
			int isadd = NetUtil.getParameterInt(getHttpServletRequest(), "isadd",
					1);
			if(isadd==0)
			{
				CompileMode.isAddCompute = false;
			}
			else
			{
				CompileMode.isAddCompute = true;
			}
//			TaskEnter.getInstance().computeIndexOfCompanyDataUpdate(uptime);
//			RuleTimerTask.getInstance().setComputeCompanyList(CompanyService.getInstance().getDataUpCompanyList(uptime));
			List<USubject> usl = USubjectService.getInstance().getDataUpUSubjectList(uptime);
			//重建公司最新季报时间
			CompanyService.getInstance().rebuildMaxMinTimeCache();
			Date stime = StockUtil.getNextTime(StockUtil.getApproPeriod(new Date()), -36);
			Date etime = new Date();
//			computeCompanyDataByPage(useOldData,0,true,false,usl,stime,etime);
			initCompile(useOldData);
			TaskEnter.getInstance().computeCompanyDataByPage(useOldData,0,true,false,usl,stime,etime);
			if (!StringUtil.isEmpty(sendmail) && "1".equals(sendmail)) {
				TaskEnter.getInstance().sendMail(uptime);
			}
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	/**
	 * 分页计算与行业相关的指标
	 * 
	 * @return
	 */
//	@Action(value = "/task/computeIndIndexByTag")
//	public String computeIndIndexByTag() {
//		String ret = ERROR;
//		try {
//			if (isloadData) {
//				ComputeIndexManager.getInstance().computeInit();
//			}
//			String useOldData = this.getHttpServletRequest().getParameter(
//					"useOldData");
//			String tag = NetUtil.getParameterString(getHttpServletRequest(),
//					"tag", "-1");
//			if ("-1".equals(tag) || StringUtil.isEmpty(tag)) {
//				System.out.println("parameter error!");
//				return ERROR;
//			}
//			initCompile(useOldData);
//			ComputeIndexManager.computeIndustryRelatIndex = true;
//
//			clearAllIndExtCache();
//			String stime = NetUtil.getParameterString(getHttpServletRequest(),
//					"stime", "1990-06-30");
//			String etime = NetUtil.getParameterString(getHttpServletRequest(),
//					"etime", "2013-06-30");
//			IndExtIndexCacheLoadService indload = new IndExtIndexCacheLoadService();
//			// 取出父行业
//			TreeNode pind = IndustryService.getInstance()
//					.getIndustryYFZXTreeFromCache(1).get(tag);
//			if (pind != null) {
//				// 取所有父行业的子行业
//				List<Industry> il = pind.getLeafChildrenIndustry();
//				if (il != null && il.size() > 0) {
//					for (Industry ind : il) {
//						// 加载所有子行业的数据
//						indload.doLoadOneIndustryData2CacheByTime(
//								ind.getName(), "indextindex", stime, etime);
//					}
//				} 
//					// 如果没有子行业，则加载本行业数据
//				indload.doLoadOneIndustryData2CacheByTime(tag,
//							"indextindex", stime, etime);
//				
//			}
//			RuleTimerTask.getInstance().computeIndIndexByTag(tag);
//
//			ret = SUCCESS;
//		} catch (Exception e) {
//			// TODO: handle exception
//			log.error("compute index failed!", e);
//		}
//		return ret;
//	}

	static boolean isfirst = true;
	static String ctag="";
	/**
	 * 分页计算与行业相关的指标
	 * 备注，行业指标应在行业数据计算完成，再算
	 * 
	 * @return
	 */
	@Action(value = "/task/computeIndIndexByTag_all")
	public String computeIndIndexByTag_all() {
		String ret = ERROR;
		try {
			if (isloadData) {
				ComputeIndexManager.getInstance().computeInit();
			}
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			 ctag = NetUtil.getParameterString(getHttpServletRequest(),
					"tag", "-1");
			if ("-1".equals(ctag) || StringUtil.isEmpty(ctag)) {
				System.out.println("parameter error!");
				return ERROR;
			}
			initCompile(useOldData);
			int isadd = NetUtil.getParameterInt(getHttpServletRequest(), "isadd",
					1);
			if(isadd==0)
			{
				CompileMode.isAddCompute = false;
			}
			else
			{
				CompileMode.isAddCompute = true;
			}
			Thread t1 = new Thread(new Runnable(){


				public void run()  {
					TreeNode rind = IndustryService.getInstance()
							.getIndustryYFZXTreeFromCache(1).get(ctag);
					List<TreeNode> tnl = rind.getChildren();
					int index =0;
					//分行业计算行业相关指标数据
					while(true)
					{
						//超过一分钟，认为此次任务完成，开始计算下一页
						long settime = ConfigCenterFactory.getLong("stock_dc.compute_next_page_expTime", 60000l);//一分钟
						if(Calendar.getInstance().getTimeInMillis()-StockFactory.expTime>settime)
						{
							StockFactory.expTime = Calendar.getInstance().getTimeInMillis();
							if(index<tnl.size())
							{
								TreeNode tn = tnl.get(index);
								if(tn==null) 
									{
										index++;
										continue;
									}
								Industry ind = (Industry) tn.getReference();
								computeIndRelativeIndexs(ind.getName());
								index++;
							}
							else
								break;	
						}
						try {
							Thread.sleep(10000);
						} catch (Exception e) {
							// TODO: handle exception
						}
					}
					
				}

				private void computeIndRelativeIndexs(String tag) {
					ComputeIndexManager.computeIndustryRelatIndex = true;
					
					clearAllIndExtCache();
					Date stime = StockUtil.getNextTime(StockUtil.getApproPeriod(new Date()), -36);
					Date etime = new Date();
					TUExtCacheLoadService indload = new TUExtCacheLoadService();
					TreeNode ptn = IndustryService.getInstance()
							.getIndustryYFZXTreeFromCache(1).get(tag);
					if (ptn == null) return;
					Industry pind = (Industry) ptn.getReference();
					if (ptn != null) {
						// 取所有父行业的子行业
						List<Industry> il = ptn.getLeafChildrenIndustry();
						if (il != null && il.size() > 0) {
							for (Industry ind : il) {
								// 加载所有子行业的数据
								indload.doLoadOneIndustryData2CacheByTime(ind.getName(), "indextindex", DateUtil.format2String(stime), DateUtil.format2String(etime));
							}
						} 
							// 如果没有子行业，则加载本行业数据
						indload.doLoadOneIndustryData2CacheByTime( ((Industry)ptn.getReference()).getName(),
									"indextindex", DateUtil.format2String(stime), DateUtil.format2String(etime));
					}
					RuleTimerTask.getInstance().computeIndIndexByTag(pind.getName(),false,stime,etime);
					}
					
				
			});
			t1.setName("compute-ind-thread");
			t1.start();
			

			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}
	
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
