package ru.georgdeveloper.assistantbaseupdate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantbaseupdate.entity.mysql.MainLinesOnline;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий для работы с таблицей main_lines_online
 */
@Repository
public interface MainLinesOnlineRepository extends JpaRepository<MainLinesOnline, Long> {

    /**
     * Получить все записи по названию линии
     */
    List<MainLinesOnline> findByLineName(String lineName);

    /**
     * Получить все записи по области
     */
    List<MainLinesOnline> findByArea(String area);

    /**
     * Получить последние записи для всех линий (по одной на линию)
     */
    @Query("SELECT m FROM MainLinesOnline m WHERE m.id IN " +
           "(SELECT MAX(m2.id) FROM MainLinesOnline m2 GROUP BY m2.lineName)")
    List<MainLinesOnline> findLatestForAllLines();

    /**
     * Получить последние записи для линий определенной области
     */
    @Query("SELECT m FROM MainLinesOnline m WHERE m.area = :area AND m.id IN " +
           "(SELECT MAX(m2.id) FROM MainLinesOnline m2 WHERE m2.area = :area GROUP BY m2.lineName)")
    List<MainLinesOnline> findLatestForAreaLines(@Param("area") String area);

    /**
     * Получить записи за последние N часов
     */
    @Query("SELECT m FROM MainLinesOnline m WHERE m.lastUpdate >= :since ORDER BY m.lastUpdate DESC")
    List<MainLinesOnline> findSince(@Param("since") LocalDateTime since);

    /**
     * Удалить старые записи (старше указанного времени)
     */
    void deleteByLastUpdateBefore(LocalDateTime cutoffTime);

    /**
     * Получить последнюю запись для конкретной линии
     */
    @Query("SELECT m FROM MainLinesOnline m WHERE m.lineName = :lineName ORDER BY m.lastUpdate DESC LIMIT 1")
    MainLinesOnline findLatestByLineName(@Param("lineName") String lineName);
}
