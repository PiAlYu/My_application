CREATE TABLE IF NOT EXISTS checklists (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS checklist_items (
  checklist_id TEXT NOT NULL,
  position INTEGER NOT NULL,
  name TEXT NOT NULL,
  PRIMARY KEY (checklist_id, position),
  FOREIGN KEY (checklist_id) REFERENCES checklists(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_checklists_updated_at ON checklists(updated_at DESC);
