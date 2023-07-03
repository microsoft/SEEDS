package com.microsoft.seeds.fsmexecutor.models.cache.cachesingleton;

import com.microsoft.seeds.fsmexecutor.models.cache.LRUCache;
import com.microsoft.seeds.fsmexecutor.models.fsm.OnDemandFSMData;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;

public class StaticOnDemandFSMDataCache {
    private LRUCache<String, OnDemandFSMData> cache;
    private static StaticOnDemandFSMDataCache instance;

    private StaticOnDemandFSMDataCache(){
        cache = new LRUCache<>(Constants.ON_DEMAND_FSM_DATA_CACHE_SIZE);
    }

    public static StaticOnDemandFSMDataCache getInstance(){
        if(instance == null){
            instance = new StaticOnDemandFSMDataCache();
        }
        return instance;
    }

    public LRUCache<String, OnDemandFSMData> getCache() {
        return cache;
    }
}
