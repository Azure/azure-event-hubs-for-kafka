using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Hosting;

namespace ProducerStatisticsLibrdKafka
{
    public class Worker : BackgroundService
    {
        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            await Task.WhenAll(
                Program.ConsumeMessages("my-client-1", "my-consumer-group-1", "evaskhub", 1000), 
                Program.ConsumeMessages("my-client-2", "my-consumer-group-2", "evaskhub", 900)
            );
        }
    }
}
