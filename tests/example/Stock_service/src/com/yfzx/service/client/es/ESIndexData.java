package com.yfzx.service.client.es;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.Topic;
import com.stock.common.model.USubject;
import com.stock.common.model.share.Article;
import com.stock.common.model.user.Members;
import com.stock.common.util.CookieUtil;
import com.stock.common.util.StockUtil;

/**
 * 索引数据
 */
public class ESIndexData {

	private static final String simple = "yyyy-MM-dd HH:mm:ss";

	private static SimpleDateFormat sdf = new SimpleDateFormat(simple);

	private static Logger logger = LoggerFactory.getLogger(ESIndexData.class);

	private static ESIndexData instance = new ESIndexData();


	private ESIndexData() {}

	public static ESIndexData getInstance() {
		return instance;
	}

	public boolean crawlArticleIndex(List<Article> articleList) {
		if(articleList == null || articleList.size() == 0) {
			return false;
		}
		if(! ESClient.isExist(ESClient.getInstance().getClient(), "crawl_articles")) {
			return false;
		}

		boolean result = true;
		BulkRequestBuilder bulkRequest = ESClient.getInstance().getClient().prepareBulk();
		try {
			for(Article article : articleList) {
				if(article != null && article.getStatus() == 1) {
					String title = article.getTitle();
					String content = article.getContent();

					XContentBuilder builder = XContentFactory.jsonBuilder().startObject().field("uuid", article.getUuid());
					if(StringUtils.isNotBlank(title)) {
						builder.field("title", StockUtil.removeHtmlTag(StringEscapeUtils.unescapeHtml(title)));
					}
					if(StringUtils.isNotBlank(content)) {
						builder.field("content", StockUtil.removeHtmlTag(StringEscapeUtils.unescapeHtml(content)));
					}
					builder.endObject();

					bulkRequest.add(ESClient.getInstance().getClient().prepareIndex("crawl_articles", "crawl_article", article.getUuid()).setSource(builder));
				}
			}

			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			if (bulkResponse.hasFailures()) {
				result = false;
				logger.error("StockPortalBatchIndexFailure "  + bulkResponse.buildFailureMessage() + "   ==>" + sdf.format(new Date()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public boolean articleIndex(List<Article> articleList) {
		if(articleList == null || articleList.size() == 0) {
			return false;
		}
		if(! ESClient.isExist(ESClient.getInstance().getClient(), "articles")) {
			return false;
		}

		boolean result = true;
		BulkRequestBuilder bulkRequest = ESClient.getInstance().getClient().prepareBulk();
		try {
			for(Article article : articleList) {
				if(article != null && article.getStatus() == 1) {
					String tags = article.getTags();
					String title = article.getTitle();
					String content = article.getContent();

					XContentBuilder builder = XContentFactory.jsonBuilder().startObject().field("uuid", article.getUuid());
					if(StringUtils.isNotBlank(tags)) {
						builder.field("tags", StockUtil.removeHtmlTag(StringEscapeUtils.unescapeHtml(tags)));
					}
					if(StringUtils.isNotBlank(title)) {
						builder.field("title", StockUtil.removeHtmlTag(StringEscapeUtils.unescapeHtml(title)));
					}
					if(StringUtils.isNotBlank(content)) {
						builder.field("content", StockUtil.removeHtmlTag(StringEscapeUtils.unescapeHtml(content)));
					}
					builder.endObject();

					bulkRequest.add(ESClient.getInstance().getClient().prepareIndex("articles", "article", article.getUuid()).setSource(builder));
				}
			}

			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			if (bulkResponse.hasFailures()) {
				result = false;
				logger.error("StockPortalBatchIndexFailure "  + bulkResponse.buildFailureMessage() + "   ==>" + sdf.format(new Date()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public void articleIndex(Map<String, String> map) {
		if(map == null || map.size() == 0) {
			return ;
		}
		if(! ESClient.isExist(ESClient.getInstance().getClient(), "articles")) {
			return ;
		}
		try {
			XContentBuilder builder = XContentFactory.jsonBuilder()
					.startObject().field("uuid", map.get("uuid"));
			if (StringUtils.isNotBlank(map.get("tags"))) {
				builder.field("tags", StockUtil
						.removeHtmlTag(StringEscapeUtils
								.unescapeHtml(map.get("tags"))));
			}
			if (StringUtils.isNotBlank(map.get("title"))) {
				builder.field("title", StockUtil
						.removeHtmlTag(StringEscapeUtils
								.unescapeHtml(map.get("title"))));
			}
			if (StringUtils.isNotBlank(map.get("content"))) {
				builder.field("content", StockUtil
						.removeHtmlTag(StringEscapeUtils
								.unescapeHtml(map.get("content"))));
			}
			builder.endObject();

			ESClient.getInstance().getClient().prepareIndex("articles", "article", map.get("uuid"))
					.setSource(builder).execute().actionGet();
		} catch (ElasticsearchException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void updateArticleIndex(Map<String, String> map, String uuid) {
		if(map == null || map.size() == 0 || StringUtils.isBlank(uuid)) {
			return ;
		}
		if(! ESClient.isExist(ESClient.getInstance().getClient(), "articles")) {
			return ;
		}
		Map<String, Object> doc = new HashMap<String, Object>();
		if (StringUtils.isNotBlank(map.get("title"))) {
			doc.put("title", StockUtil
					.removeHtmlTag(StringEscapeUtils
							.unescapeHtml(map.get("title"))));
		}
		if (StringUtils.isNotBlank(map.get("content"))) {
			doc.put("content", StockUtil
					.removeHtmlTag(StringEscapeUtils
							.unescapeHtml(map.get("content"))));
		}
		ESClient.getInstance().getClient().prepareUpdate("articles", "article", uuid)
				.setDoc(doc).execute().actionGet();
	}

	public boolean isArticleExistInEs(String uuid) {
		GetResponse response = ESClient.getInstance().getClient().prepareGet("articles", "article", uuid).execute().actionGet();
		Map<String, Object> map = response.getSourceAsMap();
		if(map == null || map.size() == 0) {
			return false;
		}
		return true;
	}

	public void deleteArticleIndex(String uuid) {
		if(StringUtils.isBlank(uuid)) {
			return;
		}
		Client client = ESClient.getInstance().getClient();
		if(! ESClient.isExist(client, "articles")) {
			return ;
		}
		client.prepareDelete("articles", "article", uuid);
		ESClient.getInstance().getClient().prepareDelete("articles", "article", uuid).execute().actionGet();
	}

	public boolean membersIndex(List<Map<String, Object>> dataList) {
		if(dataList == null || dataList.size() == 0) {
			return false;
		}
		if(! ESClient.isExist(ESClient.getInstance().getClient(), "members")) {
			return false;
		}
		boolean result = true;
		try {
			BulkRequestBuilder bulkRequest = ESClient.getInstance().getClient().prepareBulk();
			for(Map<String, Object> members : dataList) {
				if(members == null || members.size() == 0) {
					continue;
				}
				if(members.get("uid") == null || (Long)members.get("uid") == 0L) {
					continue;
				}
				Long uid = (Long)members.get("uid");
				XContentBuilder builder = XContentFactory.jsonBuilder().startObject().field("uid", uid.toString());
				builder.field("nickname",  members.get("nickname") == null ? "" : (String)members.get("nickname"));
				builder.field("pinyin", members.get("pinyin") == null ? "" : (String)members.get("pinyin"));
				builder.field("shortPinyin", members.get("shortPinyin") == null ? "" : (String)members.get("shortPinyin"));
				builder.field("desc", members.get("desc") == null ? "" : (String)members.get("desc"));
				builder.field("tag", members.get("tag") == null ? "" : (String)members.get("tag"));
				builder.field("country", members.get("country") == null ? "" : (String)members.get("country"));
				builder.field("province", members.get("province") == null ? "" : (String)members.get("province"));
				builder.field("city", members.get("city") == null ? "" : (String)members.get("city"));
			    builder.field("area", members.get("area") == null ? "" : (String)members.get("area"));
			    builder.field("street", members.get("street") == null ? "" : (String)members.get("street"));
				builder.endObject();

				bulkRequest.add(ESClient.getInstance().getClient().prepareIndex("members", "member", uid.toString()).setSource(builder));
			}

			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			if (bulkResponse.hasFailures()) {
				result = false;
				logger.error("MembersBatchIndexFailure " + bulkResponse.buildFailureMessage() + "   ==>" + sdf.format(new Date()));
			}
		} catch (ElasticsearchException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public void memberIndex(Members members) {
		if(members == null) {
			return ;
		}
		try {
				if(! ESClient.isExist(ESClient.getInstance().getClient(), "members")) {
					return ;
				}

				XContentBuilder builder = XContentFactory.jsonBuilder().startObject().field("uid", members.getUid().toString());
				builder.field("nickname", members.getNickname() == null ? "" : members.getNickname());
				builder.field("country", members.getCountry() == null ? "" : members.getCountry());
				builder.field("province", members.getProvince() == null ? "" :  members.getProvince());
				builder.field("city", members.getCity() == null ? "" : members.getCity());
				builder.field("area", members.getArea() == null ? "" : members.getArea());
				builder.field("street", members.getStreet() == null ? "" : members.getStreet());
				builder.field("tag", members.getTag() == null ? "" : members.getTag());
				builder.field("desc", members.getDesc() == null ? "" : members.getDesc());
				builder.endObject();

				ESClient.getInstance().getClient().prepareIndex("members", "member", members.getUid().toString()).setSource(builder).execute().actionGet();
		} catch (ElasticsearchException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void updateMemberIndex(Members members) {
		if(members == null) {
			return ;
		}
		try {
			if(! ESClient.isExist(ESClient.getInstance().getClient(), "members")) {
				return ;
			}
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("nickname", members.getNickname() == null ? "" : members.getNickname());
			map.put("country", members.getCountry() == null ? "" : members.getCountry());
			map.put("province", members.getProvince() == null ? "" :  members.getProvince());
			map.put("city", members.getCity() == null ? "" : members.getCity());
			map.put("area", members.getArea() == null ? "" : members.getArea());
			map.put("street", members.getStreet() == null ? "" : members.getStreet());
			map.put("tag", members.getTag() == null ? "" : members.getTag());
			map.put("desc", members.getDesc() == null ? "" : members.getDesc());
			ESClient.getInstance().getClient().prepareUpdate("members", "member", members.getUid().toString()).setDoc(map).execute().actionGet();
		} catch (ElasticsearchException e) {
			e.printStackTrace();
		}
	}

	public boolean usubjectIndex(List<USubject> usubjectList, int type) {
		boolean result = true;
		String  indexName = "";
		if(type == 0) {
			indexName = "stocks";
		} else if(type == 1) {
			indexName = "industries";
		}
		if(! ESClient.isExist(ESClient.getInstance().getClient(), indexName)) {
			return false;
		}
		BulkRequestBuilder bulkRequest = ESClient.getInstance().getClient().prepareBulk();
		try {
			for(USubject usubject : usubjectList) {
				//类型(公司:0，行业:1，板块:2，指数:3，服务号:4)
				XContentBuilder builder = XContentFactory.jsonBuilder().startObject().field("id", usubject.getId());
				if(StringUtils.isNotBlank(usubject.getUidentify())) {
					builder.field("uidentify", usubject.getUidentify());
				}
				if(StringUtils.isNotBlank(usubject.getName())) {
				    builder.field("name", usubject.getName());
				}
				if(StringUtils.isNotBlank(usubject.getPinyin())) {
					builder.field("pinyin", usubject.getPinyin());
				}
				if(StringUtils.isNotBlank(usubject.getShortPinyin())) {
					builder.field("shortPinyin", usubject.getShortPinyin());
				}
				if(type == 0) {
					builder.field("stockType", StockUtil.checkStockcode(usubject.getUidentify()));
				}
				builder.endObject();
				bulkRequest.add(ESClient.getInstance().getClient().prepareIndex(getUSubjectIndexName(type), getUSubjectTypeName(type), String.valueOf(usubject.getId())).setSource(builder));
			}

			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			if (bulkResponse.hasFailures()) {
				result = false;
				logger.error("USubjectBatchIndexFailure indexName is : " + indexName + " :   " + bulkResponse.buildFailureMessage() + "   ==>" +  sdf.format(new Date()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public boolean topicsIndex(List<Topic> topicList) {
		if(topicList == null || topicList.size() == 0) {
			return false;
		}
		if(! ESClient.isExist(ESClient.getInstance().getClient(), "topics")) {
			return false;
		}
		boolean result = true;
		BulkRequestBuilder bulkRequest = ESClient.getInstance().getClient().prepareBulk();
		try {
			for(Topic topic : topicList) {
				//类型(公司:0，行业:1，板块:2，指数:3，服务号:4)
				String id = CookieUtil.getMD5(topic.getUidentify());

				XContentBuilder builder = XContentFactory.jsonBuilder().startObject().field("id", id);
				builder.field("uidentify", topic.getUidentify());
				if(StringUtils.isNotBlank(topic.getLead())) {
				    builder.field("lead", topic.getLead());
				}
				if(StringUtils.isNotBlank(topic.getLeadimgurl())) {
					builder.field("leadImgUrl", topic.getLeadimgurl());
				}
				if(StringUtils.isNotBlank(topic.getTips())) {
					builder.field("tips", topic.getTips());
				}
				builder.field("topicType", 1);
				builder.endObject();
				bulkRequest.add(ESClient.getInstance().getClient().prepareIndex("topics", "topic", id).setSource(builder));
			}

			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			if (bulkResponse.hasFailures()) {
				result = false;
				logger.error("TopicBatchIndexFailure "  + bulkResponse.buildFailureMessage() + "   ==>" +  sdf.format(new Date()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public boolean stockTopicsIndex(List<USubject> usubjectList) {
		if(usubjectList == null || usubjectList.size() == 0) {
			return false;
		}
		if(! ESClient.isExist(ESClient.getInstance().getClient(), "topics")) {
			return false;
		}
		boolean result = true;
		BulkRequestBuilder bulkRequest = ESClient.getInstance().getClient().prepareBulk();
		try {
			for(USubject usubject : usubjectList) {
				//类型(公司:0，行业:1，板块:2，指数:3，服务号:4)
				String id = CookieUtil.getMD5(usubject.getUidentify());

				XContentBuilder builder = XContentFactory.jsonBuilder().startObject().field("id", id);
				builder.field("uidentify", usubject.getUidentify());

				if(StringUtils.isNotBlank(usubject.getName())) {
				    builder.field("name", usubject.getName());
				}
				if(StringUtils.isNotBlank(usubject.getPinyin())) {
					builder.field("pinyin", usubject.getPinyin());
				}
				if(StringUtils.isNotBlank(usubject.getShortPinyin())) {
					builder.field("shortPinyin", usubject.getShortPinyin());
				}
				builder.field("topicType", 2);
				builder.endObject();
				bulkRequest.add(ESClient.getInstance().getClient().prepareIndex("topics", "topic", id).setSource(builder));
			}

			BulkResponse bulkResponse = bulkRequest.execute().actionGet();
			if (bulkResponse.hasFailures()) {
				result = false;
				logger.error("StockTopicBatchIndexFailure "  + bulkResponse.buildFailureMessage() + "   ==>" +  sdf.format(new Date()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public void topicIndex(Topic topic) {
		if(topic == null) {
			return ;
		}
		if(! ESClient.isExist(ESClient.getInstance().getClient(), "topics")) {
			return ;
		}
		try {
			String id = CookieUtil.getMD5(topic.getUidentify());
			XContentBuilder builder = XContentFactory.jsonBuilder().startObject().field("id", id);
			builder.field("uidentify", topic.getUidentify());
			if(StringUtils.isNotBlank(topic.getLead())) {
			    builder.field("lead", topic.getLead());
			}
			if(StringUtils.isNotBlank(topic.getLeadimgurl())) {
				builder.field("leadImgUrl", topic.getLeadimgurl());
			}
			if(StringUtils.isNotBlank(topic.getTips())) {
				builder.field("tips", topic.getTips());
			}
			builder.field("topicType", 1);
			builder.endObject();

			ESClient.getInstance().getClient().prepareIndex("topics", "topic", id).setSource(builder).execute().actionGet();
		} catch (ElasticsearchException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void stockTopicIndex(USubject usubject) {
		if(usubject == null) {
			return ;
		}
		if(! ESClient.isExist(ESClient.getInstance().getClient(), "topics")) {
			return ;
		}
		try {
			String id = CookieUtil.getMD5(usubject.getUidentify());
			XContentBuilder builder = XContentFactory.jsonBuilder().startObject().field("id", id);
			builder.field("uidentify", usubject.getUidentify());
			if(StringUtils.isNotBlank(usubject.getName())) {
			    builder.field("name", usubject.getName());
			}
			if(StringUtils.isNotBlank(usubject.getPinyin())) {
				builder.field("pinyin", usubject.getPinyin());
			}
			if(StringUtils.isNotBlank(usubject.getShortPinyin())) {
				builder.field("shortPinyin", usubject.getShortPinyin());
			}
			builder.field("topicType", 2);
			builder.endObject();

			ESClient.getInstance().getClient().prepareIndex("topics", "topic", id).setSource(builder).execute().actionGet();
		} catch (ElasticsearchException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void updateTopicIndex(Topic topic) {
		if(topic == null) {
			return ;
		}
		if(! ESClient.isExist(ESClient.getInstance().getClient(), "topics")) {
			return ;
		}
		String id = CookieUtil.getMD5(topic.getUidentify());
		Map<String, Object> doc = new HashMap<String, Object>();
		doc.put("id", id);
		doc.put("lead", topic.getLead());
		doc.put("uidentify", topic.getUidentify());
		doc.put("tips", topic.getTips());
		doc.put("leadImgUrl", topic.getLeadimgurl());
//		doc.put("topicType", 1);
		ESClient.getInstance().getClient().prepareUpdate("topics", "topic", id).setDoc(doc).execute().actionGet();
	}

	public void updateStockTopicIndex(USubject usubject) {
		if(usubject == null) {
			return ;
		}
		if(! ESClient.isExist(ESClient.getInstance().getClient(), "topics")) {
			return ;
		}
		String id = CookieUtil.getMD5(usubject.getUidentify());
		Map<String, Object> doc = new HashMap<String, Object>();
		doc.put("id", id);
		doc.put("uidentify", usubject.getUidentify());
		doc.put("name", usubject.getName());
		doc.put("pinyin", usubject.getPinyin());
		doc.put("shortPinyin", usubject.getShortPinyin());
//		doc.put("topicType", 2);
		ESClient.getInstance().getClient().prepareUpdate("topics", "topic", id).setDoc(doc).execute().actionGet();
	}

	public void deleteTopicIndex(Topic topic) {
		if(topic == null) {
			return ;
		}
		if(! ESClient.isExist(ESClient.getInstance().getClient(), "topics")) {
			return ;
		}
		String id = CookieUtil.getMD5(topic.getUidentify());
		ESClient.getInstance().getClient().prepareDelete("topics", "topic", id).execute().actionGet();
	}

	public void deleteStockTopicIndex(USubject usubject) {
		if(usubject == null) {
			return ;
		}

		if(! ESClient.isExist(ESClient.getInstance().getClient(), "topics")) {
			return ;
		}
		String id = CookieUtil.getMD5(usubject.getUidentify());
		ESClient.getInstance().getClient().prepareDelete("topics", "topic", id).execute().actionGet();
	}

	//类型(公司:0，行业:1，板块:2，指数:3，服务号:4)
	private String getUSubjectIndexName(int i) {
		String str = "";
		switch (i) {
			case 0:
				str = "stocks";
				break;

			case 1:
				str = "industries";
				break;

//			case 2:
//				str = "stock_plates";
//				break;
//
//			case 3:
//				str = "stock_indexs";
//				break;
//
//			case 4:
//				str = "service_accounts";
//				break;

			default:
				break;
		}

		return str;
	}
	//类型(公司:0，行业:1，板块:2，指数:3，服务号:4)
	private String getUSubjectTypeName(int i) {
		String str = "";
		switch (i) {
			case 0:
				str = "stock";
				break;

			case 1:
				str = "industry";
				break;

//			case 2:
//				str = "stock_plate";
//				break;
//
//			case 3:
//				str = "stock_index";
//				break;
//
//			case 4:
//				str = "service_account";
//				break;

			default:
				break;
		}

		return str;
	}
}
