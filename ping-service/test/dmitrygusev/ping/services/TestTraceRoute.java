package dmitrygusev.ping.services;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import dmitrygusev.ping.pages.TraceRoute;

public class TestTraceRoute {

   @Test
   public void testRegex() {
       String traceRoute = 
       " 1     4 ms     1 ms     1 ms  192.168.1.1\n" +
       " 2     3 ms     6 ms     9 ms  192.168.111.1\n" +
       " 3     *        *        *     Превышен интервал ожидания для запроса.\n" +
       " 4   255 ms     8 ms     5 ms  87.245.244.149\n" +
       " 5   203 ms    21 ms     5 ms  87.245.232.33\n" +
       " 6     7 ms    15 ms     8 ms  74.125.51.241\n" +
       " 7   252 ms    42 ms    29 ms  72.14.236.248\n" +
       " 8   314 ms   339 ms    99 ms  209.85.242.188\n" +
       " 9    76 ms    58 ms    56 ms  216.239.43.127\n" +
       "10    65 ms    95 ms    62 ms  209.85.255.166\n" +
       "11     *       63 ms     *     209.85.255.98\n" +
       "12    55 ms    55 ms    55 ms  74.125.77.141";
       
       List<String> ips = TraceRoute.extractIPs(traceRoute);
       
       Assert.assertEquals(11, ips.size());
   }
    
}
