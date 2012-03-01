package com.anjlab.ping.components;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.tapestry5.annotations.BeforeRenderTemplate;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.beaneditor.BeanModel;
import org.apache.tapestry5.corelib.components.Grid;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.BeanModelSource;

import com.anjlab.tapestry5.AbstractReadonlyPropertyConduit;

@SuppressWarnings("rawtypes")
public class MapVisualizer {

    @Inject private BeanModelSource beanModelSource;
    @Inject private Messages messages;

    private int counter;
    
    @Parameter(required=true)
    private Map map;
    
    @Parameter(defaultPrefix="literal", value="Key")
    private String key;
    
    @Parameter(defaultPrefix="literal", value="Value")
    private String value;
    
    @Component(id="grid")
    private Grid grid;
    
    @BeforeRenderTemplate
    void beforeRender() {
        if (grid.getSortModel().getSortConstraints().isEmpty()) {
            grid.getSortModel().updateSort(value);  //  asc
            grid.getSortModel().updateSort(value);  //  desc
        }
    }
    
    public BeanModel<?> getModel() {
        BeanModel<?> beanModel = beanModelSource.createDisplayModel(Entry.class, messages);

        beanModel.add("SN", new AbstractReadonlyPropertyConduit() 
        {
            @Override 
            public Object get(Object instance) { 
                return ++counter;
            }
        }).sortable(false);
        beanModel.add(key, new AbstractReadonlyPropertyConduit() {
            @Override
            public Object get(Object instance) {
                return ((Entry) instance).getKey();
            }
        });
        beanModel.add(value, new AbstractReadonlyPropertyConduit() {
            @Override
            public Object get(Object instance) {
                return ((Entry) instance).getValue();
            }
        });

        return beanModel;
    }
    
    @SuppressWarnings("unused")
    @Property
    private Entry entry;
    
    @SuppressWarnings("unchecked")
    public Set<Entry> getEntries() {
        return map.entrySet();
    }
}
