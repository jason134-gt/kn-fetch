package com.yfzx.service.spider;

import java.util.regex.Pattern;

import com.stock.common.model.share.Article;

public class SpiderStorageBean extends Article {

	private static final long serialVersionUID = -3916901005235917301L;
	private String key ;
	//A等级发私信，B等级发博文 ,默认是即发私信，又发行情
	private String putLevel = null;
	
	public String getKey() {		
		return key;
	}	
	public String getDataType() {
		return "SpiderStorageBean";
	}
	public void setKey(String key) {
		this.key = key;
	}
	
	/**
	 * A等级发私信，B等级发博文 ,默认是即发私信，又发行情,
	 * C等级，D等级都是自定义，增加的话，需要写对应的代码
	 * @return
	 */
	public String getPutLevel() {
		return putLevel;
	}
	/**
	 * A等级发私信，B等级发博文 ,默认是即发私信，又发行情
	 * @param putLevel
	 */
	public void setPutLevel(String putLevel) {
		this.putLevel = putLevel;
	}
	
	public void setContent(String content){
		String tmp = Pattern.compile("<script[^>]*?>[\\s\\S]*</script>",Pattern.CASE_INSENSITIVE).matcher(content).replaceAll("");
		//content.replaceAll("<script[^>]*?>.*?</script>", "");
//		String content2 = "1111111 <div class=\"inner_box\">\r\n<script>\r\nfunction a();</script>2222222222<script s='s'>function a();</script>33333333";
//		content2.split("<script[^>]*?>[\\s\\S]*</script>");
//		content2 = content2.replaceAll( "<script[^>]*?>[\\s\\S]*</script>", "");  
		super.setContent(tmp);
	}

	
}
