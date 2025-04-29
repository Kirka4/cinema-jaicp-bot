require: slotfilling/slotFilling.sc
  module = sys.zb-common
theme: /start {
    q!: * (привет/start/начать) *
    a: Привет! Я помогу подобрать фильм. Назови жанр, год или название. 
       Например: "Комедии 2020" или "Фантастика".
}

theme: /search {
    q!: * [~фильм/посоветуй/найди] * (~жанр/год/название) *
    script: {
        const query = nlp.getSlot("query"); // Извлечение параметров
        const response = http.get(`https://api.kinopoisk.dev?query=${query}`);
        
        if (response.status === 200 && response.data.results.length > 0) {
            $temp.films = response.data.results.slice(0, 3);
            a: Вот подборка:
            {{ $temp.films.map((f, index) => `${index + 1}. ${f.title}`).join("\n") }}
            Напишите номер фильма для подробностей.
        } else {
            a: Извините, ничего не найдено. Попробуйте другой запрос.
        }
    }
}

theme: /details {
    q!: * [1/2/3] *
    script: {
        const filmIndex = parseInt(nlp.getSlot("number")) - 1;
        const film = $temp.films[filmIndex];
        a: {{ film.title }} 
           Рейтинг: {{ film.rating }} 
           Описание: {{ film.description }}
        image: {{ film.poster }} // Постер фильма
    }
}