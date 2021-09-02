package docusign;

import java.io.*;
import java.util.Scanner;
import org.json.JSONObject;

import okhttp3.*;
public class Main {
	public static void main(String []args) throws IOException{
		
		
		Scanner input = new Scanner(System.in); 
		System.out.println("Enter user code:");
		String userCode = input.nextLine();
		System.out.println("Enter obfuscated keys [b64(integration_key:secret_key)]:");
		String obfuscatedKeys = input.nextLine();
		System.out.println("Enter recipient email:");
		String recipientEmail = input.nextLine();
		System.out.println("Enter recipient name:");
		String recipientName = input.nextLine();
		System.out.println("Sending envelope...");
		
		// Authorization
		String accessToken;
		String refreshToken;
		
		// User info
		String userId;
		String accountId;
		String baseUrl;
		String accountName;
		
		// Envelope info
		String envelopeId;
		
		// First request: Use userCode to authenticate.
		// Returns accessToken
		OkHttpClient client = new OkHttpClient().newBuilder()
			.build();
		MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
		RequestBody body = RequestBody.create(mediaType, "code="+ userCode + "&grant_type=authorization_code");
		Request request = new Request.Builder()
			.url("https://account-d.docusign.com/oauth/token")
			.method("POST", body)
			.addHeader("Authorization", "Basic " + obfuscatedKeys)
			.addHeader("Content-Type", "application/x-www-form-urlencoded")
			.build();
		
		Response response = client.newCall(request).execute();
		JSONObject resData = new JSONObject(response.body().string());
		
		accessToken = resData.getString("access_token");
		
		// Second request: Uses accessToken to get user info.
		// Returns userId, accountId, baseUrl and accountName
		
		request = new Request.Builder()
			.url("https://account-d.docusign.com/oauth/userinfo")
			.method("GET", null)
			.addHeader("Authorization", "Bearer " + accessToken)
			.build();
		response = client.newCall(request).execute();
		resData = new JSONObject(response.body().string());
		
		userId = resData.getString("sub");
		accountId = resData.getJSONArray("accounts").getJSONObject(0).getString("account_id");
		baseUrl = resData.getJSONArray("accounts").getJSONObject(0).getString("base_uri");
		accountName = resData.getJSONArray("accounts").getJSONObject(0).getString("account_name");
		
		// Third request: Uses userId to create and send the envelope.
		// Returns envelopeId
		
		mediaType = MediaType.parse("application/json");
		body = RequestBody.create(mediaType, "{\n    \"emailSubject\": \"Please sign this document set\",\n    \"documents\": [\n        {\n            \"documentBase64\": \"dGVzdCBkb2M=\",\n            \"name\": \"Lorem Ipsum\",\n            \"fileExtension\": \"txt\",\n            \"documentId\": \"1\"\n        }\n    ],\n    \"recipients\": {\n        \"signers\": [\n            {\n                \"email\": \"" + recipientEmail + "\",\n                \"name\": \"" + recipientName + "\",\n                \"recipientId\": \"1\",\n                \"routingOrder\": \"1\"\n            }\n        ]\n    },\n    \"status\": \"sent\"\n}");
		request = new Request.Builder()
			.url(baseUrl + "/restapi//v2.1/accounts/" + accountId + "/envelopes")
			.method("POST", body)
			.addHeader("Accept", "application/json")
			.addHeader("Authorization", "Bearer " + accessToken)
			.addHeader("Content-Type", "application/json")
			.build();
		response = client.newCall(request).execute();
		resData = new JSONObject(response.body().string());

		envelopeId = resData.getString("envelopeId");
		
		if(resData.getString("status").equals("sent")) {
			System.out.println("Envelope sent.");
			System.out.println("Waiting for recipient to sign document...");
		}
		
		// Fourth request: Uses envelopeId to check envelope status.
		// Returns envelope info
		
		mediaType = MediaType.parse("application/x-www-form-urlencoded");

		request = new Request.Builder()
			.url("https://demo.docusign.net/restapi//v2.1/accounts/" + accountId + "/envelopes/" + envelopeId + "?advanced_update={{advanced_update}}&include={{include}} ")
			.method("GET", null)
			.addHeader("Accept", "application/json")
			.addHeader("Authorization", "Bearer " + accessToken)
			.build();

		while(true) {
			response = client.newCall(request).execute();
			resData = new JSONObject(response.body().string());

			if(resData.getString("status").equals("completed")) {
				System.out.println("Document " + envelopeId + " signed sucessfully!");
				break;
			}
		}
		
		input.close();
	}
}
