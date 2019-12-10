'use strict';

exports.handler = (event, context, callback) => {
  const request = event.Records[0].cf.request;

  if (!/\..+/.test(request.uri)) {
    request.uri = `${request.uri.replace(/\/$/, '')}/index.html`;
  }

  callback(null, request);
};
