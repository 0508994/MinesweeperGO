package com.mm.minesweepergo.minesweepergo;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.renderscript.ScriptGroup;
import android.util.Log;

import com.mm.minesweepergo.minesweepergo.Constants;
import com.mm.minesweepergo.minesweepergo.DomainModel.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Milan Nikolić on 31-May-17.
 */

public class HTTP {

    private static String inputStreamToString(InputStream is) {
        String line = "";
        StringBuilder total = new StringBuilder();
        BufferedReader bf = new BufferedReader(new InputStreamReader(is));
        try
        {
            while ((line = bf.readLine()) != null)
            {
                total.append(line);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return total.toString();
    }

    public static String createUser(User user) {

        String retStr = "";

        try {
            URL url = new URL(Constants.URL + "/api/register");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            Log.e("http", "por1");


            JSONObject body = new JSONObject();

            body.put("username", user.username);
            body.put("password", user.password);
            body.put("email", user.email);
            body.put("firstname", user.firstName);
            body.put("lastname", user.lastName);
            body.put("phonenumber", user.phoneNumber);
            body.put("btDevice", user.btDevice);

            Uri.Builder builder = new Uri.Builder().appendQueryParameter("action", body.toString());
            String query = builder.build().getEncodedQuery();



            OutputStream os = conn.getOutputStream();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            bw.write(query);
            bw.flush();
            bw.close();
            os.close();
            int responseCode = conn.getResponseCode();

            Log.e("http", String.valueOf(responseCode));
            if (responseCode == HttpURLConnection.HTTP_OK) {
                retStr = inputStreamToString(conn.getInputStream());
            } else
                retStr = String.valueOf("Error: " + responseCode);

            Log.e("http", retStr);

        } catch (Exception e) {
            Log.e("http", "error");
        }
        return retStr;
    }

    public static List<String> getAllUsernames() {
        List<String> names = new ArrayList<String>();
        String retStr = null;
        try {
            URL url = new URL(Constants.URL + "/api/getAllUsernames");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

//            Uri.Builder builder = new Uri.Builder().appendQueryParameter("action", "yes");
//            String query = builder.build().getEncodedQuery();

            OutputStream os = conn.getOutputStream();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            //bw.write(query);

            bw.flush();
            bw.close();
            os.close();
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String str = inputStreamToString(conn.getInputStream());
                JSONArray jsonArray = new JSONArray(str);

                for (int i = 0; i < jsonArray.length(); i++) {
                    String name = jsonArray.getString(i);
                    names.add(name);

                }
            } else
                Log.e("HTTPCOde_Error", String.valueOf(responseCode));


        } catch (Exception e) {
            e.printStackTrace();

        }
        return names;
    }

    public static User login(String username, String password)
    {
        User retUser = null;

        try {
            URL url = new URL(Constants.URL + "/api/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();

            body.put("username", username);
            body.put("password", password);

            Uri.Builder builder = new Uri.Builder().appendQueryParameter("action", body.toString());
            String query = builder.build().getEncodedQuery();

            OutputStream os = conn.getOutputStream();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            bw.write(query);

            bw.flush();
            bw.close();
            os.close();
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String str = inputStreamToString(conn.getInputStream());
                JSONObject jsonObject = new JSONObject(str);

                if(jsonObject!=null){
                    retUser = new User();
                    JSONObject properties = jsonObject.getJSONObject("properties");

                    retUser.username = properties.getString("Username");
                    retUser.password = properties.getString("Password");
                    retUser.email   = properties.getString("Email");
                    retUser.firstName = properties.getString("FirstName");
                    retUser.lastName = properties.getString("LastName");
                    retUser.phoneNumber = properties.getString("PhoneNumber");
                    retUser.imagePath = properties.getString("ImageURL");


                }
            } else
                Log.e("HTTPCOde_Error", String.valueOf(responseCode));


        } catch (Exception e) {
            e.printStackTrace();

        }

        return retUser;
    }

    public static String uploadPicture(String username, String path) {
        try {
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            @SuppressWarnings("PointlessArithmeticExpression")
            int maxBufferSize = 1 * 1024 * 1024;


            java.net.URL url = new URL(Constants.URL + "/api/imageUpload");
            //Log.d(ApplicationConstant.TAG, "url " + url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Allow Inputs &amp; Outputs.
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // Set HTTP method to POST.
            connection.setRequestMethod("POST");

            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            FileInputStream fileInputStream;
            DataOutputStream outputStream;
            {
                outputStream = new DataOutputStream(connection.getOutputStream());

                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                String filename = path;
                outputStream.writeBytes("Content-Disposition: form-data; name=\"pic\"; filename=\"" + filename+ "\""  + lineEnd);
                outputStream.writeBytes(lineEnd);
                //Log.d(ApplicationConstant.TAG, "filename " + filename);

                fileInputStream = new FileInputStream(filename);

                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);

                buffer = new byte[bufferSize];

                // Read file
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    outputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                outputStream.writeBytes(lineEnd);

                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"username\"" + lineEnd);
                outputStream.writeBytes("Content-Type: text/plain" + lineEnd);
                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(username);
                outputStream.writeBytes(lineEnd);

                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            }

            int serverResponseCode = connection.getResponseCode();
            String serverResponseMessage = connection.getResponseMessage();
            Log.d("serverResponseCode", "" + serverResponseCode);
            Log.d("serverResponseMessage", "" + serverResponseMessage);

            fileInputStream.close();
            outputStream.flush();
            outputStream.close();

            if (serverResponseCode == 200) {
                return "true";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "false";
    }
}
