# RSProt

[![GitHub Actions][actions-badge]][actions] [![MIT license][mit-badge]][mit] [![OldSchool - 221 (Alpha)](https://img.shields.io/badge/OldSchool-221_(Alpha)-9a1abd)](https://github.com/blurite/rsprot/tree/master/protocol/osrs-221-api/src/main/kotlin/net/rsprot/protocol/api)

## Status
> [!NOTE]
> This library is currently in an alpha release stage. Breaking changes may be
> done in the future.

## Alpha Usage

The artifacts can be found on [Maven Central](https://central.sonatype.com/search?q=rsprot).

In order to add it to your server, add the below line under dependencies
in your build.gradle.kts.

```kts
implementation("net.rsprot:osrs-221-api:1.0.0-ALPHA-20240505")
```

An in-depth tutorial on how to implement it will be added into this read-me
document in the future!

## Introduction
RSProt is an all-in-one networking library for private servers,
primarily targeting the OldSchool RuneScape scene. Contributions for
other revisions are welcome, but will not be provided by default.

## Prerequisites
- Kotlin 1.9.23
- Java 11

## Supported Versions
This library currently only supports revision 221 OldSchool desktop clients.

## API Analysis

All our Netty handlers disable auto-read by default. This means the protocol
is responsible for triggering reads whenever it is ready to continue, ensuring
we do not get an avalanche of requests from the client right away.
All handlers additionally have an idle state tracker to disconnect any clients
which have been idle for 30 seconds on JS5 and 60 seconds on other services.
On-top of this, we respect the channel writability status and avoid writing
to the clients if the writability starts returning false, ensuring we do not
grow the outbound buffer indefinitely, resulting in a potential attack vector.

###  JS5
This section discusses the JS5 implementation found for the OldSchool
variant. Other versions of the game may require different logic.

The API offers a JS5 service that runs on a single thread in order to serve
all connected clients _fairly_. The JS5 service thread will be in a waiting
stage whenever there are no clients to serve to, and gets woken up when
new requests come in. Unlike most JS5 implementation seen, this will serve
a specific block of bytes to each client, with an emphasis on the clients
logged into the game, rather than a full group or multiple groups.
This ensures that each connected client receives its fair share of data,
and that no clients get starved due to a malicious actor flooding with
requests for expensive groups. Furthermore, the service is set to flush
only when the need arises, rather than after each write into the channel.

By default, the JS5 service is set to write a 512 byte block to each
client per iteration. If the client is in a logged in state, a block
that's three times larger is written instead, so 1,536 bytes get written
to those. In both scenarios, if the remaining number of bytes
is less than that of the block length, only the remaining bytes are written,
meaning it is possible for the service to write less than the pre-defined
block size. After 10,240 bytes have been written into the channel since
the last flush, or if 10 complete requests have been written, the channel
is flushed and the trackers are reset. Additionally, if we run out of
requests to fulfill, the channel is flushed, even if the aforementioned
thresholds are not met. Every number mentioned here is configurable by the
server, should the need arise.

Two available methods of feeding data to the service are offered,
one that is based on normal Netty ByteBuf objects, and one that is based
on RandomAccessFiles, utilizing Netty's FileRegions to do zero-copy
writes. While the latter may sound enticing, it is replacing memory with
disk IO, which is likely significantly slower. As a result of that,
File Region based implementation is only recommended for development
environments, allowing one to not have to load JS5 up at all, not even
opening the cache. This of course requires the cache to have been split
into JS5-compatible files on the disk.

The underlying implementation utilizes a primitive Int-based ArrayDeque
to keep track of requests made by the client, with an upper limit of 200
requests of both the urgent and prefetch types. This ensures no garbage
is produced when adding and removing entries from the queue. Furthermore,
because the service itself is single-threaded, we can get away with using
non-thread-safe implementations which are far simpler and require less
processing power. Each connected client is kept in a unique queue that
is based on a mix of HashSet and ArrayDeque to ensure uniqueness in the
queue.

The clients are only written to as long as they can be written to, avoiding
exploding the outgoing buffer - any client which still has pending requests,
but cannot be written to, will be put in an idle state which is resumed as
soon as Netty notifies the client that the channel is once again writable.
Additionally, the client will stop reading any new requests if the number
of requests reaches 200 in either of the queues.
This JS5 implementation __does__ support encryption keys (xor).
While the feature itself is only used in abnormal circumstances, the support
for it is still offered.

### Login
This section discusses the login implementation found for the OldSchool
variant. Other versions of the game may require different logic.

By default, all game logins will require proof of work to have been completed.
The login block itself will be decoded only after proof of work has been
completed and verified correct. In any other circumstance, the connection
is rejected and the login block buffer gets released. For reconnections,
proof of work is not used - this is because reconnections are cheap to validate,
and the time taken to execute proof of work in the client may result in
the reconnection timing out.
The network service offers a way to decode login block on another thread,
away from Netty's own worker threads. With the default implementation,
a ForkJoinPool is used to execute the job. This is primarily because
RSA deciphering takes roughly half a millisecond for a 1024-bit key, creating
a potential attack vector via logins.

#### Proof of Work

The OldSchool client supports a proof of work implementation for logins,
allowing the server to throttle the client by giving it a CPU-intensive
job to execute before the login may take place. As of writing this section,
the OldSchool client only supports a SHA-256 hashing based proof of work
implementation. When the server receives the initial login request, it
has to decide whether to request proof of work from the client, or allow
the login right away. In our default implementation, proof of work is
always required for non-reconnect logins. The server sends out a request
to the client containing a large 495-byte random block of bytes turned into
a hexadecimal string. Along with the block, a difficulty value is provided.
The client will then iterate from 0 to infinity, taking the iteration number
and appending it to the end of hte random text block. It then runs the
SHA-256 hashing algorithm on that string. Once done, the client has to check
if the number of leading zero bits in the resulting string is equal to or
greater than the input difficulty - if it is, it transmits that number
to the server which will validate it by running the same hashing algorithm
just once, if it isn't, the client continues looping. With a default difficulty
of 18, the client is typically does anywhere from 100,000 to 500,000 hashes
before finding a solution that works. As mentioned, the server only has to
run the hashing once to confirm the results. As the data is completely random,
the number of iterations the client has to do is rather volatile.
Some requests may finish very quickly despite a high difficulty, while others
take a lot longer with a smaller one. As such, servers should account for
bad luck and slower devices, both of which increase the likelihood of the
proof of work process taking longer than the timeout duration for logins.
Servers may also choose to implement a difficulty system that scales depending
on the number of requests over a given timespan, or any other variation of this.
The default implementation uses a static difficulty of 18.
Furthermore, it's important to note that each difficulty increase of 1
doubles the amount of work the client has to perform on average, e.g. a
difficulty of 19 is twice as difficult to figure out than a difficulty of 18.

#### Beta Worlds
As of revision 220, the OldSchool client has a semi-broken implementation
for beta worlds. Jagex had intended to add a locking mechanism to the client,
to avoid sending certain groups in the cache to anyone that hasn't successfully
logged into the beta world. While the implementation itself is relatively fine,
it does nothing to prevent someone using a headless client to request the
groups anyway. The JS5 server is completely unaware of whether the client
has passed the authorization in OldSchool, so any headless client is free
to request the entire cache whenever they want. This implementation only
prevents transmitting the cache to anyone that is trying to download it via
the default client.

Because the implementation is relatively broken and doesn't actually protect
anything, we have made a choice to not try to entertain this mechanism,
and instead the login service performs an immediate request under the hood
to request all the remaining cache CRCs before informing the server of
the login request having been made. This allows us to still pass a complete
login block that looks the same as during a non-beta-world login.
The server must enable the beta world flag in both the library and the
client itself. If the flag status does not match between the two, either an
exception is thrown during login decoding, or the login will hang due to a
lack of bytes having been transmitted by the client.

### Game Connection
The game connection implementation utilizes a flipping mechanism for auto-read
and single-decode. When the handler switches from login to game, auto-read
is enabled and single decode is disabled. As the decoder decodes more messages,
a tracker keeps track of how many user-type and client-type packets have been
received within this current cycle. If the threshold of 10 user-type packets,
or 50 client-type packets is reached, auto-read is disabled and single-decode
is enabled. This guarantees that Netty will __immediately__ halt decoding of
any more packets, even if the ctx has more bytes to be read.
At the start of each game cycle, the server is responsible for polling the
packets of each connected client. After it does so, the auto-read status
is re-enabled, and single-decode is disabled, allowing the client to continue
reading packets. Using this implementation, as our socket receive buffer
size is 65536 bytes, the maximum number of incoming bytes per channel is
only 65536 * 2 in a theoretical scenario - with the socket being full,
as well as one socket's worth of data having been transferred to Netty's
internal buffer.

As the server is responsible for providing a repository of message consumers
for game packets, we can furthermore utilize this to avoid decoding packets
for which a consumer has not been registered. In such cases, we simply
skip the number of bytes that is the given packet's size, rather than
slicing the buffer and decoding a new data class out of it.
Any packets for which there has not been registered a consumer will not
count towards message count thresholds, as the service is not aware of
the category in which the given packet belongs.

## Design Choices

### Memory-Optimized Messages
A common design choice throughout this library will be to utilize smaller data types wherever applicable.
The end-user will always get access to normalized messages though.

Below are two examples of the same data structure, one in a compressed data structure, another in a traditional data class:
<details>
  <summary>Compressed HostPlatformStats</summary>

```kt
public class HostPlatformStats(
    private val _version: UByte,
    private val _osType: UByte,
    public val os64Bit: Boolean,
    private val _osVersion: UShort,
    private val _javaVendor: UByte,
    private val _javaVersionMajor: UByte,
    private val _javaVersionMinor: UByte,
    private val _javaVersionPatch: UByte,
    private val _unknownConstZero1: UByte,
    private val _javaMaxMemoryMb: UShort,
    private val _javaAvailableProcessors: UByte,
    public val systemMemory: Int,
    private val _systemSpeed: UShort,
    public val gpuDxName: String,
    public val gpuGlName: String,
    public val gpuDxVersion: String,
    public val gpuGlVersion: String,
    private val _gpuDriverMonth: UByte,
    private val _gpuDriverYear: UShort,
    public val cpuManufacturer: String,
    public val cpuBrand: String,
    private val _cpuCount1: UByte,
    private val _cpuCount2: UByte,
    public val cpuFeatures: IntArray,
    public val cpuSignature: Int,
    public val clientName: String,
    public val deviceName: String,
) {
    public val version: Int
        get() = _version.toInt()
    public val osType: Int
        get() = _osType.toInt()
    public val osVersion: Int
        get() = _osVersion.toInt()
    public val javaVendor: Int
        get() = _javaVendor.toInt()
    public val javaVersionMajor: Int
        get() = _javaVersionMajor.toInt()
    public val javaVersionMinor: Int
        get() = _javaVersionMinor.toInt()
    public val javaVersionPatch: Int
        get() = _javaVersionPatch.toInt()
    public val unknownConstZero: Int
        get() = _unknownConstZero1.toInt()
    public val javaMaxMemoryMb: Int
        get() = _javaMaxMemoryMb.toInt()
    public val javaAvailableProcessors: Int
        get() = _javaAvailableProcessors.toInt()
    public val systemSpeed: Int
        get() = _systemSpeed.toInt()
    public val gpuDriverMonth: Int
        get() = _gpuDriverMonth.toInt()
    public val gpuDriverYear: Int
        get() = _gpuDriverYear.toInt()
    public val cpuCount1: Int
        get() = _cpuCount1.toInt()
    public val cpuCount2: Int
        get() = _cpuCount2.toInt()
}
```
</details>

<details>
  <summary>Traditional HostPlatformStats</summary>

```kt
public data class HostPlatformStats(
    public val version: Int,
    public val osType: Int,
    public val os64Bit: Boolean,
    public val osVersion: Int,
    public val javaVendor: Int,
    public val javaVersionMajor: Int,
    public val javaVersionMinor: Int,
    public val javaVersionPatch: Int,
    public val unknownConstZero1: Int,
    public val javaMaxMemoryMb: Int,
    public val javaAvailableProcessors: Int,
    public val systemMemory: Int,
    public val systemSpeed: Int,
    public val gpuDxName: String,
    public val gpuGlName: String,
    public val gpuDxVersion: String,
    public val gpuGlVersion: String,
    public val gpuDriverMonth: Int,
    public val gpuDriverYear: Int,
    public val cpuManufacturer: String,
    public val cpuBrand: String,
    public val cpuCount1: Int,
    public val cpuCount2: Int,
    public val cpuFeatures: IntArray,
    public val cpuSignature: Int,
    public val clientName: String,
    public val deviceName: String,
)
```
 </details>

> [!IMPORTANT]
> There is a common misconception among developers that types on heap smaller than ints are only useful in their respective primitive arrays.
> In reality, this is only sometimes true. There are a lot more aspects to consider. Below is a breakdown on the differences.


<details>
  <summary>Memory Alignment Breakdown</summary>

[JVM's memory alignment](https://www.baeldung.com/java-memory-layout) is the reason why we prioritize compressed messages over traditional ones.
It is commonly believed that primitives like bytes and shorts do not matter on the heap and end up consuming the same amount of memory as an int,
but this is simply not true. The object itself is subject to memory alignment and will be padded to a specific amount of bytes as a whole.
Given this information, we can see the stark differences between the two objects by adding up the memory usage of each of the properties.
For this example, we will assume all the strings are empty and stored in the
[JVM's string constant pool](https://www.baeldung.com/java-string-constant-pool-heap-stack), so we only consider the reference of those.
The cpuFeatures array is a size-3 int array.
By adding up all the properties of the compressed variant of the HostPlatformStats, we come to the following results:
| Type | Count | Data Size (bytes) |
| --- | --- | --- |
| byte | 11 | 1 |
| boolean | 1 | 1 |
| short | 4 | 2 |
| int | 2 | 4 |
| intarray | 1 | Special |
| reference | 8 | Special |

By adding up all the data types, we come to a sum of (11 x 1) + (1 x 1) + (4 x 2) + (2 x 4) + (1 x intarray) + (8 x reference),
which adds up to 28 + (1 x intarray) + (8 x reference) bytes.

However, now, let's look at the traditional variant:
| Type | Count | Data Size (bytes) |
| --- | --- | --- |
| int | 18 | 4 |
| intarray | 1 | Special |
| reference | 8 | Special |

The total adds up to (18 x 4) + (1 x intarray) + (8 x reference),
which adds up to 72 + (1 x intarray) + (8 x reference) bytes.

<ins>So, what about the special types?</ins>

This is where things become less certain. It is down to the JVM and the amount of memory allocated to the JVM process.

On a 32-bit JVM, the memory layout looks like this:
| Type | Data Size (bytes) |
| --- | --- |
| Object Header | 8 |
| Object Reference | 4 |
| Byte Alignment | 4 |

On a 64-bit JVM, the memory layout is as following:
| Type | Data Size (bytes) |
| --- | --- |
| Object Header | 12 |
| Object Reference (xmx <= 32gb, compressed OOPs[^1]) | 4 |
| Object Reference (xmx > 32gb) | 8 |
| Byte Alignment | 8 |

So, how much do our HostPlatformStats objects consume in the end?
If we assume we are on a 64-bit JVM with the maximum heap size set to 32GB or less, the object memory consumption boils down to the following:
From the earlier example, the intarray will consume 12 + (3 * 4) bytes, and the string references will consume 4 bytes each.
So if we now add these values up, we come to a total of:
Compressed HostPlatformStats: 84 bytes
Traditional HostPlatformStats: 128 bytes
Due to the JVM's 8 byte alignment however, all objects are aligned to consume a multiple of 8 bytes.
In this scenario, because our compressed implementation comes to 84 bytes, which is not a multiple-of-8 bytes, a small waste occurs.
The JVM will allocate 4 extra bytes to fit the 8-byte alignment constraint, giving us a total of 88 bytes consumed.
In the case of the traditional implementation, since it is already a multiple of 8, it will remain as 128 bytes.

 </details>

> [!NOTE]
> The reason we prefer compressed implementations is to reduce the memory footprint of the library. As demonstrated above,
> the compressed implementation consumes 31.25% less memory than the traditional counterpart.
> While the compressed code may be harder to read and take longer to implement, this is a one-time job as the models rarely change.
> On the larger scale, this could result in a considerably smaller footprint of the library for servers, and less work for garbage collectors.

## Benchmarks
Benchmarks can be found [here](BENCHMARKS.md). Only performance-critical
aspects of the application will be benchmarked.

[^1]: [Compressed ordinary object pointers](https://www.baeldung.com/jvm-compressed-oops) are a trick utilized by the 64-bit JVM to compress object references into 4 bytes instead of the traditional 8. This is only possible if the Xmx is set to 32GB or less. Since Java 7, compressed OOPs are enabled by default if available.

[actions-badge]: https://github.com/blurite/rsprot/actions/workflows/ci.yml/badge.svg
[actions]: https://github.com/blurite/rsprot/actions
[mit-badge]: https://img.shields.io/badge/license-MIT-informational
[mit]: https://opensource.org/license/MIT
