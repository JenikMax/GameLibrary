package com.jenikmax.game.library.service.ai;

import java.util.*;

public final class KeywordTagMapper {

    public record Rule(Set<String> tags, Set<String> genres) {}

    private static final Map<String, Rule> KEYWORDS = new LinkedHashMap<>();

    static {
        k("открытый мир",        T("open_world"),                G());
        k("open world",          T("open_world"),                G());
        k("open-world",          T("open_world"),                G());
        k("кооператив",          T("coop"),                      G());
        k("co-op",               T("coop"),                      G());
        k("coop",                T("coop"),                      G());
        k("мультиплеер",         T("multiplayer"),               G());
        k("multiplayer",         T("multiplayer"),               G());
        k("одиночная",           T("singleplayer"),              G());
        k("singleplayer",        T("singleplayer"),              G());
        k("single player",       T("singleplayer"),              G());
        k("от первого лица",     T("fps"),                       G("first_person", "shooter"));
        k("first person",        T("fps"),                       G("first_person", "shooter"));
        k("от третьего лица",    T("tps"),                       G("third_person", "shooter"));
        k("third person",        T("tps"),                       G("third_person", "shooter"));
        k("инди",                T("indie"),                     G("indie"));
        k("indie",               T("indie"),                     G("indie"));
        k("пошаговая",           T("turn_based"),                G("turn_based"));
        k("turn-based",          T("turn_based"),                G("turn_based"));
        k("пошаговые сражения",  T("turn_based"),                G("turn_based"));
        k("пиксельная графика",  T("pixel_art", "retro"),        G("retro"));
        k("pixel art",           T("pixel_art", "retro"),        G("retro"));
        k("моддинг",             T("modding"),                   G());
        k("modding",             T("modding"),                   G());
        k("моды",                T("modding"),                   G());
        k("mods",                T("modding"),                   G());
        k("крафт",               T("crafting"),                  G());
        k("crafting",            T("crafting"),                  G());
        k("зомби",               T("zombies"),                   G("zombie"));
        k("zombie",              T("zombies"),                   G("zombie"));
        k("киберпанк",           T("cyberpunk"),                 G("cyberpunk"));
        k("cyberpunk",           T("cyberpunk"),                 G("cyberpunk"));
        k("фэнтези",             T("fantasy"),                   G("fantasy"));
        k("fantasy",             T("fantasy"),                   G("fantasy"));
        k("постапокалипсис",    T("post_apocalyptic"),          G("post_apocalyptic"));
        k("post-apocalyptic",    T("post_apocalyptic"),          G("post_apocalyptic"));
        k("стимпанк",            T("steampunk"),                 G("steampunk"));
        k("steampunk",          T("steampunk"),                 G("steampunk"));
        k("аниме",               T("anime"),                     G("anime"));
        k("anime",               T("anime"),                     G("anime"));
        k("ремейк",              T("remake"),                    G());
        k("remake",              T("remake"),                    G());
        k("процедурная генерация", T("procedural"),             G());
        k("procedural generation", T("procedural"),             G());
        k("roguelike",           T("roguelike"),                 G("roguelike"));
        k("рогалик",             T("roguelike"),                 G("roguelike"));
        k("rogue-lite",          T("roguelike"),                 G("roguelike"));
        k("soulslike",           T("soulslike"),                 G("soulslike"));
        k("соулслайк",           T("soulslike"),                 G("soulslike"));
        k("metroidvania",        T("metroidvania"),              G("metroidvaniia"));
        k("метроидвания",        T("metroidvania"),              G("metroidvaniia"));
        k("песочница",           T("sandbox"),                   G("sandbox"));
        k("sandbox",             T("sandbox"),                   G("sandbox"));
        k("визуальная новелла",  T("visual_novel"),              G("vizualnaia_novella"));
        k("visual novel",        T("visual_novel"),              G("vizualnaia_novella"));
        k("симулятор",           T(),                            G("simulators"));
        k("simulator",           T(),                            G("simulators"));
        k("survival",            T("survival"),                  G("survival"));
        k("выживание",           T("survival"),                  G("survival"));
        k("stealth",             T("stealth"),                   G("stealth"));
        k("стелс",               T("stealth"),                   G("stealth"));
        k("horror",              T("horror"),                    G("horror"));
        k("хоррор",              T("horror"),                    G("horror"));
        k("ужасы",               T("horror"),                    G("horror"));
        k("racing",              T("racing"),                    G("racing"));
        k("гонки",               T("racing"),                    G("racing"));
        k("сетевая игра",        T("online"),                    G("online"));
        k("online",              T("online"),                    G("online"));
        k("локальный кооператив", T("local_coop"),              G());
        k("local co-op",         T("local_coop"),               G());
        k("split screen",        T("local_coop"),               G());
        k("разделённый экран",   T("local_coop"),               G());
        k("pvp",                 T("pvp"),                       G());
        k("pve",                 T("pve"),                       G());
        k("контроллер",          T("controller_support"),        G());
        k("controller support",  T("controller_support"),        G());
        k("достижения",          T("achievements"),              G());
        k("achievements",        T("achievements"),              G());
        k("карточная игра",      T("card_game"),                 G("card"));
        k("card game",           T("card_game"),                 G("card"));
        k("платформер",          T(),                            G("platform"));
        k("platformer",          T(),                            G("platform"));
        k("аркада",              T(),                            G("arcade"));
        k("arcade",              T(),                            G("arcade"));
        k("квест",               T(),                            G("quest"));
        k("quest",               T(),                            G("quest"));
        k("стратегия",           T(),                            G("strategy"));
        k("strategy",            T(),                            G("strategy"));
        k("тактика",             T(),                            G("tactics"));
        k("tactics",             T(),                            G("tactics"));
        k("файтинг",             T(),                            G("fighting"));
        k("fighting",            T(),                            G("fighting"));
        k("головоломка",         T(),                            G("pazzl"));
        k("puzzle",              T(),                            G("pazzl"));
        k("музыкальная игра",    T(),                            G("music"));
        k("rhythm game",         T(),                            G("music"));
        k("спорт",               T(),                            G("sport"));
        k("sports",              T(),                            G("sport"));
        k("глубокий сюжет",      T("story_rich"),               G());
        k("story rich",          T("story_rich"),               G());
        k("атмосферная",         T("atmospheric"),              G());
        k("atmospheric",         T("atmospheric"),              G());
        k("исследование",        T("exploration"),              G("adventure"));
        k("exploration",         T("exploration"),              G("adventure"));
        k("научная фантастика",  T("sci_fi"),                   G("sci_fi"));
        k("sci-fi",              T("sci_fi"),                   G("sci_fi"));
        k("sci fi",              T("sci_fi"),                   G("sci_fi"));
        k("космос",              T("space"),                    G("space"));
        k("space",               T("space"),                    G("space"));
        k("war",                 T("war"),                      G("humour"));
        k("война",               T("war"),                      G("humour"));
        k("мрачная",             T("dark"),                     G("horror"));
        k("dark fantasy",        T("dark", "fantasy"),          G("fantasy", "horror"));
        k("скандинавская мифология", T("fantasy"),             G("fantasy", "istoriia"));
        k("norse mythology",     T("fantasy"),                  G("fantasy", "istoriia"));
        k("греческая мифология", T("fantasy"),                  G("fantasy", "istoriia"));
        k("средневековье",       T("medieval"),                 G("medieval"));
        k("medieval",            T("medieval"),                 G("medieval"));
        k("подземелья",          T("dungeons"),                 G("dungeons"));
        k("dungeon crawler",     T("dungeons"),                 G("dungeons"));
        k("башенная защита",     T("tower_defence"),            G("tower_defence"));
        k("tower defense",       T("tower_defence"),            G("tower_defence"));
        k("tower defence",       T("tower_defence"),            G("tower_defence"));
        k("tps",                 T(),                            G("shooter"));
        k("fps",                 T(),                            G("first_person", "shooter"));
        k("шутер",               T(),                            G("shooter"));
        k("shooter",             T(),                            G("shooter"));
        k("hack and slash",      T("hack_and_slash"),           G("hack_and_slash"));
        k("hack-and-slash",      T("hack_and_slash"),           G("hack_and_slash"));
        k("слэшер",              T("hack_and_slash"),           G("slasher"));
        k("beat em up",          T("beat_em_up"),               G("beat_em_up"));
        k("beat-em-up",          T("beat_em_up"),               G("beat_em_up"));
        k("битемап",             T("beat_em_up"),               G("beat_em_up"));
        k("для взрослых",        T("adult"),                    G("adult"));
        k("adult",               T("adult"),                    G("adult"));
        k("эротика",             T("adult"),                    G("adult"));
        k("nudity",              T("adult"),                    G("adult"));
        k("vr",                  T("vr"),                       G("vr"));
        k("виртуальная реальность", T("vr"),                    G("vr"));
        k("virtual reality",     T("vr"),                       G("vr"));
        k("демоны",              T(),                            G("fantasy", "horror"));
        k("мифология",           T(),                            G("istoriia"));
        k("4x",                  T(),                            G("_4X"));
        k("глобальная стратегия", T(),                           G("_4X"));
        k("интерактивное кино",  T(),                            G("interaktivnoe_kino"));
        k("менеджер",            T(),                            G("manager"));
        k("экономика",           T(),                            G("economy"));
        k("строительство",       T(),                            G("construction"));
        k("меха",                T(),                            G("meha"));
        k("mecha",               T(),                            G("meha"));
        k("авиасимулятор",       T(),                            G("flight"));
        k("flight simulator",    T(),                            G("flight"));
        k("спортивный симулятор", T(),                           G("sport", "simulators"));
    }

    private static void k(String keyword, Set<String> tags, Set<String> genres) {
        KEYWORDS.put(keyword.toLowerCase(), new Rule(tags, genres));
    }

    private static Set<String> T(String... values) {
        return values.length == 0 ? Set.of() : Set.of(values);
    }

    private static Set<String> G(String... values) {
        return values.length == 0 ? Set.of() : Set.of(values);
    }

    public static Map<String, Rule> getKeywords() {
        return Collections.unmodifiableMap(KEYWORDS);
    }

    public static Set<String> matchTags(String lowerText) {
        Set<String> tags = new LinkedHashSet<>();
        for (var entry : KEYWORDS.entrySet()) {
            if (lowerText.contains(entry.getKey())) {
                tags.addAll(entry.getValue().tags());
            }
        }
        return tags;
    }

    public static Set<String> matchGenres(String lowerText) {
        Set<String> genres = new LinkedHashSet<>();
        for (var entry : KEYWORDS.entrySet()) {
            if (lowerText.contains(entry.getKey())) {
                genres.addAll(entry.getValue().genres());
            }
        }
        return genres;
    }
}
