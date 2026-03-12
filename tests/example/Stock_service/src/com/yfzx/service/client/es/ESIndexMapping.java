package com.yfzx.service.client.es;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.util.StockUtil;
import com.yz.configcenter.ConfigCenterFactory;
/**
 * 创建索引和对应的Mapping
 */
public class ESIndexMapping {

	private static final String simple = "yyyy-MM-dd HH:mm:ss";

	private static SimpleDateFormat sdf = new SimpleDateFormat(simple);

	private static Logger logger = LoggerFactory.getLogger(ESIndexMapping.class);

	private static ESIndexMapping instance = new ESIndexMapping();

	private ESIndexMapping() {}

	public static ESIndexMapping getInstance() {
		return instance;
	}

	public boolean createIndexAndPutMapping(Client client, String indexName, String type, int selNo) {
		CreateIndexResponse response = null;
		try {
			IndicesAdminClient indicesAdminClient = client.admin().indices();
			response = indicesAdminClient.prepareCreate(indexName).setSettings(getSettings(selNo))
					.addMapping(type, getMapping(selNo)).execute().actionGet();
		} catch (ElasticsearchException e) {
			e.printStackTrace();
			logger.error("createIndexAndPutMapping: " + e);
		}

		boolean result = false;
		if(response != null) {
			result =  response.isAcknowledged();
			if(! result) {
				logger.error("createIndexAndPutMappingFailure, IndexName-Type：" + indexName + "-" + type + " " + sdf.format(new Date()));
			}
		}

		return result;
	}

	//1:博文 2:用户 3:股票    4:行业   5：服务号  6：指数  7：板块 8:话题
	private Map<String, Object> getSettings(int selNo) {
		Map<String, Object> mapping = null;
		Integer number_of_shards = 0;
		Integer number_of_replicas = 0;
		switch (selNo) {
			case 1://博文
				number_of_shards = ConfigCenterFactory.getInt("stock_zjs.es_article_shards", 100);
				number_of_replicas = ConfigCenterFactory.getInt("stock_zjs.es_article_replicas", 1);
				mapping = getCommonSettings(number_of_shards, number_of_replicas, "");
				break;

			case 2://用户
				number_of_shards = ConfigCenterFactory.getInt("stock_zjs.es_member_shards", 100);
				number_of_replicas = ConfigCenterFactory.getInt("stock_zjs.es_member_replicas", 1);
				mapping = getAutocompleteAnalyzer(number_of_shards, number_of_replicas, "");
				break;

			case 3://股票
//				mapping = getAutocompleteAnalyzer(1, 0, "memory");
				mapping = getAutocompleteAnalyzer(2, 0, "memory");
				break;

			case 4://行业
				mapping = getAutocompleteAnalyzer(2, 0, "memory");
				break;

//			case 5://服务号
//				mapping = getAutocompleteAnalyzer(5, 1, "");
//				break;
//
//			case 6://指数
//				mapping = getAutocompleteAnalyzer(5, 1, "");
//				break;
//
//			case 7://板块
//				mapping = getAutocompleteAnalyzer(5, 1, "");
//				break;

			case 8://话题
				number_of_shards = ConfigCenterFactory.getInt("stock_zjs.es_topic_shards", 100);
				number_of_replicas = ConfigCenterFactory.getInt("stock_zjs.es_topic_replicas", 1);
				mapping = getCommonSettings(number_of_shards, number_of_replicas, "");
				break;

			case 9://抓取博文
				mapping = getCommonSettings(3, 1, "");
				break;

			default:
				break;
		}

		return mapping;
	}

	/**
	 * Search Settings
	 *
		{
		  'analysis':{
			   'filter':{
					'autocomplete_filter':{
					    'type':'edge_ngram',
					    'min_gram':1,
					    'max_gram':20
					}
				},
			   'analyzer':{
			   		'autocomplete':{
			   			'type':'custom',
			   			'char_filter':'html_strip',
			   			'tokenizer':'standard',
			   			'filter':['lowercase','autocomplete_filter']
			   		}
			   }
		   },
		   'index':{
		   		'number_of_shards':3,
		   		'number_of_replicas':1
		   },
		   'index.store.type':'niofs'
		}
	 * @return
	 */
	private Map<String, Object> getAutocompleteAnalyzer(int number_of_shards, int number_of_replicas, String storeType) {
		String autocompleteJSONMapStr = "{'analysis':{'filter':{'autocomplete_filter':{'type':'edge_ngram','min_gram':1,'max_gram':20}},'analyzer':{'autocomplete':{'type':'custom','char_filter':'html_strip','tokenizer':'standard','filter':['lowercase','autocomplete_filter']}}}," +
				"'index':{'number_of_shards':" + number_of_shards + ",'number_of_replicas':" + number_of_replicas + "}";

		if(StringUtils.isNotBlank(storeType)) {
				autocompleteJSONMapStr += ",'index.store.type':'" + storeType + "'";
		}
		autocompleteJSONMapStr += "}";

		return StockUtil.parseJSON2Map(autocompleteJSONMapStr);
	}

	/**
	 * Search Settings
	 *
		{
		   'index':{
		   		'number_of_shards':3,
		   		'number_of_replicas':1
		   },
		   'index.store.type':'niofs'
		}
	 * @return
	 */
	private Map<String, Object> getCommonSettings(int number_of_shards, int number_of_replicas, String storeType) {
		String autocompleteJSONMapStr = "{'index':{'number_of_shards':" + number_of_shards + ",'number_of_replicas':" + number_of_replicas + "}";

		if(StringUtils.isNotBlank(storeType)) {
				autocompleteJSONMapStr += ",'index.store.type':'" + storeType + "'";
		}
		autocompleteJSONMapStr += "}";

		return StockUtil.parseJSON2Map(autocompleteJSONMapStr);
	}

	//'indexAnalyzer':'ik','searchAnalyzer':'ik'
	// "analyzer": "autocomplete"
	//1:博文 2:用户 3:股票    4:行业   5：服务号  6：指数  7：板块
	private Map<String, Object> getMapping(int selNo) {
		Map<String, Object> mapping = null;
		switch (selNo) {
			case 1://博文
				mapping = getArticleMappingMap();
				break;

			case 2://用户
				mapping = getMemberMappingMap();
				break;

			case 3://股票
				mapping = getStockMappingMap();
				break;

			case 4://行业
				mapping = getIndustryMappingMap();
				break;

//			case 5://服务号
//				mapping = getServiceAccountMappingMap();
//				break;
//
//			case 6://指数
//				mapping = getStockIndexMappingMap();
//				break;
//
//			case 7://板块
//				mapping = getStockPlateMappingMap();
//				break;

			case 8://话题
				mapping = getTopicMappingMap();
				break;

			case 9://抓取博文优化
				mapping = getCrawlArticlesMappingMap();
				break;

			default:
				break;
		}

		return mapping;
	}

	/**
		{
			'article':{
				'properties':{
					'uuid':{'type':'string','store':'false','index':'no'},
					'content':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'},
					'tags':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'},
					'title':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'}
				}
			}
		}

		'time':{'type':'date','format':'YYYY-MM-dd HH:mm:ss'}
	 *
	 * @return
	 */
	private Map<String, Object> getArticleMappingMap() {
		String articleJSONMappingStr = "{'article':{'properties':{'uuid':{'type':'string','store':'false','index':'no'},'content':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'},'tags':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'},'title':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'}}}}";
		return StockUtil.parseJSON2Map(articleJSONMappingStr);
	}

	/**
		{
			'crawl_article':{
				'properties':{
					'uuid':{'type':'string','store':'false','index':'no'},
					'content':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'},
					'title':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'}
				}
			}
		}

		'time':{'type':'date','format':'YYYY-MM-dd HH:mm:ss'}
	 *
	 * @return
	 */
	private Map<String, Object> getCrawlArticlesMappingMap() {
		String articleJSONMappingStr = "{'crawl_article':{'properties':{'uuid':{'type':'string','store':'false','index':'no'},'content':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'},'title':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'}}}}";
		return StockUtil.parseJSON2Map(articleJSONMappingStr);
	}

	/**
		{
			'member':{
				'properties':{
					'uid':{'type':'string','store':'false','index':'no'},
					'nickname':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},
					'desc':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},
					'tag':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},
					'country':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},
					'province':{'type':'string','term_vector':'with_positions_offsets','store':'true','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},
					'city':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},
					'area':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},
					'street':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},
					'pinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete'},
					'shortPinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete'}
				}
			}
		}
	 *
	 *
	 * @return
	 */
	private Map<String, Object> getMemberMappingMap() {
		String articleJSONMapStr = "{'member':{'properties':{'uid':{'type':'string','store':'false','index':'no'},'nickname':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},'desc':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},'tag':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},'country':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},'province':{'type':'string','term_vector':'with_positions_offsets','store':'true','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},'city':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},'area':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},'street':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},'pinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete'},'shortPinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete'}}}}";
		return StockUtil.parseJSON2Map(articleJSONMapStr);
	}
	/**
		{
			'stock':{
				'properties':{
					'id':{'type':'integer','store':'false','index':'no'},
					'stockType':{'type':'integer','store':'true'},
					'uidentify':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},
					'name':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},
					'pinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete'},
					'shortPinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete'}
				}
			}
		}
	 * @return
	 */
	private Map<String, Object> getStockMappingMap() {
		String articleJSONMapStr = "{'stock':{'properties':{'id':{'type':'integer','store':'false','index':'no'},'stockType':{'type':'integer','store':'true'},'uidentify':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},'name':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},'pinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete'},'shortPinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete'}}}}";
		return StockUtil.parseJSON2Map(articleJSONMapStr);
	}

	/**
		{
			'industry':{
				'properties':{
					'id':{'type':'integer','store':'false','index':'no'},
					'uidentify':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},
					'name':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},
					'pinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete'},
					'shortPinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets'}
				}
			}
		}
	 * @return
	 */
	private Map<String, Object> getIndustryMappingMap() {
		String articleJSONMapStr = "{'industry':{'properties':{'id':{'type':'integer','store':'false','index':'no'},'uidentify':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},'name':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','index_analyzer':'ik','search_analyzer':'ik'},'pinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete'},'shortPinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete'}}}}";
		return StockUtil.parseJSON2Map(articleJSONMapStr);
	}
	/**
		{
			'topic':{
				'properties':{
					'id':{'type':'string','store':'false','index':'no'},
					'uidentify':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'},
					'lead':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'},
					'tips':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'},
					'leadImgUrl':{'type':'string','store':'false','index':'no'},
					'topicType':{'type':'integer','store':'true'},
					'name':{'type':'string','store':'true','term_vector':'with_positions_offsets','index_analyzer':'ik','search_analyzer':'ik'},
					'pinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets'},
					'shortPinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets'}
				}
			}
		}
	 * @return
	 */
	private Map<String, Object> getTopicMappingMap() {
		String articleJSONMapStr = "{'topic':{'properties':{'id':{'type':'string','store':'false','index':'no'},'uidentify':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'},'lead':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'},'tips':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'ik','index_analyzer':'ik','search_analyzer':'ik'},'leadImgUrl':{'type':'string','store':'false','index':'no'},'topicType':{'type':'integer','store':'true'}," +
				"'name':{'type':'string','store':'true','term_vector':'with_positions_offsets','index_analyzer':'ik','search_analyzer':'ik'},'pinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets'},'shortPinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets'}}}}";
		return StockUtil.parseJSON2Map(articleJSONMapStr);
	}

//	private Map<String, Object> getServiceAccountMappingMap() {
//		String articleJSONMapStr = "{'service_account':{'properties':{'id':{'type':'integer'},'uidentify':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','searchAnalyzer':'ik'},'name':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','searchAnalyzer':'ik'},'pinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete'},'shortPinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','searchAnalyzer':'ik'}}}}";
//		return StockUtil.parseJSON2Map(articleJSONMapStr);
//	}
//	private Map<String, Object> getStockIndexMappingMap() {
//		String articleJSONMapStr = "{'stock_index':{'properties':{'id':{'type':'integer'},'uidentify':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','searchAnalyzer':'ik'},'name':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','searchAnalyzer':'ik'},'pinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete'},'shortPinyin':{'type':'string','store':'true','term_vector':'with_positions_offsets','analyzer':'autocomplete','searchAnalyzer':'ik'}}}}";
//		return StockUtil.parseJSON2Map(articleJSONMapStr);
//	}
//	private Map<String, Object> getStockPlateMappingMap() {
//		String articleJSONMapStr = "{'stock_plate':{'properties':{'id':{'type':'integer'},'uidentify':{'type':'string','store':'true','analyzer':'autocomplete','searchAnalyzer':'ik'},'name':{'type':'string','store':'true','analyzer':'autocomplete','searchAnalyzer':'ik'},'pinyin':{'type':'string','store':'true','analyzer':'autocomplete'},'shortPinyin':{'type':'string','store':'true','analyzer':'autocomplete','searchAnalyzer':'ik'}}}}";
//		return StockUtil.parseJSON2Map(articleJSONMapStr);
//	}
}
