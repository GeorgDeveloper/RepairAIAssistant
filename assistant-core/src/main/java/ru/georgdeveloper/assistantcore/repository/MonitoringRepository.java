package ru.georgdeveloper.assistantcore.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class MonitoringRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getRegions() {
        return jdbcTemplate.queryForList("SELECT id, name_region FROM monitoring_bd.region");
    }

    public List<Map<String, Object>> getEquipment(int regionId) {
        return jdbcTemplate.queryForList("SELECT id, name_equipment FROM monitoring_bd.equipment WHERE region_id = ?", regionId);
    }

    public List<Map<String, Object>> getNodes(int equipmentId) {
        return jdbcTemplate.queryForList("SELECT id, name_node FROM monitoring_bd.node WHERE equipment_id = ?", equipmentId);
    }
}
