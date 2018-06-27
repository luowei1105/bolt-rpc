package com.fmgame.bolt.benchmark;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import com.fmgame.bolt.benchmark.entity.IBenchmark;
import com.fmgame.bolt.benchmark.entity.Person;
import com.fmgame.bolt.benchmark.entity.PersonFullName;

public class TestPojoRunnable extends AbstractClientRunnable {
    Person person = new Person();

    public TestPojoRunnable(IBenchmark benchmark, String params, CyclicBarrier barrier, CountDownLatch latch, long startTime, long endTime) {
        super(benchmark, barrier, latch, startTime, endTime);
        person.setName("bolt");
        person.setFullName(new PersonFullName("first", "last"));
        person.setBirthday(new Date());
        List<String> phoneNumber = new ArrayList<String>();
        phoneNumber.add("123");
        person.setPhoneNumber(phoneNumber);
        person.setEmail(phoneNumber);
        Map<String, String> address = new HashMap<String, String>();
        address.put("hat", "123");
        person.setAddress(address);
    }

    @Override
    protected Object call(IBenchmark benchmark) {
        Object result = benchmark.echoService(person);
        return result;
    }
}
