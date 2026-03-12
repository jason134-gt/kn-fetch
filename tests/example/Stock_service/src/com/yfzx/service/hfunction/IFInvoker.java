package com.yfzx.service.hfunction;

import java.util.List;

import com.stock.common.model.IndexMessage;

public class IFInvoker {

	IFService ifs ;
	IndexMessage req;
	List<String> vls;
	
	
	public IFInvoker(IFService ifs, IndexMessage req, List<String> vls) {
		super();
		this.ifs = ifs;
		this.req = req;
		this.vls = vls;
	}


	public Double invoke()
	{
		return ifs.doInvoke(req, vls);
	}


	public IndexMessage getReq() {
		return req;
	}


	public void setReq(IndexMessage req) {
		this.req = req;
	}


	public IFService getIfs() {
		return ifs;
	}


	public void setIfs(IFService ifs) {
		this.ifs = ifs;
	}


	public List<String> getVls() {
		return vls;
	}


	public void setVls(List<String> vls) {
		this.vls = vls;
	}
	
	
}
