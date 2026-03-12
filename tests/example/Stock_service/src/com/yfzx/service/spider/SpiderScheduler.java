package com.yfzx.service.spider;

import java.text.ParseException;
import java.util.List;
import java.util.Properties;

import org.quartz.CronExpression;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;

import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;

/**
 * 爬虫调度器 
 * 默认5分钟 
 */
public class SpiderScheduler {
	static Logger log = LoggerFactory.getLogger(SpiderScheduler.class);
	
	static Scheduler scheduler = initScheduler();	
	private static Scheduler initScheduler(){
		Scheduler scheduler = null;
		try {
			// http://blog.sina.com.cn/s/blog_414cc36d0101dmw6.html
			Properties p= new Properties();
	        p.setProperty("org.quartz.threadPool.threadCount", "1");
	        p.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
	        StdSchedulerFactory factory = new StdSchedulerFactory(p);
			scheduler = factory.getScheduler();
			//StdSchedulerFactory.getDefaultScheduler();
		} catch (SchedulerException e) {			
			e.printStackTrace();
		}
		return scheduler;
	}
	
	static {
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {
			//刷新时，重新启动任务
			public void refresh() {				
				try {
					if(scheduler.isShutdown() == false && scheduler.isInStandbyMode() == false){
						SpiderScheduler.startCron();
					}
				} catch (SchedulerException e) {					
					e.printStackTrace();
				}
			}			
		});
	}
	
	/**
	 * 启动任务
	 */
	public static void startCron(){			
		try{
			//重复触发接口时
			if(scheduler.isShutdown() == false ){
				scheduler.shutdown();				
			}
			scheduler = initScheduler(); 
			
			//默认任务5分钟执行一次
			String cronExpression = ConfigCenterFactory.getString("spider.cronExpression", "0 0/5 * * * ?");
			List<SpiderConfigBean> scbList = StockSpider.keys();
			for(SpiderConfigBean scb : scbList){
				String key = scb.getKey();
				try{
					//依赖于Spring组件
					MethodInvokingJobDetailFactoryBean mijdf = new MethodInvokingJobDetailFactoryBean();		
					mijdf.setName(key);
					mijdf.setGroup("爬虫");
					mijdf.setTargetClass(StockSpider.class);
					mijdf.setTargetMethod("startSpiderThread");
					Object[] obj = {key};
					mijdf.setArguments(obj);	
					mijdf.afterPropertiesSet();
					/* 作业、任务 */
					JobDetail jobDetail =  (JobDetail)mijdf.getObject();//new JobDetail("testJobName01", "testJobGroup", TestJob.class);
					CronTrigger trigger = new CronTrigger();
					trigger.setName(key);
					/* Cron表达式制定调度规则 */
					try {
						trigger.setCronExpression(new CronExpression(cronExpression));	
					} catch (ParseException e) {
						e.printStackTrace();
					}
					scheduler.scheduleJob(jobDetail, trigger);
				}catch (Exception e) {
					log.error("",e);
				}
			}			
			//定时任务清理抓取的缓存
			{
				MethodInvokingJobDetailFactoryBean mijdf = new MethodInvokingJobDetailFactoryBean();		
				mijdf.setName("clearNoTodaySpiderCache");
				mijdf.setGroup("爬虫");
				mijdf.setTargetClass(SpiderStorage.class);
				mijdf.setTargetMethod("doSchedulerTask");
				Object[] obj = {};
				mijdf.setArguments(obj);	
				mijdf.afterPropertiesSet();
				/* 作业、任务 */
				JobDetail jobDetail =  (JobDetail)mijdf.getObject();//new JobDetail("testJobName01", "testJobGroup", TestJob.class);
				CronTrigger trigger = new CronTrigger();
				trigger.setName("clearNoTodaySpiderCache");
				/* Cron表达式制定调度规则 */
				try {
					String cronExpression_Clear =  "0 0 0/6 * * ?";
					trigger.setCronExpression(new CronExpression(cronExpression_Clear));	
				} catch (ParseException e) {
					e.printStackTrace();
				}
				scheduler.scheduleJob(jobDetail, trigger);
			}
			String[] groupNameArr = scheduler.getJobGroupNames();
			for(String groupName : groupNameArr){
				String[] jobNameArr = scheduler.getJobNames(groupName);
				for(String jobName : jobNameArr){
					JobDetail job = scheduler.getJobDetail(jobName, groupName);
					log.info(job.getFullName());
				}
			}
			
			scheduler.start();
		}catch (Exception e) {
			log.error("",e);
		}
	}
	
	/**
	 * 中断任务
	 */
	public static void stopCron(){
		try {
			String[] groupNameArr = scheduler.getJobGroupNames();
			for(String groupName : groupNameArr){
				String[] jobNameArr = scheduler.getJobNames(groupName);
				for(String jobName : jobNameArr){
					JobDetail job = scheduler.getJobDetail(jobName, groupName);
					log.info(job.getFullName());
				}
			}
			scheduler.shutdown();
		} catch (SchedulerException e) {			
			log.error("",e);
		}
	}
	
}
