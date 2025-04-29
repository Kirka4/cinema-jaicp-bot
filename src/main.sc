require: slotfilling/slotFilling.sc
  module = sys.zb-common
theme: /start {
    q!: * (привет/start/начать) *
    a: Привет! Я подберу фильм по жанру, году или названию. Например: "Комедии 2020" или "Интерстеллар".
}

theme: /search {
    q!: * [~фильм/найди/посоветуй] * (~жанр/год/название) *
    script: {
        // Формируем запрос к API Кинопоиска
        const apiKey =  $secrets.get("api_key");
        const query = nlp.getSlot("query");
        const url = `https://api.kinopoisk.dev/movie?search=${encodeURIComponent(query)}&field=name&isStrict=false`;
        
        // Отправляем HTTP-запрос
        const response = http.get(url, {
            headers: {
                "X-API-KEY": apiKey
            }
        });
        
        // Обработка ответа
        if (response.status === 200 && response.data.docs.length > 0) {
            $temp.films = response.data.docs.slice(0, 3); // Сохраняем топ-3 фильма
            a: Вот что нашлось:
               {{ $temp.films.map((f, i) => `${i+1}. ${f.name} (${f.year})`).join("\n") }}
               Напишите номер для подробностей.
        } else {
            a: Фильмы не найдены. Попробуйте другой запрос.
        }
    }
}

theme: /details {
    q!: * [1/2/3] *
    script: {
        const film = $temp.films[parseInt(nlp.getSlot("number")) - 1];
        a: {{ film.name }} ({{ film.year }})
           Рейтинг: {{ film.rating.kp || "нет данных" }}
           Описание: {{ film.description || "Описание отсутствует" }}
        image: {{ film.poster.url }}  
    }
}