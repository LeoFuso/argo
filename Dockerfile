
FROM alpine/git:2.36.3 as git
RUN git clone https://github.com/LeoFuso/argo.git /argo-build

FROM openjdk:17-alpine as jdk

COPY --from=git argo-build/ /argo/

ENV CI=true

RUN cd argo && ./gradlew :check




