package com.microsoft.seeds.fsmexecutor.models.cache.cachesingleton;

import com.microsoft.seeds.fsmexecutor.models.cache.LRUCache;
import com.microsoft.seeds.fsmexecutor.models.fsm.ExpFSM;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;

public class StaticExpFSMCache{
    private LRUCache<String, ExpFSM> cache;
    private static StaticExpFSMCache instance;

    private StaticExpFSMCache(){
        cache = new LRUCache<>(Constants.FSM_CACHE_SIZE);
    }

    public static StaticExpFSMCache getInstance(){
        if(instance == null){
            instance = new StaticExpFSMCache();
        }
        return instance;
    }

    public LRUCache<String, ExpFSM> getCache() {
        return cache;
    }
}
