package com.jenikmax.game.library.model.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "game_collection", schema = "library")
@SequenceGenerator(name = "game_collection_id_gen", allocationSize = 1, sequenceName = "game_collection_id_seq")
public class GameCollection implements Serializable {

    private Long id;
    private String name;
    private String description;
    private User user;
    private boolean isPublic;
    private boolean isSmart;
    private String smartRules;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_collection_id_gen")
    @Column(name = "id", unique = true, nullable = false)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Column(name = "name", nullable = false, length = 200)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Column(name = "description")
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    @Column(name = "is_public", nullable = false)
    public boolean getIsPublic() { return isPublic; }
    public void setIsPublic(boolean isPublic) { this.isPublic = isPublic; }

    @Column(name = "is_smart", nullable = false)
    public boolean getIsSmart() { return isSmart; }
    public void setIsSmart(boolean isSmart) { this.isSmart = isSmart; }

    @Column(name = "smart_rules", columnDefinition = "text")
    public String getSmartRules() { return smartRules; }
    public void setSmartRules(String smartRules) { this.smartRules = smartRules; }

    @Column(name = "created_at")
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Column(name = "updated_at")
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
