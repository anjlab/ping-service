package dmitrygusev.ping.services;

import org.apache.tapestry5.beaneditor.BeanModel;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.services.BeanModelSource;

public class BeanModelHelper {

    private BeanModelSource beanModelSource;

	public BeanModelHelper(BeanModelSource beanModelSource) {
		this.beanModelSource = beanModelSource;
	}

	public <T> BeanModel<T> createEditDeleteBeanModel(Class<T> clazz, Messages messages) {
    	BeanModel<T> model = beanModelSource.createDisplayModel(
    			clazz, messages);

        model.add("edit", null);
        model.add("delete", null);
        
        return model;
    }

}
