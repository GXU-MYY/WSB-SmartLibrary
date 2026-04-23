FROM pgvector/pgvector:pg16

ARG SCWS_VERSION=1.2.3

RUN set -eux; \
    sed -i \
        -e 's|http://deb.debian.org/debian|http://mirrors.aliyun.com/debian|g' \
        -e 's|http://deb.debian.org/debian-security|http://mirrors.aliyun.com/debian-security|g' \
        /etc/apt/sources.list.d/debian.sources; \
    sed -i \
        's|http://apt.postgresql.org/pub/repos/apt|http://mirrors.aliyun.com/postgresql/repos/apt|g' \
        /etc/apt/sources.list.d/pgdg.list; \
    printf 'Acquire::Retries "5";\nAcquire::http::Timeout "30";\nAcquire::https::Timeout "30";\n' > /etc/apt/apt.conf.d/80-retries; \
    apt-get update \
    && apt-get install -y --no-install-recommends \
        autoconf \
        automake \
        bzip2 \
        build-essential \
        ca-certificates \
        git \
        libtool \
        pkg-config \
        postgresql-server-dev-16 \
        wget \
    && wget -q -O /tmp/scws.tar.bz2 "http://www.xunsearch.com/scws/down/scws-${SCWS_VERSION}.tar.bz2" \
    && mkdir -p /tmp/scws \
    && tar -xjf /tmp/scws.tar.bz2 -C /tmp/scws --strip-components=1 \
    && cd /tmp/scws \
    && ./configure \
    && make -j"$(nproc)" \
    && make install \
    && git clone --depth 1 https://github.com/amutu/zhparser.git /tmp/zhparser \
    && cd /tmp/zhparser \
    && SCWS_HOME=/usr/local PG_CONFIG=/usr/lib/postgresql/16/bin/pg_config make -j"$(nproc)" \
    && SCWS_HOME=/usr/local PG_CONFIG=/usr/lib/postgresql/16/bin/pg_config make install \
    && echo "/usr/local/lib" > /etc/ld.so.conf.d/local.conf \
    && ldconfig \
    && apt-get purge -y --auto-remove \
        autoconf \
        automake \
        build-essential \
        git \
        libtool \
        pkg-config \
        postgresql-server-dev-16 \
        wget \
    && rm -rf /var/lib/apt/lists/* /tmp/scws /tmp/scws.tar.bz2 /tmp/zhparser
