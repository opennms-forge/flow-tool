package org.opennms;

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.Test;

import com.maxmind.geoip2.DatabaseReader;

public class FlowTest {

    @Test
    public void testJsonToFlow() throws Exception {
        final String json = "{\"netflow.delta_switched\":1686042018786,\"netflow.first_switched\":1686042018786,\"node_exporter\":{\"foreign_id\":\"AI-SRX1500\",\"interface_id\":59007,\"foreign_source\":\"Network\",\"categories\":[\"Routers\",\"Production\",\"Critical\",\"Notification\"],\"node_id\":625},\"netflow.src_addr_hostname\":\"opennms.informatik.hs-fulda.de\",\"netflow.version\":\"Netflow v9\",\"netflow.dscp\":0,\"netflow.ip_protocol_version\":4,\"netflow.ecn\":0,\"netflow.sampling_algorithm\":\"RandomNoutOfNSampling\",\"netflow.dst_addr_hostname\":\"dns.google\",\"netflow.dst_as\":0,\"netflow.tos\":0,\"node_src\":{\"foreign_id\":\"Internet-Connectivity\",\"interface_id\":427,\"foreign_source\":\"Datacenter\",\"categories\":[\"Production\",\"University Datacenter\",\"Notification\"],\"node_id\":101},\"netflow.dst_locality\":\"public\",\"netflow.dst_addr\":\"8.8.8.8\",\"@version\":1,\"host\":\"193.174.29.33\",\"netflow.flow_seq_num\":54698845,\"netflow.sampling_interval\":1,\"netflow.protocol\":17,\"netflow.direction\":\"ingress\",\"netflow.output_snmp\":608,\"netflow.bytes\":118,\"netflow.application\":\"domain\",\"netflow.packets\":1,\"hosts\":[\"8.8.8.8\",\"193.174.29.56\"],\"netflow.tcp_flags\":0,\"@clock_correction\":0,\"netflow.next_hop\":\"192.108.48.33\",\"netflow.flow_records\":24,\"netflow.dst_mask_len\":32,\"netflow.last_switched\":1686042067022,\"netflow.src_addr\":\"193.174.29.56\",\"netflow.src_as\":0,\"@timestamp\":1686042069000,\"netflow.convo_key\":\"[\\\"Default\\\",17,\\\"193.174.29.56\\\",\\\"8.8.8.8\\\",\\\"domain\\\"]\",\"netflow.src_port\":41496,\"netflow.src_mask_len\":27,\"netflow.src_locality\":\"public\",\"location\":\"Default\",\"netflow.input_snmp\":589,\"netflow.flow_locality\":\"public\",\"node_dst\":{\"foreign_id\":\"vm-243\",\"interface_id\":12860,\"foreign_source\":\"SWLab-vCenter\",\"categories\":[\"VMware8\",\"VMware7\",\"Servers\",\"Notification\"],\"node_id\":305},\"netflow.dst_port\":53}";
        final DatabaseReader databaseReader = new DatabaseReader.Builder(FlowTool.class.getClassLoader().getResourceAsStream("GeoLite2-City.mmdb")).build();
        final Flow flow = new Flow(new JSONObject(json), databaseReader);
        assertEquals("1686042018786", flow.getString("delta_switched"));
        assertEquals("1686042018786", flow.getString("first_switched"));
        assertEquals("1686042069000", flow.getString("timestamp"));
        assertEquals("193.174.29.33", flow.getString("exporter"));
        assertEquals("8.8.8.8", flow.getString("dst_addr"));
        assertEquals(json, flow.getString("json"));
        assertEquals("Fulda", flow.getString("src_city"));
        assertEquals("Germany", flow.getString("src_country"));
        assertEquals(null, flow.getString("dst_city"));
        assertEquals("United States", flow.getString("dst_country"));
    }
}
