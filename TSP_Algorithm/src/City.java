import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class City {
    public double x;
    public double y;
    private int index;

    // Mesafe önbelleği (Cache) - Maksimum girdi sayısı sınırlı
    private static final Map<String, Double> distanceCache = new LinkedHashMap<String, Double>(10000, 0.75f, true) {
        private static final int MAX_ENTRIES = 100000;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Double> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public City(double x, double y) {
        this.x = x;
        this.y = y;
        this.index = -1;
    }

    public City(double x, double y, int index) {
        this.x = x;
        this.y = y;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double distanceTo(City city) {
        // Çok uzak indekslere sahip şehirler için önbellek kullanma
        // (Bellek optimizasyonu için)
        if (Math.abs(this.index - city.index) > 1000) {
            double dx = this.x - city.x;
            double dy = this.y - city.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        // Yakın şehirler için önbellekte bu mesafe var mı kontrol et
        String key = cacheKey(this, city);
        Double cachedDistance = distanceCache.get(key);

        if (cachedDistance != null) {
            return cachedDistance;
        }

        // Yoksa hesapla ve önbelleğe al
        double dx = this.x - city.x;
        double dy = this.y - city.y;
        double distance = Math.sqrt(dx * dx + dy * dy);

        distanceCache.put(key, distance);
        return distance;
    }

    // Önbelleği temizle (bellek sorunlarında çağrılabilir)
    public static void clearCache() {
        distanceCache.clear();
        System.out.println("Mesafe önbelleği temizlendi, bellek boşaltıldı.");
    }

    // Önbellek için benzersiz bir anahtar oluştur
    private static String cacheKey(City a, City b) {
        // Her zaman küçük indeksi önce koy (a->b ve b->a aynı mesafeyi verir)
        if (a.index < b.index) {
            return a.index + "-" + b.index;
        } else {
            return b.index + "-" + a.index;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        City other = (City) obj;
        return this.index == other.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index);
    }

    @Override
    public String toString() {
        return String.format("%d: (%d, %d)", index, (int) x, (int) y);
    }

    public String getName() {
        return index >= 0 ? String.valueOf(index) : String.format("(%d, %d)", (int) x, (int) y);
    }
}
