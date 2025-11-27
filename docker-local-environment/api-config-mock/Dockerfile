FROM node:18.19.0-alpine@sha256:0085670310d2879621f96a4216c893f92e2ded827e9e6ef8437672e1bd72f437

COPY ./ /usr/src/app

WORKDIR /usr/src/app

RUN yarn global add json-server@0.17.4 --exact

EXPOSE 3000

CMD ["json-server", "db.json", "--routes", "routes.json", "--host", "0.0.0.0"]