package com.yz.stock.portal.action;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Dcheck;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Tagrule;
import com.stock.common.msg.Message;
import com.stock.common.util.DateUtil;
import com.stock.common.util.NetUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DcheckService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.TagruleService;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.stock.common.BaseAction;

public class DataCheckAction extends BaseAction {

	private static final long serialVersionUID = 3862986264548904032L;
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	Logger log = LoggerFactory.getLogger(this.getClass());
	DictService ds = DictService.getInstance();
	CRuleService crs = CRuleService.getInstance();

	/**
	 * 查询所有不满足条件的公司
	 * 
	 * @return
	 */
	@Action(value = "/dcheck/scanAllCompanyDcheck")
	public String testAllCompanyTagrule() {
		try {
			StringBuilder asb = new StringBuilder();
			List<Dcheck> dl = DcheckService.getInstance().queryDcheckListAll();
			if (dl != null) {
				for (Dcheck dc : dl) {
					String rule = dc.getRule();

					List<Company> cl = CompanyService.getInstance()
							.getCompanyListFromCache();
					StringBuilder sb = new StringBuilder();
					sb.append(dc.getId() + "|");
					boolean notfound = true;
					int count = 0;
					for (Company c : cl) {
						Date time = c.getReportTime();
						int accord = TagruleService.getInstance()
								.isAccordNeedCompute(c.getCompanyCode(), time,
										StockUtil.getRuleByComments(rule),
										StockConstants.DEFINE_INDEX, false);
						if (accord <= 0 && count < 5) {
							sb.append(c.getSimpileName() + ":"
									+ c.getCompanyCode());
							sb.append("^");
							notfound = false;
							count++;
						}
						if (count > 5)
							break;

					}
					if (!notfound) {
						asb.append(sb);
						asb.append("~");
					}
				}
			}

			StockUtil.outputJson(getHttpServletResponse(), "sdret",
					JSONUtil.serialize(asb.toString()));
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/dcheck/testDcheck")
	public String testDcheck() {
		try {
			HttpServletRequest req = this.getHttpServletRequest();
			String rule = req.getParameter("rule");
			String companycode = req.getParameter("companycode");
			String ruleType = NetUtil.getParameterString(
					this.getHttpServletRequest(), "ruleType", "0");
			Date time = StockUtil.getDefaultPeriodTime(null);

			if (StringUtil.isEmpty(rule) || StringUtil.isEmpty(companycode)
					|| time == null) {
				log.error("req para error!");
				return ERROR;
			}

			int accord = TagruleService.getInstance().isAccordNeedCompute(
					companycode, time, StockUtil.getRuleByComments(rule),
					Integer.valueOf(ruleType), false);
			IndexMessage im = SMsgFactory.getUMsg(companycode);
			im.setTime(time);
			im.setNeedAccessExtRemoteCache(true);
			im.setNeedUseExtDataCache(true);
			im.setNeedComput(false);
			String compileResult = CRuleService.getInstance().compileRule(
					StockUtil.getRuleByComments(rule), im,
					StockConstants.DEFINE_INDEX);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("accord", Integer.valueOf(accord));
			m.put("compileResult", compileResult);
			this.setResultData(m);
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/dcheck/testDcheckAllCompany")
	public String testDcheckAllCompany() {
		try {
			HttpServletRequest req = this.getHttpServletRequest();
			String rule = req.getParameter("rule");
			String ttime = NetUtil.getParameterString(
					this.getHttpServletRequest(), "time", "");
			int checkCount = NetUtil.getParameterInt(
					this.getHttpServletRequest(), "checkCount", 1);
			String ruleType = NetUtil.getParameterString(
					this.getHttpServletRequest(), "ruleType", "0");
			String timeUnit = NetUtil.getParameterString(
					this.getHttpServletRequest(), "timeUnit", "m");
			if (StringUtil.isEmpty(rule)) {
				log.error("req para error!");
				return ERROR;
			}
			rule = rule.trim();
			Tagrule tr = new Tagrule();
			tr.setRuleOriginal(rule);
			Date time = null;
			if (!StringUtil.isEmpty(ttime))
				time = DateUtil.format(ttime);
			else
				time = new Date();
			List<Company> cl = CompanyService.getInstance()
					.getCompanyListFromCache();
			StringBuilder sb = new StringBuilder();
			int checked = 0;
			for (Company c : cl) {
				Date actime = IndexService.getInstance().formatTime(time,
						Integer.valueOf(ruleType), DateUtil.getTUint(timeUnit),
						c.getCompanyCode());
				if (actime == null)
					continue;
				int accord = TagruleService.getInstance().isAccordNeedCompute(
						c.getCompanyCode(), actime,
						StockUtil.getRuleByComments(rule),
						Integer.valueOf(ruleType), false);
				if (accord > 0) {
					checked++;
					sb.append(c.getSimpileName() + ":" + c.getCompanyCode());
					sb.append(";");
				}
				if (checkCount != -1 && checked >= checkCount)
					break;

			}
			if (!sb.toString().endsWith("d;") && checkCount != -1
					&& checked > 0 && checked < checkCount) {
				sb.append("date:" + DateUtil.formatDate2YYYYMMDDFast(time));
				sb.append("d;");
			}
			if (Integer.valueOf(ruleType) == StockConstants.TRADE_TYPE) {
				time = StockUtil.getNextTimeV3(time, -1, Calendar.DAY_OF_MONTH);
			}

			sb.append("date:" + DateUtil.formatDate2YYYYMMDDFast(time));
			sb.append("d;");
			this.setResultData(sb.toString());
		} catch (Exception e) {
			log.error("execute /msgconst/savemsgconst failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

}
