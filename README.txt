AnkoriaDelivery (Spigot Plugin)
===========================

Amaç:
- Web siteden alınan ürünleri MySQL 'deliveries' tablosuna düşürürsün.
- Plugin her 1 saniyede bir pending kayıtları okur ve konsoldan komutları çalıştırır.

Kurulum:
1) SQL_CREATE_DELIVERIES.sql dosyasını MySQL'de çalıştır.
2) Bu plugin'i build et:
   - Java 17
   - Gradle
   Komut: gradlew build
   Jar: build/libs/AnkoriaDelivery-1.0.0.jar
3) jar'ı plugins/ içine at.
4) plugins/AnkoriaDelivery/config.yml içinde MySQL bilgilerini gir.
5) Sunucuyu başlat.

Komutlar:
- /deliveryreload

Komut placeholder:
- {player} => oyuncu adı

Not:
- LuckPerms örnek komut: lp user {player} parent add vip
