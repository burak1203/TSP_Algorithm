import java.util.*;

public class Population {
    public List<Tour> tours;

    public Population(List<City> cities, int size) {
        tours = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            tours.add(new Tour(cities));
        }
    }

    public Tour getFittest() {
        // Elit bireyi bulmak için sıralama yerine direkt karşılaştır
        Tour best = tours.get(0);
        for (int i = 1; i < tours.size(); i++) {
            if (tours.get(i).getDistance() < best.getDistance()) {
                best = tours.get(i);
            }
        }
        return best;
    }
}
