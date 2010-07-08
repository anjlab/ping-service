package dmitrygusev.ping.entities;

import org.junit.Test;

import com.google.appengine.api.quota.QuotaService;
import com.google.appengine.api.quota.QuotaServiceFactory;

public class TestQuotas {

    @Test
    public void convertMegacyclesToSeconds() {
        QuotaService qs = QuotaServiceFactory.getQuotaService();
        System.out.println(qs.convertMegacyclesToCpuSeconds(620));
    }
}
