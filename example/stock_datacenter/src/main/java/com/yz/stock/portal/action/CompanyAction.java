package com.yz.stock.portal.action;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Industry;
import com.stock.common.model.USubject;
import com.stock.common.util.DateUtil;
import com.stock.common.util.NetUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.IndustryService;
import com.yfzx.service.db.USubjectService;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.stock.common.BaseAction;
import com.yz.stock.portal.cache.CompanyCacheLoadService;

public class CompanyAction extends BaseAction {

	private static final long serialVersionUID = -2019258911147197216L;
	CompanyService cs = CompanyService.getInstance();
	private IndexMessage msg = new IndexMessage();
	Logger log = LoggerFactory.getLogger(this.getClass());
	private String vmContent;

	@SuppressWarnings("unchecked")
	@Action(value = "/company/getCompanyListString")
	public String getCompanyListString() {
		try {
			// 先从缓存中取组装好的company String,如果取不到,则再组装
			Object o = LCEnter.getInstance().get(getStringKey(),
					CacheUtil.getCacheName(StockConstants.COMPANY_TYPE));
			if (o == null) {
				// 只初始一次
				synchronized (this) {
					if (o == null) {
						StringBuilder sb = new StringBuilder();
						Object value = cs.getCompanyList();
						if (value != null) {
							List<Company> cl = (List<Company>) value;
							for (Company c : cl) {
								// sb.append(c.getCompanyCode() + " : "
								// + c.getCompanyNameChi());
								sb.append(c.getSimpileName() + ":"
										+ c.getCompanyCode());
								sb.append(";");
							}
							o = sb.toString();
							put2Cache(o);

						}
					}
				}
			}
			this.setResultData(o);
			return SUCCESS;

		} catch (Exception e) {
			// TODO: handle exception
			log.error("opt failed!", e);
		}
		return ERROR;
	}

	@SuppressWarnings("unchecked")
	@Action(value = "/company/getCompanyByTableSystemString")
	public String getCompanyByTableSystemString() {
		try {
			String skey = getStringKey() + "." + msg.getTableSystemCode();
			// 先从缓存中取组装好的company String,如果取不到,则再组装
			Object o = LCEnter
					.getInstance()
					.get(skey,
							CacheUtil
									.getCacheName(StockConstants.COMPANY));
			if (o == null) {
				// 只初始一次
				synchronized (this) {
					if (o == null) {
						StringBuilder sb = new StringBuilder();
						List<Company> cl = LCEnter
								.getInstance()
								.get(msg.getTableSystemCode(),
										CacheUtil
												.getCacheName(StockConstants.COMPANY));
						if (cl != null) {
							for (Company c : cl) {
								sb.append(c.getSimpileName() + ":"
										+ c.getCompanyCode());
								sb.append(";");
							}
							String s = sb.toString();
							if (s.endsWith(";"))
								s = sb.substring(0, s.length() - 1);
							o = s;

							// 放入缓存
							LCEnter.getInstance()
									.put(skey,
											o,
											CacheUtil
													.getCacheName(StockConstants.COMPANY));
						}
					}
				}

			}
			this.setResultData(o);
			return SUCCESS;

		} catch (Exception e) {
			// TODO: handle exception
			log.error("opt failed!", e);
		}
		return ERROR;
	}

	@SuppressWarnings("unchecked")
	@Action(value = "/company/getCompanyByTableSystemString_v2")
	public String getCompanyByTableSystemString_v2() {
		try {
			String skey = getStringKey() + "." + msg.getTableSystemCode()+".v2";
			// 先从缓存中取组装好的company String,如果取不到,则再组装
			Object o = LCEnter
					.getInstance()
					.get(skey,
							CacheUtil
									.getCacheName(StockConstants.COMPANY));
			if (o == null) {
				// 只初始一次
				synchronized (this) {
					if (o == null) {
						StringBuilder sb = new StringBuilder();
						Object value = LCEnter
								.getInstance()
								.get(msg.getTableSystemCode(),
										CacheUtil
												.getCacheName(StockConstants.COMPANY));
						if (value != null) {
							List<Company> cl = (List<Company>) value;
							for (Company c : cl) {
//								Stock0001 s = Stock0001Service.getInstance().getStock0001ByCompanycodeFromCache(c.getCompanyCode());
//								if(s==null) continue;
//								String jx = s.getF001v();
								String jx = c.getPyjx();
								sb.append(c.getSimpileName() + ":"
										+ c.getCompanyCode()+":"+jx);
								sb.append(";");
							}
							String s = sb.toString();
							if(s.endsWith(";"))
								s = sb.substring(0, s.length()-1);
							o = s;
							
							// 放入缓存
							LCEnter.getInstance()
									.put(skey,
											o,
											CacheUtil
													.getCacheName(StockConstants.COMPANY));
						}
					}
				}

			}
			if(o!=null)
			{
				String ret = o.toString();
				StockUtil.outputJson(this.getHttpServletResponse(),"retObj","'"+ret+"'");
			}
			return SUCCESS;

		} catch (Exception e) {
			// TODO: handle exception
			log.error("opt failed!", e);
		}
		return ERROR;
	}
	
	/**
	 * 不分报表体系，所有公司
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Action(value = "/company/getCompanyByTableSystemString_v3")
	public String getCompanyByTableSystemString_v3() {
		try {
			String skey = "allcompanyListString";
			// 先从缓存中取组装好的company String,如果取不到,则再组装
			Object o = LCEnter
					.getInstance()
					.get(skey,
							CacheUtil
									.getCacheName(StockConstants.COMPANY));
			if (o == null) {
				// 只初始一次
				synchronized (this) {
					if (o == null) {
						StringBuilder sb = new StringBuilder();
						List<USubject> cl = USubjectService.getInstance().getUSubjectAHZList();
						if (cl != null) {
							for (USubject c : cl) {
//								Stock0001 s = Stock0001Service.getInstance().getStock0001ByCompanycodeFromCache(c.getCompanyCode());
//								if(s==null) continue;
//								String jx = s.getF001v();
								String jx = c.getShortPinyin();
								if(StringUtil.isEmpty(jx))
									continue;
								sb.append(c.getName() + ":"
										+ c.getUidentify()+":"+jx.toUpperCase());
								sb.append(";");
							}
							String s = sb.toString();
							if(s.endsWith(";"))
								s = sb.substring(0, s.length()-1);
							o = s;
							
							// 放入缓存
							LCEnter.getInstance()
									.put(skey,
											o,
											CacheUtil
													.getCacheName(StockConstants.COMPANY));
						}
					}
				}

			}
			if(o!=null)
			{
				String ret = o.toString();
				StockUtil.outputJson(this.getHttpServletResponse(),"retObj","\""+ret+"\";");
			}else{
				StockUtil.outputJson(this.getHttpServletResponse(),"retObj","null");
			}
			return SUCCESS;

		} catch (Exception e) {			
			StockUtil.outputJson(this.getHttpServletResponse(),"retObj","null");
			log.error("opt failed!", e);
		}
		return ERROR;
	}
	

	@Action(value = "/company/getCompanyByTagWihtShort")
	public String getCompanyByTagWihtShort() {
		try {
			String tag = this.getHttpServletRequest().getParameter("tag");
			String companycode = this.getHttpServletRequest().getParameter("companycode");
			if (StringUtil.isEmpty(tag)&&!StringUtil.isEmpty(companycode)) {
				Company c = CompanyService.getInstance().getCompanyByCode(
						companycode);
				if (c == null)
					return null;
				tag = CompanyService.getInstance().getCompanyByCode(companycode).getMainTag();
			}
			if(StringUtil.isEmpty(tag))
				return ERROR;
						StringBuilder sb = new StringBuilder();
						List<Company> cl = CompanyService.getInstance().getCompanyListByTagFromCache(tag);
						if (cl != null) {
							for (Company c : cl) {
//								Stock0001 s = Stock0001Service.getInstance().getStock0001ByCompanycodeFromCache(c.getCompanyCode());
//								if(s==null) continue;
//								String jx = s.getF001v();
								String jx = c.getPyjx();
								sb.append(c.getSimpileName() + ":"
										+ c.getCompanyCode()+":"+jx.toUpperCase());
								sb.append(";");
							}
						}

				String s = sb.toString();
				if(s.endsWith(";"))
					s = sb.substring(0, s.length()-1);
				StockUtil.outputJson(this.getHttpServletResponse(),"retObj","'"+s+"'");
			return SUCCESS;

		} catch (Exception e) {
			// TODO: handle exception
			log.error("opt failed!", e);
		}
		return ERROR;
	}
	@SuppressWarnings("rawtypes")
	@Action(value = "/company/getComanyByIndustry")
	public String getComanyByTag() {
		try {
			List csl = null;
			// 解析公司列表
			String[] ca = msg.getCompanyList().split("\\|");
			// 取每个公司对应的指标数据
			for (String cc : ca) {
				String[] tmp = cc.split(":");
				if (tmp.length != 2) {
					continue;
				}
				// String companyName = tmp[0];
				String companyCode = tmp[1];
				Company c = cs.getCompanyByCode(companyCode);

				// 任取一个标签
				String tags = c.getTags();
				String tagcode = "";
				if (!StringUtil.isEmpty(tags))
					tagcode = tags.split(";")[0];
				if (StringUtil.isEmpty(tagcode))
					return ERROR;
				csl = cs.getCompanyListByTagFromCache(tagcode);
			}

			if (csl != null) {
				this.setResultData(csl);
				return SUCCESS;
			}
		} catch (Exception e) {
			// TODO: handle exception
			log.error("opt failed!", e);
		}
		return ERROR;
	}

	@SuppressWarnings("rawtypes")
	@Action(value = "/company/getComanyAllTags")
	public String getComanyAllTags() {
		try {
			// 解析公司列表
			String companycode = this.getHttpServletRequest().getParameter(
					"companycode");
			if (StringUtil.isEmpty(companycode))
				return ERROR;
			companycode = companycode.trim();
			Company c = cs.getCompanyByCodeFromCache(companycode);
			String ctags = c.getTags();
			List<String> ls = new ArrayList<String>();
			if (!StringUtil.isEmpty(ctags)) {

				String[] ta = c.getTags().split(";");
				// 如果不在缓存中存在的tag不加入
				for (String t : ta) {
					Object o = cs.getCompanyListByTagFromCache(t);
					if (o != null) {
						ls.add(t);
					}
				}

			}
			this.setResultData(ls);

		} catch (Exception e) {
			// TODO: handle exception
			log.error("opt failed!", e);
			return ERROR;
		}
		return SUCCESS;
	}

	/**
	 * 根据公司code查询到公司资料 包括f042v行业
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	@Action(value = "/company/getCompanyByCode")
	public String getCompanyByCode() {
		String companycode = this.getHttpServletRequest().getParameter(
				"companycode");
		Company c = cs.getCompanyByCode(companycode);
		this.setResultData(c);
		return SUCCESS;
	}

	@SuppressWarnings("rawtypes")
	@Action(value = "/company/getCompanyCodeBySimplename")
	public String getCompanyCodeBySimplename() {
		String companyname = this.getHttpServletRequest().getParameter(
				"companyname");
		Company c = cs.getCompanyBySimpleNameFromCache(companyname);
		if (c != null)
			this.setResultData(c.getCompanyCode());
		return SUCCESS;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Action(value = "/company/getComanyListByTags")
	public String getComanyListByTags() {
		try {
			String tag = this.getHttpServletRequest().getParameter("tag");
			String companycode = this.getHttpServletRequest().getParameter(
					"companycode");

			if (StringUtil.isEmpty(tag) && StringUtil.isEmpty(companycode))
				return ERROR;
			if (StringUtil.isEmpty(tag) && !StringUtil.isEmpty(companycode)) {
				Industry ind = IndustryService.getInstance()
						.getIndustryByCompanycode(companycode);
				if (ind != null)
					tag = ind.getName();
			}

			if (StringUtil.isEmpty(tag))
				return ERROR;

			List<Company> cl = cs.getCompanyListByTagFromCache(tag);
			StringBuilder sb = new StringBuilder();
			for (Company c : cl) {
				sb.append(c.getSimpileName() + ":" + c.getCompanyCode());
				sb.append(";");
			}
			// this.setResultData(sb.toString());
			output(sb.toString());
		} catch (Exception e) {
			// TODO: handle exception
			log.error("opt failed!", e);
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
	@Action(value = "/company/getCompanyIncomeComposeByOneTime")
	public String getCompanyIncomeComposeByOneTime() {
		try {
			String companycode = this.getHttpServletRequest().getParameter(
					"companycode");
			Date time = NetUtil.getParameterDate(this.getHttpServletRequest(),
					"time", StockUtil.getApproPeriod(new Date()));
			String type = this.getHttpServletRequest().getParameter("type");

			Map m = CompanyService.getInstance()
					.getCompanyIncomeComposeByOneTime(companycode, time, type);

			this.setResultData(m);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}



	@SuppressWarnings("rawtypes")
	@Action(value = "/company/getDateUpdateCompanyByTime")
	public String getDateUpdateCompanyByTime() {
		try {
			String uptime = this.getHttpServletRequest().getParameter("upTime");
			List<String> tsl = LCEnter.getInstance().get(
					StockConstants.TABLE_SYSTEM_LIST_CACHE_KEY,
					StockConstants.MATCH_INFO_CACHE);
			List<String> asl = new ArrayList<String>();
			for (String tsc : tsl) {
				List<String> cList = CompanyService.getInstance()
						.getConpanyListOfDateUpdate(tsc, DateUtil.format(uptime));
				if (cList != null)
					asl.addAll(cList);
			}
			StringBuffer sb = new StringBuffer();
			for (String ccode : asl) {
				Company c = CompanyService.getInstance()
						.getCompanyByCode(ccode);
				if (c != null) {
					sb.append(c.getSimpileName());
					sb.append(":");
					sb.append(c.getCompanyCode());
					sb.append(";");
				}

			}
			this.setResultData(sb.toString());
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	@Action(value = "/tagrule/delCompanysCfTags")
	public String delCompanysCfTags() {
		
		try {
			List<Company> cl = cs.getCompanyList();
			for (Company c : cl) {
				String tags = c.getTags();
				String[] tagArr = tags.split(";");
				StringBuilder sb = new StringBuilder();
				for(String tag:tagArr)
				{
					if(!tag.startsWith(StockConstants.CF_TAG_PREFIX))
					{
						sb.append(tag);
						sb.append(";");
					}
				}
				cs.delTagsOfCompany(c.getCompanyCode(), sb.toString());
				
			}
			refreshCompanyCache();
		} catch (Exception e) {
			log.error(e.toString());
			e.printStackTrace();
		}
		// this.setResultData(ce);
		return SUCCESS;
	}
	
	private void refreshCompanyCache() {
		CompanyCacheLoadService cls = new CompanyCacheLoadService();
		cls.init();
		cls.loadData2Cache();
		
	}
	

	private void put2Cache(Object value) {
		// TODO Auto-generated method stub
		LCEnter.getInstance().put(getStringKey(), value,
				CacheUtil.getCacheName(StockConstants.COMPANY_TYPE));
	}

	private String getStringKey() {
		// TODO Auto-generated method stub
		return "String.ajax.company";
	}

	public IndexMessage getMsg() {
		return msg;
	}

	public void setMsg(IndexMessage msg) {
		this.msg = msg;
	}

	public String getVmContent() {
		return vmContent;
	}

	public void setVmContent(String vmContent) {
		this.vmContent = vmContent;
	}
}
