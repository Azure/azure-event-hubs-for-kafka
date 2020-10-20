using System.Collections.Generic;
using Newtonsoft.Json;

namespace ConsumerLaglibrdkafka
{
    public class ConsumerStatistics
    {
        [JsonProperty(PropertyName = "topics")]
        public Dictionary<string, TopicStatistic> Topics { get; set; }

        [JsonProperty(PropertyName = "brokers")]
        public Dictionary<string, BrokerStatistic> Brokers { get; set; }

        public ConsumerStatistics()
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
        [JsonProperty(PropertyName = "rtt")]
        public WindowStats Rtt { get; set; }
    }

    public class WindowStats
    {
        [JsonProperty(PropertyName = "min")]
        public int min;

        [JsonProperty(PropertyName = "max")]
        public int max;

        [JsonProperty(PropertyName = "avg")]
        public int avg;

        [JsonProperty(PropertyName = "sum")]
        public int sum;

        [JsonProperty(PropertyName = "cnt")]
        public int count;

    }
}