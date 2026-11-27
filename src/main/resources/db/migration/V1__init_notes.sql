CREATE TABLE notes (
    id          UUID PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    content     VARCHAR(10000) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE note_tags (
    note_id UUID NOT NULL REFERENCES notes (id) ON DELETE CASCADE,
    tag     VARCHAR(50) NOT NULL,
    PRIMARY KEY (note_id, tag)
);

CREATE INDEX idx_note_tags_tag ON note_tags (tag);
