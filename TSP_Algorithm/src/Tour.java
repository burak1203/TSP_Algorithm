import java.util.*;

public class Tour {
    public List<City> cities;
    private double distance;
    private static final Random rand = new Random(); // Singleton Random nesnesi

    public Tour(List<City> cities) {
        this.cities = new ArrayList<>(cities);
        Collections.shuffle(this.cities, rand);
    }

    public Tour(List<City> cities, boolean shuffle) {
        this.cities = new ArrayList<>(cities);
        if (shuffle)
            Collections.shuffle(this.cities, rand);
    }

    public double getDistance() {
        if (distance == 0) {
            double total = 0;
            int size = cities.size();
            for (int i = 0; i < size; i++) {
                City from = cities.get(i);
                City to = cities.get((i + 1) % size); // son şehirden ilk şehire dönüş
                total += from.distanceTo(to);
            }
            distance = total;
        }
        return distance;
    }

    public void mutate() {
        double mutationType = rand.nextDouble();

        // Mesafe önbelleğini sıfırlayın çünkü tur değişecek
        distance = 0;

        // %50 swap, %50 reverse
        if (mutationType < 0.5) {
            // Swap mutasyonu: İki rastgele şehrin yerini değiştirme
            int size = cities.size();
            int i = rand.nextInt(size);
            int j = rand.nextInt(size);

            if (i != j) { // Aynı şehirleri değiştirmiyorsa devam et
                Collections.swap(cities, i, j);
            }
        } else {
            // Reverse mutasyonu: Bir segment'i tersine çevirme
            int size = cities.size();
            int i = rand.nextInt(size);
            int j = rand.nextInt(size);

            if (i != j) { // Gerçekten bir segment varsa devam et
                int start = Math.min(i, j);
                int end = Math.max(i, j);

                // alt segmenti tersine çevir
                Collections.reverse(cities.subList(start, end));
            }
        }
    }

    public static Tour crossover(Tour parent1, Tour parent2) {
        // Order Crossover (OX) yaklaşımını optimize et
        int size = parent1.cities.size();
        List<City> child = new ArrayList<>(Collections.nCopies(size, null));

        // Rastgele bir alt segment seç
        int start = rand.nextInt(size);
        int end = rand.nextInt(size);

        if (start == end) {
            // Segment çok küçük, basit bir kopya oluştur
            return new Tour(parent1.cities, true);
        }

        int lower = Math.min(start, end);
        int upper = Math.max(start, end);

        // Segment doğrudan parent1'den kopyala
        for (int i = lower; i < upper; i++) {
            child.set(i, parent1.cities.get(i));
        }

        // Oluşturulan çocuk turunda zaten olan şehirleri izlemek için HashSet kullan
        Set<City> childCities = new HashSet<>();
        for (int i = lower; i < upper; i++) {
            childCities.add(child.get(i));
        }

        // Diğer şehirleri parent2'den ekle
        int currentIdx = upper % size;
        for (int i = 0; i < size; i++) {
            City city = parent2.cities.get((upper + i) % size);
            if (!childCities.contains(city)) {
                child.set(currentIdx, city);
                currentIdx = (currentIdx + 1) % size;

                // Dizinin sınırlarını kontrol et
                if (currentIdx == lower) {
                    break; // Tüm boşluklar doldu
                }
            }
        }

        return new Tour(child, false);
    }

    public String getPath() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cities.size(); i++) {
            sb.append(cities.get(i).getIndex());
            if (i != cities.size() - 1)
                sb.append(" -> ");
        }
        return sb.toString();
    }

    public void resetDistance() {
        this.distance = 0;
    }
}
