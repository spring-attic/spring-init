FROM springci/spring-graalvm-native:20.1-dev-java11

ARG USER
ARG USER_ID
ARG USER_GID

RUN (groupadd --gid "${USER_GID}" "${USER}" || echo "No groupadd needed") && \
    useradd \
      --uid ${USER_ID} \
      --gid ${USER_GID} \
      --create-home \
      --shell /bin/bash \
      ${USER}
