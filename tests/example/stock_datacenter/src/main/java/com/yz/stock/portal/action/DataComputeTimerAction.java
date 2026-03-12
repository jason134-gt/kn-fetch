package com.yz.stock.portal.action;

import java.util.Calendar;
import java.util.Date;

import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.service.MailService;
import com.stock.common.util.DateUtil;
import com.stock.common.util.NetUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.SystemPropertiesService;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.CacheRefreshTimer;
import com.yz.stock.common.BaseAction;
import com.yz.stock.common.DcConst;
import com.yz.stock.portal.cache.Trade0001CacheLoadService;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.task.DataCenterTimer;

/**
 * 实时分析action
 * 
 * @author user
 * 
 */
public class DataComputeTimerAction extends BaseAction {

	Logger log = LoggerFactory.getLogger(this.getClass());
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	CompanyService cs = CompanyService.getInstance();
	DictService ds = DictService.getInstance();
	CRuleService crs = CRuleService.getInstance();
	

	@Action(value = "/datacomputetimer/compute1day")
	public String compute1day() {
		
		int brefore = NetUtil.getParameterInt(getHttpServletRequest(), "brefore",1);
		if(DcConst.timerstate.get()==0)
		{
			DcConst.timerstate.set(1);
			StockFactory.submitTaskBlocking(new DatacomputeRunnable(brefore));
		}
			
		return SUCCESS;
	}

	@Action(value = "/datacomputetimer/ftpTradeDataCheck")
	public String ftpTradeDataCheck() {
		boolean isSendMail = false;
		if (!Trade0001CacheLoadService.getInstance().checkTodayTradeDataLoad(DateUtil.getDayStartTime(new Date())))
		{
			if(!isSendMail)
			{
				MailService.getInstance().sendMail("前一天行情数据未找到!", "前一天行情数据未找到!");
				isSendMail = true;
			}
		}
			
		return SUCCESS;
	}
	
	@Action(value = "/datacomputetimer/datacomputecroncf")
	public String datacomputecroncf() {
		
		try {
			DataCenterTimer.getInstance().datacompute_cron_cf();
		} catch(StackOverflowError e){
            System.err.println("ouch!");
        }
		return SUCCESS;
	}
	
	@Action(value = "/datacomputetimer/datacomputecrontrade")
	public String datacomputecrontrade() {
		
		try {
			DataCenterTimer.getInstance().datacompute_cron_trade();
		} catch(StackOverflowError e){
            System.err.println("ouch!");
        }
		return SUCCESS;
	}
	
	
	@Action(value = "/datacomputetimer/datacomputeFromTime")
	public String datacomputeFromTime() {
		
		try {
			String uptime = NetUtil.getParameterString(getHttpServletRequest(), "uptime","");
			if(StringUtil.isEmpty(uptime))
				return ERROR;
			
			DataCenterTimer.getInstance().datacomputeFromTime(DateUtil.format(uptime));
		} catch(StackOverflowError e){
            System.err.println("ouch!");
        }
		return SUCCESS;
	}
	
	@Action(value = "/datacomputetimer/datacomputeCfFromTime")
	public String datacomputeCfFromTime() {
		
		try {
			String uptime = NetUtil.getParameterString(getHttpServletRequest(), "uptime","");
			if(StringUtil.isEmpty(uptime))
				return ERROR;
			
			DataCenterTimer.getInstance().datacomputeCfFromTime(DateUtil.format(uptime));
		} catch(StackOverflowError e){
            System.err.println("ouch!");
        }
		return SUCCESS;
	}
	
	@Action(value = "/datacomputetimer/datacomputeTradeFromTime")
	public String datacomputeTradeFromTime() {
		
		try {
			String uptime = NetUtil.getParameterString(getHttpServletRequest(), "uptime","");
			if(StringUtil.isEmpty(uptime))
				return ERROR;
			
			DataCenterTimer.getInstance().datacomputeTradeFromTime(DateUtil.format(uptime));
		} catch(StackOverflowError e){
            System.err.println("ouch!");
        }
		return SUCCESS;
	}
	
	@Action(value = "/datacomputetimer/refreshLc")
	public String refreshLc() {
		
		try {
			CacheRefreshTimer.getInstance().refreshLC_cron();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return SUCCESS;
	}
	
}
class DatacomputeRunnable implements Runnable{

	Logger log = LoggerFactory.getLogger(this.getClass());
	int brefore;
	public DatacomputeRunnable()
	{
		
	}
	public DatacomputeRunnable(int brefore)
	{
		this.brefore = brefore;
	}

	public void run() {
		try {
			Date cuptime = StockUtil.getNextTimeV3(new Date(), -brefore, Calendar.DATE);
			System.out.println("-----------------------start compute "+brefore+" day update data:"+DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, cuptime));
			cuptime = DateUtil.getDayStartTime(cuptime);
			DataCenterTimer.getInstance()
			.datacompute_auto(cuptime);
	
			Calendar nc = Calendar.getInstance();
			nc.setTimeInMillis(System.currentTimeMillis());
			//运算结束后，把运算时间写到库中
			SystemPropertiesService.getInstance().update("last_compute_date", DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, nc.getTime()));
			System.out.println("-----------------------end compute "+brefore+" day update data:"+DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, Calendar.getInstance().getTime()));
			DcConst.timerstate.set(0);
		} catch (Exception e) {
			log.error("execute /DatacomputeRunnable failed", e);
			return ;
		}
	}
	
	
}