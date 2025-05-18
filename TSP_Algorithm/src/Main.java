import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static final AtomicBoolean isRunning = new AtomicBoolean(true);

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Lütfen çözülecek dosya adını parametre olarak giriniz. Örn: tsp_318_2");
            return;
        }

        String file = args[0];
        String path = "data/" + file;

        // Bellek bilgilerini al ve görüntüle
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        System.out.println("--------------------------------------------------");
        System.out.println("Dosya işleniyor: " + file);
        System.out.println("Maksimum JVM belleği: " + maxMemory + " MB");

        long startTime = System.currentTimeMillis();

        // Sayaç başlatılıyor
        Thread timerThread = new Thread(() -> {
            while (isRunning.get()) {
                long current = System.currentTimeMillis();
                double seconds = (current - startTime) / 1000.0;
                System.out.printf("\rGeçen süre: %.2f saniye", seconds);
                try {
                    Thread.sleep(1000); // her saniyede bir güncelle
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        timerThread.setDaemon(true); // JVM'in kapanmasını engellemesin
        timerThread.start();

        try {
            // Dosya büyüklüğüne göre parametreleri ayarla
            List<City> cities = loadCities(path);
            int cityCount = cities.size();

            System.out.println("\nŞehir sayısı: " + cityCount);

            // Veri setinin büyüklüğüne göre parametreleri akıllıca ayarla
            int populationSize = calculatePopulationSize(cityCount, maxMemory);
            int generations = calculateGenerations(cityCount);
            double mutationRate = calculateMutationRate(cityCount);

            System.out.printf("\nPopülasyon boyutu: %d, Nesil sayısı: %d, Mutasyon oranı: %.4f\n",
                    populationSize, generations, mutationRate);

            // 10,000+ şehir için ekstra önlemler al
            if (cityCount > 10000) {
                System.out.println("Büyük veri seti tespit edildi. Bellek optimizasyonları uygulanıyor...");
                // Belleği temizle ve GC'yi çağır
                System.gc();
                City.clearCache();
            }

            // TSP hesaplama
            Population population = new Population(cities, populationSize);
            Tour best = GeneticAlgorithm.evolve(population, generations, mutationRate);

            long endTime = System.currentTimeMillis();
            double seconds = (endTime - startTime) / 1000.0;

            System.out.println("\nOptimal maliyet: " + best.getDistance());
            System.out.println("Path: " + best.getPath());
            System.out.printf("Toplam çözüm süresi: %.2f saniye\n", seconds);
            System.out.println("--------------------------------------------------");

            saveResults(file, best, seconds);
        } finally {
            // Temizlik işlemleri
            isRunning.set(false);
            GeneticAlgorithm.shutdown();
        }
    }

    private static int calculatePopulationSize(int cityCount, long maxMemoryMB) {
        // Bellek miktarına ve şehir sayısına göre popülasyon boyutunu belirle
        if (maxMemoryMB < 1024) { // 1GB'dan az bellek
            return Math.min(50, cityCount);
        } else if (maxMemoryMB < 4096) { // 1-4GB arası
            return Math.min(100, cityCount / 2);
        } else if (maxMemoryMB < 8192) { // 4-8GB arası
            return Math.min(200, cityCount / 3);
        } else { // 8GB+ bellek
            return Math.min(500, cityCount / 4);
        }
    }

    private static int calculateGenerations(int cityCount) {
        // Şehir sayısına göre nesil sayısını belirle
        if (cityCount <= 100)
            return 1000;
        if (cityCount <= 300)
            return 2000;
        if (cityCount <= 1000)
            return 1500;
        if (cityCount <= 5000)
            return 800;
        if (cityCount <= 10000)
            return 400;
        return 200; // Çok büyük veri seti için daha az nesil
    }

    private static double calculateMutationRate(int cityCount) {
        // Şehir sayısına göre mutasyon oranını belirle
        if (cityCount <= 100)
            return 0.05;
        if (cityCount <= 300)
            return 0.04;
        if (cityCount <= 1000)
            return 0.03;
        if (cityCount <= 5000)
            return 0.02;
        return 0.01; // Büyük veri setleri için daha düşük mutasyon
    }

    private static void saveResults(String file, Tour best, double seconds) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("results.txt", true));
        writer.write("Dosya: " + file + "\n");
        writer.write("Optimal maliyet: " + best.getDistance() + "\n");
        writer.write("Path: " + best.getPath() + "\n");
        writer.write(String.format("Çözüm süresi: %.2f saniye\n\n", seconds));
        writer.close();
    }

    public static List<City> loadCities(String fileName) throws IOException {
        List<City> cities = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            int size = Integer.parseInt(reader.readLine().trim());
            for (int i = 0; i < size; i++) {
                String[] parts = reader.readLine().trim().split("\\s+|,");
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                cities.add(new City(x, y, i));
            }
        }
        return cities;
    }
}
