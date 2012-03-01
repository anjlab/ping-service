package com.anjlab.ping.entities;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.anjlab.ping.services.Colorer;
import com.anjlab.ping.services.SpareIterator;


public class TestSpareIterator {

    @SuppressWarnings("unused")
    private class Bean {
        private String color;
        private String value;
        
        public String getColor() {
            return color;
        }
        public void setColor(String color) {
            this.color = color;
        }
        public String getValue() {
            return value;
        }
        public void setValue(String value) {
            this.value = value;
        }
        
        @Override
        public String toString() {
            return color + "/" + value;
        }
    }

    @Test
    public void testIterator2() {
        List<Bean> items = getEvery15Minutes();
        
        System.out.println(items.size());
        
        SpareIterator<Bean, String> iterator = new SpareIterator<Bean, String>(items, getBeanColorer());
        
        for (Bean bean : iterator) {
            System.out.println(bean);
        }
        
        System.out.println("=====");
        
        items = getEvery5Minutes();
        
        System.out.println(items.size());
        
        iterator = new SpareIterator<Bean, String>(items, getBeanColorer());
        
        for (Bean bean : iterator) {
            System.out.println(bean);
        }        
        System.out.println("=====");

        items = getEvery1Hours();
        
        System.out.println(items.size());
        
        iterator = new SpareIterator<Bean, String>(items, getBeanColorer());
        
        for (Bean bean : iterator) {
            System.out.println(bean);
        }
    }
    
    private Colorer<Bean, String> getBeanColorer() {
        return new Colorer<Bean, String>() {
            @Override
            public String getColor(Bean bean) {
                return bean.getColor();
            }
        };
    }

    private List<Bean> getEvery5Minutes() {
        
        String[] values = {
            "60", "1",
            "60", "1001",
            "60", "2001",
            "1026", "7",
            "1026", "2001",
            "1026", "2002",
            "281580", "1",
            "340461", "1",
            "340461", "1001",
            "392146", "1",
            "392146", "1001",
            "392146", "2001",
            "427502", "1",
            "427502", "1001",
            "429796", "1",
            "440639", "1",
            "440639", "3",
            "440639", "1001",
            "440839", "1",
            "441442", "1",
            "441442", "2",
            "441442", "3",
            "441442", "1001",
            "441442", "1002",
            "441538", "1",
            "564371", "1",
            "580304", "1",
            "662711", "1",
            "662711", "2",
            "662711", "3",
            "662711", "1004",
            "1523202", "1",
            "1523202", "3001",
            "1523202", "4001",
            "1600279", "1",
            "1600279", "1001"
        };
        
        List<Bean> result = new ArrayList<Bean>();
        
        for (int i = 0; i < values.length; i += 2) {
            Bean bean = new Bean();
            
            bean.setColor(values[i].trim());
            bean.setValue(values[i + 1].trim());
            
            result.add(bean);
        }
        
        return result;
    }
    
    private List<Bean> getEvery15Minutes() {
        
        String[] values = {
                "1026", "3",
                "1026", "4",
                "1026", "5001",
                "1026", "5002",
                "343033", "1",
                "392146", "2",
                "440593", "1",
                "440639", "4",
                "441548", "1",
                "441887", "2",
                "444171", "2",
                "444171", "1001",
                "444171", "1002",
                "1625787", "1",
                "1678780", "1"
        };
        
        List<Bean> result = new ArrayList<Bean>();
        
        for (int i = 0; i < values.length; i += 2) {
            Bean bean = new Bean();
            
            bean.setColor(values[i].trim());
            bean.setValue(values[i + 1].trim());
            
            result.add(bean);
        }
        
        return result;
    }
    
    private List<Bean> getEvery1Hours() {
        
        String[] values = {
                "28", "1",
                "41", "1",
                "48", "1",
                "62", "1",
                "1026", "2",
                "1026", "6",
                "1026", "3001",
                "1026", "6001",
                "1026", "8002",
                "1026", "9001",
                "1026", "10001",
                "1026", "11001",
                "1026", "12001",
                "1026", "13001",
                "340517", "1001",
                "815209", "1",
                "815209", "2001"
            };
        
        List<Bean> result = new ArrayList<Bean>();
        
        for (int i = 0; i < values.length; i += 2) {
            Bean bean = new Bean();
            
            bean.setColor(values[i].trim());
            bean.setValue(values[i + 1].trim());
            
            result.add(bean);
        }
        
        return result;
    }
}
