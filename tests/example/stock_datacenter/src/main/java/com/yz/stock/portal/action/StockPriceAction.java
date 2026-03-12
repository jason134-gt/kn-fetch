package com.yz.stock.portal.action;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.model.Dictionary;
import com.stock.common.model.trade.StockTrade;
import com.stock.common.util.DateUtil;
import com.stock.common.util.NetUtil;
import com.stock.common.util.StockUtil;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.stock.common.BaseAction;
import com.yz.stock.portal.manager.ComputeIndexManager;
import com.yz.stock.portal.task.TaskEnter;

/**
 * 实时分析action
 * 
 * @author user
 * 
 */
public class StockPriceAction extends BaseAction {

	Logger log = LoggerFactory.getLogger(this.getClass());
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	CompanyService cs = CompanyService.getInstance();
	DictService ds = DictService.getInstance();
	CRuleService crs = CRuleService.getInstance();

	@Action(value = "/stockprice/initStockPrice")
	public String initStockPrice() {

		String ret = ERROR;
		try {
			Dictionary d = DictService.getInstance().getDataDictionary("2290");
			ComputeIndexManager.getInstance().loadOneIndexAllExtData(d);
			ComputeIndexManager.getInstance().computeInit();
			Date detime = StockUtil.getApproPeriod(new Date());
			Date dstime = StockUtil.getNextTimeV3(detime, -6,Calendar.MONTH);
			Date stime = NetUtil.getParameterDate(getHttpServletRequest(),"stime", dstime);
			Date etime = NetUtil.getParameterDate(getHttpServletRequest(),"etime",detime);
//			TaskEnter.getInstance().loadStockPrice2ExtTable(stime,etime);
			TaskEnter.getInstance().loadFund2ExtTable(stime);
			TaskEnter.getInstance().loadCGSExtTable(stime);
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	/**
	 * 测试接口，获取最新的行情，用于比较跟zjs的行情时间差
	 * @return
	 */
	@Action(value = "/mobile/testNew")
	public String testNew() {
		StringBuffer sbuf = new StringBuffer();
		String companycode = this.getHttpServletRequest().getParameter(
				"companycode");
		String key = getStockTradeKey(companycode);
		StockTrade st = LCEnter.getInstance().get(key,
				SCache.CACHE_NAME_marketcache);
		String dateStr = DateUtil.format2String(new Date(Long.valueOf(st.getUptime())));
		String volume =  new BigDecimal(st.getCjl()).stripTrailingZeros().toPlainString();
		String cje =  new BigDecimal(st.getCje()).stripTrailingZeros().toPlainString();
		sbuf.append(dateStr).append(",").append(st.getC()).append(",").append(st.getJk()).append(",").append(st.getH()).append(",").append(st.getL()).append(",")
		.append(st.getZs()).append(",").append(volume).append(",").append(cje).append("\n");
		StockUtil.outputJson(getHttpServletResponse(),sbuf.toString() );
		return SUCCESS;
	}
	
	private static String getStockTradeKey(String companyCode) {		
		return "st." + companyCode;
	}

}
