package com.yz.stock.portal.action;

import java.util.List;

import net.sf.json.JSONObject;

import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockCodes;
import com.stock.common.model.Cfirule;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Indexcategory;
import com.stock.common.model.TableSystem;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexCategoryService;
import com.yfzx.service.db.TableSystemService;
import com.yz.stock.common.BaseAction;



public class CfiruleAction extends BaseAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2149622762066330399L;
	CRuleService crs = CRuleService.getInstance();
	private IndexMessage msg = new IndexMessage();
	private Logger log = LoggerFactory.getLogger(this.getClass());
	private Cfirule rule = new Cfirule();
	@Action(value = "/cfirule/saveRule")
	public String saveRule() {	
		try {
			String ret = crs.saveRule(rule);
			if(!ret.equals(StockCodes.SUCCESS))
			{
				return ERROR;
			}
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	@Action(value = "/cfirule/batchSaveRule")
	public String batchSaveRule() {	
		try {
			List<TableSystem> tsl = TableSystemService.getInstance().getTableSystemListByDs("ds_0002");
			for(TableSystem ts:tsl)
			{
				List<Cfirule> cfl = crs.getCfiruleList(ts.getTableSystemCode());
				for(Cfirule cf : cfl)
				{
					crs.saveRule(cf);
				}
			}
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	@Action(value = "/cfirule/modifyRule")
	public String modifyRule() {	
		try {
			String ret = crs.modifyRule(rule);
			if(!ret.equals(StockCodes.SUCCESS))
			{
				return ERROR;
			}
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	@Action(value = "/cfirule/queryRuleById")
	public String queryRuleById() {	
		try {

			Cfirule cf = crs.getCIndexRuleByCode(rule.getcIndexCode());
			if(cf!=null)
			{
				Dictionary d = DictService.getInstance().getDataDictionaryFromCache(cf.getcIndexCode());
				cf.setTimeUnit(d.getTimeUnit());
				cf.setInterval(d.getInterval());
				this.setResultData(cf);
			}
			
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	@Action(value = "/cfirule/getRuleByCondition")
	public String getRuleByCondition() {	
		try {
			if(!StringUtil.isEmpty(rule.getName()))
			{
				rule.setName("%"+rule.getName()+"%");
			}
			List<Cfirule> cfl = crs.getRuleByCondition(rule);
			if(cfl!=null)
			{
				for(Cfirule c : cfl)
				{
					Dictionary d = DictService.getInstance().getDataDictionaryDB(c.getcIndexCode());
					c.setTimeUnit(d.getTimeUnit());
					c.setInterval(d.getInterval());
				}
				this.setResultData(cfl);
			}
			
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	/*
	 * 格式：indexName:indexcode;indexName:indexcode;
	 */
	@Action(value = "/cfirule/getCfiruleListByTscid_V2")
	public String getCfiruleListByTscid_V2() {
		try {
		StringBuilder sb = new StringBuilder();
		if(StringUtil.isEmpty(msg.getTableSystemCode()))
		{
			log.error("table system code is null");
			setErrorReason("table system code is null");
			return ERROR;
		}
		//取所有基本数据项或基本指示
		List<Dictionary> dl = DictService.getInstance().getDictListByTableSystemCode(msg.getTableSystemCode());
		if(dl!=null)
		{
			for(Dictionary d :dl)
			{
				if(!StringUtil.isEmpty(d.getShowName())&&d.getType()>=0&&d.getBitSet().charAt(0)=='1')
				{
					
					sb.append(d.getShowName()+":"+d.getIndexCode());
					sb.append(";");
				}
			}
		}
		else {
			log.info("getCfiruleListByTid :"+msg);
			setErrorReason("query failed！");
		}
		String ret = sb.toString();
		//去掉尾部的分号
		if(sb.toString().endsWith(";"))
		{
			ret = sb.substring(0, sb.length()-1);
		}
		StockUtil.outputJson(this.getHttpServletResponse(),"retObj","'"+ret+"'");
	} catch (Exception e) {
		log.error("execute /cfirule/getCfiruleList failed", e);
		setErrorReason("add failed！");
		return ERROR;
	}
	return SUCCESS;
	}
	
	/*
	 * 只取扩展指标;
	 */
	@Action(value = "/cfirule/getCfiruleListByTscid_V3")
	public String getCfiruleListByTscid_V3() {
		try {
		StringBuilder sb = new StringBuilder();
		if(StringUtil.isEmpty(msg.getTableSystemCode()))
		{
			log.error("table system code is null");
			setErrorReason("table system code is null");
			return ERROR;
		}
		//取所有基本数据项或基本指示
		List<Dictionary> dl = DictService.getInstance().getDictListByTableSystemCode(msg.getTableSystemCode());
		if(dl!=null)
		{
			for(Dictionary d :dl)
			{
				if(!StringUtil.isEmpty(d.getShowName())&&!StockUtil.isBaseIndex(d.getType())&&d.getBitSet().charAt(0)=='1')
				{
					
					sb.append(d.getShowName()+":"+d.getIndexCode());
					sb.append(";");
				}
			}
		}
		else {
			log.info("getCfiruleListByTid :"+msg);
			setErrorReason("query failed！");
		}
		String ret = sb.toString();
		//去掉尾部的分号
		if(sb.toString().endsWith(";"))
		{
			ret = sb.substring(0, sb.length()-1);
		}
		StockUtil.outputJson(this.getHttpServletResponse(),"retObj","'"+ret+"'");
	} catch (Exception e) {
		log.error("execute /cfirule/getCfiruleList failed", e);
		setErrorReason("add failed！");
		return ERROR;
	}
	return SUCCESS;
	}
	
	/*
	 * 格式：indexcode:indexName:type;indexcode:indexName:type;
	 */
	@Action(value = "/cfirule/getCfiruleListByTid")
	public String getCfiruleListByTid() {
		try {
		StringBuilder sb = new StringBuilder();
		if(StringUtil.isEmpty(msg.getTableSystemCode()))
		{
			log.error("table system code is null");
			setErrorReason("table system code is null");
			return ERROR;
		}
		//取所有基本数据项或基本指示
		List<Dictionary> dl = DictService.getInstance().getDictListByTableSystemCode(msg.getTableSystemCode());
		if(dl!=null)
		{
			for(Dictionary d :dl)
			{
				if(!StringUtil.isEmpty(d.getShowName())&&d.getType()>=0&&d.getBitSet().charAt(0)=='1')
				{
					
					sb.append(d.getIndexCode() + ":" + d.getShowName()+":"+d.getTableName()+":"+d.getType());
					sb.append(";");
				}
			}
		}
		else {
			log.info("getCfiruleListByTid :"+msg);
			setErrorReason("query failed！");
		}
		String ret = sb.toString();
		//去掉尾部的分号
		if(sb.toString().endsWith(";"))
		{
			ret = sb.substring(0, sb.length()-1);
		}
		this.setResultData(ret);
	} catch (Exception e) {
		log.error("execute /cfirule/getCfiruleList failed", e);
		setErrorReason("add failed！");
		return ERROR;
	}
	return SUCCESS;
	}
	
	/*
	 * 格式：indexcode:indexName:type;indexcode:indexName:type;
	 */
	@Action(value = "/cfirule/getDictionaryByTag")
	public String getDictionaryByTag() {
		try {
		String tag = this.getHttpServletRequest().getParameter("tag");
		String tsc = this.getHttpServletRequest().getParameter("tableSystemCode");
		if(StringUtil.isEmpty(tag)||StringUtil.isEmpty(tsc))
			return ERROR;
		StringBuilder sb = new StringBuilder();
		//取所有基本数据项或基本指示
		List<Dictionary> dl = DictService.getInstance().getDictionaryListByTagfromCache(tag);
		if(dl!=null)
		{
			for(Dictionary d :dl)
			{
				if(!TableSystemService.getInstance().isThisTscDictionary(tsc,d))
					continue;
				if(!StringUtil.isEmpty(d.getShowName())&&d.getType()>=0&&d.getType()<=6&&d.getBitSet().charAt(0)=='1')
				{
					
					sb.append(d.getIndexCode() + ":" + d.getShowName());
					sb.append(";");
				}
			}
		}
		else {
			log.info("getCfiruleListByTid :"+msg);
			setErrorReason("query failed！");
		}
		String ret = sb.toString();
		//去掉尾部的分号
		if(sb.toString().endsWith(";"))
		{
			ret = sb.substring(0, sb.length()-1);
		}
//		this.setResultData(ret);
//		StockUtil.outputJsonSameStruct(this.getHttpServletResponse(),ret);
		JSONObject json = new JSONObject();
		json.put("data", ret);
		StockUtil.outputJson(this.getHttpServletResponse(), "retobj", json);
	} catch (Exception e) {
		log.error("execute /cfirule/getCfiruleList failed", e);
		setErrorReason("add failed！");
		return ERROR;
	}
	return SUCCESS;
	}
	/*
	 * tagName|tagCode:indexcode_indexName_type|indexcode_indexName_type;tagName|tagCode:indexcode_indexName_type|indexcode_indexName_type
	 */
	@Action(value = "/cfirule/getTagsCfiruleListByTid")
	public String getTagsCfiruleListByTid() {
		try {
		StringBuilder sb = new StringBuilder();
		if(StringUtil.isEmpty(msg.getTableSystemCode()))
		{
			log.error("table system code is null");
			setErrorReason("table system code is null");
			return ERROR;
		}
		List<Indexcategory> icl = IndexCategoryService.getInstance().getAllIndexcategory();
		for(Indexcategory ic : icl)
		{
			List<Dictionary> dl = DictService.getInstance().getDictionaryListByTag(String.valueOf(ic.getIndexCategoryCode()));
			if(dl!=null&&dl.size()>0)
			{
				sb.append(ic.getName()+"_"+ic.getIndexCategoryCode());
				sb.append(":");
				for(Dictionary d :dl)
				{
					if(!StringUtil.isEmpty(d.getShowName())&&d.getType()>=0&&d.getType()<=6&&d.getBitSet().charAt(0)=='1')
					{
						
						sb.append(d.getIndexCode() + "_" + d.getShowName()+"_"+d.getTableName()+"_"+d.getType());
						sb.append("|");
					}
				}
				sb.append(";");
			}
		}
	
		String ret = sb.toString();
		//去掉尾部的分号
		if(sb.toString().endsWith(";"))
		{
			ret = sb.substring(0, sb.length()-1);
		}
		this.setResultData(ret);
	} catch (Exception e) {
		log.error("execute /cfirule/getCfiruleList failed", e);
		setErrorReason("add failed！");
		return ERROR;
	}
	return SUCCESS;
	}
	
	
	@Action(value = "/cfirule/getCfiruleListByTidWithOutCache")
	public String getCfiruleListByTidWithOutCache() {
		try {
		StringBuilder sb = new StringBuilder();
		if(StringUtil.isEmpty(msg.getTableSystemCode()))
		{
			log.error("table system code is null");
			setErrorReason("table system code is null");
			return ERROR;
		}
		//取所有基本数据项或基本指示
		List<Dictionary> dl = DictService.getInstance().getDictListByTableSystemCodeWithOutCache(msg.getTableSystemCode());
		if(dl!=null)
		{
			for(Dictionary d :dl)
			{
				if(!StringUtil.isEmpty(d.getShowName())&&d.getType()>=0&&d.getType()<=6&&d.getBitSet().charAt(0)=='1')
				{
					
					sb.append(d.getIndexCode() + ":" + d.getShowName()+":"+d.getTableName()+":"+d.getType());
					sb.append(";");
				}
			}
		}
		else {
			log.info("getCfiruleListByTid :"+msg);
			setErrorReason("query failed！");
		}
		String ret = sb.toString();
		//去掉尾部的分号
		if(sb.toString().endsWith(";"))
		{
			ret = sb.substring(0, sb.length()-1);
		}
		//this.setResultData(ret);
		StockUtil.outputJson(this.getHttpServletResponse(),"retObj","'"+ret+"'");
	} catch (Exception e) {
		log.error("execute /cfirule/getCfiruleList failed", e);
		setErrorReason("add failed！");
		return ERROR;
	}
	return SUCCESS;
	}
	
	/**
	 * 格式：indexname:indexcode
	 * @return
	 */
	@Action(value = "/cfirule/getCfiruleListByTidWithOutCache_V2")
	public String getCfiruleListByTidWithOutCache_V2() {
		try {
		StringBuilder sb = new StringBuilder();
		if(StringUtil.isEmpty(msg.getTableSystemCode()))
		{
			log.error("table system code is null");
			setErrorReason("table system code is null");
			return ERROR;
		}
		//取所有基本数据项或基本指示
		List<Dictionary> dl = DictService.getInstance().getDictListByTableSystemCodeWithOutCache(msg.getTableSystemCode());
		if(dl!=null)
		{
			for(Dictionary d :dl)
			{
				if(!StringUtil.isEmpty(d.getShowName())&&d.getType()>=0&&d.getType()<=6&&d.getBitSet().charAt(0)=='1')
				{
					
					sb.append(d.getShowName()+":"+d.getIndexCode());
					sb.append(";");
				}
			}
		}
		else {
			log.info("getCfiruleListByTid :"+msg);
			setErrorReason("query failed！");
		}
		String ret = sb.toString();
		//去掉尾部的分号
		if(sb.toString().endsWith(";"))
		{
			ret = sb.substring(0, sb.length()-1);
		}
		//this.setResultData(ret);
		StockUtil.outputJson(this.getHttpServletResponse(),"retObj","'"+ret+"'");
	} catch (Exception e) {
		log.error("execute /cfirule/getCfiruleList failed", e);
		setErrorReason("add failed！");
		return ERROR;
	}
	return SUCCESS;
	}
	
	@SuppressWarnings("unchecked")
	@Action(value = "/cfirule/getCfiruleList")
	public String getCfiruleList() {
		try {
			StringBuilder sb = new StringBuilder();
			if(StringUtil.isEmpty(msg.getTableSystemCode()))
			{
				log.error("table system code is null");
				setErrorReason("table system code is null");
				return ERROR;
			}
			String icode = "";
			if(!StringUtil.isEmpty(msg.getCompanyCode()))
			{
				Company cc = CompanyService.getInstance().getCompanyByCodeFromCache(msg.getCompanyCode());
//				icode = cc.getIndustryCode();
			}
			
			//取所有基本数据项或基本指示
			List<Dictionary> dl = DictService.getInstance().getDictListByTableSystemCode(StockUtil.getTableSystemCodeByDs(msg.getDataSourceCode(),icode));
			if(dl!=null)
			{
				for(Dictionary d :dl)
				{
					if(!StringUtil.isEmpty(d.getShowName())&&d.getType()>=0&&d.getType()<=6&&d.getBitSet().charAt(0)=='1')
					{
						
						sb.append(d.getIndexCode() + ":" + d.getShowName()+":"+d.getTableName()+":"+d.getType());
						sb.append(";");
					}
				}
			}
			else {
				setErrorReason("query failed！");
			}
			String ret = sb.toString();
			//去掉尾部的分号
			if(sb.toString().endsWith(";"))
			{
				ret = sb.substring(0, sb.length()-1);
			}
			this.setResultData(ret);
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	
	
	
	public IndexMessage getMsg() {
		return msg;
	}

	public void setMsg(IndexMessage msg) {
		this.msg = msg;
	}
	public Cfirule getRule() {
		return rule;
	}
	public void setRule(Cfirule rule) {
		this.rule = rule;
	}


}
