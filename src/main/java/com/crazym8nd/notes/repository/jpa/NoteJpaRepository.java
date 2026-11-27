package com.crazym8nd.notes.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NoteJpaRepository extends JpaRepository<NoteEntity, UUID> {

    List<NoteEntity> findAllByOrderByCreatedAtDesc();

    List<NoteEntity> findByTagsOrderByCreatedAtDesc(String tag);
}
