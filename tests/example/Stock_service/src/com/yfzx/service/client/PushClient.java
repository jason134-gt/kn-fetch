package com.yfzx.service.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.core.util.BaseUtil;

public class PushClient {

//	private static MqttClient client;
	private static MqttAsyncClient client;
    private static String host = "tcp://192.168.1.112:7003";
    private static String userName = "";
    private static String passWord = "";
    static Logger logger = LoggerFactory.getLogger(PushClient.class);
    private static final PushClient instance = new PushClient();
    private static String mqttClientId = "";
	static
	{
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh(){
			@Override
			public void refresh() {
				init();
			}
		});
	}
	private PushClient() {
	}

	private static void init() {
		mqttClientId = "";
		String ups = ConfigCenterFactory.getString("push.username_password", "testuser:passwd");
		userName = ups.split(":")[0];
		passWord = ups.split(":")[1];
		host = ConfigCenterFactory.getString("push.host", "tcp://192.168.1.112:7003");
		logger.info("MqttPushClientStart:  "+userName+"  host:  "+host);
		initClientConnect();
	}

	private static void initClientConnect() {
		if(client != null) {
			try {
				client.disconnect();
			} catch (MqttException e1) {
				e1.printStackTrace();
				logger.error("MqttDisconnectClient: " + e1);
			}
			try {
				client.close();
				client = null;
			} catch (MqttException e) {
				e.printStackTrace();
				client = null;
				logger.error("MqttCloseClient: " + e);
			}
		}

		int persistToMem = ConfigCenterFactory.getInt("push.persistToMem", 1);
		logger.info("MqttPushPersistToMem: " + persistToMem);
		if(persistToMem == 0 && client == null) {
			String path = BaseUtil.getAppRootWithAppName() + File.separator + "mqtt" + File.separator + "persist";
			try {
				File file = new File(path);
				if(! file.exists()) {
					if(! file.mkdirs()) {
						logger.error("MqttMakeDdirFailure " + path);
					}
				}
				client = new MqttAsyncClient(host, getMqttClientId(), new MqttDefaultFilePersistence(path));
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("MqttPersistToMem0InitClient  " + e);
			}

		}

		if(persistToMem == 1 && client == null){
			try {
				client = new MqttAsyncClient(host, getMqttClientId(), new MemoryPersistence());
			} catch (MqttException e) {
				e.printStackTrace();
				logger.error("MqttPersistToMem1InitClient  " + e);
			}
		}
        connect();
	}

	private static String getMqttClientId() {
		if(StringUtils.isNotBlank(mqttClientId)) {
			return mqttClientId;
		}

		String path = BaseUtil.getAppRootWithAppName() + File.separator + "mqtt" + File.separator + "mqttId";
		try {
			File file = new File(path);
			if(! file.exists()) {
				if(! file.mkdirs()) {
					logger.error("mqtt_make_dir_failure " + path);
				}
			}
			String propFileName = path + File.separator + "mqttClientId.properties";
			File propFile = new File(propFileName);
			if(! propFile.exists()) {
				if(! propFile.createNewFile()) {
					logger.error("mqtt_create_file_failure " + path);
				}
			}
			InputStream inputStream = new FileInputStream(propFile);
			Properties props = new Properties();
			props.load(inputStream);
			mqttClientId = props.getProperty("mqtt_client_id");
			if(inputStream != null) {
				inputStream.close();
			}
			if(StringUtils.isNotBlank(mqttClientId)) {
				return mqttClientId;
			}

			mqttClientId = UUID.randomUUID().toString().substring(0, 23);
			props.setProperty("mqtt_client_id", mqttClientId);
			OutputStream outPutStream = new FileOutputStream(propFileName);
			props.store(outPutStream, "");
			if(outPutStream != null) {
				outPutStream.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("getMqttClientId  " + e);
		}

		return mqttClientId;
	}

	private static void connect() {
		 	MqttConnectOptions options = new MqttConnectOptions();
		 	int needClean = ConfigCenterFactory.getInt("push.cleanSession", 0);
		 	logger.info("MqttCleanSessionFlag: " + needClean);
		 	if(needClean == 0) {
		 		options.setCleanSession(false);
		 	} else if(needClean == 1) {
		 		options.setCleanSession(true);
		 	}
	        options.setUserName(userName);
	        options.setPassword(passWord.toCharArray());
	        // 设置超时时间
	        options.setConnectionTimeout(ConfigCenterFactory.getInt("push.ConnectionTimeout", 2));
	        // 设置会话心跳时间
	        options.setKeepAliveInterval(ConfigCenterFactory.getInt("push.KeepAliveInterval", 20));
	        try {
	            client.setCallback(new MqttCallback() {
	                @Override
	                public void connectionLost(Throwable cause) {
	                    logger.info("MqttConnectionLost-----------" + cause.getMessage());
	                    reconnect();
	                }
	                @Override
	                public void deliveryComplete(IMqttDeliveryToken token) {
	                	logger.info("MqttDeliveryComplete---------"+token.isComplete());
	                }
	                @Override
	                public void messageArrived(String topic, MqttMessage arg1)
	                        throws Exception {
	                	logger.info("MqttMessageArrived----------");
	                }
	            });
	            logger.info("MqttClientConnectBeginState: " + client.isConnected());
	            client.connect(options);
	        } catch (Exception e) {
	        	e.printStackTrace();
	            logger.info("MqttClientConnetException: " + e);
	        }

	}

	public static PushClient getInstance() {
		return instance;
	}

	public boolean publish(String topic,MqttMessage message)
	{
		try {
			reconnect();

			if(client != null && client.isConnected()) {
				client.publish(topic, message);
			} else {
				if(client == null) {
					logger.error("MqttClientIsNull");
					return false;
				} else if(client != null && ! client.isConnected()) {
					logger.error("MqttClientNotConnected");
					return false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("MqttPublishTopic:  " + topic + "   " + e);
			return false;
		}

		return true;
	}

	private static void reconnect() {
		if(client == null || client != null && ! client.isConnected()) {
			synchronized (PushClient.class) {
				if(client == null || client != null && ! client.isConnected()) {
					logger.info("MqttBegintoReconnect");
					init();
				}
			}
		}
	}

}
