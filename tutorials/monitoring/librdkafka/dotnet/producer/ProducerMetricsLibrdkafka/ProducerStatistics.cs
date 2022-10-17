using System.Collections.Generic;
using Newtonsoft.Json;

namespace ProducerStatisticsLibrdKafka
{
    public class ProducerStatistics
    {
        [JsonProperty(PropertyName = "topics")]
        public Dictionary<string, TopicStatistic> Topics { get; set; }


        [JsonProperty(PropertyName = "brokers")]
        public Dictionary<string, BrokerStatistic> Brokers { get; set; }

        public ProducerStatistics()
        {
            Topics = new Dictionary<string, TopicStatistic>();
            Brokers = new Dictionary<string, BrokerStatistic>();
        }
    }

    public class TopicStatistic
    {
        [JsonProperty(PropertyName = "partitions")]
        public Dictionary<string, PartitionStatistic> Partitions { get; set; }

        public TopicStatistic()
        {
            Partitions = new Dictionary<string, PartitionStatistic>();
        }
    }

    public class PartitionStatistic
    {
        [JsonProperty(PropertyName = "consumer_lag")]
        public long ConsumerLag { get; set; }
    }

    public class BrokerStatistic
    {
        [JsonProperty(PropertyName = "tx")]
        public long Tx { get; set; }

        [JsonProperty(PropertyName = "txbytes")]
        public long TxBytes { get; set; }

        [JsonProperty(PropertyName = "txerrs")]
        public long TxErrs { get; set; }

        [JsonProperty(PropertyName = "txretries")]
        public long TxRetries { get; set; }

        [JsonProperty(PropertyName = "req_timeouts")]
        public long ReqTimeouts { get; set; }
    }
}