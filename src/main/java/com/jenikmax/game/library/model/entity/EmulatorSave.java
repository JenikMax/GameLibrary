package com.jenikmax.game.library.model.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Сущность сейва браузерного эмулятора (EmulatorJS).
 * Хранит сейв-файл (srm — батарейка) или снимок состояния (state)
 * для пары игра + пользователь, в слоте с номером slot.
 */
@Entity
@Table(name = "emulator_save")
@SequenceGenerator(
        name = "emulator_save_id_gen",
        allocationSize = 1,
        sequenceName = "emulator_save_id_seq")
public class EmulatorSave implements Serializable {

    private Long id;
    private Game game;
    private User user;
    private String kind;
    private int slot;
    private String name;
    private byte[] data;
    private Long sizeBytes;
    private Timestamp updatedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "emulator_save_id_gen")
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @ManyToOne
    @JoinColumn(name = "game_id")
    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    @ManyToOne
    @JoinColumn(name = "user_id")
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    @Column(name = "kind")
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    @Column(name = "slot")
    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }

    @Column(name = "name")
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Column(name = "data")
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    @Column(name = "size_bytes")
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    @Column(name = "updated_at")
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
