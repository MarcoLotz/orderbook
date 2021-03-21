# Coding Challenge: Order Book

## Overview
The code provides an implementation of the interface Level2View.

I also performed an analysis on possible improvements and other strategies that could surely be efficient to solve the problem.
There's no silver bullet and the applied solution depends on multiple characteristics provided by the data supplier
(including how orderIDs are generated and how many orders are in the same Price Level).

## Getting started
I am using <b>GitHub Packages</b> as artifact store.
It seems that maven requires special configuration on settings.xml in order to retrieve packages from GitHub Repository.

In order to build and perform tests, please run

```shell
mvn clean verify
```

Should an error pointing that 'marcolotz-parent cannot be found' prompt, please retrieve it from [here](https://github.com/marcolotz/parent).
After retrieving it,  install the parent pom version 1.0.0 in your local maven repository with:

```shell
mvn clean install
```

## Implementation decisions

This is my favourite part :)

I tried to go as simple and loosely coupled as possible.
I had to read a bit to understand what this kind of system actually performs and investigate good strategies to handle it.
I have added my references in the last section.
I spent about 5 hours implementing the whole solution.
As any high-performance system, there's a huge amount of possible improvements - and they really depend on the scenarios.

## Interface Segregation / Composition
I used some concepts Hexagonal Architecture for this project, without any adapter implementation - since I assumed that all order handling is core domain logic.
Looking at the originally provided interface "Level2View" I decided to apply to composition pattern on it, since it seems like two components with different responsabilities.
One for listening and one for provided metrics.
This separation of concern could be used in the future - e.g. implementing a system that provide metrics in an eventually consistent fashion.

I personally do not like to use the convention InterfaceNameImpl naming to implementation classes.
Since Java libraries dont use that (there's no ListImpl but a specification detail of the implementation e.g. ArrayList).
I used the same criteria when implementing the interfaces.

Last but not least, the provided interface does not contain checked exceptions.
I have validated the operations on the OrderBook on SimpeLevel2View.class - but I am slightly unsured about this.
I am not a domain expert, but seems fair to me that the provided interface should have checked exceptions in it.
Specially since it does seem to be likely that a client of the interface uses it in an incorrect and foreseeable way.
An example of such mistake would be by removing an order that doesn't exist.
In order to not change the interface itself, I created RuntimeExceptions for those scenarios.

## Search for the best data structure

There are multiple ways to improve performance of the system. One of them is searching for the fastest data structure.
Different data structures will have different performance for different exchanges.
Also, not necessarily better Runtime/Memory complexity of a data-structure means that it will be the fastest solution.

In order to decouple the data-structure used to store the orderbook data from the implementation itself, I created an interface called OrderBook.

### Tree Search

Implemented by TreeOrderBook class, this strategy used three different collections in order to handle all the operations of the interface in O(1) amortized time.

#### Runtime complexity

The binary search tree keeps track of all the TopPrices.
Insertions on this tree are O(log (n)) time and only happen when new PriceLevel appears.
Reading about the domain, it seems that the number of Orders is much higher than the number of price levels.
Thus adding most of the orders will perform no mutations on the tree structure.
All other operations are O(1) since they are usually mutations on map. I am assuming that the maps are well balanced.
I have also pre-allocated for all the data-structures to avoid O(n) operations whenever adding elements due to space allocation.

When doing the research, I have realised that the price levels had an array of orders instead of a Map.
This performs searches in O(n) time. In my implementation I used a map, that performs O(1) search - but more testing here would be required to make a fair decision.
Maybe the array has better cache locality hit overall on processor level and TLB - which in this case the O(n) of the array would perform better than the O(1) of the map.
I haven't tested - but this can be easily done.

The price levels are surely going to change over time.
Because of this I used Red-Black tree to implement the binary search tree due to its self balancing capabilities.

```
Overall:
since there are much more orders than price levels, mutations on the tree should happen much less frequent than new orders.
This brings the solution of O(1) amortized.
```
#### Space complexity
- Tree: O(price levels)
- levelMap: O(price levels + orders)
- orderMap: O(orders)
```
Overall:
since there are much more orders than price levels, overall memory complexity is O(orders).
```
#### Small modifications on the open-source code
I used a Red and Black tree implementation that I found online. 
The original code used entities that extended Comparable.
I have changed to use a Comparator instead. 
I see the original implementation as a breach in the separation of concern.
It's not a problem of the entity stored in the tree to decide how to compare itself with another.
It's a behaviour required by the tree due to its inner workings.
I changed the code to use a custom provided Comparator instead.

Also, in order to keep track of the top element, I changed the "insert" operation of the tree to return the inserted node after insertion.

### Sparse Array

While reading about the problem, I also found many people suggesting using a sparse array in this kind of problem.
Even though many of the operations would be O(N) time, it makes heavily use of computer architecture with space locality being used for caching,
pipelining, and TLB.
I think this is quite likely to be the case, and I have seen this happening in graph processing domain before.
One of the biggest problems of graph processing is exactly the lack of cache locality on processor level.
Knowing this, a researcher with an old single-core laptop beat all the BigData Graph framework benchmarks (e.g. GraphX, Apache Giraph, GraphLab) just by using better memory layout strategies.
The results are [here](https://www.usenix.org/system/files/conference/hotos15/hotos15-paper-mcsherry.pdf) and [here](http://www.frankmcsherry.org/graph/scalability/cost/2015/01/15/COST.html)

## Garbage collection / Memory Footprint

In java, garbage collection does perform a huge impact on the performance.
For time critical systems, specially for HFT, its desirable to either minimize or completely remove garbage collection cycles.

### Streams, Optionals and Immutable Object
I did use tons of stream operations, optional and the classes in my model are immutable.
This will trigger GC from time to time. If this system was designed for HFT, I shouldn't have used those approaches in normal JVM.

A solution to avoid GC freezes while using this kind of structures is changing the JVM provider to [Azul](https://www.azul.com/products/zing/), that claims to provide pauseless execution.
Of course, over dimensioning memory to avoid GC calls is also a possibility.
Back in 2015 I used to work on 4TB Ram server to perform some graph processing experiments.
That amount of RAM costed about £80k by the time and surely only got cheaper.
Looking quickly, in 2019 it costed about 38k USD already.

### Object Pool

Another possibility to avoid custom hardware or custom JVM is to use "object pools".
This strategy allocates a pool of objects and keeps reusing them whenever required.
It also assumes that no unrequired garbage is created into memory and aims to have a close to none garbage collection calls.

In order to use this strategy, I would have had to change my domain from immutable objects to mutable and only created objects through a factory.
The factory would also need to be notified whenever an object is not of my use interest anymore, in order to make it available.

### Collections overhead
The default implementation of collections in java is not always the most memory efficient one.
In fact, I have seen stream operations being avoided by many Red Coders in the past - which could really impact a HFT system.
In my code I have used the default ones.
In order to reduce the memory footprint I could have used other implementations as [Chronicle Map](https://github.com/OpenHFT/Chronicle-Map) or [GNU Trove](http://trove4j.sourceforge.net/html/overview.html).
I used Trove in the past when creating references to large-scale lookup system.
By the time, I also used compression (with Kryo) on the map values themselves to improve memory-to-core transference speed - like many [indexing system do](https://nlp.stanford.edu/IR-book/html/htmledition/index-compression-1.html).

## Parallelism
### Thread-safe
The code is not thread safe.
In order to be thread safe, the implementations of OrderBook would have to be changed to take this requirement into consideration.

### Scaling out
In the global market, I recall that there are about 20M ISINs.
Whenever I read that NASDAQ can produce up to 200k order events a second, I understand that this is for all ISINs listed there.
From my understanding of the domain, the order book refers only to orders against a same ISIN.
Thus, this system could be scaled horizontally keyed on ISIN.
The only question would be how to route the ISINs between multiple processing systems without adding bigger overhead (e.g. hitting network).
A possibility would be the use of FPGAs or Shared Memory in a single machine (e.g. Apache Arrow).

## Async Logging
Just for fun I used async / lazy logging (Log4j2).
I've been working with lots of Kafka lately and Async logging really makes a difference on high-throughput systems.
Kafka source currently still uses synchronous logging - I saw a PR being open this week to update to Log4j2.

# Investigations for real system

## Property Based Testing
I done Unit Test of all the classes available in this code.
If this system was going to be deployed in production, I would have implemented Property Based Test on the implementation of OrderBook to make sure that all the assumptions hold.
This would avoid testing only simplistic scenarios.
There are many scala libraries focused only on that.

## IT testing to compare the performance
I would mock the ingested data from the supplier and compare the performance of each implementation of OrderBook for each data supplier.
Of course I would use common benchmarks metrics (like 95 percentile and 99 percentile analysis on operations) and make sure that the system is in stable condition (e.g. warming up cache, enforcing JIT compilation, etc.) 

# References
1. [What is an efficient data structure to model an order book](https://quant.stackexchange.com/questions/3783/what-is-an-efficient-data-structure-to-model-order-book)
2. [How to build a fast limit order book](https://gist.github.com/halfelf/db1ae032dc34278968f8bf31ee999a25)
3. [Red-Black tree implementation](https://github.com/Arsenalist/Red-Black-Tree-Java-Implementation)