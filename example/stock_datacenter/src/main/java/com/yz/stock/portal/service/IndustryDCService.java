package com.yz.stock.portal.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SExt;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.DbConnectionBroker;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.cache.CacheUtilService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.MatchinfoService;
import com.yfzx.service.db.StockServiceFactory;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.stock.portal.excel.IDataHandler;
import com.yz.stock.portal.manager.ComputeIndexManager;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.model.BatchQueueEntity;
import com.yz.stock.util.ExecuteQueueManager;

public class IndustryDCService implements IDataHandler {

	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private static IndustryDCService instance = new IndustryDCService();

	private static Map<String, Set<String>> _comapnyTagSet = new HashMap<String, Set<String>>();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private IndustryDCService() {

	}

	public static IndustryDCService getInstance() {
		return instance;
	}


	/**
	 * 求最大值，最小值，中值,平均值,在一个循环里做可以提高性能
	 * 
	 * @param columnName
	 * @param ilist
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map<String, Double> computeMinMaxMidAvg(Dictionary d,
			Date time, String tag) {
		List<Company> cl = CompanyService.getInstance().getCompanyListByTagFromCache(tag);
		if(cl==null||cl.size()==0) return null;
		List<Double> middl = new ArrayList<Double>();
		Map<String, Double> retMap = new HashMap<String, Double>();
		Double minrd = null;// 最小值
		Double maxrd = null;// 最大值
		Double mid = null;// 中值
		Double avgrd = StockConstants.DEFAULT_DOUBLE_VALUE;
		int count = 0;
		for (Company c : cl) {

			Double mmma = null;

			IndexMessage msg = SMsgFactory.getUMsg(c.getCompanyCode());
			msg.setNeedAccessCompanyBaseIndexDb(false);
			msg.setNeedAccessExtIndexDb(false);
			msg.setNeedComput(false);
			msg.setNeedRealComputeIndustryValue(false);
			msg.setIndexCode(d.getIndexCode());
			msg.setTime(time);
			// 计算平均值
			Double to = IndexValueAgent.getIndexValue(msg);
			
			if (to != null) {
				Double td = (Double) to;
				if (td != 0) {
					mmma = td;
					Double weight = CompanyService.getInstance()
							.getWeightOfTag(tag, c.getCompanyCode());
					avgrd += td * weight;
					count++;
					middl.add(td);
				}
			} else {
				// 如果本期没有数据，则取此公司上一期的数据，如果上一期也没有，则不管了。
//				IndexMessage im = SMsgFactory.getCompanyMsg(companycode, d.getIndexCode(), time);
//				im.setNeedAccessCompanyBaseIndexDb(false);
//				Double pd = IndexValueAgent.getIndexValue(im);
//				if (pd != null && pd != 0) {
//					mmma = pd;
//					Double weight = CompanyService.getInstance()
//							.getWeightOfTag(tag, companycode);
//					avgrd += pd * weight;
//					count++;
//					middl.add(pd);
//				}
			}

			if (mmma != null) {
				// 找最小值
				if (minrd == null)
					minrd = mmma;
				if (mmma < minrd && mmma != 0)
					minrd = mmma;
				// 找最大值
				if (maxrd == null)
					maxrd = mmma;
				if (mmma > maxrd && mmma != 0)
					maxrd = mmma;
			}

		}
		if (count != 0)
			avgrd = avgrd / count;

		if (avgrd != null && avgrd != 0)
			retMap.put("avg", avgrd);
		if (maxrd != null && maxrd != 0)
			retMap.put("max", maxrd);
		if (minrd != null && minrd != 0)
			retMap.put("min", minrd);
		
		mid = computeMid(middl);
		if (mid != null && mid != 0)
			retMap.put("mid", mid);
		return retMap;
	}

	private Double computeMid(List<Double> middl) {
		Double mid = StockConstants.DEFAULT_DOUBLE_VALUE;
		if (middl == null || middl.size() == 0)
			return null;
		// 对列表进行排序
		Collections.sort(middl);
		int length = middl.size();
		if (length > 0) {

			int halfindex = length / 2;
			if (length % 2 == 0) {
				// 是偶数
				mid = (middl.get(halfindex) + middl.get(halfindex - 1))/2;
			} else {
				// 是奇数
				mid = middl.get(halfindex);
			}
		}
		return mid;
	}

	public void updateIndex2IndustryExtIndexTable(Dictionary d, Double v,
			String tag, String time, String type) {
		try {
			// 不对0值处理
			if (v == null || v == 0)
				return;
			String iIndexCode = StockUtil.getMaxMinAvgIndexcode(type,
					d.getIndexCode());
			String tableName = SExt.getUExtTableName(tag,SExt.EXT_TABLE_TYPE_1);
			String sql = "replace into "
					+ tableName
					+ " (uidentify,index_code,value,time,uptime) "
					+ "values ( '" + tag + "','" + iIndexCode + "'," + v
					+ ",DATE('" + time + "'),now()) ";
			DbConnectionBroker mypool = StockServiceFactory.getMyConPool();
			Connection con = mypool.getConnection();
			// boolean isfailed = false;
			try {

				// con.setAutoCommit(false);
				Statement stmt = con.createStatement(
						ResultSet.TYPE_SCROLL_SENSITIVE,
						ResultSet.CONCUR_READ_ONLY);
				stmt.execute(sql);
				// stmt.executeBatch();
				// con.commit();
			} catch (Exception e) {
				log.error("BatchInsert failed!", e);
				// isfailed = true;

			} finally {
				try {
					mypool.freeConnection(con);

				} catch (Exception e2) {
					log.error("close db connection failed!", e2);
				}
			}
		} catch (Exception e) {
			log.error("get db connection failed!", e);
		}

	}



	/**
	 * 从某一时间点，某一指标的所有的公司数据中，检出此tag下属的公司的数据
	 * @param il
	 * @param companycodeSet
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private List buildNewindexlist(List<Map> il, Set<String> companycodeSet) {
		List nl = new ArrayList();
		for (Map m : il) {
			String code = (String) m.get("company_code");
			if (companycodeSet.contains(code)) {
				nl.add(m);
			}
		}
		return nl;
	}

	/**
	 * 去掉同一公司，不同点上市时的算两次的问题。
	 * 去掉*ST,ST的公司，以免数据失真
	 * @param tag
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Set<String> getCompanyCodeSet(String tag) {
		Set<String> s = _comapnyTagSet.get(tag);
		if (s == null) {
			s = new HashSet<String>();
			_comapnyTagSet.put(tag, s);
			List<Company> cl = CompanyService.getInstance()
					.getCompanyListByTagFromCache(tag);
			for (Company c : cl) {
				//去重，同一公司在不同的地点上市，会有多个companycode,些处去掉B股，本应按证券信息表中的上市地点来区分，为了简便用名字中是否含有B来区分
				if(CompanyService.getInstance().removeBST(c))
					{
						//System.out.println(c.getSimpileName());
						continue;
					}
				s.add(c.getCompanyCode());
			}
		}
		return s;
	}

	/**
	 * 计算某个指标在每个分类中的最在值，最小值，时间段：1980-6-30--now()
	 * 
	 * @param d
	 */
	@SuppressWarnings("rawtypes")
	public void computeMaxMinAvgOfIndex(Dictionary d, Date sTime, Date eTime,String ctags) {

		try {
			// 加载某一指标的所有数据
//			ComputeIndexManager.getInstance().loadOneIndexAllExtData(d);
			//先加载扩展指标的某一时间段的数据到缓存
			if(!StockUtil.isBaseIndex(d.getType()))
				CacheUtilService.doLoadAllCompanyOneExtIndexOneSec2cache(d.getIndexCode(),sTime,eTime);
			
			// 如果起始时间,小于结束时间
			while (sTime.compareTo(eTime) <= 0) {
				
				computeOneIndexAllTagsMMAOfTime(d, sTime, ctags);
				// 取下一个时间点
				sTime = StockUtil.getNextTimeV3(sTime, d.getInterval(),d.getTunit());
			}
		} catch (Exception e) {
			// TODO: handle exception
			log.error("update all data failed!", e);
		}

	}

	/**
	 * 计算某个指标的所有分类，所有分类做为一批
	 * il为某一时间点，某一指标的所有公司的数据
	 * @param d
	 * @param sTime
	 * @param il
	 */
	@SuppressWarnings({ "unchecked", "rawtypes", "static-access" })
	private void computeOneIndexAllTagsMMAOfTime(Dictionary d, Date time,String ctags) {

		try {
			String tsc = MatchinfoService.getInstance().getTscByTableCode(d.getTableCode());
			List<String> tags = ComputeIndexManager.getInstance().getTagsByTsc(tsc);
			for (String tag : tags) {
				try {
					//如果指定了分类，就只计算指定分类的指标
					if(!StringUtil.isEmpty(ctags)&&!ctags.equals(tag))
						continue;
//					Industry ind = IndustryService.getInstance().getIndustryCSRCByName(tag);
//					if(ind==null) continue;
	
					Map<String, Double> rm = computeMinMaxMidAvg(d,
							time, tag);
					
					String tableName = SExt.getUExtTableName(tag,SExt.EXT_TABLE_TYPE_1);
					String iIndexCode = "";
					String sql = "";
					// avg
					// Double avg = computeAvg(d, ilist, time, tag);
					Double v = rm.get("avg");
					// 不对0值处理
					if (v != null && v != 0) {
						iIndexCode = StockUtil.getMaxMinAvgIndexcode("avg",
								d.getIndexCode());
						sql = buildSql(tableName, tag, iIndexCode, v, time);
						// stmt.addBatch(sql);
						// 改为指量异步更新
						ExecuteQueueManager
								.add2IQueue(
										new BatchQueueEntity(
												Integer.valueOf(StockConstants.I_EXT_INDEX_TYPE),
												StockConstants.I_EXT_INDEX, sql));
					}
					// max
					v = rm.get("max");
					// 不对0值处理
					if (v != null && v != 0) {
						iIndexCode = StockUtil.getMaxMinAvgIndexcode("max",
								d.getIndexCode());
						sql = buildSql(tableName, tag, iIndexCode, v, time);
						// stmt.addBatch(sql);
						// 改为指量异步更新
						ExecuteQueueManager
								.add2IQueue(
										new BatchQueueEntity(
												Integer.valueOf(StockConstants.I_EXT_INDEX_TYPE),
												StockConstants.I_EXT_INDEX, sql));
					}

					// min
					v = rm.get("min");
					// 不对0值处理
					if (v != null && v != 0) {
						iIndexCode = StockUtil.getMaxMinAvgIndexcode("min",
								d.getIndexCode());
						sql = buildSql(tableName, tag, iIndexCode, v, time);
						// stmt.addBatch(sql);
						// 改为指量异步更新
						ExecuteQueueManager
								.getInstance()
								.add2IQueue(
										new BatchQueueEntity(
												Integer.valueOf(StockConstants.I_EXT_INDEX_TYPE),
												StockConstants.I_EXT_INDEX, sql));
					}

					// mid
					v = rm.get("mid");
					// 不对0值处理
					if (v != null && v != 0) {
						iIndexCode = StockUtil.getMaxMinAvgIndexcode("mid",
								d.getIndexCode());
						sql = buildSql(tableName, tag, iIndexCode, v, time);
						// stmt.addBatch(sql);
						// 改为指量异步更新
						ExecuteQueueManager
								.getInstance()
								.add2IQueue(
										new BatchQueueEntity(
												Integer.valueOf(StockConstants.I_EXT_INDEX_TYPE),
												StockConstants.I_EXT_INDEX, sql));
					}
					
					
				} catch (Exception e) {
					log.error("compute failed!", e);
				}
			}// end for
				// stmt.executeBatch();
			// con.commit();
			// iscomplete = true;
		} catch (Exception e) {
			log.error("compute failed!", e);
		}
		

	}

	private String buildSql(String tableName, String tag, String iIndexCode,
			Double v, Date time) {
		// TODO Auto-generated method stub
		return "replace into "
				+ tableName
				+ " (uidentify,index_code,value,time,uptime) "
				+ "values ( '" + tag + "','" + iIndexCode + "'," + v
				+ ",FROM_UNIXTIME('" + time.getTime()/1000 + "'),now()) ";
	}

	/**
	 * 使用的是replace，不存在更新
	 */
	public void handle(String opt, List<Object> ul) {
		BatchInsert(ul);

	}
	static AtomicInteger ai = new AtomicInteger();
	static Set<Object> _fset = new HashSet<Object>();
	private void BatchInsert(List<Object> ul) {
		try {
//			log.info("BatchInsert handle data hcount="+ai.addAndGet(ul.size())+";pcount="+RavgComputeRunnable.ai.get());
//			DbConnectionBroker mypool = StockFactory.getMyConPool();
//			Connection con = mypool.getConnection();
			SqlSession sse = StockFactory.getSqlSessionFactory().openSession();
			Connection con =  sse.getConnection();
			boolean isfailed = false;
			boolean iscomplete = false;// 用来判断，是不是在for循环中一个循环还没有结束时，连接就free了
			try {

				con.setAutoCommit(false);
				Statement stmt = con.createStatement(
						ResultSet.TYPE_SCROLL_SENSITIVE,
						ResultSet.CONCUR_READ_ONLY);
				for (int x = 0; x < ul.size(); x++) {
					BatchQueueEntity be = (BatchQueueEntity) ul.get(x);
					String sql = be.getV();
					stmt.addBatch(sql);
				}
				stmt.executeBatch();
				con.commit();
				iscomplete = true;
			} catch (Exception e) {
				log.error("BatchInsert failed!", e);
				isfailed = true;

			} finally {
				try {
					if (!iscomplete) {
						log.error("is not complete,but  db connection free!");
						isfailed = true;
					}
//					mypool.freeConnection(con);
					sse.close();
					if (isfailed&&!_fset.contains(ul)) {	
						_fset.add(ul);
						// 如果此批失败，则进行单条操作(由于此处全用的是replace，不存在插入冲突，所以直接用再递归下）
						BatchInsert(ul);
					}
				} catch (Exception e2) {
					log.error("close db connection failed!", e2);
				}
			}
		} catch (Exception e) {
			log.error("get db connection failed!", e);
		}

	}
	
	
	
	

		
		
		
}
