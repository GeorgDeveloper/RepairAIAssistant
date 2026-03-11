package ru.georgdeveloper.assistantbaseupdate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Конфигурационные свойства для синхронизации данных между базами.
 * 
 * Содержит настройки подключения к SQL Server и MySQL,
 * а также конфигурацию областей для синхронизации.
 */
@Component
@ConfigurationProperties(prefix = "data-sync")
public class DataSyncProperties {

    private boolean enabled = true;
    private String schedule = "0 */3 * * * ?";
    private SqlServer sqlServer = new SqlServer();
    private Mysql mysql = new Mysql();
    private List<Area> areas;
    private List<MainLine> mainLines;

    // Геттеры и сеттеры
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public SqlServer getSqlServer() {
        return sqlServer;
    }

    public void setSqlServer(SqlServer sqlServer) {
        this.sqlServer = sqlServer;
    }

    public Mysql getMysql() {
        return mysql;
    }

    public void setMysql(Mysql mysql) {
        this.mysql = mysql;
    }

    public List<Area> getAreas() {
        return areas;
    }

    public void setAreas(List<Area> areas) {
        this.areas = areas;
    }

    public List<MainLine> getMainLines() {
        return mainLines;
    }

    public void setMainLines(List<MainLine> mainLines) {
        this.mainLines = mainLines;
    }

    /**
     * Настройки подключения к SQL Server
     */
    public static class SqlServer {
        private String url;
        private List<String> fallbackUrls;
        private String username;
        private String password;
        private String driver;
        private int timeout = 30;

        // Геттеры и сеттеры
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public List<String> getFallbackUrls() {
            return fallbackUrls;
        }

        public void setFallbackUrls(List<String> fallbackUrls) {
            this.fallbackUrls = fallbackUrls;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriver() {
            return driver;
        }

        public void setDriver(String driver) {
            this.driver = driver;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * Настройки подключения к MySQL
     */
    public static class Mysql {
        private String url;
        private String username;
        private String password;
        private String driver;

        // Геттеры и сеттеры
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDriver() {
            return driver;
        }

        public void setDriver(String driver) {
            this.driver = driver;
        }
    }

    /**
     * Конфигурация области для синхронизации
     */
    public static class Area {
        private String name;
        private String workingTimeTable;
        private String wtColumn;
        private double defaultWt;
        private String filterColumn;
        private String filterValue;

        // Геттеры и сеттеры
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getWorkingTimeTable() {
            return workingTimeTable;
        }

        public void setWorkingTimeTable(String workingTimeTable) {
            this.workingTimeTable = workingTimeTable;
        }

        public String getWtColumn() {
            return wtColumn;
        }

        public void setWtColumn(String wtColumn) {
            this.wtColumn = wtColumn;
        }

        public double getDefaultWt() {
            return defaultWt;
        }

        public void setDefaultWt(double defaultWt) {
            this.defaultWt = defaultWt;
        }

        public String getFilterColumn() {
            return filterColumn;
        }

        public void setFilterColumn(String filterColumn) {
            this.filterColumn = filterColumn;
        }

        public String getFilterValue() {
            return filterValue;
        }

        public void setFilterValue(String filterValue) {
            this.filterValue = filterValue;
        }
    }

    /**
     * Конфигурация ключевой линии для синхронизации
     */
    public static class MainLine {
        private String name;
        private String area;
        private String machineFilter;

        // Геттеры и сеттеры
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArea() {
            return area;
        }

        public void setArea(String area) {
            this.area = area;
        }

        public String getMachineFilter() {
            return machineFilter;
        }

        public void setMachineFilter(String machineFilter) {
            this.machineFilter = machineFilter;
        }
    }
}
