package com.jenikmax.game.library.model.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "game_collection_entry", schema = "library",
       uniqueConstraints = @UniqueConstraint(columnNames = {"collection_id", "game_id"}))
@SequenceGenerator(name = "game_collection_entry_id_gen", allocationSize = 1, sequenceName = "game_collection_entry_id_seq")
public class GameCollectionEntry implements Serializable {

    private Long id;
    private GameCollection collection;
    private Long gameId;
    private int sortOrder;
    private Timestamp addedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_collection_entry_id_gen")
    @Column(name = "id", unique = true, nullable = false)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    public GameCollection getCollection() { return collection; }
    public void setCollection(GameCollection collection) { this.collection = collection; }

    @Column(name = "game_id", nullable = false)
    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }

    @Column(name = "sort_order", nullable = false)
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    @Column(name = "added_at")
    public Timestamp getAddedAt() { return addedAt; }
    public void setAddedAt(Timestamp addedAt) { this.addedAt = addedAt; }
}
