package ru.georgdeveloper.assistantbaseupdate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.georgdeveloper.assistantbaseupdate.entity.ProductionMetricsOnline;

import java.time.LocalDateTime;

/**
 * Репозиторий для работы с таблицей production_metrics_online.
 * 
 * Предоставляет методы для сохранения и обновления метрик производства
 * в режиме онлайн.
 */
@Repository
public interface ProductionMetricsOnlineRepository extends JpaRepository<ProductionMetricsOnline, Long> {

    /**
     * Удаляет старые записи для области перед вставкой новых
     * @param area название области
     */
    @Modifying
    @Query("DELETE FROM ProductionMetricsOnline p WHERE p.area = :area")
    void deleteByArea(@Param("area") String area);

    /**
     * Находит последнюю запись для области
     * @param area название области
     * @return последняя запись метрик или null
     */
    @Query("SELECT p FROM ProductionMetricsOnline p WHERE p.area = :area ORDER BY p.lastUpdate DESC")
    ProductionMetricsOnline findLatestByArea(@Param("area") String area);

    /**
     * Подсчитывает количество записей для области
     * @param area название области
     * @return количество записей
     */
    long countByArea(String area);

    /**
     * Находит записи за последние N часов
     * @param area название области
     * @param hours количество часов
     * @return количество записей
     */
    @Query("SELECT COUNT(p) FROM ProductionMetricsOnline p WHERE p.area = :area AND p.lastUpdate >= :since")
    long countByAreaAndLastUpdateAfter(@Param("area") String area, @Param("since") LocalDateTime since);
}
