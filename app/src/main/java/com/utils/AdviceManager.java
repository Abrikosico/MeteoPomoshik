package com.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.meteopomoshik.R;
import com.model.Article;
import com.model.WellbeingEntry;

import java.util.ArrayList;
import java.util.List;

public class AdviceManager {

    public static List<Article> getPersonalizedAdvice(Context context, List<WellbeingEntry> history, double currentPressure, int kpIndex) {
        List<Article> articles = new ArrayList<>();
        double pressureMmHg = Constants.hPaToMmHg(currentPressure);

        // 1. Читаем настройки чувствительности
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean sensitiveToPressure = prefs.getBoolean("sensitiveToPressure", true);
        boolean sensitiveToGeomag = prefs.getBoolean("sensitiveToGeomag", true);

        // --- БЛОК ПОГОДЫ ---

        // Анализ давления (только если пользователь чувствителен или давление экстремальное)
        if (pressureMmHg < 735) {
            articles.add(new Article(
                    "Экстремально низкое давление!",
                    "Атмосферное давление упало до " + (int)pressureMmHg + " мм рт.ст. Это серьезная нагрузка даже для здоровых людей. Избегайте физнагрузок.",
                    R.drawable.ic_risk_alert,
                    true
            ));
        } else if (sensitiveToPressure && pressureMmHg < 742) {
            articles.add(new Article(
                    "Давление понижено",
                    "Возможна вялость и сонливость. Рекомендуется контрастный душ и чашка крепкого чая.",
                    R.drawable.ic_pressure,
                    true
            ));
        } else if (sensitiveToPressure && pressureMmHg > 755) {
            articles.add(new Article(
                    "Давление повышено",
                    "Атмосферный фон повышен. Старайтесь не переедать соленого и пейте больше чистой воды.",
                    R.drawable.ic_pressure,
                    true
            ));
        }

        // Анализ магнитных бурь
        if (kpIndex >= 5) {
            boolean isSevere = kpIndex >= 7;
            articles.add(new Article(
                    isSevere ? "Мощная магнитная буря!" : "Магнитная буря (Kp=" + kpIndex + ")",
                    "Геомагнитное поле возмущено. " + (sensitiveToGeomag ? "Так как вы чувствительны к бурям, примите меры заранее: больше отдыха, меньше стресса." : "Возможны перепады настроения."),
                    R.drawable.ic_geomag,
                    true
            ));
        }

        // --- БЛОК АНАЛИЗА ДНЕВНИКА ---

        int headacheCount = 0;
        int fatigueCount = 0;
        int badDaysCount = 0;

        for (WellbeingEntry entry : history) {
            if (entry.headache) headacheCount++;
            if (entry.fatigue) fatigueCount++;
            if (entry.rating <= 2) badDaysCount++;
        }

        // Если часто болит голова (больше 2 раз за последние записи)
        if (headacheCount >= 2) {
            articles.add(new Article(
                    "Ваша статистика: Головные боли",
                    "Вы часто отмечаете головную боль в дневнике. В дни перепадов погоды (как сегодня) держите под рукой обезболивающее и соблюдайте режим сна.",
                    R.drawable.ic_diary_pen,
                    false
            ));
        }

        // Если часто усталость
        if (fatigueCount >= 2) {
            articles.add(new Article(
                    "Борьба с усталостью",
                    "Ваш дневник показывает частую усталость. Возможно, это сезонное. Попробуйте добавить витамин D и прогулки на свежем воздухе.",
                    R.drawable.ic_location,
                    false
            ));
        }

        // --- БЛОК ПОЛЕЗНЫХ СТАТЕЙ (Статический контент) ---

        // Добавляем статью "по теме"
        if (sensitiveToPressure) {
            articles.add(new Article(
                    "Как пережить скачки давления?",
                    "Главное правило «барометра» — сосуды любят тренировку. Контрастный душ утром и прогулка перед сном снижают метеочувствительность на 30%.",
                    R.drawable.ic_settings,
                    false
            ));
        }

        if (sensitiveToGeomag) {
            articles.add(new Article(
                    "Что такое Kp-индекс?",
                    "Это глобальный индекс геомагнитной активности. Значения от 0 до 3 — норма, 4 — возмущение, 5 и выше — буря. Вы можете настроить уведомления об этом в настройках.",
                    R.drawable.ic_geomag,
                    false
            ));
        }

        // Если всё хорошо
        if (articles.isEmpty()) {
            articles.add(new Article(
                    "Идеальный день",
                    "Погода стабильна, рисков нет. Отличное время для активных дел или спорта!",
                    R.drawable.ic_location,
                    false
            ));
        }

        return articles;
    }
}