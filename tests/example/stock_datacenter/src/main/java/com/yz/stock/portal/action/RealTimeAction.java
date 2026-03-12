package com.yz.stock.portal.action;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Cfirule;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.RealTimeMessage;
import com.stock.common.msg.Message;
import com.stock.common.msg.common.CIndexSeries;
import com.stock.common.msg.common.DataItem;
import com.stock.common.util.DateUtil;
import com.stock.common.util.NetUtil;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.IndustryService;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.stock.common.BaseAction;

/**
 * 实时分析action
 * 
 * @author user
 * 
 */
public class RealTimeAction extends BaseAction {

	Logger log = LoggerFactory.getLogger(this.getClass());
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private RealTimeMessage real = new RealTimeMessage();

	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	CompanyService cs = CompanyService.getInstance();
	DictService ds = DictService.getInstance();
	CRuleService crs = CRuleService.getInstance();

	// 实时分析
	@Action(value = "/index/realTimeAnalysis")
	public String getHCompareIndex() {
		List<CIndexSeries> cisl = new ArrayList<CIndexSeries>();
		try {

			// 解析公司列表
			String[] ca = real.getCompanyList().split("\\|");
			// 取每个公司对应的指标数据
			for (String cc : ca) {
				String[] tmp = cc.split(":");
				if (tmp.length != 2) {
					continue;
				}
				String companyName = tmp[0];
				String companyCode = tmp[1];

				// 公司与报表体系是否对应
//				String tk = real.getTableSystemCode() + "." + companyCode;
//				TableSystemCompanySub tscs = LCEnter
//						.getInstance()
//						.get(tk,
//								StockUtil
//										.getCacheName(StockConstants.TABLE_SYSTEM_COMPANY_SUB_TYPE));
//				if (tscs == null) {
//					this.setErrorReason("您选择的报表体系下不存在此公司数据!");
//					return ERROR;
//				}
				// 公司是否存在
				Company c = CompanyService.getInstance().getCompanyByCode(
						companyCode);
				if (c == null) {
					this.setErrorReason("公司不存在!");
					return ERROR;
				}
				int tunit = DateUtil.getTUint(real.getTimeUnit());
				// 取规则模板
				Cfirule cRule = buildRule(real);
				CIndexSeries cis = new CIndexSeries();
				cis.setName(companyName);
				String acrossType = cRule.getAcrossType();
				// 取时间的近似值
				String[] ara = real.getAccountRegion().split("\\|");
				// String sTime = StockUtil.getApproxiTime(msg.getStartTime(),
				// acrossType,
				// ara[0]);
				// String eTime = StockUtil.getApproxiTime(msg.getEndTime(),
				// acrossType,
				// ara[1]);
				Date sTime = real.getStartTime();
				Date eTime = real.getEndTime();
				eTime = StockUtil.getNextTime(eTime, 1);
				if (Integer.valueOf(real.getIndextype()) == StockConstants.TRADE_TYPE&&eTime.compareTo(new Date())>0) {
					eTime = new Date();
				}
				while (sTime.compareTo(eTime) <= 0) {
					try {
						//对迭代时间进行校正
						Date actime = IndexService.getInstance().formatTime(sTime, Integer.valueOf(real.getIndextype()), tunit, companyCode);
						if(actime!=null)
						{
							IndexMessage req =SMsgFactory.getUMsg(companyCode, real.getIndexCode(), actime);
							
							String caRegion = StockUtil.computeCurAccountRegion(
									actime, real.getAccountRegion(),
									real.getcAccountRegion());
							req.setcAccountRegion(caRegion);
					
							// 编译规则
							req.setNeedComput(true);
							req.setNeedUseExtDataCache(false);
							req.setNeedRealComputeIndustryValue(true);
							req.setNeedAccessExtRemoteCache(false);
							req.setNeedAccessExtIndexDb(true);
							req.setNeedAccessCompanyBaseIndexDb(true);
							String expresion = crs.complieRule(cRule, req);
							if (!StringUtil.isEmpty(expresion)) {
								// 计算
								Double value = crs.executeExpresion(expresion);
								if (value != null) {
									// 构造返回结果
									DataItem di = new DataItem(actime, value);
									cis.getData().add(di);
								}
							}
						}
						
					} catch (Exception e) {
						// TODO: handle exception
						log.error("compute rule failed!", e);
					}
					sTime =  StockUtil.getNextTimeV3(sTime,
							Integer.valueOf(real.getInterval()),tunit);
				}
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

	// 实时分析
		@Action(value = "/index/realTimeAnalysisv2")
		public String realTimeAnalysis() {
			List<CIndexSeries> cisl = new ArrayList<CIndexSeries>();
			try {

	
				String companycode = this.getHttpServletRequest().getParameter("companycode");
//				String tsc = this.getHttpServletRequest().getParameter("tsc");
				String r = this.getHttpServletRequest().getParameter("rule");
				Date stime = NetUtil.getParameterDate(this.getHttpServletRequest(),
						"stime", StockUtil.getNextTime(
								StockUtil.getDefaultPeriodTime(null), -12));
				Date etime = NetUtil.getParameterDate(this.getHttpServletRequest(),
						"etime", StockUtil.getDefaultPeriodTime(null));
				if(StringUtil.isEmpty(companycode)||StringUtil.isEmpty(r))
					return ERROR;
			
					// 公司是否存在
					Company c = CompanyService.getInstance().getCompanyByCodeFromCache(
							companycode);
					if (c == null) {
						this.setErrorReason("公司不存在!");
						return ERROR;
					}

					Cfirule rule = new Cfirule();
					rule.setComments(r);
					rule.setRule(StockUtil.getRuleByComments(r));
					
					CIndexSeries cis = new CIndexSeries();
					cis.setName(c.getSimpileName());
			
					Date sTime = stime;
					Date eTime = etime;
					while (sTime.compareTo(eTime) <= 0) {
						try {
							IndexMessage req = SMsgFactory.getUMsg(companycode,sTime);
							
							// 编译规则
							String expresion = crs.complieRule(rule, req);
							if (StringUtil.isEmpty(expresion)) {
								sTime = StockUtil.getNextTime(sTime,
										Integer.valueOf(real.getInterval()));
								log.warn("expression is null!req :"
										+ req.toString());
								continue;
							}
							// 计算
							Double value = crs.executeExpresion(expresion);
							if (value == null) {
								sTime = StockUtil.getNextTime(sTime,
										Integer.valueOf(real.getInterval()));
								log.warn("value is null! req :" + req.toString());
								continue;
							}
							value = SMathUtil.getDouble(value, 2);
							// 构造返回结果
							DataItem di = new DataItem(sTime, value);
							cis.getData().add(di);
						} catch (Exception e) {
							// TODO: handle exception
							log.error("compute rule failed!", e);
						}
						sTime = StockUtil.getNextTime(sTime,
								3);
					}
					cisl.add(cis);
				

				this.setResultData(cisl);
			} catch (Exception e) {
				log.error("execute /getDynamicIndex failed", e);
				setErrorReason("failed！");
				return ERROR;
			}
			return SUCCESS;
		}
		
	
	// 取指标最大值，最小值，平均值
	@Action(value = "/index/getIndexSpecialValue")
	public String getIndexSpecialValue() {
		List<CIndexSeries> cisl = new ArrayList<CIndexSeries>();
		try {
			String type = this.getHttpServletRequest().getParameter("type");
			Date stime = NetUtil.getParameterDate(this.getHttpServletRequest(),
					"stime", null);
			Date etime = NetUtil.getParameterDate(this.getHttpServletRequest(),
					"etime",null);
			String indexcode = this.getHttpServletRequest().getParameter(
					"indexcode");
			if (StringUtil.isEmpty(type) || stime==null
					|| etime==null
					|| StringUtil.isEmpty(indexcode))
				return ERROR;
			CIndexSeries cis = new CIndexSeries();
			if (type.equals(StockConstants.maxType))
				cis.setName("本指标最大值");
			if (type.equals(StockConstants.minType))
				cis.setName("本指标最小值");
			if (type.equals(StockConstants.avgType))
				cis.setName("本指标平均值");
			List<DataItem> dl = IndustryService.getInstance().getMaxMinAvgMid(IndustryService.root, type, indexcode, stime, etime);
			cis.setData(dl);
			cisl.add(cis);
			this.setResultData(cisl);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	// 取指标最大值，最小值，平均值,中值
	@Action(value = "/index/getMaxMinAvgMid")
	public String getMaxMinAvgMid() {
		List<CIndexSeries> cisl = new ArrayList<CIndexSeries>();
		try {
			String type = this.getHttpServletRequest().getParameter("type");
			Date sTime = NetUtil.getParameterDate(this.getHttpServletRequest(),
					"sTime", StockUtil.getNextTime(
							StockUtil.getDefaultPeriodTime(null), -12));
			Date eTime = NetUtil.getParameterDate(this.getHttpServletRequest(),
					"eTime", StockUtil.getDefaultPeriodTime(null));
			String mark = this.getHttpServletRequest().getParameter("mark");
			String indexcode = this.getHttpServletRequest().getParameter(
					"indexcode");
			if (StringUtil.isEmpty(type)  
					|| StringUtil.isEmpty(indexcode)|| StringUtil.isEmpty(mark))
				return ERROR;
			
			
			String markName = mark;
			if(mark.equals(IndustryService.root)) markName = IndustryService.root_name;
			Dictionary d = DictService.getInstance().getDataDictionary(indexcode);
			if(d==null) return ERROR;
			String indexname = d.getShowName();
			CIndexSeries cis = new CIndexSeries();
			if (type.equals(StockConstants.maxType))
				cis.setName(indexname+"最大值"+"("+markName+")");
			if (type.equals(StockConstants.minType))
				cis.setName(indexname+"最小值"+"("+markName+")");
			if (type.equals(StockConstants.avgType))
				cis.setName(indexname+"平均值"+"("+markName+")");
			if (type.equals(StockConstants.midType))
				cis.setName(indexname+"中值"+"("+markName+")");
			if (type.equals(StockConstants.ravgType))
				cis.setName(indexname+"行业值"+"("+markName+")");
			List<DataItem> dl = IndustryService.getInstance().getMaxMinAvgMid(mark,type,
					indexcode, sTime, eTime);
			cis.setData(dl);
			cis.setUnit(d.getUnit());
			cisl.add(cis);
			this.setResultData(cisl);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	public Double getAverageValue(Map<String, Double> _dMap, IndexMessage req,
			int points) {
		int count = 0;
		Double ave = 0.0;
		Date atime = req.getTime();
		for (int i = 0; i < points; i++) {
			Date time = atime;
			Calendar c = Calendar.getInstance();
			c.setTime(time);
			String key = getKey(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
			Double v = _dMap.get(key);
			if (v != null) {
				ave += v;
				count++;
			}
			atime = StockUtil.getNextTime(atime, -3);
		}
		if (count != 0)
			ave = ave / count;
		return ave;
	}

	private Map<String, Double> preperData(RealTimeMessage r) {
		Map<String, Double> dm = new HashMap<String, Double>();
		IndexMessage im = SMsgFactory.getUMsg(r.getCompanyCode(), r.getTime());
		im.setStartTime(r.getStartTime());
		im.setEndTime(r.getEndTime());
		im.setIndexCode(r.getIndexCode());
		List l = IndexService.getInstance().getCompanyIndexValueMapList(im);
		if (l != null && l.size() > 0) {
			for (int i = 0; i < l.size(); i++) {
				Map m = (Map) l.get(i);
				// 时间
				Timestamp tValue = (Timestamp) m
						.get(StockConstants.C_TIME_NAME);
				// 指标名
				String iName = ds.getDataDictionary(r.getIndexCode())
						.getColumnName();
				// 指标值
				Double value = (Double) m.get(iName);
				if (tValue == null || value == null) {
					continue;
				}
				// 时间的毫秒值
				Calendar c = Calendar.getInstance();
				c.setTime(tValue);
				String key = getKey(c.get(Calendar.YEAR),
						c.get(Calendar.MONTH) + 1);
				dm.put(key, value);
			}
		}

		return dm;
	}

	private String getKey(int year, int month) {
		// TODO Auto-generated method stub
		return year + "-" + month;
	}



	private Cfirule buildRule(RealTimeMessage real) {
		Cfirule rule = new Cfirule();
		try {
			rule.setAcrossType(real.getAcrossType());
			rule.setComments(real.getRuleComments());
			rule.setName(real.getName());
			rule.setType(Integer.valueOf(real.getIndextype()));
			rule.setRule(StockUtil.getRuleByComments(real.getRuleComments()));
		} catch (Exception e) {
			log.error("build rule from realTimeMsg failed!", e);
		}
		return rule;
	}

	public RealTimeMessage getReal() {
		return real;
	}

	public void setReal(RealTimeMessage real) {
		this.real = real;
	}

}
