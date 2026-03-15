package com.yz.stock.portal.action;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Dictionary;
import com.stock.common.model.Industry;
import com.stock.common.model.SPoint;
import com.stock.common.model.Scatter;
import com.stock.common.util.NetUtil;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.stock.common.util.TreeNode;
import com.stock.common.util.UnitUtil;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndustryService;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.stock.common.BaseAction;

/**
 * 
 * 
 * @author user
 * 
 */
public class IndustryAction extends BaseAction {

	Logger log = LoggerFactory.getLogger(this.getClass());
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static boolean isinitIndustryData = false;
	static Set<String> timeSet = new HashSet<String>();
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	CompanyService cs = CompanyService.getInstance();
	DictService ds = DictService.getInstance();
	CRuleService crs = CRuleService.getInstance();
	private InputStream inputStream;
	
	/**
	 * type : 
	 * z:证券所
	 * y:盈富在线
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@Action(value = "/industry/getIndustryByTypeAndCode")
	public String getIndustryByTypeAndCode() {
		try {
			String type = this.getHttpServletRequest().getParameter("type");
			String industrycode = this.getHttpServletRequest().getParameter("industrycode");

			if(StringUtil.isEmpty(type)||StringUtil.isEmpty(industrycode))
				return ERROR;
			IndustryService is = IndustryService.getInstance();
			List l = null;
			if("z".equals(type))
			{
				l = is.getIndustryCSRCTreeFromCache(0).get(industrycode).getChildrenIndustry();
			}
			if("s".equals(type))
			{
				l = is.getIndustryYFZXTree().get(industrycode).getChildrenIndustry();
			}
			this.setResultData(l);
			 
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/industry/getYFZXIndustryChildrenByName")
	public String getYFZXIndustryChildrenByName() {
		try {
			String name = this.getHttpServletRequest().getParameter("name");
			String type = this.getHttpServletRequest().getParameter("type");
			if(StringUtil.isEmpty(name))
				return ERROR;
			IndustryService is = IndustryService.getInstance();
			List<Industry> l = is.getIndustryYFZXTreeFromCache(1).get(name).getChildrenIndustry();
			
			if(l!=null)
			{
				List<Industry> il = l;
				//type =3 ,只取最底层结点
				if(!StringUtil.isEmpty(type)&&"2".equals(type))
				{
					il = new ArrayList<Industry>();
					for(Industry ind : l)
					{
						TreeNode tind = is.getIndustryYFZXTreeFromCache(1).get(ind.getName());
						if(tind==null) continue;
						List<Industry> cil = tind.getLeafChildrenIndustry();
						if(cil!=null&&cil.size()!=0)
						{
							il.addAll(cil);
						}
						else
						{
							il.add(ind);
						}
					}
					l = il;
				}

				this.setResultData(l);
			}
			 
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	@Action(value = "/industry/getYfzxAllIndustryJsonData")
	public String getYfzxAllIndustryJsonData() {
		try {

			IndustryService is = IndustryService.getInstance();
			String jsons = is.getYfzxAllIndustryJsonData();
			StockUtil.outputJson(this.getHttpServletResponse(), "yfzxjd", jsons);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	/**
	 * 取某一标签的最大值，最小值，平均值，中值
	 * @return
	 */
	@Action(value = "/industry/getTagMMAM")
	public String getTagMMAM() {

		try {
			String tag = this.getHttpServletRequest().getParameter("tag");
			Date time = NetUtil.getParameterDate(this.getHttpServletRequest(),
					"time", StockUtil.getApproPeriod(new Date()));
			String indexcode = this.getHttpServletRequest().getParameter("indexcode");
			if(StringUtil.isEmpty(tag)||StringUtil.isEmpty(indexcode))
				return ERROR;
			
			String mmam = IndustryService.getInstance().getMMAM(tag, indexcode, time);
			
			this.setResultData(mmam);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	/**
	 * 取所有没有子行业的行业数据
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@Action(value = "/industry/getAllNoChildIndustry")
	public String getAllNoChildIndustry() {
		try {

			IndustryService is = IndustryService.getInstance();
			List<Industry> l = null;
			String ret = "";
			StringBuffer sb = new StringBuffer();
			l = is.getIndustryCSRCTreeFromCache(0).get(IndustryService.root).getLeafChildrenIndustry();
			for(Industry ind : l)
			{
				sb.append(ind.getName()+":"+ind.getIndustryCode());
				sb.append(";");
			}
			ret = sb.toString();
			if(ret.endsWith(",")) ret = ret.substring(0, ret.length()-1);
			
			output(ret);
			 
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	/**
	 * 取所有行业，包括1到4级
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Action(value = "/industry/getAllIndustry")
	public String getAllIndustry() {
		try {

			IndustryService is = IndustryService.getInstance();
			List<Industry> l = null;
			String ret = "";
			StringBuffer sb = new StringBuffer();
			l = is.getIndustryCSRCTreeFromCache(0).get(IndustryService.root).getChildAndChild();
			for(Industry ind : l)
			{
				sb.append(ind.getName()+":"+ind.getIndustryCode());
				sb.append(";");
			}
			ret = sb.toString();
			if(ret.endsWith(",")) ret = ret.substring(0, ret.length()-1);
			
			output(ret);
			 
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	/**
	 * 行业分析页面使用，提供行业的上级层次，并提供子行业
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Action(value = "/industry/getIndustryCSRCPath")
	public String getIndustryCSRCPath(){
		try {
			String tagname = this.getHttpServletRequest().getParameter("tagname");
			String tagcode = this.getHttpServletRequest().getParameter("tagcode");
			IndustryService is = IndustryService.getInstance();
			String reStr = "";
			TreeNode tn = null;
			if(!StringUtil.isEmpty(tagcode)){
				tn  = is.getIndustryCSRCTreeFromCache(0).get(tagcode);
				
			}
			if(!StringUtil.isEmpty(tagname)){
				if(tn == null){
					tn = is.getIndustryCSRCTreeFromCache(1).get(tagname);
				}
			}
			reStr = tn.getXPath();//本行业及它的上级行业
			List<Industry> l = tn.getChildAndChild();//子行业
			StringBuffer sb = new StringBuffer();
			for(Industry ind : l){
				sb.append(ind.getName()+":"+ind.getIndustryCode());
				sb.append(";");
			}
			reStr += "#"+ sb.toString();//以#分隔
			output(reStr);
		}catch(Exception e){
			e.printStackTrace();
			return ERROR;
		}		
		return SUCCESS;
	}
	
	/**
	 * 取公司的所有标签
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Action(value = "/industry/getAllTags")
	public String getAllTags() {
		try {

			List<String> ls = CompanyService.getInstance().getAllTags();
			JSONObject jso = new JSONObject();
			jso.put("tags", ls);
			StockUtil.outputJson(this.getHttpServletResponse(), "retobj", jso);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	private void output(String responseText) {
		// 获取原始的PrintWriter对象,以便输出响应结果,而不用跳转到某个试图
		HttpServletResponse response = ServletActionContext.getResponse();
		// 设置字符集
		response.setContentType("text/plain");// 设置输出为文字流
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = null;
		try {
			out = response.getWriter();
			// 直接输出响应的内容
			out.println(responseText);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	

	@SuppressWarnings("rawtypes")
	@Action(value = "/industry/getscatterFhs_H")
	public String getscatterFhs_H() {
		try {

			String industrytag = this.getHttpServletRequest().getParameter("industrytag");
			Date time = NetUtil.getParameterDate(this.getHttpServletRequest(),
					"time", StockUtil.getApproPeriod(new Date()));
			String type = this.getHttpServletRequest().getParameter("type");
			String indexcode = "2022";//净资产收益率Roe
			String xindexcode = "1881";//权益乘数，现在系统中没有定义此指标 ，权益乘数 = 1/(1-资产负债率)
			String yindexcode = "2016";//资产收益率Roa

			String xminv = this.getHttpServletRequest().getParameter("xminv");
			String xmaxv = this.getHttpServletRequest().getParameter("xmaxv");
			String yminv = this.getHttpServletRequest().getParameter("yminv");
			String ymaxv = this.getHttpServletRequest().getParameter("ymaxv");
			
			
			if(StringUtil.isEmpty(industrytag)||StringUtil.isEmpty(type))
				return ERROR;
			
			Double dxminv = null;
			Double dxmaxv = null;
			Double dyminv = null;
			Double dymaxv = null;
			if (!StringUtil.isEmpty(xminv))
				dxminv = Double.valueOf(xminv);
			if (!StringUtil.isEmpty(xmaxv))
				dxmaxv = Double.valueOf(xmaxv);
			if (!StringUtil.isEmpty(yminv))
				dyminv = Double.valueOf(yminv);
			if (!StringUtil.isEmpty(ymaxv))
				dymaxv = Double.valueOf(ymaxv);
			
			Scatter sct = getScatterH(industrytag,time,xindexcode,yindexcode, dxminv, dxmaxv,dyminv, dymaxv);
			if(sct==null) return ERROR;
			
	
			Double indexavg = IndustryService.getInstance().getMaxMinAvgMidOneTimeFromDB(industrytag, StockConstants.ravgType, indexcode, time);
			if(indexavg!=null) sct.setIndexavg(indexavg);
			
			

			this.setResultData(sct);
			 
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@SuppressWarnings("rawtypes")
	@Action(value = "/industry/getscatter",
			results = { @Result(name = "success",type="stream",params={"contentType","application/json;charset=UTF-8"})})
	public String getscatter(){
		try {
			String industrytag = this.getHttpServletRequest().getParameter("industrytag");
			Date time = NetUtil.getParameterDate(this.getHttpServletRequest(),
					"time", StockUtil.getApproPeriod(new Date()));
			String type = this.getHttpServletRequest().getParameter("type");
			
			String xindexcode = this.getHttpServletRequest().getParameter("xindexcode");
			String yindexcode = this.getHttpServletRequest().getParameter("yindexcode");
	
			String xminv = this.getHttpServletRequest().getParameter("xminv");
			String xmaxv = this.getHttpServletRequest().getParameter("xmaxv");
			String yminv = this.getHttpServletRequest().getParameter("yminv");
			String ymaxv = this.getHttpServletRequest().getParameter("ymaxv");
			
			if(StringUtil.isEmpty(industrytag)||StringUtil.isEmpty(type))
				return ERROR;
			Double dxminv = null;
			Double dxmaxv = null;
			Double dyminv = null;
			Double dymaxv = null;
			if (!StringUtil.isEmpty(xminv))
				dxminv = Double.valueOf(xminv);
			if (!StringUtil.isEmpty(xmaxv))
				dxmaxv = Double.valueOf(xmaxv);
			if (!StringUtil.isEmpty(yminv))
				dyminv = Double.valueOf(yminv);
			if (!StringUtil.isEmpty(ymaxv))
				dymaxv = Double.valueOf(ymaxv);
			
			Scatter sct = getScatterH(industrytag,time,xindexcode,yindexcode, dxminv, dxmaxv,dyminv, dymaxv);
			if(sct==null) return ERROR;
			//绝对值用平均值，比率用行业值
			String xtype = StockConstants.avgType;
			Dictionary d = DictService.getInstance().getDataDictionary(xindexcode);
			if(d.getUnit()==UnitUtil.unit_0)
				xtype = StockConstants.ravgType;
			
			Double xavg = IndustryService.getInstance().getMaxMinAvgMidOneTimeFromDB(
					industrytag, xtype, xindexcode, time);
			if (xavg != null)
				sct.setXavg(xavg);

			//绝对值用平均值，比率用行业值
			String ytype = StockConstants.avgType;
			 d = DictService.getInstance().getDataDictionary(yindexcode);
			if(d.getUnit()==UnitUtil.unit_0)
				ytype = StockConstants.ravgType;
			Double yavg = IndustryService.getInstance().getMaxMinAvgMidOneTimeFromDB(
					industrytag, ytype, yindexcode, time);
			if (yavg != null)
				sct.setYavg(yavg);

			//this.setResultData(sct);
			//struts本身的返回JSON有问题,此处使用流模式返回JSON,注意注解配置的contentType=application/json;charset=UTF-8
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("data", sct);
			jsonObject.put("success", true);
			jsonObject.element("totalCount", 0);
			String text = jsonObject.toString();
			try {
				setInputStream(new ByteArrayInputStream(text.getBytes("utf-8")));
			} catch (IOException e) {			
				e.printStackTrace();
				return ERROR;
			}			 
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@SuppressWarnings("unchecked")
	private Scatter getScatterH(String tagcode,Date time,
			String xindexcode, String yindexcode, Double xminv, Double xmaxv, Double yminv, Double ymaxv) {
		if(StringUtil.isEmpty(xindexcode)||StringUtil.isEmpty(yindexcode))
			return null;

		List<Industry> li = IndustryService.getInstance().getPeerIndustry(tagcode);

		if(li==null||li.size()==0) return null;
		
		Scatter sct = new Scatter();
		for(Industry ind : li)
		{
			String ttagcode = ind.getIndustryCode();
			String tagname = ind.getName();
			Double xv = IndustryService.getInstance().getMaxMinAvgMidOneTimeWithCache(tagname, StockConstants.ravgType, xindexcode, time,true);
			Double yv = IndustryService.getInstance().getMaxMinAvgMidOneTimeWithCache(tagname, StockConstants.ravgType, yindexcode, time,true);
			if(xv==null||yv==null||xv==0||yv==0)
				continue;
			if ((xminv!=null&&xv < xminv) ||(xmaxv!=null && xv > xmaxv + 0.01))
				continue;
			if ((yminv!=null&&yv < yminv) ||(ymaxv!=null && yv > ymaxv + 0.01))
				continue;
			
			xv = SMathUtil.getDouble(xv, 2);
			yv = SMathUtil.getDouble(yv, 2);
			SPoint sctp = new SPoint(ttagcode,tagname,xv,yv);
			sct.put(sctp);
		}
		String xindexName = ds.getDataDictionary(xindexcode).getShowName();
		String yindexName = ds.getDataDictionary(yindexcode).getShowName();
		sct.setXname(xindexName);
		sct.setYname(yindexName);
		return sct;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}
}
