# OpenNMS Flow Tool

Small utility to query the flows persisted by OpenNMS from an Elasticsearch cluster.

In order to use the tool, download `GeoLite2-City.mmdb` from Maxmind and place it in the `src/main/resources directory`. After that you can build by entering:

    $ mvn install

and after that, execute the utility by entering:

    $ ./flow-tool.sh

Command line options:

    -delay N       : delay between search attempts (Vorgabe: 250)
    -delimiter VAL : delimiter separating columns
    -duration N    : duration to search for (Vorgabe: 60000)
    -end N         : search for flows older than end
    -fields VAL    : fields to be displayed (Vorgabe: first_switched,
                     last_switched, src_addr, src_port, dst_addr, dst_port,
                     direction, bytes, packets)
    -filter VAL    : filter for flows (Vorgabe: dst_addr?.startsWith("2001:638"))
    -help          : show this help (Vorgabe: true)
    -host VAL      : elastic search host (Vorgabe: localhost)
    -port N        : elastic search port (Vorgabe: 9200)
    -start N       : search for flows newer than start

Easiest way is to ssh into your ElasticSearch box and forward traffic to the ElasticSearch port:

    $ ssh -L9200:localhost:9200 user@elastic.search.host

Use `start`/`end` and `duration` to specify the time range of the flows.
If `end` isn't specified and even not implicit set by `start + duration` the utility continues to poll for more recent flows.

    start/end set      : from start to end
    start/duration set : from start to (start + duration)
    end/duration set   : from (end - duration) to end
    start set          : from start to now, polling for more
    duration set       : from (now - duration), polling for more
    nothing set        : from now, polling for more

Examples:

Get the flows from a minute ago and polling for more:

    $ ./flow-tool.sh -duration 60000
    1686049161469 | 1686049180724 | 2001:638:301:27fd:250:56ff:ae9f:3747 | 49001 | 2600:1f14:9f3:300:53f4:dc0c:da39:179b  | 6881  | ingress | 168 | 1
    1686049161693 | 1686049175531 | 2001:638:301:2cfd:0:0:0:64           | 49001 | 2804:3a8:3eda:f600:6d69:58c1:e3de:d568 | 42869 | ingress | 168 | 1
    1686049161713 | 1686049175232 | 2401:4900:4b57:1c9d:0:0:102a:e983    | 35627 | 2001:638:401:27fd:0:0:0:64             | 49001 | ingress | 205 | 1
    1686049161713 | 1686049179655 | 2001:638:301:27cd:250:56ff:fe9f:3747 | 49001 | 2a05:d014:eba:800:dfea:3dee:6694:7b9   | 6892  | ingress | 168 | 1
    [...]

Get the current flows matching a specified JEXL expression:

    $ ./flow-tool.sh -filter 'dst_addr?.startsWith("2001:638")'
    1686049376644 | 1686049376644 | 2a00:23c7:4681:3101:8679:fce:1ee0:9c8e | 6881  | 2001:638:401:201f:250:56ff:fe9f:93d | 49001 | ingress | 472 | 1
    1686049376644 | 1686049376644 | 2a00:23c7:4681:3101:8679:fce:1ee0:9c8e | 6881  | 2001:638:401:27fd:0:0:0:6           | 39683 | ingress | 472 | 1
    1686049327586 | 1686049327586 | 2a02:238:f017:b0:2472:1f22:8615:5746   | 56658 | 2001:638:401:201f:250:56ff:fe9f:93d | 33666 | ingress | 72  | 1
