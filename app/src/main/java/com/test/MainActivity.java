package com.test;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private ProgressDialog progressDialog;
    private Map<Integer, ArrayList<String>> allItems;
    private ArrayList<Integer> listIds;
    private ArrayList<String> currentDisplayItems;
    private ArrayAdapter listIdAdaptor, itemAdaptor;
    private ListView listIdListView, itemListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new JsonTask().execute("https://fetch-hiring.s3.amazonaws.com/hiring.json");

        listIds = new ArrayList<>(Arrays.asList(-1));
        listIdAdaptor = new ArrayAdapter(this, android.R.layout.simple_list_item_1, listIds);
        listIdListView = findViewById(R.id.listIdListView);
        listIdListView.setAdapter(listIdAdaptor);
        listIdListView.setClickable(true);
        listIdListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int selectedListId = listIds.get(position);
                currentDisplayItems.clear();
                currentDisplayItems.addAll(allItems.get(selectedListId));

                itemAdaptor.clear();
                itemAdaptor.addAll(currentDisplayItems);
                itemAdaptor.notifyDataSetChanged();
            }

        });

        currentDisplayItems = new ArrayList<>(Arrays.asList("Please select a list"));
        itemAdaptor = new ArrayAdapter(this, android.R.layout.simple_list_item_1, currentDisplayItems);
        itemListView = findViewById(R.id.itemListView);
        itemListView.setAdapter(itemAdaptor);
        itemListView.setClickable(false);
    }


    private class JsonTask extends AsyncTask<String, String, Map<Integer, ArrayList<String>>> {

        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Please wait");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        protected Map<Integer, ArrayList<String>> doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));

                Map<Integer, ArrayList<String>> result = new HashMap<>();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    Item item = parseLine(line);
                    if (item == null) continue;
                    ArrayList<String> temp = result.getOrDefault(item.listId, new ArrayList<>());
                    temp.add(item.name + "\t(" + item.id + ")");
                    result.put(item.listId, temp);
                }

                return result;

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        private Item parseLine(String line) {
            Pattern pattern = Pattern.compile("\"id\": ([0-9]+), \"listId\": ([0-9]+), \"name\": \"(.+)\"");
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                return new Item(Integer.valueOf(matcher.group(1)), Integer.valueOf(matcher.group(2)), matcher.group(3));
            }
            return null;
        }

        @Override
        protected void onPostExecute(Map<Integer, ArrayList<String>> result) {
            super.onPostExecute(result);
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (result != null) {
                listIds.clear();
                listIds.addAll(result.keySet());
                Collections.sort(listIds);
                listIdAdaptor.notifyDataSetChanged();

                for (Integer listId : listIds) {
                    Collections.sort(result.get(listId));
                }

                if (allItems == null) allItems = new HashMap<>();
                allItems.clear();
                allItems.putAll(result);
            }

            currentDisplayItems = new ArrayList<>(Arrays.asList("Please select a list"));
            itemListView.invalidateViews();
            itemAdaptor.notifyDataSetChanged();
        }

    }

    private class Item {
        Integer id;
        Integer listId;
        String name;

        public Item(Integer id, Integer listId, String name) {
            this.id = id;
            this.listId = listId;
            this.name = name;
        }

        @NonNull
        public String toString() {
            return "id: " + id + "\tlistId: " + listId + "\tname:" + name;
        }
    }
}