require: slotfilling/slotFilling.sc
  module = sys.zb-common

theme: /
  state: Start
    q!: * (привет/start/начать) *
    a: Привет! Я подберу фильм по жанру, году или названию. Например: "Комедии 2020" или "Интерстеллар".

  state: Search
    q!: * [~фильм/найди/посоветуй/порекомендуй] * (~жанр/год/название) *
    script:
      try {
        var apiKey = $secrets.get("api_key");
        var query = $parseTree.text; 
        var url = "https://api.kinopoisk.dev/v1.4/movie?search=" + encodeURIComponent(query) + "&limit=10&isStrict=false";

        var response = $http.get(url, {
            headers: {
                "X-API-KEY": apiKey
            }
        });

        if (response.isOk && response.data.docs && response.data.docs.length > 0) {
            $temp.films = response.data.docs.slice(0, 3);
            $reactions.transition("/Details");
            $reactions.answer("Вот что нашлось:\n" + $temp.films.map(function(f, i) {
                return (i + 1) + ". " + f.name + " (" + f.year + ")";
            }).join("\n") + "\nНапишите номер для подробностей.");
        } else {
            $reactions.answer("Фильмы не найдены. Попробуйте другой запрос.");
        }
      } catch (e) {
        $reactions.answer("Произошла ошибка при поиске. Попробуйте позже.");
      }

  state: Details
    q!: * [1/2/3] *
    script:
      if (!$temp.films) {
        // Если пользователь случайно написал 1/2/3 вне контекста — игнорируем
        $reactions.answer("Пожалуйста, сначала введите запрос для поиска фильмов.");
        return;
      }

      var input = parseInt($parseTree.text);
      if (isNaN(input) || input < 1 || input > $temp.films.length) {
          $reactions.answer("Пожалуйста, выберите номер от 1 до " + $temp.films.length + ".");
          return;
      }

      var index = input - 1;
      var film = $temp.films[index];

      if (film) {
          var rating = film.rating && film.rating.kp ? film.rating.kp : "нет данных";
          var description = film.description || "Описание отсутствует";
          var message = film.name + " (" + film.year + ")\n" +
                        "Рейтинг: " + rating + "\n" +
                        "Описание: " + description;
          $reactions.answer(message);

          if (film.poster && film.poster.url) {
              $reactions.image({ url: film.poster.url });
          }

          // Очистим $temp.films, чтобы бот не остался в режиме выбора
          delete $temp.films;

          $reactions.ask("Хотите найти другой фильм? Напишите жанр, год или название.");
      } else {
          $reactions.answer("Ошибка: фильм не найден. Попробуйте снова.");
      }

