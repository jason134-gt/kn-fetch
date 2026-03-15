/**
 *
 */
package com.yfzx.service.db;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.Index4ScreenResult;
import com.stock.common.util.StockUtil;
import com.yfzx.service.trade.TradeCenter;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;


/**
 * @author 唐斌
 */
public class SelectStockService {

	private static SelectStockService index4ScreenService = new SelectStockService();
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	DictService ds = DictService.getInstance();
	Logger log = LoggerFactory.getLogger(this.getClass());
	private static final String FIELD_SEP = "|";
	private SelectStockService(){

	}

	public static SelectStockService  getInstance() {
		return index4ScreenService;
	}

	@SuppressWarnings("rawtypes")
	public List getSelectStockResultByPage(Map<String,Object> req) {
		List value = null;
		try {
//			RequestMessage reqMsg = DAFFactory.buildRequest(StockSqlKey.base_index_key_query_0, req,StockConstants.INDEX_DATA_TYPE);
			RequestMessage reqMsg = DAFFactory.buildRequest("selectstock_datalist", req,StockConstants.INDEX_DATA_TYPE);
			value = pLayerEnter.queryForList(reqMsg);
			if (value == null) {
				return null;
			}
		} catch (Exception e) {
			log.error("operator failed!", e);
		}
		return value;
	}


	/**
	 * @param companyArray 从数据库查询的结果 动态List<Map>型
	 * @param set 显示项
	 * @return
	 */
	public static ArrayList<Index4ScreenResult> dbToRetDataResult(List selectStockResult,LinkedHashSet<String> set){
		ArrayList<Index4ScreenResult> i4srArr = new ArrayList<Index4ScreenResult>();

		if(selectStockResult != null){
			for(int i=0;i<selectStockResult.size();i++){
				Map companyMap = (Map)selectStockResult.get(i);
				Index4ScreenResult i4sr = new Index4ScreenResult();
				i4sr.setStockCode(String.valueOf(companyMap.get("STOCK_CODE")));
				i4sr.setCompanyName(String.valueOf(companyMap.get("COMPANY_NAME")));
				ArrayList<Object> valueArr = new ArrayList<Object>();
				for(String indexKey :set){
					Dictionary d = DictService.getInstance().getDataDictionary(indexKey);
					String columnName = d.getColumnName();
					if(!StockUtil.isBaseIndex(d.getType()))
						columnName = "index_"+d.getIndexCode();
					valueArr.add(companyMap.get(columnName));
				}
				i4sr.setValueArr(valueArr);
				i4srArr.add(i4sr);
			}
		}
		return i4srArr;
	}


	public Integer getSelectStockResultCount(Map<String, Object> req) {
		Object value = null;
		try {
			RequestMessage reqMsg = DAFFactory.buildRequest("selectstock_datacount", req,StockConstants.INDEX_DATA_TYPE);
			value = pLayerEnter.queryForObject(reqMsg);
			if (value == null) {
				return null;
			}
		} catch (Exception e) {
			log.error("operator failed!", e);
		}
		return (Integer) value;
	}

	public String getStockInfo(String stockcode, List<Long> tl) {
		Company c = CompanyService.getInstance().getCompanyByCode(stockcode);
		if(c == null) {
			return null;
		}

		Long localStockMaxUpdateTime = tl.get(0);
		int tradeStatus = TradeCenter.getInstance().getStockTradeStatus(StockUtil.checkStockcode(stockcode));
		Long serverUpdateTime = c.getAttr("_priceUptime") == null ? -1 : (Long)c.getAttr("_priceUptime");
		if(localStockMaxUpdateTime >= serverUpdateTime && tradeStatus == 0) {//休市
			return null;
		}

		Long stockMaxUpdateTime = tl.get(1);
		if(stockMaxUpdateTime < serverUpdateTime) {
			tl.remove(1);
			tl.add(1, serverUpdateTime);
		}
		if(serverUpdateTime <= localStockMaxUpdateTime && localStockMaxUpdateTime > 0) {
			return null;
		}

		StringBuffer str = new StringBuffer();
		str.append(c.getCompanyCode()).append(FIELD_SEP);
		str.append(c.getSimpileName()).append(FIELD_SEP);

		boolean isStop = CompanyService.getInstance().isStop(c);
		Integer jiheSwitch = ConfigCenterFactory.getInt("stock_zjs.jiheStockPriceSwitch", 1);
		if(jiheSwitch == 1) {
			if(isStop && isJihePriceTime(stockcode)) {
				String zde = "";
				String zdf = "";
				String price = "";
				List<String> list = StockUtil.getCompanyArgs(c);
				if(list != null) {
					price = (String)list.get(0);
					zde = (String)list.get(1);
					zdf = (String)list.get(2);
				}
				try {
					log.info("jihePrice: " + price + "    " + stockcode + "   " + c.getZs() + "    " + c.getZF() + "   " + c.getSD());
				} catch (Exception e) {
					e.printStackTrace();
				}
				if(c.getC() != null && c.getC() != 0 || c.getZs() != null && c.getZs() != 0) {
					Double p = 0d;
					if(c.getC() != null && c.getC() != 0) {
						p = c.getC();
					} else if(c.getZs() != null && c.getZs() != 0) {
						p = c.getZs();
					}
					str.append(0).append(FIELD_SEP);
					str.append(p).append(FIELD_SEP);
					str.append(zde).append(FIELD_SEP);
					str.append(zdf).append(FIELD_SEP);
					str.append(c.getCjl()).append(FIELD_SEP);
					str.append(serverUpdateTime).append(FIELD_SEP);
					str.append(tradeStatus);
					return str.toString();
				}
			}
		}

		Integer state = 0;//0:正常 1:停牌   2:退市
		if(isStop) {
			state = 1;//停牌
		}
		String leaveStocks = ConfigCenterFactory.getString("stock_zjs.delisting_stocks", "");
		if(leaveStocks.contains(c.getCompanyCode())) {
			state = 2;//退市
		}
		str.append(state).append(FIELD_SEP);
		String zde = "";
		String zdf = "";
		String price = "";
		if(state == 0) {
			List<String> list = StockUtil.getCompanyArgs(c);
			if(list != null) {
				price = (String)list.get(0);
				zde = (String)list.get(1);
				zdf = (String)list.get(2);
			}
		}
		str.append(price).append(FIELD_SEP);
		str.append(zde).append(FIELD_SEP);
		str.append(zdf).append(FIELD_SEP);
		str.append(c.getCjl()).append(FIELD_SEP);
		str.append(serverUpdateTime).append(FIELD_SEP);
		str.append(tradeStatus);

		return str.toString();
	}
	//为手机提供(逻辑和getStockInfo()一样) 不需要判断开休市
	public String getStockInfoForMobile(String stockcode, List<Long> tl) {
		int tradeStatus = TradeCenter.getInstance().getStockTradeStatus(StockUtil.checkStockcode(stockcode));
		Long localStockMaxUpdateTime = tl.get(0);
		/*if(tradeStatus == 0 && localStockMaxUpdateTime != 0) {//休市
			return null;
		}
		 */
		Long stockMaxUpdateTime = tl.get(1);

		Company c = CompanyService.getInstance().getCompanyByCode(stockcode);
		if(c == null) {
			return null;
		}
		Long serverUpdateTime = c.getAttr("_priceUptime") == null ? -1 : (Long)c.getAttr("_priceUptime");
		if(stockMaxUpdateTime < serverUpdateTime) {
			tl.remove(1);
			tl.add(1, serverUpdateTime);
		}
		if(serverUpdateTime <= localStockMaxUpdateTime && localStockMaxUpdateTime > 0) {
			return null;
		}

		StringBuffer str = new StringBuffer();
		str.append(c.getCompanyCode()).append(FIELD_SEP);
		str.append(c.getSimpileName()).append(FIELD_SEP);

		boolean isStop = CompanyService.getInstance().isStop(c);
		Integer state = 0;//0:正常 1:停牌   2:退市
		if(isStop) {
			state = 1;//停牌
		}
		String leaveStocks = ConfigCenterFactory.getString("stock_zjs.delisting_stocks", "");
		if(leaveStocks.contains(c.getCompanyCode())) {
			state = 2;//退市
		}
		str.append(state).append(FIELD_SEP);
		String zde = "";
		String zdf = "";
		String price = "";
		if(state == 0) {
			List<String> list = StockUtil.getCompanyArgs(c);
			if(list != null) {
				price = (String)list.get(0);
				zde = (String)list.get(1);
				zdf = (String)list.get(2);
			}
		}
		str.append(price).append(FIELD_SEP);
		str.append(zde).append(FIELD_SEP);
		str.append(zdf).append(FIELD_SEP);
		str.append(c.getCjl()).append(FIELD_SEP);
		str.append(serverUpdateTime).append(FIELD_SEP);
		str.append(tradeStatus);

		return str.toString();
	}

	public boolean isJihePriceTime(String stockcode) {
		int stocktype = StockUtil.checkStockcode(stockcode);
		Integer jiheStartHour = ConfigCenterFactory.getInt("stock_zjs.jiheStartHour" + stocktype, 9);
		Integer jiheStartMinute = ConfigCenterFactory.getInt("stock_zjs.jiheStartMinute" + stocktype, 0);
		Integer jiheEndHour = ConfigCenterFactory.getInt("stock_zjs.jiheEndHour" + stocktype, 9);
		Integer jiheEndMinute = ConfigCenterFactory.getInt("stock_zjs.jiheEndMinute" + stocktype, 30);

		Date jiheStartTime = getSpecifyDate(jiheStartHour, jiheStartMinute);
		Date jiheEndTime = getSpecifyDate(jiheEndHour, jiheEndMinute);

		long currentTime = System.currentTimeMillis();

		if(currentTime >= jiheStartTime.getTime() && currentTime <= jiheEndTime.getTime()) {
			return true;
		}

		return false;
	}

	public static Date getSpecifyDate(int hour, int minute) {
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.MINUTE, minute);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);
		Date ret = c.getTime();
		return ret;
	}
}
