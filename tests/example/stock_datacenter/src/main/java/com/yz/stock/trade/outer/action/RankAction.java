package com.yz.stock.trade.outer.action;

import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yz.stock.common.BaseAction;
import com.yz.stock.trade.outer.cache.RankCacheService;
import com.yz.stock.trade.outer.qq.QQDataAgent;

public class RankAction extends BaseAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7268696021004413716L;
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Action(value = "/rank/getSCLD")
	public void  getSCLD()
	{
		String ret = RankCacheService.getInstance().get(SCache.CACHE_KEY_SCLD_RANK);
		if(!StringUtil.isEmpty(ret))
			StockUtil.outputJson(this.getHttpServletResponse(), ret);
	}
	
	@Action(value = "/rank/getCJLJJ")
	public void  getCJLJJ()
	{
		String ret = RankCacheService.getInstance().get(SCache.CACHE_KEY_CJLJJ);
		if(!StringUtil.isEmpty(ret))
			StockUtil.outputJson(this.getHttpServletResponse(), ret);
	}
	
	
	@Action(value = "/rank/getCJLTZ")
	public void  getCJLTZ()
	{
		String ret = RankCacheService.getInstance().get(SCache.CACHE_KEY_CJLTZ);
		if(!StringUtil.isEmpty(ret))
			StockUtil.outputJson(this.getHttpServletResponse(), ret);
	}
	
	
	@Action(value = "/rank/getLXSZ")
	public void  getLXSZ()
	{
		String ret = RankCacheService.getInstance().get(SCache.CACHE_KEY_LXSZ);
		if(!StringUtil.isEmpty(ret))
			StockUtil.outputJson(this.getHttpServletResponse(), ret);
	}

	
	@Action(value = "/rank/getLXXD")
	public void  getLXXD()
	{
		String ret = RankCacheService.getInstance().get(SCache.CACHE_KEY_LXXD);
		if(!StringUtil.isEmpty(ret))
			StockUtil.outputJson(this.getHttpServletResponse(), ret);
	}
	
	
	@Action(value = "/rank/getJQXD")
	public void  getJQXD()
	{
		String ret = RankCacheService.getInstance().get(SCache.CACHE_KEY_JQXD);
		if(!StringUtil.isEmpty(ret))
			StockUtil.outputJson(this.getHttpServletResponse(), ret);
	}

	
	@Action(value = "/rank/getJQXG")
	public void  getJQXG()
	{
		String ret = RankCacheService.getInstance().get(SCache.CACHE_KEY_JQXG);
		if(!StringUtil.isEmpty(ret))
			StockUtil.outputJson(this.getHttpServletResponse(), ret);
	}

	
	@Action(value = "/rank/getDMDPX0")
	public void  getDMDPX0()
	{
		String ret = RankCacheService.getInstance().get(SCache.CACHE_KEY_DDPX0);
		if(!StringUtil.isEmpty(ret))
			StockUtil.outputJson(this.getHttpServletResponse(), ret);
	}

	
	@Action(value = "/rank/getDMDPX1")
	public void  getDMDPX1()
	{
		String ret = RankCacheService.getInstance().get(SCache.CACHE_KEY_DDPX1);
		if(!StringUtil.isEmpty(ret))
			StockUtil.outputJson(this.getHttpServletResponse(), ret);
	}


	@Action(value = "/rank/getZD10")
	public String  getZD10()
	{
		String type = this.getHttpServletRequest().getParameter("type");
		if(StringUtil.isEmpty(type))
			return ERROR;
		String ret = QQDataAgent.getInstance().getZD10Cache(Integer.valueOf(type));
		if(!StringUtil.isEmpty(ret))
			StockUtil.outputJson(this.getHttpServletResponse(), ret);
		return SUCCESS;
	}


}
