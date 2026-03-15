package com.yz.stock.portal.action;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Dictionary;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.DictService;
import com.yz.stock.common.BaseAction;

public class DictionaryMngAction extends BaseAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9171952708481009032L;

	Logger log = LoggerFactory.getLogger(this.getClass());




	@Action(value = "/dictionaryMng/updateWeightCondition")
	public String updateWeightCondition() {
		try {
			String wcexp = this.getHttpServletRequest().getParameter("wcexp");
			String indexcode = this.getHttpServletRequest().getParameter("indexcode");
			if(StringUtil.isEmpty(indexcode))
				return ERROR;
			String ret = DictService.getInstance().updateWeightCondition(indexcode,wcexp);
			this.setResultData(ret);
		} catch (Exception e) {
			log.error("compute index failed!", e);
			return ERROR;
		}
		return SUCCESS;
	}

	@Action(value = "/dictionaryMng/updateMutIndexWeightCondition")
	public String updateMutIndexWeightCondition() {
		try {
			String ups = this.getHttpServletRequest().getParameter("ups");
			if(StringUtil.isEmpty(ups)) return ERROR;
			String[] upsa = ups.split(",");

			for(String up:upsa)
			{
				String[] pa = up.split("#");
				String indexcode = pa[0];
				String wexp = pa[1];
				String bit = pa[2];
				
				Dictionary d = DictService.getInstance().getDataDictionaryNoCache(indexcode);
				
				if(!d.getWeightExp().equals(wexp))
					DictService.getInstance().updateWeightCondition(indexcode,wexp);
				
				//修改能力评分位，是越大还是越小越好
				DictService.getInstance().updateBitSet(d,2,bit);
			}
			
		} catch (Exception e) {
			log.error("compute index failed!", e);
			return ERROR;
		}
		return SUCCESS;
	}

	@SuppressWarnings("rawtypes")
	@Action(value = "/dict/getIndexsOfOneTags")
	public String getIndexsOfOneTags() {
		try {
			String tagcode = this.getHttpServletRequest().getParameter("tagcode");
			if(StringUtil.isEmpty(tagcode))
				return ERROR;
			List<Dictionary> dl = DictService.getInstance().getDictionaryListByTag(tagcode);
			output(JSONUtil.serialize(dl));
		} catch (Exception e) {
			// TODO: handle exception
			log.error("opt failed!", e);
			return ERROR;
		}
		return SUCCESS;
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
