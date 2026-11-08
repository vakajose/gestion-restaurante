-- Migration V14: Add image_url to dishes for visual catalog (TASK-506 / PB-036 / ADR-0021)
ALTER TABLE dishes ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
