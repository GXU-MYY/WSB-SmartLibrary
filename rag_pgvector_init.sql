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
