package com.microsoft.seeds.fsmexecutor.models;

import com.microsoft.seeds.fsmexecutor.models.request.StartFSMRequest;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class StartFSMReqQueue {
//    private long timeoutMillis;
//    private Map<String, StartFSMRequest> contextIdToReqMap;
//    private Map<String, List<String>> fsmIdVersionToContextIdMap;
//    private Map<String, Long> timeOutMap;
//
//    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
//
//    private Clock clock = Clock.systemDefaultZone();
//
//    public StartFSMReqQueue(long timeoutMillis) {
//        this.timeoutMillis = timeoutMillis;
//        contextIdToReqMap = new ConcurrentHashMap<>();
//        fsmIdVersionToContextIdMap = new ConcurrentHashMap<>();
//        timeOutMap = new ConcurrentHashMap<>();
//    }
//
//    public Optional<List<StartFSMRequest>> get(String fsmId, int version){
//        this.lock.writeLock().lock();
//        this.lock.readLock().lock();
//        String key = getKey(fsmId, version);
//        try {
//            evictTimedOut();
//            if (isPresent(key)) {
//                List<String> fsmContextIds = fsmIdVersionToContextIdMap.get(key);
//                List<StartFSMRequest> res = new ArrayList<>();
//                for(String contextId: fsmContextIds){
//                    if(contextIdToReqMap.containsKey(contextId))
//                        res.add(contextIdToReqMap.get(contextId));
//                }
//                evictAllWith(key);
////                System.out.println(res);
//                return Optional.of(res);
//            }
//            return Optional.empty();
//        }finally {
//            this.lock.writeLock().unlock();
//            this.lock.readLock().unlock();
//        }
//    }
//
//    public void put(StartFSMRequest startFSMRequest){
//        this.lock.writeLock().lock();
//        this.lock.readLock().lock();
//        try {
//            evictTimedOut();
//            String key = getKey(startFSMRequest.getFsmName(), startFSMRequest.getVersion());
//            if(!fsmIdVersionToContextIdMap.containsKey(key)){
//                fsmIdVersionToContextIdMap.put(key, new ArrayList<>());
//            }
//            List<String> inMapReqs = fsmIdVersionToContextIdMap.get(key);
//            inMapReqs.add(startFSMRequest.getFsmContextId());
//            timeOutMap.put(startFSMRequest.getFsmContextId(), clock.millis());
//            fsmIdVersionToContextIdMap.put(key, inMapReqs);
//            contextIdToReqMap.put(startFSMRequest.getFsmContextId(), startFSMRequest);
//        }finally {
//            this.lock.writeLock().unlock();
//            this.lock.readLock().unlock();
//        }
//    }
//
//    public void clear(){
//        this.lock.writeLock().lock();
//        try {
//            fsmIdVersionToContextIdMap.clear();
//            contextIdToReqMap.clear();
//            timeOutMap.clear();
//        }finally {
//            this.lock.writeLock().unlock();
//        }
//    }
//
//    public long size(){
//        this.lock.readLock().lock();
//        try {
//            return contextIdToReqMap.size();
//        }finally {
//            this.lock.readLock().unlock();
//        }
//    }
//
//    public boolean isContextIdPresent(String contextID){
//        return contextIdToReqMap.containsKey(contextID);
//    }
//
//    private boolean isPresent(String key){
//        this.lock.readLock().lock();
//        try{
//            return fsmIdVersionToContextIdMap.containsKey(key);
//        }finally {
//            this.lock.readLock().unlock();
//        }
//    }
//
//    private void evictAllWith(String key){
//        List<String> fsmContextIds = fsmIdVersionToContextIdMap.get(key);
//        fsmIdVersionToContextIdMap.remove(key);
//        for(String contextId : fsmContextIds){
//            contextIdToReqMap.remove(contextId);
//            timeOutMap.remove(contextId);
//        }
//    }
//
//    private void evictTimedOut(){
//        Set<String> fsmContextIds = timeOutMap.keySet();
//        for(String fsmContextId: fsmContextIds){
//            if(clock.millis() >= timeoutMillis + timeOutMap.get(fsmContextId)){
//                StartFSMRequest timeOutReq = contextIdToReqMap.get(fsmContextId);
//                evict(getKey(timeOutReq.getFsmName(), timeOutReq.getVersion()), fsmContextId);
//            }
//        }
//    }
//
//    private String getKey(String fsmID, int version){
//        return fsmID + "_$$_" + version;
//    }
//
//    private void evict(String key, String fsmContextId){
//        List<String> fsmContextIds = fsmIdVersionToContextIdMap.get(key);
//        List<String> prunedContextIds = new ArrayList<>();
//        for(String contextId : fsmContextIds){
//            if(fsmContextId.equals(contextId)) {
//                contextIdToReqMap.remove(contextId);
//                timeOutMap.remove(contextId);
//            }else{
//                prunedContextIds.add(contextId);
//            }
//        }
//        if(prunedContextIds.isEmpty()){
//            fsmIdVersionToContextIdMap.remove(key);
//        }else{
//            fsmIdVersionToContextIdMap.put(key, prunedContextIds);
//        }
//    }
}
