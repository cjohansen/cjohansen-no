# A Better Playlist Shuffle with Go

On the rare occasions that I hit "shuffle" in music players like Spotify, I
don't really want to completely randomize a playlist. In fact, what I want isn't
very random at all. More than anything, my intention is "please play these songs
with as much track-to-track variation as possible". Let's visualize this with an
example:

1. **Nirvana**: Smells Like Teen Spirit
2. **Nirvana**: Come as You Are
3. **Nirvana**: Heart-Shaped Box
4. **Nirvana**: Something in the Way
5. **Nirvana**: Aneurysm
6. **Pearl Jam**: Once
7. **Pearl Jam**: Daughter
8. **Leviathan**: Dawn Vibration
9. **Leviathan**: The Smoke of Their Torment
10. **Nile**: Black Seeds of Vengeance

A whopping half of the tracks are by the same band. As there are relatively few
orderings of this list where there _aren't_ two Nirvana tracks back to back, a
purely random shuffle has a good chance of playing me two Nirvana tracks in a
row. I don't want that - I want those Nirvana tracks spaced out evenly.

A good shuffle of this playlist would play Nirvana every other song, like this:

1. Nirvana
2. ?
3. Nirvana
4. ?
5. Nirvana
6. ?
7. Nirvana
8. ?
9. Nirvana
10. ?

Similarly, I would like those two **Pearl Jam** tracks spaced as evenly apart as
possible. If I was distributing them in a vacuum, I'd go for the following:

1. Pearl Jam
2. ?
3. ?
4. ?
5. ?
6. Pearl Jam
7. ?
8. ?
9. ?
10. ?

Unfortunately, both those places are already taken, so instead I'll settle for:

1. Nirvana
2. **Pearl Jam**
3. Nirvana
4. ?
5. Nirvana
6. ?
7. Nirvana
8. **Pearl Jam**
9. Nirvana
10. ?

There are two **Leviathan** tracks as well, I'd like those spaced out too:

1. Nirvana
2. Pearl Jam
3. Nirvana
4. **Leviathan**
5. Nirvana
6. ?
7. Nirvana
8. Pearl Jam
9. Nirvana
10. **Leviathan**

The final track is plotted into the only available spot:

1. Nirvana
2. Pearl Jam
3. Nirvana
4. Leviathan
5. Nirvana
6. **Nile**
7. Nirvana
8. Pearl Jam
9. Nirvana
10. Leviathan

Let's have a go at this using, ehm, [Go](https://golang.org/).

**Disclaimer:** I used this task to learn Go. Perhaps unsurprisingly, this sort
of data processing isn't really Go's strongest suit. I'll follow up with a post
solving this in a functional language at a later point.

**Disclaimer 2:** Go code should be indented with tabs. For presentational
reasons, I've used spaces in the listings here, meaning you can't expect to
copy-paste it without editing. Don't worry though, there's a Github link to
working code at the bottom.

**Disclaimer 3:** I probably should've used more pointers, but 1) I'm not a fan
of mutation and will avoid it if possible, and 2) I don't think it would make a
big difference perfomance-wise for the kinds of datasets this code will work on.

## Distributing the Artists

We'll start with a function `Distribute`. Its task is to distribute the artist
the specified number of times as evenly as possible. The passed in distribution
gives us the total amount of tracks, but it'll also inform us of which positions
have already been filled.

```go
func Distribute(distribution []string, artist string, tracks int) []string {
  stepSize := len(distribution) / tracks
  index := 0

}
```

We'll now go into an "indefinite" loop that will run until we've distributed all
the tracks. As with all things recursive and indefinite, we'll start with the
termination condition:

```go
func Distribute(distribution []string, artist string, tracks int) []string {
  stepSize := len(distribution) / tracks
  index := 0

  for {
    if tracks == 0 {
      return distribution
    }
  }
}
```

**Do not run this particular sample**. When there are no more tracks we're done,
and return the (now updated) distribution. The implied mutation is a bit gross,
but Go does not have built-in persistent data structures, so we're going with
the flow.

Next, we'll add a clause that identifies a suitable position:

```go
func Distribute(distribution []string, artist string, tracks int) []string {
  stepSize := len(distribution) / tracks
  index := 0
  remainder := stepSize

  for {
    if tracks == 0 {
      return distribution
    }

    if remainder == stepSize {
      // Place track
    }

    // Prepare next iteration
  }
}
```

With 5 tracks to distribute among 10 tracks, `stepSize` will be `2`. We cannot
simply drop the one track every 2 indexes, as they might be occupied. So we use
the `remainder` to flag when to position a track. The `remainder` is initialized
to the `stepSize` to force a track in the first available position.

```go
func Distribute(distribution []string, artist string, tracks int) []string {
  stepSize := len(distribution) / tracks
  index := 0
  remainder := stepSize

  for {
    if tracks == 0 {
      return distribution
    }

    if remainder >= stepSize {
      index = IndexOf(distribution, "", index)
      distribution[index] = artist
      remainder -= stepSize
      tracks--
    }

    // Prepare next iteration
  }
}
```

When the remainder is at least as much as the required step size, we find the
first available position from the current index. `IndexOf` is [a function I
wrote](https://github.com/cjohansen/shufflify/blob/2e07478078d69b7a1a5fddeecd6819f82133f453/spotify-service/shuffle/shuffle.go#L33).
In Go, every type has a "nil type", and for strings this is `""`. This is why we
look up the first index in the distribution that holds an empty string, starting
at the desired index.

We then put the artist into the position we found, subtract the step size from
the remainder, and reduce the number of tracks to distribute. To prepare the
next iteration, we increment `index` and `remainder`:

```go
func Distribute(distribution []string, artist string, tracks int) []string {
  stepSize := len(distribution) / tracks
  index := 0
  remainder := stepSize

  for {
    if tracks == 0 {
      return distribution
    }

    if remainder >= stepSize {
      index = IndexOf(distribution, "", index)
      distribution[index] = artist
      remainder -= stepSize
      tracks--
    }

    index++
    remainder++
  }
}
```

This will work so long as the `stepSize` is an integer. Let's replace one of the
Nirvana tracks with something else, and see what happens:

1. **Nirvana**: Smells Like Teen Spirit
2. **Nirvana**: Come as You Are
3. **Nirvana**: Heart-Shaped Box
4. **Nirvana**: Something in the Way
5. **Pearl Jam**: Once
6. **Pearl Jam**: Daughter
7. **Leviathan**: Dawn Vibration
8. **Leviathan**: The Smoke of Their Torment
9. **Nile**: Black Seeds of Vengeance
10. **Deathspell Omega**: Abscission

Distributing this yields:

```go
[]string{"Nirvana", "", "Nirvana", "", "Nirvana", "", "Nirvana", "", "", ""}
```

Oops! Not what we wanted. The problem is that we defined `stepSize` as an int,
but it really should be `10/4 == 2.5` in this case. Two type conversions, and
we're all good:

```go
func Distribute(distribution []string, artist string, tracks int) []string {
  stepSize := float64(len(distribution)) / float64(tracks)
  index := 0
  remainder := stepSize

  for {
    if tracks == 0 {
      return distribution
    }

    if remainder >= stepSize {
      index = IndexOf(distribution, "", index)
      distribution[index] = artist
      remainder -= stepSize
      tracks--
    }

    index++
    remainder++
  }
}
```

Which yields:

```go
[]string{"Nirvana", "", "", "Nirvana", "", "Nirvana", "", "", "Nirvana", ""}
```

Voila!

We can call the function twice to distribute two artists:

```go
distribution := DistributeSimple(make([]string, 10), "Nirvana", 4)
distribution = DistributeSimple(distribution, "Pearl Jam", 2)
//=>
[]string{
  "Nirvana",
  "Pearl Jam",
  "",
  "Nirvana",
  "",
  "Nirvana",
  "Pearl Jam",
  "",
  "Nirvana",
  "",
}
```

Looks good. However, note that the order of the calls matter:

```go
distribution := DistributeSimple(make([]string, 10), "Pearl Jam", 2)
distribution = DistributeSimple(distribution, "Leviathan", 2)
distribution = DistributeSimple(distribution, "Nirvana", 4)
//=>
[]string{
  "Pearl Jam",
  "Leviathan",
  "Nirvana",
  "Nirvana",
  "",
  "Pearl Jam",
  "Leviathan",
  "Nirvana",
  "",
  "Nirvana",
}
```

If we distribute the artists with fewer tracks first, there might not be room
left to properly spread the ones with more tracks. We'll create another function
to distribute a whole list taking this into account.

## Distributing all Tracks by Artist

Next up is a function that takes a playlist and "shuffles" it by evenly
distributing tracks from the same artist. This is how we'll do it:

1. Group all the tracks by artist
2. Sort the groups descending on tracks per artist
3. Loop through the sorted grouping and create the distribution
4. Loop through the distribution and replace artist name with actual track

The playlist will be a slice of this data type:

```go
type Item interface {
  GroupingKey(string) string
}
```

That is, it'll work for anything that implements the `GroupingKey` function that
takes a string, like `"artist"`, and returns a grouping key for the specific
track, like `"Nirvana"`.

### 1. Group Tracks by Artist

Because Go is a low-level language, and because it does not have generics, the
language does not provide niceties like "group by" out of the box. Instead we'll
have to write our own for the desired data type. I wrote one that outputs the
following type:

```go
type GroupedItems map[string][]Item
```

In other words, a map of string keys to slices of `Item`s. This type declaration
is there mostly to improve readability of the consuming code. The `GroupBy`
function takes a slice of `Item`s and a function (`Item -> string`) and returns
a `GroupedItems`:

```go
func GroupBy(items []Item, fn func(Item) string) GroupedItems {
  res := make(GroupedItems)

  for _, item := range items {
    key := fn(item)

    if res[key] == nil {
      res[key] = make([]Item, 1)
      res[key][0] = item
    } else {
      res[key] = append(res[key], item)
    }
  }

  return res
}
```

Note that this function already introduces some light randomness into our
algorithm - map access is random in Go. We'll sort this later, but artists with
the same number of tracks will come out at random positions because of how map
access works.

### 2. Sort Groups Descending on Tracks per Artist

Next up, we need to sort the entries in the map by the length of the `[]Item`
slice. Boy, oh, boy, sorting is no easy task in Go. No generics, remember? We
have to implement an interface in order to sort a collection. Because Go's
`sort.Sort` function is _very_ generic, we not only have to implement the
comparison of two elements, we also have to tell Go how to get the length of the
collection *and* how to swap two items:

```go
type Bucket struct {
  label       string
  occurrences int
}

type byOccurrences []Bucket

func (s byOccurrences) Len() int {
  return len(s)
}

func (s byOccurrences) Swap(i, j int) {
  s[i], s[j] = s[j], s[i]
}

func (s byOccurrences) Less(i, j int) bool {
  return s[i].occurrences > s[j].occurrences
}

```

Now we'll loop through our grouped map, turn it into a slice of `Bucket`s and
sort it:

```go
import (
  "sort"
)

func bucketsByOccurrences(grouped GroupedItems) []Bucket {
  res := make([]Bucket, len(grouped))
  i := 0

  for bucket, bucketItems := range grouped {
    res[i] = Bucket{bucket, len(bucketItems)}
    i++
  }

  sort.Sort(byOccurrences(res))
  return res
}
```

If you're wondering why I'm mixing `UpperCamelCase` and `lowerCamelCase`, in Go,
any `UpperCamelCase` name is public, while `lowerCamelCase` names are private.

### 3. Create the Distribution

We now have almost all the building blocks to create the distribution:

```go
func DistributeByArtist(items []Item) []Item {
  grouped := GroupBy(items, func (track Item) {
    return item.GroupingKey("artist")
  })

  buckets := bucketsByOccurrences(grouped)
  distribution := make([]string, len(items))

  for _, bucket := range buckets {
    Distribute(distribution, bucket.label, bucket.occurrences)
  }

  return ??
}
```

### 4. Create the Final Playlist

But what to return? Currently we have this:

```go
distribution //=>
[]string{
  "Nirvana",
  "Pearl Jam",
  "Leviathan",
  "Nirvana",
  "Nile",
  "Nirvana",
  "Pearl Jam",
  "Leviathan",
  "Nirvana",
  "Deathspell Omega",
}

grouped //=>
GroupedItems{
  "Nirvana": []Item{
     {"Artist": "Nirvana", "Track": "Smells Like Teen Spirit"},
     {"Artist": "Nirvana", "Track": "Come As You Are"},
     {"Artist": "Nirvana", "Track": "Heart-Shaped Box"},
     {"Artist": "Nirvana", "Track": "Something in the Way"},
     {"Artist": "Pearl Jam", "Track": "Once"},
     {"Artist": "Pearl Jam", "Track": "Daughter"},
     {"Artist": "Leviathan", "Track": "Dawn Vibration"},
     {"Artist": "Leviathan", "Track": "The Smoke of Torment"},
     {"Artist": "Nile", "Track": "Black Seeds of Vengeance"},
     {"Artist": "Deathspell Omega", "Track": "Abscission"},
   }
}
```

The final step is to loop over the `distribution`, replacing the first
occurrence of `"Nirvana"` with the first Nirvana track, the second occurrence
with the second track, and so on. We'll call this `ReifyDistribution`:

```go
func DistributeByArtist(items []Track) []Track {
  grouped := GroupBy(items, func (track Track))
  buckets := bucketsByOccurrences(grouped)
  distribution := make([]string, len(items))

  for _, bucket := range buckets {
    Distribute(distribution, bucket[0], len(bucket[1]))
  }

  return ReifyDistribution(distribution, grouped)
}
```

Which can be implemented as such:

```go
func ReifyDistribution(distribution []string, groups GroupedItems) []Item {
  items := make([]Item, len(distribution))

  for i, key := range distribution {
    items[i], groups[key] = groups[key][0], groups[key][1:]
  }

  return items
}
```

## Random Much?

This is all fine and dandy, but a "shuffle" should have _some_ randomness to it,
right? Yes. We can introduce two levels of randomness:

1. Randomize the initial playlist
2. Randomize the starting point

### 1. Randomize the Initial Playlist

Randomizing the initial playlist serves to randomize the order the tracks by
each artist are played.

Go doesn't provide a ready to use "randomize collection" function, but it does
offer `rand.Perm(int)`, which creates a random permutation of the specified
length. We can build on this to randomize the collection:

```go
import (
  "rand"
)

func randomize(src []Item) []Item {
  dest := make([]Item, len(src))
  perm := rand.Perm(len(src))

  for i, v := range perm {
    dest[v] = src[i]
  }

  return dest
}
```

We'll call this from our new, random wrapper around `DistributeByArtist`:

```go
func ShuffleByArtist(items []Item) []Item {
  return DistributeByArtist(shuffle(items))
}
```

### 2. Randomize the Starting Point

We can view the resulting playlist as a circle, and then randomly select a
starting point. This will help ensure that every permutation of our playlist
does not start with Nirvana, at the cost of _one potentially_ weakly distributed
section (original end -> start). That seems like a small price to pay for a good
amount of perceived randomness:

```go
func startRandomly(items []Item) []Item {
  index := rand.Intn(len(items))
  return append(items[index:], items[:index]...)
}

func ShuffleByArtist(items []Item) []Item {
  return startRandomly(DistributeByArtist(randomize(items)))
}
```

And there you have it - a playlist shuffler that respects the listeners desire
for a random, but balanced listening experience.

## Parting Thoughts

While we've come some way from `randomize(playlist)`, our implementation still
has weakenesses. Let's consider an example output from our shuffler:

1. **Deathspell Omega**: Abscission
2. **Nirvana**: Something in the Way
3. **Pearl Jam**: Once
4. **Leviathan**: Dawn Vibration
5. **Nirvana**: Smells Like Teen Spirit
6. **Nile**: Black Seeds of Vengeance
7. **Nirvana**: Come as You Are
8. **Pearl Jam**: Daughter
9. **Leviathan**: The Smoke of Their Torment
10. **Nirvana**: Heart-Shaped Box

Looks good enough. However, let's see the album names:

1. Deathspell Omega: Abscission - **Paracletus**
2. Nirvana: Something in the Way - **Nevermind**
3. Pearl Jam: Once - **Ten**
4. Leviathan: Dawn Vibration - **Scar Sighted**
5. Nirvana: Smells Like Teen Spirit - **Nevermind**
6. Nile: Black Seeds of Vengeance - **Black Seeds of Vengeance**
7. Nirvana: Come as You Are - **Nevermind**
8. Pearl Jam: Daughter - **VS**
9. Leviathan: The Smoke of Their Torment - **Scar Sighted**
10. Nirvana: Heart-Shaped Box - **In Utero**

Look at those Nirvana tracks: `Nevermind, Nevermind, Nevermind, In Utero`.
Terrible! Even worse, take a look at those genres:

1. Deathspell Omega: Abscission - Paracletus / **Black Metal**
2. Nirvana: Something in the Way - Nevermind / **Grunge**
3. Pearl Jam: Once - Ten / **Grunge**
4. Leviathan: Dawn Vibration - Scar /Sighted, **Black Metal**
5. Nirvana: Smells Like Teen Spirit - Nevermind / **Grunge**
6. Nile: Black Seeds of Vengeance - Black /Seeds of Vengeance, **Death Metal**
7. Nirvana: Heart-Shaped Box - In /Utero, **Grunge**
8. Pearl Jam: Daughter - VS / **Grunge**
9. Leviathan: The Smoke of Their Torment - Scar /Sighted, **Black Metal**
10. Nirvana: Come as You Are - Nevermind / **Grunge**

Very poor distribution indeed. What our shuffler needs to do is to operate along
multiple grouping keys. Ideally, we'd distribute by genre, then artist, *then*
album. Had we done that, we migth have ended up with something like this:

1. **Nirvana**: Come as You Are - Nevermind / _Grunge_
2. **Leviathan**: The Smoke of Their Torment - Scar /Sighted, _Black Metal_
3. **Pearl Jam**: Daughter - VS / _Grunge_
4. **Nile**: Black Seeds of Vengeance - Black /Seeds of Vengeance, _Death Metal_
5. **Nirvana**: Heart-Shaped Box - In /Utero, _Grunge_
6. **Deathspell Omega**: Abscission - Paracletus / _Black Metal_
7. **Nirvana**: Something in the Way - Nevermind / _Grunge_
8. **Pearl Jam**: Once - Ten / _Grunge_
9. **Leviathan**: Dawn Vibration - Scar /Sighted, _Black Metal_
10. **Nirvana**: Smells Like Teen Spirit - Nevermind / _Grunge_

To achieve this, `DistributeByArtist` needs to become `DistributeBy` and take an
additional parameter: a list of functions to group the playlist by. Imagine this call:

```go
DistributeBy(playlist, []func(Item) string{
  func(c Item) string {
    return c.GroupingKey("genre")
  },
  func(c Item) string {
    return c.GroupingKey("artist")
  },
})
```

It would first create a distribution of genres. Before reifying the distribution
with the tracks in that genre, it would recursively distribute all the songs in
each genre by artist. If you're curious how the code needs to change to support
this, check out the [Go shuffle code on Github](https://github.com/cjohansen/shufflify/blob/2e07478078d69b7a1a5fddeecd6819f82133f453/spotify-service/shuffle/shuffle.go).
