package com.yfzx.service.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stock.common.constants.SCache;
import com.stock.common.constants.StockCodes;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.StockSqlKey;
import com.stock.common.model.Dictionary;
import com.stock.common.model.Matchinfo;
import com.stock.common.util.DictUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.enter.LCEnter;


public class DictService {

	private static DictService instance = new DictService();
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private DictService()
	{
		
	}
	public static DictService getInstance()
	{
		return instance;
	}
	
	public List<Dictionary> getDataDictionaryList(String[] sa) {
		// TODO Auto-generated method stub
		List<Dictionary> ltm = new ArrayList<Dictionary>();

		for (String t : sa) {
			Dictionary tm = getDataDictionary(t);
			if (tm != null) {
				ltm.add(tm);
			}
		}

		return null;
	}

	public Dictionary getDataDictionaryDB(String indexCode) {
		// TODO Auto-generated method stub
		Dictionary d = new Dictionary(indexCode);
		String sqlMapKey = StockUtil.getBuildSqlMapKey(d,StockSqlKey.dictionary_key_0);
		RequestMessage req = DAFFactory.buildRequest(d.getIndexCode(),sqlMapKey,d,StockConstants.dictionary);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Dictionary) value;
	}
	
	public Dictionary getDataDictionary(String indexCode) {
		return getDataDictionaryFromCache(indexCode);
	}
	
	public Dictionary getDataDictionaryFromCache(String indexCode) {
		Object value = LCEnter.getInstance().get(indexCode, StockConstants.DICTIONARY_CACHE_NAME);
		if (value == null) {
			return null;
		}
		return (Dictionary) value;
	}
	public List<Dictionary> getDataListByCodeOrRemark(String indexCode,String remark) {
		// TODO Auto-generated method stub
		Dictionary d = new Dictionary();
		if(indexCode !=null) d.setIndexCode(indexCode);
		if(remark !=null) d.setRemark(remark);
		String sqlMapKey = StockUtil.getBuildSqlMapKey(d,StockSqlKey.dictionary_key_0);
		RequestMessage req = DAFFactory.buildRequest(d.getIndexCode(),sqlMapKey,d,StockConstants.dictionary);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Dictionary>) value;
	}
	
	public Dictionary getDataDictionaryNoCache(String indexCode) {
		// TODO Auto-generated method stub
		Dictionary d = new Dictionary(indexCode);
		String sqlMapKey = StockUtil.getBuildSqlMapKey(d,StockSqlKey.dictionary_key_0);
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey,d,StockConstants.dictionary);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Dictionary) value;
	}
	/**
	 * 根据报表编码查询
	 * @param tsc
	 * @return
	 */
	public List<Dictionary> getDictListByTableCode(String tc) {
		// TODO Auto-generated method stub  loaddictionary2cache
		RequestMessage req = DAFFactory.buildRequest(tc,"getDictListByTableSystemCode",StockConstants.dictionary);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Dictionary>) value;
	}
	/**
	 * 根据报表编码查询
	 * @param tsc
	 * @return
	 */
	public List<Dictionary> getDictListByTableCodeWithOutCache(String tc) {
		Dictionary d = new Dictionary();
		d.setTableCode(tc);
		RequestMessage req = DAFFactory.buildRequest("getDictListByTableSystemCode",d,StockConstants.dictionary);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Dictionary>) value;
	}
	
	public List<Dictionary> getDictListByType(int type) {
		Map<String,Integer> m = new HashMap<String,Integer>();
		m.put("type", type);
		RequestMessage req = DAFFactory.buildRequest("getDictListByType",m,StockConstants.dictionary);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Dictionary>) value;
	}
	
	/**
	 * 根据报表体系编码查询
	 * key :报表体系编码,或数据源编码+行业编码
	 * @param tsc
	 * @return
	 */
	public List<Dictionary> getDictListByTableSystemCode(String key) {
		List<Dictionary> dl = new ArrayList<Dictionary>();
		LCEnter lce = LCEnter.getInstance();
		List<Matchinfo> tcl = lce.get(key, StockConstants.MATCH_INFO_CACHE);
		if(tcl==null)
		{
			return null;
		}
		for(Matchinfo mi : tcl)
		{
			List<Dictionary> tdl = getDictListByTableCode(mi.getSystemChildTableCode());
			if(tdl!=null)
			{
				dl.addAll(tdl);
			}
			
		}
		return dl;
	}
	
	public List<Dictionary> getAllDictionaryListFromCache() {
		LCEnter lce = LCEnter.getInstance();
		return lce.get(SCache.CACHE_KEY_ALL_DICTIONARY, StockConstants.DICTIONARY_CACHE_NAME);
	}
	
	/**
	 * 根据报表体系编码查询
	 * key :报表体系编码,或数据源编码+行业编码
	 * @param tsc
	 * @return
	 */
	public List<Dictionary> getDictListByTableSystemCodeWithOutCache(String key) {
		List<Dictionary> dl = new ArrayList<Dictionary>();
		LCEnter lce = LCEnter.getInstance();
		List<Matchinfo> tcl = lce.get(key, StockConstants.MATCH_INFO_CACHE);
		if(tcl==null)
		{
			return null;
		}
		for(Matchinfo mi : tcl)
		{
			List<Dictionary> tdl = getDictListByTableCodeWithOutCache(mi.getSystemChildTableCode());
			if(tdl!=null)
			{
				dl.addAll(tdl);
			}
			
		}
		return dl;
	}
	
	@SuppressWarnings("rawtypes")
	public Integer getMaxIndexCode() {
		RequestMessage reqMsg = DAFFactory.buildRequest(StockSqlKey.dictionary_key_2,StockConstants.common);
		Object o = pLayerEnter.queryForObject(reqMsg);
		if(o==null)
		{
			return null;
		}
		Map m = (Map) o;
		Object mo = m.get("max(index_code)");
		if(mo!=null)
		{
			return Integer.valueOf(mo.toString());
		}
		return null;
	}
	public String create(Dictionary d) {
		Dictionary td = getDataDictionary(d.getIndexCode());
		if(td!=null)
		{
			td.setColumnChiName(d.getColumnChiName());
			return modify(td);
		}
		RequestMessage reqMsg = DAFFactory.buildRequest(d.getKey(),d.getClass().getName()+"."+StockSqlKey.dictionary_key_3,d,StockConstants.dictionary);
		return pLayerEnter.insert(reqMsg);
	}
	public String modify(Dictionary d) {
		Dictionary td = getDataDictionary(d.getIndexCode());
		if(td==null)
		{
			return StockCodes.FAILED;
		}
		RequestMessage reqMsg = DAFFactory.buildRequest(d.getClass().getName()+".modify",d,StockConstants.dictionary);
		return pLayerEnter.modify(reqMsg);
	}
	/**
	 * 修改公司的备注
	 * @param indexCode
	 * @param remark
	 * @return
	 */
	public String updateRemark(String indexCode,String remark) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("indexCode", indexCode);
		m.put("remark", remark);
		RequestMessage reqMsg = DAFFactory.buildRequest(new Dictionary().getClass().getName()+".updateRemark",m,StockConstants.dictionary);
		return pLayerEnter.modify(reqMsg);
		
	}
	public String updateTags(String indexCode,String tags) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("indexCode", indexCode);
		m.put("tags", tags);
		RequestMessage reqMsg = DAFFactory.buildRequest("updatetags",m,StockConstants.dictionary);
		return pLayerEnter.modify(reqMsg);
		
	}
	public List<Dictionary> getDictionaryListByTag(
			String indexcategorycode) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("indexcategorycode", indexcategorycode);
		RequestMessage req = DAFFactory.buildRequest("getDictionaryListByTag",m,StockConstants.dictionary);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return ((List<Dictionary>) value);
	}
	public List<Dictionary> getDictionaryListByTagfromCache(
			String tagcode) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("indexcategorycode", tagcode);
		RequestMessage req = DAFFactory.buildRequest(tagcode,"getDictionaryListByTag",m,StockConstants.dictionary);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return ((List<Dictionary>) value);
	}
	public String modifyUnit(String newindexs, String unit) {
		if(StringUtil.isEmpty(newindexs)||StringUtil.isEmpty(unit))
			return StockCodes.FAILED;
		String ret = StockCodes.SUCCESS;
		String[] indexcodes = newindexs.split(";");
		for(String indexcode:indexcodes)
		{
			if(!StringUtil.isEmpty(indexcode))
			{
				Dictionary d = getDataDictionaryNoCache(indexcode);
				if(d!=null)
				{
						ret = modifyUnitByIndexcode(d.getIndexCode(),unit);
					
				}
			}
			
		}
		return ret;
	}
	private String modifyUnitByIndexcode(String indexCode, String unit) {
		Map<String,String> m = new HashMap<String,String>();
		m.put("indexCode", indexCode);
		m.put("unit", unit);
		RequestMessage reqMsg = DAFFactory.buildRequest("modifyUnitByIndexcode",m,StockConstants.dictionary);
		return pLayerEnter.modify(reqMsg);
	}
	public List<Dictionary> getAllDictionaryList() {
		RequestMessage req = DAFFactory.buildRequest("loaddictionary2cache",StockConstants.dictionary);
		Object value = pLayerEnter.queryForList(req);
		if (value == null) {
			return null;
		}
		return (List<Dictionary>) value;
	}
	public String updateWeightCondition(String indexcode ,String wcexp) {
		if(StringUtil.isEmpty(wcexp)) return "";
		Map<String,String> m = new HashMap<String,String>();
		m.put("indexCode", indexcode);
		m.put("wcexp", wcexp);
		RequestMessage reqMsg = DAFFactory.buildRequest("updateWeightCondition",m,StockConstants.dictionary);
		return pLayerEnter.modify(reqMsg);
	}
	/*
	 * 把bit_set 的index位置的数修改为bit
	 */
	public void updateBitSet(Dictionary d, int index, String bit) {
		if(StringUtil.isEmpty(bit)) return ;
		Map<String,String> m = new HashMap<String,String>();
		m.put("indexCode", d.getIndexCode());
		String obit = d.getBitSet();
		String nbit = obit.substring(0,index)+bit+obit.substring(index+1,obit.length());
		m.put("nbit", nbit);
		RequestMessage reqMsg = DAFFactory.buildRequest("updateBitSet",m,StockConstants.dictionary);
		pLayerEnter.modify(reqMsg);
		
	}
	public String modifyBitSet(String newindexs, String indexof, String strue) {
		if(StringUtil.isEmpty(newindexs)||StringUtil.isEmpty(indexof)||StringUtil.isEmpty(strue))
			return StockCodes.FAILED;
		String ret = StockCodes.SUCCESS;
		String[] indexcodes = newindexs.split(";");
		for(String indexcode:indexcodes)
		{
			if(!StringUtil.isEmpty(indexcode))
			{
				Dictionary d = getDataDictionaryNoCache(indexcode);
				if(d!=null)
				{
						 updateBitSet(d,Integer.valueOf(indexof),strue);
					
				}
			}
			
		}
		return ret;
	}
	public boolean isNotSaveOrNotCompute(String indexcode) {
		Dictionary d = DictService.getInstance().getDataDictionary(indexcode);
		return DictUtil.isNotSave(d)||DictUtil.isNotComputeIndex(d);
	}
	public String deleteDictionayByCode(String indexCode) {
		Dictionary d = getDataDictionaryNoCache(indexCode);
		if(d==null) return StockCodes.SUCCESS;
		Map<String,String> m = new HashMap<String,String>();
		m.put("indexCode", indexCode);
		RequestMessage reqMsg = DAFFactory.buildRequest("deleteDictionayByCode",m,StockConstants.dictionary);
		return pLayerEnter.delete(reqMsg);
	}
	public List<Dictionary> getFCDictionary(String capTag) {
		List<Dictionary> dl = getFCDictionaryFromCache(capTag);
		if(dl==null)
		{
			dl = DictService.getInstance().getDictionaryListByTag(capTag);
		}
		return dl;
	}

	public List<Dictionary> getFCDictionaryFromCache(String capTag) {
		// TODO Auto-generated method stub
		return getDictionaryListByTagfromCache(capTag);
	}
	public boolean isBaseIndex(String indexcode) {
		Dictionary d = getDataDictionary(indexcode);
		return StockUtil.isBaseIndex(d.getType());
	}
	public void modifyDictName(String indexcode, String indexname) {
		Map<String,String> rm = new HashMap<String,String>();
		rm.put("indexcode", indexcode);
		rm.put("indexname", indexname);
		RequestMessage reqMsg = DAFFactory.buildRequest(
				"modifyDictName", rm, StockConstants.C_INDEX_RULE);
		pLayerEnter.modify(reqMsg);
		
	}
}
