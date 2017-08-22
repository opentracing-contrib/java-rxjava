[![Build Status][ci-img]][ci] [![Released Version][maven-img]][maven]

# OpenTracing RxJava Instrumentation
OpenTracing instrumentation for RxJava.

## Installation

### RxJava 1
pom.xml
```xml
<dependency>
    <groupId>io.opentracing.contrib</groupId>
    <artifactId>opentracing-rxjava-1</artifactId>
    <version>0.0.4</version>
</dependency>
```

### RxJava 2
pom.xml
```xml
<dependency>
    <groupId>io.opentracing.contrib</groupId>
    <artifactId>opentracing-rxjava-2</artifactId>
    <version>0.0.4</version>
</dependency>
```

## Usage


```java
// Instantiate tracer
Tracer tracer = ...

// Optionally register tracer with GlobalTracer 
GlobalTracer.register(tracer);

```

### RxJava 1
```java
// Enable Tracing via TracingRxJavaUtils
TracingRxJavaUtils.enableTracing(tracer);
```

### RxJava 2
```java
// Enable Tracing via TracingRxJava2Utils
TracingRxJava2Utils.enableTracing(tracer);
```

[ci-img]: https://travis-ci.org/opentracing-contrib/java-rxjava.svg?branch=master
[ci]: https://travis-ci.org/opentracing-contrib/java-rxjava
[maven-img]: https://img.shields.io/maven-central/v/io.opentracing.contrib/opentracing-rxjava-1.svg
[maven]: http://search.maven.org/#search%7Cga%7C1%7Copentracing-rxjava-1
