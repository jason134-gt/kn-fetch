package com.yz.stock.portal.action;

import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;

import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Dictionary;
import com.stock.common.model.tree.RuleTreeNode;
import com.stock.common.model.tree.SimpleRuleTreeNode;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.tree.RuleTreeService;
import com.yz.stock.common.BaseAction;

public class RuleTreeAction extends BaseAction {

	private static final long serialVersionUID = 8989251888164873509L;
	Logger log = LoggerFactory.getLogger(this.getClass());
	


	
	@Action(value = "/ruleparse/parseAllRule2Tree")
	public String parseAllRule2Tree() {
		try {
			RuleTreeService.getInstance().parseAllRule2Tree();

		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/ruleparse/parseOneIndexRule2TreeV2")
	public String parseOneIndexRule2TreeV2() {
		try {
			int depth = 3;
			String indexcode = this.getHttpServletRequest().getParameter("indexcode");
			String sdepth = this.getHttpServletRequest().getParameter("depth");
			String companycodea = this.getHttpServletRequest().getParameter("companycodea");
			String companycodeb = this.getHttpServletRequest().getParameter("companycodeb");
			String timea = this.getHttpServletRequest().getParameter("timea");
			String timeb = this.getHttpServletRequest().getParameter("timeb");//参数都必填
			if(!StringUtil.isEmpty(indexcode))
			{
				Dictionary d = DictService.getInstance().getDataDictionary(indexcode);
				if(d==null) 
					return ERROR;
				String nnrc = RuleTreeService.getInstance().getNewRuleComments(d.getIndexCode());
				if(StockUtil.isBaseIndex(d.getType())&&StringUtil.isEmpty(nnrc))
					return ERROR;
			}

			if(!StringUtil.isEmpty(sdepth))
				depth = Integer.valueOf(sdepth);
			if(StringUtil.isEmpty(companycodea) || StringUtil.isEmpty(companycodeb) ||
					StringUtil.isEmpty(timea) || StringUtil.isEmpty(timeb) ){
				return ERROR;
			}
			List<RuleTreeNode> rl = new ArrayList<RuleTreeNode>();
			if(!StringUtil.isEmpty(indexcode))
			{
				RuleTreeNode rtn = RuleTreeService.getInstance().getRuleTreeNodeFromCache(indexcode);
				rl.add(rtn);
			}
			else
			{
				//取所有没有父结点的指标
				List<RuleTreeNode> arl = RuleTreeService.getInstance().getAllNoParentRuleTreeNodeFromCache();
				rl.addAll(arl);
			}
			//每次只取一层
			if(rl.size()!=0)
			{
				List<SimpleRuleTreeNode> srnl = new ArrayList<SimpleRuleTreeNode>();
				for(RuleTreeNode rtn:rl)
				{
					SimpleRuleTreeNode  srn = 
							RuleTreeService.getInstance().
							getSimpleRuleTreeNode2CompnaySameTime(rtn, depth, 0, companycodea, companycodeb, DateUtil.format(timea), DateUtil.format(timeb));
					String showRule = RuleTreeService.getIndexcodeShowRule(indexcode);
					if(!StringUtil.isEmpty(showRule))
						srn.setShowRule(showRule);
					if(srn!=null)
						srnl.add(srn);
				}
				JSONArray jsona = new JSONArray();
				jsona.add(srnl);
				StockUtil.outputJson(this.getHttpServletResponse(), "retobj", jsona);
			}
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/ruleparse/parseOneIndexRule2Tree")
	public String parseOneIndexRule2Tree() {
		try {
			int depth = 3;
			String indexcode = this.getHttpServletRequest().getParameter("indexcode");
			String sdepth = this.getHttpServletRequest().getParameter("depth");
			String companycodea = this.getHttpServletRequest().getParameter("companycodea");
			String companycodeb = this.getHttpServletRequest().getParameter("companycodeb");
			String settime = this.getHttpServletRequest().getParameter("settime");
			if(!StringUtil.isEmpty(indexcode))
			{
				Dictionary d = DictService.getInstance().getDataDictionary(indexcode);
				if(d==null) 
					return ERROR;
				if(StockUtil.isBaseIndex(d.getType()))
					return ERROR;
			}

			if(!StringUtil.isEmpty(sdepth))
				depth = Integer.valueOf(sdepth);
			List<RuleTreeNode> rl = new ArrayList<RuleTreeNode>();
			if(!StringUtil.isEmpty(indexcode))
			{
				RuleTreeNode rtn = RuleTreeService.getInstance().getRuleTreeNodeFromCache(indexcode);
				rl.add(rtn);
			}
			else
			{
				//取所有没有父结点的指标
				List<RuleTreeNode> arl = RuleTreeService.getInstance().getAllNoParentRuleTreeNodeFromCache();
				rl.addAll(arl);
			}
			//每次只取一层
			if(rl.size()!=0)
			{
				List<SimpleRuleTreeNode> srnl = new ArrayList<SimpleRuleTreeNode>();
				for(RuleTreeNode rtn:rl)
				{
					SimpleRuleTreeNode  srn =null;
					if(!StringUtil.isEmpty(companycodea)&&!StringUtil.isEmpty(companycodeb))
					{
						srn =  RuleTreeService.getInstance().getSimpleRuleTreeNode2CompnaySameTime(rtn,depth, 0,companycodea,companycodeb);
					}
					if(!StringUtil.isEmpty(companycodea)&&StringUtil.isEmpty(companycodeb))
					{
						srn =  RuleTreeService.getInstance().getSimpleRuleTreeNode1CompanySetTime(rtn,depth, 0,companycodea,DateUtil.format(settime));
					}
					
					if(StringUtil.isEmpty(companycodea)&&StringUtil.isEmpty(companycodeb))
					{
						srn =  RuleTreeService.getInstance().getSimpleRuleTreeNode(rtn,depth, 0);
					}
					if(srn!=null)
						srnl.add(srn);
				}
//				JSONObject json = new JSONObject();
//				json.put("data", srnl);
				JSONArray jsona = new JSONArray();
				jsona.add(srnl);
				StockUtil.outputJson(this.getHttpServletResponse(), "retobj", jsona);
			}
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/ruleparse/parseOneIndexRule2TreeReal")
	public String parseOneIndexRule2TreeReal() {
		try {
			String indexcode = this.getHttpServletRequest().getParameter("indexcode");
			if(StringUtil.isEmpty(indexcode))
				return ERROR;
			Dictionary d = DictService.getInstance().getDataDictionary(indexcode);
			if(d==null) 
				return ERROR;
			if(StockUtil.isBaseIndex(d.getType()))
				return ERROR;
			RuleTreeNode rtn = RuleTreeService.getInstance().parseRuleTree(indexcode);
			//每次只取一层
			if(rtn!=null)
			{
				SimpleRuleTreeNode  srn = RuleTreeService.getInstance().getSimpleRuleTreeNode(rtn,3, 0);
				this.setResultData(srn);
				
			}
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
}
