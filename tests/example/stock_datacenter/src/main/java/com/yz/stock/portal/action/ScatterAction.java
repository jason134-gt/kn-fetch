package com.yz.stock.portal.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.DateUtil;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.stock.common.util.UnitUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndustryService;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.stock.common.BaseAction;

public class ScatterAction extends BaseAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7268696021004413716L;
	Logger log = LoggerFactory.getLogger(this.getClass());
	CompanyService cs = CompanyService.getInstance();

	
	@Action(value = "/scatter/getScatterIndexData")
	public String getScatterIndexData() {
		try {
			String industrytag = this.getHttpServletRequest().getParameter(
					"tag");
			String companycode = this.getHttpServletRequest().getParameter(
					"companycode");
			String xindexcode = this.getHttpServletRequest().getParameter(
					"xindexcode");
			String yindexcode = this.getHttpServletRequest().getParameter(
					"yindexcode");

			if (StringUtil.isEmpty(xindexcode)
					|| StringUtil.isEmpty(yindexcode)
					|| (StringUtil.isEmpty(industrytag) && StringUtil
							.isEmpty(companycode)))
				return ERROR;
			
			if (StringUtil.isEmpty(industrytag)) {
				if (!StringUtil.isEmpty(companycode)) {
					industrytag = CompanyService.getInstance()
							.getIndustryNameOfCompany(companycode);
				}

				if (StringUtil.isEmpty(industrytag))
					return ERROR;
			}

			if(industrytag.equals(IndustryService.root_name))
				industrytag = IndustryService.root;
			List<Company> cl = null;
			if (industrytag.equals(IndustryService.root)) {
				cl = cs.getCompanyListFromCache();
			} else {
				cl = cs.getCompanyListByTagFromCache(industrytag);
			}
			Dictionary xd = DictService.getInstance().getDataDictionary(xindexcode);
			Dictionary yd = DictService.getInstance().getDataDictionary(yindexcode);
			Map<String,Object> rm = new HashMap<String,Object>();
			StringBuffer vl = new StringBuffer();
			Double xavg = 0.0;
			Double yavg = 0.0;
			int dcount = 0;
			for (Company c : cl) {
				//剔除同一公司在不同地点上市，导致的重复
//				if (CompanyService.getInstance().removeBST(c))
//					continue;
				Date ctime = CompanyService.getInstance().getLatestReportTime(companycode);
				IndexMessage im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
						xindexcode, ctime);
				im1.setNeedAccessExtIndexDb(false);
				im1.setNeedAccessCompanyBaseIndexDb(false);
				Double xv = IndexValueAgent.getIndexValue(im1);
				IndexMessage im2 = SMsgFactory.getUMsg(c.getCompanyCode(),
						yindexcode, ctime);
				im2.setNeedAccessExtIndexDb(false);
				im2.setNeedAccessCompanyBaseIndexDb(false);
				Double yv = IndexValueAgent.getIndexValue(im2);
				if (xv == null || yv == null || xv == 0 || yv == 0)
					continue;
				dcount++;
				xavg+=xv;
				yavg+=yv;
				xv = SMathUtil.getDouble(xv, 6);
				yv = SMathUtil.getDouble(yv, 6);
				
				//处理单位
				xv = UnitUtil.formatByDefaultUnit(xd, xv);
				yv = UnitUtil.formatByDefaultUnit(yd, yv);
				
				vl.append(c.getSimpileName());
				vl.append("^");
				vl.append(c.getCompanyCode());
				vl.append("^");
				vl.append(xv);
				vl.append("^");
				vl.append(yv);
				vl.append("~");
			}
			if (dcount != 0)
			{
				rm.put("xavg", xavg);
				rm.put("yavg", yavg);
				rm.put("vl", vl.toString());
			}
			StockUtil.outputJson(getHttpServletResponse(), "scret", JSONUtil.serialize(rm));
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}


	@Action(value = "/plate/getMutlScatterChartData")
	public String getMutlScatterChartData() {
		try {
			String tag = this.getHttpServletRequest().getParameter(
					"tag");
			String indexs = this.getHttpServletRequest().getParameter(
					"indexs");

			String time = DateUtil.format2String(StockUtil.getDefaultPeriodTime(null));
			List<String> ls = new ArrayList<String>();
			if (StringUtil.isEmpty(indexs)
					|| StringUtil.isEmpty(tag))
				return ERROR;
			
			String[] indexarr = indexs.split("\\~");
			for(String tindexs:indexarr)
			{
				if(StringUtil.isEmpty(tindexs)) continue;
				String ret = getOneChartIndexData(tag,DateUtil.format(time),tindexs);
				if(!StringUtil.isEmpty(ret))
					ls.add(ret);
			}
			StockUtil.outputJson(getHttpServletResponse(), "mscret", JSONUtil.serialize(ls));
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	private String getOneChartIndexData(String tag,Date time, String tindexs) {
		List<Company> cl = null;
		if (tag.equals(IndustryService.root)) {
			cl = cs.getCompanyListFromCache();
		} else {
			cl = cs.getCompanyListByTagFromCache(tag);
		}
		String[] indexa = tindexs.split("\\^");
		String xindexcode = indexa[0];
		String yindexcode = indexa[1];
		Dictionary xd = DictService.getInstance().getDataDictionary(xindexcode);
		Dictionary yd = DictService.getInstance().getDataDictionary(yindexcode);
		StringBuffer rl = new StringBuffer();
		rl.append(xd.getShowName()+":"+xd.getIndexCode());
		rl.append("~");
		rl.append(yd.getShowName()+":"+yd.getIndexCode());
		rl.append("~");
		StringBuffer vl = new StringBuffer();
		Double xavg = 0.0;
		Double yavg = 0.0;
		int dcount = 0;
		for (Company c : cl) {
			//剔除同一公司在不同地点上市，导致的重复
			if (CompanyService.getInstance().removeBST(c))
				continue;
			IndexMessage im1 = SMsgFactory.getUMsg(c.getCompanyCode(),
					xindexcode, time);
			im1.setNeedAccessExtIndexDb(false);
			im1.setNeedAccessCompanyBaseIndexDb(false);
			Double xv = IndexValueAgent.getIndexValue(im1);
			IndexMessage im2 = SMsgFactory.getUMsg(c.getCompanyCode(),
					yindexcode, time);
			im2.setNeedAccessExtIndexDb(false);
			im2.setNeedAccessCompanyBaseIndexDb(false);
			Double yv = IndexValueAgent.getIndexValue(im2);
			if (xv == null || yv == null || xv == 0 || yv == 0)
				continue;
			dcount++;
			xavg+=xv;
			yavg+=yv;
			xv = SMathUtil.getDouble(xv, 6);
			yv = SMathUtil.getDouble(yv, 6);
			
			//处理单位
			xv = UnitUtil.formatByDefaultUnit(xd, xv);
			yv = UnitUtil.formatByDefaultUnit(yd, yv);
			
			vl.append(c.getSimpileName());
			vl.append("^");
			vl.append(c.getCompanyCode());
			vl.append("^");
			vl.append(xv);
			vl.append("^");
			vl.append(yv);
			vl.append("~");
		}
		if (dcount != 0)
		{
			
			rl.append(xavg);
			rl.append("~");
			rl.append(yavg);
			rl.append("~");
			rl.append(vl.toString());
		}
		return rl.toString();
	}
}
