--------------------------------------------------------------------------------
:type :meta
:title JavaScript Coercion 101
:locale :nb
:published #time/ldt "2019-11-27T12:00"
:tags [:javascript]
:description

Det er lite vi utviklere elsker mer enn å peke på galskap i JavaScript, så som
`[] + 2 === "2"`, men dersom jobben din er å skrive kode i dette språket, eller
språk som kompilerer til JavaScript uten å skjule denne oppførselen (eksempelvis
TypeScript og ClojureScript) vil du spare mye tid i feilsøking og koding ved å
forstå _hvorfor_ ting er som de er.

--------------------------------------------------------------------------------
:type :section
:section-type :centered
:theme :dark1
:title JavaScript Coercion 101
:body

Det er lite vi utviklere elsker mer enn å peke på galskap i JavaScript, så som
`[] + 2 === "2"`, men dersom jobben din er å skrive kode i dette språket, eller
språk som kompilerer til JavaScript uten å skjule denne oppførselen (eksempelvis
TypeScript og ClojureScript) vil du spare mye tid i feilsøking og koding ved å
forstå _hvorfor_ ting er som de er.

--------------------------------------------------------------------------------
:type :section
:body

JavaScript er et dynamisk språk med over gjennomsnittet finurlige regler for
automatisk typekonvertering -- "coercion". Heldigvis er det færre regler enn det
kan virke som, men sammen kan de by på mang en overraskelse for enhver. Kan du
reglene så vil du ikke bli overrasket over dette kodeeksempelet som noen nylig
viste meg:

```js
-1 < null // true
null < 1 // true
0 == null // false (exploding head)
```

Altså: fordi `null` er større enn -1, men mindre enn 1, forventet man at den i
det minste skulle være lik `0`, noe den ikke er. Les videre for å lære hvorfor.

--------------------------------------------------------------------------------
:type :section
:title Konverteringer
:theme :light1
:body

La oss starte med de reglene for konvertering som finnes, så kan vi se på bruken
av dem etterpå.

### Primitiver

For å konvertere en primitiv til en streng, altså `String(val)`:

- Tall blir tallene som strenger, altså `"32"`
- Booleans blir `"true"` eller `"false"`
- `null` og `undefined` blir henholdsvis `"null"` og `"undefined"`

For å konvertere en primitiv til et tall, `Number(val)`:

- Strenger leses som tall, `"23"` blir `23`, men strenger som inneholder
  ugyldige tegn blir `NaN`.
- `true` blir `1`, `false` blir `0`
- `null` blir `0`
- `undefined` blir `NaN`

For å konvertere en primitiv til en boolean, `Boolean(val)`:

- `0`, `null`, `undefined`, `""`, `NaN` (og `false`) blir `false` (disse
  verdiene er såkalte "falsy values").
- Alt annet blir `true` ("truthy values").

### Objekter

Ikke-primitive verdier konverteres først til en streng eller et tall, deretter
videre til feks en boolean, dersom det er nødvendig.

Et objekt konverteres til en primitiv via:

- `.valueOf`, hvis objektet har den, og den returnerer et tall
- `.toString`, hvis objektet har den, og den returnerer en streng

I praksis er det sjelden man implementerer disse direkte, men alle
JavaScript-objekter arver fra `Object.prototype`, som definerer begge:

- `valueOf` returnerer `this`, altså `obj === obj.valueOf()`
- `toString` returnerer stort sett `"[object Object]"`

Fordi den innebygde `valueOf` ikke returnerer et tall så er det stort sett
`toString` som benyttes for primitiv-konvertering, med mindre man aktivt har
jobbet for noe annet. Et unntak verdt å merke seg er `Date`, som returnerer
timestamp fra `valueOf`:

```js
var now = new Date();
now.valueOf() === now.getTime();
```

--------------------------------------------------------------------------------
:type :section
:title A wild coercion appears
:body

Nå som du har peiling på hvilke regler som styrer konvertering kan vi se litt på
hvor disse reglene tas i bruk.

### < og >

Disse to operatorene gir kun mening for tall, og konverter sine argumenter til
tall:

```js
3 < "4" //=> 3 < Number("4")
        //=> 3 < 4
        //=> true

null < 1 //=> Number(null) < 1
         //=> 0 < 1
         //=> true

-1 < null //=> -1 < Number(null)
          //=> -1 < 0
          //=> true
```

Med det eksempelet har vi fått forklart 2/3 av det første kodeeksempelet vårt.

Legg merke til at dette kan bli nokså klovnete med objekter:

```js
var clown = {
  age: 32,

  valueOf() {
    return this.age
  }
};

clown < 33 //=> ToPrimitive(clown) < 33
           //=> 32 < 33
           //=> true
```

Eller med `toString`:

```js
var clown = {
  toString() {
    return "56";
  }
};

clown < 57 //=> ToPrimitive(clown) < 57
           //=> Number("56") < 57
           //=> 56 < 57
           //=> true
```

### +

`+`-operatoren i JavaScript er, som så mye annet, litt mer bingo enn i andre
språk. Som i andre språk gjør den enten streng-konkatenering eller addisjon, men
i motsetning til andre språk så blander den gjerne disse to i ett og samme
uttrykk.

Har du en streng eller et objekt på en av sidene får du konkatenering -- ellers
får du addisjon. For konkatenering må begge argumentene konverteres til
strenger, for addisjon må begge konverteres til tall. Har du flere plusser i
samme uttrykk følger du denne regelen én pluss om gangen.

Noen eksempler med tall:

```js
2 + 3 //=> 5

2 + true //=> 2 + Number(true)
         //=> 2 + 1
         //=> 3

true + 4 //=> Number(true) + 4
         //=> 1 + 4
         //=> 5
```

Fordi du kun får konkatenering når du har et objekt med i dansen, kan du få et
uventet resultat dersom du prøver å addere et tall med et objekt som helt fint
kan konverteres til et tall:

```js
var then = new Date(2019, 0, 1);

then + 1000 //=> "Tue Jan 01 2019 00:00:00 GMT+0100 (CET)1000"
```

Hva skjedde? Vel, siden ett argument var et objekt er det streng-konkatering som
gjelder, og dermed får vi:

```js
then.toString() + String(1000)
"Tue Jan 01 2019 00:00:00 GMT+0100 (CET)" + "1000"
```

Dersom du har et lengre uttrykk evaluerer du bare én og en pluss:

```js
2 + 3 + true + []
    //=> 5 + true + []
    //=> 5 + Number(true) + []
    //=> 6 + []
    //=> String(6) + String([])
    //=> "6" + ""
```

En array har en `toString` som fungerer som `.join(",")`:

```js
2 + [1, 2, 3] + true
    //=> String(2) + String([1, 2, 3]) + true
    //=> "2" + "1,2,3" + true
    //=> "21,2,3" + String(true)
    //=> "21,2,3true"
```


--------------------------------------------------------------------------------
:type :section
:title ==
:theme :dark1
:body

Helt til slutt har vi JavaScripts aller mest skrullete operator, nemlig `==`.
Den er såppass kompleks at man bare burde la være å bruke den. Men den var en
del av det opprinnelige eksempelet, så la oss fort se på [algoritmen bak
den](https://www.ecma-international.org/ecma-262/10.0/index.html#sec-abstract-equality-comparison):

`x == y` gir alltid enten `true` eller `false`.

1. Dersom `x` og `y` har samme type (`typeof`, ikke type på objekt) så returner
   `x === y` (primitive verdier matcher "seg selv" - bortsett fra `NaN`, objekt
   `x` og `y` er kun like dersom det er samme instans - ingen verdi-likhet for
   objekter).
2. Hvis `x` og `y` begge er `null` eller `undefined`, returner `true`
3. Hvis ett argument er et tall og det andre en streng, konverter strengen til
   et tall og start på nytt
4. Hvis én av argumentene er en boolean, konverter den til et tall og start på
   nytt (!!!)
5. Hvis én av argumentene er et objekt, konverter til en primitiv og start på
   nytt
6. Returner `false`

Som sagt, dette er ikke et verktøy det er verdt å allokere hjernekapasitet til å
håndtere -- bruk `===`. Men, til vårt opprinnelige eksempel: hvordan kan `-1 <
null` og `null < 1` når `0 != null`? De to første ble forklart over, men la oss
bryte ned den siste:

1. `0` og `null` har ikke samme type (`"number"` vs `"object"`)
2. Ett argument er `null` eller `undefined`, men ikke det andre
3. Ingen av argumentene er en streng
4. Ingen av argumentene er en boolean
5. `null` er ikke et objekt
6. Returner `false`

Og der har du forklaringen.
