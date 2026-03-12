package com.yfzx.service.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Stockpool;
import com.stock.common.util.StringUtil;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.enter.LCEnter;

public class StockPoolService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	Logger logger = LoggerFactory.getLogger(this.getClass());
	DictService ds = DictService.getInstance();
	private static StockPoolService instance = new StockPoolService();

	private StockPoolService() {

	}

	public static StockPoolService getInstance() {
		return instance;
	}

	public Stockpool getStockPoolByName(String name) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("name", name);
		RequestMessage req = DAFFactory.buildRequest("getStockPoolByName", m,
				StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Stockpool) value;
	}

	public List<Stockpool> getAllStockPool() {
		RequestMessage req = DAFFactory.buildRequest("getAllStockPoolList",
				StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Stockpool>) value;
	}

	public List<Stockpool> getAllStockPoolFromcache() {
		return LCEnter.getInstance().get(SCache.KEY_ALL_STOCKPOOL,
				SCache.CACHE_NAME_stockpool);
	}

	public <T> T getStockPoolAttr(String name, String companycode) {
		return LCEnter.getInstance().get(getAttrKey(name, companycode),
				SCache.CACHE_NAME_stockpool);
	}

	public String deleteStockpoolByName(String name) {
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"deleteStockpoolByName", name, StockConstants.common);
		return pLayerEnter.delete(reqMsg);
	}

	public String deleteStockpoolByType(int type) {
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"deleteStockpoolByType", type, StockConstants.common);
		return pLayerEnter.delete(reqMsg);
	}

	public String modifyStockpoolByName(Stockpool sp) {
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"modifyStockpoolByName", sp, StockConstants.common);
		return pLayerEnter.delete(reqMsg);
	}

	public String modifyStockpool(Stockpool sp) {
		String ret = "";
		if (sp != null) {
			Stockpool nsp = getStockPoolByName(sp.getName());
			if (nsp == null) {
				ret = insert(sp);
			} else {
				ret = modifyStockpoolByName(sp);
			}
		} else {
			ret = insert(sp);
		}
		return ret;
	}

	/**
	 * 追加
	 * 
	 * @param sp
	 * @return
	 */
	public String append2Stockpool(Stockpool sp) {
		String ret = "";
		if (sp != null) {
			Stockpool nsp = getStockPoolByName(sp.getName());
			if (nsp == null) {
				ret = insert(sp);
			} else {
				String[] sa = sp.getPool().split(";");
				StringBuilder sb = new StringBuilder();
				sb.append(nsp.getPool());
				for (String p : sa) {
					if (nsp.getPool().indexOf(p + ";") < 0) {
						sb.append(p);
						sb.append(";");
					}
				}
				nsp.setPool(sb.toString());
				ret = modifyStockpoolByName(nsp);
			}
		} else {
			ret = insert(sp);
		}
		return ret;
	}

	/**
	 * 清除一个公司的所有财务标签
	 * 
	 * @param sp
	 * @return
	 */
	public void delCompanyAllCfTag(String companycode) {
		List<Stockpool> spl = getAllStockPool();
		if(spl==null) return;
		for(Stockpool sp:spl)
		{
			if (sp != null&&!StringUtil.isEmpty(sp.getPool())) {
				
					if(sp.getType()!=0)
						continue;
					String[] sa = sp.getPool().split(";");
					StringBuilder sb = new StringBuilder();
					for (String p : sa) {
						if (!p.equals(companycode)) {
							sb.append(p);
							sb.append(";");
						}
					}
					sp.setPool(sb.toString());
					modifyStockpoolByName(sp);
				
			} 
		}

	}
	
	public String insert(Stockpool sp) {
		RequestMessage reqMsg = DAFFactory.buildRequest("insertStockpool", sp,
				StockConstants.common);
		return pLayerEnter.insert(reqMsg);
	}

	/**
	 * 按类型解析
	 * 
	 * @param attr
	 * @param type
	 * @return
	 */
	public Object parseAttrByType(String attr, Integer type) {
		// TODO Auto-generated method stub
		return attr;
	}

	public String getAttrKey(String name, String companyCode) {
		// TODO Auto-generated method stub
		return name + "." + companyCode;
	}

	public void parseStockpool(Stockpool sp) {
		if (sp.getType() == -1)
			return;
		String pool = sp.getPool();
		if (!StringUtil.isEmpty(pool)) {
			List<Company> cl = new ArrayList<Company>();
			String[] clist = pool.split(";");
			for (String cas : clist) {
				String[] ca = cas.split("\\|");
				String companycode = ca[0];
				if (!StringUtil.isEmpty(companycode)) {
					Company c = CompanyService.getInstance().getCompanyByCodeFromCache(
							companycode);
					if (c != null) {
						if(!cl.contains(c))
						{
							cl.add(c);
//							c.appendTag2CompanyCache(sp.getKey());
						}
					}

					if (ca.length > 1) {
						String attr = ca[1];
						Object pattr = StockPoolService.getInstance()
								.parseAttrByType(attr, sp.getType());
						if (pattr != null) {
							String akey = StockPoolService.getInstance()
									.getAttrKey(sp.getName(),
											c.getCompanyCode());
							LCEnter.getInstance().put(akey, pattr,
									SCache.CACHE_NAME_stockpool);
						}
					}
				}

			}
			if (cl.size() > 0) {
				LCEnter.getInstance().put(sp.getName(), cl, StockConstants.COMPANY_CACHE_NAME);
			}
			// 清掉sp的未解析的pool值
			sp.setPool("");
			LCEnter.getInstance().put(sp.getKey().trim(), sp, SCache.CACHE_NAME_stockpool);
		}
	}

}
