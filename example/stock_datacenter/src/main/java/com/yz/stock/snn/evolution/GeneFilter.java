package com.yz.stock.snn.evolution;

import java.util.ArrayList;
import java.util.List;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.USubject;
import com.stock.common.model.snn.Gene;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.IndexService;

/**
 * 基因过滤器
 * 
 * @author：杨真
 * @date：2014-4-7
 */
public class GeneFilter {

	/**
	 * 过滤掉不合适的基因
	 * 
	 * @param g
	 * @return
	 */
	public static boolean isNormalGene(Gene g) {
		// 小于0的规则不计算
		if (g.getRule() != null && g.getRule().getRuleType() < 0)
			return false;
		return true;
	}

	public static boolean isGoodGene(Gene g) {
		// TODO Auto-generated method stub
		return true;
	}

	public static boolean isneedComputeGene(Gene g, IndexMessage im) {
		// 小于0的规则不计算
		if (g.getRule() != null && g.getRule().getRuleType() < 0)
			return false;
		// 如果是财务规则,则判断是否是离财报最近的一个交易日,如果不是,就不计算
		if (g.getRule() != null
				&& g.getRule().getRuleType() == StockConstants.DEFINE_INDEX
				&& !IndexService.getInstance().isJiduTradeDate(im))
			return false;
		return true;
	}

	public static List<Gene> FilterGeneByUSubject(USubject s, List<Gene> agl) {
		String tscs = s.getTscs();
		if (StringUtil.isEmpty(tscs))
			return agl;
		List<Gene> ngl = new ArrayList<Gene>();
		for (Gene g : agl) {
			if (g.getRule()==null||tscs.indexOf(g.getTsc()) >= 0)
				ngl.add(g);
		}
		return ngl;
	}
}
