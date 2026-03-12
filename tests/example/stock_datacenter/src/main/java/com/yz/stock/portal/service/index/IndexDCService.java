package com.yz.stock.portal.service.index;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SExt;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.StockSqlKey;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.UpdateIndexReq;
import com.stock.common.msg.Message;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.cache.ExtCacheService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IIService;
import com.yfzx.service.db.TUextService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.stock.portal.excel.IDataHandler;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.model.BatchQueueEntity;
import com.yz.stock.util.ExecuteQueueManager;

/**
 * 指标服务的缺省类
 * 
 * @author user
 * 
 */
public class IndexDCService implements IDataHandler {

	private static IndexDCService instance = new IndexDCService();
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	DictService ds = DictService.getInstance();
	static Logger logger = LoggerFactory.getLogger(IndexDCService.class);
	IndexDCService is = IndexDCService.getInstance();
	private final ReentrantLock lock = new ReentrantLock();

	private static String _iPrex = "index";

	private IndexDCService() {

	}

	public static IndexDCService getInstance() {
		return instance;
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
				ExecuteQueueManager.add2IQueue(
						new BatchQueueEntity(StockConstants.TYPE_BASEINDEX,
								tableName, req));

			
			} else {
				// 改为指量异步更新
				ExecuteQueueManager.getInstance().add2IQueue(
						new BatchQueueEntity(StockConstants.TYPE_BASEINDEX,
								tableName, req));

				
			}
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("operator failed!", e);
		}
	}

	@SuppressWarnings("static-access")
	public void update2ExtTable(String companyCode, String tableName,
			String colName, String indexValue, Date time, String indexCode,
			String indexName) {
		try {
			if (StringUtil.isEmpty(indexValue))
				return;
			// 如果是0值，就不做处理
			if (Math.abs(Double.valueOf(indexValue)) == 0) {
				// 放入缓存，下次可以不用计算
//				String ckey = StockUtil.getExtCachekey(companyCode,
//						indexCode, time);
//				ExtCacheService.getInstance().putV(ckey,
//						Double.valueOf(indexValue));
//				TUextService.getInstance().putData(companyCode, time.getTime(), indexCode, Float.valueOf(indexValue));
				return;
			}

			// TODO Auto-generated method stub
			UpdateIndexReq req = new UpdateIndexReq();
			req.setColumnName(colName);
			req.setTableName(tableName);
			req.setTime(time);
			req.setValue(indexValue);
			req.setIndexCode(indexCode);
			req.setUidentify(companyCode);
			req.setIndexName(indexName);
			
			// 计算结果先放缓存
//			String ckey = StockUtil.getExtCachekey(companyCode, indexCode,
//					time);
//			ExtCacheService.getInstance().putV(ckey, Double.valueOf(indexValue));
			int opencache = ConfigCenterFactory.getInt("stock_dc.open_uext_data_cache", 0);
//			if(opencache==1)
				TUextService.getInstance().putData(companyCode, time.getTime(), indexCode, Float.valueOf(indexValue));
			// 改为指量异步更新
			ExecuteQueueManager.add2IQueue(
					new BatchQueueEntity(StockConstants.TYPE_EXTINDEX,
							StockConstants.U_EXT_INDEX, req));

			
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("operator failed!", e);
		}

	}

	

	public IIService getIndexService(int key) {
		return StockFactory.get(_iPrex + key);
	}

	// 注册指标服务类
	static {
		StockFactory.register(_iPrex + StockConstants.INDEX_TYPE_6,
				CIndexService.getInstance());
		StockFactory.register(_iPrex + StockConstants.INDEX_TYPE_6,
				CIndexService.getInstance());
		StockFactory.register(_iPrex + StockConstants.I_EXT_INDEX_TYPE,
				IIndexService.getInstance());
		StockFactory.register(_iPrex + StockConstants.U_EXT_INDEX_TYPE,
				UIndexService.getInstance());
	}

	

	public void upateIndex2Db(Dictionary d, Message msg) {
		
		IndexMessage req = (IndexMessage) msg;
		if(req.getTime()==null)
		{
			System.out.println("update index data error!time is null");
		}
		int iType = d.getType();
		// 分开基础指标与非基础指标
		if (!StockUtil.isBaseIndex(iType)) {
			req.setTableName(SExt.getUExtTableName(req
					.getCompanyCode(),d));
			req.setColumnName(StockConstants.C_EXT_INDEX_VALUE);
			update2ExtTable(req.getCompanyCode(), req.getTableName(),
					req.getColumnName(), String.valueOf(req.getValue()),
					req.getTime(), req.getIndexCode(), d.getColumnChiName());

		} else {
			// 更新计算结果到数据库中
			updateBaseIndex2Db(req.getCompanyCode(), req.getTableName(),
					req.getColumnName(), String.valueOf(req.getValue()),
					req.getTime());
		}
	}

	public void handle(String opt, List<Object> ul) {
		if (opt.equals(StockConstants.OPT_UPDATE)) {
			BatchUpdate(ul);
		}
		if (opt.equals(StockConstants.OPT_INSERT)) {
			BatchInsert(ul);
		}

	}

	static Set<Object> _fset = new HashSet<Object>();
	private void BatchInsert(List<Object> ul) {
		try {
			// Connection con = DriverManager.getConnection(jdbcurl, user, pwd);
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
					UpdateIndexReq v = be.getV();
					stmt.addBatch(v.toInsertSql(StockConstants.TYPE_EXTINDEX));
					
				}
				stmt.executeBatch();
				con.commit();
				iscomplete = true;
			} catch (Exception e) {
				logger.error("BatchInsert failed!", e);
				isfailed = true;

			} finally {
				try {
					if (!iscomplete) {
						logger.error("is not complete,but  db connection free!");
						isfailed = true;
					}
//					mypool.freeConnection(con);
					sse.close();
					//只恢复一次
					if (isfailed&&!_fset.contains(ul)) {	
						_fset.add(ul);
//						printErrorSqlList(ul);
						BatchInsert(ul);
					}
				} catch (Exception e2) {
					logger.error("close db connection failed!", e2);
				}
			}
		} catch (Exception e) {
			logger.error("get db connection failed!", e);
		}

	}

	private void printErrorSqlList(List<Object> ul) {
		StringBuffer sb = new StringBuffer();
		for (int x = 0; x < ul.size(); x++) {
			UpdateIndexReq v = (UpdateIndexReq) ul.get(x);
			sb.append(v.toInsertSql(StockConstants.TYPE_EXTINDEX));
			sb.append(";");
		}
		
		System.out.println(sb.toString());
	}

	private void BatchUpdate(List<Object> ul) {
		try {
//			DbConnectionBroker mypool = StockFactory.getMyConPool();
//			Connection con = mypool.getConnection();
			// Connection con = DriverManager.getConnection(jdbcurl, user, pwd);
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
					UpdateIndexReq v = be.getV();
					stmt.addBatch(v.toUpdateSql(StockConstants.TYPE_EXTINDEX));
				}
				stmt.executeBatch();
				con.commit();
				iscomplete = true;
			} catch (Exception e) {
				logger.error("BatchInsert failed!", e);
				isfailed = true;

			} finally {
				try {
					// con.close();
					if (!iscomplete)
						logger.error("is not complete,but  db connection free!");
//					mypool.freeConnection(con);
					sse.close();
					if (isfailed) {
						// 如果此批失败，则进行单条操作
						handlerFailure(ul);
					}

				} catch (Exception e2) {
					logger.error("close db connection failed!", e2);
				}
			}
		} catch (Exception e) {
			logger.error("get db connection failed!", e);
		}

	}

	private void handlerFailure(List<Object> ul) {
		logger.info("start handle failure message!..............");
		try {
			for (Object o : ul) {
				UpdateIndexReq v = (UpdateIndexReq) o;
				try {
					insertExtTable(v);

				} catch (Exception e) {
					logger.error("handle failed! Companyasset0290 :" + v, e);
				}
			}
		} catch (Exception e) {
			logger.error("handler failure batch failed!", e);
		}

	}

	private void insertExtTable(UpdateIndexReq v) {
		try {
			RequestMessage reqMsg = DAFFactory.buildRequest(
					StockSqlKey.ext_c_index_key_query_0, v,
					StockConstants.common);
			Object value = pLayerEnter.queryForObject(reqMsg);
			if (value == null) {

				reqMsg.setSqlMapKey(StockSqlKey.ext_c_index_key_insert_0);
				String ret = pLayerEnter.insert(reqMsg);
				
			} else {
				// 如果相同，就无需更新
				if (v.getValue().equals(value))
					return;
				reqMsg.setSqlMapKey(StockSqlKey.ext_c_index_key_update_0);
				pLayerEnter.modify(reqMsg);
			}
		} catch (Exception e) {
			logger.error("operator failed!", e);
		}

	}

	
	
}
