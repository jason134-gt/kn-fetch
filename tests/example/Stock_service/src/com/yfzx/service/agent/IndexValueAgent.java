package com.yfzx.service.agent;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jfree.util.Log;

import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.USubject;
import com.stock.common.msg.Message;
import com.stock.common.util.StockUtil;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.IndustryService;
import com.yfzx.service.db.USubjectService;
import com.yfzx.service.factory.SMsgFactory;

public class IndexValueAgent {

	public static Double getBaseIndexValue(Dictionary d, Message msg) {

		Double v = null;
		if (IndexService.isCompanyMsg(msg)) {
			v = getCompanyBaseIndexValue(d, msg);
		} else {
			v = getIndustryBaseIndexValue(d, msg);
		}
		return v;
	}

	private static Double getIndustryBaseIndexValue(Dictionary d, Message para) {
		IndexMessage req = (IndexMessage) para;
		req.setColumnName(d.getColumnName());
		req.setIndexCode(d.getIndexCode());
		req.setTableName(d.getTableName());
		return IndustryService.getInstance().getIndustryBaseIndexValue(req);
	}

	private static Double getCompanyBaseIndexValue(Dictionary d, Message para) {

		IndexMessage req = (IndexMessage) para;
		req.setColumnName(d.getColumnName());
		req.setIndexCode(d.getIndexCode());
		req.setTableName(d.getTableName());
		Double v = IndexService.getInstance().getCompanyBaseIndexValue(req);
		return v;
	}

	public static Double getExtIndexValue(Dictionary d, IndexMessage msg) {

		Double v = null;
		if (IndexService.isCompanyMsg(msg)) {
			v = IndexService.getInstance().getCompanyExtIndexValue(d, msg);
		} else {
			v = IndustryService.getInstance().getIndustryExtIndexValue(d, msg);
		}
		return v;
	}

	public static Double getIndexValue(IndexMessage msg) {
		Double ret = null;
		Dictionary d = DictService.getInstance().getDataDictionary(
				msg.getIndexCode());
		if (d == null)
			return null;
		if (StockUtil.isBaseIndex(d.getType()))
			ret = getBaseIndexValue(d, msg);
		else
			ret = getExtIndexValue(d, msg);
		return ret;
	}

	public static Double getIndexValueNoCompute(IndexMessage msg) {
		Double ret = null;
		msg.setNeedComput(false);
		Dictionary d = DictService.getInstance().getDataDictionary(
				msg.getIndexCode());
		if (StockUtil.isBaseIndex(d.getType())) {
			if (IndexService.isCompanyMsg(msg)) {
				ret = getCompanyBaseIndexValue(d, msg);
			} else {
				ret = IndustryService.getInstance().getIndustryExtIndexValue(d,
						msg);
			}
		} else
			ret = getExtIndexValue(d, msg);
		return ret;
	}

	public static void cacheMidResult(Dictionary d, IndexMessage midmsg,
			Double value) {

		if (IndexService.isCompanyMsg(midmsg)) {
			IndexService.getInstance().cacheCompanyMidResult(d, midmsg, value);
		} else {
			IndustryService.getInstance().cacheIndustryMidResult(d, midmsg,
					value);
		}

	}

	public static List<Map> getIndexMapList(IndexMessage im) {
		List<Map> vml = null;
		if (IndexService.isCompanyMsg(im)) {
			vml = IndexService.getInstance().getCompanyIndexValueMapList(im);
		} else {
			vml = IndustryService.getInstance()
					.getIndustryIndexValueMapList(im);
		}
		return vml;
	}

	// public static List<Double> getIndexDoubleList(IndexMessage im) {
	// List<Double> vdl = null;
	// if(StockUtil.isCompanyMsg(im))
	// {
	// vdl = IndexService.getInstance().getCompanyIndexValueDoubleList(im);
	// }
	// if(StockUtil.isIndustryMsg(im))
	// {
	// vdl = IndustryService.getInstance().getCompanyIndexValueDoubleList(im);
	// }
	// return vdl;
	// }

	public static Double getIndexValueAvg(IndexMessage req) {
		Double sum = 0.0;
		int count = 0;
		try {

			IndexMessage im = (IndexMessage) req.clone();
			im.setCompanyCode(im.getCompanyCode());
			Double v0 = IndexValueAgent.getIndexValue(im);
			if (v0 == null)
				v0 = 0.0;
			else
				count++;
			String uidentify = "i_-1_所有行业";
			// 所有行业的平均估值
			im.setUidentify(uidentify);
			USubject us = USubjectService.getInstance()
					.getUSubjectByUIdentifyFromCache(uidentify);
			if (us == null) {
				Log.error("query USubject failed!uidentify=" + uidentify);
				return sum;
			}
			im.setMsgtype(us.getType());
			Double v1 = IndexValueAgent.getIndexValue(im);
			if (v1 == null)
				v1 = 0.0;
			else
				count++;
			sum += v1;
			Double industryAvg = 0.0;
			List<String> tags = CompanyService.getInstance()
					.getYfxzIndustryOfCompany(im.getCompanyCode());
			for (String tag : tags) {

				im.setUidentify(tag);
				industryAvg = IndexValueAgent.getIndexValue(im);
				if (im.getIndexCode().equals("2349") && industryAvg > 12) {
					industryAvg = 12.0;
				}

				if (industryAvg == null)
					industryAvg = 0.0;
				else
					count++;
				sum += industryAvg;
			}
		} catch (Exception e) {

		}
		return sum / count;
	}

	public static Double getIndexValue(String companyCode, String indexcode,
			Date time) {
		IndexMessage im = SMsgFactory.getUMsg(companyCode, indexcode, time);
		im.setNeedAccessExtIndexDb(false);
		im.setNeedAccessCompanyBaseIndexDb(false);
		return IndexValueAgent.getIndexValue(im);
	}

	public static Double getIndexValueNeedCompute(String companyCode, String indexcode,
			Date time) {
		IndexMessage im = SMsgFactory.getUMsg(companyCode, indexcode, time);
		im.setNeedAccessExtIndexDb(false);
		im.setNeedAccessCompanyBaseIndexDb(false);
		im.setNeedComput(true);
		return IndexValueAgent.getIndexValue(im);
	}
	public static Double getIndexValueNotRealCompute(String companyCode, String indexcode,
			Date time) {
		IndexMessage im = SMsgFactory.getUMsg(companyCode, indexcode, time);
		im.setNeedAccessExtIndexDb(false);
		im.setNeedAccessCompanyBaseIndexDb(false);
		im.setNeedComput(false);
		return IndexValueAgent.getIndexValueNoCompute(im);
	}
	public static Double getPlateIndexValue(String tag, String indexcode,
			Date time) {
		IndexMessage im = SMsgFactory.getUMsg(tag, indexcode, time);
		im.setNeedAccessExtIndexDb(false);
		im.setNeedAccessCompanyBaseIndexDb(false);
		return IndexValueAgent.getIndexValue(im);
	}

}
