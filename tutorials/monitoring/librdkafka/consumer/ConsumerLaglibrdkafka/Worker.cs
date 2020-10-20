using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Hosting;

namespace ConsumerLaglibrdkafka
{
    public class Worker : BackgroundService
    {
        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            //starting two consumers from two different consumer groups
            await Task.WhenAll(
                Program.ConsumeMessages("client-1", "consumer-group-1", "evaskhub", 1000),
                Program.ConsumeMessages("client-2", "consumer-group-2", "evaskhub", 900)
            );
        }
    }
}
