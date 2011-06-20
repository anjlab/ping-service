package dmitrygusev.ping.pages.task;

import javax.cache.Cache;

import org.apache.tapestry5.annotations.Meta;
import org.apache.tapestry5.ioc.annotations.Inject;

import dmitrygusev.ping.services.AppModule;

@Meta(AppModule.NO_MARKUP)
public class ClearCacheTask {

    @Inject
    private Cache cache;
    
    public void onActivate() {
        cache.clear();
    }
    
}
