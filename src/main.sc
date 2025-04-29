require: slotfilling/slotFilling.sc
  module = sys.zb-common

theme: /
  state: Start
    q!: * (привет/start/начать) *
    a: Привет! Я подберу фильм по жанру, году или названию. Например: "Комедии 2020" или "Интерстеллар".

  state: Search
    q!: * [~фильм/найди/посоветуй] * (~жанр/год/название) *
    script:
      var apiKey = $secrets.get("api_key");
      var query = $parseTree.text; 
      var url = "https://api.kinopoisk.dev/v1.4/movie?search=${encodeURIComponent(query)}&limit=10&isStrict=false";
      
      var response = $http.get(url, {
          headers: {
              "X-API-KEY": apiKey
          }
      });
      
      // Обработка ответа
      if (response.isOk && response.data.docs && response.data.docs.length > 0) {
          $temp.films = response.data.docs.slice(0, 3);
          $reactions.answer("Вот что нашлось:\n" + $temp.films.map(function(f, i) {
              return "${i+1}. ${f.name} (${f.year})";
          }).join("\n") + "\nНапишите номер для подробностей.");
      } else {
          $reactions.answer("Фильмы не найдены. Попробуйте другой запрос.");
      }

  state: Details
    q!: * [1/2/3] *
    script:
      var index = parseInt($parseTree.text) - 1;
      var film = $temp.films[index];
      if (film) {
          $reactions.answer("${film.name} (${film.year})\n" +
                            `Рейтинг: ${film.rating?.kp || "нет данных"}\n` +
                            `Описание: ${film.description || "Описание отсутствует"}`);
          if (film.poster?.url) {
              $reactions.image({ url: film.poster.url });
          }
      } else {
          $reactions.answer("Ошибка: фильм не найден. Попробуйте снова.");
      }