package com.yz.stock.portal.service;

import java.sql.Connection;
import java.sql.DriverManager;
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
import com.stock.common.model.base.Cash0292;
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
import com.yz.stock.portal.model.Companycash0292;
import com.yz.stock.util.DCenterUtil;

public class Companycash0292DataService extends ImportDataBaseService{

	private static Companycash0292DataService instance = new Companycash0292DataService();
	Logger logger = LoggerFactory.getLogger(this.getClass());
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	DBDefaultDaoImpl db = new DBDefaultDaoImpl();
	String jdbcurl = "jdbc:mysql://localhost:3306/stock";
	String user = "root";
	String pwd = "yangzhen";
//	Connection con ;
	private Companycash0292DataService() {
		try {
			Configuration c = BaseFactory.getConfiguration();
//			user = c.getString("stock.user");
//			jdbcurl = c.getString("stock.jdbcurl");
//			pwd = c.getString("stock.pwd");
//			Class.forName("com.mysql.jdbc.Driver");
//			con = DriverManager.getConnection(jdbcurl, user, pwd);
		} catch (Exception e) {
			logger.error("connect db failed!",e);
		}
	}

	public static Companycash0292DataService getInstance() {
		return instance;
	}


	public String update(Companycash0292 vo) {
		// TODO Auto-generated method stub
		String sqlMapKey = "com.yz.stock.portal.model.Companycash0292.update";
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey, vo,
				StockConstants.common);
		return pLayerEnter.modify(req);
	}

	// 通过机构id查询
	public Companycash0292 queryBySecid(Companycash0292 vo) {
		String sqlMapKey = "com.yz.stock.portal.model.Companycash0292.getObOrgid0292";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, vo, StockConstants.common);
		Object o = pLayerEnter.queryForObject(req);
		if (o == null)
			return null;
		return (Companycash0292) o;

	}

	public String insert(Companycash0292 vo) {
		// TODO Auto-generated method stub
		Companycash0292 csi = queryBySecid(vo);
		if (csi != null) {
			//如果相同，就无需更新
			if (StockUtil.compareUpdateVOEqualsQuv(vo, csi,uset))
				return StockCodes.SUCCESS;
			return updateByCode(vo);
		}
		DCenterUtil.removeFromBloomFilter(vo.getKey());
		String sqlMapKey = "com.yz.stock.portal.model.Companycash0292.insert";
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey, vo,
				StockConstants.common);
		String ret =  pLayerEnter.insert(req);
		if(!StockCodes.SUCCESS.equals(ret))
			DCenterUtil.removeFromBloomFilter(StockUtil.getBaseVoBFKey(vo.getKey(),StockConstants.TABLE_NAME_tb_company_cash_0292));
		return ret;
	}

	public String updateByCode(Companycash0292 vo) {
		String sqlMapKey = "com.yz.stock.portal.model.Companycash0292.updateByCode";
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey, vo,
				StockConstants.common);
		return pLayerEnter.modify(req);
		
	}

	public String insertRedirect(Companycash0292 vo) {
		// TODO Auto-generated method stub
		String sqlMapKey = "com.yz.stock.portal.model.Companycash0292.insert";
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey, vo,
				StockConstants.common);
		return pLayerEnter.insert(req);
	}

	@SuppressWarnings("unchecked")
	public List<Companycash0292> queryCompanycash0292List(String obSecid0007) {
		Companycash0292 vo = new Companycash0292();
		vo.setObOrgid0292(obSecid0007);
		String sqlMapKey = "com.yz.stock.portal.model.Companycash0292.queryCompanycash0292ListByOrgid";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, vo, StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null)
			return null;
		return (List<Companycash0292> ) o;
	}

	public List<Companycash0292> queryCash0292ListByCompanycode(String companycode) {
		Cash0292 vo = new Cash0292();
		vo.setcompanycode(companycode);
		String sqlMapKey = "queryCash0292ListByCompanycode";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, vo, StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null)
			return null;
		return (List<Companycash0292> ) o;
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
					Companycash0292 v = (Companycash0292) ul.get(x);
					stmt.execute(v.toInsertSql());
				}
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
					Companycash0292 v = (Companycash0292) ul.get(x);
					stmt.execute(v.toUpdateSql());
				}
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
	
	private void handlerFailure(List<Object> ul) {
		logger.info("start handle failure message!..............");
		try {
			for (Object o : ul) {
				Companycash0292 v = (Companycash0292) o;
				try {
					insert(v);
				} catch (Exception e) {
					logger.error("handle failed! Companycash0292 :" + v,e);
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
			RequestMessage req = DAFFactory.buildRequest("com.yz.stock.portal.model.Companycash0292.doLoadTableKeyList", m,
					StockConstants.common);
			ResponseMessage resp = dbOpt.queryForList(req);
			if (resp.getRetrunCode().equals(BaseCodes.SUCCESS)) {
				retList = (List) resp.getResult();
			}
			return retList;
	}

}
