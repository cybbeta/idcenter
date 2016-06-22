package com.beta.idcenter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * IdHexWorker demo
 *
 * @author chenyongbo
 * @date 2016/6/22.
 */
public class IdHexWorkerTest {
    public static void main(String[] args) {
        final long idepo = System.currentTimeMillis() /1000 - 3600;

        IdHexWorker worker = new IdHexWorker(1, 1, 0, idepo);

        for (int i = 0; i < 100000; i++) {
            System.out.println(worker.nextId());
        }

        System.out.println(worker);

        long nextId = worker.nextId();
        System.out.println(nextId);
        long time = worker.getIdTime(nextId);
        System.out.println(time + "->" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(time * 1000)));
    }
}
