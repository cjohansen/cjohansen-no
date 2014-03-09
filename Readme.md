# cjohansen-no

This is a demonstration app that shows how to put together a static website with
Clojure and these libraries:

* [Stasis](http://github.com/magnars/stasis)
* [Optimus](http://github.com/magnars/optimus)
* [enlive](https://github.com/cgrand/enlive)
* [hiccup](https://github.com/weavejester/hiccup)
* [cegdown](https://github.com/Raynes/cegdown)
* [clygments](https://github.com/bfontaine/clygments)
* [Midje](https://github.com/marick/Midje)

The hows and whys are explained in thorough detail in
[this post](http://cjohansen.no/building-static-sites-with-stasis).

This repo will eventually evolve into my new website.

## Usage

Export site to disk

```sh
lein build-site
```

Run the live server

```sh
lein ring server-headless
```

## License

BSD 2 Clause license.

Copyright © 2014 Christian Johansen

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
