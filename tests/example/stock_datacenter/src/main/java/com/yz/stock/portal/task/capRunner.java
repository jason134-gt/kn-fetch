package com.yz.stock.portal.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SExt;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.UpdateIndexReq;
import com.stock.common.util.CapUtil;
import com.stock.common.util.DateUtil;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StockUtil;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yz.stock.portal.model.BatchQueueEntity;
import com.yz.stock.util.ExecuteQueueManager;

public class capRunner implements Runnable {
	Logger log = LoggerFactory.getLogger(this.getClass());
	Date sTime;
	Date eTime;
	Dictionary d;
	List<Dictionary> gdl;
	String tag;

	public capRunner() {

	}

	public capRunner(Date stime, Date etime, Dictionary d,
			List<Dictionary> dl, String tag) {
		this.sTime = stime;
		this.eTime = etime;
		this.d = d;
		this.tag = tag;
		this.gdl = dl;
	}

	public void run() {
		try {
			log.info("start compute cap data !tag="+tag);
			CompanyService cs = CompanyService.getInstance();
			List<Company> cl = cs.getCompanyListByTagFromCacheRemoveBST(tag);
			if (cl == null || cl.size() == 0)
				return;
			String companycodes = TaskEnter.getInstance().toCompanycodes(cl);
			// 此次总分
			Double sum = cl.size() * gdl.size() * 1.0;
			Date sd = sTime;
			Date ed = eTime;
			sd = DateUtil.getCurApproPeriod(sd);
			ed = DateUtil.getCurApproPeriod(ed);
			// 如果起始时间,小于结束时间
			while (sd.compareTo(ed) <= 0) {
				try {
					Map<String, Double> scoreMap = new HashMap<String, Double>();
					// 循环计算某个能力下的所有指标
					for (Dictionary gd : gdl) {
						// 取某一指标，某一时间点的指标的有序集合
						// List<Map> ml = IndexService.getInstance()
						// .getCompanysIndexValueMapListFromDb(gd,
						// sTime,
						// companycodes);
						// 直接从缓存中取
						List<Map<String, Object>> ml = IndexService
								.getInstance()
								.getCompanysIndexValueMapListFromCache(gd,
										sd, companycodes);
						if (ml == null)
							continue;
						char b = gd.getBitSet().charAt(2);
						for (int k = 0; k < ml.size(); k++) {
							Map m = ml.get(k);
							String companycode = (String) m.get("company_code");
							Double v = (Double) m.get(gd.getIndexCode());
							Double cscore = scoreMap.get(companycode);
							if (cscore == null) {
								cscore = (double) (ml.size() - k);
								if (b == '1')
									cscore = (double) (k + 1);

								// 给分数加上权重，在还没有和原有分累加前加上
								cscore = TaskEnter.getInstance()
										.addWeight(gd, v, cscore, tag,
												gdl.size(), sd, null);

							} else {
								if (b == '1') {
									Double nscore = (double) (k + 1);

									// 给分数加上权重，在还没有和原有分累加前加上
									nscore = TaskEnter.getInstance().addWeight(
											gd, v, nscore, tag, gdl.size(),
											sd, null);
									cscore += nscore;
								} else {
									Double nscore = (double) (ml.size() - k);

									// 给分数加上权重，在还没有和原有分累加前加上
									nscore = TaskEnter.getInstance().addWeight(
											gd, v, nscore, tag, gdl.size(),
											sd, null);
									cscore += nscore;
								}
							}

							scoreMap.put(companycode, cscore);

						}
					}
					List<Entry<String, Double>> sl = new ArrayList<Entry<String, Double>>();
					sl.addAll(scoreMap.entrySet());
					Collections.sort(sl,
							new Comparator<Entry<String, Double>>() {

								public int compare(Entry<String, Double> o1,
										Entry<String, Double> o2) {
									if (o2.getValue() == null)
										return -1;
									if (o1.getValue() == null)
										return 1;
									return o2.getValue().compareTo(
											o1.getValue());
								}

							});
					for (int i = 0; i < sl.size(); i++) {
						Entry<String, Double> e = sl.get(i);
						String companycode = e.getKey();
						Double score = e.getValue();
						if (score == 0)
							continue;
						Double es = score / sum * 100;
						es = SMathUtil.getDouble(es, 2);
						// 把此时间点的此能力指标值更新到数据库
						String tableName = SExt
								.getUExtTableName(companycode,SExt.EXT_TABLE_TYPE_0);
						UpdateIndexReq req = new UpdateIndexReq();
						req.setColumnName(d.getColumnName());
						req.setTableName(tableName);
						req.setTime(sd);
						req.setValue(String.valueOf(es));
						req.setIndexCode(d.getIndexCode());
						req.setUidentify(companycode);
						req.setIndexName(d.getShowName());
						// 改为指量异步更新
						ExecuteQueueManager.add2IQueue(new BatchQueueEntity(
								Integer.valueOf(StockConstants.TYPE_EXTINDEX),
								StockConstants.U_EXT_INDEX, req));
						String sindexcode = CapUtil.getSindexcodeByCapIndex(d
								.getIndexCode());
						Dictionary xd = DictService.getInstance()
								.getDataDictionary(sindexcode);
						// 更新名次
						req = new UpdateIndexReq();
						req.setColumnName(xd.getColumnName());
						req.setTableName(tableName);
						req.setTime(sd);
						int sort = i + 1;
						req.setValue(String.valueOf(sort));
						req.setIndexCode(xd.getIndexCode());
						req.setUidentify(companycode);
						req.setIndexName(xd.getShowName());
						// 改为指量异步更新
						ExecuteQueueManager.add2IQueue(new BatchQueueEntity(
								Integer.valueOf(StockConstants.TYPE_EXTINDEX),
								StockConstants.U_EXT_INDEX, req));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
//				// 取下一个时间点
//				sTime = StockUtil.getNextTime(sTime, 3);
				sd = StockUtil.getNextTimeV3(sd, Integer.valueOf(d.getInterval()),
						d.getTunit());
				sd = DateUtil.getCurApproPeriod(sd);
//				sd = sTime;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("end compute cap data !tag="+tag);
	}

}
