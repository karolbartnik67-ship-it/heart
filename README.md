# Heart Indicator 2.0 (Fabric 1.21.4, klient)

Przerobiony mod spunkyinsaan (licencja MIT w `LICENSE`).

- **Usunięte** serca nad głowami graczy.
- Na ekranie pokazuje serca gracza, na którego **patrzysz** (np. `2/10`, max może być też np. 15; połówki: `7.5/10`).
- Gdy **uderzysz gracza**, jego serca zostają na ekranie przez **6 s** (kolejne trafienie odświeża czas, inny gracz przełącza cel). Przy aktywnym liczniku patrzenie na kogoś innego niczego nie zmienia.
- **Złote serca** są wliczone w liczbę (np. `12/10`, `17/15`) i wtedy napis jest **żółty**; bez złotych serc jest biały.
- **Boss bar** (osobny element): czerwony pasek = życie / max (5 z 10 serc = połowa). Gdy gracz ma **złote serca**, cały pasek robi się **żółty** i liczy życie + złote jak zwykłe serca (3 + 2 złote z 10 = 5/10; od 10 wzwyż pasek pełny).
- **GUI (klawisz J):** bez suwaków — łapiesz napis albo pasek myszką i przesuwasz; kwadracik ze strzałką ↘ ciągniesz w różne strony, żeby zmienić rozmiar (kółko myszy też działa). Na górze są dwa przyciski **z okiem** (Napis / Pasek) — przekreślone oko = element ukryty. Na dole przełącznik **serca / punkty zdrowia** (1 serce = 2 pkt). Esc = gotowe.
- Klawisz **H** = szybkie włączenie/wyłączenie.

Build: GitHub → Actions → **HeartIndicator-mod**, albo `gradle build`.
