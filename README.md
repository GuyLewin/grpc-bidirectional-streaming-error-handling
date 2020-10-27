# grpc-bidirectional-streaming-error-handling
While working on a [grpc-java](https://github.com/grpc/grpc-java) project with 
[bidirectional streaming](https://grpc.io/docs/languages/java/basics/#bidirectional-streaming-rpc) 
I noticed lack of documentation on how to handle errors.

I wanted to know:
* When are errors thrown?
* Do I have to manually call `onCompleted()` after receiving a `onError()` callback?

This project contains an example based on `grpc-java` version `1.33.0`, with many tests to answer these questions.