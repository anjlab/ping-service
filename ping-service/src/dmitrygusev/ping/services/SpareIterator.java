package dmitrygusev.ping.services;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SpareIterator<T, C extends Comparable<C>> implements Iterable<T>, Iterator<T> {

    private final List<T> items;

    private int counter;
    private int middle;
    private int correction;
    
    public SpareIterator(List<T> items, final Colorer<T, C> colorer) {
        this.items = items;
        this.counter = 0;
        this.middle = items.size() / 2;
        this.correction = items.size() % 2 == 0 ? 0 : 1;
        
        final Map<C, Integer> colorCount = new HashMap<C, Integer>();
        
        for (T item : items) {
            C color = colorer.getColor(item);
            Integer count = colorCount.get(color);
            count = count == null ? 1 : count + 1;
            colorCount.put(color, count);
        }
        
        Collections.sort(this.items, new Comparator<T>() {
            public int compare(T o1, T o2) {
                C c1 = colorer.getColor(o1);
                C c2 = colorer.getColor(o1);
                return colorCount.get(c1).compareTo(colorCount.get(c2));
            };
        });
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return counter < items.size();
    }

    @Override
    public T next() {
        int index;
        
        index = counter % 2 == 0 ? (counter / 2) : (middle + counter / 2 + correction);
        
        counter++;
        
        return items.get(index); 
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
}
