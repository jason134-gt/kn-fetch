package com.yz.stock.portal.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockCodes;
import com.stock.common.model.RankResultItem;
import com.stock.common.model.Ranking;
import com.stock.common.util.DateUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.RankResultService;
import com.yfzx.service.db.RankingService;
import com.yz.stock.common.BaseAction;
import com.yz.stock.portal.manager.ComputeIndexManager;
import com.yz.stock.portal.manager.StockFactory;
import com.yz.stock.portal.task.computeRankTask;

public class RankingAction extends BaseAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2149622762066330399L;
	RankingService rs = RankingService.getInstance();
	private Ranking rank = new Ranking();
	private Logger log = LoggerFactory.getLogger(this.getClass());
	@Action(value = "/ranking/saveRanking")
	public String saveRanking() {	
		try {
			String ret = rs.saveRanking(rank);
			if(!ret.equals(StockCodes.SUCCESS))
			{
				return ERROR;
			}
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	@Action(value = "/ranking/modifyRanking")
	public String modifyRanking() {	
		try {
			String ret = rs.modifyRanking(rank);
			if(!ret.equals(StockCodes.SUCCESS))
			{
				return ERROR;
			}
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	@Action(value = "/ranking/queryRankingByCode")
	public String queryRankingByCode() {	
		try {

			Ranking dbrank = rs.queryRankingByCode(rank.getRankingCode());
			if(dbrank!=null)
			{
				this.setResultData(dbrank);
			}
			
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/ranking/queryRankingByName")
	public String queryRankingByName() {	
		try {

			Ranking dbrank = rs.queryRankByName(rank.getRankingName());
			if(dbrank!=null)
			{
				this.setResultData(dbrank);
			}
			
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/ranking/queryAllRanking")
	public String queryAllRanking() {	
		try {

			List<Ranking> rl = rs.queryAllRanking();
			if(rl!=null)
			{
				this.setResultData(rl);
			}
			
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/ranking/delRankingBycode")
	public String delRankingBycode() {	
		try {
			if(rank.getRankingCode()!=null)
			{
				 rs.delRankingBycode(rank.getRankingCode());
			}
		
			
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	@Action(value = "/ranking/queryRankingByCondition")
	public String queryRankingByCondition() {	
		try {
			if(StringUtil.isEmpty(rank.getRankingName()))
			{
				List<Ranking> rl = rs.queryAllRanking();
				if(rl!=null)
				{
					this.setResultData(rl);
				}
			}
			else
			{
				Ranking dbrank = rs.queryRankByName(rank.getRankingName());
				if(dbrank!=null)
				{
					List<Ranking> rl = new ArrayList<Ranking>();
					rl.add(dbrank);
					this.setResultData(rl);
				}
			}
			
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	
	
	@Action(value = "/ranking/computeRankresult")
	public String computeRankresult() {	
		try {
			String rankCode = this.getHttpServletRequest().getParameter("rankCode");
			String rankPeriod = this.getHttpServletRequest().getParameter("rankPeriod");
			//加载基础表的数据
			ComputeIndexManager.getInstance().computeInit();
			Date time = DateUtil.format(rankPeriod);
			if(!StringUtil.isEmpty(rankCode))
			{
				//计算所有公司的某个榜单
			
				RankResultService.getInstance().computeOneRankresultAndSave(Integer.valueOf(rankCode),time);
			}
			else
			{
				//计算公司的所有榜单
				List<Ranking> rl = rs.queryAllRanking();
				for(Ranking r : rl)
				{
					StockFactory.getThreadPool().submit(new computeRankTask(r.getRankingCode(),time));
				}
			}
			
			
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/ranking/queryRankByTableSystemCode")
	public String queryRankByTableSystemCode() {	
		try {
			String tsc = this.getHttpServletRequest().getParameter("tsc");
			List<Ranking> rl = RankingService.getInstance().queryRankingByTableSystemCode(tsc);
			this.setResultData(rl);
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	
	@Action(value = "/ranking/queryCompanyListByRankcode")
	public String queryCompanyListByRankcode() {	
		try {
			String rankcode = this.getHttpServletRequest().getParameter("rankcode");
			String rankPeriod = this.getHttpServletRequest().getParameter("rankPeriod");
			if(StringUtil.isEmpty(rankcode)) return ERROR;
			List<RankResultItem> cl = RankResultService.getInstance().getListCompanys(Integer.valueOf(rankcode),rankPeriod);
			this.setResultData(cl);
		} catch (Exception e) {
			log.error("execute /cfirule/getCfiruleList failed", e);
			setErrorReason("add failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	
	public Ranking getRank() {
		return rank;
	}
	public void setRank(Ranking rank) {
		this.rank = rank;
	}

}
