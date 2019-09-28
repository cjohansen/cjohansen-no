--------------------------------------------------------------------------------
:type :meta
:title Securing HAProxy sites with Let's Encrypt SSL Certificates
:published #time/ldt "2018-03-10T12:00"
:tags [:haproxy :letsencrypt :https]
--------------------------------------------------------------------------------
:type :section
:theme :dark1
:title Securing HAProxy sites with Let's Encrypt SSL Certificates
:body

In this article:

- Provisioning free SSL/TLS certificates from Let's Encrypt
- Configuring HAProxy to serve multiple SSL domains
- Configuring HTTP -> HTTPS redirects in HAProxy
- Using certbot on Centos through Docker

--------------------------------------------------------------------------------
:type :section
:body

If you're serving websites (or APIs) with HAProxy in front, and you're looking
for how to get those sites set up with https, for free, then you've come to the
right place.

We'll start with a primer on using [certbot](https://certbot.eff.org/) to mostly
automate issuing fully valid and free SSL/TLS certificates, and then configure
HAProxy to use them.

## Environment

I am using Docker with a Docker network to run my apps. This way I don't have to
expose any ports, and services can talk via their container names (which double
as host names). To follow along, create a Docker network like so:

```sh
docker network create myworld
```

## Requesting new certificates

[certbot](https://certbot.eff.org/) is a tool from the EFF that automates most
of the process of acquiring a free SSL/TLS certificate from [Let's
Encrypt](https://letsencrypt.org/). You can probably install it with your
package manager, but I had some poor luck using the one from EPEL on Centos 7.
If your server has Docker, I recommend using the [official certbot
image](https://hub.docker.com/r/certbot/certbot/) for a trouble-free experience.

### The ACME Challenge

To request a new certificate for a domain, you must prove that you are its
owner. This done with an
[ACME](https://en.wikipedia.org/wiki/Automated_Certificate_Management_Environment)
challenge, which consists of serving some files over
[http://yourdomain.com/.well-known/acme-challenge/](http://yourdomain.com/.well-known/acme-challenge/).

Certbot comes with a bunch of plugins that can automate this completely for you
using Apache httpd, nginx, and others. If you have HAProxy in front you'll need
to do some work though, which suits me fine, as I like understanding roughly
what's going on.

Using the [webroot plugin](https://certbot.eff.org/docs/using.html#webroot)
allows you to fully control the web server - just point certbot at a directory
that is served by your web server, and it will complete the ACME challenge by
files in `WEBROOT/.well-known/acme-challenge/`. I will use nginx to serve these
files:

```sh
docker run \
    --name static-http \
    -v /home/deploy/letsencrypt:/usr/share/nginx/html \
    --restart always \
    --network myworld \
    nginx
```

### HAProxy configuration

To expose the files we will configure the nginx server as a backend for HAProxy.

**NB!** This only makes sense if you intend to use HAProxy for other things. If
you only have one static site, you might as well use nginx directly and forego
the additional setup.

Here's the frontend and backend for `haproxy.cfg` (defaults, globals, and other
sections omitted):

```sh
frontend http-in
    bind *:80
    compression algo gzip
    compression type text/html text/plain text/javascript application/javascript application/xml text/css
    option accept-invalid-http-request
    acl is_well_known path_beg -i /.well-known
    use_backend letsencrypt if is_well_known

backend letsencrypt
    mode http
    balance roundrobin
    option forwardfor
    http-request set-header X-Forwarded-Port %[dst_port]
    server static-http static-http
```

With this file in `/home/deploy/haproxy/haproxy.cfg`, run the HAProxy Docker
container like so:

```sh
docker run \
    --name load-balancer \
    -p 80:80 \
    --restart always \
    -v /home/deploy/haproxy:/config \
    --network myworld \
    haproxy:1.7 \
    haproxy -f /config/haproxy.cfg
```

Because the container is in the same Docker network as the nginx container, it
can reach it over the hostname `static-http`. Exposing HAProxy's port 80 on the
host's port 80 creates a link to the outer world. If your site's DNS is
configured correctly, you should now be able to reach files in
`/home/deploy/letsencrypt/.well-known` from `http://yoursite.com/.well-known`.

### Requesting the certificate

Now we have enough infrastructure to let certbot conduct the ACME challenge on
our behalf:

```sh
mkdir /etc/letsencrypt

docker run -i --rm --name certbot \
    -v /etc/letsencrypt:/etc/letsencrypt \
    -v /home/deploy/letsencrypt:/webroot \
    certbot/certbot certonly \
    --webroot \
    -w /webroot \
    -d mysite.com \
    --email christian@cjohansen.no \
    --non-interactive \
    --agree-tos
```

If all goes well, you should now have a freshly issued SSL/TLS certificate for
your site in `/etc/letsencrypt/live/mysite.com`.

## Serving HTTPS from HAProxy

To use your newly acquired SSL certificates with HAProxy, you must combine their
private keys and certificate:

```sh
mkdir /etc/letsencrypt/haproxy
cat /etc/letsencrypt/live/site-a.com/privkey.pem \
    /etc/letsencrypt/live/site-a.com/fullchain.pem \
    > /etc/letsencrypt/haproxy/site-a.com.pem
```

HAProxy supports [Server Name Indication
(SNI)](https://en.wikipedia.org/wiki/Server_Name_Indication), which allows you
to serve multiple HTTPS websites from the same IP address by including the
hostname in the TLS handshake. Just tell HAProxy about all your certificates,
and it'll figure out the rest.

If you have more than one certificate, you can concatenate them all in one go
like this:

```sh
function cat-cert() {
  dir="/etc/letsencrypt/live/$1"
  cat "$dir/privkey.pem" "$dir/fullchain.pem" > "/etc/letsencrypt/haproxy/$1.pem"
}

for dir in /etc/letsencrypt/live/*; do
  cat-cert $(basename "$dir")
done
```

Let's say you provisioned certificates for two sites, `site-a.com` and
`site-b.com`, and concatenated them into `/etc/letsencrypt/haproxy` as suggested
above. Assuming the certificate directory is exposed as the volume `/ssl-certs`
in the HAProxy container, you can create an HTTPS frontend as such:

```sh
frontend https-in
    bind *:443 ssl crt /ssl-certs/site-a.com.pem crt /ssl-certs/site-b.com.pem
    compression algo gzip
    compression type text/html text/plain text/javascript application/javascript application/xml text/css
    option accept-invalid-http-request

    use_backend site-a if { hdr_end(host) -i site-a.com }
    use_backend site-b if { hdr_end(host) -i site-b.com }
```

With this configuration in place, restart HAProxy with Docker the following way:

```sh
docker run \
    --name load-balancer \
    -p 80:80 \
    --restart always \
    -v /etc/letsencrypt/haproxy:/ssl-certs \
    -v /home/deploy/haproxy:/config \
    --network myworld \
    haproxy:1.7 \
    haproxy -f /config/haproxy.cfg
```

The only difference from before is the added `/ssl-certs` volume.

## Redirecting HTTP to HTTPS in HAProxy

Now that our sites have SSL certificates, we want to serve all traffic over
HTTPS. One way to do this is to redirect all attempts at HTTP to HTTPS. As we do
this, keep in mind that the ACME challenge needs to be performed over HTTP, so
there should be an exception for those URLs:

```sh
frontend http-in
    bind *:80
    compression algo gzip
    compression type text/html text/plain text/javascript application/javascript application/xml text/css
    option accept-invalid-http-request
    acl is_well_known path_beg -i /.well-known

    # Add this line
    redirect scheme https code 301 if !is_well_known !{ ssl_fc }

    use_backend letsencrypt if is_well_known
```

HAProxy processes redirects before backend assignment, and will issue a warning
in the logs if you place them out of order. It's not technically an error, but
it is potentially confusing.

## HSTS

[HSTS](https://en.wikipedia.org/wiki/HTTP_Strict_Transport_Security), or
[HTTP Strict Transport Security](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Strict-Transport-Security),
is security mechanism that avoids some phishing scenarios by informing the
browser to _never_ access the site over plain HTTP. Add this header to all
responses going out of HAProxy like so (include it in either your frontend or
backend configuration):

```sh
http-response set-header Strict-Transport-Security "max-age=16000000; includeSubDomains; preload;"
```

## Renewing certificates

Let's Encrypt certificates are [valid for 90 days](https://letsencrypt.org/2015/11/09/why-90-days.html). 
To ensure your site stays well-configured, you should renew certificates in a
cronjob. The renewal process goes like this:

- Call `certbot renew`
- Re-concatenate certificates
- Reload HAProxy's configuration by sending it a `SIGHUP`

Here's a script you can run from a cronjob that does just that, assuming the
same directories as used above:

```sh
#!/bin/bash

set -e

echo "$(date) About to renew certificates" >> /var/log/letsencrypt-renew.log
/usr/bin/docker run \
       -i \
       --rm \
       --name certbot \
       -v /etc/letsencrypt:/etc/letsencrypt \
       -v /home/deploy/letsencrypt:/webroot \
       certbot/certbot \
       renew -w /webroot

echo "$(date) Cat certificates" >> /var/log/letsencrypt-renew.log

function cat-cert() {
  dir="/etc/letsencrypt/live/$1"
  cat "$dir/privkey.pem" "$dir/fullchain.pem" > "/etc/letsencrypt/haproxy/$1.pem"
}

for dir in /etc/letsencrypt/live/*; do
  cat-cert $(basename "$dir")
done

echo "$(date) Reload haproxy" >> /var/log/letsencrypt-renew.log
/usr/bin/docker kill -s HUP load-balancer

echo "$(date) Done" >> /var/log/letsencrypt-renew.log
```
