package com.yfzx.service.spider;

import java.util.Map;

public class SpiderConfigBean {
	
	private String key;
	private String type;//json or html
	private String startType;//startUrl是否跟公司code拼接,默认是0,=1时，URL跟公司code相关拼接
	private String startUrl;//起始
	private String cookie = "";//AJAX方式需要登录	
	private String childLevel;//默认1层，可以2层	
	private String selectUrl;//第2层是需要
	private String urlRegex;//非必填	
	private Map<String,String> valueMap;//值映射关系 对应Article对象的字段
	private String encode;//utf-8 gbk等
	private String desc;
	//任务规则
	private String cron;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStartType() {
		return startType;
	}

	public void setStartType(String startType) {
		this.startType = startType;
	}

	public String getStartUrl() {
		return startUrl;
	}

	public void setStartUrl(String startUrl) {		
		this.startUrl = startUrl;
	}

	public String getCookie() {
		return cookie;
	}

	public void setCookie(String cookie) {
		this.cookie = cookie;
	}

	public String getChildLevel() {
		return childLevel;
	}

	public void setChildLevel(String childLevel) {
		this.childLevel = childLevel;
	}

	public String getSelectUrl() {
		return selectUrl;
	}

	public void setSelectUrl(String selectUrl) {
		this.selectUrl = selectUrl;
	}

	public String getUrlRegex() {
		return urlRegex;
	}

	public void setUrlRegex(String urlRegex) {
		this.urlRegex = urlRegex;
	}

	public Map<String, String> getValueMap() {
		return valueMap;
	}

	public void setValueMap(Map<String, String> valueMap) {
		this.valueMap = valueMap;
	}

	public String getEncode() {
		return encode;
	}

	public void setEncode(String encode) {
		this.encode = encode;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	
	
}
