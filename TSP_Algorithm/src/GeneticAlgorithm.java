import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class GeneticAlgorithm {
    private static final int TOURNAMENT_SIZE = 5;
    private static final Random rand = new Random(); // Singleton Random nesnesi
    private static final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    public static Tour evolve(Population pop, int generations, double mutationRate) {
        int elitismCount = 20;
        Tour bestSoFar = pop.getFittest();

        // Büyük veri setleri için erken sonlandırma parametreleri
        int noImprovementCount = 0;
        int maxNoImprovement = generations / 10; // Toplam nesil sayısının %10'u kadar iyileşme olmazsa
        double lastBestDistance = Double.MAX_VALUE;
        double improvementThreshold = 0.0001; // %0.01 iyileşme eşiği

        // Önceden şehirler arası mesafeleri hesapla (cache)
        for (Tour tour : pop.tours) {
            tour.getDistance();
        }

        // Başlangıç popülasyonunu iyileştir
        initializePopulationWithHeuristicTours(pop);

        // Nesiller boyunca evrim
        for (int gen = 0; gen < generations; gen++) {
            List<Tour> newTours = new ArrayList<>();

            // Elit bireyleri doğrudan yeni nesle ekle (referansla değil, kopya olarak)
            pop.tours.sort(Comparator.comparingDouble(Tour::getDistance));
            for (int i = 0; i < elitismCount; i++) {
                Tour elite = new Tour(pop.tours.get(i).cities, false); // kopyasını al
                newTours.add(elite);
            }

            // Yeni bireyleri üret (paralel olarak)
            int newToursNeeded = pop.tours.size() - elitismCount;
            CountDownLatch latch = new CountDownLatch(newToursNeeded);
            List<Tour> parallelTours = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < newToursNeeded; i++) {
                executor.submit(() -> {
                    try {
                        Tour parent1 = select(pop);
                        Tour parent2 = select(pop);

                        // Crossover ve mutasyon
                        Tour child = Tour.crossover(parent1, parent2);
                        if (rand.nextDouble() < mutationRate) {
                            child.mutate();
                        }

                        // Büyük turlar için yerel optimizasyon
                        if (child.cities.size() > 1000) {
                            localOptimization(child);
                        }

                        parallelTours.add(child);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await(30, TimeUnit.SECONDS); // Maksimum 30 saniye bekle
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            newTours.addAll(parallelTours);

            // Yeni nesli popülasyona ata
            pop.tours = newTours;

            // En iyi çözüm güncellemesi
            Tour currentBest = pop.getFittest();
            double currentBestDistance = currentBest.getDistance();

            if (currentBestDistance < bestSoFar.getDistance()) {
                bestSoFar = new Tour(currentBest.cities, false); // En iyinin kopyasını al

                // İyileşme oldu, sayacı sıfırla
                noImprovementCount = 0;
            } else {
                // Ne kadar iyileşme oldu?
                double improvementRate = (lastBestDistance - currentBestDistance) / lastBestDistance;
                if (improvementRate < improvementThreshold) {
                    noImprovementCount++;
                } else {
                    noImprovementCount = 0;
                }
            }

            lastBestDistance = currentBestDistance;

            // Her 100 nesilde bir log yaz
            if (gen % 100 == 0 || gen == generations - 1) {
                System.out.printf("\nNesil %d/%d - En iyi maliyet: %.2f", gen, generations, bestSoFar.getDistance());
            }

            // Erken sonlandırma: Belirli bir süre iyileşme olmazsa
            if (noImprovementCount > maxNoImprovement) {
                System.out.printf("\nSonlandırma: %d nesil boyunca anlamlı iyileşme olmadı.", noImprovementCount);
                break;
            }
        }

        return bestSoFar;
    }

    // Nearest Neighbor yaklaşımı ile popülasyona başlangıç çözümleri ekle
    private static void initializePopulationWithHeuristicTours(Population pop) {
        if (pop.tours.isEmpty() || pop.tours.get(0).cities.isEmpty()) {
            return;
        }

        // Popülasyonun %20'si için daha iyi başlangıç çözümleri oluştur
        int heuristicCount = Math.min(20, pop.tours.size() / 5);
        List<City> cities = pop.tours.get(0).cities;
        int n = cities.size();

        for (int i = 0; i < heuristicCount; i++) {
            List<City> newTour = new ArrayList<>(n);
            boolean[] visited = new boolean[n];

            // Rastgele bir başlangıç şehri seç
            int startIndex = rand.nextInt(n);
            City current = cities.get(startIndex);
            newTour.add(current);
            visited[startIndex] = true;

            // Geri kalan şehirleri en yakın komşu yaklaşımıyla ekle
            for (int j = 1; j < n; j++) {
                double minDistance = Double.MAX_VALUE;
                int nextIndex = -1;

                // Mevcut şehre en yakın ziyaret edilmemiş şehri bul
                for (int k = 0; k < n; k++) {
                    if (!visited[k]) {
                        double dist = current.distanceTo(cities.get(k));
                        if (dist < minDistance) {
                            minDistance = dist;
                            nextIndex = k;
                        }
                    }
                }

                if (nextIndex != -1) {
                    current = cities.get(nextIndex);
                    newTour.add(current);
                    visited[nextIndex] = true;
                }
            }

            // Yeni turu popülasyona ekle
            pop.tours.set(i, new Tour(newTour, false));
        }
    }

    // 2-opt yerel optimizasyon
    private static void localOptimization(Tour tour) {
        int size = tour.cities.size();
        boolean improved = true;

        // Maksimum iyileştirme sayısı
        int maxImprovements = Math.min(50, size / 20);
        int improvements = 0;

        while (improved && improvements < maxImprovements) {
            improved = false;

            // Rastgele 100 kenar seç ve test et (büyük veri setleri için tam tarama çok
            // yavaş)
            for (int i = 0; i < 100; i++) {
                int a = rand.nextInt(size);
                int b = rand.nextInt(size);

                if (Math.abs(a - b) <= 1 || Math.abs(a - b) >= size - 1) {
                    continue; // Komşu kenarlar veya başlangıç-bitiş kenarını atla
                }

                // 2-opt değişimini test et
                if (tryTwoOptSwap(tour, a, b)) {
                    improved = true;
                    improvements++;
                    break;
                }
            }
        }
    }

    // 2-opt değişimi dene ve iyileşme varsa uygula
    private static boolean tryTwoOptSwap(Tour tour, int i, int j) {
        int size = tour.cities.size();

        // i ve j'yi doğru sıraya koy
        if (i > j) {
            int temp = i;
            i = j;
            j = temp;
        }

        City a = tour.cities.get(i);
        City b = tour.cities.get((i + 1) % size);
        City c = tour.cities.get(j);
        City d = tour.cities.get((j + 1) % size);

        // Mevcut iki kenarın uzunluğu
        double currentDistance = a.distanceTo(b) + c.distanceTo(d);

        // Yeni iki kenarın uzunluğu
        double newDistance = a.distanceTo(c) + b.distanceTo(d);

        // Eğer yeni mesafe daha kısaysa, güzergahı güncelle
        if (newDistance < currentDistance) {
            // i+1 ve j arasındaki alt listeyi tersine çevir
            Collections.reverse(tour.cities.subList(i + 1, j + 1));

            // Mesafe önbelleğini sıfırla
            tour.resetDistance();
            return true;
        }

        return false;
    }

    private static Tour select(Population pop) {
        // Turnuva seçimi (daha verimli implementasyon)
        List<Tour> tournament = new ArrayList<>(TOURNAMENT_SIZE);

        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            Tour randomTour = pop.tours.get(rand.nextInt(pop.tours.size()));
            tournament.add(randomTour);
        }

        // Stream API yerine manuel minimum bulma (daha hızlı)
        Tour best = tournament.get(0);
        for (int i = 1; i < tournament.size(); i++) {
            Tour current = tournament.get(i);
            if (current.getDistance() < best.getDistance()) {
                best = current;
            }
        }

        return best;
    }

    // Program sonlandığında ExecutorService'i kapat
    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
