package com.yfzx.service.db;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Dictionary;
import com.stock.common.model.TUextData;
import com.stock.common.util.DictUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.cache.DataLoadTimeMng;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.lcs.enter.LCEnter;

/**
 * 新的数据存储模型服务类
 * 
 * @author：杨真
 * @date：2015年1月9日
 */
public class TUextService implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -318386639949329070L;
	public static Logger log = LoggerFactory.getLogger(TUextService.class);
	Map<String, Map<String, Integer>> _headCache = new HashMap<String, Map<String, Integer>>();
	static TUextService instance = new TUextService();
	Map<String, String> _headKeyCodeMap = new HashMap<String, String>();
	Lock lock = new ReentrantLock();// 锁
	String _undefineindexkey = "_undefineindexkey";

	private TUextService() {
		init();
	}

	public static TUextService getInstance() {
		return instance;
	}

	public void init() {
		// List<Dictionary> headlist = DictService.getInstance()
		// .getDictListByType(StockConstants.INDEX_TYPE_6);
		// _headKeyCodeMap.put(_undefineindexkey, "t0");
		// 需要一个升序排列的
		List<Dictionary> headlist = DictService.getInstance()
				.getAllDictionaryList();
		if (headlist != null && headlist.size() != 0) {

			for (int i = 0; i < headlist.size(); i++) {
				Dictionary d = headlist.get(i);
				if (DictUtil.isNotSave(d))
					continue;
				String key = getHeadKey(d);
				Map<String, Integer> head = getHeadByType(key);
				if (head == null) {
					head = new HashMap<String, Integer>();
					_headCache.put(key, head);
					_headKeyCodeMap.put(key, "t" + _headKeyCodeMap.size());
				}
				// 公司指标，每个指标对应的编号必须固定
				head.put(d.getIndexCode(), head.size());
			}
		}
	}

	public String getHeadkeycode(Dictionary d) {
		String key = null;
		if (d == null)
			return null;
		else
			key = getHeadKey(d);
		if (StringUtil.isEmpty(key)) {
			log.error("not found the head key!");
			return null;
		}
		return _headKeyCodeMap.get(key);
	}

	private Map<String, Integer> getHeadByType(String key) {
		// TODO Auto-generated method stub
		return _headCache.get(key);
	}

	private String getHeadKey(Dictionary d) {
		return StockUtil.getHeadKey(d);
	}

	public Integer getIndex(Dictionary d) {
		String key = getHeadKey(d);
		Map<String, Integer> head = getHeadByType(key);
		if (head != null) {
			return head.get(d.getIndexCode());
		}
		return null;
	}

	public void putData(String uidentify, Long time, String indexcode, float v) {
		try {
			if (time != null) {
				Dictionary d = getDictionary(indexcode);
				if (d == null) {
					// 为临时指标
					String key = StockUtil.joinString("", "un_", uidentify);
					Map<String, Float> _unmap = LCEnter.getInstance().get(key,
							StockUtil.getExtIndexCacheName(key));
					if (_unmap == null) {
						_unmap = new ConcurrentHashMap<String, Float>();
						LCEnter.getInstance().put(key, _unmap,
								StockUtil.getExtIndexCacheName(key));
					}
					String ikey = StockUtil.joinString("", time, indexcode);
					_unmap.put(ikey, v);
				} else {
					String key = StockUtil.joinString("", uidentify, time,
							getHeadkeycode(d));
					if (!StringUtil.isEmpty(key)) {
						TUextData tud = LCEnter.getInstance().get(key,
								StockUtil.getExtIndexCacheName(key));
						if (tud == null) {
							lock.lock();
							if (tud == null) {
								tud = new TUextData(
										getHeadByType(getHeadKey(d)).size());
								LCEnter.getInstance().put(key, tud,
										StockUtil.getExtIndexCacheName(key));
								DataLoadTimeMng.getInstance().putDataLoadTime(
										uidentify, indexcode, new Date(time));
							}

							lock.unlock();
						}

						if (tud != null) {
							Integer index = getIndex(d);
							if (index != null) {
								tud.put(index, v);
							}
						}
					}
				}

				// TUextTable tut = getTUextTable(uidentify);
				// if (tut == null) {
				// lock.lock();
				// if (tut == null) {
				// // 创建内存表结构
				// tut = new TUextTable();
				// String key = StockUtil.getTUExtTableKey(uidentify);
				// LCEnter.getInstance().put(key, tut,
				// StockUtil.getExtIndexCacheName(key));
				// }
				// lock.unlock();
				// }
				//
				// TUextRow tu = tut.getTUext(time);
				// if (tu == null) {
				// synchronized (tut) {
				// tu = tut.getTUext(time);
				// if (tu == null) {
				// tu = new TUextRow();
				// tut.putTUext(time, tu);
				// }
				// }
				//
				// }
				// Dictionary d = getDictionary(indexcode);
				// // 如果没有相关的指标，则认为是临时指标，把临时指标的数据放在附加信息中
				// if (d == null) {
				// tu.putAttr(indexcode, v);
				// } else {
				// TUextData tud = tu.getTUextData(d);
				// if (tud == null) {
				// synchronized (tu) {
				// tud = tu.getTUextData(d);
				// if (tud == null) {
				// tud = new TUextData(
				// getHeadByType(getHeadKey(d)).size());
				// tu.putTUextData(d, tud);
				// DataLoadTimeMng.getInstance().putDataLoadTime(uidentify,indexcode,
				// new Date(time));
				// }
				// }
				// }
				// if (tud != null) {
				// Integer index = getIndex(d);
				// if (index != null) {
				// tud.put(index, v);
				// }
				// }
				// }
				//
			}
		} catch (Exception e) {
			log.error("put data failed!", e);
		}
	}

	// private TUextTable getTUextTable(String uidentify) {
	// String key = StockUtil.getTUExtTableKey(uidentify);
	// return LCEnter.getInstance().get(key,
	// StockUtil.getExtIndexCacheName(key));
	// }

	private Dictionary getDictionary(String indexcode) {
		if (indexcode.indexOf("_") >= 0) {
			indexcode = indexcode.split("_")[1];
		}
		return DictService.getInstance().getDataDictionaryFromCache(indexcode);
	}

	// public Float getUExtFloat(String uidentify, Long time, Dictionary d) {
	// if (time != null) {
	// TUextTable tut = getTUextTable(uidentify);
	// if (tut != null) {
	// TUextRow tu = tut.getTUext(time);
	// if (tu != null) {
	// TUextData tud = tu.getTUextData(d);
	// if (tud != null) {
	// Integer index = getIndex(d);
	// if (index != null) {
	// float f = tud.get(index);
	// if(f!=0)
	// return f;
	// }
	// }
	// }
	// }
	//
	// }
	// return null;
	// }

	public Double getUExtDouble(String uidentify, Long time, String indexcode) {
		Object v = null;
		Dictionary d = getDictionary(indexcode);
		if (d == null) {
			String key = StockUtil.joinString("", "un_", uidentify);
			Map<String, Double> _unmap = LCEnter.getInstance().get(key,
					StockUtil.getExtIndexCacheName(key));
			if (_unmap != null) {
				String ikey = StockUtil.joinString("", time, indexcode);
				v = _unmap.get(ikey);
			}

		} else {
			String key = StockUtil.joinString("", uidentify, time,
					getHeadkeycode(d));
			if (!StringUtil.isEmpty(key)) {
				TUextData tud = LCEnter.getInstance().get(key,
						StockUtil.getExtIndexCacheName(key));

				if (tud != null) {
					Integer index = getIndex(d);
					if (index != null) {
						v = tud.get(index);
					}
				}
			}
		}

		// TUextTable tut = getTUextTable(uidentify);
		// if (tut != null) {
		// Dictionary d = getDictionary(indexcode);
		// if (d == null) {
		// TUextRow tu = tut.getTUext(time);
		// if (tu != null) {
		// v = tu.getAttr(indexcode);
		// }
		// } else {
		// v = getUExtFloat(uidentify, time, d);
		// }
		// }
		if (v == null)
			return null;
		if(v instanceof Float)
			return Double.parseDouble(String.valueOf(v));
		return (Double) v;
	}

	public void remove(String uidentify, long time, String indexcode) {
		Dictionary d = getDictionary(indexcode);
		if (d == null) {
			String key = StockUtil.joinString("", "un_", uidentify);
			Map<String, Float> _unmap = LCEnter.getInstance().get(key,
					StockUtil.getExtIndexCacheName(key));
			if (_unmap != null) {
				String ikey = StockUtil.joinString("", time, indexcode);
				_unmap.remove(ikey);
			}

		} else {
			String key = StockUtil.joinString("", uidentify, time,
					getHeadkeycode(d));
			if (!StringUtil.isEmpty(key)) {
				TUextData tud = LCEnter.getInstance().get(key,
						StockUtil.getExtIndexCacheName(key));

				if (tud != null) {
					Integer index = getIndex(d);
					if (index != null) {
						tud.clear(index);
					}
				}
			}
		}
		// TUextTable tut = getTUextTable(uidentify);
		// if (tut != null) {
		// if (d == null) {
		// TUextRow tu = tut.getTUext(time);
		// if (tu != null) {
		// tu.removeAttr(indexcode);
		// }
		// } else {
		// TUextRow tu = tut.getTUext(time);
		// if (tu != null) {
		// TUextData tud = tu.getTUextData(d);
		// if (tud != null) {
		// Integer index = getIndex(d);
		// if (index != null) {
		// tud.clear(index);
		// }
		// }
		// }
		// }
		// }

	}

	int pageSize = 5000;

	public void loadUExtDataByPage(String uidentify, String tableName,
			String stime, String uptime) {

		int pageCount = getLoadUExtPageSize(tableName, uidentify, stime, uptime);
		for (int p = 0; p < pageCount; p++) {
			
			
			
			int start = p * pageSize;
			doloadUExtDataByPage(tableName, uidentify, start, pageSize, stime,
					uptime);

		}
	}

	private void doloadUExtDataByPage(String tableName, String uidentify,
			int start, int limit, String stime, String uptime) {
		SqlSession sse = ((SqlSessionFactory) BaseFactory
				.getSqlSessionFactory()).openSession();
		Connection con = sse.getConnection();

		ResultSet rs = null;
		try {
			Statement stmt = con.createStatement();
			String sql = " select  t.uidentify as uidentify,t.INDEX_CODE as index_code ,t.VALUE as value,t.TIME as time from "
					+ tableName
					+ " t where  uidentify='"
					+ uidentify
					+ "' and  TIME >= '"
					+ stime
					+ "' "
					+ "  limit "
					+ start
					+ "," + limit + "   ";
			if (!StringUtil.isEmpty(uptime)) {
				sql = " select  t.uidentify as uidentify,t.INDEX_CODE as index_code ,t.VALUE as value,t.TIME as time from "
						+ tableName
						+ " t where  uidentify='"
						+ uidentify
						+ "' and  TIME >= '"
						+ stime
						+ "' and uptime >= '"
						+ uptime + "'   limit " + start + "," + limit + "   ";
			}
			rs = stmt.executeQuery(sql);

			if (rs != null) {
				try {
					while (rs.next()) {
						try {
							String indexcode = rs.getString("index_code");
							Float v = rs.getFloat("value");
							Date time = rs.getDate("time");
							TUextService.getInstance().putData(uidentify,
									time.getTime(), indexcode, v);

						} catch (Exception e) {
							log.error("put data 2 cache failed!", e);
						}

					}
				} catch (Exception e) {
					log.error("put data 2 cache failed!", e);
				}
			}
		} catch (Exception e) {
			log.error("query data failed!", e);
		} finally {
			try {
				sse.close();
			} catch (Exception e2) {
				log.error("close db connection failed!", e2);
			}
		}

	}

	private int getLoadUExtPageSize(String tableName, String uidentify,
			String stime, String uptime) {
		int count = 0;
		SqlSession sse = ((SqlSessionFactory) BaseFactory
				.getSqlSessionFactory()).openSession();
		Connection con = sse.getConnection();

		ResultSet rs = null;
		try {
			Statement stmt = con.createStatement();
			String sql = "select count(*) as count from " + tableName
					+ " t where uidentify='" + uidentify + "' and TIME >= '"
					+ stime + "' ";
			if (!StringUtil.isEmpty(uptime)) {
				sql = "select count(*) as count from " + tableName
						+ " t where uidentify='" + uidentify
						+ "'  and TIME >= '" + stime + "' " + "and uptime >= '"
						+ uptime + "' ";
			}

			rs = stmt.executeQuery(sql);
			if (rs != null) {
				try {
					while (rs.next()) {
						try {
							count = rs.getInt("count");
						} catch (Exception e) {
							log.error("get data 2 cache failed!", e);
						}

					}
				} catch (Exception e) {
					log.error("get data 2 cache failed!", e);
				}
			}
		} catch (Exception e) {
			log.error("query data failed!", e);
		} finally {
			try {
				sse.close();
			} catch (Exception e2) {
				log.error("close db connection failed!", e2);
			}
		}
		if (count == 0)
			return 0;
		return count / pageSize + 1;
	}

	public void loadOneUSubjectData2CacheByTimeWithSql(String uidentify,
			String tableName, String stime) {

		try {
			try {
				SqlSession sse = BaseFactory.getSqlSessionFactory()
						.openSession();
				Connection con = sse.getConnection();

				ResultSet rs = null;
				try {
					Statement stmt = con.createStatement();
					String sql = " select  uidentify, INDEX_CODE, TIME,VALUE from "
							+ tableName
							+ " where  uidentify='"
							+ uidentify
							+ "'";
					if (!StringUtil.isEmpty(stime)) {
						sql = " select  uidentify, INDEX_CODE, TIME,VALUE from "
								+ tableName
								+ " where  uidentify='"
								+ uidentify
								+ "' and time >'" + stime + "'";
					}
					rs = stmt.executeQuery(sql);

					if (rs != null) {
						try {
							while (rs.next()) {
								try {
									String indexcode = rs
											.getString("index_code");
									Float v = rs.getFloat("value");
									Date time = rs.getDate("time");
									TUextService.getInstance().putData(
											uidentify, time.getTime(),
											indexcode, v);

								} catch (Exception e) {
									log.error("put data 2 cache failed!", e);
								}

							}
						} catch (Exception e) {
							log.error("put data 2 cache failed!", e);
						}
					}
				} catch (Exception e) {
					log.error("query data failed!", e);
				} finally {
					try {
						sse.close();
					} catch (Exception e2) {
						log.error("close db connection failed!", e2);
					}
				}

			} catch (Exception e) {
				log.error("load Asset data failed!", e);
			}
		} catch (Exception e) {
			log.error("load Asset data failed!", e);
		}
	}
}
