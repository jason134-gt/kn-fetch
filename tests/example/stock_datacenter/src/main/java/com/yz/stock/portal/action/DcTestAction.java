package com.yz.stock.portal.action;

import java.util.List;

import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.USubject;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.USubjectService;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.stock.common.BaseAction;
import com.yz.stock.realtime.RealDataComputeTimer;
import com.yz.stock.realtime.SnnTimer;
import com.yz.stock.snn.Snner;

/**
 * 实时分析action
 * 
 * @author user
 * 
 */
public class DcTestAction extends BaseAction {

	Logger log = LoggerFactory.getLogger(this.getClass());
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	CompanyService cs = CompanyService.getInstance();
	DictService ds = DictService.getInstance();
	CRuleService crs = CRuleService.getInstance();

	@Action(value = "/realtime/startEvolutive")
	public String startEvolutive() {

		try {
			Snner.getInstance().start();
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/realtime/timerCheck")
	public String timerCheck() {

		try {
			List<USubject> sl = USubjectService.getInstance().getUSubjectAHZList();
			SnnTimer.getInstance().timerCheck(sl);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	@Action(value = "/realtime/baseRuleCheck")
	public String baseRuleCheck() {

		try {
			List<USubject> sl = USubjectService.getInstance().getUSubjectAHZList();
			SnnTimer.getInstance().baseRuleCheck(sl);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/realtime/clearSuccessMap")
	public String clearSuccessMap() {

		try {
			SnnTimer.getInstance().clearSuccessRecordMap();
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/realtime/realcompute")
	public String realcompute() {

		try {
			RealDataComputeTimer.getInstance().fetchFromSinaAndSend();
			RealDataComputeTimer.getInstance().computeAIndexs();
			RealDataComputeTimer.getInstance().computeAHIndexs();
			RealDataComputeTimer.getInstance().computeDayTradeHavg();
			RealDataComputeTimer.getInstance().doSendIndexs();
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	@Action(value = "/realtime/realcomputeSend")
	public String realcomputeSend() {

		try {
			RealDataComputeTimer.getInstance().doSendIndexs();
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	@Action(value = "/realtime/realfetch")
	public String realfetch() {

		try {
			RealDataComputeTimer.getInstance().fetchFromSinaAndSend();
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

}
