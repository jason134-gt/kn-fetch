package com.yz.stock.portal.service.company.spider;

public class UrlDataHanding implements Runnable {
	/**
	 *  下载对应页面并分析出页面对应的URL放在未访问队列中。 
	 *  @param url 
	 */
	public void dataHanding(String url) {
//		HrefOfPage.getHrefOfContent(DownloadPage.getContentFormUrl(url)); //页面找链接，进行往下层次的链接抓取，去万点此功能不需要
	}

	public void run() {
		while (!UrlQueue.isEmpty()) {
			dataHanding(UrlQueue.outElem());
		}
	}
}
