package org.opennms;

import java.net.InetAddress;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONObject;

import com.google.common.base.Strings;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;

public class Flow {
    private Map<String, Object> flowData = new TreeMap<String, Object>();

    public Flow(final JSONObject object, final DatabaseReader databaseReader) {
        flowData.put("timestamp", object.getLong("@timestamp"));
        flowData.put("exporter", object.getString("host"));
        flowData.put("json", object);

        for (final String key : object.keySet()) {
            if (key.startsWith("netflow.")) {
                flowData.put(key.substring(8), object.get(key));
            }
        }

        InetAddress srcAddress = null;
        InetAddress dstAddress = null;
        try {
            srcAddress = InetAddress.getByName(getString("src_addr"));
            dstAddress = InetAddress.getByName(getString("dst_addr"));

            if (!srcAddress.isLoopbackAddress()) {
                final CityResponse srcResponse = databaseReader.city(srcAddress);
                if (srcResponse != null) {
                    if (srcResponse.getCity() != null && !Strings.isNullOrEmpty(srcResponse.getCity().getName())) {
                        flowData.put("src_city", srcResponse.getCity().getName());
                    }

                    if (srcResponse.getCountry() != null && !Strings.isNullOrEmpty(srcResponse.getCountry().getName())) {
                        flowData.put("src_country", srcResponse.getCountry().getName());
                    }
                }
            }
            if (!dstAddress.isLoopbackAddress()) {
                final CityResponse dstResponse = databaseReader.city(dstAddress);
                if (dstResponse != null) {
                    if (dstResponse.getCity() != null && !Strings.isNullOrEmpty(dstResponse.getCity().getName())) {
                        flowData.put("dst_city", dstResponse.getCity().getName());
                    }

                    if (dstResponse.getCountry() != null && !Strings.isNullOrEmpty(dstResponse.getCountry().getName())) {
                        flowData.put("dst_country", dstResponse.getCountry().getName());
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public String getString(final String key) {
        if (!flowData.containsKey(key)) {
            return null;
        }
        return String.valueOf(flowData.get(key));
    }

    public Long getLong(final String key) {
        if (!flowData.containsKey(key)) {
            return null;
        }
        return Long.parseLong(flowData.get(key).toString());
    }

    public Map<String, Object> getFlowData() {
        return flowData;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Flow{");
        sb.append("map=").append(flowData);
        sb.append('}');
        return sb.toString();
    }
}
