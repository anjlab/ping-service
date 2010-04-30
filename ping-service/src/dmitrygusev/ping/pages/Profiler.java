package dmitrygusev.ping.pages;

import static dmitrygusev.ping.services.profiler.ProfilingAdvice.dumpCallStack;
import static dmitrygusev.ping.services.profiler.ProfilingAdvice.dumpPlainMetrics;
import static dmitrygusev.ping.services.profiler.ProfilingAdvice.getLeaf;
import static java.lang.Integer.toHexString;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tapestry5.PropertyConduit;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.beaneditor.BeanModel;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.BeanModelSource;

import dmitrygusev.ping.services.profiler.Leaf;
import dmitrygusev.ping.services.profiler.ProfilerMetric;
import dmitrygusev.ping.services.profiler.ProfilingAdvice;

public class Profiler {

    @Property private String signature;
    @Property private Map<String, Leaf> metrics;
    
    private List<String> context;
    
    public void onActivate(String context) {
        this.items.clear();
        this.context = new ArrayList<String>();
        if (context != null) {
            if (!context.contains("-")) {
                this.context.add(context);
            } else {
                for (String string : context.split("\\-")) {
                    this.context.add(string);
                }
            }
        }
    }

    public String onPassivate() {
        return listToWebString(context);
    }

    private String listToWebString(List<String> list) {
        return list == null ? "" : list.toString().replaceAll("\\[|\\]", "").replaceAll(", ", "-");
    }

    public Set<String> getMethods() {
        if ("callstack".equals(getView())) {
            metrics = dumpCallStack(getTracePath());
        } else {
            metrics = dumpPlainMetrics();
        }
        return metrics.keySet();
    }

    public List<String> getTracePath() {
        return context == null || context.size() <= 1
             ? Collections.<String>emptyList()
             : context.subList(1, context.size());
    }

    private String getView() {
        return context == null || context.size() == 0 
             ? null 
             : context.get(0);
    }

    @Inject private BeanModelSource beanModelSource;
    @Inject private Messages messages;
    
    public BeanModel<?> getModel() {
        BeanModel<?> beanModel = beanModelSource.createDisplayModel(String.class, messages);
        beanModel.add("signature", new AbstractPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                return instance; 
            }
        });
        beanModel.add("invocationCount", new AbstractPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                return metrics.get(instance).getMetric().invocationCount; 
            }
        });
        beanModel.add("totalTimeMillis", new AbstractPropertyConduit()
        {
            @Override 
            public Object get(Object instance) { 
                return metrics.get(instance).getMetric().totalTimeMillis; 
            }
        });
        beanModel.add("avgTimeMillis", new AbstractPropertyConduit()
        {
            @Override 
            public Object get(Object instance) { 
                ProfilerMetric metric = metrics.get(instance).getMetric();
                if (metric.totalTimeMillis == 0) {
                    return 0;
                }
                return (int) (metric.totalTimeMillis / metric.invocationCount); 
            }
        });
        if ("callstack".equals(getView())) {
            beanModel.add("drilldown", null);
        } else {
            beanModel.add("traceInto", null);
        }
        beanModel.exclude("empty");
        return beanModel;
    }
    
    private abstract class AbstractPropertyConduit implements PropertyConduit {
        @Override
        public <T extends Annotation> T getAnnotation(Class<T> arg0) {
            return null;
        }

        @Override
        public void set(Object instance, Object value) {
        }

        @Override
        public Class<?> getPropertyType() {
            return String.class;
        }
    }
    
    @Property private String item;
    
    /*
     * Call stack path
     */
    private List<String> items = new ArrayList<String>();
    
    public Leaf getItemLeaf() {
        if ("callstack".equals(item)) {
            return new Leaf(item, null);
        }
        
        items.add(item);
        
        Leaf leaf = getLeaf(items);
        
        if (leaf == null) {
            leaf = new Leaf(item, null);
        }
        
        return leaf;
    }
    

    public String getItemContext() {
        List<String> tracePath = new ArrayList<String>();
        tracePath.add("callstack");
        tracePath.addAll(items);
        return listToWebString(tracePath);
    }
    
    public int getItemPaddingLeft() {
        return items.size() * 20;
    }
    
    /*
     * End of call stack path
     */
    
    public String getTraceIntoContext() {
        Leaf leaf = metrics.get(signature);
        
        List<String> tracePath = new ArrayList<String>(); 
        
        tracePath.add(toHexString(leaf.getMethodName().hashCode()));
        
        while ((leaf = leaf.getParent()) != null) {
            tracePath.add(0, toHexString(leaf.getMethodName().hashCode()));
        }
        
        tracePath.add(0, "callstack");
        
        return listToWebString(tracePath);
    }

    public String getDrilldownContext() {
        List<String> context = new ArrayList<String>();
        for (String string : this.context) {
            context.add(string);
        }
        context.add(toHexString(signature.hashCode()));
        return listToWebString(context);
    }
    
    public void onActionFromResetStatistics() {
        ProfilingAdvice.clear();
    }
}
