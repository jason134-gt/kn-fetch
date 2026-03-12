package com.yz.stock.portal.model;

import com.yz.common.vo.BaseVO;

/**
 * 对应<<行业分类表.xls>>
 * @author 唐斌奇
 * @since 2012-07-03
 */
public class CompanyIndustry extends BaseVO {
	
	private static final long serialVersionUID = 4184446571857590363L;
	/** 公司代码 */
	private String obOrgid0018;
	/** 股票代码 */
	private String obSeccode0007;	
	/** A股或B股 */
    private String f003v0007;
    
    /** 证劵交易所 行业代码 */
	private String industryCode;
	
	/** 该公司在证劵交易所的行业分类码 */
	private String f003v0018;
    
    /** 该公司在证劵交易所 一级行业 名称*/
    private String f004v0018;
    /** 该公司在证劵交易所 二级行业 名称*/
    private String f005v0018;
    /** 该公司在证劵交易所 三级行业 名称*/
    private String f006v0018;
    /** 该公司在证劵交易所 四级行业 名称*/
    private String f007v0018;

	@Override
	public String getKey() {
		// TODO Auto-generated method stub
		return this.obSeccode0007;
	}

	@Override
	public String getDataType() {
		// TODO Auto-generated method stub
		return "CompanyIndustry";
	}

	public String getObSeccode0007() {
		return obSeccode0007;
	}

	public void setObSeccode0007(String obSeccode0007) {
		this.obSeccode0007 = obSeccode0007;
	}

	public String getF003v0007() {
		return f003v0007;
	}

	public void setF003v0007(String f003v0007) {
		this.f003v0007 = f003v0007;
	}

	public String getIndustryCode() {
		return industryCode;
	}

	public void setIndustryCode(String industryCode) {
		this.industryCode = industryCode;
	}

	public String getF004v0018() {
		return f004v0018;
	}

	public void setF004v0018(String f004v0018) {
		this.f004v0018 = f004v0018;
	}

	public String getF005v0018() {
		return f005v0018;
	}

	public void setF005v0018(String f005v0018) {
		this.f005v0018 = f005v0018;
	}

	public String getF006v0018() {
		return f006v0018;
	}

	public void setF006v0018(String f006v0018) {
		this.f006v0018 = f006v0018;
	}

	public String getF007v0018() {
		return f007v0018;
	}

	public void setF007v0018(String f007v0018) {
		this.f007v0018 = f007v0018;
	}

	public String getObOrgid0018() {
		return obOrgid0018;
	}

	public void setObOrgid0018(String obOrgid0018) {
		this.obOrgid0018 = obOrgid0018;
	}

	public String getF003v0018() {
		return f003v0018;
	}

	public void setF003v0018(String f003v0018) {
		this.f003v0018 = f003v0018;
	}
	
	

}
