FROM pgvector/pgvector:pg16 AS builder

ARG SCWS_VERSION=1.2.3

RUN set -eux; \
    if [ -f /etc/apt/sources.list.d/debian.sources ]; then \
        sed -i \
            -e 's|http://deb.debian.org/debian|http://mirrors.aliyun.com/debian|g' \
            -e 's|http://deb.debian.org/debian-security|http://mirrors.aliyun.com/debian-security|g' \
            /etc/apt/sources.list.d/debian.sources; \
    fi; \
    if [ -f /etc/apt/sources.list.d/pgdg.list ]; then \
        sed -i \
            's|http://apt.postgresql.org/pub/repos/apt|http://mirrors.aliyun.com/postgresql/repos/apt|g' \
            /etc/apt/sources.list.d/pgdg.list; \
    fi; \
    printf 'Acquire::Retries "5";\nAcquire::http::Timeout "30";\nAcquire::https::Timeout "30";\n' \
        > /etc/apt/apt.conf.d/80-retries

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        autoconf \
        automake \
        build-essential \
        bzip2 \
        ca-certificates \
        git \
        libtool \
        pkg-config \
        postgresql-server-dev-16 \
        wget \
    && rm -rf /var/lib/apt/lists/*

RUN set -eux; \
    wget -q -O /tmp/scws.tar.bz2 \
        "http://www.xunsearch.com/scws/down/scws-${SCWS_VERSION}.tar.bz2"; \
    test -s /tmp/scws.tar.bz2 || (echo "failed to download scws" && exit 1); \
    mkdir -p /tmp/scws; \
    tar -xjf /tmp/scws.tar.bz2 -C /tmp/scws --strip-components=1; \
    cd /tmp/scws; \
    ./configure --prefix=/usr/local; \
    make -j"$(nproc)"; \
    make install; \
    rm -rf /tmp/scws /tmp/scws.tar.bz2

RUN set -eux; \
    rm -rf /tmp/zhparser; \
    git clone --depth 1 https://github.com/amutu/zhparser.git /tmp/zhparser; \
    echo "cloned latest zhparser from github"; \
    cd /tmp/zhparser; \
    SCWS_HOME=/usr/local \
    PG_CONFIG=/usr/lib/postgresql/16/bin/pg_config \
    make -j"$(nproc)"; \
    SCWS_HOME=/usr/local \
    PG_CONFIG=/usr/lib/postgresql/16/bin/pg_config \
    make install; \
    rm -rf /tmp/zhparser

FROM pgvector/pgvector:pg16

COPY --from=builder /usr/local/lib/libscws* /usr/local/lib/
COPY --from=builder /usr/local/etc /usr/local/etc
COPY --from=builder /usr/lib/postgresql/16/lib/zhparser.so /usr/lib/postgresql/16/lib/
COPY --from=builder /usr/share/postgresql/16/extension/zhparser* /usr/share/postgresql/16/extension/
COPY --from=builder /usr/share/postgresql/16/tsearch_data/dict.utf8.xdb /usr/share/postgresql/16/tsearch_data/
COPY --from=builder /usr/share/postgresql/16/tsearch_data/rules.utf8.ini /usr/share/postgresql/16/tsearch_data/

RUN set -eux; \
    libscws_real="$(basename "$(find /usr/local/lib -maxdepth 1 -type f -name 'libscws.so.*.*.*' | head -n 1)")"; \
    if [ -n "$libscws_real" ]; then \
        ln -sf "$libscws_real" /usr/local/lib/libscws.so.1; \
        ln -sf "$libscws_real" /usr/local/lib/libscws.so; \
    fi; \
    echo "/usr/local/lib" > /etc/ld.so.conf.d/local.conf; \
    ldconfig
