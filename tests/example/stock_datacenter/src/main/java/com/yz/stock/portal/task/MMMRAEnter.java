package com.yz.stock.portal.task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SExt;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.TableSystem;
import com.stock.common.util.DateUtil;
import com.stock.common.util.DictUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexService;
import com.yfzx.service.db.IndustryService;
import com.yfzx.service.db.MatchinfoService;
import com.yfzx.service.db.TableSystemService;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.stock.portal.manager.ComputeIndexManager;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.model.BatchQueueEntity;
import com.yz.stock.util.ExecuteQueueManager;

public class MMMRAEnter {

	CRuleService crs = CRuleService.getInstance();
	Logger log = LoggerFactory.getLogger(this.getClass());
	static MMMRAEnter instance = new MMMRAEnter();

	public MMMRAEnter() {

	}

	public static MMMRAEnter getInstance() {
		return instance;
	}

	public void computeRAvg(final String indexcode, final Date stime,
			final Date etime, final String tags) {
		try {

			// 取所有要计算的指标
			final List<Dictionary> dl = DictService.getInstance()
					.getAllDictionaryList();
			for (final Dictionary d : dl) {
				// 如果指定了指标，就只计算指定指标的
				if (!StringUtil.isEmpty(indexcode)
						&& !d.getIndexCode().equals(indexcode)
						|| StockUtil.isTradeIndex(d.getTctype()))
					continue;
				if (DictUtil.needMMMA(d)) {
					StockFactory.submitTaskBlocking(new Runnable() {

						public void run() {

							// log.info("========================compute ravg ,indexcode ="+d.getIndexCode()+" ,tag = "+tags+" ,start =========================");
							Date sTime = stime;
							Date eTime = etime;
							computeRAvgOfIndex(d, sTime, eTime, tags);
							// log.info("========================compute ravg ,indexcode ="+d.getIndexCode()+" ,tag = "+tags+" ,end =========================");

						}

					});
				}
			}
			log.info("========================execute computeMaxMinAvg task end  ....=========================");
		} catch (Exception e) {
			log.error("execute computeMaxMinAvg task failed!", e);
		}

	}

	/**
	 * 计算某个指标在每个分类中的最在值，最小值，时间段：1980-6-30--now()
	 * 
	 * @param d
	 * @param tags
	 */
	@SuppressWarnings("rawtypes")
	public void computeRAvgOfIndex(Dictionary d, Date sTime, Date eTime,
			String tags) {

		try {
			// 引处用不上公司的扩展数据，行业数据此处是按公式算，最终都会追到公司的基础数据上
			// ComputeIndexManager.getInstance().loadAllRAvgExtData(d);
			// if(!StockUtil.isBaseIndex(d.getType()))
			// CacheUtilService.doLoadAllCompanyOneExtIndexOneSec2cache(d.getIndexCode(),sTime,eTime);
			computeOneIndexAllTagsRAvgOfTime(d, sTime, eTime, tags);

		} catch (Exception e) {
			// TODO: handle exception
			log.error("update all data failed!", e);
		}

	}

	/**
	 * 计算某个指标的所有分类，所有分类做为一批 il为某一时间点，某一指标的所有公司的数据
	 * 
	 * @param d
	 * @param ctags
	 * @param sTime
	 * @param il
	 */
	@SuppressWarnings({ "unchecked", "rawtypes", "static-access" })
	private void computeOneIndexAllTagsRAvgOfTime(Dictionary d, Date stime,
			Date etime, String ctags) {

		try {
			// 取分类
			String tsc = MatchinfoService.getInstance().getTscByTableCode(
					d.getTableCode());
			List<String> tags = ComputeIndexManager.getInstance()
					.getTagsByTscV2(tsc);
			for (String tag : tags) {
				try {
					// 如果指定了分类，就只计算指定分类的指标
					if (!StringUtil.isEmpty(ctags) && !ctags.equals(tag))
						continue;
					if ("-1".equals(tag))
						continue;
					log.info("start compute tag=" + tag + ",dictionary="
							+ d.getIndexCode() + ":" + d.getShowName());
					Date nstime = stime;
					Date netime = etime;
					Date sd = nstime;
					Date ed = netime;
					sd = IndexService.getInstance().formatTime_Tag(sd, d, tag);
					// 如果起始时间,小于结束时间
					while (sd.compareTo(ed) <= 0) {

						try {
							String tableName = SExt.getUExtTableName(tag,
									SExt.EXT_TABLE_TYPE_1);
							String iIndexCode = "";
							String sql = "";
							// 以规则公式的方式计算行业均值（只针对于扩展指标）
							IndexMessage req = SMsgFactory
									.getUDCIndexMessage(tag);
							Date actime = IndexService.getInstance()
									.formatTime_Tag(sd, d, tag);
							req.setTime(actime);
							req.setIndexCode(d.getIndexCode());
							req.setNeedAccessCompanyBaseIndexDb(false);
							req.setNeedRealComputeIndustryValue(true);
							req.setNeedAccessExtIndexDb(false);
							req.setIndustryIndexType(StockConstants.ravgType);
							Double v = null;
							v = IndustryService.getInstance()
									.computeIndustryIndex(req);
							if (v != null && v != 0) {
								iIndexCode = StockUtil.getMaxMinAvgIndexcode(
										"ravg", d.getIndexCode());
								sql = buildSql(tableName, tag, iIndexCode, v,
										actime);
								// 改为指量异步更新
								ExecuteQueueManager
										.add2IQueue(new BatchQueueEntity(
												Integer.valueOf(StockConstants.I_EXT_INDEX_TYPE),
												StockConstants.I_EXT_INDEX, sql));
							}
						} catch (Exception e) {
							log.error("compute failed!", e);
						}
						// 取下一个时间点
						sd = StockUtil.getNextTimeV3(sd, d.getInterval(),
								d.getTunit());
					}

				} catch (Exception e) {
					log.error("compute failed!", e);
				}
				log.info("end compute tag=" + tag + ",dictionary="
						+ d.getIndexCode() + ":" + d.getShowName());
			}
		} catch (Exception e) {
			log.error("compute failed!", e);
		}

	}

	private String buildSql(String tableName, String tag, String iIndexCode,
			Double v, Date time) {
		// TODO Auto-generated method stub
		return "replace into " + tableName
				+ " (uidentify,index_code,value,time,uptime) " + "values ( '"
				+ tag + "','" + iIndexCode + "'," + v + ",DATE('"
				+ DateUtil.format2String(time) + "'),now()) ";
	}

	public void computeRAvgByPage(Date stime, Date etime,
			List<String> taglist, List<Dictionary> ndl) {
		try {
			for (String tag : taglist) {
				try {
					if ("-1".equals(tag))
						continue;
					List<Company> cl = CompanyService.getInstance()
							.getCompanyListByTagFromCache(tag);
					if (cl == null || cl.size() == 0) {
						log.error("companylist is empty!tag=" + tag);
						continue;
					}
					docomputeTagRavgByPage(tag, stime, etime, ndl);

				} catch (Exception e) {
					log.error("execute computeMaxMinAvg task failed!", e);
				}
			}
		} catch (Exception e) {
			log.error("execute computeMaxMinAvg task failed!", e);
		}
	}

	private void docomputeTagRavgByPage(String tag, Date stime, Date etime,
			List<Dictionary> ndl) {
		ComputeRavgOneTag(tag, stime, etime, ndl);
	}

	private Object ComputeRavgOneTag(final String tag, final Date stime,
			final Date etime, final List<Dictionary> ndl) {
		try {
			// 切记一定要放外面
			final Long pid = Thread.currentThread().getId();
			StockFactory.submitTaskBlocking(new Callable<String>() {

				@Override
				public String call() throws Exception {
					try {
						log.info("start compute industry data!tag=" + tag);
						TaskSchedule.put(pid, Thread.currentThread().getId());
						Date sTime = stime;
						Date eTime = etime;
						for (Dictionary d : ndl) {
							// Cfirule crule =
							// CRuleService.getInstance().getCfruleByCodeFromCache(d.getIndexCode());
							// if(crule==null)
							// continue;
							// log.info("start compute industry data!tag="+tag+";d="+d.getShowName());
							computeTagRAvgByPage(d, sTime, eTime, tag);
						}
						log.info("end compute industry data!tag=" + tag);
					} catch (Exception e) {
						log.error(
								"complete compute company index failed!companycode = "
										+ tag, e);
					} finally {
						TaskSchedule
								.remove(pid, Thread.currentThread().getId());
					}
					return null;
				}
			});

		} catch (Exception e) {
			log.error("compute index failed!", e);
		}

		return null;
	}

	private void computeTagRAvgByPage(Dictionary d, Date stime, Date etime,
			String tag) {
		try {
			try {
				Date nstime = stime;
				Date netime = etime;
				Date sd = nstime;
				Date ed = netime;
				sd = IndexService.getInstance().formatTime_Tag(sd, d, tag);
				// 如果起始时间,小于结束时间
				while (sd.compareTo(ed) <= 0) {
					try {
						StockFactory.expTime = Calendar.getInstance()
								.getTimeInMillis();
						String tableName = SExt.getUExtTableName(tag,
								SExt.EXT_TABLE_TYPE_1);
						String iIndexCode = "";
						String sql = "";
						// 以规则公式的方式计算行业均值（只针对于扩展指标）
						IndexMessage req = SMsgFactory.getUDCIndexMessage(tag);
						// 计算指标
						Date actime = StockUtil.formatJiDuTime(sd);
						// actime = IndexService.getInstance()
						// .formatTime_Tag(sd, d, tag);
						if (actime != null) {
							req.setTime(actime);

							req.setIndexCode(d.getIndexCode());
							req.setNeedAccessCompanyBaseIndexDb(false);
							req.setNeedRealComputeIndustryValue(true);
							req.setNeedAccessExtIndexDb(false);
							req.setIndustryIndexType(StockConstants.ravgType);
							Double v = null;

							v = IndustryService.getInstance()
									.computeIndustryIndex(req);

							if (v != null && v != 0) {
								iIndexCode = StockUtil.getMaxMinAvgIndexcode(
										"ravg", d.getIndexCode());
								sql = buildSql(tableName, tag, iIndexCode, v,
										actime);
								// 改为指量异步更新
								ExecuteQueueManager
										.add2IQueue(new BatchQueueEntity(
												Integer.valueOf(StockConstants.I_EXT_INDEX_TYPE),
												StockConstants.I_EXT_INDEX, sql));
							}
						}
					} catch (Exception e) {
						log.error("compute failed!", e);
					}
					// 取下一个时间点
					sd = StockUtil.getNextTime(sd, 3);
				}

			} catch (Exception e) {
				log.error("compute failed!", e);
			}
		} catch (Exception e) {
			log.error("compute failed!", e);
		}

	}

}
