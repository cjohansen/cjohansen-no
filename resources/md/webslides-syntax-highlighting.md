# Webslides Syntax Highlighting with Prism

I recently discovered [Webslides](https://webslides.tv/), yet another HTML slide
framework. It is *really* well designed and comes with an impressively
comprehensive toolkit for composing slides. The only thing missing out of the
box was easy syntax highlighting of code snippets. Luckily, adding that with
[Prism.js](http://prismjs.com) is a breeze.

Download and unpack the
[Webslides bundle](https://webslides.tv/webslides-latest.zip). Then go to
[Prism's download page](http://prismjs.com/download.html) to customize your
build - choosing the theme, the languages you need support for, and optionally
any plugins. Download `prism.js` and `prism.css`.

Webslides comes with a default `index.html`, include the Prism assets in it,
then mark up your code samples using

```html
<pre class="language-clojure"><code>(println "Hello wold")</code></pre>
```

Replace `clojure` with the language of your choice (make sure you enabled
support for them when you downloaded Prism).

There is a [demo](/webslides-pygments-demo/). Its
[full source is on Github](https://github.com/cjohansen/cjohansen-no/tree/master/resources/public/webslides-pygments-demo).
Below is the essential parts of the HTML.

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Webslides Pygments Demo</title>
    <meta name="description" content="Syntax highlighting Webslides with Pygments">
    <link href="https://fonts.googleapis.com/css?family=Roboto:100,100i,300,300i,400,400i,700,700i%7CMaitree:200,300,400,600,700&amp;subset=latin-ext" rel="stylesheet">
    <link rel="stylesheet" type='text/css' media='all' href="static/css/webslides.css">
    <link rel="stylesheet" type='text/css' media='all' href="static/css/prism.css">
  </head>
  <body>
    <main role="main">
      <article id="webslides" class="vertical">
        <section>
          <span class="background"></span>
          <div class="wrap aligncenter">
            <h1><strong>Webslides with Pygments</strong></h1>
            <p><a href="/webslides-syntax-highlighting/">Read the post</a></p>
          </div>
        </section>
        <section>
          <span class="background"></span>
          <div class="wrap aligncenter">
            <pre class="language-clojure"><code>(println "Hello world")</code></pre>
          </div>
        </section>
      </article>
    </main>

    <script src="static/js/prism.js"></script>
    <script src="static/js/webslides.js"></script>
    <script>
      window.ws = new WebSlides();
    </script>
  </body>
</html>
```
