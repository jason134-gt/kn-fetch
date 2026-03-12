package com.yfzx.service.mobile;


public class MobileMessageService {
	private static MobileMessageService instance=new MobileMessageService();
	public static MobileMessageService getIntance(){
		return instance;
	}
	
	
	/**
	 *
	 * 推送信息给手机端
	 * 注：暂时挪动到zjs，因为暂时没有把对应的jar包放置到私服。
	 * 
	 * 
	 * */
	public void pushMessage2Mobile(){
//		// 1. 设置developer平台的ApiKey/SecretKey
//        String apiKey = ConfigCenterFactory.getString("stock_zjs.baiduPush_apiKey","xmUSS2fgTZzOV1w7rm0PATSi");
//        String secretKey = ConfigCenterFactory.getString("stock_zjs.baiduPush_secretKey","pNSDGkxdarnL1UGqV3W8egtqxiyCvYbG");
//        ChannelKeyPair pair = new ChannelKeyPair(apiKey, secretKey);
//		 // 2. 创建BaiduChannelClient对象实例
//        BaiduChannelClient channelClient = new BaiduChannelClient(pair);
//        
//        // 3. 若要了解交互细节，请注册YunLogHandler类
//        channelClient.setChannelLogHandler(new YunLogHandler() {
//            @Override
//            public void onHandle(YunLogEvent event) {
//                System.out.println(event.getMessage());
//            }
//        });
//        
//        try {
//            // 4. 创建请求类对象
//            // 手机端的ChannelId， 手机端的UserId， 先用1111111111111代替，用户需替换为自己的
//            PushUnicastMessageRequest request = new PushUnicastMessageRequest();
//            int requestType=MobileAgentService.getIntance().getRequestType(getHttpServletRequest());
//            request.setDeviceType(3); // device_type => 1: web 2: pc 3:android
//                                      // 4:ios 5:wp
//            request.setChannelId(pushChannelId);
//            request.setUserId(pushUserId);
//
//            request.setMessage(content);
//
//            // 5. 调用pushMessage接口
//            PushUnicastMessageResponse response = channelClient
//                    .pushUnicastMessage(request);
//
//            // 6. 认证推送成功
//            System.out.println("push amount : " + response.getSuccessAmount());
//    		sendSuccessResult(2, "消息发送成功",null);
//        } catch (ChannelClientException e) {
//        	sendErrorResult(0, "消息处理异常", null);
//            // 处理客户端错误异常
//            e.printStackTrace();
//        } catch (ChannelServerException e) {
//        	sendErrorResult(1, "消息处理异常", null);
//            // 处理服务端错误异常
//            System.out.println(String.format(
//                    "request_id: %d, error_code: %d, error_message: %s",
//                    e.getRequestId(), e.getErrorCode(), e.getErrorMsg()));
//        }
	}
}
