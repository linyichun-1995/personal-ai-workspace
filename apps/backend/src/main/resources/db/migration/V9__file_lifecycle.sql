-- [jooq ignore start]
CREATE FUNCTION synchronize_file_lifecycle() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_TABLE_NAME='projects' THEN
        IF OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
            UPDATE files SET state='DELETED',deleted_at=NEW.deleted_at,purge_after=NEW.deleted_at+interval '30 days',
                deletion_reason='PROJECT_DELETED',version=version+1,updated_at=NEW.deleted_at
                WHERE workspace_id=NEW.workspace_id AND project_id=NEW.id AND state='STORED';
        END IF;
    ELSIF TG_TABLE_NAME='tasks' THEN
        IF NEW.project_id IS DISTINCT FROM OLD.project_id AND EXISTS(SELECT 1 FROM task_files WHERE workspace_id=NEW.workspace_id AND task_id=NEW.id) THEN
            RAISE EXCEPTION 'FILE_HAS_REFERENCES' USING ERRCODE='23514';
        END IF;
        IF OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
            DELETE FROM task_files WHERE workspace_id=NEW.workspace_id AND task_id=NEW.id;
        END IF;
    ELSE
        IF NEW.project_id IS DISTINCT FROM OLD.project_id AND EXISTS(SELECT 1 FROM note_files WHERE workspace_id=NEW.workspace_id AND note_id=NEW.id) THEN
            RAISE EXCEPTION 'FILE_HAS_REFERENCES' USING ERRCODE='23514';
        END IF;
        IF OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
            DELETE FROM note_files WHERE workspace_id=NEW.workspace_id AND note_id=NEW.id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER projects_file_lifecycle AFTER UPDATE ON projects FOR EACH ROW EXECUTE FUNCTION synchronize_file_lifecycle();
CREATE TRIGGER tasks_file_lifecycle BEFORE UPDATE ON tasks FOR EACH ROW EXECUTE FUNCTION synchronize_file_lifecycle();
CREATE TRIGGER notes_file_lifecycle BEFORE UPDATE ON notes FOR EACH ROW EXECUTE FUNCTION synchronize_file_lifecycle();
-- [jooq ignore stop]
