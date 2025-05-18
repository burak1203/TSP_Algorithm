# 🧭 TSP (Traveling Salesman Problem) – Genetik Algoritma Çözümü

Bu proje, **Gezgin Satıcı Problemi (TSP)** için **Genetik Algoritma** tabanlı bir çözüm sunar. Amaç, tüm şehirleri bir kez ziyaret edip başlangıç noktasına dönerek minimum toplam mesafeyi bulmaktır.

Proje, **Java** ile geliştirilmiştir ve terminal üzerinden çalıştırılabilir.

---

## ⚙️ Uygulama Kurulumu ve Çalıştırma

### Gereksinimler
- Java JDK (11 veya üzeri)
- Visual Studio Code (veya başka bir Java editörü)
- Komut satırı (Terminal / CMD / PowerShell)

### Kurulum ve Çalıştırma Adımları

1. 📂 **Projeyi VSCode ile açın.**

2. 📁 **TSP problem dosyasını `data/` klasörüne yerleştirin.**
   - Örnek dosya ismi: `tsp_20_1`
   - Tam yol: `data/tsp_20_1`

3. 🛠️ **Projeyi derleyin.**
   - (Varsa build script veya manuel `javac` komutuyla `bin/` klasörüne derleyin.)

4. ▶️ **Genetik algoritmayı çalıştırın:**

   ```bash
   java -cp bin Main tsp_20_1
