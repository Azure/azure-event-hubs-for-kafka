using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Hosting;

namespace ProducerStatisticsLibrdKafka
{
    public class Worker : BackgroundService
    {
        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            // Starting two producers
            await Task.WhenAll(
                Program.ProduceMessages(1000), 
                Program.ProduceMessages(900)
            );
        }
    }
}
