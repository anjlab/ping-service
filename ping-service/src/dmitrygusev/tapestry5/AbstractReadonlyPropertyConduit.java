/**
 * 
 */
package dmitrygusev.tapestry5;

import java.lang.annotation.Annotation;

import org.apache.tapestry5.PropertyConduit;

public abstract class AbstractReadonlyPropertyConduit implements PropertyConduit {
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