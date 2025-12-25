package com.example.shardingdemo.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

public class OrderTableShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    private static final String TABLE_COUNT_KEY = "table-count";
    private static final int DEFAULT_TABLE_COUNT = 2;

    private Properties props = new Properties();
    private int tableCount = DEFAULT_TABLE_COUNT;

    @Override
    public void init(Properties props) {
        this.props = props;
        String count = props.getProperty(TABLE_COUNT_KEY);
        if (count != null && !count.isBlank()) {
            this.tableCount = Integer.parseInt(count);
        }
    }

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> shardingValue) {
        long value = shardingValue.getValue();
        int suffix = (int) (Math.abs(value) % tableCount);
        String expectedSuffix = "_" + suffix;
        for (String tableName : availableTargetNames) {
            if (tableName.endsWith(expectedSuffix)) {
                return tableName;
            }
        }
        throw new IllegalArgumentException("No table found for value: " + value);
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Long> shardingValue) {
        return availableTargetNames;
    }

    @Override
    public String getType() {
        return "CUSTOM_ORDER_TABLE";
    }

    @Override
    public Properties getProps() {
        return props;
    }
}
