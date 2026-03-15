package com.yz.stock.snn.evolution;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.stock.common.model.snn.Gene;
import com.stock.common.model.snn.Statistics;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.StatisticsService;
import com.yz.stock.sevent.event.SEvent;
import com.yz.stock.snn.zinput.KnowledgeInput;

/**
 * 进化器
 * 
 * @author：杨真
 * @date：2014-4-7
 */
public class Evolutioner {

	static Evolutioner instance = new Evolutioner();
	static ConcurrentHashMap<String, Gene> gc = new ConcurrentHashMap<String, Gene>();// 基因集合
	static {
		init();
	}

	public Evolutioner() {

	}

	public static Evolutioner getInstance() {
		return instance;
	}

	// 初始化
	public static void init() {
		// 初始化第一代原始基因
		List<Gene> gl = KnowledgeInput.getInstance().getAllInitGene();
		if (gl != null) {
			for (Gene g : gl) {
				if (GeneFilter.isNormalGene(g)) {
					gc.put(g.getKey(), g);
				}
			}
		}
	}

	public void addANewGene(Gene g) {
		if(gc.get(g.getKey())==null)
			gc.put(g.getKey(), g);
	}

	public void removeANewGene(String key) {
		gc.remove(key);
	}

	public Gene getGene(String k) {
		// TODO Auto-generated method stub
		return gc.get(k);
	}

	public List<Gene> getGeneList()
	{
		List<Gene> gl = new ArrayList<Gene>();
		Iterator<String> iter = gc.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Gene g = gc.get(key);
			if (g != null)
				gl.add(g);
		}
		return gl;
	}
	public List<Gene> getOrinalGeneList()
	{
		List<Gene> gl = new ArrayList<Gene>();
		Iterator<String> iter = gc.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Gene g = gc.get(key);
			if (g != null&&g.getGeneration()==0)
				gl.add(g);
		}
		return gl;
	}
	public List<Gene> getEvolutiveGeneList()
	{
		List<Gene> gl = new ArrayList<Gene>();
		Iterator<String> iter = gc.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			Gene g = gc.get(key);
			if (g != null&&g.getGeneration()>0)
				gl.add(g);
		}
		return gl;
	}
	/**
	 * 取优良的基因
	 * @return
	 */
	public List<Gene> getGoodGeneList()
	{
		List<Gene> gl = new ArrayList<Gene>();
		List<Statistics> sl = StatisticsService.getInstance().queryAll();
		if(sl!=null)
		{
			for(Statistics st :sl)
			{
				Gene g = new Gene();
				g.setStatis(st);
				if (GeneFilter.isGoodGene(g))
					gl.add(g);
			}
		}
		return gl;
	}
	
	public void evolutive(List<SEvent> el) {
		
		for(int i=el.size()-1;i>=0;i--)
		{
			SEvent se=el.get(i);
			doEvolutive(se,el.subList(0, i));
		}
	}

	private void doEvolutive(SEvent se, List<SEvent> el) {
		for(int i=el.size()-1;i>=0;i--)
		{
//			if(tse.getG().getKey().equals(se.getG().getKey()))
//				continue;
			SEvent tse=el.get(i);
			SEvent nse = compose(se,tse);
			if(nse!=null)
				doEvolutive(nse,el.subList(0, i));
			
		}
		
	}
	//se事件发生在tse事件这后,时间顺序应为tstime-->tetime-->stime-->etime
	private SEvent compose(SEvent se, SEvent tse) {
		Date stime = se.getStime();
		Date etime = se.getEtime();
		Date tstime = tse.getStime();
		Date tetime = tse.getEtime();
		//时间跨度大于一年的不组合
		Date maxKd = StockUtil.getNextTime(etime, -3);
		//时间上有交集
		if(tetime.compareTo(stime)>=0&&maxKd.compareTo(tstime)<0)
		{
			String key="" ;
			//结束时间相同
			if(tetime.compareTo(etime)==0&&tstime.compareTo(stime)==0)
				key = composeKey(se.getG().getKey(),tse.getG().getKey(),0);
			if(stime.compareTo(tetime)>=0)
				key = composeKey(se.getG().getKey(),tse.getG().getKey(),1);
			if(StringUtil.isEmpty(key))
				return null;
			Gene ng = new Gene();
			ng.setKey(key);
			//取子代中高级代+1
			int generation = se.getG().getGeneration();
			if(generation<=tse.getG().getGeneration())
				generation=tse.getG().getGeneration()+1;
			ng.setGeneration(generation);
			//保存新基因
			addANewGene(ng);
			SEvent nse = new SEvent(ng,tstime,etime);
			nse.setSourceid(se.getSourceid());
			return nse;
		}
		return null;
	}

	private String composeKey(String key, String key2, int type) {
		if(type==0)
			return key+"^"+key2;
		return key+"~"+key2;
	}
}
