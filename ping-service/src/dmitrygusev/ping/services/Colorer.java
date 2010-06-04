package dmitrygusev.ping.services;

public interface Colorer<T, C> {

    public C getColor(T item);
    
}
