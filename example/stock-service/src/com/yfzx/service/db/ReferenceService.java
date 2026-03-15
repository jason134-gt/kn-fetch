package com.yfzx.service.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stock.common.constants.StockConstants;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Indexgroup;
import com.stock.common.model.Indexreference;
import com.stock.common.model.Industry;
import com.stock.common.util.CKeyUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.stock.common.util.TreeNode;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;



public class ReferenceService {

	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private static ReferenceService instance = new ReferenceService();
	private ReferenceService()
	{
		
	}
	
	public static ReferenceService getInstance() {
		// TODO Auto-generated method stub
		return instance;
	}

	public Indexgroup getIndexGroupByKey(String indexCode, String grouptype) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("indexCode", indexCode);
		m.put("type", grouptype);
		String key = grouptype+"_"+indexCode;
		RequestMessage req = DAFFactory.buildRequest(key,"getIndexGroupByKey",m,StockConstants.DATA_TYPE_indexgroup);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Indexgroup) value;
	}

	/**
	 * 
	 * @param indexcode
	 * @param industrycode
	 * @param industryName
	 * @param rlow
	 * @param rhigh
	 * @param desc 
	 * @param mchild：是否修改子行业的参考值
	 */
	public void saveIndexReference(String indexcode, String industrycode,
			String industryName, String rlow, String rhigh,boolean mchild, String desc) {
		Indexreference inf = getIndexreference(indexcode,industrycode);
				
		if(inf==null)
			createIndexReference(indexcode, industrycode,
					 industryName, rlow, rhigh,desc);
		else if(mchild)
			modifyIndexReference(indexcode, industrycode,
					 industryName, rlow, rhigh,desc);
	}

	private void modifyIndexReference(String indexcode, String industrycode,
			String industryName, String rlow, String rhigh, String desc) {
		if(StringUtil.isEmpty(rhigh)&&StringUtil.isEmpty(rlow)) 
			return;
		Indexreference inf = new Indexreference();
		inf.setIndexCode(indexcode);
		inf.setIndustryCode(industrycode);
		inf.setIndustryName(industryName);
		if(!StringUtil.isEmpty(desc)) inf.setDesc(desc);
		if(!StringUtil.isEmpty(rhigh)) inf.setRhigh(Double.valueOf(rhigh));
		if(!StringUtil.isEmpty(rlow)) inf.setRlow(Double.valueOf(rlow));

		RequestMessage req = DAFFactory.buildRequest("com.stock.common.model.Indexreference.update",inf,StockConstants.common);
	    pLayerEnter.modify(req);
		
		
	}

	private void createIndexReference(String indexcode, String industrycode,
			String industryName, String rlow, String rhigh, String desc) {
		if(StringUtil.isEmpty(rhigh)&&StringUtil.isEmpty(rlow)) 
			return;
		Indexreference inf = new Indexreference();
		inf.setIndexCode(indexcode);
		inf.setIndustryCode(industrycode);
		inf.setIndustryName(industryName);
		if(!StringUtil.isEmpty(desc)) inf.setDesc(desc);
		if(!StringUtil.isEmpty(rhigh)) inf.setRhigh(Double.valueOf(rhigh));
		if(!StringUtil.isEmpty(rlow)) inf.setRlow(Double.valueOf(rlow));

		RequestMessage req = DAFFactory.buildRequest("com.stock.common.model.Indexreference.insert",inf,StockConstants.common);
	    pLayerEnter.modify(req);
		
	}

	private Indexreference getIndexreference(String indexcode,
			String industrycode) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("indexcode", indexcode);
		m.put("industrycode", industrycode);
		String key = CKeyUtil.getIndexreferenceCKey(industrycode,indexcode);
		RequestMessage req = DAFFactory.buildRequest(key,"getIndexreference",m,StockConstants.DATA_TYPE_indexreference);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Indexreference) value;
	}

	/**
	 * 逐层查找参考值，直到根结点
	 * @param indexcode
	 * @param industrycode
	 * @return
	 */
	public Indexreference getreferenceStepByStep(String indexcode,
			String industrycode) {
		Indexreference inf = getIndexreference(indexcode,industrycode);
		if(inf==null)
		{
			TreeNode industry = IndustryService.getInstance().getIndustryCSRCTreeFromCache(0).get(industrycode);
			if(industry==null) return null;
			TreeNode parent = industry.getParent();
			if(parent==null) return null;
			Object o = parent.getReference();
			if(o==null) return null;
			Industry pind = (Industry) o;
			inf = getreferenceStepByStep(indexcode, pind.getIndustryCode());
		}
		
		return inf;
	}
	
	public List<Indexreference> getIndexReferenceList(Indexreference inf) {

		RequestMessage req = DAFFactory.buildRequest("com.stock.common.model.Indexreference.select",inf,StockConstants.common);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Indexreference>) value;
	}

	public Indexreference getIndexReference(IndexMessage im) {
		Indexreference irf = null;
		if(IndexService.isCompanyMsg(im))
		{
			irf = getIndexReferenceByCompanycode(im);
		}
		else
		{
			irf = getIndexReferenceByIndustryName(im);
		}
		return irf;
	}

	private Indexreference getIndexReferenceByIndustryName(IndexMessage im) {
		Industry ind = IndustryService.getInstance().getIndustryCSRCByName(im.getUidentify());
		return getreferenceStepByStep(im.getIndexCode(), ind.getIndustryCode());
	}

	private Indexreference getIndexReferenceByCompanycode(IndexMessage im) {
		Industry ind = IndustryService.getInstance().getIndustryByCompanycode(im.getCompanyCode());
		return getreferenceStepByStep(im.getIndexCode(), ind.getIndustryCode());
	}

	

}
