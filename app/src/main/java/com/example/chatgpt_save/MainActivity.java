package com.example.chatgpt_save;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    EditText promptEditText;
    TextView resultTextView;
    Button submitButton;

    Button saveButton;
    DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        promptEditText = findViewById(R.id.promptEditText);
        resultTextView = findViewById(R.id.resultTextView);
        submitButton = findViewById(R.id.submitButton);
        saveButton = findViewById(R.id.saveButton);


        databaseHelper = new DatabaseHelper(this);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String prompt = promptEditText.getText().toString();
                new OpenAIRequestTask().execute(prompt);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveToDatabase();
            }
        });


        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM AuditPrompt", null);
        if (cursor.moveToFirst()) {
            do {
                String dateTime = cursor.getString(cursor.getColumnIndex("date_time"));
                String prompt = cursor.getString(cursor.getColumnIndex("prompt"));
                Log.d("AuditPrompt", "Date Time: " + dateTime + ", Prompt: " + prompt);
            } while (cursor.moveToNext());
        }
        cursor.close();

        cursor = db.rawQuery("SELECT * FROM Responses", null);
        if (cursor.moveToFirst()) {
            do {
                String dateTime = cursor.getString(cursor.getColumnIndex("date_time"));
                String response = cursor.getString(cursor.getColumnIndex("response"));
                Log.d("Responses", "Date Time: " + dateTime + ", Response: " + response);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

    }

    private void saveToDatabase() {
        // Get prompt and response
        String prompt = promptEditText.getText().toString();
        String response = resultTextView.getText().toString();

        // Insert prompt and response into database
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues promptValues = new ContentValues();
        promptValues.put("date_time", getCurrentDateTime());
        promptValues.put("prompt", prompt);
        db.insert("AuditPrompt", null, promptValues);

        ContentValues responseValues = new ContentValues();
        responseValues.put("date_time", getCurrentDateTime());
        responseValues.put("response", response);
        db.insert("Responses", null, responseValues);

        db.close();

        // Optional: Show a message indicating successful save
        Toast.makeText(MainActivity.this, "Prompt and response saved to database.", Toast.LENGTH_SHORT).show();
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private class OpenAIRequestTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String prompt = params[0];
            String apiKey = "Token here";
            String apiUrl = "https://api.openai.com/v1/chat/completions";
            String result = "";

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setDoOutput(true);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("model", "gpt-3.5-turbo");
                JSONArray messagesArray = new JSONArray();
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", prompt);
                messagesArray.put(userMessage);
                jsonObject.put("messages", messagesArray);

                OutputStream os = conn.getOutputStream();
                os.write(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                // Check if the request was successful
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    while ((line = br.readLine()) != null) {
                        result += line + "\n";
                    }
                    br.close();
                } else {
                    // Handle error response
                    Log.e("Error","Error here "+conn.getResponseCode() );
                    result = "Error: " + conn.getResponseMessage();
                }

                // Close the connection
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Error","Error below");
                result = "Error: " + e.getMessage();
            }

            return result;
        }


        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject jsonResponse = new JSONObject(result);
                JSONArray choicesArray = jsonResponse.getJSONArray("choices");
                if (choicesArray.length() > 0) {
                    JSONObject choiceObject = choicesArray.getJSONObject(0);
                    JSONObject messageObject = choiceObject.getJSONObject("message");
                    String messageContent = messageObject.getString("content");
                    resultTextView.setText(messageContent);
                } else {
                    resultTextView.setText("No message found in the response.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                resultTextView.setText("Error parsing response: " + e.getMessage());
            }
        }

    }
}
