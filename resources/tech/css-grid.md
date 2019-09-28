--------------------------------------------------------------------------------
:type :meta
:title CSS Grid
:locale :nb
:published #time/ldt "2019-07-03T12:00"
:tags [:css]
:description

CSS grids har endelig gitt oss en enkel og fleksibel modell for layout på nett.
Med bare noen få properties kan du stort sett pakke `float` helt bort, få full
kontroll over kilde-rekkefølge og et kraftig verktøy for responsiv design på
kjøpet.

--------------------------------------------------------------------------------
:type :section
:section-type :centered
:theme :dark1
:title CSS Grid
:body

CSS grids har endelig gitt oss en enkel og fleksibel modell for layout på nett.
Med bare noen få properties kan du stort sett pakke `float` helt bort, få full
kontroll over kilde-rekkefølge og et kraftig verktøy for responsiv design på
kjøpet.

--------------------------------------------------------------------------------
:type :section
:body

[CSS grids](https://www.w3.org/TR/css-grid-1/) var ferdig spekket i desember
2017, og har [bred nok nettleser-støtte](https://caniuse.com/#search=grid) til å
kunne brukes omtrent hvor enn det skulle være. Grids er enkle å jobbe med, og
gir deg så god kontroll at rekkefølge/plassering i HTML er fullstendig adskilt
fra visuelt resultat. Det er dermed et ideellt verktøy for responsive løsninger.
I dette innlegget skal jeg vise et par grunnleggende properties som godt viser
mulighetsrommet for CSS grids.

## Kolonner

For å lage et grid trenger du kun et container-element med `display: grid` og
valgfri spesifikasjon av antall og dimensjonene på kolonnene og/eller radene.
Barne-elementene til containeren vil så plassere seg inn i grid-et i den
rekkefølgen de opptrer i kilden (mer om å bøye denne regelen senere). Her er et
grid med to kolonner - én på 200 piksler, og den andre resten av tilgjengelig
plass:

```css
.grid-container {
  display: grid;
  grid-template-columns: 200px auto;
}
```

```html
<div class="grid-container">
  <div class="child1"><p>Dette er element #1</p></div>
  <div class="child2"><p>Dette er element #2</p></div>
  <div class="child3"><p>Dette er element #3</p></div>
  <div class="child4"><p>Dette er element #4</p></div>
  <div class="child5"><p>Dette er element #5</p></div>
  <div class="child6"><p>Dette er element #6</p></div>
</div>
```

<style type="text/css">
.grid-container1 {
  display: grid;
  grid-template-columns: 200px auto;
}

.child1 { background: #247ba0; color: #fff; }
.child2 { background: #70c1b3; }
.child3 { background: #b2dbbf; }
.child4 { background: #f3ffbd; }
.child5 { background: #ff1654; color: #fff; }
.child6 { background: #247ba0; color: #fff; }
.grid-ex {margin: 0 0 3rem;}
</style>

<div class="grid-ex">
  <div class="grid-container1">
    <div class="child1"><p>Dette er element #1</p></div>
    <div class="child2"><p>Dette er element #2</p></div>
    <div class="child3"><p>Dette er element #3</p></div>
    <div class="child4"><p>Dette er element #4</p></div>
    <div class="child5"><p>Dette er element #5</p></div>
    <div class="child6"><p>Dette er element #6</p></div>
  </div>
</div>

## Rader

Du kan tilsvarende si noe om høyden på radene med `grid-template-rows`. Dersom
du har så mange elementer at det blir flere rader enn `grid-template-rows`
beskriver vil påfølgende rader opptre som om de var spesifisert med `auto`.

```css
.grid-container {
  display: grid;
  grid-template-columns: 25% auto;
  grid-template-rows: auto 200px;
}
```

<style type="text/css">
.grid-container2 {
  display: grid;
  grid-template-columns: 25% auto;
  grid-template-rows: auto 200px;
}
</style>

<div class="grid-ex">
  <div class="grid-container2">
    <div class="child1"><p>Dette er element #1</p></div>
    <div class="child2"><p>Dette er element #2</p></div>
    <div class="child3"><p>Dette er element #3</p></div>
    <div class="child4"><p>Dette er element #4</p></div>
    <div class="child5"><p>Dette er element #5</p></div>
    <div class="child6"><p>Dette er element #6</p></div>
  </div>
</div>

## Tables go home

Krydrer du denne løsningen med `grid-gap`, `grid-column` og `grid-row`, så har
du en løsning som er like god som de gode gamle tabellene var (disse matcher
henholdsvis `cellpadding`, `colspan` og `rowspan`). Du kan sågar også bestemme
justering i begge retninger:

```css
.grid-container3 {
  display: grid;
  grid-gap: 10px;
  grid-template-columns: 25% auto 25%;
  grid-template-rows: auto 200px;
}

.child1 {
  grid-column: 1 / 3;
}

.child2 {
  grid-column: 3;
  grid-row: 1 / 4;
}
```

<style type="text/css">
.grid-container3 {
  display: grid;
  grid-gap: 10px;
  grid-template-columns: 25% auto 25%;
  grid-template-rows: auto 200px;
}

.grid-container3 .child1 { grid-column: 1 / 3; }
.grid-container3 .child2 { grid-column: 3; grid-row: 1 / 4; }
</style>

<div class="grid-ex">
  <div class="grid-container3">
    <div class="child1"><p>Dette er element #1</p></div>
    <div class="child2"><p>Dette er element #2</p></div>
    <div class="child3"><p>Dette er element #3</p></div>
    <div class="child4"><p>Dette er element #4</p></div>
    <div class="child5"><p>Dette er element #5</p></div>
    <div class="child6"><p>Dette er element #6</p></div>
  </div>
</div>

Den midterste kolonnen over får `auto` bredde, ikke 50%. På denne måten blir det
plass til grid gaps uten at griddet blir bredere enn plassen det har
tilgjengelig (`25% + 10px + 50% + 10px + 25%`).

## Grid units

Hittil har vi angitt rad- og kolonnedimensjoner med prosent, piksler og "auto".
Men den aller feteste enheten er den relative grid-enheten `fr`. Griddets fulle
bredde er summen av alle `fr`-enheter du har tildelt kolonnene, og en enkelt
kolonne vil ta opp den andelen plass som den har `fr`-enheter relativt til
totalen. Puh! Kort sagt: med tre kolonner på `1fr`, så vil her kolonne ta opp en
tredjedel av plassen:

```css
.grid-container4 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
}
```

<style type="text/css">
.grid-container4 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
}
</style>

<div class="grid-ex">
  <div class="grid-container4">
    <div class="child1"><p>Dette er element #1</p></div>
    <div class="child2"><p>Dette er element #2</p></div>
    <div class="child3"><p>Dette er element #3</p></div>
    <div class="child4"><p>Dette er element #4</p></div>
    <div class="child5"><p>Dette er element #5</p></div>
    <div class="child6"><p>Dette er element #6</p></div>
  </div>
</div>

For å lage en grid på 2 + 1 + 3 kan du bare fordele flere `fr`-enheter:

```css
.grid-container5 {
  display: grid;
  grid-template-columns: 2fr 1fr 3fr;
}
```

<style type="text/css">
.grid-container5 {
  display: grid;
  grid-template-columns: 2fr 1fr 3fr;
}
</style>

<div class="grid-ex">
  <div class="grid-container5">
    <div class="child1"><p>Dette er element #1</p></div>
    <div class="child2"><p>Dette er element #2</p></div>
    <div class="child3"><p>Dette er element #3</p></div>
    <div class="child4"><p>Dette er element #4</p></div>
    <div class="child5"><p>Dette er element #5</p></div>
    <div class="child6"><p>Dette er element #6</p></div>
  </div>
</div>

## Full fleks

Så langt har vi sett hvordan grids enkelt og elegant løser en del notorisk
knotete problemer i CSS. Men enda gjenstår det beste: `grid-template-areas`.
Dette er kremen på kaka som lar deg skille strukturell representasjon
(HTML-elementene) og visuell representasjon totalt. Det fungerer sånn at du gir
container-elementet en ascii-art-representasjon av hvilke elementer som skal
hvor i griddet, og deretter navngir du hvert av barne-elementene. Vips, så får
du elementene dine plassert akkurat hvor du vil.

```css
.grid-container6 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  grid-template-areas: "child5 child1 child4"
                       "child2 child6 child3"
}

.child1 { grid-area: child1; }
.child2 { grid-area: child2; }
.child3 { grid-area: child3; }
.child4 { grid-area: child4; }
.child5 { grid-area: child5; }
.child6 { grid-area: child6; }
```

<style type="text/css">
.grid-container6 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  grid-template-areas: "child5 child1 child4"
                       "child2 child6 child3"
}

.grid-container6 .child1 { grid-area: child1; }
.grid-container6 .child2 { grid-area: child2; }
.grid-container6 .child3 { grid-area: child3; }
.grid-container6 .child4 { grid-area: child4; }
.grid-container6 .child5 { grid-area: child5; }
.grid-container6 .child6 { grid-area: child6; background: #000; }
</style>

<div class="grid-ex">
  <div class="grid-container6">
    <div class="child1"><p>Dette er element #1</p></div>
    <div class="child2"><p>Dette er element #2</p></div>
    <div class="child3"><p>Dette er element #3</p></div>
    <div class="child4"><p>Dette er element #4</p></div>
    <div class="child5"><p>Dette er element #5</p></div>
    <div class="child6"><p>Dette er element #6</p></div>
  </div>
</div>

Som om ikke dete var stilig nok i seg selv kan du også bruke dette til å veldig
visuelt og fint få elementer til å bre seg over flere rader og/eller kolonner:

```css
.grid-container7 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  grid-template-rows: 1fr 1fr 1fr 1fr;
  grid-template-areas: "child5 child1 child1"
                       "child5 child2 child2"
                       "child4 child2 child2"
                       "child3 child3 child6";
}
```

<style type="text/css">
.grid-container7 {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  grid-template-rows: 1fr 1fr 1fr 1fr;
  grid-template-areas: "child5 child1 child1"
                       "child5 child2 child2"
                       "child4 child2 child2"
                       "child3 child3 child6";
}

@media screen and (max-width: 800px) {
  .grid-container7 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    grid-template-areas: "child3 child4"
                         "child3 child1"
                         "child2 child1"
                         "child6 child5";
  }
}

.grid-container7 .child1 { grid-area: child1; }
.grid-container7 .child2 { grid-area: child2; }
.grid-container7 .child3 { grid-area: child3; }
.grid-container7 .child4 { grid-area: child4; }
.grid-container7 .child5 { grid-area: child5; }
.grid-container7 .child6 { grid-area: child6; background: #000; }
</style>

<div class="grid-ex">
  <div class="grid-container7">
    <div class="child1"><p>Dette er element #1</p></div>
    <div class="child2"><p>Dette er element #2</p></div>
    <div class="child3"><p>Dette er element #3</p></div>
    <div class="child4"><p>Dette er element #4</p></div>
    <div class="child5"><p>Dette er element #5</p></div>
    <div class="child6"><p>Dette er element #6</p></div>
  </div>
</div>

<strong>NB!</strong> Dette eksempelet får en layout med to kolonner (se
nedenfor) på mobil og skjermer under 800px, tre kolonner på skjermer som er
store nok.

For ordens skyld nevner jeg at det også finnes andre måter å gjøre colspan og
rowspan i CSS grids, som ikke baserer seg på `grid-template-areas`.

Jeg nevnte innledningsvis at CSS grids gir en ideell løsning på responsive
layouts, og for et eksempel kan du se på ovenstående grid ved å gjøre
nettleservinduet ditt smalere enn 800px (evt bredere enn 800px, alt ettersom).
Man kan fullstendig brekke om layout med et lite media query:

```css
@media screen and (max-width: 800px) {
  .grid-container7 {
    display: grid;
    grid-template-columns: 1fr 1fr;
    grid-template-areas: "child3 child4"
                         "child3 child1"
                         "child2 child1"
                         "child6 child5";
  }
}
```

Det er verdt å nevne at `grid-template-areas` er en veldig programmerbar
layout-mekanisme, og kan lett brukes til å programmatisk styre layouten i appene
dine. Eksempler på dette kommer i en senere bloggpost.

## Hva med Internet Explorer?

Det er ikke uten en viss mengde frustrasjon at vi i 2019 fortsatt må stille oss
dette spørsmålet, men vi lever i en vanskelig tid. En tid der det stadig finnes
folk med IE11 på internett. IE11 (og Edge, opp til versjon 15) implementerer et
tidlig utkast av CSS grids, og med dette kan du [få enkelte ting til å
fungere](https://docs.microsoft.com/en-us/previous-versions/windows/internet-explorer/ie-developer/dev-guides/hh673533\(v=vs.85\))
([Mozilla har også noen tips](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Grid_Layout/CSS_Grid_and_Progressive_Enhancement)).
Av de tingene jeg har vist frem her er for eksempel `grid-template-areas` ikke
mulig å få til i disse nettleserne.

Jeg har sluttet å ha dette problemet ved å rett og slett droppe `display:
-ms-grid`. Da får IE bare en masse blokkelementer under hverandre. Ting er
fortsatt funksjonelt, men tar større plass og har ingen elementer side-om-side.
Gitt den lave andelen brukere som har disse utdaterte nettleserne lever vi i
mitt prosjekt helt fint med denne løsningen. Andre anmodes om å se på egen
nettleserstatstikk før dere vurderer om det kan fungere for deres prosjekter.

## Lær mer

Hvis du nå er like frelst som meg vil du sikkert lære deg mer grids - det er
masse detaljer som jeg ikke har dekket her. Jeg kan varmt anbefale
[css-tricks.com sin visuelle
guide](https://css-tricks.com/snippets/css/complete-guide-grid/) - den dekker
det meste med visuelle eksempler. Jeg bruker den jevnlig som oppslagsverk.
