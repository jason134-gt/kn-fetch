package com.yfzx.service.db.mapdb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yz.mycore.core.util.BaseUtil;
/**
 * 共用的mabdb类
 *      
 * @author：杨真 
 * @date：2014年12月5日
 */
public class StockMapdb {
	Logger log = LoggerFactory.getLogger(this.getClass());
	static StockMapdb instance = new StockMapdb();
	DB m_db;
	static Map<String, Map> _tMap = new HashMap<String, Map>();

	public StockMapdb() {

	}

	public static StockMapdb getInstance() {
		return instance;
	}

	public void initMapdb() {
		try {
			File tmpFile = null;
			try {
				tmpFile = new File(BaseUtil.getAppRootWithAppName(),
						"stock.mapdb");
				tmpFile.createNewFile();
			} catch (IOException ex) {
				log.error("init mapdb failed!",ex);

			}
			if (tmpFile != null) {
				m_db = DBMaker.newFileDB(tmpFile).make();
			}
		} catch (Exception e) {
			log.error("init mapdb failed!",e);
		}
	}

	public void registerTable(String tablename) {
		try {
			if (m_db != null) {
				
					Map kmap = _tMap.get(tablename);
					if(kmap==null)
					{
						synchronized (m_db) {
							kmap = _tMap.get(tablename);
							if(kmap==null)
							{
								kmap = m_db.getHashMap(tablename);
								_tMap.put(tablename, kmap);
							}
						}
					}
				
			}
		} catch (Exception e) {
			log.error("registerTable failed!",e);
		}
	}

	public List<String> getTableNames() {
		List<String> tnames = new ArrayList<String>();
		try {
			tnames.addAll(_tMap.keySet());
		} catch (Exception e) {
			log.error("registerTable failed!",e);
		}
		return tnames;
	}
	
	public void put(String key, Object v, String tablename) {
		try {
			Map dm = getTable(tablename);
			if (dm != null) {
				dm.put(key, v);
			}
		} catch (Exception e) {
			log.error("put to mapdb failed!",e);
		}
		
	}
	
	public void putAndCommit(String key, Object v, String tablename) {
		try {
			Map dm = getTable(tablename);
			if (dm != null) {
				dm.put(key, v);
			}
//			m_db.commit();
		} catch (Exception e) {
			log.error("put to mapdb failed!",e);
		}
		
	}
	
	public void deleteTable(String tablename) {
		try {
			m_db.delete(tablename);
			m_db.commit();
		} catch (Exception e) {
			log.error("put to mapdb failed!",e);
		}
		
	}
	public void remove(Object key,String tablename) {
		try {
			Map dm = getTable(tablename);
			if(dm!=null)
			{
				dm.remove(key);
			}
		} catch (Exception e) {
			log.error("put to mapdb failed!",e);
		}
		
	}
    public <V>V get(String k,String tablename)
    {
    	Object v = null;
    	try {
			Map dm = getTable(tablename);
			if (dm != null) {
				v = dm.get(k);
			}
			if (v == null)
				return null;
		} catch (Exception e) {
			log.error("get value from mapdb failed!",e);
		}
		return (V) v;
    }
	
	
	
	public Map getTable(String tablename) {
		Map dm = _tMap.get(tablename);
		if(dm==null)
		{
//			log.error("not register the tablename:"+tablename);
			registerTable(tablename);
			dm = _tMap.get(tablename);
			return dm;
		}
		return dm;
	}

	/**
	 * 用法
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		StockMapdb.getInstance().initMapdb();
		String tname = "stock_test_mapdb";
		StockMapdb.getInstance().registerTable(tname);
		Map<String,String> m = new HashMap<String,String>();
		StockMapdb.getInstance().put("k", m, tname);
		for(int i=0;i<1000000;i++)
		{
			m = StockMapdb.getInstance().get("k", tname);
			m.put("m", "m");
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void commit() {
		if(m_db == null)return;
		try {
			m_db.commit();
		} catch (Exception e) {
			log.error("put to mapdb failed!",e);
		}
		
	}
	
	/**
	 * 提交并压缩 mapdb 一天执行一次
	 */
	public void commitAndCompact(){
		if(m_db == null)return;
		try {			
			m_db.commit();
			m_db.compact();
		} catch (Exception e) {
			log.error("put to mapdb failed!",e);
		}		
	}
	
	public void commitAndClose() {
		if(m_db == null)return;
		try {
			m_db.commit();			
			m_db.close();
		} catch (Exception e) {
			log.error("put to mapdb failed!",e);
		}
	}
}
