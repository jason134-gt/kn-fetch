package com.yz.stock.portal.action;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.Dictionary;
import com.stock.common.model.Indexgroup;
import com.stock.common.model.snn.EConst;
import com.stock.common.util.NetUtil;
import com.yfzx.service.db.CRuleService;
import com.yfzx.service.db.CompanyService;
import com.yfzx.service.db.DictService;
import com.yfzx.service.db.IndexGroupService;
import com.yz.mycore.core.count.CountManager;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.msg.ClientEventCenter;
import com.yz.mycore.msg.ServerEventCenter;
import com.yz.stock.common.BaseAction;
import com.yz.stock.monitor.RealMoniterHandle;
import com.yz.stock.portal.task.DataCenterTimer;

/**
 * 实时分析action
 * 
 * @author user
 * 
 */
public class ToolsAction extends BaseAction {

	Logger log = LoggerFactory.getLogger(this.getClass());
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	CompanyService cs = CompanyService.getInstance();
	DictService ds = DictService.getInstance();
	CRuleService crs = CRuleService.getInstance();

	// 实时分析
	@Action(value = "/tools/buildIndexGroup")
	public String buildIndexGroup() {

		try {
			Map<String,Indexgroup> m = new HashMap<String,Indexgroup>();
			List<Dictionary> dl = ds.getAllDictionaryList();
			for(Dictionary d : dl)
			{
				if(d.getType()==StockConstants.TABLE_TYPE_6)
				{
					String name = d.getColumnChiName();
					String indexcode = d.getIndexCode();
					if(name.contains("季度"))
					{
						String nname = name.replace("季度", "");
						Indexgroup ig = m.get(nname);
						if(ig==null)
						{
							ig = new Indexgroup();
							ig.setType(Integer.valueOf(StockConstants.INDEX_GROUP_TYPE_0));
							m.put(nname, ig);
						}
						String keys = ig.getIndexGroupKeys();
						keys += indexcode+"|";
						
						String desc = ig.getIndexGroupDesc();
						desc += 0+":"+indexcode+";";
						
						ig.setIndexGroupKeys(keys);
						ig.setIndexGroupDesc(desc);
					}
					if(name.contains("半年"))
					{
						String nname = name.replace("半年", "");
						Indexgroup ig = m.get(nname);
						if(ig==null)
						{
							ig = new Indexgroup();
							ig.setType(Integer.valueOf(StockConstants.INDEX_GROUP_TYPE_0));
							m.put(nname, ig);
						}
						String keys = ig.getIndexGroupKeys();
						keys += indexcode+"|";
						
						String desc = ig.getIndexGroupDesc();
						desc += 1+":"+indexcode+";";
						
						ig.setIndexGroupKeys(keys);
						ig.setIndexGroupDesc(desc);
					}
					if(name.contains("年化"))
					{
						String nname = name.replace("年化", "");
						Indexgroup ig = m.get(nname);
						if(ig==null)
						{
							ig = new Indexgroup();
							ig.setType(Integer.valueOf(StockConstants.INDEX_GROUP_TYPE_0));
							m.put(nname, ig);
						}
						String keys = ig.getIndexGroupKeys();
						keys += indexcode+"|";
						
						String desc = ig.getIndexGroupDesc();
						desc += 2+":"+indexcode+";";
						ig.setIndexGroupKeys(keys);
						ig.setIndexGroupDesc(desc);
					}
					
				}
			}
			
			Iterator<String> iter = m.keySet().iterator();
			while(iter.hasNext())
			{
				String key = iter.next();
				Indexgroup ig = m.get(key);
				
				IndexGroupService.getInstance().create(ig);
			}
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/tools/startDataComputeTimer")
	public String startDataComputeTimer() {

		try {
			DataCenterTimer.getInstance().datacompute();
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/tools/addClientDisruptorThreadByType")
	public String addClientDisruptorThreadByType() {

		try {
			int type = NetUtil.getParameterInt(getHttpServletRequest(), "htype",-1);
			int threadNum = NetUtil.getParameterInt(getHttpServletRequest(), "threadNum",1);
			if(type==-1)
				return ERROR;
			ClientEventCenter.getInstance().addDisruptorThreadType(type,threadNum);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/tools/resetDisruptorCount")
	public String resetDisruptorCount() {

		try {
			int type = NetUtil.getParameterInt(getHttpServletRequest(), "htype",-1);
			int cstype = NetUtil.getParameterInt(getHttpServletRequest(), "cstype",-1);
			if(type==-1||cstype==-1)
				return ERROR;
			if(cstype==0)
				ClientEventCenter.getInstance().resetDisruptorCount(type);
			else
				ServerEventCenter.getInstance().resetDisruptorCount(type);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/tools/addServerDisruptorThreadByType")
	public String addServerDisruptorThreadByType() {

		try {ClientEventCenter.registerHandle("RealMoniterHandleThread",
				Integer.valueOf(EConst.EVENT_5), new RealMoniterHandle(),
				32,0);
			int type = NetUtil.getParameterInt(getHttpServletRequest(), "htype",-1);
			int threadNum = NetUtil.getParameterInt(getHttpServletRequest(), "threadNum",1);
			if(type==-1)
				return ERROR;
			ServerEventCenter.getInstance().addDisruptorThreadType(type,threadNum);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	
	@Action(value = "/tools/CountManager/print")
	public String CountManager_print() {

		try {
			CountManager.print();
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}
	@Action(value = "/tools/CountManager/reset")
	public String CountManager_reset() {

		try {
			String keys = NetUtil.getParameterString(getHttpServletRequest(), "keys","");
			CountManager.reset(keys);
		} catch (Exception e) {
			log.error("execute /getDynamicIndex failed", e);
			setErrorReason("failed！");
			return ERROR;
		}
		return SUCCESS;
	}

}
