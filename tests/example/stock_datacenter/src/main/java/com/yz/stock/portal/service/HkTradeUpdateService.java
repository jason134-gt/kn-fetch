package com.yz.stock.portal.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Trade0001;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.stock.portal.excel.IDataHandler;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.model.BatchQueueEntity;

public class HkTradeUpdateService implements IDataHandler {

	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private static HkTradeUpdateService instance = new HkTradeUpdateService();

	Logger log = LoggerFactory.getLogger(this.getClass());

	private HkTradeUpdateService() {

	}

	public static HkTradeUpdateService getInstance() {
		return instance;
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
					Trade0001 t = be.getV();
					stmt.addBatch(t.toInsertSql());
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
