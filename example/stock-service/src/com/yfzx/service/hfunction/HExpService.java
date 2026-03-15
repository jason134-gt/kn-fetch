package com.yfzx.service.hfunction;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.IndexMessage;
import com.yfzx.service.db.CRuleService;

/**
 * 按条件统计
 * timeUnit:时间单位，nd多少天,exp:表达式 $hexpcount(timeUnit,nd,exp)
 * @author：杨真
 * @date：2014年10月20日
 */
public class HExpService implements IFService {

	private static HExpService instance = new HExpService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HExpService() {

	}

	public static HExpService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage req, List<String> vls) {
		Double ret = 0.0;
		if (vls != null && vls.size() > 0) {
			String exp = vls.get(0);
			ret = exp(req, exp);
		}
		return ret;
	}

	public Double exp(IndexMessage req,
			String exp) {
		return CRuleService.getInstance().computeIndex(exp, req, StockConstants.TRADE_TYPE);
	}
}
