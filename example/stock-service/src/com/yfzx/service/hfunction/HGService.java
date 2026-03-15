package com.yfzx.service.hfunction;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.StringUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.USubjectService;

/**
 * 取指标的值，根据参数 $hg(indexcode,timeAdd,companycode) 
 * timeAdd:时间增量
 * 
 * @author：杨真
 * @date：2014年10月20日
 */
public class HGService implements IFService {

	private static HGService instance = new HGService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private HGService() {

	}

	public static HGService getInstance() {
		return instance;
	}

	@Override
	public Double doInvoke(IndexMessage nreq, List<String> vls) {
		Double ret = 0.0;
		if (vls != null && vls.size() > 1) {

			String indexcode = vls.get(0);
			Integer timeAdd = Integer.valueOf(vls.get(1));
			IndexMessage req = (IndexMessage) nreq.clone();
			if(vls.size()>2)
			{
				String ccode = vls.get(2);
				if(!StringUtil.isEmpty(ccode))
				{
					req.setCompanyCode(ccode);
				}
			}
			Date mintime = USubjectService.getInstance().getTradeIndexMinTime(req.getUidentify(),StockConstants.INDEX_CODE_TRADE_S_W);
			if(mintime==null)
				return ret;
			if (StringUtil.isEmpty(indexcode))
				return ret;
			Dictionary d = DictService.getInstance()
					.getDataDictionaryFromCache(indexcode);
			Date actime = null;
			if (timeAdd != 0) {
				actime = IndexService.getInstance().getNextTradeUtilEnd(
						req.getTime(), d, req.getCompanyCode(), timeAdd);
			} else {
				actime = IndexService.getInstance().formatTime(req.getTime(),
						d, req.getCompanyCode());
			}
			
			if (actime != null) {
				ret = IndexValueAgent.getIndexValueNeedCompute(req.getCompanyCode(),
						indexcode, actime);
			}
		}
		return ret;
	}

	

}
