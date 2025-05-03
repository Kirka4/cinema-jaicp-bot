require: slotfilling/slotFilling.sc
  module = sys.zb-common

theme: /

  state Start
   q!: *(привет|start|начать)*
   a: |
    Привет! Я подберу фильм по жанру, году или названию. Например: "Комедии 2020" или "Интерстеллар".
    script: |
      // Переход к поиску
      $state.to("Search");


  state: Search
    q!:
      * [фильм/фильмы/найди/порекомендуй/посоветуй/что посмотреть] *
    script: |
      try {
        var apiKey = $secrets.get("api_key");
        var query = $parseTree.text;
        var url = "https://api.kinopoisk.dev/v1.4/movie?search=" + encodeURIComponent(query) + "&limit=10&isStrict=false";

        var response = $http.get(url, {
        headers: { "X-API-KEY": apiKey }
      });
      
        if (response.isOk && response.data.docs && response.data.docs.length > 0) {
        // Сохраняем первые три фильма
        $temp.films = response.data.docs.slice(0, 3);
        var list = $temp.films.map(function(f, i) {
          return (i + 1) + ". " + f.name + " (" + f.year + ")";
        }).join("\n");
        
        $reactions.answer("Вот что нашлось:\n" + list);
        $reactions.ask("Напишите номер фильма для подробностей.");
        $state.to("Details");
      } else {
        $reactions.answer("Фильмы не найдены. Попробуйте другой запрос.");
        // Остаемся в состоянии Search
      }
     } catch (e) {
      $reactions.answer("Произошла ошибка при поиске. Попробуйте позже.");
      $state.to("Start");
    }

  state: Details
    q!: [1/2/3]
    script:  |
      if (!$temp.films || !$temp.films.length) {
        $reactions.answer("Пожалуйста, сначала введите запрос для поиска фильмов.");
        $state.to("Search");
        return;
     }

      var input = parseInt($parseTree.text);
     if (isNaN(input) || input < 1 || input > $temp.films.length) {
      $reactions.answer("Пожалуйста, выберите номер от 1 до " + $temp.films.length + ".");
      return;
     }

      var film = $temp.films[input - 1];
      var rating = (film.rating && film.rating.kp) ? film.rating.kp : "нет данных";
      var description = film.description || "Описание отсутствует";
      var message = film.name + " (" + film.year + ")\n" +
                  "Рейтинг: " + rating + "\n" +
                  "Описание: " + description;

        $reactions.answer(message);
        if (film.poster && film.poster.url) {
          $reactions.image({ url: film.poster.url });
     }

        delete $temp.films;
        $reactions.ask("Хотите найти другой фильм? Напишите жанр, год или название.");
        $state.to("Search");
