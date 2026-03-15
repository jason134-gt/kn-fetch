package com.yz.stock.portal.manager;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.util.DbConnectionBroker;
import com.stock.common.util.NosqlBeanUtil;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.core.config.ConfigFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.core.register.IRegister;
import com.yz.mycore.lcs.enter.LCEnter;

public class StockFactory {

	public static Map<String,Object> _pMap = new HashMap<String,Object>();
	static ThreadPoolExecutor es = null;
	static ExecutorService batchDbTheadPool = Executors.newCachedThreadPool();
	static Logger log = LoggerFactory.getLogger(StockFactory.class);
	static int myTaskSize = 5;
	static long myTaskSleep = 100l;
	static BlockingQueue<Object> _mybq =  new ArrayBlockingQueue<Object>(20);
	static DbConnectionBroker myconPool  = null; 
	static boolean computeDataing=false;
	static boolean isfirst = true;
	static BasicDataSource ds = null;
	public static long expTime = Calendar.getInstance().getTimeInMillis();
	public static void init()
	{
		
		Configuration c = ConfigFactory.getConfiguration();
		int coreSize = c.getInt("stock.thread.coreSize");
		int maxSize = c.getInt("stock.thread.maxSize");
		Long keepAliveTime  = c.getLong("stock.thread.keepAliveTime");
		int queueSize = c.getInt("stock.thread.queueSize");
	
		BlockingQueue<Runnable> bq = new ArrayBlockingQueue<Runnable>(queueSize);
		es = new ThreadPoolExecutor(coreSize,maxSize,keepAliveTime,TimeUnit.MILLISECONDS,bq,Executors.defaultThreadFactory(),new RejectedExecutionHandler(){
			
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) 
			{
				log.error("==============================================================================do not run thread!"+r);
			}
		});
		
//		 myTaskSize = c.getInt("stock.thread.myTaskSize");
		 myTaskSleep = c.getInt("stock.thread.myTaskSleep");
		 
		 myTaskSize = ConfigCenterFactory.getInt("stock_dc.compute_task_num", 5);
		 ConfigCenterFactory.notifyRefresh(new IConfigRefresh(){
				
				public void refresh() {
					myTaskSize = ConfigCenterFactory.getInt("stock_dc.compute_task_num", 5);
				}
			});
		Thread t = new Thread(new Runnable(){

			@SuppressWarnings({ "rawtypes", "unchecked" })
			public void run() {
				Map<Future,Object> m = new ConcurrentHashMap<Future,Object>();
				while(true)
				{
					try {
						
						Object o = _mybq.poll();
						
						log.debug("myTaskScheduleThread taskqueue current size : "+_mybq.size()+",not complete task size : "+m.keySet().size());
						
						if(o==null)
						{
							checkCompleteTask(m);
							Thread.sleep(1000l);
							if(Calendar.getInstance().getTimeInMillis()%10==0)
							{
								log.debug("myTaskScheduleThread  checkCompleteTask sleep 1000 ..._mybq.size:"+_mybq.size()+";noTaskComplents :"+m.keySet().size());
							}
							continue;
						}
						else
						{
							isfirst = false;
							expTime = Calendar.getInstance().getTimeInMillis();
						}
						Future f =null;
						
						if(o instanceof Runnable)
							f = es.submit((Runnable) o);
						if(o instanceof Callable)
							f = es.submit((Callable) o);
						
						if(f!=null)
							m.put(f, o);
						//如果提交的任务数据大于系统（myTaskSize)设定的数，则先执行完已提交的任务，再进行下一轮任务
						if(m.keySet().size()>myTaskSize)
						{
							boolean noTaskComplents = true;
							while(noTaskComplents)
							{
								Set<Future> oSet = m.keySet();
								for(Future tf : oSet)
								{
									try {
										if(tf.isDone())
										{
											Object tt = m.get(tf);
											log.debug("myTaskScheduleThread this task is complete ! task : "+tt);
											
											m.remove(tf);
											
											noTaskComplents = false;
										}
									} catch (Exception e) {
										log.error("myTaskScheduleThread wait future failed!",e);
									}
								}
								if(noTaskComplents)
								{
									log.debug("myTaskScheduleThread noTaskComplents sleep 200 , taskqueue current size : "+_mybq.size()+",not complete task size : "+m.keySet().size());
									Thread.sleep(200l);
								}
							}
							
//							//在上一批任务都已执行完的情况下，清除上一轮的所有任务。
//							m.clear();
						}
						
						Thread.sleep(myTaskSleep);//每个流程结束，休息一下。
					} catch (Exception e) {
						log.error("wait future failed!",e);
					}
				}
				
			}

			@SuppressWarnings("rawtypes")
			private void checkCompleteTask(Map<Future, Object> m) {
				// TODO Auto-generated method stub
				Set<Future> oSet = m.keySet();
				for(Future tf : oSet)
				{
					try {
						if(tf.isDone())
						{
							Object tt = m.get(tf);
							log.debug("myTaskScheduleThread this task is complete ! task : "+tt);
							m.remove(tf);
						}
					} catch (Exception e) {
						log.error("myTaskScheduleThread wait future failed!",e);
					}
				}
			}
			
		});
		
		t.setName("myTaskScheduleThread");
		t.start();
		
		initDbPool();
		
	}

	
//	public static DbConnectionBroker getMyConPool()
//	{
//		return myconPool;
//	}
	private static void initDbPool() {
		
		try {
			Configuration c = BaseFactory.getConfiguration();
//			String user = c.getString("stock.indexcompute.batchWrite.user");
//			String jdbcurl = c.getString("stock.indexcompute.batchWrite.jdbcurl");
//			String pwd = c.getString("stock.indexcompute.batchWrite.pwd");
//			Class.forName("com.mysql.jdbc.Driver");
//			int initSize =  c.getInt("stock.indexcompute.batchWrite.initSize");
//			int maxActive =  c.getInt("stock.indexcompute.batchWrite.maxActive");
//			int connTimeout =  c.getInt("stock.indexcompute.batchWrite.connTimeout");
//			String logPath =  c.getString("stock.indexcompute.batchWrite.logPath");
//		    logPath = BaseConfiguration.getConfBasePath()+logPath;
//			myconPool = new DbConnectionBroker("com.mysql.jdbc.Driver",
//					 jdbcurl,
//					 user, pwd, initSize, maxActive,
//					 logPath, connTimeout); 
//		    Class.forName("org.apache.commons.dbcp.BasicDataSource");
		    Object obj = BaseFactory.getBean("dataSource");
		    //不能加这句，加了就类型不相等了
//		    Class.forName("org.apache.commons.dbcp.BasicDataSource");
		    boolean check = (obj instanceof BasicDataSource);
		    if(check){		    	
		    	ds = (BasicDataSource) obj;
		    }else{
		    	//obj的是加载jetty/lib目录下的？BasicDataSource是WEB-INF/lib下的？引起的不一致？
		    	log.warn("BaseFactory.getBean('dataSource')跟BasicDataSource类型不一致");
		    }
		    if(ds ==null){
				BasicDataSource bds = new BasicDataSource();
				Map<String,String> map = NosqlBeanUtil.bean2Map(obj);
				bds.setDriverClassName(String.valueOf(map.get("driverClassName")));
				bds.setUrl(String.valueOf(map.get("url")));
				bds.setUsername(String.valueOf(map.get("username")));
				bds.setUsername(String.valueOf(map.get("password")));
				bds.setInitialSize(Integer.valueOf(String.valueOf(map.get("initialSize"))));
				bds.setMaxActive(Integer.valueOf(String.valueOf(map.get("maxActive"))));
				bds.setTimeBetweenEvictionRunsMillis(Integer.valueOf(String.valueOf(map.get("timeBetweenEvictionRunsMillis"))));
				bds.setTestWhileIdle(Boolean.valueOf(String.valueOf(map.get("testWhileIdle"))));
				ds = bds;
			}
//			Connection con = ds.getConnection();
//			System.out.println(con);			
		} catch (Exception e) {
			log.error("connect db failed!", e);
		}
	}

	//把写数据库的线程池与系统任务线程池分开。
	public static ExecutorService getBatchDBThreadPool()
	{
		return batchDbTheadPool;
	}
	public static ThreadPoolExecutor getThreadPool() {
		return es;
	}

	public static void buildReqByDictionary(IndexMessage req, Dictionary dd) {
		// TODO Auto-generated method stub
		req.setTableName(dd.getTableName());
		req.setColumnName(dd.getColumnName());
		req.setIndexCode(dd.getIndexCode());
	}
	
	public static Map<String, Object> get_pMap() {
		return _pMap;
	}

	public static void set_pMap(Map<String, Object> _pMap) {
		StockFactory._pMap = _pMap;
	}

	public static IRegister getMyRegister() {
		// TODO Auto-generated method stub
		return BaseFactory.getSysRegister().get(StockConstants.STOCK_REGISTER);
	}

	public static void register(String key, Object value) {
		// TODO Auto-generated method stub
		getMyRegister().register(key, value);
	}

	public static <T> T get(String key) {
		// TODO Auto-generated method stub
		Object value = getMyRegister().get(key);
		if(value==null)
		{
			return null;
		}
		return (T) value;
	}
	
	public static void excute(Runnable r)
	{
		try {
			es.execute(r);
		} catch (Exception e) {
			log.error("execute task failed!",e);
		}
	}
	public static Future submit(Callable r)
	{
		try {
			return es.submit(r);
		} catch (Exception e) {
			log.error("execute task failed!",e);
		}
		return null;
	}
	//把任务提交到阻塞队列中，系统控制同时运行的任务数
	public static void submitTaskBlocking(Object r)
	{
		try {
			_mybq.put(r);
//			submitNoBlocking(r);
		} catch (Exception e) {
			log.error("execute task failed!",e);
		}

	}
	
	private static void submitNoBlocking(Object o) {
		// TODO Auto-generated method stub
		if(o instanceof Runnable)
			 es.submit((Runnable) o);
		if(o instanceof Callable)
			 es.submit((Callable) o);
	}

	/*
	 * 默认从common cache中取
	 */
	public <T> T get(String key, String cacheName) {
		// TODO Auto-generated method stub
		Object value = LCEnter.getInstance().get(key, getCacheName(StockConstants.common));
		if(value==null)
		{
			return null;
		}
		return (T) value;
	}

	private String getCacheName(String type) {
		// TODO Auto-generated method stub
		return type+"cache";
	}

	/*
	 * 默认放入common cache中
	 */
	public  <T> void put(String key, T t, String cacheName) {
		// TODO Auto-generated method stub
		LCEnter.getInstance().put(key, t, getCacheName(StockConstants.common));
	}

	public static BasicDataSource getDbcpPool()
	{
		return ds;
	}
	public static SqlSessionFactory getSqlSessionFactory() {
		// TODO Auto-generated method stub
		return (SqlSessionFactory) BaseFactory.getSqlSessionFactory();
	}
	
	public static boolean noRunTask()
	{
		return _mybq.size()==0;
	}
}
