package com.yfzx.service.db.company;

import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.bloomfilter.QueryParam;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.company.Company0021;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;


public class Company0021Service {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	static Logger logger = LoggerFactory.getLogger("Company0021Service");
	private final static String DSH_NAME = "董事会成员", JSH_NAME = "监事会成员",	JLTD_NAME = "高管成员";
	private final static String SELECT_NOW_LIST = "com.yz.stock.portal.dao.company0021.selectNowList";
	private static Company0021Service instance = new Company0021Service();

	private Company0021Service() {

	}

	public static Company0021Service getInstance() {
		return instance;
	}

	/**
	 * 获取某家公司的现任董事会成员
	 * 
	 * @param stockCode
	 *            股票代码
	 * @return
	 */
	public List<Company0021> getNowDSHList(String stockCode) {
		return getNowListByF002V(stockCode, DSH_NAME);
	}

	/**
	 * 获取某家公司的现任监事会成员
	 * 
	 * @param stockCode
	 *            股票代码
	 * @return
	 */
	public List<Company0021> getNowJSHList(String stockCode) {
		return getNowListByF002V(stockCode, JSH_NAME);
	}

	/**
	 * 获取某家公司的现任经理团队成员
	 * 
	 * @param stockCode
	 *            股票代码
	 * @return
	 */
	public List<Company0021> getNowJLTDList(String stockCode) {
		return getNowListByF002V(stockCode, JLTD_NAME);
	}

	private List<Company0021> getNowListByF002V(String stockCode, String f002v) {
		Company0021 company0021 = new Company0021();
		company0021.setCompany_code(stockCode);
		company0021.setF002v(f002v);

		List value = null;
		try {
			Map map = BeanUtils.describe(company0021);
			QueryParam queryParam = null;
			map.put("queryParam", queryParam);
			RequestMessage reqMsg = DAFFactory.buildRequest(SELECT_NOW_LIST,
					map, StockConstants.COMPANY_TYPE);
			value = pLayerEnter.queryForList(reqMsg);
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return value;
	}
}
