package com.beta.idcenter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * IdBitWorker demo
 *
 * @author chenyongbo
 * @date 2016/6/21.
 */
public class IdBitWorkerTest {

    public static void main(String[] args) {
        final long idepo = System.currentTimeMillis() - 3600 * 1000L;

        IdBitWorker worker = new IdBitWorker(1, 1, 0, idepo);

        for (int i = 0; i < 10; i++) {
            System.out.println(worker.nextId());
        }

        System.out.println(worker);

        long nextId = worker.nextId();
        System.out.println(nextId);
        long time = worker.getIdTimestamp(nextId);
        System.out.println(time + "->" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(time)));
    }
}
