package com.yfzx.service.client.es;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
/**
 *
 *
 */
public class ESClient {
	private static final Logger logger = LoggerFactory.getLogger(ESClient.class);
	Object lock = new Object();
	static {
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {
			public void refresh() {
				ESClient.getInstance().initMyConfig(false);
			}
		});
	}

	private ESClient() {}

	public  void initMyConfig(boolean isInit) {
		if(client != null && ! isInit) {
			client.close();
			client = null;
		}
		if(client == null) {
			String clusterName = ConfigCenterFactory.getString("stock_zjs.es_clustername", "es_pro_01");
			String ipPortStr = ConfigCenterFactory.getString("stock_zjs.es_nodeips", "192.168.1.112:9301,192.168.1.112:9302");
			client = initClient(clusterName, ipPortStr);
		}
	}

	private static ESClient instance = new ESClient();

	private static Client client = null;

	public static ESClient getInstance() {
		return instance;
	}

	public Client getClient() {
		if(client == null) {
			synchronized (lock) {
				if(client==null)
				{
					initMyConfig(true);
				}
			}

		}
		return client;
	}

	public synchronized static void closeClient() {
		if(client != null) {
			client.close();
			client = null;
		}
	}

	private static Client initClient(String clusterName, String ipPorts) {
		org.elasticsearch.common.settings.ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder();
		builder.put("cluster.name", clusterName);
		builder.put("client.transport.sniff", true);
		TransportClient client =  new TransportClient(builder.build());

		String[] temp = ipPorts.split(",");
		if(temp.length > 0) {
			for(String ipPort : temp) {
				String[] arr = ipPort.split(":");
				String ip = arr[0];
				String port = arr[1];
				client.addTransportAddress(new InetSocketTransportAddress(ip, Integer.valueOf(port)));
			}
		}

		return client;
	}

	public static void main(String[] args) {
		org.elasticsearch.common.settings.ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder();
		builder.put("cluster.name", "es_01");
		TransportClient client =  new TransportClient(builder.build());
		client.addTransportAddress(new InetSocketTransportAddress("www.igushuo.com", 9300));
		System.out.println(isExist(client, "articles"));
		System.out.println(client);
		client.close();
	}

	public static boolean isExist(Client client, String... indexNames) {
		IndicesExistsResponse ieresp = null;
		try {
			IndicesAdminClient indicesAdminClient = client.admin().indices();
			ieresp = indicesAdminClient.prepareExists(indexNames).execute().actionGet();
		} catch (ElasticsearchException e) {
			e.printStackTrace();
			logger.error("isExist: " + e);
		}

		if(ieresp  != null) {
			return ieresp.isExists();
		}

		return false;
	}

	public static boolean isExist(IndicesAdminClient indicesAdminClient, String... indexNames) {
		IndicesExistsResponse ieresp = indicesAdminClient.prepareExists(indexNames).execute().actionGet();
		return ieresp.isExists();
	}

	public static boolean delIndex(Client client, String... indexNames) {
		IndicesAdminClient indicesAdminClient = client.admin().indices();

		if(isExist(indicesAdminClient, indexNames)) {
			DeleteIndexResponse delResp = indicesAdminClient.prepareDelete(indexNames).execute().actionGet();
			return delResp.isAcknowledged();
		}

		return false;
	}

	public static boolean delIndexWithoutCheck(Client client, String... indexNames) {
		DeleteIndexResponse delResp = null;
		try {
			IndicesAdminClient indicesAdminClient = client.admin().indices();
			delResp = indicesAdminClient.prepareDelete(indexNames).execute().actionGet();
		} catch (ElasticsearchException e) {
			e.printStackTrace();
			logger.error("delIndexWithoutCheck: " + e);
		}

		if(delResp != null) {
			return delResp.isAcknowledged();
		}

		return true;
	}

}
