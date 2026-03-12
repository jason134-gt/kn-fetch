package com.yz.stock.snn.zinput;

import java.util.ArrayList;
import java.util.List;

import com.stock.common.model.Tagrule;
import com.stock.common.model.snn.Gene;
import com.stock.common.model.snn.SnnConst;
import com.yfzx.service.db.TagruleService;

public class KnowledgeInput {

	static KnowledgeInput instance = new KnowledgeInput();

	public KnowledgeInput() {

	}

	public static KnowledgeInput getInstance() {
		return instance;
	}

	public List<Gene> getAllInitGene() {
		List<Gene> gl = new ArrayList<Gene>();
		// 初始化第一代原始基因
		List<Tagrule> tr = TagruleService.getInstance().queryAllTagrules();
		if (tr != null) {
			for (Tagrule r : tr) {
				Gene g = new Gene(String.valueOf(r.getId()), r, SnnConst.generation_0,r.getTsc());
				gl.add(g);
			}
		}
		return gl;

	}
}
