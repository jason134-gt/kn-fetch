package com.yfzx.service.db;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.IndexMessage;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;

public class ValuationService {

	static PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	Logger logger = LoggerFactory.getLogger(this.getClass());
	DictService ds = DictService.getInstance();
	private static ValuationService instance = new ValuationService();

	private ValuationService() {

	}

	public static ValuationService getInstance() {
		return instance;
	}

	public Double valuation(String companycode, Date time, int year) {
		Double ret = 0.0;
		Double sum = 0.0;
		// 计算公司所在的各级行业平均估值
		List<String> tags;
		try {
			IndexMessage im = SMsgFactory.getUMsg(companycode);
			im.setCompanyCode(companycode);
			im.setTime(time);
			// 取年化净利润1960
			im.setIndexCode("1960");
			im.setNeedAccessExtRemoteCache(true);
			Double jlr = IndexValueAgent.getIndexValue(im);
			if (jlr == null)
				jlr = 0.0;
			// 股本
			im.setIndexCode("2290");
			Double sn = IndexValueAgent.getIndexValue(im);
			if (sn == null)
				sn = 0.0;
			sum = 0.0;
			// 公司的平均估值
			Double vavg = valuationAvg(im, jlr, sn);
			sum += vavg;
			// 所有行业的平均估值
			im.setMsgtype(USubjectService.getInstance().getMsgType(im.getUidentify()));
			im.setUidentify("i_-1_所有行业");
			Double allIndustryAvg = valuationAvg(im, jlr, sn);
			sum += allIndustryAvg;
			Double industryAvg = 0.0;
			im.setMsgtype(USubjectService.getInstance().getMsgType(im.getUidentify()));
			tags = CompanyService.getInstance().getYfxzIndustryOfCompany(
					companycode);
			for (String tag : tags) {

				im.setUidentify(tag);
				industryAvg = valuationAvg(im, jlr, sn);
				sum += industryAvg;
			}
			ret = sum / (tags.size() + 2);
		} catch (Exception e) {

		}
		return ret;
	}

	private Double valuationAvg(IndexMessage im, Double jlr, Double sn) {
		// 复合增长率
		im.setIndexCode("4394");
		Double cagr = IndexValueAgent.getIndexValue(im);
		if (cagr == null)
			cagr = 0.0;
		if (cagr > 0.25)
			cagr = 0.25;
		// if(cagr<0)
		// cagr=0.0;

		// 股息率
		im.setIndexCode("4384");
		Double gxl = IndexValueAgent.getIndexValue(im);
		if (gxl == null)
			gxl = 0.0;
		if (gxl < 0)
			gxl = 0.0;

		// 下面两个指标用公司的值
		int type = im.getMsgtype();
		// 每股经常性收益
		im.setIndexCode("2112");
		im.setMsgtype(USubjectService.getInstance().getMsgType(im.getUidentify()));
		Double mgsy = IndexValueAgent.getIndexValue(im);
		if (mgsy == null)
			mgsy = 0.0;
		// 每股收益
		im.setIndexCode("2109");
		im.setMsgtype(USubjectService.getInstance().getMsgType(im.getUidentify()));
		Double mgsy1 = IndexValueAgent.getIndexValue(im);
		if (mgsy1 == null)
			mgsy1 = 0.0;
		// 两者中取小的
		if (mgsy > mgsy1)
			mgsy = mgsy1;

		// 每股净资产
		im.setIndexCode("2113");
		im.setMsgtype(USubjectService.getInstance().getMsgType(im.getUidentify()));
		Double mgjzc = IndexValueAgent.getIndexValue(im);
		if (mgjzc == null)
			mgjzc = 0.0;
		// 切回来
		im.setMsgtype(type);

		// pe,pb,peg如果为公司时，就取此指标的均值
		// pe
		im.setIndexCode("2349");
		Double pe = getIndexValue(im);
		if (pe == null || pe < 0) {
			IndexMessage nim = (IndexMessage) im.clone();
			nim.setUidentify("i_-1_所有行业");
			nim.setMsgtype(USubjectService.getInstance().getMsgType(im.getUidentify()));
			pe = getIndexValue(nim);
		}
		if (pe > 12)
			pe = 12.0;
		// peg
		im.setIndexCode("4439");
		Double peg = getIndexValue(im);

		if (peg == null || peg < 0) {
			IndexMessage nim = (IndexMessage) im.clone();
			nim.setUidentify("i_-1_所有行业");
			nim.setMsgtype(USubjectService.getInstance().getMsgType(im.getUidentify()));
			peg = getIndexValue(nim);
		}
		if (peg > 1)
			peg = 1.0;
		// pb
		im.setIndexCode("2350");
		Double pb = getIndexValue(im);
		if (pb == null) {
			IndexMessage nim = (IndexMessage) im.clone();
			nim.setUidentify("i_-1_所有行业");
			nim.setMsgtype(USubjectService.getInstance().getMsgType(im.getUidentify()));
			pb = getIndexValue(nim);
		}

		Double sum = 0.0;
		int count = 0;
		Double dcfgv = valuationDcf(jlr, sn, cagr, gxl, 0.12, pe, 3);
		if (dcfgv > 0) {
			//sum += dcfgv;
			count++;
		}
		Double peggv = valuationPeg(mgsy, cagr, peg);
		if (peggv > 0) {
			//sum += peggv;
			count++;
		}
		Double pegv = valuationPe(mgsy, pe);
		if (pegv > 0) {
			//sum += pegv;
			count++;
		}
		Double pbgv = valuationPb(mgjzc, pb);
		if (pbgv > 0) {
			//sum += pbgv;
			count++;
		}
		if(count==4)
			sum = dcfgv*0.35+peggv*0.3+pegv*0.2+pbgv*0.15;
		else
			sum = (dcfgv+peggv+pegv+pbgv)/count;
		//如果其它的估值均为0，则其估值就等于净资产
		if(dcfgv==0&&peggv==0&&pegv==0)
			sum=mgjzc;
		return sum ;
	}

	// pe,pb,peg指标，如果为公司时，就取此指标的均值
	private Double getIndexValue(IndexMessage im) {
		Double v = 0.0;
		if (IndexService.isCompanyMsg(im)) {
			v = IndexValueAgent.getIndexValueAvg(im);
		} else {
			v = IndexValueAgent.getIndexValue(im);
		}
		return v;
	}

	private Double valuationDcf(Double jlr, Double stockAllNum, Double cagr,
			Double gxl, Double txl, Double pe, int year) {
		if (cagr < 0)
			cagr = 0.0;
		Double sum = 0.0;
		Double lastJlr = 0.0;// 最后一年的新增利润
		for (int i = 0; i < year; i++) {
			Double zzl = Math.pow(1 + (cagr), (i + 1));// 第N年的增长率
			Double addjlr = jlr * zzl;// 增加的净利润
			Double txjlr = Math.pow((1 / (1 + txl)), (i + 1)) * addjlr;// 贴现利润
			Double tx = txjlr * gxl;// 分红不用贴现
			sum += tx;
			if (i == year - 1)
				lastJlr = txjlr;
		}
		Double nextTotal = lastJlr * pe + sum; // 未来贴现总市值
		return nextTotal / stockAllNum;

	}

	private Double valuationPeg(Double mgsy, Double cagr, Double peg) {
		if (cagr < 0)
			cagr = 0.0;
		return mgsy * cagr * 100 * peg;

	}

	private Double valuationPe(Double mgsy, Double pe) {

		return mgsy * pe;

	}

	private Double valuationPb(Double mgjzc, Double pb) {

		return mgjzc * pb;

	}
}
