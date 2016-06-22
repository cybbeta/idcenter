/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.  You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.beta.idcenter;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 在分布式系统中，需要生成全局UID的场合还是比较多的，twitter的snowflake解决了这种需求，实现也还是很简单的，除去配置信息，核心代码就是毫秒级时间41位+机器ID
 * 10位+毫秒内序列12位。 核心代码为其IdWorker这个类实现，其原理结构如下，我分别用一个0表示一位，用—分割开部分的作用： 0---0000000000 0000000000
 * 0000000000 0000000000 0 --- 00000 ---00000 ---0000000000 00
 * 在上面的字符串中，第一位为未使用（实际上也可作为long的符号位），接下来的41位为毫秒级时间，然后5位datacenter标识位，5位机器ID（并不算标识符，实际是为线程标识），然后12
 * 位该毫秒内的当前毫秒内的计数，加起来刚好64位，为一个Long型。
 * 这样的好处是，整体上按照时间自增排序，并且整个分布式系统内不会产生ID碰撞（由datacenter和机器ID作区分），并且效率较高，经测试，snowflake每秒能够产生26万ID
 * 左右，完全满足需要。
 *
 * @author chenyongbo
 * @date 2016/6/21.
 */
public class IdBitWorker implements IdWorker {
    private static final long startTimeStamp = 1464710400000L; //定义一个起始时间 2016-6-1 00:00:00

    private static final long workerIdBits = 5L;
    private static final long dataCenterIdBits = 5L;
    private static final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private static final long maxDataCenterId = -1L ^ (-1L << dataCenterIdBits);

    private static final long sequenceBits = 12L;
    private static final long workerIdShift = sequenceBits;
    private static final long dataCenterIdShift = sequenceBits + workerIdBits;
    private static final long timestampLeftShift = sequenceBits + workerIdBits + dataCenterIdBits;
    private static final long sequenceMask = -1L ^ (-1L << sequenceBits);
    private static final Random r = new Random();

    private final long workerId;
    private final long dataCenterId;
    private final long idEpoch;
    private long lastTimestamp = -1L;
    private long sequence = 0;

    public IdBitWorker() {
        this(startTimeStamp);
    }

    public IdBitWorker(long idEpoch) {
        this(r.nextInt((int) maxWorkerId), r.nextInt((int) maxDataCenterId), 0, idEpoch);
    }

    public IdBitWorker(long workerId, long dataCenterId, long sequence) {
        this(workerId, dataCenterId, sequence, startTimeStamp);
    }

    public IdBitWorker(long workerId, long dataCenterId, long sequence, long idEpoch) {
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
        this.sequence = sequence;
        this.idEpoch = idEpoch;

        if (workerId < 0 || workerId > maxWorkerId) {
            throw new IllegalArgumentException("workerId is illegal: " + workerId);
        }
        if (dataCenterId < 0 || dataCenterId > maxDataCenterId) {
            throw new IllegalArgumentException("dataCenterId is illegal: " + dataCenterId);
        }

        if (idEpoch >= timeGen()) {
            throw new IllegalArgumentException("idEpoch is illegal: " + idEpoch);
        }
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public long getWorkerId() {
        return workerId;
    }

    public long getTime() {
        return timeGen();
    }

    @Override
    public synchronized long nextId() {
        long timestamp = timeGen();
        if (timestamp < lastTimestamp) {
            throw new IllegalArgumentException("Clock moved backwards.");
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;
        long id = ((timestamp - idEpoch) << timestampLeftShift) | (dataCenterId <<
                dataCenterIdShift) | (workerId << workerIdShift) | sequence;
        return id;
    }

    public long getIdTimestamp(long id) {
        return idEpoch + (id >> timestampLeftShift);
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }

        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("IdBitWorker{");
        sb.append("workerId=").append(workerId);
        sb.append(", dataCenterId=").append(dataCenterId);
        sb.append(", idEpoch=").append(idEpoch);
        sb.append(", lastTimestamp=").append(lastTimestamp);
        sb.append(", sequence=").append(sequence);
        sb.append('}');
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        IdBitWorker worker = new IdBitWorker();
        ExecutorService executor = Executors.newFixedThreadPool(8);

        CountDownLatch countDownLatch = new CountDownLatch(1000000);
        Runnable run = () -> {
            worker.nextId();
            countDownLatch.countDown();
        };

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            executor.execute(run);
        }
        countDownLatch.await();
        System.out.println(System.currentTimeMillis() - startTime);
        executor.shutdown();
    }
}
