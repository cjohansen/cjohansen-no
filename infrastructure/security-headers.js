'use strict';

exports.handler = (event, context, callback) => {
  const response = event.Records[0].cf.response;

  response.headers = Object.assign(response.headers, {
    "Content-Security-Policy": [{
      key: "Content-Security-Policy",
      value: "default-src 'none'; connect-src 'self'; font-src 'self'; img-src data: 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'"
    }],
    "Strict-Transport-Security": [{
      key: "Strict-Transport-Security",
      value: "max-age=31536000"
    }],
    "X-Frame-Options": [{
      key: "X-Frame-Options",
      value: "deny"
    }],
    "X-Content-Type-Options": [{
      key: "X-Content-Type-Options",
      value: "nosniff"
    }],
    "Referrer-Policy": [{
      key: "Referrer-Policy",
      value: "strict-origin"
    }],
    "Feature-Policy": [{
      key: "Feature-Policy",
      value: "geolocation 'none'; midi 'none'; notifications 'none'; push 'none'; sync-xhr 'none'; microphone 'none'; camera 'none'; magnetometer 'none'; gyroscope 'none'; speaker 'none'; vibrate 'none'; fullscreen 'none'; payment 'none'"
    }]
  });

  callback(null, response);
};
