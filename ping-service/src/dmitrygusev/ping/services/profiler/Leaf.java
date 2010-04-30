package dmitrygusev.ping.services.profiler;

import java.util.HashMap;
import java.util.Map;

public class Leaf
{
    private Leaf parent;
    private ProfilerMetric metric;
    private String methodName;
    private Map<String, Leaf> children = new HashMap<String, Leaf>();
    public Leaf(String methodName, ProfilerMetric metric) {
        this.methodName = methodName;
        this.metric = metric;
    }
    public String getMethodName() {
        return methodName;
    }
    public void addChild(Leaf child) {
        children.put(child.getMethodName(), child);
        child.parent = this;
    }
    public ProfilerMetric getMetric() {
        return metric;
    }
    @Override
    public int hashCode() {
        return methodName.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Leaf)) {
            return false;
        }
        Leaf o = (Leaf) obj;
        return methodName.equals(o.methodName);
    }
    public boolean hasChild(String signature) {
        return children.containsKey(signature);
    }
    public Leaf getChild(String signature) {
        return children.get(signature);
    }
    public Map<String, Leaf> getChildren() {
        return children;
    }
    public Leaf getParent() {
        return parent;
    }
}