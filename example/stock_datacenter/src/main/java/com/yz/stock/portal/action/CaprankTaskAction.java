package com.yz.stock.portal.action;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SExt;
import com.stock.common.constants.StockConstants;
import com.stock.common.model.Extindex;
import com.stock.common.model.Industry;
import com.stock.common.util.CapUtil;
import com.stock.common.util.DateUtil;
import com.stock.common.util.NetUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.cache.ExtCacheService;
import com.yfzx.service.db.IndustryService;
import com.yfzx.service.db.TUextService;
import com.yz.common.vo.BaseVO;
import com.yz.mycore.core.inter.IOperator;
import com.yz.mycore.core.util.BaseCodes;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.bean.ResponseMessage;
import com.yz.mycore.daf.dao.db.DBDefaultDaoImpl;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.stock.common.BaseAction;
import com.yz.stock.portal.cache.TUExtCacheLoadService;
import com.yz.stock.portal.task.RuleTimerTask;
import com.yz.stock.portal.task.TaskEnter;

/**
 * 各能力评分任务
 * @author Administrator
 *
 */
public class CaprankTaskAction extends BaseAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9171952708481009032L;
	public IOperator dbOpt = new DBDefaultDaoImpl();
	Logger log = LoggerFactory.getLogger(this.getClass());
	RuleTimerTask rtt = RuleTimerTask.getInstance();
	boolean isloaded = false;
	static boolean isloadData = true;

	@Action(value = "/caprank/caprankcompute")
	public String caprankcompute() {
		String pstime = this.getHttpServletRequest().getParameter("stime");
		String petime = this.getHttpServletRequest().getParameter("etime");
		String type = this.getHttpServletRequest().getParameter("type");
		String loaddata = NetUtil.getParameterString(getHttpServletRequest(), "loaddata", "0");
		String ret = ERROR;
		try {
			if(StringUtil.isEmpty(pstime)||StringUtil.isEmpty(petime))
				return ERROR;
			Date stime = DateUtil.format(pstime);
			Date etime = DateUtil.format(petime);
//			if(isloadData)
//			{
//				ComputeIndexManager.getInstance().computeInit();
//			}
			IndustryService is = IndustryService.getInstance();
			List<String> tags = is.getAllMainTags();
			String cis = CapUtil.getChildCaps();// 综合能力与其它能力分开算，因为需要先算出其它的能力，再把其它能力数据转入中间表，才能算综合能力
			//计算综合能力
			if(type!=null&&type.equals("1"))
			{
				cis = CapUtil.getZHNL();
				//把子能力的数据加载到中间表
//				loadData2MidTable();
				loadCapIndex2Cache(stime,etime);
			}
			if(!isloaded||"1".equals(loaddata))
			{
				TUExtCacheLoadService el = new TUExtCacheLoadService();
				el.LoadExtData2Cache(stime,etime);	
				isloaded = true;
			}
			TaskEnter.getInstance().caprankcompute(stime,etime,tags,cis);
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	@Action(value = "/caprank/caprankcomputeOneTime")
	public String caprankcomputeOneTime() {
		String ptime = this.getHttpServletRequest().getParameter("time");
		String type = this.getHttpServletRequest().getParameter("type");
		String loaddata = NetUtil.getParameterString(getHttpServletRequest(), "loaddata", "0");
		if(StringUtil.isEmpty(ptime))
			return ERROR;
		Date time = DateUtil.format(ptime);
		String ret = ERROR;
		try {
//			if(isloadData)
//			{
//				ComputeIndexManager.getInstance().computeInit();
//			}
			IndustryService is = IndustryService.getInstance();
//			List<Industry> il = is.getIndustryCSRCTreeFromCache(0).get(IndustryService.root)
//					.getLeafChildrenIndustry();
//			List<String> tags = toTagList(il);
			List<String> tags = is.getAllMainTags();
			String cis = CapUtil.getChildCaps();// 综合能力与其它能力分开算，因为需要先算出其它的能力，再把其它能力数据转入中间表，才能算综合能力
			//计算综合能力
			if(type!=null&&type.equals("1"))
			{
				cis = CapUtil.getZHNL();
				//把子能力的数据加载到中间表
//				loadData2MidTable();
				loadCapIndex2Cache(time,time);
			}
			if(!isloaded||"1".equals(loaddata))
			{
				TUExtCacheLoadService el = new TUExtCacheLoadService();
				el.LoadExtData2Cache(time,time);
				isloaded = true;
			}
			TaskEnter.getInstance().caprankcompute(time,time,tags,cis);
			//loadData2MidTable();
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	private List<String> toTagList(List<Industry> il) {
		List<String> ls = new ArrayList<String>();
		for(Industry ind : il)
		{
			ls.add(ind.getName());
		}
		return ls;
	}
	
	@Action(value = "/caprank/caprankcomputeOneTimeOneTag")
	public String caprankcomputeOneTimeOneTag() {
		Date time = NetUtil.getParameterDate(this.getHttpServletRequest(),
				"time", null);
		String tag = this.getHttpServletRequest().getParameter("tag");
		String testIndex = this.getHttpServletRequest().getParameter("testIndex");
		String loaddata = NetUtil.getParameterString(getHttpServletRequest(), "loaddata", "0");
		if(time==null||StringUtil.isEmpty(tag)||StringUtil.isEmpty(testIndex))
			return ERROR;
		String ret = ERROR;
		try {
			String cis = CapUtil.getChildCaps();// 综合能力与其它能力分开算，因为需要先算出其它的能力，再把其它能力数据转入中间表，才能算综合能力
			if(!IndustryService.root.equals(testIndex))
			{
				cis = testIndex;
			}
			
			//计算综合能力
			if(cis.equals(CapUtil.getZHNL()))
			{
				//把子能力的数据加载到中间表
//				loadData2MidTable();
				loadCapIndex2Cache(time,time);
			}
			if(!isloaded||"1".equals(loaddata))
			{
				TUExtCacheLoadService el = new TUExtCacheLoadService();
				el.LoadExtData2Cache(time,time);	
				isloaded = true;
			}
			TaskEnter.getInstance().caprankcomputeOneTimeOneTag(time,time,tag,cis);
			//loadData2MidTable();
			ret = SUCCESS;
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}

	public void loadCapIndex2Cache(Date stime,Date etime) {
		try {
			for (int i = 0; i < SExt.UEXT_U_TABLE_NUM; i++) {
				try {
					String tableName = SExt.UEXT_U_TABLE_PREFIX + i;
						List<BaseVO> retList = loadCapIndexDataFromDB(
								tableName, stime, etime);
						addData2Cache(retList);
				} catch (Exception e) {
					log.error("load Asset data failed!", e);
				}
			}
		} catch (Exception e) {
			log.error("load Asset data failed!", e);
		}
		
	}

	private void addData2Cache(List<BaseVO> dlist) {
		// TODO Auto-generated method stub
				if (dlist != null) {
					for (BaseVO vo : dlist) {
						try {
							Extindex evo = (Extindex) vo;
							TUextService.getInstance().putData(evo.getCompanyCode(), evo.getTime().getTime(), evo.getIndexCode(), evo.getValue());
//							String key = StockUtil.getExtKeyByCompanyAndTime(
//									evo.getCompanyCode(), evo.getTime());
//							Map<Object, Float> cm = ExtCacheService
//									.getInstance().getMap(key);
//							if (cm == null) {
//								cm = new ConcurrentHashMap<Object, Float>();
//								ExtCacheService.getInstance().putMap(key, cm);
//							}
//							Object o = cm.get(Integer.valueOf(evo
//									.getIndexCode()));
//							if (o == null || !o.equals(evo.getValue()))
//								cm.put(Integer.valueOf(evo.getIndexCode()),
//										evo.getValue());
						} catch (Exception e) {
							log.error("put data 2 cache failed!",e);
						}

					}
				}
	}

	private List<BaseVO> loadCapIndexDataFromDB(String tableName, Date stime,
			Date etime) {
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("tableName", tableName);
		m.put("stime", stime);
		m.put("etime", etime);
		List<BaseVO> retList = null;
		RequestMessage req = DAFFactory.buildRequest(
				"getCapindexPageData2CacheBytime", m, StockConstants.common);
		ResponseMessage resp = dbOpt.queryForList(req);
		if (resp.getRetrunCode().equals(BaseCodes.SUCCESS)) {
			retList = (List) resp.getResult();
		}
		return retList;
	}
	
	/**
	 * 查询打分记录的前前提是所有的能力评分都已全部计算过
	 * @return
	 */
	@Action(value = "/caprank/getScoreRecord")
	public String getScoreRecord() {
		String ptime = this.getHttpServletRequest().getParameter("time");
		String companycode = this.getHttpServletRequest().getParameter("companycode");
		String indexcode = this.getHttpServletRequest().getParameter("indexcode");
		String cis = CapUtil.getAllCaps();//
		if(StringUtil.isEmpty(ptime)||StringUtil.isEmpty(companycode)||!cis.contains(indexcode))
			return ERROR;
		String ret = ERROR;
		Date time = DateUtil.format(ptime);
		try {

			String scorelog = TaskEnter.getInstance().getScoreRecord(time, companycode, indexcode);
			output(scorelog);
		} catch (Exception e) {
			// TODO: handle exception
			log.error("compute index failed!", e);
		}
		return ret;
	}
	
	private void output(String responseText) {
		// 获取原始的PrintWriter对象,以便输出响应结果,而不用跳转到某个试图
		HttpServletResponse response = ServletActionContext.getResponse();
		// 设置字符集
		response.setContentType("text/plain");// 设置输出为文字流
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = null;
		try {
			out = response.getWriter();
			// 直接输出响应的内容
			out.println(responseText);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
}
