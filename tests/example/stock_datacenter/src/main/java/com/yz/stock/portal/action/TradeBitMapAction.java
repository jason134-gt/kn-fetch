package com.yz.stock.portal.action;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.USubject;
import com.stock.common.util.DateUtil;
import com.stock.common.util.NetUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexCacheService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.trade.Trade0001Service;
import com.yfzx.service.trade.TradeBitMapService;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.stock.common.BaseAction;

/**
 * 实时分析action
 * 
 * @author user
 * 
 */
public class TradeBitMapAction extends BaseAction {

	Logger log = LoggerFactory.getLogger(this.getClass());
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	CompanyService cs = CompanyService.getInstance();
	DictService ds = DictService.getInstance();
	CRuleService crs = CRuleService.getInstance();

	@Action(value = "/tradebitmap/initTradeBitMap")
	public String initTradeBitMap() {
		try {
			int type = NetUtil.getParameterInt(getHttpServletRequest(), "type",1);
			log.info("start initBitMap ...type="+type);
			TradeBitMapService.getInstance().initBitMap(type);
			IndexCacheService.getInstance().refreshAndFlush2Disk("tradedatamap",false);
			log.info("complete initBitMap ...");
		} catch (Exception e) {
			log.error("execute /initTradeBitMap failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	@Action(value = "/tradebitmap/checkTradeBitMap")
	public String checkTradeBitMap() {
		try {
			String time = NetUtil.getParameterString(getHttpServletRequest(), "stime");
			int type = NetUtil.getParameterInt(getHttpServletRequest(), "type",0);
			Date stime = null;
			if(!StringUtil.isEmpty(time))
			{
				stime = DateUtil.format(time);
			}
			TradeBitMapService.getInstance().setMissCount(0);
			log.info("start checkBitMap ...");
			TradeBitMapService.getInstance().checkBitMap(stime);
			
			IndexCacheService.getInstance().refreshAndFlush2Disk("tradedatamap",false);
			log.info("complete checkBitMap ...");
			if(type==1)
			{
				log.info("start checkTradeData ...");
				TradeBitMapService.getInstance().checkTradeData(stime);
				
				IndexCacheService.getInstance().refreshAndFlush2Disk("trade0001",false);
				IndexCacheService.getInstance().refreshAndFlush2Disk("uext",false);
				log.info("complete checkTradeData ...");
			}
		} catch (Exception e) {
			log.error("execute /initTradeBitMap failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/tradebitmap/loadTradeUextDataFromTime")
	public String loadTradeUextDataFromTime() {
		try {
			String time = NetUtil.getParameterString(getHttpServletRequest(), "stime");
			if(StringUtil.isEmpty(time))
				return ERROR;
			log.info("load miss tradedata,time ="
					+ time);
			Date stime = DateUtil.format(time);
			IndexCacheService.getInstance().refreshAndFlush2Disk(stime);
			log.info("load miss tradedata end,time ="
					+ time);
		} catch (Exception e) {
			log.error("execute /initTradeBitMap failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	

}
