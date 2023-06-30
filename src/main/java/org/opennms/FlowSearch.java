package org.opennms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.maxmind.geoip2.DatabaseReader;

public class FlowSearch {
    protected final Long start;
    protected final Long end;
    private static DatabaseReader databaseReader;

    static {
        try {
            databaseReader = new DatabaseReader.Builder(FlowTool.class.getClassLoader().getResourceAsStream("GeoLite2-City.mmdb")).build();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    private final String host;
    private final int port;
    private List<Flow> results;

    public FlowSearch(final String host, final int port, final Long start, final Long end) {
        this.start = start;
        this.end = end;
        this.host = host;
        this.port = port;
    }

    private JSONObject post(final String data, final String url) {
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            final HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new StringEntity(data));
            httpPost.addHeader("Content-Type", "application/json");
            final HttpResponse response = httpClient.execute(httpPost);
            return new JSONObject(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            System.err.println("Error connecting to Elastic Search: " + e.getMessage());
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return null;
    }

    private JSONObject delete(final String data, final String url) {
        final DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            final HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(url);
            httpDelete.setEntity(new StringEntity(data));
            httpDelete.addHeader("Content-Type", "application/json");
            final HttpResponse response = httpClient.execute(httpDelete);
            return new JSONObject(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            System.err.println("Error connecting to Elastic Search: " + e.getMessage());
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return null;
    }

    protected JSONObject search() {
        final String data = "{\"sort\":[{\"@timestamp\":{\"order\":\"asc\"}}],\"query\":{\"range\":{\"@timestamp\":{\"gt\":\"" + start + "\",\"lte\":\"" + (end == null ? "now" : end) + "\"}}}}";
        return post(data, "http://" + host + ":" + port + "/_search?size=1000&scroll=1m");
    }

    protected JSONObject scroll(final String scrollId) {
        final String data = "{\"scroll\":\"1m\",\"scroll_id\":\"" + scrollId + "\"}";
        return post(data, "http://" + host + ":" + port + "/_search/scroll");
    }

    protected JSONObject clear(final String scrollId) {
        final String data = "{\"scroll_id\":\"" + scrollId + "\"}";
        return delete(data, "http://" + host + ":" + port + "/_search/scroll");
    }

    private List<Flow> getResults(final JSONObject jsonObject) {
        final JSONArray array = jsonObject.getJSONObject("hits").getJSONArray("hits");
        final List<Flow> resultList = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            final JSONObject object = array.getJSONObject(i).getJSONObject("_source");
            final Flow flow = new Flow(object, databaseReader);
            resultList.add(flow);
        }
        return resultList;
    }

    public List<Flow> getFlows() {
        if (results == null) {
            JSONObject jsonObject = search();
            final String scrollId = jsonObject.getString("_scroll_id");
            results = new ArrayList<>();
            List<Flow> scrollResults = getResults(jsonObject);
            while (scrollResults.size() > 0) {
                results.addAll(scrollResults);
                jsonObject = scroll(scrollId);
                scrollResults = getResults(jsonObject);
            }
            clear(scrollId);
        }
        return results;
    }
}