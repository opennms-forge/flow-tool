package org.opennms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class FlowSearchTest {
    Long timestamp = null;

    private JSONArray createHits(final long start, final long end, final int num) {
        if (timestamp == null) {
            timestamp = start;
        }

        final JSONArray array = new JSONArray();

        if (timestamp > end) {
            return array;
        }

        for (int i = 0; i < num; i++) {
            final JSONObject jsonObject = new JSONObject();
            jsonObject.put("_source", new JSONObject("{\"netflow.delta_switched\":100,\"netflow.first_switched\":" + (timestamp - 100) + ",\"node_exporter\":{\"foreign_id\":\"AI-FOOBAR\",\"interface_id\":56571,\"foreign_source\":\"Network\",\"categories\":[\"Routers\",\"Production\",\"Critical\",\"Notification\"],\"node_id\":625},\"netflow.version\":\"Netflow v9\",\"netflow.dscp\":0,\"netflow.ip_protocol_version\":6,\"netflow.ecn\":0,\"netflow.sampling_algorithm\":\"RandomNoutOfNSampling\",\"netflow.dst_as\":0,\"netflow.tos\":0,\"netflow.dst_locality\":\"public\",\"netflow.dst_addr\":\"2001:638:3e1:27fd:0:0:0:6\",\"@version\":1,\"host\":\"2001:638:3e1:27fe:0:0:0:1\",\"netflow.flow_seq_num\":56437282,\"netflow.sampling_interval\":1,\"netflow.protocol\":6,\"netflow.direction\":\"ingress\",\"netflow.output_snmp\":611,\"netflow.bytes\":288,\"netflow.packets\":4,\"hosts\":[\"2001:638:3e1:27fd:0:0:0:6\",\"2400:2653:c2a1:ad00:140d:288d:d558:c598\"],\"netflow.tcp_flags\":2,\"@clock_correction\":0,\"netflow.next_hop\":\"0:0:0:0:0:0:0:0\",\"netflow.flow_records\":15,\"netflow.dst_mask_len\":64,\"netflow.last_switched\":" + (timestamp - 80) + ",\"netflow.src_addr\":\"2400:2653:c2a1:ad00:140d:288d:d558:c598\",\"netflow.src_as\":0,\"@timestamp\":" + timestamp + ",\"netflow.convo_key\":\"[\\\"Default\\\",6,\\\"2001:638:3e1:27fd:0:0:0:6\\\",\\\"2400:2653:c2a1:ad00:140d:288d:d558:c598\\\",null]\",\"netflow.src_port\":52896,\"netflow.src_mask_len\":128,\"netflow.src_locality\":\"public\",\"location\":\"Default\",\"netflow.input_snmp\":608,\"netflow.flow_locality\":\"public\",\"netflow.dst_port\":41469}"));
            array.put(i, jsonObject);
            timestamp += 35;
            if (timestamp > end) {
                break;
            }
        }
        return array;
    }

    @Test
    public void testApp() {
        final long start = 1000;
        final long end = 10000;

        final FlowSearch flowSearch = new FlowSearch("localhost", 9200, start, end) {
            @Override
            protected JSONObject search() {
                final JSONObject jsonObject1 = new JSONObject();
                jsonObject1.put("hits", createHits(start, end, 10));
                final JSONObject jsonObject2 = new JSONObject();
                jsonObject2.put("_scroll_id", "fooBar");
                jsonObject2.put("hits", jsonObject1);
                return jsonObject2;
            }

            @Override
            protected JSONObject scroll(String scrollId) {
                assertEquals("fooBar", scrollId);
                final JSONObject jsonObject1 = new JSONObject();
                jsonObject1.put("hits", createHits(start, end, 10));
                final JSONObject jsonObject2 = new JSONObject();
                jsonObject2.put("hits", jsonObject1);
                return jsonObject2;
            }
        };

        final List<Flow> flows = flowSearch.getFlows();
        assertEquals(258, flows.size());

        for(final Flow flow : flows) {
            assertTrue(flow.getLong("timestamp") >= start);
            assertTrue(flow.getLong("timestamp") < end);
        }
    }
}
