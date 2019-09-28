--------------------------------------------------------------------------------
:type :meta
:title Idempotent Cloudformation Updates
:published #time/ldt "2017-10-19T12:00"
:tags [:cloudformation]
--------------------------------------------------------------------------------
:type :section
:theme :dark1
:title Idempotent Cloudformation Updates
:body

Having worked a lot with Ansible, I came to
[Cloudformation](https://aws.amazon.com/cloudformation/) thinking (or at least
hoping) it would be "Ansible for AWS": a declarative definition of a desired
state, which it is not quite:

- Cloudformation clearly separates creation and updates (no ["upsert"](https://en.wiktionary.org/wiki/upsert))
- Cloudformation is *not* idempotent
- Cloudformation only touches what Cloudformation already touched

The last point is actually not so bad, since it means that being serious about
automation on AWS also means you need to lay off point-and-click in the console.
This is ultimately a good thing (point-and-click is fine for learning,
automation is good for production). The first two points though, need fixing.

--------------------------------------------------------------------------------
:type :section
:title Disclaimer: Amateur at work
:body

Let's be clear: I am no Cloudformation expert. Maybe you're reading this and
thinking that I'm being the [expert
beginner](https://www.daedtech.com/how-developers-stop-learning-rise-of-the-expert-beginner/),
thinking I have "a better way" and solving the wrong problems. If that's the
case [please let me know](https://twitter.com/cjno/).

## Idempotency

I first discovered Cloudformation's lack of idempotency working on a template
that involved [provisioning an SSL
certificate](http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-certificatemanager-certificate.html).
Running the same template with the same input parameters and tags would _always_
provision a new certificate. This is a bit of a drag, as provisioning
certificates involves manually opening an email to click a link.

It turns out though, that Cloudformation can act in an idempotent-ish manner by
way of the [client request
token](http://docs.aws.amazon.com/AWSCloudFormation/latest/APIReference/API_CreateStack.html).
This is an arbitrary string you pass to `create-stack` and `update-stack`, and
Cloudformation will only ever process _one_ request with the same client request
token. If you make a second pass with the same token, Cloudformation *fails*. I
guess I would've preferred a silent no-op, but I'll take what I can get. This
means that if you build a hash of your template and the parameters (and any
tags set on the stack), we can basically make Cloudformation apply templates in
an idempotent manner.

The template and parameters can be hashed in any way, I chose `shasum`, which is
readily available:

```sh
template_hash=`shasum template.yml | awk '{ print $1 }'`
```

`shasum` also outputs the filename, so I'm using `awk` to get rid of it.

The parameters can be hashed the same way:

```sh
parameters="ParameterKey=MyParam1,ParameterValue=12 \
            ParameterKey=MyOtherParam,ParameterValue=lol"
tags="..."
input_hash=`echo $parameters$tags | shasum --text | awk '{ print $1 }'`
```

Now we can build the token and call on Cloudformation to update the stack:

```sh
aws cloudformation update-stack \
    --stack-name MyStack \
    --parameters $parameters \
    --tags $tags \
    --client-request-token=$template_hash$input_hash \
    --template-body file://template.yml
```

If you run this twice, it will fail the second time. Success! Kinda. The problem
with this approach is that CloudFormation remembers the client request token
seemingly forever. That means that the following scenario does not work as
expected:

1. Create a stack with client request token `"A"`
2. Change some parameters and update the stack with client request token `"B"`
3. Revert changes and "update" stack back to the original version -
   **CloudFormation fails**

The last step fails because the client request token was already used. So the
once so promising client request token cannot be used after all.

## Tags to the resque

We have a hash that uniquely defines a change set, but we cannot use it with
client request token, lest our stack will be unable to assume the same state
twice. Speaking of change sets, CloudFormation has a first-class notion of a
[change
sets](http://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-changesets.html).
However, they are not a good solution to quick updates (or not) of stacks as
they have side-effects, and are geared more towards reviewing changes before
applying.

One possible solution to our idempotency problem is tags. Use the hash we
computed before as a tag. Before applying the stack up, read tags from the
existing stack and abort if it is already in the desired state.

## Upsert

`create-stack` and `update-stack` take exactly the same arguments, so there's
not much point in needing to branch on them in client code. Let's instead write
an `apply-stack` function that can figure out if the stack needs creating or
updating.

`describe-stacks` fails if you use it with a non-existent stack, so something
like this would work:

```sh
aws cloudformation describe-stacks --stack-name MyStack

if [ $? -eq 0 ]; then
    aws cloudformation update-stack ...
else
    aws cloudformation create-stack ...
fi
```

This can be elaborated on and put in a wrapper script that can deliver
Cloudformation "upsert" behavior.

## Combining efforts

Let's put the pieces together and create a fully functional `cf-apply-stack`
script. We'll call it like so:

```sh
./cf-apply-stack.sh --profile myprofile \
                    --stack-name MyStack \
                    --parameters ParameterKey=MyParam,ParameterValue=12 \
                    --tags Name=MyStuff \
                    --template-body file://template.yml
```

The wrapper script will roughly parse the arguments to look at `stack-name`,
`parameters`, `tags`, `template-body`, `region`, and `profile`, then use those
values to describe the stack to figure out if it exists. It will then either
create or update the stack, passing in a hash as a tag that ensures that no-op
updates are never applied.

We'll start by extracting the arguments we're interested in:

```sh
#!/bin/bash

setArgs () {
    while [ "$1" != "" ]; do
        case $1 in
            "--stack-name")
                shift
                stack_name=$1
                ;;
            "--parameters")
                shift
                parameters=$1
                ;;
            "--tags")
                shift
                tags=$1
                ;;
            "--template-body")
                shift
                template=$1
                ;;
            "--profile")
                shift
                profile=$1
                ;;
            "--region")
                shift
                region=$1
                ;;
        esac
        shift
    done
}

setArgs $*
```

We'll then describe the stack to see if it exists:

```sh
describe_args="--stack-name $stack_name"

if [ ! -z "$profile" ]; then
    describe_args="$describe_args --profile $profile"
fi

if [ ! -z "$region" ]; then
    describe_args="$describe_args --region $region"
fi

description=`aws cloudformation describe-stacks $describe_args 2> /dev/null`
```

Based on whether it exists or not, we'll choose the action and alert the user of
our choice:

```sh
if [ $? -eq 0 ]; then
    echo "Updating stack"
    command="update-stack"
else
    echo "Creating stack"
    command="create-stack"
fi
```

We'll then use [jq](https://stedolan.github.io/jq/) to find the existing client
request token, if any:

```sh
current_crt=`echo "$description" | jq -r '.Stacks[0].Tags[] | select(.Key=="ClientRequestToken").Value'`
```

We'll use the relevant pieces to compute a new token (assuming that
`--template-body` is a `file://...` URL):

```sh
template_hash=$(echo $(shasum ${template:7:${#template}} | awk '{ print $1 }'))
input_hash=$(echo $(echo "$parameters$tags$stack_name$region" | shasum --text | awk '{ print $1 }'))
crt="$template_hash$input_hash"
```

Finally, we'll compare the two. If the new and old token are different, we'll
call on `aws cloudformation`, otherwise, we're done.

```sh
if [ "$crt" == "$current_crt" ]; then
  echo "Current version is up to date, nothing to do"
  exit 0
else
    args=`addTags "Key=ClientRequestToken,Value=$crt" $*`
    echo "aws cloudformation $command $args"
fi
```

The whole thing is available on [my GitHub](https://github.com/cjohansen/cf-apply-template):

```sh
#!/bin/bash

setArgs () {
    while [ "$1" != "" ]; do
        case $1 in
            "--stack-name")
                shift
                stack_name=$1
                ;;
            "--parameters")
                shift
                parameters=$1
                ;;
            "--tags")
                shift
                tags=$1
                ;;
            "--template-body")
                shift
                template=$1
                ;;
            "--profile")
                shift
                profile=$1
                ;;
            "--region")
                shift
                region=$1
                ;;
        esac
        shift
    done
}

addTags () {
    local tags="$1"
    shift
    local res=""
    local has_tags=0

    while [ "$1" != "" ]; do
        res="$res $1"

        if [ "$1" == "--tags" ]; then
            has_tags=1
            res="$res $2 $tags"
            shift
        fi

        shift
    done

    if [ $has_tags -eq 0 ]; then
        res="$res --tags $tags"
    fi

    echo $res
}

setArgs $*

describe_args="--stack-name $stack_name"

if [ ! -z $profile ]; then
    describe_args="$describe_args --profile $profile"
fi

if [ ! -z $region ]; then
    describe_args="$describe_args --region $region"
fi

description=`aws cloudformation describe-stacks $describe_args 2> /dev/null`

if [ $? -eq 0 ]; then
    echo "Updating stack"
    command="update-stack"
else
    echo "Creating stack"
    command="create-stack"
fi

template_hash=$(echo $(shasum ${template:7:${#template}} | awk '{ print $1 }'))
input_hash=$(echo $(echo "$parameters$tags$stack_name$region" | shasum --text | awk '{ print $1 }'))
crt="$template_hash$input_hash"
current_crt=`echo "$description" | jq -r '.Stacks[0].Tags[] | select(.Key=="ClientRequestToken").Value'`

if [ "$crt" == "$current_crt" ]; then
    echo "Current version is up to date, nothing to do"
    exit 0
else
    args=`addTags "Key=ClientRequestToken,Value=$crt" $*`
    echo "aws cloudformation $command $args"
fi
```

[Let me know what you think](https://twitter.com/cjno).
