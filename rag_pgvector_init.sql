CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS zhparser;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_ts_config cfg
        JOIN pg_namespace ns ON ns.oid = cfg.cfgnamespace
        WHERE ns.nspname = 'public'
          AND cfg.cfgname = 'wsb_zhcfg'
    ) THEN
        CREATE TEXT SEARCH CONFIGURATION public.wsb_zhcfg (PARSER = zhparser);
        ALTER TEXT SEARCH CONFIGURATION public.wsb_zhcfg ADD MAPPING FOR n, v, a, i, e, l WITH simple;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.book_embeddings (
    id uuid PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(1024)
);

CREATE INDEX IF NOT EXISTS book_embeddings_embedding_idx
    ON public.book_embeddings
    USING hnsw (embedding vector_cosine_ops);

CREATE INDEX IF NOT EXISTS book_embeddings_book_id_idx
    ON public.book_embeddings ((metadata->>'bookId'));

DROP INDEX IF EXISTS public.book_embeddings_content_fts_idx;

CREATE INDEX IF NOT EXISTS book_embeddings_content_zh_fts_idx
    ON public.book_embeddings
    USING gin (to_tsvector('public.wsb_zhcfg', coalesce(content, '')));

CREATE INDEX IF NOT EXISTS book_embeddings_content_trgm_idx
    ON public.book_embeddings
    USING gin (content gin_trgm_ops);

CREATE INDEX IF NOT EXISTS book_embeddings_title_trgm_idx
    ON public.book_embeddings
    USING gin ((metadata->>'title') gin_trgm_ops);

CREATE INDEX IF NOT EXISTS book_embeddings_author_trgm_idx
    ON public.book_embeddings
    USING gin ((metadata->>'author') gin_trgm_ops);

CREATE INDEX IF NOT EXISTS book_embeddings_keyword_trgm_idx
    ON public.book_embeddings
    USING gin ((metadata->>'keyword') gin_trgm_ops);

CREATE INDEX IF NOT EXISTS book_embeddings_clc_category_trgm_idx
    ON public.book_embeddings
    USING gin ((metadata->>'clcCategory') gin_trgm_ops);

CREATE INDEX IF NOT EXISTS book_embeddings_clc_trgm_idx
    ON public.book_embeddings
    USING gin ((metadata->>'clc') gin_trgm_ops);
