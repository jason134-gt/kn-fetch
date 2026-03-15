package com.yz.stock.portal.action;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.CompileMode;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Dictionary;
import com.stock.common.model.TableSystem;
import com.stock.common.model.TaskMsg;
import com.stock.common.util.DateUtil;
import com.stock.common.util.DictUtil;
import com.stock.common.util.NetUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.cache.ExtCacheService;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.MatchinfoService;
import com.yfzx.service.db.TableSystemService;
import com.yz.configcenter.manager.ConfigCenter;
import com.yz.stock.common.BaseAction;
import com.yz.stock.portal.manager.ComputeIndexManager;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.task.MMMRAEnter;
import com.yz.stock.portal.task.RuleTimerTask;
import com.yz.stock.portal.task.TaskEnter;
import com.yz.stock.portal.task.TaskSchedule;

/**
 * 最大，最 小，平均，中值
 * 
 * @author Administrator
 * 
 */
public class IndustryDataComputeTaskAction extends BaseAction {

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
	 * 
	 * 全量计算均值，中值，最大，最小值
	 * 
	 * @return
	 */
	@Action(value = "/task/computeMaxMinAvg")
	public String computeMaxMinAvg() {
		String ret = ERROR;
		try {
			if (isloadData) {
				ComputeIndexManager.getInstance().computeInit();
			}
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			initCompile(useOldData);
			Date sTime = DateUtil.format("1990-6-30");
			Date eTime = StockUtil.getApproPeriod(new Date());
			TaskEnter.getInstance().computeMMMA(sTime, eTime, null, null);
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	/**
	 * 计算指定分类，指定指标的均值中值，最大，最小值
	 * 
	 * @return
	 */
	@Action(value = "/task/computeMaxMinAvgOneTagOneIndex")
	public String computeMaxMinAvgOneTagOneIndex() {
		String ret = ERROR;
		try {
			String tag = this.getHttpServletRequest().getParameter("tag");
			String indexcode = this.getHttpServletRequest().getParameter(
					"indexcode");
			if (isloadData) {
				ComputeIndexManager.getInstance().computeInit();
			}
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			initCompile(useOldData);
			Date sTime = DateUtil.format("1990-6-30");
			Date eTime = StockUtil.getApproPeriod(new Date());
			TaskEnter.getInstance().computeMMMA(sTime, eTime, indexcode, tag);
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	private void initCompile(String useOldData) {
		if (!StringUtil.isEmpty(useOldData))
			CompileMode.setUseCacheExtData(Boolean.valueOf(useOldData));

	}

	/**
	 * 计算指定时间段的MMMA
	 * 
	 * @return
	 */
	@Action(value = "/task/computeMaxMinAvgOnetime")
	public String computeMaxMinAvgOnetime() {
		String ret = ERROR;
		try {
			String sTime = this.getHttpServletRequest().getParameter("sTime");
			String eTime = this.getHttpServletRequest().getParameter("eTime");
			if (isloadData) {
				ComputeIndexManager.getInstance().computeInit();
			}
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			initCompile(useOldData);
			TaskEnter.getInstance().computeMMMA(DateUtil.format(sTime), DateUtil.format(eTime), null, null);
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	/**
	 * 
	 * 全量计算均值，中值，最大，最小值
	 * 
	 * @return
	 */
	@Action(value = "/task/computeRAvg")
	public String computeRAvg() {
		String ret = ERROR;
		try {
			if (isloadData) {
				ComputeIndexManager.getInstance().computeInit();
			}
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			initCompile(useOldData);
			Date stime = DateUtil.format("1990-6-30");
			Date etime = StockUtil.getApproPeriod(new Date());
			MMMRAEnter.getInstance().computeRAvg(null, stime, etime, null);
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	/**
	 * 计算指定分类，指定指标的均值中值，最大，最小值
	 * 
	 * @return
	 */
	@Action(value = "/task/computeRAvgOneTagOneIndex")
	public String computeRAvgOneTagOneIndex() {
		String ret = ERROR;
		try {
			String tag = this.getHttpServletRequest().getParameter("tag");
			String indexcode = this.getHttpServletRequest().getParameter(
					"indexcode");
			if (isloadData) {
				ComputeIndexManager.getInstance().computeInit();
			}
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			initCompile(useOldData);
			Date stime = DateUtil.format("1990-6-30");
			Date etime = StockUtil.getApproPeriod(new Date());
			MMMRAEnter.getInstance().computeRAvg(indexcode, stime, etime, tag);
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	/**
	 * 计算指定时间段的MMMA
	 * 
	 * @return
	 */
	@Action(value = "/task/computeRAvgOnetimeByPage")
	public String computeRAvgOnetimeByPage() {
		String ret = ERROR;
		try {
			final String sTime = this.getHttpServletRequest().getParameter(
					"sTime");
			final String eTime = this.getHttpServletRequest().getParameter(
					"eTime");
			int page = NetUtil.getParameterInt(getHttpServletRequest(), "page",
					-1);
			if (page < 0) {
				System.out.println("please set page!page=" + page);
				return ERROR;
			}
			ConfigCenter.getInstance().put("stock_zjs.ind_Latest_period_time", eTime);
			
			String useOldData = this.getHttpServletRequest().getParameter(
					"useOldData");
			doComputeRavgOnetimeByPage(DateUtil.format(sTime), DateUtil.format(eTime), page,useOldData);

			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	public void doComputeRavgOnetimeByPage(Date sTime, Date eTime, int page,String useOldData) {
		if (isloadData) {
			ComputeIndexManager.getInstance().computeInit();
		}
		CRuleService.getInstance().updateIndexRuleOfCompute();
		if (!StringUtil.isEmpty(useOldData))
			CompileMode.setUseCacheExtData(Boolean.valueOf(useOldData));
		int opage = page;//原始页标
		
		try {
			List<TableSystem> tsl = TableSystemService.getInstance()
					.getTableSystemListByDs("ds_0002");
			for (TableSystem ts : tsl) {
				List<Dictionary> ndl = new ArrayList<Dictionary>();
				List<Dictionary> dl = DictService.getInstance()
						.getAllDictionaryList();
				for (Dictionary d : dl) {
					if (DictUtil.needMMMA(d)) {
						String tsc = MatchinfoService.getInstance()
								.getTscByTableCode(d.getTableCode());
						if (!tsc.equals(ts.getTableSystemCode()))
							continue;
						ndl.add(d);
					}
				}

				List<String> taglist = ComputeIndexManager.getInstance()
						.getTagsByTscV2(ts.getTableSystemCode());
				if (taglist == null)
					continue;

				TaskEnter.aip.set(page);

				// 分页计算公司数据
				while (true) {
					if (!TaskSchedule.hasRunningTask()) {
						StockFactory.expTime = Calendar.getInstance()
								.getTimeInMillis();
						int size = taglist.size();
						if(TaskEnter.aip.get()*RuleTimerTask.pagesize<size)
						{
							clearAllIndExtCache();
							List<String> tlist = getTagListByPage(TaskEnter.aip.get(), taglist);
							log.info("**************************************cur page:" + TaskEnter.aip.get()
									+ "**************************");
							if(tlist!=null&&tlist.size()>0)
								MMMRAEnter.getInstance()
										.computeRAvgByPage(sTime, eTime,
												 tlist, ndl);
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
				
				page = opage;
			}
		} catch (Exception e) {
			log.error("failed!", e);
		}
		
		

	}
	private List<String> getTagListByPage(int page, List<String> cl) {

		int start = page * RuleTimerTask.pagesize;
		if (start > cl.size()) {
			System.out.println("=========page is out range!---");
			return null;
		}

		int end = (page + 1) * RuleTimerTask.pagesize;
		if (end > cl.size())
			end = cl.size();
		return cl.subList(start, end);
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
