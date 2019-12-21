--------------------------------------------------------------------------------
:type :meta
:title Devops! Dev? Ops!
:locale :nb
:published #time/ldt "2019-12-17T12:00"
:tags [:aws]
:description

Det skorter for tiden ikke på bedrifter i IT-sfæren som skryter av at de driver
med Devops. Men gjør de egentlig det? Hvis løsningen er et "devops-team" eller
stillingsutlysninger for "en devopser", er sjansen dessverre liten for at vi
henter ut det beste devops har å by på.

--------------------------------------------------------------------------------
:type :section
:section-type :centered
:theme :dark1
:title Devops! Dev? Ops!
:body

Det skorter for tiden ikke på bedrifter i IT-sfæren som skryter av at de driver
med Devops. Men gjør de egentlig det? Hvis løsningen er et "devops-team" eller
stillingsutlysninger for "en devopser", er sjansen dessverre liten for at vi
henter ut det beste devops har å by på.

--------------------------------------------------------------------------------
:type :section
:body

La oss starte med en tilnærming til en definisjon: Hva er egentlig devops?

1. En ops-person som kan YAML?
2. En utvikler som kan provisjonere infrastruktur med YAML?
3. En kultur?

Selv holder jeg en knapp på alternativ 3.

--------------------------------------------------------------------------------
:type :section
:title Devops i mine øyne
:theme :light1
:body

Devops-begrepet stammer såvidt meg bekjent fra
[Devopsdays](https://devopsdays.org/about), en konferanse om software-utvikling,
IT-drift, og samhandling derimellom.

<blockquote class="bq text-content">
  <div class="bq-source"><a href="https://www.kodemaker.no/devops/">Kodemakers sider om devops</a></div>
  <div class="bq-quote">
    <p>
DevOps-bevegelsen jobber for å redusere den tradisjonelle motsetningen mellom
endringsfokuset i softwareutvikling og stabilitetsfokuset i IT-drift. Om du
ønsker å øke endringstakten i softwareutvikling er det å satse på DevOps noe
du bør sette høyt på prioriteringslisten.
    </p>
    <p>
Blant kjerneverdiene i DevOps regnes kultur, automatisering, måling og
deling.
    </p>
  </div>
</blockquote>

Kjernen her er altså en kultur for samarbeid, hvor alle har bedriftens nøkkelmål
som styrepinne, i stedet for at enhver kun fokuserer på sin lille tue.

--------------------------------------------------------------------------------
:type :section
:title Devops i praksis
:body

Hvordan ser dette ut i praksis? Det kan selvfølgelig arte seg som "en utvikler
som kan provisjonere infrastruktur med YAML", som jeg noe sleivete skrev
innledningsvis. Mange bedrifter har i dag autonome team der utviklerne selv er
ansvarlige for sine egne produksjonsmiljøer, og tar eierskap til disse som en
del av økosystemet til sine applikasjoner. Dersom komptansen på teamet er bred
nok mener jeg at dette er en av de aller mest effektive måten å jobbe med
software i dag.

En annen måte å praktisere devops på er å bygge bro mellom tradisjonelle
utviklings- og driftsmiljøer. Da jeg jobbet på NRK for noen år siden ble dette
gjort på en svært vellykket måte: IT-drift gikk fra å eie drift av applikasjoner
til å tilby en platform - Mesos og Marathon i første omgang - som gjorde
utviklerne i stand til å deploye oftere og med bedre forutsigbarhet. Deretter
ble det satt driftsfolk ut i utviklingsteamene for å jobbe tett sammen med
utviklerne.

På denne måten fikk utviklerne ta mer eierskap i deploymentet av appene sine
(konfigurasjon osv), mens man fortsatt hadde høyt kvalifiserte driftspersoner
til å ta seg av nettverk, hardware, og alle de vanskelige tingene. Samtidig
utviklet man en kultur for samarbeid ved å ha driftsfolk i utviklingsteamene som
kunne bidra med feilsøking, hjelpe til med oppsett av monitorering, og fungere
som en bro mot driftsavdelingen. Kulturutvikling. Resultatet var hyppigere
leveranser, kortere tid til å rette feil, og mer effektive IT-arbeidere.

--------------------------------------------------------------------------------
:type :section
:title Devops på tverke
:theme :dark1
:body

Ettersom devops blir mer og mer populært blir det dessverre også stadig
vanligere å implementere en misforstått prosess og kalle den devops. Dette er
ikke noe nytt, vi i IT-bransjen elsker å pynte oss med nye begreper og
bevegelser uten å egentlig endre hvordan vi jobber. Hvor mange selskaper er det
ikke som smykker seg med "smidig" samtidig som deres prosesser er alt annet enn
nettopp smidige?

Mange bedrifter søker i dag etter en devops-utvikler til sitt devops-team. Når
vi koker en hel kultur ned til en stillingsbeskrivelse og/eller et separat team,
så går vi glipp av fordelene som ligger i å endre kulturen på tvers av hele
selskapet.

## Hva gjør "en devopser"?

Hva gjør så et devops-team? Jo, de bygger platform. I dag bader vi i verktøy fra
de store tech-selskapene som vi selv kan sette opp hos en skyleverandør eller på
en intern maskinpark. Disse løsningene er ofte så kompliserte at det kreves et
team med infrastruktur-interesserte mennesker å skru dem sammen. Her tror jeg vi
finner opphavet til mange devops-team: De driver ikke med devops så mye som de
driver med tradisjonell IT-drift, men med moderne verktøy. Verktøy som kanskje
er designet for å løse [betydelig vanskeligere problemer enn de selv
har](https://mobile.twitter.com/Carnage4Life/status/1205664370920833025).

![Galactic Algorithm](/images/galactic-algorithm.jpg)

Men er det noe galt i et ops-team som jobber med platform til utviklerne da?
Ikke nødvendigvis. Men hele poenget med devops er denne kulturen - alle jobber
sammen for å levere i produksjon og for å hjelpe bedriften å nå sine mål. Dersom
du har et devops-team som koser seg mer med å skrive Terraform og konfigurere
Kubernetes og Istio enn å faktisk sørge for at bedriften når sine mål så blir
det feil å kalle det "devops".

Er det galt at noen bruker mye tid på å sette opp Kubernetes og økosystemet
rundt på en måte som er tilpasset bedriftens behov da? Nei, kanskje ikke. Men
har vi virkelig bedriftens behov i minne? Husker vi på å tenke
[MVP](https://en.wikipedia.org/wiki/Minimum_viable_product)? Dette prinsippet
gjelder også for infrastruktur - og jeg mener ikke at vi skal ha usikker og
skranglete infrastruktur - den skal være "viable". Men vi skal ikke gjøre den
mer komplisert enn den trenger å være. Og vi må huske på at den skal være
hyggelig for utviklerne å bruke - det er de som er kunden til en sånn løsning.
Dersom devops-teamet har sittet på bakrommet i månedsvis og kokt opp en løsning
som utviklerne hater å deploye på, så bommer vi på mål.

--------------------------------------------------------------------------------
:type :section
:title Devops på sitt beste
:body

Jeg vil avslutte med en anbefaling. Alle som jobber med software-utvikling i
dag, enten operativt som utvikler, med drift, eller i en eller annen
forretningsfunksjon/lederstilling tett på IT: Les [The Phoenix
Project](https://www.amazon.com/Phoenix-Project-DevOps-Helping-Business/dp/0988262592).
Denne romanen eksemplifiserer billedlig og vakkert hvordan en devops-kultur kan
se ut. Selv gleder jeg meg til å lese oppfølgeren [The Unicorn
Project](https://www.amazon.com/Unicorn-Project-Developers-Disruption-Thriving-ebook/dp/B07QT9QR41)
i jula, som etter omtalen å bedømme er minst like god.

God devopsing!
