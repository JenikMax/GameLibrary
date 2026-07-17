package com.jenikmax.game.library.service;

import com.jenikmax.game.library.dao.api.GameCollectionEntryRepository;
import com.jenikmax.game.library.dao.api.GameCollectionRepository;
import com.jenikmax.game.library.dao.api.UserRepository;
import com.jenikmax.game.library.model.entity.GameCollection;
import com.jenikmax.game.library.model.entity.GameCollectionEntry;
import com.jenikmax.game.library.model.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CollectionService {

    private final GameCollectionRepository collectionRepository;
    private final GameCollectionEntryRepository entryRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CollectionService(GameCollectionRepository collectionRepository,
                              GameCollectionEntryRepository entryRepository,
                              UserRepository userRepository,
                              JdbcTemplate jdbcTemplate) {
        this.collectionRepository = collectionRepository;
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<GameCollection> getUserCollections(Long userId) {
        return collectionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public List<GameCollection> getPublicCollections() {
        return collectionRepository.findByIsPublicTrueOrderByUpdatedAtDesc();
    }

    public Optional<GameCollection> getById(Long id) {
        return collectionRepository.findById(id);
    }

    @Transactional
    public GameCollection create(String name, String description, boolean isPublic, Long userId) {
        GameCollection c = new GameCollection();
        c.setName(name);
        c.setDescription(description);
        c.setIsPublic(isPublic);
        c.setUser(userRepository.getReferenceById(userId));
        c.setCreatedAt(Timestamp.from(Instant.now()));
        c.setUpdatedAt(Timestamp.from(Instant.now()));
        return collectionRepository.save(c);
    }

    @Transactional
    public GameCollection update(Long id, String name, String description, boolean isPublic) {
        GameCollection c = collectionRepository.findById(id).orElseThrow();
        c.setName(name);
        c.setDescription(description);
        c.setIsPublic(isPublic);
        c.setUpdatedAt(Timestamp.from(Instant.now()));
        return collectionRepository.save(c);
    }

    @Transactional
    public void delete(Long id) {
        collectionRepository.deleteById(id);
    }

    public List<GameCollectionEntry> getEntries(Long collectionId) {
        return entryRepository.findByCollectionIdOrderBySortOrderAsc(collectionId);
    }

    @Transactional
    public GameCollectionEntry addGame(Long collectionId, Long gameId) {
        Optional<GameCollectionEntry> existing = entryRepository.findByCollectionIdAndGameId(collectionId, gameId);
        if (existing.isPresent()) return existing.get();

        long count = entryRepository.countByCollectionId(collectionId);
        GameCollectionEntry e = new GameCollectionEntry();
        e.setCollection(collectionRepository.getReferenceById(collectionId));
        e.setGameId(gameId);
        e.setSortOrder((int) count);
        e.setAddedAt(Timestamp.from(Instant.now()));
        return entryRepository.save(e);
    }

    @Transactional
    public void removeGame(Long collectionId, Long gameId) {
        entryRepository.deleteByCollectionIdAndGameId(collectionId, gameId);
    }

    @Transactional
    public void reorder(Long collectionId, List<Long> gameIds) {
        for (int i = 0; i < gameIds.size(); i++) {
            final int order = i;
            entryRepository.findByCollectionIdAndGameId(collectionId, gameIds.get(i))
                    .ifPresent(e -> {
                        e.setSortOrder(order);
                        entryRepository.save(e);
                    });
        }
    }

    public long getEntryCount(Long collectionId) {
        return entryRepository.countByCollectionId(collectionId);
    }

    public long countSmartGames(String rulesJson) {
        if (rulesJson == null || rulesJson.isBlank()) return 0;
        try {
            JsonNode rules = objectMapper.readTree(rulesJson);
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM library.game_data g");
            List<Object> params = new ArrayList<>();
            buildSmartRulesConditions(rules, sql, params);
            return Long.parseLong(jdbcTemplate.queryForObject(sql.toString(), params.toArray(), String.class));
        } catch (Exception e) {
            return 0;
        }
    }

    public List<Map<String, Object>> findSmartGames(String rulesJson, int limit) {
        if (rulesJson == null || rulesJson.isBlank()) return List.of();
        try {
            JsonNode rules = objectMapper.readTree(rulesJson);
            StringBuilder sql = new StringBuilder(
                "SELECT g.id, g.name, g.platform, g.release_date, " +
                "COALESCE(AVG(gr.rating), 0) AS avg_rating, " +
                "COALESCE(STRING_AGG(gg.genre_code, ','), '') AS genres " +
                "FROM library.game_data g " +
                "LEFT JOIN library.game_rating gr ON g.id = gr.game_id " +
                "LEFT JOIN library.game_data_genre gg ON g.id = gg.game_id "
            );
            List<Object> params = new ArrayList<>();
            buildSmartRulesConditions(rules, sql, params);
            sql.append(" GROUP BY g.id, g.name, g.platform, g.release_date");
            sql.append(" ORDER BY g.name ASC");
            if (limit > 0) sql.append(" LIMIT ").append(limit);
            return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rn) -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("gameId", rs.getLong("id"));
                m.put("name", rs.getString("name"));
                m.put("platform", rs.getString("platform"));
                m.put("releaseDate", rs.getString("release_date"));
                m.put("avgRating", Math.round(rs.getDouble("avg_rating") * 10.0) / 10.0);
                String genresStr = rs.getString("genres");
                m.put("genres", genresStr != null && !genresStr.isEmpty() ? List.of(genresStr.split(",")) : List.of());
                return m;
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private void buildSmartRulesConditions(JsonNode rules, StringBuilder sql, List<Object> params) {
        List<String> conditions = new ArrayList<>();

        if (rules.has("platforms") && rules.get("platforms").isArray() && !rules.get("platforms").isEmpty()) {
            List<String> platforms = new ArrayList<>();
            rules.get("platforms").forEach(n -> platforms.add(n.asText()));
            String placeholders = platforms.stream().map(p -> "?").collect(Collectors.joining(","));
            conditions.add("g.platform IN (" + placeholders + ")");
            params.addAll(platforms);
        }

        if (rules.has("genres") && rules.get("genres").isArray() && !rules.get("genres").isEmpty()) {
            List<String> genres = new ArrayList<>();
            rules.get("genres").forEach(n -> genres.add(n.asText()));
            String placeholders = genres.stream().map(p -> "?").collect(Collectors.joining(","));
            conditions.add("EXISTS (SELECT 1 FROM library.game_data_genre gg WHERE gg.game_id = g.id AND gg.genre_code IN (" + placeholders + "))");
            params.addAll(genres);
        }

        if (rules.has("yearFrom") && !rules.get("yearFrom").isNull()) {
            conditions.add("g.release_date >= ?");
            params.add(String.valueOf(rules.get("yearFrom").asInt()));
        }
        if (rules.has("yearTo") && !rules.get("yearTo").isNull()) {
            conditions.add("g.release_date <= ?");
            params.add(String.valueOf(rules.get("yearTo").asInt()));
        }

        if (rules.has("minRating") && !rules.get("minRating").isNull() && rules.get("minRating").asDouble() > 0) {
            conditions.add("(SELECT COALESCE(AVG(rating), 0) FROM library.game_rating WHERE game_id = g.id) >= ?");
            params.add(rules.get("minRating").asDouble());
        }

        if (rules.has("tags") && rules.get("tags").isArray() && !rules.get("tags").isEmpty()) {
            List<String> tags = new ArrayList<>();
            rules.get("tags").forEach(n -> tags.add(n.asText()));
            String placeholders = tags.stream().map(p -> "?").collect(Collectors.joining(","));
            conditions.add("EXISTS (SELECT 1 FROM library.game_data_tag gt WHERE gt.game_id = g.id AND gt.tag_code IN (" + placeholders + "))");
            params.addAll(tags);
        }

        if (rules.has("nameContains") && !rules.get("nameContains").isNull() && !rules.get("nameContains").asText().isBlank()) {
            conditions.add("g.name ILIKE ?");
            params.add("%" + rules.get("nameContains").asText().trim() + "%");
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
    }

    public boolean isOwner(Long collectionId, Long userId) {
        return collectionRepository.findById(collectionId)
                .map(c -> c.getUser().getId().equals(userId))
                .orElse(false);
    }

    public List<Map<String, Object>> getCollectionsWithHeroData(Long currentUserId) {
        List<GameCollection> mine = getUserCollections(currentUserId);
        List<GameCollection> shared = getPublicCollections();

        Set<Long> seen = new HashSet<>();
        List<GameCollection> all = new ArrayList<>();
        for (GameCollection c : mine) {
            all.add(c);
            seen.add(c.getId());
        }
        for (GameCollection c : shared) {
            if (!seen.contains(c.getId())) {
                all.add(c);
            }
        }

        if (all.isEmpty()) return List.of();

        List<Long> regularIds = all.stream()
                .filter(c -> !c.getIsSmart())
                .map(GameCollection::getId).toList();

        final Map<Long, Integer> countMap = new HashMap<>();
        final Map<Long, List<Object[]>> grouped = new HashMap<>();

        if (!regularIds.isEmpty()) {
            String inClause = regularIds.stream().map(String::valueOf).collect(Collectors.joining(","));

            countMap.putAll(jdbcTemplate.query(
                    "SELECT collection_id, COUNT(*) FROM library.game_collection_entry WHERE collection_id IN (" + inClause + ") GROUP BY collection_id",
                    (rs, rn) -> Map.entry(rs.getLong(1), rs.getInt(2))
            ).stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

            String sql = """
                    SELECT gce.collection_id, gce.game_id, g.name,
                           COALESCE(AVG(gr.rating), 0) AS avg_rating
                    FROM library.game_collection_entry gce
                    JOIN library.game_data g ON gce.game_id = g.id
                    LEFT JOIN library.game_rating gr ON g.id = gr.game_id
                    WHERE gce.collection_id IN (%s)
                    GROUP BY gce.collection_id, gce.game_id, g.name, gce.sort_order
                    ORDER BY gce.collection_id, avg_rating DESC, g.name ASC""".formatted(inClause);

            List<Object[]> rows = jdbcTemplate.query(sql, (rs, rn) -> new Object[]{
                    rs.getLong("collection_id"),
                    rs.getLong("game_id"),
                    rs.getString("name"),
                    rs.getDouble("avg_rating")
            });

            grouped.putAll(rows.stream().collect(Collectors.groupingBy(r -> (Long) r[0])));
        }

        return all.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("description", c.getDescription());
            m.put("isPublic", c.getIsPublic());
            m.put("isSmart", c.getIsSmart());
            m.put("userId", c.getUser().getId());
            m.put("username", c.getUser().getUsername());
            m.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null);

            List<Object[]> games;
            int gameCount;

            if (c.getIsSmart() && c.getSmartRules() != null) {
                gameCount = (int) countSmartGames(c.getSmartRules());
                List<Map<String, Object>> smartGames = findSmartGames(c.getSmartRules(), 4);
                games = smartGames.stream().map(sg -> new Object[]{
                        c.getId(), sg.get("gameId"), sg.get("name"), sg.get("avgRating")
                }).toList();
            } else {
                gameCount = countMap.getOrDefault(c.getId(), 0);
                games = grouped.getOrDefault(c.getId(), List.of());
            }

            m.put("gameCount", gameCount);

            if (games.isEmpty()) {
                m.put("heroGameId", null);
                m.put("heroGameName", null);
                m.put("previewGames", List.of());
                m.put("overflow", 0);
            } else {
                Object[] hero = games.get(0);
                m.put("heroGameId", hero[1]);
                m.put("heroGameName", hero[2]);

                List<Map<String, Object>> previews = new ArrayList<>();
                int total = games.size();
                int maxPreviews = Math.min(total, 2);
                for (int i = 0; i < maxPreviews; i++) {
                    Object[] g = games.get(i);
                    Map<String, Object> pg = new LinkedHashMap<>();
                    pg.put("gameId", g[1]);
                    pg.put("name", g[2]);
                    previews.add(pg);
                }
                m.put("previewGames", previews);
                int overflow = total - maxPreviews;
                m.put("overflow", Math.max(overflow, 0));
            }

            return m;
        }).collect(Collectors.toList());
    }
}
