--------------------------------------------------------------------------------
:type :meta
:title Når AWS-regninga blir skyhøy
:locale :nb
:published #time/ldt "2019-09-25T12:00"
:tags [:aws]
:description

Det er fort gjort å komme opp og gå i skyen, men før du får sukk for deg har du
en løsning der du nærmest skuffer penger i retning Amazon. Hva gjør du den dagen
du oppdager at AWS-regninga er **mye** høyere enn du hadde forventet?

--------------------------------------------------------------------------------
:type :section
:section-type :centered
:theme :dark1
:title Når AWS-regninga blir skyhøy
:body

I disse dager er det mange som flytter systemene sine til skyen. Lovnadene er
mange: drift kommer til å gå nærmest av seg selv, systemene blir umiddelbart
"web scale", og takket være "pay as you go"-modellen skal kostnadene ned -
drastisk. I praksis er ingen av disse tingene som er sanne, med mindre du jobber
for at de skal bli det. Allikevel tror jeg det som biter flest prosjekter er at
når ting endelig har kommet opp og stå så viser det seg at Amazon (eller Google,
Microsoft osv) skal ha obskønt mye penger for tjenestene sine. Så hva skal du
gjøre når du oppdager at AWS-regninga plutselig har blitt skyhøy?

--------------------------------------------------------------------------------
:type :section
:title Send regninga til rett sted
:body

AWS-billing går ofte gjennom to faser: idet utviklerne starter å prøve seg frem
er det gjerne en av dem som legger inn sitt private kredittkort, og fører utlegg
for utgiften. Når prosjektet går over i en mer etablert fase endres billing slik
at det går direkte til en leder, eller en egen avdeling, dersom man jobber i et
stort nok selskap. Sluttresultatet blir ofte at de som betaler regninga ikke er
de samme som utvikler tjenestene, eller som har oversikt over hvilke tjenester
regninga egentlig dekker.

Hvor stor AWS-regning er for stor? $1000? $10.000? $100.000? Det kommer an på
hva du får igjen for pengene. Men hvilke forutsetninger har en
økonomi-medarbeider for å vurdere dette? Her ligger kilden til en ganske seig
feedback-loop: Utviklerne spinner opp servere og tjenester mens regninga blir
større og større, og ikke før det begynner å bli seriøse beløp går alarmen en
helt annen plass i bedriften.

Sørg for at regninga går via noen som er tett på det aktuelle prosjektet, og som
er i stand til å vurdere hvorvidt kostnadene kan forsvares ut fra hva som
leveres.

--------------------------------------------------------------------------------
:type :section
:title Ansvarliggjør utviklerne
:theme :light1
:body

Hvem vet vel bedre hva som leveres enn de som faktisk **leverer**? Gi utviklerne
tilgang til billing, og inkluder rapporten i passende jevnlige ritualer, så som
retro. En prosjektleder vil kanskje ikke rynke på nesa over en månedlig regning
på noen tusen blankpolerte dollars, mens en utvikler som vet at det som er
levert så langt like gjerne kunne kjørt på en VPS til $50 kommer til å få tics
av de samme tallene.

På den andre siden burde utviklerne ha et ativt forhold til kostnader når de
designer og utformer løsninger. I en diskusjon om hvorvidt noe skal løses med én
(eller flere) [EC2](https://aws.amazon.com/ec2/)-instanser eller en
[DynamoDB](https://aws.amazon.com/dynamodb/)-tabell, en
[Lambda](https://aws.amazon.com/lambda/) og
[ApiGateway](https://aws.amazon.com/api-gateway/) kan det godt være pris **for
det aktuelle volumet** som blir tungen på vektskålen.

--------------------------------------------------------------------------------
:type :section
:title Følg med!
:body

Tett knyttet til det forrige punktet. For mange kommer høye regninger til
skytjenester som en overraskelse. Det kan det bare bli dersom ingen følger med
før det plutselig har gått for langt. Dersom alle i prosjektet en gang i måneden
kikker litt på billing så vil man fort legge merke til om kostnaden stiger i et
høyere tempo enn verdien som leveres på tjenestene som lages. En gjennomgang av
billingrapporten kan også gi noen åpenbare tiltak for innsparinger.

Jeg har fått gjenfortalt **flere** historier om skyhøye skyregninger som skyldes
at man har glemt å skru av ting som ble spunnet opp "bare for å prøve". Dette
skjer ikke dersom noen med nok kjennskap til prosjektet faktisk følger med på
regninga. Så sørg for å gå gjennom billing-rapporten med teamet månedlig.

--------------------------------------------------------------------------------
:type :section
:title Hva er dyrt?
:theme :dark1
:body

Før man begynner å gå gjennom AWS-regninga og få hjertebank av beløpene er det
greit å ha en formening om hva drift av din tjeneste **burde** koste. Hva kan den
koste? Når blir det **for** dyrt? Mange prosjekter har absolutt ingen svar på
disse spørsmålene, og trenger dermed at regninga blir meget høy før det føles
riktig å reagere.

Dersom du flytter tjenestene dine fra onsite hosting til skyen så har du et
slags mål. Jeg antar at du flytter tjenestene for å få nye muligheter og for å
oppnå en grad av frihet og fleksibilitet som er vanskelig å gjenskape lokalt,
men kostnader bør også være en del av bildet. Det er rimelig å forvente at
driften ikke blir mye dyrere i skyen. Snarere tvert i mot.

Dersom du bygger nytt er det vanskeligere å vurdere hva som er et riktig
prisnivå. Men husk at det ikke er din private lommebok som står for regninga.

En nyttig øvelse som setter prisene i perspektiv er å omsette innsparelser i
drift til utgifter til arbeidskraft, eller tid tapt for time to market. Å bruke
15.000 kroner på at en konsulent reduserer AWS-regninga di med $50 i måneden er
antageligvis ikke spesielt god butikk - det går ikke i null før om tre år.

Driftskostnadene kan også lett vurderes opp mot inntektene fra systemene som
driftes. Dette regnestykket bør helst gå i pluss for bedriften.

--------------------------------------------------------------------------------
:type :section
:title Gratis blir plutselig dyrt
:body

Mange av tjenestene til Amazon har en oppstartsmodus som er kunstig billig,
eller til og med gratis. Jeg har tidligere fortalt
[en historie om når gratis plutselig ble et mareritt](/aws-free-tier/).

--------------------------------------------------------------------------------
:type :section
:title Bruk flere kontoer
:theme :light1
:body

Skal du vurdere rimeligheten av en regning er det alfa-omega å forstå **hva** du
betaler for. Jeg vil på det varmeste anbefale at man er rundhåndet med å
opprette nye kontoer for å skille tjenester som ikke har noe med hverandre å
gjøre. En konto koster ingenting, og de kan samles under en organization
account, hvor du også kan få såkalt [consolidated billing](https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/consolidated-billing.html),
altså én samlet regning for flere konti. Ved å knytte kontoene sammen i en
organisasjon kan du også bruke IAM for tilgangsstyring på tvers, med mer. Det er
virkelig en no-brainer. Consolidated billing gir deg én regning, men også
muligheten til å analysere kostnader per konto, og fordele volum-rabatter på
tvers av kontoene.

Hva skal man lage egne konti for? Som et minimum vil jeg anbefale å bruke en
dedikert konto for produksjonsmiljøet og dedikerte konti for eventuelle
utviklings- og staging/test-miljøer. Dette gjør deg i stand til å se kostnaden
for de individuelle miljøene på aller enklest måte, i tillegg til at det gir deg
et bedre utgangspunkt for å lage en sikker løsning med god tilgangskontroll -
men det er en annen bloggpost.

Dersom du er en stor bedrift og har flere avdelinger/produkter som alle er på
AWS bør hver av disse ha egne konti for hver av sine miljøer. Alt som ikke
trenger noen særlig grad av samhandling bør skilles. Feil heller med for mange
kontoer enn for få, du kan alltids slå ting sammen senere. Å splitte ting etter
at de har blitt litt for intime er ofte mye vanskeligere.

--------------------------------------------------------------------------------
:type :section
:title Tags, tags, tags
:body

Tagging er det tradisjonelle rådet for å vite hva ting blir brukt til, ettersom
man kan bryte billing ned på tags. Adskilte kontoer gir den aller beste
separasjonen - ikke alt kan tagges, og tagging krever mer aktiv innsats enn å
opprette ressurser i riktig konto. Men det ene utelukker ikke det andre. Tagg
alt som tagges kan. Dersom du har mikrotjenester, bruk navnene deres til å tagge
alle relevante ressurser, og bruk andre relevante klassifiseringer som det
passer. Taggene hjelper deg ytterligere å forstå hvor pengene går.

--------------------------------------------------------------------------------
:type :section
:title Automatiser alt
:theme :dark1
:body

Automasjon er relevant for billing på den måten at det gir deg enda litt bedre
sporbarhet. Som jeg nevnte tidligere har jeg snakket med mange som har opplevd å
betale i dyre dommer for ting de glemte å skru av. Det er en situasjon det er
mye lettere å sette seg i med manuelt provisjonerte ressurser.

Automasjon gjør det også lettere å sørge for at ressurser tagges så langt det er
praktisk mulig. Automasjon kan sågar håndheve internt formulerte regler for
hvordan ting skal tagges. Automasjon har selvfølgelig en kostnad i seg selv, og
"alt" er nok ofte ikke en praktisk oppnåelig løsning - men siden automasjon også
kan hjelpe med så mye annet vil jeg på det varmeste anbefale å være på ballen
fra tidlig i prosjektet av her.

På den annen side kan for hønivå automatisering også abstrahere bort/skjule
kostnader. Det er ikke lett å treffe blink.

--------------------------------------------------------------------------------
:type :section
:title Ta ned test-miljøet
:body

Produksjonsmiljøet må alltid være tilgjengelig. Men det gjelder med stor
sannsynlighet ikke de andre miljøene. Dersom du har et fullt duplisert dev-miljø
så er det relativt stor sjans for at dette ikke trenger å kjøre på kvelden og
natta, eller på helga.

Dersom dev-miljøet ditt kun "står på" fra 07:00 til 18:00 mandag til fredag så
kjører det bare 55 av ukas 128 timer - 42% av tiden. Nå er det ikke alt som
betales etter klokka, men hvis du har mange ting som går på takstameter så kan
det fort være verdt å investere litt tid i å lage en løsning for å ta ting opp
og ned rundt arbeidstid. Bare husk på å lage en mulighet for å manuelt overstyre
for de dagene folk velger seg litt andre arbeidstider. Og glem for all del ikke
å vurdere prisen på innsatsen mot besparelser i driftskostnaden - ikke bruk
100.000 kroner på å spare $100 i måneden.

--------------------------------------------------------------------------------
:type :section
:title Betal for det du bruker
:theme :light1
:body

Skyen sies å være flott blant annet fordi du systemene dine skalerer opp og ned
ved behov slik at du kun betaler for akkurat det du bruker. Det er helt klart
mulig å få til, men **det er ikke sånn ting virker ut av boksen**. Selvom Amazon
gir deg verktøyene til det, kreves det en hel del innsats å lage et oppsett som
faktisk autoskalerer på denne måten. I praksis er det lett å ende opp med å leie
(mye) mer maskinkraft enn man egentlig bruker.

Steg 1 i å begrense overbetaling av denne typen er nok en gang å følge med. Hold
et øye med utnyttelsesgraden av EC2-instanser, RDS-instanser og alt annet som i
bunn og grunn er en EC2-instans. Bytt ut underutnyttede instanser med mindre
kraftige og billigere typer. Har du mange instanser som alle har lav utnyttelse
kan du vurdere å samle tjenester på samme instans. Dette er særlig aktuelt for
managed services som er bygget oppå EC2-instanser. Det blir fort kostbart å ha
mange individuelle RDS-instanser når du egentlig kunne klart deg med én
RDS-instans med flere databaser på. Hvorvidt dette er gjennomført kommer
selvfølgelig an på i hvilken grad enkelt-databaser har trafikk-spikes, hva slags
krav du har til isolasjon osv, men det er ihvertfall noe å vurdere.

Et litt mer drastisk steg er å se på dine "alltid på"-tjenester (igjen, alt som
kjører på en EC2-instans) og vurdere om en av AWS' managed tjenester (aka
"serverless") kan passe bedre. Dersom du har API-er med lite eller ujevnt
fordelt trafikk kan det godt hende at en løsning med feks Lambda kan gi deg en
langt hyggeligere regning. BEKK klarte for eksempel å gjøre [alle landets skatteberegninger for under $2](https://blogg.bekk.no/jakten-på-fem-tusen-skatteberegninger-i-sekundet-33a70da788)
med smart bruk av Amazons tjenester.

--------------------------------------------------------------------------------
:type :section
:title Bruk spot-maskiner
:body

Dersom du har mange EC2-instanser har du to tiltak som kan gjøre gode innhogg i
regninga. Det første er å bytte ut
[on-demand-maskiner](https://aws.amazon.com/ec2/pricing/on-demand/) med
[spot-maskiner](https://aws.amazon.com/ec2/spot/). Vær obs på at dette er et
skifte du må sko deg for. En spot-maskin er kort sagt maskinkraft i Amazons
nettverk som er til overs og som selges til høystbydende på en automatisert
auksjon. For å benytte en spot-maskin setter du en makspris, feks
on-demand-prisen for samme type instans, og så får du en maskin til gjeldende
spot-pris, som typisk er 60%+ lavere. Ulempen er at Amazon kan ta den tilbake
med 2 minutters varsel. Du må med andre ord sørge for at du kun bruker disse
maskinene i en setting hvor du automatisk kommer deg på beina igjen når en
maskin forsvinner.

Spot-maskiner er i prinsippet ideellt for cluster-løsninger, så som Kubernetes
og ECS, hvor man typisk har en agent som fyller tomrommet etter én maskin med en
annen. Dersom du velger mer enn én type maskin og flere availability zones skal
det mye til å ende opp helt uten maskiner. Man kan sågar falle tilbake på on
demand-instanser når spot ikke er å oppdrive. Jeg skal ikke si at det er hverken
trivielt eller problemfritt å gå denne veien, men det kan spare deg for mye
penger.

--------------------------------------------------------------------------------
:type :section
:title Reserver instanser
:theme :light1
:body

Dersom spot-instanser blir litt for mye spenning for deg kan du heller velge
[reserverte instanser](https://aws.amazon.com/ec2/pricing/reserved-instances/).
De fungerer akkurat som vanlige on demand-instanser, bare at du committer deg
til å bruke - og betale for - dem en periode. Committer du deg til 1 år sparer
du 40%, committer du deg for 3 år sparer du 60%.

--------------------------------------------------------------------------------
:type :section
:title Velg riktig verktøy for jobben
:body

Vi bombarderes av arkitektur-forslag fra det store internett - bloggposter og
foredrag, ofte skrevet av utviklere for store internasjonale selskaper,
forteller oss om hvorfor vi burde bruke Kafka, Kubernetes, og andre verktøy. Før
vi hiver oss på er det lurt å vurdere om vi har mange nok av de samme problemene
som disse menneskene. [Amazon sin hosted Kafka-løsning](https://aws.amazon.com/msk/) for eksempel, starter på rundt $600
i måneden. Dersom du ikke får mer trafikk enn at du like gjerne kunne putta
disse eventene i en database så er dette en spurvkanon av rang.

Jeg nevnte tidligere et eksempel på at å gå fra "alltid på" EC2-instanser til
serverless-teknologier kan spare deg for penger. I noen situasjoner er det
motsatte som skal til. Det kommer an på bruksmønsteret ditt. For eksempel så kan
et serverless API med høyt trykk fort
[bruke alt for mye penger på API Gateway](https://serverless-training.com/articles/save-money-by-replacing-api-gateway-with-application-load-balancer/).

--------------------------------------------------------------------------------
:type :section
:title Inn i detaljene
:theme :dark1
:body

I dette innlegget har jeg forsøkt å belyse noen litt overordnede tiltak som kan
hjelpe deg å få innsikt i hva pengene går til, samt noen relativt enkle tiltak
som kan hjelpe deg å spare penger. Det er masse mer å hente i å gå inn i hver
enkelt tjeneste og se på detaljene. Det finnes masse stoff om individuelle
tjenester der ute, og jeg kommer nok også til å skrive mer om saken.
