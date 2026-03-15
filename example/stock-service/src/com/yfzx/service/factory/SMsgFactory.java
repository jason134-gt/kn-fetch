package com.yfzx.service.factory;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.CompileMode;
import com.stock.common.event.NotifyEvent;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.USubject;
import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.yfzx.service.db.USubjectService;
import com.yz.configcenter.ConfigCenterFactory;

public class SMsgFactory {

	static Logger log = LoggerFactory.getLogger(SMsgFactory.class);

	/*
	 * 取公司或行业类型的消息
	 */
	// public static IndexMessage getIndexMessage(int dataType) {
	//
	// return IndexMessage.getIndexMessage(dataType);
	// }
	//
	//
	/**
	 * 取数据中心类型的消息
	 * 
	 * @param dataType
	 * @return
	 */
	public static IndexMessage getUDCIndexMessage(String uidentify) {

		IndexMessage im = getUMsg(uidentify);
		im.setNeedComput(CompileMode.needCompute());
		im.setNeedAccessExtIndexDb(CompileMode.needAccessExtIndexDb());
		im.setNeedUseExtDataCache(CompileMode.useCacheExtData());
		im.setNeedAccessCompanyBaseIndexDb(CompileMode
				.needAccessCompanyBaseIndexDb());
		im.setNeedRealComputeIndustryValue(CompileMode
				.needRealComputeIndustryValue());
		return im;
	}

	public static IndexMessage getCompanyMsg(String companyCode,
			String indexcode, Date time) {
		return getUMsg(companyCode, indexcode, time);
	}

	//
	public static IndexMessage getIndustryMsg(String tag, String indexcode,
			Date time) {
		return getUMsg(tag, indexcode, time);
	}

	public static IndexMessage getUMsg(String uidentify, String indexcode,
			Date time) {
		USubject us = USubjectService.getInstance()
				.getUSubjectByUIdentifyFromCache(uidentify);
		if (us == null)
			return null;
		IndexMessage im = new IndexMessage(us.getType());
		im.setUidentify(uidentify);
		im.setIndexCode(indexcode);
		im.setTime(time);
		return im;
	}

	public static IndexMessage getUMsg(String uidentify, Date time) {
		USubject us = USubjectService.getInstance()
				.getUSubjectByUIdentifyFromCache(uidentify);
		if (us == null) {
			log.error("not found thid usubject! uidentify=" + uidentify);
			return null;
		}
		IndexMessage im = new IndexMessage(us.getType());
		im.setUidentify(uidentify);
		im.setTime(time);
		return im;
	}

	public static IndexMessage getUMsg(String uidentify) {
		USubject us = USubjectService.getInstance()
				.getUSubjectByUIdentifyFromCache(uidentify);
		if (us == null) {
			int SMsgFactorylog = ConfigCenterFactory.getInt("stock_log.SMsgFactory", 0);
			if(SMsgFactorylog == 1){
				log.error("not found thid usubject! uidentify=" + uidentify);
			}
			return null;
		}
		IndexMessage im = new IndexMessage(us.getType());
		im.setUidentify(uidentify);
		return im;
	}

	public static UserMsg getSingleUserMsgByType(int type ) {
		UserMsg um = new UserMsg();
		um.setMsgType(type);
		um.setSendType(MsgConst.SEND_TYPE_0);
		return um;
	}
	public static UserMsg getBrodCastUserMsgByType(int type) {
		UserMsg um = new UserMsg();
		um.setMsgType(type);
		um.setSendType(MsgConst.SEND_TYPE_1);
		return um;
	}
}
