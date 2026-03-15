package com.yz.stock.portal.service.company.spider;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.quartz.JobDetail;
//import org.quartz.Scheduler;
//import org.quartz.SchedulerException;
//import org.quartz.SchedulerFactory;
//import org.quartz.StatefulJob;
//import org.quartz.Trigger;
//import org.quartz.TriggerUtils;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockCodes;
import com.stock.common.model.share.Article;
import com.stock.common.model.user.Members;
import com.yfzx.service.db.user.MembersService;
import com.yfzx.service.share.MicorBlogService;
//
//import com.stock.common.model.Company;
//import com.stock.common.model.share.Article;
//import com.yfzx.service.db.CompanyService;
//
/**
 * @author wind 
 * 微博爬虫公共门面  要求有:<br>
 * 1、调度由配置application-quatz.xml,里面包含Cookie,是否运行此爬虫,以及调度策略,建议加到数据中心启动<br>
 * 2、由application-quatz.xml启动 <br>
 * 3、以前爬过的内容，不重复爬   ,继承者实现<br>
 * 4、爬下来的内容，模拟操作写入我们的微博系统 <br>
 * 提供方法:<br>
 * startXueQiu startSina startQQ
 */
public class WBSpiderFacade {
	private static final Logger log = LoggerFactory.getLogger(WBSpiderFacade.class);
	static final String UTF8="utf-8";
	List<Members> memberList = null;
	
	public void startXueQiu(){
		XueQiuWBSpider.getInstance().start();
	}
	public void startSina(){
		SinaWBSpider.getInstance().start();
	}
	public void startQQ(){
		QQWBSpider.getInstance().start();
	}
	
	void start(){
		//配置中心配置 cookie,run,sleepTime
		if(memberList == null || memberList.isEmpty() == true){
			long userIDstart = 10000;//配置中心获取
			int userCount = 100;//配置中心获取
			MembersService ms = MembersService.getInstance();
			memberList = ms.getMembers(userIDstart, userCount);
		}
	}
	
	/**
	 * 模拟用户写库操作<br>
	 * 在解析数据后调用<br>
	 * @param article 需要有微博的标题、内容和简介等
	 */
	void mockPublishArticle(Article article){		
		Random random = new Random();
		Members members =memberList.get(random.nextInt(memberList.size()));
		UUID uuid = UUID.randomUUID();
		article.setUid(members.getUid());
		article.setNick(members.getNickname());
		article.setUuid(uuid.toString());
		article.setTime(Calendar.getInstance().getTimeInMillis());		
		String ret = MicorBlogService.getInstance().publishArticle(article);
		if(!StockCodes.SUCCESS.equals(ret)){
			log.error("写微博失败");
		}		
	}
	
	void serObj(String fileName,Object obj){
		FileOutputStream fs = null;
		ObjectOutputStream os = null;
		try {
			fs = new FileOutputStream(fileName);
			os = new ObjectOutputStream(fs);   
			os.writeObject(obj);  
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			if(os != null){
				try {
					os.flush();
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(fs != null){
				try {
					fs.flush();
					fs.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	Object reSerObj(String fileName){
		Object obj = null;
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(fileName);
			ois = new ObjectInputStream(fis);
			obj = ois.readObject();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}finally{
			if(ois != null){
				try {
					ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(fis != null){
				try {					
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return obj;
	}
	
//	final static Map<Integer,Boolean> statusMap = Collections.synchronizedMap(new HashMap<Integer,Boolean>());
//	public final static int SINA = 0;
//	public final static int XUEQIU = 1;
//	public final static int QQ = 2;
//
//	/*public static WBSpiderFacade getInstance(int type) {
//		WBSpiderFacade wbSF = null;
//		if (SINA == type) {
//			wbSF = SinaWBSpider.getInstance();
//		} else if (XUEQIU == type) {
//			wbSF = XueQiuWBSpider.getInstance();
//		} else if (QQ == type) {
//			wbSF = QQWBSpider.getInstance();
//		}else{
//			log.error("类型不存在，返回空指针异常");
//			throw new NullPointerException("type="+ type +"时，不存在对应的WBSpiderFacade实例 ");
//		}
//		return wbSF;
//	}*/
//
//	/**
//	 * 启动对应的爬虫 
//	 * 使用quartz任务调度
//	 * @param cookie
//	 */
//	public static void start(int type ,String cookie){
//		SchedulerFactory schedFact = 
//				new org.quartz.impl.StdSchedulerFactory();
//		Scheduler sched = null;
//		Class clazz = null;
//		if (SINA == type) {
//			if(statusMap.get(SINA)==true){
//				log.warn("任务已经启动");
//			}
//			statusMap.put(SINA, true);
//			clazz = SinaWBSpider.class;
//		} else if (XUEQIU == type) {
//			if(statusMap.get(XUEQIU)==true){
//				log.warn("任务已经启动");
//			}
//			statusMap.put(XUEQIU, true);
//			clazz = XueQiuWBSpider.class;
//		} else if (QQ == type) {
//			if(statusMap.get(QQ)==true){
//				log.warn("任务已经启动");
//			}
//			statusMap.put(QQ, true);
//			clazz = QQWBSpider.class;
//		}else{
//			log.error("类型不存在，返回空指针异常");
//			throw new NullPointerException("type="+ type +"时，不存在对应的WBSpiderFacade实例 ");
//		}
//		try {
//			sched = schedFact.getScheduler();
//			sched.start();
//			// 创建一个JobDetail，指明name，groupname，以及具体的Job类名，
//			//该Job负责定义需要执行任务
//			JobDetail jobDetail = new JobDetail("WBSpider_"+type, "myJobGroup",
//					clazz);
//			
//			//任务存参数
//			jobDetail.getJobDataMap().put("cookie", cookie);
//
//            // 创建一个每周触发的Trigger，指明星期几几点几分执行
//			Trigger trigger = TriggerUtils.makeWeeklyTrigger(3, 16, 38);
//			trigger.setGroup("myTriggerGroup");
//			// 从当前时间的下一秒开始执行
//			trigger.setStartTime(TriggerUtils.getEvenSecondDate(new Date()));
//			// 指明trigger的name
//			trigger.setName("myTrigger");
//			// 用scheduler将JobDetail与Trigger关联在一起，开始调度任务
//			sched.scheduleJob(jobDetail, trigger);
//			
//		} catch (SchedulerException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
//
//	/**
//	 * 停止对应的爬虫
//	 */
//	abstract public void stop();
//	
//	/**
//	 * 查看对应的爬虫状态
//	 * @return true = 正在运行
//	 */
//	abstract public boolean isRun();
//
//	/**
//	 * @param artList
//	 */
//	void save(List<Article> artList) {
//		//遍历微博文章
//		//随机选ID在1000-1200模拟用户
//		//模拟用户写入微博
//	}
//	
//	List<Company> selectCompanyList(){
//		List<Company> companyArr = new ArrayList<Company>();// CompanyService.getInstance().getCompanyList();
//		Company c1 = new Company();
//		c1.setStockCode("万  科Ａ:000002.sz");
//		companyArr.add(c1);
//		Company c2 = new Company();
//		c2.setStockCode("中国银行:601988.sh");
//		companyArr.add(c2);
//		return companyArr;
//	}
//	
//
}
