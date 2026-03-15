package com.yfzx.service.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Tagrule;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.enter.LCEnter;



public class TagruleService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	Logger logger = LoggerFactory.getLogger(this.getClass());
	DictService ds = DictService.getInstance();
	private static TagruleService instance = new TagruleService();

	private TagruleService() {

	}

	public static TagruleService getInstance() {
		return instance;
	}

	public String modifyTagruleByid(Tagrule tr) {
	
		RequestMessage reqMsg = DAFFactory.buildRequest("modifyTagruleByid",tr,StockConstants.common);
		return pLayerEnter.modify(reqMsg);
	}

	public String insert(Tagrule tr) {
		RequestMessage reqMsg = DAFFactory.buildRequest("insertTagrule",tr,StockConstants.common);
		return pLayerEnter.insert(reqMsg);
	}

	public List<Tagrule> getNormalTagrules() {
		// 取当天所有新加入到库中的指标
		List<Tagrule> crl = TagruleService.getInstance().getLatestTagruleList();
		if (crl == null) {
//			log.debug("not get the new rule ;return ");
			return null;
		}
		String tagruleids = ConfigCenterFactory
				.getString("stock_dc.unnormal_tagrule_ids",
						"1226,1227,1228,1229,1230,1231,1233,1234,1236,1237,1238,1239,1240,1241");
		Set<String> ss = new HashSet<String>();
		for (String tid : tagruleids.split(",")) {
			ss.add(tid);
		}
		List<Tagrule> ncrl = new ArrayList<Tagrule>();
		for (Tagrule tr : crl) {
			if (!ss.contains(tr.getId().toString()))
				ncrl.add(tr);
		}
		return ncrl;
	}
	
	public Tagrule getTagruleById(Integer id) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("id", id);
		RequestMessage req = DAFFactory.buildRequest(
				"getTagruleById", m, StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Tagrule) value;
	}
	
	public Tagrule getTagruleByIdFromCache(String id) {
	
		return  LCEnter.getInstance().get(id,SCache.CACHE_NAME_TAGRULECACHE);
	}


	public List<Tagrule> queryListByTagDesc(String tagDesc) {
		if(tagDesc==null) tagDesc = "";
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("tagDesc", tagDesc);
		RequestMessage req = DAFFactory.buildRequest(
				"queryListByTagDesc", m, StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Tagrule>) value;
	}

	public List<Tagrule> queryAllTagrules() {
		RequestMessage req = DAFFactory.buildRequest(
				"queryAllTagrules", StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Tagrule>) value;
	}
	
	public int isAccord(String companycode,Date time, String rule,int indextype) {
		IndexMessage req = SMsgFactory.getUDCIndexMessage(companycode);;
		req.setCompanyCode(companycode);
		req.setTime(time);
		req.setNeedUseExtDataCache(true);
		req.setNeedAccessExtRemoteCache(false);
		req.setNeedComput(true);
		req.setNeedAccessCompanyBaseIndexDb(false);
		req.setNeedAccessExtIndexDb(false);
		req.setNeedRealComputeIndustryValue(true);
		Double d = CRuleService.getInstance().computeIndex(rule, req,indextype);
		if(d==null) d=-1.0;
		return d.intValue();
	}

	public int isAccordNeedCompute(String companycode,Date time, String rule,int indextype,boolean needcompute) {
		IndexMessage req = SMsgFactory.getUDCIndexMessage(companycode);;
		req.setCompanyCode(companycode);
		req.setTime(time);
		req.setNeedUseExtDataCache(true);
		req.setNeedAccessExtRemoteCache(false);
		req.setNeedComput(needcompute);
		req.setNeedRealComputeIndustryValue(false);
		Double d = CRuleService.getInstance().computeIndex(rule, req,indextype);
		if(d==null) d=-1.0;
		return d.intValue();
	}
	
	public int isAccord(String companycode,Date time, Tagrule rule) {
		IndexMessage req = SMsgFactory.getUDCIndexMessage(companycode);;
		req.setCompanyCode(companycode);
		if(StockUtil.isTradeIndex(rule.getRuleType()))
			time = IndexService.getTradeTime(companycode, time);
		if(time==null)
			return -1;
		req.setTime(time);
		req.setNeedUseExtDataCache(true);
		req.setNeedAccessExtRemoteCache(false);
		req.setNeedComput(true);
		req.setNeedAccessCompanyBaseIndexDb(false);
		req.setNeedAccessExtIndexDb(false);
		req.setNeedRealComputeIndustryValue(true);
		Double d = CRuleService.getInstance().computeIndex(req,rule);
		if(d==null) d=-1.0;
		return d.intValue();
	}
	
	public String realComputeOneCompanyAllCFTag(String companycode, Date time) {
		StringBuilder sb = new StringBuilder();
		Company c = CompanyService.getInstance().getCompanyByCodeFromCache(companycode);
		List<Tagrule> allTr = getTagrulesByTscFromDb(c.getTsc());
		if(allTr==null) return "";
		for(Tagrule t : allTr)
		{
			if(c.containsTheTag(t.getIndustryTag())&&isAccord(companycode, time, t)>0)
			{
				sb.append(t.getTagDesc());
				sb.append(",");
			}
		}
		return sb.toString();
	}

	public List<Tagrule> getAllTagrules() {
		
		return LCEnter.getInstance().get(StockConstants.TAGRULE_ALL,SCache.CACHE_NAME_TAGRULECACHE);
	}

	public List<Tagrule> getTagrulesByTscFromCache(String tsc) {
			
			return LCEnter.getInstance().get(tsc,SCache.CACHE_NAME_TAGRULECACHE);
	}

	public List<Tagrule> getTagrulesByTscFromDb(String tsc) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("tsc", tsc);
		RequestMessage req = DAFFactory.buildRequest(
				"getTagrulesByTscFromDb",m, StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Tagrule>) value;
}
	
	public String delTagruleById(Integer id) {
		RequestMessage reqMsg = DAFFactory.buildRequest("delTagruleById",id,StockConstants.common);
		return pLayerEnter.delete(reqMsg);
	}

	public String modifyTagrule(Tagrule tr) {
		String ret = "";
		if(tr.getId()>0)
		{
			Tagrule rtr = getTagruleById(tr.getId());
			if(rtr==null)
			{
				ret = insert(tr);
			}
			else
			{
				ret = modifyTagruleByid(tr);
			}
		}
		else
		{
			ret = insert(tr);
		}
		return ret;
	}

	public List<String> parseShowXYIndexs(String cs) {
		List<String> ls = new ArrayList<String>();
		String xindexs="";
		String yindexs="";
		String[] csa = cs.split("~");
		for(int i=0;i<csa.length;i++)
		{
			String[] si = csa[i].split("\\^");
			if(si==null||si.length==0) continue;
			String zi = si[0];
			if("0".equals(zi))
				{
					xindexs+=si[2];
					xindexs+="^";
				}
			if("1".equals(zi))
				{
					yindexs+=si[2];
					yindexs+="^";
				}
		}
		if(xindexs==""&&yindexs=="")
		{
			return null;
		}
		if(!StringUtil.isEmpty(xindexs))
			ls.add(xindexs);
		if(!StringUtil.isEmpty(yindexs))
			ls.add(yindexs);
		return ls;
	}

	public void modifyTagruleComments(Integer id, String string) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 取最新更新的规则
	 * @return
	 */
	public List<Tagrule> getLatestTagruleList() {
		RequestMessage req = DAFFactory.buildRequest(
				"getLatestTagruleList", StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Tagrule>) value;
	}

	public void updateCFRuleOfCompute() {
		int open = ConfigCenterFactory.getInt("stock_dc.open_update_tag_rule", 1);
		if(open==0)
			return;
		RequestMessage req = DAFFactory.buildRequest(
				"updateCFRuleOfCompute", StockConstants.common);
		pLayerEnter.modify(req);
	}

	public String translateGk(String key) {
		StringBuffer sb = new StringBuffer();
		String[] kl = key.split("~");
		for(String kk:kl)
		{
			String[] kka = kk.split("\\^");
			for(String kks:kka)
			{
				Tagrule tr = getTagruleByIdFromCache(kks);
				if(tr!=null)
				{
					sb.append(tr.getTagDesc());
					sb.append("^");
				}
			}
			if(sb.toString().endsWith("^"))
				sb=new StringBuffer(sb.substring(0, sb.length()-1));
			sb.append("~");
		}
		if(sb.toString().endsWith("~"))
			sb=new StringBuffer(sb.substring(0, sb.length()-1));	
		return sb.toString();
	}
	
}
