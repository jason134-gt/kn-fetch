package com.yfzx.service.db;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.CompileMode;
import com.stock.common.constants.SCache;
import com.stock.common.constants.SExt;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.StockSqlKey;
import com.stock.common.model.Cfirule;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.USubject;
import com.stock.common.model.UpdateIndexReq;
import com.stock.common.model.company.Index0001;
import com.stock.common.model.trade.StockTrade;
import com.stock.common.msg.Message;
import com.stock.common.msg.common.CIndexSeries;
import com.stock.common.msg.common.DataItem;
import com.stock.common.msg.common.SimpleDataItem;
import com.stock.common.msg.common.SimpleSeries;
import com.stock.common.util.ComputeUtil;
import com.stock.common.util.DateUtil;
import com.stock.common.util.DictUtil;
import com.stock.common.util.LogSvr;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.stock.common.util.UnitUtil;
import com.yfzx.service.StockCenter;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.cache.IndexCacheService;
import com.yfzx.service.cache.IndustryExtCacheService;
import com.yfzx.service.client.DcssCompanyExtIndexServiceClient;
import com.yfzx.service.factory.SMsgFactory;
import com.yfzx.service.hfunction.HDayService;
import com.yfzx.service.hfunction.HMonthService;
import com.yfzx.service.hfunction.HWeekService;
import com.yfzx.service.msg.event.ShareTimerTradeDataWapper;
import com.yfzx.service.trade.ShareTimerTradeService;
import com.yfzx.service.trade.StockTradeService;
import com.yfzx.service.trade.TradeBitMapService;
import com.yfzx.service.trade.TradeCenter;
import com.yfzx.service.trade.TradeService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.config.BaseConfiguration;
import com.yz.mycore.core.log.LogManager;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.util.BaseUtil;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.cache.EhcacheImpl;
import com.yz.mycore.lcs.config.CacheConfig;
import com.yz.mycore.lcs.enter.LCEnter;

/**
 * 指标服务的缺省类
 * 
 * @author user
 * 
 */
public class IndexService {

	private final static String SELECT = "com.yz.stock.portal.dao.stock0010.select";
	private static IndexService instance = new IndexService();
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	DictService ds = DictService.getInstance();
	static Logger logger = LoggerFactory.getLogger(IndexService.class);
	IndexService is = IndexService.getInstance();
	private static String _iPrex = "index";

	private IndexService() {

	}

	public static IndexService getInstance() {
		return instance;
	}

	// 格式：000002.sz^1388678400000^2022
	public <K, V> void put2Cache(K k, V v) {
		String[] ka = k.toString().split("\\^");
		// String pk = ka[0] + "^" + ka[1];
		Object indexcode = ka[2];
		if (indexcode.toString().indexOf("_") < 0)
			indexcode = Integer.valueOf(ka[2]);
		if (indexcode == null)
			return;
		USubject us = USubjectService.getInstance()
				.getUSubjectByUIdentifyFromCache(ka[0]);
		if (us == null)
			return;
		if (us.getType() == StockConstants.SUBJECT_TYPE_0) {
			Dictionary d = DictService.getInstance().getDataDictionary(
					indexcode.toString());
			if (StockUtil.isBaseIndex(d.getType())) {
				IndexCacheService.getInstance().putBaseIndexValue2Cache(ka[0],
						Long.valueOf(ka[1]), ka[2], v);
			} else {
//				ExtCacheService.getInstance().put(k, v);
				TUextService.getInstance().putData(us.getUidentify(), Long.valueOf(ka[1]), ka[2], ((Double)v).floatValue());
			}

		}
		if (us.getType() == StockConstants.SUBJECT_TYPE_1) {
			IndustryExtCacheService.getInstance().put(k, v);
		}

	}

	public List<Index0001> getIndex0001Select(String stockCode) {
		Index0001 value = null;

		try {
			Map map = BeanUtils.describe(value);
			RequestMessage reqMsg = DAFFactory.buildRequest(SELECT, map,
					StockConstants.COMPANY_TYPE);
			value = (Index0001) pLayerEnter.queryForObject(reqMsg);
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return null;// value;
	}

	public int getTableDataCount(String tableName) {
		int count = 0;
		try {
			RequestMessage reqMsg = DAFFactory.buildRequest(
					StockSqlKey.get_Table_DataCount, tableName,
					StockConstants.common);
			Object o = pLayerEnter.queryForObject(reqMsg);
			if (o == null)
				return 0;
			count = (Integer) o;
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return count;
	}

	@SuppressWarnings("rawtypes")
	public List getIndexListByTime(String tableName, String columnName,
			String time) {
		Object vList = null;
		Map<String, String> m = new HashMap<String, String>();
		m.put("time", time);
		m.put("columnName", columnName);
		m.put("tableName", tableName);
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"select_index_one_time", m, StockConstants.common);
		vList = pLayerEnter.queryForList(reqMsg);
		if (vList == null) {
			return null;
		}
		return (List) vList;
	}

	public Double getCompanyExtIndexValueFromExtCache(Dictionary d,
			IndexMessage tmsg) {
		IndexMessage msg = (IndexMessage) tmsg.clone();
		msg.setColumnName(d.getColumnName());
		msg.setIndexCode(d.getIndexCode());
		msg.setTableName(SExt.getUExtTableName(msg.getUidentify(), d));
		Double v = TUextService.getInstance().getUExtDouble(msg.getCompanyCode(), msg.getTime().getTime(), d.getIndexCode());
		if (v == null && StockCenter.getInstance().isNeedAccessDcss(tmsg)) {
			String key = StockUtil.getExtCachekey(msg.getUidentify(),
					d.getIndexCode(), msg.getTime());
			// 数据已全量缓存，不从数据库取，只从分布式缓存到
			v = DcssCompanyExtIndexServiceClient.getInstance().get(key);
			if (v == null)
				return null;
		}
		return v;
	}

	public Double getCompanyExtIndexValue(Dictionary d, IndexMessage msg) {
		Double v = getCompanyExtIndexValueFromExtCache(d, msg);
		if (v == null) {
			if (!StockUtil.isBaseIndex(d.getType())
					&& (DictUtil.isNotSave(d) || msg.isNeedComput())) {
				if (CompileMode.getCurrentMode() != CompileMode.mode_1) {
					msg = (IndexMessage) msg.clone();
					msg.setNeedAccessExtIndexDb(false);
					msg.setNeedAccessCompanyBaseIndexDb(false);
				}
				v = realComputeIndexOneTime(msg);
			}

			if (msg.isNeedAccessExtIndexDb()) {
				v = getCompanyExtIndexValueFromDb(d, msg);
			}
			if (v != null) {
				msg = (IndexMessage) msg.clone();
				msg.setColumnName(d.getColumnName());
				msg.setIndexCode(d.getIndexCode());
				msg.setTableName(SExt.getUExtTableName(msg.getUidentify(), d));
//				ExtCacheService.getInstance().put(msg.getKey(), v);
				TUextService.getInstance().putData(msg.getCompanyCode(), msg.getTime().getTime(), d.getIndexCode(), v.floatValue());
			}
		}

		return v;
	}

	// private boolean needRealComputeCurrentValue(Dictionary d, IndexMessage
	// im) {
	// Boolean ret = false;
	// if (!StockUtil.isBaseIndex(d.getType())
	// && d.getBitSet().charAt(4) == '1') {
	// Date rctime = im.getTime();
	// Date ctime = StockUtil.getApproPeriod(new Date());
	// if (StockUtil.isCompanyMsg(im)) {
	// Company c = CompanyService.getInstance().getCompanyByCode(
	// im.getCompanyCode());
	// Date creportTime = new Date(c.getCreportTime());
	// if (ctime.equals(rctime) || creportTime.equals(rctime)) {
	// ret = true;
	// }
	// } else {
	// Date creportTime = StockUtil.getDefaultPeriodTime(null);
	// if (ctime.equals(rctime) || creportTime.equals(rctime)) {
	// ret = true;
	// }
	// }
	// }
	// return ret;
	// }

	private Double realComputeIndexOneTime(IndexMessage msg) {
		CRuleService crs = CRuleService.getInstance();
		Cfirule r = crs.getCfruleByCodeFromCache(msg.getIndexCode());
		return crs.computeIndex(msg, r);
	}

	private Double getCompanyExtIndexValueFromDb(Dictionary d, IndexMessage msg) {
		IndexMessage req = msg;
		req.setColumnName(d.getColumnName());
		req.setTableName(d.getTableName());
		req.setIndexCode(d.getIndexCode());
		String tableName = SExt.getUExtTableName(req.getUidentify(), d);
		req.setTableName(tableName);

		IIService is = getIndexService(d.getType());
		Object o = is.getIndexValue(req);
		if (o == null)
			return null;
		return (Double) o;
	}

	public Double getCompanyIndexValue(Message msg) {
		Object value = null;
		try {
			IndexMessage req = (IndexMessage) msg;
			// TODO Auto-generated method stub
			Dictionary d = ds.getDataDictionary(req.getIndexCode());
			req.setColumnName(d.getColumnName());
			req.setTableName(d.getTableName());
			// 加一层缓存，数据已提前加载到缓存中了(此处全量加载了数据，以缓存中的数据为准，如果没有查到数据，则认为不存在）
			if (StockUtil.isBaseIndex(d.getType())) {
				value = getCompanyBaseIndexFromCache(msg);
			} else {
				req.setTableName(SExt.getUExtTableName(req.getUidentify(), d));
				value = getCompanyExtIndexValueFromExtCache(d, req);
			}
			if (value == null) {
				// 从数据库中取
				// 如果不是基本指标类型,则请求相应的指标服务类处理
				if (!StockUtil.isBaseIndex(d.getType())) {
					IIService is = getIndexService(d.getType());
					value = is.getIndexValue(req);
				} else {
					Double dv = getCompanyBaseIndexValueFromDb(msg);
					if (dv != null)
						value = dv;
				}
				// if (value == null) {
				// // 减少从数据库查询空值数据
				// value = 0.0;
				// }
			}

		} catch (Exception e) {
			// TODO: handle exception
			logger.error("operator failed!", e);
		}
		if (value == null)
			return null;
		return (Double) value;
	}

	/**
	 * 返回结果为Map
	 * 
	 * @param req
	 * @return
	 */
	public Object getCompanyIndexMapValue(Message msg) {
		Object value = 0.0;
		try {
			IndexMessage req = (IndexMessage) msg;
			Dictionary d = ds.getDataDictionary(req.getIndexCode());
			req.setColumnName(d.getColumnName());
			req.setTableName(d.getTableName());
			// 如果不是基本指标类型,则请求相应的指标服务类处理
			if (!StockUtil.isBaseIndex(d.getType())) {
				// 如果为扩展指标,则计算扩展表的表名
				req.setTableName(SExt.getUExtTableName(req.getUidentify(), d));
				IIService is = getIndexService(d.getType());
				return is.getIndexObject(req);
			}
			RequestMessage reqMsg = DAFFactory.buildRequest(
					StockSqlKey.base_index_key_query_2, req,
					StockConstants.common);
			value = pLayerEnter.queryForObject(reqMsg);

		} catch (Exception e) {
			// TODO: handle exception
			logger.error("operator failed!", e);
		}
		return value;
	}

	@SuppressWarnings("static-access")
	public void updateBaseIndex2Db(String companyCode, String tableName,
			String colName, String indexValue, Date time) {
		try {
			UpdateIndexReq req = new UpdateIndexReq();
			req.setColumnName(colName);
			req.setTableName(tableName);
			req.setTime(time);
			req.setUidentify(companyCode);
			req.setValue(indexValue);
			// 此处较特殊,需要查此公司某个特定时间点的一行指示数是否存在,而不是单个列的列,如果不存在
			// 就新增,否则就把指示数加到已存在的行中
			RequestMessage reqMsg = DAFFactory.buildRequest(
					StockSqlKey.base_index_key_query_2, req,
					StockConstants.common);
			Object value = pLayerEnter.queryForObject(reqMsg);
			if (value == null) {
				// 改为指量异步更新
				// ExecuteQueueManager.getInstance()
				// .add2IQueue(
				// new BatchQueueEntity(
				// StockConstants.TYPE_BASEINDEX, tableName,req));

				reqMsg.setSqlMapKey(StockSqlKey.base_index_key_insert_0);
				pLayerEnter.insert(reqMsg);
			} else {
				// 改为指量异步更新
				// ExecuteQueueManager.getInstance()
				// .add2IQueue(
				// new BatchQueueEntity(
				// StockConstants.TYPE_BASEINDEX,tableName, req));

				reqMsg.setSqlMapKey(StockSqlKey.base_index_key_update_0);
				pLayerEnter.modify(reqMsg);
			}
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("operator failed!", e);
		}
	}

	public void update2ExtTable(String companyCode, String tableName,
			String colName, String indexValue, Date time, String indexCode,
			String indexName) {
		try {
			// TODO Auto-generated method stub
			UpdateIndexReq req = new UpdateIndexReq();
			req.setColumnName(colName);
			req.setTableName(tableName);
			req.setTime(time);
			req.setValue(indexValue);
			req.setIndexCode(indexCode);
			req.setUidentify(companyCode);
			req.setIndexName(indexName);
			RequestMessage reqMsg = DAFFactory.buildRequest(
					StockSqlKey.ext_c_index_key_query_0, req,
					StockConstants.common);
			Object value = pLayerEnter.queryForObject(reqMsg);
			// 从bloomfilter中查找数据是否已在数据库中
			// String key = StockUtil.getExtTableVokey(companyCode, indexCode,
			// time);
			// boolean isPresent = StockUtil.isPresentInDb(key);
			if (value == null) {
				// if (!isPresent) {
				// 改为指量异步更新
				// ExecuteQueueManager.getInstance()
				// .add2IQueue(
				// new BatchQueueEntity(
				// StockConstants.TYPE_EXTINDEX,tableName, req));

				reqMsg.setSqlMapKey(StockSqlKey.ext_c_index_key_insert_0);
				pLayerEnter.insert(reqMsg);
			} else {
				// 改为指量异步更新
				// ExecuteQueueManager.getInstance()
				// .add2UQueue(
				// new BatchQueueEntity(
				// StockConstants.TYPE_EXTINDEX,tableName, req));

				reqMsg.setSqlMapKey(StockSqlKey.ext_c_index_key_update_0);
				pLayerEnter.modify(reqMsg);
			}
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("operator failed!", e);
		}
	}

	/*
	 * 返回结果是Double的list
	 */
	@SuppressWarnings("rawtypes")
	public List<Double> getCompanyIndexValueDoubleList(Message msg) {
		Object vList = null;
		try {
			IndexMessage req = (IndexMessage) msg;
			Dictionary d = ds.getDataDictionary(req.getIndexCode());
			req.setColumnName(d.getColumnName());
			req.setTableName(d.getTableName());
			// 如果不是基本指标类型,则请求相应的指标服务类处理
			if (StockUtil.isBaseIndex(d.getType())) {
				// 如果为扩展指标,则计算扩展表的表名
				req.setTableName(SExt.getUExtTableName(req.getUidentify(), d));
				IIService is = getIndexService(d.getType());
				return is.getIndexList(req);
			}
			RequestMessage reqMsg = DAFFactory.buildRequest(
					StockSqlKey.base_index_key_query_1, req,
					StockConstants.common);
			vList = pLayerEnter.queryForList(reqMsg);
			if (vList == null) {
				return null;
			}
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("get index value list failed!", e);
		}
		return (List) vList;
	}

	/*
	 * 返回结果是Map的list
	 */
	@SuppressWarnings("rawtypes")
	public List<Map> getCompanyIndexValueMapList(Message msg) {
		List<Map> ml = null;
		IndexMessage im = (IndexMessage) msg;
		Dictionary d = DictService.getInstance().getDataDictionary(
				im.getIndexCode());
		if (DictUtil.needRealCompute(d) && !StockUtil.isBaseIndex(d.getType())) {
			ml = realComputeIndex(msg);
		} else {
			ml = getCompanyIndexValueMapListNotRealCompute(msg);
		}
		return ml;
	}

	private List<Map> realComputeIndex(Message msg) {
		List<Map> ml = new ArrayList<Map>();
		CRuleService crs = CRuleService.getInstance();
		IndexMessage im = (IndexMessage) msg;
		Date sTime = im.getStartTime();
		Date eTime = im.getEndTime();
		Dictionary d = DictService.getInstance().getDataDictionary(
				im.getIndexCode());
		Cfirule rule = crs.getCfruleByCodeFromCache(im.getIndexCode());
		while (sTime.compareTo(eTime) <= 0) {
			try {
				IndexMessage req = (IndexMessage) im.clone();
				req.setTime(sTime);
				// 编译规则
				req.setNeedComput(true);
				req.setNeedUseExtDataCache(false);
				req.setNeedRealComputeIndustryValue(true);
				req.setNeedAccessExtRemoteCache(false);
				req.setNeedAccessExtIndexDb(true);
				req.setNeedAccessCompanyBaseIndexDb(true);
				String caRegion = StockUtil.computeCurAccountRegion(sTime,
						im.getAccountRegion(), im.getcAccountRegion());
				req.setcAccountRegion(caRegion);

				// 编译规则
				Double value = crs.computeIndex(req, rule);
				if (value != null && value != 0) {
					Map<String, Object> m = new HashMap<String, Object>();
					m.put(StockConstants.C_TIME_NAME, sTime);
					m.put(d.getColumnName(), value);
					ml.add(m);
				}
			} catch (Exception e) {
				logger.error("compute rule failed!", e);
			}
			sTime = StockUtil.getNextTimeV3(sTime, d.getInterval(),
					d.getTunit());
		}
		return ml;
	}

	@SuppressWarnings("unchecked")
	private List<Map> getCompanyIndexValueMapListNotRealCompute(Message msg) {
		Object vList = null;
		try {
			IndexMessage req = (IndexMessage) msg;
			Dictionary d = ds.getDataDictionary(req.getIndexCode());
			String tablename = d.getTableName();
			if(req.getCompanyCode().endsWith(".hk")&&d.getTableName().equals("v_trade0001"))
			{
				tablename="v_trade0001_hk";
			}
			req.setColumnName(d.getColumnName());
			req.setTableName(tablename);
			// 如果不是基本指标类型,则请求相应的指标服务类处理
			if (!StockUtil.isBaseIndex(d.getType())) {
				// 如果为扩展指标,则计算扩展表的表名
				req.setTableName(SExt.getUExtTableName(req.getUidentify(), d));
				// IIService is = getIndexService(d.getType());
				// return is.getIndexObjectList(req);
				System.out.println("companycode:" + req.getCompanyCode()
						+ ";tablename:" + req.getTableName());
				return USubjectService.getInstance().getIndexObjectList(req);
			}
			RequestMessage reqMsg = DAFFactory.buildRequest(
					StockSqlKey.base_index_key_query_3, req,
					StockConstants.common);
			vList = pLayerEnter.queryForList(reqMsg);
			if (vList == null) {
				return null;
			}
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("get index value list failed!", e);
		}
		return (List) vList;
	}

	public static Map<String, IIService> _isMap = new HashMap<String, IIService>();

	public IIService getIndexService(int key) {
		return _isMap.get(_iPrex + key);
	}

	// 注册指标服务类
	static {
		_isMap.put(_iPrex + StockConstants.INDEX_TYPE_6,
				CIndexService.getInstance());
		// _isMap.put(_iPrex + StockConstants.INDEX_TYPE_6,
		// CIndexService.getInstance());
		// _isMap.put(_iPrex + StockConstants.I_EXT_INDEX_TYPE,
		// IIndexService.getInstance());
		// _isMap.put(_iPrex + StockConstants.U_EXT_INDEX_TYPE,
		// UIndexService.getInstance());
	}

	// public void upateIndex2Db(SContext ctx, Message msg) {
	// // TODO Auto-generated method stub
	// IndexMessage req = (IndexMessage) msg;
	// Dictionary d = ctx.getDictionary();
	// int iType = d.getType();
	// // 分开基础指标与非基础指标
	// if (!StockUtil.isBaseIndex(iType)) {
	// req.setTableName(StockUtil.getExtIndexTableNameByCompany(req.getCompanyCode()));
	// req.setColumnName(StockConstants.C_EXT_INDEX_VALUE);
	// update2ExtTable(req.getCompanyCode(), req.getTableName(),
	// req.getColumnName(), String.valueOf(req.getValue()),
	// req.getTime(), req.getIndexCode(),d.getColumnChiName());
	// } else {
	// // 更新计算结果到数据库中
	// updateBaseIndex2Db(req.getCompanyCode(), req.getTableName(),
	// req.getColumnName(), String.valueOf(req.getValue()),
	// req.getTime());
	// }
	// }

	public void upateIndex2Db(Dictionary d, Message msg) {
		// TODO Auto-generated method stub
		IndexMessage req = (IndexMessage) msg;
		int iType = d.getType();
		// 分开基础指标与非基础指标
		if (!StockUtil.isBaseIndex(iType)) {
			req.setTableName(SExt.getUExtTableName(req.getUidentify(), d));
			req.setColumnName(StockConstants.C_EXT_INDEX_VALUE);
			update2ExtTable(req.getUidentify(), req.getTableName(),
					req.getColumnName(), String.valueOf(req.getValue()),
					req.getTime(), req.getIndexCode(), d.getColumnChiName());

		} else {
			// 更新计算结果到数据库中
			updateBaseIndex2Db(req.getUidentify(), req.getTableName(),
					req.getColumnName(), String.valueOf(req.getValue()),
					req.getTime());
		}
	}

	public Date getMaxTime(String companycode) {
		Date maxTime = null;
		try {
			String key = "maxTime_" + companycode;
			Object d = LCEnter.getInstance().get(key,
					StockUtil.getCacheName(StockConstants.common));
			if (d == null) {
				IndexMessage im = SMsgFactory.getUMsg(companycode);
				im.setTableName(StockConstants.TABLE_NAME_ASSET0290);
				RequestMessage reqMsg = DAFFactory.buildRequest(
						StockSqlKey.select_companydata_maxtime, im,
						StockConstants.common);
				d = pLayerEnter.queryForObject(reqMsg);
				if (d == null)
					return null;
				LCEnter.getInstance().put(key, d,
						StockUtil.getCacheName(StockConstants.common));
			}
			maxTime = (Date) d;

		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return maxTime;
	}

	public Date getMinTime(String companycode) {
		Date minTime = null;
		try {
			String key = "minTime_" + companycode;
			Object d = LCEnter.getInstance().get(key,
					StockUtil.getCacheName(StockConstants.common));
			if (d == null) {
				IndexMessage im = SMsgFactory.getUMsg(companycode);
				im.setTableName(StockConstants.TABLE_NAME_ASSET0290);
				RequestMessage reqMsg = DAFFactory.buildRequest(
						StockSqlKey.select_companydata_mintime, im,
						StockConstants.common);
				d = pLayerEnter.queryForObject(reqMsg);
				if (d == null)
					return null;
				LCEnter.getInstance().put(key, d,
						StockUtil.getCacheName(StockConstants.common));
			}
			minTime = (Date) d;
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return minTime;
	}

	public void clearMaxMinTimeCache(String companycode) {
		String key = "minTime_" + companycode;
		LCEnter.getInstance().remove(key,
				StockUtil.getCacheName(StockConstants.common));
		key = "maxTime_" + companycode;
		LCEnter.getInstance().remove(key,
				StockUtil.getCacheName(StockConstants.common));

	}

	public void rebuildMaxMinTimeCache(String companycode) {
		clearMaxMinTimeCache(companycode);
		Company c = CompanyService.getInstance().getCompanyByCodeFromCache(
				companycode);
		Date time = IndexService.getInstance().getMaxTime(c.getCompanyCode());
		if (time != null)
			c.setCreportTime(time.getTime());
	}

	public Date getMinTime(String companycode, Date uptime) {
		Date minTime = null;
		try {
			String key = "uptime_minTime_" + companycode;
			Object d = LCEnter.getInstance().get(key,
					StockUtil.getCacheName(StockConstants.common));
			if (d == null) {
				Map<String, String> m = new HashMap<String, String>();
				m.put("companycode", companycode);
				m.put("modtime", DateUtil.format2String(uptime));
				m.put("tableName", StockConstants.TABLE_NAME_ASSET0290);

				RequestMessage reqMsg = DAFFactory.buildRequest(
						"select_companydata_mintime_uptime", m,
						StockConstants.common);
				d = pLayerEnter.queryForObject(reqMsg);
				if (d == null)
					return null;
				LCEnter.getInstance().put(key, d,
						StockUtil.getCacheName(StockConstants.common));
			}
			minTime = (Date) d;
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}
		return minTime;
	}

	public List<Map> getCompanysIndexValueMapListFromDb(Dictionary gd,
			String sTime, String companycodes) {
		Object vList = null;
		try {
			if (StringUtil.isEmpty(companycodes))
				return null;
			String columnName = StockUtil.getExtCovertTableIndexCode(gd
					.getIndexCode());
			String tableName = "t_c_base_financial_index";
			Map<String, String> m = new HashMap<String, String>();
			m.put("columnName", columnName);
			m.put("tableName", tableName);
			m.put("companycodes", companycodes);
			m.put("time", sTime);
			RequestMessage reqMsg = DAFFactory.buildRequest(
					"getIndexvalueListDesc", m, StockConstants.common);
			vList = pLayerEnter.queryForList(reqMsg);
			if (vList == null) {
				return null;
			}
		} catch (Exception e) {
			logger.error("opt db failed!", e);
		}
		return (List) vList;
	}

	/*
	 * companyCodelist 公司编码列表用‘|’分隔 返回的结果是一个map的list
	 */
	public List<Map> getCompnaysIndexMapListFromDb(String companyCodelist,
			String indexCode, String tableName, String columnName,
			String sTime, String eTime) {
		Object vList = null;
		companyCodelist = companyCodelist.replace("|", ",");
		Map<String, String> m = new HashMap<String, String>();
		m.put("indexCode", indexCode);
		m.put("companyCodelist", companyCodelist);
		m.put("tableName", tableName);
		m.put("columnName", columnName);
		m.put("sTime", sTime);
		m.put("eTime", eTime);
		RequestMessage reqMsg = DAFFactory.buildRequest("getIndexListMap", m,
				StockConstants.common);
		vList = pLayerEnter.queryForList(reqMsg);
		if (vList == null) {
			return null;
		}
		return (List) vList;
	}

	/**
	 * 是对非扩展表使用
	 * 
	 * @param tableName
	 * @param columnName
	 * @param time
	 * @param companycode
	 * @return
	 */
	public Double getCompanyIndexValueFromDb(String tableName,
			String columnName, String time, String companycode) {
		Object rd = null;
		Map<String, String> m = new HashMap<String, String>();
		m.put("time", time);
		m.put("columnName", columnName);
		m.put("tableName", tableName);
		m.put("companycode", companycode);
		RequestMessage req = DAFFactory.buildRequest(
				"select_index_one_companyOftime", m, StockConstants.common);
		rd = pLayerEnter.queryForObject(req);
		if (rd == null) {
			return null;
		}
		return (Double) rd;
	}

	/**
	 * 此处选择转换后的扩展指标表，以减少查询查。取此指标的所有数据
	 * 
	 * @param tableName
	 * @param columnName
	 * @param time
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List getCompanyIndexListFromMidCompanyIndexExt(Dictionary d) {

		String columnName = StockUtil.getExtCovertTableIndexCode(d
				.getIndexCode());
		String tableName = "t_c_base_financial_index";
		Object vList = null;
		Map<String, String> m = new HashMap<String, String>();
		m.put("columnName", columnName);
		m.put("tableName", tableName);
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"select_index_all_data", m, StockConstants.common);
		vList = pLayerEnter.queryForList(reqMsg);
		if (vList == null) {
			return null;
		}
		return (List) vList;
	}

	/**
	 * 此处选择转换后的扩展指标表，以减少查询查。
	 * 
	 * @param tableName
	 * @param columnName
	 * @param time
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List getCompanyIndexListFromMidCompanyIndexExt(Dictionary d,
			String time) {
		String columnName = StockUtil.getExtCovertTableIndexCode(d
				.getIndexCode());
		String tableName = "t_c_base_financial_index";
		Object vList = null;
		Map<String, String> m = new HashMap<String, String>();
		m.put("time", time);
		m.put("columnName", columnName);
		m.put("tableName", tableName);
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"select_index_one_time", m, StockConstants.common);
		vList = pLayerEnter.queryForList(reqMsg);
		if (vList == null) {
			return null;
		}
		return (List) vList;
	}

	/**
	 * 加载某一时间段，某一指标的所有数据--从基表中
	 * 
	 * @param d
	 * @param time
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List getCompanyIndexListFromBaseOneTime(Dictionary d, String time) {
		String columnName = d.getColumnName();
		String tableName = d.getTableName();
		Object vList = null;
		Map<String, String> m = new HashMap<String, String>();
		m.put("time", time);
		m.put("columnName", columnName);
		m.put("tableName", tableName);
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"select_index_one_time", m, StockConstants.common);
		vList = pLayerEnter.queryForList(reqMsg);
		if (vList == null) {
			return null;
		}
		return (List) vList;
	}

	/*
	 * stime为上市时间 查找上市时间，在stime之前，并且没有退市的公司数据
	 */
	public Double getCompanyNumTrends(List<String> cl, String indexcode,
			Double drlow, Double drhigh, Date time, Date stime) {
		Double count = 0.0;
		for (String code : cl) {
			String k = StockUtil.getExtCachekey(code, indexcode, time);
			Double v = DcssCompanyExtIndexServiceClient.getInstance().get(k);
			if (v != null && v >= drlow && v <= drhigh)
				count++;
		}
		// String columnName = StockUtil.getExtCovertTableIndexCode(indexcode);
		// String tableName = "t_c_base_financial_index";
		// if (StockUtil.isBaseIndex(d.getType())) {
		// columnName = d.getColumnName();
		// tableName = d.getTableName();
		// }
		//
		// Object num = null;
		// Map<String, Object> m = new HashMap<String, Object>();
		// m.put("tag", tag);
		// m.put("stime", stime);
		// m.put("time", time);
		// m.put("drlow", drlow);
		// m.put("drhigh", drhigh);
		// m.put("columnName", columnName);
		// m.put("tableName", tableName);
		// RequestMessage reqMsg = DAFFactory.buildRequest(
		// "select_index_company_num_region", m, StockConstants.common);
		// num = pLayerEnter.queryForObject(reqMsg);
		// if (num == null) {
		// return null;
		// }
		return count;
	}

	/*
	 * stime为上市时间 查找上市时间，在stime之前上市 ，并且没有退市的公司数
	 */
	public Double getOneTagCompanySumBeforeStime(String tag, Date stime) {
		Object num = null;
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("tag", tag);
		m.put("stime", stime);

		RequestMessage reqMsg = DAFFactory.buildRequest(
				"getOneTagCompanySumBeforeStime", m, StockConstants.common);
		num = pLayerEnter.queryForObject(reqMsg);
		if (num == null) {
			return null;
		}
		return (Double) num;
	}

	public List<String> getOneTagCompanysBeforeStime(String tag, Date stime) {
		Object companycodes = null;
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("tag", tag);
		m.put("stime", stime);

		RequestMessage reqMsg = DAFFactory.buildRequest(
				"getOneTagCompanysBeforeStime", m, StockConstants.common);
		companycodes = pLayerEnter.queryForList(reqMsg);
		if (companycodes == null) {
			return null;
		}
		return (List<String>) companycodes;
	}

	public Double getCompanyBaseIndexValueFromDb(Message msg) {
		Object value = null;
		try {
			IndexMessage im = (IndexMessage) msg;
			RequestMessage reqMsg = DAFFactory.buildRequest(
					StockSqlKey.base_index_key_query_0, im,
					StockConstants.INDEX_DATA_TYPE);
			value = pLayerEnter.queryForObject(reqMsg);
			if (value == null) {
				return null;
			}

		} catch (Exception e) {
			// TODO: handle exception
			logger.error("operator failed!", e);
		}
		return (Double) value;
	}

	public Double getCompanyBaseIndexFromCache(Message msg) {
		Object value = null;
		try {
			IndexMessage req = (IndexMessage) msg;
			// TODO Auto-generated method stub
			Dictionary d = ds.getDataDictionaryFromCache(req.getIndexCode());
			req.setColumnName(d.getColumnName());
			req.setTableName(d.getTableName());
			value = IndexCacheService.getInstance().getValue(req);
			if (value == null
					&& StockCenter.getInstance().isNeedAccessDcss(req)) {
				value = DcssCompanyExtIndexServiceClient.getInstance()
						.getBaseIndexValue(req.getUidentify(),
								req.getIndexCode(), req.getTime());
			}
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("operator failed!" + msg, e);
		}
		if (value instanceof Long)
			value = ((Long) value).doubleValue();
		if (value instanceof Integer)
			value = ((Integer) value).doubleValue();
		if (value instanceof Float)
			value = ((Float) value).doubleValue();
		return (Double) value;
	}

	public Double getCompanyBaseIndexValue(IndexMessage msg) {
		Double v = getCompanyBaseIndexFromCache(msg);
		if (msg.isNeedAccessCompanyBaseIndexDb() && v == null) {
			v = getCompanyBaseIndexValueFromDb(msg);
		}
		return v;
	}

	public void cacheCompanyMidResult(Dictionary d, IndexMessage midmsg,
			Double value) {
		IndexMessage msg = (IndexMessage) midmsg.clone();
		msg.setColumnName(d.getColumnName());
		msg.setIndexCode(d.getIndexCode());
		msg.setTableName(SExt.getUExtTableName(msg.getUidentify(), d));
//		ExtCacheService.getInstance().putV(msg.getKey(), value);
		TUextService.getInstance().putData(msg.getCompanyCode(), msg.getTime().getTime(), d.getIndexCode(), value.floatValue());

	}

	@SuppressWarnings("unchecked")
	public List<Map> getAllCompanyOneExtIndexOfOneTag2cache(String tag,
			String indexcode, Date time) {
		Object value = null;
		try {
			Dictionary d = DictService.getInstance().getDataDictionary(
					indexcode);
			String tableName = StockConstants.TABLE_NAME_C_EXT_MID_TABLE;
			String columnName = StockUtil.getExtCovertTableIndexCode(d
					.getIndexCode());

			Map<String, String> m = new HashMap<String, String>();
			m.put("tableName", tableName);
			m.put("columnName", columnName);
			m.put("time", DateUtil.format2String(time));
			m.put("tag", tag);
			RequestMessage reqMsg = DAFFactory.buildRequest(
					"getAllCompanyOneIndexOfOneTag2cache", m,
					StockConstants.INDEX_DATA_TYPE);
			value = pLayerEnter.queryForList(reqMsg);
			if (value == null) {
				return null;
			}

		} catch (Exception e) {
			// TODO: handle exception
			logger.error("operator failed!", e);
		}
		return (List<Map>) value;
	}

	public List<Map> getCompanyIndexListOneTimeFromMidCompanyIndexExt(
			String time, int start, int limit) {
		Object value = null;
		try {
			String tableName = StockConstants.TABLE_NAME_C_EXT_MID_TABLE;
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("tableName", tableName);
			m.put("time", time);
			m.put("start", start);
			m.put("limit", limit);
			RequestMessage reqMsg = DAFFactory.buildRequest(
					"getCompanyIndexListOneTimeFromMidCompanyIndexExt", m,
					StockConstants.INDEX_DATA_TYPE);
			value = pLayerEnter.queryForList(reqMsg);
			if (value == null) {
				return null;
			}

		} catch (Exception e) {
			// TODO: handle exception
			logger.error("operator failed!", e);
		}
		return (List<Map>) value;
	}

	public Integer getCompanyExtIndexCountOneFromMidTable(String time) {

		Object value = null;
		try {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("time", time);
			RequestMessage reqMsg = DAFFactory.buildRequest(
					"getCompanyExtIndexCountOneFromMidTable", m,
					StockConstants.INDEX_DATA_TYPE);
			value = pLayerEnter.queryForObject(reqMsg);
			if (value == null) {
				return null;
			}

		} catch (Exception e) {
			// TODO: handle exception
			logger.error("operator failed!", e);
		}
		return (Integer) value;
	}

	public List<CIndexSeries> getIndexSeriesList(IndexMessage req, String rule) {
		List<CIndexSeries> cisl = new ArrayList<CIndexSeries>();
		try {
			CIndexSeries cis = new CIndexSeries();
			Date eTime = req.getTime();// 向前推三年
			Date sTime = StockUtil.getNextTime(eTime, -36);

			while (sTime.compareTo(eTime) <= 0) {
				try {
					IndexMessage im = (IndexMessage) req.clone();
					im.setTime(sTime);

					Double dv = CRuleService.getInstance().computeIndex(rule,
							im, StockConstants.DEFINE_INDEX);

					if (dv == null) {
						sTime = StockUtil
								.getNextTime(sTime, Integer.valueOf(3));
						logger.warn("value is null! req :" + im.toString());
						continue;
					}
					// 构造返回结果
					DataItem di = new DataItem(sTime, dv);
					cis.getData().add(di);
				} catch (Exception e) {
					// TODO: handle exception
					logger.error("compute rule failed!", e);
				}
				sTime = StockUtil.getNextTime(sTime, Integer.valueOf(3));
			}
			cisl.add(cis);

		} catch (Exception e) {
			logger.error("execute /getDynamicIndex failed", e);
			return null;
		}
		return cisl;
	}

	/**
	 * 返回简化的数据结构，为模板中展示图表的专用
	 * 
	 * @param req
	 * @param rule
	 * @return
	 */
	public List<SimpleSeries> getIndexSeriesListForShowChart(IndexMessage req,
			String rule) {
		List<SimpleSeries> cisl = new ArrayList<SimpleSeries>();
		// if(rule.matches("\\$\\{[0-9]+\\}"))
		// {
		// cisl = getIndexSimpileSeriesListFromDb(req,rule);
		// }
		// else
		// {
		cisl = getIndexSimplieSeriesListRealCompute(req, rule);
		// }
		return cisl;
	}

	private List<SimpleSeries> getIndexSimpileSeriesListFromDb(
			IndexMessage req, String rule) {
		List<SimpleSeries> cisl = new ArrayList<SimpleSeries>();
		String indexcode = rule.substring(2, rule.length() - 1);
		req.setIndexCode(indexcode);
		Date eTime = req.getTime();// 向前推三年
		Date sTime = StockUtil.getNextTime(eTime, -36);
		req.setStartTime(sTime);
		req.setEndTime(eTime);
		List<Map> vl = IndexValueAgent.getIndexMapList(req);
		SimpleSeries cis = new SimpleSeries();
		List<SimpleDataItem> dil = null;
		if (vl != null) {
			dil = getDataList(vl, req.getIndexCode(), cis);
		}
		if (dil != null) {
			cis.setD(dil);
		}
		cisl.add(cis);
		return cisl;
	}

	private List<SimpleDataItem> getDataList(List vl, String indexCode,
			SimpleSeries cis) {
		// TODO Auto-generated method stub
		List<SimpleDataItem> dil = new ArrayList<SimpleDataItem>();
		for (int i = 0; i < vl.size(); i++) {
			Map m = (Map) vl.get(i);
			// 时间
			Date tValue = (Date) m.get(StockConstants.C_TIME_NAME);
			// 指标名
			String iName = DictService.getInstance()
					.getDataDictionary(indexCode).getColumnName();
			// 指标值
			Double value = (Double) m.get(iName);
			if (tValue == null || value == null) {
				continue;
			}
			// 时间的毫秒值
			// Long time = tValue.getTime();
			Date time = tValue;
			SimpleDataItem sdi = new SimpleDataItem(time, value);
			dil.add(sdi);
		}
		return dil;
	}

	private List<SimpleSeries> getIndexSimplieSeriesListRealCompute(
			IndexMessage req, String rule) {
		List<SimpleSeries> cisl = new ArrayList<SimpleSeries>();
		try {
			SimpleSeries cis = new SimpleSeries();
			Date eTime = req.getTime();// 向前推三年
			Date sTime = StockUtil.getNextTime(eTime, -36);

			while (sTime.compareTo(eTime) <= 0) {
				try {
					IndexMessage im = (IndexMessage) req.clone();
					im.setTime(sTime);

					Double dv = CRuleService.getInstance().computeIndex(rule,
							im, StockConstants.DEFINE_INDEX);

					if (dv == null) {
						sTime = StockUtil
								.getNextTime(sTime, Integer.valueOf(3));
						logger.warn("value is null! req :" + im.toString());
						continue;
					}
					// 构造返回结果
					SimpleDataItem di = new SimpleDataItem(sTime,
							SMathUtil.getDouble(dv, 2));
					cis.getD().add(di);
				} catch (Exception e) {
					// TODO: handle exception
					logger.error("compute rule failed!", e);
				}
				sTime = StockUtil.getNextTime(sTime, Integer.valueOf(3));
			}
			cisl.add(cis);

		} catch (Exception e) {
			logger.error("execute /getDynamicIndex failed", e);
			return null;
		}
		return cisl;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Map> getCompanyIndexDataFromMidTable(String companycode,
			String indexList, Date stime, Date etime) {
		Object value = null;
		try {
			String tableName = StockConstants.TABLE_NAME_C_EXT_MID_TABLE;
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("companycode", companycode);
			m.put("indexList", indexList);
			m.put("stime", stime);
			m.put("etime", etime);
			m.put("tableName", tableName);
			RequestMessage reqMsg = DAFFactory.buildRequest(
					"getCompanyIndexDataFromMidTable", m,
					StockConstants.INDEX_DATA_TYPE);
			value = pLayerEnter.queryForList(reqMsg);
			if (value == null) {
				return null;
			}

		} catch (Exception e) {
			// TODO: handle exception
			logger.error("operator failed!", e);
		}
		return (List<Map>) value;
	}

	public List<Map> getAllCompanyOneExtIndexOneSecOfOneTag2cache(String tag,
			String indexcode, String stime, String etime) {
		Object value = null;
		try {
			Dictionary d = DictService.getInstance().getDataDictionary(
					indexcode);
			String tableName = StockConstants.TABLE_NAME_C_EXT_MID_TABLE;
			String columnName = StockUtil.getExtCovertTableIndexCode(d
					.getIndexCode());

			Map<String, String> m = new HashMap<String, String>();
			m.put("tableName", tableName);
			m.put("columnName", columnName);
			m.put("stime", stime);
			m.put("etime", etime);
			m.put("tag", tag);
			RequestMessage reqMsg = DAFFactory.buildRequest(
					"getAllCompanyOneExtIndexOneSecOfOneTag2cache", m,
					StockConstants.INDEX_DATA_TYPE);
			value = pLayerEnter.queryForList(reqMsg);
			if (value == null) {
				return null;
			}

		} catch (Exception e) {
			// TODO: handle exception
			logger.error("operator failed!", e);
		}
		return (List<Map>) value;
	}

	public List<Map> getAllCompanyOneExtIndexOneSec2cache(String indexcode,
			Date stime, Date etime) {
		Object value = null;
		try {
			Dictionary d = DictService.getInstance().getDataDictionary(
					indexcode);
			String tableName = StockConstants.TABLE_NAME_C_EXT_MID_TABLE;
			String columnName = StockUtil.getExtCovertTableIndexCode(d
					.getIndexCode());

			Map<String, String> m = new HashMap<String, String>();
			m.put("tableName", tableName);
			m.put("columnName", columnName);
			m.put("stime", DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, stime));
			m.put("etime", DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS, etime));
			RequestMessage reqMsg = DAFFactory.buildRequest(
					"getAllCompanyOneExtIndexOneSec2cache", m,
					StockConstants.INDEX_DATA_TYPE);
			value = pLayerEnter.queryForList(reqMsg);
			if (value == null) {
				return null;
			}

		} catch (Exception e) {
			// TODO: handle exception
			logger.error("operator failed!", e);
		}
		return (List<Map>) value;
	}

	public String getCompanyBaseIndexDataListFromCache(String companycode,
			String indexcode, Date stime, Date etime) {
		StringBuilder sb = new StringBuilder();
		Dictionary d = DictService.getInstance().getDataDictionary(indexcode);
		Date dstime = stime;
		Date detime = etime;
		while (dstime.compareTo(detime) <= 0) {
			try {
				Date acstime = IndexService.getInstance().formatTime(dstime, d,
						companycode);
				if (acstime != null) {
					IndexMessage im = SMsgFactory.getUMsg(companycode,
							indexcode, acstime);
					im.setNeedAccessExtIndexDb(false);
					im.setNeedAccessCompanyBaseIndexDb(false);
					Double v = getCompanyBaseIndexFromCache(im);
					long time = acstime.getTime();
					if (v != null) {
						sb.append(UnitUtil.formatByDefaultUnit(d, v));
						sb.append("^");
						sb.append(time);
						sb.append("|");
					}
				}

			} catch (Exception e) {
				logger.error("compute rule failed!", e);
			}
			dstime = StockUtil.getNextTimeV3(dstime, d.getInterval(),
					d.getTunit());
		}
		return sb.toString();
	}

	// 降序
	public List<Map<String, Object>> getCompanysIndexValueMapListFromCache(
			Dictionary gd, Date time, String companycodes) {
		final String indexcode = gd.getIndexCode();
		List<Map<String, Object>> ml = new ArrayList<Map<String, Object>>();
		String[] ca = companycodes.split(",");
		for (String ccode : ca) {
			time = IndexService.getInstance().formatTime(time, gd, ccode);
			Map<String, Object> m = new HashMap<String, Object>(2);
			IndexMessage im1 = SMsgFactory.getUMsg(ccode, gd.getIndexCode(),
					time);
			im1.setNeedAccessExtIndexDb(false);
			im1.setNeedAccessCompanyBaseIndexDb(false);
			im1.setNeedAccessExtRemoteCache(false);
			Double v = IndexValueAgent.getIndexValue(im1);
			m.put("company_code", ccode);
			m.put(gd.getIndexCode(), v);
			ml.add(m);

		}
		Collections.sort(ml, new Comparator<Map<String, Object>>() {

			public int compare(Map<String, Object> o1, Map<String, Object> o2) {
				Double d1 = (Double) o1.get(indexcode);
				Double d2 = (Double) o2.get(indexcode);
				if (d1 == null)
					return 1;
				if (d2 == null)
					return -1;
				if (d1 > d2)
					return -1;
				return (d2.compareTo(d1));
			}

		});
		return ml;
	}

	public String getIndexThcDataList(String companycode, String indexcode,
			Date stime, Date etime) {
		StringBuilder sb = new StringBuilder();
		String r = "$thc(${" + indexcode + "},0)";
		Dictionary d = DictService.getInstance().getDataDictionary(indexcode);
		while (stime.compareTo(etime) <= 0) {
			try {
				IndexMessage im = SMsgFactory.getUMsg(companycode, indexcode,
						stime);
				im.setNeedAccessExtRemoteCache(true);
				im.setNeedComput(true);
				im.setNeedUseExtDataCache(true);
				im.setNeedAccessExtIndexDb(false);
				im.setNeedAccessCompanyBaseIndexDb(false);
				Double v = CRuleService.getInstance().computeIndex(r, im,
						StockConstants.DEFINE_INDEX);
				long time = stime.getTime();
				if (v != null) {
					sb.append(UnitUtil.formatByUnit(v, 0));
					sb.append("^");
					sb.append(time);
					sb.append("|");
				}
			} catch (Exception e) {
				logger.error("compute rule failed!", e);
			}
			stime = StockUtil.getNextTime(stime, 3);
		}
		return sb.toString();
	}

	@SuppressWarnings("deprecation")
	public StringBuilder removeInvalidData(String s, Company c) {
		StringBuilder sb = new StringBuilder();
		if (StringUtil.isEmpty(s))
			return sb;

		String[] sa = s.split("~");
		for (String sai : sa) {
			String[] ita = sai.split("&");
			sb.append(ita[0]);
			sb.append("&");
			sb.append(ita[1]);
			sb.append("&");
			sb.append(ita[2]);
			sb.append("&");
			if (ita.length > 3) {
				String[] vl = ita[3].split("\\|");
				for (String vp : vl) {
					if (!StringUtil.isEmpty(vp)) {
						// 如果货币资金取不到，就认为此新股本期没有发财报
						IndexMessage im1 = SMsgFactory.getUMsg(
								c.getCompanyCode(), "1674",
								new Date(Long.valueOf(vp.split("\\^")[1])));
						im1.setNeedAccessExtIndexDb(false);
						im1.setNeedAccessCompanyBaseIndexDb(false);
						Double xv = IndexValueAgent.getIndexValue(im1);
						if (xv == null)
							continue;
						else {
							sb.append(vp);
							sb.append("|");
						}
					}
				}
			}
			sb.append("~");
		}
		return sb;
	}

	public StringBuilder removeInvalidDataV2(String s, Company c) {
		StringBuilder sb = new StringBuilder();
		if (StringUtil.isEmpty(s))
			return sb;

		String[] sa = s.split("~");
		for (String sai : sa) {
			String[] ita = sai.split("&");
			sb.append(ita[0]);
			sb.append("&");
			sb.append(ita[1]);
			sb.append("&");
			sb.append(ita[2]);
			sb.append("&");
			sb.append(ita[3]);
			sb.append("&");
			if (ita.length > 4) {
				String[] vl = ita[4].split("\\|");
				for (String vp : vl) {
					if (!StringUtil.isEmpty(vp)) {
						// 如果货币资金取不到，就认为此新股本期没有发财报
						IndexMessage im1 = SMsgFactory.getUMsg(
								c.getCompanyCode(), "1674",
								new Date(Long.valueOf(vp.split("\\^")[1])));
						im1.setNeedAccessExtIndexDb(false);
						im1.setNeedAccessCompanyBaseIndexDb(false);
						Double xv = IndexValueAgent.getIndexValue(im1);
						if (xv == null)
							continue;
						else {
							sb.append(vp);
							sb.append("|");
						}
					}
				}
			}
			sb.append("~");
		}
		return sb;
	}

	/*
	 * publicTime:上市时间
	 */
	@SuppressWarnings("deprecation")
	private boolean needRemovePoint(Date publicTime, Date curTime) {
		Calendar cp = Calendar.getInstance();
		cp.setTime(publicTime);
		Calendar cc = Calendar.getInstance();
		cc.setTime(curTime);
		if (cc.get(Calendar.YEAR) < cp.get(Calendar.YEAR)) {
			int m = cc.get(Calendar.MONTH) + 1;
			if (m != 12)
				return true;
		}
		if (cp.get(Calendar.YEAR) == cc.get(Calendar.YEAR)) {
			if (cp.get(Calendar.MONTH) != cc.get(Calendar.MONTH))
				return true;
		}
		return false;
	}

	/*
	 * 按指标类型，格式化时间
	 */
	public Date formatTime(Date time, int indextype, int per, String companycode) {

		// 如果是财务指标
		if (indextype == StockConstants.DEFINE_INDEX) {
			return formatJiDuTime(companycode, time);
		}
		// 如果是行情指标
		if (indextype == StockConstants.TRADE_TYPE) {
			return formatTimeByUnit(time, per, companycode);
		}
		return null;
	}

	private Date formatJiDuTime(String companycode, Date time) {
		Date ftime = StockUtil.formatJiDuTime(time);
		ftime = format2RightJiDuTime(companycode, ftime);
		return ftime;
	}

	/*
	 * 按指标类型，格式化时间
	 */
	public Date getNextTradeUtilEnd(Date time, Dictionary d,
			String companycode, int add) {
		Date ftime = time;
		if (companycode.indexOf(".") < 0) {
			Date ttime = StockUtil.getNextTimeV3(time, add, d.getTunit());
			ftime = formatTime_Tag(ttime, d, companycode);
		} else {
			// 判断是否为财务指标
			if (StockUtil.isCfIndex(d.getTctype())) {
				Date ttime = formatJiDuTime(companycode, time);
				ttime = StockUtil.getNextTimeV3(ttime, add, d.getTunit());
				ftime = formatJiDuTime(companycode, ttime);
			}
			if (StockUtil.isTradeIndex(d.getTctype())) {
				ftime = getNextTrade(time, d.getTunit(), companycode, add);
			}
		}
		if (ftime != null)
			ftime = formatTime(ftime, d, companycode);
		return ftime;
	}

	public Date getNextTrade(Date time, int per, String companycode, int add) {
		Date ntime = time;
		switch (per) {
		case Calendar.MONTH:
			ntime = getNMonthTradeTimeAfterCurDay(companycode, time, add);
			break;
		case Calendar.WEEK_OF_MONTH:
			ntime = getNWeekTradeTimeAfterCurDay(companycode, time, add);
			break;
		case Calendar.DAY_OF_MONTH:
			ntime = getNTradeTimeAfterCurDay(companycode, time, add);
			break;
		}
		return ntime;
	}

	public Date formatTime(Date time, Dictionary d, String companycode) {

		Date ftime = time;
		if (companycode.indexOf(".") < 0) {
			ftime = formatTime_Tag(time, d, companycode);
		} else {
			// 判断是否为财务指标
			if (StockUtil.isCfIndex(d.getTctype())) {
				ftime = formatJiDuTime(companycode, time);
			}
			if (StockUtil.isTradeIndex(d.getTctype())) {
				ftime = formatTimeByUnit(time, d.getTunit(), companycode);
			}
		}

		return ftime;
	}

	private Date format2RightJiDuTime(String companycode, Date ftime) {
		Date lp = StockUtil.getApproPeriod(new Date());
		Date crt = CompanyService.getInstance()
				.getLatestReportTime(companycode);
		if (ftime.compareTo(lp) > 0)
			ftime = lp;
		if (ftime.compareTo(crt) > 0)
			ftime = crt;
		return ftime;
	}


	
	
	
	private Date format2RightJiDuTime_plate(String tag, Date ftime) {
		Date crt = StockUtil.getCurIndustryJiDuTime();
		if (ftime.compareTo(crt) > 0)
			ftime = crt;
		return ftime;
	}

	public Date formatTimeByUnit(Date time, int per, String companycode) {
		Date ntime = time;
		switch (per) {
		case Calendar.MONTH:
			ntime = getMonthOfTrade(time, companycode);
			break;
		case Calendar.WEEK_OF_MONTH:
			ntime = getWeekOfTrade(time, companycode);
			break;
		case Calendar.DAY_OF_MONTH:
			ntime = getDayOfTrade(time, companycode);
			break;
		case Calendar.HOUR:
			ntime = getHourOfTrade(time, companycode);
			break;
		case Calendar.MINUTE:
			ntime = getMinuteOfTrade(time, companycode);
			break;
		}
		return ntime;
	}

	public Date formatTimeByUnitOfTag(Date time, int per, String tag) {
		Date ntime = time;
		switch (per) {
		case Calendar.MONTH:
			ntime = getMonthOfTrade_Tag(time, tag);
			break;
		case Calendar.WEEK_OF_MONTH:
			ntime = getWeekOfTrade_Tag(time, tag);
			break;
		case Calendar.DAY_OF_MONTH:
			ntime = getDayOfTrade_Tag(time, tag);
			break;
		case Calendar.HOUR:
			ntime = getHourOfTrade_Tag(time, tag);
			break;
		case Calendar.MINUTE:
			ntime = getMinuteOfTrade_Tag(time, tag);
			break;
		}
		return ntime;
	}

	/*
	 * 按指标类型，格式化时间
	 */
	public Date formatTime_Tag(Date time, Dictionary d, String tag) {

		Date ftime = time;
		// 判断是否为财务指标
		if (!StockUtil.isTradeIndex(d.getTctype())) {
			ftime = formatJiDuTimePlate(tag, time);
		}
		if (d.getType() == 6) {
			Cfirule cr = CRuleService.getInstance().getCfruleByCodeFromCache(
					d.getIndexCode());
			if (cr != null) {
				if (cr.getType() == StockConstants.TRADE_TYPE) {
					ftime = formatTimeByUnitOfTag(time, d.getTunit(), tag);
				} else {
					ftime = formatJiDuTimePlate(tag, time);
				}
			} else {
				if (d.getTimeUnit().equals("m") && d.getInterval() == 3) {
					ftime = formatJiDuTimePlate(tag, time);
				}
			}
		}
		if (d.getType() == 1) {
			ftime = formatTimeByUnitOfTag(time, d.getTunit(), tag);
		}
		return ftime;
	}

	private Date formatJiDuTimePlate(String tag, Date time) {
		Date ftime = StockUtil.formatJiDuTime(time);
		return format2RightJiDuTime_plate(tag, ftime);
	}

	public Date getMinuteOfTrade(Date time, String companycode) {
		Calendar c = Calendar.getInstance();
		c.setTime(time);

		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);
		if (!isTradeDate(c.getTime(), companycode))
			return null;
		return c.getTime();
	}

	public Date getHourOfTrade(Date time, String companycode) {
		Calendar c = Calendar.getInstance();
		c.setTime(time);

		c.clear(Calendar.MINUTE);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);
		if (!isTradeDate(c.getTime(), companycode))
			return null;
		return c.getTime();
	}

	public Date getDayOfTrade(Date time, String companycode) {
		Calendar c = Calendar.getInstance();
		c.setTime(time);

		c.set(Calendar.HOUR_OF_DAY, 0);
		c.clear(Calendar.MINUTE);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);
		if (!isTradeDate(c.getTime(), companycode))
			return null;
		return c.getTime();
	}

	public Date getWeekOfTrade(Date time, String companycode) {
		Date ret = null;
		Calendar c = Calendar.getInstance();
		c.setTime(time);
		c.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.clear(Calendar.MINUTE);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);
		Date wstime = DateUtil.getWeekStartTime(time);
		while (c.getTime().compareTo(wstime) >= 0) {
			// 如果不是交易日，就往前推一天
			if (!isTradeDate(c.getTime(), companycode)) {
				c.add(Calendar.DAY_OF_MONTH, -1);
			} else {
				ret = c.getTime();
				break;
			}
		}
		Date dstime = DateUtil.getDayStartTime(new Date());
		if (ret != null && ret.compareTo(dstime) > 0) {
			ret = dstime;
		}
		return ret;
	}

	/**
	 * 取本月的最后的个交易日时间
	 * 
	 * @param time
	 * @return
	 */
	public Date getMonthOfTrade(Date date, String companycode) {
		Date ret = null;
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.clear(Calendar.MINUTE);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);
		// 是否为周未0:周日，6:周六
		if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			c.set(Calendar.DAY_OF_MONTH,
					c.getActualMaximum(Calendar.DAY_OF_MONTH) - 2);
		}
		if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
			c.set(Calendar.DAY_OF_MONTH,
					c.getActualMaximum(Calendar.DAY_OF_MONTH) - 1);
		}

		Date mstime = DateUtil.getMonthStartTime(date);
		while (c.getTime().compareTo(mstime) >= 0) {
			// 如果不是交易日，就往前推一天
			if (!isTradeDate(c.getTime(), companycode)) {
				c.add(Calendar.DAY_OF_MONTH, -1);
			} else {
				ret = c.getTime();
				break;
			}
		}
		Date dstime = DateUtil.getDayStartTime(new Date());
		if (ret != null && ret.compareTo(dstime) > 0) {
			ret = dstime;
		}
		return ret;
	}

	/**
	 * 判断是否有交易数据
	 * 
	 * @param date
	 * @return
	 */
	public boolean isTradeDate(Date time, String companycode) {
		if (isCurDay(time))
			return true;
		return TradeBitMapService.getInstance().isTradeDay(companycode, time);
		// time = DateUtil.getDayStartTime(time);
		// IndexMessage im1 = SMsgFactory.getUMsg(companycode,
		// StockConstants.INDEX_CODE_TRADE_S, time);
		// im1.setNeedAccessExtIndexDb(false);
		// im1.setNeedAccessCompanyBaseIndexDb(false);
		// Double xv = IndexValueAgent.getIndexValue(im1);
		// if (xv == null || xv == 0)
		// {
		// if(isInTradeDataMap(im1))
		// return true;
		// else
		// return false;
		// }
		// return true;
	}
	
	/**
	 * 判断是否有交易数据
	 * 
	 * @param date
	 * @return
	 */
	public boolean isTradeDate_2(Date time, String companycode) {
		return TradeBitMapService.getInstance().isTradeDay(companycode, time);
	}

	public boolean isCurDay(Date time) {

		return DateUtil.isSameDay(time.getTime(), System.currentTimeMillis());
	}

	private boolean isInTradeDataMap(IndexMessage im1) {
		Set<Company> cm = LCEnter.getInstance().get(im1.getTime().getTime(),
				SCache.CACHE_NAME_tradedatamapcache);
		if (cm != null) {
			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(
					im1.getCompanyCode());
			if (cm.contains(c))
				return true;
		}
		return false;
	}

	public boolean isPlateTradeDate(Date time, String tag) {
		List<Company> cl = CompanyService.getInstance()
				.getCompanyListByTagFromCache(tag);
		if (cl != null) {
			for (Company c : cl) {
				if (isTradeDate(time, c.getCompanyCode()))
					return true;
			}
		}
		return false;
	}

	private Date getMinuteOfTrade_Tag(Date time, String tag) {
		Calendar c = Calendar.getInstance();
		c.setTime(time);

		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);
		if (!isPlateTradeDate(c.getTime(), tag))
			return null;
		return c.getTime();
	}

	private Date getHourOfTrade_Tag(Date time, String tag) {
		Calendar c = Calendar.getInstance();
		c.setTime(time);

		c.clear(Calendar.MINUTE);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);
		if (!isPlateTradeDate(c.getTime(), tag))
			return null;
		return c.getTime();
	}

	private Date getDayOfTrade_Tag(Date time, String tag) {
		Calendar c = Calendar.getInstance();
		c.setTime(time);

		c.set(Calendar.HOUR_OF_DAY, 0);
		c.clear(Calendar.MINUTE);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);
		if (!isPlateTradeDate(c.getTime(), tag))
			return null;
		return c.getTime();
	}

	private Date getWeekOfTrade_Tag(Date time, String tag) {
		Calendar c = Calendar.getInstance();
		c.setTime(time);
		Date ret = null;
		c.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.clear(Calendar.MINUTE);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);
		Date wstime = DateUtil.getWeekStartTime(time);
		while (c.getTime().compareTo(wstime) >= 0) {
			// 如果不是交易日，就往前推一天
			if (!isPlateTradeDate(c.getTime(), tag)) {
				c.add(Calendar.DAY_OF_MONTH, -1);
			} else {
				ret = c.getTime();
				break;
			}
		}
		return ret;
	}

	/**
	 * 取本月的最后的个交易日时间
	 * 
	 * @param time
	 * @return
	 */
	private Date getMonthOfTrade_Tag(Date date, String tag) {
		Date ret = null;
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.clear(Calendar.MINUTE);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);
		// 是否为周未0:周日，6:周六
		if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			c.set(Calendar.DAY_OF_MONTH,
					c.getActualMaximum(Calendar.DAY_OF_MONTH) - 2);
		}
		if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
			c.set(Calendar.DAY_OF_MONTH,
					c.getActualMaximum(Calendar.DAY_OF_MONTH) - 1);
		}
		Date mstime = DateUtil.getMonthStartTime(date);
		while (c.getTime().compareTo(mstime) >= 0) {
			// 如果不是交易日，就往前推一天
			if (!isPlateTradeDate(c.getTime(), tag)) {
				c.add(Calendar.DAY_OF_MONTH, -1);
			} else {
				ret = c.getTime();
				break;
			}
		}
		return ret;
	}

	/**
	 * 从当前时间往后推N个交易日 type:0:向后推(加)，-1向前推(减)
	 * 
	 * @param companycode
	 * @param stime
	 * @param n
	 * @return
	 */
	public Date getNTradeTimeAfterCurDay(String companycode, Date stime, int add) {
		int n = Math.abs(add);
		n = n + 1;
		Calendar c = Calendar.getInstance();
		c.setTime(stime);

		int count = 0;
		Date etime = null;
		// 取上市时间
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				companycode, StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return null;
		Date maxtime = new Date();
		while (count <= n && c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			if (IndexService.getInstance()
					.isTradeDate(c.getTime(), companycode)) {
				count++;
			}
			if (count == n) {
				etime = c.getTime();
				break;
			}
			if (add > 0)
				c.add(Calendar.DAY_OF_MONTH, 1);
			else
				c.add(Calendar.DAY_OF_MONTH, -1);

		}
		if (etime == null) {
			if (add > 0)
				etime = new Date();
			else
				etime = StockUtil.getNextTimeV3(mintime, -1,
						Calendar.DAY_OF_MONTH);
		}
		return etime;
	}

	public Date getNWeekTradeTimeAfterCurDay(String companycode, Date stime,
			int add) {
		int n = Math.abs(add);
		n = n + 1;
		Calendar c = Calendar.getInstance();
		c.setTime(stime);

		int count = 0;
		Date etime = null;
		// 取上市时间
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				companycode, StockConstants.INDEX_CODE_TRADE_S_W);
		if (mintime == null)
			return null;
		Date maxtime = new Date();
		while (count <= n && c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Date wtdate = IndexService.getInstance().getWeekOfTrade(
					DateUtil.getWeekEndTime(c.getTime()), companycode);
			if (wtdate != null) {
				count++;
			}
			if (count == n) {
				etime = wtdate;
				break;
			}
			if (add > 0)
				c.add(Calendar.WEEK_OF_MONTH, 1);
			else
				c.add(Calendar.WEEK_OF_MONTH, -1);

		}
		if (etime != null)
			etime = IndexService.getInstance().getWeekOfTrade(etime,
					companycode);
		if (etime == null) {
			if (add > 0)
				etime = new Date();
			else
				etime = StockUtil.getNextTimeV3(mintime, -1,
						Calendar.WEEK_OF_MONTH);
		}
		return etime;
	}

	public Date getNMonthTradeTimeAfterCurDay(String companycode, Date stime,
			int add) {
		int n = Math.abs(add);
		n = n + 1;
		Calendar c = Calendar.getInstance();
		c.setTime(stime);

		int count = 0;
		Date etime = null;
		// 取上市时间
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
				companycode, StockConstants.INDEX_CODE_TRADE_S_M);
		if (mintime == null)
			return null;
		Date maxtime = new Date();
		while (count <= n && c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			Date mtdate = IndexService.getInstance().getMonthOfTrade(
					DateUtil.getMonthEndTime(c.getTime()), companycode);
			if (mtdate != null) {
				count++;
			}
			if (count == n) {
				etime = mtdate;
				break;
			}
			if (add > 0)
				c.add(Calendar.MONTH, 1);
			else
				c.add(Calendar.MONTH, -1);

		}
		if (etime != null)
			etime = IndexService.getInstance().getMonthOfTrade(etime,
					companycode);
		if (etime == null) {
			if (add > 0)
				etime = new Date();
			else
				etime = StockUtil.getNextTimeV3(mintime, -1, Calendar.MONTH);
		}
		return etime;
	}

	/**
	 * 从当前时间往后推N个交易日type:0:向前推，-1向后推
	 * 
	 * @param companycode
	 * @param stime
	 * @param n
	 * @return
	 */
	public Date getNTradeTimeAfterCurDayPlate(String tag, Date stime, int add) {
		int n = Math.abs(add);
		n = n + 1;
		Calendar c = DateUtil.getFormatYYYYMMDDCalendar(stime);

		int count = 0;
		Date etime = null;
		Date mintime = USubjectService.getInstance().getTradeIndexMinTime(tag,
				StockConstants.INDEX_CODE_TRADE_S);
		if (mintime == null)
			return null;
		Date maxtime = new Date();
		while (count <= n && c.getTime().compareTo(mintime) >= 0
				&& c.getTime().compareTo(maxtime) <= 0) {
			if (IndexService.getInstance().isPlateTradeDate(c.getTime(), tag)) {
				count++;
			}
			if (count == n) {
				etime = c.getTime();
				break;
			}
			if (add > 0)
				c.add(Calendar.DAY_OF_MONTH, 1);
			else
				c.add(Calendar.DAY_OF_MONTH, -1);

		}
		if (etime == null) {
			if (add > 0)
				etime = new Date();
			else
				etime = StockUtil.getNextTimeV3(mintime, -1,
						Calendar.DAY_OF_MONTH);
		}
		return etime;
	}

	public Date getNTradeTimeAfterCurDay(IndexMessage req, Date stime, int add) {
		if (IndexService.isCompanyMsg(req)) {
			return getNTradeTimeAfterCurDay(req.getUidentify(), stime, add);
		} else {
			return getNTradeTimeAfterCurDayPlate(req.getUidentify(), stime, add);
		}
	}

	/**
	 * 判断是不是离季报期最近的一个交易日
	 * 
	 * @param time
	 * @return
	 */
	public boolean isJiduTradeDate(IndexMessage req) {
		Date jtradeDate = getJiBaoTradeDate(req);
		if (jtradeDate == null)
			return false;
		return req.getTime().compareTo(jtradeDate) == 0;
	}

	/**
	 * 离季报期最近的一个交易日
	 * 
	 * @param time
	 * @return
	 */
	private Date getJiBaoTradeDate(IndexMessage req) {
		Date curJd = DateUtil.getCurApproPeriod(req.getTime());
		Date jdtrade = getNTradeTimeAfterCurDay(req, curJd, -1);
		return jdtrade;
	}

	public String getTradeDataByType(int type, String companycode, Date stime,
			Date etime) {
		StringBuilder sb = null;
		switch (type) {
		case 0:
			sb = getDayIndexGroupDatas(companycode, stime, etime);
			break;
		case 1:
			sb = getWeekIndexGroupDatas(companycode, stime, etime);
			break;
		case 2:
			sb = getMonthIndexGroupDatas(companycode, stime, etime);
			break;
		}

		return sb.toString();
	}

	public Object getTradeDataByType(int type, String companycode, Date time,
			Date etime,int ftype, int limit) {
		StringBuilder sb = null;
		switch (type) {
		case 0:
			sb = getDayIndexGroupDatasByType(companycode, ftype, time, etime, limit);
			break;
		case 1:
			sb = getWeekIndexGroupDatasByType(companycode, ftype, time, etime,limit);
			break;
		case 2:
			sb = getMonthIndexGroupDatasByType(companycode, ftype, time, etime, limit);
			break;
		}
		return sb.toString();
	}

	private StringBuilder getMonthIndexGroupDatas(String companycode,
			Date stime, Date etime) {
		StringBuilder sb = new StringBuilder();
		etime = DateUtil.getMonthStartTime(etime);
		while (stime.compareTo(etime) < 0) {
			try {
				IndexMessage im = SMsgFactory.getUMsg(companycode);
				im.setTime(stime);
				im.setNeedAccessCompanyBaseIndexDb(false);
				im.setNeedAccessExtIndexDb(false);
				im.setNeedComput(false);
				im.setNeedRealComputeIndustryValue(false);

				String ret = getMonthGroupIndex(im);
				sb.append(ret);
			} catch (Exception e) {
				logger.error("build data failed!", e);
			}
			stime = StockUtil.getNextTimeV3(stime, 1, Calendar.MONTH);

		}

		return sb;
	}

	private StringBuilder getMonthIndexGroupDatasByType(String companycode,
			int ftype, Date stime, Date etime, int limit) {
		StringBuilder sb = new StringBuilder();
		int count = 0;
		int maxCount = 0;
		if(etime==null){
			etime = DateUtil.getMonthStartTime(new Date());
			Long time = TradeBitMapService.getInstance().getLatestTradeTime(companycode);
			if(time != null && time>0){
				etime = new Date(time);
			}
			if(DateUtil.getDayStartTime(stime).compareTo(DateUtil.getDayStartTime(new Date())) == 0 && ftype==0){
				/*Dictionary d = DictService.getInstance().getDataDictionaryFromCache(StockConstants.INDEX_CODE_TRADE_S_M);
				stime = IndexService.getInstance().formatTime(stime,d, companycode);*/
				/*long time = TradeBitMapService.getInstance().getLatestTradeTime(companycode);
				actime = new Date(time);*/
				stime = etime;
			}
			if(stime==null){
				stime = new Date();
			}
			boolean flag = (stime.compareTo(etime)<=0 || ftype==0);
			while (count < limit && maxCount < (limit * 5) && flag) {
				try {
					IndexMessage im = SMsgFactory.getUMsg(companycode);
					im.setTime(stime);
					im.setNeedAccessCompanyBaseIndexDb(false);
					im.setNeedAccessExtIndexDb(false);
					im.setNeedComput(false);
					im.setNeedRealComputeIndustryValue(false);
					String ret = getMonthGroupIndex(im);
					if (ret.length() > 0) {
						count++;
						sb.append(ret);
					}
					maxCount++;
				} catch (Exception e) {
					logger.error("build data failed!", e);
				}
				if (ftype == 0) {
					stime = StockUtil.getNextTimeV3(stime, -1,
							Calendar.MONTH);
				} else {
					stime = StockUtil
							.getNextTimeV3(stime, 1, Calendar.MONTH);
				}
				flag = (stime.compareTo(etime)<=0 || ftype==0);
			}
		}else{
			etime = DateUtil.getMonthStartTime(etime);
			while (stime.compareTo(etime) <= 0) {
				try {
					IndexMessage im = SMsgFactory.getUMsg(companycode);
					im.setTime(stime);
					im.setNeedAccessCompanyBaseIndexDb(false);
					im.setNeedAccessExtIndexDb(false);
					im.setNeedComput(false);
					im.setNeedRealComputeIndustryValue(false);
					String ret = getMonthGroupIndex(im);
					sb.append(ret);
				} catch (Exception e) {
					logger.error("build data failed!", e);
				}
				stime = StockUtil.getNextTimeV3(stime, 1, Calendar.MONTH);
			}
		}

		return sb;
	}

	private StringBuilder getWeekIndexGroupDatas(String companycode) {
		StringBuilder sb = new StringBuilder();
		//long lastTradeDate = TradeCenter.getInstance().getLastTradeDate(new Date().getTime(), companycode);
		//返回最近一个交易日时间
		//Date time = DateUtil.getWeekEndTime(new Date(),lastTradeDate);
		Date time = DateUtil.getWeekEndTime(new Date());
		try {
			IndexMessage im = SMsgFactory.getUMsg(companycode);
			im.setTime(time);
			im.setNeedAccessCompanyBaseIndexDb(false);
			im.setNeedAccessExtIndexDb(false);
			im.setNeedComput(false);
			im.setNeedRealComputeIndustryValue(false);
			String ret = getWeekGroupIndexForReal(im);
			if(ret.length()>0 && !TradeService.getInstance().isTradeDay(companycode)){//非交易日需要把时间替换成交易日
				StringBuilder bs = new StringBuilder();
				long stime = TradeCenter.getInstance().getLastTradeDate(new Date().getTime(), companycode);
				if(stime>0){
					int sindex = ret.indexOf("^");
					bs.append(stime);
					bs.append(ret.substring(sindex));
					ret = bs.toString();
				}
			}
			sb.append(ret);
		} catch (Exception e) {
			logger.error("build data failed!", e);
		}

		return sb;
	}

	private String getWeekGroupIndex(IndexMessage im) {
		//boolean isToday = false;
		StringBuilder sb = new StringBuilder();
		Dictionary d = DictService.getInstance().getDataDictionaryFromCache(StockConstants.INDEX_CODE_TRADE_S_W);
		Date actime = IndexService.getInstance().formatTime(im.getTime(),d,  im.getCompanyCode());
		if (actime != null) {
			// 时间，k:开盘价，s:收盘价,zg:最高价，zd:最低价，cjl:成交量，cje:成交额，zdf:涨跌幅，sd:升跌，hsl:换手率
			HWeekService hds = HWeekService.getInstance();
			Double dv = hds.getWeekIndex(im, ComputeUtil.TRADE_K);// 开盘价
			if (dv != null && dv != 0) {
				/*if(DateUtil.getDayStartTime(im.getTime()).compareTo(DateUtil.getDayStartTime(new Date())) == 0){
					long time = TradeBitMapService.getInstance().getLatestTradeTime(im.getCompanyCode());
					actime = new Date(time);
					isToday = true;
				}*/
				sb.append(actime.getTime());
				sb.append("^");

				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_S);// 收盘价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				// dv = hds.getDayIndex(im,
				// ComputeUtil.TRADE_ZS);//昨日收盘价
				// sb.append(dv);
				// sb.append("^");
				// 加上需要的指标
				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_ZG);// 最高价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_ZD);// 最低价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_CJL);// 成交量
				BigDecimal b = new BigDecimal(dv);
				long wCJL = b.longValue();
				sb.append(wCJL);
				sb.append("^");

				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_CJE);// 成交额
				BigDecimal b1 = new BigDecimal(dv);
				sb.append(b1.longValue());
				sb.append("^");

				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_ZDF);// 涨跌幅
				sb.append(SMathUtil.getDouble(dv * 100,3));
				sb.append("^");

				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_SD);// 升跌
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_HSL);// 换手率
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_ZS);// 昨日收盘价
				if (dv == null)
					dv = 0.0;
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_5AVG);// 5日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_10AVG);// 10日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_20AVG);// 20日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_30AVG);// 30日均线
				sb.append(SMathUtil.getDouble(dv,3));
				
				/*if(isToday) {
					sb.append("^");
					HDayService ds = HDayService.getInstance();
					dv = ds.getDayIndex(im, ComputeUtil.TRADE_CJL);
					BigDecimal bd = new BigDecimal(dv);
					long dCJL = bd.longValue();
					long cjl = wCJL - dCJL<0?0:wCJL - dCJL;
					sb.append(cjl);
				}*/
				sb.append("~");
			}
		}
		return sb.toString();
	}
	private String getWeekGroupIndexForReal(IndexMessage im) {
		StringBuilder sb = new StringBuilder();
		Date actime = DateUtil.getDayStartTime(im.getTime());
		if (actime != null) {
			// zs:昨日收盘价，k:开盘价，s:收盘价,zg:最高价，zd:最低价，cjl:成交量，cje:成交额，zdf:涨跌幅，sd:升跌，hsl:换手率
			HWeekService hds = HWeekService.getInstance();
			Double dv = hds.getWeekIndex(im, ComputeUtil.TRADE_K);// 开盘价
			if (dv != null && dv != 0) {
				sb.append(actime.getTime());
				sb.append("^");
				
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_S);// 收盘价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				// dv = hds.getDayIndex(im,
				// ComputeUtil.TRADE_ZS);//昨日收盘价
				// sb.append(dv);
				// sb.append("^");
				// 加上需要的指标
				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_ZG);// 最高价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_ZD);// 最低价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_CJL);// 成交量
				BigDecimal b = new BigDecimal(dv);
				long wCJL = b.longValue();
				sb.append(wCJL);
				sb.append("^");
				
				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_CJE);// 成交额
				BigDecimal b1 = new BigDecimal(dv);
				sb.append(b1.longValue());
				sb.append("^");
				
				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_ZDF);// 涨跌幅
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_SD);// 升跌
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_HSL);// 换手率
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_ZS);// 昨日收盘价
				if (dv == null)
					dv = 0.0;
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_5AVG);// 5日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_10AVG);// 10日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_20AVG);// 20日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getWeekIndex(im, ComputeUtil.TRADE_30AVG);// 30日均线
				sb.append(SMathUtil.getDouble(dv,3));
				
				if(DateUtil.getDayStartTime(im.getTime()).compareTo(DateUtil.getDayStartTime(new Date())) == 0) {
					sb.append("^");
					HDayService ds = HDayService.getInstance();
					dv = ds.getDayIndex(im, ComputeUtil.TRADE_CJL);
					BigDecimal bd = new BigDecimal(dv);
					long dCJL = bd.longValue();
					long cjl = wCJL - dCJL<0?0:wCJL - dCJL;
					sb.append(cjl);
				}
				sb.append("~");
			}
		}
		return sb.toString();
	}

	private StringBuilder getDayIndexGroupDatas(String companycode) {
		StringBuilder sb = new StringBuilder();
		//最近一个交易日时间
		//long lastTradeDate = TradeCenter.getInstance().getLastTradeDate(new Date().getTime(), companycode);
		Date time  = DateUtil.getDayStartTime(new Date());
		try {
			IndexMessage im = SMsgFactory.getUMsg(companycode);
			im.setTime(time);
			im.setNeedAccessCompanyBaseIndexDb(false);
			im.setNeedAccessExtIndexDb(false);
			im.setNeedComput(false);
			im.setNeedRealComputeIndustryValue(false);
			String ret = getDayGroupIndex(im);
			sb.append(ret);
		} catch (Exception e) {
			logger.error("build data failed!", e);
		}

		return sb;
	}

	private String getDayGroupIndex(IndexMessage im) {
		StringBuilder sb = new StringBuilder();
		Date actime = DateUtil.getDayStartTime(im.getTime());
		if (actime != null) {
			// zs:昨日收盘价，k:开盘价，s:收盘价,zg:最高价，zd:最低价，cjl:成交量，cje:成交额，zdf:涨跌幅，sd:升跌，hsl:换手率
			HDayService hds = HDayService.getInstance();
			Double dv = hds.getDayIndex(im, ComputeUtil.TRADE_K);// 开盘价
			if (dv != null && dv != 0) {
				sb.append(actime.getTime());
				sb.append("^");

				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getDayIndex(im, ComputeUtil.TRADE_S);// 收盘价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				// 加上需要的指标
				dv = hds.getDayIndex(im, ComputeUtil.TRADE_ZG);// 最高价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getDayIndex(im, ComputeUtil.TRADE_ZD);// 最低价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getDayIndex(im, ComputeUtil.TRADE_CJL);// 成交量
				BigDecimal b = new BigDecimal(dv);
				sb.append(b.longValue());
				sb.append("^");

				dv = hds.getDayIndex(im, ComputeUtil.TRADE_CJE);// 成交额
				BigDecimal b1 = new BigDecimal(dv);
				sb.append(b1.longValue());
				sb.append("^");

				dv = hds.getDayIndex(im, ComputeUtil.TRADE_ZDF);// 涨跌幅
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getDayIndex(im, ComputeUtil.TRADE_SD);// 升跌
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getDayIndex(im, ComputeUtil.TRADE_HSL);// 换手率
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getDayIndex(im, ComputeUtil.TRADE_ZS);// 昨日收盘价
				if (dv == null)
					dv = 0.0;
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getDayIndex(im, ComputeUtil.TRADE_5AVG);// 5日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getDayIndex(im, ComputeUtil.TRADE_10AVG);// 10日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getDayIndex(im, ComputeUtil.TRADE_20AVG);// 20日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getDayIndex(im, ComputeUtil.TRADE_30AVG);// 30日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getDayIndex(im, ComputeUtil.TRADE_60AVG);// 60日均线
				sb.append(SMathUtil.getDouble(dv,3));

				sb.append("~");
			}
		}
		return sb.toString();
	}

	public String getRealtimeTradeDataByType(int type, String companycode) {
		StringBuilder sb = null;
		switch (type) {
		case 0:
			sb = getDayIndexGroupDatas(companycode);
			break;
		case 1:
			sb = getWeekIndexGroupDatas(companycode);
			break;
		case 2:
			sb = getMonthIndexGroupDatas(companycode);
			break;
		}

		return sb.toString();
	}

	private StringBuilder getMonthIndexGroupDatas(String companycode) {
		StringBuilder sb = new StringBuilder();
		//long lastTradeDate = TradeCenter.getInstance().getLastTradeDate(new Date().getTime(), companycode);
		//Date time = DateUtil.getMonthEndTime(new Date(),lastTradeDate);
		//Date time =new Date(lastTradeDate);
		Date time = DateUtil.getMonthEndTime(new Date());
		try {
			IndexMessage im = SMsgFactory.getUMsg(companycode);
			im.setTime(time);
			im.setNeedAccessCompanyBaseIndexDb(false);
			im.setNeedAccessExtIndexDb(false);
			im.setNeedComput(false);
			im.setNeedRealComputeIndustryValue(false);
			String ret = getMonthGroupIndexForReal(im);
			if(ret.length()>0 && !TradeService.getInstance().isTradeDay(companycode)){//非交易日需要把时间替换成最近一个交易日
				StringBuilder bs = new StringBuilder();
				long stime = TradeCenter.getInstance().getLastTradeDate(new Date().getTime(), companycode);
				if(stime>0){
					int sindex = ret.indexOf("^");
					bs.append(stime);
					bs.append(ret.substring(sindex));
					ret = bs.toString();
				}
			}
			sb.append(ret);
		} catch (Exception e) {
			logger.error("build data failed!", e);
		}

		return sb;
	}

	private String getMonthGroupIndex(IndexMessage im) {
		//boolean isToday = false;
		StringBuilder sb = new StringBuilder();
		Dictionary d = DictService.getInstance().getDataDictionaryFromCache(StockConstants.INDEX_CODE_TRADE_S_M);
		Date actime = IndexService.getInstance().formatTime(im.getTime(),d,  im.getCompanyCode());
		if (actime != null) {
			// 时间，k:开盘价，s:收盘价,zg:最高价，zd:最低价，cjl:成交量，cje:成交额，zdf:涨跌幅，sd:升跌，hsl:换手率
			HMonthService hds = HMonthService.getInstance();
			Double dv = hds.getMonthIndex(im, ComputeUtil.TRADE_K);// 开盘价
			if(actime.compareTo(DateUtil.getDayStartTime(new Date())) == 0){
				dv = 0.0;
			}
			if (dv != null && dv != 0) {
				/*if(DateUtil.getDayStartTime(im.getTime()).compareTo(DateUtil.getDayStartTime(new Date())) == 0){
					long time = TradeBitMapService.getInstance().getLatestTradeTime(im.getCompanyCode());
					actime = new Date(time);
					isToday = true;
				}*/
				sb.append(actime.getTime());
				sb.append("^");

				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_S);// 收盘价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_ZG);// 最高价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_ZD);// 最低价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_CJL);// 成交量
				if (dv == null)
					dv = 0.0;
				BigDecimal b = new BigDecimal(dv);
				long mCJL = b.longValue();
				sb.append(mCJL);
				sb.append("^");

				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_CJE);// 成交额
				if (dv == null)
					dv = 0.0;
				BigDecimal b1 = new BigDecimal(dv);
				sb.append(b1.longValue());
				sb.append("^");

				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_ZDF);// 涨跌幅
				if (dv == null)
					dv = 0.0;
				sb.append(SMathUtil.getDouble(dv*100,3));
				sb.append("^");

				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_SD);// 升跌
				if (dv == null)
					dv = 0.0;
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_HSL);// 换手率
				if (dv == null)
					dv = 0.0;
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_ZS);// 昨日收盘价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_5AVG);// 5日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_10AVG);// 10日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_20AVG);// 20日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");

				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_30AVG);// 30日均线
				sb.append(SMathUtil.getDouble(dv,3));

				/*if(isToday) {
					sb.append("^");
					HDayService ds = HDayService.getInstance();
					dv = ds.getDayIndex(im, ComputeUtil.TRADE_CJL);
					BigDecimal bd = new BigDecimal(dv);
					long dCJL = bd.longValue();
					long cjl = mCJL - dCJL<0?0:mCJL - dCJL;
					sb.append(cjl);
				}*/
				sb.append("~");
			}
		}
		return sb.toString();
	}
	
	//返回最后一个交易日
	private Date getlastTradeDate(Date date, String companycode) {
		Date ret = null;
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.clear(Calendar.MINUTE);
		c.clear(Calendar.SECOND);
		c.clear(Calendar.MILLISECOND);
		Date mstime = DateUtil.getMonthStartTime(date);
		while (c.getTime().compareTo(mstime) >= 0) {
			// 如果不是交易日，就往前推一天
			if (!isTradeDate_2(c.getTime(), companycode)) {
				c.add(Calendar.DAY_OF_MONTH, -1);
			} else {
				ret = c.getTime();
				break;
			}
		}
		Date dstime = DateUtil.getDayStartTime(new Date());
		if (ret != null && ret.compareTo(dstime) > 0) {
			ret = dstime;
		}
		return ret;
	}
	
	private String getMonthGroupIndexForReal(IndexMessage im) {
		StringBuilder sb = new StringBuilder();
		Date actime = DateUtil.getDayStartTime(im.getTime());
		if (actime != null) {
			// zs:昨日收盘价，k:开盘价，s:收盘价,zg:最高价，zd:最低价，cjl:成交量，cje:成交额，zdf:涨跌幅，sd:升跌，hsl:换手率
			HMonthService hds = HMonthService.getInstance();
			Double dv = hds.getMonthIndex(im, ComputeUtil.TRADE_K);// 开盘价
			if (dv != null && dv != 0) {
				sb.append(actime.getTime());
				sb.append("^");
				
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_S);// 收盘价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_ZG);// 最高价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_ZD);// 最低价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_CJL);// 成交量
				if (dv == null)
					dv = 0.0;
				BigDecimal b = new BigDecimal(dv);
				long mCJL = b.longValue();
				sb.append(b.longValue());
				sb.append("^");
				
				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_CJE);// 成交额
				if (dv == null)
					dv = 0.0;
				BigDecimal b1 = new BigDecimal(dv);
				sb.append(b1.longValue());
				sb.append("^");
				
				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_ZDF);// 涨跌幅
				if (dv == null)
					dv = 0.0;
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_SD);// 升跌
				if (dv == null)
					dv = 0.0;
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_HSL);// 换手率
				if (dv == null)
					dv = 0.0;
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_ZS);// 昨日收盘价
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_5AVG);// 5日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_10AVG);// 10日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_20AVG);// 20日均线
				sb.append(SMathUtil.getDouble(dv,3));
				sb.append("^");
				
				dv = hds.getMonthIndex(im, ComputeUtil.TRADE_30AVG);// 30日均线
				sb.append(SMathUtil.getDouble(dv,3));
				
				if(DateUtil.getDayStartTime(im.getTime()).compareTo(DateUtil.getDayStartTime(new Date())) == 0) {
					sb.append("^");
					HDayService ds = HDayService.getInstance();
					dv = ds.getDayIndex(im, ComputeUtil.TRADE_CJL);
					BigDecimal bd = new BigDecimal(dv);
					long dCJL = bd.longValue();
					long cjl = mCJL - dCJL<0?0:mCJL - dCJL;
					sb.append(cjl);
				}
				sb.append("~");
			}
		}
		return sb.toString();
	}

	private StringBuilder getWeekIndexGroupDatas(String companycode,
			Date stime, Date etime) {
		StringBuilder sb = new StringBuilder();
		etime = DateUtil.getWeekStartTime(etime);
		while (stime.compareTo(etime) <= 0) {
			try {
				IndexMessage im = SMsgFactory.getUMsg(companycode);
				im.setTime(stime);
				im.setNeedAccessCompanyBaseIndexDb(false);
				im.setNeedAccessExtIndexDb(false);
				im.setNeedComput(false);
				im.setNeedRealComputeIndustryValue(false);

				String ret = getWeekGroupIndex(im);
				sb.append(ret);
			} catch (Exception e) {
				logger.error("build data failed!", e);
			}
			stime = StockUtil.getNextTimeV3(stime, 1, Calendar.WEEK_OF_MONTH);
		}

		return sb;
	}

	private StringBuilder getWeekIndexGroupDatasByType(String companycode,
			int ftype, Date stime, Date etime, int limit) {
		StringBuilder sb = new StringBuilder();
		int count = 0;
		int maxCount = 0;
		if(etime==null){
			etime = DateUtil.getWeekStartTime(new Date());
			Long time = TradeBitMapService.getInstance().getLatestTradeTime(companycode);
			if(time != null && time>0){
				etime = new Date(time);
			}
			if(DateUtil.getDayStartTime(stime).compareTo(DateUtil.getDayStartTime(new Date())) == 0 && ftype==0){
				stime = etime;
			}
			if(stime==null){
				stime = new Date();
			}
			boolean flag = (stime.compareTo(etime)<=0 || ftype==0);
			while (count < limit && maxCount < (limit * 5) && flag) {
				try {
					IndexMessage im = SMsgFactory.getUMsg(companycode);
					im.setTime(stime);
					im.setNeedAccessCompanyBaseIndexDb(false);
					im.setNeedAccessExtIndexDb(false);
					im.setNeedComput(false);
					im.setNeedRealComputeIndustryValue(false);
					String ret = getWeekGroupIndex(im);
					if (ret.length() > 0) {
						count++;
						sb.append(ret);
					}
					maxCount++;
				} catch (Exception e) {
					logger.error("build data failed!", e);
				}
				if (ftype == 0) {
					stime = StockUtil.getNextTimeV3(stime, -1,
							Calendar.WEEK_OF_MONTH);
				} else {
					stime = StockUtil
							.getNextTimeV3(stime, 1, Calendar.WEEK_OF_MONTH);
				}
				flag = (stime.compareTo(etime)<=0 || ftype==0);
			}
		}else{
			etime = DateUtil.getWeekStartTime(etime);
			while (stime.compareTo(etime) <= 0) {
				try {
					IndexMessage im = SMsgFactory.getUMsg(companycode);
					im.setTime(stime);
					im.setNeedAccessCompanyBaseIndexDb(false);
					im.setNeedAccessExtIndexDb(false);
					im.setNeedComput(false);
					im.setNeedRealComputeIndustryValue(false);
					String ret = getWeekGroupIndex(im);
					sb.append(ret);
				} catch (Exception e) {
					logger.error("build data failed!", e);
				}
				stime = StockUtil.getNextTimeV3(stime, 1, Calendar.WEEK_OF_MONTH);
			}
		}
		return sb;
	}

	private StringBuilder getDayIndexGroupDatas(String companycode, Date stime,
			Date etime) {
		StringBuilder sb = new StringBuilder();
		etime = DateUtil.getDayStartTime(etime);
		while (stime.compareTo(etime) <= 0) {
			try {
				IndexMessage im = SMsgFactory.getUMsg(companycode);
				im.setTime(stime);
				im.setNeedAccessCompanyBaseIndexDb(false);
				im.setNeedAccessExtIndexDb(false);
				im.setNeedComput(false);
				im.setNeedRealComputeIndustryValue(false);

				String ret = getDayGroupIndex(im);
				sb.append(ret);
			} catch (Exception e) {
				logger.error("build data failed!", e);
			}
			stime = StockUtil.getNextTimeV3(stime, 1, Calendar.DAY_OF_MONTH);
		}

		return sb;
	}

	private StringBuilder getDayIndexGroupDatasByType(String companycode,
			int ftype, Date stime,Date etime, int limit) {
		StringBuilder sb = new StringBuilder();
		int count = 0;
		int maxCount = 0;
		if(etime==null){
			etime = DateUtil.getDayStartTime(new Date());
			Long time = TradeBitMapService.getInstance().getLatestTradeTime(companycode);
			if(time != null && time>0){
				etime = new Date(time);
			}
			if(DateUtil.getDayStartTime(stime).compareTo(DateUtil.getDayStartTime(new Date())) == 0 && ftype==0){
				/*Dictionary d = DictService.getInstance().getDataDictionaryFromCache(StockConstants.INDEX_CODE_TRADE_S);
				stime = IndexService.getInstance().formatTime(stime,d, companycode);*/
				/*long time = TradeBitMapService.getInstance().getLatestTradeTime(companycode);
				stime = new Date(time);*/
				stime = etime;
			}
			if(stime==null){
				stime = new Date();
			}
			//取最新数据时 如果etime和stime间隔太大 取最近limit条
			if(ftype == 1 && (etime.getTime()-stime.getTime())/(1000*60*60*24) > (limit*2)){
				ftype = 0;
				stime = new Date();
			}
			boolean flag = (stime.compareTo(etime)<=0 || ftype==0);
			while (count < limit && maxCount < (limit * 5) && flag) {
				try {
					IndexMessage im = SMsgFactory.getUMsg(companycode);
					im.setTime(stime);
					im.setNeedAccessCompanyBaseIndexDb(false);
					im.setNeedAccessExtIndexDb(false);
					im.setNeedComput(false);
					im.setNeedRealComputeIndustryValue(false);
					String ret = getDayGroupIndex(im);
					if (ret.length() > 0) {
						count++;
						sb.append(ret);
					}
					maxCount++;
				} catch (Exception e) {
					logger.error("build data failed!", e);
				}
				if (ftype == 0) {
					stime = StockUtil.getNextTimeV3(stime, -1,
							Calendar.DAY_OF_MONTH);
				} else {
					stime = StockUtil
							.getNextTimeV3(stime, 1, Calendar.DAY_OF_MONTH);
				}
				flag = (stime.compareTo(etime)<=0 || ftype==0);
			}
		}else{
			etime = DateUtil.getDayStartTime(etime);
			while (stime.compareTo(etime) <= 0) {
				try {
					IndexMessage im = SMsgFactory.getUMsg(companycode);
					im.setTime(stime);
					im.setNeedAccessCompanyBaseIndexDb(false);
					im.setNeedAccessExtIndexDb(false);
					im.setNeedComput(false);
					im.setNeedRealComputeIndustryValue(false);
					String ret = getDayGroupIndex(im);
					if (ret.length() > 0) {
						count++;
						sb.append(ret);
					}
					maxCount++;
				} catch (Exception e) {
					logger.error("build data failed!", e);
				}
				stime = StockUtil.getNextTimeV3(stime, 1, Calendar.DAY_OF_MONTH);
			}
		}
		return sb;
	}

	public Date getComputeEtime(USubject c, Dictionary d) {
		Date stime = null;
		if (d.getTctype() == StockConstants.TRADE_TYPE) {
			Long sltime = getLatestTradeTime(c.getUidentify());
			if(sltime==null)
				return new Date();
			else
			{
				return new Date(sltime);
			}
		} else {
			stime = CompanyService.getInstance().getLatestReportTime(c.getUidentify());
		}

		return stime;
	}

	public Long getLatestTradeTime(String uidentify)
	{
		return TradeBitMapService.getInstance().getLatestTradeTime(uidentify);
	}
	// 取离给定时间最近的一个交易日
	public static Date getTradeTime(String companycode, Date time) {
		Date t = null;
		try {
			Date st = DateUtil.getDayStartTime(time);
			Date td = new Date();
			if (time.compareTo(td) > 0)
				st = DateUtil.getDayStartTime(td);
			Calendar c = Calendar.getInstance();
			c.setTime(st);
			Date mintime = USubjectService.getInstance().getTradeIndexMinTime(
					companycode, StockConstants.INDEX_CODE_TRADE_S);
			if (mintime == null)
				return null;
			if (c.getTime().compareTo(mintime) <= 0) {
				t = mintime;
			} else {
				int lxc = 0;
				while (c.getTime().compareTo(mintime) > 0 && lxc < 250) {

					if (!IndexService.getInstance().isTradeDate(c.getTime(),
							companycode)) {
						c.add(Calendar.DAY_OF_MONTH, -1);
						lxc++;
					} else {
						t = c.getTime();
						break;
					}

				}
			}

		} catch (Exception e) {
			logger.error("getTradeTime failed!companycode=" + companycode
					+ ";time=" + time);
		}
		return t;

	}

	public Double getTradeIndexValue(IndexMessage im) {
		Date time = getTradeTime(im.getCompanyCode(), im.getTime());
		if (time == null)
			return 0.0;
		return IndexValueAgent.getIndexValue(im.getCompanyCode(),
				StockConstants.INDEX_CODE_TRADE_ZS, time);
	}

	/**
	 * RealTime本地存储
	 * @param ret
	 * @param uptime
	 */
	public void putLocalRealtime(String ret, long uptime) {
		if (!StringUtil.isEmpty(ret)) {
			String[] reta = ret.split("~");
			for (String cs : reta) {
				if (StringUtil.isEmpty(cs))
					continue;
				String[] csa = cs.split("\\|");
				if (csa.length < 2)
					continue;
				String csh = csa[0];
				String companycode = csh.split("\\^")[0];
				StockTrade st = StockTradeService.getInstance()
						.getStockTradeFromCache(companycode);
				if (st != null) {
					try {
						// st在marketcache永远只一份，且永远是同一个对象，
						// 存入时分图时，必须克隆一份新的
						st = st.clone();
						st.setUptime(uptime);						
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
					synchronized (st) {
						ShareTimerTradeDataWapper tt = ShareTimerTradeService
								.getInstance().getShareTimerTradeDataWapper(
										companycode);
						tt.put(st);
					}
				}
			}
		}
	}
	
	/**
	 * 此方法目前stock_zjs在使用,用于处理时分图数据
	 * @param ret
	 */
	public void putRealtimeResult2CacheV2(String ret, long uptime) {
		if (!StringUtil.isEmpty(ret)) {
			String[] reta = ret.split("~");
//			int msgCount = reta.length;
//			long startTime = System.currentTimeMillis();
			for (String cs : reta) {
				if (StringUtil.isEmpty(cs))
					continue;
				String[] csa = cs.split("\\|");
				if (csa.length < 2)
					continue;
				String csh = csa[0];
				String companycode = csh.split("\\^")[0];			
				
				//打印日志	
				int show_log = ConfigCenterFactory.getInt("stock_log.timechart_log_switch", 0);
				if(show_log==1){
					String wstock_log_code = ConfigCenterFactory.getString("stock_log.wstock_log_code", "000002.sz,00001.hk");
					if(wstock_log_code.contains(companycode)){
												
						String fileName = BaseUtil.getConfigPath("wstock/zjsTrade.log");
						String mesInfo = "原始数据比较"+cs;
						try {
							LogSvr.logMsg(mesInfo, fileName);
						} catch (IOException e) {							
						}
					}
				}
				// //记录上一分钟的数据
				// recordLastStockTrade(companycode);
				String csv = csa[1];
				if (!StringUtil.isEmpty(csv)) {
					String[] csva = csv.split("\\^");
					for (String cv : csva) {
						if (!StringUtil.isEmpty(cv)) {
							try {
								String[] cva = cv.split(":");
								if (cva.length < 2)
									continue;
								String indexcode = cva[0];
								String v = cva[1];
								// 如果是现价指标
								if (indexcode
										.equals(StockConstants.INDEX_CODE_TRADE_S)) {
									DelegateOrderService.getInstance()
											.notifyDelegate(companycode,
													Double.valueOf(v));
								}
								RealTimeService.getInstance().put2LocalCache(
										companycode, indexcode,
										Double.valueOf(v));
							} catch (Exception e) {
								logger.error("putRealTimeResult2Cache failed!",
										e);
							}
						}
					}
				}
				if (!StringUtil.isEmpty(companycode)) {
					try {						
						long uptimeTmp = 0l;
						try{
							uptimeTmp = Long.valueOf(csh.split("\\^")[1]);
						}catch (Exception e) {
							Log.error("时间参数不合法");
						}
						initOneTradeData(companycode);
						//缓存中的st对象可能被污染了，比如成交量等等，调整使用原始数据存储到时分图中
						StockTrade st = StockTradeService.getInstance()
								.getStockTradeFromCache(companycode);
						if(uptimeTmp > 0l){
							st.setUptime(uptimeTmp);
						}else{
							uptimeTmp = uptime;
							st.setUptime(uptime);
						}
						//重构 为了解决缓存中StockTrade的数据影响问题
						StockTrade stNew = str2StockTrade(companycode,uptimeTmp,csv);
						if(stNew != null){
							recordLastStockTrade(stNew);
						}
					} catch (Exception e) {
						logger.error("时间必须是LONG型，接口数据=[" + csh.split("\\^")[1]
								+ "]");
					}
				}

			}
//			LogManager.info("处理实时行情，记录"+msgCount+"条，耗时="+(System.currentTimeMillis()-startTime)+"毫秒");
		}

	}
	
	private StockTrade str2StockTrade(String companyCode,long time,String csv){
		if (!StringUtil.isEmpty(csv)) {
			StockTrade stocktrade = new StockTrade();
			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(companyCode);			
			stocktrade.setCode(companyCode);			
			stocktrade.setUptime(time);
			String[] csva = csv.split("\\^");
			for (String cv : csva) {
				if (!StringUtil.isEmpty(cv)) {
					try {
						String[] cva = cv.split(":");
						if (cva.length < 2)
							continue;
						String indexcode = cva[0];
						String v = cva[1];
						if(StockConstants.INDEX_CODE_TRADE_ZS.equals(indexcode)){
							stocktrade.setZs(Double.valueOf(v));
						}else if(StockConstants.INDEX_CODE_TRADE_K.equals(indexcode)){
							stocktrade.setJk(Double.valueOf(v));
						}else if(StockConstants.INDEX_CODE_TRADE_S.equals(indexcode)){
							stocktrade.setC(Double.valueOf(v));
						}else if(StockConstants.INDEX_CODE_TRADE_CJL.equals(indexcode)){
							stocktrade.setCjl(Double.valueOf(v));
						}else if(StockConstants.INDEX_CODE_TRADE_CJE.equals(indexcode)){
							stocktrade.setCje(Double.valueOf(v));
						}else if(StockConstants.INDEX_CODE_TRADE_ZG.equals(indexcode)){
							stocktrade.setH(Double.valueOf(v));
						}else if(StockConstants.INDEX_CODE_TRADE_ZD.equals(indexcode)){
							stocktrade.setL(Double.valueOf(v));
						}else if(StockConstants.INDEX_CODE_TRADE_ZDF.equals(indexcode)){
							stocktrade.setCzf(Double.valueOf(v));
						}
					} catch (Exception e) {
						logger.error("str2StockTrade failed!"+csv,
								e);
					}
				}
				if(stocktrade.getJk() == 0){
					//停牌时，从新浪拉来的数据，是今开为0 时间是今天的时间//所以这里设置一个1000时间，共CompanyService来判断是否停牌
					c.putAttr("_realtime", 1000l);
				}else{
					c.putAttr("_realtime", time);
				}
			}
			return stocktrade;
		}else{
			return null;
		}
		
	}
	

	/**
	 * 此方法目前DCSS在使用
	 * @param ret
	 */
	public void putRealtimeResult2Cache(String ret) {
		if (!StringUtil.isEmpty(ret)) {
			String[] reta = ret.split("~");
			for (String cs : reta) {
				if (StringUtil.isEmpty(cs))
					continue;
				String[] csa = cs.split("\\|");
				if (csa.length < 2)
					continue;
				String csh = csa[0];
				String companycode = csh.split("\\^")[0];
				// 记录上一分钟的数据
//				recordLastStockTrade(companycode);
				String csv = csa[1];
				if (!StringUtil.isEmpty(csv)) {
					String[] csva = csv.split("\\^");
					for (String cv : csva) {
						if (!StringUtil.isEmpty(cv)) {
							try {
								String[] cva = cv.split(":");
								if (cva.length < 2)
									continue;
								String indexcode = cva[0];
								String v = cva[1];
								// 如果是现价指标
								if (indexcode
										.equals(StockConstants.INDEX_CODE_TRADE_S)) {
									DelegateOrderService.getInstance()
											.notifyDelegate(companycode,
													Double.valueOf(v));
								}
								RealTimeService.getInstance().put2LocalCache(
										companycode, indexcode,
										Double.valueOf(v));
							} catch (Exception e) {
								logger.error("putRealTimeResult2Cache failed!",
										e);
							}
						}
					}
				}
				if (!StringUtil.isEmpty(companycode)) {
					try {
						long uptime = Long.valueOf(csh.split("\\^")[1]);
						initOneTradeData(companycode);						
						StockTrade st = StockTradeService.getInstance()
								.getStockTradeFromCache(companycode);
						st.setUptime(uptime);
					} catch (Exception e) {
						logger.error("时间必须是LONG型，接口数据=[" + csh.split("\\^")[1]
								+ "]");
					}
				}
			}
		}

	}

	public void putRealtimeResult2CacheV3(String ret) {
		if (!StringUtil.isEmpty(ret)) {
			String[] reta = ret.split("~");
			for (String cs : reta) {
				if (StringUtil.isEmpty(cs))
					continue;
				String[] csa = cs.split("\\|");
				if (csa.length < 2)
					continue;
				String csh = csa[0];
				String companycode = csh.split("\\^")[0];
				// 记录上一分钟的数据
//				recordLastStockTrade(companycode);
				String csv = csa[1];
				if (!StringUtil.isEmpty(csv)) {
					String[] csva = csv.split("\\^");
					for (String cv : csva) {
						if (!StringUtil.isEmpty(cv)) {
							try {
								String[] cva = cv.split(":");
								if (cva.length < 2)
									continue;
								String indexcode = cva[0];
								String v = cva[1];
								RealTimeService.getInstance().put2LocalCache(
										companycode, indexcode,
										Double.valueOf(v));
							} catch (Exception e) {
								logger.error("putRealTimeResult2Cache failed!",
										e);
							}
						}
					}
				}
				if (!StringUtil.isEmpty(companycode)) {
					try {
						initOneTradeData(companycode);
					} catch (Exception e) {
						logger.error("init trade failed!",e);
					}
				}
			}
		}

	}
	public void putRealtimeResult2CacheV4(String ret) {
		if (!StringUtil.isEmpty(ret)) {
			String[] reta = ret.split("~");
			for (String cs : reta) {
				if (StringUtil.isEmpty(cs))
					continue;
				String[] csa = cs.split("\\|");
				if (csa.length < 2)
					continue;
				String csh = csa[0];
				String companycode = csh.split("\\^")[0];
				// 记录上一分钟的数据
//				recordLastStockTrade(companycode);
				String csv = csa[1];
				if (!StringUtil.isEmpty(csv)) {
					String[] csva = csv.split("\\^");
					for (String cv : csva) {
						if (!StringUtil.isEmpty(cv)) {
							try {
								String[] cva = cv.split(":");
								if (cva.length < 2)
									continue;
								String indexcode = cva[0];
								String v = cva[1];
//								// 如果是现价指标
//								if (indexcode
//										.equals(StockConstants.INDEX_CODE_TRADE_S)) {
//									DelegateOrderService.getInstance()
//											.notifyDelegate(companycode,
//													Double.valueOf(v));
//								}
								RealTimeService.getInstance().put2LocalCache(
										companycode, indexcode,
										Double.valueOf(v));
							} catch (Exception e) {
								logger.error("putRealTimeResult2Cache failed!",
										e);
							}
						}
					}
				}
				if (!StringUtil.isEmpty(companycode)) {
					try {
//						long uptime = Long.valueOf(csh.split("\\^")[1]);
						initOneTradeData(companycode);
//						StockTrade st = StockTradeService.getInstance()
//								.getStockTradeFromCache(companycode);
//						st.setUptime(uptime);
					} catch (Exception e) {
						logger.error("时间必须是LONG型，接口数据=[" + csh.split("\\^")[1]
								+ "]");
					}
				}
			}
		}

	}
	private void recordLastStockTrade(StockTrade st) {
		//stock_zjs才存储到时分图中
		if (BaseConfiguration.getAppName().equals("stock")) {				
			if (st != null) {
				if(st.getC() == 0 || StringUtil.isEmpty(st.getCode())){//当前价不允许为0,为0不加入分时图
					return;
				}				
				
				ShareTimerTradeDataWapper tt = ShareTimerTradeService.getInstance().
						getShareTimerTradeDataWapper(st.getCode(),st.getUptime());
				synchronized (tt) {
					tt.put(st);
				}
			}
		}
	}

	public StringBuffer getRealTimeTradeIndexs(String companycodes,
			String indexcodes, Date time) {
		StringBuffer sb = new StringBuffer();
		time = DateUtil.getDayStartTime(time);
		String[] companycodesa = companycodes.split(",");
		for (String companycode : companycodesa) {
			// Company c =
			// CompanyService.getInstance().getCompanyByCodeFromCache(companycode);
			// if(c==null)
			// continue;
			IndexMessage im = SMsgFactory.getUDCIndexMessage(companycode);
			im.setTime(time);
			im.setNeedAccessCompanyBaseIndexDb(false);
			im.setNeedAccessExtIndexDb(false);
			im.setNeedComput(false);
			im.setNeedRealComputeIndustryValue(false);
			sb.append(companycode);
			sb.append("^");
			sb.append(time.getTime());
			sb.append("|");
			String[] indexcodesa = indexcodes.split(",");
			for (String indexcode : indexcodesa) {
				im.setIndexCode(indexcode);
				im.setTime(time);
				Double v = IndexValueAgent.getIndexValue(im);
				if (v != null && v != 0) {
					sb.append(indexcode);
					sb.append(":");
					sb.append(SMathUtil.getDouble(v, 4));
					sb.append("^");
				}
			}
			sb.append("~");
		}
		return sb;

	}

	public Date getRealtime(String indexcode, String companycode) {
		Dictionary d = DictService.getInstance().getDataDictionary(indexcode);
		return IndexService.getInstance()
				.formatTime(new Date(), d, companycode);
	}

	/**
	 * 如果设置了编译器模式，则认为是在进行数据运算，则以设置的编译器模式的类型为准
	 * 
	 * @param mmsg
	 * @return
	 */
	public static boolean isCompanyMsg(Message mmsg) {
		IndexMessage msg = (IndexMessage) mmsg;
		USubject us = USubjectService.getInstance()
				.getUSubjectByUIdentifyFromCache(msg.getUidentify());
		if (us == null)
			return true;
		return us.getType() == StockConstants.SUBJECT_TYPE_0;
	}

	public static boolean isCompanyMsg(int msgtype) {
		return msgtype == StockConstants.SUBJECT_TYPE_0;
	}

	public boolean checkNeedFetchOrCompute() {
		String indexcode = ConfigCenterFactory.getString(
				"realtime_server.check_realtime_compute_indexcode", "2348");
		int count = 0;
		String companycodes = ConfigCenterFactory.getString(
				"realtime_server.check_realtime_compute_codes",
				"000002.sz,601006.sh,600519.sh,600332.sh");
		Date time = DateUtil.getDayStartTime(new Date());
		String[] companycodesa = companycodes.split(",");
		for (String companycode : companycodesa) {
			Company c = CompanyService.getInstance().getCompanyByCodeFromCache(
					companycode);
			if (c == null)
				continue;
			IndexMessage im = SMsgFactory.getUDCIndexMessage(companycode);
			im.setTime(time);
			im.setNeedAccessCompanyBaseIndexDb(false);
			im.setNeedAccessExtIndexDb(false);
			im.setNeedComput(false);
			im.setNeedRealComputeIndustryValue(false);
			im.setIndexCode(indexcode);
			im.setTime(time);
			Double v = IndexValueAgent.getIndexValue(im);
			if (v == null || v == 0) {
				count++;
			}

		}
		if (count == companycodesa.length)
			return true;
		return false;
	}

	// 清空指定某一天的行情数据
	public void clearTradeDataFromCache(Date cd,boolean isrefresh) {	
		logger.info("clearNDayRealtimeDataFromCache start !,clearStartDate="
				+ cd);
		List<Company> cl = CompanyService.getInstance()
				.getCompanyListFromCache();
		
		String indexcodes = ConfigCenterFactory.getString(
				"stock_dc.resave_realtime_indexcodes",
				"2348,2349,2350,2351,2352,3015,3249");
		String[] indexcodesa = indexcodes.split(",");
		for (Company c : cl) {
			try {
				USubject us = USubjectService.getInstance()
						.getUSubjectByUIdentifyFromCache(c.getCompanyCode());
				if (us == null)
					return;
				for (String indexcode : indexcodesa) {
					Date stime = cd;
//					String k = StockUtil.getExtCachekey(c.getCompanyCode(),
//							indexcode, stime);
					if (us.getType() == StockConstants.SUBJECT_TYPE_0) {
						Dictionary d = DictService.getInstance()
								.getDataDictionary(indexcode.toString());
						if (StockUtil.isBaseIndex(d.getType())) {
							IndexCacheService.getInstance()
									.clearBaseIndexValue2Cache(
											c.getCompanyCode(),
											stime.getTime(), indexcode);
						} else {
//							ExtCacheService.getInstance().remove(k);
							TUextService.getInstance().remove(c.getCompanyCode(),stime.getTime(),indexcode);
						}

					}
					if (us.getType() == StockConstants.SUBJECT_TYPE_1) {
//						IndustryExtCacheService.getInstance().remove(k);
						TUextService.getInstance().remove(us.getUidentify(),stime.getTime(),indexcode);
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(isrefresh)
		{
			String dtype = "trade0001";
			logger.info("start refresh cache dtype=" + dtype);
			EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance()
					.getCacheImpl();
			cimpl.refreshCacheByDataType(dtype);
			logger.info("end refresh cache dtype=" + dtype);
			dtype = "uext";
			logger.info("start refresh cache dtype=" + dtype);
			cimpl.refreshCacheByDataType(dtype);
			logger.info("end refresh cache dtype=" + dtype);
		}
		
	}
	public void clearHTradeDataFromCache(Date cd,boolean isrefresh) {
		logger.info("clearNDayRealtimeDataFromCache start !,clearStartDate="
				+ cd);
		 List<USubject> uSubjectList = USubjectService.getInstance().getUSubjectListHStock();
		
		String indexcodes = ConfigCenterFactory.getString(
				"stock_dc.resave_realtime_indexcodes",
				"2348,2349,2350,2351,2352,3015,3249");
		String[] indexcodesa = indexcodes.split(",");
		for (USubject us : uSubjectList) {
			try {
				if (us == null ||StringUtils.isBlank(us.getUidentify()))
					return;
				for (String indexcode : indexcodesa) {
					Date stime = cd;
//					String k = StockUtil.getExtCachekey(c.getCompanyCode(),
//							indexcode, stime);
					if (us.getType() == StockConstants.SUBJECT_TYPE_0) {
						Dictionary d = DictService.getInstance()
								.getDataDictionary(indexcode.toString());
						if (StockUtil.isBaseIndex(d.getType())) {
							IndexCacheService.getInstance()
							.clearBaseIndexValue2Cache(
									us.getUidentify(),
									stime.getTime(), indexcode);
						} else {
//							ExtCacheService.getInstance().remove(k);
							TUextService.getInstance().remove(us.getUidentify(),stime.getTime(),indexcode);
						}
						
					}
					if (us.getType() == StockConstants.SUBJECT_TYPE_1) {
//						IndustryExtCacheService.getInstance().remove(k);
						TUextService.getInstance().remove(us.getUidentify(),stime.getTime(),indexcode);
					}
					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(isrefresh)
		{
			String dtype = "trade0001";
			logger.info("start refresh cache dtype=" + dtype);
			EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance()
					.getCacheImpl();
			cimpl.refreshCacheByDataType(dtype);
			logger.info("end refresh cache dtype=" + dtype);
			dtype = "uext";
			logger.info("start refresh cache dtype=" + dtype);
			cimpl.refreshCacheByDataType(dtype);
			logger.info("end refresh cache dtype=" + dtype);
		}
		
	}

	/**
	 * 清空前一天的实时数据
	 */
	public void clearTradeDataFromCache(int before,boolean isrefresh) {
		Date td = DateUtil.getDayStartTime(new Date());
		Date cd = StockUtil.getNextTimeV3(td, before, Calendar.DAY_OF_MONTH);
		logger.info("clearNDayRealtimeDataFromCache start !befroe=" + before
				+ ",clearStartDate=" + cd);
		List<Company> cl = CompanyService.getInstance()
				.getCompanyListFromCache();
		String indexcodes = ConfigCenterFactory.getString(
				"stock_dc.resave_realtime_indexcodes",
				"2348,2349,2350,2351,2352,3015,3249");
		String[] indexcodesa = indexcodes.split(",");
		for (Company c : cl) {
			try {
				USubject us = USubjectService.getInstance()
						.getUSubjectByUIdentifyFromCache(c.getCompanyCode());
				if (us == null)
					return;
				for (String indexcode : indexcodesa) {
					Date stime = StockUtil.getNextTimeV3(td, -1,
							Calendar.DAY_OF_MONTH);
					while (stime.compareTo(cd) >= 0) {
//						String k = StockUtil.getExtCachekey(c.getCompanyCode(),
//								indexcode, stime);
						if (us.getType() == StockConstants.SUBJECT_TYPE_0) {
							Dictionary d = DictService.getInstance()
									.getDataDictionary(indexcode.toString());
							if (StockUtil.isBaseIndex(d.getType())) {
								IndexCacheService.getInstance()
										.clearBaseIndexValue2Cache(
												c.getCompanyCode(),
												stime.getTime(), indexcode);
							} else {
//								ExtCacheService.getInstance().remove(k);
								TUextService.getInstance().remove(c.getCompanyCode(),stime.getTime(),indexcode);
							}

						}
						if (us.getType() == StockConstants.SUBJECT_TYPE_1) {
//							IndustryExtCacheService.getInstance().remove(k);
							TUextService.getInstance().remove(us.getUidentify(),stime.getTime(),indexcode);
						}
						stime = StockUtil.getNextTimeV3(stime, -1,
								Calendar.DAY_OF_MONTH);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(isrefresh)
		{
			String dtype = "trade0001";
			logger.info("start refresh cache dtype=" + dtype);
			EhcacheImpl cimpl = (EhcacheImpl) CacheConfig.getInstance()
					.getCacheImpl();
			cimpl.refreshCacheByDataType(dtype);
			logger.info("end refresh cache dtype=" + dtype);
			dtype = "uext";
			logger.info("start refresh cache dtype=" + dtype);
			cimpl.refreshCacheByDataType(dtype);
			logger.info("end refresh cache dtype=" + dtype);
		}
	}

//	public void clearOnlyNDayRealtimeDataFromCache(int before) {
//		Date td = DateUtil.getDayStartTime(new Date());
//		Date cd = IndexService.getInstance().getNextTrade(DateUtil.getDayStartTime(new Date()), Calendar.DAY_OF_MONTH, "000001.sz", before);
//		logger.info("clearNDayRealtimeDataFromCache start !befroe=" + before
//				+ ",clearStartDate=" + cd);
//		List<USubject> cl = USubjectService.getInstance().getUSubjectAHZList();
//		String indexcodes = ConfigCenterFactory.getString(
//				"stock_dc.resave_realtime_indexcodes",
//				"2348,2349,2350,2351,2352,3015,3249");
//		String[] indexcodesa = indexcodes.split(",");
//		for (USubject c : cl) {
//			try {
//				for (String indexcode : indexcodesa) {
//					Date stime = null;
//					Long ltime = IndexService.getInstance().getLatestTradeTime("000001.sz");
//					if(ltime!=null)
//						stime = new Date(ltime);
//					else
//						stime = StockUtil.getNextTimeV3(td, -1,
//							Calendar.DAY_OF_MONTH);
//					while (stime.compareTo(cd) >= 0) {
////						String k = StockUtil.getExtCachekey(c.getCompanyCode(),
////								indexcode, stime);
//						if (c.getType() == StockConstants.SUBJECT_TYPE_0) {
//							Dictionary d = DictService.getInstance()
//									.getDataDictionary(indexcode.toString());
//							if (StockUtil.isBaseIndex(d.getType())) {
//								IndexCacheService.getInstance()
//										.clearBaseIndexValue2Cache(
//												c.getUidentify(),
//												stime.getTime(), indexcode);
//							} else {
////								ExtCacheService.getInstance().remove(k);
//								TUextService.getInstance().remove(c.getUidentify(),stime.getTime(),indexcode);
//							}
//
//						}
//						if (c.getType() == StockConstants.SUBJECT_TYPE_1) {
////							IndustryExtCacheService.getInstance().remove(k);
//							TUextService.getInstance().remove(c.getUidentify(),stime.getTime(),indexcode);
//						}
//						stime = StockUtil.getNextTimeV3(stime, -1,
//								Calendar.DAY_OF_MONTH);
//					}
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
//	}

	public void clearLastDayRealtimeData() {
		clearTradeDataFromCache(-1,false);
	}

	private String getStockTradeKey(String companyCode) {
		return "st." + companyCode;
	}

	public void initOneTradeData(String uidentify) {
		String key = getStockTradeKey(uidentify);
		Date actime = DateUtil.getDayStartTime(new Date());
		IndexMessage im = SMsgFactory.getUMsg(uidentify);
		im.setTime(actime);
		im.setNeedAccessCompanyBaseIndexDb(false);
		im.setNeedAccessExtIndexDb(false);
		im.setNeedComput(false);
		im.setNeedRealComputeIndustryValue(false);

		StockTrade st = StockTradeService.getInstance().getStockTradeFromCache(
				uidentify);
		if (st == null) {
			st = new StockTrade();
			LCEnter.getInstance().put(key, st, SCache.CACHE_NAME_marketcache);
		}

		// zs:昨日收盘价，k:开盘价，s:收盘价/现价,zg:最高价，zd:最低价，cjl:成交量，cje:成交额，zdf:涨跌幅，sd:升跌，hsl:换手率
		HDayService hds = HDayService.getInstance();
		Double dv = hds.getDayIndex(im, ComputeUtil.TRADE_K);// 开盘价
		if (dv != null && dv != 0) {
			st.setJk(dv);

			dv = hds.getDayIndex(im, ComputeUtil.TRADE_S);// 现价
			if (dv != null && dv != 0)
				st.setC(dv);

			// 加上需要的指标
			dv = hds.getDayIndex(im, ComputeUtil.TRADE_ZG);// 最高价
			if (dv != null && dv != 0)
				st.setH(dv);

			dv = hds.getDayIndex(im, ComputeUtil.TRADE_ZD);// 最低价
			if (dv != null && dv != 0)
				st.setL(dv);

			dv = hds.getDayIndex(im, ComputeUtil.TRADE_CJL);// 成交量
			if (dv != null && dv != 0)
				st.setCjl(dv);

			dv = hds.getDayIndex(im, ComputeUtil.TRADE_CJE);// 成交额
			if (dv != null && dv != 0)
				st.setCje(dv);

			dv = hds.getDayIndex(im, ComputeUtil.TRADE_ZDF);// 涨跌幅
			if (dv != null && dv != 0)
				st.setCzf(dv);

			dv = hds.getDayIndex(im, ComputeUtil.TRADE_ZS);// 昨日收盘价
			if (dv != null && dv != 0)
				st.setZs(dv);

		}
		Company cps = CompanyService.getInstance().getCompanyByCode(uidentify);
		if (cps != null) {
			CompanyService.getInstance().initCompanyIndexData(cps);
			CompanyService.getInstance().updateCompanyStockPrice(cps, st);
			st.setCode(cps.getCompanyCode());
			st.setName(cps.getChName());
		}
	}
	
	//组合k线数据
	public String combTradeData(String ret,String companycode,int type){
		long t1 = 0;//历史最后一点
		long t2 = 0;//实时
		StringBuilder buf = new StringBuilder();
		String realData = getRealtimeTrade(companycode,type);//实时
		if(realData.length()>10 && ret.length()>10){
			String t3 = ret.split("~")[0].split("^")[0];//历史最后一点
			String t4 = realData.split("^")[0];//实时
			if(StringUtils.isNumericSpace(t3)){
				t1 = Long.parseLong(t3);
			}
			if(StringUtils.isNumericSpace(t4)){
				t2 = Long.parseLong(t4);
			}
			Log.info("code "+companycode+" type "+type+" lastTime "+ new Date(t1)+" realtime " +new Date(t2));
			if(t1>t2){
				return ret;
			}/*else if(t1<t2){
				buf.append(realData);
				buf.append(ret);
			}else{
				int index= ret.indexOf("~");
				buf.append(realData);
				buf.append(ret.substring(index+1));
			}*/
			Calendar c = Calendar.getInstance();
			switch (type) {
				case 0:
					if(t1==t2){
						int index= ret.indexOf("~");
						buf.append(realData);
						buf.append(ret.substring(index+1));
					}else{
						buf.append(realData);
						buf.append(ret);
					}
					break;
				case 1:// 周K
					c.setTimeInMillis(t1);
					int yearOfWeek_1 =  c.get(Calendar.WEEK_OF_YEAR);
					c.setTimeInMillis(t2);
					int yearOfWeek_2 = c.get(Calendar.WEEK_OF_YEAR);
					if(yearOfWeek_1 == yearOfWeek_2){// 在同一周
						int index= ret.indexOf("~");
						buf.append(realData);
						buf.append(ret.substring(index+1));
					}else{
						buf.append(realData);
						buf.append(ret);
					}
					break;
				case 2://月K
					c.setTimeInMillis(t1);
					int yearOfMonth_1 =  c.get(Calendar.MONTH);
					c.setTimeInMillis(t2);
					int yearOfMonth_2 = c.get(Calendar.MONTH);
					if(yearOfMonth_1 == yearOfMonth_2){//在同一月
						int index= ret.indexOf("~");
						buf.append(realData);
						buf.append(ret.substring(index+1));
					}else{
						buf.append(realData);
						buf.append(ret);
					}
					break;
			}
		}else{
			buf.append(ret);
		}
		return buf.toString();
	}
	
	public String getRealtimeTrade(String companycode ,int type){
		StringBuilder sb = new StringBuilder();
		String result = IndexService.getInstance().getRealtimeTradeDataByType(type,companycode); 
		/*if(TradeCenter.getInstance().isClearRealData(type))
			result = "";*/
		if(!StringUtil.isEmpty(result)){
			sb.append(result);
		}
		return sb.toString();
	}

	public void clearTradeDataFromCache(Date cd, boolean b, String uidentify) {
		logger.info("clearNDayRealtimeDataFromCache start !,clearStartDate="
				+ cd+";uidentify="+uidentify);
//		List<Company> cl = CompanyService.getInstance()
//				.getCompanyListFromCache();
		String indexcodes = ConfigCenterFactory.getString(
				"stock_dc.resave_realtime_indexcodes",
				"2348,2349,2350,2351,2352,3015,3249");
		String[] indexcodesa = indexcodes.split(",");
			try {
				USubject us = USubjectService.getInstance()
						.getUSubjectByUIdentifyFromCache(uidentify);
				if (us == null)
					return;
				for (String indexcode : indexcodesa) {
					Date stime = cd;
//					String k = StockUtil.getExtCachekey(c.getCompanyCode(),
//							indexcode, stime);
					if (us.getType() == StockConstants.SUBJECT_TYPE_0) {
						Dictionary d = DictService.getInstance()
								.getDataDictionary(indexcode.toString());
						if (StockUtil.isBaseIndex(d.getType())) {
							IndexCacheService.getInstance()
									.clearBaseIndexValue2Cache(
											uidentify,
											stime.getTime(), indexcode);
						} else {
//							ExtCacheService.getInstance().remove(k);
							TUextService.getInstance().remove(uidentify,stime.getTime(),indexcode);
						}

					}
					if (us.getType() == StockConstants.SUBJECT_TYPE_1) {
//						IndustryExtCacheService.getInstance().remove(k);
						TUextService.getInstance().remove(us.getUidentify(),stime.getTime(),indexcode);
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		
	}
	
}
