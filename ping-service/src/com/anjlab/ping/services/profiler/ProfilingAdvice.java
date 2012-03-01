package com.anjlab.ping.services.profiler;

import static java.lang.Integer.toHexString;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.tapestry5.plastic.MethodAdvice;
import org.apache.tapestry5.plastic.MethodInvocation;

public class ProfilingAdvice implements MethodAdvice {

    private static final Map<String, Leaf> topTraces = new HashMap<String, Leaf>();

    private static final Stack<Leaf> stack = new Stack<Leaf>();

    private String className;
    
    public ProfilingAdvice(String className) {
        this.className = className;
    }
    
    public void advise(MethodInvocation invocation) {
        long startTime = 0;
        String signature = getMethodSignature(className, invocation);
        try
        {
            Leaf leaf;
            
            Leaf root = stack.isEmpty() ? null : stack.peek();
            if (root == null) {
                if (!topTraces.containsKey(signature)) {
                    topTraces.put(signature, new Leaf(signature, new ProfilerMetric()));
                }
                leaf = topTraces.get(signature);
            } else {
                if (!root.hasChild(signature)) {
                    leaf = new Leaf(signature, new ProfilerMetric());
                    root.addChild(leaf);
                }
                leaf = root.getChild(signature);
            }
            
            stack.push(leaf);
            
            startTime = System.currentTimeMillis();
            invocation.proceed();
        }
        finally
        {
            long endTime = System.currentTimeMillis();

            Leaf leaf = stack.pop();
            
            leaf.getMetric().invocationCount++;
            leaf.getMetric().totalTimeMillis += endTime - startTime;
        }
    }

    private String getMethodSignature(final String intfName, MethodInvocation invocation) {
        StringBuilder builder = new StringBuilder();
        builder.append(intfName);
        builder.append(".");
        builder.append(invocation.getMethod().getName());
        builder.append("(");
        for (int i = 0; i < invocation.getMethod().getParameterTypes().length; i++) {
            builder.append(invocation.getMethod().getParameterTypes()[i].getSimpleName());
            builder.append(",");
        }
        if (invocation.getMethod().getParameterTypes().length > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append(")");
        return builder.toString();
    }

    public static Map<String, Leaf> dumpPlainMetrics() {
        Map<String, Leaf> result = new HashMap<String, Leaf>();
        
        addMetrics(result, topTraces.values());
        
        return result;
    }
    
    private static void addMetrics(Map<String, Leaf> result, Collection<Leaf> leafs) {
        for (Leaf leaf : leafs) {
            result.put(leaf.getMethodName(), leaf);
            addMetrics(result, leaf.getChildren().values());
        }
    }
    
    public static Map<String, Leaf> dumpCallStack(List<String> tracePath) {
        Map<String, Leaf> result = new HashMap<String, Leaf>();

        Leaf leaf = getLeaf(tracePath);

        if (leaf == null) {
            for (String key : topTraces.keySet()) {
                result.put(key, topTraces.get(key));
            }
            return result;
        }
        
        Map<String, Leaf> slice = leaf.getChildren();

        for (String key : slice.keySet()) {
            result.put(key, slice.get(key));
        }
        
        return result;
    }

    public static Leaf getLeaf(List<String> tracePath) {
        Leaf leaf = null;
        
        Map<String, Leaf> slice = topTraces;
        for (String code : tracePath) {
            leaf = null;
            for (String key : slice.keySet()) {
                if (toHexString(key.hashCode()).equals(code)) {
                    leaf = slice.get(key);
                    break;
                }
            }
            if (leaf == null) {
                return null;
            }
            slice = leaf.getChildren();
        }
        return leaf;
    }

    public static void clear() {
        topTraces.clear();
    }

}
