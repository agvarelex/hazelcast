/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.client.proxy;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.proxy.ProxyHelper;
import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;
import com.hazelcast.core.MultiExecutionCallback;
import com.hazelcast.executor.RunnableAdapter;
import com.hazelcast.instance.MemberImpl;
import com.hazelcast.nio.protocol.Command;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ExecutorServiceClientProxy implements IExecutorService {

    final ProxyHelper proxyHelper;
    final HazelcastClient client;
    final String name;
    final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public ExecutorServiceClientProxy(HazelcastClient client, String name) {
        this.client = client;
        this.name = name;
        proxyHelper = new ProxyHelper(client.getSerializationService(), client.getConnectionPool());
    }

    public void execute(Runnable command) {
        executeOnKeyOwner(command, null);
    }

    public void executeOnKeyOwner(Runnable command, Object key) {
        submitToKeyOwner(new RunnableAdapter<Object>(command), key);
    }

    public void executeOnMember(Runnable command, Member member) {
        submitToMember(new RunnableAdapter<Object>(command), member);
    }

    public void executeOnMembers(Runnable command, Collection<Member> members) {
        for(Member member: members){
            executeOnMember(command, member);
        }
    }

    public void executeOnAllMembers(Runnable command) {
        executeOnMembers(command, client.getCluster().getMembers());
    }

    public <T> Future<T> submit(Callable<T> task) {
        return submitToKeyOwner(task, null);
    }

    public <T> Future<T> submitToKeyOwner(final Callable<T> task, final Object key) {
        return executorService.submit(new Callable<T>() {
            public T call() throws Exception {
                return (T) proxyHelper.doCommandAsObject(proxyHelper.toData(key), Command.EXECUTE,
                        new String[]{}, proxyHelper.toData(task), proxyHelper.toData(key));
            }
        });
    }

    public <T> Future<T> submitToMember(final Callable<T> task, final Member member) {
        return executorService.submit(new Callable<T>() {
            public T call() throws Exception {
                InetSocketAddress address = member.getInetSocketAddress();
                return (T) proxyHelper.doCommandAsObject(proxyHelper.toData(member), Command.EXECUTE,
                        new String[]{address.getHostName(), String.valueOf(address.getPort())}, proxyHelper.toData(task));
            }
        });
    }

    public <T> Map<Member, Future<T>> submitToMembers(Callable<T> task, Collection<Member> members) {
        return null;
    }

    public <T> Map<Member, Future<T>> submitToAllMembers(Callable<T> task) {
        return null;
    }

    public void submit(final Runnable task, final ExecutionCallback callback) {
        submit(new RunnableAdapter(task), callback);
    }

    public void submitToKeyOwner(Runnable task, Object key, ExecutionCallback callback) {
        submitToKeyOwner(new RunnableAdapter<Object>(task), key, callback);
    }

    public void submitToMember(Runnable task, Member member, ExecutionCallback callback) {
        submitToMember(new RunnableAdapter<Object>(task), member, callback);
    }

    public void submitToMembers(Runnable task, Collection<Member> members, MultiExecutionCallback callback) {
        submitToMembers(new RunnableAdapter<Object>(task), members, callback);
    }

    public void submitToAllMembers(Runnable task, MultiExecutionCallback callback) {
        submitToAllMembers(new RunnableAdapter<Object>(task), callback);
    }

    public <T> void submit(final Callable<T> task, final ExecutionCallback<T> callback) {
        executorService.submit(new Runnable() {
            public void run() {
                Future<T> f = submit(task);
                try {
                    callback.onResponse(f.get());
                } catch (InterruptedException e) {
                    return;
                } catch (ExecutionException e) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public <T> void submitToKeyOwner(final Callable<T> task, final Object key, final ExecutionCallback<T> callback) {
        executorService.submit(new Runnable() {
            public void run() {
                Future<T> f = submitToKeyOwner(task, key);
                try {
                    callback.onResponse(f.get());
                } catch (InterruptedException e) {
                    return;
                } catch (ExecutionException e) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public <T> void submitToMember(final Callable<T> task, final Member member, final ExecutionCallback<T> callback) {
        executorService.submit(new Runnable() {
            public void run() {
                Future<T> f = submitToMember(task, member);
                try {
                    callback.onResponse(f.get());
                } catch (InterruptedException e) {
                    return;
                } catch (ExecutionException e) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public <T> void submitToMembers(Callable<T> task, Collection<Member> members, MultiExecutionCallback callback) {
    }

    public <T> void submitToAllMembers(Callable<T> task, MultiExecutionCallback callback) {
    }

    public Object getId() {
        return null;
    }

    public String getName() {
        return null;
    }

    public void destroy() {
    }

    public void shutdown() {
    }

    public List<Runnable> shutdownNow() {
        return null;
    }

    public boolean isShutdown() {
        return false;
    }

    public boolean isTerminated() {
        return false;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return submit(new RunnableAdapter<T>(task, result));
    }

    public Future<?> submit(Runnable task) {
        return submit(new RunnableAdapter(task));
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return null;
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return null;
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
