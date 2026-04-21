CREATE EXTENSION IF NOT EXISTS vector;

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

CREATE INDEX IF NOT EXISTS book_embeddings_content_fts_idx
    ON public.book_embeddings
    USING gin (to_tsvector('simple', coalesce(content, '')));
