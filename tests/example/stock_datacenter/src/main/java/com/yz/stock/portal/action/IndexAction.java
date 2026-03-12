package com.yz.stock.portal.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Indexgroup;
import com.stock.common.model.SPoint;
import com.stock.common.model.SSeries;
import com.stock.common.msg.Message;
import com.stock.common.msg.common.CIndexSeries;
import com.stock.common.msg.common.DataItem;
import com.stock.common.util.DateUtil;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.stock.common.util.UnitUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.cache.CacheUtilService;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexGroupService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.IndustryService;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.stock.common.BaseAction;

public class IndexAction extends BaseAction {

	private static final long serialVersionUID = 3862986264548904032L;
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private IndexMessage msg = new IndexMessage();
	Logger log = LoggerFactory.getLogger(this.getClass());
	CompanyService cs = CompanyService.getInstance();
	DictService ds = DictService.getInstance();
	CRuleService crs = CRuleService.getInstance();
	IndexService is = IndexService.getInstance();

	// 对比
	@SuppressWarnings("rawtypes")
	@Action(value = "/index/getIndexValueList")
	public String getDCompareIndex() {
		try {
			boolean flag = msg.getCompanyList().trim()
					.matches(StockConstants.COMPANY_FORMAT_PATTERN);
			if (!flag) {
				this.setErrorReason("公司名称格式错误!");
				return ERROR;
			}
			List<CIndexSeries> cisl = new ArrayList<CIndexSeries>();
			// 解析公司列表
			String[] ca = msg.getCompanyList().split("\\|");
			// 取每个公司对应的指标数据
			for (String cc : ca) {
				String[] tmp = cc.split(":");
				if (tmp.length != 2) {
					this.setErrorReason("公司名称格式错误!");
					return ERROR;
				}
				String companyName = tmp[0];
				String companyCode = tmp[1];

				// // 公司与报表体系是否对应
				// String tk = msg.getTableSystemCode() + "." + companyCode;
				// TableSystemCompanySub tscs = LCEnter
				// .getInstance()
				// .get(tk,
				// StockUtil
				// .getCacheName(StockConstants.TABLE_SYSTEM_COMPANY_SUB_TYPE));
				// if (tscs == null) {
				// this.setErrorReason("您选择的报表体系下不存在此公司数据!");
				// return ERROR;
				// }
				// 公司是否存在
				Company c = CompanyService.getInstance().getCompanyByCode(
						companyCode);
				if (c == null) {
					this.setErrorReason("公司不存在!");
					return ERROR;
				}
				msg.setNeedAccessExtIndexDb(true);
				msg.setNeedAccessExtRemoteCache(false);
				msg.setNeedComput(true);
				Date etime = StockUtil.addDatePerMonth(msg.getEndTime(), 1);
				msg.setEndTime(etime);
				// 取公司的指示值
				CIndexSeries cis = getCompanyIndexData(msg, companyCode);
				if (cis != null) {
					cis.setName(c.getSimpileName());
					Dictionary d = DictService.getInstance().getDataDictionary(
							msg.getIndexCode());
					cis.setUnit(d.getUnit());
					cisl.add(cis);
				}

			}
			this.setResultData(cisl);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	

	private String getQMsg(String type) {
		if (type.equals("0"))
			return "(季度)";
		if (type.equals("1"))
			return "(半年)";
		if (type.equals("2"))
			return "(年化)";
		return null;
	}


	// 趋势
	@Action(value = "/index/getBehaviourIndex")
	public String getBehaviourIndex() {
		try {
			boolean flag = msg.getCompanyList().matches(
					StockConstants.COMPANY_FORMAT_PATTERN);
			if (!flag) {
				this.setErrorReason("公司名称格式错误!");
				return ERROR;
			}
			List<CIndexSeries> cisl = new ArrayList<CIndexSeries>();

			String[] tmp = msg.getCompanyCode().split(":");
			if (tmp.length != 2) {
				this.setErrorReason("公司信息不可为空");
				return ERROR;
			}
			String companyName = tmp[0];
			String companyCode = tmp[1];

			// 公司是否存在
			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(
					companyCode);
			if (c == null) {
				this.setErrorReason("公司不存在!");
				return ERROR;
			}

			// 取公司的指示值
			CIndexSeries cis = getCompanyIndexData(msg, companyCode);
			if (cis != null) {
				cis.setName(companyName);
				cisl.add(cis);
			}
			this.setResultData(cisl);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	/**
	 * 多个指标的同行业[证劵会四级行业]所有公司值 串珠图使用，外部JS分拆成本公司和其它公司
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Action(value = "/index/getMultiIndexOneTimeOneTagAllCompany")
	public String getMultiIndexOneTimeOneTagAllCompany() {
		try {
			String companycode = this.getHttpServletRequest().getParameter(
					"companycode");
			String tag = cs.getCompanyByCode(companycode).getF042v();
			String ttime = this.getHttpServletRequest().getParameter("time");
			String indexs = this.getHttpServletRequest().getParameter("indexs");

			if (StringUtil.isEmpty(indexs) || StringUtil.isEmpty(tag))
				return ERROR;
			Date time ;
			if (StringUtil.isEmpty(ttime))
				time = StockUtil.getApproPeriod(new Date());
			else
				time = DateUtil.format(ttime);
			String[] indexas = indexs.split(";");
			List<String> ils = new ArrayList<String>();
			for (String index : indexas) {
				ils.add(index);
			}

			List<Company> cl = null;
			if (tag.equals(IndustryService.root)) {
				cl = cs.getCompanyListFromCache();
			} else {
				cl = cs.getCompanyListByTagFromCache(tag);
			}
			if (cl == null || cl.size() == 0)
				return ERROR;
			boolean needAccessExtDb = true;
			if (cl.size() > 15) {
				for (String indexcode : ils) {
					Dictionary d = DictService.getInstance().getDataDictionary(
							indexcode);
					if (d == null)
						continue;
					if (!StockUtil.isBaseIndex(d.getType()))
						CacheUtilService
								.loadAllCompanyOneExtIndexOfOneTag2cache(tag,
										indexcode, time);
				}
				needAccessExtDb = false;
			}
			SSeries ss = new SSeries();
			ss.setName(tag);
			Double total = 0.0;
			for (Company c : cl) {
				// 剔除同一公司在不同地点上市，导致的重复
				if (CompanyService.getInstance().removeBST(c))
					continue;
				// if(companycode.equals(c.getCompanyCode()))continue;//去掉本公司
				for (String indexcode : ils) {
					IndexMessage im = SMsgFactory.getUMsg(
							c.getCompanyCode(), indexcode, time);
					im.setNeedAccessExtIndexDb(needAccessExtDb);
					Double xv = IndexValueAgent.getIndexValue(im);
					String vname = ds.getDataDictionary(indexcode)
							.getShowName();
					if (xv == null)
						continue;

					xv = SMathUtil.getDouble(xv, 2);
					total += xv;
					SPoint sctp = new SPoint(c.getCompanyCode(),
							c.getSimpileName(), xv, "");
					// 原始值入在x中
					sctp.setX(xv);
					sctp.setVname(vname);
					sctp.setVindexcode(indexcode);
					ss.put(sctp);
				}
			}
			ss.setTotal(total);
			this.setResultData(ss);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@SuppressWarnings("unchecked")
	@Action(value = "/index/getIndexOneTimeOneTagCompany")
	public String getIndexOneTimeOneTagCompany() {
		try {
			String tag = this.getHttpServletRequest().getParameter("tag");
			String ttime = this.getHttpServletRequest().getParameter("time");

			String indexcode = this.getHttpServletRequest().getParameter(
					"indexcode");

			if (StringUtil.isEmpty(indexcode) || StringUtil.isEmpty(tag))
				return ERROR;
			Date time ;
			if (StringUtil.isEmpty(ttime))
				time = StockUtil.getApproPeriod(new Date());
			else
				time = DateUtil.format(ttime);
			List<Company> cl = null;
			if (tag.equals(IndustryService.root)) {
				cl = cs.getCompanyListFromCache();
			} else {
				cl = cs.getCompanyListByTagFromCache(tag);
			}
			if (cl == null || cl.size() == 0) {
				List<Company> ncl = new ArrayList<Company>();
				// 模糊查询
				List<Company> acl = cs.getCompanyListFromCache();
				for (Company c : acl) {
					if (c.getTags().contains(tag)) {
						ncl.add(c);
					}
				}
				if (ncl.size() == 0)
					return ERROR;
				cl = ncl;
			}
			Dictionary d = DictService.getInstance().getDataDictionary(
					indexcode);
			if (cl.size() > 30 && !StockUtil.isBaseIndex(d.getType())) {
				CacheUtilService.loadAllCompanyOneExtIndexOfOneTag2cache(tag,
						indexcode, time);
			}
			SSeries ss = new SSeries();
			ss.setName(tag);
			Double total = 0.0;
			for (Company c : cl) {
				// 剔除同一公司在不同地点上市，导致的重复
				if (CompanyService.getInstance().removeBST(c))
					continue;
				IndexMessage im = SMsgFactory.getUMsg(c.getCompanyCode(),
						indexcode, time);
				Double xv = IndexValueAgent.getIndexValue(im);
				if (xv == null)
					continue;

				xv = SMathUtil.getDouble(xv, 2);
				total += xv;
				SPoint sctp = new SPoint(c.getCompanyCode(),
						c.getSimpileName(), xv, tag);
				// 原始值入在x中
				sctp.setX(xv);
				ss.put(sctp);
			}
			ss.setTotal(total);
			this.setResultData(ss);
			// CacheUtilService.setQuerydb(true);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	/**
	 * 取某一分类，某一时间段的多个指标值
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Action(value = "/index/getCompanyMutIndexCompareData")
	public String getCompanyMutIndexCompareData() {
		try {
			String tag = this.getHttpServletRequest().getParameter("tag");
			String ttime = this.getHttpServletRequest().getParameter("time");

			if (StringUtil.isEmpty(tag))
				return ERROR;
			Date time ;
			if (StringUtil.isEmpty(ttime))
				time = StockUtil.getApproPeriod(new Date());
			else
				time = DateUtil.format(ttime);
			List<String> ils = new ArrayList<String>();
			ils.add("1683");
			ils.add("1674");
			ils.add("2112");
			ils.add("2022");

			List<Company> cl = cs.getCompanyListByTagFromCache(tag);
			if (cl == null || cl.size() == 0)
				return ERROR;
			Map<String, Double> tm = new HashMap<String, Double>();
			List<SSeries> ssl = new ArrayList<SSeries>();

			for (Company c : cl) {
				SSeries ss = new SSeries();
				ss.setName(c.getSimpileName());
				for (String indexcode : ils) {
					Double total = tm.get(indexcode);
					if (total == null) {
						total = new Double(0.0);
						tm.put(indexcode, total);
					}
					IndexMessage im = SMsgFactory.getUMsg(
							c.getCompanyCode(), indexcode, time);
					Double xv = IndexValueAgent.getIndexValue(im);
					if (xv == null)
						xv = 0.0;
					xv = SMathUtil.getDouble(xv, 2);
					total += xv;
					SPoint sctp = new SPoint(c.getCompanyCode(),
							c.getSimpileName(), xv, tag);
					// 原始值入在x中
					sctp.setX(xv);
					ss.put(sctp);
					tm.put(indexcode, total);
				}

				ssl.add(ss);
			}
			// 取均值
			SSeries avgs = new SSeries();
			avgs.setName("均值");
			for (String indexcode : ils) {
				// 绝对值用平均值，比率用行业值
				String type = StockConstants.avgType;
				Dictionary d = DictService.getInstance().getDataDictionary(
						indexcode);
				if (d.getUnit() == UnitUtil.unit_0)
					type = StockConstants.ravgType;
				Double xavg = IndustryService.getInstance()
						.getMaxMinAvgMidOneTimeFromDB(tag, type, indexcode,
								time);
				if (xavg == null)
					xavg = 0.0;
				xavg = SMathUtil.getDouble(xavg, 2);
				SPoint sctp = new SPoint("均值", "均值", xavg, tag);
				// 原始值入在x中
				sctp.setX(xavg);
				avgs.put(sctp);
			}
			ssl.add(avgs);

			ssl = covertSsl(ssl, tm, ils);
			this.setResultData(ssl);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	/**
	 * 取某一时间点某一公司的多个指标值
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Action(value = "/index/getOneCompanyOneTimeMutIndex")
	public String getOneCompanyOneTimeMutIndex() {
		try {
			String companycode = this.getHttpServletRequest().getParameter(
					"companycode");
			String ttime = this.getHttpServletRequest().getParameter("time");
			String indexs = this.getHttpServletRequest().getParameter("indexs");
			if (StringUtil.isEmpty(indexs) || StringUtil.isEmpty(companycode))
				return ERROR;
			Date time;
			if (StringUtil.isEmpty(ttime))
				time = CompanyService.getInstance().getLatestReportTime(
						companycode);
			else
				time = DateUtil.format(ttime);
			String[] indexas = indexs.split(";");
			List<String> ils = new ArrayList<String>();
			for (String index : indexas) {
				ils.add(index);
			}

			Map<String, Double> tm = new HashMap<String, Double>();
			List<SSeries> ssl = new ArrayList<SSeries>();
			Company c = cs.getCompanyByCode(companycode);
			SSeries ss = new SSeries();
			ss.setName(c.getSimpileName());
			for (String indexcode : ils) {
				Double total = tm.get(indexcode);
				if (total == null) {
					total = new Double(0.0);
					tm.put(indexcode, total);
				}
				IndexMessage im = SMsgFactory.getUMsg(c.getCompanyCode(),
						indexcode, time);
				Double xv = IndexValueAgent.getIndexValue(im);
				String vname = ds.getDataDictionary(indexcode).getShowName();
				if (xv == null)
					xv = 0.0;
				xv = SMathUtil.getDouble(xv, 2);
				total += xv;
				SPoint sctp = new SPoint(c.getCompanyCode(),
						c.getSimpileName(), vname, xv);
				sctp.setVindexcode(indexcode);
				// 原始值入在x中
				sctp.setX(xv);
				ss.put(sctp);
				tm.put(indexcode, total);
			}
			ssl.add(ss);

			// 取均值
			// SSeries avgs = new SSeries();
			// avgs.setName("均值");
			// for(String indexcode : ils)
			// {
			// Double xavg =
			// IndustryService.getInstance().getMaxMinAvgMidOneTime(tag,
			// StockConstants.avgType, indexcode, time);
			// if(xavg==null)
			// xavg=0.0;
			// xavg = SMathUtil.getDouble(xavg, 2);
			// String vname = ds.getDataDictionary(indexcode).getShowName();
			// SPoint sctp = new SPoint("均值","均值",vname,xavg);
			// //原始值入在x中
			// sctp.setX(xavg);
			// avgs.put(sctp);
			// }
			// ssl.add(avgs);

			// ssl = covertSsl(ssl,tm,ils);
			this.setResultData(ssl);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	/**
	 * 取某一时间点某一公司的多个指标值
	 * 
	 * @return
	 */
	@Action(value = "/index/getOneTimeMutIndex")
	public String getOneTimeMutIndex() {
		try {
			String companycodeOrTag = this.getHttpServletRequest()
					.getParameter("companycodeOrTag");
			String ttime = this.getHttpServletRequest().getParameter("time");
			String indexs = this.getHttpServletRequest().getParameter("indexs");
			if (StringUtil.isEmpty(indexs)
					|| StringUtil.isEmpty(companycodeOrTag))
				return ERROR;
			Date time ;
			if (StringUtil.isEmpty(ttime))
				time = StockUtil.getDefaultPeriodTime(null);
			else
				time = DateUtil.format(ttime);
			String[] indexas = indexs.split(";");
			List<String> ils = new ArrayList<String>();
			for (String index : indexas) {
				ils.add(index);
			}

			String code = "";
			String name = "";
			IndexMessage im = SMsgFactory.getUMsg(companycodeOrTag);
			if (companycodeOrTag.indexOf(".")> 0) {
				code = companycodeOrTag;
				Company c = cs.getCompanyByCode(code);
				name = c.getSimpileName();
				im.setCompanyCode(code);
				time=c.getReportTime();
			} else {
				code = companycodeOrTag;
				name = companycodeOrTag;
			}
			im.setTime(time);
			Map<String, Object> rm = new HashMap<String, Object>();
			StringBuilder sb = new StringBuilder();
			for (String indexcode : ils) {
				im.setIndexCode(indexcode);
				Double xv = IndexValueAgent.getIndexValue(im);
				String indexname = ds.getDataDictionary(indexcode).getShowName();
				if (xv == null)
					xv = 0.0;
				xv = SMathUtil.getDouble(xv, 2);
				sb.append(indexname+":"+indexcode);
				sb.append("^");
				sb.append(xv);
				sb.append("~");
			}
			rm.put("name", name);
			rm.put("code", code);
			rm.put("vl", sb.toString());
			StockUtil.outputJson(getHttpServletResponse(), "multindex",
					JSONUtil.serialize(rm));

		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	/**
	 * 取某一时间点一个行业下多个公司的多个指标值
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Action(value = "/index/getOneTagMutlCompanyOneTimeMutIndex")
	public String getOneTagMutlCompanyOneTimeMutIndex() {
		try {
			String tag = this.getHttpServletRequest().getParameter("tag");
			String ttime = this.getHttpServletRequest().getParameter("time");
			String indexs = this.getHttpServletRequest().getParameter("indexs");
			if (StringUtil.isEmpty(indexs) || StringUtil.isEmpty(tag))
				return ERROR;
			Date time ;
			if (StringUtil.isEmpty(ttime))
				time = StockUtil.getApproPeriod(new Date());
			else
				time = DateUtil.format(ttime);
			String[] indexas = indexs.split(",");
			List<String> ils = new ArrayList<String>();
			for (String index : indexas) {
				ils.add(index);
			}

			Map<String, Double> tm = new HashMap<String, Double>();
			List<Company> cl = cs.getCompanyListByTagFromCache(tag);
			List<SSeries> ssl = new ArrayList<SSeries>();
			for (Company c : cl) {
				SSeries ss = new SSeries();
				ss.setName(c.getSimpileName());
				for (String indexcode : ils) {
					Double total = tm.get(indexcode);
					if (total == null) {
						total = new Double(0.0);
						tm.put(indexcode, total);
					}
					Dictionary d = ds.getDataDictionary(indexcode);
					IndexMessage im = SMsgFactory.getUMsg(
							c.getCompanyCode(), indexcode, time);
					Double xv = IndexValueAgent.getIndexValue(im);
					String vname = d.getShowName();
					if (xv == null)
						xv = 0.0;
					xv = SMathUtil.getDouble(xv, 2);
					total += xv;
					SPoint sctp = new SPoint(c.getCompanyCode(),
							c.getSimpileName(), vname, xv);
					// 原始值入在x中
					sctp.setX(xv);
					sctp.setXindexcode(indexcode);
					ss.put(sctp);
					tm.put(indexcode, total);
				}
				ssl.add(ss);
			}

			this.setResultData(ssl);

		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@SuppressWarnings("unchecked")
	@Action(value = "/index/getCompanyMutIndexCompareData_v2")
	public String getCompanyMutIndexCompareData_v2() {
		try {
			String tag = this.getHttpServletRequest().getParameter("tag");
			String ttime = this.getHttpServletRequest().getParameter("time");

			if (StringUtil.isEmpty(tag))
				return ERROR;
			Date time ;
			if (StringUtil.isEmpty(ttime))
				time = StockUtil.getDefaultPeriodTime(null);
			else
				time = DateUtil.format(ttime);
			List<String> ils = new ArrayList<String>();
			ils.add("1683");
			ils.add("1674");
			ils.add("2112");
			ils.add("2022");

			List<Company> cl = cs.getCompanyListByTagFromCache(tag);
			if (cl == null || cl.size() == 0)
				return ERROR;
			List<SSeries> ssl = new ArrayList<SSeries>();

			for (String indexcode : ils) {
				SSeries ss = new SSeries();
				ss.setName("");
				for (Company c : cl) {
					IndexMessage im = SMsgFactory.getUMsg(
							c.getCompanyCode(), indexcode, time);
					Double xv = IndexValueAgent.getIndexValue(im);
					if (xv == null)
						xv = 0.0;
					xv = SMathUtil.getDouble(xv, 2);
					SPoint sctp = new SPoint(c.getCompanyCode(),
							c.getSimpileName(), xv, tag);
					ss.put(sctp);

				}
				ssl.add(ss);
			}
			this.setResultData(ssl);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	private List<SSeries> covertSsl(List<SSeries> ssl, Map<String, Double> tm,
			List<String> ils) {
		for (int i = 0; i < ils.size(); i++) {
			String indexcode = ils.get(i);
			for (SSeries ss : ssl) {
				SPoint sp = ss.getData().get(i);
				Double t = tm.get(indexcode);
				if (t == null || t == 0)
					t = 1.0;
				sp.setV(sp.getV() / t);
			}
		}
		return ssl;
	}

	


	@SuppressWarnings("rawtypes")
	private CIndexSeries getCompanyIndexData(IndexMessage msg,
			String companyCode) {
		// IndexMessage req = buildRequest(msg, companyCode);
		msg.setCompanyCode(companyCode);
		List vl = IndexService.getInstance().getCompanyIndexValueMapList(msg);
		CIndexSeries cis = new CIndexSeries();
		List<DataItem> dil = null;
		if (vl != null) {
			dil = getDataList(vl, msg.getIndexCode(), cis);
		}
		if (dil != null) {
			cis.setData(dil);
		}
		return cis;
	}

	private List<DataItem> getDataList(List vl, String indexCode,
			CIndexSeries cis) {
		// TODO Auto-generated method stub
		List<DataItem> dil = new ArrayList<DataItem>();
		for (int i = 0; i < vl.size(); i++) {
			DataItem di = new DataItem();
			Map m = (Map) vl.get(i);
			// 时间
			Date tValue = (Date) m.get(StockConstants.C_TIME_NAME);
			// 指标名
			String iName = ds.getDataDictionary(indexCode).getColumnName();
			Double value = 0.0;
			// 指标值
			Object ov = m.get(iName);
			if(ov !=null)
				value = Double.parseDouble(ov.toString());
			if (tValue == null || value == null) {
				continue;
			}
			// 时间的毫秒值
			Long time = tValue.getTime();
			di.setTime(time);
			di.setValue(value);
			dil.add(di);
		}
		return dil;
	}

	/**
	 * 获取指标的定义
	 * 
	 * @return
	 */
	@Action(value = "/Index/getIndex")
	public String getIndex() {
		HttpServletRequest request = this.getHttpServletRequest();
		String indexCode = request.getParameter("indexCode");
		Dictionary dict = ds.getDataDictionary(indexCode);
		JSONObject json = JSONObject.fromObject(dict);
		StockUtil.outputJson(this.getHttpServletResponse(), json);
		return SUCCESS;
	}

	public IndexMessage getMsg() {
		return msg;
	}

	public void setMsg(IndexMessage msg) {
		this.msg = msg;
	}

}
