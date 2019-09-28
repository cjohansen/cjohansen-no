--------------------------------------------------------------------------------
:type :meta
:title An introduction to Emacs Lisp
:published #time/ldt "2012-07-01T12:00"
:updated #time/ldt "2017-07-17T12:00"
:tags [:elisp :emacs]
:description

A long, thorough introduction to programming in Lisp in general, and Emacs Lisp
in particular
--------------------------------------------------------------------------------
:type :section
:section-type :centered
:theme :dark1
:title An introduction to Emacs Lisp

:body

As a long-time passionate Emacs user, I've been curious about Lisp in general
and Emacs Lisp in particular for quite some time. Until recently I had not
written any Lisp apart from [my .emacs.d setup](http://github.com/cjohansen/.emacs.d), despite
having read both
[An introduction to programming in Emacs Lisp](http://www.amazon.com/Introduction-Programming-Emacs-Lisp/dp/1882114566/ref=sr_1_1?s=books&ie=UTF8&qid=1311627046&sr=1-1) and
[The Little Schemer](http://www.amazon.com/Little-Schemer-Daniel-P-Friedman/dp/0262560992/ref=sr_1_1?s=books&ie=UTF8&qid=1311627085&sr=1-1) last
summer. A year later, I have finally written some Lisp, and I thought I'd share
the code as an introduction to others out there curious about Lisp and extending
Emacs.

--------------------------------------------------------------------------------
:type :section
:title Who is this for?
:body

This article is intended to help you get started with 1) Lisp, and 2) extending
Emacs using Emacs Lisp (elisp). My hope is that it will help you go from being
unable to even read Lisp to have a basic knowledge of how to use elisp to extend
Emacs. If you are curious about the code we will develop, check out
the [full code-listing](#full-code-listing) at the end of this article.

<div class="toc" id="toc"></div>

### A word of warning

The code ahead is written by an absolute beginner in Lisp, and may violate any
number of best-practices and idioms, but it is somewhat working code. If you
spot something that's wrong or something you just don't like, please tell me
what and why so I can improve.


## The task
<a name="task"></a>

The task I set out to solve was to make Emacs slightly more intelligent when
working with tests written in [Buster.JS](http://busterjs.org), which is a test
framework for JavaScript I'm working on
with [August Lilleaas](http://augustl.com/). In particular I wanted Emacs to
help me with Buster's concept of deferred tests. Given a test like this:

```js
buster.testCase("Some object", {
    "should do something nice": function () {
        // ...
    }
});
```

You can "comment out" the name to defer its execution:

```js
buster.testCase("Some object", {
    "//should do something nice": function () {
        // ...
    }
});
```

When a test is deferred, it will still appear in the test report so you don't
forget about it, but it will not run (perhaps because you want to isolate some
other test, don't know how to pass it yet, or whatever).

Using simple key-bindings, I want Emacs to help me:

- toggle the deferred state
- defer all tests but the current one
- enable all tests in the current buffer

Turns out, this isn't particularly hard, and it will introduce us to some core
Emacs Lisp concepts. In this article, we will cover some navigational issues and
toggling the deferred state. In a later article, I will cover the remaining two
issues on the list.

## The basic idea
<a name="idea"></a>

Like so much else in Emacs, I am going to base my extension on regular
expression searches. The code that follows has obvious defeciencies, as I learn
more I intend make it cleverer, but for now, this will do. The core of the
solution is this expression:

```lisp
(defvar buster-test-regexp
  "^\s+\"\.+\s\.+\":\s?fun"
  "Regular expression that finds the beginning of a test function")
```

The regular expression targets a quoted string (double quotes only) that
contains at least one space, and that is followed by a colon and the word "fun".
Hopefully this will be specific enough to target a line where a test starts,
given that Buster does not mandate the test start with "test" or include any
other special words.

## Defining variables
<a name="variables"></a>

The expression above creates a documented global variable `buster-test-regexp`.
It does so using a list that is also a function call. Lists are important in
Lisp. After all, it does take its name from "List processor". The expression is
processed as follows:

```lisp
(function arg1 arg2 ...)
```

The list is space-separated. The first symbol is the function name, and the
following ones are arguments. Emacs Lisp comes with a bunch of built-in
functions, and you can look them up by pressing <kbd>C-h f
&lt;function-name&gt;</kbd>.

If you look up `defvar` you will find that it defines the variable named by the
first argument with the optional initial value provided by the second argument.
Finally there is the optional documentation string provided by the last
argument. You can evaluate this particular expression by placing the cursor
right after the closing parenthesis and hitting <kbd>C-x C-e</kbd>
(`eval-last-sexp`). The documentation string can be looked up with <kbd>C-h v
buster-test-regexp &lt;Enter&gt;</kbd>.

## Moving and restoring point
<a name="save-excursion"></a>

In order to toggle the deferred switch for tests, we need to know where a test
starts. We can break this task up into several sub-tasks. The simplest case is
to locate the exact point where the test begins, assuming it begins somewhere on
the current line:

```lisp
(defun buster-test-name-pos ()
  "Return the position where the test name starts on the current line"
  (save-excursion
    (beginning-of-line)
    (search-forward "\"" (point-at-eol))
    (1- (point))))
```

This example exposes a very central concept in elisp along with a few new
functions. Let's tackle the concept of "point" first.

### Point
<a name="point"></a>

Imagine a buffer (in Emacs, a buffer is a space of editable text, usually the
contents of a file) as a string of characters on a single line. This string
would be indexed from 1 (the very first character) to N (the very last
character). Line breaks count as a single character. Point is the position of
the cursor in this string of characters.

Assume a file with 3 lines, each with 10 characters:

```sh
0123456789
0123456789
0123456789
```

If you place the cursor after the last character on the third line and press
<kbd>M-: (point) &lt;Enter&gt;</kbd> (that's `eval-expression` with an
expression that calls the `point` function), you will see "33" displayed in the
minibuffer. Count yourself, starting from 1, including line-breaks (i.e. 11
characters per line).

### Looking about without moving
<a name="looking-about"></a>

One of the things that surprised me the most when reading up on elisp is the
fact that many functions actually have side-effects. The most common side-effect
is moving point (that is, relocating the cursor).

You will often need to use functions that move point to look around the buffer,
but without physically moving the cursor. That's where `save-excursion` comes
in. This function works as a transaction wrapper for point (and others), making
sure they are reset after the body is executed. Its signature is:

```lisp
(save-excursion &rest BODY)
```

This means that you can pass any number of expressions to `save-excursion`, and
it will execute each one of them, then restore point and return the value of the
last expression.

Let us look at the three expressions passed to `save-excursion` in the example
above:

```lisp
(save-excursion
    (beginning-of-line)
    (search-forward "\"" (point-at-eol))
    (1- (point)))
```

The first expression is a call to the function `beginning-of-line`, which moves
point to the beginning of the current line. The second expression is a call to
`search-forward`. Its two arguments are the string `"\""` (an escaped quote) and
the result of calling the function `point-at-eol`. `point-at-eol` returns the
position of point at the end of the line, but does not actually move point. This
second argument to `search-forward` is a <em>bound</em>, i.e. we only want to
find the first quote on the current line.

The `search-forward` function moves point to just after the match. The third
expression takes advantage of this by returning the position of point minus
one - in other words, the position before the leading quote.

To summarize: this piece of code returns the position of the first quote on the
current line, without moving point. It was originally seen in this context:

```lisp
(defun buster-test-name-pos ()
  "Return the position where the test name starts on the current line"
  (save-excursion
    (beginning-of-line)
    (search-forward "\"" (point-at-eol))
    (1- (point))))
```

The `defun` function defines a function. Its name is `buster-test-name-pos`.
Note the leading "buster-", which it shares with the regular expression from
before. This is just a very lo-fi way of namespacing our code. The function
takes no arguments (thus the empty list as the second argument), has a
documentation string, and one expression (the call to `save-excursion`, which
itself has three expressions) that makes up its body.

In summary, the `buster-test-name-pos` function assumes that the current line
holds the start of a test, and returns the position of the leading quote.

## Is the current line the beginning of a test?
<a name="beginning-of-testp"></a>

If we have one function that assumes that the current line contains the start of
a test, we will also need a function to verify that assumption:

```lisp
(defun buster-beginning-of-test-curr-linep ()
  "Return t if a test starts on the current line"
  (save-excursion
    (beginning-of-line)
    (search-forward-regexp buster-test-regexp (point-at-eol) t)))
```

Note the trailing p in the name. It stands for <em>predicate</em>, and is a
common way of naming functions that return booleans in Emacs. This function
calls `beginning-of-line` and `search-forward-regexp`, both of which move point,
so the body is yet again wrapped in a call to `save-excursion`.

The `search-forward-regexp` function works just like `search-forward`, except
with regular expressions. This time we also pass it a third argument - `t`
(true). If you look up the function with <kbd>C-h f search-forward-regexp</kbd>,
you will find that this argument indicates that we don't want the function to
throw an error if no result is found. Instead, it will just return `nil`, which
works like false.

## Interactively moving to the start of a test
<a name="interactive"></a>

It is time to put our two functions to work and try them out
<em>interactively</em> in an Emacs buffer. Whenever you call an Emacs Lisp
function through a key binding (e.g. <kbd>C-f</kbd>, `forward-char`) or by
hitting <kbd>M-x func-name &lt;Enter&gt;</kbd>, you are calling it
interactively, which is different from regular function calls. Functions have to
explicitly declare themselves interactive for this to work. If you type <kbd>M-x
buster-</kbd> and hit tab, you will note that none of our two functions show up,
because they are not interactive.

### The interactive function
<a name="interactive-function"></a>

Let us make an interactive function that moves to the beginning of the test name
if point is currently at a line where a test is declared:

```lisp
(defun buster-goto-beginning-of-test ()
  "Move point to the beginning of the current test function.
Does nothing if point is not currently on a line where a test is declared."
  (interactive)
  (if (buster-beginning-of-test-curr-linep)
    (goto-char (buster-test-name-pos))))
```

This function is declared interactive by calling the `interactive` function. It
can also take some arguments to control how it receives input, but we will leave
that for later.

The function then calls the `if` <em>special form</em> (a function implemented
in C in elisp core) with two arguments: the test, which is a call to
buster-beginning-of-test-curr-linep, and the expression to evaluate if the test
returns `t`. The expression is a call to `goto-char` with the position returned
by `buster-test-name-pos` as argument. There is no call to `save-excursion`,
because we actually mean to move point this time.

Place the following code in your Emacs' scratch buffer:

```lisp
(defvar buster-test-regexp
  "^\s+\"\.+\s\.+\":\s?fun"
  "Regular expression that finds the beginning of a test function")

(defun buster-test-name-pos ()
  "Return the position where the test name starts on the current line"
  (save-excursion
    (beginning-of-line)
    (search-forward "\"" (point-at-eol))
    (1- (point))))

(defun buster-beginning-of-test-curr-linep ()
  "Return t if a test starts on the current line"
  (save-excursion
    (beginning-of-line)
    (search-forward-regexp buster-test-regexp (point-at-eol) t)))

(defun buster-goto-beginning-of-test ()
  "Move point to the beginning of the current test function.
Does nothing if point is not currently on a line where a test is declared."
  (interactive)
  (if (buster-beginning-of-test-curr-linep)
    (goto-char (buster-test-name-pos))))

;; buster.el key-binding
(global-set-key (kbd "C-c t") 'buster-goto-beginning-of-test)
```

Mark it all and hit <kbd>M-x eval-region &lt;Enter></kbd>. Then place the
following into a buffer:

```js
buster.testCase("Graph", {
    setUp: function () {
        this.graph = capillary.graph.create();
        this.formatter = capillary.formatters.ascii.bindGraph(this.graph);
    },

    "should emit dot for commit": function () {
        var commit = { seqId: 0, id: "1234567", message: "Ok" };
        var listener = this.spy();
        this.graph.on("graph:dot", listener);

        this.graph.graphBranch(C.branch.create([commit]));

        var args = listener.args[0];
        assert.calledOnce(listener);
        assert.calledWith(listener, [0, 0]);
    }
});
```

Place your cursor anywhere on the line with the test name, hit <kbd>C-c t</kbd>
and watch the cursor move to the beginning of the test name. Magic! If you try
it on any other line, the function will do nothing and fail silently.

## Recognizing the current test
<a name="goto-current-test"></a>

Being able to move to where the test name starts from within the same line is
nice, but not very useful. We will improve it by recognizing which test
currently "has focus" (i.e. point is inside it).

We don't have a parser at hand, and ideally we would like to process a minimal
part of the buffer to determine what test we are in. That way our utility will
stay fast and usable.

### The "algorithm"
<a name="goto-algorithm"></a>

To figure out the start position of the current test, we will:

- Search backward for the closest match of of `buster-test-regexp`. Store the
  position as `start`.
- Find the bracket that closes this function by counting `{` and `}`. (When the
  number of forward-brackets and backward-brackets are the same, the function is
  closed). Store the position as `end`.
- If `start &lt; point &lt; end`, then `start` is the starting position of the
  current test. Otherwise, we are not currently inside a test.

Implementing this algorithm will introduce use to a few things:

- Local variables
- The commonly used `cond` and `progn` functions
- Optional function arguments

### Local variables
<a name="local-variables"></a>

Local variables are defined with the `let` function. It takes two arguments - a
list of variables and their (optional) initial value, and one or more "body"
expressions. The variables are only defined within the body. The following
snippet initializes our three variables. `curr` holds the current position
(retrieved by calling the `point` function), and `start-pos` and `end-pos` are
both set to 0 initially.

```lisp
(let ((curr (point))
      (start-pos 0)
      (end-pos 0))
  BODY...)
```

### Finding the start position
<a name="start-position"></a>

To find the start position, we search backwards for the closest match for
`buster-test-regexp` and call `point`. We use `setq` assigns a new value to an
existing variable.

```lisp
(search-backward-regexp buster-test-regexp)
(setq start-pos (point))
```

Finding the end position is a little more work, we will defer it for now by
delegating to another function. It involves counting brackets and moving forward
in the buffer until we find the closing one.

When all the variables are set, we check if the current position is inside the
test we found. If so we return the start position. Otherwise, we return the
current position.

```lisp
(defun buster-beginning-of-test-pos ()
  "Return the start position of the current test"
  (let ((curr (point))
        (start-pos 0)
        (end-pos 0))
    (save-excursion
      (search-backward-regexp buster-test-regexp)
      (setq start-pos (point))
      (setq end-pos (buster-goto-eoblock))
      (if (and (< start-pos curr)
               (< curr end-pos))
          start-pos curr))))
```

### Finding the closing bracket
<a name="closing-bracket"></a>

The function above calls `buster-goto-eoblock`, which moves point to the closing
bracket for the current block. Let's take a stab that function now. The
algorithm we will implement is once again very manual and straight-forward (and
I'm sure smarter people than me has better ways to do this).

- In the first pass, find the closest "{" and set the parens counter to 1
- In subsequent passes, find the next "{" and "}", and move to the closest one
- For each "{", increment the counter
- For each "}", decrement the counter
- When the counter is 0, return `(point)`

Iterative problems like this can be solved fairly elegantly using recursion. We
will make the function accept an optional argument which provides the parens
counter. If it is not set, we initialize it according to the description above.

```lisp
(defun buster-goto-eoblock (&optional open-paren-pairs-count)
  "Move point to the end of the next block"
  (if (null open-paren-pairs-count)
      (progn
        (search-forward "{")
        (setq open-paren-pairs-count 1))))
```

Every argument that follows the `&optional` keyword is optional. This piece of
code introduces the `progn` function. To understand it, consider the `if`
special form's signature, seen below.

```lisp
(if COND THEN ELSE...)
```

`if` can take multiple "else" expressions, but only one "then" expression. We
can work around this by using the `progn` function, which simply evaluates all
its operands and returns the result of the last one. `progn` works pretty much
exactly like JavaScript's comma operator.

#### Finding the next bracket

To complete the `buster-goto-eoblock` function, we need a way to find the
position of the next "{" or "}". We will use a small helper to search forward to
a character and return its position.

```lisp
(defun buster-find-next-pos (char)
  "Return the position at the next occurrence of `char`"
  (save-excursion
    (if (not (search-forward char nil t)) (end-of-buffer))
    (point)))
```

Once again we make use of the `save-excursion` function to search forward
without actually moving point. Note that if we can't find a character, we move
to the end of the buffer. This isn't entirely ideal, but it avoids leaving point
in the same position, possibly causing us to recursively look for the same
characer in the same spot indefinately. With the `buster-find-next-pos` function
in place, we can complete the rest of `buster-goto-eoblock`.

```lisp
(cond
 ((eq 0 open-paren-pairs-count) (point))
 (t ...))
```

The `cond` function takes multiple lists. It moves through each list and
evaluates the first expression in them until it finds one that returns a
non-`nil` value. When it does, it evaluates the following expressions in that
list and returns the value of the last one. The first case in our `cond`
expression is the case where the number of brackets is 0, meaning that we are
done, so we return point.

If we are not done, we find the next "{" and "}" and move to the closest one.
This can be considered the default case, so our modifier is simply the value `t`
(true).

```lisp
(cond
 ((eq 0 open-paren-pairs-count) (point))
 (t (let ((open (buster-find-next-pos "{"))
        (close (buster-find-next-pos "}"))))))
```

We use `let` once more to define local variables holding the position of the two
next brackets. Next up, we figure out which one is closest using a new call to
`cond`.

```lisp
(let ((open (buster-find-next-pos "{"))
      (close (buster-find-next-pos "}")))
  (cond
   ((< open close)
    (goto-char open)
    (buster-goto-eoblock (1+ open-paren-pairs-count)))
   ((< close open)
    (goto-char close)
    (buster-goto-eoblock (1- open-paren-pairs-count)))))
```

After moving to the closest bracket, we call `buster-goto-eoblock` recursively,
passing in the adjusted parens count - incremented if the "{" was closest,
decremented if "}" was closest. Note that in addition to passing the parens
counter to the recursive call, the function relies on point moving, meaning that
it cannot use `save-restriction`. We could have worked around this by passing
the position to go from too, but it seemed more appropriate to move point (given
my limited experience with built-in Emacs Lisp functions).

#### The complete function

Putting all the pieces together results in an 18 line Lisp function, the longest
one I have ever written (in my very short Lisp adventure).

```lisp
(defun buster-goto-eoblock (&optional open-paren-pairs-count)
  "Move point to the end of the next block"
  (if (not open-paren-pairs-count)
      (progn
        (search-forward "{")
        (setq open-paren-pairs-count 1)))
  (cond
   ((eq 0 open-paren-pairs-count) (point))
   (t (let ((open (buster-find-next-pos "{"))
          (close (buster-find-next-pos "}")))
      (cond
       ((< open close)
        (goto-char open)
        (buster-goto-eoblock (1+ open-paren-pairs-count)))
       ((< close open)
        (goto-char close)
        (buster-goto-eoblock (1- open-paren-pairs-count))))))))
```

## Moving to the start of the test - redux
<a name="goto-beginning-redux"></a>

Now that we can move to the beginning of a test from anywhere inside it, we can
update our interactive function from before so it does something useful.

If the cursor is not currently on a line where a test starts, we call our new
function to move to the position (the `goto-char` function moves point to the
position specified by its argument). Finally we move to the exact position where
the quoted test name starts.

The `buster-beginning-of-test-pos` function will throw an error if called before
all tests. The reason is our call to `search-backward-regexp`, which throws an
error if no result is found. Not having any test to move to isn't a big problem,
and we can silently fail that case in the interactive function.

```lisp
(defun buster-goto-beginning-of-test ()
  "Move point to the beginning of the current test function.
Does nothing if point is not currently inside a test function."
  (interactive)
  (condtion-case nil
      (progn
        (if (not (buster-beginning-of-test-curr-linep))
            (goto-char (buster-beginning-of-test-pos)))
        (goto-char (buster-test-name-pos)))
    (error nil)))
```

The updated function conveniently introduces the `condition-case` function,
which is more or less elisp's `try-catch`. Its first argument is a variable that
will have the error bound to it if one is thrown. We don't need it. The second
argument is the expression to catch errors from. Any other arguments are error
handlers. In the example above, we only have one, for the "error" type, which
does nothing.

Note again the use of the `progn` function to have two expressions evaluated
where Emacs expects one.

**Update:** Rolando Pereira emailed me to inform me of the function
`ignore-errors`, which is convenient in cases like the above, where we really
only want to ignore the error and rather return `nil`. It's docstring is
"Execute BODY; if an error occurs, return nil. Otherwise, return result of last
form in BODY." Using this function gives us a slightly shorter result:

```lisp
(defun buster-goto-beginning-of-test ()
  "Move point to the beginning of the current test function.
Does nothing if point is not currently inside a test function."
  (interactive)
  (ignore-errors
      (if (not (buster-beginning-of-test-curr-linep))
          (goto-char (buster-beginning-of-test-pos)))
      (goto-char (buster-test-name-pos))))
```

To try out our new Emacs functionality, put the following code in a buffer, mark
it all and evaluate it using <kbd>M-x eval-region &lt;Enter&gt;</kbd>.

```lisp
(defvar buster-test-regexp
  "^\s+\"\.+\s\.+\":\s?fun"
  "Regular expression that finds the beginning of a test function")

(defun buster-find-next-pos (char)
  "Return the position at the next occurrence of `char`"
  (save-excursion
    (search-forward char nil t)
    (point)))

(defun buster-test-name-pos ()
  "Return the position where the test name starts on the current line"
  (save-excursion
    (beginning-of-line)
    (search-forward "\"" (point-at-eol))
    (1- (point))))

(defun buster-beginning-of-test-curr-linep ()
  "Return t if a test starts on the current line"
  (save-excursion
    (beginning-of-line)
    (search-forward-regexp buster-test-regexp (point-at-eol) t)))

(defun buster-goto-eoblock (&optional open-paren-pairs-count)
  "Move point to the end of the next block"
  (if (null open-paren-pairs-count)
      (progn
        (search-forward "{")
        (setq open-paren-pairs-count 1)))
  (cond
   ((eq 0 open-paren-pairs-count) (point))
   (t (let ((open (buster-find-next-pos "{"))
          (close (buster-find-next-pos "}")))
      (cond
       ((< open close)
        (goto-char open)
        (buster-goto-eoblock (1+ open-paren-pairs-count)))
       ((< close open)
        (goto-char close)
        (buster-goto-eoblock (1- open-paren-pairs-count))))))))

(defun buster-beginning-of-test-pos ()
  "Return the start position of the current test"
  (let ((curr (point))
        (start-pos 0)
        (end-pos 0))
    (save-excursion
      (search-backward-regexp buster-test-regexp)
      (setq start-pos (point))
      (setq end-pos (buster-goto-eoblock))
      (if (and (< start-pos curr)
               (< curr end-pos))
          start-pos curr))))

(defun buster-goto-beginning-of-test ()
  "Move point to the beginning of the current test function.
Does nothing if point is not currently inside a test function."
  (interactive)
  (ignore-errors
      (if (not (buster-beginning-of-test-curr-linep))
          (goto-char (buster-beginning-of-test-pos)))
      (goto-char (buster-test-name-pos))))

;; buster.el key bindings
(global-set-key (kbd "C-c t") 'buster-goto-beginning-of-test)
```

Once again we can try this out on the following JavaScript. Place the cursor
somewhere in the body of the test function and hit <kbd>C-c t &lt;Enter></kbd>.
The cursor should move to the beginning of the quoted test name.

```js
buster.testCase("Graph", {
    setUp: function () {
        this.graph = capillary.graph.create();
        this.formatter = capillary.formatters.ascii.bindGraph(this.graph);
    },

    "should emit dot for commit": function () {
        var commit = { seqId: 0, id: "1234567", message: "Ok" };
        var listener = this.spy();
        this.graph.on("graph:dot", listener);

        this.graph.graphBranch(C.branch.create([commit]));

        var args = listener.args[0];
        assert.calledOnce(listener);
        assert.calledWith(listener, [0, 0]);
    }
});
```

## Disabling tests
<a name="disable-test"></a>

With the navigation in place, we can finally attempt the initial problem -
disabling tests by adding two slashes in front of their name. To do this, we
move to the beginning of the current test, move past the quote character, and
insert the string "//". Simple enough.

```lisp
(defun buster-disable-test ()
  "Disables a single test by using the 'comment out' feature of
Buster.JS xUnit style tests. Finds test to disable using
buster-goto-beginning-of-test"
  (interactive)
  (save-excursion
    (buster-goto-beginning-of-test)
    (forward-char)
    (insert "//")))

(global-set-key (kbd "C-c C-d") 'buster-disable-test)
```

To avoid actually moving the cursor, we wrap the function body in a call to
`save-excursion`. Evaluate the code above and place the cursor inside the test
from before, and hit <kbd>C-c C-d</kbd>. The test name should have the two
slashes added.

### Disabling tests outside a test
<a name="disable-test-error"></a>

What happens if we call the new `buster-disable-test` function when the cursor
is not inside a test? As you might remember, `buster-goto-beginning-of-test`
will simply leave point at the current position if it is not inside a test. This
means that we need to make sure we are inside a test before doing anything else.

```lisp
(defun buster-disable-test ()
  "Disables a single test by using the 'comment out' feature of
Buster.JS xUnit style tests. Finds test to disable using
buster-goto-beginning-of-test"
  (interactive)
  (save-excursion
    (buster-goto-beginning-of-test)
    (if (buster-beginning-of-test-curr-linep)
        (progn (forward-char)
               (insert "//")))))
```

This avoids adding "//" in random places. If we are not inside a test, the
function will simply do nothing. The last thing to consider is what to do if
attempting to disable an already disabled test. As is, the function will just
keep adding "//". We can fix this by searching for existing slashes before
adding new ones.

In this last version of the function, we will rely on the fact that
`search-forward` returns point (i.e. non-nil) after finding a match. If we pass
`t` as the third argument, it will return `nil` if no match is found. We will
only add new slashes if the search fails.

```lisp
(defun buster-disable-test ()
  "Disables a single test by using the 'comment out' feature of
Buster.JS xUnit style tests. Finds test to disable using
buster-goto-beginning-of-test"
  (interactive)
  (save-excursion
    (buster-goto-beginning-of-test)
    (if (buster-beginning-of-test-curr-linep)
        (progn
          (forward-char)
          (if (not (search-forward "//" (+ (point) 2) t))
              (insert "//"))))))
```

The search for the slashes is bounded by `(+ (point) 2)`. In other words, we
only look for them directly after the quote. This final version can be called
any number of times but will only ever add two slashes to the test name.

## Enabling tests
<a name="enable-test"></a>

When a test is disabled, we need a way to enable it again. `buster-enable-test`
is simpler than its couterpart. It will move to the start of the current test,
search for the slashes in the test name and remove them if found. Otherwise it
does nothing.

```lisp
(defun buster-enable-test ()
  "Ensables a single test by removing the 'comment' inserted by
buster-disable-test"
  (interactive)
  (save-excursion
    (buster-goto-beginning-of-test)
    (if (search-forward "\"//" (+ (point) 3) t)
        (delete-region (- (point) 2) (point)))))

(global-set-key (kbd "C-c C-e") 'buster-enable-test)
```

## Wrapping up
<a name="wrap-up">

With both `buster-disable-test` and `buster-enable-test` in place, we have
reached our initial goal. If you made it all the way down here, then thanks for
your attention. Hopefully you learned a little about both Emacs and Lisp.

To summarize, we have seen basic Lisp syntax in use, and we have succesfully
built several functions on our own, using multiple built-in Emacs Lisp
functions. Below you will find a list of all the functions we used along with a
short description of each. Note that many of these accept optional arguments
that we have not discussed. You can find usage examples throughout the article,
and more information in Emacs by typing <kbd>M-h f func-name &lt;Enter&gt;</kbd>
(many of the explanations below were lifted right from the Emacs documentation).

## Elisp function reference
<a name="function-reference"></a>

<dl>
  <dt><code>(+ &amp;rest NUMBERS-OR-MARKERS)</code></dt>
  <dd>Return the sum of the arguments.</dd>
  <dt><code>(- &amp;optional NUMBER-OR-MARKER &amp;rest MORE-NUMBERS-OR-MARKERS)</code></dt>
  <dd>Return <code>NUMBER-OR-MARKER</code> subtracted by remaining arguments</dd>
  <dt><code>(1+ NUMBER)</code></dt>
  <dd>Return <code>NUMBER</code> incremented by one</dd>
  <dt><code>(1- NUMBER)</code></dt>
  <dd>Return <code>NUMBER</code> decremented by one</dd>
  <dt><code>(< NUM1 NUM2)</code></dt>
  <dd>Return <code>t</code> if the first argument is less than the second</dd>
  <dt><code>(and CONDITIONS...)</code></dt>
  <dd>
    Evaluate arguments until one of them yields nil, then return
    nil. Otherwise return value of last argument
  </dd>
  <dt><code>(beginning-of-line &amp;optional N)</code></dt>
  <dd>Move point to the beginning of the current line</dd>
  <dt><code>(cond CLAUSES...)</code></dt>
  <dd>Try each clause until one succeeds</dd>
  <dt><code>(condition-case VAR BODYFORM &amp;rest HANDLERS)</code></dt>
  <dd>Catch errors thrown by <code>BODYFORM</code></dd>
  <dt><code>(ignore-errors &amp;rest BODY)</code></dt>
  <dd>Execute <code>BODY</code>; if an error occurs, return nil. Otherwise, return result of last form in <code>BODY</code>.</dd>
  <dt><code>(defun NAME ARGLIST [DOCSTRING] BODY...)</code></dt>
  <dd>Define a function</dd>
  <dt><code>(defvar SYMBOL &amp;optional INITVALUE DOCSTRING)</code></dt>
  <dd>Define a variable</dd>
  <dt><code>(delete-region START END)</code></dt>
  <dd>Delete text in the current buffer from <code>START</code> to <code>END</code></dd>
  <dt><code>(eq OBJ1 OBJ2)</code></dt>
  <dd>Return <code>t</code> if the arguments are equal</dd>
  <dt><code>(forward-char &amp;optional N)</code></dt>
  <dd>Move point <code>N</code> (default one) character(s) ahead</dd>
  <dt><code>(goto-char POSITION)</code></dt>
  <dd>Move point to the exact <code>POSITION</code></dd>
  <dt><code>(if COND THEN ELSE...)</code></dt>
  <dd>
    Evaluate <code>THEN</code> if <code>COND</code> is
    non-nil, <code>ELSE</code> otherwise
  </dd>
  <dt><code>(insert &amp;rest ARGS)</code></dt>
  <dd>
    Insert text into the current buffer and move point to the end of the
    inserted text
  </dd>
  <dt><code>(interactive &amp;optional ARGS)</code></dt>
  <dd>
    Mark function as interactive. Also control how arguments can be passed
    to an interactive function (not covered here)
  </dd>
  <dt><code>(let VARLIST BODY...)</code></dt>
  <dd>Define scoped variables and execute expressions in scope</dd>
  <dt><code>(not OBJECT)</code></dt>
  <dd>Return <code>t</code> if <code>OBJECT</code> is nil</dd>
  <dt><code>(point)</code></dt>
  <dd>Return the current position of point</dd>
  <dt><code>(point-at-eol &amp;optional N)</code></dt>
  <dd>Return the position of point at the end of the current line</dd>
  <dt><code>(progn BODY...)</code></dt>
  <dd>
    Eval <code>BODY</code> forms sequentially and return value of last
    one
  </dd>
  <dt><code>(save-excursion &amp;rest BODY)</code></dt>
  <dd>
    Save point, mark, and current buffer; execute <code>BODY</code>;
    restore those things
  </dd>
  <dt><code>(search-backward-regexp REGEXP &amp;optional BOUND NOERROR COUNT)</code></dt>
  <dd>
    Search backward from point for match for regular
    expression <code>REGEXP</code>
  </dd>
  <dt><code>(search-forward STRING &amp;optional BOUND NOERROR COUNT)</code></dt>
  <dd>
    Search forward from point for match for <code>STRING</code>
  </dd>
  <dt><code>(search-forward-regexp REGEXP &amp;optional BOUND NOERROR COUNT)</code></dt>
  <dd>
    Search forward from point for match for regular
    expression <code>REGEXP</code>
  </dd>
  <dt><code>(setq [SYM VAL]...)</code></dt>
  <dd>
    Set each <code>SYM</code> to the value of its <code>VAL</code>.
  </dd>
</dl>

In an upcoming article, I will show how to write interactive functions that take
arguments, operate on regions and other segments of the current buffer.

## Full code-listing
<a name="full-code-listing"></a>

```lisp
(defvar buster-test-regexp
  "^\s+\"\.+\s\.+\":\s?fun"
  "Regular expression that finds the beginning of a test function")

(defun buster-find-next-pos (char)
  "Return the position at the next occurrence of `char`"
  (save-excursion
    (search-forward char nil t)
    (point)))

(defun buster-test-name-pos ()
  "Return the position where the test name starts on the current line"
  (save-excursion
    (beginning-of-line)
    (search-forward "\"" (point-at-eol))
    (1- (point))))

(defun buster-beginning-of-test-curr-linep ()
  "Return t if a test starts on the current line"
  (save-excursion
    (beginning-of-line)
    (search-forward-regexp buster-test-regexp (point-at-eol) t)))

(defun buster-goto-eoblock (&optional open-paren-pairs-count)
  "Move point to the end of the next block"
  (if (null open-paren-pairs-count)
      (progn
        (search-forward "{")
        (setq open-paren-pairs-count 1)))
  (cond
   ((eq 0 open-paren-pairs-count) (point))
   (t (let ((open (buster-find-next-pos "{"))
          (close (buster-find-next-pos "}")))
      (cond
       ((< open close)
        (goto-char open)
        (buster-goto-eoblock (1+ open-paren-pairs-count)))
       ((< close open)
        (goto-char close)
        (buster-goto-eoblock (1- open-paren-pairs-count)))))))

(defun buster-beginning-of-test-pos ()
  "Return the start position of the current test"
  (let ((curr (point))
        (start-pos 0)
        (end-pos 0))
    (save-excursion
      (search-backward-regexp buster-test-regexp)
      (setq start-pos (point))
      (setq end-pos (buster-goto-eoblock))
      (if (and (< start-pos curr)
               (< curr end-pos))
          start-pos curr))))

(defun buster-goto-beginning-of-test ()
  "Move point to the beginning of the current test function.
Does nothing if point is not currently inside a test function."
  (interactive)
  (ignore-errors
      (if (not (buster-beginning-of-test-curr-linep))
          (goto-char (buster-beginning-of-test-pos)))
      (goto-char (buster-test-name-pos))))

(defun buster-disable-test ()
  "Disables a single test by using the 'comment out' feature of
Buster.JS xUnit style tests. Finds test to disable using
buster-goto-beginning-of-test"
  (interactive)
  (save-excursion
    (buster-goto-beginning-of-test)
    (if (buster-beginning-of-test-curr-linep)
        (progn
          (forward-char)
          (if (not (search-forward "//" (+ (point) 2) t))
              (insert "//"))))))

(defun buster-enable-test ()
  "Ensables a single test by removing the 'comment' inserted by
buster-disable-test"
  (interactive)
  (save-excursion
    (buster-goto-beginning-of-test)
    (if (search-forward "\"//" (+ (point) 3) t)
        (delete-region (- (point) 2) (point)))))

;; buster.el key bindings
(global-set-key (kbd "C-c C-d") 'buster-disable-test)
(global-set-key (kbd "C-c C-e") 'buster-enable-test)
(global-set-key (kbd "C-c t") 'buster-goto-beginning-of-test)
```

## Update 2013-05-14

Big thanks to Ala'a Mohammad for cleaning up/improving several aspects of this
article.

[Follow me (@cjno) on Twitter](http://twitter.com/cjno)
