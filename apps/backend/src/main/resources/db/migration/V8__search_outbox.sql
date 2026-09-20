-- Triggers make the outbox atomic for all existing repository writes, including cascades.
-- [jooq ignore start]
CREATE FUNCTION enqueue_search_change() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    kind text;
    extraction bigint := 0;
    event_key text;
BEGIN
    kind := CASE TG_TABLE_NAME WHEN 'projects' THEN 'PROJECT' WHEN 'tasks' THEN 'TASK'
            WHEN 'notes' THEN 'NOTE' ELSE 'FILE' END;
    IF TG_OP = 'UPDATE' THEN
        IF (to_jsonb(NEW) - 'tag_version') = (to_jsonb(OLD) - 'tag_version') THEN RETURN NEW; END IF;
    END IF;
    IF kind = 'FILE' THEN extraction := NEW.extraction_generation; END IF;
    event_key := 'search:' || kind || ':' || NEW.id || ':' || md5(to_jsonb(NEW)::text);
    INSERT INTO background_jobs(id,workspace_id,job_type,source_type,source_id,source_version,generation,dedupe_key)
        VALUES (gen_random_uuid(),NEW.workspace_id,'UPSERT_SEARCH',kind,NEW.id,NEW.version,1,event_key)
        ON CONFLICT (dedupe_key) DO NOTHING;
    RETURN NEW;
END;
$$;
CREATE TRIGGER projects_search_change AFTER INSERT OR UPDATE ON projects FOR EACH ROW EXECUTE FUNCTION enqueue_search_change();
CREATE TRIGGER tasks_search_change AFTER INSERT OR UPDATE ON tasks FOR EACH ROW EXECUTE FUNCTION enqueue_search_change();
CREATE TRIGGER notes_search_change AFTER INSERT OR UPDATE ON notes FOR EACH ROW EXECUTE FUNCTION enqueue_search_change();
CREATE TRIGGER files_search_change AFTER INSERT OR UPDATE ON files FOR EACH ROW EXECUTE FUNCTION enqueue_search_change();

INSERT INTO background_jobs(id,workspace_id,job_type,source_type,source_id,source_version,dedupe_key)
SELECT gen_random_uuid(),workspace_id,'UPSERT_SEARCH','PROJECT',id,version,'backfill:PROJECT:'||id FROM projects WHERE deleted_at IS NULL
UNION ALL SELECT gen_random_uuid(),workspace_id,'UPSERT_SEARCH','TASK',id,version,'backfill:TASK:'||id FROM tasks WHERE deleted_at IS NULL
UNION ALL SELECT gen_random_uuid(),workspace_id,'UPSERT_SEARCH','NOTE',id,version,'backfill:NOTE:'||id FROM notes WHERE deleted_at IS NULL;
-- [jooq ignore stop]
