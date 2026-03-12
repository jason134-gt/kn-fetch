package com.yz.stock.portal.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.base.Asset0290;
import com.stock.common.util.DbConnectionBroker;
import com.stock.common.util.StockUtil;
import com.yz.common.vo.BaseVO;
import com.yz.mycore.core.inter.IOperator;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.dao.db.DBDefaultDaoImpl;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.stock.portal.excel.ImportDataBaseService;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.model.Companyasset0290;
import com.yz.stock.util.DCenterUtil;


public class Companyasset0290DataService extends ImportDataBaseService{

	private static Companyasset0290DataService instance = new Companyasset0290DataService();
	Logger logger = LoggerFactory.getLogger(this.getClass());
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	DBDefaultDaoImpl db = new DBDefaultDaoImpl();
	String jdbcurl = "jdbc:mysql://localhost:3306/stock";
	String user = "root";
	String pwd = "yangzhen";
	Connection con ;
	private Companyasset0290DataService() {
		try {
			Configuration c = BaseFactory.getConfiguration();
			user = c.getString("stock.user");
			jdbcurl = c.getString("stock.jdbcurl");
			pwd = c.getString("stock.pwd");
			Class.forName("com.mysql.jdbc.Driver");
//			con = DriverManager.getConnection(jdbcurl, user, pwd);
		} catch (Exception e) {
			logger.error("connect db failed!",e);
		}
	}

	public static Companyasset0290DataService getInstance() {
		return instance;
	}


	public String update(Companyasset0290 vo) {

		String sqlMapKey = "com.yz.stock.portal.model.Companyasset0290.update";
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey, vo,
				StockConstants.common);
		return pLayerEnter.modify(req);
	}

	// 通过机构id查询
	public Companyasset0290 queryBySecid(Companyasset0290 vo) {
		String sqlMapKey = "com.yz.stock.portal.model.Companyasset0290.getObOrgid0290";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, vo, StockConstants.common);
		Object o = pLayerEnter.queryForObject(req);
		if (o == null)
			return null;
		return (Companyasset0290) o;

	}

	public String insert(Companyasset0290 vo) {
		// TODO Auto-generated method stub
		Companyasset0290 csi = queryBySecid(vo);
		if (csi != null) {
			//如果相同，就无需更新
			if (StockUtil.compareUpdateVOEqualsQuv(vo, csi,uset))
				return StockCodes.SUCCESS;
			return updateByCode(vo);
		}
		String sqlMapKey = "com.yz.stock.portal.model.Companyasset0290.insert";
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey, vo,
				StockConstants.common);
		String ret =  pLayerEnter.insert(req);
		if(!StockCodes.SUCCESS.equals(ret))
			DCenterUtil.removeFromBloomFilter(StockUtil.getBaseVoBFKey(vo.getKey(),StockConstants.TABLE_NAME_tb_company_asset_0290));
		return ret;
	}
	
	public String insertRedirect(Companyasset0290 vo) {
		// TODO Auto-generated method stub
	
		String sqlMapKey = "com.yz.stock.portal.model.Companyasset0290.insert";
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey, vo,
				StockConstants.common);
		return pLayerEnter.insert(req);
	}

	public String updateByCode(Companyasset0290 vo) {
		
		String sqlMapKey = "com.yz.stock.portal.model.Companyasset0290.updateByCode";
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey, vo,
				StockConstants.common);
		return pLayerEnter.modify(req);
	}

	@SuppressWarnings("unchecked")
	public List<Companyasset0290> queryCompanyasset0290List(String obSecid0007) {
		Companyasset0290 vo = new Companyasset0290();
		vo.setObOrgid0290(obSecid0007);
		String sqlMapKey = "com.yz.stock.portal.model.Companyasset0290.queryCompanyasset0290ListByOrgid";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, vo, StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null)
			return null;
		return (List<Companyasset0290> ) o;
	}

	@SuppressWarnings("unchecked")
	public List<Companyasset0290> queryAsset0290ListByCompanyCode(String companycode) {
		Asset0290 vo = new Asset0290();
		vo.setCompanycode(companycode);
		String sqlMapKey = "queryAsset0290ListByCompanyCode";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, vo, StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null)
			return null;
		return (List<Companyasset0290> ) o;
	}


	public void BatchInsert(List<Object> ul) {
		try {
//			DbConnectionBroker mypool = StockFactory.getMyConPool();
//			Connection con =  mypool.getConnection();
//			Connection con = DriverManager.getConnection(jdbcurl, user, pwd);
			SqlSession sse = StockFactory.getSqlSessionFactory().openSession();
			Connection con =  sse.getConnection();
			boolean isfailed = false;
			try {

				con.setAutoCommit(false);
				Statement stmt = con.createStatement(
						ResultSet.TYPE_SCROLL_SENSITIVE,
						ResultSet.CONCUR_READ_ONLY);
				for (int x = 0; x < ul.size(); x++) {
					Companyasset0290 v = (Companyasset0290) ul.get(x);
					stmt.addBatch(v.toInsertSql());
				}
				stmt.executeBatch();
				con.commit();
			} catch (Exception e) {
				logger.error("BatchInsert failed!", e);
				isfailed = true;

			} finally {
				try {
//					con.close();
//					mypool.freeConnection(con);
					sse.commit();
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
			logger.error("get db connection failed!",e);
		}
		
	}


	public void BatchUpdate(List<Object> ul) {
		
		try {
//			DbConnectionBroker mypool = StockFactory.getMyConPool();
//			Connection con =  mypool.getConnection();
			SqlSession sse = StockFactory.getSqlSessionFactory().openSession();
			Connection con =  sse.getConnection();
//			Connection con = DriverManager.getConnection(jdbcurl, user, pwd);
			boolean isfailed = false;
			try {
				con.setAutoCommit(false);
				Statement stmt = con.createStatement(
						ResultSet.TYPE_SCROLL_SENSITIVE,
						ResultSet.CONCUR_READ_ONLY);
				for (int x = 0; x < ul.size(); x++) {
					Companyasset0290 v = (Companyasset0290) ul.get(x);
					stmt.addBatch(v.toUpdateSql());
				}
				stmt.executeBatch();
				con.commit();
			} catch (Exception e) {
				logger.error("BatchInsert failed!", e);
				isfailed = true;

			} finally {
				try {
//					con.close();
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
			logger.error("get db connection failed!",e);
		}
		
	}

	
	public void handlerFailure(List<Object> ul) {
		logger.info("start handle failure message!..............");
		try {
			for (Object o : ul) {
				Companyasset0290 v = (Companyasset0290) o;
				try {
					insert(v);
				} catch (Exception e) {
					logger.error("handle failed! Companyasset0290 :" + v,e);
				}
			}
		} catch (Exception e) {
			logger.error("handler failure batch failed!",e);
		}
		
	}

	@Override
	public List doLoadTableKeyList(String tableName, int start,int limit) {
		 IOperator dbOpt = new DBDefaultDaoImpl();
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("start", start);
			m.put("limit", limit);
			m.put("tableName", tableName);
			List<BaseVO> retList = null;
			RequestMessage req = DAFFactory.buildRequest("com.yz.stock.portal.model.Companyasset0290.doLoadTableKeyList", m,
					StockConstants.common);
			ResponseMessage resp = dbOpt.queryForList(req);
			if (resp.getRetrunCode().equals(BaseCodes.SUCCESS)) {
				retList = (List) resp.getResult();
			}
			return retList;
	}

}
