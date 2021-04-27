# Results 4

<p> Three Kafka brokers and a load balancer with two servers </p>

## Instance Details

| Service           | Instance Type   | AMI / Engine   |
|:------------------|:----------------|:---------------|
| Server            |  t2.micro       | Amazon Linux 2 |
| Kafka             |  t2.xLarge      | Ubuntu 20.04   |
| Database Consumer |  t2.micro       | Amazon Linux 2 |
| Store             |  t2.medium      | Amazon Linux 2 |
| RDS               |  db.t2.micro    | MySQL Community|

------------------------------------------------------

<br/> 

## Settings Details

| Setting Type                                                        | Setting Value   |
|:--------------------------------------------------------------------|:---------------:|
| Number of Partitions                                                |  50             |
| Number of brokers                                                   |  3              |
| Consumer thread count                                               |  45             |
| Consumer polling                                                    |  10 ms          |
| Replication factor                                                  |  3              |
| linger.ms                                                           |  50             |
| acks                                                                |  0              |
| num threads Kafka server uses to receive requests                   |  3 (default)    |   
| num threads Kafka server uses to process requests                   |  8 (default)    |
| Consumer auto.commit.interval                                       |  20 ms          |

<br/>

### Notes

- The minimum replication factor for Kafka is 1. That is, there will always be
at least one copy of the data and we will always have one leader and one follower (ISR)
- Each consumer thread consumes from one partition.
- linger.ms is the maximum amount of time (in milliseconds) to accumulate at most
batch.num.messages or batch.size bytes worth of messages before sending to the Kafka broker.
- acks = 0 means that we do not wait for any acknowledgement from the broker that the
message has been sent.


-----------------------------------------------------

## Results

| Thread Count | Wall Time (s)  | Median Latency (ms) |  Mean Latency (ms) | Throughput (requests/second) | 99th Percentile (ms) |
|:-------------|:--------------:|:-----------------:|:------------------:|:-----------------------------:|:--------------------:|
|  256         |  125.0         | 30.0              | 34.2               |  5530.4                       |   91.0               |
|  512         |  280.6         | 82.0              | 79.4               |  4926.6                       |   172.0              |
|  1024        |  615.3         | 172.0             | 178.4              |  4493.2                       |   456.0              | 