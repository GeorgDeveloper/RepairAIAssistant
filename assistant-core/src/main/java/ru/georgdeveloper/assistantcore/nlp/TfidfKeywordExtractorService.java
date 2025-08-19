
package ru.georgdeveloper.assistantcore.nlp;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Сервис для извлечения ключевых слов и фраз из текста на основе TF-IDF.
 * Для MVP: простая реализация без внешних зависимостей.
 */
@Service
public class TfidfKeywordExtractorService {
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
        // Русские стоп-слова (можно расширить)
        "и", "в", "во", "не", "что", "он", "на", "я", "с", "со", "как", "а", "то", "все", "она", "так", "его", "но", "да", "ты", "к", "у", "же", "вы", "за", "бы", "по", "только", "ее", "мне", "было", "вот", "от", "меня", "еще", "нет", "о", "из", "ему", "теперь", "когда", "даже", "ну", "вдруг", "ли", "если", "уже", "или", "ни", "быть", "был", "него", "до", "вас", "нибудь", "опять", "уж", "вам", "ведь", "там", "потом", "себя", "ничего", "ей", "может", "они", "тут", "где", "есть", "надо", "ней", "для", "мы", "тебя", "их", "чем", "была", "сам", "чтоб", "без", "будто", "чего", "раз", "тоже", "себе", "под", "будет", "ж", "тогда", "кто", "этот", "того", "потому", "этого", "какой", "совсем", "ним", "здесь", "этом", "один", "почти", "мой", "тем", "чтобы", "нее", "сейчас", "были", "куда", "зачем", "всех", "никогда", "можно", "при", "наконец", "два", "об", "другой", "хоть", "после", "над", "больше", "тот", "через", "эти", "нас", "про", "всего", "них", "какая", "много", "разве", "три", "эту", "моя", "впрочем", "хорошо", "свою", "этой", "перед", "иногда", "лучше", "чуть", "том", "нельзя", "такой", "им", "более", "всегда", "конечно", "всю", "между"
    ));
    private static final Pattern WORD_PATTERN = Pattern.compile("[а-яА-Яa-zA-Z0-9-]+", Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * Извлекает топ-N ключевых слов из текста на основе частоты (TF).
     * Для MVP: без IDF, только TF и фильтрация стоп-слов.
     */
    public List<String> extractKeywords(String text, int topN) {
        if (text == null || text.isBlank()) return Collections.emptyList();
        Map<String, Integer> freq = new HashMap<>();
        Matcher matcher = WORD_PATTERN.matcher(text.toLowerCase());
        while (matcher.find()) {
            String word = matcher.group();
            if (word.length() < 3 || STOPWORDS.contains(word)) continue;
            freq.put(word, freq.getOrDefault(word, 0) + 1);
        }
        return freq.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
