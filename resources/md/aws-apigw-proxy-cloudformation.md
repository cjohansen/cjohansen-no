# Setting up an Api Gateway Proxy Resource using Cloudformation

I just spent the better part of a day trying to figure out how to do something
as seemingly simple as configuring an AWS Api Gateway catch-all endpoint to
proxy to another HTTP service. Amazon has
[documentation detailing how to do this](http://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-set-up-simple-proxy.html).
Using the console. Using the console is great for learning how stuff works, but
it is not great for creating reproducible production environments. So I wanted
to achieve this with Cloudformation. Which wasn't all that straight-forward.

Like most other things, solving said problem really is quite straight-forward
once you know _how_. My Googlings where unsuccessful in this area, so I'll
detail how here, in case someone out there has the same problem.

## The REST API

In the interest of providing a complete, working configuration, I'll include all
the required parts. If you already have an API configuration, you'll probably be
most interested in the `AWS::ApiGateway::Method` resource.

At the top of the mountain is a Rest API:

```yml
  Api:
    Type: 'AWS::ApiGateway::RestApi'
    Properties:
      Name: MyProxyAPI
```

## The proxy resource

In order to proxy all paths to the API, you need two resources: the root
resource, and a catch-all resource. Well, a catch-almost-all anyway, since the
catch-all does *not* catch the root resource. The root resource is created for
you, the proxy resource is not:

```yml
  Resource:
    Type: 'AWS::ApiGateway::Resource'
    Properties:
      ParentId: !GetAtt Api.RootResourceId
      RestApiId: !Ref Api
      PathPart: '{proxy+}'
```

As you can see, this resource references the `Api.RootResourceId` as its parent.
The path part `{proxy+}` is a greedy match for any path. If you wanted to only
match requests under e.g. `/blog/*`, you'd have to define two resources:

```yml
  BlogResource:
    Type: 'AWS::ApiGateway::Resource'
    Properties:
      ParentId: !GetAtt Api.RootResourceId
      RestApiId: !Ref Api
      PathPart: 'blog'

  Resource:
    Type: 'AWS::ApiGateway::Resource'
    Properties:
      ParentId: !Ref BlogResource
      RestApiId: !Ref Api
      PathPart: '{proxy+}'
```

## The methods

Next up we'll configure the methods. As I want to proxy everything, I just
define one `ANY` method for each resource.

The root resource is a 1:1 from the root path to the root path on your proxy
target. For this example, we're proxying to an imaginary S3 bucket website:

```yml
  RootMethod:
    Type: 'AWS::ApiGateway::Method'
    Properties:
      HttpMethod: ANY
      ResourceId: !GetAtt Api.RootResourceId
      RestApiId: !Ref Api
      AuthorizationType: NONE
      Integration:
        IntegrationHttpMethod: ANY
        Type: HTTP_PROXY
        Uri: http://my-imaginary-bucket.s3-website-eu-west-1.amazonaws.com/
        PassthroughBehavior: WHEN_NO_MATCH
        IntegrationResponses:
          - StatusCode: 200

```

Next up is the proxy resource method, and this is what took me an embarrassing
amount of time to figure out.

```yml
  ProxyMethod:
    Type: 'AWS::ApiGateway::Method'
    Properties:
      HttpMethod: ANY
      ResourceId: !Ref Resource
      RestApiId: !Ref Api
      AuthorizationType: NONE
      RequestParameters:
        method.request.path.proxy: true
      Integration:
        CacheKeyParameters:
          - 'method.request.path.proxy'
        RequestParameters:
          integration.request.path.proxy: 'method.request.path.proxy'
        IntegrationHttpMethod: ANY
        Type: HTTP_PROXY
        Uri: http://my-imaginary-bucket.s3-website-eu-west-1.amazonaws.com/
        PassthroughBehavior: WHEN_NO_MATCH
        IntegrationResponses:
          - StatusCode: 200
```

Let's discuss the key components of this. First of all, setting the resource
path to `{proxy+}` is **not** enough to be able to use this in the target URL.
You also need to specify `RequestParameters` to state that it is OK to use the
`proxy` parameter from the path in the integration configuration.

As if that wasn't enough, you also have to inform Cloudformation of how you will
access the `proxy` parameter in your integration request path, by specifying
`Integration.RequestParameters`. It is a map of parameters from the method
request to parameters in the integration request.

Those two bits are crucial, because now we can finally use `{proxy}` to insert
the proxied path in our integration uri.

## Deployment

In order to use the API you need a deployment. Because the deployment does not
have a direct dependency on either of the methods, and because we cannot deploy
an API with no methods, we use `DependsOn` to help Cloudformation figure out the
order of things:

```yml
  Deployment:
    DependsOn:
      - RootMethod
      - ProxyMethod
    Type: 'AWS::ApiGateway::Deployment'
    Properties:
      RestApiId: !Ref Api
      StageName: dev
```

Choose a stage name of your liking.

## The whole shebang

That's all there is to it. Doesn't look very hard when you know what to do.

```yml
AWSTemplateFormatVersion: 2010-09-09
Description: An API that proxies requests to another HTTP endpoint

Resources:
  Api:
    Type: 'AWS::ApiGateway::RestApi'
    Properties:
      Name: SomeProxyApi

  Resource:
    Type: 'AWS::ApiGateway::Resource'
    Properties:
      ParentId: !GetAtt Api.RootResourceId
      RestApiId: !Ref Api
      PathPart: '{proxy+}'

  RootMethod:
    Type: 'AWS::ApiGateway::Method'
    Properties:
      HttpMethod: ANY
      ResourceId: !GetAtt Api.RootResourceId
      RestApiId: !Ref Api
      AuthorizationType: NONE
      Integration:
        IntegrationHttpMethod: ANY
        Type: HTTP_PROXY
        Uri: http://my-imaginary-bucket.s3-website-eu-west-1.amazonaws.com/
        PassthroughBehavior: WHEN_NO_MATCH
        IntegrationResponses:
          - StatusCode: 200

  ProxyMethod:
    Type: 'AWS::ApiGateway::Method'
    Properties:
      HttpMethod: ANY
      ResourceId: !Ref Resource
      RestApiId: !Ref Api
      AuthorizationType: NONE
      RequestParameters:
        method.request.path.proxy: true
      Integration:
        CacheKeyParameters:
          - 'method.request.path.proxy'
        RequestParameters:
          integration.request.path.proxy: 'method.request.path.proxy'
        IntegrationHttpMethod: ANY
        Type: HTTP_PROXY
        Uri: http://my-imaginary-bucket.s3-website-eu-west-1.amazonaws.com/
        PassthroughBehavior: WHEN_NO_MATCH
        IntegrationResponses:
          - StatusCode: 200

  Deployment:
    DependsOn:
      - RootMethod
      - ProxyMethod
    Type: 'AWS::ApiGateway::Deployment'
    Properties:
      RestApiId: !Ref Api
      StageName: !Ref StageName
```

