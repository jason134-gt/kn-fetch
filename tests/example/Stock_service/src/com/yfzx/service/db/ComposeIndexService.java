package com.yfzx.service.db;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.user.Composeindex;
import com.stock.common.util.StringUtil;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;

/**
 * 行情数据中心
 * 
 * @author Administrator
 * 
 */
public class ComposeIndexService {

	private static ComposeIndexService instance = new ComposeIndexService();
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private ComposeIndexService() {

	}

	public static ComposeIndexService getInstance() {
		return instance;
	}

	public Composeindex getComposeIndex(Long uid) {
		RequestMessage req = DAFFactory.buildRequest(
				"com.stock.common.model.user.Composeindex.selectByPrimaryKey",
				uid, StockConstants.common);
		Object o = pLayerEnter.queryForObject(req);
		if (o == null) {
			return null;
		}
		return (Composeindex) o;
	}

	public String insertComposeIndex(Long uid, String composeIndexs) {
		Composeindex ci = new Composeindex(uid, composeIndexs);
		RequestMessage req = DAFFactory.buildRequest(
				"com.stock.common.model.user.Composeindex.insert", ci,
				StockConstants.common);
		return pLayerEnter.insert(req);

	}

	public String addComposeIndex(Long uid, String composeIndexs) {
		Composeindex ci = getComposeIndex(uid);
		if (ci == null&&!StringUtil.isEmpty(composeIndexs)) {
			composeIndexs+=";";
			insertComposeIndex(uid, composeIndexs);
		}
		String ncis = ci.getComposeIndexs();
		StringBuffer sb = new StringBuffer();
		if (!StringUtil.isEmpty(ncis))
			sb.append(ncis);
		if(!StringUtil.isEmpty(ncis)&&ncis.contains(composeIndexs)) return null;
		if (!StringUtil.isEmpty(composeIndexs)) {
			sb.append(composeIndexs);
			sb.append(";");
		}
		ci.setComposeIndexs(sb.toString());

		RequestMessage req = DAFFactory.buildRequest(
				"com.stock.common.model.user.Composeindex.updateByPrimaryKey",
				ci, StockConstants.common);
		return pLayerEnter.modify(req);
	}

	public String delComposeIndex(Long uid, String composeIndexs) {
		Composeindex ci = getComposeIndex(uid);
		if (ci == null) {
			return "";
		}
		String ncis = ci.getComposeIndexs();
		if (!StringUtil.isEmpty(ncis)) {
			if (!StringUtil.isEmpty(composeIndexs)) {
				String o = composeIndexs + ";";
				ncis = ncis.replace(o.trim(), "");
			}
		}

		ci.setComposeIndexs(ncis);

		RequestMessage req = DAFFactory.buildRequest(
				"com.stock.common.model.user.Composeindex.updateByPrimaryKey",
				ci, StockConstants.common);
		return pLayerEnter.modify(req);

	}

}
