# Postera: Dijital Dünyada Yeni Bir Soluk

Kıymetli ziyaretçi, hoş geldin. Evvel emirde belirtmek isterim ki, şu an incelemekte olduğun "Postera" isimli bu yazılım, alelade bir kod yığınından ibaret değildir; bilakis şahsi gayretlerimin, uykusuz gecelerimin ve teknolojiye olan hürmetimin bir tecellisidir. Gayemiz, karmaşıklaşan dijital iletişimi sade, süratli ve güvenilir bir zemine oturtmaktır.

## Bu Proje Nedir, Ne Değildir?

Postera, esas itibarıyla modern bir sosyal etkileşim ve mesajlaşma platformudur. Lakin onu sadece bir "mesajlaşma uygulaması" olarak tavsif etmek noksan kalır. Postera, kullanıcıların birbirleriyle pürüzsüz bir biçimde haberleşebildiği, dostlarını sağ menüden seçip ufak ve zarif pencerelerde sohbete dalabildiği, "yazıyor..." ve "görüldü" gibi canlı hissettiren özellikleri bünyesinde barındıran muhkem bir sistemdir.

Ne değildir? Hantal, kullanıcıyı reklamlara boğan, gereksiz karmaşayla zihni yoran ve mahremiyeti hiçe sayan ticari bir tuzak değildir. Safi iletişim ve sadelik odaklıdır.

## Hangi Meselelere Deva Olur?

Günümüz uygulamalarının en büyük sıkıntısı, ağır sayfalar ve bitmek bilmeyen yükleme süreleridir. Hülasa, Postera şu sorunları halletmek üzere vücuda getirilmiştir:
* **Hantal Arayüzler:** Karmaşık menüler yerine, göze hoş gelen, sade ve amaca doğrudan hizmet eden bir tasarım inşa ettim.
* **Kesintili İletişim:** Gerçek zamanlıya en yakın, lakin sunucuyu yormayan yenilikçi mesajlaşma mekanizmalarıyla kopuklukların önüne geçtim.
* **Kaynak İsrafı:** Dev kütüphaneler yerine, amaca yönelik kod bloklarıyla tarayıcı ve sunucu hafızasının israf edilmesini engelledim.

## Kime ve Neye Hizmet Eder?

Postera, dijital karmaşadan uzak durmak isteyen, dostlarıyla süratle ve güvenle hasbihal etmeyi arzulayan her bireye hizmet eder. Zira iletişim insanın fıtratında vardır; bu yazılım da o fıtri ihtiyacı dijital ortamda en zarif ve pratik şekilde karşılamayı maksat edinmiştir.

## Nasıl İstifade Edilir? (Kullanım Şekli)

Kullanımı gayet basittir. Arayüze dahil olduğunuzda sizi sol tarafta nizamlı bir menü, sağ tarafta ise dostlarınızın yer aldığı bir liste karşılar. Sağ menüden dilediğiniz dostunuzu seçtiğiniz an, sağ alt köşede zarif bir sohbet penceresi açılır. Aynı anda 3 farklı kişiyle bu pencereler vasıtasıyla sohbet edebilirsiniz; fazlasını açmak isterseniz sistem sizi kibarca uyaracaktır. Gelen mesajları okuduğunuzda karşı taraf bunu anında "görüldü" olarak fark eder; klavyenin tuşlarına dokunduğunuz an ise "yazıyor..." ibaresi belirir. Bütün bu işleyiş, ana sayfadan ayrılmanıza dahi lüzum kalmadan tıkır tıkır işler.

## Mimari, Tasarım Şablonları ve Algoritmalar

Yazılımın temelini atarken, uzun ömürlü ve genişlemeye müsait bir yapı kurmaya bilhassa ehemmiyet verdim. Binaenaleyh, altyapıda şu kararları tatbik ettim:

### Mimarimiz
Projede **N-Tier (Çok Katmanlı) Mimari** tercih ettim. İstekler Controller (Denetleyici) katmanında karşılanır, iş kuralları Service (Hizmet) katmanında icra edilir ve veritabanı işlemleri Repository (Depo) katmanına devredilir. Bu ayrım, kodun okunabilirliğini ve ileride yapılacak müdahalelerin kolaylığını temin etti.

### Tasarım Şablonları (Design Patterns)
* **MVC (Model-View-Controller):** Kullanıcı arayüzü ile arka plan işleyişini birbirinden kati surette ayırmak için kullanıldı.
* **DTO (Data Transfer Object):** Veritabanı varlıklarının (Entity) doğrudan dışarıya açılmasını engellemek, yalnızca lüzum hasıl olan veriyi taşımak maksadıyla `Record` yapıları halinde tatbik edildi.
* **Singleton:** Spring framework'ünün fıtratı gereği, servis ve depo sınıfları bellekte tek bir numune (instance) olarak tutularak kaynak israfının önüne geçildi.

### Algoritmik Yaklaşımlar
Bizzat kodlarken en çok zihin yorduğum hususlardan biri, "Yazıyor" ve "Görüldü" bilgilerinin karşı tarafa nasıl iletileceği idi. Ağır mesaj kuyrukları (RabbitMQ vb.) kurmak yerine, hızı artırmak adına **In-Memory (Bellek İçi) ConcurrentHashMap** yapısı ve **Debounce/Polling algoritmaları** kullandım. İstemci tarafında tuş vuruşları 3 saniyelik bir zamanlayıcı (debounce) ile süzülür. Arka planda ise 4 saniyelik TTL (Yaşam Süresi) mantığıyla çalışan bellekteki harita, kimin kime yazdığını anlık ve maliyetsiz bir şekilde tutar. Okundu bilgisi için de veritabanını yormamak adına özel `@Modifying` sorguları yazarak işlemleri süratlendirdim.

## Kullanılan Teknolojiler ve Tercih Sebepleri

| Teknoloji | Hangi Maksatla Kullanıldı? | Neden Tercih Ettim? |
| :--- | :--- | :--- |
| **Java & Spring Boot** | Arka plan (Backend) hizmetleri ve API'ler. | Muhkem, güvenli ve kurumsal dünyada rüştünü ispat etmiş bir altyapı sunduğu için. |
| **Spring Data JPA & Hibernate** | Veritabanı işlemleri (ORM). | SQL karmaşasına girmeden, nesne tabanlı bir yaklaşımla veriyi idare edebilmek maksadıyla. |
| **Thymeleaf** | Sayfa şablon motoru (Server-Side Rendering). | Ağır SPA kütüphanelerinden ziyade, sayfaların sunucuda işlenip istemciye süratle ulaştırılması için. |
| **Vanilla JavaScript & CSS** | Ön yüz dinamizmi ve tasarım. | Dış bağımlılıklara lüzum kalmadan, tarayıcıyı yormayan, sade ve şık bir görünüm elde etmek gayesiyle. |
| **PostgreSQL & H2** | Veri depolama ve test. | İlişkisel verilerin (kullanıcılar, mesajlar) noksansız ve güvenli muhafazası için. |
| **Spring Security** | Yetkilendirme ve güvenlik. | Kullanıcı mahremiyetini ve sistemin selametini sağlam temellere oturtmak için. |

Hülasa-i kelam; Postera, modern çağın hızına ayak uydururken, geçmişin zarafetini ve yazılım mühendisliğinin temel kaidelerini bünyesinde cem eden bir eserdir. Bizzat ilmek ilmek işlediğim bu projeyi incelediğiniz ve vakit ayırdığınız için teşekkür ederim.

### Tanıtım Görselleri;

![Ekran Resmi 2026-09-08 13.44.18.png](screenshots/Ekran%20Resmi%202026-09-08%2013.44.18.png)
![Ekran Resmi 2026-09-08 13.44.26.png](screenshots/Ekran%20Resmi%202026-09-08%2013.44.26.png)
![Ekran Resmi 2026-09-08 13.44.33.png](screenshots/Ekran%20Resmi%202026-09-08%2013.44.33.png)
![Ekran Resmi 2026-09-08 13.44.43.png](screenshots/Ekran%20Resmi%202026-09-08%2013.44.43.png)
![Ekran Resmi 2026-09-08 13.45.35.png](screenshots/Ekran%20Resmi%202026-09-08%2013.45.35.png)
![Ekran Resmi 2026-09-08 13.45.42.png](screenshots/Ekran%20Resmi%202026-09-08%2013.45.42.png)
![Ekran Resmi 2026-09-08 13.45.46.png](screenshots/Ekran%20Resmi%202026-09-08%2013.45.46.png)
![Ekran Resmi 2026-09-08 13.46.00.png](screenshots/Ekran%20Resmi%202026-09-08%2013.46.00.png)
![Ekran Resmi 2026-09-08 13.46.19.png](screenshots/Ekran%20Resmi%202026-09-08%2013.46.19.png)
![Ekran Resmi 2026-09-08 13.46.40.png](screenshots/Ekran%20Resmi%202026-09-08%2013.46.40.png)
![Ekran Resmi 2026-09-08 13.47.02.png](screenshots/Ekran%20Resmi%202026-09-08%2013.47.02.png)
![Ekran Resmi 2026-09-08 13.47.09.png](screenshots/Ekran%20Resmi%202026-09-08%2013.47.09.png)
![Ekran Resmi 2026-09-08 13.48.59.png](screenshots/Ekran%20Resmi%202026-09-08%2013.48.59.png)
![Ekran Resmi 2026-09-08 13.49.05.png](screenshots/Ekran%20Resmi%202026-09-08%2013.49.05.png)
![Ekran Resmi 2026-09-08 13.49.25.png](screenshots/Ekran%20Resmi%202026-09-08%2013.49.25.png)
![Ekran Resmi 2026-09-08 13.49.44.png](screenshots/Ekran%20Resmi%202026-09-08%2013.49.44.png)
![Ekran Resmi 2026-09-08 13.50.00.png](screenshots/Ekran%20Resmi%202026-09-08%2013.50.00.png)
![Ekran Resmi 2026-09-08 13.50.35.png](screenshots/Ekran%20Resmi%202026-09-08%2013.50.35.png)
![Ekran Resmi 2026-09-08 13.50.41.png](screenshots/Ekran%20Resmi%202026-09-08%2013.50.41.png)
![Ekran Resmi 2026-09-08 13.50.59.png](screenshots/Ekran%20Resmi%202026-09-08%2013.50.59.png)
![Ekran Resmi 2026-09-08 13.51.09.png](screenshots/Ekran%20Resmi%202026-09-08%2013.51.09.png)
![Ekran Resmi 2026-09-08 13.52.04.png](screenshots/Ekran%20Resmi%202026-09-08%2013.52.04.png)
![Ekran Resmi 2026-09-08 13.52.17.png](screenshots/Ekran%20Resmi%202026-09-08%2013.52.17.png)
![Ekran Resmi 2026-09-08 13.52.39.png](screenshots/Ekran%20Resmi%202026-09-08%2013.52.39.png)
![Ekran Resmi 2026-09-08 13.52.54.png](screenshots/Ekran%20Resmi%202026-09-08%2013.52.54.png)
![Ekran Resmi 2026-09-08 13.53.21.png](screenshots/Ekran%20Resmi%202026-09-08%2013.53.21.png)
![Ekran Resmi 2026-09-08 13.53.46.png](screenshots/Ekran%20Resmi%202026-09-08%2013.53.46.png)
![Ekran Resmi 2026-09-08 13.54.31.png](screenshots/Ekran%20Resmi%202026-09-08%2013.54.31.png)
![Ekran Resmi 2026-09-08 13.55.37.png](screenshots/Ekran%20Resmi%202026-09-08%2013.55.37.png)
![Ekran Resmi 2026-09-08 13.56.41.png](screenshots/Ekran%20Resmi%202026-09-08%2013.56.41.png)
![Ekran Resmi 2026-09-08 13.56.51.png](screenshots/Ekran%20Resmi%202026-09-08%2013.56.51.png)
![Ekran Resmi 2026-09-08 13.57.00.png](screenshots/Ekran%20Resmi%202026-09-08%2013.57.00.png)
![Ekran Resmi 2026-09-08 13.57.14.png](screenshots/Ekran%20Resmi%202026-09-08%2013.57.14.png)
![Ekran Resmi 2026-09-08 13.57.45.png](screenshots/Ekran%20Resmi%202026-09-08%2013.57.45.png)
![Ekran Resmi 2026-09-08 13.58.43.png](screenshots/Ekran%20Resmi%202026-09-08%2013.58.43.png)
![Ekran Resmi 2026-09-08 13.58.49.png](screenshots/Ekran%20Resmi%202026-09-08%2013.58.49.png)
![Ekran Resmi 2026-09-08 13.58.56.png](screenshots/Ekran%20Resmi%202026-09-08%2013.58.56.png)
![Ekran Resmi 2026-09-08 13.59.13.png](screenshots/Ekran%20Resmi%202026-09-08%2013.59.13.png)
![Ekran Resmi 2026-09-08 13.59.17.png](screenshots/Ekran%20Resmi%202026-09-08%2013.59.17.png)
![Ekran Resmi 2026-09-08 13.59.22.png](screenshots/Ekran%20Resmi%202026-09-08%2013.59.22.png)
![Ekran Resmi 2026-09-08 13.59.34.png](screenshots/Ekran%20Resmi%202026-09-08%2013.59.34.png)
![Ekran Resmi 2026-09-08 13.59.40.png](screenshots/Ekran%20Resmi%202026-09-08%2013.59.40.png)
![Ekran Resmi 2026-09-08 13.59.50.png](screenshots/Ekran%20Resmi%202026-09-08%2013.59.50.png)
![Ekran Resmi 2026-09-08 14.00.03.png](screenshots/Ekran%20Resmi%202026-09-08%2014.00.03.png)
![Ekran Resmi 2026-09-08 14.00.11.png](screenshots/Ekran%20Resmi%202026-09-08%2014.00.11.png)
![Ekran Resmi 2026-09-08 14.00.56.png](screenshots/Ekran%20Resmi%202026-09-08%2014.00.56.png)
![Ekran Resmi 2026-09-08 14.01.02.png](screenshots/Ekran%20Resmi%202026-09-08%2014.01.02.png)
![Ekran Resmi 2026-09-08 14.01.14.png](screenshots/Ekran%20Resmi%202026-09-08%2014.01.14.png)
![Ekran Resmi 2026-09-08 14.01.33.png](screenshots/Ekran%20Resmi%202026-09-08%2014.01.33.png)
![Ekran Resmi 2026-09-08 14.01.38.png](screenshots/Ekran%20Resmi%202026-09-08%2014.01.38.png)
![Ekran Resmi 2026-09-08 14.02.15.png](screenshots/Ekran%20Resmi%202026-09-08%2014.02.15.png)
![Ekran Resmi 2026-09-08 14.02.29.png](screenshots/Ekran%20Resmi%202026-09-08%2014.02.29.png)