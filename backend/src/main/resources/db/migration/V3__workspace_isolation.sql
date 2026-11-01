-- Workspace isolation migration for H2 and MySQL 8.0+

CREATE TABLE IF NOT EXISTS workspaces (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_hash VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed default legacy workspace for backward compatibility with pre-isolation data
INSERT INTO workspaces (id, key_hash, name)
VALUES (1, '0000000000000000000000000000000000000000000000000000000000000000', 'Legacy Default Workspace')
ON DUPLICATE KEY UPDATE id=id;

-- Add workspace_id columns to user-scoped resources
ALTER TABLE collections ADD COLUMN workspace_id BIGINT;
ALTER TABLE saved_requests ADD COLUMN workspace_id BIGINT;
ALTER TABLE environments ADD COLUMN workspace_id BIGINT;
ALTER TABLE request_history ADD COLUMN workspace_id BIGINT;

-- Assign any existing unassigned records to the legacy workspace
UPDATE collections SET workspace_id = 1 WHERE workspace_id IS NULL;
UPDATE saved_requests SET workspace_id = 1 WHERE workspace_id IS NULL;
UPDATE environments SET workspace_id = 1 WHERE workspace_id IS NULL;
UPDATE request_history SET workspace_id = 1 WHERE workspace_id IS NULL;

-- Create indexes for workspace-scoped query performance
CREATE INDEX idx_collections_workspace ON collections(workspace_id);
CREATE INDEX idx_saved_requests_workspace ON saved_requests(workspace_id);
CREATE INDEX idx_environments_workspace ON environments(workspace_id);
CREATE INDEX idx_request_history_workspace ON request_history(workspace_id);
