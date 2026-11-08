import java.util.*;
import java.util.Collections;
import java.util.stream.Collector;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.function.Function;

public class TurningVehicleCollector<T> implements Collector<Vehicle, Map<Integer, EntityStore<Vehicle>>, Map<Integer, EntityStore<Vehicle>>>{
    @Override 
    public Supplier<Map<Integer, EntityStore<Vehicle>>> supplier(){
        return () -> new HashMap<Integer, EntityStore<Vehicle>>(){{
            put(0, new EntityStore<Vehicle>());
            put(1, new EntityStore<Vehicle>());
        }};
    }

    @Override 
    public BiConsumer<Map<Integer, EntityStore<Vehicle>>, Vehicle> accumulator(){
        return (Map<Integer, EntityStore<Vehicle>> map, Vehicle item) -> {
            if(item.getState() == item.getTurnLeftState()){
                map.get(0).add(item);
            }else if(item.getState() == item.getTurnRightState()){
                map.get(1).add(item);
            }};
    }

    @Override 
    public Function<Map<Integer, EntityStore<Vehicle>>, Map<Integer, EntityStore<Vehicle>>> finisher(){
        return Function.identity();
    }

    @Override 
    public BinaryOperator<Map<Integer, EntityStore<Vehicle>>> combiner(){
        return (Map<Integer, EntityStore<Vehicle>> map1, Map<Integer, EntityStore<Vehicle>> map2) -> {
            EntityStore.addAll(map1.get(0), map2.get(0));
            EntityStore.addAll(map1.get(1), map2.get(1));
            return map1;
        }; 
    }

    @Override 
    public Set<Characteristics> characteristics(){
        return Collections.unmodifiableSet(EnumSet.of(Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH));
    }
}
