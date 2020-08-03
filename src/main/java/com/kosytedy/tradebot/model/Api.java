package com.kosytedy.tradebot.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class Api {
	
	private static final String TAG = Api.class.getSimpleName();

    private static final String ALGORITHM_HMACSHA384 = "HmacSHA384";

    private static String apiKey = "xxxxxxxxxxxxxxxxxx";
    private static String apiKeySecret = "xxxxxxxxxxxxxxx";
    private static long nonce = System.currentTimeMillis();

    /**
     * public access only
     */
    public Api() {
        apiKey = null;
        apiKeySecret = null;
    }

    /**
     * public and authenticated access
     *
     * @param apiKey
     * @param apiKeySecret
     */
    public Api(String apiKey, String apiKeySecret) {
        this.apiKey = apiKey;
        this.apiKeySecret = apiKeySecret;
    }
    
    /**
     * Creates an authenticated request WITHOUT request parameters.
     *
     * @return Response string if request successful
     * @throws IOException
     */
    public static String sendRequest(String urlPath, String payload, String method) throws IOException {
        String sResponse;

        HttpURLConnection conn = null;

        // String method = "GET";
        //String method = "POST";

        try {
            URL url = new URL("https://api.bitfinex.com/v1/" + urlPath);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);

            if (isAccessPublicOnly()) {
                String msg = "Authenticated access not possible, because key and secret was not initialized: use right constructor.";
                //Log.e(TAG, msg);
                return msg;
            }

            conn.setDoOutput(true);
            conn.setDoInput(true);

            String payload_base64 = Base64.getEncoder().encodeToString(payload.getBytes());
            
            String payload_sha384hmac = hmacDigest(payload_base64, apiKeySecret, ALGORITHM_HMACSHA384);

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.addRequestProperty("X-BFX-APIKEY", apiKey);
            conn.addRequestProperty("X-BFX-PAYLOAD", payload_base64);
            conn.addRequestProperty("X-BFX-SIGNATURE", payload_sha384hmac);

            // read the response
            InputStream in = new BufferedInputStream(conn.getInputStream());
            return convertStreamToString(in);

        } catch (MalformedURLException e) {
            throw new IOException(e.getClass().getName(), e);
        } catch (ProtocolException e) {
            throw new IOException(e.getClass().getName(), e);
        } catch (IOException e) {

            String errMsg = e.getLocalizedMessage();

            if (conn != null) {
                try {
                    sResponse = convertStreamToString(conn.getErrorStream());
                    errMsg += " -> " + sResponse;
                    //Log.e(TAG, errMsg, e);
                    return sResponse;
                } catch (IOException e1) {
                    errMsg += " Error on reading error-stream. -> " + e1.getLocalizedMessage();
                    //Log.e(TAG, errMsg, e);
                    throw new IOException(e.getClass().getName(), e1);
                }
            } else {
                throw new IOException(e.getClass().getName(), e);
            }
        } catch (JSONException e) {
            String msg = "Error on setting up the connection to server";
            throw new IOException(msg, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    private static String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static long getNonce() {
        return ++nonce;
    }

    public static boolean isAccessPublicOnly() {
        return apiKey == null;
    }

    public static String hmacDigest(String payload_base64, String keyString, String algo) {
        String digest = null;
        try {
            SecretKeySpec key = new SecretKeySpec((keyString).getBytes("UTF-8"), algo);
            Mac mac = Mac.getInstance(algo);
            mac.init(key);

            byte[] bytes = mac.doFinal(payload_base64.getBytes("ASCII"));

            StringBuffer hash = new StringBuffer();
            for (int i = 0; i < bytes.length; i++) {
                String hex = Integer.toHexString(0xFF & bytes[i]);
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }
            digest = hash.toString();
        } catch (UnsupportedEncodingException e) {
            //Log.e(TAG, "Exception: " + e.getMessage());
        } catch (InvalidKeyException e) {
            //Log.e(TAG, "Exception: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            //Log.e(TAG, "Exception: " + e.getMessage());
        }
        return digest;
    }

    /**
     * Gets the balance in trading account
     * @return
     * @throws IOException
     */
	public double getBalance() throws IOException {

		String urlPath = "/balances";
        JSONObject jo = new JSONObject();
        jo.put("request", urlPath);
        jo.put("nonce", Long.toString(getNonce()));

        String payload = jo.toString();

		String response = sendRequest(urlPath, payload, "POST");
		JSONObject obj = new JSONObject(response);
		return obj.getJSONObject("").getDouble("available");
	}
	
	public static double getMarketPrice() throws IOException {
		
		String urlPath = "/pubticker/btcusd";
        JSONObject jo = new JSONObject();
        jo.put("request", urlPath);
        jo.put("nonce", Long.toString(getNonce()));

        String payload = jo.toString();

		String response = sendRequest(urlPath, payload, "GET");
		JSONObject obj = new JSONObject(response);
		return obj.getDouble("last_price");
	}
	
	public static double placeSellOrder() throws IOException {
		String urlPath = "/order/new";
        JSONObject jo = new JSONObject();
        jo.put("request", urlPath);
        jo.put("nonce", Long.toString(getNonce()));
        jo.put("symbol", "BTCUSD");
        jo.put("amount", 0.3);
        jo.put("price", 1000);
        jo.put("exchange", "bitfinex");
        jo.put("side", "sell");
        jo.put("type", "exchange market");

        String payload = jo.toString();

		String response = sendRequest(urlPath, payload, "POST");
		JSONObject obj = new JSONObject(response);
		return obj.getDouble("price");
	}
	
	public static double placeBuyOrder() throws IOException {
		String urlPath = "/order/new";
        JSONObject jo = new JSONObject();
        jo.put("request", urlPath);
        jo.put("nonce", Long.toString(getNonce()));
        jo.put("symbol", "BTCUSD");
        jo.put("amount", 0.3);
        jo.put("price", 1000);
        jo.put("exchange", "bitfinex");
        jo.put("side", "buy");
        jo.put("type", "exchange market");

        String payload = jo.toString();

		String response = sendRequest(urlPath, payload, "POST");
		JSONObject obj = new JSONObject(response);
		return obj.getDouble("price");
	}
	
	public static double getOperationDetailsss() {
		
		return 0;
	}
}
