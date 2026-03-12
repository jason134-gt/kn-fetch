package com.yfzx.service.share.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Company;
import com.stock.common.model.share.Article;
import com.stock.common.model.share.ComparatorArticleByCounts;
import com.stock.common.model.share.TimeLine;
import com.stock.common.model.user.Members;
import com.stock.common.util.CassandraHectorGateWay;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.user.MembersService;
import com.yfzx.service.share.MicorBlogService;
import com.yfzx.service.share.TimeLineService;
import com.yfzx.service.share.TimeLineService.SAVE_TABLE;
import com.yz.configcenter.ConfigCenterFactory;

public class HotAnalysisTask {

	private static final Logger log = LoggerFactory.getLogger(HotAnalysisTask.class);
	private static CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
	private static boolean executeCompanyHot = false;
	
	
	public void executeCompanyHot(){
		if(executeCompanyHot == false){
			executeCompanyHot = true;
			long starttime = System.currentTimeMillis();
			try{
				List<Company> companyArr = CompanyService.getInstance().getCompanyList();
				for (Company company : companyArr) {
					boolean run = true;//从配置中心获取 ,如果是false,则中断分析
					if(run == false){
						log.error("中断运行executeCompanyHot！");
						return ;
					}	
					if("暂停上市".equals(company.getF032v()) || "预披露".equals(company.getF032v()) 
							||"已发行未上市".equals(company.getF032v()) ||"*ST".equals(company.getF032v()) ){
						//非正常上市的跳过
						continue;
					}
					String companyCode = company.getCompanyCode();
					List<Article> companyHotList = getHotCompany(companyCode);
					String companyhot = getUuidListStr(companyHotList);
					Map<String,String> objMap = new HashMap<String,String>();
					if(!StringUtil.isEmpty(companyhot)){
						objMap.put("value", companyhot);
						ch.insert("hot", companyCode, objMap);
					}
				}
			}catch (Exception e) {
				// TODO: handle exception
			}finally{
				long endtime = System.currentTimeMillis();
				log.debug("耗时="+(endtime - starttime)/1000+"秒");
				System.out.println("耗时="+(endtime - starttime)/1000+"秒");
				executeCompanyHot = false;
			}
			
		}
	}
	
	public void executeHot(){
		//取30天 VIP用户的热门访问文章 key=hot30
		//7天 key=hot7
		//1小时 key=hot
		//1天的文章 热门访问
		List<Article> day1HotList = getHotDays(1);
		String day1hot = getUuidListStr(day1HotList);
		Map<String,String> objMap = new HashMap<String,String>();
		if(!StringUtil.isEmpty(day1hot)){
			objMap.put("value", day1hot);
			ch.insert("hot", "day1hot", objMap);
		}
		List<Article> day7HotList = getHotDays(7);
		String day7hot = getUuidListStr(day7HotList);
		if(!StringUtil.isEmpty(day7hot)){
			objMap.put("value", day7hot);
			ch.insert("hot", "day7hot", objMap);
		}
		List<Article> day30HotList = getHotDays(30);
		String day30hot = getUuidListStr(day30HotList);
		if(!StringUtil.isEmpty(day30hot)){
			objMap.put("value", day30hot);
			ch.insert("hot", "day30hot", objMap);
		}
		
		//精选文章 规则 累积热门文章，合并
		List<Article> dayallHotList = getHotDays(0);
		String dayallhot = getUuidListStr(dayallHotList);
		if(!StringUtil.isEmpty(day30hot)){
			objMap.put("value", dayallhot);
			ch.insert("hot", "dayallhot", objMap);
		}
		
	}	
	
	private String getUuidListStr(List<Article> alist){
		StringBuffer sbuf = new StringBuffer();
		for(int i =0;i<alist.size();i++){
			Article article = alist.get(i);
			sbuf.append(article.getUuid());
			if(i != (alist.size()-1)){
				sbuf.append(",");
			}
		}
		return sbuf.toString();		
	}
	
	private List<Article> getHotCompany(String companyCode){
		TimeLineService tls = TimeLineService.getInstance();
		List<TimeLine> timelineList = tls.getTimeLineList(companyCode, SAVE_TABLE.TOPIC, 0, Integer.MAX_VALUE);
		MicorBlogService mbs = MicorBlogService.getInstance();
		List<Article> articleList = mbs.getArticleListOnlyCounts(timelineList,0);
		//所有的根据浏览次数和转播次数排序 取前100名
		ComparatorArticleByCounts cabc = new ComparatorArticleByCounts();
		Collections.sort(articleList,cabc);
		if(100 < articleList.size()){
			articleList = articleList.subList(0, 100);
		}
		return articleList;
	}
	
	/**
	 * 按次数排序 获取前100的文章uuid
	 * @param days =0时，所有时间
	 * @return List<Article> 只包括简单的uuid,浏览次数和转载次数
	 */
	private List<Article> getHotDays(int days){
		int num = 100000;//假设用户文章上限
		
		//key = topic
		long endTime = System.currentTimeMillis();
		long startTime = 0;
		if(days != 0 ){
			startTime = endTime - 1000l*3600l*24l*days ;
		}
		int minCounts = ConfigCenterFactory.getInt("stock_zjs.minCounts", 0);
		TimeLineService tls = TimeLineService.getInstance();
		List<Members> userList = getVipMembers();
		MicorBlogService mbs = MicorBlogService.getInstance();
		List<Article> articleList = new ArrayList<Article>();
		for(Members m : userList){
			List<TimeLine> tlList = 
					tls.getTimeLineListByTime(String.valueOf(m.getUid()), 
							SAVE_TABLE.ARTICLE, startTime, endTime,num);
			
			articleList.addAll(mbs.getArticleListOnlyCounts(tlList,minCounts));
		}
		
		//所有的根据浏览次数和转播次数排序 取前100名
		ComparatorArticleByCounts cabc = new ComparatorArticleByCounts();
		Collections.sort(articleList,cabc);
		if(100 < articleList.size()){
			articleList = articleList.subList(0, 100);
		}
		return articleList;
	}
	
	private List<Members> getVipMembers(){
		long userIDstart = ConfigCenterFactory.getInt("stock_zjs.uidStart", 0);//配置中心获取
		int userCount = ConfigCenterFactory.getInt("stock_zjs.userCount", 500);//配置中心获取
		MembersService ms = MembersService.getInstance();
		List<Members> memberList = ms.getMembers(userIDstart, userCount);
		return memberList;		
	}	
	
}
