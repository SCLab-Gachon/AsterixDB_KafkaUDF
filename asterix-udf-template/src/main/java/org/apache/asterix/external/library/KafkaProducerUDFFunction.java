/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.asterix.external.library;

import org.apache.asterix.external.api.IExternalScalarFunction;
import org.apache.asterix.external.api.IFunctionHelper;
import org.apache.asterix.external.library.java.base.JString;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;



public class KafkaProducerUDFFunction implements IExternalScalarFunction {

    public static String url="";
	public static String q="";
	private final static String USER_AGENT = "Mozilla/5.0";

    private HttpURLConnection con;
    private URL obj;
    private Producer<String, String> producer;
    private JSONArray results;

    private JString result;

    @Override
    public void deinitialize() {
        // nothing to do here
    }

    @Override
    public void evaluate(IFunctionHelper functionHelper) throws Exception, IOException {

		result = (JString) functionHelper.getResultObject();

        // Set properties of Kafka
		Properties props = new Properties();
		props.put("bootstrap.servers", "localhost:9092");
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		producer = new KafkaProducer<>(props);

		String url = "http://192.9.24.246:19002/query/service";
		obj = new URL(url);
		con = (HttpURLConnection) obj.openConnection();

		// Add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		String urlParameters = "use feeds; select * from DiseaseTweets Limit 10;";

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		//		System.out.println("\nSending 'POST' request to AsterixDB : " + url);
		//		System.out.println("Post parameters : " + urlParameters);
		//		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));

		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		// Print result
//		System.out.println(response.toString());

		JSONParser parser = new JSONParser();
		Object obj2 = parser.parse(response.toString());
		JSONObject jsonObj = (JSONObject) obj2;
		results =  (JSONArray) jsonObj.get("results");

		




		String text_result = "";
        for(int i=0; i<results.size(); i++) {
			JSONObject tweets = (JSONObject)results.get(i);
			JSONObject DiseaseTweets = (JSONObject) tweets.get("DiseaseTweets");

            String text = (String) DiseaseTweets.get("text");
            text_result += text;
            text_result += "\n";

			// System.out.println("----------------------------------------------------------------------------------------------------");
//			System.out.printf("i = %d"+ i + "  TEXT = %s",text);
            // System.out.println("TEXT = %s" + text);
            
			producer.send(new ProducerRecord<String, String>("disease_topic-2", text));
		}
        producer.close();
        
        result.setValue(text_result);
        functionHelper.setResult(result);

		
    }

    @Override
    public void initialize(IFunctionHelper functionHelper) throws IOException, ParseException {
        

        

    }

}
