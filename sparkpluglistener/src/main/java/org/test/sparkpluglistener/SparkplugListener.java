package org.test.sparkpluglistener;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.tahu.message.SparkplugBPayloadDecoder;
import org.eclipse.tahu.message.model.SparkplugBPayload;
import org.eclipse.tahu.message.model.Topic;
import org.eclipse.tahu.util.TopicUtil;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Properties;

public class SparkplugListener implements MqttCallbackExtended {

	// populated by configfile
		private String serverUrl = "";
		private String clientId = "";
		private String topic = "";
		private int QoS = 0;
		private String username = "";
		private String password = "";
		
		private MqttClient client;
	    
		public SparkplugListener(String filePath) throws IOException {
	        Properties properties = new Properties();
	        try (FileInputStream fis = new FileInputStream(filePath)) {
	            properties.load(fis);
	        }

	        this.serverUrl = properties.getProperty("serverUrl");
	        this.clientId = properties.getProperty("clientId");
	        this.topic = properties.getProperty("topic");
	        this.QoS = Integer.parseInt(properties.getProperty("QoS"));
	        this.username = properties.getProperty("username");
	        this.password = properties.getProperty("password");
	    }

	public static void main(String[] args) {
		 try {
	            SparkplugListener listener = new SparkplugListener("config.properties");
	            listener.run();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }	
	}
	


	public void run() {
		try {
			// Connect to the MQTT Server
			MqttConnectOptions options = new MqttConnectOptions();
			options.setAutomaticReconnect(true);
			options.setCleanSession(true);
			options.setConnectionTimeout(30);
			options.setKeepAliveInterval(30);
			if (!username.equals("-")) {
				options.setUserName(username);
				options.setPassword(password.toCharArray());
			}
			client = new MqttClient(serverUrl, clientId);
			client.setTimeToWait(5000); // short timeout on failure to connect
			client.connect(options);
			client.setCallback(this);

			// Just listen to all DDATA messages on spAv1.0 topics and wait for inbound messages
			client.subscribe(topic, QoS);
		} catch (Exception e) {
			e.	printStackTrace();
		}
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		System.out.println("Connected!");
	}

	@Override
	public void connectionLost(Throwable cause) {
		System.out.println("The MQTT Connection was lost! - will auto-reconnect");
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		Topic sparkplugTopic = TopicUtil.parseTopic(topic);
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);

		System.out.println("Message Arrived on Sparkplug topic " + sparkplugTopic.toString());

		SparkplugBPayloadDecoder decoder = new SparkplugBPayloadDecoder();
		SparkplugBPayload inboundPayload = decoder.buildFromByteArray(message.getPayload(), null);

		// Convert the message to JSON and print to system.out
		try {
			String payloadString = mapper.writeValueAsString(inboundPayload);
			System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(inboundPayload));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		System.out.println("Published message: " + token);
	}
}