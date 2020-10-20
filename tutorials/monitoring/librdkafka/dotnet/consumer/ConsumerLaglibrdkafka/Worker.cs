using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Hosting;

namespace ConsumerLaglibrdkafka
{
    public class Worker : BackgroundService
    {
        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            // starting two consumers with different delays
            await Task.WhenAll(
                Program.ConsumeMessages(1, 1000),
                Program.ConsumeMessages(2, 900)
            );
        }
    }
}
